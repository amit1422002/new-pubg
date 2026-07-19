package com.anubis.loader.fake.service;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.fake.provider.FileProviderHandler;
import com.anubis.loader.utils.ComponentUtils;
import com.anubis.loader.utils.MethodParameterUtils;
import com.anubis.loader.gms.MicroGLoginRedirect;
import com.anubis.loader.gms.PlayStoreAuthHelper;
import com.anubis.loader.utils.Slog;
import com.anubis.loader.utils.compat.BuildCompat;
import com.anubis.loader.utils.compat.StartActivityCompat;
import com.anubis.loader.proxy.ProxyManifest;
import com.anubis.loader.utils.compat.ParceledListSliceCompat;

import static android.content.pm.PackageManager.GET_META_DATA;


public class ActivityManagerCommonProxy {
    public static final String TAG = "CommonStub";

    @ProxyMethod("startActivity")
    public static class StartActivity extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            Intent intent = getIntent(args);
            Slog.d(TAG, "Hook in : " + intent);
            assert intent != null;

            if (maybeRedirectPlayStoreAuth(intent)) {
                // intent component updated below via resolveActivity
            }
            if (intent.getParcelableExtra("_B_|_target_") != null) {
                return method.invoke(who, args);
            }
            if (ComponentUtils.isRequestInstall(intent)) {
                File file = FileProviderHandler.convertFile(BActivityThread.getApplication(), intent.getData());
                
                
                if (file != null && file.exists()) {
                    try {
                        PackageInfo packageInfo = AnubisCore.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);
                        if (packageInfo != null) {
                            String packageName = packageInfo.packageName;
                            String hostPackageName = AnubisCore.getHostPkg();
                            if (packageName.equals(hostPackageName)) {
                                Slog.w(TAG, "Blocked attempt to install Anubis app from within Anubis: " + packageName);
                                
                                return 0;
                            }
                        }
                    } catch (Exception e) {
                        Slog.w(TAG, "Could not verify if this is Anubis app: " + e.getMessage());
                    }
                }
                
