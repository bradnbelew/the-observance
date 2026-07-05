# THE OBSERVANCE — CONCRETE PUZZLE DESIGNS (the diverse expansion)

> **DESIGN doc, not a seed.** Concrete enough that a later pass can wire these directly into
> `puzzles_seed.sql` (+ the `answer_kind` column from migration 0007, PUZZLES.md §4) and the plugin
> listeners. Nothing here is wired yet. Every puzzle is authored to obey the canon set:
> [OVERHAUL.md](OVERHAUL.md), [PUZZLES.md](PUZZLES.md), [canon-spine.md](../arc/lore/canon-spine.md),
> [WORLD-BIBLE.md](../arc/WORLD-BIBLE.md), [the-seventh-below.md](../arc/lore/documents/the-seventh-below.md),
> [the-companion.md](../arc/lore/documents/the-companion.md). Where this doc disagrees with the canon
> set, the canon set wins.
>
> **The rule (PUZZLES.md §0):** no two puzzles a player meets in a row share the same
> (TYPE × SURFACE × VERB × ANSWER); the 5 letter-ciphers stay a minority; every puzzle has a
> legible path (D8 Golden Question) — never "guess the GM's mind." Answers are normalized:
> lowercase, `[a-z0-9 ]`, no apostrophe, single-spaced.

---

## 0. THE EXISTING 5 CIPHERS (the minority baseline — DO NOT expand)

These already exist in the seed and stay exactly as the letter-cipher quota. Every puzzle below is
a *non-cipher* beat (or the one deliberate layered exception, `iss-vigenere-name`, which is canon).

| Keeper | Existing cipher | TYPE | Leave as-is |
|---|---|---|---|
| Vaun | Caesar stone | letter-cipher | yes |
| Mara | book-cipher (`page-line-word`) | letter-cipher (positional) | yes |
| Sella | atbash / mirror stone | letter-cipher | yes |
| Orin | substitution stone | letter-cipher | yes |
| Brann | beacon / colour-sequence | letter-cipher (positional) | yes |
| Iss | Vigenère (key = his own name) | letter-cipher (layered) | yes — canon catch |

The count below adds **17 new puzzles** across 11 TYPE categories, of which **0** are new
letter-ciphers. Cipher share after expansion: 6 of 23 (~26%), matching PUZZLES.md §0.

---

## 1. THE ANSWER_KIND / TYPE / SURFACE / VERB LEGEND (used per row)

- **answer_kind** (migration 0007): `phrase | coords | url_token | behavior | object | spoken | code | none`
- **TYPE** (PUZZLES.md §1): the encoding family.
- **SURFACE** (§2): in-world / record-website / discord / external / voice.
- **VERB** (§3): the body action.
- **clusters under**: the existing thread this hangs on (`who`, `place`, `happened`, `surface`,
  `human`, or a keeper/spine thread), for the salience showrunner.

Every row also states the **legible path** (the fast surface read that keeps nobody blocked, D8)
and the **outcome** (`lore | next_clue | main_beat | side_quest | dead_end`, per the seed's
outcome grammar).

---

## 2. VAUN — the hoarder (palette: arrange, count, set-combination; object/count/code)

Vaun's nature: he *held everything and gave nothing back*. His puzzles are about **quantity,
order, and refusal to release**. His voice speaks only of what he kept (canon-spine §1, §6.8).

### 2.1 `vaun-hoard-sorted` — the deliberate hoard (chest-arrange / sorting)
- **TYPE** logic/deduction (arrange) · **SURFACE** in-world · **VERB** arrange · **answer_kind** `object`
- **Setup.** Vaun's cache: a room of double-chests, each labelled by a hanging sign in the rune
  font with a resource-name Vaun tallied (`iron`, `salt`, `seed-grain`, `lamp-oil`, `of the deep`,
  `given back`). All chests are full **except the last two** — `of the deep` holds a single item;
  `given back` is empty, a long-ruled empty column made physical (mirrors `counted-them-in-the-dark`
  exactly). A lectern holds his last leaf. The puzzle: the group must **move the first item of the
  deep into the `given back` chest** — perform the offering Vaun never did. A plugin listener watches
  the `given back` chest's contents; when the specific PDC-tagged `first of the deep` item is
  deposited there, the flag flips.
- **Answer(s).** No typed answer. The **behavior of depositing** is the answer (`object` kind,
  a container-content check). If a group insists on typing, the fallback phrase is
  `give the first of the deep back`.
- **Legible path.** The empty second column is impossible to miss; the leaf and the margin hand
  both say *"the first of the deep is owed back."* Even a group that never reads carefully sees five
  full chests and one empty labelled one and tries filling it.
- **Outcome.** `main_beat` — Vaun's cache door opens (his Caesar stone becomes readable behind it);
  the record adds one line: *"the one column he never struck is struck, late, by a later hand.
  it is enough. the deep lifts for you where it did not for him."* Per-keeper agency (OVERHAUL
  Pillar 1): *give back what Vaun hoarded.*
- **Clusters under** `who` (Vaun). **Relief valence:** small true warmth — the group does the mercy
  Vaun couldn't.

