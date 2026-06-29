// record-projection.ts — the pure, spoiler-free projection behind The Record (A13 `arg-leaves-the-game`).
//
// THE GAP THIS CLOSES. The Record (/record) is a PUBLIC, anon-served keepers' archive that "reads the
// Supabase state read-only ... updates with progress" (BUILD-MANIFEST §7, INTEGRATION-V2 §A13). The
// security model (dashboard 0001_init / 0003_lockdown) is absolute: anon can read ONLY the SECURITY
// DEFINER spoiler-free views — never a raw table, never a sealed flag, never a player name or custom
// label. So The Record can show only what the *in-world record* would show: a count of entries, a
// coarse season, and whether the keeping has closed. Everything else is a redaction.
//
// This module is that mapping, and ONLY that mapping. It is PURE + DETERMINISTIC (no DB, no clock, no
// LLM — there is no language to author; the redactions ARE the artifact), so the route can render it on
// the server with no client JS and a `.selftest` can pin every redaction. The route does the I/O (reads
// the neutral view); this decides what the keepers are allowed to have kept of it.
//
// THE LAWS THIS HONORS:
//   - CROSS-SURFACE TRUTH: the Record never contradicts Minecraft/Discord. It shows a *subset* of the
//     same truth in the same cold keeper register (lowercase, declarative, counts and stops — WEB-MASTER
//     §6 Set-B). It never claims a state the game has not reached.
//   - PRIVACY / PRECISION over recall: it personalizes NOTHING. No "it knows you" line lives here — the
//     Record measures only the group's coarse progress, so it can only ever speak of the group.
//   - REVEAL DISCIPLINE: entries un-redact in lockstep with progress (a stone read → an entry legible),
//     never ahead of it. A redacted line is the omission/iceberg; nothing is announced before earned.
//   - ANTI-JANK: total + side-effect-free; an absent/zero input degrades to the fully-sealed baseline
//     (the archive a fresh world shows), never to an error or a leaked default.

/**
 * The neutral, anon-readable shape the route passes in. Every field is a COARSE public signal — the
 * same things the spoiler-free views already expose (a max movement, a count, a closed-flag), NEVER a
 * sealed flag, a player, or a custom label. The SQL lane owns the `v_record` view that produces this
 * (see RETURN); until it lands the route synthesizes it from the existing neutral views, so the contract
 * is the small, deliberately-blunt set below and nothing wider.
 */
export interface RecordSignal {
  /** the highest movement the group has reached (1..5), coarse season only. 0/absent = not begun. */
  movement?: number | null;
  /** how many of the six keeper-stones have been read (0..6). drives entry legibility, in lockstep. */
  stonesRead?: number | null;
  /** the keeping has closed (the Accepting resolved). the ONLY arc-end signal the Record may know. */
  accepted?: boolean | null;
}

/** A single archive entry. `legible` lines are shown plain; redacted lines render as a struck block. */
export interface RecordEntry {
  /** stable key for React + the selftest. never shown. */
  id: string;
  /** the cold keeper line, shown only when `legible`. lowercase, declarative (Set-B register). */
  line: string;
  /** whether the keeping has reached this entry. false → the line is withheld (a redaction). */
  legible: boolean;
}

/** The whole projection the route renders. Nothing here is sealed; everything is a coarse public fact. */
export interface RecordProjection {
  /** the count of entries already kept (legible). the Record's one number — a muster, not a clock. */
  kept: number;
  /** the total entries in the archive (legible + withheld). */
  total: number;
  /** the coarse season label, cold register. */
  season: string;
  /** the keeping has closed. */
  closed: boolean;
  /** the ordered archive — earned entries legible, the rest withheld. */
  entries: RecordEntry[];
  /** the standing footer line (always shown; states the count + that the rest is withheld). */
  footer: string;
}

/** Clamp an untrusted coarse signal to its valid band (defends against a malformed/early view row). */
function clampInt(v: number | null | undefined, lo: number, hi: number): number {
  const n = Math.trunc(Number(v));
  if (!Number.isFinite(n)) return lo;
  return Math.max(lo, Math.min(hi, n));
}

/**
 * The fixed archive spine. Six keeper-stone entries (un-redact one-per-stone-read, REVEAL DISCIPLINE),
 * then the closing entry (un-redacts only on `accepted`). The lines are the cold record register — they
 * state what is kept and stop; no warmth, no second person, no named feeling (WEB-MASTER §6 Set-B). They
 * are a deliberate SUBSET of the in-world record, carrying nothing the game has not already shown.
 *
 * NB: these are the keepers' OWN dead names (canon, not the living players) — the Record files the dead
 * by place; it never files a living player here (that lives in-world only; INV-16 / the privacy law).
 */
const STONE_ENTRIES: ReadonlyArray<{ id: string; line: string }> = [
  { id: 'stone-1', line: 'the first was kept. the offering was not made.' },
  { id: 'stone-2', line: 'the second was kept. the light was read too long.' },
  { id: 'stone-3', line: 'the third was kept. the far water kept her.' },
  { id: 'stone-4', line: 'the fourth was kept. the threshold was not crossed.' },
  { id: 'stone-5', line: 'the fifth was kept. the black moon was slept through.' },
  { id: 'stone-6', line: 'the sixth was kept. the name was spoken.' },
];

const CLOSING_ENTRY = {
  id: 'closing',
  line: 'the present hands are entered. the count holds.',
} as const;

const SEASONS: Record<number, string> = {
  0: 'the record is not yet opened.',
  1: 'the notice.',
  2: 'the ways.',
  3: 'the descent.',
  4: 'the catch.',
  5: 'the accepting.',
};

/** The withheld-line glyph block render hint — a struck count, never the hidden text. */
export const REDACTED_GLYPH = '████████';

/**
 * project — the pure mapping. Given a coarse public signal, return the redacted archive. Total +
 * deterministic: same signal → same archive. An absent/zero signal degrades to the sealed baseline
 * (an opened-but-empty record), never an error.
 */
export function project(signal: RecordSignal): RecordProjection {
  const movement = clampInt(signal.movement, 0, 5);
  const stonesRead = clampInt(signal.stonesRead, 0, STONE_ENTRIES.length);
  const closed = signal.accepted === true;

  const entries: RecordEntry[] = STONE_ENTRIES.map((e, i) => ({
    id: e.id,
    line: e.line,
    // REVEAL DISCIPLINE: an entry is legible only once its stone has actually been read — never ahead.
    legible: i < stonesRead,
  }));

  // The closing entry is withheld until the keeping has closed (the one arc-end signal we may know).
  entries.push({ ...CLOSING_ENTRY, legible: closed });

  const kept = entries.filter((e) => e.legible).length;
  const total = entries.length;
  const season = SEASONS[closed ? 5 : movement] ?? SEASONS[0];

  // The footer is the Record's one self-statement: a count, then the iceberg. Cold register, no warmth.
  const footer = closed
    ? `${kept} of ${total} kept. the record is closed.`
    : `${kept} of ${total} kept. the rest is not yet kept.`;

  return { kept, total, season, closed, entries, footer };
}
