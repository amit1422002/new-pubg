package com.anubis.loader.gms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.IAccountManagerResponse;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Map;

import black.android.accounts.BRIAccountManagerStub;
import black.android.os.BRServiceManager;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.fake.frameworks.BAccountManager;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.Slog;

/**
 * GmsAccountManagerProxy ΓÇö strongly based on zcore's eh.java.
 *
 * CALLER-AWARE STRATEGY (fixes both Play Store freeze AND Play Services crash):
 *
 * When addAccount/getAuthToken is called for "com.google" with no existing account:
 *
 *   If caller IS a GMS package (com.google.android.gms, com.android.vending etc):
 *     ΓåÆ Return KEY_INTENT pointing to GoogleSignInActivity
 *     ΓåÆ GMS frameworks handle KEY_INTENT correctly (they spawn the activity
 *       and wait for account-changed broadcast, not blocking the main thread)
 *
 *   If caller is ANY OTHER app (Play Store from outside GMS, third-party apps):
 *     ΓåÆ Return onError(CANCELED) immediately to unblock the caller
 *     ΓåÆ startActivity(GoogleSignInActivity) independently as new task
 *     ΓåÆ BroadcastReceiver picks up ACTION_GSI_TOKEN ΓåÆ bridges the result back
 *     ΓåÆ Avoids the Play Store freeze/deadlock
 */
public class GmsAccountManagerProxy extends BinderInvocationStub {
    public static final String TAG = "GmsAccountManagerProxy";
    static final String GOOGLE_TYPE = "com.google";

    /** When virtual microG GMS is installed, delegate to its AccountAuthenticator. */
    public static boolean useMicroGAuth() {
        try {
            int userId = BActivityThread.getUserId();
            return AnubisCore.get().isInstalled(GmsCore.GMS_PKG, userId);
        } catch (Throwable t) {
            return false;
        }
    }

