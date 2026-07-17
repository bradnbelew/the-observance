import type { Metadata } from 'next';
import { getPublicProjectionClient } from '@/lib/supabase/public-projection';
import { RuneGlyphs } from '@/lib/RuneGlyphs';
import { projectArchive, type ArchiveEvidence } from '@/lib/archive-projection';

export const metadata: Metadata = { robots: { index: false, follow: false }, title: 'recordsrv / recovered evidence' };
export const revalidate = 120;

interface DeliveredMedia { media_key: string; case_key: string; node_key: string; media_kind: string; title: string; delivery_url: string; filename: string; delivery_state: string }

async function readRecovered(): Promise<{ rows: ArchiveEvidence[]; media: DeliveredMedia[]; unavailable: boolean }> {
  try {
    const supabase = getPublicProjectionClient();
    if (!supabase) return { rows: [], media: [], unavailable: true };
    const client = supabase as unknown as { from: (relation: string) => { select: (columns: string) => Promise<{ data: Record<string, unknown>[] | null; error: unknown }> } };
    const [archiveResult, mediaResult] = await Promise.all([
      client.from('v_archive').select('node_key,case_key,case_ordinal,case_title,node_ordinal,title,modality,reward,recovered_at'),
      client.from('v_required_media_delivery').select('media_key,case_key,node_key,media_kind,title,delivery_url,filename,delivery_state'),
    ]);
    if (archiveResult.error || !Array.isArray(archiveResult.data)) return { rows: [], media: [], unavailable: true };
    const rows = archiveResult.data.filter((row) => typeof row.node_key === 'string' && typeof row.case_key === 'string').map((row) => ({
      node_key: String(row.node_key), case_key: String(row.case_key), case_ordinal: Number(row.case_ordinal) || 0,
      case_title: typeof row.case_title === 'string' ? row.case_title : '', node_ordinal: Number(row.node_ordinal) || 0,
      title: typeof row.title === 'string' ? row.title : '', modality: typeof row.modality === 'string' ? row.modality : '',
      reward: typeof row.reward === 'string' ? row.reward : '', recovered_at: typeof row.recovered_at === 'string' ? row.recovered_at : null,
    }));
    const media = !mediaResult.error && Array.isArray(mediaResult.data) ? mediaResult.data.filter((row) => typeof row.media_key === 'string' && typeof row.delivery_url === 'string').map((row) => ({
      media_key: String(row.media_key), case_key: String(row.case_key), node_key: String(row.node_key), media_kind: String(row.media_kind), title: String(row.title), delivery_url: String(row.delivery_url), filename: String(row.filename), delivery_state: String(row.delivery_state),
    })) : [];
    return { rows, media, unavailable: false };
  } catch { return { rows: [], media: [], unavailable: true }; }
}

export default async function ArchivePage() {
  const { rows, media, unavailable } = await readRecovered();
  const archive = projectArchive(rows);
  return (
    <main className="record-site archive-site"><div className="record-page archive-page">
      <header className="record-system-header"><div><span>recordsrv/0.9</span><span>projection: earned evidence</span><span>mode: read-only</span></div><RuneGlyphs text="RECOVERED EVIDENCE" className="mx-auto my-3 text-amber-700/70" height={22} /><h1>RECOVERED EVIDENCE</h1><p>{unavailable ? 'index unavailable' : `${archive.total} required findings entered`}</p></header>
      {archive.empty ? <p className="record-empty">{unavailable ? 'the archive could not be read.' : 'no evidence has been entered yet.'}</p> : archive.cases.map((caseFile) => (
        <section key={caseFile.key} className="mb-12"><header className="mb-3 flex items-baseline gap-3"><h2 className="font-mono text-xs uppercase tracking-[0.3em] text-neutral-500">{caseFile.key} · {caseFile.title}</h2><span className="text-xs text-neutral-700">{caseFile.evidence.length}</span></header><ol className="border-b border-neutral-900">{caseFile.evidence.map((entry) => <li key={entry.node_key} className="border-t border-neutral-900 py-4"><h3 className="font-mono text-sm text-neutral-300">{entry.node_key} · {entry.title}</h3><p className="mt-2 font-mono text-xs text-neutral-600">{entry.modality}</p><p className="mt-2 font-mono text-sm text-neutral-400">{entry.reward}</p></li>)}</ol></section>
      ))}
      {media.length > 0 && <section className="mb-12"><h2 className="mb-3 font-mono text-xs uppercase tracking-[0.3em] text-neutral-500">delivered media</h2><ul className="border-b border-neutral-900">{media.map((asset) => <li key={asset.media_key} className="border-t border-neutral-900 py-4 font-mono text-sm"><a href={asset.delivery_url} rel="noreferrer" className="text-neutral-300 underline decoration-neutral-700 underline-offset-4">{asset.title}</a><p className="mt-1 text-xs text-neutral-700">{asset.case_key} · {asset.node_key} · {asset.filename} · {asset.delivery_state}</p></li>)}</ul></section>}
      <footer className="mt-4 text-center font-mono text-xs lowercase tracking-wide text-neutral-700">only earned evidence is indexed. unresolved cases are not listed here.</footer>
    </div></main>
  );
}
