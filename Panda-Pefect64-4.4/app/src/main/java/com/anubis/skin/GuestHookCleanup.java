package com.anubis.skin;

import android.util.Log;

import com.elite.core.env.BEnvironment;

import java.io.File;

/** Removes legacy on-disk hook artifacts; Lua loads via memfd guest hook only. */
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
            "skin_probe.log",
            "gamemod_probe.log",
    };

    private GuestHookCleanup() {
    }

    public static void removeDeprecatedHooks(String packageName) {
        removeDeprecatedHooks(packageName, 0);
    }

    public static void removeDeprecatedHooks(String packageName, int userId) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        try {
            BEnvironment.load();
            File filesDir = BEnvironment.getDataFilesDir(packageName, userId);
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
