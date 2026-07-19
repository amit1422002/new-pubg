package com.anubis.loader.gms;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.utils.Slog;

/** Redirect microG {@code LoginActivity} to Anubis bridge that skips the broken CookieManager gate. */
public final class MicroGLoginRedirect {
    private static final String TAG = "MicroGLoginRedirect";
    private static final String MICROG_LOGIN = "org.microg.gms.auth.login.LoginActivity";

    private MicroGLoginRedirect() {}

    public static boolean redirectIfNeeded(Intent intent, ActivityInfo info) {
        if (!redirectIfNeeded(intent)) return false;
        if (info != null) {
            info.name = MicroGLoginBridgeActivity.class.getName();
            info.packageName = AnubisCore.getHostPkg();
            if (info.applicationInfo != null) {
                info.applicationInfo.packageName = AnubisCore.getHostPkg();
            }
        }
        return true;
    }

    public static boolean redirectIfNeeded(Intent intent) {
        if (intent == null) return false;
        ComponentName cn = intent.getComponent();
        if (cn == null) return false;
        if (!MICROG_LOGIN.equals(cn.getClassName())) return false;
        Slog.d(TAG, "Redirect " + MICROG_LOGIN + " -> MicroGLoginBridgeActivity");
        intent.setComponent(new ComponentName(
                AnubisCore.getHostPkg(),
                MicroGLoginBridgeActivity.class.getName()));
        return true;
    }
}
