# The Observance — MASTER PLAN (the one buildable roadmap)

> **Spoiler-free.** Safe for Ethan and the dashboard. This file fuses the four
> design deliverables — `design/atmosphere-stack.md`, `design/cipher-web.md`,
> `design/bestiary.md`, `design/immersion-blueprint.md` — into a single,
> internally-consistent program mapped onto the **existing** engine (the Paper
> plugin beat library, the `puzzles`/`solves`/`beat_queue` schema, the Discord
> bot, the Vercel dashboard, and the between-session showrunner), obeying
> `DESIGN.md`'s anti-jank contract literally.
>
> It states no story and never names the sealed twist. Anything twist-bearing is
> a one-line pointer (see §4). Read alongside `DESIGN.md` (the bible), `FLOW.md`
> (pacing/gates), `ORACLE.md` (the resolver contract), and `arc/lore/canon-spine.md`
> + `arc/lore/LORE-BIBLE.md` (the corpus — operator index only; the `_SEALED_*`
> files are off-limits if you play unspoiled).

---

## 1. RECONCILIATION PASS — conflicts found and resolved

I read all four design docs **and** the actual repo (the 24 beat classes in
`plugin/.../beats/lib/`, `discord/src/forge/ciphers.ts`, `discord/src/forge/index.ts`,
`discord/src/voice.ts`, `discord/src/oracle/resolve.ts`, `discord/ORACLE.md`,
`discord/supabase/migrations/0004_oracle.sql`, the plugin-side `oracle/` package,
`signal/`, the dashboard components, and the lore canon). The four docs are
strikingly consistent with each other and with the engine. The conflicts that
**do** exist are almost all the same kind: **a design doc describes as "to build"
something the engine already shipped.** Building to those docs verbatim would
mean re-doing finished, self-tested code — a direct violation of the brief's
"honor what already compiles." Each is resolved below.

### R1 — The cipher forge is FAR ahead of `cipher-web.md`. (biggest one)
`cipher-web.md` is written as if the forge ships **6** ciphers and must **add**
rail-fence, columnar, polybius, and **fix** an "unfair" `coordEncode`. The actual
`discord/src/forge/ciphers.ts` already ships **11** pure, round-trip-self-tested
ciphers: `caesar, atbash, vigenere, substitution, bookCipher, coordEncode,
railFence, columnar, polybius, a1z26, morse` — all wired into `forgeClue`
(`index.ts` `CipherKind` union + `forgeSelfTest`). And `coordEncode` is **already
the fixed digit-glyph scheme** (`X-1280,Z64`), with the exact self-test the doc
asked for (`coordEncode reads as a real number`, leading-zero trap, 32-bit
extremes).
- **Resolution:** strike every "NEW CODE — add rail-fence/columnar/polybius" and
  "fix coordEncode" build item. They are **DONE**. The cipher-web's real,
  remaining work is **content, not code**: authoring `puzzles` rows that *use*
  these transforms, building the in-world Rosettas that teach them, and the
  steganography modality (P17) which genuinely is not built. The catalog also
  **undercounts** — the engine gives 11 cipher modalities for free, plus a1z26
  and morse the doc never mentions; treat those as two more "early/teaching"
  rungs available at zero cost.

