/**
 * observer.llm.ts — Observer Tier-2: the LLM archivist that makes the weaponization SELECTION smarter (W4.2).
 *
 * Tier-1 (observer.ts) picks the longest substantial utterance — deterministic, grounded, but blunt.
 * Tier-2 asks the model to choose the MOST UNCANNY real quote to echo back, given the world's fiction.
 * It is a pure SELECTION: the model receives the real captured utterances and returns the id of ONE of
 * them (or null). It NEVER writes text — the echo is always the player's verbatim words. So the archivist
 * can make the scare land better, but it can never fabricate, because it only ever picks from what was
 * really said (INV-1: verbatim capture is the sole text source).
 *
 * DEGRADES TO THE DETERMINISTIC TIER-1 PICK, by construction — it never throws and returns null when:
 *   - ANTHROPIC_API_KEY is absent/blank (Tier-2 is simply not configured);
 *   - there is nothing to choose (0 or 1 candidate);
 *   - the call errors, times out, or the reply is malformed;
 *   - the model names an id that is NOT in the real candidate set (a fabricated pick is refused).
 * Cost is negligible: it fires only when an echo is already going to happen (at most once per ~12h echo
 * window, from observer.run.ts) and over a small candidate set. Best-judgment model for the taste call.
 */
import Anthropic from '@anthropic-ai/sdk';
import type { CapturedObservation } from './observer.js';

/** Best model for the "which real quote is most uncanny" taste judgment (cost is negligible at this rate). */
const MODEL = 'claude-opus-4-8';
/** A hung API call must degrade to the deterministic pick quickly — never wedge the once-per-tick cron. */
const TIMEOUT_MS = 20_000;

export interface ArchivistContext {
  /** the record's current in-world fiction in one spoiler-free line, to bias salience toward the world. */
  worldNote?: string;
}

/**
 * selectSalientObservationId — ask the archivist to pick the single most weaponizable real utterance.
 * Returns an id that is guaranteed present in `candidates`, or null (→ caller keeps the deterministic
 * Tier-1 pick). No side effects beyond the API read; never throws.
 */
export async function selectSalientObservationId(
  candidates: readonly CapturedObservation[],
  ctx: ArchivistContext = {},
): Promise<number | null> {
  const key = process.env.ANTHROPIC_API_KEY?.trim();
  if (!key) return null; // Tier-2 not configured → Tier-1 still works
  if (candidates.length <= 1) return null; // nothing to choose — let Tier-1 handle the trivial case
  const allowed = new Set(candidates.map((c) => c.id));

  try {
    const client = new Anthropic({ apiKey: key, timeout: TIMEOUT_MS, maxRetries: 1 });
    const list = candidates
      .map((c) => `- id ${c.id} — ${c.name}: ${JSON.stringify(c.text.trim().slice(0, 500))}`)
      .join('\n');
    const worldNote = ctx.worldNote?.trim()
      ? `the record's current fiction: ${ctx.worldNote.trim()}\n\n`
      : '';
    const system =
      'you are the archivist of a haunted-world minecraft arg called the observance. sparingly, the record ' +
      'echoes back — verbatim — one real thing the players said, so the world feels like it was listening. ' +
      'your only job is to choose WHICH captured utterance would be the most uncanny to surface: the one that, ' +
      'echoed back word-for-word, would most make a player feel heard and watched. prefer a specific plan, ' +
      'fear, promise, boast, or claim over small talk. you NEVER rewrite or invent text — you only pick an ' +
      'id from the list provided.';
    const prompt =
      `${worldNote}here are real captured utterances. pick the single most uncanny one to echo back ` +
      `verbatim. respond with the chosen id.\n\n${list}`;

    const resp = await client.messages.create({
      model: MODEL,
      max_tokens: 1024,
      system,
      messages: [{ role: 'user', content: prompt }],
      output_config: {
        format: {
          type: 'json_schema',
          schema: {
            type: 'object',
            properties: {
              chosen_id: { type: 'integer', description: 'the id of the most uncanny utterance to echo' },
              reason: { type: 'string', description: 'one short line on why (not shown to players)' },
            },
            required: ['chosen_id', 'reason'],
            additionalProperties: false,
          },
        },
      },
    });

    const textBlock = resp.content.find((b) => b.type === 'text');
    if (!textBlock || textBlock.type !== 'text') return null;
    const parsed = JSON.parse(textBlock.text) as { chosen_id?: unknown };
    const id = typeof parsed.chosen_id === 'number' ? parsed.chosen_id : null;
    if (id == null || !allowed.has(id)) return null; // fabricated / out-of-set pick → refuse, degrade
    return id;
  } catch {
    return null; // any failure → the deterministic Tier-1 pick still stands
  }
}
