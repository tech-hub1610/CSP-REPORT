package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.FrameLayout;
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

public final class ReportEntrySheet {
    private static final String GROUP_BANK = "Banking activity";
    private static final String GROUP_KYC = "KYC and digital";
    private static final String GROUP_OTHER = "Other parameters";
    private static final String GROUP_SOCIAL = "Social security";

    public static class Field {
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

        LinearLayout container(Context ctx) {
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(1);
            TextView title = new TextView(ctx);
            title.setText((shortLabel == null || shortLabel.isEmpty()) ? label : shortLabel);
            Ui.size(title, 11.5f);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(Ui.color(ctx, R.color.ink_2));
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(title, mw());

            LinearLayout.LayoutParams inputLp = mw();
            inputLp.topMargin = Ui.dp(ctx, 4.0f);
            col.addView(input, inputLp);
            col.addView(hint, mw());
            return col;
        }

        int value() {
            int cleanCount = Biz.cleanCount(this.input.getText().toString());
            return cleanCount < 0 ? 0 : cleanCount;
        }

        boolean invalid() {
            return Biz.cleanCount(this.input.getText().toString()) < 0;
        }
    }

    private ReportEntrySheet() {
    }

    public static void open(final Activity activity, final Screen.Host host, final CspDay initialCspDay, final String date) {
        final Dialog dialog = new Dialog(activity, R.style.Theme_Csp_Sheet);
        dialog.requestWindowFeature(1);

        final SQLiteDatabase dbRead = Core.get(activity).dbRead();
        CspDay found = null;
        for (CspDay cd : Day.dayData(dbRead, date, false)) {
            if (cd.id == initialCspDay.id) {
                found = cd;
                break;
            }
        }
        final CspDay row = found != null ? found : initialCspDay;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(1);

        // Material Design Drag Handle
        LinearLayout dragHandleBar = new LinearLayout(activity);
        dragHandleBar.setOrientation(0);
        dragHandleBar.setGravity(17);
        Ui.pad(dragHandleBar, 0, 10, 0, 8);
        View dragPill = new View(activity);
        dragPill.setBackground(Ui.pill(activity, Ui.color(activity, R.color.line_2)));
        dragHandleBar.addView(dragPill, new LinearLayout.LayoutParams(Ui.dp(activity, 38.0f), Ui.dp(activity, 4.5f)));
        root.addView(dragHandleBar, mw());

        // Header
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(1);
        header.setBackgroundResource(R.drawable.bg_hero_card);
        Ui.pad(header, 16, 12, 16, 12);

        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(0);
        titleRow.setGravity(16);

        TextView heroLabel = Cards.label(activity, row.reported ? "Editing report" : "Enter report", R.style.Text_HeroLabel, R.color.hero_text_dim);
        titleRow.addView(heroLabel, weight1());

        TextView datePill = new TextView(activity);
        datePill.setText(Biz.friendlyDate(date));
        Ui.size(datePill, 11.5f);
        datePill.setTypeface(Typeface.DEFAULT_BOLD);
        datePill.setTextColor(Ui.color(activity, R.color.paper));
        datePill.setBackground(Ui.pill(activity, 0x33FFFFFF));
        Ui.pad(datePill, 8, 3, 8, 3);
        titleRow.addView(datePill, wrap());

        ImageView closeBtn = new ImageView(activity);
        int cbSize = Ui.dp(activity, 20.0f);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(cbSize, cbSize);
        cbLp.leftMargin = Ui.dp(activity, 10.0f);
        closeBtn.setLayoutParams(cbLp);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setColorFilter(0xCCFFFFFF);
        closeBtn.setClickable(true);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        titleRow.addView(closeBtn);

        header.addView(titleRow, mw());

        TextView nameView = Cards.bold(activity, row.name, 17.0f, R.color.paper);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(nameView, mw());

        StringBuilder sub = new StringBuilder();
        sub.append("#").append(row.suffix());
        if (!row.branch.isEmpty()) {
            sub.append("  ·  ").append(row.branch);
        }
        if (!row.mobile.isEmpty()) {
            sub.append("  ·  ").append(row.mobile);
        }
        header.addView(Cards.label(activity, sub.toString(), R.style.Text_Meta, R.color.hero_text_dim), mw());

        // Quick action buttons (Call / WhatsApp / Follow-up)
        if (row.mobile.length() > 0) {
            LinearLayout callRow = new LinearLayout(activity);
            callRow.setOrientation(0);
            callRow.setGravity(16);
            LinearLayout.LayoutParams callRowLp = mw();
            callRowLp.topMargin = Ui.dp(activity, 8.0f);
            callRow.setLayoutParams(callRowLp);

            callRow.addView(sheetMiniAction(activity, "Call", R.drawable.ic_phone, R.color.paper, 0x22FFFFFF, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PhoneActions.call(activity, row);
                }
            }), weight1());

