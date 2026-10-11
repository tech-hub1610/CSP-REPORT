package org.cspdaily.report.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import org.cspdaily.report.ui.Ui;

/**
 * Modern Glassmorphic Container with interactive 3D perspective tilt physics,
 * specular dynamic light reflections, frosted translucent surface styling,
 * and squircle rounded corners.
 */
public class GlassCardView extends LinearLayout {

    private float cornerRadius;
    private boolean isDarkGlass = false;
    private boolean isHeroGlass = false;
    private boolean isObsidianGold = false;
    private boolean enable3DTilt = true;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final Path clipPath = new Path();

    private float touchX = -1f;
    private float touchY = -1f;
    private float shineAlpha = 0f;
    private ValueAnimator returnAnim;

    public GlassCardView(Context context) {
        this(context, null);
    }

    public GlassCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setWillNotDraw(false);
        this.cornerRadius = Ui.dp(context, 16.0f);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Ui.dp(context, 1.0f));

        if (Build.VERSION.SDK_INT >= 21) {
            setElevation(Ui.dp(context, 2.0f));
        }

        float density = getResources().getDisplayMetrics().density;
        setCameraDistance(10000.0f * density);
    }

    public void setCornerRadius(float dp) {
        this.cornerRadius = Ui.dp(getContext(), dp);
        updateGradients();
        invalidate();
    }

    public void setDarkGlass(boolean dark) {
        this.isDarkGlass = dark;
        updateGradients();
        invalidate();
    }

    public void setHeroGlass(boolean hero) {
        this.isHeroGlass = hero;
        updateGradients();
        invalidate();
    }

    public void setObsidianGold(boolean gold) {
        this.isObsidianGold = gold;
        updateGradients();
        invalidate();
    }

    public void setEnable3DTilt(boolean enable) {
        this.enable3DTilt = enable;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bounds.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);
        updateGradients();
    }

    private void updateGradients() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (isObsidianGold) {
            // Executive SuperAdmin Sunset Warm White / Gold
            LinearGradient goldGrad = new LinearGradient(
                0, 0, w * 0.9f, h,
                new int[]{0xFFFFFFFF, 0xFFFFFBEB, 0xFFFEF3C7},
                new float[]{0.0f, 0.55f, 1.0f},
                Shader.TileMode.CLAMP
            );
            bgPaint.setShader(goldGrad);

            LinearGradient strokeGrad = new LinearGradient(
                0, 0, w, h,
                new int[]{0xFFF59E0B, 0xFFD97706, 0xFFFCD34D},
                new float[]{0.0f, 0.45f, 1.0f},
                Shader.TileMode.CLAMP
            );
            strokePaint.setShader(strokeGrad);
        } else if (isHeroGlass) {
            // Modern CRM Hero Header: Deep Slate (#0F172A) to Rich Azure (#0284C7)
            LinearGradient heroGrad = new LinearGradient(
                0, 0, w * 0.95f, h,
                new int[]{0xFF0F172A, 0xFF075985, 0xFF0284C7},
                new float[]{0.0f, 0.55f, 1.0f},
                Shader.TileMode.CLAMP
            );
            bgPaint.setShader(heroGrad);

            LinearGradient strokeGrad = new LinearGradient(
                0, 0, w, h,
                new int[]{0x4D38BDF8, 0x33FFFFFF, 0x1A0284C7},
                new float[]{0.0f, 0.4f, 1.0f},
                Shader.TileMode.CLAMP
            );
            strokePaint.setShader(strokeGrad);
        } else if (isDarkGlass) {
            // Professional Dark Slate
            LinearGradient darkGrad = new LinearGradient(
                0, 0, 0, h,
                new int[]{0xFF0F172A, 0xFF1E293B},
                new float[]{0.0f, 1.0f},
                Shader.TileMode.CLAMP
            );
            bgPaint.setShader(darkGrad);

            LinearGradient strokeGrad = new LinearGradient(
                0, 0, 0, h,
                new int[]{0xFF334155, 0xFF1E293B},
                new float[]{0.0f, 1.0f},
                Shader.TileMode.CLAMP
            );
            strokePaint.setShader(strokeGrad);
        } else {
            // Clean Crisp White SaaS Card with subtle line border
            bgPaint.setShader(null);
            bgPaint.setColor(0xFFFFFFFF);

            strokePaint.setShader(null);
            strokePaint.setColor(0xFFE2E8F0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enable3DTilt || !isClickable() && !isFocusable() && getChildCount() > 0) {
            // Even if container isn't directly clicked, apply interactive 3D tilt
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (returnAnim != null) returnAnim.cancel();
                touchX = event.getX();
                touchY = event.getY();
                shineAlpha = 0.35f;
                apply3DTilt(touchX, touchY);
                break;

            case MotionEvent.ACTION_MOVE:
                touchX = event.getX();
                touchY = event.getY();
                apply3DTilt(touchX, touchY);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                reset3DTiltWithSpring();
                break;
        }

        return super.onTouchEvent(event);
    }

    private void apply3DTilt(float x, float y) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Normalized relative coordinates (-1.0 to +1.0)
        float normX = (x / (float) w) * 2.0f - 1.0f;
        float normY = (y / (float) h) * 2.0f - 1.0f;

        // Tilt angles: Y rotation corresponds to horizontal displacement, X to vertical
        float maxTiltDeg = 6.5f;
        float targetRotY = normX * maxTiltDeg;
        float targetRotX = -normY * maxTiltDeg;

        setRotationX(targetRotX);
        setRotationY(targetRotY);
        setScaleX(1.012f);
        setScaleY(1.012f);

        invalidate();
    }

    private void reset3DTiltWithSpring() {
        final float startRotX = getRotationX();
        final float startRotY = getRotationY();
        final float startScaleX = getScaleX();
        final float startShine = shineAlpha;

        returnAnim = ValueAnimator.ofFloat(0f, 1f);
        returnAnim.setDuration(350);
        returnAnim.setInterpolator(new OvershootInterpolator(1.3f));
        returnAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float f = animation.getAnimatedFraction();
                setRotationX(startRotX * (1.0f - f));
                setRotationY(startRotY * (1.0f - f));
                float s = startScaleX + (1.0f - startScaleX) * f;
                setScaleX(s);
                setScaleY(s);
                shineAlpha = startShine * (1.0f - f);
                invalidate();
            }
        });
        returnAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                touchX = -1f;
                touchY = -1f;
                shineAlpha = 0f;
                setRotationX(0f);
                setRotationY(0f);
                setScaleX(1.0f);
                setScaleY(1.0f);
                invalidate();
            }
        });
        returnAnim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            super.onDraw(canvas);
            return;
        }

        // 1. Draw rounded glass background
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, bgPaint);

        // 2. Draw specular border stroke
        float strokeInset = strokePaint.getStrokeWidth() * 0.5f;
        RectF strokeBounds = new RectF(
            strokeInset, strokeInset,
            w - strokeInset, h - strokeInset
        );
        canvas.drawRoundRect(strokeBounds, cornerRadius - strokeInset, cornerRadius - strokeInset, strokePaint);

        // 3. Draw dynamic interactive light specular reflection if touched
        if (touchX >= 0 && touchY >= 0 && shineAlpha > 0.01f) {
            canvas.save();
            canvas.clipPath(clipPath);

            int shineColor = isObsidianGold ? 0x66F59E0B : ((isHeroGlass || isDarkGlass) ? 0x4D38BDF8 : 0x55FFFFFF);
            RadialGradient shineGrad = new RadialGradient(
                touchX, touchY, Math.max(w, h) * 0.65f,
                shineColor,
                0x00FFFFFF,
                Shader.TileMode.CLAMP
            );
            shinePaint.setShader(shineGrad);
            canvas.drawRect(bounds, shinePaint);

            canvas.restore();
        }

        super.onDraw(canvas);
    }
}
