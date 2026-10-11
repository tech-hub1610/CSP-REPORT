package org.cspdaily.report.ui.screen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Section;

/* loaded from: classes.dex */
public class CspEditScreen extends Screen {
    private EditText branch;
    private EditText branchCode;
    private EditText code;
    private final long editId;
    private TextView error;
    private String gender;
    private Chips genderChips;
    private EditText mobile;
    private EditText name;
    private EditText notes;
    private String status;
    private Chips statusChips;
    private final Map<String, EditText> targets;

    public CspEditScreen(Context context, Screen.Host host, long j) {
        super(context, host);
        this.targets = new LinkedHashMap();
        this.gender = "M";
        this.status = Csp.ACTIVE;
        this.editId = j;
        title(j == 0 ? "Add CSP" : "Edit CSP");
        subtitle(j == 0 ? "Register a banking correspondent" : "Update the master record");
        if (j == 0) {
            headerAction("Import CSV", new Runnable() {
                @Override
                public void run() {
                    host.push(new CspImportScreen(context, host));
                }
            });

            LinearLayout importBanner = Cards.cardFlat(context);
            importBanner.setOrientation(0);
            importBanner.setGravity(16);
            Ui.pad(importBanner, 12, 10, 12, 10);
            importBanner.setBackground(Ui.round(context, Ui.color(context, R.color.primary_50), Ui.rdp(context, R.dimen.r_md), Ui.color(context, R.color.line_2), 1.0f));

            TextView prompt = new TextView(context);
            prompt.setText("Have multiple CSPs to add? Bulk import from CSV with column specs.");
            prompt.setTextAppearance(context, R.style.Text_Meta);
            prompt.setTextColor(Ui.color(context, R.color.ink));
            importBanner.addView(prompt, new LinearLayout.LayoutParams(0, -2, 1.0f));

            View btn = Cards.link(context, "Import CSV", R.color.primary_600, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    host.push(new CspImportScreen(context, host));
                }
            });
            importBanner.addView(btn, new LinearLayout.LayoutParams(-2, -2));
            add(importBanner, 0);
        }
        this.error = Cards.label(context, "", R.style.Text_Meta, R.color.danger);
        this.error.setVisibility(8);
        buildMaster(context);
        buildTargets(context);
        load();
        add(this.error, 8);
        add(Cards.primaryButton(context, j == 0 ? "Add CSP" : "Save changes", new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.CspEditScreen.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CspEditScreen.this.save();
            }
        }), 10);
        if (j > 0) {
            add(Cards.link(context, "Delete CSP", R.color.danger, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CspEditScreen.this.confirmDelete();
                }
            }), 6);
        }
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return this.editId == 0 ? "Add CSP" : "Edit CSP";
    }

    private void buildMaster(Context context) {
        Section section = new Section(context);
        section.set("Master record");
        add(section, 0);
        LinearLayout card = Cards.card(context);
        card.setPadding(0, 0, 0, 0);
        add(card, 0);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        Ui.pad(linearLayout, 14, 2, 14, 14);
        card.addView(linearLayout, mw());
        linearLayout.addView(label(context, "CSP code"), mw());
        this.code = input(context, "e.g. 32250091", 2, 12);
        linearLayout.addView(this.code, top(context, 4));
        linearLayout.addView(label(context, "CSP name"), top(context, 12));
        this.name = input(context, "Full name as on the register", 8193, 0);
        linearLayout.addView(this.name, top(context, 4));
        linearLayout.addView(label(context, "Mobile number"), top(context, 12));
        this.mobile = input(context, "10 digits", 3, 12);
        linearLayout.addView(this.mobile, top(context, 4));
        linearLayout.addView(label(context, "Gender"), top(context, 12));
        this.genderChips = new Chips(context);
        this.genderChips.set("Male", "Female");
        this.genderChips.allowNone(false);
        this.genderChips.select(0);
        this.genderChips.onChange(new Chips.OnPick() { // from class: org.cspdaily.report.ui.screen.CspEditScreen.2
            @Override // org.cspdaily.report.ui.widget.Chips.OnPick
            public void pick(int i, String str) {
                CspEditScreen.this.gender = "Female".equals(str) ? "F" : "M";
            }
        });
        linearLayout.addView(this.genderChips, top(context, 4));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setLayoutParams(top(context, 12));
        linearLayout.addView(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(context);
        linearLayout3.setOrientation(1);
        linearLayout2.addView(linearLayout3, weight1());
        linearLayout3.addView(label(context, "Branch name"), mw());
        this.branch = input(context, "e.g. KARNAJORA", 8193, 0);
        linearLayout3.addView(this.branch, top(context, 4));
        LinearLayout linearLayout4 = new LinearLayout(context);
        linearLayout4.setOrientation(1);
        LinearLayout.LayoutParams weight1 = weight1();
        weight1.leftMargin = Ui.dp(context, 10.0f);
        linearLayout4.setLayoutParams(weight1);
        linearLayout2.addView(linearLayout4);
        linearLayout4.addView(label(context, "Branch code"), mw());
        this.branchCode = input(context, "4-6 digits", 2, 6);
        linearLayout4.addView(this.branchCode, top(context, 4));
        linearLayout.addView(label(context, "Status"), top(context, 12));
        this.statusChips = new Chips(context);
        this.statusChips.set("Active", "Inactive");
        this.statusChips.allowNone(false);
        this.statusChips.select(0);
        this.statusChips.onChange(new Chips.OnPick() { // from class: org.cspdaily.report.ui.screen.CspEditScreen.3
            @Override // org.cspdaily.report.ui.widget.Chips.OnPick
            public void pick(int i, String str) {
                CspEditScreen.this.status = "Inactive".equals(str) ? Csp.INACTIVE : Csp.ACTIVE;
            }
        });
        linearLayout.addView(this.statusChips, top(context, 4));
        linearLayout.addView(label(context, "Notes"), top(context, 12));
        this.notes = input(context, "Optional", 1, 0);
        this.notes.setMinLines(2);
        this.notes.setSingleLine(false);
        this.notes.setGravity(8388659);
        linearLayout.addView(this.notes, top(context, 4));
    }

    private void buildTargets(Context context) {
        Section section = new Section(context);
        section.set("Daily targets");
        add(section, 6);
        add(Cards.label(context, "Drives the target line on report entry and monthly achievement.", R.style.Text_Meta, R.color.ink_4), 0);
        LinearLayout card = Cards.card(context);
        add(card, 6);
        List<Params.P> all = Params.all(Core.get(context).dbRead());
        LinearLayout linearLayout = null;
        int i = 0;
        while (i < all.size()) {
            if (i % 2 == 0) {
                linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(0);
                card.addView(linearLayout, mw());
            }
            LinearLayout linearLayout2 = linearLayout;
            Params.P p = all.get(i);
            LinearLayout linearLayout3 = new LinearLayout(context);
            linearLayout3.setOrientation(1);
            LinearLayout.LayoutParams weight1 = weight1();
            if (i % 2 == 1) {
                weight1.leftMargin = Ui.dp(context, 10.0f);
            }
            linearLayout3.setLayoutParams(weight1);
            linearLayout3.addView(Cards.label(context, p.shortLabel.isEmpty() ? p.label : p.shortLabel, R.style.Text_Label, R.color.ink_2), mw());
            EditText editText = new EditText(context);
            editText.setInputType(2);
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
            editText.setHint("0");
            editText.setGravity(17);
            Ui.size(editText, 17.0f);
            editText.setTypeface(Typeface.DEFAULT_BOLD);
            editText.setSingleLine(true);
            editText.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line_2), 1.0f));
            Ui.pad(editText, 8, 10, 8, 10);
            linearLayout3.addView(editText, top(context, 4));
            linearLayout2.addView(linearLayout3);
            this.targets.put(p.key, editText);
            i++;
            linearLayout = linearLayout2;
        }
    }

    private EditText input(Context context, String str, int i, int i2) {
        EditText editText = new EditText(context);
        editText.setHint(str);
        editText.setInputType(i);
        editText.setSingleLine(i2 > 0);
        Ui.size(editText, 15.0f);
        editText.setTextColor(Ui.color(context, R.color.ink));
        editText.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(editText, 12, 13, 12, 13);
        if (i2 > 0) {
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i2)});
        }
        return editText;
    }

    private TextView label(Context context, String str) {
        TextView label = Cards.label(context, str, R.style.Text_Label, R.color.ink_2);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        return label;
    }

    private void load() {
        Csp byId;
        if (this.editId > 0 && (byId = Csp.byId(Core.get(getContext()).dbRead(), this.editId)) != null) {
            this.code.setText(byId.code);
            this.code.setEnabled(false);
            this.name.setText(byId.name);
            this.mobile.setText(byId.mobile);
            this.branch.setText(byId.branch);
            this.branchCode.setText(byId.branchCode);
            this.notes.setText(byId.notes);
            this.gender = (byId.gender == null || byId.gender.isEmpty()) ? "M" : byId.gender;
            this.genderChips.select("F".equalsIgnoreCase(this.gender) ? 1 : 0);
            this.status = (byId.status == null || byId.status.isEmpty()) ? Csp.ACTIVE : byId.status;
            this.statusChips.select(Csp.INACTIVE.equals(this.status) ? 1 : 0);
            for (Map.Entry<String, EditText> entry : this.targets.entrySet()) {
                Integer num = byId.targets.get(entry.getKey());
                if (num != null && num.intValue() > 0) {
                    entry.getValue().setText(Integer.toString(num.intValue()));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void save() {
        Context context = getContext();
        String trim = this.code.getText().toString().trim();
        String trim2 = this.name.getText().toString().trim();
        if (trim.isEmpty() || trim2.isEmpty()) {
            this.error.setText("CSP code and name are both required.");
            this.error.setVisibility(0);
            return;
        }
        this.error.setVisibility(8);
        SQLiteDatabase db = Core.get(context).db();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Map.Entry<String, EditText> entry : this.targets.entrySet()) {
            int cleanCount = Biz.cleanCount(entry.getValue().getText().toString());
            String key = entry.getKey();
            if (cleanCount < 0) {
                cleanCount = 0;
            }
            linkedHashMap.put(key, Integer.valueOf(cleanCount));
        }
        Csp csp = new Csp();
        csp.id = this.editId;
        csp.code = trim;
        csp.name = trim2;
        csp.gender = this.gender;
        csp.mobile = this.mobile.getText().toString().trim();
        csp.branch = this.branch.getText().toString().trim();
        csp.branchCode = this.branchCode.getText().toString().trim();
        csp.status = this.status;
        csp.notes = this.notes.getText().toString().trim();
        csp.targets = linkedHashMap;
        if (this.editId == 0) {
            try {
                db.beginTransaction();
                if (Csp.insert(context, db, csp) <= 0) {
                    this.error.setText("That CSP code already exists in the register.");
                    this.error.setVisibility(0);
                    return;
                } else {
                    db.setTransactionSuccessful();
                    this.host.toast("CSP added");
                }
            } finally {
                db.endTransaction();
            }
        } else {
            Csp.update(db, csp);
            this.host.toast("CSP updated");
        }
        this.host.pop();
    }

    private void confirmDelete() {
        final Context context = getContext();
        Csp existing = Csp.byId(Core.get(context).dbRead(), this.editId);
        final String cspName = existing != null ? existing.name : "this CSP";
        new AlertDialog.Builder(context)
            .setTitle("Delete " + cspName + "?")
            .setMessage("Removes this CSP together with its reports, hourly snapshots, pending reasons and follow-up notes. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    long delId = CspEditScreen.this.editId;
                    Csp.delete(Core.get(context).db(), delId);
                    SupabaseClient.deleteAsync(context, "daily_report", "csp_id=eq." + delId);
                    SupabaseClient.deleteAsync(context, "hourly_report", "csp_id=eq." + delId);
                    SupabaseClient.deleteAsync(context, "pending_reason", "csp_id=eq." + delId);
                    SupabaseClient.deleteAsync(context, "followup_log", "csp_id=eq." + delId);
                    if (existing != null && existing.code != null && !existing.code.isEmpty()) {
                        SupabaseClient.deleteAsync(context, "csp", "csp_code=eq." + existing.code);
                    }
                    CspEditScreen.this.host.toast("Deleted " + cspName);
                    CspEditScreen.this.host.pop();
                }
            }).show();
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
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
