package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Journal;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.ui.widget.Section;
import org.cspdaily.report.ui.widget.Timeline;
import org.cspdaily.report.util.PhoneActions;

/* loaded from: classes.dex */
public class CspProfileScreen extends Screen {
    private final LinearLayout activity;
    private final long cspId;
    private final String date;
    private final LinearLayout details;
    private final LinearLayout identity;
    private final LinearLayout tabContainer;
    private final LinearLayout metrics;
    private final Empty missing;
    private final Section secActivity;
    private final Section secDetails;
    private final Section secReport;
    private int activeTab = 0; // 0: Today's Report, 1: Profile Details, 2: Activity

    public CspProfileScreen(Context context, Screen.Host host, long j, String str) {
        super(context, host);
        this.cspId = j;
        this.date = str;
        title("CSP profile");
        subtitle(Biz.friendlyDate(str));
        this.missing = new Empty(context);
        add(this.missing, 0);
        this.identity = new LinearLayout(context);
        this.identity.setOrientation(1);
        add(this.identity, 0);

        this.tabContainer = new LinearLayout(context);
        this.tabContainer.setOrientation(1);
        add(this.tabContainer, 10);

        this.secReport = new Section(context);
        add(this.secReport, 4);
        this.metrics = new LinearLayout(context);
        this.metrics.setOrientation(1);
        add(this.metrics, 0);

        this.secDetails = new Section(context);
        add(this.secDetails, 4);
        this.details = new LinearLayout(context);
        this.details.setOrientation(1);
        add(this.details, 0);

        this.secActivity = new Section(context);
        add(this.secActivity, 4);
        this.activity = new LinearLayout(context);
        this.activity.setOrientation(1);
        add(this.activity, 0);
    }

