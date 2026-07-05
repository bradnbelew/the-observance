/**
 * offline-skin.run.ts — the I/O wrapper for the offline-skin apparition (B3 `offline-skin-apparition`,
 * FACT 9, INV-16). The pure orchestrator (offline-skin.ts `selectGlimpse`) was built + self-tested but
 * had NO caller in production — this closes that seam, mirroring keeper-record.run.ts / unlit-deep.run.ts:
 *
 *   1. read OFFLINE candidates: every known player (`players`), filtered to those NOT in the active
 *      roster this window and whose `last_seen` is stale past a grace window (so a player who just quit
 *      mid-tick isn't instantly worn — the design doc's Risk D pairs with the plugin's own fire-time
 *      re-check in NamedMobBeat.doEnact, which is the authoritative gate against a login race),
 *   2. compute each candidate's shapeRhyme from the SAME group-relative dossier scorer the Tier-0 reports
 *      and the Hold-Book use (buildObservationDossiers), reusing keeper-record.run.ts's documented
 *      habit→keeper rhyme so this file invents no new signal: hoards→watcher_at_edge (Vaun), wanders→
 *      surface_walker (Sella), silent→stoop (Orin) — the three shapes offline-skin/conductor share,
 *      spends-words is folded into the strongest of the three since Iss has no dedicated apparition shape,
 *   3. read the carve lane's claimed cells THIS window (name-where-never-been.run.ts writes them onto
 *      showrunner_state so the separation law — INV-16, never co-locate a worn skin with that same
 *      player's active name-carve — is enforced without a second table),
 *   4. call the pure `selectGlimpse`, and on a real decision enqueue a `named_mob` beat targeting a
 *      PRESENT witness (the plugin's BeatRequest.hasTarget() requires an ONLINE target — the worn player
 *      is never the target, only the skin), then advance the one-shot `worn_skins` high-water.
 *
 * PHASE. `deniable` (M3, no name-tag) fires autonomously once FACT 9 groundwork exists; `named` (M4) is
 * the single human-approved beat — gated on `arc_state.flags.offline_skin_named_approved` (a dashboard
 * toggle), never auto-approved. Both phases post the matching voice.ts line to #the-record once the beat
 * is enqueued successfully (mirrors unlit-deep.run.ts's post-then-mark discipline).
 *
 * GROUNDING / DEGRADE. No active witness, no offline candidate with a confident shape-rhyme, or any read
 * failure → no-op (silence is canon, INV-7). This file invents no data: shape-rhyme reuses the measured
 * dossier signals; offline status reuses the existing `players.last_seen` + active-roster reads (the
 * plugin's PresenceListener already upserts `players(mc_uuid, name, last_seen)` on join AND quit — no new
 * plugin-side column needed).
 */
import { supabase } from '../db/client.js';
import { enqueueBeat, getArcFlags, logEvent, readCustomViolations, readDossiers } from '../db/repo.js';
import { readActiveRoster } from './autonomy.run.js';
import { buildObservationDossiers, type MeasuredBehavior } from './reports.js';
import { postToTheRecord } from './discord.js';
import { voice } from '../voice.js';
import {
  selectGlimpse,
  type ApparitionShape,
  type GlimpsePhase,
  type OfflineCandidate,
  type OfflineSkinInput,
} from './offline-skin.js';
import type { ShowrunnerState } from './state.js';
import type { BeatStatus } from '../db/types.js';

/** A witness must have been last-seen at least this long ago to count as OFFLINE (grace window against
 *  a mid-tick quit racing the plugin's own fire-time re-check). */
const OFFLINE_GRACE_MS = 5 * 60_000;

/** One raw `players` row we need for offline-candidate resolution. */
interface PlayerRow {
  mc_uuid: string;
  name: string | null;
  last_seen: string | null;
}

/** Read every known player with a last_seen, for the offline-candidate pool. Fault-isolated → []. */
async function readAllPlayers(): Promise<PlayerRow[]> {
  try {
    const { data, error } = await supabase
      .from('players')
      .select('mc_uuid, name, last_seen')
      .returns<PlayerRow[]>();
    if (error || !data) return [];
    return data;
  } catch {
    return [];
  }
}

