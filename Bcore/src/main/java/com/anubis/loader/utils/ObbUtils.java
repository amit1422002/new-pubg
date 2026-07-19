package com.anubis.loader.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.env.GamePackages;

public final class ObbUtils {
    private static final String TAG = "ObbUtils";
    private static final int COPY_BUFFER_SIZE = 1024 * 1024;

    private ObbUtils() {
    }

    public static boolean copyObbFromHost(String packageName, int userId) {
        return copyObbFromHost(packageName, userId, null);
    }

    public static boolean copyObbFromHost(String packageName, int userId, ObbCopyProgressListener listener) {
        File destDir = BEnvironment.getObbDir(packageName);
        FileUtils.mkdirs(destDir);

        List<File> hostFiles = resolveHostObbFiles(packageName);
        if (!hostFiles.isEmpty()) {
            long totalBytes = 0;
            for (File hostFile : hostFiles) {
                totalBytes += Math.max(0, hostFile.length());
            }

            boolean copiedAny = false;
            long copiedBase = 0;
            for (File hostFile : hostFiles) {
                File destFile = new File(destDir, hostFile.getName());
                if (copyHostObbFile(hostFile, destFile, copiedBase, totalBytes, listener)) {
                    copiedAny = true;
                    copiedBase += Math.max(0, hostFile.length());
                }
            }
            if (copiedAny) {
                notifyProgress(listener, totalBytes, totalBytes, hostFiles.get(hostFiles.size() - 1).getName());
                Slog.d(TAG, "Copied OBB for " + packageName + " to " + destDir);
                return true;
            }
            Slog.w(TAG, "Host OBB files exist but cannot be read for " + packageName
                    + " (Android blocks access to other apps' OBB)");
        } else {
            Slog.w(TAG, "No host OBB files found for " + packageName);
        }

        Context context = AnubisCore.getContext();
        Uri savedUri = ObbUriStore.get(context, packageName);
        if (savedUri != null) {
            refreshUriPermission(context, savedUri);
            if (copyObbFromDocumentTree(context, savedUri, packageName, userId, listener)) {
                return true;
            }
            Slog.w(TAG, "Saved per-package OBB URI failed for " + packageName);
        }

        Uri globalUri = ObbUriStore.getGlobal(context);
        if (globalUri != null && (savedUri == null || !globalUri.equals(savedUri))) {
            refreshUriPermission(context, globalUri);
            if (copyObbFromDocumentTree(context, globalUri, packageName, userId, listener)) {
                return true;
            }
            Slog.w(TAG, "Saved global OBB URI failed for " + packageName);
        }
        return false;
    }

    public static boolean copyObbFromDocumentTree(Context context, Uri treeUri, String packageName, int userId) {
        return copyObbFromDocumentTree(context, treeUri, packageName, userId, null);
    }

    public static boolean copyObbFromDocumentTree(Context context, Uri treeUri, String packageName, int userId,
                                                  ObbCopyProgressListener listener) {
        if (context == null || treeUri == null) {
            return false;
        }

        File destDir = BEnvironment.getObbDir(packageName);
        FileUtils.mkdirs(destDir);

        DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
        if (tree == null) {
            Slog.w(TAG, "Invalid document tree URI for " + packageName);
            return false;
        }

        DocumentFile obbDir = findObbFolder(tree, packageName);
        if (obbDir == null) {
            Slog.w(TAG, "OBB folder not found in selected directory for " + packageName);
            return false;
        }

        List<DocumentFile> obbFiles = collectObbFiles(obbDir);
        if (obbFiles.isEmpty()) {
            Slog.w(TAG, "No OBB files found for " + packageName);
            return false;
        }

        long totalBytes = 0;
        for (DocumentFile obbFile : obbFiles) {
            long length = obbFile.length();
            if (length > 0) {
                totalBytes += length;
            }
        }

        boolean copiedAny = false;
        long copiedBase = 0;
        for (DocumentFile child : obbFiles) {
            String name = child.getName();
            if (name == null) {
                continue;
            }
            File destFile = new File(destDir, name);
            long fileSize = Math.max(0, child.length());
            try {
                copyDocumentFile(context, child, destFile, copiedBase, totalBytes, listener);
                copiedAny = true;
                copiedBase += fileSize > 0 ? fileSize : destFile.length();
                Slog.d(TAG, "Copied OBB via SAF: " + name + " -> " + destFile);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to copy " + name + ": " + e.getMessage(), e);
            }
        }

        if (copiedAny && totalBytes > 0) {
            notifyProgress(listener, totalBytes, totalBytes, obbFiles.get(obbFiles.size() - 1).getName());
        }
        return copiedAny;
    }

