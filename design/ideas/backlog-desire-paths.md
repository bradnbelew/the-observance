# Backlog idea — desire paths / "the world reacts to where you walk"

Source: Ethan side-note 2026-06-29 (inspired by the "Roads More Traveled" mod). Captured for
the Pillar-3 integration + Pillar-5 Observer-Engine phases (OVERHAUL.md). NOT a commitment;
an idea on the shelf.

**The concept:** the world quietly changes along the routes players travel most — a path wears
in, a lectern/marker waits where they always pass, a rune appears only on a favored corridor.

**Don't use the mod.** Path A = no client installs; the server is Paper (won't load a
Fabric/Forge mod). Not needed anyway.

**Build it natively — the infra already exists:**
- `HeatmapAccumulator` + `heatmap_cells` (Supabase) + `LocationSampler` already record where
  players go. That IS the desire-path signal.
- A new beat (e.g. `desire_path` / `trodden_marker`) reads the hot cells and places a
  static-then-reactive feature: a worn dirt/path block trail, a lectern or grave or rune on
  the most-trodden route, etc. Per-player or group, gated like any other beat.

**Why it fits the chosen direction:**
- Pillar 5 (Observer Engine): "it knows where you go" — spatial proof the land watches, with
  zero extra surveillance plumbing (movement is already tracked, grounded, real).
- Pillar 3 (integration): a cheap, vanilla-first reactive-world beat; can be per-player
  (a path only you see worn in) or shared (the group's collective route marked).
- Honors the **dynamic-roster** invariant trivially (reads whoever's been active).

**Strong uses:** the future-dated grave appears on the route a player walks most (it knew
where to wait); a keeper-stone/lectern surfaces on the group's busiest corridor so the next
thread is found where they already are (a salience trailhead made spatial); the Watcher's
"path" subtly steering them toward the Deep Line.