/** hoards→watcher_at_edge (Vaun), wanders→surface_walker (Sella), silent→stoop (Orin) — the same
 *  chorus-habit rhyme keeper-record.run.ts's keeperSignals() documents, pointed at the three shapes the
 *  bestiary's apparition vocabulary shares (conductor.ts SHAPE_BEAT). spends-words/reads have no
 *  dedicated shape here (Iss/Mara are not apparition shapes) so they are left out of the rhyme map
 *  rather than forced onto an unrelated shape — a flat/absent signal correctly fails the precision floor.
 */
function shapeRhymeFromHabits(habits: Readonly<Partial<Record<string, number>>>): Partial<Record<ApparitionShape, number>> {
  const s: Partial<Record<ApparitionShape, number>> = {};
  if (typeof habits.hoards === 'number') s.watcher_at_edge = habits.hoards;
  if (typeof habits.wanders === 'number') s.surface_walker = habits.wanders;
  if (typeof habits.silent === 'number') s.stoop = habits.silent;
  return s;
}

export interface OfflineSkinPassResult {
  /** a glimpse beat was enqueued + its voice line posted this pass. */
  fired: boolean;
}

/**
 * runOfflineSkinPass — one autonomy-tick attempt at the offline-skin glimpse. Reads the offline pool +
 * their measured shape-rhyme + the carve lane's claimed cells, runs `selectGlimpse`, and on a decision
 * enqueues `named_mob` targeting a present witness + posts the matching voice line. One-shot per
 * (player, phase) via `state.worn_skins`; the caller persists `state` when this returns `dirty`.
 */
