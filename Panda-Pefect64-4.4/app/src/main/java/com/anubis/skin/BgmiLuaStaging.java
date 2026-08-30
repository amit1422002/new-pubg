package com.anubis.skin;

import android.content.Context;
import android.util.Log;

/**
 * Stages BGMI Lua assets: guest login + skin mod + game mod lua.
 */
public final class BgmiLuaStaging {

    private static final String TAG = "BgmiLua";

    private BgmiLuaStaging() {
    }

    public static void deployForLaunch(Context context, int userId) {
        if (context == null) {
            return;
        }
        try {
            GuestHookCleanup.removeDeprecatedHooks(BgmiSkin.BGMI_PKG, userId);
            GuestLoginHelper.deployToGuest(context.getApplicationContext(), BgmiSkin.BGMI_PKG, userId);
            Log.i(TAG, "guest + skin + gamemod lua staged for " + BgmiSkin.BGMI_PKG + " user=" + userId);
        } catch (Throwable t) {
            Log.w(TAG, "lua staging failed", t);
        }
    }

    public static void deployForLaunch(Context context) {
        deployForLaunch(context, 0);
    }
}
