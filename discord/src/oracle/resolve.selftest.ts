/**
 * resolve.selftest.ts — server-free guard on resolve.ts's pure `private_message` key-resolver
 * (the fix for `backlog-liar-engine`/`backlog-unlockbeat-producers`'s R-A: PrivateMessageBeat only
 * reads title/subtitle/actionbar/text, but authors write a symbolic `key` instead). A regression
 * here would silently reintroduce the "no-wall-catch" cold-flip no-op this fix closes.
 */
import { resolvePrivateMessageKey } from './private-message.js';
import type { OutcomeBeat } from '../db/types.js';

function unlockBeat(stepPayload: Record<string, unknown>): OutcomeBeat {
  return { type: 'unlock', payload: { step: 'private_message', step_payload: stepPayload } };
}

function selfTest(): boolean {
  // A known key resolves into a real subtitle the plugin beat actually reads.
  const resolved = resolvePrivateMessageKey(unlockBeat({ key: 'iss.dialogue.turns_cold' }));
  const resolvedPayload = (resolved.payload as any)?.step_payload ?? {};
  if (typeof resolvedPayload.subtitle !== 'string' || resolvedPayload.subtitle.length === 0) return false;
  if (resolvedPayload.key !== undefined) return false; // the symbolic key must not leak through to the plugin

  // An unknown key is left alone (degrades to the beat's existing empty-skip, never a guessed line).
  const unknown = resolvePrivateMessageKey(unlockBeat({ key: 'no.such.key' }));
  if ((unknown.payload as any)?.step_payload?.key !== 'no.such.key') return false;

  // A row that already carries a real field is left untouched (a key alongside one is never overwritten).
  const already = resolvePrivateMessageKey(unlockBeat({ key: 'iss.dialogue.turns_cold', subtitle: 'authored verbatim' }));
  if ((already.payload as any)?.step_payload?.subtitle !== 'authored verbatim') return false;

  // Non-private_message unlock steps (e.g. reveal) pass through completely untouched.
  const other: OutcomeBeat = { type: 'unlock', payload: { step: 'reveal', step_payload: { cells: [] } } };
  if (resolvePrivateMessageKey(other) !== other) return false;

  // Non-unlock beat types pass through untouched.
  const nonUnlock: OutcomeBeat = { type: 'advancement_toast', payload: { advancement: 'observance:x' } };
  if (resolvePrivateMessageKey(nonUnlock) !== nonUnlock) return false;

  return true;
}

const ok = selfTest();
console.log(ok
  ? '  ok   private_message key-resolver: known key resolves, unknown/authored/non-matching pass through\n\nresolve.selftest: OK — the private_message key-resolver holds.'
  : 'resolve.selftest: FAILED');
if (!ok) process.exit(1);
