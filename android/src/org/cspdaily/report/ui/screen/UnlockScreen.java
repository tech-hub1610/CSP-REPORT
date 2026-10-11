package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.data.User;
import org.cspdaily.report.security.BiometricHelper;
import org.cspdaily.report.security.Crypto;
import org.cspdaily.report.security.SessionManager;
import org.cspdaily.report.ui.Anim3D;
import org.cspdaily.report.ui.MainActivity;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;

public class UnlockScreen extends Screen {
    private EditText passwordInput;
    private TextView errorText;
    private TextView fpStatusText;
    private ImageView fpIconView;
    private ProgressBar progressBar;
    private View unlockBtn;
    private CancellationSignal biometricSignal;
    private boolean isBiometricActive = false;

    public UnlockScreen(final Context context, final Screen.Host host) {
        super(context, host);
        title("Session Locked");
        subtitle("Security Verification");
        showBack(false);

        final User user = Auth.currentUser(context);
        if (user == null) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).logout();
            }
            return;
        }

        // 1. Sleek Modern Header
        LinearLayout heroCard = Cards.hero(context);
        heroCard.setOrientation(1);
        heroCard.setGravity(17);
        Ui.pad(heroCard, 18, 20, 18, 18);

        // Circular Glowing Lock Badge
        LinearLayout lockBadge = new LinearLayout(context);
        lockBadge.setOrientation(0);
        lockBadge.setGravity(17);
        int badgeD = Ui.dp(context, 54.0f);
        lockBadge.setLayoutParams(new LinearLayout.LayoutParams(badgeD, badgeD));
        lockBadge.setBackground(Ui.round(context, 0x33FFFFFF, 27.0f, 0x55FFFFFF, 1.5f));

        ImageView lockIcon = new ImageView(context);
        int lockSize = Ui.dp(context, 28.0f);
        lockIcon.setLayoutParams(new LinearLayout.LayoutParams(lockSize, lockSize));
        lockIcon.setImageResource(R.drawable.ic_lock);
        lockIcon.setColorFilter(Ui.color(context, R.color.paper));
        lockBadge.addView(lockIcon);
        heroCard.addView(lockBadge);

        TextView lockTitle = new TextView(context);
        lockTitle.setText("Session Locked");
        Ui.size(lockTitle, 20.0f);
        lockTitle.setTypeface(Typeface.DEFAULT_BOLD);
        lockTitle.setTextColor(Ui.color(context, R.color.paper));
        lockTitle.setGravity(17);
        Ui.pad(lockTitle, 0, 10, 0, 2);
        heroCard.addView(lockTitle);

        TextView lockSub = new TextView(context);
        lockSub.setText("Verification required after 10-min inactivity or exit");
        Ui.size(lockSub, 12.5f);
        lockSub.setTextColor(Ui.color(context, R.color.hero_text_dim));
        lockSub.setGravity(17);
        heroCard.addView(lockSub);

        add(heroCard, 0);

        // 2. Active User Profile Card
        LinearLayout userCard = Cards.card(context);
        userCard.setOrientation(0);
        userCard.setGravity(16);
        Ui.pad(userCard, 14, 12, 14, 12);

        userCard.addView(new Avatar(context, 44).bind(user.name, user.username), new LinearLayout.LayoutParams(Ui.dp(context, 44.0f), Ui.dp(context, 44.0f)));

        LinearLayout userInfo = new LinearLayout(context);
        userInfo.setOrientation(1);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        infoLp.leftMargin = Ui.dp(context, 12.0f);
        userInfo.setLayoutParams(infoLp);
        userCard.addView(userInfo);

        TextView nameText = new TextView(context);
        nameText.setText(user.name);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.size(nameText, 15.5f);
        nameText.setTextColor(Ui.color(context, R.color.ink));
        userInfo.addView(nameText);

        TextView sub = new TextView(context);
        sub.setText("@" + user.username + (user.mobile.isEmpty() ? "" : " · " + user.mobile));
        sub.setTextAppearance(context, R.style.Text_Meta);
        sub.setTextColor(Ui.color(context, R.color.ink_3));
        userInfo.addView(sub);

        Badge badge = new Badge(context);
        badge.setKind(user.isSuperAdmin() ? 4 : (user.isAdmin() ? 1 : 0));
        badge.setText(user.roleLabel());
        userCard.addView(badge, wrap());

        add(userCard, 10);

        // 3. Biometric / Fingerprint Unlock Card
        boolean hasHardware = BiometricHelper.isHardwareDetected(context);
        boolean hasEnrolled = BiometricHelper.hasEnrolledFingerprints(context);
        boolean biometricEnabled = Settings.flag(Core.get(context).dbRead(), Settings.SECURITY_BIOMETRIC_ENABLED, true);

        LinearLayout fpCard = Cards.card(context);
        fpCard.setOrientation(1);
        fpCard.setGravity(17);
        Ui.pad(fpCard, 16, 16, 16, 16);

        if (!hasHardware) {
            this.fpIconView = new ImageView(context);
            int fpSize = Ui.dp(context, 38.0f);
            this.fpIconView.setLayoutParams(new LinearLayout.LayoutParams(fpSize, fpSize));
            this.fpIconView.setImageResource(R.drawable.ic_fingerprint);
            this.fpIconView.setColorFilter(Ui.color(context, R.color.ink_4));
            fpCard.addView(this.fpIconView);

            TextView noHwText = new TextView(context);
            noHwText.setText("Fingerprint Sensor Not Detected");
            Ui.size(noHwText, 14.0f);
            noHwText.setTypeface(Typeface.DEFAULT_BOLD);
            noHwText.setTextColor(Ui.color(context, R.color.ink_3));
            Ui.pad(noHwText, 0, 6, 0, 2);
            fpCard.addView(noHwText);

            TextView noHwSub = new TextView(context);
            noHwSub.setText("Use your account password below to unlock.");
            noHwSub.setTextAppearance(context, R.style.Text_Meta);
            noHwSub.setTextColor(Ui.color(context, R.color.ink_4));
            fpCard.addView(noHwSub);

            add(fpCard, 10);
        } else if (!hasEnrolled) {
            this.fpIconView = new ImageView(context);
            int fpSize = Ui.dp(context, 40.0f);
            this.fpIconView.setLayoutParams(new LinearLayout.LayoutParams(fpSize, fpSize));
            this.fpIconView.setImageResource(R.drawable.ic_fingerprint);
            this.fpIconView.setColorFilter(Ui.color(context, R.color.warn));
            fpCard.addView(this.fpIconView);

            TextView setupTitle = new TextView(context);
            setupTitle.setText("Setup Fingerprint on Device");
            Ui.size(setupTitle, 14.5f);
            setupTitle.setTypeface(Typeface.DEFAULT_BOLD);
            setupTitle.setTextColor(Ui.color(context, R.color.ink));
            Ui.pad(setupTitle, 0, 6, 0, 2);
            fpCard.addView(setupTitle);

            TextView setupSub = new TextView(context);
            setupSub.setText("Register your fingerprint in Android device settings to enable instant 1-touch unlock.");
            setupSub.setTextAppearance(context, R.style.Text_Meta);
            setupSub.setTextColor(Ui.color(context, R.color.ink_3));
            setupSub.setGravity(17);
            Ui.pad(setupSub, 10, 2, 10, 10);
            fpCard.addView(setupSub);

            View openSettingsBtn = Cards.tonalButton(context, "⚙️ Open Phone Security Settings", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BiometricHelper.openDeviceSecuritySettings(context);
                }
            });
            fpCard.addView(openSettingsBtn, mw());

            add(fpCard, 10);
        } else if (biometricEnabled) {
            fpCard.setBackground(Ui.round(context, Ui.color(context, R.color.primary_50), 18.0f, Ui.color(context, R.color.primary_200), 1.0f));

            this.fpIconView = new ImageView(context);
            int fpSize = Ui.dp(context, 48.0f);
            LinearLayout.LayoutParams fpLp = new LinearLayout.LayoutParams(fpSize, fpSize);
            fpLp.bottomMargin = Ui.dp(context, 6.0f);
            this.fpIconView.setLayoutParams(fpLp);
            this.fpIconView.setImageResource(R.drawable.ic_fingerprint);
            this.fpIconView.setColorFilter(Ui.color(context, R.color.primary_600));
            fpCard.addView(this.fpIconView);

            TextView fpText = new TextView(context);
            fpText.setText("Tap to Scan Fingerprint");
            fpText.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(fpText, 15.5f);
            fpText.setTextColor(Ui.color(context, R.color.primary_700));
            fpCard.addView(fpText);

            this.fpStatusText = new TextView(context);
            this.fpStatusText.setText("Touch fingerprint sensor on phone to unlock");
            this.fpStatusText.setTextAppearance(context, R.style.Text_Meta);
            this.fpStatusText.setTextColor(Ui.color(context, R.color.ink_3));
            this.fpStatusText.setGravity(17);
            Ui.pad(this.fpStatusText, 8, 2, 8, 0);
            fpCard.addView(this.fpStatusText);

            fpCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptBiometric();
                }
            });

            add(fpCard, 10);
        }

        // 4. Password Form Card
        LinearLayout passwordCard = Cards.card(context);
        passwordCard.setOrientation(1);
        Ui.pad(passwordCard, 16, 16, 16, 16);

        passwordCard.addView(Cards.bold(context, "Password Unlock", 14.5f, R.color.ink), mw());

        passwordCard.addView(Cards.label(context, "Or enter password for @" + user.username + ":", R.style.Text_Meta, R.color.ink_3), top(context, 4));

        LinearLayout pRow = new LinearLayout(context);
        pRow.setOrientation(0);
        pRow.setGravity(16);
        pRow.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(pRow, 12, 4, 12, 4);

        ImageView pIcon = new ImageView(context);
        int icSize = Ui.dp(context, 20.0f);
        LinearLayout.LayoutParams pIcParams = new LinearLayout.LayoutParams(icSize, icSize);
        pIcParams.rightMargin = Ui.dp(context, 8.0f);
        pIcon.setLayoutParams(pIcParams);
        pIcon.setImageResource(R.drawable.ic_key);
        pIcon.setColorFilter(Ui.color(context, R.color.ink_3));
        pRow.addView(pIcon);

        this.passwordInput = new EditText(context);
        this.passwordInput.setHint("Enter account password");
        this.passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        this.passwordInput.setSingleLine(true);
        Ui.size(this.passwordInput, 15.0f);
        this.passwordInput.setTextColor(Ui.color(context, R.color.ink));
        this.passwordInput.setHintTextColor(Ui.color(context, R.color.ink_4));
        this.passwordInput.setBackgroundColor(0);
        Ui.pad(this.passwordInput, 4, 10, 4, 10);
        pRow.addView(this.passwordInput, new LinearLayout.LayoutParams(0, -2, 1.0f));
        passwordCard.addView(pRow, top(context, 8));

        this.progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(8);
        passwordCard.addView(this.progressBar, top(context, 8));

        this.errorText = Cards.label(context, "", R.style.Text_Meta, R.color.danger);
        this.errorText.setVisibility(8);
        passwordCard.addView(this.errorText, top(context, 8));

        this.unlockBtn = Cards.primaryButton(context, "Unlock with Password", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPasswordUnlock();
            }
        });
        passwordCard.addView(this.unlockBtn, top(context, 12));

        add(passwordCard, 10);

        // 5. Sign Out / Switch Account
        LinearLayout logoutBox = new LinearLayout(context);
        logoutBox.setOrientation(1);
        logoutBox.setGravity(17);
        Ui.pad(logoutBox, 16, 12, 16, 16);

        View logoutLink = Cards.link(context, "Sign Out / Switch to Another Account", R.color.danger, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).logout();
                }
            }
        });
        logoutBox.addView(logoutLink, mw());

        add(logoutBox, 4);
    }

    @Override
    public String headline() {
        return "Session Locked";
    }

    @Override
    protected boolean canGoBack() {
        return false;
    }

    @Override
    public boolean onBackPressed() {
        Context context = getContext();
        if (context instanceof Activity) {
            ((Activity) context).moveTaskToBack(true);
        }
        return true;
    }

    @Override
    public void onShowScreen() {
        super.onShowScreen();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                Context context = getContext();
                if (context instanceof Activity && !((Activity) context).isFinishing()) {
                    boolean hasHardware = BiometricHelper.isHardwareDetected(context);
                    boolean hasEnrolled = BiometricHelper.hasEnrolledFingerprints(context);
                    boolean biometricEnabled = Settings.flag(Core.get(context).dbRead(), Settings.SECURITY_BIOMETRIC_ENABLED, true);
                    if (hasHardware && hasEnrolled && biometricEnabled) {
                        promptBiometric();
                    }
                }
            }
        }, 350);
    }

    @Override
    public void onHideScreen() {
        super.onHideScreen();
        cancelBiometric();
    }

    private void promptBiometric() {
        Context context = getContext();
        if (!(context instanceof Activity) || isBiometricActive) return;

        final User user = Auth.currentUser(context);
        String username = user != null ? user.username : "";

        cancelBiometric();
        isBiometricActive = true;

        if (this.fpStatusText != null) {
            this.fpStatusText.setText("⚡ Sensor Listening — Touch sensor now...");
            this.fpStatusText.setTextColor(Ui.color(context, R.color.primary_600));
        }
        if (this.fpIconView != null) {
            this.fpIconView.setColorFilter(Ui.color(context, R.color.primary_600));
        }

        try {
            this.biometricSignal = BiometricHelper.authenticate((Activity) context, "Unlock @" + username, new BiometricHelper.Callback() {
                @Override
                public void onSuccess() {
                    isBiometricActive = false;
                    if (fpStatusText != null) {
                        fpStatusText.setText("✓ Fingerprint Verified!");
                        fpStatusText.setTextColor(0xFF34D399);
                    }
                    completeUnlock();
                }

                @Override
                public void onError(String message) {
                    isBiometricActive = false;
                    if (fpStatusText != null) {
                        fpStatusText.setText(message != null ? message : "Biometric error");
                        fpStatusText.setTextColor(Ui.color(getContext(), R.color.ink_3));
                    }
                    if (errorText != null && message != null && !message.contains("canceled") && !message.contains("Switched")) {
                        errorText.setText(message);
                        errorText.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailed() {
                    isBiometricActive = false;
                    if (fpStatusText != null) {
                        fpStatusText.setText("Fingerprint not recognized. Tap to try again.");
                        fpStatusText.setTextColor(Ui.color(getContext(), R.color.danger));
                    }
                    if (errorText != null) {
                        errorText.setText("Fingerprint not recognized. Try again or enter password.");
                        errorText.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (Throwable th) {
            isBiometricActive = false;
            if (errorText != null) {
                errorText.setText("Biometric sensor error: " + th.getMessage());
                errorText.setVisibility(View.VISIBLE);
            }
        }
    }

    private void cancelBiometric() {
        if (biometricSignal != null && !biometricSignal.isCanceled()) {
            try {
                biometricSignal.cancel();
            } catch (Throwable ignored) {}
            biometricSignal = null;
        }
        isBiometricActive = false;
    }

    private void doPasswordUnlock() {
        final Context context = getContext();
        final User user = Auth.currentUser(context);
        if (user == null) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).logout();
            }
            return;
        }

        final String enteredPassword = this.passwordInput.getText().toString().trim();
        if (enteredPassword.isEmpty()) {
            this.errorText.setText("Please enter your password");
            this.errorText.setVisibility(View.VISIBLE);
            return;
        }

        boolean verified = false;
        if (user.hasPasswordHash()) {
            verified = Crypto.verify(enteredPassword, user.passwordHash, user.passwordSalt);
        } else if (user.password != null && !user.password.isEmpty()) {
            if (user.password.equals(enteredPassword)) {
                verified = true;
                user.setPassword(enteredPassword);
                SQLiteDatabase db = Core.get(context).db();
                User.save(db, user);
                Auth.setCurrentUser(context, user);
            }
        }

        if (verified) {
            completeUnlock();
            return;
        }

        this.errorText.setVisibility(View.GONE);
        this.progressBar.setVisibility(View.VISIBLE);
        this.unlockBtn.setEnabled(false);

        SupabaseClient.authenticateOnline(context, user.username, enteredPassword, new SupabaseClient.Callback<User>() {
            @Override
            public void onSuccess(User updatedUser) {
                progressBar.setVisibility(View.GONE);
                unlockBtn.setEnabled(true);
                completeUnlock();
            }

            @Override
            public void onError(String message, Throwable error) {
                progressBar.setVisibility(View.GONE);
                unlockBtn.setEnabled(true);
                errorText.setText("Incorrect password. Please try again.");
                errorText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void completeUnlock() {
        cancelBiometric();
        Context context = getContext();
        if (context instanceof Activity) {
            Ui.hideKeyboard((Activity) context);
        }
        if (context instanceof MainActivity) {
            SessionManager.unlock((MainActivity) context);
        }
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
}
