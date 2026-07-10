# The Observance - Customs Field Guide

This is the source-of-truth guide for the seven ways. It exists because the
customs cannot be decoration. Each one must give players something to compare,
remember, test, or use later.

Use this with:

- `design/CLUE-LEDGER.md` for ownership and audit status.
- `design/structures.md` for how the proof surfaces should be built.
- `discord/supabase/seeds/thread_tags.sql` for which puzzles teach each way.
- `tools/check_customs_rosetta.ps1` for the automated customs contract.

## Customs Standard

Each custom needs six faces:

1. Folk version
2. Practical reason
3. Physical proof
4. Consequence
5. False version
6. Late use

The folk version is what a surface person would say. The practical reason is
what the custom actually does. The physical proof is what players can inspect or
perform. The consequence is what changes when the custom is honored or broken.
The false version is the convenient explanation that later evidence breaks. The
late use is how the custom matters after the group already thinks they know it.

Do not explain the custom through tutorial text. Let players collect it from a
person, a place, a behavior, a record, and a later contradiction.

## The Bow

Custom key: `the_bow`

### Folk version

People lower themselves at marked places because the deep notices pride first.
Wenna can say this warmly and half-remember it. Aro can make it sound like old
manners. Both versions are incomplete.

### Practical reason

The bow makes players change height, sightline, and speed. It turns a social
gesture into a physical reading posture. Some marks are only legible from a low
angle, and some gates only accept the group when everyone present performs the
same act.

### Physical proof

- `bow_marker_01` records crouches as real conduct.
- Orin's low sightlines and fall-order markers make the posture useful before
  it is required.
- Mara's walked route can use a low final sightline as a remembered proof, not
  a sign label.
- The Threshold floor must make the final group bow feel learned, not announced.

### Consequence

Honoring the bow can credit `TrackerConfig.CUSTOM_BOW`, open Orin/Mara evidence,
and later make the Accepting possible. Breaking it should not kill momentum, but
it can withhold clean readings, keep an answer surface quiet, or make a later
Record page describe the group as unready.

### False version

The false version is "bowing is respect." That is socially plausible and useful
early, but it hides the engineering: low marks, pressure, line-of-sight, and
group consent.

### Late use

The late use is consent. At the Accepting, the bow is no longer etiquette or a
button. It is the group choosing to lower itself together at the kept light.

## The Offering

Custom key: `the_offering`

### Folk version

The first useful thing from the deep is left behind so the way back can find
you. People may frame it as gratitude because that is easier to say than debt.

### Practical reason

The offering interrupts hoarding. It proves whether the group can return a first
thing instead of treating every discovery as loot. It also gives Vaun's failure a
measurable shape: he counted what came in and left the returned column empty.

### Physical proof

- `offering_cairn_01` receives dropped first ore or authored first-item proof.
- Vaun's hoard chest contains sorted value but missing return logic.
- Vaun's bookshelf or market ledger should show three columns: found, kept,
  returned. The returned column is the absence players notice.
- Orin's sealed niche can echo the same idea with an item-frame dial.

### Consequence

Honoring the offering can credit `TrackerConfig.CUSTOM_OFFERING`, clean up a
Vaun route, or make a Record entry stop calling the group "kept." Breaking it can
make later text colder, delay a Vaun receipt, or expose a false shortcut that
looks profitable but does not help the case.

### False version

The false version is "the deep wants payment." That turns a restraint custom
into a transaction. The real rule is about firstness and returning, not buying
safety.

### Late use

The late use is Vaun and Orin. Players should recognize that a first thing held
too long can become evidence against someone, and that returning is sometimes
the only proof that a route is not owned.

## The Kept Light

Custom key: `the_kept_light`

### Folk version

Someone keeps a lamp burning because darkness makes bad luck louder. Surface
people can treat this as household habit: practical, tiring, and expensive.

### Practical reason

Light is a checksum. It marks labor, warning, and continuity. A kept lamp is
not decoration; it is a record that someone stayed awake, maintained fuel, and
kept a route readable.

### Physical proof

- `kept_light_home_01` should make ordinary maintenance visible: fuel, ash,
  spare glass, a second lamp, or a dry household note.
- `watch_floor` should distinguish a watched light from a comfortable light.
- `nether_forge` and `unbroken_light` should show that the deepest light is
  carried forward, not owned.
- The resource pack and lighting pass must keep these surfaces readable.

### Consequence

Honoring the kept light can credit `TrackerConfig.CUSTOM_KEPT_LIGHT`, open a
safe reading, or make the Director dashboard heat useful as story material.
Neglecting it can make a clue visible but untrusted, or make a watch entry note
that the group left work undone.

### False version

The false version is "light keeps everything away." The real custom is narrower:
light records care and warning. It does not save someone who misunderstands the
line, the name, or the offering.

### Late use

The late use is the release-room checksum. The group should compare household
lamp, watch fire, forge light, and unbroken light before the finale asks them to
trust one light more than the others.

## The Deep Line

Custom key: `the_deep_line`

### Folk version

People do not cross the painted line. Some will say the deep starts there. Some
will say it is only a work boundary. Both are useful because both can be wrong
in different ways.

### Practical reason

