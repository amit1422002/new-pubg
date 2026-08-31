/**
 * Injected into BGMI process — runs BGMIGameMod.lua inside the game's Lua VM.
 */
#include "fake_dlfcn.h"

#include <android/log.h>
#include <atomic>
#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <fstream>
#include <link.h>
#include <mutex>
#include <string>
#include <thread>
#include <unistd.h>
#include <vector>
#include <sys/mman.h>

#define TAG "guest-login"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

namespace {

constexpr const char *kLuaTick = R"lua(
pcall(function()
    if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end
end)
)lua";

static char g_hookLibAnchor;

static std::string gameModLuaPathBesideHook() {
    Dl_info info{};
    if (dladdr(&g_hookLibAnchor, &info) && info.dli_fname != nullptr) {
        std::string path = info.dli_fname;
        const size_t slash = path.rfind('/');
        if (slash != std::string::npos) {
            return path.substr(0, slash + 1) + "bgmi_game_mod.lua";
        }
    }
    return {};
}

// BGMI 4.4 arm64 — libUE4.so offsets (update per game patch)
constexpr uintptr_t kBgmi44_LoadBufferX = 0xB2B0DD0UL;
constexpr uintptr_t kBgmi44_PCallK      = 0xB28D318UL;
constexpr uintptr_t kBgmi44_LuaTostring = 0xB28B738UL;
constexpr uintptr_t kBgmi44_LuaSettop   = 0xAEDD5C8UL;

using lua_CFunction = int (*)(void *);
using luaL_loadstring_t = int (*)(void *, const char *);
using luaL_loadbufferx_t = int (*)(void *, const char *, size_t, const char *, const char *);
using lua_pcall_t = int (*)(void *, int, int, int);
using lua_getglobal_t = void (*)(void *, const char *);
using lua_type_t = int (*)(void *, int);
using lua_toboolean_t = int (*)(void *, int);
using lua_settop_t = void (*)(void *, int);
using lua_tostring_t = const char *(*)(void *, int);

constexpr int kLuaTBoolean = 1;
constexpr int kLuaTString = 4;

std::mutex g_mutex;
std::atomic<bool> g_hooked{false};
std::atomic<int> g_attempts{0};
std::atomic<bool> g_success{false};
std::atomic<bool> g_body_loaded{false};

using lua_pcallk_t = int (*)(void *, int, int, int, long, void *);

lua_pcall_t g_orig_pcall = nullptr;
lua_pcallk_t g_orig_pcallk = nullptr;
luaL_loadstring_t g_luaL_loadstring = nullptr;
luaL_loadbufferx_t g_luaL_loadbufferx = nullptr;
lua_getglobal_t g_lua_getglobal = nullptr;
lua_type_t g_lua_type = nullptr;
lua_toboolean_t g_lua_toboolean = nullptr;
lua_settop_t g_lua_settop = nullptr;
lua_tostring_t g_lua_tostring = nullptr;
std::atomic<bool> g_use_pcallk{false};
void *g_hook_target = nullptr;
thread_local int g_hook_depth = 0;

struct LoadedModule {
    uintptr_t base;
    std::string path;
};

struct PhdrSearch {
    const char *needle;
    uintptr_t base;
    std::string path;
};

static int phdrCallback(struct dl_phdr_info *info, size_t, void *data) {
    auto *search = static_cast<PhdrSearch *>(data);
    if (info->dlpi_name == nullptr || info->dlpi_name[0] == '\0') {
        return 0;
    }
    if (strstr(info->dlpi_name, search->needle) == nullptr) {
        return 0;
    }
    search->base = static_cast<uintptr_t>(info->dlpi_addr);
    search->path = info->dlpi_name;
    return 1;
}

static int collectModulesCallback(struct dl_phdr_info *info, size_t, void *data) {
    if (info->dlpi_name == nullptr || info->dlpi_name[0] == '\0') {
        return 0;
    }
    auto *mods = static_cast<std::vector<LoadedModule> *>(data);
    LoadedModule mod;
    mod.base = static_cast<uintptr_t>(info->dlpi_addr);
    mod.path = info->dlpi_name;
    mods->push_back(mod);
    return 0;
}

static bool findUe4Module(PhdrSearch *out) {
    PhdrSearch search{ "libUE4.so", 0, {} };
    dl_iterate_phdr(phdrCallback, &search);
    if (search.base != 0) {
        *out = search;
        return true;
    }
    return false;
}

