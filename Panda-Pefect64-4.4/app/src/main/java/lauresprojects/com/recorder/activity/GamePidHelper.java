package lauresprojects.com.recorder.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/**
 * STEALTH EDITION: Finds BGMI PID running inside BlackBox.
 * Uses shared memory / environment variable instead of file I/O.
 * Anti-detection: randomized PID cache, /proc scan evasion, Frida check.
 */
public final class GamePidHelper {

    public static final String BGMI_PKG = "com.pubg.imobile";
    /** Passed to sock64 at launch — avoids extra /proc scans in the child. */
    public static final String SOCK_PID_ENV = "XT_GAME_PID";
    private static final String TAG = "PidHelper";

    static {
        try {
            System.loadLibrary("safecheat");
        } catch (Throwable ignored) {
        }
    }

    // Volatile for thread-safe access without locks
    private static volatile int sCachedPid = 0;
    private static volatile long sLastPidScanTime = 0;
    private static volatile boolean sPidWrittenForSession = false;
    private static volatile Thread sRefreshThread = null;
    
    // Anti-detection: randomize scan intervals
    private static final Random sRandom = new Random(System.currentTimeMillis() ^ Process.myPid());
    
    // Minimum interval between PID rescans (randomized base + jitter)
    private static final long PID_CACHE_MS = 3000L;
    private static final long PID_SCAN_JITTER_MS = 2000L;
    
    // Maximum /proc/maps reads before rotating to avoid detection
    private static volatile int sProcReadsRemaining = 50;
    private static final int PROC_READS_LIMIT = 50;

    private GamePidHelper() {
        throw new AssertionError("No instances");
    }

    // ============ ANTI-DETECTION ============
    
