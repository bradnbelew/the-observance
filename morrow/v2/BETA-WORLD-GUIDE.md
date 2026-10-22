# 2011 Mossfield in Beta 1.5 (your part: one walk)

The 2011 layer of Mossfield is real Beta 1.5_01 terrain on seed `bracken` (your world, generated
2026-10-12). Claude built the residents' places into it in 2011 blocks only
(`python -m morrow.world.build2011`), and the injector places every cipher object on top. You
only need to do the one thing code can't: walk Kit's route, because only the real game can
generate the "fossil" Beta chunks puzzle D3 depends on.

## Where everything is

Every plot sits on the real terrain: its floor is level with the ground around it (never dug in), trees that
would overhang it are felled and their leaves decay, and the ground eases back to natural over a few blocks.
`python -m morrow.world.view <world> --orbit X Y Z` renders any place from four corners to check.

| Place | Around (x, z) | Built |
| --- | --- | --- |
| Spawn plaza, rules signs, cake table | -233, 30 | cobble/gravel square, fence-post torches, Kit's cake table beside the rules signs, gravel paths to Rook's and the hub |
| Rookery's house + cellar | -262, 0 | mossy cobble cottage, log corners, plank gable roof missing the corner stair over the dirt pillar he never took down; ladder to the floor room |
| Rookery's ladder room | -240, 64 (y 40) | under the sand south of spawn, shaft capped with one dirt block |
| finch's rail hub | -215, 22 | the only neat thing in town; line runs north, east, north, east to the farms |
| finch's south branch | ends -215, 62 | stops dead in the grass, cobble marking, her "leave it" sign |
| finch's hidden spur station | -224, -116 (y 70) | tunnel into the hill north of town from the spur's end |
| patchcord's relay hut | -199, -70 | cramped mossy cobble hut by the line, plank roof, stove chimney, note-block wall across the room, jukebox |
| cairn's storehouse, shed, fields, sheep pen | -80, -140 | plank barn on a cobble footing with a log frame and a steep gable roof in a cleared homestead; six tally chests, tool shed, fields, split pen terraced into the hill |
| June's house, dock, lighthouse | -246, 236 / tower -243, 249 | on the sandy spit to the south: sandstone-and-plank cottage, dock, red-and-white lighthouse with a signal yard and an empty lantern frame; her viewing hill is 33 blocks north-east of the tower |
| Iona's annex | -286, 28 (y 35) | under the hill west of spawn; stair tunnel capped under the grass |
| Iona's notes cell | -180, -20 (y 40) | |
| Kit's base | 330, -530 | **your walk** |

## Kit's walk (about 5 minutes)

1. Launcher: the `old_beta b1.5_01` installation. Singleplayer -> **Mossfield (Kit walk)**.
2. Options -> Video Settings -> Render Distance: **Short**. (A narrow strip of old terrain reads as a
   kid's footpath across the modern world later.)
3. You start at cairn's storehouse door, facing the right way. Press **F3** to see x and z.
4. **Walk** toward **x 330, z -530**: x goes up and z goes down at about the same rate. Don't use
   boats; swim if you hit water. About 560 blocks.
5. At x 330, z -530 (within a few blocks is fine), build Kit's hideout: a tiny dirt hut (you have
   128 dirt) with the **wooden door** in your inventory, and a torch or two inside. Make it
   look like a 14-year-old made it.
6. Save and quit. Tell Claude. The save gets copied back to `world\beta\Mossfield`, Kit's signs go
   in next to your door, and the pipeline upgrades the whole thing.

## Rules the builds follow (for any touch-ups you want to make)

Only blocks that existed by 18 May 2011 (Beta 1.5): stone, cobble, mossy cobble, dirt, grass,
sand, sandstone, gravel, clay, brick, oak planks only (spruce/birch *logs* are fine), glass, wool,
plain bookshelves, lapis/iron/gold/diamond blocks, stone/sandstone/wood/cobble slabs, cobble and
wood stairs, fences, ladders, wooden and iron doors, signs, torches, redstone torches, rails,
powered and detector rails, minecarts, chests, furnaces, crafting tables, dispensers, note blocks,
jukeboxes, beds, farmland, wheat, sugar cane, cactus, pumpkins, jack-o-lanterns, cake, redstone
dust, repeaters, levers, buttons, pressure plates, netherrack, soul sand, glowstone, obsidian, TNT.

**Never:** trapdoors (Beta 1.6, 26 May 2011), pistons, glass panes, stone bricks, fence gates,
iron bars, spruce/birch/jungle planks, item frames, anvils, lanterns, barrels, anything copper,
written books / book and quill. Those are the anachronisms Morrow gets caught with, so they only
appear in the modern layer.

Don't build lowpath's place. That's Morrow's, added on Paper.