            View v1 = new View(activity);
            v1.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity, 6.0f), 1));
            callRow.addView(v1);

            callRow.addView(sheetMiniAction(activity, "WhatsApp", R.drawable.ic_whatsapp, R.color.paper, 0x22FFFFFF, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PhoneActions.whatsapp(activity, row);
                }
            }), weight1());

            View v2 = new View(activity);
            v2.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity, 6.0f), 1));
            callRow.addView(v2);

            callRow.addView(sheetMiniAction(activity, "Follow-up", R.drawable.ic_note, R.color.paper, 0x22FFFFFF, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    FollowupSheet.open(activity, host, row, date);
                }
            }), weight1());

            header.addView(callRow);
        }

        root.addView(header, mw());

        // Scrollable Fields Body
        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(1);
        Ui.pad(body, 16, 10, 16, 14);
        scroll.addView(body, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        root.addView(scroll, scrollLp);

        final List<Field> fieldsList = new ArrayList<>();
        final TextView totalValue = Cards.bold(activity, "0", 24.0f, R.color.primary_700);
        final TextView totalSub = Cards.label(activity, "", R.style.Text_Meta, R.color.ink_3);
        final TextView errorLine = Cards.label(activity, "", R.style.Text_Meta, R.color.danger);
        errorLine.setVisibility(8);

        final Runnable recalculateTotals = new Runnable() {
            @Override
            public void run() {
                int sum = 0;
                int targetSum = 0;
                for (Field f : fieldsList) {
                    sum += f.value();
                    targetSum += f.target;
                }
                totalValue.setText(Integer.toString(sum));
                StringBuilder sb = new StringBuilder();
                if (targetSum > 0) {
                    sb.append("Target ").append(targetSum);
                    sb.append("  ·  ").append(Math.min(999, (int) Math.round((sum * 100.0d) / targetSum))).append("% of target");
                }
                if (row.yesterdayTotal != null) {
                    if (sb.length() > 0) {
                        sb.append("  ·  ");
                    }
                    sb.append("Yesterday ").append(row.yesterdayTotal).append(" (").append(Ui.delta(sum - row.yesterdayTotal.intValue())).append(")");
                }
                totalSub.setText(sb);
                totalSub.setVisibility(sb.length() == 0 ? 8 : 0);
            }
        };

        List<String> keys = Params.keys(dbRead);
        Map<String, String> labels = Params.labels(dbRead);
        Map<String, String> shorts = Params.shorts(dbRead);
        LinkedHashMap<String, List<String>> groups = new LinkedHashMap<>();
        for (String k : keys) {
            String g = groupOf(k);
            List<String> gl = groups.get(g);
            if (gl == null) {
                gl = new ArrayList<>();
                groups.put(g, gl);
            }
            gl.add(k);
        }

        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            Section section = new Section(activity);
            section.set(entry.getKey());
            body.addView(section, top(activity, 6));

            LinearLayout card = Cards.card(activity);
            body.addView(card, top(activity, 2));

            List<String> glist = entry.getValue();
            for (int i = 0; i < glist.size(); i++) {
                if (i % 2 == 0) {
                    LinearLayout rowLayout = new LinearLayout(activity);
                    rowLayout.setOrientation(0);
                    card.addView(rowLayout, mw());
                }
                LinearLayout currentRow = (LinearLayout) card.getChildAt(card.getChildCount() - 1);
                if (i > 0 && i % 2 == 1) {
                    View spacer = new View(activity);
                    spacer.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity, 10.0f), 1));
                    currentRow.addView(spacer);
                }
                String fk = glist.get(i);
                Field f = createField(activity, row, fk, labels.get(fk), shorts.get(fk), recalculateTotals);
                fieldsList.add(f);
                currentRow.addView(f.container(activity), weight1());

                if (i == glist.size() - 1 && i % 2 == 0) {
                    View spacer = new View(activity);
                    spacer.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity, 10.0f), 1));
                    currentRow.addView(spacer);
                    View emptyCell = new View(activity);
                    currentRow.addView(emptyCell, weight1());
                }
            }
        }

        // Remarks Section
        Section remSec = new Section(activity);
        remSec.set("Remarks");
        body.addView(remSec, top(activity, 8));

        final EditText remarksEdit = new EditText(activity);
        remarksEdit.setHint("Optional remarks · max 500 chars");
        Ui.size(remarksEdit, 14.5f);
        remarksEdit.setTextColor(Ui.color(activity, R.color.ink));
        remarksEdit.setMinLines(2);
        remarksEdit.setGravity(8388659);
        remarksEdit.setBackground(Ui.round(activity, Ui.color(activity, R.color.paper), 10.0f, Ui.color(activity, R.color.line_2), 1.0f));
        Ui.pad(remarksEdit, 10, 10, 10, 10);
        if (row.remarks != null) {
            remarksEdit.setText(row.remarks);
        }
        body.addView(remarksEdit, top(activity, 2));

        final List<String> allSuggestions = new ArrayList<>();
        String effectiveReason = row.effectiveReasonOrPromise();
        if (!effectiveReason.isEmpty()) {
            allSuggestions.add(effectiveReason);
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

        final org.cspdaily.report.ui.widget.Chips chipsRemarks = new org.cspdaily.report.ui.widget.Chips(activity);
        chipsRemarks.allowNone(true);
        chipsRemarks.set(allSuggestions);
        chipsRemarks.onChange(new org.cspdaily.report.ui.widget.Chips.OnPick() {
            @Override
            public void pick(int i, String str2) {
                if (str2 != null && !str2.isEmpty()) {
                    remarksEdit.setText(str2);
                    remarksEdit.setSelection(str2.length());
                }
            }
        });
        body.addView(chipsRemarks, top(activity, 6));

        final boolean[] updatingRemarks = {false};
        remarksEdit.addTextChangedListener(new TextWatcher() {
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

        body.addView(errorLine, top(activity, 6));

        // Totals Card
        LinearLayout totalCard = Cards.card(activity);
        totalCard.setOrientation(1);
        Ui.pad(totalCard, 12, 10, 12, 10);
        totalCard.addView(Cards.label(activity, "TOTAL FIGURES", R.style.Text_Section, R.color.ink_3), mw());
        totalCard.addView(totalValue, mw());
        totalCard.addView(totalSub, mw());
        body.addView(totalCard, top(activity, 10));

        // Bottom Sticky Action Bar
        LinearLayout bottomBar = new LinearLayout(activity);
        bottomBar.setOrientation(0);
        bottomBar.setGravity(16);
        bottomBar.setBackgroundResource(R.drawable.bg_sticky_bar);
        Ui.pad(bottomBar, 16, 8, 16, 14);
        root.addView(bottomBar, mw());

        if (row.reported) {
            LinearLayout.LayoutParams unfileLp = new LinearLayout.LayoutParams(-2, -2);
            unfileLp.rightMargin = Ui.dp(activity, 10.0f);
            bottomBar.addView(Cards.link(activity, "Un-file", R.color.danger, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(activity)
                        .setTitle("Un-file " + row.name + "'s report?")
                        .setMessage("The CSP will return to Pending status and figures for " + Biz.friendlyDate(date) + " will be cleared.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Un-file", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                boolean deleted = Day.deleteReport(Core.get(activity).db(), row.id, date);
                                if (deleted) {
                                    SupabaseClient.deleteAsync(activity, "daily_report", "csp_id=eq." + row.id + "&business_date=eq." + date);
                                    SupabaseClient.deleteAsync(activity, "hourly_report", "csp_id=eq." + row.id + "&business_date=eq." + date);
                                }
                                dialog.dismiss();
                                host.toast(deleted ? "Report cleared" : "Nothing to clear");
                                host.refresh();
                            }
                        }).show();
                }
            }), unfileLp);
        }

        bottomBar.addView(Cards.primaryButton(activity, row.reported ? "Update report" : "Save report", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (Field f : fieldsList) {
                    if (f.invalid()) {
                        f.input.setBackgroundResource(R.drawable.bg_input_error);
                        errorLine.setText(f.label + " must be a whole number.");
                        errorLine.setVisibility(0);
                        f.input.requestFocus();
                        return;
                    }
                    f.input.setBackgroundResource(R.drawable.bg_input);
                }
                String rem = remarksEdit.getText().toString().trim();
                if (rem.length() > 500) {
                    errorLine.setText("Remarks must be 500 characters or fewer.");
                    errorLine.setVisibility(0);
                    return;
                }
                LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
                for (Field f2 : fieldsList) {
                    map.put(f2.key, f2.value());
                }
                SQLiteDatabase db = Core.get(activity).db();
                String saved = Day.saveReport(db, row.id, date, map, rem);
                DaySummary summary = Day.summary(db, date);
                if (summary.isComplete()) {
                    Reminders.reportComplete(activity, summary);
                }
                Ui.hideKeyboard(activity);
                dialog.dismiss();
                host.toast("Report " + saved + " · " + summary.reported + "/" + summary.total + " filed");
                host.refresh();
            }
        }), weight1());

        recalculateTotals.run();

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(-1, Ui.dp(activity, 600));
            dialog.getWindow().setGravity(80);
            dialog.getWindow().setWindowAnimations(R.style.Animation_Sheet);
        }
        dialog.show();
    }

    private static Field createField(final Context context, CspDay row, String key, String label, String shortLabel, final Runnable onUpdate) {
        int target = (row == null || row.targets.get(key) == null) ? 0 : row.targets.get(key);
        int currentVal = (row == null || row.values.get(key) == null) ? 0 : row.values.get(key);

        EditText editText = new EditText(context);
        editText.setInputType(2);
        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        editText.setHint("0");
        editText.setGravity(17);
        Ui.size(editText, 19.0f);
        editText.setTypeface(Typeface.DEFAULT_BOLD);
        editText.setTextColor(Ui.color(context, R.color.ink));
        editText.setMinHeight(Ui.dp(context, 48.0f));
        editText.setSingleLine(true);
        editText.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), 12.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(editText, 8, 8, 8, 8);
        if (currentVal > 0) {
            editText.setText(Integer.toString(currentVal));
        }

        TextView hint = Cards.label(context, "", R.style.Text_Nano, R.color.ink_4);
        hint.setGravity(17);
        Ui.pad(hint, 0, 3, 0, 0);

        StringBuilder hsb = new StringBuilder();
        if (target > 0) {
            hsb.append("tgt ").append(target);
        }
        if (row != null && row.yesterdayValues.get(key) != null) {
            if (hsb.length() > 0) {
                hsb.append(" · ");
            }
            hsb.append("yest ").append(row.yesterdayValues.get(key));
        }
        if (hsb.length() > 0) {
            hint.setText(hsb);
            hint.setVisibility(0);
        } else {
            hint.setVisibility(8);
        }

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                onUpdate.run();
            }
        });

        return new Field(key, label, shortLabel, target, editText, hint);
    }

    private static String groupOf(String str) {
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

    private static View sheetMiniAction(Context context, String label, int iconRes, int tintColorRes, int bgCol, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(17);
        row.setBackground(Ui.round(context, bgCol, 10.0f, 0x33FFFFFF, 1.0f));
        Ui.pad(row, 6, 8, 6, 8);
        row.setClickable(true);
        row.setOnClickListener(onClick);

        ImageView iv = new ImageView(context);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(Ui.dp(context, 14.0f), Ui.dp(context, 14.0f));
        ivLp.rightMargin = Ui.dp(context, 4.0f);
        iv.setLayoutParams(ivLp);
        iv.setImageResource(iconRes);
        iv.setColorFilter(Ui.color(context, tintColorRes));
        row.addView(iv);

        TextView tv = new TextView(context);
        tv.setText(label);
        Ui.size(tv, 12.0f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Ui.color(context, tintColorRes));
        tv.setSingleLine(true);
        row.addView(tv, wrap());
        return row;
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private static LinearLayout.LayoutParams top(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }

    private static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }
}
