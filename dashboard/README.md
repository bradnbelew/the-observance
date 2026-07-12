# The Observance Website / Dashboard

Public web surface and operator dashboard for The Observance, an emergent-horror Minecraft server backed by Supabase.

The public root (`/`) is an abandoned-looking server/map host: it exposes the downloadable opening map, the live server address when configured, and the recovered Record links. The Minecraft plugin writes live game state into Supabase: players, dossiers, heatmap cells, beat queue, arc state, settings, and event logs. The operator pages give the director two views onto that state.

## Modes

- `/` is the in-fiction server listing players reach after the opening map. It should be safe to share once the live server address is ready.
- `/status` is spoiler-free. It reads only neutral public views such as health, heatmap, and compliance counts.
- `/author` is the full-spoiler operator console. It includes arc control, beat approval, whisper budgets, bond ledger, named dossiers, Watcher sleep, ending preview/override, and director run status.
- `/record/the-record-keeps`, `/record/the-record`, `/record/archive`, and `/record/terminal` are in-fiction recovered archive surfaces. They remain `noindex`.

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
NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS=<host:port shown on the public listing>
```

The dashboard and Minecraft plugin should point at the same Supabase project.
Leave `NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS` blank until the live Paper address is safe to reveal; `/` will render it as withheld.

## Local Development

```bash
npm install
npm run dev
```

Open:

- `http://localhost:3000/`
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
