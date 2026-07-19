package com.anubis.loader.gms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.anubis.loader.fake.frameworks.BAccountManager;

@SuppressLint("MissingPermission")
public class HostAccountBridge {
    private static final String TAG = "HostAccountBridge";
    private static final String GOOGLE_TYPE = "com.google";
    public static final String SCOPE_GOOGLEPLAY = "oauth2:https://www.googleapis.com/auth/googleplay";
    private static volatile boolean sBridged = false;

    private static final String[] BRIDGE_SCOPES = {
            SCOPE_GOOGLEPLAY,
            "oauth2:openid email profile",
            "oauth2:https://www.googleapis.com/auth/userinfo.email"
    };

    public static boolean hostHasGoogleAccounts(Context ctx) {
        ctx = hostCtx(ctx);
        if (ctx==null) return false;
        try { Account[] a=AccountManager.get(ctx).getAccountsByType(GOOGLE_TYPE); return a!=null&&a.length>0; }
        catch (Throwable t) { return false; }
    }

    public static Account getFirstHostAccount(Context ctx) {
        ctx = hostCtx(ctx);
        if (ctx==null) return null;
        try { Account[] a=AccountManager.get(ctx).getAccountsByType(GOOGLE_TYPE); return a!=null&&a.length>0?a[0]:null; }
        catch (Throwable t) { return null; }
    }

    public static void triggerBridgeAsync(Context ctx) {
        if (sBridged) return; sBridged=true;
        new Thread(()->{try{bridge(ctx);}catch(Throwable t){Log.w(TAG,"async",t);}},"gms-bridge").start();
    }

    /** Host app context ΓÇö virtual AccountManager must not be used for real GMS tokens. */
    private static Context hostCtx(Context ctx) {
        try {
            Context h = com.anubis.loader.AnubisCore.getContext();
            if (h != null) return h;
        } catch (Throwable ignored) {}
        return ctx;
    }

    public static boolean bridge(Context ctx) {
        ctx = hostCtx(ctx);
        if (ctx==null) return false;
        try {
            Account[] accts=AccountManager.get(ctx).getAccountsByType(GOOGLE_TYPE);
            if (accts==null||accts.length==0) return false;
            boolean any=false;
            for (Account a:accts) if (bridgeSingle(ctx,a)) any=true;
            return any;
        } catch (Throwable t) { Log.w(TAG,"bridge",t); return false; }
    }

    /** Fetch a real OAuth token from the host device account and store it in the virtual AM. */
    public static String fetchHostToken(Context ctx, Account virtualAcct, String tokenType) {
        ctx = hostCtx(ctx);
        if (ctx == null) return null;
        Account hostAcct = matchHostAccount(ctx, virtualAcct);
        if (hostAcct == null) return null;
        try { BAccountManager.get().addAccountExplicitly(virtualAcct != null ? virtualAcct : hostAcct, null, null); }
        catch (Throwable ignored) {}
        Account storeAcct = virtualAcct != null ? virtualAcct : hostAcct;
        String[] scopes = scopesFor(tokenType);
        for (String scope : scopes) {
            String tok = requestHostToken(ctx, hostAcct, scope);
            if (!TextUtils.isEmpty(tok)) {
                storeAll(ctx, storeAcct, tok, scope);
                if (!TextUtils.isEmpty(tokenType) && !tokenType.equals(scope)) {
                    setAuth(storeAcct, tokenType, tok);
                }
                return tok;
            }
        }
        return null;
    }

    private static String[] scopesFor(String tokenType) {
        if (!TextUtils.isEmpty(tokenType)) {
            return new String[]{tokenType, SCOPE_GOOGLEPLAY, "oauth2:openid email profile",
                    "oauth2:https://www.googleapis.com/auth/userinfo.email"};
        }
        return BRIDGE_SCOPES;
    }

    private static Account matchHostAccount(Context ctx, Account virtualAcct) {
        try {
            Account[] accts = AccountManager.get(ctx).getAccountsByType(GOOGLE_TYPE);
            if (accts == null || accts.length == 0) return null;
            if (virtualAcct != null && !TextUtils.isEmpty(virtualAcct.name)) {
                for (Account a : accts) {
                    if (virtualAcct.name.equalsIgnoreCase(a.name)) return a;
                }
            }
            return accts[0];
        } catch (Throwable t) { return null; }
    }

    private static String requestHostToken(Context ctx, Account acct, String scope) {
        try {
            AccountManagerFuture<Bundle> f = AccountManager.get(ctx).getAuthToken(
                    acct, scope, null, false, null, null);
            Bundle r = f.getResult();
            if (r == null || r.getParcelable(AccountManager.KEY_INTENT) != null) return null;
            return r.getString(AccountManager.KEY_AUTHTOKEN);
        } catch (Throwable t) {
            Log.d(TAG, "requestHostToken scope=" + scope + ": " + t.getMessage());
            return null;
        }
    }

    private static boolean bridgeSingle(Context ctx, Account acct) {
        Account virtual = new Account(acct.name, GOOGLE_TYPE);
        try { BAccountManager.get().addAccountExplicitly(virtual,null,null); } catch (Throwable ignored) {}
        for (String scope : BRIDGE_SCOPES) {
            String tok = requestHostToken(ctx, acct, scope);
            if (!TextUtils.isEmpty(tok)) { storeAll(ctx, virtual, tok, scope); return true; }
        }
        setUD(virtual,"email",acct.name); setUD(virtual,"account_name",acct.name);
        return false;
    }

    private static void storeAll(Context ctx, Account a, String tok, String scope) {
        setAuth(a,scope,tok);
        if (SCOPE_GOOGLEPLAY.equals(scope) || scope.contains("googleplay")) {
            setAuth(a, SCOPE_GOOGLEPLAY, tok);
        }
        setAuth(a,"oauth2",tok);
        setAuth(a,"oauth2:openid email profile",tok);
        if (!scope.contains("googleplay")) setAuth(a,"id_token",tok);
        if (!scope.contains("googleplay")) GoogleSignInHelper.saveIdToken(ctx,tok);
        String email=GoogleSignInHelper.getEmail(ctx); if(TextUtils.isEmpty(email)) email=a.name;
        setUD(a,"email",email); setUD(a,"account_name",email);
        String g=GoogleSignInHelper.getGaiaId(ctx);
        setUD(a,"name",GoogleSignInHelper.getDisplayName(ctx));
        setUD(a,"display_name",GoogleSignInHelper.getDisplayName(ctx));
        setUD(a,"given_name",GoogleSignInHelper.getGivenName(ctx));
        setUD(a,"family_name",GoogleSignInHelper.getFamilyName(ctx));
        setUD(a,"gaia_id",g); setUD(a,"gaia",g); setUD(a,"gaiaId",g); setUD(a,"GaiaId",g);
        setUD(a,"obfuscatedGaiaId",g); setUD(a,"account_id",g); setUD(a,"accountId",g);
        setUD(a,"user_id",g); setUD(a,"userId",g); setUD(a,"google_user_id",g); setUD(a,"googleUserId",g);
    }

    private static void setAuth(Account a,String k,String v){if(!TextUtils.isEmpty(k)&&!TextUtils.isEmpty(v))try{BAccountManager.get().setAuthToken(a,k,v);}catch(Throwable ignored){}}
    private static void setUD(Account a,String k,String v){if(!TextUtils.isEmpty(k)&&!TextUtils.isEmpty(v))try{BAccountManager.get().setUserData(a,k,v);}catch(Throwable ignored){}}
    public static void reset() { sBridged=false; }
}
