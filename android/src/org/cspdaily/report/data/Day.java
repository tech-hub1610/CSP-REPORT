package org.cspdaily.report.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.DaySummary;

/* loaded from: classes.dex */
public final class Day {
    public static final String NOT_CONTACTED = "Not Contacted";
    public static final String[] FOLLOWUP_STATUSES = {NOT_CONTACTED, "Called", "WhatsApp Sent", "Contacted", "No Response", "Note Added"};
    public static final String[] PENDING_REASONS = {"No Response", "Closed Today", "Technical Issue", "Branch Issue", "Will Report Later", "Not receiving call", "Not responding to message", "Switched off / unreachable", "Busy - will report later"};
    public static final String[] DAY_TYPES = {DaySummary.WORKING, DaySummary.SUNDAY, DaySummary.HOLIDAY, "CLOSURE"};

    private Day() {
    }

    public static String[] dayType(SQLiteDatabase sQLiteDatabase, String str) {
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT day_type, description FROM business_day WHERE business_date = ?", new String[]{str});
        try {
            if (rawQuery.moveToFirst()) {
                return new String[]{rawQuery.getString(0), Csp.nz(rawQuery.getString(1))};
            }
            rawQuery.close();
            boolean z = Biz.dayOfWeek(str) == 0;
            String[] strArr = new String[2];
            strArr[0] = z ? DaySummary.SUNDAY : DaySummary.WORKING;
            strArr[1] = z ? "Sunday" : "";
            return strArr;
        } finally {
            rawQuery.close();
        }
    }

    public static void saveDayType(SQLiteDatabase sQLiteDatabase, String str, String str2, String str3) {
        String now = Biz.now();
        ContentValues contentValues = new ContentValues();
        contentValues.put("day_type", str2);
        if (str3 == null) {
            str3 = "";
        }
        contentValues.put("description", str3);
        contentValues.put("updated_at", now);
        if (sQLiteDatabase.update("business_day", contentValues, "business_date = ?", new String[]{str}) == 0) {
            contentValues.put("business_date", str);
            contentValues.put("created_at", now);
            sQLiteDatabase.insert("business_day", null, contentValues);
        }
    }

    public static boolean isWorkingDate(SQLiteDatabase sQLiteDatabase, String str) {
        return DaySummary.WORKING.equals(dayType(sQLiteDatabase, str)[0]);
    }

    public static String officeNote(SQLiteDatabase sQLiteDatabase, String str) {
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT note FROM office_note WHERE business_date = ?", new String[]{str});
        try {
            return rawQuery.moveToFirst() ? Csp.nz(rawQuery.getString(0)) : "";
        } finally {
            rawQuery.close();
        }
    }

