package com.anubis.loader.core;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.utils.AnogsPatchLogger;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.env.GamePackages;
import com.anubis.loader.core.env.StealthConstants;
import com.anubis.loader.utils.BuildStealthHelper;
import com.anubis.loader.utils.StealthMode;
import com.anubis.loader.utils.VirtualPathSpoof;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Extended patched NativeCore with more anti-detection shims to cover
 * additional probes observed in the logs (proc/self/root, profile files, dev/urandom, etc.).
 *
 * Notes:
 *  - This uses simple path-to-path redirections via addIORule() implemented in native layer.
 *  - For profile files that are system-owned (permission denied), we create a benign copy
 *    under the Anubis app's private storage and redirect the game's access to it.
 *
 * Keep expanding addIORule() targets when you find new probe paths in logs.
 */
public class NativeCore {
    public static final String TAG = "NativeCore";
    private static final String DELTA_FORCE_GARENA = "com.garena.game.df";
    private static final AtomicBoolean sNukeDispatched = new AtomicBoolean(false);

    static {
        System.loadLibrary(StealthConstants.NATIVE_CORE_LIB);
    }
 

    public static void init(int apiLevel) {
        // Hide virtual traces
        
        // Nuke anti-cheat
//         try {
//            nukeTargetLibrary("libanogs", 
//     NUKE_L1_FILE_LOCK |
//     // NUKE_L2_FD_EXHAUST |
//     // NUKE_L3_MLOCK |
//      NUKE_L4_ELF_DESTROY |
//      NUKE_L5_SHDR_HIDE |
//     // NUKE_L6_GUARD_PAGE //|
//     NUKE_L7_LINKMAP |
//      NUKE_L8_ANTIDEBUG |
//      NUKE_L9_SIGNALS |
//     // NUKE_L10_SECCOMP |
//      NUKE_L11_WATCHDOG
// );
           // nukeTargetLibrary("libanort", NUKE_L4_ELF_DESTROY);
        //     Log.i(TAG, "NUKE sent in init");
        // } catch (Throwable t) {
        //     Log.e(TAG, "NUKE init failed", t);
        // }


 
        


        initNative(apiLevel,
                com.anubis.jnihook.jni.JniHook.class,
                com.anubis.jnihook.MethodUtils.class);


                applyGuestHostDataRedirect();
    }

    /**
     * BGMI UE4 guest: ELF-destroy mapped libanogs via {@link #nukeTargetLibrary(String, int)}.
     * Runs once per process; native waits for RX mapping then applies L4+L5.
     */
    public static void dispatchAcNukeForGuest(String packageName, String processName) {
        if (packageName == null || !GamePackages.isBgmi(packageName)) {
            return;
        }
        if (!sNukeDispatched.compareAndSet(false, true)) {
            return;
        }
        Log.i(TAG, "dispatchAcNukeForGuest: libanogs ELF destroy pkg=" + packageName
                + " proc=" + processName + " pid=" + Process.myPid());
        AnogsPatchLogger.setProcessLabel(packageName != null ? packageName : "bgmi");
        AnogsPatchLogger.onElfNukeDispatched(packageName);
        nukeTargetLibrary("libanogs", NUKE_LAYERS_BGMI_ANOGS_ELF);
    }

    /**
     * Redirect legacy host paths to the current guest's data dir (per package), so Global and
     * BGMI do not share com.tencent.ig data when both are cloned.
     */
    private static void applyGuestHostDataRedirect() {
        try {
            String guestPkg = BActivityThread.getAppPackageName();
            if (guestPkg == null || guestPkg.isEmpty()) return;
            Context ctx = AnubisCore.getContext();
            if (ctx == null) return;
            String hostPkg = AnubisCore.getHostPkg();
            String guestData =
                    com.anubis.loader.core.env.BEnvironment
                            .getDataDir(guestPkg, BActivityThread.getUserId())
                            .getAbsolutePath();
            if (guestData.isEmpty()) return;
            addIORule("/data/user/0/" + hostPkg, guestData);
            addIORule("/data/data/" + hostPkg, guestData);
            // Older host package names still referenced in native probes.
            if (!"com.pubgm".equals(hostPkg)) {
                addIORule("/data/user/0/com.pubgm", guestData);
                addIORule("/data/data/com.pubgm", guestData);
            }
        } catch (Exception ignored) {
        }
    }

