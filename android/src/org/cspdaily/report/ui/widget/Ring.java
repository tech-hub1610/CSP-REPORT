package org.cspdaily.report.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import org.cspdaily.report.R;
import org.cspdaily.report.ui.Ui;

/**
 * Modern 3D Depth Progress Ring with glowing neon gradient arc and smooth animation.
 */
public class Ring extends View {
    private final Paint arc;
    private final Paint glowArc;
    private final RectF box;
    private final Paint caption;
    private final Paint label;
    private final Paint track;

    private float currentPct = 0.0f;
    private float targetPct = 0.0f;
    private String sub = "";
    private String text = "";
    private boolean isDark = false;
    private ValueAnimator sweepAnim;

    public Ring(Context context) {
        this(context, null);
    }

    public Ring(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.track = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.glowArc = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.label = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.caption = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.box = new RectF();

        int strokeWidth = Ui.dp(context, 7.5f);
        this.track.setStyle(Paint.Style.STROKE);
        this.track.setStrokeWidth(strokeWidth);
        this.track.setColor(Ui.color(context, R.color.fill_hover));

        this.arc.setStyle(Paint.Style.STROKE);
        this.arc.setStrokeCap(Paint.Cap.ROUND);
        this.arc.setStrokeWidth(strokeWidth);

        this.glowArc.setStyle(Paint.Style.STROKE);
        this.glowArc.setStrokeCap(Paint.Cap.ROUND);
        this.glowArc.setStrokeWidth(strokeWidth + Ui.dp(context, 5.0f));

        this.label.setStyle(Paint.Style.FILL);
        this.label.setTextAlign(Paint.Align.CENTER);
        this.label.setTextSize(Ui.sp(context, 17.5f));
        this.label.setFakeBoldText(true);
        this.label.setColor(Ui.color(context, R.color.ink));

        this.caption.setStyle(Paint.Style.FILL);
        this.caption.setTextAlign(Paint.Align.CENTER);
        this.caption.setTextSize(Ui.sp(context, 9.5f));
        this.caption.setColor(Ui.color(context, R.color.ink_3));
    }

    public void set(float pctValue, String labelText, String subText) {
        this.targetPct = Math.max(0.0f, Math.min(100.0f, pctValue));
        this.text = (labelText == null) ? "" : labelText;
        this.sub = (subText == null) ? "" : subText;

        if (sweepAnim != null) sweepAnim.cancel();
        sweepAnim = ValueAnimator.ofFloat(currentPct, targetPct);
        sweepAnim.setDuration(450);
        sweepAnim.setInterpolator(new DecelerateInterpolator(1.4f));
        sweepAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentPct = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        sweepAnim.start();
    }

    public void setOnDarkSurface(boolean dark) {
        this.isDark = dark;
        Context context = getContext();
        this.track.setColor(Ui.color(context, dark ? R.color.hero_divider : R.color.fill_hover));
        this.label.setColor(Ui.color(context, dark ? R.color.paper : R.color.ink));
        this.caption.setColor(Ui.color(context, dark ? R.color.hero_text_dim : R.color.ink_3));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dp = Ui.dp(getContext(), 68.0f);
        int min = Math.min(resolveSize(dp, widthMeasureSpec), resolveSize(dp, heightMeasureSpec));
        setMeasuredDimension(min, min);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            Context ctx = getContext();
            if (isDark) {
                LinearGradient grad = new LinearGradient(
                    0, 0, w, h,
                    new int[]{0xFF38BDF8, 0xFF34D399},
                    null,
                    Shader.TileMode.CLAMP
                );
                arc.setShader(grad);
                glowArc.setColor(0x4038BDF8);
            } else {
                LinearGradient grad = new LinearGradient(
                    0, 0, w, h,
                    new int[]{Ui.color(ctx, R.color.primary_600), Ui.color(ctx, R.color.ok)},
                    null,
                    Shader.TileMode.CLAMP
                );
                arc.setShader(grad);
                glowArc.setColor(0x332563EB);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float strokeWidth = this.arc.getStrokeWidth();
        float inset = strokeWidth / 2.0f + Ui.dp(getContext(), 2.0f);
        this.box.set(inset, inset, w - inset, h - inset);

        // 1. Draw base track ring
        canvas.drawArc(this.box, 0.0f, 360.0f, false, this.track);

        // 2. Draw glowing progress arc
        float sweepAngle = (this.currentPct * 360.0f) / 100.0f;
        if (sweepAngle > 0.5f) {
            canvas.drawArc(this.box, -90.0f, sweepAngle, false, this.glowArc);
            canvas.drawArc(this.box, -90.0f, sweepAngle, false, this.arc);
        }

        // 3. Draw center metric and caption
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        Paint.FontMetrics fm = this.label.getFontMetrics();
        float textY;
        if (this.sub.isEmpty()) {
            textY = cy - ((fm.descent + fm.ascent) / 2.0f);
        } else {
            textY = (cy - (fm.ascent / 2.0f)) - Ui.dp(getContext(), 3.0f);
        }
        canvas.drawText(this.text, cx, textY, this.label);

        if (!this.sub.isEmpty()) {
            Paint.FontMetrics subFm = this.caption.getFontMetrics();
            canvas.drawText(this.sub, cx, (subFm.descent / 2.0f) + ((cy + Ui.dp(getContext(), 9.0f)) - (subFm.ascent / 2.0f)), this.caption);
        }
    }
}