static std::string hookFilesDir() {
    std::string luaPath = gameModLuaPathBesideHook();
    if (luaPath.empty()) {
        return {};
    }
    const size_t slash = luaPath.rfind('/');
    if (slash == std::string::npos) {
        return {};
    }
    return luaPath.substr(0, slash + 1);
}

static std::vector<std::string> readUe4PathHints() {
    std::vector<std::string> paths;
    const std::string dir = hookFilesDir();
    if (dir.empty()) {
        return paths;
    }
    std::ifstream in((dir + "guest_login_ue4.paths").c_str());
    std::string line;
    while (std::getline(in, line)) {
        if (!line.empty()) {
            paths.push_back(line);
        }
    }
    return paths;
}

static std::vector<std::string> deriveVfsUe4Paths() {
    std::vector<std::string> paths;
    const std::string dir = hookFilesDir();
    const size_t vfs = dir.find("/.vfs/");
    if (vfs == std::string::npos) {
        return paths;
    }
    const std::string root = dir.substr(0, vfs + 6);
    const char *suffixes[] = {
            "data/app/com.pubg.imobile/lib/arm64-v8a/libUE4.so",
            "data/app/com.pubg.imobile/lib/arm64/libUE4.so",
            "data/app/com.pubg.imobile/lib/libUE4.so",
    };
    for (const char *suffix : suffixes) {
        paths.emplace_back(root + suffix);
    }
    return paths;
}

static void *resolveSymbol(void *elfHandle, const char *const *names) {
    for (int i = 0; names[i] != nullptr; ++i) {
        void *addr = fake_dlsym(elfHandle, names[i]);
        if (addr != nullptr) {
            return addr;
        }
    }
    return nullptr;
}

static bool assignLuaSymbols(void *loadstring, void *pcallk, void *pcall,
                             void *getglobal, void *typeFn, void *tobool, void *settop) {
    if (loadstring == nullptr || (pcallk == nullptr && pcall == nullptr)) {
        return false;
    }
    g_luaL_loadstring = reinterpret_cast<luaL_loadstring_t>(loadstring);
    g_lua_getglobal = reinterpret_cast<lua_getglobal_t>(getglobal);
    g_lua_type = reinterpret_cast<lua_type_t>(typeFn);
    g_lua_toboolean = reinterpret_cast<lua_toboolean_t>(tobool);
    g_lua_settop = reinterpret_cast<lua_settop_t>(settop);

    if (pcallk != nullptr) {
        g_orig_pcallk = reinterpret_cast<lua_pcallk_t>(pcallk);
        g_use_pcallk.store(true, std::memory_order_relaxed);
        g_hook_target = pcallk;
    } else {
        g_orig_pcall = reinterpret_cast<lua_pcall_t>(pcall);
        g_use_pcallk.store(false, std::memory_order_relaxed);
        g_hook_target = pcall;
    }
    return true;
}

static bool resolveFromElfFile(const char *path, uintptr_t base, const char *label) {
    if (path == nullptr || base == 0) {
        return false;
    }
    void *handle = fake_dlopen_ex(path, static_cast<intptr_t>(base));
    if (handle == nullptr) {
        return false;
    }

    const char *kLoadNames[] = { "luaL_loadstring", "luaL_loadbuffer", nullptr };
    const char *kGetGlobalNames[] = { "lua_getglobal", nullptr };
    const char *kTypeNames[] = { "lua_type", nullptr };
    const char *kToBoolNames[] = { "lua_toboolean", nullptr };
    const char *kSetTopNames[] = { "lua_settop", nullptr };

    void *loadstring = resolveSymbol(handle, kLoadNames);
    void *pcallk = fake_dlsym(handle, "lua_pcallk");
    void *pcall = fake_dlsym(handle, "lua_pcall");
    void *getglobal = resolveSymbol(handle, kGetGlobalNames);
    void *typeFn = resolveSymbol(handle, kTypeNames);
    void *tobool = resolveSymbol(handle, kToBoolNames);
    void *settop = resolveSymbol(handle, kSetTopNames);

    const bool ok = assignLuaSymbols(loadstring, pcallk, pcall, getglobal, typeFn, tobool, settop);
    fake_dlclose(handle);

    if (ok) {
        LOGI("resolved lua api via %s path=%s base=%p target=%p loadstring=%p",
             label, path, reinterpret_cast<void *>(base), g_hook_target, g_luaL_loadstring);
    }
    return ok;
}

