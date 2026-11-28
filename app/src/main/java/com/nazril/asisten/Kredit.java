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
      s.edit().putString("hari", hari).putInt("pakai_chat", 0).putInt("tolak_chat", 0)
        .putInt("pakai_lite", 0).putInt("tolak_lite", 0).apply();
    return s;
  }

  private static String kelompok(String model) {
    if (model == null) return null;
    if (model.contains("lite")) return "lite";
    if (model.contains("3.6-flash")) return "chat";
    return null;
  }

  public static synchronized void catat(String model, int kode) {
    String k = kelompok(model);
    if (app == null || k == null) return;
    SharedPreferences s = data();
    String kunci = kode >= 200 && kode < 300 ? "pakai_" + k : kode == 429 ? "tolak_" + k : null;
    if (kunci != null) s.edit().putInt(kunci, s.getInt(kunci, 0) + 1).apply();
  }

  public static synchronized void aturBatas(int chat, int lite) {
    if (app != null && chat > 0 && lite > 0)
      data().edit().putInt("batas_chat", chat).putInt("batas_lite", lite).apply();
  }

  private static String baris(SharedPreferences s, String judul, String k, int batas) {
    int pakai = s.getInt("pakai_" + k, 0), tolak = s.getInt("tolak_" + k, 0);
    return judul + "\nSisa: " + Math.max(0, batas - pakai) + " dari " + batas + " (terpakai " + pakai + ")"
      + (tolak > 0 ? "\nDitolak Google: " + tolak + " kali" : "") + "\n\n";
  }

  public static synchronized String ringkasan() {
    SharedPreferences s = data();
    int chat = s.getInt("batas_chat", 20), lite = s.getInt("batas_lite", 500);
    Calendar c = Calendar.getInstance(PASIFIK);
    c.add(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    String reset = new SimpleDateFormat("dd/MM HH:mm", Locale.US).format(c.getTime());
    String teks = "Kredit Gemini (perkiraan)\n\n"
      + baris(s, "\uD83D\uDCAC Chat (3.6 Flash)", "chat", chat)
      + baris(s, "\uD83E\uDD16 Layar & robot (Flash Lite)", "lite", lite)
      + "Reset: " + reset + "\nHanya menghitung HP ini.";
    try { return new org.json.JSONObject().put("teks", teks).put("chat", chat).put("lite", lite).toString(); }
    catch (Exception e) { return "{}"; }
  }
}
