/**
 * conductor.ts — the bestiary spawn-bias conductor + the single-arbiter apparition slot
 * (D7 `backlog-bestiary-spawn-bias`, INV-18, WEB-MASTER §7).
 *
 * THE SINGLE-ARBITER SLOT (INV-18). At most ONE ambient figure/whisper per drama window. This
 * conductor is the sole arbiter: it reads the signal snapshot + the active roster, runs
 * `selectApparition`, and publishes ONE `apparitionClaim`. Every other ambient lane — offline-skin,
 * name-where, the six Keeper-NPC apparitions, the Ear's spatial voice — DEFERS to this claim before
 * firing. The conductor's restraint can't be defeated by a lane that never heard of it: there is one
 * claim per window and the run wrapper writes it where the others read it. (Player-earned DIRECTED
 * beats are exempt — they are not ambient and never pass through here.)
 *
 * PROBABILISTIC + PER-PLAYER-CAPPED (the precision contract). The bias is NEVER a deterministic "it
 * knows you" callout: a player's measured signal RAISES the probability their rhyming apparition is the
 * one this window, but the choice is a weighted draw, not an argmax. And each player's apparition is
 * capped per arc (a one-per-player budget over the built apparition beats) so the same friend is not
 * haunted on a fixed cadence. A wrong, too-precise apparition is worse than none.
 *
 * DETERMINISTIC-FOR-TEST, RANDOM-IN-WORLD (anti-jank). "Probabilistic" must still be reproducible (a
 * re-run of the same tick must claim the SAME apparition, or the slot is not idempotent and two could
 * fire). So the draw is a SEEDED weighted pick: the run wrapper passes a per-window seed (e.g. the
 * window id) and the same seed + same weights → the same claim. The world sees variety across windows
 * (the seed changes); a single tick is deterministic (idempotent slot). No `Math.random()` here.
 *
 * DEGRADES TO NO-OP (precision over recall). No active roster, no measured signal, or every candidate
 * capped → NO claim this window (silence is canon, INV-7). The conductor never invents an apparition to
 * fill the slot.
 *
 * PURE. No DB / clock / LLM. The apparition is a built beat key (NamedMobBeat / SacredAnimalBeat /
 * Private*), a vanilla-fallback shape — no language. conductor.run.ts reads the snapshot + roster +
 * per-player caps + the window seed, publishes the claim (writes it for the deferring lanes), and
 * enqueues the chosen beat. conductor.selftest.ts imports this with nothing.
 */

/** The shared apparition vocabulary (re-skins, not new creatures; WEB-MASTER §7). */
export type ApparitionShape = 'watcher_at_edge' | 'surface_walker' | 'stoop';

/** The built beat each shape rides (vanilla fallback first; ModeledMobBeat is a later silent upgrade). */
const SHAPE_BEAT: Readonly<Record<ApparitionShape, string>> = {
  watcher_at_edge: 'named_mob', // Vaun-shape, caesar
  surface_walker: 'named_mob', // Sella-shape, atbash/reflection
  stoop: 'named_mob', // Orin-shape, crouch-revealed
};

/** One active player's measured rhyme to the apparition shapes + their per-arc apparition budget. */
export interface ApparitionCandidate {
  groupKey: string;
  /** is this player ACTIVE this window? (the conductor reads the active roster only). */
  active: boolean;
  /** 0..1 measured rhyme to each shape; the dominant shape is what their apparition would wear. */
  shapeRhyme: Readonly<Partial<Record<ApparitionShape, number>>>;
  /** how many apparitions this player has already been the subject of this arc (the per-player cap). */
  apparitionCount: number;
}

export interface ConductorConstants {
  /** the per-player apparition cap over the arc (one-per-player by default → rare, never a cadence). */
  perPlayerCap: number;
  /**
   * the dominant shape-rhyme must reach this to be a candidate at all (precision floor) — below it the
   * player has no apparition this window (their rhyme is too faint to ground a wearing).
   */
  minRhyme: number;
  /**
   * a floor weight every eligible candidate gets ON TOP of their rhyme, so the draw is never a pure
   * argmax (the bias raises the odds; it does not select). Keeps it probabilistic, not deterministic.
   */
  baseWeight: number;
}

export const CONDUCTOR_DEFAULTS: ConductorConstants = {
  perPlayerCap: 1,
  minRhyme: 0.4,
  baseWeight: 0.5,
};

