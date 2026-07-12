/**
 * clue-specs.ts — THE AUTHORED-PUZZLE-KEY → FORGE-SPEC BIND (COHERENCE-AUDIT X1 / P0-1).
 *
 * THE GAP THIS CLOSES (COHERENCE-AUDIT §2 cross-seam X1, gaps A3 / B5 / C1):
 *   `cipher-web.md §5` asserts an "authoring/placement step binds the carved artifact to
 *   the node's authored kebab puzzle_key." No code did this. So:
 *     - A3: no carved ciphertext existed to author against (only decoded plaintext lived
 *           in the seed) — the in-world stone could drift from the Discord card.
 *     - C1: the showrunner drip had nothing to forge from (the whole forge was orphaned
 *           from the drip engine).
 *     - B5: the cipher↔node join lived only as prose across three design docs.
 *
 * THE FIX (one registry, machine-checkable):
 *   Map each ACTIVE, Discord-decodable seed `puzzle_key` → a typed { cipher, plaintext,
 *   params } spec. `forgeForPuzzle(key)` runs `forgeClue` with that spec, so the
 *   world-placement step and the showrunner drip render ONE plaintext IDENTICALLY on
 *   both surfaces (the carved stone and the Discord clue card are the same artifact).
 *
 * THE INVARIANT (specsSelfTest, mirrored in specs.selftest.ts):
 *   For every entry:  decode(forge(spec).ciphertext) === spec.plaintext (uppercased
 *   forge-canonical form)  AND  normalizeAnswer(spec.plaintext) ∈ that seed row's
 *   accepted_answers.  This makes it structurally impossible for the carving and the
 *   seed to drift: a wrong shift, a renamed key, or an edited seed answer fails the
 *   build, not the player.
 *
 * SCOPE (the authoring contract, COHERENCE-AUDIT §4): this registry is WIRING + a
 *   registry, NOT new story. It covers ONLY the nodes that actually CARRY a
 *   Discord-decodable cipher (the real keeper stones). Sentinel / found-document /
 *   in-world-only / observation / ritual rows are NOT cipher artifacts and are listed
 *   in NON_CIPHER_KEYS with the reason — they are intentionally excluded, never
 *   forgeable, and the showrunner must route them to an in-world-pointing report (C3),
 *   never post a forged card for them.
 *
 * PURE: no discord, no supabase, no I/O. Same imports as the forge core.
 */

import {
  forgeClue,
  caesar,
  vigenere,
  substitution,
  bookCipher,
  railFence,
} from './index.js';
import type { ClueSpec, ForgedClue } from './index.js';
import { normalizeAnswer } from '../oracle/normalize.js';

// ---------------------------------------------------------------------------
// Registry entry shape.
// ---------------------------------------------------------------------------

/**
 * One authored cipher node. `puzzleKey` is the canonical kebab key from
 * `puzzles_seed.sql` / `clue-web.md §3`. `toSpec()` builds the exact `ClueSpec`
 * the forge consumes; `decodeCiphertext()` is the cipher-aware inverse used by the
 * build-time invariant to prove the carved ciphertext decodes back to `plaintext`.
 * `acceptedAnswers` mirrors the seed row so the invariant can assert membership
 * without a DB (the seed stays the source of truth; this is the machine-checked twin).
 */
export interface ClueSpecEntry {
  /** canonical authored kebab key — the resolver PK + the seed row + clue-web node. */
  readonly puzzleKey: string;
  /** which keeper / lore doc this node embodies (authoring trace only). */
  readonly keeper: string;
  readonly doc: string;
  /** the cipher this stone is carved in. */
  readonly cipher: ClueSpec['cipher'];
  /**
   * The ONE plaintext rendered on BOTH surfaces. Letters/digits/spaces only so it
   * carves cleanly and round-trips; chosen so `normalizeAnswer(plaintext)` is one of
   * the seed row's `accepted_answers` (the bind that can't drift).
   */
  readonly plaintext: string;
  /** the seed row's accepted_answers (mirror of puzzles_seed.sql, for the invariant). */
  readonly acceptedAnswers: readonly string[];
  /** build the forge spec for this node (the params: shift / key / book / …). */
  toSpec(): ClueSpec;
  /** cipher-aware inverse of the carved ciphertext → the forge-canonical plaintext. */
  decodeCiphertext(ciphertext: string): string;
}

// ---------------------------------------------------------------------------
// The book Mara's stone indexes into. The book cipher needs the SAME source text
// at forge time and decode time (the in-world "Kept-Light shelf" walked L→R, per
// clue-web-seed-notes.md). Kept here so the bind is self-contained + checkable; the
// world build places these as the six lectern books at `kept_light_home_01`. Every
// word of Mara's plaintext MUST appear here or the forge throws (a real authoring
// error, surfaced at build, never at a player).
// ---------------------------------------------------------------------------

const MARA_BOOK = [
  'descend the stair by still water',
  'and bow your head at the door',
  'the unbroken light waits below',
  'do not write it and call it kept',
  'do the thing the marks tell you',
].join('\n\n');

