/**
 * Injected into BGMI process — runs guest/skin/game-mod Lua inside the game's Lua VM.
 * BGMI 4.4 arm64 — patchless exec-trap on lua_pcallk page (zero UE4 byte changes).
 */
#include "fake_dlfcn.h"
#include "MapsGameLibsDump.h"
#include "Ue4PatternFinder.h"

#include <android/log.h>
#include <atomic>
#include <cstdio>
#include <cstring>
#include <csignal>
#include <dlfcn.h>
#include <fstream>
#include <link.h>
#include <mutex>
#include <string>
#include <thread>
#include <unistd.h>
#include <ucontext.h>
#include <sys/mman.h>

#define TAG "guest-login"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

namespace {

constexpr const char *kLuaScript = R"lua(
_G.__GUEST_LOGIN_PATCHED = false
function _G.ForceEnableGuestLogin()
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if not ModuleManager then return end
        local login_module = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.login_module)
        if not login_module then return end
        local oldIsAvailable = login_module.IsAvailableChannel
        login_module.IsAvailableChannel = function(self, channel)
            if _G.ShareSource then
                if channel == _G.ShareSource.Guest or channel == _G.ShareSource.BgBg then return true end
            end
            if ShareSource then
                if channel == ShareSource.Guest or channel == ShareSource.BgBg then return true end
            end
            if oldIsAvailable then return oldIsAvailable(self, channel) end
            return false
        end
        login_module.CanShowMoreLoginChannel = function() return true end
        login_module.LoginTypeList = nil
        login_module.LoginTypeTable = nil
        local oldGetLoginTypeList = login_module.GetLoginTypeList
        login_module.GetLoginTypeList = function(self)
            local list = oldGetLoginTypeList(self)
            if list then
                local gGuest = (_G.ShareSource and _G.ShareSource.Guest) or (ShareSource and ShareSource.Guest)
                local gBgBg = (_G.ShareSource and _G.ShareSource.BgBg) or (ShareSource and ShareSource.BgBg)
                if gGuest or gBgBg then
                    local hasBgBg, hasGuest = false, false
                    for _, ch in pairs(list) do
                        if gBgBg and ch == gBgBg then hasBgBg = true end
                        if gGuest and ch == gGuest then hasGuest = true end
                    end
                    if not hasBgBg and gBgBg then table.insert(list, gBgBg) end
                    if not hasGuest and gGuest then table.insert(list, gGuest) end
                end
            end
            return list
        end
        local loginUI = login_module:GetLoginUI()
        if loginUI then
            if loginUI.RefreshUI then loginUI:RefreshUI() end
            if loginUI.ShowLoginButtons then loginUI:ShowLoginButtons() end
        end
        _G.__GUEST_LOGIN_PATCHED = true
    end)
end
pcall(_G.ForceEnableGuestLogin)
)lua";

static char g_hookLibAnchor;

static const char *kGuestHookFilesEnv = "XT_GUEST_HOOK_DIR";

static std::string guestHookFilesDir() {
    const char *env = getenv(kGuestHookFilesEnv);
    if (env != nullptr && env[0] != '\0') {
        std::string dir = env;
        if (!dir.empty() && dir.back() != '/') {
            dir.push_back('/');
        }
        return dir;
    }
    Dl_info info{};
    if (dladdr(&g_hookLibAnchor, &info) && info.dli_fname != nullptr) {
        std::string path = info.dli_fname;
        const size_t slash = path.rfind('/');
        if (slash != std::string::npos) {
            return path.substr(0, slash + 1);
        }
    }
    return {};
}

static std::string guestLuaPathBesideHook() {
    const std::string dir = guestHookFilesDir();
    return dir.empty() ? std::string() : dir + "guest_login_bgmi.lua";
}

static std::string skinLuaPathBesideHook() {
    const std::string dir = guestHookFilesDir();
    return dir.empty() ? std::string() : dir + "skin_mod_bgmi.lua";
}

