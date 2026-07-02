# THE OBSERVANCE — MINECRAFT INTEGRATION CATALOG

> Canonical (replaces the old drifted integration plan). Companion to [OVERHAUL.md](OVERHAUL.md)
> and [PUZZLES.md](PUZZLES.md). The full menu of what can be built into the world, by **layer**
> (resource pack · datapack · plugin · command-tier · external · recording), each tagged with what
> it SERVES and its status. Path A holds: friends install **only the auto-pushed resource pack**;
> all else is server-side. Vanilla-first — every clue degrades to the no-pack column (the pack
> carries *look*, the datapack carries *fog/mood*, the plugin carries *reactivity*; none is a single
> point of failure for a clue).

Legend — serves: 📖story 🧩puzzle 👻scare 🗺️lore 🧑character 🔗clue 🌌immersion · status: ✅built ◑partial ⬜todo

---

## LAYER 1 — RESOURCE PACK (cosmetic, no-install, auto-pushed)

- ◑ **Rune/glyph font** (`runes.png` + `runes.json`) — the carved-clue look. 🧩🔗🌌 *(built; needs PUA
  codepoint fix so it can't collide with vanilla text).*
- ⬜ **Custom sounds (THE missing layer)** — mono OGG Vorbis for the 4 declared events + more:
  whispers, drones, the cold toll, stone-breath, keeper voices, stings, the black-moon bell. 👻🌌🧑
  **P0 — without these every sound beat plays silence.** Source CC0 + ElevenLabs TTS → `ffmpeg -ac 1`.
- ⬜ **`custom_model_data` relics** (1.21.4 `item_model` + `range_dispatch`) — "letters from the deep,"
  map fragments, keeper tokens, **the Lens/reading-glass**, the record-ledger book. Render
  **dormant→warm→cold** as the plugin sets the component (story + item move in lockstep). 🔗🗺️🧑
- ⬜ **Carved block textures** — keeper-stone, the cold hearth, ash, deep-line stone, the record block. 🌌🗺️
- ⬜ **Apparition reskins** — texture-swap vanilla mobs (NOT ModelEngine, NOT a mislabeled Warden):
  a pale, wrong figure on a Husk/Stray base. 👻🧑
- ⬜ **GUI/title font + "wrongness" pack** — a UI font for the record; a few subtly-off textures that
  bank dread without announcing themselves. 🌌
- ✅ **`minecraft:illageralt` fallback** — the no-pack degrade path (real SGA cipher, decodable). 🧩

## LAYER 2 — DATAPACK (server-side, no-install) — *none exists yet; whole layer is ⬜*

- ⬜ **The Undercroft dimension** — `dimension_type` with `the_nether` fog, `has_skylight:false`,
  `ambient_light:0.0` = sealed pitch-dark with thick near-fog. 👻🌌 **P0 atmosphere.**
- ⬜ **Custom biomes** — cold near-black `fog_color`; **`mood_sound` = `observance:whisper`** so the
  *vanilla engine self-generates* the cave-whisper scare with zero plugin tick. 👻🌌 (≤3 biomes —
  MC-211878 drops fog past ~12.)
- ⬜ **Hidden advancements** — diegetic toasts ("the record notes you" / "receives you"); the seed
  already references `observance:the_ring_is_whole` etc. 📖🔗
- ⬜ **Functions / predicates** — scripted micro-sequences (tellraw/particle/sound/structure-load)
  with no plugin code; cheap scares + lore reveals. 👻🗺️
- ⬜ **Custom recipes / loot** — craft a keeper-token or the Lens; "recovered" loot in structures. 🧩🗺️
- ⬜ **`/place structure` + jigsaw** — force-place load-bearing vanilla structures (ancient city /
  trial chamber / village) at known coords via datapack `/place structure` or seed-select + `/locate`;
  use vanilla jigsaw system for modular set-piece assembly. Precision sites go here, not to
  vanilla-gen randomness. 🗺️⚙️
- ⬜ **Vanilla structures re-dressed (A12).** Use natural Minecraft generation for the world's bones,
  re-dressed additively so it reads as ours — by CODE (the A11 dresser pass), never by hand:
  - **Ancient City (deep dark) = the Undercroft / keeper-stone sites** — a built-then-abandoned
    civilization drowned in sculk; plugs directly into A10 (sculk = Watcher's sense) and licenses
    a legitimately-placed Warden as ambient dread (gated so it can't wipe a convergence beat).
  - **Trial Chamber vault (1.21) = the asymmetric co-op vault** — per-player keys are vanilla
    behavior; the vault backs the Threshold with near-zero plugin overhead.
  - **Village = surface town** (Aro/Wenna/Dob/Pell) — dressed + Citizens2 townsfolk.
  - **Mineshaft / stronghold / ruined portal / ocean ruins** = "recovered ruins" lore anchors.
  Division of labor: vanilla-gen for connective tissue + dread-texture + the vault; code-placed
  (A11) for load-bearing precision puzzles (never leave a comparator-read bookshelf lock to raw
  generation). Veterans recognize raw vanilla → always apply the dresser pass. 🗺️🌌

## LAYER 3 — PLUGIN (Paper — the reactive engine) — *core built, signature features TODO*

- ✅ **Flag/storylet gate engine** (0006 + FlagGate twin) — the progression spine. 🧩📖
- ✅ **Beat library (23 types)** + queue/poller + reveal discipline + drama budget. 👻🌌
- ◑ **Per-player illusion primitives** — `fakeBlock`/`isHiddenFrom` built; **ADD `showEntity`
  (per-player entity visibility), per-player packet light (dims when watched), per-player fog.** 👻🧩🌌
  *The "it knows ME" feel + the asymmetric co-op vault depend on these.*
- ⬜ **Display + Interaction entities — the illusion backbone.** `text_display` / `block_display` /
  `item_display` (packet, per-player, transform-animated) for floating runes, block-built faces /
  figures, glitch-corruption text, "the thing in the trees" one player sees. **Interaction entities**
  for clickable diegetic buttons / NPC surfaces without Citizens. These replace the CUT ModelEngine
  bestiary illusion — the bestiary *lore* is salvaged into vanilla-reskin + display apparitions.
  The FAWE async bug (below) must be fixed before these can be pasted reliably. 👻🧩🌌
- ⬜ **Composure signal (Observer Tier 0).** A per-player behavior accumulator: time in dark, recent
  damage, alone-vs-grouped, hoarding one item, revisiting one block. The Watcher speaks in
  *implication* grounded in this signal with zero chat/voice/LLM. Extends the existing Attention
  layer. "Behavior-heard" variant of PUZZLES §1 voice-heard. 👻🧑
- ⬜ **Asymmetric co-op controller** — partitions clue-fragments over the **active roster** at
  solve-time (dynamic-N), shows each player their piece via `showEntity`. 🧩🌌
- ⬜ **The Observer Engine** — in-game chat (`ChatListener` ✅) + Discord text + Discord **voice
  (Whisper)** → LLM archivist extracts **grounded** observations → sparse, precise weaponization
  (quote real words/plans back). 👻🧑📖 *(needs the bot hosted always-on; build after the loop runs.)*
- ⬜ **The Lens / reading-glass** — a held custom item revealing per-player runes/clues otherwise
  invisible (`showEntity` gated on holding it). Diegetic "second sight" + a puzzle key. 🧩🔗🌌
- ⬜ **The record block / lectern terminal** — an in-world block that opens "the record" (book/GUI),
  the in-game face of the website. 📖🔗
- ⬜ **Behavior-answer listeners** — bow-in-order, build-an-answer, walk-a-rune, hold-a-sequence,
  coords-arrival (extends `CustomComplianceListener`/`TerritoryListener`). 🧩 (PUZZLES §4)
- ⬜ **A3 Minecraft-native listeners** — sculk-corridor silence detector, villager-trade oracle
  watcher, item-frame dial reader, bookshelf-register comparator reader, NBT item inspector. 🧩
- ⬜ **The missing flag producers** — Ignition/CoopPlate/SeventhChoice/UnlitDeep (the arc can't start
  without Ignition; `/obs flag` ✅ proves gating meanwhile). 🧩📖
- ⬜ **Desire-paths** — read `heatmap_cells` (✅ tracked) → place a worn path / the future-dated grave
  on a player's most-walked route. 👻🗺️ ([backlog](ideas/backlog-desire-paths.md))
- ◑ **NPC framework — D4 hybrid (resolved 2026-06-30).** Citizens2 for human-passing NPCs (the
  companion Wren; surface townsfolk Aro/Wenna/Dob/Pell). Vanilla-uncanny (armor-stand / display +
  PDC + Interaction entity) for non-human / uncanny (the six keepers, apparitions, the Watcher,
  statue-things). `KeeperNpcBeat`/`Listener` (PDC-tag) stay for the keeper layer. Citizens2 version
  must be pinned against the same Paper 1.21.x as the rest (D5). 🧑🗺️
- ⬜ **Director structure-generation system (D7w/A11).** Two code-driven sources only — no hand-
  building by Ethan required: **(a) Vanilla-gen dresser:** the plugin overlays runes/carvings/decay/
  lore onto located vanilla structures additively (never overwrites). **(b) Procedural code-gen:**
  the director builds primitives (keeper stone, cairn, answer lectern, plate) AND set-pieces
  (reflection room, bookshelf-register room, threshold) from block templates + modular jigsaw pieces
  assembled by code. A **`/observance survey` / `site set`** command captures coords by walking-and-
  clicking. Schematic-stamp (FAWE) is available if Ethan opts to author a piece, but is never
  required. **R&D sub-task (quality):** learn cohesive procedural building — tight block palette,
  strict lighting (dark default, light earned), decay/wear passes, symmetry + modular jigsaw, rule-
  placed "wrongness" details, FAWE relight after every write. Without this craft, the zero-manual
  world looks generic. Validate in Playtest 1. 🗺️⚙️
- ⬜ **Reflection / display puzzles** — `TextDisplay`/`BlockDisplay`/`ItemDisplay` beats (none exist):
  floating runes, looming player-head faces, mirrored carvings. 🧩👻🌌
- ◑ **RoomSwap → teleport** — rework from in-place mutation to sealed-door + teleport-on-reentry. 👻
- ◑ **FAWE paster → async** — currently main-thread (tick-stall bug); wrap async + `fastMode(true)`
  + `changeSetNull()` + relight. **Must be fixed before display-entity pastes or any dresser pass.**
  ⚙️

## LAYER 4 — COMMAND-TIER (cheap, high-leverage, no new systems)

- ⬜ Title/subtitle/action-bar intrusions ("the record glitched"). 👻📖
- ⬜ Bossbar = a presence/countdown meter; scoreboard = the record's **count** (diegetic). 🌌📖
- ⬜ Tellraw written books; sign/lectern puzzles. 🧩🗺️
- ⬜ Player-head item displays (looming faces); map-art murals (found media). 👻🗺️🔗

## LAYER 5 — EXTERNAL / CROSS-SURFACE (the ARG "leaves the game")

> **Cohesion doctrine: one artifact, many windows.** The record website, the hint rail, the
> Discord artifact-leak, and the Iss falsified entries are all the SAME recovered-system artifact
> bleeding through different surfaces. The `hints` table is the single source; render it in-world
> AND on the website; ensure they don't double-deliver or desync. The Discord bot drops the *same
> system's echoes* into the friends' comms — not a separate game channel.

- ⬜ **The record website** (reframed Vercel app) — a **half-corrupted archive terminal of the
  Hold's own record-keeping**: degraded, half-redacted, entries out of order, integrity warnings.
  Discover-by-URL; ledger fills with names; **write answers INTO it**; redactions lift with flags;
  hint rail rendered as "error log / integrity checker" (same `hints` table as in-world whispers).
  Security: RLS / edge-function read path ONLY — never the service key in the browser. 📖🔗🧩
- ⬜ **Discord — haunted surface (D1, A9, Phase D).** The bot drops **corrupted artifact leaks** on
  in-game triggers (enters a cursed chunk → a degraded OGG drops; status changes to what they're
  looking at). No dialogue, no persona — a grounded corrupted echo (clip of their own VC, screenshot
  of what they're looking at). Reads as the recovered system bleeding into comms. Requires Observer
  capture infra to exist first; sequence after that. Same hosting as the Observer Engine. 👻🌌
- ⬜ **Google Drive "recovered archive"** — found docs/images/audio (lore + stego clues). 🗺️🔗🧩
- ⬜ **Unlisted YouTube "found footage"** — generate via **HyperFrames**; the Seventh waiting; a clue
  in a frame. 📖👻🔗
- ⬜ **Google Voice number** — a voicemail the Watcher "left" (optional, high-uncanny). 👻🧑
- ⬜ **QR codes** in clue cards / the website → a hidden page. 🔗🧩

## LAYER 6 — RECORDING (Ethan-only, for the YouTube ambition)

- ⬜ Replay Mod + Iris/Complementary shaders on **Ethan's client only** (never the pushed pack);
  server-side beat logging → a "best moments" feed. (Resolve the deep-negative-Y camera clamp first.)

---

## THE SIGNATURE INTEGRATIONS (build these first to define the feel)

1. **Per-player illusion** (`showEntity` + packet light + per-player fog) — the literal "it knows ME."
2. **The asymmetric co-op vault (the Threshold)** — each sees a fragment, combine aloud. The social
   centerpiece. Backed by a **vanilla trial-chamber vault** (per-player keys by default). Composition:
   fragments = the puzzle; vault = the payoff. Companion (Wren) may tie the betrayal to this beat.
3. **The Lens** — held item reveals hidden runes; "second sight" as item + puzzle.
4. **The Observer-heard scare** — say it in VC, the world quotes you back.
5. **The black-moon events** — real lunar-gated dread (Brann).
6. **The desire-path grave** — appears on the route you walk most.
7. **The reflection puzzle** — a rune only the water shows you (Sella).
8. **The record block + website** — the in-game ledger terminal mirrored by the real webpage.

These eight + audio + the fog datapack convert the project from "beautiful Discord cards + vanilla
terrain" into a world that watches, hides, and reacts.
