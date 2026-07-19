#pragma once

/**
 * Memory aim hardening — NO ViewPitch writes, NO per-frame camera restore.
 * ControlRotation nudges are sparse, delayed, and humanized.
 *
 * NOTE: Any cross-process memory write can still score on AC ML over time.
 * Touch Sim (BCore MotionEvent) is the zero-write path when ban risk matters.
 */
#include "CameraView.h"
#include "Offsets.h"
#include "support.h"
#include <cmath>
#include <cstdint>
#include <ctime>

extern float aimSmooth;

static uint32_t sMemAimWriteTick = 0;
static uint32_t sMemAimRng = 0xC0FFEE42u;
static bool sMemAimWasActive = false;
static uint64_t sMemAimTargetSinceMs = 0;
static uint64_t sMemAimBurstSinceMs = 0;
static uint32_t sMemAimNextThrottle = 4;
static uint32_t sMemAimActivationDelayMs = 0;
static uintptr_t sMemAimLastTargetToken = 0;

static inline uint32_t memAimRand() {
    sMemAimRng ^= sMemAimRng << 13;
    sMemAimRng ^= sMemAimRng >> 17;
    sMemAimRng ^= sMemAimRng << 5;
    return sMemAimRng;
}

static inline uint32_t memAimRandRange(uint32_t lo, uint32_t hi) {
    if (hi <= lo) {
        return lo;
    }
    return lo + (memAimRand() % (hi - lo + 1u));
}

static inline uint64_t memAimNowMs() {
    struct timespec ts{};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t) ts.tv_sec * 1000ull + (uint64_t) ts.tv_nsec / 1000000ull;
}

static inline void memAimResetStealthState() {
    sMemAimWriteTick = 0;
    sMemAimWasActive = false;
    sMemAimTargetSinceMs = 0;
    sMemAimBurstSinceMs = 0;
    sMemAimNextThrottle = 4;
    sMemAimActivationDelayMs = 0;
    sMemAimLastTargetToken = 0;
}

static inline float memAimNorm180(float a) {
    while (a > 180.f) a -= 360.f;
    while (a < -180.f) a += 360.f;
    return a;
}

static inline FRotator memAimReadRotator(uintptr_t addr) {
    FRotator r{};
    if (!isValid64(addr)) {
        return r;
    }
    r.Pitch = Read<float>(addr);
    r.Yaw = Read<float>(addr + sizeof(float));
    r.Roll = Read<float>(addr + 2 * sizeof(float));
    return r;
}

static inline float memAimHorizSpeed(uintptr_t localPlayer) {
    if (!isValid64(localPlayer)) {
        return 0.f;
    }
    const uintptr_t move = getA(localPlayer + OffsetsAll64::CharacterMovement);
    if (!isValid64(move)) {
        return 0.f;
    }
    const Vec3 vel = getVec3(move + OffsetsAll64::Velocity);
    return sqrtf(vel.X * vel.X + vel.Y * vel.Y);
}

/** Soft speed falloff — no hard on/off threshold at 260. */
static inline float memAimSpeedBlend(float horizSpeed) {
    if (horizSpeed <= 140.f) {
        return 1.f;
    }
    if (horizSpeed >= 360.f) {
        return 0.08f + (memAimRand() % 7u) * 0.01f;
    }
    const float t = (horizSpeed - 140.f) / 220.f;
    return 1.f - t * 0.88f;
}

static inline uintptr_t memAimTargetToken(const Vec3 &angles) {
    const int yawBucket = (int) (angles.X * 4.f);
    const int pitchBucket = (int) (angles.Y * 4.f);
    return (uintptr_t) ((uint32_t) yawBucket << 16) ^ (uint32_t) pitchBucket;
}

static inline bool memAimShouldWriteThisFrame() {
    if ((++sMemAimWriteTick % sMemAimNextThrottle) != 0u) {
        return false;
    }
    sMemAimNextThrottle = memAimRandRange(3u, 8u);
    if ((memAimRand() % 100u) < 28u) {
        return false;
    }
    return true;
}

/**
 * Sparse ControlRotation nudge — delayed activation, human jitter, fatigue.
 * Never touches ViewPitch / camera limits (no restore writes).
 */
