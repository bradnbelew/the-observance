/**
 * receiver.ts — the voice tier of the Observer ("it heard you SAY it", W5).
 *
 * When enabled, the Watcher silently sits in a Discord voice channel, hears each speaker, transcribes what
 * was said, and stores it as a grounded observation (source='voice'). The existing Observer echo then, very
 * sparsely, quotes one such spoken line back in the "heard aloud" register — so the world reacts to a
 * spoken truth exactly as it reacts to a typed one, through the SAME weaponizer (no parallel scare path).
 *
 * BUILT TO NOT BREAK — every dependency is optional and every gate fails to silence:
 *   - OFF BY DEFAULT: the `voice_capture` setting (default false) is the master switch; also requires the
 *     DISCORD_VOICE_CHANNEL_ID env + a configured Whisper backend (transcribe.ts). Any absent → clean no-op.
 *   - OPTIONAL LIBS: @discordjs/voice + prism-media are dynamically imported; if they're not installed the
 *     tier logs one line and does nothing (they are optionalDependencies, so a failed install never breaks
 *     the deploy or the rest of the bot).
 *   - CONSENT: only LINKED players are captured, and voice is stricter than chat — a speaker is skipped
 *     unless positively confirmed NOT opted out (observerOptedOut), BEFORE any audio is transcribed.
 *   - FAULT-ISOLATED: every per-utterance capture is wrapped; a stumble is logged once and the bot rolls on.
 *
 * The Watcher joins self-MUTED (it never speaks in voice) and self-UNDEAF only because it must hear to
 * receive. Utterances are silence-delimited and hard-capped in length; too-short/too-long are dropped.
 */
import type { Client } from 'discord.js';
import { getPlayerByDiscordId, observerOptedOut, insertObservation, logEvent } from '../db/repo.js';
import { readSetting } from '../showrunner/state.js';
import { transcribeWav, transcriptionConfigured } from './transcribe.js';
import { maybeSolveSpokenName } from './spoken-name.js';

/** Discord voice receive is 48kHz stereo signed-16 PCM. */
const SAMPLE_RATE = 48_000;
const CHANNELS = 2;
/** Drop an utterance shorter than this many PCM bytes (~0.4s) — too short to be a real thing said. */
const MIN_PCM_BYTES = SAMPLE_RATE * CHANNELS * 2 * 0.4;
/** Hard cap on one utterance (~20s) — bounds memory + transcription cost; a monologue is truncated, not lost. */
const MAX_PCM_BYTES = SAMPLE_RATE * CHANNELS * 2 * 20;
/** How long a speaker must be silent before we treat the utterance as complete. */
const SILENCE_MS = 1_200;

const SOURCE = 'the-watcher.voice';
/** speakers currently being captured — de-dupes overlapping speaking-start events for one person. */
const capturing = new Set<string>();

function note(msg: string): void {
  console.log(`[the-watcher] voice: ${msg}`);
  void logEvent('info', SOURCE, msg);
}

/**
 * startVoiceCapture — bring the voice tier online if (and only if) it is fully configured + enabled.
 * Safe to call unconditionally from the bot's ready handler; returns quietly when the tier is off. Never
 * throws (a failure logs and leaves the rest of the bot untouched).
 */
export async function startVoiceCapture(client: Client): Promise<void> {
  try {
    const channelId = process.env.DISCORD_VOICE_CHANNEL_ID?.trim();
    if (!channelId) return note('no DISCORD_VOICE_CHANNEL_ID — voice tier off');
    if (!transcriptionConfigured()) return note('no Whisper backend configured — voice tier off');
    if ((await readSetting<boolean>('voice_capture', false)) !== true) {
      return note('voice_capture is off — voice tier idle (flip the setting + restart to enable)');
    }

    // dynamically load the optional voice libs; absent/failed → clean no-op.
    type DV = typeof import('@discordjs/voice');
    type Prism = typeof import('prism-media');
    let dv: DV;
    let prism: Prism;
    try {
      dv = await import('@discordjs/voice');
      prism = await import('prism-media');
    } catch {
      return note('voice libs unavailable (@discordjs/voice / prism-media) — voice tier off');
    }

    const channel = await client.channels.fetch(channelId).catch(() => null);
    if (!channel || !channel.isVoiceBased() || !('guild' in channel)) {
      return note('DISCORD_VOICE_CHANNEL_ID is not a reachable voice channel — voice tier off');
    }

    const connection = dv.joinVoiceChannel({
      channelId,
      guildId: channel.guild.id,
      adapterCreator: channel.guild.voiceAdapterCreator,
      selfDeaf: false, // must hear to receive
      selfMute: true, // the Watcher never speaks in voice
    });

    connection.on('error', (e: unknown) => {
      console.error('[the-watcher] voice connection error:', e);
    });

    const receiver = connection.receiver;
    receiver.speaking.on('start', (userId: string) => {
      void captureUtterance(receiver, userId, dv, prism).catch((e) => {
        console.error('[the-watcher] voice capture stumbled:', e);
      });
    });

    note(`voice tier online — listening in ${channelId}`);
  } catch (e) {
    // never let a voice-setup failure touch the rest of the bot
    console.error('[the-watcher] voice startup failed (isolated):', e);
  }
}

