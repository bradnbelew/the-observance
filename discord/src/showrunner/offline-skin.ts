/**
 * offline-skin.ts — the offline-skin apparition orchestrator (B3 `offline-skin-apparition`, FACT 9, INV-16).
 *
 * "the apparition wearing an offline player's skin." A rare reflection/edge/crouch re-skin of the
 * shared apparition vocabulary, wearing an OFFLINE friend's skin. This is the pure orchestration
 * policy: pick WHEN and WHOSE skin, precision-gated, with the separation law enforced. Walking /
 * following / pathfinding / chat are all CUT; the M3 glimpse is deniable (no name-tag); exactly ONE
 * human-approved NAMED M4 beat is allowed.
 *
 * THE PRECISION GATE (the privacy law). The worn skin must be a player CANONICALLY RHYMED to the
 * apparition shape — a measured rhyme, never a callout. We re-skin ONLY a player whose dossier
 * actually rhymes with the shape; a flat/unrhymed offline player is never worn (a wrong "it is wearing
 * Brann" is worse than none).
 *
 * THE SEPARATION LAW (coherence P2-5, INV-16). Skin-wearing is OFFLINE-only; name-carving
 * (name-where-never-been) is ACTIVE-only. They must NEVER co-locate the same player's name and worn
 * skin at the same keeper stone/window. This module takes the set of cells/windows the carve lane is
 * using this window and refuses any worn-skin glimpse that would collide.
 *
 * REVEAL DISCIPLINE + DESPAWN (anti-jank). The glimpse is discovered, never witnessed appearing; if
 * the worn player REJOINS, the apparition despawns (handled by PresenceListener on the plugin side;
 * this policy emits the despawn intent). Silhouette fallback if the skin won't load (plugin). A
 * per-worn-player one-shot budget keeps it rare.
 *
 * PURE. No DB / clock / LLM. The M4 named line is an authored voice key (FACT 9). offline-skin.run.ts
 * reads presence + dossiers + the carve-window claims and fires NamedMobBeat; offline-skin.selftest.ts
 * imports this with nothing.
 */

/** The apparition shapes a skin can be worn over (the shared vocabulary; WEB-MASTER §7). */
export type ApparitionShape = 'watcher_at_edge' | 'surface_walker' | 'stoop';

/** An offline player who is a candidate to be worn, with their measured rhyme to each shape. */
export interface OfflineCandidate {
  groupKey: string;
  name: string | null;
  /** is this player currently OFFLINE? (skin-wearing is offline-only). */
  offline: boolean;
  /** 0..1 measured rhyme to each shape; the dominant one (by margin) is the shape worn. */
  shapeRhyme: Readonly<Partial<Record<ApparitionShape, number>>>;
  /** how many times this player's skin has already been worn (one-shot budget per phase). */
  wornCount: number;
}

/** The phase the glimpse fires in — M3 deniable (no name-tag) or the single M4 named beat. */
export type GlimpsePhase = 'deniable' | 'named';

export interface OfflineSkinConstants {
  /** the dominant shape-rhyme must reach this to wear the skin (precision floor). */
  minRhyme: number;
  /** the dominant shape must beat the runner-up by this margin (a real rhyme, not a tie). */
  minRhymeMargin: number;
  /** max times one player's skin is worn in a phase (one-shot budget → rare). */
  maxWornPerPhase: number;
}

export const OFFLINE_SKIN_DEFAULTS: OfflineSkinConstants = {
  minRhyme: 0.5,
  minRhymeMargin: 0.15,
  maxWornPerPhase: 1,
};

