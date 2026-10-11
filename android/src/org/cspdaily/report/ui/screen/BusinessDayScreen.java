package org.cspdaily.report.ui.screen;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.DaySummary;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;

/* loaded from: classes.dex */
public class BusinessDayScreen extends Screen {
    private static final String[] LABELS = {"Working day", "Sunday", "Holiday", "Special closure"};
    private static final String[] TYPES = {DaySummary.WORKING, DaySummary.SUNDAY, DaySummary.HOLIDAY, DaySummary.CLOSURE};
    private final Chips chips;
    private String current;
    private final String date;
    private final EditText desc;

    public BusinessDayScreen(Context context, Screen.Host host, String str) {
        super(context, host);
        this.date = str;
        title("Business day");
        subtitle(Biz.friendlyDate(str) + " · " + Biz.dayName(str));
        SQLiteDatabase dbRead = Core.get(context).dbRead();
        this.current = Day.dayType(dbRead, str)[0];
        String str2 = Day.dayType(dbRead, str)[1];
        LinearLayout card = Cards.card(context);
        add(card, 0);
        card.addView(Cards.label(context, "Day type", R.style.Text_Label, R.color.ink_2), mw());
        this.chips = new Chips(context);
        this.chips.set(LABELS);
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 8.0f);
        this.chips.setLayoutParams(mw);
        card.addView(this.chips);
        this.chips.onChange(new Chips.OnPick() { // from class: org.cspdaily.report.ui.screen.BusinessDayScreen.1
            @Override // org.cspdaily.report.ui.widget.Chips.OnPick
            public void pick(int i, String str3) {
                if (i >= 0) {
                    BusinessDayScreen.this.current = BusinessDayScreen.TYPES[i];
                }
            }
        });
        View view = new View(context);
        view.setLayoutParams(spacing(12));
        card.addView(view);
        card.addView(Cards.label(context, "Description (optional)", R.style.Text_Label, R.color.ink_2), mw());
        this.desc = new EditText(context);
        this.desc.setHint("e.g. Annual closing, branch merge");
        Ui.size(this.desc, 15.0f);
        this.desc.setTextColor(Ui.color(context, R.color.ink));
        this.desc.setSingleLine(true);
        this.desc.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(this.desc, 12, 13, 12, 13);
        LinearLayout.LayoutParams mw2 = mw();
        mw2.topMargin = Ui.dp(context, 8.0f);
        card.addView(this.desc, mw2);
        this.desc.setText(str2);
        card.addView(Cards.label(context, "Pending counts only advance on working days, so marking a holiday stops a CSP looking late.", R.style.Text_Meta, R.color.ink_4), mw());
        add(Cards.primaryButton(context, "Save business day", new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.BusinessDayScreen.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view2) {
                BusinessDayScreen.this.save();
            }
        }), 14);
        selectChipFor(this.current);
    }

    private void selectChipFor(String str) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(str)) {
                this.chips.select(i);
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void save() {
        Day.saveDayType(Core.get(getContext()).db(), this.date, this.current, this.desc.getText().toString().trim());
        this.host.toast("Day marked as " + labelFor(this.current));
        this.host.pop();
    }

    private String labelFor(String str) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(str)) {
                return LABELS[i];
            }
        }
        return str;
    }

    private LinearLayout.LayoutParams spacing(int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.height = Ui.dp(getContext(), i);
        return mw;
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
}
