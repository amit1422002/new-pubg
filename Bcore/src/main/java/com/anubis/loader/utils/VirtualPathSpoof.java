package com.anubis.loader.utils;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.text.TextUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import black.android.content.pm.BRApplicationInfoL;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.GamePackages;
import com.anubis.loader.utils.compat.BuildCompat;
import com.anubis.loader.utils.Slog;

/**
 * Guest-visible filesystem paths (look like a normal install).
 * Real files stay under {@link BEnvironment} — forward IO rules map fake → real.
 */
public final class VirtualPathSpoof {

    private static final String TAG = "PATH_SPOOF";

    /** CodeV/PUBG — inject/uedump on disk OK; AC games must not leave loader artifacts. */
    private static final Set<String> GUEST_DISK_TOOLS_ALLOWED = new HashSet<>(Arrays.asList(
            "com.tencent.tmgp.codev",
            "com.pubg.imobile",
            "com.tencent.ig",
            "com.rekoo.pubgm",
            "com.pubg.krmobile",
            "com.vng.pubgmobile"
    ));

    private static final ThreadLocal<Boolean> sSkipGuestSpoof = new ThreadLocal<>();
    /** Kernel UID for /proc/status Name line; Uid line stays at real host UID. */
    private static volatile int sProcSpoofUid;
    private static volatile String sGuestPackageBound;

    private VirtualPathSpoof() {
    }

    public static void setGuestPackageBound(String packageName) {
        sGuestPackageBound = packageName;
    }

    public static String getGuestPackageBound() {
        return sGuestPackageBound;
    }

    public static void setGuestProcessComm(String packageName) {
        // Comm/cmdline are served via fake /proc (IOCore.proc + ProcStealthHelper).
    }

