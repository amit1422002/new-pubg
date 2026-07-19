package com.anubis.loader.fake.service;

import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import black.android.content.pm.BRUserInfo;
import black.android.os.BRIUserManagerStub;
import black.android.os.BRServiceManager;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.Slog;

/**
 * Created by Milk on 4/8/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class IUserManagerProxy extends BinderInvocationStub {
    public IUserManagerProxy() {
        super(BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIUserManagerStub.get().asInterface(BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getApplicationRestrictions")
    public static class GetApplicationRestrictions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (shouldStubRestrictions()) {
                return new Bundle();
            }
            if (args.length > 0 && args[0] instanceof String) {
                args[0] = AnubisCore.getHostPkg();
            }
            try {
                return method.invoke(who, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof SecurityException) {
                    Slog.d("IUserManagerProxy", "getApplicationRestrictions stubbed: " + e.getCause().getMessage());
                    return new Bundle();
                }
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }

    /** Virtual GMS SignIn calls this; non-system UIDs get SecurityException on real UserManager. */
    @ProxyMethod("getApplicationRestrictionsForUser")
    public static class GetApplicationRestrictionsForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (shouldStubRestrictions()) {
                return new Bundle();
            }
            try {
                return method.invoke(who, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof SecurityException) {
                    Slog.d("IUserManagerProxy", "getApplicationRestrictionsForUser stubbed: "
                            + e.getCause().getMessage());
                    return new Bundle();
                }
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }

    private static boolean shouldStubRestrictions() {
        return shouldStubGmsUserQueries();
    }

    private static boolean shouldStubGmsUserQueries() {
        String appPkg = BActivityThread.getAppPackageName();
        return appPkg != null && GmsCore.isGoogleAppOrService(appPkg);
    }

    private static Object stubPrimaryUserInfo(int userId) {
        return BRUserInfo.get()._new(userId, "Primary", BRUserInfo.get().FLAG_PRIMARY());
    }

    /** Virtual GMS calls getUserInfo/isMainUser — real UserManager rejects cloned GMS UID. */
    @ProxyMethod("getUserInfo")
    public static class GetUserInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (shouldStubGmsUserQueries()) {
                int userId = args.length > 0 && args[0] instanceof Integer
                        ? (Integer) args[0] : BActivityThread.getUserId();
                Slog.d("IUserManagerProxy", "getUserInfo stub for GMS userId=" + userId);
                return stubPrimaryUserInfo(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof SecurityException) {
                    int userId = args.length > 0 && args[0] instanceof Integer
                            ? (Integer) args[0] : BActivityThread.getUserId();
                    Slog.d("IUserManagerProxy", "getUserInfo SecurityException stub");
                    return stubPrimaryUserInfo(userId);
                }
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }

    @ProxyMethod("isMainUser")
    public static class IsMainUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (shouldStubGmsUserQueries()) {
                return true;
            }
            try {
                return method.invoke(who, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof SecurityException) {
                    return true;
                }
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }

    @ProxyMethod("getProfileParent")
    public static class GetProfileParent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Guest was detecting "Anubis" profile parent user name from UserManager binder.
            Object anubisUser = BRUserInfo.get()._new(BActivityThread.getUserId(), "Primary", BRUserInfo.get().FLAG_PRIMARY());
            return anubisUser;
        }
    }

    @ProxyMethod("getUsers")
    public static class getUsers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return new ArrayList<>();
        }
    }
}
