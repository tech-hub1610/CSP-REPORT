package org.cspdaily.report.ui.screen;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Section;

public class SupabaseSyncScreen extends Screen {
    private EditText urlInput;
    private EditText keyInput;
    private TextView statusBadge;
    private TextView lastSyncText;
    private TextView logOutput;
    private ProgressBar progressBar;

    public SupabaseSyncScreen(final Context context, final Screen.Host host) {
        super(context, host);
        title("Supabase Cloud Database");
        subtitle("Cloud sync and centralized backup");

        headerAction("Test Connection", new Runnable() {
            @Override
            public void run() {
                testConnection();
            }
        });

        // 1. Connection Card
        Section secConn = new Section(context);
        secConn.set("Connection Settings");
        add(secConn, 0);

        LinearLayout cardConn = Cards.card(context);
        cardConn.setOrientation(1);
        Ui.pad(cardConn, 14, 14, 14, 14);

        TextView urlLabel = new TextView(context);
        urlLabel.setText("Supabase Project URL");
        urlLabel.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(urlLabel, 13.0f);
        urlLabel.setTextColor(Ui.color(context, R.color.ink));
        cardConn.addView(urlLabel);

        this.urlInput = new EditText(context);
        this.urlInput.setText(SupabaseClient.getProjectUrl(context));
        this.urlInput.setSingleLine(true);
        this.urlInput.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), Ui.rdp(context, R.dimen.r_md), Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(this.urlInput, 10, 8, 10, 8);
        cardConn.addView(this.urlInput, top(context, 4));

        TextView keyLabel = new TextView(context);
        keyLabel.setText("Publishable / Anon API Key");
        keyLabel.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(keyLabel, 13.0f);
        keyLabel.setTextColor(Ui.color(context, R.color.ink));
        cardConn.addView(keyLabel, top(context, 10));

        this.keyInput = new EditText(context);
        this.keyInput.setText(SupabaseClient.getPublishableKey(context));
        this.keyInput.setSingleLine(true);
        this.keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        this.keyInput.setBackground(Ui.round(context, Ui.color(context, R.color.paper_2), Ui.rdp(context, R.dimen.r_md), Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(this.keyInput, 10, 8, 10, 8);
        cardConn.addView(this.keyInput, top(context, 4));

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(0);
        btnRow.setGravity(16);

        View saveBtn = Cards.primaryButton(context, "Save Config", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfig();
            }
        });
        btnRow.addView(saveBtn, weight1());

