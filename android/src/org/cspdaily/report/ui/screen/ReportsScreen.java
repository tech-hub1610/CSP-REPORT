package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Agg;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.DaySummary;
import org.cspdaily.report.domain.Texts;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Kpi;
import org.cspdaily.report.ui.widget.Ring;
import org.cspdaily.report.util.Contacts;
import org.cspdaily.report.util.Files;

public class ReportsScreen extends Screen {
    private static final String[] TABS = {"Day", "Pending", "Self-Targets", "Monthly", "CSP-wise", "Branch-wise"};
    private static final String[] CSP_SCOPES = {"Date-wise", "Monthly"};
    
    private final LinearLayout content;
    private String date;
    private String month;
    private final TextView preview;
    private final TextView previewLabel;
    private String tab;
    private int cspScope = 0; // 0 = Date-wise, 1 = Monthly
    private final Chips tabs;

    public ReportsScreen(Context context, Screen.Host host) {
        super(context, host);
        this.tab = TABS[0];
        title("Reports");
        showBack(false);
        this.tabs = new Chips(context);
        this.tabs.set(TABS);
        this.tabs.allowNone(false);
        this.tabs.select(0);
        this.tabs.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                ReportsScreen.this.tab = str;
                ReportsScreen.this.refreshContent();
            }
        });
        add(this.tabs, 0);
        this.content = new LinearLayout(context);
        this.content.setOrientation(1);
        add(this.content, 10);
        LinearLayout card = Cards.card(context);
        card.setPadding(0, 0, 0, 0);
        add(card, 10);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        Ui.pad(linearLayout, 14, 12, 14, 8);
        card.addView(linearLayout, mw());
        this.previewLabel = Cards.label(context, "Group text preview", R.style.Text_Section, R.color.ink_3);
        linearLayout.addView(this.previewLabel, weight1());
        linearLayout.addView(Cards.link(context, "Copy", R.color.primary_600, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportsScreen.this.copy(ReportsScreen.this.preview.getText().toString());
            }
        }), wrap());
        this.preview = new TextView(context);
        this.preview.setTextAppearance(context, R.style.Text_Body);
        this.preview.setTextColor(Ui.color(context, R.color.ink_2));
        this.preview.setTypeface(Typeface.MONOSPACE);
        Ui.size(this.preview, 11.5f);
        this.preview.setTextIsSelectable(true);
        Ui.pad(this.preview, 14, 0, 14, 14);
        card.addView(this.preview, mw());
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        Ui.pad(linearLayout2, 14, 10, 14, 14);
        TextView textView = Cards.tonalButton(context, "Share text", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Contacts.shareText(ReportsScreen.this.getContext(), ReportsScreen.this.shareTitle(), ReportsScreen.this.preview.getText().toString());
            }
        });
        LinearLayout.LayoutParams weight1 = weight1();
        weight1.rightMargin = Ui.dp(context, 8.0f);
        linearLayout2.addView(textView, weight1);
        linearLayout2.addView(Cards.outlineButton(context, "Export CSV", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportsScreen.this.exportCsv();
            }
        }), weight1());
        card.addView(linearLayout2, mw());
    }

    @Override
    public String headline() {
        return "Reports";
    }

    @Override
    protected boolean canGoBack() {
        return false;
    }

    private String shareTitle() {
        if (TABS[0].equals(this.tab)) {
            return "CSP Daily " + Biz.fmtDmy(this.date);
        } else if (TABS[1].equals(this.tab)) {
            return "CSP Pending Followup - " + Biz.fmtDmy(this.date);
        } else if (TABS[2].equals(this.tab)) {
            return "Self-Target by CSPs - " + Biz.fmtDmy(this.date);
        } else if (TABS[4].equals(this.tab)) {
            if (this.cspScope == 0) {
                return "CSP Daily Report " + Biz.fmtDmy(this.date);
            } else {
                return "CSP Monthly Report " + Biz.fmtMonth(this.month);
            }
        } else if (TABS[5].equals(this.tab)) {
            return "CSP Branch-wise " + Biz.fmtMonth(this.month);
        } else {
            return "CSP Monthly " + Biz.fmtMonth(this.month);
        }
    }

    @Override
    public void refreshContent() {
        String outputText;
        Context context = getContext();
        SQLiteDatabase dbRead = Core.get(context).dbRead();
        if (this.date == null) {
            this.date = UiState.date(context);
        }
        if (this.month == null) {
            List<String> months = Agg.months(dbRead, 12);
            this.month = months.isEmpty() ? Biz.today().substring(0, 7) : months.get(0);
        }

        boolean isDateBased = TABS[0].equals(this.tab) || TABS[1].equals(this.tab) || TABS[2].equals(this.tab) || (TABS[4].equals(this.tab) && this.cspScope == 0);
        String subText = this.tab;
        if (TABS[4].equals(this.tab)) {
            subText = "CSP-wise (" + CSP_SCOPES[this.cspScope] + ")";
        }
        subtitle(subText + " · " + (isDateBased ? Biz.fmtDmy(this.date) : Biz.fmtMonth(this.month)));
        
        this.content.removeAllViews();
        if (TABS[0].equals(this.tab)) {
            this.content.addView(dateBar(context, dbRead), mw());
            outputText = dayReport(context, dbRead);
        } else if (TABS[1].equals(this.tab)) {
            this.content.addView(dateBar(context, dbRead), mw());
            outputText = Texts.pendingList(dbRead, this.date);
            this.content.addView(Cards.label(context, "Pending = an active CSP with no report row at all. Nil-figure CSPs are counted separately, exactly as in the web app.", R.style.Text_Meta, R.color.ink_4), top(context, 10));
        } else if (TABS[2].equals(this.tab)) {
            outputText = selfTargetsReport(context, dbRead);
        } else if (TABS[4].equals(this.tab)) {
            this.content.addView(cspScopeBar(context), mw());
            if (this.cspScope == 0) {
                this.content.addView(dateBar(context, dbRead), top(context, 8));
                outputText = cspWiseDate(dbRead);
            } else {
                this.content.addView(monthBar(context, dbRead), top(context, 8));
                outputText = cspWiseMonth(dbRead);
            }
        } else if (TABS[5].equals(this.tab)) {
            this.content.addView(monthBar(context, dbRead), mw());
            outputText = branchWise(dbRead);
        } else {
            this.content.addView(monthBar(context, dbRead), mw());
            outputText = monthly(dbRead);
        }
        this.preview.setText(outputText);
    }

    private View cspScopeBar(Context context) {
        Chips scopeChips = new Chips(context);
        scopeChips.set(CSP_SCOPES);
        scopeChips.allowNone(false);
        scopeChips.select(this.cspScope);
        scopeChips.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                ReportsScreen.this.cspScope = i;
                ReportsScreen.this.refreshContent();
            }
        });
        return scopeChips;
    }

    private View dateBar(final Context context, final SQLiteDatabase sQLiteDatabase) {
        LinearLayout nav = new LinearLayout(context);
        nav.setOrientation(0);
        nav.setGravity(16);
        nav.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(nav, 12, 8, 12, 8);

        TextView prev = Cards.link(context, "◀ Prev", R.color.primary_700, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReportsScreen.this.date = Biz.plusDays(ReportsScreen.this.date, -1);
                UiState.setDate(context, ReportsScreen.this.date);
                ReportsScreen.this.refreshContent();
            }
        });
        nav.addView(prev, wrap());

        TextView dateLabel = new TextView(context);
        dateLabel.setGravity(17);
        dateLabel.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(dateLabel, 14.5f);
        dateLabel.setTextColor(Ui.color(context, R.color.ink));
        String label = Biz.friendlyDate(this.date) + " (" + Biz.fmtDmy(this.date) + ")  ▾";
        dateLabel.setText(label);
        dateLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReportsScreen.this.pickDate();
            }
        });
        nav.addView(dateLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));

        if (!Biz.isToday(this.date)) {
            TextView todayBtn = Cards.link(context, "Today ↺", R.color.primary_700, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReportsScreen.this.date = Biz.today();
                    UiState.setDate(context, ReportsScreen.this.date);
                    ReportsScreen.this.refreshContent();
                }
            });
            LinearLayout.LayoutParams tLp = wrap();
            tLp.rightMargin = Ui.dp(context, 8.0f);
            nav.addView(todayBtn, tLp);
        }

        TextView next = Cards.link(context, "Next ▶", R.color.primary_700, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReportsScreen.this.date = Biz.plusDays(ReportsScreen.this.date, 1);
                UiState.setDate(context, ReportsScreen.this.date);
                ReportsScreen.this.refreshContent();
            }
        });
        nav.addView(next, wrap());

        return nav;
    }

    private void pickDate() {
        Calendar cal = Biz.cal(this.date);
        DatePickerDialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                ReportsScreen.this.date = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                UiState.setDate(ReportsScreen.this.getContext(), ReportsScreen.this.date);
                ReportsScreen.this.refreshContent();
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private View monthBar(Context context, final SQLiteDatabase sQLiteDatabase) {
        LinearLayout card = Cards.card(context);
        card.setOrientation(0);
        card.setGravity(16);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.addView(Cards.label(context, "Period", R.style.Text_Section, R.color.ink_3), mw());
        linearLayout.addView(Cards.bold(context, Biz.fmtMonth(this.month), 16.0f, R.color.ink), top(context, 1));
        card.addView(linearLayout, weight1());
        card.addView(Cards.link(context, "Change", R.color.primary_600, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportsScreen.this.pickMonth(sQLiteDatabase);
            }
        }), wrap());
        return card;
    }

    private void pickMonth(SQLiteDatabase sQLiteDatabase) {
        final List<String> months = Agg.months(sQLiteDatabase, 12);
        if (months.isEmpty()) {
            this.host.toast("No reports saved yet");
            return;
        }
        String[] strArr = new String[months.size()];
        for (int i = 0; i < months.size(); i++) {
            strArr[i] = Biz.fmtMonth(months.get(i));
        }
        new AlertDialog.Builder(getContext()).setTitle("Period").setItems(strArr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                ReportsScreen.this.month = months.get(i3);
                ReportsScreen.this.refreshContent();
            }
        }).show();
    }

    private String cspWiseDate(SQLiteDatabase sQLiteDatabase) {
        Context context = getContext();
        List<String> chosenCols = chosenCols(sQLiteDatabase);
        Map<String, String> shorts = Params.shorts(sQLiteDatabase);
        Map<String, String> labels = Params.labels(sQLiteDatabase);
        this.content.addView(paramBar(context, sQLiteDatabase, chosenCols), top(context, 10));

        List<CspDay> dayData = Day.dayData(sQLiteDatabase, this.date, true);
        LinearLayout cardFlat = Cards.cardFlat(context);
        this.content.addView(cardFlat, top(context, 10));

        int[] iArr = new int[chosenCols.size()];
        StringBuilder sb = new StringBuilder();
        sb.append("ID\n");
        if (dayData.isEmpty()) {
            cardFlat.addView(Cards.label(context, "No active CSPs in the register.", R.style.Text_Body, R.color.ink_4), mw());
            return appendDateTotals(sb, chosenCols, labels, iArr).toString().trim();
        }

        int notReportedCount = 0;
        int sl = 0;
        for (CspDay csp : dayData) {
            sl++;
            boolean reported = csp.reported;
            if (!reported) {
                notReportedCount++;
            }

            StringBuilder paramText = new StringBuilder();
            for (int k = 0; k < chosenCols.size(); k++) {
                String col = chosenCols.get(k);
                if (k > 0) {
                    paramText.append(", ");
                }
                Integer val = (csp.values != null) ? csp.values.get(col) : null;
                int intVal = (reported && val != null) ? val.intValue() : 0;
                paramText.append(shorts.get(col)).append(' ').append(intVal);
                iArr[k] += intVal;
            }

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(0);
            linearLayout.setGravity(48);
            Ui.pad(linearLayout, 14, 9, 14, 9);

            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setOrientation(1);
            linearLayout2.addView(Cards.bold(context, sl + ". " + csp.name, 14.0f, R.color.ink), mw());

            String statusSub = csp.branch + " · " + Biz.cspCodeSuffix(csp.code) + (reported ? (csp.isNil ? " · Nil Figure" : "") : " · no report");
            int subColor = !reported ? R.color.danger : (csp.isNil ? R.color.info : R.color.ink_3);
            linearLayout2.addView(Cards.label(context, statusSub, R.style.Text_Meta, subColor), top(context, 1));
            linearLayout2.addView(Cards.label(context, paramText.toString(), R.style.Text_Meta, R.color.ink_2), top(context, 4));
            linearLayout.addView(linearLayout2, weight1());

            int totalBusiness = reported ? csp.totalBusiness : 0;
            linearLayout.addView(Cards.bold(context, Integer.toString(totalBusiness), 15.0f, reported ? (csp.isNil ? R.color.info : R.color.primary_700) : R.color.ink_4), fixed(context, 62));
            cardFlat.addView(linearLayout, mw());

            if (sl < dayData.size()) {
                cardFlat.addView(Cards.rule(context, R.color.line, 0, 0), mw());
            }

            sb.append(sl).append(". ").append(Biz.cspCodeSuffix(csp.code)).append(" - ").append(paramText.toString()).append("\n");
        }

        cardFlat.addView(foot(context, dayData.size() + " active CSPs · " + notReportedCount + " not reporting"), mw());
        return appendDateTotals(sb, chosenCols, labels, iArr).toString().trim();
    }

    private StringBuilder appendDateTotals(StringBuilder sb, List<String> list, Map<String, String> map, int[] iArr) {
        sb.append("\nBusiness Figure Format\n");
        sb.append("Dated: ").append(Biz.fmtDmy(this.date)).append("\n\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append(map.get(list.get(i))).append(i == 0 ? " : " : ": ").append(iArr[i]).append("\n");
        }
        return sb;
    }

    private String cspWiseMonth(SQLiteDatabase sQLiteDatabase) {
        Context context = getContext();
        List<String> chosenCols = chosenCols(sQLiteDatabase);
        Map<String, String> shorts = Params.shorts(sQLiteDatabase);
        Map<String, String> labels = Params.labels(sQLiteDatabase);
        this.content.addView(paramBar(context, sQLiteDatabase, chosenCols), top(context, 10));
        List<Map<String, String>> cspMonth = Agg.cspMonth(sQLiteDatabase, this.month);
        LinearLayout cardFlat = Cards.cardFlat(context);
        this.content.addView(cardFlat, top(context, 10));
        int[] iArr = new int[chosenCols.size()];
        StringBuilder sb = new StringBuilder();
        sb.append("ID\n");
        if (cspMonth.isEmpty()) {
            cardFlat.addView(Cards.label(context, "No active CSPs in the register.", R.style.Text_Body, R.color.ink_4), mw());
            return appendMonthTotals(sb, chosenCols, labels, iArr).toString().trim();
        }
        int i = 0;
        int i2 = 0;
        for (Map<String, String> map : cspMonth) {
            int parseInt = parseInt(map.get("total_business"));
            boolean z = parseInt(map.get("days")) > 0;
            i2++;
            if (!z) {
                i++;
            }
            String paramText = paramText(map, chosenCols, shorts);
            int i3 = 0;
            while (true) {
                int i4 = i3;
                if (i4 >= chosenCols.size()) {
                    break;
                }
                iArr[i4] = parseInt(map.get(chosenCols.get(i4))) + iArr[i4];
                i3 = i4 + 1;
            }
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(0);
            linearLayout.setGravity(48);
            Ui.pad(linearLayout, 14, 9, 14, 9);
            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setOrientation(1);
            linearLayout2.addView(Cards.bold(context, i2 + ". " + map.get("csp_name"), 14.0f, R.color.ink), mw());
            linearLayout2.addView(Cards.label(context, map.get("branch_name") + " · " + Biz.cspCodeSuffix(map.get("csp_code")) + (z ? (" · " + map.get("days") + " days") : " · no report"), R.style.Text_Meta, z ? R.color.ink_3 : R.color.danger), top(context, 1));
            linearLayout2.addView(Cards.label(context, paramText, R.style.Text_Meta, R.color.ink_2), top(context, 4));
            linearLayout.addView(linearLayout2, weight1());
            linearLayout.addView(Cards.bold(context, Integer.toString(parseInt), 15.0f, z ? R.color.primary_700 : R.color.ink_4), fixed(context, 62));
            cardFlat.addView(linearLayout, mw());
            if (i2 < cspMonth.size()) {
                cardFlat.addView(Cards.rule(context, R.color.line, 0, 0), mw());
            }
            sb.append(i2).append(". ").append(Biz.cspCodeSuffix(map.get("csp_code"))).append(" - ").append(paramText).append("\n");
        }
        cardFlat.addView(foot(context, cspMonth.size() + " active CSPs · " + i + " not reporting"), mw());
        return appendMonthTotals(sb, chosenCols, labels, iArr).toString().trim();
    }

    private StringBuilder appendMonthTotals(StringBuilder sb, List<String> list, Map<String, String> map, int[] iArr) {
        sb.append("\nBusiness Figure Format\n");
        sb.append("Period: ").append(Biz.fmtMonth(this.month)).append("\n\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append(map.get(list.get(i))).append(i == 0 ? " : " : ": ").append(iArr[i]).append("\n");
        }
        return sb;
    }

    private String selfTargetsReport(Context context, SQLiteDatabase sQLiteDatabase) {
        this.content.addView(dateBar(context, sQLiteDatabase), mw());

        List<CspDay> dayData = Day.dayData(sQLiteDatabase, this.date, true);
        List<Params.P> params = Params.all(sQLiteDatabase);

        int totalTargets = 0;
        int cspsWithTarget = 0;
        Map<String, Integer> paramTotals = new LinkedHashMap<>();
        for (Params.P p : params) {
            paramTotals.put(p.key, 0);
        }

        for (CspDay csp : dayData) {
            if (csp.dailyTarget > 0) {
                cspsWithTarget++;
                totalTargets += csp.dailyTarget;
                if (csp.targets != null) {
                    for (Map.Entry<String, Integer> e : csp.targets.entrySet()) {
                        if (e.getValue() != null && e.getValue().intValue() > 0) {
                            int cur = paramTotals.containsKey(e.getKey()) ? paramTotals.get(e.getKey()).intValue() : 0;
                            paramTotals.put(e.getKey(), cur + e.getValue().intValue());
                        }
                    }
                }
            }
        }

        // Summary Card
        LinearLayout sumCard = Cards.card(context);
        sumCard.setOrientation(1);
        Ui.pad(sumCard, 14, 12, 14, 12);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(16);

        LinearLayout col1 = new LinearLayout(context);
        col1.setOrientation(1);
        col1.addView(Cards.label(context, "TOTAL TARGETS", R.style.Text_Meta, R.color.ink_3), mw());
        TextView tgtVal = Cards.bold(context, Integer.toString(totalTargets), 24.0f, R.color.primary_700);
        col1.addView(tgtVal, top(context, 2));
        col1.addView(Cards.label(context, cspsWithTarget + " of " + dayData.size() + " CSPs set", R.style.Text_Nano, R.color.ok), top(context, 2));
        topRow.addView(col1, weight1());

        sumCard.addView(topRow, mw());

        if (totalTargets > 0) {
            sumCard.addView(Cards.rule(context, R.color.line, 10, 8), mw());
            sumCard.addView(Cards.label(context, "Targets by Parameter", R.style.Text_Section, R.color.ink_3), mw());
            for (Params.P p : params) {
                int pVal = paramTotals.containsKey(p.key) ? paramTotals.get(p.key).intValue() : 0;
                if (pVal > 0) {
                    LinearLayout pRow = new LinearLayout(context);
                    pRow.setOrientation(0);
                    pRow.setGravity(16);
                    Ui.pad(pRow, 0, 4, 0, 4);
                    pRow.addView(Cards.label(context, p.label, R.style.Text_Body, R.color.ink_2), weight1());
                    pRow.addView(Cards.bold(context, Integer.toString(pVal), 14.0f, R.color.ink), wrap());
                    sumCard.addView(pRow, mw());
                }
            }
        }

        this.content.addView(sumCard, top(context, 10));

        // CSP List card
        LinearLayout cspCard = Cards.cardFlat(context);
        int sl = 0;
        for (CspDay csp : dayData) {
            sl++;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(0);
            row.setGravity(16);
            Ui.pad(row, 14, 9, 14, 9);

            LinearLayout info = new LinearLayout(context);
            info.setOrientation(1);
            info.addView(Cards.bold(context, sl + ". " + csp.name, 14.0f, R.color.ink), mw());
            info.addView(Cards.label(context, csp.branch + " · " + Biz.cspCodeSuffix(csp.code), R.style.Text_Meta, R.color.ink_3), top(context, 1));

            if (csp.dailyTarget > 0) {
                String tgtSummary = Texts.formatSelfTargetLine(csp.code, csp.targets, params);
                info.addView(Cards.label(context, tgtSummary, R.style.Text_Nano, R.color.ink_2), top(context, 3));
            }
            row.addView(info, weight1());

            Badge badge = new Badge(context);
            if (csp.dailyTarget > 0) {
                badge.setKind(Badge.BRAND);
                badge.setText("Target: " + csp.dailyTarget);
            } else {
                badge.setKind(Badge.NEUTRAL);
                badge.setText("No target");
            }
            row.addView(badge, wrap());

            cspCard.addView(row, mw());
            if (sl < dayData.size()) {
                cspCard.addView(Cards.rule(context, R.color.line, 0, 0), mw());
            }
        }
        cspCard.addView(foot(context, dayData.size() + " active CSPs · " + cspsWithTarget + " with targets"), mw());
        this.content.addView(cspCard, top(context, 10));

        return Texts.selfTargetList(dayData, params, this.date);
    }

    private List<String> chosenCols(SQLiteDatabase sQLiteDatabase) {
        ArrayList<String> arrayList = new ArrayList(Params.labels(sQLiteDatabase).keySet());
        String reportCols = UiState.reportCols(getContext());
        if (reportCols == null || reportCols.isEmpty()) {
            return arrayList;
        }
        ArrayList arrayList2 = new ArrayList();
        for (String str : arrayList) {
            if (("," + reportCols + ",").indexOf("," + str + ",") >= 0) {
                arrayList2.add(str);
            }
        }
        return arrayList2.isEmpty() ? arrayList : arrayList2;
    }

    private String paramText(Map<String, String> map, List<String> list, Map<String, String> map2) {
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(map2.get(str)).append(' ').append(parseInt(map.get(str)));
        }
        return sb.toString();
    }

    private String colSummary(SQLiteDatabase sQLiteDatabase, List<String> list) {
        if (list.size() == Params.keys(sQLiteDatabase).size()) {
            return "all " + list.size();
        }
        Map<String, String> shorts = Params.shorts(sQLiteDatabase);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(shorts.get(list.get(i)));
        }
        return sb.toString();
    }

    private View paramBar(Context context, final SQLiteDatabase sQLiteDatabase, List<String> list) {
        LinearLayout card = Cards.card(context);
        card.setOrientation(0);
        card.setGravity(16);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.addView(Cards.label(context, "Parameters shown", R.style.Text_Section, R.color.ink_3), mw());
        TextView bold = Cards.bold(context, colSummary(sQLiteDatabase, list), 14.0f, R.color.ink);
        bold.setMaxLines(3);
        bold.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(bold, top(context, 1));
        card.addView(linearLayout, weight1());
        card.addView(Cards.link(context, "Change", R.color.primary_600, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportsScreen.this.pickParams(sQLiteDatabase);
            }
        }), wrap());
        return card;
    }

    private void pickParams(SQLiteDatabase sQLiteDatabase) {
        final Context context = getContext();
        Map<String, String> labels = Params.labels(sQLiteDatabase);
        final ArrayList arrayList = new ArrayList(labels.keySet());
        String[] strArr = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            strArr[i] = labels.get(arrayList.get(i));
        }
        List<String> chosenCols = chosenCols(sQLiteDatabase);
        boolean[] zArr = new boolean[arrayList.size()];
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            zArr[i2] = chosenCols.contains(arrayList.get(i2));
        }
        new AlertDialog.Builder(context).setTitle("Show in CSP-wise report").setMultiChoiceItems(strArr, zArr, null).setNegativeButton("Cancel", null).setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                ListView listView = ((AlertDialog) dialogInterface).getListView();
                StringBuilder sb = new StringBuilder();
                for (int i4 = 0; i4 < arrayList.size(); i4++) {
                    if (listView.isItemChecked(i4)) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append((String) arrayList.get(i4));
                    }
                }
                if (sb.length() == 0) {
                    ReportsScreen.this.host.toast("Nothing chosen — showing all parameters");
                }
                UiState.setReportCols(context, sb.toString());
                ReportsScreen.this.refreshContent();
            }
        }).show();
    }

    private String branchWise(SQLiteDatabase sQLiteDatabase) {
        Context context = getContext();
        List<Map<String, String>> branchMonth = Agg.branchMonth(sQLiteDatabase, this.month);
        LinearLayout cardFlat = Cards.cardFlat(context);
        this.content.addView(cardFlat, top(context, 10));
        StringBuilder sb = new StringBuilder();
        sb.append("BRANCH-WISE REPORT - ").append(Biz.fmtMonth(this.month)).append("\n");
        if (branchMonth.isEmpty()) {
            cardFlat.addView(Cards.label(context, "No branches with active CSPs.", R.style.Text_Body, R.color.ink_4), mw());
            return sb.append("\nNo branches.").toString().trim();
        }
        int i = 0;
        int i2 = 0;
        for (Map<String, String> map : branchMonth) {
            int parseInt = parseInt(map.get("csps"));
            int parseInt2 = parseInt(map.get("reports"));
            int parseInt3 = parseInt(map.get("total_business"));
            i2++;
            i += parseInt2;
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(0);
            linearLayout.setGravity(16);
            Ui.pad(linearLayout, 14, 9, 14, 9);
            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setOrientation(1);
            linearLayout2.addView(Cards.bold(context, i2 + ". " + map.get("branch_name"), 14.0f, R.color.ink), mw());
            linearLayout2.addView(Cards.label(context, map.get("branch_codes") + " · " + parseInt + (parseInt == 1 ? " CSP" : " CSPs") + " · " + parseInt2 + (parseInt2 == 1 ? " report" : " reports"), R.style.Text_Meta, R.color.ink_3), top(context, 1));
            linearLayout.addView(linearLayout2, weight1());
            linearLayout.addView(Cards.bold(context, Integer.toString(parseInt3), 15.0f, R.color.primary_700), fixed(context, 62));
            cardFlat.addView(linearLayout, mw());
            if (i2 < branchMonth.size()) {
                cardFlat.addView(Cards.rule(context, R.color.line, 0, 0), mw());
            }
            sb.append(i2 + ". ").append(map.get("branch_name")).append(" (").append(map.get("branch_codes")).append(")  ").append(parseInt).append(" csp  ").append(parseInt2).append(" rpt").append("\n");
        }
        cardFlat.addView(foot(context, branchMonth.size() + (branchMonth.size() == 1 ? " branch" : " branches") + " · " + i + " reports"), mw());
        return sb.toString().trim();
    }

    private View foot(Context context, String str) {
        LinearLayout linearLayout = new LinearLayout(context);
        Ui.pad(linearLayout, 14, 11, 14, 11);
        linearLayout.addView(Cards.label(context, str, R.style.Text_Meta, R.color.ink_3), weight1());
        return linearLayout;
    }

    private String dayReport(Context context, SQLiteDatabase sQLiteDatabase) {
        DaySummary summary = Day.summary(sQLiteDatabase, this.date);

        // 1. Overview Progress Banner Card
        LinearLayout overviewCard = Cards.card(context);
        LinearLayout overviewRow = new LinearLayout(context);
        overviewRow.setOrientation(0);
        overviewRow.setGravity(16);
        overviewCard.addView(overviewRow, mw());

        Ring ring = new Ring(context);
        ring.set((float) summary.pct, Integer.toString(summary.pctInt()) + "%", summary.reported + "/" + summary.total);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(Ui.dp(context, 70.0f), Ui.dp(context, 70.0f));
        ringLp.rightMargin = Ui.dp(context, 14.0f);
        overviewRow.addView(ring, ringLp);

        LinearLayout overviewInfo = new LinearLayout(context);
        overviewInfo.setOrientation(1);
        overviewRow.addView(overviewInfo, weight1());

        TextView headline = new TextView(context);
        headline.setText(summary.pctInt() + "% Filed");
        headline.setTextAppearance(context, R.style.Text_MetricSm);
        headline.setTextColor(Ui.color(context, R.color.ink));
        overviewInfo.addView(headline, mw());

        TextView subHeadline = new TextView(context);
        subHeadline.setText(summary.reported + " of " + summary.total + " reports submitted today");
        subHeadline.setTextAppearance(context, R.style.Text_Meta);
        subHeadline.setTextColor(Ui.color(context, R.color.ink_3));
        LinearLayout.LayoutParams subLp = mw();
        subLp.topMargin = Ui.dp(context, 2.0f);
        overviewInfo.addView(subHeadline, subLp);

        Badge statusBadge = new Badge(context);
        statusBadge.setKind(summary.isComplete() ? 1 : 3);
        statusBadge.setText(summary.isComplete() ? "✓ All Reports Closed" : (summary.pending + " Pending Submissions"));
        LinearLayout.LayoutParams badgeLp = wrap();
        badgeLp.topMargin = Ui.dp(context, 6.0f);
        overviewInfo.addView(statusBadge, badgeLp);

        this.content.addView(overviewCard, top(context, 10));

        // 2. Redesigned 5-KPI Analytics Grid: Total, Reported, Pending, Nil, Business Figure
        // Row 1: Total & Reported
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(0);
        LinearLayout.LayoutParams row1Lp = mw();
        row1Lp.topMargin = Ui.dp(context, 10.0f);

        // Card 1: TOTAL CSPS
        row1.addView(metricTile(
            context,
            R.drawable.ic_csp,
            R.color.primary_700,
            R.color.primary_50,
            "TOTAL CSPS",
            Integer.toString(summary.total),
            R.color.ink,
            "Active register",
            R.color.ink_3,
            R.color.paper,
            R.color.line,
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReportsScreen.this.host.goTab(1);
                }
            }
        ), weight1());

        // Card 2: REPORTED
        LinearLayout.LayoutParams card2Lp = weight1();
        card2Lp.leftMargin = Ui.dp(context, 8.0f);
        row1.addView(metricTile(
            context,
            R.drawable.ic_check_circle,
            R.color.ok,
            R.color.ok_bg,
            "REPORTED",
            Integer.toString(summary.reported),
            R.color.ok,
            summary.pctInt() + "% completed",
            R.color.ok,
            R.color.paper,
            R.color.ok_border,
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReportsScreen.this.host.push(new CspListScreen(ReportsScreen.this.getContext(), ReportsScreen.this.host, 1));
                }
            }
        ), card2Lp);

        this.content.addView(row1, row1Lp);

        // Row 2: Pending & Nil
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(0);
        LinearLayout.LayoutParams row2Lp = mw();
        row2Lp.topMargin = Ui.dp(context, 8.0f);

        // Card 3: PENDING
        row2.addView(metricTile(
            context,
            R.drawable.ic_clock,
            R.color.danger,
            R.color.danger_bg,
            "PENDING",
            Integer.toString(summary.pending),
            R.color.danger,
            summary.pending == 0 ? "All up to date" : summary.pending + " to follow up",
            R.color.danger,
            R.color.paper,
            summary.pending > 0 ? R.color.danger_border : R.color.line,
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReportsScreen.this.host.push(new FollowupScreen(ReportsScreen.this.getContext(), ReportsScreen.this.host, ReportsScreen.this.date));
                }
            }
        ), weight1());

        // Card 4: NIL FIGURE
        LinearLayout.LayoutParams card4Lp = weight1();
        card4Lp.leftMargin = Ui.dp(context, 8.0f);
        row2.addView(metricTile(
            context,
            R.drawable.ic_nil,
            R.color.info,
            R.color.info_bg,
            "NIL FIGURE",
            Integer.toString(summary.nilFigure),
            R.color.info,
            "Zero figures",
            R.color.info,
            R.color.paper,
            R.color.info_border,
            null
        ), card4Lp);

        this.content.addView(row2, row2Lp);

        // Row 3: BUSINESS FIGURE Single Span Card
        LinearLayout row3 = new LinearLayout(context);
        row3.setOrientation(0);
        LinearLayout.LayoutParams row3Lp = mw();
        row3Lp.topMargin = Ui.dp(context, 8.0f);
        row3.addView(metricTile(
            context,
            R.drawable.ic_trending,
            R.color.primary_600,
            R.color.primary_50,
            "BUSINESS FIGURE",
            Integer.toString(summary.totalBusiness),
            R.color.primary_700,
            summary.figureReported + " CSPs with business volume",
            R.color.primary_700,
            R.color.paper,
            R.color.line,
            null
        ), weight1());
        this.content.addView(row3, row3Lp);

        // 3. Figures by Parameter Card with Visual Progress Bars
        LinearLayout card2 = Cards.card(context);
        this.content.addView(card2, top(context, 10));
        card2.addView(Cards.label(context, "Figures by parameter", R.style.Text_Section, R.color.ink_3), mw());
        Map<String, Integer> map = Day.totals(sQLiteDatabase, this.date);
        Map<String, String> labels = Params.labels(sQLiteDatabase);
        int maxVal = 1;
        for (Integer v : map.values()) {
            if (v != null && v > maxVal) maxVal = v;
        }

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            int val = entry.getValue() == null ? 0 : entry.getValue().intValue();
            LinearLayout pContainer = new LinearLayout(context);
            pContainer.setOrientation(1);
            Ui.pad(pContainer, 0, 6, 0, 6);

            LinearLayout linearLayout3 = new LinearLayout(context);
            linearLayout3.setOrientation(0);
            linearLayout3.setGravity(16);
            linearLayout3.addView(Cards.label(context, labels.get(entry.getKey()), R.style.Text_Body, R.color.ink_2), weight1());
            linearLayout3.addView(Cards.bold(context, Integer.toString(val), 14.5f, R.color.ink), wrap());
            pContainer.addView(linearLayout3, mw());

            // Visual Progress bar
            FrameLayout barContainer = new FrameLayout(context);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, Ui.dp(context, 5.0f));
            barLp.topMargin = Ui.dp(context, 4.0f);
            barContainer.setLayoutParams(barLp);

            View barTrack = new View(context);
            barTrack.setBackground(Ui.pill(context, Ui.color(context, R.color.fill)));
            barContainer.addView(barTrack, new FrameLayout.LayoutParams(-1, -1));

            View barFill = new View(context);
            barFill.setBackground(Ui.pill(context, Ui.color(context, val > 0 ? R.color.primary_600 : R.color.ink_4)));
            int fillWidth = Math.max(0, Math.round((val / (float) maxVal) * 100));
            FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(-1, -1);
            barContainer.addView(barFill, fillLp);
            barFill.setScaleX(val > 0 ? (val / (float) maxVal) : 0f);
            barFill.setPivotX(0f);

            pContainer.addView(barContainer);
            card2.addView(pContainer, mw());
        }

        int i = 0;
        for (CspDay cspDay : Day.dayData(sQLiteDatabase, this.date)) {
            if (cspDay.isPending() && cspDay.pendingDays >= 3) {
                i++;
            }
        }
        if (i > 0) {
            LinearLayout card3 = Cards.card(context);
            card3.setOrientation(0);
            card3.setGravity(16);
            card3.setBackground(Ui.round(context, Ui.color(context, R.color.warn_bg), 16.0f, Ui.color(context, R.color.warn_border), 1.0f));
            card3.addView(Cards.label(context, i + " CSP" + (i == 1 ? "" : "s") + " pending for 3 or more working days", R.style.Text_Body, R.color.warn), weight1());
            this.content.addView(card3, top(context, 10));
        }
        return Texts.officeText(sQLiteDatabase, this.date);
    }

    private View metricTile(Context context, int iconRes, int iconTintRes, int iconBgRes, String caption, String value, int valColorRes, String sub, int subColorRes, int bgRes, int borderRes, View.OnClickListener onClickListener) {
        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(1);
        tile.setBackground(Ui.round(context, Ui.color(context, bgRes), 16.0f, Ui.color(context, borderRes), 1.0f));
        if (Build.VERSION.SDK_INT >= 21) {
            tile.setElevation(Ui.px(context, R.dimen.el_1));
        }
        Ui.pad(tile, 14, 12, 14, 12);
        if (onClickListener != null) {
            tile.setClickable(true);
            tile.setOnClickListener(onClickListener);
        }

        // Top row: Icon Badge + Caption
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(16);

        // Icon Container
        FrameLayout iconBox = new FrameLayout(context);
        iconBox.setBackground(Ui.round(context, Ui.color(context, iconBgRes), 999.0f, 0, 0.0f));
        int boxSize = Ui.dp(context, 30.0f);
        LinearLayout.LayoutParams iconBoxLp = new LinearLayout.LayoutParams(boxSize, boxSize);
        iconBoxLp.rightMargin = Ui.dp(context, 8.0f);
        iconBox.setLayoutParams(iconBoxLp);

        ImageView iconView = new ImageView(context);
        int iconSize = Ui.dp(context, 16.0f);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconLp.gravity = 17;
        iconView.setLayoutParams(iconLp);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(Ui.color(context, iconTintRes));
        iconBox.addView(iconView);
        topRow.addView(iconBox);

        TextView capView = new TextView(context);
        capView.setText(caption);
        capView.setTextAppearance(context, R.style.Text_KpiCaption);
        capView.setGravity(19);
        capView.setSingleLine(true);
        capView.setTextColor(Ui.color(context, subColorRes));
        topRow.addView(capView, weight1());

        tile.addView(topRow, mw());

        // Value
        TextView valView = new TextView(context);
        valView.setText(value);
        valView.setTextAppearance(context, R.style.Text_Metric);
        Ui.size(valView, 24.0f);
        valView.setTextColor(Ui.color(context, valColorRes));
        LinearLayout.LayoutParams valLp = mw();
        valLp.topMargin = Ui.dp(context, 8.0f);
        tile.addView(valView, valLp);

        // Subtitle / Note
        TextView subView = new TextView(context);
        subView.setText(sub);
        Ui.size(subView, 11.0f);
        subView.setTypeface(Typeface.DEFAULT_BOLD);
        subView.setTextColor(Ui.color(context, subColorRes));
        subView.setSingleLine(true);
        subView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subLp = mw();
        subLp.topMargin = Ui.dp(context, 2.0f);
        tile.addView(subView, subLp);

        return tile;
    }

    private String monthly(SQLiteDatabase sQLiteDatabase) {
        Context context = getContext();
        List<Map<String, String>> month = Agg.month(sQLiteDatabase, this.month);
        LinearLayout card = Cards.card(context);
        card.setPadding(0, 0, 0, 0);
        this.content.addView(card, top(context, 10));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        Ui.pad(linearLayout, 14, 12, 14, 8);
        linearLayout.addView(Cards.label(context, "Date", R.style.Text_Section, R.color.ink_3), weight1());
        linearLayout.addView(Cards.label(context, "Rpt", R.style.Text_Section, R.color.ink_3), fixed(context, 46));
        linearLayout.addView(Cards.label(context, "Total", R.style.Text_Section, R.color.ink_3), fixed(context, 56));
        card.addView(linearLayout, mw());
        if (month.isEmpty()) {
            card.addView(Cards.label(context, "No reports in " + Biz.fmtMonth(this.month) + ".", R.style.Text_Body, R.color.ink_4), mw());
        }
        for (Map<String, String> map : month) {
            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setOrientation(0);
            Ui.pad(linearLayout2, 14, 8, 14, 8);
            linearLayout2.addView(Cards.label(context, Biz.fmtDmy(map.get("business_date")), R.style.Text_Body, R.color.ink_2), weight1());
            linearLayout2.addView(Cards.label(context, map.get("reported"), R.style.Text_Body, R.color.ink), fixed(context, 46));
            linearLayout2.addView(Cards.bold(context, map.get("total_business"), 14.0f, R.color.ink), fixed(context, 56));
            card.addView(linearLayout2, mw());
        }
        return monthlySummary(sQLiteDatabase, this.month);
    }

    private String monthlySummary(SQLiteDatabase sQLiteDatabase, String str) {
        int i;
        List<Map<String, String>> month = Agg.month(sQLiteDatabase, str);
        if (month.isEmpty()) {
            return "MONTHLY REPORT - " + Biz.fmtMonth(str) + "\n\nNo reports found for this period.";
        }
        int i2 = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("MONTHLY REPORT - ").append(Biz.fmtMonth(str)).append("\n");
        Iterator<Map<String, String>> it = month.iterator();
        while (true) {
            i = i2;
            if (!it.hasNext()) {
                break;
            }
            i2 = parseInt(it.next().get("reported")) + i;
        }
        sb.append("Days with reports: ").append(month.size()).append("   Reports: ").append(i).append("\n\n");
        sb.append("Per day:\n");
        for (Map<String, String> map : month) {
            sb.append(Biz.fmtDmy(map.get("business_date"))).append("  ").append(map.get("reported")).append(" rpt  ").append(map.get("total_business")).append("\n");
        }
        return sb.toString().trim();
    }

    private static int parseInt(String str) {
        if (str == null) {
            return 0;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void copy(String str) {
        ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService("clipboard");
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("csp", str));
            this.host.toast("Copied to clipboard");
        }
    }

    private void exportCsv() {
        try {
            SQLiteDatabase dbRead = Core.get(getContext()).dbRead();
            if (TABS[0].equals(this.tab) || TABS[1].equals(this.tab)) {
                Files.saveText((Activity) getContext(), "csp-day-" + this.date + ".csv", "text/csv", Texts.dayCsv(dbRead, this.date));
            } else if (TABS[2].equals(this.tab)) {
                Files.saveText((Activity) getContext(), "self-targets-" + this.date + ".csv", "text/csv", Texts.selfTargetCsv(dbRead, this.date));
            } else if (TABS[4].equals(this.tab)) {
                if (this.cspScope == 0) {
                    Files.saveText((Activity) getContext(), "csp-daily-" + this.date + ".csv", "text/csv", Texts.dayCsv(dbRead, this.date));
                } else {
                    Files.saveText((Activity) getContext(), "csp-monthly-" + this.month + ".csv", "text/csv", Texts.monthCsv(dbRead, this.month, "csp"));
                }
            } else if (TABS[5].equals(this.tab)) {
                Files.saveText((Activity) getContext(), "csp-branch-" + this.month + ".csv", "text/csv", Texts.monthCsv(dbRead, this.month, "branch"));
            } else {
                Files.saveText((Activity) getContext(), "csp-month-" + this.month + ".csv", "text/csv", Texts.monthCsv(dbRead, this.month, "day"));
            }
        } catch (Exception e) {
            this.host.toast("Export failed: " + e.getMessage());
        }
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

    private static LinearLayout.LayoutParams fixed(Context context, int i) {
        return new LinearLayout.LayoutParams(Ui.dp(context, i), -2);
    }

    private static LinearLayout.LayoutParams top(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }
}
