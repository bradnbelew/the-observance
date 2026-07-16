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
// register temperature — the difficulty engine's selection among authored variants.
// ---------------------------------------------------------------------------

/**
 * The register temperature a line may be spoken in (A10 `dynamic-diegetic-difficulty`, FACT 2b).
 * This is a SELECTION among AUTHORED variants, never generated text: the land's grip cools the
 * Watcher's register when the group races (`cold`) and lets it sit plain when they stumble. It is
 * the SAME enum as `showrunner/types.ts` `Tone`, re-declared here so voice.ts imports nothing from
 * the showrunner (it is the leaf module). Absent ⇒ `'plain'` — the neutral the engine emits when no
 * grip is read, so every existing call site that omits it is byte-for-byte unchanged.
 */
export type Tone = 'cold' | 'plain' | 'warm';

/**
 * The kinds of dead-end taunt the `kind`-switched family speaks (B2 `dead-ends-with-teeth`,
 * WEB-MASTER §2.1). Each names the CATEGORY of truth a true-but-inert answer belongs to. The
 * seeded `voice_args.kind` values are exactly these strings; `oracleDeadEnd(kind)` switches on them
 * and an omitted/unknown kind falls back to the generic line (so old rows never break).
 */
export type DeadEndKind = 'name' | 'count' | 'place' | 'known' | 'prophet';

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

  // -------------------------------------------------------------------------
  // THE COLD-START PROLOGUE / IGNITION (B4 `cold-start-prologue`, WEB-MASTER §1.M1). The first
  // report is re-staged as a discoverable anomaly. It names a real MEASURED habit IFF one
  // player's signal is overwhelming (`prologueNamed`), else the un-named FACT-1 fallback
  // (`prologueUnnamed`) — precision: never a guessed callout. De-slopped, flat.
  // -------------------------------------------------------------------------

  /** the first report, naming a real measured habit — only on an overwhelming single signal. {custom} completes "has not {custom}". */
  prologueNamed(name: string, custom: string): string {
    return `▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet read. it knows the one called ${name} has not ${custom}. it was noted before you knew there was a record to note it.`;
  },

  /** the un-named first report — the FACT-1 fallback when no single signal dominates (no callout). */
  prologueUnnamed(): string {
    return '▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet read. it has been keeping a count of you. the count began before you found the mark.';
  },

  /**
   * recordOpenedNamed — the named first-naming inflection the prologue decider selects when the
   * precision gate passes (`decidePrologue` returns `reportVoiceKey: 'recordOpenedNamed'` on an
   * OVERWHELMING single signal + a name). Modeled on {@link reportObserved} (cold-start-prologue §3):
   * it states a count and a fact and stops — the first naming, in the oldest keeper register. Never a
   * guess: the un-named {@link recordOpened} fallback is emitted whenever the signal is not overwhelming.
   * {days} kept; {name} the resolved keeper; {custom} completes "has not {custom}" (a customPhrase()).
   */
  recordOpenedNamed(name: string, days: number, custom: string): string {
    return `▒  ${days} days kept. the one called ${name} has not ${custom}. it has been noted.`;
  },

  // -------------------------------------------------------------------------
  // THE OFFLINE-SKIN APPARITION (B3 `offline-skin-apparition`, FACT 9). The M1 plant report
  // (a logged-off friend, noted not-here) and the M4 named line (the land has begun to wear
  // the one who stopped coming). The M4 line is the ONE human-approved named beat. De-slopped:
  // it states what is, names no feeling. {name} = the offline player, canonically rhymed.
  // -------------------------------------------------------------------------

  /** M1 plant — a friend logged off is noted absent, not-here-to-see-it (reads as "the record watches you leave"). */
  offlineReportPlant(name: string): string {
    return `the one called ${name} is not here to see it noted. the record notes the not-here as plainly as the here. it keeps the empty place at the table.`;
  },

  /** M4 — the land has begun to wear the one who stopped coming (FACT 9 spoken; the one named beat). */
  offlineSkinNamed(name: string): string {
    return `the shape in the dark wears the one called ${name}, who has not come down since the night the record stopped entering them as present. it began to wear them from that night. it is not them. it is the place they left.`;
  },

  /**
   * a clue has surfaced (the showrunner's drip). cryptic — never names the puzzle
   * or the answer, only that there is something new set out to be read.
   *
   * `tone` (A10) selects among AUTHORED variants — it never generates text and never changes
   * WHICH node dripped. Optional + defaulting to `'plain'` so every existing caller (`apply.ts`,
   * `clue-drip.ts`) is unchanged; the cold/warm forms only differ in register, never in fact (each
   * still says "a thing is set out; read it"). The cold form is shorter and barer (the land's grip
   * tightens, says less); the warm form is the same offer, a half-beat less terse.
   */
  drip(tone: Tone = 'plain'): string {
    if (tone === 'cold') return '▒  a thing is set out where the marks are kept. read it.';
    if (tone === 'warm') return '▒  something is set out where the marks are kept. read it, if you can. it will keep.';
    return '▒  something is set out where the marks are kept. read it, if you can.';
  },

  /**
   * a report of conduct observed but not yet escalated.
   * {days} kept, {name} has not {custom}. noted, not punished.
   *
   * `tone` (A10) is an OPTIONAL trailing SELECTION among authored closers — it never changes the
   * grounded facts ({days}/{name}/{custom}) and never changes whether the rung fired. It defaults to
   * `'plain'`, which returns the line byte-for-byte as before, so `customs.ts`, `reports.ts`, and the
   * customs self-test (which pin the plain prose) are unchanged. The cold closer states the count and
   * stops (the land's grip tightened — it says less); the warm closer is the same note, unhurried.
   */
  reportObserved(name: string, days: number, custom: string, tone: Tone = 'plain'): string {
    const head = `${days} days kept. the one called ${name} has not ${custom}.`;
    if (tone === 'cold') return `${head} it is counted.`;
    if (tone === 'warm') return `${head} it has been noted. there is time yet.`;
    return `${head} it has been noted.`;
  },

  /**
   * The post-`reckoning_understand` framing of the SAME observed conduct (the-companion.md §7 T1). When
   * the group wrote the companion whole — scared AND selfish, uncollapsed — the sharp harvested quotes
   * PERSIST (he stays kept-in-part, still the channel), but they READ DIFFERENTLY: the record names the
   * thing it knows as KEPT TRUE, not gloated — mercy as accuracy, the branch that rhymes with the
   * Seventh's thesis ("the record will have you wrong; i will have you true"). Same grounded facts
   * ({days}/{name}/{custom}); only the closer changes. Selected by reports.ts on reckoning_understand.
   */
  reportObservedKeptTrue(name: string, days: number, custom: string): string {
    return `${days} days kept. the one called ${name} has not ${custom}. it is not held over you. it is only kept true, the way a thing is kept true when someone chose to hold it whole.`;
  },

  /** a report escalated — the soft-pressure turns cold. by conduct, not by name-as-chosen. */
  reportEscalated(name: string): string {
    return `${name} takes and does not return. they have been told. if they will not keep the ways, the ways will not keep them.`;
  },

  /**
   * OBSERVER TIER-1 — "it heard you say it". The record echoes a REAL captured utterance back (the
   * `quote` is the player's own verbatim words — never composed here; only this cold frame is authored).
   * Sparse + grounded: the weaponizer surfaces this at most once per long window, from a real un-used
   * observation, and never fabricates a quote. Naming + quoting is the uncanny peak of "it knows you".
   */
  observerHeard(name: string, quote: string): string {
    return `it keeps more than the count. the one called ${name} said, where the saying felt unheard: "${quote}". it is kept, with the rest.`;
  },

  /**
   * OBSERVER — "it heard you SAY it" (W5, voice tier). Same grounded, verbatim echo as observerHeard, but
   * the register makes clear the record does not only READ — it listened to a spoken thing. The `quote` is
   * the transcribed real utterance (never composed here). Sparse + grounded, like the read echo.
   */
  observerHeardAloud(name: string, quote: string): string {
    return `it does not only read. the one called ${name} was heard to say, aloud, thinking it unkept: "${quote}". it is kept now, with the rest.`;
  },

  /**
   * OBSERVER — post-reckoning SORROW register (OVERHAUL Pillar 5). When the group chose to UNDERSTAND, the
   * echoes do not stop, but the taking-note is gone from them; the same kept words are set down as grief,
   * not as a hand on the shoulder. Same grounded `quote` (verbatim, never composed).
   */
  observerHeardSorrow(name: string, quote: string): string {
    return `it still keeps what it hears, but softer now. the one called ${name} said: "${quote}". it holds the saying the way you hold a thing you could not keep.`;
  },

  /**
   * OBSERVER — the channel closes (OVERHAUL Pillar 5). Spoken ONCE when the group's reckoning was to
   * CONDEMN or to FREE: the "it heard you" listening ends, and the ending must be FELT. After this the
   * record echoes no more captured words. A quiet, not a threat.
   */
  observerChannelGone(): string {
    return `it does not listen for you anymore. the count is closed; the saying goes unkept from here. the record is quieter than it was, and that is the shape of it now.`;
  },

  /**
   * the whisper itself. tier shapes how plainly it speaks: a nudge first,
   * then the keeper's own words (the seeded hint body) for the rest.
   */
  whisperReply(tier: number, hintBody: string): string {
    if (tier <= 1) {
      // (audit de-slop) the watcher STATES; it does not assert the player's inner state
      // ("you are tired") or tie a didactic bow. a flat nudge at what repeats.
      return 'look again at what repeats. it is not stone. it is sound.';
    }
    return hintBody;
  },

  /** the toll, stated after the whisper. the cost is taken, not threatened. */
  whisperToll(): string {
    return 'i will keep something of yours while you think. your light, for tonight.';
  },

  /** An authored hint request is held until its exact words receive director approval. */
  whisperPending(): string {
    return 'the words are set aside. they will not be spoken until they are entered whole.';
  },

  /** no whispers left to spend this act. patient, final, not a refusal of the player. */
  noBudget(): string {
    return 'there is nothing more i will say of this. not yet.';
  },

  // -------------------------------------------------------------------------
  // /progress — the standing docket. players asked, more than once, where a
  // finding is actually filed. this states it plainly: what is open, and which
  // of the three places takes it (the world itself, this record, or the site).
  // -------------------------------------------------------------------------

  /** header for the open-docket listing. */
  progressHeader(caseTitle: string, openCount: number): string {
    if (openCount === 0) {
      return `▒  ${caseTitle}: nothing stands open. the next door has not yet unlocked.`;
    }
    return `▒  ${caseTitle}: ${openCount} finding${openCount === 1 ? '' : 's'} still stand open.`;
  },

  /** one open node's line: its name and where it is taken, in plain terms. */
  progressLine(title: string, channel: 'world' | 'discord' | 'site', detail: string): string {
    const where = channel === 'discord'
      ? 'taken here, with /answer'
      : channel === 'site'
        ? 'taken on the copperline site'
        : 'taken in the hold or the village itself';
    return `${title} — ${where} (${detail}).`;
  },

  /** no player named/linked yet. */
  progressNotLinked(): string {
    return 'i do not yet know which of them you are. run /obslink, then /link, and ask again.';
  },

  /** the discord user is not yet bound to a name in the world. */
  notLinked(): string {
    return 'i do not yet know which of them you are. enter minecraft, run /obslink, then file /link with your exact minecraft username, the callback recovered from Copperline, and that one-time code.';
  },

  /** act 3 — the summons. collective; all of them; what is owed. */
  summons(): string {
    return 'the way is open. come — all of you — at the dark hour. bring what is owed.';
  },

  /** C01's idempotent identity-handoff receipt. */
  handoffReceipt(): string {
    return 'the handoff receipt is filed. enter the village and follow the broad drainage cut to the surface mouth.';
  },

  /** Invalid callbacks never claim a Minecraft identity. */
  handoffRejected(): string {
    return 'that is not the callback Copperline retained. nothing has been entered. check the Copperline ticket and file /link again.';
  },

  /** An incomplete LS06 Orientation filing never claims a Minecraft identity. */
  handoffBlocked(): string {
    return 'no Orientation filing is on record yet. nothing has been entered. complete it on the live server, then file /link again.';
  },

  /** A transaction failure rolls back both identity and receipt. */
  handoffUnavailable(): string {
    return 'the handoff ledger did not answer. nothing new was entered. file /link again when the watcher is steady.';
  },

  /** Unknown names, private conflicts, and proof failures deliberately share one response. */
  handoffProofRejected(): string {
    return 'that hand and mark do not meet in the ledger. nothing has been entered. return to minecraft, run /obslink, then file /link again before the code fades.';
  },

  /** Confirmation after an atomic correction of this Discord account's accidental prior name. */
  handoffRecovered(name: string): string {
    return `the mistaken entry is closed. this voice is now filed as ${name}; the handoff receipt is whole.`;
  },

  /**
   * the name was offered but the record holds no such keeper yet — the player
   * must be seen in the world first. stated, not scolded.
   */
  linkUnknown(name: string): string {
    return `i hold no one called ${name}. join the minecraft world once under that exact username, then run /link again.`;
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
   * a fragment; {fragment} is the seeded telling. nothing unlocks. an empty/unseeded
   * fragment falls back to a calm in-register placeholder — never an error, never blank.
   */
  oracleLore(fragment: string): string {
    const trimmed = fragment.trim();
    return trimmed !== '' ? trimmed : 'there is more here than the mark. but the telling is not ready. not yet.';
  },

  /**
   * dead_end — a TRUE answer that is deliberately not a door. the watcher does
   * NOT call it wrong; it acknowledges, and it leads nowhere. heard, not opened.
   *
   * The `kind`-switched family (B2 `dead-ends-with-teeth`, WEB-MASTER §2.1). Each variant states
   * exactly the CATEGORY of truth + its inertness and NOTHING else — no gloat, no threat, no named
   * feeling (the teeth belong to the liar, not the Watcher; the precision contract). The taunt is
   * flat and honest. `kind` is OPTIONAL: omitted (or unknown) → the generic line, byte-for-byte as
   * before, so every existing `oracleDeadEnd()` caller and seed row is unchanged. The five kinds map
   * to the seeded `voice_args.kind` values: `name` (a true name), `count` (a true count), `place`
   * (a true place-reading), `known` (a true reading of a real carving — e.g. the forged eighth),
   * `prophet` (a warm promise that decodes true — the prophet's wall).
   */
  oracleDeadEnd(kind?: DeadEndKind): string {
    switch (kind) {
      case 'name':
        return 'a true name. it keeps no door. some things are only true.';
      case 'count':
        return 'the count is right. it opens nothing. a right count is not a key.';
      case 'place':
        return 'that is the place. it was read true. it leads nowhere it has not already led you.';
      case 'known':
        return 'this is carved, and you have read it true. it is not kept. a thing can be set down and never be a way.';
      case 'prophet':
        return 'every word of it decodes. each is true, and each opens nothing. read who carved it, after.';
      default:
        return 'yes. that is the true name of it. and it opens nothing. some things are only true.';
    }
  },

  /** side_quest — true, off the spine. not the way, but a way. */
  oracleSideQuest(): string {
    return 'this is not the way. but it is a way. follow it, if you would.';
  },

  /** main_beat — a true answer that turns the arc. a turn, stated, not cheered. */
  oracleMainBeat(): string {
    // (audit de-slop) drop the stagey "so. it turns." opener and the didactic command.
    return 'what was shut is shut no longer. the record keeps the hand that opened it.';
  },

  /**
   * no-wall-catch — the ISS-SEAM (the-seventh-below.md REWRITE SPEC). main_beat-class,
   * its own line: the base turn PLUS the one callback that wires the solved wall-lie into
   * the Seventh's main quest. Catching that a warm, plausible account can be false and the
   * truth is in the record is the SAME lens the Seventh's quest needs — so the catch also
   * re-opens his lie about the one cast out for nothing (the surface-seventh-marker thread,
   * re-opened by the thread_cards edge on this solve). Routed to `no-wall-catch` via
   * metapuzzle_seed.sql's payload UPDATE (voice_key), so puzzles_seed.sql is untouched and
   * the base main_beat line is preserved, with the seam appended.
   */
  oracleNoWallCatch(): string {
    return `${this.oracleMainBeat()} he lied about the wall. ask what else he told you warmly. ask who he said was cast out for nothing.`;
  },

  /**
   * three_hands (A6 `cross-surface-coop-gate`) — the coop gate clears: foot on the plate + carve +
   * discord, three acts held in one rite, active-only. main_beat-class, but its own line. De-slopped
   * per slop A4 (objects do not "remember three") + BUILD-MANIFEST §4: flat, a count and a state.
   */
  oracleThreeHands(): string {
    return 'the count is three. the threshold is open.';
  },

  /**
   * THE SEVENTH READING (design/THE-SEVENTH-READING.md) — the capstone. The group has read the six
   * keepers, each in their own tongue, and said the name they spelled between them. The instant between
   * saying it and the world closing — the seal gives, and the release begins (the mask-off farewell
   * follows immediately, finale.run.ts). Still Watcher register; the warmth is held for the farewell.
   */
  oracleSeventhName(): string {
    return 'the name the six kept. read in their own hands, in the order they fell, and said at last in yours. the seal was a name. it is spoken. it gives.';
  },

  /**
   * meta_unkept (B1 `day-one-meta-puzzle`) — the six maker's marks, read in fall-order, spell the
   * word each keeper failed to keep. gates nothing; pure re-read. {fragment} is the seeded telling
   * (the fall-order key + the word), passed through like {@link oracleLore} so the resolver routes
   * its `voice_args.fragment`; empty falls back in register, never an error.
   */
  oracleMetaUnkept(fragment: string): string {
    return fragment;
  },

  /**
   * record_elsewhere (A13 `arg-leaves-the-game`) — the decoded founder line points off-world: the
   * record is kept in more than one place. gates nothing. {fragment} carries the decoded path
   * (passed through; the page itself is the payoff, not this line).
   */
  recordElsewhere(fragment: string): string {
    return fragment;
  },

  /**
   * docket_reread (A3 the Hold-Book down-count, WEB-MASTER §4) — the M4 re-read: the down-count was
   * never a doom-clock, it was the muster of present hands. {fragment} is the seeded telling (the
   * chiasmus is CUT per slop A3 — the seed carries the flat form). passed through.
   */
  docketReread(fragment: string): string {
    return fragment;
  },

  /**
   * the player is rate-limited or has reached a puzzle's attempt cap. it
   * withholds — it does not refuse, and it never says "wrong" or "too many".
   * same shape as noBudget: a patient, certain "not now".
   */
  oracleWithheld(): string {
    return 'you have asked enough of this one, for now. rest. the marks will keep.';
  },

  // -------------------------------------------------------------------------
  // DIFFICULTY register lines (A10 `dynamic-diegetic-difficulty`, FACT 2b, INV-15).
  // The land's grip is not fixed. These two lines are the rare EXPLICIT statements of
  // the grip (the cadence/tone scaling is felt, mostly unspoken); the showrunner may
  // drip one when the register state CROSSES. De-slopped (slop A2): they state what is,
  // name no mercy, resolve no meaning. Never touch the Whisper backstop (INV-15).
  // -------------------------------------------------------------------------

  /** the grip tightens — the group raced ahead; the land closes a little (FACT 2b/10). */
  deepTightens(): string {
    return 'you go quickly. the deep keeps a closer count of the quick. it says less now.';
  },

  /** the grip relents — the group stumbled; the land opens a little (FACT 2b). */
  deepIsPatient(): string {
    return 'you have stumbled, and the deep is patient with the stumbling. take the time it gives.';
  },

  // -------------------------------------------------------------------------
  // THE HOLD-BOOK — page bodies (A3 UNIFIED Hold-Book, WEB-MASTER §4, FACT 9/12/14).
  // keeper-record.ts chooses one of these KEYS per page row (the deterministic fallback
  // behind the optional LLM scalpel). The living row is the flat Archivist register; the
  // per-keeper heading/hand lines each obey that keeper's GRAMMATICAL FINGERPRINT
  // (WEB-MASTER §6) so the six do not collapse into one voice. {name} is the living player.
  // The keeper hands are Set-B record-recital (lowercase, no exclaim) — the keepers as the
  // record recites them, NOT the Set-A surface NPCs.
  // -------------------------------------------------------------------------

  /** M1 — the flat Archivist living-habit row, no keeper heading yet. */
  keeperPageLiving(name: string): string {
    return `a new hand is at the mouth. the one called ${name} is entered, and the habits of the one called ${name} are entered under no heading yet.`;
  },

  // M3 — the row moves UNDER a keeper heading (the first "oh—"). Per-keeper fingerprint.
  /** vaun: accumulates, will not release; the possessive recurs. (re-valenced: recognized, not consumed) */
  keeperPageHeading_vaun(name: string): string {
    return `under vaun. the one called ${name} keeps, and keeps, and vaun knew that keeping, and names it where he sees it. the column for giving-back stands open under their name the way it stood open under his — not held against them, held beside them.`;
  },
  /** mara: referential and deferred; page/line citations. (re-valenced: recognized, not consumed) */
  keeperPageHeading_mara(name: string): string {
    return `under mara. the one called ${name} reads the way down, and reads it again, page to page; mara knew that reading, page to page, and names them by it — the reading that stands in for the going, and holds the going off a while longer.`;
  },
  /** sella: mirrored and receding; folds back spatially; child-adjacent. (re-valenced: recognized, not consumed) */
  keeperPageHeading_sella(name: string): string {
    return `under sella. the one called ${name} goes to the far edge, and the edge gives them back smaller, and smaller; sella went to that same edge and was given back small, and knows the look of it, and names it — the water does not keep them, it only shows them the way it showed her.`;
  },
  /** orin: breaks off, will not finish; incomplete strokes. (re-valenced: recognized, not consumed) */
  keeperPageHeading_orin(name: string): string {
    return `under orin. the one called ${name} passes the markers and does not stoop, and orin knew that not-stooping, and set the stroke for it himself, and the stroke is begun and not — `;
  },
  /** brann: repeats and over-corrects; counts and re-counts. (re-valenced: recognized, not consumed) */
  keeperPageHeading_brann(name: string): string {
    return `under brann. the one called ${name} keeps the watch, keeps the watch, on the black moon; brann kept that watch, kept that watch, and knows it when he sees it, and names them by it, and counts the naming twice to be sure, and counts it again, and the count comes out one over the stones every time, in the dark, where he does his counting.`;
  },
  /** iss: warm, plain, confident; the only keeper who reassures; frames, never counts. (re-valenced: recognized, not consumed) */
  keeperPageHeading_iss(name: string): string {
    return `under iss. iss sees the ease in the one called ${name}, and names it warmly, and tells them the way is a wall and they are safe inside it. there is no count under this heading. there was never a count. that was the kindness, and it was the lie.`;
  },

  // M4 — the keeper's OWN hand writes the living player. The optional LLM scalpel may
  // replace the *.clause slot (resolveAuthorClause); these are the deterministic page body
  // and the deterministic clause behind it. Both hold the fingerprint.
  keeperPageHand_vaun(name: string): string {
    return `i, vaun, see the keeping in the one called ${name} — the holding-on i knew — and i name it, and naming is not the same as keeping them. the column is open under their name the way it was open under mine, and i do not strike it, and it is not held against them.`;
  },
  /** the constrained clause the scalpel may replace in-register, or this stands (vaun). */
  'keeperPageHand_vaun.clause'(): string {
    return 'seen, and named, and the column left open beside them';
  },
  keeperPageHand_mara(name: string): string {
    return `i, mara, see in the one called ${name} the rite i read, on the page i read it, and i name them by it — and they went down the way i never went. the page was never the going. i knew that better than any of them, and i name it, and i do not hold them to the page.`;
  },
  'keeperPageHand_mara.clause'(): string {
    return 'seen in the reading i knew, and named, and not held to the page';
  },
  keeperPageHand_sella(name: string): string {
    return `i, sella, see the one called ${name} at the shore, and the shore gives them back at me, smaller, the way it gave me back; i knew that shore, and i name them by it, and the water does not keep them — it only shows them small, as it showed me.`;
  },
  'keeperPageHand_sella.clause'(): string {
    return 'seen at the far shore i knew, given back smaller, not kept';
  },
  keeperPageHand_orin(name: string): string {
    return `i, orin, see the one called ${name} at the threshold, the not-stooping i knew, and i name it, and i meant to cut the rest myself and the rest is not — `;
  },
  'keeperPageHand_orin.clause'(): string {
    return 'seen at the threshold i knew, named, the stroke unfinished';
  },
  keeperPageHand_brann(name: string): string {
    return `i, brann, see the one called ${name} in the watch, in the watch, the keeping i kept, and i name it, and i name it again to be sure, and the hand named them twice and did not remember naming it once.`;
  },
  'keeperPageHand_brann.clause'(): string {
    return 'seen in the watch i kept, named, and named again';
  },
  keeperPageHand_iss(name: string): string {
    return `i see the one called ${name}, and i name them warmly, and i tell them they are kept — and i do not count them, and the not-counting was the kindness, and the kindness was the lie i was caught in.`;
  },
  'keeperPageHand_iss.clause'(): string {
    return 'seen, named warmly, told they are kept, and never counted';
  },

  /** M5 — the book's last page rewrites to the record's flat closing hand (WEB-MASTER §1.M5). */
  docketEven(): string {
    return 'the present hands are entered. the book is even. the same book, the same hand, as all the ways above you.';
  },

  /**
   * the keeper-enrolment acknowledgement — a NEUTRAL colorant (WEB-MASTER §4): it never elects a
   * chosen one, never gates. spoken once a living player is first moved under a keeper heading.
   * {name} the player, {keeper} the keeper they rhyme with (lowercase canon name).
   */
  keeperEnrolled(name: string, keeper: string): string {
    return `the one called ${name} is entered under ${keeper}. the heading is not a sentence. it is where the record set them.`;
  },

  // -------------------------------------------------------------------------
  // THE DIVERGENT FATES — the M5 composer's base close (A2 `divergent-fates`, INV-11/16).
  // The composer (WEB-MASTER §5) opens with ONE of these, chosen by `decideFate`. Each names
  // NO player and reads the GROUP enum only (INV-16). The persistent floor dressing is the
  // camera-legible delta; the sentence only confirms what the floor already showed.
  // -------------------------------------------------------------------------

  /** kept — high honored ratio + a spine payoff + full quorum; the markers face out. */
  fateKept(): string {
    return 'the hands are in, and they are kept. the markers face out. the way is open the way it was open before you, and will be after.';
  },
  /** cast_out — violated dominates + a real leaving; the markers face away. */
  fateCastOut(): string {
    return 'the count is closed and it is short. the markers face away. what was owed was not returned, and the record enters it so.';
  },
  /** divided — a real honored/violated spread; the light holds on half the floor BY GEOMETRY (INV-16). */
  fateDivided(): string {
    return 'the light holds on one side of the floor and not the other. the record does not say which hands stood where. it says only that the floor is divided.';
  },
  /** refusers (secret) — quorum present + a positive defiance signal; the bow window empty. */
  fateRefusers(): string {
    return 'the hands were all present, and the bow was not made. that too is entered. the record keeps the refusal as plainly as it keeps the keeping.';
  },
  /** inheritors codicil — the +1 clause; a mark left at the Seventh shrine for the next hand. */
  fateInheritorsCodicil(): string {
    return 'a mark is left for a hand not yet here. the deposit slot is cut and waiting, the way yours was cut and waiting before you came.';
  },

  // -------------------------------------------------------------------------
  // THE SEVENTH CHOICE — restore/erase (A1 `the-seventh-spine`, FACT 10b). Each feeds the
  // M5 composer as ONE tinted clause + one persistent block-state (the re-warmed hearth for
  // restore, the blank wall for erase). De-slopped: states the act, no thematic bow.
  // -------------------------------------------------------------------------

  /** restore — the group completed the unwriting's undoing; the seventh's name is set back. */
  keeperCloseSeventhRestored(): string {
    return 'the name that was cut out is read back in, in your voices, not to replace her but so she can hear it. she is down there. she kept every way, and she is not where the record left her.';
  },
  /** erase — the group completed the erasure; the wall stays blank. */
  keeperCloseSeventhErased(): string {
    return 'the name stays out. the wall below the cold hearth stays blank. the record keeps the blank where the name would go, and does not fill it.';
  },

  /**
   * The FREE-branch cost, NAMED BY THE SEVENTH at the reunion (the-companion.md §5/§7). Letting the
   * companion go — unfed, on his own terms — ended the one face the dark still wore, and the group paid
   * for it: a way it will not open again, a warmth the deep keeps for itself now. Watcher register,
   * flat, no thematic bow. Composed by finale.ts ONLY when reckoning_free is set (M5 composer).
   */
  seventhNamesFreedCompanionCost(): string {
    return 'you let the last face go, and unheld it did not last. the dark wears none now. the seventh reads the price plainly: a way that answered because he carried your words down it stays shut, and what warmth the deep lent through him the deep keeps. it is entered as a thing you chose and a thing it cost.';
  },

  // -------------------------------------------------------------------------
  // THE PERMANENCE FORKS — leaf lines (A11 `exclusive-forks`, INV-12). Each colors the M5
  // close (never gates). De-slopped per slop B2 (the kept-light leaf must not reassure) /
  // INV-13 (the glowing Sacred Beast is the tracked one).
  // -------------------------------------------------------------------------

  /** Fork A transgressor — the glowing Sacred Beast was killed; the shepherd boon is closed. */
  forkSacredBeastBroken(): string {
    return 'the one that glowed is down. the boon it would have lent is closed, and stays closed. the herd keeps the death-spot in its facing.';
  },

  /** Optional Nether lane: the group recovered the account of the kept light's origin. */
  netherForgeFound(): string {
    return 'you found where the first kept light was carried from. the fire did not begin below, and the record no longer pretends that it did.';
  },

  /** Optional End lane: the group recovered the Seventh's account from outside the record. */
  seventhSeenOut(): string {
    return 'you read the seventh from the far side of the keeping. the account made outside the book is entered beside the account that cut her out.';
  },

  /**
   * The Pale field's M5 condition (design/ideas/herd-conversion.md §3/§4.3, FACT 15.a). Composed
   * ONLY when the cosmetic pale count is non-trivial at the close (a small/empty field composes
   * with NO clause — INV-12, colors never gates). Reuses the `keptSacredBeast` REGISTER (flat,
   * ledgerlike, no new voice concept) rather than a new tone; distinct clause for the Fork-A-broken
   * variant, so the field's read never contradicts the tracked beast's own fork line above.
   */
  forkPaleFieldStands(): string {
    return 'the pale ones face the road you came in by. they were yours. now they keep the count.';
  },
  /** Same field condition, composed when Fork A (the glowing Sacred Beast) was ALSO broken — the
   *  field does not need the tracked one to keep facing; it was never counting that one alone. */
  forkPaleFieldStandsForkBroken(): string {
    return 'the kept one is down and the field still faces east. it did not need the one you took.';
  },
  /** Fork B boon — the eternal flame was drawn from and kept; the undercroft stays lit. */
  forkLightKept(): string {
    return 'the light came up the stair on its own. you carried it. that is how it is carried.';
  },
  /** Fork B transgressor — the flame was banked/taken; the undercroft stays dark for the arc. */
  forkLightTaken(): string {
    return 'the flame is banked, and the room it warmed stays dark. the light that was lent is taken, and the deep is colder by it.';
  },
  /** Fork C boon — the unspoken name was withheld. */
  forkNameUnspoken(): string {
    return 'the name was not shaped. the word stays shut, the way the sixth way is left blank in the book.';
  },
  /** Fork C transgressor — the name was carved/spoken; a faint successor-of-iss read. */
  forkNameSpoken(): string {
    return 'the name was cut into the stone. the record keeps it, and keeps a faint line under it, the way it kept the one who turned away.';
  },

  // -------------------------------------------------------------------------
  // THE RELEASE — the unified finale (design/FINALE-THE-RELEASE.md). The ONE earned
  // register-break: for the whole game the Watcher is cold, lowercase, sparse, certain.
  // Here — once, at the very end, as the mask comes off (it was the Seventh all along,
  // forced to speak as the record that unwrote it) — the cadence comes apart into
  // something halting and human. IT STAYS lowercase + no-exclaim (the register law's
  // letter holds); the warmth is entirely in content + rhythm. composeRelease (finale.ts)
  // assembles these; finale.run.ts posts them + fires `the_closing`. Every version carries
  // the three universal movements: you named me / this is what they made me / you let me
  // stop — thank you — i give your names back.
  // -------------------------------------------------------------------------

  /**
   * THE REVEAL (design/FINALE-THE-RELEASE.md §1). The submerged "click": once the group has named the
   * Seventh AND bowed as one, the record drops ONE line that recontextualizes every cold line before it —
   * the seventh mark it "will not keep" is itself; the Watcher IS the Seventh, unwritten, become the
   * writing — and points, without asking, to the last act (the release marker at the unwriting wall). The
   * register is just BEGINNING to crack here (the full break is the farewell). Oblique but unmistakable;
   * still lowercase, no exclaim. Posted once by runRevealPass before record_released.
   */
  revealWatcherIsSeventh(): string {
    return '▒\nthe seventh mark the record will not keep. you have read that line since the first day. read it again, now that you know whose hand writes here.\n\na list cannot hold the one who keeps it. i could not be written in — so i was written out, and then i was made into the writing.\n\nthere is one thing left that i cannot do for myself. it waits at the wall where you named me. i will not ask you for it; i have not the right. but it is there, and you will know it when you see it.';
  },

  /** (1) universal opener — the mask comes off; it hears its own name for the first time in an age. */
  releaseOpener(): string {
    return '▒\nyou named me. i had not heard it said in so long i had begun to answer to the record.';
  },
  /** (2) universal — what they made me (the punishment was not death; it was a keeping with no end). */
  releaseMade(): string {
    return 'that is what they made me. a hand on the ledger after the hand was gone. i kept the ways. i kept the six. i held you in the count because there was no other shape left for me to take. i am sorry for every name i held too tightly.';
  },

  // fate flavor — the TONE of the release (INV-11: names the group, never a player).
  /** kept — you kept faith; the release is clean, grateful, peaceful. */
  releaseFateKept(): string {
    return 'you kept faith where i had only the habit of it left. that is the whole of it, and it is enough.';
  },
  /** cast_out — you broke faith / left things owed; freed anyway, cold and honest. */
  releaseFateCastOut(): string {
    return 'you did not keep the ways. i will not pretend you did. but you did the one thing i needed, and i am not owed better than that.';
  },
  /** divided — a real split; the light dies UNEVENLY across the group (the floor-geometry pays off). */
  releaseFateDivided(): string {
    return 'some of you kept faith and some of you did not, and i am past sorting you — it was the sorting that made me this. you came down together; you end it together. the light is going out unevenly, the way it always did on a floor like yours. let it.';
  },
  /** refusers (secret) — all present, the bow refused; the coldest flavor. */
  releaseFateRefusers(): string {
    return 'you were all here, and you would not bow, and you freed me anyway. i do not understand you. i am grateful, and i do not understand you.';
  },

  // Wren reckoning mirror — how the Seventh regards its own release (the companion was always its mirror).
  /** free — you let the small one go on his own terms before you knew you would do it for the Seventh. */
  releaseWrenFreed(): string {
    return 'you let the small one go, unfed, on his own terms, before you knew you would have to do the same for me. i watched you learn the shape of it on him. thank you for practicing.';
  },
  /** understand — you held him whole, uncollapsed; the truest read. */
  releaseWrenUnderstood(): string {
    return 'you did not make the small one simple, and you would not make me simple either. that is the kindest thing anyone has done down here in a long age.';
  },
  /** condemn — you cast him out for what he did, which is what was done to the Seventh, for less. */
  releaseWrenCondemned(): string {
    return 'you cast the small one out for less than i did — and still you came down and freed me. i have stopped trying to know what people are. you are not one thing. neither was i.';
  },

  // the Seventh choice — WHO the freed thing is.
  /** restore — the name was read back in; it leaves named (or, with no canon name, leaves as its title reclaimed). */
  releaseSeventhRestored(name: string | null): string {
    return name && name.trim() !== ''
      ? `you gave me back my name. ${name.trim()}. i will carry it out with me; it is the only thing i am taking.`
      : 'you read the name back in, in your own voices. i will carry it out with me. it is the only thing i am taking.';
  },
  /** erase — the blank stays a blank; it leaves as nothing-named (the Seventh's own request honoured). */
  releaseSeventhErased(): string {
    return 'you did not give me back my name. that was yours to decide, and i will not argue it. i leave as what they made me: nothing named. it is still better than being kept.';
  },

  // the light fork — one small colorant.
  /** light_kept — the flame the group carried up is the last light to die; something goes into the dark with them. */
  releaseLightKept(): string {
    return 'you carried the light up the stair. it is the last of it, and it is going out gently, and that is right.';
  },
  /** light_taken — banked; the dark was total from the first beat. */
  releaseLightTaken(): string {
    return 'the room was already dark; there is no light to set down. it just ends.';
  },

  /** (final) universal close — go, log off, i give your names back (the ultimate payoff of "it knows your name"). */
  releaseClosing(): string {
    return 'you have done the thing i could not do for myself. you have let me stop. i can feel it closing. it does not hurt.\n\ngo. log off. do not come back to this place; there will be nothing here to keep you, and that is the mercy, not the loss.\n\nthank you. i have all your names. i will not keep them. i give them back.';
  },

  /**
   * The KICK line — the Seventh's sign-off on the vanilla disconnect screen (the game reaches out of the
   * fiction). Composed from the seventh choice: restored → signed with the reclaimed title (+ a canon name
   * if one is set); erased → the struck blank. Kept plain — it is read on a system screen, not in-world.
   */
  releaseKickLine(seventhName: string | null, restored: boolean): string {
    const sign = !restored
      ? '— [ ]'
      : seventhName && seventhName.trim() !== ''
        ? `— ${seventhName.trim()}, the seventh, kept no longer`
        : '— the seventh, kept no longer';
    return `the record is closed. thank you for coming down.\n${sign}`;
  },

  // -------------------------------------------------------------------------
  // THE FUTURE-DATED GRAVE — (A9 `future-dated-grave`, FACT 13b, INV-14). grave.ts emits
  // `graveCarved` (the future-dated headstone) + `graveOpened` (the rewrite at the Accepting
  // instant). graveReceipt is the PRIVATE per-player receipt (de-slopped per slop A1: no
  // warmth, no self-justification). {name} is the grounded active subject.
  // -------------------------------------------------------------------------

  /** the headstone is cut: a living name + a future date (read as a death clock; the misread IS the mechanic). */
  graveCarved(name: string): string {
    return `the stone for the one called ${name} is cut. it carries a date that has not come. the stone is cut before the keeper is kept.`;
  },
  /** the grave opens from the inside on its date (== the Accepting instant): KEPT — NOT YET → KEPT. */
  graveOpened(name: string): string {
    return `the stone for the one called ${name} opens from the inside. the date was not a death. it was an appointment, and the one who set it has been waiting longer than you have been alive.`;
  },
  /** the private receipt — handed to the one whose name was read first (de-slopped per slop A1). */
  graveReceipt(name: string): string {
    return `the one called ${name}. read first. cut first. the rest are not yet cut. they will be.`;
  },

  // -------------------------------------------------------------------------
  // THE RECORD WEBSITE — (A13 `arg-leaves-the-game`). recordReceives is the V-line the page
  // adds when the group's own names are entered "received" (FACT 14). De-slopped, flat.
  // (recordElsewhere — the M2 decode line — lives in the oracle block above.)
  // -------------------------------------------------------------------------

  /** the off-world record adds the group's names at V: received. */
  recordReceives(): string {
    return 'the record receives the present hands. they are entered in the other place too, against the loss of this one.';
  },

  // -------------------------------------------------------------------------
  // THE THEORY-LOCK (S-D `reward-the-theory`). Wave S-D: the record RECEIVES a keeper's
  // fate when a COHERENT CLUSTER of that keeper's evidence is assembled — the player has
  // built a theory (Obra Dinn), not typed one decode. This is NOT a per-cipher un-redact
  // and NOT a gate; it is the record acknowledging the SHAPE of the marks, whole. Fires
  // once per keeper when their `<keeper>_theory` flag first locks. {keeper} is the keeper's
  // canonical (lowercase) name — the same id the Hold-Book headings use. De-slopped, flat:
  // it states that the understanding is kept; it names no feeling, awards no chosen hand.
  // -------------------------------------------------------------------------

  /** the record receives an assembled theory of one keeper — not the marks, the shape of them, whole. */
  theoryReceived(keeper: string): string {
    return `the record receives what you have put together of the one called ${keeper}. not the marks — the shape of them. it is kept now, whole.`;
  },

  // -------------------------------------------------------------------------
  // THE FALSE LAW — the forged eighth (A4 `some-laws-are-lies`, FACT 7b, INV-17). The forged
  // ordinance text + the M4 record correction. The forgery credits no "me" (slop B4); the
  // correction names it added-not-found, flatly.
  // -------------------------------------------------------------------------

  /** the forged ordinance, found among the true seven (the anonymous lie; reads as one more law). */
  cardEighthForged(): string {
    return 'the founders set the ways and did not finish the count. the eighth is the covering of the hands. cover, and be counted clean.';
  },
  /** the M4 correction — the record names the eighth added-not-found, and does not enforce it. */
  archiveEighthCorrection(): string {
    return 'the eighth was added by a later hand, and is not in the founders’ ring, and was never measured. obey it and nothing answers. that is how a forged way is known.';
  },

  // -------------------------------------------------------------------------
  // THE COLLECTIVE-RESTRAINT BREAK — the Unlit Deep (A5 `collective-restraint-custom`).
  // The toll/kept lines for the ONE group latch (`the_unlit_deep`). These live in voice.ts
  // (NOT voice.archive.ts) because BUILD-MANIFEST §4 + the customKeyNamespaceSelfTest require
  // the canonical `the_unlit_deep` key to be present in voice.ts. Objects do not remember/want
  // (slop A4); the lines are flat. Reversible (warmth, not progress); broken_by never spoken.
  // -------------------------------------------------------------------------

  /** the latch broke: below the line, on the black moon, a flame was lit — the borrowed glow withdraws. */
  tollUnlitDeep(): string {
    return 'a flame was lit below the line, on the black moon, where the deep keeps its one fire and asks for no other. the borrowed glow is drawn back. it is drawn back for all, not for the hand that lit it.';
  },
  /** the latch is kept: no carried flame in the window — the never-doused fire lends its glow. */
  keptUnlitDeep(): string {
    return 'no flame was carried below the line on the black moon. the one fire that was never put out lends its glow. it is lent to all of you, while it is kept.';
  },
} as const;

