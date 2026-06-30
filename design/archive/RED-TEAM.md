# THE OBSERVANCE — RED-TEAM (consolidated, pre-build)

> Six adversarial lenses ran against the full integrated design (INTEGRATION, WORLD-BIBLE, PLAN,
> COHERENCE-AUDIT, cipher/clue webs, the live `puzzles_seed.sql`, and the shipped plugin/forge/showrunner
> reality). This file de-duplicates them into one verdict, one MUST-FIX list, the BUILD-BLOCKER subset, the
> best new ideas, and a single "ready to build when ___" line. **READ-ONLY synthesis — this is the only
> file written; no `plugin/`, `forge/`, or `showrunner/` edits.**
>
> Every claim below was re-verified against ground truth this pass (the seed rows, `sites.yml`,
> `config.yml`, `TrackerConfig.java`, `voice.ts`, `ciphers.ts`, `clue-specs.ts`). Where a lens overstated,
> it is corrected here.

---

## 1. VERDICT + THE HONEST BOTTOM LINE

**Verdict: REVISE — buildable after a small set of blockers, but not yet.** Five of six lenses returned
`revise`; none returned `ship`; none returned `block`. The integrated design is genuinely strong where it
is built (championship-grade found-document prose, a clean player-driven Liar catch, a machine-checked
forge-spec bind, real reveal/fault-isolation discipline in the engine) and the architecture is sound. It
is **not ready to start building** because of a handful of coherence faults that would fail silently on
camera, and a content-mass illusion that would collapse the "~2 weeks" promise into a long weekend.

**The dominant risk class is CONTENT LONGEVITY masquerading as content.** The "24-node web / 24-format
catalog / 11 ciphers" headline counts are largely paper: of the 24 nodes, ~17 are single-sign `lore`/
`dead_end` rows consumed in seconds; of the 11 forge ciphers only **4** are live (book, atbash,
substitution, Vigenère) — the other 5 (railFence, columnar, polybius, a1z26, morse) are built, self-tested,
and used by **zero** active seed row; and the un-speedrunnable lever the whole longevity argument rests on
(the rumor→verify TRAVEL loop) is the **least-built thing in the project** — zero `.schem` files, no
`schematics/` dir, `sites.yml` has only generic null-coord anchors and two disabled anonymous keeper-stone
slots. A smart, pooling, ARG-veteran group clears the genuine solve content in 2–3 sessions and then stares
at a 20h drip. **Longevity must be decoupled from cadence and re-funded as authored travel + observation +
gather-events, ledgered in HOURS not nodes — before the build, not discovered live on camera.**

The second risk class is **silent coherence faults that don't error** — a custom-key namespace drift
(`kept_light` in the docs vs `the_kept_light` in the code), a graph-orphaned marquee red herring, sentinel
answers leaked in plaintext in a committed file, an unsited spine — each of which produces a zero-row join
or a null-site paste, i.e. nothing happens and nothing complains. For a documentary where "nothing breaks
on camera" is a hard requirement, these are build-blockers, not polish.

Calibrated to Ethan's reality: hard-gating stalls are correctly low-risk (4 entry paths, 6-stone field,
≥2 in-doors, Whisper backstop, hand-hinting) — **do not over-invest there.** The investment goes to (a)
real content mass with travel/observation as the time-sink, (b) group-native atmosphere primitives that
don't exist yet, and (c) the small machine-checkable coherence guards that keep 3–5× content volume from
drifting on camera.

---

## 2. STRENGTHS TO PROTECT (do not let a fix break these)

- **The found-document prose is the project's real asset.** The-ways-are-a-wall → no-wall-was-ever-built
  here; Mara's "a map is not a road walked"; the warm-liar foreshadow. Any re-authoring (e.g. the Liar
  catch, the paraphrase rows) must preserve this voice, not flatten it.
- **The player-driven Liar catch is already correct in the seed.** `no-wall-catch` is `main_beat`,
  sets `iss_caught`/`true_coord_known` player-side, enqueues the warm→cold beat; the showrunner never
  touches the flag. **Keep `outcome_payload` exactly as shipped** — only the `accepted_answers` half is
  hollow (MF-7). Do not let a doc-following builder re-introduce the retired offstage flip.
- **Iss's Vigenère key = his own name = "the one who turned away"** is the best puzzle in the set
  (self-verifying, in-corpus, un-Googleable, emotionally load-bearing). The catalog should aspire to it;
  do not dilute it.
- **The X1 forge-spec bind (`forge/clue-specs.ts`) is a working, machine-checkable coherence ledger.**
  `specsSelfTest()` proves `decode(forge(spec)) === plaintext` AND that the seed answer matches — a wrong
  shift/key/edited answer fails the BUILD. This is the template the thread/site/custom layers must copy;
  protect and generalize it, never bypass it.
