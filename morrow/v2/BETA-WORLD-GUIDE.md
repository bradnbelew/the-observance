# Building Mossfield in Beta 1.5 (your part)

The 2011 layer of Mossfield must be built in real Beta 1.5, on seed `bracken`. That's what makes
the terrain, the chunk "fossils", the palette tells and the seed puzzle real. You build the
houses by hand, because 2011 player builds are lumpy and personal. My tools then place every
cipher object to the exact block (the wool floor, Iona's chest, the block-ID wall, the signs,
the note blocks), so you never have to place 162 wool blocks by hand.

## Step 1. Make the world (about 2 minutes)

1. Open the Minecraft Launcher, then **Settings**, and turn on **Show historical versions of
   Minecraft: Java Edition**.
2. **Installations**, then **New installation**. Name it `Mossfield Beta`, version `old_beta b1.5_01`.
   Play it.
3. **Singleplayer**, then **Create New World**. Name: `Mossfield`. Seed: `bracken` (lowercase, exactly).
4. Walk around spawn for a minute, then quit to title.
5. Copy the world folder from `%APPDATA%\.minecraft\saves\Mossfield` to
   `D:\the-observance\world\beta\Mossfield`.

Tell me when that's done. I'll render a top-down map of the real terrain and choose where each
house goes: June on the coast, cairn on flat farmland, Rookery's cellar on solid ground. Then
I'll send you a map with numbered plots.

## Step 2. Build the houses (your creative part)

Build each resident's place on its plot. Keep it 2011: small, practical, a bit ugly, unfinished
in places. Real 2011 players built cobblestone boxes with wooden roofs, dirt scaffolding they
forgot to remove, torches everywhere, and glass in whatever shape fit.

**Only use these blocks** (they existed by 18 May 2011, Beta 1.5):

| Use freely | Notes |
| --- | --- |
| stone, cobblestone, mossy cobblestone, dirt, grass, sand, sandstone, gravel, clay, brick | |
| wood planks (oak only, the only kind then), logs (oak, spruce, birch) | spruce/birch *planks* did not exist |
| glass, wool (all 16 colours), bookshelf (plain), lapis block, iron/gold/diamond block | |
| slabs (stone, sandstone, wood, cobble), cobble and wood stairs | |
| fences, ladders, wooden and iron doors, signs, torches, redstone torches | |
| rails, powered rails, detector rails, minecarts | powered rails were brand new |
| chests, furnaces, crafting tables, dispensers, note blocks, jukeboxes, beds | beds only since Feb 2011 |
| farmland, wheat, sugar cane, cactus, pumpkins, jack-o-lanterns, cake | |
| redstone dust, repeaters, levers, buttons, pressure plates | no pistons yet |
| netherrack, soul sand, glowstone, obsidian, TNT | |

**Never use:** trapdoors, pistons, glass panes, stone bricks, fence gates, iron bars,
spruce/birch/jungle planks, item frames, anvils, lanterns, barrels, anything copper, or
books. Those are the anachronism tells Morrow gets caught with, so they must only appear in
the modern layer I add later.

## Step 3. Things that must exist (the story depends on them)

You'll get exact plot coordinates in Step 1's map. For now, this is the list.

- **Spawn:** a small plaza. I place the three rules signs and Rookery's warning sign (`screenshot / stuff. it / changes. trust / me -rook`).
- **Rookery's house:** a plain cobble house with a ladder down to a **cellar**. Leave the cellar
  floor as an empty 18 by 9 area at the marked height, and I fill the wool. Put a sign on the
  door: `ROOK'S / boots off / yes, yours too`.
- **cairn's storehouse:** a long wooden shed with **six rows of chests**, one row per resident.
  Leave the chests empty, because I fill the uneven tally. Also build a separate **small shed**
  nearby with no bed in it. That's "the bed is gone from the shed".
- **cairn's farms and sheep pen:** wheat fields, plus a fenced pen split into an inner and an outer
  part.
- **June's point:** a little house on the coast, a **wooden dock** east of it, and a **lighthouse
  base** on the point. Build the tower to the gallery with an **open top**, with no roof and no
  lamp. Morrow adds those.
- **patchcord's relay hut:** a small hut by the rail with a **wall of note blocks** (I tune them),
  a jukebox and a chest.
- **finchline's rail:** a powered-rail line from spawn to the farms, with a **hub** and a
  **spur** that heads off and ends unfinished in the grass. The south branch should stop
  abruptly. Plus a separate hidden **spur station** reached through a tunnel, with an
  8-lever door that I wire.
- **The cake table** near spawn, where Kit stood.
- **Kit's secret base:** walk about 800 blocks out alone, along the route I'll mark, and dig a
  tiny dirt hut. Walking there is what generates the "fossil" chunks, so you have to actually
  walk it in Beta. Don't fly or teleport. I add the `KIT ONLY` signs.
- **Iona's things:** nothing for you to build. I place her buried chest, her notes chest and
  her block strip.
- **Do NOT build lowpath's place.** That's Morrow's, added later in modern blocks.

## Step 4. Hand the world back

Quit the game and copy the world folder again to `D:\the-observance\world\beta\Mossfield`. My
injector places every cipher object, writes every sign, and fills every chest from the canon. It
also regenerates the LogBlock rows from the actual blocks, so the block log always matches the
world. Then I run the upgrade chain (Beta, 1.2.5, 1.12.2, 1.21.11) and you'll see a 15-year-old
world on Paper.
