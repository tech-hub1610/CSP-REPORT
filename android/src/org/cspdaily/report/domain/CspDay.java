package org.cspdaily.report.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import org.cspdaily.report.data.Day;

/* loaded from: classes.dex */
public class CspDay {
    public int dailyTarget;
    public String followupTime;
    public boolean hasLastReport;
    public long id;
    public boolean isNil;
    public String lastContactedAt;
    public String lastContactedStatus;
    public String lastReportDate;
    public String lastReportSubmittedAt;
    public int lastReportTotal;
    public int pendingDays;
    public String pendingReason;
    public boolean reported;
    public String submittedAt;
    public int totalBusiness;
    public Integer yesterdayTotal;
    public String code = "";
    public String name = "";
    public String gender = "";
    public String mobile = "";
    public String branch = "";
    public String branchCode = "";
    public String status = "";
    public String notes = "";
    public Map<String, Integer> targets = new LinkedHashMap();
    public Map<String, Integer> values = new LinkedHashMap();
    public String remarks = "";
    public String followupStatus = Day.NOT_CONTACTED;
    public String followupNote = "";
    public Map<String, Integer> yesterdayValues = new LinkedHashMap();

    public String suffix() {
        return Biz.cspCodeSuffix(this.code);
    }

    public boolean isPending() {
        return !this.reported;
    }

    public int targetPct() {
        if (this.dailyTarget <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) Math.round((this.totalBusiness * 100.0d) / this.dailyTarget)));
    }

    public Integer deltaVsYesterday() {
        if (this.yesterdayTotal == null || !this.reported) {
            return null;
        }
        return Integer.valueOf(this.totalBusiness - this.yesterdayTotal.intValue());
    }

    public String statusWord() {
        if (this.reported) {
            return this.isNil ? "Nil Figure" : "Reported";
        }
        return "Pending";
    }

    public String effectiveReasonOrPromise() {
        if (this.pendingReason != null && !this.pendingReason.trim().isEmpty()) {
            return this.pendingReason.trim();
        }
        if (this.followupNote != null && !this.followupNote.trim().isEmpty()) {
            return this.followupNote.trim();
        }
        return "";
    }
}
