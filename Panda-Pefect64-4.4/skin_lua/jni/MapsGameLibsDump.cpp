#include "MapsGameLibsDump.h"

#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>

#include <cstdio>
#include <cstring>
#include <string>
#include <unordered_set>
#include <unistd.h>

namespace {

constexpr const char *kInjectTag = "INJECT";
constexpr const char *kMapsTag = "MAPS_DUMP";

static void ensure_maps_hide_installed() {
    static pthread_once_t once = PTHREAD_ONCE_INIT;
    pthread_once(&once, []() {
        using install_fn = void (*)();
        install_fn install = nullptr;
        void *bb = dlopen("libanubis.so", RTLD_NOW | RTLD_NOLOAD);
        if (bb == nullptr) {
            bb = dlopen("libanubis.so", RTLD_NOW);
        }
        if (bb != nullptr) {
            install = reinterpret_cast<install_fn>(dlsym(bb, "anubis_install_maps_hide"));
        }
        if (install != nullptr) {
            install();
            __android_log_print(ANDROID_LOG_INFO, kMapsTag, "maps hide via libanubis ok");
        } else {
            __android_log_print(ANDROID_LOG_WARN, kMapsTag,
                                "maps hide unavailable (libanubis missing or symbol stripped)");
        }
    });
}

static bool lineHasGameSo(const char *line) {
    if (line == nullptr || line[0] == '\0') {
        return false;
    }
    if (strstr(line, "guestloginhook") != nullptr || strstr(line, "blackbox") != nullptr
            || strstr(line, "anubis") != nullptr) {
        return true;
    }
    if (strstr(line, "com.pubg.imobile") != nullptr && strstr(line, ".so") != nullptr) {
        return true;
    }
    if (strstr(line, "anogsblocker") != nullptr || strstr(line, "hdmpveblocker") != nullptr) {
        return true;
    }
    return false;
}

static std::string extractMapPath(const char *line) {
    const char *slash = strchr(line, '/');
    if (slash == nullptr) {
        return {};
    }
    std::string path = slash;
    while (!path.empty() && (path.back() == '\r' || path.back() == '\n' || path.back() == ' ')) {
        path.pop_back();
    }
    return path;
}

static void dumpGameLibsMaps() {
    FILE *maps = fopen("/proc/self/maps", "r");
    if (maps == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, kMapsTag, "fopen /proc/self/maps failed");
        return;
    }

    std::unordered_set<std::string> seen;
    char buf[1024];
    int count = 0;
    while (fgets(buf, sizeof(buf), maps) != nullptr) {
        if (!lineHasGameSo(buf)) {
            continue;
        }
        const std::string path = extractMapPath(buf);
        if (path.empty() || !seen.insert(path).second) {
            continue;
        }
        __android_log_print(ANDROID_LOG_ERROR, kMapsTag, "FOUND: %s", buf);
        ++count;
    }
    fclose(maps);
    __android_log_print(ANDROID_LOG_ERROR, kMapsTag, "TOTAL game libs=%d pid=%d", count, getpid());
}

} // namespace

void blaze_log_inject_and_game_maps(const char *hook_name) {
    const char *label = (hook_name != nullptr && hook_name[0] != '\0')
                                ? hook_name
                                : "guest-hook";
    __android_log_print(ANDROID_LOG_ERROR, kInjectTag, "NATIVE CODE OK: %s (in-process)",
                        label);
    dumpGameLibsMaps();
}
