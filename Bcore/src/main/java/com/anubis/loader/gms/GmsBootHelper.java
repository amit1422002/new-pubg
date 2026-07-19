package com.anubis.loader.gms;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import java.io.File;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.entity.pm.InstallResult;
import com.anubis.loader.utils.Slog;
import com.anubis.loader.utils.StealthPathRules;
import com.anubis.loader.utils.VirtualPathSpoof;

/**
 * Virtual GMS must boot before Farlight post-login chimera binds — microG slim APK often
 * fails makeApplication; host clone + materialized vfs tree is the reliable path.
 */
public final class GmsBootHelper {

    private static final String TAG = "GmsBootHelper";

    private GmsBootHelper() {
    }

    public static void ensureBootable(int userId) {
        try {
            if (!hostHasGms()) {
                return;
            }
            if (!AnubisCore.get().isInstalled(GmsCore.GMS_PKG, userId)) {
                installHostClone(userId, "not installed");
                return;
            }
            if (!canBootVirtualGms(userId)) {
                AnubisCore.get().uninstallPackageAsUser(GmsCore.GMS_PKG, userId);
                installHostClone(userId, "broken virtual apk");
            }
        } catch (Throwable t) {
            Slog.w(TAG, "ensureBootable: " + t.getMessage());
        }
    }

    private static boolean hostHasGms() {
        try {
            AnubisCore.getContext().getPackageManager().getApplicationInfo(GmsCore.GMS_PKG, 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void installHostClone(int userId, String reason) {
        InstallResult r = AnubisCore.get().installPackageAsUser(GmsCore.GMS_PKG, userId);
        Slog.i(TAG, "host GMS clone (" + reason + ") user=" + userId + " ok=" + r.success
                + (r.msg != null ? " msg=" + r.msg : ""));
        StealthPathRules.ensureGmsInstallReady(userId);
    }

    static boolean canBootVirtualGms(int userId) {
        try {
            PackageInfo pi = AnubisCore.getBPackageManager().getPackageInfo(GmsCore.GMS_PKG, 0, userId);
            if (pi == null || pi.applicationInfo == null) {
                return false;
            }
            ApplicationInfo ai = new ApplicationInfo(pi.applicationInfo);
            VirtualPathSpoof.prepareGoogleBindApplicationInfo(ai, userId);
            String apk = ai.sourceDir;
            if (apk == null) {
                return false;
            }
            File apkFile = new File(apk);
            if (!apkFile.isFile() || apkFile.length() < 1_000_000L) {
                return false;
            }
            String appClass = ai.className;
            if (appClass == null || appClass.isEmpty()) {
                return false;
            }
            String lib = ai.nativeLibraryDir;
            ClassLoader cl = new dalvik.system.PathClassLoader(
                    apk, lib != null ? lib : "", ClassLoader.getSystemClassLoader());
            Class.forName(appClass, false, cl);
            return true;
        } catch (Throwable t) {
            Slog.w(TAG, "canBootVirtualGms=false: " + t.getMessage());
            return false;
        }
    }
}
