package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.content.Context;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Day;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.domain.CspDay;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.CspCard;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.util.PhoneActions;

/* loaded from: classes.dex */
public class CspListScreen extends Screen {
    private static final String[] FILTERS = {"All", "Pending", "Reported", "NIL", "Overdue", "Not Contacted", "Inactive"};
    private List<CspDay> all;
    private final TextView countLine;
    private final Empty empty;
    private String filter;
    private final Chips filters;
    private String lastDate;
    private final LinearLayout list;
    private final int mode;
    private String query;
    private EditText search;
    private String sort;
    private final Badge sortBadge;

    public CspListScreen(Context context, Screen.Host host) {
        this(context, host, 0);
    }

    public CspListScreen(Context context, final Screen.Host host, int i) {
        super(context, host);
        String str;
        this.query = "";
        this.filter = "All";
        this.all = new ArrayList();
        this.mode = i;
        this.sort = UiState.cspSort(context);
        title(i == 0 ? "CSP directory" : "Choose a CSP");
        if (i == 0) {
            str = "Banking correspondents and targets";
        } else {
            str = i == 1 ? "Then enter today's figures" : "Then log the contact";
        }
        subtitle(str);
        if (i == 0) {
            headerAction("Import CSV", new Runnable() {
                @Override
                public void run() {
                    host.push(new CspImportScreen(context, host));
                }
            });
        }
        add(search(context), 0);
        this.filters = new Chips(context);
        this.filters.set(FILTERS);
        this.filters.allowNone(false);
        this.filters.onChange(new Chips.OnPick() { // from class: org.cspdaily.report.ui.screen.CspListScreen.1
            @Override // org.cspdaily.report.ui.widget.Chips.OnPick
            public void pick(int i2, String str2) {
                CspListScreen.this.filter = str2;
                UiState.setCspFilter(CspListScreen.this.getContext(), str2);
                CspListScreen.this.render();
            }
        });
        add(this.filters, 10);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        Ui.pad(linearLayout, 2, 12, 2, 2);
        add(linearLayout, 0);
        this.countLine = Cards.label(context, "", R.style.Text_Section, R.color.ink_3);
        linearLayout.addView(this.countLine, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.sortBadge = new Badge(context);
        this.sortBadge.setKind(0);
        this.sortBadge.setOnClickListener(new View.OnClickListener() { // from class: org.cspdaily.report.ui.screen.CspListScreen.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CspListScreen.this.cycleSort();
            }
        });
        linearLayout.addView(this.sortBadge, wrap());
        this.list = new LinearLayout(context);
        this.list.setOrientation(1);
        add(this.list, 4);
        this.empty = new Empty(context);
        add(this.empty, 0);
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return this.mode == 0 ? "CSP directory" : "Choose a CSP";
    }

