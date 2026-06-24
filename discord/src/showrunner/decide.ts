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
 *  3. Clue drip: when the cadence is due, announce the next un-dripped open puzzle (deterministic order:
 *     movement asc, then key asc). In CONFIRM mode the drip is staged (awaits dashboard approval); in
 *     AUTO it fires live. Player-helpful gifts always apply; only the curatorial drip respects the gate.
 */
import type { Decision, GiftDecision, DripDecision, Snapshot } from './types.js';

export function decide(s: Snapshot): Decision {
  const notes: string[] = [];
  const health = { atMs: s.nowMs, openPuzzleCount: s.openPuzzles.length, note: '' };

  if (s.asleep) {
    health.note = 'asleep';
    notes.push('watcher_sleep=true — kill-switch engaged; heartbeat only');
    return { health, gifts: [], drips: [], notes };
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
  const drips: DripDecision[] = [];
  const dripDue = s.lastDripAtMs == null || s.nowMs - s.lastDripAtMs >= s.dripIntervalMs;
  if (dripDue) {
    const next = s.openPuzzles
      .filter((p) => !p.dripped)
      .sort((x, y) => x.movement - y.movement || x.puzzleKey.localeCompare(y.puzzleKey))[0];
    if (next) {
      drips.push({
        puzzleKey: next.puzzleKey,
        movement: next.movement,
        staged: s.mode === 'confirm',
        reason: s.lastDripAtMs == null ? 'first drip' : 'cadence due',
      });
    } else {
      notes.push('drip due but every open puzzle has already been dripped');
    }
  } else {
    notes.push('drip not due yet');
  }

  health.note = `gifts=${gifts.length} drips=${drips.length}`;
  return { health, gifts, drips, notes };
}
