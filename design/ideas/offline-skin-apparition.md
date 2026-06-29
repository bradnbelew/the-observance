# IDEA — The Apparition Wearing an Offline Player's Skin

> Build-ready design treatment. One idea, taken to ship. Operates inside the sealed
> bestiary (`arc/bestiary-sealed.md §2.7`), the canon spine (`arc/lore/canon-spine.md`,
> FACT 9 / FACT 15 / FACT 16 / INV-16), and the anti-jank contract (`DESIGN.md §3`).
> Sealed-truth-safe: nothing here states FACT 15.
>
> **Status note (2026-06-25).** This is no longer greenfield. Much of the spine is
> already built: `NamedMobBeat.java` carries `skin_player` / `offline_only` /
> `applyWornSkin` / `retreating` / the worn-UUID PDC tag; the pure orchestrator
> `discord/src/showrunner/offline-skin.ts` (`selectGlimpse`) exists and encodes the
> precision gate + separation law. This treatment is reconciled to that reality: it
> credits what exists and names the **three real remaining gaps** (§6.1) so the doc is
> a build checklist, not a re-pitch of solved work.

**Pitch.** *"Wasn't Brann logged off?"* — an apparition that wears the skin of a group
member who is **currently offline**. Not a jumpscare: it is the keeper-enactment canon
(FACT 9) made literal on a friend server, where "a face you know that should not be
here" is a real, native dread the medium hands us for free.

**One-line verdict: KEEP — SCALED.** Build it as a **rare, reflection-only / edge-glimpse
re-skin** layered on `NamedMobBeat`, gated hard on offline + precision + a one-shot
budget + the carve separation law. Do **not** build a free-standing walking-around-camp
doppelgänger; that breaks reveal discipline and reads as a mod, not the world.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 What it actually is (mechanically, as built)

A normal `NamedMobBeat` apparition (silent, persistent, `no_ai_drift`, invulnerable,
out-of-LoS `findSpawn`, reveal-disciplined despawn — every existing guarantee) with the
two payload additions already coded into the class:

1. **A worn identity** — `skin_player` (a name or UUID). `applyWornSkin` resolves it
   cache-first, sets the apparition's custom name to the worn player's last-known name
   only if the payload didn't supply one, and tags the apparition's PDC with the worn
   UUID (`worn_skin`) so the rejoin-sweep can find it. The actual skin texture is the
   ModelEngine/NPC go-live residue; until then the apparition is the dark
   WARDEN/STRAY/DROWNED **silhouette** the shape already uses — never a wrong green
   zombie (`resolveEntity`).
2. **The separation law gate** — `offline_only` (default true when a skin is named).
   `wornPlayerEligible` enforces three things: never wear the **target's own** shape;
   suppress while the worn player is **online**; fail-closed if the worn player can't be
   verified. Re-checked **at fire** in `doEnact`, not just `canEnact` — the logout/login
   race that would co-locate a player with their own apparition is closed in code.

The orchestration policy (`offline-skin.ts selectGlimpse`) sits above the beat and
decides **when** and **whose**: it filters to offline candidates with a name and budget
left, requires a **dominant shape-rhyme** (`minRhyme 0.5`, `minRhymeMargin 0.15` — a
real rhyme, not a flat tie), refuses any glimpse cell that **collides with that player's
active name-carve** (the INV-16 separation law, fed by the carve lane's
`carveClaimsByPlayer`), and shows a name-tag **only** in the human-approved `named`
phase. It is pure (no DB / clock / LLM); a thin `offline-skin.run.ts` wrapper (NOT YET
BUILT — §6.1) feeds it presence + dossiers + carve claims and emits the `NamedMobBeat`
intent.

### 1.2 The story it carries (why it is not a gimmick)

