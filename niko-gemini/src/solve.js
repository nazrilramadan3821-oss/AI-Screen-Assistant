import { boleh } from "./auth.js";
const CORS = {
  "Access-Control-Allow-Origin": "https://appassets.androidplatform.net",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, x-app-token",
};
const reply = (data, status = 200) =>
  Response.json(data, { status, headers: CORS });

export async function solve(req, env) {
  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS });
  if (req.method !== "POST") return reply({ error: "Harus POST" }, 405);
  if (!boleh(req, env))
    return reply({ error: "Token salah" }, 401);

  try {
    const body = await req.text();
    if (body.length > 100000) return reply({ error: "Terlalu besar" }, 413);
    const { questions } = JSON.parse(body);
    if (!Array.isArray(questions) || !questions.length)
      return reply({ error: "Soal kosong" }, 400);

    const prompt = 'Jawab soal latihan pilihan ganda ini. Balas JSON {"answers":[{"answer":"..."}]} ' +
      "dengan urutan sama. answer harus sama persis dengan salah satu options.\n" +
      JSON.stringify(questions);
    const r = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent",
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_KEY },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: { responseMimeType: "application/json" },
        }),
      }
    );
    if (!r.ok) return reply({ error: "Gemini error " + r.status }, 502);
    const data = await r.json();
    const text = data.candidates[0].content.parts.map(p => p.text || "").join("");
    const { answers } = JSON.parse(text);
    if (!Array.isArray(answers)) throw new Error("format");
    return reply({ answers });
  } catch (e) {
    return reply({ error: "Gagal memproses" }, 500);
  }
}
