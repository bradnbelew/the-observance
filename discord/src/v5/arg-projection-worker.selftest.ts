import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { ArgProjectionClaimRow } from '../db/types.js';
import { DISCORD_EVENT_MESSAGES } from './arg-projection-catalog.js';

// Runtime imports validate configuration at module load. Supply inert values; injected dependencies
// ensure this proof performs no network call and writes no external state.
process.env.DISCORD_BOT_TOKEN = 'offline-test-token';
process.env.DISCORD_APP_ID = '100000000000000001';
process.env.DISCORD_GUILD_ID = '100000000000000002';
process.env.CHANNEL_THE_RECORD = '100000000000000003';
process.env.SUPABASE_URL = 'https://offline-test.invalid';
process.env.SUPABASE_SERVICE_ROLE_KEY = 'offline-test-service-role';

const { runDiscordProjectionBatch } = await import('./arg-projection-worker.js');
const { projectionNonce } = await import('../showrunner/discord.js');

const migration = readFileSync(resolve('supabase/migrations/0017_arg_events.sql'), 'utf8');
const projectedToDiscord = new Set<string>();
const rowPattern = /\('([^']+)','P(?:1[0-2]|[1-9])','\{[^}]*\}','\{[^}]*\}','\{([^}]*)\}'\)/g;
for (const match of migration.matchAll(rowPattern)) {
  if (match[2]?.split(',').includes('discord')) projectedToDiscord.add(match[1]!);
}
assert.ok(projectedToDiscord.size >= 20, 'migration parse must find the campaign Discord projections');
assert.deepEqual(
  Object.keys(DISCORD_EVENT_MESSAGES).sort(),
  [...projectedToDiscord].sort(),
  'every event projected to Discord must have exactly one authored readback',
);
for (const [eventKey, message] of Object.entries(DISCORD_EVENT_MESSAGES)) {
  assert.ok(message.length >= 40 && message.length <= 500, `${eventKey} must be a bounded useful readback`);
  assert.ok(!/\b(correct|incorrect|answer accepted)\b/i.test(message), `${eventKey} must report a world response, not grade an answer`);
}

const known: ArgProjectionClaimRow = {
  event_id: '11111111-1111-4111-8111-111111111111',
  event_key: 'p4.copy_hypothesis_tested',
  source: 'minecraft',
  payload: { private_player_text: 'THIS MUST NEVER APPEAR' },
  occurred_at: '2026-07-18T00:00:00.000Z',
  lease_token: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  attempts: 1,
};
const unknown: ArgProjectionClaimRow = {
  ...known,
  event_id: '22222222-2222-4222-8222-222222222222',
  event_key: 'p4.future_open_ended_event',
  lease_token: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
};
const posts: Array<{ content: string; eventId: string }> = [];
const completions: Array<{ eventId: string; applied: boolean; error?: string }> = [];
const result = await runDiscordProjectionBatch({
  claim: async () => [known, unknown],
  post: async (content, eventId) => {
    posts.push({ content, eventId });
    return true;
  },
  complete: async (claim, applied, error) => {
    completions.push({ eventId: claim.event_id, applied, error });
    return true;
  },
});
assert.deepEqual(result, { claimed: 2, applied: 1, failed: 1 });
assert.equal(posts.length, 1, 'unknown events must fail closed without an improvised message');
assert.equal(posts[0]?.eventId, known.event_id);
assert.ok(!posts[0]?.content.includes('THIS MUST NEVER APPEAR'), 'player payload must never be echoed');
assert.equal(completions[0]?.applied, true);
assert.equal(completions[1]?.applied, false);
assert.match(completions[1]?.error ?? '', /no authored Discord projection/);

const nonce = projectionNonce(known.event_id);
assert.equal(nonce.length, 25, 'Discord nonces must fit the documented 25-character limit');
assert.equal(nonce, projectionNonce(known.event_id), 'retry nonce must be stable');
assert.notEqual(nonce, projectionNonce(unknown.event_id), 'different events must not share a nonce in this proof');

console.log(`arg-projection-worker.selftest OK: ${projectedToDiscord.size} authored event readbacks, payload isolation, fail-closed unknowns, stable retry nonce`);
