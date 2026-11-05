# Council audit (2026-11-04)

Five reviewers read the branch `morrow-v2/storyline` at e26c7ad, each with one lens: the plugin's
progress safety, the site/bridge/bot, the writing, the ARG design, and Minecraft orientation. Every
finding marked **verified** was then re-read in the code by hand. Nothing was run and nothing was
changed. Line numbers are from e26c7ad.

Two things in the reviewers' briefs were wrong and were discounted: the brief called Copperline a
UK host (canon: Chicago, `people.py:8`) and the residents "six teenagers" (canon: 14 to late 20s).
Theo's British idiom ("No worries at all!", "Cheers, T", "mate") is his designed habit
(`people.py:43-45`); whether a Naperville support lead talks like that is a decision, not a bug.

## 1. Verdict

The build is an ARG with a puzzle-hunt spine. The 2026-10-09 complaints are mostly gone: nothing
prints the answers, the world reacts to what players do, the hand-ins are acts in the fiction, and
the finale has real choices. Three things still break the spell: Theo's desk replies are stamped
"Act 4" / "Act 6" as their date, the hints read like a game master, and in Acts 7-9 the desk's
canned "none" replies arrive exactly when players most want a conversation.

**Progress can be stranded today in five ways** (section 2). Each is a few lines; none needs a
design change. Fix those before any friend plays.

## 2. Blockers (verified): a group can get stuck with no signal

**All five fixed the same night** (plus the pause-when-empty note in HUMAN-TODO §8 and the runbook).
Proof: `gradlew selftest build`, `tsc`, `e2e_site` (its fallback check now expects the last good
state), `livecheck acts` on the test server, and two live RCON/bridge runs: a waxed `MAINT ON` at an
Act 5 dawn was unwaxed, reverted and moved the act to 6 by itself; `console_key` posted at Act 6
was journalled early and applied the moment `restart` moved the act to 7 (journal: 6 -> 7 -> 8).
The test server's MAINT sign was re-placed facing north by hand (`install.maint_sign` had already
run there); a fresh `/mrrw install` places it that way now.

| # | What | Where | Repro | Smallest fix |
| --- | --- | --- | --- | --- |
| B1 | **A desk ref that arrives one act early is dropped for good.** `tryAdvance` marks it `released=true, applied=false`; `releaseDue` skips released entries; `advance()` never rescans. | `Campaign.java:283-328` | Act 6: players enter Iona's key in the panel (the site accepts it from Act 6, `build.py:149-151`; the plugin needs Act 7, `BridgeLogic.java:83`). `console_key` is journalled `advance_early` and lost. They reach 7 via `restart`, never re-enter the key (the panel already said "accepted"), and sit at 7. Same shape for `restart` filed before the MAINT dawn and `kit` filed before `console_key`. | At the end of `Campaign.advance()`, loop the desk map and `tryAdvance` every entry with `released && !applied`; and in `console/route.ts:37` answer the correct key at act < 7 with "key rejected (watchdog still armed)". |
| B2 | **The MAINT sign faces away from spawn and only its front counts.** `maintSign()` places `OAK_SIGN` with default rotation 0, so the front faces south; spawn (-233,71,30) is north of the sign (-236,71,32). A player sees the blank back, edits it, hears Morrow's "maint: on" (`onSignChange` accepts either side), but `revertUnwaxedMaint` and `Finale.maintOn` read `Side.FRONT` only: no Act 5 advance, no Act 10 ready. | `ModernLayer.java:796-804`, `Nights.java:81-98, 318-333`, `Finale.java:110` | Act 5, any player at spawn. | Set the sign's rotation to face north in `maintSign()`; ignore `Side.BACK` edits in `onSignChange` (or check both sides everywhere). |
| B3 | **Waxing the MAINT ON sign, or editing it back before dawn, kills the Act 5 advance.** The 5->6 move only fires inside the "unwaxed and not already OFF" branch. | `Nights.java:318-333` | Act 5: player writes MAINT ON and waxes it "so it sticks" (waxing is exactly what Act 10 will ask for, and waxed=untouched is visible from Act 2). Every dawn: nothing. | When `act==5 && act5.maint_on && !act5.maint_reverted`, unwax, revert and fire the callback regardless; drop the `act5.maint_on` requirement for a sign already reading ON when Act 5 opens. |
| B4 | **A thorough Act 6 ticket gets the Act 3 deflection and never fires the advance.** All rules with `act <= current` stay live; the winner is the complete match with the highest `need`, then the most hits. `found_morrow` (Act 3, nine tokens, need 2) and `restart` (need 2) tie on need, so "LogBlock shows morrow put the glass on the tower; the panel shows you restarted it at 06:44:10 mid-session" gets "That's not anyone. Probably a plugin" from a Theo who confessed two acts ago. | `desk.ts:69-83`, `later.py:74-80, 90-111` | Act 6, any ticket that piles in evidence (which groups do). | Prefer the rule with the highest `act` on ties, or give rules an `until` act and filter them out of the bundle. |
| B5 | **The site falls back to MORROW_ACT=1 for 25 s on any bridge hiccup, and hand-ins are fire-and-forget.** A null bridge state is cached for 25 s and `effectiveAct` returns the env act (documented as staying 1 for the whole run). `postDeskEvent` tries once for 2.5 s; `brief` and `booking` are never re-posted; `june_folder` is posted with a bare `void` from the proxy. | `bridge.ts:255-334`, `campaign.ts:36`, `submitticket.php/route.ts:16`, `dobrief.php/route.ts:25`, `viewticket.php/page.tsx:29`, `proxy.ts:79` | Plugin at Act 6, MC server restarting: within 25 s June's and Theo's folders 404, the panel says "locked". Bridge down during `/dobrief.php`: ticket says accepted, act never reaches 10, the only trace is a Vercel log line. | Keep the last good state (`lastGood`), retry `postDeskEvent` twice with backoff, re-post every kind from `viewticket`, use `after()` in the proxy; runbook: raise MORROW_ACT as a floor when an act moves. |

