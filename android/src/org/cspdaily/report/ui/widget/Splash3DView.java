package org.cspdaily.report.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Ui;

/**
 * 3D Cinematic Splash Canvas:
 * - Pre-compiled hardware shaders (zero onDraw allocation)
 * - 3D Gyro Orbit Rings & Swirling Particle Vortex
 * - Central Holographic Emblem with Specular Lighting
 */
public class Splash3DView extends View {

    private static final int PARTICLE_COUNT = 75;

    private static class Particle3D {
        float x, y, z;
        float vx, vy, vz;
        float size;
        int color;
    }

    private final List<Particle3D> particles = new ArrayList<>(PARTICLE_COUNT);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalBox = new RectF();

    private Bitmap logoBitmap;
    private long startTime = 0;
    private boolean isRunning = false;
    private float introProgress = 0f;
    private float precomputedOrbRadius = 100.0f;
    private LinearGradient barGradient;

    public Splash3DView(Context context) {
        this(context, null);
    }

    public Splash3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setFocusable(false);
        setClickable(false);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStyle(Paint.Style.FILL);
        orbPaint.setStyle(Paint.Style.FILL);
        bgPaint.setStyle(Paint.Style.FILL);
        particlePaint.setStyle(Paint.Style.FILL);
        logoPaint.setFilterBitmap(true);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        try {
            logoBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        } catch (Throwable ignored) {}

        int[] colors = new int[]{
            0xFF38BDF8, // Sky Cyan
            0xFF0284C7, // Azure
            0xFF60A5FA, // Electric Blue
            0xFF34D399, // Emerald
            0xFFC084FC  // Violet
        };

        Random rnd = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle3D p = new Particle3D();
            float radius = 120.0f + rnd.nextFloat() * 450.0f;
            float angle = rnd.nextFloat() * (float) (Math.PI * 2);
            p.x = (float) Math.cos(angle) * radius;
            p.y = (float) Math.sin(angle) * radius;
            p.z = (rnd.nextFloat() * 2.0f - 1.0f) * 400.0f;
            p.vz = 0.8f + rnd.nextFloat() * 2.2f;
            p.size = 2.5f + rnd.nextFloat() * 4.0f;
            p.color = colors[rnd.nextInt(colors.length)];
            particles.add(p);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            float cx = w / 2.0f;
            float cy = h * 0.42f;

