package com.anubis.loader.gms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.fake.frameworks.BAccountManager;

import java.util.UUID;

/**
 * GoogleSignInActivity ΓÇö plain Activity (NOT AccountAuthenticatorActivity).
 *
 * Plain Activity so it never sends a result to the wrong AccountAuthenticator channel.
 * On success: stores account in BAccountManager, broadcasts ACTION_GSI_TOKEN.
 *
 * Strategies:
 *  1. Host Bridge: silently acquire from host GMS (no web flow).
 *  2. WebView: zcore's exact credentials + Firebase redirect_uri + onPageFinished hash extraction.
 */
@SuppressLint("MissingPermission")
public class GoogleSignInActivity extends Activity {

    private static final String TAG = "GoogleSignInActivity";
    private static final String GOOGLE_TYPE = "com.google";
    private static final String DEFAULT_SCOPES = "openid email profile";

    private WebView mWebView;
    private ProgressDialog mProgress;
    private boolean mDone = false;
    private String mTokenType;
    private String mCanonicalType;
    private final Handler mMain = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        mTokenType     = getIntent() != null ? getIntent().getStringExtra(GoogleSignInHelper.EXTRA_AUTH_TOKEN_TYPE) : null;
        mCanonicalType = GoogleSignInHelper.canonicaliseTokenType(mTokenType);