static std::string gameModLuaPathBesideHook() {
    const std::string dir = guestHookFilesDir();
    return dir.empty() ? std::string() : dir + "bgmi_game_mod.lua";
}

static std::string probeLogPath() {
    return guestHookFilesDir() + "skin_probe.log";
}

static void appendProbeLog(const std::string &line) {
    const std::string path = probeLogPath();
    FILE *f = fopen(path.c_str(), "a");
    if (f != nullptr) {
        fprintf(f, "%s\n", line.c_str());
        fclose(f);
    }
    LOGI("PROBE %s", line.c_str());
}

static std::atomic<int> g_probe_runs{0};
/** Set false to skip bgmi_game_mod.lua entirely (guest login + skin still load). */
static constexpr bool kGameModEnabled = true;
static std::atomic<bool> g_gamemod_injected{false};
static std::atomic<bool> g_gamemod_done{!kGameModEnabled};
static std::atomic<int> g_gamemod_missing_logs{0};

static bool runLua(void *L, const char *source, const char *chunkName);

// BGMI 4.4 arm64 — libUE4.so offsets (updated)
constexpr uintptr_t kBgmi44_LoadBufferX = 0xB2B0DD0UL;
constexpr uintptr_t kBgmi44_PCallK      = 0xB28D318UL;
constexpr uintptr_t kBgmi44_ToString    = 0xB28B738UL;
constexpr uintptr_t kBgmi44_Settop      = 0xAEDD5C8UL;
constexpr uintptr_t kBgmi44_Sub         = 0xBF8C058UL;

using luaL_loadbufferx_t = int (*)(void *, const char *, size_t, const char *, const char *);
using lua_pcallk_t = int (*)(void *, int, int, int, long, void *);
using lua_getglobal_t = void (*)(void *, const char *);
using lua_type_t = int (*)(void *, int);
using lua_toboolean_t = int (*)(void *, int);
using lua_settop_t = void (*)(void *, int);
using lua_tostring_t = const char *(*)(void *, int);

constexpr int kLuaTBoolean = 1;

std::mutex g_mutex;
std::atomic<bool> g_trap_ready{false};
std::atomic<bool> g_trap_armed{false};
std::atomic<bool> g_trap_off{false};
std::atomic<int> g_attempts{0};
std::atomic<bool> g_guest_done{false};
std::atomic<bool> g_skin_done{false};
std::atomic<bool> g_skin_injected{false};
std::atomic<int> g_hook_depth{0};

lua_pcallk_t g_lua_pcallk = nullptr;
luaL_loadbufferx_t g_luaL_loadbufferx = nullptr;
lua_getglobal_t g_lua_getglobal = nullptr;
lua_type_t g_lua_type = nullptr;
lua_toboolean_t g_lua_toboolean = nullptr;
lua_settop_t g_lua_settop = nullptr;
lua_tostring_t g_lua_tostring = nullptr;
void *g_hook_target = nullptr;

#if defined(__aarch64__)
static void *g_trap_page = nullptr;
static size_t g_trap_page_size = 0;
static struct sigaction g_old_segv {};
static uint8_t g_alt_stack[64 * 1024];
static std::atomic<bool> g_segv_handler_installed{false};
#endif

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

static bool findUe4Module(PhdrSearch *out) {
    PhdrSearch search{ "libUE4.so", 0, {} };
    dl_iterate_phdr(phdrCallback, &search);
    if (search.base != 0) {
        *out = search;
        return true;
    }
    return false;
}

static bool isUe4Loaded() {
    PhdrSearch found{};
    return findUe4Module(&found);
}

static void resetLuaApi() {
    g_lua_pcallk = nullptr;
    g_luaL_loadbufferx = nullptr;
    g_lua_getglobal = nullptr;
    g_lua_type = nullptr;
    g_lua_toboolean = nullptr;
    g_lua_settop = nullptr;
    g_lua_tostring = nullptr;
    g_hook_target = nullptr;
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
    fake_dlclose(handle);
}

