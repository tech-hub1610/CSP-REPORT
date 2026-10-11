package org.cspdaily.report.ui.screen;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Journal;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.ui.widget.Timeline;

public class ActivityScreen extends Screen {
    private final Empty empty;
    private final LinearLayout list;
    private final LinearLayout searchContainer;
    private final LinearLayout filterContainer;
    private final TextView statsBanner;
    private TextView dateLabel;
    private TextView allDatesBtn;

    private String selectedDate;
    private boolean isAllDates = false;
    private String searchQuery = "";
    private int kindFilter = 0; // 0 = All, 1 = Reports, 2 = Follow-ups
    private String branchFilter = "All Branches";
    private String statusFilter = "All Statuses";

    public ActivityScreen(Context context, Screen.Host host) {
        this(context, host, UiState.date(context));
    }

    public ActivityScreen(Context context, Screen.Host host, String initialDate) {
        super(context, host);
        this.selectedDate = (initialDate != null && !initialDate.isEmpty()) ? initialDate : UiState.date(context);
        title("Activity");
        subtitle(Biz.friendlyDate(this.selectedDate));

        // 1. Day Navigator & Date Bar
        add(buildDateBar(context), 0);

        // 2. Search Bar
        this.searchContainer = buildSearchBar(context);
        add(this.searchContainer, 6);

        // 3. Filter Container (Kind chips & Branch/Status filters)
        this.filterContainer = new LinearLayout(context);
        this.filterContainer.setOrientation(1);
        add(this.filterContainer, 6);

        // 4. Stats Banner
        this.statsBanner = Cards.label(context, "", R.style.Text_Meta, R.color.ink_3);
        Ui.pad(this.statsBanner, 2, 4, 2, 4);
        add(this.statsBanner, 4);

        // 5. Activity List
        this.list = new LinearLayout(context);
        this.list.setOrientation(1);
        add(this.list, 4);

        // 6. Empty State
        this.empty = new Empty(context);
        add(this.empty, 0);
    }

    @Override
    public String headline() {
        return "Activity";
    }

