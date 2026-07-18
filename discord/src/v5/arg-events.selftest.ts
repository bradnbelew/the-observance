import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string): string => readFileSync(resolve(path), 'utf8');
const index = read('src/bot/index.ts');
const register = read('src/bot/register.ts');
const handler = read('src/bot/commands/investigate.ts');
const repo = read('src/db/repo.ts');
const migration = read('supabase/migrations/0017_arg_events.sql');

assert.ok(register.includes(".setName('investigate')"));
assert.ok(register.includes(".setName('dispatch')"));
assert.ok(register.includes(".setName('test-copy')"));
assert.ok(register.includes(".setName('clear-nessa')"));
assert.ok(register.includes(".setName('plan-repair')"));
assert.ok(index.includes("case 'investigate':"));
assert.ok(index.includes('handleInvestigate(interaction)'));

// Ordinary community speech is never parsed as a hidden answer surface.
assert.ok(!index.includes("client.on('messageCreate'"));
assert.ok(!index.includes('GatewayIntentBits.MessageContent'));

// Discord actions use a documented interaction and one server-owned event RPC.
assert.ok(handler.includes("getSubcommand(true)"));
assert.ok(handler.includes("eventKey: 'p3.dispatch_authorized'"));
assert.ok(handler.includes("eventKey: 'p4.copy_hypothesis_tested'"));
assert.ok(handler.includes("method !== 'barcode-and-node-clock'"));
assert.ok(handler.includes("eventKey: 'p7.nessa_publicly_cleared'"));
assert.ok(handler.includes("eventKey: 'p8.intervention_plan_accepted'"));
assert.ok(handler.includes("causeModel !== 'fracture-heat-watch-routing'"));
assert.ok(handler.includes('observation_receipts: 0'));
assert.ok(handler.includes("idempotencyKey: 'discord:p3:settlement-dispatch'"));
assert.ok(handler.includes('.normalize(\'NFKC\')'));
assert.ok(repo.includes("rpc('observance_record_arg_event'"));
assert.ok(migration.includes('revoke all on function public.observance_record_arg_event'));
assert.ok(migration.includes('grant execute on function public.observance_record_arg_event'));

// The authored action accepts a bounded natural sentence, not a magic long phrase.
assert.ok(register.includes('.setMinLength(12)'));
assert.ok(register.includes('.setMaxLength(180)'));
assert.ok(register.includes('no hidden phrase is required'));

console.log('Discord ARG event/input selftest: PASS');
