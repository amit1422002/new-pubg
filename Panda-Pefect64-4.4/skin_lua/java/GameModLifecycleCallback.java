package com.blazehealth.tracker.utils;

import android.app.Application;
import android.os.Process;
import android.util.Log;

import com.anubis.loader.app.configuration.AppLifecycleCallback;
/**
 * Loads {@code libgamemodhook.so} inside the cloned BGMI process (no ptrace).
 */
public final class GameModLifecycleCallback extends AppLifecycleCallback {

    private static final String TAG = "GuestLogin";

    @Override
    public void afterApplicationOnCreate(String packageName, String processName,
                                         Application application, int userId) {
        if (!CloneDataHelper.BGMI_PKG.equals(packageName) || application == null) {
            return;
        }
        // game-mod disabled — guest login only
        // Log.i(TAG, "game-mod lifecycle onCreate pid=" + Process.myPid() + " proc=" + processName);
        // GameModHookLoader.scheduleLoad(packageName, processName);
    }
}