export interface ConductorInput {
  /** the active-roster candidates (the run wrapper filters to active; absent members never appear). */
  candidates: ApparitionCandidate[];
  /**
   * a per-window seed (e.g. a numeric hash of the window id). Same seed + same weights → same claim
   * (idempotent slot). The run wrapper varies it per window so the world sees variety across windows.
   */
  windowSeed: number;
}

/** The one apparition claim for this window (INV-18), or null when no precision-safe pick exists. */
export interface ApparitionClaim {
  groupKey: string;
  shape: ApparitionShape;
  /** the built beat key the run wrapper enqueues (vanilla fallback shape). */
  beat: string;
  reason: string;
}

export interface ConductorDecision {
  claim: ApparitionClaim | null;
  notes: string[];
}

/** The dominant shape for a candidate, or null if no shape clears the rhyme floor. */
function dominantShape(c: ApparitionCandidate, k: ConductorConstants): { shape: ApparitionShape; score: number } | null {
  const entries = (Object.entries(c.shapeRhyme) as [ApparitionShape, number][])
    .filter(([, v]) => Number.isFinite(v))
    .sort((a, b) => b[1] - a[1]);
  if (entries.length === 0) return null;
  const [shape, score] = entries[0]!;
  if (score < k.minRhyme) return null;
  return { shape, score };
}

/**
 * A tiny deterministic PRNG (mulberry32) — gives a reproducible [0,1) from the window seed so the
 * weighted draw is "random across windows, fixed within a window". Pure: same seed → same stream.
 */
function seededUnit(seed: number): number {
  let t = (seed >>> 0) + 0x6d2b79f5;
  t = Math.imul(t ^ (t >>> 15), t | 1);
  t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
}

/**
 * selectApparition — pure, single-arbiter, probabilistic-but-seeded. Among the active, under-cap
 * candidates whose dominant shape clears the rhyme floor, do a SEEDED weighted draw (weight = rhyme +
 * baseWeight) — the bias raises the odds, it does not pick the max. Returns ONE claim, or null when no
 * eligible candidate exists (degrade to no-op). Same input → same claim (idempotent slot).
 */
export function selectApparition(inp: ConductorInput, k: ConductorConstants = CONDUCTOR_DEFAULTS): ConductorDecision {
  const notes: string[] = [];

  // Eligible = active, under the per-player cap, with a shape clearing the rhyme floor. Sorted by
  // groupKey so the weighted-draw indexing is stable for a given seed (determinism).
  const eligible = inp.candidates
    .filter((c) => {
      if (!c.active) { notes.push(`skipped ${c.groupKey}: inactive (active-roster only)`); return false; }
      if (c.apparitionCount >= k.perPlayerCap) { notes.push(`skipped ${c.groupKey}: per-player cap reached`); return false; }
      return true;
    })
    .map((c) => ({ c, dom: dominantShape(c, k) }))
    .filter((e): e is { c: ApparitionCandidate; dom: { shape: ApparitionShape; score: number } } => {
      if (e.dom == null) { notes.push(`skipped ${e.c.groupKey}: flat shape-rhyme below floor`); return false; }
      return true;
    })
    .sort((a, b) => a.c.groupKey.localeCompare(b.c.groupKey));

  if (eligible.length === 0) {
    return { claim: null, notes: [...notes, 'no eligible candidate this window — no apparition (single-arbiter slot stays empty)'] };
  }

  // Seeded weighted draw: weight = rhyme score + baseWeight (so a low-rhyme player still has a chance,
  // and a high-rhyme player is only MORE LIKELY — never certain; the precision contract).
  const weights = eligible.map((e) => e.dom.score + k.baseWeight);
  const total = weights.reduce((s, w) => s + w, 0);
  const roll = seededUnit(inp.windowSeed) * total;
  let acc = 0;
  let pickIdx = eligible.length - 1; // numeric-safety fallback to the last bucket
  for (let i = 0; i < eligible.length; i++) {
    acc += weights[i]!;
    if (roll < acc) { pickIdx = i; break; }
  }
  const chosen = eligible[pickIdx]!;

  return {
    claim: {
      groupKey: chosen.c.groupKey,
      shape: chosen.dom.shape,
      beat: SHAPE_BEAT[chosen.dom.shape],
      reason: `weighted draw over ${eligible.length} eligible (seed=${inp.windowSeed}); ${chosen.c.groupKey} wears ${chosen.dom.shape} (rhyme=${chosen.dom.score.toFixed(2)})`,
    },
    notes,
  };
}
