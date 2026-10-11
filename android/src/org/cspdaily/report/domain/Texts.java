package org.cspdaily.report.domain;

import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Params;

/* loaded from: classes.dex */
public final class Texts {
    private Texts() {
    }

    public static String officeText(SQLiteDatabase sQLiteDatabase, String str) {
        Map<String, Integer> map = Day.totals(sQLiteDatabase, str);
        StringBuilder sb = new StringBuilder();
        sb.append("Business Figure Format\n\n");
        sb.append("Dated: ").append(Biz.fmtDmy(str)).append("\n\n");
        boolean z = true;
        for (Map.Entry<String, String> entry : Params.labels(sQLiteDatabase).entrySet()) {
            Integer num = map.get(entry.getKey());
            sb.append(entry.getValue()).append(z ? " : " : ": ").append(num == null ? 0 : num.intValue()).append("\n");
            z = false;
        }
        Iterator<String> it = notReportedLines(sQLiteDatabase, str).iterator();
        while (it.hasNext()) {
            sb.append(it.next()).append("\n");
        }
        Iterator<String> it2 = remarkLines(sQLiteDatabase, str).iterator();
        while (it2.hasNext()) {
            sb.append(it2.next()).append("\n");
        }
        return trimTail(sb.toString());
    }

    public static List<String> notReportedLines(SQLiteDatabase sQLiteDatabase, String str) {
        String str2;
        ArrayList arrayList = new ArrayList();
        List<CspDay> dayData = Day.dayData(sQLiteDatabase, str);
        ArrayList<CspDay> arrayList2 = new ArrayList();
        for (CspDay cspDay : dayData) {
            if (cspDay.isPending() || cspDay.isNil) {
                arrayList2.add(cspDay);
            }
        }
        if (arrayList2.isEmpty()) {
            return arrayList;
        }
        arrayList.add("");
        arrayList.add("Not Reported (" + arrayList2.size() + "):");
        for (CspDay cspDay2 : arrayList2) {
            String cspCodeSuffix = Biz.cspCodeSuffix(cspDay2.code);
            String reason = cspDay2.effectiveReasonOrPromise();
            if (reason != null && !reason.isEmpty()) {
                str2 = cspCodeSuffix + " - " + reason;
            } else {
                str2 = cspDay2.isNil ? cspCodeSuffix + " - Nil Figure" : cspCodeSuffix;
            }
            arrayList.add(str2);
        }
        return arrayList;
    }

