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
