# The Observance - Keeper Investigation Dossiers

> **SUPERSEDED V4 ARCHIVE.** Do not implement the dossier text below. Current V5 authority is `design/V5-KEEPER-DOSSIERS.md`.

This is the source-of-truth guide for the six keeper investigations. It exists
to keep the keeper field from collapsing back into six similar stones, six stock
ciphers, and six typed receipts.

Use this with:

- `design/CLUE-LEDGER.md` for clue ownership and audit status.
- `design/structures.md` for build shape and non-sign clue surfaces.
- `design/PUZZLE-DESIGNS.md` for puzzle mechanics.
- `discord/supabase/seeds/metapuzzle_seed.sql` for gates.
- `tools/check_keeper_investigations.ps1` for the automated keeper contract.

## Keeper Standard

Every keeper investigation must answer these questions:

1. What behavior does this keeper ask from players?
2. Which two or more non-sign surfaces teach the behavior?
3. What optional or side evidence becomes useful later?
4. What does the old stone do after the investigation is earned?
5. What answer format is fair because of the build?
6. What rehearsal failure means this keeper must be rebuilt?

The old `stone_*` sites are runtime anchors and receipts. They are not the
experience. If players can solve a keeper by reading one sign and applying one
standard cipher, the keeper is not ready.

## Vaun - Audit Of What Was Kept

Ledger row: `vaun-audit`

### Player behavior

Audit the room. Players compare containers, book entries, empty slots, and item
categories until they can say what was kept and what should have been returned.

### Non-sign surfaces

- `vaun_hoard_chest`: sorted contents with one category overfull and the return
  category absent.
- `vaun_bookshelf`: a physical register whose book positions or counts make the
  returned column legible.
- Market traces: item frames, price marks, or shelf gaps that show what was
  treated as value.

### Side evidence with weight

The Offering custom matters here because it reframes firstness. A group that
has used `offering_cairn_01` should notice that Vaun's failure is not greed in
general; it is failure to return the first thing.

### Old stone role

`stone-vaun` is a receipt after `vaun_tally_read`, not the first clue. It can
echo the audit result or confirm a route, but it cannot replace the hoard and
ledger work.

### Fair answer shape

Object/category answers are fair only after the room makes the categories
physical. If an answer depends on "first" or "returned," the player must have
seen those words or their equivalent in the register.

### Rehearsal failure

Rebuild Vaun if players ask "which items count?" without having a physical way
to decide, or if they solve it by extracting a Caesar clue from a stone.

## Mara - Editions That Disagree

Ledger row: `mara-editions`

### Player behavior

Compare editions, choose the copy that describes a real walk, then physically
walk that route. Reading alone is suspicion. Walking makes it evidence.

### Non-sign surfaces

- `mara_lectern_*`: multiple written copies with specific page, line, and note
  differences.
- `mara_map_marker`: a route marker that proves which edition has legs.
- Redstone or lamp feedback from lecterns so wrong page choices teach partial
  information instead of feeling dead.

### Side evidence with weight

Surface testimony about Mara being a reader should become incomplete in light of
the route. The important memory is that she knew the text but did not complete
the walk.

### Old stone role

`stone-mara` is a receipt after `mara_walked`. It confirms the walked edition
and can unlock a later phrase, but it must not be the first place players learn
what Mara means.

### Fair answer shape

Page-line-word or code answers are fair only when the trusted edition is earned
through physical route proof. Otherwise the same extraction is arbitrary.

### Rehearsal failure

Rebuild Mara if players can brute-force the lecterns without understanding why a
copy is trusted, or if the walk is scenic but not necessary.

## Sella - The Count That Water Refuses

Ledger row: `sella-seven-count`

### Player behavior

Compare counts across dry land, school records, reflection, cistern, and shore.
Players should be able to argue that the seventh existed before they know the
late name.

### Non-sign surfaces

- `sella_pool`: reflection or water reading that disagrees with the dry surface.
- `school_stand`: child-height or copybook evidence that normalizes seven.
- `cistern_7`: built count proof, not a line of exposition.
- `the_far_water`: route confirmation through reeds and shore geometry.

### Side evidence with weight

The sacred-beast trail and clip 2 should confirm a suspicion seeded by the water
sites. The media should not introduce the idea cold.

### Old stone role

