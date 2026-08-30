#include "MapsGameLibsDump.h"

#include <android/log.h>

void blaze_log_inject_and_game_maps(const char *hook_name) {
    const char *label = (hook_name != nullptr && hook_name[0] != '\0')
                                ? hook_name
                                : "guest-hook";
    __android_log_print(ANDROID_LOG_INFO, "GuestLogin",
                        "hook native OK: %s (in-process)", label);
}

void blaze_dump_proc_maps() {
}

void schedule_maps_hide_after_lua() {
    // Disabled — libc maps hooks freeze UE4 even deferred; re-enable via Java when stable.
    __android_log_print(ANDROID_LOG_INFO, "GuestLogin",
                        "maps hide skipped (disabled — causes freeze)");
}