        boolean addAccount = isAddAccountFlow(getIntent());
        if (!addAccount && tryFromCache()) return;
        if (GmsAccountManagerProxy.useMicroGAuth()) {
            Log.i(TAG, "microG installed — redirect to MicroGLoginBridgeActivity");
            Intent bridge = new Intent();
            bridge.setComponent(new ComponentName(
                    AnubisCore.getHostPkg(),
                    MicroGLoginBridgeActivity.class.getName()));
            if (getIntent() != null && getIntent().getExtras() != null) {
                bridge.putExtras(getIntent().getExtras());
            }
            bridge.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(bridge);
            finish();
            return;
        }
        startWeb();
    }

    @Override public void onDestroy() { dismissProgress(); destroyWebView(); super.onDestroy(); }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) { mWebView.goBack(); return; }
        broadcast(null, null); finish(); super.onBackPressed();
    }

    // ΓöÇΓöÇ Host bridge ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    private void doHostBridge() {
        Context hostCtx = AnubisCore.getContext();
        if (hostCtx == null) hostCtx = getApplicationContext();
        Account[] accts;
        try { accts = AccountManager.get(hostCtx).getAccountsByType(GOOGLE_TYPE); }
        catch (Throwable t) { startWeb(); return; }
        if (accts == null || accts.length == 0) { startWeb(); return; }
        if (accts.length == 1) { bridgeAccount(accts[0]); return; }
        String[] names = new String[accts.length];
        for (int i=0;i<accts.length;i++) names[i]=accts[i].name;
        Account[] arr = accts;
        new AlertDialog.Builder(this)
            .setTitle("Choose Google account").setItems(names,(d,w)->bridgeAccount(arr[w]))
            .setNegativeButton("Use a different account",(d,w)->startWeb())
            .setOnCancelListener(d->{broadcast(null,null);finish();}).show();
    }

    private void bridgeAccount(Account acct) {
        showProgress("Signing in\u2026");
        Context hostCtx = AnubisCore.getContext();
        if (hostCtx == null) hostCtx = getApplicationContext();
        final Context host = hostCtx;
        new Thread(()->{
            String tok=null;
            String[] scopes={
                    HostAccountBridge.SCOPE_GOOGLEPLAY,
                    "oauth2:openid email profile",
                    "oauth2:https://www.googleapis.com/auth/userinfo.email"
            };
            try {
                AccountManager am=AccountManager.get(host);
                for (String scope:scopes) {
                    try {
                        AccountManagerFuture<Bundle> f=am.getAuthToken(acct,scope,null,false,null,new Handler(Looper.getMainLooper()));
                        Bundle r=f.getResult(); if(r==null) continue;
                        if (r.getParcelable(AccountManager.KEY_INTENT)!=null){mMain.post(()->{dismissProgress();startWeb();});return;}
                        String t=r.getString(AccountManager.KEY_AUTHTOKEN); if(!TextUtils.isEmpty(t)){tok=t;break;}
                    } catch (Throwable t){Log.d(TAG,"scope "+scope+": "+t.getMessage());}
                }
            } catch (Throwable t){Log.w(TAG,"bridge",t);}
            final String ft=tok;
            mMain.post(()->{
                dismissProgress();
                if (!TextUtils.isEmpty(ft)) {
                    boolean jwt = ft.startsWith("eyJ");
                    storeAndBroadcast(acct, jwt ? ft : null, jwt ? null : ft);
                } else startWeb();
            });
        },"gms-bridge").start();
    }

    // ΓöÇΓöÇ WebView (exact zcore cg.java approach) ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    private void startWeb() {
        String nonce=UUID.randomUUID().toString().replace("-","");
        String url="https://accounts.google.com/o/oauth2/v2/auth"
            +"?client_id="+Uri.encode(GoogleSignInHelper.GSI_CLIENT_ID)
            +"&redirect_uri="+Uri.encode(GoogleSignInHelper.GSI_REDIRECT_URI)
            +"&response_type="+Uri.encode("id_token")
            +"&scope="+Uri.encode(DEFAULT_SCOPES)
            +"&prompt="+Uri.encode("select_account")
            +"&nonce="+nonce;
        mWebView=new WebView(this);
        setContentView(mWebView);
        configureWebView(mWebView);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new ZcoreWebViewClient());
        mWebView.addJavascriptInterface(new TokenBridgeJS(),"TokenBridge");
        mWebView.loadUrl(url);
    }

    /** Exact replica of zcore's p10 + ag.java: onPageFinished ΓåÆ eval JS ΓåÆ hash ΓåÆ token. */
    private class ZcoreWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView v, String url) {
            if (url!=null&&url.startsWith(GoogleSignInHelper.GSI_REDIRECT_URI)&&hasToken(url)){onHash(url);return true;}
            return false;
        }
        @Override
        public void onPageFinished(WebView v, String url) {
            super.onPageFinished(v,url);
            if (TextUtils.isEmpty(url)) return;
            // Mirrors zcore: str.startsWith(cgVar.Chutkedhakkan) ΓåÆ evaluate JS ΓåÆ ag.onReceiveValue
            if (url.startsWith(GoogleSignInHelper.GSI_REDIRECT_URI)) {
                v.evaluateJavascript("(function(){return window.location.hash;})()", val->{
                    if (val!=null&&hasToken(val)) onHash(val);
                    else v.evaluateJavascript("(function(){return window.location.href;})()", href->{
                        if (href!=null&&hasToken(href)) onHash(href);
                    });
                });
            }
        }
    }

    private class TokenBridgeJS {
        @JavascriptInterface
        public void onHash(final String h) {
            if (!TextUtils.isEmpty(h)&&hasToken(h)) mMain.post(()->GoogleSignInActivity.this.onHash(h));
        }
    }

    // ΓöÇΓöÇ Token processing ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    private boolean hasToken(String url){return !TextUtils.isEmpty(url)&&(url.contains("id_token=")||url.contains("access_token="));}

    private void onHash(String raw) {
        if (mDone||!hasToken(raw)) return;
        mDone=true;
        String idTok=param(raw,"id_token");
        String accTok=param(raw,"access_token");
        if (TextUtils.isEmpty(idTok)) idTok=GoogleSignInHelper.getCachedIdToken(getApplicationContext());
        if (TextUtils.isEmpty(idTok)&&TextUtils.isEmpty(accTok)){broadcast(null,null);finish();return;}
        if (!TextUtils.isEmpty(idTok)) GoogleSignInHelper.saveIdToken(getApplicationContext(),idTok);
        String name=GoogleSignInHelper.getEmail(getApplicationContext());
        if (TextUtils.isEmpty(name)){fetchEmail(idTok,accTok);return;}
        storeAndBroadcast(new Account(name,GOOGLE_TYPE),idTok,accTok);
    }

    private static boolean isAddAccountFlow(Intent intent) {
        if (intent == null) return false;
        Bundle extras = intent.getExtras();
        return extras != null
                && extras.get(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE) != null;
    }

    private boolean tryFromCache() {
        String cached=GoogleSignInHelper.getCachedIdToken(getApplicationContext());
        if (TextUtils.isEmpty(cached)) return false;
        String name=GoogleSignInHelper.getEmail(getApplicationContext());
        if (TextUtils.isEmpty(name)) return false;
        mDone=true;
        storeAndBroadcast(new Account(name,GOOGLE_TYPE),cached,null);
        return true;
    }

    private void fetchEmail(String idTok, String accTok) {
        String bearer=GoogleSignInHelper.firstNonEmpty(accTok,idTok);
        if (TextUtils.isEmpty(bearer)){broadcast(null,null);finish();return;}
        new Thread(()->{
            String email=null;
            try {
                java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL("https://www.googleapis.com/oauth2/v3/userinfo").openConnection();
                c.setRequestProperty("Authorization","Bearer "+bearer);
                c.setConnectTimeout(5000);c.setReadTimeout(5000);
                if(c.getResponseCode()==200){byte[] b=new byte[4096];int n=c.getInputStream().read(b);c.disconnect();
                    if(n>0){org.json.JSONObject j=new org.json.JSONObject(new String(b,0,n,java.nio.charset.StandardCharsets.UTF_8));
                        email=j.optString("email",null);if(TextUtils.isEmpty(email))email=j.optString("sub",null);}}
            }catch(Throwable t){Log.w(TAG,"userinfo",t);}
            final String e=email;
            mMain.post(()->{if(!TextUtils.isEmpty(e))storeAndBroadcast(new Account(e,GOOGLE_TYPE),idTok,accTok);else{broadcast(null,null);finish();}});
        },"gms-userinfo").start();
    }

    // ΓöÇΓöÇ Store + broadcast (key: plain Activity, no AccountAuthenticatorActivity) ΓöÇΓöÇ

    private void storeAndBroadcast(Account acct, String idTok, String accTok) {
        try{BAccountManager.get().addAccountExplicitly(acct,null,null);}catch(Throwable ignored){}
        String eff=GoogleSignInHelper.firstNonEmpty(accTok,idTok);
        if(!TextUtils.isEmpty(idTok))  setA(acct,"id_token",idTok);
        if(!TextUtils.isEmpty(accTok)) setA(acct,"oauth2",accTok);
        setA(acct,"oauth2:openid email profile",eff);
        setA(acct,"oauth2:https://www.googleapis.com/auth/userinfo.email",eff);
        if (!TextUtils.isEmpty(accTok)) {
            setA(acct, HostAccountBridge.SCOPE_GOOGLEPLAY, accTok);
        }
        if(!TextUtils.isEmpty(mTokenType))    setA(acct,mTokenType,GoogleSignInHelper.firstNonEmpty(accTok,idTok));
        if(!TextUtils.isEmpty(mCanonicalType)) setA(acct,mCanonicalType,GoogleSignInHelper.firstNonEmpty(accTok,idTok));
        writeUD(acct,acct.name);
        PlayStoreAuthHelper.ensureGmsVisibility(acct);
        PlayStoreAuthHelper.bridgePlayTokenSync(acct);
        broadcast(idTok,accTok);
        moveHostToFront();
        finish();
    }

    private void broadcast(String idTok, String accTok) {
        try{Intent i=new Intent(GoogleSignInHelper.ACTION_GSI_TOKEN);
            i.putExtra("id_token",idTok);i.putExtra("access_token",accTok);
            i.putExtra("auth_token_type",mTokenType);i.setPackage(getPackageName());
            sendBroadcast(i);}catch(Throwable ignored){}
    }

    // ΓöÇΓöÇ Helpers ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    private String param(String url, String name) {
        if (TextUtils.isEmpty(name)||TextUtils.isEmpty(url)) return null;
        url=url.replaceAll("^\"|\"$","").replace("\\u003D","=");
        if (url.startsWith("#")) url=url.substring(1);
        String pfx=name+"=";
        for(String p:url.split("[&?#]")) if(p.startsWith(pfx)&&p.length()>pfx.length()){
            String v=p.substring(pfx.length()).replaceAll("\"$","");
            try{return Uri.decode(v);}catch(Throwable t){return v;}
        }
        return null;
    }

    private void writeUD(Account a, String email) {
        Context c=getApplicationContext();
        String gaia=GoogleSignInHelper.getGaiaId(c);
        if(TextUtils.isEmpty(email)) email=a.name;
        setUD(a,"email",email);setUD(a,"account_name",email);
        setUD(a,"name",GoogleSignInHelper.getDisplayName(c));setUD(a,"display_name",GoogleSignInHelper.getDisplayName(c));
        setUD(a,"given_name",GoogleSignInHelper.getGivenName(c));setUD(a,"family_name",GoogleSignInHelper.getFamilyName(c));
        setUD(a,"gaia_id",gaia);setUD(a,"gaia",gaia);setUD(a,"gaiaId",gaia);setUD(a,"GaiaId",gaia);
        setUD(a,"obfuscatedGaiaId",gaia);setUD(a,"account_id",gaia);setUD(a,"accountId",gaia);
        setUD(a,"user_id",gaia);setUD(a,"userId",gaia);setUD(a,"google_user_id",gaia);setUD(a,"googleUserId",gaia);
    }

    private void setA(Account a,String k,String v){if(!TextUtils.isEmpty(k)&&!TextUtils.isEmpty(v))try{BAccountManager.get().setAuthToken(a,k,v);}catch(Throwable ignored){}}
    private void setUD(Account a,String k,String v){if(!TextUtils.isEmpty(k)&&!TextUtils.isEmpty(v))try{BAccountManager.get().setUserData(a,k,v);}catch(Throwable ignored){}}

    private void configureWebView(WebView wv) {
        WebSettings ws=wv.getSettings();
        ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setDatabaseEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        String ua=ws.getUserAgentString();
        if(!TextUtils.isEmpty(ua)) ws.setUserAgentString(ua.replace("; wv",""));
        CookieManager cm=CookieManager.getInstance();cm.setAcceptCookie(true);cm.setAcceptThirdPartyCookies(wv,true);
    }

    private void showProgress(String msg){mMain.post(()->{if(!isFinishing())mProgress=ProgressDialog.show(this,null,msg,true,false);});}
    private void dismissProgress(){mMain.post(()->{if(mProgress!=null){try{mProgress.dismiss();}catch(Throwable ignored){}mProgress=null;}});}
    private void destroyWebView(){if(mWebView!=null){try{mWebView.stopLoading();mWebView.removeJavascriptInterface("TokenBridge");mWebView.destroy();}catch(Throwable ignored){}mWebView=null;}}
    private void moveHostToFront(){try{android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);if(am==null)return;int tid=getTaskId();for(android.app.ActivityManager.AppTask t:am.getAppTasks()){android.app.ActivityManager.RecentTaskInfo i=t.getTaskInfo();if(i!=null&&i.id!=tid){t.moveToFront();return;}}}catch(Throwable ignored){}}
}
