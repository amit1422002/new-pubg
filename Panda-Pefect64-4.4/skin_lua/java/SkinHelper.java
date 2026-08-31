package com.blazehealth.tracker.utils;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.anubis.loader.core.env.BEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stages skin mod config under cloned BGMI external data files/
 * (game-visible: /storage/emulated/0/Android/data/com.pubg.imobile/files/).
 */
public final class SkinHelper {

    private static final String TAG = "SkinMod";
    private static final String CONFIG_ASSET = "skin_config.ini";
    private static final String CONFIG_FILE = "config.ini";
    private static final String LUA_ASSET = "skin_mod_bgmi.lua";
    private static final String LUA_FILE = "skin_mod_bgmi.lua";
    private static final String PATHS_FILE = "skin_data.paths";
    private static final String ATTACHMENTS_ASSET = "skin_attachments.txt";
    private static final String ATTACHMENTS_FILE = "attachments.txt";
    private static final String ATTACH_MAPS_ASSET = "skin_attachment_maps.lua";
    private static final String ATTACH_MAPS_FILE = "skin_attachment_maps.lua";
    /** Paths visible inside the cloned BGMI process (not host app VFS). */
    private static final String GAME_FILES_PATH =
            "/storage/emulated/0/Android/data/" + CloneDataHelper.BGMI_PKG + "/files";
    private static final String GAME_INT_FILES_PATH =
            "/data/user/0/" + CloneDataHelper.BGMI_PKG + "/files";

    private static final String RELOAD_STAMP_FILE = "skin_reload.stamp";
    private static long sLastStampWriteMs = 0L;

    private SkinHelper() {
    }

    /** Update one config.ini key in cloned BGMI (gun or outfit). */
    public static boolean updateConfigValue(Context context, String configKey, int skinId) {
        if (context == null || configKey == null || configKey.isEmpty()) {
            return false;
        }
        try {
            BEnvironment.load();
            File extFiles = BEnvironment.getExternalDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            File intFiles = BEnvironment.getDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            boolean ok = false;
            if (extFiles != null) {
                if (!extFiles.exists()) {
                    extFiles.mkdirs();
                }
                ok |= patchConfigKey(new File(extFiles, CONFIG_FILE), configKey, skinId);
            }
            if (intFiles != null) {
                if (!intFiles.exists()) {
                    intFiles.mkdirs();
                }
                ok |= patchConfigKey(new File(intFiles, CONFIG_FILE), configKey, skinId);
            }
            if (ok) {
                long now = SystemClock.uptimeMillis();
                if (now - sLastStampWriteMs >= 600L) {
                    if (extFiles != null) {
                        writeReloadStamp(extFiles);
                    } else if (intFiles != null) {
                        writeReloadStamp(intFiles);
                    }
                    sLastStampWriteMs = now;
                }
                Log.i(TAG, "config " + configKey + '=' + skinId);
            }
            return ok;
        } catch (Throwable t) {
            Log.w(TAG, "updateConfigValue failed", t);
            return false;
        }
    }

    public static boolean updateWeaponSkin(Context context, String configKey, int skinId) {
        return updateConfigValue(context, configKey, skinId);
    }