### R2 — The six oracle `voice.ts` lines already exist.
`cipher-web.md` and `ORACLE.md §7` list "add `oracleNextClue/Lore/DeadEnd/
SideQuest/MainBeat/Withheld` to `voice.ts`." All six are present and verbatim in
`discord/src/voice.ts` (plus the `OracleVoiceKey` type).
- **Resolution:** strike that build item. The voice layer for the oracle is
  complete. New puzzle rows reference these keys; they don't add them.

### R3 — The in-world (plugin-side) Oracle is built, not a stub.
`immersion-blueprint.md` lists the "Oracle (3 entry surfaces, one resolver)" as
**built**, but `ORACLE.md §5` reads as "add `SupabaseClient.fetchOpenPuzzles()`"
and "add the answer-sign listener." The plugin already has
`oracle/OracleResolver.java`, `oracle/AnswerNormalizer.java`,
`signal/listener/AnswerSignListener.java`, and `SupabaseClient.fetchOpenPuzzles`.
- **Resolution:** the world-surface oracle path is **real**. Both surfaces share
  the resolver as designed. The only remaining oracle work is **seeding the
  `puzzles` table** (content) and the rite-sentinel bridge (P16, §3 below).

### R4 — `coordEncode` "normalization minus-sign" caveat is half-mooted.
`cipher-web.md §4.3` warns that `normalizeAnswer` strips `-`, so coordinate
*answers* must be unsigned. True — but note the forge's coord **solution** is
already authored as `"<x> <z>"` (space-joined, see `forgeClue` `case 'coord'`),
e.g. `"-1280 64"` which normalizes to `"1280 64"`. The carved *clue* keeps the
sign (`X-1280,Z64`); only the typed *answer* loses it.
- **Resolution:** keep the doc's authoring guidance (prefer direction-word answers
  for coords; list both signed/unsigned forms when a raw number is the answer).
  Add a one-line validation note to the seed: any coord `accepted_answers` must be
  pre-run through `normalizeAnswer` (so `"-1280 64"` is stored as `"1280 64"`).
  This is guidance, not code.

### R5 — `SmallStructureBeat` is **partly** built; the FAWE branch is the real gap.
All four docs lean on `SmallStructureBeat`, and `immersion-blueprint.md` flags it
"STUB." The repo shows it is **not** a stub for small cairns: it fully pastes an
**inline block list** (≤256 cells) with footprint validation, floor check,
reveal discipline (`mutateWhenUnwitnessed`), re-validation at mutation time, and
`protectedRegistry` registration. What is **missing** is the **FAWE `.schem`
branch** for large set-pieces (keeper-stones, the Undercroft rooms, the M-III A→B
swap) that the inline list can't hold.
- **Resolution:** re-scope the "finish SmallStructureBeat" item precisely: the
  small-cairn path is **done and shippable now** (the vertical slice's Structure B
  uses it as-is); the work is **adding the `schematic` payload branch** wiring
  FAWE's `ClipboardHolder.createPaste` inside the existing `mutateWhenUnwitnessed`
  wrapper (atmosphere-stack §3.3 gives the exact API). This is P0 for Movements
  II/III/V but **not** P0 for a first vertical slice (which can be all-inline).

### R6 — `UnlockBeat` is built; it needs producers, not code.
`immersion-blueprint.md` calls it "dispatcher built; needs authored producer
rows," and `ORACLE.md §4` calls it "until-now dead code." Both correct: the
`UnlockBeat` dispatcher exists and safely delegates to any other beat via
`step`/`step_payload` (with self-delegate + recursion guards). It just has no
`puzzles` rows producing it yet.
- **Resolution:** the only work is **authoring `outcome_payload.beat` rows** (the
  cipher-web seed already shows the exact shape — `{type:"unlock", payload:{step:
  "advancement_toast"|"small_structure"|"private_message"|"door_open", ...}}`).
  No Java change to `UnlockBeat`.

### R7 — Bestiary's six creatures vs. the anti-jank "Phase-2 model engine" line.
`bestiary.md` describes six **ModelEngine/MythicMobs** creatures; `atmosphere-stack.md`
and `DESIGN §4` keep model engines **Phase-2.5, optional**, with vanilla fallbacks.
These don't actually conflict — `bestiary.md §0` and §3 already state custom 3D is
"garnish" with a named vanilla fallback per creature, and every creature maps to
**already-built** beat classes (`NamedMobBeat`, `SacredAnimalBeat`,
`PrivateSound/Particle/Darkness`, `FakeBlock`, `ChestArrange`, `TorchGutter`,
`DoorOpen`, `SmallStructure`). I verified `NamedMobBeat` ships
silent/persistent/invulnerable/no-AI-drift/glow/`setTarget`/PDC-tag/reveal-
disciplined despawn, and `SacredAnimalBeat` ships PDC `sacred_beast`/persistent/
silent/glow/idempotent — exactly what the bestiary claims.
- **Resolution:** **the entire bestiary is shippable on vanilla fallbacks today.**
  The model engine (ModelEngine R4 + MythicMobs, Java-21-safe) is a **P2 cosmetic
  layer** behind a new optional `ModeledMobBeat`. The *spawn-bias orchestration*
  (read `SignalSnapshot`, pick the rhyming player **probabilistically**, cap how
  often one player is singled — bestiary §3 risk) is **new showrunner logic**, not
  new beat code, and it is the only behaviorally-new bestiary work for P1.

### R8 — `From The Fog` license note is stale in `DESIGN §4`.
`DESIGN §4` calls it "CC BY-NC-SA." Both `atmosphere-stack §8` and
`arg-deepening §4` correct it: the current release is **proprietary**; reference
techniques only, never vendor.
- **Resolution:** one-line doc fix to `DESIGN §4` (P2 housekeeping). It changes no
  code and no plan — FTF was always reference-only.

### R9 — Five new classes are correctly identified as not-yet-built.
`ResourcePackPusher`, `ModeledMobBeat`, `KeeperNpcBeat`, `VoiceListener`,
`SpatialVoiceBeat` are named across atmosphere-stack/immersion-blueprint as **new**.
Confirmed absent from `beats/lib/`. No conflict — they are net-new and phased
correctly (Pusher = P1; Keeper/Modeled = P2; Voice = P3).

### R10 — `BossBarBeat`/`BookAppearsBeat` exist (sanity check passed).
`immersion-blueprint §2` uses `BossBarBeat` and `BookAppearsBeat`; both are present
in `beats/lib/`. The per-player intimacy palette the bestiary and immersion docs
lean on (`PrivateSound/Particle/Darkness/Message/TimeShift`, `FakeBlock`,
`ItemRelabel`, `ItemSwap`, `MapMark`, `DecayCreep`) is **fully present**. No gap.

### R11 — `FACT 9` / `LORE-BIBLE TODO-3` is a real open content gap, and the
bestiary now proposes to close it.
`LORE-BIBLE §6 TODO-3`: FACT 9 ("the first hauntings were a keeper's fate
re-enacted at the group") has **no document home** — it relies on M4 dialogue.
`bestiary-sealed.md` (sealed) proposes the creatures-as-keeper-enactment as the
canonical carrier, named by M4 keeper-NPC dialogue against the plugin's own beat
log. This is consistent and is the intended closure.
- **Resolution:** the **mechanism** to close FACT 9 (M4 keeper-NPC dialogue
  referencing the logged M-I beat) is a **P2 content+dialogue task**, listed in the
  backlog. Until the Keeper NPC ships, FACT 9 is dialogue-deferred (acceptable per
  LORE-BIBLE option (b)).

### R12 — Two minor LORE wiring TODOs (D03/D06 links).
`LORE-BIBLE TODO-1/TODO-2`: `learn-them-as-we-learned-them.md` (D03) has no
`links_to`; `what-the-surface-keeps.md` (D06) is missing its second link to D03.
Non-blocking front-matter omissions.
- **Resolution:** P2 housekeeping (add reciprocal `links_to`); does not affect any
  build.

**Net of reconciliation:** the program is *much* closer to done than the four docs
imply. The forge, both oracle surfaces, the voice layer, the full per-player beat
palette, the signal tracker, the dashboard console, and the small-structure path
all compile and work. The true frontier is **(a) the FAWE schematic branch, (b)
the resource pack + pusher, (c) the between-session showrunner, and (d) authoring
the `puzzles` rows / Rosettas / set-pieces** that turn a working engine into a
played arc.

---

## 2. THE UNIFIED VISION (one page, spoiler-free)

**The Observance is one presence wearing three surfaces and speaking one voice.**
A veteran friend group joins what looks like *just their Minecraft server*. They
install nothing but one auto-pushed resource pack — a single "accept?" click that
carries the keepers' **rune font**, a mono ambient sound bed, and a few item
models. From that moment the world is quietly **measuring them** (the deterministic
Signal Tracker → the `dossiers`), and ~90% of the time it says nothing.

**Atmosphere** is the skin: a carved-rune world (the font from `runes.ts`,
rendered identically in-world and in Discord so a glyph learned at a stone unlocks
a Discord card and vice-versa), curated FAWE set-pieces *discovered, never witnessed
appearing*, a fog-thick Undercroft (a Multiverse void dimension — the one no-install
true fog), a low deniable drone, and — rarely — a tall silent figure already
standing where you round the corner. Custom 3D is garnish over vanilla; nothing
that haunts depends on the art loading.

**The cipher web** is the mind: not a linear "cipher 1 → 2 → done" but a non-linear
graph where **many doors open at once**. Eleven deterministic transforms (already
built and self-tested) plus six world-native modalities (count the markers, read a
reflection, perform a rite, decode a stego card, light a beacon sequence, arrange
items) feed the **Oracle** — a player submits plaintext in Discord `#the-record`,
via `/answer`, **or** on an in-world answer-sign, and all three hit the same
resolver against the same `puzzles` table. Some true answers are **`dead_end`s**
the watcher *acknowledges but does not open* (red-herring texture); one key fits
**two doors** (the Liar engine), forcing a re-walk; the Seventh thread gates
nothing and only rewards the curious. Difficulty comes from *finding the key in the
world*, never from a server lock — the resolver ignores `movement` by design.

