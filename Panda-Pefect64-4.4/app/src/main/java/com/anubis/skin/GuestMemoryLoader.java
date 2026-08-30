package com.anubis.skin;

import android.system.Os;
import android.util.Log;

/**
 * Memfd-loads guest hook only — no on-disk .so (anti-cheat maps probe).
 * <p>
 * Note: {@code NativeCore.memfdLoadHookElf} lived in Bcore and is not present in elite.aar.
 * Until a host-side memfd loader is wired, this returns false.
 */
public final class GuestMemoryLoader {

    private static final String TAG = "GuestLogin";
    private static final String HOOK_FILES_ENV = "XT_GUEST_HOOK_DIR";

    private GuestMemoryLoader() {
    }

    public static void prepareGuestHookFilesDir(String absoluteDir) throws Exception {
        if (absoluteDir == null || absoluteDir.isEmpty()) {
            return;
        }
        String path = absoluteDir.endsWith("/") ? absoluteDir : absoluteDir + "/";
        Os.setenv(HOOK_FILES_ENV, path, true);
    }

    public static boolean loadHookFromMemory(byte[] elf) {
        if (elf == null || elf.length < 1024) {
            return false;
        }
        Log.w(TAG, "memfd hook unavailable with elite.aar (no NativeCore.memfdLoadHookElf)");
        return false;
    }
}