    public static native void setSuppressNativeLog(boolean suppress);

    public static native void setLogScrubConfig(boolean enabled, String hostPkg, String guestPkg);

    private static native void initNative(int apiLevel, Class<?> jniHook, Class<?> methodUtils);

    public static native void enableIO();

    public static native void addIORule(String targetPath, String relocatePath);

    public static native void hideXposed();

    public static native boolean disableHiddenApi();

    public static native void init_seccomp();

    /** README layer 1 — file {@code chmod 000} (after optional hold-read thread). */
    public static final int NUKE_L1_FILE_LOCK = 1 << 0;
    public static final int NUKE_L2_FD_EXHAUST = 1 << 1;
    public static final int NUKE_L3_MLOCK = 1 << 2;
    public static final int NUKE_L4_ELF_DESTROY = 1 << 3;
    public static final int NUKE_L5_SHDR_HIDE = 1 << 4;

    /** BGMI guest libanogs: {@code block-anogs.hpp} L4 zero ELF header + L5 hide section headers. */
    public static final int NUKE_LAYERS_BGMI_ANOGS_ELF = NUKE_L4_ELF_DESTROY | NUKE_L5_SHDR_HIDE;
    public static final int NUKE_L6_GUARD_PAGE = 1 << 5;
    /** Enumerate only (unlink disabled in native — unsafe). */
    public static final int NUKE_L7_LINKMAP = 1 << 6;
    public static final int NUKE_L8_ANTIDEBUG = 1 << 7;
    public static final int NUKE_L9_SIGNALS = 1 << 8;
    public static final int NUKE_L10_SECCOMP = 1 << 9;
    public static final int NUKE_L11_WATCHDOG = 1 << 10;
    public static final int NUKE_HOLD_FILE_READ = 1 << 11;
    public static final int NUKE_LAYERS_README_11 =
            NUKE_L1_FILE_LOCK | NUKE_L2_FD_EXHAUST | NUKE_L3_MLOCK | NUKE_L4_ELF_DESTROY | NUKE_L5_SHDR_HIDE
                    | NUKE_L6_GUARD_PAGE | NUKE_L7_LINKMAP | NUKE_L8_ANTIDEBUG | NUKE_L9_SIGNALS | NUKE_L10_SECCOMP
                    | NUKE_L11_WATCHDOG;
    public static final int NUKE_LAYERS_ALL = NUKE_LAYERS_README_11 | NUKE_HOLD_FILE_READ;

    /**
     * {@link #NUKE_L2_FD_EXHAUST} + {@link #NUKE_L10_SECCOMP} — block almost all files/syscalls in the process;
     * fine for README demo, lethal for a live game / ART.
     */
    public static final int NUKE_LAYERS_FATAL_FOR_GAME = NUKE_L2_FD_EXHAUST | NUKE_L10_SECCOMP;

    /**
     * All 11 readme layers minus FD exhaustion and BPF seccomp so the guest process keeps normal file I/O.
     * Prefer this from virtualized game bootstrap; extend with caution.
     */
    public static final int NUKE_LAYERS_GUEST_STABLE = NUKE_LAYERS_README_11 & ~NUKE_LAYERS_FATAL_FOR_GAME;

    /**
     * Stable guest mask + optional hold-read thread.
     */
    public static final int NUKE_LAYERS_GUEST_STABLE_WITH_HOLD = NUKE_LAYERS_GUEST_STABLE | NUKE_HOLD_FILE_READ;

