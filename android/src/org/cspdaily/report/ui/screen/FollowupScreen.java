package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.DaySummary;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.CspCard;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.util.PhoneActions;

/* loaded from: classes.dex */
public class FollowupScreen extends Screen {
    private static final String[] MODES = {"All pending", "Not contacted", "Contacted today", "Long pending", "Nil figure"};
    private static final int[] TILE_OFF = {R.color.hero_pending, R.color.hero_pending, R.color.paper, R.color.hero_pending, R.color.hero_nil};
    private static final int[] TILE_ON = {R.color.danger, R.color.danger, R.color.navy_950, R.color.danger, R.color.info};
    private String date;
    private final Empty empty;
    private final TextView lead;
    private final LinearLayout list;
    private String mode;
    private final Chips modes;
    private String query;
    private List<CspDay> rows;
    private EditText search;
    private final TextView[] tileCounts;
    private final LinearLayout tileRow;
    private final LinearLayout[] tiles;

    public FollowupScreen(Context context, Screen.Host host) {
        this(context, host, UiState.date(context));
    }

    public FollowupScreen(Context context, Screen.Host host, String str) {
        super(context, host);
        this.tiles = new LinearLayout[MODES.length];
        this.tileCounts = new TextView[MODES.length];
        this.mode = MODES[0];
        this.query = "";
        this.rows = new ArrayList();
        if (str != null) {
            UiState.setDate(context, str);
        }
        title("Follow-up");
        this.lead = Cards.bold(context, "", 15.0f, R.color.paper);
        this.lead.setSingleLine(true);
        this.lead.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout hero = Cards.hero(context);
        add(hero, 0);
        hero.addView(this.lead, mw());
        this.tileRow = new LinearLayout(context);
        this.tileRow.setOrientation(0);
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 10.0f);
        this.tileRow.setLayoutParams(mw);
        for (int i = 0; i < MODES.length; i++) {
            this.tileRow.addView(tile(context, i));
        }
        hero.addView(this.tileRow);
        this.modes = new Chips(context);
        this.modes.set(MODES);
        this.modes.allowNone(false);
        this.modes.select(0);
        this.modes.onChange(new Chips.OnPick() { // from class: org.cspdaily.report.ui.screen.FollowupScreen.1
            @Override // org.cspdaily.report.ui.widget.Chips.OnPick
            public void pick(int i2, String str2) {
                FollowupScreen.this.setMode(i2);
            }
        });
        add(this.modes, 12);
        add(searchBar(context), 8);
        this.list = new LinearLayout(context);
        this.list.setOrientation(1);
        add(this.list, 8);
        this.empty = new Empty(context);
        add(this.empty, 0);
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return "Follow-up";
    }

    @Override // org.cspdaily.report.ui.Screen
    public void refreshContent() {
        String str;
        this.date = UiState.date(getContext());
        SQLiteDatabase dbRead = Core.get(getContext()).dbRead();
        DaySummary summary = Day.summary(dbRead, this.date);
        this.rows = Day.dayData(dbRead, this.date);
        subtitle(Biz.friendlyDate(this.date) + " · " + summary.pending + " of " + summary.total + " pending");
        getContext();
        TextView textView = this.lead;
        if (summary.pending == 0) {
            str = "Nothing requires follow-up";
        } else {
            str = summary.pending + " CSP" + (summary.pending == 1 ? "" : "s") + " require attention";
        }
        textView.setText(str);
        paintTiles();
        render();
    }

    private View searchBar(Context context) {
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
        this.search.setHint("Search name, code, mobile or branch");
        this.search.setBackground(null);
        Ui.size(this.search, 14.5f);
        this.search.setTextColor(Ui.color(context, R.color.ink));
        this.search.setHintTextColor(Ui.color(context, R.color.ink_4));
        this.search.setSingleLine(true);
        this.search.setImeOptions(3);
        linearLayout.addView(this.search, new LinearLayout.LayoutParams(0, Ui.dp(context, 46.0f), 1.0f));

        final ImageView clearBtn = new ImageView(context);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(Ui.dp(context, 32.0f), Ui.dp(context, 32.0f));
        clearLp.rightMargin = Ui.dp(context, 4.0f);
        clearBtn.setLayoutParams(clearLp);
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setColorFilter(Ui.color(context, R.color.ink_4));
        clearBtn.setBackground(Ui.round(context, 0x15000000, 16.0f, 0, 0.0f));
        Ui.pad(clearBtn, 7, 7, 7, 7);
        clearBtn.setVisibility(this.query != null && !this.query.isEmpty() ? 0 : 8);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FollowupScreen.this.search.setText("");
            }
        });
        linearLayout.addView(clearBtn);

        this.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                FollowupScreen.this.query = editable.toString().trim().toLowerCase(Locale.US);
                clearBtn.setVisibility(FollowupScreen.this.query.isEmpty() ? 8 : 0);
                FollowupScreen.this.render();
            }
        });
        return linearLayout;
    }

    private boolean matches(CspDay cspDay) {
        if (cspDay == null) return false;
        String full = (cspDay.name + " " + cspDay.code + " " + cspDay.mobile + " " + cspDay.branch + " " + cspDay.branchCode + " " + Biz.cspCodeSuffix(cspDay.code)).toLowerCase(Locale.US);
        return full.contains(this.query);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMode(int i) {
        if (i >= 0) {
            this.modes.select(i);
            this.mode = MODES[i];
            paintTiles();
            render();
        }
    }

    private boolean inMode(CspDay cspDay, int i) {
        if (i < 0 || !Csp_isActive(cspDay)) {
            return false;
        }
        if (!this.query.isEmpty() && !matches(cspDay)) {
            return false;
        }
        switch (i) {
            case 0:
                return cspDay.isPending();
            case 1:
                return cspDay.isPending() && Day.NOT_CONTACTED.equals(cspDay.followupStatus);
            case 2:
                return cspDay.isPending() && !Day.NOT_CONTACTED.equals(cspDay.followupStatus);
            case 3:
                return cspDay.isPending() && cspDay.pendingDays >= 3;
            default:
                return cspDay.isNil;
        }
    }

    private View tile(Context context, final int i) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(1);
        Ui.pad(linearLayout, 2, 9, 2, 8);
        TextView textView = new TextView(context);
        Ui.size(textView, 17.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        linearLayout.addView(textView, mw());
        TextView textView2 = new TextView(context);
        Ui.size(textView2, 9.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setGravity(17);
        textView2.setText(MODES[i]);
        textView2.setMaxLines(2);
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 2.0f);
        linearLayout.addView(textView2, mw);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        if (i > 0) {
            layoutParams.leftMargin = Ui.dp(context, 6.0f);
        }
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.FollowupScreen.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                FollowupScreen.this.setMode(i);
            }
        });
        this.tiles[i] = linearLayout;
        this.tileCounts[i] = textView;
        return linearLayout;
    }

    private void paintTiles() {
        LinearLayout linearLayout;
        Context context = getContext();
        int selected = this.modes.selected();
        int i = 0;
        while (i < this.tiles.length && (linearLayout = this.tiles[i]) != null) {
            boolean z = i == selected;
            Iterator<CspDay> it = this.rows.iterator();
            int i2 = 0;
            while (it.hasNext()) {
                i2 = inMode(it.next(), i) ? i2 + 1 : i2;
            }
            linearLayout.setSelected(z);
            linearLayout.setBackground(Ui.round(context, Ui.color(context, z ? R.color.paper : R.color.hero_divider), 12.0f, 0, 0.0f));
            this.tileCounts[i].setText(Integer.toString(i2));
            this.tileCounts[i].setTextColor(Ui.color(context, z ? TILE_ON[i] : TILE_OFF[i]));
            ((TextView) linearLayout.getChildAt(1)).setTextColor(Ui.color(context, z ? R.color.primary_700 : R.color.hero_text_dim));
            i++;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void render() {
        Context context = getContext();
        ArrayList arrayList = new ArrayList();
        for (CspDay cspDay : this.rows) {
            if (inMode(cspDay, this.modes.selected())) {
                arrayList.add(cspDay);
            }
        }
        Collections.sort(arrayList, new Comparator<CspDay>() { // from class: org.cspdaily.report.ui.screen.FollowupScreen.4
            @Override // java.util.Comparator
            public int compare(CspDay cspDay2, CspDay cspDay3) {
                if (cspDay3.pendingDays != cspDay2.pendingDays) {
                    return cspDay3.pendingDays - cspDay2.pendingDays;
                }
                String n1 = cspDay2.name == null ? "" : cspDay2.name.toLowerCase(Locale.US);
                String n2 = cspDay3.name == null ? "" : cspDay3.name.toLowerCase(Locale.US);
                int c = n1.compareTo(n2);
                return c != 0 ? c : Long.compare(cspDay2.id, cspDay3.id);
            }
        });
        this.list.removeAllViews();
        if (arrayList.isEmpty()) {
            this.empty.show(R.drawable.ic_check_circle, R.color.ok, "Nothing here", this.query.isEmpty() ? "No CSP matches “" + this.mode + "”." : "Nothing in “" + this.mode + "” matches “" + this.query + "”.");
            return;
        }
        this.empty.hide();
        final String str = this.date;
        CspCard.Sink sink = new CspCard.Sink() {
            @Override
            public void open(CspDay cspDay2) {
                FollowupScreen.this.host.push(new CspProfileScreen(FollowupScreen.this.getContext(), FollowupScreen.this.host, cspDay2.id, str));
            }

            @Override
            public void call(CspDay cspDay2) {
                PhoneActions.call(FollowupScreen.this.getContext(), cspDay2);
            }

            @Override
            public void whatsapp(CspDay cspDay2) {
                PhoneActions.whatsapp(FollowupScreen.this.getContext(), cspDay2);
            }

            @Override
            public void followup(CspDay cspDay2) {
                FollowupSheet.open((Activity) FollowupScreen.this.getContext(), FollowupScreen.this.host, cspDay2, str);
            }

            @Override
            public void report(CspDay cspDay2) {
                ReportEntrySheet.open((Activity) FollowupScreen.this.getContext(), FollowupScreen.this.host, cspDay2, str);
            }
        };
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < arrayList.size()) {
                CspCard cspCard = new CspCard(context);
                cspCard.setSink(sink);
                cspCard.bind((CspDay) arrayList.get(i2), true);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
                layoutParams.topMargin = Ui.dp(context, i2 == 0 ? 0.0f : 8.0f);
                this.list.addView(cspCard, layoutParams);
                Ui.enterRow(cspCard, i2);
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private boolean Csp_isActive(CspDay cspDay) {
        return Csp.ACTIVE.equals(cspDay.status);
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
}
