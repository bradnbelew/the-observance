# THE OBSERVANCE — OPENING RE-SEQUENCE + PLAYTHROUGH FRAMING (the vibe audit)

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT IMPLEMENT OR SHIP.** Current C01 opening order is in the V5 node and experience manifests.

> 2026-07-02. Ethan's mandate: the game must play like a **mysterious world you're slowly haunted into
> investigating**, NOT "walk up to a structure and go 'oh, a puzzle.'" You feel *watched* first; you get
> *curious*; you *dig*; clues *connect*; "oh — there are coordinates in this"; you go there; you *figure
> out* the next thing — unhandheld but fair. Puzzles/ciphers exist but are **submerged**, discovered, not
> announced. This doc audits the current opening against that vibe and re-sequences it, grounded in
> `RESHAPE-RESEARCH.md` + `design/research/*` (what makes ARGs great · MC-integration technique · puzzle
> variety) and the live-code state. Companion to `THE-RESHAPE.md` (the 6 principles) and the WAVE-S build.

---

## 1. THE TARGET (the vibe, said precisely + why it works)

The felt arc of a great ARG cold open (cited): **normal baseline → un-narrated wrongness → the player
becomes the detective hunting the source.** Analog horror (Local 58 / Kane Pixels) corrupts a *trusted
format* so no one has to say "this is a game" (`arg-craft §4`). *From The Fog* ranks the #1 dread beat as
**"a block change discovered out of sight"** — a torch gone from your base, your door open, a gift in your
chest, found *after the fact*: proof-of-presence *without* presence, and it retroactively violates the one
place you felt safe (`mc-arg-genre §1.2/§2`). Tunic/Fez/Outer Wilds: **the lock is comprehension, not a
gate** — the world is written in marks you can't read yet, and "these are letters" is the first big turn
(`RESHAPE-RESEARCH §1`). The Beast seeded **three redundant trailheads** so no missed clue locks anyone out
(`arg-craft §2`). That is the target: **haunt → curiosity → investigation → earned literacy → theory.**

---

## 2. THE AUDIT — current opening vs the vibe (live-code grounded)

### 2a. What already SERVES the vibe (keep + promote to the lead)
- **The haunting primitives exist + are wired:** per-player `sendBlockChange` (RevealBeat), `showEntity`
  apparitions, per-player positional sound (`soundAt`), particles/title/actionbar, `NameOnWallBeat`
  (your name in runes only you see), `ReflectionBeat`, the Lens. Ambient **silent world-mutation** beats —
  `DecayCreepBeat`, `DoorOpenBeat`, `TorchGutterBeat`, `RoomSwapBeat` — are exactly the *From-The-Fog*
  "wrongness discovered out of sight." **These are the gold and they should LEAD.**
- **The Observer Tier-0 is LIVE:** `reports.ts`/`reports.run.ts` reads real behavior (hoards / reads /
  wanders / night-walks / stays-in-dark) and speaks a grounded, rate-limited implication in the Watcher
  register. "It noticed you" works *today* with zero LLM.
- **Restraint is the engine:** the showrunner is ~90% quiet; pre-ignition it suppresses the curatorial drip.
  The world *can* stay silent and let attention do the work — the precondition for a slow burn.
- **The cold-start-prologue DESIGN is right** (`design/ideas/cold-start-prologue.md`): a single findable
  anomaly (a lit marker in *their own base* that "wasn't there yesterday") → someone screenshots it →
  ignition → the knob drops back to quiet. This is the haunting-first opening — on paper.

### 2b. What CONTRADICTS the vibe (the real gaps to fix)
- **G1 — the opening LEADS with a visible puzzle field.** Live flow (`RUNBOOK §2/§4`, `FIRST-PLAYTEST §2`):
  operator runs `/observance placeregion` → stamps the rune ring + **six keeper set-pieces in a cluster on
  flat ground**, then the arc ignites on **right-clicking the ring and reading it**. A player joining sees
  *structures first* and reads a *rune ring* first — the exact "oh, puzzles" you reject. **This is the #1 fix.**
