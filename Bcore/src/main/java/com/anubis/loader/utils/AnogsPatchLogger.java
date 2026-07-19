package com.anubis.loader.utils;

import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Dedicated libanogs patch log — /sdcard/anubisloader/bgmi_logs/anogs_patch.log
 */
public final class AnogsPatchLogger {

    public static final String LOG_FILE_NAME = "anogs_patch.log";
    /** Single logcat tag — filter live: {@code adb logcat -s ANOGS_PATCH:*} */
    public static final String LOGCAT_TAG = "ANOGS_PATCH";
    private static final String ROOT_FOLDER = "anubisloader";
    private static final String LOG_SUBDIR = "bgmi_logs";
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private static final Object LOCK = new Object();
    private static volatile String sProcessLabel = "unknown";
    private static volatile int sPatchOkCount;
    private static volatile boolean sElfNukeDone;
    private static volatile boolean sPatcherStarted;

    private AnogsPatchLogger() {
    }

    public static File getLogDir() {
        File dir = new File(Environment.getExternalStorageDirectory(),
                ROOT_FOLDER + File.separator + LOG_SUBDIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static File getLogFile() {
        return new File(getLogDir(), LOG_FILE_NAME);
    }

    public static void setProcessLabel(String label) {
        if (label != null && !label.isEmpty()) {
            sProcessLabel = label;
        }
    }

    public static void resetSessionCounters() {
        synchronized (LOCK) {
            sPatchOkCount = 0;
            sElfNukeDone = false;
            sPatcherStarted = false;
        }
    }

    public static void onPatcherStarted(int libCount, int patchCount) {
        sPatcherStarted = true;
        i("Patcher", "started libs=" + libCount + " patches=" + patchCount);
    }

    public static void onElfNukeDispatched(String packageName) {
        sElfNukeDone = true;
        i("ELF", "nuke dispatched pkg=" + packageName + " layers=L4+L5");
    }

    public static void onPatchOk(String label) {
        sPatchOkCount++;
        w("Patch", "OK " + label + " (total=" + sPatchOkCount + ")");
    }

    public static void onAllPatchesOk(int expected, int polls, int writes) {
        w("Patcher", "ALL_PATCHES_OK expected=" + expected + " polls=" + polls + " writes=" + writes);
    }

    public static void i(String tag, String message) {
        write("I", tag, message);
    }

    public static void w(String tag, String message) {
        write("W", tag, message);
    }

    public static void e(String tag, String message) {
        write("E", tag, message);
    }

    public static void e(String tag, String message, Throwable error) {
        write("E", tag, message + (error != null ? "\n" + stack(error) : ""));
    }

    public static String readTail(int maxLines) {
        File file = getLogFile();
        if (!file.exists()) {
            return "(no anogs_patch.log yet — launch BGMI clone first)";
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > maxLines) {
                    lines.remove(0);
                }
            }
        } catch (IOException e) {
            return "Failed to read log: " + e.getMessage();
        }
        if (lines.isEmpty()) {
            return "(log file is empty)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(buildSummaryFromLines(lines)).append('\n').append("---\n");
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static String buildSummaryFromLines(List<String> lines) {
        int patchOk = 0;
        boolean elfNuke = false;
        boolean patcherStarted = false;
        boolean allOk = false;
        for (String line : lines) {
            if (line.contains("] Patch: OK ") || line.contains("] Patch: OK")) {
                patchOk++;
            }
            if (line.contains("] ELF: nuke dispatched")) {
                elfNuke = true;
            }
            if (line.contains("] Patcher: started")) {
                patcherStarted = true;
            }
            if (line.contains("] Patcher: ALL_PATCHES_OK")) {
                allOk = true;
            }
        }
        return "=== libanogs patch status ===\n"
                + "file: " + getLogFile().getAbsolutePath() + '\n'
                + "elf_nuke: " + (elfNuke ? "YES" : "no") + '\n'
                + "patcher_started: " + (patcherStarted ? "YES" : "no") + '\n'
                + "all_patches_ok: " + (allOk ? "YES" : "no") + '\n'
                + "patch_ok_lines: " + patchOk + " (expect 7)";
    }

    public static String buildSummaryHeader() {
        return readTail(400).split("---", 2)[0].trim();
    }

    public static void clear() {
        synchronized (LOCK) {
            File file = getLogFile();
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            resetSessionCounters();
        }
    }

    private static void write(String level, String tag, String message) {
        mirrorLogcat(level, tag, message);
        synchronized (LOCK) {
            try {
                File file = getLogFile();
                rotateIfNeeded(file);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    writer.write(lineStamp());
                    writer.write(" [");
                    writer.write(level);
                    writer.write("] ");
                    writer.write(sProcessLabel);
                    writer.write(" pid=");
                    writer.write(String.valueOf(Process.myPid()));
                    writer.write(" sdk=");
                    writer.write(String.valueOf(Build.VERSION.SDK_INT));
                    writer.write(" ");
                    writer.write(tag != null ? tag : "Anogs");
                    writer.write(": ");
                    writer.write(message != null ? message : "");
                    writer.newLine();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void mirrorLogcat(String level, String tag, String message) {
        String line = sProcessLabel
                + " pid=" + Process.myPid()
                + " " + (tag != null ? tag : "Anogs")
                + ": " + (message != null ? message : "");
        int pri = Log.INFO;
        if ("E".equals(level)) {
            pri = Log.ERROR;
        } else if ("W".equals(level)) {
            pri = Log.WARN;
        }
        Log.println(pri, LOGCAT_TAG, line);
    }

    private static void rotateIfNeeded(File file) {
        if (file.exists() && file.length() > MAX_BYTES) {
            File rotated = new File(file.getParentFile(),
                    "anogs_patch_" + fileStamp() + ".log");
            //noinspection ResultOfMethodCallIgnored
            file.renameTo(rotated);
        }
    }

    private static String stack(Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append(error).append('\n');
        for (StackTraceElement el : error.getStackTrace()) {
            sb.append("  at ").append(el).append('\n');
        }
        return sb.toString();
    }

    private static String lineStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    private static String fileStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }
}
