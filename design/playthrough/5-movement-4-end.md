# PLAYTHROUGH — MOVEMENT IV (Atonement, the Keeper) + the END lane

> **Director's continuity shooting-script. Literal "what is there," in strict causal order.**
> Every element is quoted from the REAL repo artifact (file + line where load-bearing). `[GAP — TO BUILD]`
> marks an artifact named by canon/seed but NOT yet authored on disk. Nothing is invented as built.
> Spoiler-bearing. Sources: `design/WEB-MASTER.md` §1.M4/§5/§7, `discord/src/voice.ts`,
> `discord/src/showrunner/keeper.ts` + `keeper-record.ts`, `discord/supabase/seeds/puzzles_seed.sql`,
> `metapuzzle_seed.sql`, `seventh_seed.sql`, `progression_seed.sql`, `plugin/.../resources/sites.yml`,
> `arc/corpus/npc-and-watcher-voice.md` SET C, `design/content/npc-dialogue.md` §7, `design/ideas/the-seventh-spine.md`.

---

## 0. THE STATE THIS SEGMENT OPENS ON (the gate into M-IV)

M-IV is the **universal hinge** (WEB-MASTER §1.M4). Everything below is keyed on the single canonical flag
**`iss_caught`**, set by the player-driven catch — NOT by the showrunner.

**THE CATCH (the row that opens M-IV) — `no-wall-catch`** (`puzzles_seed.sql` L282–305).
- WHAT IT IS: a `main_beat` cipher row. Title *"no wall was ever built here."* Re-walk the clue falsely
  marked "kept · solved"; the Stone-after contradicts Iss line for line. Accepted answers (verbatim):
  `'no wall was ever built here'`, `'they were the reaching let in'`, `'what iss sent you to was a grave'`,
  `'back to vauns stone turn down'`.
- EFFECT (verbatim payload): `voice_key: oracleMainBeat`; `set_flags {iss_caught: true, true_coord_known: true}`;
  `next_puzzle_key: rite-tokens`; beat `unlock` → `step: private_message`, `step_payload.key:
  'iss.dialogue.turns_cold'`, priority 15.
- The Watcher's main-beat line (`voice.ts` `oracleMainBeat`, L284): *"what was shut is shut no longer. the
  record keeps the hand that opened it."*
- WHERE / GATING: movement 4, `active: true`. Reached via `iss-doubt` (`next_puzzle_key: no-wall-catch`,
  L264–277). In-roads to the catch (WEB-MASTER §2 GATE table): (A) re-walk Iss's Vigenère, key = Iss name;
  (B) the stego rune-layer hands the key early; (C) `D09` hands `ISS` in-fiction.
