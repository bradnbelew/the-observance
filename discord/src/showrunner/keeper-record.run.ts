/**
 * keeper-record.run.ts — the I/O wrapper for the Hold-Book enrolment ("the record writes you in",
 * keeper-record.ts). This is the missing consumer that finally delivers the authored keeperPage* pages.
 *
 * Once per autonomy tick: read the active group's measured dossiers, score them GROUP-RELATIVE (reusing
 * buildObservationDossiers — the same scorer the Tier-0 reports use), map each 6-axis chorus habit to its
 * rhyming keeper (the documented HabitAxis rhymes — hoards→vaun, reads→mara, wanders→sella, silent→orin,
 * night-walks→brann, spends-words→iss), decide each living player's book tier by arc movement
 * (living_row → keeper_heading → keeper_hand), and — WHEN anyone crosses to a NEW tier — re-fill the
 * Hold-Book lectern with the WHOLE current book (one page per named active player).
 *
 * DISCIPLINE. INV-16: the heading rhymes on a chorus behavior every player exhibits to some degree; a FLAT
 * dossier enrolls to NO keeper (stays a living row) — the group can never read WHICH player is honored from
 * the book. Idempotent per-player high-water (state.keeper_record) so a page only re-writes on a NEW tier
 * crossing. NO LLM here — the deterministic authored page stands (the optional scalpel is simply skipped).
 * Degrades to a clean no-op on an absent/small group or any failure; mutates the SHARED state (the caller
 * persists), mirroring runCompanionPass.
 */
import { readDossiers, readCustomViolations, enqueueBeat, logEvent } from '../db/repo.js';
import { buildObservationDossiers, type MeasuredBehavior } from './reports.js';
import {
  decideKeeperEnrolment,
  type PlayerDossier,
  type KeeperId,
  type KeeperPageRow,
  type EnrolmentTier,
} from './keeper-record.js';
import { voice } from '../voice.js';
import type { BeatStatus } from '../db/types.js';
import type { ShowrunnerState } from './state.js';

/** The Hold-Book lectern — the report lectern the base anomaly seeded (it starts flat, becomes keeper-headed). */
const HOLD_BOOK_SITE = 'first_report_lectern_01';

/** HabitAxis → the keeper it rhymes with (the mapping documented on reports.ts HabitAxis). */
function keeperSignals(habits: Readonly<Partial<Record<string, number>>>): Partial<Record<KeeperId, number>> {
  const s: Partial<Record<KeeperId, number>> = {};
  if (typeof habits.hoards === 'number') s.vaun = habits.hoards;
  if (typeof habits.reads === 'number') s.mara = habits.reads;
  if (typeof habits.wanders === 'number') s.sella = habits.wanders;
  if (typeof habits.silent === 'number') s.orin = habits.silent;
  if (typeof habits['night-walks'] === 'number') s.brann = habits['night-walks'];
  if (typeof habits['spends-words'] === 'number') s.iss = habits['spends-words'];
  return s;
}

/** The arc-movement tier the book is at (the same tier for every living player this movement). */
function tierForMovement(movement: number, issCaught: boolean): EnrolmentTier {
  if (movement >= 4 || issCaught) return 'keeper_hand'; // M4: the keeper's own hand writes the living player
  if (movement >= 3) return 'keeper_heading'; // M3: the row moves under a keeper heading
  return 'living_row'; // M1: a flat Archivist living-habit row
}

/** Resolve a page row to its full authored page text (deterministic; the LLM scalpel is skipped). */
function pageText(row: KeeperPageRow): string {
  if (row.tier === 'living_row' || row.keeper == null) return voice.keeperPageLiving(row.name);
  const k = row.keeper;
  if (row.tier === 'keeper_heading') {
    switch (k) {
      case 'vaun': return voice.keeperPageHeading_vaun(row.name);
      case 'mara': return voice.keeperPageHeading_mara(row.name);
      case 'sella': return voice.keeperPageHeading_sella(row.name);
      case 'orin': return voice.keeperPageHeading_orin(row.name);
      case 'brann': return voice.keeperPageHeading_brann(row.name);
      case 'iss': return voice.keeperPageHeading_iss(row.name);
    }
  } else {
    switch (k) {
      case 'vaun': return voice.keeperPageHand_vaun(row.name);
      case 'mara': return voice.keeperPageHand_mara(row.name);
      case 'sella': return voice.keeperPageHand_sella(row.name);
      case 'orin': return voice.keeperPageHand_orin(row.name);
      case 'brann': return voice.keeperPageHand_brann(row.name);
      case 'iss': return voice.keeperPageHand_iss(row.name);
    }
  }
  return voice.keeperPageLiving(row.name); // unreachable (keeper is a KeeperId), safe default
}

/**
 * runKeeperRecordPass — enrol the active group into the Hold-Book. Fires the lectern re-fill only when a
 * player crosses to a NEW tier (idempotent). Mutates the SHARED state's high-water; the caller persists.
 */
export async function runKeeperRecordPass(
  issCaught: boolean,
  movement: number,
  state: ShowrunnerState,
  beatStatus: BeatStatus,
): Promise<{ enrolled: number; dirty: boolean }> {
  const rows = await readDossiers();
  if (rows.length < 2) return { enrolled: 0, dirty: false }; // group-relative scoring needs a group

  const violations = await readCustomViolations();
  const honored = new Map<string, number>();
  const violated = new Map<string, number>();
  for (const v of violations) {
    honored.set(`${v.groupKey}:${v.customKey}`, v.honoredCount);
    violated.set(`${v.groupKey}:${v.customKey}`, v.violatedCount);
  }
  const behaviors: MeasuredBehavior[] = rows.map((r) => ({
    ...r,
    bowViolations: violated.get(`${r.groupKey}:the_bow`) ?? 0,
    darkHoursViolations: violated.get(`${r.groupKey}:the_dark_hours`) ?? 0,
  }));
  const obs = buildObservationDossiers(behaviors, honored);

  const dossiers: PlayerDossier[] = obs.map((d) => ({
    groupKey: d.groupKey,
    name: d.name,
    signals: keeperSignals(d.habits),
  }));

  const tier = tierForMovement(movement, issCaught);
  const tierFor: Record<string, EnrolmentTier> = {};
  for (const d of dossiers) tierFor[d.groupKey] = tier;

  const reported = state.keeper_record ?? {};
  const crossings = decideKeeperEnrolment(dossiers, tierFor, reported);
  if (crossings.rows.length === 0) return { enrolled: 0, dirty: false }; // nobody advanced → book unchanged

  // Someone crossed → re-render the WHOLE current book (every named player's page at their tier). An empty
  // `reported` makes decideKeeperEnrolment emit each player's current page, in a deterministic groupKey order.
  const fullRender = decideKeeperEnrolment(dossiers, tierFor, {});
  const pages = fullRender.rows.map(pageText).filter((t) => t.trim().length > 0);
  if (pages.length === 0) return { enrolled: 0, dirty: false };

  await enqueueBeat(
    'lectern_fill',
    null,
    { title: 'the hold-book', author: 'the record', pages, place_if_missing: true },
    beatStatus,
    HOLD_BOOK_SITE,
  );

  state.keeper_record = { ...reported, ...crossings.marks };
  await logEvent('info', 'showrunner.keeper-record', `hold-book re-filled: ${crossings.rows.length} new enrolment(s), ${pages.length} page(s)`);
  return { enrolled: crossings.rows.length, dirty: true };
}
