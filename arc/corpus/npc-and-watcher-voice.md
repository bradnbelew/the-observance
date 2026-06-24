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

separation_law: >
  No Set-A line may be utterable by the Watcher, and no Set-B line may contain a
  contraction, an exclamation, a capital letter, or a named emotion. If a line fails
  this test it belongs to the other set or to neither.
```