    public static int readConfigValue(Context context, String configKey, int defaultValue) {
        if (context == null || configKey == null || configKey.isEmpty()) {
            return defaultValue;
        }
        try {
            BEnvironment.load();
            File extFiles = BEnvironment.getExternalDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            if (extFiles != null) {
                File cfg = new File(extFiles, CONFIG_FILE);
                if (cfg.isFile()) {
                    int v = parseConfigInt(readText(cfg), configKey, Integer.MIN_VALUE);
                    if (v != Integer.MIN_VALUE) {
                        return v;
                    }
                }
            }
            File intFiles = BEnvironment.getDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            if (intFiles != null) {
                File cfg = new File(intFiles, CONFIG_FILE);
                if (cfg.isFile()) {
                    int v = parseConfigInt(readText(cfg), configKey, Integer.MIN_VALUE);
                    if (v != Integer.MIN_VALUE) {
                        return v;
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "readConfigValue failed", t);
        }
        return defaultValue;
    }

    public static int readWeaponSkin(Context context, String configKey, int defaultValue) {
        return readConfigValue(context, configKey, defaultValue);
    }

    private static int parseConfigInt(String ini, String key, int fallback) {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(key) + "=(\\d+)\\s*$").matcher(ini);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static boolean patchConfigKey(File target, String key, int value) throws IOException {
        String body = "";
        if (target.isFile() && target.length() > 0L) {
            body = readText(target);
        } else {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        }
        String newLine = key + '=' + value;
        Pattern line = Pattern.compile("(?m)^" + Pattern.quote(key) + "=\\d+\\s*$");
        String out;
        if (line.matcher(body).find()) {
            out = line.matcher(body).replaceAll(newLine);
        } else if (body.trim().isEmpty()) {
            out = "# Gun skin set from ESP menu\n" + newLine + '\n';
        } else {
            out = body.trim() + '\n' + newLine + '\n';
        }
        writeText(target, out);
        return true;
    }

    private static void writeReloadStamp(File dir) {
        try {
            File stamp = new File(dir, RELOAD_STAMP_FILE);
            try (FileOutputStream out = new FileOutputStream(stamp, false)) {
                out.write(Long.toString(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            Log.w(TAG, "reload stamp write failed", e);
        }
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
            File extFiles = BEnvironment.getExternalDataFilesDir(packageName, 0);
            File intFiles = BEnvironment.getDataFilesDir(packageName, 0);
            if (extFiles == null || intFiles == null) {
                return;
            }
            if (!extFiles.exists() && !extFiles.mkdirs()) {
                Log.w(TAG, "ext files mkdir failed: " + extFiles);
                return;
            }
            if (!intFiles.exists() && !intFiles.mkdirs()) {
                Log.w(TAG, "int files mkdir failed: " + intFiles);
            }
            writePathsFile(extFiles);
            writePathsFile(intFiles);
            deployConfigIfMissing(context.getApplicationContext(), extFiles);
            deployConfigIfMissing(context.getApplicationContext(), intFiles);
            deployAttachmentsIfMissing(context.getApplicationContext(), extFiles);
            deployAttachmentsIfMissing(context.getApplicationContext(), intFiles);
            deployAttachmentMaps(context.getApplicationContext(), extFiles);
            deployAttachmentMaps(context.getApplicationContext(), intFiles);
            deployLuaScript(context.getApplicationContext(), intFiles);
            deployLuaScript(context.getApplicationContext(), extFiles);
            Log.i(TAG, "skin config staged ext=" + extFiles.getAbsolutePath()
                    + " int=" + intFiles.getAbsolutePath());
        } catch (Throwable t) {
            Log.w(TAG, "skin deploy failed", t);
        }
    }

    private static void writePathsFile(File dir) throws IOException {
        File target = new File(dir, PATHS_FILE);
        String body = GAME_FILES_PATH + "\n" + GAME_INT_FILES_PATH + "\n";
        try (FileOutputStream out = new FileOutputStream(target, false)) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deployAttachmentsIfMissing(Context context, File dir) throws IOException {
        File target = new File(dir, ATTACHMENTS_FILE);
        try (InputStream in = context.getAssets().open(ATTACHMENTS_ASSET);
             OutputStream out = new FileOutputStream(target, false)) {
            copyStream(in, out);
        }
        Log.i(TAG, "attachments.txt deployed: " + target.getAbsolutePath());
    }

    private static void deployAttachmentMaps(Context context, File dir) throws IOException {
        File target = new File(dir, ATTACH_MAPS_FILE);
        try (InputStream in = context.getAssets().open(ATTACH_MAPS_ASSET);
             OutputStream out = new FileOutputStream(target, false)) {
            copyStream(in, out);
        }
        Log.i(TAG, "skin_attachment_maps.lua deployed: " + target.getAbsolutePath()
                + " (" + target.length() + " bytes)");
    }

    private static void deployLuaScript(Context context, File intFiles) throws IOException {
        File target = new File(intFiles, LUA_FILE);
        try (InputStream in = context.getAssets().open(LUA_ASSET);
             OutputStream out = new FileOutputStream(target, false)) {
            copyStream(in, out);
        }
        deployAttachmentMaps(context, intFiles);
        Log.i(TAG, "skin lua deployed: " + target.getAbsolutePath()
                + " (" + target.length() + " bytes)");
    }

    private static void deployConfigIfMissing(Context context, File dir) throws IOException {
        File target = new File(dir, CONFIG_FILE);
        String assetBody = readAssetText(context, CONFIG_ASSET);
        if (assetBody.isEmpty()) {
            return;
        }
        if (!target.isFile() || target.length() == 0L) {
            writeText(target, assetBody);
            Log.i(TAG, "config.ini deployed: " + target.getAbsolutePath());
            return;
        }
        String merged = mergeSkinConfigFromAsset(readText(target), assetBody);
        if (!merged.equals(readText(target))) {
            writeText(target, merged);
            Log.i(TAG, "config.ini merged from asset: " + target.getAbsolutePath());
        }
    }

    /** Fill lines that are 0/missing in existing config from asset defaults. */
    private static String mergeSkinConfigFromAsset(String existing, String asset) {
        java.util.Map<String, String> defaults = parseIniLines(asset);
        if (defaults.isEmpty()) {
            return existing;
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        StringBuilder out = new StringBuilder();
        boolean changed = false;
        for (String line : existing.split("\n", -1)) {
            String trimmed = line.trim();
            Matcher m = Pattern.compile("^([A-Za-z0-9_]+)=(\\d+)\\s*$").matcher(trimmed);
            if (m.matches()) {
                String key = m.group(1);
                String val = m.group(2);
                seen.add(key);
                if (defaults.containsKey(key) && ("0".equals(val) || val.isEmpty())) {
                    String def = defaults.get(key);
                    if (def != null && !"0".equals(def)) {
                        out.append(key).append('=').append(def).append('\n');
                        changed = true;
                        continue;
                    }
                }
            }
            out.append(line).append('\n');
        }
        for (java.util.Map.Entry<String, String> e : defaults.entrySet()) {
            if (!seen.contains(e.getKey()) && !"0".equals(e.getValue())) {
                out.append(e.getKey()).append('=').append(e.getValue()).append('\n');
                changed = true;
            }
        }
        return changed ? out.toString() : existing;
    }

    private static java.util.Map<String, String> parseIniLines(String ini) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        Matcher m = Pattern.compile("(?m)^([A-Za-z0-9_]+)=(\\d+)\\s*$").matcher(ini);
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }

    private static boolean isOutfitKey(String key) {
        switch (key) {
            case "SHIRT":
            case "HAT":
            case "FACE":
            case "MASK":
            case "GLOVES":
            case "PANT":
            case "SHOE":
            case "PARACHUTE":
            case "GLIDER":
            case "BACKPACK1":
            case "BACKPACK2":
            case "BACKPACK3":
            case "HELMET1":
            case "HELMET2":
            case "HELMET3":
            case "PET_SKIN":
            case "LobbyTheme":
            case "EMOTE1":
            case "EMOTE2":
            case "EMOTE3":
            case "HALL_EFFECT":
                return true;
            default:
                return false;
        }
    }

    private static String readAssetText(Context context, String asset) throws IOException {
        try (InputStream in = context.getAssets().open(asset)) {
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        }
    }

    private static String readText(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) Math.min(file.length(), 16384L)];
            int n = in.read(buf);
            return n > 0 ? new String(buf, 0, n, StandardCharsets.UTF_8) : "";
        }
    }

    private static void writeText(File file, String body) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
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
