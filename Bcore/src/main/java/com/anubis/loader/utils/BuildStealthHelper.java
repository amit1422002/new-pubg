package com.anubis.loader.utils;

import android.os.Build;
import android.text.TextUtils;

import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.entity.DeviceSpoofConfig;
import com.anubis.loader.core.device.DeviceSpoofManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Strips emulator / debug fingerprints from Build and ro.* reads in guest processes.
 */
public final class BuildStealthHelper {

    private static final Map<String, String> EMULATOR_PROP_OVERRIDES = new HashMap<>();

    static {
        EMULATOR_PROP_OVERRIDES.put("ro.kernel.qemu", "0");
        EMULATOR_PROP_OVERRIDES.put("ro.kernel.qemu.gles", "0");
        EMULATOR_PROP_OVERRIDES.put("ro.hardware.audio.primary", "generic");
        EMULATOR_PROP_OVERRIDES.put("init.svc.qemu-props", "stopped");
        EMULATOR_PROP_OVERRIDES.put("init.svc.qemud", "stopped");
        EMULATOR_PROP_OVERRIDES.put("qemu.hw.mainkeys", "0");
        EMULATOR_PROP_OVERRIDES.put("ro.boot.qemu", "0");
        EMULATOR_PROP_OVERRIDES.put("ro.debuggable", "0");
        EMULATOR_PROP_OVERRIDES.put("ro.secure", "1");
        EMULATOR_PROP_OVERRIDES.put("ro.build.tags", "release-keys");
        EMULATOR_PROP_OVERRIDES.put("ro.build.type", "user");
    }

    private BuildStealthHelper() {
    }

    public static void applyForGuest() {
        if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
            return;
        }
        patchBuildFields();
    }

    /** Called from native SystemProperties hook and Java compat layer. */
    public static String resolveProperty(String key, String original) {
        if (!DeviceSpoofManager.shouldSpoofCurrentProcess() || TextUtils.isEmpty(key)) {
            return original;
        }
        String override = EMULATOR_PROP_OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        if (looksEmulatorValue(original)) {
            return sanitizeValue(key, original);
        }
        int userId = BActivityThread.getUserId();
        String pkg = BActivityThread.getAppPackageName();
        DeviceSpoofConfig cfg = DeviceSpoofManager.get().getConfig(userId, pkg);
        if (cfg != null) {
            if ("ro.product.model".equals(key) && !TextUtils.isEmpty(cfg.model)) {
                return cfg.model.trim();
            }
            if ("ro.product.brand".equals(key) && !TextUtils.isEmpty(cfg.brand)) {
                return cfg.brand.trim();
            }
            if ("ro.product.manufacturer".equals(key) && !TextUtils.isEmpty(cfg.brand)) {
                return cfg.brand.trim();
            }
        }
        return original;
    }

    public static int resolvePropertyInt(String key, int original) {
        String resolved = resolveProperty(key, String.valueOf(original));
        try {
            return Integer.parseInt(resolved.trim());
        } catch (Throwable ignored) {
            return original;
        }
    }

    public static boolean resolvePropertyBoolean(String key, boolean original) {
        String resolved = resolveProperty(key, original ? "1" : "0");
        return "1".equals(resolved) || "true".equalsIgnoreCase(resolved);
    }

    private static void patchBuildFields() {
        String model = sanitizeValue("ro.product.model", Build.MODEL);
        String brand = sanitizeValue("ro.product.brand", Build.BRAND);
        String manufacturer = sanitizeValue("ro.product.manufacturer", Build.MANUFACTURER);
        String hardware = sanitizeValue("ro.hardware", Build.HARDWARE);
        String product = sanitizeValue("ro.product.name", Build.PRODUCT);
        String device = sanitizeValue("ro.product.device", Build.DEVICE);
        String board = sanitizeValue("ro.product.board", Build.BOARD);
        String fingerprint = sanitizeFingerprint(Build.FINGERPRINT);
        String tags = "release-keys";
        String type = "user";

        int userId = BActivityThread.getUserId();
        String pkg = BActivityThread.getAppPackageName();
        DeviceSpoofConfig cfg = DeviceSpoofManager.get().getConfig(userId, pkg);
        if (cfg != null) {
            if (!TextUtils.isEmpty(cfg.model)) {
                model = cfg.model.trim();
            }
            if (!TextUtils.isEmpty(cfg.brand)) {
                brand = cfg.brand.trim();
                manufacturer = brand;
            }
        }

        setStaticField(Build.class, "MODEL", model);
        setStaticField(Build.class, "BRAND", brand);
        setStaticField(Build.class, "MANUFACTURER", manufacturer);
        setStaticField(Build.class, "HARDWARE", hardware);
        setStaticField(Build.class, "PRODUCT", product);
        setStaticField(Build.class, "DEVICE", device);
        setStaticField(Build.class, "BOARD", board);
        setStaticField(Build.class, "FINGERPRINT", fingerprint);
        setStaticField(Build.class, "TAGS", tags);
        setStaticField(Build.class, "TYPE", type);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStaticField(Build.class, "SUPPORTED_ABIS", new String[]{"arm64-v8a", "armeabi-v7a"});
        }
    }

    private static String sanitizeFingerprint(String fingerprint) {
        if (TextUtils.isEmpty(fingerprint)) {
            return fingerprint;
        }
        String out = fingerprint;
        out = out.replace("test-keys", "release-keys");
        out = out.replace("generic", "device");
        out = out.replace("goldfish", "device");
        out = out.replace("ranchu", "device");
        out = out.replace("sdk_gphone", "phone");
        out = out.replace("/eng/", "/user/");
        return out;
    }

    private static String sanitizeValue(String key, String value) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }
        String lower = value.toLowerCase(Locale.US);
        if (!looksEmulatorValue(value)) {
            return value;
        }
        if (lower.contains("goldfish") || lower.contains("ranchu") || lower.contains("vbox")
                || lower.contains("generic") || lower.contains("sdk_gphone")
                || lower.contains("google_sdk") || lower.contains("emulator")
                || lower.contains("android sdk")) {
            if (key != null && key.contains("hardware")) {
                return "qcom";
            }
            if (key != null && key.contains("board")) {
                return "msm8953";
            }
            return "SM-G991B";
        }
        if ("test-keys".equals(lower) || lower.contains("test-keys")) {
            return "release-keys";
        }
        if ("eng".equals(lower) || "userdebug".equals(lower)) {
            return "user";
        }
        return value;
    }

    static boolean looksEmulatorValue(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String lower = value.toLowerCase(Locale.US);
        return lower.contains("goldfish")
                || lower.contains("ranchu")
                || lower.contains("generic")
                || lower.contains("sdk_gphone")
                || lower.contains("google_sdk")
                || lower.contains("emulator")
                || lower.contains("vbox")
                || lower.contains("test-keys")
                || "eng".equals(lower)
                || "userdebug".equals(lower)
                || lower.contains("android sdk");
    }

    private static void setStaticField(Class<?> clazz, String name, Object value) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            Field accessFlags = Field.class.getDeclaredField("accessFlags");
            accessFlags.setAccessible(true);
            accessFlags.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, value);
        } catch (Throwable ignored) {
        }
    }
}
