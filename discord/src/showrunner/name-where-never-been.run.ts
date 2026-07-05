/**
 * name-where-never-been.run.ts — the I/O wrapper for the carve selector (A8 `name-where-never-been`,
 * FACT 16, INV-14/16). The pure WHO/WHERE policy (name-where-never-been.ts `selectCarve`) was built +
 * self-tested but had NO caller in production — this closes that seam, mirroring keeper-record.run.ts /
 * theory.run.ts:
 *
 *   1. read the group-avoided + used cells from `heatmap_cells` + `showrunner_state.carved_cells`,
 *   2. read each ACTIVE player's proof-of-absence set (see the DESIGN-CALL note below — this is the one
 *      real simplification in this file, spelled out so a future hand can upgrade it cleanly),
 *   3. call the pure `selectCarve`, and on a decision enqueue a `sign_write` beat at the chosen
 *      `carve_anchor_*` site with the rune-encoded lines, then advance the one-shot `carved_cells` /
 *      `carve_counts` high-water and publish the live claim onto `state.carve_active_claims` (the
 *      separation-law hand-off offline-skin.run.ts reads).
 *
 * DESIGN-CALL: GLOBAL-HEATMAP PROXY, NOT TRUE PER-PLAYER PROOF-OF-ABSENCE.
 * The design doc (design/ideas/name-where-never-been.md §2e/§4 RISK-1) is explicit that the FAITHFUL
 * mechanic needs a genuinely NEW plugin-side data producer: a per-player `player_visited_cells` set
 * (`LocationSampler` → a new coarse per-UUID table), because the plugin's actual `HeatmapAccumulator` /
 * `heatmap_cells` table (dashboard/supabase/migrations/0001_init.sql) is AGGREGATE ONLY —
 * `heatmap_cells(world, cell_x, cell_z, visits)` carries no player column at all (verified by reading
 * both the Java accumulator and the SQL). Building that per-player producer is plugin territory (off
 * limits for this fix — another agent may be editing plugin/src concurrently, and the task instructions
 * are explicit that new plugin-side data producers are out of scope here).
 *
 * So THIS wrapper implements the SIMPLIFIED, still-faithful-to-the-spine version the design doc itself
 * names as the fallback when the per-player set isn't available: a cell is "proof-of-absence eligible"
 * when its AGGREGATE `heatmap_cells.visits` is ZERO across every player (nobody, including the subject,
 * has ever registered a visit there) — rather than "this ONE named player individually never visited it
 * while others did." This is strictly a subset of the true mechanic (zero-across-everyone implies
 * zero-for-the-subject), so it can NEVER produce a false "you've never been here" — the precision law
 * (§1d, "a wrong callout is worse than none") still holds exactly. What it gives up is the M2→M3 staging
 * the design doc describes (a cell some OTHERS have visited but this ONE subject provably has not); with
 * only aggregate data, every fired carve reads at the more conservative "the whole group has avoided
 * this ground" register, which is still true, still verifiable, and still lands FACT 16 — it just can't
 * yet do the sharper "you personally have never been here, but the group has" bite the full design
 * intends. If a future hand wires the real `player_visited_cells` producer (plugin-side), this wrapper
 * should be repointed to read it and pass real per-player `visitedCells` sets to `selectCarve` — the pure
 * policy already fully supports that; only this reader needs to change.
 *
 * REGISTER + THE SEPARATION LAW. The carve's own text is the shared rune alphabet forge output — the
 * pure module documents this is deterministic, never LLM-authored, so this wrapper composes rune lines
 * directly (no voice.ts call for the sign body). The showrunner's #the-record drip line IS voice-sourced
 * (a new `voice.ts` key would be needed for a public announcement; until one exists this wrapper stays
 * silent on #the-record and lets the in-world discovery carry the moment — matching the design doc's own
 * "no jank" preference for the carve to be FOUND, not announced).
 */
import { supabase } from '../db/client.js';
import { enqueueBeat, logEvent } from '../db/repo.js';
import { readActiveRoster } from './autonomy.run.js';
import { substitution } from '../forge/ciphers.js';
import { selectCarve, type CarveAnchor, type CarveSelectorInput, type PlayerPresence } from './name-where-never-been.js';
import type { ShowrunnerState } from './state.js';
import type { BeatStatus } from '../db/types.js';

/** The three placeholder carve-anchor sites authored in sites.yml (INTEGRATION-V2 A8). Their real
 *  world coords are resolved server-side by site_id; this wrapper never needs literal x/y/z. */
const CARVE_ANCHOR_SITE_IDS = ['carve_anchor_01', 'carve_anchor_02', 'carve_anchor_03'] as const;

interface HeatmapCellRow {
  world: string;
  cell_x: number;
  cell_z: number;
  visits: number | null;
}

/** Read every heatmap_cells row. Fault-isolated → [] (the pass degrades to no-op, never invents a cell). */
async function readHeatmapCells(): Promise<HeatmapCellRow[]> {
  try {
    const { data, error } = await supabase
      .from('heatmap_cells')
      .select('world, cell_x, cell_z, visits')
      .returns<HeatmapCellRow[]>();
    if (error || !data) return [];
    return data;
  } catch {
    return [];
  }
}

function cellId(world: string, cellX: number, cellZ: number): string {
  return `${world}:${cellX}:${cellZ}`;
}

export interface NameWhereNeverBeenPassResult {
  /** a carve beat was enqueued this pass. */
  fired: boolean;
}

