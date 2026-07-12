# The Observance Website / Dashboard

Public web surface and operator dashboard for The Observance, an emergent-horror Minecraft server backed by Supabase.

The public root (`/`) is the retired Copperline Hosting company site. It contains ordinary product, support, announcement, community, and server-directory pages. The Observance is buried at `/server.php?id=1842`; that row owns the configured live address and leads through its account activity to the world copy and Record. The Minecraft plugin writes live state into Supabase, while `/author` is the authenticated director console.

## Modes

- `/` is the ordinary Copperline company homepage and contains no direct ARG navigation.
- `/server-list.php` and `/server.php?id=1842` form the expired customer-directory trail.
- `/community/2011/02/08/world-backup` carries the Hold download and the quiet bridge to the Record lure.
- `/status` is spoiler-free. It reads only neutral public views such as health, heatmap, and compliance counts.
- `/author` is the full-spoiler operator console. It includes arc control, beat approval, whisper budgets, bond ledger, named dossiers, Watcher sleep, ending preview/override, and director run status.
- `/record/the-record-keeps`, `/record/the-record`, `/record/archive`, and `/record/terminal` are in-fiction recovered archive surfaces. They remain `noindex`.

Author mode is sign-in gated with Supabase passwordless authentication and the `ADMIN_EMAILS` allowlist. Every privileged server action re-checks authorization before constructing the service-role client.

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
ADMIN_EMAILS=<comma-separated operator email addresses>
```

The dashboard and Minecraft plugin should point at the same Supabase project.
Leave `NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS` blank until the live Paper address is safe to reveal; Copperline service `1842` will render its old address row as unavailable.

## Local Development

```bash
npm install
npm run dev
```

Open:

- `http://localhost:3000/`
- `http://localhost:3000/status`
- `http://localhost:3000/author`

`/author` redirects to `/author/login` until an allowlisted operator signs in.

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
