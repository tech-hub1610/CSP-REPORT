package org.cspdaily.report.notify;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.DaySummary;
import org.cspdaily.report.ui.MainActivity;
import org.cspdaily.report.ui.screen.CspProfileScreen;
import org.cspdaily.report.ui.screen.FollowupScreen;

/* loaded from: classes.dex */
public final class Reminders {
    public static final String ACTION_DAILY = "org.cspdaily.report.DAILY_REMINDER";
    public static final String ACTION_FOLLOWUP = "org.cspdaily.report.FOLLOWUP_REMINDER";
    public static final String ACTION_LONG = "org.cspdaily.report.LONG_PENDING";
    public static final String ACTION_HOURLY = "org.cspdaily.report.HOURLY_TARGET_REMINDER";
    public static final int REQ_DAILY = 200;
    public static final int REQ_FOLLOWUP = 201;
    public static final int REQ_LONG = 202;
    public static final int REQ_HOURLY_BASE = 300;

    private Reminders() {
    }

    public static void schedule(Context context) {
        SQLiteDatabase db = Core.get(context).db();
        cancel(context);
        int[] timeOfDay = Settings.timeOfDay(db, Settings.NOTIF_PENDING_TIME, "16:00");
        int[] timeOfDay2 = Settings.timeOfDay(db, Settings.NOTIF_FOLLOWUP_TIME, "18:00");
        if (Settings.flag(db, Settings.NOTIF_PENDING, true)) {
            arm(context, REQ_DAILY, ACTION_DAILY, timeOfDay[0], timeOfDay[1]);
        }
        if (Settings.flag(db, Settings.NOTIF_FOLLOWUP, true)) {
            arm(context, REQ_FOLLOWUP, ACTION_FOLLOWUP, timeOfDay2[0], timeOfDay2[1]);
        }
        if (Settings.flag(db, Settings.NOTIF_LONG_PENDING, true)) {
            arm(context, REQ_LONG, ACTION_LONG, timeOfDay2[0], timeOfDay2[1]);
        }

        // Hourly reporting reminders
        if (Settings.flag(db, Settings.HOURLY_ENABLED, false)) {
            int startH = Biz.parseHourSetting(Settings.get(db, Settings.HOURLY_START, "10:00"), 10);
            int endH = Biz.parseHourSetting(Settings.get(db, Settings.HOURLY_END, "18:00"), 18);
            if (endH < startH) {
                int temp = startH;
                startH = endH;
                endH = temp;
            }
            for (int h = startH; h <= endH; h++) {
                arm(context, REQ_HOURLY_BASE + h, ACTION_HOURLY, h, 0);
            }
        }
    }

    public static void cancel(Context context) {
        AlarmManager am = am(context);
        if (am != null) {
            for (int i : new int[]{REQ_DAILY, REQ_FOLLOWUP, REQ_LONG}) {
                am.cancel(PendingIntent.getBroadcast(context, i, new Intent(context, (Class<?>) ReminderReceiver.class), flags()));
            }
            for (int h = 0; h < 24; h++) {
                am.cancel(PendingIntent.getBroadcast(context, REQ_HOURLY_BASE + h, new Intent(context, (Class<?>) ReminderReceiver.class), flags()));
            }
        }
    }

