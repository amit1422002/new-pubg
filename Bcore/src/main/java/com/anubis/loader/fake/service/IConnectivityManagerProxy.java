package com.anubis.loader.fake.service;

import android.content.Context;

import java.lang.reflect.Method;

import black.android.net.BRIConnectivityManagerStub;
import black.android.os.BRServiceManager;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.fake.hook.ScanClass;
import com.anubis.loader.utils.MethodParameterUtils;
import com.anubis.loader.utils.VirtualPathSpoof;

/**
 * Created by Milk on 4/12/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
@ScanClass(VpnCommonProxy.class)
public class IConnectivityManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IConnectivityManagerProxy";

    public IConnectivityManagerProxy() {
        super(BRServiceManager.get().getService(Context.CONNECTIVITY_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIConnectivityManagerStub.get().asInterface(BRServiceManager.get().getService(Context.CONNECTIVITY_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("requestNetwork")
    public static class RequestNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            applyStealthAttribution(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("registerNetworkCallback")
    public static class RegisterNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            applyStealthAttribution(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("registerNetworkAgent")
    public static class RegisterNetworkAgent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            applyStealthAttribution(args);
            return method.invoke(who, args);
        }
    }

    private static void applyStealthAttribution(Object[] args) {
        if (!VirtualPathSpoof.shouldSpoofForGuest()) {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return;
        }
        String guest = BActivityThread.getAppPackageName();
        if (guest == null || !VirtualPathSpoof.isStealthAcPackage(guest)) {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return;
        }
        MethodParameterUtils.replaceHostPkgWithGuest(args, guest);
    }
}
