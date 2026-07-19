package com.anubis.loader.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.GamePackages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * GCloud / Dolphin / Puffer paths for Delta Force stealth guests (valo loader parity).
 */
public final class GCloudPathHelper {

    private static final String TAG = "GCloudPathHelper";
    private static final String SEED_MARKER = ".anubis_gcloud_seed_v1";
    private static final String UE_SAVED = "UE4Game/DeltaForce/DeltaForce/Saved";

    private GCloudPathHelper() {
    }

    public static boolean isDeltaForcePackage(String packageName) {
        return GamePackages.isDeltaForce(packageName);
    }

    /** GCloud opens bare {@code /fix_list.json} when CWD is root — resolve to apollo dirs. */
    public static String resolveBareFixListPath(String path) {
        if (path == null) {
            return null;
        }
        if (!"/fix_list.json".equals(path) && !"fix_list.json".equals(path)) {
            return null;
        }
        String packageName = BActivityThread.getAppPackageName();
        if (!isDeltaForcePackage(packageName)) {
            return null;
        }
        int userId = BActivityThread.getUserId();
        for (File candidate : fixListCandidates(packageName, userId)) {
            if (candidate.isFile() && candidate.length() > 0L) {
                return candidate.getAbsolutePath();
            }
        }
        File fileList = findFileListJson(packageName, userId);
        if (fileList != null) {
            return fileList.getAbsolutePath();
        }
        return null;
    }

    public static void ensureResourceAccess(String packageName, int userId) {
        if (!isDeltaForcePackage(packageName)) {
            return;
        }
        try {
            File saved = new File(BEnvironment.getDataFilesDir(packageName, userId), UE_SAVED);
            File dolphinRoot = new File(saved, "Dolphin");
            File pufferRoot = new File(saved, "Puffer");
            File contentRoot = new File(BEnvironment.getDataFilesDir(packageName, userId),
                    "UE4Game/DeltaForce/DeltaForce/Content");
            mirrorInternalSavedToExternal(packageName, userId);
            ensureMovieDirs(dolphinRoot, pufferRoot, contentRoot);
            publishGCloudConfigFiles(packageName, userId, dolphinRoot);
            installFixListCopies(packageName, userId);
            chmodResourceTree(saved);
            File extFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
            chmodResourceTree(extFiles);
            Slog.i(TAG, "resource access ready pkg=" + packageName
                    + " extData=" + BEnvironment.getExternalDataDir(packageName, userId).getAbsolutePath());
        } catch (Throwable t) {
            Slog.w(TAG, "ensureResourceAccess failed pkg=" + packageName + ": " + t.getMessage());
        }
    }

    private static void mirrorInternalSavedToExternal(String packageName, int userId) throws IOException {
        File intFiles = BEnvironment.getDataFilesDir(packageName, userId);
        File extFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
        File intUe4 = new File(intFiles, "UE4Game");
        File extUe4 = new File(extFiles, "UE4Game");
        if (!intUe4.isDirectory()) {
            return;
        }
        boolean copied = mergeTree(intUe4, extUe4, false);
        if (copied) {
            Slog.i(TAG, "mirrored UE4Game to external pkg=" + packageName
                    + " dest=" + extUe4.getAbsolutePath());
        }
    }

    public static List<File> fixListCandidates(String packageName, int userId) {
        List<File> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        addFixListDir(out, seen, BEnvironment.getExternalDataFilesDir(packageName, userId));
        addFixListDir(out, seen, BEnvironment.getDataFilesDir(packageName, userId));
        File dolphinRoot = new File(BEnvironment.getDataFilesDir(packageName, userId), UE_SAVED + "/Dolphin");
        File[] versions = dolphinRoot.listFiles();
        if (versions != null) {
            for (File version : versions) {
                if (version != null && version.isDirectory()) {
                    addFixListFile(out, seen, new File(version, "fix_list.json"));
                }
            }
        }
        for (File hostRoot : resolveHostExternalRoots(packageName)) {
            addFixListDir(out, seen, new File(hostRoot, "files"));
        }
        return out;
    }

