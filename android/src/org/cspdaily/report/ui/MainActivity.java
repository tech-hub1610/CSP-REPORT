package org.cspdaily.report.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.User;
import org.cspdaily.report.data.Params;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.data.UiState;
import org.cspdaily.report.domain.Backup;
import org.cspdaily.report.notify.Reminders;
import org.cspdaily.report.security.BiometricHelper;
import org.cspdaily.report.security.SessionManager;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.screen.AdminDashboardScreen;
import org.cspdaily.report.ui.screen.CspImportScreen;
import org.cspdaily.report.ui.screen.CspListScreen;
import org.cspdaily.report.ui.screen.CspManageScreen;
import org.cspdaily.report.ui.screen.HomeScreen;
import org.cspdaily.report.ui.screen.LoginScreen;
import org.cspdaily.report.ui.screen.MoreScreen;
import org.cspdaily.report.ui.screen.ReportsScreen;
import org.cspdaily.report.ui.screen.SupervisorManageScreen;
import org.cspdaily.report.ui.screen.UnlockScreen;
import org.cspdaily.report.ui.widget.Interactive3DBackgroundView;
import org.cspdaily.report.util.Files;
import org.cspdaily.report.util.PhoneActions;

/* loaded from: classes.dex */
public class MainActivity extends Activity implements Screen.Host {
    public static final int TAB_CSPS = 1;
    public static final int TAB_HOME = 0;
    public static final int TAB_MORE = 3;
    public static final int TAB_REPORTS = 2;
    private View fab;
    private boolean fadeNext;
    private LinearLayout navBar;
    private FrameLayout screenHost;
    private final List<TextView> navLabels = new ArrayList();
    private final List<ImageView> navIcons = new ArrayList();
    private final ArrayList<Screen> stack = new ArrayList<>();
    private int tab = 0;
    private final Screen[] tabRoot = new Screen[4];
    private final List<View> navPills = new ArrayList();
    private LinearLayout syncStatusPill;
    private android.widget.ProgressBar syncProgressBar;
    private TextView syncStatusText;
    private final android.os.Handler syncHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable syncDismissRunnable;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            if (bundle == null) {
                UiState.resetDate(this);
            }
            buildUi();
            Reminders.handleIntent(this, getIntent());
        } catch (Throwable th) {
            showCrash(th);
        }
    }

    private void showCrash(Throwable th) {
        showCrashText(Core.trace(th));
    }

    private void showCrashText(String str) {
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(2, 11.0f);
        textView.setTextIsSelectable(true);
        textView.setPadding(24, 72, 24, 24);
        scrollView.addView(textView);
        setContentView(scrollView);
    }

    private boolean paintSavedCrash() {
        int read;
        File file = new File(getFilesDir(), "crash.txt");
        if (!file.exists()) {
            return false;
        }
        try {
            byte[] bArr = new byte[(int) file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            int i = 0;
            while (i < bArr.length && (read = fileInputStream.read(bArr, i, bArr.length - i)) > 0) {
                i += read;
            }
            fileInputStream.close();
            file.delete();
            showCrashText(new String(bArr, 0, i, "UTF-8"));
            return true;
        } catch (Throwable th) {
            file.delete();
            return false;
        }
    }

    private Interactive3DBackgroundView bg3D;

    private void buildUi() {
        if (!paintSavedCrash()) {
            getWindow().setSoftInputMode(16);
            FrameLayout frameLayout = new FrameLayout(this);
            boolean isSuperAdmin = Auth.isSuperAdmin(this);
            frameLayout.setBackgroundColor(isSuperAdmin ? 0xFFFDFBF7 : Ui.color(this, R.color.bg_app));

            // Dynamic 3D ambient particle constellation background canvas
            try {
                this.bg3D = new Interactive3DBackgroundView(this);
                if (isSuperAdmin) {
                    this.bg3D.setSuperAdminMode(true);
                }
                frameLayout.addView(this.bg3D, match());

                // Translucent glass diffusion veil to maintain crisp text contrast
                View veil = new View(this);
                veil.setBackgroundColor(isSuperAdmin ? 0x33FFF8ED : 0xD9F1F5F9);
                frameLayout.addView(veil, match());
            } catch (Throwable ignored) {}

            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(1);
            frameLayout.addView(linearLayout, match());
            this.screenHost = new FrameLayout(this);
            linearLayout.addView(this.screenHost, new LinearLayout.LayoutParams(-1, 0, 1.0f));

            if (!Auth.isLoggedIn(this)) {
                setContentView(frameLayout);
                this.stack.clear();
                push(new LoginScreen(this, this));
                return;
            }

            this.navBar = buildNavBar();
            FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, Ui.dp(this, 66.0f));
            navLp.gravity = 80;
            navLp.leftMargin = Ui.dp(this, 12.0f);
            navLp.rightMargin = Ui.dp(this, 12.0f);
            navLp.bottomMargin = Ui.dp(this, 10.0f);
            frameLayout.addView(this.navBar, navLp);

            this.fab = buildFab();
            this.fab.setVisibility(Auth.isSupervisor(this) ? 0 : 8);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(Ui.dp(this, 56.0f), Ui.dp(this, 56.0f));
            layoutParams.gravity = 81;
            layoutParams.bottomMargin = Ui.dp(this, 26.0f);
            frameLayout.addView(this.fab, layoutParams);

            this.syncStatusPill = buildSyncPill();
            FrameLayout.LayoutParams syncLp = new FrameLayout.LayoutParams(-2, -2);
            syncLp.gravity = 49;
            syncLp.topMargin = Ui.dp(this, 12.0f);
            frameLayout.addView(this.syncStatusPill, syncLp);

            setContentView(frameLayout);
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 41);
                } catch (Throwable ignored) {}
            }
            try {
                Reminders.schedule(this);
            } catch (Throwable ignored) {}

            boolean lockEnabled = org.cspdaily.report.data.Settings.flag(Core.get(this).dbRead(), org.cspdaily.report.data.Settings.SECURITY_LOCK_ENABLED, true);
            boolean biometricEnabled = org.cspdaily.report.data.Settings.flag(Core.get(this).dbRead(), org.cspdaily.report.data.Settings.SECURITY_BIOMETRIC_ENABLED, true);
            boolean isProtected = lockEnabled || biometricEnabled;
            if (isProtected) {
                SessionManager.setLocked(true);
            }

            select(0);

            if (isProtected && SessionManager.isLocked(this)) {
                showLockScreen();
            } else {
                SessionManager.startInactivityWatchdog(this);
                checkMandatoryPasswordChange();
            }
        }
    }

    public void showLockScreen() {
        if (!Auth.isLoggedIn(this) || this.screenHost == null) return;
        if (top() instanceof UnlockScreen) {
            try {
                top().refreshContent();
            } catch (Throwable ignored) {}
            return;
        }
        if (this.navBar != null) this.navBar.setVisibility(View.GONE);
        if (this.fab != null) this.fab.setVisibility(View.GONE);
        if (this.syncStatusPill != null) this.syncStatusPill.setVisibility(View.GONE);
        push(new UnlockScreen(this, this));
    }

    public void onUnlocked() {
        if (this.navBar != null) this.navBar.setVisibility(View.VISIBLE);
        if (this.fab != null) this.fab.setVisibility(Auth.isSupervisor(this) ? View.VISIBLE : View.GONE);
        if (top() instanceof UnlockScreen) {
            pop();
        }
        Screen currentTop = top();
        UiState.checkDayRollover(this);
        if (currentTop == null) {
            select(0);
        } else {
            try {
                currentTop.refreshContent();
            } catch (Throwable ignored) {}
        }
        org.cspdaily.report.data.User u = Auth.currentUser(this);
        if (u != null) {
            toast("Session unlocked: " + u.name);
            if (u.isDefaultPassword) {
                checkMandatoryPasswordChange();
            }
        }
    }

    public void onUserLoggedIn() {
        SessionManager.setLocked(false);
        UiState.resetDate(this);
        for (int i = 0; i < this.tabRoot.length; i++) {
            this.tabRoot[i] = null;
        }
        buildUi();
        if (checkMandatoryPasswordChange()) {
            return;
        }
        checkFirstTimeSecuritySetup();
    }

    public boolean checkMandatoryPasswordChange() {
        final User u = Auth.currentUser(this);
        if (u == null || !u.isDefaultPassword) {
            return false;
        }

        final Context context = this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(Ui.dp(context, 20), Ui.dp(context, 12), Ui.dp(context, 20), Ui.dp(context, 12));

        TextView warningText = new TextView(context);
        warningText.setText("You are currently using the default system credentials. For security, you must set a new password before continuing.");
        warningText.setTextColor(0xFFB91C1C);
        warningText.setTextSize(14);
        layout.addView(warningText);

        final EditText newPwInput = new EditText(context);
        newPwInput.setHint("New Password (min 6 chars)");
        newPwInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        styleDialogInput(newPwInput, context);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(-1, -2);
        lp1.topMargin = Ui.dp(context, 12);
        layout.addView(newPwInput, lp1);

        final EditText confirmPwInput = new EditText(context);
        confirmPwInput.setHint("Confirm New Password");
        confirmPwInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        styleDialogInput(confirmPwInput, context);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, -2);
        lp2.topMargin = Ui.dp(context, 8);
        layout.addView(confirmPwInput, lp2);

        final AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("Action Required: Set Password")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Update Password", null)
            .setNegativeButton("Log Out", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    logout();
                }
            })
            .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface di) {
                Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newPw = newPwInput.getText().toString().trim();
                        String confirmPw = confirmPwInput.getText().toString().trim();

                        if (newPw.length() < 6) {
                            toast("Password must be at least 6 characters");
                            return;
                        }
                        if ("admin123".equalsIgnoreCase(newPw)) {
                            toast("Cannot reuse default password 'admin123'");
                            return;
                        }
                        if (!newPw.equals(confirmPw)) {
                            toast("Passwords do not match");
                            return;
                        }

                        SQLiteDatabase db = Core.get(context).db();
                        boolean ok = User.changePassword(db, u.id, newPw);
                        if (ok) {
                            u.setPassword(newPw);
                            u.isDefaultPassword = false;
                            Auth.setCurrentUser(context, u);
                            dialog.dismiss();
                            toast("✓ Password updated securely");
                            checkFirstTimeSecuritySetup();
                        } else {
                            toast("Failed to update password. Please try again.");
                        }
                    }
                });
            }
        });

        dialog.show();
        return true;
    }

    private void styleDialogInput(EditText et, Context context) {
        et.setTextSize(15);
        et.setPadding(Ui.dp(context, 12), Ui.dp(context, 10), Ui.dp(context, 12), Ui.dp(context, 10));
        et.setBackground(Ui.round(context, 0xFFF1F5F9, 8.0f, 0xFFCBD5E1, 1.0f));
    }

    private void checkFirstTimeSecuritySetup() {
        final SQLiteDatabase db = Core.get(this).db();
        boolean alreadyPrompted = Settings.flag(db, "security_setup_prompted", false);
        if (alreadyPrompted) return;

        final boolean hasHw = BiometricHelper.isHardwareDetected(this);
        final boolean hasEnrolled = BiometricHelper.hasEnrolledFingerprints(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Security & Fingerprint Setup");
        if (hasHw && hasEnrolled) {
            builder.setMessage("Enable 1-touch Fingerprint Lock for your session? Auto-lock triggers after 10 minutes of inactivity or when minimized.");
            builder.setPositiveButton("Enable Fingerprint", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Settings.setFlag(db, Settings.SECURITY_BIOMETRIC_ENABLED, true);
                    Settings.setFlag(db, Settings.SECURITY_LOCK_ENABLED, true);
                    Settings.setFlag(db, "security_setup_prompted", true);
                    toast("✓ Fingerprint security enabled (10-min lock)");
                }
            });
        } else if (hasHw) {
            builder.setMessage("Your device supports fingerprint unlock. Register your fingerprint in Android Security Settings to use 1-touch unlock.");
            builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Settings.setFlag(db, Settings.SECURITY_BIOMETRIC_ENABLED, true);
                    Settings.setFlag(db, Settings.SECURITY_LOCK_ENABLED, true);
                    Settings.setFlag(db, "security_setup_prompted", true);
                    BiometricHelper.openDeviceSecuritySettings(MainActivity.this);
                }
            });
        } else {
            builder.setMessage("Session Security Lock is available. Your session will automatically lock with password protection after 10 minutes of inactivity or when minimized.");
            builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Settings.setFlag(db, Settings.SECURITY_LOCK_ENABLED, true);
                    Settings.setFlag(db, "security_setup_prompted", true);
                    toast("✓ Security lock enabled (10-min lock)");
                }
            });
        }
        builder.setNegativeButton("Skip for Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.setFlag(db, "security_setup_prompted", true);
            }
        });
        builder.show();
    }

    public void logout() {
        SessionManager.setLocked(false);
        SessionManager.stopInactivityWatchdog();
        Auth.logout(this);
        for (int i = 0; i < this.tabRoot.length; i++) {
            this.tabRoot[i] = null;
        }
        buildUi();
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(-1, -1);
    }

    private LinearLayout buildNavBar() {
        boolean isSuperAdmin = Auth.isSuperAdmin(this);
        boolean isAdmin = Auth.isAdmin(this);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        if (isSuperAdmin) {
            linearLayout.setBackground(Ui.round(this, 0xF5FCF8F2, 22.0f, 0x66F59E0B, 1.2f));
        } else {
            linearLayout.setBackground(Ui.round(this, Ui.color(this, R.color.glass_nav_bg), 22.0f, Ui.color(this, R.color.glass_nav_border), 1.2f));
        }
        linearLayout.setElevation(Ui.px(this, R.dimen.el_4));
        int padH = Ui.dp(this, 8.0f);
        int padV = Ui.dp(this, 4.0f);
        linearLayout.setPadding(padH, padV, padH, padV);

        String[] strArr;
        int[] iArr;
        if (isSuperAdmin) {
            strArr = new String[]{"Control", "CSPs", "Users", "Settings"};
            iArr = new int[]{R.drawable.ic_home, R.drawable.ic_csp, R.drawable.ic_person, R.drawable.ic_settings};
        } else if (isAdmin) {
            strArr = new String[]{"Dashboard", "CSPs", "Reports", "More"};
            iArr = new int[]{R.drawable.ic_home, R.drawable.ic_csp, R.drawable.ic_reports, R.drawable.ic_settings};
        } else {
            strArr = new String[]{"Home", "CSPs", "Reports", "More"};
            iArr = new int[]{R.drawable.ic_home, R.drawable.ic_csp, R.drawable.ic_reports, R.drawable.ic_settings};
        }

        int[] iArr2 = {0, 1, 2, 3};
        this.navLabels.clear();
        this.navIcons.clear();
        this.navPills.clear();
        for (int i = 0; i < 4; i++) {
            if (i == 2) {
                linearLayout.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 0.9f));
            }
            final int i2 = iArr2[i];
            LinearLayout linearLayout2 = new LinearLayout(this);
            linearLayout2.setOrientation(1);
            linearLayout2.setGravity(17);
            linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -1, 1.0f));

            FrameLayout frameLayout = new FrameLayout(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(this, 44.0f), Ui.dp(this, 26.0f));
            layoutParams.gravity = 1;
            layoutParams.bottomMargin = Ui.dp(this, 2.0f);
            frameLayout.setLayoutParams(layoutParams);
            linearLayout2.addView(frameLayout);
            this.navPills.add(frameLayout);

            ImageView imageView = new ImageView(this);
            int dp = Ui.dp(this, 20.0f);
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(dp, dp);
            layoutParams2.gravity = 17;
            imageView.setLayoutParams(layoutParams2);
            imageView.setImageResource(iArr[i]);
            frameLayout.addView(imageView);
            this.navIcons.add(imageView);

            TextView textView = new TextView(this);
            Ui.size(textView, 10.5f);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setText(strArr[i]);
            textView.setGravity(17);
            textView.setSingleLine(true);
            linearLayout2.addView(textView, new LinearLayout.LayoutParams(-1, -2));
            this.navLabels.add(textView);

            Anim3D.attachTactilePress(linearLayout2);
            linearLayout2.setClickable(true);
            linearLayout2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.select(i2);
                }
            });
        }
        return linearLayout;
    }

    private View buildFab() {
        TextView textView = new TextView(this);
        textView.setGravity(17);
        textView.setText("+");
        Ui.size(textView, 28.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(Ui.color(this, R.color.paper));
        textView.setBackground(Ui.round(this, Ui.color(this, R.color.primary_600), 28.0f, 0x40FFFFFF, 1.5f));
        textView.setElevation(Ui.px(this, R.dimen.el_4));
        Anim3D.attachTactilePress(textView);
        Anim3D.startFloatLevitation(textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ui.hideKeyboard(MainActivity.this);
                QuickActions.open(MainActivity.this, MainActivity.this);
            }
        });
        return textView;
    }

    private LinearLayout buildSyncPill() {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(0);
        pill.setGravity(17);
        pill.setBackground(Ui.round(this, 0xEE0F172A, 14.0f, 0x4D38BDF8, 1.0f));
        pill.setPadding(Ui.dp(this, 12.0f), Ui.dp(this, 5.0f), Ui.dp(this, 12.0f), Ui.dp(this, 5.0f));
        if (Build.VERSION.SDK_INT >= 21) {
            pill.setElevation(Ui.dp(this, 6.0f));
        }
        pill.setVisibility(View.GONE);

        this.syncProgressBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        int pSize = Ui.dp(this, 13.0f);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(pSize, pSize);
        pLp.rightMargin = Ui.dp(this, 6.0f);
        this.syncProgressBar.setLayoutParams(pLp);
        pill.addView(this.syncProgressBar);

        this.syncStatusText = new TextView(this);
        Ui.size(this.syncStatusText, 11.0f);
        this.syncStatusText.setTypeface(Typeface.DEFAULT_BOLD);
        this.syncStatusText.setTextColor(0xFF38BDF8);
        this.syncStatusText.setText("Syncing live data...");
        pill.addView(this.syncStatusText);

        return pill;
    }

    public void select(int i) {
        Iterator it = new ArrayList(this.stack).iterator();
        while (it.hasNext()) {
            Screen screen = (Screen) it.next();
            screen.onHideScreen();
            this.screenHost.removeView(screen);
        }
        this.stack.clear();
        this.tab = i;
        if (this.tabRoot[i] == null) {
            this.tabRoot[i] = newScreen(i);
        }
        this.fadeNext = true;
        push(this.tabRoot[i]);
        paintNav();
    }

    private Screen newScreen(int i) {
        if (Auth.isSuperAdmin(this)) {
            switch (i) {
                case 1:
                    return new CspManageScreen(this, this);
                case 2:
                    return new SupervisorManageScreen(this, this);
                case 3:
                    return new MoreScreen(this, this);
                default:
                    return new AdminDashboardScreen(this, this);
            }
        } else if (Auth.isAdmin(this)) {
            switch (i) {
                case 1:
                    return new CspListScreen(this, this);
                case 2:
                    return new ReportsScreen(this, this);
                case 3:
                    return new MoreScreen(this, this);
                default:
                    return new AdminDashboardScreen(this, this);
            }
        } else {
            switch (i) {
                case 1:
                    return new CspListScreen(this, this);
                case 2:
                    return new ReportsScreen(this, this);
                case 3:
                    return new MoreScreen(this, this);
                default:
                    return new HomeScreen(this, this);
            }
        }
    }

    private void paintNav() {
        boolean isSuper = Auth.isSuperAdmin(this);
        int colorActive = isSuper ? 0xFFD97706 : Ui.color(this, R.color.primary_600);
        int colorInactive = isSuper ? 0xFF78716C : Ui.color(this, R.color.ink_3);
        int i = 0;
        while (i < this.navLabels.size()) {
            boolean active = i == this.tab;
            this.navLabels.get(i).setTextColor(active ? colorActive : colorInactive);
            this.navIcons.get(i).setColorFilter(active ? colorActive : colorInactive);
            if (i < this.navPills.size()) {
                View view = this.navPills.get(i);
                if (active) {
                    int pillBg = isSuper ? 0x2EF59E0B : Ui.color(this, R.color.glass_active_pill);
                    int pillBorder = isSuper ? 0x99F59E0B : Ui.color(this, R.color.glass_active_pill_border);
                    view.setBackground(Ui.round(this, pillBg, 13.0f, pillBorder, 1.0f));
                    view.animate().scaleX(1.08f).scaleY(1.08f).setDuration(220).setInterpolator(new android.view.animation.OvershootInterpolator(1.5f)).start();
                } else {
                    view.setBackgroundColor(Ui.color(this, R.color.transparent));
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                }
            }
            i++;
        }
    }

    public int currentTab() {
        return this.tab;
    }

    @Override // org.cspdaily.report.ui.Screen.Host
    public void push(Screen screen) {
        if (this.screenHost == null || screen == null) return;
        Screen pVar = top();
        boolean z = pVar != null;
        if (z) {
            pVar.onHideScreen();
            animateOut(pVar, -1);
        }
        this.stack.add(screen);
        if (screen.getParent() != null) {
            ((ViewGroup) screen.getParent()).removeView(screen);
        }
        screen.setVisibility(0);
        this.screenHost.addView(screen, match());
        screen.onShowScreen();
        if (z) {
            animateIn(screen, this.fadeNext ? 0 : 1);
        } else if (this.fadeNext) {
            animateIn(screen, 0);
        }
        this.fadeNext = false;
        paintNav();
    }

    @Override // org.cspdaily.report.ui.Screen.Host
    public void pop() {
        if (this.stack.size() > 1) {
            Screen remove = this.stack.remove(this.stack.size() - 1);
            detach(remove);
            this.tabRoot[this.tab] = this.stack.get(0);
            Screen pVar = top();
            if (pVar != null) {
                if (pVar.getParent() == null && this.screenHost != null) {
                    this.screenHost.addView(pVar, match());
                }
                pVar.setVisibility(0);
                pVar.onShowScreen();
                pVar.refreshContent();
                animateIn(pVar, -1);
            }
            animateOut(remove, 1);
            paintNav();
        } else if (this.tab != 0) {
            select(0);
        }
    }

    private void detach(Screen screen) {
        screen.onHideScreen();
    }

    private void animateIn(View view, int i) {
        float f;
        int slide = slide();
        view.setAlpha(0.0f);
        if (i == 0) {
            f = 0.0f;
        } else {
            if (i <= 0) {
                slide = -slide;
            }
            f = slide;
        }
        view.setTranslationX(f);
        view.animate().alpha(1.0f).translationX(0.0f).setDuration(i == 0 ? 180L : 230L).setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    private void animateOut(final View view, int i) {
        view.animate().alpha(0.0f).translationX(i > 0 ? slide() : -slide()).setDuration(190L).setInterpolator(new AccelerateInterpolator()).withEndAction(new Runnable() { // from class: org.cspdaily.report.ui.MainActivity.3
            @Override // java.lang.Runnable
            public void run() {
                view.setVisibility(8);
                view.setAlpha(1.0f);
                view.setTranslationX(0.0f);
                if (screenHost != null && !stack.contains(view)) {
                    screenHost.removeView(view);
                }
            }
        }).start();
    }

    private int slide() {
        int width = this.screenHost == null ? 0 : this.screenHost.getWidth();
        int dp = Ui.dp(this, 64.0f);
        return width > 0 ? Math.min(width / 3, dp) : dp;
    }

    public void replaceStack(Screen screen) {
        Iterator it = new ArrayList(this.stack).iterator();
        while (it.hasNext()) {
            this.screenHost.removeView((Screen) it.next());
        }
        this.stack.clear();
        push(screen);
    }

    @Override // org.cspdaily.report.ui.Screen.Host
    public void goTab(int i) {
        select(i);
    }

    @Override // org.cspdaily.report.ui.Screen.Host
    public void toast(String str) {
        Ui.toast(this, str);
    }

    @Override // org.cspdaily.report.ui.Screen.Host
    public void refresh() {
        Screen pVar = top();
        if (pVar != null) {
            try {
                pVar.refreshContent();
            } catch (Throwable ignored) {}
        }
    }

    @Override // org.cspdaily.report.ui.Screen.Host
    public int statusBarPad() {
        return 0;
    }

    public Screen top() {
        if (this.stack.isEmpty()) {
            return null;
        }
        return this.stack.get(this.stack.size() - 1);
    }

    public Core core() {
        return (Core) getApplicationContext();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        SessionManager.onUserActivity(this);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        Screen pVar = top();
        if (pVar instanceof UnlockScreen) {
            moveTaskToBack(true);
            return;
        }
        if (pVar == null || !pVar.onBackPressed()) {
            if (this.stack.size() > 1) {
                pop();
            } else if (this.tab != 0) {
                select(0);
            } else {
                super.onBackPressed();
            }
        }
    }

    private final org.cspdaily.report.data.SupabaseClient.SyncStatusListener syncStatusListener = new org.cspdaily.report.data.SupabaseClient.SyncStatusListener() {
        @Override
        public void onSyncStarted() {
            if (syncDismissRunnable != null) {
                syncHandler.removeCallbacks(syncDismissRunnable);
                syncDismissRunnable = null;
            }
            if (syncStatusPill != null) {
                syncStatusPill.setVisibility(View.VISIBLE);
                syncStatusPill.setAlpha(1.0f);
                syncStatusPill.setTranslationY(0.0f);
                if (syncProgressBar != null) {
                    syncProgressBar.setVisibility(View.VISIBLE);
                }
                if (syncStatusText != null) {
                    syncStatusText.setTextColor(0xFF38BDF8);
                    syncStatusText.setText("Syncing live data...");
                }
            }
        }

        @Override
        public void onSyncFinished(boolean success, int pulledCount, int pushedCount, String message) {
            if (syncStatusPill != null && syncStatusText != null) {
                if (syncProgressBar != null) {
                    syncProgressBar.setVisibility(View.GONE);
                }
                if (success) {
                    syncStatusText.setTextColor(0xFF34D399);
                    if (pulledCount > 0 || pushedCount > 0) {
                        syncStatusText.setText("✓ Synced (" + (pulledCount + pushedCount) + " updated)");
                    } else {
                        syncStatusText.setText("✓ Cloud Up to Date");
                    }
                } else {
                    syncStatusText.setTextColor(0xFFF87171);
                    syncStatusText.setText("Offline (Local Ready)");
                }

                syncDismissRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (syncStatusPill != null) {
                            syncStatusPill.animate()
                                .alpha(0.0f)
                                .translationY(-Ui.dp(MainActivity.this, 16.0f))
                                .setDuration(260)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (syncStatusPill != null) {
                                             syncStatusPill.setVisibility(View.GONE);
                                        }
                                    }
                                }).start();
                        }
                    }
                };
                syncHandler.postDelayed(syncDismissRunnable, 1300);
            }
        }
    };

    private final org.cspdaily.report.data.SupabaseClient.LiveSyncListener liveSyncListener = new org.cspdaily.report.data.SupabaseClient.LiveSyncListener() {
        @Override
        public void onDataSynced(int pulledCount, int pushedCount) {
            Screen s = top();
            if (s != null) {
                try {
                    s.refreshContent();
                } catch (Throwable ignored) {}
            }
        }
    };

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.screenHost == null) return;
        org.cspdaily.report.util.PhoneActions.onActivityResumed(this);
        SessionManager.onResume(this);
        UiState.checkDayRollover(this);
        Screen pVar = top();
        if (pVar != null) {
            try {
                pVar.refreshContent();
            } catch (Throwable th) {
                showCrash(th);
            }
        }
        if (Auth.isLoggedIn(this) && !SessionManager.isLocked(this)) {
            org.cspdaily.report.data.SupabaseClient.addSyncStatusListener(this.syncStatusListener);
            org.cspdaily.report.data.SupabaseClient.startLiveSync(this, this.liveSyncListener);
            org.cspdaily.report.update.UpdateManager.checkForUpdates(this, false, new org.cspdaily.report.update.UpdateManager.CheckCallback() {
                @Override
                public void onNoUpdate(int currentCode, String currentName) {}

                @Override
                public void onUpdateAvailable(org.cspdaily.report.update.AppRelease release, boolean mandatory) {
                    if (mandatory) {
                        org.cspdaily.report.update.UpdateModal.show(MainActivity.this, release, true);
                    } else {
                        org.cspdaily.report.update.UpdateNotificationManager.notifyUpdateAvailable(MainActivity.this, release);
                    }
                }

                @Override
                public void onRemoteConfigApplied() {
                    Screen s = top();
                    if (s != null) {
                        try { s.refreshContent(); } catch (Throwable ignored) {}
                    }
                }

                @Override
                public void onError(String message, Throwable error) {}
            });
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        SessionManager.onPause(this);
        Screen topScreen = top();
        if (topScreen != null) {
            try {
                topScreen.onHideScreen();
            } catch (Throwable ignored) {}
        }
        org.cspdaily.report.data.SupabaseClient.removeSyncStatusListener(this.syncStatusListener);
        org.cspdaily.report.data.SupabaseClient.stopLiveSync(this.liveSyncListener);
    }

    public Dialog sheet(View view) {
        Dialog dialog = new Dialog(this, R.style.Theme_Csp_Sheet);
        dialog.requestWindowFeature(1);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(-1, -2);
            window.setGravity(80);
            window.setWindowAnimations(R.style.Animation_Sheet);
        }
        return dialog;
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i2 == -1 && intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            if (i == 300) {
                toast(Files.write(this, data, Files.pendingText) ? "Saved " + Files.pendingName : "Could not write that file");
                Files.pendingText = null;
                Files.pendingName = null;
            } else if (i == 301) {
                try {
                    Backup.Result restore = Backup.restore(this, Files.read(this, data));
                    toast(restore.message);
                    if (restore.ok) {
                        Params.reset();
                        select(0);
                    }
                } catch (Exception e) {
                    toast("Restore failed: " + e.getMessage());
                }
            } else if (i == 302) {
                try {
                    String csvContent = Files.read(this, data);
                    Screen topScreen = top();
                    if (topScreen instanceof CspImportScreen) {
                        ((CspImportScreen) topScreen).setCsvContent(csvContent);
                    } else {
                        CspImportScreen importScreen = new CspImportScreen(this, this);
                        push(importScreen);
                        importScreen.setCsvContent(csvContent);
                    }
                } catch (Exception e) {
                    toast("Could not read CSV file: " + e.getMessage());
                }
            } else if (i == 303) {
                Screen topScreen = top();
                if (topScreen instanceof org.cspdaily.report.ui.screen.AppUpdatesScreen) {
                    ((org.cspdaily.report.ui.screen.AppUpdatesScreen) topScreen).onApkFilePicked(data);
                }
            }
        }
    }

    public void pickApkFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = new String[]{"application/vnd.android.package-archive", "application/octet-stream", "*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Release APK"), 303);
        } catch (Exception e) {
            toast("Cannot open file chooser: " + e.getMessage());
        }
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Reminders.handleIntent(this, intent);
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 43) {
            boolean callGranted = false;
            for (int k = 0; k < strArr.length && k < iArr.length; k++) {
                if ("android.permission.CALL_PHONE".equals(strArr[k]) && iArr[k] == 0) {
                    callGranted = true;
                }
            }
            PhoneActions.onCallPermission(this, callGranted);
        }
    }
}
