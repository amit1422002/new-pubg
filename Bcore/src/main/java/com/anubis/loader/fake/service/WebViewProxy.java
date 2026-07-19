package com.anubis.loader.fake.service;

import android.content.Context;
import android.os.Build;
import android.webkit.WebView;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.fake.hook.ClassInvocationStub;
import com.anubis.loader.utils.Slog;

public class WebViewProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewProxy";

    public WebViewProxy() {}

    @Override
    protected Object getWho() {
        try {
            return Class.forName("android.webkit.WebView");
        } catch (Throwable t) {
            Slog.w(TAG, "getWho: WebView class not found", t);
            return "android.webkit.WebView";
        }
    }

    @Override
    protected void inject(Object who, Object origin) {
        try {
            ensureWebViewDataDirectorySuffix();
        } catch (Throwable t) {
            Slog.w(TAG, "inject: ensureWebViewDataDirectorySuffix failed", t);
        }
    }

    @Override
    public boolean isBadEnv() {
        try {
            Context ctx = AnubisCore.get() != null ? AnubisCore.get().getContext() : null;
            if (ctx == null) {
                Slog.w(TAG, "isBadEnv: context is null");
                return true;
            }
            if (Build.VERSION.SDK_INT < 14) {
                Slog.w(TAG, "isBadEnv: SDK too old: " + Build.VERSION.SDK_INT);
                return true;
            }
            return false;
        } catch (Throwable t) {
            Slog.w(TAG, "isBadEnv error", t);
            return true;
        }
    }

    private void ensureWebViewDataDirectorySuffix() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        String packageName = BActivityThread.getAppPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        int bpid = BActivityThread.getAppPid();
        String suffix = BEnvironment.buildWebViewDataDirectorySuffix(packageName, bpid);
        BEnvironment.ensureWebViewDataDirectory(packageName, BActivityThread.getUserId(), suffix);
        try {
            WebView.setDataDirectorySuffix(suffix);
            Slog.d(TAG, "Applied WebView suffix=" + suffix);
        } catch (IllegalStateException alreadySet) {
            Slog.d(TAG, "WebView suffix already set: " + suffix);
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to set WebView suffix", t);
        }
    }
}
