/**
 * reports.ts — the personalized observation producer (D1 `backlog-full-showrunner`, the "it knows me"
 * daily report) with a DETERMINISTIC VOICE FALLBACK behind every LLM call.
 *
 * THE GAP THIS CLOSES. The customs bridge (customs.ts) reports a CONSEQUENCE — a crossed rung on a
 * broken way. This is the other half the showrunner keystone owes: the un-prompted personalized
 * OBSERVATION — the land noting a real, measured habit of a named player ("the one called brann mines
 * alone"), the texture that makes the haunting feel like it knows them. It is NOT a toll and NOT a
 * gate; it is a colorant line dripped on the cadence.
 *
 * THE LLM SCALPEL + ITS DETERMINISTIC FALLBACK (anti-jank law, the SPOF mitigation). The observation
 * MAY be sharpened by a rare text-only LLM call (the Watcher's register, conditioned on the dossier),
 * BUT every report carries a complete authored line BEFORE the model is ever offered the slot:
 * `voice.reportObserved(name, honored, customPhrase)` is the deterministic floor, always chosen first.
 * The model is handed only a constrained substitution it MAY decline; if it is slow / offline / returns
 * junk / breaks register, the authored line stands byte-identical. Nothing here ever BLOCKS on the
 * model — the row is fully formed without it. This file is the PURE policy + the single fallback
 * chokepoint; the async scalpel + its timeout live in the run wrapper.
 *
 * PRECISION OVER RECALL (the privacy law). A report names a player ONLY on a signal the tracker
 * actually MEASURED to a confident margin — a dominant habit, not a faint one, not a tie. A flat
 * dossier (no dominant habit) yields NO report — a wrong "it knows you" is worse than none. And the
 * habit it names is one every active player exhibits to SOME degree (a chorus axis), never a divergence
 * extreme (INV-16: the group can't read WHICH player is honored/violated from a report line).
 *
 * IDEMPOTENT (mirrors customs.ts / keeper-record.ts). A per-player high-water mark of the dominant
 * habit already reported, so the same habit is never re-dripped every cadence; a new report fires only
 * when the dominant habit CHANGES (the player's behavior shifted) — keyed on the habit, not a count, so
 * a steadily-mining player is observed once, not nightly.
 *
 * AUTO ⇄ CONFIRM (the autonomy dial). A personalized report is CURATORIAL (it speaks in the Watcher's
 * voice about a real player) — in AUTO it drips live; in CONFIRM it is staged for dashboard approval.
 * This module returns the report rows + the staged/live intent; the run wrapper posts or stages.
 *
 * PURE. No DB / network / clock / LLM side effect in THIS file (the optional author fn is injected and
 * ALWAYS has a deterministic result before it is offered the slot). reports.selftest.ts imports it with
 * nothing. The I/O wrapper (read dossiers, call the scalpel with a timeout, post/stage, persist marks)
 * is reports.run.ts / the showrunner authoring loop.
 */
import { voice, customPhrase } from '../voice.js';
import type { Tone } from './types.js';

/**
 * The measured habit axes a player can be dominant on. Each is a CHORUS axis — every active player
 * exhibits it to some degree; the report names the player who is dominant FOR THIS GROUP (INV-16). The
 * `customKey` maps to a `customPhrase(...)` so the deterministic line reads in the keeper register.
 */
export type HabitAxis =
  | 'hoards' // Vaun rhyme: solo-mining / hoarding (custom: offering)
  | 'reads' // Mara rhyme: idle at lecterns, low blocks-broken (custom: kept-light)
  | 'wanders' // Sella rhyme: high distance-from-group (custom: bow / markers)
  | 'silent' // Orin rhyme: passes markers uncrouched (custom: bow)
  | 'night-walks' // Brann rhyme: active on the black moon (custom: dark-hours)
  | 'spends-words'; // Iss rhyme: leans on whispers (custom: unspoken)

/** The custom phrase key each habit axis names in the deterministic line (the chorus framing). */
const HABIT_CUSTOM: Readonly<Record<HabitAxis, string>> = {
  hoards: 'the_offering',
  reads: 'the_kept_light',
  wanders: 'the_bow',
  silent: 'the_bow',
  'night-walks': 'the_dark_hours',
  'spends-words': 'the_unspoken',
};

/** A short ordinal so the high-water can be compared (the habit-changed test is on the axis, not this). */
const HABIT_ORD: Readonly<Record<HabitAxis, number>> = {
  hoards: 1, reads: 2, wanders: 3, silent: 4, 'night-walks': 5, 'spends-words': 6,
};

