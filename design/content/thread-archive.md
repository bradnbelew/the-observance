---
id: thread-archive
title: THE RECOVERY ARCHIVE — the place-anchored cards under the five threads
kind: design/draft (content authoring; NOT the live seed)
status: SUPERSEDED_PRE_V5_ARCHIVE_DO_NOT_IMPLEMENT
binds:
  - discord/supabase/migrations/0005_threads.sql   # thread_cards / threads schema this authors FOR
  - discord/src/forge/canon.ts                     # THREADS, KEEPERS, CUSTOM_KEYS (closed registries)
  - discord/supabase/seeds/puzzles_seed.sql        # revealed_by_solve puzzle_keys
  - plugin/src/main/resources/sites.yml            # anchor_site_id (enabled sites only)
  - discord/src/voice.ts                           # body text lives behind a voice key, never inline
grounds_in:
  - arc/WORLD-BIBLE.md
  - arc/corpus/official-records.md
  - arc/corpus/journals-vaun-mara-sella.md
  - arc/corpus/journals-orin-brann-iss.md
  - arc/corpus/letters.md
  - arc/corpus/npc-and-watcher-voice.md
  - arc/corpus/cipher-plaintexts.md
---

# THE RECOVERY ARCHIVE

> **What this file is.** The reconstruction layer authored as DATA: 37 place-anchored
> CARDS that cluster under the five threads (`who` / `place` / `happened` / `surface` /
> `human`). Each card is one *find* in the world — a torn record, a journal leaf, a
> carved stone, a thing seen on arrival — written as REAL recovered material in the
> corpus's own voice, anchored to a real `sites.yml` id, and wired into a citation web
> (`references_card_key`) where cards corroborate or **contradict** one another.
>
> **Progress is the threads filling in, not a step counter.** A thread is *answered* only
> by cross-referencing its cards — never by any one card stating the answer. In particular
> the `human` thread ("were they human?") is **never stated outright** anywhere; it
> resolves only when the player holds the official-records dehumanization cards (a head
> counted as a jar, a hand inventoried as a lamp, 214 names against 9 heads) beside the
> induction-twist cards (the surplus that is "the kept"; the reader filed into the same
> book). The archive supplies the pieces; the player does the arithmetic.
>
> **Voice discipline (the de-slop law).** Every `body` is recovered material, not AI prose.
> The text lives behind a `body_voice_key` (proposed in §VOICE-KEYS below for integration
> into `voice.ts`) — the engine never hardcodes story. No "a testament to" / "little did
> they know" / "the air was thick" / named emotions / tidy bows / three-adjective lists /
> "not just X but Y". Show through concrete mundane detail and omission. The corpus
> (`arc/corpus/*`) is the gold standard; these match its hand.
>
> **Canon discipline (HARD).** Every `thread_key` ∈ canon.ts `THREADS`. Every keeper named
> is a `KEEPERS` member. Every `custom_*` reference uses a `CUSTOM_KEYS` (`the_`-prefixed)
> key. Every `anchor_site_id` is an ENABLED site in `sites.yml`. Every `revealed_by_solve`
> is a real `puzzle_key` in `puzzles_seed.sql`. A reference that does not resolve is a bug
> (the `references_card_key[]` self-citations are internal to this archive and checked in
> §CHECKS).

---

## CARD-KIND SEMANTICS (matches 0005 `thread_cards.card_kind` CHECK)

| kind | meaning | flips to |
|---|---|---|
| `rumor` | heard secondhand (a surface NPC, a dotted line to an unvisited place). Not yet stood-on. | `verified` or `contradicted` on arrival at the anchor |
| `explore` | verified firsthand on-site — a record/leaf/stone actually read in the world | (terminal) |

> Rumor cards are how a surface NPC's line (Aro's lie, Wenna's half-truth, Pell's hard
> truth) enters the archive as a citable claim the deep then confirms or breaks. The flip
> is the reconstruction's heartbeat: *you were told X; you went; X was a grave.*

---

## THE CARDS

> Ordered by thread, then `sort_order`. Each card lists: **card_key · thread · anchor_site_id
> · card_kind · revealed_by_solve · references** then the in-character **body** (1–4 lines).
> The body is what the player reads on the card face; in production it is fetched by
> `body_voice_key` (§VOICE-KEYS), never stored as English in the row.

---

### THREAD: `who` — who they were  *(amber)*

The Kept as people: a market in the dark, a child minding a lamp for a token, a founder who
counted everything and a lampwright who read everything and walked none of it.

---

**WHO-01 · `who-deep-market` · who · `stone_of_reckoning` · explore**
*references: none · revealed_by_solve: (none — found on descent)*

> eighteen stalls at the fourteenth mark. bread at four of them, salt, oil-jars, mending,
> tally-sticks, the lectern-shelf. a girl minds your lamp for a chore-token while you eat.
> one stall sells nothing and is kept lit for the dead.

*(From R04 survey + Vaun "made us a people again." This is the warmth. Place it early so the
grief has something to grieve.)*

---

**WHO-02 · `who-vaun-counted` · who · `stone_vaun` · explore · revealed_by_solve: `stone-vaun`**
*references: [`who-mara-read`, `human-offering-ledger`]*

> the one called vaun kept eleven jars of oil and gave the deep none. he counted his three
> good lamps at night, twice, and wrote that the counting was a comfort and did not know why
> he wrote it. the second column of his ledger he scored deep and left empty his whole life.

*(Vaun D02/journal + L01/L02 Mara asking for one jar. Pairs with the offering ledger that
makes the empty column public.)*

---

**WHO-03 · `who-mara-read` · who · `stone_mara` · explore · revealed_by_solve: `stone-mara`**
*references: [`who-vaun-counted`, `who-sella-token`]*

> the one called mara kept every lamp in the hold lit and read every rite by the one steady
> fire and walked none of them. she kept the child's chore-token in the oil-jar drawer and
> would not spend it. she said: i never went down. i only ever read the way down.

*(Mara journal + cipher-plaintexts stone-mara maker's mark. The mirror of Vaun: he holds and
won't give, she reads and won't walk.)*

---

**WHO-04 · `who-sella-token` · who · `the_far_water` · explore · revealed_by_solve: `stone-sella`**
*references: [`who-mara-read`, `surface-seventh-marker`]*

