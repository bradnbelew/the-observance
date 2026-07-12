import { createAdminClient } from "@/lib/supabase/admin";
import { requireAuthor } from "@/lib/author-auth";
import { signOutAuthor } from "@/app/author/login/actions";
import { existsSync } from "node:fs";
import { join } from "node:path";
import { ArcControl } from "@/components/author/ArcControl";
import { BeatQueue } from "@/components/author/BeatQueue";
import { WhisperBudgets } from "@/components/author/WhisperBudgets";
import { BondLedger } from "@/components/author/BondLedger";
import { Dossiers } from "@/components/author/Dossiers";
import { WatcherSleepToggle } from "@/components/author/WatcherSleepToggle";
import { AcceptingTrigger } from "@/components/author/AcceptingTrigger";
import { EndingSelector } from "@/components/author/EndingSelector";
import { DirectorRunPanel } from "@/components/author/DirectorRunPanel";
import { DirectorStateReport } from "@/components/author/DirectorStateReport";
import { DirectorProgressReport } from "@/components/author/DirectorProgressReport";
import { SetupFlow } from "@/components/author/SetupFlow";
import { UnlitProgress } from "@/components/author/UnlitProgress";
import { KeeperTheoryProgress } from "@/components/author/KeeperTheoryProgress";
import { SideProofProgress } from "@/components/author/SideProofProgress";
import { DimensionLaneProgress } from "@/components/author/DimensionLaneProgress";
import { PriorAcceptingProgress } from "@/components/author/PriorAcceptingProgress";
import { ManualMediaProgress } from "@/components/author/ManualMediaProgress";
import type { FateInput, EndingFate } from "@/app/author/fate-preview";
import type { DossierEntry } from "@/components/author/Dossiers";
import type { WhisperBudgetRow } from "@/components/author/WhisperBudgets";
import type { BondLedgerEntry } from "@/components/author/BondLedger";
import type {
  ArcState,
  AnswerAttempt,
  Beat,
  BondLedger as BondLedgerType,
  CustomCompliance,
  Dossier,
  Hint,
  Player,
  Puzzle,
  Setting,
  Solve,
  WhisperBudget,
} from "@/lib/database.types";

// This control surface is read fresh on every request — it reflects live game
// state the plugin writes, and server actions revalidatePath("/author").
export const dynamic = "force-dynamic";

const BEAT_ORDER: Record<Beat["status"], number> = {
  pending: 0,
  approved: 1,
  failed: 2,
  fired: 3,
  skipped: 4,
};

