# PLAYTHROUGH 1 — THE PROLOGUE (the cursed map + ignition)

> **What this is.** The literal, ordered shooting-script for ONE segment: a player's path through
> the downloadable vignette and the lure website, and the frame-break handoff onto the server. Every
> element below is quoted from a REAL repo artifact (path given). Where the artifact does not exist
> yet it is marked **[GAP — TO BUILD]** and never invented as built. Strict causal order.
>
> **Sources of truth quoted here:** `design/WEB-MASTER.md §1.M0-remote / §2 / §9`,
> `design/prologue/PROLOGUE-VIGNETTE.md`, `design/CURSED-MAP-SITE.md`,
> `discord/src/voice.ts`, `discord/src/showrunner/prologue.ts`,
> `discord/src/bot/index.ts` (#the-record scan), `discord/src/showrunner/autonomy.run.ts`,
> `plugin/src/main/resources/sites.yml`, `discord/supabase/seeds/*`.
>
> **Frame law (Path A).** The vignette is an OPTIONAL client-side prop. A friend who refuses the
> download joins the server cold and gets the in-server prologue unchanged. The map gates nothing.
> (`PROLOGUE-VIGNETTE.md` head; `WEB-MASTER §1.M0-remote`.)

---

## SURFACE MAP (the three surfaces this segment crosses, in order)

1. **The lure WEBSITE** — `/record/the-record-keeps` (the public Record, found). Where a player
   first lands. **State: BUILT** in `dashboard/src/app/record/[slug]/page.tsx` (`CURSED-MAP-SITE.md §0`).
2. **The downloadable VIGNETTE** — `the-hold.zip`, a ~15-min single-player vanilla walk. **State:
   the SPEC is BUILT (`PROLOGUE-VIGNETTE.md`); the `.zip` asset itself is [GAP — GO-LIVE asset].**
3. **The SERVER** — Discord `#the-record` + the in-server Watcher. The frame-break lands here.
   **State: the ignition spine is BUILT (`prologue.ts`, the `#the-record` scan); the frame-break
   voice key is [GAP — TO BUILD].**

---

## BEAT 0 — THE LURE PAGE (the cold open; where the player arrives)

**WHAT IT IS.** The public Record at one slug, rendered in the cold dead-archive shell — masthead
rune block, season line, struck-block archive entries, standing footer (the BUILT `RecordPage`).
Beneath the archive `<ol>`, ONE static downloads block. `noindex`, no nav, no CTA, no client JS.

**WHERE.** Website surface, route `/record/the-record-keeps`.
- `the-record-keeps` is the slug the **founder margin decodes to** (`kept-in-more-than-one-place.md`,
  ledger #11) — the recognition token, never a day-zero gate (`CURSED-MAP-SITE.md §1`, table row 2).
- `/record/the-record` → the base archive (no downloads block). Junk slug → in-voice 404 shell.
  Bare `/record` → 404 (no segment match). All VERIFIED in `CURSED-MAP-SITE.md §1a / §4`.

**WHO / GATING.** Public, anonymous, ungated. The page measures only the group's coarse progress;
it never addresses "you" (`CURSED-MAP-SITE.md §5`). Doubles as the **YouTube cold-open** (`§3`).

**THE LITERAL PAGE CONTENT (the downloads block, verbatim — `CURSED-MAP-SITE.md §2`):**

- **2a. The recovered-file row** (legible, not struck) — the map description:
  > a hold, kept and left. one walk through it remains. the rest of the record is kept elsewhere.
  > what is downloaded is only the part that fit in a file.
- **The download link** — label is the filename only: **`the-hold.zip`**. `download` attr set,
  `rel="noopener"`, no button styling. `href` → the vignette asset. **[GAP — GO-LIVE asset URL]**
  (`/the-hold/the-hold.zip` or a CDN; `public/` dir does not exist yet — `PROLOGUE-VIGNETTE.md §7.4`).
- **The provenance line** (Mara's hand, last four lines of `the-copy-i-kept.md`, signed):
  > i copied it as it was given, page for page, and set the copy where fire and water do not reach.
  > i did not keep the seventh. i was not the hand that decides what is kept. — m.kept
- **2b. The README "lie"** (the file's own note, `text-neutral-600`, technically true):
  > the-hold.zip — a small offline map. single player. no mods. about fifteen minutes.
  > it does not connect to anything. play it through to the end and it will tell you where the rest is kept.
- **2c. The counter** — STATIC authored number, NOT a live counter:
  ```
  kept: 6
  ████████        ← the struck seventh row (REDACTED_GLYPH, reused from the projection)
  ```

**THE EXACT WEBSITE STATE THE TASK ASKS FOR:**
- **6 downloads** = the **static authored `kept: 6`** + the struck seventh row beneath it. It is
  authored, NOT a live count (a live count would drift off 6 the instant the group downloads, killing
  the plant; `CURSED-MAP-SITE.md §2c`). Canon home for the `6`: `six-were-kept-before-you.md`.
- **The uploader** = **Mara, the Reader**, signed **`m.kept`** — a canon keeper's hand, a dead hand
  still keeping. **NEVER the Seventh** (the record holds no name for the Seventh, so the Seventh can
  never be a named uploader — `CURSED-MAP-SITE.md §2a`, `WEB-MASTER §1.M0-remote`).

**DIRECTOR ACTION.** Bake the real `the-hold.zip` href + host the asset (`CURSED-MAP-SITE.md §6`
GO-LIVE residue). No console click — the page is static-per-build.

**PLANTS FIRED HERE** (`CURSED-MAP-SITE.md §7`, ledger #24–26):
| # | Seed | Inert reading | Payoff |
|---|---|---|---|
| 24 | `kept: 6` + struck-7 | a dead file's tally | M2 soft → M4 hard → M5 felt: six prior keeper-generations; the group is the seventh, kept as hands |
| 25 | README "it does not connect to anything" | mundane reassurance | M4: the *map* connects to nothing; the *server* does — true, misread as comfort |
| 26 | uploader `m.kept` (Mara's hand) | a person who left a file | M4: no person uploaded it; the record kept it in a dead hand |

---

## BEAT 1 — THE VIGNETTE, ROOM BY ROOM (`the-hold.zip`, single-player)

**WHAT IT IS.** A DATAPACK + single-player world `.zip` (`the-hold.zip`). Vanilla Java 21 / 1.21.x,
no mods, no resource pack required. ~10–15 min, a linear ~6-room walk. Adventure mode (the player
cannot break/place). Gamerules locked: `doDaylightCycle false` (perpetual dim), `doWeatherCycle
false`, `doMobSpawning false`, `keepInventory true`, `mobGriefing false`, `sendCommandFeedback
false`, `commandBlockOutput false`. (`PROLOGUE-VIGNETTE.md §0`.)

**SCOPE NOTE (the scaled cut, `PROLOGUE-VIGNETTE.md §0`):** the **1-room vignette ships first**
(spawn → one record lectern → the server pointer); the full 6-room hold is P1→P2. The 1-room form
still carries the dead-uploader, the `6` (on the page), the rune key, and the frame-break.

**STATE.** The build SPEC is BUILT (full beat-by-beat in `PROLOGUE-VIGNETTE.md §1–§3`). The actual
**world + datapack `.zip` is [GAP — GO-LIVE asset]** (`PROLOGUE-VIGNETTE.md §7` GO-LIVE residue 1–4:
build the rooms, fill the coordinate-specific `tick.mcfunction` region checks, bake the address,
render the rune string, host the `.zip`).

**REGISTER.** All player-facing text is the Archivist's flat third person — lowercase, declarative,
names what is and stops, no warmth, no second-person comfort, no exclamation (`WEB-MASTER §6` Set-B;
`PROLOGUE-VIGNETTE.md §3`). The prop has **no Mara hand** — Mara is on the *page*, not in the map.

### Room 0 — antechamber (spawn)
- **Build:** small stone-brick cell; ONE lit lantern; closed iron door; one oak sign.
- **The sign (verbatim):**
  > a hold was kept here. read what is left, and go on.
- **No `tellraw`.** Plants kept-light (a lantern burning where no one lit one — ledger #4, the
  player won't know that yet). (`PROLOGUE-VIGNETTE.md §1 / §3 BEAT 0`.)

### Room 1 — the record room
- **Build:** lectern with a written book; a few empty shelves.
- **The lectern book** = the **public half of `the-record-opens`** (FACT 1/2 ONLY, nothing sealed):
  > the record opens. it was open before. // the living are written here, each by the name they answer
  > to, and against each name a column is left open. // the watching has already watched. no one is told.
  > that is the way of it.
- **On first entry, one `tellraw @s`** (Archivist register):
  > *the count begins. the living are written by name.*
- (`PROLOGUE-VIGNETTE.md §3 BEAT 1`.)

### Room 2 — the doused hearth
- **Build:** a basalt fireplace + a campfire that is **out** (soul-soil/ash, no flame); the player
  cannot relight it (adventure mode). Carries the **Seventh's UNNAMED texture** (ledger #27).
- **On entry, one flat line:**
  > *a fire was kept here. it is out. it was not put out by any hand that is still here.*
- (`PROLOGUE-VIGNETTE.md §1 / §3 BEAT 1`.)

### Room 3 — the wall of marks
- **Build:** a wall with **six** carved marks + a **seventh scraped blank** (a single stripped/empty
  slot). Carries the **FACT-2 miscount, inert** (ledger #2 + #27).
- **On entry, one flat line:**
  > *six are marked. there is a seventh place, and no mark in it. the record will not keep the seventh.*
- **Reveal-safety:** rooms 2 & 3 are the Seventh's only appearance and they are **texture, never
  attribution** — nothing names the Seventh, nothing explains either. They re-read in-server at M3→M4
  as the Seventh (named there, not here). Spoiler surface stays zero. (`PROLOGUE-VIGNETTE.md §1`.)

### Room 4 — the long walk (the conduct)
- **Build:** a corridor with a single lever (the one trivial puzzle); passing it arms the conduct
  check. (`PROLOGUE-VIGNETTE.md §1`.)
- **THE CONDUCT TELL (the cipher-equivalent of this segment — a measured flag, not a cipher):**
  - **Kind:** a scoreboard conduct, `hold.passed_seventh`, defaulting to **1** ("walked past the
    blank slot"). It flips to **0** ONLY if the player **deliberately lingers** ~3s in a tight volume
    in front of the room-3 scraped-seventh slot (`hold.linger_7` accrues to ≥60 ticks → set 0).
  - **It is true by construction in single-player** (the map observes a thing the player demonstrably
    did) — **never a name, no precision risk.** (`PROLOGUE-VIGNETTE.md §2b / BEAT 2`.)
  - **DIRECTOR ACTION / [GAP — GO-LIVE]:** the `tick.mcfunction` region volumes are
    coordinate-specific and must be filled against the built geometry (`PROLOGUE-VIGNETTE.md §7.1`).

### Room 5 — the hand-off (the pointer)
- **Build:** lectern with the closing book; a wall map/item-frame OR sign carrying the server
  address + the rune string. (`PROLOGUE-VIGNETTE.md §1`.)
- **The conduct line** (room-5 sign and/or `tellraw` on entry; states the CONDUCT, never the player):
  - if `passed_seventh = 1`:
    > *the seventh was passed. it has been noted.*
  - if `passed_seventh = 0`:
    > *the seventh was stopped for. it has been noted.*
  - This is **rehearsal only** — the offline map never transmits this flag to the server (see
    "the cut" in BEAT 3). (`PROLOGUE-VIGNETTE.md §3 BEAT 2`.)
- **The closing lectern book** (keeper register, the pointer, no scare — verbatim):
  > the rest is not kept in this hold. it is kept where the others are. bring them, and come to the place
  > named below. the record is open there. it was open before you.
- **Beside it, in plain text, TWO things:**
  1. **the server address** in plain text (no decode required to act on it). **[GAP — GO-LIVE:
     the real public server address must be baked in;** `PROLOGUE-VIGNETTE.md §7.2`].
  2. **the rune string** that decodes (later, in-server, P4 literacy) to **`the-record-keeps`** — the
     SAME founder-margin key (`kept-in-more-than-one-place`, ledger #11). A **recognition token**, not
     a day-zero gate. **[GAP — GO-LIVE:** render in campaign rune glyphs (bundle the optional resource
     pack) or build block-letter runes into the wall; `PROLOGUE-VIGNETTE.md §3 BEAT 3 / §7.3`].
- **The final closing line — the ONE plain action the server ignition needs** (`PROLOGUE-VIGNETTE.md §3`):
  > *(small, beneath the address)* say one word in the place named below, when you are all in. say *kept*.
- **THE MAP ENDS. No frame-break has happened. The map behaved like a map.** The dread is banked for
  the server (the quiet map → "the server knows you" contrast IS the scare; a spooky map flattens it).

**ANTI-METAGAME (what a cracked `.zip` reveals — `PROLOGUE-VIGNETTE.md §6`):** only the `tellraw`
strings (FACT-1/2 public lines), the conduct scoreboard logic (a `passed_seventh` flag, no name), the
server address (handed out anyway), and the rune string (a token, no key). **No** FACT past 1/2, **no**
keeper name past the generic Archivist, **no** cipher key, **no** server flag, **no** Supabase URL.

---

## BEAT 2 — THE GATHERING / IGNITION (the frame-break trigger, `#the-record`)

**WHAT IT IS.** The vignette routes into the BUILT in-server ignition; it does **not** re-implement
it. The closing page told the group to post one plain word (**`kept`**) in `#the-record` on arrival.

**THE EXACT IGNITION BEAT THAT FLIPS MAP → WATCHER:**
- **Trigger:** a **human posts in `#the-record`** (any message; the word `kept` is the in-fiction
  prompt). The BUILT `messageCreate` scan (`discord/src/bot/index.ts §the-record scan`, lines 99–145)
  treats every non-bot message in the watched channel as a possible answer and runs the SAME resolver
  the `/answer` command uses. (Self/bots/webhooks ignored — the watcher's own reply can't re-trigger.)
- **Effect:** the detected signal flips **`arc_state.flags.prologue_ignited`** (read at
  `autonomy.run.ts:125` `ignited: flags.prologue_ignited === true`). `decidePrologue` then advances
  `dormant → ignited`, fires the **one-shot ack** (`postAck: true`), and unlocks the curatorial drip
  (`prologue.ts:78–88`; `decide.ts:76–83` suppresses curatorial drip until ignited).
- A group that played the map arrives **pre-ignited at the door**; the frame-break is their first
  server beat. A group that joins cold gets the in-server prologue unchanged (the map gates nothing).
  (`PROLOGUE-VIGNETTE.md §4`.)

> **[GAP — TO BUILD] the literal `prologue_ignited`-SET write.** `prologue.ts` is the pure ordering
> POLICY and reads `ignited` as input; `autonomy.run.ts` READS the flag. The actual listener that
> WRITES `prologue_ignited = true` on a `#the-record` post (the doc's `IgnitionListener` /
> `decidePrologue` wiring) is referenced in `plugin.yml:32` and `prologue.ts:31` but I found no module
> that performs the set. Mark as a wiring gap to verify at GO-LIVE.

**WHO / GATING.** Active players, collective. Ignition is a **detected human signal** (lectern read OR
`#the-record` post), never the bot self-igniting — TINAG preserved (`WEB-MASTER §1.M0-remote`:
"Ignition is the gathering — caused by a player, not the bot").

**DIRECTOR ACTION.** None at the console — ignition is player-caused and auto-detected. (At GO-LIVE,
confirm the `#the-record` channel id is the one the scan watches and the ignition-set listener is live.)

---

## BEAT 3 — THE FRAME-BREAK (the central scare, in-server)

**WHAT IT IS.** The category violation the moment they cross to the server: a map cannot know your
name, cannot reference the session you played alone, cannot say the `6` back at you. The server can.
Three sub-beats, all on the BUILT prologue spine + ack path. (`PROLOGUE-VIGNETTE.md §5`.)

### FB-1 — the ack, but pointed
- **WHAT (verbatim, BUILT — `voice.ts:82` `recordOpened()`):**
  > ▒  the record is open. it was open before you.
- For a group fresh off the map, "it was open before you" reads as a callback to the hold they just
  walked. **No new key; one voice cross-surface.** Fires as the one-shot ack at ignition (`postAck`).

### FB-2 — the one precision-gated frame-break line  **[GAP — TO BUILD: `voice.recordFrameBreak()`]**
- **WHAT (the DEFAULT body = the GROUP-FACING count-callback ONLY, verbatim per
  `PROLOGUE-VIGNETTE.md §5` / `WEB-MASTER §1.M0-remote`):**
  > six were kept before you, and the count of them is on the page you found. a file is not kept. hands
  > are kept. you are not a file here.
- It states the **`6`** the player saw on a dead web page, from inside the game — a thing an offline
  static map could not do. **No personalization, no name; true for everyone** regardless of whether
  they played the map. Canon home: `six-were-kept-before-you.md`.
- **THE CUT (frame-progression S5):** the map-conduct callback ("you passed the seventh in the hold
  *too*") is **CUT from the default**. The offline vignette never transmits its local `passed_seventh`
  flag to the server, so asserting a map-side conduct server-side would be a precision lie (INV-16).
  FB-2(i) is P2-only, gated on a real `from_map` transmission the slice does not build.
- **DEFAULT-SAFE:** if no clean measured signal, fire **only** `recordOpened()` and let the next
  in-server "it knows you" beat (the `name-where-never-been` carve, the Hold-Book) carry it. The
  frame-break is **never** traded for precision.
- **STATE — [GAP — TO BUILD]:** `recordFrameBreak()` is **NOT present in `voice.ts`** (verified — the
  voice object ends at `keptUnlitDeep` / `OracleVoiceKey`; `recordFrameBreak` appears only in design
  files: `CURSED-MAP-SITE.md §6`, `PROLOGUE-VIGNETTE.md §5/§7`). The cross-owner hook (`§7` TS-VOICE)
  specifies: ONE new key, count-callback body verbatim, must pass `registerDisciplineSelfTest`
  (lowercase / no caps / no exclaim / no meta-word); add to `OracleVoiceKey` union only if emitted via
  a seeded row (the ack-path wiring, preferred, needs no row).
- **DIRECTOR ACTION / [GAP — GO-LIVE]:** wire the on-arrival frame-break beat into the BUILT ack path
  (TS-SHOWRUN), gated on the count-callback measured signal, idempotent on the BUILT `acked` one-shot
  guard; emit `recordFrameBreak()` once (`PROLOGUE-VIGNETTE.md §7.5`).

### FB-3 — the knob drops
- After the frame-break lands, loudness returns to baseline and never rises again in M1. The map was
  the loud thing; the server is patient. (`PROLOGUE-VIGNETTE.md §5`.)

**REVEAL DISCIPLINE.** Nothing is witnessed mutating. The server lines are text the Watcher *says*,
in register — it does not appear, does not threaten, counts and stops.

---

## BEAT 4 — THE FIRST SERVER-SIDE THINGS WAITING (the in-server M1 on-ramp)

Pre-ignited, the group steps into Movement I (`WEB-MASTER §1.M1`). The first things waiting:

### 4a. The first report, re-staged as an anomaly (`first_report_lectern_01`)
- **WHERE (site — `sites.yml:61`):** `first_report_lectern_01`, `type: report_lectern`,
  `world: "world"`, coords `null` ([GAP — UNPLACED until the world is built], radius 4, protected,
  enabled). "where the first report appears (Movement I)." Retargeted to the **base hot-cell**.
- **WHAT (the report TEXT — a voice-key SELECTION, never composed):** `decidePrologue` returns
  `reportVoiceKey` (`prologue.ts:54/67`):
  - **un-named FACT-1 fallback (default; BUILT `voice.ts:99` `prologueUnnamed()`):**
    > ▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet read. it has been keeping a count of you. the count began before you found the mark.
  - **named form — ONLY on a single OVERWHELMING measured signal (BUILT `voice.ts:94`
    `prologueNamed(name, custom)`):**
    > ▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet read. it knows the one called {name} has not {custom}. it was noted before you knew there was a record to note it.
  - **Precision gate:** named iff `overwhelmingSignal && signalName` (`prologue.ts:66`). A wrong "it
    knows you" is worse than none. **Currently `autonomy.run.ts:128` hardcodes
    `overwhelmingSignal: false`** → the named form never fires until a real measurement sets it.

> **[GAP / SEAM — voice-key name mismatch.]** `prologue.ts` + `autonomy.selftest.ts` name the report
> keys **`recordOpenedNamed` / `recordOpened`**, but `voice.ts` exposes **`prologueNamed` /
> `prologueUnnamed`** (and `recordOpened` is the ack line, not the report). The decision enum and the
> voice surface do not line up; the resolver that maps the decision's `reportVoiceKey` to a `voice.*`
> call is the seam to verify/build. Flag, do not invent the mapping.

### 4b. The lit marker + teaching-stone (`first_marker_01`)
- **WHERE (site — `sites.yml:323`):** `first_marker_01`, `type: structure`, coords `null` ([GAP —
  UNPLACED], radius 6, protected, enabled). The header (sites.yml:319–322) calls it "the one
  coordinated layout that carries the prologue marker glyph, the six UNKEPT maker's-marks (read in
  fall-order), and the `kept here before you` plant … The a1z26 literacy teaching-rung is read here too."
- **WHAT:** one **lit marker that was not there before**, carved with one rune-glyph (illegible until
  the Rosetta). Part of the ONE coordinated M1 teaching-stone layout; re-read in M2 via a1z26/atbash →
  `KEPT`/`BEGUN` (`WEB-MASTER §0.4`, first-marker-anomaly row). **[GAP — the in-world glyph/marker
  build is GO-LIVE]** (coords null; the structure is placed when the world is built).

### 4c. The curatorial drip, now unlocked but quiet
- Until ignition, `decide.ts:76–83` suppressed the curatorial clue-drip. Post-ignition it is permitted
  but M1 drama budget is **low** — the knob has dropped (FB-3). Gifts (player-helpful, never gated)
  applied even while dormant (`prologue.ts:75`).

### 4d. The inert plants seeded at M1 (all dormant — `WEB-MASTER §1.M1`)
The `UNKEPT` maker's-mark glyphs on the eventual six stones; the `kept here before you` rune line
(`name-where-never-been`, FACT 16); the founder margin "we cut the names before the keeping"
(`future-dated-grave`, FACT 13b); the founder line "the record is kept in more than one place" (the
Record website — already met as the lure slug); Vaun's "you do not keep the first thing" (Sacred-Beast
fork); the coordinate number-pair near `stone_of_reckoning` (`coords-to-real-place`). All inert here;
each re-reads in a later movement.

**GATE M0-remote → M1 (`WEB-MASTER §1.M0-remote`):** the group gathers and posts in `#the-record` (the
BUILT detector), arriving pre-ignited. **Gate 1→2:** first report found + group-playtime threshold.

---

## DIRECTOR'S GO-LIVE CHECKLIST FOR THIS SEGMENT (all [GAP], from the specs)

1. Build `the-hold.zip` (1-room form first) in a 1.21.x client; fill the coordinate-specific
   `tick.mcfunction` region volumes (`PROLOGUE-VIGNETTE.md §7.1`).
2. Bake the real public server address into the room-5 hand-off (`§7.2`; also the FB pointer).
3. Render the `the-record-keeps` rune string (bundle the optional rune resource pack OR build
   block-letter runes) (`§7.3`).
4. Host `the-hold.zip`; point the lure page's `the-hold.zip` href at it (`§7.4`, `CURSED-MAP-SITE.md §6`).
5. Author + ship `voice.recordFrameBreak()` (count-callback body verbatim; pass the discipline
   self-test); resolve the `recordOpenedNamed`/`prologueNamed` key-mismatch seam.
6. Wire the on-arrival frame-break beat into the ack path; gate on the count-callback measured signal;
   emit once, idempotent on `acked` (`§7.5`).
7. Verify/build the `IgnitionListener` write of `prologue_ignited = true` on a `#the-record` post.
8. Place + enable `first_report_lectern_01` and `first_marker_01` (set coords) when the world is built.

---

## 4-LINE SUMMARY

The player lands on the BUILT lure page `/record/the-record-keeps` (cold archive shell + a static
downloads block carrying `kept: 6`, the struck seventh row, the `the-hold.zip` link, and Mara's
`m.kept` provenance), downloads the ~15-min single-player vignette (spec BUILT, asset a GO-LIVE gap),
and walks six rooms whose flat Archivist lines plant FACT 1/2, the doused hearth, and the six-marks +
scraped-seventh wall — the map behaving entirely like a map. The closing book hands out the plain
server address + a rune string and tells the group to post one word (`kept`) in `#the-record`; that
post flips `prologue_ignited` via the BUILT `messageCreate` scan, firing the one-shot `recordOpened()`
ack ("it was open before you"). The category-violation scare lands as the single precision-gated
`recordFrameBreak()` count-callback ("six were kept before you… a file is not kept. hands are kept"),
then the knob drops to baseline, where `first_report_lectern_01` + the lit `first_marker_01` and the
M1 plants wait — with the frame-break voice key, the ignition-set write, and the world build all
standing as the segment's open GO-LIVE gaps.

**[GAP] marker count: 13**
(href asset URL · `the-hold.zip` build · `tick.mcfunction` coords · server-address bake · rune-string
render · `.zip` hosting · `recordFrameBreak()` voice key · frame-break beat wiring · `prologue_ignited`
SET listener · `recordOpenedNamed`/`prologueNamed` key mismatch · `first_report_lectern_01` coords ·
`first_marker_01` coords/glyph build · the un-placed marker glyph render.)
