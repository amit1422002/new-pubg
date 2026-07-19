package com.anubis.loader.core.device;

import android.text.TextUtils;
import android.util.Log;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.entity.DeviceSpoofConfig;
import com.anubis.loader.utils.FileUtils;
import com.anubis.loader.utils.Md5Utils;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists per-user/per-package device spoof values; read by telephony and settings hooks in guest processes.
 */
public final class DeviceSpoofManager {

    private static final String TAG = "DeviceSpoofManager";
    private static final DeviceSpoofManager INSTANCE = new DeviceSpoofManager();

    private final Map<String, DeviceSpoofConfig> mConfigs = new ConcurrentHashMap<>();
    private volatile long mLastLoadMtime;

    public static DeviceSpoofManager get() {
        return INSTANCE;
    }

    public static String configKey(int userId, String packageName) {
        return userId + ":" + packageName;
    }

    public void reload() {
        File file = BEnvironment.getDeviceSpoofConf();
        long mtime = file.exists() ? file.lastModified() : 0L;
        if (mtime == mLastLoadMtime && !mConfigs.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (mtime == mLastLoadMtime && !mConfigs.isEmpty()) {
                return;
            }
            mConfigs.clear();
            if (!file.exists()) {
                mLastLoadMtime = mtime;
                return;
            }
            try {
                String raw = FileUtils.readToString(file.getAbsolutePath());
                if (TextUtils.isEmpty(raw)) {
                    mLastLoadMtime = mtime;
                    return;
                }
                JSONObject root = new JSONObject(raw);
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    mConfigs.put(key, DeviceSpoofConfig.fromJson(root.getJSONObject(key)));
                }
                mLastLoadMtime = mtime;
            } catch (Throwable t) {
                Log.e(TAG, "reload failed", t);
            }
        }
    }

    public void setConfig(int userId, String packageName, DeviceSpoofConfig config) {
        if (TextUtils.isEmpty(packageName)) return;
        reload();
        String key = configKey(userId, packageName);
        if (config == null || !config.enabled) {
            mConfigs.remove(key);
        } else {
            mConfigs.put(key, config);
        }
        persist();
    }

    public DeviceSpoofConfig getConfig(int userId, String packageName) {
        reload();
        return mConfigs.get(configKey(userId, packageName));
    }

    public void clearConfig(int userId, String packageName) {
        setConfig(userId, packageName, null);
    }

    public boolean isEnabled(int userId, String packageName) {
        if (shouldSpoofCurrentProcess()) {
            return true;
        }
        DeviceSpoofConfig c = getConfig(userId, packageName);
        return c != null && c.enabled;
    }

    /** Guest processes always get stable per-clone IDs unless explicitly overridden in config. */
    public String resolveImei(int userId, String packageName) {
        if (!shouldSpoofCurrentProcess()) {
            return null;
        }
        DeviceSpoofConfig c = getConfig(userId, packageName);
        if (c != null && !TextUtils.isEmpty(c.imei)) {
            return c.imei.trim();
        }
        return DeviceSpoofIds.stableImei(userId, packageName);
    }

    public String resolveSerial(int userId, String packageName) {
        if (!shouldSpoofCurrentProcess()) {
            return null;
        }
        DeviceSpoofConfig c = getConfig(userId, packageName);
        if (c != null && !TextUtils.isEmpty(c.serial)) {
            return c.serial.trim();
        }
        return DeviceSpoofIds.stableSerial(userId, packageName);
    }

    public String resolveAndroidId(int userId, String packageName) {
        if (!shouldSpoofCurrentProcess()) {
            return null;
        }
        DeviceSpoofConfig c = getConfig(userId, packageName);
        if (c != null && !TextUtils.isEmpty(c.androidId)) {
            return c.androidId.trim().toLowerCase(Locale.US);
        }
        return DeviceSpoofIds.stableAndroidId(userId, packageName);
    }

    public String resolveWifiMac(int userId, String packageName) {
        if (!shouldSpoofCurrentProcess()) {
            return null;
        }
        return DeviceSpoofIds.stableWifiMac(userId, packageName);
    }

    /** Legacy default when spoof disabled. */
    public static String legacyTelephonyId() {
        return Md5Utils.md5(AnubisCore.getHostPkg());
    }

    public static DeviceSpoofConfig newRandomConfig() {
        DeviceSpoofConfig c = new DeviceSpoofConfig();
        c.enabled = true;
        c.androidId = DeviceSpoofIds.randomAndroidId();
        c.imei = DeviceSpoofIds.randomImei();
        c.imsi = DeviceSpoofIds.stableImei(0, "random:imsi");
        c.serial = DeviceSpoofIds.randomSerial();
        c.wifiMac = DeviceSpoofIds.stableWifiMac(0, "random:wifi");
        c.model = null;
        c.brand = null;
        return c;
    }

    public static boolean shouldSpoofCurrentProcess() {
        String pkg = BActivityThread.getAppPackageName();
        if (TextUtils.isEmpty(pkg)) return false;
        String host = AnubisCore.getHostPkg();
        if (host != null && host.equals(pkg)) return false;
        // microG / Play services must see real host-aligned IDs and paths.
        if (GmsCore.isGoogleAppOrService(pkg)) return false;
        return true;
    }

    private void persist() {
        try {
            File file = BEnvironment.getDeviceSpoofConf();
            FileUtils.mkdirs(file.getParentFile().getAbsolutePath());
            JSONObject root = new JSONObject();
            for (Map.Entry<String, DeviceSpoofConfig> e : mConfigs.entrySet()) {
                root.put(e.getKey(), e.getValue().toJson());
            }
            FileUtils.writeToFile(root.toString().getBytes(StandardCharsets.UTF_8), file);
            mLastLoadMtime = file.lastModified();
        } catch (Throwable t) {
            Log.e(TAG, "persist failed", t);
        }
    }
}
