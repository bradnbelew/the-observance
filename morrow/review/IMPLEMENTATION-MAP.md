# Morrow Implementation Map

Status: local implementation planning plus first contract slice
Audited from authority on 2026-09-15
Production: disabled

## Read-only gap audit

The G01-G15 authority is creatively complete, but the reboot is still mostly unimplemented as runtime. The review page and fixtures prove design coherence only. They do not prove player-facing Copperline routes, Paper world content, Discord delivery, media custody, database projections, director controls, or human playability.

Current implementation gaps:

- G01-G03 Copperline entry: designed, but no release-bound public routes, fictional login predicate, rate-limit receipt, or authenticated handoff exists for the new spine.
- G04-G07 Mossfield entry and Act 1: historical Paper rooms prove useful infrastructure patterns only; no coherent Mossfield world, storehouse, book-coordinate cache, or lighthouse mismatch exists as the new campaign.
- G08-G10 media and Discord: anchor artifacts are concept-only and hashless; Discord private-fragment infrastructure must be remapped before G10 can be claimed current.
- G11-G13 continuity: the corrected G11/G13 designs are authority only; no status rewrite, immutable uptime export, Iona memo, or continued-session encounter is implemented.
- G14-G15 shutdown: no new manual stop route, arm receipt, terminal shutdown, or cross-surface silence exists.
- Discoveries and dread: D01-D24 and the twelve dread beats are ledgered; this first local slice only represents D01-D05 and binds `chest_correction` as one delayed, non-blocking consequence after G05.
- Media and accessibility: all twelve media anchors remain `to_be_authored`; accessibility equivalents are required by authority but mostly unproven in runtime.
- Idempotency and restart safety: historical M01-M12 receipts prove patterns only; the new slice adds a deterministic hash-chained receipt target for G01-G05.
- Anti-puzzle-room firewall: the reboot authority complies, but legacy room mechanics remain unsafe to reuse until gate-by-gate remapping is complete.

## Reuse map

Safe to reuse after remapping: local hash-chained journals, idempotency keys, replay collision behavior, Next.js route patterns, Paper main-thread scheduling/PDC/restart cleanup, Discord identity and authored-message delivery patterns, and Supabase event/outbox/RLS patterns after a fresh G01-G15 schema proposal.

Must be replaced or freshly authored: old M01-M12 rooms, evidence-comparison grammar, branch-governance endings, old Copperline case projections as player-facing proof, old Discord contradiction flow until remapped to G10, all final media files and hashes, production operations, migrations, director dashboard, and rehearsal contracts.

## Smallest complete local vertical slice

Implemented slice: `morrow.local.g01-g05.v1`.

It proves the smallest honest grammar: ordinary Copperline trace to Mossfield handoff, a meaningful storehouse discovery, and a delayed bounded consequence. G01-G03 are local receipt predicates for source-comment discovery, forum/support identity archaeology, and a status-log-derived recovery login. G04 records non-op Mossfield entry and advances Morrow from `support_software` to `observant`. G05 records cairn's storehouse habit as a native-world discovery. After G05, `chest_correction` is scheduled only after players leave the storehouse; it is not required for progress, has a one-second maximum, has a text equivalent, and cannot consume evidence.

This is local-only and not playable. It does not contact Minecraft, Supabase, Discord, Vercel, Railway, or production.

## Files

- `tools/run_morrow_g01_g05_vertical_slice.py` builds the deterministic receipt.
- `tools/check_morrow_g01_g05_vertical_slice.py` validates the receipt against authority, fixtures, idempotency, restart safety, and the production boundary.
- `morrow/rehearsal/g01-g05-vertical-slice/latest.json` is the retained local receipt.

## Next safest slice

Build actual local Copperline G01-G03 routes under one ordinary domain, with source comments, forum archive residue, status table text equivalents, fictional login failure behavior, and a generated local handoff receipt. Keep it static/local-first until the predicates match this contract and the review page remains production-locked.
