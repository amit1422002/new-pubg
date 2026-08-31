/**
 * Bullet track — in-process shellcode at libUE4.so offsets (sock64 IPC).
 * Patches only while firing; restores original bytes immediately when idle.
 */
#include "BulletTrackShared.h"

#include <android/log.h>
#include <atomic>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <link.h>
#include <mutex>
#include <string>
#include <sys/mman.h>
#include <thread>
#include <unistd.h>

#define BT_TAG "bullet-track"
#define BT_LOGI(...) __android_log_print(ANDROID_LOG_INFO, BT_TAG, __VA_ARGS__)
#define BT_LOGW(...) __android_log_print(ANDROID_LOG_WARN, BT_TAG, __VA_ARGS__)

namespace {

static constexpr uintptr_t kPsGlobalTargetFunc = 0x69A3B1CUL;
static constexpr uintptr_t kPsGlobalPatch1 = 0x69A3500UL;
static constexpr uintptr_t kShellAim3DPos = 0x20UL;

static const uint32_t kShellCode[11] = {
        0x10000100, 0xBD400003, 0xBD400404, 0xBD400805, 0x4EA31C60,
        0x4EA41C81, 0x4EA51CA2, 0xD65F03C0, 0x00000000, 0x00000000, 0x00000000};
static const uint32_t kBypassPatch[2] = {0x910003FF, 0x8A010021};

static uintptr_t g_ue4Base = 0;
static void *g_btPage = nullptr;
static size_t g_btPageSize = 0;
static uintptr_t g_btAimAddr = 0;
static uintptr_t g_btBypassAddr = 0;

static uint32_t g_origShell[11] = {};
static uint32_t g_origBypass[2] = {};
static bool g_origSaved = false;
static bool g_patched = false;

static std::mutex g_btMutex;
static std::atomic<bool> g_btWorkerOn{true};
static std::atomic<bool> g_btReady{false};
static uint32_t g_lastIpcSeq = 0;

static uintptr_t resolveUe4Base() {
    struct PhdrSearch {
        uintptr_t base;
    } found{};
    auto cb = +[](struct dl_phdr_info *info, size_t, void *data) -> int {
        auto *out = static_cast<PhdrSearch *>(data);
        if (info->dlpi_name == nullptr || info->dlpi_name[0] == '\0') {
            return 0;
        }
        if (strstr(info->dlpi_name, "libUE4.so") == nullptr) {
            return 0;
        }
        out->base = static_cast<uintptr_t>(info->dlpi_addr);
        return 1;
    };
    dl_iterate_phdr(cb, &found);
    return found.base;
}

static bool ensureBtPageWritable() {
    if (g_btPage == nullptr || g_btPageSize == 0) {
        return false;
    }
    return mprotect(g_btPage, g_btPageSize, PROT_READ | PROT_WRITE | PROT_EXEC) == 0;
}

static bool saveOriginals() {
    if (g_origSaved) {
        return true;
    }
    if (g_btAimAddr == 0 || g_btBypassAddr == 0) {
        return false;
    }
    if (!ensureBtPageWritable()) {
        BT_LOGW("save originals mprotect failed errno=%d", errno);
        return false;
    }
    memcpy(g_origShell, reinterpret_cast<void *>(g_btAimAddr), sizeof(g_origShell));
    memcpy(g_origBypass, reinterpret_cast<void *>(g_btBypassAddr), sizeof(g_origBypass));
    g_origSaved = true;
    BT_LOGI("saved originals aim=%p bypass=%p w0=0x%08x",
            reinterpret_cast<void *>(g_btAimAddr),
            reinterpret_cast<void *>(g_btBypassAddr),
            g_origShell[0]);
    return true;
}

static bool initBtSites() {
    if (g_btReady.load(std::memory_order_relaxed)) {
        return true;
    }
    g_ue4Base = resolveUe4Base();
    if (g_ue4Base == 0) {
        return false;
    }
    const long psz = sysconf(_SC_PAGESIZE);
    if (psz < 4096) {
        return false;
    }
    g_btAimAddr = g_ue4Base + kPsGlobalTargetFunc;
    g_btBypassAddr = g_ue4Base + kPsGlobalPatch1;
    const uintptr_t pageStart = g_btAimAddr & ~(static_cast<uintptr_t>(psz) - 1u);
    g_btPage = reinterpret_cast<void *>(pageStart);
    g_btPageSize = static_cast<size_t>(psz);

    if (!saveOriginals()) {
        return false;
    }

    g_btReady.store(true, std::memory_order_release);
    BT_LOGI("ready ue4=%p aim=%p bypass=%p", reinterpret_cast<void *>(g_ue4Base),
            reinterpret_cast<void *>(g_btAimAddr), reinterpret_cast<void *>(g_btBypassAddr));
    return true;
}

static bool isAimSiteOriginal() {
    const auto *cur = reinterpret_cast<const uint32_t *>(g_btAimAddr);
    return cur[0] == g_origShell[0];
}

static void writeAimCoords(float x, float y, float z) {
    auto *slot = reinterpret_cast<float *>(g_btAimAddr + kShellAim3DPos);
    slot[0] = x;
    slot[1] = y;
    slot[2] = z;
}

static bool applyPatch(float x, float y, float z) {
    if (!ensureBtPageWritable()) {
        BT_LOGW("apply mprotect failed errno=%d", errno);
        return false;
    }

    if (!g_patched) {
        if (!isAimSiteOriginal()) {
            BT_LOGW("aim site not original (0x%08x)", *reinterpret_cast<uint32_t *>(g_btAimAddr));
            return false;
        }
        uint32_t shell[11];
        memcpy(shell, kShellCode, sizeof(shell));
        memcpy(reinterpret_cast<uint8_t *>(shell) + kShellAim3DPos, &x, sizeof(float));
        memcpy(reinterpret_cast<uint8_t *>(shell) + kShellAim3DPos + 4, &y, sizeof(float));
        memcpy(reinterpret_cast<uint8_t *>(shell) + kShellAim3DPos + 8, &z, sizeof(float));
        memcpy(reinterpret_cast<void *>(g_btAimAddr), shell, sizeof(shell));
        memcpy(reinterpret_cast<void *>(g_btBypassAddr), kBypassPatch, sizeof(kBypassPatch));
        g_patched = true;
        BT_LOGI("shellcode armed pos=(%.1f,%.1f,%.1f)", x, y, z);
    } else {
        writeAimCoords(x, y, z);
    }
    return true;
}

static void restorePatch() {
    if (!g_patched || !g_origSaved) {
        return;
    }
    if (!ensureBtPageWritable()) {
        return;
    }
    memcpy(reinterpret_cast<void *>(g_btAimAddr), g_origShell, sizeof(g_origShell));
    memcpy(reinterpret_cast<void *>(g_btBypassAddr), g_origBypass, sizeof(g_origBypass));
    g_patched = false;
}

static bool readIpcFromPath(const char *path, BulletTrackIpc &out) {
    return path != nullptr && path[0] != '\0' && bulletTrackIpcReadFile(path, out);
}

static bool readIpc(BulletTrackIpc &out) {
    static const char *kVfsPaths[] = {
            "/data/user/0/com.blazehealth.tracker/files/.vfs/data/user/0/com.pubg.imobile/files/bullet_track.ipc",
            "/data/data/com.blazehealth.tracker/files/.vfs/data/user/0/com.pubg.imobile/files/bullet_track.ipc",
            nullptr
    };
    for (int i = 0; kVfsPaths[i] != nullptr; ++i) {
        if (readIpcFromPath(kVfsPaths[i], out)) {
            return true;
        }
    }

    char anchor;
    Dl_info info{};
    if (dladdr(&anchor, &info) && info.dli_fname != nullptr) {
        std::string path = info.dli_fname;
        const size_t slash = path.rfind('/');
        if (slash != std::string::npos) {
            path = path.substr(0, slash + 1) + "bullet_track.ipc";
            if (readIpcFromPath(path.c_str(), out)) {
                return true;
            }
        }
    }

    char tmp[64];
    bulletTrackIpcPath(tmp, sizeof tmp, getpid());
    return readIpcFromPath(tmp, out);
}

static void tickBulletTrack() {
    if (!initBtSites()) {
        return;
    }

    BulletTrackIpc ipc{};
    if (!readIpc(ipc)) {
        static uint32_t s_miss = 0;
        if ((++s_miss % 500u) == 1u) {
            BT_LOGW("waiting for bullet_track.ipc");
        }
        std::lock_guard<std::mutex> lock(g_btMutex);
        restorePatch();
        return;
    }

    if (ipc.seq != g_lastIpcSeq) {
        g_lastIpcSeq = ipc.seq;
        if (ipc.enabled) {
            BT_LOGI("IPC seq=%u trig=%u tgt=%u pos=(%.1f,%.1f,%.1f)",
                    ipc.seq, ipc.trigger, ipc.hasTarget, ipc.tx, ipc.ty, ipc.tz);
        }
    }

    std::lock_guard<std::mutex> lock(g_btMutex);

    if (!ipc.enabled) {
        restorePatch();
        return;
    }

    const bool active = ipc.trigger != 0 && ipc.hasTarget != 0;
    if (!active) {
        restorePatch();
        return;
    }

    applyPatch(ipc.tx, ipc.ty, ipc.tz);
}

static void bulletTrackWorker() {
    BT_LOGI("bullet track worker pid=%d", getpid());
    usleep(3 * 1000 * 1000);
    while (g_btWorkerOn.load(std::memory_order_relaxed)) {
        tickBulletTrack();
        usleep(2000);
    }
    std::lock_guard<std::mutex> lock(g_btMutex);
    restorePatch();
}

} // namespace

void bulletTrackInProcessStart() {
    static std::once_flag once;
    std::call_once(once, []() {
        std::thread(bulletTrackWorker).detach();
    });
}

extern "C" bool bulletTrackNeedsSegvHandler() {
    return false;
}

extern "C" bool bulletTrackOnSegv(void *) {
    return false;
}
