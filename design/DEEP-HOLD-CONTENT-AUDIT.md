# Deep Hold Executable Content Audit

> **SUPERSEDED V4 ARCHIVE.** Current executable content is owned by the V5 node, runtime-binding,
> mechanic-predicate, book, artifact, fixture, record, NPC, and media manifests.

Status: pre-rebuild blocker register. This is derived from runtime Java, live plugin configuration,
SQL puzzle definitions, and canonical found-document payloads. It is not a claim that the current world
matches the intended design.

> **CURRENT STATUS OVERRIDE — 2026-07-13.** Deep Hold V4 supersedes the pre-rebuild defects below.
> The five Mara lock books and separate six-volume D05 shelf are fully authored; all 76 fixtures have
> explicit production builders/content; unhandled fixtures throw before world placement; and the
> isolated Paper audit reports 76/76 sites, 8/8 gates, 8/8 record surfaces, and zero critical findings.
> Keep this register as the provenance for the V4 content and fallback guards.

## Direct answer: Mara's books

The five lecterns and the page combination `1, 2, 4, 4, 6` are intentional. The current book bodies are
not final content. `fillMaraLockBook` manufactures ten near-identical pages per book using `Mara shelf N`,
`page N`, and either `this is the page her hand left open` or `the line continues elsewhere, but not here`.
Those strings expose the implementation instead of presenting a believable set of records.

There are two related, distinct Mara artifacts:

1. `mara-lectern-lock` is a five-lectern physical page-state lock. Its five marked pages are `1,2,4,4,6`.
   Correct page states must light five private confirmation lamps and open the reading alcove.
2. D05 `page-line-word` is a six-volume Kept-Light shelf cipher. Each volume contributes a referenced
   word and the assembled instruction sends the group to perform the descent and bow. It must be a separate
   authored shelf inside the opened alcove, not squeezed into the five lock books.

The D05 source itself has a content defect which must be resolved before book generation: it specifies six
page/line/word references but the answer `DESCEND AND BOW AT THE UNBROKEN LIGHT` contains seven whitespace
words. Production resolution: the sixth extracted record-word is the visibly bound compound
`UNBROKEN-LIGHT`; the shelf's instruction explains that a binding stroke joins a record-word but is spoken
as a space. The accepted answer remains the canonical seven-word phrase. This preserves six books, all six
references, and the intended destination without pretending the arithmetic already agreed.

## Sella's books and reflection

Sella's five lecterns are also intentional. They are a second page lock with target pages `2,3,5,7,11`,
activated only after the reflection bearing. The present twelve-page bodies are repeated scaffolding and
must be replaced by five distinct recovered copybook gatherings. Each gathering needs genuine drawings or
tallies in prose, one unambiguous water-ring mark on its target page, and at least one human memory that is
not puzzle instruction.

The reflection clue is authored and wired, but visual correctness is not proved by the code. The current
display is spawned under the nearest water surface and flipped 180 degrees around a fixed X axis. The rebuild
must instead bind it to a named pool surface, a dry standing zone, and a pool-to-player yaw. Acceptance is a
live-client screenshot from the standing zone showing all of the following:

- direct underwater viewing is mirror-wrong or unreadable;
- the intended water view is right-way-up, not backward, clipped, or below an opaque block;
- another player cannot see the private display;
- looking away hides it and returning within its lifetime restores it;
- the base bearing works without the Lens; the Lens adds only the optional seven-count fragment.

## Release blockers found in executable paths

- `placeHoldFixture` can send an unhandled production site to `buildLabFixture`.
- The reachable lab builder can produce books authored by `the lab`, instructions saying `turn me for
  page-lock testing`, and a generic marker for unknown types.
- Mara and Sella lectern facing is hardcoded rather than calculated from a standing zone.
- The room-shaping function returns early for Mara/Sella lecterns, making its later work-table branch
  unreachable.
- Numerous full archive/set-piece types still depend on the lab/template fallback instead of an exhaustive
  production room module.
