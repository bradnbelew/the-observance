# Rework plan: from "works" to "a real place" (for approval)

Date: 2026-11-04. Status: **DRAFT, awaiting the owner's approval.** Nothing below is built yet.
The brief is `FABLE-BRIEF.md`; the findings are `PLAYTEST-CHANGELIST.md`. This plan says exactly
what I will change, where, and how each change gets verified. Where I propose a design decision the
owner should make, it is marked **DECISION** and collected again at the end.

What I read before writing this: every v2 doc, the whole canon (`morrow/canon/*.py`), the world
builder and injector, the full plugin, the site's desk logic, the bot, and `view.py` renders of
every build and the four puzzle rooms (saved as `morrow/v2/rework-previews/before-*.png`; the four puzzle rooms are
`before-interiors.png`: Rook's cellar, patch's wall, the annex shelf, the reading room). The pending Morrow chat rewrite and the playtest
docs are committed as `edb6da9`.

Rules I am holding to throughout: the 2011 layer stays Beta 1.5-correct; every Morrow string passes
the corpus law; Theo stays prewritten; no puzzle is cut or labelled optional; every hard answer keeps
an MD5 self-check; canon generates everything; every existing check stays green.

---

## 0. The shape of the work

Order of building (each chunk is committed on `morrow-v2/storyline` and leaves every check green):

1. **Canon** (`ciphers.py`, `layout.py`, `logblock.py`, `later.py`, `irc.py`, `files.py`, `forum.py`,
   `records.py`, `build.py`): reading-order fixes, the chest token, new Theo lines and rules, new
   desk hand-ins, the IRC trim, new lore files, the EXIF image, the heartbeat log, the stop-log
   fallback, new sign text, new LogBlock rows.
2. **World** (`build2011.py`, `inject.py`, `shots.py`): the lived-in pass, build by build; the
   between-places life; the re-homed cipher rooms; the landmark treatment; starting kit; Kit's trail.
3. **Plugin** (`Ghosts`, `Replays`, `ModernLayer`, `RecoveryService`, `Nights`, `Finale`, `Campaign`,
   `Corpus`, `Bridge`): figures and skins, witnessing, chat bubbles, the dock removal, the rail cap,
   the floor tidy, new hand-in triggers, the finale scene, museum repair, bed feedback.
4. **Site** (`dashboard/`): June's-folder and console hand-ins over the bridge, the roster fallback,
   the visible heartbeat date, new files and pages, print-view/ID tells.
5. **Bot**: the finale posts, Theo's unprompted notes.
6. **Pipeline + playtest**: rebuild the test world through the full chain, reset the test server,
   play every act as a non-op resident, RCON checks, renders.
7. **Self-audit** (cold solve, break attempts, clue-by-clue check, editor pass), written to
   `morrow/v2/REWORK-AUDIT.md`. Regenerate `SOLUTIONS.md`; update the walkthrough, runbook,
   HUMAN-TODO, RECORDING-SCRIPTS.

---

## 1. A world that feels lived in

