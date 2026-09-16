# System Architecture

## Runtime ownership

```text
Minecraft client
    |
Paper 1.21.11 + Observance plugin
    |-- authoritative Mossfield world state
    |-- native clue objects and bounded scares
    |-- local hash-chained event journal
    |-- final local shutdown authority
    |
    | signed, idempotent HTTPS events (async)
    v
Next.js route handlers
    |
    v
Supabase Postgres
    |-- canonical cross-surface event ledger
    |-- discovery, gate, delivery, and director receipts
    |-- Morrow-state projection and outbox leases
    |
    +--> Copperline hosting/community/forum/blog/status/archive/login
    +--> Railway Discord worker
    +--> Paper projection poller (async -> main-thread enactment)
    +--> authenticated director dashboard
```

## Hosting

- **Minecraft:** the existing Crafty-managed Paper server. Paper and its local journal remain the sole
  authority for physical world state and the final stop.
- **Copperline:** one Next.js App Router deployment on Vercel. Public hosting, community, forum, blog,
  status, archive, login, and authenticated recovery-console routes share one fictional domain.
- **Database/auth/storage:** Supabase Postgres, Auth, Realtime, and first-party media storage after a
  fresh isolated rehearsal and explicit production authorization.
- **Discord:** one always-on Railway worker for linked-player DMs, interactions, group receipts, and
  projections. It requires no Administrator permission.
- **Director:** an authenticated, role-scoped dashboard in the server-side Copperline deployment. It is
  disabled by default and cannot share player routes or browser bundles.
- **Local development:** disposable Paper clone, local journal, local Next.js, and an isolated database.
  Production secrets never enter fixtures, browser payloads, or retained receipts.

## Event path

1. A platform adapter validates a concrete clue, gate, or director action locally.
2. The owning runtime writes an immutable receipt before producing a consequence.
3. It emits a namespaced event with release, campaign, scope, idempotency key, and payload hash.
4. The ingest boundary authenticates the source and rejects replay collisions.
5. A transaction records the event and creates one projection for each required destination.
6. Each surface leases its rows, applies only authored allowlisted consequences, and acknowledges the
   exact event and release.
7. Identical replay returns the prior receipt; altered replay fails closed.

Gate events correspond to G01–G15 in the puzzle ledger. Optional discoveries use the generic discovery
event with an allowlisted D01–D24 identifier. Morrow state is a projection of immutable gate history;
dialogue, elapsed time, and operator preference cannot advance it.

## Director dashboard

The dashboard is an audited show-control and recovery surface. Every action requires an authenticated
director role, selected non-production release and campaign, exact scope, reason, idempotency key, and
before/after receipt.

Supported commands:

- trigger a named pre-authored scare;
- unlock or restore an authored Copperline page;
- send state-valid authored Morrow dialogue or player-specific Discord fragments;
- spawn, relocate, or despawn the allowlisted horror entity;
- alter allowlisted signs, books, containers, and renamed items;
- verify and mark a gate solved after its evidence predicate passes;
- issue a tiered hint referring to already discovered evidence;
- replay projections, release temporary locks, restore props, and return players to a safe checkpoint;
- arm and start G15 after all prerequisites and safe-state checks pass.

The dashboard cannot execute arbitrary server or SQL commands, invent dialogue, edit evidence, fabricate
player actions, inspect private non-ARG data, bypass a predicate, or enable production. Finale arm/start
uses two distinct actions, short expiry, a rollback snapshot, and a second confirmation.

## Minecraft thread model

- Bukkit/Paper world and entity access occurs only on the owning server thread or scheduler.
- HTTP, Supabase, media hashing, and journal compaction run asynchronously.
- Async callbacks enqueue immutable enactment plans; the main thread revalidates world preconditions.
- Scares and the finale are restartable state machines, never sleeping tasks.
- Every entity, temporary lock, changed prop, and visual effect has an owner, maximum lifetime, rollback
  record, and cleanup path.
- Required clue objects are protected from director and Morrow mutation unless an equivalent copy and
  deterministic restore path exist.

## Discord boundary

- The discord.js Gateway worker owns commands, buttons, select menus, and linked-player DMs.
- Private Morrow fragments are authored, release-bound, expiring, and delivered only to the linked
  campaign identity.
- Group resolution submits short codes or selections, not private prose.
- Database RPCs validate identity, prerequisite, purpose, nonce, expiry, and exact authored option.
- Discord stores and projects no real-world surveillance claim or unrelated personal information.
- After G15, new Morrow-authored deliveries are rejected; operational cleanup receipts remain allowed.

## Cross-surface consistency

- `release_id` appears in every event, projection, media manifest, plugin receipt, web deployment, and
  director action.
- Minecraft is local-primary and can preserve safe progress during remote outage.
- Copperline and Discord display diegetic maintenance state when projections lag.
- Reconciliation compares IDs and hashes, never timestamps alone.
- Director recovery replays or restores recorded state; it does not rewrite the event ledger.
- G15 is terminal. Once the local shutdown event commits, projections may settle the silent state but no
  new Morrow action may originate.

## Next.js boundaries

- Server Components read through a server-only Supabase client.
- Server Actions own authenticated Copperline inputs and validate campaign/player scope.
- Route Handlers own Minecraft ingest, health, media delivery, and any explicitly selected callback.
- Client Components receive only serialized projections and private scoped updates.
- Service-role keys and director privileges never receive `NEXT_PUBLIC_` names or reach the browser.
- Public source comments and archived assets are authored artifacts, not leaked implementation secrets.

## Reimplementation increments

1. Freeze the former M01–M12 progression and retain its receipts as historical evidence.
2. Implement the G01–G04 Copperline discovery and safe Mossfield entry slice.
3. Build the coherent haunted Mossfield world and native G05–G07 clue objects.
4. Add media custody, frame/metadata analysis, and safe G08–G10 Discord fragments.
5. Add G11–G13 chronology, continued-session figure, and Morrow escalation.
6. Build the audited director dashboard and rehearse every scare/recovery action.
7. Implement G14/G15 arm, rollback, terminal stop, and cross-surface silence.
8. Complete accessibility, one-/two-/six-player, restart, outage, security, and human rehearsal.
9. Only then request separate production authorization.
