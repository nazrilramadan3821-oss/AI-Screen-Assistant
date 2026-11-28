package com.nazril.asisten;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoFillService extends AccessibilityService {

    // GANTI dengan kunci API Gemini yang sama seperti di GeminiVisionClient
    private static final String API_KEY = "GANTI_API_KEY";
    private static final String MODEL = "gemini-2.0-flash";

    private static AutoFillService instance;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() {
        running = false;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        running = false;
        return super.onUnbind(intent);
    }

    public static boolean isActive() {
        return instance != null;
    }

    public static boolean start(int delayDetik, int maxHalaman) {
        if (instance == null) return false;
        instance.begin(delayDetik, maxHalaman);
        return true;
    }

    public static void stop() {
        if (instance != null) instance.running = false;
    }

    private void begin(final int delayDetik, final int maxHalaman) {
        if (running) return;
        running = true;
        toast("Buka halaman soal. Mulai dalam " + delayDetik + " detik");
        bg.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayDetik * 1000L);
                    for (int h = 0; h < maxHalaman && running; h++) {
                        isiLayar();
                        Thread.sleep(1000);
                        if (!running || !scrollKeBawah()) break;
                        Thread.sleep(1200);
                    }
                    toast("Selesai");
                } catch (Exception e) {
                    toast("Gagal: " + e.getMessage());
                }
                running = false;
            }
        });
    }

    private void isiLayar() throws Exception {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        if (getPackageName().contentEquals(String.valueOf(root.getPackageName()))) {
            toast("Buka halaman soal dulu");
            running = false;
            return;
        }

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        kumpulkan(root, nodes, sb);
        if (nodes.isEmpty()) return;

        JSONArray aksi = new JSONArray(tanyaAi(sb.toString()));
        for (int i = 0; i < aksi.length() && running; i++) {
            JSONObject o = aksi.getJSONObject(i);
            int id = o.optInt("id", -1);
            if (id < 0 || id >= nodes.size()) continue;
            AccessibilityNodeInfo n = nodes.get(id);
            n.refresh();
            String jenis = o.optString("aksi");
            if ("klik".equals(jenis)) {
                klik(n);
            } else if ("isi".equals(jenis)) {
                isi(n, o.optString("nilai"));
            }
            Thread.sleep(400);
        }
    }

    private void kumpulkan(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out, StringBuilder sb) {
        if (n == null || !n.isVisibleToUser()) return;

        CharSequence t = n.getText();
        CharSequence d = n.getContentDescription();
        String label = t != null ? t.toString() : (d != null ? d.toString() : "");
        boolean isian = n.isEditable();

        if (!label.isEmpty() || isian) {
            if (label.length() > 300) label = label.substring(0, 300);
            sb.append('[').append(out.size()).append("] ");
            if (isian) sb.append("(isian) ");
            else if (n.isCheckable()) sb.append(n.isChecked() ? "(pilihan, sudah dipilih) " : "(pilihan) ");
            else if (n.isClickable()) sb.append("(tombol) ");
            sb.append(label.replace('\n', ' ')).append('\n');
            out.add(n);
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            kumpulkan(n.getChild(i), out, sb);
        }
    }

    private void klik(AccessibilityNodeInfo n) {
        if (n.isCheckable() && n.isChecked()) return;
        AccessibilityNodeInfo x = n;
        while (x != null && !x.isClickable()) x = x.getParent();
        if (x != null) x.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private void isi(AccessibilityNodeInfo n, String nilai) {
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, nilai);
        n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    private boolean scrollKeBawah() {
        AccessibilityNodeInfo s = cariScroll(getRootInActiveWindow());
        return s != null && s.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    private AccessibilityNodeInfo cariScroll(AccessibilityNodeInfo n) {
        if (n == null) return null;
        if (n.isScrollable()) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo r = cariScroll(n.getChild(i));
            if (r != null) return r;
        }
        return null;
    }

    private String tanyaAi(String layar) throws Exception {
        String prompt = "Ini daftar elemen di layar HP. Tiap baris: [nomor] jenis teks.\n"
                + "Jawab semua soal yang terlihat. Balas HANYA berupa JSON array, contoh:\n"
                + "[{\"id\":3,\"aksi\":\"klik\"},{\"id\":7,\"aksi\":\"isi\",\"nilai\":\"42\"}]\n"
                + "Pilihan ganda: klik satu pilihan jawaban per soal. Kolom (isian): aksi isi. "
                + "Jangan klik pilihan yang sudah dipilih, dan jangan klik tombol Kirim/Submit.\n\n"
                + layar;

        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(new JSONObject()
                        .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))))
                .put("generationConfig", new JSONObject().put("responseMimeType", "application/json"));

        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL + ":generateContent?key=" + API_KEY);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes("UTF-8"));
        }

        int kode = c.getResponseCode();
        Kredit.siap(this); Kredit.catat(MODEL, kode);
        InputStream is = kode < 400 ? c.getInputStream() : c.getErrorStream();
        String resp = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
        if (kode >= 400) throw new Exception("AI error " + kode);

        return new JSONObject(resp)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                .getString("text");
    }

    private void toast(final String pesan) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AutoFillService.this, pesan, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
