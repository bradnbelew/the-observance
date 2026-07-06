# Post-Unlit Visual / World-Build Overhaul

Use this as the next-session prompt after the Unlit dimension work lands.

```text
We are starting a post-Unlit visual/world-build overhaul session for The Observance Minecraft ARG.

Important sequencing:
- Another session may have just built or planned "The Unlit" dimension. First inspect the current repo/worktree and do not overwrite or contradict those changes.
- Treat The Unlit work as canon unless it is clearly broken, and integrate visual improvements around it.
- Do not implement immediately. First research, inspect, audit, then propose a build plan. After approval, implement.

Main goal:
Go over every single structure, room, glyph, visual marker, route, shrine, puzzle site, scare site, clue location, lore location, and major environmental set-piece. Improve anything that looks messy, tiny, generic, underwhelming, unclear, unpolished, visually incohesive, or non-functional.

My wants:
1. Research how to build professionally in Minecraft using code and tooling.
   - Look up current best practices for Minecraft build composition, palette use, scale, lighting, sightlines, negative space, silhouette, terrain integration, underground room design, and readable environmental storytelling.
   - Focus especially on methods that can be generated or placed through code, schematics, WorldEdit/FAWE, datapacks, plugins, or structure files.
   - Bring back actionable techniques, not vague "make it detailed" advice.

2. Identify any additional tools I should give you.
   - Tell me if you need or strongly recommend things like FAWE/WorldEdit access, Amulet, litematica/schematic files, seed/world download access, screenshots, BlueMap/Dynmap, MCA tools, model/resourcepack editors, Blockbench, or a local Minecraft test server.
   - Be specific about why each tool would help and whether it is required or optional.

3. Build larger, better, more immersive structures.
   - The builds should feel impressive and intentional, not small/stupid/weird/generic.
   - Major ARG locations should have strong silhouettes, focal points, approach routes, and enough spatial body for a group of players.
   - Rooms should have layered detail: primary shape, secondary structure, block variation, lighting, props, readable pathing, and story clues.
   - Avoid overusing signs as labels. Visuals should communicate first; text should support.

4. Make all visual clue/lore/story locations functional.
   - If a glyph matters, it must be visible, oriented correctly, and readable from the intended angle.
   - If an NPC says "down the stairs," there must be stairs, and crossing/descending them must matter.
   - If a line, threshold, altar, lamp, cairn, door, bird-coop, grave, or wall is mentioned, it must exist physically and behave consistently with the lore/mechanic.
   - Blocks that have orientation, facing, rotation, half/slab/stair states, lit/unlit states, waterlogged states, etc. must be placed correctly.
   - Clues must not be hidden by bad lighting, cramped geometry, wrong orientation, missing interaction affordance, or visual noise.

5. Prefer underground large rooms where it helps quality.
   - For large set-pieces, default to underground or interior spaces when appropriate, because it is easier to make them polished by carving/replacing stone than by making a full exterior landmark.
   - Surface structures are fine when the surface approach is important, but do not force exterior architecture if an underground chamber would look better and be easier to control.
   - Underground rooms should still have great composition: entrances, reveals, verticality, sightlines, lighting gradients, thresholds, and landmarks.

6. Keep ARG navigation fair.
   - Players should be able to find structures through clue logic, route design, landmarks, maps, NPC hints, Record/Discord clues, coordinates where appropriate, or environmental breadcrumbing.
   - Not too easy, not physically impossible.
   - Do not rely on random wandering.
   - Every required site should have at least one clear route of discovery and one recovery hint path.
   - Optional side content can be more obscure, but it still needs a reason to be noticed.

7. Integrate with current systems.
   - Inspect current plugin site config, structure templates, rehearsal commands, launch placement tools, visual audit tools, dialogue contracts, puzzle seeds, thread cards, NPC dialogue, resourcepack, datapack, and any Unlit additions.
   - Ensure structure changes do not break puzzle answers, site IDs, trigger radii, protection, teleport destinations, room swaps, dreadpass tests, or launch readiness.
   - If needed, add or improve audit tools that catch bad visuals and broken structure contracts.

8. Be willing to make major changes.
   - If a current structure/visual idea is weak, replace it.
   - If a clue location should move underground, move it.
   - If a glyph system is too subtle or ugly, redesign it.
   - If a side location does not justify its cost, cut or merge it.
   - If a better visual motif or palette is needed, propose it and apply it consistently.

Research/audit phase:
- First produce a complete visual inventory of all major build/site/glyph/structure obligations.
- Categorize each as:
  - keep/polish
  - expand
  - move underground
  - redesign
  - cut/merge
  - needs tool/access
- Identify the highest-risk visual failures for players: unclear clues, tiny builds, bad spacing, weak silhouettes, unreadable glyphs, route confusion, bad lighting, broken mechanics, missing physical proof for dialogue, and non-cohesive palettes.

Implementation phase, after approval:
- Update structure templates/schematics/site configs/docs/tools as needed.
- Prefer generated or code-backed reproducible builds where possible.
- Use FAWE/schematics/structure templates when appropriate.
- Add visual readiness checks so major sites cannot regress into generic placeholders.
- Run the full audit stack and any world-build readiness checks.
- Provide a final "visual build packet" with what changed, what still needs manual placement/proof screenshots, and what must be tested in-game.

Desired aesthetic:
- Dark, ancient, grounded, readable, ominous, tactile.
- The Observance should feel like it has a visual language: kept light, deep line, unspoken word, pale bird, record/ledger, sealed thresholds, wrong rooms, dark below.
- Horror should come from spatial implication and rules, not random clutter.
- Builds should be group-scaled, cinematic without being theme-park, and functional as ARG surfaces.

Do not lose these priorities:
- Professional Minecraft build quality.
- Code/tool-assisted reproducibility.
- Larger, more immersive, more detailed builds.
- Correct block orientation and functional clues.
- Underground chambers for large rooms where that improves quality.
- Fair ARG navigation.
- Integration with lore, puzzles, NPC dialogue, mechanics, and audits.
```

## Required Deliverable

After the inspection phase, produce a site-by-site visual bible. For every major place include:

- palette,
- scale and room shape,
- focal object,
- route in and route out,
- group capacity,
- required clue reads,
- intended emotion,
- physical interaction/body verb,
- screenshot/proof checklist,
- current verdict: `KEEP`, `POLISH`, `EXPAND`, `MOVE UNDERGROUND`, `REDESIGN`, `CUT/MERGE`, or `NEEDS TOOL`.

This prevents the visual pass from becoming vibes-only. Every major build must have a reason, a route, a read, and a proof standard.
