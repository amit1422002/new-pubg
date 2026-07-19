package com.anubis.loader.fake.hook;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import com.anubis.loader.fake.service.WebViewFactoryProxy;
import com.anubis.loader.fake.service.WebViewProxy;
import com.anubis.loader.fake.service.IWebViewUpdateServiceProxy;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.fake.delegate.AppInstrumentation;
import com.anubis.loader.fake.service.HCallbackProxy;
import com.anubis.loader.fake.service.IAccessibilityManagerProxy;
import com.anubis.loader.fake.service.IAudioServiceProxy;
import com.anubis.loader.fake.service.GmsProxy;
import com.anubis.loader.gms.GmsAccountManagerProxy;
import com.anubis.loader.fake.service.IActivityClientProxy;
import com.anubis.loader.fake.service.IActivityManagerProxy;
import com.anubis.loader.fake.service.IActivityTaskManagerProxy;
import com.anubis.loader.fake.service.IAlarmManagerProxy;
import com.anubis.loader.fake.service.IAppOpsManagerProxy;
import com.anubis.loader.fake.service.IAppWidgetManagerProxy;
import com.anubis.loader.fake.service.IAutofillManagerProxy;
import com.anubis.loader.fake.service.IConnectivityManagerProxy;
import com.anubis.loader.fake.service.IContextHubServiceProxy;
import com.anubis.loader.fake.service.IDeviceIdentifiersPolicyProxy;
import com.anubis.loader.fake.service.IDevicePolicyManagerProxy;
import com.anubis.loader.fake.service.IDisplayManagerProxy;
import com.anubis.loader.fake.service.IFingerprintManagerProxy;
import com.anubis.loader.fake.service.IGraphicsStatsProxy;
import com.anubis.loader.fake.service.IJobServiceProxy;
import com.anubis.loader.fake.service.ILauncherAppsProxy;
import com.anubis.loader.fake.service.ILocaleManagerProxy;
import com.anubis.loader.fake.service.ILocationManagerProxy;
import com.anubis.loader.fake.service.IMediaRouterServiceProxy;
import com.anubis.loader.fake.service.IMediaSessionManagerProxy;
import com.anubis.loader.fake.service.INetworkManagementServiceProxy;
import com.anubis.loader.fake.service.INotificationManagerProxy;
import com.anubis.loader.fake.service.IPackageManagerProxy;
import com.anubis.loader.fake.service.IPermissionManagerProxy;
import com.anubis.loader.fake.service.IPersistentDataBlockServiceProxy;
import com.anubis.loader.fake.service.IPhoneSubInfoProxy;
import com.anubis.loader.fake.service.IPowerManagerProxy;
import com.anubis.loader.fake.service.IShortcutManagerProxy;
import com.anubis.loader.fake.service.IStorageManagerProxy;
import com.anubis.loader.fake.service.IStorageStatsManagerProxy;
import com.anubis.loader.fake.service.ISystemUpdateProxy;
import com.anubis.loader.fake.service.ITelephonyManagerProxy;
import com.anubis.loader.fake.service.ITelephonyRegistryProxy;
import com.anubis.loader.fake.service.IUserManagerProxy;
import com.anubis.loader.fake.service.IVibratorServiceProxy;
import com.anubis.loader.fake.service.IVpnManagerProxy;
import com.anubis.loader.fake.service.IWifiManagerProxy;
import com.anubis.loader.fake.service.IWifiScannerProxy;
import com.anubis.loader.fake.service.IWindowManagerProxy;
import com.anubis.loader.fake.service.context.ContentServiceStub;
import com.anubis.loader.fake.service.context.RestrictionsManagerStub;
import com.anubis.loader.fake.service.libcore.OsStub;
import com.anubis.loader.fake.service.vivo.IVivoPermissionServiceProxy;
import com.anubis.loader.utils.Slog;
import com.anubis.loader.utils.compat.BuildCompat;

