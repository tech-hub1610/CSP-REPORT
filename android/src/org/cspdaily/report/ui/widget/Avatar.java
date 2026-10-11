package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Ui;

/* loaded from: classes.dex */
public class Avatar extends TextView {
    public Avatar(Context context) {
        this(context, 40);
    }

    public Avatar(Context context, int i) {
        super(context);
        int dp = Ui.dp(context, i);
        setLayoutParams(new LinearLayout.LayoutParams(dp, dp));
        setGravity(17);
        setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(this, i >= 40 ? 14.0f : 11.0f);
        setTextColor(Ui.color(context, R.color.paper));
        setSingleLine(true);
        setElevation(Ui.px(context, R.dimen.el_1));
    }

    public Avatar bind(String str, String str2) {
        setText(Biz.initials((str == null || str.isEmpty()) ? str2 : str));
        Context context = getContext();
        Context context2 = getContext();
        if (str == null || str.isEmpty()) {
            str = str2;
        }
        setBackground(Ui.pill(context, Ui.avatarColor(context2, str)));
        return this;
    }

    public TextView view(String str, String str2) {
        return bind(str, str2);
    }
}