**The bestiary** is the pressure: six rare apparitions (plus an optional seventh
"absence"), each a **pressure surface** — no combat, stand-and-stare, per-player
dread, gated by **real tracked signals** (the figure at your stash appears because
the tracker measured your hoard; the night-walker comes inside because you slept
when warned). They *rhyme* with prior-keeper fates without ever singling a living
player out — judgment stays collective.

**Cross-surface immersion** is the binding law: one `voice.ts` register on every
surface, clue handoffs that cross and resolve via the shared `solves` key, all
state in Supabase, and a between-session **showrunner** (Claude Agent SDK on cron)
that reads the dossiers and changes one thing overnight — so logging in tomorrow is
never the same world. The **dashboard is a director's console**: an AUTO↔CONFIRM
dial that is the autonomy/authenticity knob (the showrunner always *authors*; in
CONFIRM, Ethan controls only *when* a beat fires for the camera, never its content),
a per-player POV/reaction-asymmetry capture plan, and a master **kill-switch** as the
only safety control. The `event_log` is the timestamped index the YouTube edit uses
to find the real reactions. The whole thing is **"From The Fog, but it knows your
name"** — and the differentiator is that it is *autonomous and reactive*, not
scripted.

---

## 3. THE PRIORITIZED BUILD BACKLOG (P0 / P1 / P2)