export default async function AuthorPage() {
  const operator = await requireAuthor();
  const supabase = createAdminClient();

  // Read the full control surface in parallel (service-role bypasses RLS).
  const [
    arcRes,
    beatsRes,
    budgetsRes,
    bondsRes,
    playersRes,
    dossiersRes,
    complianceRes,
    settingsRes,
    puzzlesRes,
    solvesRes,
    attemptsRes,
    hintsRes,
  ] = await Promise.all([
    supabase.from("arc_state").select("*").eq("id", 1).maybeSingle(),
    supabase
      .from("beat_queue")
      .select("*")
      .order("created_at", { ascending: false })
      .limit(100),
    supabase
      .from("whisper_budgets")
      .select("*")
      .order("act", { ascending: true })
      .order("id", { ascending: true }),
    supabase
      .from("bond_ledger")
      .select("*")
      .order("bond_points", { ascending: false }),
    supabase.from("players").select("*").order("name", { ascending: true }),
    // Read the reconciling views (0005_reconcile_tracker_views.sql), not the raw
    // tables: the plugin writes flat, mc_uuid-keyed rows with a NULL player_id and
    // renamed columns, so a direct table read would miss the player_id join and
    // render blank. The views synthesize player_id via players.mc_uuid and alias
    // the flat columns back into the Dossier / CustomCompliance shapes these casts
    // (and database.types.ts) already declare.
    supabase.from("v_dossiers").select("*"),
    supabase.from("v_custom_compliance").select("*"),
    supabase.from("settings").select("*"),
    supabase
      .from("puzzles")
      .select("puzzle_key,title,accepted_answers,outcome_type,outcome_payload,movement,active,max_attempts,created_at,requires_flags")
      .eq("active", true)
      .order("movement", { ascending: true })
      .order("created_at", { ascending: true })
      .limit(220),
    supabase
      .from("solves")
      .select("id,puzzle_key,player_id,mc_uuid,discord_id,attempt_count,solved_at")
      .order("solved_at", { ascending: false })
      .limit(160),
    supabase
      .from("answer_attempts")
      .select("id,puzzle_key,player_id,mc_uuid,discord_id,surface,raw,normalized,matched,at,ip_hash")
      .order("at", { ascending: false })
      .limit(180),
    supabase
      .from("hints")
      .select("id,puzzle_key,tier,body")
      .order("puzzle_key", { ascending: true })
      .order("tier", { ascending: true })
      .limit(500),
  ]);

  const arc = (arcRes.data ?? null) as ArcState | null;
  const beats = ((beatsRes.data ?? []) as Beat[])
    .slice()
    .sort(
      (a, b) =>
        BEAT_ORDER[a.status] - BEAT_ORDER[b.status] ||
        new Date(b.created_at).getTime() - new Date(a.created_at).getTime(),
    );
  const budgets = (budgetsRes.data ?? []) as WhisperBudget[];
  const bonds = (bondsRes.data ?? []) as BondLedgerType[];
  const players = (playersRes.data ?? []) as Player[];
  const dossiers = (dossiersRes.data ?? []) as Dossier[];
  const compliance = (complianceRes.data ?? []) as CustomCompliance[];
  const settings = (settingsRes.data ?? []) as Setting[];
  const puzzles = (puzzlesRes.data ?? []) as Puzzle[];
  const solves = (solvesRes.data ?? []) as Solve[];
  const attempts = (attemptsRes.data ?? []) as AnswerAttempt[];
  const hints = (hintsRes.data ?? []) as Hint[];

  // Index players for cheap joins (the plugin owns the keys; we just label).
  const playerById = new Map(players.map((p) => [p.id, p]));
  const dossierByPlayer = new Map(dossiers.map((d) => [d.player_id, d]));
  const complianceByPlayer = new Map<string, CustomCompliance[]>();
  for (const c of compliance) {
    if (!c.player_id) continue;
    const list = complianceByPlayer.get(c.player_id) ?? [];
    list.push(c);
    complianceByPlayer.set(c.player_id, list);
  }

  const budgetRows: WhisperBudgetRow[] = budgets.map((b) => ({
    ...b,
    player: b.player_id ? playerById.get(b.player_id) ?? null : null,
  }));

  const bondRows: BondLedgerEntry[] = bonds.map((b) => ({
    ...b,
    player: playerById.get(b.player_id) ?? null,
  }));

  const dossierEntries: DossierEntry[] = players.map((player) => ({
    player,
    dossier: dossierByPlayer.get(player.id) ?? null,
    compliance: complianceByPlayer.get(player.id) ?? [],
  }));

  const watcherAsleep =
    settings.find((s) => s.key === "watcher_sleep")?.value === true;
  const pendingBeats = beats.filter((beat) => beat.status === "pending").length;
  const approvedBeats = beats.filter((beat) => beat.status === "approved").length;
  const failedBeats = beats.filter((beat) => beat.status === "failed").length;

  // ---------------------------------------------------------------------------
  // Ending-selector inputs (A2 `divergent-fates`, INV-11). A spoiler-rich, live,
  // ACTIVE-ONLY estimate of where the arc would resolve, computed by the SAME pure
  // policy the engine's fate sentinel runs (fate-preview mirrors decideFate).
  //
  // The authoritative set-once write happens in the engine's resolve.ts at the
  // rite solve; this is the director's preview. We read it defensively off the
  // single arc_state.flags bag + last-seen activity, so it compiles before the
  // migration-0006 typed columns land and degrades to a clean DIVIDED baseline.
  //
  // INV-11 / "never elect a chosen one": every count below is a GROUP TALLY. We
  // never carry a player identity into the fate — the bond ledger is excluded by
  // construction (it is not a field on FateInput), and DIVIDED is a group state.
  // "Never punish an absent member": the spread is over ACTIVE players only.
  // ---------------------------------------------------------------------------
  const flags = (arc?.flags ?? {}) as Record<string, unknown>;
  const flag = (k: string): boolean => flags[k] === true;

  // ACTIVE = seen within the roster window (mirrors the engine's readActiveRoster
  // definition; the engine owns the exact ms — this preview uses a generous 7-day
  // window so the director sees the live active set, never an absent friend).
  const ACTIVE_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;
  const now = Date.now();
  const activePlayerIds = new Set(
    players
      .filter((p) => {
        const seen = new Date(p.last_seen).getTime();
        return Number.isFinite(seen) && now - seen <= ACTIVE_WINDOW_MS;
      })
      .map((p) => p.id),
  );
  const activeRosterSize = activePlayerIds.size;

  // Per active player, the dominant pole of their tracked customs (a group tally,
  // never surfaced per-player). We mirror the engine's rung semantics exactly
  // (discord/src/showrunner/customs.ts): a custom has reached the cold turn at
  // LEFT_AT = 5 standing violations; "violated-dominant" = any standing violation.
  // Keep this constant in lockstep with customs.ts::LEFT_AT (it is the same rung
  // fate.ts counts for CAST_OUT). A flat player (no measured customs) is neither.
  const LEFT_AT = 5;
  let honoredActive = 0;
  let violatedActive = 0;
  let leftAtActive = 0;
  for (const id of activePlayerIds) {
    const rows = complianceByPlayer.get(id) ?? [];
    if (rows.length === 0) continue; // no measured customs → contributes to neither pole
    const violated = rows.some((c) => c.violation_count > 0);
    const leftAt = rows.some((c) => c.violation_count >= LEFT_AT);
    if (leftAt) leftAtActive += 1;
    if (violated) violatedActive += 1;
    else honoredActive += 1;
  }

  const fateInput: FateInput = {
    honoredActive,
    violatedActive,
    leftAtActive,
    seventhFound: flag("seventh_named"),
    issCaught: flag("iss_caught"),
    // quorum is met when the rite's active cast is present; the plugin sets this
    // flag (threshold_open is the closest standing proxy until the rite arms).
    quorumMet: flag("quorum_met") || flag("threshold_open"),
    // REFUSERS requires a POSITIVE plugin-detected defiance signal, never absence.
    refusalSignal: flag("refusal_signal"),
  };

  const resolvedFateRaw = flags["ending_fate"];
  const resolvedFate: EndingFate | null =
    resolvedFateRaw === "kept" ||
    resolvedFateRaw === "cast_out" ||
    resolvedFateRaw === "divided" ||
    resolvedFateRaw === "refusers"
      ? resolvedFateRaw
      : null;

  const codicil = flag("ending_codicil");
  const seventhChoiceRaw = flags["seventh_choice"];
  const seventhChoice: "restore" | "erase" | null =
    seventhChoiceRaw === "restore" || seventhChoiceRaw === "erase"
      ? seventhChoiceRaw
      : null;
  const hasHoldZip = existsSync(
    join(process.cwd(), "public", "the-hold", "the-hold.zip"),
  );
  const serverAddressConfigured = Boolean(
    process.env.NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS?.trim(),
  );

  return (
    <div className="director-console space-y-8">
      <header className="director-header">
        <div>
          <p className="eyebrow">Live production / restricted</p>
          <h1>Observance Director</h1>
          <p>World state, story pressure, player progress, and intervention controls in one live console.</p>
        </div>
        <form action={signOutAuthor}>
          <span>{operator.email}</span>
          <button type="submit">Sign out</button>
        </form>
      </header>

      <nav className="director-nav" aria-label="Director console sections">
        <a href="#overview">Overview</a><a href="#story">Story state</a><a href="#evidence">Evidence</a>
        <a href="#operations">Operations</a><a href="#players">Players</a><a href="#ending">Ending</a>
      </nav>

      <div className="director-group" id="overview">
        <div className="director-group-title"><span>01</span><div><h2>Launch and live overview</h2><p>Readiness, immediate risks, and recommended next intervention.</p></div></div>
        <DirectorRunPanel watcherAsleep={watcherAsleep} pendingBeats={pendingBeats} approvedBeats={approvedBeats} failedBeats={failedBeats} />
        <DirectorStateReport currentAct={arc?.current_act ?? 1} flags={flags} players={players} compliance={compliance} beats={beats} watcherAsleep={watcherAsleep} activeRosterSize={activeRosterSize} hasHoldZip={hasHoldZip} serverAddressConfigured={serverAddressConfigured} />
        <SetupFlow />
      </div>

      <div className="director-group" id="story">
        <div className="director-group-title"><span>02</span><div><h2>Story state</h2><p>Arc progression, live puzzle pressure, and current narrative gates.</p></div></div>
        <DirectorProgressReport flags={flags} players={players} puzzles={puzzles} solves={solves} attempts={attempts} hints={hints} />
        <ArcControl arc={arc} />
      </div>

      <div className="director-group" id="evidence">
        <div className="director-group-title"><span>03</span><div><h2>Evidence lanes</h2><p>Completion across the Hold, the Unlit, side proofs, dimensions, and recovered media.</p></div></div>
        <UnlitProgress flags={flags} />
        <KeeperTheoryProgress flags={flags} />
        <SideProofProgress flags={flags} />
        <DimensionLaneProgress flags={flags} />
        <PriorAcceptingProgress flags={flags} />
        <ManualMediaProgress flags={flags} hasHoldZip={hasHoldZip} />
      </div>

      <div className="director-group" id="operations">
        <div className="director-group-title"><span>04</span><div><h2>Live operations</h2><p>Automation mode and the event queue. Review context before releasing a beat.</p></div></div>
        <WatcherSleepToggle asleep={watcherAsleep} />
        <BeatQueue beats={beats} />
      </div>

      <div className="director-group" id="players">
        <div className="director-group-title"><span>05</span><div><h2>Players and pressure</h2><p>Hint economy, Watcher reliance, and private dossier state.</p></div></div>
        <WhisperBudgets rows={budgetRows} />
        <BondLedger rows={bondRows} />
        <Dossiers entries={dossierEntries} />
      </div>

      <div className="director-group danger-group" id="ending">
        <div className="director-group-title"><span>06</span><div><h2>Ending controls</h2><p>Set-once fate preview and guarded climax actions. Treat this section as live ordnance.</p></div></div>
        <EndingSelector input={fateInput} activeRosterSize={activeRosterSize} codicil={codicil} resolved={resolvedFate} seventhChoice={seventhChoice} />
        <AcceptingTrigger />
      </div>
    </div>
  );
}
