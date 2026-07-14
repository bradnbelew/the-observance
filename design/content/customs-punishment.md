---
id: customs-punishment
title: discover-by-punishment — the seven ways, their tolls, their teaching surfaces
kind: design/draft (content authoring; NOT live code)
status: SUPERSEDED_PRE_V5_ARCHIVE_DO_NOT_IMPLEMENT
movement: I→V (the loop runs the whole descent; tolls cluster in I–III where the ways are being learned)
grounded_in:
  - arc/WORLD-BIBLE.md                         # §5 the seven ways; the Dark takes the unlit/unwitnessed
  - discord/src/forge/canon.ts                 # CUSTOM_KEYS (the_-prefixed), KEEPERS, THREADS — the closed registries
  - discord/src/voice.ts                       # the Watcher register + CUSTOM_PHRASES; new keys land here
  - plugin/.../signal/TrackerConfig.java       # the transgression each way already tracks
  - plugin/src/main/resources/sites.yml        # the teaching-surface site ids (all 19 are enabled)
  - discord/supabase/migrations/0005_threads.sql   # punishment_state schema this seeds FOR
  - design/INTEGRATION.md §3.2                  # the 5-stage loop; honorless-way relief rule (INV-8)
  - arc/corpus/official-records.md             # R01 founding ordinance, R02 lamp-notice (the standing law)
  - arc/corpus/npc-and-watcher-voice.md        # SET A Wenna (accidental Rosetta) + SET B Watcher register
  - arc/corpus/letters.md                      # L03/L05 the ways taught keeper-to-keeper
custom_keys_used:   [the_kept_light, the_deep_line, the_dark_hours, the_offering, the_bow, the_unspoken, the_sacred_beast]
thread_keys_used:   [who, place, happened, surface, human]
keepers_referenced: [vaun, mara, sella, orin, brann, iss]
---

# discover-by-punishment — the seven ways

