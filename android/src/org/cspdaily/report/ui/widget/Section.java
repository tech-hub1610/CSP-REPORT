package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Ui;

/* loaded from: classes.dex */
public class Section extends LinearLayout {
    private TextView action;
    private TextView title;

    public Section(Context context) {
        this(context, null);
    }

    public Section(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setOrientation(0);
        setGravity(16);
        Ui.pad(this, 2, 14, 2, 8);
        this.title = new TextView(context);
        this.title.setTextAppearance(context, R.style.Text_Section);
        addView(this.title, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.action = new TextView(context);
        Ui.size(this.action, 12.0f);
        this.action.setTypeface(Typeface.DEFAULT_BOLD);
        this.action.setTextColor(Ui.color(context, R.color.primary_600));
        this.action.setGravity(16);
        this.action.setPadding(Ui.dp(context, 10.0f), Ui.dp(context, 6.0f), Ui.dp(context, 2.0f), Ui.dp(context, 6.0f));
        this.action.setVisibility(8);
        this.action.setBackground(Ui.pressable(context, Ui.color(context, R.color.transparent), Ui.rdp(context, R.dimen.r_md), false));
        addView(this.action, new LinearLayout.LayoutParams(-2, -2));
    }

    public void set(String str) {
        TextView textView = this.title;
        if (str == null) {
            str = "";
        }
        textView.setText(str);
    }

    public void set(String str, String str2, final Runnable runnable) {
        TextView textView = this.title;
        if (str == null) {
            str = "";
        }
        textView.setText(str);
        if (str2 == null || str2.isEmpty() || runnable == null) {
            this.action.setVisibility(8);
            return;
        }
        this.action.setVisibility(0);
        this.action.setText(str2);
        this.action.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.widget.Section.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                runnable.run();
            }
        });
    }

    public void hideAction() {
        this.action.setVisibility(8);
    }
}
