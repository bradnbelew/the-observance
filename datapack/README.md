# The Observance — datapack (`observance/`)

Server-side world data for MC **1.21.11** (pack format `[94,1]`, `min_format`/`max_format`). Drop the
`observance` folder into `<world>/datapacks/` and `/reload` (or restart).

Carries the **Undercroft** custom dimension (the fog world), three Undercroft biomes, and the three
plugin-granted reward advancements (`the_ring_is_whole`, `the_count_is_yours`, `the_record_receives_you` —
`trigger: minecraft:impossible`, granted by `AdvancementToastBeat`; icons use the 1.21.x `display.icon.id`
form).

## Undercroft dimension — status: BUILT, currently DEFERRED (do not teleport players in yet)

`data/observance/dimension/undercroft.json` is a **`minecraft:noise`** generator backed by
`worldgen/noise_settings/undercroft.json`. It is **solid, walkable geometry**, not a void:

- `default_block` = `minecraft:deepslate`; the density functions carve caverns
  (`cave_layer` / `cave_cheese` / `cave_entrance`) into a deepslate mass — there is ground to stand on.
- Dark and sealed: `dimension_type/undercroft.json` sets `has_skylight:false`, `has_ceiling:true`,
  `ambient_light:0.0`, `fixed_time:18000`, Nether fog effects.
- Hostile-safe: `disable_mob_generation:true` + `monster_spawn_block_light_limit:0`.

**Deferred, not broken.** Per `design/RUNBOOK.md`, every keeper site is currently placed in the
**overworld**; nothing teleports players into `observance:undercroft` yet (no `.mcfunction`, no
teleport in this datapack — verified). Do **not** send players in until a gather-room / site is
authored inside it, because although the terrain is solid, the interior is presently unfurnished
(no keeper stones, no lighting, no landing platform at a known coordinate). When that lands, drop a
player at a **carved, lit landing coordinate**, not an arbitrary XYZ.

Reach it (admin only, for building) with:
`/execute in observance:undercroft run tp @s <x> <y> <z>` — note the world spans `min_y -64`,
`height 384`. Multiverse does NOT create datapack dimensions; use the datapack dimension directly.