    private LinearLayout search(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackground(Ui.round(context, Ui.color(context, R.color.paper), Ui.rdp(context, R.dimen.r_md), Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(linearLayout, 12, 0, 6, 0);
        linearLayout.setMinimumHeight(Ui.dp(context, 46.0f));
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(context, 18.0f), Ui.dp(context, 18.0f));
        layoutParams.rightMargin = Ui.dp(context, 8.0f);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.ic_search);
        imageView.setColorFilter(Ui.color(context, R.color.ink_4));
        linearLayout.addView(imageView);
        this.search = new EditText(context);
        this.search.setHint("Search name, code, mobile or branch");
        this.search.setBackground(null);
        Ui.size(this.search, 14.5f);
        this.search.setTextColor(Ui.color(context, R.color.ink));
        this.search.setHintTextColor(Ui.color(context, R.color.ink_4));
        this.search.setSingleLine(true);
        this.search.setImeOptions(3);
        linearLayout.addView(this.search, new LinearLayout.LayoutParams(0, Ui.dp(context, 46.0f), 1.0f));

        final ImageView clearBtn = new ImageView(context);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(Ui.dp(context, 32.0f), Ui.dp(context, 32.0f));
        clearLp.rightMargin = Ui.dp(context, 4.0f);
        clearBtn.setLayoutParams(clearLp);
        clearBtn.setImageResource(R.drawable.ic_close);
        clearBtn.setColorFilter(Ui.color(context, R.color.ink_4));
        clearBtn.setBackground(Ui.round(context, 0x15000000, 16.0f, 0, 0.0f));
        Ui.pad(clearBtn, 7, 7, 7, 7);
        clearBtn.setVisibility(this.query != null && !this.query.isEmpty() ? 0 : 8);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CspListScreen.this.search.setText("");
            }
        });
        linearLayout.addView(clearBtn);

        this.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                CspListScreen.this.query = editable.toString().trim().toLowerCase(Locale.US);
                clearBtn.setVisibility(CspListScreen.this.query.isEmpty() ? 8 : 0);
                UiState.setCspQuery(CspListScreen.this.getContext(), CspListScreen.this.query);
                CspListScreen.this.render();
            }
        });
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cycleSort() {
        if ("pdf".equals(this.sort)) {
            this.sort = "name";
        } else if ("name".equals(this.sort)) {
            this.sort = "code";
        } else if ("code".equals(this.sort)) {
            this.sort = "branch";
        } else {
            this.sort = "pdf";
        }
        UiState.setCspSort(getContext(), this.sort);
        render();
    }

    private String sortWord() {
        if ("name".equals(this.sort)) {
            return "Sort: name";
        }
        if ("code".equals(this.sort)) {
            return "Sort: code";
        }
        if ("branch".equals(this.sort)) {
            return "Sort: branch";
        }
        return "Sort: register";
    }

    @Override // org.cspdaily.report.ui.Screen
    public void refreshContent() {
        SQLiteDatabase dbRead = Core.get(getContext()).dbRead();
        String date = UiState.date(getContext());
        List<Csp> all = Csp.all(dbRead, false, "pdf");
        List<CspDay> dayData = Day.dayData(dbRead, date, false);
        HashMap hashMap = new HashMap();
        for (CspDay cspDay : dayData) {
            hashMap.put(Long.valueOf(cspDay.id), cspDay);
        }

        User currentUser = Auth.currentUser(getContext());
        Set<Long> mappedIds = null;
        if (currentUser != null && currentUser.isSupervisor()) {
            mappedIds = SupervisorMap.getCspIdsForSupervisor(dbRead, currentUser.id);
        }

        this.all = new ArrayList();
        for (Csp csp : all) {
            if (mappedIds != null && !mappedIds.isEmpty() && !mappedIds.contains(csp.id)) {
                continue;
            }
            CspDay cspDay2 = (CspDay) hashMap.get(Long.valueOf(csp.id));
            if (cspDay2 == null) {
                cspDay2 = new CspDay();
                cspDay2.id = csp.id;
                cspDay2.code = csp.code;
                cspDay2.name = csp.name;
                cspDay2.gender = csp.gender;
                cspDay2.mobile = csp.mobile;
                cspDay2.branch = csp.branch;
                cspDay2.branchCode = csp.branchCode;
                cspDay2.status = csp.status;
                cspDay2.notes = csp.notes;
                cspDay2.targets = csp.targets;
                cspDay2.dailyTarget = csp.targetTotal();
                cspDay2.followupStatus = Day.NOT_CONTACTED;
            }
            this.all.add(cspDay2);
        }
        render();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void render() {
        Context context = getContext();
        this.sortBadge.setText(sortWord());
        ArrayList<CspDay> arrayList = new ArrayList<>();
        for (CspDay cspDay : this.all) {
            if (passes(cspDay)) {
                arrayList.add(cspDay);
            }
        }
        sortInto(arrayList);
        this.countLine.setText(arrayList.size() + (arrayList.size() == 1 ? " CSP" : " CSPs") + ("All".equals(this.filter) ? "" : " · " + this.filter));
        this.list.removeAllViews();
        if (arrayList.isEmpty()) {
            this.empty.show(R.drawable.ic_search, R.color.line_2, "No CSPs match", this.query.isEmpty() ? "Try a different filter." : "Nothing for “" + this.query + "”.");
            return;
        }
        this.empty.hide();
        CspCard.Sink sink = new CspCard.Sink() {
            @Override
            public void open(CspDay cspDay2) {
                CspListScreen.this.onOpen(cspDay2);
            }

            @Override
            public void call(CspDay cspDay2) {
                PhoneActions.call(CspListScreen.this.getContext(), cspDay2);
            }

            @Override
            public void whatsapp(CspDay cspDay2) {
                PhoneActions.whatsapp(CspListScreen.this.getContext(), cspDay2);
            }

            @Override
            public void followup(CspDay cspDay2) {
                FollowupSheet.open((Activity) CspListScreen.this.getContext(), CspListScreen.this.host, cspDay2, UiState.date(CspListScreen.this.getContext()));
            }

            @Override
            public void report(CspDay cspDay2) {
                ReportEntrySheet.open((Activity) CspListScreen.this.getContext(), CspListScreen.this.host, cspDay2, UiState.date(CspListScreen.this.getContext()));
            }
        };
        String date = UiState.date(context);
        int i = 0;
        while (i < arrayList.size()) {
            CspCard cspCard = new CspCard(context);
            cspCard.setSink(sink);
            cspCard.bind((CspDay) arrayList.get(i), this.mode == 0);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.topMargin = Ui.dp(context, i == 0 ? 0.0f : 8.0f);
            this.list.addView(cspCard, layoutParams);
            Ui.enterRow(cspCard, i);
            i++;
        }
        this.lastDate = date;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onOpen(CspDay cspDay) {
        if (this.mode == 1) {
            ReportEntrySheet.open((Activity) getContext(), this.host, cspDay, UiState.date(getContext()));
        } else if (this.mode == 2) {
            FollowupSheet.open((Activity) getContext(), this.host, cspDay, UiState.date(getContext()));
        } else {
            this.host.push(new CspProfileScreen(getContext(), this.host, cspDay.id, UiState.date(getContext())));
        }
    }

    private boolean passes(CspDay cspDay) {
        if (!this.query.isEmpty() && !(cspDay.name + " " + cspDay.code + " " + cspDay.mobile + " " + cspDay.branch + " " + cspDay.branchCode + " " + Biz.cspCodeSuffix(cspDay.code)).toLowerCase(Locale.US).contains(this.query)) {
            return false;
        }
        if ("Inactive".equalsIgnoreCase(this.filter)) {
            return !Csp.ACTIVE.equals(cspDay.status);
        }
        if (!Csp.ACTIVE.equals(cspDay.status)) {
            return false;
        }
        if ("Reported".equalsIgnoreCase(this.filter)) {
            return cspDay.reported && !cspDay.isNil;
        }
        if ("NIL".equalsIgnoreCase(this.filter) || "Nil".equalsIgnoreCase(this.filter)) {
            return cspDay.isNil;
        }
        if ("Pending".equalsIgnoreCase(this.filter)) {
            return cspDay.isPending();
        }
        if ("Overdue".equalsIgnoreCase(this.filter) || "Long pending".equalsIgnoreCase(this.filter)) {
            return cspDay.isPending() && cspDay.pendingDays >= 3;
        }
        if ("Not Contacted".equalsIgnoreCase(this.filter)) {
            return cspDay.isPending() && Day.NOT_CONTACTED.equals(cspDay.followupStatus);
        }
        return true;
    }

    private void sortInto(List<CspDay> list) {
        final boolean equalsCode = "code".equals(this.sort);
        final boolean equalsBranch = "branch".equals(this.sort);
        Collections.sort(list, new Comparator<CspDay>() { // from class: org.cspdaily.report.ui.screen.CspListScreen.7
            @Override // java.util.Comparator
            public int compare(CspDay cspDay, CspDay cspDay2) {
                if (equalsCode) {
                    int c = CspListScreen.ci(cspDay.code).compareTo(CspListScreen.ci(cspDay2.code));
                    if (c != 0) return c;
                } else if (equalsBranch) {
                    int b = CspListScreen.ci(cspDay.branch).compareTo(CspListScreen.ci(cspDay2.branch));
                    if (b != 0) return b;
                }
                int n = CspListScreen.ci(cspDay.name).compareTo(CspListScreen.ci(cspDay2.name));
                if (n != 0) return n;
                return Long.compare(cspDay.id, cspDay2.id);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String ci(String str) {
        return str == null ? "" : str.toLowerCase(Locale.US);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }
}
