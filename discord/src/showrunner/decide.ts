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
 *  3. Clue drip: when the cadence is due, announce the next un-dripped open puzzle. The pool snapshot.ts
 *     hands us is ALREADY filtered to Discord-decodable forged-clue nodes (COHERENCE-AUDIT C3 / P0-7) —
 *     found-document / sentinel / in-world-only rows never reach here. Among those, the order respects
 *     STORY SHAPE before sort-key (COHERENCE-AUDIT C2 / P0-7): a node that MOVES the web
 *     (next_clue / main_beat / side_quest) is preferred over a true-but-terminal one (lore / dead_end),
 *     so the first-ever drip can never open the arc on a dead-end. Within an outcome rank the order is
 *     the original deterministic (movement asc, then key asc). In CONFIRM mode the drip is staged (awaits
 *     dashboard approval); in AUTO it fires live. Player-helpful gifts always apply; only the curatorial
 *     drip respects the gate. Still PURE + deterministic: same snapshot in → same decision out.
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
    const next = s.openPuzzles
      // Only Discord-decodable forged-clue nodes are drippable (COHERENCE-AUDIT C3 / P0-7):
      // found-document / sentinel / in-world-only rows have no card to surface. snapshot.ts
      // marks each row's `forgeable` from the P0-1 registry; non-forgeable rows still flow
      // through openPuzzles for the stall backstop, but never enter the drip pool.
      .filter((p) => !p.dripped && p.forgeable)
      .sort(
        (x, y) =>
          OUTCOME_RANK[x.outcomeType] - OUTCOME_RANK[y.outcomeType] ||
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
      // Nothing drippable. Distinguish "all forgeable nodes already dripped" from "no forgeable
      // node is open at all" (the pool is all found-document / sentinel rows, C3) so the trace is
      // accurate — both leave the cadence anchor untouched (apply.ts only advances it on a drip).
      const anyForgeableOpen = s.openPuzzles.some((p) => p.forgeable);
      notes.push(
        anyForgeableOpen
          ? 'drip due but every forgeable puzzle has already been dripped'
          : 'drip due but no forgeable (Discord-decodable) puzzle is open — pool empty',
      );
    }
  } else if (curatorialAllowed) {
    notes.push('drip not due yet');
  }

  health.note = `gifts=${gifts.length} drips=${drips.length} tone=${tone}`;
  return { health, gifts, drips, tone, notes };
}