    /**
     * Quick check for common analysis tools.
     * Avoids reading /proc/self/maps which is monitored.
     */
    public static boolean isSuspiciousEnvironment() {
        try {
            // Check for Frida specific files
            String[] fridaPaths = {
                "/data/local/tmp/frida-server",
                "/data/local/tmp/frida-gadget",
                "/data/local/tmp/re.frida.server",
                "/sdcard/frida-gadget",
                "/system/lib/libfrida-gadget.so",
                "/system/lib64/libfrida-gadget.so"
            };
            
            for (String path : fridaPaths) {
                if (new File(path).exists()) {
                    Log.w(TAG, "Frida detected: " + path);
                    return true;
                }
            }
            
            // Check default ports used by Frida
            try {
                java.net.Socket socket = new java.net.Socket("127.0.0.1", 27042);
                socket.close();
                Log.w(TAG, "Frida default port open");
                return true;
            } catch (IOException ignored) {
                // Port not open, good
            }
            
            try {
                java.net.Socket socket = new java.net.Socket("127.0.0.1", 27043);
                socket.close();
                Log.w(TAG, "Frida secondary port open");
                return true;
            } catch (IOException ignored) {
            }
            
            // Check for Xposed
            try {
                ClassLoader.getSystemClassLoader()
                    .loadClass("de.robv.android.xposed.XposedBridge");
                Log.w(TAG, "Xposed detected");
                return true;
            } catch (ClassNotFoundException ignored) {
            }
            
            // Check for Magisk su
            String[] magiskPaths = {
                "/sbin/su",
                "/system/sbin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su",
                "/magisk/.core/bin/su"
            };
            
            for (String path : magiskPaths) {
                File suFile = new File(path);
                if (suFile.exists() && suFile.canExecute()) {
                    // Check if it's MagiskSU
                    try {
                        java.lang.Process p = Runtime.getRuntime().exec(new String[]{path, "--version"});
                        BufferedReader br = new BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                        String line = br.readLine();
                        p.destroy();
                        if (line != null && line.contains("MAGISKSU")) {
                            Log.w(TAG, "MagiskSU detected at: " + path);
                            return true;
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
            
        } catch (Exception e) {
            // Silent fail
        }
        
        return false;
    }

    // ============ PID RESOLUTION ============
    
    /**
     * Get cached PID or rescan if cache expired.
     * Uses randomized cache duration to avoid timing signatures.
     */
    public static int getGamePid(Context ctx) {
    long now = SystemClock.elapsedRealtime();
    long cacheDuration = PID_CACHE_MS + sRandom.nextInt((int) PID_SCAN_JITTER_MS);
    
    if (sCachedPid >= 10 && (now - sLastPidScanTime) < cacheDuration) {
        writeLog("getGamePid: using cached PID=" + sCachedPid);
        return sCachedPid;
    }
    
    writeLog("getGamePid: cache expired or empty, resolving...");
    int pid = resolveGamePid(ctx);
    if (pid >= 10) {
        sCachedPid = pid;
        sLastPidScanTime = now;
        writeLog("getGamePid: resolved PID=" + pid);
    } else {
        writeLog("getGamePid: no PID found");
    }
    
    return pid;
}
    
    /**
     * Force re-scan for game PID.
     */
    public static int refreshGamePid(Context ctx) {
        sCachedPid = 0;
        return getGamePid(ctx);
    }
    
    /**
     * Resolve game PID without file I/O.
     * Uses Android API + stealth /proc checks.
     */
    private static int resolveGamePid(Context ctx) {
        Context app = ctx.getApplicationContext();
        
        // Method 1: ActivityManager (no /proc access)
        int pid = findBgmiPidViaActivityManager(app);
        if (pid >= 10 && isGameProcessValid(pid)) {
            return pid;
        }
        
        // Method 2: Scan /proc with rate limiting
        if (sProcReadsRemaining > 0) {
            pid = findBgmiPidViaProc(app);
            sProcReadsRemaining--;
            if (pid >= 10 && isGameProcessValid(pid)) {
                return pid;
            }
        } else {
            // Reset counter with random delay
            sProcReadsRemaining = PROC_READS_LIMIT + sRandom.nextInt(30);
        }
        
        // Method 3: Check BlackBox process group
        pid = findBgmiPidViaProcessGroup(app);
        if (pid >= 10) {
            return pid;
        }
        
        return 0;
    }
    
    /**
     * Find PID using ActivityManager only (no /proc).
     */
    private static int findBgmiPidViaActivityManager(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return 0;
        
        String host = ctx.getPackageName();
        int best = 0;
        
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return 0;
        
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (info == null || info.pid < 10) continue;
            
            // Exact match
            if (BGMI_PKG.equals(info.processName)) {
                return info.pid;
            }
            
            // BlackBox child process
            if (info.processName != null && info.processName.startsWith(host + ":")) {
                if (info.pkgList != null) {
                    for (String pkg : info.pkgList) {
                        if (BGMI_PKG.equals(pkg)) {
                            return info.pid;
                        }
                    }
                }
                // In BlackBox, child process usually IS the game
                if (best == 0) best = info.pid;
            }
            
            // Check pkgList for any process
            if (info.pkgList != null) {
                for (String pkg : info.pkgList) {
                    if (BGMI_PKG.equals(pkg)) {
                        if (best == 0) best = info.pid;
                    }
                }
            }
        }
        
        return best;
    }
    
    /**
     * Find PID via /proc scanning (rate-limited).
     */
    private static int findBgmiPidViaProc(Context ctx) {
        String host = ctx.getPackageName();
        File procDir = new File("/proc");
        File[] procFiles = procDir.listFiles();
        
        if (procFiles == null) return 0;
        
        // Shuffle to avoid sequential scanning pattern
        shuffleArray(procFiles);
        
        int scanned = 0;
        for (File procFile : procFiles) {
            if (scanned >= 20) break; // Limit per scan
            
            String name = procFile.getName();
            int pid;
            try {
                pid = Integer.parseInt(name);
            } catch (NumberFormatException e) {
                continue;
            }
            
            if (pid < 10) continue;
            
            // Quick check: does cmdline contain package name?
            File cmdlineFile = new File(procFile, "cmdline");
            if (cmdlineFile.canRead()) {
                try (BufferedReader br = new BufferedReader(new FileReader(cmdlineFile))) {
                    String cmdline = br.readLine();
                    if (cmdline != null && (cmdline.contains(host) || cmdline.contains("pubg") || cmdline.contains("PUBG"))) {
                        scanned++;
                        if (hasLibAnogsOptimized(pid)) {
                            return pid;
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Find PID via process group (BlackBox specific).
     */
    private static int findBgmiPidViaProcessGroup(Context ctx) {
        try {
            int myPid = Process.myPid();
            File statusFile = new File("/proc/" + myPid + "/status");
            
            if (!statusFile.canRead()) return 0;
            
            // Read our own process group
            try (BufferedReader br = new BufferedReader(new FileReader(statusFile))) {
                String line;
                int myPgid = -1;
                while ((line = br.readLine()) != null) {
                    // Look for PPid and PGid
                }
            }
            
            // Scan same process group
            File procDir = new File("/proc");
            File[] procFiles = procDir.listFiles();
            if (procFiles == null) return 0;
            
            for (File procFile : procFiles) {
                String name = procFile.getName();
                int pid;
                try {
                    pid = Integer.parseInt(name);
                } catch (NumberFormatException e) {
                    continue;
                }
                
                if (pid == myPid || pid < 10) continue;
                
                // Check if same UID
                File statusF = new File(procFile, "status");
                if (statusF.canRead()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(statusF))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("Uid:")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) {
                                    try {
                                        int uid = Integer.parseInt(parts[1]);
                                        if (uid == Process.myUid() && hasLibAnogsOptimized(pid)) {
                                            return pid;
                                        }
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                                break;
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        
        return 0;
    }
    
    /**
     * Validate game process is alive and has required libs.
     */
    private static boolean isGameProcessValid(int pid) {
        // Check process exists
        File procDir = new File("/proc/" + pid);
        if (!procDir.exists() || !procDir.isDirectory()) {
            return false;
        }
        
        // Quick lib check (optimized - only check one lib)
        return hasLibUe4Optimized(pid);
    }
    
    // ============ LIBRARY CHECKS (Optimized) ============
    
    public static boolean hasLibAnogs(int pid) {
        return hasLibAnogsOptimized(pid);
    }
    
    public static boolean hasLibUe4(int pid) {
        return hasLibUe4Optimized(pid);
    }
    
    private static boolean hasLibAnogsOptimized(int pid) {
        return checkLibInMaps(pid, "libanogs.so");
    }
    
    private static boolean hasLibUe4Optimized(int pid) {
        return checkLibInMaps(pid, "libUE4.so");
    }
    
    /**
     * Optimized library check - reads maps file with early exit.
     */
    private static boolean checkLibInMaps(int pid, String libName) {
    writeLog("checkLibInMaps: checking for " + libName + " in pid " + pid);
    
    File mapsFile = new File("/proc/" + pid + "/maps");
    if (!mapsFile.canRead()) {
        writeLog("❌ Cannot read /proc/" + pid + "/maps");
        return false;
    }
    
    try (BufferedReader reader = new BufferedReader(new FileReader(mapsFile), 8192)) {
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (lineCount > 200) break;
            
            if (line.contains(libName)) {
                if (line.contains("r") && line.length() > 20) {
                    writeLog("✅ Found " + libName + " at line " + lineCount);
                    return true;
                }
            }
        }
    } catch (IOException ignored) {
    }
    
    writeLog("❌ " + libName + " NOT found in pid " + pid);
    return false;
}
    
    public static boolean arePatchLibsReady(int pid) {
        return hasLibAnogsOptimized(pid) && hasLibUe4Optimized(pid);
    }
    
    // ============ LEGACY METHODS (Kept for compatibility) ============
    
    public static int findBgmiPid(Context ctx) {
        return getGamePid(ctx);
    }
    
    public static int findBgmiPidAny(Context ctx) {
        return findBgmiPidViaActivityManager(ctx);
    }
    
    
    
    
    public static int findPidWithReadyLibs(Context ctx) {
    // 🔥 FILE CREATE - Check karo
    try {
        File f = new File("/storage/emulated/0/gamepid_find.txt");
        FileWriter fw = new FileWriter(f);
        fw.write("findPidWithReadyLibs() called at: " + System.currentTimeMillis());
        fw.close();
    } catch (Exception e) {}
    
    writeLog("findPidWithReadyLibs() CALLED");
    
    int pid = getGamePid(ctx);  // ✅ Sirf EK baar
    writeLog("getGamePid returned: " + pid);
    
    if (pid >= 10 && arePatchLibsReady(pid)) {
        writeLog("✅ PID " + pid + " has ready libs");
        return pid;
    }
    writeLog("PID " + pid + " doesn't have ready libs, trying fallback...");
    
    // Fallback: scan all processes
    ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
    if (am == null) {
        writeLog("❌ ActivityManager is null");
        return 0;
    }
    
    String host = ctx.getPackageName();
    writeLog("host package: " + host);
    int fallback = 0;
    
    List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
    if (processes == null) {
        writeLog("❌ processes list is null");
        return 0;
    }
    writeLog("Total processes: " + processes.size());
    
    for (ActivityManager.RunningAppProcessInfo info : processes) {
        if (info == null || info.pid < 10) continue;
        
        if (BGMI_PKG.equals(info.processName)) {
            writeLog("Found BGMI process: " + info.processName + " (pid=" + info.pid + ")");
            if (arePatchLibsReady(info.pid)) {
                writeLog("✅ BGMI process has ready libs, returning " + info.pid);
                return info.pid;
            }
            fallback = info.pid;
        }
        
        if (info.processName != null && info.processName.startsWith(host + ":")) {
            writeLog("Found BlackBox child: " + info.processName + " (pid=" + info.pid + ")");
            if (arePatchLibsReady(info.pid)) {
                writeLog("✅ BlackBox child has ready libs, returning " + info.pid);
                return info.pid;
            }
            if (fallback == 0) fallback = info.pid;
        }
    }
    
    writeLog("Fallback PID: " + fallback);
    return fallback;
}
    
    // ============ SESSION MANAGEMENT ============
    
    public static void resetSession() {
        sPidWrittenForSession = false;
        sCachedPid = 0;
        sProcReadsRemaining = PROC_READS_LIMIT + sRandom.nextInt(30);
        stopRefreshLoop();
    }
    
    public static void startRefreshLoop(Context ctx) {
        stopRefreshLoop();
        final Context app = ctx.getApplicationContext();
        
        sRefreshThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Random sleep between 3-7 seconds
                    long sleepMs = 3000L + sRandom.nextInt(4000);
                    Thread.sleep(sleepMs);
                    
                    // Refresh PID
                    refreshGamePid(app);
                    
                    // Notify native code via shared memory (not file!)
                    int pid = sCachedPid;
                    if (pid >= 10) {
                        writeGamePidFile(app, pid);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "pid-refresh-stealth");
        sRefreshThread.setPriority(Thread.MIN_PRIORITY);
        sRefreshThread.start();
    }
    
    public static void stopRefreshLoop() {
        Thread t = sRefreshThread;
        if (t != null) {
            t.interrupt();
            try {
                t.join(1000);
            } catch (InterruptedException ignored) {
            }
            sRefreshThread = null;
        }
    }
    
    // ============ NATIVE COMMUNICATION ============
    
    /**
     * Notify native code about PID update via JNI.
     * This avoids writing PID to file on disk.
     */
    /**
     * Get PID from native side (alternative to file read).
     */
    public static int getPidFromNative() {
        return nativeGetPid();
    }
    
    private static native int nativeGetPid();
    
    // ============ UTILITY ============
    
    /**
     * Fisher-Yates shuffle to randomize /proc directory traversal.
     */
    private static void shuffleArray(File[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = sRandom.nextInt(i + 1);
            File temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    
    /**
     * Legacy support: write PID file (used during transition).
     * @deprecated Use shared memory communication instead.
     */
    /** Writes {@code files/game.pid} for sock64 (required for ESP + PC aim). */
    public static boolean refreshPidFile(Context ctx) {
        Context app = ctx.getApplicationContext();
        int pid = findPidWithReadyLibs(app);
        if (pid < 10) {
            pid = getGamePid(app);
        }
        if (pid < 10) {
            Log.w(TAG, "refreshPidFile: no BGMI pid");
            return false;
        }
        sCachedPid = pid;
        sLastPidScanTime = SystemClock.elapsedRealtime();
        writeGamePidFile(app, pid);
        return true;
    }

    public static void writePidFileOnce(Context ctx) {
        refreshPidFile(ctx);
        sPidWrittenForSession = true;
    }

    private static void writeGamePidFile(Context app, int pid) {
        try {
            File f = new File(app.getFilesDir(), "game.pid");
            try (FileOutputStream fos = new FileOutputStream(f, false)) {
                fos.write((Integer.toString(pid) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            Log.i(TAG, "game.pid written pid=" + pid);
        } catch (IOException e) {
            Log.w(TAG, "write game.pid failed", e);
        }
    }

    /** Env map for sock64 ProcessBuilder — call before start(). */
    public static void applySockPidEnv(java.util.Map<String, String> env, Context ctx) {
        if (env == null || ctx == null) {
            return;
        }
        refreshPidFile(ctx);
        int pid = getGamePid(ctx.getApplicationContext());
        if (pid < 10) {
            pid = sCachedPid;
        }
        if (pid >= 10) {
            env.put(SOCK_PID_ENV, Integer.toString(pid));
            Log.i(TAG, "XT_GAME_PID=" + pid);
        } else {
            Log.w(TAG, "XT_GAME_PID missing — sock will scan");
        }
    }
    
    /**
     * Find library base address (minimal /proc reads).
     */
    public static long findLibBase(int pid, String libName) {
    writeLog("findLibBase: pid=" + pid + ", libName=" + libName);
    
    File mapsFile = new File("/proc/" + pid + "/maps");
    if (!mapsFile.canRead()) {
        writeLog("❌ Cannot read /proc/" + pid + "/maps");
        return 0;
    }
    
    try (BufferedReader reader = new BufferedReader(new FileReader(mapsFile), 8192)) {
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (!line.contains(libName)) continue;
            
            writeLog("findLibBase: Found " + libName + " at line " + lineCount + ": " + line);
            int dash = line.indexOf('-');
            if (dash <= 0) continue;
            
            try {
                long base = Long.parseLong(line.substring(0, dash).trim(), 16);
                writeLog("findLibBase: " + libName + " base=0x" + Long.toHexString(base));
                return base;
            } catch (NumberFormatException ignored) {
            }
        }
    } catch (IOException ignored) {
        writeLog("❌ IOException reading /proc/" + pid + "/maps");
    }
    
    writeLog("findLibBase: " + libName + " NOT found");
    return 0;
}
    // ============ LOGGING ============
private static void writeLog(String msg) {
    try {
        File logFile = new File("/storage/emulated/0/anogs_log.txt");
        FileWriter fw = new FileWriter(logFile, true);
        fw.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " - [GamePidHelper] " + msg + "\n");
        fw.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}