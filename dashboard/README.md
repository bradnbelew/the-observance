# The Observance Dashboard

Control surface for The Observance, an emergent-horror Minecraft ARG backed by Supabase.

The Minecraft plugin writes live game state into Supabase: players, dossiers, heatmap cells, beat queue, arc state, settings, and event logs. This Next.js app gives the operator two views onto that state.

## Modes

- `/status` is spoiler-free. It reads only neutral public views such as health, heatmap, and compliance counts.
- `/author` is the full-spoiler operator console. It includes arc control, beat approval, whisper budgets, bond ledger, named dossiers, Watcher sleep, ending preview/override, and director run status.

Author mode is intentionally not sign-in gated. Anyone who can reach the deployed `/author` URL can use the operator controls, so protect the dashboard URL outside the app: private deployment, Vercel protection, VPN, localhost, or another access wall.

## Stack

- Next.js 15 App Router
- TypeScript
- Tailwind CSS
- Supabase via `@supabase/supabase-js` and `@supabase/ssr`

Supabase clients:

- `src/lib/supabase/server.ts`: request-scoped anon client for public/status reads.
- `src/lib/supabase/client.ts`: browser anon client for client components.
- `src/lib/supabase/admin.ts`: server-only service-role client for `/author` reads and writes.

Never expose `SUPABASE_SERVICE_ROLE_KEY` to the browser.

## Environment

Required variables:

```dotenv
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<anon/publishable key>
SUPABASE_SERVICE_ROLE_KEY=<service_role secret key>
```

The dashboard and Minecraft plugin should point at the same Supabase project.

## Local Development

```bash
npm install
npm run dev
```

Open:

- `http://localhost:3000/status`
- `http://localhost:3000/author`

`/author` opens directly. Keep local tunnels and deployed URLs private.

## Database Migrations

For launch, do not apply the dashboard folder by itself. Use the generated full-project bundle instead:

```bash
cd ../discord
npm run db:seed
```

Then apply `discord/supabase/apply-all.sql` in Supabase. That bundle includes the dashboard base,
Discord/oracle schema, public Record/Archive views, seeds, and schema repair in dependency order.
The dashboard views depend on Discord tables such as `solves`, `threads`, and `thread_cards`, so the
single bundle is the safe path.

## Deploy

1. Deploy the dashboard to Vercel or another trusted host.
2. Set the three required environment variables.
3. Protect the deployed URL before sharing it.
4. Open `/status` for spoiler-free health checks and `/author` only for operators.

## Scripts

- `npm run dev`: local dev server.
- `npm run build`: production build.
- `npm run start`: serve production build.
- `npm run lint`: lint check.
