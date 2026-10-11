package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;

/* loaded from: classes.dex */
public class OfficeNoteScreen extends Screen {
    private final String date;
    private final EditText input;

    public OfficeNoteScreen(Context context, Screen.Host host, String str) {
        super(context, host);
        this.date = str;
        title("Office note");
        subtitle(Biz.friendlyDate(str) + " · " + Biz.fmtDmy(str));
        LinearLayout card = Cards.card(context);
        add(card, 0);
        TextView label = Cards.label(context, "Note for this date", R.style.Text_Label, R.color.ink_2);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(label, mw());
        this.input = new EditText(context);
        this.input.setHint("e.g. Branch semi-final, portal downtime 2-4 PM");
        this.input.setMinLines(4);
        this.input.setGravity(8388659);
        Ui.size(this.input, 15.0f);
        this.input.setTextColor(Ui.color(context, R.color.ink));
        this.input.setHorizontallyScrolling(false);
        this.input.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(this.input, 12, 12, 12, 12);
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 8.0f);
        card.addView(this.input, mw);
        card.addView(Cards.label(context, "Appended to the office text sent to the branch group.", R.style.Text_Meta, R.color.ink_4), mw());
        add(Cards.primaryButton(context, "Save note", new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.OfficeNoteScreen.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OfficeNoteScreen.this.save();
            }
        }), 14);
        add(Cards.link(context, "Clear note for this date", R.color.danger, new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.OfficeNoteScreen.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OfficeNoteScreen.this.input.setText("");
                OfficeNoteScreen.this.save();
            }
        }), 2);
        this.input.setText(Day.officeNote(Core.get(context).dbRead(), str));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void save() {
        SQLiteDatabase db = Core.get(getContext()).db();
        String trim = this.input.getText().toString().trim();
        Day.saveOfficeNote(db, this.date, trim);
        Ui.hideKeyboard((Activity) getContext());
        this.host.toast(trim.isEmpty() ? "Office note cleared" : "Office note saved");
        this.host.pop();
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
}
