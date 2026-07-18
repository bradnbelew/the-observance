import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string): string => readFileSync(resolve(path), 'utf8');
const index = read('src/bot/index.ts');
const register = read('src/bot/register.ts');
const handler = read('src/bot/commands/investigate.ts');
const repo = read('src/db/repo.ts');
const migration = read('supabase/migrations/0017_arg_events.sql');
const leases = read('supabase/migrations/0018_arg_projection_leases.sql');
const rollback = read('supabase/rollbacks/0018_arg_projection_leases.rollback.sql');

assert.ok(register.includes(".setName('investigate')"));
assert.ok(register.includes(".setName('dispatch')"));
assert.ok(register.includes(".setName('test-copy')"));
assert.ok(register.includes(".setName('review-nessa')"));
assert.ok(register.includes(".setName('file-nessa')"));
assert.ok(!register.includes('diversion-counterfeit-lower-intake'));
assert.ok(!register.includes('edited-relief-and-complaints'));
assert.ok(!register.includes('followed-and-reported-before-shedding'));
assert.ok(!register.includes(".setName('plan-repair')"));
assert.ok(!register.includes(".setName('file-leak-window')"));
assert.ok(register.includes(".setName('contact-wren')"));
assert.ok(!register.includes(".setName('confront-wren')"));
assert.ok(register.includes(".setName('restore-name')"));
assert.ok(!register.includes(".setName('identify-averyn')"));
assert.ok(index.includes("case 'investigate':"));
assert.ok(index.includes('handleInvestigate(interaction)'));
assert.ok(index.includes('interaction.isModalSubmit()'));
assert.ok(index.includes('handleInvestigateModal(interaction)'));

// Ordinary community speech is never parsed as a hidden answer surface.
assert.ok(!index.includes("client.on('messageCreate'"));
assert.ok(!index.includes('GatewayIntentBits.MessageContent'));

// Discord actions use a documented interaction and one server-owned event RPC.
assert.ok(handler.includes("getSubcommand(true)"));
assert.ok(handler.includes("eventKey: 'p3.dispatch_authorized'"));
assert.ok(handler.includes('validSettlementDispatch(summary)'));
assert.ok(handler.includes('payload: SETTLEMENT_DISPATCH_CANONICAL_PAYLOAD'));
assert.ok(!handler.includes('payload: { summary'));
assert.ok(handler.includes("eventKey: 'p4.copy_hypothesis_tested'"));
assert.ok(handler.includes("method !== 'barcode-and-node-clock'"));
assert.ok(handler.includes("eventKey: 'p7.nessa_publicly_cleared'"));
assert.ok(handler.includes("'observance:p7:nessa-correction:v1'"));
assert.ok(handler.includes('interaction.showModal(nessaCorrectionModal())'));
assert.ok(handler.includes("interaction.fields.getTextInputValue('cause')"));
assert.ok(!handler.includes("eventKey: 'p8.intervention_plan_accepted'"));
assert.ok(!handler.includes("eventKey: 'p9.leak_window_proven'"));
assert.ok(handler.includes("eventKey: 'p10.wren_confronted'"));
assert.ok(handler.includes("'observance:p10:wren-transmission:v1'"));
assert.ok(handler.includes('interaction.showModal(wrenTransmissionModal())'));
assert.ok(handler.includes("interaction.fields.getTextInputValue('proof')"));
assert.ok(handler.includes("eventKey: 'p11.averyn_identified'"));
assert.ok(!handler.includes("eventKey: 'p11.averyn_restored_unbound'"));
assert.ok(!handler.includes('postToTheRecord'),
  'commands must not bypass the durable projection outbox or duplicate its Discord consequence');
assert.ok(handler.includes("name !== 'AVERYN'"));
assert.ok(!handler.includes("dark !== 'related-distinct-unknown'"));
assert.ok(!handler.includes("proof !== 'progressive-private-missing-countermark'"));
assert.ok(!handler.includes("boundary !== 'insider-unknown'"));
assert.ok(handler.includes('observation_receipts: 0'));
assert.ok(handler.includes("idempotencyKey: 'discord:p3:settlement-dispatch'"));
assert.ok(handler.includes('.normalize(\'NFKC\')'));
assert.ok(repo.includes("rpc('observance_record_arg_event'"));
assert.ok(migration.includes('revoke all on function public.observance_record_arg_event'));
assert.ok(migration.includes('grant execute on function public.observance_record_arg_event'));
assert.ok(migration.includes("('p8.intervention_plan_accepted','P8','{p7.nessa_publicly_cleared}','{minecraft,copperline}'"));
assert.ok(!migration.includes("('p8.intervention_plan_accepted','P8','{p7.nessa_publicly_cleared}','{minecraft,discord}'"));
assert.ok(migration.includes("('p9.leak_window_proven','P9','{p9.company_biographies_restored}','{minecraft,copperline,media}'"));
assert.ok(leases.includes('for update of p skip locked'));
assert.ok(leases.includes("p.attempts < 8"));
assert.ok(leases.includes("p.status = 'processing' and p.lease_expires_at <= now()"));
assert.ok(leases.includes("p.status = 'failed' and (p.next_attempt_at is null or p.next_attempt_at <= now())"));
assert.ok(leases.includes('power(2, greatest(0, p.attempts - 1))'));
assert.ok(leases.includes('observance_claim_arg_projections'));
assert.ok(leases.includes('observance_complete_arg_projection'));
assert.ok(leases.includes('p.lease_token = p_lease_token'));
assert.ok(leases.includes('from public, anon, authenticated'));
assert.ok(rollback.includes("where status = 'processing'"));
assert.ok(rollback.includes("check (status in ('queued', 'applied', 'failed'))"));
assert.ok(rollback.includes('drop column if exists next_attempt_at'));

// The authored action accepts a bounded natural sentence, not a magic long phrase.
assert.ok(register.includes('.setMinLength(12)'));
assert.ok(register.includes('.setMaxLength(180)'));
assert.ok(register.includes('no hidden phrase is required'));

console.log('Discord ARG event/input selftest: PASS');
