package com.anubis.skin;

import android.util.Log;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.GamePackages;

import java.io.File;

/**
 * Logs out BGMI inside the virtual clone without touching the host app data.
 */
public final class BgmiLogoutHelper {

    private static final String TAG = "BgmiLogout";

    private BgmiLogoutHelper() {
    }

    public static boolean logoutAccount(int userId) {
        return logoutAccount(GamePackages.BGMI, userId);
    }

    public static boolean logoutAccount(String packageName, int userId) {
        if (!GamePackages.isBgmi(packageName)) {
            return false;
        }
        try {
            try {
                AnubisCore.get().stopPackage(packageName, userId);
            } catch (Throwable ignored) {
            }

            BEnvironment.load();
            File dataDir = BEnvironment.getDataDir(packageName, userId);
            if (dataDir == null) {
                return false;
            }

            cleanDataRoot(dataDir);
            cleanFilesTree(BEnvironment.getDataFilesDir(packageName, userId));
            cleanFilesTree(BEnvironment.getExternalDataFilesDir(packageName, userId));

            Log.i(TAG, "logout OK virtual base=" + dataDir.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "logout failed", t);
            return false;
        }
    }

    private static void cleanDataRoot(File base) {
        File sharedPrefs = new File(base, "shared_prefs");
        deleteRecursive(sharedPrefs);
        if (!sharedPrefs.mkdirs() && !sharedPrefs.isDirectory()) {
            Log.w(TAG, "shared_prefs mkdir failed");
        }
        sharedPrefs.setReadable(true, false);
        sharedPrefs.setWritable(true, false);
        sharedPrefs.setExecutable(true, false);

        deleteRecursive(new File(base, "databases"));
    }

    private static void cleanFilesTree(File filesDir) {
        if (filesDir == null) {
            return;
        }
        deleteRecursive(new File(filesDir, "login-identifier.txt"));
        deleteRecursive(new File(filesDir,
                "UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Intermediate"));
        deleteRecursive(new File(filesDir, "TGPA"));
        deleteRecursive(new File(filesDir, "ProgramBinaryCache"));
        deleteRecursive(new File(filesDir, "ano_tmp"));
    }

    private static boolean deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        if (!file.canWrite()) {
            file.setWritable(true, false);
        }
        return file.delete() || !file.exists();
    }
}
