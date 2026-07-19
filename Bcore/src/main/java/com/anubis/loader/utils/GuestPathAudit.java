package com.anubis.loader.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.IOCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.GamePackages;

/**
 * Debug path audit for cloned games — logcat filters:
 * {@value #TAG_FARLIGHT}, {@value #TAG_DELTA}
 */
public final class GuestPathAudit {

    public static final String TAG_FARLIGHT = "FARLIGHT_PATH";
    public static final String TAG_DELTA = "DELTA_PATH";

    /** @deprecated Use {@link #TAG_FARLIGHT}. */
    public static final String TAG = TAG_FARLIGHT;

    private static final String UE_DELTA_SAVED = "UE4Game/DeltaForce/DeltaForce/Saved";

    private static ThreadLocal<String> sActiveTag = new ThreadLocal<>();

    private GuestPathAudit() {
    }

    public static void logIfFarlight(Context context, String packageName, int userId) {
        // Startup audit spam hits liblog thousands of times — skip for UE4 FPS.
    }

    public static void logIfDeltaForce(Context context, String packageName, int userId) {
        if (!GamePackages.isDeltaForce(packageName)) {
            return;
        }
        runAudit(context, packageName, userId, TAG_DELTA, "delta_path_audit.txt", false, true);
    }

    private static boolean isFarlightGray(String packageName) {
        return "com.farlightgames.farlight84.gray".equals(packageName);
    }

    private static void runAudit(Context context, String packageName, int userId,
            String tag, String auditFileName, boolean farlightExtras, boolean deltaExtras) {
        sActiveTag.set(tag);
        try {
            logStartup(context, packageName, userId, auditFileName, farlightExtras, deltaExtras);
        } finally {
            sActiveTag.remove();
        }
    }