Layout stays as it is (owner's decision). Every coordinate below is from `layout.py`; "standing
level" means the first air block above the floor. All new 2011 objects go through `build2011.py` /
`inject.py` and get `build-manifest.json` attribution, so LogBlock noise can be regenerated from
who built what.

### 1.1 Starting kit (in fiction)

Rook's 2010 welcome post says "rules are on the signs at spawn". He also kept a newbie chest. At
spawn, beside the rules board: a double chest with a 2011 sign over it:

```
new ppl: take
1 of each pls
and a bed. -rook
```

Contents (2011 items only): stone pickaxe, stone axe, stone shovel, 16 torches, 8 bread, 1 bed,
16 oak logs, 8 planks, 1 crafting table. The plugin refills it at dawn as part of the restore
("[Recovery] 1 chest restored (spawn)"), which is in character (Morrow keeps spawn as it was) and
means a group of six always finds a kit. Nothing else gives items.

### 1.2 Build by build

Palette for all of it: cobble, mossy cobble, planks, logs, glass, wool, fences, slabs, stairs,
ladders, torches, chests, furnaces, workbenches, bookshelves, note blocks, jukeboxes, beds, signs,
paintings (Beta had them), sandstone, brick, cake, pumpkins, reeds. Nothing from after 18 May 2011
except where the object is dated later in the fiction (the Nether portal, 27 May) and only blocks
that exist in Beta 1.5's block table (portal block 90 and obsidian do).

**Spawn plaza** (-233, 70, 30). Today: a small cobble square, three signs, a cake table, four
torches. Plan:
- The rules board becomes the landmark: the three signs on a one-block cobble plinth with a fence
  rail in front and a torch post either side, and the spawn point set so a new player faces it.
  Rook's `screenshot / stuff. it / changes. trust / me -rook` sign goes on the plinth's end.
- A bulletin board (six wall signs on a plank backing, west side): `cake sat night / be there -june`,
  `WHO TOOK THE / SPARE CART`, `boosters are / cursed -rook`, `bread by the / storehouse door`,
  `spur: not done / dont ask -finch`, `mumble pw is / the song`. These are forum in-jokes made
  physical. Morrow edits one in Act 6 (see 1.4).
- The newbie chest (1.1), the "free bread" chest with 20 bread.
- The Christmas tree stump: a log column with torches, a sign `tree 2010 / rip`.
- The Nether portal frame (obsidian, lit) on the plaza's east edge with Rook's sign
  `nether hub / coords on the / sign over there / x8 remember` (dated 27 May 2011 in LogBlock).
- Torch posts along the gravel paths out of the square (2011 servers were torch-spammed).
- North of the square on the hillside: Rook's **pixel-art creeper face**, 8x8 wool, half done
  (top two-thirds laid, bottom missing), with a chest of green/black/lime wool at its foot and a
  sign `creeper. wip / wool ids on / the forum -rook`. It is visible from the square, teaches the
  wool-ID table gently, and is one of the things Morrow "completes" (see 1.4).

**Rook's house** (-262, 71, -2). Today: a good exterior, an interior of one workbench, one
furnace, one chest and a bed. Plan:
- Two rooms: a front room (workbench, two furnaces, a chest row, a bookshelf wall, a table of
  fence+pressure-plate, torches) and a back bedroom (bed, chest, a painting). By the door inside: a
  boot rack (a stairs block row with a sign `boots. here. / -rook`).
- **Rook's changes wall** (the "resident's notebook"): a sign board in the front room where Rook
  logged what the restore changed in 2011, one line per sign, all under 15 characters:
  `5/19 june: roof`, `5/19 rail +3`, `5/19 cairn: ???`, `6/3 floor done`, `9/2 barn corner`.
  This is the in-world before/after ledger players can check later edits against. Morrow rewrites
  one line of it in Act 6 (over 15 characters: the tell).
- The dirt pillar he never took down stays; a sign on it: `ill take it / down later`.
- **The cellar** (re-homed cipher, see 3.2): a hangout room, not a stone box. The 18x9 wool floor
  stays exactly where it is and keeps its values. Around it: the walls get bookshelves and
  torches, a jukebox corner on the landing, a "couch" (stairs and slabs) against the east wall, a
  chest row on the west wall, the ladder with a sign at its foot:
  `my floor. dont / touch it. i / mean it -rook`. Morrow's back-of-sign note: `floor: 3 rows /
  normalised`. The three tidied rows are the three rows nearest the ladder (row order flipped, see
  3.2), and the tidy is a sorted colour run that reads as a stripe against the noise.

**cairn's storehouse, shed, fields, pen** (-80, 72, -140). Today: a long barn (empty inside but
for the six chests and a workbench), a shed, fenced fields of wheat, the split pen. Plan:
- Barn interior: the six tally chests each get a 2011 name sign above them (`rook`, `cairn`,
  `finch`, `june`, `patch`, `kit`, in join order as cairn kept them), which the forum post already
  implies. A desk (fence+slab) with cairn's chair, the "hinge tin" and "returns" fixtures moved
  INSIDE against the north wall with a sign `close-up: lot, / tin, chalk, lamp`, flour sacks (white
  wool) in a corner, a bread oven (three furnaces) by the door with the free-bread chest, hanging
  pumpkins, torches in a row down the middle beam.
- The south-west corner of the barn is built crooked (a one-block jog in the wall, a 2011 kid's
  mistake). Morrow squared it on 2 Sept 2011: the modern layer replaces the jog with a straight
  wall, and the LogBlock rows for that night describe exactly those blocks. **DECISION A:** this
  replaces the undocumented "Morrow moved the barn six blocks east" (the barn is built at the 2011
  anchor and nothing moves it). cairn's lodestone compass then points to his real door, and D9's
  "6 blocks west" wording in the cipher bank goes.
- The shed: tools (chests), a rack of ladders, and the bed-shaped gap: two blocks of floor left as
  dirt with a sign `bed was here / 5/12. who / took it -cairn`.
- Fields: a pumpkin patch, reeds by the water, a scarecrow (fence + pumpkin), a second gate, and
  cairn's first house: the creeper crater from Nov 2010 patched with cobble, with the rebuilt cobble
  hut beside it (`creeper ate / the first one`).
- Pen: a sign on the gate `census. inner / = on this week / dont shear`.
- The sealed back room (sculk, Theo-era) stays as built.

**finch's hub and rails** (-215, 71, 22). Today: a neat stone hub, a booth, a line north and east,
the south branch, the spur. Plan:
- The hub becomes a station: a timetable sign board (`hub -> farms / about 40s`, `spur: no`,
  `spare carts: 2 / BRING THEM BACK`), a chest with two minecarts, a bench, torches on the pillars.
- The **junction levers** for C3 (3.6): three lever-switched junctions on the main line (the
  corner where the spur leaves it, and two decoys), each with a small sign `jct 1` etc.
- The south branch end: finch's genuine sign (exists), the cobble marking (exists), plus a bench
  and a torch so people stop there, and a sign on the line's first booster `booster line / RIP /
  powered rails / finally` (exists in canon, placed properly).
- **The rail through the house.** Found the cause: the south branch is laid 3 blocks per act from
  z 62 toward +z, so by Act 9 it reaches z 86, which is inside lowpath's place (walls at z 84 and
  88); `railCanGo` lets it climb Morrow's hill and onto the floor. Fix in `ActDirector`: the
  extension stops at z 82, one block short of the hill's foot, which is what Act 9's hook says
  ("one segment from lowpath's place"). The main line never crosses a build (checked against every
  footprint); I will re-check with the manifest after the pass.

**June's point** (-246, 68, 236) and tower (-243, 66, 249). Today: cottage, dock, lighthouse,
empty interiors. Plan:
- Cottage: bed, chests, a wall of paintings (her "screenshots"), a bookshelf, a glass-fronted
  "lamp cabinet" of torches, her flag locker (coloured wool in a chest), a sign `F2 F2 F2`.
- Tower: the gallery gets June's failed lamp attempts (redstone torches and a repeater loop on the
  gallery floor, finch's "torch clock"), a chest of glass, a sign `lamp: still / nothing -june`.
- **The old dock goes.** The story says Morrow replaced it with the lighthouse base. The modern
  layer removes the dock planks and pilings and lays a sandstone apron round the tower's foot that
  reaches the shore (Morrow's "completion" of her plan). The 2011 screenshots (shots 2 and 9) keep
  the dock, and the Dynmap 2011 tiles show it; that is how players find it in Act 7.
- The flags (Theo-era) stay on the signal yard.

**patchcord's hut** (-199, 72, -70). Today: a hut with a furnace, jukebox, chest and the note
wall. Plan (re-homed cipher, see 3.2): a studio. The note blocks stay the tune, re-laid so they read
left to right from the door; under the bass notes planks, under the drums stone (as now); a "desk"
of stone slabs with levers and redstone torches as the mixer; a mic stand (fence + stone pressure
plate); the jukebox with a disc; a chest of "tapes" (paper); headphones on a hook (a black wool
block on a fence); signs `mumble room / push to talk` and `play from / here -> -patch` at the first
block. The hut keeps its stove chimney and mismatched windows.

**Iona's annex** (-286, 35, 28). Today: a WorldEdit-square stone room with the junk shelf and
three torches. Plan (re-homed cipher, see 3.2): an ops room. A desk (crafting table + chest), a
server-rack wall (iron blocks and stone slabs with redstone torches as LEDs), a coffee corner
(cauldron is post-1.5; a furnace and a cake instead), a bookshelf, a bed (she slept there the week
of the 18th), and the eight ID blocks placed where each makes sense: the pressure plate at the
door, the ladder on the rack, the redstone torch on the rack, the jukebox by the bed, the stone
button by the desk, two clay blocks as the rack's base, the netherrack in the "stove" (a 2011 fire
block). They sit in one row along the south wall in reading order (see 3.2), so a player who
suspects a code can read them left to right, and nothing announces itself. The annex gets a
visible way in: a cobble manhole on the hill with a 2011 sign `CL ops / keep out / -ib`, because a
sysadmin marks her room; the stair tunnel stays.

**Rook's ladder room** (-240, 40, 64) and **Iona's notes cell**: furnished small (chests, torches,
a bed in the ladder room, Rook's "spare" chest contents: 2011 junk), signs re-laid in reading order.

**Kit's base** (334, 70, -530): the owner's dirt hut stays. Added by the injector (2011 blocks):
Kit's dirt towers every ~80 blocks along his walk (the xray thread: "you would have left a dirt
tower"), a wolf kennel of fences by the hut with three signs `biscuit 2`, `biscuit 3`, `biscuit 4`,
a chest with bones and cookies. This makes D3 fair: the fossil terrain is the hard route, the dirt
towers are the one a group that reads the forum takes.

**Between the builds:**
- The low path: a worn dirt track along the hill foot through (-213, 69, 82), exactly where
  Rook's map key says. Morrow's hill now sits on it (that is the point).
- Kit's abandoned mob grinder: a 2011 cobble tower with water channels, half built, east of the
  farms, sign `mob grinder / dont laugh / -kit`.
- The x-ray tunnel under spawn: a straight tunnel to a small cave with a sign `it was a cave /
  -patch` and a few exposed diamonds nobody mined.
- A memorial stone at the farm stop: `booster line / 2010-2011 / rip` (finch).
- Rail stops: the farm stop with a bench and a torch, the hub.
- Torch posts and gravel paths between every pair of places people walked (not to lowpath's).
- Chunk-edge seams and the "skirt" cliffs the builder leaves round plots get eased (the June
  render shows a stepped sandstone footing; `site()`'s skirt will be widened at the point).

### 1.3 Hide clues in plain sight

Each cipher object is a natural part of its room (details above). The rule I apply: a player who
walks in sees a room with a purpose; a player who suspects a code can read it without moving a
block; nothing stands in an empty stone box; nothing is behind a wall.

| Object | Was | Becomes |
| --- | --- | --- |
| Rook's floor | bare box, 3 torches | the cellar hangout (1.2) |
| Iona's ID wall | junk shelf in an empty room | the ops room's fixtures (1.2) |
| patch's notes | a wall of note blocks | the studio (1.2) |
| Residents' Key signs | a sign wall in a stone cell | Rook's ladder room: bed, chests, the wall, with his `key: all of us` sign framed by torches |
| Braille shelves | a shelf row | Theo's reading room gets a chair, a lamp, a lectern with Theo's visitor book |
| Iona's chest | buried, fine | unchanged (it is meant to be buried) |
| the dust corridor | a tiled passage off the annex | the annex's "cable run": the corridor gets the rack's redstone leading into it so it reads as part of the ops room |
| cairn's listening room | sculk box | unchanged inside; outside, the routine fixtures move into the barn (1.2) |

### 1.4 Make Morrow's edits noticeable

Things Morrow edits, and where players will have stopped and read them first:

| Edited (act) | Where players meet it first | How they compare |
| --- | --- | --- |
| rules sign line (3) | the plinth they spawn facing; June's shot 3; Rook's "screenshot stuff" sign beside it | their own screenshots, shot 3, the sign's back note |
| the pixel-art creeper (3) | visible from spawn; its WIP sign | Morrow completes the bottom third at the Act 3 dawn with "[Recovery] 21 blocks restored (spawn)"; shot 3's background and the Dynmap tile show it unfinished |
| landmark signs at three builds (6) | the name signs at Rook's, the hub, June's are on the paths in | Rook's changes wall, the Dynmap tiles, sign backs |
| one bulletin-board sign (6) | the board at spawn | the others on the board, the forum |
| one line of Rook's changes wall (6) | Rook's front room | it is the ledger itself; the rewritten line breaks the 15-character rule |
| the floor's 3 rows (5) | nearest the ladder, with Rook's "dont touch it" sign | LogBlock, the sign back |
| the south rail (every act) | finch's "leave it" sign and bench at the end | the cobble marking, the Dynmap tile |

The site adds the 2011 Dynmap tiles' render (`render.py`) as a real before, and the forum print
view keeps originals (exists).

### 1.5 Believability checks

For every build: `view.py --orbit` from four corners and a first-person shot from where a player
arrives (the path), checked for sunk/floating/clipped blocks; the manifest cross-checked against
every other footprint and the rails; RCON `execute if block` on every cipher block after the
pipeline; the light level at every cipher object read with `data get` / F3 logic.

---

## 2. Figures players actually see

### 2.1 Pace and smoothness

- Canon seconds become real seconds (20 ticks). The incident replay is rewritten in `build.py` as
  an ~8-minute script that fits one in-game night: figures walk at 4 blocks/s when moving and stand
  still between lines; their chat lines sit at real intervals.
- The player ghost replays the recorded path at the pace it was recorded (one sample per 5 s, so
  100 ticks per point), interpolated per tick.
- lowpath's night walk becomes a real route (~6 minutes) with stops at each habit, then it stands
  at its place until dawn.
- Per-tick positions are interpolated between waypoints (no jumps), yaw follows the path.

### 2.2 Guaranteed witnessing

- **Cue.** At dusk, when the replay is due and a resident is online: a chat line from the engine
  `[Recovery] debug view: replay 2011-05-18 04:00` (an Iona template; "debug", "view", "replay"
  join `ENGINE_VOCAB`), June's lamp holds steady for three seconds, then the figures start.
- **Start near people.** The replay starts only when a resident is within 64 blocks of any
  figure's first point; otherwise it waits (all night if needed) and starts when someone comes
  near, so the first thing seen is a figure beginning, not a figure mid-walk.
- **Repeat until witnessed.** Each figure's run records whether a player was within 24 blocks
  while it moved. The journal line `morrow.replay.incident` gets `witnessed: [names]`; if nobody
  witnessed it, it replays the next night too (it does anyway from Act 5; this makes the journal
  say so, and the operator can tell).
- lowpath's walk and the player ghost get the same cue and start rule.

### 2.3 Chat lines above the head

`Ghosts.say` stops using the passenger trick. The TextDisplay is spawned at the body's position
plus 2.6 and moved every tick with the figure (teleport duration 1, so it glides), removed after
6 s. Billboard CENTER, shadowed, line-wrapped at 40 characters.

### 2.4 Skins

`skins.py` is rewritten as a real painter: hair shapes, faces, clothes with the outer layer used
for depth (hoods, caps, aprons, jackets), boots, and a per-resident palette. One per resident,
readable at a distance, true to the forum:

| Who | Look |
| --- | --- |
| rookery | dark hoodie up, black jeans, the boots he makes everyone take off: heavy brown boots; tired eyes |
| finchline | "steve with a red shirt lol": a Steve-shaped skin in a red shirt, blue jeans, redstone-red hairband, grease on one sleeve |
| juni_lantern | "a lighthouse keeper one ... I have a little hat": yellow oilskin coat, navy cap, blonde hair, camera strap across the chest |
| patchcord | all black, headphones round the neck, a bass strap over the shoulder, stubble |
| cairn | baker's whites, flour on the apron, rolled sleeves, a flat cap, beard |
| kitbag | the default Steve with a green shirt (June's photo: "Kit is wearing a default skin") and a kid's size haircut |
| i_bell | grey cardigan, glasses, lanyard, dark hair tied back |
| lowpath | June's head on cairn's torso with Kit's arms and finch's legs (as canon), with visible seams: one-pixel mismatched rows at every join, the head a shade too pale, one eye a pixel off. Uncanny on purpose. |

Previews: `morrow/v2/rework-previews/skins.png` (front and back of each, drafted today with the
rewritten painter; the PNGs in `morrow/v2/skins/` are these drafts, not yet uploaded, so the
plugin still carries the old signed textures until you approve). Upload via MineSkin
(`curl -F file=@x.png -F visibility=1 https://api.mineskin.org/generate/upload`), signed textures into
`plugin-v2/src/main/resources/config.yml`. Note: I cannot see them in a real client; I will verify
MineSkin's own render of each upload and the texture URL, and HUMAN-TODO will say to glance at them.

### 2.5 lowpath's bed

- Route end moves to **(-214, 73, 86)** (the anchor stays the house centre; the house interior gets
  a bed-sized gap on the west wall at exactly that spot, so the natural placement is the right one).
- Act 8, on placing a bed within 3 of the spot: at once `[Recovery] lowpath's place: 1 block queued`
  (engine template), and at dawn it is removed with `1 block normalised (lowpath's place)`. That is
  the Act 8 lesson ("their placements get removed") made visible the moment it happens.
- Act 9+, on placing: at once `[Recovery] lowpath's place: bed kept`; that night the cue fires and
  lowpath's route runs its last leg only (spawn to its place, about 90 s) so players who come when
  the cue fires see it walk in and lie down (`Mannequin` pose SLEEPING if the API allows; else it
  stops on the bed and is gone after three seconds), then `session lowpath: end (slept)`.
- `/mrrw replay lowpath` stays for the operator.

---

## 3. Puzzles: fix the existing ones, add depth

### 3.1 Reading order (systemic)

Rule: an ordered sequence reads left to right from where the player stands to read it. Audit:

| Sequence | Player stands | Today | Fix |
| --- | --- | --- | --- |
| patch's note blocks (x+i, wall at z+2, door north) | inside, facing south | right to left | lay at x−i; `play from here` sign at the first block |
| Residents' Key signs (x+i, facing north) | facing south | right to left | lay at x−i; the two preface signs first |
| Iona's ID wall (x+i on the south wall) | facing south | right to left | lay at x−i |
| Braille shelves (facing south, x+i) | facing north | left to right | no change; confirm the rotation reads with a chart (3.3) |
| dust corridor (runs east from the annex) | walking east | in order | no change |
| Rook's floor rows (row 0 at the north wall, ladder south) | from the south landing, facing north | row 1 is the far row; columns left to right correct | row r goes to z = fz + 8 − r so row 1 is nearest the ladder; boustrophedon unchanged; LogBlock rows follow |
| June's flags (x−5+i on the yard, read from the gallery facing north) | facing north | left to right | no change |
| cairn's tally chests (join order down the barn) | walking in from the door | in order | name signs added |

### 3.2 Rook's floor and the Braille shelves, verified in the world

I will verify both from what a player sees, after the pipeline, not from the generator:
1. RCON `data get block` on all 162 wool blocks and the 9 shelves on the test server.
2. A script that reads those dumps exactly as a player would (floor: nearest row first, left to
   right then right to left; shelves: the 2x3 slot grid rotated a quarter turn per the chart) and
   decodes through to the MD5 on Iona's checklist and to `sorry kit`.
3. The LogBlock rows for the three tidied rows rebuild the original (the viewer's rows are checked
   against the world dump).
4. The 14 May screenshot's rules text equals the key; the wool-ID forum post equals the values used.
If the Braille rotation is ambiguous from the south side, the room's hanging sign gets a dot motif
that shows the rotation (it already carries `. :  :. .`; I will make it the letter "r" rotated the
same way, so it is a worked example).

### 3.3 The tidy made unmistakable

Rows 1-3 nearest the ladder, sorted into a colour run; Rook's `dont touch it` sign at the ladder
foot with Morrow's back note `floor: 3 rows / normalised`; the Act 5 dawn journals and announces
`[Recovery] 54 blocks normalised (rook's)` the first time a resident is online after Act 5 opens, so
the group hears it happen.

### 3.4 Act 5's advance (DECISION B)

Today: file "dont tell theo" or ask for `moss_0518` at the desk. Both wrong (the first contradicts
the message; the second skips the floor). Proposed: **act on the plaintext in the world.** The
decoded message says the stop needs "maint sign says on". The MAINT sign at spawn (-236, 71, 32)
reads MAINT OFF from Act 2, unwaxed. Players edit it to MAINT ON (1.20+ lets you right-click an
unwaxed sign to edit it). Within a minute Morrow answers in chat with an engine line,
`[Recovery] maint: on. sessions open: 1. restore continues`, and at the next dawn it reverts the sign
and that revert **is the advance to Act 6** (Correction starts because they tried to stop it). The
MD5 on Iona's checklist is the self-check for the plaintext; the request for `moss_0518.ogg` still
delivers the file (players need the tape for layers 3 and 4) but moves nothing; filing "dont tell
theo" still gets Rook's line from Theo but moves nothing. Hint 4 for Act 5 changes to point at the
checklist. "sessions open: 1" is planted for Act 8.

### 3.5 LogBlock leaks

- **Iona's chest rows go.** The 2011 export did not include LogBlock's chest-access table (true to
  the plugin: it was a separate table and option). No `chest_put` rows at all. The chest must be
  read in the world. The golem race becomes real and fair: the first time the chest is opened, the
  plugin journals it and at the next dawn Morrow "normalises" it (stacks merged by item), announced
  at spawn. One session to read it; players know the pattern from cairn's tally by then.
- **The annex gets an in-world lead** (the manhole and `CL ops` sign, 1.2), Iona's notes cell keeps
  its pointer, and the IRC line stays. The eight `i_bell` wall rows stay in LogBlock from Act 7:
  they are layer 1 by design (the numbers are the puzzle; decoding them still needs the pre-1.13
  table and ASCII). What changes is that the annex is no longer found only through them.
- **Stacks of 1.** The token's base64 contains two `A`s (index 0 = count 1 = no number shown). I
  will change the token text to a variant whose base64 has no `A` and no padding (e.g. a different
  separator or date form; the search is scripted) and add a `verify.py` check that no chest count is
  1. The checklist MD5 regenerates. **DECISION C:** the key string changes (nothing recorded uses it).

### 3.6 Puzzles the design promised that are not in the world yet (built now)

- **C3 finch's commute.** server.log 2011-06-11 21:03 ("taking the spur home. 40 sec from the
  hub") → LogBlock lever rows for finchline that session at the three junctions → set the levers
  as logged → ride from the hub → the cart takes the spur and reaches her station (the torch lock,
  D1, is its door). Teaches "replay logged actions" before D11 and lowpath's walk. The spur chest
  gets finch's own copy of the stop in her words (a 2011 sign in the chest's room:
  `rook says stop = / maint sign on, / nobody on, / ionas key -f`), the alternative route to the
  floor's meaning for a stuck group, as the storyline wanted.
