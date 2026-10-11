package org.cspdaily.report.update;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import java.io.File;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.notify.Notifier;
import org.cspdaily.report.ui.MainActivity;

public final class UpdateNotificationManager {
    public static final int ID_UPDATE_AVAILABLE = 400;
    public static final int ID_UPDATE_PROGRESS = 401;
    public static final int ID_UPDATE_COMPLETE = 402;
    public static final int ID_UPDATE_ERROR = 403;

    private UpdateNotificationManager() {}

    private static int flags() {
        return Build.VERSION.SDK_INT >= 23 ? 201326592 : 134217728;
    }

    public static void notifyUpdateAvailable(Context context, AppRelease release) {
        if (!Notifier.permitted(context) || release == null) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, ID_UPDATE_AVAILABLE, tapIntent, flags());

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, Core.CH_UPDATE)
                : new Notification.Builder(context);

        String title = "CSP Daily Report " + release.versionName + " Available";
        String summary = release.isMandatory(UpdateManager.getInstalledVersionCode(context))
                ? "Mandatory update required to continue."
                : "New features and performance improvements are ready.";

        b.setSmallIcon(R.drawable.ic_upload)
         .setContentTitle(title)
         .setContentText(summary)
         .setStyle(new Notification.BigTextStyle().bigText(summary + (release.releaseNotes.isEmpty() ? "" : "\n\n• " + String.join("\n• ", release.releaseNotes))))
         .setContentIntent(pi)
         .setAutoCancel(true)
         .setColor(context.getResources().getColor(R.color.primary_600));

        nm.notify(ID_UPDATE_AVAILABLE, b.build());
    }

    public static void notifyDownloadProgress(Context context, String versionName, int percent, long bytesRead, long totalBytes) {
        if (!Notifier.permitted(context)) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, Core.CH_UPDATE)
                : new Notification.Builder(context);

        String sizeText = totalBytes > 0
                ? String.format("%.1f MB / %.1f MB", bytesRead / (1024.0 * 1024.0), totalBytes / (1024.0 * 1024.0))
                : String.format("%.1f MB downloaded", bytesRead / (1024.0 * 1024.0));

        b.setSmallIcon(R.drawable.ic_download)
         .setContentTitle("Downloading Update v" + versionName)
         .setContentText(sizeText + (percent >= 0 ? " (" + percent + "%)" : ""))
         .setProgress(100, percent >= 0 ? percent : 0, percent < 0)
         .setOngoing(true)
         .setAutoCancel(false)
         .setColor(context.getResources().getColor(R.color.primary_600));

        nm.notify(ID_UPDATE_PROGRESS, b.build());
    }

    public static void notifyDownloadComplete(Context context, File apkFile, AppRelease release) {
        cancelProgress(context);
        if (!Notifier.permitted(context) || apkFile == null) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Uri apkUri = ApkFileProvider.getUriForFile(context, apkFile);
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, ID_UPDATE_COMPLETE, installIntent, flags());

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, Core.CH_UPDATE)
                : new Notification.Builder(context);

        b.setSmallIcon(R.drawable.ic_check_circle)
         .setContentTitle("Update Downloaded (v" + (release != null ? release.versionName : "") + ")")
         .setContentText("Tap to install the updated APK.")
         .setContentIntent(pi)
         .setAutoCancel(true)
         .setColor(context.getResources().getColor(R.color.ok));

        nm.notify(ID_UPDATE_COMPLETE, b.build());
    }

    public static void notifyUpdateError(Context context, String errorMessage) {
        cancelProgress(context);
        if (!Notifier.permitted(context)) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, Core.CH_UPDATE)
                : new Notification.Builder(context);

        b.setSmallIcon(R.drawable.ic_alert)
         .setContentTitle("Update Failed")
         .setContentText(errorMessage != null ? errorMessage : "Could not complete update download.")
         .setAutoCancel(true)
         .setColor(context.getResources().getColor(R.color.danger));

        nm.notify(ID_UPDATE_ERROR, b.build());
    }

    public static void cancelProgress(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(ID_UPDATE_PROGRESS);
        }
    }
}
