/**
 * poster.ts — the watcher's hand. Clean, side-effecting senders the showrunner
 * calls to lay words and artifacts into the channels.
 *
 *   postReport(client, channelId, text)
 *     - posts a plain line (already in the watcher's voice — pass a voice.ts
 *       string). used for reports + the clue drip prose in #the-record.
 *
 *   postClue(client, channelId, forgeOrRenderResult)
 *     - posts a clue artifact PNG. accepts either:
 *         * a PNG Buffer (a render result already produced by the render layer), or
 *         * a ForgedClue from the forge — which this renders into the brand frame
 *           (clueCard) first.
 *       the same Keeper's Eye sigil is the watermark on every clue card.
 *
 * These functions DO NOT compose English. Reports must be passed pre-worded from
 * voice.ts. Clue cards carry the forged runes + their seeded prose, not chatter.
 */
import { AttachmentBuilder, type Client } from 'discord.js';
import type { ForgedClue } from '../../forge/index.js';
import { brand } from '../../brand.js';
import { clueCard, type ClueCardData } from '../../render/cards.js';
import { renderPng } from '../../render/render.js';
import { renderClueDetailed, type ClueRenderSpec } from '../../forge/templates/index.js';

/**
 * Anything postClue can be handed:
 *   - a ready PNG Buffer,
 *   - a forged clue (framed into the brand clue card),
 *   - a { png, name } render result,
 *   - an explicit { card } brand-frame spec, or
 *   - a { clue } forge render spec — the showrunner's natural input: the forge
 *     carves the runes and one of the five themed templates frames them.
 */
export type CluePayload =
  | Buffer
  | ForgedClue
  | { png: Buffer; name?: string }
  | { card: ClueCardData }
  | { clue: ClueRenderSpec };

/**
 * Resolve a text-capable channel by id and assert it can receive messages. The
 * showrunner passes channel ids from config.channels.
 */
async function resolveSendable(client: Client, channelId: string) {
  const channel = await client.channels.fetch(channelId);
  if (!channel || !channel.isTextBased() || !('send' in channel)) {
    throw new Error(`poster: channel ${channelId} is not a sendable text channel`);
  }
  return channel;
}

/**
 * Post a single line of the watcher's record. `text` must already be a
 * voice.ts string — this function adds no words of its own.
 */
export async function postReport(
  client: Client,
  channelId: string,
  text: string,
): Promise<void> {
  const channel = await resolveSendable(client, channelId);
  await channel.send({ content: text });
}

/**
 * Post a clue artifact as a PNG attachment. Renders a forge spec ({ clue }) into
 * a themed rune template, frames a ForgedClue or { card } into the brand frame,
 * and sends a ready Buffer (or { png }) as-is.
 */
export async function postClue(
  client: Client,
  channelId: string,
  payload: CluePayload,
): Promise<void> {
  const { png, name } = await toPng(payload);
  const channel = await resolveSendable(client, channelId);
  const file = new AttachmentBuilder(png, { name });
  await channel.send({ files: [file] });
}

/** Normalize any accepted payload into a PNG buffer + filename. */
async function toPng(payload: CluePayload): Promise<{ png: Buffer; name: string }> {
  // a ready PNG buffer
  if (Buffer.isBuffer(payload)) {
    return { png: payload, name: 'clue.png' };
  }

  // { png, name? } render result
  if ('png' in payload && Buffer.isBuffer(payload.png)) {
    return { png: payload.png, name: payload.name ?? 'clue.png' };
  }

  // a forge render spec — carve the runes + frame them with a themed template.
  if ('clue' in payload) {
    const { png, forged } = await renderClueDetailed(payload.clue);
    return { png, name: `${forged.puzzleKey}.png` };
  }

  // an explicit brand-frame card spec
  if ('card' in payload) {
    const png = await renderCard(payload.card);
    return { png, name: 'clue.png' };
  }

  // a ForgedClue — frame its meta into the standard brand clue card.
  const forged = payload as ForgedClue;
  const png = await renderCard({
    eyebrow: 'the record',
    puzzleKey: forged.puzzleKey,
    tier: 1,
    body: forged.meta.keyHint,
    footer: forged.meta.cipher,
  });
  return { png, name: `${forged.puzzleKey}.png` };
}

/** Render a clue card to a PNG buffer at brand clue dimensions. */
async function renderCard(data: ClueCardData): Promise<Buffer> {
  return renderPng(clueCard(data), brand.canvas.clueWidth, brand.canvas.clueHeight);
}