### 2.2 `vaun-bookshelf-tally` — the count that opens the door (chiseled-bookshelf register)
- **TYPE** numeral/positional · **SURFACE** in-world · **VERB** set-combination · **answer_kind** `code`
- **Setup.** A **chiseled bookshelf** of 6 slots set into the wall of Vaun's cache (Minecraft-native,
  PUZZLES.md §1 / §5-Vaun). Each slot reads a comparator signal by *which* slot holds a book. Beside
  it, Vaun's tally-leaf gives six counts down a single column (from `counted-them-in-the-dark`):
  the winters and the tallies — `first(1) · fourth(4) · sixth(6) · seventh(7) …`. The group must
  place books in the slots to encode the count Vaun actually kept (the *taken* column) and leave the
  *given-back* slots empty — the redstone completes only when the pattern matches "all taken, none
  given," i.e. the shape of his sin, which opens the door with a cold click. (The lesson is in the
  mechanism: the door only opens when you reproduce his refusal.)
- **Answer(s).** A **6-digit slot code** derived from the leaf (`code` kind; a lock listener reads
  the comparator line). Typed fallback for the record website: `taken and never given back`.
- **Legible path.** The leaf's single-column tally is a legible instruction; the bookshelf's 6 slots
  visibly map to 6 counts. A group that has read `vaun-hoard-sorted` already knows the two-column idea.
- **Outcome.** `next_clue` → reveals the maker's-mark glyph on Vaun's stone framing (the `UNKEPT`
  acrostic seed, canon-spine §8.5) and points to the Caesar stone.
- **Clusters under** `who` (Vaun).

---

## 3. MARA — the reader who never walked (palette: research, check-this, page-lock; phrase/behavior)

Mara's nature: she *knew every rite and performed none*. Her puzzles reward **cross-reference and
then make you WALK** what she only read (canon-spine §1; her book-cipher already exists).

### 3.1 `mara-lectern-lock` — the rite she annotated (lectern-page redstone lock)
- **TYPE** numeral/positional (Minecraft-native lectern lock) · **SURFACE** in-world · **VERB**
  arrange (turn pages) · **answer_kind** `code`
