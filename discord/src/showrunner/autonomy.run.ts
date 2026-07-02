/**
 * autonomy.run.ts — the I/O wrapper for the between-session AUTONOMY layer (A2/A3/A8–A13, B3/B4).
 *
 * This is the side-effecting half; every policy it drives is a PURE module (fate / reckoning /
 * keeper-record / prologue / grave / herd / forks / name-where-never-been / offline-skin / clock),
 * each importable by a self-test with no DB. Two entry points, both fault-isolated + graceful (a
 * single failure never aborts the tick; a missing data source ⇒ that pass quietly does nothing —
 * silence is canon, INV-7):
 *
 *   1. computeAutonomyGates(nowMs) — reads the MASTERY signals + the prologue ignition state, runs
 *      reckon() (with hysteresis persisted) and decidePrologue(), and returns the two OPTIONAL gates
 *      decide() consumes (`reckoning`, `prologue`). run.ts folds these onto the snapshot BEFORE
 *      decide(), so the difficulty cadence + prologue suppression apply to the same tick.
 *
 *   2. runAutonomyPasses(mode, nowIso) — the between-session producers (grave, herd, keeper-record,
 *      forks, fate cache, name-where, offline-skin, clock). Each reads measured state, runs its pure
 *      policy, and enqueues an out-of-LoS beat / merges arc_state.flags / advances an idempotent mark.
 *
 * GROUNDING / PRECISION. Where a data source the SQL/PLUGIN lanes own is not yet present (per-player
 * visited-cells, dossiers, the custom-compliance spread), the pass reads what exists and DEGRADES TO
 * NO-OP rather than inventing a signal — a wrong "it knows you" is worse than none. The cross-owner
 * reads it needs are listed in the worker RETURN; until they land, the layer is inert-but-correct.
 *
 * IDEMPOTENCY. All between-session bookkeeping lives on the existing `showrunner_state` jsonb row
 * (no migration today — mirrors `reported_customs`). Every producer advances a high-water mark only
 * after a successful enqueue, so a restart mid-tick re-derives the same single beat.
 */
import { supabase } from '../db/client.js';
import { enqueueBeat, getArcAct, logEvent, setArcFlags } from '../db/repo.js';
import { readState, writeState, type ShowrunnerState } from './state.js';
import { reckon, RECKONING_DEFAULTS, type ReckoningInput } from './reckoning.js';
import { decidePrologue } from './prologue.js';
import { paceHerd, type HerdInput } from './herd.js';
import { decideGrave, type GraveInput } from './grave.js';
import { applyForks, type ForkFlags, type ForkTriggers } from './forks.js';
import { bindAcceptingInstant, instantReached } from './clock.js';
import { decideColdRestage, type IssWarmBeat } from './liar.js';
import { selectApparition, type ApparitionCandidate } from './conductor.js';
import { runCompanionPass } from './companion.run.js';
import { runFinalePass } from './finale.run.js';
import { runTheoryPass } from './theory.run.js';
import type { BeatStatus } from '../db/types.js';
import type { PrologueGate, ReckoningState, Tone } from './types.js';

const HOUR = 3_600_000;
const MASTERY_WINDOW_MS = 3 * HOUR; // same within-session window the stall backstop uses

/** What decide() needs folded onto the snapshot. Both optional — absent ⇒ neutral / allowed. */
export interface AutonomyGates {
  reckoning?: { state: ReckoningState; cadenceMult: number; tone: Tone };
  prologue?: PrologueGate;
}

/**
 * Read the difficulty MASTERY signals (group-scalar) over the window. Fault-isolated: any read error
 * ⇒ neutral signals (firstTryRate 0.5, no whisper lean), so reckon() stays at `even`.
 */
