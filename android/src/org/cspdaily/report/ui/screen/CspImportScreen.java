package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Csp;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.User;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Section;
import org.cspdaily.report.util.Files;

public class CspImportScreen extends Screen {
    private static final String SAMPLE_CSV = 
        "csp_code,csp_name,mobile_number,branch_name,branch_code,gender,daily_target\n" +
        "32250001,Amit Kumar,9876543210,Karnajora,1234,M,50\n" +
        "32250002,Pooja Sharma,9876543211,Raiganj,5678,F,40\n" +
        "32250003,Rahul Sen,9876543212,Malda,9012,M,60";

    private EditText csvTextInput;
    private TextView resultText;

    public CspImportScreen(final Context context, final Screen.Host host) {
        super(context, host);
        title("Import CSPs");
        subtitle("Bulk register CSPs via CSV");

        // 1. CSV Structure & Specifications Card
        Section secStruct = new Section(context);
        secStruct.set("CSV File Structure & Specifications");
        add(secStruct, 0);

        LinearLayout structCard = Cards.card(context);
        structCard.setOrientation(1);
        Ui.pad(structCard, 14, 14, 14, 14);

        TextView structDesc = new TextView(context);
        structDesc.setText("Upload or paste a comma-separated (.csv) file with the following column headers:");
        structDesc.setTextAppearance(context, R.style.Text_Body);
        structDesc.setTextColor(Ui.color(context, R.color.ink_2));
        structCard.addView(structDesc);

        LinearLayout specTable = Cards.cardFlat(context);
        specTable.setOrientation(1);
        Ui.pad(specTable, 10, 10, 10, 10);

        specTable.addView(specRow(context, "csp_code", "Required", "Unique 6-10 digit CSP code (e.g. 32250001)"));
        specTable.addView(specRow(context, "csp_name", "Required", "Full name of the CSP (e.g. Amit Kumar)"));
        specTable.addView(specRow(context, "mobile_number", "Optional", "10-digit contact number (e.g. 9876543210)"));
        specTable.addView(specRow(context, "branch_name", "Optional", "Link branch name (e.g. Karnajora)"));
        specTable.addView(specRow(context, "branch_code", "Optional", "4-6 digit branch code (e.g. 1234)"));
        specTable.addView(specRow(context, "gender", "Optional", "M or F (defaults to M)"));
        specTable.addView(specRow(context, "daily_target", "Optional", "Overall daily target number (e.g. 50)"));
        structCard.addView(specTable, top(context, 8));

        // Sample CSV Preview Box
        LinearLayout sampleBox = new LinearLayout(context);
        sampleBox.setOrientation(1);
        sampleBox.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), 8.0f, Ui.color(context, R.color.line), 1.0f));
        Ui.pad(sampleBox, 10, 10, 10, 10);

        TextView sampleTitle = new TextView(context);
        sampleTitle.setText("Sample CSV Content:");
        sampleTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        Ui.size(sampleTitle, 11.5f);
        sampleTitle.setTextColor(Ui.color(context, R.color.primary_700));
        sampleBox.addView(sampleTitle);

        TextView sampleText = new TextView(context);
        sampleText.setText(SAMPLE_CSV);
        sampleText.setTypeface(Typeface.MONOSPACE);
        Ui.size(sampleText, 11.0f);
        sampleText.setTextColor(Ui.color(context, R.color.ink));
        sampleBox.addView(sampleText, top(context, 4));

        structCard.addView(sampleBox, top(context, 10));

        // Copy template button
        View copyBtn = Cards.outlineButton(context, "📋 Copy Sample CSV Template", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("sample_csv", SAMPLE_CSV));
                    CspImportScreen.this.host.toast("Sample CSV template copied to clipboard");
                }
            }
        });
        structCard.addView(copyBtn, top(context, 10));
        add(structCard, 0);

        // 2. Import Action Section
        Section secImport = new Section(context);
        secImport.set("Import Data");
        add(secImport, 10);

        LinearLayout inputCard = Cards.card(context);
        inputCard.setOrientation(1);
        Ui.pad(inputCard, 14, 14, 14, 14);

        View pickFileBtn = Cards.outlineButton(context, "📁 Select CSV File from Device", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof Activity) {
                    Files.pickCsv((Activity) context);
                    CspImportScreen.this.host.toast("Select your .csv file");
                }
            }
        });
        inputCard.addView(pickFileBtn, mw());

        TextView orText = new TextView(context);
        orText.setText("— OR PASTE CSV TEXT BELOW —");
        orText.setGravity(17);
        orText.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(orText, 12.0f);
        orText.setTextColor(Ui.color(context, R.color.ink_4));
        inputCard.addView(orText, top(context, 12));

        this.csvTextInput = new EditText(context);
        this.csvTextInput.setHint("Paste CSV content with header row here...");
        this.csvTextInput.setMinLines(5);
        this.csvTextInput.setSingleLine(false);
        this.csvTextInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        this.csvTextInput.setTypeface(Typeface.MONOSPACE);
        Ui.size(this.csvTextInput, 12.5f);
        this.csvTextInput.setTextColor(Ui.color(context, R.color.ink));
        this.csvTextInput.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 10.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(this.csvTextInput, 12, 10, 12, 10);
        inputCard.addView(this.csvTextInput, top(context, 8));

        this.resultText = Cards.label(context, "", R.style.Text_Meta, R.color.primary_700);
        this.resultText.setVisibility(8);
        inputCard.addView(this.resultText, top(context, 8));

        View importBtn = Cards.primaryButton(context, "Import CSPs Now", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = CspImportScreen.this.csvTextInput.getText().toString().trim();
                if (text.isEmpty()) {
                    CspImportScreen.this.host.toast("Please paste CSV text or select a CSV file first");
                    return;
                }
                CspImportScreen.this.processCsv(text);
            }
        });
        inputCard.addView(importBtn, top(context, 12));
        add(inputCard, 0);
    }

    @Override
    public String headline() {
        return "Import CSPs";
    }

    public void setCsvContent(String content) {
        if (this.csvTextInput != null && content != null) {
            this.csvTextInput.setText(content);
            processCsv(content);
        }
    }

    public void processCsv(String raw) {
        Context context = getContext();
        SQLiteDatabase db = Core.get(context).db();
        User currentUser = Auth.currentUser(context);
        boolean isSupervisor = currentUser != null && currentUser.isSupervisor();

        String[] lines = raw.split("\r?\n");
        if (lines.length < 2) {
            this.resultText.setText("Error: CSV must contain at least 1 header line and 1 data line.");
            this.resultText.setTextColor(Ui.color(context, R.color.danger));
            this.resultText.setVisibility(0);
            return;
        }

        // Parse header
        String[] headers = parseCsvRow(lines[0]);
        Map<String, Integer> colMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String norm = headers[i].trim().toLowerCase(Locale.US).replace(" ", "_").replace("-", "_");
            colMap.put(norm, i);
        }

        int codeCol = findCol(colMap, "csp_code", "code", "cspcode", "id", "short_code");
        int nameCol = findCol(colMap, "csp_name", "name", "cspname", "full_name");
        int mobCol = findCol(colMap, "mobile_number", "mobile", "phone", "phone_number", "contact");
        int branchCol = findCol(colMap, "branch_name", "branch", "branchname", "link_branch");
        int branchCodeCol = findCol(colMap, "branch_code", "branchcode", "bcode");
        int genderCol = findCol(colMap, "gender", "sex");
        int targetCol = findCol(colMap, "daily_target", "target", "target_total", "dailytarget");

        if (codeCol < 0 || nameCol < 0) {
            this.resultText.setText("Error: Header must have at least 'csp_code' (or 'code') and 'csp_name' (or 'name') columns.");
            this.resultText.setTextColor(Ui.color(context, R.color.danger));
            this.resultText.setVisibility(0);
            return;
        }

        int importedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        List<Long> importedIds = new ArrayList<>();

        db.beginTransaction();
        try {
            for (int r = 1; r < lines.length; r++) {
                String line = lines[r].trim();
                if (line.isEmpty()) continue;
                String[] row = parseCsvRow(line);
                if (row.length <= Math.max(codeCol, nameCol)) {
                    skippedCount++;
                    continue;
                }

                String code = row[codeCol].trim();
                String name = row[nameCol].trim();
                if (code.isEmpty() || name.isEmpty()) {
                    skippedCount++;
                    continue;
                }

                String mob = (mobCol >= 0 && mobCol < row.length) ? row[mobCol].trim() : "";
                String branch = (branchCol >= 0 && branchCol < row.length) ? row[branchCol].trim() : "";
                String branchCode = (branchCodeCol >= 0 && branchCodeCol < row.length) ? row[branchCodeCol].trim() : "";
                String gender = (genderCol >= 0 && genderCol < row.length) ? row[genderCol].trim() : "M";
                if (gender.equalsIgnoreCase("Female") || gender.equalsIgnoreCase("F")) gender = "F";
                else gender = "M";

                int target = (targetCol >= 0 && targetCol < row.length) ? Biz.cleanCount(row[targetCol].trim()) : 0;

                Csp existing = Csp.byCode(db, code);
                Csp csp = existing != null ? existing : new Csp();
                csp.code = code;
                csp.name = name;
                csp.mobile = mob;
                csp.branch = branch;
                csp.branchCode = branchCode;
                csp.gender = gender;
                csp.status = Csp.ACTIVE;

                if (target > 0 && (csp.targets == null || csp.targets.isEmpty())) {
                    csp.targets = new LinkedHashMap<>();
                    List<Params.P> params = Params.all(db);
                    int perP = (int) Math.ceil((double) target / Math.max(1, params.size()));
                    for (Params.P p : params) {
                        csp.targets.put(p.key, perP);
                    }
                }

                long cspId;
                if (existing != null) {
                    Csp.update(db, csp);
                    cspId = existing.id;
                    updatedCount++;
                } else {
                    cspId = Csp.insert(context, db, csp);
                    importedCount++;
                }

                if (cspId > 0) {
                    importedIds.add(cspId);
                    if (isSupervisor) {
                        SupervisorMap.mapCspToSupervisor(db, currentUser.id, cspId);
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (Throwable th) {
            this.resultText.setText("Import failed: " + th.getMessage());
            this.resultText.setTextColor(Ui.color(context, R.color.danger));
            this.resultText.setVisibility(0);
            return;
        } finally {
            try { db.endTransaction(); } catch (Throwable ignored) {}
        }

        String msg = "Successfully processed: " + importedCount + " added, " + updatedCount + " updated, " + skippedCount + " skipped.";
        if (isSupervisor) {
            msg += "\nMapped to your supervisor account.";
        }
        this.resultText.setText(msg);
        this.resultText.setTextColor(Ui.color(context, R.color.ok));
        this.resultText.setVisibility(0);
        this.host.toast("Import complete: " + (importedCount + updatedCount) + " CSPs processed");
    }

    private static int findCol(Map<String, Integer> colMap, String... keys) {
        for (String k : keys) {
            Integer idx = colMap.get(k);
            if (idx != null) return idx.intValue();
        }
        return -1;
    }

    private static String[] parseCsvRow(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"') {
                inQuotes = !inQuotes;
            } else if ((ch == ',' || ch == '\t' || ch == ';') && !inQuotes) {
                list.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        list.add(sb.toString().trim());
        return list.toArray(new String[0]);
    }

    private View specRow(Context context, String colName, String tag, String desc) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(0);
        row.setGravity(16);
        Ui.pad(row, 0, 4, 0, 4);

        TextView col = new TextView(context);
        col.setText(colName);
        col.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        Ui.size(col, 13.0f);
        col.setTextColor(Ui.color(context, R.color.ink));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(context, 110.0f), -2);
        row.addView(col, clp);

        Badge badge = new Badge(context);
        badge.setKind("Required".equalsIgnoreCase(tag) ? 4 : 0);
        badge.setText(tag);
        row.addView(badge, wrap());

        TextView d = new TextView(context);
        d.setText(desc);
        d.setTextAppearance(context, R.style.Text_Meta);
        d.setTextColor(Ui.color(context, R.color.ink_3));
        LinearLayout.LayoutParams dlp = weight1();
        dlp.leftMargin = Ui.dp(context, 8.0f);
        row.addView(d, dlp);

        return row;
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
