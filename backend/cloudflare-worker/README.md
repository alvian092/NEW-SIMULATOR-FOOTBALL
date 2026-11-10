# Cloudflare Worker — Soccer Data proxy

This worker keeps the Soccer Data API key on the server. Never put the provider key in the Android APK or commit it to Git.

## Deploy

1. Create a Cloudflare account and install Wrangler.
2. From this directory, run `npx wrangler login`.
3. Add the provider key as a secret (not a plain variable):

   ```bash
   npx wrangler secret put SOCCER_DATA_API_KEY
   ```

4. Deploy:

   ```bash
   npx wrangler deploy
   ```

5. Save the Worker URL. Test `/health`; it should return `ok: true` and `apiKeyConfigured: true`.

## Proxy routes

- `GET /api/competitions`
- `GET /api/teams?competition_id=...` (provider-supported filters are forwarded)
- `GET /api/matches?team_id=...&status=finished&sort=utc_date&per_page=...`
- `GET /api/coverage`
- `GET /api/coverage/summary`
- `GET /api/matches/{match_id}` — match stats, including xG when covered
- `GET /health`

All query parameters are forwarded to the matching Soccer Data API endpoint. The app must check competition/season coverage for xG before assuming it exists. The Worker intentionally does not invent missing xG values.

## Important

The Worker source is committed, but it is not deployed until you deploy it to your Cloudflare account. Android integration needs the resulting Worker URL. Provider API key stays as a Cloudflare Worker secret.
