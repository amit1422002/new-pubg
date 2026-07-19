package com.anubis.loader.gms;

import android.content.Context;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.anubis.loader.BuildConfig;
import com.anubis.loader.app.BActivityThread;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class GoogleSignInHelper {
    private static final String TAG = "GoogleSignInHelper";

    // OAuth Web client ΓÇö set GSI_CLIENT_ID / GSI_REDIRECT_URI in gradle.properties (Firebase project)
    public static final String GSI_CLIENT_ID = BuildConfig.GSI_CLIENT_ID;
    public static final String GSI_REDIRECT_URI = BuildConfig.GSI_REDIRECT_URI;

    public static final String ACTION_GSI_TOKEN  = "com.anubis.loader.google.GSI_TOKEN";
    public static final String ACTION_GSI_CANCEL = "com.anubis.loader.google.GSI_CANCEL";
    public static final String EXTRA_AUTH_TOKEN_TYPE    = "com.anubis.loader.google.auth_token_type";
    public static final String EXTRA_COMPLETE_IF_CACHED = "com.anubis.loader.google.complete_if_cached";

    // GMS/GSF packages ΓÇö these need KEY_INTENT response, not onError
    public static final String[] GMS_PACKAGES = {
        "com.google.android.gms", "com.google.android.gsf",
        "com.android.vending", "com.google.android.gms.persistent"
    };

    private static final String CACHE_FILE = "gsi_cached_token";

    private GoogleSignInHelper() {}

    /** Returns true if the Binder caller is a GMS package (needs KEY_INTENT response). */
    public static boolean isCallerGmsPackage(Context ctx) {
        if (ctx == null) return false;
        try {
            String virtualPkg = BActivityThread.getAppPackageName();
            if (!TextUtils.isEmpty(virtualPkg)) {
                for (String gmsPkg : GMS_PACKAGES) {
                    if (virtualPkg.equals(gmsPkg) || virtualPkg.startsWith(gmsPkg + ":")) return true;
                }
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            // Check process name for GMS packages
            android.app.ActivityManager am =
                (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            for (android.app.ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info.pid == callingPid) {
                    for (String gmsPkg : GMS_PACKAGES) {
                        if (info.processName != null && info.processName.startsWith(gmsPkg)) {
                            return true;
                        }
                    }
                    // Also check importance packages array
                    if (info.pkgList != null) {
                        for (String pkg : info.pkgList) {
                            for (String gmsPkg : GMS_PACKAGES) {
                                if (gmsPkg.equals(pkg)) return true;
                            }
                        }
                    }
                }
            }
            // Fallback: check via package manager UID
            String[] pkgsForUid = ctx.getPackageManager().getPackagesForUid(callingUid);
            if (pkgsForUid != null) {
                for (String pkg : pkgsForUid) {
                    for (String gmsPkg : GMS_PACKAGES) {
                        if (gmsPkg.equals(pkg)) return true;
                    }
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "isCallerGmsPackage check failed: " + t.getMessage());
        }
        return false;
    }

    public static void saveIdToken(Context ctx, String token) {
        if (ctx==null||TextUtils.isEmpty(token)) return;
        try {
            FileOutputStream f=new FileOutputStream(new File(ctx.getFilesDir(),CACHE_FILE),false);
            f.write(token.getBytes(StandardCharsets.UTF_8)); f.flush(); f.close();
        } catch (Throwable t) { Log.w(TAG,"save",t); }
    }

    public static String getCachedIdToken(Context ctx) {
        if (ctx==null) return null;
        File f=new File(ctx.getFilesDir(),CACHE_FILE); if(!f.exists()) return null;
        try {
            byte[] b=new byte[(int)f.length()];
            FileInputStream is=new FileInputStream(f); int n=is.read(b); is.close();
            if (n<=0) return null;
            String tok=new String(b,0,n,StandardCharsets.UTF_8).trim();
            if (TextUtils.isEmpty(tok)){f.delete();return null;}
            if (isJwtExpired(tok)){f.delete();return null;}
            return tok;
        } catch (Throwable t) { return null; }
    }

    public static boolean clearCachedToken(Context ctx) {
        if (ctx==null) return false;
        return ctx.deleteFile(CACHE_FILE);
    }

    public static boolean isJwtExpired(String jwt) {
        try { return parseJwtClaims(jwt).optLong("exp",0L)*1000L<=System.currentTimeMillis(); }
        catch (Throwable t) { return true; }
    }

    public static JSONObject parseJwtClaims(String jwt) throws Exception {
        String[] p=jwt.split("\\."); if(p.length<2) throw new IllegalArgumentException("bad jwt");
        String pl=p[1]; int pad=(4-(pl.length()%4))%4;
        StringBuilder sb=new StringBuilder(pl); for(int i=0;i<pad;i++) sb.append('=');
        return new JSONObject(new String(Base64.decode(sb.toString(),Base64.URL_SAFE|Base64.NO_PADDING),StandardCharsets.UTF_8));
    }

    private static String claim(Context ctx, String key) {
        String tok=getCachedIdToken(ctx); if(tok==null) return null;
        try { String v=parseJwtClaims(tok).optString(key,""); return v.isEmpty()?null:v; }
        catch (Throwable t) { return null; }
    }

    public static String getEmail(Context ctx)       { String e=claim(ctx,"email"); return e!=null?e:claim(ctx,"sub"); }
    public static String getGaiaId(Context ctx)      { return claim(ctx,"sub"); }
    public static String getDisplayName(Context ctx)  { return claim(ctx,"name"); }
    public static String getGivenName(Context ctx)    { return claim(ctx,"given_name"); }
    public static String getFamilyName(Context ctx)   { return claim(ctx,"family_name"); }

    public static String canonicaliseTokenType(String t) {
        if (TextUtils.isEmpty(t)) return null;
        String l=t.toLowerCase();
        if ("oauth2".equals(l)) return "oauth2:openid email profile";
        if (l.startsWith("oauth2:")) return t;
        int i=l.lastIndexOf(":oauth2:"); if(i>=0) return t.substring(i+1);
        return null;
    }

    public static boolean isIdTokenRequest(String t) {
        if (TextUtils.isEmpty(t)) return true;
        String l=t.toLowerCase(); return "id_token".equals(l)||"idtoken".equals(l);
    }

    public static String firstNonEmpty(String... c) {
        for (String s:c) if(!TextUtils.isEmpty(s)) return s; return null;
    }
}
