package com.anubis.loader.utils;

/** DFM stealth — Java PM / Context / ApplicationInfo path spoof (T5). */
public final class DfmStealthTier {

    public static boolean T5_JAVA_PM_SPOOF = true;

    private DfmStealthTier() {
    }

    public static boolean isTarget(String packageName) {
        return VirtualPathSpoof.isStealthAcPackage(packageName);
    }

    public static boolean javaPmSpoof(String packageName) {
        return T5_JAVA_PM_SPOOF && isTarget(packageName);
    }

    public static boolean anyBindTier(String packageName) {
        return javaPmSpoof(packageName);
    }
}
