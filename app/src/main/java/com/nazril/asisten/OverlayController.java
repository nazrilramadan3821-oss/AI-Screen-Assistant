package com.nazril.asisten;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OverlayController {
  public interface Listener {
    void onCaptureRequested();
    void onStopRequested();
  }

  private final Context ctx;
  private final WindowManager wm;
  private final int screenHeight;
  private final Listener listener;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private View bubble; private View panel;
  private TextView resultView;

  public OverlayController(Context ctx, WindowManager wm, int screenHeight, Listener listener) {
    this.ctx = ctx;
    this.wm = wm;
    this.screenHeight = screenHeight;
    this.listener = listener;
  }

  private int dp(int v) {
    return (int) (v * ctx.getResources().getDisplayMetrics().density);
  }

  public void showBubble() {
    if (bubble != null) return;
    RobotBodyView t = new RobotBodyView(ctx);
    t.setText("AI");
    t.setTextColor(Color.WHITE);
    t.setTextSize(18);
    t.setGravity(Gravity.CENTER);

    GradientDrawable g = new GradientDrawable();
    g.setShape(GradientDrawable.OVAL);
    g.setColor(Color.parseColor("#3b82f6"));
    t.setBackground(g);

    final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
      ukuranBola(), ukuranBola(),
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT);
    lp.gravity = Gravity.TOP | Gravity.START;
        lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
    lp.x = dp(8);
    lp.y = dp(200); aiLp = lp; aiView = t; aktif = new java.lang.ref.WeakReference<OverlayController>(this);
ukuranLsn = new android.content.SharedPreferences.OnSharedPreferenceChangeListener() {
public void onSharedPreferenceChanged(android.content.SharedPreferences sp, String k) {
if (!"ukuran".equals(k)) return;
ui.post(new Runnable() { public void run() {
try { lp.width = ukuranBola(); lp.height = ukuranBola(); wm.updateViewLayout(t, lp); } catch (Exception e) { }
}});
}};
ctx.getSharedPreferences("p", 0).registerOnSharedPreferenceChangeListener(ukuranLsn);

    final Runnable stopper = listener::onStopRequested;
    t.setOnTouchListener(new View.OnTouchListener() {
      int sx, sy;
      float tx, ty;
      boolean moved;

      @Override
      public boolean onTouch(View v, MotionEvent e) {
        int a = e.getAction();
        if (a == MotionEvent.ACTION_DOWN) {
          sx = lp.x;
          sy = lp.y;
          tx = e.getRawX();
          ty = e.getRawY();
          moved = false;
          ((RobotBodyView) v).poke();
          ui.postDelayed(stopper, 1000);
        } else if (a == MotionEvent.ACTION_MOVE) {
          float dx = e.getRawX() - tx;
          float dy = e.getRawY() - ty;
          if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true;
          if (moved) {
            ui.removeCallbacks(stopper);
            lp.x = sx + (int) dx;
            lp.y = sy + (int) dy;
            wm.updateViewLayout(v, lp);
            if (e.getEventTime() - e.getDownTime() > 600 && android.os.SystemClock.uptimeMillis() - pusingTerakhir > 6000) {
              pusingTerakhir = android.os.SystemClock.uptimeMillis();
              RobotBodyView.aksiGlobal("putar", kataGeser[urutGeser++ % kataGeser.length]);
            }
          }
        } else if (a == MotionEvent.ACTION_UP) {
          ui.removeCallbacks(stopper);
          if (!moved) listener.onCaptureRequested();
        } else if (a == MotionEvent.ACTION_CANCEL) {
          ui.removeCallbacks(stopper);
        }
        return true;
      }
    });

    bubble = t;
    wm.addView(bubble, lp);
mulaiSantai();
RobotBodyView.bicaraGerak = new java.util.function.BiConsumer<String, Integer>() { @Override public void accept(String x, Integer ms) { OverlayController.this.komentarGerak(x, ms); } };
RobotBodyView.bicaraBola = new java.util.function.Consumer<String>() { @Override public void accept(String x) { OverlayController.this.komentar(x); } };
  }

  public void setBubbleVisible(boolean visible) {
    if (bubble != null) {
      if (!visible) tutupGelembung();
bubble.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }
  }

  private CharSequence rapi(CharSequence t) {
if (t == null) return "";
try {
String[] baris = t.toString().split("\n", -1);
android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
java.util.regex.Pattern pn = java.util.regex.Pattern.compile("^\\s*(\\d{1,3}\\s*[\\.\\)]|Jawaban\\b).*", java.util.regex.Pattern.CASE_INSENSITIVE);
for (int i = 0; i < baris.length; i++) {
String b = baris[i];
boolean judul = pn.matcher(b).matches();
if (judul && sb.length() > 0 && !sb.toString().endsWith("\n\n")) sb.append("\n");
int a = sb.length();
sb.append(b);
if (judul) {
sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), a, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
sb.setSpan(new android.text.style.ForegroundColorSpan(0xFF93C5FD), a, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
}
if (i < baris.length - 1) sb.append("\n");
}
return sb;
} catch (Exception e) { return t; }
}
private android.widget.TextView gelembung; private Runnable gelembungTutup;
private void ikutiBola(final android.widget.TextView tv, final WindowManager.LayoutParams gp) {
ui.postDelayed(new Runnable() { @Override public void run() {
if (gelembung != tv || bubble == null) return;
try {
WindowManager.LayoutParams bl = (WindowManager.LayoutParams) bubble.getLayoutParams();
int w = tv.getMeasuredWidth(), h = tv.getMeasuredHeight();
int sw = ctx.getResources().getDisplayMetrics().widthPixels;
int x = Math.max(0, Math.min(bl.x + bl.width / 2 - w / 2, sw - w));
int y = bl.y - h - dp(6);
if (y < 0) y = bl.y + bl.height + dp(6);
if (x != gp.x || y != gp.y) { gp.x = x; gp.y = y; wm.updateViewLayout(tv, gp); }
ui.postDelayed(this, 30);
} catch (Exception e) { }
} }, 30);
}
private void tutupGelembung() {
if (gelembungTutup != null) ui.removeCallbacks(gelembungTutup);
if (gelembung != null) { try { wm.removeView(gelembung); } catch (Exception e) { } gelembung = null; }
}
private long terakhirBicara; private int kataTerakhir = -1;
private final java.util.Random acak = new java.util.Random();
private final String[] kataSantai = {
"Kamu sedang apa sih?", "Kamu ngapain?", "Halo, aku masih di sini!",
"Lagi sibuk ya?", "Ada soal yang bikin pusing?", "Ketuk aku kalau butuh bantuan.",
"Jangan lupa minum, ya!", "Hmm, aku bosan nih...", "Semangat belajarnya!",
"Istirahatkan matamu sebentar.", "Sudah makan belum?", "{nama} siap bantu!",
"Kok aku dicuekin?", "Mau belajar apa hari ini?", "Pelan-pelan saja, pasti bisa!" };
private final Runnable santai = new Runnable() { @Override public void run() {
if (bubble == null) return;
boolean panelBuka = panel != null && panel.isAttachedToWindow() && panel.getVisibility() == View.VISIBLE;
android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(android.content.Context.POWER_SERVICE);
if (jedaSapaan() > 0 && bubble.getVisibility() == View.VISIBLE && !panelBuka && pm.isInteractive() && android.os.SystemClock.uptimeMillis() - terakhirBicara > 25000 && android.os.SystemClock.uptimeMillis() > istirahatSampai) {
int i; do { i = acak.nextInt(kataSantai.length); } while (i == kataTerakhir);
kataTerakhir = i;
String n = ctx.getSharedPreferences("p", 0).getString("nama", "").trim();
String k = acak.nextInt(3) == 0 ? sapaanWaktu() : kataSantai[i];
NikoOtak.santai(ctx, aksiUntuk(k), k.replace("{nama}", n.isEmpty() ? "Nino" : n));
}
int j = jedaSapaan();
ui.postDelayed(this, j == 0 ? 15000L : j * 1000L + acak.nextInt(10000));
} };
private int jedaSapaan() {
int j = 45;
try { j = Integer.parseInt(ctx.getSharedPreferences("p", 0).getString("jeda", "45")); } catch (Exception e) { }
return j <= 0 ? 0 : Math.max(30, Math.min(j, 3600));
}
private String sapaanWaktu() {
int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
if (h >= 4 && h < 11) return "Selamat pagi! Sudah sarapan?";
if (h >= 11 && h < 15) return "Selamat siang! Jangan lupa makan.";
if (h >= 15 && h < 18) return "Selamat sore! Capek tidak?";
return "Sudah malam, belum tidur?";
}
public static java.lang.ref.WeakReference<OverlayController> aktif;
private WindowManager.LayoutParams aiLp;
private View aiView;
private android.animation.ValueAnimator aiAnim;
private int aiGen = 0, aiBx, aiBy;
private boolean aiJalan = false;

