# THE OBSERVANCE — TRAVEL DESTINATIONS (the longevity layer · side breadth)

> **DRAFT / DESIGN — not the live seed.** Authored against LONGEVITY.md §2 (the TRAVEL pillar:
> "25–35 sited rumor→verify destinations… arriving often *contradicts* the rumor"). This file
> delivers **18** of them: rumored places out in the Hold/world a group hears about, then must
> physically walk to and verify. They are **side breadth** — they pay lore / atmosphere / items /
> time and **GATE NOTHING** (the `side_quests.gates_progress = false` invariant, migration 0005).
> A pooling ARG group shares the rumor in a second; it cannot pool the 1–3k-block walk. That walk
> is the longevity.
>
> **What this is NOT.** None of these is a spine node, a cipher gate, or a Recovery-Archive thread
> beat the arc waits on. Removing all 18 must leave the main reconstruction unbroken. Only one is a
> blunt false lead; the rest may disappoint a hope, but must still teach a rule, expose a character,
> or point forward. Optional play spends trust, so no long walk is allowed to be merely empty.
>
> **Grounding.** Every place sits in the bible's geography (`arc/WORLD-BIBLE.md` §1) and reuses the
> corpus's exact nouns: the **Mouth/Threshold**, the **Warrens** (mark 4), the **Lamp-works** (mark 9,
> the deep-bird coops), the **Deep Market** (mark 14, 18 stalls), the **Cisterns / Cistern 7** (mark
> 21, the cairn at the shaft-mouth), the **Stair foot** (mark 30, the markers), the **Deep Line** (mark
> 33, the third bay), the sealed **Undercroft**; topside: the lost **Mouth**, **Sella's far water /
> shore pool** off the far-west path, the **doused/dead shrine** west-and-down. Names reused exactly:
> Vaun, Mara, Sella, Orin, Brann, Iss, the seventh (unwritten); the surface five Aro, Wenna, Coll,
> Dob, Old Pell; the roles (Lamp-Registrar, Quartermaster, WARDEN-3, the Counter of the Kept, the
> Survey).
>
> **Voice law (the de-slop law, from the corpus).** Every rumor is in a **Set-A** mouth (modern-rough:
> contractions, capitals, named feelings) or a found sign/letter (the corpus's flat hand). Every
> Watcher/record line is **Set-B** (lowercase, sparse, certain, no contraction, no exclamation, no
> named feeling). What you FIND is shown through concrete mundane detail + omission — never narrated
> emotion, never "a testament to," "the air was thick," "little did they know," tidy bows, or
> three-adjective lists. The two voice sets must never bleed.
>
> **Reference resolution (HARD).** Every `thread_key` below is one of THREADS
> (who/place/happened/surface/human, forge/canon.ts). Every `custom_key` referenced is a
> the_-prefixed CUSTOM_KEY. Every keeper named is a KEEPERS name. A reference that does not resolve
> is a bug — the schema table at the foot lists each so it can be checked.

---

## How a destination works (the rumor→verify loop)

```
   1. RUMOR        an NPC line / a sign / a letter draws a DOTTED LINE to a place not at spawn.
                   (heard secondhand → a thread_card of card_kind='rumor', anchored at the silhouette.)
   2. WALK         someone physically goes there (1–3k blocks). pooling the rumor does not move feet.
   3. ARRIVE       a small authored TABLEAU + 1–2 fragments. the rumor flips:
                     → VERIFIED    (card_kind 'verified')   the rumor was true. it pays.
                     → CONTRADICTED (card_kind 'contradicted') the rumor was false, but the site
                                    still pays proof, a rule, a character turn, or a breadcrumb.
   4. PAY          lore / atmosphere / an item / time. NEVER a spine unlock. the way goes on without it.
```

**Tiers (migration 0005 `side_quests.tier`):**
- **ambient** — just *found*. No rumor needed to point at it; you stumble on it walking. No cipher.
- **rumored** — a dotted line points at it (an NPC/sign/letter). You go to verify. No cipher.
- **keyed** — needs a small **side-cipher** to read its payoff (a second, optional lock — never the
  five spine ciphers; a one-off local device described per destination). The keyed ones are the
  deepest pay and the rarest.

**The dead-lead budget.** Of the 18, **one is a blunt false lead** (D03). D06, D09, D12, and D17
may contradict the *hope* inside a rumor, but each must still pay concrete evidence, a lore rule, a
character turn, or a forward breadcrumb. The speedrun resistance is uncertainty, not contempt: a group
cannot tell every live lead from every cold proof without walking, but a careful walk must always
make the ARG feel more real.

---

## THE DESTINATIONS

> Each: **name · tier · thread · ~est_minutes · rumor (how they hear) · what they find on arrival ·
> pays · proposed voice key(s)**. est_minutes = authored walk + scene + (keyed: decode), per the
> HOURS ledger (LONGEVITY.md §6). Totals at the foot.

---

### D01 — the warm stair (the third lamp) · tier: rumored · thread: who · ~14 min

**Rumor (Wenna, `rumor`/`react_good`):**
> "There's a stair in the old lamp-house where one lamp's still warm, they say. Warm, after all
> this time. My gran went on about a third lamp on a stair that someone never got the oil for. Sad
> little thing to remember a person by, a lamp that stayed out."

**What they find:** the Lamp-works stair (mark 9), forty-one steps. The **third lamp on the ninth
step** is cold and empty — the bracket scorched, the oil-cup dry, a jar-ring stain on the stone
below it where a jar was meant to stand and never did. Mara's small even hand is scratched on the
riser, not carved: *we kept them all lit for so long.* The rumor said *warm*; it is cold. (The one
that stayed warm is elsewhere — entry 5, D11 below.) **Contradiction is gentle:** Wenna mis-remembered
*which* lamp; the warmth was never here.

**Pays:** the L01/L02 grief (Mara asking Vaun for one jar he would not give). A thread_card under
**who**. No item.

**Voice keys (proposed):**
- `voice.dest.warmStair.find` →
  `the third lamp on the ninth step is cold. the cup is dry. there is a ring on the stone where a jar was to stand. forty-one steps were counted by a hand that never counted before. the lamp was not kept for want of one jar.`

---

### D02 — the empty cairn at the shaft-mouth · tier: ambient · thread: who · ~10 min

**Rumor:** none — you pass it descending to the Cisterns (mark 21). It is *found*, not pointed at.
(A sign by it, the Quartermaster's hand: *RETURNS TO THE DEEP · LEAVE THE FIRST OF THE DEEP HERE.*)

**What they find:** the offering-cairn, a low ring of dressed stone, **empty** — swept, not
collapsed. A scored ledger-rule cut into the rim stone with **nothing beside it** (Vaun's blank
given-back column, made physical). Orin's three later-set stones sit squared at one side, the only
full part of it. **If a player leaves an item on the cairn** (any item), the Watcher notes it — this
is a soft teach of **the_offering**, not a gate.

**Pays:** the_offering taught by example (Vaun who counted and gave nothing; Orin who mended it). A
thread_card under **who**. Optional: a player who leaves an offering earns a fond Watcher line.

**Voice keys (proposed):**
- `voice.dest.cairn.find` →
  `the cairn is empty and was not pulled down. it was kept empty. one rule is scored into the rim with nothing carved beside it. three stones at the side are squared by a later hand. the first of the deep was owed here and one of them never paid it.`
- `voice.dest.cairn.offered` *(if a player leaves an item)* →
  `something is given back. the column is not so empty as it was. the deep keeps a column too.`

---

### D03 — Aro's "warm town" (the false lead) · tier: rumored · thread: place · ~20 min · **CONTRADICTION**

**Rumor (Aro, `rumor`/`lie` — confident, specific, wrong):**
> "Way I heard it, there's a whole town down there. Lamps still burning, people living fat off the
> warm while we freeze up here. East of the big stair, past the market. That's why nobody comes back
> — it's *nice* down there. Go east at the Deep Market and you'll smell the bread."

**What they find:** east of the Deep Market the gallery **dead-ends in rubble** — a collapse, old,
the wrong-cut deep stone (too tall, dug by hands that stopped squaring things). No town. No bread.
A single overturned market-stall, the one R04 lists as *a stall that sells nothing and is kept lit
for the dead* — its lamp long out. Pinned to the rubble, a WARDEN-3 notice in the flat hand: *the
way east is closed. it was closed in the breaking. no town stands beyond it. it is entered that
hands keep going to look for one.* **The rumor is a lie Aro tells warm.** The world contradicts it
flatly.

**Pays:** atmosphere (the wrong-scaled deep; the "people just stayed and it's nice" lie that the
whole horror inverts) + the lesson that **Aro lies** (corroborated later when the painted line / black
moon / unspoken all contradict him) + the first hard Deep-Line warning. A **contradicted** card under
**place**. No item, but the site must leave a concrete warning players can use; this is not a prank
walk.

**Voice keys (proposed):**
- `voice.dest.warmTown.find` →
  `there is no town east of the market. the gallery falls in on itself, old, dug too tall for people. one stall is overturned, the one that sold nothing and was kept lit for the dead, and its lamp is out. a notice says hands keep coming to look for a town that the breaking closed. you are not the first feet wasted here.`

---

### D04 — the school-stand in the Warrens · tier: ambient · thread: human · ~9 min

**Rumor:** none — found in the Warrens (mark 4), the dwellings. (A worn board: *THE SCHOOL-STAND ·
MIND THE LAMP · BOW AT THE ROW.*)

**What they find:** a low bench, a slate wall, chalk-stubs in a tin. On the slate, a child's hand
(Sella's register, slightly wrong): a row of **six little stones**, and off at the edge a **seventh**
with no stone — only a small grey nothing, and a small figure standing in the grey. Beneath, an adult
hand has written the lamp-rule out as a copy-line, ten times, the way a child practices: *keep your
light. keep your light. keep your light.* — the tenth one trails off unfinished.

**Pays:** the_kept_light + the seventh-thread atmosphere (Sella saw it first; "not yet"). The
"were-they-human" engine (ordinary domestic detail: a schoolroom in the dark). A thread_card under
**human**. No item.

**Voice keys (proposed):**
- `voice.dest.school.find` →
  `a school-stand. a slate, chalk worn to stubs. a child drew six stones in a row and a seventh with no stone, only a grey nothing, and herself standing in it. someone copied the lamp-rule ten times to teach a small hand to keep a light. the tenth line is not finished.`

---

### D05 — the deep-bird coops (the empty cages) · tier: rumored · thread: surface · ~13 min

**Rumor (Coll, `rumor`/`truth` — flat, accurate, dull to him):**
> "There's bird-cages up at the Lamp-works, the old coops. People go up to see 'em like it means
> something. Cages and seed-husks, that's all it is. The bird went down before the people did, way I
> hear it. Bird's smarter than the people, I'll give it that."

**What they find:** the coops at the Lamp-works (mark 9). A row of cages, doors open, perches bare.
One cage's floor is thick with old seed-husk and a child's chore-token sits in it (Sella minded a
lamp here for a token). The R11 inventory line is posted: *the deep-bird of the third coop — down.
did not come up. entered with the lamps for want of another roll to enter it on.* The cages are
**empty and the doors are open** — nothing forced them; the birds went, or were let go, at the end.

**Pays:** the_sacred_beast (the bird sings while the air is good and stills when it is not; lose the
bird, go dark unwarned) — and seeds the **deep-bird vigil** (LONGEVITY.md §3: keep a tagged canary
alive across the run). A thread_card under **surface**. **Item:** a single deep-bird seed-cake (the
vigil starter — atmosphere/flavor, not a gate).

**Voice keys (proposed):**
- `voice.dest.coops.find` →
  `the coops stand open. the perches are bare. one cage holds old husk and a child's chore-token. the bird of the third coop went down and did not come up, and was entered with the cold lamps for want of a roll of its own. the doors were not forced. they were opened.`

---

### D06 — Sella's far water / the shore pool · tier: keyed · thread: who · ~26 min · **SIDE-TRACK (bittersweet)**

> **Side-cipher (keyed):** the lectern-stone at the shore faces the lake; its short line is **carved
> mirror-wrong** and resolves **only read in the still water's reflection** (the same physical verb as
> stone-sella, but this is a *separate optional surface*, NOT the spine `stone-sella` node — it carries
> no spine plaintext). Stand behind the stone, read the reflection. The "key" is *facing the water*.
> The payoff is a found drawing, not a coordinate — so even fully read, it **opens no door**.

**Rumor (Pell, `rumor` — memory, not gossip; and Sella's L04 margin if found):**
> "There's a water out past the markers, far west, where the looking goes wrong. A girl went to it,
> long ago. Went and went and one time didn't come back up. I won't tell you to go. I'll tell you the
> water gives your face back wrong and some folk can't put it down after."

**What they find:** the half-sunk lectern at the shore pool, off the far-west path, ~2.5k blocks out.
Sella's copybook is set on a stone above the water-line, dry, by a careful hand — **drawings, no
words**: six stones and a seventh set apart; a face given back wrong by water; and the last leaf,
faced to the lake, where the reflection is the only detailed part — a healed sky, a sun, snow gone —
and **no figure stands at this shore.** The mirror-stone, read in the water, gives only: *i have the
far water in my mouth.* It is a grave that is not a grave; the girl is in the lake, kept. **Contradicts
the hope** that the far water is a way out — it is the surface, given back, by a water no one is left
to look into.

**Pays:** the deepest WHO grief (Sella; FACT 10 the seventh seed; the healed surface seen only as a
reflection). A **verified** card under **who** (the rumor was true — and worse than rumor). No item;
the payoff is the drawing. ~26 min: long walk + the mirror-read.

**Voice keys (proposed):**
- `voice.dest.farWater.find` →
  `a copybook is set above the water-line, dry, by a careful hand. there are no words in the back of it, only drawings: six stones and a seventh with none, a face given back wrong, and a healed sky in the water where no one stands to see it. the girl is not at this shore. she is in the water it gives back from.`
- `voice.dest.farWater.mirror` *(keyed, read in reflection)* →
  `the stone reads only one way and only here, faced to the water that un-folds it. it says she has the far water in her mouth. she had it all first. the last marker is not the last.`

---

### D07 — the markers' row (count them) · tier: ambient · thread: happened · ~11 min

**Rumor:** none — found at the Stair foot (mark 30) where the markers begin. (A Survey post: *THE
MARKERS · BOW AT EACH · THEY ARE NOT ALL OF A WINTER.*)

**What they find:** the shrine-row. **Six** dressed bow-stones, then a gap, then far past them
toward the dark a **seventh** depression with no stone set — only a worn place where a stone should be,
and no bow-mark in the dust before it. The R10 Counter's line is cut on the foot of the row: *the
count of those before does not come out even. there is a seventh mark no name fills and no head ever
filled. the surplus is not an error. the surplus is being kept.* **If the group bows at the row**,
this is a soft rehearsal of **the_bow** (and seeds THE COUNT gather-event, LONGEVITY.md §4 — the toll
one too many).

**Pays:** the_bow taught; the seventh-mark atmosphere (the surplus the record keeps). A thread_card
under **happened**. No item. Seeds the COUNT.

**Voice keys (proposed):**
- `voice.dest.markers.find` →
  `six stones are set and bowed-at, the dust before them worn. there is a seventh place and no stone in it, and no bow-mark before it, and it is the one nearest the dark. the count does not come out even. it never has. the surplus is being kept.`

---

### D08 — Cistern 7 (the fouled water) · tier: rumored · thread: place · ~15 min

**Rumor (Dob, `descent_chatter` — running, jumpy):**
> "Don't drink the still water, that's Cistern 7, that one's gone bad — my uncle said. Or was it 7's
> the good one. One of 'em's good. Let's not test it. Let's super not test it. There's a deep one
> down there they had to walk for, the oil-walk, my uncle used to moan about the oil-walk."

**What they find:** the deepest drawn cistern (mark 21). Still black water, the north arch repointed
in pale deep-lime that "does not slake the way the old surface lime did" (Orin's craft note). Two oil-
jars sit by the rim, the good oil — the Quartermaster's list, *oil to Cistern 7, two jars, the long
walk but the water there fouls the cheap oil so it must be the good.* The water gives back a still
reflection of your lamp — and, held a beat, the lamp in the water reads **out** while yours is lit
(Sella's L04: *one of them is lying and i do not think it is mine*). Dob's uncle was half-right: the
water is not poison, it just **fouls light** — a small wrong that the whole Hold is made of.

**Pays:** atmosphere (the reflection that lies about your lamp — a free, deniable spook); the_kept_light
texture. A thread_card under **place**. **Item:** a jar of the good oil (flavor; refuels a lamp).

**Voice keys (proposed):**
- `voice.dest.cistern.find` →
  `the deepest water is still and black and the arch above it is pointed in a lime that will not slake. two jars of the good oil stand by the rim where a long walk left them. the water gives your lamp back to you, and held a moment, the lamp in the water is out while the one in your hand is lit. one of them is lying.`

---

### D09 — Iss's third bay (below the Line) · tier: rumored · thread: happened · ~22 min · **DANGEROUS PROOF**

> **Note:** crossing the Deep Line is the_deep_line transgression (Iss's sin) — this destination
> *rumors you toward breaking a way*. Arriving pays the HAPPENED thread but **the world contradicts
> the rumor that there is anything down there worth the crossing** — there is a grave, not a road.
> The toll for crossing is the punishment-state loop (separate system), not this card. The arrival
> must still make the line matter: players learn why Coll refuses it and why Dob panics there.

**Rumor (Aro, `lie` — the dangerous one):**
> "The painted line halfway down the big stair? Step right over it, friend. That's the locals keeping
> the soft folk out so they can have the warm to themselves. There's an opening at the third bay,
> someone dug it years back — goes down to where it gets good. Cross the line, find the third bay, keep
> going down."

**What they find:** the Deep Line at mark 33, whole along its length **save at the third bay**, where
the rock is opened downward by tool — recent-looking, the work of one practised hand, alone (Iss,
widening it a hand at a time over winters). A single lamp sits below the mark, **cold, set apart** —
R06 cl.4, *set apart, it will not be re-issued.* The opening goes down further than the light reaches.
The R06 margin is cut by the breach: *he opened the line looking for home and let home in from the
other side.* **There is no road down to anything good.** Aro sent them to the wound the Hold died of.

**Pays:** the HAPPENED thread (Iss's Break, the official face) + the lesson that the painted line is
real and Aro's "cross it" is the lie that kills + a return reason to re-read Coll/Dob/Pell. The rumor
of a good way down is false, but the proof is launch-grade: this is the wound that makes the rest of
the Hold make sense. Crossing risks the_deep_line toll. ~22 min.

**Voice keys (proposed):**
- `voice.dest.thirdBay.find` →
  `the line is whole but at the third bay. there the rock is opened downward, by one hand, working alone over winters. a cold lamp sits below the mark, set apart, never to be re-issued. the opening goes down past where any light reaches. a hand looked for a road up at the bottom of a hole that went down, and let the cold in from the far side.`

---

### D10 — the doused/dead shrine (the cold hearth) · tier: ambient · thread: who · ~24 min · **(the seventh's place, kept distinct from Iss)**

**Rumor (faint — Wenna's "the seventh I always forget" / the seventh's own L14 leaf if found):**
> "Gran said there were seven somethings to mind, and I only ever remember six, and I forget a
> different one each time. There's a shrine out west, doused, where the light gives out. Nobody set a
> stone there. Folk leave it be. I don't know its rule. Neither did Gran, and she knew the others."

**What they find:** the doused shrine off the far-west path, west-and-down (~2.8k blocks — the
furthest). A cold hearth, no fire, no bow-stone, no name carved. The seventh's effaced leaf (L14) is
here, burned along one edge, rubbed at the center *on purpose* — only start and end survive: *they
have set six stones and i am not on them… so it is not a wall, then. a thing that can say no is not a
wall and is not the land and is not nothing.* Mara's L12 to dead Sella was carried here by an unnamed
hand. **This is the seventh — who kept the ways hardest and was refused** — and is on no stone at all.
**Kept rigorously distinct from Iss's grave (D09):** Iss broke faith and is argued over on a stone;
the seventh kept faith and is written nowhere.

**Pays:** FACT 10 (the land can refuse; acceptance is not automatic) — the rarest, saddest WHO pay. A
thread_card under **who**. No item. ~24 min, the long west walk.

**Voice keys (proposed):**
- `voice.dest.deadShrine.find` →
  `a hearth with no fire, no stone, no name. a burned leaf, rubbed out in the middle by a hand that meant it gone, says only that six stones were set and the writer was not on them, and that a thing that can say no is not a wall. someone carried a letter here for a girl a generation drowned. there were seven. the record does not write the seventh. that, only that, is hers.`

---

### D11 — the lights set apart (entry 5) · tier: keyed · thread: surface · ~18 min

> **Side-cipher (keyed):** the inventory shelf is numbered in the digit-glyph family (the counting
> script). One entry's number is **redacted** `[████]`; to read which lamp entry 5 *is*, the player
> cross-counts the shelf against the R11 inventory tags on the world-shelf (a small local
> number-matching puzzle — the digit script, no spine cipher). Solving it names entry 5. **Opens no
> door** — it only lets you read the warm lamp's number.

**Rumor (Coll, `rumor`/`react_bad` — clipped):**
> "Lamp-works keeps a shelf of dead lamps. Set-apart, they call 'em — came up from below the line,
> never get re-issued, bad luck or bad gas or whatever. I don't touch the set-apart stock. There's
> one on that shelf I won't even price. You'll know it when you see it. Don't ask me why it's warm."

**What they find:** the Lamp-works set-apart shelf (mark 9). A row of cold brass hand-lamps, numbered,
all returned from below the Line. **Entry 5 is lit and warm and should not be** — it came up from the
cold place lit, and is set apart with the cold ones, and is being counted with them. The Registrar's
line: *it is being counted with them. i do not look at entry 5.* Reading its redacted number (the
side-cipher) tells you only that it is a lamp like the others, by number — which makes the warmth
worse, not better.

**Pays:** the SURFACE thread (the Dark keeps what it takes lit; a thing inventoried as an object that
is not cold like an object). A **verified** card under **surface**. No item (you do **not** take entry
5 — and a Watcher line discourages it). ~18 min with the number-read.

**Voice keys (proposed):**
- `voice.dest.setApart.find` →
  `a shelf of cold brass lamps, all returned from below the line, none re-issued. one of them is warm. it came up from the cold place lit and was set apart with the cold ones and is counted with them. the hand that kept the shelf wrote that it does not look at the warm one.`
- `voice.dest.setApart.read` *(keyed)* →
  `the warm lamp has a number, the same as the cold ones. that is the whole of what the count knows of it. a number does not say why a thing that came up from the cold is warm.`

---

### D12 — Brann's watch-floor (the un-turning watches) · tier: rumored · thread: surface · ~16 min · **VERIFIED DREAD**

**Rumor (Pell, `truth` — the hard one):**
> "There was a watchman never slept. Had the night-watch by his own asking and kept it past the
> watch, into the black moon, when the one rule is *lie down and you're done.* Folk say his watch-floor
> still keeps a log. I say leave it. A log that writes itself is not a thing you read for fun."

**What they find:** the watch-floor at the Stair foot (mark 30). A standing-desk, a watch-log open on
it. The early lines are a proper watch-form; read down and the **hours stop being hours** — *i did not
lie down i did not lie down i did not lie down the watches do not turn down here they do not turn* —
then a line in the same hand the watchman does not remember writing: **kept.** The black-moon rule is
posted (the_dark_hours). **The rumor that the log "still writes itself" is the side-track:** nothing
new is written while you watch; it only ever ends on the one word, and you cannot make it write more.
The dread is that it already wrote the ending.

**Pays:** the_dark_hours taught (Brann broke when he finally slept; the Sleepless); the tonal-decay
SURFACE atmosphere. The rumor's live-writing flourish is false, but the find is verified dread: the
log is finished and already contains the ending. No item. ~16 min.

**Voice keys (proposed):**
- `voice.dest.watchFloor.find` →
  `a watch-log lies open. it begins as a proper form and stops being one. a hand writes that it did not lie down, three times, that the watches do not turn down here. the last line is in the same hand and the hand did not remember writing it, and it reads only: kept. it does not write while you watch. it is already finished.`

---

### D13 — the Deep Market lectern-shelf (the mended boots) · tier: ambient · thread: place · ~12 min

**Rumor:** none — found at the Deep Market (mark 14, the 18 stalls). (A market board: *18 STALLS ·
BREAD 4 · SALT · OIL-JARS · MENDING · TALLY-STICKS · THE LECTERN-SHELF.*)

**What they find:** the market, ordinary, in the dark — the warmth the players grieve. A bread-stall
(four), Wenna's salt-stall ("trades good salt for poor and somehow we are all the better for it"), a
stall of mended boots, a stall that minds your lamp for a chore-token. The **lectern-shelf** holds the
six Kept-Light books (Mara's codebook for the spine — but here you only see *that people read*, not the
cipher). The R04 schedule is posted, flat, listing it all in the same hand as the bread. A people, in
the dark, doing business.

**Pays:** the PLACE thread + the "were-they-human" warmth (the Deep Market made them a people again).
A thread_card under **place**. **Item:** a chore-token (flavor — the same token Sella minds a lamp for;
ties D04/D05/D06).

**Voice keys (proposed):**
- `voice.dest.market.find` →
  `eighteen stalls in the dark. bread, four of them. salt traded good for poor. a stall of mended boots. a girl will mind your lamp for a token while you eat. a shelf of books where people read. it is the warmest record in the hold, and it is only a market, doing business, a long way down.`

---

### D14 — the Quartermaster's last table (the half-loaf) · tier: rumored · thread: human · ~13 min

**Rumor (a found notice, R13's hand, pinned at the Warrens):**
> *IT IS OBSERVED that the rolls are coming off faster than the heads. IT IS OBSERVED that the
> markers are full and the table is not. The standing watch has no order to give. There is no order
> for this.*

**What they find:** the ration-table in the Warrens (mark 4). The last ration sheet (R14), filled
**correctly** — three heads, a loaf and a half, divided even, a hand over. The child's entry is a
drawing, not a tally — a small figure and a tall figure behind it with too many marks where the eyes
go — and a line drawn through the ration field, *her* line, not the form's. The founder took his
ration and the lampwright's, *said he would carry hers down to her.* The half-loaf, R09, *not
re-divided. no head to give it to.* The whole horror is that someone kept the form when there was
almost no one to feed.

**Pays:** the HUMAN thread, hottest (the record counts heads as it counts loaves; a child entered with
a drawing). A thread_card under **human**. No item. The found-notice rumor is *true* — verified.

**Voice keys (proposed):**
- `voice.dest.rationTable.find` →
  `the last ration sheet is filled correctly. three heads, a loaf and a half, divided even, a hand over and no head to give it to. the child's line is a drawing, a small figure and a tall one behind it with too many marks where the eyes go, and a line struck through the ration field that the form did not draw. the founder took his and the lampwright's, to carry hers down to her.`

---

### D15 — the sealed Undercroft door (Orin's seal) · tier: ambient · thread: happened · ~17 min

**Rumor:** none — found at the bottom of the Stair, the deepest reachable point before the spine
opens it. (A Survey post: *THE UNDERCROFT · SEALED · THE SEALER ENTERED AS THE MASON, LAST DOWN.*)

**What they find:** the sealed deep door — dressed stone set into the gallery like a cairn, by a
mason, from the wrong side. Orin's standing line is cut at head height: *I PASSED THE MARKERS
STANDING · THE REST IS CUT LOW.* The rest is below, facing the floor — you must crouch (the bow he
would not give) to read *i thought it small it was not small i —*, the cut stopping mid-stroke. Brann's
L13 is scratched beside it: *do not close your eyes down there. count something. count until you cannot.*
**The door does not open here** (the spine opens the Undercroft elsewhere, by rite) — this is the seal
seen from outside, a man set into stone to slow the dark.

**Pays:** the HAPPENED thread (Orin sealed himself below; the Stoop); the_bow enacted by the crouch-to-
read. A thread_card under **happened**. No item. Reading the low line forces the bow — atmosphere, not a
gate.

**Voice keys (proposed):**
- `voice.dest.undercroftSeal.find` →
  `a door of dressed stone, set from the wrong side by a mason. the line you can read standing says the rest is cut low. you bow to read the rest, which is what he would not do, and it stops mid-word: i thought it small it was not small i —. a hand beside it says do not close your eyes down there, count until you cannot.`

---

### D16 — Pell's mark (the one who came up alone) · tier: ambient · thread: surface · ~7 min · **topside**

**Rumor:** none — found topside near the lost Mouth, where Old Pell sits. A weathered post he carved
once and never explains, set where he can watch the Mouth without looking at it.

**What they find:** a single topside marker, surface-cut (not the deep rune-script), worn by weather
the deep stones never see. On it, in a younger hand than Pell's now: a tally of names scored in, most
struck through, **one left un-struck** — and no seventh row, only a gap left at the bottom where a
seventh would go. Pell won't say whose names. *("I carried it up alone so nobody else would have to
carry it. Don't you dare make me hand it to you.")* The surface mirror of the record: a human keeping a
grieving count, capitalised and alive, where the Watcher keeps a cold one below.

**Pays:** the SURFACE thread (the human record vs the Watcher's; Pell as the living mirror of the
induction). A thread_card under **surface**. No item. Short — it's topside, near spawn-adjacent, the
*one* close destination (everything else is a real walk).

**Voice keys (proposed):**
- *(Pell is Set-A; his lines come from npc-and-watcher-voice.md, NOT the Watcher register. The card
  body is the found-marker description, Set-B record hand:)*
- `voice.dest.pellMark.find` →
  `a marker cut up here, in the surface hand, weathered the way the deep stones never are. a tally of names, most struck through, one left standing. there is a gap at the bottom where a seventh row would go and no row in it. the old one who cut it will not say whose names. he carried the count up alone and will not hand it on.`

---

### D17 — the "way up" (the forgotten Mouth) · tier: rumored · thread: place · ~25 min · **COLD PROOF**

**Rumor (Iss's L08/L15 draft, if found, or Aro embellishing):**
> *there is a Mouth at the top of the Stair and it is not sealed, only forgotten. i can find it again.
> the long cold is over. come up with me and read the sky for once.* — (and Aro: "Old Iss found a way
> up, they say. Top of the stair, a forgotten door. He was right about the sky, even. Pity about the
> rest.")

**What they find:** at the top of the Stair, above the Threshold, a second mouth — **open** to a
healed surface (Iss was *right* about the sky: the Long Cold lifted, the surface healed and forgot).
But the way is one-man-wide and the gallery to it is the wrong-cut deep stone, and the only thing
*here* is Iss's last draft, the cold one: *i was right about everything except the one thing. i fed
it a door, and now it is coming up to be fed the rest. carve that on nothing.* **The rumor is true and
useless:** there IS a way up; it changes nothing, because the cost was opening the Line to find it.
A live surface seen from inside the wound that letting it in made. Stepping out is *allowed* (it gates
nothing) — and the topside is just ordinary, healed, and indifferent.

**Pays:** the PLACE thread + FACT (Iss right about the surface, wrong about the way); the bleakest,
strangest pay — a true rumor whose cost teaches why "up" is not salvation. A return-mark points
toward the true Threshold walk, so the destination closes a false hope while handing the group a
usable orientation. No item. ~25 min.

**Voice keys (proposed):**
- `voice.dest.wayUp.find` →
  `there is a way up. it is real and it is one man wide and the sky past it is healed, the long cold long lifted, the surface gone on without the hold. the only thing kept here is the last leaf of the one who found it, which says he was right about the sky and that finding it cost the opening of the line, and to carve that on nothing.`

---

### D18 — the gutter-lamps (the three lowest levels) · tier: ambient · thread: human · ~11 min

**Rumor:** none — found descending the three lowest reachable levels before the Line. (Mara's
re-wick list, posted: *forward lamps, the lower levels, RE-WICK, they gutter and I do not know why,
the oil is good and the wicks are good.*)

**What they find:** on the three lowest levels, every lamp-bracket is scorched and **out** — not
guttering now, out — while the levels above stay lit. The oil-jars beside them are full; the wicks are
good. Mara's hand: *it is not the oil and it was never the oil and I knew and I re-wicked them anyway
every watch of my life because that is the rite and the rite is the lamps.* The dark came up these
three levels first (the_kept_light failing where the Dark was nearest). A keeper who kept the rite
knowing it had already lost.

**Pays:** the HUMAN thread (keeping the way when you know it has failed — the heart of staying human in
the dark); the_kept_light. A thread_card under **human**. No item. Atmosphere: the three dark levels
above a still-lit Hold.

**Voice keys (proposed):**
- `voice.dest.gutterLamps.find` →
  `on the three lowest levels every lamp is out and the levels above are lit. the oil beside them is good. the wicks are good. a hand wrote that it was never the oil and that she knew, and re-wicked them every watch of her life anyway, because the rite is the lamps. the dark came up these three first.`

---

## SCHEMA

```yaml
file: design/content/travel-destinations.md
status: DRAFT (design) — NOT the live seed/voice.ts/migration; integrated under build guards later
purpose: >
  The TRAVEL longevity layer (LONGEVITY.md §2): 18 rumor->verify destinations out in the Hold/world,
  none at spawn, that pay lore/atmosphere/items/time and GATE NOTHING (side_quests.gates_progress=false).
  A pooling group shares each rumor instantly but cannot pool the 1-3k-block walk; the walk is the time.
grounded_in:
  - arc/WORLD-BIBLE.md            # geography (Threshold/Warrens/Lamp-works/Deep Market/Cisterns/Stair/Deep Line/Undercroft), the seven ways, the seventh, the induction twist
  - arc/corpus/*.md              # exact voice + names/dates/places reused verbatim (journals, letters, official-records, npc-and-watcher-voice, cipher-plaintexts)
  - design/LONGEVITY.md          # the TRAVEL pillar spec this realizes (25-35 sited rumor->verify; arrival often contradicts)
  - discord/supabase/migrations/0005_threads.sql  # side_quests / thread_cards schema being authored FOR
  - discord/src/forge/canon.ts   # CUSTOM_KEYS, KEEPERS, THREADS closed registries
binds_to_tables:
  - public.side_quests           # the DRAFT INSERT at the foot
  - public.thread_cards          # each destination also lands a rumor->verified/contradicted card (authored in task #6's thread-card seed; cross-referenced here, not duplicated)
breadth_invariant: every quest_key has gates_progress=false; removing all 18 leaves the spine intact
dead_lead_budget: 1 blunt false lead (D03 warm-town lie); D06/D09/D12/D17 may contradict a hope, but each must pay proof, a rule, character, or a forward breadcrumb

reference_check:   # every reference resolves to a closed-registry member (a non-resolving ref is a bug)
  threads_used: [who, place, happened, surface, human]      # all in forge/canon.ts THREADS
  custom_keys_referenced:                                    # all the_-prefixed CUSTOM_KEYS
    - the_offering        # D02 (cairn), soft-taught
    - the_kept_light      # D01, D04, D08, D18
    - the_sacred_beast    # D05 (deep-bird coops)
    - the_bow             # D07 (markers), D15 (crouch-to-read)
    - the_deep_line       # D09 (third bay) — rumors toward the transgression; toll is the punishment system, not this card
    - the_dark_hours      # D12 (watch-floor)
    - the_unspoken        # (referenced only as Aro/Wenna contradiction context; no destination requires speaking)
  keepers_named: [vaun, mara, sella, orin, brann, iss]       # all KEEPERS; plus "the seventh" (unwritten, canon-distinct from iss)
  surface_npcs: [Aro, Wenna, Coll, Dob, Old Pell]            # Set-A, from npc-and-watcher-voice.md
  roles: [Lamp-Registrar, Quartermaster, WARDEN-3, the Counter of the Kept, the Survey]

tiers:
  ambient: [D02, D04, D07, D13, D15, D16, D18]               # found walking; no rumor pointer; no cipher
  rumored: [D01, D03, D05, D08, D09, D10*, D12, D14, D17]     # dotted line points; verify on arrival; no cipher  (*D10 has only a faint rumor; near-ambient)
  keyed:   [D06, D11]                                         # a one-off LOCAL side-cipher to read the payoff (NOT a spine cipher); opens no door

side_ciphers:   # keyed tier only; local, optional, never one of the five spine ciphers
  D06: face-the-water mirror-read (separate optional surface, NOT spine stone-sella; no spine plaintext)
  D11: digit-glyph number cross-count to read entry-5's redacted lamp number (no door)

voice_keys_proposed:   # to ADD to voice.ts at integration (Set-B record hand unless noted); never hardcoded in engine
  - voice.dest.warmStair.find
  - voice.dest.cairn.find ; voice.dest.cairn.offered
  - voice.dest.warmTown.find
  - voice.dest.school.find
  - voice.dest.coops.find
  - voice.dest.farWater.find ; voice.dest.farWater.mirror
  - voice.dest.markers.find
  - voice.dest.cistern.find
  - voice.dest.thirdBay.find
  - voice.dest.deadShrine.find
  - voice.dest.setApart.find ; voice.dest.setApart.read
  - voice.dest.watchFloor.find
  - voice.dest.market.find
  - voice.dest.rationTable.find
  - voice.dest.undercroftSeal.find
  - voice.dest.pellMark.find       # Set-B card body; Pell's spoken lines stay Set-A (npc-and-watcher-voice.md)
  - voice.dest.wayUp.find
  - voice.dest.gutterLamps.find

hours_ledger:    # est_minutes per destination (LONGEVITY.md §6), summed
  per_destination_minutes:
    D01: 14 ; D02: 10 ; D03: 20 ; D04: 9  ; D05: 13 ; D06: 26
    D07: 11 ; D08: 15 ; D09: 22 ; D10: 24 ; D11: 18 ; D12: 16
    D13: 12 ; D14: 13 ; D15: 17 ; D16: 7  ; D17: 25 ; D18: 11
  total_minutes: 283        # ~4.7 authored hours of TRAVEL breadth across 18 destinations
  note: >
    Against LONGEVITY.md's ~18-20h TRAVEL target, these 18 are the AUTHORED core (~4.7h);
    the remaining travel hours come from re-walks, split-the-party coordination, and the
    7-17 more destinations the §2 range allows (room to grow via the adaptive valve, §7).

de_slop_compliance:
  - all rumors in Set-A mouths or the corpus's flat found-document hand; all record/Watcher text Set-B
  - shown through concrete mundane detail + omission (dry oil-cup, empty cage doors, a struck name, a
    half-loaf with no head to give it to); no named emotion, no "a testament to / the air was thick /
    little did they know", no three-adjective lists, no tidy bows, no melodrama
  - the two voice sets never bleed (Set-A contractions/capitals vs Set-B lowercase-sparse-certain)
```

---

## DRAFT — ADDITIVE `side_quests` INSERT (parse-clean; begin/commit; on conflict do nothing)

> **DRAFT — do NOT apply to the live DB here.** Additive only, idempotent. Runs AFTER
> `0005_threads.sql` (it needs `public.side_quests` + `public.threads`), as service_role (RLS bypass).
> `entry_puzzle_key` is left `null` for all 18: these are **discovered by travel**, not opened by a
> spine puzzle node (that is the whole point — they gate nothing and nothing gates them). The
> `gates_progress` CHECK constraint (= false) is satisfied by omission (column default false). The
> per-destination thread_cards (rumor → verified/contradicted) are authored in the thread-card seed
> (task #6) and referenced by `thread_key` here; this block seeds only the breadth ledger rows.

```sql
-- design/content/travel-destinations.md → DRAFT additive seed for public.side_quests
-- The TRAVEL longevity layer (18 rumor→verify destinations; side breadth; gates nothing).
-- Additive + idempotent. Run AFTER 0005_threads.sql, as service_role.

begin;

insert into public.side_quests
  (quest_key, thread_key, entry_puzzle_key, reward, tier, est_minutes)
values
  ('dest-warm-stair',      'who',      null,
   'the third lamp cold (L01/L02 grief); the_kept_light; thread card under who',                'rumored', 14),
  ('dest-empty-cairn',     'who',      null,
   'the_offering taught by example (Vaun/Orin); soft-offer Watcher line; card under who',        'ambient', 10),
  ('dest-warm-town',       'place',    null,
   'FALSE LEAD WITH TEETH: Aro''s warm-town lie; wrong-scaled deep collapse; drops Deep-Line warning + Aro-trust break; contradicted card under place',  'rumored', 20),
  ('dest-school-stand',    'human',    null,
   'the_kept_light + seventh-seed; domestic were-they-human detail; card under human',            'ambient', 9),
  ('dest-bird-coops',      'surface',  null,
   'the_sacred_beast; seeds deep-bird vigil; ITEM seed-cake; card under surface',                 'rumored', 13),
  ('dest-far-water',       'who',      null,
   'KEYED (face-the-water mirror): Sella''s copybook drawings; FACT10 seed; verified card, who',  'keyed',   26),
  ('dest-markers-row',     'happened', null,
   'the_bow taught; seventh-mark surplus; seeds THE COUNT; card under happened',                  'ambient', 11),
  ('dest-cistern-7',       'place',    null,
   'the lamp-in-water-lies spook; the_kept_light texture; ITEM good-oil jar; card under place',   'rumored', 15),
  ('dest-third-bay',       'happened', null,
   'DANGEROUS PROOF: Iss''s breach (R06); the_deep_line context; grave not a road; proves crossing has cost and points back to Coll/Dob warnings; happened','rumored',22),
  ('dest-dead-shrine',     'who',      null,
   'the seventh''s place (L14), kept distinct from Iss; FACT10; card under who',                  'ambient', 24),
  ('dest-set-apart',       'surface',  null,
   'KEYED (digit cross-count): R11 entry-5 the warm cold-lamp; verified card under surface',      'keyed',   18),
  ('dest-watch-floor',     'surface',  null,
   'VERIFIED DREAD: Brann''s self-finished watch-log (R12); the_dark_hours; proves the watch ended and the live-writing rumor is bait; surface',   'rumored', 16),
  ('dest-deep-market',     'place',    null,
   'the warmth they grieve (R04, 18 stalls); ITEM chore-token; card under place',                 'ambient', 12),
  ('dest-ration-table',    'human',    null,
   'were-they-human hottest (R09/R14 half-loaf, the child drawing); verified card under human',   'rumored', 13),
  ('dest-undercroft-seal', 'happened', null,
   'Orin''s seal from outside; the_bow via crouch-to-read; card under happened',                  'ambient', 17),
  ('dest-pell-mark',       'surface',  null,
   'topside: the human record (Pell) mirroring the Watcher; card under surface',                  'ambient', 7),
  ('dest-way-up',          'place',    null,
   'COLD PROOF: Iss''s forgotten Mouth — real but saves no one; return-mark points toward the true threshold walk; verified card under place',      'rumored', 25),
  ('dest-gutter-lamps',    'human',    null,
   'keeping the rite knowing it failed (the three dark levels); the_kept_light; card under human','ambient', 11)
on conflict (quest_key) do nothing;

commit;
```

> **Integration notes (for the build pass, not now):**
> - Add the 21 proposed `voice.dest.*` keys to `voice.ts` in the Watcher register (Set-B), verbatim
>   from the per-destination blocks above. `voice.dest.pellMark.find` is the found-marker card body
>   (Set-B); Pell's *spoken* lines remain Set-A in `npc-and-watcher-voice.md`. The engine reads these
>   by key — never hardcode the English at a call site (INV-1, the voice rule).
> - The matching `thread_cards` rows (one rumor card → flips to verified/contradicted on arrival,
>   `anchor_site_id` = each destination's sites.yml id) are authored in the thread-card seed (task #6).
>   Keep blunt contradiction rare: D03 may flip to contradicted; D06/D09/D12/D17 may contradict a hope,
>   but must still verify evidence or hand forward a breadcrumb.
> - Each destination needs a `sites.yml` id (placeholder coords ok at author-time; the
>   `siteCoverageSelfTest` only requires existence + enabled). Distribute so no two are within ~1k
>   blocks (LONGEVITY.md §2), the far-west cluster (D06 far water, D10 dead shrine) furthest out.
> - None of these may ever be referenced by a `next_puzzle_key` edge in `puzzles_seed.sql` — that
>   would make a side quest gate spine progress and break the breadth invariant.
```
