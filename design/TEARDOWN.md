# THE OBSERVANCE — EDITOR-IN-CHIEF TEARDOWN

> **SUPERSEDED PRE-V5 ARCHIVE — NOT A CURRENT DEFECT OR READINESS LIST.** Current failures come only from V5 checks and live receipts.

> **CURRENT STATUS OVERRIDE — 2026-07-13.** This is a preserved pre-fix adversarial audit, not a
> description of the current release candidate. Its missing migrations, producers, resolver gates,
> Record ignition, `v_record`, resource-pack handling, Keeper/Wren paths, and generated-Hold findings
> were subsequently fixed and regression-checked. V4 has also been exercised on isolated Paper
> 1.21.11. Use `FINAL-LAUNCH-HANDOFF-2026-07-13.md` and `CURRENT-READINESS-VERDICT.md` for current
> truth; retain the numbered findings below as failure history and regression rationale.

Synthesis of six adversarial passes (story, puzzles, players, arg, code, interactions),
cross-checked against the real seeds/code (not the design guides, which over-claim).
Verdicts: KEEP / CUT / FIX / SIMPLIFY / REDESIGN. Every point names a real artifact.

---

## 1. THE VERDICT IN 3 SENTENCES

You have built a beautiful, over-engineered machine whose central nerve was never
soldered: the flag-driven progression is simultaneously **wide open** (gated rows that
ship `active=true` have no working gate — `getOpenPuzzles` at `repo.ts:361` selects 8
columns and never reads `requires_flags`) and **permanently sealed** (gated rows that
ship `active=false` have no producer — migration `0006` doesn't exist, and the
IgnitionListener / CoopPlateListener / SeventhChoiceListener / composer.ts that would
set the flags and assemble the ending are all absent), and every guard fails silent so
the breakage is invisible. The creative layer has the opposite problem from the same
root cause — it is **over-woven for a friend group that will miss most subtlety**: 24
threads, ~21 facts, 10 invariants, two extra dimensions (Nether/End), a filing-system
thesis, and three different "sevens" surround a genuinely excellent core (six distinct
broken keepers whose fates mirror player behavior, the Liar/Vigenère catch, the
receive-don't-reward rite). Strip two-thirds of the cleverness, solder the one nerve,
and prove a 1-room vertical slice on a real server — the mystery lands *harder* smaller,
and right now **nothing has ever run and the loop the whole project is built around is a
no-op.**

---

## 2. THE WORST OFFENDERS (top 12, cross-dimension, worst first)

**1. [FIX] `getOpenPuzzles` (`discord/src/db/repo.ts:361`) never reads `requires_flags`.**
VERIFIED: selects `puzzle_key,title,accepted_answers,outcome_type,outcome_payload,movement,active,max_attempts` and filters `.eq('active',true)` only. Every `active=true`+`requires_flags` row (`base-docket-reread-auto`, `meta-unkept`, `seventh-choice`, `threshold-coordinate`, the Iss chain) is solvable from minute zero, before its setup exists. The M1→M5 escalation has no order; payoffs land before plants; a debugger group reads it as "the bot is buggy" and quits. The Java twin `OracleResolver.firstMatch` has the identical gap.
**Fix:** add `requires_flags` to the select in both surfaces; in `matchPuzzle`/`firstMatch` read `arc_state.flags` once and reject any row whose `requires_flags` keys aren't all truthy. Gated on #2.

**2. [FIX] `discord/supabase/migrations/0006_*.sql` does not exist.** VERIFIED: only `0003/0004/0005` present. The `requires_flags` column the seeds depend on is never created, so `metapuzzle_seed.sql:111` / `progression_seed.sql:215` hit their `if column exists` guard and silently `raise notice` and SKIP every activation UPDATE. `active=false` gated rows are therefore permanently dead. The seeds "succeed," the game is silently broken.
**Fix:** write `0006_requires_flags.sql` adding `puzzles.requires_flags jsonb not null default '{}'`, re-run both seeds.

**3. [FIX] No flag *producers* exist.** VERIFIED: `IgnitionListener`, `CoopPlateListener`, `SeventhChoiceListener`, `UnlitDeepListener`, `RefusalRiteListener` are all absent from `plugin/`. These are the ONLY writers of `prologue_ignited`, `iss_caught`, `seventh_named`, `undercroft_open`, the coop/refusal tokens — i.e. exactly the `requires_flags` keys. Even after #1+#2, the gated rows stay closed forever because no event ever sets their flag. `prologue_ignited` is read at `autonomy.run.ts:125` and **written nowhere** (grep confirms). The M0 frame-break — the literal "it knows your name" promise — has no producer.
**Fix:** build the listeners (start with Ignition + CoopPlate), or a temporary `/obs flag set` admin command, before any playtest. #1+#2+#3 are one atomic change; nothing else matters until it lands.

**4. [REDESIGN] The central scare cannot fire: `voice.recordFrameBreak()` does not exist.** VERIFIED: grep of `voice.ts` returns zero for both `recordFrameBreak` and `recordOpenedNamed`. The group posts "kept" in `#the-record`; the real `messageCreate` handler (`bot/index.ts:99`) runs only the answer-scan → non-match → `silent`. The first server beat the entire prologue banks dread for is **silence**. The "quiet map → the server knows you" contrast inverts into "the quiet map → the server is offline."
**Fix:** author `recordFrameBreak()` + `recordOpenedNamed`, and in `messageCreate`, when `channelId===theRecord` and the poster is a keeper, set `prologue_ignited=true` idempotently and let the next autonomy tick emit the ack. ~10 lines unblocks the whole ARG dimension.

**5. [FIX] Cross-row answer collision: `the one who turned away` is a primary answer on THREE active rows** (`stone-iss-wall` L199, `prophet-wall-name` L661, `bound-word` L805). `matchPuzzle` returns the FIRST open row by DB order — one solve silently fires the wrong puzzle_key, speaks the wrong voice line, sets the wrong flags (`iss_key_turned` vs `bound_word_known`). With #1 broken, a day-one Iss decode consumes the M4 `bound-word` node. `seventh-shrine`/`seventh-cause` share `a thing that can say no is not a wall`; `iss` is a bare answer too.
**Fix:** never let two simultaneously-open rows share a normalized string; give each shared plaintext exactly one owner and disambiguate the rest.

**6. [REDESIGN] Reveal discipline collapses with a convened group — the M1 backbone frequently never appears.** `Reveal.isHidden(loc)` is true only when NO online player in the world is within `witnessRadius` with line of sight. This is a 4-8 person group standing in ONE base. So `SignWriteBeat`, `RevealBeat`, `SmallStructureBeat`, `RoomSwapBeat`, `LecternFillBeat` (the first report, the rune marker, the keeper carvings) call the GLOBAL `mutateWhenUnwitnessed` and silently never fire while anyone is looking — i.e. exactly when the group is present to be scared.
**Fix:** re-tier the in-game layer — world-build every stone/shrine/lintel/hearth/grave statically; reserve runtime beats for per-player packet illusions via `isHiddenFrom(player)`/`sendBlockChange`. (This is the single biggest interactions fix; see §7.)

**7. [FIX] The Whisper hint system has voice lines but NO mechanism — the group's only safety rail is dead.** No `whisper_budgets` table, no spend module, no per-puzzle `hintBody` strings. `/whisper <puzzle>` cannot return a tiered hint. A real group hits ONE unsignposted cipher (Mara's book cipher, Iss's Vigenère) and stalls a whole session; with the rail dead they quit or demand the answer in voice, breaking immersion permanently.
**Fix:** build the budget ledger + author 2-3 hint tiers for each of the ~8 spine ciphers BEFORE first playtest. Treat the hint corpus as P0.

**8. [FIX] The central number is broken: the lure `6` carries two incompatible meanings.** `six-were-kept-before-you.md` = six prior keeper GENERATIONS (group is the 7th group). `the-seventh-not-kept.md` + `the-record-opens` = six kept keepers of ONE generation (Vaun…Iss), 7th = the cast-out Seventh keeper. The cursed-map `kept:6` the group starts on is a third collision WEB-MASTER never addresses. A friend group instantly conflates "you are the seventh" (count) with "the Seventh, cast out for nothing" (keeper) and feels the game is calling them the doomed one.
**Fix:** pick ONE referent for the lure `6` (cleanest: six prior generations); make `the-record-opens` say "six generations," rename the in-generation roster so no bare "six" reads as the same number.

**9. [SIMPLIFY] The M2 field opens EVERYTHING at once — six ciphers + Seventh sidequest + prophet wall + forged eighth + Liar + two Rosettas + Nether + difficulty engine — and the cold Watcher never points.** A 5-person voice-chat group holds 2-3 threads; they fixate on one stone, ignore the rest, never discover that 4-of-6 fragments gates M3. The breadth that reads as "rich" reads as "we don't know what we're supposed to do." The Set-A human NPCs (Aro/Wenna/Dob) that would contrast the flat voice are all `[GAP — GO-LIVE]`, so at launch the group meets only the affectless register.
**Fix:** have the Watcher's daily drip name ONE live thread at a time ("something is set out at the far water") so the field is a queue, not a wall. Ship at least Aro + Dob in-game bodies for launch.

**10. [REDESIGN/SIMPLIFY] The M4→V coop gate (`m4-three-hands`) demands three real-time acts in a ~20s window — the worst possible mechanic for a remote/async group.** The project's premise (MEMORY) is a scattered async group whose only reason to gather is this map; the spine then requires foot-on-plate + carve + Discord-post simultaneously within 20s. Worse, the AND-join is mis-located: the seed comment says it lives in `resolve.ts applyOutcome`, but `applyOutcome` only runs AFTER one answer already matched — it has no concept of three legs or a window, so the row fires on the FIRST sentinel submission (a one-hand gate mislabeled three). And the sentinel can't be produced because `CoopPlateListener` doesn't exist (#3).
**Fix:** widen to minutes, latch each leg independently (gate fires when all three legs held), and put the leg-counting in `CoopPlateListener` which posts the token only once all legs are seen. The theme ("you cannot do this alone") survives a generous window.

**11. [FIX] The decodable stego is never applied; the "preferred" stego is undecodable.** Cards render `satori → SVG → resvg → PNG`. The "preferred" `embedRuneLayer` stamps `data-stego-payload="ISS"` into SVG markup that is **discarded at rasterization** — `extractRuneLayerPayload` round-trips an in-memory attribute the player never receives. The one PNG-surviving scheme (`embedLsb`) is imported but never called on a real card (`withStegoRuneLayer` composites the layer and never invokes `embedLsb`). The stego ships decodable-in-theory and visible-in-practice but never both on one artifact.
**Fix:** run `embedLsb(frame,'ISS')` on the Iss card's RGBA after resvg, OR accept the rune layer as a visual-only watermark (read by eye + Rosetta) and delete the false "decode" framing. Don't ship self-test theater.

**12. [FIX] Two duplicate-puzzle/answer hazards that buy nothing.** (a) `base-docket-reread` ↔ `base-docket-reread-auto` carry identical answer sets and both speak `docketReread`; once #1 is fixed, whichever opens first eats the solve and the other is unsolvable. (b) `rosetta-ring` accepts `bow offering kept light deep line unspoken sacred beast` but the carved ring + `cipher-web.md` teach `...line WARD COVERING` — the literacy gate teaches one answer and the resolver silently demands another (pure moon-logic, and it's THE rune-literacy on-ramp).
**Fix:** collapse the docket pair to one row gated on `iss_caught`; make the carved ring glyphs and `rosetta-ring`'s accepted answer the SAME six words before this ships.

---

## 3. IS IT TOO COMPLEX?

**Yes — decisively, and in a way that actively works against this specific group.** Not
"complex" as a compliment-shaped flaw; complex in the precise sense that the surface area
is a multiple of what a 4-8 person irregular voice-chat group will ever touch, and the
excess directly drowns the parts that are genuinely great.

The specific over-load points, and what the players will actually miss:

- **24 active threads / ~21 facts (15 base + children 16/2b/7b/13b/10b/17) / 10
  invariants / 4 fates / 2 codicils.** The seven load-bearing foreshadows of FACT 15 (the
  real spine) compete for attention with manufactured namespace-children like FACT 17
  ("the record files by *word*") — a whole filing axis that rides on an unbuilt P3 voice
  layer and is delivered, realistically, by one comma-clause in one document. **Players
  miss:** the entire third filing axis; nobody infers a database schema from a comma.
- **The UNKEPT six-glyph meta-acrostic (`meta-unkept`).** Requires noticing a tiny
  maker's-mark glyph on each of six stones scattered across the map, recording all six,
  learning the fall-order (Vaun/Mara/Sella/Orin/Brann/Iss) from a separate doc, and
  anagramming — with ring-order deliberately yielding nonsense. By the design's own
  admission it "gates nothing." **Players miss:** all of it. Zero unspoiled groups
  assemble a cross-map acrostic in a death-order. The single accepted answer is the bare
  word `unkept`, brute-forceable from a thesaurus.
- **Nether lane + End lane** — two full optional dimensions whose entire payload is two
  tint clauses in a close capped at ≤2 clauses, and the End lane has *written its own cut*
  (WEB-MASTER §1.M5: "if the open End cannot guarantee no per-player side, the binding is
  CUT and the End ships as the Seventh shrine alone"). **Players miss:** both — a remote
  group will not org a second-dimension expedition for an un-shaded tint, and the
  apparition placement in those worlds is physically impossible anyway (open End = no
  unwitnessed instant; Nether `findSpawn` lands over lava).
- **The Iss thread split across SEVEN puzzle rows** (`stone-iss-wall`/`iss-warm`/
  `iss-doubt`/`no-wall-catch`/`prophet-wall-comfort`/`prophet-wall-name`/`bound-word`) for
  one reveal: "the warm guy lied." **Players miss:** the distinction between "the warm
  reading" and "the skeptical name-as-key reading" of one stone; the columnar acrostic
  spelling Iss's name; the Iss-vs-Seventh distinction canon §5 spends notes defending while
  the level geometry (shared cold hearth) undermines it.
- **The dynamic-difficulty reveal (FACT 2b, `difficulty-mara`).** Asks the group to
  realize retroactively that drip pacing was a diegetic judgment of their solve speed —
  but hint-withholding is indistinguishable from "the game was being stingy." **Players
  miss:** the whole point; worse, it risks reading as the game having been *unfair*.
- **The cross-surface website un-redaction + stego second-door.** Assumes the group keeps
  a browser tab open mid-Minecraft-session and re-checks it as stones solve. They will
  not. And `readSignal()` reads `v_record`, a view that "has not shipped yet," so the page
  renders the all-redacted baseline for every movement regardless of progress.

The friend group will, realistically, experience: the six keepers, a few ciphers, the
Iss catch (if signposted), the room-swap (if they trust it's authored), and the final
bow. Everything else is authoring cost spent on surfaces they never reach. **The mystery
is not too hard to feel; it is too dense to find.**

---

## 4. CUT LIST (delete these; each subtracts more than it adds)

- **CUT the Nether build entirely** (`the-fire-is-lent.md` lane, threads #28/#30). FACT 11
  is already delivered by `undercroft-fog` ("one fire, no one tends it"); the lane is
  blocked on a canon sentence that doesn't exist; the payoff is one tint clause that gets
  out-prioritized and shows nothing. **Keep `the-fire-is-lent.md` as an Undercroft shelf
  page** — it's beautiful and carries the carrying-not-owning theme for free.
- **CUT the End build to its declared default** (Seventh shrine only, already reachable in
  the Overworld via `the_unwriting`). The exile-hold's only non-default payload is
  conditionally forbidden by its own spec. Keep D11's "one door, two sides" line.
- **CUT the UNKEPT six-glyph hunt** (`meta-unkept` as a glyph-assembly puzzle). Replace
  with a single Watcher line at the catch ("six marks, one word — the word is the one they
  did not keep: UNKEPT") delivered as told lore. Keep the chill, lose the impossible hunt.
- **CUT FACT 17 / the Ear / `the-record-axis-of-word` as a numbered fact.** Demote to a
  one-line flourish on the Archivist register. If the Simple-Voice-Chat "Ear" ever ships,
  it's cool *texture*, not a *fact*.
- **CUT the dynamic-difficulty as a REVEALED fact (FACT 2b).** Keep the engine as an
  invisible pacing tool (good UX); don't promise a reveal the players have no surface to
  perceive and may resent.
- **CUT `prophet-wall-name`** (the columnar-acrostic third encoding of "the one who turned
  away") and **SIMPLIFY `prophet-wall-comfort`** to a found carving, not a puzzle row.
- **CUT the phrase-only "type-the-theme-back" non-puzzles**: `name-where`, `m2-rhyme`,
  `haunting-biography`, `difficulty-mara`. There is no deterministic path from the world to
  these exact strings — the only way to "solve" them is to read a design doc. Convert to
  Watcher-spoken lore beats.
- **CUT `fork-light`/`fork-name`** (dead color flags read only by the nonexistent M5
  composer) until the composer exists. **CUT `pressure-glyph-walk`** unless the floor-rune
  walk is actually built and detectable.
- **CUT `ModeledMobBeat`** from the active set — with no ModelEngine purchase (R4 is paid,
  unbought) and no authored rigs, it is a ~240-line byte-for-byte clone of `NamedMobBeat`
  implying a capability the project doesn't have. Same for the entire "custom 3D
  apparition" column of bestiary §0.4 (every creature "ships as a ModelEngine model"; NONE
  exist).
- **CUT the worn-skin offline apparition from the shipping arc** until ModelEngine + a
  player-skin rig is wired. `applyWornSkin` leaves a WARDEN named "Brann" — a mislabeled
  Warden is worse than no apparition (violates the project's own rule #1).
- **CUT the dead-lead side-quests** from `side_quests.sql` (18 destinations, 5 deliberate
  duds) down to at most 1-2; a casual group reads a string of dead-ends as "we're
  off-track / the game is broken."
- **CUT the "find the other five copies" leaf** (`cursed-map-frame §4`) — an out-of-game
  taunt with no listener and no input surface to attach to.
- **CUT the 6-room prologue vignette to 1 room** (see §6) — a second full Minecraft build
  with zero validated play, feeding exactly one server line that doesn't need it.

---

## 5. USELESS-OR-DONE-BETTER (weak interaction → concrete better version)

- **The Quiet Herd "collective gaze" (`herd_anchor`, bestiary §6, sites.yml A12)** —
  THERE IS NO BEAT THAT DOES IT (grep of `beats/lib/` finds only `SacredAnimalBeat`/
  `MapMarkBeat`/`UnlockBeat`); and even if built, vanilla animals re-path every tick and
  the "all face you" pose decays in <1s without an AI-fighting `setRotation` loop (banned
  jank). **Better:** a *frozen tableau* — N `setAI(false)` copies pre-faced at the anchor,
  discovered never witnessed. Or cut the herd; one glowing watched animal already carries
  the custom.
- **The Sacred Beast (`SacredAnimalBeat`)** — "one glowing cow" with `glow` defaulting
  OFF and `setSilent(true)` is trivially overlooked OR trivially griefed (one arrow ends a
  permanence fork nobody knew they were touching). **Better:** glow default-ON + a
  rune-name nameplate so it's unmistakably "the watched one," with an in-the-moment
  acknowledgment when killed.
- **`iss-warm` as "the warm reading of the Vigenère"** — it's a hand-typed seeded phrase
  (`the ways are a wall against the watching`), NOT produced by applying key=ISS to
  anything; the "two readings of one key" (the whole Liar engine) is faked. **Better:**
  forge the warm plaintext as a real `vigenere` decode of Iss's own carving so the duality
  is genuinely discoverable, OR present it honestly as the in-fiction "if you read it
  trustingly" framing on the same stone (not as a cipher duality).
- **The Iss catch (`no-wall-catch`/`iss-doubt`)** — relies on the group spontaneously
  reusing Iss's name as a Vigenère key on a DIFFERENT stone, with nothing in the world
  hinting a key is reusable. The warm misreading walks them to a grave and they never
  doubt Iss. **Better:** have the cold-hearth dead-end itself plant the doubt ("the road
  was read true and still went nowhere — whose road was it?") so the grave PUSHES them back
  to re-test Iss's key.
- **The A→B room-swap (`undercroft-fog`/`RoomSwapBeat`)** — fires only when no player
  looks; for a Minecraft group "blocks changed when I wasn't looking" reads as a chunk/
  lighting bug or griefing, and the unwitnessed instant may never come for a group
  exploring together. **Better:** fire on a deterministic trigger (sealed door closes + a
  short delay), pre-stage Room B in an adjacent unloaded location as a teleport-on-reentry,
  and pin a Watcher line ("the room is not the room you left — read it again") so the
  change is claimed as intentional the instant they notice it.
- **The answer-sign verb (`AnswerSignListener`)** — invisible and unguessable: nothing
  tells the group a sign is the input device, radius 3 is tight on a floor-slab stone, and
  a correct solve gives no confirmation AT the stone so they re-submit endlessly. **Better:**
  world-build a pre-placed labeled answer lectern at each stone ("speak here"), widen
  radius to 4-5, and on SOLVED light a local sea-lantern so the loop reads.
- **`record-url` URL door** — the decoded answer is `the record keeps` (spaced); the lure
  route serves only `the-record-keeps` (hyphenated); nothing in-world hands the group the
  host. A correct solve is a hard dead-end disguised as a win. **Better:** make the
  `recordElsewhere` lore fragment carry the full host+path literally (gates nothing, no
  spoiler cost), and have the Watcher Discord-post the link at ignition + finale so the
  loop closes for everyone, not the one person who memorized a URL.
- **`SpatialVoiceBeat`** — not spatial; its `behind`/`offset` fields are read but never
  used, and `PerPlayer.namedSound` is non-positional, so "the dark said your word back over
  your shoulder" is just a centered clip. **Better:** drop the dead params, or use
  `PerPlayer.soundAt(loc)` with a real behind-the-player location for a *vanilla*
  (positional) sound until Simple Voice Chat is wired.

---

## 6. WILL-IT-RUN (concrete defects blocking a real playthrough, ordered)

1. **Migration `0006_requires_flags.sql` does not exist** → the `requires_flags` column is
   never created → seed activation UPDATEs silently skip → gated `active=false` rows are
   permanently dead. (VERIFIED: only 0003/0004/0005 present.)
2. **`getOpenPuzzles` (`repo.ts:361`) + Java `OracleResolver.firstMatch` ignore
   `requires_flags`** → gated `active=true` rows open day one. (VERIFIED.)
3. **No flag producers** (`IgnitionListener`/`CoopPlateListener`/`SeventhChoiceListener`/
   `UnlitDeepListener`/`RefusalRiteListener` absent) → nothing ever sets `iss_caught`,
   `seventh_named`, `undercroft_open`, `prologue_ignited` → even after #1+#2 the gated rows
   never open. (VERIFIED: grep finds none.)
4. **`prologue_ignited` is read (`autonomy.run.ts:125`) but written nowhere** + the
   `messageCreate` handler (`bot/index.ts:99`) runs only the answer-scan → the M0
   ignition/frame-break is unreachable. (VERIFIED.)
5. **`voice.recordFrameBreak()` and `recordOpenedNamed` do not exist in `voice.ts`** → the
   central scare has no voice line; `decidePrologue` can return a key with no body behind
   it. (VERIFIED: grep = 0.)
6. **`discord/src/showrunner/composer.ts` (the M5 ending assembler) does not exist** → the
   fork flags (`fork_light`, `seventh_seen_out`, `nether_forge_found`) are read by nothing;
   the ending cannot be composed. (VERIFIED.)
7. **`specscheck` is RED** — 11 active player-facing rows unclassified (`a1z26-tick-stave`,
   `forged-eighth`, `prophet-wall-comfort/-name`, `fork-light/-name`, `name-where`,
   `record-url`, `difficulty-mara`, `reckoning-rosetta`, `base-docket-reread-auto`). The
   build gate fails; several are on-ramp rows the group touches in session one. Triage: 9
   need a `NON_CIPHER_KEYS` line; `a1z26-tick-stave` + `reckoning-rosetta` are sold as
   ciphers with NO `CLUE_SPECS` forge spec (decide: build the spec or honestly demote).
8. **Cross-row answer collisions** (`the one who turned away` ×3; `a thing that can say no
   is not a wall` ×2; bare `iss`, `7`/`seven`, `threshold`) → `matchPuzzle` fires the wrong
   row/voice/flags.
9. **The opaque-sentinel terminal beats are inert** — `accepting-crouch`, `record-receives`,
   `seventh-choice`, `m4-three-hands`, `threshold-coordinate` all wait for a plugin-posted
   token that no listener produces (#3). Movement-V is unreachable.
10. **`m4-three-hands` AND-join is architecturally homeless** — the 3-leg/20s window can't
    live in `applyOutcome` (runs only post-match); it must live in `CoopPlateListener`.
11. **`setArcFlags` (repo.ts) is read-modify-write with no optimistic guard** → concurrent
    Discord + in-world solves clobber each other; a lost `iss_caught` silently seals the
    back half. Use a server-side `flags = flags || :new` jsonb merge.
12. **Java `OracleResolver.applyOutcome` never applies `set_flags`** (only enqueues the
    beat) → an in-world solve of a flag-setting puzzle advances nothing; the two surfaces
    are NOT equivalent. Either handle `set_flags` in Java or make flag-setting puzzles
    Discord-only.
13. **`BeatQueuePoller` double-fire / UNHANDLED churn** — a beat enacts before its status
    is durably flipped (replay on restart-during-poll for RoomSwap/NamedMob/KeeperNpc,
    which have no ledger idempotency); an unhandled beat type re-fetches every poll forever.
    Claim-then-act: PATCH to `firing` before enacting; mark `deferred` after K UNHANDLED.
14. **Showrunner cron has no catch-up + no liveness alarm** — a missed tick is a
    permanently-skipped drip; a CONFIRM-mode staged drip advances `last_drip_at_ms` so
    `pending_drips` piles up unposted while the engine thinks it's pacing. Add a
    `last_run_iso` staleness alarm; don't advance the cadence anchor on a staged drip.
15. **`readSignal()` reads `v_record`, a view that "has not shipped yet"** → the website
    renders the all-redacted baseline for every movement regardless of progress.
16. **Resource-pack runes font is load-bearing and un-degraded** — no `ResourcePackPusher`
    / `ResourcePackStatusEvent` gate exists; one person declining the pack sees every clue
    as tofu/Latin garbage and doesn't know why. Build the pusher with `force=true` +
    decline→reason, and keep the Discord `#the-record` mirror as the non-font fallback.

**The whole-string idempotency core (`recordSolve`/`insertSolveIfNew`) and the normalizer
agreement across TS/Java are genuinely sound — keep them.** The failure is not bad code;
it's an unfinished spine where every guard fails silent, so "broken" looks like "fine."

---

## 7. THE SIMPLER, STRONGER VERSION

### The spine that stays
Six distinct, broken keepers — **Vaun accumulates / Mara cites / Sella mirrors / Orin
breaks off / Brann doubles / Iss reassures** — whose fates mirror the group's tracked
behavior, delivered through five real, build-proven ciphers (`stone-vaun` Caesar,
`stone-mara` book, `stone-sella` atbash, `stone-orin` substitution, `stone-iss-wall`
Vigenère key=ISS). The arc is FACT 1→2→6→8→10/10b→11→12→13→14→15. The `journals-*.md`
voices are the strongest asset in the project by a wide margin — lean HARDER on the
chorus and let the ARG machinery fall away around it.

### The ONE central mystery
**The Liar and the Catch.** Iss is the warm voice who told the keepers-after that "the
ways are a wall" — a comfort that was a lie. The group trusts him, follows his warm
reading to a cold dead hearth that pays nothing, is PUSHED back by that dead-end to
re-test his own name as a key, and decodes *the one who turned away*. That single
reversal — the kind guy lied, and the land kept the proof — is the emotional engine, it's
already built and fair (`stone-iss-wall`→`iss-doubt`→`no-wall-catch`), and it needs none
of the Nether, the End, FACT 17, the difficulty reveal, the UNKEPT acrostic, or the
filing-axis thesis. The Seventh (who broke nothing and was erased) is the quiet coda; the
Accepting bow (`AcceptingRiteListener` — the strongest in-game interaction, KEEP it) is
the close.

### What gets cut to get there
Everything in §4. Net: from ~24 threads / ~21 facts / 2 extra dimensions down to one
Overworld arc, six keepers, ~6 ciphers, one Liar reversal, one Seventh coda, one bow.

### The minimal vertical slice to prove it for real (in order)
1. **Solder the nerve (one atomic change):** write migration `0006`; wire `requires_flags`
   into `getOpenPuzzles` + the Java twin; build `IgnitionListener` (or a `/obs flag set`
   admin command) so SOMETHING writes a flag.
2. **Make ignition fire:** author `recordFrameBreak()` + `recordOpenedNamed` in `voice.ts`;
   in `messageCreate`, set `prologue_ignited=true` on a keeper post in `#the-record`. Prove
   the group posts "kept" and the server says the `6` back.
3. **Green `specscheck`** (the 9 `NON_CIPHER_KEYS` lines + the 2 fake-cipher decisions) and
   **de-collide the shared answers** (#5/#8) so no two open rows share a string.
4. **Build the hint rail:** the `whisper_budgets` ledger + 2-3 `hintBody` tiers for the ~6
   spine ciphers. No playtest without it.
5. **One world-build room, statically placed** (per §7's re-tiering): one keeper-stone with
   a pre-placed labeled answer lectern, the per-player `FakeBlockBeat`/private-particle
   illusions for "it knows ME," and ONE authored reveal behind a sealed door — NOT a
   globally-unwitnessed live mutation in a shared base.
6. **Run it on a real Minecraft server with 3-4 actual friends.** Prove: ignition fires,
   one cipher is solvable with a hint, the Iss catch lands, the bow closes. Nothing past M0
   is worth building until that single loop runs once.

The north star is "From The Fog, but it knows your name" — intimate and reactive. Right
now it reads as "From The Fog, but it has 24 threads, 21 facts, 10 invariants, two extra
dimensions, and a filing-system thesis, and none of it has ever run." Cut two-thirds of
the cleverness, keep all six keepers, solder the one nerve, and prove the smallest loop on
a real server. It will land *harder* small than it ever could sprawling.