- **G2 — the haunting is garnish, not the lead.** The per-player illusions + Observer + ambient beats fire
  as *ambient texture after* ignition, on the showrunner's low budget. Nothing sequences them to **carry the
  first session** as the reason to investigate. The order is inverted.
- **G3 — structures are PRESENTED, not DISCOVERED.** `placeregion`/`placedeep` stamp all set-pieces at once,
  operator-run, visible, in a batch (`ObservanceCommand`). There's no "buried / far / hidden / revealed-by-
  the-haunting" placement. The world hands you the puzzle rooms instead of making you *find* them.
- **G4 — the cold-start-prologue is only half-wired.** Built: the pure `prologue.ts` policy + the drip
  suppression. **Gaps** (`cold-start-prologue §7`): the `IgnitionListener` that fires on the base anomaly,
  the lit-marker placement + base-retarget, the snapshot ignition inputs, the one-shot ack emission, the
  `recordOpenedNamed` voice key. So the *designed* subtle opening doesn't actually run; the *blunt*
  placeregion opening is what's live.
- **G5 — the first "puzzle" is a handed-over cipher.** Reading the rune ring is beat one. Even post-reshape
  (literacy now earned via cribs), the *ring as the opening act* still frames the experience as "decode
  this," not "notice something's wrong." The cipher should be **found deep inside the investigation**, not
  the front door.

### 2c. The one-line verdict
**The tech is 90% there; the SEQUENCING and the PLACEMENT are the reshape.** Lead with the haunting, hide
the structures, let investigation surface them, and push the first cipher deep. No new engine required — a
re-order + a placement mode + wiring the four prologue gaps.

---

## 3. THE RE-SEQUENCED COLD OPEN — session one, beat by beat (haunting-first)

> Principle: **spend zero "here is a puzzle" signals in the first session.** Spend *wrongness* instead.
> Every beat below maps to a built primitive (or a small wiring gap) + a cited technique.

**Pre-arc — the world is just… off (no structures presented).** The friends join a server that looks like a
normal (slightly old, slightly abandoned) world. **No keeper field at spawn.** The server icon is the rune
ring (a silent rabbit hole; `cold-start-prologue §1.2`). The bot sits in Discord as "The Watcher — watching
the ways," saying nothing. Baseline established (analog-horror rule: establish normal, *then* decay —
`arg-craft §4`).

**Beat 1 — the first wrongness, discovered out of sight (the hook).** Over the first session, **one** small
impossible thing happens to the group in their own space, found *after the fact* (never witnessed mutating):
a torch they placed is gone / a door they closed is open / a single block is the *wrong age* (un-oxidized
copper where all else weathered) / a lit marker sits in their base that "wasn't there yesterday." Built via
`TorchGutterBeat`/`DoorOpenBeat`/`RevealBeat`/`DecayCreepBeat` under reveal-discipline. **This is *From The
Fog* dread #1** — proof something was here while they were away. It is the screenshot-to-Discord trigger =
**ignition, caused by a player, not the bot** (`cold-start-prologue §1.2`). *Redundant trailheads
(`arg-craft §2`):* stack 2–3 (the base anomaly **and** a per-player whisper **and** the Observer naming a
habit) so at least one lands.

