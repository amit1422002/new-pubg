package com.anubis.loader.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import black.android.app.BRActivityThread;
import black.android.app.BRContextImpl;
import black.android.app.ContextImpl;
//import black.android.content.pm.BRPackageManager;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.system.BProcessManagerService;
import com.anubis.loader.core.system.ProcessRecord;
import com.anubis.loader.core.system.user.BUserHandle;
import com.anubis.loader.core.env.AppSystemEnv;
import com.anubis.loader.proxy.ProxyManifest;
//import com.anubis.loader.fake.FakeCore;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.fake.service.base.PkgMethodProxy;
import com.anubis.loader.fake.service.base.ValueMethodProxy;
import com.anubis.loader.fake.service.pm.VirtualPackageInstaller;
import com.anubis.loader.utils.MethodParameterUtils;
import com.anubis.loader.utils.Reflector;
import com.anubis.loader.utils.Slog;
import com.anubis.loader.utils.DfmStealthTier;
import com.anubis.loader.utils.VirtualPathSpoof;
import com.anubis.loader.utils.compat.BuildCompat;
import com.anubis.loader.utils.compat.ParceledListSliceCompat;
import org.lsposed.lsparanoid.Obfuscate;

/**
 * Created by Milk on 3/30/21.
 * * Γêº∩╝┐Γêº
 * (`∩╜Ñ╧ë∩╜ÑΓêÑ
 * Σ╕╢πÇÇπüñ∩╝É
 * πüùπâ╝∩╝¬
 * µ¡ñσñäµùáBug
 */
@Obfuscate
public class IPackageManagerProxy extends BinderInvocationStub {
    public static final String TAG = "PackageManagerStub";

    /**
     * Stub processes (:pN) must resolve host PackageInfo during ActivityThread.handleBindApplication.
     * Only hide the host loader from queries after the guest app is fully bound.
     */
    private static boolean shouldHideHostFromGuest() {
        if (!BActivityThread.isThreadInit()) {
            return false;
        }
        if (!BActivityThread.currentActivityThread().isInit()) {
            return false;
        }
        String appPkg = BActivityThread.getAppPackageName();
        return appPkg != null && !appPkg.equals(AnubisCore.getHostPkg());
    }

    public IPackageManagerProxy() {
        super(BRActivityThread.get().sPackageManager().asBinder());
    }

    @Override
    protected Object getWho() {
        return BRActivityThread.get().sPackageManager();
    }