> the under-warden. eight years old. she minded a lamp at the market for a token and kept her
> own light lit and counted things the grown ones would not count. she said the dark is only
> where the light is not yet. she said it the way her mother told her to.

*(Sella copybook early + L03/L04. The token ties her to Mara; the count ties her to the
seventh.)*

---

**WHO-05 · `who-orin-mason` · who · `stone_orin` · explore · revealed_by_solve: `stone-orin`**
*references: [`who-brann-watch`, `happened-orin-sealed`]*

> the one called orin set seven courses a day of good stone and re-cut the bow-stone grooves
> deep so a man could not pass them without stooping. make the stone ask, he wrote. a thing
> the dead troubled to teach you, you keep. he did not see the use of the bowing. he bowed.

*(Orin journal. The grooves-deep detail is his whole character — he engineers the rite to
keep being asked. Sets up his fate card in `happened`.)*

---

**WHO-06 · `who-brann-watch` · who · `stone_brann` · explore · revealed_by_solve: `stone-brann`**
*references: [`who-orin-mason`, `surface-watcher-counts`]*

> the one called brann had the night-watch by his own asking and kept it past the watch into
> the black moon, when the rule is plainest: do not sleep on the black moon. a watchman with
> a tale to tell has had a bad night, he wrote. he liked the quiet watches. he had fewer of
> them each winter.

*(Brann journal. "Fewer quiet watches each winter" is the decay shown, not named.)*

---

**WHO-07 · `who-iss-friend` · who · `stone_iss` · explore · revealed_by_solve: `stone-iss-wall`**
*references: [`happened-the-doubt`, `surface-iss-was-right`]*

> the one called iss was the best of the young ones. he made the new families laugh in the
> first hard month. he mended mara's bellows-arm and would take nothing for it. he kept the
> ways every winter of his life and carried the first water to the cairn three mornings
> running without being told. sella called him the under-warden first; he gave her the name.

*(Vaun fifth-winter + Mara sixth-winter + Iss letters. CRITICAL: Iss is introduced as
beloved, not as the villain. The doubt card and the catch come later.)*

---

### THREAD: `place` — what this place was  *(green)*

The Hold: a vertical colony dug down out of the Long Cold toward warmth, human-scaled at the
top and wrong-scaled at the bottom, with a marked depth no one was to pass.

---

**PLACE-01 · `place-came-down` · place · `the_threshold` · explore**
*references: [`place-deeper-wrong`]*

> it is set down, that we came down out of the long cold, the sun having failed and the sky
> having failed, and that the deep was warm, and that we were received here as tenants and
> are not the owners of this place. we did not come down to be brave. we came down to not
> freeze.

*(R01 founding ordinance + Vaun first-winter. The lintel at the Threshold — the only record
up here a person can read standing straight.)*

---

**PLACE-02 · `place-seven-ways` · place · `rune_rosetta` · explore · revealed_by_solve: `rosetta-ring`**
*references: [`place-came-down`, `surface-sixth-blank`]*

> the order is seven, and we are tenants of seven, and the rent of this place is the keeping
> of them. keep the lamp. the deep line. the dark hours. the offering. the bow. the unspoken.
> the deep-bird. this order does not say to whom the rent is paid. the first keepers were
> asked, and did not answer.

*(R01. The seven ways as received law — `the_kept_light` `the_deep_line` `the_dark_hours`
`the_offering` `the_bow` `the_unspoken` `the_sacred_beast`. The unanswered "to whom" feeds
the sixth-blank surface card.)*

---

**PLACE-03 · `place-deeper-wrong` · place · `unbroken_light` · explore · revealed_by_solve: `undercroft-descent`**
*references: [`place-came-down`, `human-galleries-unruled`]*

> depth in marks carved down the rail of the stair, each mark a man's height. the threshold
> at zero, the warrens at four, the lamp-works at nine, the market at fourteen, the cisterns
> at twenty-one, the stair foot at thirty, the deep line at thirty-three. below that the
> survey is not entered in this book. the surveyor is not entered in this book.

*(R04 survey schedule. The geography IS the history — top human-scaled, bottom unsurveyed.
Hands off to the galleries-unruled human card.)*

---

**PLACE-04 · `place-deep-line` · place · `stone_iss` · explore · revealed_by_solve: `stone-iss-wall`**
*references: [`happened-the-break`, `surface-iss-was-right`]*

> the line at the thirty-third mark, carved in the taking-hold and re-marked each winter.
> pass not the marked depth. no dwelling below it. no light re-issued from below it. the
> survey turned back at the mark, as ordered, the line being the line for a reason older
> than the order that marks it.

