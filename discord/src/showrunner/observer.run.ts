/**
 * observer.run.ts — the I/O wrapper for the Observer Tier-1 weaponizer ("it heard you say it", W4).
 *
 * Sparingly, the record echoes ONE grounded, verbatim utterance the group actually said. This closes the
 * words tier of the Observer (Tier-0 behavior is reports.run.ts). It reads the eligible captures, runs the
 * pure precision-gated policy (observer.ts), posts the one chosen echo to #the-record, marks it used, and
 * advances the sparse-rate high-water. No LLM: it QUOTES what was literally captured (never fabricates).
 *
 * CONSENT (both gates enforced before a word is ever surfaced):
 *   - gate 1 (global): settings.observer_capture must be true — the operator's master switch. Off (the
 *     default) → this whole pass is a no-op, so nothing is captured OR echoed until it is deliberately on.
 *   - gate 2 (per-player): readUnweaponizedObservations filters players.observer_opt_out — an opted-out
 *     person is never eligible, so their words are never echoed even if captured before they opted out.
 * Grounded · sparse · degrade to silence: a failed/empty pass simply says nothing.
 */
import { readSetting, readState, writeState } from './state.js';
import { readUnweaponizedObservations, markObservationWeaponized, logEvent, getArcFlags } from '../db/repo.js';
import { postToTheRecord } from './discord.js';
import { decideWeaponization, MIN_QUOTE_LEN, type CapturedObservation } from './observer.js';
import { selectSalientObservationId } from './observer.llm.js';
import { voice } from '../voice.js';

/** How rarely the record echoes a captured word — sparse by design (at most once per ~12h window). */
const OBSERVER_MIN_INTERVAL_MS = 12 * 60 * 60 * 1000;

/**
 * runObserverPass — one Tier-1 tick. No-op unless the global switch is on. Reads eligible (un-used,
 * consented, named) captures, picks at most one to echo (rate-limited + substance-gated), posts it, marks
 * it used (set-once), and advances the high-water. Returns a small tally for the tick log.
 */
export async function runObserverPass(): Promise<{ echoed: boolean }> {
  const enabled = await readSetting<boolean>('observer_capture', false);
  if (enabled !== true) return { echoed: false }; // gate 1: master switch off → the whole tier is silent

  const nowMs = Date.now();
  const state = await readState();

  // POST-RECKONING (OVERHAUL Pillar 5 — the sharp-quote consequence must be FELT). The group's reckoning
  // changes the "it heard you" channel:
  //   - condemn / free → the channel is GONE. Announce the quiet ONCE, then cease echoing forever.
  //   - understand     → the echoes persist, but in the SORROW register (handled at the render step below).
  const flags = await getArcFlags();
  const condemned = flags.reckoning_condemn === true;
  const freed = flags.reckoning_free === true;
  const understood = flags.reckoning_understand === true;
  if (condemned || freed) {
    if (state.observer_silenced !== true) {
      const ok = await postToTheRecord(voice.observerChannelGone());
      if (ok) {
        state.observer_silenced = true;
        await writeState(state, new Date(nowMs).toISOString());
        await logEvent('info', 'showrunner.observer', 'the channel closed (reckoning) — echoes cease');
      }
    }
    return { echoed: false }; // the channel is gone — no more captured words are echoed
  }

  const eligible: CapturedObservation[] = (await readUnweaponizedObservations()).map((o) => ({
    id: o.id, name: o.name, text: o.text, source: o.source, observedAtMs: o.observedAtMs,
  }));
  const decision = decideWeaponization(eligible, nowMs, state.observer_last_ms ?? null, OBSERVER_MIN_INTERVAL_MS);
  if (!decision.observation) return { echoed: false }; // too soon / nothing substantial → silence

  // Tier-2 (W4.2): the LLM archivist may pick a MORE uncanny real utterance from the same substantial,
  // grounded candidate set. Only runs now that an echo is already going to happen (≤ once/12h → cost-minimal).
  // It returns null (→ keep the deterministic Tier-1 pick) on no key / error / timeout / out-of-set pick, and
  // only ever returns an id already in the set — so the echo stays the player's verbatim words, never composed.
  let o = decision.observation;
  const substantial = eligible.filter((e) => e.text.trim().length >= MIN_QUOTE_LEN);
  const salientId = await selectSalientObservationId(substantial);
  if (salientId != null) {
    const picked = substantial.find((e) => e.id === salientId);
    if (picked) o = picked;
  }
  const nowIso = new Date(nowMs).toISOString();
  // Render register: post-reckoning UNDERSTAND → sorrow; a voice capture → "heard aloud"; else the read echo.
  const line = understood
    ? voice.observerHeardSorrow(o.name, o.text)
    : o.source === 'voice'
      ? voice.observerHeardAloud(o.name, o.text)
      : voice.observerHeard(o.name, o.text);
  const ok = await postToTheRecord(line);
  if (!ok) return { echoed: false }; // failed post → leave it un-used + the high-water untouched to retry

  await markObservationWeaponized(o.id, nowIso);
  state.observer_last_ms = nowMs;
  await writeState(state, nowIso);
  await logEvent('info', 'showrunner.observer', `heard: echoed one utterance from ${o.name} (obs ${o.id})`);
  return { echoed: true };
}