    public static boolean copyObbFromDocumentFile(Context context, Uri fileUri, String packageName, int userId) {
        return copyObbFromDocumentFile(context, fileUri, packageName, userId, null);
    }

    public static boolean copyObbFromDocumentFile(Context context, Uri fileUri, String packageName, int userId,
                                                  ObbCopyProgressListener listener) {
        if (context == null || fileUri == null) {
            return false;
        }
        DocumentFile doc = DocumentFile.fromSingleUri(context, fileUri);
        if (doc == null || !doc.isFile()) {
            Slog.w(TAG, "Invalid OBB document file URI for " + packageName);
            return false;
        }
        String name = doc.getName();
        if (name == null || !name.endsWith(".obb")) {
            Slog.w(TAG, "Selected file is not an OBB for " + packageName);
            return false;
        }
        File destDir = BEnvironment.getObbDir(packageName);
        FileUtils.mkdirs(destDir);
        File destFile = new File(destDir, name);
        long fileSize = Math.max(0, doc.length());
        try {
            copyDocumentFile(context, doc, destFile, 0, fileSize > 0 ? fileSize : fileSize, listener);
            if (fileSize > 0) {
                notifyProgress(listener, fileSize, fileSize, name);
            }
            Slog.d(TAG, "Copied OBB file via SAF: " + name + " -> " + destFile);
            return destFile.exists() && destFile.length() > 0;
        } catch (IOException e) {
            Slog.e(TAG, "Failed to copy OBB file " + name + ": " + e.getMessage(), e);
            return false;
        }
    }

    public static void persistDocumentTreeUri(Context context, String packageName, Uri treeUri) {
        if (context == null || packageName == null || treeUri == null) {
            return;
        }
        try {
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
        } catch (SecurityException e) {
            Slog.w(TAG, "Could not persist URI permission: " + e.getMessage());
        }
        ObbUriStore.save(context, packageName, treeUri);

        DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
        if (tree != null) {
            String name = tree.getName();
            if (name != null && "obb".equalsIgnoreCase(name)) {
                ObbUriStore.saveGlobal(context, treeUri);
            }
        }
    }

    public static boolean hasHostObb(String packageName) {
        return !resolveHostObbFiles(packageName).isEmpty();
    }

    public static boolean hasVirtualObb(String packageName) {
        return hasObbFiles(BEnvironment.getObbDir(packageName));
    }

    public static boolean hasSavedObbUri(String packageName) {
        Context context = AnubisCore.getContext();
        return ObbUriStore.get(context, packageName) != null;
    }

    public static boolean hasGlobalObbUri() {
        Context context = AnubisCore.getContext();
        return ObbUriStore.getGlobal(context) != null;
    }

    public static boolean needsObbCopy(String packageName) {
        if (GamePackages.isPackageDataGame(packageName) || GamePackages.isDirectLaunchGame(packageName)) {
            return false;
        }
        if (GamePackages.isBgmi(packageName)) {
            return true;
        }
        return hasHostObb(packageName) || hasSavedObbUri(packageName) || hasGlobalObbUri();
    }

    public static boolean canAutoCopyObb(String packageName) {
        return hasSavedObbUri(packageName) || hasGlobalObbUri();
    }

    public static String getVirtualObbPath(String packageName, int userId) {
        return BEnvironment.getObbDir(packageName).getAbsolutePath();
    }