    public GmsAccountManagerProxy() { super(BRServiceManager.get().getService(Context.ACCOUNT_SERVICE)); }
    @Override protected Object getWho() { return BRIAccountManagerStub.get().asInterface(BRServiceManager.get().getService(Context.ACCOUNT_SERVICE)); }
    @Override protected void inject(Object b, Object p) { replaceSystemService(Context.ACCOUNT_SERVICE); }
    @Override public boolean isBadEnv() { return false; }
    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Slog.d(TAG, "call " + method.getName()); return super.invoke(proxy, method, args);
    }

    @ProxyMethod("addAccount")
    public static class addAccount extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            String type=s(a,1); IAccountManagerResponse resp=(IAccountManagerResponse)a[0];
            if (!isG(type)) { BAccountManager.get().addAccount(resp,type,s(a,2),(String[])a[3],b(a,4),(Bundle)a[5]); return 0; }
            if (useMicroGAuth()) { BAccountManager.get().addAccount(resp,type,s(a,2),(String[])a[3],b(a,4),(Bundle)a[5]); return 0; }
            return handleGoogleAddAccount(resp, s(a,2));
        }
    }

    @ProxyMethod("addAccountAsUser")
    public static class addAccountAsUser extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            String type=s(a,1); IAccountManagerResponse resp=(IAccountManagerResponse)a[0];
            if (!isG(type)) { BAccountManager.get().addAccountAsUser(resp,type,s(a,2),(String[])a[3],b(a,4),(Bundle)a[5]); return 0; }
            if (useMicroGAuth()) { BAccountManager.get().addAccountAsUser(resp,type,s(a,2),(String[])a[3],b(a,4),(Bundle)a[5]); return 0; }
            return handleGoogleAddAccount(resp, s(a,2));
        }
    }

    @ProxyMethod("addAccountExplicitly")
    public static class addAccountExplicitly extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            Account acct=(Account)a[0];
            if (isGA(acct)&&isCached()) { populateData(acct); return Boolean.TRUE; }
            return BAccountManager.get().addAccountExplicitly(acct,s(a,1),(Bundle)a[2]);
        }
    }

    @ProxyMethod("addAccountExplicitlyWithVisibility")
    public static class addAccountExplicitlyWithVisibility extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            Account acct=(Account)a[0];
            if (isGA(acct)&&isCached()) { populateData(acct); return Boolean.TRUE; }
            return BAccountManager.get().addAccountExplicitlyWithVisibility(acct,s(a,1),(Bundle)a[2],(Map)a[3]);
        }
    }

    @ProxyMethod("getAuthToken")
    public static class getAuthToken extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            IAccountManagerResponse resp=(IAccountManagerResponse)a[0];
            Account acct=(Account)a[1]; String tt=s(a,2);
            if (!isGA(acct)) { BAccountManager.get().getAuthToken(resp,acct,tt,b(a,3),b(a,4),(Bundle)a[5]); return 0; }
            if (useMicroGAuth()) { BAccountManager.get().getAuthToken(resp,acct,tt,b(a,3),b(a,4),(Bundle)a[5]); return 0; }
            String tok=resolveToken(acct,tt);
            if (!TextUtils.isEmpty(tok)) {
                Bundle r=new Bundle();
                r.putString(AccountManager.KEY_AUTHTOKEN,tok);
                r.putString(AccountManager.KEY_ACCOUNT_NAME,acct.name);
                r.putString(AccountManager.KEY_ACCOUNT_TYPE,acct.type);
                try{resp.onResult(r);}catch(Throwable ignored){}
                return 0;
            }
            Context ctx = AnubisCore.getContext();
            if (ctx == null) { try{resp.onError(1,"no context");}catch(Throwable ignored){} return 0; }
            Log.d(TAG, "getAuthToken needs sign-in type=" + tt + " account=" + acct.name);
            registerGsiReceiver(ctx, tt);
            returnSignInIntent(resp, ctx, tt);
            return 0;
        }
    }

    @ProxyMethod("peekAuthToken")
    public static class peekAuthToken extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            Account acct=(Account)a[0]; String tt=s(a,1);
            if (isGA(acct) && useMicroGAuth()) {
                return BAccountManager.get().peekAuthToken(acct, tt);
            }
            String tok=null; try{tok=BAccountManager.get().peekAuthToken(acct,tt);}catch(Throwable ignored){}
            if (isGA(acct)&&TextUtils.isEmpty(tok)) tok=resolveToken(acct,tt);
            return tok;
        }
    }

    @ProxyMethod("getUserData")
    public static class getUserData extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            Account acct=(Account)a[0]; String k=s(a,1);
            String v=null; try{v=BAccountManager.get().getUserData(acct,k);}catch(Throwable ignored){}
            if (!TextUtils.isEmpty(v)) return v;
            return isGA(acct)?synth(acct,k):v;
        }
    }

    @ProxyMethod("getAuthenticatorTypes")
    public static class getAuthenticatorTypes extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            if (useMicroGAuth()) return BAccountManager.get().getAuthenticatorTypes();
            return injectGoogleAuthenticator(BAccountManager.get().getAuthenticatorTypes());
        }
    }

    @ProxyMethod("getAccountByTypeAndFeatures")
    public static class getAccountByTypeAndFeatures extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            String type=s(a,1); IAccountManagerResponse resp=(IAccountManagerResponse)a[0];
            if (isG(type)&&isCached()) {
                Account ex=existing(); if(ex!=null){populateData(ex);returnArr(resp,new Account[]{ex});return 0;}
                returnArr(resp,new Account[0]); return 0;
            }
            BAccountManager.get().getAccountByTypeAndFeatures(resp,type,(String[])a[2]); return 0;
        }
    }

    @ProxyMethod("removeAccountAsUser")
    public static class removeAccountAsUser extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            Account acct=(Account)a[1]; if(isGA(acct))GoogleSignInHelper.clearCachedToken(AnubisCore.getContext());
            BAccountManager.get().removeAccountAsUser((IAccountManagerResponse)a[0],acct,b(a,2)); return 0;
        }
    }
    @ProxyMethod("removeAccountExplicitly")
    public static class removeAccountExplicitly extends MethodHook {
        @Override protected Object hook(Object who, Method m, Object[] a) throws Throwable {
            Account acct=(Account)a[0]; if(isGA(acct))GoogleSignInHelper.clearCachedToken(AnubisCore.getContext());
            return BAccountManager.get().removeAccountExplicitly(acct);
        }
    }

    @ProxyMethod("accountAuthenticated") public static class accountAuthenticated extends MethodHook{
        @Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{
            Account acct=(Account)a[0];
            if (isGA(acct) && useMicroGAuth()) return BAccountManager.get().accountAuthenticated(acct);
            if (isGA(acct) && isCached()) return Boolean.TRUE;
            return BAccountManager.get().accountAuthenticated(acct);
        }
    }
    @ProxyMethod("getPassword") public static class getPassword extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{return BAccountManager.get().getPassword((Account)a[0]);}}
    @ProxyMethod("getAccountsForPackage") public static class getAccountsForPackage extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{return BAccountManager.get().getAccountsForPackage((String)a[0],(int)a[1]);}}
    @ProxyMethod("getAccountsByTypeForPackage") public static class getAccountsByTypeForPackage extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{return BAccountManager.get().getAccountsByTypeForPackage((String)a[0],(String)a[1]);}}
    @ProxyMethod("getAccountsByFeatures") public static class getAccountsByFeatures extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().getAccountsByFeatures((IAccountManagerResponse)a[0],(String)a[1],(String[])a[2]);return 0;}}
    @ProxyMethod("getAccountsAsUser") public static class getAccountsAsUser extends MethodHook{
        @Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{
            if (isG((String)a[0]) && useMicroGAuth()) return BAccountManager.get().getAccountsAsUser((String)a[0]);
            return BAccountManager.get().getAccountsAsUser((String)a[0]);
        }
    }
    @ProxyMethod("copyAccountToUser") public static class copyAccountToUser extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().copyAccountToUser((IAccountManagerResponse)a[0],(Account)a[1],(int)a[2],(int)a[3]);return 0;}}
    @ProxyMethod("invalidateAuthToken") public static class invalidateAuthToken extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().invalidateAuthToken((String)a[0],(String)a[1]);return 0;}}
    @ProxyMethod("setAuthToken") public static class setAuthToken extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().setAuthToken((Account)a[0],(String)a[1],(String)a[2]);return 0;}}
    @ProxyMethod("setPassword") public static class setPassword extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().setPassword((Account)a[0],(String)a[1]);return 0;}}
    @ProxyMethod("clearPassword") public static class clearPassword extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().clearPassword((Account)a[0]);return 0;}}
    @ProxyMethod("setUserData") public static class setUserData extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().setUserData((Account)a[0],(String)a[1],(String)a[2]);return 0;}}
    @ProxyMethod("updateAppPermission") public static class updateAppPermission extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().updateAppPermission((Account)a[0],(String)a[1],(int)a[2],(boolean)a[3]);return 0;}}
    @ProxyMethod("updateCredentials") public static class updateCredentials extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().updateCredentials((IAccountManagerResponse)a[0],(Account)a[1],(String)a[2],(boolean)a[3],(Bundle)a[4]);return 0;}}
    @ProxyMethod("editProperties") public static class editProperties extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().editProperties((IAccountManagerResponse)a[0],(String)a[1],(boolean)a[2]);return 0;}}
    @ProxyMethod("confirmCredentialsAsUser") public static class confirmCredentialsAsUser extends MethodHook{
        @Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{
            Account acct=(Account)a[1];
            if (isGA(acct) && useMicroGAuth()) {
                BAccountManager.get().confirmCredentialsAsUser((IAccountManagerResponse)a[0],acct,(Bundle)a[2],b(a,3));
                return 0;
            }
            if (isGA(acct) && hasGoogleAccount()) {
                IAccountManagerResponse resp = (IAccountManagerResponse) a[0];
                Bundle r = new Bundle();
                r.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                try { resp.onResult(r); } catch (Throwable ignored) {}
                return 0;
            }
            BAccountManager.get().confirmCredentialsAsUser((IAccountManagerResponse)a[0],acct,(Bundle)a[2],b(a,3)); return 0;
        }
    }
    @ProxyMethod("getAuthTokenLabel") public static class getAuthTokenLabel extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().getAuthTokenLabel((IAccountManagerResponse)a[0],(String)a[1],(String)a[2]);return 0;}}
    @ProxyMethod("getPackagesAndVisibilityForAccount") public static class getPackagesAndVisibilityForAccount extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{return BAccountManager.get().getPackagesAndVisibilityForAccount((Account)a[0]);}}
    @ProxyMethod("setAccountVisibility") public static class setAccountVisibility extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{return BAccountManager.get().setAccountVisibility((Account)a[0],(String)a[1],(int)a[2]);}}
    @ProxyMethod("getAccountVisibility") public static class getAccountVisibility extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{return BAccountManager.get().getAccountVisibility((Account)a[0],(String)a[1]);}}
    @ProxyMethod("getAccountsAndVisibilityForPackage") public static class getAccountsAndVisibilityForPackage extends MethodHook{
        @Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{
            String pkg = (String) a[0];
            if (isGmsPkg(pkg) && hasGoogleAccount()) {
                Account ex = existing();
                if (ex != null) {
                    java.util.HashMap<String, Integer> map = new java.util.HashMap<>();
                    map.put(ex.name, AccountManager.VISIBILITY_VISIBLE);
                    return map;
                }
            }
            return BAccountManager.get().getAccountsAndVisibilityForPackage((String)a[0],(String)a[1]);
        }
    }
    @ProxyMethod("registerAccountListener") public static class registerAccountListener extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().registerAccountListener((String[])a[0],(String)a[1]);return 0;}}
    @ProxyMethod("unregisterAccountListener") public static class unregisterAccountListener extends MethodHook{@Override protected Object hook(Object w,Method m,Object[] a)throws Throwable{BAccountManager.get().unregisterAccountListener((String[])a[0],(String)a[1]);return 0;}}

    // ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ
    // THE FIX: Caller-aware response strategy
    // ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ

    public static Object handleGoogleAddAccount(IAccountManagerResponse resp, String tokenType) {
        try {
            Account ex = existing();
            if (ex != null) {
                populateData(ex);
                String tok = resolveToken(ex, tokenType);
                if (!TextUtils.isEmpty(tok)) {
                    returnExisting(resp, ex, tokenType);
                    return 0;
                }
            }

            Context ctx = AnubisCore.getContext();
            if (ctx == null) { try{resp.onError(1,"no context");}catch(Throwable ignored){} return 0; }

            Log.d(TAG, "handleGoogleAddAccount KEY_INTENT tokenType=" + tokenType);

            registerGsiReceiver(ctx, tokenType);
            returnSignInIntent(resp, ctx, tokenType);
            return 0;
        } catch (Throwable t) {
            Log.e(TAG, "handleGoogleAddAccount", t);
            try { resp.onError(1, t.getMessage()); } catch (Throwable ignored) {}
            return 0;
        }
    }

    private static void returnSignInIntent(IAccountManagerResponse resp, Context ctx, String tokenType) {
        try {
            Bundle r = new Bundle();
            r.putParcelable(AccountManager.KEY_INTENT, buildSignInIntent(ctx, tokenType));
            resp.onResult(r);
        } catch (Throwable t) { Log.w(TAG, "returnSignInIntent", t); }
    }

    private static Intent buildSignInIntent(Context ctx, String tokenType) {
        Intent intent = new Intent();
        String activity = useMicroGAuth()
                ? "com.anubis.loader.gms.MicroGLoginBridgeActivity"
                : "com.anubis.loader.gms.GoogleSignInActivity";
        intent.setComponent(new ComponentName(AnubisCore.getHostPkg(), activity));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!TextUtils.isEmpty(tokenType))
            intent.putExtra(GoogleSignInHelper.EXTRA_AUTH_TOKEN_TYPE, tokenType);
        return intent;
    }

    private static void launchSignInActivity(Context ctx, String tokenType) {
        try { ctx.startActivity(buildSignInIntent(ctx, tokenType)); }
        catch (Throwable t) { Log.w(TAG, "startActivity failed", t); }
    }

    private static void registerGsiReceiver(Context ctx, String tokenType) {
        try {
            BroadcastReceiver[] h = new BroadcastReceiver[1];
            h[0] = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    try { c.unregisterReceiver(this); } catch (Throwable ignored) {}
                    if (!GoogleSignInHelper.ACTION_GSI_TOKEN.equals(i != null ? i.getAction() : null)) return;
                    Account acct = existing();
                    if (acct == null) {
                        String email = GoogleSignInHelper.getEmail(c);
                        if (!TextUtils.isEmpty(email)) acct = new Account(email, GOOGLE_TYPE);
                    }
                    if (acct != null) {
                        populateData(acct);
                        Log.d(TAG, "GSI_TOKEN received: "+acct.name);
                        try {
                            Intent changed = new Intent("android.accounts.action.ACCOUNT_ADDED");
                            changed.putExtra(AccountManager.KEY_ACCOUNT_NAME, acct.name);
                            changed.putExtra(AccountManager.KEY_ACCOUNT_TYPE, acct.type);
                            changed.setPackage(c.getPackageName());
                            c.sendBroadcast(changed);
                        } catch (Throwable ignored) {}
                    }
                }
            };
            IntentFilter f = new IntentFilter();
            f.addAction(GoogleSignInHelper.ACTION_GSI_TOKEN);
            f.addAction(GoogleSignInHelper.ACTION_GSI_CANCEL);
            if (Build.VERSION.SDK_INT >= 26)
                ctx.registerReceiver(h[0], f, Context.RECEIVER_NOT_EXPORTED);
            else
                ctx.registerReceiver(h[0], f);
        } catch (Throwable t) { Log.w(TAG, "registerGsiReceiver", t); }
    }

    // ΓöÇΓöÇ Helpers ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    static boolean isG(String t) { return GOOGLE_TYPE.equals(t); }
    static boolean isGA(Account a) { return a!=null&&GOOGLE_TYPE.equals(a.type); }
    static boolean isGmsPkg(String pkg) {
        if (TextUtils.isEmpty(pkg)) return false;
        for (String g : GoogleSignInHelper.GMS_PACKAGES) {
            if (g.equals(pkg)) return true;
        }
        return "com.android.vending".equals(pkg);
    }
    static boolean isCached() {
        return hasGoogleAccount();
    }

    public static boolean hasGoogleAccount() {
        if (existing() != null) return true;
        Context c=AnubisCore.getContext();
        return c!=null&&!TextUtils.isEmpty(GoogleSignInHelper.getCachedIdToken(c));
    }

    static Account existing() {
        try { Account[] a=BAccountManager.get().getAccountsAsUser(GOOGLE_TYPE); if(a!=null&&a.length>0) return a[0]; } catch (Throwable ignored) {}
        Context c=AnubisCore.getContext();
        if (c!=null) { String e=GoogleSignInHelper.getEmail(c); if(!TextUtils.isEmpty(e)) return new Account(e,GOOGLE_TYPE); }
        return null;
    }

    static void populateData(Account acct) {
        if (!isGA(acct)) return;
        Context c=AnubisCore.getContext(); if(c==null) return;
        PlayStoreAuthHelper.ensureGmsVisibility(acct);
        String email=GoogleSignInHelper.getEmail(c); if(TextUtils.isEmpty(email)) email=acct.name;
        String gaia=GoogleSignInHelper.getGaiaId(c);
        ud(acct,"email",email); ud(acct,"account_name",email);
        ud(acct,"name",GoogleSignInHelper.getDisplayName(c)); ud(acct,"display_name",GoogleSignInHelper.getDisplayName(c));
        ud(acct,"given_name",GoogleSignInHelper.getGivenName(c)); ud(acct,"family_name",GoogleSignInHelper.getFamilyName(c));
        ud(acct,"gaia_id",gaia); ud(acct,"gaia",gaia); ud(acct,"gaiaId",gaia); ud(acct,"GaiaId",gaia);
        ud(acct,"obfuscatedGaiaId",gaia); ud(acct,"account_id",gaia); ud(acct,"accountId",gaia);
        ud(acct,"user_id",gaia); ud(acct,"userId",gaia); ud(acct,"google_user_id",gaia); ud(acct,"googleUserId",gaia);
    }
    private static void ud(Account a,String k,String v){if(!TextUtils.isEmpty(k)&&!TextUtils.isEmpty(v))try{BAccountManager.get().setUserData(a,k,v);}catch(Throwable ignored){}}

    public static String resolveToken(Account acct, String tt) {
        if (!isGA(acct)) return null;
        try { String t=BAccountManager.get().peekAuthToken(acct,tt); if(!TextUtils.isEmpty(t)) return t; } catch (Throwable ignored) {}
        String can=GoogleSignInHelper.canonicaliseTokenType(tt);
        if (!TextUtils.isEmpty(can)&&!can.equals(tt)) {
            try { String t=BAccountManager.get().peekAuthToken(acct,can);
                if(!TextUtils.isEmpty(t)){try{BAccountManager.get().setAuthToken(acct,tt,t);}catch(Throwable ignored){} return t;} } catch (Throwable ignored) {}
        }
        if (GoogleSignInHelper.isIdTokenRequest(tt)) {
            Context c=AnubisCore.getContext();
            if(c!=null) return GoogleSignInHelper.getCachedIdToken(c);
        }
        String[] aliases = new String[] {
                "oauth2:https://www.googleapis.com/auth/googleplay",
                "oauth2:https://www.googleapis.com/auth/userinfo.email",
                "oauth2:openid email profile",
                "oauth2",
                "id_token"
        };
        for (String alias : aliases) {
            try {
                String t = BAccountManager.get().peekAuthToken(acct, alias);
                if (!TextUtils.isEmpty(t)) {
                    if (!TextUtils.isEmpty(tt) && !tt.equals(alias)) {
                        try { BAccountManager.get().setAuthToken(acct, tt, t); } catch (Throwable ignored) {}
                    }
                    return t;
                }
            } catch (Throwable ignored) {}
        }
        if (!TextUtils.isEmpty(tt) && tt.contains("googleplay")) {
            return null;
        }
        Context c = AnubisCore.getContext();
        if (c != null) {
            String cached = GoogleSignInHelper.getCachedIdToken(c);
            if (!TextUtils.isEmpty(cached) && !TextUtils.isEmpty(tt)) {
                try { BAccountManager.get().setAuthToken(acct, tt, cached); } catch (Throwable ignored) {}
            }
            return cached;
        }
        return null;
    }

    static String synth(Account acct, String key) {
        if (!isGA(acct)||TextUtils.isEmpty(key)) return null;
        Context c=AnubisCore.getContext(); String l=key.toLowerCase();
        if (l.startsWith("scopes:")) return "oauth2:https://www.googleapis.com/auth/googleplay";
        if ("services".equals(l)) return "mail,androidmarket,ac2dm,oauth2";
        if (l.equals("enabled_capabilities")||l.equals("disabled_capabilities")||l.equals("failed_capabilities")) return "";
        if ("package_visibilities_for_capabilities".equals(l)) return "{}";
        if ("capability_sync_time".equals(l)) return String.valueOf(System.currentTimeMillis());
        if ("lstwrittentoappdata".equals(l)) return "1";
        String gaia=c!=null?GoogleSignInHelper.getGaiaId(c):null;
        String email=c!=null?GoogleSignInHelper.getEmail(c):acct.name;
        if (l.contains("gaia")||l.contains("account_id")||l.contains("accountid")||l.contains("obfuscated")||l.contains("user_id")||l.contains("userid")||l.contains("google_user_id")||l.contains("googleuserid"))
            return TextUtils.isEmpty(gaia)?email:gaia;
        if (l.contains("email")||l.contains("account_name")) return email;
        if (l.contains("given")) return c!=null?GoogleSignInHelper.getGivenName(c):null;
        if (l.contains("family")) return c!=null?GoogleSignInHelper.getFamilyName(c):null;
        if (l.equals("name")||l.contains("display")) return c!=null?GoogleSignInHelper.getDisplayName(c):null;
        return null;
    }

    static void returnArr(IAccountManagerResponse resp, Account[] accts) {
        try { Bundle r=new Bundle(); r.putParcelableArray("accounts",accts); resp.onResult(r); } catch (Throwable t) { Log.w(TAG,"returnArr",t); }
    }
    static void returnExisting(IAccountManagerResponse resp, Account acct, String tt) {
        try {
            String tok=resolveToken(acct,tt);
            Bundle r=new Bundle();
            r.putString(AccountManager.KEY_ACCOUNT_NAME,acct.name);
            r.putString(AccountManager.KEY_ACCOUNT_TYPE,acct.type);
            r.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,true);
            if (!TextUtils.isEmpty(tok)) r.putString(AccountManager.KEY_AUTHTOKEN,tok);
            resp.onResult(r);
        } catch (Throwable t) { Log.w(TAG,"returnExisting",t); }
    }

    public static AuthenticatorDescription[] injectGoogleAuthenticator(AuthenticatorDescription[] real) {
        if (useMicroGAuth()) {
            return real != null ? real : new AuthenticatorDescription[0];
        }
        if (real==null) real=new AuthenticatorDescription[0];
        for (AuthenticatorDescription d:real) if (GOOGLE_TYPE.equals(d.type)) return real;
        try {
            AuthenticatorDescription g=new AuthenticatorDescription(GOOGLE_TYPE,"com.google.android.gms",0,0,0,0,false);
            AuthenticatorDescription[] out=new AuthenticatorDescription[real.length+1];
            System.arraycopy(real,0,out,0,real.length); out[real.length]=g; return out;
        } catch (Throwable t) { return real; }
    }

    private static String s(Object[] a,int i){if(a==null||i>=a.length)return null;return(String)a[i];}
    private static boolean b(Object[] a,int i){if(a==null||i>=a.length)return false;Object o=a[i];return o instanceof Boolean&&(Boolean)o;}
}
