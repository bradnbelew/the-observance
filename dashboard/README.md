# The Observance website and operations console

Next.js 16 public ARG surface plus authenticated V5 director console.

## Public routes

- Copperline pages are an ordinary legacy-hosting trail for C01.
- `/server-list.php` exposes an opaque damaged row, not the service number. Opening that row receipts
  LS01 and presents an A1Z26 account docket. A dedicated server-side form accepts only the normalized
  LS02 answer, then reveals the full service page and archived ticket 9137; wrong inputs, missing
  prerequisites, and backend failures share the same non-oracular response.
- `/support/ticket.php?id=9137` opens only after that service page and links the community attachment.
- `/community/2011/02/08/world-backup` exposes the private playable world only after LS03. The
  download Route Handler verifies the checked-in checksum and atomically records LS04 before sending
  the ZIP. Inside the world, the three-room North Annex investigation reconstructs the Minecraft
  destination from four register fragments, the ordinary commercial suffix, and five cabinet returns.
- `/community/remote-room.php` is a permanent fail-closed archive tombstone. The website never renders
  a Discord invite. Minecraft LS06 owns the private coordination handoff; Discord LS05 follows only
  after that physical filing succeeds.
- `/community/archive.php?service=1842&ticket=9137&locker=13` is the exact A06 route. It remains an
  ordinary missing archive until A05 is complete, then atomically receipts A06/A07 and reveals clip 1.
- `/record/the-record-keeps` is mkept's ordinary static mirror and download index; it stays missing
  until LS03 has been durably recorded, so it cannot disclose the service field out of order.
- `/record/the-record` shows only coarse V5 case/node progress.
- `/record/archive` shows only already-earned evidence and prerequisite-delivered media.
- `/record/terminal` is a read-only case docket. Its retired inscription POST returns `410`; V5 has no
  name-only web identity, so conclusions stay on their dedicated Copperline, linked Discord, or
  in-world surfaces.
- `/status` remains a spoiler-free infrastructure view.

All Record routes are `noindex`. `/status` describes retired accounts generically and does not leak
the LS02 field. Public database reads use narrow SECURITY DEFINER views. Accepted
answers, future node titles, prerequisites, expected media payloads, checksums, player identities, and
the service-role key never reach public projections.

## Director console

`/author` requires Supabase authentication and exact membership in `ADMIN_EMAILS`. It reports:

- all 10 mandatory cases and 82 required nodes;
- durable evidence receipts, input surfaces, rewards, and recovery paths;
- required-media configuration and prerequisite health;
- persistent showrunner state and guarded beat approval;
- player hint budgets and private telemetry;
- durable Wren, name-treatment, closure-branch, finale-phase, and Coda state.

The removed six-plus-one/optional-side/Accepting controls are not compatibility aliases. Minecraft's
Release Protocol owns branch selection, save, goodbye, kick, shutdown, and idempotent Coda Mode.

## Environment

```dotenv
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<anon key>
SUPABASE_SERVICE_ROLE_KEY=<service-role secret>
ADMIN_EMAILS=<comma-separated operator addresses>
AUTHOR_USERNAME=<private Basic-auth username>
AUTHOR_PASSWORD=<unique high-entropy Basic-auth password>
```

## Database

Do not apply dashboard migrations alone. From `../discord`:

```powershell
npm.cmd run db:seed
npm.cmd run db:bundlecheck
```

Apply `discord/supabase/apply-all.sql` as the migration/service role. It contains the base schema,
Discord schema, V5 investigations, public views, all legacy history-preserving seeds, the final V5
retirement/seed, and schema reconciliation in enforced order.

## Build and verification

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run selftest
npm.cmd run build
```

The selftest runner uses `tsx` from `../discord/node_modules`; run `npm.cmd ci` in `../discord` once
before the dashboard checks on a fresh checkout.

The V5 playable world, builder, and SHA-1 receipt live in the server-only
`content/the-hold-v5/` directory. No copy may exist below `public/`: the stable player URL
`/the-hold/the-hold.zip` is a dynamic, non-cacheable Route Handler that returns the same generic 404
when LS03 is incomplete, story state is unavailable, or archive integrity fails. Once LS03 is
recorded, the handler verifies `the-hold.sha1`, records LS04 through the prerequisite-enforcing RPC,
and returns the ZIP. Rebuild `the-hold.zip` and `the-hold.sha1` together with
`../tools/build_hold_prologue.py`, then run the dashboard self-test and Hold invitation check.
