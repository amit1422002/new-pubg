/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anubis.loader.utils;

import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @hide
 */
public final class Slog {
    /** Always visible in logcat during guest verify — never suppressed by {@link StealthMode}. */
    private static final Set<String> AUDIT_TAGS = new HashSet<>(Arrays.asList(
            "GUEST_AC_BYPASS",
            "HTPROTECT_FARLIGHT",
            "ANOGS_BYPASS_DFM",
            "ANOGS_BYPASS_BGMI",
            "FARLIGHT_STEALTH",
            "ANOGS_PATCH",
            "NERTC_PATCH",
            "PUBG_AYAN_F2",
            "DELTA_PATH",
            "FARLIGHT_PATH",
            "block-anogs"
    ));

    /** @hide */ public static final int LOG_ID_SYSTEM = 3;

    private Slog() {
    }

    private static boolean suppressed(String tag) {
        if (tag != null && AUDIT_TAGS.contains(tag)) {
            return false;
        }
        return StealthMode.shouldSuppressLogcat();
    }

    private static boolean suppressed() {
        return StealthMode.shouldSuppressLogcat();
    }

    public static int v(String tag, String msg) {
        if (suppressed()) return 0;
        return Log.println(Log.VERBOSE, tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        if (suppressed()) return 0;
        return Log.println(Log.VERBOSE, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int d(String tag, String msg) {
        if (suppressed()) return 0;
        return Log.println(Log.DEBUG, tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        if (suppressed()) return 0;
        return Log.println(Log.DEBUG, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int i(String tag, String msg) {
        if (suppressed(tag)) return 0;
        return Log.println(Log.INFO, tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        if (suppressed(tag)) return 0;
        return Log.println(Log.INFO, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String tag, String msg) {
        if (suppressed(tag)) return 0;
        return Log.println(Log.WARN, tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        if (suppressed(tag)) return 0;
        return Log.println(Log.WARN, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String tag, Throwable tr) {
        if (suppressed(tag)) return 0;
        return Log.println(Log.WARN, tag, Log.getStackTraceString(tr));
    }

    public static int e(String tag, String msg) {
        if (suppressed(tag)) return 0;
        return Log.println(Log.ERROR, tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (suppressed(tag)) return 0;
        return Log.println(Log.ERROR, tag,
                msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int println(int priority, String tag, String msg) {
        if (suppressed()) return 0;
        return Log.println(priority, tag, msg);
    }
}