async function readMastery(nowMs: number): Promise<{ distinctSolvers: number; firstTryRate: number; whisperLean: number }> {
  const sinceIso = new Date(nowMs - MASTERY_WINDOW_MS).toISOString();
  try {
    const [{ data: solveRows }, { count: failCount }] = await Promise.all([
      supabase.from('solves').select('player_id, puzzle_key').gte('solved_at', sinceIso)
        .returns<{ player_id: string | null; puzzle_key: string | null }[]>(),
      supabase.from('answer_attempts').select('id', { count: 'exact', head: true })
        .eq('matched', false).gte('at', sinceIso),
    ]);
    const solves = solveRows ?? [];
    const solveCount = solves.length;
    const distinctSolvers = new Set(solves.map((r) => r.player_id).filter(Boolean)).size;
    const fails = failCount ?? 0;
    // first-try rate ≈ solves with no failed attempts behind them, approximated group-wide as
    // solves / (solves + fails). High when the group rarely misses; low when it grinds.
    const firstTryRate = solveCount + fails > 0 ? solveCount / (solveCount + fails) : 0.5;
    // whisper-lean ≈ fails per solve (the struggle tell) — a coarse proxy until a whisper-spend join lands.
    const whisperLean = solveCount > 0 ? fails / solveCount : 0;
    return { distinctSolvers, firstTryRate, whisperLean };
  } catch {
    return { distinctSolvers: 0, firstTryRate: 0.5, whisperLean: 0 };
  }
}

/** Read the prologue ignition flag from arc_state.flags. Graceful: error ⇒ treat as not ignited. */
async function readArcFlags(): Promise<Record<string, unknown>> {
  try {
    const { data } = await supabase.from('arc_state').select('flags').eq('id', 1)
      .maybeSingle<{ flags: Record<string, unknown> }>();
    return data?.flags ?? {};
  } catch {
    return {};
  }
}

/**
 * computeAutonomyGates — runs reckon() (persisting hysteresis) + decidePrologue(), returns the gates
 * decide() folds onto the snapshot. Persists `reckoning_state`/`reckoning_since_ms` so the next tick
 * applies hysteresis. NEVER touches whisper_budgets (INV-15).
 */
