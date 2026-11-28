import { solve } from "./solve.js";
export default {
  async fetch(req, env) {
    if (new URL(req.url).pathname === "/api/solve") return solve(req, env);
    if (req.method !== "POST") return new Response("NIKO Gemini OK");
    if (req.headers.get("x-app-token") !== env.APP_TOKEN) {
      return new Response("Unauthorized", { status: 401 });
    }
    const body = await req.text();
    if (body.length > 4000000) return new Response("Too large", { status: 413 });
    const model = new URL(req.url).searchParams.get("model") || "gemini-2.0-flash";
    const r = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models/" +
        encodeURIComponent(model) + ":generateContent",
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_KEY },
        body,
      }
    );
    return new Response(await r.text(), {
      status: r.status,
      headers: { "Content-Type": "application/json" },
    });
  },
};