// ---------------------------------------------------------------------------
// THE REGISTRY — ACTIVE, Discord-decodable cipher nodes only.
//
// Derived from clue-web.md §3 (cipher per node) + cipher-web.md §1 (per-cipher
// params) + puzzles_seed.sql (the accepted_answers each plaintext must hit):
//   stone-vaun         → caesar    (P1, shift = Vaun's hoarding "three of each" → 3)
//   stone-mara         → book      (P5, the six-book Kept-Light shelf)
//   stone-orin         → subst     (P4, the rune alphabet; crouch-to-read)
//   stone-iss-wall     → vigenere  (P3, key = ISS — his own name, the lie)
//   stone-brann-cipher → railfence (rails = 9, "nine lit" — his own doubled count)
//
// stone-sella's Atbash was deactivated (2026-07-05 cohesion pass): redundant with its
// replacement, sella-reflection-bearing, and Sella's own LATER DRIFT note says her later
// marks resolve to drawings, not letters. Not registered here on purpose — see the seed
// row's comment for the full reasoning.
// ---------------------------------------------------------------------------

export const CLUE_SPECS: readonly ClueSpecEntry[] = [
  // ── stone-vaun · Caesar ──────────────────────────────────────────────────
  // cipher-web §1 P1: "the shift IS his hoarding (three of each)" → shift 3. The
  // plaintext is the de-shifted ledger line; normalizes to the seed's first answer.
  {
    puzzleKey: 'stone-vaun',
    keeper: 'Vaun',
    doc: 'D02 counted-them-in-the-dark',
    cipher: 'caesar',
    plaintext: 'GIVE THE FIRST OF THE DEEP BACK TO THE DEEP',
    acceptedAnswers: [
      'give the first of the deep back to the deep',
      'the land counts first',
      'i counted them in the dark and gave none back',
    ],
    toSpec() {
      return { cipher: 'caesar', text: this.plaintext, shift: 3, namespace: this.puzzleKey };
    },
    decodeCiphertext(ct) {
      return caesar.decode(ct, 3);
    },
  },

  // ── stone-mara · book cipher ────────────────────────────────────────────
  // cipher-web §1 P5: the six page/line/word triples assemble the descent sentence;
  // the oracle accepts the SENTENCE, not the raw triples. forge.solution is the
  // uppercase assembled sentence; the carved artifact is the ref-string.
  {
    puzzleKey: 'stone-mara',
    keeper: 'Mara',
    doc: 'D05 page-line-word',
    cipher: 'book',
    plaintext: 'DESCEND AND BOW AT THE UNBROKEN LIGHT',
    acceptedAnswers: ['descend and bow at the unbroken light'],
    toSpec() {
      return {
        cipher: 'book',
        text: this.plaintext,
        book: MARA_BOOK,
        namespace: this.puzzleKey,
      };
    },
    decodeCiphertext(ct) {
      // The carved ciphertext is the ref-string (bookCipher.format); parse it back to
      // BookRef[] and resolve against the SAME book the forge used.
      return bookCipher.decode(bookCipher.parse(ct), MARA_BOOK);
    },
  },

  // ── stone-sella · Atbash — REMOVED (puzzle-variety audit) ───────────────
  // stone-sella is deactivated in puzzles_seed.sql: redundant with its own better
  // replacement (sella-reflection-bearing, environmental, no letter-reversal), and
  // cipher-plaintexts.md's own "LATER DRIFT" note says Sella's later marks resolve
  // into drawings, not words. The row is deactivated, not deleted (history kept),
  // so it now belongs in NON_CIPHER_KEYS below (an inactive row is never forged) —
  // see that entry for the full reasoning.

  // ── stone-orin · substitution ────────────────────────────────────────────
  // cipher-web §1 P4: one mark, one letter via the rune alphabet; the carving faces
  // the floor (crouch-to-read). The decoded line normalizes to the seed's first answer.
  {
    puzzleKey: 'stone-orin',
    keeper: 'Orin',
    doc: 'D07 i-thought-it-small',
    cipher: 'substitution',
    plaintext: 'I THOUGHT IT SMALL IT WAS NOT SMALL',
    acceptedAnswers: [
      'i thought it small it was not small',
      'threshold',
      'the bow is the smallest of the ways',
    ],
    toSpec() {
      return { cipher: 'substitution', text: this.plaintext, namespace: this.puzzleKey };
    },
    decodeCiphertext(ct) {
      return substitution.decode(ct);
    },
  },

  // ── stone-brann-cipher · rail-fence ──────────────────────────────────────
  // design/PUZZLE-DESIGNS.md §6 / backlog-keeper-stone-expeditions §1.4: the sixth
  // keeper-stone expedition, previously staged (active=false, no CLUE_SPECS entry —
  // puzzle-variety audit fix). rails = 9, the fire-count Brann himself names in the
  // solved beat's relabel text ("nine lit one out i relit it") — his opening watch-
  // docket entry ("Lamps counted... 3 doused" / later "nine lit one out") is where a
  // player finds the rail count. The carving rakes visible only by the lit beacon-glow
  // after dark (read-by-time verb); the fire-count itself is countable in daylight.
  {
    puzzleKey: 'stone-brann-cipher',
    keeper: 'Brann',
    doc: 'D08 do-not-close-your-eyes-here',
    cipher: 'railfence',
    plaintext: 'COUNT THE FIRES BEFORE YOU SLEEP',
    acceptedAnswers: [
      'count the fires before you sleep',
      'nine lit one out i relit it',
      'the dark hours are kept by the last light',
    ],
    toSpec() {
      return { cipher: 'railfence', text: this.plaintext, rails: 9, namespace: this.puzzleKey };
    },
    decodeCiphertext(ct) {
      return railFence.decode(ct, 9);
    },
  },

  // ── stone-iss-wall · Vigenère, key = ISS ─────────────────────────────────
  // cipher-web §1 P3 / §3: the key is his own name. Turned on the other stones it
  // reads "the one who turned away" — the seed's primary answer + the name-as-key
  // catch. We bind the NAME-AS-KEY plaintext (the door), not the warm reading.
  {
    puzzleKey: 'stone-iss-wall',
    keeper: 'Iss',
    doc: 'D09 the-ways-are-a-wall',
    cipher: 'vigenere',
    plaintext: 'THE ONE WHO TURNED AWAY',
    acceptedAnswers: [
      'the one who turned away',
      'iss',
    ],
    toSpec() {
      return {
        cipher: 'vigenere',
        text: this.plaintext,
        key: 'ISS',
        namespace: this.puzzleKey,
      };
    },
    decodeCiphertext(ct) {
      return vigenere.decode(ct, 'ISS');
    },
  },
] as const;

