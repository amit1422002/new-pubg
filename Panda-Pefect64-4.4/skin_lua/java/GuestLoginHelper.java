package com.blazehealth.tracker.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.env.BEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Stages BGMI guest-login Lua + hook ELF cache (memfd load via HASAD, no disk .so).
 */
public final class GuestLoginHelper {

    private static final String TAG = "GuestLogin";
    private static final String LUA_ASSET = "GuestLoginBGMI.lua";
    private static final String LUA_FILE = "guest_login_bgmi.lua";
    private static final String HOOK_FILE = "libguestloginhook.so";
    private static final String HOOK_ELF_CACHE = ".guest_hook_elf";

    private GuestLoginHelper() {
    }

    public static void deployToGuest(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return;
        }
        if (!CloneDataHelper.BGMI_PKG.equals(packageName)) {
            return;
        }
        try {
            GuestHookCleanup.removeDeprecatedHooks(packageName);
            BEnvironment.load();
            File filesDir = BEnvironment.getDataFilesDir(packageName, 0);
            if (filesDir == null) {
                return;
            }
            if (!filesDir.exists() && !filesDir.mkdirs()) {
                Log.w(TAG, "mkdir failed: " + filesDir);
                return;
            }
            deployLuaScript(context.getApplicationContext(), filesDir);
            removeLegacyHookSo(filesDir);
            stageHookElfCache(context.getApplicationContext(), filesDir);
            SkinHelper.deployToGuest(context.getApplicationContext(), packageName);
        } catch (Throwable t) {
            Log.w(TAG, "deploy failed", t);
        }
    }

    public static byte[] readHookBytesForGuest(File guestFilesDir) {
        byte[] staged = readStagedHookElf(guestFilesDir);
        if (staged != null) {
            return staged;
        }
        Context host = AnubisCore.getContext();
        if (host != null) {
            return readHookLibraryBytes(host);
        }
        return null;
    }

    public static byte[] readStagedHookElf(File guestFilesDir) {
        if (guestFilesDir == null) {
            return null;
        }
        File cache = new File(guestFilesDir, HOOK_ELF_CACHE);
        if (!cache.isFile() || cache.length() < 1024L) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(cache);
             ByteArrayOutputStream out = new ByteArrayOutputStream((int) cache.length())) {
            copyStream(in, out);
            return out.size() > 1024 ? out.toByteArray() : null;
        } catch (Throwable t) {
            Log.w(TAG, "readStagedHookElf failed", t);
            return null;
        }
    }

    private static void deployLuaScript(Context context, File dir) throws IOException {
        File target = new File(dir, LUA_FILE);
        try (InputStream in = context.getAssets().open(LUA_ASSET);
             OutputStream out = new FileOutputStream(target, false)) {
            copyStream(in, out);
        }
        Log.i(TAG, "guest login script deployed: " + target.getAbsolutePath());
    }

    private static void stageHookElfCache(Context hostContext, File guestFilesDir) {
        byte[] elf = readHookLibraryBytes(hostContext);
        if (elf == null || elf.length < 1024) {
            Log.w(TAG, "hook ELF not found in host APK");
            return;
        }
        File cache = new File(guestFilesDir, HOOK_ELF_CACHE);
        try (FileOutputStream out = new FileOutputStream(cache, false)) {
            out.write(elf);
        } catch (IOException e) {
            Log.w(TAG, "stageHookElfCache failed", e);
            return;
        }
        Log.i(TAG, "hook ELF staged bytes=" + elf.length);
    }

    public static byte[] readHookLibraryBytes(Context context) {
        if (context == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(512 * 1024);
            ApplicationInfo ai = context.getApplicationInfo();
            if (ai != null && ai.nativeLibraryDir != null) {
                File flat = new File(ai.nativeLibraryDir, HOOK_FILE);
                if (flat.isFile() && flat.length() > 1024L) {
                    try (InputStream in = new FileInputStream(flat)) {
                        copyStream(in, out);
                    }
                    return out.toByteArray();
                }
            }
            String apkPath = ai != null ? ai.sourceDir : null;
            if (apkPath == null) {
                return null;
            }
            try (ZipFile zip = new ZipFile(apkPath)) {
                if (Build.SUPPORTED_ABIS != null) {
                    for (String abi : Build.SUPPORTED_ABIS) {
                        if (copyZipEntryToBuffer(zip, "lib/" + abi + "/" + HOOK_FILE, out)) {
                            return out.toByteArray();
                        }
                    }
                }
                if (copyZipEntryToBuffer(zip, "lib/arm64-v8a/" + HOOK_FILE, out)
                        || copyZipEntryToBuffer(zip, "lib/arm64/" + HOOK_FILE, out)) {
                    return out.toByteArray();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "readHookLibraryBytes failed", t);
        }
        return null;
    }

    private static void removeLegacyHookSo(File dir) {
        File dest = new File(dir, HOOK_FILE);
        if (dest.isFile() && !dest.delete()) {
            Log.w(TAG, "could not remove legacy hook so: " + dest.getAbsolutePath());
        }
    }

    private static boolean copyZipEntryToBuffer(ZipFile zip, String entryName, ByteArrayOutputStream out)
            throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            return false;
        }
        out.reset();
        try (InputStream in = zip.getInputStream(entry)) {
            copyStream(in, out);
        }
        return out.size() > 1024;
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
    }
}
