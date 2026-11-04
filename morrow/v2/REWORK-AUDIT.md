# Rework audit (2026-11-04)

The plan was `REWORK-PLAN.md`; this is what got built, what I checked, how, and what I could not
check. Everything below was done on the branch `morrow-v2/storyline`; `main` and the live site were
not touched. The test server is `build/mrrw-v2-test` (port 25599, RCON 25598); the playtest site
is `dashboard/scripts/playtest.mjs` on 3041.

## 1. What changed, in one table

| Area | Change | Proof |
| --- | --- | --- |
| Canon | Reading order fixed everywhere (patch's notes, the Residents' Key, Iona's ID wall, Rook's ledger, the floor's rows nearest the ladder first); Iona's key is `mrrw:halt:case6118:2011-06-02:i_bell` (base64 with no `A`: no stack of one); 50 lived-in 2011 signs in the corpus; LogBlock drops the chest rows, gains the trapdoor tell, finch's junction levers, the crooked barn corner, the portal, Act 9 player rows; Theo's arc (unmask and restart variants, the key refused at the desk, timed notes T-06c/T-07d/T-09c, the ask T-10-ask, the after T-10-after); the IRC trim; the 800x500 panel with the Mumble window behind it; heartbeat logs; six new forum posts and three archivist notes; the Copperline history and status archive; the engine lines; the skip fallback | `python -m morrow.canon.verify` (99 checks, 23 new) |
| World | `morrow/world/livedin.py` (every build furnished, the between-places life, Kit's trail), `build2011.py` (junction pins, the barn jog, the manhole), `inject.py` (every ordered run via `layout.wall_run`, all signs and chests) | `python -m morrow.world.selftest` (reads the synthetic world as a player faces it) and `python -m morrow.world.livecheck world` on the live server after the full pipeline |
| Plugin | Real-time figures with bubbles that follow, the cue, start-near-players, witness journalling, the Act 9 bed scene; the floor tidy with the sign-back note; Iona's chest merged after its first opening; the kit chest refilled; the creeper completed; the rail stops at z 82; MAINT ON answered and reverted (Act 5's advance); the console key gate; rewrites once per act; no parroting; the finale as a scene; Theo's timed notes; `/state` roster and first joins; the chest written in client order at install | `gradlew selftest build`; `python -m morrow.world.livecheck acts`; `python -m morrow.world.e2e_finale` |
| Site | June's folder and the console key as hand-ins; roster fallback; none-replies; the ask after booking; Act 9 tells (region-notation Location, post ids, backdated rows); status archive and heartbeat date; About page | `python -m morrow.world.e2e_site` (all checks, 16 new) |
| Bot | Keeps relaying Theo after the stop | `npm --prefix discord-v2 run dry` |
| Skins | Eight hand-painted skins uploaded to MineSkin on 2026-11-04 (`morrow/v2/skins/mineskin.json`), signed textures in `config.yml` | the upload receipts; not seen in a client (section 6) |

## 2. The cold solve, act by act (what a player can see, and where I needed more)

I replayed every act from the player's side of the site and the world (RCON reads of the live
server, the playtest site at Act N, the renders in `rework-previews/`). "Needs" marks the places
where I had to know something a player is not told; each has its route now.

- **Act 1.** robots.txt, the page source, the forum, the security question. Unchanged. The About
  page is new and only lore. Nothing needs outside knowledge.
- **Act 2.** The newbie chest at spawn gives tools and a bed (and Morrow refills it at dawn, in
  character: "1 chest restored (spawn)"). The board, the portal, the creeper, Rook's two rooms and
  his ledger are there to read. The trapdoor tell: the shed hatch is an oak trapdoor; the block log
  says cairn placed it on 12 May 2011 at 06:50, and trapdoors are a 26 May 2011 block. Needs: the
  wiki's version history (the Act 2 hint says so).
- **Act 3.** June's lamp flashes Fl W 5s; finch's torch clock in June's yard gives the same period
  from a 2011 object (six repeaters at 4 ticks plus a torch: 50 ticks). `sambro` opens her folder
  and that is the hand-in. The creeper gets its eyes at the Act 3 dawn with "24 blocks restored
  (spawn)": the first thing Morrow visibly completes, next to spawn, with its WIP sign beside it.
  The bread ledger: cairn's "bread count" post (142 baked, 40 home, 24 in the storehouse chest, 20
  at spawn, one per crate) equals the week-of-9-May tally column (58); with the book's lowpath row
  it would be 60. Needs: nothing a player lacks.