- **Engine production discipline is real and enforced in code:** reveal discipline
  (`AbstractBeat.mutateWhenUnwitnessed`), all-or-nothing footprint validation, pervasive `Throwable`
  fault-isolation (degrade to skip, never crash), the DramaBudget hard-cap with refund-on-non-fire, the
  idempotent customs high-water report, the watcher-sleep kill-switch. The failure direction is correct
  (a beat silently *doesn't* fire, never fires janky) — protect this on every new beat path.
- **The anti-stall web shape** (4 entry paths, ≥2 in-doors per gate, 6-stone field, resolver ignores
  movement, Whisper backstop, `dead_end`-as-acknowledged-outcome). Correct for this audience; do not
  "fix" it into a linear gauntlet.
- **One voice / one state / one resolver** holds in code (`voice.ts` sole text source; `customs.ts`
  composes no English; both surfaces resolve one `puzzles` table). The deliberate two-register split
  (Watcher = `voice.ts`; surface NPCs = human register) is correct, not an accident — preserve it.

---

## 3. MUST-FIX (de-duplicated, prioritized) — merged across lenses

> Each: **issue · why it matters · the concrete fix · lens(es).** `[H]`/`[M]`/`[L]` = severity. Items the
> build must clear *first* are also tagged **(BLOCKER)** and re-listed in §4.

### HIGH

**MF-1 [H] (BLOCKER) — Content longevity is a long weekend, not two weeks; the node/cipher/format counts
are illusory.**
*Why:* ~17 of 24 nodes are single-sign lore/dead-ends (seconds each); only 4 of 11 ciphers are live and all
4 are textbook families this audience has solved a hundred times; "2 weeks" is carried by the 20h drip +
fallow days, i.e. cadence padding a fast pooling group ignores or resents. They hit the end of genuine
material in 2–3 sessions.
*Fix:* Decouple longevity from the drip. Set a HARD content budget and re-fund longevity onto play-TIME
(travel, observation-over-days, group coordination, building) not interval-gating: (a) inflate the RUMORED
tier from "a handful" to **25–35 distributed rumor→verify pairs**, each a real 1000–3000-block journey to a
sited tableau where the verify often *contradicts* the rumor; (b) seed **4–6 cross-document correlation
puzzles** (hold two distant docs together; the answer is the contradiction — un-poolable because it needs
broad reading first); (c) make 60%+ of breadth require being-somewhere-at-a-time or across sessions, not
decoding. Treat the drip as a SLOW-group safety floor only. *(Lens 1, Lens 2, Lens 6)*

**MF-2 [H] (BLOCKER) — Zero authored world content exists to walk to; the longevity argument rests on an
unbuilt corpus.**
*Why:* Verified — **0 `.schem` files, no `schematics/` directory**, `sites.yml` has only generic anchors
with `null` coords plus two disabled anonymous `keeper_stone` slots. The dead-shrine, seventh-shrine, six
keeper-stones, Undercroft, and every rumor→verify destination are unbuilt. "A 2000-block walk can't be
whispered" is true — but there is currently nothing at the end of any walk.
*Fix:* Make the breadth corpus the gating PRE-BUILD metric. Define "ready" as N sited destinations with
authored `.schem`/inline content delivering the hours target (~20–30 discoverable sites for two weeks of
4-hr nights). Build rumor→verify destinations FIRST (the time-sink), keeper-stones second, set-piece peaks
last. Track authored-site count against an hours target in the ledger. *(Lens 2, Lens 6, COHERENCE-AUDIT
A1–A6)*

**MF-3 [H] (BLOCKER) — Custom-key NAMESPACE DRIFT: code uses `the_`-prefixed keys; every design doc uses
the unprefixed form.**
*Why:* Verified — `TrackerConfig.java` and `voice.ts` `CUSTOM_PHRASES` ship `the_kept_light`,
`the_deep_line`, `the_dark_hours`, `the_sacred_beast`, `the_bow`, `the_offering`, `the_unspoken`; but
WORLD-BIBLE §5 and INTEGRATION §3.2 write `kept_light`, `deep_line`, `dark_hours`, `sacred_beast`. The
proposed NEW tables key off these strings (`punishment_state.custom_key`, `puzzles.teaches_custom`). A
builder seeding from the docs writes `dark_hours`, the join returns **zero rows**, the punishment loop never
escalates — and **nothing errors.** Worst possible failure mode for an on-camera ARG.
*Fix:* Adopt the code's `the_`-prefixed namespace as canonical. (1) Global-replace the unprefixed forms in
WORLD-BIBLE §5, INTEGRATION §3.2, COHERENCE-AUDIT, rosetta-ring. (2) Add a `CUSTOM_KEYS` registry of the
seven legal keys to the `clue-specs.ts` harness, asserted `=== Object.keys(CUSTOM_PHRASES) === TrackerConfig
constants === every custom_key the seed/punishment_state references` — a typo fails the build, exactly as
X1 makes a cipher typo fail the build. *(Lens 5)*

**MF-4 [H] (BLOCKER) — `unbroken_light` (the Movement III→V spine site) is referenced 3× in the seed but
ABSENT from `sites.yml`.**
*Why:* Verified — `undercroft-descent`, `rite-tokens`, `accepting-crouch` all enqueue beats at
`site_id:'unbroken_light'`; `sites.yml` has no such site. The Undercroft door, the token altar, and the
Accepting bow all paste-resolve to a **null site**. The whole back half of the run silently no-ops.
*Fix:* Author `unbroken_light` + the six named keeper-stones + the Stone of Reckoning as real ENABLED sites
with real coords. Add a coverage self-test (mirror `specsCoverageSelfTest`) that parses every `site_id` in
the seed and asserts each exists AND is enabled in `sites.yml`, failing the build on an unsited beat.
Correct INTEGRATION's stale "no sites.yml" wording to "sites.yml exists but lacks the spine anchors."
*(Lens 5, COHERENCE-AUDIT A1)*

**MF-5 [H] (BLOCKER) — Sentinel answers for the detected rites are committed in PLAINTEXT in the public
seed.**
*Why:* Verified — `accepting-crouch` and `record-receives` carry `'a7f3 accepting bow sentinel posted only
by plugin'` and `'e0c4 record receives sentinel posted only by plugin'` directly in `puzzles_seed.sql`. Any
player who reads the repo (or guesses the obvious pattern) can type the terminal `main_beat` and **spoof the
Accepting without performing the rite** — the climactic on-camera judgment, defeated by a string.
*Fix:* Replace both with long opaque high-entropy tokens generated and held ONLY by the plugin (never in any
committed corpus), in charset `[a-z0-9 ]` so the stored value equals its own normalized form. Add a
seedcheck assertion that no sentinel row's `accepted_answers` contains `'sentinel'` or `'posted only by
plugin'`. *(Lens 1, Lens 5)*

**MF-6 [H] (BLOCKER) — The marquee red herring `iss-dead-shrine` is GRAPH-ORPHANED (no inbound edge).**
*Why:* Verified — `stone-iss-wall.next_puzzle_key` is `'iss-doubt'` ONLY; **no row routes to
`iss-dead-shrine`.** The design's self-described "highest-value digression / the false way up" exists as a
node and as prose but is **unreachable by play** — a trusting group is never baited to the grave.
*Fix:* Split `stone-iss-wall` into the two-door shape the design specifies (warm reading → `next_clue` →
`iss-dead-shrine`; name-as-key → `iss-doubt`), OR give `iss-dead-shrine` a real inbound edge from the warm
answer. Add a graph-integrity self-test: every `next_puzzle_key` resolves to a real row; every node the
docs tag "red herring" has ≥1 inbound edge (reachability). *(Lens 1, Lens 5 / COHERENCE-AUDIT F3)*

**MF-7 [H] — The signature Iss catch is hollow: `no-wall-catch.accepted_answers` are phrases read VERBATIM
off the answer stone.**
*Why:* Verified — accepted answers are `'no wall was ever built here'`, `'what iss sent you to was a grave'`,
`'back to vauns stone turn down'` — the exact lines delivered by D10. It reads as "type the phrase off the
stone," not "you caught him." The single best emotional beat in the arc is mechanically a transcription.
*Fix:* Re-author so the accepted answer is the DERIVED contradiction, reachable only by holding D09 against
D10/the other stones (make D09 boast something independently checkable, e.g. "turn my key on any honest
stone and it will agree" — which the cross-check falsifies with a concrete result). Submitting that result
PROVES the catch. Remove the verbatim D10 lines. **Keep `outcome_payload` exactly as shipped.** *(Lens 1 /
COHERENCE-AUDIT F2)*

**MF-8 [H] — No SYNCHRONIZED-GROUP beat primitive exists; every sensory beat is single-target. The
Accepting (terminal group judgment) has no group-crouch detector.**
*Why:* Verified intent — `PerPlayer.*` beats take exactly one `targetPlayer()`; the showrunner is a
Discord/DB conductor that never selects an in-world target; the "creepy-beat selection pass" is unbuilt.
There is no primitive to fan one logical beat to a convened group on the same tick — which is the entire
premise of a gather-event. And `accepting-crouch`'s only real surface is a single-player crouch + a
spoofable sentinel; `trigger_accepting` (the dashboard button) is NOT registered in BeatLibrary, so the
manual path silently no-ops.
*Fix:* (Plugin-owned — flag the concurrent session.) Add a **GroupBeat fan-out**: given a region/convened
set, iterate and invoke the existing `PerPlayer.*` calls for each member on the same tick, with optional
0–10-tick per-player jitter (organic, not a cutscene). Drive it from an all-present/convergence trigger,
not the Discord showrunner. Build a real group-crouch sentinel for the Accepting (≥N distinct players
sneaking in-radius in a tick window, fired deterministically like the Liar catch). Register
`trigger_accepting` so the override can't no-op. *(Lens 3, Atmosphere lens, Lens 6)*

**MF-9 [H] — Reveal discipline is a SINGLE-CAMERA assumption and breaks for a clustered group; per-player
sensory beats DESYNC across co-located players on camera.**
*Why:* `Reveal.isHidden()` returns true only when NO online player is within `witnessRadius` with line of
sight. For 4–6 friends convened (the premise of every gather-event), the union of sightlines leaves almost
no clear block — so the whole Tier-A/Tier-C "unwitnessed change" catalog silently never fires exactly when
the group is together. Separately, `PrivateTimeShiftBeat`/`PrivateDarkness`/per-player fog set state for ONE
target — so one client jumps to night+storm while the player beside them films noon, and the POV cut exposes
it (also half-applied solo: mob spawns key off real world-time).
*Fix:* Split atmosphere into two render paths. (1) GATHER-EVENT-wide effects get a region-scoped path that
does NOT use `isHidden` — a designed simultaneous event the group is MEANT to witness (client-driven
light-block/redstone toggles + a GroupBeat Darkness pulse). (2) Keep reveal discipline ONLY for
lone-wanderer beats and proof-of-presence changes discovered on RETURN (pre-stage those when the region is
empty). Add a `witnessed_ok:true` beat flag so authors choose the path. Add a `SceneAwareness.isAloneEnough`
guard so per-player sensory beats skip/downgrade if any other player is within `witnessRadius` — make the
design's "when alone" rule a code invariant. *(Lens 3, Atmosphere lens)*

**MF-10 [H] — `NamedMobBeat` hard-falls to ZOMBIE on any unparseable name; the stand-and-stare Watcher
renders as a short green zombie, and the group can surround it.**
*Why:* Verified (COHERENCE-AUDIT E1) — `entityType()` does `valueOf(name.toUpperCase())` and hard-returns
`ZOMBIE`; there is no MythicMobs resolution. `mythicmob:watcher` → comedic zombie. The despawn retry calls
`remove()` with no `isHidden` re-check (can vanish in plain sight). And the apparition is single-target,
spawns relative to one player, may stand in plain lit view of the other five — who walk up to a frozen,
invulnerable, named mob (censor-box discipline destroyed).
*Fix:* (Plugin-owned — flag.) Honor a payload `fallback_entity` defaulting to WARDEN/STRAY (tall
silhouette), never ZOMBIE; reject-and-skip rather than spawn a wrong-read entity; re-check `isHidden` on
despawn retry. Gate the lone apparition on isolation + low light (reuse `distanceFromGroup`), cap visible
lifetime, vanish-unwitnessed on first clear window. For the GROUP apparition use a per-player client-only
glimpse (<1s, render-edge, fog, never the same instant, never approachable). **Until fixed, author ALL
apparition payloads with a plain vanilla entity — no `mythicmob:` ids.** *(Lens 3, Atmosphere lens, Lens 6,
COHERENCE-AUDIT E1)*

**MF-11 [H] — No resource-pack load detection; rune-font signs render as tofu boxes for any player who
declined / joined mid-download / failed the SHA-1, and the world visibly disagrees with the Discord card.**
*Why:* The cipher spine is rune-font text on signs/cards. A custom glyph the default font lacks renders as
missing-glyph boxes (not graceful fallback). Discord cards forge server-side and render fine, so world and
archive disagree on camera.
*Fix:* Listen to `PlayerResourcePackStatusEvent`; gate rune-bearing beats on `SUCCESSFULLY_LOADED` per
player; set `require-resource-pack` OR ship a plain-ASCII transliteration sign variant so a stone is always
readable. Surface per-player pack status on the dashboard; pre-flight: every recording client shows pack
LOADED before any rune scene. *(Lens 3)*

**MF-12 [H] — Content is ~85% one verb (read → decode → log); zero authored adventure/traversal, only one
gather-event (the finale), zero authored character side-arcs.**
*Why:* The two real corpora (12 found-docs + 23 seed nodes) resolve almost entirely to one player reading
and typing a string. The descent is a decode-gated DOOR, not a journey whose payoff is discovery. A 6-friend
group never convenes around anything meaningful mid-arc — a documentary with exactly one on-camera
convergence has no middle. The six keeper "side-stories" in INTEGRATION §3.4 are one-line briefs, never
authored.
*Fix:* Add content in ≥3 non-reading verbs (traverse/discover, perform-as-group, protect/hunt): the
Lamp-Walk descent (light is the path, not a cipher), the wrong-scaled gallery (geography answers HUMAN with
no words), the Deep Market authored FIRST as a lived-in plaza. Author 3 mid-arc gather-events (the Going-Out
re-enacted, the Offering together, the deep-bird vigil). Author ≥2 standalone keeper arcs that gate nothing
and pay character/texture debt (Sella's drawings in date order; the Mara&Sella adoptive bond). See §5.
*(Lens 6, Lens 2)*

### MEDIUM

**MF-13 [M] — The "holy shit" induction twist is the single most predictable resolution for this audience
(Rusty Lake / folk-horror / SCP veterans); the fairness foreshadows read as flashing arrows.**
*Fix:* Don't rely on the base twist as the peak. Add a SECOND-ORDER turn that survives them guessing it:
(a) make the personal token each player CHOSE at the M5 rite (told it was "just flavor") be the instrument
that writes them into the record — the horror lands on a re-read of their own on-camera agency, which no
veteran can pre-spoil because it depends on their real choices; (b) reveal the recovery-archive the players
built IS the next found-document for whoever comes after (the documentary/lure recursion). Let the induction
twist be the thing they feel smart for guessing; land the real turn on top. *(Lens 1)*

**MF-14 [M] — The non-linear web's 1:1:1:1 regularity (each keeper = one cipher = one custom = one rhyming
fate) is a legible taxonomy; cracking two stones lets a veteran pre-template the rest.**
*Fix:* Deliberately break the regularity on ≥2 keepers: one keeper with NO cipher (pure tableau/inference),
one custom mapping to two keepers, one node assembled ONLY by cross-document correlation (no artifact of its
own). Withhold the "rhyming chorus" — let the group derive it as a late revelation, not meet it as a clean
grid (`m2-rhyme` currently hands it to them). *(Lens 1)*

**MF-15 [M] — Multiple live rows accept paraphrasable English, violating the project's own "correct must be
OBVIOUSLY correct" law; `seventh-shrine` also accepts `'seven'`/`'7'`.**
*Why:* Verified — `stone-brann`, `m1-named-habit`, `m2-rhyme`, `seventh-shrine`, `undercroft-fog`,
`haunting-biography` each take 2–4 near-synonym sentences; `seventh-shrine.accepted_answers` includes
`'seven'` and `'7'`.
*Fix:* Convert paraphrase rows to either a self-verifying decoded artifact (a name/date/coordinate obviously
right) or a pure found-document with NO oracle gate (read it, it's read). Reserve typed `accepted_answers`
for one-undeniable-solution inputs. Cap `seventh-shrine` or require the derived bearing, not the number.
*(Lens 1)*

**MF-16 [M] — The 5 orphan ciphers are advertised as live but are unseeded; promoting them risks
ambiguous short ciphertext with multiple readable decodes (camera can't confirm a solve).**
*Why:* Verified — railFence, columnar, polybius, a1z26, morse exist and self-test in `ciphers.ts` but no
active seed row uses them and no document teaches them. Promoting them (PATH A) is the right move for
variety, but short ciphertext under multiple in-play ciphers can have >1 readable decode.
*Fix:* SEED the orphans onto real nodes with placed Rosettas (railFence on a night-gated Brann keyed to
counted fires; polybius on a Deep-Market marker-grid = "the world is the codebook"; columnar on a late door;
a1z26/morse as early rungs) — move from ~4 to ~9 live ciphers. Author each so its ciphertext has exactly one
readable decode under exactly one in-corpus-taught cipher; add a pre-flight LINT that runs every ciphertext
through the OTHER decoders for accidental near-words; store both coordinate signs / anchor each coord to one
confirmable landmark. Until seeded, stop advertising the orphans in the catalog. *(Lens 1, Lens 3,
COHERENCE-AUDIT B1)*

**MF-17 [M] — `rosetta-ring` (the master literacy key) teaches `ward covering` — two inert customs — while
Dark Hours and the Unspoken (both detected in code) are taught NOWHERE.**
*Why:* Verified — `rosetta-ring.accepted_answers` is `'bow offering kept light deep line ward covering'`;
Ward/Covering have no detection, keeper, or consequence; `the_dark_hours` and `the_unspoken` are real
`TrackerConfig` detections taught by no document.
*Fix:* Demote Ward/Covering and promote the two real detected customs into the ring — edit
`rosetta-ring.accepted_answers`, `learn-them-as-we-learned-them.md` (D03), cipher-web §1, clue-web §3
together in one change. Pairs naturally with seeding the Unspoken + Dark Hours. *(Lens 5, COHERENCE-AUDIT
D5)*

**MF-18 [M] — The Unspoken can never fire (`forbidden-words: []`), yet the integration plan leans on it
harder (adds `unspoken-refrain` as a teaching surface).**
*Why:* Verified — `config.yml` line 107 ships `forbidden-words: []`, so `containsForbidden` is always false.
*Fix:* Author the per-arc forbidden word(s) in `config.yml` (the trigger, never a displayed string — canon's
"never write the Unspoken" holds), seed `unspoken-refrain`, wire the payoff through the customs bridge. Add
a boot assertion: if any custom is referenced by a seeded clue/`punishment_state` row but its trigger config
is empty, log a loud warning. *(Lens 5, Lens 2, COHERENCE-AUDIT D3)*

**MF-19 [M] — The customs/discover-by-punishment layer is mostly inert and, even built, is a learn-once
event, not an ongoing longevity layer.**
*Why:* Two of seven customs can never fire (Unspoken `[]`; Ward/Covering have no mechanic); three side-quest
customs (`unspoken-refrain`, `haunted-herd`, Dark-Hours perform) were never seeded; the customs→report
bridge gates the loop. Even fully built, "decipher the way from consequence" is 7 one-time learnings, not 7
weeks.
*Fix:* Convert customs from learn-once to an ongoing PRACTICE layer: make ≥2–3 require sustained group
adherence across the run with escalating stakes (the deep-bird kept alive for the FULL arc to earn the M5
boon is the right model — generalize it). Seed the three missing custom-puzzles and fire the Unspoken before
counting any of this toward duration. Demote or build Ward/Covering. *(Lens 2)*

**MF-20 [M] — The drip is over-trusted and mis-tuned: it opens on a `dead_end`, surfaces one node/day, and
a fast exploring group out-runs it while a waiting group is starved.**
*Why:* `decide.ts` sorts the un-dripped pool `(movement asc, key asc)`, so the first live drip is
`m1-named-habit` (a `dead_end`); `decide.selftest` test 7 masks it by omitting that row.
*Fix:* Fix drip ordering to never open with `dead_end`/`lore`/sentinel rows (lead with an entry/`next_clue`
node); add a selftest case including `m1-named-habit`. Re-cast the drip as a BACKFILL for a slow group; add
a dashboard adaptive valve that opens MORE standing content when a group's solved-count outpaces the drip.
Give fallow days a positive payload (archivist-decay, an it-knows-me beat, a world-change-on-return). *(Lens
2, Lens 3, COHERENCE-AUDIT C2/C3)*

**MF-21 [M] — The showrunner customs cron and drip post to Discord on their own schedule, decoupled from
the on-camera moment; there is no interlock to hold Discord while the director stages a peak.**
*Fix:* Add a `hold`/`quiet_hours` settings flag the dashboard sets, honored by BOTH the customs pass and the
drip, so Discord goes silent while a peak rolls. Keep the immediate Tier-A felt consequence in the plugin
(fires on the violation tick); let the cron handle only the slower observe→warn→left naming. *(Lens 3)*

**MF-22 [M] — FAWE paste runs on the main thread with no atomicity (half-stone left unprotected on a
mid-op throw — the Java-21-vs-25 FAWE trap), a near-32k paste hitches the tick, and FAWE-absent silently
skips a scripted gate stone.**
*Fix:* Lower the runtime paste ceiling far below 32k (keep big set-pieces deploy-time only); on paste failure
sweep-and-clear the footprint back to pre-paste state; add a startup self-check that logs LOUDLY if FAWE is
absent/mismatched while any `active=true` puzzle references a `schematic:`; pin and verify the exact Java-21
FAWE build in pre-flight. *(Lens 3)*

**MF-23 [M] (BLOCKER) — The thread/recovery-archive skeleton (5 new tables + 2 columns) has ZERO binding —
no X1 equivalent — and is the layer most certain to drift at 3–5× volume.**
*Why:* None of `threads`/`thread_cards`/`side_quests`/`punishment_state`/`npc_dialogue_state` or
`puzzles.thread_key`/`teaches_custom` exist yet, and nothing would assert that every active row carries a
valid `thread_key`, that `references_card_key[]` point at real cards, or that `total_fragments` equals the
count authored.
*Fix:* Before authoring thread content, write the thread-layer coverage invariant FIRST (mirror
`specsCoverageSelfTest`): every active row has a `thread_key` in the 5-thread enum; every
`references_card_key[]` resolves; `revealed_by_solve` points at a real `puzzle_key`; `thread.total_fragments
== COUNT(cards tagged)`; `teaches_custom` uses a legal key (ties to MF-3). Ship the migration + self-test as
one change. *(Lens 5)*

**MF-24 [M] — Stale watchers accumulate and map-ids bloat over a 2-week run; a mid-campaign reload orphans
in-flight beats.**
*Fix:* Give `named_mob` a default `despawn_seconds` ceiling and a reload-time PDC sweep of
`beat_entity`-tagged mobs; pool/reuse map ids; add a "cull all watchers" dashboard button for between-session
cleanup so no stale apparition is discovered cold. *(Lens 3)*

**MF-25 [M] — Reveal occlusion samples only a structure's BASE block and `hasLineOfSight` is FOV-gated, so a
tall stone's top can be visible while its base is hidden, and a set-piece can pop into a wide establishing
shot (render distance >> witnessRadius).**
*Fix:* Sample reveal across the footprint's visible extremes (top corners), not just the base; add a stricter
"no player has this chunk loaded-and-near" mode for WORLD-tier set-pieces; prefer pre-staging hero set-pieces
during empty-server fallow windows over live paste. *(Lens 3)*

### LOW

**MF-26 [L] — `OracleVoiceKey` omits `oracleWithheld`, which `resolve.ts` actually dispatches** — the type
is a partial mirror of the dispatch table it claims to keep in sync. *Fix:* add `oracleWithheld` to the type
or split into `PayloadVoiceKey` vs `InternalVoiceKey`. *(Lens 5)*

**MF-27 [L] — The Phase-2 Citizens2 NPC layer can show a dead dialogue stub on camera, and the "NPCs speak
HUMAN register, never `voice.ts`" rule is convention-only.** *Fix:* contract it now — every dialogue node
needs a safe terminal fallback line; add a lint rejecting any NPC string in the Watcher's register and any
NPC line sourced from `voice.ts`. *(Lens 3, Lens 5)*

**MF-28 [L] — Doc drift will re-break shipped reality.** Three docs + the sealed JSON still describe the
retired offstage `iss_caught` flip; INTEGRATION says "no sites.yml" when it exists; "black moon" (story) vs
full-moon-phase (DarkHoursListener) terminology conflicts. *Fix:* a doc-reconciliation pass with a short
"SHIPPED REALITY" header per design doc, verified against the seed (assert no showrunner code reads/writes
`iss_caught`; reconcile the moon phase before wiring any dark-hours beat). *(Lens 5, COHERENCE-AUDIT
F1/D2/E3)*

---

## 4. BUILD-BLOCKERS — must be resolved BEFORE building starts

> The subset of §3 that fails silently on camera or invalidates the longevity premise. The rest of the
> MUST-FIX list can be worked DURING the build. Order matters: the coherence guards (B-3..B-7) are cheap and
> stop the content work (B-1, B-2) from drifting as it scales.

1. **B-1 = MF-1 — Re-fund longevity as authored TRAVEL + observation + gather-events, budgeted in HOURS,
   not nodes/drip.** Until the breadth plan exists on paper with an hours target, "2 weeks" is unproven and
   the build aims at the wrong thing.
2. **B-2 = MF-2 — Build the world content to walk to (the `.schem`/site corpus), rumor→verify destinations
   FIRST.** Make authored-site-count vs hours-target the gating pre-build metric.
3. **B-3 = MF-3 — Pick the canonical `the_`-prefixed custom-key namespace and add the `CUSTOM_KEYS` build
   assertion.** Do this before seeding any `teaches_custom`/`punishment_state` row, or the joins drift dead.
4. **B-4 = MF-4 — Author `unbroken_light` + the named keeper-stones + Stone of Reckoning in `sites.yml`,
   and add the site-coverage self-test.** The back half of the run is null-sited until this lands.
5. **B-5 = MF-5 — Replace the plaintext rite sentinels with plugin-only opaque tokens + the
   no-leaked-sentinel assertion.** The finale is spoofable until this lands.
6. **B-6 = MF-6 — Give `iss-dead-shrine` a real inbound edge + add the graph reachability self-test.** The
   marquee herring is unreachable until this lands.
7. **B-7 = MF-23 — Write the thread-layer coverage invariant (the X1-equivalent) BEFORE authoring thread
   content.** This is the guard that keeps 3–5× content volume coherent on camera.

> Plugin-owned blockers to FLAG to the concurrent engine session before content goes live (not this
> session's to fix, but the slice cannot be filmed without them): **MF-8** (GroupBeat fan-out +
> group-crouch Accepting detector + register `trigger_accepting`), **MF-9** (two-path reveal +
> `SceneAwareness`), **MF-10** (`fallback_entity`, no ZOMBIE), **MF-11** (resource-pack gate). These are the
> "nothing breaks on camera in a GROUP" preconditions.

---

## 5. THE BEST NEW IDEAS TO FOLD IN (grouped)

### Real-longevity content (the un-poolable time sinks)
- **Travel as the spine of longevity** — 25–35 sited rumor→verify destinations 1000–3000 blocks apart where
  the verify often CONTRADICTS the rumor. A pooling group shares the rumor instantly but must still
  physically split up and walk; the only mechanic that scales with wall-clock AND group size at once.
- **Observation-over-days threads that cannot be rushed** — a self-rewriting journal that completes its
  tonal-decay arc only across 5+ real-night reads (NBT swap out of LoS, one step/night); a build readable
  only across 3 moon phases; proof-of-presence traps the group sets themselves and checks over days. None
  pool, none whisper.
- **The False Way Up as the longest single digression** — build Iss's dug shaft as a multi-session
  expedition that mimics the win condition (an "almost-exit" ending in fill). A grinder's own momentum
  becomes the time-sink; "being right fast is the trap." (Pairs with fixing its orphan edge, MF-6.)
- **4–6 cross-document correlation puzzles** — hold two distant docs together; the answer is the
  contradiction. Highest value-per-byte for this audience and the one shape that resists single-night
  clears (needs broad reading first).
- **Restoration / competence-first set-pieces** — Restore the Lamp-Works (a real redstone/lantern build that
  works and feels like a win, later found dark again by no hand = proof-of-presence); the deep-bird vigil.
  Non-padded time because building/surviving is intrinsically theirs, and it earns the later dread.
- **Ledger the HOURS, not the nodes** — add an "estimated genuine play-minutes" column (travel + observation
  windows + gather-event duration + solve time) summed per movement against a hard target (~40–50 hrs).
  Makes the 3-day-exhaustion risk machine-visible during authoring.
- **Adaptive content valve on the dashboard** — track solved-count vs elapsed-time per group; when a group
  is clearly fast, open additional standing side-content rather than waiting on the fixed drip. The right
  answer for "smart grinders Ethan can hand-hint."

### Multiplayer atmosphere + gather-events (re-inventing the single-cam Wifies tricks for a group)
- **THE UN-LIGHTING (flagship gather device)** — when an all-present detector confirms the group is
  convened, run a designed simultaneous darkening they are MEANT to watch: a light-level sweep radiating
  outward from the group's centroid + a GroupBeat Darkness pulse so every screen dims at once + one low
  drone, ending on one lamp left lit (the Kept Light motif). No reveal discipline, no jank — the horror is
  it happened TO ALL OF THEM. Directly pays "the Dark takes the unlit."
- **THE COUNT (a roll-call that comes back wrong)** — at a marker, the rite is for everyone to bow together
  (reuses the all-present detector); the world tolls once per person present, then tolls ONE TIME TOO MANY.
  The extra toll is the Watcher counting itself among them. Pure sound, deterministic, no entity — and it
  seeds the induction twist ("the record counts the living") as an ATMOSPHERE beat, months before it's
  stated.
- **THE CIRCLE OF STEPS** — GroupBeat-fan a "step-from-behind" `PrivateSound` to each member with 0–10-tick
  jitter; the group collectively perceives a ring closing in. Corroboration ("did you hear that?" "yeah,
  behind me too") becomes the dread instead of killing it. The cleanest group re-invention of the half-beat
  footstep, needing only the fan-out over an existing beat.
- **PRIVATE NIGHTFALL** — fan `PrivateTimeShift` (client time → deep night, world untouched) to the whole
  convened group for ~8s, then snap back. Deniable individually, un-deniable collectively — the group
  inversion of single-player deniability.
- **DOB, THE ONE YOU KNEW (the group-safe custom-mob gather-moment)** — the surface NPC who descended WITH
  the group, found below as a named, silent, lantern-eyed Watcher in heavy fog. Because they ALL knew him and
  ALL see him at once, the horror is shared recognition, not the model — no censor-box management. Cap the
  look (fog-in before anyone reaches him). The induction twist made flesh; ship it with a plain vanilla
  entity (WARDEN/STRAY), not the zombie fallback.
- **Return-frame proof-of-presence (stronger with a group)** — alter the base/home only while the region is
  player-empty (between sessions). With six people, multiple players independently notice; the confirmation
  loop ("was that there yesterday?" "no — I built that wall") is the beat. Group shared memory is more
  reliable than a lone player's, so violating it is more unnerving.
- **Audio is the one channel where "someone is always looking" doesn't apply** — the friends are in voice
  chat: fan a whisper to half the group and silence to the other half on the same tick ("I hear whispering"
  / "I don't hear anything") to split the party's trust in their own ears. Lean on audio as the primary
  group atmosphere channel.
- **Attention-as-weather (group band)** — give the unbuilt Attention accumulator a GROUP-level band: when
  the convened group collectively transgresses, the shared atmosphere thickens for everyone in the region
  (fog up, ambient down, more frequent GroupBeats). Consequence they caused together and feel together — more
  filmable than six independent meters.
- **The Accepting Rehearsal** — stage a smaller all-bow gather mid-arc to teach the convergence ritual and
  rehearse the all-present detector on camera safely, so the terminal P16 bow isn't the first time the
  detector fires live.

### Harder/deeper twists for veterans (survive the genre-savvy guess)
- **The chosen token writes you in** — the "just flavor" personal token at the M5 rite is the instrument
  that records each player; the peak lands on a re-read of their own on-camera agency. Un-pre-spoilable
  because it depends on their real choices.
- **The documentary/recovery recursion** — the decaying Discord "Archivist" is the PREVIOUS kept group
  cataloguing the new arrivals exactly as the players catalogue the keepers; the recovery-archive the
  players build IS the next found-document for whoever comes after. Reframes every session retroactively.
- **A cipher keyed on the GROUP'S OWN dossier behavior** (most-trodden coordinate, the first death's
  location, who whispered most) rather than an in-corpus name — self-verifying, un-Googleable,
  un-templatable, turns the "it knows me" layer into a solving surface. The one cipher family a Rusty Lake
  veteran has demonstrably never solved.
- **A keeper whose "lie," caught with the exact cross-document method that catches Iss, turns out to be
  TRUE** — punishes the pattern the group learned on Iss and breaks the legible-taxonomy assumption.
- **A dead_end PROMOTED to a door via conditional re-read** — a node that said "true, and it opens nothing"
  becomes a door once other fragments are held. The rarest ARG feeling: the game was deeper than the first
  model of it.

### Production-safety patterns
- **Dashboard "ALL-CLEAR BEFORE FIRE" interlock** — for any staged peak, show each recording player's
  position + co-location (blocks the desync class), resource-pack status, whether the spot is reveal-hidden
  for the intended audience, and FAWE-healthy + `.schem`-exists; keep the Force button DISABLED until the
  relevant checks are green. Turns "hope the timing works" into "fire only when the frame is safe."
- **SceneAwareness (group-aware reveal v2)** — generalize `Reveal` into a helper that knows per target who
  is in-frame-range, alone, and co-located; each beat category declares a policy (per-player-sensory →
  target must be ALONE; WORLD set-piece → no player loaded-and-in-render; AMBIENT → current). Makes the
  desync bug class structurally impossible.
- **Pre-stage hero set-pieces during empty-server fallow windows** and reveal them with a cheap
  DoorOpen/fog-lift — a revealed-by-opening structure can never half-paste or pop on camera.
- **A runnable go-live pre-flight SCRIPT (not prose)** — the red-herring/ambiguity lint, FAWE + `.schem`
  presence check, resource-pack glyph-coverage check, and a "no `mythicmob:` ids in any active payload" scan.
  Each is a mechanically-checkable camera-break source that should gate launch.
- **Between-session world-diff view** — what beats fired since last session and where, so the director keeps
  intentional proof-of-presence and culls the rest; the world never accidentally shows the same scare twice.

### The coherence machinery (turn discipline into failing builds — generalize the X1 bind)
- **A CANON REGISTRY in the forge self-test harness** — one module exporting the closed sets everything keys
  off: `CUSTOM_KEYS` (the 7 legal `the_`-prefixed strings), `KEEPERS`, `THREADS` (5 colors), `FACTS` (1..15),
  `SITE_TYPES`. Assert at build time: `TrackerConfig == CUSTOM_KEYS == keys(CUSTOM_PHRASES)`; every keeper /
  `thread_key` / `teaches_custom` / `site_id` used anywhere is in its registry. A typo becomes a failing
  build (closes MF-3 permanently).
- **A reachability + ledger self-test** — every `next_puzzle_key` resolves; every main-spine node is
  reachable from an entry node; every "red herring" node has ≥1 inbound edge (closes MF-6); every FACT 1..15
  is paid by ≥1 active fragment; every authored fragment pays ≥1 FACT or carries explicit
  `texture_debt=true`. "If it pays no debt it is cut" becomes enforceable.
- **A site-coverage self-test** — every `site_id` in the seed exists and is enabled in `sites.yml` (closes
  MF-4).
- **An authoring-contract lint per active row** — does it have (1) a forge-spec OR a `NON_CIPHER_KEYS`
  reason, (2) a resolvable `site_id` if it enqueues a sited beat, (3) a `thread_key`, (4) a legal
  `teaches_custom` if it claims to teach one, (5) a ledger debt. The four MECHANIC/STORY/CLUE/INTERACTION
  layers become five green checks per row — "no half-woven feature ships" verified mechanically.
- **A two-register lint** — NPC lines are NOT sourced from `voice.ts`; nothing outside `voice.ts` emits a
  Watcher-register string. Protects the deliberate voice split greppably, not by reviewer vigilance.

---

## 6. ADDRESSED → READY TO BUILD (the checklist)

**Seed (`discord/supabase/seeds/puzzles_seed.sql`):**
- Re-author `no-wall-catch.accepted_answers` as the derived contradiction; remove verbatim D10 lines; keep
  `outcome_payload` (MF-7).
- Add the warm-reading edge `stone-iss-wall → iss-dead-shrine` (or split into two doors) (MF-6).
- Replace `accepting-crouch` / `record-receives` plaintext sentinels with plugin-only opaque tokens (MF-5).
- Edit `rosetta-ring.accepted_answers`: demote `ward covering`, promote dark-hours + unspoken (MF-17).
- Convert paraphrase rows to self-verifying artifacts or ungated reads; cap `seventh-shrine` (MF-15).
- Seed the 5 orphan ciphers onto placed nodes; seed `unspoken-refrain` / `haunted-herd` / the Dark-Hours
  row (MF-16, MF-18, MF-19).
- Break the 1:1:1:1 regularity on ≥2 keepers; withhold the `m2-rhyme` chorus (MF-14).

**Config:** `sites.yml` — author `unbroken_light`, six named keeper-stones, Stone of Reckoning, the
Rosettas, shore/threshold/cold-hearth anchors with real coords, enabled (MF-4). `config.yml` — author the
per-arc `forbidden-words` (MF-18).

**World corpus:** build the `.schem`/`schematics/` corpus, rumor→verify destinations FIRST, then
keeper-stones, then peaks (MF-2); author the travel/observation/gather-event/keeper-arc content (MF-1,
MF-12).

**Migration + self-tests (Supabase/design):** ship the 5 tables + 2 columns WITH the thread-layer coverage
invariant (MF-23); add the canon-registry, reachability/ledger, site-coverage, and authoring-contract
self-tests to the `clue-specs.ts` harness (§5 coherence machinery, MF-3).

**Docs:** WORLD-BIBLE §5 + INTEGRATION §3.2 + COHERENCE-AUDIT — global-replace to `the_`-prefixed custom
keys (MF-3). Add a "SHIPPED REALITY" header reconciling the player-driven Liar, the existing-but-thin
`sites.yml`, and black-moon↔full-moon terminology (MF-28). Add the HOURS column to the
fragment→revelation ledger (MF-1).

**Flag to the concurrent plugin session (not this session's edits):** GroupBeat fan-out + group-crouch
Accepting detector + register `trigger_accepting` (MF-8); two-path reveal + `SceneAwareness` (MF-9);
`fallback_entity`/no-ZOMBIE + isolation-gated apparition (MF-10); resource-pack load gate (MF-11);
Discord hold/quiet-hours interlock (MF-21); FAWE atomicity + paste ceiling + absence self-check (MF-22);
stale-watcher cull + map-id pooling (MF-24); reveal footprint sampling (MF-25). Build the dashboard
ALL-CLEAR interlock + the go-live pre-flight script (§5 production safety).

---

> **Build commits when** the seven BUILD-BLOCKERS are cleared — i.e. when (1) the longevity plan exists on
> paper as authored TRAVEL + observation + gather-events budgeted in HOURS, (2) the world corpus to walk to
> is being built rumor-destinations-first against an hours target, and (3) the five coherence guards are
> green: the canonical `the_`-prefixed custom-key namespace with its build assertion, `unbroken_light` +
> the named spine sited with the site-coverage test, the rite sentinels swapped for opaque tokens, the
> `iss-dead-shrine` edge wired with the reachability test, and the thread-layer coverage invariant written
> before any thread content. Everything else in §3 is fixable during the build; these seven, and the
> plugin-owned group-safety preconditions flagged above, must be true before the camera rolls.