    /** Migrate legacy OBB + copy from anubisloader or host if clone has no OBB yet. */
    public static void ensureObbPresent(String packageName, int userId) {
        if (GamePackages.isPackageDataGame(packageName) || GamePackages.isDirectLaunchGame(packageName)) {
            return;
        }
        BEnvironment.migrateLegacyObbIfNeeded(packageName);
        File obbDir = BEnvironment.getObbDir(packageName);
        FileUtils.mkdirs(obbDir);
        if (hasObbFiles(obbDir)) {
            return;
        }
        if (copyObbFromAnubisLoader(packageName, userId)) {
            Slog.d(TAG, "OBB restored from anubisloader for " + packageName);
            return;
        }
        if (copyObbFromHost(packageName, userId)) {
            Slog.d(TAG, "OBB copied from host for " + packageName);
        }
    }

    public static boolean copyObbFromAnubisLoader(String packageName, int userId) {
        File sourceDir = new File(Environment.getExternalStorageDirectory(), "anubisloader/bgmi/obb");
        List<File> sources = collectHostObbFilesFromDir(sourceDir, packageName);
        if (sources.isEmpty()) {
            sources = collectHostObbFilesFromDir(sourceDir, null);
        }
        if (sources.isEmpty()) {
            return false;
        }
        File destDir = BEnvironment.getObbDir(packageName);
        FileUtils.mkdirs(destDir);
        boolean copiedAny = false;
        for (File source : sources) {
            File dest = new File(destDir, source.getName());
            if (dest.isFile() && dest.length() == source.length()) {
                copiedAny = true;
                continue;
            }
            try {
                copyFileWithProgress(source, dest, 0, source.length(), source.getName(), null);
                copiedAny = true;
            } catch (IOException e) {
                Slog.w(TAG, "anubisloader OBB copy failed " + source.getName() + ": " + e.getMessage());
            }
        }
        return copiedAny && hasObbFiles(destDir);
    }

    private static boolean hasObbFiles(File obbDir) {
        if (obbDir == null || !obbDir.isDirectory()) {
            return false;
        }
        File[] files = obbDir.listFiles((d, name) -> name != null && name.endsWith(".obb"));
        return files != null && files.length > 0;
    }

