# THE HOLD — PRODUCTION INVITATION PROLOGUE

Status: built, packaged, runtime-smoked, and wired to the Copperline web trail.

This describes the artifact that was actually generated and tested. The executable source is
tools/build_hold_prologue.py, the package is dashboard/public/the-hold/the-hold.zip, and
tools/check_hold_invitation.ps1 audits the package.

## Release receipt

- Target: Minecraft Java / Paper 1.21.11, data-pack format 94.
- Player mode: adventure.
- Required mods or resource pack: none.
- Payload: deterministic command-built world; no pre-generated region files.
- Size: 17468 bytes.
- SHA1: 69d227914501508c382952706c1f154e5e71152f.
- Route envelope: x=-20..20, y=235..253, z=-28..333.
- Structure: six closed rooms, five closed progression gates, five bounded passages.
- Runtime verification: every function parsed on the repository Paper 1.21.11 server; endpoint books,
  room signs, the lever condition, and the lamp transformation were queried after load.

## Player contract

The prologue is a linear, single-player evidence walk of roughly ten to fifteen minutes. It does not
contain the live Paper address or port. It gives the player enough evidence to reconstruct the abandoned
public host, locate service 1842, identify the filing account, and follow that account to the server row.

The final reconstruction is:

- provider: COPPERLINE
- service: HOSTING
- ending: common web
- directory: 1842

The resulting place is copperlinehosting.com. The player then uses the old server list and service row
rather than receiving a raw endpoint in the map.

## Physical route

Every room is a closed deepslate shell with a real floor, four walls, and a reinforced-deepslate roof.
Passages are wide, lit, and physically bounded. There is no parkour, block placement, block breaking,
swimming, or roof escape.

All five transitions are built closed and only the matching stage trigger opens them:

| Stage | Gate plane | Open condition |
| --- | ---: | --- |
| Archive Vestibule to Domestic Hall | z=28 | approach the first record lectern |
| Domestic Hall to Reed Cistern | z=88 | inspect the empty-place register |
| Reed Cistern to Lampworks | z=150 | reach the south cistern record |
| Lampworks to Register Gallery | z=207 | pull the floor lever |
| Register Gallery to Dispatch Office | z=265 | enter the seventh-line alcove |

On reload, active players lose the started tag and safely restart at the entrance. This prevents a reload
from closing a gate around a player who still carries a later-stage tag.

## Room 1 — Archive Vestibule

Purpose: establish copying, six present hands, and the missing seventh.

- Wide, ribbed archive hall with one clear central sightline.
- Paired book walls face inward.
- Decorative shelves use ordinary bookshelf blocks.
- The two interactive chiseled shelves face toward the player and each contains a complete book.
- The first lectern faces north, toward the approaching player.
- A physically backed sign reads COPY ROOM / REGISTER 0 / 7 / DO NOT AMEND.

THE RECORD says:

1. the record is kept in more than one place; not every copy is a walkable place;
2. six hands carried this copy out; one line was left empty;
3. read the rooms in order; what repeats is true; what is missing matters.

## Room 2 — Domestic Hall

Purpose: make the six-versus-seven mismatch physical before it becomes a web clue.

- Six accessible bed alcoves, three per wall.
- Six table settings and a visibly unissued seventh place.
- Two inward-facing evidence barrels.
- A north-facing lectern on the final register dais.
- Every alcove has a full-height doorway; none is a sealed decorative box.

Evidence includes the copied place ledger, a bowl named “place seven — unissued,” and the complete book
THE EMPTY PLACE. The blank is explicitly intentional and must not be “corrected.”

## Room 3 — Reed Cistern

Purpose: repeat the count through a different medium and teach the player to check below the surface.

- Tall roofed cistern with recessed water.
- Continuous dry center bridge from entrance to exit.
- Solid rails prevent accidental falls.
- Six raised basalt posts.
- Reed beds stay off the walkable route.
- A blue-glass seventh mark is lit below the water.
- A backed sign reads VII / ANSWERS BELOW / NOT ABOVE.
- A north-facing lectern closes the room.

WHERE THE REEDS FOLD says that six posts stand on dry stone, the seventh answers only from the water,
and the player should look down before counting again.

## Room 4 — Lampworks

Purpose: provide the one deliberate mechanical interaction and make the room change in response.