    public static void saveOfficeNote(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        String now = Biz.now();
        String trim = str2 == null ? "" : str2.trim();
        if (trim.isEmpty()) {
            sQLiteDatabase.delete("office_note", "business_date = ?", new String[]{str});
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("note", trim);
        contentValues.put("updated_at", now);
        if (sQLiteDatabase.update("office_note", contentValues, "business_date = ?", new String[]{str}) == 0) {
            contentValues.put("business_date", str);
            contentValues.put("created_at", now);
            sQLiteDatabase.insert("office_note", null, contentValues);
        }
    }

    public static List<CspDay> dayData(SQLiteDatabase sQLiteDatabase, String str) {
        return dayData(sQLiteDatabase, str, true);
    }

    public static List<CspDay> dayData(SQLiteDatabase sQLiteDatabase, String str, boolean z) {
        List<String> keys = Params.keys(sQLiteDatabase);
        String fieldListSql = Params.fieldListSql(sQLiteDatabase, "r");
        String tgtListSql = Params.tgtListSql(sQLiteDatabase, "c");
        Map<Long, Map<String, Integer>> dailyTargetMap = new HashMap<>();
        try {
            Cursor dtCursor = sQLiteDatabase.rawQuery("SELECT csp_id, field_key, target_value FROM daily_csp_target WHERE business_date = ?", new String[]{str});
            try {
                while (dtCursor.moveToNext()) {
                    long cspId = dtCursor.getLong(0);
                    String fKey = dtCursor.getString(1);
                    int tVal = dtCursor.getInt(2);
                    Map<String, Integer> tm = dailyTargetMap.get(Long.valueOf(cspId));
                    if (tm == null) {
                        tm = new HashMap<>();
                        dailyTargetMap.put(Long.valueOf(cspId), tm);
                    }
                    tm.put(fKey, Integer.valueOf(tVal));
                }
            } finally {
                dtCursor.close();
            }
        } catch (Throwable ignored) {}

        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT c.id, c.csp_code, c.csp_name, c.gender, c.mobile_number, c.branch_name, c.branch_code, c.status, c.notes, " + tgtListSql + ", r.id AS report_id, " + fieldListSql + ", r.remarks, r.submitted_at, p.reason AS pending_reason FROM csp c LEFT JOIN daily_report r ON r.csp_id = c.id AND r.business_date = ? LEFT JOIN pending_reason p ON p.csp_id = c.id AND p.business_date = ? " + (z ? "WHERE c.status = 'ACTIVE' " : "") + "ORDER BY c.csp_name COLLATE NOCASE ASC, c.id ASC", new String[]{str, str});
        ArrayList arrayList = new ArrayList();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        while (rawQuery.moveToNext()) {
            try {
                CspDay cspDay = new CspDay();
                cspDay.id = rawQuery.getLong(0);
                cspDay.code = s(rawQuery, 1);
                cspDay.name = s(rawQuery, 2);
                cspDay.gender = s(rawQuery, 3);
                cspDay.mobile = s(rawQuery, 4);
                cspDay.branch = s(rawQuery, 5);
                cspDay.branchCode = s(rawQuery, 6);
                cspDay.status = s(rawQuery, 7);
                cspDay.notes = s(rawQuery, 8);
                int i3 = 9;
                int i2 = i3 + keys.size();
                Map<String, Integer> customTargets = dailyTargetMap.get(Long.valueOf(cspDay.id));
                int totalTarget = 0;
                for (String next : keys) {
                    int t = 0;
                    if (customTargets != null && customTargets.containsKey(next)) {
                        t = customTargets.get(next).intValue();
                    }
                    cspDay.targets.put(next, Integer.valueOf(t));
                    totalTarget += t;
                }
                cspDay.dailyTarget = totalTarget;
                Iterator<String> it2 = keys.iterator();
                int i6 = i2 + 1;
                while (it2.hasNext()) {
                    cspDay.values.put(it2.next(), Integer.valueOf(rawQuery.isNull(i6) ? 0 : rawQuery.getInt(i6)));
                    i6++;
                }
                cspDay.reported = !rawQuery.isNull(i2);
                cspDay.remarks = rawQuery.isNull(i6) ? "" : s(rawQuery, i6);
                int i7 = i6 + 1;
                cspDay.submittedAt = rawQuery.isNull(i7) ? null : s(rawQuery, i7);
                int i8 = i7 + 1;
                cspDay.pendingReason = rawQuery.isNull(i8) ? null : s(rawQuery, i8);
                int i9 = 0;
                if (cspDay.reported) {
                    Iterator<Integer> it3 = cspDay.values.values().iterator();
                    int i10 = 0;
                    while (it3.hasNext()) {
                        Integer next2 = it3.next();
                        i10 += next2 == null ? 0 : next2.intValue();
                    }
                    i9 = i10;
                }
                cspDay.totalBusiness = i9;
                cspDay.isNil = cspDay.reported && i9 == 0;
                arrayList.add(cspDay);
                linkedHashMap.put(Long.valueOf(cspDay.id), cspDay);
            } finally {
            }
        }
        rawQuery.close();
        if (linkedHashMap.isEmpty()) {
            return arrayList;
        }
        rawQuery = sQLiteDatabase.rawQuery("SELECT csp_id, action_status, created_at, note FROM followup_log WHERE business_date = ? ORDER BY id ASC", new String[]{str});
        while (rawQuery.moveToNext()) {
            try {
                CspDay cspDay2 = (CspDay) linkedHashMap.get(Long.valueOf(rawQuery.getLong(0)));
                if (cspDay2 != null) {
                    cspDay2.followupStatus = s(rawQuery, 1);
                    cspDay2.followupTime = s(rawQuery, 2);
                    String noteVal = s(rawQuery, 3);
                    if (noteVal != null && !noteVal.trim().isEmpty()
                            && !"Call placed".equalsIgnoreCase(noteVal.trim())
                            && !"Message reminder sent".equalsIgnoreCase(noteVal.trim())) {
                        cspDay2.followupNote = noteVal.trim();
                    }
                }
            } finally {
            }
        }
        rawQuery.close();
        rawQuery = sQLiteDatabase.rawQuery("SELECT csp_id, action_status, created_at FROM followup_log ORDER BY id ASC", null);
        while (rawQuery.moveToNext()) {
            try {
                CspDay cspDay3 = (CspDay) linkedHashMap.get(Long.valueOf(rawQuery.getLong(0)));
                if (cspDay3 != null) {
                    cspDay3.lastContactedStatus = s(rawQuery, 1);
                    cspDay3.lastContactedAt = s(rawQuery, 2);
                }
            } finally {
            }
        }
        rawQuery.close();
        String plusDays = Biz.plusDays(str, -1);
        StringBuilder sb = new StringBuilder();
        for (String str2 : keys) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(str2);
        }
        rawQuery = sQLiteDatabase.rawQuery("SELECT csp_id, " + ((Object) sb) + ", (" + Params.sumSql(sQLiteDatabase) + ") AS total FROM daily_report WHERE business_date = ?", new String[]{plusDays});
        while (rawQuery.moveToNext()) {
            try {
                CspDay cspDay4 = (CspDay) linkedHashMap.get(Long.valueOf(rawQuery.getLong(0)));
                if (cspDay4 != null) {
                    Iterator<String> it4 = keys.iterator();
                    int i11 = 1;
                    while (it4.hasNext()) {
                        cspDay4.yesterdayValues.put(it4.next(), Integer.valueOf(rawQuery.isNull(i11) ? 0 : rawQuery.getInt(i11)));
                        i11++;
                    }
                    cspDay4.yesterdayTotal = Integer.valueOf(rawQuery.getInt(i11));
                }
            } finally {
            }
        }
        rawQuery.close();
        applyPendingAndLastReport(sQLiteDatabase, linkedHashMap, str);
        return arrayList;
    }

