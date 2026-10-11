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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.Texts;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.ui.widget.Section;
import org.cspdaily.report.util.Contacts;

public class DailyTargetScreen extends Screen {
    private String date;
    private int activeTab = 0; // 0 = Bulk, 1 = Individual
    private final Chips tabs;
    private final LinearLayout container;
    private final Map<String, EditText> bulkInputs = new LinkedHashMap<>();
    private String searchQuery = "";
    private List<CspDay> cspList = new ArrayList<>();
    private TextView dateLabel;

    public DailyTargetScreen(Context context, Screen.Host host, String initialDate) {
        super(context, host);
        this.date = initialDate != null && !initialDate.isEmpty() ? initialDate : UiState.date(context);
        title("Daily targets");
        subtitle("Set parameter targets for the selected day");

        add(buildDateNav(context), 0);

        this.tabs = new Chips(context);
        this.tabs.set(new String[]{"Bulk Target (All CSPs)", "Individual Target"});
        this.tabs.allowNone(false);
        this.tabs.select(0);
        this.tabs.onChange(new Chips.OnPick() {
            @Override
            public void pick(int index, String label) {
                DailyTargetScreen.this.activeTab = index;
                DailyTargetScreen.this.render();
            }
        });
        add(this.tabs, 8);

        this.container = new LinearLayout(context);
        this.container.setOrientation(1);
        add(this.container, 0);

        refreshContent();
    }