/**
 * runNameWhereNeverBeenPass — one autonomy-tick attempt at the living-name carve. Reads the aggregate
 * heatmap (the group-avoided AND — per the DESIGN-CALL above — the proof-of-absence signal), the active
 * roster, and the anchors already used, runs `selectCarve`, and on a decision enqueues `sign_write` at
 * the chosen anchor + publishes the active claim for offline-skin.run.ts's separation-law read.
 */
export async function runNameWhereNeverBeenPass(
  mode: 'auto' | 'confirm',
  state: ShowrunnerState,
): Promise<NameWhereNeverBeenPassResult & { dirty: boolean }> {
  const beatStatus: BeatStatus = mode === 'auto' ? 'approved' : 'pending';
  const result = { fired: false, dirty: false };

  try {
    const [cells, roster] = await Promise.all([
      readHeatmapCells(),
      readActiveRoster(3 * 60 * 60_000), // same MASTERY_WINDOW_MS the roster reader elsewhere uses
    ]);

    const usedCells = new Set(state.carved_cells ?? []);
    // Anchors already used (one per site — INV-permanence: never re-carve a spot). The literal cellId
    // is a stable, opaque per-site token; it never needs to match a real heatmap cell since the sites'
    // coords are placeholders resolved by site_id (AbstractBeat.anchor()), not by literal x/z lookup.
    const anchors: CarveAnchor[] = CARVE_ANCHOR_SITE_IDS
      .map((siteId) => ({ siteId, cellId: `site:${siteId}` }))
      .filter((a) => !usedCells.has(a.cellId));
    if (anchors.length === 0) return result; // every anchor already carved this arc

    // groupVisitedCells: the AGGREGATE cells with ANY recorded visit — the group-avoided test (§1d.3).
    // Only cells with visits > 0 count as "the group has been here"; a visits===0 row (or no row at all)
    // is avoided ground. This is real, measured heatmap data — no simplification on this half.
    const groupVisitedCells = new Set(
      cells.filter((c) => (c.visits ?? 0) > 0).map((c) => cellId(c.world, c.cell_x, c.cell_z)),
    );

    // DESIGN-CALL (see file header): the "per-player visited-cells" proof-of-absence set is APPROXIMATED
    // from the aggregate heatmap as "every cell with a recorded visit, by anyone" — so a player's
    // `visitedCells` is really "the group's visited set," which can only ever be a SUPERSET of what that
    // player individually visited. selectCarve's proof-of-absence test (`!visited.has(cellId)`) then
    // requires the anchor to be OUTSIDE that superset — i.e. zero visits from anyone — which is strictly
    // safe (never a false "never been here") but coarser than a true per-player difference. Every named,
    // active player shares this SAME approximated set (there is no per-player differentiation available
    // without the plugin-side producer), so the "T-never-visited" and "group-avoided" tests collapse to
    // one aggregate-zero-visits test — faithful to the precision law, simplified on specificity.
    const approximatedVisitedCells: ReadonlySet<string> = groupVisitedCells;

    const carveCounts = state.carve_counts ?? {};
    const activePlayers: PlayerPresence[] = roster
      .filter((r) => r.name)
      .map((r) => ({
        groupKey: r.groupKey,
        name: r.name,
        // Every active player gets the SAME approximated proof-set (the design-call above) rather than
        // `null` (which would mean "no proof at all, never carve anyone") — a global-zero-visits anchor
        // is a genuine (if coarser) proof of absence for every player, since nobody has been there.
        visitedCells: approximatedVisitedCells,
        carvedCount: carveCounts[r.groupKey] ?? 0,
      }));
    if (activePlayers.length === 0) return result;

    const input: CarveSelectorInput = { activePlayers, anchors, groupVisitedCells, usedCells };
    const decision = selectCarve(input);
    if (!decision.carve) {
      for (const n of decision.notes) await logEvent('info', 'showrunner.name_where', n);
      return result;
    }
    const c = decision.carve;

    const line2 = 'no visits here. the name is older than the path to it.';
    // The carve's own text is the shared rune-substitution cipher (forge/ciphers.ts `substitution`),
    // the SAME alphabet the group learns at the teaching stones (runes.ts renders the glyph forms; the
    // in-world sign carries the glyph-id string, shown as runes by the client resource pack's font).
    const runeLines = [substitution.encode('kept here before you'), substitution.encode(c.name), substitution.encode(line2), ''];

    const ok = await enqueueBeat(
      'sign_write',
      null,
      {
        lines: runeLines,
        place_if_missing: true,
        material: 'OAK_SIGN',
        glowing: false,
        kind: 'name_where_never_been',
        subject_group_key: c.groupKey,
        reason: c.reason,
      },
      beatStatus,
      c.siteId,
    ).then(() => true).catch(async (e) => {
      await logEvent('warn', 'showrunner.name_where', `enqueue failed (isolated): ${e instanceof Error ? e.message : String(e)}`);
      return false;
    });
    if (!ok) return result;

    state.carved_cells = [...usedCells, c.cellId];
    state.carve_counts = { ...carveCounts, [c.groupKey]: (carveCounts[c.groupKey] ?? 0) + 1 };
    // Publish the live claim for offline-skin.run.ts's separation-law read (INV-16): this player's name
    // is being carved at this cell THIS window, so a worn-skin glimpse must not collide with it.
    state.carve_active_claims = { ...(state.carve_active_claims ?? {}), [c.groupKey]: c.cellId };
    result.dirty = true;
    result.fired = true;
    await logEvent('info', 'showrunner.name_where', `carve: ${c.reason} (${beatStatus})`);
    return result;
  } catch (e) {
    await logEvent('warn', 'showrunner.name_where', `pass error (isolated): ${e instanceof Error ? e.message : String(e)}`);
    return result;
  }
}