- **The trapdoor tell (Act 2).** Morrow's reconstruction of the shed hatch uses an oak trapdoor,
  and LogBlock claims cairn placed it on 2011-05-12 06:50 (the morning the bed went missing).
  Trapdoors arrived 26 May. Three layers: notice, date it on the wiki, catch the forged row. Teaches
  S1 and S6 early and ties to the bed.
- **Post-ID tell (Act 9).** The players' backdated 2011 posts get post IDs above the last real
  2011 post (30112), because the importer appended them. A second, cheap, fair tell for S6.
- **Players' backdated LogBlock rows (Act 9).** Rows "from 2010-11" for each player placing blocks
  with post-2011 IDs (hay, carpet), as the storyline describes and the viewer never had.

### 3.7 New layered puzzles (proposed)

Each has an author, a reason, layers, a home, a payoff and a check. **DECISION D:** approve the set.

**N1. cairn's bread ledger (Act 3, cairn, medium).** Why: cairn counted everything. The free-bread
chest at the storehouse door and the one at spawn hold uneven stacks too, and his forum post
"i always end up with too much" has a number ("baked 112, took 40 home"). Layers: the two chests'
counts plus the crate tally for the week of 16 May reconcile only if one tally row is wrong, and the
wrong row is the one the golems have already "fixed" (so the forum arithmetic is the backup for a
tidied chest). Payoff: the true tally without the chest, in time for the lowpath-row proof. Check:
the week's totals equal the post's number.

