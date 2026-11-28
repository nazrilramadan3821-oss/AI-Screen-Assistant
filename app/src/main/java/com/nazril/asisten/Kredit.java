package com.nazril.asisten;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class Kredit {
  private static Context app;
  private static final TimeZone PASIFIK = TimeZone.getTimeZone("America/Los_Angeles");

  public static void siap(Context c) { if (c != null) app = c.getApplicationContext(); }

  private static SharedPreferences data() {
    SharedPreferences s = app.getSharedPreferences("kredit", Context.MODE_PRIVATE);
    SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
    f.setTimeZone(PASIFIK);
    String hari = f.format(new Date());
    if (!hari.equals(s.getString("hari", "")))
      s.edit().putString("hari", hari).putInt("pakai", 0).putInt("tolak", 0).apply();
    return s;
  }

  public static synchronized void catat(int kode) {
    if (app == null) return;
    SharedPreferences s = data();
    if (kode >= 200 && kode < 300) s.edit().putInt("pakai", s.getInt("pakai", 0) + 1).apply();
    else if (kode == 429) s.edit().putInt("tolak", s.getInt("tolak", 0) + 1).apply();
  }

  public static synchronized void aturBatas(int n) {
    if (app != null && n > 0) data().edit().putInt("batas", n).apply();
  }

  public static synchronized String ringkasan() {
    SharedPreferences s = data();
    int batas = s.getInt("batas", 500), pakai = s.getInt("pakai", 0), tolak = s.getInt("tolak", 0);
    Calendar c = Calendar.getInstance(PASIFIK);
    c.add(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    String reset = new SimpleDateFormat("dd/MM HH:mm", Locale.US).format(c.getTime());
    String teks = "Kredit Gemini (perkiraan)\n\nSisa: " + Math.max(0, batas - pakai) + " dari " + batas
      + "\nTerpakai hari ini: " + pakai + "\nDitolak (429): " + tolak + "\nReset: " + reset
      + (tolak > 0 ? "\n\nGemini sempat menolak. Batas mungkin habis atau permintaan terlalu cepat." : "")
      + "\n\nHanya menghitung pemakaian di HP ini.";
    try { return new org.json.JSONObject().put("teks", teks).put("batas", batas).toString(); }
    catch (Exception e) { return "{}"; }
  }
}
