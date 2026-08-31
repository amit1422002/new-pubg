package com.blazehealth.tracker.utils;

import android.content.Context;
import android.util.Log;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.env.BEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Clone data under loader app private storage ({@code .../files/.vfs/data/user/0/<package>/}).
 * No root required — same UID as {@code com.blazehealth.tracker}.
 */
public final class CloneDataHelper {

    private static final String TAG = "CloneDataHelper";
    public static final String BGMI_PKG = "com.pubg.imobile";

    private CloneDataHelper() {
    }

    public static File getRealAndroidDataDir(String packageName) {
        return ObbCopyHelper.getRealAndroidDataDir(packageName);
    }

    public static File getCloneAndroidDataDir(String packageName, int userId) {
        try {
            BEnvironment.load();
            return BEnvironment.getExternalDataDir(packageName, userId);
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean canAccessRealAndroidData(String packageName) {
        return ObbCopyHelper.canAccessRealAndroidData(packageName);
    }

    /**
     * @return null on success, otherwise error message
     */
    public static String copyAndroidDataToClone(String packageName, int userId,
                                                ObbCopyHelper.ProgressListener listener) {
        if (packageName == null || packageName.isEmpty()) {
            return "Invalid package";
        }
        if (userId != 0) {
            Log.w(TAG, "copyAndroidDataToClone: only user 0 supported, got " + userId);
        }
        return ObbCopyHelper.copyAndroidDataToAnubis(packageName, listener);
    }

    /**
     * Logout cleanup on clone path only (not real /data/data/com.pubg.imobile/).
     */
    public static boolean logoutBgmiClone(Context context, String packageName, int userId) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return false;
        }
        try {
            try {
                AnubisCore.get().stopPackage(packageName, userId);
            } catch (Throwable ignored) {
            }

            File base = resolveCloneDataDir(context.getApplicationContext(), packageName, userId);
            if (base == null) {
                return false;
            }
            if (!base.exists()) {
                base.mkdirs();
            }

            deleteRecursive(new File(base, "shared_prefs"));
            new File(base, "shared_prefs").mkdirs();
            deleteRecursive(new File(base, "files"));
            deleteRecursive(new File(base, "databases"));

            deleteRecursive(new File(base, "files/login-identifier.txt"));
            deleteRecursive(new File(base, "files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Intermediate"));
            deleteRecursive(new File(base, "files/TGPA"));
            deleteRecursive(new File(base, "files/ProgramBinaryCache"));

            try {
                BEnvironment.load();
                deleteRecursive(BEnvironment.getExternalDataDir(packageName, userId));
            } catch (Throwable ignored) {
            }

            Log.i(TAG, "logoutBgmiClone OK (no root) base=" + base.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "logoutBgmiClone failed", t);
            return false;
        }
    }

    /**
     * Guest reset on clone path only — new {@code device_id.xml}, clears identifiers/DB,
     * blocks TGPA/Intermediate caches (same idea as root sh reset, no root).
     */
    public static final class ResetGuestResult {
        public final boolean success;
        public final String backupPath;

        ResetGuestResult(boolean success, String backupPath) {
            this.success = success;
            this.backupPath = backupPath;
        }
    }

