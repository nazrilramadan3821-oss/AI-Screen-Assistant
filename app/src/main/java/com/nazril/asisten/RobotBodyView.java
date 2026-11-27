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
        CELINGAK = 4, REGANG = 5, PUTAR = 6, TIDUR = 7, LOMPAT = 8, GULING = 9;

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

    private int kShape = 0, kColor = 0; private String kIsi = ""; private long kLoad = 0;
private void kMuat() {
long n = System.currentTimeMillis();
if (n - kLoad < 1000) return;
kLoad = n;
try { android.content.SharedPreferences sp = getContext().getSharedPreferences("p", 0);
kShape = sp.getInt("k_shape", 0); kColor = sp.getInt("k_color", 0); kIsi = sp.getString("k_isi", ""); } catch (Exception e) { }
}
private boolean isiAda() { kMuat(); return kIsi != null && kIsi.length() > 0; }
private void poly(Canvas c, int n, float r1, float r2, Paint p) {
android.graphics.Path pa = new android.graphics.Path();
int tot = r2 > 0 ? n * 2 : n;
for (int i = 0; i < tot; i++) {
double a = -Math.PI / 2 + 2 * Math.PI * i / tot;
float rr = (r2 > 0 && i % 2 == 1) ? r2 : r1;
float x = (float) Math.cos(a) * rr, y = (float) Math.sin(a) * rr;
if (i == 0) pa.moveTo(x, y); else pa.lineTo(x, y);
}
pa.close(); c.drawPath(pa, p);
}
private void drawBodyKostum(Canvas c, float s, float br, Paint p) {
kMuat();
float r = s * 0.36f * br;
p.setStyle(Paint.Style.FILL);
p.setColor(kColor == 0 ? 0xFF3B82F6 : kColor);
switch (kShape) {
case 1: c.drawRoundRect(new RectF(-r, -r, r, r), r * 0.4f, r * 0.4f, p); break;
case 2: c.drawRect(-r * 0.95f, -r * 0.95f, r * 0.95f, r * 0.95f, p); break;
case 3: c.drawOval(new RectF(-r * 0.85f, -r * 1.1f, r * 0.85f, r * 1.0f), p); break;
case 4: poly(c, 6, r * 1.05f, 0, p); break;
case 5: poly(c, 4, r * 1.2f, 0, p); break;
case 6: poly(c, 5, r * 1.2f, r * 0.55f, p); break;
default: c.drawCircle(0, 0, r, p);
}
if (kIsi != null && kIsi.length() > 0) {
int len = kIsi.codePointCount(0, kIsi.length());
p.setColor(0xFFFFFFFF); p.setTextAlign(Paint.Align.CENTER);
p.setTextSize(r * (len <= 1 ? 1.1f : Math.min(1.0f, 1.8f / len)));
c.drawText(kIsi, 0, -(p.ascent() + p.descent()) / 2, p);
p.setTextAlign(Paint.Align.LEFT);
}
}
private static RobotBodyView inst;
@Override protected void onAttachedToWindow() { super.onAttachedToWindow(); inst = this; }
@Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); if (inst == this) inst = null; }
public static void aksiGlobal(String nama, String teks) { if (inst != null) inst.aksi(nama, teks); }
public void aksi(String nama, String teks) {
long now = System.currentTimeMillis();
boolean td = (shape == SHAPE3D);
if (nama != null) {
int n = -1;
switch (nama.toLowerCase()) {
case "santai": n = 0; break; case "main": n = 1; break; case "lambai": n = 2; break;
case "menari": n = 3; break; case "celingak": n = 4; break; case "regang": n = 5; break;
case "putar": n = 6; break; case "tidur": n = 7; break; case "lompat": n = 8; break;
case "guling": n = td ? 6 : 9; break;
case "goyang": n = td ? 9 : 3; break;
case "angguk": n = td ? 10 : 2; break;
}
if (n >= 0) {
long dur = n == 6 ? 2500 : n == 7 ? 10000 : n == 8 ? 4000 : (n == 9 && !td) ? 3000 : 6000;
lastTouch = 0; act = n; actStart = now; actEnd = now + dur;
if (td && web != null) web.evaluateJavascript("try{setAksi(" + n + "," + dur + ")}catch(e){}", null);
}
}
if (teks != null && !teks.trim().isEmpty() && td && web != null) {
long ms = 3000 + 70L * teks.length();
web.evaluateJavascript("try{robotBicara(" + org.json.JSONObject.quote(teks) + "," + ms + ")}catch(e){}", null);
}
}
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
        do { n = rnd.nextInt(10); } while (n == act);
        act = n;
        actStart = now;
        long dur = 6000 + rnd.nextInt(8000);
        if (n == TIDUR) dur = 20000 + rnd.nextInt(20000);
        if (n == PUTAR) dur = 2500; if (n == GULING) dur = 3000; if (n == LOMPAT) dur = 4000;
        actEnd = now + dur;
    }

    @Override protected void onDraw(Canvas c) {
kMuat(); if (kShape == 7) shape = SHAPE3D; else if (shape == SHAPE3D) shape = BALL;
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
            case LOMPAT: { float j = (float) Math.abs(Math.sin(at * 4)); dy = -j * s * 0.15f; sy = 0.85f + 0.3f * j; sx = 1.15f - 0.2f * j; break; } case GULING: { float g = Math.min(at / 3f, 1f); rot = 720f * g; dy = -(float) Math.sin(Math.PI * g) * s * 0.06f; break; } case PUTAR: {
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
            drawBodyKostum(c, s, breathe, p);
            p.setColor(isiAda() ? 0x00FFFFFF : 0xFFFFFFFF);
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