    private View buildDateBar(final Context context) {
        LinearLayout bar = Cards.card(context);
        bar.setOrientation(0);
        bar.setGravity(16);
        Ui.pad(bar, 10, 8, 10, 8);

        // Prev day
        TextView prevBtn = Cards.label(context, "◀", R.style.Text_CardTitle, R.color.primary_700);
        prevBtn.setGravity(17);
        Ui.pad(prevBtn, 10, 6, 10, 6);
        prevBtn.setClickable(true);
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAllDates = false;
                selectedDate = Biz.plusDays(selectedDate, -1);
                updateDateUI();
                refreshContent();
            }
        });
        bar.addView(prevBtn, wrap());

        // Date Display
        this.dateLabel = new TextView(context);
        this.dateLabel.setGravity(17);
        this.dateLabel.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(this.dateLabel, 13.5f);
        this.dateLabel.setTextColor(Ui.color(context, R.color.ink));
        this.dateLabel.setClickable(true);
        this.dateLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
        bar.addView(this.dateLabel, weight1());

        // Next day
        TextView nextBtn = Cards.label(context, "▶", R.style.Text_CardTitle, R.color.primary_700);
        nextBtn.setGravity(17);
        Ui.pad(nextBtn, 10, 6, 10, 6);
        nextBtn.setClickable(true);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAllDates = false;
                selectedDate = Biz.plusDays(selectedDate, 1);
                updateDateUI();
                refreshContent();
            }
        });
        bar.addView(nextBtn, wrap());

        // Toggle All Dates
        this.allDatesBtn = new TextView(context);
        this.allDatesBtn.setText("All Dates");
        this.allDatesBtn.setGravity(17);
        Ui.size(this.allDatesBtn, 11.5f);
        this.allDatesBtn.setTypeface(Typeface.DEFAULT_BOLD);
        this.allDatesBtn.setClickable(true);
        Ui.pad(this.allDatesBtn, 8, 5, 8, 5);
        this.allDatesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAllDates = !isAllDates;
                updateDateUI();
                refreshContent();
            }
        });
        bar.addView(this.allDatesBtn, wrap());

        updateDateUI();
        return bar;
    }

    private void updateDateUI() {
        if (dateLabel == null) return;
        Context context = getContext();
        if (isAllDates) {
            dateLabel.setText("All Recorded Dates");
            subtitle("All Dates");
            allDatesBtn.setText("Today");
            allDatesBtn.setTextColor(Ui.color(context, R.color.paper));
            allDatesBtn.setBackground(Ui.round(context, Ui.color(context, R.color.primary_700), 8.0f, Ui.color(context, R.color.primary_700), 1.0f));
        } else {
            dateLabel.setText(Biz.friendlyDate(selectedDate));
            subtitle(Biz.friendlyDate(selectedDate));
            allDatesBtn.setText("All Dates");
            allDatesBtn.setTextColor(Ui.color(context, R.color.primary_700));
            allDatesBtn.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), 8.0f, Ui.color(context, R.color.line), 1.0f));
        }
    }

    private void showDatePicker() {
        final Context context = getContext();
        try {
            int y = Integer.parseInt(selectedDate.substring(0, 4));
            int m = Integer.parseInt(selectedDate.substring(5, 7)) - 1;
            int d = Integer.parseInt(selectedDate.substring(8, 10));
            new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                    isAllDates = false;
                    updateDateUI();
                    refreshContent();
                }
            }, y, m, d).show();
        } catch (Exception ignored) {}
    }

    private LinearLayout buildSearchBar(final Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(16);
        row.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(row, 10, 6, 10, 6);

        ImageView searchIcon = new ImageView(context);
        int iconSize = Ui.dp(context, 18.0f);
        LinearLayout.LayoutParams silp = new LinearLayout.LayoutParams(iconSize, iconSize);
        silp.rightMargin = Ui.dp(context, 8.0f);
        searchIcon.setLayoutParams(silp);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(Ui.color(context, R.color.ink_3));
        row.addView(searchIcon);

        final EditText edit = new EditText(context);
        edit.setHint("Search activity, CSP, branch, note...");
        edit.setHintTextColor(Ui.color(context, R.color.ink_4));
        edit.setTextColor(Ui.color(context, R.color.ink));
        Ui.size(edit, 13.5f);
        edit.setBackground(null);
        edit.setSingleLine(true);
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim();
                refreshContent();
            }
        });
        row.addView(edit, weight1());

        final ImageView clearBtn = new ImageView(context);
        int cbSize = Ui.dp(context, 18.0f);
        clearBtn.setLayoutParams(new LinearLayout.LayoutParams(cbSize, cbSize));
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setColorFilter(Ui.color(context, R.color.ink_4));
        clearBtn.setClickable(true);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.setText("");
            }
        });
        row.addView(clearBtn);

        return row;
    }

    private void renderFilters(Context context, int totalCount, int reportCount, int followupCount) {
        this.filterContainer.removeAllViews();

        // Row 1: Kind Chips (All, Reports, Follow-ups)
        LinearLayout kindRow = new LinearLayout(context);
        kindRow.setOrientation(0);
        kindRow.setGravity(16);

        String[] kindLabels = new String[]{
            "All (" + totalCount + ")",
            "📝 Reports (" + reportCount + ")",
            "📞 Follow-ups (" + followupCount + ")"
        };

        for (int i = 0; i < 3; i++) {
            final int index = i;
            TextView chip = new TextView(context);
            chip.setText(kindLabels[i]);
            chip.setGravity(17);
            Ui.size(chip, 11.5f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            boolean sel = (this.kindFilter == index);
            if (sel) {
                chip.setTextColor(Ui.color(context, R.color.paper));
                chip.setBackground(Ui.round(context, Ui.color(context, R.color.primary_700), 10.0f, Ui.color(context, R.color.primary_700), 1.0f));
            } else {
                chip.setTextColor(Ui.color(context, R.color.ink_3));
                chip.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), 10.0f, Ui.color(context, R.color.line), 1.0f));
            }
            Ui.pad(chip, 8, 6, 8, 6);
            chip.setClickable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    kindFilter = index;
                    refreshContent();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            if (i > 0) lp.leftMargin = Ui.dp(context, 6.0f);
            kindRow.addView(chip, lp);
        }
        this.filterContainer.addView(kindRow, mw());

        // Row 2: Branch & Status Dropdowns
        LinearLayout filterRow2 = new LinearLayout(context);
        filterRow2.setOrientation(0);
        filterRow2.setGravity(16);
        LinearLayout.LayoutParams r2Lp = mw();
        r2Lp.topMargin = Ui.dp(context, 6.0f);
        filterRow2.setLayoutParams(r2Lp);

        // Branch Chip
        TextView branchChip = new TextView(context);
        branchChip.setText("🏢 " + branchFilter + " ▾");
        branchChip.setGravity(17);
        Ui.size(branchChip, 11.5f);
        branchChip.setTypeface(Typeface.DEFAULT_BOLD);
        boolean branchActive = !"All Branches".equals(branchFilter);
        branchChip.setTextColor(Ui.color(context, branchActive ? R.color.paper : R.color.ink_2));
        branchChip.setBackground(Ui.round(context, branchActive ? Ui.color(context, R.color.primary_700) : Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(branchChip, 8, 6, 8, 6);
        branchChip.setClickable(true);
        branchChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBranchPicker();
            }
        });
        filterRow2.addView(branchChip, weight1());

        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(context, 6.0f), 1));
        filterRow2.addView(spacer);

        // Status Chip
        TextView statusChip = new TextView(context);
        statusChip.setText("📞 " + statusFilter + " ▾");
        statusChip.setGravity(17);
        Ui.size(statusChip, 11.5f);
        statusChip.setTypeface(Typeface.DEFAULT_BOLD);
        boolean statusActive = !"All Statuses".equals(statusFilter);
        statusChip.setTextColor(Ui.color(context, statusActive ? R.color.paper : R.color.ink_2));
        statusChip.setBackground(Ui.round(context, statusActive ? Ui.color(context, R.color.primary_700) : Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(statusChip, 8, 6, 8, 6);
        statusChip.setClickable(true);
        statusChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStatusPicker();
            }
        });
        filterRow2.addView(statusChip, weight1());

        this.filterContainer.addView(filterRow2);
    }

    private void showBranchPicker() {
        final Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        final List<String> branches = new ArrayList<>();
        branches.add("All Branches");
        Cursor c = db.rawQuery("SELECT DISTINCT branch FROM csp WHERE branch IS NOT NULL AND branch != '' ORDER BY branch ASC", null);
        try {
            while (c.moveToNext()) {
                branches.add(c.getString(0));
            }
        } finally {
            c.close();
        }

        final String[] items = branches.toArray(new String[0]);
        int checked = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(branchFilter)) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
            .setTitle("Filter by Branch")
            .setSingleChoiceItems(items, checked, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    branchFilter = items[which];
                    dialog.dismiss();
                    refreshContent();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showStatusPicker() {
        final Context context = getContext();
        final String[] statuses = new String[]{
            "All Statuses",
            "Reported",
            "Contacted",
            "Promise",
            "No Answer",
            "Switch Off",
            "Technical Issue",
            "Not Reachable"
        };
        int checked = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(statusFilter)) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
            .setTitle("Filter by Calling / Report Status")
            .setSingleChoiceItems(statuses, checked, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    statusFilter = statuses[which];
                    dialog.dismiss();
                    refreshContent();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void refreshContent() {
        final Context context = getContext();
        SQLiteDatabase dbRead = Core.get(context).dbRead();

        // Query raw journal entries for date or all
        List<Journal.Entry> allEntries = Journal.recent(dbRead, 300, null, isAllDates ? null : selectedDate);

        int totalReport = 0;
        int totalFollowup = 0;
        for (Journal.Entry e : allEntries) {
            if (e.kind == 1) totalReport++;
            else if (e.kind == 2) totalFollowup++;
        }

        // Render filter chips
        renderFilters(context, allEntries.size(), totalReport, totalFollowup);

        // Filter entries according to active filters
        String q = searchQuery.toLowerCase();
        List<Journal.Entry> filtered = new ArrayList<>();
        for (Journal.Entry e : allEntries) {
            // Kind filter
            if (kindFilter == 1 && e.kind != 1) continue;
            if (kindFilter == 2 && e.kind != 2) continue;

            // Branch filter
            if (!"All Branches".equals(branchFilter) && !branchFilter.equalsIgnoreCase(e.branch)) {
                continue;
            }

            // Status filter
            if (!"All Statuses".equals(statusFilter)) {
                String act = (e.actionStatus != null ? e.actionStatus : "").toLowerCase();
                String det = (e.detail != null ? e.detail : "").toLowerCase();
                String sf = statusFilter.toLowerCase();
                if (!act.contains(sf) && !det.contains(sf)) {
                    continue;
                }
            }

            // Search query
            if (!q.isEmpty()) {
                String name = (e.name != null ? e.name : "").toLowerCase();
                String code = (e.code != null ? e.code : "").toLowerCase();
                String det = (e.detail != null ? e.detail : "").toLowerCase();
                String br = (e.branch != null ? e.branch : "").toLowerCase();
                if (!name.contains(q) && !code.contains(q) && !det.contains(q) && !br.contains(q)) {
                    continue;
                }
            }

            filtered.add(e);
        }

        this.list.removeAllViews();
        if (filtered.isEmpty()) {
            this.statsBanner.setVisibility(8);
            this.empty.show(R.drawable.ic_history, R.color.line_2, "No matching activities", "No reports or follow-ups match your current date, branch or search criteria.");
            Ui.gone(this.list, true);
            return;
        }

        Ui.gone(this.list, false);
        this.empty.hide();
        this.statsBanner.setVisibility(0);
        this.statsBanner.setText("Showing " + filtered.size() + " " + (filtered.size() == 1 ? "entry" : "entries") + (isAllDates ? " across all dates" : " for " + Biz.friendlyDate(selectedDate)));

        for (final Journal.Entry entry : filtered) {
            View rowView = Timeline.row(context, entry, false);
            rowView.setClickable(true);
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityScreen.this.host.push(new CspProfileScreen(context, ActivityScreen.this.host, entry.cspId, entry.date));
                }
            });
            this.list.addView(rowView, mw());
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
}
