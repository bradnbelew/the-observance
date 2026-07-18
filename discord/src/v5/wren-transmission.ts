export type WrenTransmissionFinding = { proof: string; pattern: string; motive: string };

export const WREN_TRANSMISSION_CANONICAL_PAYLOAD = Object.freeze({
  sender: 'wren',
  packet_payload: ['names', 'plans', 'routes', 'fears'],
  proof: 'progressive-private-knowledge-with-physical-countermark-absent',
  motive: 'fear-explains-choice-responsibility-remains',
  observation_receipts: 0,
});

const fold = (value: string): string => value.normalize('NFKC').toLocaleLowerCase('en-US')
  .replace(/[^a-z0-9]+/g, ' ').trim().replace(/\s+/g, ' ');
const any = (value: string, words: readonly string[]): boolean => words.some((word) => value.includes(word));

/** Separate provenance, transmission pattern, and motive. No single hidden sentence is accepted. */
export function validWrenTransmission(finding: WrenTransmissionFinding): boolean {
  const proof = fold(finding.proof);
  const pattern = fold(finding.pattern);
  const motive = fold(finding.motive);
  return proof.includes('wren')
    && any(proof, ['private revision', 'private route', 'rook revision', 'north brace'])
    && any(proof, ['countermark absent', 'counter mark absent', 'missing countermark', 'missing counter mark', 'countermark was missing', 'counter mark was missing', 'physical mark missing', 'physical mark was missing'])
    && any(pattern, ['progressive packets', 'packet progression', 'four packets', 'increasing packets'])
    && pattern.includes('name') && any(pattern, ['plan', 'route']) && pattern.includes('fear')
    && any(motive, ['erased', 'erasure', 'disappear', 'losing himself'])
    && any(motive, ['chose', 'choice', 'deliberate', 'responsible', 'responsibility'])
    && any(motive, ['does not excuse', 'not excuse', 'does not erase responsibility', 'responsibility remains', 'still responsible']);
}