                if (AnubisCore.get().requestInstallPackage(file, BActivityThread.getUserId())) {
                    return 0;
                }
                intent.setData(FileProviderHandler.convertFileUri(BActivityThread.getApplication(), intent.getData()));
                return method.invoke(who, args);
            }
            String dataString = intent.getDataString();
            if (dataString != null && dataString.equals("package:" + BActivityThread.getAppPackageName())) {
                intent.setData(Uri.parse("package:" + AnubisCore.getHostPkg()));
            }

            ResolveInfo resolveInfo = AnubisCore.getBPackageManager().resolveActivity(
                    intent,
                    GET_META_DATA,
                    StartActivityCompat.getResolvedType(args),
                    BActivityThread.getUserId());
            if (resolveInfo == null) {
                String origPackage = intent.getPackage();
                if (intent.getPackage() == null && intent.getComponent() == null) {
                    intent.setPackage(BActivityThread.getAppPackageName());
                } else {
                    origPackage = intent.getPackage();
                }
                resolveInfo = AnubisCore.getBPackageManager().resolveActivity(
                        intent,
                        GET_META_DATA,
                        StartActivityCompat.getResolvedType(args),
                        BActivityThread.getUserId());
                if (resolveInfo == null) {
                    intent.setPackage(origPackage);
                    return method.invoke(who, args);
                }
            }


            intent.setExtrasClassLoader(who.getClass().getClassLoader());
            intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            AnubisCore.getBActivityManager().startActivityAms(BActivityThread.getUserId(),
                    StartActivityCompat.getIntent(args),
                    StartActivityCompat.getResolvedType(args),
                    StartActivityCompat.getResultTo(args),
                    StartActivityCompat.getResultWho(args),
                    StartActivityCompat.getRequestCode(args),
                    StartActivityCompat.getFlags(args),
                    StartActivityCompat.getOptions(args));
            return 0;
        }

        private Intent getIntent(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 3;
            } else {
                index = 2;
            }
            if (args[index] instanceof Intent) {
                return (Intent) args[index];
            }
            for (Object arg : args) {
                if (arg instanceof Intent) {
                    return (Intent) arg;
                }
            }
            return null;
        }

        private boolean maybeRedirectPlayStoreAuth(Intent intent) {
            boolean play = PlayStoreAuthHelper.redirectIfAuthenticated(intent);
            boolean microG = MicroGLoginRedirect.redirectIfNeeded(intent);
            return play || microG;
        }
    }

    @ProxyMethod("startActivities")
    public static class StartActivities extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int index = getIntents();
            Intent[] intents = (Intent[]) args[index++];
            String[] resolvedTypes = (String[]) args[index++];
            IBinder resultTo = (IBinder) args[index++];
            Bundle options = (Bundle) args[index];
            
            if (!ComponentUtils.isSelf(intents)) {
                return method.invoke(who, args);
            }

            for (Intent intent : intents) {
                intent.setExtrasClassLoader(who.getClass().getClassLoader());
            }
            return AnubisCore.getBActivityManager().startActivities(BActivityThread.getUserId(),
                    intents, resolvedTypes, resultTo, options);
        }

        public int getIntents() {
            if (BuildCompat.isR()) {
                return 3;
            }
            return 2;
        }
    }

    @ProxyMethod("startIntentSenderForResult")
    public static class StartIntentSenderForResult extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("activityResumed")
    public static class ActivityResumed extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            AnubisCore.getBActivityManager().onActivityResumed((IBinder) args[0]);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("activityDestroyed")
    public static class ActivityDestroyed extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            AnubisCore.getBActivityManager().onActivityDestroyed((IBinder) args[0]);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("finishActivity")
    public static class FinishActivity extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            AnubisCore.getBActivityManager().onFinishActivity((IBinder) args[0]);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAppTasks")
    public static class GetAppTasks extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getCallingPackage")
    public static class getCallingPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AnubisCore.getBActivityManager().getCallingPackage((IBinder) args[0], BActivityThread.getUserId());
        }
    }

    @ProxyMethod("getCallingActivity")
    public static class getCallingActivity extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AnubisCore.getBActivityManager().getCallingActivity((IBinder) args[0], BActivityThread.getUserId());
        }
    }

    @ProxyMethod("getRecentTasks")
    public static class GetRecentTasks extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object result = method.invoke(who, args);
            if (!BActivityThread.isThreadInit() || !isGuestContext()) {
                return result;
            }
            return filterRecentTasks(result);
        }
    }

    private static boolean isGuestContext() {
        String pkg = BActivityThread.getAppPackageName();
        String host = AnubisCore.getHostPkg();
        return pkg != null && host != null && !pkg.equals(host);
    }

    @SuppressWarnings("unchecked")
    private static Object filterRecentTasks(Object result) {
        if (result == null) {
            return null;
        }
        List<ActivityManager.RecentTaskInfo> tasks;
        if (ParceledListSliceCompat.isParceledListSlice(result)) {
            tasks = ParceledListSliceCompat.getList(result);
        } else if (result instanceof List) {
            tasks = (List<ActivityManager.RecentTaskInfo>) result;
        } else {
            return result;
        }
        if (tasks == null || tasks.isEmpty()) {
            return result;
        }
        String guestPkg = BActivityThread.getAppPackageName();
        String hostPkg = AnubisCore.getHostPkg();
        List<ActivityManager.RecentTaskInfo> filtered = new ArrayList<>();
        for (ActivityManager.RecentTaskInfo task : tasks) {
            if (task == null || task.baseIntent == null) {
                continue;
            }
            ComponentName component = task.baseIntent.getComponent();
            if (component != null) {
                if (hostPkg != null && hostPkg.equals(component.getPackageName())) {
                    continue;
                }
                if (ProxyManifest.isHostProxyComponent(component)) {
                    continue;
                }
            }
            if (task.baseIntent.getPackage() != null
                    && hostPkg != null
                    && hostPkg.equals(task.baseIntent.getPackage())) {
                continue;
            }
            if (guestPkg != null
                    && component != null
                    && guestPkg.equals(component.getPackageName())) {
                filtered.add(task);
            } else if (component == null && guestPkg != null
                    && guestPkg.equals(task.baseIntent.getPackage())) {
                filtered.add(task);
            }
        }
        if (ParceledListSliceCompat.isParceledListSlice(result)) {
            return ParceledListSliceCompat.create(filtered);
        }
        return filtered;
    }
}
