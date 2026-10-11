package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Anim3D;
import org.cspdaily.report.ui.Ui;

/**
 * Modern UI component factory for Glassmorphic cards, hero banners, 3D buttons, and labels.
 */
public final class Cards {
    private Cards() {
    }

    public static LinearLayout card(Context context) {
        GlassCardView glass = new GlassCardView(context);
        glass.setCornerRadius(18.0f);
        glass.setElevation(Ui.px(context, R.dimen.el_2));
        Ui.pad(glass, 16, 16, 16, 16);
        return glass;
    }

    public static LinearLayout cardFlat(Context context) {
        GlassCardView glass = new GlassCardView(context);
        glass.setCornerRadius(18.0f);
        glass.setElevation(Ui.px(context, R.dimen.el_2));
        return glass;
    }

    public static LinearLayout hero(Context context) {
        GlassCardView glass = new GlassCardView(context);
        glass.setHeroGlass(true);
        glass.setCornerRadius(22.0f);
        glass.setElevation(Ui.px(context, R.dimen.el_4));
        Ui.pad(glass, 18, 18, 18, 18);
        return glass;
    }

    public static LinearLayout darkGlass(Context context) {
        GlassCardView glass = new GlassCardView(context);
        glass.setDarkGlass(true);
        glass.setCornerRadius(20.0f);
        glass.setElevation(Ui.px(context, R.dimen.el_3));
        Ui.pad(glass, 16, 16, 16, 16);
        return glass;
    }

    public static LinearLayout obsidianGold(Context context) {
        GlassCardView glass = new GlassCardView(context);
        glass.setObsidianGold(true);
        glass.setCornerRadius(22.0f);
        glass.setElevation(Ui.px(context, R.dimen.el_4));
        Ui.pad(glass, 16, 16, 16, 16);
        return glass;
    }

    public static LinearLayout obsidianGoldFlat(Context context) {
        GlassCardView glass = new GlassCardView(context);
        glass.setObsidianGold(true);
        glass.setCornerRadius(18.0f);
        glass.setElevation(Ui.px(context, R.dimen.el_2));
        return glass;
    }

    public static TextView label(Context context, String str, int i, int i2) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextAppearance(context, i);
        if (i2 != 0) {
            textView.setTextColor(Ui.color(context, i2));
        }
        return textView;
    }

    public static TextView bold(Context context, String str, float f, int i) {
        TextView textView = new TextView(context);
        textView.setText(str);
        Ui.size(textView, f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, i));
        return textView;
    }

    public static View rule(Context context, int i, int i2, int i3) {
        View view = new View(context);
        view.setBackgroundColor(Ui.color(context, i));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(context, 1.0f));
        layoutParams.topMargin = Ui.dp(context, i2);
        layoutParams.bottomMargin = Ui.dp(context, i3);
        view.setLayoutParams(layoutParams);
        return view;
    }

    public static LinearLayout.LayoutParams weighted(float f) {
        return new LinearLayout.LayoutParams(0, -2, f);
    }

    public static TextView primaryButton(Context context, String str, View.OnClickListener onClickListener) {
        return button(context, str, Ui.color(context, R.color.primary_600), Ui.color(context, R.color.paper), true, onClickListener);
    }

    public static TextView goldButton(Context context, String str, View.OnClickListener onClickListener) {
        return button(context, str, 0xFFD97706, 0xFFFFFFFF, true, onClickListener);
    }

    public static TextView goldOutlineButton(Context context, String str, View.OnClickListener onClickListener) {
        return button(context, str, 0x1AF59E0B, 0xFFD97706, false, onClickListener);
    }

    public static TextView dangerButton(Context context, String str, View.OnClickListener onClickListener) {
        return button(context, str, Ui.color(context, R.color.danger), Ui.color(context, R.color.paper), true, onClickListener);
    }

    public static TextView tonalButton(Context context, String str, View.OnClickListener onClickListener) {
        return button(context, str, Ui.color(context, R.color.primary_50), Ui.color(context, R.color.primary_700), false, onClickListener);
    }

    public static TextView whatsappButton(Context context, View.OnClickListener onClickListener) {
        return button(context, "WhatsApp", Ui.color(context, R.color.ok_bg), Ui.color(context, R.color.whatsapp_dark), false, onClickListener);
    }

    public static TextView outlineButton(Context context, String str, View.OnClickListener onClickListener) {
        return button(context, str, Ui.color(context, R.color.paper), Ui.color(context, R.color.ink_2), false, onClickListener);
    }

    private static TextView button(Context context, String str, int i, int i2, boolean z, View.OnClickListener onClickListener) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setGravity(17);
        Ui.size(textView, 15.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(i2);
        textView.setMinHeight(Ui.dp(context, 48.0f));
        textView.setClickable(true);
        Ui.pad(textView, 18, 12, 18, 12);
        if (z) {
            textView.setBackground(Ui.round(context, i, 14.0f, 0x33FFFFFF, 1.0f));
            textView.setElevation(Ui.px(context, R.dimen.el_2));
        } else {
            textView.setBackground(Ui.round(context, i, 14.0f, Ui.color(context, R.color.line), 1.0f));
        }
        Anim3D.attachTactilePress(textView);
        if (onClickListener != null) {
            textView.setOnClickListener(onClickListener);
        }
        return textView;
    }

    public static TextView link(Context context, String str, int i, View.OnClickListener onClickListener) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setGravity(17);
        Ui.size(textView, 13.5f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, i));
        textView.setMinHeight(Ui.dp(context, 44.0f));
        textView.setClickable(true);
        textView.setBackground(Ui.pressable(context, Ui.color(context, R.color.transparent), 10.0f, false));
        Anim3D.attachTactilePress(textView);
        textView.setOnClickListener(onClickListener);
        return textView;
    }
}