**P0 = unblocks a playable vertical slice** (the atmosphere-stack §6 slice + a
first cross-surface oracle loop), honoring everything that already compiles. **P1 =
the full Phase-1/2 arc spine** (the showrunner, the set-pieces, the keeper NPCs).
**P2 = depth, polish, and the late-arc voice layer.** Each item names the exact
existing class/table/file it builds against.

> The structured backlog (machine-readable) accompanies this file; the prose here
> is the authoritative version. Items already satisfied by the engine (R1, R2, R3,
> R6 voice/code) are **omitted by design** — do not rebuild them.

### P0 — the vertical slice (prove the look + close one oracle loop)

1. **Build the `[PACK]` resource pack** (the rune font + 2 mono `.ogg`s +
   `sounds.json`). Source of truth = `discord/src/forge/runes.ts` (render the
   glyph atlas to `assets/minecraft/font/runes.json` + `textures/font/runes.png`;
   declare `observance:drone_low`, `observance:whisper`, `observance:cold_toll`).
   Host static (Vercel/R2), compute SHA-1. *(atmosphere-stack §2, §6.2)*
   **Deps:** none. **Maps to:** new `observance-pack.zip` + `runes.ts`.

2. **`ResourcePackPusher`** — new `PlayerJoinEvent` listener calling
   `Player#setResourcePack(url, sha1, force, prompt)`; log `ResourcePackStatusEvent`
   to the dashboard health panel. *(atmosphere-stack §2)*
   **Deps:** P0.1. **Maps to:** new class in `signal/listener/` or a small
   `pack/` package; status → `HealthPanel.tsx`.

3. **Finish `SmallStructureBeat`'s FAWE `schematic` branch.** Add the
   `{"schematic": "...", "offset":{...}, "require_floor":true}` payload branch
   that loads a cached `.schem` and pastes via `ClipboardHolder.createPaste`
   **inside the existing `mutateWhenUnwitnessed` wrapper**, footprint-pre-checks
   `clipboard.getDimensions()` with the same `Placement.isReplaceable` sweep,
   registers the bounding box with `ctx.protectedRegistry()`, and tags the paste
   in Supabase for idempotency. The inline path stays untouched. *(atmosphere-stack
   §3.2–3.3; resolves R5)*
   **Deps:** FAWE 2.15.x on the server (Java-21-safe). **Maps to:**
   `beats/lib/SmallStructureBeat.java`.

