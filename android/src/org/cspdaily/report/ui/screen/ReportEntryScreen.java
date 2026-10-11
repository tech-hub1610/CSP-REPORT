package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.DaySummary;
import org.cspdaily.report.notify.Reminders;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Section;
import org.cspdaily.report.util.PhoneActions;

/* loaded from: classes.dex */
public class ReportEntryScreen extends Screen {
    private static final String GROUP_BANK = "Banking activity";
    private static final String GROUP_KYC = "KYC and digital";
    private static final String GROUP_OTHER = "Other parameters";
    private static final String GROUP_SOCIAL = "Social security";
    private final Map<String, Field> byKey;
    private final long cspId;
    private final String date;
    private TextView errorLine;
    private final List<Field> fields;
    private EditText remarks;
    private CspDay row;
    private TextView totalSub;
    private TextView totalValue;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Field {
        final TextView hint;
        final EditText input;
        final String key;
        final String label;
        final String shortLabel;
        final int target;

        Field(String str, String str2, String str3, int i, EditText editText, TextView textView) {
            this.key = str;
            this.label = str2;
            this.shortLabel = str3;
            this.target = i;
            this.input = editText;
            this.hint = textView;
        }

        int value() {
            int cleanCount = Biz.cleanCount(this.input.getText().toString());
            if (cleanCount < 0) {
                return 0;
            }
            return cleanCount;
        }

        boolean invalid() {
            return Biz.cleanCount(this.input.getText().toString()) < 0;
        }
    }

    public ReportEntryScreen(Context context, Screen.Host host, long j, String str) {
        super(context, host);
        this.fields = new ArrayList();
        this.byKey = new LinkedHashMap();
        this.cspId = j;
        this.date = str;
        title("Report entry");
        bodyBottomPad(96);
        build();
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return "Report entry";
    }

