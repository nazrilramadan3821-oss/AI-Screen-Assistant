package com.nazril.asisten;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import java.util.Random;

public class RobotBodyView extends FrameLayout {
    public static final int BALL = 0, ROUND = 1, BOX = 2, SHAPE3D = 3;
    private static final int SANTAI = 0, MAIN = 1, LAMBAI = 2, MENARI = 3,
        CELINGAK = 4, REGANG = 5, PUTAR = 6, TIDUR = 7;

    private int shape;
    private long lastTouch = System.currentTimeMillis();
    private int act = SANTAI;
    private long actStart = 0, actEnd = 0;
    private float lie = 0f;
    private Bitmap bmp;
    private WebView web;
    private boolean webOn = false, webLoading = false;
    private final Random rnd = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public RobotBodyView(Context ctx) {
        super(ctx);
        setWillNotDraw(false);
        shape = ctx.getSharedPreferences("robot", Context.MODE_PRIVATE).getInt("shape", ROUND);
        int id = ctx.getResources().getIdentifier("robot_biru", "drawable", ctx.getPackageName());
        if (id != 0) bmp = BitmapFactory.decodeResource(ctx.getResources(), id);
    }

    public void setShape(int s) {
        shape = s;
        getContext().getSharedPreferences("robot", Context.MODE_PRIVATE)
            .edit().putInt("shape", s).apply();
        invalidate();
    }
    public int getShape() { return shape; }

    public void poke() {
        lastTouch = System.currentTimeMillis();
        if (web != null && shape == SHAPE3D) web.evaluateJavascript("poke()", null);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) { return true; }

    private void ensureWeb() {
        if (web != null) return;
        web = new WebView(getContext());
        web.setBackgroundColor(Color.TRANSPARENT);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);
        web.setClickable(false);
        web.setFocusable(false);
        addView(web, new FrameLayout.LayoutParams(-1, -1));
        web.loadUrl("file:///android_asset/robot3d.html");
        webOn = true;
    }

    private void pickNext(long now) {
        int n;
        do { n = rnd.nextInt(8); } while (n == act);
        act = n;
        actStart = now;
        long dur = 6000 + rnd.nextInt(8000);
        if (n == TIDUR) dur = 20000 + rnd.nextInt(20000);
        if (n == PUTAR) dur = 2500;
        actEnd = now + dur;
    }

    @Override protected void onDraw(Canvas c) {
        long now = System.currentTimeMillis();
        shape = getContext().getSharedPreferences("robot", Context.MODE_PRIVATE).getInt("shape", shape);

        if (shape == SHAPE3D) {
            if (web == null && !webLoading) { webLoading = true; post(this::ensureWeb); }
            if (web != null) {
                web.setVisibility(VISIBLE);
                if (!webOn) { web.onResume(); webOn = true; }
            }
            postInvalidateDelayed(400);
            return;
        }
        if (web != null) {
            web.setVisibility(GONE);
            if (webOn) { web.onPause(); webOn = false; }
        }

        long idle = now - lastTouch;
        if (idle < 8000) { act = SANTAI; actStart = now; actEnd = now; }
        else if (now >= actEnd) pickNext(now);

        float t = (now % 100000) / 1000f;
        float at = (now - actStart) / 1000f;
        boolean sleep = (act == TIDUR);
        lie += ((sleep ? 1f : 0f) - lie) * 0.08f;

        float w = getWidth(), h = getHeight(), s = Math.min(w, h);
        float dx = 0, dy = 0, rot = 0, sx = 1, sy = 1;
        switch (act) {
            case MAIN:
                dy = -Math.abs((float) Math.sin(at * 5)) * s * 0.08f;
                rot = (float) Math.sin(at * 5) * 4f; break;
            case LAMBAI:
                rot = (float) Math.sin(at * 7) * 12f; break;
            case MENARI:
                dx = (float) Math.sin(at * 3) * s * 0.05f;
                rot = (float) Math.sin(at * 3) * 10f;
                dy = -Math.abs((float) Math.sin(at * 6)) * s * 0.03f; break;
            case CELINGAK:
                dx = (float) Math.sin(at * 0.9f) * s * 0.05f;
                rot = (float) Math.sin(at * 0.9f) * 6f; break;
            case REGANG:
                sy = 1f + 0.08f * (float) Math.sin(at * 2.5f);
                sx = 1f - 0.04f * (float) Math.sin(at * 2.5f); break;
            case PUTAR: {
                float k = Math.min(at / 2.5f, 1f);
                rot = 360f * k;
                dy = -(float) Math.sin(Math.PI * k) * s * 0.08f; break; }
            default:
                rot = (float) Math.sin(t * 1.5f) * 2f;
        }
        float breathe = 1f + 0.02f * (float) Math.sin(t * 2);

        c.save();
        c.translate(w / 2f, h / 2f);

        if (shape == BALL || bmp == null) {
            c.translate(dx, dy + lie * s * 0.1f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF3B82F6);
            c.drawCircle(0, 0, s * 0.36f * breathe, p);
            p.setColor(0xFFFFFFFF);
            if (sleep || (t % 4f) < 0.15f) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.025f);
                c.drawLine(-s * 0.2f, -s * 0.05f, -s * 0.08f, -s * 0.05f, p);
                c.drawLine(s * 0.08f, -s * 0.05f, s * 0.2f, -s * 0.05f, p);
            } else {
                c.drawCircle(-s * 0.14f, -s * 0.05f, s * 0.05f, p);
                c.drawCircle(s * 0.14f, -s * 0.05f, s * 0.05f, p);
            }
        } else {
            if (shape == BOX) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF1E3A8A);
                c.drawCircle(0, 0, s * 0.48f, p);
            }
            float fit = (shape == BOX) ? 0.78f : 0.88f;
            float bh = s * fit;
            float bw = bh * bmp.getWidth() / bmp.getHeight();
            c.translate(dx, dy + lie * s * 0.1f);
            c.rotate(rot + lie * 80f);
            c.scale(sx * breathe, sy * breathe);
            p.setAlpha(sleep ? 190 : 255);
            c.drawBitmap(bmp, null, new RectF(-bw / 2, -bh / 2, bw / 2, bh / 2), p);
            p.setAlpha(255);
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
        int d = (int) (getResources().getDisplayMetrics().density * 96);
        int mw = resolveSize(d, w), mh = resolveSize(d, h);
        setMeasuredDimension(mw, mh);
        if (web != null) web.measure(
            MeasureSpec.makeMeasureSpec(mw, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(mh, MeasureSpec.EXACTLY));
    }

    public void setText(CharSequence s) { }
    public void setTextColor(int c) { }
    public void setTextSize(float s) { }
    public void setGravity(int g) { }
}
