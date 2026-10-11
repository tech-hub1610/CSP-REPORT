package org.cspdaily.report.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.cspdaily.report.domain.Biz;

/* loaded from: classes.dex */
public final class Settings {
    public static final String CSP_SORT = "csp_sort";
    public static final String HOURLY_ENABLED = "hourly_enabled";
    public static final String HOURLY_END = "hourly_end";
    public static final String HOURLY_START = "hourly_start";
    public static final String LAST_COMPLETE_NOTICE = "last_complete_notice";
    public static final String LAST_DAILY_NOTICE = "last_daily_notice";
    public static final String LAST_FOLLOWUP_NOTICE = "last_followup_notice";
    public static final String LAST_LONG_PENDING_NOTICE = "last_long_pending_notice";
    public static final String NOTIF_COMPLETION = "notif_completion_enabled";
    public static final String NOTIF_ENABLED = "notif_enabled";
    public static final String NOTIF_FOLLOWUP = "notif_followup_enabled";
    public static final String NOTIF_FOLLOWUP_TIME = "notif_followup_time";
    public static final String NOTIF_LONG_PENDING = "notif_long_pending_enabled";
    public static final String NOTIF_LONG_THRESH = "notif_long_pending_thresh";
    public static final String NOTIF_PENDING = "notif_pending_enabled";
    public static final String NOTIF_PENDING_TIME = "notif_pending_time";
    public static final String SUPABASE_URL = "supabase_url";
    public static final String SUPABASE_KEY = "supabase_key";
    public static final String SUPABASE_AUTO_SYNC = "supabase_auto_sync";
    public static final String SUPABASE_LAST_SYNC = "supabase_last_sync";
    public static final String SECURITY_LOCK_ENABLED = "security_lock_enabled";
    public static final String SECURITY_LOCK_TIMEOUT = "security_lock_timeout";
    public static final String SECURITY_INACTIVITY_TIMEOUT = "security_inactivity_timeout";
    public static final String SECURITY_BIOMETRIC_ENABLED = "security_biometric_enabled";

    private Settings() {
    }

    public static String get(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT setting_value FROM app_settings WHERE setting_key = ?", new String[]{str});
        try {
            if (rawQuery.moveToFirst() && !rawQuery.isNull(0)) {
                str2 = rawQuery.getString(0);
            }
            return str2;
        } finally {
            rawQuery.close();
        }
    }

    public static String get(SQLiteDatabase sQLiteDatabase, String str) {
        return get(sQLiteDatabase, str, "");
    }

    public static void set(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("setting_value", str2);
        if (sQLiteDatabase.update("app_settings", contentValues, "setting_key = ?", new String[]{str}) == 0) {
            contentValues.put("setting_key", str);
            sQLiteDatabase.insertWithOnConflict("app_settings", null, contentValues, 5);
        }
    }

    public static boolean flag(SQLiteDatabase sQLiteDatabase, String str, boolean z) {
        return "1".equals(get(sQLiteDatabase, str, z ? "1" : "0"));
    }

    public static void setFlag(SQLiteDatabase sQLiteDatabase, String str, boolean z) {
        set(sQLiteDatabase, str, z ? "1" : "0");
    }

    public static boolean hourlyEnabled(SQLiteDatabase sQLiteDatabase) {
        return "1".equals(get(sQLiteDatabase, HOURLY_ENABLED));
    }

    public static int[] hourlyWindow(SQLiteDatabase sQLiteDatabase) {
        int parseHourSetting = Biz.parseHourSetting(get(sQLiteDatabase, HOURLY_START), 10);
        int parseHourSetting2 = Biz.parseHourSetting(get(sQLiteDatabase, HOURLY_END), 18);
        if (parseHourSetting2 <= parseHourSetting) {
            parseHourSetting2 = Math.min(23, parseHourSetting + 1);
        }
        return new int[]{parseHourSetting, parseHourSetting2};
    }

    public static int longPendingThreshold(SQLiteDatabase sQLiteDatabase) {
        try {
            int parseInt = Integer.parseInt(get(sQLiteDatabase, NOTIF_LONG_THRESH, "3").trim());
            if (parseInt <= 0) {
                return 3;
            }
            return parseInt;
        } catch (Exception e) {
            return 3;
        }
    }

    public static int[] timeOfDay(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        int i;
        String str3 = get(sQLiteDatabase, str, str2);
        int parseHourSetting = Biz.parseHourSetting(str3, -1);
        int indexOf = str3.indexOf(58);
        if (indexOf < 0 || indexOf + 2 > str3.length()) {
            i = 0;
        } else {
            try {
                i = Integer.parseInt(str3.substring(indexOf + 1, indexOf + 3));
            } catch (Exception e) {
                i = 0;
            }
        }
        if (parseHourSetting < 0) {
            parseHourSetting = Biz.parseHourSetting(str2, 9);
        }
        return new int[]{parseHourSetting, Math.max(0, Math.min(59, i))};
    }
}
