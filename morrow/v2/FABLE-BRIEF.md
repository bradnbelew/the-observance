# Brief for Fable: make The Observance feel like a real ARG

## Who you're working for and what happened

The owner has built **The Observance**, an alternate reality game for a small group of friends
(about 6). It runs across a fake dead hosting company's website (Copperline Hosting), a Minecraft
server (the 2011 town of Mossfield, still running in 2026), an Archive Desk where "Theo" answers
tickets, and Discord. Ten acts, about 35 puzzles. The players work out that a 2011 world-restore
bot called **Morrow** is still running on a box in Theo's spare room, learn why, and shut it down.

Everything is built and works end to end. On 2026-11-01 to 11-03 the owner played the whole thing
solo, Act 1 to the shutdown, with Claude walking them through it. The verdict: **the story and
cipher ideas are good, but it feels "just a tad cheap".** The world is sparse, the clues sit in
obvious puzzle rooms, the ghost figures flash past unseen, the skins are placeholders, and there
isn't enough to dig into. Your job is to take it from "works" to "immersive, layered, and worth a
group's 40+ hours".

**This is high-stakes and the owner is relying on you completely.** They won't debug your work.
When you say it's done, they will hand it to their friends. That means every clue works as
written, every block is where the clue says it is, nothing overlaps, floats, clips or hides
something players need, and every line of text and dialogue sounds like a real person. If you
can't verify something yourself, say so plainly; never report something as working that you
haven't checked.

Every playtest finding, with root causes and file pointers, is in
**`morrow/v2/PLAYTEST-CHANGELIST.md`**. Read it in full; this brief sets the priorities and the bar,
and that file has the specifics.

## Read these first

| File | Why |
| --- | --- |
| `morrow/v2/README.md` | Authority, status table, design deviations |
| `morrow/v2/STORYLINE.md` | The full story, every act, the ciphers (section C2) |
| `morrow/v2/THEO-AND-CIPHER-BANK.md` | The approved cipher bank and Theo's Archive Desk design |
| `morrow/v2/PLAYTEST-CHANGELIST.md` | Everything the owner found, with root causes |
| `morrow/v2/PLAYTEST-WALKTHROUGH.md` | The intended play path, step by step |
| `morrow/v2/SOLUTIONS.md` | Answer key (generated; regenerate after your changes) |
| `morrow/v2/OPERATOR-RUNBOOK.md`, `HUMAN-TODO.md` | How acts advance; what only the owner can do |

## How the project is put together

- **Repo:** `D:\the-observance`, branch **`morrow-v2/storyline`** (origin is 3eb77aa). `main` and the
  live site copperlinehosting.com are the old broken version; never touch `main`.
- **Uncommitted right now:** the Morrow chat rewrite (`Corpus.java`, `ChatListener.java`,
  `SelfTest.java`) plus `PLAYTEST-CHANGELIST.md`, `PLAYTEST-WALKTHROUGH.md` and this brief.
  Commit those first, as their own commit, before your changes.
- **Canon is the source of truth.** `morrow/canon/` (Python) generates everything:
  `python -m morrow.canon.build`, then `python -m morrow.canon.verify`. Ciphers in `ciphers.py`,
  coordinates in `layout.py`, IRC in `irc.py`, later-act text in `later.py`. Canon wins over docs.
