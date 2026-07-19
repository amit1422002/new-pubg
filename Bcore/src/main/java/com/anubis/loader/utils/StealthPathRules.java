package com.anubis.loader.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.ContainerPathStealth;
import com.anubis.loader.core.system.pm.BPackage;
import com.anubis.loader.core.system.pm.BPackageManagerService;
import com.anubis.loader.core.system.pm.BPackageSettings;

/**
 * Global guest stealth: symlink host install tree into virtual tree and register IO rules.
 * Uses internal BPackage paths (real host install), not spoofed ApplicationInfo.
 */
public final class StealthPathRules {

    private static final String TAG = "STEALTH_PATH";
    private static final ThreadLocal<Boolean> sPreparingInstall = new ThreadLocal<>();

    private StealthPathRules() {
    }

    public static void addRules(String packageName, int userId, Map<String, String> rule) {
        if (!VirtualPathSpoof.isStealthAcPackage(packageName)) {
            return;
        }
        ensureHostInstallReady(packageName, userId);
        try {
            BPackageSettings settings = BPackageManagerService.get().getBPackageSetting(packageName);
            if (settings == null || settings.pkg == null) {
                return;
            }
            BPackage pkg = settings.pkg;
            String hostApk = pkg.baseCodePath;
            if (hostApk == null) {
                return;
            }
            ApplicationInfo hostAi = null;
            try {
                hostAi = AnubisCore.getContext().getPackageManager()
                        .getPackageInfo(packageName, PackageManager.GET_META_DATA).applicationInfo;
            } catch (Throwable ignored) {
            }
            if (hostAi == null) {
                try {
                    hostAi = AnubisCore.getContext().getPackageManager()
                            .getPackageArchiveInfo(hostApk, PackageManager.GET_META_DATA).applicationInfo;
                } catch (Throwable ignored) {
                }
            }
            linkHostInstallTree(packageName, hostApk, hostAi, rule);
            ensureNativeLibs(packageName, hostApk, hostAi);
            addExternalDataRules(packageName, userId, rule);
            Slog.i(TAG, "host install linked pkg=" + packageName + " apk=" + hostApk);
        } catch (Throwable t) {
            Slog.w(TAG, "host install link failed pkg=" + packageName + ": " + t.getMessage());
        }
    }

    /** Create virtual base.apk (+ libs) before stub initProcess — FLAG_SYSTEM installs skip CopyExecutor copy. */
    public static void ensureHostInstallReady(String packageName, int userId) {
        if (!VirtualPathSpoof.isStealthAcPackage(packageName)) {
            return;
        }
        if (Boolean.TRUE.equals(sPreparingInstall.get())) {
            return;
        }
        sPreparingInstall.set(Boolean.TRUE);
        try {
            ensureHostInstallReadyInner(packageName, userId);
        } finally {
            sPreparingInstall.remove();
        }
    }

    /** Materialize host GMS APK + libs into vfs — microG slim builds fail chimera makeApplication. */
    public static void ensureGmsInstallReady(int userId) {
        if (Boolean.TRUE.equals(sPreparingInstall.get())) {
            return;
        }
        sPreparingInstall.set(Boolean.TRUE);
        try {
            ensureHostInstallReadyInner(GmsCore.GMS_PKG, userId);
        } finally {
            sPreparingInstall.remove();
        }
    }

