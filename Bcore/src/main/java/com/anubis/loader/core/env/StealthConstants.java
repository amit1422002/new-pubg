package com.anubis.loader.core.env;

import org.lsposed.lsparanoid.Obfuscate;

/**
 * Neutral names for internal artifacts visible in /proc maps and filesystem probes.
 */
@Obfuscate
public final class StealthConstants {

    /** Renamed from HASAD — appears as {@code libartpalette.so} in maps. */
    public static final String NATIVE_CORE_LIB = "artpalette";

    private StealthConstants() {
    }
}