The sealed spine: the presence **is the accumulated keepers** — the land wears people as
shapes (`bestiary-sealed §0`). The whole bestiary's thesis is *"these things are people
who were here before us."* The offline-skin apparition is the **cleanest possible
expression** of that thesis, because for the first time the person worn as a shape is one
of *them* — someone in the Discord call who logged off twenty minutes ago.

`bestiary-sealed §2.7` seals the meaning precisely and this treatment must not drift from
it: the going-out took the Kept "family by family, lamp by lamp," and the ones taken
first were the ones who had **stopped coming to the light** — who logged off, in the old
sense. The land wears the offline friend because, in the world's grammar, the friend who
is not here is the friend the going-out reaches first. **The horror is not that a monster
looks like your friend; it is that the land has begun to file your absent friend the way
it filed the Kept** — wearing their shape because they are, for now, un-witnessed. That
is the felt edge of FACT 16 ("the record files the living by *place*, not only by name")
turned on a living, absent member.

It is the carrier of **FACT 9** ("the first hauntings were a specific keeper's fate
re-enacted at the group", `canon-spine §3`, `bestiary-sealed §4`). Brann's document
(`arc/lore/documents/do-not-close-your-eyes-here.md`) is its lore home: *"what i dreamed
did not stay a dream — it came inside, and it has not gone back out, and it walks when i
walk."* On a server, the thing that "walks when you walk" and "will not go back out" is
the skin of someone who logged off.

### 1.3 How it plays across the five movements

- **Movement I (Establishment).** NOT present as an apparition. The seed is planted as a
  **report that names an offline player** for a lapse (§5), so the group learns early
  that the record holds you while you are gone. No worn shape yet.
- **Movement II (The Ways).** Still no skin-apparition. The group learns the six keepers
  are prior people worn as shapes (keeper-stones). The Surface-Walker (Sella) and Watcher
  (Vaun) are met as *strangers'* faces. The vocabulary — "a worn face," "seen in
  reflection only" — is taught with keepers the group never knew: uncanny, not yet
  personal.
- **Movement III (deepening / the Undercroft).** **First firing, most deniable**
  (`phase: 'deniable'`). A single player, alone, low on the roster that night, sees in a
  **still-water / ice reflection** a figure behind their own reflection wearing the skin
  of a friend who logged off earlier that session. **No name-tag** (`nameTag:false`,
  enforced in `selectGlimpse`). Break the surface, turn: empty shore, despawned before a
  second player arrives. This is the Surface-Walker beat re-skinned at `the_far_water`.
- **Movement IV (the catch / FACT 9 named).** The escalation and the **payoff**. Iss is
  caught; a keeper-NPC (or exposed Iss) delivers FACT 9. Only now may the apparition
  appear at the **edge of render (Watcher posture)** wearing an offline player's skin
  **with the name-tag shown** — exactly **once**, group-witnessed, on a hot path, gated
  by the dashboard approval queue (`phase:'named'` + `namedApproved`). The realisation:
  *the figure on the ridge has always been one of us, worn early.*
- **Movement V (the Accepting).** NOT a new firing. It pays its debt by
  recontextualising the finale: when a player is **received** (FACT 14→15), the world
  flips to *kept* and the persistent markers/figures a next group would meet are the
  current group. The offline-skin glimpse was the **rehearsal**: the land already showed
  them it can wear them. M5 doesn't explain it; it makes the earlier glimpse
  retroactively true.

### 1.4 The intended emotional arc

confusion (M3, deniable, single witness) → corroboration (M4, named, group-witnessed,
FACT 9 spoken) → cold understanding (M5, the flip makes the glimpse a rehearsal of their
own induction). Every step is grounded in a measured signal so it never reads as the
engine inventing.

---

## 2. CRITIQUE — adversarial, honest

### Risk A (SHARPEST) — the name-tag is an out-of-character "the plugin did that" tell.
A floating vanilla name-tag reading `Brann` over a mob is the single most likely thing to
snap a player out of TINAG into "a plugin spawned a fake Brann." A wrong/garbled name
(UUID, `Brann2`, a name-collision, a since-renamed account) is a catastrophe on camera.
**Mitigation (built + to-verify):** default to NO name-tag. `applyWornSkin` only sets a
name from the **cached last-known name**, and `selectGlimpse` sets `nameTag:true` ONLY in
the `named` phase, ONLY after `namedApproved`. **Gap to close:** the run wrapper must pass
`name_visible:false` to the beat in the deniable phase (the beat's `name_visible` already
defaults false — confirm the wrapper never overrides it), and the named M4 beat must be
human-approved through the dashboard queue, never auto-fired. The skin alone carries
recognition; that is the native, deniable dread ("I swear that was your skin").

### Risk B — reveal discipline vs. a recognisable figure.
The law is *discovered, never witnessed appearing or moving*. A doppelgänger tempts you to
make it **walk** — exactly what we cannot show. **Mitigation (built):** `no_ai_drift:true`
fully disables AI (`setAware(false)`/`setAI(false)`) — it cannot path, drift, or attack.
The `retreating` variant (the Seventh glimpse) is un-targeted and faces **away**; the
retreat is read in the **yaw**, never in pathfinding — the class flips spawn yaw 180° and
the mob still never moves. Hard-bind every firing to a built posture: reflection
(Surface-Walker), edge stand-and-stare (Watcher), crouch-reveal (Stoop), or retreating
(Seventh). **If a future hand wants it to walk, that is a CUT.**

### Risk C — PRECISION / the "it knows me" law.
Choosing *whose* skin to wear is a personalisation claim; a wrong one is worse than none
(`canon-spine §6 rule 4`, INV-16). **Mitigation (built):** `selectGlimpse.dominantShape`
refuses a flat/tied rhyme (`minRhyme`/`minRhymeMargin`) — a worn player must
**confidently** rhyme one shape (Sella→`distanceFromGroup`, Watcher→`hoardedScore`/
`soloMiningRatio`, Stoop→Bow `violationRatio`). It is the same gating bias the keeper
already uses, pointed at an offline member — **collective, never a callout**. The shape
worn is the one the *group* is closest to, never a per-player accusation of the absent
friend.

### Risk D — the offline player can log back in mid-beat.
If Brann logs in while his skin stands in a lake, two Branns = instant break.
**Mitigation (HALF built — the real gap).** Three layers: (1) `selectGlimpse` filters
online players out at enqueue; (2) `wornPlayerEligible` re-checks offline **at fire** in
`doEnact` and drops the beat (`skipped("worn-online")`); (3) on **rejoin**, despawn any
live apparition wearing the joiner's UUID. Layers 1–2 exist. **Layer 3 does not exist
yet** — `NamedMobBeat`'s javadoc references `PresenceListener.despawnApparitionsWearing(uuid)`
but `PresenceListener.onJoin` has **no such call and no such method** (verified). This is
the single most important remaining build item (§6.1).

### Risk E — collective law: never punish the group for an absent member.
Gating *on* a player being absent could feel like it weaponises absence (`canon-spine §6
rule 3`). **Mitigation (built by construction):** the apparition is **neutral colorant,
not consequence** — shown to present players, tolls nothing of the absent player's, gates
nothing, never appears in a report. Absence is a *rendering condition* (we can only wear a
skin that isn't already in the world), not a judgement of the absent person.

### Risk F — orphaned-gimmick risk.
A single "ooh creepy fake-friend" with no thread is the cheap jumpscare the brief forbids.
**Mitigation (threaded, §4):** load-bearing on FACT 9 (named payoff), Brann's document
(lore home), the Surface-Walker/Watcher shapes (it is a re-skin, not a new creature), the
M1 offline-report seed (§5), and INV-16's carve separation law (`name-where-never-been`).

### Risk G — skin acquisition reliability.
Applying a real player texture to a mob needs the ModelEngine/NPC layer and can fail.
**Mitigation (built):** `applyWornSkin` never blocks (no Mojang fetch on main thread),
never throws, and on any failure leaves the dark silhouette `resolveEntity` chose —
**never a wrong, recognizable vanilla mob**. The haunting never depends on the skin
loading (`DESIGN.md §3.5`). Prefer an offline player whose profile is already cached this
session (the run wrapper should rank cached profiles first — a depth nicety, not a gate).

### What to CUT / scale down (plainly):
- **CUT** any version that walks, follows, or pathfinds.
- **CUT** name-tags on every firing; allow exactly **one** named, human-approved M4 beat.
- **CUT** chat/whisper from it. It never speaks in a friend's name — cheaper-genre
  identity-theft horror, and it violates the keeper-voice law.
- **SCALE:** at most **once or twice per run**, drama-budget paced, `maxWornPerPhase:1` so
  the same friend's face isn't reused into comedy.

---

## 3. DE-SLOP TEST — exemplar in-voice lines

The apparition is silent; these are the only text surfaces it touches. All pass the
anti-slop law (lowercase keeper/Archivist register, no named emotion, no thematic bow,
concrete, the iceberg). Must clear `registerDisciplineSelfTest` in `voice.ts` (lowercase,
no `!`, no banned meta-word) before keying.

**M1 seed report (Archivist register, names an offline player — the plant):**
> `nine days kept. the one called brann has not kept the light. he was not here to see it noted. it was noted.`

**M4 keeper-NPC line naming FACT 9 (grounds the glimpse in the real logged beat):**
> `the face over the water wore one of your own. it does that with the ones it has already begun to keep. it began with him the night he stopped coming.`

**Whisper-tier deferral if the group asks (withholds, in register):**
> `it wears what it has. you left a face here. it kept it.`

**Brann's own hand (already canon in his doc; the line the idea hangs on):**
> `it came inside, and it has not gone back out, and it walks when i walk.`

---

## 4. THREAD IT — every place it must appear (anti-orphan)

**Canon FACTs (adds NO new FACT — it is an *expression* of existing ones):**
- **FACT 9** (`canon-spine §3`) — primary home. The offline-skin glimpse IS the
  Movement-I/II haunting that M4 dialogue names as a keeper's fate re-enacted. Registered
  here (and in `bestiary-sealed §4`) as a **canonical carrier of FACT 9**.
- **FACT 16** (`canon-spine §3b`, "files the living by *place*") — the worn absent friend
  is the felt edge of "you are already filed." Shares the **separation law** with the
  `name-where-never-been` carve (FACT 16's other limb): skin = OFFLINE-only, carve =
  ACTIVE-only, never co-located (**INV-16**).
- **FACT 15** (sealed) — one of the seven foreshadows' sharpest *felt* instances: the land
  can already wear *you*. Never stated; only shown.
- **FACT 12** ("they were kept" / Brann) — the document that gives it keeper-voice.
- **INV touched:** INV-16 (separation law — built into `selectGlimpse`); INV-1 (all text
  via `voice.ts` keys, never inline; skin/name is payload data, never English at a call
  site).

**Found-documents / records:**
- `arc/lore/documents/do-not-close-your-eyes-here.md` (**Brann**) — already carries the
  seed line; **no edit required**. Optional later-hand marginal pointer tying "what walks"
  to "a face that was here and is not." Keep ambiguous.
- The **M1 seed report** (Archivist) naming an offline player for a lapse — authored as a
  `reportObserved` voice variant (§5). **NOT YET KEYED** (§6.1).

**NPC / Watcher voice keys:**
- New `discord/src/voice.ts` showrunner key for the **M4 FACT-9 naming line** (§3, line 2),
  human-approved, fired at the M4 gate. **NOT YET PRESENT** (verified — no fact9/worn key
  in `voice.ts`).
- Optional themed whisper-deferral key (§3, line 3).

**Cipher(s) / puzzle(s) — reuse the 11, add none:**
- **Atbash / mirror** (`forge/ciphers.ts atbash`) — Sella's cipher, the reflection-reading
  verb. The reflection firing **shares the lake surface** (`the_far_water`) with the
  existing Atbash reflection puzzle (`bestiary-sealed §2.2` cross-tie). "read the other
  side" is the verb that lets the group read the worn face: the mirror-cipher made flesh —
  a friend, reflected wrong.
- **Caesar** (`forge/ciphers.ts caesar`) — Vaun's cipher, for the Watcher-edge variant
  worn by the hoarder (held-back-by-a-fixed-amount → "one of us, held back").
- No NEW cipher. It rides ciphers the keepers already own.

**Beat / listener / orchestrator / sites / pacing (real symbols):**
- **`NamedMobBeat`** (`beats/lib/NamedMobBeat.java`) — the realizing beat. **BUILT:**
  `skin_player`, `offline_only`, `applyWornSkin`, `worn_skin` PDC tag, `retreating`,
  fire-time re-check, silhouette fallback, `wornSkinSelfTest`.
- **`offline-skin.ts selectGlimpse`** (`discord/src/showrunner/`) — **BUILT:** precision
  gate (`dominantShape`), one-shot budget (`maxWornPerPhase`), separation law
  (`carveClaimsByPlayer`), deniable-vs-named name-tag rule, human-approval gate.
- **`PresenceListener.onJoin`** — **GAP:** must call a new
  `despawnApparitionsWearing(uuid)` sweep (Risk D layer 3). See §6.1.
- **Sites (`sites.yml`):** `the_far_water` (Sella reflection — present), heatmap hot cell
  (Watcher edge — `HeatmapAccumulator`), `bow_marker_01` / `keeper_stone_*` (Stoop variant
  — present). All already enabled.
- **Gating signals:** `distanceFromGroup`, `hoardedScore`/`soloMiningRatio`, Bow
  `complianceFor("the_bow").violationRatio()`; offline status from `players.last_seen` /
  live `Bukkit.getPlayer(uuid)==null`.
- **Pacing:** `DramaBudget` + cooldown; plus `maxWornPerPhase` one-shot per face.

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for" seed

**THE PLANT (Movement I, inert/ambiguous).** A single Archivist report names an OFFLINE
player for a lapse:
> `nine days kept. the one called brann has not kept the light. he was not here to see it noted. it was noted.`

At the time this reads merely as *"the record grades you even when you're logged off"* — a
deniable early dread. It plants two things without announcing either: (a) the record holds
you while you are absent, and (b) a specific named friend, Brann, is the one it has its eye
on while he's gone. On the surface it is also an ordinary FACT 1 / grading beat, so it is
never a dangling oddity.

**THE PAYOFF (Movement III glimpse → Movement IV naming).** In M3 the reflection wears
**Brann's skin** while Brann is offline, to the player rhymed to the shape. The M1 report
**re-reads**: that line wasn't just "the record watches you logged off" — it was the
record telling us it had already begun to keep Brann while he was away. *"it was noted"*
was the face in the water. In M4 the keeper-NPC names it (FACT 9, §3 line 2), closing the
loop: the early report was the **first frame** of the thing the group only now understands.

No payoff without this plant; the plant exists only for this payoff (with the FACT-1
surface as cover).

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

- **Lives in:** Movement III (first, deniable glimpse) and Movement IV (named escalation +
  payoff). Seed plant in Movement I. Recontextualised, not re-fired, in Movement V.
- **Depends on (must exist first):** the generic shapes (Watcher / Surface-Walker / Stoop)
  established M1–M2 (it is a re-skin); FACT 9 delivery wiring (M4 keeper-NPC reads the
  logged beat); the M1 offline-report plant (§5); `PresenceListener` offline truth +
  `NamedMobBeat` (both exist); the `name-where-never-been` carve lane publishing its
  per-player cell claims (for the separation law); precision signals (all measured).
- **Depended on by:** nothing gates on it (it gates nothing — collective law). It is a
  **depth payoff**, not arc-spine. The arc completes without it; it makes the arc land
  harder.
- **Priority: P2 (depth).** Wins the ARG-critic and friend-group bars, but NOT
  vertical-slice-critical and NOT arc-spine-load-bearing. Build the generic shapes and
  FACT 9 first (P0/P1); add this layer once the slice is proven — its whole power is
  parasitic on shapes and a fact the group must already know.

### 6.1 The THREE real remaining gaps (the build checklist)

1. **`PresenceListener.despawnApparitionsWearing(UUID)` + the `onJoin` hook (P2, ~30 LOC,
   plugin).** The keystone for Risk D layer 3. On join, sweep loaded entities for the
   `worn_skin` PDC tag matching the joiner's UUID and remove them **reveal-disciplined**
   (`ctx.reveal().isHidden` with the standard retry, mirroring `NamedMobBeat`'s despawn
   path) so a returning player never coexists with their worn shape. The beat already
   writes the tag; only the sweep is missing. Follow the listener's reference pattern
   (Safety-wrapped, main-thread entity reads).
2. **`offline-skin.run.ts` + `offline-skin.selftest.ts` (P2, TS).** The pure
   `selectGlimpse` has no caller. The run wrapper must: gather offline candidates with
   measured `shapeRhyme`, read the carve lane's `carveClaimsByPlayer` this window, propose
   a cell, gate the `named` phase on dashboard approval, and emit the `NamedMobBeat` intent
   with `name_visible:false` in the deniable phase. The selftest mirrors the repo idiom
   (imports the pure module with nothing; asserts: online filtered out, flat rhyme
   skipped, carve-collision skipped, deniable never name-tags, named withheld without
   approval).
3. **Two `voice.ts` keys (P2, TS).** The M4 FACT-9 naming line (§3 line 2, human-approved)
   and the M1 offline-report plant (§5, `reportObserved` variant). Optionally the whisper
   deferral (§3 line 3). All must pass `registerDisciplineSelfTest`.

### New code symbols (precise):
- `PresenceListener.despawnApparitionsWearing(java.util.UUID worn)` + a call in `onJoin`.
- `discord/src/showrunner/offline-skin.run.ts` (imperative wrapper) and
  `offline-skin.selftest.ts`.
- `voice.ts`: `offlineSkinNamedM4` (FACT 9), `offlineReportPlant` (M1); optional
  `offlineSkinWhisper`.
- **Already built (do NOT re-implement):** `NamedMobBeat.skin_player/offline_only/
  applyWornSkin/retreating/worn_skin PDC/wornPlayerEligible/wornSkinSelfTest`;
  `offline-skin.ts selectGlimpse/dominantShape/OFFLINE_SKIN_DEFAULTS`.

---

## APPENDIX — non-negotiables checklist (sealed restatement)

- [x] Reveal discipline: reflection/edge/crouch/retreating only; `no_ai_drift`; despawn
      unwitnessed. (despawn-on-rejoin sweep = §6.1 item 1, pending)
- [x] Precision: worn skin = offline player confidently rhymed to the shape; measured only.
- [x] Collective: shown to present players; tolls nothing; gates nothing; never a callout.
- [x] Never punishes absence: absence is a render condition, not a judgement.
- [x] Separation law (INV-16): skin OFFLINE-only, carve ACTIVE-only, never co-located.
- [x] FACT 15 unspoken: the glimpse foreshadows induction; the M4 line names FACT 9, never 15.
- [x] Deterministic fallback: silhouette if the skin won't load; haunting never needs art.
- [x] Keeper voice intact: it is silent; Brann's night-only voice carries the lore.
- [x] Name-tag default off; exactly one human-approved named beat at M4.
- [x] One-shot per face (`maxWornPerPhase:1`), drama-budget paced; ≤1–2 firings per run.
