#pragma once

#include <cstdint>

/** Abstract unix socket name for Delta Force ESP IPC. */
#define DELTA_ESP_SOCKET_NAME "DFM84"
#define DELTA_MAX_ESP_PLAYERS 64
#define DELTA_MAX_NAME_CHARS  8
#define DELTA_MAX_BONE_SEGS   19

struct DeltaEspEntry {
    float x;
    float top;
    float bottom;
    float w;
    float middle;
    float health;
    float teamId;
    float distance;
    float isBot;
    float headScreenX;
    float headScreenY;
    float nameChars[DELTA_MAX_NAME_CHARS];
    float boneSegs[DELTA_MAX_BONE_SEGS * 4];
};

enum DeltaEspMode : int {
    DELTA_ESP_MODE_INIT = 1,
    DELTA_ESP_MODE_DRAW = 8,
};

/** Aimbot settings passed host -> guest each frame (mirrors STARCOOL AimbotConfig). */
struct DeltaEspAimbotConfig {
    int enabled = 0;
    int teamCheck = 1;
    int visibleCheck = 0;
    int aimWhenZooming = 0;
    int aimWhenFiringOnly = 0;
    int ignoreLayingOnTheGround = 1;
    int useBoneTargeting = 1;
    int freeBoneTargeting = 0;
    int prioritizeClosest = 1;
    float maxDistance = 300.f;
    float smoothness = 5.f;
    float fov = 8.f;
    float targetHeight = 10.f;
    float verticalAdjust = 10.f;
    int aimBone = 31; /* vHead */
};

/** BGMI bullet track — shellcode redirect while firing. */
struct DeltaEspBulletTrackConfig {
    int enabled = 0;
    int teamCheck = 1;
    int whenFiring = 1;
    int whenZooming = 0;
    int useBoneTargeting = 1;
    float maxDistance = 300.f;
    float fov = 15.f;
    int aimBone = 6;
    float verticalAdjust = 0.f;
};

struct DeltaEspRequest {
    int mode = DELTA_ESP_MODE_DRAW;
    int screenWidth = 0;
    int screenHeight = 0;
    int smallCrosshair = 0;
    DeltaEspAimbotConfig aimbot{};
    DeltaEspBulletTrackConfig bulletTrack{};
};

struct DeltaEspResponse {
    int playerCount = 0;
    DeltaEspEntry players[DELTA_MAX_ESP_PLAYERS];
};
