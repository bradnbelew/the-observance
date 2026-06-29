/**
 * grave.ts — the future-dated grave producer (A9 `future-dated-grave`, FACT 13b, INV-14).
 *
 * "the stone is cut before the keeper is kept." One grave (not a graveyard) carved with a living
 * player's name and a FUTURE date that reads as a death clock — the misread IS the mechanic. The
 * date == the SINGLE Accepting instant (WEB-MASTER §0.4/§1.M5): no idea owns the instant, the
 * showrunner does; the grave, the Record website timestamp, and the summons `not_before` all share it.
 * At V the grave opens FROM THE INSIDE on its date: KEPT — NOT YET → KEPT. The death clock was an
 * appointment.
 *
 * GROUNDED ACTIVE NAME (the grounding contract / precision). The grave names a real, resolvable,
 * ACTIVE player — never a guessed or absent one. A nameless or inactive subject → no grave.
 *
 * INV-14 (INV-COORD). The carved date is READ, never typed as an answer. The producer emits the date
 * as carved glyphs (rune + digit), not a puzzle input.
 *
 * THE TWO-ROW IDEMPOTENT TRANSACTION (anti-jank). A grave is carved ONCE (row A: the future-dated
 * headstone) and opened ONCE (row B: the rewrite at the Accepting instant). A per-arc high-water of
 * which rows have fired keeps it idempotent — a restart mid-tick re-derives the same single grave,
 * never a second one.
 *
 * PURE. No DB / network / LLM; the carve is glyphs (forge) + an authored voice key, so determinism is
 * the backstop. The single Accepting instant is INJECTED (the showrunner owns it). grave.run.ts does
 * the I/O (pick the active name, read the instant, fire SignWriteBeat/LecternFillBeat, persist marks).
 * grave.selftest.ts imports this with nothing.
 */

/** Which of the grave's two one-shot rows this producer should emit this tick. */
export type GraveRowKind = 'carve' | 'open';

/** A candidate subject: a resolvable, active player. */
export interface GraveCandidate {
  groupKey: string;
  name: string | null;
  active: boolean;
}

export interface GraveInput {
  /** the single Accepting instant (epoch ms), shared with the website + summons. Null until bound. */
  acceptingInstantMs: number | null;
  /** now (epoch ms) — to decide whether the open row is due (date reached). */
  nowMs: number;
  /** the chosen subject (the run wrapper grounds + stabilizes this; one grave per arc). */
  subject: GraveCandidate | null;
  /** has the carve row already fired? (idempotency). */
  carved: boolean;
  /** has the open row already fired? (idempotency). */
  opened: boolean;
}

/** One grave row to write this tick (carve OR open), or none. */
export interface GraveRow {
  kind: GraveRowKind;
  groupKey: string;
  name: string;
  /** the carved date == the Accepting instant (epoch ms). Emitted as glyphs by the beat, read never typed. */
  dateMs: number;
  /** the authored voice key for this row (`graveCarved` / `graveOpened`). A KEY, never composed text. */
  voiceKey: 'graveCarved' | 'graveOpened';
  reason: string;
}

export interface GraveDecision {
  row: GraveRow | null;
  /** marks to merge into state (e.g. { carved: true } / { opened: true }) only when a row fired. */
  marks: Partial<{ carved: boolean; opened: boolean }>;
  notes: string[];
}

/**
 * decideGrave — pure. At most ONE row per tick, carve before open, each once:
 *   - carve: the Accepting instant is bound + a grounded active subject exists + not yet carved.
 *   - open:  already carved + now >= the Accepting instant + not yet opened.
 * Returns no row (with a note) when nothing is due or the subject is ungrounded.
 */
export function decideGrave(inp: GraveInput): GraveDecision {
  const notes: string[] = [];

  if (inp.acceptingInstantMs == null) {
    return { row: null, marks: {}, notes: ['accepting instant not bound yet — no grave'] };
  }

  // OPEN takes precedence once due (the appointment has come) — but only after a carve exists.
  if (inp.carved && !inp.opened && inp.nowMs >= inp.acceptingInstantMs) {
    // The open row re-reads the SAME subject the carve used; the run wrapper passes it through.
    if (!inp.subject?.name) {
      return { row: null, marks: {}, notes: ['open due but carved subject name unresolved — defer'] };
    }
    return {
      row: {
        kind: 'open',
        groupKey: inp.subject.groupKey,
        name: inp.subject.name,
        dateMs: inp.acceptingInstantMs,
        voiceKey: 'graveOpened',
        reason: 'accepting instant reached — the grave opens from the inside (KEPT)',
      },
      marks: { opened: true },
      notes,
    };
  }

  // CARVE: one grounded active subject, once.
  if (!inp.carved) {
    if (!inp.subject || !inp.subject.active || !inp.subject.name) {
      return { row: null, marks: {}, notes: ['no grounded active subject for the grave — never guess one'] };
    }
    return {
      row: {
        kind: 'carve',
        groupKey: inp.subject.groupKey,
        name: inp.subject.name,
        dateMs: inp.acceptingInstantMs,
        voiceKey: 'graveCarved',
        reason: `carve the one grave: ${inp.subject.name}, dated the accepting instant (read, never typed)`,
      },
      marks: { carved: true },
      notes,
    };
  }

  notes.push(inp.opened ? 'grave already carved + opened — done' : 'grave carved; open not yet due');
  return { row: null, marks: {}, notes };
}