    /**
     * Subset that tended to SIGABRT in virtualized Tencent guests: stripping ELF/{@code mmap} PROT_NONE guards,
     * {@code chmod} on {@code /data/app/…/lib/}, and anti-debug prctl/ptrace. {@link #NUKE_L9_SIGNALS} kept on user request.
     */
    public static final int NUKE_LAYERS_ABORT_HEAVY_IN_GUEST =
            NUKE_L1_FILE_LOCK
                    | NUKE_L4_ELF_DESTROY
                    | NUKE_L5_SHDR_HIDE
                    | NUKE_L6_GUARD_PAGE
                    | NUKE_L8_ANTIDEBUG;

    /** Live guest: {@code mlock}, L7, L9 (signals), L11 watchdog, hold FD; still omits L1/L4-L6/L8 and fatal L2/L10. */
    public static final int NUKE_LAYERS_GUEST_ULTRA_SOFT =
            NUKE_LAYERS_GUEST_STABLE_WITH_HOLD & ~NUKE_LAYERS_ABORT_HEAVY_IN_GUEST;

    /**
     * Same as {@link #NUKE_LAYERS_GUEST_ULTRA_SOFT} but without {@link #NUKE_L11_WATCHDOG}: no background thread
     * re-applying layers every second (often implicated in delayed / long-session instability).
     */
    public static final int NUKE_LAYERS_GUEST_ULTRA_SOFT_NO_WATCHDOG =
            NUKE_LAYERS_GUEST_ULTRA_SOFT & ~NUKE_L11_WATCHDOG;

    /**
     * Long sessions: {@link #NUKE_LAYERS_GUEST_ULTRA_SOFT} minus periodic watchdog (L11) and minus hold-read (keeps an
     * FD open indefinitely). Prefer this when the guest crashes after many minutes with ULTRA_SOFT.
     */
    public static final int NUKE_LAYERS_GUEST_LONG_PLAY =
            NUKE_LAYERS_GUEST_ULTRA_SOFT & ~(NUKE_L11_WATCHDOG | NUKE_HOLD_FILE_READ);

    /**
     * Same as {@link #NUKE_LAYERS_GUEST_LONG_PLAY} but without {@link #NUKE_L9_SIGNALS}. Custom SIGTRAP/SIGILL
     * handlers can interact badly with UE / libsigchain during long play; prefer this when inject causes delayed exits.
     */
    public static final int NUKE_LAYERS_GUEST_LONG_PLAY_NO_L9 =
            NUKE_LAYERS_GUEST_LONG_PLAY & ~NUKE_L9_SIGNALS;

    /**
     * Runs {@code nukeLibrary} with every layer bit set (including {@link #NUKE_HOLD_FILE_READ}).
     * For manual control use {@link #nukeTargetLibrary(String, int)} with a mask.
     */
    @Keep
    public static void nukeTargetLibrary(String libNameSubstring) {
        nukeTargetLibrary(libNameSubstring, NUKE_LAYERS_ALL);
    }

    /**
     * Same as README {@code nukeLibrary}: bitmask enables which of the 11 (+ optional hold-read) layers run.
     * <p><b>Warning:</b> one-way; seccomp / FD exhaustion are process-wide. Layer 7 does not unlink (SIGSEGV risk).
     */
    @Keep
    public static native void nukeTargetLibrary(String libNameSubstring, int layerMask);

    /** Dobby inline hook THUNDER @ libanogs+0x51FA80 (guest process only). */
    public static native void installAnogsThunderHook();

    /** True when RET0 + THUNDER inline hooks are both installed on mapped libanogs. */
    public static native boolean areAnogsHooksInstalled();

    private static final long ANOGS_ELF_DESTROY_DELAY_MS = 5000L;