static inline void memAimApplyStealth(uintptr_t camMgr, uintptr_t aimControl,
                                    uintptr_t localPlayer, const Vec3 &desiredAngles,
                                    bool active, bool scoped) {
    (void) camMgr;

    if (!active || !isValid64(aimControl)) {
        if (sMemAimWasActive) {
            sMemAimWasActive = false;
            sMemAimBurstSinceMs = 0;
            sMemAimTargetSinceMs = 0;
            sMemAimActivationDelayMs = 0;
        }
        return;
    }

    const uint64_t nowMs = memAimNowMs();
    const uintptr_t targetToken = memAimTargetToken(desiredAngles);
    if (targetToken != sMemAimLastTargetToken) {
        sMemAimLastTargetToken = targetToken;
        sMemAimTargetSinceMs = nowMs;
        sMemAimActivationDelayMs = (uint32_t) memAimRandRange(120u, 320u);
        sMemAimBurstSinceMs = nowMs;
    }

    if (sMemAimBurstSinceMs == 0) {
        sMemAimBurstSinceMs = nowMs;
    }

    if ((nowMs - sMemAimTargetSinceMs) < (uint64_t) sMemAimActivationDelayMs) {
        sMemAimWasActive = true;
        return;
    }

    if (!memAimShouldWriteThisFrame()) {
        sMemAimWasActive = true;
        return;
    }

    const float horizSpeed = memAimHorizSpeed(localPlayer);
    const float speedBlend = memAimSpeedBlend(horizSpeed);
    if (speedBlend < 0.12f && (memAimRand() % 100u) > 18u) {
        sMemAimWasActive = true;
        return;
    }

    const FRotator cur = memAimReadRotator(aimControl + OffsetsAll64::ControlRotation);

    float tgtYaw = desiredAngles.X;
    float tgtPitch = desiredAngles.Y;

    if (scoped && isValid64(localPlayer)) {
        const FRotator add = memAimReadRotator(localPlayer + OffsetsAll64::AimControlRotationAdditive);
        if (fabsf(add.Yaw) <= 40.f) {
            tgtYaw -= add.Yaw;
        }
        tgtYaw = memAimNorm180(tgtYaw);
    }

    const float yawDelta = memAimNorm180(tgtYaw - cur.Yaw);
    const float pitchDelta = tgtPitch - cur.Pitch;

    if (fabsf(yawDelta) < 0.07f && fabsf(pitchDelta) < 0.07f) {
        sMemAimWasActive = true;
        return;
    }
    if (fabsf(yawDelta) > 50.f || fabsf(pitchDelta) > 35.f) {
        sMemAimWasActive = true;
        return;
    }

    float alpha = 0.10f;
    if (aimSmooth > 0.f) {
        alpha = 6.f / (aimSmooth + 6.f);
        if (alpha > 0.18f) alpha = 0.18f;
        if (alpha < 0.05f) alpha = 0.05f;
    }
    alpha *= speedBlend;

    const float aimSeconds = (float) (nowMs - sMemAimBurstSinceMs) * 0.001f;
    const float fatigue = 1.f - fminf(aimSeconds * 0.035f, 0.35f);
    alpha *= fatigue;

    float maxStep = (0.55f + (memAimRand() % 25u) * 0.01f) * speedBlend;
    if (scoped) {
        maxStep *= 0.88f;
    }

    float stepYaw = yawDelta * alpha;
    float stepPitch = pitchDelta * alpha;

    const float jitter = ((float) (memAimRand() % 1000u) / 1000.f - 0.5f) * 0.09f;
    stepYaw = stepYaw * (1.f + jitter) + jitter * 0.15f;
    stepPitch = stepPitch * (1.f - jitter * 0.5f);

    if ((memAimRand() % 17u) == 0u) {
        stepYaw *= -0.35f;
        stepPitch *= 0.4f;
    }

    if (stepYaw > maxStep) stepYaw = maxStep;
    else if (stepYaw < -maxStep) stepYaw = -maxStep;
    if (stepPitch > maxStep) stepPitch = maxStep;
    else if (stepPitch < -maxStep) stepPitch = -maxStep;

    const float outYaw = memAimNorm180(cur.Yaw + stepYaw);
    float outPitch = cur.Pitch + stepPitch;
    if (outPitch > 70.f) outPitch = 70.f;
    else if (outPitch < -70.f) outPitch = -70.f;

    const bool writeYawFirst = (memAimRand() & 1u) != 0u;
    if (writeYawFirst) {
        Writes(aimControl + OffsetsAll64::ControlRotation + sizeof(float), outYaw);
        if ((memAimRand() % 100u) < 72u) {
            Writes(aimControl + OffsetsAll64::ControlRotation, outPitch);
        }
    } else {
        Writes(aimControl + OffsetsAll64::ControlRotation, outPitch);
        if ((memAimRand() % 100u) < 72u) {
            Writes(aimControl + OffsetsAll64::ControlRotation + sizeof(float), outYaw);
        }
    }

    sMemAimWasActive = true;
}
