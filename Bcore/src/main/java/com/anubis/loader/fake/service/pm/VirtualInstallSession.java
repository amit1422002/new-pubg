package com.anubis.loader.fake.service.pm;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.ParcelFileDescriptor;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.entity.pm.InstallResult;
import com.anubis.loader.utils.FileUtils;
import com.anubis.loader.utils.Slog;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual PackageInstaller session — buffers APK writes and installs into Anubis on commit.
 */
public final class VirtualInstallSession {
    public static final String TAG = "VirtualPkgInstaller";

    private static final AtomicInteger sNextSessionId = new AtomicInteger(0x20000000);
    private static final ConcurrentHashMap<Integer, SessionState> sSessions = new ConcurrentHashMap<>();

    private VirtualInstallSession() {
    }

    static final class SessionState {
        final int sessionId;
        final File stagingDir;
        final int userId;
        String targetPackageName;

        SessionState(int sessionId, File stagingDir, int userId) {
            this.sessionId = sessionId;
            this.stagingDir = stagingDir;
            this.userId = userId;
        }
    }

    public static boolean isVirtualSession(int sessionId) {
        return sSessions.containsKey(sessionId);
    }

    public static int createSession(Object sessionParams, String installerPackageName, int userId) {
        int sessionId = sNextSessionId.incrementAndGet();
        File stagingDir = new File(AnubisCore.getContext().getCacheDir(),
                "virtual_install_" + sessionId);
        FileUtils.mkdirs(stagingDir);
        SessionState state = new SessionState(sessionId, stagingDir, userId);
        state.targetPackageName = readAppPackageName(sessionParams);
        sSessions.put(sessionId, state);
        Slog.d(TAG, "createSession id=" + sessionId + " pkg=" + state.targetPackageName
                + " installer=" + installerPackageName);
        return sessionId;
    }

    public static android.content.pm.IPackageInstallerSession openSession(int sessionId) {
        SessionState state = sSessions.get(sessionId);
        if (state == null) {
            throw new SecurityException("Invalid virtual session " + sessionId);
        }
        return new SessionBinder(state);
    }

    public static void abandonSession(int sessionId) {
        SessionState state = sSessions.remove(sessionId);
        if (state != null) {
            FileUtils.deleteDir(state.stagingDir);
            Slog.d(TAG, "abandoned session " + sessionId);
        }
    }

    private static String readAppPackageName(Object sessionParams) {
        if (sessionParams == null) {
            return null;
        }
        try {
            Object value = sessionParams.getClass().getField("appPackageName").get(sessionParams);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
        }
        try {
            Object value = sessionParams.getClass().getMethod("getAppPackageName").invoke(sessionParams);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static final class SessionBinder extends android.content.pm.IPackageInstallerSession.Stub {
        private final SessionState mState;
        private boolean mClosed;

        SessionBinder(SessionState state) {
            mState = state;
        }

        @Override
        public void setClientProgress(float progress) {
        }

        @Override
        public void addClientProgress(float progress) {
        }

        @Override
        public String[] getNames() {
            File[] files = mState.stagingDir.listFiles();
            if (files == null || files.length == 0) {
                return new String[0];
            }
            List<String> names = new ArrayList<>();
            for (File file : files) {
                if (file.isFile()) {
                    names.add(file.getName());
                }
            }
            return names.toArray(new String[0]);
        }

        @Override
        public ParcelFileDescriptor openWrite(String name, long offsetBytes, long lengthBytes) {
            if (mClosed) {
                throw new IllegalStateException("session closed");
            }
            File out = new File(mState.stagingDir, name);
            File parent = out.getParentFile();
            if (parent != null) {
                FileUtils.mkdirs(parent);
            }
            try {
                return ParcelFileDescriptor.open(out,
                        ParcelFileDescriptor.MODE_CREATE
                                | ParcelFileDescriptor.MODE_READ_WRITE
                                | ParcelFileDescriptor.MODE_TRUNCATE);
            } catch (Exception e) {
                throw new IllegalStateException("openWrite failed for " + name, e);
            }
        }

        @Override
        public ParcelFileDescriptor openRead(String name) {
            File file = new File(mState.stagingDir, name);
            try {
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (Exception e) {
                throw new IllegalStateException("openRead failed for " + name, e);
            }
        }

        @Override
        public void removeSplit(String splitName) {
            File file = new File(mState.stagingDir, splitName);
            FileUtils.deleteDir(file);
        }

        @Override
        public void close() {
            mClosed = true;
        }

        @Override
        public void commit(IntentSender statusReceiver) {
            if (mClosed) {
                sendResult(statusReceiver, false, null, "session already closed");
                return;
            }
            mClosed = true;
            File apk = resolveInstallApk(mState.stagingDir);
            if (apk == null || !apk.exists()) {
                sendResult(statusReceiver, false, mState.targetPackageName, "no APK in session");
                sSessions.remove(mState.sessionId);
                return;
            }
            Slog.d(TAG, "commit session " + mState.sessionId + " apk=" + apk.getAbsolutePath());
            InstallResult result = AnubisCore.get().installPackageAsUser(apk, mState.userId);
            String installedPkg = result.packageName != null ? result.packageName : mState.targetPackageName;
            sendResult(statusReceiver, result.success, installedPkg, result.msg);
            sSessions.remove(mState.sessionId);
            FileUtils.deleteDir(mState.stagingDir);
        }

        @Override
        public void abandon() {
            mClosed = true;
            abandonSession(mState.sessionId);
        }
    }

    private static File resolveInstallApk(File stagingDir) {
        File base = new File(stagingDir, "base.apk");
        if (base.exists() && base.length() > 0) {
            return base;
        }
        File[] apks = stagingDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".apk");
            }
        });
        if (apks == null || apks.length == 0) {
            return null;
        }
        File best = apks[0];
        for (File apk : apks) {
            if (apk.length() > best.length()) {
                best = apk;
            }
        }
        return best;
    }

    static void sendResult(IntentSender statusReceiver, boolean success, String packageName, String message) {
        if (statusReceiver == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(PackageInstaller.EXTRA_STATUS,
                success ? PackageInstaller.STATUS_SUCCESS : PackageInstaller.STATUS_FAILURE);
        if (packageName != null) {
            intent.putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName);
        }
        if (!success && message != null) {
            intent.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, message);
        }
        try {
            statusReceiver.sendIntent(AnubisCore.getContext(), 0, intent, null, null);
        } catch (Exception e) {
            Slog.w(TAG, "sendResult failed", e);
        }
    }
}
