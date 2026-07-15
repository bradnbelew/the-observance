import { readFileSync } from 'node:fs';
import { project } from './record-projection';
import { projectPublicDocket } from './record-projection';

const sealed = project({});
if (sealed.nodesCompleted !== 0 || sealed.totalNodes !== 82 || sealed.closed) throw new Error('sealed V5 projection drift');
const live = project({ currentCaseKey: 'C04', currentCaseTitle: 'Cistern Winter', casesCompleted: 3, nodesCompleted: 30, totalNodes: 82 });
if (live.currentCaseKey !== 'C04' || live.casesCompleted !== 3 || !live.footer.includes('no case is optional')) throw new Error('live V5 projection drift');
if (live.currentCaseTitle !== 'Docket C04') throw new Error('incomplete current case leaked its canonical title');
const closed = project({ closed: true, nodesCompleted: 82, totalNodes: 82, endingBranch: 'witness', nameTreatment: 'publish', wrenOutcome: 'understand' });
if (!closed.closed || closed.endingBranch !== 'witness' || closed.nameTreatment !== 'publish') throw new Error('closed V5 projection drift');

const answerBearingCases = [
  { caseKey: 'C04', title: 'Cistern Winter', summary: 'Clear Nessa with counterfeit filters.', forbidden: ['nessa', 'counterfeit'] },
  { caseKey: 'C07', title: 'ASH-13 Company', summary: "Recover Ash's locker key.", forbidden: ['ash-13', 'locker'] },
  { caseKey: 'C09', title: 'Averyn and the Unwriting', summary: 'Restore Averyn and find the constrained human voice.', forbidden: ['averyn', 'human voice'] },
  { caseKey: 'C10', title: 'Release Protocol', summary: 'Return six affidavits without binding a seventh.', forbidden: ['six affidavits', 'seventh'] },
];
for (const row of answerBearingCases) {
  const open = projectPublicDocket({ ...row, complete: false });
  const rendered = `${open.title} ${open.summary}`.toLowerCase();
  for (const token of row.forbidden) {
    if (rendered.includes(token)) throw new Error(`${row.caseKey} incomplete public docket leaked ${token}`);
  }
  const earned = projectPublicDocket({ ...row, complete: true });
  if (earned.title !== row.title || earned.summary !== row.summary) throw new Error(`${row.caseKey} completed docket stayed redacted`);
}

const migration = readFileSync(new URL('../../supabase/migrations/0010_v5_public_record.sql', import.meta.url), 'utf8')
  .replace(/\r\n?/g, '\n');
for (const required of [
  "then i.title\n      else 'Docket ' || i.case_key",
  'end as public_title',
  'c.public_title as current_case_title',
  "then i.summary\n    else 'Docket open. Recover and file evidence at its named surface.'",
]) {
  if (!migration.includes(required)) throw new Error(`public Record migration lost incomplete-case redaction: ${required}`);
}

console.log('record projection selftest OK: V5 muster, completed reveals, and incomplete answer-bearing dockets redacted');
