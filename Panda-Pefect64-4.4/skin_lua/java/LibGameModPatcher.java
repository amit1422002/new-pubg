package com.blazehealth.tracker.utils;

import android.content.Context;
import android.util.Log;

/**
 * Stages BGMI game-mod Lua + hook .so; guest process loads hook via
 * {@link GameModLifecycleCallback} after libUE4 is ready (no ptrace).
 */
public final class LibGameModPatcher {

    private static final String TAG = "GuestLoginPatch";

    private LibGameModPatcher() {
    }

    public static void resetSession() {
    }

    public static void startOnGameLaunch(Context context) {
        if (context == null) {
            return;
        }
        // game-mod disabled — guest login only
        // try {
        //     GameModHelper.deployToGuest(context, CloneDataHelper.BGMI_PKG);
        //     Log.i(TAG, "game-mod assets staged (via LibGameModPatcher)");
        // } catch (Throwable t) {
        //     Log.w(TAG, "deploy failed", t);
        // }
    }
}
