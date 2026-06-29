# mc-build-visualize — Minecraft Building & Visualization research

Research lane: **mc-build-visualize** (atmosphere + the YouTube layer) for *The Observance* (Deep Hold build).
Companion to `design/atmosphere-stack.md` (engine/tool *decisions*) and `design/structures.md` (per-site build spec).
This note goes **deeper on craft**: how to make carved/ruined/oppressive stone, how to make a space feel *wrong*,
how to do murals server-side, how to record the "blow-up-worthy" video — **recording-only, never pushed to players**.

North star reminder: **"From The Fog, but it knows your name."** The look that sells dread is **reveal discipline +
scale-implies-history + carved-not-default stone**, not polygon count. Every section ends with **Apply to The Observance**.

> **Hard constraint inherited from the stack doc:** Java 21 / Paper 1.21.x. Path A = friends install ONE auto-pushed
> resource pack. Anything in §6 (shaders, Replay Mod, Distant Horizons, Camera Studio) is **capture-time on Ethan's
> client ONLY** — it is a recording rig, it is *never* in the auto-pushed pack and never required of a player.

---

## 1. Carved-not-default stone: palettes, ratios, gradients

The default failure is "Minecraft-grey cobble box." Three rules from builder-consensus sources fix it: (a) **mix a
family, never one block**; (b) **obey a distribution ratio**; (c) **bridge dark→light with tuff**.

