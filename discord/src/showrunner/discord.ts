/**
 * The thin Discord REST seam for the showrunner cron. The bot is a long-running gateway client; the
 * showrunner is a short-lived cron, so it posts via the REST API directly (one fetch) rather than
 * spinning up a gateway connection. The Watcher's voice is still the SOLE text source (voice.ts) —
 * this only delivers it, keeping the cross-surface voice identical to the bot's.
 */
import { config } from '../config.js';
import { createHash } from 'node:crypto';

const API = 'https://discord.com/api/v10';

/** Post a message to #the-record. Returns true on success; never throws (failure → false, logged by caller). */
export async function postToTheRecord(content: string): Promise<boolean> {
  try {
    const res = await fetch(`${API}/channels/${config.channels.theRecord}/messages`, {
      method: 'POST',
      headers: {
        Authorization: `Bot ${config.discord.botToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ content }),
    });
    return res.ok;
  } catch {
    return false;
  }
}

/** Stable 25-character Discord nonce derived from a canonical event id. */
export function projectionNonce(eventId: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(eventId)) {
    throw new Error('invalid ARG event id');
  }
  return createHash('sha256').update(eventId, 'utf8').digest('hex').slice(0, 25);
}

/**
 * Deliver one canonical event consequence. Discord's nonce/enforce_nonce pair makes a retry in
 * the lease window return the earlier message rather than creating a duplicate. Player payloads
 * are never echoed; the caller supplies exact authored text only.
 */
export async function postProjectionToTheRecord(content: string, eventId: string): Promise<boolean> {
  if (content.length < 1 || content.length > 2_000) return false;
  try {
    const res = await fetch(`${API}/channels/${config.channels.theRecord}/messages`, {
      method: 'POST',
      headers: {
        Authorization: `Bot ${config.discord.botToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        content,
        nonce: projectionNonce(eventId),
        enforce_nonce: true,
        allowed_mentions: { parse: [] },
      }),
    });
    return res.ok;
  } catch {
    return false;
  }
}

/**
 * Post a clue-card PNG to #the-record as a file attachment (COHERENCE-AUDIT C1 / P0-6: the
 * drip surfaces a real Discord artifact, not just a teaser line). The showrunner is a
 * short-lived cron with no gateway client, so it can't use discord.js's AttachmentBuilder
 * (poster.ts) — it uploads via the REST multipart endpoint directly, the same surface as
 * postToTheRecord. The card carries ONLY the forged runes + the seeded in-character framing
 * (no ad-hoc English — voice.ts stays the sole text source). Returns true on 2xx; never
 * throws (failure → false, logged by caller, which then falls back to the in-world report line).
 *
 * @param png      the rendered clue-card image bytes.
 * @param filename attachment name (e.g. `<puzzle_key>.png`).
 */
export async function postClueImageToTheRecord(png: Buffer, filename: string): Promise<boolean> {
  try {
    const form = new FormData();
    // discord requires the file part to be referenced from attachments[].id in the payload.
    form.append('payload_json', JSON.stringify({ attachments: [{ id: 0, filename }] }));
    const bytes = new Uint8Array(png);
    form.append('files[0]', new Blob([bytes], { type: 'image/png' }), filename);
    const res = await fetch(`${API}/channels/${config.channels.theRecord}/messages`, {
      method: 'POST',
      headers: {
        // NOTE: do NOT set Content-Type — fetch sets the multipart boundary itself.
        Authorization: `Bot ${config.discord.botToken}`,
      },
      body: form,
    });
    return res.ok;
  } catch {
    return false;
  }
}
