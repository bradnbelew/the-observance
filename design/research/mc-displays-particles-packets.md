# Research: Display Entities, Particles & Packet Illusions (Paper 1.20.5–1.21.x)

> **RESEARCH REFERENCE — NOT STORY, SETUP, OR RUNTIME AUTHORITY.** Version-specific claims require fresh verification against Paper 1.21.11.

Lane: `mc-displays-particles-packets`. Goal — render runes/holograms/apparitions with NO resource pack, drive per-player illusions (a block/mob only one client sees), and make a vanilla mob *read* as the Watcher. Direct inputs to `NamedMobBeat`, `FakeBlockBeat`, `Private*` beats, and rune rendering as a pack-free fallback.

Stack already in play: PacketEvents (v2.x), Citizens2/ZNPCsPlus, FAWE, Multiverse, Paper 1.21.x / Java 21. Everything below is callable from a Paper plugin.

---

## 0. TL;DR decision table

| Want | Use | Pack? | Per-player? |
|---|---|---|---|
| Floating rune/word/sigil | `TextDisplay` (real entity) | no | via `setVisibleByDefault(false)` + `showEntity` |
| Glyph wall / fake structure preview | `BlockDisplay` | no | yes (per-player visibility) |
| Floating object / cursed item / severed head | `ItemDisplay` (PLAYER_HEAD) | no | yes |
| Block that only ONE player sees | `Player.sendBlockChange` (no entity) | no | inherently |
| Mob only ONE player sees | PacketEvents fake entity (+EntityLib) | no | inherently |
| Make a real mob read as apparition | `setGlowing/setInvisible/setSilent/setAI(false)` + metadata | no | glow color per-player via team or packet |
| Ambient dread haze | `spawnParticle` SCULK_SOUL / SOUL_FIRE_FLAME / DUST_COLOR_TRANSITION | no | `Player.spawnParticle` |

---

## 1. Display entities (the no-pack workhorse) — 1.19.4+

`Display` is the base interface; concrete: `TextDisplay`, `BlockDisplay`, `ItemDisplay`. They have **only a visual function** (no hitbox, no AI, no physics). This is the single most important pack-free tool in The Observance — runes, apparitions, ghost-builds, and floating text all become Display entities.

### 1.1 Spawning (Paper `World#spawn` with consumer initializer)
```java
TextDisplay rune = world.spawn(loc, TextDisplay.class, e -> {
    e.text(Component.text("ᛟ I SEE YOU ᛟ", NamedTextColor.DARK_RED));
    e.setBillboard(Display.Billboard.CENTER);        // always face viewer
    e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));// transparent bg
    e.setSeeThrough(true);                            // render through walls
    e.setShadowed(false);
    e.setBrightness(new Display.Brightness(15, 15));  // full-bright, ignore cave dark
    e.setViewRange(0.6f);                             // ~ how far it stays visible
    e.setPersistent(false);                           // don't save to chunk
});
```
`BlockDisplay`: `e.setBlock(Material.SCULK.createBlockData())`.
`ItemDisplay`: `e.setItemStack(ItemStack.of(Material.SKELETON_SKULL))`.

Note: a BlockDisplay's origin is the **block corner**; an ItemDisplay's origin is the **center**. This matters when you align a transform — offset BlockDisplays by −0.5 on each axis to center them.
Source: https://docs.papermc.io/paper/dev/display-entities/

### 1.2 Billboard (the "ghost always faces you" trick)
`Display.Billboard` enum — `FIXED` (default, no rotation), `VERTICAL` (yaw only — text stays upright as you circle it), `HORIZONTAL`, `CENTER` (free, always flat-on to camera). For runes that should feel like they're *watching*, use `CENTER`. For floating words that read like signage, use `VERTICAL`.
Source: https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Display.Billboard.html

### 1.3 Transformation (scale / rotate / warp)
```java
Transformation t = new Transformation(
    new Vector3f(0f, 0f, 0f),          // translation
    new AxisAngle4f(0f, 0,1,0),        // left rotation
    new Vector3f(3f, 3f, 3f),          // scale (giant rune = 3x)
    new AxisAngle4f(0f, 0,1,0));       // right rotation
display.setTransformation(t);
// or raw: display.setTransformationMatrix(new Matrix4f()...);
```
Uses JOML `Vector3f` / `AxisAngle4f` / `Matrix4f`. Scale arbitrarily — a 10x TextDisplay = a billboard-sized message looming over a player's base; a 0.1x = a tiny sigil carved into a wall.
Source: https://docs.papermc.io/paper/dev/display-entities/

