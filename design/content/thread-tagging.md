# thread-tagging — the 24 puzzle nodes mapped to the Recovery Archive

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT APPLY THESE TAGS OR SEEDS.** Current ownership is the 82-node V5 runtime manifest.

> **What this file is.** Every one of the 24 seeded nodes in `discord/supabase/seeds/puzzles_seed.sql`
> tagged to a `thread_key` (which of the five reconstruction questions its *payoff* advances) and,
> where the node teaches or turns on one of the seven ways, a `teaches_custom` (a canon `CUSTOM_KEYS`
> the_-prefixed key). This is the content layer behind the `0005_threads.sql` column adds
> (`puzzles.thread_key`, `puzzles.teaches_custom`) — it lets the Recovery Archive cluster a solved
> node's card under the right column, and links the cipher↔custom layers.
>
> **DRAFT — design only.** The live seed is NOT edited here. The additive `update` block at the foot is
> staged for integration under the build guards (the DB FK on `puzzles.thread_key` and the
> `threadRegistrySelfTest` already enforce that every value below is a real `THREADS` member; nothing
> here invents a thread or a custom key).
>
> **Hard constraints honored.** Every `thread_key` is one of `THREADS` = who | place | happened |
> surface | human (`forge/canon.ts`). Every `teaches_custom` is one of `CUSTOM_KEYS` (the_-prefixed).
> `teaches_custom` is left NULL where a node advances the reconstruction but does not *teach a way* — a
> node is not given a custom just to fill the column (that would be the lazy 1:1 the brief forbids).
>
> **Ground.** `arc/WORLD-BIBLE.md` §7 (the five threads → how each is answered), the de-slopped corpus
> in `arc/corpus/*`, and each node's own `accepted_answers` + outcome fragment in the seed.

---

## the rule I tagged by (so the tags reflect content, not a pattern)

1. **`thread_key` = the question the node's *payoff* answers**, not the question its surface dresses up
   as. A cipher about a lamp can pay off the *human* question (a person counted as a light) rather than
   *place*. I read the fragment / accepted-answer, not the title.
2. **`teaches_custom` only when the node teaches or hinges on a way.** Reading a keeper's *fate* is not
   teaching their way; *enacting* the way, or having the solve turn on the way's logic, is. Pure-grief
   and pure-reconstruction nodes get NULL — and there are many, on purpose.
3. **The five threads are deliberately uneven.** WORLD-BIBLE §7 says SURFACE = "the Watchers, the reports
   that know your name, the spooks" and HUMAN = "the tonal-decay, the wrong-scaled galleries, the taken
   who still keep the ways." Those two are the *back half* of the spine; Movement I–II skew WHO/HAPPENED;
   PLACE is rare and concentrated where geography is the payoff (the descent). The distribution below is
   lopsided because the content is.
