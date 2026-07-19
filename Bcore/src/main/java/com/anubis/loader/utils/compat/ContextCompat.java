package com.anubis.loader.utils.compat;

import android.content.Context;
import android.content.ContextWrapper;

import black.android.app.BRContextImpl;
import black.android.app.BRContextImplKitkat;
import black.android.content.AttributionSourceStateContext;
import black.android.content.BRAttributionSource;
import black.android.content.BRAttributionSourceState;
import black.android.content.BRContentResolver;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.utils.DfmStealthTier;
import com.anubis.loader.utils.VirtualPathSpoof;
import top.niunaijun.blackreflection.BlackReflection;

/**
 * Framework ContextImpl must keep the host package + kernel UID — system_server validates
 * AttributionSource against the real process identity. Guest package name is exposed only
 * through {@link com.anubis.loader.utils.GuestPathContext} wrappers.
 */
public class ContextCompat {
    public static final String TAG = "ContextCompat";

    public static void fixAttributionSourceState(Object obj, int uid) {
        fixAttributionSourceState(obj, AnubisCore.getHostPkg(), uid);
    }

    public static void fixAttributionSourceState(Object obj, String packageName, int uid) {
        Object mAttributionSourceState;
        if (obj != null && BRAttributionSource.get(obj)._check_mAttributionSourceState() != null) {
            mAttributionSourceState = BRAttributionSource.get(obj).mAttributionSourceState();

            AttributionSourceStateContext attributionSourceStateContext = BRAttributionSourceState.get(mAttributionSourceState);
            attributionSourceStateContext._set_packageName(packageName);
            attributionSourceStateContext._set_uid(uid);
            fixAttributionSourceState(BRAttributionSource.get(obj).getNext(), packageName, uid);
        }
    }

    public static void fixAttributionSourceState(Object obj, int uid, int pid) {
        if (obj != null && BRAttributionSource.get(obj)._check_mAttributionSourceState() != null) {
            return;
        }
        AttributionSourceStateContext attributionSourceStateContext = (AttributionSourceStateContext) BlackReflection.create(AttributionSourceStateContext.class, BRAttributionSource.get(obj).mAttributionSourceState(), false);
        attributionSourceStateContext._set_packageName(AnubisCore.getHostPkg());
        attributionSourceStateContext._set_uid(Integer.valueOf(uid));
        attributionSourceStateContext._set_pid(Integer.valueOf(pid));
        fixAttributionSourceState(BRAttributionSource.get(obj).getNext(), uid, pid);
    }

    private static Context unwrap(Context context) {
        int deep = 0;
        while (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
            deep++;
            if (deep >= 10) {
                return null;
            }
        }
        return context;
    }

    /**
     * Stealth guests — ContextImpl keeps host package + kernel UID so IME / system_server
     * binders work. Guest package is exposed via {@link com.anubis.loader.utils.GuestPathContext}.
     */
    public static void fixGuestIdentity(Context context) {
        if (context == null || !shouldApplyGuestIdentity()) {
            return;
        }
        fix(context);
    }

    private static boolean shouldApplyGuestIdentity() {
        String guest = BActivityThread.getAppPackageName();
        return guest != null
                && DfmStealthTier.javaPmSpoof(guest)
                && BActivityThread.currentActivityThread().isInit();
    }

    public static void fix(Context context) {
        try {
            context = unwrap(context);
            if (context == null) {
                return;
            }
            BRContextImpl.get(context)._set_mPackageManager(null);
            try {
                context.getPackageManager();
            } catch (Throwable ignored) {
            }

            BRContextImpl.get(context)._set_mBasePackageName(AnubisCore.getHostPkg());
            BRContextImplKitkat.get(context)._set_mOpPackageName(AnubisCore.getHostPkg());
            BRContentResolver.get(context.getContentResolver())._set_mPackageName(AnubisCore.getHostPkg());

            if (BuildCompat.isS()) {
                fixAttributionSourceState(
                        BRContextImpl.get(context).getAttributionSource(),
                        AnubisCore.getHostPkg(),
                        AnubisCore.getHostUid());
            }
        } catch (Exception ignored) {
        }
    }
}
