package com.anubis.loader.fake.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import black.android.content.pm.BRILauncherAppsStub;
import black.android.os.BRServiceManager;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.env.AppSystemEnv;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.utils.MethodParameterUtils;
import com.anubis.loader.utils.VirtualPathSpoof;

/**
 * Created by Milk on 4/13/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class ILauncherAppsProxy extends BinderInvocationStub {

    public ILauncherAppsProxy() {
        super(BRServiceManager.get().getService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRILauncherAppsStub.get().asInterface(BRServiceManager.get().getService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.LAUNCHER_APPS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        Object result = super.invoke(proxy, method, args);
        return filterLauncherResult(method.getName(), result);
    }

    private static Object filterLauncherResult(String methodName, Object result) {
        if (result == null || !VirtualPathSpoof.shouldSpoofForGuest()) {
            return result;
        }
        if (result instanceof List) {
            List<?> list = (List<?>) result;
            List<Object> filtered = new ArrayList<>(list.size());
            for (Object item : list) {
                if (shouldHideLauncherItem(item)) {
                    continue;
                }
                filtered.add(item);
            }
            return filtered;
        }
        if ("getActivityList".equals(methodName) && result.getClass().isArray()) {
            return result;
        }
        if (shouldHideLauncherItem(result)) {
            return null;
        }
        return result;
    }

    private static boolean shouldHideLauncherItem(Object item) {
        if (item == null) {
            return true;
        }
        String pkg = null;
        if (item instanceof LauncherActivityInfo) {
            pkg = ((LauncherActivityInfo) item).getComponentName().getPackageName();
        } else if (item instanceof ApplicationInfo) {
            pkg = ((ApplicationInfo) item).packageName;
        } else if (item instanceof ComponentName) {
            pkg = ((ComponentName) item).getPackageName();
        }
        if (pkg == null) {
            return false;
        }
        return VirtualPathSpoof.shouldHideLoaderPackageFromGuest(pkg)
                || AppSystemEnv.shouldHideContainerPackage(pkg)
                || AnubisCore.getHostPkg().equals(pkg);
    }
}
