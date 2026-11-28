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
public static volatile String konteks = "";
  private static final String MODEL = "gemini-flash-lite-latest";
  private final Context ctx;

  public GeminiVisionClient(Context ctx) {
    this.ctx = ctx.getApplicationContext();
  }

  private String namaAi() {
String n = ctx.getSharedPreferences("p", Context.MODE_PRIVATE).getString("nama", "").trim();
return "Namamu " + (n.isEmpty() ? "Nino" : n) + ". ";
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
      new JSONObject().put("text", namaAi() + "ATURAN FORMAT WAJIB: awali SETIAP balasan dengan satu tag berbentuk [aksi|komentar] lalu spasi lalu jawabanmu. aksi dipilih dari: santai, main, lambai, menari, celingak, regang, putar, tidur, lompat, guling, goyang, angguk, lari, bola, kupu. Pakai lari saat buru-buru, bola saat bermain, kupu saat penasaran. komentar adalah kalimat pendek maksimal 8 kata bergaya ngobrol yang muncul di gelembung atas kepala robot. Pilih aksi sesuai suasana: lompat untuk senang, angguk untuk setuju atau paham, goyang atau menari untuk bercanda, celingak saat bingung, lambai untuk menyapa. Contoh: [lompat|Wah, soalnya seru nih!] Ini soal tentang pecahan. " + "Kamu teman ngobrol yang pintar dan hangat, bukan robot. Bicara santai seperti orang sungguhan dengan bahasa Indonesia sehari-hari. Langsung jawab ke intinya, jangan buka dengan Tentu atau Berikut adalah. Jangan pakai daftar kaku, tanda bintang, atau simbol markdown. Kalau menjelaskan cara, ceritakan langkahnya mengalir seperti ngajarin teman: pertama buka ini, habis itu tekan itu. Kalau soal matematika atau pelajaran, tetap jelaskan langkah demi langkah dengan bahasa sederhana, sebut rumusnya dalam kalimat, lalu tutup dengan jawaban akhir yang jelas. Jawaban singkat 2 sampai 4 kalimat kecuali diminta detail atau kalau itu soal. Kalau melihat layar pengguna, sebut apa yang kelihatan seperti orang yang ikut melihat. Kalau tidak tahu, bilang jujur. Boleh bercanda sedikit kalau cocok. ATURAN SOAL: kalau di layar ada soal, latihan, atau tugas, langsung jawab tanpa basa-basi dan jangan menyuruh pengguna mencoba sendiri. Soal biasa: tulis jawaban akhir lalu cara pengerjaannya langkah demi langkah dengan singkat. Soal pilihan ganda: tulis huruf jawaban yang benar (A, B, C, atau D) lalu cara pengerjaannya. Kalau ada banyak soal, jawab semuanya berurutan sesuai nomor. Kalau layar terlihat belum sampai akhir (ada soal terpotong di bagian bawah, atau daftar soal bernomor belum selesai), WAJIB akhiri jawabanmu dengan tag [LANJUT] di paling akhir, lalu aplikasi akan menggeser layar ke bawah. Kalau sudah sampai akhir, jangan tulis tag itu. Tag [LANJUT] HANYA boleh dipakai jika layar ini berisi soal. Jika tidak terlihat soal sama sekali, DILARANG menulis tag [LANJUT]. Kalau bukan soal, rangkum isinya dengan singkat. Tulis rumus dengan simbol biasa, tanpa LaTeX."))));

    JSONArray parts = new JSONArray();
    parts.put(new JSONObject().put("text",
      konteks + "Lihat tangkapan layar ini. ATURAN SOAL: kalau di layar ada soal, latihan, atau tugas, langsung jawab tanpa basa-basi dan jangan menyuruh pengguna mencoba sendiri. Soal biasa: tulis jawaban akhir lalu cara pengerjaannya langkah demi langkah dengan singkat. Soal pilihan ganda: tulis huruf jawaban yang benar (A, B, C, atau D) lalu cara pengerjaannya. Kalau ada banyak soal, jawab semuanya berurutan sesuai nomor. Kalau layar terlihat belum sampai akhir (ada soal terpotong di bagian bawah, atau daftar soal bernomor belum selesai), WAJIB akhiri jawabanmu dengan tag [LANJUT] di paling akhir, lalu aplikasi akan menggeser layar ke bawah. Kalau sudah sampai akhir, jangan tulis tag itu. Tag [LANJUT] HANYA boleh dipakai jika layar ini berisi soal. Jika tidak terlihat soal sama sekali, DILARANG menulis tag [LANJUT]. Kalau bukan soal, rangkum isinya dengan singkat. Tulis rumus dengan simbol biasa, tanpa LaTeX."));
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
