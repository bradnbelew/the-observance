# The Observance — PROLOGUE-VIGNETTE (the downloadable client-side prop, beat-by-beat build spec)

> **Owner:** `cursed-map-frame` (A14) — the vignette sub-piece (A) + the frame-break (C). **Path A
> holds:** this is an **optional, one-time, single-player, client-side PROLOGUE PROP** — explicitly NOT
> the delivery vehicle for the server engine. The campaign still needs only the auto-pushed resource
> pack. A friend who refuses the download joins the server and gets the in-server Prologue unchanged.
> **Mints nothing.** Carries **zero** server machinery and **zero** spoilers: no FACT past 1/2, no
> keeper name past the generic Archivist, no cipher key, no server flag, no Supabase URL beyond the
> public server address. Cracking the world file reveals only what the player already saw + the address
> the map hands out anyway. Authority: `WEB-MASTER §1.M0-remote`, `INTEGRATION-V2 §A14`,
> `CURSED-MAP-SITE.md`. Where this disagrees with those, they win.

---

## 0. FORM, TARGET, BUILD ECONOMY

- **Form: a DATAPACK + a single-player world `.zip`** (`the-hold.zip`). The datapack carries the logic
  (functions, `tellraw`, structure placement, the conduct scoreboard); the world carries the built rooms
  and the spawn. Datapack-first because it cannot leak a world's structure browser the way a raw world
  can, and it is the experience itself (`INTEGRATION-V2 §A14` 2f).
- **Target: vanilla Java 21 / 1.21.x, no mods, no resource pack required.** (The vignette may *optionally*
  ship the campaign rune font as a packaged resource pack inside the `.zip` so the closing rune string
  renders in-glyph; if absent it degrades to plain block-letter runes drawn in the build — see §3 BEAT 3.
  This is the ONE optional asset; nothing depends on it.)
- **Spawn: gamerules locked** — `doDaylightCycle false` (perpetual low light), `doWeatherCycle false`,
  `doMobSpawning false`, `keepInventory true`, `mobGriefing false`, `commandBlockOutput false`,
  `sendCommandFeedback false`. Default gamemode adventure; the player cannot break or place. Spawn fixed
  at the antechamber; spawn-point set.
- **Length: 10–15 minutes, a linear ~6-room walk.** Scaled cut (`INTEGRATION-V2 §A14` 2j): the **1-room
  vignette ships first** (spawn → one record lectern → the server pointer); the full 6-room hold is
  P1→P2. The 1-room form still carries the dead-uploader, the `6` (on the page), the rune key, and the
  frame-break (FB-2 degrades to the count-callback, which needs no in-map conduct).
- **Logic substrate:** one **datapack** with `load.json` (sets scoreboards + gamerules once) and a
  `tick.json` driver that runs a `minecraft:` function each tick. Region triggers are `execute as
  @a[...] at @s if entity @s[...]` distance/position checks, NOT redstone clocks the player can see. All
  player-facing text is `tellraw @s` / sign text in the Archivist register. **No command-block clocks are
  visible to the player** (datapack `tick`, not a hopper-clock under the floor).

### 0a. The reveal / anti-jank laws, applied to a single-player prop
- **Nothing is witnessed mutating.** The hold is fully built at world-gen; no structure block fires while
  the player watches. The only state changes are `tellraw` lines and the closing sign/lectern text, which
  are text the Archivist *says/leaves*, not geometry appearing.
- **Idempotent + replay-safe.** Every beat is gated on a scoreboard flag set once (`hold.seen_room1` etc.);
  re-entering a room does not re-fire its line. A player who reloads the world resumes; nothing double-fires.
- **The prop UNDER-PROMISES by law.** It is melancholy and competent. Its one "it noticed" tell is a
  **conduct** (true by construction in single-player), never a name. ALL personalization waits for the
  server. The contrast (quiet map → the server knows you) IS the scare; a spooky map flattens it (`2b`).

