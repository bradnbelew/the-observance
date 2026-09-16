# Morrow First-Touch Test Guide

This guide tests what is built so far. It does not claim the full ARG is launch-ready.

Production boundary: no step should start a Minecraft server, contact Discord, write Supabase state, enable director controls, or mutate production.

## Website Trail

1. Open `/game-servers.php`.
   - Expected: Copperline reads as an ordinary 2011 hosting listing.
2. Open `/community/forum?thread=6118`.
   - Expected: the forum residue provides alias/support continuity.
3. Open `/support/tickets/6118`.
   - Expected: the support ticket points toward the same recovery case.
4. Open `/status/incident-6118`.
   - Expected: the stable uptime value survives visible public edits.
5. Open `/recovery/mossfield?alias=iona&key=041722`.
   - Expected: the static Mossfield handoff opens and states production is disabled.

## Built Stop Point

6. Open `/recovery/mossfield/console`.
   - Expected: all G01-G15 gates are modeled as local contract data.
7. Open `/recovery/mossfield/media`.
   - Expected: all 12 media artifacts remain release-blocking until source files, final files, SHA-256 hashes, custody notes, accessibility equivalents, and safety review exist.
8. Open `/api/rehearsal/morrow/readiness`, `/api/rehearsal/morrow/media`, `/api/rehearsal/morrow/full`, and `/api/rehearsal/morrow/director`.
   - Expected: production remains disabled, `productionMutation` remains false, and the director surface remains locked.

## Hard Stop

Stop the test at the local contract surfaces. Minecraft runtime, Discord G10 delivery, Supabase migration, final media files, and director production controls still require fresh proof before launch.
