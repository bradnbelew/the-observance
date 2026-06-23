import { redirect } from "next/navigation";
import { isAdmin } from "@/lib/auth";
import { createClient } from "@/lib/supabase/server";
import { ArcControl } from "@/components/author/ArcControl";
import { BeatQueue } from "@/components/author/BeatQueue";
import { WhisperBudgets } from "@/components/author/WhisperBudgets";
import { BondLedger } from "@/components/author/BondLedger";
import { Dossiers } from "@/components/author/Dossiers";
import { WatcherSleepToggle } from "@/components/author/WatcherSleepToggle";
import { AcceptingTrigger } from "@/components/author/AcceptingTrigger";
import type { DossierEntry } from "@/components/author/Dossiers";
import type { WhisperBudgetRow } from "@/components/author/WhisperBudgets";
import type { BondLedgerEntry } from "@/components/author/BondLedger";
import type {
  ArcState,
  Beat,
  BondLedger as BondLedgerType,
  CustomCompliance,
  Dossier,
  Player,
  Setting,
  WhisperBudget,
} from "@/lib/database.types";

// This control surface is read fresh on every request — it reflects live game
// state the plugin writes, and server actions revalidatePath("/author").
export const dynamic = "force-dynamic";

const BEAT_ORDER: Record<Beat["status"], number> = {
  pending: 0,
  approved: 1,
  fired: 2,
  skipped: 3,
};

export default async function AuthorPage() {
  // Author mode is admin-only. Re-checked again inside every server action.
  if (!(await isAdmin())) {
    redirect("/auth/login");
  }

  const supabase = await createClient();

  // Read the full control surface in parallel. RLS grants `authenticated`
  // full read access; the ADMIN_EMAILS gate above decides who gets here.
  const [
    arcRes,
    beatsRes,
    budgetsRes,
    bondsRes,
    playersRes,
    dossiersRes,
    complianceRes,
    settingsRes,
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
    supabase.from("dossiers").select("*"),
    supabase.from("custom_compliance").select("*"),
    supabase.from("settings").select("*"),
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

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="font-mono text-2xl text-neutral-100">Author</h1>
        <p className="max-w-prose text-sm text-neutral-400">
          Full control surface. Everything here is spoiler-rich — the arc, the
          beat queue, named dossiers, the bond ledger. Writes go through
          admin-gated server actions.
        </p>
      </header>

      <ArcControl arc={arc} />

      <WatcherSleepToggle asleep={watcherAsleep} />

      <BeatQueue beats={beats} />

      <WhisperBudgets rows={budgetRows} />

      <BondLedger rows={bondRows} />

      <Dossiers entries={dossierEntries} />

      <AcceptingTrigger />
    </div>
  );
}
