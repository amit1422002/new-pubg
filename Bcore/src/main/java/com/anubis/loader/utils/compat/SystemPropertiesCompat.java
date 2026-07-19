package com.anubis.loader.utils.compat;

import android.text.TextUtils;

import com.anubis.loader.utils.BuildStealthHelper;
import com.anubis.loader.utils.Reflector;


public class SystemPropertiesCompat {

    public static String get(String key, String def) {
        try {
            String value = (String) Reflector.on("android.os.SystemProperties")
                    .method("get", String.class, String.class)
                    .call(key, def);
            return BuildStealthHelper.resolveProperty(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static String get(String key) {
        try {
            String value = (String) Reflector.on("android.os.SystemProperties")
                    .method("get", String.class)
                    .call(key);
            return BuildStealthHelper.resolveProperty(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isExist(String key) {
        return !TextUtils.isEmpty(get(key));
    }

    public static int getInt(String key, int def) {
        try {
            int value = (int) Reflector.on("android.os.SystemProperties")
                    .method("getInt", String.class, int.class)
                    .call(key, def);
            return BuildStealthHelper.resolvePropertyInt(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

}
