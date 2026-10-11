package org.cspdaily.report.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import org.cspdaily.report.R;

/* loaded from: classes.dex */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context context, float f) {
        return (int) TypedValue.applyDimension(1, f, context.getResources().getDisplayMetrics());
    }

    public static float px(Context context, int i) {
        return context.getResources().getDimension(i);
    }

    public static float rdp(Context context, int i) {
        return context.getResources().getDimension(i) / context.getResources().getDisplayMetrics().density;
    }

    public static int sp(Context context, float f) {
        return (int) TypedValue.applyDimension(2, f, context.getResources().getDisplayMetrics());
    }

    public static void size(TextView textView, float f) {
        textView.setTextSize(2, f);
    }

    public static void enterRow(View view, int i) {
        if (view == null) return;
        view.setAlpha(1.0f);
        view.setTranslationY(0.0f);
    }

    public static int color(Context context, int i) {
        return context.getResources().getColor(i);
    }

    public static GradientDrawable round(Context context, int i, float f, int i2, float f2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(0);
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(f <= 0.0f ? 0.0f : dp(context, f));
        if (i2 != 0 && f2 > 0.0f) {
            gradientDrawable.setStroke(dp(context, f2), i2);
        }
        return gradientDrawable;
    }

    public static GradientDrawable round(Context context, int color, float radiusDp) {
        return round(context, color, radiusDp, 0, 0.0f);
    }

    public static GradientDrawable pill(Context context, int i) {
        return round(context, i, 999.0f, 0, 0.0f);
    }

    public static void statusPill(TextView textView, int i, int i2, int i3) {
        Context context = textView.getContext();
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(0);
        gradientDrawable.setColor(color(context, i));
        gradientDrawable.setCornerRadius(dp(context, 999.0f));
        gradientDrawable.setStroke(dp(context, 1.0f), color(context, i3));
        textView.setBackground(gradientDrawable);
        textView.setTextColor(color(context, i2));
    }

    public static int avatarColor(Context context, String str) {
        int i = 0;
        int[] iArr = {-14796150, -14856488, -16612153, -15763858, -12371766, -8635667, -16553567, -15368131};
        if (str == null) {
            str = "";
        }
        int i2 = 0;
        while (true) {
            int i3 = i;
            if (i2 < str.length()) {
                i = ((i3 * 31) + str.charAt(i2)) & Integer.MAX_VALUE;
                i2++;
            } else {
                return iArr[i3 % iArr.length];
            }
        }
    }

    public static Drawable pressable(Context context, int i, float f, boolean z) {
        GradientDrawable round = round(context, i, f, 0, 0.0f);
        return Build.VERSION.SDK_INT >= 21 ? new RippleDrawable(ColorStateList.valueOf(color(context, z ? R.color.ripple_light : R.color.ripple_dark)), round, round) : round;
    }

    public static void pad(View view, int i, int i2, int i3, int i4) {
        view.setPadding(dp(view.getContext(), i), dp(view.getContext(), i2), dp(view.getContext(), i3), dp(view.getContext(), i4));
    }

    public static void padDp(View view, float f) {
        int dp = dp(view.getContext(), f);
        view.setPadding(dp, dp, dp, dp);
    }

    public static void gone(View view, boolean z) {
        view.setVisibility(z ? 8 : 0);
    }

    public static void toast(Context context, String str) {
        Toast makeText = Toast.makeText(context, str, 0);
        makeText.setGravity(81, 0, dp(context, 88.0f));
        makeText.show();
    }

    public static void showKeyboard(View view) {
        view.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, 1);
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager;
        if (activity != null && activity.getCurrentFocus() != null && (inputMethodManager = (InputMethodManager) activity.getSystemService("input_method")) != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public static String n(int i) {
        return Integer.toString(i);
    }

    public static String pct(double d) {
        return Integer.toString((int) Math.round(d)) + "%";
    }

    public static String delta(int i) {
        if (i > 0) {
            return "+" + i;
        }
        if (i < 0) {
            return Integer.toString(i);
        }
        return "no change";
    }

    public static void icon(TextView textView, Drawable drawable, int i, int i2, int i3) {
        if (drawable != null) {
            int dp = dp(textView.getContext(), i);
            drawable.setBounds(0, 0, dp, dp);
            if (i3 != 0) {
                drawable.mutate();
                if (Build.VERSION.SDK_INT >= 21) {
                    drawable.setTint(i3);
                }
            }
            Drawable drawable2 = i2 == 1 ? drawable : null;
            Drawable drawable3 = i2 == 2 ? drawable : null;
            Drawable drawable4 = i2 == 3 ? drawable : null;
            if (i2 != 4) {
                drawable = null;
            }
            textView.setCompoundDrawables(drawable2, drawable3, drawable4, drawable);
        }
    }

    public static LayerDrawable stacked(Drawable... drawableArr) {
        return new LayerDrawable(drawableArr);
    }
}
