package com.blazehealth.tracker.utils;

import android.content.Context;
import android.util.Log;

/**
 * Remove Limit — host-side /proc/pid/mem RET patch on libhdmpve.so (ESP toggle only).
 * Patches: 0x132030 + 0x150AB4 with C0 03 5F D6 (arm64 RET).
 */
public final class HdmpveBlockerHelper {

    private static final String TAG = "HdmpveBlocker";

    private static volatile boolean sSessionRemoveLimit;

    static {
        try {
            System.loadLibrary("BLAZEBOX");
        } catch (Throwable ignored) {
        }
    }

    private static native boolean nativeSetRemoveLimit(boolean enabled);

    private static native void nativeResetSession();

    private HdmpveBlockerHelper() {
    }

    public static void resetOnRestart(Context context) {
        sSessionRemoveLimit = false;
        nativeResetSession();
        Log.i(TAG, "remove limit reset for new session");
    }

    public static boolean isRemoveLimitEnabled(Context context) {
        return sSessionRemoveLimit;
    }

    public static boolean setRemoveLimitEnabled(Context context, boolean enabled) {
        sSessionRemoveLimit = enabled;
        boolean ok = nativeSetRemoveLimit(enabled);
        Log.i(TAG, "remove limit " + (enabled ? "ON" : "OFF")
                + " libhdmpve RET patch=" + ok);
        return ok;
    }
}