`stone-sella` is not the main Atbash gate. If it remains, it is a marker in a
water-count case, not a standalone cipher.

### Fair answer shape

Coordinate or route answers are fair only after `stone_of_reckoning` and Sella's
water surfaces teach how place grammar works.

### Rehearsal failure

Rebuild Sella if the reflection cannot be seen on a normal client, if lighting
hides the count, or if the seventh is only stated in a book.

## Orin - Posture And Sealed Sightlines

Ledger row: `orin-posture`

### Player behavior

Bow, crouch, align, rotate, and inspect. Orin teaches that body position can be
part of literacy.

### Non-sign surfaces

- `orin_marker_*`: fall-order markers that accept crouch in sequence.
- `orin_frame_dial_*`: item-frame dials whose rotations follow the learned
  sequence.
- `the_threshold`: low lintel and underside marks that are unreadable from the
  wrong posture.

### Side evidence with weight

The Bow custom is the through-line. What begins as folk courtesy becomes a
reading posture, then group consent.

### Old stone role

`stone-orin` is a receipt after `orin_key_found`. It may confirm the mason's
identity or route key, but it cannot accept `threshold` as a shortcut.

### Fair answer shape

Behavior answers are fair because the build makes crouch and sightline visible
before the answer is needed. A text-only hint is not enough.

### Rehearsal failure

Rebuild Orin if players do not naturally crouch, if the dials do not give clear
wrong-state feedback, or if the final posture is admin-explained.

## Brann - Dark Hours And Listening

Ledger row: `brann-dark-hours`

### Player behavior

Stay awake, keep quiet, listen, count, and return under the right time
condition. Brann turns time and sound into evidence.

### Non-sign surfaces

- `watch_floor`: logs, light states, and a reason to remain present.
- `brann_toll_tower`: audible toll count or rhythm.
- `brann_corridor_start` and `brann_corridor_end`: silence traversal with clear
  start and finish.
- Media clip 3 confirms `STAY AWAKE` after the behavior is seeded.

### Side evidence with weight

Kept Light and Dark Hours meet here. Players should understand that sleep is a
failure to witness, not a moral flaw.

### Old stone role

`stone-brann` and `stone-brann-cipher` are receipts after the toll and corridor
proof. Rail or fire counting can exist, but only as a confirmation of the watch
case.

### Fair answer shape

Sound or timing answers are fair only when the tower can be heard, the dark
hours are readable, and wrong attempts still teach which condition is missing.

### Rehearsal failure

Rebuild Brann if players mute the game and lose the only path, if the time
window is too narrow, or if a stock rail cipher becomes the whole keeper.

## Iss - Comfort That Does Not Match The Land

Ledger row: `iss-forgery`

### Player behavior

Compare comfort against land. Players test Iss's warm claims against records,
structure evidence, Wren behavior, and the cold hearth until the lie has a
physical shape.

### Non-sign surfaces

- `the_cold_hearth`: doused warmth, false grave language, and effaced marker.
- `warm_town_collapse`: the place where the tidy comfort fails.
- `keeper_altar` or record surfaces: mismatch between official text and built
  evidence.
- `iss-nbt-falsified-entry`: a doctored gift or record for veteran inspection.

### Side evidence with weight

Wren should matter after Iss is caught. The companion is not a plot narrator; it
is a witness whose leaks become credible because the group has independent
proof.

### Old stone role

`stone-iss` and `stone-iss-wall` can carry the lie or the catch, but the player
must catch Iss through contradiction, not because a cipher says "Iss lies."

### Fair answer shape

Deduction answers are fair when at least two surfaces conflict in a way players
can name. Vigenere, acrostic, or NBT only work when they are framed as evidence
of forgery.

### Rehearsal failure

Rebuild Iss if he is obviously villain-coded before evidence, if the contradiction
requires reading a design doc, or if Wren explains what the land should have
proved.

## Cross-Keeper Pacing

The six investigations should feel different in hand:

- Vaun: audit and sort.
- Mara: compare and walk.
- Sella: reflect and count.
- Orin: crouch and align.
- Brann: wait and listen.
- Iss: compare and accuse.

The director should be able to watch a session and tell which keeper the group
is working on from player behavior alone. If every keeper looks like "read,
decode, type," the field is not ready.
