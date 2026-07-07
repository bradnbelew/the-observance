/**
 * Minimal TS row types for the tables the Discord bot touches.
 *
 * These mirror the canonical schema in
 *   dashboard/supabase/migrations/0001_init.sql
 * plus discord/supabase/migrations/0003_discord.sql.
 * They are intentionally narrow — only columns the bot reads or writes.
 */

/** public.players — adds `discord_id` (0003) to link a Discord user. */
export interface Player {
  id: string; // uuid
  mc_uuid: string;
  name: string;
  /** Linked Discord user id, or null if never linked. */
  discord_id: string | null;
  /** Per-player observer chat/text opt-out. Unknown/missing is treated as opted out at capture edges. */
  observer_opt_out?: boolean | null;
}

/** public.whisper_budgets — per (player, act) spend tracking. PK is `id`. */
export interface WhisperBudget {
  id: number;
  player_id: string; // uuid
  act: number;
  budget: number;
  spent: number;
  earned: number;
}

/** public.whisper_events — append-only log of delivered whispers. */
export interface WhisperEvent {
  id: number;
  player_id: string; // uuid
  puzzle_key: string | null;
  tier: number | null;
  created_at: string; // timestamptz (ISO)
}

/** public.beat_queue — story beats awaiting approval / firing. `failed` is a terminal status the plugin
 *  writes when an enactor throws (widened in the beat_queue status CHECK; see the dashboard migration). */
export type BeatStatus = 'pending' | 'approved' | 'firing' | 'skipped' | 'fired' | 'failed';

export interface BeatQueueRow {
  id: number;
  type: string;
  target: string | null;
  payload: Record<string, unknown>;
  status: BeatStatus;
  created_at: string; // timestamptz (ISO)
  decided_at: string | null;
}

/** public.event_log — structured operational log. */
export type LogLevel = 'info' | 'warn' | 'error';

export interface EventLogRow {
  id: number;
  level: LogLevel;
  source: string | null;
  message: string | null;
  created_at: string; // timestamptz (ISO)
}

/** public.settings — key/jsonb control rows. */
export interface SettingRow {
  key: string;
  value: unknown; // jsonb
  updated_at: string; // timestamptz (ISO)
}

/** public.arc_state — single-row arc cursor (id is always 1). */
export interface ArcState {
  id: 1;
  current_act: number;
  gates: Record<string, unknown>;
  flags: Record<string, unknown>;
  updated_at: string; // timestamptz (ISO)
}

/** public.hints — pre-authored whisper hints, keyed by (puzzle_key, tier). */
export interface Hint {
  id: number;
  puzzle_key: string;
  tier: number;
  body: string;
}

// ---------------------------------------------------------------------------
// Oracle — the non-linear clue web (0004_oracle.sql).
// ---------------------------------------------------------------------------

/** What a correct answer DOES. The web's branching dimension. */
export type OutcomeType =
  | 'next_clue'
  | 'lore'
  | 'dead_end'
  | 'side_quest'
  | 'main_beat';

/**
 * The answer modality (0007_answer_kind.sql; design/PUZZLE-DESIGNS.md §1). `phrase`
 * (the default) / `coords` / `url_token` are TYPED and matched by the resolver against
 * accepted_answers; `code` / `behavior` / `object` / `spoken` are PLUGIN-produced (a
 * listener posts the opaque sentinel token or sets the flag directly — no new resolver
 * branch); `none` is a comprehension beat with nothing to submit.
 */
export type AnswerKind =
  | 'phrase'
  | 'coords'
  | 'url_token'
  | 'code'
  | 'behavior'
  | 'object'
  | 'spoken'
  | 'none';

/**
 * The in-world reward an outcome may enqueue. Mirrors the beat_queue row shape
 * the plugin reads (mc_uuid / site_id / priority / payload). `mc_uuid` may carry
 * the literal "{solver}" placeholder, resolved to the solving player's uuid at
 * enqueue time. All fields optional except those the plugin needs to route.
 */
export interface OutcomeBeat {
  /** beat_queue.type — e.g. 'unlock' (→ UnlockBeat dispatcher). */
  type: string;
  /** target player uuid, or the literal "{solver}" placeholder. */
  mc_uuid?: string | null;
  /** optional sites.yml id (world/ambient beats omit mc_uuid). */
  site_id?: string | null;
  /** optional ordering hint. */
  priority?: number | null;
  /** jsonb payload handed to the beat (e.g. UnlockBeat's { step, step_payload }). */
  payload?: Record<string, unknown>;
}

/**
 * public.puzzles.outcome_payload — the resolution recipe. `voice_key` names the
 * voice.ts line that speaks the reply; everything else is optional wiring. Authors
 * never write English here, only a voice_key + structured args.
 */
export interface OutcomePayload {
  /** which voice.ts oracle line speaks the reply (REQUIRED in practice). */
  voice_key?: string;
  /** args spread into the voice fn. */
  voice_args?: Record<string, unknown>;
  /** for next_clue / side_quest: the puzzle this opens (its clue is surfaced). */
  next_puzzle_key?: string;
  /** optional arc_state.flags to set on solve. */
  set_flags?: Record<string, unknown>;
  /** optional in-world reward to enqueue (status 'approved' — fires immediately). */
  beat?: OutcomeBeat;
}

/** public.puzzles — one authored row per forged clue in the web. */
export interface Puzzle {
  puzzle_key: string;
  title: string;
  /** accepted solutions, EACH already stored in normalized form. */
  accepted_answers: string[];
  outcome_type: OutcomeType;
  outcome_payload: OutcomePayload;
  movement: number;
  active: boolean;
  /** per-puzzle attempt cap, or null for no per-puzzle cap. */
  max_attempts: number | null;
  /**
   * The answer modality (0007_answer_kind.sql; design/PUZZLE-DESIGNS.md §1). One of
   * `phrase | coords | url_token | code | behavior | object | spoken | none`.
   * DECLARATIVE metadata: `phrase`/`coords`/`url_token` are typed and matched by the
   * resolver against {@link accepted_answers}; `code`/`behavior`/`object`/`spoken` are
   * produced by an in-world plugin listener (which posts the opaque sentinel token or sets
   * the flag directly), so the resolver needs NO new branch; `none` is a comprehension
   * beat. Optional here because `getOpenPuzzles` does not select it (the resolver never
   * reads it) — it defaults to `'phrase'` in the DB and is loaded only where explicitly
   * queried. Widened to `string` so a future kind never breaks the type.
   */
  answer_kind?: AnswerKind | (string & {});
  /**
   * The storylet gate (0006_requires_flags.sql; OVERHAUL.md §3). A flat
   * `{ flag_key: true }` object; the row is OPEN iff `active` AND every key here is
   * truthy in `arc_state.flags`. Empty `{}` (the default) = ungated. `getOpenPuzzles`
   * AND-tests it against the live flags before the resolver ever sees the row, so a
   * gated row is invisible until its upstream door has been solved.
   */
  requires_flags: Record<string, unknown>;
}

/** public.solves — the replay guard: one row per (puzzle, player) resolved. */
export interface Solve {
  id: number;
  puzzle_key: string;
  player_id: string; // uuid
  mc_uuid: string | null;
  discord_id: string | null;
  attempt_count: number;
  solved_at: string; // timestamptz (ISO)
}

/** Which entry-point an answer arrived on. 'web' (migration 0010) is the record website's own value —
 *  it used to log under 'discord' as a stopgap (the CHECK constraint's only non-'world' option then). */
export type AnswerSurface = 'discord' | 'world' | 'web';
