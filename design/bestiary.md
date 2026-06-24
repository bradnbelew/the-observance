# The Observance — Bestiary (player-facing stub, spoiler-free)

> Safe to read. This is in-world flavor + the buildable engineering spec for the
> custom 3D apparitions of *The Observance*. It is mechanics and atmosphere, never
> story. The story of *who* these things are — and what they want — lives in `arc/`
> (sealed). Ethan can read this whole file and stay unspoiled.
>
> Companion to `DESIGN.md` (the anti-jank contract), `design/arg-deepening.md`
> (the mod stack: MythicMobs + ModelEngine for custom 3D models, ZNPCsPlus packet
> apparitions, the resource-pack), and `FLOW.md` (signal→beat triggers).

---

## 0. What a "creature" is here, and what it is NOT

These are not enemies. *The Observance* has **no combat loop** — there is no health
bar to grind down, no boss to kill, no DPS check. Every entity in this bestiary is a
**pressure surface**: a shape the land wears to be *seen*, briefly, and then not.

The hard rules every apparition obeys (from `DESIGN.md §3`, the anti-jank contract):

1. **Soft pressure, never a jump-scare.** They follow at the edge of render, stare
   then are gone, gutter a light, stand where you'll *discover* them — never lunge,
   never strobe, never blast audio. The dread is in *restraint*, ~90% silence.
2. **Discovered, never witnessed appearing.** An apparition only spawns/mutates a
   block when **no player has line of sight** to that spot (`util/Reveal.isHidden`,
   `AbstractBeat.mutateWhenUnwitnessed`). You round a corner and it is *already there*.
3. **Per-player where it matters.** The most intimate beats are sent to **one client
   only** — a sound only you hear (`PrivateSoundBeat`), a wisp only you see
   (`PrivateParticleBeat`), a block only on your screen (`FakeBlockBeat` via
   `util/PerPlayer`). "Did you see that?" "...See what?"
4. **Reversible, warmth not progress.** Anything an apparition does to you
   (`PrivateDarknessBeat`, a doused torch) is short, capped, and self-healing. It
   takes *warmth*, never your hard-won blocks. The kill-switch on the dashboard
   stops all of it instantly.
5. **Accessibility + TINAG preserved.** No flashing, no forced blindness longer than
   a few seconds (hard-capped at 15s in code), no startle audio. It must always read
   as "...is our server doing that?", never "a plugin fired."

> **The look (custom 3D):** each apparition below ships as a **ModelEngine model**
> driven by **MythicMobs** (the `[PACK]` atmosphere stack in `arg-deepening.md §4`).
> The *spawn, naming, persistence, targeting, despawn, and reveal-discipline* are all
> handled by our own `NamedMobBeat` / `SacredAnimalBeat` — we just point the spawn at
> a MythicMob type instead of a vanilla `EntityType`. If the model pack ever fails to
> load, the **deterministic fallback** is the vanilla entity the model rides on
> (a `WARDEN`, `SKELETON`, `COW`…), still silent and still watching. The haunting
> never depends on the art.

---

## 1. The apparitions

Six shapes the watching wears. They are **rare**. In a two-week run a player might
meet two or three of them, once each. Most of the haunting is signs, reports,
doused torches, and sounds — these are the punctuation, not the sentence.

| # | Name (in-world) | Reads as | First felt | Pays off |
|---|---|---|---|---|
| 1 | **The Watcher at the Edge** | a tall, still, faceless figure at render distance | a sound behind you, no one there | it has always been the figure on the ridge |
| 2 | **The One Who Counts** | a stooped shape pacing your stored things | your chest "felt looked-at" | the deep going dark for you alone |
| 3 | **The Surface-Walker** | a pale figure seen *only* in water/ice reflections | a face in the lake that isn't yours | the far water, where wandering ends |
| 4 | **The Stoop** | a low figure that is only there when you are crouched | a marker that "watches" when you pass it standing | bowing, at last, to no one |
| 5 | **The Sleepless** | a restless walker abroad only at night / black moon | a lamp guttering at home after dark | what comes inside when you sleep when told not to |
| 6 | **The Quiet Herd** | one pale animal the others stand and stare at | the herd turning to face you, together | a small life you chose to keep, or didn't |

The seventh shape — **the Cold Hearth's tenant** — is not in this table. It is an
optional, off-path encounter (the `arc` *Seventh* side-mystery). It is described at
the end as in-world rumor only.

---

