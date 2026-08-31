package lauresprojects.com.recorder;


import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

import lauresprojects.com.recorder.utils.FLog;

import com.google.android.material.color.DynamicColors;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.IOException;

import org.lsposed.lsparanoid.Obfuscate;

import android.content.pm.PackageInfo;

import com.anubis.skin.VirtualWebViewFix;
import com.elite.EliteInstaller;
import com.elite.app.configuration.ClientConfiguration;


@Obfuscate

public class App extends Application {

    private static final String TAG = "AppWebView";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            EliteInstaller.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean setHideRoot() {
                    return true;
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                @Override
                public boolean requestInstallPackage(File file) {
                    base.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    return false;
                }
            });
            EliteInstaller.setHideXposed(true);

            // ONLY main host process. Proxy :pX processes must stay unset so
            // VirtualWebViewFix can assign a guest-specific suffix before WebView starts.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && EliteInstaller.get().isMainProcess()) {
                try {
                    WebView.setDataDirectorySuffix("host");
                    Log.i(TAG, "host WebView suffix=host pid=" + Process.myPid());
                } catch (Throwable t) {
                    Log.w(TAG, "host WebView suffix failed", t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EliteInstaller core = EliteInstaller.get();
        // Register before doCreate so guest bind sees the callback.
        core.addAppLifecycleCallback(new VirtualWebViewFix());
        core.addAppLifecycleCallback(new com.anubis.skin.GuestLoginLifecycleCallback());
        core.doCreate();

        // Virtual GMS crash-loops on Android 16 (RegisterReceiverWithFeature). Keep it uninstalled.
        try {
            if (core.isMainProcess() && core.isInstallGms(0)) {
                core.uninstallGms(0);
                Log.i(TAG, "uninstalled virtual GMS to stop :p0 crash loop");
            }
        } catch (Throwable t) {
            Log.w(TAG, "uninstallGms failed", t);
        }

        DynamicColors.applyToActivitiesIfAvailable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    public boolean checkRootAccess() {
        if (Shell.rootAccess()) {
            FLog.info("Root granted");
            return true;
        } else {
            FLog.info("Root not granted");
            return false;
        }
    }

    public void doExe(String shell) {
        if (checkRootAccess()) {
            Shell.su(shell).exec();
        } else {
            try {
                Runtime.getRuntime().exec(shell);
                FLog.info("Shell: " + shell);
            } catch (IOException e) {
                FLog.error(e.getMessage());
            }
        }
    }

    public void doExecute(String shell) {
        doChmod(shell, 777);
        doExe(shell);
    }

    public void doChmod(String shell, int mask) {
        doExe("chmod " + mask + " " + shell);
    }

    public void toast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
