/**
 * finale.ts — the M5 close composer (A1/A2 `the-seventh-spine` + `divergent-fates`, WEB-MASTER §5/§8).
 *
 * THE GAP THIS CLOSES. `decideFate` (fate.ts) computes the ending enum, `applyForks` (forks.ts) commits
 * the fork leaves, and `SeventhChoiceListener` sets `seventh_choice='restore'|'erase'` — and every one
 * of their payoff LINES was authored in voice.ts (`fate*`, `keeperCloseSeventh*`, `fork*`) with a
 * comment that "the M5 composer reads these". That composer was NEVER BUILT: a grep for those keys
 * across discord/src returns only voice.ts. So the ending fork never became player-facing. This is that
 * missing composer — the SOLE consumer that assembles the close and makes `seventh_choice` (and the
 * FREE-branch reckoning cost) reach the ending.
 *
 * ORDERED ASSEMBLY (WEB-MASTER §5). The close is a small ordered list of authored lines:
 *   (1) the base FATE opener (decideFate enum → fateKept/CastOut/Divided/Refusers);
 *   (2) the SEVENTH-CHOICE tinted clause (restore → keeperCloseSeventhRestored, erase → …Erased);
 *   (3) the FORK colorants (light kept/taken, name unspoken/spoken, sacred-beast broken);
 *   (4) the INHERITORS codicil if the +1 clause applies;
 *   (5) NEW: if reckoning_free is set, the Seventh names the price the group paid to let Wren go
 *       (the-companion.md §5/§7 — the FREE branch's cost is named by the Seventh at the reunion).
 *
 * REGISTER (the separation law). Every line here is WATCHER register from voice.ts and posts to
 * #the-record. Wren's own reveal/reckoning last-words are SET-A human speech (voice.archive.ts) and are
 * delivered in-world by companion.run.ts — they are NEVER folded into this close. This composer only
 * NAMES the cost of the free branch in the Watcher's own voice; it does not speak Wren's line.
 *
 * PRECISION (the reckoning is optional). The three reckoning flags are boolean + mutually exclusive by
 * the producer; "none set yet" is distinct from each. A missing reckoning flag composes the close with
 * NO reckoning-cost clause — the seventh_choice fork is the spine, the reckoning cost is optional colour.
 *
 * PURE + DETERMINISTIC. No DB / network / clock / LLM. finale.run.ts reads the flags once the Accepting
 * instant is reached and posts the composed lines set-once (state.finale_posted). finale.selftest lives
 * in autonomy.selftest.ts.
 */
import { voice } from '../voice.js';
import type { EndingFate } from './fate.js';

/** Everything the composer reads — the ending enum + the seventh choice + the fork leaves + reckoning. */
export interface FinaleComposeInput {
  /** the base fate enum (decideFate). */
  fate: EndingFate;
  /** the Seventh choice, or null if not made (spine fork). */
  seventhChoice: 'restore' | 'erase' | null;
  /** fork C leaves. */
  nameSpoken: boolean;
  nameUnspoken: boolean;
  /** fork B leaves. */
  lightKept: boolean;
  lightTaken: boolean;
  /** fork A transgressor leaf. */
  sacredBeastBroken: boolean;
  /** the +1 inheritors codicil applies (a deposit left for the next hand). */
  inheritorsCodicil: boolean;
  /** the companion FREE branch — releasing Wren cost the group; the Seventh names the price. */
  reckoningFree: boolean;
}

export interface FinaleComposeDecision {
  /** the ordered close lines to post to #the-record (Watcher register). Never empty (the fate opener). */
  lines: string[];
  reason: string;
}

/** The base fate opener for the enum (INV-11; each names the GROUP only). */
function fateOpener(fate: EndingFate): string {
  switch (fate) {
    case 'kept': return voice.fateKept();
    case 'cast_out': return voice.fateCastOut();
    case 'refusers': return voice.fateRefusers();
    case 'divided':
    default: return voice.fateDivided();
  }
}

/**
 * composeFinale — the pure M5 composer. Assembles the close in the fixed order above. Same input →
 * same lines. The fate opener is always present; every other clause is appended only when its flag is
 * set (colorants, never gates). The reckoning-cost clause is appended LAST and only for the FREE branch.
 */
