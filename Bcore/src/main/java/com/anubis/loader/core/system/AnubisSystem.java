package com.anubis.loader.core.system;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.core.env.AppSystemEnv;
import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.system.accounts.BAccountManagerService;
import com.anubis.loader.core.system.am.BActivityManagerService;
import com.anubis.loader.core.system.am.BJobManagerService;
import com.anubis.loader.core.system.location.BLocationManagerService;
import com.anubis.loader.core.system.notification.BNotificationManagerService;
import com.anubis.loader.core.system.os.BStorageManagerService;
import com.anubis.loader.core.system.pm.BPackageInstallerService;
import com.anubis.loader.core.system.pm.BPackageManagerService;
import com.anubis.loader.core.system.pm.BXposedManagerService;
import com.anubis.loader.core.system.user.BUserHandle;
import com.anubis.loader.core.system.user.BUserManagerService;
import com.anubis.loader.entity.pm.InstallOption;
import com.anubis.loader.utils.FileUtils;


public class AnubisSystem {
    private static AnubisSystem sAnubisSystem;
    private final List<ISystemService> mServices = new ArrayList<>();
    private final static AtomicBoolean isStartup = new AtomicBoolean(false);

    public static AnubisSystem getSystem() {
        if (sAnubisSystem == null) {
            synchronized (AnubisSystem.class) {
                if (sAnubisSystem == null) {
                    sAnubisSystem = new AnubisSystem();
                }
            }
        }
        return sAnubisSystem;
    }

    public void startup() {
        if (isStartup.getAndSet(true))
            return;
        BEnvironment.load();

        mServices.add(BPackageManagerService.get());
        mServices.add(BUserManagerService.get());
        mServices.add(BActivityManagerService.get());
        mServices.add(BJobManagerService.get());
        mServices.add(BStorageManagerService.get());
        mServices.add(BPackageInstallerService.get());
        mServices.add(BXposedManagerService.get());
        mServices.add(BProcessManagerService.get());
        mServices.add(BAccountManagerService.get());
        mServices.add(BLocationManagerService.get());
        mServices.add(BNotificationManagerService.get());

        for (ISystemService service : mServices) {
            service.systemReady();
        }

        List<String> preInstallPackages = AppSystemEnv.getPreInstallPackages();
        for (String preInstallPackage : preInstallPackages) {
            try {
                if (!BPackageManagerService.get().isInstalled(preInstallPackage, BUserHandle.USER_ALL)) {
                    PackageInfo packageInfo = AnubisCore.getPackageManager().getPackageInfo(preInstallPackage, 0);
                    BPackageManagerService.get().installPackageAsUser(packageInfo.applicationInfo.sourceDir, InstallOption.installBySystem(), BUserHandle.USER_ALL);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        
    }

}