/**
 * One player's dossier reduced to the chorus-habit scores. Each is a 0..1 GROUP-RELATIVE score (the
 * caller normalizes against the active group, so "dominant" means dominant for this group). A flat
 * dossier (all near the mean) yields no report.
 */
export interface ObservationDossier {
  groupKey: string;
  /** resolvable display name, or null. A nameless player is never reported (precision). */
  name: string | null;
  /** measured honored count on the named habit's custom — the grounded "days kept" the line cites. */
  honoredCount: number;
  /** 0..1 group-relative habit scores. The dominant axis (by margin) is what the report names. */
  habits: Readonly<Partial<Record<HabitAxis, number>>>;
}

/** Tunable precision constants (injected; keeps the policy pure + testable). */
export interface ReportConstants {
  /** the dominant habit must reach at least this to be named at all (no faint signal). */
  minDominant: number;
  /** the dominant must beat the runner-up by at least this margin (a real habit, not a tie). */
  minMargin: number;
}

export const REPORT_DEFAULTS: ReportConstants = {
  minDominant: 0.45,
  minMargin: 0.15,
};

/**
 * The constrained substitution slot the optional LLM scalpel MAY fill — exactly one clause of the
 * observation, in the Watcher register, about the named habit. ALWAYS has a deterministic fallback.
 */
export interface ReportAuthorSlot {
  name: string;
  habit: HabitAxis;
  /** the deterministic line that stands if the model is slow/offline/junk (the floor). */
  fallbackLine: string;
  /** the register constraint passed to the prompt only (never composed here). */
  register: string;
}

/** One personalized report to drip this tick. Carries the deterministic line + the optional slot. */
export interface ObservationReport {
  groupKey: string;
  name: string;
  habit: HabitAxis;
  /** the deterministic, authored line (voice.reportObserved) — ALWAYS present, the fallback floor. */
  line: string;
  /** the constrained author slot the scalpel may fill (always has a fallback); null is never returned here. */
  authorSlot: ReportAuthorSlot;
  /** the register temperature this tick (A10) — a selection among authored variants, carried through. */
  tone: Tone;
  /** AUTO → drip live; CONFIRM → stage for approval. Mirrors the customs/drip autonomy dial. */
  staged: boolean;
  reason: string;
}

export interface ReportDecision {
  reports: ObservationReport[];
  /** per-player new dominant-habit ordinal high-water, MERGED into state only for rows that fired. */
  marks: Record<string, number>;
  /** human-readable trace (logged; never player-facing). */
  notes: string[];
}

/**
 * The post-reckoning sharp-quote shift (the-companion.md §7 T1). The personalized "it knows me"
 * observation IS the sharp NAMED-quote lane the companion (Wren) is the in-fiction channel for. When
 * his reckoning resolves, that lane must change:
 *   - 'condemn' | 'free' → Wren (the harvest channel) is taken/let-go, so the harvest CLOSES: the sharp
 *     NAMED callouts go QUIET (this producer emits no reports). The ambient Tier-0 land-behavior lines
 *     (customs.ts / the drip) are a DIFFERENT lane and are never gated here.
 *   - 'understand' → he stays kept-in-part, still the channel, so the quotes PERSIST but READ
 *     DIFFERENTLY (the kept-true framing — mercy as accuracy).
 *   - null (pre-reckoning, or no reckoning) → the sharp lane is unchanged (the back-compat default).
 */
export type ReckoningShift = 'condemn' | 'understand' | 'free' | null;

/** Immutable input. `reported` is the per-player dominant-habit ordinal already dripped (idempotency). */
export interface ReportInput {
  dossiers: ObservationDossier[];
  /** groupKey → the HABIT_ORD of the habit already reported for them (idempotency: re-fire only on change). */
  reported: Record<string, number>;
  /** AUTO → live; CONFIRM → staged. */
  mode: 'auto' | 'confirm';
  /** OPTIONAL difficulty register (A10) — carried onto each report; never changes WHO is reported. */
  tone?: Tone;
  /**
   * OPTIONAL post-reckoning sharp-quote shift (§7 T1). Absent/null ⇒ the lane is unchanged (the
   * back-compat default the self-test pins). condemn/free ⇒ the sharp lane goes quiet (no reports);
   * understand ⇒ the same grounded facts, re-framed as kept-true.
   */
  reckoningShift?: ReckoningShift;
}

