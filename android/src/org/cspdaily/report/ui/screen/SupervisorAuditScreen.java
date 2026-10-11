package org.cspdaily.report.ui.screen;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.util.Contacts;
import org.cspdaily.report.util.PhoneActions;

public class SupervisorAuditScreen extends Screen {
    private static final String[] SUB_TABS = {"📋 Assigned CSPs", "📞 Calls & Logs", "📊 Filed Reports", "🎯 Target Matrix"};
    
    private final long supervisorId;
    private User supervisor;
    private String date;
    private String currentSubTab = SUB_TABS[0];

    private final LinearLayout unifiedHeroCard;
    private final Chips subTabsChips;
    private final LinearLayout listContainer;
    private final Empty empty;

    public SupervisorAuditScreen(final Context context, final Screen.Host host, long supervisorId, String date) {
        super(context, host);
        this.supervisorId = supervisorId;
        this.date = date != null ? date : UiState.date(context);

        SQLiteDatabase db = Core.get(context).dbRead();
        this.supervisor = User.byId(db, supervisorId);
        String supName = this.supervisor != null ? this.supervisor.name : "Supervisor";

        title("Supervisor Deep Audit");
        subtitle(supName + " · " + Biz.fmtDmy(this.date));

        headerAction("Change Date", new Runnable() {
            @Override
            public void run() {
                SupervisorAuditScreen.this.host.push(new BusinessDayScreen(context, host, SupervisorAuditScreen.this.date));
            }
        });

        // 1. Unified Compact Executive Command Card
        this.unifiedHeroCard = new LinearLayout(context);
        this.unifiedHeroCard.setOrientation(1);
        this.unifiedHeroCard.setBackground(buildHeroBackground(context));
        Ui.pad(this.unifiedHeroCard, 14, 12, 14, 12);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            this.unifiedHeroCard.setElevation(Ui.px(context, R.dimen.el_3));
        }
        add(this.unifiedHeroCard, 0);