/** Fast lookup by canonical puzzle_key. */
const BY_KEY: ReadonlyMap<string, ClueSpecEntry> = new Map(
  CLUE_SPECS.map((e) => [e.puzzleKey, e] as const),
);

// ---------------------------------------------------------------------------
// INTENTIONALLY EXCLUDED — active seed rows that are NOT Discord-decodable ciphers.
//
// These carry no carved cipher artifact, so they are NOT in the registry and must
// NEVER be forged/posted as a clue card. The showrunner drip pool (COHERENCE-AUDIT
// C3 / P0-7) excludes them or routes them to an in-world-pointing report. Each is
// tagged with WHY it is not a cipher node, so the exclusion is auditable.
// ---------------------------------------------------------------------------

export const NON_CIPHER_KEYS: Readonly<Record<string, string>> = {
  // found documents / in-world reports (no Discord-decodable carving):
  'm1-record-opens': 'found document — read on the group’s own base lectern (D01), not a carving',
  'm1-named-habit': 'in-world personalized report (the "scalpel"); dead-end texture, no cipher',
  // B2 MISMATCH (COHERENCE-AUDIT §2 SEAM B / P0-5): designed as Brann's beacon /
  // rail-fence night-gated cipher, but the LIVE seed row ships it as a flat
  // outcome_type:'lore' (FACT 11/12) with NO cipher and NO night gate. So today it is
  // NOT Discord-decodable and must NOT be forged. It graduates into CLUE_SPECS only
  // when B2/P0-5 re-authors it as a railFence node keyed on D08's fire-count (which
  // also un-orphans railFence, B1 PATH A). Excluded-but-flagged, not silently dropped.
  'stone-brann': 'B2: ships as flat lore today (no cipher / no night gate); not forgeable until P0-5 re-authors it as the railFence/beacon node',
  // observation / cross-reference (the world IS the puzzle; nothing to forge):
  'm2-rhyme': 'cross-reference observation — notice two stones rhyme; dead-end, no cipher',
  'seventh-shrine': 'count-the-markers observation + travel; side_quest payoff, no cipher carving',
  'iss-doubt': 're-read / key cross-check of OTHER stones; no own carved artifact',
  'iss-warm': 'the warm MISREADING of Iss’s stone (trusting his comfort) — routes to the dead-shrine grave; the interpretation of stone-iss-wall’s carving, not a separate carving. Puzzle-variety audit: now ALSO carries a real second decode (an acrostic — first letter of each of the six warm lines, down — spells "no wall"), hand-verifiable, no forge spec needed (see arc/corpus/cipher-plaintexts.md "stone-iss-wall — the second reading"); still non-cipher for CLUE_SPECS purposes because it is read by hand off the same carving, not forge-rendered separately',
  // travel / coordinate dead-end (a place, not a carved Discord card):
  'iss-dead-shrine': 'coordinate travel to a grave (dead_end); literal coords unsited (A7/G5)',
  // the literacy ROSETTA itself (it TEACHES the script; it is not carved IN it):
  'rosetta-ring': 'the rune-ring Rosetta (assembly ritual) — the master KEY, not a ciphered node',
  // re-walk / completion / atonement (in-world acts, not typed ciphers):
  'no-wall-catch': 'the Liar catch — a derived cross-document contradiction + re-walk, not a carving',
  'orin-threshold': 're-walk completing D07 in D04’s hand (atonement-bow); told fragment, no cipher',
  'haunting-biography': 'M4 keeper-NPC dialogue (FACT 9); told lore, no carving (NPC layer unbuilt, A8)',
  'atonement-refrain': 're-walk of a broken custom (conduct is the lock); detected, not a cipher',
  // Movement-III/V world progression + ritual + plugin sentinels (detected, not typed):
  'undercroft-descent': 'PERFORM Mara’s sentence (descend); world progression, plugin-driven beat',
  'undercroft-fog': 'witness the altar rebuild wrong; told lore set-piece, no carving',
  'rite-tokens': 'deposit named tokens in slots (ritual); detected world-state, not a cipher',
  'pressure-glyph-walk': 'walk the rune with footsteps (physical verb); no carved Discord card',
  'accepting-crouch': 'synchronized group bow; opaque plugin-posted sentinel, never a forged clue',
  'record-receives': 'the world’s response; opaque plugin sentinel, staged active=false until M5',
  'seventh-name': 'THE SEVENTH READING capstone — the name AVERYN, DERIVED from six distributed keeper fragments each in a different technique (seventh-reading.ts). No single carved card: the reading IS the puzzle. Saying the name triggers the release.',
  // --- OVERHAUL classification pass (2026-06-29): the 11 active rows specsCoverageSelfTest
  //     flagged UNCLASSIFIED. None carry a Discord-decodable cipher carving, so each is a
  //     non-cipher node (found document / in-world numeral-literacy / observation / in-world
  //     choice), classified here with its honest reason. See design/OVERHAUL.md §5.
  // in-world numeral-literacy teachers (twins of rosetta-ring; they TEACH a counting script,
  // they are not ciphered IN it) — the rune-numeral on-ramps:
  'a1z26-tick-stave': 'in-world numeral-literacy teacher (the tick-stave counting script); the runes-free twin of rosetta-ring, not a ciphered node',
  'reckoning-rosetta': 'in-world numeral rosetta (the reckoning-mark counting script); a literacy KEY, not a ciphered node',
  // found documents / carvings read in-world (true-but-inert, no Discord cipher to forge):
  'forged-eighth': 'the forged eighth ordinance — a found false document (dead_end); read in-world among the true seven, no cipher carving',
  'prophet-wall-comfort': 'the prophet wall’s warm promises — a found carving (dead_end); true-but-empty, no Discord cipher',
  'prophet-wall-name': 'the prophet wall’s name-reading — a found carving (dead_end); disambiguated to its own answers (the shared `the one who turned away` is owned by stone-iss-wall), no cipher',
  'name-where': 'place-filing cross-reference observation (FACT 16) — notice the record files by ground; dead_end, the world is the puzzle',
  'record-url': 'the founder line + Hold-copy handoff pointing off-world to Copperline service 1842, its owner post, and the recovered Record slug (A13/A14) - found lore, no raw server endpoint',
  'difficulty-mara': 'Mara’s “closer count of the quick” fragment — demoted to texture/lore (OVERHAUL §5: the difficulty REVEAL is cut; the line stays), not a deterministic cipher',
  'base-docket-reread-auto': 'the Hold-Book down-count re-read (A3), found-document lore gated on iss_caught via requires_flags; the showrunner-flipped twin base-docket-reread is retired (OVERHAUL §5)',
  // S3-A (archived puzzles-ciphers.md audit): ships active=false in puzzles_seed.sql (so a
  // base-seed-only parse never sees it), then metapuzzle_seed.sql flips it active=true once
  // iss_caught fires — a cross-file activation this file's own coverage guard structurally
  // couldn't see until parseSeedKeys/specsCoverageSelfTest also read metapuzzle_seed.sql's
  // activation UPDATEs (below). The row's own comment settles the classification: "NON-cipher
  // (plain lore, NON_CIPHER_KEYS)" — the fall-order acrostic lives in the carved FRAMING
  // glyphs (read by hand off the existing keeper-stone carvings), never a separately forged run.
  'meta-unkept': 'the fall-order acrostic re-read (framing glyphs, not the bound run); plain lore staged active=false then flipped true post-iss_caught by metapuzzle_seed.sql, no separate cipher to forge',
  // THE M4 / SEVENTH-DEEP CHAINS (S3-A, same blind spot as meta-unkept above) — all six ship
  // active=false in the base seed then get flipped true by metapuzzle_seed.sql's activation
  // UPDATEs once their upstream flag fires; none carries its own separately-forged Discord
  // cipher carving, so all six belong here rather than in CLUE_SPECS:
  'bound-word': 'the Iss Vigenère plaintext IS the coop-gate\'s need — derived from stone-iss-wall\'s carving after the catch (a re-walk/second-reading), not a separately forged cipher of its own',
  'm4-three-hands': 'the cross-surface co-op gate (A6) — CoopPlateListener posts an opaque, wordless AND-join conjunction token on a cleared gate; never human-typeable, no carving (no-leaked-sentinel)',
  'threshold-coordinate': 'the Threshold carving\'s decoded value is a navigation POINTER (INV-14); the typed answer is the clean destination WORD found on-site, never the signed coordinate — an on-site read, not a separately forged cipher',
  'true-walk-arrive': 'the true walk\'s destination WORD, carved on leaves placed at the on-site tableau; gated to on-site presence, no Discord carving',
  // seventh-unwriting's OWN seed comment still says "RAIL-FENCE... counted in-world on the
  // wall," but StructureTemplates.java's unwriting() build explicitly cut that (RESHAPE R0:
  // "label CUT entirely — scraped wall vs one clean slab + the stopped ceiling already carry
  // the full narrative; narration... spoils what should be the strongest room") — no literal
  // rail-fence carving exists anywhere to decode. The seed comment is stale (not re-audited
  // here beyond this classification note); the room's architecture is what's actually read.
  'seventh-unwriting': 'chamber 2 of the Seventh shrine — the room\'s architecture (the scraped wall, the one clean slab, the stopped ceiling) carries the narrative (RESHAPE R0 cut the literal carving the seed comment still describes); explored/read in-world, no forged cipher',
  'seventh-cause': 'pure told lore (outcome_type lore, oracleLore fragment) — the cause-fragment correlating the-fire-they-let-out with D11; no cipher of any kind',
  'seventh-choice': 'the restore-OR-erase choice (A1) — DETECTED IN-WORLD ONLY by SeventhChoiceListener\'s rite; two opaque, wordless tokens posted by the plugin on real detection, never human-typeable (no-leaked-sentinel)',
  // in-world permanence-fork CHOICES (A11) — a stated intent the world detects, not a typed cipher.
  // They set the M5 composer colorant flags (light_kept / name_unspoken); the detection mechanism
  // is a Phase-3 integration concern, not a forgeable carving:
  'fork-light': 'in-world permanence-fork choice (carry vs bank the kept light); side_quest setting an M5 colorant flag, no cipher',
  'fork-name': 'in-world permanence-fork choice (speak vs withhold the unspoken name); side_quest setting an M5 colorant flag, no cipher',
  // --- THE DIVERSE EXPANSION (design/PUZZLE-DESIGNS.md) — 17 new puzzles across 11 TYPE
  //     categories, none carrying a Discord-decodable letter-cipher carving (the 5 ciphers
  //     stay the minority baseline). TYPED rows (phrase/coords/url_token) are matched by the
  //     resolver but are observation / bearing / found-object reads, not forgeable ciphers;
  //     PLUGIN-PRODUCED rows (behavior/object/code/spoken) resolve via an in-world listener
  //     token, never a forged card. Each is classified with its honest reason + answer_kind.
  // Vaun (hoarder):
  'vaun-hoard-sorted': 'deposit the first-of-the-deep into the empty "given back" chest (answer_kind object); container-content check by a plugin producer, not a cipher',
  'vaun-bookshelf-tally': 'chiseled-bookshelf comparator register (answer_kind code); a lock the plugin reads, not a Discord cipher',
  // Mara (reader who never walked):
  'mara-lectern-lock': 'five-lectern comparator page-lock (answer_kind code); a plugin-read combination, not a cipher',
  'mara-walk-the-map': 'group-bow at the marker row with the active roster (answer_kind behavior); detected, not a cipher',
  // Sella (drowned child):
  'sella-reflection-bearing': 'a bearing read only in the water reflection → the far-water destination WORD (answer_kind coords, INV-14); on-site read, not a cipher',
  'sella-overlay-lake': 'RETHEMED (puzzle-variety audit, was a template repeat with its two neighbors): five lecterns holding Sella’s loose copybook pages, set to the page-numbers her ring-drawings tally (answer_kind code); a comparator lock read by LecternLockListener (the same producer as mara-lectern-lock), not a cipher',
  'sella-shore-memorial': 'map-art forced-perspective bird-glyph seen from one anchor block (answer_kind behavior); stand-at-anchor detection, not a cipher',
  // Orin (mason who would not bow):
  'orin-bow-fall-order': 'ordered crouch-at-marker sequence in fall-order (answer_kind behavior); detected, not a cipher',
  'orin-banner-heraldry': 'the mason-square banner sigil is the KEY that unlocks Orin’s EXISTING substitution stone (answer_kind phrase); key-delivery, not a new cipher',
  'orin-frame-dials': 'six item-frame rotation dials matched to the markers’ facings (answer_kind code); a plugin-read rotation lock, not a cipher',
  // Brann (watchman on the black moon):
  'brann-black-moon-toll': 'a morse toll that rings only on the in-game black moon (answer_kind phrase, temporal-gated by the producer); the plaintext is foreshadowed, not a Discord cipher',
  'brann-silence-corridor': 'a calibrated-sculk corridor passed in silence (answer_kind behavior); traversal detection, not a cipher',
  // Iss (the liar):
  'iss-which-is-true': 'cross-check the warm wall-doctrine against the land — a deduction (answer_kind phrase); derived contradiction, no own carving',
  'iss-nbt-falsified-entry': 'a doctored record entry hidden in item NBT → a record-website path token the group corrects (answer_kind url_token); datamine/found-object, not a Discord cipher',
  'iss-bound-word-callback': 're-submit the earned bound word at the deep gate (answer_kind phrase, a callback); recognition of an earned answer, not a new cipher',
  // Cross-keeper / spine:
  'prior-absence': 'failed-accepting roster deduction (answer_kind phrase); found-record condition, not a Discord cipher',
  'prior-camp-refusal': 'failed prior camp deduction (answer_kind phrase); distinguishes solved answers from witness, not a cipher',
  'prior-vaun-correction': 'prior-run Vaun correction file (answer_kind phrase); evidence synthesis across camp + market/ration proof, not a cipher',
  'prior-mara-correction': 'prior-run Mara correction file (answer_kind phrase); action-vs-record deduction, not a cipher',
  'prior-sella-correction': 'prior-run Sella correction file (answer_kind phrase); seventh/evidence deduction, not a cipher',
  'prior-orin-correction': 'prior-run Orin correction file (answer_kind phrase); posture/proof deduction, not a cipher',
  'prior-brann-correction': 'prior-run Brann correction file (answer_kind phrase); temporal duty deduction, not a cipher',
  'prior-iss-correction': 'prior-run Iss correction file (answer_kind phrase); cross-check comfort against land, not a cipher',
  'prior-witness-before-accepting': 'failed accepting synthesis (answer_kind phrase); names the final pre-rite condition, not a cipher',
  'media-prior-base': 'found-footage frame/audio/metadata payload ASH-13 (answer_kind phrase); external media observation, not a Discord cipher',
  'media-far-water': 'found-footage place phrase "where the reeds fold back" (answer_kind phrase); optional Sella media evidence, not a Discord cipher',
  'media-black-moon-toll': 'found-footage toll rhythm "stay awake" (answer_kind phrase); optional Brann media evidence, not a Discord cipher',
  'media-release-room': 'late found-footage release-room checksum "six return one is not kept" (answer_kind phrase); optional media evidence, not a Discord cipher',
  'spine-recovered-archive': 'a salvaged Drive folder + audio-spectrogram sentence "i was not kept" (answer_kind phrase); external research, not a Discord cipher',
  'spine-threshold-vault': 'asymmetric co-op vault; per-player rune fragments assemble a combination (answer_kind code); a plugin illusion + vault, not a cipher',
  'spine-spoken-name': 'the Observer Engine hears the spoken truth and quotes it back (answer_kind spoken); voice-scan token, not a cipher',
  'spine-unkept-acrostic': 'the six maker’s-mark glyphs read in fall-order spell UNKEPT (answer_kind phrase); an in-world observation acrostic in the carved framing, not a bound cipher',
  'spine-cold-hearth-shadow': 'notice the one cold hearth in a Hold where every fire is kept (answer_kind phrase); observation dead_end (the false walk), the world is the puzzle',
};

