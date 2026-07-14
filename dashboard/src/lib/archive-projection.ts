export interface ArchiveEvidence {
  node_key: string;
  case_key: string;
  case_ordinal: number;
  case_title: string;
  node_ordinal: number;
  title: string;
  modality: string;
  reward: string;
  recovered_at: string | null;
}

export interface ArchiveCase {
  key: string;
  ordinal: number;
  title: string;
  evidence: ArchiveEvidence[];
}

export interface ArchiveProjection { cases: ArchiveCase[]; total: number; empty: boolean }

export function projectArchive(rows: ArchiveEvidence[]): ArchiveProjection {
  const valid = Array.isArray(rows) ? rows.filter((row) => row && row.node_key && row.case_key) : [];
  const grouped = new Map<string, ArchiveCase>();
  for (const row of valid) {
    const existing = grouped.get(row.case_key) ?? { key: row.case_key, ordinal: row.case_ordinal, title: row.case_title, evidence: [] };
    existing.evidence.push(row);
    grouped.set(row.case_key, existing);
  }
  const cases = [...grouped.values()].sort((a, b) => a.ordinal - b.ordinal || a.key.localeCompare(b.key));
  for (const item of cases) item.evidence.sort((a, b) => a.node_ordinal - b.node_ordinal || a.node_key.localeCompare(b.node_key));
  return { cases, total: valid.length, empty: valid.length === 0 };
}
