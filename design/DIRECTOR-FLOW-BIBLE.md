# The Observance - Director Flow Bible

This is the operator-facing player-flow bible for the current overhaul. It is
not a lore appendix and not a task backlog. Its job is to keep the run legible
to the director: what players experience, how they know where to pull next, what
evidence each phase adds, and what live proof is still required.

Use this with:

- `design/CLUE-LEDGER.md` for clue ownership and truth state.
- `design/CUSTOMS-FIELD-GUIDE.md` for the seven ways and their six faces.
- `design/KEEPER-INVESTIGATION-DOSSIERS.md` for keeper-specific verbs, proof,
  and rehearsal failure conditions.
- `design/WREN-EVIDENCE-LOOP.md` for the companion reveal and reckoning rules.
- `design/DIRECTOR-CONCERN-CLOSURE.md` for the original concern categories,
  residual risks, and must-run rehearsal checks.
- `design/structures.md` for physical build doctrine.
- `design/RUNBOOK.md` for setup commands.
- `design/DIRECTOR-SETUP-GUIDE.md` for the start-here operator handoff.
- `tools/new_director_packet.ps1` for the current machine-readiness packet.

## Director Standard

The Observance should play like a two-week investigation inside and outside a
Minecraft server. The players should feel that the world is watching them, that
old records are incomplete, that side evidence matters, and that answers are
earned by comparison rather than guessed from a password list.

Current hard rules:

- Do not preserve content because it exists. Preserve it only if it creates
  evidence, contradiction, traversal, character, or later payoff.
- A required destination needs a pointer before it becomes required.
- A clue with a strange answer shape must teach that shape.
- A keeper site cannot be only a sign, a cipher, and an answer surface.
- Optional content must change a later reading or it should be merged/cut.
- Surface people speak like people; records are dry; journals are tired and
  specific; the Watcher is flat and exact; Iss is warm and too polished.

## Flow At A Glance

| Phase | Player experience | Primary traversal vector | Director proof |
| --- | --- | --- | --- |
| 0. Hold copy | Off-server adventure-map invitation, reconstructed endpoint | route + item + arithmetic | `check_hold_invitation.ps1` |
| 1. First server wrongness | Base/report anomaly and first human testimony | place + person + record | prologue placement proof |
| 2. Rosetta and Record | Seven way-names and the first Record route | script + route slug | `check_customs_rosetta.ps1` |
| 3. Surface people | Aro/Wenna/Coll/Dob/Pell contradict and humanize the rules | person + memory | dialogue/world audit |
| 4. Keeper field | Six evidence clusters, not six open stone solves | item/book/water/time/behavior/contradiction | `check_keeper_investigations.ps1` |
| 5. Side proof web | Human sites, markets, coops, cisterns, watch floors | place + record + route | side proof dashboard/flags |
| 6. Customs pressure | Rules become practical through consequence and proof | behavior + consequence | customs dashboard heat |
| 7. Iss/Wren catch | Warm claims fail against records and companion evidence | contradiction + person | `iss_caught`, Wren proof |
| 8. Media/recovered archive | Manual media confirms and re-frames earlier evidence | media + memory | external media readiness |
| 9. Unlit expedition | Mirrored village proves customs without surface myth | place + contradiction | `/obs unlit ready` |
| 10. Threshold/Accepting | Group action at the kept light, no typed finale shortcut | co-op + light + posture | live rehearsal proof |
| 11. Release | Ending reads from accumulated proof and choices | record + action | launch attestations |

## Phase 0 - Hold Copy

Players receive `the-hold.zip` before the server. It must not simply hand them
the IP. It now behaves as a contained invitation puzzle: route phrase, gate
name, common web ending, and port arithmetic reconstruct the destination.

Players should infer:

- There is a real server, but it is not being handed over as a normal invite.
- The Record phrase matters before they have the Record context.
- The first puzzle grammar is comparison and reconstruction, not "decode blob."

Onward vector:

- The final room points to `the-record-keeps`, `SNOIKERZ`, and the port math.
- First server arrival confirms they reconstructed something real.

Director watch:

- If a player can extract a raw endpoint from the zip, this phase fails.
- If the final room does not clearly teach what pieces must be combined, this
  becomes busywork rather than mystery.

## Phase 1 - First Server Wrongness

The first in-server beat is near the group's own base or first report site. It
should feel specific but not over-explained: a record that should not exist, a
lit marker, a small value that reflects real play, or a report lectern that
reacts to contact.

Players should infer:

- The world watches measured behavior.
- The record is local and external at the same time.
- The first lead is a physical place, not a menu.

Onward vector:

- Report surface points to Rosetta/literacy.
- Surface NPCs provide folk versions and contradictions before keeper work
  dominates.

Director watch:

- Run `/obs placeprologue`, `/obs townsfolk spawn`, and `/obs preflight`.
- Do not let the first hour become "go solve six stones."

## Phase 2 - Rosetta And Record

The Rosetta ring teaches seven way names in ring order:

`bow offering kept light deep line unspoken sacred beast`

The hint rail now says this is a list of way-names, not a sentence. The answer
shape is therefore fair. The Record route appears as a decoded route slug, not
as a coordinate.

Players should infer:

- The old script is consistent.
- The ways are seven.
- Some answers are route slugs, some are actions, and some are physical checks.

Onward vector:

- `rosetta_known` opens the literacy layer.
- `/record/the-record-keeps` establishes the website as diegetic evidence.

Director watch:

- If players ask "how would we know spacing?", the hint/build failed.
- Do not plant stale `ward` or `covering` Rosetta text.

## Phase 3 - Surface People

The surface people are not flavor. They are player memory tools.

- Aro is friendly and wrong.
- Wenna gives folk versions of real customs.
- Coll's records make economic sense of disappearances.
- Dob makes descent social and increasingly afraid.
- Old Pell states what the Watcher will not decorate.

Players should infer:

- Human testimony is useful but not automatically true.
- A confident explanation can be the lie.
- NPC lines are later evidence, not one-off ambience.

Onward vector:

- Wenna points toward customs.
- Aro points toward false versions of the Line and warmth.
- Coll/Pell/Dob point toward proof sites and descent consequences.

Director watch:

- If an NPC names a place, the place must exist and be proofable.
- If a player remembers an NPC line days later, the later answer should reward
  that memory.

## Phase 4 - Keeper Field

The keeper field is now six investigations. The old stones are supporting
receipts, not the open primary path.

| Keeper | Investigation grammar | Old stone role |
| --- | --- | --- |
| Vaun | audit containers, ledgers, first items, returned column | earned Caesar receipt after tally evidence |
| Mara | compare editions, use page/line/word, then physically walk | receipt after book/route work |
| Sella | compare water, school, cistern, reflection, seven-count evidence | old Atbash retired in favor of environmental proof |
| Orin | posture, crouch, fall-order, frame dials, sealed sightlines | substitution requires posture evidence |
| Brann | night timing, sound/toll, silence corridor, fire counts | rail/fires receipt after time/silence proof |
| Iss | compare warmth, records, NBT/forgery, Wren, contradictions | Vigenere/catch exposes the polished lie |

Players should infer:

- Each keeper failed a practical way, not a genre cipher.
- The side surfaces matter because they become evidence in a theory.
- A single solve is suspicion; a cluster becomes understanding.

Onward vector:

- Evidence clusters lock keeper theories.
- Archive cards reveal from investigation beats, not old stone rows.

Director watch:

- Use `/obs site plan keepers`, `/obs site todo`, and `/obs visit next`.
- A keeper site needs at least two non-sign clue surfaces wherever possible.
- If a keeper can be solved from a sign alone, rewrite the structure or gate.

## Phase 5 - Side Proof Web

Side sites are not filler. They give later answers weight.

Required side proof examples:

- Deep market proves ordinary life and trade.
- School stand proves they were human.
- Bird coops make sacred beast practical.
- Cistern seven makes Sella's count physical.
- Watch floor makes Brann's dark hours real.
- Third bay breach makes the Deep Line dangerous.
- Warm town collapse contradicts Aro.
- Forgotten mouth proves Iss found a way that did not save anyone.

Players should infer:

- Optional walks change the case file.
- Contradictions are intentional and later resolvable.
- The world is not only about the six keepers and finale.

Onward vector:

- Thread cards, dashboard archive state, and side proof flags tell the director
  what evidence has landed.

Director watch:

- Side content that only repeats "six kept / seventh missing" is dead weight.
- Side content should add proof, contradiction, or a later memory hook.

## Phase 6 - Customs Pressure

Customs are now controlled by `design/CUSTOMS-FIELD-GUIDE.md`. Every custom
needs six faces:

1. folk version
2. practical reason
3. physical proof
4. consequence
5. false version
6. late use

Examples:

- Bow begins as folk politeness, becomes posture/safety, and ends as consent.
- Offering begins as a cairn habit, becomes anti-hoarding evidence, and later
  colors Vaun/Orin.
- Kept light begins as household work, becomes watch evidence, and becomes
  release-room checksum.
- Deep Line begins as a warning, is lied about by Aro/Iss, and is proved by
  breach/Unlit.
- Unspoken begins as silence around a name, becomes Wren/archive evidence, and
  resolves as refusal rather than superstition.
- Sacred beast begins as animal care, becomes warning instrument.
- Dark hours begins as a night rule, becomes Brann's temporal route.

Players should infer:

- The ways are survival systems, not commandments for theme.
- Breaking a custom has atmosphere and evidence, not arbitrary punishment.

