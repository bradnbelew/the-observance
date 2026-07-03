/**
 * Pure unit tests for the between-session AUTONOMY policies (no DB, no network, no clock, no LLM).
 *   npx tsx src/showrunner/autonomy.selftest.ts   (or: npm run showrunner:test:autonomy)
 * Exits non-zero on any failed assertion so it gates the build alongside decide/customs/scenario.
 *
 * Covers the nine pure modules: fate, reckoning, keeper-record, prologue, grave, forks, herd, clock,
 * name-where-never-been, offline-skin. Each test pins the LAW the module exists to honor (precision,
 * idempotency, one-way permanence, the separation law, INV-15's no-whisper-budget, …), not just the
 * happy path.
 */
import { decideFate, type FateInput } from './fate.js';
import { reckon, type ReckoningInput } from './reckoning.js';
import { decideKeeperEnrolment, resolveAuthorClause, type PlayerDossier, type EnrolmentTier } from './keeper-record.js';
import { decidePrologue } from './prologue.js';
import { decideGrave } from './grave.js';
import { applyForks } from './forks.js';
import { paceHerd } from './herd.js';
import { bindAcceptingInstant, instantReached, encodeTimestamp } from './clock.js';
import { selectCarve, type PlayerPresence, type CarveAnchor } from './name-where-never-been.js';
import { selectGlimpse, type OfflineCandidate } from './offline-skin.js';
import { decidePersonalizedReports, resolveReportLine, buildObservationDossiers, type ReportInput, type ObservationDossier, type MeasuredBehavior } from './reports.js';
import { decideColdRestage, type LiarInput, type IssWarmBeat } from './liar.js';
import { selectApparition, type ConductorInput, type ApparitionCandidate } from './conductor.js';
import { resolveKeeperDialogue, type KeeperDialogueInput, type KeeperDialogueDossier } from './keeper.js';
import { resolveCompanionDialogue, type CompanionDialogueInput, type CompanionArcFlags } from './companion.js';
import { composeFinale, type FinaleComposeInput } from './finale.js';
import { decideTheories, CLUSTERS, theoryFlag } from './theory.js';
import { decideRelief, RELIEF_BEATS } from './relief.js';
import { archiveLine } from '../voice.archive.js';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

let failures = 0;
function check(label: string, cond: boolean): void {
  if (cond) console.log(`  ok   ${label}`);
  else { failures += 1; console.error(`  FAIL ${label}`); }
}

const HOUR = 3_600_000;

// ===========================================================================
// fate.ts — INV-11 active-only enum, precision REFUSERS, DIVIDED floor.
// ===========================================================================
function fateInput(over: Partial<FateInput> = {}): FateInput {
  return { honoredActive: 0, violatedActive: 0, leftAtActive: 0, seventhFound: false, issCaught: false, quorumMet: false, refusalSignal: false, ...over };
}
{
  check('fate: honored-dominant + iss + quorum → kept',
    decideFate(fateInput({ honoredActive: 5, violatedActive: 1, issCaught: true, quorumMet: true })).fate === 'kept');
  check('fate: violated-dominant + 2 left → cast_out',
    decideFate(fateInput({ honoredActive: 1, violatedActive: 5, leftAtActive: 2 })).fate === 'cast_out');
  check('fate: real spread, no pole → divided',
    decideFate(fateInput({ honoredActive: 3, violatedActive: 3 })).fate === 'divided');
  check('fate: empty arc → divided (never a pole it did not earn)',
    decideFate(fateInput()).fate === 'divided');
  // PRECISION: a quorum with NO defiance signal is NEVER refusers (slowness ≠ refusal).
  check('fate: quorum but no defiance signal → NOT refusers',
    decideFate(fateInput({ quorumMet: true, honoredActive: 4, issCaught: true })).fate !== 'refusers');
  check('fate: quorum + positive defiance → refusers',
    decideFate(fateInput({ quorumMet: true, refusalSignal: true })).fate === 'refusers');
  // honored-dominant but NO spine payoff → not kept (falls to divided).
  check('fate: honored but no seventh/iss → not kept',
    decideFate(fateInput({ honoredActive: 5, violatedActive: 1, quorumMet: true })).fate !== 'kept');
  check('fate: deterministic', JSON.stringify(decideFate(fateInput({ honoredActive: 4, violatedActive: 1, issCaught: true, quorumMet: true }))) ===
    JSON.stringify(decideFate(fateInput({ honoredActive: 4, violatedActive: 1, issCaught: true, quorumMet: true }))));
}

