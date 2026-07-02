/**
 * companion.ts — the Wren (trusted-companion) dialogue resolver (D3 `the-companion`, the-companion.md).
 *
 * THE GAP THIS CLOSES. The PLUGIN already PRODUCES the companion arc: `WrenNpcListener.advanceTrust`
 * logs `event_log(type='companion', context='npc.open', detail={surface, trust})`; `CompanionArcWatcher`
 * flips `companion_revealed` once `iss_caught` + `companion_artifact_found`; `handleReckoning` sets
 * exactly ONE of `reckoning_condemn|understand|free` (post-reveal, one-of-three) and logs the matching
 * `event_log(context='reckoning.*')`. NOTHING in the showrunner CONSUMED those rows — Wren's authored
 * last-words (voice.archive.ts `wren.*`) never reached a player. This is that missing consumer: a pure
 * resolver that maps the arc flags + trust + the companion event_log context to the `wren.*` voiceKey
 * the run wrapper delivers in-world.
 *
 * DISTINCT MODULE (the naming-collision guard). `companion.ts` (this file) is NOT `reckoning.ts` — that
 * is the DIFFICULTY grip engine (tight/even/loose), utterly unrelated to Wren's reckoning arc flags.
 * The consumer is a NEW module, never an edit to reckoning.ts, and its high-waters
 * (`companion_lines_delivered`, `companion_reckoning_delivered`) do not reuse any `reckoning_*` name.
 *
 * SEPARATION LAW (the register wall). Every key this resolves is a `wren.*` key that lives in
 * voice.archive.ts `npcLines` — SET-A HUMAN speech (warm, present-tense, hedged, lowercase), resolved
 * through `npcLine()`, NEVER `archiveLine()`. It is delivered in-world (KeeperNpcBeat / private_message),
 * never posted as the Watcher's close. The forge register-discipline guard (canon.ts GUARD 10) scans
 * only the `archive` map, so these intentionally human-register lines are correctly exempt — provided
 * they stay in `npcLines`.
 *
 * PURE. No DB / network / clock / LLM. companion.run.ts reads the companion `event_log` rows + the
 * arc_state flags + the delivery high-waters, resolves the node here, and enqueues the in-world beat
 * with `step_payload.key = node.voiceKey`. companion.selftest lives in autonomy.selftest.ts.
 */

/** The companion event_log `context` the run wrapper read (or null when no companion row exists yet). */
export type CompanionContext =
  | 'npc.open' // WrenNpcListener.advanceTrust — a trust interaction (the trust ladder)
  | 'reckoning.condemn'
  | 'reckoning.understand'
  | 'reckoning.free'
  | null;

/** The arc flags this resolver branches on (read from arc_state.flags by the run wrapper). */
export interface CompanionArcFlags {
  /** M4 gate: the reveal has landed (CompanionArcWatcher flipped it: iss_caught + artifact_found). */
  companionRevealed: boolean;
  /** M5 one-of-three (mutually exclusive, set-once by the plugin). At most one is ever true. */
  reckoningCondemn: boolean;
  reckoningUnderstand: boolean;
  reckoningFree: boolean;
}

export interface CompanionDialogueInput {
  flags: CompanionArcFlags;
  /** the measured group trust level (WrenNpcListener detail.trust), or 0 when none is read. */
  trust: number;
  /** the current arc movement (1..5) — M3 unlocks the hairline-crack lines. */
  movement: number;
  /** the latest companion event_log context the run wrapper read (drives the reckoning branch). */
  context: CompanionContext;
  /** a late joiner this window (dynamic-roster: gets the newhand line, quorum-free). */
  lateJoiner: boolean;
  /** the reveal.yes/reveal.tally lines already delivered (idempotency; one-shot each). */
  revealDelivered: boolean;
  /** the single reckoning last-words node already delivered (idempotency; one-of-three, set-once). */
  reckoningDelivered: boolean;
}

/** The resolved companion node — a `wren.*` voiceKey + delivery metadata. */
export interface CompanionDialogueNode {
  /** the authored SET-A voice key (npcLines; resolved via npcLine(), never archiveLine()). */
  voiceKey: string;
  /** true for the ONE reckoning last-words node (condemn/understand/free) — set-once via the high-water. */
  isReckoning: boolean;
  /** true for the reveal.yes/reveal.tally pair (also one-shot). */
  isReveal: boolean;
  reason: string;
}

export interface CompanionDialogueDecision {
  node: CompanionDialogueNode | null;
  notes: string[];
}

