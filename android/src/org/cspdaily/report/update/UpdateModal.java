package org.cspdaily.report.update;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.File;
import java.util.Locale;
import org.cspdaily.report.R;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;

public class UpdateModal {

    public static void show(final Activity activity, final AppRelease release, final boolean isMandatory) {
        if (activity == null || activity.isFinishing() || release == null) return;

        final Dialog dialog = new Dialog(activity, R.style.Theme_Csp_Sheet);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(!isMandatory);
        dialog.setCanceledOnTouchOutside(!isMandatory);

        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(1);
        root.setBackground(Ui.round(activity, 0xFF0F172A, 20.0f, 0x3338BDF8, 1.0f));
        Ui.pad(root, 20, 20, 20, 20);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(-1, -2);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(0x00000000));
            if (Build.VERSION.SDK_INT >= 21) {
                window.setElevation(Ui.dp(activity, 16.0f));
            }
        }

        renderInitialPrompt(activity, dialog, root, release, isMandatory);
        dialog.show();
    }

    private static void renderInitialPrompt(final Activity activity, final Dialog dialog, final LinearLayout root, final AppRelease release, final boolean isMandatory) {
        root.removeAllViews();

        // 1. Header Row
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(0);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(headerRow, mw());

        FrameLayout iconBox = new FrameLayout(activity);
        iconBox.setBackground(Ui.round(activity, isMandatory ? 0x33EF4444 : 0x3338BDF8, 12.0f));
        int isize = Ui.dp(activity, 42.0f);
        headerRow.addView(iconBox, new LinearLayout.LayoutParams(isize, isize));

        ImageView icon = new ImageView(activity);
        icon.setImageResource(isMandatory ? R.drawable.ic_alert : R.drawable.ic_upload);
        icon.setColorFilter(isMandatory ? 0xFFEF4444 : 0xFF38BDF8);
        int ipad = Ui.dp(activity, 9.0f);
        icon.setPadding(ipad, ipad, ipad, ipad);
        iconBox.addView(icon, match());

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(1);
        LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        tclp.leftMargin = Ui.dp(activity, 12.0f);
        titleCol.setLayoutParams(tclp);
        headerRow.addView(titleCol);

        TextView tag = new TextView(activity);
        tag.setText(isMandatory ? "UPDATE REQUIRED" : "NEW UPDATE AVAILABLE");
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(tag, 11.0f);
        tag.setTextColor(isMandatory ? 0xFFF87171 : 0xFF38BDF8);
        titleCol.addView(tag);

        TextView appTitle = new TextView(activity);
        appTitle.setText("CSP Daily Report " + release.versionName);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(appTitle, 16.5f);
        appTitle.setTextColor(0xFFF8FAFC);
        titleCol.addView(appTitle);

        // 2. Message / Title
        if (release.message != null && !release.message.trim().isEmpty()) {
            TextView msg = new TextView(activity);
            msg.setText(release.message.trim());
            Ui.size(msg, 13.0f);
            msg.setTextColor(0xFFCBD5E1);
            root.addView(msg, top(activity, 12));
        }

        // 3. Release Notes Card
        if (release.releaseNotes != null && !release.releaseNotes.isEmpty()) {
            LinearLayout notesCard = new LinearLayout(activity);
            notesCard.setOrientation(1);
            notesCard.setBackground(Ui.round(activity, 0x1AFFFFFF, 12.0f));
            Ui.pad(notesCard, 12, 10, 12, 10);
            root.addView(notesCard, top(activity, 12));

            TextView notesTitle = new TextView(activity);
            notesTitle.setText("What's New in v" + release.versionName + ":");
            notesTitle.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(notesTitle, 12.5f);
            notesTitle.setTextColor(0xFF38BDF8);
            notesCard.addView(notesTitle);

            for (String note : release.releaseNotes) {
                TextView bullet = new TextView(activity);
                bullet.setText("•  " + note);
                Ui.size(bullet, 12.0f);
                bullet.setTextColor(0xFFE2E8F0);
                notesCard.addView(bullet, top(activity, 4));
            }
        }

        // 4. Action Buttons
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(0);
        btnRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(btnRow, top(activity, 18));

        if (!isMandatory) {
            TextView laterBtn = new TextView(activity);
            laterBtn.setText("Later");
            laterBtn.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(laterBtn, 14.0f);
            laterBtn.setGravity(Gravity.CENTER);
            laterBtn.setTextColor(0xFF94A3B8);
            laterBtn.setBackground(Ui.round(activity, 0x1AFFFFFF, 10.0f));
            Ui.pad(laterBtn, 16, 12, 16, 12);
            laterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            LinearLayout.LayoutParams lpLater = new LinearLayout.LayoutParams(0, -2, 1.0f);
            lpLater.rightMargin = Ui.dp(activity, 8.0f);
            btnRow.addView(laterBtn, lpLater);
        }

        TextView updateBtn = new TextView(activity);
        updateBtn.setText(isMandatory ? "UPDATE NOW" : "DOWNLOAD & UPDATE");
        updateBtn.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(updateBtn, 14.0f);
        updateBtn.setGravity(Gravity.CENTER);
        updateBtn.setTextColor(0xFF0F172A);
        updateBtn.setBackground(Ui.round(activity, isMandatory ? 0xFFEF4444 : 0xFF38BDF8, 10.0f));
        if (isMandatory) updateBtn.setTextColor(0xFFFFFFFF);
        Ui.pad(updateBtn, 16, 12, 16, 12);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderDownloadProgress(activity, dialog, root, release, isMandatory);
            }
        });
        btnRow.addView(updateBtn, new LinearLayout.LayoutParams(0, -2, isMandatory ? 1.0f : 1.5f));
    }

    private static void renderDownloadProgress(final Activity activity, final Dialog dialog, final LinearLayout root, final AppRelease release, final boolean isMandatory) {
        root.removeAllViews();
        dialog.setCancelable(false);

        // Header
        TextView title = new TextView(activity);
        title.setText("Downloading Update v" + release.versionName);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(title, 16.0f);
        title.setTextColor(0xFFF8FAFC);
        root.addView(title, mw());

        // Progress Text & Metrics
        final TextView progressMetrics = new TextView(activity);
        progressMetrics.setText("Connecting to server...");
        Ui.size(progressMetrics, 12.5f);
        progressMetrics.setTextColor(0xFF94A3B8);
        root.addView(progressMetrics, top(activity, 6));

        // Horizontal Progress Bar
        final ProgressBar pbar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        pbar.setMax(100);
        pbar.setProgress(0);
        pbar.setIndeterminate(true);
        root.addView(pbar, top(activity, 14));

        final TextView percentText = new TextView(activity);
        percentText.setText("0%");
        percentText.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(percentText, 13.0f);
        percentText.setGravity(Gravity.RIGHT);
        percentText.setTextColor(0xFF38BDF8);
        root.addView(percentText, top(activity, 4));

        // Cancel Button
        final TextView cancelBtn = new TextView(activity);
        cancelBtn.setText("Cancel Download");
        cancelBtn.setGravity(Gravity.CENTER);
        Ui.size(cancelBtn, 13.0f);
        cancelBtn.setTextColor(0xFF94A3B8);
        cancelBtn.setBackground(Ui.round(activity, 0x1AFFFFFF, 8.0f));
        Ui.pad(cancelBtn, 14, 10, 14, 10);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApkDownloader.cancel();
                UpdateNotificationManager.cancelProgress(activity);
                renderInitialPrompt(activity, dialog, root, release, isMandatory);
            }
        });
        root.addView(cancelBtn, top(activity, 14));

        // Start Download
        String downloadUrl = release.getResolvedDownloadUrl(SupabaseClient.getProjectUrl(activity), UpdateManager.STORAGE_BUCKET);
        String fileName = "CSPDailyReport-" + release.versionName + ".apk";

        UpdateNotificationManager.notifyDownloadProgress(activity, release.versionName, 0, 0, 0);

        ApkDownloader.download(activity, downloadUrl, fileName, new ApkDownloader.DownloadListener() {
            @Override
            public void onDownloadStarted(long totalBytes) {
                pbar.setIndeterminate(false);
                progressMetrics.setText(totalBytes > 0
                        ? String.format(Locale.US, "0.0 MB / %.1f MB", totalBytes / (1024.0 * 1024.0))
                        : "Downloading...");
            }

            @Override
            public void onDownloadProgress(long bytesRead, long totalBytes, int percent) {
                pbar.setIndeterminate(false);
                if (percent >= 0) {
                    pbar.setProgress(percent);
                    percentText.setText(percent + "%");
                }
                if (totalBytes > 0) {
                    progressMetrics.setText(String.format(Locale.US, "%.1f MB / %.1f MB (%d%%)", bytesRead / (1024.0 * 1024.0), totalBytes / (1024.0 * 1024.0), percent));
                } else {
                    progressMetrics.setText(String.format(Locale.US, "%.1f MB downloaded", bytesRead / (1024.0 * 1024.0)));
                }
                UpdateNotificationManager.notifyDownloadProgress(activity, release.versionName, percent, bytesRead, totalBytes);
            }

            @Override
            public void onDownloadSuccess(final File apkFile) {
                UpdateNotificationManager.cancelProgress(activity);
                renderVerification(activity, dialog, root, release, apkFile, isMandatory);
            }

            @Override
            public void onDownloadError(String message, Throwable error) {
                UpdateNotificationManager.notifyUpdateError(activity, message);
                renderError(activity, dialog, root, release, "Download Failed: " + message, isMandatory);
            }

            @Override
            public void onDownloadCancelled() {
                UpdateNotificationManager.cancelProgress(activity);
                renderInitialPrompt(activity, dialog, root, release, isMandatory);
            }
        });
    }

    private static void renderVerification(final Activity activity, final Dialog dialog, final LinearLayout root, final AppRelease release, final File apkFile, final boolean isMandatory) {
        root.removeAllViews();

        TextView title = new TextView(activity);
        title.setText("Verifying APK Integrity...");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(title, 16.0f);
        title.setTextColor(0xFF38BDF8);
        root.addView(title, mw());

        ProgressBar spinner = new ProgressBar(activity);
        root.addView(spinner, top(activity, 16));

        // Perform verification on background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ApkIntegrityVerifier.VerificationResult result = ApkIntegrityVerifier.verify(activity, apkFile, release);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result.valid) {
                            UpdateNotificationManager.notifyDownloadComplete(activity, apkFile, release);
                            renderReadyToInstall(activity, dialog, root, release, apkFile, isMandatory);
                        } else {
                            UpdateNotificationManager.notifyUpdateError(activity, result.errorMessage);
                            renderError(activity, dialog, root, release, "Verification Failed:\n" + result.errorMessage, isMandatory);
                        }
                    }
                });
            }
        }).start();
    }

    private static void renderReadyToInstall(final Activity activity, final Dialog dialog, final LinearLayout root, final AppRelease release, final File apkFile, final boolean isMandatory) {
        root.removeAllViews();

        FrameLayout iconBox = new FrameLayout(activity);
        iconBox.setBackground(Ui.round(activity, 0x3310B981, 16.0f));
        int isize = Ui.dp(activity, 52.0f);
        root.addView(iconBox, new LinearLayout.LayoutParams(isize, isize));

        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_check_circle);
        icon.setColorFilter(0xFF10B981);
        int ipad = Ui.dp(activity, 10.0f);
        icon.setPadding(ipad, ipad, ipad, ipad);
        iconBox.addView(icon, match());

        TextView title = new TextView(activity);
        title.setText("Update Verified & Ready!");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(title, 16.5f);
        title.setTextColor(0xFFF8FAFC);
        root.addView(title, top(activity, 10));

        TextView sub = new TextView(activity);
        sub.setText("CSP Daily Report v" + release.versionName + " has passed SHA-256 integrity verification. Tap below to install.");
        Ui.size(sub, 13.0f);
        sub.setTextColor(0xFFCBD5E1);
        root.addView(sub, top(activity, 4));

        TextView installBtn = new TextView(activity);
        installBtn.setText("INSTALL NOW");
        installBtn.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(installBtn, 14.5f);
        installBtn.setGravity(Gravity.CENTER);
        installBtn.setTextColor(0xFF0F172A);
        installBtn.setBackground(Ui.round(activity, 0xFF34D399, 10.0f));
        Ui.pad(installBtn, 16, 12, 16, 12);
        installBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ApkInstaller.installApk(activity, apkFile);
                } catch (Exception e) {
                    Ui.toast(activity, "Could not launch installer: " + e.getMessage());
                }
            }
        });
        root.addView(installBtn, top(activity, 16));
    }

    private static void renderError(final Activity activity, final Dialog dialog, final LinearLayout root, final AppRelease release, final String errorMessage, final boolean isMandatory) {
        root.removeAllViews();

        FrameLayout iconBox = new FrameLayout(activity);
        iconBox.setBackground(Ui.round(activity, 0x33EF4444, 16.0f));
        int isize = Ui.dp(activity, 48.0f);
        root.addView(iconBox, new LinearLayout.LayoutParams(isize, isize));

        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_alert);
        icon.setColorFilter(0xFFEF4444);
        int ipad = Ui.dp(activity, 10.0f);
        icon.setPadding(ipad, ipad, ipad, ipad);
        iconBox.addView(icon, match());

        TextView title = new TextView(activity);
        title.setText("Update Failed");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(title, 16.0f);
        title.setTextColor(0xFFF87171);
        root.addView(title, top(activity, 10));

        TextView msg = new TextView(activity);
        msg.setText(errorMessage);
        Ui.size(msg, 12.5f);
        msg.setTextColor(0xFFE2E8F0);
        root.addView(msg, top(activity, 6));

        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(0);
        root.addView(btnRow, top(activity, 16));

        if (!isMandatory) {
            TextView closeBtn = new TextView(activity);
            closeBtn.setText("Dismiss");
            closeBtn.setGravity(Gravity.CENTER);
            Ui.size(closeBtn, 13.5f);
            closeBtn.setTextColor(0xFF94A3B8);
            closeBtn.setBackground(Ui.round(activity, 0x1AFFFFFF, 8.0f));
            Ui.pad(closeBtn, 14, 10, 14, 10);
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            lp.rightMargin = Ui.dp(activity, 8.0f);
            btnRow.addView(closeBtn, lp);
        }

        TextView retryBtn = new TextView(activity);
        retryBtn.setText("Retry Download");
        retryBtn.setTypeface(Typeface.DEFAULT_BOLD);
        retryBtn.setGravity(Gravity.CENTER);
        retryBtn.setTextColor(0xFF0F172A);
        retryBtn.setBackground(Ui.round(activity, 0xFF38BDF8, 8.0f));
        Ui.pad(retryBtn, 14, 10, 14, 10);
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderDownloadProgress(activity, dialog, root, release, isMandatory);
            }
        });
        btnRow.addView(retryBtn, new LinearLayout.LayoutParams(0, -2, 1.0f));
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(-1, -1);
    }

    private static LinearLayout.LayoutParams top(Context context, int dp) {
        LinearLayout.LayoutParams lp = mw();
        lp.topMargin = Ui.dp(context, dp);
        return lp;
    }
}