export async function runOfflineSkinPass(
  mode: 'auto' | 'confirm',
  state: ShowrunnerState,
  nowMs: number,
): Promise<OfflineSkinPassResult & { dirty: boolean }> {
  const beatStatus: BeatStatus = mode === 'auto' ? 'approved' : 'pending';
  const result = { fired: false, dirty: false };

  try {
    const [players, roster, dossierRows, violations, flags] = await Promise.all([
      readAllPlayers(),
      readActiveRoster(3 * 60 * 60_000), // same MASTERY_WINDOW_MS the conductor/roster reader uses
      readDossiers(),
      readCustomViolations(),
      getArcFlags(),
    ]);
    if (players.length === 0) return result;

    // A present WITNESS: an active-roster member who is currently online (the beat's hard target
    // requirement — BeatRequest.hasTarget() needs an online player). Deterministic pick (lowest
    // groupKey) so a re-run of the same tick proposes the same witness.
    const activeKeys = new Set(roster.map((r) => r.groupKey));
    const witnessKey = [...activeKeys].sort()[0];
    if (!witnessKey) return result; // no one present to witness the glimpse — no-op

    const honored = new Map<string, number>();
    const violated = new Map<string, number>();
    for (const v of violations) {
      honored.set(`${v.groupKey}:${v.customKey}`, v.honoredCount);
      violated.set(`${v.groupKey}:${v.customKey}`, v.violatedCount);
    }
    const behaviors: MeasuredBehavior[] = dossierRows.map((r) => ({
      ...r,
      bowViolations: violated.get(`${r.groupKey}:the_bow`) ?? 0,
      darkHoursViolations: violated.get(`${r.groupKey}:the_dark_hours`) ?? 0,
    }));
    const obsByKey = new Map(buildObservationDossiers(behaviors, honored).map((d) => [d.groupKey, d]));

    const wornCounts = state.worn_skins ?? {};
    const phase: GlimpsePhase = flags.iss_caught === true ? 'named' : 'deniable';
    const namedApproved = flags.offline_skin_named_approved === true;

    const candidates: OfflineCandidate[] = players
      .filter((p) => p.mc_uuid && p.name && !activeKeys.has(p.mc_uuid))
      .filter((p) => {
        if (!p.last_seen) return true; // never seen online this run — treat as offline
        const lastSeenMs = Date.parse(p.last_seen);
        return !Number.isFinite(lastSeenMs) || nowMs - lastSeenMs >= OFFLINE_GRACE_MS;
      })
      .map((p) => ({
        groupKey: p.mc_uuid,
        name: p.name,
        offline: true,
        shapeRhyme: shapeRhymeFromHabits(obsByKey.get(p.mc_uuid)?.habits ?? {}),
        wornCount: wornCounts[`${p.mc_uuid}|${phase}`] ?? 0,
      }));
    if (candidates.length === 0) return result;

    // The separation law (INV-16): cells the carve lane (name-where-never-been) is using THIS window,
    // keyed by the SAME groupKey. name-where-never-been.run.ts publishes this onto showrunner_state so
    // neither lane needs to know the other's internals — just the shared claim map. `selectGlimpse`'s own
    // collision test compares a `proposedCell` against these claims; a `named_mob` apparition has no
    // literal "cell" of its own (it spawns relative to the witness, not a heatmap cell), so instead of a
    // cell match we propose the CLAIMED cell itself whenever THIS candidate has an active carve — the
    // strongest, unambiguous form of "would collide" — so `selectGlimpse` correctly skips that candidate
    // without ever falsely colliding a candidate who has no active claim at all.
    const carveClaimsByPlayer: Record<string, ReadonlySet<string>> = {};
    for (const [gk, cell] of Object.entries(state.carve_active_claims ?? {})) {
      carveClaimsByPlayer[gk] = new Set([cell]);
    }
    const candidateWithClaim = candidates.find((c) => carveClaimsByPlayer[c.groupKey] != null);
    const proposedCell = candidateWithClaim ? [...carveClaimsByPlayer[candidateWithClaim.groupKey]!][0]! : null;

    const input: OfflineSkinInput = {
      candidates,
      phase,
      carveClaimsByPlayer,
      proposedCell,
      namedApproved,
    };
    const decision = selectGlimpse(input);
    if (!decision.glimpse) {
      for (const n of decision.notes) await logEvent('info', 'showrunner.offline_skin', n);
      return result;
    }
    const g = decision.glimpse;

    const ok = await enqueueBeat(
      'named_mob',
      witnessKey,
      {
        entity: 'WARDEN',
        fallback_entity: 'WARDEN',
        distance: 12,
        silent: true,
        no_ai_drift: true,
        invulnerable: true,
        glowing: false,
        skin_player: g.groupKey,
        offline_only: true,
        name_visible: g.nameTag, // deniable phase NEVER shows a name-tag (selectGlimpse enforces this)
        name: g.nameTag ? g.name : null,
        kind: 'offline_skin_apparition',
        shape: g.shape,
        phase: g.phase,
        reason: g.reason,
      },
      beatStatus,
    ).then(() => true).catch(async (e) => {
      await logEvent('warn', 'showrunner.offline_skin', `enqueue failed (isolated): ${e instanceof Error ? e.message : String(e)}`);
      return false;
    });
    if (!ok) return result;

    const line = g.phase === 'named' ? voice.offlineSkinNamed(g.name) : null;
    if (line) {
      const posted = await postToTheRecord(line);
      if (!posted) {
        await logEvent('warn', 'showrunner.offline_skin', 'named beat enqueued but #the-record post failed (isolated) — worn-count still advances, the beat itself carries the moment');
      }
    }

    state.worn_skins = { ...wornCounts, [`${g.groupKey}|${g.phase}`]: (wornCounts[`${g.groupKey}|${g.phase}`] ?? 0) + 1 };
    result.dirty = true;
    result.fired = true;
    await logEvent('info', 'showrunner.offline_skin', `glimpse: ${g.reason} (${beatStatus})`);
    return result;
  } catch (e) {
    await logEvent('warn', 'showrunner.offline_skin', `pass error (isolated): ${e instanceof Error ? e.message : String(e)}`);
    return result;
  }
}
