#pragma once

/**
 * Smart Memory Aim — zero-stutter: no ViewPitch lock, throttled tiny ControlRotation
 * nudge only while moving slowly. Moving fast = no writes at all.
 */
#include "CameraView.h"
#include "Offsets.h"
#include "support.h"
#include <cmath>
#include <cstdint>

extern bool touchSimMode;
extern float tsDragSpeed;
extern float tsSmoothMs;

Vec3 tsWorldTarget{};
Vec3 tsDesiredAngles{};

static uint32_t sTsWriteTick = 0;

static inline void tsResetStealthState() {
    sTsWriteTick = 0;
}

static inline float tsNorm180(float a) {
    while (a > 180.f) a -= 360.f;
    while (a < -180.f) a += 360.f;
    return a;
}

static inline FRotator tsReadRotator(uintptr_t addr) {
    FRotator r{};
    if (!isValid64(addr)) {
        return r;
    }
    r.Pitch = Read<float>(addr);
    r.Yaw = Read<float>(addr + sizeof(float));
    r.Roll = Read<float>(addr + 2 * sizeof(float));
    return r;
}

static inline float tsHorizSpeed(uintptr_t localPlayer) {
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

/** Throttled micro-assist — skipped while sprinting/moving (removes move stutter). */
static inline void tsApplySmoothAssist(uintptr_t aimControl, uintptr_t localPlayer,
                                      const Vec3 &desiredVec, bool scoped) {
    if (!touchSimMode || !isValid64(aimControl)) {
        return;
    }

    const float horizSpeed = tsHorizSpeed(localPlayer);
    if (horizSpeed > 280.f) {
        return;
    }

    if ((++sTsWriteTick % 3) != 0) {
        return;
    }

    const FRotator cur = tsReadRotator(aimControl + OffsetsAll64::ControlRotation);

    float tgtYaw = desiredVec.X;
    float tgtPitch = desiredVec.Y;

    if (scoped && isValid64(localPlayer)) {
        const FRotator add = tsReadRotator(localPlayer + OffsetsAll64::AimControlRotationAdditive);
        if (fabsf(add.Yaw) <= 40.f) {
            tgtYaw -= add.Yaw;
        }
        tgtYaw = tsNorm180(tgtYaw);
    }

    const float yawDelta = tsNorm180(tgtYaw - cur.Yaw);
    const float pitchDelta = tgtPitch - cur.Pitch;

    if (fabsf(yawDelta) < 0.08f && fabsf(pitchDelta) < 0.08f) {
        return;
    }
    if (fabsf(yawDelta) > 70.f || fabsf(pitchDelta) > 45.f) {
        return;
    }

    float alpha = 0.14f;
    if (tsSmoothMs > 0.f) {
        alpha = 8.f / (tsSmoothMs + 8.f);
        alpha *= (tsDragSpeed / 50.f);
        if (alpha > 0.28f) alpha = 0.28f;
        if (alpha < 0.08f) alpha = 0.08f;
    }

    float maxStep = horizSpeed > 120.f ? 0.65f : 1.1f;
    if (scoped) {
        maxStep = horizSpeed > 120.f ? 0.55f : 0.95f;
    }

    float stepYaw = yawDelta * alpha;
    float stepPitch = pitchDelta * alpha;
    if (stepYaw > maxStep) stepYaw = maxStep;
    else if (stepYaw < -maxStep) stepYaw = -maxStep;
    if (stepPitch > maxStep) stepPitch = maxStep;
    else if (stepPitch < -maxStep) stepPitch = -maxStep;

    const float outYaw = tsNorm180(cur.Yaw + stepYaw);
    float outPitch = cur.Pitch + stepPitch;
    if (outPitch > 70.f) outPitch = 70.f;
    else if (outPitch < -70.f) outPitch = -70.f;

    Writes(aimControl + OffsetsAll64::ControlRotation, outPitch);
    Writes(aimControl + OffsetsAll64::ControlRotation + sizeof(float), outYaw);
}
