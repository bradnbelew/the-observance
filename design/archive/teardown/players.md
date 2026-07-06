# Teardown — PLAYERS dimension (experience & complexity)

Critic lens: a real veteran friend group, 4-7 people, irregular play, in voice chat, debuggers by reflex.
Read against the REAL artifacts (seeds, `voice.ts`, `repo.ts`, `sites.yml`) — not the design guides, which over-claim.
Worst first. Every finding: [VERDICT] named artifact — the precise problem — one-line fix.

---

## TIER 0 — the experience does not exist or actively misfires for players

### 1. [FIX] `getOpenPuzzles` (`discord/src/db/repo.ts:361`) ignores `requires_flags` — the ENTIRE gated story collapses into a flat wall of answers on day one
VERIFIED: `getOpenPuzzles` selects `.eq('active', true)` and never touches `requires_flags`. `matchPuzzle` then accepts ANY `active=true` row's answer. So `base-docket-reread-auto` (active=true, `requires_flags {iss_caught}`), and every back-half row that someone flips active, is answerable before the player has done anything. For the group this means: the Watcher will "reward" answers to puzzles whose setup they have not seen, payoffs land before plants, and the careful M1→M5 escalation is invisible. The group WILL notice the order makes no sense ("wait, why did it just say the muster is read, we never found Iss") and dismiss the whole thing as buggy. This is the single most experience-destroying defect.
FIX: add `requires_flags` to the select and filter `getOpenPuzzles` so a row only opens when every flag in `arc_state.flags` is truthy — the one change that makes the story have an order at all.