- **World:** `morrow/world/` builds the 2011 town in a real Beta 1.5 world (seed `bracken`),
  then upgrades it Beta → 1.2.5 → 1.12.2 → Paper 1.21.11. `build2011.py` (2011 builds),
  `inject.py` (cipher objects), `pipeline.py`, `relight.py`, `view.py` (a first-person renderer so
  you can *see* builds), `shots.py` (June's screenshot saves), `selftest.py`.
- **Plugin:** `plugin-v2/` ("mrrw", Paper 1.21.11, Java 21). `ModernLayer.java` (Theo/Morrow's
  post-2011 additions), `Replays.java` + `Ghosts.java` (figures), `RecoveryService.java` (dawn
  repair/tidy), `Corpus.java` + `ChatListener.java` (Morrow's voice), `Fixtures.java`, `Finale.java`,
  `Bridge.java` (signed HTTP bridge the site and bot use). Build and test from PowerShell:
  `plugin-v2\gradlew.bat selftest build` (bash `cmd //c` doesn't work here).
- **Site:** `dashboard/` (Next.js 16). Canon bundles in `dashboard/src/lib/canon/act-NN.json`,
  private files in `dashboard/private-media/`. `python -m morrow.world.e2e_site` checks every act.
- **Discord bot:** `discord-v2/` (Railway). `npm --prefix discord-v2 run dry`.
- **Local test rig:** test server `build/mrrw-v2-test` (port 25599, RCON 25598 password
  `mrrwtest`, bridge 25588). It's currently in the *post-shutdown* state from the owner's playtest;
  rebuild or reset it before testing. Site previews in `D:\.claude\launch.json`: **copperline-v2**
  (port 3040, act switcher, Theo instant) and **copperline-playtest** (port 3041, real campaign,
  Theo in 3 minutes, `MORROW_PLAYERS=SirNan`). Owner's Minecraft name: **SirNan**.

## Rules that don't change

- **The 2011 layer is period-correct.** Anything presented as 2011 uses only blocks and items that
  existed in Beta 1.5, built the way teenagers built in 2011. Post-2011 blocks appear only as
  Morrow's or Theo's later additions, and that anachronism is itself a clue (sign backs, copper
  golems, chiseled bookshelves). Keep that line sharp.
- **The corpus law.** Morrow can only say words somebody already said. Every reply must pass
  `obeysLaw`.
- **Theo is never live.** He answers through the Archive Desk (prewritten replies matched to
  filings, on a delay) and voice notes the owner records. Any new Theo line goes into
  `RECORDING-SCRIPTS.md` with an ID, and the page must still work with text only.
- **Keep every existing puzzle and the full difficulty.** Improve, re-home and deepen them; don't
  cut them or label anything "optional / deep archive".
- **Hard but fair.** Every puzzle is solvable from what players can see, with outside knowledge a
  determined group can look up. Iona's MD5 checklist lets players self-verify answers; extend that
  pattern to new puzzles.
- Players use **Java 1.21.11, no ViaVersion** (don't suggest it). The repo is **public with the
  answer key**, by the owner's choice; leave it.
- Regenerate `SOLUTIONS.md` and update the walkthrough, runbook and HUMAN-TODO for whatever you
  change. Keep every existing check green and add checks for new logic.

## What the owner wants, in priority order

### 1. A world that feels lived in (the biggest gap)

The town feels sparse. **Don't move builds closer together**; the owner explicitly decided to keep
the layout. Instead:

- **Decorate and deepen every build.** Real interiors, furniture, storage, lighting, personal
  touches that match each resident's forum personality (Rook's "boots off", cairn's "leave it
  uneven" storehouse, June's lighthouse obsession, patch's music, finch's rails, Kit's dog
  Biscuit). Lived-in wear: half-finished projects, chests with real contents, signs with in-jokes,
  paths worn between places people actually visited.
- **Fill the spaces between builds** with the life of a 2011 server: farms, a mob grinder someone
  abandoned, a pixel-art attempt, a nether portal hub, a griefed-and-repaired spot, a memorial,
  rail stops, a bulletin board of signs. Exploring should reward curiosity on its own.
- **Hide clues in plain sight.** Right now each cipher object sits in an empty stone room, which
  announces "puzzle here" (Rook's wool-floor cellar, Iona's annex wall, Rook's ladder-room signs,
  patch's note blocks). Rebuild each as a natural part of a believable room, where a player who
  suspects something can find it but nothing screams.
- **Make Morrow's edits noticeable.** The "it's rewriting history" arc only works if players
  noticed things the first time. Put the things Morrow later edits (rules signs, landmark signs,
  paths, rails, Rook's floor) where players naturally stop and read, make them feel important,
  and give players ways to compare before and after (June's screenshots, a resident's notebook,
  the museum map wall).
- Keep the builds believable as kids' 2011 Beta builds, not modern showcase builds. Use `view.py`
  renders and in-game screenshots to judge; fix anything sunk, floating or clipped. **The rail
  line currently runs through a house; June's old dock is still standing although the story says
  Morrow replaced it.**
- **Players start with nothing** in a big world; give residents an in-fiction starting kit.

### 2. Figures players actually see

The incident replay, the player's own ghost and lowpath's night walk are the game's most
cinematic moments, and right now they fail:

- They're **too fast** (the replay lasts about 6 seconds; STORYLINE says about 20 minutes) and the
  player ghost runs at hyper speed. Make them walk at believable pace with smooth movement.
- They **pop in and out** and **play whether or not anyone is there**. Design a way to guarantee
  they're witnessed: start near players, trigger from where residents are, give a cue first (a
  bell, lights flickering, a chat line), replay until seen, and journal whether anyone was close.
- **Chat lines overlap the heads** and get left behind; make them sit above the figure and follow
  it.
- **Skins:** replace the generated placeholders with **much more detailed, clean, distinctive
  skins**, one per resident, readable at a distance and true to each personality; lowpath's
  stitched skin should be deliberately uncanny. Upload them to MineSkin (the API works without a
  key: `curl -F file=@x.png -F visibility=1 https://api.mineskin.org/generate/upload`) and put
  the signed textures in `plugin-v2/src/main/resources/config.yml`.
- **lowpath's bed** gives no feedback until it finishes its night walk. Give an immediate reaction
  and a payoff players witness (lowpath walking home and lying down). Move the bed spot to
  **`-214 73 86`**.

### 3. Deeper puzzles, more of them

The owner loves the layered style of Rook's wool floor (LogBlock reconstruction → wool IDs to hex
→ boustrophedon → running-key Vigenère keyed on the *original* rules sign). **Make the whole ARG
feel more filled out with more puzzles of that depth**, several layers each, each made by a
specific character for a specific reason with tools that existed at the time.

- Propose a set of new multi-layer puzzles that thread through the acts, so no act is just "file
  two words at the desk". Mix media: in-world builds, website files and metadata, audio, time
  zones, the forum's history, Morrow's own channels (MOTD, light, sound, redstone). Each should
  teach something that pays off later.
- Fix the existing ones (all in the changelist):
  - **Reading order is right to left** across the board (sequences laid along +x but read from
    the north): note blocks, sign walls, likely the Braille shelves and block-ID wall. Audit every
    ordered sequence.
  - **Verify Rook's floor and the Braille shelves in the real world**, from what a player can
    actually see, through to the MD5-matching answer.
  - **Morrow's tidy of the floor is invisible**; make it unmistakable.
  - **Act 5 ends by telling Theo when the message says "DON'T TELL THEO"**, with no prompt to do
    so, and asking for `moss_0518.ogg` skips the floor. Redesign the advance to fit the fiction.
  - **LogBlock gives away Act 7**: searching `i_bell` shows Iona's chest coordinates and every
    stack count, and the annex wall's block IDs. It's also the only route to the annex. Fix both.
  - **Iona's chest**: stacks of 1 show no number in Minecraft.
  - **The EXIF thumbnail in `panel_ok.jpg`** is unreadable at 160×100 and only repeats what
    TCAdmin shows. Keep the trick, make it legible, and make it reveal something new.
- Keep the self-verify pattern (MD5s on Iona's checklist or similar) for every hard answer.

### 4. More lore, told cleanly

The backstory is strong: Iona's restore tool, Theo naming it after his support macro, the 06:44
restart, Kit asleep in the spare room, the watchdog, Kit's 2019 email. Give players **more to
learn and more ways to learn it**, inside the world and the site: residents' personal stuff
(notebooks, letters, build journals, forum threads that pay off in game), Copperline's own
history, what happened to each resident after 2011, and Theo's slow unravelling. Lore should be
discoverable and rewarding to read, never a wall of text to wade through: the IRC log is about 216
lines with lots of padding, so trim and pace things like that.

### 5. Theo, and how players hand in answers

Almost every act advances the same way: type the right words into an Archive Desk ticket, wait,
get Theo's reply. By Act 5 that's repetitive, and it makes solving feel like form-filling.

- **Vary how answers go in.** Use the formats the fiction already offers and invent more: acting
  in the world (placing, waxing, wiring, standing somewhere at a time, bringing an item to a place),
  logging into a folder or panel with a found password, uploading or naming a file, the TCAdmin
  console, Discord (group votes, DMs players answer), the server-list MOTD, replying to an email,
  booking a window. Each act should use at least one hand-in that isn't "type keywords at the desk".
- **Make Theo a better character.** He's the emotional centre: a quiet, guilty man who kept his
  brother's childhood server running for 15 years. Give him a clearer arc across the acts
  (helpful archivist → evasive → cornered → honest → letting go), replies that react to *what* the
  players found and how (not just whether), small human details, and moments where he reaches out
  first (unprompted notes, a late-night ticket, a voice note sent at a bad hour). He must stay
  prewritten and stateless (no live operator), and every new line goes into
  `RECORDING-SCRIPTS.md`.
- **Deflections and partial answers should help.** A near miss should get a reply that nudges
  toward what's missing without giving it away; a wrong filing shouldn't feel like a dead end.

### 6. Morrow's voice and the finale

- Morrow's chat now replies with one whole resident sentence (the uncommitted rewrite; it reads
  much better). Remaining bug: **from Act 7 it parrots the line the player just typed**, since that
  line is now in the corpus. Exclude it. But Morrow's last words in the finale were the stop code
  echoed back plus Kit's "you can turn it off its ok", which landed well, so keep a deliberate
  version of that.
- The final log read `skip mossfield:south_rail (unfinished) reason=none` because the player never
  said a quotable line. Make sure a group reliably produces one, or give it a better fallback.

### 7. Small fixes
Everything else in the changelist's "Smaller notes": Theo's museum isn't covered by the dawn
repair, the 2014-12-14 heartbeat date is behind a small json link, and the playtest script's empty
`MORROW_PLAYERS` (make the site fall back to the plugin's roster so Act 9 can't break).

## The bar for quality

**Minecraft building: think it through before placing anything.** For every build and every
cipher object, work out on paper first: the footprint, what's above, below and beside it (terrain,
water, caves, other builds, rails, paths), where a player approaches from and what they see from
there, the light level, and which way an ordered sequence reads from the player's side. Then
verify in the actual world: render with `view.py`, check with RCON (`execute if block`, `data get
block`), and walk it in game from where players stand. Specifically check for: blocks overlapping
or replacing other builds, things sunk into or floating above terrain, clues hidden behind walls
or in unloaded/dark spots, rails or paths cutting through buildings, sequences that read right to
left, signs facing a wall, and anything Morrow's dawn tidy or the restore service would undo.

**Difficulty: hard, interesting, and possible.** The group should feel smart, not lucky. Every
puzzle needs: a reason it exists in the fiction, a fair route to discover it, layers that each
teach something, outside knowledge a determined group can look up, a way to self-check the answer,
and hints that unstick without spoiling. No puzzle should be solvable by skipping its layers
through a leak (like LogBlock giving away Iona's chest), and none should dead-end on something
players can't see or can't know. Work out for each one: how a group gets stuck, how long, and
what gets them moving again.

**Writing: no corny text.** Everything players read or hear should sound like the person who
wrote it: 2011 teenagers on a forum (lowercase, typos, in-jokes, short), a tired sysadmin on IRC,
a support guy's macros, Theo in 2026 (quiet, trailing off, never theatrical). No "the truth lies
within", no ominous narrator voice, no characters explaining the plot to each other, no AI-sounding
phrasing. Morrow is unsettling because it's mundane, not because it's spooky. Read every new line
aloud in your head; if it sounds like a game wrote it, rewrite it.

**Audit yourself, adversarially, before you call it done.** After building:

1. **Solve the whole ARG cold**, from what a player can see, act by act, without using the answer
   key. Note every place you had to use knowledge a player wouldn't have.
2. **Try to break it:** skip ahead, file things early or wrong, dig where you shouldn't, search
   LogBlock and page sources for leaks, play as an op and as a resident, restart the server mid-act.
3. **Check every clue against the world:** coordinates, block IDs, sign text, file contents,
   timestamps, MD5s, time zones. One wrong digit breaks a puzzle.
4. **Review the writing** as an editor: cut padding, kill clichés, keep voices distinct.
5. Write the audit up in `morrow/v2/`, with what you found and fixed, and anything you couldn't
   verify, plainly marked.

## How to work

1. **Plan first, then get the owner's approval before building.** The owner pivots often and
   wants to see the plan. Start with a written plan in `morrow/v2/`: the world and decoration pass
   (build by build), the figure and skin redesign, every new puzzle (who made it, why, its layers,
   where it lives, what it pays off, how players verify it), the new lore, and the fixes. Show
   sketches or `view.py` renders for builds and skin previews, and the new hand-in formats and
   Theo's arc act by act. Stop and ask for approval there.
2. Build in canon → world → plugin → site order, keeping each check green as you go:
   `python -m morrow.canon.verify`, `python -m morrow.world.selftest`, `plugin-v2\gradlew.bat
   selftest build`, `python -m morrow.world.e2e_site` (needs the copperline-v2 preview), and
   `npm --prefix discord-v2 run dry`.
3. Rebuild the test world through the full pipeline and **play your changes on the test server**
   (join as an op-less resident, use RCON for `/mrrw act N`, `/mrrw replay`, `/mrrw dawn`). Look at
   every build and puzzle from where a player would actually stand.
4. Run the self-audit above, fix what it finds, and audit again until a cold solve goes clean.
5. Commit on `morrow-v2/storyline` in sensible chunks. Don't push to `main`, don't deploy.

## Known gotchas

- Code-placed redstone dust is a dot: set its connections explicitly. RCON `setblock` doesn't
  propagate lever power.
- Figures (Mannequins) vanish in unloaded chunks; they need a virtual clock.
- A chiseled bookshelf needs its BlockData slot flags set, not just its inventory.
- Beta's empty NBT lists are typed BYTE. `mcregion.set` doesn't relight (run `relight.py`).
- Layout anchors mean **standing level**, not the grass block. Getting this wrong sank every
  floor once.
- `.gitattributes` marks private media `-text`; don't let autocrlf touch binaries.
- Multi-line Python string patches broke repeatedly here; use the Edit tool.
- The `vercel` CLI on this machine is logged into a partner's account; use the Vercel MCP (team
  `team_2HAUKLhWF4QVYDHEt5FbNeHu`, project `the-observance-kjxn`) if you need deployments.

## Done looks like

The owner plays it again and it feels like a real place with real people's history in it: builds
worth exploring for their own sake, clues that feel discovered rather than placed, ghosts they
can't miss, puzzles with real depth all the way through, varied ways of handing in what they
found, a Theo they care about, and lore they want to read. Every act still advances correctly,
every check is green, a cold solve of the whole game works, and the answer key and walkthrough
match the game exactly.