4. **Seed the first oracle loop into `puzzles`.** Apply the canonical seed
   `discord/supabase/seeds/puzzles_seed.sql` (23 rows realizing `design/clue-web.md`,
   authored kebab keys, exercising all five `outcome_type`s on built ciphers:
   `rosetta-ring` (`main_beat` → `advancement_toast`), `m1-named-habit` (`dead_end`),
   `stone-sella`/`seventh-shrine` (`side_quest`), `stone-vaun` (`lore`), `stone-mara`
   (`next_clue`), …). Then the SEALED endgame rows from
   `arc/cipher-web-seed.sealed.json` (service_role only, never spoiler-free view). All
   `accepted_answers` pre-normalized per `ORACLE.md §2`. *(the seed supersedes the
   retired illustrative block in `cipher-web.md §6`; resolves R1's "content not code")*
   **Deps:** none (forge + resolver + voice all built). **Maps to:** `puzzles`
   table (`0004_oracle.sql`); answers verified against `oracle/resolve.ts`
   `normalizeAnswer`.

5. **Build the first three set-pieces as schematics + one inline cairn.** Structure
   A "First Keeper-Stone" + Rosetta sign (FAWE `.schem`, via P0.3); Structure B
   "Offering cairn" (inline list, existing path — **no new code**); Structure C
   "doused alcove" with a `WALL_TORCH` for `TorchGutterBeat`. *(atmosphere-stack
   §6.1)*
   **Deps:** P0.3, P0.1 (font on signs). **Maps to:** `SmallStructureBeat`
   (both paths), `sites.yml`, `TorchGutterBeat`.

6. **The Watcher vertical-slice apparition** — a `named_mob` beat (vanilla
   husk/stray), silent, no-drift, glow, named in runes, spawned out of LoS ~12–20
   blocks off, paced by the drama budget. Proves vanilla + reveal discipline reads
   supernatural with **no** model engine. *(bestiary §1.1 "The Watcher"; resolves
   R7 — fallback ships first)*
   **Deps:** P0.1 (rune name). **Maps to:** `beats/lib/NamedMobBeat.java`
   (already built), `DramaBudget`, `HeatmapAccumulator`/`LocationSampler` for
   placement.

7. **The Coordinate Rosetta ("Stone of Reckoning").** A `small_structure` +
   `sign_write`/`lectern_fill` artifact teaching the digit glyphs 0–9 and the
   `-`/`,` marks beside their numerals, with a worked example, so the (already-built)
   digit-glyph `coordEncode` is *fair* in-world. Place at Vaun's stone/spawn in
   Movement I–II. *(cipher-web §4.2(b); the §4.2(a) code fix is already done — R1)*
   **Deps:** P0.3, P0.1. **Maps to:** `SmallStructureBeat`, `SignWriteBeat`,
   `LecternFillBeat`; teaches `coordEncode` (`ciphers.ts`).

### P1 — the arc spine (the async engine + the keeper layer)

8. **Build the between-session SHOWRUNNER** (Claude Agent SDK, VPS cron). The
   single highest-leverage missing piece: it drives the daily clue drip, authors
   personalized reports (scalpel, structured-output, **deterministic fallback** via
   `voice.reportObserved/reportEscalated`), tunes stone difficulty + Whisper
   budgets, queues `pending` beats for the console, and implements the AUTO↔CONFIRM
   side. *(DESIGN §2.10; immersion-blueprint §4–5; the engine of Sections 4 & 5)*
   **Deps:** reads `dossiers`/`custom_compliance`/`heatmap_cells`/`arc_state`;
   writes `beat_queue` + scheduled `#the-record` posts. **Maps to:** new VPS
   process; consumes the existing Supabase schema + `voice.ts`; honors the
   `status` pending/approved split (`ORACLE.md §4`).

9. **Author the `UnlockBeat` producer rows** so solved clues visibly open the
   world. Each is a `puzzles.outcome_payload.beat = {type:"unlock", payload:{step:
   "door_open"|"advancement_toast"|"small_structure"|"private_message"|"reveal"}}`.
   *(immersion-blueprint §7; resolves R6 — no Java change)*
   **Deps:** P0.3/P0.4. **Maps to:** `puzzles` rows → `UnlockBeat` →
   `DoorOpenBeat`/`AdvancementToastBeat`/`SmallStructureBeat`/`PrivateMessageBeat`.

10. **Author the six Keeper-Stone expeditions** (Movement II) — one `puzzles` row
    set per stone, each a **different built cipher + a different answer-verb** from
    the verb menu (`arg-deepening §1.7`): `caesar`→rotate frames, `book`→walk a
    lectern shelf, `atbash`→read a reflection, `vigenere`→keyed on a keeper name,
    `substitution`→fill a sign, `coord`→travel. Each stone is a FAWE set-piece +
    its forged clue + a reveal beat. *(cipher-web §2.2 Movement II; immersion §1
    M-II)*
    **Deps:** P0.3, P0.4, P8. **Maps to:** `forgeClue` (built), `puzzles`,
    `SmallStructureBeat`, `SignWriteBeat`/`LecternFillBeat`, `ItemFrame` dials.

11. **Wire the Liar engine** (the one-key-two-doors re-walk). Author
    `clue_vigenere_iss_warm` (`next_clue` → the dead-shrine `dead_end`) and
    `clue_vigenere_iss_name` (`main_beat`, `set_flags{iss_caught}`, beat =
    `private_message` `iss.dialogue.turns_cold`); implement the showrunner's
    flag-gated swap of `D10`'s effective outcome on `flags.iss_caught` (CONFIRM-mode
    `pending` curatorial beat), with an authored flag-gated duplicate row as the
    AUTO/asleep fallback. *(cipher-web §3; canon-spine §4; sealed details in §4)*
    **Deps:** P8 (the swap is showrunner-driven). **Maps to:** `puzzles`,
    `arc_state.flags`, `resolve.ts applyOutcome set_flags`, the Liar dialogue-flip
    `private_message` beat.

