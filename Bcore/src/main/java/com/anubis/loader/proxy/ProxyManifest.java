package com.anubis.loader.proxy;

import android.content.ComponentName;

import java.util.Locale;

import com.anubis.loader.AnubisCore;

/**
 * Created by Milk on 4/1/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class ProxyManifest {
    public static final int FREE_COUNT = 50;

    public static boolean isProxy(String msg) {
        if (msg == null) {
            return false;
        }
        // Guest was scanning authorities for proxy_content_provider_* / .loader.SystemCallProvider.
        return getBindProvider().equals(msg)
                || msg.contains(".android.cp.")
                || msg.contains("proxy_content_provider_")
                || msg.contains(".loader.SystemCallProvider");
    }

    public static String getBindProvider() {
        return AnubisCore.getHostPkg() + ".android.sys";
    }

    public static String getProxyAuthorities(int index) {
        return String.format(Locale.US, "%s.android.cp.%d", AnubisCore.getHostPkg(), index);
    }

    /** Guest was enumerating host manifest proxy stubs via PackageManager component queries. */
    public static boolean isHostProxyComponent(ComponentName componentName) {
        if (componentName == null) {
            return false;
        }
        if (!AnubisCore.getHostPkg().equals(componentName.getPackageName())) {
            return false;
        }
        String cls = componentName.getClassName();
        if (cls == null) {
            return false;
        }
        return cls.startsWith("com.anubis.loader.proxy.")
                || cls.contains("ProxyActivity$")
                || cls.contains("ProxyService$")
                || cls.contains("ProxyContentProvider$")
                || cls.contains("ProxyJobService$")
                || cls.contains("ProxyPendingActivity$")
                || cls.contains("TransparentProxyActivity$");
    }

    public static String getProxyPendingActivity(int index) {
        return String.format(Locale.CHINA, "com.anubis.loader.proxy.ProxyPendingActivity$P%d", index);
    }

    public static String getProxyActivity(int index) {
        return String.format(Locale.CHINA, "com.anubis.loader.proxy.ProxyActivity$P%d", index);
    }

    public static String TransparentProxyActivity(int index) {
        return String.format(Locale.CHINA, "com.anubis.loader.proxy.TransparentProxyActivity$P%d", index);
    }

    public static String getProxyService(int index) {
        return String.format(Locale.CHINA, "com.anubis.loader.proxy.ProxyService$P%d", index);
    }

    public static String getProxyJobService(int index) {
        return String.format(Locale.CHINA, "com.anubis.loader.proxy.ProxyJobService$P%d", index);
    }

    public static String getProxyFileProvider() {
        return AnubisCore.getHostPkg() + ".android.file";
    }

    public static String getProxyReceiver() {
        return AnubisCore.getHostPkg() + ".stub_receiver";
    }

    public static String getProcessName(int bPid) {
        return AnubisCore.getHostPkg() + ":p" + bPid;
    }
}
