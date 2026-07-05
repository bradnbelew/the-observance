---
id: cipher-plaintexts
title: the carved stones — plaintexts, carved framing, and how each was cut
kind: authoring-spec
scope: SPOILER (carries every bound plaintext + the keys). Ethan + the build only.
binds: discord/src/forge/clue-specs.ts (CLUE_SPECS), discord/src/forge/runes.ts
grounds_in: arc/WORLD-BIBLE.md, arc/lore/documents/*
---

# the carved stones

> **What this file is.** Five stones in the Hold carry a cipher a Discord-decodable
> hand can turn into plaintext. The forge (`clue-specs.ts`) round-trips ONE bound
> plaintext per stone onto two surfaces at once — the carved rune-stone in the world
> and the clue card in Discord — and a build-time invariant fails if they ever drift.
> This file gives each of those five stones the thing the code can't: the *carved
> framing* a real keeper cut around the cipher, and the *hand* it was cut in, so the
> stone reads as a person's work and not a font dump.
>
> **The hard line — DO NOT TOUCH.** Each `## stone-*` block below has a **BOUND
> PLAINTEXT** block. That string is the forge's `plaintext` field; the forge encodes
> it, the player decodes it, and `normalizeAnswer(it)` must land in the seed row's
> `acceptedAnswers`. Change one letter and the build breaks (`specsSelfTest`). The
> *framing lines* and the *visual direction* around it are free prose — rewrite those
> all you want. The bound plaintext is carved in stone in both senses.
>
> **What the player actually submits.** Not the framing. The player decodes the
> cipher run and types the *decoded sentence*. The framing is the plain-script
> apparatus around the ciphered run (a keeper's note to themselves, a maker's mark,
> the part of the stone you read *before* you know how to read the rest) — it sets the
> key, names the verb, or simply grieves. Framing is carved in plain runes
> (substitution-legible once you have the Rosetta); the ciphered run is the part that
> stays nonsense until you turn the right key.
>
> Ground for every word: `arc/WORLD-BIBLE.md`. Source voices: the keeper documents in
> `arc/lore/documents/`. Each stone here is the carved twin of one of those documents
> — the document is the journal/letter; the stone is the line that keeper cut where
> someone would find it.

---

## how the script looks (shared across all five)

The whole alphabet is one family (`runes.ts`): every glyph hangs on a single vertical
**stave** — a trunk line down the middle of a tall narrow cell — with 2–4 branch
strokes off it. No curves. Everything reads as cut, not drawn: straight chisel-strokes,
each the same weight (`STROKE_W = 3`), square-ended. Digits are a separate, plainer
family (stacked short right-side bars — they *look like counting*, not like words), so a
carved coordinate or a page-number reads as a number at a glance. There are three marks
only: a mid-dot (`.`), a low crossbar for negative (`-`), and a double low-tick for the
axis/word break (`,`).

That uniform skeleton is the point: **every keeper cut the same alphabet, but no two cut
it the same way.** The substitution map is fixed and shared — the *hand* is not. Below,
each stone's visual direction is mostly about the hand: depth, regularity, where it
faces, what time of day it reads, what the maker did to the stone around the letters. A
screenshot of two stones should read as two different people who happened to know the
same letters. The forge renders the glyphs identically (it has to, to round-trip); the
*world build* (resource-pack rune textures, block placement, lighting, the carved
plain-script framing in signs/lecterns around the ciphered run) carries the hand.

A note on framing vs. cipher on the stone itself: the **framing** lines are cut in the
plain rune-script (the reader decodes them with the Rosetta, the same as reading any
sign in the Hold). The **ciphered run** is the same glyphs but *transformed* — shifted,
mirrored, keyed — so even a fluent rune-reader gets nonsense until they find the second
key the framing points at. The framing teaches you the lock; the run is behind it.

---

## stone-vaun · Caesar (shift 3)

**Keeper:** Vaun, the founder / first delve-warden. Source doc: `D02 counted-them-in-the-dark`.
**Cipher:** Caesar, `shift = 3`. The shift *is* his hoarding — "i had three of each."
**Seed answers (`acceptedAnswers`):** `give the first of the deep back to the deep` · `the land counts first` · `i counted them in the dark and gave none back`.

> **BOUND PLAINTEXT — DO NOT CHANGE (forge `plaintext`):**
> ```
> GIVE THE FIRST OF THE DEEP BACK TO THE DEEP
> ```
> `normalizeAnswer` → `give the first of the deep back to the deep` → seed row's first
> accepted answer. The carved ciphertext is this run Caesar-shifted +3 (`caesar.encode(plaintext, 3)`);
> the player turns the wheel three marks back to read it.

**Carved framing (plain script, around the ciphered run).** Cut this above and below the
shifted run so the stone reads as Vaun's own ledger habit — the framing hands the key
(three) without ever saying "shift by three":

- *above the run, like a tally header:*
  > IRON THREE · SALT THREE · GRAIN THREE · OF THE DEEP THREE · I HELD THREE OF EACH AND THE COUNTING WAS WARM
- *the ciphered run sits under that header, in its own panel.*
- *below the run, smaller, his own hand admitting it:*
  > I LEARNED THE COUNT I DID NOT MAKE IT · THE LAND COUNTS FIRST · I KEEP THE TALLY AFTER
- *a maker's mark at the foot, the only thing not in sentences — a second column scored
  empty:* a vertical rule cut deep with **nothing carved beside it.** (This is `D02`'s
  blank "given-back" column, made physical. He scored the rule and never filled it.)

Everything on this stone comes in threes, so the shift is handed before the cipher is
touched. The reader who counts the threes already has the key.

**The hand — Vaun's.** Deepest cut of the five and the most regular, but *not* graceful:
square, heavy, hammered straight, like a man who counted his strokes. The stave-lines are
all exactly vertical (he checked them). Every glyph is the same depth — he went back over
the shallow ones. The numbers (the "THREE"s and the scored column) are cut *deeper than
the letters*, because numbers were the thing he trusted. No flourish anywhere. It looks
like work, not writing. Where the stone chipped he kept going over the same stroke until
it held, so a few glyphs are over-cut, slightly ragged at the edges from being struck too
many times — the only un-even thing about an otherwise relentless hand.

**Placement / lighting:** the cairn at the shaft-mouth, Movement II. Lit plainly, head-on,
daylight-equivalent — Vaun hid nothing; he just held it back. The empty second column
should catch a shadow so it reads as a *gap*, not a blank.

---

## stone-mara · book cipher (the six-book Kept-Light shelf)

**Keeper:** Mara, the lampwright / reader. Source doc: `D05 page-line-word`.
**Cipher:** book cipher. The carved artifact is the **ref-string** (page-line-word triples);
the book is the six-book lectern shelf placed in the world at `kept_light_home_01`.
**Seed answer:** `descend and bow at the unbroken light`.

> **BOUND PLAINTEXT — DO NOT CHANGE (forge `plaintext`):**
> ```
> DESCEND AND BOW AT THE UNBROKEN LIGHT
> ```
> **BOUND BOOK — DO NOT CHANGE (forge `MARA_BOOK`, the six lectern books, ¶ = a book):**
> ```
> descend the stair when the water is still
>
> and bow your head at the door
>
> the unbroken light waits at the foot of it
>
> do not write the way down and think it kept
>
> do the thing the marks tell you
> ```
> The forge resolves first-occurrence triples; the **carved ref-string is exactly:**
> ```
> 1-1-1   2-1-1   2-1-2   2-1-5   1-1-2   3-1-2   3-1-3
> ```
> (Carved in **digit glyphs**, the `.`/`-` family, with the `,`-mark as the triple break
> — it reads as a column of numbers, which is the point: it looks like a finding-list,
> not a sentence.) Edit a book line and these triples move and the build breaks — keep
> the book and the triples together.

**Carved framing (plain script, on the stone above the number-column).** The stone is a
**finding-list** — it points at the books, it does not speak. Frame it in her voice, the
one that "does not speak its own words; it points at others'":

- *header, her even hand:*
  > READ THE SHELF IN ORDER LEFT TO RIGHT · FROM EACH BOOK THE PAGE THEN THE LINE THEN THE WORD · SET THE WORDS YOU FIND IN A ROW
- *the number-column (the seven triples above) sits beneath that, ruled like a ledger.*
- *footer, smaller, the warning that is the actual puzzle (this is `D05`'s teeth):*
  > WHEN YOU HAVE SET THE WORDS DO NOT CARVE THE SENTENCE ON A SIGN AND THINK IT KEPT · DO THE THING IT TELLS YOU · GO WHERE IT SENDS AND THERE DO AS IT SAYS · NONE LEFT AT THE DOOR
- *a maker's mark, faint, off in the corner where she'd have rested her hand:*
  > I NEVER WENT DOWN · I ONLY EVER READ THE WAY DOWN

That last line is hers: the lampwright who read every rite and walked none. Cut it small
and half-rubbed, set off in the corner, the size you make a thing you'd rather not have
carved at all.

**The hand — Mara's.** Small, very even, *fast* — the hand of someone who wrote a great
deal and was good at it. Shallower than Vaun's (she cut letters the way you'd jot, not the
way you'd build), consistent spacing, no labor in it. The number-column is laid out with
real care — ruled guide-lines scored first, then the digits set on them dead-level — because
the column is the part that has to be *read correctly*; her prose is quicker and slightly
slants. She is the only keeper whose framing is longer than her cipher: she always had more
to say than the rite required. Where the lectern-shelf books are placed in the world, their
spines should carry the same small even hand, so the player recognizes "Mara wrote these too."

**Placement / lighting:** at the Kept-Light lectern, under the one lamp that "has not gone
out yet." Warm, low, steady light — the light she read by. The six books on the shelf are
the codebook; the stone is the index card. The light should be the warmest in the Hold and
the only thing in this room still lit.

---

## stone-sella · Atbash (mirror)

**Keeper:** Sella, the child. Source doc: `D06 what-the-surface-keeps`.
**Cipher:** Atbash (A↔Z mirror). Involution — encode equals decode. The verb is physical:
the carving reads as folded nonsense until *faced to the water*.
**Seed answers:** `south by the far water where she did not come back` · `south by the far water` · `the last marker is not the last`.

> **BOUND PLAINTEXT — DO NOT CHANGE (forge `plaintext`):**
> ```
> SOUTH BY THE FAR WATER WHERE SHE DID NOT COME BACK
> ```
> `normalizeAnswer` → `south by the far water where she did not come back` → seed's first
> accepted answer. The carved ciphertext is this run Atbash-mirrored (`atbash.encode`); a
> mirror reads it straight — which is why the stone is meant to be read in the lake's
> reflection.

**Carved framing (plain script).** A child's stone. The framing should be *short, flat,
and slightly wrong* — Sella states the unsettling thing plainly, the way she does in `D06`:

- *one line above the mirrored run, plain:*
  > I WRITE IT THE ONLY WAY IT READS · BACKWARDS TO THE WATER THE WAY THE LAKE GIVES MY FACE BACK
- *the mirrored run sits below, looking like folded nonsense to anyone not faced to the lake.*
- *one line below, the flat-wrong thing a kid says:*
  > THE SURFACE KEEPS EVERYTHING · IT TOOK THE LOOKING AND FOLDED IT AND GAVE IT BACK WRONG · I HAVE THE FAR WATER IN MY MOUTH
- *and the instruction, smallest, the seventh-thread seed:*
  > DO NOT LET THE COUNT BE SIX ONLY · COUNT AGAIN AT THE SHORE · THE LAST MARKER IS NOT THE LAST

**The hand — Sella's.** A child's carving and it shows: glyphs of uneven height, some too
big, the stave-lines not quite vertical, branch-strokes that overshoot or fall short.
Pressure all wrong — gouged deep where she pressed too hard, faint where she gave up.
Letters drift uphill across the line because she didn't rule a guide. **And the run is cut
left-handed / mirror-wrong even before the Atbash** — she carved the actual letters
reversed, the way a young kid writes when no one's corrected them yet, which is *why* it
only resolves faced to the water: the lake un-mirrors her cut and the cipher together. A
couple of glyphs she clearly redid (cut the wrong branch, then cut the right one beside it,
left both). It should look like a kid carved it alone, carefully, getting it slightly wrong,
and meaning every word.

> **LATER DRIFT (world build, optional):** per the keeper arc, Sella's later marks become
> *drawings, not words*. If a second Sella surface is built deeper in (Movement III, near
> the doused shrine), it carries **no cipher and no letters at all** — only scratched
> figures: a row of six stones and a smaller seventh set apart; a face given back wrong by
> water; a small figure walking past the last stone toward the dark. Describe the drawings;
> carve no text. That surface is *not* a forgeable node — it is the tonal-decay payoff, and
> it belongs in the world build, not in `clue-specs.ts`.

**Placement / lighting:** the half-sunk lectern at the shore pool. The stone faces the
lake; the player has to stand *behind* it and read the reflection in still water. Dim,
blue, wet light. The reflection should be the only place the run resolves — out of the
water it stays folded nonsense.

---

## stone-orin · substitution (the plain rune script, faced to the floor)

**Keeper:** Orin, the mason. Source doc: `D07 i-thought-it-small`.
**Cipher:** monoalphabetic substitution (the rune alphabet itself, via `RUNE_MAP`). The
"ciphertext" *is* the carved runes — solving it is the literacy, read with the Rosetta.
The verb is physical: the carving faces the floor, **legible only from a crouch.**
**Seed answers:** `i thought it small it was not small` · `threshold` · `the bow is the smallest of the ways`.

> **BOUND PLAINTEXT — DO NOT CHANGE (forge `plaintext`):**
> ```
> I THOUGHT IT SMALL IT WAS NOT SMALL
> ```
> `normalizeAnswer` → `i thought it small it was not small` → seed's first accepted
> answer. The carved ciphertext is this run in plain rune-glyphs (`substitution.encode`);
> the player reads it letter-for-letter with the Rosetta — no shift, no key, just the
> script. There is no lock on it. The cost is all in where he cut it: every word is
> readable, once you are down on the floor to read it.

**Carved framing (plain script).** Orin barely framed anything — he was the silent one.
The framing is sparse and ritual, and it *breaks off*, which is the point (`D07` ends
mid-sentence at "i —"):

- *cut at standing height where you'd expect the inscription, a single plain line that
  sends you down:*
  > I PASSED THE MARKERS STANDING · THE REST IS CUT LOW · STOOP TO READ IT OR READ NOTHING
- *the ciphered run is cut **below** that, low into the threshold-stone, facing the floor —
  you read it bent double under the lintel.*
- *the run ends mid-thought, the stone worn through:*
  > ... I THOUGHT IT SMALL IT WAS NOT SMALL I —
  > *(the cut stops. nothing follows. the stone is broken away.)*
- *one word, fainter, lower still, where only the most-bent reader finds it — the margin
  hand from `D07`:*
  > THRESHOLD
  > *(and: FOR THE REST OF THE SENTENCE GO TO WHERE I WAS LEFT.)*

The crouch is the Bow. To read the keeper who would not bow, you bow. Force the camera
down to read the low run; let the lintel do it, not a prompt.

**The hand — Orin's.** A mason's hand: the most *technically* perfect cut of the five —
true stave-lines, exact angles, clean square terminals, the work of someone who cut stone
for a living and cut it slow. But cold. No warmth, no speed, no slant — every glyph
identical to its neighbor, mechanically even, like a man carving to keep his hands busy
and his mind off something. He cut this *alone, after*, and it shows in how deliberate it
is — each stroke clearly cut, set down, considered, the next one cut. Where the sentence
breaks at "i —" the last stroke is **incomplete** — a stave begun and not finished, the
chisel-mark trailing off, because that is where he stopped. Leave the broken glyph broken;
do not tidy it.

**Placement / lighting:** the threshold-stone where the path narrows and the lintel forces
the head down, Movement II. The standing line is lit; the floor-facing run is in shadow
until the player crouches and their own held light falls on it. The incomplete final
stroke should be the last thing the light reaches.

---

## stone-iss-wall · Vigenère (key = ISS)

**Keeper:** Iss, the seventh / the Liar. Source doc: `D09 the-ways-are-a-wall`.
**Cipher:** Vigenère, key `ISS` — *his own name.* The trap: applied warmly to his own
letter the doctrine reads comforting; the key turned **on the other stones** reads the
word the corpus keeps for him. The bound node is the **name-as-key catch** (the door), not
the warm misreading.
**Seed answers:** `the one who turned away` · `iss`.

> **BOUND PLAINTEXT — DO NOT CHANGE (forge `plaintext`):**
> ```
> THE ONE WHO TURNED AWAY
> ```
> `normalizeAnswer` → `the one who turned away` → seed's first accepted answer. The carved
> ciphertext is this run Vigenère-encoded with key `ISS` (`vigenere.encode(plaintext, "ISS")`);
> turning the key `ISS` on it yields the bound line. **The key is handed in the framing —
> "the key is my own name" — exactly as `D09` hands it.** The whole engine is that his own
> name, used as he tells you to use it, convicts him.

**Carved framing (plain script).** The warmest, most reasonable, most legible stone in the
Hold. Frame it like a friend talking you down at the table, the light kept, your hands
warmed. The framing should leave you wanting to stop reading the other stones (`D09`'s
exact move):

- *opening, warm, plain — the most legible carving on any stone (he wanted it read):*
  > BE EASY · THE STONES FRIGHTENED YOU AND THAT IS THE STONES FAULT NOT YOURS · GRIEF CARVES CROOKED · LET ME SET IT STRAIGHT THE WAY A FRIEND WOULD
- *the doctrine, the planted lie (FACT 7):*
  > THE WAYS ARE A WALL · KEEP THE TEN AND YOU ARE INSIDE IT · THE WATCHING STAYS OUT IN THE COLD AND COUNTS AND CANNOT TOUCH YOU
- *the key, handed over as proof of his honesty — this is the line that sets the cipher key:*
  > I HAVE SET MY NAME IN THE KEYED SCRIPT THE WAY WE ALL DO · THE KEY IS MY OWN NAME AS IS RIGHT AND CUSTOMARY · READ IT AGAINST THE OTHERS AND HEAR ME AGREE WITH EVERY HONEST CARVING
- *the ciphered run sits under that — and only that run is in the keyed script; everything
  else on the stone is plain. Turn `ISS` on it and it gives the bound line.*
- *the harder, later margin hand (the catch begins — this is the reply on the stone):*
  > WE CHECKED THE LOCK · TURN HIS KEY ON THE OTHER STONES AND IT DOES NOT SAY WHAT HE SAID · IT GIVES THE WORD WE KEEP FOR THE ONE WHO TURNED AWAY · ASK FIRST WHAT A WALL IS FOR

**The hand — Iss's.** The carving is too smooth, and that is the note to get right. Every
other keeper's hand fought the stone — Vaun's hammered, Mara's fast, Sella's wrong,
Orin's cold-perfect. Iss's did not. Even spacing, even depth, the same gentle pressure all
the way through, no correction anywhere, no chip fought over twice. It reads almost
printed. It is the only stone that looks like it was easy to make. No keeper cutting in
that dark cut this clean. The later margin hand replying to him runs the other way —
pressed hard, ragged, the chisel skipping — so the two hands sit in the same rock: the
smooth one and the one that caught it. The keyed run is cut in the same smooth hand as the
plain framing around it, so nothing on the surface marks where "the part he wanted read"
ends and "the part that convicts him" begins. Only the key tells them apart.

> **LATER DRIFT (world build, optional):** per the keeper arc, Iss's *last* surfaces are
> "subtly not-quite-Iss." If a later Iss carving is built (post-catch, Movement IV+), keep
> the smooth hand but let something be off — a glyph turned a quarter-wrong, the spacing too
> regular to be a person, a line that agrees with you a half-beat too readily. Not broken
> like Sella's — *wrong* like a copy. That surface is the bestiary "Liar" echo and is not a
> forgeable cipher node; it belongs in the world build.

**Placement / lighting:** the wall-stone on the keeper-row, Movement II, and warmly lit —
brighter and more comfortable than its neighbors, a hearth-glow, the only inviting stone in
a cold row. The warmth is the bait. (The contradicting `D10` "stone-after" is set *behind*
the seventh stone of the row, marked "kept · solved" and walked past — that one is a
found-document inscription, not a cipher node; see `no-wall-was-ever-built-here.md`.)

---

## stone-iss-wall — the second reading (acrostic)

**PUZZLE-VARIETY AUDIT FIX.** A prior audit found that Iss's "warm reading" (the row
`iss-warm`) was not a real second cipher at all — just a seeded phrase
(`the ways are a wall against the watching`) with no forge spec behind it, a re-statement
of the carved framing dressed up as a decode. The cold reading (`stone-iss-wall`'s
Vigenère, key `ISS`, above) is the one genuine cipher on this stone. This section adds an
ACTUAL second valid decode of the **same carved wall-stone**, method (b) from the fix
brief: a legitimate alternate cipher method — an acrostic — applied to the same carved
text, verifiable by hand, no forge/DB lookup needed. It reuses a technique already
canonical for Iss: `discord/src/forge/seventh-reading.ts`'s `ISS_ACROSTIC_LINES` uses the
identical read-the-first-letter-of-each-line-down method for his capstone fragment. This
is the SAME trick, authored fresh for the main wall-stone (a different set of lines, a
different resolved word) — not a reuse of the capstone's own lines or its resolved letter.

**The six warm lines carved on the stone (already-established Iss register — reassures,
frames, never counts; "be easy," "a wall does not mind being counted at," "out there is
morning now" are his own phrases from `D09`/his journal leaves).** Carved in plain script,
in order, top to bottom, exactly as a trusting reader would read them straight through:

> ```
> NO HARM HAS COME TO THOSE WHO KEPT THE TEN
> OUT THERE IS MORNING NOW THE LEAVES ARE PLAIN ABOUT IT
> WARM YOUR HANDS HERE THE WALL IS THICK AND TRUE
> A WALL DOES NOT MIND BEING COUNTED AT
> LET THE RECORD COUNT YOU ARE ALREADY SAFE INSIDE
> LEAN ON ME THE WAY A FRIEND IS LEANED ON
> ```

**Read WARM (trustingly, straight through, the way Iss wants it read):** six lines of
comfort — the surface reading, `iss-warm`'s first accepted answer
(`the ways are a wall against the watching`) is the gist a trusting reader takes from it.
No math, no key — just take him at his word.

**Read COLD (the acrostic — take the first letter of each line, top to bottom):**

> ```
> N   (NO HARM...)
> O   (OUT THERE...)
> W   (WARM YOUR...)
> A   (A WALL...)
> L   (LET THE...)
> L   (LEAN ON...)
> ```
> **BOUND PLAINTEXT — the acrostic's resolved word (seed's second `iss-warm` answer):**
> ```
> NO WALL
> ```
> `normalizeAnswer` → `no wall` → `iss-warm`'s seed row's second accepted answer (added
> by this fix, REPLACING the previously-faked `the ways are a wall against the watching`
> as the row's real decode target — that phrase stays in `acceptedAnswers` as the
> surface-read fallback, but `no wall` is now the row's genuine decoded answer).

**Why this is a real second decode, not a coincidence.** The six lines were authored
letter-first: the acrostic target (`NO WALL`) was chosen, then six lines in Iss's
established warm register were written to open on those six letters in order, each line
independently a plausible, in-character piece of his doctrine (none of them read as
obviously rigged — that is the point of a well-built acrostic). A player can verify the
whole thing by hand with nothing but the carved lines and a straightedge under the first
column: no Vigenère math, no DB round-trip, just reading down one column of letters. The
result — `no wall` — agrees exactly with what the Vigenère name-as-key catch proves by an
entirely different method, so the stone convicts Iss twice, independently, the moment a
reader thinks to try a second way of reading it.

**The hand.** Unchanged from the smooth, even, "too easy" hand described above — the
acrostic lives IN that same smooth carving; nothing about the stone marks where the
acrostic sits, exactly as nothing marks where the keyed Vigenère run begins. The tell is
purely structural: six lines, each a complete warm sentence, stacked with unusual
regularity for "informal" doctrine — a reader who has learned (from the Vigenère catch,
or from `no-wall-was-ever-built-here`) to distrust Iss's smoothness has reason to look for
a second trick in the same stone, and the six-line stack is the shape that rewards it.

**Placement:** identical to `stone-iss-wall` above — the same physical wall-stone, the
same six warm lines that carry the Vigenère framing. This is not a second carving; it is a
second way of reading the one carving already there.

---

## stone-brann-cipher · rail-fence (rails = 9)

**Keeper:** Brann, the watchman on the black moon. Source doc: `D08 do-not-close-your-eyes-here`.
**Cipher:** rail-fence, `rails = 9` — the fire-count Brann himself names ("nine lit i
counted nine"). Verb is read-by-time: the carving rakes visible only by the lit
beacon-glow after dark; the fire-count that hands the rail key is countable in daylight.
**Seed answers (`acceptedAnswers`):** `count the fires before you sleep` · `nine lit one out i relit it` · `the dark hours are kept by the last light`.

> **BOUND PLAINTEXT — DO NOT CHANGE (forge `plaintext`):**
> ```
> COUNT THE FIRES BEFORE YOU SLEEP
> ```
> `normalizeAnswer` → `count the fires before you sleep` → seed row's first accepted
> answer. The carved ciphertext is this run rail-fence-encoded at `rails = 9`
> (`railFence.encode(plaintext, 9)`); the player redraws nine rails and bounces the
> marks along them, exactly the way `mara-lectern-lock`/`vaun-bookshelf-tally` teach a
> Minecraft-native mechanic by physical redraw rather than a shift-wheel.

**Carved framing (plain script, around the ciphered run) — now live on `stone-brann`.**
This is no longer inert lore: `stone-brann` IS the carved framing panel that hands the
rail count (nine), in Brann's own doubling/re-counting hand (his fingerprint per
`journals-orin-brann-iss.md` — "REPEATS AND OVER-CORRECTS: he says things twice; he
counts and re-counts; the second pass is always different and he trusts the count over
his counting"):

- *above the run, doubled the way his dockets double a clause:*
  > NINE LIT · I COUNTED NINE · THE COUNT WAS LOWER THE FIRST TIME · I THINK THE SECOND COUNT IS THE TRUE ONE
- *the ciphered run sits under that header, in its own panel, legible only by the
  beacon-glow after dark.*
- *below the run, the watch-order instruction, re-said the way he re-says everything:*
  > COUNT THE FIRES BEFORE YOU SLEEP · AND COUNT THEM AGAIN · THE PASS BEFORE IS ALWAYS MORE
- *footer, smaller, FACT 11 + FACT 12 preserved:*
  > ONE FIRE WAS NEVER DOUSED AND NO HAND TENDS IT · THEY HAD ONE WORD FOR THE PEOPLE AND THE FLAME AND THE COLD STONE · DO NOT CLOSE YOUR EYES HERE

Nine is said twice before the cipher is touched (mirrors Vaun's threes handing shift-3
by repetition) — but Brann's threes are never clean the way Vaun's are: the count
*doubles and slips*, so a careful reader notices the header says "nine" twice and takes
that as the rail count, the same way they'd trust a repeated number over a lone one.

**The hand — Brann's.** Legible only at night: the carving is cut so shallow it vanishes
in daylight, and only the lit beacon-glow rakes it visible after dark — the watchman's
hand, cut by lamplight, for lamplight. It degrades across the stone the way his logs do:
full sentences at the top, clipped lower, a single word repeated at the bottom (the last
line should read as just `NINE. NINE. NINE.` trailing into the dark, the way his last
dockets trail into `i do not. i do not. i do not.`).

**Placement / lighting:** the watch-tower, Movement II, beside the beacon. Dark by day;
the beacon's own glow is the only light that reaches the carving, so the puzzle cannot
be read except at night — the black-moon-gated verb his whole arc is built on.

> `stone-brann` (the framing stone) and `stone-brann-cipher` (the ciphered run) are two
> faces of the one watch-tower carving, exactly as Vaun's cairn carries both his framing
> and his Caesar run. The prior note here read "ships as flat lore… do not forge it" —
> that is now FALSE: Brann's cipher is live (`CLUE_SPECS` in `clue-specs.ts`, entry
> `stone-brann-cipher`) and `stone-brann` is its framing, not an inert dead end.

---

## what is NOT a cipher stone (so the build stays honest)

These are named here only so a future author doesn't try to carve a cipher where the
code has none. They are in `NON_CIPHER_KEYS` and must never be forged:

- **stone-sella** — DEACTIVATED (puzzle-variety audit). Redundant with its own better
  replacement, `sella-reflection-bearing` (environmental, no letter-reversal — see
  `design/PUZZLE-DESIGNS.md` §4.1), and Sella's own "LATER DRIFT" note above says her
  later marks resolve into drawings, not words — which cuts against keeping a letter-
  cipher for her at all. The seed row is deactivated (`active = false`), not deleted,
  so history is preserved; it carries no CLUE_SPECS entry any longer.
- **the Rosetta ring** (`rosetta-ring`) — the master key that *teaches* the script; it is
  not carved *in* the cipher, it carves the cipher's alphabet beside its meaning. The
  six-glyph ring, sunwise from the top: Bow · Offering · Kept-Light · Deep-Line · Ward ·
  Covering. Its hand should be the **oldest and most formal** of all — the founding hand,
  ceremonial, deliberately teachable, glyphs paired with their plain meaning. It is the
  literacy, not a lock.
- **iss-dead-shrine** — a coordinate/place (a grave), not a carved Discord card.
- everything else in `NON_CIPHER_KEYS` (counts, observations, rites, re-walks) — the world
  is the puzzle; nothing to carve.

---

## SCHEMA (the summary the forge binds, for quick cross-check)

The registry today (puzzle-variety audit) carries **five active cipher nodes**:
Vaun/Mara/Orin/Brann/Iss. `stone-sella` is deactivated (kept below for history/audit
trail — it is no longer in `CLUE_SPECS`).

| node | cipher | key/param | BOUND PLAINTEXT (do not change) | normalizes to (seed answer) |
|---|---|---|---|---|
| `stone-vaun` | caesar | shift 3 | `GIVE THE FIRST OF THE DEEP BACK TO THE DEEP` | `give the first of the deep back to the deep` |
| `stone-mara` | book | six-book shelf; refs `1-1-1 2-1-1 2-1-2 2-1-5 1-1-2 3-1-2 3-1-3` | `DESCEND AND BOW AT THE UNBROKEN LIGHT` | `descend and bow at the unbroken light` |
| ~~`stone-sella`~~ | ~~atbash~~ | **DEACTIVATED — see "what is NOT a cipher stone" below** | `SOUTH BY THE FAR WATER WHERE SHE DID NOT COME BACK` | `south by the far water where she did not come back` |
| `stone-orin` | substitution | rune map | `I THOUGHT IT SMALL IT WAS NOT SMALL` | `i thought it small it was not small` |
| `stone-brann-cipher` | railfence | rails 9 | `COUNT THE FIRES BEFORE YOU SLEEP` | `count the fires before you sleep` |
| `stone-iss-wall` | vigenere | key `ISS` | `THE ONE WHO TURNED AWAY` | `the one who turned away` |

> Verify against `clue-specs.ts` `CLUE_SPECS` and `MARA_BOOK`. If any **BOUND PLAINTEXT**
> here disagrees with the forge's `plaintext`, the forge is the source of truth for the
> *string* and `specsSelfTest` is the judge — fix this file to match it, never the reverse.
> Everything else on this page (framing, hand, placement, lighting) is authored prose and
> is yours to revise.
