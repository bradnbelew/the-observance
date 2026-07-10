# The Observance - Wren Evidence Loop

This is the source-of-truth guide for Wren as a companion, witness, and leak.
It exists to stop Wren from becoming a quest giver who explains the plot.

Use this with:

- `design/CLUE-LEDGER.md` row `wren-evidence-loop`.
- `design/KEEPER-INVESTIGATION-DOSSIERS.md` for the Iss catch.
- `design/CUSTOMS-FIELD-GUIDE.md` for the Unspoken.
- `tools/check_companion_arc_contracts.ps1` for the automated companion guard.

## Wren Standard

Wren is useful only when players can compare him to independent evidence. His
lines may be warm, evasive, frightened, or guilty, but they must not solve the
case by themselves.

Every Wren beat must have:

1. Independent proof before the reveal.
2. A player memory or action Wren can react to.
3. A reason Wren benefits from staying close.
4. A contradiction the group can name.
5. A reckoning choice after the reveal.
6. A finale callback that reflects the choice without erasing player agency.

If a Wren line tells players what to believe without a prior surface they can
cite, rewrite the line or gate it later.

## Beat 1 - Trust As Access

Wren enters as practical company, not as a lore source. He should sound like
someone trying to be useful in a bad place: route caution, spare supplies, fear
of being left, and small favors.

Player evidence:

- Wren appears as a body or interaction target, not a website narrator.
- `WrenNpcListener` sets `companion_introduced` and increments
  `companion_trust`.
- Trust is group-scoped and capped, so repeated clicking cannot become a hidden
  grind.

Director note:

At this stage Wren should make players like having him nearby. The suspicious
part is not his tone. The suspicious part is that the world seems sharper when
he is close.

## Beat 2 - Memory As Bait

Wren can ask what the group would never do, where they are headed, or where they
would run if it got bad. These should feel like nervous questions in the moment.
Later, they become evidence that he has been feeding the Watcher usable shape.

Player evidence:

- Wren asks for personal or group direction, not passwords.
- The Observer can later echo behavior or statements through existing report and
  sharp-quote systems.
- The director dashboard and event log show companion openings as `companion`
  events rather than hidden magic.

Director note:

The goal is not to make a player feel tricked by another player. Wren is a group
companion and a group problem. Keep the betrayal aimed at the fiction, not at
friend distrust.

## Beat 3 - Independent Proof First

Wren's reveal only works after Iss has been caught. The group needs to learn the
pattern on a dead, documented liar before judging a living companion.

Player evidence:

- `iss_caught` gates the colder readings.
- `CompanionArcWatcher` requires `iss_caught` plus companion evidence before
  flipping `companion_revealed`.
- Iss's comfort fails against land records before Wren's comfort is judged.

Director note:

Do not let Wren confess early. Do not let him explain Iss. Let the group catch
Iss, feel clever, and then realize the same evidence grammar applies nearby.

## Beat 4 - The Reveal Is A Comparison

The reveal should not be "Wren says he did it." It should be a comparison among
his closeness, the Watcher's precision, the companion artifact, and the
Unspoken/Iss evidence the group already has.

Player evidence:

- `companion_revealed` opens `wren.reveal.yes` and `wren.reveal.tally`.
- The artifact or tally proves his channel in a physical or recorded way.
- The group can name what changed: Wren was not making the Watcher omniscient;
  he was giving it a useful edge.

Director note:

The Watcher can remain larger than Wren. Wren explains one channel, not the
whole haunting.

## Beat 5 - Reckoning Choice

After the reveal, the group gets a reckoning choice. The choice is not a morality
quiz with one correct answer. It records how the group reads his harm, fear, and
remaining personhood.

Player evidence:

- `WrenNpcListener` accepts exactly one of `reckoning_condemn`,
  `reckoning_understand`, or `reckoning_free`.
- Reckoning markers are inert until `companion_revealed`.
- Choosing twice is a no-op because the record is already written.

Director note:

The three choices should be staged as physical markers or embodied decisions,
not a menu. The group should discuss what they know, not which ending is best.

## Beat 6 - Finale Callback

The finale can remember Wren without letting Wren own the ending. His branch
colors the release and the Seventh's response, but the main proof still belongs
to the group.

Player evidence:

- Finale code reads `reckoning_free`, `reckoning_understand`, and
  `reckoning_condemn`.
- The release voice can name the cost of the choice.
- The callback should be one clause or beat, not a replacement climax.

Director note:

If players remember Wren as the whole plot, the arc has failed. If they remember
him as the intimate version of the same evidence problem, it worked.

## Hard Lines

- Wren never reveals a mandatory destination without another pointer existing.
- Wren never explains a keeper before the keeper's own evidence can be found.
- Wren never becomes an out-of-fiction hint button.
- Wren never asks players to distrust each other.
- Wren's reveal must be gated behind `iss_caught`.
- Wren's reckoning must be one-of-three and set-once.
- Wren's finale callback must reflect, not overrule, the accumulated proof.

## Rehearsal Failure

Rewrite or re-stage Wren if:

- players treat him as the answer dispenser;
- the reveal lands before they understand Iss;
- the betrayal makes players suspicious of each other instead of the fiction;
- the companion artifact is not placed or readable;
- the reckoning markers are reachable before the reveal;
- the finale talks more about Wren than about the Seventh, the release, and the
  group's proof.
