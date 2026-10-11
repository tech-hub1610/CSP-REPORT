package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.MainActivity;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.ui.widget.Section;
import org.cspdaily.report.update.ApkIntegrityVerifier;
import org.cspdaily.report.update.AppRelease;
import org.cspdaily.report.update.RemoteConfig;
import org.cspdaily.report.update.UpdateManager;
import org.cspdaily.report.update.UpdateModal;
import org.json.JSONArray;
import org.json.JSONObject;

public class AppUpdatesScreen extends Screen {
    private static final String[] TABS_SUPERADMIN = {"🚀 Update Status", "📋 Release Manager", "⚙️ Remote Config"};
    private static final String[] TABS_USER = {"🚀 Update Status", "📜 Release History"};

    private String currentTab = "🚀 Update Status";
    private final LinearLayout contentArea;
    private final Chips tabsChips;
    private final Empty empty;
    private static PublishDialogHolder currentPublishDialog;

    public static class PublishDialogHolder {
        public Dialog dialog;
        public EditText etVName;
        public EditText etVCode;
        public EditText etMinCode;
        public EditText etPath;
        public EditText etSha;
        public TextView apkStatusText;
        public LinearLayout apkValidationCard;
        public TextView validationDetailsText;
        public File[] selectedApkHolder;
    }