Related and verified: the plugin's bridge answers every request on the main thread with a 10 s
wait (`Bridge.java:115-122`). The test server sets `pause-when-empty-seconds=-1`
(`build/mrrw-v2-test/server.properties:45`) but HUMAN-TODO never tells the owner to; on Paper's
default (60 s) the world stops ticking with nobody online: no dawns between sessions, no Theo timed
notes, no online-minute count, and every bridge call returns 503 so the site drops to the env act.
**Add `pause-when-empty-seconds=-1` to HUMAN-TODO §8 and the runbook.**

## 3. High (verified unless marked)

**All fixed the same night.** Sign-back notes: Morrow re-hangs a wall sign as a post one block out
(or beside that spot when something stands there), front text kept, note on the back
(`ActDirector.noteOnBack`; verified live on Rook's door, June's F2, the barn close-up). Rune signs
stand on the plaza. The restore removes players' blocks before putting originals back and keeps an
original whose spot is still filled; signs, banners, chests and lecterns inside a build cannot be
broken by non-ops; explosions are restored and fire is refused on builds. Hint 8.1 now invites
the question; `ingest` carries the same 36-hour silence as `kit`. A `stop_pending` flag goes to
disk the moment the stop begins and a restart mid-scene finishes it. The desk dates drops by day
(no more "Act N"). `restoreTune` sets each note block's instrument from the block under it. The
desk serves `moss_0518.txt` (patch's index of the Mumble night, generated by `files.py`) while the
recording is missing, so the floor is solvable now. Ops are not residents in the roster.
Proof: canon build + verify, `gradlew selftest build`, `tsc`, `e2e_site` (new check: the
attachment serves the recording or the notes), live RCON runs of the sign re-hang and the rune
signs.

- **Sign-back notes are written on wall signs** (`build.py:312-321`, `ActDirector.java:79-96`,
  `RecoveryService.java:219-224`). Four of five targets (Rook's door, the barn close-up, June's F2,
  the floor sign) are `wall_sign` in `inject.py:119-139`; a wall sign's back is flush against its
  support, so "floor: 3 rows normalised" can never be read. Only the rules post sign works. Retarget
  each note to a post sign near the same build.
- **Rune signs sink into the plaza floor** (`Corrections.java:136-147`): `near(w,"spawn",-3-i,0,4)`
  with `spawn` a GROUND anchor (y=70 is the floor block) replaces floor blocks; the boards sit below
  the player's feet. Use `dy=1`.
- **Broken signs, banners and chests inside a build come back blank** (`RecoveryService.java:119,
  250-256`): the restore stores `BlockData`, which has no block-entity contents. Rules sign 2 (the
  C6 key, a once-flag), the sign backs, the flags' patterns, the tally chests and the kit chest are
  all inside a build radius and not in `Fixtures.isProtected`. Cancel the break for non-ops on
  `TileState` blocks inside builds, or snapshot `BlockState`.
- **A refilled hole is skipped at dawn and the original forgotten** (`RecoveryService.java:250-266`):
  the `broken` loop restores only if the block is air, then clears; the `placed` loop that would
  re-open the hole runs after it. Break a wool block of Rook's floor, fill it with cobble: a permanent
  hole in the cipher. Run `placed` first, keep `broken` entries that are still not air.
- **Explosions and fire bypass the restore** (`Fixtures.java:245-252`, `RecoveryService.java:110`):
  only `BlockBreakEvent` is recorded. Rook's floor is wool in a dark cellar. Record
  `EntityExplodeEvent`/`BlockExplodeEvent` damage inside builds; cancel `BlockBurnEvent`.
- **Act 8's gate exists only in the answer key** (`BridgeLogic.java:84-85`, `later.py:234-236`): the
  only route 8->9 is to file Kit's phrase at the desk, and no hint says to ask Theo where Morrow
  learned it. Rewrite hint 8.1 to invite it.
- **The Act 8 beat can run backwards** (`BridgeLogic.java:85`, `later.py:139-158`,
  `bot.mjs:490-495`): `ingest` (phrase alone) advances at once, so Act 9 opens and the fake-Theo DM
  goes out before T-08a's 36-hour note. Drop `ingest` from `DESK_ADVANCES` or give it the delay.
- **The stop lives only in memory for ~2-3 min** (`Finale.java:566-614`): `stopped` is flagged in
  the last scheduled task. A crash mid-finale un-stops the game; the code can be typed again and the
  scene replays. Mark `stopped` and `stopped_at` at the top of `stop()`.
- **Theo's desk drops are dated "Act N"** (`desk.ts:203`, rendered at `support/index.php/page.tsx:32`).
  The one place the site says the word "Act". Compute a real evening timestamp from act start +
  `after_hours`.
- **Note blocks probably play harp after the upgrade** (reviewer claim, unverified in game): the
  chunk fixer sets `note`/`powered` but not `instrument`, which only recomputes on a neighbour update
  the protected wall never gets; `Fixtures.restoreTune` sets only the note. C2's bass-vs-drum sound
  would be lost (planks/stone still visible). Check with F3 on (-189,73,-68); if `instrument=harp`,
  set it in `restoreTune` and at install.
- **Act 5's signature cipher depends on a tape that doesn't exist** (`later.py:167-170`,
  HUMAN-TODO §3): "back and forth" and "shifted by the rules" are only on moss_0518.ogg, and the desk
  hands the file over whether or not it is on disk. Ship the transcript fallback STORYLINE already
  proposed and guard the dead file.
- **Op counted as a resident** (`ChatListener.java:55-58`): `onJoin` adds every joiner to the roster,
  so with MORROW_PLAYERS unset the owner's op account gets an Act 9 backdated profile and counts
  toward the USB quorum. Skip ops unless `auto_advance_count_ops`.

## 4. Medium

**Fixed 2026-11-05, all but one.** `/mrrw hint N` releases an act's first N hints at once (the
site reads `hint_floor` over the bridge beside its timers). The finale's figures walk to the free
block outside each home's nearest wooden door. A bed queued in Act 8 survives the first Act 9 dawn
and the two bed boxes agree. A player's chest with things in it is never deleted at dawn.
`state.yml` is written to a temp file and moved into place. The bot's Act 7 DMs are keyed by
member id and slot. `usb_quorum` in config.yml caps the residents the vote counts. Held and
partial tickets carry the rule as a hash, so a decoded link no longer says an early guess was
right. The 10 June IRC block is cut from Theo's Act 7 copy and arrives as its own Act 9 drop
("There's a day missing from the IRC log I sent you"); Iona's chest line no longer gives the
spot, her third notes sign does. lowpath's import note is gated to Act 6. The console key is
compared case- and space-insensitively. The password brake only counts failures. The replay cue
goes out at dusk so a group elsewhere can come. World scripts: chest signs on a two-high post
above the chests, the warning sign moved left of the rules (the cake table stood in front of it),
the mic stand dropped, the Mumble sign off the desk lever, the shed cellar two blocks high with its
torch on stone, the farm-stop fence past the last rail. **Left as is:** junction 3's stored rail
shape (straight vs the game's settled south-east): it cannot be verified from code and the hub-to-
farms ride works either way; `layout.py`'s stale comment was fixed.
Proof: canon build + verify, world selftest, `gradlew selftest build`, `tsc`, `e2e_site`, bot dry,
and a full first-install run on 2026-11-05 (`build2011` on a fresh copy of the source world ->
`pipeline` -> Paper `--forceUpgrade` -> `/mrrw install`, 24 parts -> `livecheck world` all green,
the MAINT sign facing spawn on a clean install; `livecheck acts` green except the pre-existing
"six figures walking" count of five).

- Hints are a wall-clock drip from act start (24/72/120/168 h × player factor, `desk.ts:210-214`),
  not a stuck detector; weekly groups get all four whether stuck or not, fast groups see none. Clock
  from the last progress journal line, or expose `/mrrw hint N`.
- Finale going-home targets assume a north door (`Finale.java:317`): June's figure ends inside her
  NE corner log, Kit's inside his hut. Per-home door offsets.
- An Act 8 bed still queued when Act 9 opens is removed at the first Act 9 dawn (`RecoveryService`
  `placed` + `Replays.java:249-266`), contradicting "bed kept"; `bedNear` and `onPlace` disagree at
  dy=-2. Skip lowpath beds in the `placed` loop at act >= 9.
- A player's chest near any build is deleted with contents at dawn (`RecoveryService.java:295-305`,
  `setType(AIR,false)` drops nothing). Skip non-empty containers.
