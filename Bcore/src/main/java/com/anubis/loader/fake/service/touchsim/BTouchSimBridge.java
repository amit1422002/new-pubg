package com.anubis.loader.fake.service.touchsim;

import android.os.Bundle;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.IOCore;
import com.anubis.loader.core.env.BEnvironment;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Host ↔ game aim IPC via vxdata file (fallback when ContentProvider poll fails).
 */
public final class BTouchSimBridge {

    private static final int VERSION = 1;
    private static final String FILE_NAME = "touchsim_ipc.dat";
    private static final long MAX_AGE_MS = 5000L;

    private BTouchSimBridge() {
    }

    static File primaryPath() {
        try {
            return new File(BEnvironment.getSystemDir(), FILE_NAME);
        } catch (Throwable ignored) {
        }
        String host = AnubisCore.getHostPkg();
        if (host == null || host.isEmpty()) {
            host = "com.blazehealth.tracker";
        }
        return new File("/data/user/0/" + host + "/vxdata/system", FILE_NAME);
    }

    static File fallbackPath() {
        String host = AnubisCore.getHostPkg();
        if (host == null || host.isEmpty()) {
            host = "com.blazehealth.tracker";
        }
        return new File("/data/user/0/" + host + "/files", FILE_NAME);
    }

    public static void write(Bundle in) {
        if (in == null) {
            return;
        }
        writeTo(primaryPath(), in);
        writeTo(fallbackPath(), in);
    }

    private static void writeTo(File f, Bundle in) {
        File parent = f.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File tmp = new File(f.getAbsolutePath() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp);
             DataOutputStream out = new DataOutputStream(fos)) {
            out.writeInt(VERSION);
            out.writeBoolean(in.getBoolean("touchSimRunning", false));
            out.writeBoolean(in.getBoolean("aimActive", false));
            out.writeBoolean(in.getBoolean("invert", false));
            out.writeFloat(in.getFloat("aimX", 0f));
            out.writeFloat(in.getFloat("aimY", 0f));
            out.writeFloat(in.getFloat("touchX", 650f));
            out.writeFloat(in.getFloat("touchY", 1400f));
            out.writeFloat(in.getFloat("touchRange", 300f));
            out.writeFloat(in.getFloat("aimSpeed", 20f));
            out.writeInt(in.getInt("screenW", 1080));
            out.writeInt(in.getInt("screenH", 2400));
            out.writeLong(System.currentTimeMillis());
            out.flush();
            if (f.exists() && !f.delete()) {
                return;
            }
            if (!tmp.renameTo(f)) {
                copyTmp(tmp, f);
            }
        } catch (Throwable ignored) {
        } finally {
            tmp.delete();
        }
    }

    static Bundle read() {
        Bundle b = readFile(primaryPath());
        if (b != null) {
            return b;
        }
        b = readFile(fallbackPath());
        if (b != null) {
            return b;
        }
        String host = AnubisCore.getHostPkg();
        if (host == null || host.isEmpty()) {
            host = "com.blazehealth.tracker";
        }
        return readFile(new File("/data/user/0/" + host + "/vxdata/system", FILE_NAME));
    }

    private static Bundle readFile(File f) {
        if (f == null) {
            return null;
        }
        File resolved = IOCore.get().redirectPath(f);
        if (resolved != null && resolved.isFile()) {
            f = resolved;
        } else if (!f.isFile()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(f);
             DataInputStream in = new DataInputStream(fis)) {
            int ver = in.readInt();
            if (ver != VERSION) {
                return null;
            }
            Bundle b = new Bundle();
            b.putBoolean("touchSimRunning", in.readBoolean());
            b.putBoolean("aimActive", in.readBoolean());
            b.putBoolean("invert", in.readBoolean());
            b.putFloat("aimX", in.readFloat());
            b.putFloat("aimY", in.readFloat());
            b.putFloat("touchX", in.readFloat());
            b.putFloat("touchY", in.readFloat());
            b.putFloat("touchRange", in.readFloat());
            b.putFloat("aimSpeed", in.readFloat());
            b.putInt("screenW", in.readInt());
            b.putInt("screenH", in.readInt());
            long ts = in.readLong();
            if (System.currentTimeMillis() - ts > MAX_AGE_MS) {
                return null;
            }
            return b;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void copyTmp(File tmp, File dest) {
        try (FileInputStream fis = new FileInputStream(tmp);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buf = new byte[512];
            int n;
            while ((n = fis.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
        } catch (Throwable ignored) {
        }
    }
}
