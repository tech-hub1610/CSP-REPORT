package org.cspdaily.report.domain;

/* loaded from: classes.dex */
public class DaySummary {
    public static final String CLOSURE = "SPECIAL_CLOSURE";
    public static final String HOLIDAY = "HOLIDAY";
    public static final String SUNDAY = "SUNDAY";
    public static final String WORKING = "WORKING";
    public int completed;
    public String date;
    public int figureReported;
    public int nilFigure;
    public double pct;
    public int pending;
    public int remaining;
    public int reported;
    public int total;
    public int totalBusiness;
    public int totalTarget;
    public String dayType = WORKING;
    public String dayTypeDesc = "";
    public String officeNote = "";

    public boolean isWorkingDay() {
        return WORKING.equals(this.dayType);
    }

    public String dayTypeLabel() {
        if (SUNDAY.equals(this.dayType)) {
            return "Sunday";
        }
        if (HOLIDAY.equals(this.dayType)) {
            return "Holiday";
        }
        if (CLOSURE.equals(this.dayType)) {
            return "Special Closure";
        }
        return (this.dayTypeDesc == null || this.dayTypeDesc.isEmpty()) ? "Working Day" : this.dayTypeDesc;
    }

    public boolean isComplete() {
        return this.total > 0 && this.reported >= this.total;
    }

    public int pctInt() {
        return (int) Math.round(this.pct);
    }
}
