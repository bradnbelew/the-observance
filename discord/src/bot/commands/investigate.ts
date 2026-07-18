import { MessageFlags, type ChatInputCommandInteraction } from 'discord.js';
import { getArgEventHistory, getPlayerByDiscordId, recordArgEvent } from '../../db/repo.js';
import { postToTheRecord } from '../../showrunner/discord.js';

const PHASE_LABELS: Record<string, string> = {
  p1: 'Copperline recovery', p2: 'world handoff', p3: 'settlement accounts',
  p4: 'Mouth copies', p5: 'civic services', p6: 'six Keepers',
  p7: 'Nessa finding', p8: 'Hold repair', p9: 'last company',
  p10: 'Wren', p11: 'Averyn', p12: 'release',
};

export async function handleInvestigate(interaction: ChatInputCommandInteraction): Promise<void> {
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });
  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply('Link your Minecraft name first with /link.');
    return;
  }
  const action = interaction.options.getSubcommand(true);
  if (action === 'status') {
    const events = await getArgEventHistory();
    if (events.length === 0) {
      await interaction.editReply('No shared campaign action has been recorded yet.');
      return;
    }
    const latest = new Map<string, string>();
    for (const event of events) {
      const [phase = 'unknown'] = event.event_key.split('.');
      latest.set(phase, event.event_key);
    }
    const lines = [...latest.entries()].map(([phase, event]) => {
      const detail = event.split('.')[1] ?? event;
      return `- ${PHASE_LABELS[phase] ?? phase}: ${detail.replaceAll('_', ' ')}`;
    });
    await interaction.editReply(`Shared changes so far:\n${lines.join('\n')}`);
    return;
  }
  if (action === 'test-copy') {
    const method = interaction.options.getString('method', true);
    if (method !== 'barcode-and-node-clock') {
      await interaction.editReply('That test relies on timestamps or filenames written by the damaged guest. It cannot order the retained cartridges. Nothing changed.');
      return;
    }
    const result = await recordArgEvent({
      eventKey: 'p4.copy_hypothesis_tested',
      idempotencyKey: 'discord:p4:barcode-node-clock-test',
      source: 'discord',
      actorId: interaction.user.id,
      payload: { method, finding: 'copy-order-independent-of-guest-metadata' },
    });
    if (result.status === 'blocked') {
      await interaction.editReply('The retained Mouth revision is not available yet. Nothing changed.');
      return;
    }
    if (result.status === 'collision') {
      await interaction.editReply('A different copy test already owns that receipt. Nothing changed; use /investigate status.');
      return;
    }
    await interaction.editReply(result.created
      ? 'Test complete. Barcode 03 was imaged before 04, and the independent recovery-node clock stayed stable. Guest filenames and modified times cannot reverse that order.'
      : 'That custody test is already complete. Nothing was duplicated.');
    if (result.created) {
      void postToTheRecord('Copy test complete: cartridge 03 precedes 04. The recovery-node clock stayed stable; guest filenames and modified times are excluded from the order.');
    }
    return;
  }
  if (action === 'clear-nessa') {
    const cause = interaction.options.getString('cause', true);
    const record = interaction.options.getString('record', true);
    const conduct = interaction.options.getString('conduct', true);
    if (cause !== 'diversion-counterfeit-lower-intake'
        || record !== 'edited-relief-and-complaints'
        || conduct !== 'followed-and-reported-before-shedding') {
      await interaction.editReply('Those three findings do not support a complete public correction. Nothing changed.');
      return;
    }
    const result = await recordArgEvent({
      eventKey: 'p7.nessa_publicly_cleared',
      idempotencyKey: 'discord:p7:nessa-public-correction',
      source: 'discord',
      actorId: interaction.user.id,
      payload: { cause, record, conduct, observation_receipts: 0 },
    });
    if (result.status === 'blocked') {
      await interaction.editReply('The six Keeper responsibility records are not complete yet. Nothing changed.');
      return;
    }
    if (result.status === 'collision') {
      await interaction.editReply('A different Nessa correction already owns that receipt. Nothing changed; use /investigate status.');
      return;
    }
    await interaction.editReply(result.created
      ? 'Correction filed. Nessa Vale followed procedure and reported before the shedding. Genuine stock was diverted, counterfeit cloth failed upstream, and relief and complaint records were edited.'
      : 'That public correction is already filed. Nothing was duplicated.');
    if (result.created) {
      void postToTheRecord('Public correction: Nessa Vale followed procedure and reported before the shedding. Genuine stock was diverted, counterfeit cloth failed upstream, and the relief and complaint records were edited.');
    }
    return;
  }
  if (action === 'plan-repair') {
    const causeModel = interaction.options.getString('cause-model', true);
    const issFinding = interaction.options.getString('iss-finding', true);
    const worksOrder = interaction.options.getString('works-order', true);
    if (causeModel !== 'fracture-heat-watch-routing'
        || issFinding !== 'surface-true-route-unsafe'
        || worksOrder !== 'water-light-pressure-route') {
      await interaction.editReply('That model either drops a proven cause, turns Iss into the only cause, or starts work before the systems are safe. Nothing changed.');
      return;
    }
    const result = await recordArgEvent({
      eventKey: 'p8.intervention_plan_accepted',
      idempotencyKey: 'discord:p8:intervention-plan',
      source: 'discord',
      actorId: interaction.user.id,
      payload: {
        cause_model: causeModel,
        iss_finding: issFinding,
        works_order: worksOrder,
        observation_receipts: 0,
      },
    });
    if (result.status === 'blocked') {
      await interaction.editReply('Nessa’s public correction is not on the shared record yet. Nothing changed.');
      return;
    }
    if (result.status === 'collision') {
      await interaction.editReply('A different intervention plan already owns that receipt. Nothing changed; use /investigate status.');
      return;
    }
    await interaction.editReply(result.created
      ? 'Plan accepted. Treat the Break as interacting failures. Keep Iss’s sound surface proof and reject his unsafe route. The Hold works can now be repaired in the tested order.'
      : 'That intervention plan is already accepted. Nothing was duplicated.');
    if (result.created) {
      void postToTheRecord('Intervention plan accepted: restore water, paired light, and pressure control before opening the staff route. Iss’s surface proof stands; his unreviewed cut does not.');
    }
    return;
  }
  if (action === 'file-leak-window') {
    const readiness = interaction.options.getString('readiness', true);
    const privateObject = interaction.options.getString('private-object', true);
    const window = interaction.options.getString('window', true);
    const boundary = interaction.options.getString('boundary', true);
    if (readiness !== 'release-ready' || privateObject !== 'rook-revision-and-identities'
        || window !== 'private-before-public' || boundary !== 'insider-unknown') {
      await interaction.editReply('That filing either treats the company as unprepared, uses public material, breaks the version order, or names a sender P9 has not proved. Nothing changed.');
      return;
    }
    const result = await recordArgEvent({
      eventKey: 'p9.leak_window_proven',
      idempotencyKey: 'discord:p9:private-revision-window',
      source: 'discord',
      actorId: interaction.user.id,
      payload: { readiness, private_object: privateObject, window, boundary, observation_receipts: 0 },
    });
    if (result.status === 'blocked') {
      await interaction.editReply('The four Ash Camp owner cards are not restored yet. Nothing changed.');
      return;
    }
    if (result.status === 'collision') {
      await interaction.editReply('A different private-window filing already owns that receipt. Nothing changed; use /investigate status.');
      return;
    }
    await interaction.editReply(result.created
      ? 'Finding filed. The four-person company was ready. Rook’s private revision and their identities crossed into the Witness Spool before public upload. The sender remains an open question.'
      : 'That private-window finding is already filed. Nothing was duplicated.');
    if (result.created) {
      void postToTheRecord('Ash Camp finding: the release work was ready. Rook’s private revision and the team identities crossed before public upload. The transmission came from inside the four-person company; P9 does not name the sender.');
    }
    return;
  }
  if (action !== 'dispatch') throw new Error('unsupported investigate action');

  const summary = interaction.options.getString('summary', true).normalize('NFKC').trim().replace(/\s+/g, ' ');
  if (summary.length < 12 || summary.length > 180) {
    await interaction.editReply('Write one plain 12-180 character summary of the disagreement you want the settlement to keep open.');
    return;
  }
  const result = await recordArgEvent({
    eventKey: 'p3.dispatch_authorized',
    idempotencyKey: 'discord:p3:settlement-dispatch',
    source: 'discord',
    actorId: interaction.user.id,
    payload: { summary, minecraft_name: player.name },
  });
  if (result.status === 'blocked') {
    await interaction.editReply('The settlement interviews are not open yet. Nothing changed.');
    return;
  }
  if (result.status === 'collision') {
    await interaction.editReply('A settlement dispatch is already on file. Use /investigate status to read the shared state.');
    return;
  }
  await interaction.editReply(result.created
    ? 'Dispatch accepted. The settlement will keep the conflicting accounts open instead of forcing one version.'
    : 'That dispatch is already on file. Nothing was duplicated.');
  if (result.created) {
    void postToTheRecord('Settlement dispatch received. Aro and Dob will keep both accounts in the public record until the Mouth copies can be compared.');
  }
}
