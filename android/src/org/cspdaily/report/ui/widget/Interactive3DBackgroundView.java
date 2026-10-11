package org.cspdaily.report.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.cspdaily.report.ui.Ui;

/**
 * High-performance 3D interactive ambient particle & mesh canvas.
 * Projects 3D node coordinates onto a 2D viewport with perspective distortion,
 * depth-based z-sorting, touch parallax tilt, and ambient gyroscope response.
 */
public class Interactive3DBackgroundView extends View implements SensorEventListener {

    private static final int NODE_COUNT = 36;
    private static final float MAX_CONNECT_DISTANCE = 220.0f;

    private static class Node3D {
        float x, y, z;          // 3D coordinates in space (-1.0 to 1.0)
        float origX, origY, origZ;
        float vx, vy, vz;       // Velocities
        float size;             // Projected base radius
        int color;              // Glowing color
        float pulsePhase;
        float pulseSpeed;
    }

    private final List<Node3D> nodes = new ArrayList<>(NODE_COUNT);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float rotX = 0f;
    private float rotY = 0f;
    private float targetRotX = 0f;
    private float targetRotY = 0f;
    private float touchStartX = 0f;
    private float touchStartY = 0f;
    private boolean isTouching = false;

    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private long lastFrameTime = 0;
    private boolean isRunning = false;

    // Projected coordinates buffer
    private final float[] projX = new float[NODE_COUNT];
    private final float[] projY = new float[NODE_COUNT];
    private final float[] projScale = new float[NODE_COUNT];
    private final float[] projAlpha = new float[NODE_COUNT];

    // Ambient floating orbs in background
    private float orb1Angle = 0f;
    private float orb2Angle = (float) Math.PI;
    private RadialGradient orb1Grad;
    private RadialGradient orb2Grad;

    public Interactive3DBackgroundView(Context context) {
        this(context, null);
    }

    public Interactive3DBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setFocusable(false);
        setClickable(false);

        nodePaint.setStyle(Paint.Style.FILL);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStyle(Paint.Style.FILL);

        int[] colors = new int[]{
            0xFF0284C7, // Sky Blue
            0xFF0369A1, // Deep Ice Azure
            0xFF0891B2, // Crystal Cyan
            0xFF2563EB, // Electric Blue
            0xFF0D9488  // Arctic Teal
        };

        Random rnd = new Random();
        for (int i = 0; i < NODE_COUNT; i++) {
            Node3D n = new Node3D();
            n.origX = n.x = (rnd.nextFloat() * 2.0f - 1.0f) * 400.0f;
            n.origY = n.y = (rnd.nextFloat() * 2.0f - 1.0f) * 600.0f;
            n.origZ = n.z = (rnd.nextFloat() * 2.0f - 1.0f) * 350.0f;
            n.vx = (rnd.nextFloat() - 0.5f) * 0.45f;
            n.vy = (rnd.nextFloat() - 0.5f) * 0.45f;
            n.vz = (rnd.nextFloat() - 0.5f) * 0.35f;
            n.size = 3.5f + rnd.nextFloat() * 3.5f;
            n.color = colors[rnd.nextInt(colors.length)];
            n.pulsePhase = rnd.nextFloat() * (float) (Math.PI * 2);
            n.pulseSpeed = 0.02f + rnd.nextFloat() * 0.03f;
            nodes.add(n);
        }