- Existing layout checks prove table coverage and broad gate order, but do not inspect Minecraft block-state
  facing, book NBT, inventories, sightlines, room ownership boxes, or collision volumes.

## Keeper and finale audit beyond Mara/Sella

### Rosetta and Reckoning

- The live Rosetta answer names seven ways: Bow, Offering, Kept Light, Deep Line, Unspoken, and Sacred
  Beast plus the remaining taught way in the ordered ring. The current template physically makes a six-
  pillar ring and only three rune/plain crib pairs. The rebuild requires seven distinct marks and a complete
  learnable alphabet/key path; three example words cannot be treated as proof that an unknown font is fair.
- Sneak-clicking anywhere inside the Rosetta site currently ignites the prologue. That is acceptable only as
  ignition, never as proof that the player solved literacy. `rosetta_known` must still come from the answer.
- Reckoning must keep its six counted studs, disputed seventh accent, four coordinate signs, low reading
  posture, and a separate editable surface. The fixture readback must confirm all of them; a single
  chiseled anchor is not a Reckoning room.

### Vaun

- The intended object is singular and authored: `the first of the deep`. Runtime currently accepts any
  ordinary `DEEPSLATE` plus `COBBLED_DEEPSLATE`, allowing players to bring unrelated world blocks. The
  rebuilt relic needs a PDC identity and the listener must require that identity.
- The `given back` chest starts empty. The source relic remains obtainable by an adventure-mode player,
  and no hopper/service path can steal it. After a solve it either stays visibly given back or is replaced
  by a stable aftermath prop; a rebuild cannot duplicate it.
- Vaun's mechanic bookshelf currently starts empty and the target is all six slots filled. The room must
  provide exactly six removable tally volumes after `vaun_cache_open`, while decorative shelf books remain
  non-mechanic/non-removable. The mechanic shelf back touches the wall, its book face points into the aisle,
  and a readback checks both block-state occupancy and actual inventory slots.
- The tally leaf must make `all taken, none given` an investigative conclusion rather than printing a slot
  walkthrough. The cold click/door change supplies solve feedback.

### Orin

- Current marker signs disclose `fall mark`, compass bearing, and the literal dial rotation number. Replace
  them with six physically oriented maker marks whose geometry is the key. The player learns fall order from
  evidence, bows in that order, and then transfers observed direction to the dials.
- Current item frames spawn facing south; the authored player arcade stands north of them, so their fronts
  must face north. Each frame gets its own backing block/entity UUID and a seven-block separation in the
  fixture manifest.
- The dial listener currently scans every `orin_frame_dial` site globally. It must resolve exactly ids 1..6
  belonging to this Hold/lock. One additional test or world dial must not deadlock the puzzle.
- Bow progress needs subtle per-step feedback and a clear soft reset. It remains per-player unless the
  narrative explicitly changes it to a group act; Mara's group walk is a separate mechanic.

### Brann

- The black-moon producer plays one bell sound. One sound cannot carry the promised Morse `AWAKE` sequence.
  The rebuild needs a scheduled, private five-letter rhythm with tested dot/dash timing, an audible radius,
  replay/cooldown behavior, and a visual bell source that actually corresponds to the sound.
- Vanilla phase 0 is visually a full moon. If the resource pack makes it the ARG's black moon, that asset
  and phase mapping must be tested together; otherwise the prose and sky contradict each other.
- The production corridor core currently builds only local start/end dressing and no continuous sculk
  corridor. The new room owns the entire roofed passage, sensors/shriekers, service reset line, and far gate.
- The run state has no timeout or continuous corridor containment. A player can arm at start, leave while
  sneaking, and later enter the end. The listener must require a continuous in-corridor route, reset on exit,
  teleport/death/logout/world change, and produce an immediate but diegetic failure response.

### Dread Procession

