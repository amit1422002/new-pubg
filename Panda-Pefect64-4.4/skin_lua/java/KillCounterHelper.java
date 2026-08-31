package com.blazehealth.tracker.utils;

import android.content.Context;
import android.util.Log;

import com.anubis.loader.core.env.BEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read/write NumberUpdate.txt for Lua kill counter (_G.killCountInfo). */
public final class KillCounterHelper {

    private static final String TAG = "KillCounter";
    private static final String KILL_FILE = "NumberUpdate.txt";
    private static final Pattern ENTRY =
            Pattern.compile("\\[(\\d+)\\]\\s*=\\s*(\\d+)");

    private KillCounterHelper() {
    }

    public static Map<Integer, Integer> readKillCounts(Context context) {
        Map<Integer, Integer> out = new HashMap<>();
        for (File file : resolveKillFiles(context)) {
            parseInto(out, readText(file));
        }
        return out;
    }

    public static boolean writeKillCounts(Context context, Map<Integer, Integer> counts) {
        if (context == null || counts == null) {
            return false;
        }
        String body = formatKillFile(counts);
        boolean ok = false;
        try {
            BEnvironment.load();
            File ext = BEnvironment.getExternalDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            File intF = BEnvironment.getDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            if (ext != null) {
                if (!ext.exists()) {
                    ext.mkdirs();
                }
                ok |= writeText(new File(ext, KILL_FILE), body);
            }
            if (intF != null) {
                if (!intF.exists()) {
                    intF.mkdirs();
                }
                ok |= writeText(new File(intF, KILL_FILE), body);
            }
            if (ok) {
                Log.i(TAG, "saved kill counts (" + counts.size() + " guns)");
            }
        } catch (Throwable t) {
            Log.w(TAG, "writeKillCounts failed", t);
        }
        return ok;
    }

    private static File[] resolveKillFiles(Context context) {
        try {
            BEnvironment.load();
            File ext = BEnvironment.getExternalDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            File intF = BEnvironment.getDataFilesDir(CloneDataHelper.BGMI_PKG, 0);
            if (ext != null && intF != null) {
                return new File[]{new File(ext, KILL_FILE), new File(intF, KILL_FILE)};
            }
            if (ext != null) {
                return new File[]{new File(ext, KILL_FILE)};
            }
            if (intF != null) {
                return new File[]{new File(intF, KILL_FILE)};
            }
        } catch (Throwable t) {
            Log.w(TAG, "resolveKillFiles failed", t);
        }
        return new File[0];
    }

    private static void parseInto(Map<Integer, Integer> out, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        Matcher m = ENTRY.matcher(content);
        while (m.find()) {
            try {
                int id = Integer.parseInt(m.group(1));
                int kills = Integer.parseInt(m.group(2));
                out.put(id, kills);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    static String formatKillFile(Map<Integer, Integer> counts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() < 0) {
                continue;
            }
            sb.append("    [").append(e.getKey()).append("] = ").append(e.getValue()).append(",\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String readText(File file) {
        if (file == null || !file.isFile()) {
            return "";
        }
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) Math.min(file.length(), 256 * 1024)];
            int n = in.read(buf);
            if (n <= 0) {
                return "";
            }
            return new String(buf, 0, n, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return "";
        }
    }

    private static boolean writeText(File file, String text) {
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "writeText " + file, t);
            return false;
        }
    }
}