    private static void applyPendingAndLastReport(SQLiteDatabase sQLiteDatabase, Map<Long, CspDay> map, String str) {
        HashMap hashMap = new HashMap();
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT business_date, day_type FROM business_day", null);
        while (rawQuery.moveToNext()) {
            try {
                hashMap.put(rawQuery.getString(0), rawQuery.getString(1));
            } finally {
            }
        }
        rawQuery.close();
        StringBuilder sb = new StringBuilder();
        for (Long l : map.keySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(l);
        }
        HashMap hashMap2 = new HashMap();
        rawQuery = sQLiteDatabase.rawQuery("SELECT csp_id, business_date, submitted_at, (" + Params.sumSql(sQLiteDatabase) + ") AS total FROM daily_report WHERE business_date <= ? AND csp_id IN (" + ((Object) sb) + ") ORDER BY business_date ASC, id ASC", new String[]{str});
        while (rawQuery.moveToNext()) {
            try {
                long j = rawQuery.getLong(0);
                TreeMap2 treeMap2 = (TreeMap2) hashMap2.get(Long.valueOf(j));
                if (treeMap2 == null) {
                    treeMap2 = new TreeMap2();
                    hashMap2.put(Long.valueOf(j), treeMap2);
                }
                treeMap2.put(rawQuery.getString(1), new String[]{rawQuery.getString(2), String.valueOf(rawQuery.getInt(3))});
            } finally {
            }
        }
        rawQuery.close();
        for (CspDay cspDay : map.values()) {
            TreeMap2 treeMap22 = (TreeMap2) hashMap2.get(Long.valueOf(cspDay.id));
            if (treeMap22 == null) {
                treeMap22 = TreeMap2.EMPTY;
            }
            cspDay.pendingDays = treeMap22.contains(str) ? 0 : walkPending(cspDay, treeMap22, hashMap, str);
            String[] lastUpTo = treeMap22.lastUpTo(str);
            if (lastUpTo != null) {
                cspDay.hasLastReport = true;
                cspDay.lastReportDate = treeMap22.lastKey;
                cspDay.lastReportSubmittedAt = lastUpTo[0];
                cspDay.lastReportTotal = Integer.parseInt(lastUpTo[1]);
            }
        }
    }

