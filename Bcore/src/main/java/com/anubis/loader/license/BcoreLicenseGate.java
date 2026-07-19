package com.anubis.loader.license;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.anubis.loader.app.configuration.ClientConfiguration;

/**
 * Synchronous embedded key + expiry gate for BCore.
 */
final class BcoreLicenseGate {

    private static volatile boolean sAuthorized;

    static boolean isAuthorized() {
        return sAuthorized;
    }

    static void verifyOrThrow(ClientConfiguration config) {
        String embedded = safe(BcoreEmbedConfig.EMBEDDED_ACTIVATION_KEY).trim();

        boolean gateByKey = !embedded.isEmpty();
        if (gateByKey) {
            String provided = config == null ? "" : safe(config.getLicenseActivationKey()).trim();
            if (!constantTimeEquals(provided, embedded)) {
                throw new IllegalStateException(
                        "BCore activation key missing or incorrect. Override ClientConfiguration#getLicenseActivationKey() to match BcoreEmbedConfig.EMBEDDED_ACTIVATION_KEY.");
            }
        }

        boolean enforceExpiry =
                gateByKey || BcoreEmbedConfig.ENFORCE_EXPIRY_WHEN_KEY_EMPTY;
        long now = System.currentTimeMillis();
        if (enforceExpiry && now >= BcoreEmbedConfig.licenseExpiresExclusiveStartMsUtc()) {
            throw new IllegalStateException(
                    "BCore build has expired (see BcoreEmbedConfig.LICENSE_EXPIRES_ON_YYYYMMDD).");
        }

        sAuthorized = true;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(ab, bb);
    }
}
