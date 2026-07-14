# Dialogue-World Audit

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT USE AS A DIALOGUE OR SITE AUTHORITY.** Canonical NPC text is `arc/v5/npc-dialogue.json`.

NPC dialogue is not flavor copy unless it is clearly throwaway color. If a character names a place, route, object, rule, ritual, danger, reward, or consequence, that line becomes a world contract.

The launch standard is simple: the player must be able to look at the world and believe the NPC was talking about a real thing.

## Pass Rule

Every referenced claim must end in one of five statuses:

- `KEEP`: the line is supported by visible world evidence and a working payoff.
- `BUILD`: the line is good, but the place/object/route does not exist strongly enough yet.
- `WIRE`: the world evidence exists, but the implied action has no consequence yet.
- `REWRITE`: the line is useful, but it overpromises something we are not building.
- `CUT`: the line creates noise, confusion, or a fake side quest.

No `BUILD`, `WIRE`, `REWRITE`, or `CUT` item is allowed to survive into the live ARG without a deliberate owner decision.

## What To Audit

Pull every NPC line that mentions any of these:

- A physical route: stairs, corridors, levels, doors, holes, ladders, bridges, gates.
- A landmark: lamp-house, Lamp-works, dead-stall, shelf-stall, stones, market, cistern, vault.
- A visible mark: painted line, stripe, sign, third lamp, bowing stones, dark stand.
- A rule: do not cross, keep light, do not sleep, bow, give, do not say a name.
- A consequence: people stop coming back, something follows, a reward appears, a route opens.
- A side task: deliver, light, leave, bring, count, follow, avoid, return.

For each line, record:

```text
NPC/key:
Line summary:
Referenced object/place/rule:
World proof:
Mechanic proof:
Status:
Decision:
```

## Required Tests

For every factual or directive NPC claim, answer these four questions:

1. Can a player physically find the referenced place from the context where they hear the line?
2. Is the landmark visually strong enough to be recognized without author explanation?
3. If the line suggests an action, does that action matter through a flag, beat, puzzle, scare, reward, route change, or later acknowledgement?
4. If the answer is no, is the line rewritten so it stops promising a nonexistent thing?

## Current High-Risk Lines

### The Painted Line

Claim family:

- Aro says there is a painted line across the big Stair, halfway down, past the lamp-house.
- Aro tells the player to cross it.
- Coll says the painted line is real and he does not cross it.

Launch proof required:

- There is a big Stair, not just a vague cave path.
- There is a lamp-house or Lamp-works landmark before the line.
- The line is visible enough that a player can identify it as the line from the dialogue.
- Crossing it matters. It must set a flag, trigger a beat, alter NPC responses, advance a side lane, mark the player for a scare, or become a puzzle threshold.

Current implementation hook:

- `/obs descentproof` stages `lampworks_stair`, `third_lamp_stand`, `painted_line`, and `dead_stall`.
- `PaintedLineListener` records `painted_line_crossed`, `painted_line_crossed_by`, and `painted_line_crossed_at` when a player first enters the placed line site.
- The crossing cue is deliberately private and subtle. No title-card scare, no quest banner.

If crossing it does nothing, this line is not ready. Build the consequence or rewrite the NPCs so the line is only rumor, not instruction.

### The Third Lamp

Claim family:

- Coll sends the player to the third lamp on the Lamp-works stair.
- The stand has been dark for years.
- Lighting it earns acknowledgement and a reward.

Launch proof required:

- The Lamp-works stair has countable lamp stands.
- The third lamp is visibly different, dark, broken, or marked.
- Lighting/interacting with it records completion through `coll_lamp`.
- Coll's completion dialogue and reward are wired to that proof.

Current implementation hook:

- After Coll has offered the errand, placing a light at `third_lamp_stand`, or right-clicking the stand with a light item, completes `coll_lamp`.
- Proximity alone is not enough.

### The Dead-Stall

Claim family:

- Wenna asks the player to leave crust at a shelf-stall/dead-stall.
- Wenna later knows it was done.

