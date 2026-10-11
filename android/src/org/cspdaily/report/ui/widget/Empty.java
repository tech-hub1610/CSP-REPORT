package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Ui;

/* loaded from: classes.dex */
public class Empty extends LinearLayout {
    private TextView body;
    private TextView cta;
    private Runnable ctaAction;
    private ImageView icon;
    private TextView title;

    public Empty(Context context) {
        this(context, null);
    }

    public Empty(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setOrientation(1);
        setGravity(17);
        Ui.pad(this, 24, 30, 24, 30);
        setVisibility(8);
        this.icon = new ImageView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(context, 44.0f), Ui.dp(context, 44.0f));
        layoutParams.bottomMargin = Ui.dp(context, 10.0f);
        this.icon.setLayoutParams(layoutParams);
        this.icon.setImageResource(R.drawable.ic_check_circle);
        this.icon.setColorFilter(Ui.color(context, R.color.line_2));
        addView(this.icon);
        this.title = new TextView(context);
        this.title.setTextAppearance(context, R.style.Text_CardTitle);
        this.title.setGravity(17);
        addView(this.title);
        this.body = new TextView(context);
        this.body.setTextAppearance(context, R.style.Text_Meta);
        this.body.setGravity(17);
        Ui.pad(this.body, 0, 3, 0, 0);
        addView(this.body);
        this.cta = new TextView(context);
        Ui.size(this.cta, 13.0f);
        this.cta.setTypeface(Typeface.DEFAULT_BOLD);
        this.cta.setTextColor(Ui.color(context, R.color.paper));
        this.cta.setGravity(17);
        Ui.pad(this.cta, 18, 10, 18, 10);
        this.cta.setBackground(Ui.pressable(context, Ui.color(context, R.color.primary_600), 10.0f, true));
        this.cta.setVisibility(8);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = Ui.dp(context, 12.0f);
        this.cta.setLayoutParams(layoutParams2);
        this.cta.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.widget.Empty.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (Empty.this.ctaAction != null) {
                    Empty.this.ctaAction.run();
                }
            }
        });
        addView(this.cta);
    }

    public void show(int i, int i2, String str, String str2) {
        show(i, i2, str, str2, null, null);
    }

    public void show(int i, int i2, String str, String str2, String str3, Runnable runnable) {
        setVisibility(0);
        if (i != 0) {
            this.icon.setImageResource(i);
            ImageView imageView = this.icon;
            Context context = getContext();
            if (i2 == 0) {
                i2 = R.color.line_2;
            }
            imageView.setColorFilter(Ui.color(context, i2));
            this.icon.setVisibility(0);
        } else {
            this.icon.setVisibility(8);
        }
        TextView textView = this.title;
        if (str == null) {
            str = "";
        }
        textView.setText(str);
        if (str2 == null || str2.isEmpty()) {
            this.body.setVisibility(8);
        } else {
            this.body.setVisibility(0);
            this.body.setText(str2);
        }
        if (str3 == null || str3.isEmpty() || runnable == null) {
            this.cta.setVisibility(8);
            return;
        }
        this.cta.setVisibility(0);
        this.cta.setText(str3);
        this.ctaAction = runnable;
    }

    public void hide() {
        setVisibility(8);
    }
}
