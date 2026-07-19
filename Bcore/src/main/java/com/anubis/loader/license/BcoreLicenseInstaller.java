package com.anubis.loader.license;

import android.content.Context;

import androidx.annotation.Nullable;

import com.anubis.loader.app.configuration.ClientConfiguration;

/**
 * Validates the embedded secret and expiry from {@code AnubisCore.doAttachBaseContext}.
 */
public final class BcoreLicenseInstaller {

    private BcoreLicenseInstaller() {
    }

    /**
     * Validates {@link ClientConfiguration#getLicenseActivationKey()} against {@link BcoreEmbedConfig}.
     *
     * @throws IllegalStateException if key or expiry checks fail when enabled
     */
    public static void verifyOrThrow(ClientConfiguration configuration) throws IllegalStateException {
        BcoreLicenseGate.verifyOrThrow(configuration);
    }

    /**
     * @deprecated Prefer {@link #verifyOrThrow(ClientConfiguration)} — context is unused.
     */
    @Deprecated
    public static void install(@Nullable Context ignored, ClientConfiguration configuration) {
        verifyOrThrow(configuration);
    }

    /** @return Whether {@link #verifyOrThrow(ClientConfiguration)} succeeded in this VM. */
    public static boolean isAuthorized() {
        return BcoreLicenseGate.isAuthorized();
    }
}
