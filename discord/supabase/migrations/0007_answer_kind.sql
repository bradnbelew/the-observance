-- The Observance — 0007_answer_kind.sql
-- Adds `puzzles.answer_kind` — the coarse resolution modality of a puzzle's answer, so the
-- seed can DECLARE (and the tooling can reason about) which rows are typed-phrase solves vs
-- which are produced by an in-world PLUGIN listener that sets the puzzle's flag directly.
--
-- Why this column exists (design/PUZZLE-DESIGNS.md §1 / PUZZLES.md §4): the diverse-expansion
-- puzzles span many modalities. Some resolve by a TYPED phrase the resolver matches against
-- accepted_answers (answer_kind='phrase' — today's only resolver path, and the DEFAULT). Others
-- resolve by a body action, a deposited object, a spoken line, or an on-site bearing — those are
-- detected by a plugin PRODUCER listener that posts an opaque sentinel token OR sets the puzzle's
-- flag directly. The resolver needs NO new branch for the non-typed kinds: it keeps matching
-- normalized phrases / opaque tokens exactly as it does today. answer_kind is DECLARATIVE metadata
-- that records the intent and lets a later plugin round know which rows need a producer.
--
--   answer_kind ∈ { phrase | coords | url_token | behavior | object | spoken | code | none }
--     phrase     — typed free text matched against accepted_answers (DEFAULT; the resolver path)
--     coords     — an on-site destination WORD found by following a bearing (typed; INV-14)
--     url_token  — a decoded record-website path token (typed)
--     code       — a lock/dial/comparator combination the plugin reads (plugin-produced)
--     behavior   — a body action (bow / stand-at-anchor / silence) the plugin detects
--     object     — a container-content / deposit the plugin detects
--     spoken     — a voice-chat line the Observer Engine hears
--     none       — a comprehension beat with nothing to submit (the world reacts)
--
-- ADDITIVE + IDEMPOTENT: `add column if not exists … default 'phrase'` — every existing row is
-- unaffected (they are all typed-phrase solves, the historical implicit default). Safe to re-run.
--
-- Security model (identical to 0004/0005/0006): RLS already enabled on puzzles; service_role
-- bypasses; no anon/authenticated policies.

begin;

alter table public.puzzles
  add column if not exists answer_kind text not null default 'phrase';

comment on column public.puzzles.answer_kind is
  'The answer modality (design/PUZZLE-DESIGNS.md §1): phrase|coords|url_token|code|behavior|'
  'object|spoken|none. DECLARATIVE metadata. phrase/coords/url_token are typed and matched by '
  'the resolver against accepted_answers; code/behavior/object/spoken are produced by a plugin '
  'listener that posts the sentinel token or sets the flag directly (no new resolver branch); '
  'none is a comprehension beat. Default ''phrase'' = the historical typed-solve path.';

commit;
