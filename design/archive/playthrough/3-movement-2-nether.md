# 3 — MOVEMENT II (The Ways) + the NETHER lane — the literal shooting-script

> **What this file is.** The ordered, literal "what is there" for MOVEMENT II — the six
> keeper-stone field, the two Rosettas, the meta-acrostic plant, the Whisper economy going
> live, every cross-surface handoff — followed by the optional NETHER lane. Every element is
> sourced from a REAL repo artifact, quoted. `[GAP — TO BUILD]` marks anything not yet a real
> artifact (a null-coord site placement, a voice key not yet in `voice.ts`/`voice.archive.ts`,
> a dashboard console action, a world-build schematic). Director actions are flagged
> **DIRECTOR ACTION**. This is the continuity script the editor reviews; it does not invent.
>
> **Sources of truth read for this segment (all quoted below where load-bearing):**
> `design/WEB-MASTER.md` §1.MII/§2/§3/§9; `discord/supabase/seeds/puzzles_seed.sql` (the six
> stones + back-half rows); `discord/supabase/seeds/progression_seed.sql` (Nether/End);
> `discord/supabase/seeds/seventh_seed.sql`; `discord/supabase/seeds/side_quests.sql`;
> `arc/corpus/cipher-plaintexts.md` (the bound plaintexts + carved framing + hands);
> `arc/lore/documents/*` (the keeper journals, verbatim); `discord/src/voice.ts` (the
> Watcher register + the oracle/dead-end/whisper lines, verbatim); `plugin/.../sites.yml`;
> `plugin/.../config.yml` (the drama/difficulty budget); `design/structures.md` (build
> palette + footprint); `design/content/npc-dialogue.md` (the human surface NPCs).

---

## 0. ENTRY STATE — what is already true when MOVEMENT II opens

Carried from Movement I (`2-movement-1.md`). MII is **gated on**: *"first report found +
group-playtime threshold"* (WEB-MASTER §1.MI, "Gate 1→2") AND literacy. By the time the
field is live the group has — or can have — these flags set in `arc_state`:

- `rosetta_known` — set by **either** `rosetta-ring` (icon-ring metadata leap) **or**
  `a1z26-tick-stave` (the runes-free number door). Both seed `set_flags {rosetta_known:true}`
  (puzzles_seed.sql L92, L578). This is the literacy that makes every MII carving readable.
- `reckoning_known` — set by `reckoning-rosetta` (puzzles_seed.sql L1028), the **second
  Rosetta** (digit-glyphs + the `-`/`,` sign marks). *"makes coordinates into PLACES"*
  (WEB-MASTER §2 table). Coord-bearing rows stay inactive until `stone_of_reckoning` is placed.
- `prologue_ignited` — set in M1; the curatorial clue-drip is no longer suppressed.

**WHO this targets / gating:** the whole active group. No per-player gate in MII except the
night-gate on Brann's stone (below) and `reckoning_known` on coordinate readings.