Onward vector:

- Custom reports, tolls, and the dashboard customs heat show the director what
  behavior has become story material.

Director watch:

- Do not explain customs through popup instruction.
- Let consequence, folk memory, and physical proof triangulate the rule.

## Phase 7 - Iss And Wren

Iss should feel comforting until evidence makes that comfort suspicious. Wren
should not dump exposition; Wren should become meaningful because players can
compare his behavior against prior proof and their own captured words/actions.
Use `design/WREN-EVIDENCE-LOOP.md` as the rulebook for this sequence.

Players should infer:

- Warmth can be forged.
- "The ways are a wall" is too convenient.
- A companion who reacts to lived evidence is a witness, not a quest giver.

Onward vector:

- `iss_caught` opens colder readings and late callbacks.
- Wren's reckoning choices should only land after independent proof exists.

Director watch:

- If Iss is obviously villain-coded too early, soften him.
- If Wren explains the plot without requiring comparison, rewrite.

## Phase 8 - Media And Recovered Archive

Manual media should not be decorative. Each artifact confirms or re-frames an
already-seeded clue.

| Artifact | Payload | Use |
| --- | --- | --- |
| clip 1 | `ASH-13` | prior base / record provenance |
| clip 2 | `WHERE THE REEDS FOLD BACK` | Sella/far-water confirmation |
| clip 3 | `STAY AWAKE` | Brann/dark-hours confirmation |
| clip 4 | `SIX RETURN, ONE IS NOT KEPT` | release-room checksum |
| recovered archive | `I WAS NOT KEPT` | late proof of refusal/not-kept truth |

Players should infer:

- External artifacts are evidence, not ARG garnish.
- Media confirms earlier suspicion rather than replacing in-world proof.

Onward vector:

- Media gates should only flip after the relevant setup exists.

Director watch:

- Do not plant a lure to missing media.
- Do not let media become the only path through the main spine.

## Phase 9 - Unlit Expedition

The Unlit village is the fresh expedition layer. It should feel like a real
place with wrong social physics, not an appendix dimension.

Players should infer:

- The customs survive without the surface stories.
- Some surface explanations were false, but the behaviors still matter.
- The late game is now evidence comparison, not bigger ciphers.

Onward vector:

- Unlit proof corrects earlier customs and prepares Threshold/Accepting.

Director watch:

- Run `/obs unlit audit` and `/obs unlit ready`.
- The eight evidence houses must be placed and readable.
- Extraction/retreat proof matters; avoid softlocks.

## Phase 10 - Threshold And Accepting

The finale must not be a typed password. It is a physical convergence of:

- learned posture
- kept light
- group presence
- evidence of the seventh
- the correction of Iss's lie
- the decision about release/restoration/erasure

Players should infer:

- They are not solving one last cipher. They are performing the thing the whole
  investigation taught them.

Onward vector:

- `accepting_onramp_open`, `tokens_laid`, and `bowed_as_one` control the final
  sequence.

Director watch:

- Rehearse this with real clients.
- Make sure every participant knows where to stand and what act is expected
  from prior evidence, not from an admin instruction.

## Phase 11 - Release And Debrief

Release should read as an answer to evidence. The ending can vary, but the
players must be able to trace why it happened.

Players should infer:

- Their remembered choices and evidence shaped the result.
- The seventh was not a puzzle object; the seventh was a moral case.
- The Record keeps the proof afterward.

Director watch:

- Fill launch attestations.
- Debrief out of fiction.
- Archive what confused players for the next rewrite pass.

## Director Dashboard Interpretation

The Author dashboard now has a Director State panel. Use it as the live cockpit:

- Act and active roster tell you whether the run state is plausible.
- Theory count tells you whether keeper understanding is actually landing.
- Side/media counts tell you whether optional evidence is becoming real proof.
- Open leads tell you what the group can reasonably pull next.
- Risk lights show pending/failed beats, missing Hold zip, watcher sleep, or an
  empty active roster.
- Customs heat shows which measured behaviors can become story material.

This panel is not a replacement for live observation. It is a way to keep the
director from losing the thread.

## Setup-Readiness Boundary

The repo can be setup-ready while the launch is not. Do not collapse those
states.

Repo/setup-ready means:

- audits pass
- dashboard builds
- plugin compiles/packages
- Hold/media/Record routes are internally coherent
- clue ledger, flow bible, runbook, and director packet agree
- remaining tasks are live placement, hosting, credentials, rehearsal, and
  real-client proof

Launch-ready means all of the above plus:

- hosted resource pack configured and byte-verified
- live Supabase bundle applied and attested
- all launch coordinates placed and proofed
- Unlit ready on the actual server
- live Paper/client visual proof captured
- rehearsal packet complete
- Session Zero done
- credentials rotated

If any launch-only item is incomplete, say so plainly.
