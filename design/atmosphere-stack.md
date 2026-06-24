# The Observance — Atmosphere & Custom-Content Stack (server-side, 2026)

Companion to `DESIGN.md`, `FLOW.md`, and `design/arg-deepening.md`. **Spoiler-free** — safe for Ethan
and the dashboard. This is the definitive, current (June 2026) stack that makes The Observance *look and
feel blow-up-worthy* — the From-The-Fog-grade dread, the rune-carved world, the tall silent watcher —
**without breaking Path A** (friends install nothing but one auto-pushed resource pack: a single in-game
"accept?" click).

Everything here obeys the anti-jank contract (`DESIGN §3`): deterministic spine, LLM is a text-only
scalpel, reveals stay out of line of sight, structures are curated footprint-checked schematics, tolls
are reversible. **No tool here generates geometry or coordinates at runtime** — they *render* and *paste*
pre-authored content.

> **The single most important constraint this document resolves:** the plugin is **Java 21 / Paper 1.21.x**
> (`README`, `DESIGN §5`). Several 2026 model engines have moved to **Java 25**. Tool choices below are
> filtered for Java-21 compatibility first, look second. Where a tool needs Java 25, it is flagged and a
> Java-21 alternative is named.

---

## 0. The one axis that matters (and the headline call)

Per `arg-deepening §4`: on Java there is **no way to show a custom texture / model / sound without a
resource pack on the client** — but since MC 1.20.3, Paper's `Player#setResourcePack(...)` auto-pushes the
pack on join (one prompt, not a modpack). So the axis is *how much custom content* vs *pure vanilla
tricks*, never "install or not."

**Headline recommendation (unchanged from `arg-deepening`, now version-grounded):**

| Layer | Decision | Why |
|---|---|---|
| **Structures** | **FastAsyncWorldEdit 2.15.x** (GPL-3.0, Java 21, free) | async off-main-thread paste; wires straight into `SmallStructureBeat` |
| **The rune font + sounds + item art** | **one auto-pushed `[PACK]`** (vanilla resource-pack levers) | the cipher *needs* the font; pack also carries the ambient bed + ritual-item models |
| **NPCs (Keeper + 6 apparitions)** | **Citizens2** (GPL, Java 21) + **ZNPCsPlus** packet NPCs | server-side, per-player, no install; wires into a new `KeeperNpcBeat` |
| **Custom 3D models (watcher/follower/mimic)** | **OPTIONAL, Phase 2.5+.** Default **vanilla mob + name/glow** via `NamedMobBeat`. If a true custom rig is wanted: **ModelEngine R4 (paid, Java-21-safe) + MythicMobs (free)**, *not* BetterModel 3.x (Java 25) | the differentiator is *behavior + reveal discipline*, not polygon count; a vanilla husk only ever **discovered** already reads supernatural |
| **Fog / no-sky dread world** | **datapack `dimension_type` effects + biome `fog_color`** (zero install) | the Undercroft (`arg-deepening §3 Movement III`) gets real environmental fog with no client mod |
| **Voice (late arc)** | **Simple Voice Chat** (one `[MODPACK]` install, Movements III–V only) | the single justified client install; spatial keeper whispers |
| **Recording (Ethan solo)** | Complementary/BSL shaders + Replay Mod / camera mods | **capture-time only, never pushed to players** |

**Net friction to the friend group: one "accept resource pack?" click.** (Plus, optionally, one Simple
Voice Chat install in the late arc.)

---

## 1. Custom 3D entities with no client mod

### 1.1 The honest baseline — you may not need a model engine at all

The repo's `NamedMobBeat`
(`plugin/.../beats/lib/NamedMobBeat.java`) already spawns a **named, silent, persistent, invulnerable,
non-drifting** mob that **stares at one player**, spawned out of line of sight on validated ground, tagged
in PDC for cleanup. With `setGlowing` + a custom name in the rune font `[PACK]`, a vanilla **husk / stray /
warden-silhouette** that the group only ever *discovers standing there* is already the canonical "tall
silent watcher." Reveal discipline (`DESIGN §2.2`) — *discovered, never witnessed appearing* — is what
sells it, not the polygon count. **Ship this first. A custom rig is a Phase-2.5 garnish, not a dependency.**