### 1.1 — The Watcher at the Edge

**Concept.** The canonical figure. A tall, narrow, faceless silhouette — robed,
unhurried, *patient*. It does not move while you look at it. You glance away to check
a noise; you glance back; it is closer, or it is gone. It never crosses the gap. It is
always at the threshold of how far you can see.

**Appearance (ModelEngine).** A 3.2–3.5-block-tall hooded form, no visible face under
the cowl, hands hidden in sleeves. Matte, light-drinking texture — it reads as a
cut-out of darkness more than a creature. Idle animation: an almost-imperceptible
sway and a slow head-turn to track the one it watches. No walk cycle is ever seen
(it only relocates while unwitnessed). Rides a vanilla `WARDEN` or `ENDERMAN` as the
deterministic fallback skeleton (tall, already unsettling, already silent-capable).

**Behavior — soft pressure.** Stand-and-stare. Spawned ~16–28 blocks out, **out of
line of sight**, on valid ground, already **facing the player**. Silent, persistent,
invulnerable, no AI drift — it will not approach, will not attack, will not wander.
It simply *is there, looking*. After a short window, or once the player turns away, it
**despawns only while unwitnessed** so it is never seen to vanish. Often preceded by a
single per-player sound from behind ("did you hear that?") with nothing there.

**How it's built (named classes).**
- Spawn + naming + persistence + stare: **`NamedMobBeat`** — payload
  `{ "entity":"<mythicmob:watcher | WARDEN>", "distance":20, "silent":true,
  "no_ai_drift":true, "invulnerable":true, "despawn_seconds":<short> }`. The class
  already finds an out-of-LoS ring spot (`findSpawn` + `ctx.reveal().isHidden`),
  faces the player, sets `setTarget` without attacking, and despawns
  reveal-disciplined.
- The footstep-behind-you hook: **`PrivateSoundBeat`** (`behind:true`, low volume).
- Optional dread dim as it's noticed: **`PrivateDarknessBeat`** (`DARKNESS`, a few
  seconds, reversible).

**Trigger / gating.** This is the showrunner's signature ambient apparition, paced by
the **drama budget** (`beats/DramaBudget.java`) and hard cooldown (`FLOW §2`,
≤1 ambient/hour, ≥20-min gap). The **Traffic & Territory** heatmap
(`signal/HeatmapAccumulator`, `signal/LocationSampler`) is used to place it where it
will actually be *seen* — a hot path cell, predictively ahead of a player's facing.
Fires more readily for a player whose dossier shows high `soloMiningRatio` /
`distanceFromGroup` (the alone one is the easiest to make feel watched), but it is
not exclusive to them.

**Foreshadow → payoff.** First *felt* as nothing more than a sound with no source in
Movement I. By later Movements the group has learned that the figure on the ridge has
always been the same figure — patient, counting days. Why it watches, and what the
watching is *for*, is the spine of the whole arc (sealed).

---

### 1.2 — The One Who Counts

**Concept.** A stooped, gaunt shape found *near stored things* — beside a base chest
wall, a furnace bank, an ore stash. It looks like someone bent over a tally,
endlessly counting what is kept. It is never violent; it is *covetous-looking*, and
the wrongness is that it is interested in your hoard, not in you.

**Appearance (ModelEngine).** A thin, hunched humanoid, long-fingered, head bowed as
if reading. Threadbare. An idle "counting" gesture — one hand moving slightly, over
and over, as if notching a stick. Worn lamp-glow baked into the texture (the figure
that hoarded light). Fallback skeleton: a vanilla `STRAY` or `SKELETON`, silent.

**Behavior — soft pressure.** Appears at the **edge of a detected base / container
cluster**, out of LoS, when the owner is away or just returning. Stands at the stash,
"counting," then is gone when next unwitnessed. The intimate version is **per-player**
and bloodless: a chest that **looks rearranged to you only** (`ChestArrangeBeat` /
`FakeBlockBeat`), a count-under-the-breath sound only you hear. Its consequence-rhyme
is the **light toll**: a torch in the deep gutters out (reversible).

**How it's built.**
- The figure at the stash: **`NamedMobBeat`** anchored on a base cell from
  **`signal/BaseDetector`** (container-density + bed anchor clusters), `no_ai_drift`,
  short `despawn_seconds`.
- The "someone's been in my chest" beat: **`ChestArrangeBeat`** (reversible
  rearrange) and/or **`FakeBlockBeat`** (a client-only "wrong" block at the stash that
  reverts).