**N2. June's torch clock (Act 3, June and finch, medium).** Why: June wanted the tower to flash like
home; finch tried a torch clock on the gallery. Layers: the relic circuit on the gallery is a
repeater loop; count the repeater delays (visible ticks) to get its period, which is 5 s; June's
thread says "like the one at home" and her profile says Halifax; the period plus Halifax gives
Sambro (Fl W 5s), as now, but from a 2011 object, so the lamp Morrow built in Act 3 is confirmation
rather than the only route. Payoff: `sambro`. Check: the lamp.

**N3. Theo's heartbeat log (Act 8, Iona's cron, lore-bearing, medium).** Why: `crontab.txt` logs a
heartbeat every five minutes to `/var/log/mrrw/heartbeat.log`. Theo's box gets a 300-line excerpt.
Layers: find the gaps; one gap is 2011-05-18 06:17 to 06:44 (the restart, in machine time, CDT
offsets in the ISO stamps); one is 2011-09-02 (the reboot); one is **2019-03-02 22:51 to 23:34**,
forty-three minutes after Kit's email arrived. Theo turned it off that night and turned it back on.
Payoff: T-08a gains a line about it (see 5.3); the case brief's "Kit" field accepts the gap's date
too. Check: the gaps line up with the TCAdmin log and the email header to the minute.

