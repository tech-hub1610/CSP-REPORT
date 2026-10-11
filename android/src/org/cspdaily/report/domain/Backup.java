package org.cspdaily.report.domain;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.data.Db;
import org.cspdaily.report.data.Params;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class Backup {
    public static final String APP_ID = "CSP Daily Report";
    private static final String[][] ENTITIES = {new String[]{"csps", "csp"}, new String[]{"daily_reports", "daily_report"}, new String[]{"pending_reasons", "pending_reason"}, new String[]{"followup_logs", "followup_log"}, new String[]{"business_days", "business_day"}, new String[]{"office_notes", "office_note"}};
    private static final String[][] EXTRAS = {new String[]{"hourly_reports", "hourly_report"}, new String[]{"app_settings", "app_settings"}, new String[]{"report_params", "report_param"}, new String[]{"daily_csp_targets", "daily_csp_target"}};
    public static final int VERSION = 3;

    private Backup() {
    }

    /* loaded from: classes.dex */
    public static class Result {
        public final String message;
        public final boolean ok;
        public final int reports;
        public final int rows;

        Result(boolean z, String str, int i, int i2) {
            this.ok = z;
            this.message = str;
            this.reports = i;
            this.rows = i2;
        }
    }

    public static String exportJson(SQLiteDatabase sQLiteDatabase) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("app", APP_ID);
        jSONObject.put("version", 3);
        jSONObject.put("exported_at", Biz.now());
        for (String[] strArr : ENTITIES) {
            jSONObject.put(strArr[0], rows(sQLiteDatabase, strArr[1]));
        }
        for (String[] strArr2 : EXTRAS) {
            jSONObject.put(strArr2[0], rows(sQLiteDatabase, strArr2[1]));
        }
        return jSONObject.toString(2);
    }

    private static JSONArray rows(SQLiteDatabase sQLiteDatabase, String str) {
        JSONArray jSONArray = new JSONArray();
        ArrayList<String> arrayList = new ArrayList(Db.columnsOf(sQLiteDatabase, str));
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT * FROM " + str, null);
        try {
            while (rawQuery.moveToNext()) {
                try {
                    JSONObject jSONObject = new JSONObject();
                    for (String str2 : arrayList) {
                        int columnIndex = rawQuery.getColumnIndex(str2);
                        if (columnIndex >= 0) {
                            try {
                                jSONObject.put(str2, rawQuery.isNull(columnIndex) ? JSONObject.NULL : rawQuery.getString(columnIndex));
                            } catch (Exception e) {
                            }
                        }
                    }
                    jSONArray.put(jSONObject);
                } catch (Exception e2) {
                }
            }
        } finally {
            rawQuery.close();
        }
        return jSONArray;
    }

    public static void write(Context context, Uri uri, String str) throws Exception {
        OutputStream openOutputStream = context.getContentResolver().openOutputStream(uri, "wt");
        if (openOutputStream == null) {
            throw new IOException("Could not open the selected file");
        }
        try {
            openOutputStream.write(str.getBytes(Charset.forName("UTF-8")));
            openOutputStream.flush();
        } finally {
            openOutputStream.close();
        }
    }

    public static String read(Context context, Uri uri) throws Exception {
        InputStream openInputStream = context.getContentResolver().openInputStream(uri);
        if (openInputStream == null) {
            throw new IOException("Could not open the selected file");
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(openInputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    sb.append(readLine).append('\n');
                } else {
                    return sb.toString();
                }
            }
        } finally {
            openInputStream.close();
        }
    }

    public static Result restore(Context context, String str) {
        ContentValues values;
        JSONArray optJSONArray;
        int i;
        try {
            JSONObject jSONObject = new JSONObject(str);
            if (!APP_ID.equals(jSONObject.optString("app", ""))) {
                return new Result(false, "Not a CSP Daily Report backup (missing app id).", 0, 0);
            }
            if (jSONObject.optJSONArray("daily_reports") == null) {
                return new Result(false, "Not a valid backup file (no daily_reports).", 0, 0);
            }
            int optInt = jSONObject.optInt("version", 0);
            if (optInt > 3) {
                return new Result(false, "Backup is from a newer version (" + optInt + ").", 0, 0);
            }
            for (String[] strArr : ENTITIES) {
                JSONArray optJSONArray2 = jSONObject.optJSONArray(strArr[0]);
                if (optJSONArray2 != null && !validTableShape(strArr[1], optJSONArray2)) {
                    return new Result(false, "Backup row in “" + strArr[0] + "” is unusable.", 0, 0);
                }
            }
            SQLiteDatabase db = Core.get(context).db();
            List<String> keys = Params.keys(db);
            boolean inTransaction = db.inTransaction();
            int i2 = 0;
            try {
                if (!inTransaction) {
                    try {
                        db.beginTransaction();
                    } catch (Exception e) {
                        Result result = new Result(false, "Restore failed, nothing was changed: " + e.getMessage(), 0, 0);
                        if (inTransaction) {
                            return result;
                        }
                        db.endTransaction();
                        return result;
                    }
                }
                HashMap hashMap = new HashMap();
                Cursor rawQuery = db.rawQuery("SELECT id, csp_code FROM csp", null);
                while (rawQuery.moveToNext()) {
                    try {
                        hashMap.put(rawQuery.getString(1), Long.valueOf(rawQuery.getLong(0)));
                    } catch (Throwable th) {
                        rawQuery.close();
                        throw th;
                    }
                }
                rawQuery.close();
                int mergeCsps = mergeCsps(db, jSONObject.optJSONArray("csps"), keys, hashMap) + 0;
                for (String[] strArr2 : ENTITIES) {
                    if (!"csps".equals(strArr2[0]) && (optJSONArray = jSONObject.optJSONArray(strArr2[0])) != null) {
                        db.delete(strArr2[1], null, null);
                        int i3 = 0;
                        while (i3 < optJSONArray.length()) {
                            JSONObject optJSONObject = optJSONArray.optJSONObject(i3);
                            if (optJSONObject == null) {
                                i = mergeCsps;
                            } else {
                                ContentValues values2 = toValues(optJSONObject, colsOf(db, strArr2[1]), keys);
                                if (values2 == null) {
                                    i = mergeCsps;
                                } else {
                                    long longValue = values2.getAsLong("csp_id") == null ? 0L : values2.getAsLong("csp_id").longValue();
                                    if (strArr2[1].equals("daily_report") || strArr2[1].equals("pending_reason") || strArr2[1].equals("followup_log")) {
                                        String firstCode = firstCode(optJSONObject, longValue, hashMap);
                                        Long l = firstCode == null ? null : (Long) hashMap.get(firstCode);
                                        if (l == null) {
                                            i = mergeCsps;
                                        } else {
                                            values2.put("csp_id", l);
                                        }
                                    }
                                    if (db.insert(strArr2[1], null, values2) > 0) {
                                        i = mergeCsps + 1;
                                        if ("daily_report".equals(strArr2[1])) {
                                            i2++;
                                        }
                                    } else {
                                        i = mergeCsps;
                                    }
                                }
                            }
                            i3++;
                            mergeCsps = i;
                        }
                    }
                }
                int i4 = mergeCsps;
                for (String[] strArr3 : EXTRAS) {
                    JSONArray optJSONArray3 = jSONObject.optJSONArray(strArr3[0]);
                    if (optJSONArray3 != null) {
                        db.delete(strArr3[1], null, null);
                        for (int i5 = 0; i5 < optJSONArray3.length(); i5++) {
                            JSONObject optJSONObject2 = optJSONArray3.optJSONObject(i5);
                            if (optJSONObject2 != null && (values = toValues(optJSONObject2, colsOf(db, strArr3[1]), keys)) != null && db.insert(strArr3[1], null, values) > 0) {
                                i4++;
                            }
                        }
                    }
                }
                if (!inTransaction) {
                    db.setTransactionSuccessful();
                }
                Params.reset();
                String str2 = i2 + " reports restored";
                Result result2 = new Result(true, i4 > i2 ? str2 + ", " + i4 + " rows total" : str2, i2, i4);
                if (!inTransaction) {
                    db.endTransaction();
                }
                return result2;
            } catch (Throwable th2) {
                if (!inTransaction) {
                    db.endTransaction();
                }
                throw th2;
            }
        } catch (Exception e2) {
            return new Result(false, "Not a valid backup file (unreadable JSON).", 0, 0);
        }
    }

    private static int mergeCsps(SQLiteDatabase sQLiteDatabase, JSONArray jSONArray, List<String> list, Map<String, Long> map) {
        ContentValues values;
        if (jSONArray == null) {
            return 0;
        }
        List<String> colsOf = colsOf(sQLiteDatabase, "csp");
        String now = Biz.now();
        int i = 0;
        for (int i2 = 0; i2 < jSONArray.length(); i2++) {
            JSONObject optJSONObject = jSONArray.optJSONObject(i2);
            if (optJSONObject != null) {
                String str = str(optJSONObject, "csp_code");
                if (!str.isEmpty() && (values = toValues(optJSONObject, colsOf, list)) != null) {
                    values.remove("id");
                    values.put("csp_code", str);
                    values.put("updated_at", now);
                    Long l = map.get(str);
                    if (l == null) {
                        values.put("created_at", now);
                        long insert = sQLiteDatabase.insert("csp", null, values);
                        if (insert > 0) {
                            map.put(str, Long.valueOf(insert));
                            i++;
                        }
                    } else {
                        i += sQLiteDatabase.update("csp", values, "id = ?", new String[]{String.valueOf(l)});
                    }
                }
            }
        }
        return i;
    }

    private static String firstCode(JSONObject jSONObject, long j, Map<String, Long> map) {
        String str = str(jSONObject, "csp_code");
        if (str.isEmpty()) {
            return null;
        }
        return str;
    }

    private static ContentValues toValues(JSONObject jSONObject, List<String> list, List<String> list2) {
        if (list.isEmpty()) {
            return null;
        }
        ContentValues contentValues = new ContentValues();
        Iterator<String> keys = jSONObject.keys();
        while (keys.hasNext()) {
            String next = keys.next();
            if (list.contains(next)) {
                Object opt = jSONObject.opt(next);
                if (opt == null || opt == JSONObject.NULL) {
                    contentValues.putNull(next);
                } else {
                    String trim = String.valueOf(opt).trim();
                    if (isNumericColumn(next, list, list2)) {
                        int cleanCount = Biz.cleanCount(trim);
                        if (cleanCount < 0) {
                            cleanCount = 0;
                        }
                        contentValues.put(next, Integer.valueOf(cleanCount));
                    } else {
                        contentValues.put(next, trim);
                    }
                }
            }
        }
        return contentValues.size() == 0 ? null : contentValues;
    }

    private static boolean isNumericColumn(String str, List<String> list, List<String> list2) {
        if ("id".equals(str) || "csp_id".equals(str) || "hour".equals(str) || "sort".equals(str) || "is_custom".equals(str) || str.startsWith("tgt_")) {
            return true;
        }
        return list2.contains(str);
    }

    private static List<String> colsOf(SQLiteDatabase sQLiteDatabase, String str) {
        try {
            return new ArrayList(Db.columnsOf(sQLiteDatabase, str));
        } catch (Exception e) {
            return new ArrayList();
        }
    }

    private static boolean validTableShape(String str, JSONArray jSONArray) {
        for (int i = 0; i < jSONArray.length(); i++) {
            if (jSONArray.optJSONObject(i) == null) {
                return false;
            }
        }
        return true;
    }

    private static String str(JSONObject jSONObject, String str) {
        Object opt = jSONObject.opt(str);
        return (opt == null || opt == JSONObject.NULL) ? "" : String.valueOf(opt).trim();
    }
}
