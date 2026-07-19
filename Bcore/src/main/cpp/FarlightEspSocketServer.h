#pragma once

#include <atomic>

bool FarlightEspServerEnsure();
void FarlightEspServerSetScreen(int width, int height);
bool FarlightEspServerFetch(int screenW, int screenH, struct FarlightEspEntry *out, int maxOut, int *outCount);
int FarlightEspServerLastCount();
bool FarlightEspServerListening();
bool FarlightEspServerClientConnected();