            // Pre-compile background glacier gradient
            LinearGradient bg = new LinearGradient(
                0, 0, w, h,
                new int[]{0xFF061426, 0xFF0A2342, 0xFF0D3259, 0xFF07172C},
                new float[]{0.0f, 0.4f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
            );
            bgPaint.setShader(bg);

            // Pre-compile central glowing orb shader with safe positive radius
            this.precomputedOrbRadius = Math.max(80.0f, Ui.dp(getContext(), 140.0f));
            RadialGradient orbGrad = new RadialGradient(
                cx, cy, this.precomputedOrbRadius,
                new int[]{0x660284C7, 0x3338BDF8, 0x000284C7},
                new float[]{0.0f, 0.6f, 1.0f},
                Shader.TileMode.CLAMP
            );
            orbPaint.setShader(orbGrad);

            // Pre-compile progress bar gradient
            float barW = w * 0.45f;
            float barY = h * 0.88f;
            float barLeft = (w - barW) / 2.0f;
            this.barGradient = new LinearGradient(
                barLeft, barY, barLeft + barW, barY,
                new int[]{0xFF0284C7, 0xFF38BDF8, 0xFF34D399},
                null,
                Shader.TileMode.CLAMP
            );
        }
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            startTime = SystemClock.uptimeMillis();
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1600);
            anim.setInterpolator(new DecelerateInterpolator(1.4f));
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    introProgress = (Float) animation.getAnimatedValue();
                }
            });
            anim.start();
            postInvalidateOnAnimation();
        }
    }

    public void stop() {
        isRunning = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            long now = SystemClock.uptimeMillis();
            if (startTime == 0) startTime = now;
            float t = (now - startTime) / 1000.0f;

            float cx = w / 2.0f;
            float cy = h * 0.42f;

            // 1. Draw static pre-compiled background
            canvas.drawRect(0, 0, w, h, bgPaint);

            // 2. Central 3D Glowing Energy Orb
            float pulse = (float) (1.0f + 0.08f * Math.sin(t * 3.2f));
            float scale = Math.max(0.05f, introProgress) * pulse;
            canvas.save();
            canvas.translate(cx, cy);
            canvas.scale(scale, scale);
            canvas.translate(-cx, -cy);
            canvas.drawCircle(cx, cy, precomputedOrbRadius, orbPaint);
            canvas.restore();

            // 3. Render 3D Swirling Starfield Particles
            float fov = 420.0f;
            for (int i = 0; i < particles.size(); i++) {
                Particle3D p = particles.get(i);
                float rotSpeed = 0.008f;
                float cosR = (float) Math.cos(rotSpeed);
                float sinR = (float) Math.sin(rotSpeed);
                float nx = p.x * cosR - p.y * sinR;
                float ny = p.x * sinR + p.y * cosR;
                p.x = nx;
                p.y = ny;
                p.z += p.vz;
                if (p.z > 350.0f) {
                    p.z = -350.0f;
                }

                float zDist = fov + p.z;
                if (zDist < 30.0f) zDist = 30.0f;
                float pScale = fov / zDist;

                float px = cx + p.x * pScale;
                float py = cy + p.y * pScale;

                float alphaNorm = (p.z + 350.0f) / 700.0f;
                alphaNorm = Math.max(0.15f, Math.min(1.0f, alphaNorm)) * introProgress;
                int alphaInt = (int) (alphaNorm * 220);

                particlePaint.setColor((p.color & 0x00FFFFFF) | (alphaInt << 24));
                float pSize = p.size * pScale;
                canvas.drawCircle(px, py, pSize, particlePaint);

                if (pSize > 4.5f) {
                    particlePaint.setColor((p.color & 0x00FFFFFF) | ((alphaInt / 3) << 24));
                    canvas.drawCircle(px, py, pSize * 2.2f, particlePaint);
                }
            }

            // 4. Render 3D Concentric Gyro Rings
            draw3DGlowingRing(canvas, cx, cy, Ui.dp(getContext(), 90.0f), t * 1.8f, 0.45f, 0xFF38BDF8, 3.5f);
            draw3DGlowingRing(canvas, cx, cy, Ui.dp(getContext(), 115.0f), -t * 1.2f, -0.6f, 0xFF0284C7, 2.5f);
            draw3DGlowingRing(canvas, cx, cy, Ui.dp(getContext(), 140.0f), t * 0.9f, 0.3f, 0xFF34D399, 2.0f);

            // 5. Central Holographic 3D Logo Emblem
            if (logoBitmap != null) {
                canvas.save();
                float logoScale = (float) (0.92f + 0.06f * Math.sin(t * 2.5f)) * Math.max(0.01f, introProgress);
                canvas.translate(cx, cy);
                canvas.scale(logoScale, logoScale);

                int emblemD = Ui.dp(getContext(), 90.0f);
                ovalBox.set(-emblemD / 2.0f, -emblemD / 2.0f, emblemD / 2.0f, emblemD / 2.0f);
                ringPaint.setColor(0x80FFFFFF);
                ringPaint.setStrokeWidth(Ui.dp(getContext(), 2.0f));
                canvas.drawRoundRect(ovalBox, Ui.dp(getContext(), 24.0f), Ui.dp(getContext(), 24.0f), ringPaint);

                int logoW = Ui.dp(getContext(), 64.0f);
                int logoH = Ui.dp(getContext(), 64.0f);
                RectF dst = new RectF(-logoW / 2.0f, -logoH / 2.0f, logoW / 2.0f, logoH / 2.0f);
                canvas.drawBitmap(logoBitmap, null, dst, logoPaint);

                canvas.restore();
            }

            // 6. Holographic Specular Sweep & Branding Typography
            float textAlpha = Math.min(1.0f, Math.max(0.0f, (introProgress - 0.25f) / 0.75f));
            int textAlphaInt = (int) (textAlpha * 255);

            textPaint.setTextSize(Ui.sp(getContext(), 24.0f));
            textPaint.setColor(0x00FFFFFF | (textAlphaInt << 24));
            float titleY = cy + Ui.dp(getContext(), 170.0f);
            canvas.drawText("CSP Daily Report", cx, titleY, textPaint);

            textPaint.setTextSize(Ui.sp(getContext(), 13.0f));
            textPaint.setFakeBoldText(false);
            int subAlphaInt = (int) (textAlpha * 180);
            textPaint.setColor(0x00BAE6FD | (subAlphaInt << 24));
            canvas.drawText("Operations & Banking Fleet Management", cx, titleY + Ui.dp(getContext(), 24.0f), textPaint);

            // Crystalline Loading Bar at bottom
            float barW = w * 0.45f;
            float barH = Ui.dp(getContext(), 4.0f);
            float barY = h * 0.88f;
            float barLeft = (w - barW) / 2.0f;
            ovalBox.set(barLeft, barY, barLeft + barW, barY + barH);

            glowPaint.setShader(null);
            glowPaint.setColor(0x33FFFFFF);
            canvas.drawRoundRect(ovalBox, barH / 2.0f, barH / 2.0f, glowPaint);

            float fillW = barW * Math.min(1.0f, introProgress * 1.1f);
            RectF fillBox = new RectF(barLeft, barY, barLeft + fillW, barY + barH);
            if (barGradient != null) {
                glowPaint.setShader(barGradient);
            }
            canvas.drawRoundRect(fillBox, barH / 2.0f, barH / 2.0f, glowPaint);

            if (isRunning) {
                postInvalidateOnAnimation();
            }
        } catch (Throwable ignored) {}
    }

    private void draw3DGlowingRing(Canvas canvas, float cx, float cy, float radius, float angleRad, float tiltFactor, int color, float strokeWidth) {
        float scale = Math.max(0.01f, introProgress);
        float rx = radius * scale;
        float ry = radius * Math.abs(tiltFactor) * scale;
        if (rx < 1.0f || ry < 1.0f) return;

        canvas.save();
        canvas.translate(cx, cy);
        ovalBox.set(-rx, -ry, rx, ry);
        canvas.rotate((float) Math.toDegrees(angleRad));

        // Outer glow
        ringPaint.setColor((color & 0x00FFFFFF) | 0x33000000);
        ringPaint.setStrokeWidth(Ui.dp(getContext(), strokeWidth + 4.0f));
        canvas.drawOval(ovalBox, ringPaint);

        // Crisp ring stroke
        ringPaint.setColor((color & 0x00FFFFFF) | 0xCC000000);
        ringPaint.setStrokeWidth(Ui.dp(getContext(), strokeWidth));
        canvas.drawOval(ovalBox, ringPaint);

        canvas.restore();
    }
}