    private static void addFixListDir(List<File> out, Set<String> seen, File root) {
        if (root == null || !root.isDirectory()) {
            return;
        }
        addFixListFile(out, seen, new File(root, "fix_list.json"));
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child != null && child.isDirectory() && child.getName().startsWith("App.")) {
                addFixListFile(out, seen, new File(child, "fix_list.json"));
            }
        }
    }

    private static void addFixListFile(List<File> out, Set<String> seen, File file) {
        if (file == null) {
            return;
        }
        String key = file.getAbsolutePath();
        if (seen.add(key)) {
            out.add(file);
        }
    }

    public static void seedFromHostIfNeeded(String packageName, int userId) {
        if (!isDeltaForcePackage(packageName)) {
            return;
        }
        File guestDataFiles = BEnvironment.getDataFilesDir(packageName, userId);
        File guestDolphin = new File(guestDataFiles, UE_SAVED + "/Dolphin");
        File guestPuffer = new File(guestDataFiles, UE_SAVED + "/Puffer");
        File marker = new File(BEnvironment.getExternalDataDir(packageName, userId), SEED_MARKER);
        if (marker.isFile() && !needsDolphinSeed(guestDolphin) && !needsPufferSeed(guestPuffer)) {
            installFixListCopies(packageName, userId);
            return;
        }
        try {
            boolean copied = copyHostGCloudTree(packageName, userId);
            installFixListCopies(packageName, userId);
            if (copied || !needsDolphinSeed(guestDolphin) || !needsPufferSeed(guestPuffer)) {
                FileUtils.writeToFile(new byte[]{'1'}, marker);
                Slog.i(TAG, "seeded GCloud resources pkg=" + packageName
                        + " dolphinReady=" + !needsDolphinSeed(guestDolphin)
                        + " pufferReady=" + !needsPufferSeed(guestPuffer));
            } else {
                Slog.w(TAG, "host GCloud seed skipped (no readable host data) pkg=" + packageName);
            }
        } catch (Throwable t) {
            Slog.w(TAG, "seed failed pkg=" + packageName + ": " + t.getMessage());
        }
    }

    private static boolean copyHostGCloudTree(String packageName, int userId) throws IOException {
        boolean copied = false;
        File guestExtFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
        File guestDataFiles = BEnvironment.getDataFilesDir(packageName, userId);
        File guestDolphin = new File(guestDataFiles, UE_SAVED + "/Dolphin");
        File guestPuffer = new File(guestDataFiles, UE_SAVED + "/Puffer");

        for (File hostRoot : resolveHostExternalRoots(packageName)) {
            File hostFiles = new File(hostRoot, "files");
            if (hostFiles.isDirectory()) {
                copied |= mergeTree(hostFiles, guestExtFiles, true);
                File hostSaved = new File(hostFiles, UE_SAVED);
                if (hostSaved.isDirectory()) {
                    File guestSaved = new File(guestDataFiles, UE_SAVED);
                    copied |= mergeTree(hostSaved, guestSaved, false);
                }
            }
        }

        for (File hostRoot : resolveHostReadableDataRoots(packageName)) {
            File hostSaved = new File(hostRoot, UE_SAVED);
            File hostDolphin = new File(hostSaved, "Dolphin");
            File hostPuffer = new File(hostSaved, "Puffer");
            if (needsDolphinSeed(guestDolphin) && hostDolphin.isDirectory()) {
                copied |= mergeTree(hostDolphin, guestDolphin, false);
            }
            if (needsPufferSeed(guestPuffer) && hostPuffer.isDirectory()) {
                copied |= mergeTree(hostPuffer, guestPuffer, false);
            }
        }
        return copied;
    }

    private static void installFixListCopies(String packageName, int userId) {
        try {
            File source = findFirstFixList(resolveHostReadableDataRoots(packageName),
                    resolveHostExternalRoots(packageName));
            if (source == null) {
                source = findFirstFixListFromGuests(packageName, userId);
            }
            if (source == null) {
                source = findFileListJson(packageName, userId);
            }
            if (source == null) {
                return;
            }
            for (File dest : fixListCandidates(packageName, userId)) {
                File parent = dest.getParentFile();
                if (parent != null) {
                    FileUtils.mkdirs(parent);
                }
                if (!dest.isFile() || dest.length() == 0L) {
                    FileUtils.copyFile(source, dest);
                }
            }
        } catch (IOException e) {
            Slog.w(TAG, "fix_list copy failed pkg=" + packageName + ": " + e.getMessage());
        }
    }

    private static File findFileListJson(String packageName, int userId) {
        File dolphinRoot = new File(BEnvironment.getDataFilesDir(packageName, userId), UE_SAVED + "/Dolphin");
        File[] versions = dolphinRoot.listFiles();
        if (versions == null) {
            return null;
        }
        File newest = null;
        for (File version : versions) {
            if (version == null || !version.isDirectory()) {
                continue;
            }
            File fileList = new File(version, "filelist.json");
            if (!fileList.isFile() || fileList.length() == 0L) {
                continue;
            }
            if (newest == null || version.getName().compareTo(newest.getName()) > 0) {
                newest = fileList;
            }
        }
        return newest;
    }

    private static void publishGCloudConfigFiles(String packageName, int userId, File dolphinRoot) throws IOException {
        File fileList = findFileListJson(packageName, userId);
        if (fileList == null) {
            return;
        }
        String appDirName = detectAppDirName(dolphinRoot);
        File extFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
        File appDir = new File(extFiles, appDirName);
        FileUtils.mkdirs(appDir);
        copyIfMissing(fileList, new File(appDir, "fix_list.json"));
        copyIfMissing(fileList, new File(appDir, "filelist.json"));
        copyIfMissing(fileList, new File(extFiles, "fix_list.json"));
        File dolphinVersion = fileList.getParentFile();
        if (dolphinVersion != null) {
            File resList = new File(dolphinVersion, "apollo_reslist.flist");
            if (resList.isFile()) {
                copyIfMissing(resList, new File(appDir, "apollo_reslist.flist"));
            }
            copyIfMissing(fileList, new File(dolphinVersion, "fix_list.json"));
        }
    }

    private static String detectAppDirName(File dolphinRoot) {
        File[] versions = dolphinRoot != null ? dolphinRoot.listFiles() : null;
        if (versions != null) {
            String best = null;
            for (File version : versions) {
                if (version == null || !version.isDirectory()) {
                    continue;
                }
                String name = version.getName();
                if (best == null || name.compareTo(best) > 0) {
                    best = name;
                }
            }
            if (best != null) {
                return "App." + best;
            }
        }
        return "App.1.202.37112.5401";
    }

    private static void copyIfMissing(File source, File dest) throws IOException {
        if (source == null || !source.isFile() || dest == null) {
            return;
        }
        File parent = dest.getParentFile();
        if (parent != null) {
            FileUtils.mkdirs(parent);
        }
        if (!dest.isFile() || dest.length() == 0L || dest.length() < source.length()) {
            FileUtils.copyFile(source, dest);
        }
    }

    private static void ensureMovieDirs(File dolphinRoot, File pufferRoot, File contentRoot) {
        FileUtils.mkdirs(new File(pufferRoot, "Movies"));
        File[] versions = dolphinRoot.listFiles();
        if (versions != null) {
            for (File version : versions) {
                if (version != null && version.isDirectory()) {
                    FileUtils.mkdirs(new File(version, "Paks/Movies"));
                }
            }
        }
        FileUtils.mkdirs(new File(contentRoot, "Movies"));
    }

    private static void chmodResourceTree(File root) {
        if (root == null || !root.exists()) {
            return;
        }
        try {
            if (root.isDirectory()) {
                FileUtils.chmod(root.getAbsolutePath(), 0755);
                File[] children = root.listFiles();
                if (children == null) {
                    return;
                }
                for (File child : children) {
                    chmodResourceTree(child);
                }
            } else {
                FileUtils.chmod(root.getAbsolutePath(), 0644);
            }
        } catch (Throwable ignored) {
        }
    }

    private static File findFirstFixListFromGuests(String packageName, int userId) {
        for (File candidate : fixListCandidates(packageName, userId)) {
            if (candidate.isFile() && candidate.length() > 0L) {
                return candidate;
            }
        }
        return null;
    }

    private static File findFirstFixList(List<File> dataRoots, List<File> extRoots) {
        for (File root : dataRoots) {
            File saved = new File(root, UE_SAVED);
            File found = findFixListInTree(saved);
            if (found != null) {
                return found;
            }
            found = findFixListInTree(root);
            if (found != null) {
                return found;
            }
        }
        for (File root : extRoots) {
            File files = new File(root, "files");
            File found = findFixListInTree(files);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static File findFixListInTree(File root) {
        if (root == null || !root.exists()) {
            return null;
        }
        File direct = new File(root, "fix_list.json");
        if (direct.isFile() && direct.length() > 0L) {
            return direct;
        }
        if (!root.isDirectory()) {
            return null;
        }
        File[] children = root.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (child == null) {
                continue;
            }
            if (child.isFile() && "fix_list.json".equals(child.getName())) {
                return child;
            }
            if (child.isDirectory() && child.getName().startsWith("App.")) {
                File nested = new File(child, "fix_list.json");
                if (nested.isFile() && nested.length() > 0L) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static boolean needsDolphinSeed(File guestDolphin) {
        if (!guestDolphin.isDirectory()) {
            return true;
        }
        File[] versions = guestDolphin.listFiles();
        if (versions == null || versions.length == 0) {
            return true;
        }
        for (File version : versions) {
            if (version == null || !version.isDirectory()) {
                continue;
            }
            File manifest = new File(version, "Paks/manifest.txt");
            File logo = new File(version, "Paks/Movies/V_Logo2_Looping.bin");
            if (manifest.isFile() && logo.isFile()) {
                return false;
            }
        }
        return true;
    }

    private static boolean needsPufferSeed(File guestPuffer) {
        File index = new File(guestPuffer, "puffer_res.eifs");
        return !index.isFile() || index.length() < 1024L;
    }

    private static boolean mergeTree(File source, File dest, boolean mergeAppDirsOnly) throws IOException {
        if (source == null || !source.isDirectory() || dest == null) {
            return false;
        }
        boolean copied = false;
        File[] children = source.listFiles();
        if (children == null) {
            return false;
        }
        FileUtils.mkdirs(dest);
        for (File child : children) {
            if (child == null) {
                continue;
            }
            if (mergeAppDirsOnly && child.isDirectory() && !child.getName().startsWith("App.")) {
                continue;
            }
            File target = new File(dest, child.getName());
            if (child.isDirectory()) {
                copied |= mergeTree(child, target, false);
            } else if (shouldCopyFile(child, target)) {
                File parent = target.getParentFile();
                if (parent != null) {
                    FileUtils.mkdirs(parent);
                }
                FileUtils.copyFile(child, target);
                copied = true;
            }
        }
        return copied;
    }

    private static boolean shouldCopyFile(File source, File dest) {
        if (!source.canRead() || !source.isFile()) {
            return false;
        }
        if (!dest.exists()) {
            return true;
        }
        return dest.length() < source.length();
    }

    private static List<File> resolveHostExternalRoots(String packageName) {
        List<File> roots = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int hostUserId = AnubisCore.getHostUserId();
        addRoot(roots, seen, new File(Environment.getExternalStorageDirectory(),
                "Android/data/" + packageName));
        addRoot(roots, seen, new File(String.format(Locale.US,
                "/storage/emulated/%d/Android/data/%s", hostUserId, packageName)));
        try {
            VirtualPathSpoof.beginInternalBind();
            try {
                Context pkgCtx = AnubisCore.getContext()
                        .createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
                File ext = pkgCtx.getExternalFilesDir(null);
                if (ext != null) {
                    File root = ext.getParentFile();
                    addRoot(roots, seen, root);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } finally {
            VirtualPathSpoof.endInternalBind();
        }
        return roots;
    }

    private static List<File> resolveHostReadableDataRoots(String packageName) {
        List<File> roots = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        try {
            VirtualPathSpoof.beginInternalBind();
            try {
                Context pkgCtx = AnubisCore.getContext()
                        .createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
                File files = pkgCtx.getFilesDir();
                if (files != null) {
                    addRoot(roots, seen, files);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } finally {
            VirtualPathSpoof.endInternalBind();
        }
        addRoot(roots, seen, new File("/data/data/" + packageName + "/files"));
        return roots;
    }

    private static void addRoot(List<File> roots, Set<String> seen, File root) {
        if (root == null) {
            return;
        }
        String path = root.getAbsolutePath();
        if (!seen.add(path)) {
            return;
        }
        if (root.exists()) {
            roots.add(root);
        }
    }
}
