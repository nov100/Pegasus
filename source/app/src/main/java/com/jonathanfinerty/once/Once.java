package com.jonathanfinerty.once;

import android.content.Context;
import android.content.SharedPreferences;

/** Minimal local replacement for the old 'once' library (dropped with jcenter). */
public final class Once {
    public static final int THIS_APP_INSTALL = 0;
    private static SharedPreferences sp;

    private Once() {}

    public static void initialise(Context context) {
        sp = context.getApplicationContext().getSharedPreferences("once_done", Context.MODE_PRIVATE);
    }

    public static boolean beenDone(String tag) {
        return sp != null && sp.contains(tag);
    }

    public static boolean beenDone(int scope, String tag) {
        return beenDone(tag);
    }

    public static void markDone(String tag) {
        if (sp != null) sp.edit().putBoolean(tag, true).apply();
    }
}
