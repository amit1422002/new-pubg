#pragma once

void bulletTrackInProcessStart();

/** Called from guest-login SIGSEGV handler — true if handled. */
extern "C" bool bulletTrackOnSegv(void *ctx_void);

/** True while bullet trap may need the shared SIGSEGV handler. */
extern "C" bool bulletTrackNeedsSegvHandler();
