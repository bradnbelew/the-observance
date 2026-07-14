/**
 * LS02's exact answer grammar. Production imports are restricted to the dedicated Server Action;
 * the source-level self-test imports it directly to lock normalization and rejection behavior.
 */
const NUMBER_WORDS: Readonly<Record<string, string>> = Object.freeze({
  ONE: '1',
  EIGHT: '8',
  FOUR: '4',
  TWO: '2',
});

/** Collapse harmless case, Unicode-width, punctuation, and word-spacing differences. */
export function normalizeLs02DocketAnswer(raw: string): string {
  const normalized = raw.normalize('NFKC').toUpperCase();
  const compact = normalized.replace(/[^A-Z0-9]+/g, '');
  if (compact === '1842' || compact === 'SERVICE1842') return compact;
  if (compact === 'ONEEIGHTFOURTWO') return '1842';
  if (compact === 'SERVICEONEEIGHTFOURTWO') return 'SERVICE1842';

  const tokens = normalized.match(/[A-Z]+|[0-9]+/g) ?? [];
  return tokens.map((token) => NUMBER_WORDS[token] ?? token).join('');
}

export function isCorrectLs02DocketAnswer(raw: string): boolean {
  const answer = normalizeLs02DocketAnswer(raw);
  return answer === '1842' || answer === 'SERVICE1842';
}
