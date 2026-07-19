package com.anubis.loader.fake.service.context.providers;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IInterface;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import black.android.content.BRAttributionSource;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.device.DeviceSpoofManager;
import com.anubis.loader.fake.hook.ClassInvocationStub;
import com.anubis.loader.utils.compat.ContextCompat;

/**
 * Created by Milk on 4/8/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class SystemProviderStub extends ClassInvocationStub implements BContentProvider {
    private IInterface mBase;

    private static final Set<String> SPOOF_SECURE_KEYS = new HashSet<>();
    private static final Map<String, String> SECURE_SPOOF_VALUES = new HashMap<>();

    static {
        SPOOF_SECURE_KEYS.add("android_id");
        SPOOF_SECURE_KEYS.add("adb_enabled");
        SPOOF_SECURE_KEYS.add("development_settings_enabled");
        SPOOF_SECURE_KEYS.add("mock_location");
        SPOOF_SECURE_KEYS.add("install_non_market_apps");
        SPOOF_SECURE_KEYS.add("enabled_accessibility_services");
        SPOOF_SECURE_KEYS.add("accessibility_enabled");
        SPOOF_SECURE_KEYS.add("usb_mass_storage_enabled");

        SECURE_SPOOF_VALUES.put("adb_enabled", "0");
        SECURE_SPOOF_VALUES.put("development_settings_enabled", "0");
        SECURE_SPOOF_VALUES.put("mock_location", "0");
        SECURE_SPOOF_VALUES.put("install_non_market_apps", "0");
        SECURE_SPOOF_VALUES.put("accessibility_enabled", "0");
        SECURE_SPOOF_VALUES.put("usb_mass_storage_enabled", "0");
    }

    @Override
    public IInterface wrapper(IInterface contentProviderProxy, String appPkg) {
        mBase = contentProviderProxy;
        injectHook();
        return (IInterface) getProxyInvocation();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {

    }

    @Override
    protected void onBindMethod() {

    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("asBinder".equals(method.getName())) {
            return method.invoke(mBase, args);
        }
        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof String) {
                args[0] = AnubisCore.getHostPkg();
            } else if (arg.getClass().getName().equals(BRAttributionSource.getRealClass().getName())) {
                ContextCompat.fixAttributionSourceState(arg, AnubisCore.getHostUid());
            }
        }
        Object result = method.invoke(mBase, args);
        return maybeSpoofSettings(method, args, result);
    }

    private static Object maybeSpoofSettings(Method method, Object[] args, Object result) {
        if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
            return result;
        }
        String key = extractSettingsKey(method, args);
        if (TextUtils.isEmpty(key) || !SPOOF_SECURE_KEYS.contains(key)) {
            return result;
        }
        if ("android_id".equals(key)) {
            return spoofAndroidId(method, args, result);
        }
        String spoofed = SECURE_SPOOF_VALUES.get(key);
        if (spoofed == null) {
            return result;
        }
        if ("call".equals(method.getName())) {
            Bundle out = result instanceof Bundle ? new Bundle((Bundle) result) : new Bundle();
            out.putString("value", spoofed);
            return out;
        }
        if ("query".equals(method.getName()) && result instanceof Cursor) {
            MatrixCursor cursor = new MatrixCursor(new String[]{"name", "value"});
            cursor.addRow(new Object[]{key, spoofed});
            return cursor;
        }
        return result;
    }

    private static Object spoofAndroidId(Method method, Object[] args, Object result) {
        int userId = BActivityThread.getUserId();
        String pkg = BActivityThread.getAppPackageName();
        String androidId = DeviceSpoofManager.get().resolveAndroidId(userId, pkg);
        if (TextUtils.isEmpty(androidId)) {
            return result;
        }
        if ("call".equals(method.getName())) {
            Bundle out = result instanceof Bundle ? new Bundle((Bundle) result) : new Bundle();
            out.putString("value", androidId);
            return out;
        }
        if ("query".equals(method.getName())) {
            MatrixCursor cursor = new MatrixCursor(new String[]{"name", "value"});
            cursor.addRow(new Object[]{"android_id", androidId});
            return cursor;
        }
        return result;
    }

    private static String extractSettingsKey(Method method, Object[] args) {
        if (args == null) {
            return null;
        }
        if ("call".equals(method.getName()) && args.length >= 2 && args[1] instanceof String) {
            return (String) args[1];
        }
        if ("query".equals(method.getName()) && args.length >= 1 && args[0] instanceof Uri) {
            Uri uri = (Uri) args[0];
            return uri != null ? uri.getLastPathSegment() : null;
        }
        return null;
    }
}