    private static void ensureHostInstallReadyInner(String packageName, int userId) {
        try {
            scrubBrokenVirtApks(packageName);
            File virtApk = BEnvironment.getBaseApkDir(packageName);
            ApplicationInfo hostAi = resolveHostApplicationInfo(packageName, null);
            if (virtApk.isFile() && virtApk.length() > 1024L
                    && virtApk.getAbsolutePath().contains("/.vfs/")
                    && canReadApk(virtApk)
                    && virtSplitsReady(packageName, hostAi)) {
                markVirtApkReadOnly(virtApk);
                refreshStoredApkPath(packageName, virtApk);
                return;
            }
            if (virtApk.exists()) {
                if (virtApk.isDirectory()) {
                    return;
                }
                makeVirtApkWritable(virtApk);
                virtApk.delete();
            }
            File sourceApk = resolveGuestApkSource(packageName);
            if (sourceApk == null || !sourceApk.isFile()) {
                Slog.w(TAG, "no apk source pkg=" + packageName);
                return;
            }
            if (hostAi == null) {
                hostAi = resolveHostApplicationInfo(packageName, sourceApk);
            }
            materializeIntoVirt(virtApk, sourceApk);
            File virtAppDir = BEnvironment.getAppDir(packageName);
            FileUtils.mkdirs(virtAppDir);
            if (hostAi != null && hostAi.splitSourceDirs != null) {
                for (String split : hostAi.splitSourceDirs) {
                    if (split == null) {
                        continue;
                    }
                    File hostSplit = new File(split);
                    if (!hostSplit.isFile()) {
                        continue;
                    }
                    File virtSplit = new File(virtAppDir, hostSplit.getName());
                    materializeIntoVirt(virtSplit, hostSplit);
                }
            }
            ensureNativeLibs(packageName, virtApk.getAbsolutePath(), hostAi);
            refreshStoredApkPath(packageName, virtApk);
            Slog.i(TAG, "host install ready pkg=" + packageName + " apk=" + virtApk.getAbsolutePath()
                    + " src=" + sourceApk.getAbsolutePath());
        } catch (Throwable t) {
            Slog.w(TAG, "ensureHostInstallReady failed pkg=" + packageName + ": " + t.getMessage());
        }
    }

    private static boolean virtSplitsReady(String packageName, ApplicationInfo hostAi) {
        if (hostAi == null || hostAi.splitSourceDirs == null || hostAi.splitSourceDirs.length == 0) {
            return true;
        }
        File virtAppDir = BEnvironment.getAppDir(packageName);
        for (String split : hostAi.splitSourceDirs) {
            if (split == null) {
                continue;
            }
            File virtSplit = new File(virtAppDir, new File(split).getName());
            if (!virtSplit.isFile() || !canReadApk(virtSplit)) {
                return false;
            }
        }
        return true;
    }

    /** Copy host/device APK into .vfs; symlink only when source is already inside .vfs. */
    private static void materializeIntoVirt(File dest, File source) throws IOException {
        if (source == null || !source.isFile()) {
            return;
        }
        File parent = dest.getParentFile();
        if (parent != null) {
            FileUtils.mkdirs(parent);
        }
        if (dest.exists()) {
            if (dest.isDirectory()) {
                return;
            }
            dest.delete();
        }
        if (source.getAbsolutePath().contains("/.vfs/")) {
            linkInto(dest, source);
        } else {
            FileUtils.copyFile(source, dest);
        }
        markVirtApkReadOnly(dest);
    }

    /** Android 14+ rejects dex opened from writable app_data APK copies. */
    public static void ensureVirtApkReadOnly(String packageName) {
        if (packageName == null) {
            return;
        }
        markVirtApkReadOnly(BEnvironment.getBaseApkDir(packageName));
        File appDir = BEnvironment.getAppDir(packageName);
        File[] splits = appDir.listFiles((dir, name) -> name != null && name.endsWith(".apk"));
        if (splits != null) {
            for (File split : splits) {
                markVirtApkReadOnly(split);
            }
        }
    }

    private static void makeVirtApkWritable(File apk) {
        if (apk == null || !apk.isFile()) {
            return;
        }
        try {
            FileUtils.chmod(apk.getAbsolutePath(), 0644);
        } catch (Throwable ignored) {
        }
    }

    private static void markVirtApkReadOnly(File apk) {
        if (apk == null || !apk.isFile()) {
            return;
        }
        try {
            apk.setReadOnly();
            FileUtils.chmod(apk.getAbsolutePath(), 0444);
        } catch (Throwable ignored) {
        }
    }

    private static void scrubBrokenVirtApks(String packageName) {
        File appDir = BEnvironment.getAppDir(packageName);
        File[] apks = appDir.listFiles((dir, name) -> name != null && name.endsWith(".apk"));
        if (apks == null) {
            return;
        }
        for (File apk : apks) {
            if (!apk.exists()) {
                continue;
            }
            if (isSymlink(apk) || !canReadApk(apk) || isSelfReferentialLink(apk)) {
                apk.delete();
            }
        }
    }

