# The Observance — ARG Design-Craft Research

**Purpose.** A cited, transferable design-craft report to drive a ground-up redesign of *The Observance*
from a **puzzle-gauntlet** ("solve a cipher on a stone, find five more, win") into an immersive,
slow-burn, **lore-driven world a friend group is dropped into and must reconstruct**: who were these
people, what was their place, what happened, what is on the surface with us — were they even *human*
before? It is also filmed as a Wifies-style YouTube mystery video.

Every technique below is drawn from a real, cited work and tagged with a concrete **server-side
Minecraft realization** in our toolset (block/structure placement via FAWE schematics, item staging,
written books, sign text, entity naming, NPC dialogue trees, per-player effects, custom mobs via
MythicMobs + ModelEngine, resource-pack textures/fonts/sounds/fog, plus a Discord companion layer).

---

## 1. The Thesis — world-first, fragments-first; puzzles are the *texture*, never the skeleton

**The single biggest mindset shift:** *the world is the document.* Players do not "solve to progress."
They **accrete a picture** by reading objects, reading space, and clustering what they *hear* against
what they *verify*. Deep history is delivered through **distributed, cross-referencing fragments the
player reassembles** — never through exposition, and never gated behind a lock that yields only the
next lock.

Three master sources converge on this one principle:

- **FromSoftware** proves *the item IS the lore* and *geography implies history*: a description, a
  corpse's position, an area sunk beneath a newer one, or a decay state each encode a **verifiable
  claim** about who lived here and what happened — with no exposition.
  ([cbr.com](https://www.cbr.com/elden-ring-environmental-storytelling-fromsoftware/),
  [lokeysouls.com](https://lokeysouls.com/2020/11/16/environmental-storytelling/))
- **Outer Wilds' ship log** gives the single most portable machine for a "reconstruct the lost world"
  ARG: a **two-tier knowledge model** (*rumor* facts you hear vs *explore* facts you verify on-site),
  **place-anchored entry cards**, dotted **rumor lines** pointing from a rumor to an unvisited place,
  and color-coded **curiosities** that auto-cluster scattered fragments into distinct mystery threads.
  ([nh.outerwildsmods.com](https://nh.outerwildsmods.com/guides/ship-log/))
- The **ARG canon** (TINAG / Elan Lee's three rules; I Love Bees; Year Zero; the Portal ARG) proves
  that *story is the skeleton and puzzles/decoding are only texture* — and that the genre's whole
  philosophy is to make players feel they **found a real situation** rather than were handed a quest.
  ([argn.com](https://www.argn.com/2005/04/this_is_not_a_game/),
  [gamedeveloper.com](https://www.gamedeveloper.com/business/-i-frog-fractions-2-i-arg-co-creator-reflects-on-what-makes-a-good-alternate-reality-game))

That ship-log model maps almost 1:1 onto a **Discord companion "journal"** that turns The Observance's
found papers, builds, and NPC overhears into a self-assembling, **per-player rumor map** — the
"hear it, then go verify it, watch the threads form" loop you want *instead of* a puzzle gauntlet.

> **Reframe rule of thumb:** every time you are tempted to design "a puzzle that unlocks X," instead
> design "a fragment that, combined with other fragments, *reveals* X." The decode is how you pry the
> page loose; the **human revelation underneath is the actual reward.**

---

## 2. Techniques (cited, with server-side Minecraft realization)

### Theme A — Environmental & item lore (the world is the document)

**A1. The item IS the lore — never the lorebook.**
Put history into the **descriptions of ordinary held objects**, in short oblique fragments that imply
far more than they state and reference *other* items/places, so meaning only emerges when several are
collected. Text is terse and easy to miss; it rewards attention, not completion.
*From:* Dark Souls / Elden Ring / Bloodborne item descriptions (FromSoftware).
*Source:* https://lokeysouls.com/2020/11/16/environmental-storytelling/
**MC:** Written books and renamed items (anvil/MythicMobs lore tags) are the primary lore carriers,
*not* a wiki codex. Each book is 1–3 short paragraphs in an in-world voice (a clerk's ledger line, a
soldier's last note), oblique and incomplete. Cross-reference deliberately: a book in the flooded
quarter names "the Warden's seal," described in a different book across the map; a named item
("Warden's Ration Tag") is the physical proof. Truth only resolves when the group holds 3+ fragments
together. Bury one decisive line mid-paragraph so it's missable.

**A2. Geography implies history — vertical/spatial build relationships encode a timeline.**
An area sunk *below* a newer one reads as "this came first and was buried/abandoned"; distinct
architectural iconography lets players attribute a building to a people and infer migration, conquest,
or collapse from how styles overlap.
*From:* FromSoftware level design (sunken areas; per-area iconography).
*Source:* https://lokeysouls.com/2020/11/16/environmental-storytelling/
**MC:** Use **FAWE schematics to layer the world's history physically**: build the "old human city"
first, then bury part of it under a newer, alien/overgrown layer the players walk in on — descend and
the architecture changes era and culture (different blocks, motifs, lighting). Give each lost faction a
signature block palette + banner motif (resource-pack textures) so players learn to read "this wall was
made by THEM, that one by the later occupants." The build relationships answer *"were they human
before us?"* without a line of text — the bottom layer is human-scaled, the top is wrong-scaled.

**A3. Placement is evidence — *where* a corpse/cache/trap sits is a claim.**
A guarded chest implies the owner valued/feared its contents; an item on a body implies ownership,
theft, or scavenging; the static "snapshot" of a scene freezes the moment of catastrophe.
*From:* FromSoftware corpse/item/Mimic placement; the "static snapshot" of an area.
*Source:* https://lokeysouls.com/2020/11/16/environmental-storytelling/
**MC:** **Stage scenes, not loot.** A skeleton clutching a renamed book at a barricaded door (the
barricade facing *inward*) says "they died holding the line from inside." A child-sized bed beside an
adult's hastily-packed chest implies a family fleeing. Use armor stands, frozen MythicMobs corpses,
item frames, and redstone-trap remnants so **position carries the story.** Freeze each tableau as a
deliberate "snapshot of the last day."

**A4. Decay state is a clock — ruin intensity orders events without dates.**
The level of overgrowth/destruction tells players *when* a scene happened relative to others, letting
them order events into a timeline.
*From:* FromSoftware areas as decay-state snapshots; Elden Ring "reading ruins."
*Source:* https://medium.com/@neallebenedict/reading-ruins-environmental-storytelling-in-elden-ring-fbddad0387fb
**MC:** Vary ruin intensity as a readable timeline: the temple district is lightly cracked (fell last),
the harbor is fully drowned/mossed (fell first). Use a **consistent decay vocabulary** (same
moss/crack/scorch language) across builds so players can *rank* locations by age. Per-zone resource-pack
fog/sky tints reinforce "this is the deep past."

**A5. Narrative stratigraphy + osmosis — the space teaches its own history before any text is read.**
Layer cultural/historical/emotional information physically into the space so players **absorb story just
by being there** ("narrative osmosis"), supporting nonlinear worldbuilding without gating.
*From:* Embedded-narrative / spatial-storytelling theory (Gone Home; narrative-stratigraphy).
*Source:* https://wardrome.com/environmental-storytelling-building-immersive-worlds-through-design/
**MC:** Compose every district so atmosphere alone teaches first: resource-pack ambient sounds (a
distant bell that never stops, choral hum near the temple), zone fog/sky color, lighting temperature,
and signature props. A player who reads *nothing* should still leave the harbor feeling "something
drowned here and didn't want to be found." Then books/NPCs **sharpen what the space already implied** —
text confirms a feeling rather than delivering information cold. Keeps it slow-burn, not a reading
assignment.

**A6. The four narrative architectures as a layering checklist.**
EVOCATIVE (prime with familiar archetypes) · ENACTED (let players physically perform the story) ·
EMBEDDED (scatter discoverable ephemera to reassemble) · EMERGENT (build systems that let unscripted
meaning arise). They are "tracks on a mixing board" — combine, don't pick one.
*From:* Henry-Jenkins-derived narrative-architecture framework (Laura E. Hall, Celia Pearce).
*Source:* https://www.noproscenium.com/evocative-enacted-embedded-emergent-narrative-architectures-for-immersive-storytelling/
**MC:** Run all four lanes per location. EVOCATIVE — a recognizable "drowned-civilization / cult-temple"
visual archetype primes the group instantly. ENACTED — make players *do* the ritual (place an item on
an altar, ring a bell, descend a sealed shaft) so they perform the lost people's actions. EMBEDDED —
the books/corpses/caches above. EMERGENT — per-player world reactions (the Watcher responding to
specific players) so each group's reconstruction is genuinely their own. Use it as a **checklist that
every location hits 2+ lanes.**

### Theme B — Found-document / epistolary & the "were-they-human / what-happened" dread

**B1. Clinical / dispassionate register — emotion leaks through facts, not adjectives.**
Write the surviving record like an official log: third person ("Observers recorded…"), neutral nouns
("the subject," "the entity"), hard objective observations ("induces a fear response"), precise dates.
The flatness *is* the horror — when feeling slips through, it lands hard.
*From:* SCP Foundation — How To Write An SCP / Guide to Clinical Tone.
*Source:* https://scp-wiki.wikidot.com/how-to-write-an-scp
**MC:** Write the "official" layer — surveyor's ledgers, ration logs, containment notices — as written
books authored by **ranks/roles, not names** (author: "WARDEN-3"; date stamped in the title). Lectern
sign text reads as posted notices in the same dry voice. NPC barks deliver status-report cadence, never
feelings. The contrast layer (personal journals, B5) holds warmth and panic, so the clinical books feel
colder by comparison. (This is also a primary engine for the *"were they human?"* question — clinical
language describing people as instances.)

**B2. Redaction & omission as the primary engine — censor the one thing they most want to know.**
Concealment forces speculation and the question *why is it hidden?* Rule: give enough context for an
intelligent guess, but leave enough unsaid that the real answer stays the reader's. Distinguish
[REDACTED] (someone is *hiding* this) from [DATA EXPUNGED]/torn-out (this was *destroyed*) — the choice
implies who survived and what they feared. Use sparingly. Iceberg theory: omit what you fully
understand; the reader senses the unstated mass and supplies the weight.
*From:* SCP redaction guidance; Hemingway's Iceberg theory.
*Sources:* https://scp-wiki.wikidot.com/how-to-write-an-scp ·
https://en.wikipedia.org/wiki/Iceberg_theory
**MC:** Resource-pack item textures of pages physically **blacked-out, water-stained, torn at the
exact load-bearing line, or burned past the key word**. A written book whose page 4 is missing entirely
(jumps 3→5). Signs with a strikethrough/redaction glyph (custom font) over the name of the place or
person. A lectern book ending mid-sentence. Stage the *answer* to a redacted question elsewhere as a
half-clue, so the blackout invites a guess but never confirms it.

**B3. Documents that reference each other — a citation web implies a world larger than what survives.**
Letters answer letters; a journal cites an incident report that's been expunged; a log refers to "the
third gate" the player hasn't found. The **gaps between cross-references** are where imagination builds
the lost city.
*From:* SCP found-document structure (addenda/cross-refs) + epistolary intercalated documents.
*Source:* https://scp-wiki.wikidot.com/how-to-write-an-scp
**MC:** Number/name things **consistently across the whole world**: a book in Build A names "Cistern 7"
and "Warden Aro"; a sign two regions away labels "Cistern 7 — sealed"; an NPC bark mentions Aro in past
tense. Let one book quote or reply to another ("Re: your note of the 14th — you are wrong about the
water"). Some referenced documents **never appear** (expunged), so the web has deliberate holes. Track
which fragments a player has read (scoreboard/tags) so later NPC dialogue can reference "what you
already know."

**B4. Contradictory accounts the player must reconcile — truth becomes personal; narrators unreliable.**
Two or three records of the same event disagree. The document stops *conveying* truth and starts
*destabilizing* it — the player assembles their own version and learns to distrust every narrator
(including the clinical one). This is also how you plant the "were they even human?" seed: accounts of
the same people drift from each other and from humanity.
*From:* What Remains of Edith Finch (conflicting family accounts); epistolary unreliable-testimony.
*Source:* https://www.rpgfan.com/feature/narrative-design-analysis-what-remains-of-edith-finch/
**MC:** Stage two books on the same event in different buildings with different authors: the warden's
report says the flood was an accident; a child's diary says "they opened it on purpose." A third source
(an NPC bark, or a build detail — chains on the *inside* of a door) silently favors one. **Never
adjudicate in-world.** Let the Discord layer be where players post both scans and argue — the
disagreement *is* the gameplay.

**B5. Dated entries that decay in tone — "human once, then something happened," shown not told.**
Date everything for real chronology, then let the **authorial voice deteriorate** across the timeline:
early calm and domestic; middle strained, with corrections and denials; late clipped, mis-spelled,
addressed to no one, or in a register no longer quite human. The reader infers the catastrophe from the
*gradient of the prose* and the silent gaps between dates.
*From:* SCP dated logs (degradation over time) + analog-horror slow-burn.
*Source:* https://en.wikipedia.org/wiki/Analog_horror
**MC:** A single author's books, staged in date order along a route, that change: legible font →
shakier custom font → a "wrong" glyph set (resource pack) for the final ones. Tone shifts from "planted
the east field today" → "the water remembers names" → single repeated words. Gate the late, worst books
behind progress (only illuminate/spawn them once the early ones are read, via tags) so everyone walks
the decline in order. Leave a **dated gap** (no entries for "winter") that the builds fill in.

**B6. The discovered-manuscript frame — every fragment carries *how* it was found.**
Classic epistolary horror always states provenance ("recovered from a missing-persons file," "audio
pulled from a dead man's PC"). That voyeuristic "this is real, firsthand, surviving" framing sets the
chill — and lets a present-day archivist narrator's *own* notes start to decay, so the investigation
becomes infected.
*From:* Dead Letters / epistolary-horror submission guidance.
*Source:* https://horrortree.com/taking-submissions-dead-letters-episodes-of-epistolary-horror-early/
**MC:** Stage fragments inside their recovery context: a book sealed in a barrel at the bottom of a
flooded room; a page pinned under a skeleton's hand; a chest labeled "RECOVERED — do not open" (item-
frame label). Frame the **Discord layer as the present-day recovery effort** — players are
"cataloguers" posting scans of what they pulled out, and a present-day "archivist" voice (bot/NPC)
annotates finds. Over the run, that archivist's annotations themselves get shorter, then *wrong*,
mirroring the in-world decay.

**B7. Rooms & staged objects as physical autobiographies / time-capsule memorials.**
A preserved space is a character's autobiography in objects: curate items that *complicate* one reading
rather than spell it out. Seal the space at the moment of death so nothing was updated after — finality
is felt. Period-accurate artifacts place the person in time better than any written date.
*From:* What Remains of Edith Finch / Gone Home (sealed memorial rooms; materialized narration).
*Source:* https://www.rpgfan.com/feature/narrative-design-analysis-what-remains-of-edith-finch/
**MC:** FAWE schematics for sealed dwellings frozen at the end: a half-eaten meal (item frames), a
slept-in bed, a child's toys beside an adult's tools, a door barred from the *inside*. Curate
contradictory props in one room (offerings to a god + a hidden escape tunnel) so the inhabitant is
ambiguous. Put the relevant book/sign physically *in* the room it explains. A custom mob (MythicMobs +
ModelEngine) can be the room's former occupant — not attacking, just present/echoing — so the memorial
is literally inhabited by *what they became*, seeding "were they human?"

### Theme C — Pacing, trailheads & collaborative discovery (ARG canon)

**C1. TINAG (This Is Not A Game) — never acknowledge it's a game; never define its boundaries.**
Build a complete, self-consistent world that behaves as real as our own, so every artifact reads as a
real document/place/person, not a quest object.
*From:* ARGN TINAG essay; Elan Lee's three rules (GDC 2002).
*Source:* https://www.argn.com/2005/04/this_is_not_a_game/
**MC:** Forbid all 4th-wall language server-side: no [Quest] tags, no objective markers, no "you found
a clue" toasts. Books/signs/entity names use in-world voice only. The showrunner/Discord persona never
breaks character to say "good job, next puzzle." Resource-pack reskins strip gamey HUD affordances so
the place feels *found,* not authored.

**C2. Trailhead / rabbit-hole — one understated anomaly the group stumbles onto, not an announcement.**
The entry point sets world and tone in miniature and provokes "wait, what *is* that?" Seed multiple
trailheads so someone bites.
*From:* I Love Bees (URL flashed in a Halo 2 trailer); Year Zero (highlighted t-shirt letters).
*Source:* https://intervirals.wordpress.com/2015/02/14/following-rabbit-consider-your-rabbit-hole-world-alternate-reality-games/
**MC:** Open with ONE quiet world-anomaly near spawn — a block out of place, a structure that shouldn't
exist, a distant unreachable light, an entity-named villager who says one cryptic line and won't
elaborate. Seed parallel trailheads (a half-burned book, coordinates scratched on a sign, a Discord
"glitch" post) so whichever friend explores first finds a thread. The showrunner nudges only if days
pass with no bite.

**C3. Deliberate under-specification to force collaborative group solving.**
Hand out raw, unlabeled fragments with no instructions, and require **more inputs than one person can
gather**, so the friend group must pool what each found.
*From:* I Love Bees (210 unlabeled GPS/time pairs; 2-of-7 cluster answers needed).
*Source:* https://en.wikipedia.org/wiki/I_Love_Bees
**MC:** Never give a fragment *and* its interpretation together. Scatter pieces so no single player
holds them all: A finds coordinates on a sign, B finds the matching time-of-day in a book across the
map, C overhears the NPC line explaining why it matters. Gate a reveal behind **N-of-M fragments** (need
2 of 7 staged witness books). Per-player effects/loot ensure friends literally see different pieces,
forcing them to compare notes in Discord.

**C4. Two-tier knowledge: HEAR-then-VERIFY (the core reconstruction loop).**
Model every piece of lore as a **RUMOR** (secondhand: heard, glimpsed from afar, told) or an **EXPLORE**
fact (firsthand: confirmed on site). Rumors create direction; arriving converts rumor to verified truth
— and verified truth can **contradict** the rumor, which is itself the revelation.
*From:* Outer Wilds ship log RumorFact vs ExploreFact.
*Source:* https://nh.outerwildsmods.com/guides/ship-log/
**MC:** Split the entire information layer into rumor vs confirmed. NPCs (Citizens/MythicMobs dialogue),
overheard fragments, and distant skyline silhouettes plant **rumors** ("they say the Warden sealed the
lower gate"). Only standing in the place + finding the staged evidence yields the **explore fact** ("the
lower gate was sealed from the *outside* — to keep something *in*"). The Discord journal logs both
states per player and visibly flips a rumor to "verified" (or "contradicted") when the group arrives —
the contradiction is the emotional beat.

**C5. Place-anchored cards with directional "rumor lines" — the map shows its own holes.**
Each fragment is a card pinned to a *specific* location; hearing a rumor draws a dotted line *from* your
knowledge *to* an unvisited place. No quest markers — **the gaps in the map ARE the call to adventure.**
*From:* Outer Wilds ship-log entries anchored to astro objects, with SourceID rumor lines.
*Source:* https://nh.outerwildsmods.com/guides/ship-log/
**MC:** In the Discord journal, every fragment is a card tied to an in-world location. When a player
finds a fragment that *references* another place, the bot draws a dotted link to a still-locked card
(silhouette + "?"), so the group literally sees "we've heard of the Cistern but never been." Direction
emerges from lore, not objective arrows.

**C6. Cluster fragments into colored THREADS (curiosities) — the skeleton that replaces the gauntlet.**
Group every fragment under one of a few named, color-coded mystery threads, so a chaotic pile of finds
self-organizes — and the player *sees* which threads are nearly complete and which barely begun.
*From:* Outer Wilds "curiosities" (color-coded entry clusters in rumor mode).
*Source:* https://nh.outerwildsmods.com/guides/ship-log/
**MC:** Define ~4–6 named threads up front: **WHO THEY WERE / WHAT THE PLACE WAS / WHAT HAPPENED /
WHAT'S ON THE SURFACE WITH US / WERE THEY HUMAN.** Tag every book, build, NPC line, and item to a thread
+ color. The Discord journal renders found fragments grouped and colored by thread, with a quiet
completeness meter each. **Progress = threads filling in**, and the group naturally chases the
half-finished one.

**C7. Conditional reveals — a fragment's meaning *updates* once related fragments are found.**
The same object shows added text after the player has other knowledge, so re-reading old finds yields
new understanding and rewards holding the whole picture.
*From:* Outer Wilds ExploreFact AltText gated by a Condition.
*Source:* https://nh.outerwildsmods.com/guides/ship-log/
**MC:** Stage "rereads": the Discord journal expands annotation on an *old* card once the connected
fragments are logged ("Now that you've found the Warden's tag, this ledger line means X"). Server-side,
swap a book's contents or unlock a follow-up NPC line via a per-team scoreboard flag (MythicMobs
conditions / function gating). Meaning compounds; nothing is consumed-and-done.

**C8. Story is the skeleton; reconstruct lore from fragments (Dark Souls model).**
Distribute meaning across many cryptic, cross-referencing discovery points; the group constructs a
coherent history from pieces. *If it is just puzzles with no story, it is a puzzle hunt, not an ARG.*
*From:* Frog Fractions 2 ARG co-creator ("make it more like Dark Souls"); Year Zero memory-dumps.
*Source:* https://www.gamedeveloper.com/business/-i-frog-fractions-2-i-arg-co-creator-reflects-on-what-makes-a-good-alternate-reality-game
**MC:** Author the full lost-world history first, then **shatter it**: split each fact across multiple
half-survived books that reference each other ("see the eastern cistern," "as Maren wrote before the
quiet"), builds that imply a function, item-staging that implies an event (tools dropped mid-task), and
overheard NPC lines. No single artifact states the truth; the group triangulates it.

**C9. Stage clues as physical-feeling objects with a second analysis layer.**
The thrill is the object feels planted by the *fiction,* not the game; hide secondary layers revealed
only by closer analysis.
*From:* Year Zero (USB drives in venue bathrooms; spectrogram-hidden phone number; acrostic t-shirt).
*Source:* https://en.wikipedia.org/wiki/Year_Zero_(album)
**MC:** Stage items where a real person would have hidden them, not in glowing chests: a key under a
bed, a journal in a chimney, a map behind a painting (item frame). Add second-layer reveals: a book
whose first letters of each line spell something (acrostic), a resource-pack sound that slowed down
contains a whispered place-name (spectrogram), a structure whose shadow at a specific in-game time
points to the next site. Discovery feels *excavated,* not awarded.

**C10. A live showrunner/puppetmaster drips content and monitors in real time.**
A motivated group moves faster than designers expect; the puppetmaster accelerates/slows release to
match actual pace, holds reserve content for racers, releases a nudge for the stuck.
*From:* I Love Bees (Jane McGonigal's round-the-clock monitoring + daily reports).
*Source:* https://en.wikipedia.org/wiki/I_Love_Bees
**MC:** Run a live showrunner + Discord layer as the puppetmaster's console: watch what the friends
actually found (server logs, Discord chatter), then drip the next fragment at the right moment — place
a new staged book overnight, advance an NPC dialogue tree, trigger a world-anomaly only when the group
is ready. Keep a daily private log of group state to tune pacing.

### Theme D — Emotional-beat texture

**D1. Puzzles are texture, with one rate-determining gate per beat — broadcast the win.**
Layer many quick, recognizable, low-stakes decode-this puzzles that reward exploration (each yielding
one sentence of lore, never blocking), and reserve **at most one genuinely hard step per beat** to pace
the whole group. When it cracks, stage a visible world-reaction.
*From:* Portal ARG (Morse/SSTV = quick; MD5 brute-force = the rate-determining step, then broadcast).
*Source:* https://www.gamedeveloper.com/design/alternate-reality-game-puzzle-design
**MC:** Most "puzzles" are flavor — a partly-legible book, a simple substitution sign, a banner that
spells something — solvable in minutes. Per multi-week beat place exactly **one** slow gate (a build
observed at a specific in-game time; a combination assembled from fragments held across players). When
solved, the showrunner stages a server-wide world-reaction (a sealed door opens, a structure changes)
so everyone feels the unlock.

**D2. Alternate emotional beats; give breathing room; trickle in batches.**
Vary texture (shock, grief, quiet, mundane, confusing) and drip content in batches so players discover
at their own pace. **Trickle-truthing** prevents burnout and leaves room for twists.
*From:* Community ARG design (Welcome Home batch updates).
*Source:* https://en.wikipedia.org/wiki/Alternate_reality_game
**MC:** Compose the multi-week arc as an **emotional rhythm, not a difficulty ramp**: a shock beat (a
horror reveal in a build) → a quiet mundane beat (a child's toy, a recipe book, an ordinary day) for
grief and breathing room → a confusing beat (contradictory accounts) before the next reveal. Release in
**overnight batches** (new staged items/books appear between sessions), not a firehose. Schedule
deliberate **fallow days** where nothing new appears — the silence becomes ominous texture.

**D3. Earn the tonal contrast — celebrate competence *before* the downturn.**
Pace the opening as a "celebration of competence" so the collapse lands harder; the relatable,
low-stakes protagonist beats the action hero ("a low-life accountant in an abandoned office is scarier
than Batman").
*From:* Wifies ARG; Minecraft Forum horror-map guide.
*Sources:* https://www.argn.com/2025/11/the_king_in_yellow_as_found_footage_minecraft_arg/ ·
https://www.minecraftforum.net/forums/minecraft-java-edition/creative-mode/367898-how-to-create-the-best-horror-map-possible-and
**MC:** Frame the friend group as ordinary salvagers/researchers, not chosen heroes — give them mundane
starting goals (catalog the ruins, restore a building, survive a winter) so their early competence is
real and earned. The lost *people* should also read as ordinary (a baker, a watchman, a child) via
their staged homes — making "what happened to them" land emotionally. Reconstruction of normal lives,
then the wrongness, is the whole engine.

**D4. Ground the mystery in character/emotion so it never reads as an intellectual gauntlet.**
Pure plot-puzzle reveals create a fan "caste system" and feel unearned; the strongest reveals
(Shawshank, Sixth Sense) tie the answer to an **emotional payoff.**
*From:* Mystery-box critique (emotion over intellect).
*Source:* https://www.justinkownacki.com/mystery-box-storytelling-plot-emotion/
**MC:** Give the vanished inhabitants **names, desires, and relationships** that recur across papers,
NPC dialogue, and builds — so a decoded fragment lands as "oh, *this* person did this to *that*
person," a gut-punch, not a trivia unlock. Write books as personal voices (diaries, last letters,
accusations), not neutral lore dumps. The decode is texture; the human revelation underneath is the
reward.

### Theme E — Failure modes to avoid (the structural spine)

**E1. Plan and SEAL the ending first — a mystery is not a story.**
Lost's fatal flaw: the creators never intended to solve every question they planted, so questions
accumulated faster than answers and the finale felt like betrayal. Author the resolution **before** you
place the first fragment; make every planted question a debt you've already decided how to pay.
*From:* Lost / JJ Abrams mystery-box critique.
*Source:* https://www.justinkownacki.com/mystery-box-storytelling-plot-emotion/
**MC:** Before staging anything, write the full truth (who, what their place was, what happened, whether
they were human) as a **sealed "answer bible"** kept out-of-world (Discord admin-only / the answer-
oracle module). Maintain a **fragment-to-revelation ledger**: each findable fragment is tagged with
which sealed answer it advances. *If a fragment doesn't pay down a planned debt, cut it.* The world is
authored backward from a known ending.

**E2. Trickle real answers on a fixed cadence — keep question-to-answer ratio solvable.**
The mystery-box collapse is mechanical: intrigue extends, answers delayed, dissatisfaction compounds.
Resolve threads steadily; partial confirmed truths keep the group oriented.
*From:* Lost / TV mystery genre failure pattern.
*Source:* https://www.justinkownacki.com/mystery-box-storytelling-plot-emotion/
**MC:** Gate lore release by **milestones, not a single climax.** Each act/week deliver at least one
undeniable reveal (a fully-legible recovered page, an NPC who finally states a plain fact, a build whose
meaning clicks). Track an internal "open vs answered questions" count and never let open outrun answered
by more than a small margin. Use the Discord layer to periodically *surface* a now-readable fragment so
the trickle is visible and paced.

**E3. Make every GATE also a REWARD/REVEAL.**
Bad gating blocks and frustrates; good gating pays out something meaningful. For a reconstruction story,
the payout of every solved fragment must be **lore** — a piece of the lost world snapping into place —
never merely a key to the next puzzle. *A gate that yields only another lock is the puzzle-gauntlet
tell.*
*From:* Gating best practice + Portal ARG multi-tier rewards.
*Source:* https://www.gamedeveloper.com/design/alternate-reality-game-puzzle-design
**MC:** Decoding a cipher-stone, opening a sealed vault (FAWE schematic), or assembling cross-
referencing papers **always** reveals a chunk of WHO/WHAT/WHAT-HAPPENED, not coordinates to the next
stone. The solved book reads as a person's last letter; the unlocked room is a tableau that answers a
standing question. The "key" is incidental; the reveal is the point.

**E4. A correct solution must be OBVIOUSLY correct — never ambiguous encodings.**
Anagrams and arbitrary/opinion ciphers create red herrings with no undeniable answer — players can't
tell if they solved it, so they can't move on, and chained uncertainty compounds. Use real-world,
researchable, self-verifying systems (only one solution fits).
*From:* ARG puzzle design (obvious correctness + real-world encodings). **(Verified this session.)**
*Source:* https://www.gamedeveloper.com/design/alternate-reality-game-puzzle-design
**MC:** When a fragment must be decoded, use a system a friend can recognize-or-Google and confirm:
Morse on note-block/redstone pulses, a real cipher with a discoverable key, a checksum/format that fits
exactly one answer. The solve should feel unambiguous ("that's clearly a word / a date / a name"). When
the group does the right thing, **the world answers instantly and unmistakably** (sound cue, fog lifts,
a locked area opens, an NPC line advances) — never make players guess whether they got it.

**E5. Be ruthless about accidental red herrings; define the canon surface.**
Players extract meaning from *any* numbers/strings; accidental false clues derail the fiction (and have
sent real ARG players into dangerous rabbit holes). Test every snippet; explicitly establish what counts
as in-game.
*From:* ARG design (guarding against unintended clues + game boundaries).
*Source:* https://www.gamedeveloper.com/design/alternate-reality-game-puzzle-design
**MC:** Audit every sign, book, coordinate, entity name, and texture for unintended patterns before
going live — a stray seed-looking number or real-sounding name will get over-investigated. Establish the
canon surface clearly (this server's builds + the official Discord channel only); anything outside is
not the game. Keep a "no accidental clue" checklist.

**E6. Build for multiple engagement levels so non-solvers still inhabit the world.**
Frustration/burnout come from gates with no guidance and no alternate participation; fix with hints,
feedback, and parallel ways to engage.
*From:* Portal ARG tiered participation (casual radio-hunters got achievements alongside decoders).
*Source:* https://www.gamedeveloper.com/design/alternate-reality-game-puzzle-design
**MC:** Ensure the lazy explorer, the lore-reader, and the cipher-solver all have a lane: per-player
effects/reactions reward exploration, the Discord layer surfaces recovered fragments for readers,
decode-minded players get the hard nuts. Bake in escalating in-fiction hints (an NPC who says more if
you linger; a second paper cross-referencing the first) so a stuck group is **nudged, never walled.**

### Theme F — Minecraft-medium craft (Wifies / found-footage genre)

**F1. The ordinary made wrong — seed anomalies into a normal world, not a built "horror map."**
The breakout works open on an unremarkable, even cozy survival world so the first small wrongness lands
hard. Wrongness is conveyed by the **world,** never exposition.
*From:* Wifies "Searching For A World That Doesn't Exist" / King in Yellow ARG. **(Verified this session.)**
*Source:* https://www.argn.com/2025/11/the_king_in_yellow_as_found_footage_minecraft_arg/
**MC:** Build the lost city as a believable, lived-in, functional place first (homes with stored food,
tools mid-task, working farms, lit streets) via FAWE. Stage it as if its people were mid-life,
competent, ordinary. Then introduce anomalies *sparingly* into that ordinariness — one wrong plant, one
impossible block — so reconstruction starts from "these were normal people" and curdles. Avoid any block
palette that screams "spooky map."

**F2. Impossible-but-rule-based generation — break Minecraft's own logic.**
Full-grown forests, grass, and moss deep underground where no light reaches read as "*someone* did
this" — environmental narrative with zero text, because viewers know the game's rules.
*From:* Wifies ARG (Subterranean Forests). **(Verified this session.)**
*Source:* https://tvtropes.org/pmwiki/pmwiki.php/WebVideo/SearchingForAWorldThatDoesntExist
**MC:** FAWE-paste **subterranean forests** — living trees, grass, daylight-blue glass skylights — into
deep caves. Crops growing on stone, water flowing uphill in a sealed room, a wheat field with no sky.
Each impossibility is a silent clue about what the lost people (or the surface thing) were capable of.

**F3. The half-beat-late footstep — implication over evidence.**
Every time the player stops, one extra footstep just out of sync, as if something stopped behind them a
fraction too late. Audible from the first cave; most viewers miss it first watch. Nothing is ever shown.
*From:* Wifies ARG. **(Verified this session.)**
*Source:* https://en.namu.wiki/w/Searching%20For%20A%20World%20That%20Doesn't%20Exist
**MC:** Per-player tick-loop tracks last movement; when the player stops, play a single muffled
`block.*.step` (custom resource-pack variant) from ~1 block *behind* their facing, delayed 4–6 ticks.
Tie it to specific players the world has "noticed" (those who read certain books / entered certain
rooms) so it feels personal. Use spatial audio — it must come from *behind.* Never spawn a visible
entity.

**F4. Silent extinguishing — the torch scene.**
Torches *behind* the player go out one by one in total silence, noticed only on turning around. Light =
the safety/competence the player established; its quiet removal says "something is here and doesn't want
you to see."
*From:* Wifies ARG. **(Verified this session.)**
*Source:* https://www.minecraft-arg.fandom.com/wiki/Searching_For_A_World_That_Doesn't_Exist
**MC:** Track the player's path; with a `setblock` loop replace lit torches/lanterns *behind* them with
unlit variants (or air), in chunks they can no longer see, in silence. Generalize it: a lamp the player
placed is dark when they return; a room they lit is dark again. The city "un-lights" itself as the
player advances.

**F5. Proof-of-presence via mechanics players understand (the unloaded-chunk grass trap).**
Grass spreading in a chunk that should have unloaded proves another entity was physically standing there
loading it — dread built from the game's *real rules,* not magic. The creator even sets chunk-load traps
to confirm he's followed.
*From:* Wifies ARG. **(Verified this session.)**
*Source:* https://en.namu.wiki/w/Searching%20For%20A%20World%20That%20Doesn't%20Exist
**MC:** Engineer mechanic-literate "someone was here" proofs the caretaker leaves: crops that advanced a
growth stage in an area the player sealed and left; a door found open that was closed; a single
disturbed-snow footprint trail; a redstone lamp toggled. Let savvy players set their *own* traps and
reward the paranoia by subtly altering the world on return. Discord can host an "is anyone else loading
my chunks?" rabbit hole.

**F6. Restraint at the reveal — censor the source.**
The entity appears only briefly, covered by a literal **censor box**; at the climax the player stares at
something behind the doors long enough that the viewer strains to see — then turns and runs, and we are
never shown. 10M+ views, essentially no jumpscares; the withheld image beats any model.
*From:* Wifies ARG (King in Yellow behind a censor box). **(Verified this session.)**
*Source:* https://tvtropes.org/pmwiki/pmwiki.php/WebVideo/SearchingForAWorldThatDoesntExist
**MC:** Never give players a clean, lit look at the truth of the lost world or the surface presence. If
the antagonist is a MythicMobs + ModelEngine entity, reveal it only in fog, at render-distance edge,
partially occluded, or behind a resource-pack "corruption" overlay that blocks screen-center. Stage the
climactic reveal as a sealed door / a thing glimpsed through a gap — give the staring beat, then force
blindness (Darkness/blindness effect) before resolution. Let Discord lore fill the gap, never the game.

**F7. Diegetic chain-of-custody — the fake-bureaucratic recovery frame.**
The story is recovered media: a found laptop, a prior player's Google Drive footage, and a "Notice of
Removal" PDF stating the footage was "seized for investigation by the US D.M.S." Fake institutional
paperwork grounds the impossible in mundane authenticity.
*From:* King in Yellow ARG ("Notice of Removal" PDF; found footage of d3rlord3).
*Source:* https://www.argn.com/2025/11/the_king_in_yellow_as_found_footage_minecraft_arg/
**MC:** Frame The Observance's world as **recovered** — not "a server" but the salvaged save of the
people who lived there. In-world books are recovered correspondence/logs that half-survive (torn pages =
missing text). Mirror this in a Discord/PDF companion layer: official-looking "recovery notices,"
redacted incident reports, a named prior inhabitant whose half-deleted account the players piece
together. The bureaucratic frame answers "why are we here" diegetically and seeds "were they even human"
through clinical language.

**F8. Deceit over explicit threat — the empty room is the asset; the scare is rare and earned.**
"Deceit is the source of a quality scare"; a completely empty room with just ambience and lighting can
be made horrifying. Atmosphere and the *breaking* of a familiar pattern do the work.
*From:* Minecraft Forum horror-map guide.
*Source:* https://www.minecraftforum.net/forums/minecraft-java-edition/creative-mode/367898-how-to-create-the-best-horror-map-possible-and
**MC:** Let 95% of the lost city be safe, empty, quiet — let players relax and explore freely. Plant
pattern-breaks: a house identical to twenty others but with the furniture rearranged wrong; a room
slightly larger inside than its footprint; a corridor that loops where it shouldn't. Per-player triggers
fire rarely. The Watcher should mostly do *nothing* visible — its restraint is what makes the rare
intervention terrifying.

**F9. Sound is the primary horror surface; a sharp sting must be brief.**
Custom audio via resource pack is "critical"; for a sting "make sure you can't hear it longer than you
need to." Wifies' signature stack: a scare chord, then loud incoherent whispers "loud enough to drown
out everything except the player's rapidly beating heart."
*From:* Wifies ARG + Minecraft Forum guide.
*Source:* https://tvtropes.org/pmwiki/pmwiki.php/NightmareFuel/SearchingForAWorldThatDoesntExist
**MC:** Ship a custom resource pack that (a) replaces/extends ambient cave sounds with bespoke whispers,
distant knocks, and a subsonic heartbeat that ramps near certain coords; (b) adds rare per-player
"scare chord" stings via `/playsound`, kept very short; (c) swells a low heartbeat when a player is
being "noticed." Reserve the loud whisper-wash + heartbeat for the few apex moments. Spatialize
everything (behind, below, the next room).

**F10. Fog/darkness/desaturated sky as a permanent mood floor.**
The desolate apex zone is "eternally foggy with barely any light." Resource packs achieve dread by
darkening daytime, blackening nights, and recoloring fog/sky to muted sepia. Darkness is a *designed*
oppressive material, not mere absence of light.
*From:* Wifies ARG + horror resource-pack craft.
*Source:* https://www.curseforge.com/minecraft/texture-packs/dark-atmosphere-and-dreadful-skies
**MC:** Use a resource pack + fog/render settings (and a "Foggy"-style datapack) to give the
deepest/most-wrong zones a custom fog color, shortened fog distance, desaturated sky, and near-black
nights. Make daylight in the ruined city dimmer than vanilla so even "safe" exploration feels off.
Reserve full vanilla brightness/saturation for rare "this is how it was when they lived" flashback
builds — the contrast becomes a storytelling tool.

**F11. Escalating, trigger-based ambient anomalies tied to player BEHAVIOR (an "Attention" system).**
The longer/deeper a player transgresses (lingers in dark, mines too long, kills many mobs), the more the
world manifests — ambient dread → doors moving → lights snuffing. Localized (whispers) or server-wide (a
"Red Moon"). Gradual escalation avoids the predictable screamer.
*From:* AmbientEvents plugin + Foggy datapack Attention tiers.
*Source:* https://modrinth.com/plugin/ambientevents
**MC:** Build (or adapt AmbientEvents/Foggy-style plugins) an **Attention/Observance meter** per player
that rises with transgressive acts (digging into sealed tombs, reading forbidden books, looting the
dead) and decays with restraint. Tier the manifestations: **T1 ambient** (cold breeze, falling leaves,
distant whisper) → **T2 personal** (footstep behind, a torch out) → **T3 world-level** (server-wide fog
event, the Watcher moves a build). The world reacts to *specific players' choices.*

**F12. Puzzles as the texture of the descent, not the skeleton (genre-native).**
Even in the puzzle-heavy King in Yellow ARG, ciphers are *carved into cave walls,* poems decode with
Vigenère keys, and a Drive link is found by decoding a *pattern in the player's inventory* — decoding
always emerges from the world and serves the story of who these people were, never gating raw progress.
*From:* King in Yellow ARG (wall ciphers, Vigenère poems, decoded inventory pattern → Drive link).
*Source:* https://www.argn.com/2025/11/the_king_in_yellow_as_found_footage_minecraft_arg/
**MC:** When you use a cipher, carve it into a build that already tells a story (a tomb wall, a child's
schoolbook) so even *un-decoded* it reads as character/history. Hide keys as environmental patterns
(block colors in a floor, the order of graves, an item arrangement in a chest), not standalone puzzle
rooms. Always make the **solution a lore fragment** ("we stopped being able to sleep"; "the third one
came back changed"), so decoding deepens reconstruction rather than just unlocking a door.

**F13. The found-footage explainer video is part of the artifact; the slow multi-drop cadence is what
makes it blow up.**
The ~40-min ARG-explainer documentary frame lets the audience reconstruct *alongside* the creator;
withholding across multiple drops sustains the mystery and the community speculation between drops
becomes the engine. (4 months, 10M+ views, no jumpscares.)
*From:* Wifies ARG.
*Source:* https://wifies.fandom.com/wiki/Searching_For_A_World_That_Doesn't_Exist
**MC:** Plan the YouTube cut as a Wifies-style mystery doc that reconstructs the lost world *on camera*
from fragments, withholding who/what they were until the finale. Mirror the cadence in-world and on
Discord: release fragments (a recovered book, a new accessible zone, a "leaked" incident PDF) on a slow
drip so the friend group's *real* speculation drives the next reveal. Let the Discord layer host the
"explainer" role — pinned theory threads, a redacted document-drop channel.

### Theme G — The reactive / "it-knows-me" advantage (The Observance's structural edge)

**G1. Emergent/reactive moments feel more authentic than scripted ones — the consequences are *yours.*
**
"Emergent moments stick with players far longer than a pre-written plot twist." Design world-*systems*
that react to specific actions, then let story emerge, rather than pre-authoring every outcome.
*From:* PlayStation Universe; Wayline (systems-first design).
*Source:* https://www.psu.com/news/why-player-driven-stories-outshine-scripted-narratives-in-gaming/
**MC:** Don't script "on day 3 the bridge collapses." Build a small reactive engine keyed to a
**per-player/per-group dossier** (usernames, total playtime, which fragments each read, where they
linger, deaths, chat keywords). Wire **conditions, not timelines**: "if this player has read papers A+C
but not B, the Watcher leaves B's missing half where they sleep." Scoreboard objectives + a sidecar
data store = the dossier; FAWE pastes and `/execute`-driven block changes = the world's reactions. The
reveal feels authored-by-play because it literally is.

**G2. Systems-first environmental cause-and-effect — small input cascades into unscripted story.**
"If a player kills all the merchants, the economy should suffer." Authenticity comes from consequence,
not prose.
*From:* Wayline (Dwarf Fortress cascade example).
*Source:* https://www.wayline.io/blog/beyond-branching-narratives-emergent-storytelling
**MC:** Give the lost city a few simulated "organs" that visibly respond: a faction-attention meter (the
more players excavate/loot a district, the more the Watcher's MythicMobs patrol it); a light/water
economy (break a seal → FAWE floods/drains a connected area on a delay). Implement with WorldGuard
region flags + scoreboard counters + a scheduled task that translates counters into FAWE pastes, mob
spawns, fog density. The friends learn the world has rules *by violating them* — the pushback is the
narration.

**G3. The uncanny "it knows me" beat — grounded in REAL player data, used sparingly.**
DDLC/Eternal Darkness/IMSCARED reading your name/saves/clock and calling out your specific behavior is
devastating *because* it's real data — but collapses into gimmick the moment it's gratuitous. One
perfectly-timed beat is worth ten cheap ones.
*From:* TheGamer — best fourth-wall breaks (Eternal Darkness, IMSCARED, DDLC).
*Source:* https://www.thegamer.com/horror-game-broke-fourth-wall/
**MC:** Stage *rare, earned* moments where the Watcher demonstrates it knows *this* player using real
dossier data, in-fiction: a book that addresses them by username in old script ("We saw you, [Username].
You came back to the well four times." — auto-generated from a movement heatmap); a sign that rewrites
itself to reference a fact only true of them ("you have died here twice"); a whisper played to ONE
player via `/playsound @p` when alone; a Discord DM from the Watcher persona referencing a real thing
they did an hour ago. Per-player books via plugin-generated NBT; per-player signs via packet-level
updates (ProtocolLib) so only the target sees their version. **Restraint is the whole craft.**

**G4. NPCs that remember and treat you differently by past actions/reputation.**
Memory + behavioral adaptation (not scripted lines) is what makes an NPC feel alive and aware of the
individual.
*From:* Lenovo — how AI personalizes games (RDR2 reputation memory).
*Source:* https://www.lenovo.com/us/en/gaming/ai-in-gaming/ai-and-game-personalization/
**MC:** Citizens2/ZNPCsPlus NPCs whose dialogue trees branch on the **dossier**, not a global flag. The
same NPC greets a player who respected the graves differently from one who looted them ("You're the one
who broke the seals"). Quote players back to themselves; have a proximity-triggered hologram mutter
about an event *this group* caused. The NPC isn't reciting lore — it's reacting to who's standing in
front of it.

**G5. Puppetmaster craft: alter content live, incorporate player content, leave white space — keep
TINAG.**
Puppetmasters "alter the game's content in real time," "incorporate player content and respond to
players' actions, analysis and speculation," and deliberately "leave white space for players to fill
in" — all while maintaining TINAG so interactions feel genuine.
*From:* ARGN/Wikipedia/TVTropes ARG craft.
*Source:* https://en.wikipedia.org/wiki/Alternate_reality_game
**MC:** Run The Observance like a live ARG, not a pre-baked map. The autonomous director handles
baseline reactivity (mobs, fog, ambient events keyed to the dossier); a human puppetmaster conducts the
peaks — hand-place a paper that answers (or deepens) a theory the group voiced in chat ten minutes ago;
rename a mob or write a sign that **picks up a name the players invented** for something; withhold a
reveal because they're close on their own (the white space). Tooling: a live op console (RCON or
dashboard) to trigger FAWE pastes, spawn named MythicMobs, push per-player books, and post Discord
messages on the fly. The Watcher never says "good puzzle" — it only ever *behaves as a thing that
exists.* Their speculation becomes canon the world quietly confirms.

**G6. Environmental storytelling = show the final outcome, invite reconstruction of the cause.**
Five techniques: level-design geometry for emotional tone; narrative props ("treat every asset like an
artifact"); item descriptions that "imply more than they explain"; NPC/enemy placement that shows how
the world works; safe zones established then strategically *broken.*
*From:* Keewano; USC Scalar (narrative stratigraphy).
*Source:* https://keewano.com/blog/5-environmental-storytelling-techniques-every-game-writer-must-know/
**MC:** This is the substrate the reactivity rides on. (1) **Geometry:** FAWE-paste a district whose
layout alone implies who lived there (cramped worker warrens vs one impossible cathedral) — answers "were
they human?" spatially. (2) **Narrative props:** frozen scenes — two skeletons mid-argument, a child's
toy walled into fresh stone, a meal set for guests who never came. (3) **Item descriptions:** renamed
items / books that imply more than they state — a ration tin labeled in a script that's almost-but-not-
Latin. (4) **NPC placement:** where surviving NPCs stand and what they fear shows the power structure.
(5) **Broken safe zones:** give players a "home" base, then have the director quietly alter it between
sessions (a door now bricked, an extra chair at the fire) so the familiar turns uncanny — environmental
storytelling fused with per-player reactivity.

**G7. Real-time behavioral personalization — bend the world to each player's habits, no overt "gotcha."
**
Analyze behavior to deliver content matched to habits and discovery order (Destiny 2 adapting events).
The "it knows me" feeling at the *system* level, with no explicit fourth-wall break.
*From:* CleverTap; Lenovo.
*Source:* https://clevertap.com/blog/real-time-personalization-in-mobile-gaming/
**MC:** Quietly tune the world per player via the dossier. If someone always explores at night, the
Watcher's manifestations cluster at night for them (per-player time/fog via packets). If someone reads
every paper, route more papers near their path; if someone rushes, give them ambient dread (per-player
`/playsound`, packet particles only they see) instead of text. **Discovery-order awareness:** the same
room reveals different fragments depending on what the group already knows, so two playthroughs diverge.
The reactivity is invisible-but-felt — the world simply *fits* them.

**G8. Stage the *conditions* for reactive moments (so they land on camera) — never fake the beat.**
The ARG resolution to camera-vs-genuine: author the conditions that make a reactive beat *likely,* not
the beat itself; the real, player-triggered moment then happens richly and on camera without being
faked.
*From:* ARGN/TVTropes puppetmaster craft + Wayline (authenticity from real consequence).
*Source:* https://tvtropes.org/pmwiki/pmwiki.php/Main/AlternateRealityGame
**MC:** For the Wifies-style video, never fake a reaction — **engineer the odds.** Pre-stage a FAWE
reveal, a custom MythicMob entrance, and a per-player whisper so they're *armed,* then let the player's
genuine action fire them while recording. The puppetmaster nudges pacing live (delays a spawn until the
group is together in frame, holds a reveal until a player turns a corner) — conducting, not scripting.
Keep a "reaction palette" ready in the live console (3–4 prepped FAWE pastes, named mobs, book pushes)
so the world can answer a great unexpected moment within seconds. The footage is authentic because every
beat is gated on a real player choice; the camera just happens to be there because the conditions were
staged.

---

## 3. Design Commandments (the do/don'ts)

1. **Plan and seal the ending FIRST.** Write the full truth (who / what the place was / what happened /
   were they human) in an out-of-world answer bible before placing a single fragment. (E1)
2. **Every gate is also a reward.** A solved fragment always yields *lore* — a piece of the lost world
   snapping into place — never just a key to the next lock. (E3)
3. **Ground every mechanic in fiction (TINAG).** No [Quest] tags, no "clue found" toasts. Decoding must
   emerge *from* the world; the Watcher only ever behaves as a thing that exists. (C1, F12)
4. **Trickle real answers on a fixed cadence.** Never let open questions outrun answered ones; deliver at
   least one undeniable reveal per act. (E2)
5. **Omit to imply (iceberg / redaction).** Censor the one thing they most want; give enough to guess,
   never enough to confirm. The unstated mass carries the weight. (B2)
6. **Under-specify to force collaboration.** Never give a fragment *and* its interpretation together;
   gate reveals behind N-of-M pieces no single player holds. (C3)
7. **Wrongness via the world, not exposition.** The ordinary made wrong; impossible-but-rule-based;
   footsteps and snuffed torches over narration. A player who reads nothing should still feel it. (F1–F5,
   A5)
8. **Earn the tonal contrast.** Celebrate competence and ordinary life *before* the downturn so the
   collapse lands. Reconstruct normal people, then curdle. (D3)
9. **Reconstruct, don't progress.** Threads filling in = progress (C6). The skeleton is the rumor→verify
   loop (C4) and the colored thread map, *not* a cipher chain.
10. **Correct must be obviously correct.** No anagrams or opinion ciphers; only researchable, self-
    verifying systems; the world confirms instantly. (E4)
11. **Restraint at the reveal.** Never a clean, lit look at the truth — censor box, fog, glimpse-through-
    a-gap, forced blindness. The withheld image beats any model. (F6)
12. **Reactivity reads as a world that notices, not a show.** "It knows me" only when earned and grounded
    in real player data; stage the *conditions,* never the beat. (G3, G8)
13. **Ruthless red-herring audit.** Test every number/name/texture for unintended patterns; define the
    canon surface. Only authored trails exist. (E5)
14. **Multiple lanes.** The explorer, the reader, and the solver each advance the reconstruction; a stuck
    group is nudged in-fiction, never walled. (E6)

---

## 4. Bridge — how this reshapes The Observance specifically

**Keep** the load-bearing pillars that already match best practice: **the Watcher** (it becomes the
soft-pressure entity revealed only behind censor/fog/glimpse, F6, and the "it-knows-me" presence
grounded in a real per-player dossier, G3); **the customs** (they become the *enacted* lane — players
physically perform the lost people's rituals, A6 — and the trigger surface for the Attention/Observance
meter, F11); **the keepers AS CHARACTERS** (give them names, desires, relationships, contradictory
accounts, and a tonal-decay arc so the reconstruction lands as emotion, not trivia — B4, B5, D4); **the
consistency discipline** (this is exactly the cross-referencing citation web and the sealed answer
bible / fragment-to-revelation ledger — B3, E1); and **the reactive showrunner** (the live puppetmaster
conducting peaks, incorporating the friends' own speculation and invented names, staging conditions for
on-camera moments — C10, G5, G8). **Change** the spine: retire the **stone-cipher gauntlet** as the
skeleton and replace it with a **reconstruct-the-lost-people mystery** — a layered FAWE world where
geography implies history (human city buried under a wrong-scaled later layer, A2), staged tableaux and
half-survived cross-referencing papers carry the truth (A1, A3, B1–B7), and a Discord "ship-log/recovery
archive" turns every find into a place-anchored rumor card that clusters into ~5 colored threads (WHO /
WHAT / WHAT HAPPENED / WHAT'S ON THE SURFACE / WERE THEY HUMAN) the group watches fill in (C4–C7).
Decoding survives only as **texture** — a Vigenère poem on a tomb wall whose *solution is a lore
fragment,* one rate-determining gate per beat whose payout is a reveal not a key (D1, E3, F12). The
result is the experience the redesign is after: a friend group dropped into an ordinary world made
quietly wrong, reconstructing who these people were and what happened to them — filmed, Wifies-style, as
the answer assembles itself out of the fragments.
