import { createAdminClient } from '@/lib/supabase/admin';
import { requireAuthor } from '@/lib/author-auth';
import { signOutAuthor } from '@/app/author/login/actions';
import { BeatQueue } from '@/components/author/BeatQueue';
import { WhisperBudgets, type WhisperBudgetRow } from '@/components/author/WhisperBudgets';
import { BondLedger, type BondLedgerEntry } from '@/components/author/BondLedger';
import { Dossiers, type DossierEntry } from '@/components/author/Dossiers';
import { WatcherSleepToggle } from '@/components/author/WatcherSleepToggle';
import { V5CaseConsole, type V5InvestigationRow, type V5MediaRow, type V5NodeRow, type V5ReceiptRow } from '@/components/author/V5CaseConsole';
import { V5FinalePanel } from '@/components/author/V5FinalePanel';
import type { ArcState, Beat, BondLedger as BondLedgerType, CustomCompliance, Dossier, Player, Setting, WhisperBudget } from '@/lib/database.types';

export const dynamic = 'force-dynamic';

const BEAT_ORDER: Record<Beat['status'], number> = { pending: 0, approved: 1, failed: 2, fired: 3, skipped: 4 };

export default async function AuthorPage() {
  const operator = await requireAuthor();
  const supabase = createAdminClient();

  const [arcRes, beatsRes, budgetsRes, bondsRes, playersRes, dossiersRes, complianceRes, settingsRes, casesRes, nodesRes, receiptsRes, mediaRes] = await Promise.all([
    supabase.from('arc_state').select('*').eq('id', 1).maybeSingle(),
    supabase.from('beat_queue').select('*').order('created_at', { ascending: false }).limit(150),
    supabase.from('whisper_budgets').select('*').order('act').order('id'),
    supabase.from('bond_ledger').select('*').order('bond_points', { ascending: false }),
    supabase.from('players').select('*').order('name'),
    supabase.from('v_dossiers').select('*'),
    supabase.from('v_custom_compliance').select('*'),
    supabase.from('settings').select('*'),
    supabase.from('investigations').select('*').eq('active', true).order('ordinal'),
    supabase.from('investigation_nodes').select('*').eq('active', true).order('case_key').order('ordinal'),
    supabase.from('evidence_receipts').select('node_key,received_at').order('received_at', { ascending: false }),
    supabase.from('required_media').select('*').eq('active', true).order('case_key').order('media_key'),
  ]);

  const arc = (arcRes.data ?? null) as ArcState | null;
  const flags = (arc?.flags ?? {}) as Record<string, unknown>;
  const beats = ((beatsRes.data ?? []) as Beat[]).slice().sort((a, b) => BEAT_ORDER[a.status] - BEAT_ORDER[b.status] || new Date(b.created_at).getTime() - new Date(a.created_at).getTime());
  const budgets = (budgetsRes.data ?? []) as WhisperBudget[];
  const bonds = (bondsRes.data ?? []) as BondLedgerType[];
  const players = (playersRes.data ?? []) as Player[];
  const dossiers = (dossiersRes.data ?? []) as Dossier[];
  const compliance = (complianceRes.data ?? []) as CustomCompliance[];
  const settings = (settingsRes.data ?? []) as Setting[];
  const investigations = (casesRes.data ?? []) as V5InvestigationRow[];
  const nodes = (nodesRes.data ?? []) as V5NodeRow[];
  const receipts = (receiptsRes.data ?? []) as V5ReceiptRow[];
  const media = (mediaRes.data ?? []) as V5MediaRow[];

  const playerById = new Map(players.map((player) => [player.id, player]));
  const dossierByPlayer = new Map(dossiers.map((dossier) => [dossier.player_id, dossier]));
  const complianceByPlayer = new Map<string, CustomCompliance[]>();
  for (const row of compliance) {
    if (!row.player_id) continue;
    complianceByPlayer.set(row.player_id, [...(complianceByPlayer.get(row.player_id) ?? []), row]);
  }
  const budgetRows: WhisperBudgetRow[] = budgets.map((budget) => ({ ...budget, player: budget.player_id ? playerById.get(budget.player_id) ?? null : null }));
  const bondRows: BondLedgerEntry[] = bonds.map((bond) => ({ ...bond, player: playerById.get(bond.player_id) ?? null }));
  const dossierEntries: DossierEntry[] = players.map((player) => ({ player, dossier: dossierByPlayer.get(player.id) ?? null, compliance: complianceByPlayer.get(player.id) ?? [] }));
  const watcherAsleep = settings.find((setting) => setting.key === 'watcher_sleep')?.value === true;
  const phaseKey = typeof (arc as unknown as { phase_key?: unknown } | null)?.phase_key === 'string'
    ? (arc as unknown as { phase_key: string }).phase_key : 'c01-lost-server';

  return (
    <div className="director-console space-y-8">
      <header className="director-header">
        <div><p className="eyebrow">V5 production operations · live state</p><h1>Observance Operations Console</h1><p>Ten mandatory cases, 82 required nodes, media health, player support, and durable finale state.</p></div>
        <form action={signOutAuthor}><span>{operator.email}</span><button type="submit">Sign out</button></form>
      </header>

      <nav className="director-nav" aria-label="Director console sections">
        <a href="#overview">Overview</a><a href="#cases">Cases</a><a href="#operations">Operations</a><a href="#players">Players</a><a href="#finale">Finale</a>
      </nav>

      <div className="director-group" id="overview">
        <div className="director-group-title"><span>01</span><div><h2>Live overview</h2><p>Compatibility act remains coarse; V5 progression is the phase key plus durable node flags.</p></div></div>
        <section className="director-card p-5"><div className="grid gap-3 sm:grid-cols-4"><div><p className="eyebrow">Current act</p><strong>{arc?.current_act ?? 1}</strong></div><div><p className="eyebrow">Phase key</p><strong>{phaseKey}</strong></div><div><p className="eyebrow">Watcher</p><strong>{watcherAsleep ? 'asleep' : 'awake'}</strong></div><div><p className="eyebrow">Queue</p><strong>{beats.filter((beat) => beat.status === 'pending').length} pending</strong></div></div></section>
      </div>

      <div className="director-group" id="cases">
        <div className="director-group-title"><span>02</span><div><h2>Mandatory investigation spine</h2><p>Every node is required. Receipt, recovery, media, and oracle wiring are visible here.</p></div></div>
        <section className="p-5"><V5CaseConsole investigations={investigations} nodes={nodes} receipts={receipts} media={media} flags={flags} /></section>
      </div>

      <div className="director-group" id="operations">
        <div className="director-group-title"><span>03</span><div><h2>Live operations</h2><p>The persistent worker runs the lease-safe showrunner. Review queued interventions before approval.</p></div></div>
        <WatcherSleepToggle asleep={watcherAsleep} />
        <BeatQueue beats={beats} />
      </div>

      <div className="director-group" id="players">
        <div className="director-group-title"><span>04</span><div><h2>Players and support</h2><p>Hint economy and private operational telemetry. No player is selected as a story protagonist.</p></div></div>
        <WhisperBudgets rows={budgetRows} /><BondLedger rows={bondRows} /><Dossiers entries={dossierEntries} />
      </div>

      <div className="director-group danger-group" id="finale">
        <div className="director-group-title"><span>05</span><div><h2>Release Protocol</h2><p>Branch state is written before theater; Minecraft owns save, goodbye, kick, shutdown, and Coda Mode.</p></div></div>
        <V5FinalePanel flags={flags} />
      </div>
    </div>
  );
}
