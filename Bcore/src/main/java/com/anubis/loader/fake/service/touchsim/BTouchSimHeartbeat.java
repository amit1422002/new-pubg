package com.anubis.loader.fake.service.touchsim;

import com.anubis.loader.AnubisCore;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Game writes alive stamp; host reads to verify inject without logcat.
 */
public final class BTouchSimHeartbeat {

    private static final String FILE_NAME = "touchsim_game_alive.txt";

    private BTouchSimHeartbeat() {
    }

    public static File path() {
        String host = AnubisCore.getHostPkg();
        if (host == null || host.isEmpty()) {
            host = "com.blazehealth.tracker";
        }
        return new File("/data/user/0/" + host + "/files", FILE_NAME);
    }

    public static void ping(String pkg, boolean aimActive, int ax, int ay) {
        File f = path();
        File parent = f.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        String line = System.currentTimeMillis() + "|" + pkg + "|aim=" + aimActive
                + "|xy=" + ax + "," + ay + "\n";
        try (FileOutputStream fos = new FileOutputStream(f, false)) {
            fos.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    public static boolean hasRecentPing(long maxAgeMs) {
        File f = path();
        return f.isFile() && System.currentTimeMillis() - f.lastModified() <= maxAgeMs;
    }

    public static String readLine() {
        File f = path();
        if (!f.isFile()) {
            return null;
        }
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            byte[] buf = new byte[256];
            int n = fis.read(buf);
            if (n <= 0) {
                return null;
            }
            return new String(buf, 0, n, StandardCharsets.UTF_8).trim();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
