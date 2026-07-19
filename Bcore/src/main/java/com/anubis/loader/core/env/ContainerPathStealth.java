package com.anubis.loader.core.env;

import android.text.TextUtils;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.GmsCore;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Rewrites container/host filesystem paths so guest native/Java probes see a normal app data tree.
 */
@Obfuscate
public final class ContainerPathStealth {

    /** Neutral internal cache dir (replaces legacy {@code anubis/cache}). */
    public static final String INTERNAL_CACHE_SEGMENT = ".sys/cache";

    private ContainerPathStealth() {
    }

    public static boolean isGuestProcess() {
        String pkg = BActivityThread.getAppPackageName();
        String host = AnubisCore.getHostPkg();
        return pkg != null && host != null && !host.equals(pkg);
    }

    /** microG / GMS need real vfs paths and host-aligned identity — no container stealth. */
    public static boolean shouldApplyPathStealth() {
        if (!isGuestProcess()) {
            return false;
        }
        String pkg = BActivityThread.getAppPackageName();
        return pkg == null || !GmsCore.isGoogleAppOrService(pkg);
    }

    public static String canonicalDataDir(String guestPkg, int systemUserId) {
        return String.format(Locale.US, "/data/user/%d/%s", systemUserId, guestPkg);
    }

    public static String canonicalLegacyDataDir(String guestPkg) {
        return String.format(Locale.US, "/data/data/%s", guestPkg);
    }

    public static String sanitizeGuestPath(String path) {
        if (TextUtils.isEmpty(path) || !shouldApplyPathStealth()) {
            return path;
        }
        String guestPkg = BActivityThread.getAppPackageName();
        int userId = BActivityThread.getUserId();
        int sysUser = AnubisCore.getHostUserId();
        String hostPkg = AnubisCore.getHostPkg();
        if (guestPkg == null || hostPkg == null) {
            return path;
        }

        String canonical = canonicalDataDir(guestPkg, sysUser);
        String canonicalData = canonicalLegacyDataDir(guestPkg);

        path = path.replace(canonicalLegacyDataDir(hostPkg), canonicalData);
        path = path.replace(canonicalDataDir(hostPkg, sysUser), canonical);
        path = path.replace("/data/data/" + hostPkg, canonicalData);
        path = path.replace(String.format(Locale.US, "/data/user/%d/%s", sysUser, hostPkg), canonical);

        String vfsSuffix = "/files/" + BEnvironment.VIRTUAL_ROOT_DIR + "/data/user/" + userId + "/" + guestPkg;
        int vfsIdx = path.indexOf(vfsSuffix);
        if (vfsIdx >= 0) {
            path = canonical + path.substring(vfsIdx + vfsSuffix.length());
        }
        vfsSuffix = "/files/" + BEnvironment.VIRTUAL_ROOT_DIR + "/data/data/" + guestPkg;
        vfsIdx = path.indexOf(vfsSuffix);
        if (vfsIdx >= 0) {
            path = canonicalData + path.substring(vfsIdx + vfsSuffix.length());
        }

        if (path.contains("/" + BEnvironment.VIRTUAL_ROOT_DIR + "/")) {
            int marker = path.indexOf("/" + BEnvironment.VIRTUAL_ROOT_DIR + "/");
            String tail = path.substring(marker + BEnvironment.VIRTUAL_ROOT_DIR.length() + 2);
            if (tail.startsWith("data/user/" + userId + "/" + guestPkg)) {
                path = canonical + tail.substring(("data/user/" + userId + "/" + guestPkg).length());
            } else if (tail.startsWith("data/data/" + guestPkg)) {
                path = canonicalData + tail.substring(("data/data/" + guestPkg).length());
            }
        }

        path = path.replace("/anubis/cache", "/cache");
        path = path.replace("/anubis/", "/.sys/");
        path = path.replace("/" + INTERNAL_CACHE_SEGMENT, "/cache");
        return path;
    }

    public static void applyGuestRedirects(
            Map<String, String> rule, String packageName, int userId, int systemUserId) {
        if (!shouldApplyPathStealth() || TextUtils.isEmpty(packageName)) {
            return;
        }
        String hostPkg = AnubisCore.getHostPkg();
        if (hostPkg == null || AnubisCore.getContext() == null) {
            return;
        }

        String canonical = canonicalDataDir(packageName, systemUserId);
        String guestVfsData = BEnvironment.getDataDir(packageName, userId).getAbsolutePath();
        String guestDeData = BEnvironment.getDeDataDir(packageName, userId).getAbsolutePath();

        // Only map this guest's vfs data/de dirs to canonical paths (never the whole .vfs root).
        rule.put(guestVfsData, canonical);
        rule.put(guestVfsData + "/", canonical + "/");
        rule.put(guestDeData, String.format(Locale.US, "/data/user_de/%d/%s", systemUserId, packageName));
    }
}
