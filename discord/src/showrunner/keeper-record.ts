/**
 * keeper-record.ts — "the record writes you in" (A3 UNIFIED Hold-Book, WEB-MASTER §4, FACT 9/12/14).
 *
 * THE GAP THIS CLOSES. The Hold-Book starts (M1) as the Archivist's flat living-habit rows. Over the
 * arc the book must (M3) move those rows UNDER keeper headings, then (M4) have the keeper's OWN hand
 * write the living player, then (M5) rewrite to "the present hands are entered." This module is the
 * pure producer of the page rows for each living player — the per-player keeper-voice MAPPING from a
 * measured dossier into Lectern page-swap / BookAppears producer rows.
 *
 * THE LLM SCALPEL + ITS DETERMINISTIC FALLBACK (anti-jank law). The M4 keeper-hand page may be
 * authored by a rare text-only LLM call (the keeper's voice, conditioned on the player's dossier),
 * BUT every page has a deterministic fallback behind it: an AUTHORED VOICE KEY (`keeperPage*`) is
 * always chosen first as the page's text source, and the LLM is offered ONLY a constrained
 * substitution slot it may decline. If the model is slow/offline/returns junk, the authored key
 * stands unchanged. Nothing here ever blocks on the model; the page row is fully formed without it.
 *
 * PRECISION FLOOR (INV-16 / the grounding contract). A player is enrolled to a keeper ONLY when the
 * dossier shows a measured behavior that RHYMES with that keeper to a confident margin. A FLAT player
 * (no dominant signal) is enrolled to NO ONE — the book leaves them an un-headed living row, never a
 * guessed heading. And the heading rhymes on a behavior every active player exhibits to SOME degree
 * (a chorus), never spotlighting the divergence extremes (INV-16: the group can't read WHICH player
 * is honored/violated from the book).
 *
 * IDEMPOTENT HIGH-WATER (mirrors customs.ts). A per-player enrolment tier high-water mark, so the
 * book only re-fires a page when the player crosses to a NEW tier (living-row → keeper-heading →
 * keeper-hand), never re-writing the same page every cadence. Out-of-LoS + idempotent (apply/plugin).
 *
 * THE INV NOTE (WEB-MASTER §4). The keeper-enrolment tally is a NEUTRAL COLORANT — it never elects a
 * "chosen one", never gates anything. It is texture, counted, not scored.
 *
 * PURE. No DB / network / clock / LLM side effect in THIS file (the optional author fn is injected,
 * and ALWAYS has a deterministic result before it is offered the slot) — keeper-record.selftest.ts
 * imports it with nothing. The I/O wrapper (read dossiers, fire LecternFillBeat, persist marks) is
 * keeper-record.run.ts.
 */

/** The six keepers a living player can be enrolled under (fall-order; WEB-MASTER §6). */
export type KeeperId = 'vaun' | 'mara' | 'sella' | 'orin' | 'brann' | 'iss';

/** The enrolment tier a player has reached in the book — the M1→M5 face, in order. */
export type EnrolmentTier =
  | 'living_row' // M1: a flat Archivist living-habit row, no heading yet.
  | 'keeper_heading' // M3: the row moved under a keeper heading (the first "oh—").
  | 'keeper_hand'; // M4: the keeper's own hand writes the living player.

const TIER_ORDER: Readonly<Record<EnrolmentTier, number>> = {
  living_row: 1,
  keeper_heading: 2,
  keeper_hand: 3,
};

/**
 * One player's measured dossier, reduced to the signals that rhyme with a keeper. Each is a
 * group-relative score in 0..1 (the caller normalizes against the active group, so "dominant" means
 * dominant FOR THIS GROUP — a chorus behavior, not an absolute). A flat dossier (all near the mean)
 * yields no enrolment.
 */
export interface PlayerDossier {
  /** stable grouping key (mc_uuid when present) — the producer-row + high-water key. */
  groupKey: string;
  /** resolvable display name, or null. A nameless player is never written (precision). */
  name: string | null;
  /** 0..1 group-relative scores. The keeper whose score is dominant (by margin) claims the player. */
  signals: Readonly<Partial<Record<KeeperId, number>>>;
}