    private static int walkPending(CspDay cspDay, TreeMap2 treeMap2, Map<String, String> map, String str) {
        int i;
        int i2 = 0;
        int i3 = 0;
        while (true) {
            i = i2;
            if (i3 >= 60) {
                break;
            }
            String str2 = map.get(str);
            if (str2 == null) {
                str2 = Biz.dayOfWeek(str) == 0 ? DaySummary.SUNDAY : DaySummary.WORKING;
            }
            if (!DaySummary.WORKING.equals(str2)) {
                i2 = i;
            } else {
                if (treeMap2.contains(str)) {
                    break;
                }
                i2 = i + 1;
            }
            str = Biz.plusDays(str, -1);
            i3++;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class TreeMap2 {
        static final TreeMap2 EMPTY = new TreeMap2();
        String lastKey;
        private final TreeMap<String, String[]> map = new TreeMap<>();

        TreeMap2() {
        }

        void put(String str, String[] strArr) {
            this.map.put(str, strArr);
        }

        boolean contains(String str) {
            return this.map.containsKey(str);
        }

        String[] lastUpTo(String str) {
            Map.Entry<String, String[]> floorEntry = this.map.floorEntry(str);
            if (floorEntry == null) {
                return null;
            }
            this.lastKey = floorEntry.getKey();
            return floorEntry.getValue();
        }
    }

    public static DaySummary summary(SQLiteDatabase sQLiteDatabase, String str) {
        DaySummary daySummary = new DaySummary();
        daySummary.date = str;
        String[] dayType = dayType(sQLiteDatabase, str);
        daySummary.dayType = dayType[0];
        daySummary.dayTypeDesc = dayType[1];
        daySummary.officeNote = officeNote(sQLiteDatabase, str);
        daySummary.total = Csp.activeCount(sQLiteDatabase);
        String sumSql = Params.sumSql(sQLiteDatabase, "r", true);
        daySummary.reported = count(sQLiteDatabase, "SELECT COUNT(*) FROM daily_report r JOIN csp c ON c.id = r.csp_id WHERE r.business_date = ? AND c.status = 'ACTIVE'", str);
        daySummary.nilFigure = count(sQLiteDatabase, "SELECT COUNT(*) FROM daily_report r JOIN csp c ON c.id = r.csp_id WHERE r.business_date = ? AND c.status = 'ACTIVE' AND (" + sumSql + ") = 0", str);
        daySummary.figureReported = daySummary.reported - daySummary.nilFigure;
        daySummary.pending = Math.max(0, daySummary.total - daySummary.reported);
        daySummary.remaining = daySummary.pending;
        daySummary.completed = daySummary.reported;
        daySummary.pct = daySummary.total > 0 ? round2((daySummary.reported * 100.0d) / daySummary.total) : 0.0d;
        Iterator<Integer> it = totals(sQLiteDatabase, str).values().iterator();
        while (it.hasNext()) {
            Integer next = it.next();
            daySummary.totalBusiness = (next == null ? 0 : next.intValue()) + daySummary.totalBusiness;
        }
        StringBuilder sb = new StringBuilder();
        for (String str2 : Params.keys(sQLiteDatabase)) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append("COALESCE(SUM(tgt_").append(str2).append("), 0)");
        }
        daySummary.totalTarget = 0;
        if (sb.length() > 0) {
            Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT (" + ((Object) sb) + ") FROM csp WHERE status = '" + Csp.ACTIVE + "'", null);
            try {
                if (rawQuery.moveToFirst() && !rawQuery.isNull(0)) {
                    daySummary.totalTarget = rawQuery.getInt(0);
                }
            } finally {
                rawQuery.close();
            }
        }
        return daySummary;
    }

    private static double round2(double d) {
        return Math.round(d * 100.0d) / 100.0d;
    }

    private static int count(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        int i = 0;
        Cursor rawQuery = sQLiteDatabase.rawQuery(str, new String[]{str2});
        try {
            if (rawQuery.moveToFirst() && !rawQuery.isNull(0)) {
                i = rawQuery.getInt(0);
            }
            return i;
        } finally {
            rawQuery.close();
        }
    }

    public static Map<String, Integer> totals(SQLiteDatabase sQLiteDatabase, String str) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        Iterator<String> it = Params.keys(sQLiteDatabase).iterator();
        while (it.hasNext()) {
            linkedHashMap.put(it.next(), 0);
        }
        if (linkedHashMap.isEmpty()) {
            return linkedHashMap;
        }
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT " + Params.sumEachSql(sQLiteDatabase, "") + " FROM daily_report WHERE business_date = ?", new String[]{str});
        try {
            if (rawQuery.moveToFirst()) {
                Iterator<String> it2 = Params.keys(sQLiteDatabase).iterator();
                int i = 0;
                while (it2.hasNext()) {
                    linkedHashMap.put(it2.next(), Integer.valueOf(rawQuery.isNull(i) ? 0 : rawQuery.getInt(i)));
                    i++;
                }
            }
            return linkedHashMap;
        } finally {
            rawQuery.close();
        }
    }

