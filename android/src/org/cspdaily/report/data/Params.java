package org.cspdaily.report.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Params {
    private static List<P> cache;

    public static class P {
        public final boolean custom;
        public final int id;
        public final String key;
        public final String label;
        public final String shortLabel;
        public final int sort;

        P(int i, String str, String str2, String str3, int i2, boolean z) {
            this.id = i;
            this.key = str;
            this.label = str2;
            this.shortLabel = str3;
            this.sort = i2;
            this.custom = z;
        }
    }

    private Params() {
    }

    public static synchronized void reset() {
        synchronized (Params.class) {
            cache = null;
        }
    }

    public static synchronized List<P> all(SQLiteDatabase sQLiteDatabase) {
        List<P> list;
        synchronized (Params.class) {
            if (cache != null) {
                list = cache;
            } else {
                ArrayList<P> arrayList = new ArrayList<>();
                if (sQLiteDatabase != null && sQLiteDatabase.isOpen()) {
                    Cursor rawQuery = null;
                    try {
                        rawQuery = sQLiteDatabase.rawQuery("SELECT id, field_key, label, short_label, sort, is_custom FROM report_param ORDER BY sort, id", null);
                        if (rawQuery != null) {
                            while (rawQuery.moveToNext()) {
                                arrayList.add(new P(rawQuery.getInt(0), rawQuery.getString(1), rawQuery.getString(2), rawQuery.getString(3), rawQuery.getInt(4), rawQuery.getInt(5) != 0));
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
                }
                if (arrayList.isEmpty()) {
                    String[][] strArr = Db.DEFAULT_PARAMS;
                    int length = strArr.length;
                    int i2 = 1;
                    for (String[] strArr2 : strArr) {
                        arrayList.add(new P(i2, strArr2[0], strArr2[1], strArr2[2], i2, false));
                        i2++;
                    }
                }
                cache = arrayList;
                list = arrayList;
            }
        }
        return list;
    }

    public static String shortLabel(String key) {
        if (key == null) return "";
        for (String[] def : Db.DEFAULT_PARAMS) {
            if (def[0].equalsIgnoreCase(key)) {
                return def[2];
            }
        }
        return key.replace("_", " ");
    }

    public static Map<String, String> labels(SQLiteDatabase sQLiteDatabase) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        for (P p : all(sQLiteDatabase)) {
            linkedHashMap.put(p.key, p.label);
        }
        return linkedHashMap;
    }

    public static Map<String, String> shorts(SQLiteDatabase sQLiteDatabase) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        for (P p : all(sQLiteDatabase)) {
            linkedHashMap.put(p.key, p.shortLabel);
        }
        return linkedHashMap;
    }

    public static List<String> keys(SQLiteDatabase sQLiteDatabase) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (P p : all(sQLiteDatabase)) {
            arrayList.add(p.key);
        }
        return arrayList;
    }

    public static String keysSql(SQLiteDatabase sQLiteDatabase) {
        StringBuilder sb = new StringBuilder();
        for (String str : keys(sQLiteDatabase)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(str);
        }
        return sb.toString();
    }

    public static String sumSql(SQLiteDatabase sQLiteDatabase, String str, boolean z) {
        String str2 = (str == null || str.isEmpty()) ? "" : str + ".";
        StringBuilder sb = new StringBuilder();
        for (String str3 : keys(sQLiteDatabase)) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(z ? "COALESCE(" + str2 + str3 + ", 0)" : str2 + str3);
        }
        return sb.length() == 0 ? "0" : sb.toString();
    }

    public static String sumSql(SQLiteDatabase sQLiteDatabase) {
        return sumSql(sQLiteDatabase, "", false);
    }

    public static String sumSql(SQLiteDatabase sQLiteDatabase, String str) {
        return sumSql(sQLiteDatabase, str, false);
    }

    public static String tgtListSql(SQLiteDatabase sQLiteDatabase, String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : keys(sQLiteDatabase)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(str).append(".tgt_").append(str2);
        }
        return sb.toString();
    }

    public static String fieldListSql(SQLiteDatabase sQLiteDatabase, String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : keys(sQLiteDatabase)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(str).append(".").append(str2);
        }
        return sb.toString();
    }

    public static String sumEachSql(SQLiteDatabase sQLiteDatabase, String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : keys(sQLiteDatabase)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("COALESCE(SUM(").append(str).append(str2).append("), 0) AS ").append(str2);
        }
        return sb.toString();
    }

    public static String shortOf(SQLiteDatabase sQLiteDatabase, String str) {
        for (P p : all(sQLiteDatabase)) {
            if (p.key.equals(str)) {
                return p.shortLabel;
            }
        }
        return str;
    }
}
