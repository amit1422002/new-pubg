#pragma once

#include "esp_protocol/delta_esp_protocol.h"

bool DeltaForceEspServerEnsure();
void DeltaForceEspServerSetScreen(int width, int height);
void DeltaForceEspServerSetSmallCrosshair(int enabled);
void DeltaForceEspServerSetAimbotConfig(const DeltaEspAimbotConfig *cfg);
void DeltaForceEspServerSetBulletTrackConfig(const DeltaEspBulletTrackConfig *cfg);
bool DeltaForceEspServerFetch(int screenW, int screenH, struct DeltaEspEntry *out, int maxOut, int *outCount);
int DeltaForceEspServerLastCount();
bool DeltaForceEspServerListening();
bool DeltaForceEspServerClientConnected();