    public static String saveReport(SQLiteDatabase sQLiteDatabase, long j, String str, Map<String, Integer> map, String str2) {
        String str3;
        String str4;
        String now = Biz.now();
        sQLiteDatabase.delete("pending_reason", "csp_id = ? AND business_date = ?", new String[]{String.valueOf(j), str});
        List<String> keys = Params.keys(sQLiteDatabase);
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT id FROM daily_report WHERE csp_id = ? AND business_date = ?", new String[]{String.valueOf(j), str});
        try {
            if (!rawQuery.moveToFirst()) {
                str3 = null;
            } else {
                str3 = String.valueOf(rawQuery.getLong(0));
            }
            rawQuery.close();
            ContentValues contentValues = new ContentValues();
            for (String str5 : keys) {
                Integer num = map.get(str5);
                contentValues.put(str5, Integer.valueOf(num == null ? 0 : num.intValue()));
            }
            if (str2 == null) {
                str2 = "";
            }
            contentValues.put("remarks", str2);
            contentValues.put("submitted_at", now);
            contentValues.put("updated_at", now);
            if (str3 != null) {
                sQLiteDatabase.update("daily_report", contentValues, "id = ?", new String[]{str3});
                str4 = "updated";
            } else {
                contentValues.put("csp_id", Long.valueOf(j));
                contentValues.put("business_date", str);
                contentValues.put("created_at", now);
                sQLiteDatabase.insert("daily_report", null, contentValues);
                str4 = "inserted";
            }
            if (Settings.hourlyEnabled(sQLiteDatabase)) {
                saveHourly(sQLiteDatabase, j, str, map);
            }
            return str4;
        } catch (Throwable th) {
            rawQuery.close();
            throw th;
        }
    }

