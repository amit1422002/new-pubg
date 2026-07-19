package com.anubis.loader.core.system.pm.installer;

import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.system.pm.BPackageSettings;
import com.anubis.loader.entity.pm.InstallOption;
import com.anubis.loader.utils.FileUtils;

/**
 * Created by Milk on 4/27/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class RemoveAppExecutor implements Executor {
    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        FileUtils.deleteDir(BEnvironment.getAppDir(ps.pkg.packageName));
        return 0;
    }
}
