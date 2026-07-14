import type { Metadata } from 'next';
import { createClient } from '@/lib/supabase/server';
import { RuneGlyphs } from '@/lib/RuneGlyphs';
import { projectPublicDocket } from '@/lib/record-projection';

export const metadata: Metadata = { robots: { index: false, follow: false }, title: 'recordsrv / investigation terminal' };
export const dynamic = 'force-dynamic';

interface CaseProgress { case_key: string; ordinal: number; title: string; summary: string; total_nodes: number; completed_nodes: number; complete: boolean }
interface RecordStatus { phase_key: string; nodes_completed: number; total_nodes: number; closed: boolean }

async function readStatus(): Promise<{ status: RecordStatus | null; cases: CaseProgress[] }> {
  try {
    const supabase = await createClient();
    const client = supabase as unknown as { from: (relation: string) => { select: (columns: string) => Promise<{ data: Record<string, unknown>[] | null; error: unknown }> & { maybeSingle?: never } } };
    const recordPromise = (supabase as unknown as { from: (relation: string) => { select: (columns: string) => { maybeSingle: () => Promise<{ data: Record<string, unknown> | null; error: unknown }> } } }).from('v_record').select('phase_key,nodes_completed,total_nodes,closed').maybeSingle();
    const casesPromise = client.from('v_case_progress').select('case_key,ordinal,title,summary,total_nodes,completed_nodes,complete');
    const [recordResult, casesResult] = await Promise.all([recordPromise, casesPromise]);
    if (recordResult.error || !recordResult.data || casesResult.error || !Array.isArray(casesResult.data)) return { status: null, cases: [] };
    const row = recordResult.data;
    const status: RecordStatus = { phase_key: typeof row.phase_key === 'string' ? row.phase_key : 'unavailable', nodes_completed: Number(row.nodes_completed) || 0, total_nodes: Number(row.total_nodes) || 82, closed: row.closed === true };
    const cases = casesResult.data.map((item) => {
      const docket = projectPublicDocket({
        caseKey: typeof item.case_key === 'string' ? item.case_key : null,
        title: typeof item.title === 'string' ? item.title : null,
        summary: typeof item.summary === 'string' ? item.summary : null,
        complete: item.complete === true,
      });
      return {
        case_key: docket.caseKey ?? 'UNAVAILABLE',
        ordinal: Number(item.ordinal) || 0,
        title: docket.title,
        summary: docket.summary,
        total_nodes: Number(item.total_nodes) || 0,
        completed_nodes: Number(item.completed_nodes) || 0,
        complete: docket.complete,
      };
    }).sort((a, b) => a.ordinal - b.ordinal);
    return { status, cases };
  } catch { return { status: null, cases: [] }; }
}

export default async function RecordTerminalPage() {
  const { status, cases } = await readStatus();
  return (
    <main className="record-site terminal-site"><div className="record-page terminal-page">
      <header className="record-system-header terminal-system-header"><div><span>recordsrv/0.9</span><span>endpoint: /terminal</span><span>{status ? 'integrity: live' : 'integrity: unavailable'}</span></div><RuneGlyphs text="INVESTIGATION DOCKET" className="mx-auto my-3 text-amber-700/70" height={22} /><h1>THE RECORD / INVESTIGATION TERMINAL</h1><p>required case docket · exact submissions · no closeness response</p></header>
      <div className="mb-8 flex flex-wrap items-baseline gap-8 font-mono"><div><span className="text-3xl text-neutral-300">{status?.nodes_completed ?? '—'}</span><span className="ml-2 text-[11px] uppercase text-neutral-700">of {status?.total_nodes ?? 82} findings</span></div><div><span className="text-xl text-neutral-400">{cases.filter((item) => item.complete).length}</span><span className="ml-2 text-[11px] uppercase text-neutral-700">of 10 cases</span></div><div><span className="text-sm text-neutral-500">{status?.phase_key ?? 'unavailable'}</span></div></div>
      <section className="mb-10"><div className="mb-3 border-b border-neutral-900 pb-1 font-mono text-[10px] uppercase tracking-[0.3em] text-neutral-600">unlocked case docket</div>{cases.length === 0 ? <p className="font-mono text-sm text-neutral-700">no case index is readable.</p> : <ol>{cases.map((item) => <li key={item.case_key} className="border-t border-neutral-900 py-3 font-mono"><div className="flex justify-between gap-4"><strong className="text-neutral-300">{item.case_key} · {item.title}</strong><span className="text-neutral-600">{item.completed_nodes}/{item.total_nodes}</span></div><p className="mt-1 text-xs text-neutral-700">{item.summary}</p></li>)}</ol>}</section>
      <section className="mb-10"><div className="mb-3 border-b border-neutral-900 pb-1 font-mono text-[10px] uppercase tracking-[0.3em] text-neutral-600">filing discipline</div><p className="mb-2 font-mono text-[11px] lowercase leading-relaxed text-neutral-700">this docket is read-only. file each finding only at the surface named by its evidence: the dedicated copperline resolver, the linked discord record, or the in-world mechanism. a typed minecraft name is not an identity credential.</p></section>
      <footer className="mt-8 border-t border-neutral-900 pt-4 text-center font-mono text-[10px] lowercase tracking-wide text-neutral-700">{status?.closed ? 'the record is closed. submissions are disabled.' : 'every listed case is required. future dockets remain absent until opened.'}</footer>
    </div></main>
  );
}
