# Next Session: Deep Hold Redesign Handoff

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT RESUME WORK FROM THIS HANDOFF.** The redesign is represented by the V5 blueprint, manifests, and operator guide.

## Read this first

The generated Deep Hold builds through plugin versions `0.3.25`-`0.3.28` are **rejected**. Do not deploy them, polish them incrementally, or treat their layouts as a foundation. Version `0.3.28` passed automated checks but still failed the live visual review badly; those checks proved only internal coordinate consistency, not architectural quality.

The next session is a design-first rebuild. Do not write another full generator before the spatial design and a small representative prototype have been reviewed.

## Paste-ready opening instruction

> Continue from `design/NEXT-SESSION-DEEP-HOLD-REDESIGN-HANDOFF.md`. Treat Deep Hold versions 0.3.25 through 0.3.28 as rejected and do not deploy 0.3.28. Preserve all existing modified and untracked work. Do not change Supabase/database state. First audit the current Hold code and canonical lore/fixture requirements, then design a compact authored floor plan and room-by-room fixture plan. Show me the design and build one representative vertical slice for visual approval before generating the entire Hold.

## Current position

- Phase 0 of the full playtest remains incomplete.
- Strategy remains: rehearse against the real world/database, then perform a reviewed clean reset.
- IGN: `SirNan`.
- Railway has a Discord worker on `feat/build-everything-2026-07-01` and a showrunner cron on `main`.
- Known stale finale state still needs a read-only audit later.
- No Supabase/database state was changed during the rejected Hold rebuilds.
- Existing failed Holds/world placements must not be deleted or overwritten without explicit approval.
- Every new prototype or build must use a fresh location.

## Rejected build history

### `0.3.25`

Preflight failed because `mara_map_marker` had zero interior owners.

### `0.3.26`

Preflight failed because record station `court_census` had zero interior owners.

### `0.3.27`

Built, then failed live inspection. Problems included:

- stairways ending in solid walls;
- signs embedded in walls or facing into walls;
- inaccessible rooms with no doorways;
- enormous empty chambers and corridors;
- furnishings and puzzle fixtures swallowed by walls and pillars;
- disconnected or nonsensical side passages;
- rooms that did not communicate their intended function;
- exposed void, caves, water, lava, and terrain intrusion;
- scattered props instead of authored environments;
- no cohesive, believable megastructure.

### `0.3.28`

This was a compact V3 rewrite with static and runtime validations. It packaged successfully and passed those checks, but the user's live verdict was: **"absolutely sucks."** It is rejected.

Rejected artifact - do not deploy:

- `plugin/build/libs/observance-0.3.28.jar`
- SHA-1: `cbfa163038df229b15375869acc8b32c269456c6`

The deploy manifest currently points at this rejected artifact and must not be treated as an approved release manifest.

## Non-negotiable acceptance standard

The Hold must be a compact, cohesive, authored underground megastructure - not a collection of large procedural boxes.

- No unexplained giant open rooms. Condense the scale to human-scale Adventure-mode movement.
- Every room must visibly and immediately read as its purpose: food hall, library/archive, school, market, cistern, court, keeper space, and so on.
- Every intended room and route needs an obvious, usable entrance.
- A player in Adventure mode must be able to traverse every required route and interact with every required clue.
- No block, fixture, lectern, book, sign, item, gate, stair, wall, floor, or ceiling may overlap incorrectly.
- Signs must be readable, supported, correctly oriented, and never embedded in or facing a wall.
- Lecterns and books must be reachable from valid standing positions and contain the correct content.
- Gates must be visually legible, reachable, and behave according to the story state.
- Furnishings must fit their rooms intentionally. Do not place a generic fixture pattern and hope the room absorbs it.
- Rooms need density, composition, sightlines, lighting, landmarks, and environmental storytelling.
- Lore, puzzles, ciphers, records, and ARG continuity must remain correct and cohesive even if the entire physical plan changes.
- Automated checks are required but cannot certify aesthetics. Live visual approval is mandatory.

## Why the previous approach failed

The implementation optimized for coordinate ownership, manifest counts, and nominal traversability. It did not author spaces as places. Generic rectangular shells plus perimeter decoration created technically populated but visually empty rooms. Fixtures were designed separately from architecture, so placements collided with walls, pillars, routes, and each other.

Do not repeat that approach. The architecture, circulation, puzzles, furniture, lighting, and interaction positions must be designed together.

## Required next-session workflow

