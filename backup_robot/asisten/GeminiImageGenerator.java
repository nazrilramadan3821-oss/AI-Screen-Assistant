package com.nazril.asisten;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GeminiImageGenerator {

    private GeminiImageGenerator() {}

    public static Bitmap generate(String apiKey, String prompt)
            throws Exception {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key belum diatur.");
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Deskripsi gambar tidak boleh kosong."
            );
        }

        String endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/"
            + "gemini-2.5-flash-image:generateContent";

        URL url = new URL(endpoint);
        HttpURLConnection conn =
            (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(90000);
            conn.setDoOutput(true);
            conn.setRequestProperty(
                "Content-Type", "application/json; charset=UTF-8"
            );
            conn.setRequestProperty("x-goog-api-key", apiKey);

            JSONObject part = new JSONObject();
            part.put("text",
                "Buat satu gambar berdasarkan deskripsi berikut: "
                + prompt.trim()
            );

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            JSONObject imageConfig = new JSONObject();
            imageConfig.put("responseModalities",
                new JSONArray().put("IMAGE"));

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseModalities",
                new JSONArray().put("IMAGE").put("TEXT"));

            body.put("generationConfig", generationConfig);

            byte[] payload =
                body.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int status = conn.getResponseCode();

            InputStream stream = status >= 200 && status < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

            String response;
            try (InputStream is = stream;
                 ByteArrayOutputStream out =
                     new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int count;

                while ((count = is.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }

                response = out.toString("UTF-8");
            }

            if (status < 200 || status >= 300) {
                throw new Exception(
                    "Gemini API HTTP " + status + ": " + response
                );
            }

            JSONObject result = new JSONObject(response);
            JSONArray candidates = result.optJSONArray("candidates");

            if (candidates == null || candidates.length() == 0) {
                throw new Exception(
                    "AI tidak mengembalikan gambar: " + response
                );
            }

            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject resultContent = candidate.optJSONObject("content");

            if (resultContent == null) {
                throw new Exception("Respons gambar tidak tersedia.");
            }

            JSONArray resultParts = resultContent.optJSONArray("parts");

            if (resultParts != null) {
                for (int i = 0; i < resultParts.length(); i++) {
                    JSONObject resultPart =
                        resultParts.getJSONObject(i);

                    JSONObject inlineData =
                        resultPart.optJSONObject("inlineData");

                    if (inlineData == null) {
                        inlineData = resultPart.optJSONObject("inline_data");
                    }

                    if (inlineData != null) {
                        String mimeType =
                            inlineData.optString("mimeType",
                                inlineData.optString("mime_type", ""));

                        String encoded = inlineData.optString("data", "");

                        if (mimeType.startsWith("image/")
                                && !encoded.isEmpty()) {
                            byte[] imageBytes =
                                Base64.decode(encoded, Base64.DEFAULT);

                            Bitmap bitmap = BitmapFactory.decodeByteArray(
                                imageBytes, 0, imageBytes.length
                            );

                            if (bitmap != null) {
                                return bitmap;
                            }
                        }
                    }
                }
            }

            throw new Exception(
                "Gambar tidak ditemukan dalam respons. "
                + "Pastikan model mendukung pembuatan gambar. "
                + response
            );

        } finally {
            conn.disconnect();
        }
    }
}
