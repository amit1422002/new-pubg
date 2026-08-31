package com.anubis.skin;

import android.os.Build;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;

/**
 * Loads guest hook library via memfd / System.load with fallbacks.
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

        // Method 1: Linux / Android memfd
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                FileDescriptor fd = Os.memfd_create("guest_hook", 0);
                if (fd != null && fd.valid()) {
                    Os.write(fd, elf, 0, elf.length);
                    int fdNum = getFdNum(fd);
                    if (fdNum >= 0) {
                        try {
                            System.load("/proc/self/fd/" + fdNum);
                            Log.i(TAG, "memfd System.load ok fd=" + fdNum);
                            return true;
                        } catch (Throwable t) {
                            Log.w(TAG, "System.load(/proc/self/fd/...) failed: " + t.getMessage());
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "memfd_create failed: " + t.getMessage());
            }
        }

        // Method 2: System.loadLibrary
        try {
            System.loadLibrary("guestloginhook");
            Log.i(TAG, "System.loadLibrary(guestloginhook) ok");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "System.loadLibrary(guestloginhook) failed: " + t.getMessage());
        }

        // Method 3: Anonymous temporary cache file
        try {
            File tempSo = File.createTempFile(".hook_", ".so");
            try (FileOutputStream fos = new FileOutputStream(tempSo)) {
                fos.write(elf);
            }
            tempSo.setReadable(true, true);
            tempSo.setExecutable(true, true);
            System.load(tempSo.getAbsolutePath());
            tempSo.delete();
            Log.i(TAG, "temp cache System.load ok");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "temp cache System.load failed: " + t.getMessage());
        }

        return false;
    }

    private static int getFdNum(FileDescriptor fd) {
        try {
            java.lang.reflect.Field field = FileDescriptor.class.getDeclaredField("descriptor");
            field.setAccessible(true);
            return field.getInt(fd);
        } catch (Throwable ignored) {
            try {
                java.lang.reflect.Method method = FileDescriptor.class.getDeclaredMethod("getInt$");
                method.setAccessible(true);
                return (Integer) method.invoke(fd);
            } catch (Throwable ignored2) {
            }
        }
        return -1;
    }
}