- **Setup.** Five lecterns stand before a sealed reading-alcove (Minecraft-native, PUZZLES.md
  §1/§5-Mara). Each lectern holds one of Mara's books; each book's *comparator signal strength*
  depends on which page it's turned to (vanilla: a lectern outputs a redstone signal 1–15 by page).
  The circuit behind the wall completes only when the five books are turned to the five pages Mara
  **annotated** — a marginal note (`i marked this one`) sits on exactly one page of each book. The
  combination is *the rite she read most*: the pages spell, by page-number, a sequence the group
  already met (the Bow's fall-order marker count, `1 2 4 4 6` — legible from the marker row).
- **Answer(s).** A **5-page combination** (`code` kind; comparator lock listener). Typed fallback:
  `the rite read and never walked`.
- **Legible path.** Every annotated page is physically marked; the group turns pages until each
  lectern shows its marked note. The redstone gives immediate feedback (a lamp lights per correct
  lectern) so wrong attempts still teach — satisfies the Golden Question (wrong pages = a partial
  lamp row, never a hard block).
- **Outcome.** `next_clue` → the alcove opens onto a single instruction carved inside:
  *walk the rite you have only read.* Sends the group physically to the marker row (sets up
  `mara-walk-the-map` below). Surface-hop: lectern → in-world travel (PUZZLES.md §5-Mara).
- **Clusters under** `who` (Mara).

### 3.2 `mara-walk-the-map` — do the thing she only knew (embodied / check-this)
- **TYPE** embodied/behavioral · **SURFACE** in-world · **VERB** perform (travel + bow) ·
  **answer_kind** `behavior`
- **Setup.** The payoff of `mara-lectern-lock` and of her book-cipher's own instruction
  (*"do the thing it tells you… go where it sends you, and there, do as it says — together, all of
  you, none left at the door"* — from `page-line-word`). The group must physically **go to the
  marker row and bow at the markers, together, with the active roster** (a plugin listener detects
  simultaneous crouch at the marker interaction-entities within a window; dynamic-roster-safe —
  quorum = `effectiveQuorum`, OVERHAUL §4). No typing. This is the honest realization of Mara's
  whole thesis: a map is not a road walked.
- **Answer(s).** The **group-bow behavior** (`behavior` kind; the existing bow custom listener,
  scoped to this site + window).
- **Legible path.** Her book-cipher literally instructs it ("do the thing it tells you"), and the
  marker row teaches the bow by example. A group that reads either surface knows to bow.
- **Outcome.** `main_beat` — the record writes Mara whole: *"the one called Mara is kept by the
  light. what she read, you walked. that is the part she could not give."* Warm-Grief relief beat
  (OVERHAUL Pillar 2): a keeper memory that survives the reckoning — Mara's kinder margin note
  surfaces here (see the new lore doc `the-margin-she-kept.md`).
- **Clusters under** `who` (Mara). **Co-op note:** scales to N; degrades to the present roster.

---

## 4. SELLA — the drowned child (palette: reflection, overlay, forced-perspective; coords/comprehension)

Sella's nature: she *drew the dark before the adults would* and *speaks only as a reflection*
(canon-spine §1, §6.8). Her puzzles are **visual/spatial** and wordless — her copybook ends in
drawings, not words.

### 4.1 `sella-reflection-bearing` — the rune only the water shows (reflection)
- **TYPE** visual/spatial (reflection) · **SURFACE** in-world · **VERB** reflect (look at water) ·
  **answer_kind** `coords` (as a spoken bearing found on-site, INV-14)
- **Setup.** (This is PUZZLES.md §6 worked-example #1, made concrete.) Sella's shore keeper-stone
  face is blank in daylight. Standing at the **shore pool** and looking at the *reflection* in the
  water, a per-player `TextDisplay` mirrored below the surface (or a real carving cut so it only
  reads inverted) shows a rune line: a **bearing** — *"south, by the far water, where the reeds
  fold back."* The bearing points to `dest-far-water` (already a keyed travel destination in
  `side_quests.sql`). The answer is the **destination word cut on-site**, never the coordinate
  (INV-14 / INV-COORD).
- **Answer(s).** The clean destination token found at the far water (`coords` kind, resolved as the
  on-site word). Canonically the far-water site already exists; this stone is the *pointer to it*.
- **Legible path.** The blank stone + a shore pool is a strong "look at the reflection" nudge (the
  keeper is canonically "speaks only as a reflection"). A group that faces the water gets the line;
  one that never does gets silence (honest — canon-spine §1 Sella voice note).
- **Outcome.** `next_clue` → sends to the far water (Sella's copybook drawings, FACT 10 seed).
- **Clusters under** `who` (Sella). **Surface-hop:** in-world stone → in-world travel.

### 4.2 `sella-overlay-lake` — count the rings she drew (lectern comparator lock)
> **RETHEMED (puzzle-variety audit).** This was originally a map-art overlay
> (reflect(coords) → **overlay(coords)** → observe(behavior)) — the same "look at
> something → get a destination word → walk there → read a sign" template as its
> neighbors `sella-reflection-bearing` and `sella-shore-memorial`, three of that
> template back to back (the clearest §0/§11 adjacency violation in the whole set,
> since the answer_kind never rotated either). Converted to a numeral/positional lock
> reusing the exact `mara-lectern-lock` producer/answer_kind pairing (no new mechanic
> type, no new Java). This resolves the violation §11 describes for the Sella run.
- **TYPE** numeral/positional (lectern comparator lock) · **SURFACE** in-world · **VERB**
  arrange (turn pages) · **answer_kind** `code`
- **Setup.** Sella's copybook (at the far water) splits into two physical objects: the six-book
  Kept-Light shelf is Mara's; Sella kept her OWN loose pages in a second, smaller stack. Her
  drawings there are concentric rings — ripples fanning out from a dropped stone, each ring
  numbered in a child's own uneven hand, a tally and not a rite. Five lecterns beside the shore
  pool hold her loose pages; each lectern's comparator signal depends on which page it is turned
  to (vanilla lectern behavior, identical rig to `mara-lectern-lock`). The circuit completes only
  when the five lecterns are turned to the five pages her ring-drawings tally — water-logic
  applied to a Minecraft-native mechanism instead of a second map-art.
- **Answer(s).** A **5-page combination** (`code` kind; the same comparator lock listener
  `mara-lectern-lock` uses). Typed fallback: `the rings she counted`.
- **Legible path.** The rings are visibly numbered in the copybook; the five lecterns visibly map
  to five rings. A group that solved `mara-lectern-lock` already recognizes the rig.
- **Outcome.** `next_clue` → `sella-shore-memorial` (unchanged). The prior `lore` beat (the drowned
  place, a Sella drawing that is only joy) moves to the copybook's OTHER pages, read on the way to
  this lock, so the untainted-joy relief beat (OVERHAUL Pillar 2) is preserved, just not the puzzle
  payload.
- **Clusters under** `who` (Sella).

### 4.3 `sella-shore-memorial` — legible only from above (map-art forced perspective)
- **TYPE** visual/spatial (forced perspective) · **SURFACE** in-world · **VERB** observe (stand at
  one block) · **answer_kind** `behavior`
- **Setup.** (Minecraft-native map-art forced perspective, PUZZLES.md §1/§5-Sella.) Above the shore
  pool, a scatter of coloured blocks on the pool floor and the surrounding banks looks like debris
  from ground level. From **one specific block above the pool** (a ledge with a single worn
  standing-stone, or the elytra approach), the scatter forced-perspective-resolves into Sella's
  own drawn glyph — a small bird over water (her deep-bird, the canary, the thing she kept). The
  "answer" is **comprehension**: standing on the right block and *seeing it* trips a per-player
  detection (the plugin knows when a player stands at the forced-perspective anchor and looks down).
- **Answer(s).** `behavior` (stand-at-anchor + look-down detection). No typing.
- **Legible path.** The worn standing-stone marks the anchor block (a mason left it for exactly this
  vantage); Sella's copybook has the same bird-over-water drawing, so a group that saw the copybook
  recognizes the resolved image. Wrong vantages show only debris — reactive, never blocking.
- **Outcome.** `lore` — a wordless Sella beat: the record, for once, has nothing to say; only the
  bird. Ties the deep-bird custom (`the_sacred_beast`) to Sella's memory (she kept it; she is the
  reflection). Foreshadows the herd/deep-bird thread without a word.
- **Clusters under** `who` (Sella) / `surface` (deep-bird).

---

## 5. ORIN — the mason who would not bow (palette: embodied, banner-heraldry, dials; behavior/phrase/code)

Orin's nature: he *would not bow* and *speaks only when you are crouched* (canon-spine §1, §6.8).
His puzzles are **embodied** — the body IS the answer.

### 5.1 `orin-bow-fall-order` — bow at the markers in fall-order (embodied)
- **TYPE** embodied/behavioral · **SURFACE** in-world · **VERB** perform (bow sequence) ·
  **answer_kind** `behavior`
- **Setup.** (PUZZLES.md §5-Orin.) Six markers stand at Orin's threshold, each carrying a keeper's
  maker's-mark. The group must **bow (crouch) at each marker in fall-order** — Vaun, Mara, Sella,
  Orin, Brann, Iss (canon-spine §8.1). A plugin listener detects the crouch-at-marker sequence in
  order. Bowing out of order (e.g. the founders'-ring order) does nothing (self-correcting, exactly
  as the `UNKEPT` acrostic is — canon-spine §8.5). The order is legible because the stones fell in
  that order and the corpus teaches fall-order at the marker row.
- **Answer(s).** The **ordered bow sequence** (`behavior` kind). No typing.
- **Legible path.** Orin's inscription (`i-thought-it-small`) literally teaches "the bow was not
  small"; the markers are physically there. The fall-order key is taught at the marker row and re-
  taught by the catch. Wrong order = no harm, a soft reset — reactive, not punishing.
- **Outcome.** `next_clue` → Orin's threshold-stone becomes readable from the crouch (`i-thought-it-
  small`'s missing sentence is at "where i was left"). This is Orin's atonement made playable: the
  group bows where he would not. Per-keeper agency (OVERHAUL Pillar 1).
- **Clusters under** `who` (Orin) / `happened`.

### 5.2 `orin-banner-heraldry` — the sigil that is the key (banner-heraldry cipher)
- **TYPE** letter-adjacent but **not a new plaintext cipher** — a banner-heraldry *substitution key*
  that unlocks his EXISTING substitution stone · **SURFACE** in-world · **VERB** decrypt-with-a-
  found-key · **answer_kind** `phrase`
- **Setup.** (Minecraft-native banner heraldry cipher, PUZZLES.md §1/§5-Orin.) Six keeper **banners**
  hang in Orin's mason-hall, each built from banner patterns = one keeper's sigil. Orin's own sigil
  (the mason's square) *is the substitution alphabet key* for his existing substitution stone: the
  banner's pattern order maps plain-letters to cut-letters. The group reads the banner to get the
  key, then applies it to the stone they already found. This makes the **existing** substitution
  cipher a *find-the-key* puzzle rather than a raw decode — it stays the one Orin cipher, but the
  work moves from "grind substitution" to "notice the banner is the key" (PUZZLES.md §0: difficulty
  in *noticing*, not grinding).
- **Answer(s).** The substitution stone's plaintext, now solvable (`phrase`). Canon: Orin's plaintext
  already exists in `cipher-plaintexts.md`; this row is the *key delivery*, not a new answer.
- **Legible path.** Six banners, one per keeper, is a clear "these mean something" set; the mason's-
  square sigil matches the maker's-marks the group has been collecting. If they ignore the banner
  they can still brute the substitution (the fragile solution has a backup — Golden Question).
- **Outcome.** `next_clue`. **Clusters under** `who` (Orin).

### 5.3 `orin-frame-dials` — the marker-sequence lock (item-frame rotation dials)
- **TYPE** numeral/positional (Minecraft-native rotation dials) · **SURFACE** in-world · **VERB**
  rotate · **answer_kind** `code`
- **Setup.** (Minecraft-native item-frame rotation dials, PUZZLES.md §1/§5-Orin.) A sealed
  offering-niche behind six **item frames**, each an 8-position rotation dial holding an arrow-marked
  disc. The group must rotate each dial to *point at* the direction each marker faces in fall-order
  (a physical combination lock; the mason cut the niche so its dials echo his markers). A lock
  listener reads the 6×8 rotation state.
- **Answer(s).** The **6-dial rotation code** (`code` kind). Typed fallback: `bow to the markers`.
- **Legible path.** The dials visibly point like the markers do; the group has already walked the
  markers (`orin-bow-fall-order`). Wrong rotations click but don't open — immediate feedback.
- **Outcome.** `lore` — inside the niche, Orin's private offering (the one custom he *did* keep);
  a small true relief beat and a maker's-mark glyph.
- **Clusters under** `who` (Orin).

---

## 6. BRANN — the watchman on the black moon (palette: temporal, audio, silence-corridor; temporal-phrase/count/listen/behavior)

Brann's nature: he *never slept, watched for the black moon*, *speaks only at night* (canon-spine
§1, §6.8). His puzzles are **temporal and auditory** — you must be there in the dark.

### 6.1 `brann-black-moon-toll` — the toll that only rings in the dark (temporal + audio/morse)
- **TYPE** audio (morse/rhythm) + temporal · **SURFACE** in-world · **VERB** listen (+ wait/return) ·
  **answer_kind** `phrase` (temporal-gated)
- **Setup.** (PUZZLES.md §6 worked-example #3, made concrete.) A bell / note-block sequence at
  Brann's watch-tower plays **only on the in-game black moon** (his way). Its rhythm is **morse**
  for a single word — the word Brann most needs said (`awake`). Off the black moon the tower is
  silent; the record website's ledger nudges *"come when it is dark"* (temporal, PUZZLES.md §6-3).
- **Answer(s).** The morse word `awake` (`phrase`, gated so it only accepts during/after the toll
  has been heard on a black moon — a temporal flag the plugin sets).
- **Legible path.** Brann's journal (`do-not-close-your-eyes-here`) is *readable only at night held
  to the hearth* and repeatedly says *stay awake / do not close your eyes* — the morse word is
  foreshadowed in plaintext. The ledger's "come when it is dark" nudge means a group that shows up
  in daylight isn't stuck, just delayed.
- **Outcome.** `next_clue` → Brann's beacon/colour stone (his existing cipher) becomes readable, its
  colours lit only now. Surface-hop: temporal in-world → the record ledger's "come back" nudge.
- **Clusters under** `who` (Brann) / `happened`.

### 6.2 `brann-silence-corridor` — the corridor that hears you (calibrated-sculk silence)
- **TYPE** behavior-heard (calibrated sculk) · **SURFACE** in-world · **VERB** perform (move in
  silence / sneak) · **answer_kind** `behavior`
- **Setup.** (Minecraft-native calibrated-sculk "it hears you", PUZZLES.md §1/§5-Brann. Sculk as the
  Watcher's sensory organ.) Brann's watch-walk is a corridor lined with **calibrated sculk sensors
  and shriekers**. It is passable **only in silence** — sneaking, no sprinting, no block-breaking,
  no attacking (vanilla sculk ignores sneak-muffled movement). Making a vibration re-seals the far
  door and the shriekers answer (a soft "it heard you" beat). This is the physical enactment of
  Brann's whole vigil: pass through the dark without being noticed. **Observer tie-in (later phase):**
  a shrieker can answer *voice chat* — talking in VC while in the corridor trips it (ties to the
  Observer Engine, PUZZLES.md §1; optional, degrades gracefully if the voice layer is absent).
- **Answer(s).** **Reaching the far door in silence** (`behavior` kind; the plugin reads the sculk /
  movement state). No typing.
- **Legible path.** The shriekers *tell you* the moment you're too loud — the failure IS the tutorial
  (reactive, D8). Vanilla players know sculk = be quiet.
- **Outcome.** `next_clue` → the far door opens onto Brann's watch-record; foreshadows the Watcher-
  as-sensory-organ (the sculk is *it*, listening). **Clusters under** `who` (Brann) / `surface`.

---

## 7. ISS — the liar (palette: logic/deduction, NBT-stego, callback; deduction/object/callback)

Iss's nature: the **warmest, most trustworthy voice** — which is the trap (canon-spine §1, §4). His
catch is a **deduction, not a decode** (PUZZLES.md §5-Iss). His Vigenère (key = his own name) is the
one canon layered cipher and stays.

### 7.1 `iss-which-is-true` — the warm account vs the land (logic/deduction)
- **TYPE** logic/deduction · **SURFACE** in-world + record-website · **VERB** combine (cross-check
  claims) · **answer_kind** `phrase`
- **Setup.** (PUZZLES.md §5-Iss.) Iss's warm wall-doctrine (`the-ways-are-a-wall`,
  `the-wall-of-warm-words`) claims the ways are *a wall against the watching*. Three other surfaces
  contradict him: `no-wall-was-ever-built-here` (a later stone), the empty cold hearth his false
  coordinate led to, and the record's flat line. The group cross-checks: **which of the two accounts
  does the land actually agree with?** The deduction resolves to *the ways are not a wall* — and the
  catch re-reads Iss's whole tree cold (canon-spine §4, a dialogue-state flip).
- **Answer(s).** `the ways are not a wall` (also accept `no wall was ever built here`, `he lied about
  the wall`). `phrase`.
- **Legible path.** Two flatly contradictory claims (his warm one, the later stone's cold one) + a
  dead cold hearth where his "salvation" was = an obvious "someone is lying" setup. The Golden
  Question is satisfied: any of the three contradicting surfaces alone points to the catch.
- **Outcome.** `main_beat` — `iss_caught`. Fires the Iss-seam (the-seventh-below.md REWRITE SPEC):
  one Watcher line (*"he lied about the wall. ask what else he told you warmly. ask who he said was
  cast out for nothing."*) + re-opens `surface-seventh-marker` (catching the wall-lie re-opens the
  Seventh thread). **Also the gate for the companion reveal** (`companion_revealed` requires
  `iss_caught`, the-companion.md §7): the group learns the warm-liar pattern on Iss first, then turns
  it on Wren. **Clusters under** `iss` / spine.

### 7.2 `iss-nbt-falsified-entry` — the record he doctored (NBT-heavy item stego)
- **TYPE** steganographic (NBT-heavy item) · **SURFACE** in-world → external (a URL/hex in NBT) ·
  **VERB** research (inspect the item) · **answer_kind** `object` (→ `url_token`)
- **Setup.** (Minecraft-native NBT-heavy item, PUZZLES.md §1/§5-Iss; weaponizes datamining, D7.)
  Iss leaves a **normal-looking item** — a warm-worded gift, a "keepsake lamp" — whose NBT hides the
  *falsified record entry* he wrote about the Seventh (the lie that the Seventh was *spared / a
  mercy*, the exact lie the-seventh-below.md rebuts). A hex/base64 field in the item's custom NBT
  decodes to a short line + a record-website `/path` token. Meant to be inspected; the datamine is
  the find (D7 — leave a message for the xrayer: the hidden field, once decoded, reads
  *"you looked. good. he counted on no one looking."*).
- **Answer(s).** The `url_token` found by decoding the NBT and visiting the record path (`object` →
  `url_token`). The token resolves the falsified entry on the website, which the group then
  **corrects** (the record-website's "falsified entries the group corrects" surface, OVERHAUL
  Pillar 4).
- **Legible path.** The item is a gift from the warmest keeper — a group that has learned to distrust
  Iss (post-`iss-which-is-true`) will inspect his gift. Vanilla-savvy players inspect NBT reflexively
  (D7). If they never datamine, the same falsified entry is *also* reachable on the record website's
  corrupted archive (two doors — canon web rule).
- **Outcome.** `next_clue` → correcting the falsified entry advances the Seventh thread (proof of
  Iss's lie is testimony the group carries down, OVERHAUL §0). **Clusters under** `iss` / `seventh`.

### 7.3 `iss-bound-word-callback` — re-submit the name in a new context (callback)
- **TYPE** layered (callback / cipher-of-cipher) · **SURFACE** in-world · **VERB** combine (re-use an
  earned answer) · **answer_kind** `callback` (a re-submitted phrase; resolver's unsolved-preference)
- **Setup.** (PUZZLES.md §5-Iss "a callback — re-submit the bound word at the M4 gate — built".) The
  Vigenère key resolved to Iss's name, which decodes to *the one who turned away* (canon-spine §4).
  At the M4 gate, the group must **re-submit that earned phrase** in a new context — the bound word
  binds the deep-gate. The resolver's "prefer-unsolved-among-collisions" rule handles the deliberate
  collision (OVERHAUL §5: `the one who turned away` on `stone-iss-wall` + `prophet-wall-name` — one
  owner + the sequential re-submission).
- **Answer(s).** `the one who turned away` re-submitted (`callback`).
- **Legible path.** The group earned the phrase decoding the Vigenère; the M4 gate carving quotes the
  phrase's shape back at them (*"speak again the name of the one who turned away"*). It's a
  recognition, not a new puzzle.
- **Outcome.** `main_beat` → opens the deep gate toward the Threshold vault. **Clusters under** `iss`
  / spine. **NOTE:** this is the one *genuinely sequential* gate that keeps a hard `requires_flags`
  (OVERHAUL Pillar 2: hard gates become salience boosts *except* the catch → the deep).

---

## 8. CROSS-KEEPER / SPINE PUZZLES (external surfaces, co-op vault, Observer, meta-acrostic)

### 8.1 `spine-recovered-archive` — the salvaged Drive folder (steganographic / research)
- **TYPE** steganographic (spectrogram) + cross-reference/research · **SURFACE** external (Drive) →
  record-website · **VERB** research + listen · **answer_kind** `phrase`
- **Setup.** (PUZZLES.md §6 worked-example #2, made concrete.) A carved sign in the Hold gives a
  string that, entered on the record website (or googled), resolves to an **unlisted Google Drive
  folder** named like a salvaged archive of the Hold's records. Inside: scans (more lore, honest
  flavor), and one image whose embedded **audio spectrogram** hides a name when viewed as a
  spectrogram (PUZZLES.md §1 steganographic). The name is a keeper's or the Seventh's-adjacent word.
- **Answer(s).** The hidden spectrogram name (`phrase`). The Drive folder's other contents pay lore
  only (no fake-puzzle framing — honest archive).
- **Legible path.** The sign says *"what was recovered is kept off the record, at [string]"*; the
  spectrogram image is visibly a waveform/spectrogram graphic (a group that opens it in any
  spectrogram tool — Audacity, an online viewer — reads it). If the external surface is never built,
  this whole row is optional (the spine never depends on it — OVERHAUL §4 async, canon INV-12).
- **Outcome.** `lore` + a Whisper-budget grant. **Clusters under** spine / `happened`. **Surface-hop:**
  in-world sign → external Drive → record website (the point, PUZZLES.md §2).

### 8.2 `spine-threshold-vault` — the asymmetric co-op vault (asymmetric co-op, signature)
- **TYPE** asymmetric co-op · **SURFACE** in-world (per-player illusion) · **VERB** combine + speak ·
  **answer_kind** `code`
- **Setup.** (PUZZLES.md §6 worked-example #4 + OVERHAUL Pillar 3, the signature mechanic, made
  concrete.) A sealed room, backed by a **vanilla 1.21 trial-chamber vault with per-player keys**
  (OVERHAUL Pillar 3 — vanilla's per-player lock-and-key handles the dynamic roster by default). Each
  active player is shown a **different set of wall-runes** via per-player `showEntity` — a different
  fragment of the combination. Only by **reading them aloud together and combining** do they get the
  full code. The fragments are **partitioned over the active roster at solve-time** (fewer players →
  fewer, larger fragments; more → more — OVERHAUL §4, dynamic-roster invariant). The combination
  they assemble produces the per-player keys; the vault is the reward container the keys open
  (OVERHAUL Pillar 3: fragments = puzzle, vault = payoff).
- **Answer(s).** The **assembled combination code** (`code` kind → per-player vault keys). The
  Observer Engine may *hear* them solve it and react (PUZZLES.md §6-4), but the answer does not
  depend on the voice layer.
- **Legible path.** Each player plainly sees their own runes and plainly cannot see the others' — the
  "we each have a piece" realization is immediate (Keep Talking And Nobody Explodes, native to the
  illusion tech). Wren may have steered the group here as his escape key (OVERHAUL Pillar 3 / the-
  companion.md); wire the reckoning to this vault if the lore pass confirms.
- **Outcome.** `main_beat` → the Threshold opens; the true coordinate to the Accepting on-ramp is
  cut on the vault's inner lintel (the "true walk", WORLD-BIBLE §11.2). **Clusters under** spine.
  **Convergence beat — needs quorum** (`effectiveQuorum`, INV-19); the showrunner must not surface
  it unless `activeRosterSize ≥ effectiveQuorum` (OVERHAUL §4 async).

### 8.3 `spine-spoken-name` — the Watcher quotes you back (voice-heard, Observer)
- **TYPE** voice-heard (Observer Engine) · **SURFACE** voice → in-world · **VERB** speak ·
  **answer_kind** `spoken`
- **Setup.** (PUZZLES.md §6 worked-example #5, made concrete.) Once a player **says the catch's truth
  aloud in voice chat** — *"the one who turned away"* — the Observer Engine (Whisper) hears it and,
  within the hour, the Watcher **quotes it back carved on a sign** where the group will pass. The
  scare IS the answer (PUZZLES.md §6-5). Grounding discipline: only fires on the *real* spoken phrase,
  never a fabricated callout (OVERHAUL §4 grounding invariant). This is the diegetic payoff of the
  companion's leak (the-companion.md §7 — Wren carried the words down; the sharp register).
- **Answer(s).** The **spoken phrase** (`spoken` kind; Observer transcript scan sets the flag).
- **Legible path.** No blocking — this is a *bonus* uncanny beat layered on the already-solvable
  `iss-bound-word-callback`. If the voice layer is absent it simply never fires (degrade to silence,
  the grounding invariant). It never gates.
- **Outcome.** `lore` (an uncanny "it knows" beat) — and post-reckoning the sharp quotes *change*
  (OVERHAUL Pillar 5: condemn/free → they cease; understand → they read as sorrow). **Clusters
  under** spine / Observer.

### 8.4 `spine-unkept-acrostic` — the six marks spell one word (observation / meta)
- **TYPE** observation/counting (meta-acrostic) · **SURFACE** in-world · **VERB** observe (read six
  marks in order) · **answer_kind** `phrase`
- **Setup.** (Canon-spine §8.5 / WORLD-BIBLE §11.3, made playable.) Each keeper-stone carries a
  **maker's-mark** in its carved framing. Read in **fall-order** (Vaun, Mara, Sella, Orin, Brann,
  Iss), the six marks spell **`unkept`**. Inert until the catch hands the fall-order key; before that
  each mark reads as one keeper's private grief. The stones' *even, squared* layout (WORLD-BIBLE
  §11.3) is the tell that they were cut as a set — a commission, not a graveyard.
- **Answer(s).** `unkept` (`phrase`).
- **Legible path.** The order-key clue names fall-order explicitly; the glyphs are placed so they
  **fail in ring-order** (self-correcting — a wrong order gives nonsense and nudges to the right one,
  canon-spine §8.5). It **gates nothing** — pure recontextualizing texture (the cleanest "oh, that's
  what those were for"). Lives in the carved framing, never bound cipher plaintext (X1-safe).
- **Outcome.** `lore` — the day-one re-read; the record adds nothing, the group *feels* it. **Clusters
  under** spine / `happened`.

### 8.5 `spine-cold-hearth-shadow` — the shrine that is only cold (observation + F3 instrument)
- **TYPE** observation/counting + F3-as-instrument · **SURFACE** in-world · **VERB** observe (count
  fires / read F3) · **answer_kind** `phrase`
- **Setup.** (WORLD-BIBLE §11.1 the dead-shrine, made playable.) Every home in the Hold keeps one
  fire always (FACT 11). At the dead-shrine (Iss's false-coordinate endpoint), the group **observes**
  the one thing wrong: the hearth is *cold all through* — the only one in the Hold let go out. The
  puzzle is noticing: count the lit hearths across the Hold's homes (all lit) against this one (cold).
  Optional F3-instrument layer (PUZZLES.md §1): the F3 "looking-at" readout at the hearth reads a
  block that is *ash*, not *fire* — the diegetic instrument confirms the wrongness. The answer is the
  comprehension made into a word.
- **Answer(s).** `the only fire let go out` (also accept `a cold hearth`, `the fire was not kept`).
  `phrase`.
- **Legible path.** A cold hearth in a world where every other hearth burns is a strong visual
  anomaly (WORLD-BIBLE §11.1: "the cold is the history"). The F3 layer is a bonus for the observant,
  never required.
- **Outcome.** `dead_end` **with teeth** (honest — this is the *false walk*; the surface answers Iss,
  the deep answers the Seventh, WORLD-BIBLE §11.1). The Watcher acknowledges: *"you came all this way
  on a warm man's word. this is what was here."* It yields the *question*, not progress — and that
  question (why was this one home not kept?) is what re-opens the Seventh thread post-catch. **Clusters
  under** `place` / `seventh`.

---

## 9. THE COMPANION (Wren) — one honest reactive beat (NOT a fake puzzle)

Per the-companion.md §6/§7 and the OVERHAUL cohesion gate (§5, §7.1: nothing inert may costume itself
as a puzzle), Wren's one artifact is **forensic proof, not a decode**. Included here for completeness
so no later pass mistakes it for a cipher.

### 9.1 `wren-kept-close` — the tally in his own hand (found object → comprehension)
- **TYPE** NBT-heavy item / found object (but the "solve" is *recognition*, not decoding) · **SURFACE**
  in-world (drops at the reveal) · **VERB** observe (read your own words back) · **answer_kind** `none`
- **Setup.** (the-companion.md §6.) Post-`companion_revealed` only, Wren's tally-book **"kept close"**
  surfaces — a soft-handled inventory of *the group's own real names, plans, fears, inside jokes,
  dates*, in the same warm hand that drew them the safe path on night one. Seeing their own true words
  in his handwriting is the proof no accusation could be (the-companion.md design note: this is where
  the Observer's grounded observations become diegetic proof — the sharp quotes were harvested here).
- **Answer(s).** **None.** There is nothing to submit. It is *comprehension* — the world reacts once
  the group has understood (PUZZLES.md §4 "Comprehension" answer type). Reading it sets no flag the
  group can "solve"; it *is* the reveal's payload.
- **Legible path.** N/A — it is a document, honestly labelled flavor/proof, dropped at a story beat.
  It never poses as a puzzle (cohesion gate).
- **Outcome.** Feeds the reckoning (`reckoning_condemn | understand | free`, the-companion.md §5).
- **Clusters under** the companion thread. **HONEST LABEL: not a puzzle — forensic proof.**

---

## 10. TYPE-DIVERSITY LEDGER (proves the §0 rule + coverage)

17 new puzzles + the 1 honest-proof artifact, across **11** TYPE categories (≥5 required):

| # | puzzle_key | keeper/spine | TYPE | SURFACE | VERB | answer_kind |
|---|---|---|---|---|---|---|
| 1 | `vaun-hoard-sorted` | Vaun | logic (arrange) | in-world | arrange | object |
| 2 | `vaun-bookshelf-tally` | Vaun | numeral/positional (bookshelf) | in-world | set-combination | code |
| 3 | `mara-lectern-lock` | Mara | numeral/positional (lectern-lock) | in-world | arrange | code |
| 4 | `mara-walk-the-map` | Mara | embodied | in-world | perform | behavior |
| 5 | `sella-reflection-bearing` | Sella | visual (reflection) | in-world | reflect | coords |
| 6 | `sella-overlay-lake` | Sella | numeral/positional (lectern-lock) *(retheme, was visual/overlay — puzzle-variety audit)* | in-world | arrange | code |
| 7 | `sella-shore-memorial` | Sella | visual (forced-perspective) | in-world | observe | behavior |
| 8 | `orin-bow-fall-order` | Orin | embodied | in-world | perform | behavior |
| 9 | `orin-banner-heraldry` | Orin | banner-heraldry (key-find) | in-world | decrypt-with-key | phrase |
| 10 | `orin-frame-dials` | Orin | numeral/positional (dials) | in-world | rotate | code |
| 11 | `brann-black-moon-toll` | Brann | audio (morse) + temporal | in-world | listen/wait | phrase |
| 12 | `brann-silence-corridor` | Brann | behavior-heard (sculk) | in-world | perform | behavior |
| 13 | `iss-which-is-true` | Iss | logic/deduction | in-world + web | combine | phrase |
| 14 | `iss-nbt-falsified-entry` | Iss | steganographic (NBT) | in-world → external | research | object→url_token |
| 15 | `iss-bound-word-callback` | Iss | layered (callback) | in-world | combine | callback |
| 16 | `spine-recovered-archive` | spine | steganographic (spectrogram) + research | external → web | research/listen | phrase |
| 17 | `spine-threshold-vault` | spine | asymmetric co-op | in-world | combine+speak | code |
| 18 | `spine-spoken-name` | spine | voice-heard (Observer) | voice → in-world | speak | spoken |
| 19 | `spine-unkept-acrostic` | spine | observation (meta) | in-world | observe | phrase |
| 20 | `spine-cold-hearth-shadow` | spine | observation + F3 | in-world | observe | phrase |
| — | `wren-kept-close` | companion | found object (comprehension) | in-world | observe | none |

TYPE categories covered: **logic/deduction · numeral-positional · visual/spatial · embodied ·
audio · behavior-heard · steganographic · layered/callback · asymmetric-co-op · voice-heard ·
observation/counting** (11). Minecraft-native types used: **map-art forced-perspective,
banner-heraldry, lectern-page redstone lock, chiseled-bookshelf register, calibrated-sculk silence,
reflection, F3-instrument, item-frame dials, NBT-heavy item, map-art overlay, trial-chamber vault**
(11 of the §1-A3 menu). New letter-ciphers added: **0**. Cipher share: 6/23 ≈ 26%.

> **Puzzle-variety audit update:** `sella-overlay-lake` no longer carries the map-art-overlay
> TYPE — it was retired in favor of a lectern-comparator-lock retheme (row 6 above), fixing the
> §11 back-to-back-template violation in the Sella run. The ledger's TYPE-category COUNT (11) and
> Minecraft-native-menu count are unaffected: lectern-page redstone lock was already counted once
> (via `mara-lectern-lock`), and map-art overlay drops out of the *used* list accordingly — the
> menu-coverage claim in the paragraph above should be read as 10 of the §1-A3 menu post-fix, not 11.

## 11. ADJACENCY CHECK (the §0 rule — no two-in-a-row share ≥3 axes)

The salience showrunner surfaces one thread at a time, so strict ordering isn't fixed; but a sane
progression within a keeper never repeats: e.g. Vaun runs **arrange(object) → set-combination(code)**
(differ on TYPE/VERB/ANSWER); Sella runs **reflect(coords) → overlay(coords) → observe(behavior)**
(differ on VERB/TYPE, and the third differs on ANSWER too); Orin runs **perform(behavior) →
decrypt(phrase) → rotate(code)** (all three axes rotate each step). Cross-keeper hops always change
SURFACE (in-world → external → voice). The tone-rotation (OVERHAUL Pillar 2) maps cleanly: Archive
beats = the ciphers + `spine-recovered-archive`; Uncanny = `brann-silence-corridor`,
`spine-spoken-name`, `wren-kept-close`; Warm-Grief = `mara-walk-the-map`, `sella-overlay-lake`,
`orin-frame-dials` (the untainted keeper-memory relief).

> **Puzzle-variety audit finding + fix:** the Sella line above under-states its own repeat — the
> stated "differ on VERB/TYPE" for `reflect(coords) → overlay(coords)` is true on VERB/TYPE but
> BOTH steps still share ANSWER_KIND (`coords`) AND the same "look at something → get a destination
> word → walk there → read a sign" template as the third step, `sella-shore-memorial` — the clearest
> back-to-back template repeat in the whole puzzle set (§0). `sella-overlay-lake` has been retheme'd
> (§4.2) to a numeral/positional lectern-comparator lock (answer_kind `code`, reusing the
> `mara-lectern-lock` producer) so Sella's run now genuinely rotates TYPE + answer_kind at every
> step: **reflect(coords) → arrange(code) → observe(behavior)**. This section's own worked example
> above is left as-authored prose (historical record of the finding); §4.2 and the ledger (§10) carry
> the corrected mechanic.
