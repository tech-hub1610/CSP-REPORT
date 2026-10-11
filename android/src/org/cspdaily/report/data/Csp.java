package org.cspdaily.report.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.domain.Biz;

/* loaded from: classes.dex */
public final class Csp {
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
    public long id;
    public String code = "";
    public String name = "";
    public String gender = "";
    public String mobile = "";
    public String branch = "";
    public String branchCode = "";
    public String status = ACTIVE;
    public String notes = "";
    public Map<String, Integer> targets = new LinkedHashMap();

    public String suffix() {
        return Biz.cspCodeSuffix(this.code);
    }

    public int targetTotal() {
        Iterator<Integer> it = this.targets.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            Integer next = it.next();
            i = (next == null ? 0 : next.intValue()) + i;
        }
        return i;
    }

    public static Csp from(Cursor cursor, List<String> list) {
        Csp csp = new Csp();
        csp.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        csp.code = str(cursor, "csp_code");
        csp.name = str(cursor, "csp_name");
        csp.gender = str(cursor, "gender");
        csp.mobile = str(cursor, "mobile_number");
        csp.branch = str(cursor, "branch_name");
        csp.branchCode = str(cursor, "branch_code");
        csp.status = str(cursor, "status");
        csp.notes = str(cursor, "notes");
        for (String str : list) {
            int columnIndex = cursor.getColumnIndex("tgt_" + str);
            csp.targets.put(str, Integer.valueOf((columnIndex < 0 || cursor.isNull(columnIndex)) ? 0 : cursor.getInt(columnIndex)));
        }
        return csp;
    }

    static String str(Cursor cursor, String str) {
        int columnIndex = cursor.getColumnIndex(str);
        return (columnIndex < 0 || cursor.isNull(columnIndex)) ? "" : cursor.getString(columnIndex);
    }

    static Integer intOrNull(Cursor cursor, String str) {
        int columnIndex = cursor.getColumnIndex(str);
        if (columnIndex < 0 || cursor.isNull(columnIndex)) {
            return null;
        }
        return Integer.valueOf(cursor.getInt(columnIndex));
    }

    public static String nz(String str) {
        return str == null ? "" : str;
    }

    public static Csp byId(SQLiteDatabase sQLiteDatabase, long j) {
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT * FROM csp WHERE id = ?", new String[]{String.valueOf(j)});
        try {
            if (rawQuery.moveToFirst()) {
                return from(rawQuery, Params.keys(sQLiteDatabase));
            }
            return null;
        } finally {
            rawQuery.close();
        }
    }

    public static Csp byCode(SQLiteDatabase sQLiteDatabase, String code) {
        if (code == null || code.trim().isEmpty()) return null;
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT * FROM csp WHERE csp_code = ? COLLATE NOCASE", new String[]{code.trim()});
        try {
            if (rawQuery.moveToFirst()) {
                return from(rawQuery, Params.keys(sQLiteDatabase));
            }
            return null;
        } finally {
            rawQuery.close();
        }
    }

    public static List<Csp> all(SQLiteDatabase sQLiteDatabase, boolean z, String str) {
        String str2;
        if ("code".equals(str)) {
            str2 = "csp_code COLLATE NOCASE ASC, id ASC";
        } else if ("branch".equals(str)) {
            str2 = "branch_name COLLATE NOCASE ASC, id ASC";
        } else {
            str2 = "csp_name COLLATE NOCASE ASC, id ASC";
        }
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT * FROM csp" + (z ? " WHERE status = 'ACTIVE'" : "") + " ORDER BY " + str2, null);
        try {
            ArrayList arrayList = new ArrayList();
            List<String> keys = Params.keys(sQLiteDatabase);
            while (rawQuery.moveToNext()) {
                arrayList.add(from(rawQuery, keys));
            }
            return arrayList;
        } finally {
            rawQuery.close();
        }
    }

    public static int activeCount(SQLiteDatabase sQLiteDatabase) {
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT COUNT(*) FROM csp WHERE status = 'ACTIVE'", null);
        try {
            return rawQuery.moveToFirst() ? rawQuery.getInt(0) : 0;
        } finally {
            rawQuery.close();
        }
    }

    public static ContentValues values(Csp csp, String str, boolean z) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("csp_code", csp.code);
        contentValues.put("csp_name", csp.name);
        contentValues.put("gender", csp.gender);
        contentValues.put("mobile_number", csp.mobile);
        contentValues.put("branch_name", csp.branch);
        contentValues.put("branch_code", csp.branchCode);
        contentValues.put("status", csp.status);
        contentValues.put("notes", csp.notes);
        for (String str2 : csp.targets.keySet()) {
            Integer num = csp.targets.get(str2);
            contentValues.put("tgt_" + str2, Integer.valueOf(num == null ? 0 : num.intValue()));
        }
        contentValues.put("updated_at", str);
        if (z) {
            contentValues.put("created_at", str);
        }
        return contentValues;
    }

    public static long insert(Context context, SQLiteDatabase sQLiteDatabase, Csp csp) {
        String now = Biz.now();
        for (String str : Params.keys(sQLiteDatabase)) {
            if (!csp.targets.containsKey(str)) {
                csp.targets.put(str, 0);
            }
        }
        return sQLiteDatabase.insertWithOnConflict("csp", null, values(csp, now, true), 2);
    }

    public static void update(SQLiteDatabase sQLiteDatabase, Csp csp) {
        sQLiteDatabase.update("csp", values(csp, Biz.now(), false), "id = ?", new String[]{String.valueOf(csp.id)});
    }

    public static void delete(SQLiteDatabase sQLiteDatabase, long j) {
        sQLiteDatabase.execSQL("DELETE FROM hourly_report WHERE csp_id = ?", new Object[]{Long.valueOf(j)});
        sQLiteDatabase.execSQL("DELETE FROM daily_report WHERE csp_id = ?", new Object[]{Long.valueOf(j)});
        sQLiteDatabase.execSQL("DELETE FROM pending_reason WHERE csp_id = ?", new Object[]{Long.valueOf(j)});
        sQLiteDatabase.execSQL("DELETE FROM followup_log WHERE csp_id = ?", new Object[]{Long.valueOf(j)});
        sQLiteDatabase.execSQL("DELETE FROM csp WHERE id = ?", new Object[]{Long.valueOf(j)});
    }

    public static void setStatus(SQLiteDatabase sQLiteDatabase, List<Long> list, String str) {
        if (!list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < list.size()) {
                sb.append(i == 0 ? "?" : ",?");
                i++;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("status", str);
            contentValues.put("updated_at", Biz.now());
            sQLiteDatabase.update("csp", contentValues, "id IN (" + ((Object) sb) + ")", (String[]) list.toArray(new String[0]));
        }
    }

    public static void saveTargets(SQLiteDatabase sQLiteDatabase, Map<String, Integer> map, Long l) {
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("tgt_").append(entry.getKey()).append(" = ?");
            arrayList.add(Integer.valueOf(entry.getValue() == null ? 0 : entry.getValue().intValue()));
        }
        if (sb.length() != 0) {
            arrayList.add(Biz.now());
            if (l != null) {
                arrayList.add(l);
                sQLiteDatabase.execSQL("UPDATE csp SET " + ((Object) sb) + ", updated_at = ? WHERE id = ?", arrayList.toArray());
            } else {
                sQLiteDatabase.execSQL("UPDATE csp SET " + ((Object) sb) + ", updated_at = ? WHERE status = '" + ACTIVE + "'", arrayList.toArray());
            }
        }
    }
}