/** Tunable precision constants (injected; keeps the policy pure + testable). */
export interface KeeperRecordConstants {
  /** the leader's score must reach at least this to enrol at all (no faint signal enrols). */
  minLeadScore: number;
  /** the leader must beat the runner-up by at least this margin (a real dominance, not a tie). */
  minLeadMargin: number;
}

export const KEEPER_RECORD_DEFAULTS: KeeperRecordConstants = {
  minLeadScore: 0.45,
  minLeadMargin: 0.15,
};

/**
 * One producer row the book should write for a player this tick. Carries the chosen AUTHORED voice
 * key (the deterministic page text source) + the structured slot the optional LLM scalpel may fill;
 * the run wrapper turns this into a LecternFillBeat. NO English is composed here.
 */
export interface KeeperPageRow {
  groupKey: string;
  name: string;
  /** null at living_row (un-headed); the enrolling keeper at heading/hand tiers. */
  keeper: KeeperId | null;
  tier: EnrolmentTier;
  /** the authored voice key that ALWAYS produces this page's text (the deterministic fallback). */
  voiceKey: string;
  /**
   * the constrained substitution slot the optional LLM scalpel MAY fill (e.g. one clause of the
   * keeper's hand). Always has a deterministic value here; the model may only replace it in-register
   * or decline. Null when the tier has no authored slot.
   */
  authorSlot: KeeperAuthorSlot | null;
}

/** The bounded ask handed to the optional author fn. It returns a replacement clause or declines. */
export interface KeeperAuthorSlot {
  keeper: KeeperId;
  /** the deterministic clause that stands if the model is slow/offline/junk. */
  fallbackClause: string;
  /** the grammatical fingerprint the model must honor (WEB-MASTER §6) — passed for the prompt only. */
  fingerprint: string;
}

export interface KeeperRecordDecision {
  rows: KeeperPageRow[];
  /** per-player new high-water tier ordinal, MERGED into state only for rows that fired. */
  marks: Record<string, number>;
  /** human-readable trace (logged; never player-facing). */
  notes: string[];
}

/**
 * The authored page voice keys per tier. Living rows are the flat Archivist register (no keeper);
 * heading/hand keys are per-keeper so each obeys its grammatical fingerprint (TS-VOICE inserts the
 * bodies). These are KEYS, not text — voice.ts is the sole text source.
 */
const LIVING_ROW_KEY = 'keeperPageLiving';
function headingKey(k: KeeperId): string {
  return `keeperPageHeading_${k}`;
}
function handKey(k: KeeperId): string {
  return `keeperPageHand_${k}`;
}

/** The grammatical fingerprint per keeper (WEB-MASTER §6) — handed to the author slot as a constraint. */
const FINGERPRINT: Readonly<Record<KeeperId, string>> = {
  vaun: 'accumulates and will not release; clauses keep adding; the possessive recurs',
  mara: 'referential and deferred; page/line citations; "i read that…"',
  sella: 'mirrored and receding; folds back spatially (shore/water/reflection); child-adjacent diction',
  orin: 'breaks off and will not finish; incomplete strokes; sentences that stop ("i —")',
  brann: 'repeats and over-corrects; says things twice; counts and re-counts',
  iss: 'warm, plain, confident; the only keeper who reassures; frames, never counts',
};

/** Pick the dominant keeper for a dossier, or null if the signal is flat (precision floor). */
function dominantKeeper(d: PlayerDossier, k: KeeperRecordConstants): KeeperId | null {
  const entries = (Object.entries(d.signals) as [KeeperId, number][])
    .filter(([, v]) => Number.isFinite(v))
    .sort((a, b) => b[1] - a[1]);
  if (entries.length === 0) return null;
  const [topId, topScore] = entries[0]!;
  const runnerUp = entries[1]?.[1] ?? 0;
  if (topScore < k.minLeadScore) return null; // too faint to enrol anyone
  if (topScore - runnerUp < k.minLeadMargin) return null; // a tie → no confident rhyme
  return topId;
}

