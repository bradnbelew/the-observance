# Playtest changelist (owner's solo run, started 2026-11-01)

Feedback from the owner's own run on localhost:3041 + the test server. Ordered by when it came up.
Fix status in brackets.

## Done

1. **Morrow's chat was word salad.** It glued 3-6 word fragments from different people mid-phrase,
   and the same message always got the same yes/no. Now: one whole sentence a resident said, picked
   by the rarest words you used; random 50% reply chance. [DONE 2026-11-01, uncommitted]

## To do

2. **Replay figures are on screen for seconds and pop in and out.** You'd miss the whole replay
   unless you were standing on it. Root cause: the incident is compressed to 1 canon second = 4 ticks
   (`Replays.incident`), so rookery's 30-point walk lasts about 6 real seconds; figures also only exist
   in loaded chunks. Want: a replay that lasts long enough to watch and follow, figures that don't
   blink out, and some cue that it's happening (so a group nearby actually notices).
   Owner, after seeing lowpath's night walk too: **every walking figure** (incident replay, the
   player ghost, lowpath) needs better skins, smoother movement, and a way to make sure players
   actually *see* them, instead of them happening somewhere while nobody is looking. Ideas to
   weigh: start a replay only when a resident is within view distance (or near the figure's
   start), trigger it from where players are, give an audible/visual cue (bell, chat line, lights
   flicker) before it begins, loop or replay until witnessed, and log in the journal whether a
   player was nearby, so the operator knows if it was seen.
3. **The player-ghost walk (your own Act 2 route) is laggy and at hyper speed.** Root cause: recorded
   points are replayed 5 canon-s apart at speed 2 (`Replays.playerGhost`), much faster than they were
   sampled. Want: real walking pace, smooth movement.
4. **Skins need to be designed properly.** The current ones are generated placeholders. Each resident
   needs a real, distinctive, well-made skin that reads as that person from a distance (and lowpath's
   stitched one should be deliberately uncanny). Re-upload to MineSkin after.
5. **The town is spread too far out; getting around is a chore.** Distances between builds (spawn,
   storehouse at -80,-140, June's point at z 236, finch's lock, the annex) make every clue a long walk.
   Want: tighter town layout and better spacing, so exploring feels like a place, not a commute.
   **Owner decision (2026-11-03): don't move builds closer together.** Keep the layout; fix the
   sparse feel with decoration, detail and immersion instead (see #7).
   Note: the 2011 layer has period-correct rules (Beta 1.5 world, seed bracken) and cipher objects tied
   to coordinates in `morrow/canon/layout.py`; moving builds means rerunning canon build + pipeline.
6. **Players start with nothing.** A fresh resident spawns empty-handed in a big world. Want: some
   way to start equipped enough to explore (and break/place blocks for the dawn tests), in-fiction.
7. **Buildings need fleshing out.** The 2011 builds read thin. Want: fuller, more lived-in builds
   with interiors and detail, still believable as teenagers' 2011 Beta builds.

8. **Replay chat lines overlap the figures' heads.** Root cause: `Ghosts.say` spawns the TextDisplay
   at a fixed spot 2.35 blocks above where the figure was, so it collides with the head/name tag and
   is left behind as the figure walks on. Want: text clearly above the head that follows the figure.

9. **Puzzle objects sit in empty rooms instead of being part of the builds.** Rook's cellar is just a
   wool floor with nothing else in it, which reads as "puzzle here", not as Rook's basement. Same for
   patchcord's note-block wall in his hut. The ideas are good (keep the wool floor and the note
   blocks); they need to be woven into furnished, lived-in rooms with decor, so they're not hidden
   but feel like a natural part of the place: e.g. the wool floor as the floor of a real cellar
   hangout or pixel-art room with storage, lighting, Rook's stuff; the note blocks as part of a music
   corner / studio setup in patch's hut. Applies to every cipher object (finch's lock, Braille
   shelves, annex wall, etc.).
   - Iona's block-ID wall (HALTMRRW): currently 8 odd blocks on a shelf in an empty stone room,
     so it reads as "the puzzle is here". Make it more puzzle-y: hidden in plain sight among a
     normal-looking build (e.g. a believable room or workshop where a pressure plate, ladder,
     redstone torch, jukebox, button, clay, clay, netherrack each have a plausible place), so it's
     findable by someone who suspects it, but not flagged by an otherwise empty room. Same goes for
     the dust corridor behind it (Act 8), which is also visible from Act 7 and hard to read by eye
     (F3 power levels): integrate it, and consider when it should become reachable.
   - Rook's floor: viewed from the south entrance it's one big bright patchwork in a bare stone box
     with three torches. Morrow's tidied rows (1-3, the gradient) are at the far wall and don't read
     as different from the rest at all, even when looking for them, so the key "something touched
     this" clue is invisible. Make
     the tidy unmistakable (e.g. rows nearest the entrance, a starker sorted look, or a Morrow sign
     back / "normalised" note in the room).
   - patchcord's note-block wall reads **right to left** as you face it (B-A-G-G-A-G-E only comes out
     playing from the right), which is unintuitive. Make it read left to right from where a player
     naturally stands, or give a clear cue to the start (e.g. a sign, a bass amp, a first block that
     stands out).