### 1.4 Brightness override (apparition glow without a light source)
`new Display.Brightness(int blockLight, int skyLight)`, each 0–15. Set `(15,15)` and the display renders **full-bright even in pitch dark** — perfect for a glowing rune or a ghostly figure that's visible but casts no actual light. Set `(0,0)` for an object that stays sinister-dark even in daylight.
Source: https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Display.Brightness.html

### 1.5 Glow outline on a Display
`display.setGlowing(true)` + `display.setGlowColorOverride(Color)` gives a per-entity colored outline that **bypasses the scoreboard-team requirement** (unlike mobs/players, where color must come from a team). Cheap way to make a rune pulse red.
Source: https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Display.html

### 1.6 Interpolation (smooth fade-in / drift / pulse) — and THE gotcha
Displays smoothly tween translation/scale/rotation, color, and opacity over ticks:
```java
display.setInterpolationDelay(0);        // start next tick
display.setInterpolationDuration(20);    // 1s tween
// THEN, on a LATER tick, change the interpolated property:
display.setTransformation(biggerTransform);
display.setTeleportDuration(20*2);       // smooth MOVEMENT over 2s (separate axis!)
```
**Gotcha (critical):** interpolation only triggers when the property *changes after the delay is set*. If you spawn and set the final transform in the same tick, the client has no "previous" value and it **snaps** instead of tweening. Pattern: spawn with start state → next tick set delay/duration → next tick apply target state. Multiple changes within one tick collapse into a single interpolation step (server syncs ≤ once/tick).
`setTeleportDuration` is a *separate* smoothing axis for position updates (good for a slow-drifting apparition); `0` = snap.
Sources: https://docs.papermc.io/paper/dev/display-entities/ ; https://minecraft.wiki/w/Display

### 1.7 Per-player visibility (a rune only the targeted player sees)
```java
display.setVisibleByDefault(false);      // nobody sees it
targetPlayer.showEntity(plugin, display);// only this player
// later: targetPlayer.hideEntity(plugin, display);
```
Requires the `Plugin` handle (Paper tracks ownership). This is the clean, no-packet route for `Private*` beats — a real entity exists server-side but is revealed to exactly one client.
Source: https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Entity.html

