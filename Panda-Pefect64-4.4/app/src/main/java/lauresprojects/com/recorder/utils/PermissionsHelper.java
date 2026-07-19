package lauresprojects.com.recorder.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Central place for the runtime/special permissions the app needs. Each entry knows how to check
 * itself and how to open the right grant flow, with version guards so it's a no-op (auto-granted)
 * on OS versions where the permission doesn't apply.
 */
public final class PermissionsHelper {

    public enum Perm {
        OVERLAY("Display over other apps", "Lets the ESP & floating menu show on top of the game. Without it, nothing appears on screen."),
        STORAGE("All files access", "Only to copy the game's OBB/data so it can load. We don't read your personal files."),
        INSTALL("Install unknown apps", "Needed to install the game/loader. Nothing installs unless you tap Install."),
        MICROPHONE("Microphone", "Only for in-game voice chat. It's never recorded or sent anywhere."),
        NOTIFICATIONS("Notifications", "Shows the 'service running' notice so Android doesn't kill the tool in the background."),
        // Android 13+ granular media access. The virtualized game (BlackBox) sees storage as
        // granted only if the HOST holds these — otherwise it reports "read/write missing".
        MEDIA("Photos & media", "Android 13+ only. Lets the game see its own files. We don't open your gallery.");

        public final String title;
        public final String subtitle;

        Perm(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public static final int REQ_STORAGE_LEGACY = 1001;
    public static final int REQ_NOTIFICATIONS = 1002;
    public static final int REQ_MICROPHONE = 1003;
    public static final int REQ_MEDIA = 1004;

    private PermissionsHelper() {
    }

    /** Permissions that actually apply on this device's OS version. */
    public static List<Perm> applicable() {
        List<Perm> list = new ArrayList<>();
        list.add(Perm.OVERLAY);
        list.add(Perm.STORAGE);
        list.add(Perm.INSTALL);
        list.add(Perm.MICROPHONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Perm.NOTIFICATIONS);
            list.add(Perm.MEDIA);
        }
        return list;
    }

    public static boolean isGranted(Context ctx, Perm p) {
        switch (p) {
            case OVERLAY:
                return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx);
            case STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return Environment.isExternalStorageManager();
                }
                return ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
            case INSTALL:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return ctx.getPackageManager().canRequestPackageInstalls();
                }
                return true;
            case MICROPHONE:
                return ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED;
            case NOTIFICATIONS:
                // areNotificationsEnabled() reflects the real on/off state (toggled via the
                // settings screen) for any targetSdk, unlike checkSelfPermission which is
                // unreliable on a low-target app.
                return NotificationManagerCompat.from(ctx).areNotificationsEnabled();
            case MEDIA:
                // Below Android 13 this is covered by legacy storage; treat as granted.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES)
                            == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_VIDEO)
                            == PackageManager.PERMISSION_GRANTED;
                }
                return true;
            default:
                return true;
        }
    }

    public static boolean allGranted(Context ctx) {
        for (Perm p : applicable()) {
            if (!isGranted(ctx, p)) return false;
        }
        return true;
    }

    /** Opens the appropriate system screen / prompt to grant the given permission. */
    public static void request(Activity act, Perm p) {
        String pkg = act.getPackageName();
        try {
            switch (p) {
                case OVERLAY:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        act.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + pkg)));
                    }
                    break;
                case STORAGE:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            act.startActivity(new Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + pkg)));
                        } catch (Exception e) {
                            act.startActivity(new Intent(
                                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                        }
                    } else {
                        ActivityCompat.requestPermissions(act, new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE_LEGACY);
                    }
                    break;
                case INSTALL:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        act.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + pkg)));
                    }
                    break;
                case MICROPHONE:
                    ActivityCompat.requestPermissions(act,
                            new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MICROPHONE);
                    break;
                case MEDIA:
                    // Android 13+: request the granular media perms. Pick "Allow all" (not
                    // "Select photos") so the virtualized game gets full read access.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(act, new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO}, REQ_MEDIA);
                    }
                    break;
                case NOTIFICATIONS:
                    // The runtime POST_NOTIFICATIONS dialog frequently no-ops on a low-targetSdk
                    // app ("nothing happens"), so open the app's notification settings instead —
                    // it reliably lets the user enable notifications; onResume re-checks.
                    try {
                        act.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, pkg));
                    } catch (Exception e) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ActivityCompat.requestPermissions(act,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
                        }
                    }
                    break;
            }
        } catch (Exception ignored) {
        }
    }
}
