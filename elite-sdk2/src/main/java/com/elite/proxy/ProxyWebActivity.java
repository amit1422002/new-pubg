package com.elite.proxy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.HashMap;
import java.util.Map;
import com.elite.R;

public class ProxyWebActivity extends Activity {

    public static final String TAG = "ProxyWebActivity";

    private WebView mWebView;

    private static final Map<String, String> TOKEN_MAP = new HashMap<>();

    public static final String FACEBOOK = "facebook";
    public static final String GOOGLE   = "google";
    public static final String WECHAT   = "wechat";
    public static final String QQ       = "qq";

    private static void setToken(String key, String value) {
        if (key != null && value != null) {
            TOKEN_MAP.put(key, value);
        }
    }

    public static String getToken(String key) {
        return TOKEN_MAP.get(key);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        mWebView = findViewById(R.id.webView);
        initWebView(mWebView);

        String url = getIntent().getStringExtra("url");
        if (url != null) {
            mWebView.loadUrl(url);
        }
    }

    private void initWebView(WebView webView) {

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "Redirect: " + url);
                if (url.contains("facebook.com")) {
                    setToken(FACEBOOK, url);
                    Log.i(TAG, "Facebook login detected");
                }
                else if (url.contains("google.com")) {
                    setToken(GOOGLE, url);
                    Log.i(TAG, "Google login detected");
                }
                else if (url.contains("wechat.com")) {
                    setToken(WECHAT, url);
                    Log.i(TAG, "WeChat login detected");
                }
                else if (url.contains("qq.com")) {
                    setToken(QQ, url);
                    Log.i(TAG, "QQ login detected");
                }

                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
        }
        super.onDestroy();
    }
}