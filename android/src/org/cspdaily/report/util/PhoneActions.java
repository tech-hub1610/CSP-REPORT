package org.cspdaily.report.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;
import java.util.Locale;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.ui.widget.Badge;

public final class PhoneActions {
    public static final int REQ_CALL = 43;
    private static String pendingCallMobile;
    private static CspDay pendingCallCsp;

    private PhoneActions() {
    }

    public static void call(Context context, CspDay cspDay) {
        if (Contacts.normalize(cspDay.mobile).isEmpty()) {
            Contacts.call(context, cspDay.mobile);
            return;
        }
        if (!(context instanceof Activity)) {
            Contacts.dial(context, cspDay.mobile);
            return;
        }
        Activity activity = (Activity) context;
        boolean hasCall = activity.checkSelfPermission("android.permission.CALL_PHONE") == 0;
        
        if (hasCall) {
            initiateCall(activity, cspDay);
        } else {
            pendingCallMobile = cspDay.mobile;
            pendingCallCsp = cspDay;
            activity.requestPermissions(new String[]{
                "android.permission.CALL_PHONE"
            }, REQ_CALL);
        }
    }

    private static void initiateCall(final Activity activity, CspDay cspDay) {
        String today = Biz.today();
        try {
            SQLiteDatabase db = Core.get(activity).db();
            Day.logFollowup(db, cspDay.id, today, "Called", "");
        } catch (Throwable ignored) {
        }

        Contacts.call(activity, cspDay.mobile);
    }

    public static void onCallPermission(Activity activity, boolean granted) {
        String mobile = pendingCallMobile;
        CspDay csp = pendingCallCsp;
        pendingCallMobile = null;
        pendingCallCsp = null;
        if (mobile != null) {
            if (granted && csp != null) {
                initiateCall(activity, csp);
            } else {
                Toast.makeText(activity, "Call permission refused — dialling instead", 1).show();
                Contacts.dial(activity, mobile);
            }
        }
    }

    public static void onActivityResumed(final Context context) {
    }

    public static void syncPendingCallsFromDialer(Context context) {
    }

    private static void updateLog(Context context, long logId, String note) {
        if (logId <= 0) return;
        try {
            SQLiteDatabase db = Core.get(context).db();
            ContentValues cv = new ContentValues();
            cv.put("note", note);
            db.update("followup_log", cv, "id = ?", new String[]{String.valueOf(logId)});
        } catch (Throwable ignored) {}
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        if (seconds < 60) {
            return seconds + "s";
        }
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (secs == 0) {
            return mins + "m";
        }
        return mins + "m " + secs + "s";
    }

    public static void sms(Context context, CspDay cspDay) {
        Contacts.sms(context, cspDay.mobile, chase(cspDay.name, cspDay.code, cspDay.effectiveReasonOrPromise(), Biz.today()));
    }

    public static void whatsapp(Context context, CspDay cspDay) {
        String today = Biz.today();
        try {
            SQLiteDatabase db = Core.get(context).db();
            Day.logFollowup(db, cspDay.id, today, "WhatsApp Sent", "");
        } catch (Throwable ignored) {}

        String str = cspDay.effectiveReasonOrPromise();
        if (str != null && str.length() > 0) {
            Contacts.whatsapp(context, cspDay.mobile, remind(context, cspDay));
        } else {
            Contacts.whatsapp(context, cspDay.mobile, chase(cspDay.name, cspDay.code, null, today));
        }
    }