**N4. Rook's ledger (Acts 2-6, Rook, the comparison tool).** Not a cipher; the changes wall (1.2).
Its last line in 2011 is `9/4 im done`. Morrow's Act 6 rewrite of one line is itself the proof.

**N5. The spawn-point register (Act 9, Morrow, hard).** Why: Morrow files the players as residents
with first-join coordinates. The recovery compass (exists) points at each player's first-join
spot; the pots there read Friend (exists). New layer: the players' fake forum profiles carry a
"Location" field that is their first-join coordinates written as Iona's region notation
(`r.-1.0.mcr c(20,30)`), which players decode with C7's formula to confirm the absorption and learn
they are indexed like blocks. Check: the compass.

**N6. The MOTD queue (Acts 1-10, Morrow, medium, runs the whole game).** Exists (C14); made
legible: the status page's raw JSON shows `queue 412/413` from Act 5 and the number of jobs in the
queue equals the number of 2011 session jobs plus one: the one that never ends. At Act 10 the
console prints the queue before the stop, and the last job is `lowpath (open)`.

Dropped from the bank, honestly: **D4 (the second spawner past the ravine)**: Beta 1.5 had no
ravines, so a seed-world comparison cannot show one. **DECISION E:** drop it, or I reword it as
"the second dungeon under the big cave" and build the dungeon (Beta did have dungeons; the cave
is real on seed bracken). My recommendation: drop; the seed comparison already pays off through
June's cove and lowpath's hill.

