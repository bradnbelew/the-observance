# ARG Design Craft — The Genre's Real Rules

**Lane:** arg-craft. **For:** The Observance (server-side Minecraft ARG, Paper 1.21.x + Discord bot + Supabase/Vercel dashboard, veteran friend group, Path A = one auto-pushed resource pack). North star: *"From The Fog, but it knows your name."*

This note is the **design-craft companion** to `design/ARG-RESEARCH.md` (which covers environmental-storytelling technique and the Minecraft realization layer). Here the focus is the *genre's authored rules*: TINAG, trailheads/rabbit holes, hard-but-fair puzzle design, hint/curtain economies, multi-week pacing under irregular play, solo-vs-collective design, red herrings, and the curtain. Every lesson is pulled from a named case study and tagged **→ Observance:**.

A friend-group ARG (5–15 veterans, irregular login, no marketing budget, no hivemind) is **a different animal** from the megagames below. Most ARG canon was built for tens of thousands of simultaneous solvers. The single most important meta-lesson: **scale down the difficulty and scale up the rescue.** A 10-person group has no thousand-person hivemind to brute-force a cipher in 20 minutes — so puzzles that would be "too easy" for I Love Bees are correctly calibrated for us, and a stuck friend group has no crowd to carry it. Design accordingly.

---

## 0. The meta-frame: friend-group scale changes every rule

| ARG-canon assumption | Reality for The Observance | Consequence |
|---|---|---|
| 10,000+ simultaneous solvers (hivemind) | 5–15 friends, often solo, irregular | Puzzles must be ~10× easier; rescue must be ~10× more reliable |
| Players opt in by hunting a rabbit hole | Friends are already *on the server* | The "rabbit hole" is internal — a trailhead they trip over in-world, not a hidden URL |
| Anonymous strangers, TINAG illusion intact | Friends *know* you built it | TINAG is theatrical, not literal — "stay in character," not "pretend it's real" |
| Designer reacts in real time to a crowd | One showrunner, between-session batch | Reactivity is *asynchronous* (the showrunner), not live (puppetmaster on a payphone) |
| If it dies, 9,990 others carry on | If 3 people lose the thread, it's dead | Pacing must survive a 2-week gap where nobody logs in |

Hold this table next to every lesson below.

---

## 1. TINAG — "This Is Not A Game" (the aesthetic, not the lie)

