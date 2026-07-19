package com.anubis.loader.fake.service.context;

import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import black.android.content.BRIRestrictionsManagerStub;
import black.android.os.BRServiceManager;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;

/**
 * Created by Milk on 4/8/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class RestrictionsManagerStub extends BinderInvocationStub {

    public RestrictionsManagerStub() {
        super(BRServiceManager.get().getService(Context.RESTRICTIONS_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIRestrictionsManagerStub.get().asInterface(BRServiceManager.get().getService(Context.RESTRICTIONS_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.RESTRICTIONS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getApplicationRestrictions")
    public static class GetApplicationRestrictions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String appPkg = BActivityThread.getAppPackageName();
            if (appPkg != null && GmsCore.isGoogleAppOrService(appPkg)) {
                return new Bundle();
            }
            if (args.length > 0 && args[0] instanceof String) {
                args[0] = AnubisCore.getHostPkg();
            }
            try {
                return method.invoke(who, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof SecurityException) {
                    return new Bundle();
                }
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }
}