**DIRECTOR ACTION (setup, one-time, pre-MII):** confirm the two literacy flags are reachable
on the dashboard health panel before opening the field. The drama budget that governs MII's
pacing is the deterministic gate block in `config.yml` (L50, "DRAMA BUDGET + COOLDOWNS — the
L4D-style director — pure deterministic gates"); no per-beat console click is required to run
the field — it is self-pacing. `[GAP — TO BUILD]` the dashboard health-panel view that
surfaces `rosetta_known`/`reckoning_known` is referenced (config.yml L255 "the dashboard
health-panel reads") but the panel itself is not in this repo slice.

---

## 1. THE SIX-STONE FIELD — a field, not a row (any order; resolver ignores movement)

**WHAT IT IS (the field rule).** Six keeper-stones, each a `keeper_stone` answer-site. The
non-linearity is real: per `sites.yml` L82, omitting `puzzle-key:` on a stone means *"the sign
then resolves against ALL open puzzles, so any clue a group has solved can be answered at any
keeper-stone."* The answer-verb is the same on every stone — **edit a sign within the stone's
radius**; the `AnswerSignListener` *"reads the typed lines, normalizes them (the exact
ORACLE.md §2 algorithm — identical to the Discord surface), and matches against the OPEN
`puzzles` rows… on a miss the sign simply blanks, no error, no hint"* (sites.yml L77–82).

**WHERE.** Six sites in `sites.yml`, all `type: keeper_stone`, `enabled: true`, coords `null`:
`stone_vaun` `stone_mara` `stone_sella` `stone_orin` `stone_brann` `stone_iss` (L143–201).
Per `structures.md` L16 they are *"scattered through the Warrens/Market/Lamp-works, mid Hold."*

**BUILD PALETTE / FOOTPRINT (structures.md L25–29, the keeper-stone spec):**
> **Form:** a single floor-set or low-canted slab (deepslate/polished blackstone), ~3×4
> blocks, **angled or low enough that the camera must tilt down to read it** (the bow built
> in). One soul-lantern at a fixed offset so it's *legible only by stooping into its light*.

Theme law (structures.md L5): *"carved stone, not cobble… deepslate / tuff / polished basalt /
blackstone… oxidized copper + soul-lantern light… Runes are signs/lecterns/text-displays in
the `observance:runes` font (resource pack), never hand-placed blocks."*

**`[GAP — TO BUILD]`** every stone's `x/y/z` is `null` in sites.yml ("UNPLACED until the world
is built in-game" — a GO-LIVE step, structures.md §GO-LIVE). The plugin silently skips an
unplaced site. The `.schem` exports and the `observance:runes` resource-pack font are likewise
GO-LIVE. The carving on each stone is *"a sign / hanging-sign / text-display in
`observance:runes` carrying that keeper's cipher (the bound plaintext from `clue-specs.ts`)"*
(structures.md L27) — the forge round-trip is wired but the in-world carving is a build step.

The six are now given **in fall-order** (the meta-acrostic key order — see §3), because that is
the causal order the cold-Iss reveal later names. Within MII the player may hit them in ANY order.

---

### 1.1 STONE-VAUN — caesar (shift 3) — `give the first of the deep back to the deep`

- **CIPHER KIND:** Caesar, `shift = 3`. *"The shift IS his hoarding — 'i had three of each.'"*
  (cipher-plaintexts.md L72).
- **BOUND PLAINTEXT (do not change):** `GIVE THE FIRST OF THE DEEP BACK TO THE DEEP`
  (cipher-plaintexts.md L78). Carved ciphertext = this run Caesar-shifted +3.
- **THE CARVED CLUE (framing, plain script, verbatim — cipher-plaintexts.md L88–95):**
  - above, a tally header: `IRON THREE · SALT THREE · GRAIN THREE · OF THE DEEP THREE · I HELD THREE OF EACH AND THE COUNTING WAS WARM`
  - below: `I LEARNED THE COUNT I DID NOT MAKE IT · THE LAND COUNTS FIRST · I KEEP THE TALLY AFTER`
  - maker's mark at the foot: *"a vertical rule cut deep with nothing carved beside it"* — D02's
    blank "given-back" column made physical. **This blank column is the UNKEPT maker's-mark for
    Vaun** (§3) and the literal plant of the give-back column re-read at V (ledger #5).
- **ACCEPTED ANSWER(S):** `give the first of the deep back to the deep` · `the land counts first`
  · `i counted them in the dark and gave none back` (puzzles_seed.sql L114–118).
- **ANSWER-VERB:** edit the sign at `stone_vaun` (caesar → "rotate the wheel three marks back").
- **OUTCOME / REVEAL BEAT:** `outcome_type: lore` (pure, opens no door). Watcher speaks
  `oracleLore(fragment)` = verbatim *"vaun counted everything and gave nothing back. the
  given-back column of his ledger is blank. the land counts first, and it had already counted
  him."* (puzzles_seed.sql L122). FACT 5 + FACT 4.
- **SOURCE DOC:** `counted-them-in-the-dark.md` (D02).
- **HAND (for the build):** *"Deepest cut of the five and the most regular, but not graceful:
  square, heavy, hammered straight… numbers cut deeper than the letters"* (cipher-plaintexts.md
  L100). Lit plainly, head-on, daylight-equivalent (L109).

### 1.2 STONE-MARA — book cipher (the six-book shelf) — SPINE KEY → the descent

- **CIPHER KIND:** book cipher. The carved artifact is the **ref-string** (page-line-word
  triples); the codebook is the six-book lectern shelf at `kept_light_home_01`
  (cipher-plaintexts.md L117–119).
- **BOUND PLAINTEXT (do not change):** `DESCEND AND BOW AT THE UNBROKEN LIGHT`
  (cipher-plaintexts.md L124).
- **BOUND CARVED REF-STRING (digit glyphs, verbatim):** `1-1-1   2-1-1   2-1-2   2-1-5
  1-1-2   3-1-2   3-1-3` (cipher-plaintexts.md L139). Reads as a column of numbers — *"it
  looks like a finding-list, not a sentence."*
- **THE CARVED CLUE (framing, verbatim — cipher-plaintexts.md L151–157):**
  - header: `READ THE SHELF IN ORDER LEFT TO RIGHT · FROM EACH BOOK THE PAGE THEN THE LINE THEN THE WORD · SET THE WORDS YOU FIND IN A ROW`
  - footer (the teeth): `WHEN YOU HAVE SET THE WORDS DO NOT CARVE THE SENTENCE ON A SIGN AND THINK IT KEPT · DO THE THING IT TELLS YOU · GO WHERE IT SENDS AND THERE DO AS IT SAYS · NONE LEFT AT THE DOOR`
  - maker's mark, faint, in the corner: `I NEVER WENT DOWN · I ONLY EVER READ THE WAY DOWN`
    (Mara's grammatical fingerprint: referential/deferred — she read every rite, performed none).
- **ACCEPTED ANSWER:** `descend and bow at the unbroken light` — *"the Oracle accepts the
  ASSEMBLED SENTENCE, not triples"* (puzzles_seed.sql L128).
- **ANSWER-VERB:** walk the shelf, assemble the sentence, edit the sign at `stone_mara`.
- **OUTCOME / REVEAL BEAT:** `outcome_type: next_clue` — `oracleNextClue()` = verbatim *"kept.
  the way goes on — look where the marks were not, before."* (voice.ts L235). Sets
  `mara_read:true`; `next_puzzle_key: undercroft-descent` (puzzles_seed.sql L137). **This is the
  spine in-road to Movement III** (the Undercroft). FACT 5 + FACT 13 seed.
- **HAND:** *"Small, very even, fast… the number-column laid out with real care… her framing is
  longer than her cipher"* (cipher-plaintexts.md L163). Warmest, lowest, steadiest light in the
  Hold — *"the only thing in this room still lit"* (L177).

### 1.3 STONE-SELLA — atbash/mirror + bearing → the Seventh side-quest

- **CIPHER KIND:** Atbash (A↔Z, involution). *"reads as folded nonsense until faced to the
  water"* — the verb is physical (cipher-plaintexts.md L181).
- **BOUND PLAINTEXT (do not change):** `SOUTH BY THE FAR WATER WHERE SHE DID NOT COME BACK`
  (cipher-plaintexts.md L187).
- **THE CARVED CLUE (framing, verbatim — cipher-plaintexts.md L199–205):**
  - above: `I WRITE IT THE ONLY WAY IT READS · BACKWARDS TO THE WATER THE WAY THE LAKE GIVES MY FACE BACK`
  - below: `THE SURFACE KEEPS EVERYTHING · IT TOOK THE LOOKING AND FOLDED IT AND GAVE IT BACK WRONG · I HAVE THE FAR WATER IN MY MOUTH`
  - the seventh-thread seed, smallest: `DO NOT LET THE COUNT BE SIX ONLY · COUNT AGAIN AT THE SHORE · THE LAST MARKER IS NOT THE LAST`
- **ACCEPTED ANSWER(S):** `south by the far water where she did not come back` · `south by the
  far water` · `the last marker is not the last` (puzzles_seed.sql L146–149). Coord-tolerant:
  bearing words AND the unsigned far-water coordinate.
- **ANSWER-VERB:** read the mirrored run **in the lake's reflection** at `the_far_water`, then
  edit the sign. Build: *"the player has to stand BEHIND it and read the reflection in still
  water… the reflection should be the only place the run resolves"* (cipher-plaintexts.md L226).
- **OUTCOME / REVEAL BEAT:** `outcome_type: side_quest` — `oracleSideQuest()` = verbatim *"this
  is not the way. but it is a way. follow it, if you would."* (voice.ts L278). Sets
  `seventh_suspected:true`; `next_puzzle_key: seventh-shrine` (puzzles_seed.sql L153). **This is
  in-road A to the Seventh thread.** FACT 5 + seeds the Seventh.
- **HAND:** a child's carving, *"glyphs of uneven height… cut left-handed / mirror-wrong even
  before the Atbash"* (cipher-plaintexts.md L210). Dim, blue, wet light.

### 1.4 STONE-ORIN — substitution + crouch-only reveal → the Threshold (M4)

- **CIPHER KIND:** monoalphabetic substitution (the rune alphabet itself). *"solving it is the
  literacy… The verb is physical: the carving faces the floor, legible only from a crouch."*
  (cipher-plaintexts.md L234–238). The crouch is the Bow — *"To read the keeper who would not
  bow, you bow."* (L267).
- **BOUND PLAINTEXT (do not change):** `I THOUGHT IT SMALL IT WAS NOT SMALL`
  (cipher-plaintexts.md L242).
- **THE CARVED CLUE (framing, verbatim — cipher-plaintexts.md L255–265):**
  - standing height: `I PASSED THE MARKERS STANDING · THE REST IS CUT LOW · STOOP TO READ IT OR READ NOTHING`
  - the run ends mid-thought: `... I THOUGHT IT SMALL IT WAS NOT SMALL I —` *(the cut stops.
    nothing follows. the stone is broken away.)* — this **broken "i —" is the plant for FACT 6
    and the `E` glyph of UNKEPT** (WEB-MASTER §9 ledger #3).
  - lowest, the margin hand: `THRESHOLD` *(and: FOR THE REST OF THE SENTENCE GO TO WHERE I WAS LEFT.)*
- **ACCEPTED ANSWER(S):** `i thought it small it was not small` · `threshold` · `the bow is the
  smallest of the ways` (puzzles_seed.sql L163–166).
- **ANSWER-VERB:** crouch under the lintel at `stone_orin` to read the floor-facing run; edit
  the sign. Build: *"you read it bent double under the lintel… let the lintel do it, not a
  prompt"* (cipher-plaintexts.md L258, L268).
- **OUTCOME / REVEAL BEAT:** `outcome_type: next_clue` — `oracleNextClue()` (as §1.2). No flag
  set; `next_puzzle_key: orin-threshold` (the M4 completion node, puzzles_seed.sql L170).
- **HAND:** *"the most technically perfect cut of the five… But cold… the last stroke is
  incomplete — a stave begun and not finished"* (cipher-plaintexts.md L271–278).

### 1.5 STONE-BRANN — flat lore today; the railFence night-stone is STAGED

There are **two** Brann rows; only one is live in MII.

- **LIVE: `stone-brann` (`outcome_type: lore`, active=true).** No cipher, night/black-moon
  framing only. **ACCEPTED ANSWER(S):** `one fire was never doused` · `do not close your eyes
  here` · `the one fire that will not be doused` (puzzles_seed.sql L178–181). REVEAL:
  `oracleLore` = verbatim *"count the fires at night. one of them never went out, and no hand
  tends it. they had one word for the people and the flame and the cold stone. do not close
  your eyes here."* (puzzles_seed.sql L186). FACT 11 + FACT 12. Source doc:
  `do-not-close-your-eyes-here.md` (D08), Brann's hand, *"read him only at night"* (D08 L19).
- **THE NETHER INERT SEED RIDES THIS STONE'S FRAMING (WEB-MASTER §1.MII):** *"Brann's stone
  (night-gated) carries, in its carved framing (never a bound plaintext)… a watchman's line —
  'the fire we keep is not ours. it is lent… below the below.' Inert texture; a riddle with no
  door yet."* This is **ledger plant #28** (WEB-MASTER §9). It is the M2 plant the Nether lane
  pays off in M3/M4. `[GAP — TO BUILD]` this exact framing line is specified in WEB-MASTER but
  is **not yet carved into a seed row or `cipher-plaintexts.md`** — it is framing-only,
  carried at world-build on `stone_brann`. It must NOT become a bound plaintext (it would trip
  the X1 round-trip guard).
- **STAGED `[GAP — activation]`: `stone-brann-cipher` (active=FALSE, puzzles_seed.sql L1081).**
  The real railFence node — *"railFence (rails = the fire-count Brann names in D08), the verb
  is read-by-time (the carving rakes visible only by the lit beacon-glow after dark)."* BOUND
  PLAINTEXT (when built): `count the fires before you sleep` (L1058). It ships staged until
  **TS-FORGE adds the railFence `CLUE_SPECS` entry + removes `stone-brann` from
  `NON_CIPHER_KEYS`** (the cross-owner dependency, L1050–1055). Until then *"do not forge it"*
  (cipher-plaintexts.md L360). Its `next_puzzle_key: undercroft-descent` makes it the **second
  in-road to the descent** (the web rule). REVEAL when active: relabel beat on `stone_brann`,
  text *"nine lit one out i relit it i will count again before i sleep"* (L1077).

### 1.6 STONE-ISS — vigenère (key = ISS) — the warmest stone (the trap)

This stone is the head of the whole Liar thread. It has **two readings → two rows.**

- **CIPHER KIND:** Vigenère, key `ISS` — *his own name.* *"applied warmly to his own letter the
  doctrine reads comforting; the key turned on the other stones reads the word the corpus keeps
  for him"* (cipher-plaintexts.md L288).
- **BOUND PLAINTEXT (do not change):** `THE ONE WHO TURNED AWAY` (cipher-plaintexts.md L298).
- **THE CARVED CLUE (framing, verbatim — cipher-plaintexts.md L312–320):**
  - opening, warmest carving in the Hold: `BE EASY · THE STONES FRIGHTENED YOU AND THAT IS THE STONES FAULT NOT YOURS · GRIEF CARVES CROOKED · LET ME SET IT STRAIGHT THE WAY A FRIEND WOULD`
  - the planted lie (FACT 7): `THE WAYS ARE A WALL · KEEP THE TEN AND YOU ARE INSIDE IT · THE WATCHING STAYS OUT IN THE COLD AND COUNTS AND CANNOT TOUCH YOU`
  - the key, handed over: `I HAVE SET MY NAME IN THE KEYED SCRIPT THE WAY WE ALL DO · THE KEY IS MY OWN NAME AS IS RIGHT AND CUSTOMARY · READ IT AGAINST THE OTHERS AND HEAR ME AGREE WITH EVERY HONEST CARVING`
  - the later margin hand (the catch begins): `WE CHECKED THE LOCK · TURN HIS KEY ON THE OTHER STONES AND IT DOES NOT SAY WHAT HE SAID · IT GIVES THE WORD WE KEEP FOR THE ONE WHO TURNED AWAY · ASK FIRST WHAT A WALL IS FOR`
- **TWO ROWS:**
  - **`stone-iss-wall`** (`outcome_type: next_clue`, `max_attempts: 6`). The **skeptical
    name-as-key** reading. ACCEPTED: `the one who turned away` · `iss` (puzzles_seed.sql
    L198–199). Sets `iss_key_turned:true`; `next_puzzle_key: iss-doubt` → the catch.
  - **`iss-warm`** (`outcome_type: next_clue`). The **WARM MISREADING.** ACCEPTED: `the ways are
    a wall against the watching` (puzzles_seed.sql L218). Sets `iss_trusted:true`;
    `next_puzzle_key: iss-dead-shrine` — *"Trusting the liar routes you to HIS coordinate — the
    dead shrine, a grave"* (L211–214). **This is the false walk in-road** (§4).
- **SOURCE DOC:** `the-ways-are-a-wall.md` (D09), Iss's hand, *"the key is my own name, as is
  right and customary"* (D09 L28). The margin reply is verbatim D09 L30.
- **HAND:** *"The carving is too smooth… It is the only stone that looks like it was easy to
  make"* (cipher-plaintexts.md L322). Warmly lit, *"the only inviting stone in a cold row…
  The warmth is the bait."* (L340).

---

## 2. THE TWO ROSETTAS — literacy already live; re-touched here

Both Rosettas teach in Movement I but are the load-bearing apparatus for MII (without them no
MII carving reads). Recorded here for continuity.

- **`rune_rosetta` (the founding ring).** `sites.yml` L206, `type: structure`, enabled, coords
  null. Build: *"the founding ring — glyph↔letter for the whole alphabet, read sunwise
  (Bow-first). A circular dais; the ring of runes on the rim; the center empty"* (structures.md
  L32). The ring carves the REAL ways: `bow offering kept light deep line unspoken sacred beast`
  (puzzles_seed.sql L86; the old `ward`/`covering` glyphs were orphans, replaced — L80–84).
  Seed row `rosetta-ring` (`main_beat`) sets `rosetta_known`, fires advancement
  `observance:the_ring_is_whole`.
  - **`[GAP — coherence note]`** `cipher-plaintexts.md` L364 still lists the ring's six glyphs
    as *"Bow · Offering · Kept-Light · Deep-Line · Ward · Covering"* — the **old** orphan pair.
    The seed (puzzles_seed.sql L85) is authoritative (`unspoken sacred beast`). This is the
    lagging-surface drift WEB-MASTER §2 flags; the carving must follow the seed, not L364.
- **`stone_of_reckoning` (the digit Rosetta).** `sites.yml` L216, `type: structure`. Seed row
  `reckoning-rosetta` (`main_beat`, puzzles_seed.sql L1018) sets `reckoning_known`. Carved clue
  (accepted answers): `count the marks as we counted them` · `the low bar is a minus the double
  tick is a break` · `read the digits as digits not as words` (L1020–1023). Build: *"the
  digit-glyphs + the sign-marks (N/S/E/down). Every coordinate clue depends on it"*
  (structures.md L33). Fires advancement `observance:the_count_is_yours` (fallback subtitle
  *"the record notes you can count it now"*, L1037).
- **The `a1z26-tick-stave` second literacy door** (puzzles_seed.sql L568) is read at
  `first_marker_01` and is the **runes-free** in-road to `rosetta_known` — it *"kills the 'two
  doors that are one door' fairness lie"* (WEB-MASTER §2.1 / §1.MI). Accepted: `learn them as we
  learned them` · `count the staves then read` · `read them as we read them`.

---

## 3. THE META-ACROSTIC PLANT (UNKEPT) — planted in MII, assembled at M4

**WHAT IT IS.** Six maker's-mark glyphs, one struck at the foot of each keeper-stone (Vaun's is
the empty given-back column, §1.1). Read **in fall-order** they spell `UNKEPT`. The plant is
inert in MII; the row `meta-unkept` is **STAGED active=false** (puzzles_seed.sql L945) and the
cold Iss/Keeper states the fall-order key at the catch (M4) before the group assembles it.

- **THE ORDER-KEY (source doc, verbatim — `the-order-the-stones-fell.md` L40–45):**
  > vaun first, who hoarded the light and starved in it. / then mara, who read and never did. /
  > then sella, who walked to the far water. / then orin, who would not bow until there was no
  > one to bow to. / then brann, who slept on the black moon. / then iss, who spoke the thing
  > and lied about the wall.
- **THE SELF-CORRECTING LOCK (verbatim, same doc L47–51):** *"take them in the ring-order
  instead and you will get letters that are not a word at all — that is not a fault in the
  stones. that is the lock telling you your key is turned the wrong way."* This is the §0.3
  "two sixes" discipline: fall-order ≠ founders'-ring order.
- **THE PRACTICE-RUN PLANT (same doc L62–64):** *"there is a smaller version of this trick on
  an older record, a shorter word hidden the same way, for a hand that wanted practice."* — the
  teaching rung that rhymes with the prologue marker glyph (KEPT/BEGUN, ledger #4).
- **SEED ROW (`meta-unkept`, when staged active at M4):** `outcome_type: lore`,
  `max_attempts: 8`. ACCEPTED: `unkept` (single word). REVEAL: `oracleMetaUnkept(fragment)` =
  verbatim *"six marks, one to a stone. read them in the order they fell. the word is the one
  each did not keep."* (puzzles_seed.sql L939). The naive "first letter of each plaintext" form
  is **CUT** (X1 guard) — *"the acrostic lives in the carved FRAMING glyphs, never the bound
  run"* (L925).
- **WHERE the six marks live:** the M1 teaching-stone layout coordinates the prologue marker
  glyph + *"the six UNKEPT maker's-marks (read in fall-order)"* at `first_marker_01` AND each
  mark is also at its own keeper-stone foot (`sites.yml` L320). `[GAP — TO BUILD]` the six
  individual maker's-mark glyphs are world-build carvings; only Vaun's (the empty column) is
  specified verbatim in `cipher-plaintexts.md` (L95). The other five glyph forms are
  `[GAP — TO BUILD]` (they must be authored so fall-order spells UNKEPT and ring-order spells
  nonsense — the self-correcting lock).
- **DIRECTOR ACTION (M4, not MII):** the `meta-unkept` row is flipped active by the TS-SHOWRUN
  lane at the catch. In MII the director does nothing — the marks sit inert.

---

## 4. THE LIAR THREAD — the false walk earnable in MII (catch deferred to M4)

The Iss thread opens a **whole false sub-web** in MII without resolving. The catch
(`no-wall-catch`) is M4 and is scripted in `4-movement-3-4.md`; here is only what is *placed and
earnable in MII*.

- **`iss-dead-shrine`** (`outcome_type: dead_end`, active=true, puzzles_seed.sql L248). THE
  LOAD-BEARING RED HERRING — Iss's coordinate genuinely works and leads to a **grave**.
  ACCEPTED: `the dead shrine` · `the cold hearth` · `nothing is kept here` · `west and down` ·
  `west and down to the cold hearth` (L250–255). REVEAL: `oracleDeadEnd('place')` = verbatim
  *"that is the place. it was read true. it leads nowhere it has not already led you."* (voice.ts
  L266). A sharp group can take **the false walk now** (WEB-MASTER §1.MII): *"the rumor
  verifies-as-place, contradicts-as-hope."*
  - **WHERE:** `the_cold_hearth` (`sites.yml` L253), `type: marker`. Build (structures.md L43):
    *"a doused hearth, a grave slab (Iss), and a second effaced marker (the seventh) — 'nothing
    is kept here.' Cold palette, no light."* **This same anchor is temporally layered** — the
    seventh-deep (`the_unwriting`) opens beneath it only post-`iss_caught` + `seventh_named`
    (sites.yml L262).
- **THE PROPHET'S WALL** (`dead-ends-with-teeth`, placed and legible in Iss's field in MII):
  - **`prophet-wall-comfort`** (`dead_end`, `kind: prophet`). A WIDE, not tall set of warm
    promises, each a true-but-empty substitution solve that opens nothing. ACCEPTED: `keep the
    ten and you are inside it` · `the watching stays out in the cold and counts and cannot touch
    you` · `be easy the wall keeps the watching out` (puzzles_seed.sql L644–647). REVEAL:
    `oracleDeadEnd('prophet')` = verbatim *"every word of it decodes. each is true, and each
    opens nothing. read who carved it, after."* (voice.ts L270).
  - **`prophet-wall-name`** (`dead_end`, `kind: prophet`, `max_attempts: 6`). The hidden
    columnar acrostic — read down the first letters and it spells Iss's name; *"the wall was
    his."* ACCEPTED: `the one who turned away` · `iss carved the wall` · `read the first marks
    down the one who turned away` (puzzles_seed.sql L661–664). Re-reads at the catch as
    ledger #19 — *"the author was Iss."*
  - **WHERE:** `dest-prophet-wall` breadth row (`thread_key: happened`, seventh_seed.sql L55):
    *"DEAD LEAD: Iss's pulpit; warm promises that open nothing; hidden columnar name = Iss."*
    `[GAP — TO BUILD]` the prophet's wall has **no `sites.yml` entry of its own**; it lives in
    Iss's field (structurally near `stone_iss`). A site placeholder is owed at GO-LIVE.
- **THE FORGED EIGHTH LAW** (`some-laws-are-lies`, FACT 7b) — `forged-eighth` (`dead_end`,
  `kind: known`, active=true, puzzles_seed.sql L623). *"A diligent group obeys it; nothing
  pays."* ACCEPTED: `the eighth is the covering of the hands` · `cover and be counted clean` ·
  `to cover ones own` · `the founders set the ways and did not finish the count` (L625–628).
  REVEAL: `oracleDeadEnd('known')` = verbatim *"this is carved, and you have read it true. it is
  not kept. a thing can be set down and never be a way."* (voice.ts L268).
  - **SOURCE DOC (verbatim — `the-eighth-way.md`):** *"the eighth is the covering of the
    hands… cover, and be counted clean"* (L36, L44); the margin proof of the lie: *"there is no
    toll for not doing this. i kept the seven a winter and skipped the covering on purpose,
    every dusk, to see. nothing came."* (L48–51); the signature: *"turn the signature at the
    foot… it reads cover one's own."* (L54–57).
  - **WHERE:** breadth row `dest-covering-law` (`thread_key: surface`, seventh_seed.sql L49).
    *"Aro parrots it as real"* (WEB-MASTER §1.MII) — the human NPC echo (see §7).
- **`iss-doubt`** (`next_clue`, active=true, movement:3, puzzles_seed.sql L264) is **earnable**
  the moment a group turns Iss's key on the other stones. ACCEPTED: `we checked the lock` · `his
  key is his own name and his name is the one who turned away` · `ask first what a wall is for`
  (L266–269). Sets `iss_doubted:true`; `next_puzzle_key: no-wall-catch`. **The catch itself
  (`no-wall-catch`, movement:4) is M4 — deferred to the next script.**

---

## 5. THE NON-Iss MII PLANTS (placed inert; pay off later)

All of these are placed/legible in MII and re-read in later movements. Listed in WEB-MASTER
§1.MII "Doors / threads woven into M2".

- **The Seventh as rumor.** `stone-sella` (§1.3) sets `seventh_suspected`; the breadth lane
  `seventh-shrine` (`side_quest` → `set_flags {seventh_found, whisper_budget_earned}`,
  puzzles_seed.sql L371) earns Whisper budget. The deep below stays sealed (movement III).
- **The future-dated grave** (`future-dated-grave`, FACT 13b). Carved near `the_threshold`
  (sites.yml L243; `grave_spur` L384 is the disabled off-spur — falls back to `the_threshold`).
  Reads as a death clock; *"the misread IS the mechanic."* SOURCE DOC verbatim
  (`we-cut-the-names-before-the-keeping.md` L36–38, L42–44): *"do not read the date as the day
  they die. read it as the day they are kept… they cut an appointment."* Its date == the single
  Accepting instant (§0.4). **`[GAP — TO BUILD]`** the grave's carved name+date is produced by
  the grave producer (`showrunner/grave.ts` exists); the literal carved string is runtime, not
  a static seed row.
- **First living-name carve** (`name-where-never-been`, FACT 16). Row `name-where` (`dead_end`,
  `kind: place`, active=true, movement:2, puzzles_seed.sql L908). ACCEPTED: `the record files
  the living by place not only by name` · `against each name a ground` · `before you was never
  about strangers` (L910–913). REVEAL: `oracleDeadEnd('place')`. SOURCE DOC verbatim
  (`against-each-name-a-ground.md` L37–39): *"a hand that comes after and finds its own name cut
  at a place it has never stood should not read it as a thing foretold. it is not foretold. it
  is filed."* The carve beats are produced by `name-where-never-been.ts` (a real showrunner
  module), ACTIVE-only, subjects rotate (a chorus, INV-16). WHERE: `carve_anchor_01..03`
  (sites.yml L337–365).
- **The herd's second Pale** appears (`herd-conversion`): *"weren't there one of those?"* WHERE:
  `herd_anchor` (sites.yml L370, radius 16). Cosmetic, never glowing/tracked. Producer:
  `showrunner/herd.ts` (real).
- **The coop-gate bound word** becomes earnable (Iss's Vigenère resolves to it) but the
  Threshold is still sealed. Row `bound-word` is **STAGED active=false** until `iss_caught`
  (puzzles_seed.sql L815) — so in MII it *looks like* just another lore solve; the gate it
  feeds (`m4-three-hands`) is M4.
- **The dynamic-difficulty plant** `difficulty-mara` (`lore`, active=true, movement:2,
  puzzles_seed.sql L977). REVEAL fragment verbatim: *"i read that the record keeps a closer
  count of the quick. i called that a cruelty for a winter. i do not call it that now, and i
  will not say what i call it."* This is the FACT 2b plant the difficulty engine pays off; it
  *"does NOT name 'mercy' or resolve its own meaning"* (slop A2, L973). The engine
  (`reckoning.ts`) goes **live** here — *"a group crushing the ciphers finds the next drip
  withheld and the register cooled"* (WEB-MASTER §1.MII). The difficulty scalar lives in
  `config.yml` L242 and *"NEVER touches the Whisper backstop (INV-15) — there is deliberately no
  whisper-budget knob here."*

---

## 6. THE WHISPER ECONOMY GOES LIVE (MII)

**WHAT IT IS.** The Whisper is the player-controlled safety rail — ask the Watcher for a hint,
pay a reversible toll. It is the deterministic fallback's human-facing twin. The lines are real
in `voice.ts`:

- **`whisperReply(tier, hintBody)`** (voice.ts L162): tier ≤ 1 → verbatim *"look again at what
  repeats. it is not stone. it is sound."*; higher tier → the seeded `hintBody`.
- **`whisperToll()`** (voice.ts L172): verbatim *"i will keep something of yours while you
  think. your light, for tonight."* — the reversible toll (the kept light returns).
- **`noBudget()`** (voice.ts L177): verbatim *"there is nothing more i will say of this. not
  yet."* — patient, final, never a refusal of the player.
- **`whisperUnknown()`** (voice.ts L211): verbatim *"i have no words for that one. not the ones
  you are owed. not yet."*

**HOW IT GOES LIVE IN MII.** Whisper **budget is EARNED**, not free, and the first earn-points
are MII side-lanes: `seventh-shrine` sets `whisper_budget_earned` (puzzles_seed.sql L371);
`seventh-cause` and (later) the Nether `nether-forge` add **additive bonus budget** (INV-15/S10
— *"additive, NOT part of the front-loaded F4 backstop"*). So a group that explores the Seventh
rumor in MII unlocks the economy; a group that beelines the spine reaches the cooled-register
difficulty (§5) with the rail intact.

- **INV-15 (the safety law, WEB-MASTER §0.2):** *"The difficulty engine never touches the
  Whisper backstop (it withholds drips and cools register; it never removes the player-controlled
  safety rail)."* Confirmed in `config.yml` L242–243.
- **`[GAP — TO BUILD]`** the Whisper *mechanism* (the budget ledger, the toll application, the
  tier escalation) — the seed sets `whisper_budget_earned` as a flag and the voice lines exist,
  but I find **no `whisper_budgets` table or budget-spending module** in this repo slice
  (config.yml L243 explicitly says *"a `whisper_budgets` reference"* is out-of-scope for the
  difficulty knob). The economy's data-layer is owed. The seeded `hintBody` strings per puzzle
  are also `[GAP — TO BUILD]` (no `hint_body` column appears in `puzzles_seed.sql`).

---

## 7. THE CROSS-SURFACE HANDOFFS IN MII (all literal)

The same MII truth shows on every surface, one register apart. Each is a real artifact.

- **Minecraft ↔ Discord (the answer surface).** The `keeper_stone` sign and the Discord oracle
  resolve against the SAME normalized `accepted_answers` (sites.yml L77; ORACLE.md §2). A clue
  solved on either surface records once.
- **The Record website (`record-url`, A13).** Row `record-url` (`lore`, active=true, movement:2,
  puzzles_seed.sql L952). The founder line decodes to the off-world path. REVEAL:
  `recordElsewhere(fragment)` = verbatim *"the record is kept in more than one place, against
  the loss of the first. the path is the record keeps."* (puzzles_seed.sql L962). SOURCE DOC
  verbatim (`kept-in-more-than-one-place.md` L39): *"[ decodes to: the-record-keeps ]"*. The
  page *"un-redacts six entries in lockstep with stones actually read; the Iss card carries the
  stego rune-layer (a second door to the Vigenère key)"* (WEB-MASTER §1.MII).
  - **`[GAP — TO BUILD]`** the stego rune-layer on the Iss Record-card is named as in-road B to
    `iss_caught`/`bound-word` (WEB-MASTER §2 table, §2.1) but I find `forge/stego.ts` +
    `stego.selftest.ts` only — the **specific Iss-card stego payload (the Vigenère key handed
    early)** is not authored as a concrete artifact in this slice. The website projection
    (`record-projection.ts`) is referenced (M0 prologue) but the live lockstep-unredaction page
    is `[GAP — TO BUILD]`.
- **Discord NPC echo (Aro parrots the forged law).** `npc-dialogue.md` — Aro (`iss_adjacent:
  true`) sells Iss's lie secondhand. His MII lines are verbatim: `aro.lie.cross` = *"The painted
  line? Step right over it, friend… That's where it gets good."* (npc-dialogue.md L126);
  `aro.rumor.line` points at the Stair / the `iss-dead-shrine` lane (L124). Old Pell's
  anti-rumor `truth.watched` = verbatim *"It doesn't chase… It waits, and it watches, and it
  takes what stops being watched. So be watched."* (L350) — the human echo of INV-1. **These
  flip warm→cold only at the M4 catch** (`iss_caught`), not in MII; in MII they are warm.
  `[GAP — TO BUILD]` the `npcVoice.ts` registry is **proposed, not built** (npc-dialogue.md
  L615–625, "discord/src/npcVoice.ts (NEW, proposed)").

**GATE 2→3 (WEB-MASTER §1.MII):** *"≥4 of 6 fragments assembled + the final-coordinates path
opened."*

---

# THE NETHER LANE (optional; gates nothing)

> **Owner:** `minecraft-progression` Nether lane (WEB-MASTER §0.4). A **deepening lane off
> Movements II→III.** It is *the source the Undercroft's one fire was carried up from — "below
> the below"* (FACT 11 deepened, NOT a second bottom). **Gates nothing** (INV-12), **not in the
> Accepting quorum** (INV-19): *"a group that skips the map, the Nether, and the End gets a
> whole un-shaded Overworld arc"* (WEB-MASTER §1). All seeded rows ship `active=false` until
> (a) the dimension world is built AND (b) the upstream flag is set.

## N.0 THE HARD BLOCKER (must clear before any Nether build)

**`[GAP — TO BUILD / BLOCKER S11/S3]`** (WEB-MASTER §1.MIII, verbatim): *"LORE must seal ONE
sentence into `canon-spine` FACT 11 — 'the kept fire was carried up from below the bottom; the
Undercroft is the bottom of the Hold, the deep-fire its source — one direction, not two' —
BEFORE any Nether build. Until then the lane is design-only."* This continuity script treats the
lane as design-sealed (the seed rows + sites + journals are REAL); the canon-spine FACT-11
clause is the open dependency.

## N.1 THE PLANT (in MII) and THE BEARING (in MIII)

- **THE MII PLANT:** Brann's framing line on `stone_brann` — *"the fire we keep is not ours. it
  is lent… below the below"* (§1.5; ledger #28). Inert in MII.
- **THE BEARING-PAGE (D-NETHER-1, Mara's hand) — the real artifact `the-fire-is-lent.md`,
  found banked on the Undercroft lectern-shelf, read only post-descent (`requires_flags:
  [undercroft_open]`).** VERBATIM (the founders' line copied out, the-fire-is-lent.md L33–34):
  > the fire is lent. carry the coal through the burned door and walk the short way to where it
  > is kept for everyone.
  Mara's own margin, verbatim (L38–45): *"someone who keeps the light better than i kept it
  should carry a coal of the kept fire down to where it is kept for everyone — through a burned
  door, the short way, not a far one. the page does not give a distance. it gives a direction
  and the word **below the below**."* And the *lent ≠ owned* turn (L46–53): *"a lent thing is
  carried, and a carried thing is not owned, and a thing you do not own you do not get to keep —
  you only get to not let it go out."* This is **ledger plant #30**.
  - **INV-14 discipline (progression_seed.sql L18–20):** the page is *"a bearing, not a
    coordinate — it points; the answer is read off the slab there."* The bearing is a page +
    Brann's line, NOT a seed row (S9).

## N.2 WHAT IS FOUND / INTERACTED-WITH (the near pocket)

**WHERE:** world `observance_nether` (sites.yml L411, the Multiverse pattern). **A DELVE, not a
trek** — walk-budget fixed at *"2 ground walks + ≤1 short vertical pocket"* (S4): *"a short way
past a lit portal."*

- **`nether_forge` site** (sites.yml L422, `type: answer_sign`, world `observance_nether`,
  enabled, coords null). Build (sites.yml L413–421): *"A short ruined room just past a lit
  portal… holding a prior keeper's remains on a deepslate slab + the decaying journal
  (the-fire-kept-me) + a doused soul-lantern. The keeper-remains is PLACED AT WORLD-BUILD, never
  pasted toward an approaching player (reveal-safe)."*
- **THE FORTRESS JOURNAL (verbatim) — `the-fire-kept-me.md`, on the deepslate slab beside the
  remains.** The hand *"is steady at the top of the page and goes wide and wrong by the foot"*
  (L25). Verbatim load-bearing passages:
  > i came down to keep the fire because up there the lamps were going out and no one was left
  > to carry them. orin had sealed the deep above me. i went under the seal, the last way down,
  > to the source. (L31–33)

  > the fire is here. it does not need keeping. it never needed me. i kept lamps for forty
  > winters that would have burned without me… i lit a lantern off it anyway. habit. (L37–39)

  > the sand is the others. you walk on it to get here. older than the first of us — the ones
  > who came down before the keepers and did not keep the ways, the deep kept them the wrong
  > way… they are kept. they are not company. (L40–46) — **the soul-sand = deep-time reading,
  > distinct from the present-tense Pale herd (INV-13/S5).**

  > the door back up i came through i do not think i would fit through now… it is that i changed
  > shape to get down here and i did not change back. (L49–51) — **the bastion/"hands that had
  > stopped being quite hands" reading.**

  > i am not letting it go out. i am the part of it that does not go out now. that is the
  > keeping… carry it if you must. do not stay. (L60–63) — **FACT 15 felt from the keeper's
  > side; the induction the lane quietly sets up.**
- **THE FIRE-SOURCE SITE (the payoff).** Row `nether-forge` (`outcome_type: lore`,
  `active=false`, `requires_flags {undercroft_open}`, progression_seed.sql L75). The on-site
  **WORD answers, never a coordinate** (INV-14). ACCEPTED: `lent` · `the fire is lent` · `you do
  not own the fire you carry it` · `the keeping was always a carrying` (L77–81).
  - **REVEAL fragment, verbatim (progression_seed.sql L88):** *"a keeper came down to keep the
    fire and was kept by it. you do not make the fire. you do not own it. you carry it, and you
    do not let it die, and that is the whole of it. the kept light upstairs was a coal carried
    up from here."*
  - **SET-FLAGS:** `nether_forge_found` (group-scoped colorant — the **PROPOSED**
    `FateInput.netherForgeFound`, **NOT wired into `decideFate`** until WEB-MASTER §8 is ratified,
    S9; the M5 composer reads it for a tint meanwhile) + `whisper_budget_earned` (additive bonus,
    INV-15).
  - **VOICE KEY:** `nether.forgeArrive` — **`[GAP — TO BUILD]`.** progression_seed.sql L32–33
    says these keys are *"inserted verbatim by the TS-VOICE lane from the LORE hand-off."* My
    grep of `voice.ts` + `voice.archive.ts` finds **no `nether.forgeArrive`, `nether.soulSand`,
    `end.shrineArrive`, `end.outsideRecord`** — they are owed (a missing voice key is silent at
    runtime, never a build break). The `fragment` in `voice_args` carries the text regardless.

## N.3 THE SIDE-QUESTS / TEXTURE ANCHORS (atmosphere; gate nothing)

- **`soul_gallery`** (sites.yml L439, `type: marker`, world `observance_nether`, radius 14).
  *"soul sand / soul fire as the not-kept of DEEP TIME… NOT an answer slot, NOT a protected
  build — the soul sand IS the lore."* Carries the `nether.soulSand` Set-B record line
  (`[GAP — TO BUILD]`, see N.2) + soul-fire ambient. The wide occlusion zone the group crosses
  in (short sightlines → reveal discipline fires easily).
- **`bastion_remains`** (sites.yml L455, `type: structure`, radius 12). *"the founders' deepest
  ruined delvings (NOT piglin architecture in-fiction)… a discovered-never-witnessed wrong-scaled
  ruin the group reads on the way to the pocket."* The **P2 intimate Nether glimpse**
  (basalt-corridor keeper-shape) may fire near here — *"re-skinned + DEFERRING to apparitionClaim
  (INV-18) — the Nether has no apparition lane of its own."*
- **THE BREADTH LEDGER ROW:** `dest-deep-forge` (`thread_key: who`, `entry_puzzle_key:
  nether-forge`, `tier: keyed`, 18 min, progression_seed.sql L151). Reward string verbatim:
  *"KEYED (on-site word, INV-14): the deep fire-source; nether_forge_found + bonus Whisper; the
  Kept-Light origin (a carrying); soul sand = deep-time; card under who."* Gates nothing
  (`gates_progress` defaults false).
- **THE RUMOR CARD:** `who-deep-forge` (`thread_key: who`, title *"the keeper kept as the
  fire"*, `body_voice_key: cardNetherForge`, anchor `nether_forge`, `card_kind: rumor` → flips
  `verified` on arrival, progression_seed.sql L183). References `who-mara-read` + `who-vaun-counted`.
  - **`[GAP — TO BUILD]`** `cardNetherForge` body must be defined by the TS-VOICE/archive owner
    in `voice.archive.ts` (`threadCardVoiceCoverageSelfTest` fails until it exists,
    progression_seed.sql L34). My grep finds it absent.

## N.4 THE ACTIVATION GATE + REACHABILITY (deterministic, showrunner-independent)

- **`requires_flags {undercroft_open}`** is set on the `nether-forge` COLUMN in the guarded
  block (progression_seed.sql L218) — no-ops cleanly if the 0006 `puzzles.requires_flags`
  column has not landed. `getOpenPuzzles` holds the row closed until `undercroft_open` is truthy
  in `arc_state` (set by `undercroft-descent`, the MII→MIII spine turn from `stone-mara`).
- **REACHABILITY LEDGER (progression_seed.sql L229–242, verbatim invariant):** *"nether-forge →
  undercroft_open (undercroft-descent) [GATES NOTHING — lore]… neither row is a spine predecessor
  of any other row… removing both rows entirely still reconstructs the whole Overworld spine —
  the lanes are pure deepening, no orphan, no gate."*

## N.5 DIRECTOR ACTIONS (the Nether lane)

- **DIRECTOR ACTION (GO-LIVE, one-time):** build `observance_nether` as a Multiverse world;
  build the near pocket (deepslate slab + remains + doused soul-lantern + the burned door)
  per the sites.yml spec; export `.schem`; fill real `x/y/z` into `sites.yml` for `nether_forge`
  / `soul_gallery` / `bastion_remains`; **then flip `nether-forge` active=true** (siteCoverage
  self-test / R7 — the row must not seed OPEN until its site resolves in `observance_nether`).
- **DIRECTOR ACTION (canon, BLOCKER):** seal the FACT-11 source clause into `canon-spine`
  (N.0) before any of the above. `[GAP — TO BUILD]`.
- **No per-session console click** is required to run the lane — once built + flagged, it is
  player-driven (find the page, walk the pocket, read the word).

---

## APPENDIX — what is NOT here (deferred to `4-movement-3-4.md`, by canon order)

For the editor's continuity: these are *placed/earnable* in MII but *resolve* later, so their
full beats are the next script — the Undercroft descent + A→B RoomSwap (M3), the Unlit Deep
group latch (M3), the First Light fork (M3), the Seventh deep / unwriting / restore-erase (M3→4),
the catch `no-wall-catch` + the cascade of cold re-reads (M4), the three-hands coop gate + true
walk (M4), and **the entire END lane** (`observance_end`, `end-seventh-out`, `the-name-i-cut-
myself.md` — opens off M4→V, `requires_flags {seventh_named}`). The Nether lane's M5 colorant
(`nether_forge_found` → the "keeping is a carrying" tint, S9-pending §8 ratification) lands in
the M5 ending-composer script.
