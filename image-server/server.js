const express = require("express");
const app = express();
app.use(express.json({ limit: "10mb" }));
app.use(express.static("public"));

let lastImage = null;
const MODEL = process.env.CF_MODEL || "@cf/black-forest-labs/flux-1-schnell";

async function runCF(url, token, body) {
  const r = await fetch(url, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await r.json().catch(() => ({}));
  return { ok: r.ok && data.success !== false, data };
}

app.post("/api/generate", async (req, res) => {
  try {
    const token = process.env.CF_API_TOKEN;
    const account = process.env.CF_ACCOUNT_ID;
    if (!token || !account) return res.status(500).json({ error: "CF_API_TOKEN atau CF_ACCOUNT_ID belum diset" });
    const prompt = (req.body.prompt || "").trim();
    if (!prompt) return res.status(400).json({ error: "Prompt kosong" });
    let width = 1024, height = 1024;
    if (prompt.includes("rasio 16:9")) { width = 1024; height = 576; }
    if (prompt.includes("rasio 9:16")) { width = 576; height = 1024; }
    const url = `https://api.cloudflare.com/client/v4/accounts/${account}/ai/run/${MODEL}`;
    let out = await runCF(url, token, { prompt, steps: 4, width, height });
    if (!out.ok) out = await runCF(url, token, { prompt, steps: 4 });
    if (!out.ok || !out.data.result || !out.data.result.image) {
      const e0 = out.data.errors && out.data.errors[0];
      return res.status(500).json({ error: String((e0 && e0.message) || "Gagal membuat gambar").slice(0, 300) });
    }
    lastImage = out.data.result.image;
    res.json({ image: `data:image/jpeg;base64,${out.data.result.image}` });
  } catch (e) {
    res.status(500).json({ error: String(e.message || e).slice(0, 300) });
  }
});
app.get("/api/last.jpg", (req, res) => {
  if (!lastImage) return res.status(404).send("Belum ada gambar");
  res.set({ "Content-Type": "image/jpeg", "Content-Disposition": "attachment; filename=gambar-ai.jpg" });
  res.send(Buffer.from(lastImage, "base64"));
});
app.listen(process.env.PORT || 3000, () => console.log("AI Image Server berjalan di port 3000"));