### 1.8 ItemDisplay player-head apparition (a floating face)
Build a `PLAYER_HEAD` ItemStack with a custom profile (texture = the Watcher's face), then `itemDisplay.setItemStack(head)` and `setItemDisplayTransform(ItemDisplayTransform.THIRDPERSON_RIGHTHAND)` for clean head-model orientation. Scaled 5–10x via transform = a giant face hanging in the fog with no resource pack. (For a *full* skin rather than a head, the `stable_player_display` resource-pack/shader trick exists but breaks Path A — avoid; the bare head needs no pack.)
Sources: https://docs.papermc.io/paper/dev/display-entities/ ; https://github.com/bradleyq/stable_player_display

### 1.9 Display pitfalls
- **Tracking range is 128 blocks** for displays (`spigot.yml` `entity-tracking-range: display: 128`) — much larger than mobs (48). A distant rune *will* render; rely on `setViewRange` (a multiplier, ~0.5–1.0 typical) to pull it in, not on tracking range.
- Always `setPersistent(false)` + tag with PDC so the showrunner can find-and-`remove()` them; orphaned displays accumulate and never despawn on their own.
- `TextDisplay` background defaults to a translucent dark box — set `setBackgroundColor(Color.fromARGB(0,0,0,0))` or `setDefaultBackground(false)` for clean floating glyphs.

**Apply to The Observance:** Displays are the rune/apparition engine with zero pack dependency. `TextDisplay` (CENTER billboard, brightness 15/15, transparent bg, glow override) = pack-free rune fallback; `ItemDisplay` PLAYER_HEAD scaled = the looming face; per-player `showEntity` = `Private*` reveals; interpolation = the slow fade-in that sells "it was always there."

---

## 2. Particles — ambient dread & directed effects

### 2.1 Signatures
```java
// everyone in range:
world.spawnParticle(Particle type, double x,y,z, int count,
                    double offX, offY, offZ, double extra, T data);
// ONE player only (the core per-player atmosphere primitive):
player.spawnParticle(Particle type, Location loc, int count,
                    double offX, offY, offZ, double extra, T data);
```
`count = 0` → spawns a **single** particle and `offX/Y/Z` become its **velocity** (directional). `count > 0` → that many particles scattered with Gaussian offset; `extra` = speed.
Source: https://docs.papermc.io/paper/dev/particles/

### 2.2 ParticleBuilder (cleaner, has built-in receiver targeting)
```java
Particle.DUST_COLOR_TRANSITION.builder()
    .location(loc).offset(0.5, 0, 0).count(3)
    .colorTransition(Color.fromRGB(40,0,0), Color.BLACK)  // bleed red→black
    .receivers(32, true)    // only players within 32 blocks (sphere)
    .spawn();
```
`.receivers(distance, sphere)` is the painless way to scope a beat to nearby players without manual loops.
Source: https://docs.papermc.io/paper/dev/particles/

### 2.3 Data requirements (will throw if data type is wrong)
| Particle | Required data | Use for The Observance |
|---|---|---|
| `DUST` | `new Particle.DustOptions(Color, float size)` | colored rune dust, blood motes |
| `DUST_COLOR_TRANSITION` | `new Particle.DustTransition(from, to, size)` | red→black "wound," soul-bleed |
| `BLOCK`/`BLOCK_CRUMBLE` | `BlockData` | crumbling/decay illusion |
| `ITEM` | `ItemStack` | shattered-object burst |
| `TRAIL` (1.21.4+) | `new Particle.Trail(targetLoc, Color, ticks)` | a mote that *travels toward the player* |
| `SHRIEK` | `int` (tick delay) | the warden shriek visual |
| `VIBRATION` | `Vibration(...)` | "something noticed you" ping |

Mood particles needing **no data**: `SCULK_SOUL` (drifting cyan soul wisps — the signature Deep Hold particle), `SOUL_FIRE_FLAME` (cold blue flame), `SMOKE`/`CAMPFIRE_SIGNAL_SMOKE` (low haze), `ASH`, `WHITE_SMOKE`, `SQUID_INK` (creeping dark).
Source: https://docs.papermc.io/paper/dev/particles/

### 2.4 Per-player atmosphere loop (the "it knows your name" primitive)
```java
ParticleBuilder b = Particle.SCULK_SOUL.builder().offset(0.3,0.5,0.3).count(2).extra(0.01);
Bukkit.getScheduler().runTaskTimer(plugin, () ->
    b.location(target.getLocation().add(0,1,0)).receivers(List.of(target)).spawn(),
    0L, 4L);  // every 4 ticks, ONLY the targeted player sees soul-wisps follow them
```
Because `player.spawnParticle`/`.receivers(List<Player>)` is per-client, you can make the haze stalk one specific friend while the others see a clear room — pure soft-pressure, no shared state.

### 2.5 Particle pitfalls
- Wrong `data` type for the particle = runtime exception — match the table.
- High `count` × tight timer = client FPS death and "spam" feel that kills dread. Keep counts low (1–4) and rely on placement/timing, not volume.
- `extra` (speed) on `count>0` randomizes velocity; for a controlled drift use `count=0` + offset-as-velocity.
- Note/spell color particles encode color in `offsetX` as a fraction (legacy) — for color use DUST, not NOTE.

**Apply to The Observance:** `Player.spawnParticle` + `.receivers(List.of(player))` is the per-player dread layer — SCULK_SOUL wisps that trail only the targeted friend; DUST_COLOR_TRANSITION red→black on a rune; TRAIL motes that crawl toward a single client. Drive these from `Private*` beats with low counts for unease, not spectacle.

---

## 3. Packet illusions — blocks & mobs only ONE client sees

### 3.1 Fake block change (no entity, inherently per-player)
```java
player.sendBlockChange(location, Material.SOUL_SAND.createBlockData());
// the world is UNCHANGED; only this client renders it.
```
"Fakes a block change packet for a user at a location and will not actually change the world." Reverting = send the real block back, or `player.sendBlockChange(loc, loc.getBlock().getBlockData())`.

**Pitfalls (documented Paper bugs):**
- `sendBlockChange` vs bulk `sendBlockChanges(Collection<BlockState>)` can produce **different client results** and garbled states (Paper issue #10010) — prefer the single-block form, or test the bulk form carefully.
- Tile entities (skulls, signs, heads) are **not** fully supported by `sendBlockChange` — only simple blocks reliably (RFC #10515). For a fake player-head block use an ItemDisplay instead.
- The fake block desyncs the instant the player interacts with it or the chunk reloads — treat it as a momentary illusion (a torch that becomes a redstone torch when glanced at), then revert on a timer or on PlayerMoveEvent leaving range.
Sources: https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Player.html ; https://github.com/PaperMC/Paper/issues/10010 ; https://github.com/PaperMC/Paper/discussions/10515

### 3.2 Fake (client-side) entity via PacketEvents
Pattern: pick a unique entity ID + UUID, send a spawn packet, send a metadata packet to style it, send only to chosen players. Despawn with a destroy packet.
```java
int eid = SpigotReflectionUtil.generateEntityId();         // unique, server-safe id
UUID uuid = UUID.randomUUID();
WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
    eid, uuid, EntityTypes.ZOMBIE, locationVector3d,
    pitch, yaw, headYaw, 0 /*data*/, Optional.empty() /*velocity*/);
PacketEvents.getAPI().getPlayerManager().sendPacket(target, spawn);

// style it: invisible + glowing + custom name via metadata
List<EntityData> meta = List.of(
    new EntityData(0, EntityDataTypes.BYTE, (byte)(0x20 | 0x40)), // invisible+glowing
    new EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(comp)),
    new EntityData(3, EntityDataTypes.BOOLEAN, true) /*name visible*/);
PacketEvents.getAPI().getPlayerManager().sendPacket(target,
    new WrapperPlayServerEntityMetadata(eid, meta));

// despawn:
PacketEvents.getAPI().getPlayerManager().sendPacket(target,
    new WrapperPlayServerDestroyEntities(eid));
```
The metadata **index/flag map** (base Entity, index 0 byte):
`0x01` on-fire, `0x02` sneaking (also hides nametag), `0x08` sprinting, `0x10` swimming, **`0x20` invisible**, **`0x40` glowing**, `0x80` elytra. Index 2 = custom name (optional component), 3 = name-visible, 4 = silent, 5 = no-gravity.
Sources: https://docs.packetevents.com/processing-and-sending/advanced-packetevents-example-combining-our-knowledge/ ; https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata

### 3.3 EntityLib (PacketEvents addon) — skip the boilerplate
`Tofaa2/EntityLib` wraps the above: `WrapperEntity` objects with `spawn()`, `remove()`, `teleport()`, `rotateHead()`, type-safe `EntityMeta` (e.g. set invisible/glowing via methods not raw bytes), **automatic entity-ID allocation**, and **per-player `addViewer()`/`removeViewer()`**. This is the recommended path for `NamedMobBeat`/`FakeBlockBeat` packet entities — far less error-prone than hand-rolled wrappers.
Source: https://github.com/Tofaa2/EntityLib

### 3.4 Packet-entity pitfalls
- **Entity-ID collisions:** never invent a raw int — use `SpigotReflectionUtil.generateEntityId()` / EntityLib's provider, or you'll clobber a real entity and corrupt that client's world view.
- **No server-side existence:** the client thinks the entity is real; the server doesn't track it, so movement/animation must be *driven by you* via teleport/metadata packets each tick.
- **Cleanup is on you:** if you never send `DestroyEntities` (or the player leaves render distance), the ghost lingers on the client until relog. Always pair spawn with a tracked despawn.
- **Movement range:** position updates beyond ~8 blocks need a teleport packet, not a relative-move packet (relative move overflows).

**Apply to The Observance:** Use EntityLib `WrapperEntity` + `addViewer(targetFriend)` for the Watcher mob that *only the stalked friend sees* — invisible+glowing metadata, no AI to fight the script, you drive its slow approach via teleport packets. `sendBlockChange` = the "your torch is now a redstone torch (only you saw it)" beat, reverted on glance-away.

---

## 4. Making a REAL vanilla mob read as the Watcher

When you want server-side physics/pathing but an uncanny *appearance*, spawn a real mob and strip its mob-ness:
```java
mob.setAI(false);            // frozen / you script its motion
mob.setSilent(true);         // no zombie groans — silence IS the horror
mob.setInvulnerable(true);   // can't be killed/knocked
mob.setPersistent(true);     // survives chunk unload while staged...
mob.setRemoveWhenFarAway(false); // ...and won't despawn at distance
mob.setCollidable(false);    // no bumping
mob.setGlowing(true);        // outline through walls
mob.getPersistentDataContainer().set(WATCHER_KEY, BOOL, true); // tag for the showrunner
// invisible body so only the glow/aura reads:
mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, INF, 0, false, false));
```
- `setRemoveWhenFarAway(false)` is the Bukkit equivalent of the NBT `PersistenceRequired`/`NoDespawn` flag — essential so a staged apparition doesn't vanish before its cue.
- **PDC tagging** (`PersistentDataContainer`) is how the between-session showrunner reliably re-finds and cleans up its own entities after a restart — never rely on UUID lists held in memory.

### 4.1 Per-player glow COLOR (the team gotcha)
Mob/player glow color comes from the **scoreboard team**, not a per-entity setter (only Displays have `setGlowColorOverride`). For a shared color:
```java
Team t = scoreboard.getTeam("red") != null ? scoreboard.getTeam("red")
        : scoreboard.registerNewTeam("red");
t.setColor(NamedTextColor.DARK_RED);
t.addEntry(mob.getUniqueId().toString()); // mobs use UUID string; players use name
```
**For DIFFERENT colors per viewer**, you must intercept the `ENTITY_METADATA` (and spawn) packet per-recipient and OR-in `0x40`, optionally pairing each viewer with a different team — done via PacketEvents/ProtocolLib listener. Regression Games' case study did exactly this for selective outlines.

### 4.2 Real-mob pitfalls
- **Tracking/despawn:** monster tracking range is 48 blocks (`spigot.yml`); a Watcher beyond that won't render for the player even if alive. Stage it inside range or use a packet entity for long-distance reveals.
- **Per-player glow desync (Regression Games findings):** metadata packets are *broadcast* — to vary per-player you must **clone the packet before modifying**, or the last write wins for everyone. And on respawn the recipient is briefly off the online-players list, so the spawn packet fires before they're "back" — delay evaluation by one tick (`runTask`) or keep your own viewer list.
- `setAI(false)` stops gravity too — the mob floats where placed; teleport it to script motion.
Sources: https://medium.com/ai-and-games-by-regression-games/minecraft-displaying-entity-outlines-on-specific-players-with-protocollib-a0ce598e7702 ; https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Entity.html ; https://gist.github.com/davidjpfeiffer/fd5ed83f465f42100368ee53d8fe67d3 ; https://docs.papermc.io/paper/reference/spigot-configuration/

**Apply to The Observance:** For `NamedMobBeat`, spawn a real mob → `setAI(false)+setSilent(true)+setInvulnerable+INVISIBILITY(no particles)+setGlowing` → it reads as a glowing silent outline you puppet via teleport, tagged in PDC so the showrunner cleans it up after a crash. Use a team for the dread-red glow; only go per-player-packet-glow if two friends must see different colors at once.

---

## 5. Cross-cutting pitfalls (apply to every beat)

1. **Cleanup discipline.** Displays, packet entities, staged mobs, and fake blocks all leak if not torn down. Standard: `setPersistent(false)` where possible, PDC-tag everything, and have the showrunner sweep tagged entities + revert fake blocks on session end and on plugin enable (post-crash).
2. **Tracking range mismatch.** Displays = 128, mobs = 48 — a reveal staged for a display will render farther than one staged on a mob. Pick the medium to match the intended distance.
3. **One-tick-late client state.** Respawn, join, and first-tick spawns all have a window where the player isn't fully registered — defer per-player reveals by one tick (`Bukkit.getScheduler().runTask`).
4. **Interpolation snap.** Set delay/duration, then change the property on a *later* tick, or it pops instead of fades.
5. **Broadcast-packet aliasing.** Per-player metadata edits must clone the packet first (PacketEvents/ProtocolLib) — otherwise all viewers get the last-written value.
6. **Path A integrity.** Displays + particles + packets need **no resource pack** — they are the safe fallback when the one auto-pushed pack isn't loaded. Keep a pack-free render path for every rune/apparition.

---

## Sources
- PaperMC Display entities guide — https://docs.papermc.io/paper/dev/display-entities/
- Display Javadoc (1.21.8) — https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Display.html
- TextDisplay Javadoc — https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/TextDisplay.html
- Display.Billboard / Display.Brightness — https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Display.Billboard.html , .../Display.Brightness.html
- Entity Javadoc (visibility/glow/AI/persistence) — https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Entity.html
- Player Javadoc (sendBlockChange/showEntity) — https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Player.html
- PaperMC Particles guide — https://docs.papermc.io/paper/dev/particles/
- Minecraft Wiki — Entity metadata flags — https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata
- Minecraft Wiki — Display interpolation semantics — https://minecraft.wiki/w/Display
- PacketEvents advanced example — https://docs.packetevents.com/processing-and-sending/advanced-packetevents-example-combining-our-knowledge/
- EntityLib (PacketEvents addon) — https://github.com/Tofaa2/EntityLib
- Regression Games — per-player ProtocolLib outlines — https://medium.com/ai-and-games-by-regression-games/minecraft-displaying-entity-outlines-on-specific-players-with-protocollib-a0ce598e7702
- Glow-color via scoreboard team gist — https://gist.github.com/davidjpfeiffer/fd5ed83f465f42100368ee53d8fe67d3
- Paper bug — sendBlockChange vs sendBlockChanges — https://github.com/PaperMC/Paper/issues/10010 ; tile-entity RFC — https://github.com/PaperMC/Paper/discussions/10515
- spigot.yml tracking ranges — https://docs.papermc.io/paper/reference/spigot-configuration/
- From The Fog (north-star reference) — https://lunareclipse.studio/creations/from-the-fog