- The cold rhyme: **`TorchGutterBeat`** (`WALL_TORCH`→air outside LoS, relights) +
  **`PrivateSoundBeat`** (a cold, low tone).

**Trigger / gating.** Reads the dossier's **`hoardedScore`** (computed by
`signal/InventoryScanner`) and high `soloMiningRatio`. Surfaces softly when a player
hoards and does not give back — measured through the **Offering** custom-compliance
tally (`SignalSnapshot.complianceFor(...).violationRatio()`). True by construction: it
only ever appears for a hoard the tracker actually measured (grounding, anti-jank #6).

**Foreshadow → payoff.** First a chest that *felt* looked-at. Pays off as the deep
quietly going dark *for that player specifically* — the lamps that won't catch. The
reports name the law. The figure is the warning, shaped like a habit the land already
measured.

---

### 1.3 — The Surface-Walker

**Concept.** A pale figure you see **only in a reflection** — in still water, in ice,
in a calm pool. Look up from the lake and the shore is empty. Look back down and she
is standing just behind your reflected shoulder. The water keeps her; the air does not.

**Appearance (ModelEngine).** A slight, drifting human form, hair and hem moving as if
underwater even on dry land, pale and rinsed of color. Because she is meant to be seen
*reflected*, the model is built to read cleanly upside-down / mirrored. Fallback
skeleton: a `DROWNED` (silent, no AI drift), only ever placed where a reflective
surface sells the effect.

**Behavior — soft pressure.** The purest **per-player, deniable** apparition. She is
shown **only to the player facing the water**, ideally via a packet apparition
(ZNPCsPlus, `arg-deepening §2`) or a reveal-disciplined `NamedMobBeat` placed at the
reflection point; the social-horror payload is one player insisting they saw a second
face. No approach, no contact. She is gone the instant the surface is broken or the
player turns.

**How it's built.**
- The reflected figure: **`NamedMobBeat`** (or a per-player ZNPCsPlus apparition)
  placed at a water/ice anchor out of LoS, `silent`, `no_ai_drift`, short despawn.
- The "only you saw it": **`PrivateParticleBeat`** (a faint disturbance on the water
  for one player) + **`PrivateSoundBeat`** (a soft lap/breath behind).
- The chill: **`PrivateDarknessBeat`**, brief.

**Trigger / gating.** Anchored on water by the **Traffic & Territory** map (shore
cells the group visits) and biased toward a player with high **`distanceFromGroup`** —
the one who wanders off alone. Paced by the drama budget; never spammed. Strongly tied
to the **reflection puzzle vocabulary** already in the arc (the Atbash/mirror clue
verb, `arg-deepening §1.2`), so the apparition and the cipher share a surface.

**Foreshadow → payoff.** First a face in the lake that isn't yours. Pays off at *the
far water* — the place wandering leads, where a reflection is the only thing that comes
back. Tied to the same "do not follow her" thread the documents carry (sealed detail in
`arc/`).

---

### 1.4 — The Stoop

**Concept.** A low, kneeling shape near the **markers** (the bow-stones). It is only
*there when you are crouched*. Walk past a marker standing and you see an empty stone;
crouch at it and a bowed figure is already kneeling beside you, having always been
there. The custom and the creature are the same gesture.

**Appearance (ModelEngine).** A small, perpetually-bowed humanoid, head to the ground,
hands flat — a figure frozen mid-bow. Stone-dusted, half-merged with the marker it
kneels at, as if it has been bowing long enough to become part of the stone. Fallback
skeleton: a baby/armor-stand-scale silent mob, only ever rendered at a marker.

**Behavior — soft pressure.** Tied to **`PlayerToggleSneakEvent`** at a marker (the
**Bow** custom's own detection, `signal/listener/CustomComplianceListener`). The figure
is revealed/hidden by your *posture*: a per-player reveal when you crouch, gone when you
stand. For a player who **repeatedly passes markers standing**, the marker itself "gains
a name and watches" — the existing escalation in `FLOW.md` (Bow custom →
`NamedMobBeat` attention at the marker).

**How it's built.**
- The crouch-gated reveal: **`NamedMobBeat`** (or packet apparition) at a marker
  anchor, surfaced on the player's crouch state; `silent`, `no_ai_drift`. The
  posture-gating reuses the Bow detection already wired in
  `CustomComplianceListener`.
- The watching marker (for the standing offender): **`NamedMobBeat`** giving the
  marker mob a **name** + glow, per `FLOW §1` ("marker mob gains a name and watches").
- The breath when you bow: **`PrivateSoundBeat`**, very low.

**Trigger / gating.** The **Bow** custom-compliance tally
(`SignalSnapshot.complianceFor("bow")`): honored crouches reveal the kneeling figure
gently; a high `violationRatio` (passing standing, repeatedly) escalates the marker to
"watching." Soft-pressure: ignoring markers makes the thread go quiet, not louder.

**Foreshadow → payoff.** First, a marker that "felt like it was watching" when you
walked past it standing. Pays off as the realization that the smallest custom — a
stoop that costs nothing — was the one being counted all along, and that bowing too
late is its own kind of fate (sealed payoff in `arc/`).

---

### 1.5 — The Sleepless

**Concept.** A restless figure abroad **only at night, worst on the black moon** —
the one who will not lie down. It walks when you should be sleeping and is most present
when you *try* to sleep on a night you were warned against. It is the home that does not
rest, and the warning against resting in it.

**Appearance (ModelEngine).** A gaunt, over-awake humanoid with a faint, never-doused
lantern-glow, eyes that read as open even in shadow, a tireless walk-in-place idle.
Built to be legible only in low light (it belongs to the dark hours). Fallback
skeleton: a `STRAY` or `SKELETON`, silent, surfaced only at night / black moon.

**Behavior — soft pressure.** Time-gated. Abroad after dark (moon phase via
`fullTime/24000 % 8`, the same check the **Dark Hours** custom uses). At a **detected
base after dusk with no kept light** (the **Kept Light** custom, `BaseDetector` +
light scan), it manifests the cold-comes-inside beat: a lamp gutters, a sound at the
threshold, a brief chill — all reversible. If a player **sleeps on the black moon**
(the taboo), a private nightmare title/sound fires and the bed denies (per the `arc`
Dark Hours escalation) — atmospheric, never harmful.

**How it's built.**
- The night-walker figure: **`NamedMobBeat`**, surfaced only when world time is night
  / black moon, `silent`, `no_ai_drift`, short despawn.
- The cold at home: **`TorchGutterBeat`** (a base light guttering, relights) +
  **`PrivateSoundBeat`** (a threshold creak) + **`DoorOpenBeat`** (a door that opens
  itself, out of LoS).
- The nightmare on a forbidden sleep: **`PrivateSoundBeat`** + a title/subtitle beat +
  **`PrivateDarknessBeat`** (brief).

**Trigger / gating.** **Dark Hours** + **Kept Light** custom-compliance tallies; moon
phase; base detection. Fires once per night at most (`FLOW §1`, "once/night"). Reads
the dossier for who slept when they shouldn't.

**Foreshadow → payoff.** First, a lamp at home guttering after dark for no reason.
Pays off as the thing that "came inside" the night someone slept when warned not to —
and would not go back out. The one fire at home that never gutters is its mirror
(sealed in `arc/`).

---

### 1.6 — The Quiet Herd (the Sacred Beast)

**Concept.** Among the local animals, **one is pale and wrong** — and the rest of the
herd stands and *faces it*, or faces *you*, together. The Sacred Beast is the gentlest
apparition and the one most fully in your hands: it is a small life you can choose to
protect across the whole run, or not.

**Appearance (ModelEngine, light touch).** Mostly a **retagged vanilla animal** (a cow,
a sheep) made pale and faintly glowing so it reads as *the watched one* — optionally a
ModelEngine reskin (whitened, hollow-eyed) for the marquee. The horror is **behavioral,
not monstrous**: the *ordinary* herd turning as one to watch is scarier than any model.
Fallback: pure vanilla retag (already what the beat does).

**Behavior — soft pressure.** Ambient, opt-in, completely ignorable. The pale one is
persistent, silent, marked. The herd "watches." **Killing it is a tracked
transgression**; **protecting it across the run earns a quiet boon** at the ending. No
aggression, ever — it just *is*, and is remembered.

**How it's built.**
- Tag + mark + glow: **`SacredAnimalBeat`** — payload `{ "match_type":"COW",
  "radius":12, "glow":true }`. The class tags one nearby `Animals` in PDC
  (`sacred_beast`), makes it persistent + silent, idempotently (won't re-tag if one
  exists).
- The kill is recorded via the **`mobKills` / `EntityDeathEvent`** path
  (`signal/listener/DeathListener`) checking the `sacred_beast` PDC tag — a tracked
  transgression, surfaced through the reports.
- The herd-turns-to-watch beat: light per-player **`NamedMobBeat`** facing logic or a
  simple coordinated-gaze pass (kept cheap and vanilla).

**Trigger / gating.** Established once near a herd in the group's territory (heatmap /
base). Its only "trigger" thereafter is **player conduct** — protect or kill — tracked
deterministically. True by measurement; the report only names what the
`DeathListener` actually saw.

