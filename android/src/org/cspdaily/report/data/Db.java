package org.cspdaily.report.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.cspdaily.report.domain.Biz;

public class Db extends SQLiteOpenHelper {
    private static final String FILE = "csp_daily.db";
    private static final int VERSION = 2;
    private static Db sInstance;

    public static final String[][] DEFAULT_PARAMS = {
        new String[]{"inoperative_rekyc", "Inoperative ReKYC/ReKYC", "ReKYC"},
        new String[]{"qr_code_rekyc", "QR Code ReKYC", "QR ReKYC"},
        new String[]{"pmjjby", "PMJJBY", "PMJJBY"},
        new String[]{"pmsby", "PMSBY", "PMSBY"},
        new String[]{"pmjdy", "PMJDY", "PMJDY"},
        new String[]{"apy", "APY", "APY"},
        new String[]{"inoperative_rekyc_branch_submit", "Inoperative ReKYC Branch Submit", "Br. Submit"},
        new String[]{"whatsapp_banking", "WhatsApp Banking", "WhatsApp"}
    };

    public static final String[][] SEED_CSPS = {
        new String[]{"32250071", "SHUBHAM ROY CHOWDHURY", "M", "7908875354", "ACB KALIYAGNJ", "1806"},
        new String[]{"32250072", "AJINA BEGAM", "F", "9733273058", "ACB KALIYAGNJ", "1806"},
        new String[]{"32250079", "GOURAB NASKAR", "M", "7384480368", "ACB KALIYAGNJ", "1806"},
        new String[]{"32250111", "NAZIYA HASSAN", "F", "9851746470", "DALKHOLA", "2047"},
        new String[]{"32250088", "RIAJUL ISLAM", "M", "9733079432", "DASPARA", "8526"},
        new String[]{"32250104", "SARAJIT SINGHA", "M", "8167459229", "DASPARA", "8526"},
        new String[]{"32250127", "NIJA MUDDIN", "M", "9609924725", "DASPARA", "8526"},
        new String[]{"32250141", "AFJAL HUSSAIN", "M", "8637897606", "DASPARA", "8526"},
        new String[]{"32250093", "PROSENJIT SARKAR", "M", "8918185976", "KALANAGIN", "8928"},
        new String[]{"32250142", "KANAI BISWAS", "M", "8670094203", "KALANAGIN", "8928"},
        new String[]{"32250164", "MD SAHJAHAN", "M", "9735075064", "KALIGANJ", "9038"},
        new String[]{"32250076", "ANUBHAB CHOWDHURY", "M", "7797987979", "KALIYAGANJ", "2074"},
        new String[]{"32250101", "BASUDEB BARMAN", "M", "9614846085", "KALIYAGANJ", "2074"},
        new String[]{"32250102", "RAMKUMAR MONDAL", "M", "7001706209", "KALIYAGANJ", "2074"},
        new String[]{"32250105", "JYOTIRMOY HUI", "M", "9434424075", "KALIYAGANJ", "2074"},
        new String[]{"32250096", "MUJARUL ISLAM", "M", "8372854677", "KARANDIGHI", "6665"},
        new String[]{"32250049", "DEBASISH MODAK", "M", "9474848417", "KARNAJORA", "1654"},
        new String[]{"32250090", "ANNI BARMAN", "F", "7076966411", "KARNAJORA", "1654"},
        new String[]{"32250092", "SOMEN BOSE", "M", "9614949365", "KARNAJORA", "1654"},
        new String[]{"32250085", "MDMUNNA MUSTAQUE", "M", "9734900015", "LAKHIPUR", "9728"},
        new String[]{"32250086", "RASHID ALAM", "M", "9733197405", "LAKHIPUR", "9728"},
        new String[]{"32250094", "JAHANGIR HUSSAIN", "M", "9735094453", "LAKHIPUR", "9728"},
        new String[]{"32250169", "MD ISMAIL", "M", "9064302358", "LAKHIPUR", "9728"},
        new String[]{"32250162", "MANJUSHREE DEY", "F", "9382162592", "MADANPUR", "9701"},
        new String[]{"32250160", "MDINTEKHAB ALAM", "M", "8918936686", "PANJIPARA", "4788"},
        new String[]{"32250170", "ISHTEYAK AHMED", "M", "8617506306", "PANJIPARA", "4788"},
        new String[]{"32250108", "DIPANKAR MANDAL", "M", "9734399399", "RAIGANJ", "0162"},
        new String[]{"32250107", "NAJMA KHATUN", "F", "8001851641", "SHYAMPUR", "8927"},
        new String[]{"32250117", "ZIAUR RAHAMAN", "M", "9732470010", "SME RAIGANJ", "18264"},
        new String[]{"32250146", "MANIRUL ISLAM", "M", "9735912542", "SME RAIGANJ", "61283"},
        new String[]{"32250087", "RANA DEBNATH", "M", "8617533035", "COLLEGEPARA", "12461"},
        new String[]{"322500163", "SUBHANKAR SARKAR", "M", "7908670911", "COLLEGEPARA", "12461"},
        new String[]{"32250091", "AMIT KUMAR THAKUR", "M", "8906776521", "COLLEGEPARA", "12461"}
    };