export function composeFinale(inp: FinaleComposeInput): FinaleComposeDecision {
  const lines: string[] = [];

  // (1) the base fate opener.
  lines.push(fateOpener(inp.fate));

  // (2) the seventh-choice tinted clause (the spine fork; null = not made, no clause).
  if (inp.seventhChoice === 'restore') lines.push(voice.keeperCloseSeventhRestored());
  else if (inp.seventhChoice === 'erase') lines.push(voice.keeperCloseSeventhErased());

  // (3) the fork colorants (each one-way leaf, first-writer-wins upstream).
  if (inp.lightKept) lines.push(voice.forkLightKept());
  else if (inp.lightTaken) lines.push(voice.forkLightTaken());
  if (inp.nameUnspoken) lines.push(voice.forkNameUnspoken());
  else if (inp.nameSpoken) lines.push(voice.forkNameSpoken());
  if (inp.sacredBeastBroken) lines.push(voice.forkSacredBeastBroken());

  // (4) the inheritors codicil (+1 clause) if it applies.
  if (inp.inheritorsCodicil) lines.push(voice.fateInheritorsCodicil());

  // (5) NEW — the FREE-branch reckoning cost, named by the Seventh at the reunion (companion §5/§7).
  //     Only the FREE branch reaches the ending with a cost; condemn/understand do not (their weight is
  //     the sharp-quote shift + the in-world last words, not a finale price).
  if (inp.reckoningFree) lines.push(voice.seventhNamesFreedCompanionCost());

  return {
    lines,
    reason:
      `fate=${inp.fate}` +
      (inp.seventhChoice ? ` seventh=${inp.seventhChoice}` : '') +
      (inp.reckoningFree ? ' +free-cost' : ''),
  };
}

// ---------------------------------------------------------------------------
// THE RELEASE — the unified finale (design/FINALE-THE-RELEASE.md). This is the FINAL
// beat, AFTER the Accepting close above: the mask comes off (the Watcher was the
// Seventh all along), the group performs the release, and the world ends. This composer
// assembles the register-break farewell + the disconnect-screen kick line, from the SAME
// measured state the fate close reads. It EXTENDS composeFinale (does not replace it): the
// Accepting close still posts at the bow; this posts when the group performs the release.
// ---------------------------------------------------------------------------

/** Everything the release composer reads — the finale inputs + the two extra reckoning branches
 *  (condemn/understand, alongside the free branch already in FinaleComposeInput) + the restored name. */
export interface ReleaseComposeInput extends FinaleComposeInput {
  /** the group condemned Wren (cast him out for what he did — the Seventh notices the irony). */
  reckoningCondemn: boolean;
  /** the group held Wren whole (the truest read). */
  reckoningUnderstand: boolean;
  /** the Seventh's restored name, or null when no canon name is set (then it leaves as its title reclaimed). */
  seventhName: string | null;
}

export interface ReleaseComposeDecision {
  /** the ordered mask-off farewell lines to post to #the-record (Watcher register, broken warm). */
  lines: string[];
  /** the disconnect-screen sign-off (the kick reason the plugin shows every player). */
  kickLine: string;
  reason: string;
}

/** The fate → the TONE of the release (INV-11 group enum; never a player). */
function releaseFateClause(fate: EndingFate): string {
  switch (fate) {
    case 'kept': return voice.releaseFateKept();
    case 'cast_out': return voice.releaseFateCastOut();
    case 'refusers': return voice.releaseFateRefusers();
    case 'divided':
    default: return voice.releaseFateDivided();
  }
}

/**
 * composeRelease — the pure mask-off farewell + kick line. Same input → same output. The three
 * universal movements are always present (opener · what-they-made-me · the closing "i give them back");
 * the fate/reckoning/seventh/light clauses weave BETWEEN them as the character of the release. The kick
 * line is composed from the seventh choice (restored title/name vs the struck blank).
 */
export function composeRelease(inp: ReleaseComposeInput): ReleaseComposeDecision {
  const lines: string[] = [];

  // (1) + (2) universal: it hears its name; it says what it was made into.
  lines.push(voice.releaseOpener());
  lines.push(voice.releaseMade());

  // (3) the fate flavor — the tone.
  lines.push(releaseFateClause(inp.fate));

  // (4) the Wren reckoning mirror (optional; mutually exclusive by the producer — free > understand > condemn).
  if (inp.reckoningFree) lines.push(voice.releaseWrenFreed());
  else if (inp.reckoningUnderstand) lines.push(voice.releaseWrenUnderstood());
  else if (inp.reckoningCondemn) lines.push(voice.releaseWrenCondemned());

  // (5) the seventh choice — who leaves (named/reclaimed vs the struck blank).
  const restored = inp.seventhChoice === 'restore';
  if (restored) lines.push(voice.releaseSeventhRestored(inp.seventhName));
  else if (inp.seventhChoice === 'erase') lines.push(voice.releaseSeventhErased());

  // (6) the light-fork colorant.
  if (inp.lightKept) lines.push(voice.releaseLightKept());
  else if (inp.lightTaken) lines.push(voice.releaseLightTaken());

  // (7) the universal close — go, log off, i give your names back.
  lines.push(voice.releaseClosing());

  const kickLine = voice.releaseKickLine(restored ? inp.seventhName : null, restored);

  return {
    lines,
    kickLine,
    reason:
      `release fate=${inp.fate}` +
      (inp.seventhChoice ? ` seventh=${inp.seventhChoice}` : '') +
      (inp.reckoningFree ? ' +free' : inp.reckoningUnderstand ? ' +understand' : inp.reckoningCondemn ? ' +condemn' : ''),
  };
}
