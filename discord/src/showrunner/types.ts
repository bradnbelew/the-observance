/**
 * Showrunner types — the deterministic, zero-LLM spine.
 *
 * The showrunner is the between-session cron (Render Cron Job) that keeps the mystery alive: it drips
 * the next clue on a cadence and auto-gifts a hint to a group that is genuinely stuck. This first
 * layer is 100% DETERMINISTIC — no Claude/LLM call anywhere — so it works even if the AI layer is down
 * (the critics' #1 pacing risk). The pure decision function `decide(Snapshot): Decision` (decide.ts)
 * takes an immutable snapshot of the world and returns what to do; snapshot.ts reads it, apply.ts
 * writes it. Keeping decide pure makes the whole spine unit-testable without a DB or network.
 */

/** CONFIRM = curatorial drips wait for a human to approve in the dashboard; AUTO = they fire live. */
export type ShowrunnerMode = 'auto' | 'confirm';

/**
 * A puzzle's outcome semantics, mirrored from `puzzles.outcome_type` (ORACLE.md §3). The
 * showrunner drip ordering reads this so it never OPENS the arc on a `dead_end`/`lore`
 * found-document row (COHERENCE-AUDIT C2 / P0-7): it prefers a node that moves the web
 * (`next_clue`/`main_beat`/`side_quest`) over one that is true-but-terminal. `'unknown'`
 * is the safe default for a row whose type didn't parse — treated as low-priority, never
 * preferred, but never crashing the pure policy.
 */
export type OutcomeType = 'next_clue' | 'main_beat' | 'side_quest' | 'lore' | 'dead_end' | 'unknown';

/** One open puzzle as the showrunner sees it (a projection of the puzzles row + live signals). */
export interface SnapshotPuzzle {
  puzzleKey: string;
  movement: number;
  /**
   * the row's outcome semantics (ORACLE.md §3). Carried so the pure `decide` can order the
   * drip by story-shape, not just (movement, key) — see {@link OutcomeType}.
   */
  outcomeType: OutcomeType;
  /**
   * whether this node carries a Discord-decodable forged clue card (it is in the P0-1
   * forge-spec registry, `forgeablePuzzleKeys()`). snapshot.ts only places forgeable rows
   * into the drip pool (COHERENCE-AUDIT C3 / P0-7), so this is `true` for every pooled
   * puzzle today; it is carried explicitly so the invariant is visible to `decide` + tests
   * and so a future in-world-report routing can branch on it without re-deriving it.
   */
  forgeable: boolean;
  /** failed answer_attempts on this puzzle within the stall window (group-wide). */
  failedAttemptsInWindow: number;
  /** solved by anyone within the stall window? if so it is not "stuck". */
  solvedInWindow: boolean;
  /** the distinct people who attempted it in the window, with their whisper state. */
  attempters: AttempterState[];
  /** has this puzzle already been announced/dripped before? (drip each clue once). */
  dripped: boolean;
}

/** A player's whisper state for one stuck puzzle — enough to decide a fair auto-gift. */
export interface AttempterState {
  playerId: string;
  act: number;
  /** remaining whisper allowance = budget + earned - spent. */
  whisperRemaining: number;
  /** the next hint tier for this (player, puzzle) = prior whispers + 1. */
  nextTier: number;
  /** whether a pre-authored hint body exists for nextTier (never invent a hint). */
  nextTierHintExists: boolean;
}

/**
 * The difficulty grip the showrunner reads onto the tick (A10 `dynamic-diegetic-difficulty`, FACT 2b).
 * `state` is the grip; `cadenceMult` scales dripIntervalMs (the land waits / relents); `tone` selects
 * the Watcher's register variant. Mirrors {@link reckon} in reckoning.ts. Carried OPTIONALLY on the
 * Snapshot so the existing pure self-tests (which omit it) still compile — absent ⇒ `even`/×1/`plain`.
 */
export type ReckoningState = 'tight' | 'even' | 'loose';
export type Tone = 'cold' | 'plain' | 'warm';

/** The cold-start prologue gate the showrunner reads onto the tick (B4, WEB-MASTER §1.M1). */
export interface PrologueGate {
  /** whether the curatorial clue-drip is permitted this tick (false until the prologue is ignited). */
  curatorialAllowed: boolean;
}

/** Immutable input to decide(). Thresholds are injected so the policy stays pure + tunable. */
export interface Snapshot {
  nowMs: number;
  /** settings.watcher_sleep — the master kill-switch. When true the spine does nothing. */
  asleep: boolean;
  /** settings.showrunner_mode (default 'confirm'). */
  mode: ShowrunnerMode;
  /** arc_state.current_act. */
  currentAct: number;
  openPuzzles: SnapshotPuzzle[];
  /** epoch ms of the last drip, or null if none ever. */
  lastDripAtMs: number | null;
  /** group-wide failed attempts on one puzzle (no solve) to count as "stuck". */
  stallFailedThreshold: number;
  /** minimum gap between drips. */
  dripIntervalMs: number;
  /**
   * OPTIONAL difficulty grip for this tick (A10). When present, decide() multiplies the effective
   * drip interval by `cadenceMult` (the land waits longer when the group races / sooner when it
   * stumbles) and carries `tone` onto the Decision for the voice layer. Absent ⇒ neutral (no scaling,
   * `plain` tone) — so the deterministic self-tests that omit it are unchanged.
   */
  reckoning?: { state: ReckoningState; cadenceMult: number; tone: Tone };
  /**
   * OPTIONAL cold-start prologue gate (B4). When present and NOT yet ignited, the curatorial drip is
   * suppressed (gifts still apply — player-helpful, never gated). Absent ⇒ allowed (back-compat: the
   * existing self-tests have no prologue and must keep dripping).
   */
  prologue?: PrologueGate;
}

/** Bump one earned whisper for a stuck, exhausted player so they can claim `tier`. */
export interface GiftDecision {
  playerId: string;
  act: number;
  puzzleKey: string;
  tier: number;
  reason: string;
}

/** Announce the next clue. In confirm mode it is staged (pending a dashboard approval), not posted. */
export interface DripDecision {
  puzzleKey: string;
  movement: number;
  /**
   * whether this dripped node carries a forgeable Discord clue card (P0-1 registry). AUTO
   * apply.ts forges + posts its card when true (COHERENCE-AUDIT C1 / P0-6); when false it
   * posts the in-world-pointing report line (`voice.drip()`) instead of a forged card.
   * Today the pool is forgeable-only (P0-7), so this is true for live drips; carried so the
   * apply step never has to re-derive it and so the routing rule is explicit.
   */
  forgeable: boolean;
  staged: boolean;
  reason: string;
}

/** A heartbeat written every tick so the dashboard can show "showrunner alive / last run". */
export interface HealthBeat {
  atMs: number;
  openPuzzleCount: number;
  note: string;
}

export interface Decision {
  health: HealthBeat;
  gifts: GiftDecision[];
  drips: DripDecision[];
  /**
   * the register temperature for this tick's Watcher lines (A10). Carried so apply.ts can pass a
   * `tone` arg into the voice calls (a SELECTION among authored variants, never generated text).
   * Defaults to `'plain'` when the snapshot carries no reckoning — the back-compat neutral.
   */
  tone: Tone;
  /** human-readable trace lines (logged; never player-facing). */
  notes: string[];
}
