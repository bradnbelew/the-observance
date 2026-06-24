/**
 * resolve.ts — THE ORACLE CORE. The single resolver both surfaces share.
 *
 * The closed loop (ORACLE.md §1): a player solves a forged clue and submits the
 * plaintext — in Discord (/answer or the #the-record scan) OR in-world (the
 * plugin's answer-sign). Both call THIS resolver against the SAME `puzzles`
 * table, so a match on either surface produces the same outcome.
 *
 *   normalize → rate-limit → match OPEN puzzles → (replay guard) → record solve
 *   → apply outcome (speak a voice line; maybe set flags; maybe enqueue a beat).
 *
 * This module writes NO English. It returns a discriminated `ResolveResult`; the
 * calling surface decides whether/how to speak it (Discord posts the voice line;
 * the world surface lets the enqueued beat BE the reply). On a true miss it
 * returns `{ kind: 'miss' }` and the surface stays SILENT — never an error,
 * never a closeness tell.
 *
 * Fault isolation: the resolver itself does not crash. It is still wrapped by the
 * caller's try/catch (per index.ts), and every Supabase helper it calls fails
 * gracefully. A wrong answer is silence; a stumble is silence too.
 */
import {
  getOpenPuzzles,
  matchPuzzle,
  hasSolved,
  recordSolve,
  enqueueOracleBeat,
  setArcFlags,
  countRecentAttempts,
  logAttempt,
  logEvent,
} from '../db/repo.js';
import { voice } from '../voice.js';
import type { OracleVoiceKey } from '../voice.js';
import type {
  AnswerSurface,
  OutcomePayload,
  OutcomeType,
  Player,
  Puzzle,
} from '../db/types.js';

const SOURCE = 'the-watcher/oracle';

// ---------------------------------------------------------------------------
// Rate-limit policy (anti-brute-force). Two gates, both windowed over
// answer_attempts.at:
//   1. a global token bucket per player — caps total attempts in a window so a
//      short answer can't be brute-forced before the bucket empties.
//   2. a per-puzzle cap — puzzles.max_attempts (nullable) limits tries on ONE
//      puzzle on top of the global bucket.
// Reaching either = in-voice withholding (voice.oracleWithheld), NEVER a hint.
//
// Tuning: 8 attempts / 60s. The shortest sane answer space (a 4-letter word or a
// 2-number coord) has thousands of candidates; 8/min makes brute force take days
// while never impeding a real solver, who needs one or two tries.
// ---------------------------------------------------------------------------
export const RATE_WINDOW_MS = 60_000;
export const RATE_MAX_IN_WINDOW = 8;

/**
 * Hard ceiling on how much raw input we read/store. A real answer is a handful of
 * words; anything past this is paste-spam/abuse and is truncated BEFORE normalize +
 * before it ever reaches answer_attempts.raw (so a giant message can't bloat the
 * table or the normalize regex). The match itself is unaffected — no real answer is
 * anywhere near this long. Discord already caps message length, but #the-record runs
 * on every message and /answer takes free text, so we cap defensively regardless.
 */
export const MAX_RAW_LEN = 512;

// ---------------------------------------------------------------------------
// Normalization — the EXACT algorithm (ORACLE.md §2), byte-for-byte identical to
// the plugin's Java normalizeAnswer. Drift here breaks the loop silently.
// ---------------------------------------------------------------------------

/**
 * Normalize raw player input to the matchable form. NFKC → lower → strip every
 * char that is not [a-z0-9 ] (mapping each run to a SPACE, so "bow,at" → "bow
 * at") → collapse whitespace → trim. An empty result never matches and is never
 * logged (it is not "plausibly an answer").
 */
export function normalizeAnswer(s: string): string {
  return s
    .normalize('NFKC')
    .toLowerCase()
    .replace(/[^a-z0-9 ]+/g, ' ') // non-alnum → space (so "BOW,AT" → "bow at")
    .replace(/\s+/g, ' ')
    .trim();
}

// ---------------------------------------------------------------------------
// Result type — what the resolver hands back to a surface.
// ---------------------------------------------------------------------------

