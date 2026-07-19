package com.anubis.loader.fake.service;

import android.content.ComponentName;
import android.content.Context;

import java.lang.reflect.Method;

import black.android.app.admin.BRIDevicePolicyManagerStub;
import black.android.os.BRServiceManager;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.MethodParameterUtils;
import com.anubis.loader.utils.Slog;

/**
 * Created by Milk on 2021/5/17.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class IDevicePolicyManagerProxy extends BinderInvocationStub {
    public IDevicePolicyManagerProxy() {
        super(BRServiceManager.get().getService(Context.DEVICE_POLICY_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIDevicePolicyManagerStub.get().asInterface(BRServiceManager.get().getService(Context.DEVICE_POLICY_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getStorageEncryptionStatus")
    public static class GetStorageEncryptionStatus extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getDeviceOwnerComponent")
    public static class GetDeviceOwnerComponent extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return new ComponentName("", "");
        }
    }

    @ProxyMethod("getDeviceOwnerName")
    public static class getDeviceOwnerName extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Guest was detecting literal "Anubis" device owner name from DPM binder.
            return null;
        }
    }

    @ProxyMethod("getProfileOwnerName")
    public static class getProfileOwnerName extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("isDeviceProvisioned")
    public static class isDeviceProvisioned extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return true;
        }
    }

    /** Virtual GMS SignIn UI — real DPM rejects non-system / non-GMS callers. */
    @ProxyMethod("getAccountTypesWithManagementDisabled")
    public static class GetAccountTypesWithManagementDisabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (shouldStubForGmsUi()) {
                return new String[0];
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                if (isSecurityDenial(t)) {
                    Slog.d("IDevicePolicyProxy", "stub getAccountTypesWithManagementDisabled");
                    return new String[0];
                }
                throw t;
            }
        }
    }

    @ProxyMethod("getAccountTypesWithManagementDisabledAsUser")
    public static class GetAccountTypesWithManagementDisabledAsUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (shouldStubForGmsUi()) {
                return new String[0];
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                if (isSecurityDenial(t)) {
                    Slog.d("IDevicePolicyProxy", "stub getAccountTypesWithManagementDisabledAsUser");
                    return new String[0];
                }
                throw t;
            }
        }
    }

    private static boolean shouldStubForGmsUi() {
        String pkg = BActivityThread.getAppPackageName();
        return pkg != null && GmsCore.isGoogleAppOrService(pkg);
    }

    private static boolean isSecurityDenial(Throwable t) {
        Throwable c = t;
        while (c != null) {
            if (c instanceof SecurityException) {
                return true;
            }
            c = c.getCause();
        }
        return false;
    }
}