### 1.2 If you do want a true custom rig — the 2026 landscape, Java-21-filtered

The decisive fact for **us specifically**: The Observance targets **Java 21 / Paper 1.21.x**. This eliminates
the otherwise-best free option.

| Engine | License / Cost | Java | Paper 1.21.x | Needs MythicMobs? | Pushes models via pack? | Verdict for us |
|---|---|---|---|---|---|---|
| **ModelEngine R4** (`R4.x`, updated Dec 2025) | **Proprietary, paid** (free demo w/ a few models) | **Java 21 ✓** | ✓ (1.19.4–1.21.11) | No (standalone), pairs with MythicMobs | ✓ auto-generates pack | **Recommended IF a rig is wanted.** Industry standard, Java-21-safe. The only "real" option that runs on our JVM. |
| **BetterModel** (`3.2.0`, Jun 19 2026) | **MIT, free**, auto-pack | **Java 25 ✗** (since 3.0.0) | 1.21.4+ only | No (standalone) | ✓ item-display packet | **Blocked by Java 25.** *Would* be the top pick on look+license. Last Java-21 build is **2.2.0** (Mar 2026) — viable only if you pin an older line and accept no further updates. |
| **MythicMobs [Free]** | Free (premium = newer builds) | Java 21 ✓ | ✓ (1.21.x) | — | — (mob *behavior* engine; pairs with a model engine for the look) | **Use the mob-scripting layer** if you adopt ModelEngine, for clean per-mob config. Not required for the look on its own. |
| **Mythic Crucible** | Paid add-on to MythicMobs | Java 21 ✓ | ✓ | Yes | — | Item/ability authoring, not a model engine. Skip for atmosphere. |
| **FreeMinecraftModels** (MagmaGuy) | **GPLv3, free** | Java 21 ✓ (pre-Java-25 lines) | ✓ | No | ✓ pack | Lighter free alternative to ModelEngine; fewer features, BlockBench models in-game. **Viable free fallback** if you refuse a paid plugin and want a real rig on Java 21. |
| **ItemsAdder** | Paid | Java 21 ✓ | ✓ (1.21.x) | No | ✓ pack | Heavy "content framework"; overkill — we already have a beat engine. Skip. |
| **Oraxen / Nexo** (Oraxen → Nexo migration ongoing 2026) | Oraxen free; Nexo paid | Java 21 ✓ | ✓ (Paper for 1.21.11+) | No | ✓ pack | Item/furniture/block focus; entity models are secondary. Useful only if you want custom *furniture* altars (see §3.4). Skip for the watcher. |

**Call:** Default = **no engine** (vanilla `NamedMobBeat`). If a marquee rig is wanted for one or two
hero beats: **ModelEngine R4 + MythicMobs (free)** — both Java-21-safe — *or* **FreeMinecraftModels** if you
want to stay free/open. **Do not adopt BetterModel 3.x** unless/until the server moves to Java 25.

### 1.3 The four creatures, mapped to beats

- **Tall silent watcher** → `NamedMobBeat` (vanilla husk/stray, silent, no-drift, glow optional). With a
  rig: a MythicMob whose ModelEngine model is a faceless tall figure. **Same beat class either way** — only
  the spawned entity/mob-id changes.
- **A follower** → `NamedMobBeat` with `setTarget(player)` (already implemented) and a low movement speed
  attribute. The "it walks behind you" beat is `noDrift=true` + a slow pathfind goal.
