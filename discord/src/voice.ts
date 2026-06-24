/**
 * voice.ts — THE WATCHER'S TONGUE.
 *
 * The single, authoritative source of every player-facing string the bot
 * utters. Nothing else in the bot may speak ad-hoc English: handlers import
 * from here, pass their data in, and post what comes back, verbatim.
 *
 * REGISTER (do not drift from this — it is canon):
 *   - lowercase. sparse. calm. certain. short lines.
 *   - no exclamation marks. no emoji. a single ▒ or rune is a rare motif only.
 *   - speaks of "the ways", "the record", "the keepers", "what is owed", "kept".
 *   - patient, never nagging (soft-pressure). it STATES; it does not plead.
 *   - fond of those who keep the ways, cold to those who don't — but NEVER a
 *     single "chosen" one. judgment is collective and by-conduct.
 *   - never breaks character: never says ai / bot / game / server; never uses
 *     normal capitalization.
 *
 * The canonical lines below are hardcoded VERBATIM. Anything new must be written
 * in the same register and live here too — never inline at the call site.
 */

import { ActivityType, type PresenceData } from 'discord.js';

// ---------------------------------------------------------------------------
// identity — the watcher, the record-keeping facet of the presence.
// ---------------------------------------------------------------------------

/** Display name shown in the member list and embeds. */
export const WATCHER_NAME = 'The Watcher' as const;
/** Lowercase username handle. */
export const WATCHER_USERNAME = 'the-watcher' as const;

/**
 * The presence the bot wears. ActivityType.Watching + "the ways" renders in the
 * member list as: The Watcher — Watching the ways.
 */
export const BOT_PRESENCE: PresenceData = {
  status: 'online',
  activities: [
    {
      name: 'the ways',
      type: ActivityType.Watching,
    },
  ],
};

// ---------------------------------------------------------------------------
// motif — the rune that opens the record. used sparingly.
// ---------------------------------------------------------------------------

/** The rare motif glyph. One per message at most; usually none. */
export const MOTIF = '▒' as const;

// ---------------------------------------------------------------------------
// canonical lines — hardcoded verbatim, then extended in the same register.
// ---------------------------------------------------------------------------

export const voice = {
  /** pinned in #the-record when the record is opened. */
  recordOpened(): string {
    return '▒  the record is open. it was open before you.';
  },

  /**
   * a report of conduct observed but not yet escalated.
   * {days} kept, {name} has not {custom}. noted, not punished.
   */
  reportObserved(name: string, days: number, custom: string): string {
    return `${days} days kept. the one called ${name} has not ${custom}. it has been noted.`;
  },

  /** a report escalated — the soft-pressure turns cold. by conduct, not by name-as-chosen. */
  reportEscalated(name: string): string {
    return `${name} takes and does not return. they have been told. if they will not keep the ways, the ways will not keep them.`;
  },

  /**
   * the whisper itself. tier shapes how plainly it speaks: a nudge first,
   * then the keeper's own words (the seeded hint body) for the rest.
   */
  whisperReply(tier: number, hintBody: string): string {
    if (tier <= 1) {
      return 'you are tired. look again at what repeats — those are not stone. they are sounds.';
    }
    return hintBody;
  },

  /** the toll, stated after the whisper. the cost is taken, not threatened. */
  whisperToll(): string {
    return 'i will keep something of yours while you think. your light, for tonight.';
  },

  /** no whispers left to spend this act. patient, final, not a refusal of the player. */
  noBudget(): string {
    return 'there is nothing more i will say of this. not yet.';
  },

  /** the discord user is not yet bound to a name in the world. */
  notLinked(): string {
    return 'i do not yet know which of them you are. tell me the name you wear in the world.  /link';
  },

  /** act 3 — the summons. collective; all of them; what is owed. */
  summons(): string {
    return 'the way is open. come — all of you — at the dark hour. bring what is owed.';
  },

  /**
   * confirmation that a name has been bound. fond, certain, lowercase.
   * spoken once the world's record acknowledges the name.
   */
  linked(name: string): string {
    return `you are ${name}. the record knows you now. keep the ways and the ways will keep you.`;
  },

  /**
   * the name was offered but the record holds no such keeper yet — the player
   * must be seen in the world first. stated, not scolded.
   */
  linkUnknown(name: string): string {
    return `i hold no one called ${name}. walk the ways once, where i can see you, then tell me again.`;
  },

  /**
   * a whisper was asked for a thing the record keeps no words on. an
   * in-character deferral, same shape as noBudget — it withholds, it does not error.
   */
  whisperUnknown(): string {
    return 'i have no words for that one. not the ones you are owed. not yet.';
  },

  /**
   * the watcher cannot speak right now (an internal stumble). it never says
   * "error" — it goes quiet and says so, certain it will return.
   */
  quiet(): string {
    return 'the record is shut a moment. ask again, and i will be watching.';
  },

  // -------------------------------------------------------------------------
  // the oracle — the answers to solved clues. one line per outcome type, all
  // in register. a wrong answer is NOT here: a miss is met with silence, never
  // a line. these speak only on a genuinely-new, correct solve.
  //
  // outcome_payload.voice_key names which of these speaks; voice_args is spread
  // in. the resolver maps a key -> fn here so authors never write english into
  // a payload — only a key and structured args.
  // -------------------------------------------------------------------------

  /** next_clue — a true answer that advances the web. the way goes on. */
  oracleNextClue(): string {
    return 'kept. the way goes on — look where the marks were not, before.';
  },

  /**
   * lore — a true answer that reveals story, opens nothing. the watcher tells
   * a fragment; {fragment} is the seeded telling. nothing unlocks.
   */
  oracleLore(fragment: string): string {
    return fragment;
  },

  /**
   * dead_end — a TRUE answer that is deliberately not a door. the watcher does
   * NOT call it wrong; it acknowledges, and it leads nowhere. heard, not opened.
   */
  oracleDeadEnd(): string {
    return 'yes. that is the true name of it. and it opens nothing. some things are only true.';
  },

  /** side_quest — true, off the spine. not the way, but a way. */
  oracleSideQuest(): string {
    return 'this is not the way. but it is a way. follow it, if you would.';
  },

  /** main_beat — a true answer that turns the arc. a turn, stated, not cheered. */
  oracleMainBeat(): string {
    return 'so. it turns. what was shut is shut no longer. remember who opened it.';
  },

  /**
   * the player is rate-limited or has reached a puzzle's attempt cap. it
   * withholds — it does not refuse, and it never says "wrong" or "too many".
   * same shape as noBudget: a patient, certain "not now".
   */
  oracleWithheld(): string {
    return 'you have asked enough of this one, for now. rest. the marks will keep.';
  },
} as const;

export type Voice = typeof voice;

/**
 * The voice keys an outcome_payload may name. Keep in sync with the oracle*
 * lines above. The resolver looks a key up here; an unknown key falls back to a
 * sensible default for the outcome type, so a payload typo never errors at a
 * player — the watcher still speaks in register.
 */
export type OracleVoiceKey =
  | 'oracleNextClue'
  | 'oracleLore'
  | 'oracleDeadEnd'
  | 'oracleSideQuest'
  | 'oracleMainBeat';