export interface OfflineSkinInput {
  /** offline candidates (the run wrapper filters to offline; online players never appear). */
  candidates: OfflineCandidate[];
  /** the phase the arc is in for this beat (deniable M3 vs the named M4 beat). */
  phase: GlimpsePhase;
  /**
   * the cells the carve lane (name-where-never-been) is using THIS window — the separation law: a
   * worn-skin glimpse may not be placed where that lane carves the same player's name. Keyed by
   * groupKey → the set of cells claimed for that player's carve.
   */
  carveClaimsByPlayer: Readonly<Record<string, ReadonlySet<string>>>;
  /** the candidate cell the glimpse would occupy (the run wrapper proposes; null = pick none). */
  proposedCell: string | null;
  /** the named M4 beat requires explicit human approval — only true after the dashboard approves. */
  namedApproved: boolean;
}

/** The single glimpse to produce, or null when no precision-safe, non-colliding wearing exists. */
export interface GlimpseDecision {
  groupKey: string;
  name: string;
  shape: ApparitionShape;
  phase: GlimpsePhase;
  /** show the name-tag? ONLY for the approved named M4 beat; never in the deniable phase. */
  nameTag: boolean;
  cell: string | null;
  reason: string;
}

export interface OfflineSkinResult {
  glimpse: GlimpseDecision | null;
  notes: string[];
}

/** Dominant shape for a candidate, or null if the rhyme is flat/tied (precision floor). */
function dominantShape(c: OfflineCandidate, k: OfflineSkinConstants): ApparitionShape | null {
  const entries = (Object.entries(c.shapeRhyme) as [ApparitionShape, number][])
    .filter(([, v]) => Number.isFinite(v))
    .sort((a, b) => b[1] - a[1]);
  if (entries.length === 0) return null;
  const [topShape, top] = entries[0]!;
  const runnerUp = entries[1]?.[1] ?? 0;
  if (top < k.minRhyme || top - runnerUp < k.minRhymeMargin) return null;
  return topShape;
}

/**
 * selectGlimpse — pure. Picks ONE offline player to wear (rarest-worn first → spread), whose dossier
 * confidently rhymes with a shape, who is under the one-shot budget, and whose glimpse cell does NOT
 * collide with that player's active carve claim (the separation law). The named phase additionally
 * requires explicit human approval and is the only phase that shows a name-tag.
 */
export function selectGlimpse(inp: OfflineSkinInput, k: OfflineSkinConstants = OFFLINE_SKIN_DEFAULTS): OfflineSkinResult {
  const notes: string[] = [];

  if (inp.phase === 'named' && !inp.namedApproved) {
    return { glimpse: null, notes: ['named M4 beat requires human approval — withheld'] };
  }

  const ranked = [...inp.candidates]
    .filter((c) => {
      if (!c.offline) { notes.push(`skipped ${c.name ?? c.groupKey}: online (skin-wearing is offline-only)`); return false; }
      if (!c.name) { notes.push(`skipped ${c.groupKey}: no name`); return false; }
      if (c.wornCount >= k.maxWornPerPhase) { notes.push(`skipped ${c.name}: one-shot budget spent`); return false; }
      return true;
    })
    .sort((a, b) => a.wornCount - b.wornCount || (a.name ?? '').localeCompare(b.name ?? ''));

  for (const c of ranked) {
    const shape = dominantShape(c, k);
    if (!shape) { notes.push(`skipped ${c.name}: flat shape-rhyme (no canonical wearing)`); continue; }

    // Separation law: don't place the glimpse where THIS player's name is being carved this window.
    const claims = inp.carveClaimsByPlayer[c.groupKey];
    if (inp.proposedCell != null && claims && claims.has(inp.proposedCell)) {
      notes.push(`skipped ${c.name}: glimpse cell collides with their active name-carve (separation law)`);
      continue;
    }

    return {
      glimpse: {
        groupKey: c.groupKey,
        name: c.name!,
        shape,
        phase: inp.phase,
        nameTag: inp.phase === 'named', // deniable phase NEVER shows a name-tag
        cell: inp.proposedCell,
        reason: `wear ${c.name} over ${shape} (${inp.phase}; worn ${c.wornCount}x; no carve collision)`,
      },
      notes,
    };
  }

  notes.push('no precision-safe, non-colliding offline wearing this window — no glimpse');
  return { glimpse: null, notes };
}
