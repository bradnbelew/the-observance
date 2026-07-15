# The Observance V5 — technical design

This file is safe for implementers. Spoiler truth is isolated in `arc/WORLD-BIBLE.md`.

## Design thesis

The player experience is deterministic where correctness matters and reactive where personalization helps. No model generates world geometry, coordinates, answers, progression, or critical dialogue. Every external dependency has a local fail-safe.

## Canonical data flow

```text
WORLD-BIBLE + 82-node CSV + canonical content JSON
  -> validated generated SQL/resources/projections
  -> Paper plugin / Discord / dashboard
  -> parity hashes and live readback audits
```

Hand-edited duplicate story strings are defects. Old content is transactionally retired rather than left active beside V5.

## Minecraft runtime

### Build system

The Deep Hold uses 32 non-overlapping owned rooms across Y −40, −68, and −96 relative to one Surface Mouth. The build compiles exact mutations/specs, validates bounds/cover/collisions/reachability/content/facing, persists a pending receipt, applies work with a tick budget, reads state back, and commits readiness only after reload verification.

Supported production orientation is explicit +Z until four-way rotation has equivalent tests. Player yaw is not silently treated as authority.

### Interactions

- PDC-tagged items and exact protected inventory slots.
- Stable item-frame and chiseled-bookshelf states.
- Filled locked lecterns with exact hashes.
- Bounded route/crouch/group operations.
- Editable answer signs routed through the same normalizer as Discord.
- Monotonic persistent gates.
- Synchronous NPC dialogue from canonical content.

Every mechanism is idempotent and recoverable. Rebuild and repair are separate commands; repair preserves solved state.

### Protection

The Hold/Unlit reject ordinary breaking/placing, explosions, fire, fluid changes, piston movement, hopper/dropper extraction, frame/lectern/entity damage, equipment changes, vehicle/hanging clutter, and teleport bypass. An intended puzzle listener exempts only exact cells/actions.

### Finale

An explicit durable state machine owns arm, validation, commit, save, darkening, goodbye, kick, shutdown, and Coda boot. Duplicate input and restart are safe. External mirroring never blocks local presentation.

## Supabase

V5 uses explicit investigation/node/evidence state plus a coarse act and `phase_key`. Migrations preserve player links, consent, settings, and event history while deactivating stale puzzles/hints/cards/optional side rows. The generated apply bundle is authoritative; it is never hand-edited.

Public website data comes from anon-safe projections/security-definer functions. Raw accepted answers and private player state are not exposed.

## Discord and showrunner

The persistent bot worker owns slash commands, normalization, linking, hints, receipts, and a lease-safe showrunner tick every 10–15 seconds. The ten-minute Railway cron is recovery only.

Answer resolution is fail-closed on prerequisites and selection. Autocomplete cannot spoil closed node titles/keys. Repeated delivery is idempotent.

## Website

The Copperline shell remains intentionally mundane. Required archive/media routes reveal automatically from durable prerequisites. Public Record projections describe cases/evidence and ending state, not a hard-coded `kept: 6` counter. Author pages operate the V5 case/finale model and are protected by production auth.

## Media

Five immutable external assets are listed in `arc/v5/media-manifest.json` with URL, payload, byte/hash receipt, prerequisite, and narrative use. Health checks verify reachability/extractability; readiness is not a story switch.

## Content validation

`tools/check_v5_freshness.py` prevents archived story/setup material or retired packet generators from becoming operational again.

`tools/check_v5_content.py` enforces:

- 82 nodes and exact case counts;
- unique IDs/flags and acyclic prerequisites;
- 32/32 rooms and 76/76 fixture ownership;
- 44 exact non-empty book sources and text limits;
- five required media assets/payloads/hashes;
- five townsfolk plus Wren branch dialogue;
- forbidden V4 claims on runtime surfaces.

`tools/check_v5_physical_predicates.py` enforces exact success, wrong-input, reward, reset, concurrency, and durability contracts for all 60 Minecraft-owned nodes. Plugin tests additionally verify exact live block/entity/inventory/PDC/content state and route/gate behavior.

## Failure policy

- Unknown prerequisite: do not open/resolve.
- Missing canonical content: fail before world mutation/build.
- Database outage: preserve last locally latched gates and queue bounded receipts.
- Discord/Railway outage: Minecraft interactions/NPC/finale continue locally.
- Website/media unavailable before reveal: launch preflight fails.
- Lost artifact: recover from durable completion; never reset the case.
- Partial Hold build: never report ready; use fresh site or verified rollback.
- Restart during finale: resume idempotently into the only safe next phase/Coda.

## Release verification

Clean builds and unit/self-tests are necessary but insufficient. Production requires exact Paper `1.21.11`, real-client traversal/typography/hitboxes, non-op grief and recovery scenarios, live Supabase/Discord/Railway/Vercel/DNS checks, all finale branches, artifact hashes, and a dated launch receipt.
