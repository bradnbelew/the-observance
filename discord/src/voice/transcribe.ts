/**
 * transcribe.ts — the pluggable Whisper backend for the voice tier ("it heard you", W5).
 *
 * Turns a WAV buffer (one captured utterance) into text, or null. Two interchangeable backends, chosen by
 * env; if neither is configured, voice capture is simply OFF (returns null → the whole tier no-ops):
 *
 *   1) HTTP (recommended) — an OpenAI/Groq-compatible /audio/transcriptions endpoint:
 *        WHISPER_API_URL   e.g. https://api.openai.com/v1/audio/transcriptions
 *        WHISPER_API_KEY   bearer token
 *        WHISPER_MODEL     default "whisper-1"
 *   2) LOCAL — a whisper binary/wrapper that reads a WAV path as its LAST arg and prints the transcript to
 *      stdout:  WHISPER_BIN  e.g. /usr/local/bin/whisper-transcribe   (any wrapper honoring that contract)
 *
 * Grounded + safe by construction: this only ever returns what the transcriber produced (never invents),
 * and NEVER throws — any failure (missing config, network, bad audio, timeout, non-zero exit) returns null,
 * so the voice tier degrades to silence. Language defaults to English (WHISPER_LANG).
 */
import { spawn } from 'node:child_process';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

/** Hard ceiling on any single transcription — a hung backend must degrade to silence, never hang the bot. */
const TRANSCRIBE_TIMEOUT_MS = 30_000;

/** True iff at least one transcription backend is configured (else the voice tier is a no-op). */
export function transcriptionConfigured(): boolean {
  return Boolean(process.env.WHISPER_API_URL?.trim() || process.env.WHISPER_BIN?.trim());
}

/** Transcribe one utterance WAV to text, or null on any failure/absence. Never throws. */
export async function transcribeWav(wav: Buffer): Promise<string | null> {
  try {
    if (process.env.WHISPER_API_URL?.trim()) return await transcribeHttp(wav);
    if (process.env.WHISPER_BIN?.trim()) return await transcribeLocal(wav);
    return null; // no backend configured
  } catch {
    return null; // any failure → silence
  }
}

async function transcribeHttp(wav: Buffer): Promise<string | null> {
  const url = process.env.WHISPER_API_URL!.trim();
  const key = process.env.WHISPER_API_KEY?.trim();
  const model = process.env.WHISPER_MODEL?.trim() || 'whisper-1';
  const lang = process.env.WHISPER_LANG?.trim() || 'en';

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TRANSCRIBE_TIMEOUT_MS);
  try {
    const form = new FormData();
    form.append('file', new Blob([new Uint8Array(wav)], { type: 'audio/wav' }), 'utterance.wav');
    form.append('model', model);
    form.append('language', lang);
    form.append('response_format', 'json');
    const resp = await fetch(url, {
      method: 'POST',
      headers: key ? { authorization: `Bearer ${key}` } : {},
      body: form,
      signal: controller.signal,
    });
    if (!resp.ok) return null;
    const data = (await resp.json()) as { text?: unknown };
    const text = typeof data.text === 'string' ? data.text.trim() : '';
    return text.length > 0 ? text : null;
  } finally {
    clearTimeout(timer);
  }
}

async function transcribeLocal(wav: Buffer): Promise<string | null> {
  const bin = process.env.WHISPER_BIN!.trim();
  const dir = await mkdtemp(join(tmpdir(), 'obs-voice-'));
  const wavPath = join(dir, 'utterance.wav');
  try {
    await writeFile(wavPath, wav);
    const out = await runBinary(bin, [wavPath]);
    const text = out.trim();
    return text.length > 0 ? text : null;
  } finally {
    await rm(dir, { recursive: true, force: true }).catch(() => {});
  }
}

/** Spawn a command with args, resolve its stdout, reject/timeout → throws (caught upstream → null). */
function runBinary(bin: string, args: string[]): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const child = spawn(bin, args, { stdio: ['ignore', 'pipe', 'ignore'] });
    let stdout = '';
    const timer = setTimeout(() => {
      child.kill('SIGKILL');
      reject(new Error('transcription timed out'));
    }, TRANSCRIBE_TIMEOUT_MS);
    child.stdout.on('data', (d: Buffer) => {
      stdout += d.toString('utf8');
    });
    child.on('error', (e) => {
      clearTimeout(timer);
      reject(e);
    });
    child.on('close', (code) => {
      clearTimeout(timer);
      if (code === 0) resolve(stdout);
      else reject(new Error(`transcriber exited ${code}`));
    });
  });
}
