package org.cspdaily.report.ui.screen;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.SupabaseClient;
import org.cspdaily.report.data.User;
import org.cspdaily.report.security.BiometricHelper;
import org.cspdaily.report.ui.MainActivity;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Avatar;
import org.cspdaily.report.ui.widget.Badge;
import org.cspdaily.report.ui.widget.Cards;

public class LoginScreen extends Screen {
    private EditText usernameInput;
    private EditText passwordInput;
    private TextView errorText;
    private ProgressBar progressBar;
    private View loginBtn;
    private boolean isBiometricActive = false;

    public LoginScreen(final Context context, final Screen.Host host) {
        super(context, host);
        title("Sign In");
        subtitle("Cloud Authentication");
        showBack(false);

        // 1. Modern Hero Branding Header
        LinearLayout headerCard = Cards.hero(context);
        headerCard.setOrientation(1);
        headerCard.setGravity(17);
        Ui.pad(headerCard, 20, 22, 20, 20);

        ImageView logo = new ImageView(context);
        int logoSize = Ui.dp(context, 60.0f);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoLp.bottomMargin = Ui.dp(context, 10.0f);
        logo.setLayoutParams(logoLp);
        logo.setImageResource(R.mipmap.ic_launcher);
        headerCard.addView(logo);

        TextView titleText = new TextView(context);
        titleText.setText("CSP Daily Report");
        Ui.size(titleText, 22.0f);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Ui.color(context, R.color.paper));
        titleText.setGravity(17);
        headerCard.addView(titleText);

        TextView subText = new TextView(context);
        subText.setText("Operations & Banking Fleet Management");
        Ui.size(subText, 13.0f);
        subText.setTextColor(Ui.color(context, R.color.hero_text_dim));
        subText.setGravity(17);
        Ui.pad(subText, 0, 3, 0, 8);
        headerCard.addView(subText);

        // Cloud status badge inside hero
        LinearLayout badgePill = new LinearLayout(context);
        badgePill.setOrientation(0);
        badgePill.setGravity(16);
        badgePill.setBackground(Ui.round(context, 0x33FFFFFF, 12.0f, 0x4DFFFFFF, 1.0f));
        badgePill.setPadding(Ui.dp(context, 10.0f), Ui.dp(context, 4.0f), Ui.dp(context, 10.0f), Ui.dp(context, 4.0f));

        ImageView shieldIcon = new ImageView(context);
        int sSize = Ui.dp(context, 14.0f);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(sSize, sSize);
        sLp.rightMargin = Ui.dp(context, 5.0f);
        shieldIcon.setLayoutParams(sLp);
        shieldIcon.setImageResource(R.drawable.ic_shield_auth);
        shieldIcon.setColorFilter(Ui.color(context, R.color.hero_ok));
        badgePill.addView(shieldIcon);

        TextView badgeText = new TextView(context);
        badgeText.setText("Supabase Cloud Connected");
        Ui.size(badgeText, 11.5f);
        badgeText.setTypeface(Typeface.DEFAULT_BOLD);
        badgeText.setTextColor(Ui.color(context, R.color.paper));
        badgePill.addView(badgeText);

        headerCard.addView(badgePill, wrap());
        add(headerCard, 0);

        // 2. Quick Fingerprint Sign-In Card (if remembered account & sensor ready)
        final User lastUser = Auth.lastUser(context);
        boolean biometricReady = BiometricHelper.isBiometricReady(context);

