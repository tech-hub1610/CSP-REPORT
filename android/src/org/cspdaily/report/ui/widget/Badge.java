package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Ui;

/* loaded from: classes.dex */
public class Badge extends TextView {
    public static final int BRAND = 5;
    public static final int DANGER = 2;
    public static final int INFO = 4;
    public static final int NEUTRAL = 0;
    public static final int OK = 1;
    public static final int WARN = 3;
    public static final int WHATSAPP = 6;
    private int kind;

    public Badge(Context context) {
        this(context, null);
    }

    public Badge(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.kind = 0;
        setTextAppearance(context, R.style.Text_Badge);
        Ui.pad(this, 8, 3, 8, 3);
        paint();
    }

    public void setKind(int i) {
        if (this.kind != i) {
            this.kind = i;
            paint();
        }
    }

    public int getKind() {
        return this.kind;
    }

    private void paint() {
        int i;
        int i2 = R.color.ok_border;
        int i3 = R.color.ok_bg;
        getContext();
        switch (this.kind) {
            case 1:
                i = R.color.ok;
                break;
            case 2:
                i3 = R.color.danger_bg;
                i = R.color.danger;
                i2 = R.color.danger_border;
                break;
            case 3:
                i3 = R.color.warn_bg;
                i = R.color.warn;
                i2 = R.color.warn_border;
                break;
            case INFO /* 4 */:
                i3 = R.color.info_bg;
                i = R.color.info;
                i2 = R.color.info_border;
                break;
            case BRAND /* 5 */:
                i3 = R.color.primary_50;
                i = R.color.primary_700;
                i2 = R.color.primary_200;
                break;
            case WHATSAPP /* 6 */:
                i = R.color.whatsapp_dark;
                break;
            default:
                i3 = R.color.fill;
                i = R.color.ink_3;
                i2 = R.color.line;
                break;
        }
        Ui.statusPill(this, i3, i, i2);
    }
}