- The four `dread_route` sites are not consumed by a live listener. The complete scare sequence is fired
  manually by `/obs dreadpass run`, while production Hold placement creates isolated anchor dressings rather
  than the command's continuous corridor. The rebuild needs one owned corridor and an automatic per-player
  state machine keyed to its four trigger planes; operator controls remain override/rehearsal tools only.
- Each beat must be private where appropriate, preserve the group's navigable route, and cancel cleanly on
  exit/death/logout. No operator actionbar text or named generic mob should appear in the live version.
- The Dread wing stays optional and rejoins before G7. Automatic triggers cannot teleport a player across a
  progression gate or leave a physical entity blocking the corridor.

### Iss

- Warm parlor versus cold land is visually promising and should remain: the contradiction must be readable
  before any explanatory label says Iss lied.
- The keepsake lamp's required clue is base64 text inside a custom PDC field. An ordinary adventure-mode
  player has no reliable in-game UI for reading that. F3+I/admin/NBT-viewer knowledge cannot be mandatory.
  Keep the PDC layer as optional steganography, but expose the same earned payload through an in-world Lens,
  tooltip/data-component lore change, or inspect interaction available to every intended player.
- Warm and cold submission surfaces must have separate focused radii. The cold hearth must be physically
  reachable after the false lead, and its land evidence—not a spoiler sign—must carry the contradiction.
- The found lamp chest must be persistent, non-duplicating, openable, and visibly distinct from decoration.

### Bookshelves and removable decoration

Both current chiseled-bookshelf builders apply occupied slots but never set the shelf's facing, directly
explaining the backwards book faces seen in-world. Decorative shelves also contain ordinary removable books,
so players can unknowingly change scenery or source mechanic books from any wall. The rebuild must derive
facing from the player frame before occupancy, classify each shelf as mechanic/supply/locked-decoration, and
audit both its block data and inventory. Any removable supply must have a narrative origin and an exact count.

### Threshold and cooperative vault

- The vault's player-facing combination is currently `v8k3 mq2n x6w1 t4d9 c7s5`: it looks like an opaque
  implementation token because it is one. The opaque oracle token may remain internal, but the fragments
  players see must assemble an authored diegetic phrase/code in learned runes.
- Each fragment must be private, stable while the roster is stable, visible against the intended wall, and
  not float through the player's camera or clip behind a block. A live two-, three-, and four-player pass is
  required.
- The editable vault sign is the only submission surface in its focused radius. A correct phrase with too
  few present players needs a restrained convergence response rather than indistinguishable silence.

### Accepting, Unwriting, choice, and release

- `rite-tokens` has no physical producer. Despite the story saying players lay personal tokens, it is
  currently a typed SQL phrase. The chamber needs real per-player deposit slots/ownership records and a
  listener that sets `tokens_laid` only after the active group has placed qualifying personal items.
- `AcceptingRiteListener` is registered through its legacy always-ready constructor. It can announce that
  the floor answered before `tokens_laid`, after which the SQL resolver refuses the inactive row and the
  five-minute cooldown punishes the later real attempt. Wire the live `tokens_laid`/on-ramp gate before any
  success feedback or cooldown consumption.
- Restore, erase, and release are currently three named armor stands two blocks apart. Replace them with
  three architecturally distinct, backed interaction objects: restore and erase at least six blocks apart;
  release in a later alcove that is inaccessible/inert until a choice is recorded.
- The release listener checks only `bowed_as_one`, so it can skip restore/erase and set
  `record_released`. It must also require a recorded `seventh_choice` (and whatever named/erase condition
  the chosen ending needs) before ending the world.
- Choice markers require the terminal G8 physical gate as well as DB defense-in-depth. Rebuild/readback must
  assert their PDC values, positions, labels/symbols, and one-once changed states.

### Sella memorial addendum

The memorial listener currently proves only that the player is inside `sella_anchor` and looking downward.
It does not prove the bird is in the view. The final detector must ray/angle-check the authored bird focal
point and unobstructed sightline from the standing zone; looking down at the stair or another block cannot
solve it.

### Mara route and cross-surface mechanics