        if (lastUser != null && biometricReady) {
            LinearLayout bioCard = Cards.card(context);
            bioCard.setOrientation(1);
            Ui.pad(bioCard, 16, 16, 16, 16);
            bioCard.setBackground(Ui.round(context, Ui.color(context, R.color.primary_50), 18.0f, Ui.color(context, R.color.primary_200), 1.0f));

            LinearLayout bioTop = new LinearLayout(context);
            bioTop.setOrientation(0);
            bioTop.setGravity(16);
            bioCard.addView(bioTop, mw());

            bioTop.addView(new Avatar(context, 44).bind(lastUser.name, lastUser.username), new LinearLayout.LayoutParams(Ui.dp(context, 44.0f), Ui.dp(context, 44.0f)));

            LinearLayout bioInfo = new LinearLayout(context);
            bioInfo.setOrientation(1);
            LinearLayout.LayoutParams bioInfoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            bioInfoLp.leftMargin = Ui.dp(context, 12.0f);
            bioInfo.setLayoutParams(bioInfoLp);
            bioTop.addView(bioInfo);

            TextView bioName = new TextView(context);
            bioName.setText(lastUser.name);
            bioName.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(bioName, 15.5f);
            bioName.setTextColor(Ui.color(context, R.color.ink));
            bioInfo.addView(bioName);

            TextView bioSub = new TextView(context);
            bioSub.setText("@" + lastUser.username);
            bioSub.setTextAppearance(context, R.style.Text_Meta);
            bioSub.setTextColor(Ui.color(context, R.color.ink_3));
            bioInfo.addView(bioSub);

            Badge bioBadge = new Badge(context);
            bioBadge.setKind(lastUser.isSuperAdmin() ? 4 : (lastUser.isAdmin() ? 1 : 0));
            bioBadge.setText(lastUser.roleLabel());
            bioTop.addView(bioBadge, wrap());

            // 1-Touch Fingerprint Button inside card
            LinearLayout fpBtn = new LinearLayout(context);
            fpBtn.setOrientation(0);
            fpBtn.setGravity(17);
            fpBtn.setBackground(Ui.pressable(context, Ui.color(context, R.color.primary_600), 14.0f, true));
            fpBtn.setElevation(Ui.px(context, R.dimen.el_2));
            Ui.pad(fpBtn, 16, 12, 16, 12);
            fpBtn.setClickable(true);

            ImageView fpIcon = new ImageView(context);
            int fpSize = Ui.dp(context, 24.0f);
            LinearLayout.LayoutParams fpLp = new LinearLayout.LayoutParams(fpSize, fpSize);
            fpLp.rightMargin = Ui.dp(context, 8.0f);
            fpIcon.setLayoutParams(fpLp);
            fpIcon.setImageResource(R.drawable.ic_fingerprint);
            fpIcon.setColorFilter(Ui.color(context, R.color.paper));
            fpBtn.addView(fpIcon);

            TextView fpBtnText = new TextView(context);
            fpBtnText.setText("1-Touch Fingerprint Sign In");
            fpBtnText.setTypeface(Typeface.DEFAULT_BOLD);
            Ui.size(fpBtnText, 15.0f);
            fpBtnText.setTextColor(Ui.color(context, R.color.paper));
            fpBtn.addView(fpBtnText);

            fpBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptFingerprintSignIn(lastUser);
                }
            });

            bioCard.addView(fpBtn, top(context, 12));
            add(bioCard, 10);
        }

        // 3. Password Authentication Form Card
        LinearLayout formCard = Cards.card(context);
        formCard.setOrientation(1);
        Ui.pad(formCard, 18, 18, 18, 20);

        TextView formTitle = Cards.bold(context, lastUser != null ? "Or Sign In with Password" : "Account Credentials", 15.5f, R.color.ink);
        formCard.addView(formTitle, mw());

        // Username Field with leading icon
        formCard.addView(Cards.label(context, "Username", R.style.Text_Label, R.color.ink_2), top(context, 14));
        LinearLayout uRow = new LinearLayout(context);
        uRow.setOrientation(0);
        uRow.setGravity(16);
        uRow.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(uRow, 12, 4, 12, 4);

        ImageView uIcon = new ImageView(context);
        int icSize = Ui.dp(context, 20.0f);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(icSize, icSize);
        icLp.rightMargin = Ui.dp(context, 8.0f);
        uIcon.setLayoutParams(icLp);
        uIcon.setImageResource(R.drawable.ic_user);
        uIcon.setColorFilter(Ui.color(context, R.color.ink_3));
        uRow.addView(uIcon);

        this.usernameInput = new EditText(context);
        this.usernameInput.setHint("Enter username");
        if (lastUser != null && lastUser.username != null && !lastUser.username.isEmpty()) {
            this.usernameInput.setText(lastUser.username);
        }
        styleInput(this.usernameInput, context, InputType.TYPE_CLASS_TEXT);
        uRow.addView(this.usernameInput, new LinearLayout.LayoutParams(0, -2, 1.0f));
        formCard.addView(uRow, top(context, 4));

        // Password Field with leading icon
        formCard.addView(Cards.label(context, "Password", R.style.Text_Label, R.color.ink_2), top(context, 12));
        LinearLayout pRow = new LinearLayout(context);
        pRow.setOrientation(0);
        pRow.setGravity(16);
        pRow.setBackground(Ui.round(context, Ui.color(context, R.color.paper), 12.0f, Ui.color(context, R.color.line_2), 1.0f));
        Ui.pad(pRow, 12, 4, 12, 4);

        ImageView pIcon = new ImageView(context);
        LinearLayout.LayoutParams pIcParams = new LinearLayout.LayoutParams(icSize, icSize);
        pIcParams.rightMargin = Ui.dp(context, 8.0f);
        pIcon.setLayoutParams(pIcParams);
        pIcon.setImageResource(R.drawable.ic_key);
        pIcon.setColorFilter(Ui.color(context, R.color.ink_3));
        pRow.addView(pIcon);

        this.passwordInput = new EditText(context);
        this.passwordInput.setHint("Enter password");
        styleInput(this.passwordInput, context, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pRow.addView(this.passwordInput, new LinearLayout.LayoutParams(0, -2, 1.0f));
        formCard.addView(pRow, top(context, 4));

        // Progress Bar
        this.progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(8);
        formCard.addView(this.progressBar, top(context, 10));

        // Error message
        this.errorText = Cards.label(context, "", R.style.Text_Meta, R.color.danger);
        this.errorText.setVisibility(8);
        formCard.addView(this.errorText, top(context, 8));

        // Login Button
        this.loginBtn = Cards.primaryButton(context, "Sign In with Password", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginScreen.this.doLogin();
            }
        });
        formCard.addView(this.loginBtn, top(context, 16));

        add(formCard, 10);

        // 4. Cloud Database Config footer link
        LinearLayout cloudConfigRow = new LinearLayout(context);
        cloudConfigRow.setOrientation(1);
        cloudConfigRow.setGravity(17);
        Ui.pad(cloudConfigRow, 16, 14, 16, 16);

        View configLink = Cards.link(context, "Supabase Database Settings & SQL Setup", R.color.primary_600, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.push(new SupabaseSyncScreen(context, host));
            }
        });
        cloudConfigRow.addView(configLink, mw());

        TextView cloudNote = new TextView(context);
        cloudNote.setText("Direct Supabase cloud PostgreSQL connection · Fast offline caching");
        cloudNote.setTextAppearance(context, R.style.Text_Nano);
        cloudNote.setTextColor(Ui.color(context, R.color.ink_4));
        cloudNote.setGravity(17);
        Ui.pad(cloudNote, 12, 4, 12, 0);
        cloudConfigRow.addView(cloudNote, mw());

        add(cloudConfigRow, 0);
    }

    @Override
    public String headline() {
        return "Sign In";
    }

    @Override
    protected boolean canGoBack() {
        return false;
    }

    private void styleInput(EditText editText, Context context, int inputType) {
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        Ui.size(editText, 15.0f);
        editText.setTextColor(Ui.color(context, R.color.ink));
        editText.setHintTextColor(Ui.color(context, R.color.ink_4));
        editText.setBackgroundColor(0);
        Ui.pad(editText, 4, 10, 4, 10);
    }

    private void promptFingerprintSignIn(final User lastUser) {
        final Context context = getContext();
        if (!(context instanceof Activity) || lastUser == null || isBiometricActive) return;

        isBiometricActive = true;
        BiometricHelper.authenticate((Activity) context, "Quick Sign In as " + lastUser.name, new BiometricHelper.Callback() {
            @Override
            public void onSuccess() {
                isBiometricActive = false;
                Auth.setCurrentUser(context, lastUser);
                host.toast("Fingerprint verified: Welcome, " + lastUser.name);
                if (context instanceof MainActivity) {
                    ((MainActivity) context).onUserLoggedIn();
                }
            }

            @Override
            public void onError(String message) {
                isBiometricActive = false;
                if (message != null && !message.contains("canceled") && !message.contains("Switched")) {
                    errorText.setText(message);
                    errorText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailed() {
                isBiometricActive = false;
                errorText.setText("Fingerprint not recognized. Please try again or use password.");
                errorText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void doLogin() {
        final Context context = getContext();
        final String u = this.usernameInput.getText().toString().trim();
        final String p = this.passwordInput.getText().toString().trim();

        if (u.isEmpty() || p.isEmpty()) {
            this.errorText.setText("Please enter username and password");
            this.errorText.setVisibility(0);
            return;
        }

        this.errorText.setVisibility(8);
        this.progressBar.setVisibility(0);
        this.loginBtn.setEnabled(false);

        // Authenticate strictly online via Supabase
        SupabaseClient.authenticateOnline(context, u, p, new SupabaseClient.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                progressBar.setVisibility(8);
                loginBtn.setEnabled(true);
                Ui.hideKeyboard((Activity) context);
                host.toast("Authenticated via Supabase: Welcome, " + user.name + " (" + user.roleLabel() + ")");
                if (context instanceof MainActivity) {
                    ((MainActivity) context).onUserLoggedIn();
                }
            }

            @Override
            public void onError(String message, Throwable error) {
                progressBar.setVisibility(8);
                loginBtn.setEnabled(true);
                errorText.setText(message);
                errorText.setVisibility(0);
            }
        });
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
