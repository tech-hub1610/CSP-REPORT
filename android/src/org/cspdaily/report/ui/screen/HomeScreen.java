package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.Journal;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.domain.DaySummary;
import org.cspdaily.report.ui.Anim3D;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.ui.widget.Kpi;
import org.cspdaily.report.ui.widget.Ring;
import org.cspdaily.report.ui.widget.Section;
import org.cspdaily.report.ui.widget.Timeline;
import org.cspdaily.report.util.PhoneActions;

/* loaded from: classes.dex */
public class HomeScreen extends Screen {
    private final Section actSection;
    private final LinearLayout activity;
    private final LinearLayout attention;
    private final Section attnSection;
    private String date;
    private final Empty emptyAttention;
    private final LinearLayout hero;
    private final LinearLayout instructionsContainer;
    private final LinearLayout kpiCard;
    private View progressFill;
    private View progressTrack;
    private int reportedCount;
    private final LinearLayout tilesRow;
    private int totalCount;

    public HomeScreen(Context context, Screen.Host host) {
        super(context, host);
        this.totalCount = 1;
        showBack(false);
        showLogo(true);
        headerAction("Sync Cloud", new Runnable() {
            @Override
            public void run() {
                host.toast("Syncing with Supabase...");
                org.cspdaily.report.data.SupabaseClient.syncAll(HomeScreen.this.getContext(), new org.cspdaily.report.data.SupabaseClient.Callback<org.cspdaily.report.data.SupabaseClient.SyncResult>() {
                    @Override
                    public void onSuccess(org.cspdaily.report.data.SupabaseClient.SyncResult res) {
                        HomeScreen.this.host.toast("Cloud synced: " + res.pulledCount + " updated");
                        HomeScreen.this.refreshContent();
                    }
                    @Override
                    public void onError(String msg, Throwable t) {
                        HomeScreen.this.host.toast("Sync error: " + msg);
                    }
                });
            }
        });
        this.instructionsContainer = new LinearLayout(context);
        this.instructionsContainer.setOrientation(1);
        add(this.instructionsContainer, 0);
        this.hero = Cards.hero(context);
        add(this.hero, 0);
        this.kpiCard = Cards.card(context);
        add(this.kpiCard, 10);
        this.tilesRow = new LinearLayout(context);
        this.tilesRow.setOrientation(0);
        add(this.tilesRow, 10);
        this.attnSection = new Section(context);
        add(this.attnSection, 2);
        this.attention = new LinearLayout(context);
        this.attention.setOrientation(1);
        add(this.attention, 0);
        this.emptyAttention = new Empty(context);
        add(this.emptyAttention, 0);
        this.actSection = new Section(context);
        add(this.actSection, 6);
        this.activity = new LinearLayout(context);
        this.activity.setOrientation(1);
        add(this.activity, 0);
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return "CSP Daily";
    }

    @Override // org.cspdaily.report.ui.Screen
    protected boolean canGoBack() {
        return false;
    }

    @Override // org.cspdaily.report.ui.Screen
    public void refreshContent() {
        this.date = UiState.date(getContext());
        SQLiteDatabase dbRead = Core.get(getContext()).dbRead();
        DaySummary summary = Day.summary(dbRead, this.date);
        List<CspDay> dayData = Day.dayData(dbRead, this.date);

        User currentUser = Auth.currentUser(getContext());
        if (currentUser != null && currentUser.isSupervisor()) {
            Set<Long> mappedIds = SupervisorMap.getCspIdsForSupervisor(dbRead, currentUser.id);
            if (!mappedIds.isEmpty()) {
                List<CspDay> filtered = new ArrayList<>();
                for (CspDay cd : dayData) {
                    if (mappedIds.contains(cd.id)) filtered.add(cd);
                }
                dayData = filtered;
            }
        }

        paintInstructions(dbRead);
        paintHero(dbRead, summary);
        paintKpis(summary);
        paintTiles(summary, dayData);
        paintAttention(dbRead, summary, dayData);
        paintActivity(dbRead);
        subtitle(summary.isWorkingDay() ? "Working day" : summary.dayTypeLabel());
    }

