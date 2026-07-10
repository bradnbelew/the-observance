# The Observance - Director Concern Closure

This file exists to answer the original director concerns directly. It is not a
victory lap. It is the guard against drifting back into a polished version of
the same problems: six keeper stations, irrelevant side lore, brittle answer
phrases, fantasy-gibberish copy, unclear traversal, and signboard structures.

Use this with:

- `design/DIRECTOR-FLOW-BIBLE.md`
- `design/CLUE-LEDGER.md`
- `design/CUSTOMS-FIELD-GUIDE.md`
- `design/KEEPER-INVESTIGATION-DOSSIERS.md`
- `design/WREN-EVIDENCE-LOOP.md`
- `design/structures.md`
- `outputs/observance-200-scenario-break-speedrun-audit.md`
- `outputs/observance-200-concern-closure-scenario-audit.md`

## Status

The repo is setup-ready by automated checks, but the experience is not
playtest-proven. Live Minecraft rehearsal overrides every document here. If
players experience a scene as boring, obvious, corny, linear, irrelevant, or
only solvable because the director understands it, the scene loses to the
rehearsal and must be rebuilt.

## Original Concern: Hold Hands Over The Server Too Easily

Closure standard:

- The Hold is an invitation adventure map, not an information brochure.
- The endpoint cannot appear as a raw address in books, signs, item names, zip
  file paths, or obvious extracted files.
- The final destination is reconstructed from route, gate, web ending, item, and
  port evidence.
- The first server hour must echo at least one Hold idea so the map was not
  decorative.

Residual risk:

- A player can always guess or share the endpoint socially. The story must still
  start through first report and literacy proof, not through possession of the
  address alone.

## Original Concern: Too Much Content Feels Irrelevant

Closure standard:

- Side content must change a later reading, confirm a suspicion, contradict a
  claim, open a route, or deepen a character in a way players can retell.
- If a side site only repeats "six kept, seventh missing," it is a rewrite or
  cut candidate.
- Every required side proof needs a later proof/use in `design/CLUE-LEDGER.md`.
- During rehearsal, testers must be able to answer: "what did this side site
  change about the case?"

Residual risk:

- Docs can claim a side site matters while players experience it as optional
  mood. The retelling rehearsal is the truth test.
- The Accepting gate intentionally waits on keeper theories, Unlit recoveries,
  side proofs, and surface kindnesses. If players experience that as an
  invisible checklist instead of a built case, the failure is in the surfaces,
  not in the players; add visible evidence feedback or reduce the gate.

## Original Concern: Six Keeper Stones Are Repetitive

Closure standard:

- Vaun is audit and sort.
- Mara is compare and walk.
- Sella is reflect and count.
- Orin is crouch and align.
- Brann is wait and listen.
- Iss is compare and accuse.
- The director should be able to watch play silently and identify the keeper by
  player behavior alone.
- Old stones are receipts/supporting objects, never the whole keeper scene.

Residual risk:

- Live structures can still make the six keepers feel identical if each room
  centers a stone, a book, and a terminal. Rebuild any keeper players describe
  as "the stone room."

## Original Concern: Answers Are Weirdly Formatted

Closure standard:

- Any strange answer shape must be taught before use.
- Typed answers should accept fair variants where possible.
- Major late gates should prefer physical/plugin proof over exact player text.
- A combined answer is fair only if each piece has already appeared through at
  least two surfaces.

Residual risk:

- A puzzle can pass seed checks and still feel parser-ish. Rehearsal should
  record every near-right answer and decide whether to add variants, a clearer
  surface, or a physical solve.

## Original Concern: Customs Need More To Work With

Closure standard:

Each custom needs all six faces in play, not just in docs:

1. folk version
2. practical reason
3. physical proof
4. consequence
5. false version
6. late use

The customs are local survival habits. They are not commandments and not theme
words. Players should learn them from people, work, spaces, consequences, and
later contradictions.

Residual risk:

- If customs are only mentioned in NPC dialogue or Rosetta text, they are still
  decorative. They must alter how players inspect keeper sites, Unlit, and the
  Threshold.

## Original Concern: Writing Sounds Like Fantasy Gibberish

Closure standard:

- Surface NPCs speak like people.
- Official records are dry and specific.
- Journals are tired, practical, and imperfect.
- Watcher is flat and exact.
- Iss is warm, polished, and suspiciously helpful.
- Deep inscriptions are short and strange.

Rehearsal test:

- Read important books/NPC lines aloud. If the line sounds like lore voice
  instead of a person, record, journal, Watcher, Iss, or inscription, rewrite it.

## Original Concern: Traversal Is Unclear

Closure standard:

Every required move must have at least one prior vector:

- place
- time
- behavior
- person
- record
- observer
- media
- memory
- route
- item
- consequence

Mandatory destinations should have at least two vectors by the time players must
find them. No answer may say "go there" unless the world has already taught what
kind of there it is.

Residual risk:

- A director can see traversal because they know the graph. Players need a
  physical, social, record, media, or memory reason to pull the next thread.

## Original Concern: Expeditions Could Become New Monotony

Closure standard:

The run is braided, not a chapter list. These lanes should overlap:

- survival/customs
- investigation/records
- exploration/sites
- media/archive
- social/Wren
- name/seventh
- threshold/action

Each lane needs distinct player verbs and at least one payoff that affects
another lane.

Residual risk:

- "Eight expeditions" can become "eight chapters." Rehearse legal alternate
  orders and make sure players can choose useful pulls.

## Original Concern: Structures Could Be Boring Or Broken

Closure standard:

- No major site is launch-ready if players call it a sign room.
- Every major site needs a focal object, an action beyond reading, entrance,
  exit/aftermath, readable lighting, and at least two non-sign clue surfaces
  where clue weight is major.
- Real client proof beats static docs.

Residual risk:

- A structure can pass static audit and still feel fake. Visual rescue verdicts
  must be allowed to reshape, replace, or cut scenes.

## Original Concern: Director Needs To Stay In The Loop

Closure standard:

- The flow bible explains the player experience.
- The clue ledger explains ownership and truth state.
- `/obs director state` explains open leads and setup risk.
- `/obs director progress` explains solves, wrong-answer clusters, open puzzles,
  and stuck-player hints.
- The Author dashboard mirrors state/progress without Supabase spelunking.
- The director packet summarizes receipts and live blockers.

Residual risk:

- Tools can report facts without taste. The director still needs a player
  retelling rehearsal: ask testers to explain the story as a case, not as a
  puzzle list.

## Required Rehearsals Before Launch

1. Speedrun rehearsal: fastest legal route from Hold to Threshold. Fail if it
   reaches finale without keeper theory, side proof, Wren/Iss context, media
   proof, Unlit correction, and physical convergence.
2. Side-proof retelling: after each side site, ask what it changed about the
   case. Fail if answer is "it was lore."
3. Copy read-aloud: read key NPC/book/record/Watcher/Iss lines aloud and rewrite
   lines that sound like generic fantasy.
4. Sign-room pass: any major site described as a sign room, book room, or answer
   room is reshaped before launch.
5. Traversal pass: for every required next move, name the prior vector players
   had before it became mandatory.