- Six lit oxidized copper lamps surround a central kept fire.
- One dark hanging lamp occupies the room’s axis.
- A floor lever sits at the far station.
- A backed sign reads WAKE THE LAMP / NO HAND TENDS / DO NOT RELIGHT.
- No book or decorative control competes with the lever.

Pulling the lever extinguishes the fire bed, lights the untended lamp, opens the gate, and emits a
restrained sound and actionbar response. The tick function reads the lever block state directly. There is
no visible command block, redstone clock, or fragile item detector.

## Room 5 — Register Gallery

Purpose: make all six prior hands readable, preserve the seventh as deliberately empty, and seed the
plain phrase “the record keeps.”

- Six inward-facing chiseled shelves, each holding one complete two-page deposition.
- Three west-wall shelves face east; three east-wall shelves face west.
- The seventh north-facing shelf is deliberately empty inside an accessible alcove.
- The central lectern faces the player.

The six books describe the copied room, the repeated count, the answer below water, the kept fire and
untended lamp, provider filed apart from service, and address removed while the directory row remained.
REGISTER 0 / 7 states that the seventh line was never filed and carries the margin phrase “the record
keeps.”

## Room 6 — Dispatch Office

Purpose: turn the website transition into an evidence reconstruction rather than an out-of-fiction URL.

- Roofed account-dispatch office with a real doorway through the front partition.
- Four field-copy barrels on one evidence table.
- Separate expired-account and row-owner barrels.
- A final north-facing lectern.
- No raw domain-plus-port string, IP, Supabase URL, or Minecraft endpoint.

The four field copies are COPPERLINE, HOSTING, common web, and 1842. Secondary evidence reads
“status: expired / slots: 0 of 7” and “row owner: mkept.” HANDOFF tells the player to assemble the first
three fields as one place, open the directory number in the old server list, follow the filing hand, bring
the others to the listed address, and say kept when all are present.

## Text and container implementation

Minecraft 1.21.11 does not display the old quoted-JSON page technique as intended prose. A quoted string
containing {"text":"..."} is shown literally. Every book here instead uses structured SNBT page compounds:

~~~text
pages:[{text:"visible page text",color:"black"}]
~~~

Lectern books store minecraft:written_book_content under the Book item components map. Shelf and barrel
books use the 1.21.11 item-component form. Runtime NBT queries confirmed that Paper normalized these into
raw structured components.

The package audit enforces:

- no quoted JSON pages;
- at least fourteen complete structured written-book components;
- all five lecterns face the approaching player;
- all interactive chiseled shelves face inward;
- every interactive archive/register shelf is filled except the authored seventh blank;
- no placeholder, TODO, TBD, “shelf 3 page 1,” generated line shard, or stale command ID;
- no raw server endpoint.

## Runtime construction

The load function applies the 1.21.11 snake_case game rules, fixes time and weather, prevents mobs, sets
peaceful difficulty and a world border, temporarily force-loads the route, builds the rooms, then removes
the force-load.

The tick function starts untagged players, recovers anyone outside the route, prevents hunger from
interrupting the walk, evaluates the five ordered stages, reads the Lampworks lever, and fires the final
service-1842 title once.

No fill command exceeds the vanilla 32768-block limit. The audit rejects fill or setblock commands outside
the designed coordinate envelope.

## Rebuild and verification

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/rebuild_hold_invitation.ps1 -NoBackup
powershell -NoProfile -ExecutionPolicy Bypass -File tools/check_hold_invitation.ps1
~~~

Expected receipt:

~~~text
hold prologue check: OK - 6 rooms, 5 controlled gates, structured books, filled evidence, accessible route, bounded geometry
~~~

The release smoke log must contain no failed function load, command parse, unknown block, missing function
tag, or pack-format error.

## Web integration

The public trail remains:

1. old Copperline host surface;
2. server-list.php;
3. service 1842;
4. owner mkept;
5. the owner’s community post and plain mirror;
6. the current Paper address shown only in the directory row.

The map is served from /the-hold/the-hold.zip. Before sharing the lure, verify the custom domain serves the
release SHA1 and service 1842 contains the real address. If the address setting is unset, the row must stay
unavailable instead of inventing an endpoint.

## Reveal boundary

Cracking the zip reveals only the day-zero route: six present and one missing; copying, water, fire, and
filing motifs; “the record keeps”; Copperline destination grammar; service 1842; mkept as filing hand; and
the instruction to gather and say kept.

It does not contain the live server endpoint, a Supabase credential, later keeper answers, late-game cipher
keys, fate state, or director-only automation data.
