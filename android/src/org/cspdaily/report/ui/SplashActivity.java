package org.cspdaily.report.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import org.cspdaily.report.ui.widget.Splash3DView;

/**
 * Modern 3D Splash Screen Activity with cinematic particle vortex,
 * concentric 3D gyro rings, and seamless transition to MainActivity.
 */
public class SplashActivity extends Activity {

    private static final long SPLASH_DURATION_MS = 1800L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isNavigating = false;

    private final Runnable proceedRunnable = new Runnable() {
        @Override
        public void run() {
            proceedToMain();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            if (Build.VERSION.SDK_INT >= 21) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(0xFF061426);
                window.setNavigationBarColor(0xFF07172C);
            }

            Splash3DView splashView = new Splash3DView(this);
            setContentView(splashView);

            // Allow immediate skip on tap
            splashView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    proceedToMain();
                }
            });

            handler.postDelayed(proceedRunnable, SPLASH_DURATION_MS);
        } catch (Throwable t) {
            proceedToMain();
        }
    }

    private synchronized void proceedToMain() {
        if (isNavigating) return;
        isNavigating = true;
        handler.removeCallbacks(proceedRunnable);

        try {
            Intent intent = new Intent(this, MainActivity.class);
            if (getIntent() != null && getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            if (getIntent() != null && getIntent().getData() != null) {
                intent.setData(getIntent().getData());
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Throwable ignored) {}

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(proceedRunnable);
    }

    @Override
    public void onBackPressed() {
        // Prevent back press during splash
    }
}
