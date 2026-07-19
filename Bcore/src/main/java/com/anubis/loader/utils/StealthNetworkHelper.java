package com.anubis.loader.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

/**
 * Stealth guests must not route through loader VPN (192.0.0.4) — breaks version fetch / AC checks.
 */
public final class StealthNetworkHelper {

    private StealthNetworkHelper() {
    }

    public static void ensureRealNetwork(Context context) {
        if (context == null) {
            return;
        }
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.bindProcessToNetwork(null);
            } else {
                ConnectivityManager.setProcessDefaultNetwork(null);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Network[] networks = cm.getAllNetworks();
                if (networks != null) {
                    for (Network network : networks) {
                        try {
                            cm.reportNetworkConnectivity(network, true);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
