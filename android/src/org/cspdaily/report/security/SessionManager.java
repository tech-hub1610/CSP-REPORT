package org.cspdaily.report.security;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import org.cspdaily.report.Core;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.ui.MainActivity;

public final class SessionManager {
    private static volatile boolean isLocked = false;
    private static volatile long lastActiveTimestamp = System.currentTimeMillis();
    private static volatile long lastBackgroundTimestamp = 0;
    private static final Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private static Runnable inactivityRunnable;

    private SessionManager() {}

    public static boolean isLocked(Context context) {
        return isLocked;
    }

    public static void setLocked(boolean locked) {
        isLocked = locked;
        if (locked) {
            stopInactivityWatchdog();
        }
    }

    public static void onUserActivity(MainActivity activity) {
        lastActiveTimestamp = System.currentTimeMillis();
        if (!isLocked && Auth.isLoggedIn(activity)) {
            startInactivityWatchdog(activity);
        }
    }

    public static void onPause(MainActivity activity) {
        lastBackgroundTimestamp = System.currentTimeMillis();
        stopInactivityWatchdog();
    }

    public static void onResume(final MainActivity activity) {
        if (!Auth.isLoggedIn(activity)) {
            isLocked = false;
            stopInactivityWatchdog();
            return;
        }

        SQLiteDatabase db = Core.get(activity).dbRead();
        boolean lockEnabled = Settings.flag(db, Settings.SECURITY_LOCK_ENABLED, true);
        boolean biometricEnabled = Settings.flag(db, Settings.SECURITY_BIOMETRIC_ENABLED, true);
        boolean isProtected = lockEnabled || biometricEnabled;

        if (isProtected) {
            int lockTimeoutSec = getLockTimeoutSec(db);
            if (lastBackgroundTimestamp > 0) {
                long elapsedSec = (System.currentTimeMillis() - lastBackgroundTimestamp) / 1000L;
                if (elapsedSec >= lockTimeoutSec) {
                    isLocked = true;
                }
            }
        }

        if (isLocked) {
            activity.showLockScreen();
        } else {
            lastActiveTimestamp = System.currentTimeMillis();
            if (isProtected) {
                startInactivityWatchdog(activity);
            }
        }
    }

    public static void startInactivityWatchdog(final MainActivity activity) {
        stopInactivityWatchdog();
        if (isLocked || !Auth.isLoggedIn(activity)) return;

        SQLiteDatabase db = Core.get(activity).dbRead();
        boolean lockEnabled = Settings.flag(db, Settings.SECURITY_LOCK_ENABLED, true);
        boolean biometricEnabled = Settings.flag(db, Settings.SECURITY_BIOMETRIC_ENABLED, true);
        if (!lockEnabled && !biometricEnabled) return;

        final int inactivityTimeoutSec = getInactivityTimeoutSec(db);
        if (inactivityTimeoutSec <= 0) return;

        inactivityRunnable = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (!Auth.isLoggedIn(activity) || isLocked) return;

                long idleSec = (System.currentTimeMillis() - lastActiveTimestamp) / 1000L;
                if (idleSec >= inactivityTimeoutSec) {
                    isLocked = true;
                    activity.showLockScreen();
                } else {
                    long remainingMs = Math.max(1000L, (inactivityTimeoutSec - idleSec) * 1000L);
                    inactivityHandler.postDelayed(this, remainingMs);
                }
            }
        };

        inactivityHandler.postDelayed(inactivityRunnable, inactivityTimeoutSec * 1000L);
    }

    public static void stopInactivityWatchdog() {
        if (inactivityRunnable != null) {
            inactivityHandler.removeCallbacks(inactivityRunnable);
            inactivityRunnable = null;
        }
    }

    public static void unlock(MainActivity activity) {
        isLocked = false;
        lastActiveTimestamp = System.currentTimeMillis();
        lastBackgroundTimestamp = 0;
        startInactivityWatchdog(activity);
        activity.onUnlocked();
    }

    public static int getLockTimeoutSec(SQLiteDatabase db) {
        try {
            String val = Settings.get(db, Settings.SECURITY_LOCK_TIMEOUT, "0");
            int sec = Integer.parseInt(val.trim());
            return Math.max(0, sec);
        } catch (Throwable ignored) {
            return 0; // Default: Immediately when minimized / background
        }
    }

    public static int getInactivityTimeoutSec(SQLiteDatabase db) {
        try {
            String val = Settings.get(db, Settings.SECURITY_INACTIVITY_TIMEOUT, "600");
            int sec = Integer.parseInt(val.trim());
            return sec > 0 ? sec : 600; // Default: 10 minutes of idle inactivity
        } catch (Throwable ignored) {
            return 600;
        }
    }
}
