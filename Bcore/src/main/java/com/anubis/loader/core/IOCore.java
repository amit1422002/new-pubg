package com.anubis.loader.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.text.TextUtils;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.ContainerPathStealth;
import com.anubis.loader.core.GmsCore;
import com.anubis.loader.utils.FileUtils;
import com.anubis.loader.utils.GCloudPathHelper;
import com.anubis.loader.utils.StealthPathRules;
import com.anubis.loader.utils.TrieTree;
import com.anubis.loader.utils.VirtualPathSpoof;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate

/** Created by Milk on 4/9/21. * ∧＿∧ (`･ω･∥ 丶　つ０ しーＪ 此处无Bug */
@SuppressLint("SdCardPath")
public class IOCore {
  public static final String TAG = "IOCore";

  private static final IOCore sIOCore = new IOCore();
  private static final TrieTree mTrieTree = new TrieTree();
  private static final TrieTree sBlackTree = new TrieTree();
  private final Map<String, String> mRedirectMap = new LinkedHashMap<>();

  private static final Map<String, Map<String, String>> sCachePackageRedirect = new HashMap<>();
  private static final ThreadLocal<Boolean> sSkipRedirect = new ThreadLocal<>();
  private static volatile boolean sRedirectEnabled;

  public static void beginInternalRedirect() {
    sSkipRedirect.set(Boolean.TRUE);
  }

  public static void endInternalRedirect() {
    sSkipRedirect.remove();
  }

  public static boolean isInternalRedirectActive() {
    return Boolean.TRUE.equals(sSkipRedirect.get());
  }

  public static IOCore get() {
    return sIOCore;
  }

  public static boolean isRedirectEnabled() {
    return sRedirectEnabled;
  }

  // /data/data/com.google/  ----->  /data/data/com.virtual/data/com.google/
  public void addRedirect(String origPath, String redirectPath) {
    if (TextUtils.isEmpty(origPath)
        || TextUtils.isEmpty(redirectPath)
        || mRedirectMap.get(origPath) != null) return;
    // Add the key to TrieTree
    mTrieTree.add(origPath);
    mRedirectMap.put(origPath, redirectPath);
    File redirectFile = new File(redirectPath);
    if (!redirectFile.exists()) {
      if (redirectPath.endsWith(".apk") || redirectPath.endsWith(".so")) {
        File parent = redirectFile.getParentFile();
        if (parent != null) {
          FileUtils.mkdirs(parent);
        }
      } else {
        FileUtils.mkdirs(redirectPath);
      }
    }
    NativeCore.addIORule(origPath, redirectPath);
  }

  public static boolean pathMayNeedRedirect(String path) {
    if (TextUtils.isEmpty(path)) {
      return false;
    }
    if (path.contains("/" + BEnvironment.VIRTUAL_ROOT_DIR + "/")
        || path.contains("/" + ContainerPathStealth.INTERNAL_CACHE_SEGMENT)) {
      return false;
    }
    if (path.startsWith("/proc/")
        || path.startsWith("/storage/")
        || path.startsWith("/sdcard/")
        || path.startsWith("/data/")
        || path.startsWith("/mnt/")
        || path.startsWith("/data/app/")) {
      return true;
    }
    return path.contains("anubis") || path.contains("blackbox");
  }

  public String redirectPath(String path) {
    if (TextUtils.isEmpty(path)) return path;
    if (isInternalRedirectActive()) {
      return path;
    }
    if (path.contains("/" + BEnvironment.VIRTUAL_ROOT_DIR + "/")
        || path.contains("/" + ContainerPathStealth.INTERNAL_CACHE_SEGMENT)) {
      return path;
    }
    if (!pathMayNeedRedirect(path)) {
      return path;
    }
    for (String real : mRedirectMap.values()) {
      if (!TextUtils.isEmpty(real) && path.startsWith(real)) {
        return path;
      }
    }
    String gcloudBare = GCloudPathHelper.resolveBareFixListPath(path);
    if (gcloudBare != null) {
      return gcloudBare;
    }

    String key = mTrieTree.search(path);
    if (!TextUtils.isEmpty(key)) {
      path = path.replace(key, Objects.requireNonNull(mRedirectMap.get(key)));
    }
    return path;
  }

  public String reversePath(String path) {
    if (TextUtils.isEmpty(path)) {
      return path;
    }
    if (isInternalRedirectActive() || !VirtualPathSpoof.shouldSpoofForGuest()) {
      return path;
    }
    String out = path;
    for (Map.Entry<String, String> entry : mRedirectMap.entrySet()) {
      String real = entry.getValue();
      if (!TextUtils.isEmpty(real) && out.startsWith(real)) {
        String suffix = out.substring(real.length());
        out = entry.getKey() + suffix;
        break;
      }
    }
    return VirtualPathSpoof.reversePath(out);
  }

