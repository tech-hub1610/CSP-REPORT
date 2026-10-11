package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.Locale;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Backup;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.Texts;
import org.cspdaily.report.notify.Notifier;
import org.cspdaily.report.notify.Reminders;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Section;
import org.cspdaily.report.util.Files;

import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.User;
import org.cspdaily.report.ui.MainActivity;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.security.BiometricHelper;
import org.cspdaily.report.security.SessionManager;
import org.cspdaily.report.update.UpdateManager;

/* loaded from: classes.dex */
public class MoreScreen extends Screen {
    public MoreScreen(Context context, Screen.Host host) {
        super(context, host);
        title("More");
        subtitle("Settings, data and about");
        showBack(false);
        build(context);
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return "More";
    }

    @Override // org.cspdaily.report.ui.Screen
    protected boolean canGoBack() {
        return false;
    }

    private void build(final Context context) {
        // 1. Account & Session Card
        final User user = Auth.currentUser(context);
        final boolean isSuper = Auth.isSuperAdmin(context);
        if (user != null) {
            LinearLayout accountCard = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
            accountCard.setOrientation(1);
            Ui.pad(accountCard, 14, 12, 14, 12);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(0);
            topRow.setGravity(16);
            accountCard.addView(topRow, mw());

            topRow.addView(new Avatar(context, 40).bind(user.name, user.username), new LinearLayout.LayoutParams(Ui.dp(context, 40.0f), Ui.dp(context, 40.0f)));

            LinearLayout info = new LinearLayout(context);
            info.setOrientation(1);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            infoLp.leftMargin = Ui.dp(context, 10.0f);
            info.setLayoutParams(infoLp);
            topRow.addView(info);

            TextView nameText = new TextView(context);
            nameText.setText(user.name);
            nameText.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(nameText, 15.5f);
            nameText.setTextColor(isSuper ? 0xFF1E1B18 : Ui.color(context, R.color.ink));
            info.addView(nameText);

            TextView sub = new TextView(context);
            sub.setText("@" + user.username + (user.mobile.isEmpty() ? "" : " · " + user.mobile));
            sub.setTextAppearance(context, R.style.Text_Meta);
            sub.setTextColor(isSuper ? 0xFF78716C : Ui.color(context, R.color.ink_3));
            info.addView(sub);

            Badge badge = new Badge(context);
            badge.setKind(user.isSuperAdmin() ? 4 : (user.isAdmin() ? 1 : 0));
            badge.setText(user.roleLabel());
            topRow.addView(badge, wrap());

            View logoutBtn = Cards.link(context, "Switch User / Sign Out", R.color.danger, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).logout();
                    }
                }
            });
            accountCard.addView(logoutBtn, top(context, 8));
            add(accountCard, 0);
        }

        if (Auth.isAdmin(context)) {
            LinearLayout bodyAdmin = new LinearLayout(context);
            bodyAdmin.setOrientation(1);
            if (isSuper) {
                bodyAdmin.addView(row(context, R.drawable.ic_crown, "APK Releases & Remote Config", "Manage APK builds, mandatory updates & cloud feature flags", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MoreScreen.this.host.push(new AppUpdatesScreen(context, MoreScreen.this.host));
                    }
                }));
            }
            bodyAdmin.addView(row(context, R.drawable.ic_shield_check, "Staff & User Credentials", "Roster, roles (Superadmin / Admin / Supervisor) & passwords", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MoreScreen.this.host.push(new SupervisorManageScreen(context, MoreScreen.this.host));
                }
            }));
            bodyAdmin.addView(row(context, R.drawable.ic_trending, "Supervisor Targets Master", "Set daily parameter quotas and targets per supervisor", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MoreScreen.this.host.push(new SupervisorTargetScreen(context, MoreScreen.this.host, UiState.date(context)));
                }
            }));
            bodyAdmin.addView(row(context, R.drawable.ic_note, "Broadcast Bulletins & Notices", "Post priority notices and operational directives to staff", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MoreScreen.this.host.push(new AdminInstructionScreen(context, MoreScreen.this.host, UiState.date(context)));
                }
            }));
            bodyAdmin.addView(row(context, R.drawable.ic_refresh, "Supabase Cloud Central", "Cloud synchronization status, health ping & SQL schema backup", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MoreScreen.this.host.push(new SupabaseSyncScreen(context, MoreScreen.this.host));
                }
            }));
            add(collapsibleCard(context, R.drawable.ic_crown, R.color.primary_600, isSuper ? "👑 Executive Superadmin Commands" : "⚡ Admin & Fleet Operations", "Staff, quotas, directives & cloud central", false, bodyAdmin), 8);
        }

        // Today's Operations Collapsible Card
        LinearLayout bodyOps = new LinearLayout(context);
        bodyOps.setOrientation(1);
        bodyOps.addView(row(context, R.drawable.ic_bank, "Office note", "Text appended to the group message", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.host.push(new OfficeNoteScreen(MoreScreen.this.getContext(), MoreScreen.this.host, UiState.date(MoreScreen.this.getContext())));
            }
        }));
        bodyOps.addView(row(context, R.drawable.ic_trending, "Daily targets", "Set bulk or individual CSP targets for the day", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.host.push(new DailyTargetScreen(MoreScreen.this.getContext(), MoreScreen.this.host, UiState.date(MoreScreen.this.getContext())));
            }
        }));
        bodyOps.addView(row(context, R.drawable.ic_calendar, "Business day", "Working, Sunday, holiday or closure", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.host.push(new BusinessDayScreen(MoreScreen.this.getContext(), MoreScreen.this.host, UiState.date(MoreScreen.this.getContext())));
            }
        }));
        add(collapsibleCard(context, R.drawable.ic_calendar, R.color.primary_600, "Today's Operations", "Office note, daily targets & business day", true, bodyOps), 8);

        // CSP Management Collapsible Card
        LinearLayout bodyCsp = new LinearLayout(context);
        bodyCsp.setOrientation(1);
        bodyCsp.addView(row(context, R.drawable.ic_edit, "Manage CSPs (Edit / Delete)", "Add, edit master details or delete CSPs", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.host.push(new CspManageScreen(MoreScreen.this.getContext(), MoreScreen.this.host));
            }
        }));
        bodyCsp.addView(row(context, R.drawable.ic_add_circle, "Add new CSP", "Register a new banking correspondent", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.host.push(new CspEditScreen(MoreScreen.this.getContext(), MoreScreen.this.host, 0L));
            }
        }));
        bodyCsp.addView(row(context, R.drawable.ic_download, "Import CSPs (CSV)", "Bulk import CSPs from spreadsheet with CSV format", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.host.push(new CspImportScreen(MoreScreen.this.getContext(), MoreScreen.this.host));
            }
        }));
        add(collapsibleCard(context, R.drawable.ic_csp, R.color.primary_600, "CSP Management", "Manage, register & import correspondents", true, bodyCsp), 8);

        // Notifications & Reminders Collapsible Card
        LinearLayout bodyNotif = new LinearLayout(context);
        bodyNotif.setOrientation(1);
        bodyNotif.setPadding(0, 0, 0, 0);
        toggle(bodyNotif, "All reminders", Settings.NOTIF_ENABLED, true);
        toggle(bodyNotif, "Daily report reminder", Settings.NOTIF_PENDING, true);
        timeRow(bodyNotif, "Reminder time", Settings.NOTIF_PENDING_TIME, "16:00");
        toggle(bodyNotif, "Follow-up reminder", Settings.NOTIF_FOLLOWUP, true);
        timeRow(bodyNotif, "Follow-up time", Settings.NOTIF_FOLLOWUP_TIME, "18:00");
        toggle(bodyNotif, "Long pending alert", Settings.NOTIF_LONG_PENDING, true);
        numberRow(bodyNotif, "Long pending after", Settings.NOTIF_LONG_THRESH, "3", "working days");
        toggle(bodyNotif, "100% completion notice", Settings.NOTIF_COMPLETION, true);
        TextView notifFoot = Cards.label(context, "Reminders are scheduled with AlarmManager and fire with no network, no browser and no service worker.", R.style.Text_Meta, R.color.ink_4);
        Ui.pad(notifFoot, 14, 8, 14, 8);
        bodyNotif.addView(notifFoot, mw());
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.permitted(context)) {
            LinearLayout card2 = Cards.card(context);
            card2.setOrientation(0);
            card2.setGravity(16);
            card2.setBackground(Ui.round(context, Ui.color(context, R.color.warn_bg), 14.0f, Ui.color(context, R.color.warn_border), 1.0f));
            Ui.pad(card2, 12, 10, 12, 10);
            card2.addView(Cards.label(context, "Notification permission is off — reminders will not appear. Tap to allow.", R.style.Text_Body, R.color.warn), weight1());
            card2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((Activity) MoreScreen.this.getContext()).requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 41);
                }
            });
            LinearLayout.LayoutParams c2lp = mw();
            c2lp.leftMargin = Ui.dp(context, 14.0f);
            c2lp.rightMargin = Ui.dp(context, 14.0f);
            c2lp.bottomMargin = Ui.dp(context, 10.0f);
            bodyNotif.addView(card2, c2lp);
        }
        add(collapsibleCard(context, R.drawable.ic_bell, R.color.primary_600, "Notifications & Reminders", "Alarm schedules, daily & follow-up alerts", false, bodyNotif), 8);

        // Hourly Reporting Collapsible Card
        LinearLayout bodyHourly = new LinearLayout(context);
        bodyHourly.setOrientation(1);
        bodyHourly.setPadding(0, 0, 0, 0);
        toggle(bodyHourly, "Hourly reporting & target alerts", Settings.HOURLY_ENABLED, false);
        timeRow(bodyHourly, "Window opens", Settings.HOURLY_START, "10:00");
        timeRow(bodyHourly, "Window closes", Settings.HOURLY_END, "18:00");
        TextView hourlyFoot = Cards.label(context, "Sends hourly notifications with parameter-wise target progress and pending CSP count to follow up and take reports.", R.style.Text_Meta, R.color.ink_3);
        Ui.pad(hourlyFoot, 14, 8, 14, 8);
        bodyHourly.addView(hourlyFoot, mw());
        add(collapsibleCard(context, R.drawable.ic_clock, R.color.primary_600, "Hourly Reporting & Targets", "Progress notifications & active window", false, bodyHourly), 8);

        // Security & Session Lock Collapsible Card
        LinearLayout bodySec = new LinearLayout(context);
        bodySec.setOrientation(1);
        bodySec.setPadding(0, 0, 0, 0);

        final boolean hasFpHw = BiometricHelper.isHardwareDetected(context);
        final boolean hasFpEnrolled = BiometricHelper.hasEnrolledFingerprints(context);

        toggle(bodySec, "Device Fingerprint Service", Settings.SECURITY_BIOMETRIC_ENABLED, true);
        toggle(bodySec, "Auto-Lock Session", Settings.SECURITY_LOCK_ENABLED, true);
        lockTimeoutRow(bodySec);

        String fpSummary = BiometricHelper.getBiometricStatusSummary(context);
        TextView fpStatusLabel = Cards.label(context, "Fingerprint: " + fpSummary + " · Auto-lock active on minimize & inactivity", R.style.Text_Meta, (hasFpHw && hasFpEnrolled) ? R.color.primary_700 : R.color.ink_4);
        Ui.pad(fpStatusLabel, 14, 8, 14, 8);
        bodySec.addView(fpStatusLabel, mw());

        if (hasFpHw && !hasFpEnrolled) {
            bodySec.addView(row(context, R.drawable.ic_fingerprint, "Setup Fingerprint on Device", "Register your fingerprint in Android Security Settings", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BiometricHelper.openDeviceSecuritySettings(context);
                }
            }));
        } else if (hasFpHw && hasFpEnrolled) {
            bodySec.addView(row(context, R.drawable.ic_fingerprint, "Test Fingerprint Sensor", "Verify scanner and link fingerprint to this account", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context instanceof Activity) {
                        BiometricHelper.authenticate((Activity) context, "Testing Fingerprint Sensor", new BiometricHelper.Callback() {
                            @Override
                            public void onSuccess() {
                                host.toast("✓ Fingerprint sensor test passed! Ready for 1-touch unlock.");
                            }

                            @Override
                            public void onError(String message) {
                                if (message != null && !message.contains("canceled") && !message.contains("Switched")) {
                                    host.toast("Fingerprint note: " + message);
                                }
                            }

                            @Override
                            public void onFailed() {
                                host.toast("Fingerprint not recognized. Please try again.");
                            }
                        });
                    }
                }
            }));
        }

        bodySec.addView(row(context, R.drawable.ic_lock, "Lock Session Now", "Immediately lock session and require re-authentication", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof MainActivity) {
                    SessionManager.setLocked(true);
                    ((MainActivity) context).showLockScreen();
                }
            }
        }));
        add(collapsibleCard(context, R.drawable.ic_fingerprint, R.color.primary_600, "Security & Session Lock", "Biometrics, auto-lock & session lock", false, bodySec), 8);

        // Data & Backups (or Reports & Safe Sharing for Supervisor)
        final boolean isAdmin = Auth.isAdmin(context);
        LinearLayout bodyData = new LinearLayout(context);
        bodyData.setOrientation(1);

        if (isAdmin) {
            // Admin only features: Full raw database JSON backup, restore & Cloud PostgreSQL sync
            bodyData.addView(row(context, R.drawable.ic_upload, "Export backup", "Whole database as JSON", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MoreScreen.this.exportBackup();
                }
            }));
            bodyData.addView(row(context, R.drawable.ic_download, "Restore backup", "Replace local data from a JSON file", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MoreScreen.this.restoreBackup();
                }
            }));
            bodyData.addView(row(context, R.drawable.ic_upload, "Supabase Cloud Database", "Sync database with Supabase cloud PostgreSQL", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MoreScreen.this.host.push(new SupabaseSyncScreen(context, MoreScreen.this.host));
                }
            }));
        }

        // Operational daily safe reports (available to both Admin and Supervisor)
        bodyData.addView(row(context, R.drawable.ic_pdf, "Export day CSV", "Figures for the selected date", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.exportCsv();
            }
        }));
        bodyData.addView(row(context, R.drawable.ic_share, "Copy group office text", "The exact message format", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoreScreen.this.copyOfficeText();
            }
        }));

        if (!isAdmin) {
            // Note for Supervisor: Explain restricted database controls
            TextView supNotice = Cards.label(context, "🔒 Database backup, restore and cloud database sync are restricted to Administrator to protect data integrity.", R.style.Text_Meta, R.color.ink_3);
            supNotice.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), 10.0f, Ui.color(context, R.color.line), 1.0f));
            Ui.pad(supNotice, 12, 8, 12, 8);
            LinearLayout.LayoutParams snlp = mw();
            snlp.leftMargin = Ui.dp(context, 12.0f);
            snlp.rightMargin = Ui.dp(context, 12.0f);
            snlp.topMargin = Ui.dp(context, 6.0f);
            snlp.bottomMargin = Ui.dp(context, 8.0f);
            bodyData.addView(supNotice, snlp);
        }

        add(collapsibleCard(context, isAdmin ? R.drawable.ic_upload : R.drawable.ic_pdf, R.color.primary_600, isAdmin ? "Data & Database Backups" : "Reports & Safe Sharing", isAdmin ? "Full database backup, restore, cloud sync & CSV" : "Export day CSV & copy group text", false, bodyData), 8);

        // About & Updates Collapsible Card
        LinearLayout bodyAbout = new LinearLayout(context);
        bodyAbout.setOrientation(1);
        Ui.pad(bodyAbout, 14, 10, 14, 12);
        bodyAbout.addView(Cards.label(context, "CSP Daily Report — Native Android", R.style.Text_CardTitle, R.color.ink), mw());
        bodyAbout.addView(Cards.label(context, "Standalone native build with SQLite offline persistence & Supabase cloud sync.", R.style.Text_Meta, R.color.ink_3), top(context, 4));
        bodyAbout.addView(Cards.label(context, "Installed: v" + version(context) + " (Build " + UpdateManager.getInstalledVersionCode(context) + ") · Asia/Kolkata", R.style.Text_Nano, R.color.ink_4), top(context, 8));

        View updateRow = row(context, R.drawable.ic_upload, "Check for Updates", "Verify latest releases and remote config", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoreScreen.this.host.push(new AppUpdatesScreen(context, MoreScreen.this.host));
            }
        });
        bodyAbout.addView(updateRow, top(context, 10));
        add(collapsibleCard(context, R.drawable.ic_shield_check, R.color.primary_600, "About & Updates", "v" + version(context) + " · Build " + UpdateManager.getInstalledVersionCode(context), false, bodyAbout), 8);
    }

    private String version(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    private View collapsibleCard(final Context context, int iconRes, int iconTint, String title, String subtitle, boolean defaultExpanded, final View body) {
        LinearLayout outer = Cards.card(context);
        outer.setOrientation(1);
        outer.setPadding(0, 0, 0, 0);

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(0);
        headerRow.setGravity(16);
        headerRow.setBackground(Ui.pressable(context, Ui.color(context, R.color.paper), Ui.rdp(context, R.dimen.r_md), false));
        headerRow.setClickable(true);
        Ui.pad(headerRow, 14, 12, 14, 12);

        ImageView iconView = new ImageView(context);
        int iconSize = Ui.dp(context, 20.0f);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.rightMargin = Ui.dp(context, 12.0f);
        iconView.setLayoutParams(iconLp);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(Ui.color(context, iconTint));
        headerRow.addView(iconView);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(1);
        headerRow.addView(textCol, weight1());

        TextView titleView = new TextView(context);
        titleView.setText(title);
        Ui.size(titleView, 14.5f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Ui.color(context, R.color.ink));
        textCol.addView(titleView, mw());

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subView = Cards.label(context, subtitle, R.style.Text_Meta, R.color.ink_3);
            textCol.addView(subView, mw());
        }

        final ImageView chevron = new ImageView(context);
        int chevSize = Ui.dp(context, 18.0f);
        chevron.setLayoutParams(new LinearLayout.LayoutParams(chevSize, chevSize));
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(Ui.color(context, R.color.ink_3));
        chevron.setRotation(defaultExpanded ? 90.0f : 0.0f);
        headerRow.addView(chevron);

        outer.addView(headerRow, mw());

        final View divider = new View(context);
        divider.setBackgroundColor(Ui.color(context, R.color.line));
        divider.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f)));
        divider.setVisibility(defaultExpanded ? View.VISIBLE : View.GONE);
        outer.addView(divider);

        body.setVisibility(defaultExpanded ? View.VISIBLE : View.GONE);
        outer.addView(body, mw());

        final boolean[] expanded = new boolean[]{defaultExpanded};
        headerRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expanded[0] = !expanded[0];
                body.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
                divider.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
                chevron.animate().rotation(expanded[0] ? 90.0f : 0.0f).setDuration(180).start();
            }
        });

        return outer;
    }

    private View row(Context context, int i, int i2, String str, String str2, View.OnClickListener onClickListener) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackground(Ui.pressable(context, Ui.color(context, R.color.paper), Ui.rdp(context, R.dimen.r_md), false));
        Ui.pad(linearLayout, 14, 13, 14, 13);
        linearLayout.setClickable(true);
        linearLayout.setMinimumHeight(Ui.dp(context, 52.0f));
        linearLayout.setOnClickListener(onClickListener);
        ImageView imageView = new ImageView(context);
        int dp = Ui.dp(context, 20.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp, dp);
        layoutParams.rightMargin = Ui.dp(context, 14.0f);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(i);
        imageView.setColorFilter(Ui.color(context, i2));
        linearLayout.addView(imageView);
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(1);
        linearLayout.addView(linearLayout2, weight1());
        TextView textView = new TextView(context);
        textView.setText(str);
        Ui.size(textView, 14.5f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, R.color.ink));
        linearLayout2.addView(textView, mw());
        linearLayout2.addView(Cards.label(context, str2, R.style.Text_Meta, R.color.ink_3), mw());
        ImageView imageView2 = new ImageView(context);
        int dp2 = Ui.dp(context, 18.0f);
        imageView2.setLayoutParams(new LinearLayout.LayoutParams(dp2, dp2));
        imageView2.setImageResource(R.drawable.ic_chevron_right);
        imageView2.setColorFilter(Ui.color(context, R.color.line_3));
        linearLayout.addView(imageView2);
        return linearLayout;
    }

    private View row(Context context, int i, String str, String str2, View.OnClickListener onClickListener) {
        return row(context, i, R.color.primary_600, str, str2, onClickListener);
    }

    private View toggle(LinearLayout linearLayout, String str, final String str2, boolean z) {
        final Context context = linearLayout.getContext();
        SQLiteDatabase db = Core.get(context).db();
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        Ui.pad(linearLayout2, 14, 10, 14, 10);
        linearLayout2.addView(Cards.label(context, str, R.style.Text_Body, R.color.ink), weight1());
        Switch r3 = new Switch(context);
        r3.setChecked(Settings.flag(db, str2, z));
        r3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: org.cspdaily.report.ui.screen.MoreScreen.8
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton compoundButton, boolean z2) {
                Settings.set(Core.get(context).db(), str2, z2 ? "1" : "0");
                if (Settings.SECURITY_BIOMETRIC_ENABLED.equals(str2) && z2) {
                    Settings.set(Core.get(context).db(), Settings.SECURITY_LOCK_ENABLED, "1");
                }
                Reminders.schedule(context);
            }
        });
        linearLayout2.addView(r3, wrap());
        linearLayout.addView(linearLayout2, mw());
        View view = new View(context);
        view.setBackgroundColor(Ui.color(context, R.color.line));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        layoutParams.leftMargin = Ui.dp(context, 14.0f);
        view.setLayoutParams(layoutParams);
        linearLayout.addView(view, mw());
        return linearLayout2;
    }

    private View lockTimeoutRow(LinearLayout parent) {
        final Context context = parent.getContext();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(16);
        Ui.pad(row, 14, 10, 14, 10);
        row.addView(Cards.label(context, "Lock when app minimized", R.style.Text_Body, R.color.ink), weight1());

        final String[] labels = new String[]{"Immediately (Default)", "After 1 minute", "After 5 minutes", "After 10 minutes"};
        final int[] values = new int[]{0, 60, 300, 600};

        int currentSec = SessionManager.getLockTimeoutSec(Core.get(context).dbRead());
        int selectedIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentSec) {
                selectedIdx = i;
                break;
            }
        }

        final TextView valText = Cards.bold(context, labels[selectedIdx], 14.0f, R.color.primary_700);
        row.addView(valText, wrap());

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cur = SessionManager.getLockTimeoutSec(Core.get(context).dbRead());
                int curIdx = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == cur) {
                        curIdx = i;
                        break;
                    }
                }

                new AlertDialog.Builder(context)
                    .setTitle("Lock when app minimized")
                    .setSingleChoiceItems(labels, curIdx, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int chosenSec = values[which];
                            Settings.set(Core.get(context).db(), Settings.SECURITY_LOCK_TIMEOUT, String.valueOf(chosenSec));
                            valText.setText(labels[which]);
                            dialog.dismiss();
                            host.toast("Lock timeout set: " + labels[which]);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        parent.addView(row, mw());
        View div = new View(context);
        div.setBackgroundColor(Ui.color(context, R.color.line));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        lp.leftMargin = Ui.dp(context, 14.0f);
        div.setLayoutParams(lp);
        parent.addView(div, mw());
        return row;
    }

    private View timeRow(LinearLayout linearLayout, String str, final String str2, final String str3) {
        final Context context = linearLayout.getContext();
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        Ui.pad(linearLayout2, 14, 10, 14, 10);
        linearLayout2.addView(Cards.label(context, str, R.style.Text_Body, R.color.ink), weight1());
        final TextView bold = Cards.bold(context, Settings.get(Core.get(context).dbRead(), str2, str3), 14.0f, R.color.primary_700);
        linearLayout2.addView(bold, wrap());
        linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.MoreScreen.9
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                int[] timeOfDay = Settings.timeOfDay(Core.get(context).dbRead(), str2, str3);
                new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() { // from class: org.cspdaily.report.ui.screen.MoreScreen.9.1
                    @Override // android.app.TimePickerDialog.OnTimeSetListener
                    public void onTimeSet(TimePicker timePicker, int i, int i2) {
                        String format = String.format(Locale.US, "%02d:%02d", Integer.valueOf(i), Integer.valueOf(i2));
                        Settings.set(Core.get(context).db(), str2, format);
                        bold.setText(format);
                        Reminders.schedule(context);
                    }
                }, timeOfDay[0], timeOfDay[1], true).show();
            }
        });
        linearLayout.addView(linearLayout2, mw());
        View view = new View(context);
        view.setBackgroundColor(Ui.color(context, R.color.line));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        layoutParams.leftMargin = Ui.dp(context, 14.0f);
        view.setLayoutParams(layoutParams);
        linearLayout.addView(view, mw());
        return linearLayout2;
    }

    private View numberRow(LinearLayout linearLayout, final String str, final String str2, final String str3, final String str4) {
        Context context = linearLayout.getContext();
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        Ui.pad(linearLayout2, 14, 10, 14, 10);
        linearLayout2.addView(Cards.label(context, str, R.style.Text_Body, R.color.ink), weight1());
        final TextView bold = Cards.bold(context, Settings.get(Core.get(context).dbRead(), str2, str3) + " " + str4, 14.0f, R.color.primary_700);
        linearLayout2.addView(bold, wrap());
        linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.MoreScreen.10
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MoreScreen.this.pickNumber(str, str2, str3, str4, bold);
            }
        });
        linearLayout.addView(linearLayout2, mw());
        View view = new View(context);
        view.setBackgroundColor(Ui.color(context, R.color.line));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        layoutParams.leftMargin = Ui.dp(context, 14.0f);
        view.setLayoutParams(layoutParams);
        linearLayout.addView(view, mw());
        return linearLayout2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pickNumber(String str, final String str2, String str3, final String str4, final TextView textView) {
        final Context context = getContext();
        final EditText editText = new EditText(context);
        editText.setInputType(2);
        editText.setText(Settings.get(Core.get(context).dbRead(), str2, str3));
        editText.setSelection(editText.getText().length());
        Ui.pad(editText, 24, 6, 24, 6);
        new AlertDialog.Builder(context).setTitle(str).setView(editText).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).setPositiveButton("Save", new DialogInterface.OnClickListener() { // from class: org.cspdaily.report.ui.screen.MoreScreen.11
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    int parseInt = Integer.parseInt(editText.getText().toString().trim());
                    if (parseInt < 1) {
                        MoreScreen.this.host.toast("Must be at least 1");
                        return;
                    }
                    Settings.set(Core.get(context).db(), str2, Integer.toString(parseInt));
                    textView.setText(parseInt + " " + str4);
                    Reminders.schedule(context);
                } catch (NumberFormatException e) {
                    MoreScreen.this.host.toast("Enter a whole number");
                }
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exportBackup() {
        try {
            Files.saveText((Activity) getContext(), "csp-daily-backup-" + Biz.today() + ".json", "application/json", Backup.exportJson(Core.get(getContext()).dbRead()));
        } catch (Exception e) {
            this.host.toast("Backup failed: " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreBackup() {
        Files.pickJson((Activity) getContext());
        this.host.toast("Pick the backup file to restore");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exportCsv() {
        try {
            Files.saveText((Activity) getContext(), "csp-day-" + UiState.date(getContext()) + ".csv", "text/csv", Texts.dayCsv(Core.get(getContext()).dbRead(), UiState.date(getContext())));
        } catch (Exception e) {
            this.host.toast("Export failed: " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void copyOfficeText() {
        Context context = getContext();
        String officeText = Texts.officeText(Core.get(context).dbRead(), UiState.date(context));
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService("clipboard");
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("csp", officeText));
            this.host.toast("Office text copied");
        }
    }

    private View optionRow(LinearLayout linearLayout, final String title, final String settingKey, final String defaultVal, final String[] labels, final String[] values) {
        final Context context = linearLayout.getContext();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(16);
        Ui.pad(row, 14, 10, 14, 10);
        row.addView(Cards.label(context, title, R.style.Text_Body, R.color.ink), weight1());

        String currentVal = Settings.get(Core.get(context).dbRead(), settingKey, defaultVal);
        String displayLabel = labels[0];
        for (int k = 0; k < values.length; k++) {
            if (values[k].equals(currentVal)) {
                displayLabel = labels[k];
                break;
            }
        }

        final TextView bold = Cards.bold(context, displayLabel, 14.0f, R.color.primary_700);
        row.addView(bold, wrap());

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cur = Settings.get(Core.get(context).dbRead(), settingKey, defaultVal);
                int checkedItem = 0;
                for (int k = 0; k < values.length; k++) {
                    if (values[k].equals(cur)) {
                        checkedItem = k;
                        break;
                    }
                }
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setSingleChoiceItems(labels, checkedItem, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Settings.set(Core.get(context).db(), settingKey, values[which]);
                                bold.setText(labels[which]);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        linearLayout.addView(row, mw());
        View divider = new View(context);
        divider.setBackgroundColor(Ui.color(context, R.color.line));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        lp.leftMargin = Ui.dp(context, 14.0f);
        divider.setLayoutParams(lp);
        linearLayout.addView(divider, mw());
        return row;
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }

    private static LinearLayout.LayoutParams top(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }
}