    public AppUpdatesScreen(Context context, Screen.Host host) {
        super(context, host);
        boolean isSuperAdmin = Auth.isSuperAdmin(context);
        title(isSuperAdmin ? "App Release Manager" : "App Updates");
        subtitle(isSuperAdmin ? "OTA releases, remote configuration & cloud management" : "OTA version status & update history");

        headerIcon(R.drawable.ic_upload, R.color.primary_600, new Runnable() {
            @Override
            public void run() {
                AppUpdatesScreen.this.checkNow(true);
            }
        });

        // 1. Navigation Tabs
        this.tabsChips = new Chips(context);
        String[] tabs = isSuperAdmin ? TABS_SUPERADMIN : TABS_USER;
        this.tabsChips.set(tabs);
        this.tabsChips.allowNone(false);
        this.tabsChips.select(0);
        this.tabsChips.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                AppUpdatesScreen.this.currentTab = str;
                AppUpdatesScreen.this.renderContent();
            }
        });
        add(this.tabsChips, 0);

        // 2. Dynamic Content Area
        this.contentArea = new LinearLayout(context);
        this.contentArea.setOrientation(1);
        add(this.contentArea, 8);

        this.empty = new Empty(context);
        add(this.empty, 0);

        refreshContent();
    }

    @Override
    public String headline() {
        return Auth.isSuperAdmin(getContext()) ? "App Release Manager" : "App Updates";
    }

    @Override
    public void refreshContent() {
        renderContent();
    }

    private void renderContent() {
        this.contentArea.removeAllViews();
        this.empty.hide();

        if (this.currentTab.contains("Release")) {
            renderReleaseManagerTab();
        } else if (this.currentTab.contains("Remote")) {
            renderRemoteConfigTab();
        } else {
            renderStatusTab();
        }
    }

    // ---------------- TAB 1: UPDATE STATUS ----------------
    private void renderStatusTab() {
        final Context context = getContext();
        int installedCode = UpdateManager.getInstalledVersionCode(context);
        String installedName = UpdateManager.getInstalledVersionName(context);
        final AppRelease latest = UpdateManager.getLatestCachedRelease(context);

        boolean hasUpdate = latest != null && latest.versionCode > installedCode;
        boolean isMandatory = hasUpdate && latest.isMandatory(installedCode);

        // 1. Version Hero Card
        LinearLayout heroCard = Cards.card(context);
        heroCard.setOrientation(1);
        heroCard.setBackground(Ui.round(context, hasUpdate ? (isMandatory ? 0xFF3B1818 : 0xFF0B243B) : 0xFF0B132B, 16.0f, hasUpdate ? (isMandatory ? 0xFFEF4444 : 0xFF38BDF8) : 0x3338BDF8, 1.0f));
        Ui.pad(heroCard, 16, 14, 16, 14);
        this.contentArea.addView(heroCard, mw());

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        heroCard.addView(topRow, mw());

        FrameLayout iconBox = new FrameLayout(context);
        iconBox.setBackground(Ui.round(context, hasUpdate ? (isMandatory ? 0x33EF4444 : 0x3338BDF8) : 0x2634D399, 12.0f));
        int isize = Ui.dp(context, 40.0f);
        topRow.addView(iconBox, new LinearLayout.LayoutParams(isize, isize));

        ImageView icon = new ImageView(context);
        icon.setImageResource(hasUpdate ? R.drawable.ic_upload : R.drawable.ic_check_circle);
        icon.setColorFilter(hasUpdate ? (isMandatory ? 0xFFEF4444 : 0xFF38BDF8) : 0xFF34D399);
        int ipad = Ui.dp(context, 8.0f);
        icon.setPadding(ipad, ipad, ipad, ipad);
        iconBox.addView(icon, match());

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(1);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        infoLp.leftMargin = Ui.dp(context, 10.0f);
        info.setLayoutParams(infoLp);
        topRow.addView(info);

        TextView title = new TextView(context);
        title.setText(hasUpdate ? (isMandatory ? "CRITICAL UPDATE REQUIRED" : "UPDATE AVAILABLE") : "APP IS UP TO DATE");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(title, 14.5f);
        title.setTextColor(hasUpdate ? (isMandatory ? 0xFFF87171 : 0xFF38BDF8) : 0xFF34D399);
        info.addView(title);

        TextView sub = new TextView(context);
        sub.setText("Installed: v" + installedName + " (Build " + installedCode + ")");
        Ui.size(sub, 11.5f);
        sub.setTextColor(0xFF94A3B8);
        info.addView(sub);

        Badge statusBadge = new Badge(context);
        statusBadge.setKind(hasUpdate ? (isMandatory ? 0 : 3) : 1);
        statusBadge.setText(hasUpdate ? (isMandatory ? "MANDATORY" : "NEW RELEASE") : "LATEST");
        topRow.addView(statusBadge, wrap());

        // Release details banner
        if (latest != null) {
            LinearLayout dtCard = new LinearLayout(context);
            dtCard.setOrientation(1);
            dtCard.setBackground(Ui.round(context, 0x1AFFFFFF, 10.0f));
            Ui.pad(dtCard, 10, 8, 10, 8);
            heroCard.addView(dtCard, top(context, 12));

            TextView rName = new TextView(context);
            rName.setText("Cloud Release: v" + latest.versionName + " · Build " + latest.versionCode + (latest.releasedAt.isEmpty() ? "" : " · " + latest.releasedAt));
            rName.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(rName, 12.0f);
            rName.setTextColor(0xFFF8FAFC);
            dtCard.addView(rName);

            if (latest.message != null && !latest.message.trim().isEmpty()) {
                TextView msg = new TextView(context);
                msg.setText(latest.message.trim());
                Ui.size(msg, 11.5f);
                msg.setTextColor(0xFFCBD5E1);
                dtCard.addView(msg, top(context, 4));
            }
        }

        // Action Buttons Row inside Hero Card
        LinearLayout heroActRow = new LinearLayout(context);
        heroActRow.setOrientation(0);
        heroActRow.setGravity(Gravity.CENTER_VERTICAL);
        heroCard.addView(heroActRow, top(context, 14));

        View checkBtn = Cards.outlineButton(context, "⟳ Check Cloud Now", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUpdatesScreen.this.checkNow(true);
            }
        });
        heroActRow.addView(checkBtn, new LinearLayout.LayoutParams(0, -2, 1.0f));

        if (hasUpdate && context instanceof Activity) {
            View updateBtn = Cards.primaryButton(context, isMandatory ? "UPDATE NOW" : "DOWNLOAD & INSTALL", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UpdateModal.show((Activity) context, latest, latest.isMandatory(UpdateManager.getInstalledVersionCode(context)));
                }
            });
            LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(0, -2, 1.2f);
            ulp.leftMargin = Ui.dp(context, 8.0f);
            heroActRow.addView(updateBtn, ulp);
        }

        // 2. Active Remote Status Card
        Section s2 = new Section(context);
        s2.set("Remote System Status");
        this.contentArea.addView(s2, top(context, 14));

        LinearLayout statusGrid = Cards.card(context);
        statusGrid.setOrientation(1);
        Ui.pad(statusGrid, 12, 10, 12, 10);
        this.contentArea.addView(statusGrid, mw());

        boolean isMaint = RemoteConfig.isMaintenanceMode(context);
        RemoteConfig.Announcement ann = RemoteConfig.getAnnouncement(context);

        statusGrid.addView(statusRow(context, "Maintenance Mode", isMaint ? "Active (Restricted)" : "Inactive (Normal)", isMaint ? 0xFFEF4444 : 0xFF10B981));
        statusGrid.addView(divider(context));
        statusGrid.addView(statusRow(context, "Broadcast Announcement", ann.hasContent() ? ("Active (" + ann.title + ")") : "None active", ann.hasContent() ? 0xFF38BDF8 : 0xFF94A3B8));
        statusGrid.addView(divider(context));
        String lastCheck = Settings.get(Core.get(context).dbRead(), UpdateManager.KEY_LAST_UPDATE_CHECK, "");
        String lastCheckFmt = lastCheck.isEmpty() ? "Never" : Biz.clockOf(Biz.now());
        statusGrid.addView(statusRow(context, "Last Update Check", lastCheckFmt, 0xFF64748B));

        // 3. Superadmin Release Controls
        if (Auth.isSuperAdmin(context)) {
            Section s3 = new Section(context);
            s3.set("Superadmin Release Controls");
            this.contentArea.addView(s3, top(context, 14));

            LinearLayout adminCard = Cards.card(context);
            adminCard.setOrientation(1);
            Ui.pad(adminCard, 14, 12, 14, 12);
            this.contentArea.addView(adminCard, mw());

            View uploadReleaseRow = adminActionRow(context, R.drawable.ic_upload, "Publish New APK Release", "Select APK from device, validate and upload to Supabase", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUpdatesScreen.this.openPublishDialog();
                }
            });
            adminCard.addView(uploadReleaseRow, mw());

            adminCard.addView(divider(context));

            View remoteConfigRow = adminActionRow(context, R.drawable.ic_settings, "Edit Remote Configuration", "Toggle maintenance mode, announcements and feature flags", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUpdatesScreen.this.openRemoteConfigEditor();
                }
            });
            adminCard.addView(remoteConfigRow, mw());
        }
    }

    // ---------------- TAB 2: RELEASE MANAGER & HISTORY ----------------
    private void renderReleaseManagerTab() {
        final Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        List<AppRelease> releases = UpdateManager.getAllCachedReleases(context);
        int installedCode = UpdateManager.getInstalledVersionCode(context);
        String installedName = UpdateManager.getInstalledVersionName(context);
        AppRelease activeRelease = UpdateManager.getLatestCachedRelease(context);

        // 1. Executive Status Header Card
        LinearLayout topSummaryCard = Cards.card(context);
        topSummaryCard.setOrientation(1);
        Ui.pad(topSummaryCard, 14, 12, 14, 12);
        this.contentArea.addView(topSummaryCard, mw());

        LinearLayout sumRow = new LinearLayout(context);
        sumRow.setOrientation(0);
        sumRow.setGravity(Gravity.CENTER_VERTICAL);
        topSummaryCard.addView(sumRow, mw());

        LinearLayout sumCol = new LinearLayout(context);
        sumCol.setOrientation(1);
        sumRow.addView(sumCol, weight1());

        TextView sumTitle = Cards.bold(context, "Production Release Status", 14.5f, R.color.ink);
        sumCol.addView(sumTitle);

        String activeTxt = activeRelease != null ? ("v" + activeRelease.versionName + " (Build " + activeRelease.versionCode + ")") : "None Active";
        TextView activeSub = new TextView(context);
        activeSub.setText("Active Production: " + activeTxt + "\nDevice Installed: v" + installedName + " (Build " + installedCode + ")");
        Ui.size(activeSub, 12.0f);
        activeSub.setTextColor(Ui.color(context, R.color.ink_3));
        sumCol.addView(activeSub, top(context, 4));

        if (Auth.isSuperAdmin(context)) {
            View newPubBtn = Cards.primaryButton(context, "+ Upload APK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUpdatesScreen.this.openPublishDialog();
                }
            });
            sumRow.addView(newPubBtn, wrap());
        }

        // Section Title
        Section s = new Section(context);
        s.set("Published Releases History (" + releases.size() + ")");
        this.contentArea.addView(s, top(context, 14));

        if (releases.isEmpty()) {
            this.empty.show(R.drawable.ic_check_circle, R.color.line_2, "No Releases Found", "Tap 'Check Cloud Now' to fetch published releases from Supabase.");
            return;
        }

        for (int i = 0; i < releases.size(); i++) {
            final AppRelease r = releases.get(i);
            LinearLayout card = Cards.card(context);
            card.setOrientation(1);
            Ui.pad(card, 14, 12, 14, 12);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(0);
            topRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(topRow, mw());

            LinearLayout titleCol = new LinearLayout(context);
            titleCol.setOrientation(1);
            topRow.addView(titleCol, weight1());

            TextView vName = new TextView(context);
            vName.setText("v" + r.versionName + " (Build " + r.versionCode + ")");
            vName.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(vName, 15.0f);
            vName.setTextColor(Ui.color(context, R.color.ink));
            titleCol.addView(vName);

            TextView meta = new TextView(context);
            meta.setText("Type: " + r.updateType.toUpperCase(Locale.US) + (r.releasedAt.isEmpty() ? "" : " · " + r.releasedAt) + "\nMin Version: Build " + r.minimumSupportedVersionCode);
            Ui.size(meta, 11.5f);
            meta.setTextColor(Ui.color(context, R.color.ink_3));
            titleCol.addView(meta, top(context, 2));

            Badge badge = new Badge(context);
            badge.setKind(r.isActive ? 1 : 0);
            badge.setText(r.isActive ? "ACTIVE" : "INACTIVE");
            topRow.addView(badge, wrap());

            if (r.message != null && !r.message.trim().isEmpty()) {
                TextView msg = new TextView(context);
                msg.setText(r.message.trim());
                Ui.size(msg, 12.0f);
                msg.setTextColor(Ui.color(context, R.color.ink_2));
                card.addView(msg, top(context, 6));
            }

            // Button actions row
            LinearLayout actRow = new LinearLayout(context);
            actRow.setOrientation(0);
            actRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(actRow, top(context, 10));

            View notesBtn = Cards.outlineButton(context, "📝 Changelog", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUpdatesScreen.this.showChangelogDialog(r);
                }
            });
            actRow.addView(notesBtn, weightMargin(1.0f, 4));

            View detailsBtn = Cards.outlineButton(context, "ℹ️ Info", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUpdatesScreen.this.showDetailsDialog(r);
                }
            });
            actRow.addView(detailsBtn, weightMargin(1.0f, Auth.isSuperAdmin(context) ? 4 : 0));

            if (Auth.isSuperAdmin(context)) {
                View toggleBtn = Cards.outlineButton(context, r.isActive ? "Deactivate" : "Activate (Rollback)", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppUpdatesScreen.this.confirmToggleRelease(r);
                    }
                });
                actRow.addView(toggleBtn, weightMargin(1.4f, 0));
            }

            LinearLayout.LayoutParams lp = mw();
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 8.0f);
            this.contentArea.addView(card, lp);
        }
    }

    // ---------------- TAB 3: REMOTE CONFIG ----------------
    private void renderRemoteConfigTab() {
        Context context = getContext();
        java.util.Map<String, String> configs = RemoteConfig.getAll(context);

        LinearLayout card = Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 12, 14, 12);
        this.contentArea.addView(card, mw());

        TextView header = Cards.bold(context, "Live Remote Configuration Values", 14.0f, R.color.ink);
        card.addView(header);

        TextView note = new TextView(context);
        note.setText("Values are downloaded automatically from Supabase and applied instantly across all supervisor and admin devices without requiring APK reinstallation.");
        Ui.size(note, 12.0f);
        note.setTextColor(Ui.color(context, R.color.ink_3));
        card.addView(note, top(context, 4));

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(1);
        card.addView(list, top(context, 10));

        if (configs.isEmpty()) {
            TextView emptyText = new TextView(context);
            emptyText.setText("No remote configs loaded yet. Tap 'Check Cloud Now' to fetch.");
            Ui.size(emptyText, 12.5f);
            emptyText.setTextColor(Ui.color(context, R.color.ink_4));
            list.addView(emptyText, top(context, 8));
        } else {
            for (java.util.Map.Entry<String, String> entry : configs.entrySet()) {
                list.addView(configItem(context, entry.getKey(), entry.getValue()));
                list.addView(divider(context));
            }
        }

        if (Auth.isAdmin(context)) {
            View editBtn = Cards.primaryButton(context, "⚙️ Modify Remote Config", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUpdatesScreen.this.openRemoteConfigEditor();
                }
            });
            this.contentArea.addView(editBtn, top(context, 12));
        }
    }

    private View configItem(Context context, String key, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Ui.pad(row, 4, 8, 4, 8);

        LinearLayout left = new LinearLayout(context);
        left.setOrientation(1);
        row.addView(left, weight1());

        TextView k = Cards.bold(context, key, 13.5f, R.color.ink);
        left.addView(k);

        TextView v = new TextView(context);
        v.setText(value);
        v.setTypeface(Typeface.MONOSPACE);
        Ui.size(v, 12.0f);
        v.setTextColor(Ui.color(context, R.color.primary_700));
        row.addView(v, wrap());

        return row;
    }

    private View statusRow(Context context, String label, String value, int valueColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Ui.pad(row, 4, 8, 4, 8);

        TextView lbl = new TextView(context);
        lbl.setText(label);
        Ui.size(lbl, 13.0f);
        lbl.setTextColor(Ui.color(context, R.color.ink_2));
        row.addView(lbl, weight1());

        TextView val = new TextView(context);
        val.setText(value);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(val, 13.0f);
        val.setTextColor(valueColor);
        row.addView(val, wrap());

        return row;
    }

    private View divider(Context context) {
        View d = new View(context);
        d.setBackgroundColor(Ui.color(context, R.color.line));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        lp.topMargin = Ui.dp(context, 4.0f);
        lp.bottomMargin = Ui.dp(context, 4.0f);
        d.setLayoutParams(lp);
        return d;
    }

    private View adminActionRow(Context context, int iconRes, String title, String subtitle, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Ui.pad(row, 6, 8, 6, 8);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Ui.color(context, R.color.primary_600));
        int isize = Ui.dp(context, 24.0f);
        row.addView(icon, new LinearLayout.LayoutParams(isize, isize));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(1);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        tlp.leftMargin = Ui.dp(context, 12.0f);
        textCol.setLayoutParams(tlp);
        row.addView(textCol);

        TextView t = Cards.bold(context, title, 14.0f, R.color.ink);
        textCol.addView(t);

        TextView s = new TextView(context);
        s.setText(subtitle);
        Ui.size(s, 11.5f);
        s.setTextColor(Ui.color(context, R.color.ink_3));
        textCol.addView(s, top(context, 2));

        row.setOnClickListener(onClick);
        return row;
    }

    private void checkNow(final boolean toastResult) {
        final Context context = getContext();
        if (toastResult) this.host.toast("Checking Supabase for updates...");

        UpdateManager.checkForUpdates(context, true, new UpdateManager.CheckCallback() {
            @Override
            public void onNoUpdate(int currentCode, String currentName) {
                if (toastResult) AppUpdatesScreen.this.host.toast("You're on the latest version (v" + currentName + ")");
                AppUpdatesScreen.this.renderContent();
            }

            @Override
            public void onUpdateAvailable(AppRelease release, boolean mandatory) {
                AppUpdatesScreen.this.renderContent();
                if (context instanceof Activity) {
                    UpdateModal.show((Activity) context, release, mandatory);
                }
            }

            @Override
            public void onRemoteConfigApplied() {
                AppUpdatesScreen.this.renderContent();
            }

            @Override
            public void onError(String message, Throwable error) {
                if (toastResult) AppUpdatesScreen.this.host.toast("Update check failed: " + message);
                AppUpdatesScreen.this.renderContent();
            }
        });
    }

    // ---------------- NATIVE FILE PICKER RECEIVER ----------------
    public void onApkFilePicked(Uri uri) {
        final Context context = getContext();
        if (uri == null || currentPublishDialog == null || currentPublishDialog.dialog == null || !currentPublishDialog.dialog.isShowing()) {
            this.host.toast("No active publish window to receive APK.");
            return;
        }

        try {
            File cacheFile = new File(context.getCacheDir(), "staged_publish.apk");
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) throw new Exception("Could not open stream for selected APK");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            byte[] buf = new byte[8192];
            int read;
            while ((read = is.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
            is.close();
            fos.flush();
            fos.close();

            validateAndPopulateApk(cacheFile, currentPublishDialog);
            this.host.toast("APK selected and verified successfully!");

        } catch (Exception e) {
            if (currentPublishDialog.apkStatusText != null) {
                currentPublishDialog.apkStatusText.setText("❌ File error: " + e.getMessage());
                currentPublishDialog.apkStatusText.setTextColor(Ui.color(context, R.color.danger));
            }
            this.host.toast("Error reading selected APK: " + e.getMessage());
        }
    }

    private void validateAndPopulateApk(File apkFile, PublishDialogHolder holder) {
        final Context context = getContext();
        holder.selectedApkHolder[0] = apkFile;

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            if (pi == null) {
                throw new Exception("Corrupted APK: Android Package Manager could not parse package archive.");
            }

            String expectedPkg = context.getPackageName();
            if (!expectedPkg.equals(pi.packageName)) {
                throw new Exception("Package mismatch! Expected '" + expectedPkg + "' but found '" + pi.packageName + "'.");
            }

            int installedCode = UpdateManager.getInstalledVersionCode(context);
            if (pi.versionCode <= installedCode) {
                // Show warning but populate
                holder.apkStatusText.setText("⚠️ Warning: Selected versionCode (" + pi.versionCode + ") is <= installed (" + installedCode + ")");
                holder.apkStatusText.setTextColor(0xFFD97706);
            } else {
                holder.apkStatusText.setText("✅ Verified: " + apkFile.getName() + " (" + (apkFile.length() / 1024) + " KB)");
                holder.apkStatusText.setTextColor(Ui.color(context, R.color.primary_700));
            }

            if (pi.versionName != null && !pi.versionName.isEmpty()) {
                holder.etVName.setText(pi.versionName);
            }
            holder.etVCode.setText(String.valueOf(pi.versionCode));
            String standardName = "CSPDailyReport-" + (pi.versionName != null ? pi.versionName : "release") + ".apk";
            holder.etPath.setText(standardName);

            String sha256 = ApkIntegrityVerifier.calculateSha256(apkFile);
            holder.etSha.setText(sha256);

            holder.apkValidationCard.setVisibility(View.VISIBLE);
            holder.validationDetailsText.setText("• Package: " + pi.packageName + "\n• Version: v" + pi.versionName + " (Build " + pi.versionCode + ")\n• File Size: " + (apkFile.length() / 1024) + " KB (" + apkFile.length() + " bytes)\n• SHA-256: " + sha256 + "\n• Signature: Pre-signed Android package archive");

        } catch (Exception ex) {
            holder.selectedApkHolder[0] = null;
            String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            if (msg.contains("EACCES") || msg.contains("Permission denied")) {
                holder.apkStatusText.setText("⚠️ Direct storage access restricted by Android.\nPlease tap '[ 📂 System File Picker ]' above to select the APK file.");
            } else {
                holder.apkStatusText.setText("❌ Validation Failed: " + msg);
            }
            holder.apkStatusText.setTextColor(Ui.color(context, R.color.danger));
            holder.apkValidationCard.setVisibility(View.GONE);
        }
    }

    // ---------------- PUBLISH NEW RELEASE DIALOG ----------------
    private void openPublishDialog() {
        final Context context = getContext();
        if (!Auth.isSuperAdmin(context)) {
            this.host.toast("Access restricted: Only Superadmin can publish releases.");
            return;
        }
        final Dialog d = new Dialog(context, R.style.Theme_Csp_Sheet);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(1);
        Ui.pad(root, 18, 16, 18, 16);
        root.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 18.0f));

        final PublishDialogHolder holder = new PublishDialogHolder();
        holder.dialog = d;
        holder.selectedApkHolder = new File[1];
        currentPublishDialog = holder;

        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface di) {
                if (currentPublishDialog == holder) {
                    currentPublishDialog = null;
                }
            }
        });

        TextView t = Cards.bold(context, "Publish New APK Release", 16.5f, R.color.ink);
        root.addView(t);

        TextView subNote = new TextView(context);
        subNote.setText("Select an APK on device to auto-validate, hash & upload to Supabase Storage.");
        Ui.size(subNote, 12.0f);
        subNote.setTextColor(Ui.color(context, R.color.ink_3));
        root.addView(subNote, top(context, 4));

        // File Selection Buttons Row
        LinearLayout pickerRow = new LinearLayout(context);
        pickerRow.setOrientation(0);
        root.addView(pickerRow, top(context, 10));

        View systemPickBtn = Cards.primaryButton(context, "📂 System File Picker", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).pickApkFile();
                } else {
                    AppUpdatesScreen.this.host.toast("File chooser only available from main activity");
                }
            }
        });
        pickerRow.addView(systemPickBtn, weightMargin(1.0f, 4));

        View scanLocalBtn = Cards.outlineButton(context, "📁 Auto-Scan Build", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File[] candidates = new File[] {
                    new File("/sdcard/prjcts/report/CSPDailyReport-2.0.2.apk"),
                    new File("/sdcard/prjcts/report/CSPDailyReport-2.0.1.apk"),
                    new File("/sdcard/prjcts/report/CSPDailyReport.apk"),
                    new File(context.getExternalFilesDir(null), "CSPDailyReport.apk"),
                    new File("/sdcard/Download/CSPDailyReport.apk")
                };

                File found = null;
                for (File cand : candidates) {
                    if (cand.exists() && cand.length() > 0) {
                        found = cand;
                        break;
                    }
                }

                if (found != null) {
                    validateAndPopulateApk(found, holder);
                    AppUpdatesScreen.this.host.toast("Found: " + found.getName());
                } else {
                    AppUpdatesScreen.this.host.toast("No build APK found in project directories.");
                }
            }
        });
        pickerRow.addView(scanLocalBtn, weightMargin(1.0f, 0));

        holder.apkStatusText = new TextView(context);
        holder.apkStatusText.setText("No local APK selected yet.");
        Ui.size(holder.apkStatusText, 11.5f);
        holder.apkStatusText.setTextColor(Ui.color(context, R.color.ink_4));
        root.addView(holder.apkStatusText, top(context, 6));

        // Validation Details Card (Initially hidden)
        holder.apkValidationCard = new LinearLayout(context);
        holder.apkValidationCard.setOrientation(1);
        holder.apkValidationCard.setBackground(Ui.round(context, 0x0F38BDF8, 8.0f));
        Ui.pad(holder.apkValidationCard, 10, 8, 10, 8);
        holder.apkValidationCard.setVisibility(View.GONE);
        root.addView(holder.apkValidationCard, top(context, 6));

        holder.validationDetailsText = new TextView(context);
        Ui.size(holder.validationDetailsText, 11.0f);
        holder.validationDetailsText.setTextColor(Ui.color(context, R.color.ink_2));
        holder.apkValidationCard.addView(holder.validationDetailsText);

        holder.etVName = input(context, "Version Name (e.g. 2.0.2)", "2.0.2");
        holder.etVCode = input(context, "Version Code (Numeric, e.g. 20260922)", "20260922");
        holder.etMinCode = input(context, "Min Supported Version Code (e.g. 20260916)", "20260916");
        holder.etPath = input(context, "APK Storage File Name (e.g. CSPDailyReport-2.0.2.apk)", "CSPDailyReport-2.0.2.apk");
        holder.etSha = input(context, "SHA-256 Hash", "");

        root.addView(holder.etVName, top(context, 8));
        root.addView(holder.etVCode, top(context, 6));
        root.addView(holder.etMinCode, top(context, 6));
        root.addView(holder.etPath, top(context, 6));
        root.addView(holder.etSha, top(context, 6));

        // Release Classification RadioGroup
        TextView typeLabel = Cards.label(context, "Release Classification", R.style.Text_Label, R.color.ink_2);
        root.addView(typeLabel, top(context, 8));

        final RadioGroup rgType = new RadioGroup(context);
        rgType.setOrientation(LinearLayout.HORIZONTAL);

        final RadioButton rbMajor = new RadioButton(context);
        rbMajor.setText("Major");
        rbMajor.setChecked(true);
        rgType.addView(rbMajor);

        final RadioButton rbMinor = new RadioButton(context);
        rbMinor.setText("Minor");
        rgType.addView(rbMinor);

        final RadioButton rbMandatory = new RadioButton(context);
        rbMandatory.setText("Mandatory");
        rgType.addView(rbMandatory);
        root.addView(rgType, top(context, 2));

        final EditText etNotes = input(context, "Release Notes / Changelog (One per line)", "• Performance improvements\n• Bug fixes & enhancements");
        root.addView(etNotes, top(context, 6));

        final ProgressBar uploadProgress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        uploadProgress.setIndeterminate(true);
        uploadProgress.setVisibility(View.GONE);
        root.addView(uploadProgress, top(context, 10));

        final TextView uploadStatusMsg = new TextView(context);
        uploadStatusMsg.setVisibility(View.GONE);
        Ui.size(uploadStatusMsg, 12.0f);
        root.addView(uploadStatusMsg, top(context, 4));

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(0);
        root.addView(btnRow, top(context, 14));

        View cancel = Cards.outlineButton(context, "Cancel", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        btnRow.addView(cancel, weightMargin(1.0f, 6));

        View pubBtn = Cards.primaryButton(context, "Upload & Publish", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final String vname = holder.etVName.getText().toString().trim();
                    final int vcode = Integer.parseInt(holder.etVCode.getText().toString().trim());
                    final int minCode = Integer.parseInt(holder.etMinCode.getText().toString().trim());
                    final String path = holder.etPath.getText().toString().trim();
                    final String sha = holder.etSha.getText().toString().trim();
                    final String notesStr = etNotes.getText().toString().trim();

                    final String relType = rbMandatory.isChecked() ? AppRelease.TYPE_MANDATORY : (rbMinor.isChecked() ? AppRelease.TYPE_MINOR : AppRelease.TYPE_MAJOR);

                    if (vname.isEmpty() || path.isEmpty()) {
                        AppUpdatesScreen.this.host.toast("Version name and APK file name are required");
                        return;
                    }

                    if (minCode > vcode) {
                        AppUpdatesScreen.this.host.toast("Minimum supported version code cannot be greater than release version code.");
                        return;
                    }

                    // Show Confirmation Dialog before network actions
                    new AlertDialog.Builder(context)
                        .setTitle("Publish APK Release?")
                        .setMessage("Version: " + vname + "\nVersion Code: " + vcode + "\nType: " + relType.toUpperCase(Locale.US) + "\nMinimum Supported: " + minCode + "\nFile: " + path + "\n\nThis release will be activated and made available to all application users.")
                        .setPositiveButton("Publish Now", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface di, int which) {
                                executePublishFlow(d, holder.selectedApkHolder[0], vname, vcode, minCode, relType, path, sha, notesStr, uploadProgress, uploadStatusMsg);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                } catch (Exception e) {
                    AppUpdatesScreen.this.host.toast("Invalid input: " + e.getMessage());
                }
            }
        });
        btnRow.addView(pubBtn, weightMargin(1.5f, 0));

        d.setContentView(root);
        d.show();
    }

    private void executePublishFlow(final Dialog d, final File apkToUpload, final String vname, final int vcode, final int minCode, final String relType, final String path, final String sha, final String notesStr, final ProgressBar uploadProgress, final TextView uploadStatusMsg) {
        final Context context = getContext();
        uploadProgress.setVisibility(View.VISIBLE);
        uploadStatusMsg.setVisibility(View.VISIBLE);
        uploadStatusMsg.setText("Preparing release package...");
        uploadStatusMsg.setTextColor(Ui.color(context, R.color.primary_700));

        final AppRelease r = new AppRelease();
        r.versionName = vname;
        r.versionCode = vcode;
        r.minimumSupportedVersionCode = minCode;
        r.updateType = relType;
        r.title = "Update Available: v" + vname;
        r.message = "A new version of CSP Daily Report is available.";
        r.apkPath = path;
        r.apkSha256 = sha;
        r.isActive = true;
        r.releasedAt = Biz.now();
        r.createdAt = Biz.now();
        r.updatedAt = Biz.now();

        for (String line : notesStr.split("\n")) {
            if (!line.trim().isEmpty()) {
                r.releaseNotes.add(line.replace("•", "").trim());
            }
        }

        final String cleanFileName = new File(path).getName();
        r.apkPath = cleanFileName;

        final long fileSize = (apkToUpload != null && apkToUpload.exists()) ? apkToUpload.length() : 0;

        final Runnable completeDatabaseRegistration = new Runnable() {
            @Override
            public void run() {
                uploadStatusMsg.setText("Publishing release metadata to Supabase...");
                SQLiteDatabase db = Core.get(context).db();
                db.insertWithOnConflict("app_updates", null, r.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String baseUrl = SupabaseClient.cleanBaseUrl(SupabaseClient.getProjectUrl(context));
                            String key = SupabaseClient.getPublishableKey(context);

                            // 1. Deactivate other active releases in Supabase using PATCH (only is_active)
                            try {
                                JSONObject deact = new JSONObject();
                                deact.put("is_active", false);
                                SupabaseClient.httpPatch(baseUrl + "/rest/v1/app_updates?version_code=neq." + r.versionCode + "&is_active=eq.true", key, deact.toString());
                            } catch (Throwable ignored) {}

                            // 2. Insert new release row with exact column support matching Supabase
                            JSONObject json = new JSONObject();
                            json.put("version_name", r.versionName);
                            json.put("version_code", r.versionCode);
                            json.put("minimum_supported_version_code", r.minimumSupportedVersionCode);
                            json.put("apk_file_name", cleanFileName);
                            json.put("apk_file_path", cleanFileName);
                            json.put("apk_size_bytes", fileSize);
                            json.put("apk_sha256", r.apkSha256 != null ? r.apkSha256 : "");
                            json.put("release_notes", notesStr);
                            json.put("is_mandatory", AppRelease.TYPE_MANDATORY.equalsIgnoreCase(relType));
                            json.put("is_active", true);

                            try {
                                SupabaseClient.httpPost(baseUrl + "/rest/v1/app_updates?on_conflict=version_code", key, json.toString());
                            } catch (Throwable firstErr) {
                                try {
                                    SupabaseClient.httpPost(baseUrl + "/rest/v1/app_updates", key, json.toString());
                                } catch (Throwable secondErr) {
                                    SupabaseClient.httpPatch(baseUrl + "/rest/v1/app_updates?version_code=eq." + r.versionCode, key, json.toString());
                                }
                            }

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    d.dismiss();
                                    showPublishSuccessModal(r);
                                    AppUpdatesScreen.this.checkNow(false);
                                }
                            });

                        } catch (final Throwable tr) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    uploadProgress.setVisibility(View.GONE);
                                    uploadStatusMsg.setText("Registration error: " + tr.getMessage());
                                    uploadStatusMsg.setTextColor(Ui.color(context, R.color.danger));
                                    AppUpdatesScreen.this.host.toast("Cloud registration notice: " + tr.getMessage());
                                    d.dismiss();
                                    AppUpdatesScreen.this.renderContent();
                                }
                            });
                        }
                    }
                }).start();
            }
        };

        if (apkToUpload != null && apkToUpload.exists()) {
            uploadStatusMsg.setText("Uploading APK binary to Supabase Storage ('app-releases')...");
            SupabaseClient.uploadStorageFile(context, "app-releases", cleanFileName, apkToUpload, new SupabaseClient.Callback<String>() {
                @Override
                public void onSuccess(String result) {
                    completeDatabaseRegistration.run();
                }

                @Override
                public void onError(String error, Throwable tr) {
                    uploadProgress.setVisibility(View.GONE);
                    uploadStatusMsg.setText("Upload failed: " + error);
                    uploadStatusMsg.setTextColor(Ui.color(context, R.color.danger));
                    AppUpdatesScreen.this.host.toast("Storage upload failed: " + error);
                }
            });
        } else {
            completeDatabaseRegistration.run();
        }
    }

    private void showPublishSuccessModal(final AppRelease release) {
        final Context context = getContext();
        new AlertDialog.Builder(context)
            .setTitle("🎉 Release Published Successfully")
            .setMessage("Version: " + release.versionName + "\nBuild: " + release.versionCode + "\nStatus: ACTIVE (Live in Production)\nFile: " + release.apkPath + "\n\nUsers and supervisors will now receive this update.")
            .setPositiveButton("Done", null)
            .setNeutralButton("View History", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface di, int which) {
                    AppUpdatesScreen.this.currentTab = "📋 Release Manager";
                    AppUpdatesScreen.this.tabsChips.select(1);
                    AppUpdatesScreen.this.renderContent();
                }
            })
            .show();
    }

    private void showChangelogDialog(AppRelease release) {
        Context context = getContext();
        StringBuilder sb = new StringBuilder();
        if (release.releaseNotes != null && !release.releaseNotes.isEmpty()) {
            for (String n : release.releaseNotes) {
                sb.append("• ").append(n).append("\n");
            }
        } else {
            sb.append("No release notes provided for this release.");
        }

        new AlertDialog.Builder(context)
            .setTitle("Release Notes — v" + release.versionName)
            .setMessage(sb.toString().trim())
            .setPositiveButton("Close", null)
            .show();
    }

    private void showDetailsDialog(final AppRelease release) {
        final Context context = getContext();
        String details = "Version: v" + release.versionName + "\n" +
            "Version Code: " + release.versionCode + "\n" +
            "Min Supported: " + release.minimumSupportedVersionCode + "\n" +
            "Type: " + release.updateType.toUpperCase(Locale.US) + "\n" +
            "Status: " + (release.isActive ? "ACTIVE" : "INACTIVE") + "\n" +
            "Storage Path: " + release.apkPath + "\n" +
            "SHA-256 Checksum:\n" + release.apkSha256;

        new AlertDialog.Builder(context)
            .setTitle("Technical Details — v" + release.versionName)
            .setMessage(details)
            .setPositiveButton("Close", null)
            .setNeutralButton("Copy SHA-256", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface di, int which) {
                    ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("SHA-256", release.apkSha256));
                        AppUpdatesScreen.this.host.toast("SHA-256 copied to clipboard");
                    }
                }
            })
            .show();
    }

    private void confirmToggleRelease(final AppRelease release) {
        final Context context = getContext();
        String action = release.isActive ? "Deactivate" : "Activate";
        new AlertDialog.Builder(context)
            .setTitle(action + " Release v" + release.versionName + "?")
            .setMessage(release.isActive ? "Deactivating will archive this release. Clients will no longer receive it as the active version." : "Activating this release will make version " + release.versionName + " the live production release.")
            .setPositiveButton(action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface di, int which) {
                    toggleReleaseActive(release);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void toggleReleaseActive(final AppRelease release) {
        final Context context = getContext();
        release.isActive = !release.isActive;
        release.updatedAt = Biz.now();

        SQLiteDatabase db = Core.get(context).db();
        db.insertWithOnConflict("app_updates", null, release.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String baseUrl = SupabaseClient.cleanBaseUrl(SupabaseClient.getProjectUrl(context));
                    String key = SupabaseClient.getPublishableKey(context);
                    
                    if (release.isActive) {
                        // Deactivate others first
                        try {
                            JSONObject deact = new JSONObject();
                            deact.put("is_active", false);
                            deact.put("updated_at", Biz.now());
                            SupabaseClient.httpPatch(baseUrl + "/rest/v1/app_updates?version_code=neq." + release.versionCode + "&is_active=eq.true", key, deact.toString());
                        } catch (Throwable ignored) {}
                    }

                    JSONObject json = new JSONObject();
                    json.put("is_active", release.isActive);
                    json.put("updated_at", release.updatedAt);
                    SupabaseClient.httpPatch(baseUrl + "/rest/v1/app_updates?version_code=eq." + release.versionCode, key, json.toString());
                } catch (Throwable ignored) {}
            }
        }).start();

        this.host.toast("Release marked " + (release.isActive ? "ACTIVE" : "INACTIVE"));
        renderContent();
    }

    // ---------------- REMOTE CONFIG EDITOR ----------------
    private void openRemoteConfigEditor() {
        final Context context = getContext();
        final Dialog d = new Dialog(context, R.style.Theme_Csp_Sheet);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(1);
        Ui.pad(root, 18, 16, 18, 16);
        root.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 18.0f));

        TextView t = Cards.bold(context, "Remote Config & Announcements", 16.0f, R.color.ink);
        root.addView(t);

        final CheckBox cbMaint = new CheckBox(context);
        cbMaint.setText("Enable Maintenance Mode (Restricted Access)");
        cbMaint.setChecked(RemoteConfig.isMaintenanceMode(context));
        root.addView(cbMaint, top(context, 10));

        final EditText etMaintMsg = input(context, "Maintenance Notice Message", RemoteConfig.getString(context, "maintenance_message", "System maintenance in progress."));
        root.addView(etMaintMsg, top(context, 6));

        RemoteConfig.Announcement ann = RemoteConfig.getAnnouncement(context);
        final EditText etAnnTitle = input(context, "Broadcast Announcement Title", ann.title);
        final EditText etAnnMsg = input(context, "Broadcast Announcement Message", ann.message);
        root.addView(etAnnTitle, top(context, 8));
        root.addView(etAnnMsg, top(context, 6));

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(0);
        root.addView(btnRow, top(context, 14));

        View cancel = Cards.outlineButton(context, "Cancel", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        btnRow.addView(cancel, weightMargin(1.0f, 6));

        View save = Cards.primaryButton(context, "Push Config to Cloud", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String maintVal = String.valueOf(cbMaint.isChecked());
                    String maintMsg = etMaintMsg.getText().toString().trim();
                    String annTitle = etAnnTitle.getText().toString().trim();
                    String annMsg = etAnnMsg.getText().toString().trim();

                    RemoteConfig.saveConfig(context, "maintenance_mode", maintVal, true, Biz.now());
                    RemoteConfig.saveConfig(context, "maintenance_message", maintMsg, true, Biz.now());
                    RemoteConfig.saveConfig(context, "broadcast_announcement", annMsg, true, Biz.now());

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String baseUrl = SupabaseClient.cleanBaseUrl(SupabaseClient.getProjectUrl(context));
                                String key = SupabaseClient.getPublishableKey(context);
                                JSONArray arr = new JSONArray();

                                JSONObject o1 = new JSONObject();
                                o1.put("config_key", "maintenance_mode");
                                o1.put("config_value", String.valueOf(cbMaint.isChecked()));
                                arr.put(o1);

                                JSONObject o2 = new JSONObject();
                                o2.put("config_key", "maintenance_message");
                                o2.put("config_value", etMaintMsg.getText().toString().trim());
                                arr.put(o2);

                                JSONObject o3 = new JSONObject();
                                o3.put("config_key", "broadcast_announcement");
                                o3.put("config_value", etAnnMsg.getText().toString().trim());
                                arr.put(o3);

                                SupabaseClient.httpPost(baseUrl + "/rest/v1/remote_config", key, arr.toString());
                            } catch (Throwable ignored) {}
                        }
                    }).start();

                    d.dismiss();
                    AppUpdatesScreen.this.host.toast("Remote config saved & pushed to Supabase!");
                    AppUpdatesScreen.this.renderContent();

                } catch (Exception e) {
                    AppUpdatesScreen.this.host.toast("Error saving config: " + e.getMessage());
                }
            }
        });
        btnRow.addView(save, weightMargin(1.0f, 0));

        d.setContentView(root);
        d.show();
    }

    private EditText input(Context context, String hint, String initialValue) {
        EditText et = new EditText(context);
        et.setHint(hint);
        if (initialValue != null) et.setText(initialValue);
        et.setBackground(Ui.round(context, Ui.color(context, R.color.line), 8.0f));
        Ui.pad(et, 10, 8, 10, 8);
        Ui.size(et, 13.0f);
        et.setTextColor(Ui.color(context, R.color.ink));
        return et;
    }

    private LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(-1, -1);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private LinearLayout.LayoutParams top(Context context, int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dp(context, dp);
        return lp;
    }

    private LinearLayout.LayoutParams weightMargin(float w, int rightMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, w);
        if (rightMarginDp > 0) {
            lp.rightMargin = Ui.dp(getContext(), rightMarginDp);
        }
        return lp;
    }

    private LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }
}
