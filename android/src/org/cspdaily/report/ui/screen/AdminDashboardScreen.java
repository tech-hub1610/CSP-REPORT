package org.cspdaily.report.ui.screen;

import android.app.DatePickerDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Journal;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.DaySummary;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.util.Contacts;
import org.cspdaily.report.util.PhoneActions;

public class AdminDashboardScreen extends Screen {
    private static final String[] MAIN_TABS = {
        "👔 Supervisors",
        "📞 Live Calls & Logs",
        "📊 Reports Ledger",
        "⚠️ Pending CSPs",
        "⚡ Audit Stream"
    };

    private String date;
    private String activeTab = MAIN_TABS[0];
    private Long filterSupervisorId = null;
    private String query = "";

    // Header & Command views
    private final LinearLayout commandDeck;
    private final LinearLayout quickRibbon;
    private final Chips viewChips;
    private final LinearLayout supervisorFilterBar;
    private final LinearLayout searchContainer;
    private EditText searchEdit;
    private final LinearLayout contentArea;
    private final Empty empty;

    public AdminDashboardScreen(final Context context, final Screen.Host host) {
        super(context, host);
        this.date = UiState.date(context);

        User currentUser = Auth.currentUser(context);
        boolean isSuper = currentUser != null && currentUser.isSuperAdmin();

        title(isSuper ? "Executive Command Deck" : "Admin Command Center");
        subtitle("Network Oversight, Supervisor Logs & Live Sync");
        showBack(false);

        headerAction("Sync Cloud", new Runnable() {
            @Override
            public void run() {
                host.toast("Syncing with Supabase Cloud...");
                org.cspdaily.report.data.SupabaseClient.syncAll(AdminDashboardScreen.this.getContext(), new org.cspdaily.report.data.SupabaseClient.Callback<org.cspdaily.report.data.SupabaseClient.SyncResult>() {
                    @Override
                    public void onSuccess(org.cspdaily.report.data.SupabaseClient.SyncResult res) {
                        AdminDashboardScreen.this.host.toast("Cloud synced: " + (res.pulledCount + res.pushedCount) + " updated");
                        AdminDashboardScreen.this.refreshContent();
                    }
                    @Override
                    public void onError(String msg, Throwable t) {
                        AdminDashboardScreen.this.host.toast("Sync error: " + msg);
                    }
                });
            }
        });

        // 1. Executive Master Command Deck
        this.commandDeck = new LinearLayout(context);
        this.commandDeck.setOrientation(1);
        this.commandDeck.setBackground(buildDeckBackground(context));
        Ui.pad(this.commandDeck, 16, 16, 16, 16);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            this.commandDeck.setElevation(Ui.px(context, R.dimen.el_4));
        }
        add(this.commandDeck, 0);

        // 2. Administrative Quick Ribbon Dock
        this.quickRibbon = new LinearLayout(context);
        this.quickRibbon.setOrientation(1);
        add(this.quickRibbon, 10);

