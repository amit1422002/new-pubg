package com.anubis.loader.fake.service;

import java.lang.reflect.Method;

import black.android.telephony.BRTelephonyManager;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.device.DeviceSpoofManager;
import com.anubis.loader.core.device.DeviceSpoofIds;
import com.anubis.loader.fake.hook.ClassInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.MethodParameterUtils;

/**
 * Created by Anubis on 2022/2/26.
 */
public class IPhoneSubInfoProxy extends ClassInvocationStub {
    public static final String TAG = "IPhoneSubInfoProxy";

    public IPhoneSubInfoProxy() {
        if (BRTelephonyManager.get()._check_sServiceHandleCacheEnabled() != null) {
            BRTelephonyManager.get()._set_sServiceHandleCacheEnabled(true);
        }
        if (BRTelephonyManager.get()._check_getSubscriberInfoService() != null) {
            BRTelephonyManager.get().getSubscriberInfoService();
        }
    }

    @Override
    protected Object getWho() {
        return BRTelephonyManager.get().sIPhoneSubInfo();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRTelephonyManager.get()._set_sIPhoneSubInfo(proxyInvocation);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        return super.invoke(proxy, method, args);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    private static String resolveImei() {
        int userId = BActivityThread.getUserId();
        String pkg = BActivityThread.getAppPackageName();
        String spoof = DeviceSpoofManager.get().resolveImei(userId, pkg);
        return spoof != null ? spoof : DeviceSpoofIds.stableImei(userId, pkg);
    }

    private static String resolveSubscriberId() {
        int userId = BActivityThread.getUserId();
        String pkg = BActivityThread.getAppPackageName();
        if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
            return null;
        }
        return DeviceSpoofIds.stableImei(userId, pkg + ":imsi");
    }

    private static String resolveSerial() {
        int userId = BActivityThread.getUserId();
        String pkg = BActivityThread.getAppPackageName();
        String spoof = DeviceSpoofManager.get().resolveSerial(userId, pkg);
        return spoof != null ? spoof : DeviceSpoofIds.stableSerial(userId, pkg);
    }

    @ProxyMethod("getLine1NumberForSubscriber")
    public static class getLine1NumberForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
                return method.invoke(who, args);
            }
            return resolveImei();
        }
    }

    @ProxyMethod("getDeviceIdForPhone")
    public static class GetDeviceIdForPhone extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
                return method.invoke(who, args);
            }
            return resolveImei();
        }
    }

    @ProxyMethod("getImeiForSubscriber")
    public static class GetImeiForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
                return method.invoke(who, args);
            }
            return resolveImei();
        }
    }

    @ProxyMethod("getSubscriberId")
    public static class GetSubscriberId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
                return method.invoke(who, args);
            }
            String imsi = resolveSubscriberId();
            return imsi != null ? imsi : resolveImei();
        }
    }

    @ProxyMethod("getSubscriberIdForSubscriber")
    public static class GetSubscriberIdForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
                return method.invoke(who, args);
            }
            String imsi = resolveSubscriberId();
            return imsi != null ? imsi : resolveImei();
        }
    }

    @ProxyMethod("getIccSerialNumber")
    public static class GetIccSerialNumber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
                return method.invoke(who, args);
            }
            return resolveSerial();
        }
    }

    @ProxyMethod("getIccSerialNumberForSubscriber")
    public static class GetIccSerialNumberForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!DeviceSpoofManager.shouldSpoofCurrentProcess()) {
                return method.invoke(who, args);
            }
            return resolveSerial();
        }
    }
}
