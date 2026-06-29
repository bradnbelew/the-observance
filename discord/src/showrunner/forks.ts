/**
 * forks.ts — one-way fork application (A11 `exclusive-forks-permanence`, INV-12/13, WEB-MASTER §3.4).
 *
 * THREE mutually-exclusive, diegetically-irreversible forks color the M5 close. This is the pure
 * application policy: given the measured fork triggers + the already-set fork flags, it computes the
 * leaf flags to commit — FIRST-WRITER-WINS (a fork resolves once, permanently) and COLORS, NEVER
 * GATES (INV-12; seedcheck asserts no spine puzzle requires a fork flag).
 *
 * THE THREE FORKS (WEB-MASTER §3.4):
 *   A — Sacred Beast: the kill of the GLOWING beast (INV-13: only the glowing one is tracked + the
 *       only one that glows, so the irreversible fork is always fairly avoidable) → sacred_beast_broken.
 *       The boon leaf is the absence of the break (no kill).
 *   B — First Light: an M3 puzzle CHOICE (two plaintexts) → light_kept | light_taken.
 *   C — Spoken Name: an M4 puzzle (carve Iss's name vs withhold) → name_unspoken | name_spoken.
 *
 * ONE-WAY / FIRST-WRITER-WINS (anti-jank, permanence). Once a fork's flag is set it NEVER flips — a
 * later opposite trigger is ignored. This makes the permanence real and the apply idempotent: a
 * re-run commits nothing new. INV-13's precision guard: the cosmetic Pale (`pale_cosmetic` PDC) are
 * IGNORED for conduct — only a GLOWING-beast kill arms Fork A, so a herd-conversion Pale can never
 * trip the irreversible fork.
 *
 * PURE. No DB / network / LLM. The leaf LINES are authored voice keys the M5 composer reads (de-slop
 * notes B2 applied at authoring) — there is no language here. forks.run.ts reads triggers + writes
 * arc_state.flags; forks.selftest.ts imports this with nothing.
 */

/** The leaf flags each fork can commit (the M5 composer reads these; they never gate). */
export interface ForkFlags {
  sacred_beast_broken?: boolean;
  light_kept?: boolean;
  light_taken?: boolean;
  name_unspoken?: boolean;
  name_spoken?: boolean;
}

/** The measured triggers this tick (set by listeners / puzzle solves; null = no signal yet). */
export interface ForkTriggers {
  /** a GLOWING sacred beast was killed (INV-13: pale_cosmetic kills are NOT this — never set here). */
  glowingBeastKilled?: boolean;
  /** the First Light choice resolved: 'kept' | 'taken' | null. */
  firstLightChoice?: 'kept' | 'taken' | null;
  /** the Spoken Name choice resolved: 'spoken' | 'unspoken' | null. */
  spokenNameChoice?: 'spoken' | 'unspoken' | null;
}

export interface ForkApplyResult {
  /** the leaf flags to MERGE into arc_state.flags (only newly-decided leaves; empty if nothing new). */
  setFlags: ForkFlags;
  notes: string[];
}

/** True once EITHER leaf of a binary fork is set (so the opposite trigger is ignored — one-way). */
function settled(a: boolean | undefined, b: boolean | undefined): boolean {
  return a === true || b === true;
}

/**
 * applyForks — pure, first-writer-wins. For each fork, if it is NOT already settled and its trigger
 * fired, commit the matching leaf. An already-settled fork ignores any new trigger (permanence).
 * Same input → same output; re-running with the leaf already present commits nothing.
 */
export function applyForks(prior: ForkFlags, triggers: ForkTriggers): ForkApplyResult {
  const setFlags: ForkFlags = {};
  const notes: string[] = [];

  // Fork A — Sacred Beast. Only a GLOWING-beast kill arms it (INV-13). The boon leaf is the absence
  // of a break, so there is nothing to commit for "kept" — only the transgressor leaf is a flag.
  if (prior.sacred_beast_broken === true) {
    notes.push('fork A already settled (sacred_beast_broken) — ignoring');
  } else if (triggers.glowingBeastKilled === true) {
    setFlags.sacred_beast_broken = true;
    notes.push('fork A: glowing sacred beast killed → sacred_beast_broken (one-way)');
  }

  // Fork B — First Light (binary, one leaf each).
  if (settled(prior.light_kept, prior.light_taken)) {
    notes.push('fork B already settled (first light) — ignoring');
  } else if (triggers.firstLightChoice === 'kept') {
    setFlags.light_kept = true;
    notes.push('fork B: first light kept → light_kept (one-way)');
  } else if (triggers.firstLightChoice === 'taken') {
    setFlags.light_taken = true;
    notes.push('fork B: first light taken → light_taken (one-way)');
  }

  // Fork C — Spoken Name (binary, one leaf each).
  if (settled(prior.name_unspoken, prior.name_spoken)) {
    notes.push('fork C already settled (spoken name) — ignoring');
  } else if (triggers.spokenNameChoice === 'unspoken') {
    setFlags.name_unspoken = true;
    notes.push('fork C: name withheld → name_unspoken (one-way)');
  } else if (triggers.spokenNameChoice === 'spoken') {
    setFlags.name_spoken = true;
    notes.push('fork C: name carved → name_spoken (one-way)');
  }

  return { setFlags, notes };
}