### 2. [FIX] The Whisper hint system (`FLOW.md §3`, `voice.ts whisperReply/whisperToll`) has voice lines but NO mechanism — the group's ONLY safety rail does not function
The hint economy is the documented answer to "hard puzzle with no signposted path." But there is no `whisper_budgets` table, no budget-spend module, no per-puzzle `hintBody` strings (GAP #24). `/whisper <puzzle>` cannot return a tiered hint because no hint bodies exist. A real friend group hits exactly ONE unsignposted cipher (Mara's book cipher, Iss's Vigenère) and stalls for a whole session; with the rail dead, they quit or demand the answer in voice chat — breaking immersion permanently.
FIX: build the budget ledger + author 2-3 `hintBody` tiers for each of the ~8 spine ciphers BEFORE first playtest; treat the hint corpus as P0, not P2.

### 3. [REDESIGN] The M0 frame-break (the literal central scare, `PLAYTHROUGH §I.3 FB-2`) cannot fire — no ignition listener writes `prologue_ignited`, and `recordFrameBreak()` is not in `voice.ts`
The "From the Fog but it knows your name" promise is delivered by exactly one moment: the group posts in `#the-record`, the server says "six were kept before you." Neither the SET listener (GAP #16) nor the voice key (GAP #15) exists. So the group's first server beat — the thing the entire prologue banks dread for — is silence. They will type `kept`, nothing happens, and conclude the bot is offline. The scare's whole architecture ("the quiet map → the server knows you" contrast) inverts into "the quiet map → the server is broken."
FIX: build the ignition setter + `recordFrameBreak()` as the literal first deliverable; nothing else in the project matters if this one beat does not land.

---

## TIER 1 — the group will get hard-stuck with no signposted path

### 4. [FIX] STONE-MARA book cipher (`puzzles_seed.sql` stone-mara) — the SPINE descent gate depends on a six-book lectern shelf the group has no reason to read as a codebook
Mara's stone carries digit-glyph triples (`1-1-1 2-1-1 ...`) that only resolve against the six-book shelf at `kept_light_home_01`. Nothing tells the group "those numbers index books." A debugger group will brute-force the sign for an hour, never connecting a house-shelf across the map to a stone. This is THE in-road to all of Movement III. With the second in-road (Brann night-beacon rail-fence) unbuilt (`§IV.A.3` SEAM) and Whispers dead (#2), this is a single point of total stall.
FIX: carve an explicit pointer on Mara's stone framing ("read the count against her shelf, book by line") AND ship the Brann second in-road, OR demote the book cipher to a Whisper-tier-1 freebie.

### 5. [SIMPLIFY] Literacy gate `rosetta_known` claims "TWO genuine in-roads" but `a1z26-tick-stave` is UNCLASSIFIED (specscheck RED) and the rune-ring icon asset is unbuilt — realistically there is ONE fragile door
`§II.2.2` sells in-road A (a1z26 tick-stave, no runes) and B (rune-ring metadata leap). A is an unclassified seed row (in the 11 that fail `specscheck`); B depends on an unbuilt server-icon asset that is "unremarked — no post, no callout." A friend group does not reverse-engineer an unremarked server icon. If A is broken/unclear, the group cannot read ANY rune in the game — every stone, every clue is gibberish. The literacy on-ramp is the highest-stakes fairness point and it rests on one unverified row.
FIX: make ONE literacy door bulletproof and explicitly signposted (a teaching-stone that walks them through it), and stop claiming two doors until the second is actually built and classified.

### 6. [FIX] The Iss catch (`no-wall-catch`, `iss-doubt`) requires the group to spontaneously "turn Iss's key on the OTHER stones" — nothing in the world tells them a key is even reusable across stones
The load-bearing mystery beat (the Liar) hinges on the group realizing each stone uses a different cipher AND that Iss's name-as-Vigenère-key, applied to a DIFFERENT stone, produces "the one who turned away." This is a leap even experienced ARG players miss. The warm misreading (`iss-warm` → a grave) is the path of least resistance; the group will happily walk to the dead-shrine, get `oracleDeadEnd`, and never doubt Iss. The catch — the emotional core of M4 — is reachable only by an insight the design assumes and never seeds.
FIX: have the cold-hearth dead-end itself plant the doubt ("the road was read true and still went nowhere — whose road was it?") so the grave PUSHES them back to re-test Iss's key, instead of relying on unprompted genius.

### 7. [FIXED] `m4-three-hands` coop gate — the tight cross-surface timing puzzle is now a forgiving held rite
Three people must, within 20 seconds, stand on a plate, carve a sign, AND post in Discord. For a group playing irregularly with one person tabbed out, this is a janky scramble that will fail repeatedly and read as a broken trigger, not a ritual. The "even 1 person with a 2nd device, slowly" fallback is a confession that the timing is too tight.
FIXED: the plugin/Discord window is now 180 seconds, the two world legs give per-leg receipts ("one hand stands.", "one hand marks.", "the square waits on the word."), and the hints now teach a held square instead of a "same short breath" QTE.

### 8. [FIXED] The Accepting group-bow (`AcceptingRiteListener`) requires EVERY present player sneaking at once on the finale floor — the listener now gives literal bow coordination feedback
The climax detector fires on synchronized sneak by all present players inside the radius. The Keeper's letter says "bow as one" but a friend group reads "bow" as flavor, not a literal simultaneous-shift mechanic. They will stand on the floor, nothing happens, and the run ends in confusion at the most important moment. The `readyGate` is also unwired (defaults always-ready) and `readActiveRoster` unwired (GAP) — so quorum behavior is untested.
FIXED: `AcceptingRiteListener` now shows per-player progress ("you are bowed. waiting on N.", "the light waits for your bow."), not-enough-roster feedback, and "not the hour." for early attempts while preserving the opaque token solve.

---

## TIER 2 — cognitive load too high / too many open threads

### 9. [SIMPLIFY] The non-linear six-stone field + Nether lane + End lane + Seventh side-quest + forks + Whisper economy ALL open at once in M2 — the group cannot hold the thread count
`§III.1` opens six stones in any order, each a different cipher, PLUS the Seventh side-quest, the prophet's wall, the forged eighth, the liar thread, two Rosettas, the Nether bearing, and the dynamic-difficulty engine — simultaneously, "the resolver ignores order." A 5-person group in voice chat tracks maybe 2-3 active threads. They will fixate on one stone, ignore the rest, and never discover that 4-of-6 fragments gates M3. The breadth that reads as "rich" to the director reads as "we don't know what we're supposed to be doing" to players.
FIX: have the Watcher's daily drip name ONE live thread at a time ("something is set out at the far water") so the field is a queue the group works, not a wall they stare at.

### 10. [CUT] 58 story-map nodes / 96 GAP markers / 24 active seed rows / 18 side-quest destinations (5 deliberate dead leads) — the surface area is multiples of what a friend group will ever touch
`side_quests.sql` ships 18 rumor→verify destinations of which FIVE are deliberate dead leads. A casual group will visit maybe 4-6 sites total across the whole run. Deliberate dead-ends (`m1-named-habit`, `forged-eighth`, `iss-dead-shrine`, prophet-wall, 5 side-quest duds) mean a large fraction of everything they DO find says "this opens nothing." A debugger group reads a string of dead-ends as "the game is broken / we're off-track" and loses confidence.
FIX: cut the dead-lead side-quests to at most 1-2, and make every dead-end carry a forward breadcrumb so "true but shut" still moves them somewhere.

### 11. [SIMPLIFY] The cold Archivist register (lowercase, no second person, no warmth, never threatens) is consistent but FLAT — a friend group needs a hook louder than "it counts and records"
Every Watcher line is deliberately affectless: "the count began before you knew there was a record to note it." This is artful but for a friend group whose attention competes with cracking jokes in voice chat, an entity that never escalates, never addresses them, never threatens, gives them little to react TO. The Set-A NPCs (Aro, Wenna, Dob) are the counterweight but are all `[GAP — GO-LIVE]` (no in-game bodies). So at launch the group meets ONLY the flat voice.
FIX: ship at least Aro + Dob's in-game bodies for launch — the human-voice contrast is what makes the cold voice land; without it the tone reads as "the bot is just logging stuff."

### 12. [FIXED] INV-14 ("the WORD answers, never the coordinate") fights player instinct everywhere — coordinate-shaped sign submissions now get a narrow course-correction
Multiple rows (`threshold-coordinate`, `true-walk-arrive`, the Nether/End on-site words) accept the destination word but explicitly reject the signed coordinate the player just decoded. A group that solves a coordinate clue will naturally answer with the coordinate, get a silent blank (no error, no hint), and conclude they solved it wrong. The design's own anti-frustration rule (blank-on-miss, no hint) compounds this into pure stall.
FIXED: `AnswerSignListener` now treats coordinate-shaped submissions as a heard attempt but returns the diegetic action-bar nudge "a place is not an answer." instead of a silent blank.

---

## TIER 3 — things players will never notice / dismiss as a bug

### 13. [CUT] The meta-acrostic UNKEPT (`meta-unkept`, six maker's-mark glyphs read in fall-order) — no friend group on earth assembles a 6-stone cross-map acrostic in a specific death-order
`§III.3` asks the group to notice six separate maker's-mark glyphs (one per stone, across the map), recall the ORDER the keepers died (from a separate lore doc), and read the glyphs in that order to spell UNKEPT — with ring-order deliberately yielding nonsense as a "self-correcting lock." This is a setter's puzzle, not a player's. It will be discovered by zero groups unspoiled. It gates nothing (good) but the authoring + glyph-carving cost (GAP) buys nothing.
FIX: cut to a single optional Whisper-revealed lore card, or have the cold Iss/Keeper simply STATE the word at the catch (already half-planned) and drop the glyph-assembly entirely.

### 14. [FIXED] The A→B room-swap (`undercroft-fog`, RoomSwapBeat) now has a private re-entry receipt instead of pure "horror by omission"
The midpoint gut-punch is: leave an ordinary altar room, return to find it rebuilt wrong. For a Minecraft group, "blocks changed when I wasn't looking" is indistinguishable from a known engine artifact (chunk reload, lighting glitch, someone griefing). The intended chill ("they did not depart, they were kept") requires the group to TRUST that the change was authored. Their reflex is `/back`, "did the server crash?", "did someone edit this?".
FIXED: the swap already uses the sealed-door + teleport-on-reentry model, and `RoomSwapReentryListener` now gives the re-entering player a private low-info receipt ("the room returns wrong.") as the transition lands. The rehearsal guard fails if that acknowledgement is removed.

### 15. [FIX] The future-dated grave (`future-dated-grave`/`grave.ts`) opens "from the inside" on the Accepting instant — but `grave.run.ts` is unbuilt, so this signature payoff silently never fires
`§V.4.1` is one of the best "oh THAT'S what that was for" payoffs (the death-date was an appointment; the hole is a deposit slot). The pure policy exists; the I/O wrapper that actually opens the grave does not (GAP #15-ish / register #1's grave columns). The group reaches the finale, the grave they saw in M2 does nothing, and the payoff they were primed for is a dud — worse than never planting it.
FIX: build `grave.run.ts` + the `arc_state` instant binding as part of the finale slice, or cut the grave plant entirely so nothing dangles.

### 16. [SIMPLIFY] The cross-surface website (`/record/[slug]` lockstep un-redaction) assumes the group keeps a browser tab open and re-checks it as stones are solved — they will not
`§III.7` / `§V.4.3` make the website un-redact six entries "in lockstep with stones actually read," with the Iss-card carrying a stego rune-layer that is a SECOND door to the Vigenère key. A friend group in a Minecraft session does not babysit a webpage. The stego second-door (GAP #27, unbuilt anyway) will never be found. The site is a beautiful artifact almost no one will witness mid-run.
FIX: have the Watcher Discord-post a direct link at the two moments it matters (ignition, finale) so the site is pushed to them, not waited-on; cut the stego second-door as a player feature (keep as video-only flavor).

### 17. [FIXED] The Unspoken word (`config.yml tracker.forbidden-words`) was empty (`[]`) — the forbidden-word custom now has a live, taught word
`The Unspoken` is sold as an always-on cheap ambient beat (say the word → lights flicker). But the word is unset (GAP #13), and even when set, nothing teaches the group there IS a forbidden word until they accidentally say it — which, being a deliberately obscure lore word, they may never do in normal chat. A custom no one triggers is a custom that does not exist.
FIXED: `tracker.forbidden-words` now ships with `unkept`, the word the keeper-field eventually teaches; Wenna's folk-charm already warns against saying the cold's name, and the rehearsal guard now fails if the list goes empty again.

### 18. [FIXED] The forks (Sacred Beast / First Light / Spoken Name) now give immediate/permanent-choice acknowledgement instead of silent ending tint
`§V.7`: killing the one glowing Beast, banking vs carrying the First Light, speaking vs not-speaking the name — each silently sets a permanent ending colorant. The group will kill a glowing animal because it's a glowing animal (loot reflex), with zero sense they just closed a boon for the whole run. At the finale the tinted clause ("the herd keeps the death-spot in its facing") reads as a non-sequitur because they never connected the act to the consequence.
FIXED: First Light and Spoken Name are explicitly authored as permanent choices in their tier-2/tier-3 hint text, and the Sacred Beast fork now gives the killer a private in-world receipt ("the warning is silenced.") the instant the one glowing fork-arming beast dies. The rehearsal guard now fails if that Beast acknowledgement is removed.

---

## TIER 4 — friction that erodes trust

### 19. [FIXED] Blank-on-miss with no error and no hint (every `keeper_stone` sign) — answer signs now acknowledge heard attempts without leaking correctness
The universal answer-verb is "edit a sign; on a miss the sign blanks, no error, no hint." A group that reflexively debugs will edit a sign, see it blank, and assume the input didn't register / the plugin choked — especially after a correct-feeling decode. Silence is indistinguishable from failure.
FIXED: on a normalized non-empty submission, the sign still blanks but fires the same deniable bass/action-bar receipt for wrong, withheld, duplicate, and solve paths; the reward beat remains the only solve tell.

### 20. [SIMPLIFY] Five divergent endings (KEPT/CAST_OUT/DIVIDED/REFUSERS/INHERITORS) decided by an active-only honored/violated tally the group never sees accumulating — the ending will feel random
`decideFate` reads `honoredActive`/`violatedActive`/`leftAtActive` etc. The group has no running sense of their "record" (the compliance tally is a director-only dashboard number, deliberately hidden). So whichever of five endings they get arrives with no felt causality — DIVIDED (the default) especially will read as "the game couldn't decide." For a group, an ending they can't trace back to their behavior is unsatisfying.
FIX: surface a diegetic, non-numeric "the column is filling" signal over the run (the Hold-Book's tone shifting warm/cold) so the ending feels earned, even without a score.

### 21. [CUT] The Nether and End lanes (`PROGRESSION-LANES.md`) add two whole Multiverse dimensions of optional content blocked on a LORE seal (FACT-11) that isn't done — for a first run with a friend group, this is scope the group will never reach
Both lanes gate nothing and are explicitly P1→P2, but they carry their own worlds, datapacks, journals, `SeventhChoiceListener` (the single largest Seventh gap), and voice keys — none built. A friend group doing a 2-week run will not light a portal looking for "below the below" without a strong push, and there is none. This is enormous build cost for content the target group is least likely to touch.
FIX: explicitly DEFER both lanes out of the first-run slice; ship the Overworld spine alone (the docs already say it reconstructs without them) and only build the Nether/End if the group asks for more.

### 22. [FIX] `specscheck` is RED (11 unclassified active rows incl. `a1z26-tick-stave`, `fork-light`, `fork-name`, `name-where`, `record-url`, `reckoning-rosetta`) — these are LIVE player-facing rows in an unverified state
Six of the eleven unclassified rows are things players directly touch in M1-M2: the literacy door, both forks, the first living-name carve, the website pointer, the digit Rosetta. An unclassified row means the cipher/answer contract is unverified — at launch these may accept nothing, accept the wrong thing, or crash the resolver. The group meets several of these in the first session.
FIX: classify and green `specscheck` before ANY playtest — these are not back-half niceties, they are the on-ramp the group walks first.

---

## IF I COULD CHANGE ONE THING

Make the world TELL the group what thread is live, one at a time. Right now the design's pride is a flat, non-linear, many-doors-open field with a cold voice that never points — and that is precisely the failure mode for a casual voice-chat friend group who will hold 2-3 threads, miss every unprompted leap, and read silence as a bug. A single change — the Watcher's daily drip names the ONE thread currently in reach, and every dead-end/miss carries a forward breadcrumb instead of silence — converts the project from "an intricate machine only the director can see working" into "a mystery a real group can actually follow." Breadth is already the strength; legibility is the entire gap. Everything else (the cold register, the cross-surface payoffs, the five endings) only pays off if the group can find the next thing to pull.