// ---------------------------------------------------------------------------
// forgeForPuzzle — the public bind. Consumed by BOTH the world-placement step and
// the showrunner drip so a node renders ONE plaintext identically on both surfaces.
// ---------------------------------------------------------------------------

export class UnknownPuzzleKeyError extends Error {
  constructor(public readonly puzzleKey: string) {
    super(
      NON_CIPHER_KEYS[puzzleKey]
        ? `forgeForPuzzle: "${puzzleKey}" is a non-cipher node (${NON_CIPHER_KEYS[puzzleKey]}); ` +
            `it has no forgeable clue — route it to an in-world report, do not post a card.`
        : `forgeForPuzzle: no clue-spec registered for "${puzzleKey}".`,
    );
    this.name = 'UnknownPuzzleKeyError';
  }
}

/** True iff this authored key is a registered, forgeable cipher node. */
export function hasClueSpec(puzzleKey: string): boolean {
  return BY_KEY.has(puzzleKey);
}

/** The registered entry for a key, or undefined (no throw — for callers that branch). */
export function clueSpecFor(puzzleKey: string): ClueSpecEntry | undefined {
  return BY_KEY.get(puzzleKey);
}

/**
 * Forge the clue artifact for an authored cipher node.
 *
 * THROWS {@link UnknownPuzzleKeyError} for any key not in the registry — including
 * the intentionally-excluded non-cipher rows (with a message pointing the caller to
 * route it to an in-world report instead of posting a forged card). Callers that may
 * be handed a non-cipher key should guard with {@link hasClueSpec} first; the
 * showrunner drip already filters to forgeable keys (C3).
 *
 * Deterministic + pure: same key → same ForgedClue forever (forgeClue is pure).
 */
