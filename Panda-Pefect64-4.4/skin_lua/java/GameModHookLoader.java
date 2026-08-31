package com.blazehealth.tracker.utils;

import android.os.Process;
import android.util.Log;

import com.anubis.loader.core.env.BEnvironment;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loads {@code libgamemodhook.so} once inside the cloned BGMI guest process. */
public final class GameModHookLoader {

    private static final String TAG = "GuestLogin";
    private static final String HOOK_FILE = "libgamemodhook.so";
    private static final AtomicBoolean sLoadStarted = new AtomicBoolean(false);

    private GameModHookLoader() {
    }

    public static void scheduleLoad(String packageName, String processName) {
        // game-mod disabled — guest login only
        return;
        /*
        if (!CloneDataHelper.BGMI_PKG.equals(packageName)) {
            return;
        }
        if (!sLoadStarted.compareAndSet(false, true)) {
            Log.i(TAG, "game-mod hook load already scheduled");
            return;
        }
        Log.i(TAG, "game-mod hook load scheduled pid=" + Process.myPid()
                + " proc=" + processName);
        new Thread(() -> loadWhenReady(packageName), "game-mod-load").start();
        */
    }

    public static void resetSession() {
        sLoadStarted.set(false);
    }

    private static void loadWhenReady(String packageName) {
        try {
            Log.i(TAG, "game-mod: waiting 8s for UE4/Lua...");
            Thread.sleep(8000L);

            BEnvironment.load();
            File hook = new File(BEnvironment.getDataFilesDir(packageName, 0), HOOK_FILE);
            if (!hook.isFile() || hook.length() < 1024L) {
                Log.w(TAG, "game-mod hook missing: " + hook.getAbsolutePath());
                return;
            }
            for (int attempt = 1; attempt <= 6; attempt++) {
                try {
                    System.load(hook.getAbsolutePath());
                    Log.i(TAG, "game-mod hook loaded pid=" + Process.myPid()
                            + " attempt=" + attempt + " path=" + hook.getAbsolutePath());
                    return;
                } catch (UnsatisfiedLinkError e) {
                    Log.w(TAG, "game-mod hook retry " + attempt, e);
                    Thread.sleep(3000L);
                }
            }
            Log.w(TAG, "game-mod hook load gave up");
        } catch (Throwable t) {
            Log.w(TAG, "game-mod hook load failed", t);
        }
    }
}
