# Visual Rescue Pass

This is the new art-direction gate for The Observance.

The plugin can already place and rehearse a large amount of content. That is no longer the hard part. The hard part is making every visible place feel like a discovered part of the same dead civic/religious world instead of a cluster of useful Minecraft test props.

For the dedicated post-Unlit structure/glyph/world-build overhaul session, use
`design/POST-UNLIT-VISUAL-OVERHAUL.md`. That handoff preserves Ethan's full ask: research professional
Minecraft build methods using code/tools, identify any needed external tools, then audit and improve every
structure, glyph, clue site, lore site, route, and visual mechanic after the Unlit dimension lands.

## Verdict

Current generated structures are acceptable as mechanics fixtures, but they are not automatically launch-quality. A site being placed, protected, and covered by `/obs coverage` only proves that it functions. It does not prove that it looks worthy, feels cohesive, or will make players care.

From this point on, visual readiness is a separate launch gate.

NPC lines count as visual promises when they name physical things. If dialogue says there is a big Stair, a painted line, a lamp-house, or a third lamp, the visual pass must prove those things are readable before the mechanic pass asks whether they work.

## Visual Standard

Every load-bearing site must pass all seven questions:

1. Would a player stop here without being told this is important?
2. Does the silhouette read from approach distance?
3. Does the palette belong to the Deep Hold?
4. Is there one memorable focal object or spatial gesture?
5. Does the lighting communicate lore instead of merely making blocks visible?
6. Does the player's body have to do something meaningful: stoop, circle, gather, descend, look up, look down, or cross a threshold?
7. If the site accepts an answer or action, is that surface physically legible without operator explanation?

If a site fails two or more questions, it is not ready for live placement.

## Palette Law

Default materials:

- deepslate brick/tile,
- polished blackstone,
- basalt and polished basalt,
- tuff as the gradient bridge,
- oxidized/weathered copper for old civic metal,
- soul lanterns or soul fire for cold record-light,
- rare warm fire only where keeping, memory, or life still matters.

Avoid:

- ordinary cobble as a dominant texture,
- colorful blocks without a lore reason,
- flat 5x5 prop pads for major discoveries,
- exposed test signs,
- obvious route labels,
- answer-capable stones with no blank diegetic answer surface,
- symmetrical mini-shrines repeated without a keeper-specific variation.

## Scale Law

The deeper a site sits, the less human it should feel.

Upper Hold:

- readable, civic, carved, still plausibly built by people.

Middle Hold:

- stoop-to-read stones, tight thresholds, broken order, market/lamp-work remnants.

Lower Hold:

- larger voids, heavier stone, wrong-scaled supports, light that feels scarce.

Seventh/End/Unwriting material:

- unfinished edges, missing count, one clean mark among damaged stone, absence made spatial.

## Structure Disposition

Use this status during audit:

- `KEEP`: already visually strong; only needs final placement.
- `RESHAPE`: concept is right, execution needs scale/palette/lighting/depth work.
- `REPLACE`: mechanic is right, physical form is too weak; rebuild as a better template or schematic.
- `CUT`: does not earn player attention or duplicates a stronger site.

## First Audit Targets

Highest risk:

- generic keeper-stone variants that read as small prop pads,
- compact 5x5 rooms that should feel like discoveries,
- dread routes that read as flat admin hallways instead of start-gate -> pressure passage -> wrong room -> figure niche -> exit threshold,
- dread route signage that explains a scare instead of letting architecture imply it,
- any English label explaining what the architecture should imply,
- side-lore NPC rows that feel like a checklist rather than people in a place,
- optional Nether/End sites if they are too modest for their narrative weight.

Strong candidates to become hero `.schem` builds:

- the descent mouth,
- rune_rosetta,
- stone_of_reckoning,
- the_threshold,
- unbroken_light,
- the_unwriting,
- threshold_vault,
- nether_forge,
- end_seventh_shrine.

## Launch Gate

Before go-live, run a visual rehearsal pass after `/obs director lab` or `/obs director world`:

1. Visit each lane with `/obs visit next`.
2. Screenshot the approach, focal object, answer surface, and exit.
3. Mark each site `KEEP`, `RESHAPE`, `REPLACE`, or `CUT`.
4. Fill the five proof fields for every major site: silhouette, palette, lighting, body verb, and action/answer legibility.
5. Only sites marked `KEEP` are allowed into the real world placement list.

The screenshot set must prove the approach silhouette, palette, focal object, lighting, and player movement.
If the route, body verb, or exit only makes sense after an operator explains it, the site is not ready.

Record the proof in `design/LIVE-REHEARSAL-EVIDENCE.md`: every major site needs approach, focal object,
answer/action surface, and exit screenshots. A green `/obs visualaudit` means the site avoids the most
obvious test-prop failures, including missing answer surfaces on keeper-stones and answer-sign sites; it
also catches missing Deep Hold palette anchors, obvious palette clashes, and sign-heavy sites that read like
labels instead of places. It now also catches places with no focal object to inspect, no route shape or
approach vector, no gatherable body space, painted lines that are not visibly crossable, and dread beats
with no sightline or exit space. These are the mechanical versions of the concern that a build can be
functional while still feeling small, messy, or ignorable. It does not replace the screenshot decision. A completed rehearsal packet fails
if any major site remains `RESHAPE`, `REPLACE`, or `CUT`; those states mean rebuild, remove, or re-audit
before launch.

The standard is simple: if the player sees a structure and thinks "this is where the plugin wants us to go," it fails. If they think "was this always here?" it works.