    private void renderTabs(final Context context) {
        this.tabContainer.removeAllViews();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(16);

        final String[] tabLabels = new String[]{"📊 Today's Report", "👤 Profile Details", "⏱️ Activity"};
        for (int i = 0; i < tabLabels.length; i++) {
            final int index = i;
            TextView tabBtn = new TextView(context);
            tabBtn.setText(tabLabels[i]);
            tabBtn.setGravity(17);
            Ui.size(tabBtn, 12.0f);
            tabBtn.setTypeface(Typeface.DEFAULT_BOLD);
            boolean isSelected = (this.activeTab == index);
            if (isSelected) {
                tabBtn.setTextColor(Ui.color(context, R.color.paper));
                tabBtn.setBackground(Ui.round(context, Ui.color(context, R.color.primary_800), 12.0f, Ui.color(context, R.color.primary_800), 1.0f));
            } else {
                tabBtn.setTextColor(Ui.color(context, R.color.ink_3));
                tabBtn.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line), 1.0f));
            }
            Ui.pad(tabBtn, 6, 8, 6, 8);
            tabBtn.setClickable(true);
            tabBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CspProfileScreen.this.activeTab = index;
                    CspProfileScreen.this.renderTabs(context);
                    CspProfileScreen.this.applyTabVisibility();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            if (i > 0) {
                lp.leftMargin = Ui.dp(context, 6.0f);
            }
            row.addView(tabBtn, lp);
        }
        this.tabContainer.addView(row, mw());
    }

    private void applyTabVisibility() {
        boolean showReport = (this.activeTab == 0);
        boolean showDetails = (this.activeTab == 1);
        boolean showActivity = (this.activeTab == 2);

        this.secReport.setVisibility(showReport ? 0 : 8);
        this.metrics.setVisibility(showReport ? 0 : 8);

        this.secDetails.setVisibility(showDetails ? 0 : 8);
        this.details.setVisibility(showDetails ? 0 : 8);

        this.secActivity.setVisibility(showActivity ? 0 : 8);
        this.activity.setVisibility(showActivity ? 0 : 8);
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return "CSP profile";
    }

    @Override // org.cspdaily.report.ui.Screen
    public void refreshContent() {
        final CspDay cspDay;
        String str;
        Context context = getContext();
        SQLiteDatabase dbRead = Core.get(context).dbRead();
        Iterator<CspDay> it = Day.dayData(dbRead, this.date, false).iterator();
        while (true) {
            if (!it.hasNext()) {
                cspDay = null;
                break;
            }
            CspDay next = it.next();
            if (next.id == this.cspId) {
                cspDay = next;
                break;
            }
        }
        if (cspDay == null) {
            this.missing.show(R.drawable.ic_alert, R.color.danger, "CSP not found", "It may have been deleted from the register.");
            this.identity.setVisibility(8);
            this.tabContainer.setVisibility(8);
            this.secReport.setVisibility(8);
            this.metrics.setVisibility(8);
            this.secDetails.setVisibility(8);
            this.details.setVisibility(8);
            this.secActivity.setVisibility(8);
            this.activity.setVisibility(8);
            return;
        }
        this.missing.hide();
        this.identity.setVisibility(0);
        this.tabContainer.setVisibility(0);
        renderTabs(context);
        applyTabVisibility();
        title(cspDay.name);
        subtitle("#" + cspDay.suffix() + " · " + Biz.friendlyDate(this.date));
        this.identity.removeAllViews();
        this.identity.addView(header(context, cspDay), mw());
        this.identity.addView(actions(context, cspDay), top(context, 12));
        this.secReport.set(cspDay.reported ? "Today's report · filed" : "Today's report · pending");
        this.metrics.removeAllViews();
        LinearLayout card = Cards.card(context);
        LinearLayout linearLayout = null;
        int i = 0;
        Iterator<Map.Entry<String, Integer>> it2 = cspDay.values.entrySet().iterator();
        while (true) {
            int i2 = i;
            if (!it2.hasNext()) {
                break;
            }
            Map.Entry<String, Integer> next2 = it2.next();
            if (i2 % 2 == 0) {
                LinearLayout linearLayout2 = new LinearLayout(context);
                linearLayout2.setOrientation(0);
                card.addView(linearLayout2, mw());
                linearLayout = linearLayout2;
            }
            Integer num = cspDay.targets.get(next2.getKey());
            int intValue = num == null ? 0 : num.intValue();
            linearLayout.addView(metricTile(context, Params.shortOf(dbRead, next2.getKey()), Integer.toString(next2.getValue().intValue()), intValue > 0 ? "target " + intValue : null), weight1());
            i = i2 + 1;
        }
        if (cspDay.values.isEmpty()) {
            card.addView(Cards.label(context, "No report parameters configured.", R.style.Text_Meta, R.color.ink_4), mw());
        }
        this.metrics.addView(card, mw());
        LinearLayout card2 = Cards.card(context);
        card2.setOrientation(0);
        card2.setGravity(16);
        if (cspDay.reported) {
            str = "Filed at " + Biz.shortWhen(cspDay.submittedAt);
        } else {
            String reasonOrPromise = cspDay.effectiveReasonOrPromise();
            str = reasonOrPromise.isEmpty() ? "Not reported · " + cspDay.followupStatus : reasonOrPromise;
        }
        card2.addView(Cards.label(context, str, R.style.Text_Meta, cspDay.reported ? R.color.ok : R.color.danger), weight1());
        card2.addView(Cards.bold(context, "Total " + cspDay.totalBusiness, 15.0f, R.color.ink), wrap());
        this.metrics.addView(card2, top(context, 8));
        this.secDetails.set("Profile details");
        this.details.removeAllViews();
        LinearLayout cardFlat = Cards.cardFlat(context);
        cardFlat.addView(detail(context, "CSP code", cspDay.code));
        cardFlat.addView(detail(context, "Short id", "#" + cspDay.suffix()));
        cardFlat.addView(detail(context, "Gender", "F".equalsIgnoreCase(cspDay.gender) ? "Female" : "Male"));
        cardFlat.addView(detail(context, "Mobile", cspDay.mobile));
        cardFlat.addView(detail(context, "Branch", cspDay.branch + (cspDay.branchCode.isEmpty() ? "" : " (" + cspDay.branchCode + ")")));
        cardFlat.addView(detail(context, "Status", Csp.ACTIVE.equals(cspDay.status) ? "Active" : "Inactive"));
        cardFlat.addView(detail(context, "Daily target", Integer.toString(cspDay.dailyTarget)));
        cardFlat.addView(detail(context, "Last report", cspDay.hasLastReport ? Biz.fmtDmy(cspDay.lastReportDate) + " · total " + cspDay.lastReportTotal : "none"));
        if (!cspDay.notes.isEmpty()) {
            cardFlat.addView(detail(context, "Notes", cspDay.notes));
        }
        this.details.addView(cardFlat, mw());
        LinearLayout linearLayout3 = new LinearLayout(context);
        linearLayout3.setOrientation(0);
        linearLayout3.addView(Cards.outlineButton(context, "Business day", new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                CspProfileScreen.this.host.push(new BusinessDayScreen(CspProfileScreen.this.getContext(), CspProfileScreen.this.host, CspProfileScreen.this.date));
            }
        }), mw());
        this.details.addView(linearLayout3, top(context, 10));

        this.secActivity.set("Activity");
        this.activity.removeAllViews();
        List<Journal.Entry> recent = Journal.recent(dbRead, 10, Long.valueOf(this.cspId));
        if (recent.isEmpty()) {
            org.cspdaily.report.ui.widget.Empty empty = new org.cspdaily.report.ui.widget.Empty(context);
            empty.show(R.drawable.ic_history, R.color.line_2, "No activity yet", "Reports and follow-ups for this CSP will appear here.");
            this.activity.addView(empty, mw());
        } else {
            LinearLayout cardFlat2 = Cards.cardFlat(context);
            for (Journal.Entry entry : recent) {
                cardFlat2.addView(org.cspdaily.report.ui.widget.Timeline.row(context, entry, false), mw());
            }
            this.activity.addView(cardFlat2, mw());
        }
    }

    private View header(Context context, CspDay cspDay) {
        LinearLayout card = Cards.card(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        card.addView(linearLayout, mw());
        linearLayout.addView(new Avatar(context, 44).bind(cspDay.name, cspDay.code), new LinearLayout.LayoutParams(Ui.dp(context, 44.0f), Ui.dp(context, 44.0f)));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(1);
        LinearLayout.LayoutParams weight1 = weight1();
        weight1.leftMargin = Ui.dp(context, 12.0f);
        linearLayout.addView(linearLayout2, weight1);
        TextView textView = new TextView(context);
        textView.setText(cspDay.name);
        textView.setTextAppearance(context, R.style.Text_CardTitle);
        Ui.size(textView, 17.0f);
        textView.setMaxLines(2);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textView, mw());
        StringBuilder sb = new StringBuilder();
        sb.append(cspDay.code).append("  ·  #").append(cspDay.suffix());
        if (!cspDay.branch.isEmpty()) {
            sb.append("  ·  ").append(cspDay.branch);
        }
        linearLayout2.addView(Cards.label(context, sb.toString(), R.style.Text_Meta, R.color.ink_3), mw());
        if (!cspDay.mobile.isEmpty()) {
            linearLayout2.addView(Cards.label(context, cspDay.mobile, R.style.Text_Body, R.color.primary_700), top(context, 2));
        }
        Badge badge = new Badge(context);
        if (!Csp.ACTIVE.equals(cspDay.status)) {
            badge.setKind(0);
            badge.setText("Inactive");
        } else if (cspDay.reported && !cspDay.isNil) {
            badge.setKind(1);
            badge.setText("Reported");
        } else if (cspDay.isNil) {
            badge.setKind(4);
            badge.setText("Nil");
        } else {
            badge.setKind(2);
            badge.setText("Pending");
        }
        linearLayout.addView(badge, wrap());
        return card;
    }

    private View actions(final Context context, final CspDay cspDay) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(1);

        // Row 1: Primary Report & Follow-up actions
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(0);
        row1.addView(Cards.primaryButton(context, cspDay.reported ? "Edit report" : "File report", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportEntrySheet.open((Activity) CspProfileScreen.this.getContext(), CspProfileScreen.this.host, cspDay, CspProfileScreen.this.date);
            }
        }), weight1());

        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));
        row1.addView(spacer);

        row1.addView(Cards.tonalButton(context, "Follow-up", new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                FollowupSheet.open((Activity) CspProfileScreen.this.getContext(), CspProfileScreen.this.host, cspDay, CspProfileScreen.this.date);
            }
        }), weight1());
        container.addView(row1, mw());

        // Row 2: Direct Call, WhatsApp & Master Edit actions
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(0);
        LinearLayout.LayoutParams row2Lp = mw();
        row2Lp.topMargin = Ui.dp(context, 8.0f);
        row2.setLayoutParams(row2Lp);

        if (cspDay.mobile != null && !cspDay.mobile.trim().isEmpty()) {
            TextView callBtn = Cards.outlineButton(context, "📞 Call", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PhoneActions.call(context, cspDay);
                }
            });
            row2.addView(callBtn, weight1());

            View sp2 = new View(context);
            sp2.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 6.0f), 1));
            row2.addView(sp2);

            TextView waBtn = Cards.whatsappButton(context, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PhoneActions.whatsapp(context, cspDay);
                }
            });
            row2.addView(waBtn, weight1());

            View sp3 = new View(context);
            sp3.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 6.0f), 1));
            row2.addView(sp3);
        }

        TextView editBtn = Cards.outlineButton(context, "Edit CSP", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CspProfileScreen.this.host.push(new CspEditScreen(context, CspProfileScreen.this.host, cspDay.id));
            }
        });
        row2.addView(editBtn, weight1());

        container.addView(row2);
        return container;
    }

    private View metricTile(Context context, String str, String str2, String str3) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(1);
        Ui.pad(linearLayout, 4, 8, 4, 8);
        TextView textView = new TextView(context);
        textView.setText(str2);
        Ui.size(textView, 19.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, "0".equals(str2) ? R.color.ink_4 : R.color.ink));
        linearLayout.addView(textView, mw());
        TextView textView2 = new TextView(context);
        textView2.setText(str);
        Ui.size(textView2, 11.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(Ui.color(context, R.color.ink_3));
        textView2.setSingleLine(true);
        textView2.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(textView2, mw());
        if (str3 != null) {
            linearLayout.addView(Cards.label(context, str3, R.style.Text_Nano, R.color.ink_4), mw());
        }
        return linearLayout;
    }

    private View detail(Context context, String str, String str2) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        Ui.pad(linearLayout2, 14, 10, 14, 10);
        linearLayout2.addView(Cards.label(context, str, R.style.Text_Label, R.color.ink_3), new LinearLayout.LayoutParams(0, -2, 1.1f));
        TextView textView = new TextView(context);
        if (str2 == null || str2.isEmpty()) {
            str2 = "—";
        }
        textView.setText(str2);
        textView.setTextAppearance(context, R.style.Text_Body);
        textView.setTextColor(Ui.color(context, R.color.ink));
        textView.setGravity(8388613);
        textView.setMaxLines(3);
        linearLayout2.addView(textView, new LinearLayout.LayoutParams(0, -2, 1.4f));
        linearLayout.addView(linearLayout2, mw());
        View view = new View(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        layoutParams.leftMargin = Ui.dp(context, 14.0f);
        layoutParams.rightMargin = Ui.dp(context, 14.0f);
        view.setLayoutParams(layoutParams);
        view.setBackgroundColor(Ui.color(context, R.color.line));
        linearLayout.addView(view, mw());
        return linearLayout;
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