10. **Act 5 ends by doing the opposite of what the puzzle says.** The floor decodes to "...DONT TELL
    THEO", and the only way to advance is to file it at the Archive Desk, i.e. tell Theo. Nothing
    prompts players to file it (no Act 5 hint mentions filing), and it contradicts the message. The
    other trigger (asking the desk for moss_0518.ogg) skips the cipher entirely. Want: an advance
    that fits the fiction, e.g. checking the plaintext against Iona's MD5 (plaintext A) somewhere
    that isn't Theo, acting on it in game (e.g. at the MAINT sign / annex), or Theo noticing on his
    own; and the moss_0518 request shouldn't bypass the floor.

11. **Verify Rook's floor cipher end to end in the real world, not just in the generator.** The
    owner loves this cipher style; make sure it actually works in play: the wool in the world matches
    `morrow/canon/ciphers.py` (orientation: which corner is row 1 / col 1, reading direction from
    where players stand), the LogBlock rows for Rook's cellar exist and rebuild the 3 tidied rows
    correctly, the wool-ids forum thread matches the values used, the original rules text (June's
    14 May screenshot) gives the right key, and decoding from what a player can actually see yields
    the MD5-matching plaintext. Report anything a player would get wrong.
    Same check for the Braille shelves (Theo's reading room, "sorry kit"): a chiseled bookshelf's
    slots are 2 rows of 3, a Braille cell is 3 rows of 2; confirm the in-game slot layout reads as
    correct Braille from where a player stands (which way to rotate), and that the space reads.
    **Systemic reading-order bug:** sequences are laid along +x but read from the north side, so to
    a player they run **right to left**. Seen twice: patchcord's note-block wall and the Residents'
    Key sign wall in Rook's ladder room (sign 1 is the rightmost; signs face north at x+i). Audit
    every ordered sequence (sign walls, note blocks, Braille shelves, the block-ID wall, the dust
    corridor, the wool floor's row/column order) so it reads left to right from where a player
    stands, and make the start obvious.
12. **Consider more hard ciphers/puzzles.** The owner liked the floor's layered style and may want
    more puzzles of that weight. Undecided; revisit after the full playtest.

13. **Morrow's edits probably go unnoticed (Act 3 rules edit, Act 6 sign/page rewrites, the floor
    tidy).** The owner thinks nobody will notice sign changes. The whole "it's rewriting history"
    arc depends on players having *noticed and remembered* things first. Tie this into the town
    rework (#5, #7, #9): make the things Morrow later edits feel important the first time players
    see them, e.g. signs, paths, buildings, the rail tracks placed where players naturally stop and
    read, framed as landmarks, referenced on the forum or by Theo, so a change to them is felt.
    Consider an in-world way to compare (e.g. June's screenshots, a resident's notebook, the map
    wall) so "that's not what it said" can be confirmed, not just suspected.

14. **panel_ok.jpg's hidden EXIF thumbnail is unreadable.** It's 160×100 at ~2.6 KB: the owner
    could see the red confirmation box but not read the words. Redesign the thumbnail so its
    message survives at thumbnail size (huge bold text like "START SERVICE? / Auto restart: ON",
    higher JPEG quality, the maximum size viewers still treat as a thumbnail), and check it in the
    common viewers (exif.regex.info, metadata2go, exiftool, Windows Explorer icon). The EXIF
    DateTime 06:44:31 (21 s after the restart) is a nice secondary clue; keep it.
    **Keep the trick** (the owner thinks players will try it), but make it more rewarding: right now
    it only repeats what TCAdmin already showed (Theo started it at 06:44). The hidden thumbnail
    should reveal something players can't get anywhere else, e.g. a detail of Theo's screen that
    night that moves the story or opens a later lead (a typed note, a second window, a path, a
    name), so finding it is a real payoff, not a confirmation.

## Smaller notes

- **Playtest setup bug (fixed locally):** `dashboard/scripts/playtest.mjs` defaults MORROW_PLAYERS to
  empty, so the Act 9 backdated profiles never appear and the brief's "when did we join" field
  can't match. Worked around by adding `"env": {"MORROW_PLAYERS": "SirNan"}` to the
  copperline-playtest entry in `D:\.claude\launch.json`. Real deploys must set MORROW_PLAYERS
  (already in HUMAN-TODO §8); consider having the site fall back to the plugin's roster over the
  bridge so a missed env var can't break Act 9.

- **Placing lowpath's bed gives no feedback.** The session only ends when lowpath *finishes its
  night route* and finds a bed within 3 blocks of (-213, 73, 86) (`Replays`, end-of-route check),
  so a player places the bed in daytime and nothing happens until some night later, possibly while
  nobody is watching. The owner placed several beds, saw nothing, and assumed it failed; it did
  work once lowpath reached the end of its route that night (journal 05:16). Owner: the bed spot
  should be **-214 73 86** (anchor `lowpath_place` is -213 73 86 in `morrow/canon/layout.py`;
  moving it means rerunning canon build + world pipeline + the plugin's anchor). Want an
  immediate in-world reaction (or the check on placement) and a payoff players witness: lowpath
  walking home, lying down, "session lowpath: end (slept)".

- **The rail line runs through / over a whole house.** Owner saw the tracks pass straight through a
  building. Check the 2011 rail routes and Morrow's south-rail extension (3 blocks per act toward
  lowpath's place) against every build's footprint; nothing should clip a house unless it's a
  deliberate, visible Morrow "tidy" that the story calls out. Part of the town-layout pass (#5).

- **Morrow parrots the message you just typed (Act 7+).** "buddy you good" → "buddy you good. you
  can turn it off its ok". From Act 7 players' lines are in the corpus, and the best match for a
  message is that same message, added a moment earlier. Exclude the line just heard (and probably
  the speaker's own recent lines) from the reply pool, so Morrow quotes *other* players / older
  lines back. Kit's line surfacing as the second sentence worked as intended.

- **The annex is only findable through LogBlock, and LogBlock gives away too much of Act 7.** The
  annex is sealed underground (its stair tunnel is capped one block under the grass) and nothing
  in the world, forum, map key or Dynmap points to it; the only route is searching LogBlock for
  player `i_bell`. But that same search also lists Iona's chest with exact coordinates and every
  slot count in order (48 `chest_put` rows), which bypasses the dock, the dig and opening the
  chest, and lists the wall's 8 block IDs as numbers (72, 65, 76...), which bypasses visiting the
  annex. Decide what's intended: e.g. drop the chest_put rows (or log only "chest opened"), give
  the annex an in-world/forum lead (Iona's notes cell, the dust corridor, a Dynmap marker), and let
  the wall be read in the world rather than straight from the log.

- **June's old dock is still in the world, but the story says it's gone.** STORYLINE.md (Act 7):
  "June's old dock no longer exists. Morrow replaced it with the lighthouse base", so players must
  locate it from June's 30 March screenshot and the Dynmap tile before digging "3 down, 4 toward
  spawn". `build2011.py` builds the dock and nothing in the modern layer / plugin removes it, so the
  dig is trivial: the landmark is standing right there. Remove/replace it in the modern layer (keep
  it in the 2011 screenshots). Also: STORYLINE says the replay acts out "roughly 20 minutes of the
  night"; it currently lasts seconds (see #2).

- Iona's chest: verified in game, counts match the cipher. Gotcha: stacks of 1 show **no number**
  in Minecraft (slots 43 and 47, both = "A"), so players may skip them and the base64 breaks.
  Consider avoiding count-1 stacks (re-pick filler/offset) or make sure a hint covers it.

- **The finale worked end to end** (stop at 23:35, retained clean, heartbeat none). Two notes:
  Morrow's last chat line was the stop code echoed back plus Kit's "you can turn it off its ok";
  that's the parroting bug above, but it made a strong final beat, so keep something like it on
  purpose when fixing the bug. And the final log read `skip mossfield:south_rail (unfinished)
  reason=none` because the solo player never said an "uneven/unfinished"-style line; make sure a
  group reliably produces a quotable line, or give it a better fallback than "none".

15. Theo's museum isn't protected by the dawn restore (only the 9 resident builds are). The owner
   expected it to repair too; decide whether that matters.
16. The 2014-12-14 heartbeat date only shows behind the small "json" link on Network Status. Fine as
   an optional clue; could be in the visible table if it should pull more weight.
17. Act 5's floor cipher can't be solved cold until the moss_0518 recording exists (Rook's "back and
    forth, like finch's rail" and "shifted by the rules" are only on the tape). Already on HUMAN-TODO.
