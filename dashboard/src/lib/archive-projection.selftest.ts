import { projectArchive, type ArchiveEvidence } from './archive-projection';

const row = (node_key: string, case_key: string, case_ordinal: number, node_ordinal: number): ArchiveEvidence => ({ node_key, case_key, case_ordinal, case_title: case_key, node_ordinal, title: node_key, modality: 'deduction', reward: 'receipt', recovered_at: null });
const projection = projectArchive([row('CW02', 'C04', 4, 2), row('LS01', 'C01', 1, 1), row('CW01', 'C04', 4, 1)]);
if (projection.total !== 3 || projection.cases.map((item) => item.key).join() !== 'C01,C04') throw new Error('V5 archive case ordering drift');
if (projection.cases[1]?.evidence.map((item) => item.node_key).join() !== 'CW01,CW02') throw new Error('V5 archive node ordering drift');
if (!projectArchive([]).empty) throw new Error('V5 archive empty state drift');

console.log('archive projection selftest OK: earned V5 evidence grouped by mandatory case');
