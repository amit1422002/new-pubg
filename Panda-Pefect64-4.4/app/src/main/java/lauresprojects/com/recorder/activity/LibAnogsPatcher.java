package lauresprojects.com.recorder.activity;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.topjohnwu.superuser.Shell;

import java.io.IOException;
import java.io.RandomAccessFile;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import lauresprojects.com.recorder.activity.BgmiLibUe4Patcher;
import lauresprojects.com.recorder.activity.MemPatchUtil;
import android.system.Os;
import android.system.OsConstants;
import android.system.ErrnoException;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
/**
 * Loader-side: libanogs + libUE4 byte patches, plus THUNDER + PRIVATE_SRC_FILES hooks (not BCore).
 */
public final class LibAnogsPatcher {

    private static final boolean PATCHING_ENABLED = true;

    private static final String LIB_ANOGS = "libanogs.so";
    private static final String LIB_UE4 = "libUE4.so";

    private static final String ANOGS_HOOK_SO = "libanogshook.so";
    private static final String ANOGS_HOOK_TMP = "/data/local/tmp/libanogshook.so";
    private static final String UE4_HOOK_SO = "libue4privhook.so";
    private static final String UE4_HOOK_TMP = "/data/local/tmp/libue4privhook.so";
 
    private static final boolean ENABLE_UE4_PRIVATE_SRC_HOOK = false;

  //  private static final long UE4_PRIVATE_SRC_OFFSET = 0xC4DFB90L;

    private static final Patch[] PATCHES_ANOGS = {
            // 0x37F2CC — guest Dobby hook return 0 (not host byte patch)
    };

    private static final Patch[] PATCHES_UE4 = {
        //patchUe4(0x4, "00 00 80 D2 C0 03 5F D6"),
    };
    
    // ============ LOGGING (logcat tag BgmiBypass — live dekho) ============
    private static final String LOG_TAG = "BgmiBypass";