static bool resolveLuaViaBgmiOffsets(uintptr_t base, const std::string &path) {
    if (base == 0) {
        return false;
    }
    g_luaL_loadbufferx = reinterpret_cast<luaL_loadbufferx_t>(base + kBgmi44_LoadBufferX);
    g_lua_pcallk = reinterpret_cast<lua_pcallk_t>(base + kBgmi44_PCallK);
    g_lua_tostring = reinterpret_cast<lua_tostring_t>(base + kBgmi44_ToString);
    g_lua_settop = reinterpret_cast<lua_settop_t>(base + kBgmi44_Settop);
    g_hook_target = reinterpret_cast<void *>(base + kBgmi44_PCallK);
    resolveLuaHelpersFromUe4(base, path);
    LOGI("lua api via offsets base=%p load=%p pcallk=%p getglobal=%p (no UE4 bytes patched)",
         reinterpret_cast<void *>(base),
         reinterpret_cast<void *>(g_luaL_loadbufferx),
         g_hook_target,
         reinterpret_cast<void *>(g_lua_getglobal));
    return g_luaL_loadbufferx != nullptr && g_lua_pcallk != nullptr;
}

static bool resolveLuaApi() {
    resetLuaApi();

    PhdrSearch ue4{};
    findUe4Module(&ue4);
    if (ue4.base != 0 && resolveLuaViaBgmiOffsets(ue4.base, ue4.path)) {
        return true;
    }

    static int s_failLog;
    if ((++s_failLog % 20) == 1) {
        LOGW("resolveLuaApi failed ue4_base=%p", reinterpret_cast<void *>(ue4.base));
    }
    return false;
}

static bool luaGlobalBool(void *L, const char *name) {
    if (g_lua_getglobal == nullptr || g_lua_type == nullptr
        || g_lua_toboolean == nullptr || g_lua_settop == nullptr) {
        return false;
    }
    g_lua_getglobal(L, name);
    const bool value = g_lua_type(L, -1) == kLuaTBoolean && g_lua_toboolean(L, -1) != 0;
    g_lua_settop(L, -2);
    return value;
}

static bool patchWasApplied(void *L, int attempt) {
    if (L == nullptr) {
        return false;
    }
    if (g_lua_getglobal != nullptr && g_lua_type != nullptr
        && g_lua_toboolean != nullptr && g_lua_settop != nullptr) {
        return luaGlobalBool(L, "__GUEST_LOGIN_PATCHED")
               || luaGlobalBool(L, "__SKIN_PATCHED");
    }
    return attempt >= 40;
}

