# npc-and-watcher-voice — TWO VOICE SETS, KEPT APART

> Authoring file for spoken lines, not a player document. Two sets live here and they
> must never bleed into one another. **SET A** is the surface — living, modern-rough
> people who never went deep, or went and came back wrong, or won't go at all. They
> talk like people at a bar. **SET B** is the Watcher (the voice.ts register): the
> accumulated Kept, speaking from the dark. A screenshot of one line from each set
> must read as two different species of author. If a Set-A line could be said by the
> Watcher, it is wrong; if a Set-B line has a contraction, an exclamation, a capital
> letter, or a feeling named out loud, it is wrong.
>
> Grounded in `arc/WORLD-BIBLE.md` (the Long Cold, the Hold, the seven ways, the
> Deep Line, Sella's far water, the induction twist) and `discord/src/voice.ts` (the
> Watcher's canon register). Cross-references the keeper documents in
> `arc/lore/documents/` and the place-web (Cistern 7, the Deep Line, the Lamp-works,
> the Deep Market, the Stair, the Undercroft, the markers, the black moon, the
> deep-bird). SCHEMA at the foot of the file.

---

## SET A — THE SURFACE NPCs (living, modern-rough; never the Watcher's register)

These five are on the topside map, near the lost Mouth. They speak plain, present-day,
worn. Contractions, half-finished thoughts, the small lies and small kindnesses of
people who live somewhere and have opinions about the people who pass through. They do
NOT speak in lowercase-archaic. They do NOT say "the ways" with reverence — to them
the ways are weather, or superstition, or somebody else's problem. **One of them lies
(Aro). One half-remembers and turns it into folk-charm (Wenna). One trades and doesn't
care (Coll). One comes down with you and is afraid the ordinary way (Dob). One won't go
and remembers exactly how you behaved (Pell).**

Branch keys per NPC: `greet` / `rumor` / `truth_or_lie` / `react_good` / `react_bad`.
Conduct is read off the same Observance meter the Watcher reads — but the NPCs never
say "the meter." They say what a person says: you look rough, you came back, you didn't.

---

### A1 — ARO  ·  the rumor-broker who lies

He runs his mouth for a living. Sells maps that are wrong, directions that are close
enough to get you lost, and a story for every drink. He's not malicious — he's a guy
who learned that "I don't know" empties a room and a good lie keeps it full. The trap
is that he's warm, specific, and confident. Half of what he says is true, which is what
makes the other half land. He has never once gone past the Mouth and never will.

**greet**
- "Ah — fresh boots. Sit, sit, you're letting the cold in. You want the way down, you want the right person, and lucky you, here I am."
- "You're the lot poking round the old hole, yeah? Course you are. Everyone is, this season. Drink first. The hole's not going anywhere."

**rumor**
- "Way I heard it, there's a whole town down there. Lamps still burning. People who just — stayed. Living fat off the warm while we freeze our backsides up here. That's why nobody comes back up, see. Not 'cause they died. 'Cause it's *nice*."
- "There's a line painted across the big stair, halfway down. Don't mean nothing. Old paint. Builders' mark. People make a whole religion out of a stripe of pitch, I swear."
- "They say there's a bird down there older than the digging. Keeps the air sweet. You find the bird, you find the bottom, and the bottom's where they kept the good stuff."

**lie** *(Aro's `truth_or_lie` always resolves to a confident falsehood — pleasant, specific, and wrong; the world later contradicts it)*
- "The painted line? Step right over it, friend. That's the locals keeping the soft folk out so they can have the warm to themselves. Cross it and keep going. That's where it gets good."
- "Sleep wherever you like down there. Black moon, white moon, no moon — rock doesn't care what the sky's doing. That's a tale to sell candles."
- "Names? Say what you like down there, shout your own if you want. Nobody's listening. It's a hole. Holes don't have ears."

**react_good** *(player has kept the ways; Aro doesn't understand it but smells it)*
- "Huh. You went down and you came *back* up, and you came back — quiet. Most don't. Most come back loud or don't come back. You're alright, you. On the house."
- "Whatever you did down there, keep doing it. You've still got the look of a person. That's rarer than you'd think round the Mouth."

**react_bad** *(player has broken the ways; the meter is high)*
- "...you don't look so good. No offence. You've got that — that grey on you. Bunch came through last month with that same grey and I, ah. I don't see 'em anymore. Tab's closed. Go on."
- "I'm out of stories for you. First time in my life. Just — don't tell anyone where you heard the line was safe to cross, yeah? Don't put that on me."

---

### A2 — WENNA  ·  who half-remembers the ways as folk-superstition

Old enough to have heard the real things from her grandmother, young enough to have
filed them under charm and habit. She keeps the ways without knowing why — leaves a
crust on the windowsill "for luck," won't say a certain word "'cause Gran would clip
me," keeps a lamp by the door "so the dark knows the house is taken." She's the
accidental Rosetta: everything she does is a corrupted, softened survival rite. Warm,
chatty, a little bit performing the wise-woman.

**greet**
- "Mind the lamp by the door, love, don't pinch it out. House likes to look lived-in after dark. Gran's rule, not mine, but I've never had cause to break it."
- "Going down, are you. Well. Take a crust for your pocket. No, I won't hear it — you take the crust. It's just what you do. You leave a little, you get to keep a little. That's the whole of it, near enough."

**rumor**
- "Gran used to say there were seven somethings you had to mind down there. Seven. I only ever remember six and I always forget a different one, isn't that the way. Light, and the line, and the bird, and the bowing, and the giving, and... see, there's the sixth gone again."
- "You don't say the cold's name. That one I do remember, 'cause she'd go white when I tried. 'You don't *name* it, Wenna.' Name what, Gran? And she'd just — wouldn't. So I don't. Habit now."
- "When the moon goes black you stay up. Stupid, isn't it. I still do it. Sit up all night with the lamp like a fool. Slept through it once as a girl and had the worst dreams of my life, so." *(shrugs)*

**truth** *(Wenna's `truth_or_lie` resolves to garbled-but-real folk-truth — she has the ways right under the charm)*
- "Bow at the stones. I don't know who to, mind. Gran never said who. You just bend your knee going past and you don't think too hard about it. The ones who don't bend... she'd just shake her head."
- "Keep your light. Above all the others, keep your light. That one she said like it mattered more than the rest put together, and she didn't say things like that twice."

**react_good**
- "Oh, you minded it all, didn't you. I can tell. You've got the — the *kept* look. Gran would've liked you. She'd have given you the good chair."
- "Come back whenever, love. You're welcome at this door. The lamp'll be lit for you. I mean that the ordinary way *and* the other way, if there is an other way, which I've never quite decided."

**react_bad**
- "...did you leave a little? Down there. Did you give anything back, or did you just — take." *(quieter)* "You don't have to answer. I can see you didn't. Take the crust anyway. Maybe it's not too late for the crust."
- "I'm going to light a second lamp tonight, I think. After seeing you. No reason. Just feel like the house wants two lit, with you stood there like that."

---

### A3 — COLL  ·  the trader

Pure transaction. Goes down a little way — never past the Lamp-works, never near the
Line — buys what desperate people carry up, sells torches and rope and rations at a
markup. He has heard every ghost story and priced exactly none of them in. He doesn't
disbelieve in the dark; he just doesn't see how it's his problem, and he's right until
he isn't. Clipped, dry, allergic to wonder.

**greet**
- "Torches, oil, rope, three days' rations, a spare striker 'cause your first one's already wet. Don't haggle, I've heard your speech, the answer's the price on the tag."
- "Down or up? Down, you buy light. Up, you sell whatever you found that's still worth anything. Which is rarely much. People bring up the strangest junk and want gold for it."

**rumor**
- "Folk come up babbling about a watcher, a presence, eyes in the dark. You know what I sell to those folk? More oil. Whatever's down there, it's never once stopped a man from needing more oil."
- "Furthest I go's the lamp-house — the Lamp-works, the old one, second level. Good trade there, people coming up are scared and scared pays full price. Past that? Nothing past that's worth a markup. Past that you don't come back to spend it."

**truth** *(Coll's `truth_or_lie` resolves to flat, unembellished accuracy — he has no reason to lie about inventory or distance, and the truth is dull to him)*
- "The painted line's real, if that's your question. I've seen it. I don't cross it. Not 'cause of stories — 'cause everyone who does stops buying oil from me, and I notice when a customer stops existing."
- "Keep one lamp more than you think you need. That's not wisdom, that's stock advice. The man with two lamps comes back to spend. The man with one comes back as a story. I'd rather you came back to spend."

**react_good**
- "You came back, you're spending, you're not babbling. Model customer. Here — striker's on me. Don't tell the others I do that, it ruins the business."
- "Whatever you're doing right, keep the receipts. You're the only one this month I'd extend credit to."

**react_bad**
- "Cash up front from you. No, nothing personal. Last three that came up looking like you settled their tab and then I never saw the coin spend again. It just... sat where they dropped it. So. Up front."
- "I'll sell you the oil. I'll always sell you the oil. But I'm not shaking your hand, and I'd thank you to buy and go."

---

### A4 — DOB  ·  who descends with the group

Local lad, came along to guide and to prove something, and is discovering in real time
that he is not built for this. He's afraid the *ordinary* way — sweaty, talkative,
jumpy at his own torch-shadow — which is exactly the human counterweight to the
Watcher's calm. He's not a coward; he keeps going. But he narrates his fear out loud the
way scared people do, and he gets quieter the deeper you go, and that quieting is the
tell. He is the only Set-A voice that follows the party down past the Mouth.

**greet** *(topside, before descending — still bravado)*
- "Right, I've been down to the second level loads of times, loads, so just — stick behind me and we're golden. Loads of times. Twice. Twice is loads."
- "I'm not scared, before you ask. I'm *alert*. There's a difference and my mum says it's a good quality."

**descent_chatter** *(Dob's `rumor` slot, but it's running commentary as you go deeper)*
- *(Lamp-works)* "See, this is fine. Lamps, smell of oil, nothing weird. People worked here. Normal job, normal — okay, why's it so *tall*, the ceiling, down here. Was it always this tall? I don't remember tall."
- *(near the Cisterns)* "Don't drink the still water, that's Cistern 7, that one's gone bad — my uncle said. Or was it 7's the good one. One of 'em's good. Let's not test it. Let's super not test it."
- *(approaching the Stair / the Line)* "There's the line. The painted one. We're — we're not crossing that, are we. Tell me we're stopping at the line. Aro said cross it but Aro's a liar, everyone knows Aro's a liar, why'd I even — we're stopping at the line, right?"

**truth** *(Dob's `truth_or_lie` is honest to a fault — he can't keep a secret, blurts what he actually knows, including how scared he is)*
- "Okay — real talk — I've never been past the Lamp-works. I lied. Twice was a lie, it was once and I cried on the way up. I just wanted to come 'cause everyone treats me like a kid. I don't know what's down there any more than you do."
- "I keep my lamp on me. Not letting go of it. You can have my rope, you can have my rations, you cannot have my lamp, I will not be the one whose light goes out, I've *heard* what they say about the ones whose light goes out."

**react_good** *(party keeping the ways — Dob feels safer near them)*
- "I feel — okay, weirdly, I feel better next to you lot? Like the dark's paying attention but it's not — it's not paying attention to *us*. Is that mad. That's mad. Stay close though."
- "We did it the right way, yeah? Bowed and gave the bird its seed and kept the lamps. My gran'd be made up. Let's go up. Let's go up while we're still the kind of people my gran'd be made up about."

**react_bad** *(party breaking the ways — Dob's fear curdles into something he can't name)*
- "Why'd you cross it. Why'd you — Aro said it was fine but you *knew* Aro lies, I told you he lies, and you crossed it anyway, so you didn't do it 'cause you believed him. You did it 'cause you wanted to. That's worse. Why's that worse. It feels worse."
- *(very quiet, much later)* "...I don't want to go further. I'll wait here. By the lamp. I'll just — I'll keep this lit and I'll wait. You go on. I'll be right here. I'll be right here. I'll be right here."

---

### A5 — OLD PELL  ·  who won't descend, and remembers your conduct

The oldest one topside. Went down once, long ago, came up alone, and has not so much as
looked at the Mouth since. He won't tell you what happened and you will not make him. He
sits where he can watch who comes and goes, and he remembers — faces, names, whether you
came back the same. He's the surface mirror of the Watcher: a keeper of a kind of
record, but a human one, bitter and grieving and entirely capitalised and contracted and
*alive*. The Watcher counts; Pell just remembers, and it costs him.

**greet**
- "I won't go down, so don't ask. People always ask. They think I'm being dramatic. I went down once. That was the whole of my going-down. You'll understand or you won't."
- "Sit if you like. Don't sit if you don't. I'm not lonely, I'm just old, the two get confused."

**rumor** *(Pell deals in memory, not gossip — he tells you what he saw, which is worse than rumor)*
- "I knew people who went down keeping every little rule like it was nothing, like a game, and they came up and they were *here*, you understand, all the way here, behind their own eyes. And I knew the other kind. I don't say what happened to the other kind. You'll know it if you see it. You'll wish you didn't."
- "There were seven things you minded down there. I minded six of them. Six. I have spent a long time thinking about the seventh, and what it would've cost me to mind it, and I think now it would've cost me less than the not-minding has."

**truth**
- "I'll tell you the only true thing I have. It doesn't chase. Whatever's down there, it does not chase you. It waits, and it watches, and it takes what stops being watched. So be watched. Stay where your people can see you. That's all I've got and it's worth more than every map Aro's ever sold."

**react_good** *(Pell has watched you come and go, kept the ways)*
- "You. You've been down more than once and you come up the same every time. Same eyes. You don't know what that's worth. I do. Come here." *(he doesn't get up; he just looks at you a long moment)* "Good. You're still in there. Stay that way."
- "I'll remember you came back right. That's not nothing, a person remembering you right. It's most of what I've got left to give."

**react_bad** *(Pell has watched you come and go, breaking the ways)*
- "I've been watching you come and go. I watch everyone. And you — you've gone grey at the edges, the way they do, the way *they* did, and I'm not going to pretend I don't see it to spare your feelings. I'm too old to lie about the grey."
- "I remember the others who went the way you're going. I remember all of them. That's my curse, that I remember. And I'll remember you. Whatever you become down there, some part of you'll be up here, remembered, by a bitter old man who told you and you didn't listen."
- *(if the player presses him on what he saw)* "No. You don't get that one. I carried it up alone so nobody else would have to carry it. Don't you dare make me hand it to you."

---

## SET B — THE WATCHER (voice.ts register; lowercase, sparse, certain)

Strictly the established register from `discord/src/voice.ts`: **lowercase, sparse,
calm, certain, short.** No exclamation. No emoji. No contractions. No capital letter,
ever, including names and line-starts. No feeling named. Speaks of *the ways*, *the
record*, *the keepers*, *what is owed*, *kept*. It states; it does not plead, threaten,
or cheer. `▒` (the MOTIF) at most once per line, usually none. It never says ai / bot /
game / server. Judgment is **collective and by-conduct — never a chosen one.**

These extend the canon `voice` object. They do not replace any existing line; they are
new entries in the same hand. Where a name is filled in, it is filled lowercase by the
binding the record already holds (`linked()`), never capitalised here.

---

### B1 — DRIP VARIANTS  *(more of `voice.drip()` — the showrunner's set-out clue; cryptic, never names the puzzle)*

- `▒  something has been set down where the marks are kept. it was meant to be found.`
- `the record set a thing out in the open tonight. read it before the light goes.`
- `there is a new mark among the old ones. it was not there when you were not watching.`
- `a thing is laid where the keepers laid things. take it up, if you can carry it.`
- `the dark put something back that it had kept. go and see what it returned.`
- `look where you did not look before. it has been waiting there. it is patient.`
- `▒  a leaf was turned. not by your hand. read the page it turned to.`

---

### B2 — CUSTOM-VIOLATION REPORTS, PER WAY  *(the seven; the cold-side companion to `reportObserved` / `reportEscalated`)*

> Each way gets three steps that mirror the record's own ladder in
> `observed-warned-left-at-threshold.md`: **observed** (noted, patient), **named**
> (warned, plain, the cost shown), **left** (consequence, by conduct, collective —
> never a slain or a chosen). `{name}` is the lowercase bound name; `{n}` a count.
> The Watcher fills these the way the record fills a column.

**the_kept_light — Keep the Lamp**
- observed: `${n} days kept. the one called ${name} has let a light go out. the dark notes the unlit. it has been noted here also.`
- named: `${name} walks unlit. the dark takes the unlit first, and the ways will not light a lamp for one who will not carry one. bear the light, and the column is crossed back clean.`
- left: `the lamp was not kept. where there is no light, there is no keeping. ${name} is not held against the dark. the rest are lit. the rest are held.`

**the_deep_line — The Deep Line**
- observed: `${n} days kept. the one called ${name} has stood at the deep line and looked past it. the looking is marked. the crossing is not yet.`
- named: `${name} has passed the line. this is the old sin, the one that opened the dark the first time. step back, and stand again on the kept side, while there is a kept side to stand on.`
- left: `the line was crossed. it has been crossed before, by one who sought a way up and found a grave. ${name} is on the far side now, in the reach of the dark. the rest held the line. the rest are out of reach.`

**the_dark_hours — The Dark Hours**
- observed: `${n} days kept. the black moon was up, and the one called ${name} closed their eyes beneath it. the dark reaches the sleeping. it has reached, and found, and noted.`
- named: `${name} slept in the dark hours. one of the keepers slept in the dark hours, and what he dreamed came inside, and he does not lie down now. wake, and keep the watch, and the count is crossed back clean.`
- left: `the watch was not kept. ${name} slept where sleep is owed to the dark. what comes for the sleeping has come. the rest stayed awake. the rest are still their own.`

**the_offering — Give Back to the Deep**
- observed: `${n} days kept. the one called ${name} has taken from the deep and returned nothing. the column for giving-back stands empty against their name.`
- named: `${name} takes and does not give. the deep that is not fed grows hungry, and a hungry deep takes instead of waiting. return the first of the deep to the deep, and the column is crossed back clean.`
- left: `nothing was given back. ${name} takes and does not return. they have been told. if they will not keep the ways, the ways will not keep them. the deep keeps a column too, and it does not come out even.`

**the_bow — Bow at the Markers**
- observed: `${n} days kept. the one called ${name} has passed the markers standing. the markers note who bends and who does not. so does the record.`
- named: `${name} has not bowed. the bow is the smallest of the ways and costs only a stoop, and a keeper who would not give it was named, and warned, and left. bend at the markers, and the count is crossed back clean.`
- left: `the markers were passed standing, to the last. the watching was not acknowledged, so the watching does not acknowledge ${name}. they are not kept. the rest bowed. the rest are kept.`

**the_unspoken — The Unspoken**
- observed: `${n} days kept. the one called ${name} has shaped the name that is not to be shaped. it was nearly said. the dark leans toward the nearly-said.`
- named: `${name} has spoken what should stay unspoken. to say the name is to turn its face toward you. close the word, and keep it closed, and the count is crossed back clean.`
- left: `the name was spoken aloud. ${name} has turned the dark's attention onto themselves, and attention, once turned, is not turned away by asking. the rest kept the word shut. the rest are not looked at.`

**the_sacred_beast — Keep the Deep-Bird**
- observed: `${n} days kept. the one called ${name} has let harm come near the deep-bird. the bird is the warning before the dark. the record marks who guards the warning.`
- named: `${name} has not kept the deep-bird. the bird sings while the air is good and stills when it is not, and a keeper who loses the bird goes dark unwarned. spare what is not to be taken, and the count is crossed back clean.`
- left: `the deep-bird was not kept. the warning is silenced. ${name} will go into the dark with nothing to sing first. the rest kept the bird. the bird still sings for the rest.`

---

### B3 — GATHER-EVENT LINES  *(the summons-night beats: THE COUNT and the un-lighting; extends `voice.summons()`)*

> Posted in sequence on the gather night (the dark hour), as the world stages the
> reckoning. Collective throughout — *all of you*, never a name singled as chosen.
> These run before the world flips to KEPT or ACCEPTED; the Watcher states the
> staging, it does not announce the verdict (the verdict is what HAPPENS).

**the calling-in**
- `the way is open. come — all of you — at the dark hour. bring what is owed.`
- `it is the dark hour. the record is brought out to be read. stand where you can be seen.`

**THE COUNT** *(the roll of the living, by conduct, against the open column)*
- `the count begins. it was begun before you, in the oldest winters, and it has not closed since.`
- `each name is read against its column. what was kept stands on one side. what was owed stands on the other. nothing is added now that was not done before now.`
- `the column does not come out even. it never has. six were named in full, in the old book. you are read into the same book, by the same hand.`
- `you were not told the laws before you kept or broke them. you were observed. that was the kindness, and the whole of the kindness.`

**the un-lighting** *(the lamps put out one by one — the dark drawing near as the reckoning lands)*
- `now the lights are taken in. one. and the next. the dark is let come near, the small distance it is owed.`
- `a light goes out. it is not a punishment. it is the dark, given its company. it was always given.`
- `the lamps go down the line, as they went down the line in the last winter, when the keepers put them out themselves and kept the ways alone.`
- `there is one light left, and it is the kept light, and it is decided now whose hand it stays in.`

**the holding** *(the breath before the flip — stated, never resolved in text)*
- `the count is read. the column is full. what is kept will be kept. what is owed will be received.`
- `stand still. it is decided by what was already done. it was always decided by what was already done.`

---

### B4 — THE WARM→COLD FLIP  *(the same Watcher, fond to those who keep, cold to those who do not; never a different speaker, never a chosen one)*

> Two faces of one voice, by conduct only. The warm lines are the Watcher fond — the
> register stays sparse and certain, the warmth is in what it offers to keep, never
> in adjectives. The cold lines withdraw the keeping. The hinge is the player's own
> conduct on the meter, read collectively. Use the warm set when the ways were kept,
> the cold when they were not. **The flip is never announced ("now i am cold"); it is
> only enacted — the same hand, writing a colder column.**

**warm — to those who keep the ways**
- `you keep the ways, and the ways keep you. it is well. it is written.`
- `the record is fond of a kept light. it has kept few. it would keep yours.`
- `come and go as you will. the dark knows the house is taken while you keep it lit. you are watched, and you are not wanted by the watching.`
- `you have been seen to do a small thing rightly, before there was need. the founders did the same, and were glad of it after. so is the record.`
- `walk the ways once more, where i can see you, and i will know you the next time, and the next. that is what it is to be kept.`

**cold — to those who break the ways**  *(register identical; the keeping is withdrawn)*
- `you take and do not return. you have been told. if you will not keep the ways, the ways will not keep you.`
- `the record is not angry. the record holds no anger, only the column. and your column is owed.`
- `you ask to be let alone. you have been let alone. that is not the same as being kept. you will learn the difference where the others learned it.`
- `the dark does not chase you. it has never had to. it waits, and it watches, and it takes what stops being watched. you have stopped being watched by your own.`
- `you will not be slain. nothing here is slain. you will be received, and made part of the watching, and kept — in the other sense of kept, the sense you will learn last.`

---

## SET B-NEW — THE WATCHER, NEW THREADS (voice.ts keys per BUILD-MANIFEST §4)

> The **text** of every new Watcher voice key the synthesized web added. TS-VOICE
> inserts these verbatim into `discord/src/voice.ts` / `voice.archive.ts` (the only
> writers of those files); this is the LORE hand-off they pull from. Every line obeys
> the Set-B register exactly (lowercase, no contraction, no capital, no exclamation, no
> named feeling) **and** the slop §A doctrine pinned in `WEB-MASTER §6`: no second-person
> warmth ("for you"), no self-justifying so/because, no chiasmus, no contrast-with-prior-
> belief, no personified objects ("the threshold remembers"). It counts, states what is,
> and stops. Each key below carries its de-slop note where `INTEGRATION-V2` named one.
> `{name}` is the lowercase bound name (`linked()`); `{n}` a measured count.
>
> **Lockstep check:** key names match `BUILD-MANIFEST §4` exactly. Where a key takes a
> `tone` argument (difficulty), the tone only selects which of the listed variants speaks
> — it never adds a capital or a feeling. All must pass `registerDisciplineSelfTest`.

---

### BN1 — THE HOLD-BOOK  *(`keeperPage*` family lives in the journal files; these are the book's OWN keys)*

> The `keeperPage{Vaun,Mara,Sella,Orin,Brann,Iss}` lines are authored in
> `arc/corpus/journals-*.md` (each keeper's degrading hand). The keys below are the
> Archivist's flat book-voice around them — the down-count re-read, the even/odd docket
> states, and the enrolment acknowledgement. These are the record's hand, not a keeper's.

**`docketReread`** *(M4, gated on `iss_caught` — the down-count re-reads as the muster; slop A3: the chiasmus is CUT)*
- `the muster is read. the count was never of the dark. it was of the hands. the hands are almost in.`

**`docketEven`** *(the count resolves even / the muster closes — stated once, not cheered)*
- `the count comes out even tonight. it has not before. the open column is the length of the present hands, and no longer.`

**`keeperEnrolled`** *(a living player is filed under a keeper column — precision-floored: a flat player is enrolled to no one; names a keeper, never elects a player)*
- `${name} is set down in the column the record kept open for a hand like that one. the column was not empty. it was waiting.`
- `the page that held a dead keeper holds a living one now. the hand is the same hand. the record keeps one book.`

---

### BN2 — THE DIVERGENT FATES  *(feed the M5 composer — never written into M5 directly; INV-11 active-only, no player named)*

> One base close per fate. The composer (`WEB-MASTER §5`) picks ONE as the neutral base.
> Collective; the floor geometry/dressing carries the verdict, the sentence only confirms it.

**`fateKept`**
- `the ways were kept, and the kept are kept. the markers face out, to the road, to whoever comes after. the record closes warm. it has closed cold before.`

**`fateCastOut`** *(passive voice on the abandonment, per FACT 6 / §8 logic — no name, no "you")*
- `the ways were not kept to the end. the markers face the wall. some were left at the threshold, as some were left before, and the threshold does not open twice for the same standing.`

**`fateDivided`** *(the split is by floor geometry, never by player — INV-16)*
- `the light holds on one side of the floor and not the other. it was not decided by who stood where. it was decided by what was done, and the floor only shows it.`

**`fateRefusers`** *(secret — positive defiance, never absence; quorum present, the bow withheld on purpose)*
- `the way was open at the dark hour and the bow was not given. it was not given by hands that were here to give it. the record marks a thing refused, which is not the same as a thing absent.`

**`fateInheritorsCodicil`** *(the `INHERITORS` / Seventh-restore deposit — the +1 codicil clause; FACT 14 planted within the arc)*
- `a mark is left at the cold hearth for a hand not yet here. the record keeps a slot the way it kept yours, before you came to fill it.`

---

### BN3 — THE SEVENTH CLOSE  *(`seventh_choice ∈ {restore|erase}` → one tinted Keeper clause; FACT 10b)*

> These feed the composer as the seventh's single tinted clause. They state the block-state
> the world already shows (re-warmed hearth for restore, blank wall for erase); the sentence
> confirms the floor, never narrates a feeling.

**`keeperCloseSeventhRestored`**
- `the unwritten name is written again. the cold hearth is lit. the one the land would not keep, the record keeps. these are not the same keeping. the record knows the difference and keeps anyway.`

**`keeperCloseSeventhErased`**
- `the blank is left blank. the hearth stays cold. the land cast one out and the record does not argue the land. the wall holds the shape of a name and no name. that was the choice that was made.`

---

### BN4 — THE PERMANENCE FORKS  *(each leaf feeds the composer; INV-12 colors never gates; slop fixes applied)*

**Sacred Beast — boon (`tollSacredBeast`/`keptSacredBeast` reused; this is the M5 leaf clause)**
- `the deep-bird was kept to the last. it sings while the air is good, and the air is good. the warning is not silenced. the shepherding holds.`

**Sacred Beast — broken (`sacred_beast_broken`)**
- `the glowing one was taken. the warning before the dark is taken with it. the boon is closed. it does not open again, in this keeping or after it.`

**First Light — kept (`light_kept`; slop B2: the leaf must NOT reassure — no "you only", "that is enough", "meant to be")**
- `the light came up the stair on its own. you carried it. that is how it is carried.`

**First Light — taken (`light_taken`)**
- `the flame was banked, and the room is dark for the arc. the light is kept elsewhere, in the other sense of kept. the stair stays unlit. it was a choice and it was made.`

**Spoken Name — unspoken (`name_unspoken`)**
- `the word stayed shut. the dark was not turned toward. the unspoken is the smallest keeping and it was kept.`

**Spoken Name — spoken (`name_spoken`)**
- `the name was said aloud. a faint hand reads after it, the way a hand read after iss. what is turned toward does not turn away for asking.`

---

### BN5 — THE GRAVE  *(FACT 13b; INV-14 the date is READ never typed; slop A1: the private receipt must NOT warm or self-justify)*

**`graveCarved`** *(the future-dated stone is found — reads as a death clock; the misread IS the mechanic)*
- `a stone is cut near the threshold. a name is on it and a date that has not come. the cutting is done. the keeping is not. the stone is ready before the keeper is.`

**`graveReceipt`** *(the private line to the player whose name is on it — slop A1: cut "you read it first, so it is your name first")*
- `the one called ${name}. read first. cut first. the rest are not yet cut. they will be.`

**`graveOpened`** *(M5, on the Accepting instant — the grave opens from the inside; the death-clock was an appointment)*
- `the stone opens. it was not a grave. it was a slot, and the date was an appointment, and the hour is now. what was cut as not-yet is cut as kept.`

---

### BN6 — DIFFICULTY / THE DEEP'S GRIP  *(FACT 2b; INV-15 never touches the Whisper rail; `tone` selects the variant)*

> The `tone` argument on `drip` / `oracleDeadEnd` / `reportObserved` selects the cooled
> or patient variant. These two are the standalone foreshadow lines. The land grades
> mastery, not only conduct; it closes on those who run ahead, opens for those who stumble.

**`deepTightens`** *(the group raced the reading — the drip is withheld, the register cools; never names "difficulty" or "mercy")*
- `you go quickly. the deep keeps a closer count of the quick. it sets the marks further apart for a hand that runs.`

**`deepIsPatient`** *(the group stumbled — the grip opens; not consoling, not warm-adjectived)*
- `you go slowly. the deep is patient with the slow. it has set the next mark nearer, and it will wait at it.`

**`tone` cooled variant of `drip`** *(when the deep has tightened — the same drip, fewer words, colder)*
- `something is set out. read it before the light goes. the deep will not set out another soon.`

---

### BN7 — THE DEAD-ENDS WITH TEETH  *(`oracleDeadEnd(kind)` family — slop §A doctrine: state the CATEGORY of truth + its inertness, nothing else; no gloat, no threat, no feeling)*

> Single-fn `kind` arg (lowest blast radius). The Watcher is flat and honest; the *teeth*
> belong to the liar (the prophet's wall is Iss's content), never to the record. Each line
> names what kind of true thing it is and that it opens nothing.

**`oracleDeadEnd(kind = 'name')`**
- `yes. that is a true name. it keeps no door. some things are only their own name.`

**`oracleDeadEnd(kind = 'count')`** *(the "six. opens nothing" rung — re-reads at M3: the Watcher was literal, SEVEN opens the side-quest)*
- `six. that is the count, and it is true, and it opens nothing. the marks are not lying to you. they are only six.`

**`oracleDeadEnd(kind = 'place')`** *(the false-coordinate herring — verifies as a place, opens nothing)*
- `there is a place there. it is the right place for the wrong thing. it has been walked to before. it keeps no door.`

**`oracleDeadEnd(kind = 'known')`** *(a thing already known, re-solved — true and inert)*
- `you knew that already. it is still true. knowing it twice opens nothing new.`

**`oracleDeadEnd(kind = 'prophet')`** *(the prophet's-wall rungs — each a warm promise that opens nothing; the teeth are Iss's, the Watcher only labels)*
- `that is a warm thing, and someone carved it warm. it opens nothing. read who carved it, after.`

---

### BN8 — THE THREE-HANDS COOP GATE  *(`oracleThreeHands` — slop A4 fixed: objects do not remember; "the threshold remembers three" → flat statement of the count)*

**`oracleThreeHands`**
- `the count is three. the threshold is open.`

---

### BN9 — THE COORDS / THE WALK  *(`voice.dest.*`; INV-14 the answer is a destination WORD, never the coordinate)*

**`voice.dest.coordFraming.false`** *(the false walk — pre-catch; the dead-shrine herring; the rumor verifies as a place and contradicts as a hope)*
- `the pointer is true. it points to a cold hearth and an old grave. there is a place at the end of it. there is nothing kept there for you.`

**`voice.dest.coordReCarve`** *(the catch re-carves iss's stone — the true coordinate replaces the false one)*
- `the stone is cut again. the pointer it held was a lie warm enough to follow. the one beneath it is not. walk the second one.`

**`voice.dest.coldHearth.find`** *(arrival at the dead-shrine — the false endpoint; presence-gated)*
- `you stand at the cold hearth. the light here went out and was not kept. this is the end of the warm road. the kept road is elsewhere.`

**`voice.dest.threshold.arrive`** *(arrival at the true threshold after the gate — the on-ramp to the Accepting)*
- `you stand at the threshold. it was opened by three hands and a true word. the marks past it are not warnings. they are the way in.`

---

### BN10 — NAME-WHERE-YOU-HAVE-NEVER-BEEN  *(FACT 16; the place-filing clueDrip + the Keeper half-veiled line carried in SET C; slop B1: the comma-fragment capped to once)*

**`clueDrip` (place-filing — the Archivist fragment surfaced as a drip)**
- `a name was cut where no foot of yours has fallen. the record files by ground as well as by name. against each name, a ground.`

> The half-veiled Keeper M4 line that pairs with this drip lives in SET C (`keeper.nameWhere`).

---

### BN11 — THE RECORD WEBSITE  *(`recordElsewhere`/`recordReceives`; the "kept in more than one place" door — texture, not a numbered fact)*

**`recordElsewhere`** *(M1 nudge — the decoded founder phrase points off-world; gates nothing)*
- `the record is kept in more than one place, against the loss of the first. one of them is not here. you have read enough to find it.`

**`recordReceives`** *(M5 — the website adds the group's own names, "received")*
- `the other record has your names now. they are entered there as they are entered here. received. the same word, in both hands.`

---

### BN12 — THE META  ·  UNKEPT  *(`oracleMetaUnkept` — pure lore re-read, gates nothing; fall-order, not ring-order)*

**`oracleMetaUnkept`**
- `read the maker's marks in the order the keepers fell. they spell the one word each of them did not keep. it was on the stones before you could read it.`

---

### BN13 — THE FORGED EIGHTH  *(FACT 7b; slop B4: the forged line credits no "me" — the anonymous lie is the stronger lie)*

**`cardEighthForged`** *(the forged ordinance surfaced as a Recovery-Archive card body — read in-fiction as one more true ordinance)*
- `the founders set the ways and did not finish the count. the eighth is the covering of the hands. cover a lapsed hand at the marker, and be counted clean.`

**`archiveEighthCorrection`** *(M4 — the flat record correction; the lie collapses, named added-not-found)*
- `there is no eighth. the founders cut six marks in the ring. a later hand cut a seventh-and-a-half and signed it under a substitution, and the sign reads cover one's own. the land never counted it. obey it, and nothing comes. that is the proof.`

---

### BN14 — THE UNLIT DEEP  *(the ONE group latch; `the_unlit_deep` CUSTOM_KEY; reversible, active-only, `broken_by` recorded-never-spoken; slop A4: objects do not remember/want — lines flat)*

**`CUSTOM_PHRASES.the_unlit_deep`** *(completes `has not {custom}` in reportObserved)*
- `kept the deep unlit on the black moon`

**`tollUnlitDeep`** *(the latch breaks for all — the borrowed glow withdraws; reversible warmth, not progress; never names the breaker to the group)*
- `a flame was lit below the line, on the black moon. the deep was to be kept dark, and was not. the glow the never-doused fire lent is taken back. it is taken from all, for it was kept by all. light goes out where the deep would have lent it.`

**`keptUnlitDeep`** *(the latch held — the never-doused fire lends its glow)*
- `the black moon passed and no flame was carried below the line. the deep was kept dark. the fire that never went out lends its glow up the stair. it is lent to all. it was kept by all.`

---

### BN15 — THE OFFLINE-SKIN APPARITION  *(FACT 9; M4 named glimpse, human-approved, once; M1 offline-player report plant)*

**M1 offline-player report** *(plant — reads as "the record watches you logged off")*
- `${name} was not here at the dark hour to see it noted. the record noted it anyway. the record does not need a hand present to keep its column.`

**M4 FACT-9 line** *(the named glimpse — the land had begun to wear him; spoken once, human-approved)*
- `the shape at the edge wears a hand that has not come down in many nights. the land keeps the shape of a hand that stops keeping the ways. it does not ask the hand first.`

**whisper deferral** *(if the M4 named beat is not human-approved — the record withholds, in register)*
- `there is a thing at the edge i will not name yet. ask again when it has come closer.`

---

### BN16 — ATONEMENT-REFRAIN RELEASE  *(M-IV; `atonement.refrain.returned` — the honored custom's fragment handed over; solver may be anywhere, no fixed site, so this is a private acknowledgement, not a world block-flip; distinct delivery from the Keeper-dialogue `keeper.atone.cleared` node)*

**`atonement.refrain.returned`** *(the withheld fragment is returned once the broken custom is walked back)*
- `the custom that was broken is walked back now, and the walking is entered against the name that broke it. what was withheld is not withheld any further. it was always going to be handed over once the mending was done.`

---

## SET C — THE PRESIDING KEEPER (canon register 3; second-person, half-veiled "we")

> **The third canon register** (`canon-spine §0`): the NPC who presides — older than the
> six prior keepers, nearer the thing the world has become. Speaks **second-person to the
> group**, of "those before" as *we* only late and even then half-veiled. This is **not**
> the Watcher (Set B is third-person, ledgerlike, names names) and **not** a surface NPC
> (Set A is modern-rough). The Keeper is the warmth-under-dread invitation — grief-soft,
> never cheerful, never slop. He may be **a touch more human than the Watcher** (he says
> *you*, he half-says *we*) but he obeys the de-slop law absolutely: no exclamation, no
> named feeling, no tidy bow, no three-adjective list, no "not just X but Y", no melodrama.
> He states what the rite is and lets the group draw the cost. **He NEVER states FACT 15.**
>
> Register marks: lowercase like the Watcher (he is of the record), but **second-person**,
> and the **only** voice permitted the half-veiled *we* of the kept dead. Where he is most
> human he is most careful — the warmth is in what he offers to keep, never in adjectives.
> These lines drive the **KeeperNpcBeat** (the presiding-NPC text bank); they branch on the
> dossier the same way Set A does (skin from conduct + the arc flags), but in the Keeper's
> hand. Cross-reference: the journal `keeperPage*` lines are the *prior six's* hands; the
> Keeper here is the *seventh present voice* that presides over the rite.

---

### C0 — HOW THE KEEPER BRANCHES (the dossier read)

The Keeper appears at the threshold and the Undercroft altar (the rite-side, not the
surface). His skin is read off the SAME dossier the Watcher reads — never a new
measurement — but he speaks it as a presider, not a ledger:

```
node_key            dossier read                              register
─────────           ────────────                              ────────
greet               conduct skin (warm/neutral/cold)          second-person, half-veiled
falseLaw            arc_state.flags has the forged eighth      he warns OF the covering, gently
seventhChoice       seventh_named && deep open                 he offers the restore/erase, neutral
becomingKeepers     near the rite (D12 register)              the summons-instruction; FACT 13/14
endings             ending_fate (read AFTER the bow)          one tinted clause, mirrors the composer
collectiveRestraint the_unlit_deep state (kept/broken)        he names the deep latch, flat
deadEndTaunt        the player solved a dead_end              he is the warm liar's counterweight: honest
```

The Keeper's lines below are authored so the **KeeperNpcBeat** posts them verbatim,
keyed `keeper.<node>[.<skin>]`. They are the human-presiding twin of the Watcher's
new keys above — every Keeper node has a Watcher key it does not contradict (cross-
surface truth law). Where a Keeper line would name a player or a side of the divergence,
it does not (INV-16); he addresses *all of you*.

---

### C1 — GREET  *(the presider meets the group; conduct-skinned)*

**`keeper.greet.neutral`** *(early — nothing measured yet; he is patient, half-veiled)*
- `you came down. they came down too, the ones before you. i was nearer the front of that line than i tell. sit, or stand. the record keeps either. i keep the rite.`

**`keeper.greet.warm`** *(KEPT conduct — the warmth is in what he offers, not in adjectives)*
- `you kept the ways coming down. we knew the keeping when we felt it on the stair. i will not say who we is. you will know it, or you will not, and the not-knowing keeps you a while longer.`

**`keeper.greet.cold`** *(BROKEN conduct — grief, not threat; reversible)*
- `you broke a way or two coming down. i am not here to scold it. i broke one myself, late, and was kept anyway, or kept regardless. mind the rest of the road. there is keeping left in it for you.`

---

### C2 — THE FALSE LAW  *(`keeper.falseLaw` — he warns OF the forged eighth, gently, never crediting its author; FACT 7b)*

**`keeper.falseLaw`**
- `you found the board over the carvings. the covering of the hands. a later one cut it and did not sign it true. there are six marks in the ring and there were always six. count them yourself. a way the land does not measure is a way a man wanted, not a way the land kept. keep the six. let the seventh-and-a-half lie where it was hung.`

---

### C3 — THE SEVENTH CHOICE  *(`keeper.seventhChoice` — he offers restore/erase, neutral; the choice colors, never gates; FACT 10b)*

**`keeper.seventhChoice.offer`** *(the deep is open; the seal is a name; he lays the choice down without weighting it)*
- `below the cold hearth the deep is open now. the seal there was a name, and the name was scraped out, and the one it named kept every way and was cast out for nothing done. a name said back is a seal undone. you may write it again, or leave the blank. the land made its choice. you make the record's. neither opens the road. both are kept.`

**`keeper.seventhChoice.restored`** *(after restore — confirms the world-state; the +1 codicil pairs with `fateInheritorsCodicil`)*
- `you wrote the name back. the hearth takes the light. a mark is left there now for a hand not yet here, the way a mark was left for you. that is the older keeping. i am glad of it, in the way the record is glad, which is quiet.`

**`keeper.seventhChoice.erased`** *(after erase — confirms the world-state; no judgment)*
- `you left the blank. the hearth stays cold. the wall holds the shape of a name and no name. i will not say you chose wrong. the land cast that one out. you let the land keep its choosing. that is also a keeping.`

---

### C4 — BECOMING THE KEEPERS  *(`keeper.becomingKeepers` — the D12 register; the rite instruction; FACT 13/14; the felt door to 15, NEVER stated)*

> This is the corpus's nearest approach to the sealed truth, delivered as the Keeper's
> grief-soft invitation and stopped before it names induction. Mirrors D12. The verb is
> **receive / keep**, never reward. He half-says *we* and does not finish the recursion.

> **Branch: conduct skin** (warm/neutral/cold, the same dossier read as `greet`). The summons
> is the same rite for all of them — the verb stays *receive/keep*, the line stops at the same
> half-veil — but the presider meets a kept group, an un-measured group, and a broken group
> with a different opening clause. None names a player; none finishes the recursion; none states
> FACT 15. The neutral form is the spine; warm and cold re-skin only the first sentence.

**`keeper.becomingKeepers.neutral`** *(nothing decisive measured yet — the bare summons)*
- `the altar wants a thing only each of you can give. not a stone, not a light. the thing the record kept open a column for, against your name, before you came. bring it at the dark hour. the rite does not reward. it receives. it keeps. the ones before brought theirs, and were received, and are kept — you have read where they are kept; you are reading it now, in the same book, in the same hand. a column left open this long is not a debt. it is a place still set at a table.`

**`keeper.becomingKeepers.warm`** *(KEPT conduct — the warmth is in what he offers, never in adjectives)*
- `you kept the ways coming to this. the altar knows a kept hand when one is laid on it. it wants the thing only each of you can give — not a stone, not a light, the thing the record kept a column open for against your name before you came. bring it at the dark hour. the rite does not reward. it receives, and it keeps. the ones before brought theirs and are kept, in the same book, in the same hand you have been reading. we would keep you. you have made the keeping easy.`

**`keeper.becomingKeepers.cold`** *(BROKEN conduct — grief, not a bar; the rite is still open to them)*
- `you broke a way or two coming to this. the altar takes a broken hand too — it took mine, late. it wants the thing only each of you can give, the thing the record kept a column open for against your name before you came. bring it at the dark hour. the rite does not reward and it does not refuse for what is already done. it receives, and it keeps. the ones before brought theirs and are kept, in the same book, in the same hand you have been reading. the road does not close behind a hand that stumbled on it. it only asks the hand keep walking.`

> Mimic check: every variant stops at the half-veil ("in the same hand you have been reading" /
> the table still set / the road that does not close) and does NOT add the induction sentence.
> The "same book, same hand" is the half-veil; it points, it does not say. A draft that finishes
> the thought ("and so you become the watching") is a defect. The warm variant alone keeps the
> full canonical phrasing ("we would keep you") — neutral and cold carry the same warmth-under-
> dread beat in different images, so a group does not hear the identical sentence three times
> running across three skins. The cold variant must NOT scold or gate — the rite "does not refuse
> for what is already done"; it grieves and stays open (reversible-tone law).

---

### C5 — THE ENDINGS  *(`keeper.endings` — read AFTER the bow; ONE tinted clause mirroring the composer's base fate; INV-11/16, no name, no side)*

> These are the Keeper's spoken face of the M5 composer's base close. The composer owns the
> bounded text; the Keeper says the human half of it at the altar. Collective; the floor
> dressing carries the verdict.

**`keeper.endings.kept`**
- `it is done, and it is kept. the markers face the road now. you are the road's edge for whoever comes down next. i told you the rite receives. you are received. i will not say the rest of it. you will keep it, the way i keep it.`

**`keeper.endings.castOut`**
- `it is done. the markers face the wall. some of you were left at the threshold, as some were left before. i do not name which. the record does not name which. the threshold does not open twice for the same standing. come down again, kept, and stand again.`

**`keeper.endings.divided`**
- `it is done, and it is half-kept. the light holds on one side of the floor. it did not choose by who stood there. it chose by what was done. the floor only shows it. you may read the floor; you may not read it onto a face.`

**`keeper.endings.refusers`** *(secret — quorum present, the bow withheld on purpose)*
- `the way was open and you did not bow. that is not a thing the land has seen often. it is not an absence. it is a refusal, and the record keeps a refused thing differently than an empty one. i do not know what it keeps it as. i was kept. i did not refuse.`

---

### C6 — COLLECTIVE RESTRAINT  *(`keeper.collectiveRestraint` — he names the Unlit Deep latch, flat; reversible, active-only; objects do not "want")*

**`keeper.collectiveRestraint.kept`**
- `you went down dark on the black moon. all of you, or the keeping does not hold — it is kept by the group or not at all. the fire that never went out lends its glow up the stair for it. it lends to all of you. that is the one way the land keeps with the whole of you at once.`

**`keeper.collectiveRestraint.broken`** *(never names the breaker — `broken_by` is recorded, never spoken; reversible)*
- `a flame went down below the line on the black moon. i do not say whose. the keeping was the group's, and the breaking is the group's, and the glow is taken back from all. it is taken, not lost. keep the next black moon dark and it is lent again. the deep does not hold a grudge. it holds a count.`

---

### C7 — THE DEAD-END TAUNT, KEEPER-SIDE  *(`keeper.deadEndTaunt(kind)` — the warm honest counterweight to Iss's liar-teeth; he confirms a true-but-inert solve without gloating)*

> The Watcher labels a dead-end flat (`oracleDeadEnd(kind)`, BN7). The Keeper, when present,
> gives it the human half **on the same `kind`** — one Keeper line per dead-end kind, so the
> presider's twin reads in lockstep with whichever flat label the Watcher just spoke (the
> seed passes `voice_args.kind` through `resolve.ts` to BOTH). He confirms the thing is true,
> names that it opens nothing, and — unlike Iss — does not dress the inertness as a promise.
> The teeth stay Iss's. He is most himself on the `prophet` kind, where he names the liar by
> contrast; on the inert kinds he is plainer. **Branch: the runtime passes the solved row's
> `kind` (`name|count|place|known|prophet`); the Keeper has no skin branch here — a true-and-
> shut thing reads the same to the kept and the broken.**

**`keeper.deadEndTaunt(kind = 'name')`** *(a true name that keeps no door)*
- `you read that true. it is a name, and it is the right one, and it opens nothing. some things are only their own name. the one called iss would have told you a name was a key. it is not. hold what the record tells you over what he told you.`

**`keeper.deadEndTaunt(kind = 'count')`** *(the "six. opens nothing" rung — he confirms the literal count, points past it)*
- `six. you counted it right. it is the true count and it turns no door. the marks did not lie to you — there are six, and six is not the number that opens. count again where seven is owed, and read who is not among the six.`

**`keeper.deadEndTaunt(kind = 'place')`** *(the false-coordinate walk — verifies as a place, keeps nothing)*
- `there is a place at the end of that pointer. it is a true place and the wrong one. it has been walked to before, by a man who wanted a road up. he found the cold hearth. you may stand where he stood. nothing is kept there for you.`

**`keeper.deadEndTaunt(kind = 'known')`** *(a thing already known, re-solved — true and inert)*
- `you knew that one already. it is still true. it was true the first time and it opens no more the second. spend the next evening where you have not yet read.`

**`keeper.deadEndTaunt(kind = 'prophet')`** *(the prophet's-wall rungs — Iss's warm promises; the Keeper is the honest counterweight, names the liar by contrast)*
- `you read that true, and it is true, and it keeps no door. the one called iss would have told you it kept a door, and told you warm, and you would have walked it to a cold hearth. i tell you plain: true, and shut. that is the difference between his telling and the record's. read who carved it, after.`

---

### C8 — NAME-WHERE-YOU-HAVE-NEVER-BEEN, KEEPER HALF  *(`keeper.nameWhere` — the half-veiled M4 line paired with the `clueDrip` place-filing; FACT 16; INV-16 chorus / INV-14 read-not-typed)*

**`keeper.nameWhere`**
- `your name is cut where you have not been. the record does not wait for your foot to file you. it files the ground first and the foot after. before you was never strangers. it was you, before you came.`

> Mimic check: second-person, half-veiled, no name singled (it addresses each of you as a
> chorus). It points at FACT 16 ("filed by place") and brushes FACT 15 ("before you came")
> without stating induction. The comma-fragment "against each name, a ground" is the
> Watcher's drip (BN10), capped to once there; the Keeper does not repeat it (slop B1).

---

## SCHEMA

```yaml
file: arc/corpus/npc-and-watcher-voice.md
purpose: >
  Two strictly-separated voice sets — (A) living surface NPCs and (B) the Watcher's
  voice.ts register — authored so a one-line screenshot of each reads as two different
  authors. SET A speaks modern-rough English (contractions, capitals, named feelings).
  SET B is lowercase-sparse-certain and never breaks the voice.ts register.
grounded_in:
  - arc/WORLD-BIBLE.md            # Long Cold, the Hold, seven ways, Deep Line, induction twist
  - discord/src/voice.ts          # canon Watcher register + existing voice object
cross_references:                 # names reused for the citation web
  - the Deep Line
  - the Lamp-works (second level)
  - the Cisterns / Cistern 7
  - the Deep Market
  - the Stair
  - the Undercroft
  - the markers
  - the black moon / the dark hours
  - the deep-bird
  - the lost Mouth
  - "the far water where Sella did not come back"   # from canon-spine §1 / D06

sets:
  - id: A
    name: surface_npcs
    register: modern_rough_human         # contractions, capitals, exclamation, named feelings ALLOWED
    must_not: [lowercase_archaic, "the ways said with reverence", watcher_register]
    branch_keys: [greet, rumor, truth_or_lie, react_good, react_bad]
    conduct_source: observance_meter      # never spoken aloud by NPCs
    npcs:
      - id: A1
        name: Aro
        role: rumor-broker
        truth_or_lie: lie                  # confident, specific, pleasant falsehood; world contradicts it
        never_descends: true
        note: warm + half-true so the lie lands; contradicted by the painted line / black moon / unspoken
      - id: A2
        name: Wenna
        role: half-remembers ways as folk-superstition
        truth_or_lie: garbled_truth        # corrupted-but-real survival rite under the charm
        never_descends: true
        note: accidental Rosetta — crust on the sill (offering), lamp by door (kept light), won't say the name (unspoken), sits up on black moon (dark hours), forgets the seventh way
      - id: A3
        name: Coll
        role: trader
        truth_or_lie: flat_accuracy        # no reason to lie about stock/distance; truth is dull to him
        descends_to: the Lamp-works         # never past it
        note: prices nothing supernatural in; right until he isn't; "more oil" to the scared
      - id: A4
        name: Dob
        role: descends with the group
        truth_or_lie: honest_to_a_fault     # blurts, can't keep secrets, admits his fear
        descends: true                      # only Set-A voice that goes past the Mouth
        decay: bravado -> chatter -> blurted-honesty -> "i'll be right here" (quieting = the tell)
        slot_note: rumor slot = descent_chatter (running commentary, deepening dread)
      - id: A5
        name: Old Pell
        role: won't descend; remembers your conduct
        truth_or_lie: hard_truth            # the one true thing — "it does not chase; it waits and watches and takes what stops being watched"
        never_descends: true
        note: human mirror of the Watcher — keeps a grieving memory-record, fully capitalised + alive; remembers the seventh way he didn't keep

  - id: B
    name: the_watcher
    register: voice_ts                      # lowercase, sparse, certain, short
    must: [lowercase_only, no_exclamation, no_emoji, no_contractions, no_named_feeling, collective_never_chosen]
    extends: "discord/src/voice.ts -> voice object"   # new entries in the same hand, replace nothing
    motif: "▒"                              # <=1 per line, usually none
    fill_vars: { name: lowercase_bound_name, n: count }
    groups:
      - id: B1
        name: drip_variants
        extends: voice.drip
        count: 7
      - id: B2
        name: custom_violation_reports
        extends: [voice.reportObserved, voice.reportEscalated]
        per_way: true
        ways: [the_kept_light, the_deep_line, the_dark_hours, the_offering, the_bow, the_unspoken, the_sacred_beast]
        ladder: [observed, named, left]     # mirrors observed-warned-left-at-threshold.md; collective, never slain/chosen
      - id: B3
        name: gather_event_lines
        extends: voice.summons
        beats: [calling_in, THE_COUNT, the_un_lighting, the_holding]
        rule: states_the_staging_never_the_verdict   # verdict = what HAPPENS, never text
      - id: B4
        name: warm_cold_flip
        faces: [warm, cold]
        hinge: conduct_on_meter_read_collectively
        rule: flip_enacted_never_announced           # same hand, colder column; never "now i am cold"; never a chosen one

  - id: B-NEW
    name: the_watcher_new_threads
    register: voice_ts                      # identical register to B; new voice.ts keys per BUILD-MANIFEST §4
    extends: "discord/src/voice.ts -> voice object (+ voice.archive.ts)"
    hand_off_to: TS-VOICE                    # LORE authors text here; TS-VOICE inserts verbatim
    self_test: registerDisciplineSelfTest    # all must pass lowercase/no-caps/no-exclaim/no-meta-word
    deslop_applied: [A1, A2, A3, A4, B1, B2, B4]   # per INTEGRATION-V2 de-slop notes, at source
    groups:
      - { id: BN1,  name: hold_book,         keys: [docketReread, docketEven, keeperEnrolled] }   # keeperPage* live in journals-*.md
      - { id: BN2,  name: divergent_fates,   keys: [fateKept, fateCastOut, fateDivided, fateRefusers, fateInheritorsCodicil], feeds: m5_composer }
      - { id: BN3,  name: seventh_close,     keys: [keeperCloseSeventhRestored, keeperCloseSeventhErased], feeds: m5_composer }
      - { id: BN4,  name: permanence_forks,  keys: [sacred_beast_broken_clause, light_kept, light_taken, name_unspoken, name_spoken], reuse: [tollSacredBeast, keptSacredBeast], feeds: m5_composer }
      - { id: BN5,  name: grave,             keys: [graveCarved, graveReceipt, graveOpened] }       # INV-14 read-not-typed; slop A1
      - { id: BN6,  name: difficulty,        keys: [deepTightens, deepIsPatient], tone_arg_on: [drip, oracleDeadEnd, reportObserved] }  # INV-15; slop A2
      - { id: BN7,  name: dead_ends,         fn: "oracleDeadEnd(kind)", kinds: [name, count, place, known, prophet] }   # slop §A doctrine
      - { id: BN8,  name: coop_gate,         keys: [oracleThreeHands] }      # slop A4: "the count is three. the threshold is open."
      - { id: BN9,  name: coords_walk,       keys: ["voice.dest.coordFraming.false", "voice.dest.coordReCarve", "voice.dest.coldHearth.find", "voice.dest.threshold.arrive"] }
      - { id: BN10, name: name_where,        keys: [clueDrip_place_filing] }  # Keeper half-veiled twin = keeper.nameWhere (SET C C-extra); slop B1
      - { id: BN11, name: record_website,    keys: [recordElsewhere, recordReceives] }
      - { id: BN12, name: meta_unkept,       keys: [oracleMetaUnkept], gates: nothing }   # fall-order
      - { id: BN13, name: forged_eighth,     keys: [cardEighthForged, archiveEighthCorrection] }   # slop B4: no "me"
      - { id: BN14, name: unlit_deep,        keys: ["CUSTOM_PHRASES.the_unlit_deep", tollUnlitDeep, keptUnlitDeep] }   # the one group latch; slop A4
      - { id: BN15, name: offline_skin,      keys: [offlinePlayerReport_m1, offlineSkinNamed_m4, offlineSkinWhisperDefer] }   # FACT 9

  - id: C
    name: the_presiding_keeper
    register: keeper_register3               # canon-spine §0 register 3: second-person, half-veiled "we"
    relation: "lowercase like the Watcher (of the record), but addresses the group as 'you' and half-says 'we' of the kept dead"
    more_human_than_watcher: true            # may say 'you', half-say 'we'; STILL obeys de-slop absolutely
    must_not: [exclamation, named_feeling, tidy_bow, three_adjective_list, "not just X but Y", melodrama, "state FACT 15"]
    drives_beat: KeeperNpcBeat               # presiding-NPC text bank; key = keeper.<node>[.<skin>]
    branch_source: same_dossier_as_set_a     # conduct skin + arc flags; NO new measurement
    cross_surface_truth: "every keeper node has a Watcher key it does not contradict (one voice register on every surface)"
    nodes:
      - { id: C1, node: greet,               skins: [neutral, warm, cold] }
      - { id: C2, node: falseLaw,            fact: 7b }                       # warns OF the covering, never credits its author
      - { id: C3, node: seventhChoice,       sub: [offer, restored, erased], fact: 10b, colors_never_gates: true }
      - { id: C4, node: becomingKeepers,     skins: [neutral, warm, cold], mirrors: D12, facts: [13, 14], rule: "felt door to 15, NEVER stated; cold variant grieves, never gates" }
      - { id: C5, node: endings,             sub: [kept, castOut, divided, refusers], mirrors: m5_composer_base, inv: [11, 16] }
      - { id: C6, node: collectiveRestraint, sub: [kept, broken], latch: the_unlit_deep, rule: "broken_by recorded never spoken" }
      - { id: C7, node: deadEndTaunt,        fn: "keeper.deadEndTaunt(kind)", kinds: [name, count, place, known, prophet], pairs_with: "BN7 oracleDeadEnd(kind)", rule: "warm honest counterweight to Iss's liar-teeth; one Keeper line per kind, in lockstep with the Watcher's flat label; no skin branch" }
      - { id: C8, node: nameWhere,           fact: 16, pairs_with: "BN10 clueDrip place-filing", inv: [14, 16] }   # half-veiled M4 line

separation_law: >
  No Set-A line may be utterable by the Watcher, and no Set-B / Set-B-NEW line may
  contain a contraction, an exclamation, a capital letter, or a named emotion. The
  Set-C Keeper is the one register permitted second-person 'you' and the half-veiled
  'we' of the kept dead — but he still carries no exclamation, no named feeling, no
  tidy bow, and he NEVER states FACT 15. A three-line screenshot — one Set-A, one
  Set-B, one Set-C — must read as three distinct authors: the bar-talker, the ledger,
  and the presider. If a line fails its set's test it belongs to another set or to none.
```