    public static List<String> remarkLines(SQLiteDatabase sQLiteDatabase, String str) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (CspDay cspDay : Day.dayData(sQLiteDatabase, str)) {
            if (cspDay.reported && cspDay.remarks != null && !cspDay.remarks.trim().isEmpty()) {
                arrayList2.add(Biz.cspCodeSuffix(cspDay.code) + " - " + cspDay.remarks.trim());
            }
        }
        if (arrayList2.isEmpty()) {
            return arrayList;
        }
        arrayList.add("");
        arrayList.add("Remarks:");
        arrayList.addAll(arrayList2);
        return arrayList;
    }

    public static String pendingList(SQLiteDatabase sQLiteDatabase, String str) {
        List<CspDay> dayData = Day.dayData(sQLiteDatabase, str);
        ArrayList arrayList = new ArrayList();
        for (CspDay cspDay : dayData) {
            if (cspDay.isPending() && Csp.ACTIVE.equals(cspDay.status)) {
                arrayList.add(cspDay);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CSP DAILY FOLLOW-UP\n");
        sb.append("Date: ").append(Biz.fmtDmy(str)).append("\n\n");
        sb.append("Pending CSPs: ").append(arrayList.size()).append("\n\n");
        if (arrayList.isEmpty()) {
            sb.append("All CSPs have reported today!\n");
        } else {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                CspDay cspDay2 = (CspDay) it.next();
                String trim = cspDay2.effectiveReasonOrPromise();
                if (trim.isEmpty()) {
                    trim = (cspDay2.followupStatus == null || Day.NOT_CONTACTED.equals(cspDay2.followupStatus)) ? "Pending" : cspDay2.followupStatus;
                }
                sb.append("CSP ").append(Biz.cspCodeSuffix(cspDay2.code)).append(" - ").append(trim).append("\n");
            }
        }
        String officeNote = Day.officeNote(sQLiteDatabase, str);
        if (!officeNote.isEmpty()) {
            sb.append("\nOffice Note: ").append(officeNote).append("\n");
        }
        return trimTail(sb.toString());
    }

    public static String cspReport(SQLiteDatabase sQLiteDatabase, long j, String str) {
        CspDay cspDay;
        Iterator<CspDay> it = Day.dayData(sQLiteDatabase, str, false).iterator();
        while (true) {
            if (!it.hasNext()) {
                cspDay = null;
                break;
            }
            CspDay next = it.next();
            if (next.id == j) {
                cspDay = next;
                break;
            }
        }
        if (cspDay == null || !cspDay.reported) {
            return null;
        }
        Map<String, String> labels = Params.labels(sQLiteDatabase);
        StringBuilder sb = new StringBuilder();
        sb.append("CSP Daily Report\n");
        sb.append(cspDay.name).append(" (").append(cspDay.code).append(")\n");
        sb.append(cspDay.branch).append(" (").append(cspDay.branchCode).append(")\n");
        sb.append("Dated: ").append(Biz.fmtDmy(str)).append("\n\n");
        boolean z = true;
        Iterator<Map.Entry<String, String>> it2 = labels.entrySet().iterator();
        while (true) {
            boolean z2 = z;
            if (!it2.hasNext()) {
                break;
            }
            Map.Entry<String, String> next2 = it2.next();
            Integer num = cspDay.values.get(next2.getKey());
            int intValue = num == null ? 0 : num.intValue();
            if (intValue > 0) {
                sb.append(next2.getValue()).append(z2 ? " : " : ": ").append(intValue).append("\n");
                z = false;
            } else {
                z = z2;
            }
        }
        if (cspDay.totalBusiness == 0) {
            sb.append("Nil Figure\n");
        }
        if (cspDay.remarks != null && !cspDay.remarks.trim().isEmpty()) {
            sb.append("\nRemarks: ").append(cspDay.remarks.trim()).append("\n");
        }
        return trimTail(sb.toString());
    }

    public static String formatSelfTargetLine(String cspCode, Map<String, Integer> targets, List<Params.P> params) {
        String idSuffix = Biz.cspCodeSuffix(cspCode);
        StringBuilder sb = new StringBuilder();
        int lineTotal = 0;
        if (params != null && targets != null) {
            for (Params.P p : params) {
                Integer val = targets.get(p.key);
                int intVal = val == null ? 0 : val.intValue();
                if (intVal > 0) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    String pLabel = (p.shortLabel != null && !p.shortLabel.trim().isEmpty()) ? p.shortLabel.trim() : p.label;
                    sb.append(pLabel).append(": ").append(intVal);
                    lineTotal += intVal;
                }
            }
        }
        if (sb.length() == 0) {
            return idSuffix + " - Nil";
        }
        return idSuffix + " - " + sb.toString() + " (Total: " + lineTotal + ")";
    }

    public static String selfTargetText(String cspCode, Map<String, Integer> targets, List<Params.P> params) {
        return selfTargetText(cspCode, targets, params, null);
    }

    public static String selfTargetText(String cspCode, Map<String, Integer> targets, List<Params.P> params, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Self-Target by CSPs - \n");
        if (date != null && !date.isEmpty()) {
            sb.append("Dated: ").append(Biz.fmtDmy(date)).append("\n");
        }
        sb.append("ID \n");
        sb.append(formatSelfTargetLine(cspCode, targets, params));
        return sb.toString();
    }

    public static String selfTargetText(CspDay csp, List<Params.P> params) {
        return selfTargetText(csp, params, null);
    }

    public static String selfTargetText(CspDay csp, List<Params.P> params, String date) {
        if (csp == null) return "Self-Target by CSPs - \nID \n";
        return selfTargetText(csp.code, csp.targets, params, date);
    }

    public static String selfTargetList(SQLiteDatabase db, String date) {
        List<CspDay> dayData = Day.dayData(db, date, true);
        List<Params.P> params = Params.all(db);
        return selfTargetList(dayData, params, date);
    }

    public static String selfTargetList(List<CspDay> cspList, List<Params.P> params) {
        return selfTargetList(cspList, params, null);
    }

    public static String selfTargetList(List<CspDay> cspList, List<Params.P> params, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Self-Target by CSPs - \n");
        if (date != null && !date.isEmpty()) {
            sb.append("Dated: ").append(Biz.fmtDmy(date)).append("\n");
        }
        sb.append("ID \n");
        int count = 0;
        int grandTotal = 0;
        Map<String, Integer> paramTotals = new LinkedHashMap<>();
        if (params != null) {
            for (Params.P p : params) {
                paramTotals.put(p.key, 0);
            }
        }

        if (cspList != null) {
            for (CspDay csp : cspList) {
                if (csp.dailyTarget > 0) {
                    sb.append(formatSelfTargetLine(csp.code, csp.targets, params)).append("\n");
                    count++;
                    grandTotal += csp.dailyTarget;
                    if (csp.targets != null) {
                        for (Map.Entry<String, Integer> entry : csp.targets.entrySet()) {
                            if (entry.getValue() != null && entry.getValue().intValue() > 0) {
                                int current = paramTotals.containsKey(entry.getKey()) ? paramTotals.get(entry.getKey()).intValue() : 0;
                                paramTotals.put(entry.getKey(), current + entry.getValue().intValue());
                            }
                        }
                    }
                }
            }
        }

        if (count == 0) {
            sb.append("No self-targets set for this day.\n");
        } else {
            sb.append("\nSubtotal -\n");
            if (params != null) {
                for (Params.P p : params) {
                    Integer val = paramTotals.get(p.key);
                    int intVal = val == null ? 0 : val.intValue();
                    if (intVal > 0) {
                        String pLabel = (p.shortLabel != null && !p.shortLabel.trim().isEmpty()) ? p.shortLabel.trim() : p.label;
                        sb.append(pLabel).append(": ").append(intVal).append("\n");
                    }
                }
            }
            sb.append("Total: ").append(grandTotal).append("\n");
        }
        return trimTail(sb.toString());
    }

    public static String selfTargetCsv(SQLiteDatabase sQLiteDatabase, String str) {
        List<CspDay> dayData = Day.dayData(sQLiteDatabase, str, true);
        List<Params.P> params = Params.all(sQLiteDatabase);
        StringBuilder sb = new StringBuilder();
        String[] strArr = {"SL", "Business Date", "CSP Code", "CSP Name", "Branch", "Branch Code"};
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(Biz.csv(strArr[i]));
        }
        for (Params.P p : params) {
            sb.append(",").append(Biz.csv(p.label));
        }
        sb.append(",Total Target\n");
        int sl = 1;
        for (CspDay cspDay : dayData) {
            sb.append(sl).append(",").append(Biz.csv(str)).append(",").append(Biz.csv(cspDay.code)).append(",").append(Biz.csv(cspDay.name)).append(",").append(Biz.csv(cspDay.branch)).append(",").append(Biz.csv(cspDay.branchCode));
            for (Params.P p2 : params) {
                Integer v = cspDay.targets.get(p2.key);
                sb.append(",").append(v == null ? 0 : v.intValue());
            }
            sb.append(",").append(cspDay.dailyTarget).append("\n");
            sl++;
        }
        return sb.toString();
    }

    public static String dayCsv(SQLiteDatabase sQLiteDatabase, String str) {
        String str2;
        int i = 1;
        List<CspDay> dayData = Day.dayData(sQLiteDatabase, str);
        Map<String, String> labels = Params.labels(sQLiteDatabase);
        StringBuilder sb = new StringBuilder();
        String[] strArr = {"SL", "Business Date", "CSP Code", "CSP Name", "Branch", "Branch Code", "Status"};
        for (int i2 = 0; i2 < strArr.length; i2++) {
            if (i2 > 0) {
                sb.append(",");
            }
            sb.append(Biz.csv(strArr[i2]));
        }
        Iterator<String> it = labels.values().iterator();
        while (it.hasNext()) {
            sb.append(",").append(Biz.csv(it.next()));
        }
        sb.append(",Total Business,Remarks,Pending Reason,Follow-up Status\n");
        for (CspDay cspDay : dayData) {
            StringBuilder sb2 = new StringBuilder();
            int i3 = i + 1;
            StringBuilder append = sb2.append(i).append(",").append(Biz.csv(cspDay.reported ? str : "")).append(",").append(Biz.csv(cspDay.code)).append(",").append(Biz.csv(cspDay.name)).append(",").append(Biz.csv(cspDay.branch)).append(",").append(Biz.csv(cspDay.branchCode)).append(",");
            if (cspDay.reported) {
                str2 = cspDay.isNil ? "NIL FIGURE" : "REPORTED";
            } else {
                str2 = "PENDING";
            }
            append.append(str2);
            Iterator<Map.Entry<String, String>> it2 = labels.entrySet().iterator();
            while (it2.hasNext()) {
                Integer num = cspDay.values.get(it2.next().getKey());
                sb2.append(",").append((!cspDay.reported || num == null) ? 0 : num.intValue());
            }
            sb2.append(",").append(cspDay.totalBusiness).append(",").append(Biz.csv(cspDay.remarks)).append(",").append(Biz.csv(cspDay.effectiveReasonOrPromise())).append(",").append(Biz.csv(cspDay.followupStatus)).append("\n");
            sb.append((CharSequence) sb2);
            i = i3;
        }
        return sb.toString();
    }

    public static String monthCsv(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        String[] strArr;
        List<Map<String, String>> month;
        Map<String, String> labels = Params.labels(sQLiteDatabase);
        StringBuilder sb = new StringBuilder();
        if ("csp".equals(str2)) {
            strArr = new String[]{"SL", "CSP Code", "CSP Name", "Branch", "Branch Code", "Days Reported"};
            month = Agg.cspMonth(sQLiteDatabase, str);
        } else if ("branch".equals(str2)) {
            strArr = new String[]{"SL", "Branch Name", "Branch Codes", "CSPs", "Reports"};
            month = Agg.branchMonth(sQLiteDatabase, str);
        } else {
            strArr = new String[]{"Date", "Reported CSPs"};
            month = Agg.month(sQLiteDatabase, str);
        }
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(Biz.csv(strArr[i]));
        }
        Iterator<String> it = labels.values().iterator();
        while (it.hasNext()) {
            sb.append(",").append(Biz.csv(it.next()));
        }
        sb.append(",Total Business\n");
        int i2 = 1;
        for (Map<String, String> map : month) {
            StringBuilder sb2 = new StringBuilder();
            if ("day".equals(str2)) {
                sb2.append(Biz.csv(map.get("business_date"))).append(",").append(nz(map.get("reported")));
            } else if ("csp".equals(str2)) {
                sb2.append(i2).append(",").append(Biz.csv(map.get("csp_code"))).append(",").append(Biz.csv(map.get("csp_name"))).append(",").append(Biz.csv(map.get("branch_name"))).append(",").append(Biz.csv(map.get("branch_code"))).append(",").append(nz(map.get("days")));
                i2++;
            } else {
                sb2.append(i2).append(",").append(Biz.csv(map.get("branch_name"))).append(",").append(Biz.csv(map.get("branch_codes"))).append(",").append(nz(map.get("csps"))).append(",").append(nz(map.get("reports")));
                i2++;
            }
            Iterator<String> it2 = labels.keySet().iterator();
            while (it2.hasNext()) {
                sb2.append(",").append(nz(map.get(it2.next())));
            }
            sb2.append(",").append(nz(map.get("total_business"))).append("\n");
            sb.append((CharSequence) sb2);
        }
        return sb.toString();
    }

    private static int nz(String str) {
        if (str == null) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String csvRow(String[] strArr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(Biz.csv(strArr[i]));
        }
        return sb.toString();
    }

    private static String trimTail(String str) {
        while (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }
}