Launch proof required:

- The stall exists as a memorable small place, not a random shelf.
- The offering action is detectable through `wenna_crust`.
- Completion changes dialogue, flags, or route state.

Current implementation hook:

- After Wenna has offered the errand, dropping bread, wheat, or a cookie at `dead_stall` completes `wenna_crust`.
- Proximity alone is not enough.

### The Bird Coops

Claim family:

- Aro says the old bird and the coops were up at the Lamp-works.
- Wenna remembers "the bird" as one of the seven things people minded.

Launch proof required:

- The coops exist as a visible site, not just a sentence in a rumor.
- The cages are readable as empty/open, with a small left-behind trace.
- The discovery pays into the breadth layer: `dest-bird-coops`, `the_sacred_beast`, and the archive line `voice.dest.coops.find`.
- It must not become a fake "find a live bird" quest unless the live-bird vigil is deliberately built.

Current implementation hook:

- `/obs descentproof` stages `deep_bird_coops` beside the Lamp-works proof chain.
- `/obs placeworld` builds `bird_coops` sites when they are surveyed in curated placement.
- `visualaudit` and `audit` fail a placed `bird_coops` site with no visible cage bars.

Aro's bird/coops rumor resolves to `deep_bird_coops`. If the coops are absent, either build/place them or rewrite Aro so the line no longer points at a reachable destination.

### The Warm-Town Lie

Claim family:

- Aro says there is a whole warm town below, east of the Deep Market, with lamps still burning and bread in the air.
- The side destination `dest-warm-town` is explicitly the one blunt false lead in the travel set.

Launch proof required:

- The destination exists as a visible contradiction, not an empty walk or a missing cave.
- The site reads as a wrong-scale collapsed gallery with an overturned market stall, an out lamp, and no town beyond it.
- The contradiction teaches something useful: Aro lies warmly, the Deep-Line has older closures, and hands keep going to look for a place that is gone.
- The player gets a concrete warning or belief change from the site, not just "you came to the wrong place."

Current implementation hook:

- `warm_town_collapse` is a launch-required `warm_town_collapse` site.
- `/obs prepworld` and `/obs sidepass` stage `set_apart_shelf`, `undercroft_seal`, `forgotten_mouth`, `deep_market`, `ration_table`, `third_bay_breach`, and the collapsed-gallery fixture for rehearsal.
- `/obs site set warm_town_collapse`, then `/obs placeworld`, stamps the surveyed production fixture.
- `coverage`, `visit`, `visualaudit`, and `audit` include the site; `audit` fails it if the collapse rubble or WARDEN-3 notice is missing.
- The sidequest row `dest-warm-town` must retain `FALSE LEAD WITH TEETH`, and the travel spec must retain `voice.dest.warmTown.find`.

Aro's warm-town lie resolves to `warm_town_collapse`. If that collapse is absent, either build/place it or rewrite Aro so he no longer sends players on a twenty-minute false lead.

### Bowing Stones

Claim family:

- Wenna says to bow at the stones.

Launch proof required:

- The stones exist and read as a ritual threshold.
- The bowing mechanic is obvious enough to test, or another clue teaches it.
- Failure or success matters somewhere.

### Black-Moon Sleep

Claim family:

- NPCs mention staying awake when the moon goes black.

Launch proof required:

- The black-moon state is legible in the ARG schedule or world state.
- Sleeping or refusing sleep has an observable consequence.
- If it is only superstition, later content must make that uncertainty intentional.

## Static Guard

Run `tools/check_dialogue_contracts.ps1` before launch, or just run `tools/audit_all.ps1`.
This guard fails if the live surface NPC lines name one of the current high-risk contracts
without matching world/mechanic proof:

