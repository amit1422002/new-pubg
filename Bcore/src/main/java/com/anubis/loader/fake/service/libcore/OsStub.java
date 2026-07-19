package com.anubis.loader.fake.service.libcore;

import android.os.Process;

import java.lang.reflect.Method;

import black.libcore.io.BRLibcore;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.IOCore;
import com.anubis.loader.fake.hook.ClassInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.Reflector;
import com.anubis.loader.utils.VirtualPathSpoof;

/**
 * Created by Milk on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class OsStub extends ClassInvocationStub {
    public static final String TAG = "OsStub";
    private Object mBase;

    public OsStub() {
        mBase = BRLibcore.get().os();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRLibcore.get()._set_os(proxyInvocation);
    }

    @Override
    protected void onBindMethod() {
    }

    @Override
    public boolean isBadEnv() {
        return BRLibcore.get().os() != getProxyInvocation();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args != null) {
            String name = method.getName();
            boolean touchesPath = name.equals("open")
                    || name.equals("stat")
                    || name.equals("lstat")
                    || name.equals("access")
                    || name.equals("chmod")
                    || name.equals("chown")
                    || name.equals("rename")
                    || name.equals("mkdir")
                    || name.equals("readlink")
                    || name.equals("unlink");
            if (touchesPath) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof String) {
                        String orig = (String) args[i];
                        if (orig.startsWith("/") && IOCore.pathMayNeedRedirect(orig)) {
                            args[i] = IOCore.get().redirectPath(orig);
                        }
                    }
                }
            }
        }
        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("getuid")
    public static class getuid extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int callUid = (int) method.invoke(who, args);
            return getFakeUid(callUid);
        }
    }

    @ProxyMethod("stat")
    public static class stat extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object invoke = null;
            try {
                invoke = method.invoke(who, args);
            } catch (Throwable e) {
                throw e.getCause();
            }
            Reflector.with(invoke).field("st_uid").set(getFakeUid(-1));
            return invoke;
        }
    }

    private static int getFakeUid(int callUid) {
        if (callUid > 0 && callUid <= Process.FIRST_APPLICATION_UID)
            return callUid;
        if (BActivityThread.isThreadInit() && BActivityThread.currentActivityThread().isInit()) {
            int spoofUid = VirtualPathSpoof.getProcSpoofUid();
            if (spoofUid > 0) {
                return spoofUid;
            }
            return AnubisCore.getHostUid();
        } else {
            return AnubisCore.getHostUid();
        }
    }
}