        View sp1 = new View(context);
        btnRow.addView(sp1, new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));

        View testBtn = Cards.outlineButton(context, "Test Connection", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });
        btnRow.addView(testBtn, weight1());
        cardConn.addView(btnRow, top(context, 12));

        add(cardConn, 0);

        // 2. Sync Actions Card
        Section secSync = new Section(context);
        secSync.set("Cloud Synchronization");
        add(secSync, 6);

        LinearLayout cardSync = Cards.card(context);
        cardSync.setOrientation(1);
        Ui.pad(cardSync, 14, 14, 14, 14);

        SQLiteDatabase db = Core.get(context).dbRead();
        String lastSync = Settings.get(db, Settings.SUPABASE_LAST_SYNC, "Never");
        this.lastSyncText = new TextView(context);
        this.lastSyncText.setText("Last Synced: " + (lastSync.isEmpty() ? "Never" : lastSync));
        this.lastSyncText.setTextAppearance(context, R.style.Text_Meta);
        this.lastSyncText.setTextColor(Ui.color(context, R.color.ink_3));
        cardSync.addView(this.lastSyncText);

        this.progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(8);
        cardSync.addView(this.progressBar, top(context, 8));

        this.logOutput = new TextView(context);
        this.logOutput.setText("Ready to sync data with Supabase.");
        this.logOutput.setTextAppearance(context, R.style.Text_Meta);
        this.logOutput.setTextColor(Ui.color(context, R.color.ink_2));
        cardSync.addView(this.logOutput, top(context, 6));

        View syncAllBtn = Cards.primaryButton(context, "Sync Now (Push & Pull)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSyncAll();
            }
        });
        cardSync.addView(syncAllBtn, top(context, 10));

        LinearLayout subActions = new LinearLayout(context);
        subActions.setOrientation(0);
        subActions.setGravity(16);

        View pushBtn = Cards.outlineButton(context, "Push Local -> Cloud", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPush();
            }
        });
        subActions.addView(pushBtn, weight1());

        View sp2 = new View(context);
        subActions.addView(sp2, new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));

        View pullBtn = Cards.outlineButton(context, "Pull Cloud -> Local", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPull();
            }
        });
        subActions.addView(pullBtn, weight1());
        cardSync.addView(subActions, top(context, 8));

        add(cardSync, 0);

        // 3. Database Schema Setup Card
        Section secSchema = new Section(context);
        secSchema.set("Supabase SQL Schema Script");
        add(secSchema, 6);

        LinearLayout cardSchema = Cards.card(context);
        cardSchema.setOrientation(1);
        Ui.pad(cardSchema, 14, 14, 14, 14);

        TextView schemaDesc = new TextView(context);
        schemaDesc.setText("To initialize the PostgreSQL tables in your Supabase project, open your Supabase Dashboard -> SQL Editor and run the script.");
        schemaDesc.setTextAppearance(context, R.style.Text_Body);
        schemaDesc.setTextColor(Ui.color(context, R.color.ink_2));
        cardSchema.addView(schemaDesc);

        View copySchemaBtn = Cards.primaryButton(context, "Copy Complete SQL Schema to Clipboard", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copySqlSchema();
            }
        });
        cardSchema.addView(copySchemaBtn, top(context, 10));

        View viewSchemaBtn = Cards.outlineButton(context, "View SQL Script Details", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSqlDialog();
            }
        });
        cardSchema.addView(viewSchemaBtn, top(context, 6));

        add(cardSchema, 0);
    }

    @Override
    public String headline() {
        return "Supabase Database";
    }

    private void saveConfig() {
        Context context = getContext();
        String url = this.urlInput.getText().toString().trim();
        String key = this.keyInput.getText().toString().trim();
        SupabaseClient.setProjectUrl(context, url);
        SupabaseClient.setPublishableKey(context, key);
        this.host.toast("Supabase settings saved.");
    }

    private void testConnection() {
        saveConfig();
        this.progressBar.setVisibility(0);
        this.logOutput.setText("Testing connection to Supabase...");
        SupabaseClient.testConnection(getContext(), new SupabaseClient.Callback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(8);
                logOutput.setText("Status: " + result);
                host.toast("Supabase: " + result);
            }

            @Override
            public void onError(String message, Throwable error) {
                progressBar.setVisibility(8);
                logOutput.setText("Error: " + message);
                host.toast("Connection error: " + message);
            }
        });
    }

    private void doSyncAll() {
        saveConfig();
        this.progressBar.setVisibility(0);
        this.logOutput.setText("Performing full bidirectional synchronization...");
        SupabaseClient.syncAll(getContext(), new SupabaseClient.Callback<SupabaseClient.SyncResult>() {
            @Override
            public void onSuccess(SupabaseClient.SyncResult result) {
                progressBar.setVisibility(8);
                logOutput.setText(result.message);
                SQLiteDatabase db = Core.get(getContext()).dbRead();
                lastSyncText.setText("Last Synced: " + Settings.get(db, Settings.SUPABASE_LAST_SYNC, "Just now"));
                host.toast("Sync complete!");
            }

            @Override
            public void onError(String message, Throwable error) {
                progressBar.setVisibility(8);
                logOutput.setText("Sync Failed: " + message);
                host.toast("Sync error: " + message);
            }
        });
    }

    private void doPush() {
        saveConfig();
        this.progressBar.setVisibility(0);
        this.logOutput.setText("Pushing local data to Supabase...");
        SupabaseClient.pushAll(getContext(), new SupabaseClient.Callback<SupabaseClient.SyncResult>() {
            @Override
            public void onSuccess(SupabaseClient.SyncResult result) {
                progressBar.setVisibility(8);
                logOutput.setText(result.message);
                host.toast("Push complete!");
            }

            @Override
            public void onError(String message, Throwable error) {
                progressBar.setVisibility(8);
                logOutput.setText("Push Failed: " + message);
                host.toast("Push error: " + message);
            }
        });
    }

    private void doPull() {
        saveConfig();
        this.progressBar.setVisibility(0);
        this.logOutput.setText("Pulling cloud data from Supabase...");
        SupabaseClient.pullAll(getContext(), new SupabaseClient.Callback<SupabaseClient.SyncResult>() {
            @Override
            public void onSuccess(SupabaseClient.SyncResult result) {
                progressBar.setVisibility(8);
                logOutput.setText(result.message);
                host.toast("Pull complete!");
            }

            @Override
            public void onError(String message, Throwable error) {
                progressBar.setVisibility(8);
                logOutput.setText("Pull Failed: " + message);
                host.toast("Pull error: " + message);
            }
        });
    }

    private void copySqlSchema() {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService("clipboard");
        cm.setPrimaryClip(ClipData.newPlainText("Supabase Schema SQL", SupabaseClient.getSupabaseSqlSchema()));
        this.host.toast("SQL Schema copied to clipboard.");
    }

    private void showSqlDialog() {
        new AlertDialog.Builder(getContext())
            .setTitle("Supabase SQL Schema")
            .setMessage(SupabaseClient.getSupabaseSqlSchema())
            .setPositiveButton("Copy", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    copySqlSchema();
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private static LinearLayout.LayoutParams top(Context context, int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dp(context, dp);
        return lp;
    }

    private static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }
}
