package com.anubis.loader.utils;

import android.os.Build;
import android.os.Environment;
import android.os.Process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Writes BGMI / loader diagnostics to /sdcard/anubisloader/bgmi_logs/
 */
public final class BgmiFileLogger {

    private static final String ROOT_FOLDER = "anubisloader";
    private static final String LOG_SUBDIR = "bgmi_logs";
    private static final String DAILY_PREFIX = "bgmi_";
    private static final String CRASH_PREFIX = "crash_";
    private static final long MAX_DAILY_BYTES = 5L * 1024 * 1024;

    private static final Object LOCK = new Object();
    private static volatile String sProcessLabel = "unknown";

    private BgmiFileLogger() {
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

    public static void setProcessLabel(String label) {
        if (label != null && !label.isEmpty()) {
            sProcessLabel = label;
        }
    }

    public static void i(String tag, String message) {
        write("I", tag, message, null);
    }

    public static void w(String tag, String message) {
        write("W", tag, message, null);
    }

    public static void w(String tag, String message, Throwable error) {
        write("W", tag, message, error);
    }

    public static void e(String tag, String message) {
        write("E", tag, message, null);
    }

    public static void e(String tag, String message, Throwable error) {
        write("E", tag, message, error);
    }

    public static void logCrash(Thread thread, Throwable error) {
        if (error == null) {
            return;
        }
        synchronized (LOCK) {
            try {
                File dir = getLogDir();
                String stamp = fileStamp();
                File file = new File(dir, CRASH_PREFIX + stamp + "_pid" + Process.myPid() + ".log");
                try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                    out.println("time=" + lineStamp());
                    out.println("process=" + sProcessLabel);
                    out.println("pid=" + Process.myPid());
                    out.println("thread=" + (thread != null ? thread.getName() : "null"));
                    out.println("sdk=" + Build.VERSION.SDK_INT);
                    out.println("device=" + Build.MANUFACTURER + " " + Build.MODEL);
                    out.println();
                    error.printStackTrace(out);
                }
                appendDaily("E", "Crash", "saved crash log -> " + file.getAbsolutePath(), error);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void write(String level, String tag, String message, Throwable error) {
        appendDaily(level, tag, message, error);
    }

    private static void appendDaily(String level, String tag, String message, Throwable error) {
        synchronized (LOCK) {
            try {
                File dir = getLogDir();
                String day = dayStamp();
                File file = new File(dir, DAILY_PREFIX + day + ".log");
                rotateIfNeeded(file);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    writer.write(lineStamp());
                    writer.write(" [");
                    writer.write(level);
                    writer.write("] ");
                    writer.write(sProcessLabel);
                    writer.write(" pid=");
                    writer.write(String.valueOf(Process.myPid()));
                    writer.write(" ");
                    writer.write(tag != null ? tag : "Log");
                    writer.write(": ");
                    writer.write(message != null ? message : "");
                    writer.newLine();
                    if (error != null) {
                        writer.write(stackTrace(error));
                        writer.newLine();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void rotateIfNeeded(File file) {
        if (file.exists() && file.length() > MAX_DAILY_BYTES) {
            File rotated = new File(file.getParentFile(),
                    file.getName().replace(".log", "_" + fileStamp() + ".log"));
            //noinspection ResultOfMethodCallIgnored
            file.renameTo(rotated);
        }
    }

    private static String stackTrace(Throwable error) {
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String lineStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    private static String dayStamp() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static String fileStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }
}
