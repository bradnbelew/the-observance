# THE OBSERVANCE - THE UNLIT

> **SUPERSEDED V4 ARCHIVE.** Current seven-houses-plus-base-mirror authority is `design/V5-UNLIT.md`.

> Canon/implementation spec for the dark spawn-village copy. This is a build document, not a mood note:
> it defines the loop, non-linear clue structure, model requirement, anti-cheese rules, and operator work.

## Director Decision

The director-facing pillar name is **The Unlit**. The late in-world phrase is **the village unkept**.

Do not rename the existing **Unlit Deep** latch. The Unlit Deep remains the group restraint custom:
no explicit flame below the Deep Line on the black moon. The Unlit is the place that makes that law
obvious in the body. It is what a surface home looks like when the old keeping reaches upward.

The Unlit is not a combat arena, a maze, or a second linear campaign. It is a dark copy of the spawn
village, filed by the Record under the laws of the Hold. Players can enter any reachable house in any
order, and the content must survive that.

## Lore Contract

- **Deep Line:** the Unlit proves the Line was not only a depth marker. The Line was the edge of being
  kept. In the copied village, ordinary paths become lines players learn to respect with limited light.
- **Unbroken Light:** every expedition begins by borrowing light from the one fire that did not go out.
  The light is lent, never owned.
- **`unkept`:** the word should not be handed to players at entry. Early text says copy, wrong village,
  dark village, or the place without lamps. The phrase village unkept arrives only after enough houses.
- **Kept villagers / Keepers:** houses are domestic memories of the seven ways. A house is a person-scale
  proof, not a plaque.
- **Watcher:** the Watcher is not inventing a dungeon. It is showing the living their home as the Hold
  would count it.
- **Sacred Beast / deep-bird:** a missing bird is a warning, not decoration. The coop house is one of the
  most important Unlit reads because silence is the clue.
- **Customs:** every house teaches one way by changing what the player can safely do, not by repeating the
  same journal format.

## Unlock And Cadence

Unlock after `undercroft_open` is true. The first entry is a controlled operator/staged event at the
Unbroken Light or an Unlit entry site. Do not make a public Record route the first unlock.

Recommended cadence:

- first glimpse after `undercroft-fog`
- first expedition after the group has had one ordinary return to the surface
- later expeditions after keeper-theory, custom, or M4 progress turns

The Unlit has **eight house nodes**, but only four are required for the main spine: lamp, well, watch,
and base. The rest are archive, fate color, and coherence. This prevents the pillar from becoming a
brittle mandatory checklist while still making the mirrored village necessary to finish the ARG.

## Required Endgame Evidence Set

The Unlit is not optional reinforcement. Before `rite-tokens` can open, the group must already have
`accepting_onramp_open` and must recover these four house flags:

- `unlit_seen_lamp`: the borrowed-light account proves the ending light is not owned by the surface.
- `unlit_seen_well`: the reflection proves the entry copy and the surface village are answering each other.
- `unlit_seen_watch`: the dark-hours room proves Brann's law is part of the ending pressure, not flavor.
- `unlit_seen_base`: the copied village files the surface in the Record and ties player-made history to the rite.

The other four house flags can still unlock archive cards, fallback hints, and rehearsal proof. They should
feel valuable, but the ending should not require all eight unless a later director pass deliberately raises
the burden.

## Non-Linear House Graph

Players may reach any house if they spend light well. Therefore no house may say "fourth expedition" or
assume another house was read first. Each house produces:

- one **local clue** that is understandable alone
- one **cross-link** that becomes richer if another house is known
- one **fallback hint** that points to a different house type, not a numbered step
- one **operator placement proof** so the site can be checked in rehearsal
- one distinct **fixture signature** so `/obs unlit audit` can prove the house is not another generic
  lectern/sign stop

