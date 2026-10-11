package org.cspdaily.report.ui.screen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Empty;

public class AdminInstructionScreen extends Screen {
    private String date;
    private final LinearLayout list;
    private final Empty empty;

    public AdminInstructionScreen(final Context context, final Screen.Host host, String d) {
        super(context, host);
        this.date = d == null ? Biz.today() : d;
        title("Admin Instructions");
        subtitle("Broadcast instructions to supervisors");

        headerAction("+ New Notice", new Runnable() {
            @Override
            public void run() {
                AdminInstructionScreen.this.openNewInstructionDialog();
            }
        });

        LinearLayout dateBar = Cards.cardFlat(context);
        dateBar.setOrientation(0);
        dateBar.setGravity(16);
        Ui.pad(dateBar, 12, 10, 12, 10);
        dateBar.addView(Cards.bold(context, "Instructions for: " + Biz.fmtDmy(this.date), 14.5f, R.color.primary_700), weight1());
        add(dateBar, 0);

        this.list = new LinearLayout(context);
        this.list.setOrientation(1);
        add(this.list, 6);

        this.empty = new Empty(context);
        add(this.empty, 0);

        refreshContent();
    }

    @Override
    public String headline() {
        return "Admin Instructions";
    }

    @Override
    public void refreshContent() {
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).dbRead();
        List<SupervisorMap.Instruction> instructions = SupervisorMap.getInstructions(db, this.date);

        this.list.removeAllViews();
        if (instructions.isEmpty()) {
            this.empty.show(R.drawable.ic_note, R.color.line_2, "No instructions posted", "Tap '+ New Notice' to give priority tasks or guidelines to supervisors.");
            return;
        }
        this.empty.hide();

        for (int i = 0; i < instructions.size(); i++) {
            final SupervisorMap.Instruction inst = instructions.get(i);
            View row = buildInstructionCard(context, inst);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 8.0f);
            this.list.addView(row, lp);
        }
    }

    private View buildInstructionCard(final Context context, final SupervisorMap.Instruction inst) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 14, 14, 14);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(16);
        card.addView(topRow, mw());

        TextView title = new TextView(context);
        title.setText(inst.title);
        Ui.size(title, 15.5f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(isSuper ? 0xFF1E1B18 : Ui.color(context, R.color.ink));
        topRow.addView(title, weight1());

        Badge badge = new Badge(context);
        if ("URGENT".equalsIgnoreCase(inst.priority)) {
            badge.setKind(4);
            badge.setText("🚨 URGENT");
        } else if ("HIGH".equalsIgnoreCase(inst.priority)) {
            badge.setKind(3);
            badge.setText("⚡ HIGH PRIORITY");
        } else {
            badge.setKind(0);
            badge.setText("📢 NOTICE");
        }
        topRow.addView(badge, wrap());

        TextView meta = new TextView(context);
        meta.setText("From: " + inst.senderName + " · " + Biz.fmtDmy(inst.businessDate));
        meta.setTextAppearance(context, R.style.Text_Meta);
        meta.setTextColor(isSuper ? 0xFF78716C : Ui.color(context, R.color.ink_3));
        card.addView(meta, top(context, 4));

        TextView msg = new TextView(context);
        msg.setText(inst.message);
        Ui.size(msg, 14.0f);
        msg.setTextColor(isSuper ? 0xFF44403C : Ui.color(context, R.color.ink_2));
        card.addView(msg, top(context, 8));

        View delBtn = Cards.link(context, "🗑️ Delete Notice", R.color.danger, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db = Core.get(context).db();
                SupervisorMap.deleteInstruction(db, inst.id);
                AdminInstructionScreen.this.host.toast("Instruction removed");
                AdminInstructionScreen.this.refreshContent();
            }
        });
        card.addView(delBtn, top(context, 6));

        return card;
    }

    private void openNewInstructionDialog() {
        final Context context = getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(1);
        Ui.pad(layout, 20, 14, 20, 14);

        final EditText titleInput = new EditText(context);
        titleInput.setHint("Instruction title (e.g. Focus on PMJJBY / Rekyc)");
        styleDialogInput(titleInput, context, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(Cards.label(context, "Title", R.style.Text_Label, R.color.ink_2));
        layout.addView(titleInput, top(context, 4));

        final EditText msgInput = new EditText(context);
        msgInput.setHint("Detailed guidelines for supervisors...");
        msgInput.setMinLines(3);
        msgInput.setSingleLine(false);
        msgInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        Ui.size(msgInput, 14.5f);
        msgInput.setTextColor(Ui.color(context, R.color.ink));
        msgInput.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 8.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(msgInput, 10, 10, 10, 10);
        layout.addView(Cards.label(context, "Message", R.style.Text_Label, R.color.ink_2), top(context, 10));
        layout.addView(msgInput, top(context, 4));

        layout.addView(Cards.label(context, "Priority Level", R.style.Text_Label, R.color.ink_2), top(context, 10));
        final Chips prioChips = new Chips(context);
        prioChips.set("Normal", "High", "Urgent");
        prioChips.allowNone(false);
        prioChips.select(0);
        layout.addView(prioChips, top(context, 4));

        new AlertDialog.Builder(context)
            .setTitle("Broadcast Instruction")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send Notice", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String title = titleInput.getText().toString().trim();
                    String msg = msgInput.getText().toString().trim();
                    if (title.isEmpty() || msg.isEmpty()) {
                        AdminInstructionScreen.this.host.toast("Title and message are required");
                        return;
                    }

                    int selPrio = prioChips.selected();
                    String prio = selPrio == 2 ? "URGENT" : (selPrio == 1 ? "HIGH" : "NORMAL");

                    User cur = Auth.currentUser(context);
                    String sender = cur != null ? cur.name : "Admin";

                    SQLiteDatabase db = Core.get(context).db();
                    SupervisorMap.addInstruction(db, AdminInstructionScreen.this.date, sender, title, msg, prio);
                    AdminInstructionScreen.this.host.toast("Instruction broadcasted");
                    AdminInstructionScreen.this.refreshContent();
                }
            }).show();
    }

    private void styleDialogInput(EditText editText, Context context, int inputType) {
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        Ui.size(editText, 14.5f);
        editText.setTextColor(Ui.color(context, R.color.ink));
        editText.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 8.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(editText, 10, 10, 10, 10);
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

    private static LinearLayout.LayoutParams top(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }
}
