/**
 * decide() — the PURE, deterministic showrunner policy. No DB, no network, no LLM, no clock of its own
 * (the time comes in via Snapshot.nowMs). Same snapshot in → same decision out, so it is fully
 * unit-testable (decide.selftest.ts) and behaves identically with the AI layer absent.
 *
 * Policy:
 *  1. Kill-switch: if asleep (settings.watcher_sleep), do nothing but heartbeat.
 *  2. Stall auto-gift (the retention backstop the critics asked for): a puzzle is "stuck" when it has
 *     >= stallFailedThreshold failed attempts in the window AND no solve in the window. For each stuck
 *     puzzle, gift ONE earned whisper to each attempter who is OUT of whispers AND has a real next-tier
 *     hint to receive. The double guard (exhausted + hint-exists) makes it a true backstop — it never
 *     over-gifts a player who still has a whisper, and never promises a hint that wasn't authored.
 *  3. Clue drip: when the cadence is due, announce the next un-dripped open puzzle. The pool is every
 *     open row that is EITHER forgeable (a Discord-decodable clue card, COHERENCE-AUDIT C3 / P0-7) OR a
 *     non-forgeable STORY-ADVANCING node apply.ts can surface via the in-world report line (audit #7,
 *     the salience dead-air fix — so a pointer keeps flowing after the five keeper ciphers are spent).
 *     Non-forgeable TERMINAL rows (lore / dead_end / unknown found-documents) never enter the pool.
 *     ROSTER GUARD (S-F): a convergence/quorum-gated node (requiresQuorum > activeRosterSize) is ALSO
 *     excluded — surfacing a thread the present roster cannot close is the dead-air failure we are
 *     fixing. The guard is optional/no-op when either field is absent (see types.ts).
 *     Among the pool, the pick order is (COHERENCE-AUDIT C2 / P0-7 + the S-F salience reshape "web not
 *     chain"):
 *       (a) STORY SHAPE first — a node that MOVES the web (next_clue / main_beat / side_quest) outranks a
 *           true-but-terminal one (lore / dead_end), so the first-ever drip can never open on a dead_end;
 *       (b) then ENGAGEMENT SALIENCE desc — the thread the group is actually pulling on rises: distinct
 *           attempter count (player-fingerprints) first, then failed-attempts-in-window (recency);
 *       (c) then the existing deterministic tiebreakers: forgeable desc (a real card beats a report
 *           line), movement asc, then puzzleKey asc.
 *     In CONFIRM mode the drip is staged (awaits dashboard approval); in AUTO it fires live.
 *     Player-helpful gifts always apply; only the curatorial drip respects the gate. Still PURE +
 *     deterministic: same snapshot in → same decision out.
 */
import type { Decision, GiftDecision, DripDecision, OutcomeType, Snapshot, Tone } from './types.js';

/**
 * Drip ordering by story-shape (COHERENCE-AUDIT C2 / P0-7). LOWER ranks first, so a node that
 * advances the web outranks one that is true-but-terminal. `dead_end` and `lore` are the highest
 * (last) ranks — they may still drip if nothing better remains, but they can NEVER be the opener.
 * `unknown` sorts between movers and terminals: never preferred, never crashing the policy.
 */
const OUTCOME_RANK: Readonly<Record<OutcomeType, number>> = {
  next_clue: 0,
  main_beat: 1,
  side_quest: 2,
  unknown: 3,
  lore: 4,
  dead_end: 5,
};

/**
 * Story-ADVANCING outcome types (a node that MOVES the web). Used by the drip pool to decide when a
 * NON-forgeable row may be surfaced (audit #7, the salience dead-air fix): a non-cipher node has no
 * clue card, but apply.ts already degrades such a drip to the in-world-pointing report line
 * (`voice.drip()`). So a non-forgeable MOVER — the currently-live back-half spine node after the five
 * keeper ciphers are spent (undercroft-descent, rite-tokens, accepting-crouch, …) — can still get a
 * pointer, closing the "nothing points at the next thread" cliff. A non-forgeable TERMINAL row
 * (lore / dead_end / unknown) is still NEVER dripped: it has neither a card nor forward motion, so a
 * report line about it would point at a thread that opens nothing (the found-document guardrail).
 */