### 1.1 The 60/30/10 texturing rule (load-bearing)
For any large stone surface: **~60% primary material, ~30% transition blocks, ~10% accent/shadow blocks.** This is the
single most repeated wall-texturing heuristic. Applied to the Deep Hold keeper-stonework:
- **60% primary:** `deepslate_bricks` / `polished_deepslate` (the keepers' dressed stone).
- **30% transition:** `tuff`, `cobbled_deepslate`, `deepslate_tiles` (wear + grain).
- **10% accent/shadow:** `cracked_deepslate_bricks`, `blackstone`, `polished_blackstone`, `gilded_blackstone` flecks
  (a glint of dressed wealth in a dead place), and the occasional `chiseled_deepslate` as a "carving panel."

Pitfall: people over-scatter the 10% accent and it turns to noise — keep accents in *runs and clusters* (a cracked
seam, a soot patch), not salt-and-pepper.
Source: BlockBlend deepslate gradient guide; deltacalculator deepslate palette; Craftdex stone-variant guide.

### 1.2 The canonical dark gradient (use tuff as the bridge)
The reliable dark→light gradient is:
`stone → andesite → tuff → cobblestone/cobbled_deepslate → deepslate → polished_deepslate → blackstone`.
**Tuff is the indispensable bridge** — its grey-brown sits literally halfway between deepslate's cool grey and lighter
stone; without it the gradient bands harshly. For the Deep Hold "deeper = older = more wrong," run the build *darker as
it descends*: upper Hold leans tuff/cobbled_deepslate, the deep line (Undercroft / `unbroken_light`) is near-pure
polished_deepslate + blackstone + soul-light.
Source: minecraftgradient.blog deepslate-to-stone; BlockBlend.

### 1.3 The Ancient City palette (Mojang's own dark-temple recipe — steal it wholesale)
The vanilla Ancient City is the AAA reference for "ancient + dark + sacred + wrong." Its block set:
- `deepslate` variants (plain, cobbled, tiles, bricks, **chiseled_deepslate** as the carved face-panels),
- **`sculk`, `sculk_vein`, `sculk_sensor`** for the organic "infected/listening" texture (use sparingly — sculk reads
  as *recent wrongness*, good for the dead shrine `the_cold_hearth`),
- **`soul_lantern`** + **candles** for the eerie blue-white light (this is the signature; see §3),
- **grey + blue/light-blue/cyan wool/carpet** runners on floors (in vanilla it's vibration-damping; aesthetically it's
  a faded processional carpet — perfect for a "this was a *ritual* place" read),
- `reinforced_deepslate` frame at the center (unobtainable in survival — irrelevant; you paste schematics anyway).
Scatter `deepslate_tiles` at **~3–5%** across all bands for grain "without breaking the gradient" (BlockBlend).
The palace footprint is ~220×220, floor at **Y=-51** — i.e. Mojang itself signals "ancient ritual hall" lives in the
deep-negative Y band, which is exactly where the Deep Hold descends.
Source: Minecraft Wiki *Ancient City*; BlockBlend Ancient-City palette note.

**Apply to The Observance:** Adopt the Ancient-City palette as the Deep Hold's base recipe but **strip the sculk to near-zero**
(sculk = Mojang's monster, not ours) and **swap blue wool runners for faded/grey + dark-red** so it reads as *the keepers'*
ritual, not the vanilla city. Keep soul-lanterns and the deep-negative-Y placement. Build keeper-stones in
`chiseled_deepslate` face-panels (the carved-rune surface) framed in `polished_deepslate`.

---

## 2. Depth & broken symmetry (so it reads "built by hands," not extruded)

### 2.1 Push/pull every 3–4 blocks (the depth rule)
Flat walls are the tell. The fix: **every 3–4 blocks along a wall, recess one column back OR pull one forward by 1 block.**
Add an **outer layer** (pillars, lintels, frames) and an **inner layer** (recessed niches). Micro-details that catch
light: **upside-down stairs as corbels under ledges**, **slabs for half-height shelves**, stairs as roofline/arch trim.
Vertical pillars every **3–5 blocks** are "the fastest way to break a flat surface" and imply structural intent.
Source: Sportskeeda "4 best ways to add depth"; Minecraft.net *Raeyzeus' Top 5 Building Tips*; Switchblade building tips.

### 2.2 Break symmetry on purpose
"Perfect symmetry reads as artificial." Introduce one asymmetric element: an offset buttress, a niche on only one side,
a collapsed corner, a stair that doesn't mirror. For an *ancient* read this doubles as damage/history.
Source: Raeyzeus (official Minecraft.net); Sportskeeda.

### 2.3 Scale that implies history (cyclopean / "too big for us")
The lost-civilization read comes from **a few elements at a scale no human needs**: a doorway 6 high for a 2-tall player,
a stair tread a person must clamber, a hall ceiling lost in fog. Pair big-scale skeleton with **partial collapse**
(crumbled walls, collapsed roof sections, `mossy`/`cracked` variants, debris piles of slabs/stairs at the base) and
**broken statues / fragments** to "give a sense of history and mystery."
Source: Minecraft-schematics ancient theme; Semantic/Pinterest ancient-civ build patterns.

**Apply to The Observance:** This *is* the geography-implies-history law from `structures.md` ("deeper = older = more
wrong-scaled"). Make the **descent shaft** oversized and the keeper-stones human-stoop-scale, so the contrast says
"giants dug this, then small frightened people knelt in it." Use push/pull + asymmetric collapse on every gallery wall;
build one **deliberately wrong-scaled lintel** at `the_threshold` (Orin's crouch-to-pass) so the architecture itself
forces the bow.

---

## 3. Making a space feel *wrong* (off-grid, dim, claustrophobic, liminal)

This is the highest-leverage section for our genre. Four levers:

### 3.1 Off-grid geometry via **Display entities** (no resource pack, no client mod)
Vanilla blocks snap to the grid — and the grid is *comfortable*. Wrongness comes from things that are **slightly off-axis,
oversized, or floating where a block can't go.** Since 1.19.4, **`BlockDisplay` / `ItemDisplay` / `TextDisplay`** can apply an
**arbitrary affine transformation** (translation + rotation + scale, as `Vector3f` + quaternions). These are server-side
entities — **zero install** — with no hitbox, no sound, no gravity.

Paper API (`org.bukkit.entity.BlockDisplay`, `Display`, `org.bukkit.util.Transformation`):
```java
// A deepslate-brick pillar tilted 6° off-true and scaled 1.4× — subtly, skin-crawlingly wrong.
BlockDisplay d = world.spawn(loc, BlockDisplay.class, e -> {
    e.setBlock(Material.DEEPSLATE_BRICKS.createBlockData());
    e.setTransformation(new Transformation(
        new Vector3f(0f, 0f, 0f),                                  // translation
        new AxisAngle4f((float)Math.toRadians(6), 0, 0, 1),        // left rotation (tilt about Z)
        new Vector3f(1.0f, 1.4f, 1.0f),                            // non-uniform scale
        new AxisAngle4f()));                                       // right rotation
    e.setBrightness(new Display.Brightness(0, 0));                 // force it DARK regardless of light
    e.setViewRange(0.6f);                                          // only renders up close → "appears" as you near
    e.setBillboard(Display.Billboard.FIXED);
});
```
Key levers for *wrong*: **`setBrightness(new Display.Brightness(block, sky))`** to force a block to render unnaturally dark
(or unnaturally lit) independent of its surroundings; **`setViewRange`** so a thing only pops into existence inside a tight
radius (a reveal that obeys our line-of-sight discipline); **`setInterpolationDuration` / `setTransformation` interpolation**
to make a structure *slowly lean* or grow over real seconds; **`setTeleportDuration`** for smooth drift.
Transform order is fixed: translation → left-rotation → scale → right-rotation.
**Gotcha (resource leak):** display entities in a never-unloading chunk are never GC'd — **always PDC-tag and `remove()`**
exactly like the existing `NamedMobBeat` does. Reuse that cleanup pattern.
Source: PaperMC dev docs *Display Entities*; Minecraft Wiki *Display*.

### 3.2 Real environmental fog & no-sky dread (datapack — zero install)
A dimension/biome can carry true fog with **no client mod**: biome JSON `effects.fog_color` / `sky_color` (hex) and the
dimension `effects` field. Set dimension `effects: "minecraft:the_nether"` to inherit **thick sight-blocking fog**; or use
`ambient_light` (0 = follows torch light, up to 1 = uniform glow) to flatten depth cues. The vanilla **Darkness effect**
("pure black fog, hides the sky, sight distance ~15") is the exact "From The Fog" envelope and can be applied as a
potion-effect server-side around the Watcher.
Pitfall: full custom dimensions are heavier than needed; a **single overworld-biome override** (fog_color near-black,
low sky brightness) plus the `Darkness` effect on proximity gets 90% of the look at 10% of the risk.
Source: Minecraft Wiki *Fog*, *Effect (dimension)*, *Custom dimension*; planetminecraft Foggy datapack.

### 3.3 Claustrophobia & liminality (architectural rules)
From liminal-horror Minecraft consensus (wonderland.jar, Backrooms packs): the dread comes from **insufficient/wrong
lighting**, **rooms with no obvious exit**, **repetition that loses the player**, and **off-sound** (a door opening with
no one there). Translate to build rules:
- **Ceiling height = 2–3** in the Warrens/approach (forces the player to feel pressed); open to fog only at ritual sites.
- **Light by pools, not flood:** place soul-lanterns/candles in isolated puddles with long dark gaps between — the eye
  strains, the brain fills the dark. (See §3.4.)
- **One-block-off corridors:** a passage that's 1 wide where you expect 2, a doorway you must crouch through.
- **Repeating identical galleries** with one tiny difference each loop (a cipher's "you've been here, but not quite").
Source: Gamezebo wonderland.jar spotlight; CurseForge Backrooms/Liminal Horrors packs.

### 3.4 Lighting as horror (the dim-room craft)
- **Soul fire / soul lantern blue-white** is the canonical "wrong, cold, dead" light (it's why the Ancient City uses it).
  Reserve warm orange torch/campfire for *living* sites (the `kept_light_home_01` hearth must be the only warmth in the
  Hold — that contrast is the whole point of the Kept Light).
- **Light-blocking with carpets/trapdoors/light-level-0 displays:** keep ambient light low so soul-lanterns *carry*.
- **Off-grid dark via `Display.Brightness(0,0)`** (§3.1) lets you paint a single block jet-black even under a lantern —
  a "hole the light won't touch."
Pitfall: 1.18+ light propagation will quietly brighten a "dark" room through one gap — seal cracks or the dread leaks out.

**Apply to The Observance:** (1) Use **block_display tilts (3–8°) + non-uniform scale on a handful of keeper-pillars and
the threshold lintel** so the Deep Hold is *almost* square — uncanny, never cartoonish. (2) Wrap the Watcher reveal in a
proximity **Darkness effect + near-black biome fog** to get the From-The-Fog envelope with no install. (3) Build the
Warrens at ceiling-height 2–3 with pooled soul-light; make `kept_light_home_01` the **only warm light** in the whole Hold.
(4) Reuse `NamedMobBeat`'s PDC-tag-and-remove discipline for every display entity.

---

## 4. Weathering the build with FAWE (the paste pipeline already in the repo)

The repo already wires **FastAsyncWorldEdit** (`FaweSchematicPaster`). FAWE/WorldEdit **brushes** are how you age a clean
build into a ruin in minutes (do this in-build, then export the `.schem`):
- **`//brush erode [size] [erodeFaces erodeRec fillFaces fillRec]`** (a.k.a. the erosion/morph brush). Modes:
  - **erode** (default): pushes terrain *in* — chews stone away → crumbled edges.
  - **lift:** pushes *out*.
  - **fill / melt:** fill recesses / do the opposite.
  - **smooth:** rounds shapes.
  Newer WorldEdit exposes the same via the **morph brush** with `/br erode` and `/br dilate` presets (erode = shrink/age,
  dilate = expand/encrust).
- **`//brush smooth [radius] [iterations] [mask]`** — softens hard machine edges into a worn read; the smooth brush works
  on an area *double* the given radius.
- **`//brush blendball [radius]`** — gentler smoothing; good for organic decay without melting detail.
- **Masks** (`/mask`) restrict brushing to a material so you erode *only* the dressed stone and leave runes/lecterns intact.
- `/br vis 1` to preview before committing.
Pitfall: erode is destructive and **respects whatever mask you forgot to set** — mask to the stone palette so you don't
chew through carved rune panels or protected answer-sites; FAWE undo is available but verify region selection first.
Source: WorldEdit 7.4 docs *Brushes*; FAWE GitBook *Brushes*; ManaCube WorldEdit guide; Madeline Miller WorldEdit 7.3 notes.

**Apply to The Observance:** Build each gallery *clean*, then **mask to the deepslate/blackstone palette and run `//brush
erode`** along edges/corners + a light `//brush smooth` pass to get "carved long ago, half-collapsed" — then export the
`.schem` per `structures.md`. Never brush over rune lecterns/answer-sites (mask them out); `BeatProtectionListener` would
restore broken carvings anyway, but don't rely on it during the build.

---

## 5. Murals — map art & item-frame walls server-side, and the display-text alternative

Two ways to put images/text on Deep Hold walls.

### 5.1 Map-art murals (raster images, e.g. a "found photo," a sigil, a redacted document)
- **Server-side, no client mod:** **ImageOnMap** (zDevelopers, Bukkit/Paper) — `/tomap <URL>` renders an image to a map;
  **big images auto-split into a grid** of maps (a 1024×1024 → 16 maps) for whole-wall murals; `resize-covered` (no
  distortion, crops edges) or `resize-stretched`. **Runs from console/command-blocks** → fully automatable by the plugin.
  Permission `imageonmap.new`.
- **Modern alternative:** **ImageFrame** (LOOHP, Paper plugin) — actively maintained, animated-map + combined-map murals,
  good 1.21 support; prefer it if ImageOnMap lags a Paper version.
- **Authoring tools (offline, give cleaner output):** **rebane2001 MapartCraft** or **MC Map Item Tool (djfun)** convert an
  image to the exact block/map palette; useful if you want *placed-block* map art instead of plugin maps.
- **Mural build rules:** N×M image = N frames across × M down; **light evenly from behind** (glowstone/`Display.Brightness`)
  or maps render muddy from uneven light; **use `glow_item_frame`** so frames are invisible and the image reads clean;
  prefer **simple, high-contrast source images** (a sigil, a redacted page, a single face) over busy photos.
- **Pitfall (perf):** item frames + maps are **entities** — too many in one spot = FPS lag, exactly the moment Ethan is
  recording. Keep mural walls modest and spread out; despawn-protect with PDC tags.
Source: ImageOnMap (SpigotMC/GitHub/zcraft docs); ImageFrame (Hangar); MapartCraft; MC Map Item Tool; minecraftart.net
server map-art guide; Empire MC "maps as paintings."

### 5.2 The rune/text mural via **TextDisplay** (fits our cipher aesthetic better)
For the *rune* carvings the game is read in, **`TextDisplay`** entities (with the `observance:runes` font from the pack)
beat maps: crisp at any scale, server-spawned, transformable (tilt/scale per §3.1), `setBackgroundColor(transparent)` so
it sits *in* the stone, `setBrightness` to make it glow faintly in the dark. This is the holographic-carving look without
hand-placing blocks. `structures.md` already specifies signs/lecterns/text-displays in `observance:runes` — TextDisplay is
the upscale-able version for hero carvings.
Source: PaperMC *Display Entities*; Minecraft Wiki *Display*.

**Apply to The Observance:** Use **ImageFrame/ImageOnMap map-murals** for the *non-rune* found-media (a redacted notice, a
"photo" of the seventh keeper's effaced face at `the_cold_hearth`, a hand-drawn Hold map) — automatable, atmospheric,
spread thin for perf. Use **TextDisplay in `observance:runes`** for hero rune-carvings that must be legible and huge
(the Rosetta ring, the Accepting-floor inscription). Keep both behind line-of-sight reveal discipline.

---

## 6. The recording rig (Ethan-only, capture-time, NEVER pushed)

This is the "blow-up-worthy YouTube" layer. Everything here lives on **Ethan's recording client**; players never see it.

### 6.1 Replay Mod — the camera (primary recommendation)
**Replay Mod** records sessions to tiny files and gives an **in-game keyframe editor** — author smooth cinematic camera
moves *after the fact*, including shots impossible to fly live.
Workflow (exact keys/buttons):
1. Enable auto-record in Replay Settings; `M` drops event markers while playing (mark the moment the Watcher is found).
2. Open the replay → scrub the timeline.
3. **Green rhomb button = add a Position Keyframe** (stores x/y/z/yaw/pitch/roll). Place several → a path.
4. **Green hourglass = Time Keyframe** (controls replay timestamp along the path; **no backward time travel** — it crashes
   renders).
5. **`O` toggles interpolation:** **Cubic spline** (smooth, default) vs **Linear** (straight). `H` toggles the red path
   line; `V` syncs keyframe timing; right-click a keyframe to jump there; `DELETE` removes; Ctrl+Z/Y undo/redo.
6. Spectator keyframes: right-click an entity to spectate (button turns blue); need **≥2 spectator keyframes** of the same
   entity — good for an over-the-shoulder follow of a friend as the Watcher looms behind.
7. **Render Camera Path** → choose codec (MP4/WEBM or **PNG/OpenEXR sequence** for grading), resolution, fps, plus
   nametag-hiding, **camera stabilization**, chroma key, depth maps. Hold `Ctrl` on Render = high-perf mode.
Pitfalls: needs **FFmpeg** configured; **camera can't go below Y=0 or above Y=255** while rendering (the Deep Hold is in
deep-negative Y — *this matters*: render the deep scenes at higher build-Y or be aware Replay Mod may clamp); semi-transparent
blocks break depth maps; backward time travel crashes.
Source: replaymod.com/docs; Badlion/Sportskeeda/PremiumMinecraft Replay guides.

> **Action item flagged for the build:** the Deep Hold sits in deep-negative Y, but Replay Mod's render camera is bounded to
> Y 0–255. Decide early whether the *recorded* slice is built/copied into a 0–255 band for filming, or whether a different
> camera tool is used for the deep scenes.

### 6.2 Camera Studio / CMDCam — the lightweight alternative
**Camera Studio** (and **CMDCam**) = command/point-based camera paths with interpolation, zoom, tilt, and **independent
day/night-length control** — lighter than Replay Mod, no replay file, good for *live* scripted flythroughs of a static set
(e.g. a slow reveal of the Accepting floor). Use when you don't need to re-time recorded gameplay. **CPCameraStudioReborn**
is a server-side Spigot variant if you'd rather drive the camera from the plugin.
Source: 9minecraft Camera Studio; CurseForge CMDCam; SpigotMC CPCameraStudioReborn; CraftyMania cinematics roundup.

### 6.3 Shaders — the grade (Iris, recording-only)
- **Complementary Reimagined** (Iris) — top-tier optimized; **excels underground/in caves** with warm-but-not-harsh torch/
  lava light and dynamic fog → ideal for a deepslate hall lit by lone lanterns. Started as a BSL edit; keeps MC's stylized
  look while adding colored light, soft shadow, bloom.
- **BSL** — the classic cinematic base; slightly more "filmic" sky/water.
- **Horror-specialized:** **Paranoia VHS: Isolation** (desaturated bleak VHS look) and **Vintage Vibes** (grain + fog +
  vignette baked in) — for the found-footage/"unwell tape" cut.
Install: **Iris** (Fabric, lightweight) loads the pack. Dial settings toward **deep shadows, desaturated midtones, dense
fog, low bloom** for dread.
Pitfall: shaders + **Distant Horizons** need matched Iris versions (DH 2.1+ wants Iris 1.7+; 1.21 availability is fiddly —
verify the exact build pairing before a shoot).
Source: complementary.dev; CurseForge Complementary Reimagined / Paranoia VHS / Vintage Vibes; shadersmods cinematic list.

### 6.4 Distant Horizons — the scale (recording-only, optional)
**Distant Horizons** adds LOD render of far terrain → a *vast* render distance so the oversized descent/galleries read as
endless on camera. **Recording-only** (it's a client mod; never required of players). Set CPU load to "I Paid For The Whole
CPU" to speed LOD baking. **Version-pin carefully** with Iris/Sodium for 1.21.
Source: Modrinth Distant Horizons; IrisShaders DH discussion; Steveplays28 DH shader-compat gist.

### 6.5 Post (outside Minecraft) — the dread grade
Horror grade = **deep shadows + desaturated midtones + eerie blue-green tint**; add a **light vignette (-15 to -25 at
corners)** and **3–5% film grain** — both subconsciously read as "cinema." Cut on rhythm; let dread breathe (timing of the
cut is what makes/breaks a reveal). Sound: every ambient layer chosen to unsettle.
Source: KROCK.IO editing-for-horror; markstudios/capcut/filmora color-grading guides; gamineAI game-cinematic post.

**Apply to The Observance:** Standardize Ethan's rig as **Iris + Complementary Reimagined** (dial dark) for hero shots,
**Replay Mod** for camera (resolve the Y-bound issue first), **Camera Studio/CMDCam** for simple static-set flythroughs,
**Distant Horizons** only when a shot needs the descent to look infinite. Render PNG/EXR sequences and grade to the horror
spec (desat + blue-green + vignette + 3–5% grain). For the ARG's in-fiction "found tape" beats, switch to **Paranoia VHS**.
None of this touches the auto-pushed pack or any player's client.

---

## 7. NPC presence — when a custom rig is worth it (cross-ref atmosphere-stack §1)

`atmosphere-stack.md` already made the engine call; the *craft* finding for this lane: **a vanilla mob you only ever
DISCOVER already reads supernatural** — the reveal discipline does the work, not the model. Concretely:
- **Default = `NamedMobBeat`** (vanilla husk/stray/warden-silhouette: silent, no-drift, invulnerable, glow optional,
  spawned out of line of sight). With the rune-font name + optional glow, this is the canonical "tall silent watcher."
- **A custom rig (ModelEngine R4 + MythicMobs, Java-21-safe; or FreeMinecraftModels if staying free) is worth it ONLY for
  one or two HERO beats** where the camera holds on the figure long enough that silhouette detail matters — i.e. a recorded
  cinematic close-up, not ambient presence. For 95% of beats the vanilla mob + reveal discipline wins.
- **For the *recorded* hero shot specifically**, a Citizens2/ZNPCsPlus NPC or a ModelEngine rig posed motionless and shot
  through a Replay Mod slow push is the highest-value use of a custom model — the cost is justified by the frame, not the
  gameplay.
Source: `atmosphere-stack.md §1`; PaperMC display/entity docs; ModelEngine R4 / MythicMobs / FreeMinecraftModels listings.

**Apply to The Observance:** Ship the vanilla `NamedMobBeat` watcher everywhere. Reserve a single ModelEngine R4 (or
FreeMinecraftModels) rig for the **one cinematic Watcher hero shot** in the YouTube cut, posed and recorded — never as a
live mechanic the friends fight.

---

## 8. Top pitfalls (consolidated)
1. **Flat single-block walls** — violate the 60/30/10 + push/pull-every-3–4 rules → "Minecraft default," dread dies.
2. **No tuff in the gradient** — dark/light bands harshly; tuff is the mandatory bridge.
3. **Display-entity leaks** — never-unloading chunks never GC them; PDC-tag + `remove()` (reuse `NamedMobBeat`'s pattern).
4. **Light leaking into "dark" rooms** — 1.18+ propagation brightens through one gap; seal cracks or use `Brightness(0,0)`.
5. **Eroding over runes** — always mask FAWE brushes to the stone palette; don't chew carved/answer-sites.
6. **Mural/item-frame lag** — maps + frames are entities; too many = FPS drop mid-record; spread thin, PDC-protect.
7. **Replay Mod Y-bound** — render camera clamps Y 0–255 but the Deep Hold is deep-negative Y; resolve before filming.
8. **DH/Iris/Sodium version mismatch on 1.21** — pin exact builds before a shoot.
9. **Recording rig creep** — shaders/DH/Replay/Camera Studio are Ethan-only; NEVER let them touch the auto-pushed pack
   (would break Path A).

---

## Sources
- BlockBlend — deepslate gradient guide, medieval palettes, Ancient-City palette note: https://blockblend.app/guides/deepslate-gradient-guide
- DeltaCalculator deepslate palette: https://www.deltacalculator.com/minecraft/block-palette/deepslate/
- Craftdex stone-variant palette guide: https://craftdex.net/articles/stone-variant-palette-guide
- minecraftgradient.blog deepslate→stone: https://minecraftgradient.blog/deepslate-to-stone-gradient/
- Minecraft Wiki — Ancient City: https://minecraft.wiki/w/Ancient_City
- Minecraft Wiki — Display: https://minecraft.wiki/w/Display
- Minecraft Wiki — Fog / Effect (dimension) / Custom dimension: https://minecraft.wiki/w/Fog
- PaperMC dev docs — Display Entities: https://docs.papermc.io/paper/dev/display-entities/
- Minecraft.net — Raeyzeus' Top 5 Building Tips: https://www.minecraft.net/en-us/article/raeyzeus-top-5-building-tips
- Sportskeeda — 4 best ways to add depth: https://www.sportskeeda.com/minecraft/4-best-ways-add-depth-minecraft-build
- WorldEdit 7.4 docs — Brushes: https://worldedit.enginehub.org/en/latest/usage/tools/brushes/
- FAWE GitBook — Brushes: https://intellectualsites.gitbook.io/fastasyncworldedit/command-utilities/brushes
- ImageOnMap (zDevelopers) — SpigotMC/GitHub/zcraft: https://github.com/zDevelopers/ImageOnMap
- ImageFrame (LOOHP) — Hangar: https://hangar.papermc.io/LOOHP/ImageFrame
- MapartCraft (rebane2001): https://rebane2001.com/mapartcraft/
- MC Map Item Tool (djfun): https://mc-map.djfun.de/
- Replay Mod docs: https://www.replaymod.com/docs/
- Complementary Reimagined: https://www.complementary.dev/shaders/
- Paranoia VHS: Isolation (shader): https://www.curseforge.com/minecraft/shaders/paranoia-vhs-isolation
- Distant Horizons: https://modrinth.com/mod/distanthorizons
- Camera Studio / CMDCam / CPCameraStudioReborn: https://www.curseforge.com/minecraft/mc-mods/cmdcam
- From The Fog (north-star reference): https://lunareclipse.studio/creations/from-the-fog
- KROCK.IO — editing for horror (post): https://krock.io/blog/stay-creative/editing-for-horror-creating-suspense-and-dread-through-post-production/
- wonderland.jar liminal-space spotlight: https://www.gamezebo.com/features/minecraft-mod-spotlight-wonderland-jar/