export function forgeForPuzzle(puzzleKey: string): ForgedClue {
  const entry = BY_KEY.get(puzzleKey);
  if (!entry) throw new UnknownPuzzleKeyError(puzzleKey);
  return forgeClue(entry.toSpec());
}

/** All registered, forgeable cipher puzzle_keys (stable order = registry order). */
export function forgeablePuzzleKeys(): readonly string[] {
  return CLUE_SPECS.map((e) => e.puzzleKey);
}

// ---------------------------------------------------------------------------
// BUILD-TIME INVARIANT — the carving and the seed can never drift.
//
// For every registry entry, assert:
//   (1) decode(forge(spec).ciphertext) === forge(spec).solution   (the carved runes
//       really decode back to the bound plaintext — no shift/key/book mistake);
//   (2) the forge solution equals plaintext in forge-canonical (uppercase) form;
//   (3) normalizeAnswer(plaintext) ∈ entry.acceptedAnswers          (the player's
//       decoded answer actually matches the seed row — the bind that closes A3/B5).
//
// Also exposed via the dedicated specs.selftest.ts so it runs in the same harness as
// forgeSelfTest. THROWS on any violation (build fails, not the player).
// ---------------------------------------------------------------------------

export function specsSelfTest(): { passed: number; cases: string[] } {
  const cases: string[] = [];

  for (const entry of CLUE_SPECS) {
    const forged = forgeForPuzzle(entry.puzzleKey);
    const canonical = entry.plaintext.toUpperCase();

    // (2) forge.solution is the forge-canonical plaintext.
    if (forged.solution !== canonical) {
      throw new Error(
        `specsSelfTest [${entry.puzzleKey}]: forge.solution "${forged.solution}" ` +
          `!== plaintext "${canonical}"`,
      );
    }

    // (1) the carved ciphertext decodes back to that plaintext.
    const decoded = entry.decodeCiphertext(forged.meta.cipherText);
    if (decoded !== canonical) {
      throw new Error(
        `specsSelfTest [${entry.puzzleKey}]: decode(forge.ciphertext) "${decoded}" ` +
          `!== plaintext "${canonical}" (cipher=${entry.cipher})`,
      );
    }

    // (3) the player's normalized plaintext is in the seed row's accepted_answers.
    const norm = normalizeAnswer(entry.plaintext);
    if (!entry.acceptedAnswers.includes(norm)) {
      throw new Error(
        `specsSelfTest [${entry.puzzleKey}]: normalizeAnswer(plaintext) "${norm}" ` +
          `is NOT in accepted_answers [${entry.acceptedAnswers.join(' | ')}] — ` +
          `the carving would decode to an answer the seed rejects (drift).`,
      );
    }

    // Every accepted_answers mirror entry must itself be pre-normalized (ORACLE.md §2),
    // matching seedcheck.ts — so this registry can't drift from the seed's own rule.
    for (const a of entry.acceptedAnswers) {
      if (normalizeAnswer(a) !== a || a.trim() === '') {
        throw new Error(
          `specsSelfTest [${entry.puzzleKey}]: accepted_answers entry "${a}" is not ` +
            `pre-normalized/non-empty (ORACLE.md §2).`,
        );
      }
    }

    // The forged artifact must render to a carveable <g> fragment (no uncarvable glyph).
    if (!forged.svg.startsWith('<g')) {
      throw new Error(
        `specsSelfTest [${entry.puzzleKey}]: forged clue did not render a <g> fragment.`,
      );
    }

    cases.push(
      `${entry.puzzleKey} (${entry.cipher}): forge↔decode↔seed bound — ` +
        `"${canonical}" → carves → decodes → matches "${norm}"`,
    );
  }

  // Disjointness guard: a key is either a forgeable cipher node OR an excluded
  // non-cipher row, never both (catches a row mis-classified in two places).
  for (const key of forgeablePuzzleKeys()) {
    if (key in NON_CIPHER_KEYS) {
      throw new Error(
        `specsSelfTest: "${key}" is BOTH a registered cipher spec and in NON_CIPHER_KEYS.`,
      );
    }
  }
  cases.push(`registry covers ${CLUE_SPECS.length} cipher nodes; ${Object.keys(NON_CIPHER_KEYS).length} rows excluded as non-cipher`);

  return { passed: cases.length, cases };
}