// ===========================================================================
// reckoning.ts — grip state, cadence dial, hysteresis, INV-15 (no whisper budget).
// ===========================================================================
function reckInput(over: Partial<ReckoningInput> = {}): ReckoningInput {
  return { nowMs: 1000 * HOUR, distinctSolvers: 3, firstTryRate: 0.4, whisperLean: 0.5, priorState: null, priorSinceMs: null, ...over };
}
{
  check('reckon: fast group → tight (cadence > 1)',
    (() => { const r = reckon(reckInput({ firstTryRate: 0.8, whisperLean: 0.2 })); return r.state === 'tight' && r.cadenceMult > 1 && r.tone === 'cold'; })());
  check('reckon: stumbling group → loose (cadence < 1, warm)',
    (() => { const r = reckon(reckInput({ firstTryRate: 0.1 })); return r.state === 'loose' && r.cadenceMult < 1 && r.tone === 'warm'; })());
  check('reckon: middle → even (×1, plain)',
    (() => { const r = reckon(reckInput({ firstTryRate: 0.45, whisperLean: 0.5 })); return r.state === 'even' && r.cadenceMult === 1 && r.tone === 'plain'; })());
  check('reckon: whisper-lean forces loose even when fast (struggle wins)',
    reckon(reckInput({ firstTryRate: 0.9, whisperLean: 2 })).state === 'loose');
  // HYSTERESIS: a flip is suppressed until the prior state has aged.
  check('reckon: flip suppressed before hysteresis ages',
    reckon(reckInput({ firstTryRate: 0.9, whisperLean: 0.1, priorState: 'loose', priorSinceMs: 1000 * HOUR - 1 * HOUR })).state === 'loose');
  check('reckon: flip allowed after hysteresis ages',
    reckon(reckInput({ firstTryRate: 0.9, whisperLean: 0.1, priorState: 'loose', priorSinceMs: 1000 * HOUR - 100 * HOUR })).state === 'tight');
  check('reckon: unchanged reading keeps the original sinceMs',
    reckon(reckInput({ firstTryRate: 0.1, priorState: 'loose', priorSinceMs: 500 * HOUR })).sinceMs === 500 * HOUR);
  check('reckon: deterministic',
    JSON.stringify(reckon(reckInput({ firstTryRate: 0.8, whisperLean: 0.1 }))) === JSON.stringify(reckon(reckInput({ firstTryRate: 0.8, whisperLean: 0.1 }))));
}
// INV-15 grep-guard (the manifest's "grep the module for whisper_budgets; a hit is a bug"): the
// difficulty engine never touches the player-controlled Whisper safety rail. We assert the SOURCE
// of reckoning.ts contains no `whisper_budgets` reference. (The `whisperLean` *signal* — whispers
// leaned on as a struggle tell — is allowed; the forbidden thing is the budget TABLE/knob.)
{
  const reckSrc = readFileSync(fileURLToPath(new URL('./reckoning.ts', import.meta.url)), 'utf8');
  // Strip comments first — the doc-comments that PROMISE "never touches whisper_budgets" name the
  // table by design; the bug INV-15 forbids is a real CODE reference (a `from('whisper_budgets')`
  // read/write). So we grep the de-commented source for the table token.
  const code = reckSrc
    .replace(/\/\*[\s\S]*?\*\//g, '') // block comments
    .replace(/\/\/.*$/gm, ''); // line comments
  check('reckon: INV-15 — code never references whisper_budgets', !/whisper_budgets/.test(code));
}

// ===========================================================================
// keeper-record.ts — precision floor (flat → no one), idempotent tiers, author fallback.
// ===========================================================================
function dossier(over: Partial<PlayerDossier> = {}): PlayerDossier {
  return { groupKey: 'u1', name: 'vauney', signals: { vaun: 0.7, mara: 0.1 }, ...over };
}
{
  const tiers: Record<string, EnrolmentTier> = { u1: 'keeper_heading' };
  const d = decideKeeperEnrolment([dossier()], tiers, {});
  check('keeper: confident dossier → enrolled under the dominant keeper',
    d.rows.length === 1 && d.rows[0]?.keeper === 'vaun' && d.rows[0]?.tier === 'keeper_heading');
  // PRECISION FLOOR: a flat dossier at a heading tier enrols to NO ONE (stays living_row).
  const flat = decideKeeperEnrolment([dossier({ groupKey: 'u2', signals: { vaun: 0.3, mara: 0.28 } })], { u2: 'keeper_heading' }, {});
  check('keeper: flat dossier → enrolled to no one (living_row, keeper null)',
    flat.rows.every((r) => r.keeper === null));
  // PRECISION: nameless → skipped, never written.
  const nameless = decideKeeperEnrolment([dossier({ groupKey: 'u3', name: null })], { u3: 'keeper_hand' }, {});
  check('keeper: nameless → no row', nameless.rows.length === 0 && nameless.notes.some((n) => n.includes('no name')));
  // IDEMPOTENT: a tier already at/under the high-water never re-fires.
  const idemp = decideKeeperEnrolment([dossier()], { u1: 'keeper_heading' }, { u1: 2 });
  check('keeper: already-written tier → no re-fire', idemp.rows.length === 0);
  // keeper_hand offers the author slot; the FALLBACK stands when the model declines/throws.
  const hand = decideKeeperEnrolment([dossier()], { u1: 'keeper_hand' }, {});
  const row = hand.rows[0]!;
  check('keeper: hand tier carries an author slot', row.authorSlot != null);
  check('keeper: author DECLINES → fallback clause', resolveAuthorClause(row, () => null) === row.authorSlot!.fallbackClause);
  check('keeper: author THROWS → fallback clause', resolveAuthorClause(row, () => { throw new Error('slow model'); }) === row.authorSlot!.fallbackClause);
  check('keeper: author returns text → that text', resolveAuthorClause(row, () => 'a kept thing, and another, and the one before') === 'a kept thing, and another, and the one before');
}

// ===========================================================================
// prologue.ts — ignition gate, one-shot ack, named-report precision.
// ===========================================================================
{
  const dormant = decidePrologue({ ignited: false, acked: false, overwhelmingSignal: true, signalName: 'brann' });
  check('prologue: not ignited → curatorial suppressed, no ack', !dormant.curatorialAllowed && !dormant.postAck && dormant.step === 'dormant');
  const ignite = decidePrologue({ ignited: true, acked: false, overwhelmingSignal: true, signalName: 'brann' });
  check('prologue: ignited + not acked → post one-shot ack + unlock', ignite.postAck && ignite.curatorialAllowed && ignite.step === 'ignited');
  check('prologue: overwhelming + name → named report key', ignite.reportVoiceKey === 'recordOpenedNamed');
  const acked = decidePrologue({ ignited: true, acked: true, overwhelmingSignal: true, signalName: 'brann' });
  check('prologue: already acked → no re-post', !acked.postAck && acked.step === 'acknowledged');
  // PRECISION: no single overwhelming signal → un-named fallback (never a guess).
  const weak = decidePrologue({ ignited: true, acked: false, overwhelmingSignal: false, signalName: null });
  check('prologue: no overwhelming signal → un-named FACT-1 fallback', weak.reportVoiceKey === 'recordOpened');
  const noName = decidePrologue({ ignited: true, acked: false, overwhelmingSignal: true, signalName: null });
  check('prologue: overwhelming but no name → still un-named fallback', noName.reportVoiceKey === 'recordOpened');
}

// ===========================================================================
// grave.ts — one grave, carve-before-open, INV-14 date-is-read, idempotent.
// ===========================================================================
{
  const subj = { groupKey: 'u1', name: 'sella', active: true };
  const unbound = decideGrave({ acceptingInstantMs: null, nowMs: 0, subject: subj, carved: false, opened: false });
  check('grave: unbound instant → no grave', unbound.row === null);
  const carve = decideGrave({ acceptingInstantMs: 2000 * HOUR, nowMs: 1000 * HOUR, subject: subj, carved: false, opened: false });
  check('grave: bound + active subject + not carved → carve row', carve.row?.kind === 'carve' && carve.marks.carved === true);
  check('grave: carved date == accepting instant (read, not typed)', carve.row?.dateMs === 2000 * HOUR);
  const notDue = decideGrave({ acceptingInstantMs: 2000 * HOUR, nowMs: 1500 * HOUR, subject: subj, carved: true, opened: false });
  check('grave: carved but date not reached → no open', notDue.row === null);
  const open = decideGrave({ acceptingInstantMs: 2000 * HOUR, nowMs: 2000 * HOUR, subject: subj, carved: true, opened: false });
  check('grave: date reached → open row', open.row?.kind === 'open' && open.marks.opened === true);
  const done = decideGrave({ acceptingInstantMs: 2000 * HOUR, nowMs: 3000 * HOUR, subject: subj, carved: true, opened: true });
  check('grave: carved + opened → done, no row', done.row === null);
  // GROUNDING: no active named subject → never guess a grave.
  const noSubj = decideGrave({ acceptingInstantMs: 2000 * HOUR, nowMs: 2000 * HOUR, subject: null, carved: false, opened: false });
  check('grave: no grounded subject → no grave', noSubj.row === null);
}

// ===========================================================================
// forks.ts — one-way / first-writer-wins, colors-never-gates, INV-13 pale guard.
// ===========================================================================
{
  const a = applyForks({}, { glowingBeastKilled: true });
  check('fork A: glowing kill → sacred_beast_broken', a.setFlags.sacred_beast_broken === true);
  const settled = applyForks({ sacred_beast_broken: true }, { glowingBeastKilled: true });
  check('fork A: already settled → no re-commit (one-way)', Object.keys(settled.setFlags).length === 0);
  const b = applyForks({}, { firstLightChoice: 'taken' });
  check('fork B: taken → light_taken', b.setFlags.light_taken === true);
  const bSettled = applyForks({ light_kept: true }, { firstLightChoice: 'taken' });
  check('fork B: kept already set → ignore opposite trigger (permanence)', Object.keys(bSettled.setFlags).length === 0);
  const c = applyForks({}, { spokenNameChoice: 'unspoken' });
  check('fork C: unspoken → name_unspoken', c.setFlags.name_unspoken === true);
  // INV-13: a pale-cosmetic kill is NEVER passed as glowingBeastKilled, so it can't arm fork A.
  const pale = applyForks({}, { glowingBeastKilled: undefined });
  check('fork A: no glowing kill (pale only) → fork unarmed', pale.setFlags.sacred_beast_broken === undefined);
  check('forks: deterministic', JSON.stringify(applyForks({}, { firstLightChoice: 'kept' })) === JSON.stringify(applyForks({}, { firstLightChoice: 'kept' })));
}

// ===========================================================================
// herd.ts — capped, monotone, one-per-pass, cosmetic (never glowing).
// ===========================================================================
{
  const before = paceHerd({ movement: 1, priorPaleCount: 1, passDoneThisWindow: false });
  check('herd: before start movement → no spread', !before.spread && before.addThisPass === 0);
  const spread = paceHerd({ movement: 2, priorPaleCount: 1, passDoneThisWindow: false });
  check('herd: at movement → spread +1, monotone', spread.spread && spread.paleTarget === 2);
  const doneWindow = paceHerd({ movement: 2, priorPaleCount: 2, passDoneThisWindow: true });
  check('herd: pass already done this window → no double-add', !doneWindow.spread);
  const capped = paceHerd({ movement: 4, priorPaleCount: 16, passDoneThisWindow: false });
  check('herd: at cap → no further spread', !capped.spread && capped.paleTarget === 16);
  check('herd: never exceeds cap', paceHerd({ movement: 4, priorPaleCount: 15, passDoneThisWindow: false }).paleTarget <= 16);
}

// ===========================================================================
// clock.ts — single instant, set-once, shared not_before, encoded timestamp.
// ===========================================================================
{
  const notReady = bindAcceptingInstant({ boundInstantMs: null, anchorMs: 1000 * HOUR, readyToBind: false });
  check('clock: not ready → unbound', notReady.instantMs === null && !notReady.newlyBound);
  const bound = bindAcceptingInstant({ boundInstantMs: null, anchorMs: 1000 * HOUR, readyToBind: true });
  check('clock: ready → bound newly', bound.instantMs != null && bound.newlyBound);
  const already = bindAcceptingInstant({ boundInstantMs: 5000 * HOUR, anchorMs: 9999 * HOUR, readyToBind: true });
  check('clock: already bound → unchanged (set-once, idempotent)', already.instantMs === 5000 * HOUR && !already.newlyBound);
  check('clock: deterministic anchor → same instant on re-run',
    bindAcceptingInstant({ boundInstantMs: null, anchorMs: 1000 * HOUR, readyToBind: true }).instantMs ===
    bindAcceptingInstant({ boundInstantMs: null, anchorMs: 1000 * HOUR, readyToBind: true }).instantMs);
  check('clock: instantReached false when unbound', !instantReached(null, 9e15));
  check('clock: instantReached true at/after the instant', instantReached(1000, 1000) && instantReached(1000, 2000));
  check('clock: encodeTimestamp stable + in-world form', /^\d{4}\.\d{3}\.\d$/.test(encodeTimestamp(Date.UTC(2026, 5, 25, 18))));
}

// ===========================================================================
// name-where-never-been.ts — proof-of-absence gate, chorus rotation, separation.
// ===========================================================================
function presence(over: Partial<PlayerPresence> = {}): PlayerPresence {
  return { groupKey: 'u1', name: 'orin', visitedCells: new Set<string>(), carvedCount: 0, ...over };
}
const anchors: CarveAnchor[] = [{ siteId: 's_a', cellId: 'c_a' }, { siteId: 's_b', cellId: 'c_b' }];
{
  // happy: a named player with a proof-set, an avoided+unused anchor they never visited → carve.
  const ok = selectCarve({
    activePlayers: [presence({ visitedCells: new Set(['c_x']) })],
    anchors, groupVisitedCells: new Set(['c_x']), usedCells: new Set(),
  });
  check('carve: proof-of-absence pairing → a carve', ok.carve != null && ok.carve.cellId === 'c_a');
  // PRECISION: no proof-set (null) → NEVER carve (can't prove absence).
  const noProof = selectCarve({
    activePlayers: [presence({ visitedCells: null })],
    anchors, groupVisitedCells: new Set(), usedCells: new Set(),
  });
  check('carve: no visited-proof-set → no carve (precision)', noProof.carve === null);
  // group-avoided: an anchor the group HAS visited is ineligible.
  const groupThere = selectCarve({
    activePlayers: [presence({ visitedCells: new Set() })],
    anchors: [{ siteId: 's_a', cellId: 'c_a' }], groupVisitedCells: new Set(['c_a']), usedCells: new Set(),
  });
  check('carve: group-frequented cell → ineligible → no carve', groupThere.carve === null);
  // proof-of-absence: a player who HAS visited the only avoided anchor → no proof-safe carve.
  const wasThere = selectCarve({
    activePlayers: [presence({ visitedCells: new Set(['c_a']) })],
    anchors: [{ siteId: 's_a', cellId: 'c_a' }], groupVisitedCells: new Set(), usedCells: new Set(),
  });
  check('carve: player visited the only anchor → no carve', wasThere.carve === null);
  // chorus rotation: fewest-carved subject first.
  const rot = selectCarve({
    activePlayers: [presence({ groupKey: 'u1', name: 'a', carvedCount: 2, visitedCells: new Set() }), presence({ groupKey: 'u2', name: 'b', carvedCount: 0, visitedCells: new Set() })],
    anchors, groupVisitedCells: new Set(), usedCells: new Set(),
  });
  check('carve: rotates to the fewest-carved player', rot.carve?.groupKey === 'u2');
  // used cells are skipped.
  const used = selectCarve({
    activePlayers: [presence({ visitedCells: new Set() })],
    anchors: [{ siteId: 's_a', cellId: 'c_a' }], groupVisitedCells: new Set(), usedCells: new Set(['c_a']),
  });
  check('carve: used cell → no carve', used.carve === null);
}

// ===========================================================================
// offline-skin.ts — offline-only, precision rhyme, separation law, named approval.
// ===========================================================================
function offline(over: Partial<OfflineCandidate> = {}): OfflineCandidate {
  return { groupKey: 'u1', name: 'brann', offline: true, shapeRhyme: { watcher_at_edge: 0.8, surface_walker: 0.2 }, wornCount: 0, ...over };
}
{
  const ok = selectGlimpse({ candidates: [offline()], phase: 'deniable', carveClaimsByPlayer: {}, proposedCell: 'c1', namedApproved: false });
  check('skin: offline + confident rhyme → glimpse (deniable, no name-tag)', ok.glimpse != null && ok.glimpse.nameTag === false && ok.glimpse.shape === 'watcher_at_edge');
  // offline-only: an online candidate is never worn.
  const online = selectGlimpse({ candidates: [offline({ offline: false })], phase: 'deniable', carveClaimsByPlayer: {}, proposedCell: 'c1', namedApproved: false });
  check('skin: online player → never worn (offline-only)', online.glimpse === null);
  // precision: a flat rhyme → no canonical wearing.
  const flat = selectGlimpse({ candidates: [offline({ shapeRhyme: { watcher_at_edge: 0.4, surface_walker: 0.38 } })], phase: 'deniable', carveClaimsByPlayer: {}, proposedCell: 'c1', namedApproved: false });
  check('skin: flat shape-rhyme → no glimpse', flat.glimpse === null);
  // separation law: glimpse cell collides with the player's active carve → skip.
  const collide = selectGlimpse({ candidates: [offline()], phase: 'deniable', carveClaimsByPlayer: { u1: new Set(['c1']) }, proposedCell: 'c1', namedApproved: false });
  check('skin: collides with own name-carve cell → no glimpse (separation law)', collide.glimpse === null);
  // named M4 beat requires explicit approval.
  const unapproved = selectGlimpse({ candidates: [offline()], phase: 'named', carveClaimsByPlayer: {}, proposedCell: 'c1', namedApproved: false });
  check('skin: named phase unapproved → withheld', unapproved.glimpse === null);
  const approved = selectGlimpse({ candidates: [offline()], phase: 'named', carveClaimsByPlayer: {}, proposedCell: 'c1', namedApproved: true });
  check('skin: named phase approved → glimpse with name-tag', approved.glimpse != null && approved.glimpse.nameTag === true);
  // one-shot budget: a spent player is not worn again.
  const spent = selectGlimpse({ candidates: [offline({ wornCount: 1 })], phase: 'deniable', carveClaimsByPlayer: {}, proposedCell: 'c1', namedApproved: false });
  check('skin: one-shot budget spent → no glimpse', spent.glimpse === null);
}

// ===========================================================================
// reports.ts — personalized observation: precision floor, deterministic fallback, idempotency.
// ===========================================================================
function obsDossier(over: Partial<ObservationDossier> = {}): ObservationDossier {
  return { groupKey: 'u1', name: 'brann', honoredCount: 4, habits: { 'night-walks': 0.7, hoards: 0.1 }, ...over };
}
function reportInput(over: Partial<ReportInput> = {}): ReportInput {
  return { dossiers: [obsDossier()], reported: {}, mode: 'auto', ...over };
}
{
  const ok = decidePersonalizedReports(reportInput());
  check('reports: confident dominant habit → one report, grounded line',
    ok.reports.length === 1 && ok.reports[0]!.habit === 'night-walks' && ok.reports[0]!.line.includes('brann'));
  // PRECISION: a flat dossier names no one.
  const flat = decidePersonalizedReports(reportInput({ dossiers: [obsDossier({ habits: { 'night-walks': 0.4, hoards: 0.38 } })] }));
  check('reports: flat dossier → no report (precision)', flat.reports.length === 0);
  // PRECISION: nameless → never reported.
  const nameless = decidePersonalizedReports(reportInput({ dossiers: [obsDossier({ name: null })] }));
  check('reports: nameless → no report', nameless.reports.length === 0);
  // IDEMPOTENT: the same dominant habit already reported → no re-fire.
  const idemp = decidePersonalizedReports(reportInput({ reported: { u1: 5 } })); // 5 = HABIT_ORD['night-walks']
  check('reports: same dominant habit already dripped → no re-fire', idemp.reports.length === 0);
  // CONFIRM → staged; AUTO → live.
  check('reports: confirm mode → staged', decidePersonalizedReports(reportInput({ mode: 'confirm' })).reports[0]!.staged === true);
  check('reports: auto mode → live (not staged)', ok.reports[0]!.staged === false);
  // DETERMINISTIC FALLBACK: scalpel declines / throws / returns junk → the authored floor stands.
  const row = ok.reports[0]!;
  check('reports: author DECLINES → fallback line', resolveReportLine(row, () => null) === row.authorSlot.fallbackLine);
  check('reports: author THROWS → fallback line', resolveReportLine(row, () => { throw new Error('slow model'); }) === row.authorSlot.fallbackLine);
  check('reports: author EMPTY → fallback line', resolveReportLine(row, () => '   ') === row.authorSlot.fallbackLine);
  check('reports: author returns in-register text → that text', resolveReportLine(row, () => 'the one called brann keeps the dark hours alone') === 'the one called brann keeps the dark hours alone');
  check('reports: deterministic', JSON.stringify(decidePersonalizedReports(reportInput())) === JSON.stringify(decidePersonalizedReports(reportInput())));
}

// ===========================================================================
// liar.ts — flag-gated, one-way warm→cold re-stage, curatorial pending default, idempotent.
// ===========================================================================
const warmBeats: IssWarmBeat[] = [
  { id: 'b1', warmKey: 'iss.warm.wall', coldKey: 'iss.cold.wall', siteId: 'stone_iss' },
  { id: 'b2', warmKey: 'iss.warm.promise', coldKey: 'iss.cold.promise' },
];
function liarInput(over: Partial<LiarInput> = {}): LiarInput {
  return { issCaught: true, warmBeats, alreadyFlipped: new Set(), mode: 'auto', ...over };
}
{
  // FLAG GATE: before the catch nothing flips.
  const warm = decideColdRestage(liarInput({ issCaught: false }));
  check('liar: iss not caught → no cold re-stage (flag-gated)', warm.rows.length === 0);
  // AT the catch: each warm beat re-staged cold, once.
  const flip = decideColdRestage(liarInput());
  check('liar: iss caught → re-stage every warm beat cold', flip.rows.length === 2 && flip.rows[0]!.coldKey === 'iss.cold.wall');
  check('liar: AUTO mode → approved status', flip.rows.every((r) => r.status === 'approved'));
  check('liar: CONFIRM mode → pending (curatorial gate)', decideColdRestage(liarInput({ mode: 'confirm' })).rows.every((r) => r.status === 'pending'));
  check('liar: carries the in-world site when present', flip.rows.find((r) => r.beatId === 'b1')!.siteId === 'stone_iss');
  // ONE-WAY / IDEMPOTENT: an already-flipped beat is skipped.
  const partial = decideColdRestage(liarInput({ alreadyFlipped: new Set(['b1']) }));
  check('liar: already-flipped beat skipped (one-way)', partial.rows.length === 1 && partial.rows[0]!.beatId === 'b2');
  const done = decideColdRestage(liarInput({ alreadyFlipped: new Set(['b1', 'b2']) }));
  check('liar: all flipped → steady state, no rows', done.rows.length === 0);
  check('liar: deterministic', JSON.stringify(decideColdRestage(liarInput())) === JSON.stringify(decideColdRestage(liarInput())));
}

// ===========================================================================
// conductor.ts — single-arbiter slot, probabilistic-but-seeded, per-player cap, degrade-to-no-op.
// ===========================================================================
function appCand(over: Partial<ApparitionCandidate> = {}): ApparitionCandidate {
  return { groupKey: 'u1', active: true, shapeRhyme: { watcher_at_edge: 0.8, stoop: 0.1 }, apparitionCount: 0, ...over };
}
function condInput(over: Partial<ConductorInput> = {}): ConductorInput {
  return { candidates: [appCand()], windowSeed: 12345, ...over };
}
{
  // SINGLE ARBITER: at most one claim.
  const one = selectApparition(condInput({ candidates: [appCand({ groupKey: 'u1' }), appCand({ groupKey: 'u2' })] }));
  check('conductor: at most ONE claim (single-arbiter slot)', one.claim != null);
  // ACTIVE-ROSTER ONLY: an inactive candidate is never claimed.
  const inactive = selectApparition(condInput({ candidates: [appCand({ active: false })] }));
  check('conductor: inactive candidate → no claim (active-only)', inactive.claim === null);
  // PER-PLAYER CAP: a capped player is skipped → degrade to no-op when alone.
  const capped = selectApparition(condInput({ candidates: [appCand({ apparitionCount: 1 })] }));
  check('conductor: per-player cap reached → no claim (degrade to no-op)', capped.claim === null);
  // PRECISION FLOOR: a flat rhyme is not a candidate.
  const flat = selectApparition(condInput({ candidates: [appCand({ shapeRhyme: { watcher_at_edge: 0.2, stoop: 0.1 } })] }));
  check('conductor: flat shape-rhyme below floor → no claim', flat.claim === null);
  // SEEDED DETERMINISM: same seed + weights → same claim (idempotent slot).
  check('conductor: same seed → same claim (idempotent slot)',
    JSON.stringify(selectApparition(condInput())) === JSON.stringify(selectApparition(condInput())));
  // PROBABILISTIC (not argmax): across many seeds a lower-rhyme player CAN be chosen sometimes (the
  // bias raises odds, it does not select). Assert both players win at least one of 200 seeds.
  {
    const cands = [appCand({ groupKey: 'u1', shapeRhyme: { watcher_at_edge: 0.9 } }), appCand({ groupKey: 'u2', shapeRhyme: { watcher_at_edge: 0.45 } })];
    const winners = new Set<string>();
    for (let s = 0; s < 200; s++) {
      const c = selectApparition({ candidates: cands, windowSeed: s }).claim;
      if (c) winners.add(c.groupKey);
    }
    check('conductor: probabilistic — the lower-rhyme player still wins some windows (not argmax)', winners.has('u1') && winners.has('u2'));
  }
  // DEGRADE: empty roster → no claim.
  check('conductor: empty roster → no claim', selectApparition(condInput({ candidates: [] })).claim === null);
}

// ===========================================================================
// keeper.ts — dialogue resolver: INV-18 defer, M-IV atonement, FACT-9 one-surface, precision floor.
// ===========================================================================
function kDossier(over: Partial<KeeperDialogueDossier> = {}): KeeperDialogueDossier {
  return { groupKey: 'u1', name: 'orin', rhymesWith: 'orin', brokenCustom: null, atoned: false, ...over };
}
function kInput(over: Partial<KeeperDialogueInput> = {}): KeeperDialogueInput {
  return { kind: 'presiding', dossier: kDossier(), issCaught: false, movement: 2, loggedFirstBeat: null, fact9ShownThisWindow: false, apparitionClaimedFor: false, ...over };
}
{
  // INV-18: a prior-keeper apparition NOT claimed by the conductor → no node (defer to the slot).
  const deferred = resolveKeeperDialogue(kInput({ kind: 'prior', apparitionClaimedFor: false }));
  check('keeper: unclaimed prior apparition → defers to the slot (INV-18)', deferred.node === null);
  const claimed = resolveKeeperDialogue(kInput({ kind: 'prior', apparitionClaimedFor: true }));
  check('keeper: claimed prior apparition → a node', claimed.node != null);
  // PRESIDING is non-ambient → always available even without a claim.
  check('keeper: presiding NPC → node without a claim (not ambient)', resolveKeeperDialogue(kInput()).node != null);
  // M-IV ATONEMENT: a measured broken custom, not atoned, at movement IV → withholding node.
  const withhold = resolveKeeperDialogue(kInput({ movement: 4, dossier: kDossier({ brokenCustom: 'the_bow', atoned: false }) }));
  check('keeper: M-IV unatoned broken custom → withholds a fragment', withhold.node?.withholdsFragment === true && withhold.node?.voiceKey === 'keeper.atone.withheld');
  const cleared = resolveKeeperDialogue(kInput({ movement: 4, dossier: kDossier({ brokenCustom: 'the_bow', atoned: true }) }));
  check('keeper: M-IV atoned → fragment released', cleared.node?.voiceKey === 'keeper.atone.cleared' && cleared.node?.withholdsFragment === false);
  // FACT 9 via dialogue — names the logged beat, but ONLY one surface per window.
  const fact9 = resolveKeeperDialogue(kInput({ loggedFirstBeat: 'mined_alone_night1', fact9ShownThisWindow: false }));
  check('keeper: logged M-I beat + sole surface → delivers FACT 9', fact9.node?.deliversFact9 === true && fact9.node?.voiceKey === 'keeper.fact9.named');
  const fact9Held = resolveKeeperDialogue(kInput({ loggedFirstBeat: 'mined_alone_night1', fact9ShownThisWindow: true }));
  check('keeper: FACT 9 already shown this window → withholds the dialogue node (one surface per window)', fact9Held.node?.deliversFact9 !== true);
  // ISS COLD SWAP: an iss-rhymed node reads cold post-catch (same iss_caught flag).
  const issWarm = resolveKeeperDialogue(kInput({ dossier: kDossier({ rhymesWith: 'iss' }), issCaught: false }));
  check('keeper: iss-rhyme pre-catch → warm node', issWarm.node?.voiceKey === 'keeper.rhyme.iss');
  const issCold = resolveKeeperDialogue(kInput({ dossier: kDossier({ rhymesWith: 'iss' }), issCaught: true }));
  check('keeper: iss-rhyme post-catch → cold node (same flag as activation lane)', issCold.node?.voiceKey === 'keeper.iss.cold');
  // PRECISION FLOOR: a flat dossier → the neutral node, never a guessed callout.
  const neutral = resolveKeeperDialogue(kInput({ dossier: kDossier({ rhymesWith: null }) }));
  check('keeper: flat dossier → neutral presiding node (no callout)', neutral.node?.voiceKey === 'keeper.presiding.neutral');
  check('keeper: deterministic', JSON.stringify(resolveKeeperDialogue(kInput())) === JSON.stringify(resolveKeeperDialogue(kInput())));
}

// ===========================================================================
// companion.ts — Wren resolver: reckoning one-of-three set-once, reveal pair, the arc precedence.
// ===========================================================================
function cFlags(over: Partial<CompanionArcFlags> = {}): CompanionArcFlags {
  return { companionRevealed: false, reckoningCondemn: false, reckoningUnderstand: false, reckoningFree: false, ...over };
}
function cInput(over: Partial<CompanionDialogueInput> = {}): CompanionDialogueInput {
  return { flags: cFlags(), trust: 0, movement: 2, context: null, lateJoiner: false, revealDelivered: false, reckoningDelivered: false, ...over };
}
{
  // TRUST ladder (M1–M2): rungs map to the wren.trust.* keys; below the floor → absent.
  check('companion: trust floor → absent', resolveCompanionDialogue(cInput({ trust: 0 })).node?.voiceKey === 'wren.trust.absent');
  check('companion: trust rung → meet', resolveCompanionDialogue(cInput({ trust: 1 })).node?.voiceKey === 'wren.trust.meet');
  check('companion: trust ask rung', resolveCompanionDialogue(cInput({ trust: 4 })).node?.voiceKey === 'wren.trust.ask');
  // M3 crack.
  check('companion: M3 low-trust → crack.slow', resolveCompanionDialogue(cInput({ movement: 3, trust: 0 })).node?.voiceKey === 'wren.crack.slow');
  check('companion: M3 noticed → crack.notice', resolveCompanionDialogue(cInput({ movement: 3, trust: 2 })).node?.voiceKey === 'wren.crack.notice');
  // LATE JOINER (quorum-free).
  check('companion: late joiner → newhand', resolveCompanionDialogue(cInput({ lateJoiner: true })).node?.voiceKey === 'wren.roster.newhand');
  // M4 REVEAL — companion_revealed && !reckoned → reveal.yes; already delivered → null (one-shot).
  check('companion: revealed → reveal.yes (once)', resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true }) })).node?.voiceKey === 'wren.reveal.yes');
  check('companion: reveal already delivered → null', resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true }), revealDelivered: true })).node === null);
  // M5 RECKONING — the flag AND the matching context BOTH required; one-of-three; set-once.
  check('companion: condemn flag + context → reckoning.condemn',
    resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true, reckoningCondemn: true }), context: 'reckoning.condemn' })).node?.voiceKey === 'wren.reckoning.condemn');
  check('companion: understand flag + context → reckoning.understand',
    resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true, reckoningUnderstand: true }), context: 'reckoning.understand' })).node?.voiceKey === 'wren.reckoning.understand');
  check('companion: free flag + context → reckoning.free',
    resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true, reckoningFree: true }), context: 'reckoning.free' })).node?.voiceKey === 'wren.reckoning.free');
  // A flag WITHOUT its context does not fire the reckoning branch (falls through to the reveal beat).
  check('companion: reckoning flag but no context → not the reckoning node',
    resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true, reckoningCondemn: true }), context: null })).node?.isReckoning !== true);
  // set-once: an already-delivered reckoning → null.
  check('companion: reckoning already delivered → null',
    resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true, reckoningFree: true }), context: 'reckoning.free', reckoningDelivered: true })).node === null);
  check('companion: reckoning node flagged isReckoning',
    resolveCompanionDialogue(cInput({ flags: cFlags({ companionRevealed: true, reckoningCondemn: true }), context: 'reckoning.condemn' })).node?.isReckoning === true);
  check('companion: deterministic', JSON.stringify(resolveCompanionDialogue(cInput())) === JSON.stringify(resolveCompanionDialogue(cInput())));
}

