package org.cspdaily.report.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * 3D Motion, perspective flips, spring physics, and tactile micro-interaction animations.
 */
public final class Anim3D {

    private Anim3D() {}

    /**
     * Staggered 3D perspective flip-in cascade entrance.
     */
    public static void flipIn3D(final View view, int index) {
        if (view == null) return;
        view.setAlpha(1.0f);
        view.setTranslationY(0.0f);
        view.setRotationX(0.0f);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
    }

    /**
     * 3D Depth Pop animation for KPI badges, counters, and highlights.
     */
    public static void pop3D(View view, int delayMs) {
        if (view == null) return;
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(1.0f);
    }

    /**
     * Attaches tactile 3D spring press feedback on any view.
     */
    public static void attachTactilePress(final View view) {
        if (view == null) return;
        final float density = view.getResources().getDisplayMetrics().density;
        view.setCameraDistance(8000.0f * density);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .translationZ(Ui.dp(v.getContext(), 2.0f))
                            .setDuration(120)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationZ(0f)
                            .setDuration(240)
                            .setInterpolator(new OvershootInterpolator(1.4f))
                            .start();
                        break;
                }
                return false;
            }
        });
    }

    /**
     * Ambient 3D floating levitation effect.
     */
    public static void startFloatLevitation(final View view) {
        if (view == null) return;
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(view, "translationY", 0f, -Ui.dp(view.getContext(), 6.0f), 0f);
        floatAnim.setDuration(2800);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setRepeatMode(ValueAnimator.RESTART);
        floatAnim.setInterpolator(new DecelerateInterpolator());
        floatAnim.start();
    }
}