### 1. Preserve and audit before editing

- Read this file fully.
- Inspect `git status` and the complete relevant diff.
- Do not reset, clean, checkout, delete, or overwrite broad changes.
- Separate Hold-specific rejected work from unrelated launch/playtest work before proposing any rollback or salvage.
- Do not change database state.

### 2. Inventory immutable content

Create a definitive inventory of all canonical:

- rooms and story functions;
- gates and state transitions;
- records and record stations;
- books, pages, and lecterns;
- signs and required text;
- items, map markers, and physical clues;
- puzzles, ciphers, solutions, dependencies, and finale connections;
- NPC or player interaction positions.

For every entry, record what must remain logically identical and what may move or be visually redesigned.

### 3. Produce an authored floor plan before code

Design a compact plan with exact dimensions and coordinates for:

- room shells and ceiling heights;
- doors, thresholds, stairs, ramps, and gates;
- the main circulation loop and optional branches;
- player sightlines and orientation landmarks;
- fixtures and their interaction clearances;
- wall, floor, and ceiling thickness;
- terrain-buffer volume around the full structure;
- lighting and environmental identity.

The plan must demonstrate that every room is reachable without spectator mode and that no route terminates unintentionally.

### 4. Write a room brief for every space

Each room brief must specify:

- narrative purpose and player action;
- architectural identity and palette;
- centerpiece and secondary compositions;
- exact furniture/prop placement;
- clue, sign, lectern, book, or gate locations;
- valid approach and reading positions;
- entrances, exits, and sightlines;
- intended density and empty-space rationale;
- room-specific validation criteria.

Avoid shared generic dressing routines unless they produce truly appropriate results for that exact room.

### 5. Build one representative vertical slice

Before rebuilding the whole Hold, implement a fresh-location prototype containing:

- the surface entrance and descent;
- orientation/arrival space;
- one circulation transition;
- one richly authored functional room;
- one lore-heavy archive/lectern/sign interaction;
- one representative gate or puzzle boundary.

Package it as `0.3.29` or later only when it is ready for inspection. The user must visually approve scale, composition, density, navigation, readability, and interaction quality before full production continues.

### 6. Prefer authored templates over generic boxes

Use explicitly authored structure templates/schematics or exact room-specific block compositions. A programmatic builder may assemble approved authored modules, but it must not invent oversized shells and scatter props into them.

### 7. Validate at several levels

Checks must fail the build for any defect in:

- shell containment and terrain buffer;
- doorway and stair continuity;
- Adventure-mode reachability;
- fixture collision and support;
- readable sign orientation;
- lectern/book presence, contents, and interaction position;
- gate geometry and state behavior;
- required item/block placement;
- room ownership and manifest completeness;
- puzzle/cipher dependency correctness;
- unintended voids, openings, or dead ends.

Also add room-specific density and identity checks where objective measures are possible. Never claim those checks prove the build looks good.

### 8. Review progressively

After the vertical slice is approved, build and review the Hold in small room groups. Require screenshots and in-game inspection for each group. Do not wait until the entire megastructure is generated to discover systemic architectural failures.

## Current working tree constraints

Current branch: `codex/arg-launch-director-pass`.

The working tree contains modified files across Hold manifests, plugin code, documentation, and tooling. It also contains untracked user paths that must be preserved:

- `.codex/`
- `dashboard/public/the-hold/the-hold/`
- `design/NEXT-SESSION-FULL-PLAYTEST-HANDOFF.md`

This handoff file is also untracked until the user chooses to stage it. Do not stage, commit, push, or open a PR unless explicitly asked.

Relevant modified areas currently include:

- `design/DEEP-HOLD-*.csv`
- `plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java`
- `plugin/src/main/java/com/observance/watcher/structure/StructureTemplates.java`
- plugin version/package metadata and build scripts;
- Hold audit/layout/readiness tooling;
- launch and setup documentation.

Some changes may be worth salvaging, especially canonical content inventory and non-Hold launch work. The rejected geometry and generic room-generation strategy are not presumed salvageable.

## Out of scope until the Hold is approved

- No clean database reset.
- No database writes.
- No live finale mutation.
- No deletion of failed builds.
- No continuation of the full playtest beyond the point that depends on an acceptable Hold.

Once the Hold is visually and functionally approved, return to `design/NEXT-SESSION-FULL-PLAYTEST-HANDOFF.md`, refresh its stale version/state notes, complete the read-only finale-state audit, and resume Phase 0.