/** The watcher should speak `reply` (a correct, genuinely-new solve). */
export interface SolvedResult {
  kind: 'solved';
  outcomeType: OutcomeType;
  puzzleKey: string;
  /** the in-character line to post (already resolved from voice.ts). */
  reply: string;
  /** present when the outcome opens another clue (next_clue / side_quest / main_beat). */
  nextPuzzleKey?: string;
  /** true if an in-world beat was enqueued (status 'approved'). */
  enqueuedBeat: boolean;
}

/** The watcher should withhold in voice (rate-limited or per-puzzle cap). */
export interface WithheldResult {
  kind: 'withheld';
  reply: string;
}

/**
 * No open puzzle matched, OR a genuine replay of an already-solved puzzle. The
 * surface stays SILENT — say nothing at all. (A miss and a replay are both
 * silence; only a dead_end is a *heard* non-advance.)
 */
export interface SilentResult {
  kind: 'silent';
  /** why we are silent — for logs only, never surfaced to a player. */
  reason: 'miss' | 'already-solved' | 'empty';
}

export type ResolveResult = SolvedResult | WithheldResult | SilentResult;

// ---------------------------------------------------------------------------
// Who is answering. Discord supplies discord_id always, player when linked. The
// world surface (plugin) has its own resolver; this TS path serves Discord, but
// the shape is surface-agnostic on purpose.
// ---------------------------------------------------------------------------
export interface Answerer {
  /** the bound keeper, or null for an unlinked discord user scanned in #the-record. */
  player: Player | null;
  /** always present for a discord attempt; the rate-limit key when unlinked. */
  discordId: string | null;
}

// ---------------------------------------------------------------------------
// resolveAnswer — the one entry point.
// ---------------------------------------------------------------------------

/**
 * Resolve one raw answer for one answerer on one surface.
 *
 * Order (each step a quiet exit, never a tell):
 *   1. normalize; empty → silent('empty') (not plausibly an answer; not logged).
 *   2. global rate-limit (token bucket); over → withheld.
 *   3. fetch OPEN puzzles; match by whole-string set-membership.
 *   4. no match → log attempt (matched=false) → silent('miss').
 *   5. matched but UNLINKED player → silent('miss') (a reward needs a keeper; we
 *      still logged the attempt so we never leak that it *was* a match).
 *   6. per-puzzle max_attempts reached → withheld.
 *   7. already solved → log (matched=true) → silent('already-solved').
 *   8. recordSolve (idempotent; ON CONFLICT DO NOTHING). lost the race → silent.
 *   9. apply outcome: set flags, enqueue beat (status 'approved'), pick voice.
 *      log attempt (matched=true) → solved.
 */
