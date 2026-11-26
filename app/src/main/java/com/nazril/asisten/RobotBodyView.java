package com.nazril.asisten;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class RobotBodyView extends View {
    public static final int BALL = 0, ROUND = 1, BOX = 2;
    private int shape;
    private long lastTouch = System.currentTimeMillis();
    private float lie = 0f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RobotBodyView(Context ctx) {
        super(ctx);
        shape = ctx.getSharedPreferences("robot", Context.MODE_PRIVATE).getInt("shape", ROUND);
    }

    public void setShape(int s) {
        shape = s;
        getContext().getSharedPreferences("robot", Context.MODE_PRIVATE)
            .edit().putInt("shape", s).apply();
        invalidate();
    }

    public int getShape() { return shape; }
    public void poke() { lastTouch = System.currentTimeMillis(); }

    private void limb(Canvas c, float x, float y, float deg, float len) {
        double a = Math.toRadians(deg);
        c.drawLine(x, y, x + (float) Math.sin(a) * len, y + (float) Math.cos(a) * len, p);
    }

    @Override protected void onDraw(Canvas c) {
        long now = System.currentTimeMillis();
        float t = (now % 100000) / 1000f;
        long idle = now - lastTouch;
        boolean sleep = idle > 60000, play = !sleep && idle > 15000;
        lie += ((sleep ? 1f : 0f) - lie) * 0.08f;

        float w = getWidth(), h = getHeight(), s = Math.min(w, h);
        float r = (shape == BALL) ? s * 0.38f : s * 0.24f;
        float bob = play ? -Math.abs((float) Math.sin(t * 5)) * s * 0.06f : 0f;

        c.save();
        c.translate(w / 2f, h / 2f);
        c.rotate(lie * 90f);
        c.translate(0, bob);

        if (shape != BALL) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.06f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(0xFFF472B6);
            float armSwing = sleep ? 0 : play ? (float) Math.sin(t * 6) * 60 : (float) Math.sin(t * 2) * 10;
            float legSwing = sleep ? 0 : play ? (float) Math.sin(t * 5) * 35 : (float) Math.sin(t * 1.5) * 5;
            float len = s * 0.2f;
            limb(c, -r, 0, 20 + armSwing, len);
            limb(c, r, 0, -20 - armSwing, len);
            limb(c, -r * 0.5f, r * 1.1f, legSwing, len);
            limb(c, r * 0.5f, r * 1.1f, -legSwing, len);
        }

        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFEC4899);
        if (shape == BALL) c.drawCircle(0, 0, r, p);
        else if (shape == ROUND) c.drawRoundRect(new RectF(-r, -r * 1.2f, r, r * 1.2f), r, r, p);
        else c.drawRoundRect(new RectF(-r, -r * 1.1f, r, r * 1.1f), r * 0.2f, r * 0.2f, p);

        // mata
        boolean closed = sleep || (t % 4f) < 0.15f;
        p.setColor(0xFFFFFFFF);
        float ex = r * 0.4f, ey = -r * 0.25f;
        if (closed) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.025f);
            c.drawLine(-ex - r * 0.12f, ey, -ex + r * 0.12f, ey, p);
            c.drawLine(ex - r * 0.12f, ey, ex + r * 0.12f, ey, p);
        } else {
            c.drawCircle(-ex, ey, r * 0.12f, p);
            c.drawCircle(ex, ey, r * 0.12f, p);
        }
        c.restore();

        if (lie > 0.8f) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFFFFF);
            p.setTextSize(s * 0.2f);
            c.drawText("z", w * 0.7f, h * 0.3f - (t % 2) * 4, p);
        }
        postInvalidateOnAnimation();
    }
    @Override protected void onMeasure(int w, int h) {
        int d = (int) (getResources().getDisplayMetrics().density * 72);
        setMeasuredDimension(resolveSize(d, w), resolveSize(d, h));
    }
}
