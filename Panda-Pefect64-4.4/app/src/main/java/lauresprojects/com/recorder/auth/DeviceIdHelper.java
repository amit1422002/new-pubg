package lauresprojects.com.recorder.auth;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

public final class DeviceIdHelper {
    private DeviceIdHelper() {
    }

    public static String getDeviceHash(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (TextUtils.isEmpty(androidId)) {
            androidId = "";
        }
        return LicenseHasher.device(androidId, Build.BOARD, Build.BRAND, Build.MODEL);
    }

    public static String getDeviceLabel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }
}
