package com.anubis.loader.utils;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.device.DeviceSpoofManager;

/**
 * When active, suppresses logcat and other traces inside cloned guest processes.
 */
public final class StealthMode {

    private StealthMode() {
    }

    public static boolean isActive() {
        try {
            AnubisCore core = AnubisCore.get();
            return core.isAnubisProcess() || core.isServerProcess();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Guest / AC-facing processes must not emit loader or path traces to logcat. */
    public static boolean shouldSuppressLogcat() {
        if (DeviceSpoofManager.shouldSpoofCurrentProcess()) {
            return true;
        }
        try {
            String pkg = BActivityThread.getAppPackageName();
            if (pkg != null && VirtualPathSpoof.isStealthAcPackage(pkg)
                    && BActivityThread.isThreadInit()
                    && BActivityThread.currentActivityThread().isInit()) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