    public static synchronized Db get(Context context) {
        Db db;
        synchronized (Db.class) {
            if (sInstance == null) {
                sInstance = new Db(context.getApplicationContext());
            }
            db = sInstance;
        }
        return db;
    }

    private Db(Context context) {
        super(context, FILE, (SQLiteDatabase.CursorFactory) null, VERSION);
    }

    private static String cspColumns() {
        StringBuilder sb = new StringBuilder();
        for (String[] strArr : DEFAULT_PARAMS) {
            sb.append("  tgt_").append(strArr[0]).append(" INTEGER NOT NULL DEFAULT 0,\n");
        }
        return sb.toString();
    }

    private static String reportColumns() {
        StringBuilder sb = new StringBuilder();
        for (String[] strArr : DEFAULT_PARAMS) {
            sb.append("  ").append(strArr[0]).append(" INTEGER NOT NULL DEFAULT 0,\n");
        }
        return sb.toString();
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        ensureSchema(sQLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int oldVersion, int newVersion) {
        ensureSchema(sQLiteDatabase);
    }

    @Override
    public void onOpen(SQLiteDatabase sQLiteDatabase) {
        super.onOpen(sQLiteDatabase);
        ensureSchema(sQLiteDatabase);
    }

    public synchronized void ensureSchema(SQLiteDatabase sQLiteDatabase) {
        if (sQLiteDatabase == null || !sQLiteDatabase.isOpen()) {
            return;
        }
        try {
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS csp (id INTEGER PRIMARY KEY AUTOINCREMENT,csp_code TEXT NOT NULL UNIQUE,csp_name TEXT NOT NULL,gender TEXT NOT NULL DEFAULT '',mobile_number TEXT NOT NULL DEFAULT '',branch_name TEXT NOT NULL DEFAULT '',branch_code TEXT NOT NULL DEFAULT '',status TEXT NOT NULL DEFAULT 'ACTIVE',notes TEXT NOT NULL DEFAULT ''," + cspColumns() + "created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS daily_report (id INTEGER PRIMARY KEY AUTOINCREMENT,csp_id INTEGER NOT NULL REFERENCES csp(id),business_date TEXT NOT NULL," + reportColumns() + "remarks TEXT NOT NULL DEFAULT '',submitted_at TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL,UNIQUE (csp_id, business_date))");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_report_date ON daily_report(business_date)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_report_csp ON daily_report(csp_id)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS pending_reason (id INTEGER PRIMARY KEY AUTOINCREMENT,csp_id INTEGER NOT NULL REFERENCES csp(id),business_date TEXT NOT NULL,reason TEXT NOT NULL,created_at TEXT NOT NULL,updated_at TEXT NOT NULL,UNIQUE (csp_id, business_date))");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_reason_date ON pending_reason(business_date)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS followup_log (id INTEGER PRIMARY KEY AUTOINCREMENT,csp_id INTEGER NOT NULL REFERENCES csp(id),business_date TEXT NOT NULL,action_status TEXT NOT NULL,note TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_followup_log_csp_date ON followup_log(csp_id, business_date)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_followup_log_date ON followup_log(business_date)");
            sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_followup_log_unique ON followup_log(csp_id, business_date, created_at)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS business_day (business_date TEXT PRIMARY KEY,day_type TEXT NOT NULL DEFAULT 'WORKING',description TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS office_note (business_date TEXT PRIMARY KEY,note TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS app_settings (setting_key TEXT PRIMARY KEY,setting_value TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS hourly_report (id INTEGER PRIMARY KEY AUTOINCREMENT,csp_id INTEGER NOT NULL REFERENCES csp(id),business_date TEXT NOT NULL,hour INTEGER NOT NULL," + reportColumns() + "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,UNIQUE (csp_id, business_date, hour))");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS report_param (id INTEGER PRIMARY KEY AUTOINCREMENT,field_key TEXT NOT NULL UNIQUE,label TEXT NOT NULL,short_label TEXT NOT NULL,sort INTEGER NOT NULL,is_custom INTEGER NOT NULL DEFAULT 0)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS daily_csp_target (id INTEGER PRIMARY KEY AUTOINCREMENT,csp_id INTEGER NOT NULL REFERENCES csp(id),business_date TEXT NOT NULL,field_key TEXT NOT NULL,target_value INTEGER NOT NULL DEFAULT 0,created_at TEXT NOT NULL,updated_at TEXT NOT NULL,UNIQUE (csp_id, business_date, field_key))");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_csp_target_date ON daily_csp_target(business_date)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_csp_target_csp ON daily_csp_target(csp_id, business_date)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS app_user (id INTEGER PRIMARY KEY AUTOINCREMENT,username TEXT NOT NULL UNIQUE,password_hash TEXT NOT NULL DEFAULT '',password_salt TEXT NOT NULL DEFAULT '',name TEXT NOT NULL,role TEXT NOT NULL DEFAULT 'SUPERVISOR',mobile TEXT NOT NULL DEFAULT '',email TEXT NOT NULL DEFAULT '',status TEXT NOT NULL DEFAULT 'ACTIVE',is_default_password INTEGER NOT NULL DEFAULT 0,password TEXT DEFAULT '',created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(username)");

            // Migrate legacy app_user schema if upgrading
            Set<String> userCols = columnsOf(sQLiteDatabase, "app_user");
            if (!userCols.contains("password_hash")) {
                sQLiteDatabase.execSQL("ALTER TABLE app_user ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''");
            }
            if (!userCols.contains("password_salt")) {
                sQLiteDatabase.execSQL("ALTER TABLE app_user ADD COLUMN password_salt TEXT NOT NULL DEFAULT ''");
            }
            if (!userCols.contains("is_default_password")) {
                sQLiteDatabase.execSQL("ALTER TABLE app_user ADD COLUMN is_default_password INTEGER NOT NULL DEFAULT 0");
            }

            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS supervisor_csp_map (supervisor_id INTEGER NOT NULL REFERENCES app_user(id),csp_id INTEGER NOT NULL REFERENCES csp(id),assigned_date TEXT NOT NULL,PRIMARY KEY(supervisor_id, csp_id))");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_supervisor_csp_map_sup ON supervisor_csp_map(supervisor_id)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_supervisor_csp_map_csp ON supervisor_csp_map(csp_id)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS admin_instruction (id INTEGER PRIMARY KEY AUTOINCREMENT,business_date TEXT NOT NULL,sender_name TEXT NOT NULL,title TEXT NOT NULL,message TEXT NOT NULL,priority TEXT NOT NULL DEFAULT 'NORMAL',created_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_admin_instruction_date ON admin_instruction(business_date)");
            sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_instruction_unique ON admin_instruction(business_date, sender_name, created_at)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS app_updates (id INTEGER PRIMARY KEY AUTOINCREMENT,version_name TEXT NOT NULL,version_code INTEGER NOT NULL UNIQUE,minimum_supported_version_code INTEGER NOT NULL DEFAULT 0,update_type TEXT NOT NULL DEFAULT 'major',title TEXT NOT NULL,message TEXT NOT NULL DEFAULT '',apk_path TEXT NOT NULL,apk_sha256 TEXT NOT NULL DEFAULT '',release_notes TEXT NOT NULL DEFAULT '[]',is_active INTEGER NOT NULL DEFAULT 1,released_at TEXT NOT NULL,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_app_updates_version_code ON app_updates(version_code)");
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS remote_config (id INTEGER PRIMARY KEY AUTOINCREMENT,config_key TEXT NOT NULL UNIQUE,config_value TEXT NOT NULL DEFAULT '',is_active INTEGER NOT NULL DEFAULT 1,updated_at TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_remote_config_key ON remote_config(config_key)");
            seedParams(sQLiteDatabase);
            seedCsps(sQLiteDatabase);
            seedSettings(sQLiteDatabase);
            seedRemoteConfig(sQLiteDatabase);
            Auth.ensureDefaultSuperAdmin(sQLiteDatabase);
        } catch (Throwable ignored) {
        }
    }

    private void seedParams(SQLiteDatabase sQLiteDatabase) {
        try {
            String[][] strArr = DEFAULT_PARAMS;
            int length = strArr.length;
            int i = 0;
            int i2 = 1;
            while (i < length) {
                String[] strArr2 = strArr[i];
                ContentValues contentValues = new ContentValues();
                contentValues.put("field_key", strArr2[0]);
                contentValues.put("label", strArr2[1]);
                contentValues.put("short_label", strArr2[2]);
                contentValues.put("sort", Integer.valueOf(i2));
                contentValues.put("is_custom", (Integer) 0);
                sQLiteDatabase.insertWithOnConflict("report_param", null, contentValues, 4);
                i++;
                i2++;
            }
        } catch (Throwable ignored) {
        }
    }

    private void seedCsps(SQLiteDatabase sQLiteDatabase) {
        try {
            String now = Biz.now();
            for (String[] strArr : SEED_CSPS) {
                if (!exists(sQLiteDatabase, "SELECT id FROM csp WHERE csp_code = ?", strArr[0])) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("csp_code", strArr[0]);
                    contentValues.put("csp_name", strArr[1]);
                    contentValues.put("gender", strArr[2]);
                    contentValues.put("mobile_number", strArr[3]);
                    contentValues.put("branch_name", strArr[4]);
                    contentValues.put("branch_code", strArr[5]);
                    contentValues.put("status", Csp.ACTIVE);
                    contentValues.put("notes", "");
                    contentValues.put("created_at", now);
                    contentValues.put("updated_at", now);
                    sQLiteDatabase.insert("csp", null, contentValues);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void seedSettings(SQLiteDatabase sQLiteDatabase) {
        try {
            setIfAbsent(sQLiteDatabase, Settings.HOURLY_ENABLED, "0");
            setIfAbsent(sQLiteDatabase, Settings.HOURLY_START, "10");
            setIfAbsent(sQLiteDatabase, Settings.HOURLY_END, "18");
            setIfAbsent(sQLiteDatabase, Settings.CSP_SORT, "pdf");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_ENABLED, "1");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_PENDING, "1");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_FOLLOWUP, "1");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_LONG_PENDING, "1");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_COMPLETION, "1");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_PENDING_TIME, "16:00");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_FOLLOWUP_TIME, "18:00");
            setIfAbsent(sQLiteDatabase, Settings.NOTIF_LONG_THRESH, "3");
        } catch (Throwable ignored) {
        }
    }

    private void seedRemoteConfig(SQLiteDatabase db) {
        try {
            setRemoteConfigIfAbsent(db, "maintenance_mode", "0");
            setRemoteConfigIfAbsent(db, "announcement_active", "0");
            setRemoteConfigIfAbsent(db, "announcement_title", "System Notice");
            setRemoteConfigIfAbsent(db, "announcement_message", "");
            setRemoteConfigIfAbsent(db, "announcement_severity", "info");
            setRemoteConfigIfAbsent(db, "min_sync_interval_sec", "15");
        } catch (Throwable ignored) {
        }
    }

    private static void setRemoteConfigIfAbsent(SQLiteDatabase db, String key, String value) {
        try {
            if (!exists(db, "SELECT id FROM remote_config WHERE config_key = ?", key)) {
                ContentValues cv = new ContentValues();
                cv.put("config_key", key);
                cv.put("config_value", value);
                cv.put("is_active", 1);
                cv.put("updated_at", Biz.now());
                db.insertWithOnConflict("remote_config", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean exists(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        Cursor rawQuery = null;
        try {
            rawQuery = sQLiteDatabase.rawQuery(str, new String[]{str2});
            return rawQuery != null && rawQuery.moveToFirst();
        } catch (Throwable th) {
            return false;
        } finally {
            if (rawQuery != null) {
                try {
                    rawQuery.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void setIfAbsent(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        try {
            if (!exists(sQLiteDatabase, "SELECT setting_key FROM app_settings WHERE setting_key = ?", str)) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("setting_key", str);
                contentValues.put("setting_value", str2);
                sQLiteDatabase.insertWithOnConflict("app_settings", null, contentValues, 4);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Set<String> columnsOf(SQLiteDatabase sQLiteDatabase, String str) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        Cursor rawQuery = null;
        try {
            rawQuery = sQLiteDatabase.rawQuery("PRAGMA table_info(" + str + ")", null);
            if (rawQuery != null) {
                while (rawQuery.moveToNext()) {
                    linkedHashSet.add(rawQuery.getString(1).toLowerCase(Locale.US));
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (rawQuery != null) {
                try {
                    rawQuery.close();
                } catch (Throwable ignored) {}
            }
        }
        return linkedHashSet;
    }

    public static void addParamColumns(SQLiteDatabase sQLiteDatabase, String str) {
        try {
            if (!columnsOf(sQLiteDatabase, "daily_report").contains(str)) {
                sQLiteDatabase.execSQL("ALTER TABLE daily_report ADD COLUMN " + str + " INTEGER NOT NULL DEFAULT 0");
            }
            if (!columnsOf(sQLiteDatabase, "hourly_report").contains(str)) {
                sQLiteDatabase.execSQL("ALTER TABLE hourly_report ADD COLUMN " + str + " INTEGER NOT NULL DEFAULT 0");
            }
            if (!columnsOf(sQLiteDatabase, "csp").contains("tgt_" + str)) {
                sQLiteDatabase.execSQL("ALTER TABLE csp ADD COLUMN tgt_" + str + " INTEGER NOT NULL DEFAULT 0");
            }
        } catch (Throwable ignored) {
        }
    }

    public SQLiteDatabase w() {
        return getWritableDatabase();
    }

    public SQLiteDatabase r() {
        return getReadableDatabase();
    }
}
