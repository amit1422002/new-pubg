package com.anubis.loader.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.gms.MicroGCompat;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.Slog;


public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";
    private static IBinder sOriginalGmsBinder;
    private static volatile boolean sDisabledForMicroG;

    public GmsProxy() {
        super(resolveGmsBinder());
    }

    private static IBinder resolveGmsBinder() {
        IBinder binder = BRServiceManager.get().getService("gms");
        if (binder != null && !(binder instanceof GmsProxy)) {
            sOriginalGmsBinder = binder;
        }
        return sOriginalGmsBinder != null ? sOriginalGmsBinder : binder;
    }

    /** Bypass GmsProxy hooks when virtual microG is active — restore host gms binder for sandbox auth. */
    public static void disableForMicroG() {
        sDisabledForMicroG = true;
        if (sOriginalGmsBinder == null) {
            resolveGmsBinder();
        }
        if (sOriginalGmsBinder != null) {
            java.util.Map<String, IBinder> services = BRServiceManager.get().sCache();
            services.put("gms", sOriginalGmsBinder);
            Slog.d(TAG, "GmsProxy disabled — restored original gms binder for microG");
        } else {
            Slog.d(TAG, "GmsProxy disabled for virtual microG (no host gms binder)");
        }
    }

    private static boolean bypass() {
        return sDisabledForMicroG || MicroGCompat.shouldBypassGmsHooks();
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService("gms");
        if (binder == null) {
            Slog.e(TAG, "Failed to get gms service binder");
            return null;
        }
        try {
            Class<?> stubClass = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object iface = asInterfaceMethod.invoke(null, binder);
            if (iface != null) {
                Slog.d(TAG, "Successfully obtained IGmsServiceBroker interface");
                return iface;
            } else {
                Slog.e(TAG, "Reflection succeeded but returned null interface");
                return null;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IGmsServiceBroker interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (bypass()) {
            disableForMicroG();
            return;
        }
        IBinder binder = BRServiceManager.get().getService("gms");
        if (binder == null) {
            Slog.d(TAG, "No host gms binder — skip GmsProxy (microG-only)");
            return;
        }
        replaceSystemService("gms");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (bypass()) {
                return method.invoke(who, args);
            }
            try {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String callingPackage = (String) args[0];
                    String hostPkg = AnubisCore.getHostPkg();
                    if (!hostPkg.equals(callingPackage)) {
                        args[0] = hostPkg;
                        Slog.d(TAG, "GmsProxy: Fixed calling package from "
                                + callingPackage + " to " + hostPkg);
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Error in getService", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Error in getServiceBroker", e);
                
                return null;
            }
        }
    }

    
    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (bypass()) {
                return method.invoke(who, args);
            }
            try {
                Slog.d(TAG, "GmsProxy: Handling authenticate call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Authentication error, returning success", e);
                return createMockAuthResult();
            }
        }
    }

    
    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getAccount call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetAccount error, returning null", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (bypass()) {
                return method.invoke(who, args);
            }
            try {
                Slog.d(TAG, "GmsProxy: Handling getToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetToken error, returning mock token", e);
                return "mock_gms_token_" + System.currentTimeMillis();
            }
        }
    }

    
    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling invalidateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: InvalidateToken error, ignoring", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling clearToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: ClearToken error, ignoring", e);
                return null;
            }
        }
    }

    
    private static Object createMockAuthResult() {
        try {
            
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            return bundleClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock auth result", e);
            return null;
        }
    }
}