---

## 1. THE WORLD — structures, room by room (all vanilla blocks)

A linear stone hold, lit by a few standing lanterns, cold palette (stone bricks, deepslate, polished
basalt accents — the same cold-archive feel as `/record`). Iron doors between rooms open on a hidden
pressure plate or a lever the room's beat arms; the player always has exactly one way forward. No combat,
no inventory puzzles beyond one lever.

| Room | Build (vanilla) | Carries | Plant |
|---|---|---|---|
| **0 — antechamber (spawn)** | small stone-brick cell; ONE lit lantern; closed iron door; one oak sign | the cold register, the kept-light motif (a fire burning where no one lit one) | ledger #4 (the lit marker), rehearsed |
| **1 — the record room** | lectern with a written book; a few empty shelves | the public half of `the-record-opens` (FACT 1/2 ONLY) | FACT 1/2 |
| **2 — the doused hearth** | a fireplace of basalt + a campfire that is **out** (soul-soil/ash, no flame); the player cannot relight it (adventure mode) | the Seventh's UNNAMED texture | ledger #27 (doused hearth) |
| **3 — the wall of marks** | a wall with **six** carved marks (banner/sign glyphs or block-letter runes) and a **seventh scraped blank** (a single stripped/empty slot) | the FACT-2 miscount, inert | ledger #2 + #27 (scraped wall) |
| **4 — the long walk** | a corridor with a single lever (the one trivial puzzle); passing it arms the conduct check | pacing; the conduct tell setup | — |
| **5 — the hand-off** | lectern with the closing book; a wall map (item frame) OR a sign carrying the server address + the rune string | the pointer to the server | ledger #11 (rune string → `the-record-keeps`) |

**Reveal-safety in build:** rooms 2 and 3 are the Seventh's only appearance and they are **texture, never
attribution** — the hearth is doused and the seventh slot is blank; nothing names the Seventh, nothing
explains either. The doused hearth + scraped wall re-read in-server at M3→M4 as the Seventh (named there,
not here). Spoiler surface stays zero.

---

## 2. THE DATAPACK — load + tick + the conduct scoreboard

```
the-hold.zip/
  the-hold/                      (the world)
    datapacks/
      thehold/
        pack.mcmeta
        data/thehold/function/
          load.mcfunction        ← #minecraft:load
          tick.mcfunction        ← #minecraft:tick  (the driver)
          room1.mcfunction … room5.mcfunction
          handoff.mcfunction
        data/minecraft/tags/function/
          load.json   → ["thehold:load"]
          tick.json   → ["thehold:tick"]
```

### 2a. `load.mcfunction` (runs once on load; idempotent)
```
# objectives (created once; re-running is a no-op)
scoreboard objectives add hold dummy
# gamerules — perpetual dim, no weather, no mobs, no block edits
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule keepInventory true
gamerule mobGriefing false
gamerule sendCommandFeedback false
gamerule commandBlockOutput false
# the conduct flag defaults to "passed the seventh" = 1 (true by construction unless they pause at it)
# seeded only if never seeded (the player may have set it on a prior session)
execute unless score #seeded hold matches 1 run scoreboard players set @a hold.passed_seventh 1
scoreboard players set #seeded hold 1
```

### 2b. `tick.mcfunction` (the driver — distance/position triggers, no visible clock)
Each beat fires once, gated on a per-player flag set the first time the region check passes. Example
(room 1; the others follow the same shape, region coords are build-specific):