/**
 * decideKeeperEnrolment — the pure Hold-Book producer. For each dossier it computes the tier the
 * player has earned, and if that tier is ABOVE the player's high-water mark, emits one page row with
 * its authored voice key (+ optional author slot). A flat or nameless dossier never produces a keeper
 * row. Same input → same output.
 *
 * @param dossiers   active-only player dossiers (the caller filters to active; absent members never appear).
 * @param tierFor    the tier each player has reached this arc (from arc movement / iss_caught state),
 *                   keyed by groupKey. A player not present is treated as living_row.
 * @param reported   per-player high-water tier ordinal already written (idempotency).
 */
export function decideKeeperEnrolment(
  dossiers: PlayerDossier[],
  tierFor: Record<string, EnrolmentTier>,
  reported: Record<string, number>,
  k: KeeperRecordConstants = KEEPER_RECORD_DEFAULTS,
): KeeperRecordDecision {
  const rows: KeeperPageRow[] = [];
  const marks: Record<string, number> = {};
  const notes: string[] = [];

  for (const d of dossiers) {
    if (!d.name) {
      notes.push(`skipped ${d.groupKey}: no name (precision floor)`);
      continue; // never write a nameless player (the grounding contract)
    }

    const tier: EnrolmentTier = tierFor[d.groupKey] ?? 'living_row';
    const tierOrd = TIER_ORDER[tier];
    const already = reported[d.groupKey] ?? 0;
    if (tierOrd <= already) continue; // idempotent: only fire a NEW tier crossing

    // At heading/hand tiers a keeper is required; a flat dossier can't be enrolled, so it stays a
    // living row (it never invents a heading — precision floor / INV-16 chorus rule).
    const keeper = tier === 'living_row' ? null : dominantKeeper(d, k);
    if (tier !== 'living_row' && keeper == null) {
      notes.push(`held ${d.name} at living_row: flat dossier, enrolled to no one`);
      // Emit the living_row page if not yet written, so the player still appears in the book.
      if (already < TIER_ORDER.living_row) {
        rows.push({ groupKey: d.groupKey, name: d.name, keeper: null, tier: 'living_row', voiceKey: LIVING_ROW_KEY, authorSlot: null });
        marks[d.groupKey] = TIER_ORDER.living_row;
      }
      continue;
    }

    let voiceKey: string;
    let authorSlot: KeeperAuthorSlot | null = null;
    if (tier === 'living_row' || keeper == null) {
      voiceKey = LIVING_ROW_KEY;
    } else if (tier === 'keeper_heading') {
      voiceKey = headingKey(keeper);
    } else {
      voiceKey = handKey(keeper);
      // Only the keeper_hand tier offers the constrained author slot; everything else is fixed text.
      authorSlot = {
        keeper,
        fallbackClause: `${handKey(keeper)}.clause`, // a stable key the run wrapper resolves to authored text
        fingerprint: FINGERPRINT[keeper],
      };
    }

    rows.push({ groupKey: d.groupKey, name: d.name, keeper, tier, voiceKey, authorSlot });
    marks[d.groupKey] = tierOrd;
  }

  // Deterministic order (by groupKey) so a tick's pages are stable + testable.
  rows.sort((a, b) => a.groupKey.localeCompare(b.groupKey));
  return { rows, marks, notes };
}

/**
 * applyAuthorSlot — the SINGLE chokepoint where the optional LLM scalpel may touch a page, with its
 * deterministic fallback inline. Given a row + an optional author fn, returns the final clause: the
 * model's text IF it returned a non-empty in-register string, else the authored fallback. The author
 * fn is `(slot) => string | null | Promise<...>`; ANY throw/empty/null → fallback. Never blocks the
 * book: the row already carries a complete fallback before this is ever called.
 *
 * This is pure given a SYNCHRONOUS author fn (for the self-test); the run wrapper passes the real
 * async scalpel and awaits it with a timeout. The fallback path is what the self-test pins.
 */
export function resolveAuthorClause(
  row: KeeperPageRow,
  authored: (slot: KeeperAuthorSlot) => string | null,
): string {
  if (!row.authorSlot) return row.voiceKey; // no slot → the authored page key stands as-is
  try {
    const out = authored(row.authorSlot);
    if (typeof out === 'string' && out.trim().length > 0) return out;
  } catch {
    /* fall through to fallback */
  }
  return row.authorSlot.fallbackClause;
}
