# The Observance — Dashboard

The control surface for **The Observance**, an emergent-horror Minecraft experience.
A companion Minecraft plugin (connecting to Supabase with the **service-role** key)
writes live game state — players, dossiers, heatmap cells, beat queue, arc state — into
a Postgres database. This Next.js app is the human-facing window onto that state, with
two distinct modes:

- **`/status` — spoiler-free.** Anyone running the world can watch this. It reads
  **only** the three neutral `v_*` views (`v_health`, `v_heatmap`,
  `v_compliance_counts`): is it running, is it misfiring, where are people moving, and
  the neutral compliance counts. No player names, no custom labels, no story.
- **`/author` — full spoilers, admin only.** The arc control, the beat-approval queue,
  whisper budgets, the bond ledger, and named dossiers. Gated by an email allowlist
  (`ADMIN_EMAILS`) and Supabase magic-link auth. Every write goes through an
  admin-gated server action.

## Tech stack

- **Next.js 15** (App Router) + **TypeScript** + **Tailwind CSS**. Server Components by
  default; `"use client"` only where interactivity is needed; **Server Actions** for all
  writes.
- **Supabase** via `@supabase/supabase-js` and `@supabase/ssr`.
- Three Supabase clients, used deliberately:
  - `src/lib/supabase/server.ts` — request-scoped, **anon key**, RLS-enforced. Used by
    Server Components, the auth callback, and login. `/status` reads through this and can
    only see the `v_*` views.
  - `src/lib/supabase/client.ts` — browser, anon key, RLS-enforced (available for client
    components).
  - `src/lib/supabase/admin.ts` — **service-role key, server-only.** Bypasses RLS. Used
    **only** inside server actions, and every action re-checks `isAdmin()` before it
    touches this client.
- **Deploy target: Vercel.**

## Prerequisites

- **Node.js 18.18+ or 20+** (Next.js 15 requirement; 20 LTS recommended) and npm.
- **A Supabase project in BRADEN'S Supabase account** — **not** the Voxaris / Ethan
  account. The dashboard, the Minecraft plugin, and the `ADMIN_EMAILS` allowlist all
  point at the same single project, and that project must live in Braden's account so he
  owns the data and keys. When you create env values below, take them from **that**
  project.

## Environment variables

The app reads exactly four variables. **Never hardcode these in source** — they live in
`.env.local` for local dev and in Vercel project settings for deploys.

| Variable | Where it's used | Notes |
| --- | --- | --- |
| `NEXT_PUBLIC_SUPABASE_URL` | browser + server | Public. Supabase **Project URL**. |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | browser + server | Public, RLS-protected. The **anon / publishable** key. |
| `SUPABASE_SERVICE_ROLE_KEY` | **server only** | **Secret. Bypasses RLS.** Never expose to the browser or commit it. |
| `ADMIN_EMAILS` | server only | Comma-separated allowlist of emails allowed into `/author`. Use Braden's email. |

Find the URL and keys in the Supabase dashboard for Braden's project under
**Project Settings → API** (Project URL, `anon` public key, and `service_role` secret
key). `.env.example` documents the same four.

## Local development

1. **Install dependencies**

   ```bash
   npm install
   ```

2. **Create `.env.local`** in the project root, pointed at **Braden's** Supabase project:

   ```bash
   cp .env.example .env.local
   ```

   Then fill in the four values from Braden's project:

   ```dotenv
   NEXT_PUBLIC_SUPABASE_URL=https://<braden-project-ref>.supabase.co
   NEXT_PUBLIC_SUPABASE_ANON_KEY=<braden anon/publishable key>
   SUPABASE_SERVICE_ROLE_KEY=<braden service_role secret key>
   ADMIN_EMAILS=braden@example.com
   ```

   `.env.local` is gitignored — keep the service-role key out of version control.

3. **Apply the database migrations** (see next section) to Braden's project.

4. **Run the dev server**

   ```bash
   npm run dev
   ```

   Open <http://localhost:3000>. `/status` works immediately. For `/author`, sign in at
   `/auth/login` with an email listed in `ADMIN_EMAILS`; Supabase emails a magic link
   that routes through `/auth/callback`.

## Applying the database migrations

The schema lives in `supabase/migrations/`:

- `0001_init.sql` — tables, indexes, `updated_at` triggers, **RLS on every table**, the
  `authenticated` full-access policies, the grant lock-down, and the three spoiler-free
  views (`v_health`, `v_heatmap`, `v_compliance_counts`) granted to `anon`.
- `0002_seed.sql` — idempotent seed of the control rows the app and plugin depend on (the
  `settings` keys and the single `arc_state` row). Safe to re-run.

Apply them **in order** to Braden's project, using either path:

### Option A — Supabase SQL editor (no tooling)

1. Open Braden's project → **SQL Editor**.
2. Paste the full contents of `supabase/migrations/0001_init.sql`, run it.
3. Paste the full contents of `supabase/migrations/0002_seed.sql`, run it.

### Option B — Supabase CLI

```bash
# one-time
npm install -g supabase   # or: brew install supabase/tap/supabase

# link this repo to Braden's project (grab the ref from the project URL / dashboard)
supabase link --project-ref <braden-project-ref>

# push every migration in supabase/migrations in order
supabase db push
```

> Security note: the views are `SECURITY DEFINER`, `anon` has **no** table grants and no
> base-table RLS policies, so the only thing the public `/status` page can physically read
> is the three neutral views. Don't relax those grants.

## Deploying to Vercel

1. Push this repo to GitHub (or import the project directly into Vercel).
2. In Vercel, **New Project → Import** this repository. Framework preset auto-detects as
   **Next.js**; no build-command overrides are needed (`next build`).
3. **Set the same four environment variables** in **Vercel → Project → Settings →
   Environment Variables**, pointed at **Braden's** Supabase project:
   - `NEXT_PUBLIC_SUPABASE_URL`
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY`
   - `SUPABASE_SERVICE_ROLE_KEY` (mark as a secret; server-only)
   - `ADMIN_EMAILS`

   Add them to **Production** (and Preview/Development if you use those environments).
4. Deploy. After the first deploy, in Supabase **Authentication → URL Configuration** add
   the deployed origin (e.g. `https://your-app.vercel.app`) to the **Site URL** /
   **Redirect URLs** so the magic-link callback (`/auth/callback`) is allowed in
   production.

## Project layout

```
src/
  app/
    layout.tsx              Root layout + nav (noindex)
    page.tsx                Landing: Status vs Author
    status/page.tsx         Spoiler-free; reads ONLY v_* views
    author/
      page.tsx              Admin-only control surface (isAdmin gate + redirect)
      actions.ts            Server actions; each re-checks isAdmin() + uses admin client
    auth/
      login/                Magic-link sign-in (page + action)
      callback/route.ts     PKCE code -> session, redirect to /author
  components/
    status/                 HealthPanel, Heatmap, ComplianceCounts (view rows only)
    author/                 ArcControl, BeatQueue, WhisperBudgets, BondLedger,
                            Dossiers, WatcherSleepToggle, AcceptingTrigger
  lib/
    auth.ts                 getUser() + isAdmin() (ADMIN_EMAILS allowlist)
    database.types.ts       Single source of truth for row/view types
    supabase/{server,client,admin}.ts
  middleware.ts             Refreshes the Supabase session on every request
supabase/migrations/        0001_init.sql, 0002_seed.sql
```

## Scripts

- `npm run dev` — local dev server.
- `npm run build` — production build (`next build`).
- `npm run start` — serve a production build.
- `npm run lint` — ESLint (`next lint`, `next/core-web-vitals`).
