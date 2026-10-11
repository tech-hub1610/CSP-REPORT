package org.cspdaily.report.ui.screen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.data.SupervisorMap;
import org.cspdaily.report.data.User;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Chips;
import org.cspdaily.report.ui.widget.Empty;

public class SupervisorManageScreen extends Screen {
    private List<User> allUsers = new ArrayList<>();
    private final TextView countLine;
    private final Empty empty;
    private final LinearLayout list;
    private EditText search;
    private String query = "";
    private String roleFilter = "ALL";

    public SupervisorManageScreen(final Context context, final Screen.Host host) {
        super(context, host);
        title("Users & Supervisors");
        subtitle("Manage login accounts & staff");

        headerAction("+ Add User", new Runnable() {
            @Override
            public void run() {
                SupervisorManageScreen.this.openEditUserDialog(null);
            }
        });

        add(searchView(context), 0);

        // Role filter chips
        Chips chips = new Chips(context);
        chips.set("All", "Supervisors", "Admins", "Superadmins");
        chips.allowNone(false);
        chips.select(0);
        chips.onChange(new Chips.OnPick() {
            @Override
            public void pick(int i, String str) {
                if ("Supervisors".equals(str)) roleFilter = User.ROLE_SUPERVISOR;
                else if ("Admins".equals(str)) roleFilter = User.ROLE_ADMIN;
                else if ("Superadmins".equals(str)) roleFilter = User.ROLE_SUPERADMIN;
                else roleFilter = "ALL";
                render();
            }
        });
        add(chips, 8);

        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(0);
        topBar.setGravity(16);
        Ui.pad(topBar, 2, 8, 2, 4);
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
        return Auth.isSuperAdmin(getContext()) ? "Executive User & Staff Roster" : "Users & Supervisors";
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
        this.search.setHint("Search name, username, mobile, role");
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
                SupervisorManageScreen.this.search.setText("");
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
                SupervisorManageScreen.this.query = editable.toString().trim().toLowerCase(Locale.US);
                clearBtn.setVisibility(SupervisorManageScreen.this.query.isEmpty() ? 8 : 0);
                SupervisorManageScreen.this.render();
            }
        });

        return linearLayout;
    }

    @Override
    public void refreshContent() {
        SQLiteDatabase db = Core.get(getContext()).dbRead();
        this.allUsers = User.all(db, null);
        render();
    }

    private void render() {
        Context context = getContext();
        boolean isSuper = Auth.isSuperAdmin(context);
        SQLiteDatabase db = Core.get(context).dbRead();
        ArrayList<User> filtered = new ArrayList<>();

        for (User u : this.allUsers) {
            if (!"ALL".equalsIgnoreCase(this.roleFilter) && !this.roleFilter.equalsIgnoreCase(u.role)) {
                continue;
            }
            if (!this.query.isEmpty()) {
                String full = (u.name + " " + u.username + " " + u.mobile + " " + u.role).toLowerCase(Locale.US);
                if (!full.contains(this.query)) continue;
            }
            filtered.add(u);
        }

        this.countLine.setText(filtered.size() + (filtered.size() == 1 ? " user" : " users"));
        if (isSuper) {
            this.countLine.setTextColor(0xFFFEF08A);
        }
        this.list.removeAllViews();

        if (filtered.isEmpty()) {
            this.empty.show(R.drawable.ic_person, R.color.line_2, "No users found", "Tap '+ Add User' to create supervisor or admin accounts.");
            return;
        }
        this.empty.hide();

        for (int i = 0; i < filtered.size(); i++) {
            final User u = filtered.get(i);
            int mappedCspCount = SupervisorMap.getCspIdsForSupervisor(db, u.id).size();
            View row = buildUserCard(context, u, mappedCspCount);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(context, i == 0 ? 0.0f : 10.0f);
            this.list.addView(row, lp);
            Ui.enterRow(row, i);
        }
    }

    private View buildUserCard(final Context context, final User user, int mappedCsps) {
        boolean isSuperAdminUser = Auth.isSuperAdmin(context);
        LinearLayout card = isSuperAdminUser ? Cards.obsidianGold(context) : Cards.card(context);
        card.setOrientation(1);
        Ui.pad(card, 14, 14, 14, 14);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(0);
        topRow.setGravity(16);
        card.addView(topRow, mw());

        topRow.addView(new Avatar(context, 42).bind(user.name, user.username), new LinearLayout.LayoutParams(Ui.dp(context, 42.0f), Ui.dp(context, 42.0f)));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(1);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        infoLp.leftMargin = Ui.dp(context, 12.0f);
        info.setLayoutParams(infoLp);
        topRow.addView(info);

        TextView nameText = new TextView(context);
        nameText.setText(user.name);
        Ui.size(nameText, 15.5f);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setTextColor(isSuperAdminUser ? 0xFF1E1B18 : Ui.color(context, R.color.ink));
        info.addView(nameText);

        TextView userMeta = new TextView(context);
        String metaStr = "@" + user.username + (user.mobile.isEmpty() ? "" : " · " + user.mobile);
        userMeta.setText(metaStr);
        userMeta.setTextAppearance(context, R.style.Text_Meta);
        userMeta.setTextColor(isSuperAdminUser ? 0xFF78716C : Ui.color(context, R.color.ink_3));
        info.addView(userMeta);

        // Role badge
        Badge badge = new Badge(context);
        if (user.isSuperAdmin()) {
            badge.setKind(4);
            badge.setText("👑 Superadmin");
        } else if (User.ROLE_ADMIN.equalsIgnoreCase(user.role)) {
            badge.setKind(1);
            badge.setText("⚡ Admin");
        } else {
            badge.setKind(0);
            badge.setText("👔 Supervisor (" + mappedCsps + " CSPs)");
        }
        topRow.addView(badge, wrap());

        // Actions: Edit / Map CSPs / Delete
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(0);
        actions.setGravity(16);
        LinearLayout.LayoutParams actLp = mw();
        actLp.topMargin = Ui.dp(context, 12.0f);
        card.addView(actions, actLp);

        if (user.isSupervisor()) {
            View mapBtn = isSuperAdminUser ? 
                Cards.goldOutlineButton(context, "🗺️ Map CSPs (" + mappedCsps + ")", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SupervisorManageScreen.this.host.push(new CspSupervisorMappingScreen(context, SupervisorManageScreen.this.host, user.id));
                    }
                }) :
                Cards.outlineButton(context, "Map CSPs (" + mappedCsps + ")", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SupervisorManageScreen.this.host.push(new CspSupervisorMappingScreen(context, SupervisorManageScreen.this.host, user.id));
                    }
                });
            actions.addView(mapBtn, weight1());

            View sp = new View(context);
            actions.addView(sp, new LinearLayout.LayoutParams(Ui.dp(context, 6.0f), 1));
        }

        View editBtn = isSuperAdminUser ?
            Cards.goldButton(context, "✏️ Edit", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SupervisorManageScreen.this.openEditUserDialog(user);
                }
            }) :
            Cards.outlineButton(context, "Edit", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SupervisorManageScreen.this.openEditUserDialog(user);
                }
            });
        actions.addView(editBtn, user.isSupervisor() ? wrap() : weight1());

        if (!"admin".equalsIgnoreCase(user.username)) {
            View sp2 = new View(context);
            actions.addView(sp2, new LinearLayout.LayoutParams(Ui.dp(context, 8.0f), 1));

            View delBtn = Cards.link(context, "🗑️ Delete", R.color.danger, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SupervisorManageScreen.this.confirmDelete(user);
                }
            });
            actions.addView(delBtn, wrap());
        }

        return card;
    }

    private void openEditUserDialog(final User existing) {
        final Context context = getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(1);
        Ui.pad(layout, 20, 14, 20, 14);

        final EditText nameInput = new EditText(context);
        nameInput.setHint("Full Name");
        if (existing != null) nameInput.setText(existing.name);
        styleDialogInput(nameInput, context, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(Cards.label(context, "Full Name", R.style.Text_Label, R.color.ink_2));
        layout.addView(nameInput, top(context, 4));

        final EditText usernameInput = new EditText(context);
        usernameInput.setHint("Username (for login)");
        if (existing != null) {
            usernameInput.setText(existing.username);
            if ("admin".equalsIgnoreCase(existing.username)) usernameInput.setEnabled(false);
        }
        styleDialogInput(usernameInput, context, InputType.TYPE_CLASS_TEXT);
        layout.addView(Cards.label(context, "Username", R.style.Text_Label, R.color.ink_2), top(context, 10));
        layout.addView(usernameInput, top(context, 4));

        final EditText passwordInput = new EditText(context);
        passwordInput.setHint(existing == null ? "Password" : "Leave blank to keep current password");
        styleDialogInput(passwordInput, context, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(Cards.label(context, "Password", R.style.Text_Label, R.color.ink_2), top(context, 10));
        layout.addView(passwordInput, top(context, 4));

        final EditText mobileInput = new EditText(context);
        mobileInput.setHint("Mobile (optional)");
        if (existing != null) mobileInput.setText(existing.mobile);
        styleDialogInput(mobileInput, context, InputType.TYPE_CLASS_PHONE);
        layout.addView(Cards.label(context, "Mobile Number", R.style.Text_Label, R.color.ink_2), top(context, 10));
        layout.addView(mobileInput, top(context, 4));

        layout.addView(Cards.label(context, "Account Role", R.style.Text_Label, R.color.ink_2), top(context, 10));
        final Chips roleChips = new Chips(context);
        roleChips.set("Supervisor", "Admin", "Superadmin");
        roleChips.allowNone(false);
        if (existing != null) {
            if (existing.isSuperAdmin()) roleChips.select(2);
            else if (User.ROLE_ADMIN.equalsIgnoreCase(existing.role)) roleChips.select(1);
            else roleChips.select(0);
        } else {
            roleChips.select(0);
        }
        layout.addView(roleChips, top(context, 4));

        new AlertDialog.Builder(context)
            .setTitle(existing == null ? "Create New User" : "Edit User")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = nameInput.getText().toString().trim();
                    String un = usernameInput.getText().toString().trim();
                    String pw = passwordInput.getText().toString().trim();
                    String mob = mobileInput.getText().toString().trim();

                    if (name.isEmpty() || un.isEmpty() || (existing == null && pw.isEmpty())) {
                        SupervisorManageScreen.this.host.toast("Name, Username, and Password are required");
                        return;
                    }

                    int selectedRoleIdx = roleChips.selected();
                    String role = User.ROLE_SUPERVISOR;
                    if (selectedRoleIdx == 2) role = User.ROLE_SUPERADMIN;
                    else if (selectedRoleIdx == 1) role = User.ROLE_ADMIN;

                    User u = existing != null ? existing : new User();
                    u.name = name;
                    u.username = un;
                    if (!pw.isEmpty()) {
                        u.setPassword(pw);
                    }
                    u.mobile = mob;
                    u.role = role;
                    u.status = "ACTIVE";

                    SQLiteDatabase db = Core.get(context).db();
                    User.save(db, u);
                    SupervisorManageScreen.this.host.toast("User saved: " + name);
                    SupervisorManageScreen.this.refreshContent();
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

    private void confirmDelete(final User user) {
        final Context context = getContext();
        new AlertDialog.Builder(context)
            .setTitle("Delete " + user.name + "?")
            .setMessage("Removes this user account and all their assigned mappings. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SQLiteDatabase db = Core.get(context).db();
                    User.delete(db, user.id);
                    SupervisorManageScreen.this.host.toast("Deleted user " + user.name);
                    SupervisorManageScreen.this.refreshContent();
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

    private static LinearLayout.LayoutParams top(Context context, int i) {
        LinearLayout.LayoutParams mw = mw();
        mw.topMargin = Ui.dp(context, i);
        return mw;
    }
}