- Painted line / Stair / Lamp-works: `lampworks_stair`, `painted_line`, `PaintedLineListener`, and `painted_line_crossed`.
- Third lamp: `third_lamp_stand`, Coll's quest keys, and the light-place/touch completion hooks.
- Dead-stall: `dead_stall`, Wenna's quest keys, and the bread/wheat/cookie drop completion hook.
- Bird coops: `deep_bird_coops`, the `bird_coops` fixture, `dest-bird-coops`, and `voice.dest.coops.find`.
- Warm-town lie: `warm_town_collapse`, the collapse fixture, `dest-warm-town`, and `voice.dest.warmTown.find`.
- Far-water mirror: `the_far_water`, the far-water fixture, `dest-far-water`, `who-sella-token`, and `voice.dest.farWater.find`.
- School stand: `school_stand`, the school-stand fixture, `dest-school-stand`, `human-school-stand`, and `voice.dest.school.find`.
- Marker row: `markers_row`, the marker-row fixture, `dest-markers-row`, `happened-markers-row`, and `voice.dest.markers.find`.
- Cistern 7: `cistern_7`, the cistern fixture, `dest-cistern-7`, `place-cistern-seven`, and `voice.dest.cistern.find`.
- Watch-floor: `watch_floor`, the watch-floor fixture, `dest-watch-floor`, `surface-watch-floor`, and `voice.dest.watchFloor.find`.
- Set-apart shelf: `set_apart_shelf`, the entry-5 shelf fixture, `dest-set-apart`, `surface-set-apart`, and `voice.dest.setApart.find`.
- Undercroft seal: `undercroft_seal`, the sealed-door fixture, `dest-undercroft-seal`, `happened-undercroft-seal`, and `voice.dest.undercroftSeal.find`.
- Forgotten Mouth: `forgotten_mouth`, the way-up fixture, `dest-way-up`, `place-way-up`, and `voice.dest.wayUp.find`.
- Deep Market: `deep_market`, the market fixture, `dest-deep-market`, and `voice.dest.market.find`.
- Ration table: `ration_table`, the ration fixture, `dest-ration-table`, and `voice.dest.rationTable.find`.
- Third-bay breach: `third_bay_breach`, the broken Deep Line fixture, `dest-third-bay`, and `voice.dest.thirdBay.find`.
- Bowing stones: `bow_marker_01` and the crouch custom.
- Black-moon sleep: Dark Hours config, listener registration, bed-enter check, and `the_dark_hours`.
- Kept light: `kept_light_home_01` and the night scanner for `the_kept_light`.
- Giving back: `offering_cairn_01` and the drop custom for `the_offering`.

Passing this script does not make a scene emotionally good. It only proves the line is not a fake promise.

## Live Prep Use

Run `/obs dialogueaudit` during rehearsal and then walk the NPC lines by hand. This command is a checklist, not a parser. The real pass is still human: read the line, go to the place, do the implied action, and decide `KEEP`, `BUILD`, `WIRE`, `REWRITE`, or `CUT`.

For the current surface townsfolk pass, run `/obs descentproof` or `/obs prepworld`, then verify:

- Aro's line points to the staged Lamp-works/Stair.
- Aro's bird/coops rumor points to visible empty cages at `deep_bird_coops`.
- Aro's warm-town lie points to a visible collapsed gallery at `warm_town_collapse`, not an empty walk.
- D11's entry-five line points to a visible `set_apart_shelf` with a warm lamp counted among cold ones.
- D15's seal line points to `undercroft_seal`, including the low bow-to-read text; if players need to bow, the line must actually be low.
- D17's way-up rumor points to `forgotten_mouth`, and the return mark must matter as a route cue rather than decoration.
- Deep Market, ration table, and third-bay breach each prove a different thing: lived civic warmth, human ration grief, and the cost of treating the Deep Line as a road.
- Coll's third-lamp job points to a visibly dark third stand.
- A player crossing the painted line sets `painted_line_crossed`.
- Dropping bread at the dead-stall completes Wenna's crust quest.
- Placing or touching light at the third lamp completes Coll's lamp quest.

The goal is not to make every side line complicated. The goal is to stop fake promises. A small line can stay small, but a pointed line has to point at something.
