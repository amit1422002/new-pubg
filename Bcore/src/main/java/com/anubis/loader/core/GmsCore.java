package com.anubis.loader.core;

import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.Set;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.entity.pm.InstallResult;
import com.anubis.loader.gms.MicroGBootstrap;
import com.anubis.loader.gms.MicroGInstaller;
import com.anubis.loader.utils.Slog;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class GmsCore {
    private static final String TAG = "GmsCore";

    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();
    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";
    public static final String SETTINGS_PKG = "com.android.settings";

    static {
        GOOGLE_APP.add(VENDING_PKG);
        GOOGLE_APP.add("com.google.android.play.games");
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");

        GOOGLE_SERVICE.add(GMS_PKG);
        GOOGLE_SERVICE.add(GSF_PKG);
        GOOGLE_SERVICE.add("com.google.android.gsf.login");
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
    }

    public static boolean isGoogleService(String packageName) {
        return GOOGLE_SERVICE.contains(packageName);
    }

    public static boolean isGoogleAppOrService(String str) {
        return GOOGLE_APP.contains(str) || GOOGLE_SERVICE.contains(str);
    }

    private static InstallResult installPackages(Set<String> list, int userId) {
        AnubisCore blackBoxCore = AnubisCore.get();
        for (String packageName : list) {
            if (blackBoxCore.isInstalled(packageName, userId)) {
                continue;
            }
            try {
                AnubisCore.getContext().getPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            InstallResult installResult = blackBoxCore.installPackageAsUser(packageName, userId);
            if (!installResult.success) {
                return installResult;
            }
        }
        return new InstallResult();
    }

    private static void uninstallPackages(Set<String> list, int userId) {
        AnubisCore blackBoxCore = AnubisCore.get();
        for (String packageName : list) {
            blackBoxCore.uninstallPackageAsUser(packageName, userId);
        }
    }

    /** Install microG GMS + FakeStore only (no host GMS clone). */
    public static InstallResult installGApps(int userId) {
        Slog.d(TAG, "Installing microG GMS + FakeStore for user " + userId);
        InstallResult microG = MicroGInstaller.install(userId);
        if (microG.success) {
            Slog.d(TAG, "microG install OK");
            return microG;
        }
        Slog.e(TAG, "microG install failed: " + microG.msg);
        return microG;
    }

    public static void uninstallGApps(int userId) {
        uninstallPackages(GOOGLE_SERVICE, userId);
        uninstallPackages(GOOGLE_APP, userId);
    }

    public static void remove(String packageName) {
        GOOGLE_SERVICE.remove(packageName);
        GOOGLE_APP.remove(packageName);
    }

    public static boolean isSupportGms() {
        return true;
    }

    public static boolean isInstalledGoogleService(int userId) {
        return AnubisCore.get().isInstalled(GMS_PKG, userId);
    }

    public static String getIntentPackage(Intent intent) {
        if (intent == null) {
            return null;
        }
        String pkg = intent.getPackage();
        if (pkg == null && intent.getComponent() != null) {
            pkg = intent.getComponent().getPackageName();
        }
        return pkg;
    }

    public static boolean shouldUseHostGoogle(String callerPackage) {
        return callerPackage != null
                && !callerPackage.equals(AnubisCore.getHostPkg())
                && !isGoogleAppOrService(callerPackage);
    }

    public static Set<String> getAllGooglePackages() {
        Set<String> all = new HashSet<>();
        all.addAll(GOOGLE_APP);
        all.addAll(GOOGLE_SERVICE);
        return all;
    }
}
