export function boleh(req, env) {
  const t = req.headers.get("x-app-token");
  if (!t) return false;
  if (t === env.APP_TOKEN) return true;
  return (env.APP_TOKENS || "").split(",").map(x => x.split(":")[1]?.trim()).includes(t);
}