// ===========================================================================
// finale.ts — M5 composer: ordered close, seventh_choice tint, reckoning_free cost, reckoning optional.
// ===========================================================================
function finInput(over: Partial<FinaleComposeInput> = {}): FinaleComposeInput {
  return { fate: 'divided', seventhChoice: null, nameSpoken: false, nameUnspoken: false, lightKept: false, lightTaken: false, sacredBeastBroken: false, inheritorsCodicil: false, reckoningFree: false, ...over };
}
{
  // The base opener is always present (never an empty close).
  check('finale: base close is the fate opener', composeFinale(finInput({ fate: 'kept' })).lines.length >= 1);
  // seventh_choice tints the close.
  const restore = composeFinale(finInput({ fate: 'kept', seventhChoice: 'restore' }));
  check('finale: seventh restore → 2 lines (opener + restored clause)', restore.lines.length === 2);
  const erase = composeFinale(finInput({ seventhChoice: 'erase' }));
  check('finale: seventh erase → erased clause present', erase.lines.length === 2);
  // null seventh_choice → no seventh clause (the fork is the spine but not yet made).
  check('finale: seventh null → opener only (no seventh clause)', composeFinale(finInput()).lines.length === 1);
  // reckoning is OPTIONAL: none set → no cost clause; FREE set → the cost clause is appended LAST.
  const noReck = composeFinale(finInput({ seventhChoice: 'restore' }));
  const free = composeFinale(finInput({ seventhChoice: 'restore', reckoningFree: true }));
  check('finale: reckoning_free appends exactly one extra (cost) line', free.lines.length === noReck.lines.length + 1);
  check('finale: the free-cost clause is LAST', free.lines[free.lines.length - 1] !== noReck.lines[noReck.lines.length - 1]);
  // fork colorants stack in order.
  const forks = composeFinale(finInput({ fate: 'cast_out', lightTaken: true, nameSpoken: true, sacredBeastBroken: true }));
  check('finale: fork leaves add colorant lines', forks.lines.length === 4);
  check('finale: inheritors codicil appends', composeFinale(finInput({ inheritorsCodicil: true })).lines.length === 2);
  check('finale: deterministic', JSON.stringify(composeFinale(finInput({ fate: 'kept', seventhChoice: 'restore', reckoningFree: true }))) ===
    JSON.stringify(composeFinale(finInput({ fate: 'kept', seventhChoice: 'restore', reckoningFree: true }))));
}

