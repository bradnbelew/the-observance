# Research Lane: mc-pack-fog-sound

> **RESEARCH REFERENCE — NOT STORY, SETUP, OR RUNTIME AUTHORITY.** The custom-dimension recommendations below are retired; V5 uses no custom Undercroft dimension.

**Plugin tech — resource pack push, custom fonts (rune alphabet), custom sounds (spatial), custom_model_data, datapack fog/dimensions, dynamic lighting.**
Target stack: Paper 1.21.x / Java 21. Path A = friends accept ONE auto-pushed pack. Scope: the `[PACK]` and the Undercroft fog world.

> **CRITICAL ARCHITECTURE FACT (informs everything below):** In Java Edition, **fog is NOT controlled by the resource pack.** Fog density/color/distance is driven by the **dimension type `effects`** field and **biome `effects`** in a **datapack** (server-side). The resource pack handles *textures, models, fonts, sounds, GUIs* only. So The Observance needs BOTH a pushed resource pack (cosmetics/runes/sounds) AND a server datapack (the Undercroft dimension + biomes for fog). Do not try to ship the Undercroft fog "in the pack" — it cannot work.

---

## 1. Server-pushed resource pack — modern Paper/Adventure API (NOT deprecated `setResourcePack`)

**Do not use** the old `Player#setResourcePack(String url, ...)` overloads — they are all marked `@Deprecated` in Paper 1.21 and only manage a single legacy pack slot. Use the **Adventure `ResourcePackRequest` / `ResourcePackInfo`** API (or the Bukkit `addResourcePack(UUID, url, hash, prompt, force)` equivalent). Since MC **1.20.3** the client supports a **stack** of server packs, each addressed by its own UUID, added/removed independently.

```java
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import java.net.URI;
import java.util.UUID;

UUID PACK_ID = UUID.fromString("…stable-uuid-per-pack…");

ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
    .id(PACK_ID)                                  // stable identity — same UUID = "this pack", lets you removeResourcePacks(PACK_ID) later
    .uri(URI.create("https://cdn.example/observance-v7.zip"))
    .hash("2849ace6aa689a8c610907a41c03537310949294") // 40-char lowercase SHA-1 HEX of the EXACT zip bytes
    .build();

ResourcePackRequest req = ResourcePackRequest.resourcePackRequest()
    .packs(info)
    .prompt(Component.text("The Observance is listening.", NamedTextColor.DARK_GRAY))
    .required(true)        // client may decline+disconnect; see UX pitfall
    .replace(false)        // false = STACK on top of any existing server packs; true = clear server packs first
    .callback((uuid, status, audience) -> {        // status updates, may arrive out of order
        if (status == ResourcePackStatus.SUCCESSFULLY_LOADED) { /* mark player "in-world" */ }
        else if (status == ResourcePackStatus.DECLINED || status == ResourcePackStatus.FAILED_DOWNLOAD) { /* fallback path */ }
    })
    .build();

player.sendResourcePacks(req);
// later, to swap or pull the pack for a player:
player.removeResourcePacks(PACK_ID);   // or player.clearResourcePacks();
```

**Status enum to listen for** (Adventure `ResourcePackStatus` / Bukkit `PlayerResourcePackStatusEvent.Status`): `ACCEPTED`, `DOWNLOADED`, `SUCCESSFULLY_LOADED`, `DECLINED`, `FAILED_DOWNLOAD`, `INVALID_URL`, `FAILED_RELOAD`, `DISCARDED`. `Player#hasResourcePack()` returns true only when last status was `SUCCESSFULLY_LOADED`. **`getResourcePackHash()` is dead** — "no longer sent from the client, always null" — so track state via the status callback, not the hash.

**Pitfalls:**
- **Status is client-driven and unreliable.** PaperMC docs explicitly warn responses can arrive out of order, be incomplete, or contradictory (and modded clients can lie). Design defensively; never *block* gameplay on `SUCCESSFULLY_LOADED` without a timeout.
- **`required(true)` UX:** if a player declines a required pack they are **disconnected**. If they decline once they are *not re-prompted* — they must manually enable "Server Resource Packs" for your server in MC settings. For a friend group this is fine (you tell them once), but a hard-fail on a flaky download is a bad first impression. Consider `required(false)` + a re-send on join, or a "you must enable the pack" lobby gate.
- **Vanilla single-pack server.properties** (`resource-pack=`, `resource-pack-sha1=`, `require-resource-pack=true`, `resource-pack-prompt=`) is the *non-plugin* path; it only manages one pack and can't do per-player. Use the plugin API so you can push/remove per player and stack future "act" packs.