        // 3. View Tabs Segmented Selector
        this.viewChips = new Chips(context);
        this.viewChips.set(MAIN_TABS);
        this.viewChips.allowNone(false);
        this.viewChips.select(0);
        this.viewChips.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                AdminDashboardScreen.this.activeTab = str;
                AdminDashboardScreen.this.renderContentArea();
            }
        });
        add(this.viewChips, 10);

        // 4. Supervisor Filter Bar
        this.supervisorFilterBar = new LinearLayout(context);
        this.supervisorFilterBar.setOrientation(0);
        add(this.supervisorFilterBar, 6);

        // 5. Search Bar
        this.searchContainer = buildSearchBar(context);
        add(this.searchContainer, 6);

        // 6. Dynamic Content Container
        this.contentArea = new LinearLayout(context);
        this.contentArea.setOrientation(1);
        add(this.contentArea, 6);

        this.empty = new Empty(context);
        add(this.empty, 0);

        buildQuickRibbon(context, host);
        refreshContent();
    }

    private GradientDrawable buildDeckBackground(Context context) {
        User currentUser = Auth.currentUser(context);
        boolean isSuper = currentUser != null && currentUser.isSuperAdmin();
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(Ui.dp(context, 22.0f));
        if (isSuper) {
            gd.setColors(new int[]{0xFFFFFFFF, 0xFFFFF8ED, 0xFFFEF3C7});
            gd.setStroke(Ui.dp(context, 1.5f), 0xCCF59E0B);
        } else {
            gd.setColors(new int[]{0xFF0B132B, 0xFF1C2541});
            gd.setStroke(Ui.dp(context, 1.0f), 0x3338BDF8);
        }
        gd.setOrientation(GradientDrawable.Orientation.TL_BR);
        return gd;
    }

    @Override
    public String headline() {
        User currentUser = Auth.currentUser(getContext());
        return (currentUser != null && currentUser.isSuperAdmin()) ? "Executive Superadmin Cockpit" : "Admin Command Deck";
    }

    @Override
    protected boolean canGoBack() {
        return false;
    }

    private void paintCommandDeckHeader(final Context context, DaySummary summary, List<SupervisorMap.SupervisorStats> statsList) {
        this.commandDeck.removeAllViews();
        this.commandDeck.setBackground(buildDeckBackground(context));

        User currentUser = Auth.currentUser(context);
        boolean isSuper = currentUser != null && currentUser.isSuperAdmin();

        // 1. Top Identity Row
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        this.commandDeck.addView(topRow, mw());

        // Glowing Command Icon
        FrameLayout iconBadge = new FrameLayout(context);
        int badgeBg = isSuper ? 0x26F59E0B : 0x2638BDF8;
        int badgeBorder = isSuper ? 0x80F59E0B : 0x6638BDF8;
        iconBadge.setBackground(Ui.round(context, badgeBg, 12.0f, badgeBorder, 1.0f));
        int badgeSize = Ui.dp(context, 40.0f);
        topRow.addView(iconBadge, new LinearLayout.LayoutParams(badgeSize, badgeSize));

        ImageView cmdIcon = new ImageView(context);
        cmdIcon.setImageResource(isSuper ? R.drawable.ic_crown : R.drawable.ic_shield_check);
        cmdIcon.setColorFilter(isSuper ? 0xFFD97706 : 0xFF38BDF8);
        int iconPad = Ui.dp(context, 7.0f);
        cmdIcon.setPadding(iconPad, iconPad, iconPad, iconPad);
        iconBadge.addView(cmdIcon, match());

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(1);
        LinearLayout.LayoutParams tclp = weight1();
        tclp.leftMargin = Ui.dp(context, 10.0f);
        titleCol.setLayoutParams(tclp);
        topRow.addView(titleCol);

        TextView appTitle = new TextView(context);
        appTitle.setText(isSuper ? "👑 SUPERADMIN EXECUTIVE COCKPIT" : "ADMIN EXECUTIVE HUB");
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(appTitle, 14.5f);
        appTitle.setTextColor(isSuper ? 0xFF1E1B18 : 0xFFF8FAFC);
        titleCol.addView(appTitle);

        TextView sub = new TextView(context);
        if (isSuper && currentUser != null) {
            sub.setText("Master Control · @" + currentUser.username + " (" + currentUser.name + ")");
        } else {
            sub.setText("Supervisors · Live Calls · Reports Ledger");
        }
        Ui.size(sub, 11.0f);
        sub.setTextColor(isSuper ? 0xFF78716C : 0xFF94A3B8);
        titleCol.addView(sub);

        // Live Real-Time Pulse Pill
        LinearLayout pulsePill = new LinearLayout(context);
        pulsePill.setOrientation(0);
        pulsePill.setGravity(Gravity.CENTER_VERTICAL);
        boolean isSync = org.cspdaily.report.data.SupabaseClient.isSyncing();
        int pillBg = isSync ? 0x330284C7 : (summary.isComplete() ? 0x2610B981 : 0x2638BDF8);
        int pillBorder = isSync ? 0xFF0284C7 : (summary.isComplete() ? 0xFF10B981 : 0xFF38BDF8);
        pulsePill.setBackground(Ui.round(context, pillBg, 14.0f, pillBorder, 1.0f));
        pulsePill.setPadding(Ui.dp(context, 10.0f), Ui.dp(context, 5.0f), Ui.dp(context, 10.0f), Ui.dp(context, 5.0f));
        pulsePill.setClickable(true);
        pulsePill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdminDashboardScreen.this.host.refresh();
            }
        });

        TextView pulseDot = new TextView(context);
        pulseDot.setText(isSync ? "⟳ " : "● ");
        Ui.size(pulseDot, 12.0f);
        pulseDot.setTextColor(isSync ? 0xFF38BDF8 : (summary.isComplete() ? 0xFF34D399 : 0xFF38BDF8));
        pulsePill.addView(pulseDot);

        TextView pulseText = new TextView(context);
        pulseText.setText(isSync ? "Syncing..." : (summary.isComplete() ? "Completed" : "Live Cloud"));
        pulseText.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(pulseText, 11.0f);
        pulseText.setTextColor(isSync ? 0xFF38BDF8 : (summary.isComplete() ? 0xFF34D399 : 0xFFF8FAFC));
        pulsePill.addView(pulseText);

        topRow.addView(pulsePill, wrap());

        // Announcement / Maintenance Banners
        org.cspdaily.report.update.RemoteConfig.Announcement ann = org.cspdaily.report.update.RemoteConfig.getAnnouncement(context);
        if (ann != null && ann.hasContent()) {
            LinearLayout annCard = new LinearLayout(context);
            annCard.setOrientation(1);
            int bg = "alert".equalsIgnoreCase(ann.severity) ? 0x33EF4444 : ("warn".equalsIgnoreCase(ann.severity) ? 0x33F59E0B : 0x3338BDF8);
            int border = "alert".equalsIgnoreCase(ann.severity) ? 0xFFEF4444 : ("warn".equalsIgnoreCase(ann.severity) ? 0xFFF59E0B : 0xFF38BDF8);
            annCard.setBackground(Ui.round(context, bg, 10.0f, border, 1.0f));
            Ui.pad(annCard, 10, 8, 10, 8);
            LinearLayout.LayoutParams alp = mw();
            alp.topMargin = Ui.dp(context, 10.0f);
            annCard.setLayoutParams(alp);

            TextView at = new TextView(context);
            at.setText("📢 " + (ann.title.isEmpty() ? "Announcement" : ann.title) + ": " + ann.message);
            Ui.size(at, 12.0f);
            at.setTextColor(0xFFF8FAFC);
            annCard.addView(at);
            this.commandDeck.addView(annCard);
        }

        if (org.cspdaily.report.update.RemoteConfig.isMaintenanceMode(context)) {
            LinearLayout mCard = new LinearLayout(context);
            mCard.setOrientation(1);
            mCard.setBackground(Ui.round(context, 0x44EF4444, 10.0f, 0xFFEF4444, 1.0f));
            Ui.pad(mCard, 10, 8, 10, 8);
            LinearLayout.LayoutParams mlp = mw();
            mlp.topMargin = Ui.dp(context, 10.0f);
            mCard.setLayoutParams(mlp);

            TextView mt = new TextView(context);
            mt.setText("⚠️ MAINTENANCE MODE: " + org.cspdaily.report.update.RemoteConfig.getMaintenanceMessage(context));
            mt.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(mt, 12.0f);
            mt.setTextColor(0xFFFCA5A5);
            mCard.addView(mt);
            this.commandDeck.addView(mCard);
        }

        // 2. Interactive Glassmorphic Date Navigation Bar
        LinearLayout dateBar = new LinearLayout(context);
        dateBar.setOrientation(0);
        dateBar.setGravity(Gravity.CENTER_VERTICAL);
        dateBar.setBackground(Ui.round(context, isSuper ? 0xF2FDFBF7 : 0x1AFFFFFF, 14.0f, isSuper ? 0x66F59E0B : 0x33FFFFFF, 1.0f));
        Ui.pad(dateBar, 6, 4, 6, 4);
        LinearLayout.LayoutParams dblp = mw();
        dblp.topMargin = Ui.dp(context, 12.0f);
        dateBar.setLayoutParams(dblp);
        this.commandDeck.addView(dateBar);

        ImageView prevBtn = edgeBtn(context, R.drawable.ic_arrow_back, isSuper, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdminDashboardScreen.this.step(-1);
            }
        });
        dateBar.addView(prevBtn);

        TextView dateTxt = new TextView(context);
        dateTxt.setGravity(Gravity.CENTER);
        dateTxt.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(dateTxt, 13.5f);
        dateTxt.setTextColor(isSuper ? 0xFF1E1B18 : 0xFFF8FAFC);
        dateTxt.setText(Biz.fmtDmy(this.date) + (Biz.isToday(this.date) ? " (Today)" : "  ▾"));
        dateTxt.setBackground(Ui.pressable(context, isSuper ? 0x1A000000 : 0x22FFFFFF, 10.0f, true));
        Ui.pad(dateTxt, 6, 4, 6, 4);
        dateTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdminDashboardScreen.this.pickDate();
            }
        });
        dateBar.addView(dateTxt, weight1());

        if (!Biz.isToday(this.date)) {
            TextView todayBtn = new TextView(context);
            todayBtn.setText("Today ↺");
            Ui.size(todayBtn, 11.5f);
            todayBtn.setTypeface(Typeface.DEFAULT_BOLD);
            todayBtn.setTextColor(0xFF0F172A);
            todayBtn.setBackground(Ui.round(context, 0xFFFCD34D, 11.0f, 0xFFFFFFFF, 1.0f));
            Ui.pad(todayBtn, 8, 3, 8, 3);
            todayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdminDashboardScreen.this.date = Biz.today();
                    UiState.setDate(context, AdminDashboardScreen.this.date);
                    AdminDashboardScreen.this.refreshContent();
                }
            });
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(-2, -2);
            tLp.rightMargin = Ui.dp(context, 6.0f);
            dateBar.addView(todayBtn, tLp);
        }

        ImageView nextBtn = edgeBtn(context, R.drawable.ic_chevron_right, isSuper, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdminDashboardScreen.this.step(1);
            }
        });
        dateBar.addView(nextBtn);

        // 3. Superadmin Executive Bento KPI Grid (6 Cards in 2 Rows)
        int totalCsps = summary.total;
        int totalReported = summary.reported;
        int totalCalls = 0;
        int totalBusiness = summary.totalBusiness;
        int activeSupsCount = 0;

        for (SupervisorMap.SupervisorStats st : statsList) {
            totalCalls += st.totalCalls;
            if (st.reportedCsps > 0 || st.totalCalls > 0) activeSupsCount++;
        }

        int pct = totalCsps > 0 ? (totalReported * 100 / totalCsps) : 0;
        int pendingCount = Math.max(0, totalCsps - totalReported);

        // Row 1: Operations Velocity
        LinearLayout kpiRow1 = new LinearLayout(context);
        kpiRow1.setOrientation(0);
        kpiRow1.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams kpilp1 = mw();
        kpilp1.topMargin = Ui.dp(context, 12.0f);
        kpiRow1.setLayoutParams(kpilp1);
        this.commandDeck.addView(kpiRow1);

        kpiRow1.addView(bentoKpiTile(context, "FILING RATE", pct + "%", totalReported + "/" + totalCsps + " Filed", 0xFF059669, isSuper ? 0x2210B981 : 0x2210B981, isSuper), weight1());
        kpiRow1.addView(kpiSpacer(context));
        kpiRow1.addView(bentoKpiTile(context, "ACTIVE FLEET", activeSupsCount + "/" + statsList.size(), "Supervisors", 0xFF0284C7, isSuper ? 0x2238BDF8 : 0x2238BDF8, isSuper), weight1());
        kpiRow1.addView(kpiSpacer(context));
        kpiRow1.addView(bentoKpiTile(context, "CALLS LOGGED", String.valueOf(totalCalls), "Follow-ups", 0xFFD97706, isSuper ? 0x22F59E0B : 0x22F59E0B, isSuper), weight1());

        // Row 2: Enterprise Telemetry & Escalate
        LinearLayout kpiRow2 = new LinearLayout(context);
        kpiRow2.setOrientation(0);
        kpiRow2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams kpilp2 = mw();
        kpilp2.topMargin = Ui.dp(context, 6.0f);
        kpiRow2.setLayoutParams(kpilp2);
        this.commandDeck.addView(kpiRow2);

        kpiRow2.addView(bentoKpiTile(context, "BUSINESS", String.valueOf(totalBusiness), "Total Enrolled", 0xFF9333EA, isSuper ? 0x22A855F7 : 0x22A855F7, isSuper), weight1());
        kpiRow2.addView(kpiSpacer(context));
        kpiRow2.addView(bentoKpiTile(context, "PENDING RADAR", String.valueOf(pendingCount), "Unfiled CSPs", pendingCount > 0 ? 0xFFDC2626 : 0xFF059669, pendingCount > 0 ? 0x22F43F5E : 0x2210B981, isSuper), weight1());
        kpiRow2.addView(kpiSpacer(context));
        String installedV = org.cspdaily.report.update.UpdateManager.getInstalledVersionName(context);
        kpiRow2.addView(bentoKpiTile(context, "FLEET BUILD", "v" + installedV, "Supabase Online", 0xFF4F46E5, isSuper ? 0x22818CF8 : 0x22818CF8, isSuper), weight1());

        // 4. Progress Bar Gauge
        LinearLayout progressContainer = new LinearLayout(context);
        progressContainer.setOrientation(1);
        LinearLayout.LayoutParams pclp = mw();
        pclp.topMargin = Ui.dp(context, 10.0f);
        progressContainer.setLayoutParams(pclp);
        this.commandDeck.addView(progressContainer);

        FrameLayout barTrack = new FrameLayout(context);
        barTrack.setBackground(Ui.round(context, isSuper ? 0x33F59E0B : 0x33FFFFFF, 4.0f));
        int barH = Ui.dp(context, 6.0f);
        progressContainer.addView(barTrack, new LinearLayout.LayoutParams(-1, barH));

        if (pct > 0) {
            View barFill = new View(context);
            barFill.setBackground(Ui.round(context, pct >= 100 ? 0xFF059669 : 0xFF0284C7, 4.0f));
            int fillW = pct >= 100 ? -1 : 0;
            LinearLayout.LayoutParams bflp = new LinearLayout.LayoutParams(fillW, barH);
            if (pct < 100) {
                bflp.width = 0;
                bflp.weight = pct;
                LinearLayout flexRow = new LinearLayout(context);
                flexRow.setOrientation(0);
                flexRow.addView(barFill, bflp);
                flexRow.addView(new View(context), new LinearLayout.LayoutParams(0, barH, 100 - pct));
                barTrack.addView(flexRow, match());
            } else {
                barTrack.addView(barFill, match());
            }
        }
    }

    private View kpiSpacer(Context context) {
        View v = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(context, 6.0f), 1);
        v.setLayoutParams(lp);
        return v;
    }

    private LinearLayout bentoKpiTile(Context context, String label, String value, String sub, int accentColor, int bgTint, boolean isSuper) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(1);
        col.setGravity(Gravity.CENTER);
        col.setBackground(Ui.round(context, isSuper ? 0xF0FFFFFF : bgTint, 12.0f, accentColor, 1.0f));
        Ui.pad(col, 6, 7, 6, 7);

        TextView lbl = new TextView(context);
        lbl.setText(label);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(lbl, 8.5f);
        lbl.setTextColor(isSuper ? 0xFF78716C : 0xFFCBD5E1);
        lbl.setGravity(Gravity.CENTER);
        lbl.setSingleLine(true);
        col.addView(lbl, mw());

        TextView val = new TextView(context);
        val.setText(value);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(val, 15.0f);
        val.setTextColor(accentColor);
        val.setGravity(Gravity.CENTER);
        val.setSingleLine(true);
        col.addView(val, top(context, 2));

        TextView s = new TextView(context);
        s.setText(sub);
        Ui.size(s, 9.0f);
        s.setTextColor(isSuper ? 0xFF78716C : 0xFF94A3B8);
        s.setGravity(Gravity.CENTER);
        s.setSingleLine(true);
        col.addView(s, top(context, 1));

        return col;
    }

    private ImageView edgeBtn(Context context, int iconRes, boolean isSuper, View.OnClickListener listener) {
        ImageView iv = new ImageView(context);
        iv.setImageResource(iconRes);
        iv.setColorFilter(isSuper ? 0xFFD97706 : 0xFFF8FAFC);
        iv.setBackground(Ui.pressable(context, 0x26FFFFFF, 16.0f, true));
        int pad = Ui.dp(context, 8.0f);
        iv.setPadding(pad, pad, pad, pad);
        int size = Ui.dp(context, 32.0f);
        iv.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        iv.setOnClickListener(listener);
        return iv;
    }

    private void buildQuickRibbon(final Context context, final Screen.Host host) {
        this.quickRibbon.removeAllViews();

        final boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(0);
        card.setGravity(Gravity.CENTER_VERTICAL);
        Ui.pad(card, 4, 8, 4, 8);

        card.addView(quickActionTile(context, isSuper ? R.drawable.ic_crown : R.drawable.ic_upload, isSuper ? "Releases" : "Updates", isSuper ? "Manager" : "OTA Status", isSuper ? 0xFFF59E0B : 0xFF6366F1, isSuper ? 0x26F59E0B : 0x1A6366F1, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new AppUpdatesScreen(context, host));
            }
        }), weight1());

        card.addView(quickActionTile(context, R.drawable.ic_note, "Broadcast", "Notices", isSuper ? 0xFFFBBF24 : 0xFF4F46E5, isSuper ? 0x26FBBF24 : 0x1A4F46E5, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new AdminInstructionScreen(context, host, AdminDashboardScreen.this.date));
            }
        }), weight1());

        card.addView(quickActionTile(context, R.drawable.ic_trending, "Targets", "Quotas", isSuper ? 0xFF38BDF8 : 0xFF0284C7, isSuper ? 0x2638BDF8 : 0x1A0284C7, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new SupervisorTargetScreen(context, host, AdminDashboardScreen.this.date));
            }
        }), weight1());

        card.addView(quickActionTile(context, R.drawable.ic_person, "Staff", "Roster", isSuper ? 0xFF10B981 : 0xFF10B981, isSuper ? 0x2610B981 : 0x1A10B981, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new SupervisorManageScreen(context, host));
            }
        }), weight1());

        card.addView(quickActionTile(context, R.drawable.ic_bank, "CSP Master", "Kiosks", isSuper ? 0xFFF59E0B : 0xFFF59E0B, isSuper ? 0x26F59E0B : 0x1AF59E0B, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new CspManageScreen(context, host));
            }
        }), weight1());

        card.addView(quickActionTile(context, R.drawable.ic_refresh, "Database", "Cloud Sync", isSuper ? 0xFFA855F7 : 0xFF8B5CF6, isSuper ? 0x26A855F7 : 0x1A8B5CF6, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new SupabaseSyncScreen(context, host));
            }
        }), weight1());

        this.quickRibbon.addView(card, mw());
    }

    private View quickActionTile(Context context, int iconRes, String title, String sub, int iconTint, int bgTint, View.OnClickListener listener) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(1);
        tile.setGravity(Gravity.CENTER);
        tile.setClickable(true);
        tile.setBackground(Ui.pressable(context, isSuper ? 0x221E293B : Ui.color(context, R.color.paper), 12.0f, false));
        Ui.pad(tile, 4, 8, 4, 8);
        tile.setOnClickListener(listener);

        FrameLayout iconBox = new FrameLayout(context);
        iconBox.setBackground(Ui.round(context, bgTint, 10.0f, isSuper ? iconTint : 0, isSuper ? 1.0f : 0f));
        int boxSize = Ui.dp(context, 32.0f);
        LinearLayout.LayoutParams iblp = new LinearLayout.LayoutParams(boxSize, boxSize);
        iblp.bottomMargin = Ui.dp(context, 4.0f);
        iconBox.setLayoutParams(iblp);

        ImageView icon = new ImageView(context);
        int pad = Ui.dp(context, 6.0f);
        icon.setPadding(pad, pad, pad, pad);
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconTint);
        iconBox.addView(icon, match());
        tile.addView(iconBox);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(t, 11.5f);
        t.setTextColor(isSuper ? 0xFFF8FAFC : Ui.color(context, R.color.ink));
        t.setSingleLine(true);
        tile.addView(t);

        TextView st = new TextView(context);
        st.setText(sub);
        Ui.size(st, 9.5f);
        st.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_3));
        st.setSingleLine(true);
        tile.addView(st);

        return tile;
    }

    private LinearLayout buildSearchBar(Context context) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(0);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        if (isSuper) {
            layout.setBackground(Ui.round(context, 0xEE090D1A, Ui.rdp(context, R.dimen.r_md), 0x99F59E0B, 1.0f));
        } else {
            layout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), Ui.rdp(context, R.dimen.r_md), Ui.color(context, R.color.line_2), 1.0f));
        }
        Ui.pad(layout, 12, 0, 6, 0);
        layout.setMinimumHeight(Ui.dp(context, 44.0f));

        ImageView icon = new ImageView(context);
        int size = Ui.dp(context, 18.0f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(size, size);
        ilp.rightMargin = Ui.dp(context, 8.0f);
        icon.setLayoutParams(ilp);
        icon.setImageResource(R.drawable.ic_search);
        icon.setColorFilter(isSuper ? 0xFFF59E0B : Ui.color(context, R.color.ink_4));
        layout.addView(icon);

        this.searchEdit = new EditText(context);
        this.searchEdit.setHint("Search staff, CSP code, branch, notes...");
        this.searchEdit.setBackground(null);
        Ui.size(this.searchEdit, 13.5f);
        this.searchEdit.setTextColor(isSuper ? 0xFFF8FAFC : Ui.color(context, R.color.ink));
        this.searchEdit.setHintTextColor(isSuper ? 0xFF64748B : Ui.color(context, R.color.ink_4));
        this.searchEdit.setSingleLine(true);
        layout.addView(this.searchEdit, weight1());

        final ImageView clearBtn = new ImageView(context);
        int csize = Ui.dp(context, 28.0f);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(csize, csize);
        clp.rightMargin = Ui.dp(context, 4.0f);
        clearBtn.setLayoutParams(clp);
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setColorFilter(isSuper ? 0xFFF59E0B : Ui.color(context, R.color.ink_4));
        clearBtn.setBackground(Ui.round(context, isSuper ? 0x33F59E0B : 0x15000000, 14.0f, 0, 0f));
        Ui.pad(clearBtn, 6, 6, 6, 6);
        clearBtn.setVisibility(8);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdminDashboardScreen.this.searchEdit.setText("");
            }
        });
        layout.addView(clearBtn);

        this.searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                AdminDashboardScreen.this.query = s.toString().trim().toLowerCase(Locale.US);
                clearBtn.setVisibility(AdminDashboardScreen.this.query.isEmpty() ? 8 : 0);
                AdminDashboardScreen.this.renderContentArea();
            }
        });

        return layout;
    }

    @Override
    public void refreshContent() {
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        this.date = UiState.date(context);

        DaySummary summary = Day.summary(db, this.date);
        List<SupervisorMap.SupervisorStats> statsList = SupervisorMap.getAllSupervisorsStats(db, this.date);

        paintCommandDeckHeader(context, summary, statsList);
        paintSupervisorFilterBar(statsList);
        renderContentArea();
    }

    private void paintSupervisorFilterBar(final List<SupervisorMap.SupervisorStats> statsList) {
        Context context = getContext();
        this.supervisorFilterBar.removeAllViews();

        if (statsList.isEmpty()) {
            return;
        }

        String[] supOptions = new String[statsList.size() + 1];
        supOptions[0] = "All Staff (" + statsList.size() + ")";
        int selectedIndex = 0;

        for (int i = 0; i < statsList.size(); i++) {
            supOptions[i + 1] = statsList.get(i).supervisor.name;
            if (this.filterSupervisorId != null && this.filterSupervisorId == statsList.get(i).supervisor.id) {
                selectedIndex = i + 1;
            }
        }

        Chips supChips = new Chips(context);
        supChips.set(supOptions);
        supChips.allowNone(false);
        supChips.select(selectedIndex);
        supChips.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                if (i == 0) {
                    filterSupervisorId = null;
                } else {
                    filterSupervisorId = statsList.get(i - 1).supervisor.id;
                }
                renderContentArea();
            }
        });

        this.supervisorFilterBar.addView(supChips, mw());
    }

    private void renderContentArea() {
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        this.contentArea.removeAllViews();

        if (MAIN_TABS[0].equals(this.activeTab)) {
            renderSupervisorsOverview(db, context);
        } else if (MAIN_TABS[1].equals(this.activeTab)) {
            renderCallsAndFollowups(db, context);
        } else if (MAIN_TABS[2].equals(this.activeTab)) {
            renderReportsLedger(db, context);
        } else if (MAIN_TABS[3].equals(this.activeTab)) {
            renderPendingCsps(db, context);
        } else {
            renderLiveActivityStream(db, context);
        }
    }

    // ==========================================
    // TAB 1: SUPERVISORS OPERATIONS OVERVIEW
    // ==========================================
    private void renderSupervisorsOverview(SQLiteDatabase db, final Context context) {
        List<SupervisorMap.SupervisorStats> statsList = SupervisorMap.getAllSupervisorsStats(db, this.date);
        List<SupervisorMap.SupervisorStats> filtered = new ArrayList<>();

        for (SupervisorMap.SupervisorStats st : statsList) {
            if (this.filterSupervisorId != null && st.supervisor.id != this.filterSupervisorId) {
                continue;
            }
            if (!this.query.isEmpty()) {
                String full = (st.supervisor.name + " " + st.supervisor.username + " " + String.join(" ", st.branches)).toLowerCase(Locale.US);
                if (!full.contains(this.query)) continue;
            }
            filtered.add(st);
        }

        if (filtered.isEmpty()) {
            this.empty.show(R.drawable.ic_person, R.color.line_2, "No supervisor records found", "Tap 'Staff / Users' above to add staff accounts or adjust filters.");
            return;
        }
        this.empty.hide();

        for (int i = 0; i < filtered.size(); i++) {
            final SupervisorMap.SupervisorStats st = filtered.get(i);
            View card = buildExecutiveSupervisorCard(context, st);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 10.0f);
            this.contentArea.addView(card, lp);
            Ui.enterRow(card, i);
        }
    }

    private View buildExecutiveSupervisorCard(final Context context, final SupervisorMap.SupervisorStats st) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 14, 14, 14);

        // Header Row: Avatar + Name + Branches + Completion Badge
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow, mw());

        topRow.addView(new Avatar(context, 44).bind(st.supervisor.name, st.supervisor.username), new LinearLayout.LayoutParams(Ui.dp(context, 44.0f), Ui.dp(context, 44.0f)));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(1);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        infoLp.leftMargin = Ui.dp(context, 12.0f);
        info.setLayoutParams(infoLp);
        topRow.addView(info);

        TextView nameText = new TextView(context);
        nameText.setText(st.supervisor.name);
        Ui.size(nameText, 16.0f);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setTextColor(isSuper ? 0xFFFEF08A : Ui.color(context, R.color.ink));
        info.addView(nameText);

        TextView supMeta = new TextView(context);
        String branchesStr = st.branches.isEmpty() ? "No branches assigned" : String.join(", ", st.branches);
        supMeta.setText(branchesStr + (st.supervisor.mobile.isEmpty() ? "" : " · " + st.supervisor.mobile));
        Ui.size(supMeta, 11.5f);
        supMeta.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_3));
        info.addView(supMeta);

        int pct = st.totalCsps > 0 ? (st.reportedCsps * 100 / st.totalCsps) : 0;
        Badge badge = new Badge(context);
        badge.setKind(pct >= 100 ? 1 : (pct > 0 ? 3 : 0));
        badge.setText(pct + "% DONE");
        topRow.addView(badge, wrap());

        // Scorecard Metrics Bar (4 Columns)
        LinearLayout statPanel = new LinearLayout(context);
        statPanel.setOrientation(0);
        statPanel.setGravity(Gravity.CENTER_VERTICAL);
        if (isSuper) {
            statPanel.setBackground(Ui.round(context, 0x331E293B, 12.0f, 0x66475569, 1.0f));
        } else {
            statPanel.setBackground(Ui.round(context, 0xFFF8FAFC, 12.0f, Ui.color(context, R.color.line), 1.0f));
        }
        Ui.pad(statPanel, 10, 8, 10, 8);
        LinearLayout.LayoutParams splp = mw();
        splp.topMargin = Ui.dp(context, 12.0f);
        card.addView(statPanel, splp);

        statPanel.addView(subMetricCol(context, "FILED", st.reportedCsps + "/" + st.totalCsps, isSuper ? 0xFF34D399 : R.color.ok), weight1());
        statPanel.addView(ruleDivider(context, isSuper), ruleDividerLp(context));

        statPanel.addView(subMetricCol(context, "PENDING", String.valueOf(st.pendingCsps), isSuper ? 0xFFF87171 : R.color.danger), weight1());
        statPanel.addView(ruleDivider(context, isSuper), ruleDividerLp(context));

        statPanel.addView(subMetricCol(context, "CALLS", String.valueOf(st.totalCalls), isSuper ? 0xFFFBBF24 : R.color.warn), weight1());
        statPanel.addView(ruleDivider(context, isSuper), ruleDividerLp(context));

        statPanel.addView(subMetricCol(context, "BUSINESS", String.valueOf(st.totalBusiness), isSuper ? 0xFF60A5FA : R.color.primary_700), weight1());

        // Action Buttons Row: Deep Dive Audit / Map CSPs / Contact
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(0);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actLp = mw();
        actLp.topMargin = Ui.dp(context, 12.0f);
        card.addView(actions, actLp);

        View auditBtn = isSuper ?
            Cards.goldButton(context, "🔍 Deep Dive Audit", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdminDashboardScreen.this.host.push(new SupervisorAuditScreen(context, AdminDashboardScreen.this.host, st.supervisor.id, AdminDashboardScreen.this.date));
                }
            }) :
            Cards.primaryButton(context, "🔍 Deep Dive Audit", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdminDashboardScreen.this.host.push(new SupervisorAuditScreen(context, AdminDashboardScreen.this.host, st.supervisor.id, AdminDashboardScreen.this.date));
                }
            });
        actions.addView(auditBtn, weight1());

        View sp1 = new View(context);
        actions.addView(sp1, new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));

        View mapBtn = isSuper ?
            Cards.goldOutlineButton(context, "🗺️ Map CSPs", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdminDashboardScreen.this.host.push(new CspSupervisorMappingScreen(context, AdminDashboardScreen.this.host, st.supervisor.id));
                }
            }) :
            Cards.outlineButton(context, "🗺️ Map CSPs", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdminDashboardScreen.this.host.push(new CspSupervisorMappingScreen(context, AdminDashboardScreen.this.host, st.supervisor.id));
                }
            });
        actions.addView(mapBtn, weight1());

        if (!st.supervisor.mobile.isEmpty()) {
            View sp2 = new View(context);
            actions.addView(sp2, new LinearLayout.LayoutParams(Ui.dp(context, 6.0f), 1));

            ImageView callIcon = new ImageView(context);
            callIcon.setImageResource(R.drawable.ic_phone);
            callIcon.setColorFilter(isSuper ? 0xFFF59E0B : Ui.color(context, R.color.warn));
            callIcon.setBackground(Ui.round(context, 0x1AF59E0B, 10.0f, 0xFFF59E0B, 1.0f));
            int p = Ui.dp(context, 8.0f);
            callIcon.setPadding(p, p, p, p);
            int btnS = Ui.dp(context, 38.0f);
            callIcon.setLayoutParams(new LinearLayout.LayoutParams(btnS, btnS));
            callIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Contacts.call(context, st.supervisor.mobile);
                }
            });
            actions.addView(callIcon);

            View sp3 = new View(context);
            actions.addView(sp3, new LinearLayout.LayoutParams(Ui.dp(context, 6.0f), 1));

            ImageView waIcon = new ImageView(context);
            waIcon.setImageResource(R.drawable.ic_whatsapp);
            waIcon.setColorFilter(0xFF25D366);
            waIcon.setBackground(Ui.round(context, 0x1A25D366, 10.0f, 0xFF25D366, 1.0f));
            waIcon.setPadding(p, p, p, p);
            waIcon.setLayoutParams(new LinearLayout.LayoutParams(btnS, btnS));
            waIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Contacts.whatsapp(context, st.supervisor.mobile, "Hi " + st.supervisor.name + ", reviewing CSP Daily Reports for " + AdminDashboardScreen.this.date + ". Please update today's status.");
                }
            });
            actions.addView(waIcon);
        }

        return card;
    }

    private LinearLayout subMetricCol(Context context, String label, String value, int colorVal) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(1);
        col.setGravity(Gravity.CENTER);

        TextView lbl = new TextView(context);
        lbl.setText(label);
        Ui.size(lbl, 9.5f);
        lbl.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_3));
        lbl.setGravity(Gravity.CENTER);
        col.addView(lbl, mw());

        TextView val = new TextView(context);
        val.setText(value);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(val, 15.0f);
        if ((colorVal & 0xFF000000) != 0) {
            val.setTextColor(colorVal);
        } else {
            val.setTextColor(Ui.color(context, colorVal));
        }
        val.setGravity(Gravity.CENTER);
        col.addView(val, top(context, 1));

        return col;
    }

    private View ruleDivider(Context context, boolean isSuper) {
        View v = new View(context);
        v.setBackgroundColor(isSuper ? 0x4D64748B : Ui.color(context, R.color.line));
        return v;
    }

    private View ruleDivider(Context context) {
        return ruleDivider(context, Auth.isSuperAdmin(context));
    }

    private LinearLayout.LayoutParams ruleDividerLp(Context context) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(context, 1.0f), Ui.dp(context, 22.0f));
        lp.gravity = Gravity.CENTER_VERTICAL;
        return lp;
    }

    // ==========================================
    // TAB 2: CALLS, WHATSAPP & FOLLOW-UPS AUDIT
    // ==========================================
    private void renderCallsAndFollowups(SQLiteDatabase db, Context context) {
        List<SupervisorMap.SupervisorFollowupItem> followups = SupervisorMap.getFollowups(db, this.date, this.filterSupervisorId, null);
        List<SupervisorMap.SupervisorFollowupItem> filtered = new ArrayList<>();

        for (SupervisorMap.SupervisorFollowupItem f : followups) {
            if (!this.query.isEmpty()) {
                String full = (f.supervisorName + " " + f.cspName + " " + f.cspCode + " " + f.branchName + " " + f.actionStatus + " " + f.note).toLowerCase(Locale.US);
                if (!full.contains(this.query)) continue;
            }
            filtered.add(f);
        }

        if (filtered.isEmpty()) {
            this.empty.show(R.drawable.ic_phone, R.color.line_2, "No supervisor calls or follow-ups", "Calls and follow-ups made by supervisors will stream here live.");
            return;
        }
        this.empty.hide();

        TextView countHeader = Cards.bold(context, "Logged Follow-ups & Calls (" + filtered.size() + ")", 14.0f, R.color.ink_2);
        Ui.pad(countHeader, 4, 4, 4, 6);
        this.contentArea.addView(countHeader);

        for (int i = 0; i < filtered.size(); i++) {
            final SupervisorMap.SupervisorFollowupItem item = filtered.get(i);
            View card = buildFollowupAuditCard(context, item);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 8.0f);
            this.contentArea.addView(card, lp);
            Ui.enterRow(card, i);
        }
    }

    private View buildFollowupAuditCard(final Context context, final SupervisorMap.SupervisorFollowupItem item) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 12, 14, 12);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow, mw());

        FrameLayout iconBox = new FrameLayout(context);
        int tint = item.isCall() ? 0xFFF59E0B : (item.isWhatsApp() ? 0xFF10B981 : 0xFF4F46E5);
        int bgTint = item.isCall() ? 0x26F59E0B : (item.isWhatsApp() ? 0x2610B981 : 0x264F46E5);
        iconBox.setBackground(Ui.round(context, bgTint, 10.0f, tint, 1.0f));
        int isize = Ui.dp(context, 34.0f);
        topRow.addView(iconBox, new LinearLayout.LayoutParams(isize, isize));

        ImageView icon = new ImageView(context);
        int ip = Ui.dp(context, 7.0f);
        icon.setPadding(ip, ip, ip, ip);
        icon.setImageResource(item.isCall() ? R.drawable.ic_phone : (item.isWhatsApp() ? R.drawable.ic_whatsapp : R.drawable.ic_note));
        icon.setColorFilter(tint);
        iconBox.addView(icon, match());

        LinearLayout nameCol = new LinearLayout(context);
        nameCol.setOrientation(1);
        LinearLayout.LayoutParams nclp = weight1();
        nclp.leftMargin = Ui.dp(context, 10.0f);
        nameCol.setLayoutParams(nclp);
        topRow.addView(nameCol);

        TextView cspName = new TextView(context);
        cspName.setText(item.cspName);
        cspName.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(cspName, 15.0f);
        cspName.setTextColor(isSuper ? 0xFFFEF08A : Ui.color(context, R.color.ink));
        nameCol.addView(cspName);

        TextView supTag = new TextView(context);
        supTag.setText("👤 Supervisor: " + item.supervisorName);
        Ui.size(supTag, 11.5f);
        supTag.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.primary_700));
        supTag.setTypeface(Typeface.DEFAULT_BOLD);
        nameCol.addView(supTag);

        TextView timeText = new TextView(context);
        timeText.setText(item.formattedTime());
        Ui.size(timeText, 11.0f);
        timeText.setTextColor(isSuper ? 0xFF64748B : Ui.color(context, R.color.ink_4));
        topRow.addView(timeText, wrap());

        // Meta Tag Bar
        LinearLayout metaBar = new LinearLayout(context);
        metaBar.setOrientation(0);
        metaBar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mblp = mw();
        mblp.topMargin = Ui.dp(context, 8.0f);
        metaBar.setLayoutParams(mblp);
        card.addView(metaBar);

        TextView codeBadge = new TextView(context);
        codeBadge.setText("#" + Biz.cspCodeSuffix(item.cspCode));
        codeBadge.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(codeBadge, 11.0f);
        codeBadge.setTextColor(isSuper ? 0xFFF59E0B : Ui.color(context, R.color.ink_2));
        codeBadge.setBackground(Ui.round(context, isSuper ? 0x331E293B : 0xFFF1F5F9, 6.0f, isSuper ? 0x66F59E0B : 0, isSuper ? 1.0f : 0f));
        codeBadge.setPadding(Ui.dp(context, 6.0f), Ui.dp(context, 2.0f), Ui.dp(context, 6.0f), Ui.dp(context, 2.0f));
        metaBar.addView(codeBadge);

        TextView brText = new TextView(context);
        brText.setText(" · " + item.branchName + " · " + item.actionStatus);
        Ui.size(brText, 11.5f);
        brText.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_3));
        metaBar.addView(brText);

        if (item.note != null && !item.note.trim().isEmpty()) {
            LinearLayout noteBox = new LinearLayout(context);
            noteBox.setOrientation(0);
            noteBox.setBackground(Ui.round(context, isSuper ? 0x261E293B : 0xFFF8FAFC, 8.0f, isSuper ? 0x4D64748B : 0xFFE2E8F0, 1.0f));
            Ui.pad(noteBox, 10, 6, 10, 6);
            LinearLayout.LayoutParams nblp = mw();
            nblp.topMargin = Ui.dp(context, 6.0f);
            noteBox.setLayoutParams(nblp);

            TextView noteView = new TextView(context);
            noteView.setText("💬 " + item.note.trim());
            Ui.size(noteView, 12.5f);
            noteView.setTextColor(isSuper ? 0xFFCBD5E1 : Ui.color(context, R.color.ink_2));
            noteBox.addView(noteView);
            card.addView(noteBox);
        }

        return card;
    }

    // ==========================================
    // TAB 3: REPORTS & PARAMETER LEDGER AUDIT
    // ==========================================
    private void renderReportsLedger(SQLiteDatabase db, Context context) {
        boolean isSuper = Auth.isSuperAdmin(context);
        List<SupervisorMap.SupervisorReportItem> reports = SupervisorMap.getReports(db, this.date, this.filterSupervisorId, null);
        List<SupervisorMap.SupervisorReportItem> filtered = new ArrayList<>();

        for (SupervisorMap.SupervisorReportItem r : reports) {
            if (!this.query.isEmpty()) {
                String full = (r.supervisorName + " " + r.cspName + " " + r.cspCode + " " + r.branchName + " " + r.remarks).toLowerCase(Locale.US);
                if (!full.contains(this.query)) continue;
            }
            filtered.add(r);
        }

        if (filtered.isEmpty()) {
            this.empty.show(R.drawable.ic_reports, R.color.line_2, "No reports submitted yet", "Submitted daily figures under supervisors will be audited here.");
            return;
        }
        this.empty.hide();

        TextView countHeader = Cards.bold(context, "Submitted Daily Reports (" + filtered.size() + ")", 14.0f, isSuper ? 0xFFFEF08A : R.color.ink_2);
        Ui.pad(countHeader, 4, 4, 4, 6);
        this.contentArea.addView(countHeader);

        for (int i = 0; i < filtered.size(); i++) {
            final SupervisorMap.SupervisorReportItem r = filtered.get(i);
            View card = buildReportAuditCard(context, r);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 8.0f);
            this.contentArea.addView(card, lp);
            Ui.enterRow(card, i);
        }
    }

    private View buildReportAuditCard(Context context, SupervisorMap.SupervisorReportItem r) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 12, 14, 12);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow, mw());

        LinearLayout nameCol = new LinearLayout(context);
        nameCol.setOrientation(1);
        topRow.addView(nameCol, weight1());

        TextView cspName = new TextView(context);
        cspName.setText(r.cspName);
        cspName.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(cspName, 15.5f);
        cspName.setTextColor(isSuper ? 0xFFFEF08A : Ui.color(context, R.color.ink));
        nameCol.addView(cspName);

        TextView supTag = new TextView(context);
        supTag.setText("👤 Field Staff: " + r.supervisorName + " · " + r.branchName + " (#" + Biz.cspCodeSuffix(r.cspCode) + ")");
        Ui.size(supTag, 11.5f);
        supTag.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_3));
        nameCol.addView(supTag);

        Badge totalBadge = new Badge(context);
        totalBadge.setKind(1);
        totalBadge.setText("Total: " + r.totalBusiness);
        topRow.addView(totalBadge, wrap());

        // Parameter Breakdown Grid
        LinearLayout chipsRow = new LinearLayout(context);
        chipsRow.setOrientation(0);
        LinearLayout.LayoutParams crlp = mw();
        crlp.topMargin = Ui.dp(context, 10.0f);
        chipsRow.setLayoutParams(crlp);
        card.addView(chipsRow);

        int[][] tints = new int[][]{
            {0xFFF59E0B, 0x26F59E0B},
            {0xFF38BDF8, 0x2638BDF8},
            {0xFF10B981, 0x2610B981},
            {0xFFA855F7, 0x26A855F7},
            {0xFF818CF8, 0x26818CF8},
            {0xFFFB7185, 0x26FB7185},
            {0xFF34D399, 0x2634D399}
        };

        LinearLayout chipsContainer = new LinearLayout(context);
        chipsContainer.setOrientation(0);
        int colorIdx = 0;
        for (Map.Entry<String, Integer> entry : r.paramValues.entrySet()) {
            int val = entry.getValue() != null ? entry.getValue() : 0;
            if (val > 0) {
                int[] tint = tints[colorIdx % tints.length];
                chipsContainer.addView(paramChip(context, entry.getKey(), val, tint[0], tint[1]));
                colorIdx++;
            }
        }
        if (chipsContainer.getChildCount() == 0) {
            TextView noBiz = new TextView(context);
            noBiz.setText("Nil report filed (0 across all parameters)");
            Ui.size(noBiz, 12.0f);
            noBiz.setTextColor(isSuper ? 0xFF64748B : Ui.color(context, R.color.ink_4));
            chipsContainer.addView(noBiz);
        }
        chipsRow.addView(chipsContainer);

        if (r.remarks != null && !r.remarks.trim().isEmpty()) {
            TextView remarksView = new TextView(context);
            remarksView.setText("Remarks: " + r.remarks.trim());
            Ui.size(remarksView, 12.0f);
            remarksView.setTextColor(isSuper ? 0xFFCBD5E1 : Ui.color(context, R.color.ink_3));
            card.addView(remarksView, top(context, 6));
        }

        return card;
    }

    private View paramChip(Context context, String key, int value, int tint, int bgTint) {
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(0);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setBackground(Ui.round(context, bgTint, 8.0f, tint, 1.0f));
        chip.setPadding(Ui.dp(context, 7.0f), Ui.dp(context, 3.0f), Ui.dp(context, 7.0f), Ui.dp(context, 3.0f));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
        clp.rightMargin = Ui.dp(context, 6.0f);
        clp.bottomMargin = Ui.dp(context, 4.0f);
        chip.setLayoutParams(clp);

        String displayLabel = Params.shortLabel(key);
        TextView lbl = new TextView(context);
        lbl.setText(displayLabel + ": ");
        Ui.size(lbl, 11.0f);
        lbl.setTextColor(tint);
        chip.addView(lbl);

        TextView val = new TextView(context);
        val.setText(String.valueOf(value));
        val.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(val, 11.5f);
        val.setTextColor(tint);
        chip.addView(val);

        return chip;
    }

    // ==========================================
    // TAB 4: PENDING CSPS NETWORK AUDIT
    // ==========================================
    private void renderPendingCsps(SQLiteDatabase db, Context context) {
        boolean isSuper = Auth.isSuperAdmin(context);
        List<SupervisorMap.SupervisorPendingItem> pendingList = SupervisorMap.getPendingCsps(db, this.date, this.filterSupervisorId);
        List<SupervisorMap.SupervisorPendingItem> filtered = new ArrayList<>();

        for (SupervisorMap.SupervisorPendingItem p : pendingList) {
            if (!this.query.isEmpty()) {
                String full = (p.supervisorName + " " + p.cspName + " " + p.cspCode + " " + p.branchName + " " + p.pendingReason).toLowerCase(Locale.US);
                if (!full.contains(this.query)) continue;
            }
            filtered.add(p);
        }

        if (filtered.isEmpty()) {
            this.empty.show(R.drawable.ic_check_circle, R.color.ok, "All CSPs reported!", "Great job! 100% of CSP reports for this date are completed.");
            return;
        }
        this.empty.hide();

        TextView countHeader = Cards.bold(context, "Pending Unfiled CSPs (" + filtered.size() + ")", 14.0f, isSuper ? 0xFFF87171 : R.color.danger);
        Ui.pad(countHeader, 4, 4, 4, 6);
        this.contentArea.addView(countHeader);

        for (int i = 0; i < filtered.size(); i++) {
            final SupervisorMap.SupervisorPendingItem p = filtered.get(i);
            View card = buildPendingCspCard(context, p);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 8.0f);
            this.contentArea.addView(card, lp);
            Ui.enterRow(card, i);
        }
    }

    private View buildPendingCspCard(final Context context, final SupervisorMap.SupervisorPendingItem p) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 12, 14, 12);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow, mw());

        LinearLayout nameCol = new LinearLayout(context);
        nameCol.setOrientation(1);
        topRow.addView(nameCol, weight1());

        TextView cspName = new TextView(context);
        cspName.setText(p.cspName);
        cspName.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(cspName, 15.5f);
        cspName.setTextColor(isSuper ? 0xFFFEF08A : Ui.color(context, R.color.ink));
        nameCol.addView(cspName);

        TextView supTag = new TextView(context);
        supTag.setText("👤 Supervisor: " + p.supervisorName + " · " + p.branchName + " (#" + Biz.cspCodeSuffix(p.cspCode) + ")");
        Ui.size(supTag, 11.5f);
        supTag.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_3));
        nameCol.addView(supTag);

        Badge pendingBadge = new Badge(context);
        pendingBadge.setKind(0);
        pendingBadge.setText("UNFILED");
        topRow.addView(pendingBadge, wrap());

        // Reason box if recorded
        String reasonOrNote = (p.pendingReason != null && !p.pendingReason.trim().isEmpty()) ? p.pendingReason.trim() : ((p.latestNote != null && !p.latestNote.trim().isEmpty()) ? p.latestNote.trim() : "");
        if (!reasonOrNote.isEmpty()) {
            LinearLayout rBox = new LinearLayout(context);
            rBox.setOrientation(0);
            rBox.setBackground(Ui.round(context, isSuper ? 0x33450A0A : 0xFFFEF2F2, 8.0f, isSuper ? 0xFFDC2626 : 0xFFFCA5A5, 1.0f));
            Ui.pad(rBox, 10, 6, 10, 6);
            LinearLayout.LayoutParams rblp = mw();
            rblp.topMargin = Ui.dp(context, 8.0f);
            rBox.setLayoutParams(rblp);

            TextView rTxt = new TextView(context);
            rTxt.setText("⚠️ " + ((p.pendingReason != null && !p.pendingReason.trim().isEmpty()) ? "Reason: " : "Note: ") + reasonOrNote);
            Ui.size(rTxt, 12.0f);
            rTxt.setTextColor(isSuper ? 0xFFFCA5A5 : 0xFFDC2626);
            rTxt.setTypeface(Typeface.DEFAULT_BOLD);
            rBox.addView(rTxt);
            card.addView(rBox);
        }

        // Action Buttons Row (Direct Call CSP / WhatsApp Reminder)
        if (!p.mobileNumber.isEmpty()) {
            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(0);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams alp = mw();
            alp.topMargin = Ui.dp(context, 10.0f);
            actions.setLayoutParams(alp);
            card.addView(actions);

            View callBtn = isSuper ?
                Cards.goldOutlineButton(context, "📞 Call CSP", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Contacts.call(context, p.mobileNumber);
                    }
                }) :
                Cards.outlineButton(context, "📞 Call CSP", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Contacts.call(context, p.mobileNumber);
                    }
                });
            actions.addView(callBtn, weight1());

            View sp = new View(context);
            actions.addView(sp, new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));

            View waBtn = isSuper ?
                Cards.goldButton(context, "💬 WhatsApp Reminder", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Contacts.whatsapp(context, p.mobileNumber, "Reminder: Please submit daily report for dated " + Biz.fmtDmy(AdminDashboardScreen.this.date) + ".");
                    }
                }) :
                Cards.outlineButton(context, "💬 WhatsApp Reminder", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Contacts.whatsapp(context, p.mobileNumber, "Reminder: Please submit daily report for dated " + Biz.fmtDmy(AdminDashboardScreen.this.date) + ".");
                    }
                });
            actions.addView(waBtn, weight1());
        }

        return card;
    }

    // ==========================================
    // TAB 5: UNIFIED LIVE ACTIVITY STREAM
    // ==========================================
    private void renderLiveActivityStream(SQLiteDatabase db, Context context) {
        boolean isSuper = Auth.isSuperAdmin(context);
        List<SupervisorMap.SupervisorFollowupItem> followups = SupervisorMap.getFollowups(db, this.date, this.filterSupervisorId, null);
        List<SupervisorMap.SupervisorReportItem> reports = SupervisorMap.getReports(db, this.date, this.filterSupervisorId, null);

        class StreamEvent {
            String time;
            String title;
            String subtitle;
            String note;
            int type; // 1: Report, 2: Call, 3: WhatsApp, 4: Note
        }

        List<StreamEvent> events = new ArrayList<>();

        for (SupervisorMap.SupervisorReportItem r : reports) {
            StreamEvent e = new StreamEvent();
            e.time = r.submittedAt != null ? r.submittedAt : "";
            e.title = "📊 Report Filed: " + r.cspName + " (" + Biz.cspCodeSuffix(r.cspCode) + ")";
            e.subtitle = "Staff: " + r.supervisorName + " · " + r.branchName + " · Business: " + r.totalBusiness;
            e.note = r.remarks;
            e.type = 1;
            events.add(e);
        }

        for (SupervisorMap.SupervisorFollowupItem f : followups) {
            StreamEvent e = new StreamEvent();
            e.time = f.createdAt;
            e.title = (f.isCall() ? "📞 Call Logged: " : (f.isWhatsApp() ? "💬 WhatsApp Sent: " : "📝 Note: ")) + f.cspName;
            e.subtitle = "Staff: " + f.supervisorName + " · " + f.branchName + " · Action: " + f.actionStatus;
            e.note = f.note;
            e.type = f.isCall() ? 2 : (f.isWhatsApp() ? 3 : 4);
            events.add(e);
        }

        Collections.sort(events, new Comparator<StreamEvent>() {
            @Override
            public int compare(StreamEvent a, StreamEvent b) {
                String t1 = a.time == null ? "" : a.time;
                String t2 = b.time == null ? "" : b.time;
                return t2.compareTo(t1);
            }
        });

        if (events.isEmpty()) {
            this.empty.show(R.drawable.ic_note, R.color.line_2, "No live activity yet", "Reports and communications will stream here in real-time.");
            return;
        }
        this.empty.hide();

        TextView countHeader = Cards.bold(context, "Chronological Live Audit Stream (" + events.size() + ")", 14.0f, isSuper ? 0xFFFEF08A : R.color.ink_2);
        Ui.pad(countHeader, 4, 4, 4, 6);
        this.contentArea.addView(countHeader);

        for (int i = 0; i < events.size(); i++) {
            StreamEvent e = events.get(i);
            LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
            card.setOrientation(1);
            Ui.pad(card, 12, 10, 12, 10);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(0);
            topRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(topRow, mw());

            int dotColor = e.type == 1 ? 0xFF10B981 : (e.type == 2 ? 0xFFF59E0B : (e.type == 3 ? 0xFF059669 : 0xFF818CF8));
            View dot = new View(context);
            dot.setBackground(Ui.round(context, dotColor, 4.0f));
            int dsize = Ui.dp(context, 8.0f);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dsize, dsize);
            dlp.rightMargin = Ui.dp(context, 8.0f);
            topRow.addView(dot, dlp);

            TextView titleView = new TextView(context);
            titleView.setText(e.title);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(titleView, 14.0f);
            titleView.setTextColor(isSuper ? 0xFFFEF08A : Ui.color(context, R.color.ink));
            topRow.addView(titleView, weight1());

            TextView tView = new TextView(context);
            tView.setText(Biz.clockOf(e.time));
            Ui.size(tView, 11.0f);
            tView.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_4));
            topRow.addView(tView, wrap());

            TextView subView = new TextView(context);
            subView.setText(e.subtitle);
            Ui.size(subView, 11.5f);
            subView.setTextColor(isSuper ? 0xFF94A3B8 : Ui.color(context, R.color.ink_3));
            card.addView(subView, top(context, 4));

            if (e.note != null && !e.note.trim().isEmpty()) {
                TextView nView = new TextView(context);
                nView.setText("💬 " + e.note.trim());
                Ui.size(nView, 12.0f);
                nView.setTextColor(isSuper ? 0xFFCBD5E1 : Ui.color(context, R.color.ink_2));
                card.addView(nView, top(context, 4));
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 6.0f);
            this.contentArea.addView(card, lp);
            Ui.enterRow(card, i);
        }
    }

    private void step(int offset) {
        this.date = Biz.plusDays(this.date, offset);
        UiState.setDate(getContext(), this.date);
        refreshContent();
    }

    private void pickDate() {
        final Context context = getContext();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Biz.millisOf(this.date));

        DatePickerDialog dpd = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                AdminDashboardScreen.this.date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                UiState.setDate(context, AdminDashboardScreen.this.date);
                AdminDashboardScreen.this.refreshContent();
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpd.show();
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

    private LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }

    private LinearLayout.LayoutParams top(Context context, int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dp(context, dp);
        return lp;
    }
}
