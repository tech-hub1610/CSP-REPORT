package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.util.PhoneActions;

public final class FollowupSheet {
    private FollowupSheet() {
    }

    public static void open(final Activity activity, final Screen.Host host, final CspDay initialCspDay, final String date) {
        final Dialog dialog = new Dialog(activity, R.style.Theme_Csp_Sheet);
        dialog.requestWindowFeature(1);

        final SQLiteDatabase dbRead = Core.get(activity).dbRead();
        CspDay found = null;
        for (CspDay cd : Day.dayData(dbRead, date, false)) {
            if (cd.id == initialCspDay.id) {
                found = cd;
                break;
            }
        }
        final CspDay cspDay = found != null ? found : initialCspDay;

        final String[] followupStatusHolder = {cspDay.followupStatus};
        final String[] pendingReasonHolder = {cspDay.pendingReason == null ? "" : cspDay.pendingReason};

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(1);

        // 1. Hero Header
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(1);
        header.setBackgroundResource(R.drawable.bg_hero_card);
        Ui.pad(header, 16, 12, 16, 12);

        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(0);
        titleRow.setGravity(16);

        TextView heroLabel = Cards.label(activity, "Follow-up & Contact", R.style.Text_HeroLabel, R.color.hero_text_dim);
        titleRow.addView(heroLabel, weight1());

        TextView datePill = new TextView(activity);
        datePill.setText(Biz.friendlyDate(date));
        Ui.size(datePill, 11.5f);
        datePill.setTypeface(Typeface.DEFAULT_BOLD);
        datePill.setTextColor(Ui.color(activity, R.color.paper));
        datePill.setBackground(Ui.pill(activity, 0x33FFFFFF));
        Ui.pad(datePill, 8, 3, 8, 3);
        titleRow.addView(datePill, wrap());
        header.addView(titleRow, mw());

        TextView nameView = Cards.bold(activity, cspDay.name, 17.0f, R.color.paper);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(nameView, mw());

        StringBuilder sub = new StringBuilder();
        sub.append("#").append(cspDay.suffix());
        if (!cspDay.branch.isEmpty()) {
            sub.append("  ·  ").append(cspDay.branch);
        }
        if (!cspDay.mobile.isEmpty()) {
            sub.append("  ·  ").append(cspDay.mobile);
        }
        header.addView(Cards.label(activity, sub.toString(), R.style.Text_Meta, R.color.hero_text_dim), mw());

        // Quick action row in header
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(0);
        actionRow.setGravity(16);
        LinearLayout.LayoutParams actionRowLp = mw();
        actionRowLp.topMargin = Ui.dp(activity, 8.0f);
        actionRow.setLayoutParams(actionRowLp);

        if (cspDay.mobile != null && cspDay.mobile.length() > 0) {
            actionRow.addView(sheetMiniAction(activity, "Call", R.drawable.ic_phone, R.color.paper, 0x22FFFFFF, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PhoneActions.call(activity, cspDay);
                }
            }), weight1());

