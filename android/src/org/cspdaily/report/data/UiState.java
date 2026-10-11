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
    private static final String K_REPORT_COLS = "report_cols";

    private UiState() {
    }

    private static SharedPreferences p(Context context) {
        return context.getSharedPreferences(FILE, 0);
    }

    public static String date(Context context) {
        String string = p(context).getString(K_DATE, "");
        if (string == null || string.length() != 10) {
            return Biz.today();
        }
        return string;
    }

    public static void setDate(Context context, String str) {
        SharedPreferences.Editor edit = p(context).edit();
        if (str == null) {
            str = Biz.today();
        }
        edit.putString(K_DATE, str).apply();
    }

    public static void resetDate(Context context) {
        p(context).edit().remove(K_DATE).apply();
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
