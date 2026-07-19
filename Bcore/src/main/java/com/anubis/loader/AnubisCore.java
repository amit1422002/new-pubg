package com.anubis.loader;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import black.android.app.Activity;
import black.android.app.BRActivityThread;
import black.android.os.BRUserHandle;
import me.weishu.reflection.Reflection;

import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.app.LauncherActivity;
import com.anubis.loader.app.configuration.AppLifecycleCallback;
import com.anubis.loader.app.configuration.ClientConfiguration;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.core.NativeCore;
import com.anubis.loader.core.device.DeviceSpoofManager;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.entity.DeviceSpoofConfig;
import com.anubis.loader.core.system.DaemonService;
import com.anubis.loader.core.system.ServiceManager;
import com.anubis.loader.core.system.user.BUserHandle;
import com.anubis.loader.core.system.user.BUserInfo;
import com.anubis.loader.entity.pm.InstallOption;
import com.anubis.loader.entity.pm.InstallResult;
import com.anubis.loader.entity.pm.InstalledModule;
import com.anubis.loader.fake.delegate.ContentProviderDelegate;
import com.anubis.loader.fake.frameworks.BActivityManager;
import com.anubis.loader.fake.frameworks.BJobManager;
import com.anubis.loader.fake.frameworks.BPackageManager;
import com.anubis.loader.fake.frameworks.BStorageManager;
import com.anubis.loader.fake.frameworks.BUserManager;
import com.anubis.loader.fake.frameworks.BXposedManager;
import com.anubis.loader.fake.hook.HookManager;
import com.anubis.loader.proxy.ProxyManifest;
import com.anubis.loader.utils.FileUtils;
import com.anubis.loader.utils.StealthPathRules;
import com.anubis.loader.utils.VirtualPathSpoof;
import com.anubis.loader.utils.ObbCopyProgressListener;
import com.anubis.loader.utils.ObbUtils;
import com.anubis.loader.utils.ShellUtils;
import com.anubis.loader.utils.Slog;
import com.anubis.loader.utils.compat.BuildCompat;
import com.anubis.loader.utils.compat.BundleCompat;
import com.anubis.loader.utils.compat.XposedParserCompat;
import com.anubis.loader.utils.provider.ProviderCall;
import com.anubis.loader.license.BcoreEmbedConfig;
import com.anubis.loader.license.BcoreLicenseInstaller;