/**
 * Created by Milk on 3/30/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class HookManager {
    public static final String TAG = "HookManager";

    private static final HookManager sHookManager = new HookManager();

    private final Map<Class<?>, IInjectHook> mInjectors = new HashMap<>();

    public static HookManager get() {
        return sHookManager;
    }

    public void init() {
        if (AnubisCore.get().isAnubisProcess() || AnubisCore.get().isServerProcess()) {
            addInjector(new IDisplayManagerProxy());
            addInjector(new OsStub());
            addInjector(new IActivityManagerProxy());
            addInjector(new IPackageManagerProxy());
            addInjector(new ITelephonyManagerProxy());
            addInjector(new HCallbackProxy());
            addInjector(new IAppOpsManagerProxy());
            addInjector(new IAudioServiceProxy());
            addInjector(new INotificationManagerProxy());
            addInjector(new IAlarmManagerProxy());
            addInjector(new IAppWidgetManagerProxy());
            addInjector(new ContentServiceStub());
            addInjector(new IWindowManagerProxy());
            addInjector(new IUserManagerProxy());
            addInjector(new RestrictionsManagerStub());
            addInjector(new IMediaSessionManagerProxy());
            addInjector(new ILocationManagerProxy());
            addInjector(new IStorageManagerProxy());
            addInjector(new ILauncherAppsProxy());
            addInjector(new IJobServiceProxy());
            addInjector(new IAccessibilityManagerProxy());
            addInjector(new ITelephonyRegistryProxy());
            addInjector(new IDevicePolicyManagerProxy());
            addInjector(new GmsProxy());
            addInjector(new GmsAccountManagerProxy());
            addInjector(new IConnectivityManagerProxy());
            addInjector(new IPhoneSubInfoProxy());
            addInjector(new IMediaRouterServiceProxy());
            addInjector(new IPowerManagerProxy());
            addInjector(new IContextHubServiceProxy());
            addInjector(new IVibratorServiceProxy());
            addInjector(new IPersistentDataBlockServiceProxy());
            
            addInjector(AppInstrumentation.get());
            addInjector(new IWifiManagerProxy());
            addInjector(new IWifiScannerProxy());
            addInjector(new WebViewProxy());
            addInjector(new WebViewFactoryProxy());
            addInjector(new IWebViewUpdateServiceProxy());
            
            // 15.0
            if (BuildCompat.isVivo()){
                addInjector(new IVivoPermissionServiceProxy());
            }
            
            // 13.0
            if (BuildCompat.isT()){
                addInjector(new ILocaleManagerProxy());
            }
            
            // 12.0
            if (BuildCompat.isS()) {
                addInjector(new IActivityClientProxy(null));
                addInjector(new IVpnManagerProxy());
            }
            // 11.0
            if (BuildCompat.isR()) {
                addInjector(new IPermissionManagerProxy());
            }
            // 10.0
            if (BuildCompat.isQ()) {
                addInjector(new IActivityTaskManagerProxy());
            }
            // 9.0
            if (BuildCompat.isPie()) {
                addInjector(new ISystemUpdateProxy());
            }
            // 8.0
            if (BuildCompat.isOreo()) {
                addInjector(new IAutofillManagerProxy());
                addInjector(new IDeviceIdentifiersPolicyProxy());
                addInjector(new IStorageStatsManagerProxy());
            }
            // 7.1
            if (BuildCompat.isN_MR1()) {
                addInjector(new IShortcutManagerProxy());
            }
            // 7.0
            if (BuildCompat.isN()) {
                addInjector(new INetworkManagementServiceProxy());
            }
            // 6.0
            if (BuildCompat.isM()) {
                addInjector(new IFingerprintManagerProxy());
                addInjector(new IGraphicsStatsProxy());
            }
            // 5.0
            if (BuildCompat.isL()) {
                addInjector(new IJobServiceProxy());
            }
        }
        injectAll();
    }

    public void checkEnv(Class<?> clazz) {
        IInjectHook iInjectHook = mInjectors.get(clazz);
        if (iInjectHook != null && iInjectHook.isBadEnv()) {
            Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env");
            iInjectHook.injectHook();
        }
    }

    public void checkAll() {
        for (Class<?> aClass : mInjectors.keySet()) {
            IInjectHook iInjectHook = mInjectors.get(aClass);
            if (iInjectHook != null && iInjectHook.isBadEnv()) {
                Log.d(TAG, "checkEnv: " + aClass.getSimpleName() + " is bad env");
                iInjectHook.injectHook();
            }
        }
    }

    void addInjector(IInjectHook injectHook) {
        mInjectors.put(injectHook.getClass(), injectHook);
    }

    void injectAll() {
        for (IInjectHook value : mInjectors.values()) {
            try {
                value.injectHook();
            } catch (Exception ignored) {
            }
        }
    }
}