    private static boolean isSymlink(File file) {
        try {
            Os.readlink(file.getAbsolutePath());
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    private static boolean isSelfReferentialLink(File file) {
        try {
            String target = Os.readlink(file.getAbsolutePath());
            String abs = file.getAbsolutePath();
            return abs.equals(target) || abs.equals(new File(target).getAbsolutePath());
        } catch (ErrnoException e) {
            return false;
        }
    }

    private static boolean canReadApk(File apk) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(apk)) {
            return in.read() >= 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ApplicationInfo resolveHostApplicationInfo(String packageName, File sourceApk) {
        VirtualPathSpoof.beginInternalBind();
        try {
            try {
                return AnubisCore.getContext().getPackageManager()
                        .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            } catch (Throwable ignored) {
            }
            try {
                PackageManager pm = AnubisCore.getContext().getPackageManager();
                android.content.pm.PackageInfo pi = pm.getPackageArchiveInfo(
                        sourceApk.getAbsolutePath(), PackageManager.GET_META_DATA);
                if (pi != null) {
                    return pi.applicationInfo;
                }
            } catch (Throwable ignored) {
            }
            return null;
        } finally {
            VirtualPathSpoof.endInternalBind();
        }
    }

    /**
     * Resolve a readable APK for the clone: virtual copy, live device install, or stored BPackage path.
     * Never returns a stale hashed path when the file is already gone.
     */
    public static File resolveGuestApkSource(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        File virtApk = BEnvironment.getBaseApkDir(packageName);
        if (virtApk.isFile() && virtApk.length() > 1024L && canReadApk(virtApk)) {
            return virtApk;
        }
        try {
            VirtualPathSpoof.beginInternalBind();
            try {
                ApplicationInfo installed = AnubisCore.getContext().getPackageManager()
                        .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                if (installed != null && installed.sourceDir != null) {
                    File live = new File(installed.sourceDir);
                    if (live.isFile() && live.length() > 1024L) {
                        return live;
                    }
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            } catch (Throwable ignored) {
            }
        } finally {
            VirtualPathSpoof.endInternalBind();
        }
        try {
            BPackageSettings settings = BPackageManagerService.get().getBPackageSetting(packageName);
            if (settings != null && settings.pkg != null && settings.pkg.baseCodePath != null) {
                File stored = new File(settings.pkg.baseCodePath);
                if (stored.isFile() && stored.length() > 1024L) {
                    return stored;
                }
            }
        } catch (Throwable ignored) {
        }
        File appDir = BEnvironment.getAppDir(packageName);
        File named = new File(appDir, "base.apk");
        if (named.isFile() && named.length() > 1024L) {
            return named;
        }
        File[] apks = appDir.listFiles((dir, name) -> name != null && name.endsWith(".apk"));
        if (apks != null) {
            for (File apk : apks) {
                if (apk.isFile() && apk.length() > 1024L) {
                    return apk;
                }
            }
        }
        return null;
    }

    private static void refreshStoredApkPath(String packageName, File sourceApk) {
        try {
            BPackageSettings settings = BPackageManagerService.get().getBPackageSetting(packageName);
            if (settings == null || settings.pkg == null || sourceApk == null) {
                return;
            }
            String resolved = sourceApk.getAbsolutePath();
            if (!resolved.equals(settings.pkg.baseCodePath)) {
                settings.pkg.baseCodePath = resolved;
                Slog.i(TAG, "refreshed baseCodePath pkg=" + packageName + " -> " + resolved);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addExternalDataRules(String packageName, int userId, Map<String, String> rule) {
        File guestExtData = BEnvironment.getExternalDataDir(packageName, userId);
        File guestExtFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
        File guestExtCache = BEnvironment.getExternalDataCacheDir(packageName, userId);
        File guestObb = BEnvironment.getObbDir(packageName);
        int hostUserId = AnubisCore.getHostUserId();

        String guestExtPath = guestExtData.getAbsolutePath();
        String fakeExt = VirtualPathSpoof.fakeExternalDataDir(packageName, userId);
        String fakeObb = VirtualPathSpoof.fakeObbDir(packageName, userId);

        rule.put(String.format("/storage/emulated/%d/Android/data/%s", hostUserId, packageName), guestExtPath);
        rule.put(fakeExt, guestExtPath);
        rule.put(fakeExt + "/files", guestExtFiles.getAbsolutePath());
        rule.put(fakeExt + "/cache", guestExtCache.getAbsolutePath());
        rule.put("/sdcard/Android/data/" + packageName, guestExtPath);
        rule.put(String.format("/storage/emulated/%d/Android/obb/%s", hostUserId, packageName),
                guestObb.getAbsolutePath());
        rule.put(fakeObb, guestObb.getAbsolutePath());
    }

    /** Fake guest-visible data/lib paths → real virtual tree (IO redirect only; same process). */
    public static void addFakePathRules(String packageName, int userId, ApplicationInfo packageInfo,
                                        Map<String, String> rule) {
        if (packageInfo == null) {
            return;
        }
        String fakeData = VirtualPathSpoof.fakeDataDir(packageName, userId);
        String fakeDe = VirtualPathSpoof.fakeDeviceProtectedDataDir(packageName, userId);
        String fakeLib = VirtualPathSpoof.fakeNativeLibDir(packageName);
        File realDe = BEnvironment.getDeDataDir(packageName, userId);

        rule.put(fakeData, packageInfo.dataDir);
        rule.put(fakeData + "/files", BEnvironment.getDataFilesDir(packageName, userId).getAbsolutePath());
        rule.put(fakeData + "/cache", BEnvironment.getDataCacheDir(packageName, userId).getAbsolutePath());
        rule.put(fakeData + "/databases", BEnvironment.getDataDatabasesDir(packageName, userId).getAbsolutePath());
        rule.put(fakeDe, realDe.getAbsolutePath());
        File virtLib = BEnvironment.resolveNativeLibDir(packageName);
        rule.put(fakeLib, virtLib.getAbsolutePath());
        File[] libs = virtLib.listFiles((dir, name) -> name != null && name.endsWith(".so"));
        if (libs != null) {
            for (File lib : libs) {
                rule.put(fakeLib + "/" + lib.getName(), lib.getAbsolutePath());
            }
        }
    }

    public static void addHostLeakGuards(String packageName, int userId, Map<String, String> rule) {
        if (!VirtualPathSpoof.isStealthAcPackage(packageName)) {
            return;
        }
        String hostPkg = AnubisCore.getHostPkg();
        int hostUid = AnubisCore.getHostUserId();
        File guestData = BEnvironment.getDataDir(packageName, userId);
        String hostRoot = String.format(java.util.Locale.US, "/data/user/%d/%s", hostUid, hostPkg);
        String hostData = String.format(java.util.Locale.US, "/data/data/%s", hostPkg);

        rule.put(hostRoot + "/shared_prefs", new File(guestData, "shared_prefs").getAbsolutePath());
        rule.put(hostData + "/shared_prefs", new File(guestData, "shared_prefs").getAbsolutePath());
        rule.put(hostRoot + "/files", BEnvironment.getDataFilesDir(packageName, userId).getAbsolutePath());
        rule.put(hostRoot + "/cache", BEnvironment.getDataCacheDir(packageName, userId).getAbsolutePath());
        rule.put(hostRoot + "/databases", BEnvironment.getDataDatabasesDir(packageName, userId).getAbsolutePath());

        File profilesRoot = new File(BEnvironment.getVirtualRoot(), "profiles");
        rule.put(hostRoot + "/files/.vfs/profiles", profilesRoot.getAbsolutePath());

        File hostExtCache = AnubisCore.getContext().getExternalCacheDir();
        if (hostExtCache != null) {
            rule.put(hostExtCache.getAbsolutePath(),
                    BEnvironment.getDataCacheDir(packageName, userId).getAbsolutePath());
        }
        redirectDeviceInstallIfPresent(packageName, rule);

        int sysUser = AnubisCore.getHostUserId();
        String canonical = ContainerPathStealth.canonicalDataDir(packageName, sysUser);
        File virtApp = BEnvironment.getAppDir(packageName);
        File virtApk = BEnvironment.getBaseApkDir(packageName);
        File virtLib = BEnvironment.resolveNativeLibDir(packageName);
        String leakApp = canonical + "/files/data/app/" + packageName;
        rule.put(leakApp, virtApp.getAbsolutePath());
        rule.put(leakApp + "/", virtApp.getAbsolutePath() + "/");
        rule.put(leakApp + "/base.apk", virtApk.getAbsolutePath());
        rule.put(leakApp + "/lib", virtLib.getParentFile() != null
                ? virtLib.getParentFile().getAbsolutePath() : virtLib.getAbsolutePath());
        rule.put(leakApp + "/lib/", (virtLib.getParentFile() != null
                ? virtLib.getParentFile() : virtLib).getAbsolutePath() + "/");

        String nestedUnderFakeData = canonical + "/data/app/" + packageName;
        rule.put(nestedUnderFakeData, virtApp.getAbsolutePath());
        rule.put(nestedUnderFakeData + "/base.apk", virtApk.getAbsolutePath());
        rule.put(nestedUnderFakeData + "/lib", virtLib.getParentFile() != null
                ? virtLib.getParentFile().getAbsolutePath() : virtLib.getAbsolutePath());
        rule.put(nestedUnderFakeData + "/lib/", (virtLib.getParentFile() != null
                ? virtLib.getParentFile() : virtLib).getAbsolutePath() + "/");

        String leakStorage = canonical + "/files/storage/emulated";
        File extRoot = BEnvironment.getExternalUserDir(userId);
        rule.put(leakStorage, extRoot.getAbsolutePath());
        rule.put(leakStorage + "/" + userId, extRoot.getAbsolutePath());
        rule.put(leakStorage + "/0", BEnvironment.getExternalUserDir(0).getAbsolutePath());

        // UE4/NERTC: fake dataDir + "/storage/emulated/0/Android/data/<pkg>/files/..."
        String leakStorageDirect = canonical + "/storage/emulated";
        File guestExtData = BEnvironment.getExternalDataDir(packageName, userId);
        File guestExtFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
        rule.put(leakStorageDirect, extRoot.getAbsolutePath());
        rule.put(leakStorageDirect + "/" + userId, extRoot.getAbsolutePath());
        rule.put(leakStorageDirect + "/0", BEnvironment.getExternalUserDir(0).getAbsolutePath());
        rule.put(leakStorageDirect + "/0/Android/data/" + packageName, guestExtData.getAbsolutePath());
        rule.put(leakStorageDirect + "/0/Android/data/" + packageName + "/files",
                guestExtFiles.getAbsolutePath());
    }

    /** When the same game is installed on the host, native open() on that path must hit the virtual tree. */
    private static void redirectDeviceInstallIfPresent(String packageName, Map<String, String> rule) {
        try {
            ApplicationInfo installed = AnubisCore.getContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (installed == null || installed.sourceDir == null) {
                return;
            }
            File virtApk = BEnvironment.getBaseApkDir(packageName);
            File virtAppDir = BEnvironment.getAppDir(packageName);
            rule.put(installed.sourceDir, virtApk.getAbsolutePath());
            File hostApk = new File(installed.sourceDir);
            File hostDir = hostApk.getParentFile();
            if (hostDir != null) {
                rule.put(hostDir.getAbsolutePath(), virtAppDir.getAbsolutePath());
            }
            if (installed.splitSourceDirs != null) {
                for (String split : installed.splitSourceDirs) {
                    if (split == null) {
                        continue;
                    }
                    File hostSplit = new File(split);
                    File virtSplit = new File(virtAppDir, hostSplit.getName());
                    rule.put(split, virtSplit.getAbsolutePath());
                }
            }
            if (installed.nativeLibraryDir != null) {
                rule.put(installed.nativeLibraryDir,
                        BEnvironment.resolveNativeLibDir(packageName).getAbsolutePath());
            }
            Slog.i(TAG, "device install redirect pkg=" + packageName + " apk=" + installed.sourceDir);
        } catch (PackageManager.NameNotFoundException ignored) {
        } catch (Throwable t) {
            Slog.w(TAG, "device install redirect failed pkg=" + packageName + ": " + t.getMessage());
        }
    }

    private static void linkHostInstallTree(String packageName, String hostApk, ApplicationInfo hostAi,
                                            Map<String, String> rule) throws IOException {
        File hostApkFile = new File(hostApk);
        File virtApk = BEnvironment.getBaseApkDir(packageName);
        if (!canReadApk(virtApk)) {
            materializeIntoVirt(virtApk, hostApkFile);
        }
        rule.put(hostApk, virtApk.getAbsolutePath());

        File virtAppDir = BEnvironment.getAppDir(packageName);
        FileUtils.mkdirs(virtAppDir);
        rule.put("/data/app/" + packageName, virtAppDir.getAbsolutePath());
        rule.put(VirtualPathSpoof.fakeApkPath(packageName), virtApk.getAbsolutePath());
        rule.put(VirtualPathSpoof.fakeApkPath(packageName) + "/", virtApk.getParentFile().getAbsolutePath() + "/");
        rule.put(VirtualPathSpoof.fakeNativeLibDir(packageName),
                BEnvironment.resolveNativeLibDir(packageName).getAbsolutePath());
        File hostInstallDir = hostApkFile.getParentFile();
        if (hostInstallDir != null) {
            rule.put(hostInstallDir.getAbsolutePath(), virtAppDir.getAbsolutePath());
        }

        if (hostAi != null && hostAi.splitSourceDirs != null) {
            for (String split : hostAi.splitSourceDirs) {
                if (split == null) {
                    continue;
                }
                File hostSplit = new File(split);
                File virtSplit = new File(virtAppDir, hostSplit.getName());
                if (!canReadApk(virtSplit)) {
                    materializeIntoVirt(virtSplit, hostSplit);
                }
                rule.put(split, virtSplit.getAbsolutePath());
                rule.put("/data/app/" + packageName + "/" + hostSplit.getName(),
                        virtSplit.getAbsolutePath());
            }
        }

        File hostLib = resolveHostLibDir(hostApkFile, hostAi);
        if (hostLib != null && hostLib.isDirectory()) {
            File virtLib = BEnvironment.resolveNativeLibDir(packageName);
            FileUtils.mkdirs(virtLib);
            File[] libs = hostLib.listFiles((dir, name) -> name != null && name.endsWith(".so"));
            if (libs != null) {
                for (File lib : libs) {
                    File virtSo = new File(virtLib, lib.getName());
                    linkInto(virtSo, lib);
                    rule.put(lib.getAbsolutePath(), virtSo.getAbsolutePath());
                }
            }
            rule.put(hostLib.getAbsolutePath(), virtLib.getAbsolutePath());
        }
    }

    private static void ensureNativeLibs(String packageName, String hostApk, ApplicationInfo hostAi) {
        File libRoot = BEnvironment.getAppLibDir(packageName);
        try {
            NativeUtils.copyNativeLib(new File(hostApk), libRoot);
        } catch (Exception e) {
            Slog.w(TAG, "base native copy failed pkg=" + packageName + ": " + e.getMessage());
        }
        if (hostAi != null && hostAi.splitSourceDirs != null) {
            for (String split : hostAi.splitSourceDirs) {
                if (split == null) {
                    continue;
                }
                try {
                    NativeUtils.copyNativeLib(new File(split), libRoot);
                } catch (Exception e) {
                    Slog.w(TAG, "split native copy failed " + split + ": " + e.getMessage());
                }
            }
        }
    }

    private static File resolveHostLibDir(File hostApkFile, ApplicationInfo hostAi) {
        if (hostAi != null && hostAi.nativeLibraryDir != null) {
            File lib = new File(hostAi.nativeLibraryDir);
            if (lib.isDirectory()) {
                return lib;
            }
        }
        File parent = hostApkFile.getParentFile();
        if (parent == null) {
            return null;
        }
        File arm64 = new File(parent, "lib/arm64");
        if (arm64.isDirectory()) {
            return arm64;
        }
        File arm = new File(parent, "lib/arm");
        if (arm.isDirectory()) {
            return arm;
        }
        return new File(parent, "lib");
    }

    private static void linkInto(File link, File target) throws IOException {
        if (target == null || !target.exists()) {
            return;
        }
        String linkPath = link.getAbsolutePath();
        String targetPath = target.getAbsolutePath();
        if (linkPath.equals(targetPath)) {
            FileUtils.copyFile(target, link);
            return;
        }
        File parent = link.getParentFile();
        if (parent != null) {
            FileUtils.mkdirs(parent);
        }
        if (link.exists()) {
            if (link.isDirectory()) {
                return;
            }
            link.delete();
        }
        try {
            Os.symlink(target.getAbsolutePath(), link.getAbsolutePath());
        } catch (ErrnoException e) {
            if (!link.exists()) {
                FileUtils.copyFile(target, link);
            }
        }
    }
}