// ===========================================================================
// reports.ts — the post-reckoning sharp-quote SHIFT (§7 T1): condemn/free quiet, understand re-framed.
// ===========================================================================
{
  const base = reportInput(); // one dossier with a dominant 'night-walks' habit
  // Default (no shift) → the sharp lane is unchanged (back-compat).
  check('reports: no shift → the sharp lane fires as before', decidePersonalizedReports(base).reports.length === 1);
  // condemn / free → the harvest channel closes: NO reports (the sharp NAMED lane goes quiet).
  check('reports: reckoning condemn → sharp lane quiet (no reports)',
    decidePersonalizedReports({ ...base, reckoningShift: 'condemn' }).reports.length === 0);
  check('reports: reckoning free → sharp lane quiet (no reports)',
    decidePersonalizedReports({ ...base, reckoningShift: 'free' }).reports.length === 0);
  // understand → the quotes PERSIST but READ DIFFERENTLY (still one report, a DIFFERENT line).
  const plainLine = decidePersonalizedReports(base).reports[0]!.line;
  const understand = decidePersonalizedReports({ ...base, reckoningShift: 'understand' });
  check('reports: reckoning understand → still fires (quotes persist)', understand.reports.length === 1);
  check('reports: reckoning understand → the line reads differently (kept-true framing)', understand.reports[0]!.line !== plainLine);
}

