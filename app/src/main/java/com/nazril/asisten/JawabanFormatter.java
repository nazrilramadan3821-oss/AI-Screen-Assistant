package com.nazril.asisten;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JawabanFormatter {
    private static final int WARNA_NOMOR = Color.parseColor("#7CC4FF");
    private static final int WARNA_JAWABAN = Color.parseColor("#FFD54F");
    private static final boolean UBAH_KATA_JADI_SIMBOL = true;
    private static final String[] URUTAN = {"pertama","kedua","ketiga","keempat","kelima","keenam","ketujuh","kedelapan","kesembilan","kesepuluh"};
    private static final Pattern POLA_SOAL = Pattern.compile(
        "(?i)soal\\s+(?:nomor\\s*)?(\\d+|pertama|kedua|ketiga|keempat|kelima|keenam|ketujuh|kedelapan|kesembilan|kesepuluh)\\s+jawabannya\\s+([A-E])\\b\\s*[.,:]?");
    private static final int FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

    private JawabanFormatter() {}

    public static SpannableStringBuilder format(String mentah) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        if (mentah == null) return out;
        String teks = bersihkan(mentah);
        Matcher m = POLA_SOAL.matcher(teks);
        int prevEnd = 0;
        String prevNo = null, prevHuruf = null;
        while (m.find()) {
            if (prevNo == null) tambahParagraf(out, teks.substring(0, m.start()));
            else tambahBlok(out, prevNo, prevHuruf, teks.substring(prevEnd, m.start()));
            prevNo = nomor(m.group(1));
            prevHuruf = m.group(2).toUpperCase();
            prevEnd = m.end();
        }
        if (prevNo == null) tambahParagraf(out, teks);
        else tambahBlok(out, prevNo, prevHuruf, teks.substring(prevEnd));
        while (out.length() > 0 && out.charAt(out.length() - 1) == '\n')
            out.delete(out.length() - 1, out.length());
        return out;
    }

    private static void tambahBlok(SpannableStringBuilder out, String no, String huruf, String cara) {
        String bNo = "No. " + no, bJwb = "Jawaban: " + huruf;
        int s = out.length();
        out.append(bNo).append("   ").append(bJwb);
        int e = out.length();
        out.setSpan(new StyleSpan(Typeface.BOLD), s, e, FLAG);
        out.setSpan(new RelativeSizeSpan(1.1f), s, e, FLAG);
        out.setSpan(new ForegroundColorSpan(WARNA_NOMOR), s, s + bNo.length(), FLAG);
        out.setSpan(new ForegroundColorSpan(WARNA_JAWABAN), e - bJwb.length(), e, FLAG);
        out.append("\n");
        for (String k : pecahKalimat(cara)) out.append("• ").append(rapikanRumus(k)).append("\n");
        out.append("\n");
    }

    private static void tambahParagraf(SpannableStringBuilder out, String isi) {
        List<String> ks = pecahKalimat(isi);
        if (ks.isEmpty()) return;
        for (String k : ks) out.append("• ").append(rapikanRumus(k)).append("\n");
        out.append("\n");
    }

    private static List<String> pecahKalimat(String s) {
        List<String> hasil = new ArrayList<>();
        s = s.replaceAll("^[\\s.,:;]+", "").trim();
        if (s.isEmpty()) return hasil;
        for (String k : s.split("(?<=[.!?])\\s+")) {
            k = k.trim();
            if (k.length() > 1) hasil.add(k);
        }
        return hasil;
    }

    private static String nomor(String s) {
        if (s.matches("\\d+")) return s;
        for (int i = 0; i < URUTAN.length; i++)
            if (URUTAN[i].equalsIgnoreCase(s)) return String.valueOf(i + 1);
        return s;
    }

    private static String bersihkan(String s) {
        s = s.replace("\\times", "×").replace("\\cdot", "×").replace("\\div", "÷");
        s = s.replace("\\left", "").replace("\\right", "");
        s = s.replaceAll("\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}", "$1/$2");
        s = s.replace("\\(", "").replace("\\)", "").replace("\\[", "").replace("\\]", "");
        s = s.replace("$", "").replace("**", "").replace("__", "").replace("`", "");
        s = s.replaceAll("(?m)^\\s*#+\\s*", "");
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String rapikanRumus(String s) {
        if (!UBAH_KATA_JADI_SIMBOL) return s;
        s = s.replaceAll("(\\d+)\\s+per\\s+(\\d+)", "$1/$2");
        s = s.replaceAll("(?i)\\s+sama dengan\\s+", " = ");
        s = s.replaceAll("(?i)\\s+(?:dikalikan|dikali|kali)\\s+", " × ");
        s = s.replaceAll("(?i)\\s+(?:ditambahkan|ditambah)\\s+", " + ");
        s = s.replaceAll("(?i)\\s+dikurangi\\s+", " − ");
        s = s.replaceAll("(?i)\\s+dibagi\\s+", " ÷ ");
        return s;
    }
}
