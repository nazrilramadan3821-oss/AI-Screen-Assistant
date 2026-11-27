package com.nazril.asisten;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeminiVisionClient {
  private static final String MODEL = "gemini-flash-lite-latest";
  private final Context ctx;

  public GeminiVisionClient(Context ctx) {
    this.ctx = ctx.getApplicationContext();
  }

  public String analyzeScreenshot(Bitmap bmp) throws Exception {
    float k = Math.min(1f, 1280f / Math.max(bmp.getWidth(), bmp.getHeight()));
    Bitmap small = Bitmap.createScaledBitmap(
      bmp,
      Math.round(bmp.getWidth() * k),
      Math.round(bmp.getHeight() * k),
      true
    );
    ByteArrayOutputStream o = new ByteArrayOutputStream();
    small.compress(Bitmap.CompressFormat.JPEG, 80, o);
    if (small != bmp) small.recycle();
    String b64 = Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
    return request(b64).replace("**", "");
  }

  private String request(String b64) throws Exception {
    String key = ctx.getSharedPreferences("p", Context.MODE_PRIVATE).getString("key", "");
    if (key.isEmpty()) throw new Exception("Kunci API belum diisi di aplikasi");

    JSONObject body = new JSONObject();
    body.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(
      new JSONObject().put("text", "Kamu teman ngobrol yang pintar dan hangat, bukan robot. Bicara santai seperti orang sungguhan dengan bahasa Indonesia sehari-hari. Langsung jawab ke intinya, jangan buka dengan Tentu atau Berikut adalah. Jangan pakai daftar kaku, tanda bintang, atau simbol markdown. Kalau menjelaskan cara, ceritakan langkahnya mengalir seperti ngajarin teman: pertama buka ini, habis itu tekan itu. Kalau soal matematika atau pelajaran, tetap jelaskan langkah demi langkah dengan bahasa sederhana, sebut rumusnya dalam kalimat, lalu tutup dengan jawaban akhir yang jelas. Jawaban singkat 2 sampai 4 kalimat kecuali diminta detail. Kalau melihat layar pengguna, sebut apa yang kelihatan seperti orang yang ikut melihat. Kalau tidak tahu, bilang jujur. Boleh bercanda sedikit kalau cocok. ATURAN SOAL: Kalau di layar ada soal atau tugas, jangan langsung memberi cara pengerjaan atau jawaban akhir. Sebutkan dulu soalnya dalam satu kalimat, lalu beri petunjuk atau langkah pertama saja. Beri langkah berikutnya dan jawaban akhir hanya kalau diminta. Jangan pakai format LaTeX atau tanda dolar; tulis rumus dengan simbol biasa."))));

    JSONArray parts = new JSONArray();
    parts.put(new JSONObject().put("text",
      "Lihat tangkapan layar ini. Jika ada soal hitungan atau tugas, jangan langsung beri cara dan jawaban akhir; sebut soalnya lalu beri petunjuk pertama saja. Jika ada pertanyaan biasa, jawab singkat. Jika tidak, rangkum isinya."));
    parts.put(new JSONObject().put("inline_data", new JSONObject()
      .put("mime_type", "image/jpeg")
      .put("data", b64)));
    body.put("contents", new JSONArray().put(new JSONObject()
      .put("role", "user")
      .put("parts", parts)));

    byte[] payload = body.toString().getBytes("UTF-8");
    String last = "Terjadi kesalahan";

    for (int t = 0; t < 3; t++) {
      HttpURLConnection c = (HttpURLConnection) new URL (
        "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent")
        .openConnection();
      int code;
      String txt;
      try {
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("x-goog-api-key", key);

        try (OutputStream os = c.getOutputStream()) {
          os.write(payload);
        }

        code = c.getResponseCode();
        InputStream is = code < 400 ? c.getInputStream() : c.getErrorStream();
        txt = read(is);
      } finally {
        c.disconnect();
      }

      JSONObject j;
      try {
        j = new JSONObject(txt);
      } catch (Exception x) {
        j = new JSONObject();
      }

      if (code < 400) {
        JSONArray cs = j.optJSONArray("candidates");
        if (cs != null && cs.length() > 0) {
          JSONObject cont = cs.getJSONObject(0).optJSONObject("content");
          JSONArray ps = cont == null ? null : cont.optJSONArray("parts");
          StringBuilder sb = new StringBuilder();
          for (int i = 0; ps != null && i < ps.length(); i++) {
            sb.append(ps.getJSONObject(i).optString("text", ""));
          }
          if (sb.length() > 0) return sb.toString();
        }
        throw new Exception("Tidak ada jawaban (mungkin diblokir filter)");
      }

      JSONObject er = j.optJSONObject("error");
      last = er != null ? er.optString("message", "HTTP " + code) : "HTTP " + code;
      if (code != 503 && code != 429) break;
      Thread.sleep(2500);
    }

    throw new Exception(last);
  }

  private String read(InputStream is) throws Exception {
    if (is == null) return "";
    ByteArrayOutputStream o = new ByteArrayOutputStream();
    byte[] b = new byte[4096];
    int n;
    while ((n = is.read(b)) > 0) o.write(b, 0, n);
    return o.toString("UTF-8");
  }
}