### 3.8 The EXIF thumbnail (C19)

Keep the trick, make it legible, make it new. The uncropped original is redrawn at 800x500 with
large type; the thumbnail becomes 320x200 at quality 90 (EXIF viewers show any size; I will check
exiftool, exif.regex.info, metadata2go and the Explorer icon). The region Theo cropped off shows two
things: the "Start service" confirmation with the reason, as now, in type that survives at 320
wide, and **a Mumble window behind the panel** listing the channel `mossfield` with `rookery`,
`patchcord (idle)` and `tvale` in it at 06:44. New information: Theo was on Mumble with Rowan at the
moment he pressed Start, which is the lead to `MIC-5-18-B.WAV` (patchcord's desk mic caught it from
his headphones) two acts before D11 hands it over. DateTime stays 06:44:31.

### 3.9 Self-checks

Iona's checklist keeps its four hashes and gains none (she would not hash finch's sign). New
self-checks use the pattern already in the game: the sheep census, the bread ledger and the
heartbeat gaps check against each other; the trapdoor and post-ID tells check against public dates.
`SOLUTIONS.md` regenerates with every new answer.

---

## 4. More lore, told cleanly

- **The IRC log is trimmed.** 113 of its 216 lines are `--- Day changed` padding. The renderer
  prints a day-changed line only for days with text and `Log opened/closed` round each gap (her
  screen session dropped; irssi logs look exactly like that). About 100 lines, all content.