// ---------------------------------------------------------------------------
// COVERAGE INVARIANT — every active seed row is CLASSIFIED (forgeable OR excluded).
//
// `specsSelfTest` proves the rows we DID classify don't drift. This proves we did not
// LEAVE ONE OUT: every `active=true` row in puzzles_seed.sql must be either a
// registered cipher spec or in NON_CIPHER_KEYS. An unclassified row is the silent gap
// that let stone-brann (COHERENCE-AUDIT B2) fall through both lists — the showrunner
// drip pool (C3) then can't tell whether to forge it or route it. This makes that a
// BUILD failure, not a runtime ambiguity.
//
// Pure: takes the seed SQL as a string (the standalone runner reads the file), so the
// forge harness stays I/O-free. Best-effort SQL parse — it only needs the leading
// `( 'puzzle_key', 'title', ...)` of each row + the trailing `active` boolean; the
// resolver/seed remain the source of truth.
// ---------------------------------------------------------------------------

/** Parse the rows of puzzles_seed.sql, returning `{ active, all }` puzzle_key sets
 *  (the leading authored key of each row + its trailing positional `active` boolean). */
export function parseSeedKeys(seedSql: string): { active: string[]; all: string[] } {
  // Strip line comments so a `--` mention of a key never parses as a row.
  const sql = seedSql.replace(/--[^\n]*/g, '');
  // Each row: ( 'key', 'title', array[...], 'outcome', jsonb..., movement, active, max )
  // We anchor on the key, then find this row's `, true,` / `, false,` (the active flag is
  // the boolean just before the trailing max_attempts). Match per-row by slicing to the
  // next row's opening `( '` or the closing `on conflict`.
  const active: string[] = [];
  const all: string[] = [];
  // A ROW opener is `( 'key', 'title', array[` — the only place a quoted key is
  // immediately followed by a quoted title then `array[`. Nested
  // `jsonb_build_object('fragment', …)` calls never have that shape, so this anchors on
  // real rows only (no preceding-identifier ambiguity).
  const keyRe = /\(\s*'([a-z0-9-]+)'\s*,\s*'(?:[^']|'')*'\s*,\s*array\s*\[/g;
  const starts: { key: string; idx: number }[] = [];
  let m: RegExpExecArray | null;
  while ((m = keyRe.exec(sql))) starts.push({ key: m[1]!, idx: m.index });
  for (let i = 0; i < starts.length; i++) {
    const s = starts[i]!;
    const end = i + 1 < starts.length ? starts[i + 1]!.idx : sql.length;
    const body = sql.slice(s.idx, end);
    // The row tail is `… , <movement int> , <active bool> , <max: null|int> )`. Take the
    // LAST such trailing triple in the row body so a `('flag', true)` INSIDE a set_flags
    // payload can never be mistaken for the row's own active column.
    const tails = [...body.matchAll(/,\s*\d+\s*,\s*(true|false)\s*,\s*(?:null|\d+)\s*\)/g)];
    if (tails.length === 0) {
      throw new Error(
        `parseSeedKeys: could not find the trailing (movement, active, max) tuple ` +
          `for row "${s.key}" — the seed row shape changed; update the parser.`,
      );
    }
    all.push(s.key);
    if (tails[tails.length - 1]![1] === 'true') active.push(s.key);
  }
  return { active, all };
}