    private void paintInstructions(SQLiteDatabase db) {
        Context context = getContext();
        this.instructionsContainer.removeAllViews();

        // 1. Remote Broadcast Announcement Banner (from Supabase remote_config)
        org.cspdaily.report.update.RemoteConfig.Announcement ann = org.cspdaily.report.update.RemoteConfig.getAnnouncement(context);
        if (ann != null && ann.hasContent()) {
            LinearLayout annCard = Cards.card(context);
            annCard.setOrientation(1);
            int bg = "alert".equalsIgnoreCase(ann.severity) ? 0x26EF4444 : ("warn".equalsIgnoreCase(ann.severity) ? 0x26F59E0B : 0x2638BDF8);
            int border = "alert".equalsIgnoreCase(ann.severity) ? 0xFFEF4444 : ("warn".equalsIgnoreCase(ann.severity) ? 0xFFF59E0B : 0xFF38BDF8);
            annCard.setBackground(Ui.round(context, bg, 14.0f, border, 1.0f));
            Ui.pad(annCard, 14, 12, 14, 12);

            TextView annTitle = new TextView(context);
            annTitle.setText("📢 " + (ann.title.isEmpty() ? "Announcement" : ann.title));
            annTitle.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(annTitle, 14.5f);
            annTitle.setTextColor(border);
            annCard.addView(annTitle);

            TextView annMsg = new TextView(context);
            annMsg.setText(ann.message);
            Ui.size(annMsg, 13.0f);
            annMsg.setTextColor(Ui.color(context, R.color.ink));
            annCard.addView(annMsg, top(context, 4));

            LinearLayout.LayoutParams alp = mw();
            alp.bottomMargin = Ui.dp(context, 8.0f);
            this.instructionsContainer.addView(annCard, alp);
        }

        // 2. Maintenance Mode Alert
        if (org.cspdaily.report.update.RemoteConfig.isMaintenanceMode(context)) {
            LinearLayout maintCard = Cards.card(context);
            maintCard.setOrientation(1);
            maintCard.setBackground(Ui.round(context, 0x33EF4444, 14.0f, 0xFFEF4444, 1.0f));
            Ui.pad(maintCard, 14, 12, 14, 12);

            TextView mTitle = new TextView(context);
            mTitle.setText("⚠️ SYSTEM MAINTENANCE IN PROGRESS");
            mTitle.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(mTitle, 14.5f);
            mTitle.setTextColor(0xFFEF4444);
            maintCard.addView(mTitle);

            TextView mMsg = new TextView(context);
            mMsg.setText(org.cspdaily.report.update.RemoteConfig.getMaintenanceMessage(context));
            Ui.size(mMsg, 13.0f);
            mMsg.setTextColor(Ui.color(context, R.color.ink));
            maintCard.addView(mMsg, top(context, 4));

            LinearLayout.LayoutParams mlp = mw();
            mlp.bottomMargin = Ui.dp(context, 8.0f);
            this.instructionsContainer.addView(maintCard, mlp);
        }

        List<SupervisorMap.Instruction> list = SupervisorMap.getInstructions(db, this.date);
        if (list == null || list.isEmpty()) {
            return;
        }

        for (SupervisorMap.Instruction inst : list) {
            LinearLayout card = Cards.card(context);
            card.setOrientation(1);
            card.setBackground(Ui.round(context, Ui.color(context, R.color.primary_50), 16.0f, Ui.color(context, R.color.primary_200), 1.0f));
            Ui.pad(card, 14, 12, 14, 12);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(0);
            topRow.setGravity(16);
            card.addView(topRow, mw());

            TextView t = new TextView(context);
            t.setText("📌 " + inst.title);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(t, 15.0f);
            t.setTextColor(Ui.color(context, R.color.primary_900));
            topRow.addView(t, weight1());

            Badge b = new Badge(context);
            b.setKind("URGENT".equalsIgnoreCase(inst.priority) ? 4 : 3);
            b.setText(inst.priority);
            topRow.addView(b, wrap());

            TextView msg = new TextView(context);
            msg.setText(inst.message);
            Ui.size(msg, 13.5f);
            msg.setTextColor(Ui.color(context, R.color.ink));
            card.addView(msg, top(context, 4));

            TextView meta = new TextView(context);
            meta.setText("From: " + inst.senderName);
            meta.setTextAppearance(context, R.style.Text_Nano);
            meta.setTextColor(Ui.color(context, R.color.primary_700));
            card.addView(meta, top(context, 4));

            LinearLayout.LayoutParams lp = mw();
            lp.bottomMargin = Ui.dp(context, 8.0f);
            this.instructionsContainer.addView(card, lp);
        }
    }

