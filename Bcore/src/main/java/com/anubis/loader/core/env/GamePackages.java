package com.anubis.loader.core.env;

/**
 * Cloned games that need OBB / expansion staging (same flow as BGMI).
 */
public final class GamePackages {

    public static final String BGMI = "com.pubg.imobile";
    public static final String FARLIGHT = "com.miraclegames.farlight84";
    public static final String FARLIGHT_LEGACY = "com.lilith.farlight84";
    public static final String ARENA_BREAKOUT = "com.proximabeta.mf.uamo";
    public static final String ARENA_BREAKOUT_LITE = "com.proximabeta.mf.liteuamo";
    public static final String DELTA_FORCE = "com.proxima.dfm";
    public static final String DELTA_FORCE_GARENA = "com.garena.game.df";
    public static final String VALORANT_MOBILE = "com.tencent.tmgp.codev";

    private GamePackages() {
    }

    public static boolean isBgmi(String packageName) {
        return BGMI.equals(packageName);
    }

    /** Global / regional PUBG Mobile — own libanogs profile; skip DFM-style path/proc stealth (1-day ban). */
    public static boolean isPubgMobileFamily(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        return BGMI.equals(packageName)
                || "com.tencent.ig".equals(packageName)
                || "com.rekoo.pubgm".equals(packageName)
                || "com.pubg.krmobile".equals(packageName)
                || "com.vng.pubgmobile".equals(packageName);
    }

    public static boolean isFarlight(String packageName) {
        return FARLIGHT.equals(packageName) || FARLIGHT_LEGACY.equals(packageName);
    }

    public static boolean isArenaBreakout(String packageName) {
        return ARENA_BREAKOUT.equals(packageName) || ARENA_BREAKOUT_LITE.equals(packageName);
    }

    public static boolean isDeltaForce(String packageName) {
        return DELTA_FORCE.equals(packageName) || DELTA_FORCE_GARENA.equals(packageName);
    }

    public static boolean isValorantMobile(String packageName) {
        return VALORANT_MOBILE.equals(packageName);
    }

    /** No Android/obb split — install clone and launch directly. */
    public static boolean isDirectLaunchGame(String packageName) {
        return isValorantMobile(packageName);
    }

    /** Farlight / Arena / Delta store obb.png + paks in package data — no Android/obb copy. */
    public static boolean isPackageDataGame(String packageName) {
        return isFarlight(packageName) || isArenaBreakout(packageName) || isDeltaForce(packageName);
    }
}
