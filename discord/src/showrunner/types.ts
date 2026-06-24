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

/** One open puzzle as the showrunner sees it (a projection of the puzzles row + live signals). */
export interface SnapshotPuzzle {
  puzzleKey: string;
  movement: number;
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
  /** human-readable trace lines (logged; never player-facing). */
  notes: string[];
}
