# System Architecture

## Runtime ownership

```text
Minecraft client
    |
Paper 1.21.11 + Observance plugin
    |-- authoritative world state
    |-- local hash-chained event journal
    |-- dialogue, displays, structures, echoes
    |
    | signed, idempotent HTTPS events (async)
    v
Next.js Route Handler: /api/runtime/minecraft/events
    |
    v
Supabase Postgres
    |-- canonical cross-surface event ledger
    |-- evidence and choice receipts
    |-- capability and relationship projections
    |-- projection outbox and leases
    |-- private Realtime Broadcast
    |
    +--> Next.js Copperline (Server Components + Server Actions)
    +--> Railway Discord worker/projector
    +--> Paper projection poller (async -> main-thread enactment)
```

## Hosting

- **Minecraft:** existing Crafty-managed Paper server. The plugin and world remain the sole authority
  for physical state. No public RCON dependency.
- **Copperline:** existing Next.js 16 App Router project on Vercel. Use the Node runtime by default.
- **Database/auth/storage:** existing Supabase project, after a fresh backup and safe-target migration
  rehearsal. Required public media is stored first-party with fixed hashes.
- **Discord:** existing always-on Railway worker for Gateway events and projection handling. Keep the
  recovery/cron process separate and idempotent.
- **Local development:** disposable Paper clone, local plugin journal, local Next.js, and a local or
  isolated Supabase target. Production secrets never enter fixtures or test receipts.

## Event path

1. A platform adapter validates a concrete action locally.
2. The owning runtime writes an immutable local receipt before producing consequences.
3. It emits a namespaced event with a deterministic idempotency key and payload hash.
4. The ingest boundary authenticates the source, rejects replay/collision, and records one event.
5. A database transaction creates one projection row for every required destination surface.
6. Each surface leases its own rows, applies the authored consequence, and acknowledges the exact
   event and release version.
7. Replays are harmless: an identical idempotency key returns the prior receipt; a mismatched payload
   is a collision and fails closed.

## Discord contradiction boundary

- The existing discord.js Gateway worker owns `/morrow`, buttons, and select menus. No second adapter,
  HTTP interaction endpoint, or mandatory live-chat loop is introduced.
- The earned `morrow.act2.behavior_reuse_proven` projection activates a release- and scope-bound group
  flow. Each linked player receives only their own ephemeral authored evidence and an expiring nonce.
- A service-role RPC validates identity, prerequisite, scope, purpose, nonce, expiry, and exact authored
  selection under durable locks. Freeform language cannot decide progress.
- After every one-to-six expected participant has independently filed the inferred-source choice, the
  database commits one `morrow.act2.private_contradiction_resolved` event with no private payload. The
  outbox posts a spoiler-safe group receipt and projects the callback to Minecraft.
- Decline, cancel, timeout, reconnect, outage, and worker restart preserve recovery without fabricating
  progress. The later Paper `live_capture_authorized` dialog remains a separate explicit decision.

## Minecraft thread model

- Bukkit/Paper world and entity access occurs only on the owning server thread/scheduler.
- HTTP, Supabase, file compaction, and SQLite work run asynchronously.
- Async callbacks enqueue immutable enactment plans; the main thread revalidates world preconditions
  before applying them.
- Every long scene is represented by a restartable state machine, not one sleeping task.
- Every spawned display/interaction entity has a scene owner, cleanup path, persistence rule, and
  maximum lifetime.

## Cross-surface consistency

- `release_id` is included in every event, projection, media manifest, plugin JAR receipt, and web
  deployment receipt.
- Minecraft uses local-primary progress and can continue through remote outage.
- Copperline and Discord show a deliberate diegetic maintenance state when projections are behind;
  they never invent progress from client state.
- Reconciliation compares event IDs and hashes, never timestamps alone.
- Director repair replays projections; it does not manually set story flags unless an audited recovery
  procedure explicitly owns that action.

## Next.js boundaries

- Server Components perform internal reads directly through a server-only Supabase client.
- Server Actions own Copperline form mutations and validate the current authenticated campaign/player.
- Route Handlers own Minecraft event ingest, Discord callbacks if used, health, and media delivery.
- Client Components receive plain serialized projections and subscribe only to private, scoped updates.
- The service-role key never has a `NEXT_PUBLIC_` name and never reaches the browser.

## Production increments

1. Pure relationship/capability domain and contract validators.
2. Recovery Room 04 structure and static Morrow display body.
3. Native Paper dialogue adapter and local restart-safe state.
4. Static restoration proposal and physical diff investigation.
5. Bounded movement recorder/replayer.
6. Signed event ingest and Copperline ticket projection.
7. End-to-end disposable Paper/website restart test.
8. Only then expand into split backups, account continuity, cold storage, and rollback.