static bool moduleLooksRelevant(const std::string &path) {
    return path.find("libUE4.so") != std::string::npos
           || path.find("UE4") != std::string::npos
           || path.find("lua") != std::string::npos
           || path.find("slua") != std::string::npos
           || path.find("Slua") != std::string::npos;
}

static bool isUe4Loaded() {
    PhdrSearch found{};
    return findUe4Module(&found);
}

static void *resolveLdSymbol(const char *const *names) {
    for (int i = 0; names[i] != nullptr; ++i) {
        void *addr = dlsym(RTLD_DEFAULT, names[i]);
        if (addr != nullptr) {
            return addr;
        }
    }
    return nullptr;
}

static void resetLuaApi() {
    g_orig_pcall = nullptr;
    g_orig_pcallk = nullptr;
    g_luaL_loadstring = nullptr;
    g_luaL_loadbufferx = nullptr;
    g_lua_getglobal = nullptr;
    g_lua_type = nullptr;
    g_lua_toboolean = nullptr;
    g_lua_settop = nullptr;
    g_lua_tostring = nullptr;
    g_hook_target = nullptr;
    g_use_pcallk.store(false, std::memory_order_relaxed);
}

static void resolveLuaHelpersFromUe4(uintptr_t base, const std::string &path) {
    if (base == 0) {
        return;
    }
    const char *elfPath = path.empty() ? "libUE4.so" : path.c_str();
    void *handle = fake_dlopen_ex(elfPath, static_cast<intptr_t>(base));
    if (handle == nullptr) {
        return;
    }
    g_lua_getglobal = reinterpret_cast<lua_getglobal_t>(fake_dlsym(handle, "lua_getglobal"));
    g_lua_type = reinterpret_cast<lua_type_t>(fake_dlsym(handle, "lua_type"));
    g_lua_toboolean = reinterpret_cast<lua_toboolean_t>(fake_dlsym(handle, "lua_toboolean"));
    g_lua_settop = reinterpret_cast<lua_settop_t>(fake_dlsym(handle, "lua_settop"));
    g_lua_tostring = reinterpret_cast<lua_tostring_t>(fake_dlsym(handle, "lua_tostring"));
    fake_dlclose(handle);
    if (g_lua_tostring == nullptr && base != 0) {
        g_lua_tostring = reinterpret_cast<lua_tostring_t>(base + kBgmi44_LuaTostring);
    }
    if (g_lua_settop == nullptr && base != 0) {
        g_lua_settop = reinterpret_cast<lua_settop_t>(base + kBgmi44_LuaSettop);
    }
}

static bool resolveLuaViaBgmiOffsets(uintptr_t base, const std::string &path) {
    if (base == 0) {
        return false;
    }
    g_luaL_loadbufferx = reinterpret_cast<luaL_loadbufferx_t>(base + kBgmi44_LoadBufferX);
    g_orig_pcallk = reinterpret_cast<lua_pcallk_t>(base + kBgmi44_PCallK);
    g_use_pcallk.store(true, std::memory_order_relaxed);
    g_hook_target = reinterpret_cast<void *>(base + kBgmi44_PCallK);
    resolveLuaHelpersFromUe4(base, path);
    LOGI("resolved lua via BGMI4.4 offsets base=%p load=%p pcallk=%p settop=%p tostring=%p",
         reinterpret_cast<void *>(base),
         reinterpret_cast<void *>(g_luaL_loadbufferx),
         g_hook_target,
         reinterpret_cast<void *>(g_lua_settop),
         reinterpret_cast<void *>(g_lua_tostring));
    return g_luaL_loadbufferx != nullptr && g_orig_pcallk != nullptr;
}

static bool evalLuaExpr(void *L, const char *expr, int nresults) {
    if (L == nullptr || expr == nullptr || g_luaL_loadbufferx == nullptr) {
        return false;
    }
    if (g_luaL_loadbufferx(L, expr, std::strlen(expr), "=bgmi_chk", nullptr) != 0) {
        if (g_lua_tostring != nullptr) {
            const char *err = g_lua_tostring(L, -1);
            if (err != nullptr) {
                LOGW("eval load err: %s", err);
            }
        }
        if (g_lua_settop != nullptr) {
            g_lua_settop(L, -2);
        }
        return false;
    }
    const int rc = g_use_pcallk.load(std::memory_order_relaxed)
            ? (g_orig_pcallk != nullptr ? g_orig_pcallk(L, 0, nresults, 0, 0, nullptr) : -1)
            : (g_orig_pcall != nullptr ? g_orig_pcall(L, 0, nresults, 0) : -1);
    if (rc != 0) {
        if (g_lua_tostring != nullptr) {
            const char *err = g_lua_tostring(L, -1);
            if (err != nullptr) {
                LOGW("eval pcall err: %s", err);
            }
        }
        if (g_lua_settop != nullptr) {
            g_lua_settop(L, -2);
        }
        return false;
    }
    return true;
}