    private static void logStartup(Context context, String packageName, int userId,
            String auditFileName, boolean farlightExtras, boolean deltaExtras) {
        logLine("========== PATH AUDIT pkg=" + packageName + " userId=" + userId + " ==========");
        logLine("getGuestPkg() = " + packageName);
        logLine("BActivityThread.getAppPackageName() = " + com.anubis.loader.app.BActivityThread.getAppPackageName());
        logLine("getAttachLoaderPkg() = " + AnubisCore.getAttachLoaderPkg());
        logLine("loaderPkgLiteral() = " + AnubisCore.loaderPkgLiteral());
        logLine("defaultLoaderPackage() = " + AnubisCore.defaultLoaderPackage());
        logLine("getHostPkg() = " + AnubisCore.getHostPkg() + " (loader storage only, game never sees this)");
        logLine("getPinnedLoaderPkg() = " + AnubisCore.getPinnedLoaderPkg());
        logLine("HOST anchor = " + BEnvironment.getHostFilesAnchor().getAbsolutePath());
        logLine("VFS root = " + BEnvironment.getVirtualRoot().getAbsolutePath());

        File dataDir = BEnvironment.getDataDir(packageName, userId);
        File filesDir = BEnvironment.getDataFilesDir(packageName, userId);
        File extData = BEnvironment.getExternalDataDir(packageName, userId);
        File extFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
        File obbDir = BEnvironment.getObbDir(packageName);

        line("REAL dataDir", dataDir);
        line("REAL filesDir", filesDir);
        line("REAL extDataDir", extData);
        line("REAL extFilesDir", extFiles);
        line("REAL obbDir", obbDir);

        if (farlightExtras) {
            line("REAL UE4 internal", new File(filesDir, "UE4Game/Farlight84/Saved"));
            line("REAL UE4 external", new File(extFiles, "UE4Game/Farlight84/Saved"));
            line("REAL BasePaks", new File(filesDir, "BasePaks"));
            line("REAL SolarDownloader", new File(filesDir, "SolarDownloader"));
        }
        if (deltaExtras) {
            line("REAL UE4 Delta int", new File(filesDir, UE_DELTA_SAVED));
            line("REAL UE4 Delta ext", new File(extFiles, UE_DELTA_SAVED));
            line("REAL Dolphin int", new File(filesDir, UE_DELTA_SAVED + "/Dolphin"));
            line("REAL Dolphin ext", new File(extFiles, UE_DELTA_SAVED + "/Dolphin"));
            line("REAL Puffer int", new File(filesDir, UE_DELTA_SAVED + "/Puffer"));
            line("REAL Puffer ext", new File(extFiles, UE_DELTA_SAVED + "/Puffer"));
            line("REAL Delta Content int", new File(filesDir, "UE4Game/DeltaForce/DeltaForce/Content"));
            line("REAL Delta Content ext", new File(extFiles, "UE4Game/DeltaForce/DeltaForce/Content"));
        }

        lineStr("FAKE dataDir", VirtualPathSpoof.fakeDataDir(packageName, userId));
        lineStr("FAKE extDataDir", VirtualPathSpoof.fakeExternalDataDir(packageName, userId));
        lineStr("FAKE obbDir", VirtualPathSpoof.fakeObbDir(packageName, userId));
        lineStr("FAKE nativeLibDir", VirtualPathSpoof.fakeNativeLibDir(packageName));
        lineStr("FAKE apk", VirtualPathSpoof.fakeApkPath(packageName));

        String[] probes = new String[] {
                "/sdcard/Android/data/" + packageName + "/files",
                "/storage/emulated/0/Android/data/" + packageName + "/files",
                VirtualPathSpoof.fakeExternalDataDir(packageName, userId) + "/files",
                VirtualPathSpoof.fakeDataDir(packageName, userId) + "/files",
                "/sdcard/Android/obb/" + packageName,
        };
        for (String probe : probes) {
            String redirected = IOCore.get().redirectPath(probe);
            logLine("IO " + probe + " -> " + redirected);
        }

        if (context != null) {
            try {
                line("CTX getDataDir", context.getDataDir());
                line("CTX getFilesDir", context.getFilesDir());
                line("CTX getCacheDir", context.getCacheDir());
                line("CTX getExternalFilesDir", context.getExternalFilesDir(null));
                line("CTX getExternalCacheDir", context.getExternalCacheDir());
                line("CTX getObbDir", context.getObbDir());
                ApplicationInfo ai = context.getApplicationInfo();
                if (ai != null) {
                    logLine("CTX appInfo.dataDir=" + ai.dataDir);
                    logLine("CTX appInfo.nativeLibraryDir=" + ai.nativeLibraryDir);
                    logLine("CTX appInfo.sourceDir=" + ai.sourceDir);
                    logLine("CTX appInfo.publicSourceDir=" + ai.publicSourceDir);
                }
                try {
                    Object at = com.anubis.loader.AnubisCore.mainThread();
                    Object bound = black.android.app.BRActivityThread.get(at).mBoundApplication();
                    if (bound != null) {
                        Object loadedApk = black.android.app.BRActivityThreadAppBindData.get(bound).info();
                        if (loadedApk != null) {
                            ApplicationInfo lai = black.android.app.BRLoadedApk.get(loadedApk).mApplicationInfo();
                            if (lai != null) {
                                logLine("LoadedApk sourceDir=" + lai.sourceDir);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable t) {
                Log.w(currentTag(), "CTX audit failed: " + t.getMessage());
            }
        }

        if (farlightExtras) {
            logDirContents("UE4 int Paks", new File(filesDir, "UE4Game/Farlight84/Saved/Paks"));
            logDirContents("UE4 ext Paks", new File(extFiles, "UE4Game/Farlight84/Saved/Paks"));
            logDirContents("BasePaks", new File(filesDir, "BasePaks"));
        }
        if (deltaExtras) {
            logDirContents("Delta int Paks", new File(filesDir, UE_DELTA_SAVED + "/Paks"));
            logDirContents("Delta ext Paks", new File(extFiles, UE_DELTA_SAVED + "/Paks"));
            logDirContents("Dolphin int", new File(filesDir, UE_DELTA_SAVED + "/Dolphin"));
            logDirContents("Dolphin ext", new File(extFiles, UE_DELTA_SAVED + "/Dolphin"));
            logDirContents("Puffer int", new File(filesDir, UE_DELTA_SAVED + "/Puffer"));
            logDirContents("Puffer ext", new File(extFiles, UE_DELTA_SAVED + "/Puffer"));
        }

        logLine("========== END PATH AUDIT ==========");
        writeAuditFile(packageName, userId, auditFileName);
    }

    private static void writeAuditFile(String packageName, int userId, String auditFileName) {
        try {
            File out = new File(BEnvironment.getHostFilesAnchor(), auditFileName);
            File parent = out.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("guest=").append(packageName).append('\n');
            sb.append("loader=").append(AnubisCore.defaultLoaderPackage()).append('\n');
            sb.append("host_anchor=").append(BEnvironment.getHostFilesAnchor().getAbsolutePath()).append('\n');
            sb.append("vfs_root=").append(BEnvironment.getVirtualRoot().getAbsolutePath()).append('\n');
            sb.append("real_files=").append(BEnvironment.getDataFilesDir(packageName, userId).getAbsolutePath()).append('\n');
            if (GamePackages.isDeltaForce(packageName)) {
                File saved = BEnvironment.getDataFilesDir(packageName, userId);
                sb.append("delta_dolphin=")
                        .append(new File(saved, UE_DELTA_SAVED + "/Dolphin").getAbsolutePath()).append('\n');
                sb.append("delta_puffer=")
                        .append(new File(saved, UE_DELTA_SAVED + "/Puffer").getAbsolutePath()).append('\n');
            }
            try (FileOutputStream fos = new FileOutputStream(out, false)) {
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            logLine("audit file = " + out.getAbsolutePath());
        } catch (Throwable ignored) {
        }
    }

    private static String currentTag() {
        String tag = sActiveTag.get();
        return tag != null ? tag : TAG_FARLIGHT;
    }

    private static void line(String label, File file) {
        if (file == null) {
            logLine(label + " = null");
            return;
        }
        String exists = file.exists() ? (file.isDirectory() ? "dir" : "file:" + file.length()) : "MISSING";
        logLine(label + " = " + file.getAbsolutePath() + " [" + exists + "]");
    }

    private static void lineStr(String label, String path) {
        logLine(label + " = " + path);
    }

    private static void logLine(String msg) {
        Log.i(currentTag(), msg);
    }

    private static void logDirContents(String label, File dir) {
        if (dir == null || !dir.isDirectory()) {
            logLine(label + " contents: (not a dir)");
            return;
        }
        File[] children = dir.listFiles();
        if (children == null || children.length == 0) {
            logLine(label + " contents: (empty)");
            return;
        }
        int shown = 0;
        StringBuilder sb = new StringBuilder(label).append(" contents: ");
        for (File child : children) {
            if (shown >= 8) {
                sb.append("... +").append(children.length - shown).append(" more");
                break;
            }
            if (shown > 0) {
                sb.append(", ");
            }
            sb.append(child.getName());
            if (child.isFile()) {
                sb.append("(").append(formatSize(child.length())).append(")");
            }
            shown++;
        }
        logLine(sb.toString());
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + "KB";
        }
        return String.format(java.util.Locale.US, "%.1fMB", bytes / (1024.0 * 1024));
    }
}