// ===========================================================================
// theory.ts — S-D theory-lock: cluster threshold, idempotent flag high-water, fall-order, partials.
// ===========================================================================
{
  // THRESHOLD NOT MET: a single stone-decode is NOT a coherent theory (threshold 2) → none.
  check('theory: below threshold (one solve) → no lock',
    decideTheories(new Set(['stone-vaun']), new Set()).length === 0);
  // THRESHOLD MET: stone + one corroborating solve → the keeper's theory locks.
  check('theory: threshold met → keeper locks',
    JSON.stringify(decideTheories(new Set(['stone-vaun', 'vaun-hoard-sorted']), new Set())) === JSON.stringify(['vaun']));
  // IDEMPOTENT: an already-locked keeper is never re-emitted (the flag is the high-water).
  check('theory: already-locked keeper → not re-emitted',
    decideTheories(new Set(['stone-vaun', 'vaun-hoard-sorted', 'vaun-bookshelf-tally']), new Set(['vaun'])).length === 0);
  // PARTIAL CLUSTERS across keepers: only the keeper(s) that CROSS threshold lock, others held.
  const mixed = decideTheories(
    new Set(['stone-vaun', 'vaun-hoard-sorted', 'stone-mara' /* mara has only 1 → held */, 'stone-brann', 'brann-black-moon-toll']),
    new Set(),
  );
  check('theory: partial clusters → only threshold-crossers lock', JSON.stringify(mixed) === JSON.stringify(['vaun', 'brann']));
  // FALL-ORDER DETERMINISM: many keepers coherent at once → emitted in CLUSTERS declaration order.
  const all = new Set<string>();
  for (const c of CLUSTERS) for (const k of c.evidence) all.add(k);
  check('theory: all clusters coherent → all keepers, in fall-order',
    JSON.stringify(decideTheories(all, new Set())) === JSON.stringify(['vaun', 'mara', 'sella', 'orin', 'brann', 'iss']));
  check('theory: deterministic',
    JSON.stringify(decideTheories(new Set(['stone-sella', 'sella-overlay-lake']), new Set())) ===
    JSON.stringify(decideTheories(new Set(['stone-sella', 'sella-overlay-lake']), new Set())));
  // The flag key is the canonical `<keeper>_theory` (the S-E dovetail contract).
  check('theory: flag key is <keeper>_theory', theoryFlag('iss') === 'iss_theory');
  // Alread-locked mid-set: locked vaun + newly-coherent mara → only mara (locked never repeats).
  check('theory: locked one + new one → only the new one',
    JSON.stringify(decideTheories(new Set(['stone-vaun', 'vaun-hoard-sorted', 'stone-mara', 'mara-lectern-lock']), new Set(['vaun']))) === JSON.stringify(['mara']));
}