/**
 * S3-A FIX (archived `design/archive/audit/puzzles-ciphers.md`): `parseSeedKeys` only sees a row's
 * OWN literal `active` boolean in `puzzles_seed.sql` — it has no notion of a LATER file flipping
 * that row active at apply time. `metapuzzle_seed.sql` does exactly that (`update public.puzzles
 * set active = true where puzzle_key in (...)`, e.g. for `meta-unkept` once `iss_caught` fires) —
 * a row shipped inactive in the base seed becomes genuinely active in the live database, but the
 * coverage guard that's supposed to catch "an active row nobody classified" never saw it happen.
 * This parses those activation UPDATEs (best-effort, matches this repo's one established idiom:
 * `where puzzle_key in (...)` / `where puzzle_key = '...'`) so `specsCoverageSelfTest` can union
 * them into its active set — closing the blind spot instead of leaving it open for the next
 * cross-file-activated row to slip through unnoticed the same way `meta-unkept` did.
 */
export function parseActivationUpdates(metapuzzleSql: string): string[] {
  const sql = metapuzzleSql.replace(/--[^\n]*/g, '');
  const keys = new Set<string>();
  const updateRe = /update\s+public\.puzzles\s+set\s+active\s*=\s*true\s+where\s+puzzle_key\s+(in\s*\(([^)]*)\)|=\s*'([a-z0-9-]+)')/gi;
  let m: RegExpExecArray | null;
  while ((m = updateRe.exec(sql))) {
    const list = m[2] ?? m[3];
    if (!list) continue;
    for (const km of list.matchAll(/'([a-z0-9-]+)'/g)) keys.add(km[1]!);
  }
  return [...keys];
}