    private void paintHero(SQLiteDatabase sQLiteDatabase, DaySummary daySummary) {
        Context context = getContext();
        this.hero.removeAllViews();

        // 1. Top Date Navigation & Header Bar
        LinearLayout dateNavRow = new LinearLayout(context);
        dateNavRow.setOrientation(0);
        dateNavRow.setGravity(16);
        this.hero.addView(dateNavRow, mw());

        ImageView prevBtn = edgeButton(R.drawable.ic_arrow_back);
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeScreen.this.step(-1);
            }
        });
        dateNavRow.addView(prevBtn, square(context, 38));

        LinearLayout dateCenter = new LinearLayout(context);
        dateCenter.setOrientation(1);
        dateCenter.setGravity(17);
        dateCenter.setBackground(Ui.pressable(context, 0x22FFFFFF, 16.0f, true));
        Ui.pad(dateCenter, 10, 6, 10, 6);
        dateCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeScreen.this.pickDate();
            }
        });
        LinearLayout.LayoutParams dateCenterLp = weight1();
        dateCenterLp.leftMargin = Ui.dp(context, 6.0f);
        dateCenterLp.rightMargin = Ui.dp(context, 6.0f);
        dateNavRow.addView(dateCenter, dateCenterLp);

        String relDay = Biz.isToday(this.date) ? "Today" : (Biz.isYesterday(this.date) ? "Yesterday" : Biz.dayName(this.date));
        String dateFormatted = Biz.fmtDmy(this.date);
        String workdayStatus = daySummary.isWorkingDay() ? "Working day" : daySummary.dayTypeLabel();

        TextView dateRel = new TextView(context);
        dateRel.setText(relDay + " · " + dateFormatted);
        Ui.size(dateRel, 15.5f);
        dateRel.setTypeface(Typeface.DEFAULT_BOLD);
        dateRel.setTextColor(Ui.color(context, R.color.paper));
        dateRel.setGravity(17);
        dateCenter.addView(dateRel, mw());

        TextView dateSubtitle = new TextView(context);
        dateSubtitle.setText(Biz.dayName(this.date) + " · " + workdayStatus + "  ▾");
        dateSubtitle.setTextAppearance(context, R.style.Text_HeroLabel);
        dateSubtitle.setGravity(17);
        dateCenter.addView(dateSubtitle, mw());

        ImageView nextBtn = edgeButton(R.drawable.ic_chevron_right);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeScreen.this.step(1);
            }
        });
        dateNavRow.addView(nextBtn, square(context, 38));

        // 2. Status Badge Row
        LinearLayout statusRow = new LinearLayout(context);
        statusRow.setOrientation(0);
        statusRow.setGravity(16);
        LinearLayout.LayoutParams statusLp = wrap();
        statusLp.topMargin = Ui.dp(context, 10.0f);
        statusRow.setLayoutParams(statusLp);

        Badge dPill = dayPill(context, daySummary);
        dPill.setBackground(Ui.pressable(context, 0x22FFFFFF, 12.0f, true));
        dPill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeScreen.this.host.push(new BusinessDayScreen(HomeScreen.this.getContext(), HomeScreen.this.host, HomeScreen.this.date));
            }
        });
        statusRow.addView(dPill);

        if (!Biz.isToday(this.date)) {
            TextView todayBadge = new TextView(context);
            todayBadge.setText("Today ↺");
            Ui.size(todayBadge, 11.0f);
            todayBadge.setTypeface(Typeface.DEFAULT_BOLD);
            todayBadge.setTextColor(0xFF0F172A);
            todayBadge.setBackground(Ui.round(context, 0xFFFCD34D, 11.0f, 0xFFFFFFFF, 1.0f));
            Ui.pad(todayBadge, 8, 3, 8, 3);
            Anim3D.attachTactilePress(todayBadge);
            todayBadge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UiState.setDate(HomeScreen.this.getContext(), Biz.today());
                    HomeScreen.this.refreshContent();
                }
            });
            LinearLayout.LayoutParams tLp = wrap();
            tLp.leftMargin = Ui.dp(context, 6.0f);
            statusRow.addView(todayBadge, tLp);
        }

        if (daySummary.isWorkingDay() && UiState.isViewingToday(context) && daySummary.total > 0 && !daySummary.isComplete()) {
            Badge badge = new Badge(context);
            badge.setKind(3);
            badge.setText("● Live");
            LinearLayout.LayoutParams wrap = wrap();
            wrap.leftMargin = Ui.dp(context, 6.0f);
            statusRow.addView(badge, wrap);
        } else if (daySummary.isComplete()) {
            Badge badge2 = new Badge(context);
            badge2.setKind(1);
            badge2.setText("✓ Closed");
            LinearLayout.LayoutParams wrap2 = wrap();
            wrap2.leftMargin = Ui.dp(context, 6.0f);
            statusRow.addView(badge2, wrap2);
        }
        this.hero.addView(statusRow, statusLp);

        // 3. Middle Metrics Section: Circular Ring + 3-Stat Counter Panel
        LinearLayout metricsSection = new LinearLayout(context);
        metricsSection.setOrientation(0);
        metricsSection.setGravity(16);
        LinearLayout.LayoutParams metricsLp = new LinearLayout.LayoutParams(-1, -2);
        metricsLp.topMargin = Ui.dp(context, 12.0f);
        metricsSection.setLayoutParams(metricsLp);
        this.hero.addView(metricsSection);

        Ring ring = new Ring(context);
        ring.setOnDarkSurface(true);
        ring.set((float) daySummary.pct, Integer.toString(daySummary.pctInt()) + "%", daySummary.reported + " of " + daySummary.total);
        metricsSection.addView(ring, square(context, 76));

        // Glass 3-Stat Counter Panel
        LinearLayout statPanel = new LinearLayout(context);
        statPanel.setOrientation(0);
        statPanel.setGravity(16);
        statPanel.setBackground(Ui.round(context, 0x22FFFFFF, 14.0f, 0x33FFFFFF, 1.0f));
        Ui.pad(statPanel, 8, 10, 8, 10);
        LinearLayout.LayoutParams statPanelLp = weight1();
        statPanelLp.leftMargin = Ui.dp(context, 10.0f);
        statPanel.setLayoutParams(statPanelLp);
        metricsSection.addView(statPanel);

        // Stat 1: Reported (Filed)
        statPanel.addView(heroStatCol(context, "FILED", Integer.toString(daySummary.reported), R.color.hero_ok), weight1());
        statPanel.addView(heroStatDivider(context), heroDividerLp(context));

        // Stat 2: Pending
        statPanel.addView(heroStatCol(context, "PENDING", Integer.toString(daySummary.pending), R.color.hero_pending), weight1());
        statPanel.addView(heroStatDivider(context), heroDividerLp(context));

        // Stat 3: Nil
        statPanel.addView(heroStatCol(context, "NIL", Integer.toString(daySummary.nilFigure), R.color.hero_nil), weight1());

        // 4. Progress Track & Animated Fill
        FrameLayout progressContainer = new FrameLayout(context);
        LinearLayout.LayoutParams progressTrackLp = new LinearLayout.LayoutParams(-1, Ui.dp(context, 6.0f));
        progressTrackLp.topMargin = Ui.dp(context, 12.0f);
        progressContainer.setLayoutParams(progressTrackLp);

        this.progressTrack = new View(context);
        this.progressTrack.setBackground(Ui.pill(context, 0x33FFFFFF));
        progressContainer.addView(this.progressTrack, new FrameLayout.LayoutParams(-1, -1));

        this.progressFill = new View(context);
        this.progressFill.setBackground(Ui.pill(context, Ui.color(context, R.color.paper)));
        progressContainer.addView(this.progressFill, new FrameLayout.LayoutParams(0, -1));

        this.hero.addView(progressContainer);

        this.reportedCount = daySummary.reported;
        this.totalCount = Math.max(1, daySummary.total);
        layoutProgress();

        // 5. Office Note Row
        this.hero.addView(officeNoteRow(sQLiteDatabase, context));
    }

    private LinearLayout heroStatCol(Context context, String label, String value, int valueColorRes) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(1);
        col.setGravity(17);

        TextView valView = new TextView(context);
        valView.setText(value);
        Ui.size(valView, 17.0f);
        valView.setTypeface(Typeface.DEFAULT_BOLD);
        valView.setTextColor(Ui.color(context, valueColorRes));
        valView.setGravity(17);
        col.addView(valView, mw());

        TextView lblView = new TextView(context);
        lblView.setText(label);
        lblView.setTextAppearance(context, R.style.Text_HeroLabel);
        Ui.size(lblView, 9.5f);
        lblView.setGravity(17);
        col.addView(lblView, mw());

        return col;
    }

    private View heroStatDivider(Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(0x33FFFFFF);
        return divider;
    }

    private LinearLayout.LayoutParams heroDividerLp(Context context) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(context, 1.0f), Ui.dp(context, 26.0f));
        lp.gravity = 16;
        return lp;
    }

    private TextView heroPrimaryButton(Context context, String text, View.OnClickListener onClickListener) {
        TextView textView = new TextView(context);
        textView.setText(text);
        Ui.size(textView, 13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setSingleLine(true);
        textView.setTextColor(Ui.color(context, R.color.primary_700));
        textView.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, 0x40FFFFFF, 1.0f));
        if (Build.VERSION.SDK_INT >= 21) {
            textView.setElevation(Ui.px(context, R.dimen.el_2));
        }
        Ui.pad(textView, 8, 9, 8, 9);
        textView.setMinimumHeight(Ui.dp(context, 40.0f));
        Anim3D.attachTactilePress(textView);
        textView.setOnClickListener(onClickListener);
        return textView;
    }

    private TextView heroSecondaryButton(Context context, String text, View.OnClickListener onClickListener) {
        TextView textView = new TextView(context);
        textView.setText(text);
        Ui.size(textView, 13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setSingleLine(true);
        textView.setTextColor(Ui.color(context, R.color.paper));
        textView.setBackground(Ui.round(context, 0x2AFFFFFF, 12.0f, 0x40FFFFFF, 1.0f));
        Ui.pad(textView, 8, 9, 8, 9);
        textView.setMinimumHeight(Ui.dp(context, 40.0f));
        Anim3D.attachTactilePress(textView);
        textView.setOnClickListener(onClickListener);
        return textView;
    }

    private void layoutProgress() {
        final View view = this.progressTrack;
        final View view2 = this.progressFill;
        if (view != null && view2 != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    int width = view.getWidth();
                    if (width <= 0) {
                        return;
                    }
                    float max = Math.max(0.0f, Math.min(1.0f, HomeScreen.this.reportedCount / (float) HomeScreen.this.totalCount));
                    android.view.ViewGroup.LayoutParams layoutParams = view2.getLayoutParams();
                    if (layoutParams != null) {
                        layoutParams.width = Math.round(width * max);
                        view2.setLayoutParams(layoutParams);
                        view2.setPivotX(0.0f);
                        view2.setScaleX(0.0f);
                        view2.animate().scaleX(1.0f).setDuration(620L).setInterpolator(new DecelerateInterpolator(1.2f)).start();
                    }
                }
            });
        }
    }

    private View officeNoteRow(SQLiteDatabase sQLiteDatabase, Context context) {
        String officeNote = Day.officeNote(sQLiteDatabase, this.date);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackground(Ui.pressable(context, 0x20FFFFFF, 12.0f, true));
        Ui.pad(linearLayout, 10, 8, 10, 8);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(context, 10.0f);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeScreen.this.host.push(new OfficeNoteScreen(HomeScreen.this.getContext(), HomeScreen.this.host, HomeScreen.this.date));
            }
        });
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams square = square(context, 16);
        square.rightMargin = Ui.dp(context, 8.0f);
        imageView.setLayoutParams(square);
        imageView.setImageResource(R.drawable.ic_note);
        imageView.setColorFilter(Ui.color(context, R.color.paper));
        linearLayout.addView(imageView);
        TextView textView = new TextView(context);
        textView.setText(officeNote.isEmpty() ? "Tap to add today's office note..." : officeNote);
        Ui.size(textView, 12.5f);
        textView.setTextColor(officeNote.isEmpty() ? Ui.color(context, R.color.hero_text_dim) : Ui.color(context, R.color.paper));
        textView.setMaxLines(2);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(textView, weight1());

        ImageView chevron = new ImageView(context);
        chevron.setLayoutParams(square(context, 14));
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(Ui.color(context, R.color.hero_text_dim));
        linearLayout.addView(chevron);

        return linearLayout;
    }

    private Badge dayPill(Context context, DaySummary daySummary) {
        Badge badge = new Badge(context);
        if (daySummary.isWorkingDay()) {
            badge.setKind(5);
            badge.setText("Working day");
        } else if (DaySummary.SUNDAY.equals(daySummary.dayType)) {
            badge.setKind(3);
            badge.setText("Sunday");
        } else if (DaySummary.HOLIDAY.equals(daySummary.dayType)) {
            badge.setKind(2);
            badge.setText("Holiday");
        } else {
            badge.setKind(0);
            badge.setText("Special closure");
        }
        return badge;
    }

    private ImageView edgeButton(int i) {
        Context context = getContext();
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(i);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setColorFilter(Ui.color(context, R.color.paper));
        imageView.setBackground(Ui.pressable(context, 0x22FFFFFF, 19.0f, true));
        int pad = Ui.dp(context, 10.0f);
        imageView.setPadding(pad, pad, pad, pad);
        return imageView;
    }

    private String headlineDate() {
        if (Biz.isToday(this.date)) {
            return "Today";
        }
        if (Biz.isYesterday(this.date)) {
            return "Yesterday";
        }
        return Biz.fmtDmy(this.date);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void step(int i) {
        UiState.setDate(getContext(), Biz.plusDays(this.date, i));
        refreshContent();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pickDate() {
        Calendar cal = Biz.cal(this.date);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() { // from class: org.cspdaily.report.ui.screen.HomeScreen.8
            @Override // android.app.DatePickerDialog.OnDateSetListener
            public void onDateSet(DatePicker datePicker, int i, int i2, int i3) {
                UiState.setDate(HomeScreen.this.getContext(), String.format(Locale.US, "%04d-%02d-%02d", Integer.valueOf(i), Integer.valueOf(i2 + 1), Integer.valueOf(i3)));
                HomeScreen.this.refreshContent();
            }
        }, cal.get(1), cal.get(2), cal.get(5));
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis() + 31622400000L);
        datePickerDialog.show();
    }

    private void paintKpis(DaySummary daySummary) {
        Context context = getContext();
        this.kpiCard.removeAllViews();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        this.kpiCard.addView(linearLayout, mw());

        Kpi kpi = new Kpi(context);
        kpi.set("REPORTED", Integer.toString(daySummary.reported), daySummary.pctInt() + "% done", R.color.ok);
        kpi.valueColor(R.color.ok);
        linearLayout.addView(kpi, weight1());

        Kpi kpi2 = new Kpi(context);
        kpi2.set("PENDING", Integer.toString(daySummary.pending), daySummary.remaining + " left", R.color.danger);
        kpi2.valueColor(R.color.danger);
        LinearLayout.LayoutParams weight1 = weight1();
        weight1.leftMargin = Ui.dp(context, 6.0f);
        linearLayout.addView(kpi2, weight1);

        Kpi kpi3 = new Kpi(context);
        kpi3.set("NIL", Integer.toString(daySummary.nilFigure), "Zero figures", R.color.info);
        kpi3.valueColor(R.color.info);
        LinearLayout.LayoutParams weight12 = weight1();
        weight12.leftMargin = Ui.dp(context, 6.0f);
        linearLayout.addView(kpi3, weight12);

        Kpi kpi4 = new Kpi(context);
        kpi4.set("BUSINESS FIGURE", Integer.toString(daySummary.figureReported), "With business", R.color.primary_700);
        kpi4.valueColor(R.color.primary_700);
        LinearLayout.LayoutParams weight13 = weight1();
        weight13.leftMargin = Ui.dp(context, 6.0f);
        linearLayout.addView(kpi4, weight13);

        this.kpiCard.addView(Cards.rule(context, R.color.line, 10, 8));

        // Business Summary Row
        LinearLayout summaryRow = new LinearLayout(context);
        summaryRow.setOrientation(0);
        summaryRow.setGravity(16);
        this.kpiCard.addView(summaryRow, mw());

        TextView activeText = Cards.bold(context, daySummary.total + " active CSPs", 12.5f, R.color.ink_2);
        summaryRow.addView(activeText, weight1());

        TextView bizTotalText = Cards.bold(context, "Business total " + daySummary.totalBusiness, 12.5f, R.color.primary_700);
        summaryRow.addView(bizTotalText, wrap());
    }

    private void paintTiles(DaySummary daySummary, List<CspDay> list) {
        int notContacted = 0;
        Context context = getContext();
        this.tilesRow.removeAllViews();
        for (CspDay next : list) {
            if (next.isPending() && Day.NOT_CONTACTED.equals(next.followupStatus)) {
                notContacted++;
            }
        }

        // Consolidated premier CSPs Directory Card
        String sub = daySummary.total + " Total CSPs · " + daySummary.reported + " Reported · " + daySummary.pending + " Pending" + (notContacted > 0 ? " (" + notContacted + " follow-up due)" : "");
        View directoryCard = directoryTile(context, "CSPs DIRECTORY", sub, daySummary.pending, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeScreen.this.host.push(new CspListScreen(HomeScreen.this.getContext(), HomeScreen.this.host, 0));
            }
        });
        this.tilesRow.addView(directoryCard, mw());
    }

    private LinearLayout.LayoutParams tileGap(Context context) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 8.0f);
        return mw;
    }

    private LinearLayout tile(Context context, int i, int i2, String str, String str2, View.OnClickListener onClickListener) {
        int i3;
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 16.0f, Ui.color(context, R.color.line), 1.0f));
        if (Build.VERSION.SDK_INT >= 21) {
            linearLayout.setElevation(Ui.dp(context, 2.0f));
        }
        Ui.pad(linearLayout, 14, 14, 14, 14);
        linearLayout.setClickable(true);
        Anim3D.attachTactilePress(linearLayout);
        linearLayout.setOnClickListener(onClickListener);

        FrameLayout frameLayout = new FrameLayout(context);
        if (i2 == R.color.primary_600) {
            i3 = R.color.primary_50;
        } else if (i2 == R.color.warn) {
            i3 = R.color.warn_bg;
        } else if (i2 == R.color.ok) {
            i3 = R.color.ok_bg;
        } else {
            i3 = R.color.info_bg;
        }
        frameLayout.setBackground(Ui.round(context, Ui.color(context, i3), 12.0f, 0, 0.0f));
        int dp = Ui.dp(context, 38.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp, dp);
        layoutParams.bottomMargin = Ui.dp(context, 10.0f);
        frameLayout.setLayoutParams(layoutParams);
        linearLayout.addView(frameLayout);

        ImageView imageView = new ImageView(context);
        int dp2 = Ui.dp(context, 20.0f);
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(dp2, dp2);
        layoutParams2.gravity = 17;
        imageView.setLayoutParams(layoutParams2);
        imageView.setImageResource(i);
        imageView.setColorFilter(Ui.color(context, i2));
        frameLayout.addView(imageView);

        TextView textView = new TextView(context);
        textView.setText(str);
        Ui.size(textView, 14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, R.color.ink));
        textView.setSingleLine(true);
        linearLayout.addView(textView, mw());

        TextView textView2 = new TextView(context);
        textView2.setText(str2);
        Ui.size(textView2, 12.0f);
        textView2.setTextColor(Ui.color(context, i2 == R.color.warn ? R.color.warn : R.color.primary_700));
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setSingleLine(true);
        textView2.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 3.0f);
        linearLayout.addView(textView2, mw);
        return linearLayout;
    }

    private LinearLayout directoryTile(Context context, String title, String subtitle, int pendingCount, View.OnClickListener onClickListener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(0);
        card.setGravity(16);
        card.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 16.0f, Ui.color(context, R.color.line), 1.0f));
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(Ui.dp(context, 2.0f));
        }
        Ui.pad(card, 16, 14, 16, 14);
        card.setClickable(true);
        Anim3D.attachTactilePress(card);
        card.setOnClickListener(onClickListener);

        FrameLayout iconFrame = new FrameLayout(context);
        iconFrame.setBackground(Ui.round(context, Ui.color(context, R.color.primary_50), 14.0f, Ui.color(context, R.color.primary_200), 1.0f));
        int dp = Ui.dp(context, 44.0f);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp, dp);
        iconLp.rightMargin = Ui.dp(context, 14.0f);
        iconFrame.setLayoutParams(iconLp);

        ImageView icon = new ImageView(context);
        int dpIcon = Ui.dp(context, 24.0f);
        FrameLayout.LayoutParams iconInnerLp = new FrameLayout.LayoutParams(dpIcon, dpIcon);
        iconInnerLp.gravity = 17;
        icon.setLayoutParams(iconInnerLp);
        icon.setImageResource(R.drawable.ic_csp);
        icon.setColorFilter(Ui.color(context, R.color.primary_600));
        iconFrame.addView(icon);
        card.addView(iconFrame);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(1);
        LinearLayout.LayoutParams colLp = weight1();
        textCol.setLayoutParams(colLp);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        Ui.size(titleView, 15.5f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Ui.color(context, R.color.ink));
        textCol.addView(titleView, mw());

        TextView subView = new TextView(context);
        subView.setText(subtitle);
        subView.setTextAppearance(context, R.style.Text_Meta);
        subView.setTextColor(Ui.color(context, R.color.ink_3));
        Ui.pad(subView, 0, 2, 0, 0);
        textCol.addView(subView, mw());
        card.addView(textCol);

        if (pendingCount > 0) {
            Badge badge = new Badge(context);
            badge.setKind(Badge.WARN);
            badge.setText(pendingCount + " Pending");
            card.addView(badge, wrap());
        } else {
            Badge badge = new Badge(context);
            badge.setKind(Badge.OK);
            badge.setText("100% Done");
            card.addView(badge, wrap());
        }

        ImageView chevron = new ImageView(context);
        int chSize = Ui.dp(context, 18.0f);
        LinearLayout.LayoutParams chLp = new LinearLayout.LayoutParams(chSize, chSize);
        chLp.leftMargin = Ui.dp(context, 8.0f);
        chevron.setLayoutParams(chLp);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(Ui.color(context, R.color.ink_4));
        card.addView(chevron);

        return card;
    }

    private void paintAttention(SQLiteDatabase sQLiteDatabase, DaySummary daySummary, List<CspDay> list) {
        Context context = getContext();
        this.attnSection.set("Attention Required", daySummary.pending > 0 ? "View all" : null, new Runnable() {
            @Override
            public void run() {
                HomeScreen.this.host.push(new CspListScreen(HomeScreen.this.getContext(), HomeScreen.this.host, 0, "Attention Required"));
            }
        });
        this.attention.removeAllViews();
        ArrayList<CspDay> pendingList = new ArrayList<>();
        int staleCount = 0;
        for (CspDay cspDay : list) {
            if (cspDay.isPending()) {
                pendingList.add(cspDay);
                if (cspDay.pendingDays >= 3) {
                    staleCount++;
                }
            }
        }
        if (pendingList.isEmpty()) {
            this.emptyAttention.show(R.drawable.ic_check_circle, R.color.ok, "All CSPs are up to date", "Nothing requires attention for " + headlineDate().toLowerCase() + ".");
            this.attnSection.hideAction();
            return;
        }
        this.emptyAttention.hide();
        this.attention.addView(attentionBanner(context, pendingList.size(), staleCount), mw());
        int shownCount = 0;
        for (CspDay cspDay : pendingList) {
            if (shownCount >= 5) {
                break;
            }
            View pendingRow = pendingRow(context, cspDay);
            this.attention.addView(pendingRow, mw());
            Ui.enterRow(pendingRow, shownCount);
            shownCount++;
        }
        if (pendingList.size() > shownCount) {
            TextView textView = new TextView(context);
            textView.setText("View all " + pendingList.size() + " pending CSPs");
            Ui.size(textView, 13.0f);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextColor(Ui.color(context, R.color.primary_600));
            textView.setGravity(17);
            textView.setClickable(true);
            textView.setBackground(Ui.pressable(context, Ui.color(context, R.color.transparent), 10.0f, false));
            Ui.pad(textView, 12, 12, 12, 8);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    HomeScreen.this.host.push(new FollowupScreen(HomeScreen.this.getContext(), HomeScreen.this.host, HomeScreen.this.date));
                }
            });
            this.attention.addView(textView, mw());
        }
    }

    private View attentionBanner(Context context, int totalPending, int staleCount) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackground(Ui.round(context, Ui.color(context, R.color.danger_bg), 12.0f, Ui.color(context, R.color.danger_border), 1.0f));
        Ui.pad(linearLayout, 12, 10, 12, 10);
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams square = square(context, 16);
        square.rightMargin = Ui.dp(context, 8.0f);
        imageView.setLayoutParams(square);
        imageView.setImageResource(R.drawable.ic_alert);
        imageView.setColorFilter(Ui.color(context, R.color.danger));
        linearLayout.addView(imageView);

        TextView textView = new TextView(context);
        textView.setText(totalPending + " CSP" + (totalPending == 1 ? " hasn't" : "s haven't") + " reported" + (staleCount > 0 ? "  ·  " + staleCount + " long pending" : ""));
        Ui.size(textView, 12.5f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(context, R.color.danger));
        linearLayout.addView(textView, weight1());
        return linearLayout;
    }

    private View pendingRow(final Context context, final CspDay cspDay) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 14.0f, Ui.color(context, R.color.line), 1.0f));
        if (Build.VERSION.SDK_INT >= 21) {
            linearLayout.setElevation(Ui.dp(context, 1.5f));
        }
        Ui.pad(linearLayout, 12, 10, 12, 10);
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, 8.0f);
        linearLayout.setLayoutParams(mw);

        Avatar avatar = new Avatar(context, 38);
        avatar.bind(cspDay.name, cspDay.code);
        linearLayout.addView(avatar);

        LinearLayout infoCol = new LinearLayout(context);
        infoCol.setOrientation(1);
        LinearLayout.LayoutParams weight1 = weight1();
        weight1.leftMargin = Ui.dp(context, 10.0f);
        infoCol.setLayoutParams(weight1);
        linearLayout.addView(infoCol);

        TextView titleView = new TextView(context);
        titleView.setText(cspDay.name);
        titleView.setTextAppearance(context, R.style.Text_CardTitle);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(titleView, mw());

        StringBuilder sb = new StringBuilder();
        sb.append("#").append(cspDay.suffix());
        String reasonOrPromise = cspDay.effectiveReasonOrPromise();
        sb.append(!reasonOrPromise.isEmpty() ? " · " + reasonOrPromise : " · " + cspDay.followupStatus);
        TextView subView = new TextView(context);
        subView.setText(sb);
        subView.setTextAppearance(context, R.style.Text_Meta);
        subView.setSingleLine(true);
        subView.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(subView, mw());

        Badge badge = new Badge(context);
        badge.setKind(cspDay.pendingDays >= 3 ? 2 : 3);
        badge.setText(cspDay.pendingDays >= 3 ? "Stale" : "Pending");
        linearLayout.addView(badge, wrap());

        if (cspDay.mobile != null && !cspDay.mobile.trim().isEmpty()) {
            ImageView callBtn = new ImageView(context);
            int dp = Ui.dp(context, 32.0f);
            LinearLayout.LayoutParams callLp = new LinearLayout.LayoutParams(dp, dp);
            callLp.leftMargin = Ui.dp(context, 8.0f);
            callBtn.setLayoutParams(callLp);
            callBtn.setImageResource(R.drawable.ic_phone);
            callBtn.setColorFilter(Ui.color(context, R.color.primary_600));
            callBtn.setBackground(Ui.round(context, Ui.color(context, R.color.primary_50), 16.0f, Ui.color(context, R.color.primary_200), 1.0f));
            int pad = Ui.dp(context, 7.0f);
            callBtn.setPadding(pad, pad, pad, pad);
            Anim3D.attachTactilePress(callBtn);
            callBtn.setClickable(true);
            callBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PhoneActions.call(context, cspDay);
                }
            });
            linearLayout.addView(callBtn);
        }

        linearLayout.setClickable(true);
        Anim3D.attachTactilePress(linearLayout);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeScreen.this.host.push(new CspProfileScreen(HomeScreen.this.getContext(), HomeScreen.this.host, cspDay.id, HomeScreen.this.date));
            }
        });
        return linearLayout;
    }

    private void paintActivity(SQLiteDatabase sQLiteDatabase) {
        Context context = getContext();
        this.actSection.set("Today's activity", "View all", new Runnable() {
            @Override
            public void run() {
                HomeScreen.this.host.push(new ActivityScreen(HomeScreen.this.getContext(), HomeScreen.this.host, HomeScreen.this.date));
            }
        });
        this.activity.removeAllViews();
        List<Journal.Entry> recent = Journal.recentForDate(sQLiteDatabase, 6, this.date);
        if (recent.isEmpty()) {
            Empty empty = new Empty(context);
            empty.show(R.drawable.ic_history, R.color.line_2, "No activity today", "Reports filed and follow-ups logged for " + headlineDate().toLowerCase() + " will appear here.");
            this.activity.addView(empty, mw());
        } else {
            LinearLayout cardFlat = Cards.cardFlat(context);
            int i = 0;
            while (i < recent.size()) {
                cardFlat.addView(Timeline.row(context, recent.get(i), i == recent.size() - 1), mw());
                i++;
            }
            this.activity.addView(cardFlat, mw());
        }
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }

    private static LinearLayout.LayoutParams square(Context context, int i) {
        int dp = Ui.dp(context, i);
        return new LinearLayout.LayoutParams(dp, dp);
    }

    private static LinearLayout.LayoutParams top(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }

    /* loaded from: classes.dex */
    private static final class QuickScreens {
        static final int PICK_FOLLOWUP = 2;
        static final int PICK_REPORT = 1;

        private QuickScreens() {
        }
    }
}
