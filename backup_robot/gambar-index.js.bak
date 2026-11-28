export default {
  async fetch(req, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    };
    if (req.method === "OPTIONS") return new Response(null, { headers: cors });
    if (req.method !== "POST") return new Response("NIKO gambar", { headers: cors });
    try {
      const { prompt } = await req.json();
      if (!prompt || !prompt.trim()) {
        return Response.json({ error: "Prompt kosong" }, { status: 400, headers: cors });
      }
      const out = await env.AI.run("@cf/black-forest-labs/flux-1-schnell", {
        prompt: prompt.trim(),
        steps: 4,
      });
      return Response.json({ image: out.image }, { headers: cors });
    } catch (e) {
      return Response.json({ error: String(e.message || e) }, { status: 500, headers: cors });
    }
  },
};
