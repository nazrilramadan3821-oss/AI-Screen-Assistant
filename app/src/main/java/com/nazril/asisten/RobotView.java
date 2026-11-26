package com.nazril.asisten;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

public class RobotView extends TextView {
    public static final int HAPPY = 0, THINK = 1, TALK = 2, WOW = 3, SLEEP = 4;
    private static RobotView inst;

    private static final String[] NAMES = {"Biru bulat", "Ungu kotak", "Hijau oval", "Oranye", "Pink", "Gelap"};
    private static final int[] COLORS = {0xFF3B82F6, 0xFF8B5CF6, 0xFF10B981, 0xFFF97316, 0xFFEC4899, 0xFF334155};
    private static final int[] HEAD = {0, 1, 0, 1, 0, 1};
    private static final int[] EYE = {0, 2, 1, 0, 1, 2};
    private static final boolean[] ANT = {true, true, true, false, true, true};
    private static final boolean[] CHEEK = {true, true, false, true, true, false};

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Handler h = new Handler(Looper.getMainLooper());
    private int pre, mood = HAPPY;
    private long moodUntil, lastTouch, blinkAt, blinkEnd, downAt;
    private float lookX, lookY, dx0, dy0, lx, ly;
    private boolean lp, moved;

    private final Runnable longPress = new Runnable() {
        @Override public void run() { lp = true; cycle(); }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() { step(); invalidate(); h.postDelayed(this, 66); }
    };

    public RobotView(Context c) {
        super(c);
        setWillNotDraw(false);
        pre = c.getSharedPreferences("ai_robot", Context.MODE_PRIVATE).getInt("pre", 0) % NAMES.length;
        lastTouch = SystemClock.uptimeMillis();
        blinkAt = lastTouch + 2000;
    }

    public static void think() { send(THINK, 30000); }
    public static void talk() { send(TALK, 3500); }
    public static void wow() { send(WOW, 1200); }
    public static void idle() { send(HAPPY, 0); }

    private static void send(final int m, final long ms) {
        final RobotView v = inst;
        if (v != null) v.h.post(new Runnable() { @Override public void run() { v.setMood(m, ms); } });
    }

