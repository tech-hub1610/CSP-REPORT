package org.cspdaily.report.data;

import android.content.Context;
import android.content.SharedPreferences;
import org.cspdaily.report.domain.Biz;

/* loaded from: classes.dex */
public final class UiState {
    private static final String FILE = "csp_ui";
    private static final String K_CSP_FILTER = "csp_filter";
    private static final String K_CSP_QUERY = "csp_query";
    private static final String K_CSP_SORT = "csp_sort_mode";
    private static final String K_DATE = "view_date";
    private static final String K_DATE_SET_DAY = "view_date_set_day";
    private static final String K_REPORT_COLS = "report_cols";

    private UiState() {
    }

    private static SharedPreferences p(Context context) {
        return context.getSharedPreferences(FILE, 0);
    }

    public static String date(Context context) {
        String today = Biz.today();
        SharedPreferences sp = p(context);
        String string = sp.getString(K_DATE, "");
        String setDay = sp.getString(K_DATE_SET_DAY, "");
        if (string == null || string.length() != 10 || !today.equals(setDay)) {
            setDate(context, today);
            return today;
        }
        return string;
    }

    public static void setDate(Context context, String str) {
        String today = Biz.today();
        SharedPreferences.Editor edit = p(context).edit();
        if (str == null || str.length() != 10) {
            str = today;
        }
        edit.putString(K_DATE, str);
        edit.putString(K_DATE_SET_DAY, today);
        edit.apply();
    }

    public static void resetDate(Context context) {
        setDate(context, Biz.today());
    }

    public static boolean checkDayRollover(Context context) {
        String today = Biz.today();
        SharedPreferences sp = p(context);
        String setDay = sp.getString(K_DATE_SET_DAY, "");
        String string = sp.getString(K_DATE, "");
        if (string == null || string.length() != 10 || !today.equals(setDay)) {
            setDate(context, today);
            return true;
        }
        return false;
    }

    public static boolean isViewingToday(Context context) {
        return Biz.today().equals(date(context));
    }

    public static String cspFilter(Context context) {
        return p(context).getString(K_CSP_FILTER, "all");
    }

    public static void setCspFilter(Context context, String str) {
        p(context).edit().putString(K_CSP_FILTER, str).apply();
    }

    public static String cspSort(Context context) {
        return p(context).getString(K_CSP_SORT, "name");
    }

    public static void setCspSort(Context context, String str) {
        p(context).edit().putString(K_CSP_SORT, str).apply();
    }

    public static String cspQuery(Context context) {
        return p(context).getString(K_CSP_QUERY, "");
    }

    public static void setCspQuery(Context context, String str) {
        SharedPreferences.Editor edit = p(context).edit();
        if (str == null) {
            str = "";
        }
        edit.putString(K_CSP_QUERY, str).apply();
    }

    public static String reportCols(Context context) {
        return p(context).getString(K_REPORT_COLS, "");
    }

    public static void setReportCols(Context context, String str) {
        SharedPreferences.Editor edit = p(context).edit();
        if (str == null) {
            str = "";
        }
        edit.putString(K_REPORT_COLS, str).apply();
    }
}