- **A mimic** (an entity disguised as a block/animal until approached) → `SacredAnimalBeat`-style retag of
  an existing mob, OR a `FakeBlockBeat` (per-player fake block via PacketEvents) that, when broken/
  approached, swaps to a `NamedMobBeat` spawn. The mimic is a **two-beat composition** through `UnlockBeat`'s
  dispatcher (`step: named_mob`).
- **A "thing made of a past group"** (a watcher built from a prior keeper's silhouette) → this is the one
  that *earns* a custom rig: a ModelEngine model. Until then, a glowing named mob at a keeper-stone, spawned
  by the same `NamedMobBeat`, carrying that keeper's name in runes.

> **New beat to add: `ModeledMobBeat` (Phase 2.5, optional).** A near-clone of `NamedMobBeat` that, after
> spawn, calls the model engine API to attach a rig — `ModelEngine`: `ModeledEntity` + `ActiveModel`
> applied to the Bukkit entity; or `FreeMinecraftModels` equivalent. **Deterministic fallback baked in:**
> if the model plugin is absent or the API throws, it degrades to exactly `NamedMobBeat` behavior (named
> vanilla mob). This keeps the anti-jank "graceful degradation" rule (#5): the haunting continues even if
> the rig fails. It is a *cosmetic* layer over an already-working beat.

---

## 2. Resource-pack levers (one accept-click) — what is and isn't possible

Push mechanism: **`Player#setResourcePack(UUID, url, byte[] hash, Component prompt, boolean force)`** (Paper
1.21.x; the `force` flag + `ResourcePackStatusEvent` gate the join). Host the `.zip` anywhere static (the
Vercel dashboard, an S3/R2 bucket, or `server.properties` `resource-pack=`). **The plugin ships no pack** —
it only *references* keys the pack must provide (`TODO-GOLIVE §5`), and **falls back gracefully** (vanilla
sound / default font) until the pack is live. There is precedent: `discord/assets/fonts/` already holds the
brand fonts; the rune font is the in-game twin of `discord/src/forge/runes.ts`.

| Pack lever | Possible server-side? | How | Wires into |
|---|---|---|---|
| **Rune font** (the whole substitution cipher) | **Yes** — required | `assets/minecraft/font/*.json` glyph provider (`bitmap` or `unihex`), mapping each rune glyph to a private-use codepoint; signs/books/titles/lecterns render it | `SignWriteBeat`, `LecternFillBeat`, `BookAppearsBeat`, `BossBarBeat`, `PrivateMessageBeat` — all already write text; the font reskins it. **Source of truth = `discord/src/forge/runes.ts`** (26-glyph bijective alphabet) so in-world and Discord never disagree. |
| **Ambient + whisper sounds** | **Yes** | `assets/minecraft/sounds.json` defines custom sound events (e.g. `observance:whisper`, `observance:drone`); `.ogg` files under `assets/minecraft/sounds/`. **Author MONO `.ogg` or it won't attenuate spatially.** | `PrivateSoundBeat` (`namedSound`/`named_sound` already implemented in `PerPlayer.namedSound`) — *zero new code*, the key just needs to exist in the pack |
| **Custom item models** (`custom_model_data`) | **Yes** | item override models for the named ritual components (*the deep's first heart*, etc.) | `ItemRelabelBeat`, `ChestArrangeBeat`, the Accepting components (`arg-deepening §1.6`) — validated by name/lore/PDC, *looked* by `custom_model_data` |
| **Block / entity reskins** | **Yes (cosmetic)** | re-texture a block state or a vanilla mob's texture in `assets/minecraft/textures/` | makes a vanilla husk read as "the watcher"; reskin a block used by `FakeBlockBeat`/`SmallStructureBeat` so a curated structure looks carved/ancient |
| **Negative-space font** (HUD/positioning) | **Yes** | AmberWat's NegativeSpaceFont glyphs for precise overlay positioning of rune art | optional polish for `BossBarBeat`/title beats |
| **Creepy texture swaps** (subtle wrong-textures) | **Yes** | swap a handful of textures to "off" variants (a face in the bark, a wrong eye) | pure atmosphere; no beat needed — it's ambient to the whole pack |
| **Fog / sky tweaks** | **NO, not via resource pack on Java.** | Java-edition fog is **not** a resource-pack feature (that's Bedrock). It is controlled by **datapack `dimension_type` `effects` + biome `fog_color`/`sky_color`**. | the Undercroft fog world — see §3.5. This is a **datapack**, not the pushed resource pack, and needs **no client install**. |

**Bottom line on the pack:** **one `.zip`** carries fonts + sounds + item models + reskins. It is the single
highest-leverage non-Tier-0 step, and the cipher requires the font regardless. **Build it once, host it, set
`force=true` (or prompt), done.** New tiny class to add: **`ResourcePackPusher`** — a `PlayerJoinEvent`
listener calling `setResourcePack(...)` with the hosted URL + SHA-1; logs `ResourcePackStatusEvent` to the
dashboard health panel (so "did everyone accept the pack?" is visible spoiler-free).

---

## 3. Structures — FastAsyncWorldEdit, wired into `SmallStructureBeat`

### 3.1 Tool & versions

- **FastAsyncWorldEdit (FAWE)** — **2.15.2** (Jun 4 2026), **GPL-3.0**, **free**, **requires Java 21**
  (✓ our JVM), supports 1.21.4–26.x on Paper. Sources: Modrinth / CurseForge / SpigotMC. This is the async,
  off-main-thread schematic engine named in `DESIGN §4` and `arg-deepening §1.2`.

### 3.2 The current state vs the upgrade

`SmallStructureBeat` (`plugin/.../beats/lib/SmallStructureBeat.java`) **today** pastes a structure from an
**inline block list** (relative offsets → materials, ≤256 cells), with full footprint validation, floor
check, reveal discipline, and protected-block registration. This is the Phase-0 / no-FAWE path and it
already obeys anti-jank #2/#4/#9. **Keep it as the small-cairn path.**

**The FAWE upgrade** is for *larger* curated set-pieces the inline list can't hold (the six keeper-stone
shrines, the Undercroft rooms, the altar room, the Movement-III A→B "room rebuilds itself" swap). Add a
**`schematic` payload branch**:

```jsonc
// SmallStructureBeat payload (new branch)
{ "schematic": "stone_03_atbash_mirror", "offset": {"dy": 0}, "require_floor": true }
```

### 3.3 Exact FAWE API entry points (Java, off-main-thread paste)

```java
// 1) Load the .schem once (cache it). FAWE classes:
ClipboardFormat fmt = ClipboardFormats.findByFile(schemFile);          // SPONGE_V3 for modern .schem
Clipboard clipboard;
try (ClipboardReader reader = fmt.getReader(new FileInputStream(schemFile))) {
    clipboard = reader.read();
}

// 2) Paste ASYNC, out of sight (called from the beat AFTER reveal check passes).
//    FAWE does the heavy lifting off the main thread itself.
com.sk89q.worldedit.world.World weWorld =
        com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(bukkitWorld);
try (EditSession edit = com.sk89q.worldedit.WorldEdit.getInstance()
        .newEditSessionBuilder().world(weWorld).build()) {
    Operation op = new ClipboardHolder(clipboard)
            .createPaste(edit)
            .to(com.sk89q.worldedit.math.BlockVector3.at(x, y, z))
            .ignoreAirBlocks(true)        // never carve the player's world with the schematic's air
            .copyEntities(false)          // no stray entities (anti-jank)
            .build();
    Operations.complete(op);
}
```

**Anti-jank wiring (must hold):**
- Footprint pre-check **before** paste: read the clipboard's dimensions (`clipboard.getDimensions()`) and
  run the same `Placement.isReplaceable` sweep `SmallStructureBeat` already does, so a schematic never
  carves rock or floats. Author schematics with a known solid base layer.
- **Reveal discipline:** paste only inside the existing `mutateWhenUnwitnessed(...)` wrapper, in an
  unloaded/unwitnessed chunk hundreds of blocks ahead (`arg-deepening §1.2 step 2`).
- **Protect** the pasted region: after paste, register the bounding box with `ctx.protectedRegistry()` (or a
  region tag) so the anti-grief listener guards it.
- **Relight** if needed (`edit` handles lighting on modern FAWE; verify no black chunks).
- **Idempotent** (anti-jank #9): tag the paste in Supabase so a restart never double-pastes the same stone.

### 3.4 Authoring loop (you, between sessions)

1. Build the set-piece in a scratch world (creative).
2. `//pos1` / `//pos2` → `//copy` (stand at the intended origin so the offset is baked) → `//schem save
   stone_03_atbash_mirror`.
3. Drop the `.schem` into `plugins/<plugin>/schematics/` (or a `schematics/` resource dir the showrunner
   syncs).
4. The showrunner enqueues a `small_structure` beat with `"schematic": "stone_03_atbash_mirror"`.

**Editor tools (your machine, never the players'):**
- **Axiom** (Fabric mod, free for personal) — fastest in-game building/sculpting for the curated set-pieces.
- **Amulet** (standalone, free) — world/schematic editor; convert/import builds, edit `.schem` outside MC.
- **WorldEdit/FAWE in a creative scratch world** — the `//copy → //schem save` loop above.
- **BlockBench** — only if you go the ModelEngine/FMM route (§1.2) for *entity* models, not structures.

### 3.5 The fog world (Undercroft) — datapack, no install

Per research, Java-edition fog is **not** resource-pack-driven. The Undercroft's "true environmental fog"
(`arg-deepening §3 Movement III`) comes from a **datapack** custom `dimension_type` with
`"effects": "minecraft:the_nether"` (thick sight-blocking fog) **or** a custom biome with a dark
`fog_color`/`sky_color` and `ambient_light: 0`. **Multiverse-Core** (GPL, free) manages it as a separate
world the lectern-comparator door teleports into. No client mod, no pushed pack needed for the fog itself —
the pack only adds the *sounds/textures* inside it. This is the one place you get a visually distinct,
oppressive space server-side.

---

## 4. Audio atmosphere

### 4.1 Per-player vs world sounds (already built)

`PerPlayer` (`plugin/.../util/PerPlayer.java`) already exposes:
- `sound(player, Sound, vol, pitch)` — vanilla, this-player-only.
- `soundAt(player, Location, Sound, ...)` — spatialized for one player (`SoundCategory.AMBIENT`).
- `namedSound(player, "observance:whisper", ...)` — **resource-pack sound key, one player only.**

`PrivateSoundBeat` consumes all three. **No new code is needed for custom audio** — only the pack must
define the keys. This is the deniable "did you hear that?" cornerstone (`DESIGN §2.3`).

### 4.2 Custom sound events + the ambient bed (pack work)

In the `[PACK]`:
- `sounds.json` declares: `observance:whisper`, `observance:drone_low`, `observance:stone_breath`,
  `observance:cold_toll` (the Kept-Light / Offering toll sound used by `WhisperTollBeat` and the custom
  consequences), `observance:keeper_voice` (per-keeper, optional).
- **Ambient bed design:** a low mono `drone_low` looped *sparingly* by the showrunner at hot cells
  (`DESIGN §2.2` heatmap) via `PrivateSoundBeat` with low volume + long cooldown — restraint is the mechanic
  (`DESIGN §1`, ~90% silence). The bed is **per-player and rationed by the drama budget**, never a global
  music track (which would break the deniable, "is it just me?" feeling).
- **MONO `.ogg`** is mandatory for spatialization — stereo files won't attenuate with distance.

### 4.3 Simple Voice Chat — the one late-arc `[MODPACK]` install

- **Simple Voice Chat** (henkelmax) — Paper plugin on Hangar; mod versions on Modrinth/CurseForge track
  1.21.x (e.g. `1.21.x-2.5.x`/`2.6.x`). **License: "All Rights Reserved"** — *free to download and run as a
  server operator*, but **not redistributable**; you reference/depend on it, never vendor it (same posture
  as the GPL plugins). API artifact `voicechat-api` is on `maven.maxhenkel.de`.
- **API entry point:** `MicrophonePacketEvent` (server receives a player's mic packet) + the bundled Opus
  decoder → feed a **Whisper STT** pipeline (`DESIGN §4`, Phase 3 "The Ear") that slowly enriches the
  dossier from *what people actually say*. The Keeper can also **whisper spatial audio** to one player
  (large dread gain).
- **Cost to friends:** it *does* require a one-time client install — this is the **single justified Path-B
  install**, reserved for **Movements III–V** where whispered spatial audio earns it (`arg-deepening §4`).
  Gate it behind the dashboard so it's opt-in, and the whole arc remains fully playable without it (the STT
  enrichment and keeper-whisper are *additive*, never gating).
- **Wiring:** a new `VoiceListener` (Phase 3) on `MicrophonePacketEvent` → async STT → writes neutral
  signals to the tracker. A keeper whisper is a new `SpatialVoiceBeat` that plays an authored `.ogg` (or
  TTS) into the SVC channel for one player. **Deterministic fallback:** if SVC is absent, both degrade to
  `PrivateSoundBeat` `observance:keeper_voice` (pack sound) — the haunting continues.

---

## 5. Recording-only (Ethan solo) — never pushed to players

Strictly capture-time, on **your** client, for the YouTube-ARG B-roll (the Wifies-grade look). These never
touch `setResourcePack`, never enter `plugin.yml`, never become a dependency:

- **Shaders:** **Complementary Reimagined / Complementary Unbound** or **BSL** (via Iris + Sodium on a
  Fabric client). For the dread look: low ambient light, volumetric fog, sharp shadows. Record your solo
  pass through the *same* server with shaders on; the players see vanilla.
- **Camera / cinematics:** **Replay Mod** (record the session server-side-agnostic, then fly a cinematic
  camera through it in post) and/or **Camera Studio / freecam** mods for smooth dolly shots.
- **Distant Horizons** (optional) for big establishing vistas of a keeper-stone on the horizon.

These are documented here so the line stays bright: **everything in §1–§4 is server-side / one pack;
everything in §5 is your private recording rig.** Mixing them would break Path A and the TINAG illusion.

---

## 6. The vertical-slice atmosphere pack (prove the look before scaling)

A minimal, buildable recipe to validate the whole aesthetic in one sitting — **2–3 curated structures + ONE
custom creature + an ambient sound bed + the rune font** — using only what's above.

### 6.1 Contents

1. **Structure A — The First Keeper-Stone** (FAWE `.schem`, ~40–80 blocks): a carved standing stone +
   a Rosetta sign (4–5 runes ↔ letters, `arg-deepening §1.1`). Pasted via `SmallStructureBeat`
   (`"schematic": "stone_01_caesar"`).
2. **Structure B — The Cairn / Offering altar** (inline block list, the *existing* `SmallStructureBeat`
   path — no FAWE needed): cobble + mossy stack with a `Barrel` deposit slot (`arg-deepening §1.3`).
3. **Structure C — A doused alcove** (FAWE `.schem`, small): a niche with a `WALL_TORCH` that the
   `TorchGutterBeat` can extinguish out of sight as the Kept-Light toll.
4. **One custom creature — The Watcher:** `NamedMobBeat` → a silent, no-drift, glowing **husk** spawned out
   of LoS ~12 blocks off, named in the rune font. (No model engine in the slice — prove that vanilla +
   reveal discipline already reads supernatural. If it doesn't *feel* like enough, *then* add the ModelEngine
   rig in §1.2.)
5. **Ambient sound bed:** pack `sounds.json` with `observance:drone_low` (mono) + `observance:whisper`
   (mono). Fired by `PrivateSoundBeat` at the stone, low volume, long cooldown.
6. **The rune font:** `assets/minecraft/font/` glyph provider built from `discord/src/forge/runes.ts` (the
   26-glyph bijective alphabet). Reskins the Stone's sign, the Rosetta, and the Watcher's name.

### 6.2 The single `[PACK]` zip layout

```
observance-pack/
  pack.mcmeta
  assets/minecraft/font/runes.json          # bitmap/unihex provider -> rune glyphs (from runes.ts)
  assets/minecraft/textures/font/runes.png  # the glyph atlas
  assets/minecraft/sounds.json              # observance:drone_low, observance:whisper, observance:cold_toll
  assets/minecraft/sounds/observance/*.ogg  # MONO oggs
  assets/minecraft/models/item/*.json       # (later) custom_model_data ritual components
```

### 6.3 Build order (one sitting)

1. Generate `runes.png` + `runes.json` from `discord/src/forge/runes.ts` (the alphabet already exists in
   code — render the 26 glyphs to a bitmap atlas, one private-use codepoint each).
2. Drop 2 mono `.ogg`s, write `sounds.json`.
3. Zip, host (Vercel static / R2), compute SHA-1.
4. Add `ResourcePackPusher` (`PlayerJoinEvent` → `setResourcePack(url, sha1, force)`); log status to the
   dashboard.
5. `//copy → //schem save` Structures A and C; build B as an inline block list.
6. Enqueue, in CONFIRM mode from the dashboard: `small_structure` (A), `small_structure` (B, inline),
   `named_mob` (Watcher), `private_sound` (`named_sound: observance:whisper`).
7. Walk up. If a vanilla husk standing silent under a carved rune stone with a low drone *already* makes the
   hair stand up — and it will — you have proven the look, and every scale-up (more stones via FAWE, the
   Undercroft fog world, optional ModelEngine rig, Simple Voice Chat) is now just *more of a thing that
   works*.

### 6.4 Acceptance check (the slice is "good" when)

- One pack prompt on join; everyone accepts; dashboard shows green.
- The stone's sign renders in runes; the Rosetta is readable.
- The Watcher is **discovered**, never seen spawning; it stares; it's silent; it can't be trivially killed.
- The drone is faintly there and *deniable* ("is that just me?").
- Nothing floats, nothing carved into rock, no half-built structure, no double-fire after a restart.

---

## 7. Integration map — every recommendation → exact code point

| Atmosphere element | Tool | Exact integration point in our codebase |
|---|---|---|
| Large curated structures | FAWE 2.15.x | **`SmallStructureBeat`** — add `schematic` payload branch + FAWE `ClipboardHolder.createPaste` inside the existing `mutateWhenUnwitnessed` wrapper; protect the pasted region |
| Small cairns | (vanilla, built) | **`SmallStructureBeat`** inline-block-list path (unchanged) |
| Rune font everywhere | `[PACK]` font | **`SignWriteBeat`, `LecternFillBeat`, `BookAppearsBeat`, `BossBarBeat`, `PrivateMessageBeat`** — text already written; font reskins it. Truth = `discord/src/forge/runes.ts` |
| Custom whisper/ambient sounds | `[PACK]` `sounds.json` | **`PrivateSoundBeat`** via `PerPlayer.namedSound` (already implemented) — key only needs to exist |
| Tall silent watcher / follower | vanilla `NamedMobBeat` | **`NamedMobBeat`** (built; silent/persistent/no-drift/glow/PDC-tagged) |
| Optional custom rig | ModelEngine R4 + MythicMobs (or FreeMinecraftModels) | **new `ModeledMobBeat`** — clone of `NamedMobBeat` that attaches a model after spawn; **falls back to `NamedMobBeat` if absent** |
| Mimic | PacketEvents + `NamedMobBeat` | **`FakeBlockBeat`** → on approach, `UnlockBeat`(`step: named_mob`) composition |
| Sacred / haunted-herd beast | vanilla retag | **`SacredAnimalBeat`** (built) |
| Custom item looks (ritual components) | `[PACK]` `custom_model_data` | **`ItemRelabelBeat`, `ChestArrangeBeat`** + the Accepting (`arg-deepening §1.6`) validators |
| Keeper NPC + 6 apparitions | Citizens2 + ZNPCsPlus | **new `KeeperNpcBeat`** (`NPCRightClickEvent` → dialogue tree branching on the Supabase dossier, per `arg-deepening §2`) |
| Undercroft fog | datapack `dimension_type` + Multiverse-Core | world-level (not a beat); the lectern-comparator door teleports in |
| Resource-pack push | Paper `setResourcePack` | **new `ResourcePackPusher`** (`PlayerJoinEvent`); status → dashboard health panel |
| Late-arc voice | Simple Voice Chat | **new `VoiceListener`** (`MicrophonePacketEvent` → STT → tracker) + **`SpatialVoiceBeat`**; both fall back to `PrivateSoundBeat` |
| Recording look | shaders / Replay Mod | **none** — Ethan's solo client only, never in the plugin |

---

## 8. Cost & license summary (server-operator posture)

| Tool | Cost | License | Java 21? | Posture |
|---|---|---|---|---|
| FastAsyncWorldEdit 2.15.x | Free | GPL-3.0 | ✓ | depend on as operator (fine) |
| Citizens2 | Free | GPL | ✓ | depend on (fine) |
| ZNPCsPlus | Free | GPL | ✓ | depend on (fine) |
| Multiverse-Core | Free | GPL | ✓ | depend on (fine) |
| PacketEvents | Free | open-source | ✓ | depend on (fine) |
| MythicMobs [Free] | Free (premium optional) | proprietary | ✓ | runtime dependency only |
| ModelEngine R4 | **Paid** (free demo) | proprietary | ✓ | **optional**; buy only if a rig is wanted |
| FreeMinecraftModels | Free | GPLv3 | ✓ | free rig alternative |
| BetterModel 3.x | Free | MIT | **✗ Java 25** | **blocked until JVM upgrade**; 2.2.0 is the last Java-21 build |
| Simple Voice Chat | Free | All Rights Reserved | ✓ | run as operator (fine); never redistribute |
| The resource pack | Free (your assets) | yours | — | host static; one accept-click |

**From The Fog** stays a **design reference only** — its current 2025 release is **proprietary** (correct the
stale `DESIGN §4` note that calls it CC BY-NC-SA). Reference techniques, never vendor code.

**API spend** (separate, per `COSTS.md`): unchanged by this stack — the atmosphere layer is deterministic;
the LLM scalpel remains pennies. The only *recurring* atmosphere cost is **$0** (all server-side/free) unless
you opt into **ModelEngine R4** (one-time purchase).

---

## 9. The three things this stack protects

1. **YouTube-worthy:** carved-rune world + a discovered silent watcher + a fog Undercroft + (your solo)
   shaders = the Wifies-grade look — *and it's autonomous*, which is the differentiator.
2. **Friend-group-worthy:** hard, non-linear, "it knows my name" — the pack carries the cipher font that
   makes the whole rune web legible; nothing here makes the ARG *easier*, only *deeper*.
3. **ARG-critic-worthy:** TINAG holds because there is **no modpack** — "it's just our Minecraft server,"
   one accept-click, and every custom asset has a deterministic fallback so the seams never show.

> **Single biggest gotcha to remember:** the **Java 21 vs Java 25** line. It is why **FAWE / Citizens2 /
> ModelEngine R4 / FreeMinecraftModels** are in and **BetterModel 3.x** is out. Re-evaluate the model-engine
> choice the day the server moves to Java 25 — BetterModel (MIT, free, auto-pack) becomes the top pick then.