> **Apply to The Observance:** Push ONE required pack on join via `sendResourcePacks` with a stable `PACK_ID`; use the callback to flip the player's "initiated" flag in Supabase; keep the UUID stable so later ARG "acts" can `addResourcePack`/`removeResourcePacks` to layer in new runes/sounds without re-sending the base.

**Sources:** [Paper Player Javadoc 1.21.11](https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Player.html) · [PaperMC Adventure resource-pack docs](https://docs.papermc.io/adventure/resource-pack/) · [PlayerResourcePackStatusEvent](https://jd.papermc.io/paper/1.21.11/org/bukkit/event/player/PlayerResourcePackStatusEvent.html)

---

## 2. Hosting + SHA-1 — the cache trap

The client caches packs **by SHA-1**. If a player already has a zip with that hash, the client **uses the cache and never re-downloads**, even if the URL bytes changed. Conversely, if the hash you send ≠ the hash of the served bytes, the client may reject/redownload-loop.

**Rules:**
- SHA-1 must be of the **exact uploaded bytes**. Compute *after* upload, or hash the local file you're about to upload byte-for-byte: `sha1sum observance.zip` / `openssl dgst -sha1 observance.zip` / PowerShell `Get-FileHash -Algorithm SHA1`. Send it as a **40-char lowercase hex** string.
- **Every content change must change the hash** AND you should change the URL (e.g. `observance-v7.zip`). Re-zipping alone *almost always* changes the hash anyway (timestamps, file order, compression level all alter bytes) — which is good, but means you must re-hash every build.
- Hash-mismatch culprits: host re-encoded the upload, CRLF/LF line-ending changes on text assets, or the host hashing a stale CDN copy. **Local file hash is the source of truth.**
- Serve over **HTTPS**, raw `.zip`, ideally `Content-Type: application/zip`, no auth wall, stable URL. Cheap reliable hosts: a Vercel/Cloudflare static asset, an S3/R2 bucket, or `mc-packs.net`. (The Observance already has Vercel — drop the zip in `/public` and you get a CDN URL for free.)

> **Apply to The Observance:** Add a build step that zips `pack/`, computes SHA-1, and writes `{url, sha1, version}` to a config the plugin reads at boot — so a pack edit auto-busts the client cache with zero manual hash juggling. Version the filename (`observance-vN.zip`).

**Sources:** [SHA-1 caching/redownload explanation](https://www.planetminecraft.com/blog/how-to-set-resource-packs-for-your-server/) · [Wabbanode server pack guide](https://wabbanode.com/help/minecraft/how-to-add-a-server-side-resource-pack-to-your-minecraft-server)

---

## 3. Custom fonts — the rune alphabet (signs, books, titles, chat)

Fonts live at `assets/<namespace>/font/<name>.json` and are referenced in any text component via the **`"font"` field** (`{"text":"…","font":"observance:runes"}`). Three provider types:

### (a) `bitmap` provider — your hand-drawn rune glyphs
```json
{ "providers": [
  { "type": "bitmap",
    "file": "observance:font/runes.png",   // PNG; glyph width auto-sized by rightmost non-transparent pixel column
    "ascent": 7,                            // baseline shift up; must be <= height
    "height": 8,                            // render scale (8 = vanilla size; 16 = double-res HD glyph)
    "chars": [ "",        // grid rows; texture is split evenly by row count x longest-row length
               "" ] }
] }
```
- **Use Private Use Area codepoints `U+E000–U+F8FF`** for runes so they never collide with real text. Map A→``, B→``, … in your plugin when you want to render "encoded" English.
- HD runes: set `height` to a multiple of 8 and make the PNG that many times taller per glyph.

### (b) `space` provider — negative/positive kerning (for sign/GUI alignment tricks)
```json
{ "type": "space", "advances": { "": -8, "": 8, " ": 4 } }
```
Lets you nudge glyphs left/right by exact pixel counts (negative-space-font technique) — useful for layering rune overlays or building custom HUD text out of glyphs. Default space `U+0020`=4px, ZWNJ `U+200C`=0.

### (c) `unihex` provider — only if you need full Unicode coverage (a `.zip` of GNU Unifont `.hex` files). Overkill for a rune cipher; skip.

### FREE WIN: the built-in Standard Galactic Alphabet
Minecraft already ships **`minecraft:illageralt`** (internal `alt`) — the enchanting-table rune font, a clean 1:1 substitution cipher A–Z. You can render ANY text in runes with **zero pack work**:
```java
player.sendMessage(Component.text("THE WATCHER KNOWS").font(Key.key("minecraft:illageralt")));
// or in a sign/book/title component: {"text":"...","font":"minecraft:illageralt"}
```
This is the single highest-leverage cosmetic in the whole lane: instant "alien runes" on signs, books, titles, boss bars, item lore — and players who *want* to decode it can (it's a real cipher), which is perfect ARG bait. A *custom* bitmap font only buys you a **bespoke glyph look** (your own runic aesthetic vs the known Commander-Keen glyphs) — worth doing for the signature mystery alphabet, but `illageralt` works on day one.

**Authoring tools:** [HD-Font-Generator (mattmess1221)](https://github.com/killjoy1221/HD-Font-Generator) and [minecraft-font-resourcepack-generator (TTF→pack)](https://github.com/kafuuchino-desu/minecraft-font-resourcepack-generator) auto-build bitmap fonts from a TTF — use one to turn a runic TTF into the `runes.png` + `font.json`.

**Pitfalls:** glyph width is set by the *rightmost opaque pixel*, so trailing transparent columns are trimmed — pad deliberately for fixed-width runes via the `space` provider. `ascent > height` crashes the font load. Custom fonts apply to **signs, books, titles/subtitles, action bar, boss bars, item names/lore, chat** — but the player's *input* is still normal letters; the cipher is display-only (so a sign typed by a player won't auto-encode — your plugin must write the rune component).

> **Apply to The Observance:** Day-one, use `minecraft:illageralt` for Watcher signs/titles/lore (decodable cipher = built-in ARG puzzle). In parallel, author a *bespoke* PUA rune font (`observance:runes`, `U+E000+`) for the signature alphabet on the cursed-map site / deep-lore clues, generated from a runic TTF, so the look is unique and the codepoints never collide with chat.

**Sources:** [Minecraft Wiki: Font](https://minecraft.wiki/w/Font) · [Standard Galactic Alphabet](https://minecraft.wiki/w/Standard_Galactic_Alphabet) · [NegativeSpaceFont (space provider)](https://github.com/AmberWat/NegativeSpaceFont)

---

## 4. Custom sounds — spatial horror audio (MONO is mandatory)

Sounds are declared in `assets/<namespace>/sounds.json` and the `.ogg` files live under `assets/<namespace>/sounds/…`.

```json
// assets/observance/sounds.json
{
  "ambient.observance.breath": {
    "subtitle": "subtitles.observance.breath",   // closed-caption translation key (also shows the "><" directional caption!)
    "sounds": [
      { "name": "observance:ambient/breath_1", "volume": 0.6, "pitch": 0.95, "weight": 3,
        "stream": true,  "attenuation_distance": 24 },
      { "name": "observance:ambient/breath_2", "volume": 0.5, "pitch": 1.05, "weight": 1,
        "stream": true,  "attenuation_distance": 16 }
    ]
  },
  "event.observance.sting": {
    "sounds": [ { "name": "observance:sting/heartbeat", "stream": false } ]
  }
}
```
Play from the plugin (positional, so it attenuates with distance):
```java
player.playSound(loc, "observance:ambient.observance.breath",
                 SoundCategory.AMBIENT, 1.0f, 1.0f, player.getUniqueId().getMostSignificantBits());
// the trailing long 'seed' picks which weighted variant plays — pass a per-player seed for per-player variation
```

**THE killer gotcha — MONO vs STEREO:**
- A **mono** (single-channel) OGG Vorbis **attenuates with distance and pans 3D** — i.e. it has a *position*, gets quieter as you walk away, comes from a direction. **This is the entire point for soft-pressure horror** ("a breath from somewhere behind you").
- A **stereo** OGG plays at **constant full volume with no position** — fine for music/global stings, *useless* for "it's near you." If your spatial sound won't fade, it's stereo — re-export as mono.
- Must be **OGG Vorbis**, not OGG Opus (Minecraft can't decode Opus). `ffmpeg -i in.wav -ac 1 -c:a libvorbis out.ogg` (the `-ac 1` forces mono).

**Other fields:** `stream:true` for anything over a few seconds (streams from disk; avoids preload lag) — **but only 4 streamed sounds can play at once**, so reserve streaming for long ambient beds and keep short stings non-streamed. `attenuation_distance` (default 16) widens/narrows the falloff radius — bump it for a creeping far-off drone, lower it for an intimate whisper. `weight` makes random variants non-uniform. `SoundCategory` matters: use `AMBIENT`/`MASTER` so players can't trivially mute the Watcher by turning music to 0 (avoid `MUSIC`/`RECORD`).

> **Apply to The Observance:** Author all Watcher proximity cues (breath, footstep-behind, whisper, name-call) as **mono** OGG Vorbis so they attenuate and direction-pan; play them positionally near (not on) the player via `playSound(loc,…)` with a per-player seed for variation. Long Undercroft ambient drone = one `stream:true` AMBIENT bed; short stings = non-streamed. Add `subtitle` keys so deaf-moment captions read "[ something breathes ]" with the vanilla directional `><` indicator — free atmosphere.

**Sources:** [Minecraft Wiki: sounds.json](https://minecraft.wiki/w/Sounds.json) · [Fabric docs: custom sounds (mono requirement)](https://docs.fabricmc.net/develop/sounds/custom)

---

## 5. `custom_model_data` — bespoke items (relics, the Watcher's tokens) — 1.21.4 format CHANGED

Minecraft **1.21.2/1.21.4 overhauled item models.** The old `assets/<ns>/models/item/foo.json` + `overrides` + `CustomModelData:int` predicate is **gone**. New system:
1. An **item-model definition** at `assets/<ns>/items/<id>.json` controls how an item renders (tree of `model` / `range_dispatch` / `condition` / `select` nodes).
2. The item carries the **`minecraft:item_model`** component (points at that `items/<id>` file) and the **`minecraft:custom_model_data`** component (now a struct with `floats`, `flags`, `strings`, `colors` lists, not a bare int).

```json
// assets/observance/items/relic.json — render different model per custom_model_data float
{ "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:custom_model_data",   // reads custom_model_data.floats[0]
    "entries": [
      { "threshold": 1, "model": { "type": "minecraft:model", "model": "observance:item/sigil_cold" } },
      { "threshold": 2, "model": { "type": "minecraft:model", "model": "observance:item/sigil_warm" } }
    ],
    "fallback": { "type": "minecraft:model", "model": "observance:item/sigil_dormant" }
} }
```
Give a paper/totem the component server-side and the client swaps the model. **Pitfall:** packs and plugins written for ≤1.21.1 use the old override format and **silently render as the base item** on 1.21.4 — pick ONE target version and author for it. Converters exist ([ItemModel_PackConverter](https://hangar.papermc.io/RICE0707/ItemModel_PackConverter)) but verify on your exact Paper build.

> **Apply to The Observance:** Author Watcher relics (sigil totems, "letters from the deep," map fragments) as base items (e.g. paper/totem) + `item_model`/`custom_model_data` so they look unique; use `range_dispatch` thresholds so one relic item visibly *changes state* (dormant→warm→cold) as the ARG advances — the model is driven by the component your plugin sets, keeping mechanic and story in lockstep.

**Sources:** [Minecraft Wiki: Items model definition](https://minecraft.wiki/w/Items_model_definition) · [Data component: custom_model_data](https://minecraft.wiki/w/Data_component_format/custom_model_data) · [NeoForged 1.21.4 primer](https://docs.neoforged.net/primer/docs/1.21.4/)

---

## 6. The Undercroft fog world — datapack dimension + biome (NOT the pack)

Fog is a **dimension `effects` + biome `effects`** thing. Two levers:

### (a) Dimension type `effects` — the global fog *mode*
`data/<ns>/dimension_type/undercroft.json`:
```json
{
  "ultrawarm": false,
  "natural": false,
  "coordinate_scale": 1.0,
  "has_skylight": false,        // no sun = permanent darkness
  "has_ceiling": true,          // bedrock-roof spawning + disables weather/sky (claustrophobic)
  "ambient_light": 0.0,         // 0 = pure pitch black except placed light; (Nether default is 0.1)
  "fixed_time": 18000,          // optional: lock to midnight-dark
  "piglin_safe": false, "bed_works": false, "respawn_anchor_works": false,
  "has_raids": false,
  "logical_height": 256, "min_y": -64, "height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "effects": "minecraft:the_nether",   // <-- THE FOG: gives dense, sight-blocking fog (begins ~10 blocks, opaque ~96)
  "monster_spawn_light_level": 0,
  "monster_spawn_block_light_limit": 0
}
```
- **`effects` choices & their fog:** `minecraft:overworld` = normal distant render-fog; `minecraft:the_nether` = **thick near fog that blocks sight** (this is what you want for the Undercroft); `minecraft:the_end` = dark spotted void sky, ignores sky/fog color. So **`"effects": "minecraft:the_nether"` is the one-field switch for oppressive fog.**
- `has_ceiling:true` + `has_skylight:false` + `ambient_light:0.0` = a sealed pitch-dark space where the only light is what *you* place — the player's torch becomes the whole world, and fog eats everything past ~a dozen blocks.

### (b) Biome `effects` — the fog *color* + ambience
The dimension sets fog *mode*; the **biome** sets `fog_color`, `sky_color`, plus the ambient sound bed. `data/<ns>/worldgen/biome/undercroft_dark.json`:
```json
{ "temperature": 0.4, "downfall": 0.0, "has_precipitation": false,
  "effects": {
    "fog_color": 1908001,        // 0x1D1721 — cold near-black violet
    "sky_color": 1908001,
    "water_color": 2105376, "water_fog_color": 1052688,
    "mood_sound": { "sound": "observance:ambient.observance.breath",
                    "tick_delay": 4800, "block_search_extent": 8, "offset": 2.0 },
    "ambient_sound": "observance:ambient.observance.drone"   // continuous bed
  },
  "spawners": {}, "spawn_costs": {}, "carvers": [], "features": [] }
```
- The **`mood_sound`** is a vanilla horror mechanic for free: in low light it builds an internal "mood" counter and, when it tops out, plays your sound *from a random nearby dark block* (the classic cave-noise jumpscare). Point it at a Watcher whisper and the engine self-generates dread without a single line of plugin code.
- `ambient_sound` = a looped bed tied to the biome (like the Nether's wind moan). Point at a low drone.

### Loading it (you're on Paper + Multiverse-Core)
- Custom dimension JSON loads from a **datapack in the main world's `datapacks/` folder** (vanilla; there is no per-world datapack folder). Multiverse 5 **auto-imports** datapack-defined dimensions on startup; you can also place dimension files under `data/<ns>/multiverse/dimension/<path>.json` for MV's loader, then `/mv import` / teleport via MV. Use **MVDatapackLoader (MVDL)** if you want per-world packs namespaced by world.
- **Known fog bug (MC-211878):** a dimension using `effects: the_nether` with **12+ biomes** loses the fog. Keep the Undercroft to a **small handful of biomes** (1–3) to be safe.

> **Apply to The Observance:** Build the Undercroft as a datapack dimension with `effects:"minecraft:the_nether"`, `has_ceiling:true`, `has_skylight:false`, `ambient_light:0.0` for sealed claustrophobic fog-dark; pair it with 1–3 custom biomes whose `fog_color` is a cold near-black and whose `mood_sound`/`ambient_sound` point at the mono Watcher beds from §4 — so the fog world *self-generates* proximity dread. Load via Multiverse (≤3 biomes to dodge MC-211878). Remember: this is the **datapack**, shipped/enabled server-side — it is NOT in the pushed resource pack.

**Sources:** [Minecraft Wiki: Fog](https://minecraft.wiki/w/Fog) · [Dimension type](https://minecraft.wiki/w/Dimension_type) · [Tutorial: Adding a new dimension](https://minecraft.wiki/w/Tutorial:Adding_a_new_dimension) · [Biome definition](https://minecraft.wiki/w/Biome_definition) · [MC-211878 (12-biome fog bug)](https://bugs.mojang.com/browse/MC-211878) · [Multiverse FAQ: datapack dimensions](https://mvplugins.org/core/reference/faq/) · [MVDatapackLoader](https://modrinth.com/plugin/mvdl)

---

## 7. Dynamic / fake lighting — making the dark *move* (server-side, no client mod)

Path A = no client mods, so use the **light-block packet** trick (the technique behind server-side DynamicLights plugins): send the player a **phantom `minecraft:light[level=N]` block-update packet** at a position, and the client renders light there with no real block placed. PacketEvents (already in the stack) sends the block-state change.

```java
// pseudo: phantom light that follows the player's held "lantern" or trails behind them
WrapperPlayServerBlockChange pkt = new WrapperPlayServerBlockChange(
    pos, lightBlockStateId(level));        // minecraft:light with level=N
packetEventsApi.getPlayerManager().sendPacket(player, pkt);
// to clear: send the real block-state at pos again (usually air/cave_air)
```

**Uses for horror:**
- A light that **recedes as you approach** (the Watcher's lantern always just out of reach).
- A torch you place that **gutters/dies** on a Watcher beat (drop phantom light level over a few ticks).
- A faint glow that **appears behind the player** for a second, then vanishes (peripheral dread; tie to the §4 mono breath).
- Glowing-entity outline (`Entity#setGlowing(true)` / team color packet) for a brief "something is there" silhouette through walls — pairs with the fog so the *outline* reads before the model does.

**Pitfalls:** phantom light blocks are per-position on the block grid (not smooth) — move them in small steps and cull beyond ~32 blocks/player. Real `minecraft:light` blocks are immovable by pistons and block explosions — but you're sending *fake* ones via packet, so no world state is touched (clean). Sending too many per tick to many players = bandwidth; throttle to the nearest player(s).

> **Apply to The Observance:** Use PacketEvents phantom `minecraft:light` packets for a Watcher "lantern" that recedes, for torches that gutter on a beat, and for a one-second behind-you glow synced to the mono breath cue — all server-side, no client install. Combine with `setGlowing` for a brief outline-through-fog silhouette. Throttle to nearest player, cull at ~32 blocks.

**Sources:** [Tschipcraft DynamicLights (light-block technique)](https://github.com/Tschipcraft/dynamiclights) · [xCykrix DynamicLights (no-mod server-side)](https://github.com/xCykrix/DynamicLights) · [SpigotMC: basic vanilla server-side dynamic lighting](https://www.spigotmc.org/resources/basic-vanilla-server-side-dynamic-lighting.112024/)

---

## 8. `pack_format` & version discipline (the silent killer)

Each MC version expects a specific `pack.mcmeta` `pack_format` number, and the **item-model format itself changed at 1.21.4** (§5) and the **dimension_type fog stays datapack-side** regardless. A pack built for the wrong format loads partially or not at all (textures fine, models/fonts broken — hard to debug because it *looks* like it loaded).

- `pack.mcmeta` minimal:
```json
{ "pack": { "pack_format": 46, "description": "The Observance" },
  "supported_formats": { "min_inclusive": 42, "max_inclusive": 99 } }
```
  Use `supported_formats` (a range) so one zip survives minor version bumps without the "made for a different version" warning.
- **Resource pack `pack_format` ≠ datapack `pack_format`** — they're separate numbers in separate `pack.mcmeta` files (the pushed pack vs the Undercroft datapack). Don't cross them.
- Pin the server to ONE Paper 1.21.x and author the pack + datapack for exactly that. Bumping Minecraft mid-ARG can break the item models and fonts.

> **Apply to The Observance:** Lock to one Paper 1.21.x; set the resource-pack `pack.mcmeta` to that format with a `supported_formats` range; keep the Undercroft datapack's `pack.mcmeta` as a *separate* datapack format number. Treat a Minecraft version bump as a release event (re-test fonts + item models), not a silent update.

**Sources:** [Minecraft Wiki: Pack format](https://minecraft.wiki/w/Pack_format) · [Items model definition (1.21.4 change)](https://minecraft.wiki/w/Items_model_definition)

---

## Quick reference: which layer owns what

| Effect | Lives in | Mechanism |
|---|---|---|
| Rune glyphs on signs/books/titles | **Resource pack** | `assets/ns/font/*.json` + `"font"` component (or built-in `minecraft:illageralt`) |
| Spatial Watcher sounds | **Resource pack** | `sounds.json` + **mono** OGG; played via `playSound(loc,…)` |
| Bespoke relic items | **Resource pack** | `assets/ns/items/*.json` + `item_model`/`custom_model_data` components (1.21.4 format) |
| **Undercroft fog (density/color)** | **Datapack** | `dimension_type effects:"the_nether"` + biome `fog_color` |
| Mood/ambient horror loops | **Datapack** | biome `mood_sound` / `ambient_sound` → custom sound event |
| Moving/fake light | **Plugin** | PacketEvents phantom `minecraft:light` block packets + `setGlowing` |
| Push the pack / per-player layers | **Plugin** | Adventure `sendResourcePacks` / `removeResourcePacks(UUID)` |
