package com.anubis.loader.utils;

import android.content.Context;
import android.net.Uri;

public final class ObbUriStore {
    private static final String PREFS = "obb_tree_uri";
    private static final String GLOBAL_KEY = "__global_obb_root__";

    private ObbUriStore() {
    }

    public static void save(Context context, String packageName, Uri treeUri) {
        if (context == null || packageName == null || treeUri == null) {
            return;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(packageName, treeUri.toString())
                .apply();
    }

    public static Uri get(Context context, String packageName) {
        if (context == null || packageName == null) {
            return null;
        }
        String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(packageName, null);
        return value == null ? null : Uri.parse(value);
    }

    public static void remove(Context context, String packageName) {
        if (context == null || packageName == null) {
            return;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(packageName)
                .apply();
    }

    public static void saveGlobal(Context context, Uri treeUri) {
        if (context == null || treeUri == null) {
            return;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(GLOBAL_KEY, treeUri.toString())
                .apply();
    }

    public static Uri getGlobal(Context context) {
        if (context == null) {
            return null;
        }
        String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(GLOBAL_KEY, null);
        return value == null ? null : Uri.parse(value);
    }
}