/** The dominant habit for a dossier, or null if flat/tied (precision floor). */
function dominantHabit(d: ObservationDossier, k: ReportConstants): HabitAxis | null {
  const entries = (Object.entries(d.habits) as [HabitAxis, number][])
    .filter(([, v]) => Number.isFinite(v))
    .sort((a, b) => b[1] - a[1]);
  if (entries.length === 0) return null;
  const [topAxis, top] = entries[0]!;
  const runnerUp = entries[1]?.[1] ?? 0;
  if (top < k.minDominant) return null; // too faint to name anyone
  if (top - runnerUp < k.minMargin) return null; // a tie → no confident habit
  return topAxis;
}

/**
 * decidePersonalizedReports — the pure observation policy. For each dossier: find the dominant chorus
 * habit (precision-gated), and if it DIFFERS from the habit already reported for that player, emit one
 * report carrying the deterministic `voice.reportObserved` line + the constrained author slot. A flat
 * or nameless dossier never produces a report. Same input → same output.
 */
export function decidePersonalizedReports(
  input: ReportInput,
  k: ReportConstants = REPORT_DEFAULTS,
): ReportDecision {
  const reports: ObservationReport[] = [];
  const marks: Record<string, number> = {};
  const notes: string[] = [];
  const tone: Tone = input.tone ?? 'plain';
  const shift: ReckoningShift = input.reckoningShift ?? null;

  // §7 T1: condemn/free close the harvest channel — the sharp NAMED lane goes QUIET. (The ambient
  // Tier-0 land-behavior lines live in a different producer and are never gated here.)
  if (shift === 'condemn' || shift === 'free') {
    notes.push(`sharp-quote lane quiet — reckoning '${shift}' closed the harvest channel (§7 T1); ambient Tier-0 unaffected`);
    return { reports, marks, notes };
  }

  for (const d of input.dossiers) {
    if (!d.name) {
      notes.push(`skipped ${d.groupKey}: no name (precision floor)`);
      continue; // never report a nameless player
    }
    const habit = dominantHabit(d, k);
    if (!habit) {
      notes.push(`held ${d.name}: flat dossier — no dominant habit, no report`);
      continue; // a wrong "it knows you" is worse than none
    }

    const ord = HABIT_ORD[habit];
    const already = input.reported[d.groupKey] ?? 0;
    if (ord === already) continue; // idempotent: same dominant habit already dripped

    // The deterministic floor — ALWAYS the authored voice line, chosen before the model is offered
    // the slot. customPhrase resolves the chorus custom; reportObserved is the Watcher register.
    // §7 T1: post-'understand' the quotes PERSIST but READ DIFFERENTLY — the same grounded facts,
    // re-framed as kept-true (mercy as accuracy). Any other shift value uses the standard line.
    const phrase = customPhrase(HABIT_CUSTOM[habit]);
    const line = shift === 'understand'
      ? voice.reportObservedKeptTrue(d.name, d.honoredCount, phrase)
      : voice.reportObserved(d.name, d.honoredCount, phrase);

    reports.push({
      groupKey: d.groupKey,
      name: d.name,
      habit,
      line,
      authorSlot: {
        name: d.name,
        habit,
        fallbackLine: line, // the scalpel may only REPLACE this in-register, or decline
        register: 'lowercase; sparse; the watcher counts and states; it does not emote or threaten',
      },
      tone,
      staged: input.mode === 'confirm',
      reason: `dominant habit '${habit}' (changed from ord ${already}); grounded on ${d.honoredCount} kept`,
    });
    marks[d.groupKey] = ord;
  }

  // Deterministic order (by groupKey) so a tick's reports are stable + testable.
  reports.sort((a, b) => a.groupKey.localeCompare(b.groupKey));
  return { reports, marks, notes };
}

/**
 * resolveReportLine — the SINGLE chokepoint where the optional LLM scalpel may touch a report, with its
 * deterministic fallback inline. Given a report + an optional author fn, returns the final line: the
 * model's text IF it returned a non-empty in-register string, else the authored fallback. ANY
 * throw/empty/null → fallback. Never blocks: the report already carries a complete line.
 *
 * Pure given a SYNCHRONOUS author fn (for the self-test). The run wrapper passes the real async scalpel
 * and awaits it with a timeout; the fallback path is what the self-test pins.
 */