export async function resolveAnswer(
  answerer: Answerer,
  raw: string,
  surface: AnswerSurface,
): Promise<ResolveResult> {
  // 0. defensively bound the input length BEFORE any work. A real answer is short;
  //    a giant paste is abuse — cap it so it can't bloat answer_attempts.raw or feed
  //    a pathological string into the normalize regex. The capped text is what we
  //    normalize AND what we log as `raw` (so the stored row is bounded too).
  const rawCapped =
    typeof raw === 'string' && raw.length > MAX_RAW_LEN
      ? raw.slice(0, MAX_RAW_LEN)
      : (raw ?? '');

  const normalized = normalizeAnswer(rawCapped);

  // 1. empty → not plausibly an answer. Silent, and NOT logged (ORACLE.md §2).
  if (normalized === '') return { kind: 'silent', reason: 'empty' };

  const playerId = answerer.player?.id ?? null;
  const discordId = answerer.player?.discord_id ?? answerer.discordId;
  const mcUuid = answerer.player?.mc_uuid ?? null;

  // 2. global token bucket. Over → withhold, in voice. (Counts unlinked by
  //    discord_id; an outage fails open to 0 so a hiccup never locks anyone out.)
  const recent = await countRecentAttempts({
    playerId,
    discordId,
    windowMs: RATE_WINDOW_MS,
  });
  if (recent >= RATE_MAX_IN_WINDOW) {
    // still log this attempt as a no-match so the audit shows the pressure.
    await logAttempt({
      puzzleKey: null,
      playerId,
      mcUuid,
      discordId,
      surface,
      raw: rawCapped,
      normalized,
      matched: false,
    });
    return { kind: 'withheld', reply: voice.oracleWithheld() };
  }

  // 3. the OPEN web, and the first puzzle whose answers contain this string.
  const open = await getOpenPuzzles();
  const puzzle = matchPuzzle(open, normalized);

  // 4. true miss → log, stay silent. NEVER reveal it matched nothing-close.
  if (!puzzle) {
    await logAttempt({
      puzzleKey: null,
      playerId,
      mcUuid,
      discordId,
      surface,
      raw: rawCapped,
      normalized,
      matched: false,
    });
    return { kind: 'silent', reason: 'miss' };
  }

  // 5. matched, but no bound keeper to reward. We DO log (matched=true, for the
  //    audit) but stay silent to players: a reward requires a known keeper, and
  //    we must not betray to an unlinked scanner that they hit a real answer.
  if (!answerer.player) {
    await logAttempt({
      puzzleKey: puzzle.puzzle_key,
      playerId: null,
      mcUuid: null,
      discordId,
      surface,
      raw: rawCapped,
      normalized,
      matched: true,
    });
    return { kind: 'silent', reason: 'miss' };
  }
  const player = answerer.player;

  // 6. per-puzzle cap (on top of the global bucket). Reached → withhold.
  if (puzzle.max_attempts !== null) {
    const onThis = await countRecentAttempts({
      playerId: player.id,
      discordId,
      windowMs: RATE_WINDOW_MS,
      puzzleKey: puzzle.puzzle_key,
    });
    if (onThis >= puzzle.max_attempts) {
      await logAttempt({
        puzzleKey: puzzle.puzzle_key,
        playerId: player.id,
        mcUuid: player.mc_uuid,
        discordId,
        surface,
        raw: rawCapped,
        normalized,
        matched: true,
      });
      return { kind: 'withheld', reply: voice.oracleWithheld() };
    }
  }

  // 7. already solved → silent (the watcher does not acknowledge twice; even a
  //    dead_end fires its line only once). Log the matched attempt for audit.
  const already = await hasSolved(puzzle.puzzle_key, player.id);
  if (already) {
    await logAttempt({
      puzzleKey: puzzle.puzzle_key,
      playerId: player.id,
      mcUuid: player.mc_uuid,
      discordId,
      surface,
      raw: rawCapped,
      normalized,
      matched: true,
    });
    return { kind: 'silent', reason: 'already-solved' };
  }

  // 8. idempotent record FIRST (ON CONFLICT DO NOTHING). Only a genuinely-new
  //    row proceeds to the reward — this is the double-fire guard under a race.
  const attemptCount = recent + 1;
  const isNew = await recordSolve(puzzle.puzzle_key, player, attemptCount);
  if (!isNew) {
    // lost the race to a concurrent solve — treat as a replay, stay silent.
    await logAttempt({
      puzzleKey: puzzle.puzzle_key,
      playerId: player.id,
      mcUuid: player.mc_uuid,
      discordId,
      surface,
      raw: rawCapped,
      normalized,
      matched: true,
    });
    return { kind: 'silent', reason: 'already-solved' };
  }

  // 9. apply the outcome. From here the solve is durable; do reward work, then
  //    log the matched attempt and return the line to speak.
  const result = await applyOutcome(puzzle, player);

  await logAttempt({
    puzzleKey: puzzle.puzzle_key,
    playerId: player.id,
    mcUuid: player.mc_uuid,
    discordId,
    surface,
    raw: rawCapped,
    normalized,
    matched: true,
  });

  await logEvent(
    'info',
    SOURCE,
    `solved: ${player.name} ${puzzle.puzzle_key} (${puzzle.outcome_type}` +
      `${result.enqueuedBeat ? ', beat enqueued' : ''}) via ${surface}`,
  );

  return result;
}