- `state.yml` and the roster are written non-atomically (`State.java:260-271`); a crash boots at
  Act 1 with no flags (golems/tally re-created, `install.*` lost so the corridor is unprotected).
  Write to `.tmp` and `ATOMIC_MOVE`.
- Act 7 DM plan keyed by member index and display name (`bot.mjs:201-212, 477-495`): a joiner,
  leaver or nickname change re-sends DMs. Key on `member.id` and a slot index.
- USB quorum counts everyone who ever joined (`BridgeLogic.java:28,145`); six joined, four quit,
  two can never reach quorum. Count residents seen in 14 days, or `/mrrw quorum N`.
- Ticket tokens are readable base64 (`campaign.ts:482-486`): `?c=` decodes to the matched rule id,
  including held future-act refs ("ingest" at Act 3 tells them the guess was right). Encrypt or hash.
- The Act 7 IRC drop (`later.py:191`, `irc.py:115-123`) states the clean copy's existence, location
  and MD5 two acts before the Residents' Key pays it off, and has Iona announce the chest's exact
  spot and encoding in a logged channel after refusing to on Mumble. Trim the 06-10 block to Act 9;
  move "3 down, 4 toward spawn" to her notes signs.
- lowpath's forum profile shows "account created 2016-08-03 by archive import" from Act 1
  (`forum.py:49`, `member.php/page.tsx:34`; unverified whether the Act 1 bundle carries it). Gate the
  note to act >= 6.
