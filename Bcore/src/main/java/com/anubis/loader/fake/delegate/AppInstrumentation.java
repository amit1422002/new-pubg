package com.anubis.loader.fake.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PersistableBundle;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import black.android.app.BRActivity;
import black.android.app.BRActivityThread;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.fake.hook.HookManager;
import com.anubis.loader.fake.hook.IInjectHook;
import com.anubis.loader.fake.service.HCallbackProxy;
import com.anubis.loader.utils.DfmStealthTier;
import com.anubis.loader.utils.GuestPathContext;
import com.anubis.loader.utils.HackAppUtils;
import com.anubis.loader.utils.VirtualPathSpoof;
import com.anubis.loader.utils.compat.ActivityCompat;
import com.anubis.loader.utils.compat.ActivityManagerCompat;
import com.anubis.loader.utils.compat.ContextCompat;

public final class AppInstrumentation extends BaseInstrumentationDelegate implements IInjectHook {

    private static final String TAG = AppInstrumentation.class.getSimpleName();

    private static AppInstrumentation sAppInstrumentation;
    private static final Set<Integer> sPreparedActivities =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static AppInstrumentation get() {
        if (sAppInstrumentation == null) {
            synchronized (AppInstrumentation.class) {
                if (sAppInstrumentation == null) {
                    sAppInstrumentation = new AppInstrumentation();
                }
            }
        }
        return sAppInstrumentation;
    }

    public AppInstrumentation() {
    }

    @Override
    public void injectHook() {
        try {
            Instrumentation mInstrumentation = getCurrInstrumentation();
            if (mInstrumentation == this || checkInstrumentation(mInstrumentation))
                return;
            mBaseInstrumentation = (Instrumentation) mInstrumentation;
            BRActivityThread.get(AnubisCore.mainThread())._set_mInstrumentation(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instrumentation getCurrInstrumentation() {
        Object currentActivityThread = AnubisCore.mainThread();
        return BRActivityThread.get(currentActivityThread).mInstrumentation();
    }

    @Override
    public boolean isBadEnv() {
        return !checkInstrumentation(getCurrInstrumentation());
    }

    private boolean checkInstrumentation(Instrumentation instrumentation) {
        if (instrumentation instanceof AppInstrumentation) {
            return true;
        }
        Class<?> clazz = instrumentation.getClass();
        if (Instrumentation.class.equals(clazz)) {
            return false;
        }
        do {
            assert clazz != null;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Instrumentation.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Object obj = field.get(instrumentation);
                        if ((obj instanceof AppInstrumentation)) {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (!Instrumentation.class.equals(clazz));
        return false;
    }

    private void checkHCallback() {
        HookManager.get().checkEnv(HCallbackProxy.class);
    }

    private void prepareActivityOnce(Activity activity) {
        int key = System.identityHashCode(activity);
        if (!sPreparedActivities.add(key)) {
            return;
        }
        HackAppUtils.enableQQLogOutput(activity.getPackageName(), activity.getClassLoader());
        ActivityInfo info = BRActivity.get(activity).mActivityInfo();
        String guestPkg = BActivityThread.getAppPackageName();
        if (info != null && info.applicationInfo != null) {
            int userId = BActivityThread.getUserId();
            if (guestPkg != null && DfmStealthTier.javaPmSpoof(guestPkg)) {
                VirtualPathSpoof.ensureLoadedApkInternalInfo(info.applicationInfo, userId);
            } else if (guestPkg == null || !VirtualPathSpoof.isStealthAcPackage(guestPkg)) {
                info.applicationInfo = VirtualPathSpoof.spoofApplicationInfoRuntimeVisible(
                        info.applicationInfo, userId);
                VirtualPathSpoof.ensureRealApkPaths(info.applicationInfo, userId);
            }
        }
        if (info != null && info.theme != 0) {
            activity.setTheme(info.theme);
        }
        if (guestPkg != null && DfmStealthTier.javaPmSpoof(guestPkg)) {
            ContextCompat.fixGuestIdentity(activity);
            GuestPathContext.wrapIfNeeded(activity, guestPkg);
        } else {
            ContextCompat.fix(activity);
        }
        ActivityCompat.fix(activity);
        if (info != null) {
            ActivityManagerCompat.setActivityOrientation(activity, info.screenOrientation);
        }
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ContextCompat.fix(context);
        return super.newApplication(cl, className, context);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        checkHCallback();
        prepareActivityOnce(activity);
        super.callActivityOnCreate(activity, icicle, persistentState);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        checkHCallback();
        prepareActivityOnce(activity);
        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        checkHCallback();
        super.callApplicationOnCreate(app);
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newActivity(cl, className, intent);
    }
}