    public static String remind(Context context, CspDay cspDay) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : cspDay.targets.entrySet()) {
            int intValue = entry.getValue() == null ? 0 : entry.getValue().intValue();
            if (intValue > 0) {
                int intValue2 = cspDay.values.containsKey(entry.getKey()) ? cspDay.values.get(entry.getKey()).intValue() : 0;
                if (intValue2 < intValue) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(shortLabel(entry.getKey())).append(": ").append(intValue - intValue2);
                }
            }
        }
        return sb.length() == 0 ? chase(cspDay.name, cspDay.code, cspDay.effectiveReasonOrPromise(), Biz.today()) : "Reminder: Your pending business for " + Biz.cspCodeSuffix(cspDay.code) + " is " + ((Object) sb) + ". Please submit.";
    }

    public static String chase(String str, String str2, String str3, String str4) {
        String str5;
        String lowerCase = str3 == null ? "" : str3.trim().toLowerCase(Locale.US);
        if (lowerCase.length() > 0) {
            if (lowerCase.contains("closed")) {
                str5 = "Please submit business today and report the figures";
            } else {
                str5 = "Please follow up and report the figures";
            }
            return "Business Figure Reminder\n\nCSP " + str + " (" + Biz.cspCodeSuffix(str2) + ") - " + str5 + ".\nReason recorded: " + str3 + "\nDate: " + Biz.fmtDmy(str4) + "\n\nContact Branch Manager";
        }
        return "CSP " + str + " (" + Biz.cspCodeSuffix(str2) + "), Please Report Your Business Figure. Dated:- " + Biz.fmtDmy(str4);
    }

    public static String notePresets() {
        return "By 4 PM, By Evening, Portal Down, Switched Off, Closed Today";
    }

    public static String pendingNote(CspDay cspDay) {
        int i = 0;
        for (Map.Entry<String, Integer> entry : cspDay.targets.entrySet()) {
            i = (entry.getValue() == null ? 0 : entry.getValue().intValue()) > (cspDay.values.containsKey(entry.getKey()) ? cspDay.values.get(entry.getKey()).intValue() : 0) ? i + 1 : i;
        }
        return i <= 0 ? "Please report today's figure" : i + " Pending";
    }

    private static String shortLabel(String str) {
        return shortOverrides(str);
    }

    private static String shortOverrides(String str) {
        char c = 65535;
        switch (str.hashCode()) {
            case -1518020139:
                if (str.equals("inoperative_rekyc")) {
                    c = 1;
                    break;
                }
                break;
            case -1381030494:
                if (str.equals("branch")) {
                    c = '\b';
                    break;
                }
                break;
            case -984575724:
                if (str.equals("pmjjby")) {
                    c = 4;
                    break;
                }
                break;
            case -750778962:
                if (str.equals("qr_code_rekyc")) {
                    c = 3;
                    break;
                }
                break;
            case 3617:
                if (str.equals("qr")) {
                    c = 2;
                    break;
                }
                break;
            case 3786:
                if (str.equals("wa")) {
                    c = '\n';
                    break;
                }
                break;
            case 96810:
                if (str.equals("apy")) {
                    c = 7;
                    break;
                }
                break;
            case 106786658:
                if (str.equals("pmjdy")) {
                    c = 6;
                    break;
                }
                break;
            case 106795245:
                if (str.equals("pmsby")) {
                    c = 5;
                    break;
                }
                break;
            case 108396962:
                if (str.equals("rekyc")) {
                    c = 0;
                    break;
                }
                break;
            case 442070649:
                if (str.equals("whatsapp_banking")) {
                    c = 11;
                    break;
                }
                break;
            case 1412051051:
                if (str.equals("inoperative_rekyc_branch_submit")) {
                    c = '\t';
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return "ReKYC";
            case 2:
            case 3:
                return "QR ReKYC";
            case Badge.INFO /* 4 */:
                return "PMJJBY";
            case Badge.BRAND /* 5 */:
                return "PMSBY";
            case Badge.WHATSAPP /* 6 */:
                return "PMJDY";
            case 7:
                return "APY";
            case '\b':
            case '\t':
                return "Branch Submit";
            case '\n':
            case 11:
                return "WhatsApp";
            default:
                return str;
        }
    }

    public static String columnForChip(String str) {
        char c = 65535;
        switch (str.hashCode()) {
            case -1381030494:
                if (str.equals("branch")) {
                    c = 2;
                    break;
                }
                break;
            case -984575724:
                if (str.equals("pmjjby")) {
                    c = 4;
                    break;
                }
                break;
            case 3617:
                if (str.equals("qr")) {
                    c = 1;
                    break;
                }
                break;
            case 3786:
                if (str.equals("wa")) {
                    c = 3;
                    break;
                }
                break;
            case 96810:
                if (str.equals("apy")) {
                    c = 7;
                    break;
                }
                break;
            case 106786658:
                if (str.equals("pmjdy")) {
                    c = 6;
                    break;
                }
                break;
            case 106795245:
                if (str.equals("pmsby")) {
                    c = 5;
                    break;
                }
                break;
            case 108396962:
                if (str.equals("rekyc")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return "inoperative_rekyc";
            case 1:
                return "qr_code_rekyc";
            case 2:
                return "inoperative_rekyc_branch_submit";
            case 3:
                return "whatsapp_banking";
            case Badge.INFO /* 4 */:
            case Badge.BRAND /* 5 */:
            case Badge.WHATSAPP /* 6 */:
            case 7:
            default:
                return str;
        }
    }
}