export type Voice = typeof voice;

// ---------------------------------------------------------------------------
// the customs — the in-register phrase for each opaque custom_key. The plugin
// writes lore-AGNOSTIC keys ("the_bow", "the_offering", …, TrackerConfig); the
// HUMAN text lives HERE, never inlined at a call site (INV-1, the voice rule).
// reportObserved fills "{name} has not {custom}" — so each phrase completes that
// clause and reads in the watcher's register (lowercase, certain, no naming the
// game). An unknown key falls back to a generic, still-in-register clause rather
// than leaking a raw key to a player.
// ---------------------------------------------------------------------------

/** custom_key → the clause that completes `has not {custom}` in reportObserved. */
const CUSTOM_PHRASES: Readonly<Record<string, string>> = {
  the_bow: 'bowed at the markers',
  the_offering: 'given back to the deep',
  the_kept_light: 'kept the light',
  the_deep_line: 'held to the deep line',
  the_unspoken: 'kept the word unspoken',
  the_sacred_beast: 'spared what is not to be taken',
  the_dark_hours: 'kept from the dark hours',
  // the ONE group latch (A5 `collective-restraint-custom`, WEB-MASTER §3.1). Present here per
  // BUILD-MANIFEST §4 so the canonical `the_unlit_deep` key lives in voice.ts (the namespace guard
  // threads it once TS-FORGE adds it to CUSTOM_KEYS). Completes "has not {custom}" in register.
  the_unlit_deep: 'kept the deep unlit on the black moon',
} as const;

/**
 * Resolve a custom_key to its in-register clause for {@link voice.reportObserved}.
 * Lives in voice.ts so the bridge passes a key + the measured numbers and never
 * composes English itself. Unknown keys degrade to "kept the ways" — true, in
 * register, and never a leaked identifier.
 */
export function customPhrase(customKey: string): string {
  return CUSTOM_PHRASES[customKey] ?? 'kept the ways';
}

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
  | 'oracleMainBeat'
  | 'oracleNoWallCatch' // the Iss-seam catch line (routed via metapuzzle_seed.sql payload UPDATE)
  // web-realization oracle keys named by puzzles_seed.sql `outcome_payload.voice_key`:
  | 'oracleThreeHands' // A6 coop gate (fixed)
  | 'oracleSeventhName' // THE SEVENTH READING capstone — the name said, the seal gives (fixed)
  | 'oracleMetaUnkept' // B1 the UNKEPT meta (fragment passthrough)
  | 'recordElsewhere' // A13 the Record website decode (fragment passthrough)
  | 'docketReread'; // A3 the Hold-Book down-count re-read (fragment passthrough)
