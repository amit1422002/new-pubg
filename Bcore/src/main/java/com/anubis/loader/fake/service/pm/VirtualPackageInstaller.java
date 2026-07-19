package com.anubis.loader.fake.service.pm;

import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.fake.hook.ClassInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.MethodParameterUtils;
import com.anubis.loader.utils.Slog;

import java.lang.reflect.Method;

/**
 * Routes Play Store / virtual-app PackageInstaller sessions into Anubis install pipeline.
 */
public class VirtualPackageInstaller extends ClassInvocationStub {
    public static final String TAG = "VirtualPkgInstaller";

    private final Object mBase;

    private VirtualPackageInstaller(Object base) {
        mBase = base;
    }

    public static Object wrap(Object baseInstaller) {
        if (baseInstaller == null) {
            return null;
        }
        VirtualPackageInstaller installer = new VirtualPackageInstaller(baseInstaller);
        installer.onlyProxy(true);
        installer.injectHook();
        return installer.getProxyInvocation();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("createSession")
    public static class CreateSession extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object params = args[0];
            String installerPkg = args.length > 1 && args[1] instanceof String ? (String) args[1] : null;
            int userId = args.length > 2 ? MethodParameterUtils.toInt(args[2]) : BActivityThread.getUserId();
            int sessionId = VirtualInstallSession.createSession(params, installerPkg, userId);
            Slog.d(TAG, "virtual createSession -> " + sessionId);
            return sessionId;
        }
    }

    @ProxyMethod("openSession")
    public static class OpenSession extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int sessionId = MethodParameterUtils.toInt(args[0]);
            if (VirtualInstallSession.isVirtualSession(sessionId)) {
                return VirtualInstallSession.openSession(sessionId);
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("abandonSession")
    public static class AbandonSession extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int sessionId = MethodParameterUtils.toInt(args[0]);
            if (VirtualInstallSession.isVirtualSession(sessionId)) {
                VirtualInstallSession.abandonSession(sessionId);
                return null;
            }
            return method.invoke(who, args);
        }
    }
}