**Beat 2 — it knows *me* (the escalation from "weird" to "watched").** Once curious, the world gets
*personal* but stays quiet: the Observer names a real habit (`reports.ts`, live — "you keep one thing you
never use"); your name appears in a script you can't read on a wall only you see (`NameOnWallBeat`); a
whisper plays *only when you're alone*, positioned *behind* you (mono-OGG spatial, `mc-pack-fog-sound §4`);
your light dims when something has line-of-sight to you (`PerPlayer.dimLight`, needs wiring). No jump-scares,
no text dumps — it *states and goes quiet* (the restraint that makes silence read as *watching*, not
*absent* — `cold-start-prologue §2.1`).

**Beat 3 — the marks resolve into a language (the first turn, not the first gate).** They've been seeing
runes as "decoration" the whole time (server icon, the wall-name, a carving on the base anomaly). Through the
cribs (a rune-word beside the thing it names — WAVE-S), literacy *accretes* until the epiphany: **"these are
letters, and they've been readable this whole time"** (Tunic/Fez, `RESHAPE-RESEARCH §1`). The rune ring is
**not** shown to them as beat one — it's a thing they *seek out* once they can read, or find on the base
anomaly. The alphabet is earned, never taught.

**Beat 4 — the investigation surfaces the first place (structures discovered, not presented).** A clue — a
whisper, a coordinate hidden in a found journal (your artifact, §7), the recovery-compass/lodestone needle —
points *out into the world*, and they travel to it and **find** the first keeper site (buried / off-path /
revealed by the haunting), rather than it being stamped in a field at spawn. Arriving, they don't get a
"solve me" sign — they get a *place* that reads wrong (the altar rebuilt wrong, the hearth with no fire) and
have to *figure out what they're looking at* (`THE-RESHAPE §2`, environmental storytelling).

**Beat 5 — the record answers back (the surface-hop + the "oh").** Something they do or find in-world
cashes out on **the record website** (which they discover via a URL hidden in-game) — and it *knows
something* (a name, the count, a fate). The cross-surface synthesis (in-world + record only meaningful
*combined*, I Love Bees; `arg-craft §2`) is the first "this is bigger than the Minecraft server" moment.

> **After session one, the group is a group that is *investigating* — which is the precondition the whole
> downstream engine assumes** (`cold-start-prologue §6`). We spent wrongness, not spectacle, to buy it.

---

## 4. THE WHOLE-PLAYTHROUGH FRAMING + PACING (weeks, no dead air)

- **Mystery-primary spine, ciphers submerged.** The spine is *build a theory of what happened to the Deep
  Hold and reach the living Seventh* — the ciphers are **how you gather evidence**, found deep inside the
  investigation, never the front door (`THE-RESHAPE §4`, reward-the-theory). The letter-ciphers stay a
  **minority**, each now *characterizing* its keeper (WAVE-S cipher-as-inversion), each earned via
  behavior/observation, well-spaced (`PUZZLES §0`).
- **A redundant WEB, not a chain (Three-Clue Rule).** Every conclusion reachable by ≥3 independent clues
  (in-world mark · record fragment · Observer line · found artifact) so a two-week gap where nobody logs on
  can't ghost the reveal, and the web self-heals (`arg-craft §2`). The **seven-motif** (WAVE-S) is the
  connective tissue that makes distant keepers *rhyme* — the "Vaun's thing and Sella's thing are the same
  thing" leap (`RESHAPE-RESEARCH §6`).
- **Waves + a weekly heartbeat, state-gated not clock-gated.** Progress advances when *group actions* cross
  thresholds (the showrunner reads Supabase state), with an ambient real-time drift underneath (fog creeps,
  sculk spreads, decay clocks tick) so the world feels alive between sessions (`arg-craft §6`). A "previously
  on" recap when a player returns. The salience picker (WAVE-S S-F) already surfaces the thread the group is
  pulling on; roster-aware so it never surfaces a convergence beat to too-few players.
- **Reward the theory (Obra Dinn).** The record "receives" a keeper's *fate* only when a **cluster** of
  their evidence is coherent (WAVE-S S-D, built) — you commit a reasoned theory, you don't type a decode.
- **Layered + false facts, recontextualized.** Confirmed/implied/misleading/false as *whole believable
  documents* (WAVE-S R2 herrings: "the seventh was spared," "the faithful are kept") — the best twists
  *reframe* earlier material without invalidating it (the re-read IS the fairness test; `RESHAPE-RESEARCH §6`).
- **Design for the committed tail.** The finale audience is the 3–5 who stay; escalate stakes + cliffhangers
  so motivation survives the gaps (`arg-craft §6`).

---

## 4B. THE ARC IS A FIELD, NOT A SPINE (diversity · offshoots · parallel + in-between threads)

> Ethan: *"more than just solve keeper stones → finale → the end. more diversity, off-shoots, parallel
> things, in-between things."* The current build reads as one line (6 stones → catch → descent → Seventh →
> Accepting). The research says a rich mystery is a **field of simultaneous curiosities**, not a corridor.
> This section turns the corridor into a constellation. Grounded, concrete, and mostly *reframing +
> surfacing content that already exists* + a few new threads.