The line is a safety system, not a slogan. It marks pressure, depth, breach
history, and the difference between entering the deep prepared and being pulled
past a threshold.

### Physical proof

- `painted_line` must be visible from approach and from the wrong side.
- `third_bay_breach` must prove that the line marks consequence, not taboo.
- Aro or Iss can give the tidy false explanation; Coll or Pell should give the
  drier evidence.
- Unlit should repeat the line without the surface myth attached.

### Consequence

Crossing or breaking below the line can credit a violation through the plugin's
deep-line tracking. The consequence should be evidence: colder messages, changed
ambient response, a withheld clean reading, or a Record note. It should not be a
cheap punishment.

### False version

The false version is "the line is a wall." Iss benefits from that. The later
truth is harsher: no wall was ever built there. The line was warning, record,
and consent boundary.

### Late use

The late use is catching Iss. When a late answer asks whether a wall protected
anyone, players should remember the painted line, the breach, the low record,
and the Unlit copy.

## The Unspoken

Custom key: `the_unspoken`

### Folk version

Certain names are not said because saying them gives the deep a handle. Most
surface people should speak around the problem rather than make a dramatic rule.

### Practical reason

Not speaking can protect someone, erase someone, or preserve a refusal. The
point is not "magic word bad." The point is that silence has politics and
evidence. Who refuses to say a name matters as much as the name.

### Physical proof

- Townsfolk dialogue should show different kinds of avoidance: fear, habit,
  guilt, and administration.
- Wren should leak against that silence only after the group has independent
  proof.
- Brann's silence corridor can make quiet physical.
- The recovered archive must turn the avoidance into evidence, not trivia.

### Consequence

Speaking the forbidden word can credit `TrackerConfig.CUSTOM_UNSPOKEN` through
the plugin and make the world answer flatly. Keeping silence should not be a
perfect virtue either; it can protect in one scene and erase in another.

### False version

The false version is "never say the name." The truer version is contextual:
some silences protect, some silences hide responsibility, and some names need to
be spoken once there is proof.

### Late use

The late use is the recovered archive and Wren evidence loop. Players should be
able to argue why the seventh was not merely missing, and why "not kept" is not
the same as "forgotten."

## The Sacred Beast

Custom key: `the_sacred_beast`

### Folk version

One watched creature is fed first and not harmed. Surface people can make this
sound like superstition or a child's attachment, which is exactly why veterans
may overlook it.

### Practical reason

The custom preserves a witness. The deep-bird pattern is a warning instrument,
a household habit, and a way to keep one small living signal outside the keeper
ledger.

### Physical proof

- `deep_bird_coops` should show differentiated care: one marked perch, one feed
  measure, one household note, and one empty place where a count should be.
- `deep_market` should show feed records beside ordinary trade.
- `sella-shore-memorial` teaches `the_sacred_beast` through a solved memory,
  not through a lecture.
- The plugin's sacred-beast tracking must stay tied to the authored marked
  witness, not generic harm.

### Consequence

Protecting the witness can make Sella's side trail legible and keep a later
fork clean. Harming the marked witness can arm a darker consequence, but only if
the build has first made the distinction fair.

### False version

The false version is "the beast is holy." That is too broad and too easy to
mock. The real reason is evidence preservation: one marked witness was kept
outside the official count.

### Late use

The late use is Sella's count and the offering trail. The group should be able
to explain why a living witness, a missing seventh count, and a returned first
thing belong in the same case file.

## The Dark Hours

Custom key: `the_dark_hours`

### Folk version

Nobody sleeps on black-moon nights. People can say this like a practical
schedule, not a prophecy: there is work to do, lights to check, and sounds to
count.

### Practical reason

The dark hours are a time window. Staying awake lets players hear tolls, watch
light states change, and compare what happens when the server clock matters.

### Physical proof

- `watch_floor` should have logs, light states, and a reason to remain present.
- `brann_toll_tower` should make sound count and timing matter.
- Clip 3 with `STAY AWAKE` confirms an already-seeded behavior.
- The dark-hours listener records sleeping during the taboo phase as conduct.

### Consequence

Staying awake can keep Brann's route clean and let the group hear the right
toll. Sleeping can credit `TrackerConfig.CUSTOM_DARK_HOURS`, alter later Record
language, or deny an easy version of the watch-floor proof.

### False version

The false version is "sleeping invites punishment." The real reason is
attention. If everyone sleeps, no one can keep light, hear tolls, or witness the
change.

### Late use

The late use is Brann and the media clip. `STAY AWAKE` should feel like a
confirmation of something the group already tested, not a random instruction.

## Cross-Custom Use

The seven ways should not feel like seven passwords. They are investigative
tools:

- Bow teaches body position and later consent.
- Offering teaches firstness, return, and anti-hoarding.
- Kept light teaches labor, watch, and proof by continuity.
- Deep line teaches boundaries that can be lied about.
- Unspoken teaches the difference between protection and erasure.
- Sacred beast teaches preserved witness evidence.
- Dark hours teaches time as a route condition.

Late puzzles may combine customs, but only when each custom has already been
encountered through at least two surfaces. A combined answer is fair when the
players can point to the earlier person, place, behavior, or record that taught
each piece.