     @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRActivityThread.get()._set_sPackageManager(proxyInvocation);
        replaceSystemService("package");
        Object systemContext = BRActivityThread.get(AnubisCore.mainThread()).getSystemContext();
        PackageManager mPackageManager = BRContextImpl.get(systemContext).mPackageManager();
        if (mPackageManager != null) {
            try {
                Reflector.on("android.app.ApplicationPackageManager").field("mPM").set(mPackageManager, proxyInvocation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
        addMethodHook(new PkgMethodProxy("shouldShowRequestPermissionRationale"));
        if (BuildCompat.isT()) {
            return;
        }
        addMethodHook(new PkgMethodProxy("clearPackagePreferredActivities"));
    }

    @ProxyMethod("resolveIntent")
    public static class ResolveIntent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            int flags = MethodParameterUtils.toInt(args[2]);
            ResolveInfo resolveInfo = AnubisCore.getBPackageManager().resolveIntent(intent, resolvedType, flags, BActivityThread.getUserId());
            if (resolveInfo != null) {
                return spoofResolveInfoForGuest(resolveInfo);
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("resolveService")
    public static class ResolveService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            int flags = MethodParameterUtils.toInt(args[2]);
            ResolveInfo resolveInfo = AnubisCore.getBPackageManager().resolveService(intent, flags, resolvedType, BActivityThread.getUserId());
            if (resolveInfo != null) {
                return spoofResolveInfoForGuest(resolveInfo);
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setComponentEnabledSetting")
    public static class SetComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

  @ProxyMethod("getPackageInfo")
    public static class GetPackageInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];
            // API 33+: getPackageInfo(String, long versionCode, int flags) ΓÇö arg[1] is versionCode, not flags.
            if (args.length >= 3 && args[1] instanceof Long) {
                return method.invoke(who, args);
            }
            int flags;
            if (args.length >= 3 && args[1] instanceof Integer && args[2] instanceof Integer) {
                flags = (int) args[1];
                MethodParameterUtils.replaceLastUserId(args);
                return method.invoke(who, args);
            }
            flags = MethodParameterUtils.toPackageFlags(args[1]);

            PackageInfo result;
            if (packageName != null && packageName.equals(AnubisCore.getHostPkg())) {
                if (shouldHideHostFromGuest()) {
                    return null;
                }
                result = (PackageInfo) method.invoke(who, args);
            } else if (GmsCore.isGoogleAppOrService(packageName)) {
                result = getClonedGooglePackageInfo(packageName, flags);
                if (result == null) {
                    result = (PackageInfo) method.invoke(who, args);
                }
            } else {
                PackageInfo packageInfo = AnubisCore.getBPackageManager()
                        .getPackageInfo(packageName, flags, BActivityThread.getUserId());
                if (packageInfo != null) {
                    result = packageInfo;
                } else if (shouldHideHostFromGuest()) {
                    result = null;
                } else {
                    result = (PackageInfo) method.invoke(who, args);
                }
            }
            return spoofPackageInfoForGuest(result);
        }
    }

    @ProxyMethod("queryIntentActivities")
    public static class QueryIntentActivities extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = args.length > 1 ? (String) args[1] : null;
            int flags = MethodParameterUtils.toInt(args.length > 2 ? args[2] : args[1]);
            List<ResolveInfo> resolves = AnubisCore.getBPackageManager()
                    .queryIntentActivities(intent, flags, resolvedType, BActivityThread.getUserId());
            filterResolveInfoFromGuest(resolves);
            spoofResolveInfoListForGuest(resolves);
            if (BuildCompat.isN()) {
                return ParceledListSliceCompat.create(resolves);
            }
            return resolves;
        }
    }

    @ProxyMethod("getPackageUid")
    public static class GetPackageUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            String pkg = (String) args[0];
            if (pkg != null) {
                try {
                    if (AnubisCore.getBPackageManager().getApplicationInfo(
                            pkg, 0, BActivityThread.getUserId()) != null) {
                        return AnubisCore.getHostUid();
                    }
                } catch (Throwable ignored) {
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("hasSigningCertificate")
    public static class HasSigningCertificate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderInfo")
    public static class GetProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            if (ProxyManifest.isHostProxyComponent(componentName) && shouldHideHostFromGuest()) {
                return null;
            }
            if (ProxyManifest.isHostProxyComponent(componentName)) {
                return method.invoke(who, args);
            }
            int flags = MethodParameterUtils.toInt(args[1]);
            ProviderInfo providerInfo = AnubisCore.getBPackageManager().getProviderInfo(componentName, flags, BActivityThread.getUserId());
            if (providerInfo != null)
                return spoofProviderInfoForGuest(providerInfo);
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getReceiverInfo")
    public static class GetReceiverInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            if (ProxyManifest.isHostProxyComponent(componentName) && shouldHideHostFromGuest()) {
                return null;
            }
            if (ProxyManifest.isHostProxyComponent(componentName)) {
                return method.invoke(who, args);
            }
            int flags = MethodParameterUtils.toInt(args[1]);
            ActivityInfo receiverInfo = AnubisCore.getBPackageManager().getReceiverInfo(componentName, flags, BActivityThread.getUserId());
            if (receiverInfo != null)
                return spoofActivityInfoForGuest(receiverInfo);
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getActivityInfo")
    public static class GetActivityInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            if (ProxyManifest.isHostProxyComponent(componentName) && shouldHideHostFromGuest()) {
                return null;
            }
            if (ProxyManifest.isHostProxyComponent(componentName)) {
                return method.invoke(who, args);
            }
            int flags = MethodParameterUtils.toInt(args[1]);
            ActivityInfo activityInfo = AnubisCore.getBPackageManager().getActivityInfo(componentName, flags, BActivityThread.getUserId());
            if (activityInfo != null)
                return spoofActivityInfoForGuest(activityInfo);
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getServiceInfo")
    public static class GetServiceInfo extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            if (ProxyManifest.isHostProxyComponent(componentName) && shouldHideHostFromGuest()) {
                return null;
            }
            if (ProxyManifest.isHostProxyComponent(componentName)) {
                return method.invoke(who, args);
            }
            int flags = MethodParameterUtils.toInt(args[1]);
            ServiceInfo serviceInfo = AnubisCore.getBPackageManager().getServiceInfo(componentName, flags, BActivityThread.getUserId());
            if (serviceInfo != null)
                return spoofServiceInfoForGuest(serviceInfo);
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getInstalledApplications")
    public static class GetInstalledApplications extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[0]);
            List<ApplicationInfo> installedApplications =
                    new ArrayList<>(AnubisCore.getBPackageManager().getInstalledApplications(flags, BActivityThread.getUserId()));
            filterHiddenFromGuest(installedApplications);
            mergeHostGoogleApplications(installedApplications, flags);
            spoofApplicationInfoListForGuest(installedApplications);
            return ParceledListSliceCompat.create(installedApplications);
        }
    }

    @ProxyMethod("getInstalledPackages")
    public static class GetInstalledPackages extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[0]);
            List<PackageInfo> installedPackages =
                    new ArrayList<>(AnubisCore.getBPackageManager().getInstalledPackages(flags, BActivityThread.getUserId()));
            filterHiddenPackagesFromGuest(installedPackages);
            mergeHostGooglePackages(installedPackages, flags);
            spoofPackageInfoListForGuest(installedPackages);
            return ParceledListSliceCompat.create(installedPackages);
        }
    }

    private static final int GMS_VERSION_META = 12451000;

    private static void ensureHostGmsMetaData(ApplicationInfo info) {
        if (info == null) {
            return;
        }
        if (info.metaData == null) {
            info.metaData = new Bundle();
        }
        if (!info.metaData.containsKey("com.google.android.gms.version")) {
            info.metaData.putInt("com.google.android.gms.version", GMS_VERSION_META);
        }
    }

    @ProxyMethod("getApplicationInfo")
    public static class GetApplicationInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];
            int flags = MethodParameterUtils.toPackageFlags(args[1]);
            if (packageName != null && packageName.equals(AnubisCore.getHostPkg())) {
                if (shouldHideHostFromGuest()) {
                    return null;
                }
                ApplicationInfo info = (ApplicationInfo) method.invoke(who, args);
                ensureHostGmsMetaData(info);
                return info;
            }
            if (GmsCore.isGoogleAppOrService(packageName)) {
                ApplicationInfo cloned = getClonedGoogleApplicationInfo(packageName, flags);
                if (cloned != null) {
                    return cloned;
                }
                ApplicationInfo info = (ApplicationInfo) method.invoke(who, args);
                ensureHostGmsMetaData(info);
                return info;
            }
            ApplicationInfo applicationInfo = AnubisCore.getBPackageManager().getApplicationInfo(packageName, flags, BActivityThread.getUserId());
            if (applicationInfo != null) {
                applicationInfo.flags |= ApplicationInfo.FLAG_EXTERNAL_STORAGE;
                return spoofApplicationInfoForGuest(applicationInfo);
            }
            if (shouldHideHostFromGuest()) {
                return null;
            }
            ApplicationInfo hostInfo = (ApplicationInfo) method.invoke(who, args);
            return spoofApplicationInfoForGuest(hostInfo);
        }
    }

    @ProxyMethod("getPackageInstaller")
    public static class GetPackageInstaller extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object installer = method.invoke(who, args);
            if (!shouldVirtualizePackageInstaller()) {
                return installer;
            }
            return VirtualPackageInstaller.wrap(installer);
        }
    }

    @ProxyMethod("queryContentProviders")
    public static class QueryContentProviders extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[2]);
            List<ProviderInfo> providers = AnubisCore.getBPackageManager().
                    queryContentProviders(BActivityThread.getAppProcessName(), BActivityThread.getBUid(), flags, BActivityThread.getUserId());
            return ParceledListSliceCompat.create(providers);
        }
    }

    @ProxyMethod("queryIntentReceivers")
    public static class QueryBroadcastReceivers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String type = MethodParameterUtils.getFirstParam(args, String.class);
            Integer flags = MethodParameterUtils.getFirstParam(args, Integer.class);
            List<ResolveInfo> resolves = AnubisCore.getBPackageManager().queryBroadcastReceivers(intent, flags, type, BActivityThread.getUserId());
            filterResolveInfoFromGuest(resolves);
            spoofResolveInfoListForGuest(resolves);

            // http://androidxref.com/7.0.0_r1/xref/frameworks/base/core/java/android/app/ApplicationPackageManager.java#872
            if (BuildCompat.isN()) {
                return ParceledListSliceCompat.create(resolves);
            }

            // http://androidxref.com/6.0.1_r10/xref/frameworks/base/core/java/android/app/ApplicationPackageManager.java#699
            return resolves;
        }
    }

    @ProxyMethod("resolveContentProvider")
    public static class ResolveContentProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String authority = (String) args[0];
            if (ProxyManifest.isProxy(authority) && shouldHideHostFromGuest()) {
                return null;
            }
            if (ProxyManifest.isProxy(authority)) {
                return method.invoke(who, args);
            }
            int flags = MethodParameterUtils.toInt(args[1]);
            ProviderInfo providerInfo = AnubisCore.getBPackageManager().resolveContentProvider(authority, flags, BActivityThread.getUserId());
            if (providerInfo == null) {
                return method.invoke(who, args);
            }
            return spoofProviderInfoForGuest(providerInfo);
        }
    }

    @ProxyMethod("canRequestPackageInstalls")
    public static class CanRequestPackageInstalls extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getPackagesForUid")
    public static class GetPackagesForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int uid = (Integer) args[0];
            int hostUid = AnubisCore.getHostUid();
            int callingPid = Binder.getCallingPid();
            String callerPkg = BProcessManagerService.get().getPackageNameByPid(callingPid);

            if (uid == hostUid
                    && BActivityThread.isThreadInit()) {
                String guest = BActivityThread.getAppPackageName();
                if (DfmStealthTier.javaPmSpoof(guest)) {
                    return new String[]{guest};
                }
            }

            if (callerPkg != null && !GmsCore.isGoogleAppOrService(callerPkg)) {
                if (uid == hostUid) {
                    return new String[]{callerPkg};
                }
            }

            if (uid == hostUid) {
                args[0] = BActivityThread.getBUid();
                uid = (int) args[0];
            }
            return AnubisCore.getBPackageManager().getPackagesForUid(uid);
        }
    }

    @ProxyMethod("getNameForUid")
    public static class GetNameForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int uid = (Integer) args[0];
            if (uid == AnubisCore.getHostUid()
                    && BActivityThread.isThreadInit()) {
                String guest = BActivityThread.getAppPackageName();
                if (DfmStealthTier.javaPmSpoof(guest)) {
                    return guest;
                }
            }
            if (uid == AnubisCore.getHostUid()) {
                args[0] = BActivityThread.getBUid();
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getInstallerPackageName")
    public static class GetInstallerPackageName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // fake google play
            return "com.android.vending";
        }
    }

    @ProxyMethod("getSharedLibraries")
    public static class GetSharedLibraries extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getComponentEnabledSetting")
    public static class getComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            String packageName = componentName.getPackageName();
            
            ApplicationInfo applicationInfo = AnubisCore.getBPackageManager().getApplicationInfo(packageName,0, BActivityThread.getUserId());
            
            if(applicationInfo != null){
                return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            }
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            throw new IllegalArgumentException();
        }
    }

  /** Cloned GSF/GMS/Play Store must use virtual dataDir so Finsky can write prefs/SQLite. */
    private static boolean shouldUseClonedGooglePaths() {
        String caller = BActivityThread.getAppPackageName();
        if (caller == null) {
            return false;
        }
        return GmsCore.isGoogleAppOrService(caller)
                || GmsCore.SETTINGS_PKG.equals(caller);
    }

    private static boolean shouldVirtualizePackageInstaller() {
        if (!BActivityThread.isThreadInit()) {
            return false;
        }
        String caller = BActivityThread.getAppPackageName();
        return caller != null && !caller.equals(AnubisCore.getHostPkg());
    }

    private static ApplicationInfo getClonedGoogleApplicationInfo(String packageName, int flags) {
        if (!shouldUseClonedGooglePaths()) {
            return null;
        }
        int userId = BActivityThread.getUserId();
        if (!AnubisCore.get().isInstalled(packageName, userId)) {
            return null;
        }
        ApplicationInfo virtual = AnubisCore.getBPackageManager()
                .getApplicationInfo(packageName, flags, userId);
        if (virtual != null) {
            ensureHostGmsMetaData(virtual);
        }
        return virtual;
    }

    private static PackageInfo getClonedGooglePackageInfo(String packageName, int flags) {
        if (!shouldUseClonedGooglePaths()) {
            return null;
        }
        int userId = BActivityThread.getUserId();
        if (!AnubisCore.get().isInstalled(packageName, userId)) {
            return null;
        }
        PackageInfo virtual = AnubisCore.getBPackageManager()
                .getPackageInfo(packageName, flags, userId);
        if (virtual != null && virtual.applicationInfo != null) {
            ensureHostGmsMetaData(virtual.applicationInfo);
        }
        return virtual;
    }

    private static PackageInfo spoofPackageInfoForGuest(PackageInfo info) {
        if (info == null || !DfmStealthTier.javaPmSpoof(info.packageName)) {
            return info;
        }
        return VirtualPathSpoof.spoofPackageInfoForGuest(info, BActivityThread.getUserId());
    }

    private static ApplicationInfo spoofApplicationInfoForGuest(ApplicationInfo info) {
        if (info == null || !DfmStealthTier.javaPmSpoof(info.packageName)) {
            return info;
        }
        return VirtualPathSpoof.spoofApplicationInfoForGuest(info, BActivityThread.getUserId());
    }

    private static void spoofPackageInfoListForGuest(List<PackageInfo> packages) {
        if (packages == null) {
            return;
        }
        int userId = BActivityThread.getUserId();
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo info = packages.get(i);
            if (info != null) {
                packages.set(i, spoofPackageInfoForGuest(info));
            }
        }
    }

    private static void spoofApplicationInfoListForGuest(List<ApplicationInfo> apps) {
        if (apps == null) {
            return;
        }
        int userId = BActivityThread.getUserId();
        for (int i = 0; i < apps.size(); i++) {
            ApplicationInfo info = apps.get(i);
            if (info != null && DfmStealthTier.javaPmSpoof(info.packageName)) {
                apps.set(i, VirtualPathSpoof.spoofApplicationInfoForGuest(info, userId));
            }
        }
    }

    private static ProviderInfo spoofProviderInfoForGuest(ProviderInfo info) {
        if (info == null || !DfmStealthTier.javaPmSpoof(info.packageName)) {
            return info;
        }
        return VirtualPathSpoof.spoofProviderInfoForGuest(info, BActivityThread.getUserId());
    }

    private static ActivityInfo spoofActivityInfoForGuest(ActivityInfo info) {
        if (info == null || !DfmStealthTier.javaPmSpoof(info.packageName)) {
            return info;
        }
        return VirtualPathSpoof.spoofActivityInfoForGuest(info, BActivityThread.getUserId());
    }

    private static ServiceInfo spoofServiceInfoForGuest(ServiceInfo info) {
        if (info == null || !DfmStealthTier.javaPmSpoof(info.packageName)) {
            return info;
        }
        return VirtualPathSpoof.spoofServiceInfoForGuest(info, BActivityThread.getUserId());
    }

    private static ResolveInfo spoofResolveInfoForGuest(ResolveInfo info) {
        if (info == null) {
            return null;
        }
        String pkg = info.activityInfo != null ? info.activityInfo.packageName
                : (info.serviceInfo != null ? info.serviceInfo.packageName : null);
        if (!DfmStealthTier.javaPmSpoof(pkg)) {
            return info;
        }
        return VirtualPathSpoof.spoofResolveInfoForGuest(info, BActivityThread.getUserId());
    }

    private static void spoofResolveInfoListForGuest(List<ResolveInfo> list) {
        if (list == null) {
            return;
        }
        int userId = BActivityThread.getUserId();
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            if (info != null) {
                list.set(i, spoofResolveInfoForGuest(info));
            }
        }
    }

    private static void filterResolveInfoFromGuest(List<ResolveInfo> list) {
        if (list == null || !shouldHideHostFromGuest()) {
            return;
        }
        Iterator<ResolveInfo> it = list.iterator();
        while (it.hasNext()) {
            ResolveInfo info = it.next();
            if (info == null) {
                it.remove();
                continue;
            }
            ActivityInfo ai = info.activityInfo;
            ServiceInfo si = info.serviceInfo;
            if (ai == null && si == null) {
                continue;
            }
            String pkg = ai != null ? ai.packageName : si.packageName;
            String cls = ai != null ? ai.name : si.name;
            ComponentName cn = new ComponentName(pkg, cls);
            if (VirtualPathSpoof.shouldHideLoaderPackageFromGuest(pkg)
                    || AppSystemEnv.shouldHideContainerPackage(pkg)
                    || ProxyManifest.isHostProxyComponent(cn)) {
                it.remove();
            }
        }
    }

    private static void filterHiddenPackagesFromGuest(List<PackageInfo> packages) {
        if (!shouldHideHostFromGuest()) {
            return;
        }
        Iterator<PackageInfo> it = packages.iterator();
        while (it.hasNext()) {
            PackageInfo info = it.next();
            if (info != null && AppSystemEnv.shouldHideContainerPackage(info.packageName)) {
                it.remove();
            }
        }
    }

    private static void filterHiddenFromGuest(List<ApplicationInfo> apps) {
        if (!shouldHideHostFromGuest()) {
            return;
        }
        Iterator<ApplicationInfo> it = apps.iterator();
        while (it.hasNext()) {
            ApplicationInfo info = it.next();
            if (info != null && AppSystemEnv.shouldHideContainerPackage(info.packageName)) {
                it.remove();
            }
        }
    }

    private static void mergeHostGooglePackages(List<PackageInfo> installedPackages, int flags) {
        if (!GmsCore.shouldUseHostGoogle(BActivityThread.getAppPackageName())) {
            return;
        }
        Set<String> present = new HashSet<>();
        for (PackageInfo info : installedPackages) {
            present.add(info.packageName);
        }
        PackageManager hostPm = AnubisCore.getContext().getPackageManager();
        for (String googlePkg : GmsCore.getAllGooglePackages()) {
            if (present.contains(googlePkg)) {
                continue;
            }
            try {
                installedPackages.add(hostPm.getPackageInfo(googlePkg, flags));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }

    private static void mergeHostGoogleApplications(List<ApplicationInfo> installedApplications, int flags) {
        if (!GmsCore.shouldUseHostGoogle(BActivityThread.getAppPackageName())) {
            return;
        }
        Set<String> present = new HashSet<>();
        for (ApplicationInfo info : installedApplications) {
            present.add(info.packageName);
        }
        PackageManager hostPm = AnubisCore.getContext().getPackageManager();
        for (String googlePkg : GmsCore.getAllGooglePackages()) {
            if (present.contains(googlePkg)) {
                continue;
            }
            try {
                installedApplications.add(hostPm.getApplicationInfo(googlePkg, flags));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }
}
