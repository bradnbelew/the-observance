# Research Note: Minecraft Horror / ARG / "Haunted Server" Genre

Lane: `mc-arg-genre`. For **The Observance** — an autonomous, per-player, soft-pressure
server-side haunting. North star: "From The Fog, but it knows your name."

This note is organized by **technique / lesson**. Each item gives the concrete source detail
(real mod, mechanic, or named case study), authoring steps or a behavior pattern where useful,
the **pitfalls**, and a one-line **Apply to The Observance**.

Primary sources are listed at the bottom. The single most valuable artifact to internalize is the
**From The Fog feature list** (LunarEclipse Studio + CurseForge FAQ) — it is essentially a
field-tested taxonomy of "what unsettles" that we can cherry-pick from.

---

## PART 1 — The genre's gold standard: "From The Fog" (Herobrine datapack/mod, LunarEclipse Studio)

This is the closest existing thing to our north star and the most important reference. It is a
**datapack/mod** (works on Java 1.21.x) that turns a vanilla-looking world into a slow cat-and-mouse
haunting. Crucially it is **NOT a chase mod** — its whole identity is restraint. The dev's stated
philosophy (CurseForge FAQ): *"built around psychological endurance"* — the only way to "stop" him
is to disable the mod or config his appearances down. It deliberately avoids the constant-chase
mechanics of copycats.

### 1.1 The full mechanic taxonomy (steal this list)
Every one of these is a discrete, low-cost "phenomenon." This is our menu of haunting beats.

**Presence / sightings (the core dread engine):**
- Watches from the **foot of your bed** while you sleep.
- Watches from **just behind you** — vanishes the instant you turn and spot him.
- Watches **from the shadows in the distance** and flees if you approach (stays at render edge).
- Watches **from deep within caves**.
- **Sighting Sense:** wolves/tamed dogs *growl* when he is near — a non-visual tell.

**Environmental manipulation (proof he was here, while you were away):**
- **Disappearing Torches** — removes torches from areas you've left.
- **Crimson Curse** — replaces your torches with *redstone* torches; lanterns with *soul* lanterns.
- **Chilled Candles** — extinguishes candles when you're not nearby.
- **Burning Base** — sets fire to your structures after you leave the area.
- **Leafless Groves** — strips leaves from forests you've visited.
- **Ghost Miner** — digs strip mines and surface tunnels near you.
- **Twisted Tapestries** — swaps paintings for "haunted" variants.
- **Builds structures** — pyramids, **glowstone letters**, formations rising "from the fog."

