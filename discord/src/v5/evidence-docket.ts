/**
 * Replayable Discord-native records earned by the investigation.
 *
 * These are evidence fragments and provenance notes, not conclusions or answer menus. They are
 * available to any linked participant once the prior case transition is durable; reading them is
 * never an eligibility or correctness prerequisite.
 */
export type EvidenceDocketRecord = Readonly<{
  id: string;
  phase: 'p5' | 'p6' | 'p7' | 'p8' | 'p9' | 'p10' | 'p11' | 'p12';
  title: string;
  provenance: string;
  body: string;
  availableAfter: string;
}>;

export const EVIDENCE_DOCKET: readonly EvidenceDocketRecord[] = Object.freeze([
  {
    id: 'p5.e10', phase: 'p5', title: 'Orris Pell / unsent appeal',
    provenance: 'Discipline-drawer scan; clerk notation predates the final penalty copy.',
    body: 'My daughter slept because she had carried air cages for two shifts. The first watch card says paired coverage protects an exhausted watcher. Your ruling says exhaustion proves disregard. Those cannot both be the same Way. Clerk note: Do not answer the distinction. File under Unspoken.',
    availableAfter: 'p4.control_reversal_earned',
  },
  {
    id: 'p6.m3', phase: 'p6', title: 'Route consequence list',
    provenance: 'Dispatch counts align only with Mara edition E2 issue dates.',
    body: 'Three family dispatches and one brace crew crossed the stressed bay while the substituted clean route was in force. Mara’s correction existed in her margin but did not circulate with those copies.',
    availableAfter: 'p5.civic_gallery_recurated',
  },
  {
    id: 'p6.b2', phase: 'p6', title: 'Meal chit and paired-lamp roll',
    provenance: 'Market batch time and two independently drawn lamp shapes place Pell at relief.',
    body: 'Pell collected two meals and lit the paired board at relief. A later clean rota writes the ninth toll as eight and places Iven in the cell. The ordinary meal and lamp records disagree with that clean time.',
    availableAfter: 'p5.civic_gallery_recurated',
  },
  {
    id: 'p7.e04', phase: 'p7', title: 'Seal register / two receiving hands',
    provenance: 'Receiving 44 uses Vaun’s known hand; the later seal image was pasted after receipt.',
    body: 'The approved Merrit lot is paired-warp cloth. The installed single-warp lot carries a copied Merrit seal but a North Cut delivery hand. The seal can authenticate the invoice and still be false for the delivered material.',
    availableAfter: 'p6.six_responsibilities_acknowledged',
  },
  {
    id: 'p8.e07', phase: 'p8', title: 'Private work roster / erased analyst',
    provenance: 'Carbon transfer under the clean roster preserves a seventh checking hand without a Keeper permission grant.',
    body: 'Six office leads sign their own work. A registrar checks intake, closure, and household returns across the columns. The public roster keeps the checks but removes the name. This does not add a seventh Keeper.',
    availableAfter: 'p7.nessa_publicly_cleared',
  },
  {
    id: 'p9.e05', phase: 'p9', title: 'Three route recollections',
    provenance: 'Separate messages from Ash, Rook, and Wren; none was copied from the others.',
    body: 'Ash counts the lower turn by camera frames. Rook identifies the north brace by two physical cuts. Wren gives a longer walking time and later says the turn moved. Their shared overlap fixes a route window; their differences remain evidence.',
    availableAfter: 'p8.hold_systems_repaired',
  },
  {
    id: 'p10.e01', phase: 'p10', title: 'Private quotation matrix',
    provenance: 'Four packets compared by first appearance, account visibility, and physical counter-mark.',
    body: 'Packet one carries names. Packet two adds operational plans. Packet three adds private route changes. Packet four adds fears said only at camp. The missing Rook counter-mark proves the private plan was copied before its public image; the sequence does not make fear an excuse.',
    availableAfter: 'p9.leak_window_proven',
  },
  {
    id: 'p10.e06', phase: 'p10', title: 'Wren / corrected minimizing account',
    provenance: 'Stable conversation transcript attached after the independent packet finding.',
    body: 'Wren first says the Record already knew. Shown Rook’s absent counter-mark and Ash’s sealed schedule, he corrects himself: It knew because I made knowing cheaper than letting me disappear. His response explains the choice; it does not create the proof.',
    availableAfter: 'p9.leak_window_proven',
  },
  {
    id: 'p11.e07', phase: 'p11', title: 'Paired-watch overlay and affidavit return',
    provenance: 'Brann’s lamp shapes align with one of six separate affidavit margins.',
    body: 'The watch overlay restores the ninth toll and points to the Y stroke in the returned margin. Five other professional routes produce different strokes. Together they spell a person’s name; none creates a seventh office.',
    availableAfter: 'p10.wren_remembrance_committed',
  },
  {
    id: 'p11.e09', phase: 'p11', title: 'Relationship brief / four distinct claims',
    provenance: 'Group docket assembled from the recovered packet, six affidavits, and local Record behavior.',
    body: 'Averyn is the human registrar and analyst. The Record is the civic monitoring and memory mechanism that trapped her. The Watcher is defensive Record speech through her constrained consciousness. The Dark is related pressure or cause and remains unidentified. None of these claims makes the four identical.',
    availableAfter: 'p10.wren_remembrance_committed',
  },
  {
    id: 'p12.e05', phase: 'p12', title: 'Name treatment discussion copy',
    provenance: 'Two exact release ledgers mirrored from the protected chamber after relationship restoration.',
    body: 'PUBLISH keeps Averyn’s name in the human record outside the machine and releases her. RELEASE UNNAMED removes the final filing, lets the chosen blank belong to her, and releases her. The group chooses a memory boundary, not whether she deserves freedom.',
    availableAfter: 'p11.averyn_restored_unbound',
  },
]);

export function evidenceDocketForPhase(phase: string): readonly EvidenceDocketRecord[] {
  return EVIDENCE_DOCKET.filter((record) => record.phase === phase);
}

export function formatEvidenceDocket(records: readonly EvidenceDocketRecord[]): string {
  return records.map((record) => `**${record.title}**\nProvenance: ${record.provenance}\n${record.body}`).join('\n\n');
}
