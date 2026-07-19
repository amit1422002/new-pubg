package com.anubis.loader.app.configuration;

import java.io.File;

/**
 * Created by Milk on 5/4/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public abstract class ClientConfiguration {

    public boolean isHideRoot() {
        return true;
    }

    public boolean isHideXposed() {
        return true;
    }

    public abstract String getHostPackageName();

    public boolean isEnableDaemonService() {
        return true;
    }

    public boolean isEnableLauncherActivity() {
        return true;
    }

    /**
     * This method is called when an internal application requests to install a new application.
     *
     * @return Is it handled?
     */
    public boolean requestInstallPackage(File file, int userId) {
        return false;
    }

    /**
     * Must match {@link com.anubis.loader.license.BcoreEmbedConfig#EMBEDDED_ACTIVATION_KEY} when that
     * constant is non-empty; otherwise initialization throws via {@link com.anubis.loader.license.BcoreLicenseInstaller}.
     * Default empty is only valid when embedded key is also empty (open gate — not for shipped AAR builds).
     */
    public String getLicenseActivationKey() {
        return "";
    }
}
