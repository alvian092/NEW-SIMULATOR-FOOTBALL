const BASE = "https://api.soccer-data.co/v1";

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "public, max-age=60", "access-control-allow-origin": "*" }
  });
}
function cleanPath(path) {
  if (!path.startsWith("/")) return null;
  if (path.includes("..") || path.includes("://")) return null;
  return path;
}
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { headers: {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, OPTIONS",
      "access-control-allow-headers": "content-type"
    }});
    if (request.method !== "GET") return json({ error: "METHOD_NOT_ALLOWED" }, 405);
    if (url.pathname === "/health") return json({ ok: true, provider: "Soccer Data", apiKeyConfigured: Boolean(env.SOCCER_DATA_API_KEY) });
    if (!env.SOCCER_DATA_API_KEY) return json({ error: "API_KEY_NOT_CONFIGURED", message: "Set SOCCER_DATA_API_KEY as a Worker secret." }, 503);

    const routes = {
      "/api/competitions": "/competitions",
      "/api/teams": "/teams",
      "/api/matches": "/matches",
      "/api/coverage": "/coverage/leagues",
      "/api/coverage/summary": "/coverage/summary"
    };
    let upstreamPath = routes[url.pathname];
    if (!upstreamPath && url.pathname.startsWith("/api/matches/")) {
      const id = url.pathname.slice("/api/matches/".length);
      if (!/^[a-zA-Z0-9_-]+$/.test(id)) return json({ error: "INVALID_MATCH_ID" }, 400);
      upstreamPath = "/matches/" + encodeURIComponent(id) + "/stats";
    }
    if (!upstreamPath) return json({ error: "NOT_FOUND" }, 404);

    const upstream = new URL(BASE + upstreamPath);
    for (const [key, value] of url.searchParams) upstream.searchParams.set(key, value);
    try {
      const response = await fetch(upstream.toString(), {
        headers: { "X-Api-Key": env.SOCCER_DATA_API_KEY, "Accept": "application/json" }
      });
      const body = await response.text();
      return new Response(body, { status: response.status, headers: {
        "content-type": response.headers.get("content-type") || "application/json",
        "cache-control": response.ok ? "public, max-age=300" : "no-store",
        "access-control-allow-origin": "*"
      }});
    } catch (error) {
      return json({ error: "UPSTREAM_UNAVAILABLE", message: "Soccer Data API could not be reached." }, 502);
    }
  }
};