const MOVER_OUTCOMES: ReadonlySet<OutcomeType> = new Set<OutcomeType>(['next_clue', 'main_beat', 'side_quest']);

/**
 * S-F ROSTER GUARD. `true` unless the node is quorum-gated AND the active roster is below that quorum.
 * Never surface a convergence thread the present group cannot possibly close (the dead-air failure the
 * reshape is fixing). Optional-and-safe: if the puzzle carries no `requiresQuorum`, or the snapshot
 * carries no `activeRosterSize`, this is a pure no-op (returns true) — so it does nothing until BOTH
 * fields are wired, and the existing quorum-free self-tests are unchanged.
 *
 * WIRED (S-F roster, 0008_requires_quorum): both feeds are live. snapshot.ts selects
 * `puzzles.requires_quorum` (-> SnapshotPuzzle.requiresQuorum) and populates Snapshot.activeRosterSize
 * from `readActiveRoster(STALL_WINDOW_MS).length` (the single active-set source, autonomy.run.js).
 * Four convergence nodes carry requires_quorum=2 (m4-three-hands, accepting-crouch,
 * spine-threshold-vault, mara-walk-the-map). The effectiveQuorum enforced in the plugin
 * (AcceptingRiteListener: min(configQuorum, activeRosterSize)) is the in-world twin of this drip-side
 * guard. A puzzle with no requires_quorum (the common case) still no-ops it, so the quorum-free
 * self-tests are unchanged.
 */
function rosterCanClose(p: { requiresQuorum?: number }, s: Snapshot): boolean {
  if (p.requiresQuorum == null || s.activeRosterSize == null) return true;
  return s.activeRosterSize >= p.requiresQuorum;
}

