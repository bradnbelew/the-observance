# Optional Garnish — `ModeledMobBeat` + The Ear (late / optional)

> Build-ready design treatment. Reconciles the prior-session backlog items
> **MASTER-PLAN P2.17 (`ModeledMobBeat`)** and **P3.18 (Phase-3 voice layer / "The Ear")**
> against the new mystery web, specifies the genuinely-unbuilt remainder, and de-slops it.
>
> Source docs honored: `design/atmosphere-stack.md` §1.2–1.3 / §4.3; `design/bestiary.md`
> §0–§3; `DESIGN.md` Phase 3 (§4); `design/MASTER-PLAN.md` items 17–18 + reconciliation
> R7/R9; `arc/lore/canon-spine.md` (FACTs, INV-11/13/16). Built classes referenced verbatim:
> `NamedMobBeat`, `PrivateSoundBeat`, `PerPlayer`, `Reveal`, `SacredAnimalBeat`.
>
> **Verdict up front: KEEP-SCALED.** Ship `ModeledMobBeat` as a one-rig cosmetic over ONE
> hero apparition. Ship The Ear as a **keyword-spotter only** (not open transcription) on a
> hard opt-in, gating nothing. Both are P2/P3 depth — never on the vertical-slice critical path.

---

## 1. EXPOUND — the full mechanic + story + mystery treatment

### 1.0 What is already finished (do NOT re-invent)

- `NamedMobBeat` ships complete: out-of-LoS ring spawn (`findSpawn`), faces player, silent /
  persistent / invulnerable / `no_ai_drift`, PDC `beat_entity`/`beat_owner` tags, reveal-
  disciplined despawn, the **offline-skin separation law** (`skin_player` / `offline_only`,
  re-checked at fire), the **retreating** un-targeted variant (the Seventh glimpse), and the
  `resolveEntity` WARDEN→STRAY fallback that **never falls to a wrong green zombie**.
- `PrivateSoundBeat` ships complete: vanilla `Sound` or `named_sound` (resource-pack key),
  `behind`+`offset` spatialization, all three `PerPlayer` paths.
- The bestiary's six creatures are **already shippable on vanilla fallbacks** (MASTER-PLAN R7).
  The model engine is explicitly a **P2 cosmetic layer**; The Ear is **P3, additive, never gating**.

So the unbuilt remainder is exactly two new classes plus a thin wiring layer — and the
**narrative interlock** that keeps them from being orphaned gimmicks. That interlock is the
real work of this document.

### 1.1 `ModeledMobBeat` — the rig garnish

**What it is.** A subclass of `NamedMobBeat` that, *after* the parent has spawned and fully
configured the vanilla entity (silent, no-drift, PDC-tagged, facing, separation-law-checked),
attaches a ModelEngine R4 rig to that same Bukkit entity via reflection-guarded API calls. If
the model plugin is absent, disabled, or the API throws, the entity is **already** a correct,
silent, watching vanilla apparition — the rig simply never lands, and nobody can tell a beat
"failed." This is the anti-jank #5 graceful-degradation rule made literal: the garnish is a
*post-spawn decoration* on a beat that has already succeeded.