  public File reversePath(File path) {
    if (path == null) {
      return null;
    }
    return new File(reversePath(path.getAbsolutePath()));
  }

  public File redirectPath(File path) {
    if (path == null) return null;
    String pathStr = path.getAbsolutePath();
    String redirectPath = redirectPath(pathStr);
    if (pathStr.equals(redirectPath)){
      return path;
    }
    return new File(redirectPath);
  }

  public String redirectPath(String path, Map<String, String> rule) {
    if (TextUtils.isEmpty(path)) return path;

    // Search the key from TrieTree
    String key = mTrieTree.search(path);
    if (!TextUtils.isEmpty(key)) path = path.replace(key, Objects.requireNonNull(rule.get(key)));

    return path;
  }

  public File redirectPath(File path, Map<String, String> rule) {
    if (path == null) return null;
    String pathStr = path.getAbsolutePath();
    return new File(redirectPath(pathStr, rule));
  }

  // 由于正常情况Application已完成重定向，以下重定向是怕代码写死。
  public void enableRedirect(Context context) {
    if (context == null || sRedirectEnabled) {
      return;
    }
    Map<String, String> rule = new LinkedHashMap<>();
    String packageName = BActivityThread.getAppPackageName();
    if (TextUtils.isEmpty(packageName)) {
      packageName = context.getPackageName();
    }
    int userId = BActivityThread.getUserId();

    try {
      ApplicationInfo packageInfo = AnubisCore.getBPackageManager()
          .getApplicationInfo(packageName, PackageManager.GET_META_DATA, userId);
      int systemUserId = AnubisCore.getHostUserId();
      String virtualData = BEnvironment.getDataDir(packageName, userId).getAbsolutePath();
      String virtualLib = BEnvironment.resolveNativeLibDir(packageName).getAbsolutePath();
      String virtualDe = BEnvironment.getDeDataDir(packageName, userId).getAbsolutePath();

      // GMS uses real LoadedApk data dirs — canonical-path redirects nest .vfs paths (WorkManager).
      if (!GmsCore.isGoogleAppOrService(packageName)) {
        addPackageDataRedirects(rule, packageName, systemUserId, virtualData, virtualDe, virtualLib);
        ContainerPathStealth.applyGuestRedirects(rule, packageName, userId, systemUserId);
      }

      if (AnubisCore.getContext().getExternalCacheDir() != null
          && context.getExternalCacheDir() != null) {
        File external = BEnvironment.getExternalUserDir(BActivityThread.getUserId());
        // sdcard
        File sdcardAndroidFile = new File("/sdcard/Android");
        String androidDir = String.format("/storage/emulated/%d/Android", systemUserId);
        if (!sdcardAndroidFile.exists()) {
          sdcardAndroidFile = new File(androidDir);
        }
        if (sdcardAndroidFile.exists()) {
          File[] childDirs = sdcardAndroidFile.listFiles(pathname -> pathname.isDirectory());
          if (childDirs != null) {
            for (File childDir : childDirs) {
              rule.put(
                  "/sdcard/Android/" + childDir.getName(),
                  external.getAbsolutePath() + "/Android/" + childDir.getName());
              rule.put(
                  androidDir + "/" + childDir.getName(),
                  external.getAbsolutePath() + "/Android/" + childDir.getName());
            }
          } else {
            rule.put("/sdcard/Android", external.getAbsolutePath() + "/Android");
            rule.put(androidDir, external.getAbsolutePath() + "/Android");
          }
        } else {
          rule.put("/sdcard/Android", external.getAbsolutePath());
          rule.put(androidDir, external.getAbsolutePath());
        }
        // Loader copies used /localhost/Android/obb before user/0 alignment; forward reads.
        File legacyObbRoot = new File(BEnvironment.getExternalVirtualRoot(), "Android/obb");
        File userObbRoot = new File(external, "Android/obb");
        String legacyPath = legacyObbRoot.getAbsolutePath();
        String userPath = userObbRoot.getAbsolutePath();
        if (!legacyPath.equals(userPath)) {
          rule.put(legacyPath, userPath);
          rule.put(legacyPath + "/", userPath + "/");
        }
      }
      if (AnubisCore.get().isHideRoot()) {
        hideRoot(rule);
      }
      hideEmulatorTraces(rule);
      // ProcStealthHelper disabled — do not redirect /proc to fake mirror files.
      if (VirtualPathSpoof.isStealthAcPackage(packageName)
          && !GmsCore.isGoogleAppOrService(packageName)) {
        rule.put(VirtualPathSpoof.fakeApkPath(packageName),
            BEnvironment.getBaseApkDir(packageName).getAbsolutePath());
        rule.put(VirtualPathSpoof.fakeNativeLibDir(packageName), virtualLib);
        StealthPathRules.addRules(packageName, userId, rule);
        StealthPathRules.addFakePathRules(packageName, userId, packageInfo, rule);
        StealthPathRules.addHostLeakGuards(packageName, userId, rule);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    for (String key : rule.keySet()) {
      get().addRedirect(key, rule.get(key));
    }
    NativeCore.enableIO();
    sRedirectEnabled = true;
  }

  private static void addPackageDataRedirects(
      Map<String, String> rule,
      String packageName,
      int systemUserId,
      String dataDir,
      String deDataDir,
      String libDir) {
    rule.put(String.format("/data/data/%s/lib", packageName), libDir);
    rule.put(String.format("/data/user/%d/%s/lib", systemUserId, packageName), libDir);
    rule.put(String.format("/data/data/%s", packageName), dataDir);
    rule.put(String.format("/data/user/%d/%s", systemUserId, packageName), dataDir);
    rule.put(String.format("/data/user_de/%d/%s", systemUserId, packageName), deDataDir);
  }

  private void hideRoot(Map<String, String> rule) {
    rule.put("/system/app/Superuser.apk", "/system/app/Superuser.apk-fake");
    rule.put("/sbin/su", "/sbin/su-fake");
    rule.put("/system/bin/su", "/system/bin/su-fake");
    rule.put("/system/xbin/su", "/system/xbin/su-fake");
    rule.put("/data/local/xbin/su", "/data/local/xbin/su-fake");
    rule.put("/data/local/bin/su", "/data/local/bin/su-fake");
    rule.put("/system/sd/xbin/su", "/system/sd/xbin/su-fake");
    rule.put("/system/bin/failsafe/su", "/system/bin/failsafe/su-fake");
    rule.put("/data/local/su", "/data/local/su-fake");
    rule.put("/su/bin/su", "/su/bin/su-fake");
  }

  private void hideEmulatorTraces(Map<String, String> rule) {
    rule.put("/dev/qemu_pipe", "/dev/qemu_pipe-fake");
    rule.put("/dev/goldfish_pipe", "/dev/goldfish_pipe-fake");
    rule.put("/dev/socket/qemud", "/dev/socket/qemud-fake");
    rule.put("/system/lib/libc_malloc_debug_qemu.so", "/system/lib/libc_malloc_debug_qemu.so-fake");
    rule.put("/system/lib64/libc_malloc_debug_qemu.so", "/system/lib64/libc_malloc_debug_qemu.so-fake");
    rule.put("/system/bin/qemu-props", "/system/bin/qemu-props-fake");
    rule.put("/sys/qemu_trace", "/sys/qemu_trace-fake");
    rule.put("/system/bin/microvirt-prop", "/system/bin/microvirt-prop-fake");
  }

  private void proc(Map<String, String> rule) {
    int appPid = BActivityThread.getAppPid();
    if (appPid < 0) {
      return;
    }
    int pid = Process.myPid();
    File procDir = BEnvironment.getProcDir(appPid);
    String procBase = procDir.getAbsolutePath() + "/";

    rule.put("/proc/" + pid + "/cmdline", procBase + "cmdline");
    rule.put("/proc/self/cmdline", procBase + "cmdline");
    rule.put("/proc/" + pid + "/comm", procBase + "comm");
    rule.put("/proc/self/comm", procBase + "comm");
    rule.put("/proc/" + pid + "/status", procBase + "status");
    rule.put("/proc/self/status", procBase + "status");
    rule.put("/proc/" + pid + "/maps", procBase + "maps");
    rule.put("/proc/self/maps", procBase + "maps");
    rule.put("/proc/thread-self/maps", procBase + "maps");
    rule.put("/proc/" + pid + "/smaps", procBase + "maps");
    rule.put("/proc/self/smaps", procBase + "maps");
    rule.put("/proc/thread-self/smaps", procBase + "maps");
    rule.put("/proc/" + pid + "/smaps_rollup", procBase + "maps");
    rule.put("/proc/self/smaps_rollup", procBase + "maps");
    rule.put("/proc/thread-self/smaps_rollup", procBase + "maps");
    rule.put("/proc/" + pid + "/mountinfo", procBase + "mountinfo");
    rule.put("/proc/self/mountinfo", procBase + "mountinfo");
    rule.put("/proc/" + pid + "/cgroup", procBase + "cgroup");
    rule.put("/proc/self/cgroup", procBase + "cgroup");
    rule.put("/proc/" + pid + "/environ", procBase + "environ");
    rule.put("/proc/self/environ", procBase + "environ");
  }
}
