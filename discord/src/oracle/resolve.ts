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
  matchPuzzles,
  hasSolved,
  recordSolve,
  enqueueOracleBeat,
  setArcFlags,
  countRecentAttempts,
  logAttempt,
  logEvent,
} from '../db/repo.js';
import { voice } from '../voice.js';
import type { OracleVoiceKey, DeadEndKind } from '../voice.js';
import type {
  AnswerSurface,
  OutcomePayload,
  OutcomeType,
  Player,
  Puzzle,
} from '../db/types.js';
// The pure normalization algorithm lives in its own dependency-free module so tools/tests
// (seedcheck.ts) can import it without the DB/config chain. Re-exported here so the resolver's
// public surface is unchanged.
import { normalizeAnswer, MAX_RAW_LEN } from './normalize.js';
export { normalizeAnswer, MAX_RAW_LEN };

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

// (normalizeAnswer + MAX_RAW_LEN are imported from ./normalize.js above — the pure,
//  byte-for-byte twin of the plugin's Java normalizer. Drift breaks the loop silently.)

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

  // 3. the OPEN web (gated to the storylet-legal set for the current flags), and EVERY
  //    open puzzle whose answers contain this string. Usually 0 or 1; >1 only for a
  //    plaintext legitimately shared by a SEQUENCED pair (an already-solved upstream owner
  //    that stays open + its freshly-open downstream consumer — e.g. the bound word on
  //    `stone-iss-wall` then `bound-word`). We pick the player's first UNSOLVED candidate.
  const open = await getOpenPuzzles();
  const candidates = matchPuzzles(open, normalized);

  // 4. true miss → log, stay silent. NEVER reveal it matched nothing-close.
  if (candidates.length === 0) {
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
  //    (Unlinked answerers don't progress, so we attribute the log to the first match.)
  if (!answerer.player) {
    await logAttempt({
      puzzleKey: candidates[0]!.puzzle_key,
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

  // 5b. among the candidates, choose the first this player has NOT already solved (so an
  //     already-done upstream owner never shadows its downstream re-submission). If they
  //     have solved them all, `alreadySolved` flags the standard already-solved path below.
  const { puzzle, alreadySolved } = await pickCandidate(candidates, player.id);

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
  //    dead_end fires its line only once). `alreadySolved` was already determined by
  //    pickCandidate (true only when EVERY candidate is solved), so no re-query here.
  if (alreadySolved) {
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
// pickCandidate — when a normalized answer is shared by more than one OPEN row,
// choose the one this player has NOT solved yet. Single-candidate is the common
// case (one hasSolved read, identical to the old single-match path). The >1 case
// is a legitimately-sequenced re-submission: an upstream owner stays open after
// being solved (it is ungated active=true) and would otherwise shadow its freshly-
// opened downstream consumer forever. Returns `alreadySolved` only when EVERY
// candidate is solved, so the caller takes the standard already-solved silent path.
// (Simultaneous same-movement collisions are an authoring error, disambiguated in
// the seed — not papered over here.)
// ---------------------------------------------------------------------------
async function pickCandidate(
  candidates: readonly Puzzle[],
  playerId: string,
): Promise<{ puzzle: Puzzle; alreadySolved: boolean }> {
  for (const candidate of candidates) {
    if (!(await hasSolved(candidate.puzzle_key, playerId))) {
      return { puzzle: candidate, alreadySolved: false };
    }
  }
  // every candidate already solved → the first is the canonical owner; the caller
  // emits the already-solved silence. (candidates is non-empty by the caller's guard.)
  return { puzzle: candidates[0]!, alreadySolved: true };
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
      // speak the seeded dead-end KIND (name/count/place/known/prophet) so each dead-end reads specific,
      // not generic — the resolver was dropping voice_args.kind. Unknown/absent kind → the generic default.
      return voice.oracleDeadEnd(deadEndKind(payload));
    case 'oracleSideQuest':
      return voice.oracleSideQuest();
    case 'oracleMainBeat':
      return voice.oracleMainBeat();
    case 'oracleThreeHands':
      // the A6 three-hands coop gate — its bespoke "three hands at once" line (was falling through to
      // the generic main_beat line before this case existed).
      return voice.oracleThreeHands();
    case 'oracleNoWallCatch':
      // the Iss-seam: the catch's main_beat line + the callback into the Seventh quest.
      return voice.oracleNoWallCatch();
    case 'oracleLore':
      return voice.oracleLore(loreFragment(payload));
    default:
      // no/unknown key → the default line for the outcome type.
      return defaultLineFor(type, payload);
  }
}

/** The seeded dead-end kind (name/count/place/known/prophet) from voice_args, or undefined (generic). */
function deadEndKind(payload: OutcomePayload): DeadEndKind | undefined {
  const k = payload.voice_args?.['kind'];
  return k === 'name' || k === 'count' || k === 'place' || k === 'known' || k === 'prophet'
    ? k
    : undefined;
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
      return voice.oracleDeadEnd(deadEndKind(payload));
    case 'side_quest':
      return voice.oracleSideQuest();
    case 'main_beat':
      return voice.oracleMainBeat();
    default:
      return voice.oracleNextClue();
  }
}