    private static void writeLog(String msg) {
        Log.i(LOG_TAG, msg);
        try {
            File logFile = new File("/storage/emulated/0/anogs_log.txt");
            FileWriter fw = new FileWriter(logFile, true);
            fw.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " - " + msg + "\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int POLL_MS = 500;
    private static final int ANOGS_FAST_POLL_MS = 50;
    private static final int MAX_WAIT_MS = 5 * 60 * 1000;

    private static volatile boolean patchApplied;
    private static volatile boolean workerRunning;
    private static volatile boolean ue4HookInjected;

    static {
        System.loadLibrary("safecheat");
    }

    private LibAnogsPatcher() {
    }

    public static void resetSession() {
        writeLog("resetSession() CALLED");
        patchApplied = false;
        ue4HookInjected = false;
    }
    
    
    
    
    
    
    

    public static void startOnGameLaunch(Context context) {
        if (!PATCHING_ENABLED) {
            return;
        }
        logPatchManifest();
    try {
        File f = new File("/storage/emulated/0/libanogs_start.txt");
        FileWriter fw = new FileWriter(f);
        fw.write("startOnGameLaunch() called at: " + System.currentTimeMillis());
        fw.close();
    } catch (Exception e) {}
    
    writeLog("========================================");
    writeLog("startOnGameLaunch() CALLED");
    
        writeLog("========================================");
        writeLog("startOnGameLaunch() CALLED");
        writeLog("context: " + (context != null ? "NOT NULL" : "NULL"));
        
        if (context == null) {
            writeLog("❌ context is NULL, returning");
            return;
        }
        
        final Context app = context.getApplicationContext();
        writeLog("app: " + (app != null ? "NOT NULL" : "NULL"));
        
        synchronized (LibAnogsPatcher.class) {
            if (workerRunning) {
                writeLog("⚠️ workerRunning is TRUE, returning (already running)");
                return;
            }
            writeLog("Starting new patcher thread...");
            patchApplied = false;
            ue4HookInjected = false;
            workerRunning = true;
        }
        
        new Thread(() -> {
            try {
                writeLog("🟢 Patcher thread STARTED");
                waitAndPatch(app);
            } finally {
                workerRunning = false;
                writeLog("🔴 Patcher thread FINISHED");
            }
        }, "libanogs-patcher").start();
    }





    private static void waitAndPatch(Context ctx) {
    try {
        File f = new File("/storage/emulated/0/libanogs_wait.txt");
        FileWriter fw = new FileWriter(f);
        fw.write("waitAndPatch() called at: " + System.currentTimeMillis());
        fw.close();
    } catch (Exception e) {}
        writeLog("waitAndPatch() STARTED");
        writeLog("Root access: " + Shell.rootAccess());
        writeLog("Poll interval: " + POLL_MS + "ms, Max wait: " + MAX_WAIT_MS + "ms");
        
        final long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        int loopCount = 0;
        
        while (!patchApplied && System.currentTimeMillis() < deadline) {
            loopCount++;
            writeLog("--- Loop #" + loopCount + " ---");
            
            int pid = GamePidHelper.findPidWithReadyLibs(ctx);
            writeLog("PID from GamePidHelper: " + pid);
            
            if (pid >= 10) {
                writeLog("✅ PID >= 10, finding lib bases...");
                long anogsBase = GamePidHelper.findLibBase(pid, LIB_ANOGS);
                long ue4Base = GamePidHelper.findLibBase(pid, LIB_UE4);
                writeLog("anogsBase: 0x" + Long.toHexString(anogsBase));
                writeLog("ue4Base: 0x" + Long.toHexString(ue4Base));

                // Anogs 0x37F2CC = guest Dobby return-0 hook (no host byte patch)
                if (ue4Base != 0) {
                    writeLog("⚡ UE4 patches NOW (start, no delay)");
                    if (applyUe4PatchesOnly(ctx, pid, ue4Base)) {
                        patchApplied = true;
                        writeLog("🎉🎉🎉 PATCHES APPLIED SUCCESSFULLY! 🎉🎉🎉");
                        showToast(ctx, "Bypass OK — UE4 x"
                                + (PATCHES_UE4.length + BgmiLibUe4Patcher.collectRuntimePatches().length)
                                + " | anogs hooks/ELF=OFF"
                                + " UE4hook=" + (ENABLE_UE4_PRIVATE_SRC_HOOK ? "on" : "off"));
                        return;
                    } else {
                        writeLog("❌ UE4 patches returned FALSE");
                    }
                } else {
                    writeLog("⚠️ waiting libs: anogsBase=" + anogsBase + ", ue4Base=" + ue4Base);
                }
            } else {
                writeLog("⚠️ No valid PID found (pid=" + pid + ")");
            }
            
            try {
                Thread.sleep(ANOGS_FAST_POLL_MS);
            } catch (InterruptedException e) {
                writeLog("Thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        writeLog("⏰ TIMEOUT - patchApplied=" + patchApplied);
        if (!patchApplied) {
            writeLog("❌ BYPASS TIMEOUT - patches not applied");
            showToast(ctx, "Bypass Timeout");
        }
    }

    private static void showToast(Context ctx, String message) {
        writeLog("showToast: " + message);
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show());
    }

    private static boolean applyUe4PatchesOnly(Context ctx, int pid, long ue4Base) {
        writeLog("applyUe4PatchesOnly() STARTED");

        if (!applyPatchesForLib(pid, ue4Base, LIB_UE4, PATCHES_UE4)) {
            writeLog("❌ UE4 static patches fail");
            return false;
        }

        if (!applyUe4RuntimePatches(pid, ue4Base)) {
            writeLog("❌ UE4 runtime patches fail");
            return false;
        }

        if (!applyUe4PrivateSrcHook(ctx, pid, ue4Base)) {
            writeLog("❌ UE4 private src hook fail");
            return false;
        }

        writeLog("✅ UE4 SAB APPLY HO GAYA!");
        return true;
    }

    private static boolean applyAllPatches(Context ctx, int pid, long anogsBase, long ue4Base) {
    writeLog("applyAllPatches() STARTED");
    
    // 1. Anogs patches apply karo
    if (!applyPatchesForLib(pid, anogsBase, LIB_ANOGS, PATCHES_ANOGS)) {
        writeLog("❌ Anogs patches fail");
        return false;
    }
    
    // 2. UE4 static patches apply karo
    if (!applyPatchesForLib(pid, ue4Base, LIB_UE4, PATCHES_UE4)) {
        writeLog("❌ UE4 patches fail");
        return false;
    }
    
    // 4. 🔥 NAYA: UE4 runtime patches apply karo (BgmiLibUe4Patcher se)
    if (!applyUe4RuntimePatches(pid, ue4Base)) {
        writeLog("❌ UE4 runtime patches fail");
        return false;
    }
    
    // 5. UE4 private src hook apply karo
    if (!applyUe4PrivateSrcHook(ctx, pid, ue4Base)) {
        writeLog("❌ UE4 private src hook fail");
        return false;
    }
    
    writeLog("✅ SAB KUCH APPLY HO GAYA!");
    return true;
}

// Yeh method BgmiLibUe4Patcher ke saare patches apply karega
private static boolean applyUe4RuntimePatches(int pid, long ue4Base) {
    writeLog("🚀 UE4 Runtime Patches apply kar rahe hain...");
    
    if (ue4Base == 0) {
        writeLog("❌ ue4Base 0 hai, return");
        return false;
    }
    
    // BgmiLibUe4Patcher se saare patches le lo
    MemPatchUtil.Patch[] runtimePatches = BgmiLibUe4Patcher.collectRuntimePatches();
    writeLog("✅ " + runtimePatches.length + " runtime patches mile");
    
    // Har ek patch ko apply karo
    for (int i = 0; i < runtimePatches.length; i++) {
        MemPatchUtil.Patch patch = runtimePatches[i];
        long addr = ue4Base + patch.offset;
        
        writeLog("📌 Patch #" + i + ": " + patch.name + 
                " @ 0x" + Long.toHexString(addr));
        
        // Patch ki bytes ko memory mein likho
        if (!writePatch(pid, addr, patch.bytes)) {
            writeLog("❌ Patch #" + i + " fail ho gaya!");
            return false;
        }
        writeLog("✅ Patch #" + i + " successfully apply");
    }
    
    writeLog("🎉 Saare " + runtimePatches.length + " patches apply ho gaye!");
    return true;
}

    private static boolean applyUe4PrivateSrcHook(Context ctx, int pid, long ue4Base) {
        if (!ENABLE_UE4_PRIVATE_SRC_HOOK) {
            writeLog("applyUe4PrivateSrcHook: disabled");
            return true;
        }
        writeLog("applyUe4PrivateSrcHook: ENABLED");
        if (ue4Base == 0) {
            writeLog("❌ ue4Base is 0");
            return false;
        }
        if (ue4HookInjected) {
            writeLog("ue4HookInjected already true");
            return true;
        }
        if (!prepareHookLibrary(ctx, UE4_HOOK_SO, UE4_HOOK_TMP)) {
            writeLog("❌ prepareHookLibrary failed for " + UE4_HOOK_SO);
            return false;
        }
        if (!nativeInjectHookSo(pid, UE4_HOOK_TMP)) {
            writeLog("❌ nativeInjectHookSo failed");
            return false;
        }
        ue4HookInjected = true;
        writeLog("✅ UE4 private src hook injected successfully");
        return true;
    }

    private static boolean prepareHookLibrary(Context ctx, String soFileName, String tmpPath) {
        ApplicationInfo ai = ctx.getApplicationInfo();
        if (ai == null || ai.nativeLibraryDir == null) {
            writeLog("prepareHookLibrary: ai is null or nativeLibraryDir is null");
            return false;
        }
        String src = ai.nativeLibraryDir + "/" + soFileName;
        writeLog("prepareHookLibrary: src=" + src + ", tmpPath=" + tmpPath);
        if (!Shell.rootAccess()) {
            writeLog("prepareHookLibrary: NO ROOT ACCESS");
            return false;
        }
        String cmd = "cp \"" + src + "\" \"" + tmpPath + "\" && chmod 755 \"" + tmpPath + "\"";
        writeLog("prepareHookLibrary: executing: " + cmd);
        Shell.Result r = Shell.su(cmd).exec();
        writeLog("prepareHookLibrary: result success=" + r.isSuccess());
        return r.isSuccess();
    }

    private static void logPatchManifest() {
        writeLog("========== BYPASS MANIFEST ==========");
        writeLog("ANOGS byte patches: " + PATCHES_ANOGS.length + " (0x37F2CC = guest hook return 0)");
        for (int i = 0; i < PATCHES_ANOGS.length; i++) {
            Patch p = PATCHES_ANOGS[i];
            writeLog("  [ANOGS #" + i + "] offset=0x" + Long.toHexString(p.offset)
                    + " bytes=" + bytesToHex(p.bytes));
        }
        writeLog("UE4 static patches: " + PATCHES_UE4.length);
        for (int i = 0; i < PATCHES_UE4.length; i++) {
            Patch p = PATCHES_UE4[i];
            writeLog("  [UE4 static #" + i + "] offset=0x" + Long.toHexString(p.offset)
                    + " bytes=" + bytesToHex(p.bytes));
        }
        MemPatchUtil.Patch[] runtime = BgmiLibUe4Patcher.collectRuntimePatches();
        writeLog("UE4 runtime patches: " + runtime.length + " (apply at start, no delay)");
        for (int i = 0; i < runtime.length; i++) {
            MemPatchUtil.Patch p = runtime[i];
            writeLog("  [UE4 runtime #" + i + "] " + p.name + " offset=0x"
                    + Long.toHexString(p.offset) + " bytes=" + bytesToHex(p.bytes));
        }
        writeLog("ANOGS RET0/THUNDER hooks: OFF");
        writeLog("ANOGS ELF destroy: OFF");
        writeLog("=====================================");
    }

    private static native boolean nativeInjectHookSo(int pid, String hookSoPath);

    private static boolean applyPatchesForLib(int pid, long base, String libName, Patch[] patches) {
        writeLog("applyPatchesForLib: " + libName + ", base=0x" + Long.toHexString(base) + ", patches=" + (patches != null ? patches.length : 0));
        
        if (patches == null || patches.length == 0) {
            writeLog("No patches for " + libName);
            return true;
        }
        
        for (int i = 0; i < patches.length; i++) {
            Patch p = patches[i];
            long addr = base + p.offset;
            writeLog("Patch #" + i + ": offset=0x" + Long.toHexString(p.offset) + ", addr=0x" + Long.toHexString(addr) + ", bytes=" + bytesToHex(p.bytes));
            
            if (!writePatch(pid, addr, p.bytes)) {
                writeLog("❌ Failed to write patch #" + i + " at 0x" + Long.toHexString(addr));
                return false;
            }
            writeLog("✅ Patch #" + i + " written successfully at 0x" + Long.toHexString(addr));
        }
        
        writeLog("All " + patches.length + " patches applied for " + libName);
        return true;
    }

    private static boolean writePatch(int pid, long address, byte[] bytes) {
    writeLog("writePatch: Writing at 0x" + Long.toHexString(address) + " size=" + bytes.length);
    
    // Try 1: Direct write
    boolean success = writeProcMem(pid, address, bytes);
    if (success) {
        writeLog("✅ Direct write successful");
        flushCache(pid, address, bytes.length);
        return true;
    }
    
    // Try 2: ProcessVmAware
    writeLog("⚠️ Direct write failed, trying ProcessVmAware...");
    success = writeWithProcessVmAware(pid, address, bytes);
    if (success) {
        writeLog("✅ ProcessVmAware write successful");
        flushCache(pid, address, bytes.length);
        return true;
    }
    
    // Try 3: FileStream
    writeLog("⚠️ ProcessVmAware failed, trying FileStream...");
    success = writeWithFileStream(pid, address, bytes);
    if (success) {
        writeLog("✅ FileStream write successful");
        flushCache(pid, address, bytes.length);
        return true;
    }
    
    // Try 4: Root
    if (Shell.rootAccess()) {
        writeLog("⚠️ All methods failed, trying root...");
        success = writeProcMemAsRoot(pid, address, bytes);
        if (success) {
            writeLog("✅ Root write successful");
            flushCache(pid, address, bytes.length);
            return true;
        }
    }
    
    writeLog("❌ ALL write methods FAILED for 0x" + Long.toHexString(address));
    return false;
}
    private static boolean writeProcMem(int pid, long address, byte[] bytes) {
        String path = "/proc/" + pid + "/mem";
        try (RandomAccessFile mem = new RandomAccessFile(path, "rw")) {
            mem.seek(address);
            mem.write(bytes);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean writeProcMemAsRoot(int pid, long address, byte[] bytes) {
        StringBuilder printf = new StringBuilder("printf '");
        for (byte b : bytes) {
            printf.append(String.format("\\x%02x", b & 0xFF));
        }
        printf.append("' | dd of=/proc/").append(pid)
                .append("/mem bs=1 seek=").append(address)
                .append(" count=").append(bytes.length)
                .append(" conv=notrunc 2>/dev/null");
        String cmd = printf.toString();
        writeLog("writeProcMemAsRoot: executing: " + cmd);
        Shell.Result r = Shell.su(cmd).exec();
        writeLog("writeProcMemAsRoot: result success=" + r.isSuccess());
        return r.isSuccess();
    }

// ============ PROCESS VMAWARE METHODS (Add after writeProcMemAsRoot) ============

private static boolean writeWithProcessVmAware(int pid, long address, byte[] bytes) {
    try {
        FileDescriptor fd = Os.open("/proc/" + pid + "/mem", OsConstants.O_RDWR, 0);
        Os.lseek(fd, address, OsConstants.SEEK_SET);
        Os.write(fd, bytes, 0, bytes.length);
        Os.close(fd);
        writeLog("✅ ProcessVmAware: Wrote " + bytes.length + " bytes");
        return true;
    } catch (ErrnoException e) {
        writeLog("❌ ProcessVmAware failed: errno=" + e.errno);
        return false;
    } catch (Exception e) {
        writeLog("❌ ProcessVmAware exception: " + e.getMessage());
        return false;
    }
}

private static boolean writeWithFileStream(int pid, long address, byte[] bytes) {
    try {
        FileOutputStream fos = new FileOutputStream("/proc/" + pid + "/mem");
        FileChannel channel = fos.getChannel();
        channel.position(address);
        channel.write(ByteBuffer.wrap(bytes));
        channel.close();
        fos.close();
        writeLog("✅ FileStream: Wrote " + bytes.length + " bytes");
        return true;
    } catch (Exception e) {
        writeLog("❌ FileStream failed: " + e.getMessage());
        return false;
    }
}

private static void flushCache(int pid, long address, int size) {
    try {
        String cmd = "su -c 'echo \"" + address + " " + (address + size) + "\" > /proc/" + pid + "/clear_cache'";
        Shell.Result r = Shell.su(cmd).exec();
        writeLog("flushCache: result=" + r.isSuccess());
    } catch (Exception e) {
        writeLog("flushCache EXCEPTION: " + e.getMessage());
    }
}


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private static Patch patch(long offset, String hex) {
        return new Patch(offset, parseHex(hex));
    }

    private static Patch patchUe4(long offset, String hex) {
        return new Patch(offset, parseHex(hex));
    }

    private static byte[] parseHex(String hex) {
        String[] parts = hex.trim().split("\\s+");
        byte[] out = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return out;
    }

    private static final class Patch {
        final long offset;
        final byte[] bytes;

        Patch(long offset, byte[] bytes) {
            this.offset = offset;
            this.bytes = bytes;
        }
    }
}