static void popLuaStack(void *L, int remove) {
    if (L == nullptr || remove <= 0) {
        return;
    }
    if (g_lua_settop != nullptr) {
        g_lua_settop(L, -remove);
        return;
    }
    char expr[48];
    std::snprintf(expr, sizeof(expr), "return");
    if (evalLuaExpr(L, expr, 0)) {
        return;
    }
}

static bool readLuaStringGlobal(void *L, const char *name, const char **out) {
    if (out == nullptr || name == nullptr || g_lua_tostring == nullptr) {
        return false;
    }
    char expr[128];
    std::snprintf(expr, sizeof(expr), "return tostring(_G.%s or \"\")", name);
    if (!evalLuaExpr(L, expr, 1)) {
        return false;
    }
    *out = g_lua_tostring(L, -1);
    popLuaStack(L, 1);
    return *out != nullptr;
}

static bool readLuaBoolGlobal(void *L, const char *name, bool &out) {
    char expr[128];
    std::snprintf(expr, sizeof(expr), "return (_G.%s and true or false)", name);
    if (!evalLuaExpr(L, expr, 1) || g_lua_tostring == nullptr) {
        return false;
    }
    const char *s = g_lua_tostring(L, -1);
    out = s != nullptr && (s[0] == 't' || s[0] == '1');
    popLuaStack(L, 1);
    return true;
}

static void logLuaModStatus(void *L, int attempt) {
    if (L == nullptr) {
        return;
    }
    const char *fail = "?";
    bool bodyRan = false;
    bool active = false;
    readLuaStringGlobal(L, "__BGMI_LAST_FAIL", &fail);
    readLuaBoolGlobal(L, "__BGMI_BODY_RAN", bodyRan);
    readLuaBoolGlobal(L, "__BGMI_FEATURES_ACTIVE", active);
    LOGI("mod status attempt=%d body=%d active=%d fail=%s",
         attempt + 1, bodyRan ? 1 : 0, active ? 1 : 0, fail != nullptr ? fail : "?");
}

static bool resolveLuaApi() {
    resetLuaApi();

    PhdrSearch ue4{};
    findUe4Module(&ue4);
    if (ue4.base != 0 && resolveLuaViaBgmiOffsets(ue4.base, ue4.path)) {
        return true;
    }

    const char *kLoadNames[] = { "luaL_loadstring", "luaL_loadbuffer", nullptr };
    void *loadstring = resolveLdSymbol(kLoadNames);
    void *pcallk = dlsym(RTLD_DEFAULT, "lua_pcallk");
    void *pcall = dlsym(RTLD_DEFAULT, "lua_pcall");
    if (assignLuaSymbols(loadstring, pcallk, pcall,
                         dlsym(RTLD_DEFAULT, "lua_getglobal"),
                         dlsym(RTLD_DEFAULT, "lua_type"),
                         dlsym(RTLD_DEFAULT, "lua_toboolean"),
                         dlsym(RTLD_DEFAULT, "lua_settop"))) {
        LOGI("resolved lua api via RTLD_DEFAULT");
        return true;
    }

    static int s_failLog;
    if ((++s_failLog % 20) == 1) {
        LOGW("resolveLuaApi failed ue4_base=%p", reinterpret_cast<void *>(ue4.base));
    }
    return false;
}

static bool patchWasApplied(void *L, bool ranOk, int attempt) {
    (void) attempt;
    if (L == nullptr || !ranOk) {
        return false;
    }
    bool active = false;
    return readLuaBoolGlobal(L, "__BGMI_FEATURES_ACTIVE", active) && active;
}