*(R06 survey of the Deep Line. The edge of the Dark's reach; the thing Iss crossed.)*

---

**PLACE-05 · `place-cairn` · place · `offering_cairn_01` · explore**
*references: [`who-vaun-counted`, `human-offering-ledger`]*

> the offering-cairn at the shaft-mouth above the cisterns. you carry the first of the deep —
> first ore, first water, first warmth — back to it and give it to the deep again. you do not
> keep the first thing. orin built it back twenty-one courses, capstone last, the first of the
> deep laid in the hollow, every winter his hands held.

*(Vaun first-winter + Orin journal + L05. The rite-site for `the_offering`; sets up the
ledger that shows Vaun's column blank.)*

---

**PLACE-06 · `place-undercroft-sealed` · place · `unbroken_light` · explore · revealed_by_solve: `undercroft-fog`**
*references: [`happened-orin-sealed`, `human-galleries-unruled`]*

> the gallery being of a height the survey could not rule, and a length the survey did not
> reach the end of. sealed. the seal is entered. the sealer is entered as the mason, last
> down. nothing below is surveyed. when you find the cold arch at the bottom of the stair,
> bow at it. there is someone there to bow to.

*(R04 last lines + Orin marginalia. The deepest place; the door Orin made himself into.)*

---

### THREAD: `happened` — what happened  *(red)*

The break and the going-out. Iss came to believe the surface had healed (he was right), dug
past the Line seeking a way up (there was none), and the dark came through. The Break is told
three ways that cannot all be true.

---

**HAPPENED-01 · `happened-the-doubt` · happened · `stone_iss` · explore · revealed_by_solve: `stone-iss-wall`**
*references: [`who-iss-friend`, `surface-iss-was-right`]*

> iss began to talk past the deep line. not over it. past it. at table, easy: that the long
> cold cannot last forever, that a sky that failed can mend, that a man could climb the stair
> and put his hand out and find the snow gone. the young ones leaned in when he said it. the
> lower lamps began to gutter where he had been talking.

*(Vaun ninth-winter + Mara ninth-winter. The fraying. Mara's "the lower levels gutter where
Iss has been talking — I will not write that I think those are the same thing. I have written
it.")*

---

**HAPPENED-02 · `happened-ways-are-wall` · happened · `stone_iss` · explore · revealed_by_solve: `iss-warm`**
*references: [`happened-no-wall`, `surface-iss-was-right`]*  **← CONTRADICTED BY `happened-no-wall`**

> the ways are a wall. keep the ten and you are inside it and the watching stays out in the
> cold and counts and cannot touch you. i have kept them every winter of my life and it has
> never come inside. not once. not for me. a people safe inside a wall may unbar the gate and
> walk up to a healed morning whenever they have the nerve.

*(Iss D09 / the-ways-are-a-wall, the warm misreading. This is the LIE — the warm reading the
player is steered to trust. The catch card directly contradicts it. The trusting route leads
to the dead shrine.)*

---

**HAPPENED-03 · `happened-no-wall` · happened · `stone_iss` · explore · revealed_by_solve: `no-wall-catch`**
*references: [`happened-ways-are-wall`, `surface-iss-was-right`, `human-they-were-kept`]*  **← CONTRADICTS `happened-ways-are-wall`**

> a wall does not let you out the far side and pull you back in warm. a wall does not choose
> who it lets through. turn his key on the other stones and it does not say what he said. it
> gives the word the keepers kept for the one who turned away. no wall was ever built here.
> they were the reaching, let in.

*(Orin L07 "a wall does not choose" + the catch-stone no-wall-was-ever-built-here. This is the
overturning. It cites the warm card to mark it false and hands off to the human thread.)*

---

**HAPPENED-04 · `happened-the-break` · happened · `stone_brann` · explore · revealed_by_solve: `stone-brann`**
*references: [`happened-break-accident`, `happened-break-betrayal`, `happened-break-mercy`]*

> iss took eight of the young ones and the good picks past the chalk on the stair, seeking the
> way up. something came up the stair behind him. the forward lamps on the lower three levels
> were out. all of them. there was no count. they were out. then it came up the way water comes
> up a cistern. lamp by lamp.

*(Vaun twelfth-winter + Brann L09. The pivot. The three accounts that follow cannot all be
true — this card is the hub that points at all three.)*

---

**HAPPENED-05 · `happened-break-accident` · happened · `stone_brann` · explore · revealed_by_solve: `stone-brann`**
*references: [`happened-break-betrayal`, `happened-break-mercy`]*  **← CONTRADICTS the other two**

> i was on the night-watch. the deep line gave way. the rock there is old and wrong-cut and it
> came down. not pushed. not opened. fell. they will tell you he opened it. they were not awake.
> i was awake. it was a floor that was always going to drop and he was the weight that happened
> to be standing there.

*(Brann L09 — the Break as accident. Only Brann was on watch; only Brann had not slept in a
generation.)*

---

**HAPPENED-06 · `happened-break-betrayal` · happened · `the_cold_hearth` · rumor · revealed_by_solve: `iss-warm`**
*references: [`happened-break-accident`, `happened-break-mercy`]*  **← CONTRADICTS the other two**

> do not believe brann. a man who has not slept sees a wall fall and calls it weather. i put my
> hands on the broken edge and it was cut. tooled. opened from our side, a course at a time,
> over winters. he told you it was a wall that keeps the cold out, then spent his life quietly
> taking it down.

*(Orin L10 — the Break as betrayal. Found carried west to the dead shrine. card_kind rumor: it
is the account the trusting route reaches before the catch, and the catch reframes it.)*

---

**HAPPENED-07 · `happened-break-mercy` · happened · `the_far_water` · explore · revealed_by_solve: `seventh-shrine`**
*references: [`happened-break-accident`, `happened-break-betrayal`]*  **← CONTRADICTS the other two**

> he did open it, by hand, over winters. but he did not open it to save himself. he had a way
> up one man could have walked alone in a night and never been missed. he spent a year opening
> the line wide enough for all of us — wide enough to carry the old and the children and the
> deep-bird's cage. he measured the cage with his hands to be sure the gap would take it. that
> is the man.

*(Mara L11 — the Break as mercy. The kindest account, the one no stone confirms. Three hands,
one Break; the dark does not say which is true, only that all three end the same way.)*

---

**HAPPENED-08 · `happened-orin-sealed` · happened · `unbroken_light` · explore · revealed_by_solve: `orin-threshold`**
*references: [`who-orin-mason`, `place-undercroft-sealed`, `human-they-were-kept`]*

> i am to seal it. twenty-one courses and a capstone and the first of the deep in the hollow,
> the same as a cairn, because i do not know how else to close a thing but the way i was taught
> to honor one. when the wall is capped i will be inside it. i have known this since the
> thirteenth mark and laid the courses anyway. he capped it from the inside.

*(Orin journal + marginalia. The seal that held. "We know he capped it from the inside because
there is no scaffold below the cap and no man lays the last course of an arch from above.")*

---

**HAPPENED-09 · `happened-going-out` · happened · `kept_light_home_01` · explore · revealed_by_solve: `undercroft-fog`**
*references: [`human-lamp-roll-counts-down`, `human-they-were-kept`]*

> family by family, lamp by lamp, the hold went dark. mara relit the same lamp on the second
> level six times in a day; vaun found it dark, she lit it, it was dark again before she reached
> the next. she stopped looking behind her. a lampwright who looks behind her cannot keep the
> row ahead. one light, somewhere below, did not go out.

*(Mara going-out + Vaun going-out. The long sad middle. Hands off to the lamp-roll that counts
down and the kept-light that needs no hand.)*

---

### THREAD: `surface` — what is on the surface  *(grey)*

What is up here with the players now — the living NPCs who lie or half-remember; the lost
Mouth; and the thing that is true: it does not chase, it waits and watches and takes what
stops being watched. And the slow turn: Iss was right about the sky.

---

**SURFACE-01 · `surface-aro-lie` · surface · `first_report_lectern_01` · rumor**
*references: [`place-deep-line`, `surface-pell-truth`]*  **← CONTRADICTED on arrival**

> the painted line on the big stair? step right over it, friend. that is the locals keeping the
> soft folk out so they can have the warm to themselves. cross it and keep going. that is where
> it gets good. sleep wherever you like down there. it is a hole. holes do not have ears.

*(Aro, Set-A lie. A rumor card: the deep contradicts every line of it — the line is the Deep
Line, the sleeping are reached, the name is heard. Flips to `contradicted` once the player
stands at `place-deep-line`.)*

---

**SURFACE-02 · `surface-wenna-folk` · surface · `first_report_lectern_01` · rumor**
*references: [`place-seven-ways`, `surface-seventh-marker`]*

> gran used to say there were seven somethings you had to mind down there. seven. i only ever
> remember six and i always forget a different one. light, and the line, and the bird, and the
> bowing, and the giving, and — see, there is the sixth gone again. you do not say the cold's
> name. that one i remember, because she would go white when i tried.

*(Wenna, Set-A garbled-truth. The accidental Rosetta — every charm is a softened rite. Her
"always forget a different one / only ever six" foreshadows the seventh that is refused.
Verifies against `place-seven-ways`.)*

---

**SURFACE-03 · `surface-pell-truth` · surface · `first_report_lectern_01` · explore**
*references: [`surface-watcher-counts`, `surface-aro-lie`]*

> i will tell you the only true thing i have. it does not chase. whatever is down there, it does
> not chase you. it waits, and it watches, and it takes what stops being watched. so be watched.
> stay where your people can see you. i minded six of the seven. i have thought a long time about
> what the seventh would have cost me, and it would have cost less than the not-minding has.

*(Old Pell, Set-A hard-truth. The human mirror of the Watcher — he remembers, and it costs him.
This is the operating rule of the whole horror, stated by a living man so it is not the
Watcher's self-serving claim.)*

---

**SURFACE-04 · `surface-iss-was-right` · surface · `the_threshold` · explore · revealed_by_solve: `no-wall-catch`**
*references: [`happened-no-wall`, `happened-the-doubt`]*

> he was right that the surface had healed. he was wrong about which way it lay. he opened the
> line looking for home and let home in from the other side. do not strike his name to spite
> him. strike it because the record could not bear to keep writing it.

*(R06 margin + Iss L15. The tragedy resolved: Iss's hope was correct and fatal. Not a villain —
a man right about the sky and wrong about the door. Cites the catch and the doubt.)*

---

**SURFACE-05 · `surface-watcher-counts` · surface · `first_report_lectern_01` · explore · revealed_by_solve: `m1-named-habit`**
*references: [`surface-pell-truth`, `human-names-over-heads`, `human-the-record-opens`]*

> the record was open before you found it. it counts the living by name, and grades them by
> laws no one was told. it does not close at the rite. it named a habit of yours before you knew
> it was a custom. you were measured before you were told. that was the kindness, and the whole
> of the kindness.

*(m1-record-opens + m1-named-habit + Watcher B3. The presence that knows your name. Points at
the human-thread cards without saying what the names are.)*

---

**SURFACE-06 · `surface-sixth-blank` · surface · `rune_rosetta` · explore · revealed_by_solve: `rosetta-ring`**
*references: [`place-seven-ways`, `surface-seventh-marker`]*

> the line against the sixth way is left blank. speak not the name — and then nothing, where
> the to-whom would go. the first keepers were asked, and did not answer, and the blank is
> theirs. it costs you nothing to be silent.

*(R01 sixth-way blank + R07. The Unspoken (`the_unspoken`) as a hole in the record. The blank
is load-bearing: the record will not name the thing the rent is paid to.)*

---

**SURFACE-07 · `surface-seventh-marker` · surface · `the_far_water` · explore · revealed_by_solve: `seventh-shrine`**
*references: [`who-sella-token`, `surface-wenna-folk`, `human-names-over-heads`]*  **← CONTRADICTS the "six markers" of the official count**

> there should be six markers. i counted them. there are six and then one more place where a
> marker should go and there is no marker there, only the grey, and a person standing in the
> grey. the last marker is not the last. do not let the count be six only. count again at the
> shore.

*(Sella D06/copybook + L04. The seventh the land refused — DISTINCT from Iss. Sella saw it
first, at eight, and was read as a child drifting. Contradicts the official six.)*

---

### THREAD: `human` — were they human?  *(black)*

> **This thread is never answered by a card. It is answered by holding these cards together.**
> The official record counts a person exactly as it counts a jar or a lamp; when heads come off
> the roll the names do not; the surplus that will not reconcile "is the kept"; and the reader
> is filed into the same book. No card states the induction twist. The player infers it — that
> the taken were people, fully, and that being made a watcher is what becomes of a person who
> fails the ways, which is the choice the players themselves now face.

---

**HUMAN-01 · `human-offering-ledger` · human · `offering_cairn_01` · explore · revealed_by_solve: `stone-vaun`**
*references: [`who-vaun-counted`, `place-cairn`]*

> the cairn at the shaft-mouth, returns to the deep. hand, taken of the deep, returned to the
> deep, kept even. mara: eleven, eleven, yes. orin: nine, nine, yes. vaun: six, then a great
> count, returned nothing, no. the one called vaun is the only open column in this book. i am
> instructed to keep entering him and not to strike the column.

*(R03 offering ledger. A person accounted in the same two columns as ore and water. The open
column foreshadows the open markers' column the reader fills.)*

---

**HUMAN-02 · `human-lamp-roll-counts-down` · human · `kept_light_home_01` · explore · revealed_by_solve: `undercroft-fog`**
*references: [`happened-going-out`, `human-they-were-kept`]*

> on roll two hundred fourteen, lit two hundred fourteen, unlit none. then unlit three. then the
> unlit no longer entered, there is not time to enter them. lit one hundred thirty-one. lit
> forty-one. lit nineteen. lit nine. lit three. lit one. i can't keep them all lit. — m.

*(R05 lamp-count rolls + Mara's signed line. The catastrophe read off the right-hand total —
214 down to 1. The decay shown structurally, never announced.)*

---

**HUMAN-03 · `human-ration-redivided` · human · `stone_of_reckoning` · explore**
*references: [`human-names-over-heads`, `who-deep-market`]*

> a head comes off the roll and i strike it and the next day the store is short as though the
> head still ate. the form has a field for heads and a field for loaves. it has no field for a
> head that is off the roll and still hungry. i am not putting that in the form. i am putting it
> here. then i am closing the book.

*(R09 ration schedule margin. The form counts heads exactly as it counts jars and does not ask
where a struck head goes — it only re-divides the bread. The "still hungry" off-roll head is
the taken, never named as such.)*

---

**HUMAN-04 · `human-hand-as-lamp` · human · `kept_light_home_01` · explore**
*references: [`human-lamp-roll-counts-down`, `human-they-were-kept`]*

> inventory, lights set apart, returned from below the line. four: the deep-bird of the third
> coop. down. did not come up. entered with the lamps for want of another roll to enter it on.
> five: a hand-lamp, returned from the cold place lit. it should not be. it is set apart with
> the cold ones. it is being counted with them. i do not look at entry five.

*(R11 inventory. A living warning-bird and a wrong-warm hand both filed as objects. The form
does not notice the difference — that is the whole engine of the thread.)*

---

**HUMAN-05 · `human-names-over-heads` · human · `stone_of_reckoning` · explore · revealed_by_solve: `m1-named-habit`**
*references: [`human-ration-redivided`, `human-the-record-opens`, `surface-watcher-counts`]*

> the four counts, squared. heads, lamps, names, markers. they are to come even. they do not.
> the going-out: nine heads, two hundred fourteen names. the names do not come off when the
> heads do. i have checked them. they answer to no head and are still on the roll. something is
> being counted that is not at the table. the surplus is not an error. the surplus is being kept.

*(R10 reconciliation. 214 names against 9 heads — the keystone of the human thread. "The
surplus is being kept" is as close as the record comes; it never says what the surplus IS. The
player joins it to the induction-twist card.)*

---

**HUMAN-06 · `human-galleries-unruled` · human · `unbroken_light` · explore · revealed_by_solve: `undercroft-fog`**
*references: [`place-deeper-wrong`, `place-undercroft-sealed`]*

> the deep galleries are dug wrong. too tall, too long. i measured the threshold and a man fits
> it. i measured the bottom and i do not know what fits it. i have stopped writing down the
> heights below the line. they were not built for us.

*(R04 survey margin + Orin "dug to no reach I know." The geography as evidence: hands that dug
the deepest galleries had stopped being quite man-sized. Wrong-scale = the slow loss of human,
shown in stone.)*

---

**HUMAN-07 · `human-they-were-kept` · human · `unbroken_light` · explore · revealed_by_solve: `undercroft-fog`**
*references: [`human-names-over-heads`, `human-the-record-opens`, `happened-going-out`]*

> they did not depart. it is written of the faithful that they did not depart. the word for what
> they did instead is the word this record uses for a stone, and for a flame, and for the cold
> marker at the threshold, and it does not change between the people and the things. the room
> rebuilds wrong. one fire is kept, attended by no one.

*(L16 the record's last hand + undercroft-fog. The induction twist approached and NOT stated:
"kept" is the word for a person and a stone alike, and the faithful "did not depart." The
player supplies the meaning the record withholds.)*

---

**HUMAN-08 · `human-the-record-opens` · human · `first_report_lectern_01` · explore · revealed_by_solve: `m1-record-opens`**
*references: [`human-names-over-heads`, `human-they-were-kept`, `surface-watcher-counts`]*

> it is observed that new hands are at the mouth. they carry lamps, and the lamps are lit, and
> this is well, and it has begun the same way every time. the rolls of those before are closed
> and were not reconciled. there is room in the markers' column. there has always been room. an
> open column is a thing that fills. keep your lamp.

*(R16 the sheet-after / m1-record-opens. The reader filed into the same book as everyone above.
The twist completes HERE, in the player's head: the open column is for them. Never stated; the
"it has begun the same way every time" + "an open column is a thing that fills" does the work.)*

---

## HOW EACH THREAD BECOMES ANSWERABLE (cross-reference, never a single card)

> The Recovery Archive UI shows a thread "answered" when its citation web closes — the key
> cards are held and their references resolve. No card is the answer; the answer is the join.

- **`who`** — answered by reading the six keepers as PEOPLE (WHO-02…07) against the market that
  made them a people (WHO-01). Closes when all six keeper cards are held: the player knows them
  by their habits (Vaun's count, Mara's reading, Orin's grooves, Brann's watch, Sella's token,
  Iss's kindness), not their fates.

- **`place`** — answered by the descent geography: came-down (PLACE-01) → seven ways (PLACE-02)
  → the levels (PLACE-03) → the Line (PLACE-04) → the sealed Undercroft (PLACE-06). Closes when
  the player can say *why down, not out* (the warmth) and *what the Line was for* (the edge of
  reach). The wrong-scale (PLACE-03 → HUMAN-06) is the seam into the human thread.

- **`happened`** — answered by the doubt (HAPPENED-01) → the warm lie (HAPPENED-02) → the catch
  that contradicts it (HAPPENED-03) → the Break hub (HAPPENED-04) and its three irreconcilable
  accounts (HAPPENED-05/06/07). Closes NOT by picking which Break account is true — it is
  deliberately unresolved — but by understanding that all three end the same way and that Iss's
  doctrine was the lie the catch overturns.

- **`surface`** — answered by setting the surface NPCs' claims against the deep: Aro's lie
  (SURFACE-01) is contradicted on arrival; Wenna's charm (SURFACE-02) verifies as garbled rite;
  Pell's hard truth (SURFACE-03) — *it waits, it watches, it takes what stops being watched* — is
  confirmed by the Watcher that counts (SURFACE-05). Closes with *Iss was right about the sky*
  (SURFACE-04): the tragedy, not the monster.

- **`human`** — **NEVER answered by a card.** Answered ONLY by holding the dehumanization cards
  (HUMAN-01 person-as-ledger-column, HUMAN-03 head-as-jar, HUMAN-04 hand-as-lamp, HUMAN-05 214
  names / 9 heads, HUMAN-06 galleries not built for us) beside the induction-twist cards (HUMAN-07
  *they did not depart; "kept" is the word for a stone and a person alike*; HUMAN-08 *an open
  column is a thing that fills*, and the reader filed into the same book). The player infers:
  **they were people, fully; the taken are the surplus the record keeps; the open column is for
  whoever fails the ways next — which is the choice the players themselves now hold.** The archive
  states none of this. It supplies the columns and lets them not come out even.

---

## DRAFT ADDITIVE SQL — `thread_cards` INSERT  *(do NOT run against live; integrate under guards)*

> Additive, parse-clean, idempotent (`on conflict do nothing`). `body_voice_key` references the
> keys proposed in §VOICE-KEYS (added to `voice.ts` at integration; the column is nullable so
> this seeds safely before the keys exist, then a follow-up `update` wires `body_voice_key`).
> `anchor_site_id` values are all ENABLED sites in `sites.yml`. `revealed_by_solve` values are
> all real `puzzle_key`s in `puzzles_seed.sql`. `references_card_key[]` are card_keys defined in
> this same block. Run AFTER 0005_threads.sql, as service_role (RLS bypass — spoiler table).

```sql
-- DRAFT — design/content/thread-archive.md → public.thread_cards (0005 schema)
-- NOT THE LIVE SEED. Integrate after review; body_voice_key keys land in voice.ts first.
begin;

insert into public.thread_cards
  (card_key, thread_key, title, body_voice_key, anchor_site_id, card_kind,
   references_card_key, revealed_by_solve, sort_order)
values
  -- ===== WHO =====
  ('who-deep-market','who','the deep market','cardWhoDeepMarket','stone_of_reckoning','explore',
     '{}', null, 10),
  ('who-vaun-counted','who','the founder who counted','cardWhoVaunCounted','stone_vaun','explore',
     array['who-mara-read','human-offering-ledger'], 'stone-vaun', 20),
  ('who-mara-read','who','the lampwright who read','cardWhoMaraRead','stone_mara','explore',
     array['who-vaun-counted','who-sella-token'], 'stone-mara', 30),
  ('who-sella-token','who','the under-warden','cardWhoSellaToken','the_far_water','explore',
     array['who-mara-read','surface-seventh-marker'], 'stone-sella', 40),
  ('who-orin-mason','who','the mason who would not bow','cardWhoOrinMason','stone_orin','explore',
     array['who-brann-watch','happened-orin-sealed'], 'stone-orin', 50),
  ('who-brann-watch','who','the watchman who would not sleep','cardWhoBrannWatch','stone_brann','explore',
     array['who-orin-mason','surface-watcher-counts'], 'stone-brann', 60),
  ('who-iss-friend','who','the best of the young ones','cardWhoIssFriend','stone_iss','explore',
     array['happened-the-doubt','surface-iss-was-right'], 'stone-iss-wall', 70),

  -- ===== PLACE =====
  ('place-came-down','place','we came down','cardPlaceCameDown','the_threshold','explore',
     array['place-deeper-wrong'], null, 10),
  ('place-seven-ways','place','the order is seven','cardPlaceSevenWays','rune_rosetta','explore',
     array['place-came-down','surface-sixth-blank'], 'rosetta-ring', 20),
  ('place-deeper-wrong','place','the marks down the stair','cardPlaceDeeperWrong','unbroken_light','explore',
     array['place-came-down','human-galleries-unruled'], 'undercroft-descent', 30),
  ('place-deep-line','place','the deep line','cardPlaceDeepLine','stone_iss','explore',
     array['happened-the-break','surface-iss-was-right'], 'stone-iss-wall', 40),
  ('place-cairn','place','the offering-cairn','cardPlaceCairn','offering_cairn_01','explore',
     array['who-vaun-counted','human-offering-ledger'], null, 50),
  ('place-undercroft-sealed','place','the sealed undercroft','cardPlaceUndercroftSealed','unbroken_light','explore',
     array['happened-orin-sealed','human-galleries-unruled'], 'undercroft-fog', 60),

  -- ===== HAPPENED =====
  ('happened-the-doubt','happened','the doubt','cardHappenedTheDoubt','stone_iss','explore',
     array['who-iss-friend','surface-iss-was-right'], 'stone-iss-wall', 10),
  ('happened-ways-are-wall','happened','the ways are a wall','cardHappenedWaysAreWall','stone_iss','explore',
     array['happened-no-wall','surface-iss-was-right'], 'iss-warm', 20),
  ('happened-no-wall','happened','no wall was ever built here','cardHappenedNoWall','stone_iss','explore',
     array['happened-ways-are-wall','surface-iss-was-right','human-they-were-kept'], 'no-wall-catch', 30),
  ('happened-the-break','happened','the break','cardHappenedTheBreak','stone_brann','explore',
     array['happened-break-accident','happened-break-betrayal','happened-break-mercy'], 'stone-brann', 40),
  ('happened-break-accident','happened','an accident','cardHappenedBreakAccident','stone_brann','explore',
     array['happened-break-betrayal','happened-break-mercy'], 'stone-brann', 50),
  ('happened-break-betrayal','happened','a betrayal','cardHappenedBreakBetrayal','the_cold_hearth','rumor',
     array['happened-break-accident','happened-break-mercy'], 'iss-warm', 60),
  ('happened-break-mercy','happened','a mercy','cardHappenedBreakMercy','the_far_water','explore',
     array['happened-break-accident','happened-break-betrayal'], 'seventh-shrine', 70),
  ('happened-orin-sealed','happened','sealed from the inside','cardHappenedOrinSealed','unbroken_light','explore',
     array['who-orin-mason','place-undercroft-sealed','human-they-were-kept'], 'orin-threshold', 80),
  ('happened-going-out','happened','the going-out','cardHappenedGoingOut','kept_light_home_01','explore',
     array['human-lamp-roll-counts-down','human-they-were-kept'], 'undercroft-fog', 90),

  -- ===== SURFACE =====
  ('surface-aro-lie','surface','step right over it','cardSurfaceAroLie','first_report_lectern_01','rumor',
     array['place-deep-line','surface-pell-truth'], null, 10),
  ('surface-wenna-folk','surface','seven somethings','cardSurfaceWennaFolk','first_report_lectern_01','rumor',
     array['place-seven-ways','surface-seventh-marker'], null, 20),
  ('surface-pell-truth','surface','it does not chase','cardSurfacePellTruth','first_report_lectern_01','explore',
     array['surface-watcher-counts','surface-aro-lie'], null, 30),
  ('surface-iss-was-right','surface','right about the sky','cardSurfaceIssWasRight','the_threshold','explore',
     array['happened-no-wall','happened-the-doubt'], 'no-wall-catch', 40),
  ('surface-watcher-counts','surface','the record that knows your name','cardSurfaceWatcherCounts','first_report_lectern_01','explore',
     array['surface-pell-truth','human-names-over-heads','human-the-record-opens'], 'm1-named-habit', 50),
  ('surface-sixth-blank','surface','the blank sixth way','cardSurfaceSixthBlank','rune_rosetta','explore',
     array['place-seven-ways','surface-seventh-marker'], 'rosetta-ring', 60),
  ('surface-seventh-marker','surface','the last marker is not the last','cardSurfaceSeventhMarker','the_far_water','explore',
     array['who-sella-token','surface-wenna-folk','human-names-over-heads'], 'seventh-shrine', 70),

  -- ===== HUMAN (never resolved by a single card) =====
  ('human-offering-ledger','human','the open column','cardHumanOfferingLedger','offering_cairn_01','explore',
     array['who-vaun-counted','place-cairn'], 'stone-vaun', 10),
  ('human-lamp-roll-counts-down','human','two hundred fourteen to one','cardHumanLampRoll','kept_light_home_01','explore',
     array['happened-going-out','human-they-were-kept'], 'undercroft-fog', 20),
  ('human-ration-redivided','human','a head off the roll, still hungry','cardHumanRation','stone_of_reckoning','explore',
     array['human-names-over-heads','who-deep-market'], null, 30),
  ('human-hand-as-lamp','human','entry five','cardHumanHandAsLamp','kept_light_home_01','explore',
     array['human-lamp-roll-counts-down','human-they-were-kept'], null, 40),
  ('human-names-over-heads','human','nine heads, two hundred fourteen names','cardHumanNamesOverHeads','stone_of_reckoning','explore',
     array['human-ration-redivided','human-the-record-opens','surface-watcher-counts'], 'm1-named-habit', 50),
  ('human-galleries-unruled','human','not built for us','cardHumanGalleries','unbroken_light','explore',
     array['place-deeper-wrong','place-undercroft-sealed'], 'undercroft-fog', 60),
  ('human-they-were-kept','human','they did not depart','cardHumanTheyWereKept','unbroken_light','explore',
     array['human-names-over-heads','human-the-record-opens','happened-going-out'], 'undercroft-fog', 70),
  ('human-the-record-opens','human','an open column is a thing that fills','cardHumanRecordOpens','first_report_lectern_01','explore',
     array['human-names-over-heads','human-they-were-kept','surface-watcher-counts'], 'm1-record-opens', 80)

on conflict (card_key) do nothing;

commit;
```

---

## VOICE-KEYS — proposed `voice.ts` entries for each card body  *(integration step)*

> The engine never hardcodes story (INV-1 / the voice rule). Each card's `body` text above
> lives behind one of these keys, added to the `voice` object in `discord/src/voice.ts` in the
> Watcher register (lowercase, sparse, certain). These are RECOVERED-MATERIAL bodies — the
> Watcher is reciting the record, so the register holds. A `cardBody(key)` resolver (mirroring
> `customPhrase`) maps key → fn; an unknown key degrades to a generic in-register line, never a
> leaked identifier. The bodies are exactly the card bodies above; reproduced here as the
> integration source of truth.

```ts
// voice.ts — APPEND to the `voice` object (DRAFT; bodies = thread-archive.md card bodies verbatim).
// Each returns the recovered text for one Recovery Archive card. Register: lowercase, sparse.
// (Showing the keys + first line; integrate the full body text from the cards above 1:1.)

cardWhoDeepMarket()       { return 'eighteen stalls at the fourteenth mark...'; },
cardWhoVaunCounted()      { return 'the one called vaun kept eleven jars of oil and gave the deep none...'; },
cardWhoMaraRead()         { return 'the one called mara kept every lamp in the hold lit...'; },
cardWhoSellaToken()       { return 'the under-warden. eight years old...'; },
cardWhoOrinMason()        { return 'the one called orin set seven courses a day of good stone...'; },
cardWhoBrannWatch()       { return 'the one called brann had the night-watch by his own asking...'; },
cardWhoIssFriend()        { return 'the one called iss was the best of the young ones...'; },

cardPlaceCameDown()       { return 'it is set down, that we came down out of the long cold...'; },
cardPlaceSevenWays()      { return 'the order is seven, and we are tenants of seven...'; },
cardPlaceDeeperWrong()    { return 'depth in marks carved down the rail of the stair...'; },
cardPlaceDeepLine()       { return 'the line at the thirty-third mark...'; },
cardPlaceCairn()          { return 'the offering-cairn at the shaft-mouth above the cisterns...'; },
cardPlaceUndercroftSealed(){ return 'the gallery being of a height the survey could not rule...'; },

cardHappenedTheDoubt()    { return 'iss began to talk past the deep line. not over it. past it...'; },
cardHappenedWaysAreWall() { return 'the ways are a wall. keep the ten and you are inside it...'; },
cardHappenedNoWall()      { return 'a wall does not let you out the far side and pull you back in warm...'; },
cardHappenedTheBreak()    { return 'iss took eight of the young ones and the good picks past the chalk...'; },
cardHappenedBreakAccident(){ return 'i was on the night-watch. the deep line gave way...'; },
cardHappenedBreakBetrayal(){ return 'do not believe brann. i put my hands on the broken edge and it was cut...'; },
cardHappenedBreakMercy()  { return 'he did open it, by hand, over winters. but not to save himself...'; },
cardHappenedOrinSealed()  { return 'i am to seal it. twenty-one courses and a capstone...'; },
cardHappenedGoingOut()    { return 'family by family, lamp by lamp, the hold went dark...'; },

cardSurfaceAroLie()       { return 'the painted line on the big stair? step right over it, friend...'; }, // SET-A voice (see note)
cardSurfaceWennaFolk()    { return 'gran used to say there were seven somethings...'; },                  // SET-A voice
cardSurfacePellTruth()    { return 'i will tell you the only true thing i have. it does not chase...'; }, // SET-A voice
cardSurfaceIssWasRight()  { return 'he was right that the surface had healed...'; },
cardSurfaceWatcherCounts(){ return 'the record was open before you found it...'; },
cardSurfaceSixthBlank()   { return 'the line against the sixth way is left blank...'; },
cardSurfaceSeventhMarker(){ return 'there should be six markers. i counted them...'; },

cardHumanOfferingLedger() { return 'the cairn at the shaft-mouth, returns to the deep...'; },
cardHumanLampRoll()       { return 'on roll two hundred fourteen, lit two hundred fourteen...'; },
cardHumanRation()         { return 'a head comes off the roll and i strike it...'; },
cardHumanHandAsLamp()     { return 'inventory, lights set apart, returned from below the line...'; },
cardHumanNamesOverHeads() { return 'the four counts, squared. heads, lamps, names, markers...'; },
cardHumanGalleries()      { return 'the deep galleries are dug wrong. too tall, too long...'; },
cardHumanTheyWereKept()   { return 'they did not depart. it is written of the faithful...'; },
cardHumanRecordOpens()    { return 'it is observed that new hands are at the mouth...'; },
```

> **VOICE NOTE — the three SET-A surface cards.** `surface-aro-lie`, `surface-wenna-folk`, and
> `surface-pell-truth` are SPOKEN BY LIVING NPCs (Aro, Wenna, Pell) — Set A in
> `arc/corpus/npc-and-watcher-voice.md`: modern-rough, capitals, contractions, named feeling
> ALLOWED. They are quoted as found surface testimony, not the Watcher's recitation. At
> integration, store these three behind keys that are EXEMPT from the Watcher register lint
> (they are direct quotation, marked), or carry them as `npc_dialogue` rows instead of `voice`
> entries. Every other card body is the Watcher reciting the record and obeys the register.

---

## CHECKS

- **Card count:** 37 cards (7 who · 6 place · 9 happened · 7 surface · 8 human). Sits above the
  requested 24–32+ band, weighted toward the two threads that carry the hardest reveals
  (`happened` = the contradiction set; `human` = the inferred twist). ✔
- **Every `thread_key` ∈ canon.ts THREADS** (who/place/happened/surface/human). ✔
- **Every keeper named ∈ KEEPERS** (vaun/mara/sella/orin/brann/iss). ✔
- **Every way referenced ∈ CUSTOM_KEYS** (`the_kept_light` `the_deep_line` `the_dark_hours`
  `the_offering` `the_bow` `the_unspoken` `the_sacred_beast`) — used only in design notes, never
  as a stored column value (cards reference ways by their in-fiction names; the cipher/custom
  link is `puzzles.teaches_custom`, not a card field). ✔
- **Every `anchor_site_id` is ENABLED in sites.yml:** `stone_of_reckoning`, `stone_vaun`,
  `stone_mara`, `the_far_water`, `stone_orin`, `stone_brann`, `stone_iss`, `the_threshold`,
  `rune_rosetta`, `unbroken_light`, `offering_cairn_01`, `the_cold_hearth`, `kept_light_home_01`,
  `first_report_lectern_01`. All `enabled: true`. ✔ (No card anchors to a disabled
  `keeper_stone_01/02` or to an unplaced/absent id.)
- **Every `revealed_by_solve` is a real `puzzle_key`:** `stone-vaun`, `stone-mara`, `stone-sella`,
  `stone-orin`, `stone-brann`, `stone-iss-wall`, `iss-warm`, `no-wall-catch`, `seventh-shrine`,
  `orin-threshold`, `undercroft-descent`, `undercroft-fog`, `rosetta-ring`, `m1-named-habit`,
  `m1-record-opens`. All present in `puzzles_seed.sql`. ✔ (Cards with no gate use `null` — found
  on descent / by reading the surface, not solve-gated.)
- **Every `references_card_key` resolves to a card in this block** (no dangling citation). The web
  is mutual where it should be (who-vaun ↔ who-mara; the three Break accounts cross-cite each
  other) and directional where a contradiction overturns (happened-ways-are-wall ← happened-no-wall).
  ✔
- **Citation web carries real CONTRADICTIONS, not just corroboration:**
  - `happened-ways-are-wall` (Iss's warm lie) ⟂ `happened-no-wall` (the catch) — the doctrine vs.
    its refutation.
  - `happened-break-accident` ⟂ `happened-break-betrayal` ⟂ `happened-break-mercy` — the three
    irreconcilable Break accounts (Brann/Orin/Mara); deliberately never resolved.
  - `surface-aro-lie` ⟂ `place-deep-line` / `surface-pell-truth` — the lie the deep contradicts on
    arrival (rumor → contradicted).
  - `surface-seventh-marker` ⟂ the official "six markers" — Sella's count against the record's. ✔
- **The `human` thread is NOT answerable by any single card:** no card states the induction twist.
  HUMAN-07 (`they did not depart` / "kept" is the word for a stone and a person) and HUMAN-08 (`an
  open column is a thing that fills` / the reader filed into the same book) come CLOSEST and still
  stop short — they require the dehumanization cards (HUMAN-01/03/04/05/06) held alongside to mean
  anything. The §"HOW EACH THREAD BECOMES ANSWERABLE" rule for `human` makes the inference the
  PLAYER's, sourced from the official-records dehumanization + the induction twist, never stated. ✔
- **Bodies read as recovered material, no slop:** drawn from R01/R03/R04/R05/R06/R07/R09/R10/R11/R16
  and the keeper journals/letters verbatim-in-spirit; no banned phrase; emotion via the struck
  column, the re-divided loaf, "i do not look at entry five," the count that runs 214→1. Set-A
  cards keep their living register; all others keep the Watcher's. ✔
- **DRAFT SQL is additive, idempotent, parse-clean:** `begin`/`commit`, single `insert`, `on
  conflict (card_key) do nothing`, `body_voice_key` nullable (seeds before the voice keys exist),
  all FKs (`thread_key`, and at runtime `anchor_site_id`/`revealed_by_solve` are free-text in the
  0005 schema) satisfiable. NOT the live seed. ✔
- **Voice keys proposed, not inlined into the live engine:** §VOICE-KEYS gives the exact key + body
  for `voice.ts`; the SQL stores only the KEY, never English. Three SET-A cards flagged for the
  register exemption / npc_dialogue path. ✔
