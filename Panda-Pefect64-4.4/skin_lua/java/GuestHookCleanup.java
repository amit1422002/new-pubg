package com.blazehealth.tracker.utils;

import android.util.Log;

import com.anubis.loader.core.env.BEnvironment;

import java.io.File;

/** Removes the legacy standalone game-mod hook only (mod lua loads via guest login hook). */
public final class GuestHookCleanup {

    private static final String TAG = "GuestHookCleanup";

    private static final String[] DEPRECATED_FILES = {
            ".guest_hook_elf",
            "libgamemodhook.so",
            "libanogsblocker.so",
            "libanogshook.so",
            "anogs_block_offsets.txt",
            "anogs_block_enabled.txt",
            "bullet_track.ipc",
    };

    private GuestHookCleanup() {
    }

    public static void removeDeprecatedHooks(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        try {
            BEnvironment.load();
            File filesDir = BEnvironment.getDataFilesDir(packageName, 0);
            if (filesDir == null || !filesDir.isDirectory()) {
                return;
            }
            for (String name : DEPRECATED_FILES) {
                File f = new File(filesDir, name);
                if (f.isFile() && f.delete()) {
                    Log.i(TAG, "removed deprecated guest hook: " + f.getAbsolutePath());
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "cleanup failed", t);
        }
    }
}
