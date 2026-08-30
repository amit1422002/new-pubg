package com.anubis.skin;

public final class BgmiSkin {
    public static final String BGMI_PKG = "com.pubg.imobile";

    private BgmiSkin() {
    }

    public static boolean isBgmi(String packageName) {
        return BGMI_PKG.equals(packageName);
    }
}
