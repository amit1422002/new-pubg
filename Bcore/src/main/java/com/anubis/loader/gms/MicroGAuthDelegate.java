package com.anubis.loader.gms;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;

import com.anubis.loader.fake.frameworks.BAccountManager;

/**
 * Completes microG Google account registration after WebView OAuth.
 */
final class MicroGAuthDelegate {
    private static final String TAG = "MicroGAuthDelegate";
    private static final String GOOGLE_TYPE = "com.google";
    private static final String GMS_PKG = "com.google.android.gms";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private MicroGAuthDelegate() {}

    static void exchangeOAuthToken(Activity activity, String oAuthToken,
                                   AccountAuthenticatorResponse authResponse) {
        new Thread(() -> {
            try {
                ClassLoader cl = MicroGCompat.getGmsClassLoader();
                Context gmsCtx = MicroGCompat.createAuthContext();
                Class<?> callbackClass = cl.loadClass("org.microg.gms.common.HttpFormClient$Callback");

                Object request = newAuthRequest(gmsCtx, cl);
                request = invoke(request, "token", oAuthToken);
                request = invoke(request, "isAccessToken");
                request = invoke(request, "addAccount");
                request = invoke(request, "getAccountId");
                request = invoke(request, "droidguardResults", "null");

                Object callback = Proxy.newProxyInstance(
                        cl,
                        new Class<?>[]{callbackClass},
                        (proxy, method, args) -> {
                            String name = method.getName();
                            if ("onResponse".equals(name)) {
                                MAIN.post(() -> onAuthResponse(activity, gmsCtx, cl, args[0], authResponse));
                            } else if ("onException".equals(name)) {
                                Log.e(TAG, "token exchange failed", (Throwable) args[0]);
                                MAIN.post(() -> {
                                    fail(authResponse, AccountManager.ERROR_CODE_NETWORK_ERROR, "auth failed");
                                    activity.finish();
                                });
                            }
                            return null;
                        });

                Method async = request.getClass().getMethod("getResponseAsync", callbackClass);
                async.invoke(request, callback);
            } catch (Throwable t) {
                Log.e(TAG, "exchangeOAuthToken", t);
                MAIN.post(() -> {
                    fail(authResponse, AccountManager.ERROR_CODE_NETWORK_ERROR, t.getMessage());
                    activity.finish();
                });
            }
        }, "microg-auth").start();
    }

