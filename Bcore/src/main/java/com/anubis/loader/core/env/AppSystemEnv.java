package com.anubis.loader.core.env;

import android.content.ComponentName;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.anubis.loader.AnubisCore;

/**
 * Created by Milk on 4/21/21.
 */
public class AppSystemEnv {
    private static final List<String> sSystemPackages = new ArrayList<>();
    private static final List<String> sSuPackages = new ArrayList<>();
    private static final List<String> sXposedPackages = new ArrayList<>();
    private static final List<String> sContainerPackages = new ArrayList<>();
    private static final List<String> sPreInstallPackages = new ArrayList<>();

    static {
        sSystemPackages.add("android");
        sSystemPackages.add("com.google.android.webview");
        sSystemPackages.add("com.google.android.webview.dev");
        sSystemPackages.add("com.google.android.webview.beta");
        sSystemPackages.add("com.google.android.webview.canary");
        sSystemPackages.add("com.android.webview");
        sSystemPackages.add("com.android.camera");
        sSystemPackages.add("com.android.talkback");
        sSystemPackages.add("com.miui.gallery");
        sSystemPackages.add("com.google.android.inputmethod.latin");
        sSystemPackages.add("com.huawei.webview");
        sSystemPackages.add("com.coloros.safecenter");

        sSuPackages.addAll(Arrays.asList(
                "com.noshufou.android.su",
                "com.noshufou.android.su.elite",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su",
                "com.topjohnwu.magisk",
                "com.kingroot.kinguser",
                "com.kingo.root",
                "com.devadvance.rootcloak",
                "com.devadvance.rootcloakplus",
                "de.robv.android.xposed.installer",
                "com.saurik.substrate",
                "com.zachspong.temprootremovejb",
                "com.ramdroid.appquarantine",
                "com.android.vending.billing.InAppBillingService.COIN",
                "com.chelpus.lackypatch",
                "com.ramdroid.appquarantine",
                "com.amphoras.hidemyroot",
                "com.formyhm.hideroot",
                "me.weishu.kernelsu",
                "com.tsng.hidemyapplist",
                "io.github.huskydg.magisk"));

        sXposedPackages.addAll(Arrays.asList(
                "de.robv.android.xposed.installer",
                "org.meowcat.edxposed.manager",
                "org.lsposed.manager",
                "io.github.lsposed.manager",
                "com.solohsu.android.edxp.manager",
                "org.lsposed.lspatch",
                "com.android.vendinf"));

        sContainerPackages.addAll(Arrays.asList(
                "com.anubis.loader",
                "top.niunaijun.blackbox",
                "com.excean.gspace",
                "com.lbe.parallel",
                "com.parallel.space.lite",
                "com.dualspace.multispace",
                "com.ludashi.dualspace",
                "com.polestar.super.clone",
                "com.excelliance.dualaid",
                "com.clone.android.dual.space",
                "com.polestar.domultiple",
                "com.dual.clone.space",
                "com.lody.virtual",
                "io.va.exposed",
                "com.vphonegaga.titan",
                "com.anubis",
                "top.anubis",
                "com.ayan.box",
                "com.parallel.space",
                "com.excelliance.multaccount",
                "com.cloneapp.parallelspace"));
    }

    public static boolean isOpenPackage(String packageName) {
        return sSystemPackages.contains(packageName);
    }

    public static boolean isOpenPackage(ComponentName componentName) {
        return componentName != null && isOpenPackage(componentName.getPackageName());
    }

    public static boolean isBlackPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        if (AnubisCore.get().isHideRoot() && sSuPackages.contains(packageName)) {
            return true;
        }
        if (AnubisCore.get().isHideXposed() && sXposedPackages.contains(packageName)) {
            return true;
        }
        return shouldHideContainerPackage(packageName);
    }

    /** Hide virtual-app / clone containers from guest package scans. */
    public static boolean shouldHideContainerPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        String host = AnubisCore.getHostPkg();
        if (host != null && host.equals(packageName)) {
            return true;
        }
        if (sContainerPackages.contains(packageName)) {
            return true;
        }
        String lower = packageName.toLowerCase(Locale.US);
        return lower.contains("blackbox")
                || lower.contains("virtual")
                || lower.contains("parallel")
                || lower.contains("dualspace")
                || lower.contains("clone")
                || lower.contains("vspace")
                || lower.contains("niunaijun")
                || lower.contains("anubis.loader")
                || lower.contains("gspace")
                || lower.contains("dualaid")
                || lower.contains("vphonegaga");
    }

    public static List<String> getPreInstallPackages() {
        return sPreInstallPackages;
    }
}
