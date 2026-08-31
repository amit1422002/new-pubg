package com.blazehealth.tracker.utils;

import android.content.Context;
import android.util.Log;

/**
 * Stages BGMI guest-login + skin + game-mod Lua only (no libanogs / bullet-track).
 */
public final class LibAnogsPatcher {

    private static final String TAG = "GuestLoginPatch";

    private LibAnogsPatcher() {
    }

    public static void resetSession() {
    }

    public static void resetSession(Context context) {
    }

    public static void startOnGameLaunch(Context context) {
        if (context == null) {
            return;
        }
        try {
            GuestLoginHelper.deployToGuest(context, CloneDataHelper.BGMI_PKG);
            GameModHelper.deployToGuest(context, CloneDataHelper.BGMI_PKG);
            Log.i(TAG, "guest login + skin + game mod staged");
        } catch (Throwable t) {
            Log.w(TAG, "guest login init failed", t);
        }
    }
}