**Foreshadow → payoff.** First, a herd that turns to face you, together — uncanny but
harmless. Pays off at the ending as a small kept (or broken) faith that colors how the
land receives the group. A quiet test of whether you keep small lives, not just laws.

---

## 2. Rumor only — the Cold Hearth's tenant (the Seventh)

> Off-path. Optional. Gates nothing. Pure rumor here; its meaning is sealed in `arc/`.

There is talk, in the oldest barely-readable notes, of a **seventh** shape — at a
ruined shrine off the far path where the fire was *let go out*, the one hearth cold all
the way through. Nothing is said to *be* there. The shape, if it is a shape, is an
**absence**: a place where a marker should stand and does not, a name the notes will
not say back. Finding the cold shrine (a discovered-never-witnessed structure) earns a
little extra Whisper budget and a colder understanding of what "kept" means — and what
*not-kept* means. Players who never go looking will never miss it.

**How it would be built, if pursued.** A `SmallStructureBeat`-pasted ruined shrine
(footprint-checked, placed out of LoS), a doused hearth, and — at most — a single,
brief, *retreating* apparition glimpsed once and never explained: a **`NamedMobBeat`**
with a very short `despawn_seconds`, seen leaving, never arriving. The restraint is the
point. (Its true nature is sealed.)

---

