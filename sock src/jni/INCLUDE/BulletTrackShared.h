#pragma once

#include <cstdint>
#include <cstdio>
#include <ctime>
#include <sys/stat.h>

/** sock64 → in-game guestloginhook: bullet-track command (no external UE4 pwrite). */
struct BulletTrackIpc {
    uint32_t magic;
    uint32_t seq;
    uint8_t enabled;
    uint8_t trigger;
    uint8_t hasTarget;
    uint8_t pad;
    float tx;
    float ty;
    float tz;
};

static constexpr uint32_t kBulletTrackIpcMagic = 0x4B544242u; /* 'BTBK' */

static inline void bulletTrackIpcPath(char *out, size_t cap, int pid) {
    if (cap < 48 || pid < 1) {
        if (cap > 0) {
            out[0] = '\0';
        }
        return;
    }
    snprintf(out, cap, "/data/local/tmp/bgmi_bt_%d.bin", pid);
}

static inline void bulletTrackIpcWriteFile(const char *path, const BulletTrackIpc &ipc) {
    if (path == nullptr || path[0] == '\0') {
        return;
    }
    FILE *f = fopen(path, "wb");
    if (f == nullptr) {
        return;
    }
    fwrite(&ipc, sizeof(ipc), 1, f);
    fclose(f);
    chmod(path, 0666);
}

static inline uint64_t bulletTrackNowMs() {
    struct timespec ts {};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<uint64_t>(ts.tv_sec) * 1000ull +
           static_cast<uint64_t>(ts.tv_nsec / 1000000ull);
}

/**
 * BlackBox guest files/ — same dir as skin_mod_bgmi.lua beside the hook.
 * Writing 3 files every frame (~180 fops/sec) was both a hot-path bottleneck
 * and a constant-I/O detection signal. We now only touch disk when the command
 * actually changes, or on a 200 ms heartbeat, so an idle session is silent.
 */
static inline void bulletTrackIpcPublish(int pid, bool enabled, bool trigger, bool hasTarget,
                                         float tx, float ty, float tz, uint32_t &seq) {
    static uint8_t sLastEnabled = 0xFF, sLastTrigger = 0xFF, sLastHasTarget = 0xFF;
    static float sLastTx = 0.f, sLastTy = 0.f, sLastTz = 0.f;
    static uint64_t sLastWriteMs = 0;

    const uint8_t en = enabled ? 1u : 0u;
    const uint8_t tr = trigger ? 1u : 0u;
    const uint8_t ht = hasTarget ? 1u : 0u;
    const uint64_t nowMs = bulletTrackNowMs();

    const bool stateChanged = en != sLastEnabled || tr != sLastTrigger || ht != sLastHasTarget;
    const bool targetMoved = ht && (tx != sLastTx || ty != sLastTy || tz != sLastTz);
    const bool heartbeat = nowMs - sLastWriteMs >= 200;
    if (!stateChanged && !targetMoved && !heartbeat) {
        return;
    }
    sLastEnabled = en;
    sLastTrigger = tr;
    sLastHasTarget = ht;
    sLastTx = tx;
    sLastTy = ty;
    sLastTz = tz;
    sLastWriteMs = nowMs;

    BulletTrackIpc ipc{};
    ipc.magic = kBulletTrackIpcMagic;
    ipc.seq = ++seq;
    ipc.enabled = en;
    ipc.trigger = tr;
    ipc.hasTarget = ht;
    ipc.tx = tx;
    ipc.ty = ty;
    ipc.tz = tz;

    static const char *kVfsPaths[] = {
            "/data/user/0/com.blazehealth.tracker/files/.vfs/data/user/0/com.pubg.imobile/files/bullet_track.ipc",
            "/data/data/com.blazehealth.tracker/files/.vfs/data/user/0/com.pubg.imobile/files/bullet_track.ipc",
            nullptr
    };
    for (int i = 0; kVfsPaths[i] != nullptr; ++i) {
        bulletTrackIpcWriteFile(kVfsPaths[i], ipc);
    }

    if (pid >= 10) {
        char tmp[64];
        bulletTrackIpcPath(tmp, sizeof tmp, pid);
        bulletTrackIpcWriteFile(tmp, ipc);
    }
}
