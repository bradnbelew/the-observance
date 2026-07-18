export type WrenTransmissionFinding = { proof: string; pattern: string; motive: string };

export const WREN_TRANSMISSION_CANONICAL_PAYLOAD = Object.freeze({
  sender: 'wren',
  packet_payload: ['names', 'plans', 'routes', 'fears'],
  proof: 'progressive-private-knowledge-with-physical-countermark-absent',
  motive: 'fear-explains-choice-responsibility-remains',
  observation_receipts: 0,
});

export type WrenTransmissionResponse =
  | 'supported' | 'confession_only' | 'wrong_sender' | 'single_packet'
  | 'missing_provenance' | 'absolution' | 'missing_motive' | 'incomplete';

export const WREN_TRANSMISSION_RESPONSE_TEXT: Readonly<Record<WrenTransmissionResponse, string>> = Object.freeze({
  supported: 'The provenance, packet progression, and motive boundary support the finding.',
  confession_only: 'Wren\'s words can answer evidence, but they cannot authenticate the private revision. Use an independent custody difference.',
  wrong_sender: 'That person may explain one source, but the finding must identify who knew the private revision before its public copy.',
  single_packet: 'One packet proves possession at one moment. Compare how the retained packets gain new categories over time.',
  missing_provenance: 'The claim lacks a physical or version difference that the sender could not learn from the public copy.',
  absolution: 'Fear may explain a choice. It does not remove agency or responsibility for what was sent.',
  missing_motive: 'The transmission can be attributed before motive is known, but Wren\'s response needs a supported fear and a clear responsibility boundary.',
  incomplete: 'The finding does not yet connect private provenance, packet progression, and responsible choice.',
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
    && any(pattern, ['progressive packets', 'packet progression', 'four packets', 'increasing packets', 'each packet knew more', 'packets grew', 'copies grew'])
    && any(pattern, ['name', 'people']) && any(pattern, ['plan', 'route', 'build'])
    && any(pattern, ['fear', 'afraid', 'private worry'])
    && any(motive, ['erased', 'erasure', 'disappear', 'losing himself', 'being forgotten'])
    && any(motive, ['chose', 'choice', 'deliberate', 'responsible', 'responsibility'])
    && any(motive, ['does not excuse', 'not excuse', 'does not erase responsibility', 'responsibility remains', 'still responsible']);
}

/** Non-committing response to a player's actual theory; deliberately does not supply missing facts. */
export function wrenTransmissionResponse(finding: WrenTransmissionFinding): WrenTransmissionResponse {
  if (validWrenTransmission(finding)) return 'supported';
  const proof = fold(finding.proof);
  const pattern = fold(finding.pattern);
  const motive = fold(finding.motive);
  if (any(motive, ['fear excuses', 'fear removes responsibility', 'not responsible', 'absolves', 'forgive'])) return 'absolution';
  const privateProof = any(proof, ['private revision', 'private route', 'rook revision', 'north brace'])
    && any(proof, ['countermark absent', 'counter mark absent', 'missing countermark', 'missing counter mark', 'countermark was missing', 'counter mark was missing', 'physical mark missing', 'physical mark was missing']);
  if (!privateProof && any(proof, ['confessed', 'confession', 'said it', 'admitted', 'wren said'])) return 'confession_only';
  if (!proof.includes('wren') && any(proof, ['mkept', 'ash', 'rook'])) return 'wrong_sender';
  if (!privateProof) return 'missing_provenance';
  if (!any(pattern, ['progressive packets', 'packet progression', 'four packets', 'increasing packets', 'each packet knew more', 'packets grew', 'copies grew'])) return 'single_packet';
  if (!any(motive, ['erased', 'erasure', 'disappear', 'losing himself', 'being forgotten'])) return 'missing_motive';
  return 'incomplete';
}
