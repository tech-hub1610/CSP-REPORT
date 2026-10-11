package org.cspdaily.report.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.ui.Anim3D;
import org.cspdaily.report.ui.Ui;

/**
 * Modern Glassmorphic CSP Card with 3D touch physics, status pill badges, and quick action dock.
 */
public class CspCard extends GlassCardView {
    private final LinearLayout actions;
    private final TextView avatar;
    private final Badge badge;
    private CspDay bound;
    private final TextView figures;
    private final TextView meta;
    private final TextView name;
    private Sink sink;

    public interface Sink {
        void call(CspDay cspDay);
        void followup(CspDay cspDay);
        void open(CspDay cspDay);
        void report(CspDay cspDay);
        void whatsapp(CspDay cspDay);
    }

    public CspCard(Context context) {
        super(context);
        setCornerRadius(18.0f);
        setElevation(Ui.px(context, R.dimen.el_2));
        Ui.pad(this, 14, 14, 14, 14);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(16);
        addView(topRow, new LinearLayout.LayoutParams(-1, -2));

        this.avatar = new TextView(context);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(Ui.dp(context, 44.0f), Ui.dp(context, 44.0f));
        avatarLp.rightMargin = Ui.dp(context, 12.0f);
        this.avatar.setLayoutParams(avatarLp);
        this.avatar.setGravity(17);
        this.avatar.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(this.avatar, 15.0f);
        this.avatar.setTextColor(Ui.color(context, R.color.paper));
        topRow.addView(this.avatar);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(1);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        topRow.addView(titleCol);

        this.name = new TextView(context);
        this.name.setTextAppearance(context, R.style.Text_CardTitle);
        this.name.setSingleLine(true);
        this.name.setEllipsize(TextUtils.TruncateAt.END);
        titleCol.addView(this.name, new LinearLayout.LayoutParams(-1, -2));

        this.meta = new TextView(context);
        this.meta.setTextAppearance(context, R.style.Text_Meta);
        this.meta.setSingleLine(true);
        Ui.pad(this.meta, 0, 2, 0, 0);
        titleCol.addView(this.meta, new LinearLayout.LayoutParams(-1, -2));

        this.badge = new Badge(context);
        topRow.addView(this.badge, new LinearLayout.LayoutParams(-2, -2));

        this.figures = new TextView(context);
        this.figures.setTextAppearance(context, R.style.Text_Meta);
        this.figures.setTextColor(Ui.color(context, R.color.ink_2));
        this.figures.setVisibility(8);
        LinearLayout.LayoutParams figuresLp = new LinearLayout.LayoutParams(-1, -2);
        figuresLp.topMargin = Ui.dp(context, 8.0f);
        addView(this.figures, figuresLp);

        this.actions = new LinearLayout(context);
        this.actions.setOrientation(0);
        this.actions.setGravity(16);
        this.actions.setVisibility(8);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, -2);
        actionsLp.topMargin = Ui.dp(context, 10.0f);
        addView(this.actions, actionsLp);

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CspCard.this.sink != null && CspCard.this.bound != null) {
                    CspCard.this.sink.open(CspCard.this.bound);
                }
            }
        });
    }

    public void setSink(Sink sink) {
        this.sink = sink;
    }

    public CspDay bound() {
        return this.bound;
    }

    public void bind(CspDay cspDay, boolean showActions) {
        int badgeKind;
        String badgeText;
        this.bound = cspDay;
        Context context = getContext();

        this.avatar.setText(Biz.initials(cspDay.name));
        this.avatar.setBackground(Ui.round(context, Ui.avatarColor(context, cspDay.name == null ? cspDay.code : cspDay.name), 22.0f, 0x40FFFFFF, 1.0f));
        this.name.setText(cspDay.name);

        StringBuilder sb = new StringBuilder();
        sb.append("#").append(Biz.cspCodeSuffix(cspDay.code));
        if (!cspDay.branch.isEmpty()) {
            sb.append("  ·  ").append(cspDay.branch);
            if (!cspDay.branchCode.isEmpty()) {
                sb.append(" (").append(cspDay.branchCode).append(")");
            }
        }
        this.meta.setText(sb);

        if (!Csp.ACTIVE.equals(cspDay.status)) {
            badgeText = "Inactive";
            badgeKind = 0;
        } else if (cspDay.reported && !cspDay.isNil) {
            badgeKind = 1;
            badgeText = "Reported";
        } else if (cspDay.isNil) {
            badgeKind = 4;
            badgeText = "Nil";
        } else {
            badgeKind = 2;
            badgeText = "Pending";
        }
        this.badge.setKind(badgeKind);
        this.badge.setText(badgeText);

        StringBuilder sb2 = new StringBuilder();
        if (cspDay.reported) {
            sb2.append("Total ").append(cspDay.totalBusiness);
            if (cspDay.dailyTarget > 0) {
                sb2.append(" / tgt ").append(cspDay.dailyTarget);
            }
            Integer deltaVsYesterday = cspDay.deltaVsYesterday();
            if (deltaVsYesterday != null) {
                sb2.append("  ·  vs yesterday ").append(Ui.delta(deltaVsYesterday.intValue()));
            }
        } else {
            String reasonOrPromise = cspDay.effectiveReasonOrPromise();
            if (!reasonOrPromise.isEmpty()) {
                sb2.append(reasonOrPromise).append("  ·  ");
            }
            sb2.append(cspDay.followupStatus);
            if (cspDay.pendingDays >= 2) {
                sb2.append(" (").append(cspDay.pendingDays).append(" days pending)");
            }
        }

        if (sb2.length() == 0) {
            this.figures.setVisibility(8);
        } else {
            this.figures.setVisibility(0);
            this.figures.setText(sb2);
        }

        if (!showActions || Auth.isAdmin(context)) {
            this.actions.setVisibility(8);
            return;
        }

        this.actions.setVisibility(0);
        this.actions.removeAllViews();

        addAction("Call", R.drawable.ic_phone, R.color.primary_600, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CspCard.this.sink != null && CspCard.this.bound != null) {
                    CspCard.this.sink.call(CspCard.this.bound);
                }
            }
        });
        addAction("WhatsApp", R.drawable.ic_whatsapp, R.color.whatsapp_dark, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CspCard.this.sink != null && CspCard.this.bound != null) {
                    CspCard.this.sink.whatsapp(CspCard.this.bound);
                }
            }
        });
        addAction("Follow up", R.drawable.ic_note, R.color.warn, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CspCard.this.sink != null && CspCard.this.bound != null) {
                    CspCard.this.sink.followup(CspCard.this.bound);
                }
            }
        });
        addAction(cspDay.reported ? "Edit report" : "Enter report", R.drawable.ic_note, R.color.primary_700, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CspCard.this.sink != null && CspCard.this.bound != null) {
                    CspCard.this.sink.report(CspCard.this.bound);
                }
            }
        });
    }

    private void addAction(String label, int iconRes, int colorRes, View.OnClickListener onClickListener) {
        Context context = getContext();
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(0);
        button.setGravity(17);

        int bgRes = (colorRes == R.color.whatsapp_dark) ? R.color.ok_bg : (colorRes == R.color.warn) ? R.color.warn_bg : R.color.primary_50;
        button.setBackground(Ui.round(context, Ui.color(context, bgRes), 12.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(button, 6, 8, 6, 8);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        if (this.actions.getChildCount() > 0) {
            lp.leftMargin = Ui.dp(context, 5.0f);
        }
        button.setLayoutParams(lp);

        ImageView icon = new ImageView(context);
        int dp = Ui.dp(context, 14.0f);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp, dp);
        iconLp.rightMargin = Ui.dp(context, 4.0f);
        icon.setLayoutParams(iconLp);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Ui.color(context, colorRes));
        button.addView(icon);

        TextView text = new TextView(context);
        Ui.size(text, 11.5f);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextColor(Ui.color(context, colorRes));
        text.setSingleLine(true);
        text.setText(label);
        button.addView(text, new LinearLayout.LayoutParams(-2, -2));

        button.setClickable(true);
        Anim3D.attachTactilePress(button);
        button.setOnClickListener(onClickListener);
        this.actions.addView(button);
    }
}
