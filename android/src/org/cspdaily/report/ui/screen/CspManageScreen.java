package org.cspdaily.report.ui.screen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Empty;

public class CspManageScreen extends Screen {
    private List<Csp> all = new ArrayList<>();
    private final TextView countLine;
    private final Empty empty;
    private final LinearLayout list;
    private EditText search;
    private String query = "";

    public CspManageScreen(Context context, final Screen.Host host) {
        super(context, host);
        title("Manage CSPs");
        subtitle("Add, edit details or delete CSPs");

        headerAction("Import CSV", new Runnable() {
            @Override
            public void run() {
                host.push(new CspImportScreen(CspManageScreen.this.getContext(), host));
            }
        });

        LinearLayout actionRow = new LinearLayout(context);
        actionRow.setOrientation(0);
        actionRow.setGravity(16);

        View addBtn = Cards.primaryButton(context, "+ Add CSP", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new CspEditScreen(CspManageScreen.this.getContext(), host, 0L));
            }
        });
        actionRow.addView(addBtn, weight1());

        View sp = new View(context);
        actionRow.addView(sp, new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));

        View importBtn = Cards.outlineButton(context, "Import CSV", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new CspImportScreen(CspManageScreen.this.getContext(), host));
            }
        });
        actionRow.addView(importBtn, weight1());

        add(actionRow, 0);
        add(searchView(context), 10);

        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(0);
        topBar.setGravity(16);
        Ui.pad(topBar, 2, 10, 2, 4);
        add(topBar, 0);

        this.countLine = Cards.label(context, "", R.style.Text_Section, R.color.ink_3);
        topBar.addView(this.countLine, new LinearLayout.LayoutParams(0, -2, 1.0f));

        this.list = new LinearLayout(context);
        this.list.setOrientation(1);
        add(this.list, 4);

        this.empty = new Empty(context);
        add(this.empty, 0);

        refreshContent();
    }

    @Override
    public String headline() {
        return Auth.isSuperAdmin(getContext()) ? "Executive Master CSP Register" : "Manage CSPs";
    }

    private LinearLayout searchView(Context context) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        if (isSuper) {
            linearLayout.setBackground(Ui.round(context, 0xF5FCF8F2, Ui.rdp(context, R.dimen.r_md), 0x66F59E0B, 1.0f));
        } else {
            linearLayout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), Ui.rdp(context, R.dimen.r_md), Ui.color(context, R.color.line_2), 1.0f));
        }
        Ui.pad(linearLayout, 12, 0, 6, 0);
        linearLayout.setMinimumHeight(Ui.dp(context, 46.0f));

        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(context, 18.0f), Ui.dp(context, 18.0f));
        layoutParams.rightMargin = Ui.dp(context, 8.0f);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.ic_search);
        imageView.setColorFilter(isSuper ? 0xFFD97706 : Ui.color(context, R.color.ink_4));
        linearLayout.addView(imageView);

        this.search = new EditText(context);
        this.search.setHint("Search name, code, mobile or branch");
        this.search.setBackground(null);
        Ui.size(this.search, 14.5f);
        this.search.setTextColor(isSuper ? 0xFF1E1B18 : Ui.color(context, R.color.ink));
        this.search.setHintTextColor(isSuper ? 0xFF78716C : Ui.color(context, R.color.ink_4));
        this.search.setSingleLine(true);
        this.search.setImeOptions(3);
        linearLayout.addView(this.search, new LinearLayout.LayoutParams(0, Ui.dp(context, 46.0f), 1.0f));

        final ImageView clearBtn = new ImageView(context);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(Ui.dp(context, 32.0f), Ui.dp(context, 32.0f));
        clearLp.rightMargin = Ui.dp(context, 4.0f);
        clearBtn.setLayoutParams(clearLp);
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setColorFilter(isSuper ? 0xFFD97706 : Ui.color(context, R.color.ink_4));
        clearBtn.setBackground(Ui.round(context, isSuper ? 0x26F59E0B : 0x15000000, 16.0f, 0, 0.0f));
        Ui.pad(clearBtn, 7, 7, 7, 7);
        clearBtn.setVisibility(8);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CspManageScreen.this.search.setText("");
            }
        });
        linearLayout.addView(clearBtn);

        this.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                CspManageScreen.this.query = editable.toString().trim().toLowerCase(Locale.US);
                clearBtn.setVisibility(CspManageScreen.this.query.isEmpty() ? 8 : 0);
                CspManageScreen.this.render();
            }
        });

        return linearLayout;
    }

    @Override
    public void refreshContent() {
        SQLiteDatabase db = Core.get(getContext()).dbRead();
        this.all = Csp.all(db, false, "name");
        render();
    }

    private void render() {
        Context context = getContext();
        boolean isSuper = Auth.isSuperAdmin(context);
        ArrayList<Csp> filtered = new ArrayList<>();
        for (Csp csp : this.all) {
            if (this.query.isEmpty() || (csp.name + " " + csp.code + " " + csp.mobile + " " + csp.branch + " " + csp.branchCode + " " + Biz.cspCodeSuffix(csp.code)).toLowerCase(Locale.US).contains(this.query)) {
                filtered.add(csp);
            }
        }

        Collections.sort(filtered, new Comparator<Csp>() {
            @Override
            public int compare(Csp a, Csp b) {
                String na = a.name == null ? "" : a.name.toLowerCase(Locale.US);
                String nb = b.name == null ? "" : b.name.toLowerCase(Locale.US);
                int cmp = na.compareTo(nb);
                if (cmp != 0) return cmp;
                return Long.compare(a.id, b.id);
            }
        });

        this.countLine.setText(filtered.size() + (filtered.size() == 1 ? " CSP in register" : " CSPs in register"));
        if (isSuper) {
            this.countLine.setTextColor(0xFFFEF08A);
        }
        this.list.removeAllViews();

        if (filtered.isEmpty()) {
            this.empty.show(R.drawable.ic_search, R.color.line_2, "No CSPs match", this.query.isEmpty() ? "Register is empty." : "Nothing for “" + this.query + "”.");
            return;
        }
        this.empty.hide();

        for (int i = 0; i < filtered.size(); i++) {
            final Csp csp = filtered.get(i);
            View rowView = buildCspManageCard(context, csp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 10.0f);
            this.list.addView(rowView, lp);
            Ui.enterRow(rowView, i);
        }
    }

    private View buildCspManageCard(final Context context, final Csp csp) {
        boolean isSuper = Auth.isSuperAdmin(context);
        LinearLayout card = isSuper ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 14, 14, 14);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(16);
        card.addView(topRow, mw());

        topRow.addView(new Avatar(context, 42).bind(csp.name, csp.code), new LinearLayout.LayoutParams(Ui.dp(context, 42.0f), Ui.dp(context, 42.0f)));

        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(1);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        infoLp.leftMargin = Ui.dp(context, 12.0f);
        infoLayout.setLayoutParams(infoLp);
        topRow.addView(infoLayout);

        TextView nameText = new TextView(context);
        nameText.setText(csp.name);
        Ui.size(nameText, 15.5f);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setTextColor(isSuper ? 0xFF1E1B18 : Ui.color(context, R.color.ink));
        infoLayout.addView(nameText);

        TextView metaText = new TextView(context);
        String meta = csp.code + (csp.branch.isEmpty() ? "" : " · " + csp.branch) + (csp.mobile.isEmpty() ? "" : " · " + csp.mobile);
        metaText.setText(meta);
        metaText.setTextAppearance(context, R.style.Text_Meta);
        metaText.setTextColor(isSuper ? 0xFF78716C : Ui.color(context, R.color.ink_3));
        infoLayout.addView(metaText);

        // Action buttons: Edit & Delete
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(0);
        actions.setGravity(16);
        LinearLayout.LayoutParams actLp = mw();
        actLp.topMargin = Ui.dp(context, 12.0f);
        card.addView(actions, actLp);

        View editBtn = isSuper ?
            Cards.goldButton(context, "✏️ Edit Details", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CspManageScreen.this.host.push(new CspEditScreen(context, CspManageScreen.this.host, csp.id));
                }
            }) :
            Cards.outlineButton(context, "Edit details", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CspManageScreen.this.host.push(new CspEditScreen(context, CspManageScreen.this.host, csp.id));
                }
            });
        actions.addView(editBtn, weight1());

        View spacer = new View(context);
        actions.addView(spacer, new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));

        View deleteBtn = Cards.link(context, "🗑️ Delete", R.color.danger, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CspManageScreen.this.confirmDelete(csp);
            }
        });
        actions.addView(deleteBtn, wrap());

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CspManageScreen.this.host.push(new CspEditScreen(context, CspManageScreen.this.host, csp.id));
            }
        });

        return card;
    }

    private void confirmDelete(final Csp csp) {
        final Context context = getContext();
        new AlertDialog.Builder(context)
            .setTitle("Delete " + csp.name + "?")
            .setMessage("Removes this CSP together with its reports, hourly snapshots, pending reasons and follow-up notes. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    long delId = csp.id;
                    Csp.delete(Core.get(context).db(), delId);
                    SupabaseClient.deleteAsync(context, "daily_report", "csp_id=eq." + delId);
                    SupabaseClient.deleteAsync(context, "hourly_report", "csp_id=eq." + delId);
                    SupabaseClient.deleteAsync(context, "pending_reason", "csp_id=eq." + delId);
                    SupabaseClient.deleteAsync(context, "followup_log", "csp_id=eq." + delId);
                    if (csp.code != null && !csp.code.isEmpty()) {
                        SupabaseClient.deleteAsync(context, "csp", "csp_code=eq." + csp.code);
                    }
                    CspManageScreen.this.host.toast("Deleted " + csp.name);
                    CspManageScreen.this.refreshContent();
                }
            }).show();
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
}