/**
 * Created by Milk on 3/30/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
@SuppressLint({"StaticFieldLeak", "NewApi"})
public class AnubisCore extends ClientConfiguration {
    public static final String TAG = "AnubisCore";

    private static final AnubisCore sAnubisCore = new AnubisCore();
    private static Context sContext;
    /** Captured at attach — never the cloned guest package. */
    private static volatile String sImmutableHostPkg;
    /** Pinned loader identity; must never equal the bound guest package. */
    private static volatile String sPinnedLoaderPkg;
    /** Loader APK package resolved from UID / attach context before guest bind. */
    private static volatile String sAttachLoaderPkg;
    private ProcessType mProcessType;
    private final Map<String, IBinder> mServices = new HashMap<>();
    private Thread.UncaughtExceptionHandler mExceptionHandler;
    private ClientConfiguration mClientConfiguration;
    private final List<AppLifecycleCallback> mAppLifecycleCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final int mHostUid = Process.myUid();
    private final int mHostUserId = BRUserHandle.get().myUserId();

    public static AnubisCore get() {
        return sAnubisCore;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public static PackageManager getPackageManager() {
        return sContext.getPackageManager();
    }

    public static String getHostPkg() {
        String guest = BActivityThread.getAppPackageName();
        if (sPinnedLoaderPkg != null && guest != null && guest.equals(sPinnedLoaderPkg)) {
            sPinnedLoaderPkg = null;
            sImmutableHostPkg = null;
        }
        String loader = defaultLoaderPackage();
        if (guest == null || !guest.equals(loader)) {
            pinLoaderPkg(loader);
            return loader;
        }
        pinLoaderPkg(loader);
        return loader;
    }

    /** Loader APK id — obfuscation-safe literal, never the bound guest. */
    public static String loaderPkgLiteral() {
        return new String(new char[] {'c', 'o', 'm', '.', 'a', 'n', 'u', 'b', 'i', 's'});
    }

    /** Loader APK id — never the bound guest package. */
    public static String defaultLoaderPackage() {
        String guest = BActivityThread.getAppPackageName();
        String loader = sAttachLoaderPkg;
        if (loader == null || loader.isEmpty()) {
            loader = loaderFromUidPackages(guest);
        }
        if (loader == null || loader.isEmpty()) {
            loader = loaderPkgLiteral();
        }
        if (guest != null && guest.equals(loader)) {
            loader = loaderPkgLiteral();
        }
        return loader;
    }

    public static String getAttachLoaderPkg() {
        return sAttachLoaderPkg;
    }

    private static String loaderFromUidPackages(String guestPkg) {
        Context ctx = sContext;
        if (ctx == null) {
            return null;
        }
        try {
            String literal = loaderPkgLiteral();
            String[] uidPkgs = ctx.getPackageManager().getPackagesForUid(Process.myUid());
            if (uidPkgs == null) {
                return null;
            }
            for (String pkg : uidPkgs) {
                if (pkg != null && literal.equals(pkg)) {
                    return pkg;
                }
            }
            for (String pkg : uidPkgs) {
                if (pkg == null || pkg.isEmpty()) {
                    continue;
                }
                if (guestPkg != null && guestPkg.equals(pkg)) {
                    continue;
                }
                return pkg;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String resolveAttachLoaderPackage(Context context) {
        String guest = BActivityThread.getAppPackageName();
        String fromUid = loaderFromUidPackages(guest);
        if (fromUid != null && !fromUid.isEmpty()
                && (guest == null || !guest.equals(fromUid))) {
            return fromUid;
        }
        try {
            String pkg = context.getApplicationInfo().packageName;
            if (pkg != null && !pkg.isEmpty() && (guest == null || !guest.equals(pkg))) {
                return pkg;
            }
        } catch (Throwable ignored) {
        }
        try {
            String pkg = context.getPackageName();
            if (pkg != null && !pkg.isEmpty() && (guest == null || !guest.equals(pkg))) {
                return pkg;
            }
        } catch (Throwable ignored) {
        }
        return loaderPkgLiteral();
    }

    public static String getPinnedLoaderPkg() {
        return sPinnedLoaderPkg;
    }

    public static void pinLoaderPkg(String hostPkg) {
        String loader = defaultLoaderPackage();
        String guest = BActivityThread.getAppPackageName();
        if (guest != null && !guest.equals(loader)) {
            hostPkg = loader;
        } else if (hostPkg == null || hostPkg.isEmpty() || (guest != null && guest.equals(hostPkg))) {
            hostPkg = loader;
        }
        sPinnedLoaderPkg = hostPkg;
        sImmutableHostPkg = hostPkg;
    }

    /**
     * @deprecated Disk scan picked up leaked guest {@code .vfs}; use {@link #defaultLoaderPackage()}.
     */
    public static String discoverLoaderPackageOnDisk(String guestPkg) {
        return defaultLoaderPackage();
    }

    private static boolean isLeakedGuestVfsRoot(String packageName, String guestPkg) {
        if (packageName == null || guestPkg == null || !packageName.equals(guestPkg)) {
            return false;
        }
        return new File(String.format(java.util.Locale.US,
                "/data/user/0/%s/files/data/user/0/%s", packageName, guestPkg)).isDirectory();
    }

    /**
     * Loader APK package (com.anubis) — only for on-disk VFS anchor, never shown to the guest.
     * Recovers from process name when context was already spoofed to the guest package.
     */
    private static String reconcileHostAgainstGuest(String host) {
        if (host == null || host.isEmpty()) {
            return host;
        }
        String guest = BActivityThread.getAppPackageName();
        if (guest == null || !guest.equals(host)) {
            return host;
        }
        String fromProc = hostPkgFromRunningProcess();
        if (fromProc != null && !fromProc.isEmpty() && !fromProc.equals(guest)) {
            return fromProc;
        }
        fromProc = hostPkgFromProcCmdline();
        if (fromProc != null && !fromProc.isEmpty() && !fromProc.equals(guest)) {
            return fromProc;
        }
        return host;
    }

    private static String hostPkgFromProcCmdline() {
        try {
            byte[] buf = new byte[256];
            int n;
            try (java.io.FileInputStream in = new java.io.FileInputStream("/proc/self/cmdline")) {
                n = in.read(buf);
            }
            if (n <= 0) {
                return null;
            }
            int end = 0;
            while (end < n && buf[end] != 0) {
                end++;
            }
            String proc = new String(buf, 0, end, java.nio.charset.StandardCharsets.UTF_8);
            int colon = proc.indexOf(':');
            return colon > 0 ? proc.substring(0, colon) : proc;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String hostPkgFromRunningProcess() {
        Context ctx = sContext;
        if (ctx == null) {
            return null;
        }
        int myPid = Process.myPid();
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info != null && info.pid == myPid && info.processName != null) {
                    String proc = info.processName;
                    int colon = proc.indexOf(':');
                    return colon > 0 ? proc.substring(0, colon) : proc;
                }
            }
        }
        return null;
    }

    public static void reconcileStorageHostPkg(String guestPkg) {
        if (guestPkg == null || guestPkg.isEmpty()) {
            return;
        }
        if (sAttachLoaderPkg == null || sAttachLoaderPkg.isEmpty() || guestPkg.equals(sAttachLoaderPkg)) {
            Context ctx = sContext;
            if (ctx != null) {
                sAttachLoaderPkg = resolveAttachLoaderPackage(ctx);
            }
        }
        pinLoaderPkg(defaultLoaderPackage());
    }

    public static int getHostUid() {
        return get().mHostUid;
    }

    public static int getHostUserId() {
        return get().mHostUserId;
    }

    public static Context getContext() {
        return sContext;
    }

    public Thread.UncaughtExceptionHandler getExceptionHandler() {
        return mExceptionHandler;
    }

    public void setExceptionHandler(Thread.UncaughtExceptionHandler exceptionHandler) {
        mExceptionHandler = exceptionHandler;
    }

    public void doAttachBaseContext(Context context, ClientConfiguration clientConfiguration) {
        if (clientConfiguration == null) {
            throw new IllegalArgumentException("ClientConfiguration is null!");
        }
        BcoreLicenseInstaller.verifyOrThrow(clientConfiguration);
        Reflection.unseal(context);
        sContext = context;
        mClientConfiguration = clientConfiguration;
        sAttachLoaderPkg = resolveAttachLoaderPackage(context);
        pinLoaderPkg(sAttachLoaderPkg);
        initNotificationManager();

        String processName = getProcessName(getContext());
        if (processName.equals(AnubisCore.getHostPkg())) {
            mProcessType = ProcessType.Main;
           // startLogcat();
        } else if (processName.endsWith(getContext().getString(R.string.black_box_service_name))) {
            mProcessType = ProcessType.Server;
        } else {
            mProcessType = ProcessType.BAppClient;
        }
        // Guest (BAppClient) and system server (:black) both need virtual paths before SystemCallProvider/BPackageManager run.
        if (AnubisCore.get().isAnubisProcess() || AnubisCore.get().isServerProcess()) {
            BEnvironment.load();
        }
        if (isServerProcess()) {
            if (clientConfiguration.isEnableDaemonService()) {
                Intent intent = new Intent();
                intent.setClass(getContext(), DaemonService.class);
                if (BuildCompat.isOreo()) {
                    getContext().startForegroundService(intent);
                } else {
                    getContext().startService(intent);
                }
            }
        }
        
        HookManager.get().init();
    }

    public void doCreate() {
        // fix contentProvider
        if (isAnubisProcess()) {
            ContentProviderDelegate.init();
        }
        if (!isServerProcess()) {
            ServiceManager.initBlackManager();
        }
        stageBypassPayload(getContext());

    }

    public static Object mainThread() {
        return BRActivityThread.get().currentActivityThread();
    }

    public void startActivity(Intent intent, int userId) {
        if (mClientConfiguration.isEnableLauncherActivity()) {
            LauncherActivity.launch(intent, userId);
        } else {
            getBActivityManager().startActivity(intent, userId);
        }
    }

    public static BJobManager getBJobManager() {
        return BJobManager.get();
    }

    public static BPackageManager getBPackageManager() {
        return BPackageManager.get();
    }

    public static BActivityManager getBActivityManager() {
        return BActivityManager.get();
    }

    public static BStorageManager getBStorageManager() {
        return BStorageManager.get();
    }

    public static void prepareGuestLaunch(String packageName, int userId) {
        try {
            BEnvironment.load();
            BEnvironment.ensureGuestDataLayout(packageName, userId, packageName);
            FileUtils.mkdirs(BEnvironment.getObbDir(packageName));
            if (VirtualPathSpoof.isStealthAcPackage(packageName)) {
                StealthPathRules.ensureHostInstallReady(packageName, userId);
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean launchApk(String packageName, int userId) {
        prepareGuestLaunch(packageName, userId);
        onBeforeMainLaunchApk(packageName,userId);

        Intent launchIntentForPackage = getBPackageManager().getLaunchIntentForPackage(packageName, userId);
        if (launchIntentForPackage == null) {
            return false;
        }
        startActivity(launchIntentForPackage, userId);
        // temp992 (libanogs 0x575550 + libgcloud) disabled — faulty OOB / SIGSEGV in patcher.
        return true;
    }

    public boolean isInstalled(String packageName, int userId) {
        return getBPackageManager().isInstalled(packageName, userId);
    }

    public void uninstallPackageAsUser(String packageName, int userId) {
        getBPackageManager().uninstallPackageAsUser(packageName, userId);
    }

    public void uninstallPackage(String packageName) {
        getBPackageManager().uninstallPackage(packageName);
    }

    public InstallResult installPackageAsUser(String packageName, int userId) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            return getBPackageManager().installPackageAsUser(packageInfo.applicationInfo.sourceDir, InstallOption.installBySystem(), userId);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return new InstallResult().installError(e.getMessage());
        }
    }

    public InstallResult installPackageAsUser(File apk, int userId) {
        return getBPackageManager().installPackageAsUser(apk.getAbsolutePath(), InstallOption.installByStorage(), userId);
    }

    public InstallResult installPackageAsUser(Uri apk, int userId) {
        return getBPackageManager().installPackageAsUser(apk.toString(), InstallOption.installByStorage().makeUriFile(), userId);
    }

    public InstallResult installXPModule(File apk) {
        return getBPackageManager().installPackageAsUser(apk.getAbsolutePath(), InstallOption.installByStorage().makeXposed(), BUserHandle.USER_XPOSED);
    }

    public InstallResult installXPModule(Uri apk) {
        return getBPackageManager().installPackageAsUser(apk.toString(), InstallOption.installByStorage().makeXposed().makeUriFile(), BUserHandle.USER_XPOSED);
    }

    public InstallResult installXPModule(String packageName) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            String path = packageInfo.applicationInfo.sourceDir;
            return getBPackageManager().installPackageAsUser(path, InstallOption.installBySystem().makeXposed(), BUserHandle.USER_XPOSED);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return new InstallResult().installError(e.getMessage());
        }
    }

    public void uninstallXPModule(String packageName) {
        uninstallPackage(packageName);
    }

    public boolean isXPEnable() {
        return BXposedManager.get().isXPEnable();
    }

    public void setXPEnable(boolean enable) {
        BXposedManager.get().setXPEnable(enable);
    }

    public boolean isXposedModule(File file) {
        return XposedParserCompat.isXPModule(file.getAbsolutePath());
    }

    public boolean isInstalledXposedModule(String packageName) {
        return isInstalled(packageName, BUserHandle.USER_XPOSED);
    }

    public boolean isModuleEnable(String packageName) {
        return BXposedManager.get().isModuleEnable(packageName);
    }

    public void setModuleEnable(String packageName, boolean enable) {
        BXposedManager.get().setModuleEnable(packageName, enable);
    }

    public List<InstalledModule> getInstalledXPModules() {
        return BXposedManager.get().getInstalledModules();
    }

    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        return getBPackageManager().getInstalledApplications(flags, userId);
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        return getBPackageManager().getInstalledPackages(flags, userId);
    }

    public void clearPackage(String packageName, int userId) {
        BPackageManager.get().clearPackage(packageName, userId);
    }

    public boolean copyObbFromHost(String packageName, int userId) {
        return ObbUtils.copyObbFromHost(packageName, userId);
    }

    public boolean copyObbFromHost(String packageName, int userId, ObbCopyProgressListener listener) {
        return ObbUtils.copyObbFromHost(packageName, userId, listener);
    }

    public boolean copyObbFromDocumentTree(android.net.Uri treeUri, String packageName, int userId) {
        return ObbUtils.copyObbFromDocumentTree(getContext(), treeUri, packageName, userId);
    }

    public boolean copyObbFromDocumentTree(android.net.Uri treeUri, String packageName, int userId,
                                           ObbCopyProgressListener listener) {
        return ObbUtils.copyObbFromDocumentTree(getContext(), treeUri, packageName, userId, listener);
    }

    public void persistObbDocumentTreeUri(String packageName, android.net.Uri treeUri) {
        ObbUtils.persistDocumentTreeUri(getContext(), packageName, treeUri);
    }

    public boolean needsObbCopy(String packageName) {
        return ObbUtils.needsObbCopy(packageName);
    }

    public static boolean hasVirtualObb(String packageName) {
        return ObbUtils.hasVirtualObb(packageName);
    }

    public static void ensureObbPresent(String packageName, int userId) {
        ObbUtils.ensureObbPresent(packageName, userId);
    }

    public boolean copyObbFromImportStaging(String packageName, int userId) {
        return ObbUtils.copyObbFromAnubisLoader(packageName, userId);
    }

    public boolean canAutoCopyObb(String packageName) {
        return ObbUtils.canAutoCopyObb(packageName);
    }

    public boolean copyObbFromDocumentFile(android.net.Uri fileUri, String packageName, int userId) {
        return ObbUtils.copyObbFromDocumentFile(getContext(), fileUri, packageName, userId);
    }

    public boolean isAnubisApp(java.io.File apkFile) {
        try {
            if (apkFile == null || !apkFile.exists()) {
                return false;
            }
            android.content.pm.PackageInfo packageInfo =
                    getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (packageInfo != null) {
                return packageInfo.packageName.equals(getHostPkg());
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error checking if APK is Anubis app: " + e.getMessage());
        }
        return false;
    }

    public boolean isAnubisApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        return packageName.equals(getHostPkg());
    }

    /**
     * Per-cloned-app device identifiers (IMEI, serial, Android ID). Persists under virtual system dir.
     * Pass {@code config.enabled == false} or use {@link #clearDeviceSpoof(int, String)} to disable.
     */
    public static void setDeviceSpoof(int userId, String packageName, DeviceSpoofConfig config) {
        ensureVirtualRootForConfig();
        DeviceSpoofManager.get().setConfig(userId, packageName, config);
    }

    public static DeviceSpoofConfig getDeviceSpoof(int userId, String packageName) {
        ensureVirtualRootForConfig();
        return DeviceSpoofManager.get().getConfig(userId, packageName);
    }

    public static void clearDeviceSpoof(int userId, String packageName) {
        ensureVirtualRootForConfig();
        DeviceSpoofManager.get().clearConfig(userId, packageName);
    }

    public static DeviceSpoofConfig newRandomDeviceSpoof() {
        return DeviceSpoofManager.newRandomConfig();
    }

    private static void ensureVirtualRootForConfig() {
        try {
            FileUtils.mkdirs(BEnvironment.getSystemDir().getAbsolutePath());
        } catch (Throwable ignored) {
        }
    }

    public void stopPackage(String packageName, int userId) {
        BPackageManager.get().stopPackage(packageName, userId);
    }

    public List<BUserInfo> getUsers() {
        return BUserManager.get().getUsers();
    }

    public BUserInfo createUser(int userId) {
        return BUserManager.get().createUser(userId);
    }

    public void deleteUser(int userId) {
        BUserManager.get().deleteUser(userId);
    }

    public List<AppLifecycleCallback> getAppLifecycleCallbacks() {
        return mAppLifecycleCallbacks;
    }

    public void removeAppLifecycleCallback(AppLifecycleCallback appLifecycleCallback) {
        mAppLifecycleCallbacks.remove(appLifecycleCallback);
    }

    public void addAppLifecycleCallback(AppLifecycleCallback appLifecycleCallback) {
        mAppLifecycleCallbacks.add(appLifecycleCallback);
    }

    public boolean isSupportGms() {
        return GmsCore.isSupportGms();
    }

    public boolean isInstallGms(int userId) {
        return GmsCore.isInstalledGoogleService(userId);
    }

    public InstallResult installGms(int userId) {
        return GmsCore.installGApps(userId);
    }

    public boolean uninstallGms(int userId) {
        GmsCore.uninstallGApps(userId);
        return !GmsCore.isInstalledGoogleService(userId);
    }

    public IBinder getService(String name) {
        IBinder binder = mServices.get(name);
        if (binder != null && binder.isBinderAlive()) {
            return binder;
        }
        Bundle bundle = new Bundle();
        bundle.putString("_B_|_server_name_", name);
        Bundle vm = ProviderCall.callSafely(ProxyManifest.getBindProvider(), "VM", null, bundle);
        binder = BundleCompat.getBinder(vm, "_B_|_server_");
        Slog.d(TAG, "getService: " + name + ", " + binder);
        mServices.put(name, binder);
        return binder;
    }

    /**
     * Process type
     */
    private enum ProcessType {
        /**
         * Server process
         */
        Server,
        /**
         * Black app process
         */
        BAppClient,
        /**
         * Main process
         */
        Main,
    }

    public boolean isAnubisProcess() {
        return mProcessType == ProcessType.BAppClient;
    }

    public boolean isMainProcess() {
        return mProcessType == ProcessType.Main;
    }

    public boolean isServerProcess() {
        return mProcessType == ProcessType.Server;
    }

    @Override
    public boolean isHideRoot() {
        return mClientConfiguration.isHideRoot();
    }

    @Override
    public boolean isHideXposed() {
        return mClientConfiguration.isHideXposed();
    }

    @Override
    public String getHostPackageName() {
        return mClientConfiguration.getHostPackageName();
    }

    @Override
    public boolean requestInstallPackage(File file, int userId) {
        return mClientConfiguration.requestInstallPackage(file, userId);
    }

    private void startLogcat() {
        new Thread(() -> {
            try {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getContext().getPackageName() + "_logcat.txt");
                FileUtils.deleteDir(file);
                ShellUtils.execCommand("logcat -c", false);
                ShellUtils.execCommand("logcat -f " + file.getAbsolutePath(), false);
            } catch (Throwable t) {
                Log.w(TAG, "startLogcat failed", t);
            }
        }).start();
    }

    private static String getProcessName(Context context) {
        int myPid = Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info != null && info.pid == myPid && info.processName != null) {
                    return info.processName;
                }
            }
        }
        // Internal classification only — guest sees spoofed name via proc/AM hooks.
        String hostPkg = getHostPkg();
        if (hostPkg != null) {
            return hostPkg;
        }
        return context.getPackageName();
    }

    public static boolean is64Bit() {
        if (BuildCompat.isM()) {
            return Process.is64Bit();
        } else {
            return Build.CPU_ABI.equals("arm64-v8a");
        }
    }

    private void initNotificationManager() {
        NotificationManager nm = (NotificationManager) AnubisCore.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ONE_ID = AnubisCore.getContext().getPackageName() + ".core_svc";
        String CHANNEL_ONE_NAME = "core_svc";
        if (BuildCompat.isOreo()) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(notificationChannel);
        }
    }
    
    public void onBeforeMainLaunchApk(String packageName,int userid) {
        for (AppLifecycleCallback appLifecycleCallback : AnubisCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeMainLaunchApk(packageName,userid);
        }
    }
    public void onBeforeMainApplicationAttach(Application app, Context context) {
        for (AppLifecycleCallback appLifecycleCallback : AnubisCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeMainApplicationAttach(app, context);
        }
    }
    public void onAfterMainApplicationAttach(Application app, Context context) {
        for (AppLifecycleCallback appLifecycleCallback : AnubisCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.afterMainApplicationAttach(app, context);
        }
    }
    public void onBeforeMainActivityOnCreate(android.app.Activity activity) {
        for (AppLifecycleCallback appLifecycleCallback : AnubisCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeMainActivityOnCreate(activity);
        }
    }
    public void onAfterMainActivityOnCreate(android.app.Activity activity) {
        for (AppLifecycleCallback appLifecycleCallback : AnubisCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.afterMainActivityOnCreate(activity);
        }
    }
    private static final String BYPASS_BIN = "temp992";

    /** Stages {@code res/raw/temp} into neutral {@code .sys/cache/temp992}. */
    private void stageBypassPayload(Context context) {
        if (context == null) {
            return;
        }
        try {
            File dataDir;
            try {
                dataDir = context.getDataDir();
            } catch (NoSuchMethodError e) {
                dataDir = context.getFilesDir().getParentFile();
            }
            if (dataDir == null) {
                return;
            }
            File outDir = new File(dataDir, com.anubis.loader.core.env.ContainerPathStealth.INTERNAL_CACHE_SEGMENT);
            File legacyDir = new File(dataDir, "anubis/cache");
            if (legacyDir.exists() && !outDir.exists()) {
                legacyDir.renameTo(outDir);
            }
            if (!outDir.exists() && !outDir.mkdirs()) {
                Log.w(TAG, "stageBypassPayload: mkdirs failed " + outDir.getAbsolutePath());
                return;
            }
            File outFile = new File(outDir, BYPASS_BIN);
            if (outFile.exists() && outFile.length() > 0L) {
                return;
            }
            try (InputStream in = context.getResources().openRawResource(R.raw.temp)) {
                FileUtils.copyFile(in, outFile);
            }
            if (!outFile.setExecutable(true, false)) {
                Log.w(TAG, "stageBypassPayload: setExecutable failed " + outFile.getAbsolutePath());
            }
        } catch (Throwable t) {
            Log.w(TAG, "stageBypassPayload", t);
        }
    }

    void runant(final String nf) {
        excpp("/" + com.anubis.loader.core.env.ContainerPathStealth.INTERNAL_CACHE_SEGMENT + "/" + nf);
    }

    private void executeBypassBinary(String fullPath) {
        // HASAD: exec temp with argv "992" (temp feature 992). Bare exec prints Usage and exits.
        String cmd = fullPath + " 992";
        ShellUtils.execCommand("chmod 777 " + fullPath, false, false);
        ShellUtils.execCommand(cmd, false, false);
        ShellUtils.execCommand("chmod 777 " + fullPath, true, false);
        ShellUtils.execCommand(cmd, true, false);
    }

    public void excpp(String path) {
        try {
            String fullPath = new File(sContext.getDataDir(), path.startsWith("/") ? path.substring(1) : path)
                    .getAbsolutePath();
            File bin = new File(fullPath);
            if (!bin.exists() || bin.length() == 0L) {
                stageBypassPayload(sContext);
            }
            if (!bin.exists() || bin.length() == 0L) {
                Log.w(TAG, "excpp: bypass binary missing " + fullPath);
                return;
            }
            executeBypassBinary(fullPath);
        } catch (Exception e) {
            Log.w(TAG, "excpp", e);
        }
    }

    public void bypass() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            runant(BYPASS_BIN);
            handler.postDelayed(() -> {
                runant(BYPASS_BIN);
                handler.postDelayed(() -> runant(BYPASS_BIN), 38_000);
            }, 30_000);
        }, 15_000);
    }



}