| House id | Surface form | Primary way | Verb | Payload | Cross-links |
|---|---|---|---|---|---|
| `unlit_house_lamp` | copied lamp/home | kept light | spend a borrowed lantern to read a cold lamp ledger | the borrowed light is lent from below | base mirror, watch house |
| `unlit_house_cairn` | storehouse/cellar | offering | leave one non-light token in a marked bowl | taking light without return is how a home goes dark | lamp house, warm house |
| `unlit_house_coop` | bird coop / garden | sacred beast | listen for missing birdsong, then find the silent perch | warning lost before danger arrives | well house, watch house |
| `unlit_house_well` | well / trough / flooded floor | reflection | read a clue only in water/reflection | the copied village has no sky of its own | coop house, base mirror |
| `unlit_house_watch` | watch post / upper room | dark hours | survive a timed no-sleep/no-idle watch while light runs low | Brann's law becomes a surface room | lamp house, threshold |
| `unlit_house_warm` | warm-looking house | deep line / Iss | refuse a too-bright false route; find the cold back room | the false way up can be built inside home | cairn house, threshold |
| `unlit_house_threshold` | meeting house / door lintel | bow / threshold | crouch under a low lintel to extract; standing route fails | the way out is low and shared | any two houses |
| `unlit_house_base` | player-base mirror | record/surface | compare copied player-made details against the real base | the living surface is now filed | any house |

Only six to eight houses should ship. If the real spawn village has many houses, most stay dark dressing.
Do not fill every building with a clue.

## Expedition Loop

Entry:

1. After the undercroft/kept-light beat, the Record can reveal the old-well on-ramp.
2. Players stand at `unlit_entry`, ideally down in the real spawn-village well, or at `unbroken_light`.
3. Their inventory is stored.
4. They receive the kit: borrowed lanterns, one return token, and optionally a clue-safe book/map.
5. They enter `observance_unlit` at `unlit_spawn_mirror`.

Light:

- Each entrant starts with `light-budget` borrowed lanterns, default 7, echoing the seven kept.
- A borrowed lantern places an authored Unlit lamp with a small safe radius.
- Unlit lamps cannot be reclaimed.
- Spent lamps persist as cold stubs between visits, but do not keep the area safe forever.

Dark pressure:

- Outside safe light, a cold counter rises.
- Early pressure: audio, particles, actionbar, Darkness pulses.
- Late pressure: slowness/weakness and small damage.
- Failure extracts or downs the player; it should not erase progress.

Figure:

- The Unlit figure is a humanoid black model with glowing eyes. Do not ship the Unlit using a Wither
  Skeleton as the final visual.
- The final vanilla path is a plugin-assembled display-entity model: black block-display body parts with
  separate self-lit eye displays. Armor stands may be invisible anchors only if ever needed; they are not
  the visible figure.
- Behavior is authored: stalk, retreat, reposition, rush exposed lights, extinguish, vanish.
- It does not camp a light edge. If it reaches a light boundary, it leaves, circles, or attacks the light.
- It can break even player-spent borrowed lanterns. The player should understand the lantern was useful,
  temporary, and lost, not bugged or reclaimed by the system.

Exit:

- Normal extraction happens at `unlit_exit` or `unlit_house_threshold`.
- Emergency extraction consumes the return token and remaining light.
- Death restores inventory outside, records the failed expedition, and keeps discovered flags.
- Disconnect extracts safely.

Persistence:

- Persist: discovered houses, opened authored doors, cold stubs, found clues, expedition count.
- Do not persist: player-created safe light zones as permanent highways.

## Anti-Cheese Rules

Inside `observance_unlit`:

- cancel block break and block place except plugin-authored Unlit light placement
- cancel buckets, lava, water, flint/steel, fire charges, beds, boats, minecarts, elytra, chorus fruit,
  ender pearls, ladders, scaffolding, trapdoors, fence gates, redstone controls, and arbitrary light items
