package com.elite.utils.auth;

import java.util.HashSet;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public final class Auth {

    public static final HashSet<String> AUTH_PKG_SET = new HashSet<String>();

    static {
        AUTH_PKG_SET.add("com.twitter.android");
        AUTH_PKG_SET.add("com.twitter.android.lite");
        AUTH_PKG_SET.add("com.x.android");
        AUTH_PKG_SET.add("com.discord");
        AUTH_PKG_SET.add("com.discord.canary");
        AUTH_PKG_SET.add("com.discord.ptb");
        AUTH_PKG_SET.add("com.aliucord");
        AUTH_PKG_SET.add("com.facebook.katana");
        AUTH_PKG_SET.add("com.facebook.orca");
        AUTH_PKG_SET.add("com.facebook.lite");
        AUTH_PKG_SET.add("com.facebook.mlite");
    }

}