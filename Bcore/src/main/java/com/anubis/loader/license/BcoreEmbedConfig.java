package com.anubis.loader.license;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Embedded license for the BCore AAR. Edit these constants when you publish a build.
 * <ul>
 *   <li>{@link #EMBEDDED_ACTIVATION_KEY} — host app's {@link com.anubis.loader.app.configuration.ClientConfiguration#getLicenseActivationKey()}
 *       must return the exact same value (trimmed).</li>
 *   <li>{@link #LICENSE_EXPIRES_ON_YYYYMMDD} — UTC calendar date; valid through the end of that day (23:59:59.999 UTC).</li>
 * </ul>
 * If {@link #EMBEDDED_ACTIVATION_KEY} is empty, the gate is turned off (for local/debug builds).
 */
public final class BcoreEmbedConfig {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** Default loader package when config/context is spoofed in the guest process. */
    public static final String DEFAULT_LOADER_PACKAGE = "com.anubis";

    private BcoreEmbedConfig() {
    }

    /**
     * Non-empty → server-style check is enforced. Empty string → skip activation check (still honour expiry if set).
     */
    public static final String EMBEDDED_ACTIVATION_KEY = "";

    /**
     * Expiry as {@code YYYYMMDD} in UTC (e.g. {@code 20290601} = 1 June 2029). License is valid for the whole of that day UTC.
     * Use {@code 0} to disable the date limit (expiry check never trips on date).
     */
    public static final int LICENSE_EXPIRES_ON_YYYYMMDD = 20290601;

    /** When {@code false} and {@link #EMBEDDED_ACTIVATION_KEY} is empty, expiry is skipped as well (fully open gate). */
    public static final boolean ENFORCE_EXPIRY_WHEN_KEY_EMPTY = true;

    /**
     * First millisecond when the license is invalid: start of UTC day after {@link #LICENSE_EXPIRES_ON_YYYYMMDD}.
     * Returns {@link Long#MAX_VALUE} when {@link #LICENSE_EXPIRES_ON_YYYYMMDD} is {@code <= 0} (no date cap).
     */
    public static long licenseExpiresExclusiveStartMsUtc() {
        int ymd = LICENSE_EXPIRES_ON_YYYYMMDD;
        if (ymd <= 0) {
            return Long.MAX_VALUE;
        }
        int year = ymd / 10000;
        int month = (ymd / 100) % 100;
        int day = ymd % 100;
        Calendar cal = Calendar.getInstance(UTC);
        cal.setLenient(false);
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return cal.getTimeInMillis();
    }
}