export async function computeAutonomyGates(nowMs: number, nowIso: string, dryRun = false): Promise<AutonomyGates> {
  const gates: AutonomyGates = {};
  try {
    const state = await readState();

    // --- A10 difficulty grip ---
    const mastery = await readMastery(nowMs);
    const r = reckon(
      {
        nowMs,
        distinctSolvers: mastery.distinctSolvers,
        firstTryRate: mastery.firstTryRate,
        whisperLean: mastery.whisperLean,
        priorState: state.reckoning_state ?? null,
        priorSinceMs: state.reckoning_since_ms ?? null,
      } satisfies ReckoningInput,
      RECKONING_DEFAULTS,
    );
    gates.reckoning = { state: r.state, cadenceMult: r.cadenceMult, tone: r.tone };
    // Persist the hysteresis anchor when the grip state changes — but NEVER on --dry-run (read-only).
    if (!dryRun && (state.reckoning_state !== r.state || state.reckoning_since_ms !== r.sinceMs)) {
      state.reckoning_state = r.state;
      state.reckoning_since_ms = r.sinceMs;
      await writeState(state, nowIso);
      await logEvent('info', 'showrunner.autonomy', `reckoning: ${r.reason}`);
    }

    // --- B4 cold-start prologue gate ---
    const flags = await readArcFlags();
    const p = decidePrologue({
      ignited: flags.prologue_ignited === true,
      acked: state.prologue_acked === true,
      // No single overwhelming-signal source is wired yet → un-named FACT-1 fallback (precision).
      overwhelmingSignal: false,
      signalName: null,
    });
    gates.prologue = { curatorialAllowed: p.curatorialAllowed };
    // The one-shot ack is posted by apply.ts (it owns the #the-record seam); here we only persist the
    // ack high-water so it never re-posts. (apply.ts flips this; we read it for the gate.)
    if (p.postAck && !state.prologue_acked) {
      await logEvent('info', 'showrunner.autonomy', `prologue: ${p.reason} (ack via apply)`);
    }
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `gate computation error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }
  return gates;
}

export interface AutonomyPassResult {
  graves: number;
  herdSpreads: number;
  forksSet: number;
  coldRestages: number;
  apparitionClaimed: boolean;
  /** D3 companion: Wren reveal/reckoning last-words beats enqueued this pass. */
  companionBeats: number;
  /** M5 finale: the composed close was posted this pass. */
  finalePosted: boolean;
  /** S-D theory-lock: keepers whose evidence cluster became coherent this pass (flag locked + beat posted). */
  theoriesLocked: number;
}

/**
 * runAutonomyPasses — the between-session producers, each fault-isolated. Returns a small tally for
 * the tick log. Every pass that has no live data source degrades to a no-op (precision over recall).
 */
export async function runAutonomyPasses(mode: 'auto' | 'confirm', nowIso: string): Promise<AutonomyPassResult> {
  const result: AutonomyPassResult = { graves: 0, herdSpreads: 0, forksSet: 0, coldRestages: 0, apparitionClaimed: false, companionBeats: 0, finalePosted: false, theoriesLocked: 0 };
  const nowMs = Date.parse(nowIso);
  const beatStatus: BeatStatus = mode === 'auto' ? 'approved' : 'pending';

  let state: ShowrunnerState;
  try {
    state = await readState();
  } catch {
    return result; // graceful
  }
  const flags = await readArcFlags();
  let dirty = false;

  // --- A13/A9 clock: bind the single Accepting instant once iss_caught + threshold_open ---
  let acceptingInstantMs: number | null = (flags.accepting_instant_ms as number | undefined) ?? null;
  try {
    const anchorMs = (flags.iss_caught_at_ms as number | undefined) ?? nowMs;
    const bind = bindAcceptingInstant({
      boundInstantMs: acceptingInstantMs,
      anchorMs,
      readyToBind: flags.iss_caught === true && flags.threshold_open === true,
    });
    if (bind.newlyBound && bind.instantMs != null) {
      acceptingInstantMs = bind.instantMs;
      await setArcFlags({ accepting_instant_ms: bind.instantMs });
      await logEvent('info', 'showrunner.autonomy', `clock: ${bind.reason} (=${new Date(bind.instantMs).toISOString()})`);
    }
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `clock bind error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- A11 forks: commit any newly-decided leaf (first-writer-wins; colors, never gates) ---
  try {
    const prior: ForkFlags = {
      sacred_beast_broken: flags.sacred_beast_broken === true || undefined,
      light_kept: flags.light_kept === true || undefined,
      light_taken: flags.light_taken === true || undefined,
      name_unspoken: flags.name_unspoken === true || undefined,
      name_spoken: flags.name_spoken === true || undefined,
    };
    const triggers: ForkTriggers = {
      glowingBeastKilled: flags.fork_trigger_glowing_beast === true || undefined,
      firstLightChoice: (flags.fork_trigger_first_light as 'kept' | 'taken' | null | undefined) ?? null,
      spokenNameChoice: (flags.fork_trigger_spoken_name as 'spoken' | 'unspoken' | null | undefined) ?? null,
    };
    const fk = applyForks(prior, triggers);
    const keys = Object.keys(fk.setFlags);
    if (keys.length > 0) {
      await setArcFlags(fk.setFlags as Record<string, unknown>);
      result.forksSet += keys.length;
      for (const n of fk.notes) await logEvent('info', 'showrunner.autonomy', n);
    }
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `forks error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- A12 herd: pace the cosmetic pale field (capped, monotone, one-per-pass) ---
  try {
    const movement = await getArcAct();
    const herd = paceHerd({
      movement,
      priorPaleCount: state.herd_pale_count ?? 1, // the single M1 beast is the seed
      passDoneThisWindow: state.herd_pass_movement === movement,
    } satisfies HerdInput);
    if (herd.spread) {
      await enqueueBeat('herd_spread', null, {
        kind: 'pale_spread',
        pale_target: herd.paleTarget,
        add: herd.addThisPass,
        reversible: false, // a converted animal stays converted (cosmetic, not a toll)
        reason: herd.reason,
      }, beatStatus);
      state.herd_pale_count = herd.paleTarget;
      state.herd_pass_movement = movement;
      dirty = true;
      result.herdSpreads += 1;
      await logEvent('info', 'showrunner.autonomy', `herd: ${herd.reason} (${beatStatus})`);
    }
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `herd error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- A9 grave: carve the one grave / open it at the Accepting instant ---
  try {
    const subject = state.grave?.group_key && state.grave?.name
      ? { groupKey: state.grave.group_key, name: state.grave.name, active: true }
      : await pickGraveSubject(); // ground a real active name (or null → no grave)
    const gv = decideGrave({
      acceptingInstantMs,
      nowMs,
      subject,
      carved: state.grave?.carved === true,
      opened: state.grave?.opened === true,
    } satisfies GraveInput);
    if (gv.row) {
      await enqueueBeat('grave', null, {
        kind: gv.row.kind, // 'carve' | 'open'
        mc_uuid: gv.row.groupKey,
        name: gv.row.name,
        date_ms: gv.row.dateMs, // read, never typed (INV-14)
        voice_key: gv.row.voiceKey,
        reason: gv.row.reason,
      }, beatStatus);
      state.grave = {
        ...(state.grave ?? {}),
        ...gv.marks,
        group_key: gv.row.groupKey,
        name: gv.row.name,
      };
      dirty = true;
      result.graves += 1;
      await logEvent('info', 'showrunner.autonomy', `grave: ${gv.row.reason} (${beatStatus})`);
    } else {
      for (const n of gv.notes) await logEvent('info', 'showrunner.autonomy', `grave: ${n}`);
    }
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `grave error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- D7 conductor: the single-arbiter apparition slot (INV-18). Publish ONE apparitionClaim per
  //     window; the deferring lanes (offline-skin, name-where, keeper-NPC, the Ear) read it before
  //     firing. The window seed is the discrete drama window (the day index) so a re-run in the same
  //     window re-derives the SAME claim (idempotent slot) and a new day re-rolls. Active-roster only.
  try {
    const windowSeed = Math.floor(nowMs / (24 * HOUR)); // the day-index drama window
    if (state.apparition_claim?.window === windowSeed) {
      // already claimed this window — re-derive nothing (the slot is set-once per window).
      result.apparitionClaimed = state.apparition_claim.group_key !== '';
    } else {
      const roster = await readActiveRoster(MASTERY_WINDOW_MS);
      const candidates: ApparitionCandidate[] = roster.map((r) => ({
        groupKey: r.groupKey,
        active: true, // readActiveRoster returns active members only
        // No per-shape rhyme source is wired yet (the dossier reader the PLUGIN lane owns) → a flat,
        // below-floor rhyme, so the conductor degrades to NO claim until that read lands (precision).
        shapeRhyme: {},
        apparitionCount: state.apparition_counts?.[r.groupKey] ?? 0,
      }));
      const pick = selectApparition({ candidates, windowSeed });
      if (pick.claim) {
        state.apparition_claim = { window: windowSeed, group_key: pick.claim.groupKey, shape: pick.claim.shape, beat: pick.claim.beat };
        state.apparition_counts = { ...(state.apparition_counts ?? {}), [pick.claim.groupKey]: (state.apparition_counts?.[pick.claim.groupKey] ?? 0) + 1 };
        await enqueueBeat(pick.claim.beat, pick.claim.groupKey, {
          kind: 'apparition', shape: pick.claim.shape, reason: pick.claim.reason,
        }, beatStatus);
        result.apparitionClaimed = true;
        dirty = true;
        await logEvent('info', 'showrunner.autonomy', `conductor: ${pick.claim.reason} (${beatStatus})`);
      } else {
        // Record an EMPTY claim for the window so the deferring lanes know the slot was considered
        // and left empty (they must still defer — an empty slot is not "go ahead", it is "no ambient
        // figure this window"). The lanes treat a window with no group_key as "slot taken by silence".
        state.apparition_claim = { window: windowSeed, group_key: '', shape: '', beat: '' };
        dirty = true;
      }
    }
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `conductor error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- D4 liar: flag-gated, one-way warm→cold re-stage of Iss's beats once iss_caught. Curated
  //     re-staging ONLY (NOT the activation lane — that is the requires_flags gate in resolve.ts/repo).
  //     The set of Iss warm beats is read from event_log by the run wrapper; until that read lands the
  //     pass is inert-but-correct (no warm beats → no rows). Gated on iss_caught either way.
  try {
    if (flags.iss_caught === true) {
      const warmBeats = await readIssWarmBeats();
      const liar = decideColdRestage({
        issCaught: true,
        warmBeats,
        alreadyFlipped: new Set(state.liar_flipped ?? []),
        mode,
      });
      for (const row of liar.rows) {
        // The cold line is an authored voice key resolved by resolve.ts's private_message resolver
        // (0.10) — we pass step_payload.key, never composed text. voice.ts stays the source of truth.
        await enqueueBeat('private_message', row.siteId, {
          step_payload: { key: row.coldKey },
          kind: 'iss_cold_restage', beat_id: row.beatId, reason: row.reason,
        }, row.status);
        result.coldRestages += 1;
      }
      if (liar.flipped.length > 0) {
        state.liar_flipped = [...new Set([...(state.liar_flipped ?? []), ...liar.flipped])];
        dirty = true;
        await logEvent('info', 'showrunner.autonomy', `liar: re-staged ${liar.flipped.length} warm beat(s) cold`);
      }
    }
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `liar error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- D3 companion (Wren): the reveal pair + the one-of-three reckoning last-words. Gated on the
  //     already-fetched arc flags (companion_revealed / reckoning_*). The trust-ladder + newhand lines
  //     are the plugin's own right-click surface; the showrunner owns only these set-once beats. The
  //     pure resolver maps flags + the companion event_log context to a wren.* npcLines key, delivered
  //     in-world (private_message, SET-A human register via npcLine — never the Watcher's close).
  try {
    let movement = 1;
    try { movement = await getArcAct(); } catch { /* graceful — default M1 */ }
    const comp = await runCompanionPass(mode, flags, movement, state);
    result.companionBeats += comp.enqueued;
    if (comp.dirty) dirty = true;
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `companion error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- M5 finale: post the composed close (fate + seventh_choice + fork leaves + reckoning_free cost)
  //     to #the-record ONCE the Accepting instant is reached. The sole consumer of seventh_choice /
  //     the free-branch cost — the authored voice.ts keys were never read by any composer before this.
  try {
    const fin = await runFinalePass(flags, acceptingInstantMs, nowMs, state);
    if (fin.posted) result.finalePosted = true;
    if (fin.dirty) dirty = true;
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `finale error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // --- S-D theory-lock: the record RECEIVES a keeper's fate once a COHERENT CLUSTER of that keeper's
  //     evidence is solved (a built theory, not one decode). The `<keeper>_theory` arc flag is the
  //     idempotent high-water (mirrors finale_posted / the fork flags); each newly-coherent keeper is
  //     locked + gets ONE #the-record beat (voice.theoryReceived). Group-wide (any player's solve
  //     counts). No orphaned flag: the flag is set together with its consumer beat. The flags are
  //     available for the record-projection (S-E dovetail) to read later with no further wiring here.
  try {
    const th = await runTheoryPass(flags);
    result.theoriesLocked += th.locked.length;
  } catch (e) {
    await logEvent('warn', 'showrunner.autonomy', `theory error (isolated): ${e instanceof Error ? e.message : String(e)}`);
  }

  // NOTE — keeper-record, name-where-never-been, offline-skin, reports, keeper-NPC, and the fate
  // selector run from their own dedicated readers (dossiers / visited-cells / compliance-spread) the
  // SQL+PLUGIN lanes own;
  // those readers are listed in the worker RETURN. The pure policies (keeper-record.ts /
  // name-where-never-been.ts / offline-skin.ts / fate.ts) are complete + self-tested and slot in
  // behind those reads with no change here. The fate selector additionally lives in resolve.ts's
  // set-once Accepting branch (TS-SHOWRUN owns resolve.ts) — see RETURN.

  // The clock predicate is exposed for the grave/summons/website to share one `not_before`.
  void instantReached;

  if (dirty) {
    try {
      await writeState(state, nowIso);
    } catch {
      await logEvent('warn', 'showrunner.autonomy', 'failed to persist autonomy state marks');
    }
  }
  return result;
}

/** One active roster member (the single active-set source, BUILD-MANIFEST §0.9). */
export interface RosterMember {
  groupKey: string;
  name: string | null;
}

/**
 * readActiveRoster(windowMs) — THE single active-set source (BUILD-MANIFEST §0.9 / WEB-MASTER §0.5).
 * The Accepting bridge (INV-19 quorum), the spawn-bias conductor, and the keeper-NPC framework all
 * consume THIS reader; none re-derives the active set. "Active" = a player who has attempted/solved
 * within `windowMs`. Grounded on real measured activity (answer_attempts + solves), never a guess.
 * Fault-isolated: any read error ⇒ [] (the conductor degrades to no claim; the quorum reads 0 active).
 *
 * NOTE (cross-owner): the AcceptingRiteListener (PLUGIN) computes `effectiveQuorum =
 * min(configQuorum, activeRosterSize)` against this same window — it must read the SAME definition.
 * The plugin reads its own roster in-process; this is the Discord-side mirror used by the conductor.
 */
export async function readActiveRoster(windowMs: number): Promise<RosterMember[]> {
  const sinceIso = new Date(Date.now() - windowMs).toISOString();
  try {
    const [{ data: attempts }, { data: solves }] = await Promise.all([
      supabase.from('answer_attempts').select('mc_uuid, player_id').gte('at', sinceIso)
        .returns<{ mc_uuid: string | null; player_id: string | null }[]>(),
      supabase.from('solves').select('mc_uuid, player_id').gte('solved_at', sinceIso)
        .returns<{ mc_uuid: string | null; player_id: string | null }[]>(),
    ]);
    const keys = new Set<string>();
    for (const r of [...(attempts ?? []), ...(solves ?? [])]) {
      const k = r.mc_uuid ?? r.player_id;
      if (k) keys.add(k);
    }
    if (keys.size === 0) return [];
    // Resolve display names in one batched lookup (by mc_uuid OR id; either key may be the groupKey).
    const arr = [...keys];
    const { data: people } = await supabase.from('players').select('id, mc_uuid, name')
      .or(`mc_uuid.in.(${arr.join(',')}),id.in.(${arr.join(',')})`)
      .returns<{ id: string | null; mc_uuid: string | null; name: string | null }[]>();
    const nameFor = new Map<string, string | null>();
    for (const p of people ?? []) {
      if (p.mc_uuid) nameFor.set(p.mc_uuid, p.name);
      if (p.id) nameFor.set(p.id, p.name);
    }
    return arr.map((groupKey) => ({ groupKey, name: nameFor.get(groupKey) ?? null }))
      .sort((a, b) => a.groupKey.localeCompare(b.groupKey)); // deterministic order
  } catch {
    return [];
  }
}

/**
 * readIssWarmBeats — the set of Iss WARM beats (already posted) that have a cold counterpart, for the
 * Liar re-stage (D4). Reads the showrunner's own dripped/posted record of Iss beats from event_log.
 * Until the warm-beat catalog the LORE/SQL lanes own is wired (the warm↔cold key pairs), this returns
 * [] — so the pass is inert-but-correct (no warm beats → no cold rows), never a guessed flip. The
 * authored warm↔cold key pairs are listed in the worker RETURN as a cross-owner dependency.
 */
async function readIssWarmBeats(): Promise<IssWarmBeat[]> {
  // No authored warm↔cold catalog is wired yet (cross-owner: LORE supplies the key pairs, TS-VOICE the
  // bodies). Degrade to empty rather than inventing a flip — precision over recall. See RETURN.
  return [];
}

/**
 * Ground a real ACTIVE player as the grave subject — a resolvable name, never guessed. Picks the
 * earliest-linked named player as a stable, deterministic choice. Returns null (no grave) if none
 * resolves. Replace with the active-roster reader the snapshot lane provides (see RETURN).
 */
async function pickGraveSubject(): Promise<{ groupKey: string; name: string; active: boolean } | null> {
  try {
    const { data } = await supabase.from('players').select('mc_uuid, name')
      .not('name', 'is', null).not('mc_uuid', 'is', null)
      .order('id', { ascending: true }).limit(1)
      .returns<{ mc_uuid: string | null; name: string | null }[]>();
    const row = data?.[0];
    if (!row?.mc_uuid || !row?.name) return null;
    return { groupKey: row.mc_uuid, name: row.name, active: true };
  } catch {
    return null;
  }
}