    public static void saveHourly(SQLiteDatabase sQLiteDatabase, long j, String str, Map<String, Integer> map) {
        int[] hourlyWindow = Settings.hourlyWindow(sQLiteDatabase);
        int max = Math.max(hourlyWindow[0], Math.min(hourlyWindow[1], Biz.hourOfNow()));
        String now = Biz.now();
        ContentValues contentValues = new ContentValues();
        for (String str2 : Params.keys(sQLiteDatabase)) {
            Integer num = map.get(str2);
            contentValues.put(str2, Integer.valueOf(num == null ? 0 : num.intValue()));
        }
        contentValues.put("updated_at", now);
        if (sQLiteDatabase.update("hourly_report", contentValues, "csp_id = ? AND business_date = ? AND hour = ?", new String[]{String.valueOf(j), str, String.valueOf(max)}) == 0) {
            contentValues.put("csp_id", Long.valueOf(j));
            contentValues.put("business_date", str);
            contentValues.put("hour", Integer.valueOf(max));
            contentValues.put("created_at", now);
            sQLiteDatabase.insert("hourly_report", null, contentValues);
        }
    }

    public static boolean deleteReport(SQLiteDatabase sQLiteDatabase, long j, String str) {
        if (str == null || str.length() < 10 || j <= 0) {
            return false;
        }
        boolean inTransaction = sQLiteDatabase.inTransaction();
        if (!inTransaction) {
            sQLiteDatabase.beginTransaction();
        }
        try {
            sQLiteDatabase.delete("daily_report", "csp_id = ? AND business_date = ?", new String[]{String.valueOf(j), str});
            sQLiteDatabase.delete("hourly_report", "csp_id = ? AND business_date = ?", new String[]{String.valueOf(j), str});
            if (!inTransaction) {
                sQLiteDatabase.setTransactionSuccessful();
            }
            return true;
        } finally {
            if (!inTransaction) {
                sQLiteDatabase.endTransaction();
            }
        }
    }

    public static String saveReason(SQLiteDatabase sQLiteDatabase, long j, String str, String str2) {
        String trim = str2 == null ? "" : str2.trim();
        if (trim.length() > 200) {
            return "error:Reason too long (max 200)";
        }
        String valueOf = String.valueOf(j);
        if (trim.isEmpty()) {
            sQLiteDatabase.delete("pending_reason", "csp_id = ? AND business_date = ?", new String[]{valueOf, str});
            return "cleared";
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("reason", trim);
        contentValues.put("updated_at", Biz.now());
        if (sQLiteDatabase.update("pending_reason", contentValues, "csp_id = ? AND business_date = ?", new String[]{valueOf, str}) > 0) {
            return "updated";
        }
        contentValues.put("csp_id", Long.valueOf(j));
        contentValues.put("business_date", str);
        contentValues.put("created_at", Biz.now());
        sQLiteDatabase.insert("pending_reason", null, contentValues);
        return "inserted";
    }

    public static long logFollowup(SQLiteDatabase sQLiteDatabase, long j, String str, String str2, String str3) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("csp_id", Long.valueOf(j));
        contentValues.put("business_date", str);
        contentValues.put("action_status", str2);
        contentValues.put("note", str3 == null ? "" : str3.trim());
        contentValues.put("created_at", Biz.now());
        long res = sQLiteDatabase.insert("followup_log", null, contentValues);
        return res;
    }

    public static List<Map<String, String>> followupHistory(SQLiteDatabase sQLiteDatabase, long j, int i) {
        return followupHistory(sQLiteDatabase, j, null, i);
    }

