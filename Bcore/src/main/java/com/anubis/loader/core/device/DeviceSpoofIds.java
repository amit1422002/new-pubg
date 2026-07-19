package com.anubis.loader.core.device;

import com.anubis.loader.utils.Md5Utils;

import java.security.SecureRandom;
import java.util.Locale;

/** Stable / random device id formatting for spoof layer. */
public final class DeviceSpoofIds {

    public static final String PROFILE_MODEL = "SM-G991B";
    public static final String PROFILE_BRAND = "samsung";
    public static final String PROFILE_MARKETING_NAME = "Galaxy S21 5G";

    private static final SecureRandom RANDOM = new SecureRandom();

    private DeviceSpoofIds() {
    }

    public static String stableAndroidId(int userId, String pkg) {
        String seed = Md5Utils.md5(pkg + ":android_id:" + userId);
        if (seed == null) seed = "0000000000000000";
        return seed.substring(0, Math.min(16, seed.length())).toLowerCase(Locale.US);
    }

    public static String stableImei(int userId, String pkg) {
        return digitsFromSeed(pkg + ":imei:" + userId, 15);
    }

    public static String stableSerial(int userId, String pkg) {
        String seed = Md5Utils.md5(pkg + ":serial:" + userId);
        if (seed == null) seed = "unknown";
        return seed.substring(0, Math.min(16, seed.length())).toUpperCase(Locale.US);
    }

    /** Locally administered, unique per clone/user. */
    public static String stableWifiMac(int userId, String pkg) {
        String md5 = Md5Utils.md5(pkg + ":wifi_mac:" + userId);
        if (md5 == null || md5.length() < 12) {
            md5 = "aabbccddeeff";
        }
        int[] octets = new int[6];
        for (int i = 0; i < 6; i++) {
            int hi = Character.digit(md5.charAt(i * 2), 16);
            int lo = Character.digit(md5.charAt(i * 2 + 1), 16);
            octets[i] = ((hi << 4) | lo) & 0xFF;
        }
        octets[0] = (octets[0] | 0x02) & 0xFE;
        StringBuilder mac = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                mac.append(':');
            }
            mac.append(String.format(Locale.US, "%02x", octets[i]));
        }
        return mac.toString();
    }

    public static String randomAndroidId() {
        byte[] b = new byte[8];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder(16);
        for (byte value : b) {
            sb.append(String.format(Locale.US, "%02x", value));
        }
        return sb.toString();
    }

    public static String randomImei() {
        StringBuilder sb = new StringBuilder("86");
        for (int i = 0; i < 13; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomSerial() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String digitsFromSeed(String seed, int len) {
        String md5 = Md5Utils.md5(seed);
        if (md5 == null) md5 = "000000000000000";
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < md5.length() && digits.length() < len; i++) {
            char c = md5.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        while (digits.length() < len) {
            digits.append('0');
        }
        return digits.substring(0, len);
    }
}