export function resolveReportLine(
  report: ObservationReport,
  authored: (slot: ReportAuthorSlot) => string | null,
): string {
  try {
    const out = authored(report.authorSlot);
    if (typeof out === 'string' && out.trim().length > 0) return out;
  } catch {
    /* fall through to the deterministic floor */
  }
  return report.authorSlot.fallbackLine;
}

/**
 * One player's raw MEASURED behavior — the minimal shape buildObservationDossiers scores (the plugin's
 * `dossiers` row satisfies this structurally; reports.run.ts feeds it in). Kept here, in the PURE policy,
 * so the group-relative scoring is DB-free and self-testable.
 */
export interface MeasuredBehavior {
  /** stable per-player key (mc_uuid). */
  groupKey: string;
  name: string | null;
  hoardedScore: number;
  soloMiningSeconds: number;
  distanceFromGroup: number;
  forbiddenWordHits: number;
  /** measured violations of the_bow — passing markers uncrouched (the `silent` axis; from custom_compliance). */
  bowViolations: number;
  /** measured violations of the_dark_hours — active on the black moon (the `night-walks` axis; compliance). */
  darkHoursViolations: number;
  /** lectern reads — studies the lore (the `reads` axis; from the dossier's lectern_reads). */
  lecternReads: number;
}

/**
 * ALL SIX chorus axes are measured now: hoards / wanders / spends-words from the dossier, silent /
 * night-walks derived from custom_compliance VIOLATIONS (most passes markers uncrouched / most walks the
 * black moon), and reads from the dossier's lectern_reads (studies the lore). The full "it knows you".
 */
const SCORED_AXES: readonly HabitAxis[] = ['hoards', 'wanders', 'spends-words', 'silent', 'night-walks', 'reads'];

/** The raw per-player signal for each scored axis (higher = more of that habit). Pure. */
function rawSignal(b: MeasuredBehavior, axis: HabitAxis): number {
  switch (axis) {
    case 'hoards': return Math.max(b.hoardedScore, b.soloMiningSeconds / 60); // score, or minutes mined alone
    case 'wanders': return Math.max(0, b.distanceFromGroup);
    case 'spends-words': return Math.max(0, b.forbiddenWordHits);
    case 'silent': return Math.max(0, b.bowViolations);         // passes the markers without the bow
    case 'night-walks': return Math.max(0, b.darkHoursViolations); // moves under the black moon
    case 'reads': return Math.max(0, b.lecternReads);           // studies the lore at the lecterns
    default: return 0;
  }
}

/**
 * buildObservationDossiers — PURE. Turn measured behavior into the group-relative habit scores the policy
 * consumes. For each scored axis a player's score is where they sit in the GROUP'S SPREAD on that axis —
 * (raw − groupMin) ÷ (groupMax − groupMin) — so the standout leads at 1.0 and a FLAT axis (everyone the
 * same, e.g. all mining equally) scores EVERYONE 0. That is what makes "dominant" mean "dominant FOR THIS
 * GROUP": if no one stands out, no one is named (a raw ÷ max would falsely crown every identical player).
 * honoredCount is looked up on the player's OWN argmax axis's custom (HABIT_CUSTOM), so the line's "days
 * kept" matches the habit the policy will name (same scores → same argmax). Deterministic.
 */
export function buildObservationDossiers(
  rows: readonly MeasuredBehavior[],
  honoredByPlayerCustom: ReadonlyMap<string, number>,
): ObservationDossier[] {
  const spreadByAxis = new Map<HabitAxis, { min: number; max: number }>();
  for (const axis of SCORED_AXES) {
    const vals = rows.map((r) => rawSignal(r, axis));
    spreadByAxis.set(axis, { min: Math.min(...vals), max: Math.max(...vals) });
  }
  return rows.map((r) => {
    const habits: Partial<Record<HabitAxis, number>> = {};
    for (const axis of SCORED_AXES) {
      const { min, max } = spreadByAxis.get(axis) ?? { min: 0, max: 0 };
      // A flat axis (max === min) → 0 for everyone: no standout, no false "dominant".
      habits[axis] = max > min ? (rawSignal(r, axis) - min) / (max - min) : 0;
    }
    let argmax: HabitAxis = SCORED_AXES[0]!;
    for (const axis of SCORED_AXES) if ((habits[axis] ?? 0) > (habits[argmax] ?? 0)) argmax = axis;
    const honoredCount = honoredByPlayerCustom.get(`${r.groupKey}:${HABIT_CUSTOM[argmax]}`) ?? 0;
    return { groupKey: r.groupKey, name: r.name, honoredCount, habits };
  });
}