## 3. Build & safety summary (engineering quick-reference)

| Apparition | Primary beat class(es) | Per-player intimacy | Time/space gate | Tracked signal |
|---|---|---|---|---|
| Watcher at the Edge | `NamedMobBeat` | `PrivateSoundBeat`, `PrivateDarknessBeat` | heatmap hot cell, out of LoS | `soloMiningRatio`, `distanceFromGroup` |
| The One Who Counts | `NamedMobBeat`, `ChestArrangeBeat`, `TorchGutterBeat` | `FakeBlockBeat`, `PrivateSoundBeat` | `BaseDetector` cluster | `hoardedScore`, Offering compliance |
| The Surface-Walker | `NamedMobBeat` / ZNPCsPlus | `PrivateParticleBeat`, `PrivateSoundBeat` | water/ice anchor, out of LoS | `distanceFromGroup` |
| The Stoop | `NamedMobBeat` | crouch-gated reveal, `PrivateSoundBeat` | marker + `PlayerToggleSneakEvent` | Bow compliance `violationRatio` |
| The Sleepless | `NamedMobBeat`, `TorchGutterBeat`, `DoorOpenBeat` | `PrivateSoundBeat`, `PrivateDarknessBeat` | night / black moon, base + dusk | Dark Hours + Kept Light compliance |
| The Quiet Herd | `SacredAnimalBeat` | — (collective gaze) | territory/herd, once | `mobKills` on tagged PDC |
| Cold Hearth (Seventh) | `SmallStructureBeat`, `NamedMobBeat` (retreating) | — | off-path, discovered | optional, non-gating |

**Non-negotiables (all enforced in code already):**
- Every spawn/mutation goes through **reveal discipline** (`util/Reveal.isHidden`,
  `mutateWhenUnwitnessed`) — discovered, never witnessed.
- Every apparition mob is **silent + persistent + invulnerable + no-AI-drift** and
  PDC-tagged (`beat_entity` / `beat_owner`) for clean despawn and anti-grief.
- Every sensory toll is **bounded + reversible** (darkness capped 15s; fake blocks
  auto-revert; torches relight) — warmth, not progress.
- The whole bestiary is paced by the **drama budget + cooldown** (`DramaBudget`,
  `FLOW §2`). Apparitions are rare by design.
- **Custom 3D is garnish.** ModelEngine/MythicMobs supply the *look*; if the pack
  fails, the vanilla fallback entity still carries the beat. No haunting depends on
  the art loading.
- The **dashboard kill-switch** ("Watcher sleeps", `FLOW §4`) silences everything,
  instantly, for any session.