**Interaction / manipulation (he touches *your* things):**
- **Ghost Doors** — opens/closes your doors.
- **Dreadful Donations** — leaves *items in your chests* (a gift you didn't ask for = deeply wrong).
- **Sinister Signs** — places signs with ominous messages.
- **No Sleep** — blocks sleep while he's nearby, trapping you in the night.

**Communication (sparse, never chatty):**
- **Caution Caption** — spooky lines injected into the *subtitle* channel (the accessibility
  caption box), not chat — feels like the game itself glitched.
- **Eerie Entrance** — fake join/leave messages (`X joined the game`) after a haunting delay.
- **Fearful Footsteps** — disembodied footstep sounds at random.

**Escalation / meta:**
- **Activation Delay** — does *nothing* for **3 in-game days** after install (configurable). Builds
  safety so the first event lands harder. A **shrine** the player builds skips the delay & raises
  frequency (player opts into more).
- **Sneaky Strike** — only attacks if you take too long to notice him approaching (rare punctuation).
- **Sudden Scare / Malicious Malfunction** — jumpscare + optional fake game-crash. **Disabled by
  default**, epilepsy-warned. The dev treats the jumpscare as the *exception*, not the product.
- **Give Him Control** — Herobrine can autonomously edit his own config (escalate himself).

### 1.2 WHY it unsettles (the psychology, cross-referenced w/ horror-design sources)
- **Anticipation > shock.** Sustained anticipation = prolonged arousal + lingering unease; jumpscares
  = a brief spike that dissipates and is "quickly forgotten." Dread "guides the audience to the shape
  of fear *before* revealing the monster." (Marni Molina; Horror Homeroom.)
- **The threat you imagine.** "It's not the monster we see, but the one we imagine, that terrifies."
  Glimpses + ambiguity engage imagination; full reveals defuse it. "The longer you get to stare at a
  monster, the less scary it becomes." (Jay Kozatt horror-design.)
- **Proof-of-presence without presence.** The *environmental* edits (moved torch, doused candle,
  opened door, gift in chest) are scarier than the entity because they prove intelligence + intent
  while you were alone, and they're discovered *after the fact* — your safe base is retroactively
  violated.
- **Asymmetry of knowledge.** He sees you; you almost never see him. Wolves growling = he's giving
  *you* a sensor but withholding himself.

**Pitfall (from the genre's own copycats):** the "comes closer when ignored, vanishes/aggros when
looked at" stalker loop (The Man From The Fog) is punchier but burns out fast and collapses into a
chase game. From The Fog is revered *because* it resists that.

**Apply to The Observance:** Build our autonomous beat library as a direct analog of this taxonomy,
but **per-player and name-aware** ("it knows your name"). Tag every beat as `presence | env-edit |
interaction | comms | escalation`. Default to env-edit + presence; gate jumpscares behind an
explicit, rare, opt-in-ish escalation tier exactly as From The Fog gates `Sudden Scare`.

---

## PART 2 — Specific in-game phenomena that reliably create dread (ranked)

A practical, ranked list distilled from From The Fog, cursed-seed lore, and liminal-space theory.
Roughly ordered most-reliable → most-overused.

1. **Block changes discovered out of sight** — something altered in a space you *thought* was yours,
   found *after* the fact (torch gone, candle out, painting swapped, door now open, a single block of
   the wrong type in your wall). Highest dread-per-effort. *Requires the player to have a "home" the
   change can violate.*
2. **An entity always at exactly render distance** — seen, then gone when approached/looked-at.
   Never closes. The unbroken distance IS the horror.
3. **A gift / message addressed to YOU** — item placed in your chest, a sign with your name, a book.
   Intent + personalization. (Our name-aware north star lives here.)
4. **Wrong sounds with no source** — footsteps behind you, a far cave ambience indoors, a mob sound
   from a mob that isn't there, sudden *silence* (cutting ambient audio is itself a scare cue).
5. **Subtitle / UI channel intrusion** — text in the caption box or a fake join message; feels like
   the *game* is haunted, not a mob. Cheap, deniable, deeply uncanny.
6. **Sensor tells** — wolves growl, a compass spins, particles where nothing is. Gives the player a
   detector but not the thing.
7. **Light/atmosphere shifts** — torches→redstone (dimmer red), fog thickening, leaves stripped,
   a structure altered. Liminal "wrongness."
8. **Named mobs / possessed passive mobs** — a sheep named after the player, an animal that follows
   too precisely. (Use sparingly; one-off.)
9. **Built structures rising in the distance** — glowstone letters, a pyramid, a shrine you didn't
   build. High-effort, high-payoff, best as rare arc beats.
10. **Direct sighting / jumpscare** — the *least* reusable. One good one per arc, maximum.

**Liminal-space principle (Backrooms / wonderland.jar):** dread also comes from *absence* — an
emptied, context-stripped, too-quiet version of a familiar place. "The absence of living things,
particularly other people." Minecraft is *already* slightly liminal; a haunting can lean on
emptiness (no mobs spawning, unnatural quiet) instead of adding a monster.

**Apply to The Observance:** Weight the autonomous director's beat-selection toward 1–7. Reserve
8–10 for arc punctuation managed by the between-session showrunner, never the per-tick loop.

---

## PART 3 — What is OVERUSED / cliché / cringe (hard avoid-list)

From genre fatigue threads, creepypasta-wiki retakes, and ARG critique:

- **Green/white-eyed Steve jumpscare + screech + screen-black.** The single most exhausted trope.
  Forums literally beg creators to "find something new." Avoid the literal Herobrine skin entirely.
- **Loud screech + flash + fake crash as the *payoff*.** Reads as cheap; "quickly forgotten." If used
  at all, it's punctuation after a long dread build, never the substance.
- **Obvious command-block / redstone trap reveals.** Breaks the "it's intelligent and real" spell —
  the player sees the machinery.
- **Edgy gore/satanic text, "666," "I'm watching you," demonic fonts.** Tryhard; collapses mystery
  into camp. Cursed-seed creepypasta is widely mocked for this.
- **Over-explaining / over-appearing.** Mystery dies the moment it's fully shown or fully explained.
  Frequency is the enemy: a beat that fires often becomes a mechanic, not a haunting.
- **Constant chase.** Turns horror into an action loop; resets to zero dread between chases.
- **Breaking the diegetic frame** — meta "haha got you" messaging, creator self-inserts, anything
  that says "this is a prank." Kills the unfiction.

**Apply to The Observance:** Encode this as a **"slop filter"** in the director: ban literal
Herobrine assets, cap jumpscares (e.g. ≤1 per arc, opt-in tier), forbid `666`/edgy strings in the
generated-text validator, and rate-limit every beat type so nothing becomes predictable.

---

## PART 4 — Sustaining mystery without it collapsing (ARG craft)

From ARG-craft sources + the "Searching For A World That Doesn't Exist" (Wifies/D3rlord3) case study,
the most-praised modern Minecraft mystery (10M+ views, *no jumpscares*).

### 4.1 Lessons from "Searching For A World That Doesn't Exist"
- **Dread with zero jumpscares.** Praised precisely for inducing anxiety "without any jump scare
  elements" — anomaly + atmosphere + a competent protagonist who's clearly out of his depth.
- **The "smart protagonist" amplifies fear.** D3rlord3 reads as the *smartest* protagonist in the
  genre — and it's *more* terrifying that even he is "playing into the hands of the King." A capable
  victim losing makes the threat feel vast. (For us: the *player* is the smart protagonist; the
  haunting should make a competent player feel subtly outmaneuvered, never cheated.)
- **Layered, slow-cooked puzzles.** Ciphers built by *overlapping* encryption methods took the
  audience weeks to crack; a real Google-Drive payload existed to verify. Depth rewards the obsessed
  without gating the casual.
- **Lovecraftian grounding (The King in Yellow).** An external mythos gives the mystery a coherent
  spine so clues feel like they point *somewhere*, even unsolved.
- **The "ARG Explainer" as content.** Wifies handed it over as a polished, fully-solved 40-min video
  while *secretly being the author* — control of the narrative frame.

### 4.2 General ARG pacing rules
- **Start subtle; don't beg to be found.** A good ARG "doesn't try too desperately to be discovered."
  Only ramp once eyes are on it. (Mirror From The Fog's 3-day delay.)
- **Obscurity + ambiguity are features.** Players "should feel confused… about authenticity *and*
  meaning." The question "is this real?" is the engine.
- **Never confess authorship in-world.** "Even if it ends, it never surrenders to admittance." The
  mask staying on is what makes it novelty rather than a bit.
- **Let imagination fill the gaps.** "The mystery itself becomes more frightening than any answer."
  Withhold the explanation as long as possible; a revealed answer is a smaller answer.

**Pitfall:** the moment the audience can fully model the system, dread → curiosity → boredom. Keep an
irreducible unknown. Wifies' ARG stays scary partly because it was *never fully solved*.

**Apply to The Observance:** (a) The autonomous director must preserve an "irreducible unknown" — its
beat selection should feel *intentional* but never fully predictable to the friends, so they can't
reverse-engineer the rules. (b) The between-session showrunner is our "King in Yellow spine": a
loose, evolving mythos that makes scattered beats feel like they point somewhere. (c) **Never break
frame in-game** — all "it's a project" acknowledgement stays out-of-band (the friends installing a
resource pack is the only seam; nothing in chat should wink).

---

## PART 5 — Herobrine as proto-ARG (lore lessons)

Origin (Minecraft Wiki / Wikipedia / GameRant): **2010-07-31**, a forum post copy-pasted from 4chan
/v/ — a player sees a white-eyed entity after playing music disc "13," tied to Notch's (fictional)
dead brother. It exploded because streamer **Copeland (Brocraft)** photoshopped/texture-modded
Herobrine onto a wall and spread the footage. Notch confirmed it a hoax at MinecraftCon 2010 — *but
joked it might not be.* Later, **OldRoot (2014, Alex Bale)** was a true ARG with spectrograms, codes,
hazy blank-eyed-Steve images; **never solved.**

**Lessons:**
- **Plausible deniability is the fuel.** Herobrine worked because every sighting *could* be a mod, a
  glitch, or real. The ambiguity, not the entity, is the legend. Notch's "it's a hoax… or is it?"
  is the perfect register.
- **Sparse, low-fidelity sightings out-perform polished ones.** A blurry, brief, deniable glimpse
  spreads further and lasts longer than a clear render.
- **Community storytelling > author storytelling.** Herobrine grew because *players* generated and
  swapped sightings. The legend lived in their retellings.
- **A single iconic音 / artifact anchors it** — music disc "13," the white eyes, the `13`/`11` cave
  ambience discs. One recognizable motif gives the myth a handle.

**Apply to The Observance:** (a) Engineer for *plausible deniability* — many beats should be
explainable as "lag," "did I leave that open?", "did you do that?" so friends argue about whether it's
real. That argument IS the product. (b) Pick **one anchoring motif** (a sound, a symbol, a name-glyph)
that recurs so retellings have a handle. (c) Design so the *friends* become the storytellers — beats
should be screenshot-worthy and "wait did that just happen to YOU too?" — leveraging a veteran friend
group's group chat as the propagation layer.

---

## PART 6 — Server-side "someone is messing with us" (prank / gaslight craft)

From griefing/social-engineering write-ups (caliasiangirl Medium case studies, MinecraftOnline). The
useful part is the *psychology*, not the malice — we want the unease, not the harm.

- **Subtle alteration beats destruction.** What rattles victims is *gaslighting* — "did I move that?
  did you?" — far more than overt griefing. A single moved/added/removed block that the player can't
  prove they didn't do is the strongest prank primitive.
- **Social engineering / trust-then-violate.** Griefers gain build trust, then abuse it; the betrayal
  is the payload. Our analog: the haunting should target the player's *sense of ownership* of their
  base, the one space they trust.
- **Hard ethical line for us:** real griefing groups leave fake "ransom notes," mock, and gaslight to
  cause genuine distress. **The Observance must invert this** — soft-pressure, consent-framed (they
  opted in by joining), never destructive of progress they care about, always *recoverable*. The dread
  should be "this is delightfully creepy," never "my stuff is actually gone."

**Apply to The Observance:** Our most potent, cheapest beats are **deniable micro-alterations** to a
player's own builds (one block, a torch, a door) — pure gaslight, zero destruction. Pair every
"violation" beat with reversibility so it never crosses into real grief. The veteran-friend context
means the bonding is "we survived this together," so keep harm at zero and shared-experience high.

---

## PART 7 — The "I found a cursed world" / Wifies video format (if we ever film it)

(Cross-ref: separate `youtube-atmosphere` lane.) Format DNA:
- **Found-footage / "analyzing the files" framing** — the creator presents as an investigator of
  someone else's footage/world, not a player. Distance + documentary tone = credibility.
- **The polished "ARG Explainer."** Hand the audience a crisp, mostly-solved package while *being* the
  secret author. Total narrative control; leaves a deliberate residue of unsolved.
- **Editorial = "smartest protagonist."** Cut anything that contradicts the protagonist-is-sharp
  narrative; the smart victim losing is what scares.

**Pitfall:** the explainer format only works if the underlying haunting is *genuinely* eerie on its
own — editing can't manufacture dread that the footage doesn't contain.

**Apply to The Observance:** Our autonomous, real, per-player haunting is exactly the kind of footage
that *can't be faked* — that authenticity is the asset a Wifies-style edit would amplify. Capture beats
server-side (we control the server) so a later "what happened on our server" cut has real material.

---

## Sources (primary first)

- From The Fog — LunarEclipse Studio (official): https://lunareclipse.studio/creations/from-the-fog
- From The Fog FAQ — CurseForge (dev): https://blog.curseforge.com/from-the-fog-frequently-asked-questions/
- From The Fog — CurseForge: https://www.curseforge.com/minecraft/mc-mods/from-the-fog
- From the Fog (Herobrine) — Minecraft Horror Wiki: https://minecraft-horror.fandom.com/wiki/From_the_Fog_(Herobrine)
- The Man From The Fog — official: https://manfromthefog.com/ ; CurseForge: https://www.curseforge.com/minecraft/mc-mods/the-man-from-the-fog
- The Man From The Fog FAQ: https://blog.curseforge.com/the-man-from-the-fog-mod-frequently-asked-questions/
- Herobrine — Minecraft Wiki: https://minecraft.wiki/w/Herobrine ; Wikipedia: https://en.wikipedia.org/wiki/Herobrine
- Herobrine creepypasta explained — GameRant: https://gamerant.com/minecraft-herobrine-creepypasta-gaming-horror-legend-explained/
- The King in Yellow as Found-Footage Minecraft ARG — ARGNet: https://www.argn.com/2025/11/the_king_in_yellow_as_found_footage_minecraft_arg/
- Searching For A World That Doesn't Exist — Minecraft ARG Wiki: https://minecraft-arg.fandom.com/wiki/Searching_For_A_World_That_Doesn't_Exist ; TV Tropes: https://tvtropes.org/pmwiki/pmwiki.php/WebVideo/SearchingForAWorldThatDoesntExist
- Alternate Reality Game — TV Tropes: https://tvtropes.org/pmwiki/pmwiki.php/Main/AlternateRealityGame
- ARG: When Haunting Fiction Becomes Real — Charity Storm (Medium): https://medium.com/@goldmossmoon/arg-what-happens-when-haunting-fiction-becomes-real-life-horror-22b4ad739b5c
- Fear Without Jumpscares — Jay Kozatt (Medium): https://jaykozatt.medium.com/the-key-to-horror-game-design-68ff3a13c17b
- Dread vs Terror vs Horror — Marni Molina (Medium): https://medium.com/@MarniWrites/dread-vs-terror-vs-horror-1c9c5dd8e5b0
- Pervasive Dread in Subnautica — Game Studies: https://gamestudies.org/2404/articles/evans
- Liminal space (aesthetic) — Wikipedia: https://en.wikipedia.org/wiki/Liminal_space_(aesthetic) ; The Backrooms: https://en.wikipedia.org/wiki/The_Backrooms
- wonderland.jar liminal-space mod — Gamezebo: https://www.gamezebo.com/features/minecraft-mod-spotlight-wonderland-jar/
- Minecraft griefing organizations case studies — caliasiangirl (Medium): https://medium.com/@caliasiangirl/how-griefing-groups-are-exploiting-unsecured-minecraft-servers-mlpi-ogmur-5th-column-104c98a372ea