- Mara route progress currently ignores an out-of-order marker rather than resetting, persists through
  detours/teleports, and can be accumulated before the page lock opens. Require continuous ordered passage
  through markers 1..4 after `mara_alcove_open`; reset on wrong marker, leaving the route envelope, teleport,
  death, logout, or timeout.
- Mara's group-bow quorum clamps against every online player. An operator or unrelated player elsewhere can
  increase the requirement without being present. Use the same live active ARG roster as the finale, then
  require the active participants who began this route (or a clearly defined present subgroup).
- The three-hands listener keys both its plate and carve to the broad `coop_plate` site type. Because
  `threshold_vault` uses that same type, interactions inside the vault can publish the global world-ready
  flag. Give the vault a distinct type or explicitly bind three-hands to site id `coop_plate`.
- The carve leg is currently any left-click on any block within the site radius, and the foot leg is any
  physical event there. Bind both to exact authored blocks/PDC roles and read those blocks back after build.
- The painted-line mechanic fires when a player enters the site's radius, not when their movement segment
  crosses the painted plane. Bind it to the actual line geometry so walking alongside it is not a crossing.

Run `python tools/audit_deep_hold_content.py` for the current report. The rebuild is not releaseable until
`python tools/audit_deep_hold_content.py --strict` passes in addition to the geometry and live-world audits.

## Production content contract

Every player-readable or player-manipulable surface is an authored fixture record. A fixture record must
contain:

- stable fixture/site id and owning room id;
- exact block/entity type and local coordinate;
- standing-zone coordinate, approach route, and derived facing;
- exact sign faces, book title/author/page array, displayed item, or container slots;
- initial and changed state, including the flag that causes the change;
- interaction radius and non-op reachability;
- whether it is required, corroborating, optional relief, or atmosphere;
- an acceptance assertion that reads the placed block state/NBT after generation.

No fixture record may use generated labels such as `shelf 3`, `page 1`, `START`, `END`, `LAB`, a site id,
or an operator instruction as diegetic prose. Page numbers may appear in the normal Minecraft book UI; they
must not be repeated as filler inside every page.

## Authoring rules for the two page locks

### Mara: five lock volumes

The volumes are five conflicting ritual editions, not numbered test books. Working titles are:

1. *The Bow, as Kept at First Snow* — marked page 1.
2. *Offerings Recorded at the Low Hearth* — marked page 2.
3. *Six Ways Down to the Kept Light* — marked page 4.
4. *The Marker Procession* — marked page 4.
5. *What Is Owed at the Last Landing* — marked page 6.

Each has ten intentionally written pages. The mark is a pressed-dark thumb margin, never a sentence saying
that it is the correct page. Read together, the marked passages describe the route as observation; the room
then forces the group to turn reading into action. The opened alcove contains the separate six-volume D05
shelf and the line `a map is not a road walked` on a non-interactive lintel.

### Sella: five copybook gatherings

Working titles are *Near Shore*, *Rain Count*, *Bird Lessons*, *Reed Water*, and *The Far Edge*, with marked
pages `2,3,5,7,11`. They contain twelve intentionally written pages apiece. The correct pages carry distinct
concentric ring drawings/tallies; no prose labels them correct. The pool floor supplies the inner-to-outer
ordering. The books preserve Sella as a child/person through ordinary lines about lessons, birds, cold hands,
and being told not to wander; the lock clue occupies only one page in each gathering.

## Required live-world acceptance pass

Static checks cannot prove visual Minecraft rendering. After the new generator is complete, a non-op player
must traverse the entire Hold in survival/adventure permissions while an operator runs the audit. For every
room, capture entry and focal-object views and verify: closed shell, visible exit, no ownership overlap,
correct fronts, readable text, populated containers, expected book pages, correct bookshelf faces/slots,
working gates, correct private displays, no escape without building, and a return route. Any fixture that is
not visible, reachable, readable, or attributable to its room fails the room; visual plausibility cannot
override functional failure.
