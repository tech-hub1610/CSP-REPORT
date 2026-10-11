package org.cspdaily.report.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;

public final class BiometricHelper {
    private static final String PREF_BIOMETRIC = "csp_biometric";
    private static final String KEY_FP_LINKED = "fp_linked_user_";

    private BiometricHelper() {}

    public interface Callback {
        void onSuccess();
        void onError(String message);
        void onFailed();
    }

    public static boolean isHardwareDetected(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                BiometricManager bm = (BiometricManager) context.getSystemService(Context.BIOMETRIC_SERVICE);
                if (bm != null) {
                    int res = bm.canAuthenticate();
                    if (res != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
                if (fm != null && fm.isHardwareDetected()) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean hasEnrolledFingerprints(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                BiometricManager bm = (BiometricManager) context.getSystemService(Context.BIOMETRIC_SERVICE);
                if (bm != null) {
                    int res = bm.canAuthenticate();
                    if (res == BiometricManager.BIOMETRIC_SUCCESS) {
                        return true;
                    } else if (res == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        return false;
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
                if (fm != null && fm.isHardwareDetected()) {
                    return fm.hasEnrolledFingerprints();
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean isDeviceSecure(Context context) {
        try {
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return km.isDeviceSecure();
                } else {
                    return km.isKeyguardSecure();
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isBiometricReady(Context context) {
        return isHardwareDetected(context) && hasEnrolledFingerprints(context);
    }

    public static boolean isFingerprintLinkedForUser(Context context, long userId) {
        if (userId <= 0) return true;
        SharedPreferences sp = context.getSharedPreferences(PREF_BIOMETRIC, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_FP_LINKED + userId, true);
    }

    public static void setFingerprintLinkedForUser(Context context, long userId, boolean linked) {
        if (userId <= 0) return;
        SharedPreferences sp = context.getSharedPreferences(PREF_BIOMETRIC, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_FP_LINKED + userId, linked).apply();
    }

    public static String getBiometricStatusSummary(Context context) {
        if (!isHardwareDetected(context)) {
            return "No fingerprint sensor detected on this device hardware";
        }
        if (!hasEnrolledFingerprints(context)) {
            return "Sensor available, but no fingerprints registered in Android Settings. Tap Setup to add fingerprint.";
        }
        return "Device fingerprint sensor ready & active";
    }

    public static boolean openDeviceSecuritySettings(Context context) {
        String[] actionList;
        if (Build.VERSION.SDK_INT >= 30) {
            actionList = new String[]{
                "android.settings.BIOMETRIC_ENROLL",
                "android.settings.FINGERPRINT_ENROLL",
                android.provider.Settings.ACTION_SECURITY_SETTINGS,
                android.provider.Settings.ACTION_SETTINGS
            };
        } else {
            actionList = new String[]{
                "android.settings.FINGERPRINT_ENROLL",
                android.provider.Settings.ACTION_SECURITY_SETTINGS,
                android.provider.Settings.ACTION_SETTINGS
            };
        }

        for (String action : actionList) {
            try {
                Intent intent = new Intent(action);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static CancellationSignal authenticate(final Activity activity, final String promptSubtitle, final Callback callback) {
        final CancellationSignal cancellationSignal = new CancellationSignal();
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                Executor executor = new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        mainHandler.post(command);
                    }
                };

                String sub = promptSubtitle != null && !promptSubtitle.isEmpty() ? promptSubtitle : "Touch sensor to verify session";

                BiometricPrompt.Builder builder = new BiometricPrompt.Builder(activity)
                        .setTitle("CSP Daily Report")
                        .setSubtitle(sub)
                        .setDescription("Touch your device fingerprint sensor to verify identity")
                        .setNegativeButton("Cancel / Use Password", executor, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (callback != null) {
                                    callback.onError("Switched to password entry");
                                }
                            }
                        });

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        builder.setConfirmationRequired(false);
                    } catch (Throwable ignored) {}
                }

                BiometricPrompt prompt = builder.build();

                prompt.authenticate(cancellationSignal, executor, new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        String msg = errString != null ? errString.toString() : ("Biometric error (" + errorCode + ")");
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) {
                            if (callback != null) {
                                callback.onError("Scan canceled. Tap to scan again.");
                            }
                        } else {
                            if (callback != null) {
                                callback.onError(msg);
                            }
                        }
                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        super.onAuthenticationHelp(helpCode, helpString);
                        if (callback != null && helpString != null) {
                            callback.onError(helpString.toString());
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        if (callback != null) {
                            callback.onFailed();
                        }
                    }
                });
                return cancellationSignal;
            } catch (Throwable th) {
                // If BiometricPrompt fails to initialize, fallback to FingerprintManager
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                FingerprintManager fm = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
                if (fm != null && fm.isHardwareDetected() && fm.hasEnrolledFingerprints()) {
                    fm.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            String msg = errString != null ? errString.toString() : ("Fingerprint error (" + errorCode + ")");
                            if (errorCode == FingerprintManager.FINGERPRINT_ERROR_CANCELED ||
                                errorCode == FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED) {
                                if (callback != null) {
                                    callback.onError("Scan canceled. Tap to scan again.");
                                }
                            } else {
                                if (callback != null) {
                                    callback.onError(msg);
                                }
                            }
                        }

                        @Override
                        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                            super.onAuthenticationHelp(helpCode, helpString);
                            if (callback != null && helpString != null) {
                                callback.onError(helpString.toString());
                            }
                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            if (callback != null) {
                                callback.onFailed();
                            }
                        }
                    }, mainHandler);
                    return cancellationSignal;
                }
            } catch (Throwable th) {
                if (callback != null) {
                    callback.onError("Fingerprint sensor error: " + th.getMessage());
                }
                return cancellationSignal;
            }
        }

        if (callback != null) {
            callback.onError("Fingerprint sensor not available on this device");
        }
        return cancellationSignal;
    }
}