    private static void onAuthResponse(Activity activity, Context gmsCtx, ClassLoader cl,
                                       Object authResponseObj,
                                       AccountAuthenticatorResponse authResponse) {
        try {
            String email = field(authResponseObj, "email");
            String token = field(authResponseObj, "token");
            if (email == null || token == null) {
                Log.e(TAG, "empty auth response email=" + email);
                fail(authResponse, AccountManager.ERROR_CODE_INVALID_RESPONSE, "empty auth");
                activity.finish();
                return;
            }
            Account account = new Account(email, GOOGLE_TYPE);
            boolean added = BAccountManager.get().addAccountExplicitly(account, token, null);
            if (!added) {
                try {
                    BAccountManager.get().setPassword(account, token);
                } catch (Throwable t) {
                    Log.w(TAG, "setPassword fallback", t);
                }
            }
            setIfPresent(account, "SID", field(authResponseObj, "Sid"));
            setIfPresent(account, "LSID", field(authResponseObj, "LSid"));
            setUserData(account, "flags", "1");
            setUserDataIfPresent(account, "services", field(authResponseObj, "services"));
            setUserData(account, "oauthAccessToken", "1");
            setUserDataIfPresent(account, "firstName", field(authResponseObj, "firstName"));
            setUserDataIfPresent(account, "lastName", field(authResponseObj, "lastName"));
            setUserDataIfPresent(account, "GoogleUserId", field(authResponseObj, "accountId"));

            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    BAccountManager.get().setAccountVisibility(account,
                            AccountManager.PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE,
                            AccountManager.VISIBILITY_USER_MANAGED_VISIBLE);
                } catch (Throwable t) {
                    Log.w(TAG, "setAccountVisibility legacy", t);
                }
            }
            PlayStoreAuthHelper.ensureGmsVisibility(account);
            Log.i(TAG, "Account saved, retrieving GMS token for " + email);
            retrieveGmsToken(activity, gmsCtx, cl, account, authResponse);
        } catch (Throwable t) {
            Log.e(TAG, "onAuthResponse", t);
            fail(authResponse, AccountManager.ERROR_CODE_NETWORK_ERROR, t.getMessage());
            activity.finish();
        }
    }

    private static void retrieveGmsToken(Activity activity, Context gmsCtx, ClassLoader cl,
                                         Account account, AccountAuthenticatorResponse authResponse) {
        new Thread(() -> {
            try {
                Class<?> authManagerCls = cl.loadClass("org.microg.gms.auth.AuthManager");
                Object authManager = authManagerCls
                        .getConstructor(Context.class, String.class, String.class, String.class)
                        .newInstance(gmsCtx, account.name, GMS_PKG, "ac2dm");
                invoke(authManager, "setPermitted", true);

                String password = BAccountManager.get().getPassword(account);
                Object request = newAuthRequest(gmsCtx, cl);
                request = invoke(request, "email", account.name);
                request = invoke(request, "token", password != null ? password : "");
                request = invoke(request, "systemPartition", true);
                request = invoke(request, "hasPermission", true);
                request = invoke(request, "addAccount");
                request = invoke(request, "getAccountId");
                request = invoke(request, "droidguardResults", "null");

                Class<?> callbackClass = cl.loadClass("org.microg.gms.common.HttpFormClient$Callback");
                Object callback = Proxy.newProxyInstance(
                        cl,
                        new Class<?>[]{callbackClass},
                        (proxy, method, args) -> {
                            if ("onResponse".equals(method.getName())) {
                                MAIN.post(() -> onGmsTokenReady(activity, authManager, args[0], account, authResponse));
                            } else if ("onException".equals(method.getName())) {
                                Log.e(TAG, "retrieveGmsToken failed", (Throwable) args[0]);
                                MAIN.post(() -> {
                                    notifyAccountAdded(activity, account.name);
                                    finishSuccess(activity, authResponse, account);
                                });
                            }
                            return null;
                        });

                Method async = request.getClass().getMethod("getResponseAsync", callbackClass);
                async.invoke(request, callback);
            } catch (Throwable t) {
                Log.e(TAG, "retrieveGmsToken", t);
                MAIN.post(() -> {
                    notifyAccountAdded(activity, account.name);
                    finishSuccess(activity, authResponse, account);
                });
            }
        }, "microg-gms-token").start();
    }

    private static void onGmsTokenReady(Activity activity, Object authManager, Object response,
                                        Account account, AccountAuthenticatorResponse authResponse) {
        try {
            invoke(authManager, "storeResponse", response);
        } catch (Throwable t) {
            Log.w(TAG, "storeResponse", t);
        }
        PlayStoreAuthHelper.ensureGmsVisibility(account);
        notifyAccountAdded(activity, account.name);
        finishSuccess(activity, authResponse, account);
        Log.i(TAG, "Google account added: " + account.name);
    }

    private static void notifyAccountAdded(Activity activity, String accountName) {
        try {
            Intent intent = new Intent("com.google.android.gms.auth.GOOGLE_ACCOUNT_CHANGE");
            intent.setPackage("com.android.vending");
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
            activity.sendBroadcast(intent, "com.google.android.gms.auth.permission.GOOGLE_ACCOUNT_CHANGE");
        } catch (Throwable t) {
            Log.w(TAG, "notifyAccountAdded", t);
        }
    }

    private static void finishSuccess(Activity activity, AccountAuthenticatorResponse authResponse,
                                      Account account) {
        if (authResponse != null) {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, GOOGLE_TYPE);
            result.putBoolean("new_account_created", true);
            authResponse.onResult(result);
        } else {
            notifyAccountAdded(activity, account.name);
            try {
                Intent added = new Intent("android.accounts.action.ACCOUNT_ADDED");
                added.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
                added.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                added.setPackage(activity.getPackageName());
                activity.sendBroadcast(added);
            } catch (Throwable t) {
                Log.w(TAG, "ACCOUNT_ADDED broadcast", t);
            }
            try {
                Intent gsi = new Intent(GoogleSignInHelper.ACTION_GSI_TOKEN);
                gsi.setPackage(activity.getPackageName());
                activity.sendBroadcast(gsi);
            } catch (Throwable t) {
                Log.w(TAG, "GSI_TOKEN broadcast", t);
            }
        }
        activity.setResult(Activity.RESULT_OK);
        activity.finish();
    }

    private static void fail(AccountAuthenticatorResponse resp, int code, String msg) {
        if (resp != null) resp.onError(code, msg != null ? msg : "error");
    }

    private static Object newAuthRequest(Context gmsCtx, ClassLoader cl) throws Exception {
        Class<?> cls = cl.loadClass("org.microg.gms.auth.AuthRequest");
        Object req = cls.getDeclaredConstructor().newInstance();
        req = invoke(req, "build", gmsCtx);
        req = invoke(req, "locale", Locale.getDefault());
        setField(req, "androidIdHex", Long.toHexString(MicroGCompat.getHostAndroidIdLong()));
        req = invoke(req, "appIsGms");
        req = invoke(req, "callerIsGms");
        req = invoke(req, "service", "ac2dm");
        return req;
    }

    private static Object invoke(Object target, String method, Object... args) throws Exception {
        if (args.length == 0) {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == 0) {
                    return m.invoke(target);
                }
            }
        }
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(method) || m.getParameterCount() != args.length) continue;
            Class<?>[] types = m.getParameterTypes();
            if (args.length == 1) {
                if (args[0] == null || types[0].isInstance(args[0])
                        || (args[0] instanceof Boolean && types[0] == boolean.class)) {
                    return m.invoke(target, args);
                }
                if (args[0] instanceof Activity && Context.class.isAssignableFrom(types[0])) {
                    return m.invoke(target, (Context) args[0]);
                }
            } else {
                return m.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(method);
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        for (Field f : obj.getClass().getFields()) {
            if (f.getName().equals(name)) {
                f.set(obj, value);
                return;
            }
        }
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static String field(Object obj, String name) throws Exception {
        return (String) obj.getClass().getField(name).get(obj);
    }

    private static void setIfPresent(Account account, String type, String value) {
        if (value != null) {
            try {
                BAccountManager.get().setAuthToken(account, type, value);
            } catch (Throwable t) {
                Log.w(TAG, "setAuthToken " + type, t);
            }
        }
    }

    private static void setUserData(Account account, String key, String value) {
        try {
            BAccountManager.get().setUserData(account, key, value);
        } catch (Throwable t) {
            Log.w(TAG, "setUserData " + key, t);
        }
    }

    private static void setUserDataIfPresent(Account account, String key, String value) {
        if (value != null) setUserData(account, key, value);
    }
}
