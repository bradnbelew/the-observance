# mc-structures — World & Structure tech for The Observance (Paper 1.21.x / Java 21)

Research lane: code-level world mutation via FastAsyncWorldEdit (FAWE) + WorldEdit API, with
"reveal discipline" (only mutate when unobserved). Target consumers: `SmallStructureBeat`,
the FAWE branch, and the showrunner's between-session structure spawns.

Convention used below: every technique ends with **→ Observance:** = the concrete application.

---

## 0. The single most important fact (resolves the whole async question)

**Vanilla Bukkit `block.setType()` / `world.setBlockData()` throws on any thread but the main
server thread.** Spigot's `AsyncCatcher` actively detects "Asynchronous block remove/place!" and
throws `IllegalStateException`. This is *not* advisory — it crashes the task.

**FAWE is the deliberate exception.** When FAWE is installed, a WorldEdit `EditSession` (or
`setBlocks(region, block)`) routes writes through FAWE's own chunk queue, which *bypasses*
AsyncCatcher and is engineered to be called off-thread. So the rule for The Observance is:

- **Bulk / schematic / region writes → FAWE EditSession, run async** (off the tick loop).
- **One-off cosmetic blocks, anything that must interact with vanilla block events / BlockState
  / inventories / signs text → main thread via `Bukkit.getScheduler().runTask(plugin, …)`.**