    private View buildDateNav(final Context context) {
        LinearLayout nav = new LinearLayout(context);
        nav.setOrientation(0);
        nav.setGravity(16);
        nav.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(nav, 12, 8, 12, 8);

        TextView prev = Cards.link(context, "◀ Prev", R.color.primary_700, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                date = Biz.plusDays(date, -1);
                updateDateHeader();
                refreshContent();
            }
        });
        nav.addView(prev, wrap());

        this.dateLabel = new TextView(context);
        this.dateLabel.setGravity(17);
        this.dateLabel.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(this.dateLabel, 14.5f);
        this.dateLabel.setTextColor(Ui.color(context, R.color.ink));
        updateDateHeader();
        this.dateLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(context);
            }
        });
        nav.addView(this.dateLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView next = Cards.link(context, "Next ▶", R.color.primary_700, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                date = Biz.plusDays(date, 1);
                updateDateHeader();
                refreshContent();
            }
        });
        nav.addView(next, wrap());

        return nav;
    }

    private void updateDateHeader() {
        if (this.dateLabel != null) {
            String label = Biz.friendlyDate(this.date) + " (" + Biz.fmtDmy(this.date) + ")";
            this.dateLabel.setText(label);
        }
    }

    private void pickDate(final Context context) {
        Calendar c = Biz.cal(this.date);
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                updateDateHeader();
                refreshContent();
            }
        }, y, m, d);
        dialog.show();
    }

    @Override
    public void refreshContent() {
        SQLiteDatabase db = Core.get(getContext()).dbRead();
        this.cspList = Day.dayData(db, this.date, true);
        Collections.sort(this.cspList, new Comparator<CspDay>() {
            @Override
            public int compare(CspDay c1, CspDay c2) {
                String n1 = c1.name != null ? c1.name.toLowerCase(Locale.US) : "";
                String n2 = c2.name != null ? c2.name.toLowerCase(Locale.US) : "";
                int c = n1.compareTo(n2);
                return c != 0 ? c : Long.compare(c1.id, c2.id);
            }
        });
        render();
    }

    private void render() {
        this.container.removeAllViews();
        Context context = getContext();

        if (this.activeTab == 0) {
            renderBulkTab(context);
        } else {
            renderIndividualTab(context);
        }
    }

    private void renderBulkTab(final Context context) {
        final SQLiteDatabase db = Core.get(context).db();
        List<Params.P> params = Params.all(db);

        LinearLayout card = Cards.card(context);
        card.setLayoutParams(top(context, 4));

        TextView head = Cards.label(context, "Bulk Targets for " + Biz.fmtDmy(this.date), R.style.Text_CardTitle, R.color.ink);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(head, mw());

        TextView sub = Cards.label(context, "Enter target numbers for each parameter below. Applying will set these targets for all " + this.cspList.size() + " active CSPs for this day only.", R.style.Text_Meta, R.color.ink_4);
        card.addView(sub, top(context, 4));

        this.bulkInputs.clear();

        for (final Params.P p : params) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(0);
            row.setGravity(16);
            Ui.pad(row, 0, 8, 0, 8);

            LinearLayout labelBox = new LinearLayout(context);
            labelBox.setOrientation(1);
            TextView lbl = Cards.label(context, p.label, R.style.Text_Body, R.color.ink);
            lbl.setTypeface(Typeface.DEFAULT_BOLD);
            labelBox.addView(lbl, mw());
            TextView shortLbl = Cards.label(context, p.shortLabel, R.style.Text_Nano, R.color.ink_4);
            labelBox.addView(shortLbl, mw());
            row.addView(labelBox, new LinearLayout.LayoutParams(0, -2, 1.0f));

            final EditText edit = new EditText(context);
            edit.setInputType(2); // TYPE_CLASS_NUMBER
            edit.setHint("0");
            edit.setText("0");
            edit.setGravity(17);
            Ui.size(edit, 16.0f);
            edit.setTextColor(Ui.color(context, R.color.ink));
            edit.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 8.0f, Ui.color(context, R.color.line_2), 1.0f));
            Ui.pad(edit, 12, 6, 12, 6);
            LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(Ui.dp(context, 75.0f), -2);
            row.addView(edit, editLp);

            this.bulkInputs.put(p.key, edit);
            card.addView(row, mw());
            card.addView(Cards.rule(context, R.color.line_2, 0, 0));
        }

        this.container.addView(card, mw());

        TextView applyBtn = Cards.primaryButton(context, "Apply Target to All " + this.cspList.size() + " CSPs", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBulkTargets(context);
            }
        });
        this.container.addView(applyBtn, top(context, 14));

        TextView clearBtn = Cards.link(context, "Clear all targets for " + Biz.fmtDmy(this.date), R.color.danger, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                    .setTitle("Clear Daily Targets")
                    .setMessage("Are you sure you want to clear all targets for " + Biz.fmtDmy(date) + "?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            SQLiteDatabase dbw = Core.get(context).db();
                            Day.clearDailyTargets(dbw, date, null);
                            host.toast("Targets cleared for " + Biz.fmtDmy(date));
                            refreshContent();
                        }
                    }).show();
            }
        });
        this.container.addView(clearBtn, top(context, 10));
    }

    private void applyBulkTargets(Context context) {
        Map<String, Integer> targets = new LinkedHashMap<>();
        int totalTgt = 0;
        for (Map.Entry<String, EditText> entry : this.bulkInputs.entrySet()) {
            String txt = entry.getValue().getText().toString().trim();
            int val = 0;
            try {
                if (!txt.isEmpty()) {
                    val = Integer.parseInt(txt);
                }
            } catch (Throwable ignored) {}
            targets.put(entry.getKey(), Integer.valueOf(val));
            totalTgt += val;
        }

        List<Long> cspIds = new ArrayList<>();
        for (CspDay c : this.cspList) {
            cspIds.add(Long.valueOf(c.id));
        }

        SQLiteDatabase db = Core.get(context).db();
        Day.setBulkDailyTargets(db, this.date, targets, cspIds);
        this.host.toast("Applied target (" + totalTgt + " total) to " + cspIds.size() + " CSPs");
        refreshContent();
    }

    private void renderIndividualTab(final Context context) {
        final SQLiteDatabase dbRead = Core.get(context).dbRead();
        final List<Params.P> params = Params.all(dbRead);

        // Action bar for sharing all self-targets
        LinearLayout actionCard = Cards.cardFlat(context);
        actionCard.setOrientation(0);
        actionCard.setGravity(16);
        Ui.pad(actionCard, 10, 8, 10, 8);

        ImageView waIcon = new ImageView(context);
        waIcon.setImageResource(R.drawable.ic_whatsapp);
        waIcon.setColorFilter(Ui.color(context, R.color.whatsapp_dark));
        actionCard.addView(waIcon, new LinearLayout.LayoutParams(Ui.dp(context, 20.0f), Ui.dp(context, 20.0f)));

        TextView actionTitle = Cards.label(context, " Self-Targets WhatsApp", R.style.Text_Body, R.color.ink);
        actionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        actionCard.addView(actionTitle, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView copyAllBtn = Cards.link(context, "Copy", R.color.primary_700, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullText = Texts.selfTargetList(cspList, params, date);
                copyToClipboard(context, fullText);
            }
        });
        Ui.pad(copyAllBtn, 8, 4, 8, 4);
        actionCard.addView(copyAllBtn, wrap());

        TextView shareAllBtn = Cards.tonalButton(context, "Share All", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = 0;
                for (CspDay c : cspList) {
                    if (c.dailyTarget > 0) count++;
                }
                if (count == 0) {
                    host.toast("No CSP targets set for " + Biz.fmtDmy(date));
                    return;
                }
                String fullText = Texts.selfTargetList(cspList, params, date);
                Contacts.shareText(context, "Self-Target by CSPs - " + Biz.fmtDmy(date), fullText);
            }
        });
        Ui.pad(shareAllBtn, 10, 4, 10, 4);
        actionCard.addView(shareAllBtn, wrap());

        this.container.addView(actionCard, top(context, 4));

        // Search bar
        LinearLayout searchLayout = new LinearLayout(context);
        searchLayout.setOrientation(0);
        searchLayout.setGravity(16);
        searchLayout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(searchLayout, 10, 2, 10, 2);

        ImageView searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(Ui.color(context, R.color.ink_4));
        searchLayout.addView(searchIcon, new LinearLayout.LayoutParams(Ui.dp(context, 18.0f), Ui.dp(context, 18.0f)));

        final EditText searchEdit = new EditText(context);
        searchEdit.setHint("Search CSP name, code, branch...");
        searchEdit.setText(this.searchQuery);
        searchEdit.setTextColor(Ui.color(context, R.color.ink));
        searchEdit.setHintTextColor(Ui.color(context, R.color.ink_4));
        searchEdit.setBackground(null);
        Ui.size(searchEdit, 14.5f);
        Ui.pad(searchEdit, 8, 8, 8, 8);
        searchLayout.addView(searchEdit, new LinearLayout.LayoutParams(0, -2, 1.0f));

        final ImageView clearBtn = new ImageView(context);
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setColorFilter(Ui.color(context, R.color.ink_4));
        clearBtn.setVisibility(this.searchQuery.isEmpty() ? 8 : 0);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEdit.setText("");
                searchQuery = "";
                clearBtn.setVisibility(8);
                renderIndividualList(context);
            }
        });
        searchLayout.addView(clearBtn, new LinearLayout.LayoutParams(Ui.dp(context, 20.0f), Ui.dp(context, 20.0f)));

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim().toLowerCase(Locale.US);
                clearBtn.setVisibility(searchQuery.isEmpty() ? 8 : 0);
                renderIndividualList(context);
            }
        });

        this.container.addView(searchLayout, top(context, 6));

        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(1);
        listContainer.setTag("individual_list");
        this.container.addView(listContainer, top(context, 8));

        renderIndividualList(context);
    }

    private void renderIndividualList(final Context context) {
        LinearLayout listContainer = (LinearLayout) this.container.findViewWithTag("individual_list");
        if (listContainer == null) return;
        listContainer.removeAllViews();

        final SQLiteDatabase dbRead = Core.get(context).dbRead();
        final List<Params.P> params = Params.all(dbRead);

        List<CspDay> filtered = new ArrayList<>();
        int targetSetCount = 0;
        int totalTargetSum = 0;

        for (CspDay c : this.cspList) {
            if (c.dailyTarget > 0) {
                targetSetCount++;
                totalTargetSum += c.dailyTarget;
            }
            if (this.searchQuery.isEmpty() || (c.name + " " + c.code + " " + c.branch + " " + c.branchCode).toLowerCase(Locale.US).contains(this.searchQuery)) {
                filtered.add(c);
            }
        }

        TextView countTv = Cards.label(context, filtered.size() + " CSPs · " + targetSetCount + " with target set (" + totalTargetSum + " total targets)", R.style.Text_Meta, R.color.ink_3);
        Ui.pad(countTv, 2, 4, 2, 6);
        listContainer.addView(countTv, mw());

        if (filtered.isEmpty()) {
            Empty empty = new Empty(context);
            empty.show(R.drawable.ic_search, R.color.ink_4, "No CSP found", "Try a different search keyword.");
            listContainer.addView(empty, mw());
            return;
        }

        for (int i = 0; i < filtered.size(); i++) {
            final CspDay csp = filtered.get(i);
            LinearLayout row = Cards.cardFlat(context);
            row.setOrientation(0);
            row.setGravity(16);
            Ui.pad(row, 12, 10, 12, 10);
            row.setLayoutParams(top(context, i == 0 ? 0 : 6));

            LinearLayout info = new LinearLayout(context);
            info.setOrientation(1);
            TextView nameTv = Cards.label(context, csp.name, R.style.Text_Body, R.color.ink);
            nameTv.setTypeface(Typeface.DEFAULT_BOLD);
            info.addView(nameTv, mw());

            TextView subTv = Cards.label(context, Biz.cspCodeSuffix(csp.code) + " · " + csp.branch, R.style.Text_Nano, R.color.ink_4);
            info.addView(subTv, mw());

            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1.0f));

            if (csp.dailyTarget > 0) {
                ImageView waRowBtn = new ImageView(context);
                waRowBtn.setImageResource(R.drawable.ic_whatsapp);
                waRowBtn.setColorFilter(Ui.color(context, R.color.whatsapp_dark));
                waRowBtn.setBackground(Ui.round(context, Ui.color(context, R.color.ok_bg), 16.0f, 0, 0.0f));
                Ui.pad(waRowBtn, 6, 6, 6, 6);
                waRowBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String msg = Texts.selfTargetText(csp, params, date);
                        shareOrSendWhatsApp(context, csp, msg);
                    }
                });
                LinearLayout.LayoutParams waLp = new LinearLayout.LayoutParams(Ui.dp(context, 32.0f), Ui.dp(context, 32.0f));
                waLp.rightMargin = Ui.dp(context, 8.0f);
                row.addView(waRowBtn, waLp);
            }

            Badge targetBadge = new Badge(context);
            if (csp.dailyTarget > 0) {
                targetBadge.setKind(Badge.BRAND);
                targetBadge.setText("Target: " + csp.dailyTarget);
            } else {
                targetBadge.setKind(Badge.NEUTRAL);
                targetBadge.setText("No target (0)");
            }
            row.addView(targetBadge, wrap());

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showIndividualEditDialog(context, csp);
                }
            });

            listContainer.addView(row);
        }
    }

    private void showIndividualEditDialog(final Context context, final CspDay csp) {
        final SQLiteDatabase db = Core.get(context).db();
        final List<Params.P> params = Params.all(db);

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout dlgRoot = new LinearLayout(context);
        dlgRoot.setOrientation(1);
        Ui.pad(dlgRoot, 18, 14, 18, 14);

        TextView titleTv = Cards.label(context, csp.name, R.style.Text_CardTitle, R.color.ink);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        dlgRoot.addView(titleTv, mw());

        TextView subTv = Cards.label(context, "Targets for " + Biz.friendlyDate(this.date) + " (" + Biz.fmtDmy(this.date) + ") · ID " + Biz.cspCodeSuffix(csp.code), R.style.Text_Meta, R.color.ink_3);
        dlgRoot.addView(subTv, top(context, 2));

        dlgRoot.addView(Cards.rule(context, R.color.line_2, 0, 0), top(context, 8));

        final Map<String, EditText> editMap = new LinkedHashMap<>();

        for (Params.P p : params) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(0);
            row.setGravity(16);
            Ui.pad(row, 0, 6, 0, 6);

            TextView pLabel = Cards.label(context, p.label, R.style.Text_Body, R.color.ink);
            pLabel.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(pLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));

            int curVal = (csp.targets != null && csp.targets.containsKey(p.key)) ? csp.targets.get(p.key).intValue() : 0;

            EditText edit = new EditText(context);
            edit.setInputType(2); // TYPE_CLASS_NUMBER
            edit.setHint("0");
            edit.setText(String.valueOf(curVal));
            edit.setGravity(17);
            Ui.size(edit, 15.0f);
            edit.setTextColor(Ui.color(context, R.color.ink));
            edit.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), 6.0f, Ui.color(context, R.color.line_2), 1.0f));
            Ui.pad(edit, 10, 4, 10, 4);

            LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(Ui.dp(context, 70.0f), -2);
            row.addView(edit, editLp);

            editMap.put(p.key, edit);
            dlgRoot.addView(row, mw());
        }

        scroll.addView(dlgRoot);

        final AlertDialog[] dialogHolder = new AlertDialog[1];

        // Save and WhatsApp button
        TextView saveAndWaBtn = Cards.whatsappButton(context, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Integer> saved = saveTargetsFromEditMap(context, csp, editMap);
                String msg = Texts.selfTargetText(csp.code, saved, params, date);
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
                refreshContent();
                shareOrSendWhatsApp(context, csp, msg);
            }
        });
        saveAndWaBtn.setText("Save & Share on WhatsApp");
        dlgRoot.addView(saveAndWaBtn, top(context, 12));

        dialogHolder[0] = new AlertDialog.Builder(context)
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear Targets", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SQLiteDatabase dbw = Core.get(context).db();
                    Day.clearDailyTargets(dbw, date, Long.valueOf(csp.id));
                    host.toast("Cleared targets for " + csp.name);
                    refreshContent();
                }
            })
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Map<String, Integer> saved = saveTargetsFromEditMap(context, csp, editMap);
                    int total = 0;
                    for (Integer val : saved.values()) {
                        if (val != null) total += val.intValue();
                    }
                    host.toast("Target saved: " + total + " total for " + csp.name);
                    refreshContent();
                }
            })
            .show();
    }

    private Map<String, Integer> saveTargetsFromEditMap(Context context, CspDay csp, Map<String, EditText> editMap) {
        SQLiteDatabase dbw = Core.get(context).db();
        Map<String, Integer> saved = new LinkedHashMap<>();
        int total = 0;
        for (Map.Entry<String, EditText> e : editMap.entrySet()) {
            String txt = e.getValue().getText().toString().trim();
            int val = 0;
            try {
                if (!txt.isEmpty()) {
                    val = Integer.parseInt(txt);
                }
            } catch (Throwable ignored) {}
            Day.setDailyTarget(dbw, csp.id, this.date, e.getKey(), val);
            saved.put(e.getKey(), Integer.valueOf(val));
            total += val;
        }
        csp.targets = saved;
        csp.dailyTarget = total;
        return saved;
    }

    private void shareOrSendWhatsApp(final Context context, final CspDay csp, final String text) {
        final String normMobile = (csp != null && csp.mobile != null) ? Contacts.normalize(csp.mobile) : "";
        if (!normMobile.isEmpty()) {
            new AlertDialog.Builder(context)
                .setTitle("Share Self-Target")
                .setMessage("Share self-target text to WhatsApp group/other apps, or send directly to " + csp.name + " (" + csp.mobile + ")?")
                .setPositiveButton("Share / Group", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Contacts.shareText(context, "Self-Target by CSPs", text);
                    }
                })
                .setNeutralButton("Direct to CSP", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Contacts.whatsapp(context, csp.mobile, text);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            Contacts.shareText(context, "Self-Target by CSPs", text);
        }
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Self-Target", text));
            Toast.makeText(context, "Copied self-targets to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private static LinearLayout.LayoutParams top(Context context, int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dp(context, (float) dp);
        return lp;
    }
}
