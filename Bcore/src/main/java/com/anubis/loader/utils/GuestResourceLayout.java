package com.anubis.loader.utils;

import java.io.File;

import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.GamePackages;

/**
 * Pre-create game resource dirs on the same paths as IO redirect rules (valo loader parity).
 */
public final class GuestResourceLayout {

    private static final String TAG = "GuestResourceLayout";

    private GuestResourceLayout() {
    }

    public static void prepare(String packageName, int userId) {
        if (!VirtualPathSpoof.isStealthAcPackage(packageName)) {
            return;
        }
        try {
            File dataRoot = BEnvironment.getDataDir(packageName, userId);
            File files = BEnvironment.getDataFilesDir(packageName, userId);
            File extRoot = BEnvironment.getExternalDataDir(packageName, userId);
            File extFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
            File obb = BEnvironment.getObbDir(packageName);

            FileUtils.mkdirs(dataRoot);
            FileUtils.mkdirs(files);
            FileUtils.mkdirs(extRoot);
            FileUtils.mkdirs(extFiles);
            FileUtils.mkdirs(obb);
            chmodDir(dataRoot);
            chmodDir(files);
            chmodDir(extRoot);
            chmodDir(extFiles);
            FileUtils.mkdirs(obb);
            chmodDir(dataRoot);
            chmodDir(files);
            chmodDir(extRoot);
            chmodDir(extFiles);
            chmodDir(obb);

            if (!GamePackages.isPackageDataGame(packageName) && !GamePackages.isDirectLaunchGame(packageName)) {
                ObbUtils.ensureObbPresent(packageName, userId);
            }
            mirrorUe4Resources(packageName, userId, files, extFiles);

            if (GCloudPathHelper.isDeltaForcePackage(packageName)) {
                File dolphin = new File(files, "UE4Game/DeltaForce/DeltaForce/Saved/Dolphin");
                File puffer = new File(files, "UE4Game/DeltaForce/DeltaForce/Saved/Puffer");
                File extDolphin = new File(extFiles, "UE4Game/DeltaForce/DeltaForce/Saved/Dolphin");
                File extPuffer = new File(extFiles, "UE4Game/DeltaForce/DeltaForce/Saved/Puffer");
                File pufferTemp = new File(puffer, "puffer_temp");
                FileUtils.mkdirs(dolphin);
                FileUtils.mkdirs(puffer);
                FileUtils.mkdirs(extDolphin);
                FileUtils.mkdirs(extPuffer);
                chmodDir(dolphin);
                chmodDir(puffer);
                chmodDir(extDolphin);
                chmodDir(extPuffer);
                resetPufferTemp(pufferTemp);
                GCloudPathHelper.seedFromHostIfNeeded(packageName, userId);
                GCloudPathHelper.ensureResourceAccess(packageName, userId);
                if (!StealthMode.shouldSuppressLogcat()) {
                    Slog.i(TAG, "prepared pkg=" + packageName
                            + " dataFiles=" + files.getAbsolutePath()
                            + " extData=" + extRoot.getAbsolutePath()
                            + " dolphin=" + dolphin.getAbsolutePath()
                            + " puffer=" + puffer.getAbsolutePath());
                }
            } else if (!StealthMode.shouldSuppressLogcat()) {
                Slog.i(TAG, "prepared pkg=" + packageName
                        + " dataFiles=" + files.getAbsolutePath()
                        + " extData=" + extRoot.getAbsolutePath());
            }
        } catch (Throwable t) {
            Slog.w(TAG, "prepare failed pkg=" + packageName + ": " + t.getMessage());
        }
    }

    private static void mirrorUe4Resources(String packageName, int userId, File intFiles, File extFiles) {
        File intUe4 = new File(intFiles, "UE4Game");
        File extUe4 = new File(extFiles, "UE4Game");
        if (intUe4.isDirectory() && dirSize(intUe4) > dirSize(extUe4)) {
            mergeTree(intUe4, extUe4);
        } else if (extUe4.isDirectory() && dirSize(extUe4) > dirSize(intUe4)) {
            mergeTree(extUe4, intUe4);
        }
        if (GamePackages.isBgmi(packageName)) {
            File intPaks = new File(intFiles, "UE4Game/ShadowTrackerExtra/Saved/Paks");
            File extPaks = new File(extFiles, "UE4Game/ShadowTrackerExtra/Saved/Paks");
            if (intPaks.isDirectory() && dirSize(intPaks) > dirSize(extPaks)) {
                mergeTree(intPaks, extPaks);
            } else if (extPaks.isDirectory() && dirSize(extPaks) > dirSize(intPaks)) {
                mergeTree(extPaks, intPaks);
            }
        }
    }

    private static long dirSize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }
        if (dir.isFile()) {
            return dir.length();
        }
        long total = 0;
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        for (File child : children) {
            total += dirSize(child);
        }
        return total;
    }

    private static void mergeTree(File source, File dest) {
        if (source == null || dest == null || !source.isDirectory()) {
            return;
        }
        FileUtils.mkdirs(dest);
        File[] children = source.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            File target = new File(dest, child.getName());
            if (child.isDirectory()) {
                mergeTree(child, target);
            } else if (!target.isFile() || target.length() != child.length()) {
                try {
                    FileUtils.copyFile(child, target);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void resetPufferTemp(File pufferTemp) {
        if (pufferTemp.exists()) {
            FileUtils.deleteDir(pufferTemp);
        }
        FileUtils.mkdirs(pufferTemp);
        chmodDir(pufferTemp);
    }

    private static void chmodDir(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        try {
            FileUtils.chmod(dir.getAbsolutePath(), 0755);
        } catch (Throwable ignored) {
        }
    }
}
