package com.anubis.loader.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.env.BEnvironment;

/**
 * Stable per-clone telephony IDs for virtual user 0.
 * <p>
 * Optional override file (host or guest files dir):
 * {@code device_spoof.ini} ΓÇö keys {@code imei}, {@code imsi}, {@code serial}, {@code meid}, {@code nonce}.
 * Set {@code nonce=<millis>} (no imei line) to get a new random IMEI on next read.
 */
public final class VirtualDeviceIds {

    public static final int SPOOF_USER_ID = 0;
    private static final String SPOOF_FILE = "device_spoof.ini";

    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> FILE_OVERRIDES = new HashMap<>();
    private static volatile long sFileLastModified = -1L;
    private static volatile String sLoadedIniPath = "";

    private VirtualDeviceIds() {
    }

    public static void clearCache() {
        CACHE.clear();
        FILE_OVERRIDES.clear();
        sFileLastModified = -1L;
        sLoadedIniPath = "";
    }

    public static boolean shouldSpoof() {
        return shouldSpoof(BActivityThread.getUserId());
    }

    public static boolean shouldSpoof(int userId) {
        return userId == SPOOF_USER_ID;
    }

    public static String getImei() {
        return getImei(BActivityThread.getUserId(), currentPackage());
    }

    public static String getImei(int userId, String packageName) {
        return resolve("imei", userId, packageName, VirtualDeviceIds::buildImei);
    }

    public static String getMeid() {
        return getMeid(BActivityThread.getUserId(), currentPackage());
    }

    public static String getMeid(int userId, String packageName) {
        return resolve("meid", userId, packageName, VirtualDeviceIds::buildMeid);
    }

    public static String getImsi() {
        return getImsi(BActivityThread.getUserId(), currentPackage());
    }

    public static String getImsi(int userId, String packageName) {
        return resolve("imsi", userId, packageName, VirtualDeviceIds::buildImsi);
    }

    public static String getSerial() {
        return getSerial(BActivityThread.getUserId(), currentPackage());
    }

    public static String getSerial(int userId, String packageName) {
        return resolve("serial", userId, packageName, VirtualDeviceIds::buildSerial);
    }

    private static String currentPackage() {
        String pkg = BActivityThread.getAppPackageName();
        return pkg != null ? pkg : "unknown";
    }

    private interface Generator {
        String generate(String seed);
    }

    private static String resolve(String kind, int userId, String packageName, Generator gen) {
        reloadIniIfNeeded(userId, packageName);
        String fromFile = FILE_OVERRIDES.get(kind);
        if (fromFile != null && !fromFile.isEmpty()) {
            return fromFile;
        }
        String nonce = FILE_OVERRIDES.getOrDefault("nonce", "");
        String cacheKey = kind + ':' + userId + ':' + packageName + ':' + nonce;
        String hit = CACHE.get(cacheKey);
        if (hit != null) {
            return hit;
        }
        String seed = sha256(kind + "|" + userId + "|" + packageName + "|" + nonce);
        String value = gen.generate(seed);
        CACHE.put(cacheKey, value);
        return value;
    }

    private static void reloadIniIfNeeded(int userId, String packageName) {
        if (!shouldSpoof(userId)) {
            return;
        }
        File filesDir = BEnvironment.getDataFilesDir(packageName, userId);
        if (filesDir == null) {
            return;
        }
        File ini = new File(filesDir, SPOOF_FILE);
        final String path = ini.getAbsolutePath();
        final long mod = ini.isFile() ? ini.lastModified() : 0L;
        if (path.equals(sLoadedIniPath) && mod == sFileLastModified) {
            return;
        }
        synchronized (FILE_OVERRIDES) {
            if (path.equals(sLoadedIniPath) && mod == sFileLastModified) {
                return;
            }
            FILE_OVERRIDES.clear();
            if (ini.isFile()) {
                parseIni(ini);
            }
            sLoadedIniPath = path;
            sFileLastModified = mod;
            CACHE.clear();
        }
    }

    private static void parseIni(File ini) {
        try (BufferedReader br = new BufferedReader(new FileReader(ini))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim().toLowerCase();
                String val = line.substring(eq + 1).trim();
                if (!val.isEmpty()) {
                    FILE_OVERRIDES.put(key, val);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** 15-digit IMEI with Luhn check digit. */
    private static String buildImei(String seed) {
        byte[] b = seed.getBytes(StandardCharsets.UTF_8);
        int a = bytesToInt(b, 0);
        int c = bytesToInt(b, 4);
        int d = bytesToInt(b, 8);
        StringBuilder body = new StringBuilder(14);
        body.append("35");
        appendDigits(body, 6, a);
        appendDigits(body, 6, c ^ d);
        return body.toString() + luhnCheckDigit(body.toString());
    }

    private static String buildMeid(String seed) {
        return sha256("meid|" + seed).substring(0, 14).toUpperCase();
    }

    private static String buildImsi(String seed) {
        byte[] b = seed.getBytes(StandardCharsets.UTF_8);
        int n = bytesToInt(b, 0);
        StringBuilder imsi = new StringBuilder(15);
        imsi.append("404");
        imsi.append(String.format("%02d", 10 + (Math.abs(n) % 80)));
        appendDigits(imsi, 10, bytesToInt(b, 4) ^ bytesToInt(b, 8));
        return imsi.toString();
    }

    private static String buildSerial(String seed) {
        return "R58M" + sha256("serial|" + seed).toUpperCase().substring(0, 10);
    }

    private static void appendDigits(StringBuilder out, int count, int seed) {
        int x = Math.abs(seed);
        for (int i = 0; i < count; i++) {
            out.append((char) ('0' + (x % 10)));
            x /= 10;
            if (x == 0) {
                x = Math.abs(seed) + (i + 1) * 31;
            }
        }
    }

    private static int luhnCheckDigit(String digits) {
        int sum = 0;
        boolean alt = true;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alt = !alt;
        }
        return (10 - (sum % 10)) % 10;
    }

    private static int bytesToInt(byte[] b, int off) {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            v = (v << 8) | (b[(off + i) % b.length] & 0xff);
        }
        return v;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte x : dig) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            return Md5Utils.md5(input);
        }
    }
}
