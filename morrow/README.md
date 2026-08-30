# Morrow Reboot Authority

This directory is the clean-room authority for the Copperline/Morrow reboot. The legacy Hold,
Keeper, Averyn, Wren, and Unlit campaign remains preserved elsewhere in the repository but is not
canon for this branch unless an item is explicitly adopted here.

The reboot keeps the proven infrastructure:

- Paper 1.21.11 plugin and local-first event journal
- Minecraft datapack/resource-pack pipeline
- Next.js 16 Copperline site on Vercel
- Discord worker/showrunner on Railway
- Supabase Postgres/Auth/Realtime/Storage
- deterministic packaging, rehearsal, and audit tooling

The reboot replaces the player story, progression, world, dialogue, puzzles, media, and surface
bindings.

## Authority order

1. `authority/CANON.md` — factual truth, characters, Morrow rules, and allowed ambiguity
2. `authority/PLAYER-JOURNEY.md` — act order, player beliefs, discoveries, and consequences
3. `authority/PUZZLE-LEDGER.json` — every required investigation, input, proof, callback, and payoff
4. `contracts/relationship-states.json` — Morrow's relationship/capability state machine
5. `contracts/event-catalog.json` — append-only cross-surface event vocabulary
6. `contracts/surface-contracts.json` — ownership and input/output rules per platform
7. `architecture/SYSTEM.md` — service topology and runtime ownership
8. `architecture/DATABASE.md` — database and security model
9. `architecture/MINECRAFT-UX.md` — exact in-game presentation and interaction rules
10. `architecture/CAPABILITY-MATRIX.md` — researched feasibility decisions and rejected approaches

If two files disagree, the earlier item wins. A change to an earlier authority must update every
dependent contract in the same commit.

## Current build target

The first production target is a 60–90 minute vertical slice:

`Copperline discovery -> Recovery Room 04 -> static restore -> impossible inferred block ->
37-second replay -> deliberate movement test -> synchronized ticket -> embodied Morrow -> echo payoff`

It must be playable by one to six players, preserve progress across restart, tolerate temporary
Supabase/website/Discord outage, and never require an operator to explain around a broken affordance.

## Non-negotiable design rules

- Minecraft is where theories are tested and consequences become physical.
- Copperline supplies custody, diffs, tickets, and asynchronous investigation—not plot dumps.
- Discord supplies person-to-person contact, private contradiction, and group receipts—not a quest log.
- Media exists because a server event produced or preserved it.
- Every required clue has an accessible equivalent; no required answer is sound-only or color-only.
- Inputs are concrete and platform-native: place, compare, select, route, authenticate, or enter a
  short token. Never require guessing an author's preferred sentence.
- Difficulty comes from correlation, experimentation, provenance, ciphers, and deduction.
- Morrow may vary authored dialogue but cannot invent canon, decide progression, or execute world
  actions through an LLM.
- Only behavior knowingly performed inside the ARG may be reflected back to players.
- Critical progress is local-first, append-only, idempotent, restart-safe, and auditable.

