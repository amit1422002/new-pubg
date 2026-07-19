package lauresprojects.com.recorder;


import android.content.Context;
import android.os.Build;
import android.widget.Toast;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
//import androidx.multidex.MultiDex;
//import androidx.multidex.MultiDexApplication;

//import com.blankj.molihuan.utilcode.util.ToastUtils;
//import lauresprojects.com.recorder.activity.CrashHandler;
import lauresprojects.com.recorder.utils.BuildCompat;
import lauresprojects.com.recorder.utils.FLog;
import lauresprojects.com.recorder.utils.FPrefs;

import com.google.android.material.color.DynamicColors;
import com.topjohnwu.superuser.Shell;

import java.io.IOException;

import org.lsposed.lsparanoid.Obfuscate;

import android.content.pm.PackageInfo;

import java.io.File;
import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.configuration.AppLifecycleCallback;
import com.anubis.loader.app.configuration.ClientConfiguration;
import org.lsposed.lsparanoid.Obfuscate;


@Obfuscate

public class App extends Application {



    

    
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
        
            AnubisCore.get().doAttachBaseContext(base, new ClientConfiguration() {

               
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                
                public boolean isHideRoot() {
                    return true;
                }

                
                public boolean isHideXposed() {
                    return true;
                }

                
                public boolean isEnableDaemonService(){
                    return false;
                }

                public boolean requestInstallPackage(File file){
                    PackageInfo packageInfo = base.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);
                    return false;
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    
    public void onCreate() {
        super.onCreate();
       // gApp = this;

            

            AnubisCore.get().doCreate();



        
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