static std::string readFile(const char *path) {
    std::ifstream in(path, std::ios::binary);
    if (!in) {
        return {};
    }
    return std::string((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
}

static bool execLuaChunk(void *L) {
    if (g_use_pcallk.load(std::memory_order_relaxed)) {
        if (g_orig_pcallk == nullptr) {
            return false;
        }
        if (g_orig_pcallk(L, 0, 0, 0, 0, nullptr) != 0) {
            if (g_lua_tostring != nullptr) {
                const char *err = g_lua_tostring(L, -1);
                if (err != nullptr) {
                    LOGW("lua pcall err: %s", err);
                }
            }
            if (g_lua_settop != nullptr) {
                g_lua_settop(L, -2);
            }
            return false;
        }
        return true;
    }
    if (g_orig_pcall == nullptr) {
        return false;
    }
    if (g_orig_pcall(L, 0, 0, 0) != 0) {
        if (g_lua_tostring != nullptr) {
            const char *err = g_lua_tostring(L, -1);
            if (err != nullptr) {
                LOGW("lua pcall err: %s", err);
            }
        }
        if (g_lua_settop != nullptr) {
            g_lua_settop(L, -2);
        }
        return false;
    }
    return true;
}

static bool runLua(void *L, const char *source) {
    if (L == nullptr || source == nullptr || g_hook_target == nullptr) {
        return false;
    }
    if (g_luaL_loadbufferx != nullptr) {
        const size_t len = std::strlen(source);
        if (g_luaL_loadbufferx(L, source, len, "=bgmi_game_mod", nullptr) != 0) {
            if (g_lua_tostring != nullptr) {
                const char *err = g_lua_tostring(L, -1);
                if (err != nullptr) {
                    LOGW("lua load err: %s", err);
                }
            }
            if (g_lua_settop != nullptr) {
                g_lua_settop(L, -2);
            }
            return false;
        }
    } else if (g_luaL_loadstring != nullptr) {
        if (g_luaL_loadstring(L, source) != 0) {
            if (g_lua_tostring != nullptr) {
                const char *err = g_lua_tostring(L, -1);
                if (err != nullptr) {
                    LOGW("lua load err: %s", err);
                }
            }
            if (g_lua_settop != nullptr) {
                g_lua_settop(L, -2);
            }
            return false;
        }
    } else {
        return false;
    }
    return execLuaChunk(L);
}

static void tryApplyGameModPatch(void *L) {
    if (g_hook_depth > 0) {
        return;
    }
    int attempt = g_attempts.fetch_add(1, std::memory_order_relaxed);
    if (g_success.load(std::memory_order_relaxed) && (attempt % 60) != 0) {
        return;
    }
    if (attempt >= 1200) {
        return;
    }
    if (!g_body_loaded.load(std::memory_order_relaxed)) {
        std::string luaPath = gameModLuaPathBesideHook();
        std::string fileScript = luaPath.empty() ? std::string() : readFile(luaPath.c_str());
        if (!fileScript.empty() && runLua(L, fileScript.c_str())) {
            g_body_loaded.store(true, std::memory_order_relaxed);
            LOGI("game mod body loaded from file");
            logLuaModStatus(L, attempt);
        } else if (!fileScript.empty()) {
            LOGW("game mod body load failed attempt=%d", attempt + 1);
        }
    }
    bool ran = runLua(L, kLuaTick);
    if (ran && ((attempt % 10) == 0 || attempt < 5)) {
        logLuaModStatus(L, attempt);
    }
    if (ran && patchWasApplied(L, ran, attempt)) {
        if (!g_success.load(std::memory_order_relaxed)) {
            g_success.store(true, std::memory_order_relaxed);
            LOGI("game mod features active (attempt %d)", attempt + 1);
        }
    }
}

static int hook_lua_pcall(void *L, int nargs, int nresults, int errfunc) {
    if (g_hook_depth > 0) {
        return g_orig_pcall(L, nargs, nresults, errfunc);
    }
    ++g_hook_depth;
    tryApplyGameModPatch(L);
    --g_hook_depth;
    return g_orig_pcall(L, nargs, nresults, errfunc);
}

static int hook_lua_pcallk(void *L, int nargs, int nresults, int errfunc, long ctx, void *k) {
    if (g_hook_depth > 0) {
        return g_orig_pcallk(L, nargs, nresults, errfunc, ctx, k);
    }
    ++g_hook_depth;
    tryApplyGameModPatch(L);
    --g_hook_depth;
    return g_orig_pcallk(L, nargs, nresults, errfunc, ctx, k);
}

#if defined(__aarch64__)

#include <sys/mman.h>

static bool setWritable(void *addr, size_t len) {
    const long page = sysconf(_SC_PAGESIZE);
    uintptr_t start = reinterpret_cast<uintptr_t>(addr) & ~(static_cast<uintptr_t>(page) - 1);
    uintptr_t end = (reinterpret_cast<uintptr_t>(addr) + len + page - 1) & ~(static_cast<uintptr_t>(page) - 1);
    return mprotect(reinterpret_cast<void *>(start), end - start, PROT_READ | PROT_WRITE | PROT_EXEC) == 0;
}

static bool installInlineHook(void *target, void *replace) {
    if (!setWritable(target, 16)) {
        return false;
    }
    auto *patch = reinterpret_cast<uint32_t *>(target);
    patch[0] = 0x58000051;
    patch[1] = 0xD61F0220;
    *reinterpret_cast<uint64_t *>(patch + 2) = reinterpret_cast<uint64_t>(replace);
    __builtin___clear_cache(reinterpret_cast<char *>(target),
                            reinterpret_cast<char *>(target) + 16);
    return true;
}

static lua_pcall_t buildTrampoline(void *target) {
    constexpr size_t kStolen = 16;
    void *page = mmap(nullptr, 4096, PROT_READ | PROT_WRITE | PROT_EXEC,
                      MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (page == MAP_FAILED) {
        return nullptr;
    }
    std::memcpy(page, target, kStolen);
    auto *patch = reinterpret_cast<uint32_t *>(static_cast<uint8_t *>(page) + kStolen);
    patch[0] = 0x58000051;
    patch[1] = 0xD61F0220;
    *reinterpret_cast<uint64_t *>(patch + 2) =
            reinterpret_cast<uint64_t>(target) + kStolen;
    __builtin___clear_cache(static_cast<char *>(page),
                            static_cast<char *>(page) + kStolen + 16);
    return reinterpret_cast<lua_pcall_t>(page);
}

#endif

static bool tryInstallHook() {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_hooked.load(std::memory_order_relaxed)) {
        return true;
    }
    if (!resolveLuaApi()) {
        return false;
    }
#if defined(__aarch64__)
    void *target = g_hook_target;
    void *replace = g_use_pcallk.load(std::memory_order_relaxed)
            ? reinterpret_cast<void *>(hook_lua_pcallk)
            : reinterpret_cast<void *>(hook_lua_pcall);

    if (g_use_pcallk.load(std::memory_order_relaxed)) {
        lua_pcallk_t trampoline = reinterpret_cast<lua_pcallk_t>(buildTrampoline(target));
        if (trampoline == nullptr) {
            LOGW("trampoline alloc failed");
            return false;
        }
        g_orig_pcallk = trampoline;
    } else {
        lua_pcall_t trampoline = buildTrampoline(target);
        if (trampoline == nullptr) {
            LOGW("trampoline alloc failed");
            return false;
        }
        g_orig_pcall = trampoline;
    }

    if (!installInlineHook(target, replace)) {
        LOGW("inline hook failed at %p", target);
        return false;
    }
    g_hooked.store(true, std::memory_order_relaxed);
    LOGI("%s hook installed at %p",
         g_use_pcallk.load(std::memory_order_relaxed) ? "lua_pcallk" : "lua_pcall",
         target);
    return true;
#else
    return false;
#endif
}

static void worker() {
    LOGI("game mod worker started");
    for (int i = 0; i < 600 && !g_hooked.load(std::memory_order_relaxed); ++i) {
        if (isUe4Loaded()) {
            if (tryInstallHook()) {
                return;
            }
        } else if (i % 20 == 0) {
            PhdrSearch found{};
            findUe4Module(&found);
            LOGI("waiting for UE4/Lua (iter=%d phdr=%p)", i, reinterpret_cast<void *>(found.base));
        }
        usleep(500 * 1000);
    }
    if (!g_hooked.load(std::memory_order_relaxed)) {
        LOGW("game mod hook timeout (ue4=%d)", isUe4Loaded() ? 1 : 0);
    }
}

} // namespace

static void delayedWorker() {
    // Short defer — Java side already waits past splash for login screen.
    usleep(3 * 1000 * 1000);
    worker();
}

__attribute__((constructor)) static void onGameModHookLoad() {
    LOGI("libgamemodhook loaded in pid=%d", getpid());
    std::thread(delayedWorker).detach();
}
