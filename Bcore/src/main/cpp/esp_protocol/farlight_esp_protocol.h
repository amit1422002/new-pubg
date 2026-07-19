#pragma once

#include <cstdint>

#define FARLIGHT_ESP_SOCKET_NAME "FLT84"
#define FARLIGHT_MAX_ESP_PLAYERS 64

struct FarlightEspEntry {
    float x;
    float top;
    float bottom;
    float w;
    float middle;
    float health;
    float teamId;
    float distance;
};

enum FarlightEspMode : int {
    FARLIGHT_ESP_MODE_INIT = 1,
    FARLIGHT_ESP_MODE_DRAW = 8,
};

struct FarlightEspRequest {
    int mode = FARLIGHT_ESP_MODE_DRAW;
    int screenWidth = 0;
    int screenHeight = 0;
};

struct FarlightEspResponse {
    int playerCount = 0;
    FarlightEspEntry players[FARLIGHT_MAX_ESP_PLAYERS];
};