12. **The Undercroft custom dimension** (Multiverse-Core void world, ambient-light-0,
    datapack `dimension_type` fog) + the M-III A→B "room rebuilds itself" swap
    (a second FAWE schematic pasted reveal-disciplined, idempotent). *(atmosphere-
    stack §3.5; immersion §1 M-III; arg-deepening §3 Movement III)*
    **Deps:** P0.3, Multiverse-Core. **Maps to:** world-level + `SmallStructureBeat`
    (A→B); descent gated by `DoorOpenBeat` on a lectern page or a solved `main_beat`.

13. **Bestiary spawn-bias orchestration** (showrunner logic, not new beats). Read
    `SignalSnapshot` (`hoardedScore`, `soloMiningRatio`, `distanceFromGroup`,
    per-custom `violationRatio`, `mobKills`); emit the right built beat for the
    rhyming player **probabilistically** (not deterministically — bestiary §3 risk),
    capped so one player is never repeatedly singled. Ship all six creatures on
    **vanilla fallbacks** first. *(bestiary §1.1–1.6, §3; resolves R7)*
    **Deps:** P8. **Maps to:** showrunner → `NamedMobBeat`/`SacredAnimalBeat`/
    `PrivateSound/Particle/Darkness`/`FakeBlock`/`ChestArrange`/`TorchGutter`/
    `DoorOpen` (all built); reads `signal/SignalSnapshot.java` + `SignalSnapshot`.

14. **Keeper NPC framework** (Citizens2 = the presiding Keeper; ZNPCsPlus =
    per-player prior-keeper apparitions) branching on the dossier; M-IV atonement
    gating (withhold a fragment until a broken custom is honored). New
    `KeeperNpcBeat` (`NPCRightClickEvent` → JSON-chat dialogue tree reading the
    Supabase dossier). *(arg-deepening §2; immersion §1 M-IV; atmosphere-stack §7)*
    **Deps:** Citizens2 + ZNPCsPlus (Java-21-safe). **Maps to:** new
    `beats/lib/KeeperNpcBeat.java`; reads `dossiers`/`custom_compliance`; the
    M-IV dialogue node closes **FACT 9 / LORE-BIBLE TODO-3** by naming the logged
    M-I beat (read `event_log`).

15. **The Accepting rite (P16) sentinel bridge.** Keep `clue_rite_descend_bow`
    `active:false` until Movement V; the plugin posts the opaque normalized sentinel
    to the shared resolver **only after** the custom listeners confirm
    simultaneous-bow + deposits + time window, so the terminal `main_beat` inherits
    the `solves` replay-guard for free. Gate satisfiability on **ACTIVE players
    only**. *(cipher-web §5, P16; arg-deepening §1.6; immersion §1 M-V)*
    **Deps:** custom listeners (`CustomComplianceListener` pattern, built),
    `AcceptingTrigger.tsx` (built). **Maps to:** `puzzles` (sentinel row),
    `oracle/resolve.ts`, `PlayerToggleSneakEvent` sync detection, component
    validation (name+lore+PDC+`custom_model_data`).

### P2 — depth, polish, late-arc voice

