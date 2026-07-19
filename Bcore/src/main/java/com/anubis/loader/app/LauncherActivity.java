package com.anubis.loader.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.R;
import com.anubis.loader.utils.Slog;

/**
 * Created by Anubis on 2022/2/24.
 */
public class LauncherActivity extends Activity {
    public static final String TAG = "SplashScreen";

    public static final String KEY_INTENT = "launch_intent";
    public static final String KEY_PKG = "launch_pkg";
    public static final String KEY_USER_ID = "launch_user_id";
    private boolean isRunning = false;

    /**
     * Android 13+ requires the typed {@link Intent#getParcelableExtra(String, Class)} overload;
     * legacy {@code getParcelableExtra(name)} returns null for non-system parcelables.
     */
    private static Intent readLaunchIntentExtra(Intent wrapper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return wrapper.getParcelableExtra(KEY_INTENT, Intent.class);
        }
        return wrapper.getParcelableExtra(KEY_INTENT);
    }

    public static void launch(Intent intent, int userId) {
        Intent splash = new Intent();
        splash.setClass(AnubisCore.getContext(), LauncherActivity.class);
        splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        splash.putExtra(LauncherActivity.KEY_INTENT, intent);
        splash.putExtra(LauncherActivity.KEY_PKG, intent.getPackage());
        splash.putExtra(LauncherActivity.KEY_USER_ID, userId);
        AnubisCore.getContext().startActivity(splash);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        Intent launchIntent = readLaunchIntentExtra(intent);
        String packageName = intent.getStringExtra(KEY_PKG);
        int userId = intent.getIntExtra(KEY_USER_ID, 0);

        if (launchIntent == null) {
            Slog.e(TAG, "Missing launch intent extra; need API 33+ typed getParcelableExtra. SDK="
                    + Build.VERSION.SDK_INT);
            finish();
            return;
        }
        if (packageName == null) {
            Slog.e(TAG, "Missing package extra; cannot start activity.");
            finish();
            return;
        }

        PackageInfo packageInfo = AnubisCore.getBPackageManager().getPackageInfo(packageName, 0, userId);
        if (packageInfo == null) {
            Slog.e(TAG, packageName + " not installed!");
            finish();
            return;
        }
        Drawable drawable;
        try {
            drawable = packageInfo.applicationInfo.loadIcon(AnubisCore.getPackageManager());
        } catch (Throwable t) {
            Slog.e(TAG, "loadIcon failed", t);
            drawable = null;
        }
        setContentView(R.layout.activity_launcher);
        if (drawable != null) {
            findViewById(R.id.iv_icon).setBackgroundDrawable(drawable);
        }
        final Intent startCopy = launchIntent;
        new Thread(() -> {
            try {
                AnubisCore.getBActivityManager().startActivity(startCopy, userId);
            } catch (Throwable t) {
                Slog.e(TAG, "startActivity failed", t);
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRunning) {
            finish();
        }
    }
}
