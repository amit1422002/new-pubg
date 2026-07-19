package com.anubis.loader.gms;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import java.io.File;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.utils.Slog;

/**
 * Helpers for running microG GMS inside the virtual environment.
 */
public final class MicroGCompat {
    private static final String TAG = "MicroGCompat";
    private static volatile String sHostAndroidId;
    private static volatile long sHostAndroidIdLong = -1L;
    private static volatile ClassLoader sGmsClassLoader;

    private MicroGCompat() {}

    /** ClassLoader for the installed virtual microG GMS APK (not the host Anubis APK). */
    public static ClassLoader getGmsClassLoader() {
        int userId = BActivityThread.getUserId();
        if (sGmsClassLoader != null) return sGmsClassLoader;
        synchronized (MicroGCompat.class) {
            if (sGmsClassLoader != null) return sGmsClassLoader;
            try {
                PackageInfo pi = AnubisCore.getBPackageManager()
                        .getPackageInfo(GmsCore.GMS_PKG, 0, userId);
                if (pi != null && pi.applicationInfo != null && pi.applicationInfo.sourceDir != null) {
                    ApplicationInfo ai = pi.applicationInfo;
                    sGmsClassLoader = new dalvik.system.PathClassLoader(
                            ai.sourceDir, ai.nativeLibraryDir, ClassLoader.getSystemClassLoader());
                    Slog.d(TAG, "GMS classloader: " + ai.sourceDir);
                    return sGmsClassLoader;
                }
            } catch (Throwable t) {
                Slog.w(TAG, "getPackageInfo for GMS classloader", t);
            }
            File apk = BEnvironment.getBaseApkDir(GmsCore.GMS_PKG);
            if (apk.exists()) {
                sGmsClassLoader = new dalvik.system.PathClassLoader(
                        apk.getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
                Slog.d(TAG, "GMS classloader from " + apk);
                return sGmsClassLoader;
            }
            throw new IllegalStateException("microG GMS APK not found");
        }
    }

    /** Context that identifies as virtual GMS for microG auth/token exchange. */
    public static Context createAuthContext() {
        int userId = BActivityThread.getUserId();
        ApplicationInfo appInfo = null;
        try {
            PackageInfo pi = AnubisCore.getBPackageManager().getPackageInfo(
                    GmsCore.GMS_PKG, PackageManager.GET_META_DATA, userId);
            appInfo = pi.applicationInfo;
        } catch (Throwable t) {
            Slog.w(TAG, "getPackageInfo for auth context", t);
        }
        final ApplicationInfo gmsAppInfo = appInfo;
        final ClassLoader gmsLoader = getGmsClassLoader();
        return new ContextWrapper(AnubisCore.getContext()) {
            @Override
            public String getPackageName() {
                return GmsCore.GMS_PKG;
            }

            @Override
            public ClassLoader getClassLoader() {
                return gmsLoader;
            }

            @Override
            public ApplicationInfo getApplicationInfo() {
                if (gmsAppInfo != null) return gmsAppInfo;
                try {
                    return getPackageManager().getApplicationInfo(GmsCore.GMS_PKG, PackageManager.GET_META_DATA);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static boolean isMicroGInstalled(int userId) {
        try {
            return AnubisCore.get().isInstalled(GmsCore.GMS_PKG, userId);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean shouldBypassGmsHooks() {
        try {
            return isMicroGInstalled(BActivityThread.getUserId());
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isGoogleProcess() {
        try {
            String pkg = BActivityThread.getAppPackageName();
            return pkg != null && GmsCore.isGoogleAppOrService(pkg);
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getHostAndroidId() {
        if (sHostAndroidId == null) {
            synchronized (MicroGCompat.class) {
                if (sHostAndroidId == null) {
                    try {
                        sHostAndroidId = Settings.Secure.getString(
                                AnubisCore.getContext().getContentResolver(),
                                Settings.Secure.ANDROID_ID);
                    } catch (Throwable t) {
                        Slog.w(TAG, "Failed to read host android_id", t);
                    }
                    if (sHostAndroidId == null || sHostAndroidId.isEmpty()) {
                        sHostAndroidId = "0";
                    }
                }
            }
        }
        return sHostAndroidId;
    }

    public static long getHostAndroidIdLong() {
        if (sHostAndroidIdLong < 0) {
            synchronized (MicroGCompat.class) {
                if (sHostAndroidIdLong < 0) {
                    String id = getHostAndroidId();
                    try {
                        sHostAndroidIdLong = Long.parseUnsignedLong(id, 16);
                    } catch (NumberFormatException e) {
                        try {
                            sHostAndroidIdLong = Long.parseLong(id);
                        } catch (NumberFormatException e2) {
                            sHostAndroidIdLong = 0L;
                        }
                    }
                }
            }
        }
        return sHostAndroidIdLong;
    }
}