    /**
     * Guests that need fake install paths + optional /proc scrub (DFM, Farlight, Arena, …).
     * PUBG/BGMI excluded — T5 path spoof triggers libanogs SIGBUS at logo (~12s); DFM-only tier.
     * Host loader and GMS are never stealth-targeted.
     */
    public static boolean isStealthAcPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (packageName.equals(AnubisCore.getHostPkg())) {
            return false;
        }
        // GMS/GSF/Play Store need real host-aligned paths — stealth breaks makeApplication.
        if (GmsCore.isGoogleAppOrService(packageName)) {
            return false;
        }
        if (GamePackages.isPubgMobileFamily(packageName)) {
            return false;
        }
        return true;
    }

    /** AC/GCloud games — no inject.so / libUEDump3r / uedump_trigger on guest storage. */
    public static boolean allowGuestDiskTools(String packageName) {
        return packageName != null && GUEST_DISK_TOOLS_ALLOWED.contains(packageName);
    }

    /** Map proxy stub name (top.anubis.anubis:p0) → guest package for comm / Java APIs. */
    public static String guestVisibleProcessName(String packageName, String proxyProcessName) {
        if (!isStealthAcPackage(packageName)) {
            return proxyProcessName;
        }
        if (TextUtils.isEmpty(proxyProcessName) || proxyProcessName.equals(packageName)) {
            return packageName;
        }
        if (proxyProcessName.startsWith(packageName + ":")) {
            return proxyProcessName;
        }
        return packageName;
    }

    public static void setProcSpoofUid(int uid) {
        sProcSpoofUid = uid > 0 ? uid : 0;
    }

    public static int getProcSpoofUid() {
        return sProcSpoofUid;
    }

    private static String rewriteProcIdentityLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return line;
        }
        String trimmed = line.trim();
        int colon = trimmed.indexOf(':');
        if (colon < 0) {
            return rewriteProcShortName(trimmed);
        }
        String key = trimmed.substring(0, colon);
        String guestPkg = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (TextUtils.isEmpty(guestPkg)) {
            guestPkg = BActivityThread.getAppPackageName();
        }
        if ("Name".equals(key) && !TextUtils.isEmpty(guestPkg)) {
            return "Name:\t" + guestPkg + "\n";
        }
        if ("TracerPid".equals(key)) {
            return "TracerPid:\t0\n";
        }
        if ("PPid".equals(key)) {
            return "PPid:\t1\n";
        }
        if ("Groups".equals(key) && sProcSpoofUid > 0) {
            return "Groups:\t" + sProcSpoofUid + "\n";
        }
        if (sProcSpoofUid <= 0) {
            return line;
        }
        if (!trimmed.startsWith("Uid:") && !trimmed.startsWith("Gid:")) {
            return line;
        }
        return key + ":\t" + sProcSpoofUid + "\t" + sProcSpoofUid + "\t"
                + sProcSpoofUid + "\t" + sProcSpoofUid + "\n";
    }

    private static String rewriteProcShortName(String line) {
        if (TextUtils.isEmpty(line)) {
            return line;
        }
        String hostPkg = AnubisCore.getHostPkg();
        String guestPkg = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (TextUtils.isEmpty(guestPkg)) {
            guestPkg = BActivityThread.getAppPackageName();
        }
        if (TextUtils.isEmpty(guestPkg) || !isStealthAcPackage(guestPkg)) {
            return line;
        }
        String trimmed = line.trim();
        String guestShort = guestPkg.length() > 15 ? guestPkg.substring(0, 15) : guestPkg;
        if (trimmed.equals(guestPkg) || trimmed.equals(guestShort)) {
            return line;
        }
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains(hostPkg.toLowerCase(java.util.Locale.ROOT))
                || lower.contains("anubis")
                || lower.contains("anubis")
                || lower.contains("ue4")
                || lower.contains("mainthread")
                || trimmed.contains(":")) {
            return guestShort + "\n";
        }
        return line;
    }

    public static void beginInternalBind() {
        sSkipGuestSpoof.set(Boolean.TRUE);
    }

    public static void endInternalBind() {
        sSkipGuestSpoof.remove();
    }

    public static boolean shouldSpoofForGuest() {
        return !Boolean.TRUE.equals(sSkipGuestSpoof.get());
    }

    /** Host loader / anubis packages — must not appear in guest PM or context identity. */
    public static boolean isLoaderPackageIdentity(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        String host = AnubisCore.getHostPkg();
        return packageName.equals(host)
                || packageName.startsWith(host + ".")
                || packageName.contains("anubis")
                || packageName.contains("anubis");
    }

    /** Block PM queries for loader identity while guest is bound (not during internal bind). */
    public static boolean shouldHideLoaderPackageFromGuest(String packageName) {
        if (!shouldSpoofForGuest() || TextUtils.isEmpty(packageName)) {
            return false;
        }
        // Stub process still runs system handleBindApplication (host instrumentation/onCreate)
        // before virtual guest bind completes — host ApplicationInfo must stay visible.
        if (!com.anubis.loader.app.BActivityThread.currentActivityThread().isInit()) {
            return false;
        }
        String guest = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (TextUtils.isEmpty(guest) || !isStealthAcPackage(guest)) {
            return false;
        }
        return isLoaderPackageIdentity(packageName);
    }

    public static String guestVisibleAttributionPackage() {
        String guest = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (!TextUtils.isEmpty(guest) && isStealthAcPackage(guest) && shouldSpoofForGuest()) {
            return guest;
        }
        return AnubisCore.getHostPkg();
    }

    public static int guestVisibleAttributionUid() {
        String guest = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (!TextUtils.isEmpty(guest) && isStealthAcPackage(guest) && shouldSpoofForGuest()) {
            return AnubisCore.getHostUid();
        }
        return AnubisCore.getHostUid();
    }

    public static String fakeDataDir(String packageName, int userId) {
        if (userId == 0) {
            return "/data/data/" + packageName;
        }
        return "/data/user/" + userId + "/" + packageName;
    }

    public static String fakeDeviceProtectedDataDir(String packageName, int userId) {
        return "/data/user_de/" + userId + "/" + packageName;
    }

    public static String fakeNativeLibDir(String packageName) {
        return "/data/app/" + packageName + "/lib/arm64";
    }

    public static String fakeExternalDataDir(String packageName, int userId) {
        return String.format("/storage/emulated/%d/Android/data/%s", userId, packageName);
    }

    public static String fakeApkPath(String packageName) {
        return "/data/app/" + packageName + "/base.apk";
    }

    public static boolean isGuestVirtualIoPath(String path, String guestPkg) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(guestPkg)) {
            return false;
        }
        return path.contains("/files/.vfs/data/app/" + guestPkg)
                || path.contains("/files/.vfs/data/user/0/" + guestPkg)
                || path.contains("/files/.vfs/data/user_de/0/" + guestPkg)
                || path.contains("/.vfs/data/app/" + guestPkg)
                || path.contains("/.vfs/data/user/0/" + guestPkg)
                || (path.contains("/.vfs/") && path.contains("/" + guestPkg + "/"));
    }

    public static boolean isGuestVirtualIoPath(String path) {
        String guest = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (TextUtils.isEmpty(guest)) {
            guest = BActivityThread.getAppPackageName();
        }
        return isGuestVirtualIoPath(path, guest);
    }

    /** Strip container paths before strings reach guest / AC. */
    public static String reversePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        String hostPkg = AnubisCore.getHostPkg();
        String guestPkg = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (TextUtils.isEmpty(guestPkg)) {
            guestPkg = BActivityThread.getAppPackageName();
        }
        int hostUserId = AnubisCore.getHostUserId();
        String out = path;
        for (int pass = 0; pass < 16; pass++) {
            String prev = out;
            out = reversePathOnePass(out, hostPkg, guestPkg, hostUserId);
            if (out.equals(prev) && !containsLeak(out)) {
                break;
            }
        }
        if (containsLeak(out)) {
            String collapsed = collapseToGuestExternalRoot(out, guestPkg, hostUserId);
            if (!containsLeak(collapsed)) {
                out = collapsed;
            }
        }
        return out;
    }

    /**
     * Nested external mirror (host/files/.vfs/storage/.../guest/...) → single guest external root.
     */
    static String collapseToGuestExternalRoot(String path, String guestPkg, int userId) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(guestPkg)) {
            return path;
        }
        String guestRoot = String.format(Locale.US, "/storage/emulated/%d/Android/data/%s", userId, guestPkg);
        int last = path.lastIndexOf(guestRoot);
        if (last < 0) {
            return path;
        }
        String suffix = path.substring(last + guestRoot.length());
        if (suffix.startsWith("/storage/emulated/")) {
            int dataIdx = suffix.indexOf("/Android/data/" + guestPkg);
            if (dataIdx >= 0) {
                suffix = suffix.substring(dataIdx + ("/Android/data/" + guestPkg).length());
            }
        }
        return guestRoot + suffix;
    }

    private static String reversePathOnePass(String path, String hostPkg, String guestPkg, int hostUserId) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        String vbRoot = BEnvironment.getVirtualRoot().getAbsolutePath();
        String out = path;

        String userHostVfs = "/data/user/" + hostUserId + "/" + hostPkg + "/files/.vfs/";
        if (out.startsWith(userHostVfs)) {
            out = "/" + out.substring(userHostVfs.length());
        }
        String deHostVfs = "/data/user_de/" + hostUserId + "/" + hostPkg + "/files/.vfs/";
        if (out.startsWith(deHostVfs)) {
            out = "/" + out.substring(deHostVfs.length());
        }

        if (out.startsWith(vbRoot)) {
            out = "/" + out.substring(vbRoot.length() + 1);
        }

        String hostLegacyVfs = "/data/data/" + hostPkg + "/files/.vfs/";
        if (out.startsWith(hostLegacyVfs)) {
            out = "/" + out.substring(hostLegacyVfs.length());
        }

        if (guestPkg != null && !guestPkg.isEmpty()) {
            String leakApp = "/files/data/app/" + guestPkg;
            if (out.contains(leakApp)) {
                out = out.replace(leakApp, "/data/app/" + guestPkg);
            }
            String nestedApp = "/data/app/" + guestPkg;
            String fakeRoot = String.format(Locale.US, "/data/user/%d/%s", hostUserId, guestPkg);
            String nestedUnderData = fakeRoot + nestedApp;
            if (out.contains(nestedUnderData)) {
                out = out.replace(nestedUnderData, nestedApp);
            }
            String leakStorage = "/files/storage/emulated/";
            if (out.contains(leakStorage)) {
                out = out.replace(leakStorage, "/storage/emulated/");
            }
            String leakStorageDirect = fakeRoot + "/storage/emulated/";
            if (out.contains(leakStorageDirect)) {
                out = out.replace(leakStorageDirect, "/storage/emulated/");
            }
        }

        if (guestPkg != null && !guestPkg.isEmpty() && out.contains("/.vfs/")) {
            String vfsLib = "/.vfs/data/app/" + guestPkg + "/lib/";
            int libIdx = out.indexOf(vfsLib);
            if (libIdx >= 0) {
                String tail = out.substring(libIdx + vfsLib.length());
                int end = tail.indexOf(' ');
                String soName = end > 0 ? tail.substring(0, end) : tail;
                if (soName.endsWith(".so")) {
                    String fake = fakeNativeLibDir(guestPkg) + "/" + soName;
                    out = out.substring(0, libIdx) + fake + (end > 0 ? tail.substring(end) : "");
                }
            }
        }

        // /data/app/~~hash~~/hostPkg-.../base.apk -> fake apk
        if (guestPkg != null && !guestPkg.isEmpty() && out.contains("/data/app/")) {
            int appIdx = out.indexOf("/data/app/");
            if (appIdx >= 0) {
                String tail = out.substring(appIdx);
                if (tail.contains(hostPkg)) {
                    String fakeApk = fakeApkPath(guestPkg);
                    if (tail.endsWith("/base.apk") || tail.contains("/base.apk")) {
                        out = out.substring(0, appIdx) + fakeApk;
                    } else if (tail.contains("/lib/arm64/libartpalette.so")
                            || tail.contains("/lib/arm64/libartpalette")) {
                        out = out.substring(0, appIdx) + fakeNativeLibDir(guestPkg) + "/libc.so";
                    } else if (tail.contains("libartpalette.so")) {
                        out = out.replace("libartpalette.so", "libc.so");
                        out = out.replace(hostPkg, guestPkg);
                    } else {
                        out = out.replace(hostPkg, guestPkg);
                    }
                } else if (tail.contains(guestPkg)) {
                    if (tail.contains("/base.apk")) {
                        out = out.substring(0, appIdx) + fakeApkPath(guestPkg);
                    } else if (tail.contains("/lib/") && tail.contains(".so")) {
                        int soSlash = tail.lastIndexOf('/');
                        if (soSlash > 0) {
                            out = out.substring(0, appIdx) + fakeNativeLibDir(guestPkg)
                                    + tail.substring(soSlash);
                        }
                    } else if (tail.contains("split_") && tail.endsWith(".apk")) {
                        int splitSlash = tail.lastIndexOf('/');
                        if (splitSlash > 0) {
                            out = out.substring(0, appIdx) + "/data/app/" + guestPkg
                                    + tail.substring(splitSlash);
                        }
                    }
                }
            }
        }

        if (guestPkg != null && !guestPkg.isEmpty() && out.contains("/data/dalvik-cache/")
                && out.contains(guestPkg)) {
            out = "/data/data/" + guestPkg + "/cache/oat.dex";
        }

        // External storage: strip nested host mirror segments (repeat-safe)
        String extHost = "/storage/emulated/" + hostUserId + "/Android/data/" + hostPkg;
        String extGuest = (guestPkg != null && !guestPkg.isEmpty())
                ? "/storage/emulated/" + hostUserId + "/Android/data/" + guestPkg
                : extHost;
        while (out.contains(extHost)) {
            out = out.replace(extHost, extGuest);
        }
        while (out.contains("/files/.vfs/")) {
            int fb = out.indexOf("/files/.vfs/");
            out = out.substring(0, fb) + out.substring(fb + "/files/.vfs/".length());
        }
        // After mirror strip, pkg can glue to "storage/..." (mf.uamostorage → mf.uamo/storage)
        if (guestPkg != null && !guestPkg.isEmpty()) {
            out = out.replace(guestPkg + "storage/", guestPkg + "/storage/");
        }

        while (out.contains("/.vfs/")) {
            int i = out.indexOf("/.vfs/");
            String after = out.substring(i + "/.vfs/".length());
            if (after.startsWith("data/")) {
                out = "/" + after;
            } else if (after.startsWith("storage/")) {
                out = "/storage/" + after.substring("storage/".length());
            } else {
                out = out.substring(0, i) + "/" + after;
            }
        }

        if (hostUserId == 0 && out.startsWith("/data/user/0/") && !out.contains(hostPkg)) {
            String rest = out.substring("/data/user/0/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) {
                out = "/data/data/" + rest;
            }
        }

        if (guestPkg != null && !guestPkg.isEmpty()) {
            String hostOnly = "/data/user/" + hostUserId + "/" + hostPkg;
            if (out.startsWith(hostOnly) && !out.contains("/.vfs/")) {
                out = fakeDataDir(guestPkg, hostUserId) + out.substring(hostOnly.length());
            }
            String hostLegacy = "/data/data/" + hostPkg;
            if (out.startsWith(hostLegacy) && !out.contains("/.vfs/")) {
                out = fakeDataDir(guestPkg, 0) + out.substring(hostLegacy.length());
            }
            out = collapseToGuestExternalRoot(out, guestPkg, hostUserId);
        }

        return out;
    }

    /** Drop entire maps/smaps line when it exposes injected / loader artifacts. */
    public static boolean shouldDropMapsLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("libartpalette.so") || lower.contains("libbcore.so") || lower.contains("libbystom.so")
                || lower.contains("libuedump3r.so") || lower.contains("libboxesp.so")
                || lower.contains("libguestloginhook.so") || lower.contains("guestloginhook")
                || lower.contains("inject.so")) {
            return true;
        }
        if (lower.contains("dalvik-classes") && lower.contains("anubis")) {
            return true;
        }
        if (lower.contains("extrac") && lower.contains("anubis") && lower.contains(".apk")) {
            return true;
        }
        if (lower.contains("anubis") && (lower.contains(".so") || lower.contains(".apk")
                || lower.contains("dalvik-classes"))) {
            return true;
        }
        return false;
    }

    /** Scrub one line from /proc maps, cmdline, etc. Null = drop line from maps output. */
    public static String scrubProcLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return line;
        }
        line = rewriteProcIdentityLine(line);
        if (shouldDropMapsLine(line)) {
            return null;
        }
        String out = reversePath(line);
        String hostPkg = AnubisCore.getHostPkg();
        String guestPkg = com.anubis.loader.app.BActivityThread.getAppPackageName();
        if (guestPkg != null && !guestPkg.isEmpty()) {
            if (out.contains(hostPkg)) {
                out = out.replace(hostPkg, guestPkg);
            }
            String hostProcPrefix = hostPkg + ":p";
            int pIdx = out.indexOf(hostProcPrefix);
            while (pIdx >= 0) {
                int end = pIdx + hostProcPrefix.length();
                while (end < out.length() && Character.isDigit(out.charAt(end))) {
                    end++;
                }
                out = out.substring(0, pIdx) + guestPkg + out.substring(end);
                pIdx = out.indexOf(hostProcPrefix);
            }
            out = out.replace(hostPkg + ":black", guestPkg);
            if (out.contains("anubis:")) {
                out = out.replaceAll("anubis:p\\d+", guestPkg);
                out = out.replace("anubis:black", guestPkg);
            }
            out = out.replace("com.anubis.loader.task_affinity", guestPkg);
        }
        if (out.contains("libboxesp.so")) {
            out = out.replace("libboxesp.so", "liblog.so");
        }
        if (out.contains("libboxesp")) {
            out = out.replace("libboxesp", "liblog");
        }
        if (out.contains("libartpalette.so")) {
            out = out.replace("libartpalette.so", "libc.so");
        }
        if (out.contains("libbcore.so")) {
            out = out.replace("libbcore.so", "libc.so");
        }
        if (out.contains("libartpalette")) {
            out = out.replace("libartpalette", "libc");
        }
        if (out.contains("libbcore")) {
            out = out.replace("libbcore", "libc");
        }
        if (out.contains("anubis")) {
            out = out.replace("anubis", "android");
        }
        return out;
    }

    /** fakeDataDir + "/data/app/{guest}/..." — NERtc / AC virtual-env fingerprint. */
    private static boolean pathContainsNestedDataAppLeak(String path) {
        String guest = BActivityThread.getAppPackageName();
        if (TextUtils.isEmpty(guest)) {
            return false;
        }
        return path.contains("/data/user/") && path.contains("/data/app/" + guest);
    }

    static boolean pathContainsNestedStorageLeak(String path) {
        int dataIdx = path.indexOf("/data/user/");
        if (dataIdx < 0) {
            return false;
        }
        int storageIdx = path.indexOf("/storage/emulated/", dataIdx);
        if (storageIdx < 0) {
            return false;
        }
        String mid = path.substring(dataIdx, storageIdx);
        return !mid.contains("/.vfs/") && !mid.contains("/files/");
    }

    public static boolean containsLeak(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        String hostPkg = AnubisCore.getHostPkg();
        return path.contains(hostPkg)
                || path.contains("/.vfs/")
                || path.contains("/files/data/app/")
                || pathContainsNestedDataAppLeak(path)
                || pathContainsNestedStorageLeak(path)
                || path.contains("/files/storage/emulated/")
                || path.contains("anubis")
                || path.contains("blackbox")
                || path.contains("bcore")
                || path.contains("UEDump3r")
                || path.contains("libUEDump3r")
                || path.contains("uedump_trigger")
                || path.contains("uedump_output")
                || path.contains("inject.so");
    }

    public static void logGuestPath(String field, String packageName, String value) {
        // Intentionally silent — logcat must not expose path spoof state to guest / AC.
    }

    /** Guest-visible copy — fake paths for foreign PM queries; own package keeps vfs paths for native I/O. */
    public static ApplicationInfo spoofApplicationInfoForGuest(ApplicationInfo src, int userId) {
        if (src == null) {
            return src;
        }
        if (!shouldSpoofForGuest()) {
            ensureRealApkPaths(src, userId);
            return src;
        }
        String self = BActivityThread.getAppPackageName();
        if (self != null && self.equals(src.packageName) && isStealthAcPackage(self)) {
            ApplicationInfo ai = new ApplicationInfo(src);
            ensureFrameworkApplicationInfo(ai, userId);
            return ai;
        }
        return spoofApplicationInfoRuntimeVisible(src, userId);
    }

    /** LoadedApk ApplicationInfo — guest-visible fake data/lib paths; real dirs set separately on LoadedApk. */
    public static ApplicationInfo spoofApplicationInfoForLoadedApk(ApplicationInfo src, int userId) {
        if (src == null || !isStealthAcPackage(src.packageName)) {
            return src;
        }
        return spoofApplicationInfoRuntimeVisible(src, userId);
    }

    /** Runtime bind — guest-visible data/storage paths; real tree via IO redirect. */
    public static ApplicationInfo spoofApplicationInfoRuntimeVisible(ApplicationInfo src, int userId) {
        if (src == null || !isStealthAcPackage(src.packageName)) {
            return src;
        }
        ApplicationInfo ai = spoofApplicationInfoPaths(src, userId);
        String pkg = src.packageName;
        ai.dataDir = fakeDataDir(pkg, userId);
        ai.deviceProtectedDataDir = fakeDeviceProtectedDataDir(pkg, userId);
        ai.nativeLibraryDir = fakeNativeLibDir(pkg);
        logGuestPath("dataDir", pkg, ai.dataDir);
        logGuestPath("nativeLibraryDir", pkg, ai.nativeLibraryDir);
        return ai;
    }

    /** @deprecated use {@link #spoofApplicationInfoRuntimeVisible} */
    public static ApplicationInfo spoofRuntimeApplicationInfo(ApplicationInfo src, int userId) {
        return spoofApplicationInfoRuntimeVisible(src, userId);
    }

    /** PM guest queries — spoof APK paths only; data/storage dirs stay on the real virtual tree. */
    public static ApplicationInfo spoofApplicationInfoPaths(ApplicationInfo src, int userId) {
        if (src == null) {
            return null;
        }
        String pkg = src.packageName;
        if (pkg == null || pkg.equals(AnubisCore.getHostPkg())) {
            return src;
        }
        ApplicationInfo ai = new ApplicationInfo(src);
        String fakeApk = fakeApkPath(pkg);

        ai.sourceDir = fakeApk;
        ai.publicSourceDir = fakeApk;
        if (ai.splitSourceDirs != null && ai.splitSourceDirs.length > 0) {
            String[] fakeSplits = new String[ai.splitSourceDirs.length];
            for (int i = 0; i < ai.splitSourceDirs.length; i++) {
                String split = ai.splitSourceDirs[i];
                if (split == null) {
                    continue;
                }
                int slash = split.lastIndexOf('/');
                fakeSplits[i] = slash >= 0 ? "/data/app/" + pkg + split.substring(slash) : fakeApk;
            }
            ai.splitSourceDirs = fakeSplits;
        }
        logGuestPath("sourceDir", pkg, ai.sourceDir);

        if (BuildCompat.isL()) {
            BRApplicationInfoL.get(ai)._set_scanPublicSourceDir(fakeApk);
            BRApplicationInfoL.get(ai)._set_scanSourceDir(fakeApk);
        }
        return ai;
    }

    /**
     * Stealth AC: guest-visible APK paths; real vfs data + native lib dirs.
     * Opens on fake paths are forwarded by {@link com.anubis.loader.core.IOCore}.
     */
    public static void ensureStealthFrameworkApplicationInfo(ApplicationInfo ai, int userId) {
        if (ai == null || TextUtils.isEmpty(ai.packageName)) {
            return;
        }
        if (isLoaderPackageIdentity(ai.packageName)) {
            return;
        }
        String pkg = ai.packageName;
        String fakeApk = fakeApkPath(pkg);
        ai.sourceDir = fakeApk;
        ai.publicSourceDir = fakeApk;
        File appDir = BEnvironment.getAppDir(pkg);
        File[] splits = appDir.listFiles((dir, name) -> name != null && name.endsWith(".apk")
                && !"base.apk".equals(name));
        if (splits != null && splits.length > 0) {
            String[] fakeSplits = new String[splits.length];
            for (int i = 0; i < splits.length; i++) {
                fakeSplits[i] = "/data/app/" + pkg + "/" + splits[i].getName();
            }
            ai.splitSourceDirs = fakeSplits;
        }
        if (BuildCompat.isL()) {
            BRApplicationInfoL.get(ai)._set_scanPublicSourceDir(fakeApk);
            BRApplicationInfoL.get(ai)._set_scanSourceDir(fakeApk);
        }
        ai.nativeLibraryDir = fakeNativeLibDir(pkg);
        File data = BEnvironment.getDataDir(pkg, userId);
        ai.dataDir = data.getAbsolutePath();
        ai.deviceProtectedDataDir = BEnvironment.getDeDataDir(pkg, userId).getAbsolutePath();
    }

    /**
     * LoadedApk internal paths — real vfs APK for dex (marked read-only); real dataDir for I/O.
     */
    public static void ensureLoadedApkInternalInfo(ApplicationInfo ai, int userId) {
        ensureFrameworkApplicationInfo(ai, userId);
    }

    /**
     * Framework / LoadedApk — vfs APK paths for dex loading (never stale host install hash).
     */
    public static void ensureFrameworkApplicationInfo(ApplicationInfo ai, int userId) {
        if (ai == null || TextUtils.isEmpty(ai.packageName)) {
            return;
        }
        if (isLoaderPackageIdentity(ai.packageName)) {
            return;
        }
        if (GmsCore.isGoogleAppOrService(ai.packageName)) {
            ensureGoogleFrameworkApplicationInfo(ai, userId);
            return;
        }
        ensureRealApkPaths(ai, userId);
        ensureRealNativeLibDir(ai);
        String pkg = ai.packageName;
        File data = BEnvironment.getDataDir(pkg, userId);
        ai.dataDir = data.getAbsolutePath();
        ai.deviceProtectedDataDir = BEnvironment.getDeDataDir(pkg, userId).getAbsolutePath();
    }

    /** microG / Play — vfs APK paths; no androidx CoreComponentFactory on slim clones. */
    private static void ensureGoogleFrameworkApplicationInfo(ApplicationInfo ai, int userId) {
        prepareGoogleBindApplicationInfo(ai, userId);
        String pkg = ai.packageName;
        File data = BEnvironment.getDataDir(pkg, userId);
        ai.dataDir = data.getAbsolutePath();
        ai.deviceProtectedDataDir = BEnvironment.getDeDataDir(pkg, userId).getAbsolutePath();
    }

    /** Before LoadedApk / makeApplication for virtual Google packages. */
    public static void prepareGoogleBindApplicationInfo(ApplicationInfo ai, int userId) {
        if (ai == null || !GmsCore.isGoogleAppOrService(ai.packageName)) {
            return;
        }
        ensureRealApkPaths(ai, userId);
        ensureRealNativeLibDir(ai);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ai.appComponentFactory = null;
        }
    }

    /**
     * Virtual GMS/microG clones often declare androidx CoreComponentFactory but ship without it —
     * LoadedApk then fails makeApplication and post-login GMS bind kills the guest.
     * Result is cached — must not run on every {@link android.content.Context#getApplicationInfo()}.
     */
    private static final ConcurrentHashMap<String, Boolean> sAppComponentFactoryProbe = new ConcurrentHashMap<>();

    public static void sanitizeAppComponentFactory(ApplicationInfo ai) {
        if (ai == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        String factory = ai.appComponentFactory;
        if (TextUtils.isEmpty(factory)) {
            return;
        }
        String apk = ai.sourceDir;
        if (TextUtils.isEmpty(apk)) {
            ai.appComponentFactory = null;
            return;
        }
        String pkg = ai.packageName != null ? ai.packageName : "";
        String cacheKey = pkg + '|' + factory + '|' + apk;
        Boolean known = sAppComponentFactoryProbe.get(cacheKey);
        if (known != null) {
            if (!known) {
                ai.appComponentFactory = null;
            }
            return;
        }
        try {
            String lib = ai.nativeLibraryDir;
            ClassLoader probe = new dalvik.system.PathClassLoader(
                    apk, lib != null ? lib : "", ClassLoader.getSystemClassLoader());
            Class.forName(factory, false, probe);
            sAppComponentFactoryProbe.put(cacheKey, true);
        } catch (Throwable t) {
            Slog.w(TAG, "dropping appComponentFactory " + factory + " pkg=" + ai.packageName
                    + ": " + t.getMessage());
            sAppComponentFactoryProbe.put(cacheKey, false);
            ai.appComponentFactory = null;
        }
    }

    public static void ensureRealNativeLibDir(ApplicationInfo ai) {
        if (ai == null || TextUtils.isEmpty(ai.packageName)) {
            return;
        }
        if (isLoaderPackageIdentity(ai.packageName)) {
            return;
        }
        File libDir = BEnvironment.getAppLibDir(ai.packageName);
        ai.nativeLibraryDir = libDir.getAbsolutePath();
    }

    /** Framework launch — real APK paths only (Resources); data/lib stay guest-visible fakes via spoof. */
    public static void ensureRealApkPaths(ApplicationInfo ai, int userId) {
        if (ai == null || TextUtils.isEmpty(ai.packageName)) {
            return;
        }
        if (isLoaderPackageIdentity(ai.packageName)) {
            return;
        }
        String pkg = ai.packageName;
        File baseApk = BEnvironment.getBaseApkDir(pkg);
        if (baseApk.isFile()) {
            ai.sourceDir = baseApk.getAbsolutePath();
            ai.publicSourceDir = ai.sourceDir;
        }
        String fakeApk = fakeApkPath(pkg);
        if (fakeApk.equals(ai.sourceDir) || fakeApk.equals(ai.publicSourceDir)) {
            File hostApk = resolveHostInstalledApk(pkg);
            if (hostApk != null) {
                ai.sourceDir = hostApk.getAbsolutePath();
                ai.publicSourceDir = ai.sourceDir;
            }
        }
        File appDir = BEnvironment.getAppDir(pkg);
        File[] splits = appDir.listFiles((dir, name) -> name != null && name.endsWith(".apk")
                && !"base.apk".equals(name));
        if (splits != null && splits.length > 0) {
            String[] splitPaths = new String[splits.length];
            for (int i = 0; i < splits.length; i++) {
                splitPaths[i] = splits[i].getAbsolutePath();
            }
            ai.splitSourceDirs = splitPaths;
        }
        if (BuildCompat.isL()) {
            BRApplicationInfoL.get(ai)._set_scanPublicSourceDir(ai.publicSourceDir);
            BRApplicationInfoL.get(ai)._set_scanSourceDir(ai.sourceDir);
        }
    }

    private static File resolveHostInstalledApk(String packageName) {
        try {
            android.content.pm.ApplicationInfo host = AnubisCore.getContext().getPackageManager()
                    .getApplicationInfo(packageName, 0);
            if (host != null && host.sourceDir != null) {
                File apk = new File(host.sourceDir);
                if (apk.isFile()) {
                    return apk;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static PackageInfo spoofPackageInfoForGuest(PackageInfo src, int userId) {
        if (src == null) {
            return src;
        }
        if (!shouldSpoofForGuest()) {
            ensureRealApkPaths(src.applicationInfo, userId);
            return src;
        }
        PackageInfo pi = src;
        pi.applicationInfo = spoofApplicationInfoRuntimeVisible(pi.applicationInfo, userId);
        ensureRealApkPaths(pi.applicationInfo, userId);
        return pi;
    }

    public static ActivityInfo spoofActivityInfoForGuest(ActivityInfo src, int userId) {
        if (src == null || !shouldSpoofForGuest()) {
            return src;
        }
        ActivityInfo ai = new ActivityInfo(src);
        ai.applicationInfo = spoofApplicationInfoRuntimeVisible(ai.applicationInfo, userId);
        ensureRealApkPaths(ai.applicationInfo, userId);
        return ai;
    }

    public static ServiceInfo spoofServiceInfoForGuest(ServiceInfo src, int userId) {
        if (src == null || !shouldSpoofForGuest()) {
            return src;
        }
        ServiceInfo si = new ServiceInfo(src);
        si.applicationInfo = spoofApplicationInfoRuntimeVisible(si.applicationInfo, userId);
        ensureRealApkPaths(si.applicationInfo, userId);
        return si;
    }

    public static ProviderInfo spoofProviderInfoForGuest(ProviderInfo src, int userId) {
        if (src == null || !shouldSpoofForGuest()) {
            return src;
        }
        ProviderInfo pi = new ProviderInfo(src);
        pi.applicationInfo = spoofApplicationInfoRuntimeVisible(pi.applicationInfo, userId);
        ensureRealApkPaths(pi.applicationInfo, userId);
        return pi;
    }

    public static ResolveInfo spoofResolveInfoForGuest(ResolveInfo src, int userId) {
        if (src == null || !shouldSpoofForGuest()) {
            return src;
        }
        ResolveInfo ri = new ResolveInfo(src);
        if (ri.activityInfo != null) {
            ri.activityInfo = spoofActivityInfoForGuest(ri.activityInfo, userId);
        }
        if (ri.serviceInfo != null) {
            ri.serviceInfo = spoofServiceInfoForGuest(ri.serviceInfo, userId);
        }
        if (ri.providerInfo != null) {
            ri.providerInfo = spoofProviderInfoForGuest(ri.providerInfo, userId);
        }
        return ri;
    }

    public static String fakeObbDir(String packageName, int userId) {
        return String.format("/storage/emulated/%d/Android/obb/%s", userId, packageName);
    }
}