/** The trust rungs (below the first rung there is no meet line yet). Mirrors the plugin's thresholds. */
const TRUST_MEET = 1;
const TRUST_WARN = 2;
const TRUST_GIFT = 3;
const TRUST_ASK = 4;

/**
 * resolveCompanionDialogue — the pure companion resolver. Fixed precedence (the arc runs forward, so
 * the LATEST movement/flag wins):
 *   1. RECKONING (M5): a reckoning flag is set AND the context matches AND it has not yet been
 *      delivered → the matching `wren.reckoning.*` last-words node (one-of-three, set-once).
 *   2. REVEAL (M4): companion_revealed AND not yet delivered → `wren.reveal.yes` then `wren.reveal.tally`.
 *   3. LATE JOINER: a new hand this window → `wren.roster.newhand` (quorum-free).
 *   4. M3 crack: movement >= 3 → `wren.crack.slow` / `wren.crack.notice` (the hairline).
 *   5. TRUST I–II: the trust ladder → `wren.trust.meet/warn/gift/ask`; below the floor → `wren.trust.absent`.
 * Same input → same node. A reckoning/reveal already delivered yields null (nothing to re-say).
 */
export function resolveCompanionDialogue(inp: CompanionDialogueInput): CompanionDialogueDecision {
  const notes: string[] = [];
  const f = inp.flags;

  // 1. M5 RECKONING — the group has entered his last words into the record (one-of-three, set-once).
  //    The plugin sets exactly one flag AND logs the matching context; we require BOTH so a stale flag
  //    without its context does not fire, and re-fire is guarded by the delivery high-water.
  const reckoningKey =
    f.reckoningCondemn && inp.context === 'reckoning.condemn' ? 'wren.reckoning.condemn'
    : f.reckoningUnderstand && inp.context === 'reckoning.understand' ? 'wren.reckoning.understand'
    : f.reckoningFree && inp.context === 'reckoning.free' ? 'wren.reckoning.free'
    : null;
  if (reckoningKey) {
    if (inp.reckoningDelivered) {
      notes.push('reckoning last-words already delivered — set-once, nothing to re-say');
      return { node: null, notes };
    }
    return {
      node: {
        voiceKey: reckoningKey,
        isReckoning: true,
        isReveal: false,
        reason: `M5 reckoning (${inp.context}) — his last words, entered once`,
      },
      notes,
    };
  }

  // 2. M4 REVEAL — companion_revealed && !reckoned. Deliver the reveal pair once. reveal.tally is the
  //    found-artifact companion beat; reveal.yes is the spoken admission. Both one-shot (high-water).
  if (f.companionRevealed) {
    if (inp.revealDelivered) {
      notes.push('reveal lines already delivered — awaiting the reckoning');
      return { node: null, notes };
    }
    return {
      node: {
        voiceKey: 'wren.reveal.yes',
        isReckoning: false,
        isReveal: true,
        reason: 'companion_revealed && !reckoned — the admission (reveal.yes; reveal.tally follows on the artifact)',
      },
      notes,
    };
  }

  // 3. LATE JOINER — a new hand gets a Wren line, quorum-free (dynamic-roster invariant, §7).
  if (inp.lateJoiner) {
    return {
      node: {
        voiceKey: 'wren.roster.newhand',
        isReckoning: false,
        isReveal: false,
        reason: 'a late joiner this window — the newhand line (quorum-free)',
      },
      notes,
    };
  }

  // 4. M3 — the first hairline crack (every warning is the cautious direction; the scares track him).
  if (inp.movement >= 3) {
    const key = inp.trust >= TRUST_WARN ? 'wren.crack.notice' : 'wren.crack.slow';
    return {
      node: {
        voiceKey: key,
        isReckoning: false,
        isReveal: false,
        reason: `M3 crack (trust=${inp.trust}) — ${key}`,
      },
      notes,
    };
  }

  // 5. TRUST I–II — the warm ladder. Below the meet floor he is "just behind you" (absent).
  const trustKey =
    inp.trust >= TRUST_ASK ? 'wren.trust.ask'
    : inp.trust >= TRUST_GIFT ? 'wren.trust.gift'
    : inp.trust >= TRUST_WARN ? 'wren.trust.warn'
    : inp.trust >= TRUST_MEET ? 'wren.trust.meet'
    : 'wren.trust.absent';
  return {
    node: {
      voiceKey: trustKey,
      isReckoning: false,
      isReveal: false,
      reason: `trust ladder (trust=${inp.trust}) — ${trustKey}`,
    },
    notes,
  };
}
