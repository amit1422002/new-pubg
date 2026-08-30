package com.anubis.skin;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import android.webkit.WebView;

import com.elite.app.configuration.AppLifecycleCallback;

/**
 * Isolates WebView data dirs per virtual guest process.
 * Fixes Twitter/Facebook Web login crash:
 * "Using WebView from more than one process at once with the same data directory".
 */
public final class VirtualWebViewFix extends AppLifecycleCallback {

    private static final String TAG = "WebViewFix";
    private static boolean sApplied;

    @Override
    public void beforeCreateApplication(String packageName, String processName,
                                        Context context, int userId) {
        apply(packageName, processName, userId);
    }

    @Override
    public void beforeApplicationOnCreate(String packageName, String processName,
                                          Application application, int userId) {
        apply(packageName, processName, userId);
    }

    @Override
    public void afterApplicationOnCreate(String packageName, String processName,
                                         Application application, int userId) {
        apply(packageName, processName, userId);
    }

    @Override
    public void beforeMainActivityOnCreate(android.app.Activity activity) {
        // Last-chance before TwitterWebActivity inflates WebView (same process).
        if (activity == null) {
            return;
        }
        String pkg = activity.getPackageName();
        apply(pkg, pkg, 0);
    }

    private static void apply(String packageName, String processName, int userId) {
        if (sApplied || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        // Never override main-host "host" suffix with a guest package name.
        // Guest runs inside proxy processes where suffix is still unset.
        try {
            String raw = "v" + userId + "_" + packageName + "_p" + Process.myPid();
            if (processName != null && !processName.isEmpty() && !processName.equals(packageName)) {
                raw = raw + "_" + Integer.toHexString(processName.hashCode());
            }
            String suffix = sanitize(raw);
            WebView.setDataDirectorySuffix(suffix);
            sApplied = true;
            Log.i(TAG, "WebView data dir suffix=" + suffix + " pkg=" + packageName);
        } catch (IllegalStateException already) {
            sApplied = true;
            Log.w(TAG, "WebView suffix already set (pid=" + Process.myPid()
                    + " pkg=" + packageName + "): " + already.getMessage());
        } catch (Throwable t) {
            Log.w(TAG, "failed to set WebView data dir suffix", t);
        }
    }

    private static String sanitize(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        if (sb.length() > 64) {
            sb.setLength(64);
        }
        return sb.toString();
    }
}