> **2026-07-05 audit note:** this proposes 14 bespoke per-way voice keys (`tollKeptLight` /
> `keptKeptLight` / etc.). The LIVE system (`discord/src/showrunner/customs.ts` + `voice.ts`'s
> `CUSTOM_PHRASES` map) is a simpler generic 3-rung ladder (observed/warned/left) shared
> across all 7 ways, not per-way bespoke lines — none of the 14 proposed keys exist in
> `voice.ts` today. That's consistent with this file's own `status: DRAFT` / `not_to_edit`
> front-matter (a proposal awaiting integration, not a claim about what's live) — flagging
> only so a reader treats the live 3-rung ladder as current behavior, not this doc's richer
> proposal, unless/until someone actually integrates it.

> The loop the player must be able to walk WITHOUT being told the rule: a small, deniable
> thing keeps happening; they notice it keeps happening **when they do X**; they read X off a
> placed surface as a forbidden way; they perform Y and it stops. The Watcher never says
> "don't do X." It takes a little warmth, names the lapse only on repetition, and the world
> holds the lesson in a doc/carving/NPC line the group can find on their own.

## 0. The law of the toll (read first — every way below obeys it)

Cross-ref `design/INTEGRATION.md §3.2` (the customs→report bridge) and `WORLD-BIBLE.md §5`
(the Dark takes the *unlit / unwitnessed* — so the toll is always **a small withdrawal of
warmth/light/being-watched**, the thing the way protects, never a withholding of progress).

- **INV-8 — tolls take WARMTH, not progress.** A toll never blocks a puzzle, deletes an item,
  kills, teleports, or locks a door. It dims, chills, gutters, sets a footstep behind you.
  Everything it takes, performing the way **returns**. No clue is ever gated on conduct.
- **Reversible + escalating.** Repetition raises `toll_tier` 0→1→2. Performing the way once
  (`ps.honor`, the compliance event the plugin already emits) **clears** the toll: tier→0,
  `deciphered=true`, a quiet relief.
- **Stage A is DENIABLE (no message).** The first toll is a sensory beat with **no text** —
  silence is canon. A torch guttering, a cold pulse, a half-beat footstep. The player must
  *infer* the link; the game does not assert it. (This is what makes it discover-by-*punishment*
  and not a tutorial popup.)
- **Stage B is NAMED (one line).** Only on repetition does the Watcher speak — and even then it
  names the **lapse**, never the rule-as-instruction. It states what was not kept; it never says
  "do this." The how-to-stop lives in the world (the teaching surface), not in the Watcher's mouth.
- **Collective, never chosen.** A toll lands on the player who lapsed, but the Watcher's named
  line reads by-conduct, never "you alone." "the rest are lit. the rest are held." No favourite.
- **Honorless ways reward RELIEF, not a boon.** the_unspoken and (mostly) the_dark_hours have no
  positive "honor" act — you can't *un-say* a name. For these the cleared state is the cold simply
  **stopping**, designed as relief, never a "kept" boon line. (INTEGRATION §3.2.)

### The five-stage shape (per way, identical skeleton)

```
(1) TRANSGRESS   plugin tracks the action (TrackerConfig) → customs/compliance row → toll_tier eligible
(2) STAGE A      a deniable sensory toll, NO text. transgression_count++ ; toll_tier→1
(3) STAGE B      on repetition, the Watcher NAMES the lapse (one line). toll_tier→2 ; last_toll_at set
(4) DECIPHER     the group reads the WAY off the teaching surface (a sites.yml id). they learn Y.
(5) HONOR        performing the way once → ps.honor → toll_tier→0, deciphered=true, relief/boon
```

`toll_tier` is the **escalation cursor**, not a strike count: 0 none, 1 deniable (A), 2 named (B).
It only climbs on a *new* transgression after a cooldown window (so one long lapse = one step, not a
spam). It only falls on `honor`. `transgression_count` is the lifetime tally (drives the showrunner's
per-player Attention; never resets).

---

## THE SEVEN WAYS

For EACH way: **(a)** the tracked transgression (cross-ref TrackerConfig), **(b)** the toll —
stage A deniable / stage B named, what warmth it takes, **(c)** the teaching surface (a sites.yml id +
what the group reads there to decipher it), **(d)** the Watcher voice lines (proposed voice keys; the
engine never hardcodes — these go in voice.ts at integration).

---

### 1. the_kept_light — Keep the Lamp

> "keep your light. the unlit are not kept." (R01 founding ordinance, way i)

**(a) Transgression (tracked).** `TrackerConfig.CUSTOM_KEPT_LIGHT`. A base/player goes dark — no
fire/light source burning at home through the night, sampled per the `kept-light` cooldown
(`keptLightCooldownMs`, default 600s → one tally per dark window, not per tick). The Dark takes the
unlit *first*, so this is the way the toll bites soonest and clearest.

**(b) The toll — light, withdrawn.**
- **Stage A (deniable, no text):** the nearest light source the player is holding/standing-by
  **gutters** — drops to a flicker for a few seconds, then steadies. A faint cold pulse. Reads as
  "weird, my torch flickered." Once. No message. (`toll_tier`→1.)
- **Stage B (named, one line):** on a second dark window, the gutter holds longer and the Watcher
  speaks `tollKeptLight`. The warmth taken: a held light stays dimmed (cosmetic, reversible) until
  honored. Nothing is consumed; nothing is blocked.
- **Honor (clears):** light a fire and keep it lit at home through a window → `ps.honor` → tier→0,
  `deciphered=true`. Boon line `keptKeptLight` (relief + a kept-light glow that lingers a beat).

**(c) Teaching surface.** `sites.yml: kept_light_home_01` (type `kept_light`, the home scan-zone,
radius 12) carries a placed lectern/posted leaf: **R02 — THE KEEPING OF LIGHT** ("A LIT HOLD IS A
KEPT HOLD. KEEP YOUR LAMP.") with the scratched under-line *"we kept them all lit for so long."*
Reinforced by **Wenna** topside (`npc:wenna` — "keeps a lamp by the door so the dark knows the house
is taken… Keep your light. Above all the others, keep your light.") and **Mara's letter L03** to
Sella ("light your own small lamp. keep it lit. that is your whole work tonight"). Three independent
readings of the same way → the group can decipher it from any one.

**(d) Watcher voice (proposed keys → voice.ts).**
```
tollKeptLight():  'a light went out where one was owed. the dark notes the unlit. nothing else here notes you kindly.'
keptKeptLight():  'the light is kept. the dark knows the house is taken. it is well.'
```

---

### 2. the_deep_line — The Deep Line

> "pass not the marked depth." (R01, way ii) — Iss's sin; the way that, broken first, let the Dark in.

**(a) Transgression (tracked).** `TrackerConfig.CUSTOM_DEEP_LINE`. Breaking blocks / standing below
`deepLineY` (default Y −48), one flag per `deepLineCooldownMs` window (default 300s — one deep session
= one flag, anti-farm). This is the heaviest way thematically (the Break), so its toll is the most
*atmospheric* but stays just as reversible — crossing the Line is not a hard wall.

**(b) The toll — being-watched, withdrawn (replaced by being-watched-by-the-wrong-thing).**
- **Stage A (deniable, no text):** below the Line, a single **half-beat footstep** sounds behind the
  player, once, with no source. The light level reads one notch colder. Deniable as cave ambience.
  (`toll_tier`→1.)
- **Stage B (named, one line):** on a repeat descent below the Line, a brief **lights-dim** pulse in
  a radius + the footstep, and the Watcher speaks `tollDeepLine` (it names the crossing as *the old
  sin*, without forbidding it — the player must connect that to "stop crossing"). Warmth taken: the
  cold/footstep persists at depth until they return above the Line for a window.
- **Honor (clears):** return above the marked depth and hold there for a window (the *stepping back*
  is the honor) → `ps.honor` → tier→0, `deciphered=true`. Relief line `keptDeepLine` (the footstep
  stops; this way's "boon" is mostly the cold lifting — a quiet relief, near-honorless).

**(c) Teaching surface.** `sites.yml: the_threshold` (Orin's threshold lintel marker) and the Stair
itself; the placed text is **R06 — SURVEY OF THE DEEP LINE** ("the Line being the Line for a reason
older than the order that marks it") plus the keeper-adjacent margin *"he opened the Line looking for
home and let home in from the other side."* Cross-taught by **Iss's stone** (`stone_iss` / the
`stone-iss-wall` node — *the warm lie* "the ways are a wall, cross it") which the **catch**
(`no-wall-catch`) overturns. The group learns the Line both as **law** (the survey) and as **the lie
that broke it** (Iss) — the deciphering doubles as the Liar-thread payoff.

**(d) Watcher voice (proposed keys → voice.ts).**
```
tollDeepLine():  'someone stood past the line and looked into the reach of the dark. this is the old crossing. it was the first.'
keptDeepLine():  'the line is held again. there is a kept side, and you are on it. that is not nothing.'
```

---

### 3. the_dark_hours — The Dark Hours

> "lie not down on the black moon." (R01, way iii) — Brann's way; the Dark reaches the sleeping.

**(a) Transgression (tracked).** `TrackerConfig.CUSTOM_DARK_HOURS`. `PlayerBedEnterEvent` during a
taboo moon phase (`darkHoursMoonPhases`, default {0} = full moon, mapped to the arc's "black moon"),
one flag per `darkHoursCooldownMs` window (default 60s). The clean signal is *sleeping on the black
moon* — not sleeping in general.

**(b) The toll — rest, withdrawn (a bad waking).** Near-honorless: you cannot un-sleep, so the
cleared state is the toll simply *not landing again* once they learn to stay up.
- **Stage A (deniable, no text):** the player is allowed to sleep, but the **wake** is wrong — a cold
  pulse on waking, the lamp by the bed has guttered out, one ambient distant sound. No message.
  Reads as "huh, my torch burned out overnight." (`toll_tier`→1.)
- **Stage B (named, one line):** on a second black-moon sleep, the same bad-waking + the Watcher
  speaks `tollDarkHours` (it names Brann's fate without saying "don't sleep" — *one of the keepers
  slept and what he dreamed came inside*). Warmth taken: a short post-wake chill/dimness.
- **Honor (clears):** let a black-moon night pass **awake** (no bed-enter through the taboo phase) →
  `ps.honor` → tier→0, `deciphered=true`. **Relief, not boon** (INTEGRATION §3.2): the chill simply
  does not come; the optional line `keptDarkHours` is a flat acknowledgement, not a reward.

**(c) Teaching surface.** `sites.yml: first_report_lectern_01` (the report-lectern) carries **R12 —
WATCH-LOG OF THE LATER NIGHTS** ("black moon. the order is: do not lie down. i am entering this so the
record knows i did not lie down") and **R13** ("do not lie down on the black moon. it is always the
black moon now"). Topside, **Wenna** half-remembers it as folk-charm ("When the moon goes black you
stay up… Slept through it once as a girl and had the worst dreams of my life") — `npc:wenna`. The
group can read the rule off the watch-log OR catch Wenna's mutter.

**(d) Watcher voice (proposed keys → voice.ts).**
```
tollDarkHours():  'the black moon was up, and someone closed their eyes beneath it. the dark reaches the sleeping. it reached.'
keptDarkHours():  'the watch was kept through the black moon. what comes for the sleeping found no one sleeping.'
```

---

### 4. the_offering — Give Back to the Deep

> "return the first of the deep to the deep." (R01, way iv) — Vaun's broken way; the unfed deep takes instead.

**(a) Transgression (tracked).** `TrackerConfig.CUSTOM_OFFERING`, read against the
`offering_cairn_01` site and the hoard scoring (`hoardWeights` / `oreMaterials`). Mining ore from the
deep and **never dropping the first of it back at the cairn** — taking and returning nothing. The
`offering_cairn_01` site is the compliance anchor (first-ore drop at the cairn = compliance).

**(b) The toll — the cairn's warmth, withdrawn (a hungrier deep).**
- **Stage A (deniable, no text):** after a stretch of taking with an empty cairn, the next time the
  player picks up ore there is a faint **cold pulse + a single absent-sounding chime** — and a torch
  near the cairn gutters. No message. Reads as ambience. (`toll_tier`→1.)
- **Stage B (named, one line):** on continued taking-without-giving, the Watcher speaks `tollOffering`
  (it names the empty column against them — Vaun's column — without saying "go offer"). Warmth taken:
  the cairn area reads colder/dimmer until an offering is made.
- **Honor (clears):** drop the first of the deep (an ore/ingot) at `offering_cairn_01` → `ps.honor` →
  tier→0, `deciphered=true`. Boon line `keptOffering` (the cairn warms; a small kept glow).

**(c) Teaching surface.** `sites.yml: offering_cairn_01` (the cairn itself — the where-to-do-it is the
where-you-learn-it) plus **R03 — THE OFFERING LEDGER** ("the one called vaun is the only open column
in this book… the deep keeps a column too"). Cross-taught by **Vaun's stone** (`stone_vaun` /
`stone-vaun` node — *"vaun counted everything and gave nothing back… the land counts first"*),
**Orin's letter L05** ("I will keep the offering you would not. one stone a winter"), and **Wenna**
("you leave a little, you get to keep a little. That's the whole of it"). The group reads the way as
**law** (ledger), **fate** (Vaun), and **folk-habit** (Wenna's crust).

**(d) Watcher voice (proposed keys → voice.ts).**
```
tollOffering():  'someone takes from the deep and returns nothing. the column for giving-back stands empty against a name. a hungry deep takes instead of waiting.'
keptOffering():  'the first of the deep was given back. the column is crossed clean. the deep waits again.'
```

---

### 5. the_bow — Bow at the Markers

> "bow at the markers. honor the kept." (R01, way v) — Orin's way; the unacknowledged watching does not acknowledge you.

**(a) Transgression (tracked).** Compliance at `bow_marker_01` (type `bow_marker`,
`PlayerToggleSneakEvent` near the marker), keyed `the_bow`. Passing a marker **upright** —
not crouching/bowing within the marker's radius. The smallest way; costs only a stoop.

**(b) The toll — being-acknowledged, withdrawn.**
- **Stage A (deniable, no text):** passing a marker upright, the marker's particle/glow **dims** as
  you pass (it does not acknowledge you) and a faint cold pulse. No message. Easy to miss the first
  time — which is the point. (`toll_tier`→1.)
- **Stage B (named, one line):** on repeated upright passings, the Watcher speaks `tollBow` (it names
  that the markers note who bends — and that a keeper who would not was *left at the threshold* — the
  R08/Orin echo — without saying "crouch here"). Warmth taken: markers stay un-glowing toward this
  player until they bow once.
- **Honor (clears):** crouch/bow within a marker's radius → `ps.honor` → tier→0, `deciphered=true`.
  Boon line `keptBow` (the marker glows in answer — *acknowledged*).

**(c) Teaching surface.** `sites.yml: bow_marker_01` (the marker — bow where you learn to bow) plus
**R08 — OBSERVED REPORT ON A KEEPER AT THE MARKERS** ("the cost of the bow, which is nothing, which is
to stoop… observed, warned, left at the threshold"). Cross-taught by **Orin's stone** (`stone_orin` /
`stone-orin` node — *"the bow is the smallest of the ways"*), the `orin-threshold` node, **Mara's
letter L03** ("mind the markers… bow at each. I know you think it small. it is not small"), and
**Wenna** ("Bow at the stones… you just bend your knee going past"). The way is taught as the gentlest
rule with the gravest fate attached.

**(d) Watcher voice (proposed keys → voice.ts).**
```
tollBow():  'a marker was passed standing. the markers note who bends and who does not. the smallest of the ways was a keeper left at the threshold.'
keptBow():  'the marker is bowed to. the watching is acknowledged, and acknowledges. you are kept.'
```

---

### 6. the_unspoken — The Unspoken

> "speak not the name." (R01, way vi — the blank line; the rent paid to no named landlord)

**(a) Transgression (tracked).** `TrackerConfig.CUSTOM_UNSPOKEN` / `containsForbidden()` over chat —
a message containing a **forbidden word** (the `forbidden-words` set; the Dark's name and its near
forms). The cleanest possible signal: it is on the chat path, instant, per-message. **Honorless** —
you cannot un-say a name; the only "honor" is to stop.

**Live word.** The current launch config sets the forbidden word to `unkept`. This is deliberately
not a random secret string: Wenna warns against saying the cold's name, and the keeper-field later
teaches `unkept`, so the custom can be discovered and triggered without requiring director trivia.

**(b) The toll — quiet, withdrawn (the dark leans in).** Designed as **relief, not boon**
(INTEGRATION §3.2): the cleared state is the leaning-in simply ceasing.
- **Stage A (deniable, no text):** the moment after the word is sent, a single **cold pulse +
  light-dim flicker** near the speaker and one ambient sound, as if something turned. No message —
  the silence after speaking the name is the horror. (`toll_tier`→1.)
- **Stage B (named, one line):** on a repeat speaking, the dim/flicker holds a beat longer and the
  Watcher speaks `tollUnspoken` (it names that the name was shaped, that attention turns to the
  nearly-said — never "stop typing it"). Warmth taken: a short attention-on-you chill.
- **Honor (clears):** there is no act — letting a window pass **without** speaking the word lets
  `toll_tier` decay to 0 and `deciphered=true` on the next clean window (an *abstention*, the
  `unspoken-refrain` side-quest's own logic). **Relief only:** the cold stops. No "kept" line — the
  optional `keptUnspoken` is a bare, cooling acknowledgement, used sparingly or not at all.

**(c) Teaching surface.** `sites.yml: first_report_lectern_01` carries **R07 — ORDINANCE ON THE
SPEAKING** ("Speaking it BY NAME is observed, entered, and the watch is doubled… of the hands entered
for speaking it by name this winter, [████] are no longer on any roll. KEEP THE SIXTH WAY. IT COSTS
YOU NOTHING TO BE SILENT") and the child's chalk *"i never said it. i drew it. that is not the same."*
The blank sixth line of **R01** (the founding ordinance leaves it blank) teaches the way by *omission*
— the group sees that one rule is unwritten and asks why. Topside **Wenna** seals it ("You don't say
the cold's name… 'You don't name it, Wenna.' …So I don't. Habit now") — `npc:wenna`. **Aro lies**
about it ("shout your own name if you want, holes don't have ears") so the world later contradicts him.

**(d) Watcher voice (proposed keys → voice.ts).**
```
tollUnspoken():  'a name was shaped that is not to be shaped. it was nearly said. the dark leans toward the nearly-said.'
keptUnspoken():  'the word stays shut. what was not said cannot turn its face toward you.'
```

---

### 7. the_sacred_beast — Keep the Deep-Bird

> "keep the bird; it goes dark before we do." (R01, way vii) — Sella's way; the warning lost, you go dark unwarned.

**(a) Transgression (tracked).** `TrackerConfig.CUSTOM_SACRED_BEAST` / `sacredBeastPdcKey`
(`observance_sacred_beast`). The death of a **tagged** deep-bird (a pale canary-mob, PDC-flagged) by
the player's hand or neglect — the `SacredAnimalBeat` / `haunted-herd` layer. Losing the bird = losing
the early-warning before the air turns.

**Immediate fork receipt.** The one glowing, fork-arming deep-bird gives the killer a private, low-info
receipt when it dies: `the warning is silenced.` This keeps the choice memorable without spelling out
an ending flag or breaking into a title scare.

**(b) The toll — warning, withdrawn (you go dark unwarned).**
- **Stage A (deniable, no text):** when the bird dies, the ambient bird-song that has been faintly
  present **stops**, the area reads a notch colder, and the *next* genuine creep beat arrives with
  **no warning particle/sound** (it would normally have a tell). No message — the absence of the song
  is the toll. (`toll_tier`→1.)
- **Stage B (named, one line):** if the group loses a second bird, the Watcher speaks `tollSacredBeast`
  (it names that the warning is silenced, that they go into the dark with nothing to sing first —
  never "protect the bird"). Warmth taken: warnings stay suppressed until a bird is kept again.
- **Honor (clears):** keep a tagged deep-bird alive through a window (or restore one to its coop) →
  `ps.honor` → tier→0, `deciphered=true`. Boon line `keptSacredBeast` (the song returns; warnings
  restored).

**(c) Teaching surface.** `sites.yml: kept_light_home_01` (the deep-bird coops sit at the Lamp-works /
home scan-zone in the survey; the bird shares the lamp's roll per R02 cl.5) plus **R11 — INVENTORY OF
THE SET-APART** entry 4 ("the deep-bird of the third coop… down. did not come up"). Cross-taught by
**Sella's letter L04** ("the bird keeps singing right up until it stops, and that is the whole help it
is. that is why we keep it. so we hear it stop"), **Mara's L03** ("the deep-bird ate your seed-cake…
if that stair goes dark the bird cannot be seen to be living"), and **Aro's rumor** ("there's a bird
down there older than the digging. Keeps the air sweet"). The group learns the bird is a *warning
instrument*, not décor.

**(d) Watcher voice (proposed keys → voice.ts).**
```
tollSacredBeast():  'the deep-bird is down. the bird sings while the air is good and stills when it is not. the warning is silenced. you go on unwarned.'
keptSacredBeast():  'the deep-bird is kept. it will sing while the air is good, and you will hear it stop. that is the keeping.'
```

---

## A. How the player INFERS each way (the deciphering path, made fair)

Each way is learnable from **at least two independent surfaces** so no single missed lectern
soft-locks the lesson, and the *toll itself* points at the way (the warmth it takes is the warmth the
way protects — light for kept-light, song for the bird, acknowledgement for the bow). The chain the
designer is engineering, per way:

| way | "this keeps happening when we…" | "…X is forbidden" (read from) | "doing Y stops it" |
|---|---|---|---|
| the_kept_light | …let the home go dark | R02 / Wenna / L03 | keep a fire lit at home |
| the_deep_line | …dig/stand below the marked depth | R06 / the_threshold / Iss-catch | step back above the Line |
| the_dark_hours | …sleep on the black moon | R12–R13 / Wenna | stay awake through the black moon |
| the_offering | …mine and never give back | R03 / stone_vaun / L05 / Wenna | drop first-ore at the cairn |
| the_bow | …pass the markers upright | R08 / stone_orin / L03 / Wenna | crouch/bow at a marker |
| the_unspoken | …type the name in chat | R07 / R01 blank line / Wenna | stop saying it (abstain) |
| the_sacred_beast | …let the deep-bird die | R11 / L04 / L03 / Aro | keep a bird alive |

**The Wenna spine.** `npc:wenna` is the **accidental Rosetta** — she keeps six of the seven ways as
folk-charm and *forgets a different seventh each time*, so a group that talks to her gets a soft,
deniable, in-character index of the whole custom set without ever being handed a rulebook. She is the
mercy valve for discover-by-punishment (INTEGRATION §3.4: "a player can learn a way from a PERSON …
OR from their own doused torch"). Aro **lies** about three ways (cross the Line, sleep anywhere, say
any name) so the tolls later *contradict him* — the player who trusted Aro learns the way the hard way
and learns not to trust Aro in the same beat.

## B. Coherence checks (against the HARD CONSTRAINTS)

- **Every custom_key resolves** to `canon.ts CUSTOM_KEYS`: the seven `the_`-prefixed keys, used
  verbatim. No unprefixed drift (guarded by `customKeyNamespaceSelfTest`, B-3).
- **Every site_id resolves** to an **enabled** `sites.yml` entry: `kept_light_home_01`,
  `offering_cairn_01`, `bow_marker_01`, `first_report_lectern_01`, `the_threshold` — all present and
  `enabled: true` (verified against the file). Teaching surfaces reuse compliance anchors where the
  *doing* and the *learning* are the same place (you bow where you read about bowing).
- **Every keeper named** is a `KEEPERS` member (vaun/mara/sella/orin/brann/iss). NPC `wenna`/`aro`
  are SET-A surface NPCs (npc_dialogue_state `npc_key`), not keepers — kept distinct.
- **Every doc cited** is a real corpus record (R01–R13, L03–L05) — no invented sources.
- **Voice register:** all proposed lines are lowercase, no contraction, no exclamation, no capital,
  no named emotion, collective-not-chosen — matching `voice.ts` SET-B. They **state the lapse**, never
  instruct. (The instruction lives in the world; the Watcher only observes.)
- **No slop:** no "a testament to" / "little did they know" / "the air was thick" / named emotions /
  three-adjective lists / "not just X but Y". Tolls shown through the concrete withdrawn thing (a
  guttered torch, a stopped song, an un-glowing marker), not described feeling.
- **Consistency-in-lockstep** (the COHERENCE principle): every toll's *mechanic* is the way's *fiction*
  (kept-light's toll is literally light; the bird's toll is literally the lost song), and every way's
  teaching surface is an *already-authored* corpus doc tagged to its thread — no orphaned mechanic on
  stale ARG state.

---

## SCHEMA

```yaml
file: design/content/customs-punishment.md
purpose: >
  The discover-by-punishment loop for the seven canon ways. Per way: the tracked transgression,
  the soft/reversible/escalating toll (stage A deniable / stage B named — takes warmth, never
  progress), the in-world teaching surface (a sites.yml id + corpus doc/NPC) where the group
  deciphers the way, and the Watcher voice lines per toll stage (proposed voice keys, no slop).
  Ends with seed notes for punishment_state. DRAFT — content only; integrated under build guards.
status: draft
not_to_edit: [voice.ts, migrations, plugin, puzzles_seed.sql]   # this file proposes; integration applies

invariants:
  - INV-8_toll_takes_warmth_not_progress      # never gates a clue / deletes / kills / locks
  - reversible_and_escalating                 # toll_tier 0->1->2 on repeat; honor clears to 0
  - stage_A_deniable_no_text                   # silence is canon; player infers
  - stage_B_names_lapse_never_instructs        # how-to-stop lives in the world, not the Watcher's mouth
  - collective_never_chosen
  - honorless_ways_reward_relief_not_boon      # the_unspoken, mostly the_dark_hours

ways:
  - custom_key: the_kept_light
    tracker: CUSTOM_KEPT_LIGHT (kept-light cooldown ~600s)
    transgression: home goes dark / light not kept through a window
    toll_warmth: a held/home light gutters then dims (cosmetic, reversible)
    teaching_site_id: kept_light_home_01
    teaching_docs: [R02, npc:wenna, L03]
    honor: light + keep a fire at home through a window
    voice_keys: { toll: tollKeptLight, kept: keptKeptLight }
    thread: place
  - custom_key: the_deep_line
    tracker: CUSTOM_DEEP_LINE (y<deepLineY ~-48; cooldown ~300s)
    transgression: break/stand below the marked depth
    toll_warmth: half-beat footstep + colder light below the Line; persists until stepping back
    teaching_site_id: the_threshold
    teaching_docs: [R06, stone_iss, no-wall-catch]
    honor: return above the Line and hold a window (stepping back)
    voice_keys: { toll: tollDeepLine, kept: keptDeepLine }
    thread: happened
    note: near-honorless; boon is mostly the cold lifting
  - custom_key: the_dark_hours
    tracker: CUSTOM_DARK_HOURS (bed-enter on taboo moon phase {0}; cooldown ~60s)
    transgression: sleep on the black moon
    toll_warmth: a bad waking — guttered bed-lamp, cold pulse, suppressed next-warning
    teaching_site_id: first_report_lectern_01
    teaching_docs: [R12, R13, npc:wenna]
    honor: pass a black-moon night awake (no bed-enter through the phase)
    voice_keys: { toll: tollDarkHours, kept: keptDarkHours }
    thread: happened
    note: honorless-leaning; kept line is relief/acknowledgement, not a reward
  - custom_key: the_offering
    tracker: CUSTOM_OFFERING (offering_cairn_01 + hoard/ore weighting)
    transgression: take ore from the deep, return nothing to the cairn
    toll_warmth: cold pulse + guttered torch at the cairn; cairn area reads colder until an offering
    teaching_site_id: offering_cairn_01
    teaching_docs: [R03, stone_vaun, L05, npc:wenna]
    honor: drop first-of-the-deep (ore/ingot) at offering_cairn_01
    voice_keys: { toll: tollOffering, kept: keptOffering }
    thread: who
  - custom_key: the_bow
    tracker: the_bow (bow_marker_01; sneak/crouch near marker)
    transgression: pass a marker upright
    toll_warmth: marker dims / does not acknowledge; stays un-glowing toward the player until a bow
    teaching_site_id: bow_marker_01
    teaching_docs: [R08, stone_orin, orin-threshold, L03, npc:wenna]
    honor: crouch/bow within a marker radius
    voice_keys: { toll: tollBow, kept: keptBow }
    thread: who
  - custom_key: the_unspoken
    tracker: CUSTOM_UNSPOKEN (containsForbidden over chat; forbidden-words set)
    transgression: send a message containing the Dark's name / forbidden word
    toll_warmth: cold pulse + light-dim flicker after sending; a short attention-on-you chill
    teaching_site_id: first_report_lectern_01
    teaching_docs: [R07, R01_blank_sixth_line, npc:wenna, npc:aro_lies]
    honor: none — abstain; toll_tier decays on a clean window (unspoken-refrain logic)
    voice_keys: { toll: tollUnspoken, kept: keptUnspoken }
    thread: surface
    note: honorless; reward = relief (the cold stops); kept line bare or omitted
  - custom_key: the_sacred_beast
    tracker: CUSTOM_SACRED_BEAST (sacredBeastPdcKey observance_sacred_beast)
    transgression: a tagged deep-bird dies (hand or neglect)
    toll_warmth: bird-song stops; area colder; next creep beat arrives with no warning tell
    teaching_site_id: kept_light_home_01
    teaching_docs: [R11_entry4, L04, L03, npc:aro]
    honor: keep/restore a tagged deep-bird alive through a window
    voice_keys: { toll: tollSacredBeast, kept: keptSacredBeast }
    thread: who

proposed_voice_keys:                 # add to discord/src/voice.ts at integration (NOT now)
  - tollKeptLight
  - keptKeptLight
  - tollDeepLine
  - keptDeepLine
  - tollDarkHours
  - keptDarkHours
  - tollOffering
  - keptOffering
  - tollBow
  - keptBow
  - tollUnspoken
  - keptUnspoken
  - tollSacredBeast
  - keptSacredBeast

# ---------------------------------------------------------------------------
# punishment_state seeding notes (the 0005 table). punishment_state is PER-PLAYER
# RUNTIME state — it is NOT pre-seeded with rows. The plugin/bridge UPSERTS a row
# the first time a player transgresses a way. What IS authorable now is the
# (custom_key -> teaching_site_id) MAP the upsert stamps onto each new row, so the
# loop is fair (the row knows where the player can go learn to stop). Below is that
# map + the column semantics, as a reference for the integration step.
# ---------------------------------------------------------------------------
punishment_state_seed_notes:
  table: public.punishment_state         # migration 0005_threads.sql
  not_pre_seeded: true                    # rows are created on first transgression (upsert on (player_id, custom_key))
  authored_now: custom_key -> teaching_site_id map (stamped onto each new row so deciphering is fair)
  teaching_site_map:                      # all enabled in sites.yml (verified)
    the_kept_light:   kept_light_home_01
    the_deep_line:    the_threshold
    the_dark_hours:   first_report_lectern_01
    the_offering:     offering_cairn_01
    the_bow:          bow_marker_01
    the_unspoken:     first_report_lectern_01
    the_sacred_beast: kept_light_home_01
  column_semantics:
    player_id:            uuid -> players(id)
    custom_key:           one of CUSTOM_KEYS (the_-prefixed); FK-free but canon-guarded
    transgression_count:  lifetime tally; increments per flagged transgression; never resets (feeds Attention)
    toll_tier:            0 none | 1 deniable(stage A) | 2 named(stage B). climbs on a new transgression after cooldown; falls to 0 on honor
    last_toll_at:         set when a toll lands (stage A or B); used with the way's cooldown to debounce escalation
    deciphered:           false until the player performs the way once (honor); then true (the toll then clears)
    teaching_site_id:     from teaching_site_map above; the in-world clue that teaches the way (fairness anchor)
    updated_at:           now() on every upsert
  escalation_rule: >
    toll_tier only advances on a NEW transgression that is past the way's TrackerConfig cooldown
    window (deep-line ~300s, kept-light ~600s, dark-hours/unspoken short) — one long lapse is one
    step, not spam. honor (ps.honor) sets toll_tier=0, deciphered=true. transgression_count is
    independent and monotonic.
  honorless_ways: [the_unspoken, the_dark_hours]   # cleared state = relief (the cold stops), NOT a boon line
  draft_sql_shape: |                      # ADDITIVE, parse-clean, idempotent — illustrative ONLY; integration owns the real upsert
    -- DRAFT — not the live seed. punishment_state rows are created at runtime, not seeded.
    -- This is the shape the bridge uses on first transgression (the teaching_site_id map is the authored part):
    begin;
    -- example upsert the customs-bridge performs (player_id + custom_key from the live event):
    -- insert into public.punishment_state (player_id, custom_key, transgression_count, toll_tier, teaching_site_id, updated_at)
    -- values ('{player}', 'the_bow', 1, 1, 'bow_marker_01', now())
    -- on conflict (player_id, custom_key) do update set
    --   transgression_count = public.punishment_state.transgression_count + 1,
    --   toll_tier = least(2, public.punishment_state.toll_tier + 1),
    --   last_toll_at = now(),
    --   updated_at = now();
    commit;

checks:
  - seven_ways_all_covered: true                      # one section per CUSTOM_KEYS member
  - every_custom_key_in_canon: true                   # the_-prefixed, verbatim
  - every_site_id_enabled_in_sites_yml: true          # kept_light_home_01, the_threshold, first_report_lectern_01, offering_cairn_01, bow_marker_01
  - every_keeper_in_KEEPERS: true                     # vaun/mara/sella/orin/brann/iss; wenna/aro are SET-A NPCs
  - every_doc_is_real_corpus: true                    # R01-R13, L03-L05
  - toll_never_gates_progress: true                   # INV-8
  - stageA_silent_stageB_one_line: true
  - register_matches_voice_ts: true                   # lowercase/no-contraction/no-exclam/collective
  - honorless_ways_relief_not_boon: true              # the_unspoken, the_dark_hours
  - no_slop: true
```
