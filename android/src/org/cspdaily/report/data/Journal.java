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
        public final String branch;
        public final String actionStatus;

        public Entry(int i, long j, String str, String str2, String str3, String str4, String str5) {
            this(i, j, str, str2, str3, str4, str5, "", "");
        }

        public Entry(int i, long j, String str, String str2, String str3, String str4, String str5, String branch, String actionStatus) {
            this.kind = i;
            this.cspId = j;
            this.code = str;
            this.name = str2;
            this.date = str3;
            this.time = str4;
            this.detail = str5;
            this.branch = branch != null ? branch : "";
            this.actionStatus = actionStatus != null ? actionStatus : "";
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
        return recent(sQLiteDatabase, i, l, null);
    }

    public static List<Entry> recentForDate(SQLiteDatabase sQLiteDatabase, int i, String date) {
        return recent(sQLiteDatabase, i, null, date);
    }

    public static List<Entry> recent(SQLiteDatabase sQLiteDatabase, int i, Long l, String date) {
        ArrayList<Entry> arrayList = new ArrayList<>();

        StringBuilder rWhere = new StringBuilder();
        ArrayList<String> rArgs = new ArrayList<>();
        if (l != null) {
            rWhere.append("r.csp_id = ? ");
            rArgs.add(String.valueOf(l));
        }
        if (date != null && !date.isEmpty()) {
            if (rWhere.length() > 0) rWhere.append("AND ");
            rWhere.append("r.business_date = ? ");
            rArgs.add(date);
        }
        String rWhereClause = rWhere.length() > 0 ? "WHERE " + rWhere.toString() : "";

        String rSql = "SELECT r.csp_id, c.csp_code, c.csp_name, r.business_date, r.submitted_at, (" + Params.sumSql(sQLiteDatabase, "r") + ") AS total, c.branch FROM daily_report r JOIN csp c ON c.id = r.csp_id " + rWhereClause + "ORDER BY r.submitted_at DESC LIMIT " + Math.max(i, 40);
        Cursor rawQuery = sQLiteDatabase.rawQuery(rSql, rArgs.toArray(new String[0]));
        try {
            while (rawQuery.moveToNext()) {
                try {
                    int total = rawQuery.getInt(5);
                    String branch = rawQuery.getString(6);
                    arrayList.add(new Entry(1, rawQuery.getLong(0), rawQuery.getString(1), rawQuery.getString(2), rawQuery.getString(3), rawQuery.getString(4), "Reported total " + total, branch, "Reported"));
                } catch (Exception ignored) {
                }
            }
        } finally {
            rawQuery.close();
        }

        StringBuilder fWhere = new StringBuilder();
        ArrayList<String> fArgs = new ArrayList<>();
        if (l != null) {
            fWhere.append("f.csp_id = ? ");
            fArgs.add(String.valueOf(l));
        }
        if (date != null && !date.isEmpty()) {
            if (fWhere.length() > 0) fWhere.append("AND ");
            fWhere.append("f.business_date = ? ");
            fArgs.add(date);
        }
        String fWhereClause = fWhere.length() > 0 ? "WHERE " + fWhere.toString() : "";

        String fSql = "SELECT f.csp_id, c.csp_code, c.csp_name, f.business_date, f.created_at, f.action_status, f.note, c.branch FROM followup_log f JOIN csp c ON c.id = f.csp_id " + fWhereClause + "ORDER BY f.id DESC LIMIT " + Math.max(i, 40);
        Cursor rawQuery2 = sQLiteDatabase.rawQuery(fSql, fArgs.toArray(new String[0]));
        try {
            while (rawQuery2.moveToNext()) {
                String actStatus = rawQuery2.getString(5);
                String nz = Csp.nz(rawQuery2.getString(6));
                String branch = rawQuery2.getString(7);
                arrayList.add(new Entry(2, rawQuery2.getLong(0), rawQuery2.getString(1), rawQuery2.getString(2), rawQuery2.getString(3), rawQuery2.getString(4), actStatus + (nz.isEmpty() ? "" : " · " + nz), branch, actStatus));
            }
        } finally {
            rawQuery2.close();
        }

        Collections.sort(arrayList, new Comparator<Entry>() {
            @Override
            public int compare(Entry entry, Entry entry2) {
                return Journal.str(entry2.time).compareTo(Journal.str(entry.time));
            }
        });
        return arrayList.size() > i ? new ArrayList<>(arrayList.subList(0, i)) : arrayList;
    }

    public static String str(String str) {
        return str == null ? "" : str;
    }
}
