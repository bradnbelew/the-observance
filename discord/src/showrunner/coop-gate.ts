/**
 * coop-gate.ts — the Discord closer for the three-hands coop gate (m4-three-hands; design/PUZZLE-DESIGNS §A6).
 *
 * The IV→V hinge is the one CROSS-SURFACE coop gate: three acts in one window — a foot on the plate + a
 * carve at the mark (both in-world, owned by the plugin's CoopPlateListener) + the convergence WORD posted
 * in #the-record (owned here). The plugin publishes `coop_world_ready_at` (epoch ms) when both world legs
 * are fresh together; this module is the SOLE closer — when the convergence word is posted while that marker
 * is fresh, it submits the puzzle's opaque token through the sanctioned oracle, opening the Threshold. One
 * closer → no double-fire; grounded (only the real word closes it); self-healing (redo any leg in-window).
 *
 * DEGRADE TO SILENCE: non-matching post, stale/absent marker, closed puzzle, replay, or any failure → null.
 * Never throws.
 */
import { resolveAnswer } from '../oracle/resolve.js';
import { normalizeAnswer } from '../oracle/normalize.js';
import { getArcFlags } from '../db/repo.js';
import type { Player } from '../db/types.js';

/**
 * The convergence word the catch yields — the word another gate waits on (hints_seed.sql:157). It is the
 * same phrase as bound-word's answer and spine-spoken-name's spoken truth (a deliberate motif rhyme).
 */
const CONVERGENCE_WORD = 'the one who turned away';
/**
 * The opaque accepted token for m4-three-hands (discord/supabase/seeds/puzzles_seed.sql). The closer posts
 * this on a cleared gate — never typeable by a player (it is not in any hint), kept in sync with the seed.
 */
const M4_TOKEN = 'h3n8k1 q5m2x7 w9j4p6 t1b6f0 c8d3s5 v2z7r4';
/** The plugin's ready marker (kept in sync with CoopPlateListener.READY_FLAG). */
const READY_FLAG = 'coop_world_ready_at';
/** How fresh the world-legs marker must be for the word to close the gate. Match the plugin window-seconds. */
const COOP_WINDOW_MS = 90_000;

/** Pre-normalized needle so the constant isn't re-normalized on every post. */
const WORD_NEEDLE = normalizeAnswer(CONVERGENCE_WORD);

/**
 * maybeCloseCoopGate — if the convergence word was posted while the plugin's world-legs marker is fresh,
 * solve m4-three-hands and return the Watcher's reply to speak. Otherwise null (say nothing). Never throws.
 */
export async function maybeCloseCoopGate(player: Player, raw: string): Promise<string | null> {
  try {
    const said = normalizeAnswer(raw);
    if (!said || !said.includes(WORD_NEEDLE)) return null; // not the convergence word → nothing

    const flags = await getArcFlags();
    const readyAt = Number((flags as Record<string, unknown>)[READY_FLAG]);
    if (!Number.isFinite(readyAt) || Date.now() - readyAt > COOP_WINDOW_MS) return null; // legs not fresh

    // The two world legs are fresh AND the word is now posted — clear the gate through the real resolver
    // (open only while m4 is the open row; idempotent; rate-limited). Only a genuinely-new solve speaks.
    const result = await resolveAnswer({ player, discordId: player.discord_id ?? null }, M4_TOKEN, 'world');
    return result.kind === 'solved' ? result.reply : null;
  } catch {
    return null;
  }
}
