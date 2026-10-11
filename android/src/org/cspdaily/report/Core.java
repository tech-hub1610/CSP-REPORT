package org.cspdaily.report;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.Thread;
import org.cspdaily.report.data.Db;
import org.cspdaily.report.data.Params;

/* loaded from: classes.dex */
public class Core extends Application {
    public static final String CH_ALERT = "csp_alert";
    public static final String CH_DONE = "csp_done";
    public static final String CH_FOLLOWUP = "csp_followup";
    public static final String CH_REMINDER = "csp_reminder";
    public static final String CH_UPDATE = "csp_update";
    private static Core sInstance;

    public static Core get(Context context) {
        return sInstance != null ? sInstance : (Core) context.getApplicationContext();
    }

    public static Core instance() {
        return sInstance;
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        armCrashLog();
        try {
            Params.all(Db.get(this).getWritableDatabase());
        } catch (Throwable th) {
        }
        try {
            channels();
        } catch (Throwable th) {
        }
    }

    private void armCrashLog() {
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { // from class: org.cspdaily.report.Core.1
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public void uncaughtException(Thread thread, Throwable th) {
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(Core.this.getFilesDir(), "crash.txt")));
                    outputStreamWriter.write(Core.trace(th));
                    outputStreamWriter.close();
                } catch (Throwable th2) {
                }
                if (defaultUncaughtExceptionHandler != null) {
                    defaultUncaughtExceptionHandler.uncaughtException(thread, th);
                }
            }
        });
    }

    public static String trace(Throwable th) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (th != null && i < 6) {
            sb.append(i == 0 ? "" : "Caused by: ").append(th.getClass().getName()).append(": ").append(th.getMessage()).append('\n');
            StackTraceElement[] stackTrace = th.getStackTrace();
            for (int i2 = 0; i2 < Math.min(stackTrace.length, 40); i2++) {
                sb.append("    at ").append(stackTrace[i2]).append('\n');
            }
            th = th.getCause();
            i++;
        }
        return sb.toString();
    }

    public SQLiteDatabase db() {
        return Db.get(this).getWritableDatabase();
    }

    public SQLiteDatabase dbRead() {
        return Db.get(this).getReadableDatabase();
    }

    private void channels() {
        NotificationManager notificationManager;
        if (Build.VERSION.SDK_INT >= 26 && (notificationManager = (NotificationManager) getSystemService("notification")) != null) {
            mk(notificationManager, CH_REMINDER, "Report reminder", "Daily reminder to file CSP figures", 3);
            mk(notificationManager, CH_FOLLOWUP, "Follow-up reminder", "Reminds you to chase CSPs that have not reported", 3);
            mk(notificationManager, CH_ALERT, "Long pending alert", "Warns about CSPs pending for several working days", 4);
            mk(notificationManager, CH_DONE, "Reporting complete", "Confirms when every active CSP has reported", 2);
            mk(notificationManager, CH_UPDATE, "App Updates", "Notifications for new releases and APK downloads", 3);
        }
    }

    private void mk(NotificationManager notificationManager, String str, String str2, String str3, int i) {
        NotificationChannel notificationChannel = new NotificationChannel(str, str2, i);
        notificationChannel.setDescription(str3);
        notificationChannel.enableLights(false);
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}