    private void setMood(int m, long ms) {
        mood = m;
        moodUntil = ms > 0 ? SystemClock.uptimeMillis() + ms : 0;
        if (m == HAPPY) lastTouch = SystemClock.uptimeMillis();
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); inst = this; h.post(tick); }
    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        h.removeCallbacksAndMessages(null);
        if (inst == this) inst = null;
    }

    @Override public void setBackground(Drawable d) { }
    @Override public void setBackgroundDrawable(Drawable d) { }
    @Override public void setBackgroundColor(int c) { }
    @Override public void setBackgroundResource(int res) { }

    private void step() {
        long now = SystemClock.uptimeMillis();
        if (moodUntil > 0 && now > moodUntil) { mood = HAPPY; moodUntil = 0; lastTouch = now; }
        if (mood == HAPPY && now - lastTouch > 45000) mood = SLEEP;
        if (now >= blinkAt) { blinkEnd = now + 130; blinkAt = now + 2500 + (long) (Math.random() * 3000); }
        lookX *= 0.8f; lookY *= 0.8f;
    }

    private void cycle() {
        pre = (pre + 1) % NAMES.length;
        getContext().getSharedPreferences("ai_robot", Context.MODE_PRIVATE).edit().putInt("pre", pre).apply();
        MotionEvent c = MotionEvent.obtain(downAt, SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, dx0, dy0, 0);
        super.dispatchTouchEvent(c);
        c.recycle();
        setMood(WOW, 900);
        try {
            Toast.makeText(getContext().getApplicationContext(), "Gaya robot: " + NAMES[pre], Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) { }
        invalidate();
    }

    private static float clamp(float v) { return Math.max(-1f, Math.min(1f, v)); }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        long now = SystemClock.uptimeMillis();
        lastTouch = now;
        if (mood == SLEEP) { mood = HAPPY; moodUntil = 0; }
        if (lp) {
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) lp = false;
            return true;
        }
        if (a == MotionEvent.ACTION_DOWN) {
            dx0 = e.getRawX(); dy0 = e.getRawY(); lx = dx0; ly = dy0; moved = false; downAt = now;
            h.postDelayed(longPress, 700);
        } else if (a == MotionEvent.ACTION_MOVE) {
            float rx = e.getRawX(), ry = e.getRawY();
            lookX = clamp((rx - lx) / 6f); lookY = clamp((ry - ly) / 6f);
            lx = rx; ly = ry;
            if (!moved && Math.hypot(rx - dx0, ry - dy0) > 24) { moved = true; h.removeCallbacks(longPress); }
        } else if (a == MotionEvent.ACTION_UP) {
            h.removeCallbacks(longPress);
            if (!moved && now - downAt < 400) setMood(WOW, 900);
        } else if (a == MotionEvent.ACTION_CANCEL) {
            h.removeCallbacks(longPress);
        }
        return super.dispatchTouchEvent(e);
    }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), hh = getHeight();
        float s = Math.min(w, hh);
        if (s <= 0) return;
        long now = SystemClock.uptimeMillis();
        float cx = w / 2f, oy = (hh - s) / 2f, cy = oy + 0.58f * s, hr = 0.38f * s;
        boolean blink = now < blinkEnd, sleep = mood == SLEEP, wow = mood == WOW;

        p.setStyle(Paint.Style.FILL);
        p.setColor(COLORS[pre]);
        if (HEAD[pre] == 0) c.drawCircle(cx, cy, hr, p);
        else { r.set(cx - hr, cy - hr, cx + hr, cy + hr); c.drawRoundRect(r, hr * 0.45f, hr * 0.45f, p); }

        if (ANT[pre]) {
            float sway = wow ? (float) Math.sin(now / 60.0) * s * 0.05f : 0f;
            float ty = oy + 0.10f * s;
            p.setColor(0xFFFFFFFF);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.045f);
            p.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(cx, cy - hr + s * 0.02f, cx + sway, ty, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFEF4444);
            c.drawCircle(cx + sway, ty, s * 0.06f, p);
        }

        if (CHEEK[pre]) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x80FB7185);
            c.drawCircle(cx - hr * 0.68f, cy + hr * 0.22f, hr * 0.1f, p);
            c.drawCircle(cx + hr * 0.68f, cy + hr * 0.22f, hr * 0.1f, p);
        }

        float ey = cy - hr * 0.12f, edx = hr * 0.42f, er = hr * 0.24f;
        float sc = wow ? 1.25f : 1f, kv = (blink || sleep) ? 0.12f : 1f;
        float px = lookX, py = lookY;
        if (mood == THINK) { px = 0.75f; py = -0.8f; }
        for (int i = -1; i <= 1; i += 2) {
            float ex = cx + i * edx;
            float rx = er * sc, ry = er * sc * kv;
            if (EYE[pre] == 1) { rx = er * 0.8f * sc; ry = er * 1.2f * sc * kv; }
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFFFFF);
            r.set(ex - rx, ey - ry, ex + rx, ey + ry);
            if (EYE[pre] == 2) c.drawRoundRect(r, er * 0.5f, er * 0.5f * kv, p);
            else c.drawOval(r, p);
            if (kv > 0.5f) {
                p.setColor(0xFF0F172A);
                c.drawCircle(ex + px * er * 0.45f, ey + py * er * 0.45f, er * 0.5f, p);
            }
        }

        float my = cy + hr * 0.42f, m = hr * 0.28f;
        if (mood == TALK || wow) {
            float oh = wow ? hr * 0.14f : hr * (0.04f + 0.14f * (float) Math.abs(Math.sin(now / 110.0)));
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF0F172A);
            r.set(cx - hr * 0.14f, my - oh, cx + hr * 0.14f, my + oh);
            c.drawOval(r, p);
        } else {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.035f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(0xFFFFFFFF);
            if (mood == HAPPY) {
                r.set(cx - m, my - m * 0.6f, cx + m, my + m * 0.6f);
                c.drawArc(r, 15f, 150f, false, p);
            } else {
                c.drawLine(cx - m * 0.7f, my, cx + m * 0.7f, my, p);
            }
        }

        if (sleep) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFFFFF);
            p.setTextSize(s * 0.2f);
            c.drawText("z", cx + hr * 0.6f, cy - hr * 0.55f, p);
        }
    }
}
