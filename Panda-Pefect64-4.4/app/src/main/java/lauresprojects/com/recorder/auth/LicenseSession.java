package lauresprojects.com.recorder.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public final class LicenseSession {
    private static final String PREFS = "anubis_license_session";
    private static final String KEY_HASH = "key_hash";
    private static final String LICENSE_KEY = "license_key";
    private static final String EXPIRES_AT_MS = "expires_at_ms";
    private static final String EXPIRY_DATE = "expiry_date";
    private static final String EXPIRY_TIME = "expiry_time";
    private static final String DEVICE_HASH = "device_hash";

    private LicenseSession() {
    }

    private static SharedPreferences prefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public static void save(Context context, LicenseRecord record,
                            String deviceHash, String licenseKey) {
        try {
            prefs(context).edit()
                    .putString(KEY_HASH, record.keyHash)
                    .putString(LICENSE_KEY, licenseKey)
                    .putLong(EXPIRES_AT_MS, record.expiresAtMs)
                    .putString(EXPIRY_DATE, record.expiryDate)
                    .putString(EXPIRY_TIME, record.expiryTime)
                    .putString(DEVICE_HASH, deviceHash)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public static LicenseRecord load(Context context, String expectedDeviceHash) {
        try {
            SharedPreferences p = prefs(context);
            String keyHash = p.getString(KEY_HASH, null);
            if (keyHash == null || keyHash.isEmpty()) {
                return null;
            }
            String savedDevice = p.getString(DEVICE_HASH, null);
            if (!expectedDeviceHash.equals(savedDevice)) {
                clear(context);
                return null;
            }
            long expiresAtMs = p.getLong(EXPIRES_AT_MS, 0L);
            if (expiresAtMs > 0L && System.currentTimeMillis() >= expiresAtMs) {
                clear(context);
                return null;
            }
            return new LicenseRecord(
                    keyHash,
                    p.getString(EXPIRY_DATE, ""),
                    p.getString(EXPIRY_TIME, ""),
                    expiresAtMs,
                    true,
                    0);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getLicenseKey(Context context) {
        try {
            return prefs(context).getString(LICENSE_KEY, "");
        } catch (Exception ignored) {
            return "";
        }
    }

    public static void clear(Context context) {
        try {
            prefs(context).edit().clear().apply();
        } catch (Exception ignored) {
        }
    }
}
