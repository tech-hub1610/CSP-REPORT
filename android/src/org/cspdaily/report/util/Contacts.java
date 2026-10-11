package org.cspdaily.report.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;
import java.util.List;
import org.cspdaily.report.domain.Biz;

/* loaded from: classes.dex */
public final class Contacts {
    private static final String WA_PACKAGE = "com.whatsapp";
    private static final String WA_PACKAGE_BUSINESS = "com.whatsapp.w4b";

    private Contacts() {
    }

    public static String normalize(String str) {
        return Biz.phoneDigits(str);
    }

    public static String dialForm(String str) {
        String normalize = normalize(str);
        if (normalize.length() == 12 && normalize.startsWith("91")) {
            return normalize.substring(2);
        }
        return !normalize.isEmpty() ? normalize : str;
    }

    public static String e164(String str) {
        return Biz.phoneE164(str);
    }

    public static void dial(Context context, String str) {
        if (normalize(str).isEmpty()) {
            Toast.makeText(context, "No mobile number recorded for this CSP", 0).show();
        } else {
            start(context, new Intent("android.intent.action.DIAL", Uri.fromParts("tel", e164(str), null)), "No phone dialler available");
        }
    }

    public static void call(Context context, String str) {
        if (normalize(str).isEmpty()) {
            Toast.makeText(context, "No mobile number recorded for this CSP", 0).show();
            return;
        }
        try {
            context.startActivity(new Intent("android.intent.action.CALL", Uri.fromParts("tel", e164(str), null)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No phone dialler available", 1).show();
        } catch (SecurityException e2) {
            dial(context, str);
        }
    }

    public static void sms(Context context, String str, String str2) {
        if (normalize(str).isEmpty()) {
            Toast.makeText(context, "No mobile number recorded for this CSP", 0).show();
            return;
        }
        Intent intent = new Intent("android.intent.action.SENDTO", Uri.fromParts("smsto", e164(str), null));
        if (str2 != null) {
            intent.putExtra("sms_body", str2);
        }
        start(context, intent, "No messaging app available");
    }

    public static void whatsapp(Context context, String str, String str2) {
        String normalize = normalize(str);
        if (normalize.isEmpty()) {
            Toast.makeText(context, "No mobile number recorded for this CSP", 0).show();
            return;
        }

        String msgParam = (str2 != null && str2.length() > 0) ? "&text=" + Uri.encode(str2) : "";
        String uriStr = "whatsapp://send?phone=" + normalize + msgParam;

        // 1. Try explicit package intent if WhatsApp or WhatsApp Business is found
        String whatsappPackage = whatsappPackage(context);
        if (whatsappPackage != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
                intent.setPackage(whatsappPackage);
                context.startActivity(intent);
                return;
            } catch (Throwable ignored) {
            }
        }

        // 2. Try generic whatsapp:// scheme (matches any installed WhatsApp client / mod / clone)
        try {
            Intent waIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
            context.startActivity(waIntent);
            return;
        } catch (Throwable ignored) {
        }

        // 3. Fallback to SMS if no WhatsApp app can handle the request
        sms(context, normalize, str2);
        Toast.makeText(context, "Could not launch WhatsApp — opened SMS instead", 1).show();
    }

    private static String whatsappPackage(Context context) {
        if (resolvable(context, WA_PACKAGE)) {
            return WA_PACKAGE;
        }
        if (resolvable(context, WA_PACKAGE_BUSINESS)) {
            return WA_PACKAGE_BUSINESS;
        }
        return null;
    }

    private static boolean resolvable(Context context, String str) {
        try {
            return context.getPackageManager().getPackageInfo(str, 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasWhatsapp(Context context) {
        return whatsappPackage(context) != null;
    }

    private static void start(Context context, Intent intent, String str) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, str, 1).show();
        } catch (Exception e2) {
            Toast.makeText(context, str, 1).show();
        }
    }

    public static void shareText(Context context, String str, String str2) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.TEXT", str2);
        if (str != null && str.length() > 0) {
            intent.putExtra("android.intent.extra.SUBJECT", str);
        }
        if (str == null) {
            str = "Share";
        }
        try {
            context.startActivity(Intent.createChooser(intent, str));
        } catch (Exception e) {
            Toast.makeText(context, "Nothing available to share to", 1).show();
        }
    }

    public static List<ResolveInfo> pick(Context context, Intent intent) {
        return context.getPackageManager().queryIntentActivities(intent, 0);
    }
}
