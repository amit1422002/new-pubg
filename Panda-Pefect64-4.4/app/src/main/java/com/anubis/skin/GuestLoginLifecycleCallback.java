package com.anubis.skin;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.elite.EliteInstaller;
import com.elite.app.configuration.AppLifecycleCallback;
import com.elite.core.env.BEnvironment;

import java.io.File;

/**
 * Loads guest hook + Lua only inside cloned BGMI UE4 process ({@link BgmiSkin#BGMI_PKG}).
 * Timing matches skin_lua: ~18s wait then memfd load.
 */
public final class GuestLoginLifecycleCallback extends AppLifecycleCallback {

    private static final String TAG = "GuestLogin";

    private static boolean isUe4HostProcess(String packageName, String processName) {
        if (processName == null || packageName == null) {
            return false;
        }
        if (packageName.equals(processName)) {
            return true;
        }
        if (!processName.startsWith(packageName + ":")) {
            return false;
        }
        final String suffix = processName.substring(packageName.length());
        if (":plugin".equals(suffix)
                || suffix.startsWith(":sandbox")
                || suffix.startsWith(":webview")
                || suffix.contains("GPUProcess")
                || suffix.contains("Privileged")) {
            return false;
        }
        return suffix.matches(":p\\d+");
    }

    @Override
    public void afterApplicationOnCreate(String packageName, String processName,
                                         Application application, int userId) {
        if (!BgmiSkin.isBgmi(packageName) || application == null) {
            return;
        }
        if (!isUe4HostProcess(packageName, processName)) {
            Log.i(TAG, "skip hook proc=" + processName + " pid=" + Process.myPid());
            return;
        }
        Log.i(TAG, "schedule hook pkg=" + packageName + " proc=" + processName
                + " user=" + userId + " pid=" + Process.myPid());
        new Thread(() -> loadHookWhenReady(packageName, userId), "guest-login-load").start();
    }

    private static String guestVisibleFilesDir(String packageName, int userId) {
        return "/data/user/" + userId + "/" + packageName + "/files";
    }

    private static void loadHookWhenReady(String packageName, int userId) {
        try {
            Log.i(TAG, "waiting for login screen before memfd hook...");
            Thread.sleep(18_000L);

            BEnvironment.load();
            File filesDir = BEnvironment.getDataFilesDir(packageName, userId);
            if (filesDir == null) {
                Log.w(TAG, "guest files dir null user=" + userId);
                return;
            }
            GuestHookCleanup.removeDeprecatedHooks(packageName, userId);

            Context host = EliteInstaller.getContext();
            if (host != null) {
                GuestLoginHelper.deployToGuest(host.getApplicationContext(), packageName, userId);
            }

            byte[] elf = GuestLoginHelper.readHookBytesForGuest(filesDir);
            if (elf == null || elf.length < 1024) {
                Log.w(TAG, "hook ELF missing — relaunch from app (dir=" + filesDir + ")");
                return;
            }

            GuestMemoryLoader.prepareGuestHookFilesDir(filesDir.getAbsolutePath());
            Log.i(TAG, "hook files dir (real vfs): " + filesDir.getAbsolutePath()
                    + " guest-visible=" + guestVisibleFilesDir(packageName, userId));

            for (int attempt = 1; attempt <= 6; attempt++) {
                try {
                    if (GuestMemoryLoader.loadHookFromMemory(elf)) {
                        Log.i(TAG, "memfd hook ok attempt=" + attempt + " bytes=" + elf.length
                                + " pid=" + Process.myPid());
                        return;
                    }
                    Log.w(TAG, "memfd hook failed attempt=" + attempt);
                } catch (Throwable e) {
                    Log.w(TAG, "hook load retry " + attempt, e);
                }
                Thread.sleep(3000L);
            }
            Log.w(TAG, "guest hook load gave up (memfd only)");
        } catch (Throwable t) {
            Log.w(TAG, "guest hook load failed", t);
        }
    }
}