```
# ROOM 1 — the record room: fire the line once when the player first stands in the room volume
execute as @a[scores={hold.seen_r1=0..0}] at @s if entity @s[x=.., dx=.., y=.., dy=.., z=.., dz=..] \
  run function thehold:room1
# (room1 sets hold.seen_r1 = 1 on the player, so it never re-fires)
...
# ROOM 3 — the wall of marks: if the player LINGERS at the scraped slot (stands in a tight volume
# in front of it for ~3s), clear the "passed_seventh" flag (they paused at it — a different conduct)
execute as @a[scores={hold.passed_seventh=1}] at @s if entity @s[x=.., dx=2, y=.., dy=2, z=.., dz=2] \
  run scoreboard players add @s hold.linger_7 1
execute as @a[scores={hold.linger_7=60..}] run scoreboard players set @s hold.passed_seventh 0
```

> **The conduct is structural, not personal.** `passed_seventh` is 1 by default and only flips to 0 if
> the player deliberately lingers at the scraped-seventh slot. It is **true by construction** in
> single-player (the map observes a thing the player demonstrably did) — never a name, no precision risk
> (`INTEGRATION-V2 §A14` 2b/FB-2). It is consumed ONLY in §3 BEAT 2's flat closing-sign line, which
> states the *conduct*, never the player.

---

## 3. THE BEATS — beat-by-beat, with the de-slopped in-vignette text

All player-facing text is the Archivist's flat third person (lowercase, declarative, names what is and
stops, no warmth, no second-person comfort, no announced emotion, no exclamation — `WEB-MASTER §6`
Set-B; the prop has no Mara hand — Mara is on the *page*, not in the map). Each line is `tellraw @s` in
italic gray, or written-book/sign text. **No line claims the map is haunted; no line explains a trick.**

### BEAT 0 — the threshold (spawn)
- The player spawns in room 0. One sign on the iron door:
  > a hold was kept here. read what is left, and go on.
