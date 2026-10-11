package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Journal;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Ui;

/* loaded from: classes.dex */
public final class Timeline {
    private Timeline() {
    }

    public static View row(Context context, Journal.Entry entry, boolean z) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setClickable(true);
        linearLayout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 14.0f, Ui.color(context, R.color.line), 1.0f));
        linearLayout.setElevation(Ui.px(context, R.dimen.el_1));
        Ui.pad(linearLayout, 12, 10, 12, 10);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = Ui.dp(context, 6.0f);
        linearLayout.setLayoutParams(layoutParams);
        int bgRes;
        int iconColorRes;
        int iconRes;

        if (entry.kind == 1) {
            if (entry.detail != null && entry.detail.contains("total 0")) {
                bgRes = R.color.info_bg;
                iconColorRes = R.color.info;
                iconRes = R.drawable.ic_nil;
            } else {
                bgRes = R.color.ok_bg;
                iconColorRes = R.color.ok;
                iconRes = R.drawable.ic_check_circle;
            }
        } else {
            String d = entry.detail == null ? "" : entry.detail.toLowerCase();
            if (d.contains("call") || d.contains("phone")) {
                bgRes = R.color.primary_50;
                iconColorRes = R.color.primary_600;
                iconRes = R.drawable.ic_phone;
            } else if (d.contains("not contacted") || d.contains("not available") || d.contains("down") || d.contains("stale")) {
                bgRes = R.color.warn_bg;
                iconColorRes = R.color.warn;
                iconRes = R.drawable.ic_alert;
            } else {
                bgRes = R.color.primary_100;
                iconColorRes = R.color.primary_700;
                iconRes = R.drawable.ic_note;
            }
        }

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackground(Ui.pill(context, Ui.color(context, bgRes)));
        int dp = Ui.dp(context, 36.0f);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp, dp);
        layoutParams2.rightMargin = Ui.dp(context, 12.0f);
        frameLayout.setLayoutParams(layoutParams2);
        linearLayout.addView(frameLayout);

        ImageView imageView = new ImageView(context);
        int dp2 = Ui.dp(context, 18.0f);
        FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(dp2, dp2);
        layoutParams3.gravity = 17;
        imageView.setLayoutParams(layoutParams3);
        imageView.setImageResource(iconRes);
        imageView.setColorFilter(Ui.color(context, iconColorRes));
        frameLayout.addView(imageView);
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(1);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textView = new TextView(context);
        textView.setText(entry.name);
        textView.setTextAppearance(context, R.style.Text_CardTitle);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textView, matchWrap());
        TextView textView2 = new TextView(context);
        textView2.setText("#" + suffix(entry.code) + " · " + entry.detail);
        textView2.setTextAppearance(context, R.style.Text_Meta);
        textView2.setMaxLines(2);
        textView2.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textView2, matchWrap());
        TextView textView3 = new TextView(context);
        textView3.setText(entry.when());
        Ui.size(textView3, 11.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setTextColor(Ui.color(context, R.color.ink_4));
        textView3.setGravity(8388613);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams4.leftMargin = Ui.dp(context, 8.0f);
        linearLayout.addView(textView3, layoutParams4);
        return linearLayout;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static String suffix(String str) {
        return Biz.cspCodeSuffix(str);
    }
}