- **Residents after 2011** (Theo's drafts barrel, written books in Theo's own words, Theo-era so
  books are allowed): three unsent letters, each short. To June (he found her on a nursing-board
  photo in Halifax, 2016, never wrote); to Rowan (the email bounced in 2013); to Iona (he saw the
  ficus in a bank lobby in 2022 and did not go in). The one he did send is Kit's reply thread
  (exists). These are discoverable through the drafts barrel (C10's payoff) and nowhere else.
- **Copperline's own history**: the site's About page gets the real 2008-2014 timeline (the
  switch failure, the array, the closure notice) and the status page's incident archive gets two
  more small incidents, so 6118 sits among others.
- **Theo's slow unravelling** through the archivist notes (gated by act, as now): 2014-12-14 "box
  is up in the spare room", 2016 lowpath, 2019-03-02 (no text, just an edit timestamp at 23:40:
  he started a note and deleted it), 2023 waxing, 2026 the mirror.
- **The forum**: three more small threads (a skin contest, "kit's dirt towers", "who's on this
  week" that nobody answered, which is why cairn did it with sheep) and the "done" thread gets
  finch's reply "same".
- **In-world**: every sign listed in section 1; Theo's visitor book on the reading-room lectern
  ("if you were a resident, write your name" with his own entry and Kit's from 2016: "kit. its still
  here?"); cairn's `bed was here`; Kit's wolf graves.

Everything new is short. Nothing explains the plot; everything is something a person would leave.

---

## 5. Theo, and how answers are handed in

### 5.1 Hand-in formats per act (DECISION F)

| Act | Advances when | Format | Change |
| --- | --- | --- | --- |
| 1→2 | a resident joins | in-world | none |
| 2→3 | 45 min + a dawn | in-world | none |
| 3→4 | **June's folder is opened** with `sambro` (the site's basic-auth success posts `june_folder` over the bridge) | a found password | was: a desk ticket naming morrow. Theo's "That's not anyone" reply stays as a reply, not a gate |
| 4→5 | Theo unmasked | desk ticket | none (it is a confrontation; the desk is right) |
| 5→6 | **MAINT ON set and reverted** (3.4) | acting in the world | was: desk |
| 6→7 | the 06:44 restart filed | desk ticket | none (a confession) |
| 7→8 | **Iona's key entered in the TCAdmin console** (`/tcadmin/legacy`, Rookery's login): it prints `key accepted. stop refused: maint=off sessions=1 (lowpath)` | the panel | was: desk. Theo is never told the key (consistent with the floor) |
| 8→9 | Kit's line filed | desk ticket, 36-hour silence | none (the emotional centre) |
| 9→10 | the case brief, then **booking a window** | a form, a booking | none |
| 10 | sign, bed, console, vote, chat code, bridge cut | world + panel + Discord | the finale scene (6.2) |

So the desk gates three acts (4, 6, 8), the panel two (7, 9/10), the world three, a password one.
In between, Theo still answers filings about anything (the floor, the reading room, the watchdog),
so the desk stays the conversation, not the form.

### 5.2 Theo's arc

| Acts | Theo is | What that sounds like |
| --- | --- | --- |
| 1-3 | the helpful archivist | short, formal, "no worries at all", offers files |
| 4-5 | evasive | unmasked; chatty about Mossfield, steers off 2011: partials deflect warmly |
| 6 | cornered | the confession; then a late-night unprompted ticket (new, T-06c) |
| 7 | reaching out | the Metra DMs (exist) plus one note after a bad night (new, T-07d) |
| 8 | honest | the silence, T-08a, with the heartbeat-log line added (new text) |
| 9 | letting go | the brief accepted; a note the next morning (new, T-09c): "I went and stood in the spare room for a bit" |
| 10 | gone | T-10-cut, T-10-clean/recovered/corpus, and one more a week later (new, T-10-after) |

Replies react to how, not just whether: the unmask rule gets three variants keyed on which tokens
were filed (the lot number: "You found the auction paperwork.  I kept it, I don't know why."; the
heartbeat date: "Five days after.  Yes."; the invoices: "Keep it billable.  I did keep it
billable."), picked by the strongest token. Same for the restart rule (filed with "mid-session" vs
with "CHI-07/mrrw").

### 5.3 New Theo lines (all into RECORDING-SCRIPTS.md with IDs; text-only works)

- T-04c/d/e: the three unmask variants (above).
- T-06c (unprompted, 02:10 Chicago, the night after the confession): "Couldn't sleep.  Went and
  looked at it.  It's just a box with a light on.  I don't know what I expected.  Ignore this."
- T-07d (unprompted, after the DMs): "If one of those wasn't me, I'm sorry.  I can't see what it
  sends."
- T-08a (amended, one line added): "...I turned it off that night.  Forty minutes.  Then I put it
  back on.  That's in the log too, you'll have seen."
- T-09c (unprompted, the morning after the brief): "Stood in the spare room for a bit this
  morning.  It's loud, the box.  I'd stopped hearing it."
- T-10-ask (at booking): "One more thing.  Rowan's sign at the south branch.  Leave it, or let it
  finish?  Say it in Mossfield.  It listens to chat."  (This reliably produces the skip-line quote,
  section 6.)
- T-10-after (a week after the stop, Discord): "Kit came over.  We put the stick in his laptop.
  He said it looked smaller than he remembered.  Thanks for coming.  T"

### 5.4 Deflections that help

Every rule gets a partial that nudges toward what is missing without giving it: restart filed
without the time → "Which line.  There's a lot of lines in that log.  Read the reason field too.";
unmask with one token → "That's one thing.  I'd want two before I said anything."; the key filed
at the desk (new, since the console is the hand-in) → "That's not for me.  Iona built it so it
wasn't.  Put it where she'd put it."; a "none" ticket → three variants by what it mentions (a
place, a person, a file).