### 4B.1 The model: Outer Wilds' field + Animal Well's three layers
- **A field of open threads, entered in any order (Outer Wilds Ship Log).** *"The biggest verb is
  observation"*; the player pulls whatever curiosity has momentum, and the log shows a *web of leads*, not a
  quest arrow (`RESHAPE-RESEARCH §1`). Our OVERHAUL Pillar 2 already specs this ("each keeper is a
  **constellation** of 3–5 storylets, enterable in almost any order; multiple threads open at once") — it's
  designed, under-built. **Make the six keepers a FIELD**: several live threads at once, the salience picker
  (S-F, built) surfacing the one with momentum, no forced order beyond the few genuinely-sequential gates.
- **Three delineated layers (Animal Well), so depth is opt-in not a wall (ESA's caution).** Design three
  audiences at once, clearly separated so a stuck group always has a fair main path (`RESHAPE-RESEARCH §5/§E`):
  1. **The spine** — the fair, completable investigation (reach the living Seventh, correct the record).
  2. **The secret layer** — optional deep lore + extreme-creativity puzzles for the obsessives, that
     *recontextualize* the spine (the Nether origin lane, the End exile lane, the "6 prior groups," hidden
     reveals).
  3. **The community/ARG layer** — things *deliberately too big for one player* (the asymmetric co-op vault,
     cross-session drifts, the record they collectively restore) — Cicada/I-Love-Bees at friend-scale.

### 4B.2 The threads (a constellation, not a list) — what runs in parallel
Concrete threads, most already seeded; the reshape *surfaces + grows + interconnects* them:

- **The six keeper constellations (parallel, any order).** Each keeper = a small self-contained mystery
  (their journals, their site, their way-they-broke, their cipher-as-inversion) you can investigate in any
  order — a field, not stops 1–6. *Built content; needs de-linearized surfacing.*
- **The Liar-and-the-Catch (a spine that cuts across the field).** Iss's warm lie → the cold re-read →
  everything recontextualizes. The one thread that reframes the others (`RESHAPE-RESEARCH §6`). *Built.*
- **The Wren betrayal (a parallel human arc, present-tense).** A companion you trust, who is feeding you to
  the Watcher — the emotional offshoot that pays off "how does it know sharp things about us." *Built (S-D
  era); grow the trust→crack→reveal→reckoning drip.*
- **The record's own mystery (a documentary offshoot).** The half-corrupted record website is a *thread you
  investigate*: falsified entries (the Iss lie), the seven-motif count, the fair red herrings ("the seventh
  was spared," "the faithful are kept"), restoring the true record. A parallel paper-trail mystery that
  rewards the record-diggers. *Built (R2 + S-D dovetail); frame it as a thread, not a status page.*
- **The townsfolk secrets (human-scale offshoots).** Aro/Wenna/Coll/Dob/Old-Pell are now interactive NPCs
  (S-G). Give 2–3 of them a *small mystery of their own* — Aro the rumor-broker who lies (which rumor is
  true?), Old Pell who won't go down and *remembers* — parallel human threads that don't feed the keeper
  spine but deepen the world and occasionally *rhyme* into it. *NPCs built; the mini-threads are new-light content.*
- **The "six downloads / prior groups" thread (a slow-burn meta-mystery).** The `kept: 6` on the lure page,
  the six who came before — a thread that re-reads across movements ("six downloads = six keepers = six prior
  groups the record kept the same way it's keeping us"). *Designed (cursed-map ledger #24); surface it.*
- **The Nether origin lane + End exile lane (optional deep offshoots).** Where the kept-light came from; where
  the Seventh was cast out first. Non-gating deepening that makes the finale land harder (`OVERHAUL D-NE`).
  *Seeds staged; optional.*
- **Emergent per-player offshoots (the Observer spins them).** Your specific habit becomes a small personal
  thread the world develops (the hoarder gets a hoarding beat that curdles; the night-walker gets watched in
  the dark). Grounded in Tier-0 behavior (`reports.ts`, live). *Live; author more per-habit threads.*
- **The breadth field (20+ travel destinations + dead-leads-with-teeth).** Already parallel, gates-nothing —
  atmosphere, lore, the anti-speedrun tax. *Built; keep as the ambient field between the big threads.*

### 4B.3 The in-between (connective tissue so the field feels like one living world)
- **Cross-thread rhymes (the motif web).** The seven-motif + repeated symbols/phrases make a side thread
  suddenly connect to the spine — "the townsperson's rumor and Sella's copybook are the same number." The
  "oh, this offshoot was load-bearing" is the highest-value in-between beat (`RESHAPE-RESEARCH §6`).
- **Ambient world-drift between sessions.** Copper oxidizes, sculk spreads, fog creeps, decay accumulates —
  the world *ages on its own* so returning feels like time passed (`RESHAPE-RESEARCH §8`). *Beats built
  (DecayCreep); add the slow diegetic clocks.*
- **"Previously on" + reactive callbacks.** A returning player gets a recap; the world references what they
  did last session (Nemesis-system "build on what they did"; `RESHAPE-RESEARCH §7`). *Partly built (reports).*
- **Relief / exhale beats (pacing).** After a climax, a kinder keeper memory or a small true gift — the
  "ways that companied the Dark" — so weeks of dread don't fatigue (`OVERHAUL Pillar 2`, Warm-Grief). *Speced.*
- **Convergence beats (the community layer punctuation).** The asymmetric co-op vault + the Accepting — the
  few moments the whole active roster must pool, roster-aware so they never dead-air (S-F). *Built.*

### 4B.4 How it stays fair + coherent (not sprawl)
- **Three-Clue redundancy per conclusion** so parallel threads don't create dead-ends; the web self-heals.
- **The 3 layers are *delineated*** (ESA's caution) — the spine is always completable without the secret/
  community layers; offshoots are clearly optional so a stuck group never mistakes deep water for the path.
- **The consistency principle** — every new offshoot gets its story + clue + interaction in lockstep; no
  inert thread costuming as load-bearing. Offshoots may be pure honest flavor (Ethan's "pure lore is good"),
  but they must *read* as what they are.
- **Salience + roster-awareness** (S-F) keeps the field from overwhelming: the showrunner surfaces the one
  thread with momentum, never a convergence beat below quorum.

---

## 5. THE OBSERVER / "IT KNOWS YOU" — the haunting brain (its role, ramped)

- **Tier 0 — the land notices (LIVE, always-on, no LLM/consent cost).** `reports.ts` behavior implication +
  per-player illusions. This *carries the cold open* (Beat 2) and never stops. Ambient, never names/quotes.
- **Tier 1/2 — it quotes you (the ramp, via the companion channel).** Sharp, real-word/plan callbacks are
  the diegetic payoff of Wren's betrayal (grounded: only real observed things; degrade to silence, never
  fabricate — `RESHAPE-RESEARCH §7`). This is the **scope decision** still open (LLM brain + the
  `0008_observations` table) — the arc works at Tier 0; Tier 1/2 is the "it hears me" heavy lift.
- **The discipline that makes it story not gimmick:** tied to something they actually did · escalated/paced
  not a one-off · reconnected to the fiction (Nemesis-system rule, `RESHAPE-RESEARCH §7`). Rare = uncanny;
  frequent = spam that breaks the spell.
- **Delivery techniques to add** (all cited, mostly built): mono-OGG spatial whisper behind you; datapack
  **biome `mood_sound`** so the world self-generates dread with zero plugin code (`mc-pack-fog-sound §6`);
  display-entity apparitions (billboard TextDisplay / scaled ItemDisplay head), per-player; a vanilla mob as
  the Watcher (AI off, silent, invisible+glowing) discovered *already there* (`mc-build-visualize §7`).

---

## 6. PUZZLES SUBMERGED — discovered, not announced (the variety, applied)

The variety already shipped (~70 puzzles across the full matrix). The reshape here is **framing + placement**,
not more puzzles:
- **No "solve me" surfaces.** Strip the announcement (WAVE-S label cull, done). A puzzle should read first as
  *a thing that is wrong* (a hearth with no fire), and only reveal it's solvable once you've understood it.
- **Real-world, self-confirming systems only** (fairness): Morse, spectrograms, a book-cipher with a *named*
  book, substitution with an *in-world crib* (Chants of Sennaar). No homemade unnamed ciphers. When you've
  solved it you *know* — a coord that leads somewhere real, a word that's obviously right (`arg-craft §4`).
- **Lead with the non-cipher verbs** (observe · count · travel · listen · reflect · perform · combine) so
  "solving" rarely feels like "decode." Save letter-ciphers for the deep, earned, well-spaced beats.
- **Surface-hop chains** (in-world → record → external → back) so no single screen holds the game
  (`PUZZLES §2`) — and cross-surface locks that only cash out combined (I Love Bees).
- **The fairness rail** (so hard ≠ punishing): Three-Clue redundancy · the retrace test on every leap ·
  GUMSHOE "the core clue is free, the interpreting is the game" · anti-hollow-victory (the *reconstruction*
  is the reward, never a brute-forceable lock) · the escalating hint log as the always-available valve.

---

## 7. WHERE YOUR REAL ARTIFACTS SLOT (the content shot-list)

Real content beats AI for the hero artifacts because the "oh wow it actually has coordinates" only lands if
the data is *really there to extract*. Highest-value things for you to make, mapped to the beats:

1. **A found journal / recovered document** (photo/scan/screenshot) — the Beat-4 artifact that points to the
   first place. Hide real data *in the file*: an acrostic (first letters), a coordinate in the margin,
   zero-width characters, **EXIF GPS**, a filename payload. → its coords lead to the first keeper site.
2. **A found-footage video** (unlisted YouTube, degraded/recovered-looking, NOT a trailer) — surfaces on the
   record for a mid/late beat; a detail hidden in a frame (a sign, a word, coords). The Seventh in the dark,
   waiting, is the canonical one.
3. **A steganographic image** — LSB-hidden text, or an image whose **audio spectrogram** shows a word/coords.
   Slots as a record attachment or a Drive "recovered archive" file.
4. **An audio artifact** — a voicemail / distorted recording with a real spectrogram or backmasked message.
5. **Screenshots** that read like a real leak/recovery (a corrupted terminal, a half-deleted post).

Division of labor: **you make the artifacts that reward digging; I build the world that haunts them into
looking + the connective tissue** (the in-world wrongness that points at the file · the record that
references it · the coord that leads back in-world · the flag that flips when they crack it). I can also do
the Minecraft-side generation (degraded builds, datapack fog/sound, the rune font incl. the `illageralt`
fallback) and the wiring.

### 7a. FIRST HERO ARTIFACT (chosen: a found-footage video) — the production brief
Ethan's pick. A **"recovered recording"** — degraded, found-not-produced, ~30–90s, that surfaces on the
record website as a single recovered-file entry and reads as *unearthed*, never a trailer. Spec:
- **Form + look:** unlisted YouTube (or a hosted `.mp4`). Handheld/static-cam or "screen recording of a
  recording"; heavy degradation (VHS tracking lines, dropped frames, dead-air audio, timestamp burn-in that's
  *wrong*). No music, no titles, no editing polish. The *format is the trusted-thing-corrupted* (analog
  horror, `arg-craft §4`). Silence + one wrong detail beats spectacle.
- **Content (two viable slots — pick by where you want it to land):**
  - *Early cross-surface "oh" (mid session 1–2):* a recovered recording from **a prior group's last session**
    — mundane base-building footage, then one impossible frame (a block/door/marker changes on camera, or the
    Watcher's mark on a wall), then it cuts. Establishes "this happened to someone before us."
  - *Finale lead-in (canonical, `PUZZLES §6.6`):* **the Seventh, in the dark, waiting** — a figure far down,
    a slow pan, a held breath. The emotional gut-punch that points at the reunion.
- **What to hide (the payload — pick 1–2, I'll wire the check):**
  - **Visible coordinates in one frame** — an XYZ on a sign/carving/F3 overlay, on screen for <1s (rewards
    frame-by-frame). → leads to an in-world place. *Answer_kind: coords (arrival) or a token found there.*
  - **A word/name burned into a corrupt frame or spelled by the timestamps** → typed answer.
  - **A spectrogram payload in the audio** — a word/coords visible only in the audio spectrogram (rewards
    "what's a spectrogram?"). *Answer_kind: phrase.*
  - **EXIF / description-field / filename payload** on the hosted file (the datamine reward, D7).
- **Where I wire it:** an in-world haunting beat (a whisper, the record) surfaces the URL; the hidden payload
  resolves to an in-world place or a typed token; solving flips a flag that advances a thread. Retrace-fair:
  once solved, the trail is traceable. **Assist offer:** I can generate a *draft* found-footage clip via
  HyperFrames (the tooling's installed) as a starting point or placeholder, so the beat is wired and testable
  before your real version lands — your real one drops in when ready.

---

## 8. THE BIG CALLS (decide before build) + build implications

The re-sequence is **mostly a re-order + a placement mode + wiring four known gaps** — not new engine. The
decisions:

- **C1 — Kill the placeregion-field opening.** Replace "stamp six visible set-pieces at spawn" with
  **haunting-first + discovered placement**: (a) wire the cold-start-prologue gaps (IgnitionListener on a
  base anomaly, lit-marker placement, snapshot inputs, ack, `recordOpenedNamed`); (b) add a **hidden/gated
  placement mode** so keeper sites are buried/far/revealed-by-progress, found through investigation, not
  stamped in a field. *(Recommended — this is the whole point.)*
- **C2 — Sequence the haunting to LEAD session one.** A first-session "wrongness" schedule (Beats 1–2 above)
  that fires the built illusions/ambient beats *as the opening*, before any structure or cipher. A small
  showrunner opening-cadence + a couple of wiring hooks (dimLight, the base anomaly).
- **C3 — Push the first cipher deep.** The rune ring stops being beat one; literacy is earned via cribs and
  the first real cipher lives inside the investigation. *(Mostly done by WAVE-S; this is a placement/framing
  confirm.)*
- **C4 — Observer Tier 1/2 scope.** Ship the cold open on **Tier 0** (live). Decide separately whether/when
  to build the "it quotes your real words" Tier 1/2 (LLM + `0008_observations` + voice) — the heaviest lift,
  not needed for the re-sequenced opening to land.
- **C5 — First hero artifact = a found-footage "recovered recording" (chosen).** Brief in §7a. You produce
  the real one; I wire the beat (and can drop a HyperFrames draft in first so it's testable). Open sub-calls
  for you: which SLOT (early prior-group recording vs. the Seventh finale lead-in) and which PAYLOAD (visible
  coords / a word / spectrogram / EXIF).
- **C6 — Field-not-spine (Ethan's expansion, §4B).** Build the arc as a constellation: de-linearize the six
  keepers into a parallel field (salience already surfaces them), grow the offshoots (townsfolk secrets, the
  record-as-a-thread, the "6 prior groups," Wren, per-player emergent threads), add the in-between connective
  tissue (motif rhymes, world-drift, "previously on," relief beats), and delineate the 3 layers (spine /
  secret / community) so depth is opt-in. *(Recommended — this is what makes it a world, not a corridor.)*

**Nothing here is built yet — this is the REFINED audit + design for your review (per "refine then build").**
Mark it up; then the build order, each a green wave:
1. **C2** — sequence the haunting to lead session one (fire the built illusions/ambient beats as the opening)
   + the base-anomaly hook. *The feel-changer; do first.*
2. **C1** — kill the placeregion-field opening; add discovered/hidden placement + wire the 4 prologue gaps.
3. **C6** — de-linearize the keepers into a field + surface 2–3 offshoots + the in-between tissue.
4. **C5** — wire the found-footage cross-surface beat (HyperFrames draft → your real clip).
5. Ripple the framing across the spine; grow the secret/community layers.
C3 (first cipher pushed deep) is mostly done by WAVE-S; C4 (Observer Tier 1/2) stays a separate scope call.
