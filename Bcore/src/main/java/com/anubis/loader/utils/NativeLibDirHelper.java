package com.anubis.loader.utils;

import android.system.Os;
import android.system.OsConstants;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import dalvik.system.PathClassLoader;
import com.anubis.loader.core.env.BEnvironment;

import org.lsposed.lsparanoid.Obfuscate;

/** Adds virt native lib dir to PathClassLoader without replacing the classloader instance. */
@Obfuscate
final class NativeLibDirHelper {

    private NativeLibDirHelper() {
    }

    static void appendNativeLibDir(PathClassLoader classLoader, String packageName) {
        if (classLoader == null || packageName == null) {
            return;
        }
        File libDir = BEnvironment.resolveNativeLibDir(packageName);
        if (!libDir.isDirectory()) {
            return;
        }
        try {
            Field pathListField = dalvik.system.BaseDexClassLoader.class.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathList = pathListField.get(classLoader);
            if (pathList == null) {
                return;
            }
            Field nativeDirsField = pathList.getClass().getDeclaredField("nativeLibraryDirectories");
            nativeDirsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<File> dirs = (List<File>) nativeDirsField.get(pathList);
            if (dirs != null && !dirs.contains(libDir)) {
                dirs.add(libDir);
            }
        } catch (Throwable t) {
            Slog.w("NativeLibDir", "appendNativeLibDir pkg=" + packageName + ": " + t.getMessage());
        }
    }

    static String resolveExtractedSo(String packageName, String libraryName) {
        if (packageName == null || libraryName == null) {
            return null;
        }
        String fileName = System.mapLibraryName(libraryName);
        File so = new File(BEnvironment.resolveNativeLibDir(packageName), fileName);
        if (so.isFile() && so.length() > 0L) {
            return so.getAbsolutePath();
        }
        try {
            if (Os.access(so.getAbsolutePath(), OsConstants.R_OK)) {
                return so.getAbsolutePath();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
