package com.nazril.asisten;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.DisplayMetrics;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceService extends Service {
    SpeechRecognizer sr;
    Handler h = new Handler(Looper.getMainLooper());
    long awakeUntil = 0;
    boolean running = false;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent i, int f, int id) {
        if (i != null && "STOP".equals(i.getAction())) { stopSelf(); return START_NOT_STICKY; }
        try { startFg(); } catch (Exception e) { stopSelf(); return START_NOT_STICKY; }
        if (!running) { running = true; h.post(() -> listen()); }
        return START_STICKY;
    }

    void startFg() {
        String ch = "niko_voice";
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel(ch, "NIKO suara", NotificationManager.IMPORTANCE_LOW));
        Intent st = new Intent(this, VoiceService.class);
        st.setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 1, st, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        b.setContentTitle("NIKO mendengarkan")
         .setContentText("Ucapkan: " + (namaPanggil().isEmpty() ? "niko" : namaPanggil()) + ", buka TikTok")
         .setSmallIcon(android.R.drawable.ic_btn_speak_now)
         .addAction(android.R.drawable.ic_delete, "Stop", pi);
        Notification n = b.build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(77, n);
    }

    boolean abaikan = false;
    void listen() {
        abaikan = false;
        if (!running) return;
        try {
            if (sr != null) sr.destroy();
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) {}
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float v) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() {}
                public void onError(int e) {
                        if (abaikan) return;
                    h.postDelayed(() -> listen(), e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1500 : 250);
                }
                public void onResults(Bundle b) {
                        if (abaikan) return;
                    ArrayList<String> l = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (l != null) for (String t : l) { if (handle(t)) break; }
                    h.postDelayed(() -> listen(), 120);
                }
                public void onPartialResults(Bundle b) {
                        try {
                            ArrayList<String> p = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (p == null || p.isEmpty() || abaikan) return;
                            String q = p.get(0).toLowerCase(Locale.ROOT).trim();
                            if (q.split("\\s+").length > 4) return;
                            if (q.contains("turun") || q.contains("naik") || q.contains("lanjut") || q.contains("next")
                                || q.contains("balik") || q.equals("stop") || q.equals("cukup")) {
                                if (handle(q)) { abaikan = true; h.postDelayed(() -> listen(), 150); }
                            }
                        } catch (Exception e) { }
                    }
                public void onEvent(int t, Bundle b) {}
            });
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            sr.startListening(i);
        } catch (Exception e) {
            h.postDelayed(() -> listen(), 3000);
        }
    }

    String namaPanggil() {
        try {
            return getSharedPreferences("p", MODE_PRIVATE).getString("nama", "").trim().toLowerCase(Locale.ROOT);
        } catch (Exception e) { return ""; }
    }

    java.util.List<String> kataPanggil() {
        java.util.ArrayList<String> l = new java.util.ArrayList<String>();
        String n = namaPanggil();
        if (n.length() >= 2) l.add(n);
        if (!l.contains("niko")) l.add("niko");
        if (!l.contains("nico")) l.add("nico");
        return l;
    }

    boolean autoOn = false;
    final android.os.Handler hg = new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable langkahGulir = new Runnable() {
        public void run() {
            if (!autoOn) return;
            gulir(true);
            awakeUntil = System.currentTimeMillis() + 30000;
            hg.postDelayed(this, 7000);
        }
    };

    void autoGulir(boolean on) {
        autoOn = on;
        hg.removeCallbacks(langkahGulir);
        if (on) hg.postDelayed(langkahGulir, 1500);
    }

    void gulir(boolean turun) {
        NikoAccess x = NikoAccess.inst;
        if (x != null) x.gulirNiko(turun);
    }

    boolean handle(String t) {
        String s = t.toLowerCase(Locale.ROOT).trim();
        long now = System.currentTimeMillis();
        int k = -1;
        for (String w : kataPanggil()) {
            int x = s.indexOf(w);
            if (x >= 0) { k = x + w.length(); break; }
        }
        if (k >= 0) { s = s.substring(k); awakeUntil = now + 60000; }
        else if (now > awakeUntil) return false;
        s = s.replaceAll("^[ ,.:!?]+", "").trim();
        if (s.isEmpty()) return k >= 0;
        boolean ok = run(s);
        if (ok) awakeUntil = System.currentTimeMillis() + 60000;
        return ok;
    }

    boolean run(String s) {
        if (s.contains("berhenti") || (s.contains("matikan niko") || (!namaPanggil().isEmpty() && s.contains("matikan " + namaPanggil())))) { autoOn = false; stopSelf(); return true; }
        if (s.contains("scroll terus") || s.contains("otomatis")) { autoGulir(true); return true; }
        if (s.equals("stop") || s.equals("cukup") || s.equals("diam")) { autoGulir(false); return true; }
        if (s.contains("turun") || s.contains("lanjut") || s.contains("next")) { gulir(true); return true; }
        if (s.contains("naik") || s.contains("balik")) { gulir(false); return true; }
        if (s.startsWith("buka ") || s.startsWith("jalankan ")) {
            return open(s.substring(s.indexOf(' ') + 1));
        }
        if (s.contains("kembali") || s.equals("back")) return global(AccessibilityService.GLOBAL_ACTION_BACK);
        if (s.contains("beranda") || s.equals("home")) return global(AccessibilityService.GLOBAL_ACTION_HOME);
        if (s.contains("naik") || s.contains("sebelumnya") || s.contains("atas")) return swipe(false);
        if (s.contains("scroll") || s.contains("gulir") || s.contains("turun") || s.contains("lanjut")
                || s.contains("berikutnya") || s.contains("next") || s.contains("geser")) return swipe(true);
        if (s.contains("ketuk") || s.contains("jeda") || s.contains("putar") || s.equals("tap")) return tap();
        return false;
    }

    boolean open(String name) {
        name = name.replace("aplikasi", "").replace(" ", "").trim();
        if (name.isEmpty()) return false;
        PackageManager pm = getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> l = pm.queryIntentActivities(q, 0);
        String pkg = null;
        for (ResolveInfo r : l) {
            String lb = r.loadLabel(pm).toString().toLowerCase(Locale.ROOT).replace(" ", "");
            if (lb.equals(name)) { pkg = r.activityInfo.packageName; break; }
            if (pkg == null && (lb.contains(name) || name.contains(lb)) && lb.length() > 2) pkg = r.activityInfo.packageName;
        }
        if (pkg == null) { toast("Aplikasi tidak ditemukan: " + name); return false; }
        try {
            Intent li = pm.getLaunchIntentForPackage(pkg);
            li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(li);
            return true;
        } catch (Exception e) { return false; }
    }

    boolean global(int a) {
        NikoAccess x = NikoAccess.inst;
        if (x == null) { toast("Aktifkan Aksesibilitas NIKO dulu"); return false; }
        return x.performGlobalAction(a);
    }

    boolean swipe(boolean up) {
        NikoAccess x = NikoAccess.inst;
        if (x == null) { toast("Aktifkan Aksesibilitas NIKO dulu"); return false; }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float cx = dm.widthPixels / 2f, a = dm.heightPixels * 0.75f, b = dm.heightPixels * 0.25f;
        if (up) x.swipe(cx, a, cx, b, 250); else x.swipe(cx, b, cx, a, 250);
        return true;
    }

    boolean tap() {
        NikoAccess x = NikoAccess.inst;
        if (x == null) { toast("Aktifkan Aksesibilitas NIKO dulu"); return false; }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        x.tap(dm.widthPixels / 2f, dm.heightPixels / 2f);
        return true;
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    @Override public void onDestroy() {
        running = false;
        h.removeCallbacksAndMessages(null);
        if (sr != null) { sr.destroy(); sr = null; }
        super.onDestroy();
    }
}
