export const SETTLEMENT_DISPATCH_CANONICAL_PAYLOAD = Object.freeze({
  action: 'preserve-conflicting-resident-accounts',
  scope: 'changed-mark-and-work-history',
  conclusion_status: 'open-for-covered-survey',
  observation_receipts: 0,
});

const fold = (value: string): string => value.normalize('NFKC').toLocaleLowerCase('en-US')
  .replace(/[^a-z0-9]+/g, ' ').trim().replace(/\s+/g, ' ');
const any = (value: string, terms: readonly string[]): boolean => terms.some((term) => value.includes(term));

/** A field note must preserve a real disagreement without choosing an official story. */
export function validSettlementDispatch(raw: string): boolean {
  const value = fold(raw);
  if (value.length < 12 || value.length > 180) return false;
  return any(value, ['disagree', 'conflict', 'contradict', 'different account', 'accounts differ', 'both accounts'])
    && any(value, ['mark', 'date', 'time', 'name', 'place', 'location', 'work'])
    && any(value, ['keep open', 'keep both', 'leave open', 'preserve both', 'record both', 'do not choose', 'don t choose', 'no official version', 'without choosing', 'cannot settle', 'can t settle', 'not enough to decide', 'not enough to choose', 'needs checking', 'needs more checking']);
}
