export type NessaFinding = { cause: string; record: string; conduct: string };

export const NESSA_CORRECTION_CANONICAL_PAYLOAD = Object.freeze({
  finding_shape: 'cause-record-conduct-v1',
  cause: 'genuine-diverted-counterfeit-lower-intake',
  record: 'relief-and-complaint-chronology-edited',
  conduct: 'procedure-followed-report-before-failure',
  observation_receipts: 0,
});

export const normalizeNessaFindingText = (value: string): string => value.normalize('NFKC')
  .toLocaleLowerCase('en-US').replace(/[^a-z0-9]+/g, ' ').trim().replace(/\s+/g, ' ');

const hasAny = (value: string, terms: readonly string[]): boolean => terms.some((term) => value.includes(term));

/** Component-wise meaning predicate: flexible short wording, never one hidden canonical sentence. */
export function validNessaCorrection(finding: NessaFinding): boolean {
  const cause = normalizeNessaFindingText(finding.cause);
  const record = normalizeNessaFindingText(finding.record);
  const conduct = normalizeNessaFindingText(finding.conduct);
  return hasAny(cause, ['divert', 'moved', 'rerouted'])
    && hasAny(cause, ['counterfeit', 'substitute', 'single warp'])
    && hasAny(cause, ['lower intake', 'upstream intake'])
    && hasAny(record, ['edit', 'alter', 'change', 'rewrit', 'rewrote', 'revis'])
    && hasAny(record, ['relief', 'shift'])
    && hasAny(record, ['complaint', 'report'])
    && hasAny(conduct, ['followed procedure', 'used procedure', 'worked to procedure'])
    && hasAny(conduct, ['report', 'raised alarm', 'raised the alarm', 'flagged'])
    && conduct.includes('before')
    && hasAny(conduct, ['shed', 'fail', 'broke', 'break']);
}