Sources: [Bukkit AsyncCatcher thread](https://bukkit.org/threads/world-settypeanddata-not-working.460582/),
[FAWE async tips wiki](https://github.com/boy0001/FastAsyncWorldedit/wiki/Some-tips-when-using-the-FAWE-API),
[FAWE async paste discussion #2484](https://github.com/IntellectualSites/FastAsyncWorldEdit/discussions/2484).

**→ Observance:** Give `SmallStructureBeat` two code paths: a FAWE path (`schem` pastes, region
fills) that runs on an async executor, and a tiny main-thread path for single decorative blocks
(a placed totem, a flipped lever) where you want the vanilla physics/update. Never call vanilla
`setType` from the async FAWE path.

---

## 1. Load a schematic (modern WorldEdit/FAWE API)

Canonical, format-discovering loader. Classes:
`com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats`,
`ClipboardFormat`, `ClipboardReader`, `com.sk89q.worldedit.extent.clipboard.Clipboard`.

```java
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;

Clipboard load(File file) throws IOException {
    ClipboardFormat format = ClipboardFormats.findByFile(file); // sniffs the format
    if (format == null) throw new IOException("Unknown schematic: " + file);
    try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
        return reader.read(); // Clipboard is in memory; cache it
    }
}
```

- `ClipboardReader` is `Closeable` → always try-with-resources.
- `ClipboardFormats.findByFile(file)` auto-detects; `findByAlias("schem")` forces one.
- File reading is pure I/O — **safe and good to do async** (and ideally cache the `Clipboard`
  object at startup so the reveal moment has zero disk latency).

Sources: [WorldEdit Clipboard API examples](https://worldedit.enginehub.org/en/latest/api/examples/clipboard/),
[Madeline Miller — load/save schematics](https://madelinemiller.dev/blog/how-to-load-and-save-schematics-with-the-worldedit-api/).

**→ Observance:** At plugin enable, preload every structure `.schem` from the beat library into a
`Map<String, Clipboard>` once. The reveal beat then only does the paste — no FS hit at the
dramatic moment, no stutter that tips the player off.

---

## 2. Paste — main-thread WorldEdit form

```java
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

void paste(Clipboard clipboard, org.bukkit.World bukkitWorld, int x, int y, int z) {
    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
        Operation op = new ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(BlockVector3.at(x, y, z))
                .ignoreAirBlocks(true)   // see §6 idempotency
                .copyEntities(false)
                .build();
        Operations.complete(op);
    } // try-with-resources flushes/closes the EditSession
}
```

- `BukkitAdapter.adapt(world)` is the bridge from Bukkit `World` → WorldEdit `World`.
- `.to(...)` is **offset by the clipboard's offset/origin** (see §5).
- Rotation/flip: call `holder.setTransform(new AffineTransform().rotateY(90))` *before*
  `createPaste`.
- Closing the `EditSession` flushes; if you build it manually (not try-with-resources) you must
  call `editSession.flushSession()` (older: `flushQueue()`).

Sources: [WorldEdit Clipboard API examples](https://worldedit.enginehub.org/en/latest/api/examples/clipboard/),
[Madeline Miller](https://madelinemiller.dev/blog/how-to-load-and-save-schematics-with-the-worldedit-api/).

---

## 3. Paste — async FAWE form (the one to actually use)

FAWE adds `newEditSessionBuilder()` and lets the whole thing run off-thread. Run the paste inside
`runTaskAsynchronously`; FAWE's queue handles chunk loading + write safety.

```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
    try (EditSession editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(weWorld)
            .fastMode(true)        // skip physics/neighbour updates (faster, quieter)
            .changeSetNull()       // <-- disable undo history; we never undo a reveal
            .maxBlocks(-1)         // no block cap
            .build()) {

        Operation op = new ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(BlockVector3.at(x, y, z))
                .ignoreAirBlocks(true)
                .copyEntities(false)
                .build();
        Operations.complete(op);
        editSession.flushSession(); // explicit flush before exiting the async task
    }
});
```

FAWE performance tips that matter here:
- **Disable what you don't need.** `changeSetNull()` (or `EditSession#setChangeSet(null)`) drops
  undo recording — undo is the single biggest memory/IO cost and The Observance never undoes.
- `fastMode(true)` = no neighbour/physics updates → quieter paste, no cascading block updates
  that could light up a redstone lamp or drop gravel and reveal the trick.
- Use `editSession.setBlocks(region, pattern)` for region *fills* (chunk-parallel, preloaded)
  instead of looping `setBlock`.
- For position sets, FAWE's `BlockVectorSet` uses ~800× less memory than `HashSet<BlockVector3>`.
- Iterate with `RegionVisitor` / `FastChunkIterator`, never `for (var pt : region)`.

**Pitfall — not everything in FAWE is async-safe.** Block/biome writes are; many
`AsyncWorld`/`Actor` convenience methods are *not* fully async-optimized. Keep the async task to
pure block/biome work; bounce anything else to the main thread with a scheduler/`TaskBuilder`.

**Pitfall — server forks.** FAWE maintainers explicitly say "use Paper" (you are) and that very
old version lines (1.16.5-era) carried paste bugs. On 1.21.x you're in the supported lane.

Sources: [FAWE async paste #2484](https://github.com/IntellectualSites/FastAsyncWorldEdit/discussions/2484),
[FAWE API tips wiki](https://github.com/boy0001/FastAsyncWorldedit/wiki/Some-tips-when-using-the-FAWE-API).

**→ Observance:** `SmallStructureBeat` should build its `EditSession` with
`fastMode(true).changeSetNull().maxBlocks(-1)` and run inside an async task. The reveal is
fire-and-forget; we never want history, physics, or a memory balloon from 50 schem pastes a
session.

---

## 4. Relighting — the #1 visible tell that a structure was *placed*

A pasted structure with stale lighting is the classic "this wasn't here naturally" giveaway:
black interiors, light leaking through walls, neighbouring torches not casting. FAWE has a long
history of lighting bugs and an explicit relight config.

- FAWE config key **`lighting.mode`**: `0` = none, `1` = optimal (relight only changed sections),
  `2` = all. For believable reveals you want at least `1`.
- **`lighting.delay-packet-sending`** controls whether the light-update packets
  (`ClientboundLightUpdatePacket`) are batched/delayed before resend to clients. After relight,
  FAWE re-sends affected chunks to all viewers.
- In code, FAWE EditSession exposes relight controls; the simplest robust approach for a small
  structure is: paste with `fastMode(true)`, then **force a relight of the affected region**
  (FAWE `Relighter` / `relight` on the queue), OR resend the chunk to nearby players so the
  client recomputes/receives correct light.
- Known failure mode: "Certain chunk section light glitch related to how FAWE creates chunk
  sections" — if you ever see a structure paste with a pitch-black or fullbright section, that's
  this class of bug; mitigate by relight mode `2` for that paste or a follow-up chunk resend.

Sources: [FAWE broken lighting #459](https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/459),
[chunk-section light glitch #497](https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/497),
[FAWE relight fix commit](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/bdc14c10c79d2c8cf9416ea63ae75ed06d1d5925),
[Light Cleaner (relight tooling)](https://www.spigotmc.org/resources/light-cleaner.42469/).

**→ Observance:** After every structure paste, queue a relight of the bounding box and, as a
belt-and-suspenders, resend the affected chunks to any player within render distance on the next
tick. Add a config flag `observance.relightMode` so you can bump to `2` for any structure that
shows lighting artifacts. Treat "structure with wrong light" as a P1 immersion bug, not cosmetic.

---

## 5. Origin / offset — the "floating or buried by 1 block" trap

Sponge `.schem` stores **both the copy origin and your offset** to it. `//copy` records where you
stood relative to the selection; on paste, `.to(p)` places the clipboard *relative to that saved
offset*, not flush at `p`. This is why API pastes routinely land 1 block off, half-buried, or
floating versus what you saw with `//paste`.

Two reliable fixes:
1. **Author with a known offset.** When saving (`//copy` then `//schem save`), stand at a
   deterministic point (e.g. the structure's intended block-0 corner) so the stored offset is
   predictable.
2. **In code, neutralize the offset** by pasting to `clipboard.getOrigin()` semantics or by
   subtracting `clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin())` so the
   minimum corner lands exactly at your target. Test once visually, then lock the math.

Sources: [WorldEdit Clipboard usage docs](https://worldedit.enginehub.org/en/latest/usage/clipboard/),
[WorldEdit API examples](https://worldedit.enginehub.org/en/latest/api/examples/clipboard/).

**→ Observance:** Standardize the authoring loop: select → stand at the NW-bottom corner →
`//copy` → `//schem save observance/<name>`. Then in code, paste to the literal target block and
verify the first time that NW-bottom corner == target. Bake a 1-block ground-snap check (raytrace
down to find surface Y) so structures never float or sink on uneven terrain.

---

## 6. Idempotency / double-paste safety

A reactive director *will* occasionally fire the same beat twice (retries, missed
acknowledgement, restart mid-task). Defenses:

- **`.ignoreAirBlocks(true)`** so re-pasting doesn't blow away blocks a player added inside, and
  so air in the schem doesn't clobber existing terrain. (Use `false` only when you *want* a clean
  hollow.)
- **Idempotency key**: store `(structureId, x,y,z)` of every placed reveal in Supabase / a local
  set. Before pasting, check the key; skip if present. This is cheaper and more reliable than
  reading blocks back.
- **Block-probe guard**: optionally read one signature block (e.g. the structure's center) on the
  main thread; if it already matches, skip. (Block reads on a loaded chunk are fine; reads on
  unloaded chunks force a load — see §8.)
- Persist a **revert snapshot** *before* pasting if a structure is meant to be transient: copy the
  target region to a temp `Clipboard` first, then paste; to revert, paste the snapshot back. Since
  you ran `changeSetNull()`, WorldEdit undo is *not* available — your snapshot is the undo.

**→ Observance:** Every `SmallStructureBeat` write goes through a `placedStructures` ledger keyed
by `(beatId, blockPos)`. Reveal = check ledger → snapshot region → paste → record ledger. Revert
beat = paste snapshot back. This makes the showrunner safe to re-run and gives you clean teardown
between sessions.

---

## 7. Reveal discipline — only mutate when unobserved

North star is "From The Fog, but it knows your name": structures should *appear* without the
player catching the pop-in. Gate every paste behind a visibility test.

**(a) Frustum/FOV test (cheap, first filter).** Dot product of the player's normalized look
direction against the normalized (target − eye) vector:

```java
Location eye = player.getEyeLocation();
Vector look = eye.getDirection();                 // already normalized
Vector toTarget = target.toVector().subtract(eye.toVector()).normalize();
double dot = look.dot(toTarget);
double halfFovCos = Math.cos(Math.toRadians(70));  // MC vFOV ~70°, widen for safety
boolean inView = dot >= halfFovCos;                // target roughly in the player's cone
```

If `dot < halfFovCos`, the target is behind/beside the player → safe to build regardless of walls.

**(b) Occlusion test (when target IS in the FOV cone).** Use a ray trace from eye to target; if a
solid block is hit first, the player can't see it:

```java
RayTraceResult r = world.rayTraceBlocks(
        eye, toTarget, eye.distance(target),
        FluidCollisionMode.NEVER, true /*ignorePassableBlocks*/);
boolean occluded = (r != null && r.getHitBlock() != null
        && r.getHitBlock().getLocation().distance(eye) < eye.distance(target) - 1.0);
boolean canSee = inView && !occluded;
```

- Paper 1.21 added `BlockCollisionMode` overloads to `World#rayTrace` / `World#rayTraceBlocks`
  (PR #12162) replacing the old `ignorePassableBlocks` boolean — prefer those on current builds.
- `LivingEntity#getLineOfSight(transparent, maxDistance)` and `hasLineOfSight(Entity/Location)`
  are simpler built-ins but coarser; `rayTraceBlocks` with precise collision shapes is the
  accurate one.

**(c) Combine: build only if NO nearby player can see it.** Loop all players within ~render
distance of the target; if every one is either out-of-FOV or occluded, paste. Otherwise defer the
beat one tick / a few seconds and re-check. This is the core "soft-pressure" engine: the structure
materializes the instant attention drifts.

**(d) The smarter trick — build behind them on purpose.** Rather than wait for line-of-sight to
clear, *choose the placement* to be just outside the current FOV cone (use the dot-product test to
pick a spot at, say, dot ≈ −0.3 relative to look), so the reveal happens in the player's
periphery/behind. Maximum "was that there before?" effect.

Sources: [Paper ray trace PR #12162](https://github.com/PaperMC/Paper/pull/12162),
[Paper LivingEntity javadocs (getLineOfSight/hasLineOfSight)](https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/LivingEntity.html),
[Dot-product FOV tutorial](https://freakoutstudios.com/dot-product-fov.html),
[From The Fog (genre reference)](https://modrinth.com/project/p1WH6sHr).

**→ Observance:** Implement a `RevealGate.canMutate(target, radius)` util: FOV cone test → ray
trace occlusion → return true only if every nearby player fails to see it. `SmallStructureBeat`
calls it; on false, it re-queues itself with a short backoff. Add a "build in periphery" mode that
picks placement just outside the look cone for the strongest unnoticed-appearance effect.

---

## 8. Chunk load state — don't trigger loads, don't write to unloaded chunks

- **`world.isChunkLoaded(cx, cz)`** before any read/probe. Calling `getBlockAt` /
  `getChunkAt` on an unloaded chunk **forces a synchronous chunk load** — a main-thread stall
  *and* it can generate terrain you didn't want, near a player.
- FAWE *can* write to and auto-load chunks for its queue, but for reveal beats you almost always
  only want to place where a **player is present and the chunk is already loaded** (that's the
  whole point — they're nearby to be spooked).
- Use `world.getLoadedChunks()` or proximity to a player to constrain candidate placements to
  loaded space. Skip beats whose target chunk isn't loaded rather than forcing a load.
- Paper async chunk APIs (`getChunkAtAsync`) exist if you must ensure load without blocking the
  tick, but for The Observance prefer "only act in already-loaded, player-adjacent chunks."

**→ Observance:** `RevealGate` and `SmallStructureBeat` both short-circuit if
`!world.isChunkLoaded(target)`. Reveal beats are inherently player-proximate, so a loaded-chunk
precondition is free and prevents accidental world-gen / main-thread hitches.

---

## 9. Region protection / anti-grief for placed structures

When you spawn a structure as ARG content, players may try to mine it apart (or accidentally
destroy a clue). WorldGuard API lets you cordon it off programmatically.

```java
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.math.BlockVector3;

BlockVector3 min = BlockVector3.at(x1, y1, z1);
BlockVector3 max = BlockVector3.at(x2, y2, z2);
ProtectedCuboidRegion region = new ProtectedCuboidRegion("observance_" + id, min, max);
region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);   // or allow, per design
region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);

RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
RegionManager regions = container.get(BukkitAdapter.adapt(world));
regions.addRegion(region);
```

- Region IDs: only `A-Z a-z 0-9 _ , ' - + /`; validate with `ProtectedRegion.isValidId(id)`.
- Set flags *before or after* `addRegion`; `setFlag` / `getFlag`.
- Remove protection when a structure is meant to be destructible/temporary:
  `regions.removeRegion(id)`.

Sources: [WorldGuard region developer docs](https://worldguard.enginehub.org/en/latest/developer/regions/protected-region/),
[WorldGuard managers docs](https://worldguard.enginehub.org/en/latest/developer/regions/managers/).

**→ Observance:** Decide per beat: *clue* structures (must survive to be read) get a
DENY-break region auto-created on paste and removed at teardown; *scare* structures (meant to be
torn apart by paranoid players) stay unprotected. Tie region lifecycle to the `placedStructures`
ledger from §6 so teardown removes both blocks and region.

---

## 10. Authoring loop — making the `.schem` library

Standardized in-game workflow so code-side offsets stay predictable (§5):

1. Build the structure in a staging world.
2. Stand at the **NW-bottom corner** (deterministic origin).
3. Select region (`//wand`, two corners) → **`//copy`** (records origin/offset relative to you).
4. **`//schem save observance/<name>`** → writes Sponge `.schem` (modern WorldEdit defaults to
   Sponge v2; v3 is newer — confirm your FAWE build's default, target
   `BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC` when saving via API).
5. Drop the file in the server's `plugins/FastAsyncWorldEdit/schematics/` (or your beat library
   dir) and reference it by name from the beat config.

- **Sponge v3** support note: FAWE has a tracked issue/PR for v3; on 1.21.x current FAWE,
  `SPONGE_V3_SCHEMATIC` is the forward format. If a `.schem` won't load, it may be an
  MCEdit-legacy `.schematic` — those need the legacy reader and pre-1.13 block IDs; re-save as
  Sponge.

Sources: [WorldEdit //schematic docs](https://worldedit.enginehub.org/en/latest/usage/clipboard/),
[Sponge schematic formats (DeepWiki)](https://deepwiki.com/EngineHub/WorldEdit/3.2-schematic-formats),
[FAWE Sponge v3 support #2547](https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/2547),
[MCEdit schematics in 1.13+](https://madelinemiller.dev/blog/use-mcedit-schematics-113/).

**→ Observance:** Lock the authoring SOP into the beat-library README: NW-bottom-corner origin,
Sponge v3, named `observance/<name>.schem`. Every structure follows it so the code never needs
per-structure offset tuning.

---

## 11. Consolidated pitfall checklist (for the FAWE branch PR)

| Pitfall | Symptom | Fix |
|---|---|---|
| Vanilla `setType` off-thread | `IllegalStateException: Asynchronous block place` crash | Use FAWE EditSession async, or bounce vanilla writes to `runTask` main thread (§0) |
| Undo history left on | RAM balloon / disk thrash after many pastes | `changeSetNull()` on the builder (§3) |
| Physics on | Gravel falls, redstone fires, cascading updates leak the reveal | `fastMode(true)` (§3) |
| Stale lighting | Black/fullbright structure, light leaks → obvious it was placed | relight mode ≥1, resend chunks (§4) |
| Origin/offset | Structure floats / buried 1 block / off-center | Standard NW-corner authoring + snap math (§5) |
| Double paste | Beat fires twice, clobbers player edits | `ignoreAirBlocks(true)` + idempotency ledger (§6) |
| Pop-in seen | Player watches blocks appear | `RevealGate` FOV + raytrace occlusion before paste (§7) |
| Unloaded chunk | Main-thread stall / accidental world-gen | `isChunkLoaded` precondition (§8) |
| No flush | Blocks never appear / partial paste | try-with-resources or explicit `flushSession()` (§2,§3) |
| Grief | Clue structure mined apart | WorldGuard DENY region tied to ledger (§9) |

---

## 12. Reference EditSession builder for The Observance (drop-in)

```java
// async-safe, no history, no physics — the standard "reveal" session
EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
        .world(BukkitAdapter.adapt(world))
        .fastMode(true)        // no neighbour updates
        .changeSetNull()       // no undo recording
        .maxBlocks(-1)         // no cap
        .build();
// ... ClipboardHolder(...).createPaste(session).to(pos).ignoreAirBlocks(true).build() ...
// Operations.complete(op); session.flushSession();
```

Wrap it: `Bukkit.getScheduler().runTaskAsynchronously(plugin, …)`, *after* `RevealGate.canMutate`
passed on the main thread (visibility checks read player state → do them on the main thread, then
hand off the paste to async).

---

### Primary sources
- WorldEdit Clipboard API examples — https://worldedit.enginehub.org/en/latest/api/examples/clipboard/
- WorldEdit Clipboard usage (origin/offset, //copy //schem) — https://worldedit.enginehub.org/en/latest/usage/clipboard/
- Madeline Miller, load/save schematics — https://madelinemiller.dev/blog/how-to-load-and-save-schematics-with-the-worldedit-api/
- FAWE async paste discussion #2484 — https://github.com/IntellectualSites/FastAsyncWorldEdit/discussions/2484
- FAWE API tips (history/fastmode/iterators) — https://github.com/boy0001/FastAsyncWorldedit/wiki/Some-tips-when-using-the-FAWE-API
- FAWE broken lighting #459 / chunk-section glitch #497 / relight fix commit — see §4 links
- FAWE Sponge v3 support #2547 — https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/2547
- Paper ray trace PR #12162 (BlockCollisionMode) — https://github.com/PaperMC/Paper/pull/12162
- Paper LivingEntity javadocs (getLineOfSight/hasLineOfSight) — https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/LivingEntity.html
- Dot-product FOV tutorial — https://freakoutstudios.com/dot-product-fov.html
- WorldGuard region developer docs — https://worldguard.enginehub.org/en/latest/developer/regions/protected-region/
- Bukkit AsyncCatcher thread — https://bukkit.org/threads/world-settypeanddata-not-working.460582/
- From The Fog (genre reference) — https://modrinth.com/project/p1WH6sHr
