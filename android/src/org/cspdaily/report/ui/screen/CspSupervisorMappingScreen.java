package org.cspdaily.report.ui.screen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Empty;

public class CspSupervisorMappingScreen extends Screen {
    private final long supervisorId;
    private User supervisor;
    private List<Csp> allCsps = new ArrayList<>();
    private final Set<Long> selectedCspIds = new HashSet<>();
    private final TextView countLine;
    private final Empty empty;
    private final LinearLayout list;
    private EditText search;
    private String query = "";

    public CspSupervisorMappingScreen(final Context context, final Screen.Host host, long supId) {
        super(context, host);
        this.supervisorId = supId;

        SQLiteDatabase db = Core.get(context).dbRead();
        this.supervisor = User.byId(db, supId);
        String supName = this.supervisor != null ? this.supervisor.name : "Supervisor";

        title("Map CSPs");
        subtitle("Assign CSPs to " + supName);

        headerAction("Map Branch", new Runnable() {
            @Override
            public void run() {
                CspSupervisorMappingScreen.this.openBranchPicker();
            }
        });

        add(searchView(context), 0);

        // Action bar for Select All / Deselect All / Save
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(0);
        bar.setGravity(16);
        Ui.pad(bar, 2, 8, 2, 8);

        this.countLine = Cards.label(context, "", R.style.Text_Section, R.color.ink_3);
        bar.addView(this.countLine, weight1());

        View selectAllBtn = Cards.link(context, "Select All", R.color.primary_700, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Csp csp : allCsps) selectedCspIds.add(csp.id);
                render();
            }
        });
        bar.addView(selectAllBtn, wrap());

        View sp = new View(context);
        bar.addView(sp, new LinearLayout.LayoutParams(Ui.dp(context, 12.0f), 1));

        View clearBtn = Cards.link(context, "Clear All", R.color.danger, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCspIds.clear();
                render();
            }
        });
        bar.addView(clearBtn, wrap());

        add(bar, 0);

        // Save Button
        View saveBtn = Cards.primaryButton(context, "Save CSP Assignments", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CspSupervisorMappingScreen.this.saveAssignments();
            }
        });
        add(saveBtn, 0);

        this.list = new LinearLayout(context);
        this.list.setOrientation(1);
        add(this.list, 8);

        this.empty = new Empty(context);
        add(this.empty, 0);

        loadMappings();
        refreshContent();
    }

    @Override
    public String headline() {
        return "Map CSPs";
    }

    private void loadMappings() {
        SQLiteDatabase db = Core.get(getContext()).dbRead();
        this.selectedCspIds.clear();
        this.selectedCspIds.addAll(SupervisorMap.getCspIdsForSupervisor(db, this.supervisorId));
    }

    private LinearLayout searchView(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), Ui.rdp(context, R.dimen.r_md), Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(linearLayout, 12, 0, 6, 0);
        linearLayout.setMinimumHeight(Ui.dp(context, 46.0f));

        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(context, 18.0f), Ui.dp(context, 18.0f));
        layoutParams.rightMargin = Ui.dp(context, 8.0f);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.ic_search);
        imageView.setColorFilter(Ui.color(context, R.color.ink_4));
        linearLayout.addView(imageView);

        this.search = new EditText(context);
        this.search.setHint("Search name, code, branch");
        this.search.setBackground(null);
        Ui.size(this.search, 14.5f);
        this.search.setTextColor(Ui.color(context, R.color.ink));
        this.search.setHintTextColor(Ui.color(context, R.color.ink_4));
        this.search.setSingleLine(true);
        linearLayout.addView(this.search, new LinearLayout.LayoutParams(0, Ui.dp(context, 46.0f), 1.0f));

        this.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                CspSupervisorMappingScreen.this.query = editable.toString().trim().toLowerCase(Locale.US);
                CspSupervisorMappingScreen.this.render();
            }
        });

        return linearLayout;
    }

    @Override
    public void refreshContent() {
        SQLiteDatabase db = Core.get(getContext()).dbRead();
        this.allCsps = Csp.all(db, false, "name");
        render();
    }

    private void render() {
        Context context = getContext();
        ArrayList<Csp> filtered = new ArrayList<>();
        for (Csp csp : this.allCsps) {
            if (this.query.isEmpty() || (csp.name + " " + csp.code + " " + csp.branch + " " + csp.branchCode + " " + Biz.cspCodeSuffix(csp.code)).toLowerCase(Locale.US).contains(this.query)) {
                filtered.add(csp);
            }
        }

        Collections.sort(filtered, new Comparator<Csp>() {
            @Override
            public int compare(Csp a, Csp b) {
                boolean aSel = selectedCspIds.contains(a.id);
                boolean bSel = selectedCspIds.contains(b.id);
                if (aSel != bSel) return aSel ? -1 : 1;
                String na = a.name == null ? "" : a.name.toLowerCase(Locale.US);
                String nb = b.name == null ? "" : b.name.toLowerCase(Locale.US);
                return na.compareTo(nb);
            }
        });

        this.countLine.setText(selectedCspIds.size() + " of " + allCsps.size() + " CSPs mapped");
        this.list.removeAllViews();

        if (filtered.isEmpty()) {
            this.empty.show(R.drawable.ic_search, R.color.line_2, "No CSPs match", "Try a different search.");
            return;
        }
        this.empty.hide();

        for (int i = 0; i < filtered.size(); i++) {
            final Csp csp = filtered.get(i);
            View row = buildCspCheckCard(context, csp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 6.0f);
            this.list.addView(row, lp);
        }
    }

    private View buildCspCheckCard(final Context context, final Csp csp) {
        LinearLayout card = Cards.cardFlat(context);
        card.setOrientation(0);
        card.setGravity(16);
        Ui.pad(card, 12, 10, 12, 10);

        final CheckBox cb = new CheckBox(context);
        cb.setChecked(this.selectedCspIds.contains(csp.id));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectedCspIds.add(csp.id);
                } else {
                    selectedCspIds.remove(csp.id);
                }
                countLine.setText(selectedCspIds.size() + " of " + allCsps.size() + " CSPs mapped");
            }
        });
        card.addView(cb, wrap());

        card.addView(new Avatar(context, 36).bind(csp.name, csp.code), new LinearLayout.LayoutParams(Ui.dp(context, 36.0f), Ui.dp(context, 36.0f)));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(1);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        infoLp.leftMargin = Ui.dp(context, 10.0f);
        info.setLayoutParams(infoLp);
        card.addView(info);

        TextView nameText = new TextView(context);
        nameText.setText(csp.name);
        Ui.size(nameText, 14.5f);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setTextColor(Ui.color(context, R.color.ink));
        info.addView(nameText);

        TextView metaText = new TextView(context);
        metaText.setText(csp.code + (csp.branch.isEmpty() ? "" : " · " + csp.branch));
        metaText.setTextAppearance(context, R.style.Text_Meta);
        metaText.setTextColor(Ui.color(context, R.color.ink_3));
        info.addView(metaText);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.setChecked(!cb.isChecked());
            }
        });

        return card;
    }

    private void openBranchPicker() {
        final Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        final List<String> branches = new ArrayList<>();
        android.database.Cursor c = null;
        try {
            c = db.rawQuery("SELECT DISTINCT branch_name FROM csp WHERE branch_name != '' ORDER BY branch_name ASC", null);
            while (c != null && c.moveToNext()) {
                branches.add(c.getString(0));
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }

        if (branches.isEmpty()) {
            this.host.toast("No branches available in register");
            return;
        }

        final String[] branchArray = branches.toArray(new String[0]);
        new AlertDialog.Builder(context)
            .setTitle("Map All CSPs in Branch")
            .setItems(branchArray, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String chosenBranch = branchArray[which];
                    int added = 0;
                    for (Csp csp : allCsps) {
                        if (chosenBranch.equalsIgnoreCase(csp.branch)) {
                            selectedCspIds.add(csp.id);
                            added++;
                        }
                    }
                    CspSupervisorMappingScreen.this.host.toast("Mapped " + added + " CSPs from " + chosenBranch);
                    CspSupervisorMappingScreen.this.render();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveAssignments() {
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).db();
        SupervisorMap.setSupervisorCspBatch(db, this.supervisorId, new ArrayList<>(this.selectedCspIds));
        this.host.toast("Mappings updated (" + this.selectedCspIds.size() + " CSPs mapped)");
        this.host.pop();
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
}