16. **P17 steganography** in the forged clue-card PNGs — a pure, decodable post-step
    (LSB on the cream buffer, or a faint second rune layer in `getSigilSvg`) with a
    round-trip self-test. Prefer the **visual-rune-layer** variant (survives Discord
    re-compression; LSB may not — cipher-web risk). *(cipher-web §1 P17; the one
    genuinely-new cipher-web code item that survives reconciliation)*
    **Deps:** none. **Maps to:** `forge/templates/index.ts` + `sigil.ts`; payload
    → `accepted_answers` (e.g. hides a Vigenère key → chains P17→P3).

17. **Optional `ModeledMobBeat`** (Phase-2.5 cosmetic). A near-clone of
    `NamedMobBeat` that attaches a ModelEngine R4 rig after spawn, **degrading to
    exactly `NamedMobBeat` if the model plugin is absent or throws** (anti-jank #5).
    Pure garnish for one or two hero apparitions. *(atmosphere-stack §1.2–1.3;
    bestiary §0)*
    **Deps:** ModelEngine R4 + MythicMobs (paid/free, Java-21-safe; **not**
    BetterModel 3.x → Java 25). **Maps to:** new `beats/lib/ModeledMobBeat.java`.

18. **Phase-3 voice layer** — Simple Voice Chat (the one justified late-arc
    `[MODPACK]` install, Movements III–V). New `VoiceListener`
    (`MicrophonePacketEvent` → async Whisper STT → neutral dossier signals) +
    `SpatialVoiceBeat` (authored `.ogg`/TTS to one player), both falling back to
    `PrivateSoundBeat observance:keeper_voice` if SVC is absent. *(atmosphere-stack
    §4.3; DESIGN §4 Phase 3)*
    **Deps:** Simple Voice Chat (run-as-operator). **Maps to:** new
    `VoiceListener` + `SpatialVoiceBeat`; falls back to `PrivateSoundBeat` (built).

19. **Side-mysteries & journal evolution.** The self-rewriting base journal
    (`BookAppearsBeat`/`LecternFillBeat` `pages` NBT swap, out of sight); the Haunted
    Herd boon/transgression at the Accepting (`SacredAnimalBeat` + `DeathListener`).
    *(arg-deepening §3; immersion §2.10/2.12; bestiary §1.6)* **Deps:** P8.
    **Maps to:** built beats; showrunner authors page swaps.

20. **Lore housekeeping (R8, R11, R12).** Fix `DESIGN §4`'s stale "From The Fog
    CC BY-NC-SA" → proprietary; add D03/D06 reciprocal `links_to`; record FACT 9 as
    dialogue-delivered (closed by P1.14's M-IV node) or author the one fragment.
    *(atmosphere-stack §8; LORE-BIBLE §6 TODO-1/2/3)* **Deps:** none. **Maps to:**
    `DESIGN.md`, `arc/lore/documents/learn-them-as-we-learned-them.md` +
    `what-the-surface-keeps.md`, `LORE-BIBLE.md`.

---

## 4. SEALED NOTES (one-line pointer only)

The twist-bearing material — the true endgame puzzle plaintexts/coordinates, the
flag-gated `D10` dual payload, the Accepting component/token validation, and the
creatures' true nature — lives **only** in the sealed arc files:
`arc/_SEALED_ARC_BIBLE.md`, `arc/cipher-web-seed.sealed.json`, and
`arc/bestiary-sealed.md`. **Ethan must not open them; the showrunner and plugin
read them, he plays unspoiled.** Nothing in this MASTER-PLAN states or depends on
the sealed truth.

---

## 5. TOP RISKS to "nothing breaks" — and how the plan mitigates each

1. **The showrunner is a single point of failure for the whole async layer.** The
   clue drip, the AUTO↔CONFIRM dial, the Liar re-walk swap, and the bestiary
   spawn-bias all hang on P1.8. *Mitigation:* every showrunner output has a
   **deterministic fallback already in the engine** — the daily report falls back to
   `voice.reportObserved/Escalated`; the Liar swap has an authored flag-gated
   duplicate row for AUTO/asleep mode (P1.11); player-earned beats fire `approved`
   **without** the showrunner at all (`ORACLE.md §4`). If the showrunner is down, the
   *earned* haunting continues; only the *curated* drip pauses. Build P0 (which needs
   no showrunner) first.

2. **Cross-surface normalization drift silently breaks the Oracle.** If the TS
   `normalizeAnswer` (`resolve.ts`) and Java `AnswerNormalizer.normalize`
   (`oracle/AnswerNormalizer.java`) ever diverge byte-for-byte, a clue solvable on
   one surface dies on the other, *silently*. The minus-sign caveat (R4) is the
   sharpest edge — a future author reintroducing signed coords or hyphenated
   sentinels breaks matching. *Mitigation:* `ORACLE.md §2` pins the exact algorithm
   on both surfaces (already verified byte-identical on a tricky-input battery);
   **every** `accepted_answers` and the rite sentinel are authored **pre-normalized**
   (`[a-z0-9 ]` only); add a CI/self-test that round-trips the seed answers through
   both normalizers before any go `active`.

3. **The FAWE schematic branch could carve rock, float, or double-paste.** Large
   set-pieces are where the anti-jank contract is easiest to violate. *Mitigation:*
   P0.3 pastes **only** inside the existing `mutateWhenUnwitnessed` wrapper, runs the
   same `Placement.isReplaceable` footprint sweep the inline path already uses (on
   `clipboard.getDimensions()`), uses `ignoreAirBlocks(true)` + `copyEntities(false)`,
   protects the region, and **tags the paste in Supabase for idempotency** (anti-jank
   #9). Schematics are authored with a known solid base layer. The inline path
   (already battle-tested) is the fallback for anything ≤256 cells.

4. **Personalized "it knows me" callouts misfire (a wrong callout is worse than
   none).** The scalpel naming a transgression the tracker didn't actually measure
   would shatter the illusion. *Mitigation:* precision-over-recall is enforced in
   structure — the scalpel personalizes **only** on an overwhelming signal, returns
   **structured output validated byte-by-byte**, and every personalized beat has a
   deterministic `voice.ts` fallback; reports name a living player **only** for a
   `dossiers`/`custom_compliance` measurement (`canon-spine §6` rule 4). The bestiary
   spawn-bias is **probabilistic + capped** so it never reads as a deterministic
   callout (R7, bestiary §3).

5. **Third-party plugin/version risk on the spine (Java 21 vs 25).** A model engine
   or voice mod breaking mid-run must degrade silently, not error visibly.
   *Mitigation:* the spine depends only on **Java-21-safe, free/operator-licensed**
   plugins (FAWE 2.15.x, Citizens2, ZNPCsPlus, Multiverse-Core, PacketEvents);
   **BetterModel 3.x is explicitly blocked** (Java 25). Every garnish layer
   (ModeledMobBeat, Simple Voice Chat) has a named vanilla fallback so the haunting
   continues if it fails. Re-evaluate the model engine only the day the server moves
   to Java 25.

6. **Collective-judgment / absent-member violations.** Punishing the group for an
   offline member, or accidentally electing a "chosen one," breaks a hard constraint.
   *Mitigation:* the Accepting gate (P1.15) satisfiability checks **ACTIVE players
   only**; the bond/Whisper tally is a **neutral colorant**, never a verdict
   (`canon-spine §6` rule 3, INV-9); keeper-fate rhymes stay soft and are never said
   as "this keeper is you." Encoded as an invariant the dashboard and showrunner both
   honor.

7. **Difficulty collapsing into a countable ladder (or becoming wall-stuck).** A HARD
   web that reduces to "step N of M" bores this group; one with a single in-door
   strands them. *Mitigation:* the resolver **ignores `movement`** (out-of-order is
   allowed), so many nodes are `active` at once; the redundancy table (cipher-web
   §2.3) gives **every gate ≥2 in-doors**; `dead_end`s + the Seventh side-thread +
   the Liar's false door add red herrings without blocking; and the **Whisper
   economy** (rationed, earnable, auto-gifted on a true stall — `FLOW §3`) is the
   diegetic backstop so HARD never becomes *dead*.

8. **The kill-switch is the only safety control — it must always work.** Full
   profiling and per-player dread mean the master mute has to be absolute and
   instant. *Mitigation:* `WatcherSleepToggle.tsx` writes a `settings` flag the
   plugin poller and showrunner both read every cycle; with the engine deterministic
   and beats idempotent (`status='fired'` cross-restart guard), flipping it halts all
   new beats immediately and the in-flight set drains cleanly. It is the single
   privacy/safety lever by design.

---

> **Bottom line:** the engine is real and largely done — forge (11 ciphers), both
> oracle surfaces, the voice layer, the full per-player beat palette, the signal
> tracker, and the dashboard all compile. The path to a played arc is **finish the
> FAWE branch (P0.3), ship the pack (P0.1–2), seed the puzzles (P0.4), prove the
> slice — then build the showrunner (P1.8) as the keystone of the async, autonomous,
> "it knows your name" experience.**