    private static void arm(Context context, int i, String str, int i2, int i3) {
        AlarmManager am = am(context);
        if (am != null) {
            PendingIntent broadcast = PendingIntent.getBroadcast(context, i, new Intent(context, (Class<?>) ReminderReceiver.class).setAction(str), flags());
            Calendar calendar = Calendar.getInstance(Biz.TZ);
            calendar.set(11, i2);
            calendar.set(12, i3);
            calendar.set(13, 0);
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(5, 1);
            }
            am.setInexactRepeating(0, calendar.getTimeInMillis(), 86400000L, broadcast);
        }
    }

    private static int flags() {
        if (Build.VERSION.SDK_INT < 23) {
            return 134217728;
        }
        return 201326592;
    }

    private static AlarmManager am(Context context) {
        return (AlarmManager) context.getSystemService("alarm");
    }

    public static void fire(Context context, String str) {
        SQLiteDatabase db = Core.get(context).db();
        if (Settings.flag(db, Settings.NOTIF_ENABLED, true)) {
            String str2 = Biz.today();
            DaySummary summary = Day.summary(db, str2);

            if (ACTION_HOURLY.equals(str)) {
                if (Settings.flag(db, Settings.HOURLY_ENABLED, false) && summary.isWorkingDay() && summary.total > 0 && summary.pending > 0) {
                    List<CspDay> cspDays = Day.dayData(db, str2, true);
                    List<org.cspdaily.report.data.Params.P> params = org.cspdaily.report.data.Params.all(db);

                    StringBuilder paramSummary = new StringBuilder();
                    StringBuilder bigTextBuilder = new StringBuilder();

                    bigTextBuilder.append("Reports: ").append(summary.reported).append("/").append(summary.total)
                                  .append(" filed (").append(summary.pending).append(" pending)\n\nParameter Targets:");

                    int targetCount = 0;
                    for (org.cspdaily.report.data.Params.P p : params) {
                        int tgtSum = 0;
                        int achSum = 0;
                        for (CspDay c : cspDays) {
                            if (c.targets != null && c.targets.containsKey(p.key)) {
                                tgtSum += c.targets.get(p.key).intValue();
                            }
                            if (c.values != null && c.values.containsKey(p.key)) {
                                achSum += c.values.get(p.key).intValue();
                            }
                        }
                        if (tgtSum > 0) {
                            targetCount++;
                            int pendingTgt = Math.max(0, tgtSum - achSum);
                            if (paramSummary.length() > 0) {
                                paramSummary.append(" · ");
                            }
                            paramSummary.append(p.shortLabel).append(": ").append(achSum).append("/").append(tgtSum);

                            bigTextBuilder.append("\n• ").append(p.label).append(": ")
                                          .append(achSum).append("/").append(tgtSum)
                                          .append(" (").append(pendingTgt).append(" pending)");
                        }
                    }

                    bigTextBuilder.append("\n\nTap to follow up with pending CSPs and take reports.");

                    String contentText;
                    if (targetCount > 0) {
                        contentText = paramSummary.toString() + " (" + summary.pending + " CSPs pending)";
                    } else {
                        contentText = summary.pending + " CSPs pending report (" + summary.reported + "/" + summary.total + " done). Tap to follow up.";
                    }

                    String title = "Hourly Follow-up & Targets (" + Biz.clockOf(Biz.now()) + ")";

                    Notifier.postBig(
                        context,
                        Core.CH_REMINDER,
                        Notifier.ID_HOURLY,
                        title,
                        contentText,
                        bigTextBuilder.toString(),
                        Notifier.tapIntent(context, 0, str2, 0L, true)
                    );
                }
                return;
            }

            if (ACTION_DAILY.equals(str)) {
                if (Settings.flag(db, Settings.NOTIF_PENDING, true) && summary.isWorkingDay() && summary.total != 0 && summary.pending > 0 && !str2.equals(Settings.get(db, Settings.LAST_DAILY_NOTICE, ""))) {
                    Settings.set(db, Settings.LAST_DAILY_NOTICE, str2);
                    Notifier.post(context, Core.CH_REMINDER, 100, "Daily CSP Report Reminder", plural(summary.pending, "CSP") + " yet to report for today (" + summary.reported + "/" + summary.total + " done).", Notifier.tapIntent(context, 0, str2, 0L, false));
                    return;
                }
                return;
            }
            if (ACTION_FOLLOWUP.equals(str)) {
                if (Settings.flag(db, Settings.NOTIF_FOLLOWUP, true) && summary.pending > 0 && !str2.equals(Settings.get(db, Settings.LAST_FOLLOWUP_NOTICE, ""))) {
                    Settings.set(db, Settings.LAST_FOLLOWUP_NOTICE, str2);
                    Notifier.post(context, Core.CH_FOLLOWUP, Notifier.ID_FOLLOWUP, "Follow-up Reminder", plural(summary.pending, "CSP") + " still pending — time to follow up.", Notifier.tapIntent(context, 0, str2, 0L, true));
                    return;
                }
                return;
            }
            if (ACTION_LONG.equals(str) && Settings.flag(db, Settings.NOTIF_LONG_PENDING, true)) {
                int longPendingThreshold = Settings.longPendingThreshold(db);
                int i = 0;
                for (CspDay cspDay : Day.dayData(db, str2)) {
                    if (cspDay.isPending() && cspDay.pendingDays >= longPendingThreshold) {
                        i++;
                    }
                    i = i;
                }
                if (i != 0 && !str2.equals(Settings.get(db, Settings.LAST_LONG_PENDING_NOTICE, ""))) {
                    Settings.set(db, Settings.LAST_LONG_PENDING_NOTICE, str2);
                    Notifier.post(context, Core.CH_ALERT, Notifier.ID_LONG_PENDING, "Long Pending CSP Alert", plural(i, "CSP") + " pending for " + longPendingThreshold + " or more working days.", Notifier.tapIntent(context, 0, str2, 0L, true));
                }
            }
        }
    }

    public static void reportComplete(Context context, DaySummary daySummary) {
        SQLiteDatabase db = Core.get(context).db();
        if (Settings.flag(db, Settings.NOTIF_COMPLETION, true)) {
            String str = Biz.today();
            if (!str.equals(Settings.get(db, Settings.LAST_COMPLETE_NOTICE, ""))) {
                Settings.set(db, Settings.LAST_COMPLETE_NOTICE, str);
                Notifier.post(context, Core.CH_DONE, Notifier.ID_COMPLETE, "Reporting Completed", "100% of the " + daySummary.total + " active CSPs have reported for today.", Notifier.tapIntent(context, 0, str, 0L, false));
            }
        }
    }

    public static void handleIntent(Activity activity, Intent intent) {
        if (intent != null && intent.hasExtra(Notifier.EXTRA_TAB)) {
            int intExtra = intent.getIntExtra(Notifier.EXTRA_TAB, 0);
            String stringExtra = intent.getStringExtra(Notifier.EXTRA_DATE);
            long longExtra = intent.getLongExtra(Notifier.EXTRA_CSP, 0L);
            boolean booleanExtra = intent.getBooleanExtra(Notifier.EXTRA_FOLLOWUP, false);
            if (activity instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) activity;
                if (stringExtra != null) {
                    UiState.setDate(activity, stringExtra);
                }
                if (longExtra > 0) {
                    if (stringExtra == null) {
                        stringExtra = Biz.today();
                    }
                    mainActivity.push(new CspProfileScreen(mainActivity, mainActivity, longExtra, stringExtra));
                } else if (booleanExtra) {
                    mainActivity.push(new FollowupScreen(mainActivity, mainActivity));
                } else {
                    mainActivity.select(intExtra);
                    intent.removeExtra(Notifier.EXTRA_TAB);
                }
            }
        }
    }

    public static String plural(int i, String str) {
        return i + " " + str + (i == 1 ? "" : "s");
    }
}