/**
 * captureUtterance — one speaker started talking: gate on consent, subscribe to their audio until they go
 * quiet, decode to PCM, transcribe, and store the transcript as a voice observation. Every failure path
 * ends in silence (no throw escapes; the caller also guards).
 */
async function captureUtterance(
  receiver: import('@discordjs/voice').VoiceReceiver,
  userId: string,
  dv: typeof import('@discordjs/voice'),
  prism: typeof import('prism-media'),
): Promise<void> {
  if (capturing.has(userId)) return; // already capturing this speaker's current utterance
  capturing.add(userId);
  try {
    // consent + link gate, BEFORE any audio is transcribed.
    const player = await getPlayerByDiscordId(userId);
    if (!player || typeof player.mc_uuid !== 'string' || player.mc_uuid.trim() === '') return; // unlinked
    if (await observerOptedOut(player.mc_uuid)) return; // opted out (or unconfirmed) → never captured

    const opus = receiver.subscribe(userId, {
      end: { behavior: dv.EndBehaviorType.AfterSilence, duration: SILENCE_MS },
    });
    const decoder = new prism.opus.Decoder({ rate: SAMPLE_RATE, channels: CHANNELS, frameSize: 960 });

    const pcm = await new Promise<Buffer>((resolve) => {
      const chunks: Buffer[] = [];
      let total = 0;
      let done = false;
      const finish = (): void => {
        if (done) return;
        done = true;
        try { opus.destroy(); } catch { /* ignore */ }
        try { decoder.destroy(); } catch { /* ignore */ }
        resolve(Buffer.concat(chunks));
      };
      decoder.on('data', (c: Buffer) => {
        if (done) return;
        chunks.push(c);
        total += c.length;
        if (total >= MAX_PCM_BYTES) finish(); // hard cap → stop collecting
      });
      decoder.on('end', finish);
      decoder.on('error', finish);
      opus.on('error', finish);
      opus.pipe(decoder);
    });

    if (pcm.length < MIN_PCM_BYTES) return; // too short to be a real utterance
    const wav = pcmToWav(pcm.length > MAX_PCM_BYTES ? pcm.subarray(0, MAX_PCM_BYTES) : pcm);
    const text = await transcribeWav(wav);
    if (!text) return; // no backend / failed / empty → silence
    const t = text.trim();
    if (t.length < 4 || t.length > 512) return; // mirror the chat-capture bounds

    await insertObservation({ mc_uuid: player.mc_uuid, source: 'voice', text: t, context: 'voice' });

    // Cohesion (§8.3): a spoken truth also SOLVES spine-spoken-name — the world answers what it heard.
    // Gated + idempotent inside (fires only post-iss_caught, once); a no-op on non-match. Never throws.
    await maybeSolveSpokenName(player, t);
  } finally {
    capturing.delete(userId);
  }
}

/** Wrap raw PCM (s16le, 48kHz stereo) in a minimal 44-byte WAV header for the transcriber. */
function pcmToWav(pcm: Buffer): Buffer {
  const byteRate = SAMPLE_RATE * CHANNELS * 2;
  const blockAlign = CHANNELS * 2;
  const header = Buffer.alloc(44);
  header.write('RIFF', 0);
  header.writeUInt32LE(36 + pcm.length, 4);
  header.write('WAVE', 8);
  header.write('fmt ', 12);
  header.writeUInt32LE(16, 16); // PCM fmt chunk size
  header.writeUInt16LE(1, 20); // audio format = PCM
  header.writeUInt16LE(CHANNELS, 22);
  header.writeUInt32LE(SAMPLE_RATE, 24);
  header.writeUInt32LE(byteRate, 28);
  header.writeUInt16LE(blockAlign, 32);
  header.writeUInt16LE(16, 34); // bits per sample
  header.write('data', 36);
  header.writeUInt32LE(pcm.length, 40);
  return Buffer.concat([header, pcm]);
}
