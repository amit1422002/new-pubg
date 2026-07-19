package com.anubis.loader.utils.compat;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.Locale;

import com.anubis.loader.AnubisCore;
import com.anubis.loader.app.BActivityThread;
import com.anubis.loader.utils.DrawableUtils;

import com.anubis.loader.utils.VirtualPathSpoof;

public class TaskDescriptionCompat {
    public static ActivityManager.TaskDescription fix(ActivityManager.TaskDescription td) {
        String label = td.getLabel();
        Bitmap icon = td.getIcon();

        if (label != null && icon != null)
            return td;

        String guestPkg = BActivityThread.getAppPackageName();
        if (guestPkg != null && VirtualPathSpoof.isStealthAcPackage(guestPkg)) {
            if (label == null) {
                label = getTaskDescriptionLabel(BActivityThread.getUserId(), getApplicationLabel());
            }
            if (label != null) {
                return new ActivityManager.TaskDescription(label, icon, td.getPrimaryColor());
            }
            return td;
        }

        label = getTaskDescriptionLabel(BActivityThread.getUserId(), getApplicationLabel());
        Drawable drawable = getApplicationIcon();
        if (drawable == null)
            return td;

        ActivityManager am = (ActivityManager) AnubisCore.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        int iconSize = am.getLauncherLargeIconSize();
        icon = DrawableUtils.drawableToBitmap(drawable, iconSize, iconSize);
        td = new ActivityManager.TaskDescription(label, icon, td.getPrimaryColor());
        return td;
    }

    public static String getTaskDescriptionLabel(int userId, CharSequence label) {
        return String.format(Locale.CHINA, "%s", label);
    }

    private static CharSequence getApplicationLabel() {
        try {
            PackageManager pm = AnubisCore.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(BActivityThread.getAppPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static Drawable getApplicationIcon() {
		try {
			PackageManager pm = AnubisCore.getContext().getPackageManager();
			ApplicationInfo appInfo = pm.getApplicationInfo(BActivityThread.getAppPackageName(), 0);
			return pm.getApplicationIcon(appInfo);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
