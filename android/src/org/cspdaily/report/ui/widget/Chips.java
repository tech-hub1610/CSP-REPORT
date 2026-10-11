package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Ui;

/* loaded from: classes.dex */
public class Chips extends HorizontalScrollView {
    private boolean allowNone;
    private OnPick onPick;
    private final LinearLayout row;
    private int selected;
    private final List<String> values;

    /* loaded from: classes.dex */
    public interface OnPick {
        void pick(int i, String str);
    }

    public Chips(Context context) {
        super(context);
        this.values = new ArrayList();
        this.selected = -1;
        setHorizontalScrollBarEnabled(false);
        setClipToPadding(false);
        this.row = new LinearLayout(context);
        this.row.setOrientation(0);
        this.row.setGravity(16);
        addView(this.row, new FrameLayout.LayoutParams(-2, -2));
    }

    public Chips onChange(OnPick onPick) {
        this.onPick = onPick;
        return this;
    }

    public Chips allowNone(boolean z) {
        this.allowNone = z;
        return this;
    }

    public Chips set(String... strArr) {
        return set(Arrays.asList(strArr));
    }

    public Chips set(List<String> list) {
        this.row.removeAllViews();
        this.values.clear();
        this.values.addAll(list);
        Context context = getContext();
        int i = 0;
        while (true) {
            final int i2 = i;
            if (i2 < this.values.size()) {
                TextView textView = new TextView(context);
                textView.setText(this.values.get(i2));
                Ui.size(textView, 12.5f);
                textView.setTypeface(Typeface.DEFAULT_BOLD);
                textView.setSingleLine(true);
                textView.setGravity(17);
                Ui.pad(textView, 16, 7, 16, 7);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, Ui.dp(context, 34.0f));
                layoutParams.rightMargin = Ui.dp(context, 8.0f);
                textView.setLayoutParams(layoutParams);
                textView.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.widget.Chips.1
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        if (Chips.this.allowNone && Chips.this.selected == i2) {
                            Chips.this.select(-1);
                            if (Chips.this.onPick != null) {
                                Chips.this.onPick.pick(-1, "");
                                return;
                            }
                            return;
                        }
                        Chips.this.select(i2);
                        if (Chips.this.onPick != null) {
                            Chips.this.onPick.pick(i2, (String) Chips.this.values.get(i2));
                        }
                    }
                });
                this.row.addView(textView);
                i = i2 + 1;
            } else {
                paint();
                return this;
            }
        }
    }

    public void select(int i) {
        this.selected = i;
        paint();
    }

    public int selected() {
        return this.selected;
    }

    public String value() {
        return (this.selected < 0 || this.selected >= this.values.size()) ? "" : this.values.get(this.selected);
    }

    private void paint() {
        Context context = getContext();
        boolean isSuper = org.cspdaily.report.data.Auth.isSuperAdmin(context);
        int i = 0;
        while (i < this.row.getChildCount()) {
            TextView textView = (TextView) this.row.getChildAt(i);
            boolean z = i == this.selected;
            textView.setSelected(z);
            if (isSuper) {
                textView.setTextColor(z ? 0xFFFFFFFF : 0xFF78716C);
                if (z) {
                    textView.setBackground(Ui.pressable(context, 0xFFD97706, 17.0f, true));
                    textView.setElevation(Ui.px(context, R.dimen.el_1));
                } else {
                    textView.setBackground(Ui.round(context, 0xF5FCF8F2, 17.0f, 0x66F59E0B, 1.0f));
                    textView.setElevation(0.0f);
                }
            } else {
                textView.setTextColor(Ui.color(context, z ? R.color.paper : R.color.ink_2));
                if (z) {
                    textView.setBackground(Ui.pressable(context, Ui.color(context, R.color.primary_600), 17.0f, true));
                    textView.setElevation(Ui.px(context, R.dimen.el_1));
                } else {
                    textView.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 17.0f, Ui.color(context, R.color.line), 1.0f));
                    textView.setElevation(0.0f);
                }
            }
            i++;
        }
    }
}