- **Act 4.** Unmasking by any two tokens. The reply follows the strongest token (lot, date,
  waived) so the group hears a different confession depending on what they found. One token gets
  "That's one thing. I'd want two before I said anything."
- **Act 5.** The floor reads nearest row first from the landing (the sign says whose floor it is;
  Morrow's note on its back says what it did). Rows 1-3 are the colour run; LogBlock rebuilds them.
  The plaintext hashes to the checklist. The hand-in is the plaintext's own instruction: edit the
  unwaxed MAINT sign to MAINT ON. Within a minute: "maint: on. sessions open: 1. restore
  continues". At dawn it reverts ("1 sign normalised (spawn)") and Correction begins. Filing "dont
  tell theo" gets Rowan's line and moves nothing; the tape request still delivers the file. C3:
  the 11 June lever rows (jct 1 on, 2 and 3 off), set them, ride, and her station holds the stop
  in her own words. Needs: nothing. The replay starts when someone is near a figure's first spot,
  after the cue line, in real time, with bubbles that follow the head.
- **Act 6.** The rewrites (forum, status, three landmark signs, the board's spur note, the last
  line of Rook's ledger) all break 15 characters or print CST in May. The ledger is its own
  before/after. The EXIF thumbnail (320x200, readable) shows the Start confirmation and the Mumble
  window with rookery, patchcord (idle) and tvale at 06:44: the lead to MIC-5-18-B.WAV two acts
  early. T-06c arrives the night after the confession.
- **Act 7.** The ID wall reads HALTMRRW left to right facing south, inside an ops room with a
  manhole marked "CL ops / keep out". Iona's chest: no log rows; read it in the world; the first
  opening is journalled and the next dawn merges it ("1 chest normalised (june's point)"), so one
  session reads it. Counts are base64 index + 1 and never 1. The hand-in is the TCAdmin console
  (`key accepted. stop refused: maint=off sessions=1 (lowpath)`); the desk refuses it with "Put it
  where she'd put it", and hint 4 says the panel takes a key. Needs: the player must think of the
  console, which the floor's plaintext named in Act 5.
- **Act 8.** Theo's box has the two heartbeat logs: the 2011 excerpt shows the 06:17 kill, the
  06:44 restart (pid changes, uptime resets) and the 11:14 stop; the March 2019 file shows `down`
  from 22:55 to 23:30 and `up=0d00:00:50` at 23:35, four minutes after Kit's email. T-08a now says
  so. cairn's routine fixtures are inside his barn along the north wall under a sign. A bed at
  lowpath's place is queued at once and removed at dawn.
- **Act 9.** The absorbed profiles carry a Location in Iona's region notation (the first-join
  spot), post ids above 30112, and LogBlock rows "from 2010" placing hay, carpet and quartz. A bed
  placed now is kept ("bed kept"); that night lowpath walks its last leg in, lies down, and the
  session ends. T-09c the next morning.
- **Act 10.** Booking gets Theo's ask about finch's sign. The console, the vote, the code at the
  annex. Morrow's last words are the code and Kit's line; then the residents' figures walk home one
  session at a time in join order (`session rookery: end` ... `session kitbag: end`), the golems
  stop, the log prints with the skip reason in a resident's words (else finch's 2011 sign), the lamp
  flashes once, "exit 0", lights out; the status page reads "no heartbeat" beside the first
  heartbeat date. A week later, T-10-after.

## 3. Break attempts

| Attempt | Result |
| --- | --- |
| File the floor's words, the tape, Morrow's name, or the key at the desk to skip an act | Each gets Theo's reply and moves nothing (`BridgeLogic.DESK_ADVANCES` has no such ref; SelfTest covers it) |
| Open June's folder before Act 3 | 404 (the folder does not exist yet); the hand-in event only fires on a successful login |
| Paste the key in the console before Act 6 | The panel is locked; from Act 6 a wrong key is "key rejected (watchdog still armed)" |
| Edit the MAINT sign before Act 5 | Nothing listens until Act 5; from Act 8 an unwaxed MAINT ON is reverted at dawn every day; a waxed one is never touched |
| Dig for Iona's chest and read it over several sessions | The first opening is journalled; the next dawn merges the stacks (the golem race). LogBlock has no chest rows to fall back on; the IRC line and the notes cell say where it is |
| Read the chest by LogBlock | No `chest_put` rows exist (verify checks `rows_for_act` has none) |
| Guess the junction levers | Three levers, eight states; the log gives the answer, and the main line still runs to the farms with them all off |
| Break the note wall, the corridor, the sculk room | Fixtures protect them (unchanged); the note wall's protection follows the new reading order |
| Op vs resident | Ops do not count for the join and online triggers; `/mrrw act N` always wins |
| Mid-act restart | State, recovery, roster, paths and the bridge state persist; the floor tidy, the chest merge and the kit refill are flagged once |
| Page source / bundles | The bundles are per act; the key is only ever an MD5; the console key hash joins at Act 6 (a wrong key cannot be derived from it) |
| Two residents' lines and parroting | Morrow never quotes the line just heard or the speaker's last ten minutes (SelfTest: 0 of 200 echoes; another player still can be quoted their words) |