static std::string readFile(const char *path) {
    std::ifstream in(path, std::ios::binary);
    if (!in) {
        return {};
    }
    return std::string((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
}

static bool execLuaChunk(void *L) {
    if (g_lua_pcallk == nullptr) {
        return false;
    }
    const int rc = g_lua_pcallk(L, 0, 0, 0, 0, nullptr);
    if (rc != 0) {
        if (g_lua_tostring != nullptr) {
            const char *err = g_lua_tostring(L, -1);
            if (err != nullptr) {
                LOGW("lua exec error: %s", err);
                appendProbeLog(std::string("lua exec error: ") + err);
            }
        }
        if (g_lua_settop != nullptr) {
            g_lua_settop(L, -2);
        }
        return false;
    }
    return true;
}

static bool runLua(void *L, const char *source, const char *chunkName) {
    if (L == nullptr || source == nullptr || g_luaL_loadbufferx == nullptr) {
        return false;
    }
    const size_t len = std::strlen(source);
    if (g_luaL_loadbufferx(L, source, len, chunkName != nullptr ? chunkName : "=bgmi_lua", nullptr) != 0) {
        if (g_lua_tostring != nullptr) {
            const char *err = g_lua_tostring(L, -1);
            if (err != nullptr) {
                LOGW("lua load error: %s", err);
                appendProbeLog(std::string("lua load error: ") + err);
            }
        }
        if (g_lua_settop != nullptr) {
            g_lua_settop(L, -2);
        }
        return false;
    }
    return execLuaChunk(L);
}

static void runActorProbe(void *L) {
    if (L == nullptr || g_skin_done.load(std::memory_order_relaxed)) {
        return;
    }
    const int n = g_probe_runs.fetch_add(1, std::memory_order_relaxed);
    if (n > 2) {
        return;
    }

    const std::string path = probeLogPath();
    std::string script = "pcall(function()\n"
                         "  local path = [[";
    script += path;
    script += R"(]]
  local f = io.open(path, 'a')
  if not f then return end
  local function w(...)
    local t = {}
    for i = 1, select('#', ...) do t[#t + 1] = tostring(select(i, ...)) end
    f:write(table.concat(t, ' ') .. '\n')
  end
  w('=== ACTOR PROBE', os.date('%H:%M:%S'))
  w('HUD', slua_GameFrontendHUD ~= nil)
  local pc = nil
  if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
    pc = slua_GameFrontendHUD:GetPlayerController()
  end
  w('PC', pc ~= nil, pc and slua.isValid(pc))
  if _G.ReadConfigFile then pcall(_G.ReadConfigFile, true) end
  local ch = nil
  if _G.getSkinPlayerCharacter then
    ch = _G.getSkinPlayerCharacter()
  elseif pc and pc.GetPlayerCharacterSafety then
    ch = pc:GetPlayerCharacterSafety()
  end
  w('CHAR', ch ~= nil, ch and slua.isValid(ch))
  local pawn = nil
  if pc then
  pcall(function()
    if pc.GetPawn then pawn = pc:GetPawn() end
    if (not pawn or not slua.isValid(pawn)) and pc.K2_GetPawn then pawn = pc:K2_GetPawn() end
    if (not pawn or not slua.isValid(pawn)) and pc.AcknowledgedPawn then pawn = pc.AcknowledgedPawn end
  end)
  end
  w('PAWN', pawn ~= nil, pawn and slua.isValid(pawn))
  local nt = 0
  if _G.collectOutfitTargets then
    pcall(function() nt = #_G.collectOutfitTargets() end)
  end
  w('OUTFIT_TARGETS', nt)
  if ch and slua.isValid(ch) then
    local wpn = ch:GetCurrentWeapon()
    w('WEAPON', wpn ~= nil, wpn and slua.isValid(wpn))
    if wpn and slua.isValid(wpn) then
      pcall(function()
        local id = wpn:GetItemDefineID().TypeSpecificID
        local skin = slua.IndexReference(wpn.synData:Get(7), 'defineID').TypeSpecificID
        w('GUN defineId=' .. tostring(id), 'slotSkin=' .. tostring(skin))
      end)
    end
  end
  local n = 0
  if _G.WeaponSkinIndex then for _ in pairs(_G.WeaponSkinIndex) do n = n + 1 end end
  w('WeaponSkinIndex', n, 'timers=' .. tostring(_G.__SKIN_TIMERS_STARTED))
  w('SHIRT', _G.SuitSkin, 'PET', _G.PetSkin, 'Theme', _G.TargetLobbyThemeID)
  if _G.GameAvatarHandlerplayers then pcall(_G.GameAvatarHandlerplayers) end
  w('after outfit apply')
  if _G.collectOutfitTargets then
    pcall(function() nt = #_G.collectOutfitTargets() end)
  end
  w('OUTFIT_TARGETS_AFTER', nt)
  if _G.getSkinPlayerCharacter then
    ch = _G.getSkinPlayerCharacter()
  end
  w('CHAR_AFTER', ch ~= nil, ch and slua.isValid(ch))
  f:close()
end)
)";
    runLua(L, script.c_str(), "=actor_probe");
    appendProbeLog("probe run #" + std::to_string(n + 1));
}

static bool runGuestLoginLua(void *L) {
    if (runLua(L, kLuaScript, "=guest_login")) {
        return true;
    }
    const std::string luaPath = guestLuaPathBesideHook();
    const std::string fileScript = luaPath.empty() ? std::string() : readFile(luaPath.c_str());
    return !fileScript.empty() && runLua(L, fileScript.c_str(), "=guest_login_file");
}

static bool runSkinModLua(void *L) {
    const std::string hookDir = guestHookFilesDir();
    const std::string probePath = probeLogPath();
    if (!hookDir.empty()) {
        std::string setPaths = "_G.__SKIN_PROBE_PATH = [[";
        setPaths += probePath;
        setPaths += "]]\n_G.__SKIN_CONFIG_BASE = [[";
        setPaths += hookDir;
        setPaths += "]]";
        runLua(L, setPaths.c_str(), "=skin_paths");
    }
    const std::string luaPath = skinLuaPathBesideHook();
    const std::string fileScript = luaPath.empty() ? std::string() : readFile(luaPath.c_str());
    if (fileScript.empty()) {
        LOGW("skin_mod_bgmi.lua missing beside hook path=%s", luaPath.c_str());
        appendProbeLog("skin_mod_bgmi.lua missing path=" + luaPath);
        return false;
    }
    LOGI("loading skin lua bytes=%zu path=%s", fileScript.size(), luaPath.c_str());
    appendProbeLog("loading skin lua bytes=" + std::to_string(fileScript.size()));
    const bool ok = runLua(L, fileScript.c_str(), "=skin_mod_file");
    if (!ok) {
        appendProbeLog("skin_mod_bgmi.lua run failed");
    }
    return ok;
}

static bool guestPatchLooksApplied(void *L) {
    return luaGlobalBool(L, "__GUEST_LOGIN_PATCHED");
}

static bool skinPatchLooksApplied(void *L) {
    if (luaGlobalBool(L, "__SKIN_TIMERS_STARTED") || luaGlobalBool(L, "__SKIN_PATCHED")) {
        return true;
    }
    return g_skin_injected.load(std::memory_order_relaxed)
           && g_attempts.load(std::memory_order_relaxed) >= 8;
}

static bool gameModLooksApplied(void *L) {
    return luaGlobalBool(L, "__BGMI_GAME_MOD_PATCHED")
        || luaGlobalBool(L, "__BGMI_FEATURES_ACTIVE")
        || luaGlobalBool(L, "__BGMI_BODY_RAN");
}

static bool runGameModLua(void *L) {
    const std::string hookDir = guestHookFilesDir();
    if (!hookDir.empty()) {
        std::string setPaths = "_G.__GAMEMOD_CONFIG_BASE = [[";
        setPaths += hookDir;
        setPaths += "]]";
        runLua(L, setPaths.c_str(), "=gamemod_paths");
    }
    const std::string luaPath = gameModLuaPathBesideHook();
    const std::string fileScript = luaPath.empty() ? std::string() : readFile(luaPath.c_str());
    if (fileScript.empty()) {
        const int n = g_gamemod_missing_logs.fetch_add(1, std::memory_order_relaxed);
        if (n < 5 || (n % 20) == 0) {
            LOGW("bgmi_game_mod.lua missing beside hook (try %d) path=%s",
                 n + 1, luaPath.c_str());
        }
        return false;
    }
    return runLua(L, fileScript.c_str(), "=gamemod_file");
}

static void tryApplyGuestPatch(void *L) {
    if (g_guest_done.load(std::memory_order_relaxed)
        && g_skin_done.load(std::memory_order_relaxed)
        && g_gamemod_done.load(std::memory_order_relaxed)) {
        return;
    }
    const int attempt = g_attempts.fetch_add(1, std::memory_order_relaxed);
    if (attempt >= 200) {
        return;
    }
    if (attempt > 0 && (attempt % 4) != 0) {
        return;
    }

    if (!g_guest_done.load(std::memory_order_relaxed)) {
        if (runGuestLoginLua(L)
            && (guestPatchLooksApplied(L) || g_lua_getglobal == nullptr || attempt >= 5)) {
            g_guest_done.store(true, std::memory_order_relaxed);
            LOGI("guest login lua applied (attempt %d)", attempt + 1);
        }
        return;
    }

    if (!g_skin_done.load(std::memory_order_relaxed)) {
        if (skinPatchLooksApplied(L)) {
            g_skin_done.store(true, std::memory_order_relaxed);
            LOGI("skin mod already active (attempt %d)", attempt + 1);
        } else if (!g_skin_injected.load(std::memory_order_relaxed)) {
            if (runSkinModLua(L)) {
                g_skin_injected.store(true, std::memory_order_relaxed);
                // Lua start() is deferred inside skin_mod — don't block trap on timers/hooks.
                g_skin_done.store(true, std::memory_order_relaxed);
                LOGI("skin mod lua queued (attempt %d)", attempt + 1);
            }
        }
        return;
    }

    if (kGameModEnabled && !g_gamemod_done.load(std::memory_order_relaxed)) {
        if (gameModLooksApplied(L)) {
            g_gamemod_done.store(true, std::memory_order_relaxed);
            LOGI("game mod ready (attempt %d)", attempt + 1);
        } else if (!g_gamemod_injected.load(std::memory_order_relaxed)) {
            if (runGameModLua(L)) {
                g_gamemod_injected.store(true, std::memory_order_relaxed);
                runLua(L,
                       "pcall(function()"
                       " if _G.ensureGameModTimers then _G.ensureGameModTimers() end"
                       " end)",
                       "=gamemod_timers");
                // Lua timers own retries — stop blocking allPatchesDone on active flag.
                g_gamemod_done.store(true, std::memory_order_relaxed);
                LOGI("game mod lua executed (attempt %d)", attempt + 1);
            }
        } else {
            runLua(L,
                   "pcall(function()"
                   " if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end"
                   " if _G.ensureGameModTimers then _G.ensureGameModTimers() end"
                   " end)",
                   "=gamemod_retry");
        }
        if (!g_gamemod_done.load(std::memory_order_relaxed)
            && g_gamemod_injected.load(std::memory_order_relaxed)
            && gameModLooksApplied(L)) {
            g_gamemod_done.store(true, std::memory_order_relaxed);
            LOGI("game mod active (attempt %d)", attempt + 1);
        }
    }
}

static bool allPatchesDone() {
    return g_guest_done.load(std::memory_order_relaxed)
           && g_skin_done.load(std::memory_order_relaxed)
           && g_gamemod_done.load(std::memory_order_relaxed);
}

#if defined(__aarch64__)

static bool armExecTrap();
static void disarmExecTrap();
static void permanentShutdownTrap();

extern "C" int callPcallkAndInject(void *L, int nargs, int nresults, int errfunc, long ctx, void *k) {
    if (g_hook_depth.fetch_add(1, std::memory_order_acq_rel) > 0) {
        g_hook_depth.fetch_sub(1, std::memory_order_acq_rel);
        return g_lua_pcallk != nullptr
                ? g_lua_pcallk(L, nargs, nresults, errfunc, ctx, k) : -1;
    }

    static int s_hits = 0;
    const int hit = ++s_hits;
    if (hit == 1 || (hit % 40) == 0) {
        LOGI("exec trap hit #%d L=%p (UE4 bytes untouched)", hit, L);
    }

    int result = -1;
    if (g_lua_pcallk != nullptr) {
        result = g_lua_pcallk(L, nargs, nresults, errfunc, ctx, k);
    }

    if (hit >= 2 && !allPatchesDone()) {
        tryApplyGuestPatch(L);
        if (allPatchesDone()) {
            LOGI("all lua patches applied (patchless trap)");
            permanentShutdownTrap();
        }
    }

    if (!allPatchesDone() && !g_trap_off.load(std::memory_order_relaxed)
            && g_hook_depth.load(std::memory_order_relaxed) == 0) {
        armExecTrap();
    }

    g_hook_depth.fetch_sub(1, std::memory_order_acq_rel);
    return result;
}

__attribute__((naked)) static void trapResumeBridge(void) {
    __asm__ volatile(
        "stp x29, x30, [sp, #-16]!\n"
        "mov x29, sp\n"
        "bl callPcallkAndInject\n"
        "ldp x29, x30, [sp], #16\n"
        "ret\n"
    );
}

static bool setTrapPageExec(bool exec) {
    if (g_trap_page == nullptr || g_trap_page_size == 0) {
        return false;
    }
    const int prot = exec ? (PROT_READ | PROT_EXEC) : PROT_READ;
    return mprotect(g_trap_page, g_trap_page_size, prot) == 0;
}

static void disarmExecTrap() {
    if (!g_trap_armed.load(std::memory_order_relaxed)) {
        return;
    }
    if (setTrapPageExec(true)) {
        g_trap_armed.store(false, std::memory_order_release);
    }
}

static bool armExecTrap() {
    if (g_trap_off.load(std::memory_order_relaxed)
            || !g_trap_ready.load(std::memory_order_relaxed)
            || g_trap_page == nullptr
            || allPatchesDone()) {
        return false;
    }
    if (g_trap_armed.load(std::memory_order_relaxed)) {
        return true;
    }
    if (!setTrapPageExec(false)) {
        LOGW("exec trap arm failed page=%p", g_trap_page);
        return false;
    }
    g_trap_armed.store(true, std::memory_order_release);
    static bool s_arm_logged = false;
    if (!s_arm_logged) {
        s_arm_logged = true;
        LOGI("exec trap armed page=%p (PROT_READ only)", g_trap_page);
    }
    return true;
}

static void permanentShutdownTrap() {
    if (g_trap_off.exchange(true, std::memory_order_acq_rel)) {
        return;
    }
    disarmExecTrap();
    LOGI("lua exec trap off — UE4 bytes never modified");
}

static void forwardSegv(int sig, siginfo_t *info, void *ctx) {
    if ((g_old_segv.sa_flags & SA_SIGINFO) != 0 && g_old_segv.sa_sigaction != nullptr) {
        g_old_segv.sa_sigaction(sig, info, ctx);
        return;
    }
    if (g_old_segv.sa_handler != nullptr && g_old_segv.sa_handler != SIG_DFL
        && g_old_segv.sa_handler != SIG_IGN) {
        g_old_segv.sa_handler(sig);
    }
}

static void forwardSegv(int sig, siginfo_t *info, void *ctx);

static void trapHandler(int sig, siginfo_t *info, void *ctx_void) {
    auto *ctx = static_cast<ucontext_t *>(ctx_void);
    mcontext_t &mctx = ctx->uc_mcontext;
    const uintptr_t pc = mctx.pc;
    const uintptr_t pageStart = reinterpret_cast<uintptr_t>(g_trap_page);
    const uintptr_t pageEnd = pageStart + g_trap_page_size;
    const bool onTrapPage = g_trap_armed.load(std::memory_order_acquire)
            && pc >= pageStart && pc < pageEnd;

    if (!onTrapPage) {
        forwardSegv(sig, info, ctx_void);
        return;
    }

    if (pc != reinterpret_cast<uintptr_t>(g_hook_target)) {
        // Other code on the same 4K page — restore exec and retry; never forward to CrashKit.
        disarmExecTrap();
        return;
    }

    disarmExecTrap();
    // Resume on the faulting thread's normal stack (uc_mcontext.sp), not the signal alt stack.
    mctx.pc = reinterpret_cast<uintptr_t>(trapResumeBridge);
}

static bool installSharedSegvHandlerLocked() {
    if (g_segv_handler_installed.load(std::memory_order_relaxed)) {
        return true;
    }
    stack_t ss {};
    ss.ss_sp = g_alt_stack;
    ss.ss_size = sizeof(g_alt_stack);
    ss.ss_flags = 0;
    sigaltstack(&ss, nullptr);

    struct sigaction sa {};
    sa.sa_sigaction = trapHandler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
    sigemptyset(&sa.sa_mask);
    sigaddset(&sa.sa_mask, SIGSEGV);
    if (sigaction(SIGSEGV, &sa, &g_old_segv) != 0) {
        LOGW("sigaction SIGSEGV failed");
        return false;
    }
    g_segv_handler_installed.store(true, std::memory_order_release);
    LOGI("shared SIGSEGV handler installed (lua exec trap)");
    return true;
}

extern "C" bool guestLoginEnsureSegvHandler() {
    std::lock_guard<std::mutex> lock(g_mutex);
    return installSharedSegvHandlerLocked();
}

static bool installExecTrap() {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_trap_ready.load(std::memory_order_relaxed)) {
        return armExecTrap();
    }
    if (!resolveLuaApi() || g_hook_target == nullptr) {
        return false;
    }

    const long page = sysconf(_SC_PAGESIZE);
    const uintptr_t target = reinterpret_cast<uintptr_t>(g_hook_target);
    const uintptr_t start = target & ~(static_cast<uintptr_t>(page) - 1);
    g_trap_page = reinterpret_cast<void *>(start);
    g_trap_page_size = static_cast<size_t>(page);

    if (!installSharedSegvHandlerLocked()) {
        return false;
    }

    g_trap_ready.store(true, std::memory_order_release);
    LOGI("exec trap ready page=%p fn=%p (UE4 .text bytes unchanged)",
         g_trap_page, g_hook_target);
    return true;
}

#else

static bool installExecTrap() {
    return false;
}

static void permanentShutdownTrap() {}

extern "C" bool guestLoginEnsureSegvHandler() {
    return false;
}

#endif

static std::atomic<bool> g_pattern_scan_done{false};

static void worker() {
    LOGI("guest login worker started (patchless — no UE4 byte patch)");
    for (int i = 0; i < 600 && !g_trap_ready.load(std::memory_order_relaxed); ++i) {
        if (isUe4Loaded()) {
            if (!g_pattern_scan_done.exchange(true, std::memory_order_relaxed)) {
                std::thread([]() {
                    ue4_pattern_finder_scan_and_log();
                }).detach();
            }
            // Let CrashKit finish init before first intentional SIGSEGV.
            if (i < 40) {
                if (i % 10 == 0) {
                    LOGI("deferring exec trap until CrashKit ready (iter=%d)", i);
                }
                usleep(500 * 1000);
                continue;
            }
            if (installExecTrap()) {
                break;
            }
        }
        if (i % 20 == 0) {
            PhdrSearch found{};
            findUe4Module(&found);
            LOGI("waiting UE4 iter=%d base=%p", i, reinterpret_cast<void *>(found.base));
        }
        usleep(500 * 1000);
    }

    for (int i = 0; i < 600 && !allPatchesDone()
            && !g_trap_off.load(std::memory_order_relaxed); ++i) {
        if (g_trap_ready.load(std::memory_order_relaxed)
                && g_hook_depth.load(std::memory_order_relaxed) == 0) {
            armExecTrap();
        }
        usleep(200 * 1000);
    }

    if (allPatchesDone()) {
        permanentShutdownTrap();
    } else if (!g_trap_ready.load(std::memory_order_relaxed)) {
        LOGW("exec trap timeout (ue4=%d)", isUe4Loaded() ? 1 : 0);
    } else {
        LOGW("pending guest=%d skin=%d mod=%d",
             g_guest_done.load() ? 1 : 0,
             g_skin_done.load() ? 1 : 0,
             g_gamemod_done.load() ? 1 : 0);
        permanentShutdownTrap();
    }
}

} // namespace

static void delayedWorker() {
    usleep(3 * 1000 * 1000);
    worker();
}

__attribute__((constructor)) static void onGuestLoginHookLoad() {
    blaze_log_inject_and_game_maps("guest-hook");
    LOGI("guest hook loaded pid=%d (guest/skin/gamemod lua only)", getpid());
    std::thread(delayedWorker).detach();
}
