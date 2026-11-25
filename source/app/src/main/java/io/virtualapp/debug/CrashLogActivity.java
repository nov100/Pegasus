package io.virtualapp.debug;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shows crash information of this app. Runs in its own process, where VirtualApp
 * is not started, so it still opens when the main app keeps crashing.
 */
public class CrashLogActivity extends Activity {

    private static final int COPY_LIMIT = 30000;
    private static final Pattern IMPORTANT = Pattern.compile(
            " [EF] |AndroidRuntime|Fatal signal|System\\.err|VA\\+\\+|UnsatisfiedLink|hiddenapi|Accessing hidden");

    private TextView mText;
    private String mReport = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);

        addButton(root, "รีเฟรช", this::refresh);
        addButton(root, "คัดลอก (ส่วนสำคัญ)", this::copy);
        addButton(root, "บันทึกเป็นไฟล์ใน Download (ฉบับเต็ม)", this::save);
        addButton(root, "ล้าง log", this::clear);

        mText = new TextView(this);
        mText.setTextSize(11);
        mText.setTextIsSelectable(true);
        mText.setPadding(16, 16, 16, 16);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(mText);
        root.addView(scroll);
        setContentView(root);
        refresh();
    }

    private void addButton(LinearLayout root, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        root.addView(button);
    }

    private void refresh() {
        mText.setText("กำลังอ่าน log...");
        new Thread(() -> {
            String report = buildReport();
            runOnUiThread(() -> {
                mReport = report;
                mText.setText(shorten(report));
            });
        }).start();
    }

    private String buildReport() {
        StringBuilder out = new StringBuilder();
        out.append("Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT)
                .append(") ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        out.append("VA build: phase2i\n");
        out.append("ABIs:");
        for (String abi : Build.SUPPORTED_ABIS) {
            out.append(' ').append(abi);
        }
        out.append("\n\n##### exit reasons\n");
        appendExitReasons(out);
        out.append("\n##### java crash file\n");
        appendCrashFile(out);

        StringBuilder important = new StringBuilder();
        ArrayDeque<String> tail = new ArrayDeque<>();
        try {
            // Without READ_LOGS, logcat only returns lines written by this app.
            java.lang.Process p = new ProcessBuilder("logcat", "-d", "-v", "threadtime",
                    "-b", "main,system,crash", "-t", "3000").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (IMPORTANT.matcher(line).find()) {
                        important.append(line).append('\n');
                    }
                    tail.add(line);
                    if (tail.size() > 1000) {
                        tail.removeFirst();
                    }
                }
            }
        } catch (Exception e) {
            important.append("logcat failed: ").append(e).append('\n');
        }
        out.append("\n##### logcat: important lines\n").append(important);
        out.append("\n##### logcat: tail\n");
        for (String line : tail) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private void appendExitReasons(StringBuilder out) {
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> list = am.getHistoricalProcessExitReasons(null, 0, 8);
            for (ApplicationExitInfo info : list) {
                out.append(new java.util.Date(info.getTimestamp())).append(' ')
                        .append(info.getProcessName())
                        .append(" reason=").append(info.getReason())
                        .append(" status=").append(info.getStatus())
                        .append(' ').append(info.getDescription()).append('\n');
            }
        } catch (Exception e) {
            out.append(e).append('\n');
        }
    }

    private void appendCrashFile(StringBuilder out) {
        File file = CrashLog.file(this);
        if (!file.exists()) {
            out.append("(ไม่มี)\n");
            return;
        }
        try (InputStream in = new FileInputStream(file);
             BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append('\n');
            }
        } catch (Exception e) {
            out.append(e).append('\n');
        }
    }

    private static String shorten(String text) {
        return text.length() <= COPY_LIMIT ? text : text.substring(0, COPY_LIMIT) + "\n...(ตัด)";
    }

    private void copy() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("va-log", shorten(mReport)));
        toast("คัดลอกแล้ว");
    }

    private void save() {
        if (Build.VERSION.SDK_INT < 29) {
            toast("ใช้ปุ่มคัดลอกแทน");
            return;
        }
        try {
            String name = "va-log-" + System.currentTimeMillis() + ".txt";
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            try (OutputStream o = getContentResolver().openOutputStream(uri)) {
                o.write(mReport.getBytes("UTF-8"));
            }
            toast("บันทึกแล้ว: Download/" + name);
        } catch (Exception e) {
            toast("บันทึกไม่ได้: " + e);
        }
    }

    private void clear() {
        CrashLog.file(this).delete();
        try {
            new ProcessBuilder("logcat", "-c").start().waitFor();
        } catch (Exception ignored) {
            // Clearing the log buffer is best effort.
        }
        refresh();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
