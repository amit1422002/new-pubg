package com.anubis.loader.gms;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.entity.pm.InstallResult;
import com.anubis.loader.utils.Slog;

/**
 * Installs microG GMS + FakeStore into the virtual environment.
 * These provide a real {@code com.google} AccountAuthenticator and proper
 * {@code oauth2:.../auth/googleplay} tokens for Play Store.
 */
public final class MicroGInstaller {
    private static final String TAG = "MicroGInstaller";

    private static final String RELEASE = "v0.3.15.250932";
    private static final String BASE = "https://github.com/microg/GmsCore/releases/download/" + RELEASE + "/";
    private static final String GMS_APK = "com.google.android.gms-250932030.apk";
    private static final String VENDING_APK = "com.android.vending-84022630.apk";

    private MicroGInstaller() {}

    public static InstallResult install(int userId) {
        Context ctx = AnubisCore.getContext();
        if (ctx == null) return new InstallResult().installError("no context");

        try {
            AnubisCore core = AnubisCore.get();
            if (core.isInstalled(GmsCore.GMS_PKG, userId)) {
                core.uninstallPackageAsUser(GmsCore.GMS_PKG, userId);
            }
            if (core.isInstalled(GmsCore.VENDING_PKG, userId)) {
                core.uninstallPackageAsUser(GmsCore.VENDING_PKG, userId);
            }

            File gmsApk = ensureApk(ctx, GMS_APK);
            if (gmsApk == null) {
                return new InstallResult().installError("Failed to download microG GMS");
            }
            InstallResult gmsResult = core.installPackageAsUser(gmsApk, userId);
            if (!gmsResult.success) {
                Slog.e(TAG, "GMS install failed: " + gmsResult.msg);
                return gmsResult;
            }
            Slog.d(TAG, "microG GMS installed");

            File vendingApk = ensureApk(ctx, VENDING_APK);
            if (vendingApk == null) {
                return new InstallResult().installError("Failed to download FakeStore");
            }
            InstallResult vendingResult = core.installPackageAsUser(vendingApk, userId);
            if (!vendingResult.success) {
                Slog.e(TAG, "FakeStore install failed: " + vendingResult.msg);
                core.uninstallPackageAsUser(GmsCore.GMS_PKG, userId);
                return vendingResult;
            }
            Slog.d(TAG, "microG FakeStore installed");
            MicroGBootstrap.afterInstall(userId);
            return new InstallResult();
        } catch (Throwable t) {
            Log.e(TAG, "install", t);
            return new InstallResult().installError(t.getMessage());
        }
    }

    private static File ensureApk(Context ctx, String name) {
        File dir = new File(ctx.getFilesDir(), "microg");
        if (!dir.exists() && !dir.mkdirs()) return null;
        File out = new File(dir, name);
        if (out.exists() && out.length() > 1_000_000L) return out;

        try (InputStream asset = ctx.getAssets().open("microg/" + name)) {
            copy(asset, out);
            if (out.length() > 1_000_000L) return out;
        } catch (Throwable ignored) {
            Slog.d(TAG, "No asset for " + name + ", downloadingΓÇª");
        }

        if (download(BASE + name, out)) return out;
        return out.exists() && out.length() > 1_000_000L ? out : null;
    }

    private static boolean download(String url, File dest) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(120_000);
            conn.setInstanceFollowRedirects(true);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Slog.e(TAG, "HTTP " + conn.getResponseCode() + " for " + url);
                return false;
            }
            try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
                copy(in, dest);
            }
            Slog.d(TAG, "Downloaded " + dest.getName() + " (" + dest.length() + " bytes)");
            return dest.length() > 1_000_000L;
        } catch (Throwable t) {
            Slog.e(TAG, "download " + url, t);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void copy(InputStream in, File dest) throws java.io.IOException {
        File tmp = new File(dest.getAbsolutePath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
        if (dest.exists()) dest.delete();
        if (!tmp.renameTo(dest)) {
            try (FileOutputStream out = new FileOutputStream(dest)) {
                try (InputStream again = new java.io.FileInputStream(tmp)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = again.read(buf)) != -1) out.write(buf, 0, n);
                }
            }
            tmp.delete();
        }
    }
}
