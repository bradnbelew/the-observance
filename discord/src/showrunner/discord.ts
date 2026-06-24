/**
 * The thin Discord REST seam for the showrunner cron. The bot is a long-running gateway client; the
 * showrunner is a short-lived cron, so it posts via the REST API directly (one fetch) rather than
 * spinning up a gateway connection. The Watcher's voice is still the SOLE text source (voice.ts) —
 * this only delivers it, keeping the cross-surface voice identical to the bot's.
 */
import { config } from '../config.js';

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