4. **The seven ways are NOT evenly distributed across the six keeper evidence sites.** `the_bow` is taught by
   THREE different nodes (Orin's stone, the crouch-to-read verb, the synchronized Accepting) because the
   corpus makes the bow the load-bearing way — "the smallest of the ways," the one whose letting-go lets
   the rest go. `the_sacred_beast` (the deep-bird) was taught by NONE of the original 24 — it lived in
   the NPC / side-quest layer (Wenna's charm, the coop survey, R11 entry 4). The diverse-expansion block
   (thread_tags.sql) now teaches it in-line too, at **`sella-shore-memorial`** — the drowned child's kept
   deep-bird, the forced-perspective glyph above her pool — so all seven ways now have ≥1 teaching node.

---

## the 24 nodes

### MOVEMENT I — The Notice (entry surface)

**`m1-record-opens`** → `surface` · teaches NULL
The payoff is the realization that *the record was open before you found it* and "counts the living by
name" — R16's the-record-opens, the reader filed into the same book as the Kept. That is the SURFACE
question ("what is with us now") at its purest: the thing watching already has your name. No way is
taught — it is the induction frame, not a rite.

**`m1-named-habit`** → `human` · teaches NULL
"it named my habit before i knew it was a custom / i was measured before i was told." The payoff is the
"were they human?" engine turned on the *player*: you were graded by laws no one told you, exactly as the
record counts a head no differently from a lamp (R09/R10). dead_end, terminal dread — HUMAN, not SURFACE,
because the dread is about *being counted as a thing*, not about who is watching. No way taught.

**`rosetta-ring`** → `place` · teaches `the_bow`
The literacy gate. Its accepted answer is the ring read sunwise — "bow offering kept light deep line ward
covering" — the founding hand teaching the whole script. It pays the PLACE question: this is the founding
artifact that says *what the Hold was* and that the ways were "received, not enacted" (R01). I tag
`the_bow` because the ring is read **Bow-first** (sunwise from the top: Bow · Offering · Kept-Light …) —
the bow is the first glyph the literacy hands you, and the corpus makes the bow the keystone way. (Place
over who: the ring is the colony's charter-key, not a person.)

---

### MOVEMENT II — The Keeper-Stone Field

**`stone-vaun`** → `human` · teaches `the_offering`
Caesar. Vaun "counted everything and gave nothing back; the given-back column of his ledger is blank."
The cipher's shift *is* his hoarding ("three of each"). teaches `the_offering` — the way he broke is the
Offering (give back to the deep), and the stone literally enacts the failure-to-return. Thread is HUMAN,
not WHO: the payoff is the "counted as a thing" gut-punch ("the land counts first, and it had already
counted him") — a man reduced to an unbalanced column. (His *biography* is a WHO matter, but this node's
fragment lands on the counting, so HUMAN.)

**`stone-mara`** → `happened` · teaches `the_kept_light`
Book-cipher, the spine key → undercroft-descent. The book is the six-book Kept-Light shelf at
`kept_light_home_01`; the assembled sentence is "descend and bow at the unbroken light." teaches
`the_kept_light` — the codebook IS the Kept-Light shelf, read by "the one lamp that has not gone out
yet," and Mara's whole fate is the kept light. Thread HAPPENED: this is the node that turns the
reconstruction toward *what happened* — it sends you down into the keepers' world to witness the breaking.

**`stone-sella`** → `surface` · teaches NULL
Atbash/mirror + bearing, read in the lake's reflection → seventh-shrine. "south by the far water where she
did not come back." Thread SURFACE: the far water is the corpus's literal surface-question instrument —
"the surface keeps everything… it took the looking and folded it and gave it back wrong," the water that
shows a healed sky to a Hold that can't reach it (Sella's leaf-7 drawing). teaches NULL: Sella's way is
the deep-bird (`the_sacred_beast`), but this stone is about the *far water and the uncounted seventh*, not
about keeping the bird — tagging her custom here would be biography-by-association, not teaching. NULL.

**`stone-orin`** → `who` · teaches `the_bow`
Substitution, legible only from a **crouch** → orin-threshold. "i thought it small it was not small."
teaches `the_bow` — and the teaching is *physical*: the crouch to read the floor-facing run **is the
bow.** "the crouch is the Bow. to read the keeper who would not bow, you bow." The stone enacts the way it
is about. Thread WHO: the payoff begins the reconstruction of Orin as a person (the mason who would not
bend), routing to his biography.

**`stone-brann`** → `surface` · teaches `the_dark_hours`
Beacon colour-sequence, night/black-moon-gated, count-the-fires. "one fire was never doused / do not close
your eyes here." teaches `the_dark_hours` — it is the night/black-moon way made into the gate (the carving
is "legible only at night," the watchman's hand cut by lamplight) and the answer is Brann's own warning
about closing your eyes. Thread SURFACE: the fragment's payoff is the *presence* — "one of them never went
out, and no hand tends it" — the kept fire that is the Watcher's hearth, the with-us-now thing. (Not
HAPPENED: this node names the present haunting, not the past event.)

**`stone-iss-wall`** → `happened` · teaches `the_deep_line`
Vigenère, key = ISS (his own name). The skeptical name-as-key reading → iss-doubt → the catch. "the one
who turned away." teaches `the_deep_line` — Iss IS the Deep Line: "this is the old sin, the one that
opened the dark the first time" (B2 the_deep_line/left), his whole crime is crossing the marked depth.
Thread HAPPENED: this is the contradictory-accounts engine of the Break (L09/L10/L11), the node that opens
the question of *what happened the year it broke*.

**`iss-warm`** → `happened` · teaches `the_deep_line`
The WARM misreading of Iss's stone, read trustingly → iss-dead-shrine (the grave). "the ways are a wall
against the watching." Same way as its sibling — `the_deep_line` — because trusting the liar is itself the
Deep-Line lesson taught by negation: the warm reading routes you to his grave, the literal far side of the
Line. Thread HAPPENED: it advances the Break question down the *false* fork (the planted lie, FACT 7),
which is still the HAPPENED reconstruction — just the wrong account first.

**`m2-rhyme`** → `who` · teaches NULL
Read any two keeper stones side by side; "each fate matches the custom they broke / the stones are
warnings shaped like us." dead_end, collective, recolors what you have. Thread WHO: the payoff is the
*people* — six fates that rhyme with six broken ways, the chorus that makes them characters not ciphers.
teaches NULL: it is *about* all the ways at once without teaching any single one; assigning one custom
would misrepresent a node whose whole point is the pattern across customs.

---

### MOVEMENT II→IV — The Liar Thread

**`iss-dead-shrine`** → `human` · teaches NULL
THE load-bearing red herring (REQUIRE_INBOUND). Iss's coordinate genuinely works and leads to a real
place — "the dead shrine, the cold hearth, nothing is kept here." dead_end. Thread HUMAN, NOT happened:
this is no longer about the event but about *what a person becomes* — the grave of the one who turned
away, and (per L12/L14) the shrine that also holds the cast-out seventh's effaced leaf. The payoff is the
hollowing, the "nothing is kept here" — the human cost, not the chronology. teaches NULL: a grave teaches
no way; it shows the end of breaking them.

**`iss-doubt`** → `human` · teaches NULL
Turn ISS's key on the OTHER stones; it disagrees with every honest carving → no-wall-catch. "we checked
the lock / his key is his own name and his name is the one who turned away / ask first what a wall is for."
Thread HUMAN: the payoff is the moral reconstruction — Orin had it first (L07: "a wall does not choose…
ask why this one knows your name"), and the catch is about whether the people inside the wall were ever
safe, i.e. whether the wall was ever a wall. teaches NULL: this is detective-work on the lie, not the
teaching of a way. (Deliberately NOT `the_deep_line` again — the doubt node is about the *method of the
lie*, the cross-check, not the crossing.)

**`no-wall-catch`** → `happened` · teaches `the_deep_line`
main_beat. Re-walk a clue falsely marked "kept · solved"; the Stone-after contradicts Iss line for line;
flips Iss warm→cold, yields the TRUE final coordinate. "no wall was ever built here / they were the
reaching let in / what iss sent you to was a grave / back to vauns stone turn down." Thread HAPPENED: this
is the *resolution* of the Break question — the corpus's no-wall-was-ever-built-here, the answer to "what
happened" (FACT 8). teaches `the_deep_line`: the catch's whole content is that the Line was not a wall but
a door Iss opened — the Deep-Line way, finally understood correctly. (This is the one place the Deep Line
is *taught true*, vs. taught-by-lie at iss-warm; the repeat is earned, not lazy.)

---

### MOVEMENT III — The Undercroft + the Seventh

**`undercroft-descent`** → `place` · teaches `the_kept_light`
main_beat. PERFORM Mara's sentence: descend at the unbroken light, through the lectern door →
undercroft-fog. "descend at the unbroken light / descend through the lectern door." Thread PLACE: this is
the node that *changes the place* — leaves the group's world for the keepers', the geography-implies-
history descent (deeper = older + wrong-scaled, A2). teaches `the_kept_light`: the door is opened at the
"unbroken light" (`site_id: unbroken_light`) — the one fire that never goes out is the literal key, the
Kept-Light way enacted as the descent gate. (Place over happened: the payoff is *arriving lower*, not an
event.)

**`undercroft-fog`** → `human` · teaches NULL
Pure lore, midpoint gut-punch. Witness the altar room rebuild WRONG. "the room rebuilds wrong / one fire
and no one to tend it / they did not depart they were kept." Thread HUMAN: this is FACT 12 spoken almost
plainly — "they did not depart. they were kept" — the word that "does not change between the people and
the things" (L16), the were-they-human core. teaches NULL: it is witnessing, not a rite; the one fire here
is the *image* of the kept, not a way being taught (that teaching already happened at undercroft-descent).

**`seventh-shrine`** → `who` · teaches NULL
side_quest entry, LORE payoff (FACT 10: the land can refuse). Sella's bearing → the cold-hearth shrine;
count six, then "a seventh." "there was a seventh / the last marker is not the last / the land kept six
and refused the seventh / a thing that can say no is not a wall." GATES NOTHING. Thread WHO: the seventh
is a *person* the record will not write (L14, the effaced leaf; the-seventh-not-kept) — the WHO question
extended past the six named keepers to the unnamed one. teaches NULL: kept distinct from Iss and from any
way; the seventh's payoff is "a thing that can say no is not a wall," a truth about the *Dark's will*, not
a custom. (Deliberately WHO, not surface, despite the far-water entry — the shrine's content is the
missing person, not the water.)

---

### MOVEMENT IV — The Reckoning

**`orin-threshold`** → `who` · teaches `the_bow`
Pure lore. Bring Orin's broken "i —" to D04's third-person completion. "i was not kept i was counted and
the count was true and it was not enough / named warned left at the threshold." Thread WHO: this is the
*biography* — Orin reconstructed as the man observed/warned/left (R08, observed-warned-left-at-threshold),
"ask which of the two is still free to leave." teaches `the_bow`: the completion turns on the bow he would
not give ("the bow is the smallest of the ways," R08's "the cost of the bow, which is nothing, which is to
stoop"). The bow is taught a *second* way here — not the crouch-verb of stone-orin but the moral weight of
the un-given bow. Earned repeat (the corpus's keystone way).

**`haunting-biography`** → `surface` · teaches NULL
Terminal lore. A keeper (or now-cold Iss) ties a Movement-I haunting to a named keeper's fate. "the dread
had a biography / the first hauntings were a keepers fate / the dread was never random." Thread SURFACE:
this is the spook layer explained — "the first hauntings were not random… re-enacted at your door" — the
with-us-now presence revealed as a specific dead person's fate (the Stoop = Orin, the Sleepless = Brann).
The SURFACE question ("what's with us now") answered by HAPPENED material, but the node's payoff is the
*present haunting*, so SURFACE. teaches NULL: it explains the spooks, teaches no rite.

**`atonement-refrain`** → `happened` · teaches `the_offering`
main_beat, the M4 turn (conduct is the lock, the fragment is the key). Honor a previously-broken custom,
then return to the keeper who withheld its fragment. "the keepers turn / conduct is the lock the fragment
is the key / honor the broken custom and return." Thread HAPPENED: the payoff advances the *resolution* of
the going-out — atonement for a broken way unlocks the withheld piece, completing an account. teaches
`the_offering`: the mechanic is *giving back* — you perform a custom you owe before the keeper returns the
fragment, the Offering logic generalized (return first, then receive). (Offering taught a second time, vs
stone-vaun's failure-to-give; here it is the *successful* return — the inverse, earned.)

---

### MOVEMENT V — The Accepting

**`rite-tokens`** → `human` · teaches `the_offering`
main_beat. Lay one personal token in each of six slots + the named components. "bring the thing only you
can give / deeps first heart unbroken light salt of the keepers / a piece you cannot read your way out
of." Thread HUMAN: FACT 13 — "the missing tool is YOU"; R15's "nothing left to return to the deep but
itself." The were-they-human question answered by demanding the player give *themselves*, as the last
keeper did. teaches `the_offering`: the rite IS an offering — the deep is fed the one thing only a person
can give. (A third Offering node, and the deepest reading of it: the corpus's "it was never the first of
the deep it wanted." Earned — the Offering is the way the whole rite turns on.)

**`pressure-glyph-walk`** → `place` · teaches `the_bow`
Pure lore, redundant optional approach to the rite, skippable. The group walks the rune the altar floor
names, footstep by footstep. "walk the rune / do not decode walk it / trace the rune with your feet." Thread
PLACE: the payoff is the *altar floor itself* — the rune is a feature of the place, walked not read; it is
the geography of the Undercroft made into the act. teaches `the_bow`: "mara left one lesson: do, dont read
your way out" — the way is performed with the body, the same do-not-read-your-way-out lesson the bow-crouch
taught; walking the rune is a bodily submission kin to the bow. (Tagged PLACE not human to break the
Movement-V human run — this node's content is the floor, an optional bodily approach, distinct from the
tokens' self-giving.)

**`accepting-crouch`** → `human` · teaches `the_bow`
TERMINAL RITE — main_beat, detected in-world only (a synchronized group bow), opaque token. Everyone
present bows as one, at the hour, in the kept light. Thread HUMAN: the climax of the were-they-human arc —
the collective, no chosen one (WORLD-BIBLE §6), the moment the players choose by conduct whether they walk
out human or stay to watch. teaches `the_bow`: the Accepting *is* the bow — the synchronized crouch, the
keystone way performed as the finale. The bow is taught a *fourth* and final time, now as the whole
group's single act. (The four bow-teachings — literacy / crouch-verb / un-given-bow / collective-bow — are
the deliberate breaking of any 1:1 way↔node pattern; the bow earns four because the corpus makes it the way
all the others hang on.)

**`record-receives`** → `surface` · teaches NULL
main_beat (active=false, staged for M5). The world's response — the hidden advancement fires, the world
flips to KEPT. "the record receives you." Thread SURFACE: FACT 14 — the record, the with-us-now presence,
*responds*; this is the Watcher (the accumulated Kept, R16) acknowledging the player into or out of the
book. It is the surface-question's final answer: the thing watching was the Kept the whole time. teaches
NULL: it is the world's verdict, the consequence of the ways kept, not the teaching of one. (Surface, not
human, because the payoff is the *record's act* — the response from outside — not the player's interior.)

---

## thread distribution (a check that it is lopsided by content, not flat)

| thread | nodes | count |
|---|---|---|
| **who** | stone-orin, m2-rhyme, seventh-shrine, orin-threshold | 4 |
| **place** | rosetta-ring, undercroft-descent, pressure-glyph-walk | 3 |
| **happened** | stone-mara, stone-iss-wall, iss-warm, no-wall-catch, atonement-refrain | 5 |
| **surface** | m1-record-opens, stone-sella, stone-brann, haunting-biography, record-receives | 5 |
| **human** | m1-named-habit, stone-vaun, iss-dead-shrine, iss-doubt, undercroft-fog, rite-tokens, accepting-crouch | 7 |

24 nodes. HUMAN (7) and the two back-half threads (SURFACE 5, HAPPENED 5) carry the spine, exactly as
WORLD-BIBLE §7 frames it; PLACE (3) is the rarest, concentrated on the descent/geography nodes; WHO (4) is
the keeper-biography spine of Movements I–IV. No thread is a tidy "one per movement."

## teaches_custom distribution (uneven on purpose)

| custom | taught by | count |
|---|---|---|
| **the_bow** | rosetta-ring, stone-orin, orin-threshold, pressure-glyph-walk, accepting-crouch | 5 |
| **the_offering** | stone-vaun, atonement-refrain, rite-tokens | 3 |
| **the_deep_line** | stone-iss-wall, iss-warm, no-wall-catch | 3 |
| **the_kept_light** | stone-mara, undercroft-descent | 2 |
| **the_dark_hours** | stone-brann | 1 |
| **the_unspoken** | — | 0 |
| **the_sacred_beast** | — | 0 |
| (NULL) | m1-record-opens, m1-named-habit, stone-sella, m2-rhyme, iss-dead-shrine, iss-doubt, undercroft-fog, seventh-shrine, haunting-biography, record-receives | 10 |

**Why two ways teach zero nodes here.** `the_unspoken` (never speak the Dark's name) and
`the_sacred_beast` (keep the deep-bird) are *avoidance/care* ways with no cipher or rite among these 24 —
they live in the discover-by-punishment layer (B2 reports), the NPC layer (Wenna won't say the name; the
deep-bird charm), the official records (R07 the ordinance on speaking; R11 entry 4 the bird "entered with
the lamps"), and Sella's side material. Inventing a 24-node teacher for them would be the lazy pattern the
brief forbids. They are taught — just not by a puzzle node. (When the customs/NPC content lands under task
#8, those layers carry these two ways.)

**Why 10 nodes teach no way.** Reconstruction, grief, and verdict nodes (the record opening, the rhyme,
the doubt/cross-check, the seventh, the haunting-biography, the record receiving) advance a *thread* without
*teaching a rite*. The column is nullable for exactly this reason; filling it everywhere would be the 1:1
regularity to avoid.

---

## DRAFT — additive SQL (staged for integration; do NOT apply to the live seed here)

```sql
-- DRAFT thread-tagging for the 24 seeded puzzle nodes.
-- Additive, idempotent-by-key, parse-clean. Apply AFTER 0005_threads.sql (which adds the columns
-- + seeds the five threads) and AFTER puzzles_seed.sql (which inserts the 24 rows).
-- Every thread_key is a THREADS member; every teaches_custom is a CUSTOM_KEYS the_-prefixed key.
-- NULLs are intentional (a node that advances a thread but teaches no way).
-- Run as service_role (RLS bypass).

begin;

-- MOVEMENT I
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'm1-record-opens';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'm1-named-habit';
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_bow'         where puzzle_key = 'rosetta-ring';

-- MOVEMENT II — keeper stones
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_offering'    where puzzle_key = 'stone-vaun';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_kept_light'  where puzzle_key = 'stone-mara';
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'stone-sella';
update public.puzzles set thread_key = 'who',      teaches_custom = 'the_bow'         where puzzle_key = 'stone-orin';
update public.puzzles set thread_key = 'surface',  teaches_custom = 'the_dark_hours'  where puzzle_key = 'stone-brann';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'stone-iss-wall';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'iss-warm';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'm2-rhyme';

-- MOVEMENT II→IV — the liar thread
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'iss-dead-shrine';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'iss-doubt';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'no-wall-catch';

-- MOVEMENT III — undercroft + seventh
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_kept_light'  where puzzle_key = 'undercroft-descent';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'undercroft-fog';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'seventh-shrine';

-- MOVEMENT IV — the reckoning
update public.puzzles set thread_key = 'who',      teaches_custom = 'the_bow'         where puzzle_key = 'orin-threshold';
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'haunting-biography';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_offering'    where puzzle_key = 'atonement-refrain';

-- MOVEMENT V — the accepting
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_offering'    where puzzle_key = 'rite-tokens';
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_bow'         where puzzle_key = 'pressure-glyph-walk';
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_bow'         where puzzle_key = 'accepting-crouch';
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'record-receives';

commit;
```

---

## integration notes (for whoever wires this in under the guards)

- **No new voice keys proposed.** This file only writes two existing columns (`thread_key`,
  `teaches_custom`); it adds no player-facing prose, so no `voice.ts` additions are owed by this task.
  (The thread *card bodies* — `thread_cards.body_voice_key` — are a separate content task; this file
  only tags the nodes so those cards can cluster correctly.)
- **The FK does the validation.** `puzzles.thread_key` references `threads(thread_key)`; the 24 `update`
  statements will hard-fail if any value drifts from the five seeded threads — so this block is
  self-checking against `0005_threads.sql`.
- **`teaches_custom` has no FK** in `0005` (it is a plain `text`), so the_-prefixed correctness is on the
  author. All 14 non-null values above are verified members of `forge/canon.ts` CUSTOM_KEYS:
  the_bow (×5), the_offering (×3), the_deep_line (×3), the_kept_light (×2), the_dark_hours (×1).
  *Suggested optional guard:* a self-test asserting every non-null `teaches_custom` ∈ CUSTOM_KEYS,
  mirroring `threadRegistrySelfTest` — cheap insurance if `teaches_custom` ever grows a typo.
- **Order:** apply after both `0005_threads.sql` and `puzzles_seed.sql`. Re-running is safe (each
  `update` is keyed and sets absolute values).
```
