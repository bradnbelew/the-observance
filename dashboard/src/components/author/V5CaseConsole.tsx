export interface V5InvestigationRow {
  case_key: string;
  ordinal: number;
  title: string;
  summary: string;
  phase_key: string;
  unlock_flag: string | null;
  completion_flag: string;
  expected_nodes: number;
  active: boolean;
}

export interface V5NodeRow {
  node_key: string;
  case_key: string;
  ordinal: number;
  title: string;
  room_id: string;
  modality: string;
  input_surface: string;
  prerequisite_flags: string[];
  completion_flag: string;
  reward: string;
  recovery: string;
  oracle_puzzle_key: string | null;
  active: boolean;
}

export interface V5ReceiptRow { node_key: string; received_at: string }

export interface V5MediaRow {
  media_key: string;
  case_key: string;
  node_key: string;
  title: string;
  filename: string;
  delivery_url: string;
  delivery_state: string;
  prerequisite_flags: string[];
  last_verified_at: string | null;
  active: boolean;
}

function flagOn(flags: Record<string, unknown>, key: string): boolean {
  return flags[key] === true;
}

export function V5CaseConsole({
  investigations,
  nodes,
  receipts,
  media,
  flags,
}: {
  investigations: V5InvestigationRow[];
  nodes: V5NodeRow[];
  receipts: V5ReceiptRow[];
  media: V5MediaRow[];
  flags: Record<string, unknown>;
}) {
  const receiptNodes = new Set(receipts.map((receipt) => receipt.node_key));
  const completed = (node: V5NodeRow) => flagOn(flags, node.completion_flag) || receiptNodes.has(node.node_key);

  return (
    <div className="space-y-5">
      <div className="grid gap-3 sm:grid-cols-3">
        <div className="director-card"><p className="eyebrow">Mandatory cases</p><strong>{investigations.length} / 10</strong></div>
        <div className="director-card"><p className="eyebrow">Required nodes</p><strong>{nodes.filter(completed).length} / 82</strong></div>
        <div className="director-card"><p className="eyebrow">Durable receipts</p><strong>{receiptNodes.size}</strong></div>
      </div>

      {investigations.map((investigation) => {
        const caseNodes = nodes.filter((node) => node.case_key === investigation.case_key);
        const done = caseNodes.filter(completed).length;
        const unlocked = investigation.unlock_flag === null || flagOn(flags, investigation.unlock_flag);
        const complete = flagOn(flags, investigation.completion_flag);
        return (
          <details key={investigation.case_key} className="director-card" open={unlocked && !complete}>
            <summary className="cursor-pointer list-none">
              <div className="flex flex-wrap items-baseline justify-between gap-3">
                <div><span className="eyebrow">{investigation.case_key} · {investigation.phase_key}</span><h3>{investigation.title}</h3></div>
                <strong>{done}/{investigation.expected_nodes} · {complete ? 'complete' : unlocked ? 'open' : 'sealed'}</strong>
              </div>
              <p>{investigation.summary}</p>
            </summary>
            <ol className="mt-4 space-y-2">
              {caseNodes.map((node) => (
                <li key={node.node_key} className="rounded border border-neutral-800 p-3 text-sm">
                  <div className="flex flex-wrap justify-between gap-2"><strong>{node.node_key} · {node.title}</strong><span>{completed(node) ? 'complete' : 'pending'}</span></div>
                  <p>{node.room_id} · {node.modality} · input: {node.input_surface}</p>
                  <p>Reward: {node.reward}</p><p>Recovery: {node.recovery}</p>
                  {node.oracle_puzzle_key && <code>{node.oracle_puzzle_key}</code>}
                </li>
              ))}
            </ol>
          </details>
        );
      })}

      <section className="director-card">
        <h3>Required media delivery</h3>
        <p>Availability is derived from prerequisites. These are health records, not story switches.</p>
        <ul className="mt-3 space-y-2">
          {media.map((asset) => {
            const prereqsMet = asset.prerequisite_flags.every((key) => flagOn(flags, key));
            return <li key={asset.media_key}><strong>{asset.media_key}</strong> · {asset.title} · {asset.filename} · {asset.delivery_state} · {prereqsMet ? 'deliverable' : 'prerequisites pending'}{asset.last_verified_at ? ` · checked ${asset.last_verified_at}` : ' · not externally verified'}</li>;
          })}
        </ul>
      </section>
    </div>
  );
}
