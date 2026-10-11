package org.cspdaily.report.notify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.MainActivity;

/* loaded from: classes.dex */
public final class Notifier {
    public static final String EXTRA_CSP = "csp.id";
    public static final String EXTRA_DATE = "csp.date";
    public static final String EXTRA_FOLLOWUP = "csp.followup";
    public static final String EXTRA_TAB = "csp.tab";
    public static final int ID_COMPLETE = 103;
    public static final int ID_DAILY = 100;
    public static final int ID_FOLLOWUP = 101;
    public static final int ID_LONG_PENDING = 102;
    public static final int ID_HOURLY = 104;

    private Notifier() {
    }

    public static boolean permitted(Context context) {
        return Build.VERSION.SDK_INT < 33 || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == 0;
    }

    public static void post(Context context, String str, int i, String str2, String str3, Intent intent) {
        postBig(context, str, i, str2, str3, str3, intent);
    }

    public static void postBig(Context context, String str, int i, String str2, String str3, String bigText, Intent intent) {
        NotificationManager notificationManager;
        Notification.Builder builder;
        if (permitted(context) && (notificationManager = (NotificationManager) context.getSystemService("notification")) != null) {
            int i2 = 134217728;
            if (Build.VERSION.SDK_INT >= 23) {
                i2 = 201326592;
            }
            PendingIntent activity = intent == null ? null : PendingIntent.getActivity(context, i, intent, i2);
            if (Build.VERSION.SDK_INT >= 26) {
                builder = new Notification.Builder(context, str);
            } else {
                builder = new Notification.Builder(context);
            }
            builder.setSmallIcon(R.drawable.ic_bell).setContentTitle(str2).setContentText(str3).setStyle(new Notification.BigTextStyle().bigText(bigText != null ? bigText : str3)).setAutoCancel(true).setWhen(System.currentTimeMillis()).setColor(context.getResources().getColor(R.color.primary_600));
            if (activity != null) {
                builder.setContentIntent(activity);
            }
            notificationManager.notify(i, builder.build());
        }
    }

    public static void cancel(Context context, int i) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        if (notificationManager != null) {
            notificationManager.cancel(i);
        }
    }

    public static Intent tapIntent(Context context, int i, String str, long j, boolean z) {
        Intent intent = new Intent(context, (Class<?>) MainActivity.class);
        intent.setFlags(335544320);
        intent.putExtra(EXTRA_TAB, i);
        if (str != null) {
            intent.putExtra(EXTRA_DATE, str);
        }
        if (j > 0) {
            intent.putExtra(EXTRA_CSP, j);
        }
        intent.putExtra(EXTRA_FOLLOWUP, z);
        return intent;
    }
}