- No `tellraw` here; the room is just cold and lit. (Plants kept-light: a lantern burning where no one is
  — ledger #4, the player won't know that yet.)

### BEAT 1 — the short hold (`room1.mcfunction` on entry)
- The lectern book in room 1 is the **public half of `the-record-opens`** — FACT 1/2 lines ONLY, nothing
  sealed. Written-book pages, e.g.:
  > the record opens. it was open before. // the living are written here, each by the name they answer
  > to, and against each name a column is left open. // the watching has already watched. no one is told.
  > that is the way of it.
- On first entry, one `tellraw @s` in the Archivist register:
  > *the count begins. the living are written by name.*
- Rooms 2 and 3 each fire one flat line on entry, naming what is there and stopping:
  - room 2 (doused hearth):
    > *a fire was kept here. it is out. it was not put out by any hand that is still here.*
  - room 3 (wall of marks):
    > *six are marked. there is a seventh place, and no mark in it. the record will not keep the seventh.*

### BEAT 2 — the one "it noticed" tell (conduct, deniable, NOT personalization)
- Consumed at the **closing sign in room 5** (and/or a `tellraw` on entering room 5), reading the
  `passed_seventh` conduct. Two authored variants, selected by the flag — both state the **conduct**,
  never the player, never a name:
  - if `passed_seventh = 1` (they walked past the blank slot):
    > *the seventh was passed. it has been noted.*
  - if `passed_seventh = 0` (they lingered at it):
    > *the seventh was stopped for. it has been noted.*
- This rehearses the in-server grammar (FACT 2 — graded by laws no one told you) **without a precision
  violation**, because a conduct in a single-player map is true by construction. It is the seed for the
  OPTIONAL conduct-callback frame-break (FB-2(i)) — but per `WEB-MASTER §1.M0-remote` / `INTEGRATION-V2
  §A14` S5, **the conduct-callback FB-2(i) is CUT from the default frame-break** (the offline map never
  transmits this flag to the server, so asserting it server-side would be a precision lie). The conduct
  line lives in the prop as *rehearsal*; the **server's** frame-break default is the count-callback only
  (§5). FB-2(i) is P2-only, gated on a real `from_map` transmission that the slice does not build.

### BEAT 3 — the hand-off (the pointer; `handoff.mcfunction` in room 5)
- The closing lectern book (the keeper register, the pointer, no scare — verbatim, de-slopped):
  > the rest is not kept in this hold. it is kept where the others are. bring them, and come to the place
  > named below. the record is open there. it was open before you.
- Beneath/beside it, a sign or item-frame map carrying **two things**, in plain text:
  1. **the server address** in plain text (so no decode is required to act on it).
  2. **the rune string** that decodes (later, in-server, P4 literacy) to `the-record-keeps` — the **same**
     founder-margin key (`kept-in-more-than-one-place`, ledger #11). It is a **recognition token**, not a
     day-zero gate; the player is not asked to crack it. Rendered in the campaign rune glyphs if the
     optional resource pack is bundled, else as block-letter runes built into the wall.
- A final closing line tells the group, in voice, the ONE plain action the server ignition needs (§4):
  > *(small, beneath the address)* say one word in the place named below, when you are all in. say *kept*.
- **The map ends. No frame-break has happened. The map behaved like a map.** (The dread is banked for the
  server — `2b`.)

---

## 4. THE HAND-OFF INTO THE SERVER (no new detector code)

The vignette routes into the **BUILT** in-server ignition, it does not re-implement it:

- The closing page tells the group to **post one plain word (`kept`) in `#the-record`** on arrival.
- The **BUILT `messageCreate` ignition detector** (`prologue.ts` / the in-server `IgnitionListener` →
  `decidePrologue`) already fires `prologue_ignited` on a human post in `#the-record`. So the vignette
  hand-off **needs no new detector** — it is a third *on-ramp* into the existing detector.
- A group that played the map arrives **pre-ignited at the door**; the frame-break is their first server
  beat. A group that joins cold gets the in-server Prologue unchanged (the map gates nothing).
- The keener `from_map` flag (set by a dedicated arrival beat) is **P2/optional**; the bare detector
  cannot distinguish a map-arrival from a cold join, and it does not need to — the count-callback
  frame-break (§5) is true for everyone (`INTEGRATION-V2 §A14` S7).

---

## 5. THE FRAME-BREAK — the central scare (in-server, one new voice key)

The map behaved like a map. The scare is the **category violation** the moment they cross to the server:
a map cannot know your name, cannot reference the session you played alone, cannot say the `6` back at
you. The server can. Beat-by-beat (all reuse the BUILT prologue spine + ack path):

- **FB-1 — the ack, but pointed.** The Watcher's first server line is the BUILT `voice.recordOpened()` —
  *"▒ the record is open. it was open before you."* For a group fresh off the map, "it was open before
  you" reads as a callback to the hold they just walked. (No new key; one voice cross-surface.)
- **FB-2 — the one precision-gated frame-break line (the new key, `voice.recordFrameBreak()`).** Fired
  **once**, after ignition, gated on a measured signal, **never** as a guess. **Default body = the
  count-callback ONLY** — a group-facing fact the offline static map could not have said, true for
  everyone, naming no one (de-slopped, `INTEGRATION-V2 §A14 §3`):
  > six were kept before you, and the count of them is on the page you found. a file is not kept. hands
  > are kept. you are not a file here.
  - It states the `6` the player saw on a dead web page, from *inside* the game — a thing a map (offline,
    static) could not do. No personalization; no name; the count is true regardless of whether they even
    played the map (`S7`). Canon home: `six-were-kept-before-you.md`.
  - **The conduct-callback overload FB-2(i) is CUT from the default** (`S5`): the offline vignette never
    transmits its local `passed_seventh` flag to the server, so a map-side conduct assertion would be a
    precision lie (INV-16 / canon §6.4). It is P2-only, behind a real `from_map` transmission the slice
    does not build.
  - **Default-safe:** if no clean measured signal, fire **only** `recordOpened()` and let the next
    in-server "it knows you" beat (the BUILT `name-where-never-been` carve, the Hold-Book) carry it. The
    frame-break is **never** traded for precision.
- **FB-3 — the knob drops.** Exactly as the in-server prologue: after the frame-break lands, loudness
  returns to baseline and never rises again in M1. The map was the loud thing; the server is patient.

**Reveal discipline holds:** nothing is witnessed mutating. The server lines are text the Watcher *says*,
in register — it does not appear, does not threaten, counts and stops.

---

## 6. ANTI-METAGAME / SPOILER AUDIT (what a cracked `.zip` reveals)

A player who opens the datapack in a text editor / NBT tool finds **only**:
- the `tellraw` strings (the FACT-1/2 public lines + the flat doused-hearth/seventh-wall texture) — all of
  which they already saw in-game;
- the conduct scoreboard logic (a `passed_seventh` flag) — no name, no personalization, no server signal;
- the server address — handed out anyway on the closing sign;
- the rune string — a recognition token, no key, no plaintext, no decode logic.

It contains **no** FACT past 1/2, **no** keeper name past the generic Archivist (Mara is on the page, not
in the map), **no** cipher key, **no** server flag, **no** Supabase URL. The real machinery is server-side
and unreachable from the client (`2f`). Cracking it spoils nothing.

---

## 7. CROSS-OWNER HOOKS + GO-LIVE RESIDUE

**Cross-owner hooks:**
- **TS-VOICE:** ONE new key, `voice.recordFrameBreak(): string`, count-callback body (verbatim §5),
  de-slopped, must pass `registerDisciplineSelfTest` (lowercase / no caps / no exclaim / no meta-word).
  Add to the `OracleVoiceKey` union **only if** emitted via a seeded `puzzles`/beat row; the ack-path
  wiring (preferred) needs no row. (Hand-off text is verbatim above — TS-VOICE inserts, does not author.)
- **TS-SHOWRUN / apply:** the on-arrival frame-break beat emits `recordFrameBreak()` **once** after
  ignition, idempotent on the BUILT `acked` one-shot guard (same shape as the in-server `recordOpened`
  ack). No new flag, no new table (`INTEGRATION-V2 §A14` — reuses `prologue_ignited` + `acked`).
- **DASH / WEB-REC:** the lure page (`CURSED-MAP-SITE.md`) links to this `.zip`; the page's
  `the-hold.zip` `href` is the GO-LIVE asset URL.
- **LORE:** the prop's `tellraw` lines draw FACT-1/2 text from `the-record-opens.md` (public half only);
  the wall texture rhymes `the-seventh-not-kept.md` without naming it. No new doc required for the prop.

**GO-LIVE residue (needs a client / asset, not buildable as text here):**
1. **Build the world + datapack** (`the-hold.zip`) in a 1.21.x client — the 6 rooms, the lectern books,
   the lever, the conduct volumes' exact coordinates (the `tick.mcfunction` region checks are
   coordinate-specific and must be filled against the built geometry). Ship the 1-room form first.
2. **Bake the real server address** into the room-5 sign/handoff function (placeholder until the server
   has a public address).
3. **Render the rune string** for `the-record-keeps` — either bundle the campaign rune resource pack
   inside the `.zip` (the one optional asset) or build block-letter runes into the room-5 wall.
4. **Host the `.zip`** at the URL the lure page's `the-hold.zip` link points to (`/the-hold/the-hold.zip`
   under the dashboard `public/` dir — which does not exist yet — or a CDN).
5. **Wire the on-arrival frame-break beat** into the BUILT ack path (TS-SHOWRUN), gated on the
   count-callback measured signal; emit `recordFrameBreak()` once.
> **Current override (2026-07-08):** the Hold handoff must not print a raw server endpoint. Use the rebuilt
> destination grammar instead: route (`record / the-record-keeps`), gate name (`SNOIKERZ`), common ending,
> and port arithmetic (`25500 + (six marked x 11) + the third room`). If older text below says to bake a
> plain server address into room 5, treat that as retired.