public void jalankanAI(String teks, org.json.JSONArray langkah) {
RobotBodyView.aksiGlobal(null, teks);
RobotBodyView.sendiGlobal(langkah);
aiGen++;
if (aiAnim != null) aiAnim.cancel();
if (aiLp == null || aiView == null || langkah == null || langkah.length() == 0) return;
if (!aiJalan) { aiBx = aiLp.x; aiBy = aiLp.y; }
aiJalan = true;
aiStep(langkah, 0, aiGen);
}

private void aiStep(final org.json.JSONArray a, final int i, final int gen) {
if (gen != aiGen || aiLp == null || aiView == null) return;
if (i >= a.length() || i >= 8) { aiJalan = false; aiKe(aiBx, aiBy, 0f, 350, null, gen); return; }
org.json.JSONObject o = a.optJSONObject(i);
if (o == null) { aiStep(a, i + 1, gen); return; }
float d = ctx.getResources().getDisplayMetrics().density * skala() * 2f;
int x = aiBx + Math.round((float) Math.max(-30, Math.min(30, o.optDouble("x", 0))) * d);
int y = aiBy + Math.round((float) Math.max(-30, Math.min(30, o.optDouble("y", 0))) * d);
float r = (float) Math.max(-40, Math.min(40, o.optDouble("putar", 0)));
long ms = Math.max(150, Math.min(800, o.optLong("ms", 300)));
aiKe(x, y, r, ms, new Runnable() { public void run() { aiStep(a, i + 1, gen); } }, gen);
}