            View v1 = new View(activity);
            v1.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity, 6.0f), 1));
            actionRow.addView(v1);

            actionRow.addView(sheetMiniAction(activity, "WhatsApp", R.drawable.ic_whatsapp, R.color.paper, 0x22FFFFFF, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PhoneActions.whatsapp(activity, cspDay);
                }
            }), weight1());

            View v2 = new View(activity);
            v2.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity, 6.0f), 1));
            actionRow.addView(v2);
        }

        actionRow.addView(sheetMiniAction(activity, cspDay.reported ? "Edit Report" : "Enter Report", R.drawable.ic_edit, R.color.paper, 0x22FFFFFF, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                ReportEntrySheet.open(activity, host, cspDay, date);
            }
        }), weight1());

        header.addView(actionRow);
        root.addView(header, mw());

        // 2. Scrollable Body
        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(1);
        Ui.pad(body, 16, 12, 16, 14);
        scroll.addView(body, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        root.addView(scroll, scrollLp);

        // Section A: Current Status Banner & Contact Outcome
        LinearLayout sectionStatus = Cards.cardFlat(activity);
        Ui.pad(sectionStatus, 14, 12, 14, 12);

        LinearLayout statusHeader = new LinearLayout(activity);
        statusHeader.setOrientation(0);
        statusHeader.setGravity(16);

        TextView lblContact = Cards.label(activity, "Contact Status", R.style.Text_Section, R.color.ink_3);
        statusHeader.addView(lblContact, weight1());

        final Badge curBadge = new Badge(activity);
        curBadge.setKind(badgeKindForStatus(followupStatusHolder[0]));
        curBadge.setText(followupStatusHolder[0]);
        statusHeader.addView(curBadge, wrap());
        sectionStatus.addView(statusHeader, mw());

        Chips chipsStatus = new Chips(activity);
        chipsStatus.set(Day.FOLLOWUP_STATUSES);
        chipsStatus.allowNone(false);
        chipsStatus.select(indexOf(Day.FOLLOWUP_STATUSES, followupStatusHolder[0]));
        chipsStatus.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str2) {
                followupStatusHolder[0] = str2;
                curBadge.setKind(badgeKindForStatus(str2));
                curBadge.setText(str2);
            }
        });
        sectionStatus.addView(chipsStatus, top(activity, 8));
        body.addView(sectionStatus, mw());

        // Section B: Pending Reason
        LinearLayout sectionReason = Cards.cardFlat(activity);
        Ui.pad(sectionReason, 14, 12, 14, 12);
        sectionReason.setLayoutParams(top(activity, 10));

        LinearLayout reasonHeader = new LinearLayout(activity);
        reasonHeader.setOrientation(0);
        reasonHeader.setGravity(16);
        reasonHeader.addView(Cards.label(activity, "Reason for Delay", R.style.Text_Section, R.color.ink_3), weight1());

        if (cspDay.pendingDays > 0) {
            TextView pendingDaysPill = Cards.label(activity, cspDay.pendingDays == 1 ? "1 day pending" : (cspDay.pendingDays + " days pending"), R.style.Text_Nano, R.color.danger);
            pendingDaysPill.setBackground(Ui.pill(activity, Ui.color(activity, R.color.danger_bg)));
            Ui.pad(pendingDaysPill, 6, 2, 6, 2);
            reasonHeader.addView(pendingDaysPill, wrap());
        }
        sectionReason.addView(reasonHeader, mw());

        String[] withNone = withNone(Day.PENDING_REASONS);
        final Chips chipsReason = new Chips(activity);
        chipsReason.set(withNone);
        chipsReason.allowNone(true);
        chipsReason.select(indexOf(withNone, pendingReasonHolder[0]));
        chipsReason.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str2) {
                if ("None".equals(str2)) {
                    str2 = "";
                }
                pendingReasonHolder[0] = str2;
            }
        });
        sectionReason.addView(chipsReason, top(activity, 8));

        if (cspDay.remarks != null && !cspDay.remarks.trim().isEmpty()) {
            String remLow = cspDay.remarks.trim().toLowerCase(java.util.Locale.US);
            String matchedReason = null;
            for (String pr : Day.PENDING_REASONS) {
                if (remLow.contains(pr.toLowerCase(java.util.Locale.US)) || pr.toLowerCase(java.util.Locale.US).contains(remLow)) {
                    matchedReason = pr;
                    break;
                }
            }
            if (matchedReason == null) {
                if (remLow.contains("close")) matchedReason = "Closed Today";
                else if (remLow.contains("tech") || remLow.contains("server") || remLow.contains("down") || remLow.contains("net")) matchedReason = "Technical Issue";
                else if (remLow.contains("branch")) matchedReason = "Branch Issue";
                else if (remLow.contains("call") || remLow.contains("ring")) matchedReason = "Not receiving call";
                else if (remLow.contains("busy") || remLow.contains("later")) matchedReason = "Busy - will report later";
                else if (remLow.contains("switch") || remLow.contains("reach")) matchedReason = "Switched off / unreachable";
            }
            if (matchedReason != null) {
                final String finalReason = matchedReason;
                TextView sugPill = Cards.label(activity, "💡 Suggested from remarks: " + matchedReason, R.style.Text_Nano, R.color.primary_700);
                sugPill.setBackground(Ui.pill(activity, Ui.color(activity, R.color.primary_50)));
                Ui.pad(sugPill, 8, 4, 8, 4);
                sugPill.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        chipsReason.select(indexOf(withNone, finalReason));
                        pendingReasonHolder[0] = finalReason;
                    }
                });
                sectionReason.addView(sugPill, top(activity, 6));
            }
        }

        body.addView(sectionReason, mw());

        // Section C: Note & Quick Prompts
        LinearLayout sectionNote = Cards.cardFlat(activity);
        Ui.pad(sectionNote, 14, 12, 14, 12);
        sectionNote.setLayoutParams(top(activity, 10));

        sectionNote.addView(Cards.label(activity, "Follow-up Note / Promise", R.style.Text_Section, R.color.ink_3), mw());

        final EditText editTextNote = new EditText(activity);
        editTextNote.setHint("e.g. Will file by 4 PM, customer rush");
        Ui.size(editTextNote, 14.5f);
        editTextNote.setTextColor(Ui.color(activity, R.color.ink));
        editTextNote.setSingleLine(true);
        editTextNote.setBackground(Ui.round(activity, Ui.color(activity, R.color.paper_2), 10.0f, Ui.color(activity, R.color.line), 1.0f));
        Ui.pad(editTextNote, 12, 10, 12, 10);
        if (cspDay.followupNote != null && !cspDay.followupNote.isEmpty()) {
            editTextNote.setText(cspDay.followupNote);
            editTextNote.setSelection(cspDay.followupNote.length());
        }
        sectionNote.addView(editTextNote, top(activity, 6));

        Chips chipsPresets = new Chips(activity);
        chipsPresets.set("Will report by 4 PM", "Will report by 6 PM", "Portal / Server Down", "Not reachable", "Closed Today", "Cash problem");
        chipsPresets.allowNone(true);
        chipsPresets.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str2) {
                if (str2 != null && str2.length() > 0) {
                    editTextNote.setText(str2);
                    editTextNote.setSelection(str2.length());
                }
            }
        });
        sectionNote.addView(chipsPresets, top(activity, 8));
        body.addView(sectionNote, mw());

        // Section D: Activity & Timeline History
        List<Map<String, String>> followupHistory = Day.followupHistory(Core.get(activity).dbRead(), cspDay.id, date, 12);
        LinearLayout sectionHistory = Cards.cardFlat(activity);
        Ui.pad(sectionHistory, 14, 12, 14, 12);
        sectionHistory.setLayoutParams(top(activity, 10));

        LinearLayout histHead = new LinearLayout(activity);
        histHead.setOrientation(0);
        histHead.setGravity(16);
        histHead.addView(Cards.label(activity, "Activity Log (" + Biz.friendlyDate(date) + ")", R.style.Text_Section, R.color.ink_3), weight1());
        sectionHistory.addView(histHead, mw());

        if (followupHistory.isEmpty()) {
            TextView emptyHist = Cards.label(activity, "No activity logged for " + Biz.friendlyDate(date) + " yet.", R.style.Text_Meta, R.color.ink_4);
            Ui.pad(emptyHist, 0, 6, 0, 2);
            sectionHistory.addView(emptyHist, mw());
        } else {
            LinearLayout historyContainer = new LinearLayout(activity);
            historyContainer.setOrientation(1);
            historyContainer.setBackground(Ui.round(activity, Ui.color(activity, R.color.paper_2), 10.0f, Ui.color(activity, R.color.line), 1.0f));
            historyContainer.setLayoutParams(top(activity, 6));

            int i = 0;
            Iterator<Map<String, String>> it = followupHistory.iterator();
            while (it.hasNext()) {
                Map<String, String> next = it.next();
                if (i > 0) {
                    historyContainer.addView(Cards.rule(activity, R.color.line_2, 0, 0));
                }
                historyContainer.addView(historyRow(activity, next, i == 0), mw());
                i++;
            }
            sectionHistory.addView(historyContainer, mw());
        }
        body.addView(sectionHistory, mw());

        // 3. Sticky Bottom Action Bar
        LinearLayout bottomBar = new LinearLayout(activity);
        bottomBar.setOrientation(0);
        bottomBar.setGravity(16);
        bottomBar.setBackgroundResource(R.drawable.bg_sticky_bar);
        Ui.pad(bottomBar, 16, 10, 16, 14);
        root.addView(bottomBar, mw());

        if (cspDay.reported) {
            bottomBar.addView(Cards.label(activity, "Report already filed.", R.style.Text_Meta, R.color.ok), weight1());
        } else {
            LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(-2, -2);
            clearLp.rightMargin = Ui.dp(activity, 8.0f);
            TextView clearBtn = Cards.link(activity, "Clear reason", R.color.danger, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    persist(activity, host, dialog, cspDay, date, followupStatusHolder[0], "", editTextNote.getText().toString(), true);
                }
            });
            bottomBar.addView(clearBtn, clearLp);
        }

        TextView saveBtn = Cards.primaryButton(activity, "Save Follow-up", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                persist(activity, host, dialog, cspDay, date, followupStatusHolder[0], pendingReasonHolder[0], editTextNote.getText().toString(), false);
            }
        });
        bottomBar.addView(saveBtn, weight1());

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(-1, dp(activity, 580));
            dialog.getWindow().setGravity(80);
            dialog.getWindow().setWindowAnimations(R.style.Animation_Sheet);
        }
        dialog.show();
    }

    private static int badgeKindForStatus(String status) {
        if (status == null) return Badge.NEUTRAL;
        if (Day.NOT_CONTACTED.equalsIgnoreCase(status)) return Badge.WARN;
        if ("Called".equalsIgnoreCase(status) || "Contacted".equalsIgnoreCase(status)) return Badge.OK;
        if ("WhatsApp Sent".equalsIgnoreCase(status)) return Badge.WHATSAPP;
        if ("No Response".equalsIgnoreCase(status)) return Badge.DANGER;
        return Badge.INFO;
    }

    private static void persist(Context context, Screen.Host host, Dialog dialog, CspDay cspDay, String date, String status, String reason, String note, boolean isClear) {
        SQLiteDatabase db = Core.get(context).db();
        if (!isClear) {
            String trim = note == null ? "" : note.trim();
            if (!Day.NOT_CONTACTED.equals(status) || trim.length() > 0) {
                Day.logFollowup(db, cspDay.id, date, status, trim);
            }
        }
        Day.saveReason(db, cspDay.id, date, reason);
        dialog.dismiss();
        host.refresh();
        host.toast(isClear ? "Pending reason cleared" : "Follow-up saved");
    }

    private static View historyRow(Context context, Map<String, String> map, boolean isFirst) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(1);
        Ui.pad(row, 12, 9, 12, 9);

        String note = map.get("note");
        String actionStatus = map.get("action_status");
        String createdAt = map.get("created_at");
        String bDate = map.get("business_date");

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(0);
        titleRow.setGravity(16);

        TextView statusTv = new TextView(context);
        statusTv.setText(actionStatus);
        Ui.size(statusTv, 13.5f);
        statusTv.setTypeface(Typeface.DEFAULT_BOLD);
        statusTv.setTextColor(Ui.color(context, isFirst ? R.color.primary_700 : R.color.ink));
        titleRow.addView(statusTv, weight1());

        String timeStr = Biz.clockOf(createdAt);
        if (timeStr.isEmpty()) {
            timeStr = Biz.shortWhen(createdAt);
        }
        TextView whenTv = Cards.label(context, (bDate != null ? Biz.fmtDmy(bDate) + " · " : "") + timeStr, R.style.Text_Nano, R.color.ink_4);
        titleRow.addView(whenTv, wrap());
        row.addView(titleRow, mw());

        if (note != null && !note.trim().isEmpty()) {
            TextView noteTv = Cards.label(context, note.trim(), R.style.Text_Meta, R.color.ink_2);
            noteTv.setMaxLines(2);
            noteTv.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams noteLp = mw();
            noteLp.topMargin = Ui.dp(context, 4.0f);
            row.addView(noteTv, noteLp);
        }

        return row;
    }

    private static View sheetMiniAction(Context context, String label, int iconRes, int tintColorRes, int bgCol, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(17);
        row.setBackground(Ui.round(context, bgCol, 10.0f, 0x33FFFFFF, 1.0f));
        Ui.pad(row, 6, 8, 6, 8);
        row.setClickable(true);
        row.setOnClickListener(onClick);

        ImageView iv = new ImageView(context);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(Ui.dp(context, 14.0f), Ui.dp(context, 14.0f));
        ivLp.rightMargin = Ui.dp(context, 4.0f);
        iv.setLayoutParams(ivLp);
        iv.setImageResource(iconRes);
        iv.setColorFilter(Ui.color(context, tintColorRes));
        row.addView(iv);

        TextView tv = new TextView(context);
        tv.setText(label);
        Ui.size(tv, 12.0f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Ui.color(context, tintColorRes));
        tv.setSingleLine(true);
        row.addView(tv, wrap());
        return row;
    }

    private static String[] withNone(String[] strArr) {
        String[] strArr2 = new String[strArr.length + 1];
        strArr2[0] = "None";
        System.arraycopy(strArr, 0, strArr2, 1, strArr.length);
        return strArr2;
    }

    private static int indexOf(String[] strArr, String str) {
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].equals(str)) {
                return i;
            }
        }
        return -1;
    }

    private static int dp(Context context, int i) {
        return Ui.dp(context, i);
    }

    private static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private static LinearLayout.LayoutParams top(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }

    private static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }
}
