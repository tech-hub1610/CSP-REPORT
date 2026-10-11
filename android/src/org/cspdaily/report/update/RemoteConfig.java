package org.cspdaily.report.update;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.cspdaily.report.Core;
import org.json.JSONObject;

public final class RemoteConfig {
    public static final String KEY_MAINTENANCE_MODE = "maintenance_mode";
    public static final String KEY_MAINTENANCE_MESSAGE = "maintenance_message";
    public static final String KEY_ANNOUNCEMENT_TITLE = "announcement_title";
    public static final String KEY_ANNOUNCEMENT_MESSAGE = "announcement_message";
    public static final String KEY_ANNOUNCEMENT_SEVERITY = "announcement_severity"; // info, warn, alert
    public static final String KEY_ANNOUNCEMENT_ACTIVE = "announcement_active";
    public static final String KEY_FEATURE_FLAGS = "feature_flags";
    public static final String KEY_MIN_SYNC_INTERVAL_SEC = "min_sync_interval_sec";

    private static final Map<String, String> MEMORY_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean sLoaded = false;

    public static class Announcement {
        public boolean active;
        public String title = "";
        public String message = "";
        public String severity = "info";

        public boolean hasContent() {
            return active && message != null && !message.trim().isEmpty();
        }
    }

    private RemoteConfig() {}

    public static synchronized void init(Context context) {
        if (sLoaded) return;
        loadFromDb(context);
        sLoaded = true;
    }

    public static synchronized void loadFromDb(Context context) {
        if (context == null) return;
        SQLiteDatabase db = Core.get(context).dbRead();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT config_key, config_value FROM remote_config WHERE is_active = 1", null);
            if (c != null && c.moveToFirst()) {
                do {
                    String k = c.getString(0);
                    String v = c.getString(1);
                    if (k != null && v != null) {
                        MEMORY_CACHE.put(k, v);
                    }
                } while (c.moveToNext());
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }
    }

    public static void saveConfig(Context context, String key, String value, boolean isActive, String updatedAt) {
        if (context == null || key == null) return;
        MEMORY_CACHE.put(key, value != null ? value : "");
        SQLiteDatabase db = Core.get(context).db();
        try {
            ContentValues cv = new ContentValues();
            cv.put("config_key", key);
            cv.put("config_value", value != null ? value : "");
            cv.put("is_active", isActive ? 1 : 0);
            cv.put("updated_at", updatedAt != null ? updatedAt : "");
            db.insertWithOnConflict("remote_config", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable ignored) {}
    }

    public static String getString(Context context, String key, String fallback) {
        if (!sLoaded) init(context);
        String val = MEMORY_CACHE.get(key);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }

    public static boolean getBoolean(Context context, String key, boolean fallback) {
        String val = getString(context, key, null);
        if (val == null) return fallback;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    public static int getInt(Context context, String key, int fallback) {
        String val = getString(context, key, null);
        if (val == null) return fallback;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean isMaintenanceMode(Context context) {
        return getBoolean(context, KEY_MAINTENANCE_MODE, false);
    }

    public static String getMaintenanceMessage(Context context) {
        return getString(context, KEY_MAINTENANCE_MESSAGE, "CSP Daily Report is temporarily under maintenance. Please try again in a few moments.");
    }

    public static Announcement getAnnouncement(Context context) {
        Announcement a = new Announcement();
        a.active = getBoolean(context, KEY_ANNOUNCEMENT_ACTIVE, false);
        a.title = getString(context, KEY_ANNOUNCEMENT_TITLE, "");
        a.message = getString(context, KEY_ANNOUNCEMENT_MESSAGE, "");
        a.severity = getString(context, KEY_ANNOUNCEMENT_SEVERITY, "info");
        return a;
    }

    public static Map<String, String> getAll(Context context) {
        if (!sLoaded) init(context);
        return new HashMap<>(MEMORY_CACHE);
    }
}
