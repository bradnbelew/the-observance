/**
 * name-where-never-been.ts — the carve selector (A8 `name-where-never-been`, FACT 16, INV-14/16).
 *
 * "your name where you have never been." Between sessions the record carves a living player's name
 * at a cell the GROUP has avoided AND the carved player has provably never visited — so the carve
 * reads as the land filing the living by PLACE, not prediction (FACT 16). The unsettling precision
 * ("it put my name somewhere I've never been") is only earned if it is TRUE: the selector fires ONLY
 * with positive proof-of-absence.
 *
 * THE PROOF-OF-ABSENCE PRECISION GATE (the whole point). A cell is eligible for player T only if:
 *   - it is in the GROUP-AVOIDED set (no active player has marked it — a quiet corner), AND
 *   - T has provably NEVER visited it (T's `player_visited_cells` set does NOT contain it).
 * If we cannot PROVE T was never there (no visited-set for T yet), the beat does NOT fire for T — a
 * wrong "you've never been here" is worse than none (privacy precision law). No proof → no carve.
 *
 * ACTIVE-ONLY + THE SEPARATION LAW (INV-16 / WEB-MASTER §7). Carving is ACTIVE-only (never an offline
 * player — that is the offline-skin lane's domain, and the two must never co-locate the same player's
 * name and worn skin at the same stone/window). Subjects ROTATE across all active players (a chorus),
 * never the divergence extremes — so the group can't read WHICH player is honored/violated from who
 * gets carved.
 *
 * INV-14 (INV-COORD). The optional `coordEncode` back-pointer to the M1 teaching stone is a POINTER,
 * not an answer — the carve never asks the player to type a coordinate.
 *
 * PURE. No DB / clock / LLM. The carve TEXT is the shared rune alphabet (forge), not authored prose,
 * so there is nothing to fall back FROM — the determinism is the backstop. The run wrapper reads the
 * visited-sets + fires SignWriteBeat; this picks WHO and WHERE. name-where-never-been.selftest.ts
 * imports it with nothing.
 */

/** One active player's coarse visited-cell proof-set + their carve history. */
export interface PlayerPresence {
  groupKey: string;
  name: string | null;
  /**
   * the coarse cells this player has PROVABLY visited (LocationSampler → player_visited_cells).
   * `null` means we have NO proof-set yet — which BLOCKS a carve for them (we can't prove absence).
   */
  visitedCells: ReadonlySet<string> | null;
  /** how many times this player has already been carved (for chorus rotation — fewest-first). */
  carvedCount: number;
}

/** The candidate carve anchors (sites.yml `carve_anchor` cells), each with its coarse cell id. */
export interface CarveAnchor {
  siteId: string;
  cellId: string;
}

export interface CarveSelectorInput {
  /** active-only players (the caller filters; absent members never appear — the collective law). */
  activePlayers: PlayerPresence[];
  /** the available carve anchors. */
  anchors: CarveAnchor[];
  /**
   * the cells ANY active player has marked (the group's frequented set). A group-avoided cell is one
   * NOT in here. Built by the run wrapper as the union of all active visited-sets.
   */
  groupVisitedCells: ReadonlySet<string>;
  /** cells already used for a carve this arc (one carve per cell; don't re-carve a spot). */
  usedCells: ReadonlySet<string>;
}

/** The single carve to produce this movement, or null when no precision-safe pairing exists. */
export interface CarveDecision {
  groupKey: string;
  name: string;
  siteId: string;
  cellId: string;
  reason: string;
}

export interface CarveSelectorResult {
  carve: CarveDecision | null;
  /** human-readable trace (logged; never player-facing). */
  notes: string[];
}

/**
 * selectCarve — pure. Picks ONE (player, anchor) pairing that satisfies proof-of-absence, rotating
 * the subject across active players (fewest-carved first → a chorus, never the extremes). Returns
 * null (no carve this movement) rather than ever firing on an unproven absence.
 *
 * Selection (all deterministic):
 *   1. eligible anchors = group-avoided (cell ∉ groupVisitedCells) AND unused (cell ∉ usedCells).
 *   2. eligible players = named AND have a proof-set (visitedCells != null).
 *   3. for the subject, pick the active player with the FEWEST prior carves (ties → name asc), who
 *      has at least one eligible anchor they have provably never visited.
 *   4. for that player, pick their lowest-cellId eligible-and-never-visited anchor (stable).
 */
export function selectCarve(inp: CarveSelectorInput): CarveSelectorResult {
  const notes: string[] = [];

  const eligibleAnchors = inp.anchors.filter(
    (a) => !inp.groupVisitedCells.has(a.cellId) && !inp.usedCells.has(a.cellId),
  );
  if (eligibleAnchors.length === 0) {
    return { carve: null, notes: ['no group-avoided, unused anchor available this movement'] };
  }

  // Chorus rotation: fewest-carved first, then name asc — stable + spreads across the group.
  const candidates = [...inp.activePlayers]
    .filter((p) => {
      if (!p.name) { notes.push(`skipped ${p.groupKey}: no name`); return false; }
      if (p.visitedCells == null) { notes.push(`skipped ${p.name}: no visited-proof-set (cannot prove absence)`); return false; }
      return true;
    })
    .sort((a, b) => a.carvedCount - b.carvedCount || (a.name ?? '').localeCompare(b.name ?? ''));

  for (const p of candidates) {
    const visited = p.visitedCells!; // non-null by the filter above
    // anchors this player has PROVABLY never visited, lowest cell id first (stable).
    const neverVisited = eligibleAnchors
      .filter((a) => !visited.has(a.cellId))
      .sort((x, y) => x.cellId.localeCompare(y.cellId));
    if (neverVisited.length === 0) {
      notes.push(`${p.name}: every eligible anchor is one they've visited — no proof-safe carve`);
      continue;
    }
    const anchor = neverVisited[0]!;
    return {
      carve: {
        groupKey: p.groupKey,
        name: p.name!,
        siteId: anchor.siteId,
        cellId: anchor.cellId,
        reason: `proof-of-absence: ${p.name} provably never at ${anchor.cellId}, group-avoided, unused (carved ${p.carvedCount}x before)`,
      },
      notes,
    };
  }

  notes.push('no active player has a proof-safe anchor this movement — no carve (precision over recall)');
  return { carve: null, notes };
}
