package io.virtualapp.debug;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Local-only crash recorder. Nothing is ever sent off the device. */
public final class CrashLog {

    static final String VIEWER_SUFFIX = ":vacrash";
    private static final String FILE_NAME = "va_last_crash.txt";
    private static final long MAX_BYTES = 200 * 1024;

    private CrashLog() {
    }

    public static String processName() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream("/proc/self/cmdline"), "UTF-8"))) {
            String line = r.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean isViewerProcess() {
        return processName().endsWith(VIEWER_SUFFIX);
    }

    static File file(Context context) {
        // Resolve the path before VirtualApp installs any hooks.
        return new File(context.getApplicationInfo().dataDir, FILE_NAME);
    }

    public static void install(Context context) {
        final File target = file(context);
        final String process = processName();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            write(target, process, "uncaught in thread " + thread.getName(), error);
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
    }

    public static void record(Context context, String where, Throwable error) {
        write(file(context), processName(), where, error);
    }

    private static synchronized void write(File target, String process, String where, Throwable error) {
        try {
            boolean append = target.length() < MAX_BYTES;
            String time = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            String text = "\n=== " + time + " [" + process + "] " + where + "\n"
                    + Log.getStackTraceString(error);
            try (FileOutputStream out = new FileOutputStream(target, append)) {
                out.write(text.getBytes("UTF-8"));
            }
        } catch (Throwable ignored) {
            // The recorder must never cause a second crash.
        }
    }
}
