package com.nazril.asisten;

import android.app.Notification;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class AssistService extends Service implements OverlayController.Listener {
  private final Handler ui = new Handler(Looper.getMainLooper());
  private MediaProjection mp;
  private VirtualDisplay vd;
  private ImageReader ir;
  private OverlayController overlay;
  private GeminiVisionClient client;
  private boolean want = false, busy = false;
private int halaman = 0, sigLalu = 0; private boolean lanjut = false; private final StringBuilder semua = new StringBuilder();
  private volatile boolean dead = false;
  private int sw, sh;

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }

  @Override
  public int onStartCommand(Intent in, int flags, int startId) {
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel(
      "a", "Asisten", NotificationManager.IMPORTANCE_LOW));
    Notification n = new Notification.Builder(this, "a")
      .setContentTitle("AI Asisten aktif")
      .setContentText("Ketuk tombol AI. Tahan 1 detik untuk berhenti.")
      .setSmallIcon(android.R.drawable.ic_menu_camera)
      .build();

    if (Build.VERSION.SDK_INT >= 29) {
      startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
    } else {
      startForeground(1, n);
    }

    if (mp != null || in == null) return START_NOT_STICKY;
    try {
      MediaProjectionManager m =
        (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
      Intent data;
      if (Build.VERSION.SDK_INT >= 33) {
        data = in.getParcelableExtra("data", Intent.class);
      } else {
        data = (Intent) in.getParcelableExtra("data");
      }
      mp = m.getMediaProjection(in.getIntExtra("resultCode", 0), data);
      client = new GeminiVisionClient(this);
      setupCapture();
      overlay.showBubble();
    } catch (Exception e) {
      stopSelf();
    }
    return START_NOT_STICKY;
  }

  private void setupCapture() {
    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    DisplayMetrics dm = new DisplayMetrics();
    wm.getDefaultDisplay().getRealMetrics(dm);
    sw = dm.widthPixels;
    sh = dm.heightPixels;
    overlay = new OverlayController(this, wm, sh, this);

    // Android 14+ mewajibkan callback didaftarkan sebelum createVirtualDisplay
    mp.registerCallback(new MediaProjection.Callback() {
      @Override
      public void onStop() {
        ui.post(() -> stopSelf());
      }
    }, ui);

    ir = ImageReader.newInstance(sw, sh, PixelFormat.RGBA_8888, 2);
    vd = mp.createVirtualDisplay(
      "cap", sw, sh, dm.densityDpi,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
      ir.getSurface(), null, null
    );

    ir.setOnImageAvailableListener(r -> {
      Image img = r.acquireLatestImage();
      if (img == null) return;
      Bitmap bmp = null;
      boolean failed = false;
      try {
        if (want) {
          want = false;
          Image.Plane p = img.getPlanes()[0];
          int w = p.getRowStride() / p.getPixelStride();
          Bitmap full = Bitmap.createBitmap(w, sh, Bitmap.Config.ARGB_8888);
          full.copyPixelsFromBuffer(p.getBuffer());
          bmp = Bitmap.createBitmap(full, 0, 0, sw, sh);
          // createBitmap bisa mengembalikan objek yang sama jika ukurannya identik
          if (bmp != full) full.recycle();
        }
      } catch (Exception e) {
        failed = true;
      }
      img.close();
      if (failed) done("Gagal mengambil layar. Coba lagi.");
      else if (bmp != null) analyze(bmp);
    }, ui);
  }

  @Override
  public void onCaptureRequested() {
    if (busy || dead) return;
    busy = true; if (!lanjut) { halaman = 0; semua.setLength(0); sigLalu = 0; } lanjut = false;
    overlay.hidePanel();
    overlay.setBubbleVisible(false);

    // Beri jeda agar bubble/panel benar-benar hilang dari frame sebelum diambil
    ui.postDelayed(() -> {
NikoAccess na = NikoAccess.inst;
if (na != null && android.os.Build.VERSION.SDK_INT >= 30) {
na.ambilLayar(b -> ui.post(() -> { if (dead) { b.recycle(); } else { analyze(b); } }), () -> ui.post(() -> want = true));
} else { want = true; }
}, 250);

    // Batas waktu jika tidak ada frame yang masuk
    ui.postDelayed(() -> {
      if (want) {
        want = false;
        done("Tidak ada gambar layar. Coba ketuk lagi.");
      }
    }, 3000);
  }

  @Override
  public void onStopRequested() {
    stopSelf();
  }

  private int sigBmp(Bitmap b) {
Bitmap t = Bitmap.createScaledBitmap(b, 16, 16, true);
int h = 17;
for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { int c = t.getPixel(x, y); h = h * 31 + ((c >> 20) & 15) * 256 + ((c >> 12) & 15) * 16 + ((c >> 4) & 15); }
if (t != b) t.recycle();
return h;
}
private void analyze(final Bitmap bmp) {
    overlay.setBubbleVisible(true);
    overlay.showPanel("Menganalisis layar...");
    new Thread(() -> {
      String ans;
      try {
        int sg = sigBmp(bmp);
if (halaman > 0 && sg == sigLalu) { ans = "##END##"; }
else {
sigLalu = sg;
String kt = semua.length() > 400 ? semua.substring(semua.length() - 400) : semua.toString();
GeminiVisionClient.konteks = halaman == 0 ? "" : "Ini lanjutan setelah layar digeser ke bawah. Soal yang sudah dijawab sebelumnya (jangan dijawab ulang): " + kt + " Jawab hanya soal baru yang belum ada di situ. ";
ans = client.analyzeScreenshot(bmp);
}
      } catch (Exception e) {
        ans = "Gagal: " + e.getMessage();
      }
      bmp.recycle();
      final String out = ans;
      ui.post(() -> done(out));
    }).start();
  }

  private void geserLayar() {
NikoAccess na = NikoAccess.inst;
if (na == null || dead) return;
android.util.DisplayMetrics d2 = getResources().getDisplayMetrics();
float x = d2.widthPixels * 0.5f;
na.swipe(x, d2.heightPixels * 0.78f, x, d2.heightPixels * 0.28f, 450);
ui.postDelayed(() -> { if (dead) return; lanjut = true; onCaptureRequested(); }, 1500);
}
private void done(String s) {
String aksi = null, komen = null;
java.util.regex.Matcher mt = java.util.regex.Pattern.compile("^\\s*\\[(\\w+)(?:\\|([^\\]]*))?\\]\\s*").matcher(s);
if (mt.find()) { aksi = mt.group(1); komen = mt.group(2); s = s.substring(mt.end()); }
    if (dead) return;
    busy = false;
    overlay.setBubbleVisible(true);
    boolean habis = s.startsWith("##END##");
boolean galat = !habis && (s.startsWith("Gagal") || s.startsWith("Tidak ada gambar"));
boolean lagi = false;
java.util.regex.Matcher ml = java.util.regex.Pattern.compile("\\s*\\[LANJUT\\]\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(s);
if (ml.find()) { lagi = true; s = s.substring(0, ml.start()); }
if (!galat && !habis) { halaman++; if (semua.length() > 0) semua.append("\n\n"); semua.append(s.trim()); }
RobotBodyView.aksiGlobal(aksi, komen);
if (lagi && !galat && !habis && halaman < 6 && NikoAccess.inst != null) {
overlay.hidePanel();
ui.postDelayed(() -> geserLayar(), 300);
return;
}
String tampil = semua.toString();
if (tampil.isEmpty()) tampil = habis ? "Tidak ada yang berubah di layar." : s;
else if (galat) tampil = tampil + "\n\n(" + s + ")";
else if (lagi && halaman >= 6) tampil = tampil + "\n\n(Berhenti di halaman 6. Ketuk bola lagi untuk lanjut.)";
overlay.showPanel(tampil);
  }

  @Override
  public void onDestroy() {
    dead = true;
    ui.removeCallbacksAndMessages(null);
    if (overlay != null) overlay.destroy();
    try { if (vd != null) vd.release(); } catch (Exception ignored) {}
    try { if (ir != null) ir.close(); } catch (Exception ignored) {}
    try { if (mp != null) mp.stop(); } catch (Exception ignored) {}
    super.onDestroy();
  }
}