private void aiKe(int x, int y, float rot, long ms, final Runnable end, final int gen) {
android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
final int x1 = Math.max(0, Math.min(x, dm.widthPixels - aiLp.width));
final int y1 = Math.max(0, Math.min(y, dm.heightPixels - aiLp.height));
final int x0 = aiLp.x, y0 = aiLp.y;
final float r0 = aiView.getRotation(), r1 = rot;
aiAnim = android.animation.ValueAnimator.ofFloat(0f, 1f);
aiAnim.setDuration(ms);
aiAnim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
public void onAnimationUpdate(android.animation.ValueAnimator va) {
float f = (Float) va.getAnimatedValue();
try {
aiLp.x = x0 + Math.round((x1 - x0) * f);
aiLp.y = y0 + Math.round((y1 - y0) * f);
aiView.setRotation(r0 + (r1 - r0) * f);
wm.updateViewLayout(aiView, aiLp);
} catch (Exception e) { }
}});
aiAnim.addListener(new android.animation.AnimatorListenerAdapter() {
public void onAnimationEnd(android.animation.Animator an) { if (gen == aiGen && end != null) end.run(); }
});
aiAnim.start();
}

private android.content.SharedPreferences.OnSharedPreferenceChangeListener ukuranLsn;
public float skala() {
String u = ctx.getSharedPreferences("p", 0).getString("ukuran", "2");
if ("1".equals(u)) return 44f / 56f;
if ("3".equals(u)) return 80f / 56f;
try { int v = Integer.parseInt(u.trim()); if (v >= 60 && v <= 200) return v / 100f; } catch (Exception e) { }
return 1f;
}
private int ukuranBola() {
String u = ctx.getSharedPreferences("p", 0).getString("ukuran", "2");
return dp(Math.round(56 * skala()));
}
private long pusingTerakhir; private int urutGeser = 0;
private final String[] kataGeser = { "Aduh, aku pusing!", "Pelan-pelan, aku pusing!", "Kepalaku pusing nih!", "Stop dulu, aku pusing!" };
private String aksiUntuk(String k) {
if (k.startsWith("Selamat") || k.startsWith("Halo")) return "lambai";
if (k.contains("bosan") || k.contains("dicuekin")) return "bola";
if (k.contains("Semangat") || k.contains("bisa")) return "lompat";
if (k.contains("Istirahat")) return "tidur";
String[] a = { "kupu", "lari", "bola", "celingak", "menari" };
return a[acak.nextInt(a.length)];
}
private int urutRiwayat = 0;
public void simpanRiwayat(String teks) {
try {
android.content.SharedPreferences sp = ctx.getSharedPreferences("p", 0);
org.json.JSONArray lama = new org.json.JSONArray(sp.getString("riwayat_bola", "[]"));
org.json.JSONArray baru = new org.json.JSONArray();
baru.put(new org.json.JSONObject().put("w", System.currentTimeMillis()).put("t", teks));
for (int i = 0; i < lama.length() && baru.length() < 10; i++) baru.put(lama.getJSONObject(i));
sp.edit().putString("riwayat_bola", baru.toString()).apply();
urutRiwayat = 0;
} catch (Exception e) { }
}
private void tampilRiwayat() {
try {
org.json.JSONArray a = new org.json.JSONArray(ctx.getSharedPreferences("p", 0).getString("riwayat_bola", "[]"));
if (a.length() == 0) { resultView.setText("Belum ada riwayat."); return; }
int i = urutRiwayat % a.length();
urutRiwayat = i + 1;
org.json.JSONObject o = a.getJSONObject(i);
String jam = new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.US).format(new java.util.Date(o.getLong("w")));
resultView.setText("Riwayat " + (i + 1) + "/" + a.length() + " (" + jam + ")\n\n" + o.getString("t"));
} catch (Exception e) { resultView.setText("Riwayat tidak bisa dibaca."); }
}
private void mulaiSantai() { ui.removeCallbacks(santai); ui.postDelayed(santai, 20000); }
private long istirahatSampai;
private void komentarGerak(String teks, int istirahat) {
istirahatSampai = istirahat > 0 ? android.os.SystemClock.uptimeMillis() + istirahat : 0;
android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
boolean panelBuka = panel != null && panel.isAttachedToWindow() && panel.getVisibility() == View.VISIBLE;
if (jedaSapaan() == 0 || bubble == null || bubble.getVisibility() != View.VISIBLE || panelBuka || !pm.isInteractive()) return;
long sebelum = terakhirBicara;
komentar(teks);
terakhirBicara = sebelum;
}
public void komentar(final String teks) {
terakhirBicara = android.os.SystemClock.uptimeMillis();
ui.post(new Runnable() { @Override public void run() {
try {
tutupGelembung();
if (teks == null || teks.trim().isEmpty() || bubble == null) return;
android.widget.TextView tv = new android.widget.TextView(ctx);
tv.setText(teks.trim()); tv.setTextColor(0xFF111111); tv.setTextSize(14);
tv.setTypeface(null, android.graphics.Typeface.BOLD);
tv.setPadding(dp(12), dp(8), dp(12), dp(8)); tv.setMaxWidth(dp(220));
GradientDrawable g2 = new GradientDrawable(); g2.setColor(0xFFFFFFFF); g2.setCornerRadius(dp(16)); g2.setStroke(dp(1), 0x33000000);
tv.setBackground(g2);
tv.measure(View.MeasureSpec.makeMeasureSpec(dp(240), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
int w = tv.getMeasuredWidth(), h = tv.getMeasuredHeight();
WindowManager.LayoutParams bl = (WindowManager.LayoutParams) bubble.getLayoutParams();
int sw = ctx.getResources().getDisplayMetrics().widthPixels;
int x = Math.max(0, Math.min(bl.x + bl.width / 2 - w / 2, sw - w));
int y = bl.y - h - dp(6);
if (y < 0) y = bl.y + bl.height + dp(6);
WindowManager.LayoutParams gp = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
android.graphics.PixelFormat.TRANSLUCENT);
gp.gravity = Gravity.TOP | Gravity.START; gp.x = x; gp.y = y;
wm.addView(tv, gp);
gelembung = tv;
ikutiBola(tv, gp);
gelembungTutup = new Runnable() { @Override public void run() { tutupGelembung(); } };
ui.postDelayed(gelembungTutup, Math.min(9000, 3000 + 70L * teks.length()));
} catch (Exception e) { }
} });
}
public void showPanel(String text) {
    if (panel == null) {
      LinearLayout box = new LinearLayout(ctx);
      box.setOrientation(LinearLayout.VERTICAL);
      GradientDrawable g = new GradientDrawable();
      g.setColor(Color.parseColor("#F2131a30"));
      g.setCornerRadius(dp(16));
      g.setStroke(dp(1), Color.parseColor("#24304f"));
      box.setBackground(g);
      box.setPadding(dp(14), dp(10), dp(14), dp(10));

      resultView = new TextView(ctx) { @Override public void setText(CharSequence t, BufferType bt) { super.setText(t instanceof android.text.Spanned ? t : rapi(t), BufferType.SPANNABLE); } };
      resultView.setTextColor(Color.WHITE);
      resultView.setTextSize(17); resultView.setLineSpacing(dp(4), 1f);

      ScrollView sv = new ScrollView(ctx);
      sv.addView(resultView);

      Button close = new Button(ctx);
      close.setText("Tutup");
      close.setOnClickListener(v -> hidePanel());

      box.addView(sv, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
      final Button salin = new Button(ctx);
      salin.setText("Salin");
      salin.setOnClickListener(v -> {
        try {
          android.content.ClipboardManager cm = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
          cm.setPrimaryClip(android.content.ClipData.newPlainText("Jawaban", resultView.getText().toString()));
          salin.setText("Tersalin");
          ui.postDelayed(() -> salin.setText("Salin"), 1500);
        } catch (Exception e) { salin.setText("Gagal"); }
      });
      Button riwayat = new Button(ctx);
      riwayat.setText("Riwayat");
      riwayat.setOnClickListener(v -> tampilRiwayat());

      LinearLayout baris = new LinearLayout(ctx);
      baris.addView(riwayat, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
      baris.addView(salin, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
      baris.addView(close, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
      box.addView(baris, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

      WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, (int) (screenHeight * 0.45f),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
      lp.gravity = Gravity.BOTTOM;
      panel = box;
      wm.addView(panel, lp);
    }
    resultView.setText(JawabanFormatter.format(text));
  }

  public void hidePanel() {
    if (panel != null) {
      try {
        wm.removeView(panel);
      } catch (Exception ignored) {
      }
      panel = null;
    }
  }

  public void destroy() {
    tutupGelembung();
    RobotBodyView.bicaraBola = null;
    RobotBodyView.bicaraGerak = null;
    ui.removeCallbacksAndMessages(null);
    hidePanel();
    if (bubble != null) {
      try {
        wm.removeView(bubble);
      } catch (Exception ignored) {
      }
      bubble = null;
    }
  }
}
