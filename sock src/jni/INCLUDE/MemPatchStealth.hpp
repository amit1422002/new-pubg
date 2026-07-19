#pragma once

/**
 * Transient memory patches — write only in a short window, restore before scans see it.
 * Memory holds the original game value almost all of the time.
 */
#include "Offsets.h"
#include "support.h"
#include <cstdint>
#include <unistd.h>

struct TransientFloatPatch {
    uintptr_t entity = 0;
    uintptr_t fieldOff = 0;
    float orig = -1.f;
    bool patched = false;

    void restore() {
        if (!patched || entity == 0 || !isValid64(entity) || orig < 0.f) {
            patched = false;
            return;
        }
        Write2<float>(entity + fieldOff, orig);
        patched = false;
    }

    void reset() {
        restore();
        entity = 0;
        fieldOff = 0;
        orig = -1.f;
    }

    void onEntityChange(uintptr_t newEntity) {
        if (newEntity != entity) {
            restore();
            entity = newEntity;
            orig = -1.f;
        }
    }

    /** Call at loop start — skip restore while crosshair patch should stay active. */
    void beginFrame(bool restorePatch = true) {
        if (restorePatch) {
            restore();
        }
    }

    /**
     * Call at loop end — brief 0 write for crosshair, optional micro-hold, then restore.
     * Returns true if a pulse was applied this tick.
     */
    bool pulseEnd(uintptr_t shootEntity, uintptr_t off, bool enable, bool weaponActive) {
        onEntityChange(shootEntity);
        if (!enable || !weaponActive || !isValid64(shootEntity)) {
            restore();
            if (!enable) {
                reset();
            }
            return false;
        }
        fieldOff = off;
        float cur = getF(shootEntity + off);
        if (orig < 0.f && cur > 0.0001f && cur < 50.f) {
            orig = cur;
        }
        if (orig < 0.f) {
            return false;
        }
        if (cur < 0.0001f) {
            patched = true;
            return true;
        }
        Write2<float>(shootEntity + off, 0.f);
        patched = true;
        restore();
        return true;
    }
};

static TransientFloatPatch sSmallCrosshairPatch;

/** Keep deviation at 0 while enabled — restored on disable or next beginFrame before ESP reads. */
static inline void tickSmallCrosshairStealth(uintptr_t shootEntity, bool enable, bool weaponActive) {
    sSmallCrosshairPatch.onEntityChange(shootEntity);
    if (!enable) {
        sSmallCrosshairPatch.reset();
        return;
    }
    if (!weaponActive || !isValid64(shootEntity)) {
        sSmallCrosshairPatch.restore();
        return;
    }
    const uintptr_t off = OffsetsAll64::GameDeviationFactor;
    const float cur = getF(shootEntity + off);
    if (sSmallCrosshairPatch.orig < 0.f && cur > 0.0001f && cur < 50.f) {
        sSmallCrosshairPatch.orig = cur;
    }
    if (sSmallCrosshairPatch.orig < 0.f) {
        return;
    }
    sSmallCrosshairPatch.fieldOff = off;
    sSmallCrosshairPatch.entity = shootEntity;
    if (cur > 0.0001f) {
        Write2<float>(shootEntity + off, 0.f);
        sSmallCrosshairPatch.patched = true;
    }
}
