package org.cspdaily.report.domain;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Params;

/* loaded from: classes.dex */
public final class Agg {
    private static List<String> KEYS = new ArrayList();

    private Agg() {
    }

    private static String sumEach() {
        StringBuilder sb = new StringBuilder();
        for (String str : KEYS) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("COALESCE(SUM(").append(str).append("), 0) AS ").append(str);
        }
        return sb.toString();
    }

    private static List<String> keys(SQLiteDatabase sQLiteDatabase) {
        KEYS = Params.keys(sQLiteDatabase);
        return KEYS;
    }

    public static List<Map<String, String>> month(SQLiteDatabase sQLiteDatabase, String str) {
        ArrayList arrayList = new ArrayList();
        keys(sQLiteDatabase);
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT business_date, COUNT(*) AS reported, " + sumEach() + ", (" + Params.sumSql(sQLiteDatabase) + ") AS total_business FROM daily_report WHERE substr(business_date,1,7) = ? GROUP BY business_date ORDER BY business_date", new String[]{str});
        try {
            while (rawQuery.moveToNext()) {
                arrayList.add(read(rawQuery, KEYS, new String[]{"business_date", "reported"}));
            }
        } finally {
            rawQuery.close();
        }
        return arrayList;
    }

    public static List<Map<String, String>> cspMonth(SQLiteDatabase sQLiteDatabase, String str) {
        ArrayList arrayList = new ArrayList();
        keys(sQLiteDatabase);
        StringBuilder sb = new StringBuilder();
        for (String str2 : KEYS) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("COALESCE(SUM(r.").append(str2).append("), 0) AS ").append(str2);
        }
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT c.csp_code, c.csp_name, c.branch_name, c.branch_code, COUNT(r.id) AS days, " + ((Object) sb) + ", COALESCE(SUM(" + Params.sumSql(sQLiteDatabase, "r") + "), 0) AS total_business FROM csp c LEFT JOIN daily_report r ON r.csp_id = c.id AND substr(r.business_date,1,7) = ? WHERE c.status = '" + Csp.ACTIVE + "' GROUP BY c.id ORDER BY c.csp_name COLLATE NOCASE ASC, c.id ASC", new String[]{str});
        try {
            while (rawQuery.moveToNext()) {
                arrayList.add(read(rawQuery, KEYS, new String[]{"csp_code", "csp_name", "branch_name", "branch_code", "days"}));
            }
        } finally {
            rawQuery.close();
        }
        return arrayList;
    }

    public static List<Map<String, String>> branchMonth(SQLiteDatabase sQLiteDatabase, String str) {
        ArrayList arrayList = new ArrayList();
        keys(sQLiteDatabase);
        StringBuilder sb = new StringBuilder();
        for (String str2 : KEYS) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("COALESCE(SUM(r.").append(str2).append("), 0) AS ").append(str2);
        }
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT c.branch_name, GROUP_CONCAT(DISTINCT c.branch_code) AS branch_codes, COUNT(DISTINCT c.id) AS csps, COUNT(r.id) AS reports, " + ((Object) sb) + ", COALESCE(SUM(" + Params.sumSql(sQLiteDatabase, "r") + "), 0) AS total_business FROM csp c LEFT JOIN daily_report r ON r.csp_id = c.id AND substr(r.business_date,1,7) = ? WHERE c.status = '" + Csp.ACTIVE + "' GROUP BY c.branch_name ORDER BY total_business DESC, c.branch_name", new String[]{str});
        try {
            while (rawQuery.moveToNext()) {
                arrayList.add(read(rawQuery, KEYS, new String[]{"branch_name", "branch_codes", "csps", "reports"}));
            }
        } finally {
            rawQuery.close();
        }
        return arrayList;
    }

    private static Map<String, String> read(Cursor cursor, List<String> list, String[] strArr) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (String str : strArr) {
            int columnIndex = cursor.getColumnIndex(str);
            linkedHashMap.put(str, (columnIndex < 0 || cursor.isNull(columnIndex)) ? "" : cursor.getString(columnIndex));
        }
        for (String str2 : list) {
            int columnIndex2 = cursor.getColumnIndex(str2);
            linkedHashMap.put(str2, (columnIndex2 < 0 || cursor.isNull(columnIndex2)) ? "0" : Integer.toString(cursor.getInt(columnIndex2)));
        }
        int columnIndex3 = cursor.getColumnIndex("total_business");
        linkedHashMap.put("total_business", (columnIndex3 < 0 || cursor.isNull(columnIndex3)) ? "0" : Integer.toString(cursor.getInt(columnIndex3)));
        return linkedHashMap;
    }

    public static List<String> months(SQLiteDatabase sQLiteDatabase, int i) {
        ArrayList arrayList = new ArrayList();
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT substr(business_date,1,7) AS ym FROM daily_report GROUP BY ym ORDER BY ym DESC LIMIT " + i, null);
        try {
            while (rawQuery.moveToNext()) {
                arrayList.add(rawQuery.getString(0));
            }
        } finally {
            rawQuery.close();
        }
        return arrayList;
    }
}