    /**
     * BGMI guest anogs hooks + ELF destroy — disabled.
     */
    public static void scheduleAnogsHookWithElfFallback(String packageName, String processName) {
        Log.i(TAG, "scheduleAnogsHookWithElfFallback: DISABLED (hooks+ELF off) proc="
                + processName + " pid=" + Process.myPid());
    }

    /** Memfd-load ELF bytes in guest without host BLAZEBOX / base.apk maps. */
    public static native boolean memfdLoadElf(byte[] elf);

    /** Memfd-load guest login hook only — does not start ESP socket servers. */
    public static native boolean memfdLoadHookElf(byte[] elf);

    /** Raw /proc maps lookup (bypasses scrubbed Java maps file). 0 if not mapped. */
    public static native long getMappedLibraryBase(String libNameSubstring);

    /** Real /proc/self/maps content — bypasses guest redirect (scrubber input only). */
    public static native String readRealProcSelfMaps();

    /** Patch mapped .so at RVA using mprotect (works on RX pages; /proc/self/mem often cannot). */
    public static native boolean patchMappedLibraryRva(String libName, long rva,
            byte[] patch, byte[] expected);

    /** Patch already-mapped VA using mprotect; skips write if bytes already match. */
    public static native boolean patchAbsoluteAddress(long address, byte[] patch, byte[] expected);

    /**
     * Bind FarlightEspNative JNI to symbols in the last memfd-loaded reader ELF.
     * Must run after {@link #memfdLoadElf(byte[])} succeeds.
     */
    public static native boolean registerFarlightEspNatives(Class<?> bridgeClass);

    /** Poll ESP frames via cached memfd reader handle (NativeCore JNI). */
    public static native float[] pollFarlightEspFrames();

    /** [publishedCount, publishCalls, pollFnBound, memfdHandle] */
    public static native int[] getFarlightEspBridgeStatus();

    public static native boolean registerDeltaForceEspNatives(Class<?> bridgeClass);

    /** Start abstract UNIX socket server before memfd reader connects. */
    public static native boolean ensureDeltaForceEspServer();

    /** Ensure Delta Force ESP reader/socket bridge is running after memfd load. */
    public static void startDeltaForceEspReader() {
        ensureDeltaForceEspServer();
    }

    public static native void setDeltaForceEspScreen(int width, int height);

    public static native void setDeltaForceEspSmallCrosshair(boolean enabled);

    /** 15 floats: enabled, teamCheck, visibleCheck, aimWhenZooming, aimWhenFiringOnly,
     *  ignoreLaying, useBoneTargeting, freeBoneTargeting, prioritizeClosest,
     *  maxDistance, smoothness, fov, targetHeight, verticalAdjust, aimBone */
    public static native void setDeltaForceEspAimbotConfig(float[] config);

    public static native void setDeltaForceEspBulletTrackConfig(float[] config);

    public static native float[] pollDeltaForceEspFrames();

    public static native int[] getDeltaForceEspBridgeStatus();

    @Keep
    public static int getCallingUid(int origCallingUid) {
        if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
            return origCallingUid;
        if (origCallingUid > Process.LAST_APPLICATION_UID)
            return origCallingUid;

        if (origCallingUid == AnubisCore.getHostUid()) {
            String appPackageName = BActivityThread.getAppPackageName();

            if ("com.google.android.webview".equals(appPackageName)) {
                return Process.myUid();
            }

            try {
                int callingBUid = BActivityThread.getCallingBUid();
                if (callingBUid > 0 && callingBUid < Process.LAST_APPLICATION_UID) {
                    return callingBUid;
                }
            } catch (Exception e) {
                if (!StealthMode.shouldSuppressLogcat()) {
                    Log.w(TAG, "Error getting calling BUid: " + e.getMessage());
                }
            }

            return AnubisCore.getHostUid();
        }
        return origCallingUid;
    }