    private static List<File> collectHostObbFilesFromDir(File dir, String packageName) {
        List<File> result = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) {
            return result;
        }
        for (File file : dir.listFiles()) {
            if (file == null || !file.isFile()) {
                continue;
            }
            String name = file.getName();
            if (!name.endsWith(".obb")) {
                continue;
            }
            if (packageName == null || name.contains(packageName)) {
                result.add(file);
            }
        }
        return result;
    }

    private static List<DocumentFile> collectObbFiles(DocumentFile obbDir) {
        List<DocumentFile> result = new ArrayList<>();
        for (DocumentFile child : obbDir.listFiles()) {
            if (!child.isFile()) {
                continue;
            }
            String name = child.getName();
            if (name != null && name.endsWith(".obb")) {
                result.add(child);
            }
        }
        return result;
    }

    private static List<File> resolveHostObbFiles(String packageName) {
        Set<String> names = new LinkedHashSet<>();
        long versionCode = resolveHostVersionCode(packageName);
        if (versionCode > 0) {
            names.add(String.format(Locale.US, "main.%d.%s.obb", versionCode, packageName));
            names.add(String.format(Locale.US, "patch.%d.%s.obb", versionCode, packageName));
        }

        int hostUserId = AnubisCore.getHostUserId();
        File[] roots = new File[] {
                new File(Environment.getExternalStorageDirectory(), "Android/obb/" + packageName),
                new File("/storage/emulated/" + hostUserId + "/Android/obb/" + packageName),
                new File("/sdcard/Android/obb/" + packageName)
        };

        List<File> result = new ArrayList<>();
        for (String name : names) {
            for (File root : roots) {
                File candidate = new File(root, name);
                if (candidate.exists() && candidate.isFile()) {
                    result.add(candidate);
                    break;
                }
            }
        }
        if (!result.isEmpty()) {
            return result;
        }

        for (File root : roots) {
            if (!root.isDirectory()) {
                continue;
            }
            File[] files = root.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                String fileName = file.getName();
                if (fileName.endsWith(".obb") && fileName.contains(packageName)) {
                    result.add(file);
                }
            }
            if (!result.isEmpty()) {
                break;
            }
        }
        return result;
    }

    private static void refreshUriPermission(Context context, Uri treeUri) {
        try {
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
        } catch (SecurityException e) {
            Slog.w(TAG, "Could not refresh URI permission: " + e.getMessage());
        }
    }

    private static long resolveHostVersionCode(String packageName) {
        try {
            PackageManager pm = AnubisCore.getContext().getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return info.getLongVersionCode();
            }
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    private static boolean copyHostObbFile(File source, File dest, long copiedBase, long totalBytes,
                                           ObbCopyProgressListener listener) {
        if (!source.canRead()) {
            Slog.w(TAG, "Cannot read host OBB file: " + source);
            return false;
        }
        try {
            copyFileWithProgress(source, dest, copiedBase, totalBytes, source.getName(), listener);
            return dest.exists() && dest.length() > 0;
        } catch (IOException e) {
            Slog.e(TAG, "Failed to copy " + source + ": " + e.getMessage(), e);
            return false;
        }
    }

    private static void copyFileWithProgress(File source, File dest, long copiedBase, long totalBytes,
                                             String fileName, ObbCopyProgressListener listener) throws IOException {
        if (dest.exists() && !dest.delete()) {
            throw new IOException("Failed to replace " + dest);
        }
        File parent = dest.getParentFile();
        if (parent != null) {
            FileUtils.mkdirs(parent);
        }

        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            copyStreamWithProgress(in, out, copiedBase, totalBytes, fileName, listener);
        }
    }

    private static void copyDocumentFile(Context context, DocumentFile source, File dest, long copiedBase,
                                         long totalBytes, ObbCopyProgressListener listener) throws IOException {
        if (dest.exists() && !dest.delete()) {
            throw new IOException("Failed to replace " + dest);
        }
        File parent = dest.getParentFile();
        if (parent != null) {
            FileUtils.mkdirs(parent);
        }

        String fileName = source.getName() == null ? dest.getName() : source.getName();
        try (InputStream in = context.getContentResolver().openInputStream(source.getUri());
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) {
                throw new IOException("Cannot open " + source.getUri());
            }
            copyStreamWithProgress(in, out, copiedBase, totalBytes, fileName, listener);
        }
    }

    private static void copyStreamWithProgress(InputStream in, OutputStream out, long copiedBase, long totalBytes,
                                               String fileName, ObbCopyProgressListener listener) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        long fileCopied = 0;
        int lastPercent = -1;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            fileCopied += read;
            if (listener == null) {
                continue;
            }
            long overallCopied = copiedBase + fileCopied;
            if (totalBytes > 0) {
                int percent = (int) Math.min(100, (overallCopied * 100) / totalBytes);
                if (percent != lastPercent) {
                    lastPercent = percent;
                    listener.onProgress(percent, overallCopied, totalBytes, fileName);
                }
            } else {
                listener.onProgress(-1, overallCopied, 0, fileName);
            }
        }
        out.flush();
    }

    private static void notifyProgress(ObbCopyProgressListener listener, long copiedBytes, long totalBytes,
                                       String fileName) {
        if (listener == null) {
            return;
        }
        int percent = totalBytes > 0 ? (int) Math.min(100, (copiedBytes * 100) / totalBytes) : 100;
        listener.onProgress(percent, copiedBytes, totalBytes, fileName);
    }

    private static DocumentFile findObbFolder(DocumentFile tree, String packageName) {
        String treeName = tree.getName();
        if (packageName.equals(treeName)) {
            return tree;
        }

        DocumentFile child = tree.findFile(packageName);
        if (child != null && child.isDirectory()) {
            return child;
        }

        for (DocumentFile item : tree.listFiles()) {
            if (!item.isDirectory()) {
                continue;
            }
            String name = item.getName();
            if (packageName.equals(name)) {
                return item;
            }
            if ("obb".equalsIgnoreCase(name)) {
                DocumentFile nested = item.findFile(packageName);
                if (nested != null && nested.isDirectory()) {
                    return nested;
                }
            }
        }
        return null;
    }
}
