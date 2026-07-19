package com.anubis.loader.gms;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.anubis.loader.fake.frameworks.BAccountManager;

import java.util.Locale;

/**
 * Full-screen Google sign-in for microG inside Anubis.
 */
public class MicroGLoginBridgeActivity extends Activity {
    private static final String TAG = "MicroGLoginBridge";
    private static final String EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup";
    private static final String PROGRAMMATIC_AUTH_URL = "https://accounts.google.com/o/oauth2/programmatic_auth";
    private static final String GOOGLE_SUITE_URL = "https://accounts.google.com/signin/continue";
    private static final String COOKIE_OAUTH_TOKEN = "oauth_token";

    private WebView mWebView;
    private ProgressBar mProgress;
    private AccountAuthenticatorResponse mAuthResponse;
    private boolean mFinished;
    private boolean mSawCompletionUrl;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mCookiePolls;

    @SuppressLint({"SetJavaScriptEnabled", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent() != null ? getIntent().getExtras() : null;
        if (extras != null) {
            Object resp = extras.get(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (resp instanceof AccountAuthenticatorResponse) {
                mAuthResponse = (AccountAuthenticatorResponse) resp;
            }
        }

        FrameLayout root = new FrameLayout(this);
        mProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        root.addView(mProgress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER));

        mWebView = new WebView(this);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cm.setAcceptThirdPartyCookies(mWebView, true);
        }

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgress.setVisibility(newProgress >= 100 ? android.view.View.GONE : android.view.View.VISIBLE);
            }
        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "pageFinished: " + url);
                if (url == null) return;
                if (shouldComplete(url)) {
                    scheduleCookieCheck();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverride: " + url);
                if (url != null && shouldComplete(url)) {
                    scheduleCookieCheck();
                    return url.startsWith(GOOGLE_SUITE_URL);
                }
                return false;
            }
        });

        root.addView(mWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Stale oauth_token from a previous account makes the WebView finish immediately.
        clearGoogleSession();
        mWebView.loadUrl(buildSetupUrl());
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        cancel();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    private boolean shouldComplete(String url) {
        if (url == null) return false;
        if (url.startsWith(GOOGLE_SUITE_URL)) {
            mSawCompletionUrl = true;
            return true;
        }
        if (url.startsWith(PROGRAMMATIC_AUTH_URL)) {
            mSawCompletionUrl = true;
            return true;
        }
        if (url.contains("/o/android/auth")) {
            mSawCompletionUrl = true;
            return true;
        }
        Uri uri = Uri.parse(url);
        String fragment = uri.getFragment();
        if (fragment != null && fragment.contains("close")) {
            mSawCompletionUrl = true;
            return true;
        }
        return false;
    }

    /** Drop prior Google session so "add another account" shows the email field. */
    private void clearGoogleSession() {
        try {
            CookieManager cm = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cm.removeAllCookies(value -> {
                    cm.flush();
                    Log.d(TAG, "Google cookies cleared for new account flow");
                });
            } else {
                cm.removeAllCookie();
            }
            if (mWebView != null) {
                mWebView.clearCache(true);
                mWebView.clearHistory();
            }
        } catch (Throwable t) {
            Log.w(TAG, "clearGoogleSession", t);
        }
    }

    private void scheduleCookieCheck() {
        if (mFinished || !mSawCompletionUrl) return;
        mHandler.removeCallbacks(mCookieRunnable);
        mHandler.postDelayed(mCookieRunnable, 400);
    }

    private final Runnable mCookieRunnable = new Runnable() {
        @Override
        public void run() {
            if (mFinished) return;
            if (tryExtractOAuthToken()) return;
            if (++mCookiePolls < 40) {
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    private boolean tryExtractOAuthToken() {
        if (mFinished) return true;
        String[] bases = {
                EMBEDDED_SETUP_URL,
                "https://accounts.google.com",
                "https://google.com",
                PROGRAMMATIC_AUTH_URL
        };
        for (String base : bases) {
            String token = findOAuthToken(CookieManager.getInstance().getCookie(base));
            if (token != null) {
                Log.i(TAG, "Found oauth_token from " + base);
                mFinished = true;
                mHandler.removeCallbacks(mCookieRunnable);
                MicroGAuthDelegate.exchangeOAuthToken(MicroGLoginBridgeActivity.this, token, mAuthResponse);
                return true;
            }
        }
        return false;
    }

    private static String findOAuthToken(String cookies) {
        if (cookies == null || cookies.isEmpty()) return null;
        for (String part : cookies.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(COOKIE_OAUTH_TOKEN + "=")) {
                String token = trimmed.substring(COOKIE_OAUTH_TOKEN.length() + 1);
                return token.isEmpty() ? null : token;
            }
        }
        return null;
    }

    private String buildSetupUrl() {
        Locale locale = Locale.getDefault();
        Uri.Builder builder = Uri.parse(EMBEDDED_SETUP_URL).buildUpon()
                .appendQueryParameter("source", "android")
                .appendQueryParameter("xoauth_display_name", "Android Device")
                .appendQueryParameter("lang", locale.getLanguage())
                .appendQueryParameter("cc", locale.getCountry().toLowerCase(Locale.US))
                .appendQueryParameter("langCountry", locale.toString().toLowerCase(Locale.US))
                .appendQueryParameter("hl", locale.toString().replace("_", "-"))
                .appendQueryParameter("tmpl", "new_account");
        if (hasExistingGoogleAccount()) {
            builder.appendQueryParameter("ss_mode", "1");
        }
        return builder.build().toString();
    }

    private static boolean hasExistingGoogleAccount() {
        try {
            Account[] accts = BAccountManager.get().getAccountsAsUser(GOOGLE_TYPE);
            return accts != null && accts.length > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final String GOOGLE_TYPE = "com.google";

    private void cancel() {
        if (mFinished) return;
        mFinished = true;
        if (mAuthResponse != null) {
            mAuthResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
        }
        finish();
    }
}