        try {
            sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
        if (sensorManager != null && gyroSensor != null) {
            try {
                sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
        if (sensorManager != null) {
            try {
                sensorManager.unregisterListener(this);
            } catch (Throwable ignored) {}
        }
    }

    public void startAnimation() {
        if (!isRunning) {
            isRunning = true;
            lastFrameTime = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
        }
    }

    public void stopAnimation() {
        isRunning = false;
    }

    private boolean isSuperAdminMode = false;

    public void setSuperAdminMode(boolean superAdmin) {
        this.isSuperAdminMode = superAdmin;
        initNodes();
        updateBgShaders();
        invalidate();
    }

    private void updateBgShaders() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (isSuperAdminMode) {
            // Executive SuperAdmin Royal Sunset Aura Canvas
            LinearGradient bg = new LinearGradient(
                0, 0, w, h,
                new int[]{0xFFFDFBF7, 0xFFFBF4EB, 0xFFF8EDE0, 0xFFFDF9F3},
                new float[]{0.0f, 0.35f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
            );
            bgGradientPaint.setShader(bg);

            float r1 = Math.max(50.0f, w * 0.55f);
            orb1Grad = new RadialGradient(
                w * 0.3f, h * 0.25f, r1,
                0x33F59E0B, 0x00000000,
                Shader.TileMode.CLAMP
            );

            float r2 = Math.max(50.0f, w * 0.60f);
            orb2Grad = new RadialGradient(
                w * 0.7f, h * 0.65f, r2,
                0x2BEA580C, 0x00000000,
                Shader.TileMode.CLAMP
            );
        } else {
            // Frosted Ice crystal background gradient
            LinearGradient bg = new LinearGradient(
                0, 0, w, h,
                new int[]{0xFFEDF5FC, 0xFFE1EFF9, 0xFFD8EAF8, 0xFFEAF3FB},
                new float[]{0.0f, 0.35f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
            );
            bgGradientPaint.setShader(bg);

            float r1 = Math.max(50.0f, w * 0.55f);
            orb1Grad = new RadialGradient(
                w * 0.3f, h * 0.25f, r1,
                0x240284C7, 0x00000000,
                Shader.TileMode.CLAMP
            );

            float r2 = Math.max(50.0f, w * 0.60f);
            orb2Grad = new RadialGradient(
                w * 0.7f, h * 0.65f, r2,
                0x2638BDF8, 0x00000000,
                Shader.TileMode.CLAMP
            );
        }
    }

    private void initNodes() {
        nodes.clear();
        int[] colors;
        if (isSuperAdminMode) {
            colors = new int[]{
                0xFFD97706, // Warm Amber
                0xFFEA580C, // Sunset Flame
                0xFFF59E0B, // 24K Gold
                0xFFEAB308, // Sunlit Topaz
                0xFFB45309  // Royal Bronze
            };
        } else {
            colors = new int[]{
                0xFF0284C7, // Sky Blue
                0xFF0369A1, // Deep Ice Azure
                0xFF0891B2, // Crystal Cyan
                0xFF2563EB, // Electric Blue
                0xFF0D9488  // Arctic Teal
            };
        }

        Random rnd = new Random();
        for (int i = 0; i < NODE_COUNT; i++) {
            Node3D n = new Node3D();
            n.origX = n.x = (rnd.nextFloat() * 2.0f - 1.0f) * 400.0f;
            n.origY = n.y = (rnd.nextFloat() * 2.0f - 1.0f) * 600.0f;
            n.origZ = n.z = (rnd.nextFloat() * 2.0f - 1.0f) * 350.0f;
            n.vx = (rnd.nextFloat() - 0.5f) * 0.45f;
            n.vy = (rnd.nextFloat() - 0.5f) * 0.45f;
            n.vz = (rnd.nextFloat() - 0.5f) * 0.35f;
            n.size = 3.5f + rnd.nextFloat() * 3.5f;
            n.color = colors[rnd.nextInt(colors.length)];
            n.pulsePhase = rnd.nextFloat() * (float) (Math.PI * 2);
            n.pulseSpeed = 0.02f + rnd.nextFloat() * 0.03f;
            nodes.add(n);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBgShaders();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                isTouching = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                targetRotY = dx * 0.0015f;
                targetRotX = -dy * 0.0015f;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                targetRotX = 0f;
                targetRotY = 0f;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && !isTouching) {
                float[] rotationMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                targetRotX = orientation[1] * 0.35f;
                targetRotY = orientation[2] * 0.35f;
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            long now = SystemClock.uptimeMillis();
            float dt = (lastFrameTime == 0) ? 0.016f : Math.min((now - lastFrameTime) / 1000f, 0.05f);
            lastFrameTime = now;

            // 1. Draw dynamic Frosted Ice gradient
            canvas.drawRect(0, 0, w, h, bgGradientPaint);

            // 2. Draw ambient crystal glowing orbs
            orb1Angle += 0.008f;
            orb2Angle += 0.006f;
            float orb1X = w * 0.3f + (float) Math.cos(orb1Angle) * (w * 0.25f);
            float orb1Y = h * 0.25f + (float) Math.sin(orb1Angle * 1.3f) * (h * 0.15f);
            if (orb1Grad != null) {
                glowPaint.setShader(orb1Grad);
                canvas.drawCircle(orb1X, orb1Y, w * 0.55f, glowPaint);
            }

            float orb2X = w * 0.7f + (float) Math.sin(orb2Angle) * (w * 0.22f);
            float orb2Y = h * 0.65f + (float) Math.cos(orb2Angle * 0.9f) * (h * 0.18f);
            if (orb2Grad != null) {
                glowPaint.setShader(orb2Grad);
                canvas.drawCircle(orb2X, orb2Y, w * 0.60f, glowPaint);
            }

            // 3. Smooth rotation damping
            rotX += (targetRotX - rotX) * 0.08f;
            rotY += (targetRotY - rotY) * 0.08f;

        float autoRotY = (float) (now * 0.00018);
        float totalRotX = rotX;
        float totalRotY = rotY + autoRotY;

        float cosX = (float) Math.cos(totalRotX);
        float sinX = (float) Math.sin(totalRotX);
        float cosY = (float) Math.cos(totalRotY);
        float sinY = (float) Math.sin(totalRotY);

        float cx = w / 2.0f;
        float cy = h / 2.0f;
        float fov = 500.0f;

        // 4. Update & project 3D coordinates
        for (int i = 0; i < nodes.size(); i++) {
            Node3D n = nodes.get(i);
            n.x += n.vx;
            n.y += n.vy;
            n.z += n.vz;

            if (Math.abs(n.x) > 380f) n.vx *= -1f;
            if (Math.abs(n.y) > 550f) n.vy *= -1f;
            if (Math.abs(n.z) > 320f) n.vz *= -1f;

            n.pulsePhase += n.pulseSpeed;

            float x1 = n.x * cosY + n.z * sinY;
            float z1 = -n.x * sinY + n.z * cosY;
            float y1 = n.y * cosX - z1 * sinX;
            float z2 = n.y * sinX + z1 * cosX;

            float zDist = fov + z2;
            if (zDist < 50.0f) zDist = 50.0f;
            float scale = fov / zDist;

            projX[i] = cx + x1 * scale;
            projY[i] = cy + y1 * scale;
            projScale[i] = scale;

            float alphaNorm = (z2 + 350f) / 700f;
            if (alphaNorm < 0.25f) alphaNorm = 0.25f;
            if (alphaNorm > 1.0f) alphaNorm = 1.0f;
            projAlpha[i] = alphaNorm;
        }

        // 5. Draw 3D connecting filament lines
        float maxDistSq = MAX_CONNECT_DISTANCE * MAX_CONNECT_DISTANCE;
        for (int i = 0; i < nodes.size(); i++) {
            float xA = projX[i];
            float yA = projY[i];
            for (int j = i + 1; j < nodes.size(); j++) {
                float xB = projX[j];
                float yB = projY[j];
                float dx = xB - xA;
                float dy = yB - yA;
                float distSq = dx * dx + dy * dy;

                if (distSq < maxDistSq) {
                    float dist = (float) Math.sqrt(distSq);
                    float lineAlpha = (1.0f - dist / MAX_CONNECT_DISTANCE) * Math.min(projAlpha[i], projAlpha[j]) * 0.45f;
                    int alphaInt = (int) (lineAlpha * 255);
                    if (alphaInt > 2) {
                        linePaint.setColor(0x000284C7 | (alphaInt << 24));
                        linePaint.setStrokeWidth(1.2f * ((projScale[i] + projScale[j]) * 0.5f));
                        canvas.drawLine(xA, yA, xB, yB, linePaint);
                    }
                }
            }
        }

        // 6. Draw 3D nodes with glowing aura
        for (int i = 0; i < nodes.size(); i++) {
            Node3D n = nodes.get(i);
            float x = projX[i];
            float y = projY[i];
            float scale = projScale[i];
            float alpha = projAlpha[i];

            float pulse = (float) (0.85f + 0.3f * Math.sin(n.pulsePhase));
            float r = n.size * scale * pulse;

            // Outer glow aura
            int glowAlpha = (int) (alpha * 65);
            nodePaint.setColor((n.color & 0x00FFFFFF) | (glowAlpha << 24));
            canvas.drawCircle(x, y, r * 2.8f, nodePaint);

            // Core bright dot
            int coreAlpha = (int) (alpha * 240);
            nodePaint.setColor((n.color & 0x00FFFFFF) | (coreAlpha << 24));
            canvas.drawCircle(x, y, r, nodePaint);
        }

        if (isRunning) {
            postInvalidateOnAnimation();
        }
        } catch (Throwable ignored) {}
    }
}
