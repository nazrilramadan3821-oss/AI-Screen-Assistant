export default {
  async fetch(req, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, x-app-token",
    };
    if (req.method === "OPTIONS") return new Response(null, { headers: cors });
    if (req.method !== "POST") return new Response("NIKO gambar", { headers: cors });
    const cek = await env.AUTH.fetch("https://auth/api/cek", { headers: { "x-app-token": req.headers.get("x-app-token") || "" } });
    if (cek.status !== 204) return Response.json({ error: "Token salah" }, { status: 401, headers: cors });
    try {
      const { prompt } = await req.json();
      if (!prompt || !prompt.trim()) {
        return Response.json({ error: "Prompt kosong" }, { status: 400, headers: cors });
      }
      let inggris = prompt.trim();
      try {
        const h = await env.AI.run("@cf/meta/m2m100-1.2b", { text: inggris, source_lang: "id", target_lang: "en" });
        if (h.translated_text) inggris = h.translated_text;
      } catch (e) {}
      const out = await env.AI.run("@cf/black-forest-labs/flux-1-schnell", {
        prompt: inggris,
        steps: 4,
      });
      return Response.json({ image: out.image }, { headers: cors });
    } catch (e) {
      return Response.json({ error: String(e.message || e) }, { status: 500, headers: cors });
    }
  },
};
