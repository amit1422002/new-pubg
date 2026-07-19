package com.anubis.loader.fake.hook;

import java.lang.reflect.Method;
import com.anubis.loader.AnubisCore;
import black.android.content.BRAttributionSource;
import com.anubis.loader.utils.compat.BuildCompat;
import com.anubis.loader.utils.compat.ContextCompat;

/**
 * @author virtual_space
 * @function
 **/
public class ReplacePackageNameMethodHook extends MethodHook {
    private int packageNameIndex;

    public ReplacePackageNameMethodHook(int Index) {
        this.packageNameIndex = Index;
    }

    public Object hook(Object proxy, Method method, Object[] args) throws Throwable {
        Class realClass;
        if (args != null) {
            int i = this.packageNameIndex;
            if (i < 0) {
                this.packageNameIndex = i + args.length;
            }
            int i2 = this.packageNameIndex;
            if (i2 >= 0 && i2 < args.length && args[i2] != null) {
                if (args[i2] instanceof String) {
                    args[i2] = AnubisCore.getHostPkg();
                } else if (BuildCompat.isS() && (realClass = BRAttributionSource.getRealClass()) != null && realClass.isInstance(args[this.packageNameIndex])) {
                    ContextCompat.fixAttributionSourceState(args[this.packageNameIndex], AnubisCore.getHostUid());
                }
            }
        }
        return method.invoke(proxy, args);
    }
}