import type { OutcomeBeat } from '../db/types.js';
import { archiveLine } from '../voice.archive.js';

/** Pure symbolic archive-key resolver, isolated from database and secret-bearing config. */
export function resolvePrivateMessageKey(beat: OutcomeBeat): OutcomeBeat {
  if (beat.type !== 'unlock') return beat;
  const outer = beat.payload ?? {};
  if (outer['step'] !== 'private_message') return beat;
  const stepPayload = (outer['step_payload'] ?? {}) as Record<string, unknown>;
  const key = stepPayload['key'];
  if (typeof key !== 'string' || key.length === 0) return beat;
  if (stepPayload['title'] || stepPayload['subtitle'] || stepPayload['actionbar'] || stepPayload['text']) {
    return beat;
  }
  const line = archiveLine(key);
  if (line === null) return beat;
  const { key: _drop, ...rest } = stepPayload;
  return {
    ...beat,
    payload: { ...outer, step_payload: { ...rest, subtitle: line } },
  };
}
