package com.anubis.loader.utils;

import android.content.pm.ApplicationInfo;

import black.android.app.BRLoadedApk;
import com.anubis.loader.core.env.BEnvironment;

public final class StealthClassLoaderHelper {

    private StealthClassLoaderHelper() {
    }

    public static void replaceIfNeeded(Object loadedApk, String packageName, ApplicationInfo loadInfo) {
        if (!VirtualPathSpoof.isStealthAcPackage(packageName) || loadedApk == null || loadInfo == null) {
            return;
        }
        try {
            String libSearch = BEnvironment.resolveNativeLibDir(packageName).getAbsolutePath();
            ClassLoader current = BRLoadedApk.get(loadedApk).getClassLoader();
            if (current instanceof dalvik.system.PathClassLoader) {
                NativeLibDirHelper.appendNativeLibDir((dalvik.system.PathClassLoader) current, packageName);
            }
            if (!StealthMode.shouldSuppressLogcat()) {
                Slog.i("StealthClassLoader", "lib fallback armed pkg=" + packageName + " lib=" + libSearch);
            }
        } catch (Throwable t) {
            Slog.w("StealthClassLoader", "patch failed pkg=" + packageName + ": " + t.getMessage());
        }
    }
}