## 4. Clue by clue

| Clue | Checked against |
| --- | --- |
| Rook's floor | `livecheck world`: 162 wool blocks read nearest row first decode to the plaintext; md5 in the checklist |
| The ID wall | reads HALTMRRW left to right facing south on the live server |
| patch's notes | read left to right from the door spell BAGGAGE (pitches and bases) |
| The Residents' Key | four signs read facing south decode with the join-order keys |
| Iona's chest | 48 stacks, none of 1, decoded in the order the client lists a double chest (the install writes them through the double inventory, because the upgrade swaps the halves: found and fixed here) |
| Finch's junctions | rails at the pinned heights with a lever beside each; the LogBlock rows on 2011-06-11 21:02 precede her 21:03:10 line |
| The trapdoor | at the shed hatch; the row is dated 2011-05-12 06:50:31, block 96 |
| The barn corner | the jog cairn built in March is exactly the set Morrow broke on 2 Sept (verify); the world shows it squared after install; the server log says 18 blocks |
| The dock | gone; the sandstone apron starts where it stood; the 2011 screenshots and Dynmap tiles keep it |
| The torch clock | six repeaters at 4 ticks and one torch in June's yard; period 50 ticks = 5 s = the lamp's Fl W 5s |
| The EXIF thumbnail | 320x200 inside panel_ok.jpg, Mumble window with rookery / patchcord (idle) / tvale at 06:44 (rendered in `rework-previews/panel-thumb.png`) |
| The heartbeat logs | 2011: down 06:20-06:40 and after 11:14, pid 14022 then 14310; 2019: down 22:55-23:30, back 23:35 with up=0d00:00:50 |
| Bread arithmetic | 142 - 40 - 24 - 20 = 58 = the real week-of-9-May column |
| Post-id tell | backdated posts start at the last real id + 1 (30113) |
| Region notation | `r.-1.0.mcr c(17,1)` for a first join at -230, 28 (e2e) |
| Time zones | unchanged and still verified (Iona's CDT/CST, June's Halifax filenames, Morrow's CST tell) |
| Every 2011 sign | 15 characters a line (verify); every lived-in sign stands in the world (selftest) |

## 5. Editor's pass on the new lines

Theo: double spaces everywhere, no "mate", short sentences, nothing explains the plot. The four
unmask variants share the last line so the scene lands the same way whichever is heard. The
drafts are three short letters that say one true thing each. The visitor book has only Theo's
entries (Kit "hadn't logged in since he was fifteen", so he is not in it). The engine lines are
Iona's templates filled with read words (verify holds them to `ENGINE_VOCAB` plus the corpus).
The signs are 2010 teenagers' signs: lowercase, abbreviations, one joke each.

## 6. What I could not verify here (for HUMAN-TODO)

- How the eight skins look on a Mannequin in a real client, and whether the sleeping pose takes on
  a Mannequin (the code falls back to standing on the bed for three seconds).
- The chat bubbles' size and readability at distance (TextDisplay, line width 200).
- The MAINT ON edit by hand (SignChangeEvent); I reverted a code-written sign at dawn, which is the
  same path minus the event.
- The bed scene with a real player placing a bed (BlockPlaceEvent); the RCON run used the state.
- Whether the torch clock's period reads as 5 s by eye in 1.21 (the repeaters came through the
  upgrade and the install re-links the wire; I counted the repeaters, not the ticks).
- Which lever state at junction 1 sends a cart up the spur in 1.21: the canon says jct 1 ON. If the
  game's switch routes the other way, swap the `on` flags in `logblock.JUNCTION_LEVERS` (the
  rows regenerate) rather than the world.
- Voice notes and the owner's media (as before).

## 7. Known limits

- The incident replay waits up to three minutes for a resident near a figure's start, then runs
  anyway; lowpath waits one minute. With nobody online they wait all night (journalled).
- The finale's going-home walks are capped at 60 blocks so Kit's figure (800 blocks out) arrives
  inside the ninety-second phase.
- `/mrrw stop CODE` (the operator fallback) echoes "console" where a player's code would be.
