-- The Observance — 0008_requires_quorum.sql
-- THE ROSTER-QUORUM COLUMN (S-F roster guard). Adds the one nullable column the showrunner's
-- convergence guard reads: `puzzles.requires_quorum`.
--
-- Why (design/OVERHAUL.md dynamic-roster invariant + decide.ts rosterCanClose): a handful of beats
-- are genuine CONVERGENCES — they cannot be closed by a lone player because the in-world detection is
-- a group act (everyone present bows as one; three hands on three surfaces at once; the group walks
-- the map; an asymmetric co-op vault). The showrunner must NOT drip such a thread to a roster too
-- small to close it — surfacing a convergence the present group cannot possibly complete is the
-- dead-air failure the reshape fixes. `decide.rosterCanClose(p, s)` already implements the guard
-- (excludes a row when requires_quorum > activeRosterSize); it was inert because no column fed it.
--
-- THE CONTRACT (decide.ts / snapshot.ts / types.ts):
--   requires_quorum NULL  ⟺  no quorum gate — the row is always eligible for the drip (back-compat:
--                            every existing row keeps NULL, so its drip eligibility is unchanged).
--   requires_quorum = N   ⟺  the node opens to the drip only once >= N active players are present
--                            (the SAME active-set the plugin's AcceptingRiteListener uses for
--                            effectiveQuorum = min(configQuorum, activeRosterSize)). snapshot.ts
--                            selects this column onto SnapshotPuzzle.requiresQuorum; the guard is a
--                            no-op for any row that leaves it NULL.
--
-- Security model (identical to 0004/0005/0006): RLS already enabled on puzzles; service_role
-- bypasses; no anon/authenticated policies. Additive + idempotent (add column if not exists) — safe
-- to re-run and changes the behavior of NO existing row until a seed sets a non-NULL value.

begin;

-- ===========================================================================
-- 1. puzzles.requires_quorum — the convergence gate for the showrunner drip.
--    Nullable integer, default NULL = no quorum gate. Adding the column changes the
--    behavior of NO existing row (the guard no-ops on NULL); a later seed sets it on
--    the genuine convergence rows (metapuzzle_seed.sql §5).
-- ===========================================================================

alter table public.puzzles
  add column if not exists requires_quorum integer;

comment on column public.puzzles.requires_quorum is
  'Convergence quorum for the showrunner drip (decide.rosterCanClose): the minimum number of ACTIVE '
  'players that must be present before this node is surfaced in the clue-drip. NULL = no quorum gate '
  '(the default; the guard no-ops). Read by snapshot.ts onto SnapshotPuzzle.requiresQuorum and '
  'compared against Snapshot.activeRosterSize (readActiveRoster().length). Does NOT gate resolution '
  '(that is requires_flags / the in-world detection) — only the curatorial drip.';

commit;
