package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Anim3D;
import org.cspdaily.report.ui.Ui;

/**
 * Modern Glassmorphic KPI metric block with 3D depth, pop animations, and specular border.
 */
public class Kpi extends LinearLayout {
    private TextView caption;
    private TextView delta;
    private TextView value;

    public Kpi(Context context) {
        this(context, null);
    }

    public Kpi(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setOrientation(VERTICAL);
        setGravity(17);
        setBackground(Ui.round(context, 0xFFFFFFFF, 14.0f, Ui.color(context, R.color.line), 1.0f));
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            setElevation(Ui.dp(context, 1.5f));
        }
        Ui.pad(this, 10, 12, 10, 12);
        Anim3D.attachTactilePress(this);

        this.caption = new TextView(context);
        this.caption.setTextAppearance(context, R.style.Text_KpiCaption);
        this.caption.setGravity(17);
        this.caption.setSingleLine(true);
        this.caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
        addView(this.caption, lp(-1));

        this.value = new TextView(context);
        this.value.setTextAppearance(context, R.style.Text_Metric);
        Ui.size(this.value, 21.0f);
        this.value.setTypeface(Typeface.DEFAULT_BOLD);
        this.value.setGravity(17);
        Ui.pad(this.value, 0, 3, 0, 0);
        addView(this.value, lp(-1));

        this.delta = new TextView(context);
        Ui.size(this.delta, 10.5f);
        this.delta.setTypeface(Typeface.DEFAULT_BOLD);
        this.delta.setGravity(17);
        this.delta.setSingleLine(true);
        this.delta.setEllipsize(android.text.TextUtils.TruncateAt.END);
        this.delta.setTextColor(Ui.color(context, R.color.ink_4));
        Ui.pad(this.delta, 0, 3, 0, 0);
        addView(this.delta, lp(-1));
    }

    private LinearLayout.LayoutParams lp(int i) {
        return new LinearLayout.LayoutParams(i, -2);
    }

    public void set(String str, int i) {
        set(str, String.valueOf(i), "", 0);
    }

    public void set(String str, String str2) {
        set(str, str2, "", 0);
    }

    public void set(String str, String str2, String str3, int i) {
        this.caption.setText(str);
        if (!str2.equals(this.value.getText().toString())) {
            this.value.setText(str2);
            Anim3D.pop3D(this.value, 60);
        }
        if (str3 == null || str3.isEmpty()) {
            this.delta.setVisibility(8);
            return;
        }
        this.delta.setVisibility(0);
        this.delta.setText(str3);
        Context context = getContext();
        if (i == 0) {
            i = R.color.ink_4;
        }
        this.delta.setTextColor(Ui.color(context, i));
    }

    public void valueColor(int i) {
        this.value.setTextColor(Ui.color(getContext(), i));
    }
}
