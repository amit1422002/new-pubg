package com.blazehealth.tracker.utils;

import android.system.Os;

import com.anubis.loader.core.NativeCore;

/**
 * Memfd-loads guest hook via libHASAD (already in guest) — no BLAZEBOX / host base.apk in maps.
 */
public final class GuestMemoryLoader {

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
        return NativeCore.memfdLoadElf(elf);
    }
}
