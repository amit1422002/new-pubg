package com.blazehealth.tracker.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import com.anubis.loader.core.env.BEnvironment;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Stages patchless libanogs offset blocker into cloned BGMI files/. */
public final class AnogsBlockerHelper {

    private static final String TAG = "AnogsBlocker";
    /** Set true to re-enable libanogs offset patching. */
    public static final boolean ENABLED = false;
    static final String HOOK_FILE = "libanogsblocker.so";
    static final String OFFSETS_FILE = "anogs_block_offsets.txt";

    private AnogsBlockerHelper() {
    }

    public static void deployToGuest(Context context, String packageName) {
        if (!ENABLED) {
            return;
        }
        if (context == null || packageName == null || packageName.isEmpty()) {
            return;
        }
        if (!CloneDataHelper.BGMI_PKG.equals(packageName)) {
            return;
        }
        try {
            BEnvironment.load();
            File filesDir = BEnvironment.getDataFilesDir(packageName, 0);
            if (filesDir == null) {
                return;
            }
            if (!filesDir.exists() && !filesDir.mkdirs()) {
                Log.w(TAG, "mkdir failed: " + filesDir);
                return;
            }
            deployHookLibrary(context.getApplicationContext(), filesDir);
            deployDefaultOffsets(filesDir);
            deployBlockEnabledFlag(filesDir);
        } catch (Throwable t) {
            Log.w(TAG, "deploy failed", t);
        }
    }

    private static final String[] DEFAULT_OFFSETS = {"0x490988"};
    /** Skip-trap on these RVAs crashes BGMI — never block them. */
    private static final String[] DISALLOWED_OFFSETS = {"0x51fa80", "0x213360"};

    private static boolean isDisallowedOffset(String normalized) {
        if (normalized == null) {
            return true;
        }
        for (String banned : DISALLOWED_OFFSETS) {
            if (normalized.equals(banned.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    private static void deployDefaultOffsets(File dir) throws IOException {
        File target = new File(dir, OFFSETS_FILE);
        Set<String> offsets = new LinkedHashSet<>();
        for (String off : DEFAULT_OFFSETS) {
            offsets.add(off.toLowerCase(Locale.US));
        }
        if (target.isFile()) {
            try (BufferedReader in = new BufferedReader(new FileReader(target))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String normalized = normalizeOffsetLine(line);
                    if (normalized == null || isDisallowedOffset(normalized)) {
                        continue;
                    }
                    offsets.add(normalized);
                }
            }
        }
        try (Writer out = new OutputStreamWriter(new FileOutputStream(target, false), StandardCharsets.UTF_8)) {
            out.write("# libanogs RVAs to block via mprotect trap (hex, one per line)\n");
            out.write("# 0x51FA80 / 0x213360 do NOT add — game crash\n");
            for (String off : offsets) {
                out.write(off);
                out.write('\n');
            }
        }
        Log.i(TAG, "offsets synced (" + offsets.size() + "): " + target.getAbsolutePath());
    }

    private static void deployBlockEnabledFlag(File dir) throws IOException {
        File target = new File(dir, "anogs_block_enabled.txt");
        if (target.isFile()) {
            return;
        }
        try (Writer out = new OutputStreamWriter(new FileOutputStream(target, false), StandardCharsets.UTF_8)) {
            out.write("1\n");
        }
    }

    private static String normalizeOffsetLine(String line) {
        if (line == null) {
            return null;
        }
        String s = line.trim();
        if (s.isEmpty() || s.startsWith("#")) {
            return null;
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (!s.matches("(?i)[0-9a-f]+")) {
            return null;
        }
        return ("0x" + s).toLowerCase(Locale.US);
    }

    private static void deployHookLibrary(Context context, File dir) throws IOException {
        File dest = new File(dir, HOOK_FILE);
        if (!extractHookLibrary(context, dest)) {
            Log.w(TAG, "hook lib missing in apk");
            return;
        }
        dest.setReadable(true, false);
        dest.setExecutable(true, false);
        Log.i(TAG, "anogs blocker deployed: " + dest.getAbsolutePath()
                + " (" + dest.length() + " bytes)");
    }

    private static boolean extractHookLibrary(Context context, File dest) throws IOException {
        ApplicationInfo ai = context.getApplicationInfo();
        if (ai != null && ai.nativeLibraryDir != null) {
            File flat = new File(ai.nativeLibraryDir, HOOK_FILE);
            if (flat.isFile() && flat.length() > 1024L) {
                copyFile(flat, dest);
                return true;
            }
        }
        String apkPath = ai != null ? ai.sourceDir : null;
        if (apkPath == null) {
            return false;
        }
        try (ZipFile zip = new ZipFile(apkPath)) {
            if (Build.SUPPORTED_ABIS != null) {
                for (String abi : Build.SUPPORTED_ABIS) {
                    if (extractZipEntry(zip, "lib/" + abi + "/" + HOOK_FILE, dest)) {
                        return true;
                    }
                }
            }
            return extractZipEntry(zip, "lib/arm64-v8a/" + HOOK_FILE, dest)
                    || extractZipEntry(zip, "lib/arm64/" + HOOK_FILE, dest);
        }
    }

    private static boolean extractZipEntry(ZipFile zip, String entryName, File dest) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            return false;
        }
        try (InputStream in = zip.getInputStream(entry);
             OutputStream out = new FileOutputStream(dest, false)) {
            copyStream(in, out);
        }
        return dest.isFile() && dest.length() > 1024L;
    }

    private static void copyFile(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest, false)) {
            copyStream(in, out);
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
    }
}
