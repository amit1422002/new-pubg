#pragma once

/** Log hook OK (in-process). */
void blaze_log_inject_and_game_maps(const char *hook_name);

/** Maps-only dump (GuestLogin tag). */
void blaze_dump_proc_maps();

/** Defer libc maps-hide ~12s after lua — never on the lua/trap thread. */
void schedule_maps_hide_after_lua();
