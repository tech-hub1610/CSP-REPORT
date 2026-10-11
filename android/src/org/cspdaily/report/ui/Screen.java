package org.cspdaily.report.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.cspdaily.report.R;

import org.cspdaily.report.data.Auth;

/**
 * Base Screen class with Glassmorphic app bar, 3D entrance animations, and tactile interactions.
 */
public abstract class Screen extends LinearLayout {
    protected LinearLayout actionSlot;
    public boolean active;
    protected ImageView backView;
    protected ImageView logoView;
    protected LinearLayout body;
    protected Host host;
    protected ScrollView scroll;
    private LinearLayout stickyBar;
    protected TextView subtitleView;
    protected TextView titleView;
    protected LinearLayout headerBar;

    public interface Host {
        void goTab(int i);
        void pop();
        void push(Screen screen);
        void refresh();
        int statusBarPad();
        void toast(String str);
    }

    public Screen(Context context, final Host host) {
        super(context);
        this.host = host;
        setOrientation(VERTICAL);
        boolean isSuperAdmin = Auth.isSuperAdmin(context);
        setBackgroundColor(isSuperAdmin ? 0xFF050710 : Ui.color(context, R.color.bg_app));
        setFitsSystemWindows(false);

        // Modern Glassmorphic App Bar
        this.headerBar = new LinearLayout(context);
        this.headerBar.setOrientation(HORIZONTAL);
        this.headerBar.setGravity(16);
        if (isSuperAdmin) {
            this.headerBar.setBackground(Ui.round(context, 0xF5FCF8F2, 0f, 0x66F59E0B, 1.0f));
        } else {
            this.headerBar.setBackground(Ui.round(context, Ui.color(context, R.color.glass_card_bg), 0f, Ui.color(context, R.color.glass_card_border), 1.0f));
        }
        this.headerBar.setElevation(Ui.px(context, R.dimen.el_2));
        int dpH = Ui.dp(context, 14.0f);
        int dpV = Ui.dp(context, 10.0f);
        this.headerBar.setPadding(dpH, dpV, dpH, dpV);
        addView(this.headerBar, new LinearLayout.LayoutParams(-1, -2));

        this.backView = new ImageView(context);
        int dpBack = Ui.dp(context, 38.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dpBack, dpBack);
        layoutParams.rightMargin = Ui.dp(context, 8.0f);
        this.backView.setLayoutParams(layoutParams);
        this.backView.setPadding(Ui.dp(context, 8.0f), Ui.dp(context, 8.0f), Ui.dp(context, 8.0f), Ui.dp(context, 8.0f));
        this.backView.setImageResource(R.drawable.ic_arrow_back);
        this.backView.setColorFilter(isSuperAdmin ? 0xFFD97706 : Ui.color(context, R.color.ink));
        this.backView.setBackground(Ui.round(context, isSuperAdmin ? 0x26F59E0B : Ui.color(context, R.color.fill), 19.0f, isSuperAdmin ? 0x80F59E0B : Ui.color(context, R.color.line), 1.0f));
        Anim3D.attachTactilePress(this.backView);
        this.backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Screen.this.onBackPressed()) {
                    host.pop();
                }
            }
        });
        this.headerBar.addView(this.backView);

        this.logoView = new ImageView(context);
        int dpLogo = Ui.dp(context, 36.0f);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dpLogo, dpLogo);
        logoParams.rightMargin = Ui.dp(context, 10.0f);
        this.logoView.setLayoutParams(logoParams);
        this.logoView.setImageResource(R.mipmap.ic_launcher);
        this.logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.logoView.setVisibility(8);
        this.headerBar.addView(this.logoView);

        LinearLayout titleContainer = new LinearLayout(context);
        titleContainer.setOrientation(VERTICAL);
        titleContainer.setGravity(16);
        titleContainer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.headerBar.addView(titleContainer);

        this.titleView = new TextView(context);
        this.titleView.setTextAppearance(context, R.style.Text_ScreenTitle);
        titleContainer.addView(this.titleView, new LinearLayout.LayoutParams(-1, -2));

        this.subtitleView = new TextView(context);
        this.subtitleView.setTextAppearance(context, R.style.Text_Meta);
        this.subtitleView.setSingleLine(true);
        this.subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        this.subtitleView.setVisibility(8);
        titleContainer.addView(this.subtitleView, new LinearLayout.LayoutParams(-1, -2));

        this.actionSlot = new LinearLayout(context);
        this.actionSlot.setOrientation(HORIZONTAL);
        this.actionSlot.setGravity(16);
        this.headerBar.addView(this.actionSlot, new LinearLayout.LayoutParams(-2, -1));

        this.scroll = new ScrollView(context);
        this.scroll.setVerticalScrollBarEnabled(false);
        this.scroll.setClipToPadding(false);
        addView(this.scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        this.body = new LinearLayout(context);
        this.body.setOrientation(VERTICAL);
        int dpPad = Ui.dp(context, 16.0f);
        this.body.setPadding(dpPad, dpPad, dpPad, Ui.dp(context, 92.0f));
        this.scroll.addView(this.body, new FrameLayout.LayoutParams(-1, -2));

        title(headline());
        showBack(canGoBack());
    }

    public String headline() {
        return "";
    }

    protected boolean canGoBack() {
        return true;
    }

    public void onShowScreen() {
        this.active = true;
        refreshContent();
    }

    public void refreshContent() {
    }

    protected void setStickyAction(View view) {
        Context context = getContext();
        if (this.stickyBar == null) {
            this.stickyBar = new LinearLayout(context);
            this.stickyBar.setOrientation(HORIZONTAL);
            this.stickyBar.setGravity(16);
            this.stickyBar.setBackground(Ui.round(context, Ui.color(context, R.color.glass_card_bg), 0f, Ui.color(context, R.color.glass_card_border), 1.0f));
            this.stickyBar.setElevation(Ui.px(context, R.dimen.el_4));
            int dp = Ui.dp(context, 14.0f);
            this.stickyBar.setPadding(dp, Ui.dp(context, 10.0f), dp, Ui.dp(context, 10.0f));
            addView(this.stickyBar, new LinearLayout.LayoutParams(-1, -2));
        }
        this.stickyBar.removeAllViews();
        if (view == null) {
            this.stickyBar.setVisibility(8);
        } else {
            this.stickyBar.setVisibility(0);
            this.stickyBar.addView(view, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    protected boolean hasStickyAction() {
        return this.stickyBar != null && this.stickyBar.getVisibility() == 0;
    }

    public void onHideScreen() {
        this.active = false;
    }

    public boolean onBackPressed() {
        return false;
    }

    protected void title(String str) {
        TextView textView = this.titleView;
        if (str == null) {
            str = "";
        }
        textView.setText(str);
        if (Auth.isSuperAdmin(getContext())) {
            textView.setTextColor(0xFF1E1B18);
        }
    }

    protected void subtitle(String str) {
        if (str == null || str.isEmpty()) {
            this.subtitleView.setVisibility(8);
        } else {
            this.subtitleView.setVisibility(0);
            this.subtitleView.setText(str);
            if (Auth.isSuperAdmin(getContext())) {
                this.subtitleView.setTextColor(0xFF78716C);
            }
        }
    }

    protected void showBack(boolean z) {
        this.backView.setVisibility(z ? 0 : 8);
    }

    protected void showLogo(boolean z) {
        if (z) {
            this.backView.setVisibility(8);
            this.logoView.setVisibility(0);
        } else {
            this.logoView.setVisibility(8);
        }
    }

    public void headerAction(String str, final Runnable runnable) {
        this.actionSlot.removeAllViews();
        Context context = getContext();
        boolean isSuper = Auth.isSuperAdmin(context);
        TextView textView = new TextView(context);
        textView.setText(str);
        Ui.size(textView, 13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        if (isSuper) {
            textView.setTextColor(0xFFFFFFFF);
            textView.setBackground(Ui.round(context, 0xFFD97706, 16.0f, 0xFFF59E0B, 1.0f));
        } else {
            textView.setTextColor(Ui.color(context, R.color.primary_600));
            textView.setBackground(Ui.round(context, Ui.color(context, R.color.primary_50), 16.0f, Ui.color(context, R.color.primary_200), 1.0f));
        }
        Ui.pad(textView, 12, 6, 12, 6);
        textView.setGravity(17);
        Anim3D.attachTactilePress(textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        this.actionSlot.addView(textView);
    }

    public void headerIcon(int i, int i2, final Runnable runnable) {
        this.actionSlot.removeAllViews();
        Context context = getContext();
        boolean isSuper = Auth.isSuperAdmin(context);
        ImageView imageView = new ImageView(context);
        int dp = Ui.dp(context, 36.0f);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(dp, dp));
        imageView.setImageResource(i);
        if (isSuper) {
            imageView.setColorFilter(0xFFF59E0B);
            imageView.setBackground(Ui.round(context, 0x26F59E0B, 18.0f, 0x66F59E0B, 1.0f));
        } else {
            if (i2 == 0) {
                i2 = R.color.ink_2;
            }
            imageView.setColorFilter(Ui.color(context, i2));
            imageView.setBackground(Ui.round(context, Ui.color(context, R.color.fill), 18.0f, Ui.color(context, R.color.line), 1.0f));
        }
        imageView.setPadding(Ui.dp(context, 8.0f), Ui.dp(context, 8.0f), Ui.dp(context, 8.0f), Ui.dp(context, 8.0f));
        Anim3D.attachTactilePress(imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        this.actionSlot.addView(imageView);
    }

    protected View inflate(int i) {
        return LayoutInflater.from(getContext()).inflate(i, (ViewGroup) this.body, false);
    }

    protected void add(View view) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(getContext(), 10.0f);
        this.body.addView(view, layoutParams);
    }

    protected void add(View view, int i) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(getContext(), i);
        this.body.addView(view, layoutParams);
    }

    protected void bodyBottomPad(int i) {
        int dp = Ui.dp(getContext(), 14.0f);
        this.body.setPadding(dp, dp, dp, Ui.dp(getContext(), i));
    }
}