        // 2. Sub Tabs Chips
        this.subTabsChips = new Chips(context);
        this.subTabsChips.set(SUB_TABS);
        this.subTabsChips.allowNone(false);
        this.subTabsChips.select(0);
        this.subTabsChips.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                SupervisorAuditScreen.this.currentSubTab = str;
                SupervisorAuditScreen.this.renderSubTab();
            }
        });
        add(this.subTabsChips, 8);

        // 3. Dynamic List Container & Empty state
        this.listContainer = new LinearLayout(context);
        this.listContainer.setOrientation(1);
        add(this.listContainer, 4);

        this.empty = new Empty(context);
        add(this.empty, 0);

        refreshContent();
    }

    private GradientDrawable buildHeroBackground(Context context) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(Ui.dp(context, 16.0f));
        gd.setColors(new int[]{0xFF0B132B, 0xFF1C2541});
        gd.setOrientation(GradientDrawable.Orientation.TL_BR);
        gd.setStroke(Ui.dp(context, 1.0f), 0x3338BDF8);
        return gd;
    }

    @Override
    public String headline() {
        return "Supervisor Deep Audit";
    }

    @Override
    public void refreshContent() {
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        this.supervisor = User.byId(db, this.supervisorId);
        if (this.supervisor == null) {
            this.empty.show(R.drawable.ic_person, R.color.line_2, "Supervisor account not found", "This staff account may have been removed.");
            return;
        }

        SupervisorMap.SupervisorStats stats = SupervisorMap.getStatsForSupervisor(db, this.supervisor, this.date);

        paintUnifiedHeroCard(stats);
        renderSubTab();
    }

    private void paintUnifiedHeroCard(final SupervisorMap.SupervisorStats stats) {
        final Context context = getContext();
        this.unifiedHeroCard.removeAllViews();

        // 1. Identity Row
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        this.unifiedHeroCard.addView(topRow, mw());

        topRow.addView(new Avatar(context, 40).bind(this.supervisor.name, this.supervisor.username), new LinearLayout.LayoutParams(Ui.dp(context, 40.0f), Ui.dp(context, 40.0f)));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(1);
        LinearLayout.LayoutParams infoLp = weight1();
        infoLp.leftMargin = Ui.dp(context, 10.0f);
        info.setLayoutParams(infoLp);
        topRow.addView(info);

        TextView nameText = new TextView(context);
        nameText.setText(this.supervisor.name);
        Ui.size(nameText, 15.5f);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setTextColor(0xFFF8FAFC);
        info.addView(nameText);

        TextView subMeta = new TextView(context);
        String branchText;
        if (stats.branches.isEmpty()) {
            branchText = "No branch mapped";
        } else if (stats.branches.size() <= 3) {
            branchText = String.join(", ", stats.branches);
        } else {
            branchText = stats.branches.get(0) + ", " + stats.branches.get(1) + ", " + stats.branches.get(2) + " +" + (stats.branches.size() - 3) + " more";
        }
        subMeta.setText("@" + this.supervisor.username + " · " + branchText);
        subMeta.setSingleLine(true);
        subMeta.setEllipsize(TextUtils.TruncateAt.END);
        Ui.size(subMeta, 11.0f);
        subMeta.setTextColor(0xFF94A3B8);
        info.addView(subMeta);

        int pct = stats.totalCsps > 0 ? (stats.reportedCsps * 100 / stats.totalCsps) : 0;
        Badge badge = new Badge(context);
        badge.setKind(pct >= 100 ? 1 : (pct > 0 ? 3 : 0));
        badge.setText(pct + "% DONE");
        topRow.addView(badge, wrap());

        // 2. Embedded Scorecard 4-Metric Grid
        LinearLayout scoreGrid = new LinearLayout(context);
        scoreGrid.setOrientation(0);
        scoreGrid.setGravity(Gravity.CENTER_VERTICAL);
        scoreGrid.setBackground(Ui.round(context, 0x1AFFFFFF, 10.0f, 0x33FFFFFF, 1.0f));
        Ui.pad(scoreGrid, 6, 6, 6, 6);
        LinearLayout.LayoutParams sglp = mw();
        sglp.topMargin = Ui.dp(context, 10.0f);
        scoreGrid.setLayoutParams(sglp);
        this.unifiedHeroCard.addView(scoreGrid);

        scoreGrid.addView(compactStat(context, "REPORTED", stats.reportedCsps + "/" + stats.totalCsps, pct + "% filed", 0xFF10B981), weight1());
        scoreGrid.addView(verticalBar(context));
        scoreGrid.addView(compactStat(context, "PENDING", String.valueOf(stats.pendingCsps), stats.nilCsps + " nil", 0xFFEF4444), weight1());
        scoreGrid.addView(verticalBar(context));
        scoreGrid.addView(compactStat(context, "CALLS", String.valueOf(stats.totalCalls), "follow-ups", 0xFFF59E0B), weight1());
        scoreGrid.addView(verticalBar(context));
        scoreGrid.addView(compactStat(context, "BUSINESS", String.valueOf(stats.totalBusiness), "tgt " + stats.totalTarget, 0xFF38BDF8), weight1());

        // 3. Compact Action Ribbon
        LinearLayout actRow = new LinearLayout(context);
        actRow.setOrientation(0);
        actRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actLp = mw();
        actLp.topMargin = Ui.dp(context, 10.0f);
        actRow.setLayoutParams(actLp);
        this.unifiedHeroCard.addView(actRow);

        if (this.supervisor.mobile != null && !this.supervisor.mobile.trim().isEmpty()) {
            actRow.addView(compactHeroBtn(context, R.drawable.ic_phone, "Call Staff", 0xFFF59E0B, 0x26F59E0B, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Contacts.dial(context, SupervisorAuditScreen.this.supervisor.mobile);
                }
            }), weightMargin(1.0f, 6));

            actRow.addView(compactHeroBtn(context, R.drawable.ic_whatsapp, "WhatsApp", 0xFF10B981, 0x2610B981, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Contacts.whatsapp(context, SupervisorAuditScreen.this.supervisor.mobile, "Hi " + SupervisorAuditScreen.this.supervisor.name + ", please update on pending CSP reports for " + Biz.fmtDmy(SupervisorAuditScreen.this.date) + ".");
                }
            }), weightMargin(1.0f, 6));
        }

        actRow.addView(compactHeroBtn(context, R.drawable.ic_csp, "Map CSPs", 0xFF38BDF8, 0x2638BDF8, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SupervisorAuditScreen.this.host.push(new CspSupervisorMappingScreen(context, SupervisorAuditScreen.this.host, SupervisorAuditScreen.this.supervisorId));
            }
        }), weightMargin(1.0f, 6));

        actRow.addView(compactHeroBtn(context, R.drawable.ic_trending, "Set Targets", 0xFFA855F7, 0x26A855F7, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SupervisorAuditScreen.this.host.push(new SupervisorTargetScreen(context, SupervisorAuditScreen.this.host, SupervisorAuditScreen.this.date));
            }
        }), weightMargin(1.0f, 0));
    }

    private View verticalBar(Context context) {
        View v = new View(context);
        v.setBackgroundColor(0x2EFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(context, 1.0f), Ui.dp(context, 20.0f));
        lp.gravity = Gravity.CENTER_VERTICAL;
        v.setLayoutParams(lp);
        return v;
    }

    private View compactStat(Context context, String label, String value, String sub, int accentColor) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(1);
        col.setGravity(Gravity.CENTER);
        Ui.pad(col, 2, 4, 2, 4);

        TextView lbl = new TextView(context);
        lbl.setText(label);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(lbl, 8.5f);
        lbl.setTextColor(0xFF94A3B8);
        lbl.setGravity(Gravity.CENTER);
        lbl.setSingleLine(true);
        col.addView(lbl, mw());

        TextView val = new TextView(context);
        val.setText(value);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(val, 14.5f);
        val.setTextColor(accentColor);
        val.setGravity(Gravity.CENTER);
        val.setSingleLine(true);
        col.addView(val, top(context, 1));

        TextView s = new TextView(context);
        s.setText(sub);
        Ui.size(s, 9.0f);
        s.setTextColor(0xFF94A3B8);
        s.setGravity(Gravity.CENTER);
        s.setSingleLine(true);
        col.addView(s, top(context, 1));

        return col;
    }

    private View compactHeroBtn(Context context, int iconRes, String title, int tint, int bgTint, View.OnClickListener listener) {
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(0);
        pill.setGravity(Gravity.CENTER);
        pill.setClickable(true);
        pill.setBackground(Ui.round(context, bgTint, 8.0f, tint, 1.0f));
        pill.setPadding(Ui.dp(context, 4.0f), Ui.dp(context, 6.0f), Ui.dp(context, 4.0f), Ui.dp(context, 6.0f));
        pill.setOnClickListener(listener);

        ImageView icon = new ImageView(context);
        int isize = Ui.dp(context, 13.0f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(isize, isize);
        ilp.rightMargin = Ui.dp(context, 4.0f);
        icon.setLayoutParams(ilp);
        icon.setImageResource(iconRes);
        icon.setColorFilter(tint);
        pill.addView(icon);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(t, 10.5f);
        t.setTextColor(0xFFF8FAFC);
        t.setSingleLine(true);
        pill.addView(t);

        return pill;
    }

    private void renderSubTab() {
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        this.listContainer.removeAllViews();

        if (SUB_TABS[0].equals(this.currentSubTab)) {
            renderAssignedCsps(db, context);
        } else if (SUB_TABS[1].equals(this.currentSubTab)) {
            renderCallsAndFollowups(db, context);
        } else if (SUB_TABS[2].equals(this.currentSubTab)) {
            renderSubmittedReports(db, context);
        } else {
            renderTargetBreakdown(db, context);
        }
    }

    // SUB TAB 1: ASSIGNED CSPS (PENDING & FILED)
    private void renderAssignedCsps(SQLiteDatabase db, final Context context) {
        List<SupervisorMap.SupervisorPendingItem> pendingList = SupervisorMap.getPendingCsps(db, this.date, this.supervisorId);
        List<SupervisorMap.SupervisorReportItem> reportList = SupervisorMap.getReports(db, this.date, this.supervisorId, null);

        if (pendingList.isEmpty() && reportList.isEmpty()) {
            this.empty.show(R.drawable.ic_csp, R.color.line_2, "No CSPs assigned", "Tap 'Map CSPs' above to assign CSPs to this supervisor.");
            return;
        }
        this.empty.hide();

        if (!pendingList.isEmpty()) {
            TextView pHeader = Cards.bold(context, "⚠️ Pending Unfiled CSPs (" + pendingList.size() + ")", 13.5f, R.color.danger);
            Ui.pad(pHeader, 2, 4, 2, 4);
            this.listContainer.addView(pHeader);

            for (int i = 0; i < pendingList.size(); i++) {
                final SupervisorMap.SupervisorPendingItem p = pendingList.get(i);
                LinearLayout card = Cards.card(context);
                card.setOrientation(1);
                Ui.pad(card, 12, 10, 12, 10);

                LinearLayout topRow = new LinearLayout(context);
                topRow.setOrientation(0);
                topRow.setGravity(Gravity.CENTER_VERTICAL);
                card.addView(topRow, mw());

                topRow.addView(new Avatar(context, 34).bind(p.cspName, p.cspCode), new LinearLayout.LayoutParams(Ui.dp(context, 34.0f), Ui.dp(context, 34.0f)));

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(1);
                LinearLayout.LayoutParams infoLp = weight1();
                infoLp.leftMargin = Ui.dp(context, 8.0f);
                info.setLayoutParams(infoLp);
                topRow.addView(info);

                TextView nameText = new TextView(context);
                nameText.setText(p.cspName);
                nameText.setTypeface(Typeface.DEFAULT_BOLD);
                Ui.size(nameText, 14.0f);
                nameText.setTextColor(Ui.color(context, R.color.ink));
                info.addView(nameText);

                TextView meta = new TextView(context);
                meta.setText("#" + Biz.cspCodeSuffix(p.cspCode) + " · " + p.branchName + (p.mobileNumber.isEmpty() ? "" : " · " + p.mobileNumber));
                Ui.size(meta, 11.0f);
                meta.setTextColor(Ui.color(context, R.color.ink_3));
                info.addView(meta);

                Badge unfiledBadge = new Badge(context);
                unfiledBadge.setKind(0);
                unfiledBadge.setText("UNFILED");
                topRow.addView(unfiledBadge, wrap());

                String reasonOrNote = (p.pendingReason != null && !p.pendingReason.trim().isEmpty()) ? p.pendingReason.trim() : ((p.latestNote != null && !p.latestNote.trim().isEmpty()) ? p.latestNote.trim() : "");
                if (!reasonOrNote.isEmpty()) {
                    TextView rTxt = new TextView(context);
                    rTxt.setText("⚠️ " + ((p.pendingReason != null && !p.pendingReason.trim().isEmpty()) ? "Reason: " : "Note: ") + reasonOrNote);
                    Ui.size(rTxt, 11.5f);
                    rTxt.setTextColor(0xFFDC2626);
                    rTxt.setTypeface(Typeface.DEFAULT_BOLD);
                    card.addView(rTxt, top(context, 4));
                }

                if (!p.mobileNumber.isEmpty()) {
                    LinearLayout actRow = new LinearLayout(context);
                    actRow.setOrientation(0);
                    actRow.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams alp = mw();
                    alp.topMargin = Ui.dp(context, 6.0f);
                    actRow.setLayoutParams(alp);
                    card.addView(actRow);

                    View callBtn = Cards.outlineButton(context, "📞 Call CSP", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Contacts.call(context, p.mobileNumber);
                        }
                    });
                    actRow.addView(callBtn, weightMargin(1.0f, 6));

                    View waBtn = Cards.outlineButton(context, "💬 WhatsApp", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Contacts.whatsapp(context, p.mobileNumber, "Reminder: Please submit daily report for " + Biz.fmtDmy(SupervisorAuditScreen.this.date) + ".");
                        }
                    });
                    actRow.addView(waBtn, weightMargin(1.0f, 0));
                }

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 6.0f);
                this.listContainer.addView(card, lp);
            }
        }

        if (!reportList.isEmpty()) {
            TextView rHeader = Cards.bold(context, "✓ Filed Reports (" + reportList.size() + ")", 13.5f, R.color.ok);
            Ui.pad(rHeader, 2, 10, 2, 4);
            this.listContainer.addView(rHeader);

            for (int k = 0; k < reportList.size(); k++) {
                final SupervisorMap.SupervisorReportItem r = reportList.get(k);
                LinearLayout card = Cards.card(context);
                card.setOrientation(1);
                Ui.pad(card, 12, 10, 12, 10);

                LinearLayout topRow = new LinearLayout(context);
                topRow.setOrientation(0);
                topRow.setGravity(Gravity.CENTER_VERTICAL);
                card.addView(topRow, mw());

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(1);
                topRow.addView(info, weight1());

                TextView nameText = new TextView(context);
                nameText.setText(r.cspName);
                nameText.setTypeface(Typeface.DEFAULT_BOLD);
                Ui.size(nameText, 14.0f);
                nameText.setTextColor(Ui.color(context, R.color.ink));
                info.addView(nameText);

                TextView meta = new TextView(context);
                meta.setText("#" + Biz.cspCodeSuffix(r.cspCode) + " · " + r.branchName);
                Ui.size(meta, 11.0f);
                meta.setTextColor(Ui.color(context, R.color.ink_3));
                info.addView(meta);

                Badge bizBadge = new Badge(context);
                bizBadge.setKind(1);
                bizBadge.setText("Total: " + r.totalBusiness);
                topRow.addView(bizBadge, wrap());

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = Ui.dp(context, k == 0 ? 0.0f : 6.0f);
                this.listContainer.addView(card, lp);
            }
        }
    }

    // SUB TAB 2: CALLS & FOLLOW-UPS
    private void renderCallsAndFollowups(SQLiteDatabase db, Context context) {
        List<SupervisorMap.SupervisorFollowupItem> followups = SupervisorMap.getFollowups(db, this.date, this.supervisorId, null);

        if (followups.isEmpty()) {
            this.empty.show(R.drawable.ic_phone, R.color.line_2, "No call logs recorded", "Calls placed to CSPs by this supervisor will appear here.");
            return;
        }
        this.empty.hide();

        TextView countHeader = Cards.bold(context, "Logged Calls & Communications (" + followups.size() + ")", 13.5f, R.color.ink_2);
        Ui.pad(countHeader, 2, 4, 2, 4);
        this.listContainer.addView(countHeader);

        for (int i = 0; i < followups.size(); i++) {
            SupervisorMap.SupervisorFollowupItem item = followups.get(i);
            LinearLayout card = Cards.card(context);
            card.setOrientation(1);
            Ui.pad(card, 12, 10, 12, 10);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(0);
            topRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(topRow, mw());

            ImageView icon = new ImageView(context);
            int isize = Ui.dp(context, 18.0f);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(isize, isize);
            ilp.rightMargin = Ui.dp(context, 8.0f);
            icon.setLayoutParams(ilp);
            icon.setImageResource(item.isCall() ? R.drawable.ic_phone : (item.isWhatsApp() ? R.drawable.ic_whatsapp : R.drawable.ic_note));
            icon.setColorFilter(Ui.color(context, item.isCall() ? R.color.warn : (item.isWhatsApp() ? R.color.whatsapp : R.color.primary_600)));
            topRow.addView(icon);

            LinearLayout nameCol = new LinearLayout(context);
            nameCol.setOrientation(1);
            topRow.addView(nameCol, weight1());

            TextView cspName = new TextView(context);
            cspName.setText(item.cspName);
            cspName.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(cspName, 14.0f);
            cspName.setTextColor(Ui.color(context, R.color.ink));
            nameCol.addView(cspName);

            TextView meta = new TextView(context);
            meta.setText("#" + Biz.cspCodeSuffix(item.cspCode) + " · " + item.branchName + " · " + item.actionStatus);
            Ui.size(meta, 11.0f);
            meta.setTextColor(Ui.color(context, R.color.ink_3));
            nameCol.addView(meta);

            TextView timeText = new TextView(context);
            timeText.setText(item.formattedTime());
            Ui.size(timeText, 11.0f);
            timeText.setTextColor(Ui.color(context, R.color.ink_4));
            topRow.addView(timeText, wrap());

            if (item.note != null && !item.note.trim().isEmpty()) {
                TextView noteView = new TextView(context);
                noteView.setText("💬 " + item.note.trim());
                Ui.size(noteView, 12.0f);
                noteView.setTextColor(Ui.color(context, R.color.ink_2));
                card.addView(noteView, top(context, 4));
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 6.0f);
            this.listContainer.addView(card, lp);
        }
    }

    // SUB TAB 3: SUBMITTED REPORTS
    private void renderSubmittedReports(SQLiteDatabase db, Context context) {
        List<SupervisorMap.SupervisorReportItem> reports = SupervisorMap.getReports(db, this.date, this.supervisorId, null);

        if (reports.isEmpty()) {
            this.empty.show(R.drawable.ic_reports, R.color.line_2, "No reports submitted", "Reports filed under this supervisor will be listed here.");
            return;
        }
        this.empty.hide();

        TextView countHeader = Cards.bold(context, "Submitted Daily Figures (" + reports.size() + ")", 13.5f, R.color.ink_2);
        Ui.pad(countHeader, 2, 4, 2, 4);
        this.listContainer.addView(countHeader);

        for (int i = 0; i < reports.size(); i++) {
            SupervisorMap.SupervisorReportItem r = reports.get(i);
            LinearLayout card = Cards.card(context);
            card.setOrientation(1);
            Ui.pad(card, 12, 10, 12, 10);

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
            Ui.size(cspName, 14.0f);
            cspName.setTextColor(Ui.color(context, R.color.ink));
            nameCol.addView(cspName);

            TextView meta = new TextView(context);
            meta.setText("#" + Biz.cspCodeSuffix(r.cspCode) + " · " + r.branchName);
            Ui.size(meta, 11.0f);
            meta.setTextColor(Ui.color(context, R.color.ink_3));
            nameCol.addView(meta);

            Badge totalBadge = new Badge(context);
            totalBadge.setKind(1);
            totalBadge.setText("Total: " + r.totalBusiness);
            topRow.addView(totalBadge, wrap());

            if (r.remarks != null && !r.remarks.trim().isEmpty()) {
                TextView remarksView = new TextView(context);
                remarksView.setText("Remarks: " + r.remarks.trim());
                Ui.size(remarksView, 12.0f);
                remarksView.setTextColor(Ui.color(context, R.color.ink_3));
                card.addView(remarksView, top(context, 4));
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 6.0f);
            this.listContainer.addView(card, lp);
        }
    }

    // SUB TAB 4: TARGET BREAKDOWN MATRIX
    private void renderTargetBreakdown(SQLiteDatabase db, Context context) {
        SupervisorMap.SupervisorStats stats = SupervisorMap.getStatsForSupervisor(db, this.supervisor, this.date);

        LinearLayout card = Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 12, 10, 12, 10);

        TextView title = Cards.bold(context, "Parameter Target vs Actual Matrix", 14.0f, R.color.ink);
        card.addView(title);

        LinearLayout table = new LinearLayout(context);
        table.setOrientation(1);
        LinearLayout.LayoutParams tlp = mw();
        tlp.topMargin = Ui.dp(context, 8.0f);
        table.setLayoutParams(tlp);
        card.addView(table);

        table.addView(buildMatrixRow(context, "PARAMETER", "ACHIEVED", "TARGET", "STATUS", true, 0));

        List<Params.P> paramList = Params.all(db);
        for (Params.P p : paramList) {
            int achieved = stats.paramTotals.containsKey(p.key) ? stats.paramTotals.get(p.key) : 0;
            int target = stats.paramTargets.containsKey(p.key) ? stats.paramTargets.get(p.key) : 0;
            int pct = target > 0 ? (achieved * 100 / target) : (achieved > 0 ? 100 : 0);
            String statusStr = target > 0 ? (pct + "%") : (achieved > 0 ? "✓ Done" : "—");
            table.addView(buildMatrixRow(context, p.label, String.valueOf(achieved), String.valueOf(target), statusStr, false, pct));
        }

        this.listContainer.addView(card, mw());
    }

    private View buildMatrixRow(Context context, String p1, String p2, String p3, String p4, boolean isHeader, int pct) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(isHeader ? Ui.round(context, 0xFFF1F5F9, 6.0f) : null);
        Ui.pad(row, 6, 5, 6, 5);

        TextView c1 = new TextView(context);
        c1.setText(p1);
        c1.setTypeface(isHeader ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        Ui.size(c1, 11.5f);
        c1.setTextColor(Ui.color(context, isHeader ? R.color.ink_3 : R.color.ink));
        row.addView(c1, weightRatio(1.8f));

        TextView c2 = new TextView(context);
        c2.setText(p2);
        c2.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(c2, 11.5f);
        c2.setTextColor(Ui.color(context, isHeader ? R.color.ink_3 : (pct >= 100 ? R.color.ok : R.color.ink)));
        c2.setGravity(Gravity.CENTER);
        row.addView(c2, weightRatio(1.0f));

        TextView c3 = new TextView(context);
        c3.setText(p3);
        Ui.size(c3, 11.5f);
        c3.setTextColor(Ui.color(context, isHeader ? R.color.ink_3 : R.color.ink_3));
        c3.setGravity(Gravity.CENTER);
        row.addView(c3, weightRatio(1.0f));

        TextView c4 = new TextView(context);
        c4.setText(p4);
        c4.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(c4, 11.0f);
        c4.setTextColor(Ui.color(context, isHeader ? R.color.ink_3 : (pct >= 100 ? R.color.ok : (pct > 0 ? R.color.warn : R.color.danger))));
        c4.setGravity(Gravity.RIGHT);
        row.addView(c4, weightRatio(1.2f));

        return row;
    }

    private LinearLayout.LayoutParams weightRatio(float w) {
        return new LinearLayout.LayoutParams(0, -2, w);
    }

    private LinearLayout.LayoutParams weightMargin(float w, int rightMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, w);
        if (rightMarginDp > 0) {
            lp.rightMargin = Ui.dp(getContext(), rightMarginDp);
        }
        return lp;
    }

    private LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
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