    public static ResetGuestResult resetGuestAccountBgmiClone(Context context, String packageName,
                                                              int userId) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return new ResetGuestResult(false, null);
        }
        try {
            GuestAccountBackupHelper.GuestSnapshot snap =
                    GuestAccountBackupHelper.captureBeforeReset(context, packageName, userId);

            try {
                AnubisCore.get().stopPackage(packageName, userId);
            } catch (Throwable ignored) {
            }

            BEnvironment.load();
            File base = resolveCloneDataDir(context.getApplicationContext(), packageName, userId);
            if (base == null) {
                return new ResetGuestResult(false, null);
            }
            if (!base.exists()) {
                base.mkdirs();
            }

            String newUuid = generateGuestUuid();

            deleteRecursive(new File(base, "shared_prefs"));
            File sharedPrefs = new File(base, "shared_prefs");
            if (!sharedPrefs.mkdirs()) {
                Log.w(TAG, "shared_prefs mkdir failed");
            }
            writeDeviceIdXml(new File(sharedPrefs, "device_id.xml"), newUuid);

            deleteRecursive(new File(base, "databases"));

            try {
                deleteRecursive(new File(BEnvironment.getExternalUserDir(userId), "Documents"));
            } catch (Throwable ignored) {
            }

            File intFiles = BEnvironment.getDataFilesDir(packageName, userId);
            File extFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
            deleteRecursive(new File(intFiles, "login-identifier.txt"));
            deleteRecursive(new File(extFiles, "login-identifier.txt"));

            clearAndBlock(new File(intFiles,
                    "UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Intermediate"));
            clearAndBlock(new File(extFiles,
                    "UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Intermediate"));
            clearAndBlock(new File(intFiles, "TGPA"));
            clearAndBlock(new File(extFiles, "TGPA"));
            clearAndBlock(new File(intFiles, "ProgramBinaryCache"));
            clearAndBlock(new File(extFiles, "ProgramBinaryCache"));

            GuestLoginHelper.deployToGuest(context.getApplicationContext(), packageName);

            String backupPath = GuestAccountBackupHelper.saveBackupAndDump(context, snap, newUuid);
            Log.i(TAG, "resetGuestAccountBgmiClone OK base=" + base.getAbsolutePath()
                    + " backup=" + backupPath);
            return new ResetGuestResult(true, backupPath);
        } catch (Throwable t) {
            Log.e(TAG, "resetGuestAccountBgmiClone failed", t);
            return new ResetGuestResult(false, null);
        }
    }

    public static String getCloneDataPath(Context context, String packageName, int userId) {
        if (context == null) {
            return "";
        }
        File dir = resolveCloneDataDir(context.getApplicationContext(), packageName, userId);
        return dir != null ? dir.getAbsolutePath() : "";
    }

    /** Same layout as {@link BEnvironment#getDataDir}. */
    private static File resolveCloneDataDir(Context app, String packageName, int userId) {
        try {
            BEnvironment.load();
            return BEnvironment.getDataDir(packageName, userId);
        } catch (Throwable t) {
            Log.w(TAG, "BEnvironment fallback: " + t.getMessage());
        }
        File cacheParent = app.getCacheDir().getParentFile();
        if (cacheParent == null) {
            return null;
        }
        return new File(new File(app.getFilesDir(), BEnvironment.VIRTUAL_ROOT_DIR),
                String.format(Locale.US, "data/user/%d/%s", userId, packageName));
    }

    private static void writeDeviceIdXml(File target, String uuid) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                + "<map>\n"
                + "    <string name=\"random\"></string>\n"
                + "    <string name=\"install\"></string>\n"
                + "    <string name=\"uuid\">" + uuid + "</string>\n"
                + "</map>\n";
        try (FileOutputStream out = new FileOutputStream(target, false)) {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Same shape as shell {@code $RANDOM$RANDOM-$RANDOM-...} guest device id. */
    private static String generateGuestUuid() {
        SecureRandom r = new SecureRandom();
        return String.format(Locale.US, "%d%d-%d-%d-%d-%d%d%d",
                r.nextInt(32768), r.nextInt(32768),
                r.nextInt(32768),
                r.nextInt(32768),
                r.nextInt(32768),
                r.nextInt(32768), r.nextInt(32768), r.nextInt(32768));
    }

    /** Delete path then create empty file (touch) so game cannot recreate as folder. */
    private static void clearAndBlock(File path) {
        deleteRecursive(path);
        try {
            File parent = path.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!path.createNewFile() && !path.isFile()) {
                path.delete();
                path.createNewFile();
            }
        } catch (IOException e) {
            Log.w(TAG, "clearAndBlock: " + path, e);
        }
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
