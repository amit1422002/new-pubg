package com.anubis.loader.gms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Intent;
import android.text.TextUtils;

import com.anubis.loader.fake.frameworks.BAccountManager;
import com.anubis.loader.utils.Slog;

/**
 * Play Store auth helpers ΓÇö redirect unauthenticated UI and expose accounts to GMS/vending.
 */
public final class PlayStoreAuthHelper {
    private static final String TAG = "PlayStoreAuthHelper";
    private static final String VENDING = "com.android.vending";
    private static final String ASSET_BROWSER = "com.android.vending.AssetBrowserActivity";

    private PlayStoreAuthHelper() {}

    public static boolean isUnauthenticatedActivity(String className) {
        return className != null && className.contains("UnauthenticatedMainActivity");
    }

    /** Rewrite launch/start intents away from the sign-in gate when we already have an account. */
    public static boolean redirectIfAuthenticated(Intent intent) {
        if (intent == null || !GmsAccountManagerProxy.hasGoogleAccount()) return false;
        ComponentName cn = intent.getComponent();
        if (cn == null || !VENDING.equals(cn.getPackageName())) return false;
        if (!isUnauthenticatedActivity(cn.getClassName())) return false;
        Slog.d(TAG, "Redirect " + cn.getClassName() + " -> " + ASSET_BROWSER);
        intent.setComponent(new ComponentName(VENDING, ASSET_BROWSER));
        return true;
    }

    public static void ensureGmsVisibility(Account acct) {
        if (acct == null) return;
        for (String pkg : GoogleSignInHelper.GMS_PACKAGES) {
            try {
                BAccountManager.get().setAccountVisibility(acct, pkg, AccountManager.VISIBILITY_VISIBLE);
            } catch (Throwable ignored) {}
        }
        try {
            BAccountManager.get().setAccountVisibility(acct, VENDING, AccountManager.VISIBILITY_VISIBLE);
        } catch (Throwable ignored) {}
    }

    public static void bridgePlayTokenSync(Account acct) {
        if (acct == null || GmsAccountManagerProxy.useMicroGAuth()) return;
        // microG issues googleplay tokens via its own AccountAuthenticator
    }
}