**Which apparition gets the rig (KEEP-SCALED).** Exactly ONE, and it is **The Watcher at the
Edge** (bestiary §1.1) — the canonical 3.2–3.5-block hooded silhouette. Reason: it is the only
apparition where the *shape itself* is the payoff ("it has always been the figure on the
ridge"), it appears at render distance where rig fidelity reads, and it recurs across all five
Movements so the one authored rig amortizes. A **second, optional** rig target is **"the thing
made of a past group"** — the keeper-stone watcher that wears a dead keeper's silhouette
(atmosphere-stack §1.3, the creature that "earns a custom rig"). That second rig is P2-late and
strictly skippable; the silhouette + worn-name already carries it via the existing `skin_player`
path.

**How it plays across the arc.**
- **Movement I–II:** the Watcher appears as the vanilla WARDEN/ENDERMAN silhouette. The rig
  may not even be loaded yet (or Ethan ships it dark). Players meet "a tall dark shape on the
  ridge." This is deliberately *before* the rig — so the rig, when it lands, is an escalation
  the players feel rather than a baseline they got used to.
- **Movement III–IV:** the rig is live. The same Watcher beat now renders the authored hooded
  form with the imperceptible sway + slow head-track. Because it is the *same beat* with the
  *same behavior*, no player can point to "the day it changed engines" — it reads as the thing
  becoming more *itself* as the arc deepens. (Tonal escalation shown structurally, not stated.)
- **Movement V:** the worn-keeper rig (if built) appears at the keeper-stones during the close —
  the watcher that is visibly assembled from a prior keeper's outline, carrying that keeper's
  name in runes. This is the on-camera expression of FACT 15 (to be accepted is to *become* the
  watching) without a line of dialogue saying it.

**Mystery function.** The rig is not a clue and holds no information — that is correct and
intentional (a clue that requires a paid plugin to load would violate Path A). Its job is
**recognition**: by Movement IV the group should be able to say "that's the *same* figure" across
sightings, and the consistent rigged silhouette makes that recognition unambiguous. It converts a
scatter of "tall dark mobs" into ONE recurring character — which is what lets the late re-read
("it was always the same figure, counting") land.

### 1.2 The Ear — Phase-3 voice layer (`VoiceListener` + `SpatialVoiceBeat`)

**What it is.** Simple Voice Chat (henkelmax) is the single justified late-arc client install
(atmosphere-stack §4.3, DESIGN §4). On `MicrophonePacketEvent` the server already holds the
Opus-decoded mic audio. **The Ear** is the pipeline that turns *what people say out loud* into
two things: (a) **neutral dossier signals** that make the haunting feel like it overheard you,
and (b) the ability for the Keeper to **whisper spatial audio** to one player only.

**The hard scaling call (KEEP-SCALED — read this twice).** The backlog says "Whisper STT →
neutral dossier signals." Open free-form transcription of a veteran friend group's voice chat is
a precision and privacy hazard (see CRITIQUE §2). **Scale it down to a closed keyword spotter:**
the STT pass does not store or transcribe sentences; it matches the audio against a **small,
authored watchlist of in-world tokens** — keeper names (`vaun`, `mara`, `sella`, `orin`, `brann`,
`iss`), custom names (`offering`, `bow`, `kept light`), and a handful of dread words the ARG
itself taught them (`the deep`, `the record`, `kept`). A match writes **one boolean-ish signal**
("the group said the Liar's name aloud before catching the lie") — never a quote, never a
sentence, never raw text. This keeps PRECISION over recall (you only ever react to a token the
spotter actually matched against a known list) and keeps the privacy surface to a fixed,
auditable vocabulary instead of "we transcribe your calls."

**How it plays.**
- **The overhearing (signals → later callout).** The group is in voice chat planning. Someone
  says, out loud, "let's just trust Iss, he's never lied." The spotter matches `iss` + `trust`-
  adjacent. Hours later, a found report or a Keeper line lands that *rhymes* with having trusted
  the Liar — delivered through the existing report/whisper economy, never as a quote. The effect
  the friend group feels: **it heard us.** This is the highest "it knows me" gain in the project,
  which is exactly why its precision must be airtight.
- **The keeper whisper (`SpatialVoiceBeat`).** The Keeper speaks one short authored line into the
  SVC channel, positioned spatially behind/beside ONE player — an `.ogg` (or pre-rendered TTS),
  never live-generated. Deterministic fallback if SVC is absent: it degrades to `PrivateSoundBeat`
  with `named_sound: observance:keeper_voice` (the pack sound). Same line, lower fidelity, no client
  install required — the haunting continues.
- **Arc placement.** The Ear is dark until **Movement III** (the first SVC install prompt is gated
  behind the dashboard and framed in-world as crossing into the Undercroft). It earns its install
  there because whispered spatial audio in a fog dimension is the single biggest dread beat the
  project has. Movements I–II run fully without it.

**Mystery function.** The Ear closes the loop on FACT 1 ("the record is keeping a list of the
living, by name") at the sensory layer: the record does not only file what you *do*, it files what
you *say of the ways*. The keeper-whisper in M-III–V is the first time the watching addresses a
single player by something only that player said — the most intimate "it knows me" the arc reaches.

---

## 2. CRITIQUE — adversarial, honest

**R1 — The Ear as open transcription breaks PRECISION + privacy. (Sharpest risk.)**
Full Whisper STT over a real friend group's mic is the project's biggest landmine. (a) STT mishears;
a wrong "it knows you" from a misheard word is worse than none (privacy law: personalize ONLY on a
measured signal). (b) Transcribing veterans' off-topic, profane, personal voice chat and storing it
is a trust violation the "full profiling" license does NOT cover — profiling game *behavior* is the
deal, recording conversations is not.
**Mitigation (adopted above):** CUT open transcription. The Ear is a **closed keyword spotter** over a
fixed authored watchlist; it persists only a typed signal flag, never audio or text; the watchlist is
checked into the repo and visible on the dashboard; the dashboard master kill-switch silences the
listener like everything else; and there is a hard "the Ear is asleep" default-off until M-III opt-in.
Precision becomes trivial because the only thing it can ever react to is an in-world token it was told
to listen for.

**R2 — `ModeledMobBeat` as a separate beat class risks divergence from `NamedMobBeat`.**
If it is a *clone*, every separation-law / reveal / despawn fix to `NamedMobBeat` must be duplicated —
exactly the orphan-divergence the consistency law forbids in code form.
**Mitigation:** Do NOT clone. `ModeledMobBeat extends NamedMobBeat` and overrides a single new
`protected` post-spawn hook (`decorate(LivingEntity, BeatContext, BeatRequest)`) that the parent calls
at the end of `doEnact`. The parent gets a no-op default hook (zero behavior change to the built class);
the subclass fills it with the rig attach. One spawn path, one separation law, one despawn.

**R3 — A rig that loads mid-arc could be *witnessed* changing (reveal-discipline break on camera).**
If a player is looking at the vanilla silhouette the instant the rig attaches, they see it pop into a
hooded model — the exact "witnessed mutating" defect.
**Mitigation:** the rig attaches **synchronously inside the same `doEnact` tick, before the entity is
ever revealed** — i.e. while still out of LoS (the parent already only spawned out of LoS). The entity
is rigged-or-not at the moment of first sight; it never transforms in view. Across *Movements* the
look "escalates" only because Ethan toggles the rig between sessions when no one is online, never live.

**R4 — Orphaned-mechanic risk: a paid rig that carries no story is a gimmick.**
If the rig is just "prettier mob," it violates the no-orphaned-mechanics law.
**Mitigation:** it is threaded (§4) to a real narrative job — **recognition / "it was always the same
figure"** and the M-V worn-keeper FACT-15 expression — and it is explicitly NOT a clue (clues stay
vanilla, Path-A-safe). Its narrative home is "consistency of the recurring character," which is a
legitimate, non-decorative function.

**R5 — SVC install fractures the group; some won't install.**
The collective-ending law forbids punishing the group for an absent member; an SVC-only beat could
disadvantage non-installers.
**Mitigation:** the Ear gates NOTHING. Every keeper-whisper has the `PrivateSoundBeat` fallback;
every spotter signal is a *colorant* on the neutral report economy, never a gate. A player who never
installs SVC gets the pack-sound whisper and is never blocked from any door. INV-11 holds: ending reads
ACTIVE-player tallies only, and the Ear contributes none of them.

**R6 — Whisper STT latency / model offline could stall a beat (anti-jank: nothing breaks if the model
is slow/offline).**
**Mitigation:** the spotter runs **fully async** off the main thread; it never holds a beat. Keeper
whispers are *authored audio*, not model output, so they have zero STT dependency. If the spotter is
slow or down, the only consequence is a missed colorant — no beat waits on it, no world write depends
on it. Main-thread world writes stay in the beats, never in the listener.

**What to CUT outright:** open free-form transcription; any persistence of audio or text; any
second/third rigged apparition beyond the one Watcher (the worn-keeper rig stays optional/P2-late);
live TTS in the SVC channel (pre-render only). What to KEEP-SCALED: rig on one hero mob; spotter on a
closed watchlist; keeper-whisper with vanilla fallback.

---

## 3. DE-SLOP TEST — exemplar lines (cold, plain, concrete)

*The keeper-whisper, M-III, spoken to one player after the spotter matched the Liar's name:*
> you said his name like it was safe to say.

*Archivist report fragment that lands hours after the group trusted Iss aloud (no quote, the iceberg):*
> seven days kept. the name they leaned on is in the record under a mark i will not write twice.

*The worn-keeper watcher's rune name at a keeper-stone, M-V (plain, a fact, not a threat):*
> kept here before you. kept here still.

*Dashboard-internal log line for the spotter (register holds even in tooling — it counts, it does not emote):*
> heard: keeper-name token, 1 of 6. no text retained.

All four avoid named emotions, tidy bows, three-adjective lists, melodrama, and em-dash drama. The
threat in line 1 is in what it omits (why is saying his name *not* safe?); line 2 withholds the mark;
line 3 states a fact and lets the player do the dread.

---

## 4. THREAD IT — where this must appear so it is not an orphan

**Canon FACTs / INV touched or added.**
- **NEW: FACT 17 — "The record files what is *said* of the ways, not only what is done."** Child of
  FACT 1 (the list of the living, by name). M3→M5 · FORESHADOW→REVEAL. *Paths:* the keeper-whisper that
  repeats a token the group spoke aloud; an Archivist fragment that names "what was said over the deep."
  Inert meaning early: a keeper muttering about loose talk. True meaning: the watching has been listening
  at the voice layer the whole late arc. Realized **only** through The Ear's spotter + `SpatialVoiceBeat`;
  fully degradable so the FACT still lands via pack-sound whisper for non-SVC players. → ties FACT 1, 15.
  *(This is the next free synthesis-minted integer after the frozen namespace 16/10b/2b/7b/13b — confirm
  in the synthesis namespace pass before canonizing; do not self-mint elsewhere.)*
- **Touches FACT 15** (becoming the watching): the M-V worn-keeper **rig** is a visual expression of it.
- **Touches FACT 1 / FACT 16** (filed by name / by place): The Ear adds a "by word" axis under FACT 1.
- **Obeys INV-11** (active-player tallies only — the Ear contributes none), **INV-16** (no surface names
  the ending — the whisper foreshadows, never blurts), and the privacy/precision law (closed watchlist).

**Found-documents / journals that must foreshadow it.**
- An **Archivist fragment** (`arc/lore/documents/`) planted in **Movement II**: "*against each name, a
  ground; against each ground, what was said over it.*" Inert (reads as place-filing, ties FACT 16); pays
  off when the keeper-whisper repeats your own word.
- A **Brann (Night-Walker) hand-fragment** warning, in his voice, about talk after dark: "*do not say the
  names out loud here. i said one. it answered.*" Plants both the Ear and his FACT-5 fate-rhyme.
- The **self-rewriting base journal** (MASTER-PLAN #19, `BookAppearsBeat`/`LecternFillBeat` page swap)
  gains, out of sight in M-IV, one line that quotes a *token* the group used — same closed-watchlist
  vocabulary, so it can never quote something the spotter didn't authorize.

**NPC / Watcher voice lines that carry it.**
- The Keeper's `SpatialVoiceBeat` lines (M-III–V), authored `.ogg`, one per matched token category
  (name / custom / dread-word), each with a `PrivateSoundBeat observance:keeper_voice` fallback twin.
- The Watcher carries no voice (it is silent by canon, bestiary rule #1) — the **rig** is its only new
  expression; it never speaks.

**Cipher(s) / puzzle(s) that express it.** The rig and the Ear are deliberately **not gating puzzles**
(Path-A law). They *brush* existing ciphers without owning them:
- The worn-keeper rig appears at keeper-stones whose names are already encoded in the per-keeper cipher
  (Caesar/Vaun, bookCipher/Mara, Atbash/Sella, substitution/Orin, beacon-colour/Brann, **Vigenère/Iss**).
  No new cipher; the rig just *wears* the name the existing carving already ciphers.
- The Ear's watchlist tokens are the **decoded plaintexts** of those same carvings — so a group that
  *solves* a keeper-name cipher and then *says the solved name aloud* triggers the spotter. The puzzle and
  the Ear share a vocabulary (one source of truth = the keeper names in `canon-spine §1`), so a name
  learned at a stone and a name spoken aloud are the same token. (This is the cross-surface-truth law made
  concrete: the rune alphabet / decoded names are shared between stone, Discord card, and the Ear.)

**Beat classes / listeners / tables / seed rows / sites.yml / voice keys to realize it.**
- **New `plugin/.../beats/lib/ModeledMobBeat.java`** — `extends NamedMobBeat`; overrides the new
  `protected void decorate(LivingEntity, BeatContext, BeatRequest)` hook (added as a no-op to
  `NamedMobBeat`) to attach a ModelEngine `ModeledEntity` + `ActiveModel`, all reflection/try-guarded so
  absence degrades to plain `NamedMobBeat`. Payload adds `model_id` (e.g. `watcher_hooded`).
- **New `plugin/.../signal/VoiceListener.java`** — `MicrophonePacketEvent` handler; async Opus→keyword
  spotter over the authored watchlist; writes a typed signal via the existing tracker/`SignalSnapshot`
  path (a new `heardTokens`-style neutral counter), never audio/text. Honors the dashboard kill-switch.
- **New `plugin/.../beats/lib/SpatialVoiceBeat.java`** — plays an authored `.ogg`/TTS into the SVC channel
  for one player; **fallback** to `PrivateSoundBeat`(`named_sound: observance:keeper_voice`) when SVC is
  absent (reuse `PerPlayer.namedSound`).
- **Resource pack** must define `observance:keeper_voice` in `sounds.json` (the fallback key) — the pack
  lever already planned in atmosphere-stack §2/§4.2. **Voice keys:** add `keeper_whisper.name`,
  `keeper_whisper.custom`, `keeper_whisper.dread` to the showrunner voice config alongside the existing
  `voice.ts` register, each pointing at an authored `.ogg`.
- **Tables / seeds:** a small `heard_tokens` (or a column on the existing dossier) for the spotter's
  typed flags; a checked-in `voice_watchlist` seed (the closed token list) so it is auditable. No new
  cipher seeds — reuse `puzzles_seed.sql` keeper-name plaintexts as the watchlist source.
- **sites.yml:** the M-V worn-keeper rig anchors on the existing keeper-stone site entries (no new sites);
  the Watcher rig rides the showrunner's existing heatmap placement (no site change).
- **MASTER-PLAN:** mark items **17 and 18** as "spec'd in `design/ideas/backlog-modeled-mob-and-voice.md`;
  KEEP-SCALED (one rig, spotter-not-transcription)."

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" seed

**Plant A (the rig — recognition).** In **Movement I**, the Watcher is met only as a tall dark
*vanilla* silhouette and a sound-behind-you with no source (already the bestiary §1.1 foreshadow). It
reads as "a creepy mob." Inert. **Payoff in Movement IV:** once the rig is live and the same hooded form
has recurred at the ridge across sessions, the group realizes — without being told — that every "tall
dark shape" since Day 1 was **the same figure**, patient, counting. The rig is what makes that
recognition possible; the early vanilla sightings are what make it a *re-read* rather than a reveal.

**Plant B (the Ear — overhearing).** In **Movement II**, the Archivist fragment "*against each name, a
ground; against each ground, what was said over it*" is found and reads as place-filing (ties FACT 16).
Inert and ambiguous. **Payoff in Movement III–V:** the first keeper-whisper repeats a token the group
actually said aloud in voice chat ("you said his name like it was safe to say"). The Movement-II line
retroactively means *the record was logging your talk the whole time* — the plant pays off as FACT 17.
No payoff without that plant; the M-II fragment exists specifically so the first whisper is a
recognition, not a jump-scare.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Element | Movements | Depends on | Depended on by | Priority |
|---|---|---|---|---|
| `ModeledMobBeat` (Watcher rig) | III–V live; vanilla in I–II | `NamedMobBeat` (built) + new no-op `decorate` hook; ModelEngine R4 (paid, Java-21) | Plant-A recognition payoff (M-IV); narratively, nothing *gates* on it | **P2 (depth)** |
| Worn-keeper rig (2nd, optional) | V | `ModeledMobBeat`; `skin_player` path (built); keeper-name ciphers (built) | FACT-15 visual expression (skippable) | **P2-late / cut-safe** |
| `VoiceListener` (The Ear, spotter) | III–V (dark until M-III opt-in) | Simple Voice Chat (operator install); closed watchlist seed; tracker path (built) | FACT 17 payoff; keeper-whisper relevance | **P3 (depth)** |
| `SpatialVoiceBeat` (+ fallback) | III–V | `VoiceListener` (for targeting) OR none (can fire authored whispers without a match); `PrivateSoundBeat` fallback (built) | the M-III–V intimate "it knows me" beat | **P3 (depth)** |
| FACT 17 + M-II Archivist plant | II (plant) → III–V (payoff) | none to plant; the Ear to fully pay off (degrades to pack-sound whisper otherwise) | the late "it heard us" re-read | **P2 to plant, P3 to fully realize** |

**Critical-path note:** NONE of this is on the P0 vertical slice (atmosphere-stack §6 proves the look on
pure vanilla `NamedMobBeat` + `PrivateSoundBeat`). It is all additive garnish with a named vanilla
fallback at every call, exactly as the backlog framed it. Build order: no-op `decorate` hook on
`NamedMobBeat` first (zero-risk, enables the rig later) → spotter watchlist seed + `VoiceListener`
(async, kill-switched) → `SpatialVoiceBeat` with fallback → rig last, only if the look needs it.