// ===========================================================================
// relief.ts — the warm-memory exhale after a climax (W3e). Fires once per climax,
// idempotent via the relieved_* flag; never before its climax.
// ===========================================================================
{
  check('relief: no climax → no exhale', decideRelief({}).length === 0);
  check('relief: undercroft_open set → the market exhales',
    decideRelief({ undercroft_open: true }).map((r) => r.relievedFlag).join() === 'relieved_market');
  check('relief: idempotent — already relieved → nothing',
    decideRelief({ undercroft_open: true, relieved_market: true }).length === 0);
  check('relief: iss_caught set → Iss remembered exhales',
    decideRelief({ iss_caught: true }).map((r) => r.bodyKey).join() === 'cardWhoIssFriend');
  check('relief: both climaxes, none relieved → both, in order',
    JSON.stringify(decideRelief({ undercroft_open: true, iss_caught: true }).map((r) => r.climax)) ===
    JSON.stringify(['undercroft_open', 'iss_caught']));
  // Every relief body key must resolve in the Watcher archive (no blank exhale on camera).
  check('relief: every bodyKey resolves in voice.archive',
    RELIEF_BEATS.every((r) => archiveLine(r.bodyKey) != null));
}

// ===========================================================================
// reports.run buildObservationDossiers — the group-relative scoring that makes the
// Tier-0 "it knows you" loop fire (W3). Pure; the DB read/post live in reports.run.ts.
// ===========================================================================
{
  const mb = (groupKey: string, name: string, o: Partial<MeasuredBehavior>): MeasuredBehavior => ({
    groupKey, name, hoardedScore: 0, soloMiningSeconds: 0, distanceFromGroup: 0, forbiddenWordHits: 0,
    bowViolations: 0, darkHoursViolations: 0, ...o,
  });
  const honored = new Map<string, number>([['p1:the_offering', 5]]);

  // A clear hoarder vs a clear wanderer → each leads its own axis at 1.0, the other proportional.
  const ds = buildObservationDossiers(
    [mb('p1', 'vaun', { hoardedScore: 100, distanceFromGroup: 10 }),
     mb('p2', 'sella', { hoardedScore: 10, distanceFromGroup: 100 })],
    honored,
  );
  const p1 = ds.find((d) => d.groupKey === 'p1')!;
  const p2 = ds.find((d) => d.groupKey === 'p2')!;
  check('reports: group-relative — hoarder leads hoards at 1.0', p1.habits.hoards === 1);
  check('reports: group-relative — wanderer leads wanders at 1.0', p2.habits.wanders === 1);
  check('reports: honoredCount looked up on the argmax custom', p1.honoredCount === 5);
  // The hoarder is dominant on hoards for this group → a named report.
  const fired = decidePersonalizedReports({ dossiers: ds, reported: {}, mode: 'auto' }).reports;
  check('reports: the clear hoarder is named', fired.some((r) => r.name === 'vaun' && r.habit === 'hoards'));

  // The compliance-derived axes: the one who most passes markers uncrouched is named `silent`.
  const compliance = buildObservationDossiers(
    [mb('p1', 'orin', { bowViolations: 20 }), mb('p2', 'other', { bowViolations: 1 })],
    new Map(),
  );
  check('reports: silent axis scored from the_bow violations',
    (compliance.find((d) => d.groupKey === 'p1')!.habits.silent ?? 0) === 1);
  check('reports: the marker-passer is named silent',
    decidePersonalizedReports({ dossiers: compliance, reported: {}, mode: 'auto' }).reports
      .some((r) => r.name === 'orin' && r.habit === 'silent'));

  // A FLAT group (identical behavior) names no one — the precision floor (a wrong "it knows you" is worse).
  const flat = buildObservationDossiers(
    [mb('a', 'a', { hoardedScore: 50 }), mb('b', 'b', { hoardedScore: 50 })],
    new Map(),
  );
  check('reports: flat group → no report (precision floor)',
    decidePersonalizedReports({ dossiers: flat, reported: {}, mode: 'auto' }).reports.length === 0);
  // Zero signal everywhere → all scores 0, no report, no throw.
  check('reports: all-zero group → no report, no throw',
    decidePersonalizedReports({ dossiers: buildObservationDossiers([mb('a', 'a', {}), mb('b', 'b', {})], new Map()), reported: {}, mode: 'auto' }).reports.length === 0);
}

if (failures > 0) {
  console.error(`\nshowrunner autonomy: FAILED — ${failures} assertion(s)`);
  process.exit(1);
}
console.log('\nshowrunner autonomy: OK — all autonomy policies hold.');