- Console key compare is case-sensitive while the desk lowercases (`console/route.ts:37` vs
  `desk.ts:38`). Normalise.
- The password brake refuses the correct password past 60 failures per IP (`proxy.ts:40-41, 74`);
  five friends behind one NAT.
- Incident replays start when a resident is within 64 blocks, else after 3 min anyway
  (`Replays.java:40-41,103`); a group in Rook's cellar all night misses it with no cue. Wait the night
  while players are online but far; print the cue line globally first.
- Newbie/bread chest signs are hidden behind the chests (`inject.py:101,104`, same y, far side);
  the spawn_warning sign is occluded by the cake table; patchcord's studio is cut in two by the mic
  stand (`livedin.py:374`) and the `patch_mumble` sign overwrites the desk lever (`inject.py:171`);
  the shed root cellar is one block high with a torch on air (`livedin.py:214-221`); junction 3 is
  laid straight but is a T and settles to `south_east`, diverting westbound carts (`build2011.py:631`);
  the farm-stop fence overwrites the penultimate rail (`build2011.py:633`).

## 5. Orientation: what is right

Independently computed from the right-hand rule, not from comments: `layout.LEFT_TO_RIGHT` and
`wall_run` are correct for all four facings; `floor_row_z` puts row 0 by the ladder landing with
columns west to east for a reader facing north. Verified correct from the player's stance: Rook's
floor and its three tidied rows; the Residents' Key signs (facing south, preface leftmost); Rook's
ledger (facing west); the spawn rules posts; patchcord's note wall (facing south, note 0 at the
east end = left, "play from here ->" on top of it); the Braille shelves (facing north, cell rotated
90° clockwise consistently, "sorry kit" with shelf 5 the space); Iona's ID wall (H at -277 = left
facing south); the dust corridor (walk east); the flags (K leftmost from the gallery facing north;
mirrored from June's rise, which is not the reading spot); finch's lock (bit 0 west = left entering
from the spur); Iona's chest (west half = slots 0-26 in Beta; the install rewrites through the
double inventory in GUI order); the torch clock (period 100 game ticks = 5.0 s, so "50 ticks" is
redstone ticks; strictly Iso not Fl); June's lamp (1 s on / 4 s off); the creeper (code lays the
TOP three rows: `SOLUTIONS.md:17` "bottom three rows" is the doc being wrong); lowpath's bed (any
bed in the room passes `bedNear`; "west wall" in SOLUTIONS is wrong text); Kit's hut signs; the
ladder landing; the rail against every footprint (nothing clips a house; the extension stops at
z 82 on the low path's dirt). The self-tests read through the same helpers they test, so they prove
consistency, not handedness; the handedness is what was computed here.

## 6. Writing

Scores: 2011 forum 4/5 (the best thing in the build), later posts/notes 2/5, IRC 3/5, Theo's desk
3/5, **hints 1/5**, Morrow in-game 4/5, signs 4/5, Discord 4/5, site chrome 3/5, trailhead 3/5.

Rules that fix most of it at once:

1. No hint may be an aphorism or an imperative. Every hint is Theo noticing something in his own
   files, first person, with a reason to mention it. Offenders: `later.py:209` "Old sites always
   said where not to look. Search engines were told first."; `:229` "Look at what a picture says
   about itself"; `:241-242` "Three things. The sign, the sessions, the key." / "Wax the sign."
2. Theo never tells anyone where to go or what to type (`later.py:88, 100, 126, 199-200`; the
   panel at `tcadmin/legacy/page.tsx:456, 466, 479` gives walkthrough directions and names #general).
3. Answers appear once, at most twice. The stop recipe lives in five places (IRC, Iona's notes,
   finch's station chest, the floor, hint 10.2).
4. No "Act", "#general", "clue", "key" in anything rendered in the 2011/2026 fiction.
5. 2026 Theo writes like a tired IT guy on a phone. Cut trailer cadences: `later.py:33` the
   refrain "It's just Mossfield. It's just the residents." on all four unmask variants (a player
   who files two tickets hears the script); `:97-98` "That's not the bit I'm ashamed of."; `:134-135`
   "Not the morning. Everything after."; `:168-169` "I don't think I'm going to now."
6. Theme words are banned from character mouths: "someone comes home" (`records.py:19`, the invoice
   note; `later.py:323, 525, 532`), "the holes are ours" / "dont let it finish anything"
   (`later.py:328-331`, rook.txt), "deleting who did the work" (`forum.py:226`).
7. Morrow is Iona's templates or a whole corpus sentence, nothing else. Two breaches:
   `Finale.java:132` "the low path brings you home", `:158` "no console here. the console's at the
   maintenance annex".
8. Iona doesn't do bits (`irc.py:37, 42, 67, 115` her "lol"); Dale gets the jokes.
9. Tell-words must be load-bearing: Metra works, "radiator" and "cardigan" (`later.py:300-303`) don't.
10. The owner's own trailhead line "find the key" (`OPERATOR-RUNBOOK.md:263`) frames it as a puzzle
    before it starts; the fallback line on :272 is the right register.

Facts to settle: Rook "sixteen" when he wrote the rules for bracken (`RECORDING-SCRIPTS.md:51`) vs
19 in 2011 (SMP didn't exist in 2008); Iona says "not on here, patch is recording" at 06:01:30 then
types "im putting a key somewhere theo cant reach" into server chat at 06:02:31 (`serverlog.py:88`);
Morrow places glowstone on 18 May 2011 (`logblock.py:69`), eight days before Beta 1.6 brought the
Nether to SMP (if deliberate, say so in the hint; players with the wiki will catch it); Theo 25->40
in scripts vs "twenty-two" at `later.py:151`; Kit, 14, asleep at Theo's on a school night (one IRC
line fixes it).

## 7. Design

Gate ratings (in-fiction motivation): 1->2 self-evident; 2->3 automatic; 3->4 needs the hint
(5-second flash -> lighthouse characteristic -> Sambro is specialist); 4->5 needs hint 4.1, which
arrives day 1; 5->6 self-evident once any of three copies is read; 6->7 **needs a hint and no Act 6
hint mentions filing**; 7->8 self-evident if they remember Act 5; 8->9 **answer key only**; 9->10
self-evident; 10 self-evident but Saturdays only and quorum-gated.

Cipher census: 33 built. Keep as organic: the seed, commute, lighthouse, hub coords, region
notation, MD5s, map ids, sign backs, torch lock, slipped hands, fossil chunk, dye inversion,
lodestones, wrong moon, door register, habit lock (the best idea in the game), base64 chest, the ID
wall. Thin but redeemed: the floor, Braille, flags. Built because the design wanted one: **D11**
(subtitles -> vibration -> hex -> filename) and **D13** (dust power -> `SESSIONS:1:LOWPATH`, which the
console already printed at the Act 7 hand-in); D15's double columnar on six passwords by a
19-year-old on 15-char signs. Cut D13, soften D11, reduce D15 to one pass and make it the only route
to rook.txt. The "don't tell Theo" dilemma has no teeth: the only 9->10 path is telling him; let
T-09a at least name the choice.

Conversations the desk cannot have, and what it says: "Theo, are you okay?" -> "Thanks! I'm not
sure I have anything on that, sorry. If you've got a filename, that helps." (`later.py:177`); "we're
sorry about Kit" -> "I remember kit. I don't have anything of theirs that isn't on the site"
(`:174`); any ticket with "marked" or "our server" -> the trailhead denial, live in every act
(`:160-164`). Five more none-replies keyed on act, one of which invites evidence, would carry Acts 7-9.

Doc inconsistencies: Iona's chest 54 stacks (STORYLINE:509) vs 48; crontab in Rookery's FTP
(STORYLINE:536) vs Theo's box; the stop plan's TCAdmin auto-restart toggle (STORYLINE:542-547, 641)
is not built and players will look for it; replay 20 min (STORYLINE:393) vs 8 (`build.py:195`); hints
three tiers by stuck time (THEO:105-109) vs four by act clock.

## 8. One-minute in-game checks for the owner

- Stand at spawn (-233,71,30) facing south: you should read "MAINT OFF" on the sign at (-236,71,32).
  If it is blank, B2 is real.
- F3 on (-189,73,-68): expect `instrument=bass`. `harp` means the note-wall finding is real.
- After digging to Iona's chest, F3 each half: expect `type=left` at x=-238, `type=right` at -237,
  `facing=north`; the first GUI stack is 28 cobblestone. Two `type=single` means the install skipped it.
- Lever ON at (-216,71,-60), cart from the hub: F3 on the rail reads `shape=north_east` and the
  cart goes north. Flip jct 3 ON then OFF: F3 on (-140,74,-120) reads `south_east`.
- Landing (-260,63,4) facing north: the nearest row starts, left to right, at x=-270.
- (-238,40,62) facing south: the leftmost sign reads "key: all of us".
- (-244,70,-28) facing north: shelf 0 has top-left, top-middle and bottom-right filled (= 's').
- Gallery (-243,87,250) facing north: K is the leftmost banner, yellow half on your left.
- After `/mrrw act 6`: the rune signs at (-236..-237, ?, 34) should be at eye level, not underfoot.
- `/mrrw stop` on the test server: June's figure should stop in front of her east door, not in the
  corner post.

## 9. Not covered

Nothing was executed. Paper's pause-when-empty behaviour, the 1.13 chunk-fixer's note-block
instrument handling, Vercel's treatment of a dangling promise in the proxy, and the chest `type`
after upgrade are from version knowledge, to be checked in-game (section 8). `plugin-trailhead/`
was read for text only. Skins, bubbles, voice notes and the owner's media remain as in
REWORK-AUDIT §6.