    @Keep
    public static String reversePath(String path) {
        if (path == null || IOCore.isInternalRedirectActive()
                || !VirtualPathSpoof.shouldSpoofForGuest()) {
            return path;
        }
        String out = IOCore.get().reversePath(path);
        if (out != null && VirtualPathSpoof.containsLeak(out)) {
            String scrubbed = VirtualPathSpoof.scrubProcLine(out);
            if (scrubbed != null) {
                out = scrubbed;
            }
        }
        return out;
    }

    @Keep
    public static File reversePath(File path) {
        return IOCore.get().reversePath(path);
    }

    public static void setGuestPackageForStealth(String packageName) {
        VirtualPathSpoof.setGuestPackageBound(packageName);
        boolean suppress = packageName != null && VirtualPathSpoof.isStealthAcPackage(packageName);
        try {
            setSuppressNativeLog(suppress);
            if (suppress && !isFarlightGuest()) {
                String host = AnubisCore.getHostPkg();
                setLogScrubConfig(true, host != null ? host : "", packageName);
            } else {
                setLogScrubConfig(false, "", "");
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setGuestProcSpoofUid(int uid) {
        VirtualPathSpoof.setProcSpoofUid(uid);
    }

    public static void setGuestProcessComm(String packageName) {
        VirtualPathSpoof.setGuestProcessComm(packageName);
    }

    public static void refreshStealthProcNow() {
    }

    public static void refreshStealthProcLight() {
    }

    private static volatile long sLastLightProcRefreshMs;

    /** Proc scrub reads/writes full /proc — never run on the main thread (UE4 ANR on loading screen). */
    public static void refreshStealthProcNowAsync() {
        android.os.Handler h = AnubisCore.get().getHandler();
        if (h != null) {
            h.post(NativeCore::refreshStealthProcNow);
        } else {
            new Thread(NativeCore::refreshStealthProcNow, "stealth-proc").start();
        }
    }

    public static void refreshStealthProcLightAsync() {
        long now = System.currentTimeMillis();
        if (now - sLastLightProcRefreshMs < 30_000L) {
            return;
        }
        sLastLightProcRefreshMs = now;
        android.os.Handler h = AnubisCore.get().getHandler();
        if (h != null) {
            h.post(NativeCore::refreshStealthProcLight);
        } else {
            new Thread(NativeCore::refreshStealthProcLight, "stealth-proc-light").start();
        }
    }

    /**
     * Seccomp disabled for stealth guests — traps openat and causes SIGSYS on Android 16 / UE4.
     * Proc scrubbing uses fake /proc files via Java IO redirect + periodic refresh instead.
     */
    public static void initSeccompForStealthIfNeeded() {
        // intentionally no-op
    }

    private static volatile boolean sStealthProcScheduleStarted;

    public static void hideSelfLoaderFromAc() {
        // ProcStealthHelper disabled — no runtime /proc scrub.
    }

    private static boolean isFarlightGuest() {
        String pkg = BActivityThread.getAppPackageName();
        return pkg != null && (GamePackages.isFarlight(pkg)
                || "com.farlightgames.farlight84.gray".equals(pkg));
    }

    private static void scheduleMapsRefresh(android.os.Handler h, long delayMs, boolean light) {
        h.postDelayed(() -> {
            if (light) {
                refreshStealthProcLight();
            } else {
                refreshStealthProcNow();
            }
        }, delayMs);
    }

    private static void schedulePeriodicMapsRefresh(
            android.os.Handler h, long startDelayMs, long intervalMs, int count) {
        final int[] remaining = {count};
        final Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            if (remaining[0]-- <= 0) {
                return;
            }
            if (remaining[0] % 5 == 0) {
                refreshStealthProcNow();
            } else {
                refreshStealthProcLight();
            }
            if (remaining[0] > 0) {
                h.postDelayed(tick[0], intervalMs);
            }
        };
        h.postDelayed(tick[0], startDelayMs);
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static String spoofSystemProperty(String key, String original) {
        return BuildStealthHelper.resolveProperty(key, original);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }
}