// ---------------------------------------------------------------------------
// applyOutcome — realize one of the five outcome types. The TYPE is semantic
// (does it open a door?); the EFFECT is whatever the author put in the payload.
// All five share this path; they differ only in which payload keys are present.
// ---------------------------------------------------------------------------
async function applyOutcome(
  puzzle: Puzzle,
  solver: Player,
): Promise<SolvedResult> {
  const payload: OutcomePayload = puzzle.outcome_payload ?? {};

  // (a) optional arc_state flags — best-effort; never block the reward on it.
  if (payload.set_flags && Object.keys(payload.set_flags).length > 0) {
    try {
      await setArcFlags(payload.set_flags);
    } catch (err) {
      const m = err instanceof Error ? err.message : String(err);
      void logEvent('warn', SOURCE, `set_flags failed for ${puzzle.puzzle_key}: ${m}`);
    }
  }

  // (b) optional in-world beat — player reward, status 'approved' (fires on the
  //     plugin's next poll, no human gate). dead_end/lore rows carry no beat.
  let enqueuedBeat = false;
  if (payload.beat && payload.beat.type) {
    try {
      await enqueueOracleBeat(payload.beat, solver.mc_uuid);
      enqueuedBeat = true;
    } catch (err) {
      const m = err instanceof Error ? err.message : String(err);
      // the solve is already recorded; a failed enqueue must not error at the
      // player. Log it; the dashboard/showrunner can re-fire. Still speak.
      void logEvent('error', SOURCE, `beat enqueue failed for ${puzzle.puzzle_key}: ${m}`);
    }
  }

  // (c) the voice line. The payload's voice_key picks it; an unknown/missing key
  //     falls back to the outcome type's default so a typo never errors.
  const reply = speakOutcome(puzzle.outcome_type, payload);

  return {
    kind: 'solved',
    outcomeType: puzzle.outcome_type,
    puzzleKey: puzzle.puzzle_key,
    reply,
    ...(payload.next_puzzle_key ? { nextPuzzleKey: payload.next_puzzle_key } : {}),
    enqueuedBeat,
  };
}

/**
 * Map (outcome_type, payload) → the in-character line. `voice_key` names the
 * line; `voice_args` feed the ones that take an argument (only oracleLore does).
 * An unknown key falls through to the type default — the watcher always speaks
 * in register, even on an authoring typo. No English is written here.
 */
function speakOutcome(type: OutcomeType, payload: OutcomePayload): string {
  const key = payload.voice_key as OracleVoiceKey | undefined;

  switch (key) {
    case 'oracleNextClue':
      return voice.oracleNextClue();
    case 'oracleDeadEnd':
      return voice.oracleDeadEnd();
    case 'oracleSideQuest':
      return voice.oracleSideQuest();
    case 'oracleMainBeat':
      return voice.oracleMainBeat();
    case 'oracleLore':
      return voice.oracleLore(loreFragment(payload));
    default:
      // no/unknown key → the default line for the outcome type.
      return defaultLineFor(type, payload);
  }
}

/** The lore telling lives in voice_args.fragment; empty falls back in register. */
function loreFragment(payload: OutcomePayload): string {
  const frag = payload.voice_args?.['fragment'];
  if (typeof frag === 'string' && frag.trim() !== '') return frag;
  // no seeded telling — speak a calm placeholder in register, never an error.
  return 'there is more here than the mark. but the telling is not ready. not yet.';
}

/** Default voice line per outcome type when no explicit voice_key is given. */
function defaultLineFor(type: OutcomeType, payload: OutcomePayload): string {
  switch (type) {
    case 'next_clue':
      return voice.oracleNextClue();
    case 'lore':
      return voice.oracleLore(loreFragment(payload));
    case 'dead_end':
      return voice.oracleDeadEnd();
    case 'side_quest':
      return voice.oracleSideQuest();
    case 'main_beat':
      return voice.oracleMainBeat();
    default:
      return voice.oracleNextClue();
  }
}