- allow normal wooden village doors so players can enter houses without turning the loop into a build/edit test
- cancel item drops/pickups except authored clue interactions
- prevent reclaiming special lights
- audit and scrub inherited village light sources so copied torches, lanterns, glowstone, lit candles,
  and lit campfires do not become accidental safe zones
- suppress normal mob spawns/noise unless deliberately authored
- expose `/obs unlit buildmode on|off|status` for ops/admins only; buildmode is for fixture edits and must be
  off before player-facing tests
- restore inventory on exit/death/quit
- enforce a world border around the curated duplicate; current launch tuning uses `border-radius: 138`,
  matching the tested approximately 275-wide border

## Dreadpass Test Variants

Use dreadpass before the full village.

1. `unlit_light_pass` (`/obs unlit pass light`): light budget, safe zones, darkness pressure.
2. `unlit_stalker_pass` (`/obs unlit pass stalker`): lightweight anchor-only test for model figure observe/retreat/reposition.
3. `unlit_extinguish_pass` (`/obs unlit pass extinguish`): lightweight anchor-only test where the figure attacks one exposed Unlit lamp and vanishes.
4. `unlit_house_pass` (`/obs unlit pass house`): three small house clues with different verbs.
5. `unlit_extract_pass` (`/obs unlit pass extract`): retreat, death, quit, inventory restore, and event log.

## Operator Placement Workflow

The operator should not have to hand-edit ten files during launch.

1. Build or paste the curated duplicate village in `observance_unlit`.
2. Stand at the entry in the real village well and run `/obs unlit site entry`.
3. Stand at the spawn mirror and run `/obs unlit site spawn`.
4. Stand at each chosen house focal point and run `/obs unlit clue <house-id>` to register the house and
   stamp its distinct clue fixture. Use `/obs unlit site <house-id>` only when hand-building that clue.
5. Stand at the exit and run `/obs unlit site exit`.
6. Use `/obs unlit buildmode on` while editing fixtures, then `/obs unlit buildmode off` before any playtest.
7. Run `/obs unlit darken all [radius]` to remove/dim inherited light inside the bordered village scan.
   During hand-building, `/obs unlit darken [radius]` remains available for a local anchor scrub. Both
   leave authored `unlit_safe` zones alone.
8. Run `/obs unlit border` to set the world border around the configured center/radius.
9. Run `/obs unlit audit` before inviting players. It should show placed coordinates, fixture proof for
   each house, stray light OK, and a border OK line. A placed coordinate without fixture proof is not
   launch-ready.

Required proof for each house:

- approach screenshot
- light radius screenshot
- clue readable screenshot
- exit route screenshot
- one failed-cheese attempt

## Playtest Handoff Gate

This goal has an ending. When these conditions are true, stop building and let Nano playtest:

1. `powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1` is green.
2. The curated `observance_unlit` village is pasted/built on the server.
3. `/obs unlit site entry`, `/obs unlit site spawn`, `/obs unlit site exit`, all chosen
   `/obs unlit clue <house>`, `/obs unlit buildmode off`, `/obs unlit darken all [radius]`, and
   `/obs unlit border` have been run in the live world.
4. `/obs unlit audit` reports placed coordinates, fixture proof, stray light OK, and border OK.
5. `/obs unlit ready` has been run and points the operator to the final playtest gate.
6. The live rehearsal packet contains Unlit screenshots/clips: approach, light radius, clue readable,
   exit route, and failed-cheese proof for each house.
7. `powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_unlit_playtest_ready.ps1 -PacketDir rehearsals\<date>`
   prints `unlit playtest readiness: OK`.

After that line appears, do not keep polishing. Hand the build to Nano for playtest.

## Cut Lines

- No combat win condition.
- No true same-seed automatic mirror for launch; use a curated duplicate.
- No mandatory exact expedition order.
- No house that repeats "book on lectern with a plaintext answer" as its only interaction.
- Do not ship the final Unlit using a Wither Skeleton.
- No public Record projection expansion until the archive/dashboard/Discord flow is stable.
