package com.anubis.loader.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.system.pm.BPackageManagerService;
import com.anubis.loader.core.system.pm.BPackageSettings;

/**
 * Ensures guest native libraries exist under the virtual app lib tree.
 * UE split APKs (e.g. {@code split_config.arm64_v8a.apk}) hold most game .so files.
 */
public final class GuestNativeLibs {

    private static final String TAG = "GuestNativeLibs";

    private GuestNativeLibs() {
    }

    public static void ensureExtracted(String packageName, ApplicationInfo guestAppInfo) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        if (hasNativeLibs(BEnvironment.getAppLibDir(packageName))) {
            return;
        }
        File resolved = BEnvironment.resolveNativeLibDir(packageName);
        if (hasNativeLibs(resolved)) {
            return;
        }
        File libRoot = BEnvironment.getAppLibDir(packageName);
        FileUtils.mkdirs(libRoot);
        try {
            ApplicationInfo hostAi = resolveHostApplicationInfo(packageName);
            if (hostAi != null && copyFromHostLibDir(hostAi, libRoot)) {
                Slog.i(TAG, "linked host native libs pkg=" + packageName + " from=" + hostAi.nativeLibraryDir);
            } else {
                extractFromApks(packageName, guestAppInfo, hostAi, libRoot);
            }
            File after = BEnvironment.resolveNativeLibDir(packageName);
            Slog.i(TAG, "native libs ready pkg=" + packageName + " dir=" + after + " count=" + countNativeLibs(after));
        } catch (Exception e) {
            Slog.e(TAG, "ensureExtracted failed pkg=" + packageName + ": " + e.getMessage());
        }
    }

    private static ApplicationInfo resolveHostApplicationInfo(String packageName) {
        try {
            return AnubisCore.getContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static void extractFromApks(String packageName, ApplicationInfo guestAppInfo,
                                        ApplicationInfo hostAi, File libRoot) {
        String apkPath = null;
        try {
            BPackageSettings settings = BPackageManagerService.get().getBPackageSetting(packageName);
            if (settings != null && settings.pkg != null) {
                apkPath = settings.pkg.baseCodePath;
            }
        } catch (Throwable ignored) {
        }
        if ((apkPath == null || apkPath.isEmpty()) && guestAppInfo != null) {
            apkPath = guestAppInfo.sourceDir;
        }
        if (apkPath != null && !apkPath.isEmpty()) {
            try {
                NativeUtils.copyNativeLib(new File(apkPath), libRoot);
                Slog.i(TAG, "extracted base native libs pkg=" + packageName);
            } catch (Exception e) {
                Slog.w(TAG, "base native extract failed: " + e.getMessage());
            }
        }
        String[] splits = null;
        if (hostAi != null && hostAi.splitSourceDirs != null && hostAi.splitSourceDirs.length > 0) {
            splits = hostAi.splitSourceDirs;
        } else if (guestAppInfo != null && guestAppInfo.splitSourceDirs != null) {
            splits = guestAppInfo.splitSourceDirs;
        }
        if (splits != null) {
            for (String split : splits) {
                if (split == null) {
                    continue;
                }
                try {
                    NativeUtils.copyNativeLib(new File(split), libRoot);
                    Slog.i(TAG, "extracted split native libs pkg=" + packageName + " split=" + split);
                } catch (Exception e) {
                    Slog.w(TAG, "split native extract failed " + split + ": " + e.getMessage());
                }
            }
        }
    }

    private static boolean copyFromHostLibDir(ApplicationInfo hostAi, File libRoot) {
        if (hostAi == null || hostAi.nativeLibraryDir == null) {
            return false;
        }
        File hostLib = new File(hostAi.nativeLibraryDir);
        if (!hostLib.isDirectory() || countNativeLibs(hostLib) == 0) {
            File parent = hostAi.sourceDir != null ? new File(hostAi.sourceDir).getParentFile() : null;
            if (parent != null) {
                File arm64 = new File(parent, "lib/arm64");
                if (arm64.isDirectory() && countNativeLibs(arm64) > 0) {
                    hostLib = arm64;
                }
            }
        }
        if (!hostLib.isDirectory() || countNativeLibs(hostLib) == 0) {
            return false;
        }
        File[] libs = hostLib.listFiles((dir, name) -> name != null && name.endsWith(".so"));
        if (libs == null || libs.length == 0) {
            return false;
        }
        for (File lib : libs) {
            File dest = new File(libRoot, lib.getName());
            if (dest.exists() && dest.length() == lib.length()) {
                continue;
            }
            try {
                linkOrCopy(dest, lib);
            } catch (Exception e) {
                Slog.w(TAG, "host lib copy failed " + lib.getName() + ": " + e.getMessage());
            }
        }
        return countNativeLibs(libRoot) > 0;
    }

    private static void linkOrCopy(File dest, File src) throws Exception {
        File parent = dest.getParentFile();
        if (parent != null) {
            FileUtils.mkdirs(parent);
        }
        if (dest.exists()) {
            dest.delete();
        }
        try {
            android.system.Os.symlink(src.getAbsolutePath(), dest.getAbsolutePath());
        } catch (Throwable e) {
            FileUtils.copyFile(src, dest);
        }
    }

    private static boolean hasNativeLibs(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        if (countNativeLibs(dir) > 0) {
            return true;
        }
        File[] children = dir.listFiles(File::isDirectory);
        if (children == null) {
            return false;
        }
        for (File child : children) {
            if (countNativeLibs(child) > 0) {
                return true;
            }
        }
        return false;
    }

    private static int countNativeLibs(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return 0;
        }
        File[] sos = dir.listFiles((d, name) -> name != null && name.endsWith(".so"));
        return sos != null ? sos.length : 0;
    }
}
