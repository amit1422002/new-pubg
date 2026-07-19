// IBXposedManagerService.aidl

package com.anubis.loader.core.system.pm;

import java.util.List;
import com.anubis.loader.entity.pm.InstalledModule;

interface IBXposedManagerService {
    boolean isXPEnable();
    void setXPEnable(boolean enable);
    boolean isModuleEnable(String packageName);
    void setModuleEnable(String packageName, boolean enable);
    List<InstalledModule> getInstalledModules();
}