**Source / who:** Elan Lee (lead developer, *The Beast* 2001 & *I Love Bees* 2004), first articulated at GDC 2002. ([argn.com](https://www.argn.com/2005/04/this_is_not_a_game/), [Wikipedia: TINAG / ARG](https://en.wikipedia.org/wiki/Alternate_reality_game), [Wikipedia: The Beast](https://en.wikipedia.org/wiki/The_Beast_(game)))

**Elan Lee's three rules (GDC 2002):**
1. **Don't tell anyone** (no announcement, no logo, no "press start"). The creators of *The Beast* kept their identities secret until the game ended and even flashed *"This is not a game"* in red in the A.I. movie trailers.
2. **Don't define the game space** (no boundary — clues live in trailers, websites, phone numbers, real payphones; the player never knows where the edge is).
3. **Don't build a game** (build a *situation* players discover, not a quest they're handed).

**The crucial nuance that everyone gets wrong:** Players were *never meant to literally believe* "this is not a game." TINAG is **bait, not deception** — the phrase signals *"engage with this as if the stakes were real,"* a contract to treat the fiction seriously. The complementary term **TINAG-the-design-principle** is owned by the puppetmasters; the *feeling* is owned by players ([argn.com](https://www.argn.com/2005/04/this_is_not_a_game/)).

**Pitfall:** Treating TINAG as "trick my friends into thinking a ghost is real." They know you built it. If you lean on literal deception, the first person who catches the seams feels stupid and the spell breaks for everyone. The durable version is the **theatrical contract**: nobody breaks character, the in-world voice never winks, but everyone tacitly agrees to play along.

**→ Observance:** TINAG = **the Watcher never acknowledges it is a plugin.** No `[Server]` prefixes on its messages, no command output that reads like a feature, no "you triggered event #14." Signs, books, NPC dialogue, and the Discord companion all speak strictly in-world. The *one* rule for every authored surface: **would this sentence survive being read aloud by someone who believes the Deep Hold is real?** If it sounds like a game system, rewrite it. (This is the "don't define the game space" rule realized: the friends should never be able to point at the boundary between "the server" and "the Observance.")

---

## 2. Rabbit holes & trailheads — multiple, redundant entry points

**Source / who:** *The Beast* had **three overlapping rabbit holes** by deliberate design (Sean Stewart head writer, Elan Lee director). ([if50.substack.com deep-dive](https://if50.substack.com/p/2001-the-beast), [Wikipedia](https://en.wikipedia.org/wiki/The_Beast_(game)))

The three entries:
1. A hidden credit in the A.I. trailer/posters: *"Jeanine Salla — Sentient Machine Therapist."*
2. A phone number encoded in markings on promotional text.
3. A press-kit poster with a plain code: *"Evan Chan was murdered. Jeanine is the key."*

**Why redundancy mattered:** *"This redundancy ensured casual fans could stumble into deeper layers organically rather than requiring a single entry point."* If one trailhead is missed, two others still pull a player in. They also **pre-indexed fictional future websites in search engines** so that googling an in-world name (Jeanine Salla) *worked* — the trailhead self-completes the moment a curious player acts on it.

**The practitioner distinction (treasure-hunt theory):** a **trailhead/rabbit hole** is an *intended* entry the designer planted; a **rabbit hole** (pejorative) is an *unintended* tangent a player falls into that goes nowhere. The line: *"if any path leads away from the clue being easily understood and followed, the path you're on is probably not a clue."* ([mysteriouswritings.com](https://mysteriouswritings.com/armchair-treasure-hunting-whats-the-difference-between-clues-hints-red-herrings-and-rabbit-holes/))

**Pitfall:** One single, subtle entry point = most of your group never engages. The Beast's designers assumed players were sharp; they still built three doors.

**→ Observance:** Every arc beat needs **2–3 redundant trailheads** so no single missed login kills it. E.g. the "someone was here before us" reveal could surface as (a) a half-buried structure a builder trips over, (b) a written book left in a chest near spawn, AND (c) a Discord journal entry from the companion that name-drops a place. Plus a **standing rabbit hole at spawn** that's always discoverable for late-joiners (a permanent oddity — a sign that shouldn't be there, an NPC who says the wrong name). "Pre-indexing" maps to: when a friend acts on a clue (visits the coordinates, types the name into the Discord bot), **the world must already reward it** — the payoff is staged *before* they look, never generated on-demand in a way that feels reactive.

---

## 3. The Cloudmakers Problem — your puzzles are 10× too hard

**Source / who:** *The Beast*, week one. The single most-cited cautionary tale in ARG history. ([if50.substack.com](https://if50.substack.com/p/2001-the-beast))

Elan Lee, in his own words: *"These puzzles they'd probably never figure out until we broke down and gave them the answers. So we built a three-month schedule around this... The Cloudmakers solved all of these puzzles on the first day."* The team's response: *"After week one, we took all those documents and threw them away"* and pivoted to **reactive, drip-fed storytelling** — writing ahead of the players instead of gating behind locks.

The deeper structural finding: a hivemind (the Cloudmakers Yahoo group) is functionally a distributed supercomputer. The designers even canonized this by seeding a fake academic paper, *"Multi-person social problem-solving arrays considered as a form of artificial intelligence"* — acknowledging the crowd *is* the player.

**The inverse risk for us:** The Observance has **no hivemind.** A 10-person veteran group is the opposite failure mode — a puzzle that 10,000 strangers crack in 20 minutes might *never* fall to 4 sporadic friends. So both the Beast's lesson AND its inverse apply: **don't gate the story behind puzzle difficulty at all.** Story should advance on a schedule/showrunner cadence; puzzles are *flavor and texture that reward attention*, never the load-bearing skeleton (this is the central thesis of `ARG-RESEARCH.md` §1).

**Pitfall:** Building a "puzzle gauntlet" (the exact thing The Observance was being redesigned *away* from per the project memory). If progress requires solving X to unlock Y, a stuck friend group = a dead game with zero recovery.

**→ Observance:** **Decouple story progression from puzzle-solving.** The between-session showrunner advances the arc on a cadence (a new structure appears, the Watcher escalates) **whether or not** anyone solved last week's cipher. Puzzles unlock *bonus* lore/cosmetics/recognition, not the next chapter. When in doubt, make the puzzle 3× easier than feels right and put the difficulty in *noticing the puzzle exists*, not in cracking it.

---

## 4. Hard-but-fair puzzle design — the "aha" vs moon-logic line

**Source / who:** Kyle Foster (designer of the Portal/*Aperture Investment Opportunity* ARG), "Alternate Reality Game puzzle design," Game Developer. The most actionable primary source on the craft. ([gamedeveloper.com](https://www.gamedeveloper.com/design/alternate-reality-game-puzzle-design))

Concrete rules, verbatim where load-bearing:

1. **Encoded information must be recognizable AS encoded** — *"even if the actual encoding is a mystery."* Use **real-world systems** (Morse, SSTV slow-scan TV, MD5 hashes, barcodes, spectrograms) because players spot them as data-containers and can *research the real thing on Wikipedia*. Don't invent a homemade cipher with no name.
2. **"A correct solution must be obviously correct."** When you've solved it, you *know* — no interpretation. This is the litmus test that separates a fair puzzle from moon-logic.
3. **Researchability = fairness.** If a player can learn the technique from Wikipedia in ~30 minutes, difficulty is calibrated right. Genius leaps are not.
4. **Rate-determining step:** make several *easy* puzzles feed into *one* hard gate (like the slowest step in a chemical reaction sets the pace). Portal used quick Morse/SSTV wins that yielded clues for the genuinely hard MD5 bottleneck.
5. **Layered density:** put multiple difficulty levels in the *same* puzzle so everyone contributes — Portal had "find the radios" (easy), "locate signal points" (medium), "decode the audio" (expert). Skill gaps don't lock anyone out.
6. **Google-test everything.** Players *"plug straight into Google"* any interesting string. Run your clue text through search first; if it surfaces something unintended, change it.
7. **Chain verification:** in linked puzzles, each stage must *confirm* success before the next — otherwise *"a low percentage of a low percentage"* compounds toward zero solvers.

**Moon-logic defined:** a puzzle whose solution is "obvious in retrospect, but only if you guessed the right thing" (the ARG *Junko Junsui* is the cited offender — "facepalm" solutions). ([TV Tropes: Moon Logic Puzzle](https://tvtropes.org/pmwiki/pmwiki.php/Main/MoonLogicPuzzle)). Avoid: arbitrary anagrams, "read every third letter for no signposted reason," ciphers with no name, anything requiring you to think like the designer.

**Pitfall:** Domain-expert designers build expert puzzles. Foster's rule: *"something appearing super-advanced beats actually being advanced."* The *look* of difficulty is the reward; actual obscurity just locks people out.

**→ Observance:** Use **real, recognizable systems** Minecraft-natively: a sign in obvious cipher (book cipher referencing a specific in-world book; a substitution with the key findable in-world), coordinates in a spectrogram of a resource-pack sound, redstone/lever Morse, an enchantment-table-glyph (Standard Galactic Alphabet) message — SGA is *already a real recognizable system* players can look up. Every puzzle's answer must be **self-confirming** (it spits out coordinates that lead somewhere real, or a word that's obviously the password). Build a few trivial "noticing" puzzles per one genuine "work" puzzle. Pre-run every cipher string through Google so a friend doesn't accidentally find your dev notes.

---

## 5. Hint & curtain economy — how good ARGs rescue stuck players

**Sources:** Foster/Portal (layered + redundant paths), *The Beast* (reactive content), treasure-hunt clue/hint theory ([mysteriouswritings.com](https://mysteriouswritings.com/armchair-treasure-hunting-whats-the-difference-between-clues-hints-red-herrings-and-rabbit-holes/)).

The mechanisms real ARGs use to keep a stuck group moving:

1. **Redundant solution paths** — *"never make one solution path mandatory."* If players exhaust one avenue, alternatives exist; this *"prevents the hive-mind from mass-deadlocking."* (Foster). For us with no hivemind, this is survival-critical.
2. **Layered difficulty in one puzzle** — everyone finds *something*, so a hard core doesn't stall the whole group (Foster).
3. **Manual pacing gates** — the designer *unlocks the next stage manually after verifying the last is done*, which "prevents the game exhausting itself too soon" AND lets you **inject a hint when you see them stuck** without breaking the fiction (Foster, Beast). This is the operational backbone of a rescue economy.
4. **In-world hint delivery** — the rescue must arrive *in character*. The Beast pivoted to writing more content/character posts when players stalled; the hint came as story, not as a `/hint` command.

**The "curtain" (a distinct concept):** the curtain is the wall between authored game and reality — *who controls what.* Good ARGs **never visibly pull the curtain back during play**; the puppetmasters of The Beast *"never intervened directly in forums — everything players received they earned through investigation."* The curtain is revealed only at the *authored end* (or never — see §9 Cicada). Pulling it back mid-game ("ok guys here's the answer, signed: the dev") shatters TINAG.

**Pitfall:** No rescue economy = the game silently dies the first time 3 friends lose the thread, and you never even find out until weeks of dead air.

**→ Observance:** This is where you have a structural *advantage* over historical ARGs: **the Supabase dashboard + Discord companion + between-session showrunner = a live telemetry-driven rescue economy** that 2001 puppetmasters would have killed for. Concretely:
- **Stuck-detection:** track per-player/-group progress in Supabase (last clue touched, time since last advance). When the group is N days stalled on a beat, the showrunner fires an **in-world nudge**: the Watcher leaves a *new* book that's a softer restatement, an NPC volunteers a hint as dialogue, a structure subtly lights up. The hint is **always in character** (preserve the curtain).
- **Tiered nudges:** soft (atmosphere points at the answer) → medium (NPC says it more plainly) → hard (the companion's "journal" basically spells it out as if *it* figured it out). Escalate only on continued stall.
- **Never** let the bot emit `Hint: try X`. The curtain stays up. The Watcher/companion *knows things*; it doesn't *give hints*.

---

## 6. Multi-week pacing that survives irregular play

**Sources:** *The Beast*'s reactive drip-feed ([if50](https://if50.substack.com/p/2001-the-beast)); *I Love Bees*'s **210 timed payphone GPS coordinates over a 12-hour wave** as a designed escalation ([42 Entertainment](https://42entertainment.com/work/ilovebees), [Wikipedia: I Love Bees](https://en.wikipedia.org/wiki/I_Love_Bees)); serialized-storytelling craft (recap sequences, cliffhangers, weekly-beats-daily) ([Grokipedia: recap sequence](https://grokipedia.com/page/Recap_sequence)).

Key findings:
- **Waves of anticipation, not a constant stream.** Foster: feed content in *waves* — a puzzle burst, then manually unlock the next stage. *I Love Bees* literally orchestrated a timed wave sweeping the country. Peaks and valleys beat a flat drip.
- **"Weekly beats daily if daily is inconsistent."** Consistency of cadence matters more than frequency. A reliable weekly heartbeat the group can count on > sporadic bursts.
- **Recaps reduce cognitive load for asynchronous audiences** who "juggle multiple shows" — the explicit mechanism serialized fiction uses to let people rejoin after a gap.
- **The committed-tail truth:** *"the audience that makes it to the finale is smaller but highly committed."* Retention *rises* in late episodes among the invested. Don't design for everyone to finish; design so the 3–5 who stay get a real payoff.
- **Cliffhangers / unresolved mysteries / escalating stakes** create the urgency that survives gaps.

**The irregular-play problem (our hardest constraint):** veterans log in sporadically; weeks may pass with nobody on. A live, real-time ARG (payphones ringing on a schedule) is *impossible* for us. We need a **state-machine arc, not a clock-driven one** — progress is gated on *group actions reaching thresholds*, with a slow ambient escalation underneath so the world still *changes* between logins (so returning feels alive, not paused).

**Pitfall:** Designing a fixed real-time calendar ("Day 5 the ritual happens"). If nobody logs in on Day 5, the climax happens to an empty server. Equally bad: a totally static world that only moves when prodded — returning players feel nothing happened.

**→ Observance:** Hybrid pacing:
- **State-driven spine:** arc beats advance when the *group* (not the clock) crosses thresholds — the showrunner checks Supabase state between sessions and stages the next beat. Nobody misses the climax by being offline.
- **Ambient clock underneath:** a slow, low-stakes drift that *does* run on real time so the world feels alive on return — fog creeps a little, the Watcher's presence grows, a structure decays one stage. Cheap, automated, never gates the story.
- **The "previously on" recap:** when a player returns after a gap, the **Discord companion DMs a short in-world recap** ("While you were gone, the Hold went quiet, and the markings near spawn changed") — this is the recap-sequence mechanism, fixing the asynchronous-rejoin problem directly.
- **Weekly heartbeat:** one reliable in-world event per week (showrunner-fired) the group learns to expect — the cadence that beats sporadic bursts.
- **Design for the committed tail:** make the *finale* land for the 3–5 who stay, not gate it on full attendance.

---

## 7. Solo vs. collective design

**Sources:** *The Beast*'s casual-vs-Cloudmaker tension ([if50](https://if50.substack.com/p/2001-the-beast)); *I Love Bees*'s forced-collaboration payphone mechanic ([Wikipedia](https://en.wikipedia.org/wiki/I_Love_Bees)); Foster on collaborative-requirement design.

The central tension, from The Beast: *"It became increasingly difficult to be a casual Cloudmaker"* — *"virtually any new puzzle was solved before the majority of players had a chance to even see it."* The hardcore core outpaced everyone, and casual players got spectators' seats. *I Love Bees* solved this structurally: 210 payphones across the country **physically required hundreds of geographically-distributed people** to answer simultaneously — you *could not* solo it; the design *forced* collective participation and gave everyone a role.

Foster's principle: **design so key info must come from multiple sources/participants** — *"forcing collaboration prevents advantage hoarding."*

**Pitfall #1 (collective):** One sharp friend solves everything first and the rest spectate — the Cloudmaker problem at friend-group scale, where it's *worse* because there's no crowd to absorb it; the other 7 just disengage.

**Pitfall #2 (solo):** "Per-player, it knows your name" can isolate players into private threads that never knit into a shared story — everyone has their own mini-ARG and the group never converges on a campfire moment.

**→ Observance:** The "it knows your name" north star is **per-player texture on a shared spine** — not 10 separate ARGs. Concretely:
- **Distribute roles so no one friend can solo it.** Give different players different *fragments* (the Watcher whispers different lines to different people; one finds the cipher, another finds the key, a third the location). The reveal requires **pooling** — recreating I Love Bees' forced collaboration at LAN scale. This also defeats the "one sharp friend wins" failure.
- **Per-player hooks, collective payoffs.** The Watcher personalizes the *approach* (uses your name, references your base, haunts you specifically) but the *milestone reveals are shared* — everyone converges for the beat. Personal dread → communal "did you see that?!"
- **Asymmetric knowledge as a feature:** veterans love specialization. Make "the one who reads the glyphs" a *role* someone owns, so being the expert is a contribution, not a spoiler.

---

## 8. Red herrings done well — fair misdirection vs. unfair traps

**Sources:** mystery/treasure-hunt craft ([thewritepractice.com](https://thewritepractice.com/mystery-clues/), [mysteriouswritings.com](https://mysteriouswritings.com/armchair-treasure-hunting-whats-the-difference-between-clues-hints-red-herrings-and-rabbit-holes/), [storygrid.com](https://storygrid.com/red-herrings/)); the ARG-specific apophenia risk.

Rules for *fair* red herrings:
1. **A red herring is misdirection, not a lie.** You may misdirect *attention*; you may not *withhold* a piece the solver needs. *"We cannot hide information — but we can misdirect their attention."*
2. **Mix true clues among the herrings.** *"By mixing the meaningful with the incidental, you allow them to at least attempt to identify the important clues."* Pure noise with no signal = unfair.
3. **Don't over-emphasize a triviality.** *"Repetition or emphasis signals importance"* — if you repeat/highlight a detail, players *will* treat it as load-bearing. Emphasizing a dead end is a cheap-shot herring that breeds revolt.

**The ARG-specific danger — apophenia.** Communities of solvers see patterns *everywhere*; coincidence becomes "clue." In The Beast, **stock-photo reuse** (the same image for two characters) was spotted by players and the designers had to **retroactively canonize it** as "step-selfs" (robot duplicates) — turning an accidental herring into lore ([if50](https://if50.substack.com/p/2001-the-beast)). Cicada-style mysteries generate endless false "solutions" from over-reading.

**Pitfall:** Unintended red herrings are *more* common than intended ones in a hand-built ARG — every accidental coincidence (a misplaced block, a reused texture, an off-by-one coordinate) becomes a 3-hour group rabbit hole. At friend-group scale that can burn an entire session and sour the mood.

**→ Observance:**
- **Use red herrings sparingly and fairly:** an NPC who lies (and is *findably* established as unreliable), a decoy structure that's a dead survivor's failed attempt (canon, not noise) — herrings that are *themselves lore*. Never a pure dead-end with no payoff.
- **Audit for accidental herrings before each beat ships.** This connects to the project's existing *coherence/orphan audit* (COHERENCE-AUDIT). A reused schematic, a stray sign, a block that "looks placed" — playtesters *will* chase it. Run a "what would an over-reader latch onto here?" pass.
- **Have an "absorb-the-coincidence" escape hatch ready:** if the group fixates on an unintended pattern, the showrunner can *retroactively make it real* (the Beast's step-self move) rather than fight it — turn the bug into canon. The dashboard makes this easy: watch what they're theorizing about in Discord and feed it back.
- **Don't over-emphasize the wrong thing:** if you repeat/spotlight a detail, it *must* matter. Save emphasis for real clues.

---

## 9. The curtain — when (and whether) to reveal it is authored

**Sources:** *Petscop* (ambiguity-as-engine, never fully explains) ([Wikipedia: Petscop](https://en.wikipedia.org/wiki/Petscop), [SuperJump](https://www.superjumpmagazine.com/petscop-the-game-that-doesnt-exist/)); *Local 58* / Kris Straub (restraint, show-don't-tell) ([Dread Central](https://www.dreadcentral.com/editorials/534205/local58-and-the-birth-of-analog-horror/), [Wikipedia: Local 58](https://en.wikipedia.org/wiki/Local_58)); *Marble Hornets* (give *only* what's needed) ([Wikipedia](https://en.wikipedia.org/wiki/Marble_Hornets)); *Cicada 3301*'s deliberate non-resolution ([Wikipedia](https://en.wikipedia.org/wiki/Cicada_3301), [versus.com](https://versus.com/en/news/cicada-3301-the-internet-s-greatest-unsolved-puzzle)).

Two distinct meanings of "curtain" — both must be **authored decisions, not accidents:**

**(a) The horror/mystery curtain — how much you show.** The analog-horror canon is unanimous: **restraint is the engine.**
- *Petscop:* *"Items gained meaning by not being shown, because their ambiguity was more disturbing than reality."* Horror via implication, *"uncomfortable silences, things that didn't fully make sense"* — no jumpscares. *"Because Petscop refused to provide easy answers, the mystery only grew larger."*
- *Local 58:* *"The genius lies in its restraint... it relies on implication and suggestion, allowing the viewer's imagination to fill in the blanks."* Show-don't-tell; the terror is *"silent, always lingering."*
- *Marble Hornets:* *"never giving you anything more than what you absolutely need at any given point, laying out the story with remarkable precision."*
- The shared aesthetic goal (analog horror): make it *"feel discovered instead of presented"* — *"that viewers accidentally found something real and not meant to be seen."* (This is TINAG and environmental storytelling fused.)

**(b) The authorship curtain — whether you ever confirm "this was made."** *Cicada 3301* is the cautionary extreme: the trail *"did not end with a finish line, but with a book — the Liber Primus,"* still ~56 pages unsolved, no contact since 2014. The non-ending is *iconic* but it also left a generation of solvers with **no payoff and no closure** — a permanent "is this even real?" Mystery without *any* resolution becomes frustration; many quit.

**The synthesis:** withhold *information* to generate dread (curtain (a) stays mostly down forever), but author a *deliberate emotional resolution* (curtain (b) — the reveal that the lost people were *us*, or whatever the Deep Hold's gut-punch is) so the group gets closure even if mechanical mysteries stay open. Ambiguity is fuel; *total* non-resolution is abandonment.

**Pitfall:** Two opposite failures — (1) over-explaining (a lore-dump book that answers everything kills the dread instantly, the anti-Petscop), and (2) Cicada-style total non-answer (the group feels cheated, "we did all that for nothing"). A friend group will forgive open *details* but not a missing *emotional* payoff.

**→ Observance:**
- **Curtain (a) — never fully explain the Watcher/Deep Hold.** Imply, don't state. The most-disturbing facts stay *unshown* (what happened to the previous inhabitants is reconstructed from fragments, never narrated). Silence, near-misses, the wrong block in the corner of your eye > any monster reveal. This aligns with the project's "soft-pressure, not scripted" north star.
- **Curtain (b) — author ONE intended emotional gut-punch** the arc converges on (e.g. "the ones who were here before were friends like us, and they're still watching / we're becoming them"). Leave the *mechanics* ambiguous forever; resolve the *feeling* on purpose. The showrunner ensures the group reaches this beat (state-gated, §6) so nobody is left with Cicada-style nothing.
- **Restraint as a production budget hack:** show-don't-tell is *cheaper to build* than elaborate reveals — a single eerie sign + silence outperforms a custom boss cutscene. Lean into it.

---

## 10. Case-study quit-causes (what made participants leave) — quick-reference

| ARG | What made it *work* | What made participants *quit* | → Observance guardrail |
|---|---|---|---|
| **The Beast** (2001) | 3 rabbit holes, reactive drip-feed, character-driven stakes, TINAG | Casual players outpaced by Cloudmaker hivemind → spectators ([if50](https://if50.substack.com/p/2001-the-beast)) | Distribute fragments so no one friend solos it (§7) |
| **I Love Bees** (2004) | Forced collective payphone mechanic, timed escalating waves ([42E](https://42entertainment.com/work/ilovebees)) | Required physical real-world presence — high barrier | State-gated, in-server beats; no real-world logistics |
| **Cicada 3301** (2012–14) | Multi-media trail, real crypto, mystique ([Wikipedia](https://en.wikipedia.org/wiki/Cicada_3301)) | Expert-only crypto (excludes most); **zero resolution** (Liber Primus unsolved, vanished) → "is this real?" abandonment | Easy puzzles + authored emotional payoff (§4, §9b) |
| **Year Zero** (2007) | USB/spectrogram/phone trailheads, real-world props, cohesive dystopia ([Wikipedia](https://en.wikipedia.org/wiki/Year_Zero_(album))) | Tied to album cycle; wound down fast | Plan a graceful, authored *ending*, not a fade-out |
| **Petscop** (2017+) | Ambiguity-as-engine, slow dread, "feels discovered" ([SuperJump](https://www.superjumpmagazine.com/petscop-the-game-that-doesnt-exist/)) | Glacial/irregular updates; never resolved (frustration for some) | Reliable weekly heartbeat (§6); resolve the *feeling* |
| **Local 58 / Marble Hornets** | Restraint, show-don't-tell, "only what you need" ([Dread Central](https://www.dreadcentral.com/editorials/534205/local58-and-the-birth-of-analog-horror/)) | (Web series, not interactive — limited agency) | Borrow the restraint; add the interactivity Minecraft gives |

**The two universal quit-causes across all of them:** (1) **difficulty/access barriers** that exclude most participants (expert crypto, physical presence, hivemind-outpacing), and (2) **resolution failures** — either no payoff at all (Cicada) or a fade-out instead of an ending (Year Zero). The Observance's whole design must aim at *low barrier + authored emotional resolution.*

---

## 11. Synthesis — the 8 rules for a friend-group ARG

1. **Story is the skeleton; puzzles are texture.** Never gate arc progression behind a solve (Cloudmaker lesson, §3).
2. **Scale puzzles down ~10×, scale rescue up ~10×.** No hivemind = easy puzzles + a reliable in-world hint economy driven by Supabase telemetry (§4–5).
3. **TINAG is theatrical, not literal.** The Watcher never breaks character; no sentence may read like a game system (§1).
4. **Redundant trailheads (2–3 per beat) + a standing rabbit hole at spawn.** No single missed login kills a beat (§2).
5. **State-gated spine + ambient real-time drift + weekly heartbeat + companion "previously on" recaps.** Survives irregular play; nobody misses the climax (§6).
6. **Per-player texture, collective payoff. Distribute fragments so it can't be soloed.** "It knows your name" on a shared spine (§7).
7. **Red herrings only as lore; audit for accidental ones; keep an absorb-the-coincidence escape hatch.** Apophenia is the friend-group time-sink (§8).
8. **Restraint forever on the mystery; author exactly one emotional resolution.** Ambiguity is fuel, total non-resolution is abandonment (§9).

---

### Sources (primary/authoritative)
- Elan Lee's three rules & TINAG: [argn.com](https://www.argn.com/2005/04/this_is_not_a_game/), [Wikipedia: ARG](https://en.wikipedia.org/wiki/Alternate_reality_game)
- The Beast deep-dive (Cloudmaker problem, 3 rabbit holes, step-selfs): [if50.substack.com](https://if50.substack.com/p/2001-the-beast), [Wikipedia: The Beast](https://en.wikipedia.org/wiki/The_Beast_(game)), Sean Stewart's site seanstewart.org
- ARG puzzle design (hard-but-fair, moon-logic, layering, Google-test): [gamedeveloper.com](https://www.gamedeveloper.com/design/alternate-reality-game-puzzle-design), [TV Tropes: Moon Logic](https://tvtropes.org/pmwiki/pmwiki.php/Main/MoonLogicPuzzle)
- I Love Bees (payphones, waves, forced collaboration): [42entertainment.com](https://42entertainment.com/work/ilovebees), [Wikipedia](https://en.wikipedia.org/wiki/I_Love_Bees)
- Year Zero (trailheads, props): [Wikipedia](https://en.wikipedia.org/wiki/Year_Zero_(album)), [nin.wiki](https://www.nin.wiki/Year_Zero_Numbers)
- Cicada 3301 (non-resolution, expert crypto): [Wikipedia](https://en.wikipedia.org/wiki/Cicada_3301), [versus.com](https://versus.com/en/news/cicada-3301-the-internet-s-greatest-unsolved-puzzle)
- Petscop / analog-horror restraint: [Wikipedia: Petscop](https://en.wikipedia.org/wiki/Petscop), [SuperJump](https://www.superjumpmagazine.com/petscop-the-game-that-doesnt-exist/)
- Local 58 / Kris Straub (restraint, show-don't-tell): [Dread Central](https://www.dreadcentral.com/editorials/534205/local58-and-the-birth-of-analog-horror/), [Wikipedia](https://en.wikipedia.org/wiki/Local_58)
- Marble Hornets ("only what you need"): [Wikipedia](https://en.wikipedia.org/wiki/Marble_Hornets)
- Red herrings / clue-vs-rabbit-hole craft: [thewritepractice.com](https://thewritepractice.com/mystery-clues/), [mysteriouswritings.com](https://mysteriouswritings.com/armchair-treasure-hunting-whats-the-difference-between-clues-hints-red-herrings-and-rabbit-holes/), [storygrid.com](https://storygrid.com/red-herrings/)
- Serialized pacing / recaps for asynchronous audiences: [Grokipedia: recap sequence](https://grokipedia.com/page/Recap_sequence)
