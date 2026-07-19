package com.anubis.loader.gms;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import com.anubis.loader.core.GmsCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.system.accounts.BAccountManagerService;
import com.anubis.loader.fake.service.GmsProxy;
import com.anubis.loader.utils.FileUtils;
import com.anubis.loader.utils.Slog;

/**
 * Seeds microG settings after install so login does not hang on device check-in.
 */
public final class MicroGBootstrap {
    private static final String TAG = "MicroGBootstrap";
    private static final String INITIAL_DIGEST = "1-929a0dca0eee55513280171a8585da7dcd3700f8";

    private MicroGBootstrap() {}

    public static void afterInstall(int userId) {
        try {
            GmsProxy.disableForMicroG();
            seedPreferences(userId);
            BAccountManagerService.get().loadAuthenticatorCache(null);
            Slog.d(TAG, "microG bootstrap OK for user " + userId);
        } catch (Throwable t) {
            Slog.w(TAG, "microG bootstrap failed", t);
        }
    }

    private static void seedPreferences(int userId) {
        File prefsDir = new File(BEnvironment.getDataDir(GmsCore.GMS_PKG, userId), "shared_prefs");
        FileUtils.mkdirs(prefsDir);

        long androidId = MicroGCompat.getHostAndroidIdLong();
        long now = System.currentTimeMillis();

        writeUtf8(new File(prefsDir, "checkin.xml"),
                "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                        + "<map>\n"
                        + "    <long name=\"androidId\" value=\"" + androidId + "\" />\n"
                        + "    <string name=\"digest\">" + INITIAL_DIGEST + "</string>\n"
                        + "    <long name=\"lastCheckin\" value=\"" + now + "\" />\n"
                        + "    <long name=\"securityToken\" value=\"0\" />\n"
                        + "    <string name=\"versionInfo\"></string>\n"
                        + "    <string name=\"deviceDataVersionInfo\"></string>\n"
                        + "</map>\n");

        writeUtf8(new File(prefsDir, GmsCore.GMS_PKG + "_preferences.xml"),
                "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                        + "<map>\n"
                        + "    <boolean name=\"checkin_enable_service\" value=\"true\" />\n"
                        + "    <boolean name=\"auth_manager_visible\" value=\"true\" />\n"
                        + "    <boolean name=\"gcm_enable_mcs_service\" value=\"true\" />\n"
                        + "    <boolean name=\"vending_licensing\" value=\"true\" />\n"
                        + "    <boolean name=\"vending_billing\" value=\"true\" />\n"
                        + "    <boolean name=\"vending_apps_install\" value=\"true\" />\n"
                        + "</map>\n");
    }

    private static void writeUtf8(File file, String content) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to write " + file.getName(), t);
        }
    }
}
