# Morrow Reboot Authority

This directory is the clean-room authority for the Copperline/Morrow reboot. The legacy Hold,
Keeper, Averyn, Wren, and Unlit campaign remains preserved elsewhere in the repository but is not
canon for this branch unless an item is explicitly adopted here.

The reboot keeps the proven infrastructure:

- Paper 1.21.11 plugin and local-first event journal;
- Minecraft datapack/resource-pack pipeline;
- Next.js Copperline site on Vercel;
- Discord worker/showrunner on Railway;
- Supabase Postgres/Auth/Realtime/Storage;
- deterministic packaging, rehearsal, and audit tooling.

The September 2026 creative pivot replaces the prior M01–M12 investigation-room campaign. Existing
runtime code and rehearsal receipts are retained as historical engineering evidence, not proof of the
new experience.

## Authority order

1. `authority/ARG-GUIDEBOOK.md` — creative promise, campaign shape, puzzle language, safety, and ending
2. `authority/CANON.md` — factual truth, characters, Morrow rules, and allowed ambiguity
3. `authority/PLAYER-JOURNEY.md` — act order, player beliefs, discoveries, and consequences
4. `authority/PUZZLE-LEDGER.json` — all 24 discoveries and 15 required gates
5. `contracts/relationship-states.json` — Morrow's observable escalation and terminal silence
6. `contracts/event-catalog.json` — append-only cross-surface event vocabulary
7. `contracts/surface-contracts.json` — ownership and input/output rules per platform
8. `contracts/media-catalog.json` — required found-media roles and accessibility obligations
9. `architecture/SYSTEM.md` — service topology and runtime ownership
10. `architecture/DATABASE.md` — database and security model
11. `architecture/MINECRAFT-UX.md` — exact in-game presentation and interaction rules
12. `architecture/CAPABILITY-MATRIX.md` — researched feasibility decisions and rejected approaches

`authority/IMPLEMENTATION-AUDIT.md` reports implementation and proof against the authority above; it
does not override canon or contracts. Deployment and incident procedure lives in
`architecture/OPERATIONS.md` and likewise cannot override creative authority.

If two files disagree, the earlier item wins. A change to an earlier authority must update every
dependent contract in the same change.

## Current design

Morrow is a traditional layered Minecraft ARG and found-footage mystery. Players stumble into an old
Copperline hosting site, recover Mossfield, explore a haunted survival world, correlate records across
Minecraft, the Copperline archive, Discord, and retained media, and gradually realize the recovery
software is observing and using them.

The campaign contains 24 meaningful discoveries, 15 required gates, and one main ending: the players
shut Morrow down. The final ambiguity is whether they stopped a dangerous system or erased the last
witness to Mossfield.

Morrow is the phenomenon, not the puzzle host. Minecraft is a coherent ruined survival server built
from books, lecterns, chests, maps, signs, renamed items, redstone, ruins, authored NPC dialogue, and
bounded scares—not a sequence of provenance-comparison chambers.

## Implementation status

Creative authority has changed and production rehearsal is paused. The former M01–M12 Paper rooms,
four-ending branch governance, and associated website/Discord projections do not implement this
authority. They may be mined for safe infrastructure patterns only after an explicit remapping.

Production remains disabled. No deployment, database migration, Discord command, external
authentication, or production-world mutation is authorized by this redesign.

## Non-negotiable design rules

- Morrow begins as plausible recovery support software and escalates indirectly.
- Required conclusions come from correlating authored records, not trusting character testimony.
- Clues may be discovered out of order; required gates remain idempotent and restart-safe.
- Optional lore deepens interpretation but never blocks completion.
- Copperline is one domain containing hosting, community, forum, blog, status, archive, and login.
- Discord may divide players with authored fragments, but never implies real-world surveillance.
- Every required audio or color clue has a readable equivalent.
- Real-internet research uses stable public facts and never involves uninvolved people.
- Operator-triggered scares are allowlisted, reversible, receipt-bound, and cannot destroy evidence.
- The director dashboard cannot invent dialogue, fabricate actions, or silently solve a gate.
- Morrow cannot invent canon or progression through an LLM.
- Only behavior knowingly performed inside the ARG may be reflected back to players.
- Critical progress is local-first, append-only, idempotent, restart-safe, and auditable.
- After the canonical shutdown, Morrow is silent.
