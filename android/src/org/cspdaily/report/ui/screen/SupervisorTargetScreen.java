package org.cspdaily.report.ui.screen;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.ui.widget.Section;

public class SupervisorTargetScreen extends Screen {
    private String date;
    private List<User> supervisors = new ArrayList<>();
    private int selectedSupIdx = 0;
    private final Map<String, EditText> paramInputs = new LinkedHashMap<>();
    private final LinearLayout targetContainer;
    private final TextView subtitleText;
    private final Chips supChips;

    public SupervisorTargetScreen(final Context context, final Screen.Host host, String d) {
        super(context, host);
        this.date = d == null ? Biz.today() : d;
        title("Supervisor Targets");
        subtitle("Set parameter targets per supervisor");

        SQLiteDatabase db = Core.get(context).dbRead();
        this.supervisors = User.supervisors(db);

        // Date selection
        LinearLayout dateBar = Cards.cardFlat(context);
        dateBar.setOrientation(0);
        dateBar.setGravity(16);
        Ui.pad(dateBar, 12, 10, 12, 10);
        dateBar.addView(Cards.bold(context, "Date: " + Biz.fmtDmy(this.date) + " (" + Biz.dayName(this.date) + ")", 14.5f, R.color.primary_700), weight1());
        add(dateBar, 0);

        // Supervisor selector chips
        this.supChips = new Chips(context);
        this.supChips.allowNone(false);
        updateSupervisorChips();
        this.supChips.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                selectedSupIdx = i;
                loadTargetsForSelectedSupervisor();
            }
        });
        add(this.supChips, 8);

        this.subtitleText = Cards.label(context, "", R.style.Text_Meta, R.color.ink_3);
        add(this.subtitleText, 4);

        this.targetContainer = new LinearLayout(context);
        this.targetContainer.setOrientation(1);
        add(this.targetContainer, 4);

        // Save Button
        View saveBtn = Cards.primaryButton(context, "Save Supervisor Targets", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SupervisorTargetScreen.this.saveTargets();
            }
        });
        add(saveBtn, 12);

        buildParamInputs(context);
        loadTargetsForSelectedSupervisor();
    }

    @Override
    public String headline() {
        return "Supervisor Targets";
    }

    private void updateSupervisorChips() {
        if (this.supervisors.isEmpty()) {
            this.supChips.set("No supervisors");
        } else {
            String[] names = new String[this.supervisors.size()];
            for (int i = 0; i < this.supervisors.size(); i++) {
                names[i] = this.supervisors.get(i).name;
            }
            this.supChips.set(names);
            if (this.selectedSupIdx < names.length) {
                this.supChips.select(this.selectedSupIdx);
            }
        }
    }

    private void buildParamInputs(Context context) {
        this.targetContainer.removeAllViews();
        this.paramInputs.clear();

        if (this.supervisors.isEmpty()) {
            Empty empty = new Empty(context);
            empty.show(R.drawable.ic_person, R.color.line_2, "No supervisors found", "Go to Users & Supervisors to add supervisors first.");
            this.targetContainer.addView(empty, mw());
            return;
        }

        Section sec = new Section(context);
        sec.set("Daily Parameter Targets");
        this.targetContainer.addView(sec, 0);

        LinearLayout card = Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 12, 10, 12, 10);
        this.targetContainer.addView(card, mw());

        List<Params.P> params = Params.all(Core.get(context).dbRead());
        for (Params.P p : params) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(0);
            row.setGravity(16);
            Ui.pad(row, 6, 6, 6, 6);

            TextView label = new TextView(context);
            label.setText(p.label);
            Ui.size(label, 14.5f);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(Ui.color(context, R.color.ink));
            row.addView(label, weight1());

            EditText edit = new EditText(context);
            edit.setInputType(2);
            edit.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            edit.setHint("0");
            edit.setText("0");
            edit.setGravity(17);
            Ui.size(edit, 16.0f);
            edit.setTypeface(Typeface.DEFAULT_BOLD);
            edit.setSingleLine(true);
            edit.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line_2), 1.0f));
            Ui.pad(edit, 12, 8, 12, 8);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(Ui.dp(context, 75.0f), Ui.dp(context, 40.0f));
            edit.setLayoutParams(elp);

            row.addView(edit);
            card.addView(row, mw());
            this.paramInputs.put(p.key, edit);
        }
    }

    private void loadTargetsForSelectedSupervisor() {
        if (this.supervisors.isEmpty() || this.selectedSupIdx >= this.supervisors.size()) return;
        User supervisor = this.supervisors.get(this.selectedSupIdx);
        SQLiteDatabase db = Core.get(getContext()).dbRead();
        Set<Long> cspIds = SupervisorMap.getCspIdsForSupervisor(db, supervisor.id);

        this.subtitleText.setText("Supervisor " + supervisor.name + " has " + cspIds.size() + " mapped CSPs. Setting targets will distribute them across their mapped CSPs.");

        // Clear inputs first
        for (EditText edit : this.paramInputs.values()) {
            edit.setText("0");
        }

        if (cspIds.isEmpty()) return;

        // Sum current targets for mapped CSPs
        SupervisorMap.SupervisorStats stats = SupervisorMap.getStatsForSupervisor(db, supervisor, this.date);
        for (Map.Entry<String, Integer> e : stats.paramTargets.entrySet()) {
            EditText edit = this.paramInputs.get(e.getKey());
            if (edit != null) {
                edit.setText(String.valueOf(e.getValue()));
            }
        }
    }

    private void saveTargets() {
        if (this.supervisors.isEmpty() || this.selectedSupIdx >= this.supervisors.size()) return;
        User supervisor = this.supervisors.get(this.selectedSupIdx);
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).db();

        Set<Long> cspIds = SupervisorMap.getCspIdsForSupervisor(db, supervisor.id);
        if (cspIds.isEmpty()) {
            this.host.toast("Cannot assign targets: " + supervisor.name + " has no mapped CSPs. Map CSPs first.");
            return;
        }

        int numCsps = cspIds.size();
        Map<String, Integer> targetPerCsp = new LinkedHashMap<>();

        for (Map.Entry<String, EditText> e : this.paramInputs.entrySet()) {
            int totalForParam = Biz.cleanCount(e.getValue().getText().toString());
            // Per CSP target
            int perCsp = (int) Math.ceil((double) totalForParam / numCsps);
            targetPerCsp.put(e.getKey(), perCsp);
        }

        // Apply to all mapped CSPs for this supervisor
        try {
            db.beginTransaction();
            for (Long cspId : cspIds) {
                for (Map.Entry<String, Integer> e : targetPerCsp.entrySet()) {
                    Day.setDailyTarget(db, cspId.longValue(), this.date, e.getKey(), e.getValue() != null ? e.getValue().intValue() : 0);
                }
            }
            db.setTransactionSuccessful();
            this.host.toast("Targets saved for " + supervisor.name + " (" + numCsps + " CSPs updated)");
        } catch (Throwable th) {
            this.host.toast("Error saving targets: " + th.getMessage());
        } finally {
            try { db.endTransaction(); } catch (Throwable ignored) {}
        }
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }
}
