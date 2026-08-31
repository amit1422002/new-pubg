package com.blazehealth.tracker.utils;

import android.content.Context;
import android.util.Log;

import com.anubis.loader.core.env.BEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stages in-game Lua mod (points 1-5) beside guest login hook — same path as skin mod.
 */
public final class GameModHelper {

    private static final String TAG = "GameMod";
    private static final String MOD_ASSET = "bgmi_game_mod.lua";
    private static final String MOD_FILE = "bgmi_game_mod.lua";
    private static final String CONFIG_ASSET = "gamemod_config.ini";
    private static final String CONFIG_FILE = "gamemod_config.ini";
    private static final long MIN_MOD_BYTES = 4096L;

    private GameModHelper() {
    }

    public static void deployToGuest(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return;
        }
        if (!CloneDataHelper.BGMI_PKG.equals(packageName)) {
            return;
        }
        try {
            BEnvironment.load();
            File intFiles = BEnvironment.getDataFilesDir(packageName, 0);
            File extFiles = BEnvironment.getExternalDataFilesDir(packageName, 0);
            if (intFiles == null) {
                Log.w(TAG, "int files dir null");
                return;
            }
            deployToDir(context.getApplicationContext(), intFiles);
            if (extFiles != null) {
                if (!extFiles.exists()) {
                    extFiles.mkdirs();
                }
                deployConfig(context.getApplicationContext(), extFiles);
            }
        } catch (Throwable t) {
            Log.w(TAG, "game mod deploy failed", t);
        }
    }

    /** Deploy merged mod lua + config into the same files/ dir as the hook. */
    public static void deployToDir(Context context, File filesDir) throws IOException {
        if (context == null || filesDir == null) {
            return;
        }
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IOException("mkdir failed: " + filesDir);
        }
        File mod = new File(filesDir, MOD_FILE);
        copyAssetToFile(context, MOD_ASSET, mod);
        if (!mod.isFile() || mod.length() < MIN_MOD_BYTES) {
            throw new IOException("mod file too small after deploy: " + mod
                    + " len=" + (mod.isFile() ? mod.length() : -1));
        }
        deployConfig(context, filesDir);
        Log.i(TAG, "game mod deployed: " + mod.getAbsolutePath() + " (" + mod.length() + " bytes)");
    }

    private static void deployConfig(Context context, File dir) throws IOException {
        File target = new File(dir, CONFIG_FILE);
        copyAssetToFile(context, CONFIG_ASSET, target);
        Log.i(TAG, "gamemod_config.ini deployed: " + target.getAbsolutePath());
    }

    private static void copyAssetToFile(Context context, String assetName, File dest) throws IOException {
        try (InputStream in = context.getAssets().open(assetName);
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

    /** Toggle in-guest Lua mod flags without touching libUE4 .text (e.g. MAGIC_BULLET). */
    public static void setConfigFlag(Context context, String packageName, String key, boolean enabled) {
        if (context == null || packageName == null || key == null || key.isEmpty()) {
            return;
        }
        try {
            BEnvironment.load();
            File intFiles = BEnvironment.getDataFilesDir(packageName, 0);
            File extFiles = BEnvironment.getExternalDataFilesDir(packageName, 0);
            if (intFiles != null) {
                patchConfigFile(new File(intFiles, CONFIG_FILE), key, enabled);
            }
            if (extFiles != null) {
                if (!extFiles.exists()) {
                    extFiles.mkdirs();
                }
                patchConfigFile(new File(extFiles, CONFIG_FILE), key, enabled);
            }
            Log.i(TAG, "gamemod " + key + "=" + (enabled ? 1 : 0));
        } catch (Throwable t) {
            Log.w(TAG, "setConfigFlag failed", t);
        }
    }

    private static void patchConfigFile(File config, String key, boolean enabled) throws IOException {
        if (config == null) {
            return;
        }
        final String want = key + "=" + (enabled ? "1" : "0");
        String body = "";
        if (config.isFile()) {
            byte[] raw = java.nio.file.Files.readAllBytes(config.toPath());
            body = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
        }
        final String lineKey = key + "=";
        final StringBuilder out = new StringBuilder();
        boolean replaced = false;
        for (String line : body.split("\n", -1)) {
            if (line.startsWith(lineKey)) {
                out.append(want).append('\n');
                replaced = true;
            } else {
                out.append(line).append('\n');
            }
        }
        if (!replaced) {
            if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            out.append(want).append('\n');
        }
        java.nio.file.Files.write(config.toPath(), out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
