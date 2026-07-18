import assert from 'node:assert/strict';
import { validWrenTransmission, WREN_TRANSMISSION_CANONICAL_PAYLOAD } from './wren-transmission.js';

assert.equal(WREN_TRANSMISSION_CANONICAL_PAYLOAD.observation_receipts, 0);
for (const finding of [
  { proof: 'Wren knew Rook\'s private revision, but the physical counter-mark was missing.', pattern: 'Four progressive packets added names, plans, routes, and fears.', motive: 'He feared erasure and chose to send them. That does not excuse him.' },
  { proof: 'The north brace came through Wren; its physical mark was missing.', pattern: 'The packet progression moved from names to route plans and private fears.', motive: 'He was afraid of disappearing. He is still responsible for that deliberate choice.' },
  { proof: 'Wren knew the north-brace private route when the physical mark was missing.', pattern: 'Each packet knew more: people, then the build route, then a private worry.', motive: 'Being forgotten explains why he chose this. He is still responsible.' },
]) assert.equal(validWrenTransmission(finding), true);

for (const finding of [
  { proof: 'Wren was nervous.', pattern: 'One route appeared once.', motive: 'Fear removes responsibility.' },
  { proof: 'Rook private route, missing countermark.', pattern: 'Progressive packets had names, plans, routes, and fears.', motive: 'Wren feared erasure and remains responsible.' },
  { proof: 'Wren private route, missing countermark.', pattern: 'Progressive packets had names and fears.', motive: 'Wren feared erasure and remains responsible.' },
]) assert.equal(validWrenTransmission(finding), false);

console.log('wren-transmission.selftest OK: provenance, progression, motive, and wrong-theory refusal');
