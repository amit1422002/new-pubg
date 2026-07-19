#pragma once

#include "TouchInput.hpp"
#include <cmath>
#include <unistd.h>
#include <pthread.h>

/** Touch Simulation (SDK tab) — separate from legacy aimbot touch (aimT==0). */
bool touchSimMode = false;
float tsScrW = 0.f;
float tsScrH = 0.f;
float tsAimX = 0.f;
float tsAimY = 0.f;
bool tsAimActive = false;

float tsTouchX = 650.f;
float tsTouchY = 1400.f;
float tsTouchRange = 300.f;
float tsDragSpeed = 20.f;
float tsSmoothMs = 18.f;
bool tsInvert = false;

static bool tsTouchDown = false;
static double tsCurX = 0.0;
static double tsCurY = 0.0;

static inline float tsDist(float x1, float y1, float x2, float y2) {
    const float dx = x1 - x2;
    const float dy = y1 - y2;
    return sqrtf(dx * dx + dy * dy);
}

static inline float tsSpeedForError(float errDist) {
    float div = tsDragSpeed;
    if (errDist < 8.f) div = tsDragSpeed * 0.35f;
    else if (errDist < 25.f) div = tsDragSpeed * 0.55f;
    else if (errDist < 60.f) div = tsDragSpeed * 0.85f;
    else if (errDist < 120.f) div = tsDragSpeed * 1.15f;
    else div = tsDragSpeed * 1.45f;
    if (div < 4.f) div = 4.f;
    return div;
}

static inline void tsReleaseTouch() {
    if (tsTouchDown) {
        TouchInput::sendTouchUp();
        tsTouchDown = false;
        tsCurX = tsTouchX;
        tsCurY = tsTouchY;
    }
}

static inline void tsApplyMove(int x, int y) {
    if (!tsInvert) {
        TouchInput::sendTouchMove(x, y);
    } else {
        const float cx = tsScrW * 0.5f;
        const float cy = tsScrH * 0.5f;
        TouchInput::sendTouchMove((int) (cy * 2.f - (float) y), (int) (cx * 2.f - (float) x));
    }
}

[[noreturn]] inline void *TouchSimDragLoop(void *) {
    int initTries = 0;
    while (true) {
        if (touchSimMode && tsScrW > 0.f && tsScrH > 0.f) {
            TouchInput::ensureTouchInput((int) tsScrW, (int) tsScrH);
            if (TouchInput::isTouchReady()) {
                break;
            }
        }
        if (++initTries > 80) {
            break;
        }
        usleep(250000);
    }

    tsCurX = tsTouchX;
    tsCurY = tsTouchY;

    while (true) {
        if (!touchSimMode || tsScrW <= 0.f || tsScrH <= 0.f) {
            tsReleaseTouch();
            usleep(50000);
            continue;
        }

        if (!TouchInput::isTouchReady()) {
            TouchInput::ensureTouchInput((int) tsScrW, (int) tsScrH);
            usleep(50000);
            continue;
        }

        const float cx = tsScrW * 0.5f;
        const float cy = tsScrH * 0.5f;
        const float tx = tsAimX;
        const float ty = tsAimY;

        if (!tsAimActive || tx <= 0.f || ty <= 0.f || tx >= tsScrW || ty >= tsScrH) {
            tsReleaseTouch();
            usleep((useconds_t) (tsSmoothMs * 1000.f));
            continue;
        }

        const float errX = tx - cx;
        const float errY = ty - cy;
        const float errDist = tsDist(tx, ty, cx, cy);
        if (errDist < 1.5f) {
            tsReleaseTouch();
            usleep((useconds_t) (tsSmoothMs * 1000.f));
            continue;
        }

        const float speed = tsSpeedForError(errDist);
        const float stepX = errX / speed;
        const float stepY = errY / speed;

        if (!tsTouchDown) {
            tsCurX = tsTouchX;
            tsCurY = tsTouchY;
            tsApplyMove((int) tsCurX, (int) tsCurY);
            tsTouchDown = true;
        }

        tsCurX += stepX;
        tsCurY += stepY;

        const float half = tsTouchRange * 0.5f;
        if (tsCurX - half < tsTouchX - tsTouchRange
            || tsCurX + half > tsTouchX + tsTouchRange
            || tsCurY - half < tsTouchY - tsTouchRange
            || tsCurY + half > tsTouchY + tsTouchRange) {
            tsReleaseTouch();
            usleep((useconds_t) (tsSmoothMs * 1000.f));
            continue;
        }

        tsApplyMove((int) tsCurX, (int) tsCurY);
        usleep((useconds_t) (tsSmoothMs * 1000.f));
    }
}

inline void StartTouchSimThread() {
    static bool started = false;
    if (started) {
        return;
    }
    started = true;
    pthread_t tid;
    pthread_create(&tid, nullptr, TouchSimDragLoop, nullptr);
    pthread_detach(tid);
}