export function specsCoverageSelfTest(
  seedSql: string,
  activationSql?: string,
): { passed: number; cases: string[] } {
  const { active: parsedActive, all: allKeys } = parseSeedKeys(seedSql);
  // Union in any cross-file runtime activations (S3-A) so a row that's inactive in the base
  // seed but flipped active by metapuzzle_seed.sql is treated as active here too.
  const activatedElsewhere = activationSql ? parseActivationUpdates(activationSql) : [];
  const activeKeys = [...new Set([...parsedActive, ...activatedElsewhere])];
  if (activeKeys.length === 0) {
    throw new Error('specsCoverageSelfTest: parsed 0 active rows from the seed — parser or seed broke.');
  }
  const forgeable = new Set(forgeablePuzzleKeys());
  // Every ACTIVE row must be classified: forgeable cipher OR explicitly excluded.
  const unaccounted = activeKeys.filter((k) => !forgeable.has(k) && !(k in NON_CIPHER_KEYS));
  if (unaccounted.length > 0) {
    throw new Error(
      `specsCoverageSelfTest: ${unaccounted.length} active seed row(s) are UNCLASSIFIED ` +
        `(neither a registered cipher spec nor in NON_CIPHER_KEYS): ${unaccounted.join(', ')}. ` +
        `Add each to CLUE_SPECS (if it carries a Discord cipher) or NON_CIPHER_KEYS (with a reason).`,
    );
  }
  // Stale exclusion: a NON_CIPHER_KEYS entry for a key that is NOT IN THE SEED AT ALL
  // (active or inactive). Inactive rows (e.g. record-receives, staged for M5) legitimately
  // stay excluded, so we compare against ALL seed keys, not just active ones.
  const allSet = new Set(allKeys);
  const stale = Object.keys(NON_CIPHER_KEYS).filter((k) => !allSet.has(k));
  if (stale.length > 0) {
    throw new Error(
      `specsCoverageSelfTest: NON_CIPHER_KEYS lists ${stale.length} key(s) absent from the ` +
        `seed entirely (stale exclusion): ${stale.join(', ')}. Remove them or re-add the row.`,
    );
  }
  // A forgeable cipher key must also be a REAL active seed row (the registry can't bind a
  // node the seed doesn't carry).
  const activeSet = new Set(activeKeys);
  const phantom = [...forgeable].filter((k) => !activeSet.has(k));
  if (phantom.length > 0) {
    throw new Error(
      `specsCoverageSelfTest: ${phantom.length} registered cipher spec(s) have no active seed ` +
        `row: ${phantom.join(', ')}. Seed the row or remove the spec.`,
    );
  }
  return {
    passed: 1,
    cases: [
      `coverage: all ${activeKeys.length} active seed rows classified ` +
        `(${forgeable.size} forgeable cipher + ${activeKeys.filter((k) => k in NON_CIPHER_KEYS).length} active excluded; ` +
        `${Object.keys(NON_CIPHER_KEYS).length} excluded total incl. staged-inactive); 0 unaccounted, 0 stale, 0 phantom`,
    ],
  };
}
