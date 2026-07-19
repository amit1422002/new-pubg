package com.anubis.loader.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.media.BRIAudioServiceStub;
import black.android.os.BRServiceManager;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.fake.hook.BinderInvocationStub;
import com.anubis.loader.fake.hook.MethodHook;
import com.anubis.loader.fake.hook.ProxyMethod;
import com.anubis.loader.utils.MethodParameterUtils;
import com.anubis.loader.utils.VirtualPathSpoof;

/**
 * Route audio binder attribution to the cloned guest package when stealth is active.
 */
public class IAudioServiceProxy extends BinderInvocationStub {

    public IAudioServiceProxy() {
        super(BRServiceManager.get().getService(Context.AUDIO_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService(Context.AUDIO_SERVICE);
        return BRIAudioServiceStub.get().asInterface(binder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        applyStealthAttribution(args);
        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("trackPlayer")
    public static class TrackPlayer extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            applyStealthAttribution(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("playerHasOpPlayAudio")
    public static class PlayerHasOpPlayAudio extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            applyStealthAttribution(args);
            return method.invoke(who, args);
        }
    }

    private static void applyStealthAttribution(Object[] args) {
        if (!VirtualPathSpoof.shouldSpoofForGuest()) {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return;
        }
        String guest = BActivityThread.getAppPackageName();
        if (guest == null || !VirtualPathSpoof.isStealthAcPackage(guest)) {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return;
        }
        MethodParameterUtils.replaceHostPkgWithGuest(args, guest);
    }
}
