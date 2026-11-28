package com.nazril.asisten;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NikoOtak {
  private static final String MODEL = "gemini-flash-lite-latest";
  private static final Handler UI = new Handler(Looper.getMainLooper());

  public static void santai(final Context ctx, final String aksiLama, final String teksLama) {
    new Thread(new Runnable() { public void run() {
      String t = null; JSONArray l = null;
      try {
        android.content.SharedPreferences sp = ctx.getSharedPreferences("p", Context.MODE_PRIVATE);
        String key = sp.getString("key", "");
        if (!key.isEmpty()) {
          JSONObject h = tanya(key, sp.getString("nama", ""));
          t = h.optString("teks", "").trim();
          l = h.optJSONArray("langkah");
          if (t.isEmpty() || l == null || l.length() == 0) { t = null; l = null; }
          else if (t.length() > 80) t = t.substring(0, 80);
        }
      } catch (Exception e) { t = null; l = null; }
      final String ft = t; final JSONArray fl = l;
      UI.post(new Runnable() { public void run() {
        if (ft != null) { OverlayController oc = OverlayController.aktif == null ? null : OverlayController.aktif.get(); if (oc != null) oc.jalankanAI(ft, fl); else RobotBodyView.aksiAIGlobal(ft, fl); }
        else RobotBodyView.aksiGlobal(aksiLama, teksLama);
      }});
    }}).start();
  }

  private static JSONObject tanya(String key, String nama) throws Exception {
    String jam = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(new java.util.Date());
    String prompt = "Kamu NIKO, robot asisten belajar yang lucu di layar HP. Sekarang jam " + jam
      + ". Nama pengguna: " + (nama.isEmpty() ? "Nino" : nama)
      + ". Buat satu celetukan singkat (maks 60 karakter, bahasa Indonesia santai, jangan klise) dan satu gerakan baru yang cocok dengan isinya."
      + " Balas HANYA JSON: {\"teks\":\"...\",\"langkah\":[{\"x\":0,\"y\":-20,\"putar\":10,\"skala\":1.1,\"ms\":300}]}."
      + " Aturan: 2 sampai 6 langkah; x dan y antara -30 dan 30; putar antara -40 dan 40; skala antara 0.8 dan 1.3; ms antara 150 dan 800.";
    JSONObject body = new JSONObject()
      .put("contents", new JSONArray().put(new JSONObject().put("role", "user")
        .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))))
      .put("generationConfig", new JSONObject().put("temperature", 1.1).put("responseMimeType", "application/json"));
    HttpURLConnection c = (HttpURLConnection) new URL(
      "https://niko-gemini.nazril-niko.workers.dev/?api=generativelanguage&model=" + MODEL).openConnection();
    try {
      c.setRequestMethod("POST"); c.setDoOutput(true);
      c.setConnectTimeout(10000); c.setReadTimeout(20000);
      c.setRequestProperty("Content-Type", "application/json");
      c.setRequestProperty("x-app-token", key);
      try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
      int code = c.getResponseCode();
      if (code >= 400) throw new Exception("HTTP " + code);
      InputStream is = c.getInputStream();
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      byte[] buf = new byte[4096]; int n;
      while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
      JSONObject j = new JSONObject(bo.toString("UTF-8"));
      JSONArray ps = j.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < ps.length(); i++) sb.append(ps.getJSONObject(i).optString("text", ""));
      String s = sb.toString().replace("```json", "").replace("```", "").trim();
      return new JSONObject(s);
    } finally { c.disconnect(); }
  }
}