    private void build() {
        Context context = getContext();
        SQLiteDatabase dbRead = Core.get(context).dbRead();
        Params.sumSql(dbRead);
        CspDay cspDay = null;
        for (CspDay cd : Day.dayData(dbRead, this.date, false)) {
            if (cd.id == this.cspId) {
                cspDay = cd;
                break;
            }
        }
        this.row = cspDay;
        LinearLayout card = Cards.card(context);
        card.setOrientation(1);
        add(card, 0);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        card.addView(linearLayout, mw());
        TextView bold = Cards.bold(context, this.row == null ? "CSP" : this.row.name, 17.0f, R.color.ink);
        bold.setSingleLine(true);
        bold.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(bold, weight1());
        TextView textView = new TextView(context);
        textView.setText(Biz.friendlyDate(this.date));
        Ui.size(textView, 12.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, R.color.primary_700));
        textView.setBackground(Ui.pill(context, Ui.color(context, R.color.primary_50)));
        Ui.pad(textView, 10, 4, 10, 4);
        linearLayout.addView(textView, wrap());
        StringBuilder sb = new StringBuilder();
        if (this.row != null) {
            sb.append("Code ").append(this.row.code).append("  ·  #").append(this.row.suffix());
            if (!this.row.branch.isEmpty()) {
                sb.append("  ·  ").append(this.row.branch);
                if (!this.row.branchCode.isEmpty()) {
                    sb.append(" (").append(this.row.branchCode).append(")");
                }
            }
            if (!this.row.gender.isEmpty()) {
                sb.append("  ·  ").append("F".equalsIgnoreCase(this.row.gender) ? "Female" : "Male");
            }
        }
        card.addView(Cards.label(context, sb.toString(), R.style.Text_Meta, R.color.ink_3), labelTop(context, 2));
        if (this.row != null && this.row.mobile.length() > 0) {
            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setOrientation(0);
            linearLayout2.setGravity(16);
            LinearLayout.LayoutParams mw = mw();
            mw.topMargin = Ui.dp(context, 10.0f);
            linearLayout2.setLayoutParams(mw);
            linearLayout2.addView(miniAction(context, "Call", R.drawable.ic_phone, R.color.primary_600, new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.1
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    PhoneActions.call(ReportEntryScreen.this.getContext(), ReportEntryScreen.this.row);
                }
            }), weight1());
            View view = new View(context);
            view.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));
            linearLayout2.addView(view);
            linearLayout2.addView(miniAction(context, "WhatsApp", R.drawable.ic_whatsapp, R.color.whatsapp_dark, new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.2
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    PhoneActions.whatsapp(ReportEntryScreen.this.getContext(), ReportEntryScreen.this.row);
                }
            }), weight1());
            View view2 = new View(context);
            view2.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));
            linearLayout2.addView(view2);
            linearLayout2.addView(miniAction(context, "Follow-up", R.drawable.ic_note, R.color.warn, new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.3
                @Override // android.view.View.OnClickListener
                public void onClick(View view3) {
                    FollowupSheet.open((Activity) ReportEntryScreen.this.getContext(), ReportEntryScreen.this.host, ReportEntryScreen.this.row, ReportEntryScreen.this.date);
                }
            }), weight1());
            card.addView(linearLayout2);
        }
        if (this.row != null && this.row.reported) {
            LinearLayout linearLayout3 = new LinearLayout(context);
            linearLayout3.setOrientation(0);
            linearLayout3.setGravity(16);
            linearLayout3.setBackground(Ui.round(context, Ui.color(context, R.color.ok_bg), 10.0f, Ui.color(context, R.color.ok_border), 1.0f));
            Ui.pad(linearLayout3, 10, 8, 10, 8);
            LinearLayout.LayoutParams mw2 = mw();
            mw2.topMargin = Ui.dp(context, 10.0f);
            linearLayout3.setLayoutParams(mw2);
            String shortWhen = this.row.submittedAt == null ? "" : Biz.shortWhen(this.row.submittedAt);
            linearLayout3.addView(Cards.label(context, "Editing a saved report" + (shortWhen.isEmpty() ? "" : " · filed " + shortWhen), R.style.Text_Meta, R.color.ok), weight1());
            final CspDay cspDay2 = this.row;
            linearLayout3.addView(Cards.link(context, "Un-file", R.color.danger, new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.4
                @Override // android.view.View.OnClickListener
                public void onClick(View view3) {
                    ReportEntryScreen.this.confirmUnfile(cspDay2);
                }
            }), wrap());
            card.addView(linearLayout3);
        }
        List<String> keys = Params.keys(dbRead);
        Map<String, String> labels = Params.labels(dbRead);
        Map<String, String> shorts = Params.shorts(dbRead);
        LinkedHashMap<String, List<String>> linkedHashMap = new LinkedHashMap<>();
        for (String str : keys) {
            String groupOf = groupOf(str);
            List<String> list = linkedHashMap.get(groupOf);
            if (list == null) {
                list = new ArrayList<>();
                linkedHashMap.put(groupOf, list);
            }
            list.add(str);
        }
        for (Map.Entry<String, List<String>> entry : linkedHashMap.entrySet()) {
            Section section = new Section(context);
            section.set(entry.getKey());
            add(section, 4);
            LinearLayout card2 = Cards.card(context);
            add(card2, 0);
            List<String> list2 = entry.getValue();
            for (int i2 = 0; i2 < list2.size(); i2++) {
                if (i2 % 2 == 0) {
                    LinearLayout linearLayout4 = new LinearLayout(context);
                    linearLayout4.setOrientation(0);
                    card2.addView(linearLayout4, mw());
                }
                LinearLayout linearLayout5 = (LinearLayout) card2.getChildAt(card2.getChildCount() - 1);
                if (i2 > 0 && i2 % 2 == 1) {
                    View view3 = new View(context);
                    view3.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 10.0f), 1));
                    linearLayout5.addView(view3);
                }
                String fieldKey = list2.get(i2);
                linearLayout5.addView(field(context, dbRead, fieldKey, labels.get(fieldKey), shorts.get(fieldKey)), weight1());
                if (i2 == list2.size() - 1 && i2 % 2 == 0) {
                    View view4 = new View(context);
                    view4.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 10.0f), 1));
                    linearLayout5.addView(view4);
                    View view5 = new View(context);
                    linearLayout5.addView(view5, weight1());
                }
            }
        }
        Section section2 = new Section(context);
        section2.set("Remarks");
        add(section2, 8);
        this.remarks = new EditText(context);
        this.remarks.setHint("Optional · max 500 characters");
        Ui.size(this.remarks, 15.0f);
        this.remarks.setTextColor(Ui.color(context, R.color.ink));
        this.remarks.setMinLines(3);
        this.remarks.setGravity(8388659);
        this.remarks.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(this.remarks, 12, 12, 12, 12);
        add(this.remarks, 0);
        if (this.row != null) {
            this.remarks.setText(this.row.remarks);
        }

        final List<String> allSuggestions = new ArrayList<>();
        if (this.row != null) {
            String effectiveReason = this.row.effectiveReasonOrPromise();
            if (!effectiveReason.isEmpty()) {
                allSuggestions.add(effectiveReason);
            }
        }
        for (String r : Day.PENDING_REASONS) {
            if (!allSuggestions.contains(r)) {
                allSuggestions.add(r);
            }
        }
        String[] extraRemarks = {"Portal / Server Down", "Biometric device issue", "Customer rush / power outage"};
        for (String ex : extraRemarks) {
            if (!allSuggestions.contains(ex)) {
                allSuggestions.add(ex);
            }
        }

        final org.cspdaily.report.ui.widget.Chips chipsRemarks = new org.cspdaily.report.ui.widget.Chips(context);
        chipsRemarks.allowNone(true);
        chipsRemarks.set(allSuggestions);
        chipsRemarks.onChange(new org.cspdaily.report.ui.widget.Chips.OnPick() {
            @Override
            public void pick(int i, String str2) {
                if (str2 != null && !str2.isEmpty()) {
                    ReportEntryScreen.this.remarks.setText(str2);
                    ReportEntryScreen.this.remarks.setSelection(str2.length());
                }
            }
        });
        add(chipsRemarks, 6);

        final boolean[] updatingRemarks = {false};
        this.remarks.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (updatingRemarks[0]) return;
                String query = s == null ? "" : s.toString().trim().toLowerCase(java.util.Locale.US);
                List<String> matched = new ArrayList<>();
                if (!query.isEmpty()) {
                    for (String cand : allSuggestions) {
                        if (cand.toLowerCase(java.util.Locale.US).contains(query)) {
                            matched.add(cand);
                        }
                    }
                }
                if (matched.isEmpty()) {
                    matched.addAll(allSuggestions);
                }
                updatingRemarks[0] = true;
                chipsRemarks.set(matched);
                updatingRemarks[0] = false;
            }
        });

        this.errorLine = Cards.label(context, "", R.style.Text_Meta, R.color.danger);
        this.errorLine.setVisibility(8);
        add(this.errorLine, 6);
        LinearLayout card3 = Cards.card(context);
        card3.setOrientation(0);
        card3.setGravity(16);
        card3.setBackgroundResource(R.drawable.bg_hero_card);
        LinearLayout.LayoutParams mw3 = mw();
        mw3.topMargin = Ui.dp(context, 12.0f);
        card3.setLayoutParams(mw3);
        LinearLayout linearLayout6 = new LinearLayout(context);
        linearLayout6.setOrientation(1);
        card3.addView(linearLayout6, weight1());
        linearLayout6.addView(Cards.label(context, "Total business", R.style.Text_HeroLabel, R.color.hero_text_dim), mw());
        this.totalValue = Cards.bold(context, "0", 26.0f, R.color.paper);
        linearLayout6.addView(this.totalValue, mw());
        this.totalSub = Cards.label(context, "", R.style.Text_Meta, R.color.hero_text_dim);
        linearLayout6.addView(this.totalSub, mw());
        add(card3, 0);
        this.host.refresh();
    }

    private String groupOf(String str) {
        if (str.contains("rekyc") && !str.contains("branch")) {
            return GROUP_KYC;
        }
        if (str.equals("qr_code_rekyc")) {
            return GROUP_KYC;
        }
        if (str.equals("pmjjby") || str.equals("pmsby") || str.equals("pmjdy") || str.equals("apy")) {
            return GROUP_SOCIAL;
        }
        if (str.contains("branch_submit") || str.contains("whatsapp")) {
            return GROUP_BANK;
        }
        return GROUP_OTHER;
    }

    private LinearLayout field(Context context, SQLiteDatabase sQLiteDatabase, String str, String str2, String str3) {
        int intValue;
        int intValue2 = (this.row == null || this.row.targets.get(str) == null) ? 0 : this.row.targets.get(str).intValue();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        TextView textView = new TextView(context);
        textView.setText((str3 == null || str3.isEmpty()) ? str2 : str3);
        Ui.size(textView, 12.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, R.color.ink_2));
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(textView, mw());
        EditText editText = new EditText(context);
        editText.setInputType(2);
        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        editText.setHint("0");
        editText.setGravity(17);
        Ui.size(editText, 20.0f);
        editText.setTypeface(Typeface.DEFAULT_BOLD);
        editText.setTextColor(Ui.color(context, R.color.ink));
        editText.setMinHeight(Ui.dp(context, 52.0f));
        editText.setSingleLine(true);
        editText.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), 14.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(editText, 10, 10, 10, 10);
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 5.0f);
        linearLayout.addView(editText, mw);
        TextView label = Cards.label(context, "", R.style.Text_Nano, R.color.ink_4);
        label.setGravity(17);
        Ui.pad(label, 0, 4, 0, 0);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(label, mw());
        if (this.row == null) {
            intValue = 0;
        } else {
            intValue = this.row.values.get(str) == null ? 0 : this.row.values.get(str).intValue();
        }
        editText.setText(intValue == 0 ? "" : Integer.toString(intValue));
        Field field = new Field(str, str2, str3, intValue2, editText, label);
        this.fields.add(field);
        this.byKey.put(str, field);
        editText.addTextChangedListener(new TextWatcher() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.5
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                ReportEntryScreen.this.updateTotals();
            }
        });
        paintHint(field, this.row);
        return linearLayout;
    }

    private void paintHint(Field field, CspDay cspDay) {
        Integer num;
        StringBuilder sb = new StringBuilder();
        if (field.target > 0) {
            sb.append("target ").append(field.target);
        }
        if (cspDay != null && (num = cspDay.yesterdayValues.get(field.key)) != null) {
            if (sb.length() > 0) {
                sb.append("  ·  ");
            }
            sb.append("yest ").append(num);
        }
        if (sb.length() == 0) {
            field.hint.setVisibility(8);
            field.hint.setText("");
        } else {
            field.hint.setVisibility(0);
            field.hint.setText(sb);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTotals() {
        int i = 0;
        int i2 = 0;
        for (Field field : this.fields) {
            i2 += field.value();
            i = field.target + i;
        }
        this.totalValue.setText(Integer.toString(i2));
        StringBuilder sb = new StringBuilder();
        if (i > 0) {
            sb.append("target ").append(i);
            sb.append("  ·  ").append(Math.min(999, (int) Math.round((i2 * 100.0d) / i))).append("% of target");
        }
        if (this.row != null && this.row.yesterdayTotal != null) {
            if (sb.length() > 0) {
                sb.append("  ·  ");
            }
            sb.append("yesterday ").append(this.row.yesterdayTotal).append(" (").append(Ui.delta(i2 - this.row.yesterdayTotal.intValue())).append(")");
        }
        this.totalSub.setText(sb);
        if (sb.length() == 0) {
            this.totalSub.setVisibility(8);
        } else {
            this.totalSub.setVisibility(0);
        }
    }

    @Override // org.cspdaily.report.ui.Screen
    public void refreshContent() {
        buildStickyBar();
        updateTotals();
    }

    private void buildStickyBar() {
        Context context = getContext();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        if (nextPendingId() > 0) {
            TextView textView = Cards.tonalButton(context, "Save", new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.6
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    ReportEntryScreen.this.save(false);
                }
            });
            LinearLayout.LayoutParams weight1 = weight1();
            weight1.rightMargin = Ui.dp(context, 8.0f);
            linearLayout.addView(textView, weight1);
            linearLayout.addView(Cards.primaryButton(context, "Save & next", new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.7
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    ReportEntryScreen.this.save(true);
                }
            }), weight1());
        } else {
            linearLayout.addView(Cards.primaryButton(context, "Save report", new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.8
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    ReportEntryScreen.this.save(false);
                }
            }), weight1());
        }
        setStickyAction(linearLayout);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void confirmUnfile(final CspDay cspDay) {
        final Context context = getContext();
        new AlertDialog.Builder(context).setTitle("Un-file " + cspDay.name + "'s report?").setMessage("The CSP returns to Pending for " + Biz.friendlyDate(this.date) + ", and its figures for that date are deleted along with the hourly snapshots taken for it. This cannot be undone.").setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).setPositiveButton("Un-file", new DialogInterface.OnClickListener() { // from class: org.cspdaily.report.ui.screen.ReportEntryScreen.9
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                boolean deleteReport = Day.deleteReport(Core.get(context).db(), cspDay.id, ReportEntryScreen.this.date);
                if (deleteReport) {
                    SupabaseClient.deleteAsync(context, "daily_report", "csp_id=eq." + cspDay.id + "&business_date=eq." + ReportEntryScreen.this.date);
                    SupabaseClient.deleteAsync(context, "hourly_report", "csp_id=eq." + cspDay.id + "&business_date=eq." + ReportEntryScreen.this.date);
                }
                ReportEntryScreen.this.host.toast(deleteReport ? "Report cleared" : "Nothing to clear");
                if (deleteReport) {
                    ReportEntryScreen.this.host.pop();
                }
            }
        }).show();
    }

    private long nextPendingId() {
        int i = -1;
        for (CspDay cspDay : Day.dayData(Core.get(getContext()).dbRead(), this.date)) {
            if (cspDay.isPending() && cspDay.id != this.cspId) {
                i = (i < 0 || cspDay.id < ((long) i)) ? (int) cspDay.id : i;
            }
        }
        if (i < 0) {
            return 0L;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void save(boolean z) {
        Context context = getContext();
        for (Field field : this.fields) {
            if (field.invalid()) {
                field.input.setBackgroundResource(R.drawable.bg_input_error);
                this.errorLine.setText(field.label + " must be a whole number of 7 digits or fewer.");
                this.errorLine.setVisibility(0);
                field.input.requestFocus();
                return;
            }
            field.input.setBackgroundResource(R.drawable.bg_input);
        }
        String trim = this.remarks.getText().toString().trim();
        if (trim.length() > 500) {
            this.errorLine.setText("Remarks must be 500 characters or fewer.");
            this.errorLine.setVisibility(0);
            return;
        }
        this.errorLine.setVisibility(8);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Field field2 : this.fields) {
            linkedHashMap.put(field2.key, Integer.valueOf(field2.value()));
        }
        SQLiteDatabase db = Core.get(context).db();
        String saveReport = Day.saveReport(db, this.cspId, this.date, linkedHashMap, trim);
        DaySummary summary = Day.summary(db, this.date);
        if (summary.isComplete()) {
            Reminders.reportComplete(context, summary);
        }
        Ui.hideKeyboard((Activity) context);
        this.host.toast("Report " + saveReport + " · " + summary.reported + "/" + summary.total + " filed");
        if (z && nextPendingId() > 0) {
            this.host.push(new ReportEntryScreen(context, this.host, nextPendingId(), this.date));
        } else {
            this.host.pop();
        }
    }

    @Override // org.cspdaily.report.ui.Screen
    public boolean onBackPressed() {
        Ui.hideKeyboard((Activity) getContext());
        return false;
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

    private static LinearLayout.LayoutParams labelTop(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }

    private View miniAction(Context context, String str, int i, int i2, View.OnClickListener onClickListener) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(17);
        linearLayout.setBackground(Ui.pressable(context, Ui.color(context, R.color.fill), 10.0f, false));
        Ui.pad(linearLayout, 0, 10, 0, 10);
        linearLayout.setClickable(true);
        linearLayout.setOnClickListener(onClickListener);
        ImageView imageView = new ImageView(context);
        int dp = Ui.dp(context, 15.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp, dp);
        layoutParams.rightMargin = Ui.dp(context, 5.0f);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(i);
        imageView.setColorFilter(Ui.color(context, i2));
        linearLayout.addView(imageView);
        TextView textView = new TextView(context);
        textView.setText(str);
        Ui.size(textView, 12.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, i2));
        textView.setSingleLine(true);
        linearLayout.addView(textView, wrap());
        return linearLayout;
    }
}
