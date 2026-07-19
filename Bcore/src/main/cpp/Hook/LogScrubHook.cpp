#include "LogScrubHook.h"

#include <android/log.h>
#include <Dobby/dobby.h>

#include <cstdarg>
#include <cstring>
#include <dlfcn.h>
#include <mutex>
#include <string>
#include <vector>

namespace {

std::mutex g_mutex;
bool g_enabled = false;
bool g_hooks_installed = false;
std::string g_hostPkg;
std::string g_guestPkg;

typedef int (*log_print_fn)(int, const char *, const char *, ...);
typedef int (*log_vprint_fn)(int, const char *, const char *, va_list);
typedef int (*log_buf_write_fn)(int, const char *, const char *, const char *);

static log_print_fn orig_log_print = nullptr;
static log_vprint_fn orig_log_vprint = nullptr;
static log_buf_write_fn orig_log_buf_write = nullptr;

static void replaceAll(std::string &s, const std::string &from, const std::string &to) {
    if (from.empty()) {
        return;
    }
    size_t pos = 0;
    while ((pos = s.find(from, pos)) != std::string::npos) {
        s.replace(pos, from.length(), to);
        pos += to.length();
    }
}

static std::string scrub(const char *msg) {
    if (msg == nullptr) {
        return {};
    }
    std::string out(msg);
    if (!g_hostPkg.empty() && !g_guestPkg.empty()) {
        replaceAll(out, g_hostPkg, g_guestPkg);
    }
    replaceAll(out, "/.vfs/", "/");
    replaceAll(out, "files/.vfs/data", "data");
    if (!g_guestPkg.empty()) {
        const std::string leak = "/files/data/app/" + g_guestPkg;
        const std::string fake = "/data/app/" + g_guestPkg;
        replaceAll(out, leak, fake);
        const std::string nested = "/data/user/0/" + g_guestPkg + "/data/app/" + g_guestPkg;
        replaceAll(out, nested, fake);
        const std::string nestedStorage = "/data/user/0/" + g_guestPkg + "/storage/emulated/";
        replaceAll(out, nestedStorage, "/storage/emulated/");
    }
    replaceAll(out, "/files/storage/emulated/", "/storage/emulated/");
    replaceAll(out, "com.anubis.loader.proxy.ProxyActivity", "android.app.Activity");
    replaceAll(out, "com.anubis.loader", "android.app");
    replaceAll(out, "libartpalette.so", "libc.so");
    replaceAll(out, "libguestloginhook.so", "libc.so");
    replaceAll(out, "libbcore.so", "libc.so");
    replaceAll(out, "top.niunaijun.blackbox", g_guestPkg.empty() ? "android" : g_guestPkg);
    replaceAll(out, "anubis", "android");
    replaceAll(out, "blackbox", "android");
    return out;
}

static bool isAuditLogTag(const char *tag) {
    return tag != nullptr
            && (strcmp(tag, "FARLIGHT_PATH") == 0 || strcmp(tag, "DELTA_PATH") == 0
                || strcmp(tag, "GUEST_AC_BYPASS") == 0 || strcmp(tag, "block-anogs") == 0
                || strcmp(tag, "ANOGS_PATCH") == 0 || strcmp(tag, "NERTC_PATCH") == 0
                || strcmp(tag, "PUBG_AYAN_F2") == 0
                || strcmp(tag, "HTPROTECT_FARLIGHT") == 0
                || strcmp(tag, "ANOGS_BYPASS_DFM") == 0);
}

static bool likelyNeedsScrub(const char *msg) {
    if (msg == nullptr) {
        return false;
    }
    return strstr(msg, "anubis") != nullptr || strstr(msg, "blackbox") != nullptr
            || strstr(msg, "bcore") != nullptr || strstr(msg, ".vfs") != nullptr
            || strstr(msg, "artpalette") != nullptr || strstr(msg, "guestloginhook") != nullptr
            || strstr(msg, "niunaijun") != nullptr || strstr(msg, "loader.proxy") != nullptr
            || strstr(msg, "/files/data/app/") != nullptr
            || strstr(msg, "/files/storage/emulated/") != nullptr;
}

static int fake_log_print(int prio, const char *tag, const char *fmt, ...) {
    if (orig_log_print == nullptr) {
        return 0;
    }
    va_list ap;
    va_start(ap, fmt);
  if (!g_enabled || isAuditLogTag(tag)) {
        int ret = orig_log_vprint != nullptr
                ? orig_log_vprint(prio, tag, fmt != nullptr ? fmt : "", ap)
                : 0;
        va_end(ap);
        return ret;
    }
    char buf[4096];
    vsnprintf(buf, sizeof(buf), fmt != nullptr ? fmt : "", ap);
    va_end(ap);
    if (!likelyNeedsScrub(buf)) {
        return orig_log_print(prio, tag, "%s", buf);
    }
    std::string cleaned = scrub(buf);
    return orig_log_print(prio, tag, "%s", cleaned.c_str());
}

static int fake_log_buf_write(int bufId, const char *prio, const char *tag, const char *msg) {
    if (orig_log_buf_write == nullptr) {
        return 0;
    }
    if (!g_enabled || msg == nullptr || isAuditLogTag(tag) || !likelyNeedsScrub(msg)) {
        return orig_log_buf_write(bufId, prio, tag, msg);
    }
    std::string cleaned = scrub(msg);
    return orig_log_buf_write(bufId, prio, tag, cleaned.c_str());
}

static bool hookSym(void *handle, const char *name, void *replace, void **orig) {
    void *sym = dlsym(handle, name);
    if (sym == nullptr || orig == nullptr) {
        return false;
    }
    return DobbyHook(sym, replace, orig) == 0;
}

static void ensureHooksInstalled() {
    if (g_hooks_installed) {
        return;
    }
    void *liblog = dlopen("liblog.so", RTLD_NOW);
    if (liblog == nullptr) {
        return;
    }
    orig_log_vprint = (log_vprint_fn) dlsym(liblog, "__android_log_vprint");
    hookSym(liblog, "__android_log_print", (void *) fake_log_print, (void **) &orig_log_print);
    hookSym(liblog, "__android_log_buf_write", (void *) fake_log_buf_write, (void **) &orig_log_buf_write);
    g_hooks_installed = true;
}

} // namespace

void LogScrubHook::setConfig(bool enabled, const char *hostPkg, const char *guestPkg) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_enabled = enabled;
    g_hostPkg = hostPkg != nullptr ? hostPkg : "";
    g_guestPkg = guestPkg != nullptr ? guestPkg : "";
    if (enabled) {
        ensureHooksInstalled();
    }
}

void LogScrubHook::init() {
    // Hooks installed lazily on setConfig(true) — avoids UE4 FPS hit when scrub is off.
}