---

## 6. Morrow's voice and the finale

### 6.1 Parroting

`Corpus.reply` excludes the line just heard and the speaker's own lines from the last 10 minutes
from the candidate pool (players' lines stay in the corpus, so they are quoted back later, by other
voices). A SelfTest case covers it.

### 6.2 The finale as a scene (DECISION G)

Today the stop is six grey lines in chat. Planned sequence, all bounded and prewritten:

1. **The ask.** At booking, T-10-ask (5.3). Whatever a resident then types in Mossfield containing
   "leave it" / "finish it" is kept as the skip reason. Fallback if nobody says it: finch's own
   2011 sign, `reason="leave it" (finchline, 2011-05-19)`, never `none`.
2. **The window.** The bot posts Theo's "I'm on." in #general at the booked time. The MOTD reads
   `maint=on sessions=0` once the sign is waxed and lowpath has slept.
3. **The code.** A player types it at the annex. Morrow's reply in chat is its last sentence,
   deliberately: the stop code echoed (an engine line, `stop requested: C-xxxx-xxxx`) and then Kit's
   line, `you can turn it off its ok`, as the one fresh sentence it has that matches "stop". Kept on
   purpose, as the playtest found it landed.
4. **Finalising (about 90 s).** Instead of nothing: each resident's figure appears at the last
   place its replay stood and walks home; as each reaches its door the engine prints
   `session cairn: end`, `session juni_lantern: end`, and so on in join order; every door closes
   (the register all-off); the golems stop where they stand; the lamp flashes once more; the
   players' own figures walk to their first-join spots and stand.
5. **The log.** The six lines, two seconds apart, with the skip line carrying the resident's words,
   then `exit 0`, then every figure is gone and the lamp is dark.
6. **Silence.** No MOTD, no heartbeat, the bot's last post is T-10-cut's text, and the site's status
   page reads `mrrw-01: no heartbeat` with the first-heartbeat date beside it, so the fifteen years
   are visible on one line.
7. **After.** Theo's closing note per USB choice (exists), and a week later T-10-after.

### 6.3 The emotional weight

What makes it land is not spectacle but the residents going home and the two human lines: Kit's,
and Theo's a week later. The USB vote already makes the group own the ending; the ask about the
south rail gives them the last word Morrow quotes. I will keep every added line mundane.

---

## 7. Small fixes

- Theo's archive hall (museum, reading room, map wall) joins `RecoveryService.BUILDS` so the dawn
  repair covers it.
- The 2014-12-14 first-heartbeat date shows in the visible status table, not only in the JSON.
- The site falls back to the plugin's roster for player names (`GET /state` gains `names`;
  `players()` in `campaign.ts` uses them when `MORROW_PLAYERS` is empty), so Act 9 cannot break on a
  missed variable. `playtest.mjs` stops defaulting the variable to empty.
- `SIGN_REWRITES` run only at night once per act (not every night at 18000) so a group can keep a
  waxed sign and see the unwaxed one flip once, not endlessly.
- The ladder room's shaft cap and the annex manhole get a `data get` check in `selftest.py`.

---

## 8. Verification and the self-audit

Checks kept green at every commit: `python -m morrow.canon.verify`, `python -m
morrow.world.selftest`, `plugin-v2\gradlew.bat selftest build`, `python -m morrow.world.e2e_site`
(with the copperline-v2 preview), `npm --prefix discord-v2 run dry`. New checks: no chest count of 1;
every ordered sequence's world order equals its canon order (selftest reads them back from the
Beta world the way a player faces them); the south rail never enters a footprint; the replay script
fits a night; the finale sequence in `e2e_finale`.

Then: the pipeline rebuilds the test world, the test server is reset to Act 1, and I play every act
as a non-op resident (RCON for `/mrrw act N`, `/mrrw replay`, `/mrrw dawn`), standing where a
player stands, with `view.py` renders of every build and cipher object from the approach path,
before and after each act's changes.

The self-audit (`REWORK-AUDIT.md`): a cold solve act by act from what a player can see, noting every
place I needed knowledge a player lacks; break attempts (skip ahead, file early and wrong, dig,
LogBlock and page-source leaks, op vs resident, mid-act restart); a clue-by-clue check of every
coordinate, ID, sign, file, timestamp, MD5 and time zone; an editor's pass on every new line.
Anything I cannot verify myself is listed there plainly. Known already: how the skins look in a real
client, the Mannequin sleeping pose, and anything that needs the owner's recordings.

---

## 9. Decisions I need from you

| | Decision | My recommendation |
| --- | --- | --- |
| A | Storehouse: Morrow squared a crooked corner (LogBlock matches), not a six-block move | yes |
| B | Act 5→6 advances when MAINT ON is set and Morrow reverts it; `moss_0518` and "dont tell theo" no longer advance | yes |
| C | Iona's key string changes so no chest stack is 1 (checklist MD5 regenerates) | yes |
| D | New puzzles N1-N6 and the built-now set (C3, trapdoor tell, post-ID tell, player rows) | all |
| E | D4 (ravine spawner): drop, since Beta 1.5 had no ravines | drop |
| F | Hand-in changes: June's folder (3→4) and the console key (7→8) | yes |
| G | The finale sequence in 6.2 and the new Theo lines in 5.3 | yes |
| H | Skin designs in 2.4 (previews follow before upload) | approve the brief, see previews |

Anything not listed is a judgment call I will make inside the rules above and report in the audit.
