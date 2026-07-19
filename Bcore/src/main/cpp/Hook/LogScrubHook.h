#ifndef ANUBIS_LOG_SCRUB_HOOK_H
#define ANUBIS_LOG_SCRUB_HOOK_H

#include <jni.h>

namespace LogScrubHook {
    void init();
    void setConfig(bool enabled, const char *hostPkg, const char *guestPkg);
}

#endif