    public static List<Map<String, String>> followupHistory(SQLiteDatabase sQLiteDatabase, long j, String date, int i) {
        ArrayList arrayList = new ArrayList();
        String sql;
        String[] args;
        if (date != null && !date.isEmpty()) {
            sql = "SELECT id, csp_id, business_date, action_status, note, created_at FROM followup_log WHERE csp_id = ? AND business_date = ? ORDER BY created_at DESC, id DESC LIMIT " + i;
            args = new String[]{String.valueOf(j), date};
        } else {
            sql = "SELECT id, csp_id, business_date, action_status, note, created_at FROM followup_log WHERE csp_id = ? ORDER BY created_at DESC, id DESC LIMIT " + i;
            args = new String[]{String.valueOf(j)};
        }
        Cursor rawQuery = sQLiteDatabase.rawQuery(sql, args);
        try {
            while (rawQuery.moveToNext()) {
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                linkedHashMap.put("id", rawQuery.getString(0));
                linkedHashMap.put("csp_id", rawQuery.getString(1));
                linkedHashMap.put("business_date", rawQuery.getString(2));
                linkedHashMap.put("action_status", rawQuery.getString(3));
                linkedHashMap.put("note", Csp.nz(rawQuery.getString(4)));
                linkedHashMap.put("created_at", Csp.nz(rawQuery.getString(5)));
                arrayList.add(linkedHashMap);
            }
        } finally {
            rawQuery.close();
        }
        return arrayList;
    }

    public static List<Object[]> reportDates(SQLiteDatabase sQLiteDatabase, int i) {
        ArrayList arrayList = new ArrayList();
        Cursor rawQuery = sQLiteDatabase.rawQuery("SELECT business_date, COUNT(*) AS reported, (" + Params.sumSql(sQLiteDatabase) + ") AS total_business FROM daily_report GROUP BY business_date ORDER BY business_date DESC LIMIT " + i, null);
        while (rawQuery.moveToNext()) {
            try {
                arrayList.add(new Object[]{rawQuery.getString(0), Integer.valueOf(rawQuery.getInt(1)), Integer.valueOf(rawQuery.getInt(2))});
            } finally {
                rawQuery.close();
            }
        }
        return arrayList;
    }

    public static void setDailyTarget(SQLiteDatabase db, long cspId, String date, String fieldKey, int targetValue) {
        ContentValues cv = new ContentValues();
        cv.put("csp_id", Long.valueOf(cspId));
        cv.put("business_date", date);
        cv.put("field_key", fieldKey);
        cv.put("target_value", Integer.valueOf(targetValue));
        cv.put("updated_at", Biz.now());
        if (db.update("daily_csp_target", cv, "csp_id = ? AND business_date = ? AND field_key = ?", new String[]{String.valueOf(cspId), date, fieldKey}) <= 0) {
            cv.put("created_at", Biz.now());
            db.insert("daily_csp_target", null, cv);
        }
    }

    public static void setBulkDailyTargets(SQLiteDatabase db, String date, Map<String, Integer> paramTargets, List<Long> cspIds) {
        db.beginTransaction();
        try {
            for (Long cspId : cspIds) {
                for (Map.Entry<String, Integer> entry : paramTargets.entrySet()) {
                    setDailyTarget(db, cspId.longValue(), date, entry.getKey(), entry.getValue() != null ? entry.getValue().intValue() : 0);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static void clearDailyTargets(SQLiteDatabase db, String date, Long optionalCspId) {
        if (optionalCspId != null) {
            db.delete("daily_csp_target", "business_date = ? AND csp_id = ?", new String[]{date, String.valueOf(optionalCspId)});
        } else {
            db.delete("daily_csp_target", "business_date = ?", new String[]{date});
        }
    }

    public static Map<String, Integer> getDailyTargets(SQLiteDatabase db, long cspId, String date) {
        Map<String, Integer> map = new LinkedHashMap<>();
        Cursor c = db.rawQuery("SELECT field_key, target_value FROM daily_csp_target WHERE csp_id = ? AND business_date = ?", new String[]{String.valueOf(cspId), date});
        try {
            while (c.moveToNext()) {
                map.put(c.getString(0), Integer.valueOf(c.getInt(1)));
            }
        } finally {
            c.close();
        }
        return map;
    }

    public static void triggerPush() {
    }

    public static void triggerPush(android.content.Context context) {
    }

    private static String s(Cursor cursor, int i) {
        String string = cursor.getString(i);
        return string == null ? "" : string;
    }
}