export function decide(s: Snapshot): Decision {
  const notes: string[] = [];
  const health = { atMs: s.nowMs, openPuzzleCount: s.openPuzzles.length, note: '' };

  // A10: the register temperature for this tick (selection among authored voice variants). Neutral
  // (`plain`) when the snapshot carries no difficulty grip — the back-compat default for the spine.
  const tone: Tone = s.reckoning?.tone ?? 'plain';

  if (s.asleep) {
    health.note = 'asleep';
    notes.push('watcher_sleep=true — kill-switch engaged; heartbeat only');
    return { health, gifts: [], drips: [], tone, notes };
  }

  // 1. Stall auto-gifts ------------------------------------------------------
  const gifts: GiftDecision[] = [];
  for (const p of s.openPuzzles) {
    const stuck = !p.solvedInWindow && p.failedAttemptsInWindow >= s.stallFailedThreshold;
    if (!stuck) continue;
    for (const a of p.attempters) {
      if (a.whisperRemaining > 0) continue;       // still has a whisper — not a backstop case
      if (!a.nextTierHintExists) continue;        // nothing authored left to give
      gifts.push({
        playerId: a.playerId,
        act: a.act,
        puzzleKey: p.puzzleKey,
        tier: a.nextTier,
        reason: `stuck: ${p.failedAttemptsInWindow} failed, no solve in window; whispers exhausted`,
      });
    }
  }

  // 2. Clue drip -------------------------------------------------------------
  // A10: the land's grip scales the cadence — it WAITS longer on a racing group, RELENTS for one that
  // stumbles. One pure line: multiply the interval by cadenceMult (×1 when no reckoning is present).
  const effectiveDripInterval = s.dripIntervalMs * (s.reckoning?.cadenceMult ?? 1);
  // B4: never drip a curatorial clue before the prologue is ignited (gifts above are unaffected —
  // player-helpful, never gated). Absent prologue ⇒ allowed (back-compat with the existing tests).
  const curatorialAllowed = s.prologue?.curatorialAllowed ?? true;

  const drips: DripDecision[] = [];
  const dripDue = curatorialAllowed && (s.lastDripAtMs == null || s.nowMs - s.lastDripAtMs >= effectiveDripInterval);
  if (!curatorialAllowed) {
    notes.push('curatorial drip suppressed — prologue not ignited');
  }
  if (dripDue) {
    // DRIP POOL (COHERENCE-AUDIT C3 / P0-7 + audit #7 salience fix): a node is drippable if it
    // EITHER carries a Discord-decodable forged clue card (`forgeable`, the P0-1 registry) OR it is
    // a NON-forgeable STORY-ADVANCING node (a mover) that apply.ts can surface via the
    // in-world-pointing report line. This is what keeps a pointer flowing after Movement II: once
    // the five keeper ciphers are spent, the live back-half spine (undercroft-descent, rite-tokens,
    // accepting-crouch, the catch chain, …) is non-forgeable, and WITHOUT this fallback the pool
    // went empty → dead-air with no in-world pointer to the next thread. A non-forgeable TERMINAL
    // row (lore / dead_end / unknown) is still excluded: no card, no forward motion — a report line
    // about it would point at a found-document that opens nothing.
    const drippable = s.openPuzzles.filter(
      (p) => !p.dripped && (p.forgeable || MOVER_OUTCOMES.has(p.outcomeType)) && rosterCanClose(p, s),
    );
    // PICK ORDER (S-F salience reshape). Lower = picked first:
    //   (a) story-shape rank (OUTCOME_RANK): a mover MUST still outrank a terminal — the "a drip can
    //       never open on a dead_end" guarantee holds because shape is the FIRST key;
    //   (b) SALIENCE desc: surface the thread the group is actually pulling on. Score = distinct
    //       attempter count (player-fingerprints), then failed-attempts-in-window (recency/engagement);
    //   (c) tiebreakers, unchanged + deterministic: forgeable desc (a real card beats a report line),
    //       movement asc, then puzzleKey asc.
    const next = drippable.sort(
      (x, y) =>
        OUTCOME_RANK[x.outcomeType] - OUTCOME_RANK[y.outcomeType] ||
        // (b) engagement salience — higher wins. Distinct attempters first (how many DIFFERENT people
        // are on this thread), then failed attempts in the window (how hard the group is pulling).
        y.attempters.length - x.attempters.length ||
        y.failedAttemptsInWindow - x.failedAttemptsInWindow ||
        // (c) Within the same shape+salience, PREFER a forgeable node (a real decodable clue card
        // beats a bare in-world report line) before falling back to (movement asc, key asc).
        Number(y.forgeable) - Number(x.forgeable) ||
        x.movement - y.movement ||
        x.puzzleKey.localeCompare(y.puzzleKey),
    )[0];
    if (next) {
      drips.push({
        puzzleKey: next.puzzleKey,
        movement: next.movement,
        forgeable: next.forgeable,
        staged: s.mode === 'confirm',
        reason: s.lastDripAtMs == null ? 'first drip' : 'cadence due',
      });
    } else {
      // Nothing drippable. Distinguish "everything drippable is already dripped" from "no drippable
      // (forgeable OR mover) node is open at all" (the pool is only terminal found-document /
      // sentinel rows, C3) so the trace is accurate — both leave the cadence anchor untouched
      // (apply.ts only advances it on a drip). "no forgeable" is retained in the empty-pool note so
      // the existing pool-empty guardrail assertions stay meaningful.
      const anyDrippableOpen = s.openPuzzles.some(
        (p) => (p.forgeable || MOVER_OUTCOMES.has(p.outcomeType)) && rosterCanClose(p, s),
      );
      notes.push(
        anyDrippableOpen
          ? 'drip due but every drippable (forgeable or story-advancing) puzzle has already been dripped'
          : 'drip due but no forgeable or story-advancing puzzle is open — pool empty',
      );
    }
  } else if (curatorialAllowed) {
    notes.push('drip not due yet');
  }

  health.note = `gifts=${gifts.length} drips=${drips.length} tone=${tone}`;
  return { health, gifts, drips, tone, notes };
}
