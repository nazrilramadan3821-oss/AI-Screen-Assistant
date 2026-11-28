package com.nazril.asisten;

import android.content.Context;
import android.content.SharedPreferences;
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
  private static final java.util.Random RD = new java.util.Random();
  private static final String[] TEMA = { "semangatin belajar", "ingetin minum air", "tanya lagi ngapain",
    "ledek bercanda", "pamer gerakan", "minta ditemani", "pujian kecil", "ingetin istirahat mata",
    "kaget lucu", "pura-pura sok sibuk", "ngeluh pusing", "minta pelan-pelan" };

  public static void santai(final Context ctx, final String aksiLama, final String teksLama) {
    new Thread(new Runnable() { public void run() {
      String t = null; JSONArray l = null;
      try {
        SharedPreferences sp = ctx.getSharedPreferences("p", Context.MODE_PRIVATE);
        String key = sp.getString("key", "");
        if (!key.isEmpty()) {
          int hh = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
          String per = hh >= 4 && hh < 11 ? "pagi" : hh >= 11 && hh < 15 ? "siang" : hh >= 15 && hh < 18 ? "sore" : "malam";
          String tag = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date()) + per;
          boolean boleh = !tag.equals(sp.getString("sapa_terakhir", ""));
          Kredit.siap(ctx);
          JSONObject h = tanya(key, per, boleh);
          t = h.optString("teks", "").trim();
          l = h.optJSONArray("langkah");
          String em = h.optString("emosi", "").toLowerCase();
          if (t.isEmpty() || l == null || l.length() == 0) { t = null; l = null; }
          else {
            String lc = t.toLowerCase();
            String nm = sp.getString("nama", "").trim().toLowerCase();
            boolean waktu = lc.contains("pagi") || lc.contains("siang") || lc.contains("sore")
              || lc.contains("malam") || lc.contains("makan") || lc.contains("sarapan");
            if (t.length() > 40 || lc.contains("nino") || (!nm.isEmpty() && lc.contains(nm)) || (waktu && !boleh)) {
              t = null; l = null;
            } else {
              if (waktu) sp.edit().putString("sapa_terakhir", tag).apply();
              boolean kalem = em.equals("pusing") || em.equals("ngantuk") || em.equals("tenang")
                || lc.contains("pelan") || lc.contains("pusing") || lc.contains("ngantuk")
                || lc.contains("capek") || lc.contains("santai");
              if (kalem) redam(l);
            }
          }
        }
      } catch (Exception e) { t = null; l = null; }
      final String ft = t; final JSONArray fl = l;
      UI.post(new Runnable() { public void run() {
        if (ft != null) jalan(ft, fl);
        else RobotBodyView.aksiGlobal(aksiLama, teksLama);
      }});
    }}).start();
  }

  private static void jalan(String teks, JSONArray l) {
    try {
      Class<?> oc = Class.forName("com.nazril.asisten.OverlayController");
      Object ref = oc.getField("aktif").get(null);
      Object inst = ref == null ? null : ((java.lang.ref.WeakReference<?>) ref).get();
      if (inst != null) { oc.getMethod("jalankanAI", String.class, JSONArray.class).invoke(inst, teks, l); return; }
    } catch (Throwable e) { }
    try {
      RobotBodyView.class.getMethod("aksiAIGlobal", String.class, JSONArray.class).invoke(null, teks, l);
      return;
    } catch (Throwable e) { }
    RobotBodyView.aksiGlobal(null, teks);
  }

  private static void redam(JSONArray l) {
    for (int i = 0; i < l.length(); i++) {
      JSONObject o = l.optJSONObject(i); if (o == null) continue;
      try {
        o.put("x", Math.max(-8, Math.min(8, o.optDouble("x", 0))));
        o.put("y", Math.max(-4, Math.min(4, o.optDouble("y", 0))));
        o.put("putar", Math.max(-15, Math.min(15, o.optDouble("putar", 0))));
        o.put("ms", Math.max(600, o.optLong("ms", 600)));
      } catch (Exception e) { }
    }
  }

  private static JSONObject tanya(String key, String per, boolean boleh) throws Exception {
    String jam = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(new java.util.Date());
    String prompt = "Kamu NIKO, robot kecil lucu di layar HP pelajar. Buat satu celetukan SANGAT singkat (maks 5 kata, maks 30 karakter), bahasa Indonesia gaul. "
      + "DILARANG menyebut nama orang atau nama pengguna, dan DILARANG menyapa dengan kata halo atau hai. "
      + "Sekarang jam " + jam + " (" + per + "). "
      + (boleh ? "Boleh menyinggung waktu ini (misalnya tanya sudah sarapan atau makan) tapi hanya kalau cocok. "
               : "DILARANG menyinggung waktu, jam, makan, atau sarapan. ")
      + "Tema kali ini: " + TEMA[RD.nextInt(TEMA.length)] + ". "
      + "Gerakan HARUS sesuai arti teks: kalau teks bilang pelan, pusing, ngantuk atau capek, gerakan kecil, lambat, miring atau goyang saja tanpa loncat. Kalau teks semangat atau kaget, gerakan boleh besar dan cepat. "
      + "Isi juga emosi: salah satu dari tenang, riang, kaget, pusing, ngantuk, semangat. "
      + "Balas HANYA JSON: {\"teks\":\"...\",\"emosi\":\"tenang\",\"langkah\":[{\"x\":0,\"y\":-20,\"putar\":10,\"skala\":1.1,\"ms\":300}]}. "
      + "Aturan: 3 sampai 7 langkah; x dan y antara -30 dan 30; putar antara -40 dan 40; skala antara 0.8 dan 1.3; ms antara 150 dan 800."
      + " Robot 3D: ciptakan gerakan BARU yang unik, tiap langkah WAJIB punya minimal 2 sendi (sudut derajat): kpl kepala miring -25..25, ang kepala angguk -25..25, tka tangan kanan diangkat 0..170, tki tangan kiri diangkat 0..170, ska siku kanan ditekuk 0..120, ski siku kiri ditekuk 0..120, kk kaki kanan diayun -40..40, kl kaki kiri diayun -40..40, tek lutut ditekuk 0..80, wajah salah satu dari senang, biasa, kaget, kedip, tidur."
      + " Pilih sendi yang cocok dengan teksnya, misalnya sapaan: tka 150 dan wajah senang; kaget: tka 120, tki 120, wajah kaget; mengantuk: kpl 20, wajah tidur.";
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
      Kredit.catat(code);
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
