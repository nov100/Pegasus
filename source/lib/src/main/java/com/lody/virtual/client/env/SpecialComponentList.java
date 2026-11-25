package com.lody.virtual.client.env;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public final class SpecialComponentList {
    private static final List<String> ACTION_BLACK_LIST = new ArrayList<String>(1);
    private static final Map<String, String> PROTECTED_ACTION_MAP = new HashMap<>(5);
    private static final HashSet<String> WHITE_PERMISSION = new HashSet<>(3);
    private static final HashSet<String> INSTRUMENTATION_CONFLICTING = new HashSet<>(3);
    private static final HashSet<String> SPEC_SYSTEM_APP_LIST = new HashSet<>(5);
    private static final Set<String> SYSTEM_BROADCAST_ACTION = new HashSet<>(24);
    private static final String PROTECT_ACTION_PREFIX = "_VA_protected_";

    static {
        SYSTEM_BROADCAST_ACTION.add(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_SCREEN_ON);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_SCREEN_OFF);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_NEW_OUTGOING_CALL);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_TIME_TICK);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_TIME_CHANGED);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_TIMEZONE_CHANGED);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_BATTERY_CHANGED);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_BATTERY_LOW);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_BATTERY_OKAY);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_POWER_CONNECTED);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_POWER_DISCONNECTED);
        SYSTEM_BROADCAST_ACTION.add(Intent.ACTION_USER_PRESENT);
        SYSTEM_BROADCAST_ACTION.add("android.provider.Telephony.SMS_RECEIVED");
        SYSTEM_BROADCAST_ACTION.add("android.provider.Telephony.SMS_DELIVER");
        SYSTEM_BROADCAST_ACTION.add("android.net.wifi.STATE_CHANGE");
        SYSTEM_BROADCAST_ACTION.add("android.net.wifi.SCAN_RESULTS");
        SYSTEM_BROADCAST_ACTION.add("android.net.wifi.WIFI_STATE_CHANGED");
        SYSTEM_BROADCAST_ACTION.add("android.net.conn.CONNECTIVITY_CHANGE");
        SYSTEM_BROADCAST_ACTION.add("android.intent.action.ANY_DATA_STATE");
        SYSTEM_BROADCAST_ACTION.add("android.intent.action.SIM_STATE_CHANGED");
        SYSTEM_BROADCAST_ACTION.add("android.location.PROVIDERS_CHANGED");
        SYSTEM_BROADCAST_ACTION.add("android.location.MODE_CHANGED");

        ACTION_BLACK_LIST.add("android.appwidget.action.APPWIDGET_UPDATE");
        WHITE_PERMISSION.add("com.google.android.gms.settings.SECURITY_SETTINGS");
        WHITE_PERMISSION.add("com.google.android.apps.plus.PRIVACY_SETTINGS");
        WHITE_PERMISSION.add(Manifest.permission.ACCOUNT_MANAGER);

        PROTECTED_ACTION_MAP.put(Intent.ACTION_PACKAGE_ADDED, Constants.ACTION_PACKAGE_ADDED);
        PROTECTED_ACTION_MAP.put(Intent.ACTION_PACKAGE_REMOVED, Constants.ACTION_PACKAGE_REMOVED);
        PROTECTED_ACTION_MAP.put(Intent.ACTION_PACKAGE_CHANGED, Constants.ACTION_PACKAGE_CHANGED);
        PROTECTED_ACTION_MAP.put("android.intent.action.USER_ADDED", Constants.ACTION_USER_ADDED);
        PROTECTED_ACTION_MAP.put("android.intent.action.USER_REMOVED", Constants.ACTION_USER_REMOVED);

        INSTRUMENTATION_CONFLICTING.add("com.qihoo.magic");
        INSTRUMENTATION_CONFLICTING.add("com.qihoo.magic_mutiple");
        INSTRUMENTATION_CONFLICTING.add("com.facebook.katana");

        // Avoid WebViewFactory hidden APIs during class initialization. Android 15 may load
        // this class before Application.attach(), when WebViewFactory dereferences a null app.
        SPEC_SYSTEM_APP_LIST.add("android");
        SPEC_SYSTEM_APP_LIST.add("com.google.android.webview");
        SPEC_SYSTEM_APP_LIST.add("com.android.webview");
        SPEC_SYSTEM_APP_LIST.add("com.google.android.trichromelibrary");
        SPEC_SYSTEM_APP_LIST.add("com.android.chrome");
    }

    public static boolean isSpecSystemPackage(String pkg) { return SPEC_SYSTEM_APP_LIST.contains(pkg); }
    public static boolean isConflictingInstrumentation(String packageName) { return INSTRUMENTATION_CONFLICTING.contains(packageName); }
    public static boolean isActionInBlackList(String action) { return ACTION_BLACK_LIST.contains(action); }
    public static void addBlackAction(String action) { ACTION_BLACK_LIST.add(action); }

    public static void protectIntentFilter(IntentFilter filter) {
        if (filter == null) return;
        Object obj = mirror.android.content.IntentFilter.mActions.get(filter);
        if (obj == null) return;
        if (obj instanceof List) {
            @SuppressWarnings("unchecked") List<String> actions = (List<String>) obj;
            ListIterator<String> it = actions.listIterator();
            while (it.hasNext()) {
                String action = it.next();
                if (isActionInBlackList(action)) { it.remove(); continue; }
                if (SYSTEM_BROADCAST_ACTION.contains(action)) continue;
                String replacement = protectAction(action);
                if (replacement != null) it.set(replacement);
            }
        } else if (obj instanceof Collection) {
            @SuppressWarnings("unchecked") Collection<String> actions = (Collection<String>) obj;
            List<String> remove = new ArrayList<>();
            List<String> add = new ArrayList<>();
            for (String action : actions) {
                if (isActionInBlackList(action)) { remove.add(action); continue; }
                if (SYSTEM_BROADCAST_ACTION.contains(action)) continue;
                String replacement = protectAction(action);
                if (replacement != null) { remove.add(action); add.add(replacement); }
            }
            actions.removeAll(remove);
            actions.addAll(add);
        }
    }

    public static void protectIntent(Intent intent) {
        String action = protectAction(intent.getAction());
        if (action != null) intent.setAction(action);
    }

    public static void unprotectIntent(Intent intent) {
        String action = unprotectAction(intent.getAction());
        if (action != null) intent.setAction(action);
    }

    public static String protectAction(String origin) {
        if (origin == null) return null;
        if (origin.startsWith("_VA_")) return origin;
        String value = PROTECTED_ACTION_MAP.get(origin);
        return value != null ? value : PROTECT_ACTION_PREFIX + origin;
    }

    public static String unprotectAction(String action) {
        if (action == null) return null;
        if (action.startsWith(PROTECT_ACTION_PREFIX)) return action.substring(PROTECT_ACTION_PREFIX.length());
        for (Map.Entry<String, String> entry : PROTECTED_ACTION_MAP.entrySet()) {
            if (entry.getValue().equals(action)) return entry.getKey();
        }
        return null;
    }

    public static boolean isWhitePermission(String permission) { return WHITE_PERMISSION.contains(permission); }
}
