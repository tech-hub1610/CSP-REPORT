package org.cspdaily.report.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.cspdaily.report.domain.Biz;

/* loaded from: classes.dex */
public final class Journal {
    public static final int FOLLOWUP = 2;
    public static final int REPORT = 1;

    /* loaded from: classes.dex */
    public static class Entry {
        public final String code;
        public final long cspId;
        public final String date;
        public final String detail;
        public final int kind;
        public final String name;
        public final String time;

        Entry(int i, long j, String str, String str2, String str3, String str4, String str5) {
            this.kind = i;
            this.cspId = j;
            this.code = str;
            this.name = str2;
            this.date = str3;
            this.time = str4;
            this.detail = str5;
        }

        public String key() {
            return this.kind + "|" + this.cspId + "|" + this.time;
        }

        public String when() {
            return Biz.shortWhen(this.time);
        }
    }

    private Journal() {
    }

    public static List<Entry> recent(SQLiteDatabase sQLiteDatabase, int i, Long l) {
        ArrayList arrayList = new ArrayList();
        String str = l == null ? "" : "WHERE x.csp_id = " + l + " ";
        String str2 = l == null ? "" : "WHERE r.csp_id = " + l + " ";
        if (l != null) {
            String str3 = "WHERE f.csp_id = " + l + " ";
        }
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT r.csp_id, c.csp_code, c.csp_name, r.business_date, r.submitted_at, (" + Params.sumSql(sQLiteDatabase, "r") + ") AS total FROM daily_report r JOIN csp c ON c.id = r.csp_id " + str2 + "ORDER BY r.submitted_at DESC LIMIT 40", null);
        try {
            while (rawQuery.moveToNext()) {
                try {
                    arrayList.add(new Entry(1, rawQuery.getLong(0), rawQuery.getString(1), rawQuery.getString(2), rawQuery.getString(3), rawQuery.getString(4), "Reported total " + rawQuery.getInt(5)));
                } catch (Exception e) {
                }
            }
        } finally {
            rawQuery.close();
        }
        Cursor rawQuery2 = sQLiteDatabase.rawQuery("SELECT f.csp_id, c.csp_code, c.csp_name, f.business_date, f.created_at, f.action_status, f.note FROM followup_log f JOIN csp c ON c.id = f.csp_id " + (l != null ? "WHERE f.csp_id = " + l + " " : str) + "ORDER BY f.id DESC LIMIT 40", null);
        try {
            while (rawQuery2.moveToNext()) {
                String nz = Csp.nz(rawQuery2.getString(6));
                arrayList.add(new Entry(2, rawQuery2.getLong(0), rawQuery2.getString(1), rawQuery2.getString(2), rawQuery2.getString(3), rawQuery2.getString(4), rawQuery2.getString(5) + (nz.isEmpty() ? "" : " · " + nz)));
            }
        } finally {
            rawQuery2.close();
        }
        Collections.sort(arrayList, new Comparator<Entry>() { // from class: org.cspdaily.report.data.Journal.1
            @Override // java.util.Comparator
            public int compare(Entry entry, Entry entry2) {
                return Journal.str(entry2.time).compareTo(Journal.str(entry.time));
            }
        });
        return arrayList.size() > i ? new ArrayList(arrayList.subList(0, i)) : arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String str(String str) {
        return str == null ? "" : str;
    }
}
