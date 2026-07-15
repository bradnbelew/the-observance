/**
 * /progress — the standing docket: every open required finding, and which of the
 * three surfaces (the world itself, this record, or the copperline site) takes it.
 *
 * Players asked, more than once, where a finding is actually filed. The game already
 * carries this per-node (`investigation_nodes.input_surface`, authored per node in
 * `design/ARG-V5-NODE-MANIFEST.csv`); this command just reads the live open set and
 * states it plainly, grouped by case. No new game state, no new authored content —
 * a read-only view over data that already exists.
 *
 * Ephemeral: the docket can spoil which cases are near completion for players who
 * haven't caught up, so only the asker sees the reply.
 */
import { MessageFlags, type ChatInputCommandInteraction } from 'discord.js';
import {
  getPlayerByDiscordId,
  getOpenInvestigationNodes,
  getCaseTitles,
  type OpenInvestigationNode,
} from '../../db/repo.js';
import { voice } from '../../voice.js';

const SOURCE = 'the-watcher/progress';

/** Which of the three player-facing surfaces a node's input_surface value routes to. */
function channelFor(inputSurface: string): 'world' | 'discord' | 'site' {
  const s = inputSurface.toLowerCase();
  if (s.includes('website')) return 'site';
  if (s.includes('discord')) return 'discord';
  return 'world';
}

export async function handleProgress(
  interaction: ChatInputCommandInteraction,
): Promise<void> {
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });

  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply({ content: voice.progressNotLinked() });
    return;
  }

  let open: OpenInvestigationNode[];
  let caseTitles: Record<string, string>;
  try {
    [open, caseTitles] = await Promise.all([getOpenInvestigationNodes(), getCaseTitles()]);
  } catch (err) {
    console.error('[the-watcher/progress] docket read failed:', err);
    await interaction.editReply({ content: voice.noBudget() });
    return;
  }

  if (open.length === 0) {
    await interaction.editReply({ content: voice.progressHeader('the record', 0) });
    return;
  }

  const byCase = new Map<string, OpenInvestigationNode[]>();
  for (const node of open) {
    const bucket = byCase.get(node.caseKey);
    if (bucket) bucket.push(node);
    else byCase.set(node.caseKey, [node]);
  }

  const sections: string[] = [];
  for (const [caseKey, nodes] of byCase) {
    const caseTitle = caseTitles[caseKey] ?? caseKey;
    const lines = nodes
      .sort((a, b) => a.ordinal - b.ordinal)
      .map((node) => voice.progressLine(node.title, channelFor(node.inputSurface), node.inputSurface));
    sections.push([voice.progressHeader(caseTitle, nodes.length), ...lines].join('\n'));
  }

  await interaction.editReply({ content: sections.join('\n\n') });
  void logEventSafe(`docket read: ${player.name} (${open.length} open across ${byCase.size} cases)`);
}

async function logEventSafe(message: string): Promise<void> {
  try {
    const { logEvent } = await import('../../db/repo.js');
    await logEvent('info', SOURCE, message);
  } catch {
    // best-effort audit only; never blocks the reply
  }
}
