package org.cspdaily.report.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.screen.BusinessDayScreen;
import org.cspdaily.report.ui.screen.CspEditScreen;
import org.cspdaily.report.ui.screen.CspListScreen;
import org.cspdaily.report.ui.screen.OfficeNoteScreen;

/* loaded from: classes.dex */
public final class QuickActions {
    public static final int PICK_FOLLOWUP = 2;
    public static final int PICK_NONE = 0;
    public static final int PICK_REPORT = 1;

    private QuickActions() {
    }

    public static void open(Activity activity, Screen.Host host) {
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(1);
        Ui.pad(linearLayout, 16, 10, 16, 18);
        View view = new View(activity);
        view.setBackground(Ui.pill(activity, Ui.color(activity, R.color.line_2)));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(activity, 38.0f), Ui.dp(activity, 4.0f));
        layoutParams.gravity = 1;
        layoutParams.bottomMargin = Ui.dp(activity, 12.0f);
        linearLayout.addView(view, layoutParams);
        TextView textView = new TextView(activity);
        textView.setText("Quick actions");
        textView.setTextAppearance(activity, R.style.Text_ScreenTitle);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(activity);
        textView2.setText(Biz.friendlyDate(Biz.today()) + " · " + Biz.dayName(Biz.today()));
        textView2.setTextAppearance(activity, R.style.Text_Meta);
        Ui.pad(textView2, 0, 2, 0, 10);
        linearLayout.addView(textView2);
        Dialog sheet = ((MainActivity) activity).sheet(linearLayout);
        add(activity, linearLayout, R.drawable.ic_note, R.color.primary_600, "Enter report", "File today's figures for a CSP", go(sheet, host, new CspListScreen(activity, host, 1)));
        add(activity, linearLayout, R.drawable.ic_phone, R.color.warn, "Log follow-up", "Record a call, message or pending reason", go(sheet, host, new CspListScreen(activity, host, 2)));
        add(activity, linearLayout, R.drawable.ic_bank, R.color.info, "Add office note", "Note for today's office text", go(sheet, host, new OfficeNoteScreen(activity, host, Biz.today())));
        add(activity, linearLayout, R.drawable.ic_nil, R.color.ink_3, "Business day", "Mark a holiday, Sunday or special closure", go(sheet, host, new BusinessDayScreen(activity, host, Biz.today())));
        sheet.show();
    }

    private static Runnable go(final Dialog dialog, final Screen.Host host, final Screen screen) {
        return new Runnable() { // from class: org.cspdaily.report.ui.QuickActions.1
            @Override // java.lang.Runnable
            public void run() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                host.push(screen);
            }
        };
    }

    private static void add(Context context, LinearLayout linearLayout, int i, int i2, String str, String str2, final Runnable runnable) {
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setBackground(Ui.pressable(context, Ui.color(context, R.color.fill), Ui.rdp(context, R.dimen.r_md), false));
        Ui.pad(linearLayout2, 14, 13, 14, 13);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        if (linearLayout.getChildCount() > 3) {
            layoutParams.topMargin = Ui.dp(context, 8.0f);
        }
        linearLayout2.setLayoutParams(layoutParams);
        ImageView imageView = new ImageView(context);
        int dp = Ui.dp(context, 22.0f);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp, dp);
        layoutParams2.rightMargin = Ui.dp(context, 14.0f);
        imageView.setLayoutParams(layoutParams2);
        imageView.setImageResource(i);
        imageView.setColorFilter(Ui.color(context, i2));
        linearLayout2.addView(imageView);
        LinearLayout linearLayout3 = new LinearLayout(context);
        linearLayout3.setOrientation(1);
        linearLayout2.addView(linearLayout3, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textView = new TextView(context);
        textView.setText(str);
        Ui.size(textView, 15.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, R.color.ink));
        linearLayout3.addView(textView);
        TextView textView2 = new TextView(context);
        textView2.setText(str2);
        textView2.setTextAppearance(context, R.style.Text_Meta);
        linearLayout3.addView(textView2);
        linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.QuickActions.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                runnable.run();
            }
        });
        linearLayout.addView(linearLayout2);
    }
}