- **NOTE — the prior `next_puzzle_key: rite-tokens` is the shortcut the activation-lane fix bans**
  (WEB-MASTER §2.1 point 2: *"`no-wall-catch` sets `iss_caught` … and stops — it does NOT carry
  `next_puzzle_key: rite-tokens`"*). The shipped seed STILL carries `next_puzzle_key: rite-tokens` at L294.
  **[GAP — TO BUILD]:** repoint/remove this so the rite is reached only through the chain. As shipped, the
  catch both opens the chain (via `requires_flags`) AND hands the rite — the F1 monorail the fix kills.

**THE ACTIVATION CASCADE `iss_caught` fires** (`metapuzzle_seed.sql` L117–130, the deterministic
`requires_flags` gate — NOT the showrunner; `getOpenPuzzles` opens a row iff every flag truthy):
- `iss_caught: true` → opens `bound-word`, `base-docket-reread`, `base-docket-reread-auto`, `meta-unkept`.
- `bound_word_known` → opens `m4-three-hands`.
- `threshold_open` → opens `threshold-coordinate`.
- `true_coord_known` → opens `true-walk-arrive`.
- `iss_caught ∧ seventh_suspected` → opens `seventh-unwriting`, `seventh-cause`.
- `seventh_named` → opens `seventh-choice`, AND (`progression_seed.sql` L221) `end-seventh-out`.

The DIRECTOR does nothing to "advance" the catch — it is player-driven. The console role in M-IV is the
**approval gate** (beats enqueue `status='approved'` on the Oracle path) and any showrunner `active`-flips,
both now backstopped by the `requires_flags` lane so a sleeping showrunner never hard-locks the back half.

---

## 1. THE KEEPER-NPC FRAMEWORK — the literal dialogue tree, branching on dossier state

> The presiding Keeper (`npc_key: keeper`, canon register 3 / corpus SET C). He presides at
> `the_threshold` and `keeper_altar` (sites.yml: `KeeperNpcListener` opens only at type `the_threshold`
> or `keeper_altar`), never at the Mouth where Set A lives. **Branch logic is `keeper.ts` (pure resolver);
> text is `voice.ts` / corpus SET C (verbatim).** No English in the Java beat.

### 1.1 The resolver's fixed precedence (`keeper.ts` `resolveKeeperDialogue`, L105–182)

Same input → same node. Ordered:
1. **Prior-keeper apparition NOT claimed by the conductor → NO node** (defer to the single-arbiter slot,
   INV-18). `if (inp.kind === 'prior' && !inp.apparitionClaimedFor) return { node: null }`. The PRESIDING
   Keeper (Citizens2 / PDC-tagged) is a fixed NPC, never slot-gated; only the ambient prior-keeper
   apparition defers.
2. **M-IV atonement** (movement ≥ 4, measured `brokenCustom`, not `atoned`) → the **withholding node**
   `keeper.atone.withheld`, `withholdsFragment: true`.
3. M-IV atonement honored (movement ≥ 4, `brokenCustom`, `atoned`) → `keeper.atone.cleared`,
   `withholdsFragment: false`.
4. **FACT 9** (a `loggedFirstBeat` exists, player is namable, AND `!fact9ShownThisWindow`) →
   `keeper.fact9.named`, `deliversFact9: true`. (P1-5: one FACT-9 surface per window. If another surface
   showed it, this node is withheld — note logged, no spotlight.)
5. **Dossier-rhymed node** — `keeper.rhyme.<id>`; if `rhymesWith === 'iss' && issCaught` → **`keeper.iss.cold`**
   (the liar node-text reads cold, same `iss_caught` flag as the activation lane).
6. **Neutral floor** — `keeper.presiding.neutral` (flat dossier, no guessed callout — precision over recall).

### 1.2 The branch table (`npc-dialogue.md` §7.1; conditions verbatim)

| node_key | voice key | branch condition | thread / fact |
|---|---|---|---|
| `greet` (neutral) | `keeper.greet.neutral` | early; nothing measured | the presider, half-veiled |
| `greet` (warm) | `keeper.greet.warm` | KEPT conduct | warmth-under-dread |
| `greet` (cold) | `keeper.greet.cold` | BROKEN conduct | grief not threat; reversible |
| `falseLaw` | `keeper.falseLaw` | `eighth_seen` | the forged eighth (FACT 7b) |
| `seventhChoice` (offer) | `keeper.seventhChoice.offer` | `seventh_named` & deep open | the Seventh (FACT 10b) |
| `seventhChoice` (restored) | `keeper.seventhChoice.restored` | `seventh_choice = restore` | INHERITORS codicil (FACT 14) |
| `seventhChoice` (erased) | `keeper.seventhChoice.erased` | `seventh_choice = erase` | the blank left |
| `becomingKeepers` (neutral/warm/cold) | `keeper.becomingKeepers.<skin>` | M5 on-ramp + skin | the rite (FACT 13/14); door to 15 |
| `endings` (kept/castOut/divided/refusers) | `keeper.endings.<fate>` | `ending_fate` (read AFTER bow) | M5 close |
| `collectiveRestraint` (kept/broken) | `keeper.collectiveRestraint.<state>` | `group_restraint_state` | the Unlit Deep |
| `deadEndTaunt` (kind) | `keeper.deadEndTaunt(kind)` | on a dead-end solve; pass row's `kind` | honest counterweight to Iss |

> Conduct skin = the §0.2 resolution (off `punishment_state` toll_tier/deciphered). `iss_caught` is **NOT**
> a Keeper-skin input — the Keeper is of the record, not Iss-adjacent. He has **no `truth_or_lie` tell**
> (the one voice that never lies; §7.3).

### 1.3 The dialogue node text the player actually hears in M-IV (verbatim)

**`keeper.greet.neutral`** (corpus C1):
> *"you came down. they came down too, the ones before you. i was nearer the front of that line than i
> tell. sit, or stand. the record keeps either. i keep the rite."*

**`keeper.greet.warm`** (C1):
> *"you kept the ways coming down. we knew the keeping when we felt it on the stair. i will not say who we
> is. you will know it, or you will not, and the not-knowing keeps you a while longer."*

**`keeper.greet.cold`** (C1):
> *"you broke a way or two coming down. i am not here to scold it. i broke one myself, late, and was kept
> anyway, or kept regardless. mind the rest of the road. there is keeping left in it for you."*

**`keeper.iss.cold`** (resolver L161; precedence step 5 when `rhymesWith==='iss' && issCaught`).
- **[GAP — TO BUILD]:** the node *key* is referenced by `keeper.ts`, but no `keeper.iss.cold` BODY is
  authored in corpus SET C or `voice.ts`. The nearest authored cold-Iss text is the surface NPC line
  `aro.greet.iss_cold` (Set A, wrong register for the Keeper). The Keeper's cold-Iss body is owed.

**`keeper.rhyme.<id>`** (resolver L161; the six prior-keeper rhymed nodes, e.g. `keeper.rhyme.vaun`).
- **[GAP — TO BUILD]:** the per-keeper rhymed node bodies are not authored as `keeper.rhyme.*` strings.
  (The Hold-Book `keeperPageHeading_*` lines exist — different surface; the dialogue rhyme bodies are owed.)

**`keeper.presiding.neutral`** (resolver L177, the floor).
- **[GAP — TO BUILD]:** the resolver returns key `keeper.presiding.neutral`; the authored floor body is the
  idea-file `keeper.greet.deniable` (`backlog-keeper-npc-framework.md` §3): *"you came to the stone. i am
  here, the way i am always here. ask the rite when you have a thing to ask it. the record keeps the asking
  and the not-asking the same."* — but it is keyed `greet.deniable`, not `presiding.neutral`. The key/text
  binding is owed (rename or author).

---

## 2. THE M-IV ATONEMENT GATE — withhold a fragment until a broken custom is honored

> The signature M-IV mechanic. A fragment the group needs is **refused** until a genuinely-measured broken
> custom is honored. Conduct is the lock, the fragment the key. Two surfaces move in lockstep: the **puzzle
> row** (`atonement-refrain`) and the **dialogue node** (`keeper.atone.*`).

### 2.1 The puzzle row — `atonement-refrain` (`puzzles_seed.sql` L417–439)

- WHAT IT IS: `main_beat`. Title *"the keepers turn."* Accepted answers (verbatim): `'the keepers turn'`,
  `'conduct is the lock the fragment is the key'`, `'honor the broken custom and return'`.
- EFFECT (verbatim): `voice_key: oracleMainBeat`; `set_flags {atonement_made: true}`;
  `next_puzzle_key: rite-tokens`; beat `unlock` → `step: reveal`,
  `step_payload.fragment: 'keeper_withheld_returned'`, priority 12.
- WHO / GATING: movement 4, `active: true`. The act it gates on: the player must have honored the
  previously-broken custom (the listener path that sets `punishment_state.deciphered=true` for that custom),
  then return to the Keeper.

### 2.2 The dialogue half — `keeper.atone.withheld` (refusal) → `keeper.atone.cleared` (release)

Resolver step 2/3 (`keeper.ts` L116–137). The atonement node **names the conduct, never the player**
(`keeper.ts` precision note L29).

**`keeper.atone.withheld`** (corpus / `backlog-keeper-npc-framework.md` §3 — the M-IV refusal):
> *"the one whose mark you wear coming down kept the offering. you did not, the once it was asked. she will
> hand you nothing until the deep has had its first ore back from you. go up. give it. come down. the hand
> opens to a hand that gave."*

**`keeper.atone.cleared`** (after the custom is honored — DE-SLOPPED per slop E3; the named feeling
*"she felt the weight leave the seam"* must be struck):
- Authored draft (`backlog-keeper-npc-framework.md` §3, pre-deslop): *"you gave it back. she felt the weight
  leave the seam. the fragment is yours now. it was always yours; it was only held."*
- **[GAP — TO BUILD]:** the de-slopped final body is owed (COHERENCE-AUDIT-V2 slop E3 / BUILD-MANIFEST §E3:
  *"strike the named feeling in `keeper.atone.cleared`"*). Strike *"she felt the weight leave the seam";*
  let the cleared act stand. Final string not yet on disk.

### 2.3 FACT 9 at the same window — `keeper.fact9.named` (the offline-haunt named)

Resolver step 4. Binds the logged M-I haunt (read from `event_log`) to the keeper whose fate re-enacted it.
Spoken ONCE, human-approved; one surface per window (P1-5).

**`keeper.fact9.named`** (DE-SLOPPED per slop E4 — the *"you did not, then. you are learning it now"* bow
must be cut):
- Authored draft (`backlog-keeper-npc-framework.md` §3, pre-deslop): *"the cold that stood in your doorway
  the first week wore a walking that stopped at night. brann walked it first. the land kept the walking
  after him and set it at your door to see if you would learn the hour. you did not, then. you are learning
  it now."*
- **[GAP — TO BUILD]:** the de-slopped final body is owed (BUILD-MANIFEST §E4: *"cut the bow … the record
  states, never reassures"*). Cut the closing then/now couplet.
- The standalone Watcher twin lore row is shipped: `haunting-biography` (`puzzles_seed.sql` L399–413), lore,
  fragment: *"the first hauntings were not random. they were one keepers fate, re-enacted at your door. the
  dread had a biography."*

### 2.4 The Hold-Book M-IV face (the keeper's OWN hand writes the living player)

The atonement window co-occurs with the Hold-Book reaching its **`keeper_hand` tier** (M4). Producer:
`keeper-record.ts` `decideKeeperEnrolment` (L165–222), precision-floored (`minLeadScore 0.45`,
`minLeadMargin 0.15`; a flat dossier stays an un-headed living row — INV-16). The keeper-hand bodies
(`voice.ts` L395–431) are the deterministic fallback behind the optional LLM scalpel (`resolveAuthorClause`,
the `*.clause` slot). Verbatim, per fingerprint:

- `keeperPageHand_vaun` (L396): *"i, vaun, write the one called {name} into my column, and they are mine to
  enter, and i do not strike the column, i am instructed not to strike it."*
- `keeperPageHand_mara` (L403): *"i, mara, write that the one called {name} read the rite i read, on the
  page i read it, and went down the way i never went."*
- `keeperPageHand_sella` (L409): *"i, sella, write the one called {name} at the shore, and the shore writes
  them back at me, smaller, where the water keeps the ones it kept."*
- `keeperPageHand_orin` (L415): *"i, orin, set the one called {name} at the threshold and meant to cut the
  rest and the rest is not —"*
- `keeperPageHand_brann` (L421): *"i, brann, write the one called {name} into the watch, into the watch, and
  the hand wrote them twice and did not remember writing it once."*
- `keeperPageHand_iss` (L427): *"i write the one called {name} and i tell them they are kept, and i do not
  count them, and the not-counting is the lie i was caught in."*

---

## 3. THE CASCADE THAT RE-READS COLD AT THE CATCH (M-IV density)

In strict order of how a group meets them once `iss_caught` opens them.

### 3.1 `bound-word` — the Iss plaintext IS the coop-gate's need (`puzzles_seed.sql` L802–815)
- WHAT IT IS: `next_clue`. Title *"the word the catch yields."* Accepted: `'the one who turned away'`,
  `'turned away'`, `'the bound word is his name'`.
- EFFECT: `next_puzzle_key: m4-three-hands`; `set_flags {bound_word_known: true}`. `max_attempts: 6`.
- GATING: movement 4, `active: false` in-seed, opened by `requires_flags {iss_caught}`. SECOND in-road
  exists (WEB-MASTER §2.1): `stone-orin` substitution stego normalizes to the same bound word — so the
  hardest coordination beat is not single-point-of-failure.

### 3.2 `m4-three-hands` — the cross-surface coop gate (`puzzles_seed.sql` L824–845)
- WHAT IT IS: `main_beat`. Title *"three hands at once."* THREE distinct ACTS (not three people; active-only):
  foot on the plate + a carve + a Discord post in the same ~20s window. Accepted answer is a single OPAQUE
  conjunction token the plugin posts on a CLEARED gate: `'h3n8k1 q5m2x7 w9j4p6 t1b6f0 c8d3s5 v2z7r4'`.
- EFFECT: `voice_key: oracleThreeHands`; `set_flags {threshold_open: true}`;
  `next_puzzle_key: threshold-coordinate`; beat `unlock` → `step: door_open`,
  `step_payload {radius: 3, open: true}`, `site_id: coop_plate`, priority 14.
- Watcher line (`voice.ts` `oracleThreeHands` L293): *"the count is three. the threshold is open."*
- WHERE: `coop_plate` (sites.yml type `coop_plate`, `CoopPlateListener`). In-roads (WEB-MASTER §2): best 3
  people; 2 can clear it; even 1 with a 2nd device, slowly.

### 3.3 `threshold-coordinate` — the true coordinate (`puzzles_seed.sql` L853–866)
- WHAT IT IS: `next_clue`. Title *"the true road opens."* Accepted: `'follow the threshold mark to where it
  points'`, `'the true coordinate is a road not an answer'`, `'walk where the threshold sends you'`.
- INV-14: the decoded value is a NAVIGATION POINTER; the typed answer is the clean destination word found
  on-site, never the signed coordinate.
- EFFECT: `next_puzzle_key: true-walk-arrive`; `set_flags {true_coord_known: true}`. Opened by
  `requires_flags {threshold_open}`.

### 3.4 `true-walk-arrive` — the true walk endpoint (`puzzles_seed.sql` L873–896)
- WHAT IT IS: `main_beat`. Title *"the road kept its word."* The destination WORD carved on leaves at the
  on-site tableau is the answer (gated to on-site presence). Accepted: **`'kept here before you'`**,
  `'the road kept its word'`, `'we were already filed here'`.
- EFFECT: `set_flags {true_destination_reached: true}`; `next_puzzle_key: rite-tokens` (the Accepting
  on-ramp); beat `reveal` `fragment: 'destination_leaves_read'`, `site_id: the_threshold`. Opened by
  `requires_flags {true_coord_known}`.
- The `'kept here before you'` answer is the M-IV payoff of plant #9 (`name-where-never-been`): the carves
  were never prediction; the group is already filed.

### 3.5 `base-docket-reread` (+ `-auto` twin) — the down-count re-reads (`puzzles_seed.sql` L596–611, L1094–1112)
- WHAT IT IS: `lore`. The Hold-Book down-count was never a doom-clock; it is the muster of present hands.
  Accepted (both rows): `'the count was never of the dark it was of the hands'`, `'the muster is read the
  hands are almost in'`, `'the down count is a muster of present hands'`, `'not a doom clock a roll call'`.
- TEXT (`voice.ts` `docketReread` via `voice_args.fragment`, DE-SLOPPED — chiasmus CUT per slop A3):
  *"the muster is read. the count was never of the dark. it was of the hands. the hands are almost in."*
- TWIN MODEL: `base-docket-reread` ships `active: false` (showrunner flips at catch); `base-docket-reread-auto`
  ships `active: true` gated by `requires_flags {iss_caught}` — the offline duplicate so the signature
  re-read STILL fires if the showrunner is asleep (no SPOF). Same de-slopped fragment.

### 3.6 `meta-unkept` — the UNKEPT acrostic stages active (`puzzles_seed.sql` L930–945)
- WHAT IT IS: `lore`, gates nothing. The six maker's-mark glyphs read in **fall-order** (Vaun, Mara, Sella,
  Orin, Brann, Iss) carry **UNKEPT** — the word each keeper failed to keep. Accepted: `'unkept'`.
  `max_attempts: 8`.
- TEXT (`voice.ts` `oracleMetaUnkept(fragment)` pass-through; `voice_args.fragment`): *"six marks, one to a
  stone. read them in the order they fell. the word is the one each did not keep."*
- GATING: `active: false` in-seed, opened by `requires_flags {iss_caught}` — the cold Iss/Keeper states the
  fall-order key at the catch, then the group assembles it. The glyphs fail in ring-order (self-correcting,
  WEB-MASTER §0.3).

### 3.7 The forged eighth collapses — `forged-eighth` → `archiveEighthCorrection`
- PLANT (`forged-eighth`, `puzzles_seed.sql` L623–636): `dead_end` kind `known`. The Covering of the Hands.
  Card text (`voice.ts` `cardEighthForged` L556): *"the founders set the ways and did not finish the count.
  the eighth is the covering of the hands. cover, and be counted clean."*
- PAYOFF (`voice.ts` `archiveEighthCorrection` L560): *"the eighth was added by a later hand, and is not in
  the founders' ring, and was never measured. obey it and nothing answers. that is how a forged way is
  known."*
- KEEPER HALF (`keeper.falseLaw`, corpus C2, fires on flag `eighth_seen`): *"you found the board over the
  carvings. the covering of the hands. a later one cut it and did not sign it true. there are six marks in
  the ring and there were always six. count them yourself. a way the land does not measure is a way a man
  wanted, not a way the land kept. keep the six. let the seventh-and-a-half lie where it was hung."*
- INV-17: the forgery is fiction by construction (no `CUSTOM_KEYS` member, no listener; the proof is the
  reliable absence of a toll).
- **FLAG NOTE — [GAP]:** `keeper.falseLaw` and the dialogue table branch on `eighth_seen`, but no shipped
  puzzle row sets `eighth_seen` (the `forged-eighth` dead_end carries only a `voice_key`). The flag-set on
  reading the forged board is owed.

### 3.8 The prophet's wall re-reads — `prophet-wall-name` (`puzzles_seed.sql` L658–667)
- WHAT IT IS: `dead_end` kind `prophet`. Title *"read who carved it after."* The columnar acrostic down the
  warm rungs spells Iss. Accepted: `'the one who turned away'`, `'iss carved the wall'`, `'read the first
  marks down the one who turned away'`. `max_attempts: 6`.
- KEEPER HALF (`keeper.deadEndTaunt(kind='prophet')`, corpus C7): *"you read that true, and it is true, and
  it keeps no door. the one called iss would have told you it kept a door, and told you warm, and you would
  have walked it to a cold hearth. i tell you plain: true, and shut. that is the difference between his
  telling and the record's. read who carved it, after."* — the *"after"* was literal (plant #19).

### 3.9 `name-where` re-reads under FACT 9 (`puzzles_seed.sql` L908–917)
- `dead_end` kind `place`. Accepted: `'the record files the living by place not only by name'`, `'against
  each name a ground'`, `'before you was never about strangers'`.
- KEEPER HALF (`keeper.nameWhere`, corpus C8): *"your name is cut where you have not been. the record does
  not wait for your foot to file you. it files the ground first and the foot after. before you was never
  strangers. it was you, before you came."*

---

## 4. THE SEVENTH SPINE CHOICE — restore vs erase

> The hearth-deep below `the_cold_hearth` (the dead-shrine surface = Iss's grave). Distinct PLACES,
> temporally layered (WEB-MASTER §0.4): the deep opens only post `iss_caught` + `seventh_named`.
> Site: `the_unwriting` (sites.yml type `seventh_shrine`, `SeventhChoiceListener` watches it; one-site
> break-whitelist lives here).

### 4.1 `seventh-unwriting` — name the Seventh (`puzzles_seed.sql` L678–701)
- WHAT IT IS: `main_beat`. Title *"the seal is a name."* RAIL-FENCE (rails = 6, counted in-world on the wall)
  REUSING Brann's taught rail-fence literacy (it does not teach the cipher cold; gated on Brann's literacy
  per WEB-MASTER §2 rail-fence home). Accepted: `'below the cold hearth the deep is sealed the seal is a
  name'`, `'the unwriting keeps the name it cut out'`, `'the seventh kept all the ways and was cast out'`.
- THE CIPHER LOGIC (the-seventh-spine §1.1): erasure *is* transposition — the scraped seventh name was
  displaced into the **kerning** of the six kept names; rail-fence reads it back. (Chamber-2 lore on solve,
  §3a: *"six names are cut whole here. the seventh was cut into the space between them. you have read it now.
  the record did not keep it; you did."* — carried by `oracleMainBeat`/lore fragment.)
- EFFECT: `set_flags {seventh_named: true}`; `next_puzzle_key: seventh-choice`; beat `reveal`
  `fragment: 'seventh_name_unsealed'`, `site_id: the_unwriting`. `active: false`, opened by
  `requires_flags {iss_caught, seventh_suspected}` (metapuzzle_seed L128).

### 4.2 `seventh-cause` — why erased, FACT 10b (`puzzles_seed.sql` L706–720)
- `lore`, gates nothing, earns Whisper budget. Accepted: `'the land refused a keeper who broke nothing'`,
  `'kept all the ways and cast out anyway'`, `'a thing that can say no is not a wall'`.
- TEXT (`oracleLore` fragment): *"the seventh kept every way and was not kept. the fire they let out was
  never theirs to lose. the land can refuse. whether that is mercy the record does not say."*
- The cause-fragment document `D-new the-fire-they-let-out.md` is **[GAP — TO BUILD]** (the-seventh-spine
  §7 P2.6: *"Author D-new … to back the seeded `seventh-cause` / `dest-fire-let-out` correlation"*). The
  effaced-hand draft exists in the idea file §3b only: *"they did not break a custom. they kept all ten and
  were refused anyway. that is the thing the record could not hold and stay a record. so it let the fire out,
  and shut the door, and made the refusing into a thing that never happened."*

### 4.3 The Keeper lays the choice down — `keeper.seventhChoice.offer` (corpus C3; fires `seventh_named` & deep open)
> *"below the cold hearth the deep is open now. the seal there was a name, and the name was scraped out, and
> the one it named kept every way and was cast out for nothing done. a name said back is a seal undone. you
> may write it again, or leave the blank. the land made its choice. you make the record's. neither opens the
> road. both are kept."* — **non-prompting** (no "press 1"; states the deep is open, lays the fork without
> weighting it; R1).

### 4.4 `seventh-choice` — the restore/erase rite (`puzzles_seed.sql` L731–751)
- WHAT IT IS: `main_beat`, DETECTED IN-WORLD ONLY by `SeventhChoiceListener` (no typed answer). Two opaque
  wordless tokens, one per branch (verbatim): **restore** = `'r7n4k2 m1x8p5 w3j6h9'`; **erase** =
  `'e5t0b7 c2d4s8 v6f1z3'`.
- THE TWO PHYSICAL ACTS (the-seventh-spine §1.4):
  - **RESTORE**: set a marker block in the bare socket + light the cold pit + all present bow (synchronized
    sneak, active-only) at `the_cold_hearth` deep → `seventh_choice = 'restore'`. The deposit is ALSO the
    INHERITORS codicil (`ending_codicil`) — ONE act, ONE flag-origin (no separate `dark_shrine`).
  - **ERASE**: break the six name-carvings at the unwriting wall (the one whitelisted break-site, gated on
    `seventh_named` + the deliberate all-six-in-window act) + leave no carried light →
    `seventh_choice = 'erase'`.
- EFFECT: beat `reveal` `fragment: 'seventh_choice_marked'`, `site_id: the_unwriting`. The resolver's
  Seventh-choice sentinel branch sets `seventh_choice` + `ending_codicil` from which token matched
  (TS-SHOWRUN owns the branch). `active: false`, opened by `requires_flags {seventh_named}`. GATES NOTHING.
- IRREVERSIBLE in fiction + flag (chamber-3 listener self-disables for the session, idempotent); not a loss
  condition, not a gate. A group can refuse to choose and lose only the shading.
- **PLUGIN [GAP — TO BUILD]:** `SeventhChoiceListener.java` DOES NOT EXIST (the-seventh-spine §0/§7 P1.1:
  *"the single largest gap — without it the seeded restore/erase tokens can never be posted, so the whole
  choice is inert"*). Also owed: the unwriting `.schem` set-piece + real coords for `the_unwriting`
  (currently `x/y/z: null`).

### 4.5 The retreating anti-creature (chamber-3 entry)
- A single un-targeted retreating glimpse, once, on entry to chamber 3 — the one apparition NOT grounded in
  a measured signal (WEB-MASTER §7; bestiary-sealed §2.6). *The absence of profiling IS the characterization*
  (the Seventh is the one the land did not measure). **[GAP — TO BUILD]:** the `NamedMobBeat`
  (un-targeted, very short `despawn_seconds`) glimpse is P2 (the-seventh-spine §7 P2.7), unbuilt.

### 4.6 The single sanctioned Iss/Seventh ambiguity line (post-`iss_caught`)
- One `lore` line, a question the world refuses to answer (the-seventh-spine §3e): *"two hands scraped this
  stone. the record does not say they were two."* **[GAP — TO BUILD]:** P2, not yet a seeded row
  (the-seventh-spine §7 P2.8).

---

## 5. BECOMING THE KEEPERS — the reveal (the record writing them in)

> The felt door to FACT 15, **never stated**. Delivered as the Keeper's M5 on-ramp summons (corpus C4) +
> the Hold-Book's keeper-hand pages (§2.4) + the down-count re-read (§3.5) converging: the group is being
> written into the same book, same hand, as every keeper above them.

**`keeper.becomingKeepers.neutral`** (corpus C4 — the bare summons; the nearest approach to the sealed
truth, stopping at the half-veil):
> *"the altar wants a thing only each of you can give. not a stone, not a light. the thing the record kept
> open a column for, against your name, before you came. bring it at the dark hour. the rite does not reward.
> it receives. it keeps. the ones before brought theirs, and were received, and are kept — you have read
> where they are kept; you are reading it now, in the same book, in the same hand. we would keep you, if you
> would keep the ways."*

**`keeper.becomingKeepers.warm`** (C4 — KEPT conduct; the warmth in what he offers):
> *"… we would keep you. you have made the keeping easy."* (first sentence re-skinned; same half-veil close.)

**`keeper.becomingKeepers.cold`** (C4 — BROKEN conduct; grief, never a bar; the rite stays open):
> *"you broke a way or two coming to this. the altar takes a broken hand too — it took mine, late. … the
> rite does not reward and it does not refuse for what is already done. it receives, and it keeps. … we would
> keep you, if you would keep the rest of the road."*

> **The discipline (corpus mimic-check / §7.3):** every variant STOPS at *"we would keep you…"* / *"in the
> same book, in the same hand you have been reading."* A draft that finishes the induction (*"and so you
> become the watching"*) STATES FACT 15 and is a defect. The half-veiled *we* is the only place the
> recursion shows, and it only points.

---

## 6. THE COUNTING-JOURNAL PAYOFF (the Hold-Book, M4 → M5)

The down-count and the keeper-record are ONE book on ONE anchor (`stone_of_reckoning`'s companion lectern;
WEB-MASTER §4). The M4 payoff = §3.5 (`docketReread`: the count was the muster of present hands, not the
dark). The M5 close (forward-reference, the journal's last face):
- `voice.ts docketEven` (L435): *"the present hands are entered. the book is even. the same book, the same
  hand, as all the ways above you."*
- `voice.ts keeperEnrolled` (L443) — the neutral colorant ack when first moved under a heading: *"the one
  called {name} is entered under {keeper}. the heading is not a sentence. it is where the record set them."*

---

## 7. THE END LANE (optional deepening off M4→V; gates nothing — `progression_seed.sql` + WEB-MASTER §1.M4/§5)

> *"the one place outside the record — no kept fire, no markers, no Archivist, no count."* D11 is its plant
> (*"to be kept and to be cast out are one door, looked at from either side"* — ledger #29). No new M3 plant.
> Worlds: `observance_end` (Multiverse pattern). ZERO ambient apparition lane (a positive canon choice).

### 7.1 The pointer (a REVEAL on the existing surface — NO new puzzle node, S9)
- WHAT IT IS: the `the_unwriting` chamber-2 wall gains **one extra effaced line**, legible only at
  `seventh_named`, pointing *"out, past the door that is not a threshold… you will not be kept there; the
  record is not there to keep you."* (WEB-MASTER §1.M4). A `RevealBeat` flip on the existing wall, not a row.
- **[GAP — TO BUILD]:** the extra effaced line is the one new M4-catch cold-re-read the End is budgeted
  (S2); its authored reveal is owed (no shipped artifact carries this exact string yet).

### 7.2 The payoff — `end-seventh-out` (`progression_seed.sql` L106–122)
- WHAT IT IS: `lore`, gates nothing. Title *"the name i cut myself."* The Seventh's own account from outside
  the record. The on-site READ is the answer (INV-14), at `end_seventh_shrine` (sites.yml type `answer_sign`,
  `observance_end`). Accepted: `'i kept all the ways and it did not matter'`, `'the keeping was never the
  price'`, `'i went out past the door that is not a threshold'`, `'you only came to look'`.
- EFFECT: `voice_key: end.shrineArrive`; `set_flags {seventh_seen_out: true}`; `voice_args.fragment`
  (verbatim): *"the seventh kept every way and was not kept, and went out past the door that is not a
  threshold, to the one place the record does not reach, and cut the name themselves. exile is the other side
  of keeping. you are not cast out. you only came to look."*
- GATING: movement 4, `active: false` (STAGED until `observance_end` is built AND `seventh_named` set);
  `requires_flags {seventh_named}` (progression_seed L221). `seventh_seen_out` is a group-scoped flag, **NEVER
  a fate input** (S2) — it deepens `seventh_choice` context + licenses the End cast_out/refusers re-read in
  the M5 composer only.
- **[GAP — TO BUILD]:** the `end.shrineArrive` voice key is referenced but the fragment IS the payload (so
  the line ships); however the thread-card body `cardEndSeventhOut` (`who-seventh-out` card,
  `progression_seed` L190) is **not** authored in `voice.archive.ts` (`threadCardVoiceCoverageSelfTest` fails
  until it exists). The `observance_end` world itself is unbuilt (GO-LIVE).

### 7.3 The breadth ledger + rumor card (the clustering surfaces)
- `dest-out-of-record` (`progression_seed.sql` L159, thread `who`, tier `keyed`, 16 min): *"KEYED (on-site
  read, INV-14): the Seventh outside the record; seventh_seen_out (not a fate input, S2); exile = the other
  side of keeping."*
- `who-seventh-out` rumor card (L190): title *"the name cut in exile,"* anchor `end_seventh_shrine`,
  references `who-sella-token` + `surface-seventh-marker`, `revealed_by_solve: end-seventh-out` (flips
  verified on arrival).

### 7.4 The exile / Seventh set-pieces (the place)
- **`end_seventh_shrine`** (sites.yml L476–486): the Seventh's home, built to the Seventh's unfinished
  wrong-scaled hand. Reveal-safe: a re-dressed end-ship (force-load → mutate on unwitnessed relog → unload)
  OR a world-build pre-generated outer island — NEVER a lazy paste toward an approaching glider (S6).
  Producer rule (S7): additive pastes onto verified-clear adjacent air (a deepslate carving-slab beside the
  vanilla blocks) or `RevealBeat` flips — NEVER an occupied overwrite, NEVER `RoomSwapBeat` (Undercroft-only).
  `enabled: true`, coords null (unplaced until GO-LIVE).
- **`end_exile_hold`** (sites.yml L494–502): the `cast_out` divergent fate made a PLACE — a re-dressed
  end-city, markers all facing AWAY, vast, static, discovered, never witnessed mutating. **`enabled: false`**
  — GATED until the INV-16-bound binding is BUILT (S10, non-negotiable): the hold must name NO living player
  and encode NO per-player side; chorus-only dressing (*"you only came to look"*), never the `LEFT_AT` set,
  no dressing spatially corresponding to any per-player carve. **If the open End cannot guarantee that, this
  stays disabled and the End ships as the Seventh shrine ALONE (the default).** **[GAP — TO BUILD]:** the
  INV-16-safe exile-hold dressing + its set-piece is unbuilt (P2/cuttable; sits disabled by design).

---

## 8. THE CAST-OUT = END LINK, AT V (forward-reference into the ending composer)

> The End lane's weight TRAILS into V (S2): only the wall's extra line lands at the M4 catch; the shrine
> carving + the cast_out/refusers re-read land at the Accepting via the M5 composer (`voice.ts` §5).

- The composer reads `seventh_seen_out` as a colorant (WEB-MASTER §5, L564–570): it gates the End-lane
  "you stood where the unwritten stood" re-read clause; this clause **REPLACES** the neutral fate clause (it
  does not add one, so the ≤2-clause cap holds). It is NOT a `decideFate` input (S2).
- **CAST_OUT + `seventh_seen_out`** (WEB-MASTER §1.M5): the re-dressed end-city (`end_exile_hold`) reads as
  the group's OWN exile-hold. Base fate line (`voice.ts fateCastOut` L459): *"the count is closed and it is
  short. the markers face away. what was owed was not returned, and the record enters it so."* Keeper face
  (`keeper.endings.castOut`, corpus C5): *"it is done. the markers face the wall. some of you were left at
  the threshold, as some were left before. i do not name which. … come down again, kept, and stand again."*
- **REFUSERS + `seventh_seen_out`** (positive defiance; bow window empty): the shrine re-reads as *the model
  they followed.* Base (`voice.ts fateRefusers` L467): *"the hands were all present, and the bow was not made.
  that too is entered. the record keeps the refusal as plainly as it keeps the keeping."* Keeper face
  (`keeper.endings.refusers`, C5): *"… i do not know what it keeps it as. i was kept. i did not refuse."* The
  *"one before you"* the shrine evokes = the Seventh whose shrine they stood in (still group-scoped).
- **The Seventh choice tints the close** regardless of the End lane (`voice.ts` L482–488):
  `keeperCloseSeventhRestored`: *"the name that was cut out is cut back in. the hearth below the cold hearth
  is lit again. one that broke nothing is kept, late."*; `keeperCloseSeventhErased`: *"the name stays out.
  the wall below the cold hearth stays blank. the record keeps the blank where the name would go, and does
  not fill it."* The Keeper faces are `keeper.seventhChoice.restored` / `.erased` (corpus C3). The two
  deepening lanes deepen the tint (Nether = "keeping is a carrying"; End = "exile is the door's far side")
  but NEVER change the mechanic (INV-12 colors-never-gates).
- INV-16 teeth (non-negotiable, S10): no surface lets the group derive WHICH active player is on the
  honored/violated side; the `DIVIDED` floor split is BY GEOMETRY, never by player (`voice.ts fateDivided`
  L463; `keeper.endings.divided` C5).

---

## 9. THE INHERITORS CODICIL (the +1 clause the Seventh restore act plants)

- `restore` sets `ending_codicil` (= INHERITORS) at the same act — ONE flag. The composer may append the +1
  clause (`voice.ts fateInheritorsCodicil` L471): *"a mark is left for a hand not yet here. the deposit slot
  is cut and waiting, the way yours was cut and waiting before you came."* Keeper face
  (`keeper.seventhChoice.restored`, C3): *"… a mark is left there now for a hand not yet here, the way a mark
  was left for you. that is the older keeping."*
- This pays off within-arc (the group just inducted a seventh; they are about to be inducted) — the felt
  FACT 15 rehearsal from the keeper's side, NEVER stated.

---

## APPENDIX — the requires_flags activation map for this segment (metapuzzle_seed.sql L155–163; progression_seed.sql L234–235)

```
no-wall-catch           → (player-driven catch; sets iss_caught, true_coord_known)
bound-word              → iss_caught                       (no-wall-catch)
base-docket-reread      → iss_caught                       (showrunner OR auto)
base-docket-reread-auto → iss_caught                       (offline twin)
meta-unkept             → iss_caught                       (no-wall-catch)
m4-three-hands          → bound_word_known                 (bound-word)
threshold-coordinate    → threshold_open                   (m4-three-hands)
true-walk-arrive        → true_coord_known                 (threshold-coordinate / catch)
seventh-unwriting       → iss_caught ∧ seventh_suspected   (no-wall-catch ∧ stone-sella)
seventh-cause           → iss_caught ∧ seventh_suspected
seventh-choice          → seventh_named                    (seventh-unwriting)
end-seventh-out         → seventh_named                    (seventh-unwriting)  [GATES NOTHING — lore]
nether-forge            → undercroft_open                  (undercroft-descent) [the Nether twin lane]
```
