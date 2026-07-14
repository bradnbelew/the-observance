import type { Metadata } from 'next';
import Link from 'next/link';
import { createClient } from '@/lib/supabase/server';
import { RuneGlyphs } from '@/lib/RuneGlyphs';
import { project, REDACTED_GLYPH, type RecordSignal } from '@/lib/record-projection';
import { readValidatedV5HoldArchive, V5_HOLD_ARCHIVE_DOWNLOAD_PATH } from '@/lib/v5-hold-archive';
import { readV5CompletionFlag } from '@/lib/v5-web-progress';

const LURE_SLUG = 'the-record-keeps';

export const dynamic = 'force-dynamic';

type RecordSlug = 'base' | 'lure' | 'unknown';
function resolveSlug(raw: string | undefined): RecordSlug {
  const slug = (raw ?? '').trim().toLowerCase();
  if (slug === LURE_SLUG) return 'lure';
  if (slug === 'the-record') return 'base';
  return 'unknown';
}

export async function generateMetadata({ params }: { params: Promise<{ slug?: string }> }): Promise<Metadata> {
  const which = resolveSlug((await params).slug);
  return { robots: { index: false, follow: false }, title: which === 'lure' ? 'Index of /~mkept/record/' : which === 'base' ? 'recordsrv / public projection' : 'record key not found' };
}

async function readSignal(): Promise<{ signal: RecordSignal; unavailable: boolean }> {
  try {
    const supabase = await createClient();
    const { data, error } = await (supabase as unknown as {
      from: (relation: string) => { select: (columns: string) => { maybeSingle: () => Promise<{ data: Record<string, unknown> | null; error: unknown }> } };
    }).from('v_record').select('movement,phase_key,current_case_key,current_case_title,cases_completed,nodes_completed,total_nodes,closed,ending_branch,name_treatment,wren_outcome').maybeSingle();
    if (error || !data) return { signal: {}, unavailable: true };
    return { unavailable: false, signal: {
      movement: typeof data.movement === 'number' ? data.movement : null,
      phaseKey: typeof data.phase_key === 'string' ? data.phase_key : null,
      currentCaseKey: typeof data.current_case_key === 'string' ? data.current_case_key : null,
      currentCaseTitle: typeof data.current_case_title === 'string' ? data.current_case_title : null,
      casesCompleted: typeof data.cases_completed === 'number' ? data.cases_completed : null,
      nodesCompleted: typeof data.nodes_completed === 'number' ? data.nodes_completed : null,
      totalNodes: typeof data.total_nodes === 'number' ? data.total_nodes : null,
      closed: data.closed === true,
      endingBranch: typeof data.ending_branch === 'string' ? data.ending_branch : null,
      nameTreatment: typeof data.name_treatment === 'string' ? data.name_treatment : null,
      wrenOutcome: typeof data.wren_outcome === 'string' ? data.wren_outcome : null,
    } };
  } catch { return { signal: {}, unavailable: true }; }
}

function NotFoundShell() {
  return <main className="record-error-site"><div className="record-error-box"><p>recordsrv/0.9</p><h1>404: key not found</h1><pre>{`lookup failed\nnothing is filed under that name here.`}</pre></div></main>;
}

async function LurePage() {
  const available = await readValidatedV5HoldArchive() !== null;
  return (
    <main className="mirror-site"><div className="mirror-page">
      <header className="mirror-header"><p>files.copperlinehosting.com :: static user mirror</p><h1>/home/mkept/public_html/record/</h1><span>read-only snapshot · last modified 2011-02-08 23:51 CST</span></header>
      <nav className="mirror-nav"><Link href="/community/2011/02/08/world-backup">../ return to community post</Link></nav>
      <section className="mirror-note-block"><h2>recovery-roster.txt</h2><pre>{`COPY OWNER    mkept    administration / backups\nCAMERA        Ash      field footage / timestamps\nBUILD         Rook     routes / temporary works\nACCESS        Wren     keys / communication\n\nSERVICE       1842\nARCHIVE SET   2011-02-08\nSTATUS        split before upload`}</pre></section>
      <section className="mirror-files" aria-labelledby="mirror-files-heading"><h2 id="mirror-files-heading">files</h2><div className="mirror-file-row"><span>-rw-r--r--</span><span>mkept</span><span>{available ? 'retained' : '—'}</span>{available ? <a href={V5_HOLD_ARCHIVE_DOWNLOAD_PATH} download rel="noopener">the-hold.zip</a> : <span className="mirror-missing">the-hold.zip</span>}</div>{!available && <p className="mirror-error">archive withdrawn after checksum mismatch; no replacement has been published.</p>}</section>
      <section className="mirror-note-block"><h2>uploader-note.txt</h2><p>This is the stripped recovery copy. Player data and chat were removed. The route fragments were left in four work areas because I could not tell which copy was current.</p><p>If the archive checksum does not match the index, do not merge it with another world.</p><p>— m.kept</p></section>
      <footer className="mirror-footer">lighttpd/1.4.28 · preserved directory index · write methods disabled</footer>
    </div></main>
  );
}

export default async function RecordPage({ params }: { params: Promise<{ slug?: string }> }) {
  const which = resolveSlug((await params).slug);
  if (which === 'unknown') return <NotFoundShell />;
  if (which === 'lure') {
    const directoryTrail = await readV5CompletionFlag('v5_ls03_directory_trail');
    return directoryTrail.complete ? await LurePage() : <NotFoundShell />;
  }

  const { signal, unavailable } = await readSignal();
  const record = project(signal);
  return (
    <main className="record-site"><div className="record-page narrow">
      <header className="record-system-header"><div><span>recordsrv/0.9</span><span>projection: public</span><span>mode: read-only</span></div><RuneGlyphs text="THE RECORD" className="mx-auto my-3 text-amber-700/70" height={22} /><h1>THE RECORD</h1><p>{unavailable ? 'the record could not be read.' : record.closed ? 'closure receipt' : record.phaseKey}</p></header>
      <div className="record-muster"><span className="text-3xl tabular-nums text-neutral-300">{unavailable ? '—' : record.nodesCompleted}</span><span className="text-sm text-neutral-700">/</span><span className="text-sm tabular-nums text-neutral-600">{record.totalNodes}</span><span className="ml-2 text-xs uppercase tracking-wide text-neutral-700">required findings</span></div>
      <section className="border-y border-neutral-900 py-6 font-mono"><p className="text-xs uppercase tracking-[0.25em] text-neutral-700">current case</p><h2 className="mt-2 text-neutral-300">{unavailable ? REDACTED_GLYPH : `${record.currentCaseKey ?? '—'} · ${record.currentCaseTitle}`}</h2><p className="mt-2 text-sm text-neutral-600">{unavailable ? 'case state unavailable.' : `${record.casesCompleted} of 10 mandatory cases closed.`}</p></section>
      {record.closed && <section className="mt-6 border border-neutral-900 p-4 font-mono text-sm text-neutral-500"><p>closure: {record.endingBranch ?? 'recorded'}</p><p>name: {record.nameTreatment ?? 'returned'}</p><p>wren: {record.wrenOutcome ?? 'adjudicated'}</p></section>}
      <footer className="record-footer">{unavailable ? 'the entries remain unavailable.' : record.footer}{record.nodesCompleted > 0 && <><br /><Link href="/record/archive" className="mt-2 inline-block text-neutral-600 underline decoration-neutral-800 underline-offset-4">open recovered evidence index</Link></>}</footer>
    </div></main>
  );
}
