# THE OBSERVANCE — RESEARCH DOSSIER (distilled, prioritized, project-applied)

Research lead synthesis of all seven lane notes (`arg-craft`, `mc-arg-genre`,
`mc-displays-particles-packets`, `mc-pack-fog-sound`, `mc-structures`, `mc-build-visualize`,
`self-directed`), aimed at the real failures named in `design/TEARDOWN.md`. This is the brief
for the next build session.

**North star:** *"From The Fog, but it knows your name."* — autonomous, per-player,
soft-pressure, not scripted.

**The one-sentence frame that decides everything below (from TEARDOWN §1):** the central nerve
was never soldered (flag-gate is simultaneously wide-open and permanently-sealed, every guard
fails silent), AND the creative layer is over-woven for a 4–8 person irregular voice group. So
the research that matters most is the research that lets us **solder one loop and make it land
small** — not the research that adds capability. Every technique below is tagged with which
TEARDOWN defect it kills or which "land it small" goal it serves.

---

## A. TOP 20 ACTIONABLE TECHNIQUES (each: technique → real API/example → where in OUR stack → pitfall)

Ordered by leverage against the teardown, not by lane.

### 1. Re-tier the in-game layer: world-build static, reserve runtime for **per-player packet illusion**
- **Technique:** stop relying on global "mutate only when no one is witnessing" (`mutateWhenUnwitnessed`/`Reveal.isHidden`) — for a 4–8 group standing in one base the unwitnessed instant never comes, so `SignWriteBeat`/`RevealBeat`/`SmallStructureBeat`/`RoomSwapBeat`/`LecternFillBeat` silently never fire (TEARDOWN #6). Split into two layers: **(a)** every stone/shrine/lintel/hearth/grave is built statically into the world ahead of time; **(b)** the "it knows ME" reactivity is per-player illusion that needs no unwitnessed instant.
- **API/example:** `player.sendBlockChange(loc, data)` — fakes a block packet for ONE client, world untouched (`mc-displays-particles-packets §3.1`); `display.setVisibleByDefault(false)` + `targetPlayer.showEntity(plugin, display)` for a rune only one friend sees (confirmed canonical per Paper docs, `§1.7`); EntityLib `WrapperEntity.addViewer(target)` for a mob only the stalked friend sees (`§3.3`).
- **In our stack:** rebuild `FakeBlockBeat`/`Private*` beats around `sendBlockChange` + `showEntity`/`addViewer` keyed to one player; demote `RoomSwapBeat` to a sealed-door + teleport-on-reentry (see #6 below). This is "the single biggest interactions fix" per the teardown.
- **Pitfall:** `sendBlockChange` desyncs the instant the player interacts or the chunk reloads, and tile-entities (skulls/signs/heads) are NOT reliably faked (Paper #10010, #10515) — use it for *simple* blocks (torch→redstone torch) and revert on `PlayerMoveEvent` leaving range or on a short timer; for fake heads/signs use an ItemDisplay/TextDisplay instead.

### 2. Atomic jsonb flag merge — kill the silent flag-clobber
- **Technique:** `setArcFlags` is read-modify-write with no guard, so a concurrent Discord + in-world solve clobbers each other and a lost `iss_caught` silently seals the back half (TEARDOWN #11, will-it-run #11).
- **API/example:** Postgres `||` jsonb concat does a top-level key union taking the right operand on conflict (postgres docs) — exactly right for a flat flags object: `update arc_state set flags = flags || :new_flags::jsonb where ...` as ONE server-side statement (RPC / `.rpc()`), never SELECT-then-UPDATE in app code.
- **In our stack:** `discord/src/db/repo.ts setArcFlags` → replace with a Supabase RPC that does the `||` merge in-DB; the Java twin must do the same (or be made Discord-only for flag-setting puzzles).
- **Pitfall:** `||` is a *shallow* merge — fine for our flat `{prologue_ignited:true, iss_caught:true}` shape, but do NOT use it if flags ever nest (nested keys get wholesale-overwritten); keep the flags object flat.

### 3. Gate puzzles by reading `requires_flags` against `arc_state.flags` once per resolve
- **Technique:** `getOpenPuzzles` never selects `requires_flags` and filters only `active=true` (TEARDOWN #1) so every gated row is solvable from minute zero; the Java `OracleResolver.firstMatch` has the identical gap. Migration `0006` that creates the column doesn't even exist (#2), so seeds silently skip activation.
- **API/example:** plain SQL — add `requires_flags` to the SELECT; in `matchPuzzle`/`firstMatch` load `arc_state.flags` once and reject any row whose `requires_flags` keys aren't all truthy.
- **In our stack:** write `0006_requires_flags.sql` (`puzzles.requires_flags jsonb not null default '{}'`), re-run both seeds, wire the read into BOTH surfaces. This + #2 + the producers (#4) are ONE atomic change; nothing else matters until it lands.
- **Pitfall:** keep the normalized-string idempotency core (`recordSolve`/`insertSolveIfNew`) and the TS/Java normalizer agreement — those are sound (TEARDOWN §6 close); don't "refactor" them while you're in there.

### 4. Build flag *producers* (or a `/obs flag set` admin command) before any playtest
- **Technique:** none of `IgnitionListener`/`CoopPlateListener`/`SeventhChoiceListener`/`UnlitDeepListener`/`RefusalRiteListener` exist, so nothing ever writes `prologue_ignited`/`iss_caught`/`seventh_named`/`undercroft_open` — even after #2+#3 the gates never open (TEARDOWN #3). `prologue_ignited` is *read* at `autonomy.run.ts:125` and written nowhere.
- **API/example:** standard Bukkit listeners; for the ignition, the cleanest producer is the Discord side — in `messageCreate`, when `channelId===theRecord` and the poster is a keeper, set `prologue_ignited=true` idempotently (via the #2 atomic merge) and let the next autonomy tick ack.
- **In our stack:** start with Ignition + CoopPlate; ship a temporary `/obs flag set <key>` admin command first so you can prove the gated rows open before the listeners are perfect.
- **Pitfall:** the Java `OracleResolver.applyOutcome` never applies `set_flags` (only enqueues the beat) — an in-world solve of a flag-setting puzzle advances nothing (will-it-run #12); either handle `set_flags` in Java or make flag-setting puzzles Discord-only and document it.

### 5. Model the arc as a **salience-based storylet engine**, not a linear pointer
- **Technique:** the teardown's "wide-open vs permanently-sealed" pathology and the "Watcher never points at one thread, the M2 field opens everything at once" problem (TEARDOWN #9) are both symptoms of script-thinking. A storylet engine (Emily Short / Failbetter; salience-based = the *engine* auto-picks the most-applicable legal beat, à la Façade's drama manager; Kreminski's storylet weights) gives order-independence, skip-tolerance, and gap-resilience for free (`self-directed §7`).
- **API/example:** each beat = `{preconditions over qualities, content, state-change}`; world state = per-player qualities in Supabase; the showrunner evaluates *legal* storylets and fires the most *salient* one. Murder-mystery proof: 3 clues in any order = 4 storylets, not 6+ branches.
- **In our stack:** the `requires_flags` column IS the storylet precondition system — lean into it. The showrunner's drip becomes "fire the single most-salient legal storylet for this player," which directly fixes #9 (name ONE live thread at a time instead of opening a wall). Use **Carousel** (rotating ambient dread) under a **Midnight Staircase** spine.
- **Pitfall:** don't let salience-weighting become opaque DDA the players resent — keep the *availability* of rescue disclosed once up front (see Principle violations §B), content stays diegetic.

### 6. Sealed-door + teleport room-swap instead of unwitnessed live mutation
- **Technique:** `undercroft-fog`/`RoomSwapBeat` fire only when no player looks; "blocks changed when I wasn't looking" reads as a chunk/lighting bug or grief, and the instant may never come (TEARDOWN §5). Replace with a deterministic trigger.
- **API/example:** pre-stage Room B in an adjacent unloaded location; fire on `sealed door closes + short delay`; teleport-on-reentry; pin a Watcher line ("the room is not the room you left — read it again") so the change is *claimed as intentional* the instant they notice.
- **In our stack:** `RoomSwapBeat` becomes a teleport beat, not a FAWE in-place repaste; Multiverse already in the stack handles the staging world.
- **Pitfall:** FAWE relight — a repasted Room B with stale lighting is the #1 "this was placed" tell (`mc-structures §4`); after any paste set `lighting.mode ≥ 1` and resend affected chunks to nearby players next tick.

### 7. Vanilla-mob-as-Watcher with reveal discipline (model is NOT the scare)
- **Technique:** "a vanilla mob you only ever DISCOVER already reads supernatural" (`mc-build-visualize §7`). The reveal discipline does the work, not polygon count.
- **API/example:** `mob.setAI(false)+setSilent(true)+setInvulnerable(true)+setRemoveWhenFarAway(false)` + `INVISIBILITY` potion (no particles) + `setGlowing(true)` + PDC-tag for showrunner cleanup; per-player glow COLOR needs a scoreboard team (mobs can't `setGlowColorOverride`, only Displays can) (`mc-displays §4`).
- **In our stack:** keep `NamedMobBeat` as the everywhere-watcher; **CUT `ModeledMobBeat`** (no ModelEngine purchase, it's a 240-line clone implying a capability we don't have — TEARDOWN §4) and the worn-skin offline apparition (a mislabeled Warden named "Brann" is worse than no apparition).
- **Pitfall:** monster tracking range is 48 blocks; a Watcher staged beyond that won't render — stage in-range or use a packet entity for distance. `setAI(false)` stops gravity → it floats unless you teleport it.

### 8. Mono OGG spatial sound is the whole soft-pressure audio layer
- **Technique:** a **mono** OGG Vorbis attenuates with distance and pans 3D (has a *position*); a **stereo** OGG plays at constant volume with no position. "A breath from somewhere behind you" REQUIRES mono (`mc-pack-fog-sound §4`).
- **API/example:** `ffmpeg -i in.wav -ac 1 -c:a libvorbis out.ogg`; `player.playSound(loc, "observance:...", AMBIENT, vol, pitch, perPlayerSeed)`; the trailing long seed picks the weighted variant → pass a per-player seed for per-player variation.
- **In our stack:** fixes `SpatialVoiceBeat`, which is "not spatial" — its `behind`/`offset` fields are read but never used and `PerPlayer.namedSound` is non-positional (TEARDOWN §5). Use `soundAt(loc)` at a real behind-the-player location.
- **Pitfall:** must be OGG **Vorbis** not Opus (MC can't decode Opus); only 4 `stream:true` sounds at once (reserve streaming for long ambient beds, keep stings non-streamed); use `AMBIENT`/`MASTER` category so friends can't mute the Watcher via the music slider.

### 9. `minecraft:illageralt` rune font on day one; bespoke PUA font later
- **Technique:** MC already ships the Standard Galactic Alphabet (`minecraft:illageralt`) — a real 1:1 substitution cipher A–Z, renderable with ZERO pack work, and decodable by curious players (perfect ARG bait) (`mc-pack-fog-sound §3`).
- **API/example:** `Component.text("THE WATCHER KNOWS").font(Key.key("minecraft:illageralt"))` on signs/titles/lore/bossbars; a bespoke `observance:runes` bitmap font in PUA codepoints `U+E000+` later for the signature look.
- **In our stack:** this is the **graceful-degradation backbone** for the resource-pack font being load-bearing and un-degraded (TEARDOWN will-it-run #16) — `illageralt` needs no pack, so a friend who declines the pack still gets readable runes.
- **Pitfall:** the cipher is display-only — a player typing on a sign won't auto-encode; your plugin must write the rune component. Author the pushed-pack runes with a `supported_formats` range and re-hash every build (`§2`, `§8`).

### 10. Datapack fog for the Undercroft — NOT the resource pack
- **Technique:** in Java, fog is driven by **dimension `effects` + biome `effects` in a datapack** (server-side), never the resource pack (`mc-pack-fog-sound §6`, hard architecture fact).
- **API/example:** dimension `"effects":"minecraft:the_nether"` = thick sight-blocking near fog; `has_skylight:false`, `has_ceiling:true`, `ambient_light:0.0` = sealed pitch-dark; biome `fog_color` cold near-black + `mood_sound` pointed at a Watcher whisper = the cave-noise jumpscare self-generates with zero plugin code.
- **In our stack:** the Undercroft ships as a datapack loaded via Multiverse (already in stack), separate `pack.mcmeta` from the pushed pack.
- **Pitfall:** **MC-211878** — `effects:the_nether` with 12+ biomes loses the fog; keep the Undercroft to 1–3 biomes. The Replay-Mod render camera clamps Y 0–255 but the Deep Hold is deep-negative-Y (`mc-build-visualize §6`) — resolve before filming.

### 11. Display entities = the no-pack rune/apparition engine (graceful degradation)
- **Technique:** `TextDisplay`/`BlockDisplay`/`ItemDisplay` are visual-only entities (no hitbox/AI/physics), spawnable server-side with no pack (`mc-displays §1`, `mc-build-visualize §3.1`).
- **API/example:** `TextDisplay` with `setBillboard(CENTER)` (always faces viewer), `setBrightness(15,15)` (full-bright in the dark), `setBackgroundColor(ARGB 0,0,0,0)` (clean glyph), `setGlowing+setGlowColorOverride` (per-entity colored outline, bypasses the team requirement); `ItemDisplay` PLAYER_HEAD scaled 5–10× = a looming face with no pack.
- **In our stack:** the pack-free fallback render path for every rune/carving (Rosetta ring, Accepting-floor inscription); per-player reveals via `showEntity`.
- **Pitfall:** interpolation **snaps** instead of tweening if you set the final transform in the same tick you spawn — spawn with start state, set delay/duration next tick, apply target the tick after. Displays leak (never GC'd in a never-unloading chunk) → `setPersistent(false)` + PDC-tag + `remove()`, reuse `NamedMobBeat`'s cleanup.

### 12. De-collide shared answer strings before anything ships
- **Technique:** `matchPuzzle` returns the FIRST open row by DB order, so one solve fires the wrong puzzle_key/voice/flags. `the one who turned away` is a primary answer on THREE active rows; `a thing that can say no is not a wall` on two; bare `iss`, `7`/`seven`, `threshold` collide (TEARDOWN #5, #8).
- **API/example:** no API — an authoring invariant: never let two *simultaneously-open* rows share a normalized string; give each shared plaintext exactly one owner and gate the rest behind `requires_flags`.
- **In our stack:** collapse `base-docket-reread` ↔ `base-docket-reread-auto` to one row gated on `iss_caught`; make the carved Rosetta ring glyphs and `rosetta-ring`'s accepted answer the SAME six words (currently teaches `...line WARD COVERING`, demands `...line unspoken sacred beast` — pure moon-logic on the literacy on-ramp).
- **Pitfall:** this interacts with the lure-`6` collision — pick ONE referent for `6` (cleanest: six prior keeper *generations*) so the group never conflates "you are the seventh" (count) with "the Seventh, cast out" (keeper).

### 13. Build the hint/whisper rail as P0 (the group's only safety rail is dead)
- **Technique:** there is no `whisper_budgets` table, no spend module, no per-puzzle `hintBody` — `/whisper <puzzle>` can't return a tiered hint (TEARDOWN #7). A real group hits ONE unsignposted cipher and stalls a whole session.
- **API/example:** tiered hints unlocking on *time-since-last-progress*, delivered diegetically (Layton/Drawn pattern, `self-directed §3.1`): Tier 1 ambient nudge → Tier 2 NPC says it plainer → Tier 3 the companion "journal" basically spells it. Never `Hint: try X` — the Watcher *knows things*, it doesn't *give hints* (`arg-craft §5`).
- **In our stack:** build the budget ledger + 2–3 `hintBody` tiers for each of the ~6 spine ciphers before first playtest.
- **Pitfall:** disclose *availability* of rescue once, out-of-fiction ("the world will never hard-lock you"), so players trust it to escalate instead of rage-quitting — but keep every hint's *content* fully in-character (`self-directed §3.2`).

### 14. Coop gate: widen to minutes, latch each leg independently
- **Technique:** `m4-three-hands` demands 3 real-time acts in a ~20s window — the worst mechanic for a remote async group, and the AND-join is architecturally homeless (it can't live in `applyOutcome`, which runs only AFTER one answer matched) (TEARDOWN #10).
- **API/example:** put leg-counting in `CoopPlateListener`; latch each leg (foot-on-plate / carve / Discord-post) independently with no tight window; the gate fires when all three legs are *held*, and the listener posts the sentinel token once.
- **In our stack:** `CoopPlateListener` is also one of the missing producers (#4) — build it once, it serves both. The theme ("you cannot do this alone") survives a generous window.
- **Pitfall:** the opaque-sentinel terminal beats (`accepting-crouch`, `record-receives`, `seventh-choice`, `m4-three-hands`, `threshold-coordinate`) are ALL inert until their listener exists — Movement-V is unreachable until producers ship.

### 15. FAWE reveal session: async, no-history, no-physics, then relight
- **Technique:** vanilla `setType` off-thread throws (`AsyncCatcher`); FAWE EditSession is the deliberate async exception (`mc-structures §0`). For 50 schem pastes a session you never want undo history or physics.
- **API/example:** `newEditSessionBuilder().world(w).fastMode(true).changeSetNull().maxBlocks(-1).build()` inside `runTaskAsynchronously`; `ClipboardHolder(clip).createPaste(s).to(pos).ignoreAirBlocks(true).build()`; `Operations.complete(op); s.flushSession()`.
- **In our stack:** `SmallStructureBeat`/`FaweSchematicPaster` — but per #1, most structures should be world-built *statically* now, so this is mainly for the few genuine runtime reveals behind sealed doors.
- **Pitfall:** preload every `.schem` into a `Map<String,Clipboard>` at enable (no FS hit at the dramatic moment); standardize NW-bottom-corner origin so pastes don't land 1 block off; idempotency ledger keyed `(beatId, blockPos)` so a re-fired beat doesn't double-paste.

### 16. RevealGate only for the few remaining live mutations; build *behind* them on purpose
- **Technique:** when a runtime reveal genuinely must avoid pop-in, use FOV cone (dot product) → raytrace occlusion, and prefer to *place outside the look cone* rather than wait for line-of-sight to clear (`mc-structures §7`).
- **API/example:** `look.dot(toTarget) >= cos(70°)` for "in view"; `world.rayTraceBlocks(eye, dir, dist, FluidCollisionMode.NEVER, true)` for occlusion; pick placement at `dot ≈ -0.3` for "behind/periphery."
- **In our stack:** keep `RevealGate.canMutate` but stop making it the backbone — it's a tool for ≤a handful of beats, not the M1 spine (that's the #1 re-tier).
- **Pitfall:** `isChunkLoaded` precondition first — `getBlockAt` on an unloaded chunk forces a sync load and can world-gen near a player.

### 17. Diegetic surfaces only — no sentence may read like a game system (TINAG)
- **Technique:** TINAG is the *aesthetic*, not a lie the friends don't know (`arg-craft §1`). The test: "would this sentence survive being read aloud by someone who believes the Deep Hold is real?"
- **API/example:** no `[Server]` prefixes on Watcher messages, no "you triggered event #14," no command output that reads like a feature; the Vercel dashboard presents as an in-world *record/console*, never "The Observance — A Minecraft ARG" (`self-directed §4.3`).
- **In our stack:** fixes the website `readSignal()` reading `v_record` (a view that hasn't shipped → renders all-redacted regardless of progress, TEARDOWN will-it-run #15) — ship the view AND keep the page diegetic.
- **Pitfall:** surface-voice consistency — the Discord bot and the in-game haunt must be the SAME entity; if the bot sounds like a bot the cross-surface illusion breaks.

### 18. Per-player particle dread loop (the cheap "it knows ME" primitive)
- **Technique:** `Player.spawnParticle` / `ParticleBuilder.receivers(List.of(player))` is per-client — the haze can stalk one friend while the others see a clear room (`mc-displays §2.4`).
- **API/example:** `Particle.SCULK_SOUL.builder().offset(.3,.5,.3).count(2).receivers(List.of(target)).spawn()` on a 4-tick timer following the target; `DUST_COLOR_TRANSITION` red→black on a rune; `TRAIL` (1.21.4+) motes that crawl toward one client.
- **In our stack:** drives `Private*` beats; pure soft-pressure, no shared state, no pack.
- **Pitfall:** keep counts low (1–4) — high count × tight timer is FPS death and reads as spam, which kills dread; wrong `data` type for a particle throws at runtime.

### 19. custom_model_data relics that visibly change state with the ARG (lockstep)
- **Technique:** 1.21.4 overhauled item models — old `overrides`+`CustomModelData:int` is gone; new `item_model` component points at `assets/ns/items/<id>.json` with a `range_dispatch` over `custom_model_data` (`mc-pack-fog-sound §5`).
- **API/example:** `range_dispatch` thresholds so one relic item renders dormant→warm→cold as your plugin sets the component — mechanic and story move together (the consistency principle in MEMORY).
- **In our stack:** Watcher relics, "letters from the deep," map fragments; the LSB stego decision (TEARDOWN #11) — either run `embedLsb(frame,'ISS')` on the Iss card's RGBA *after* resvg, OR accept the rune layer as a visual-only watermark and delete the false "decode" framing. Don't ship self-test theater.
- **Pitfall:** packs/plugins written for ≤1.21.1 silently render the base item on 1.21.4 — pin ONE Paper 1.21.x and author for exactly it.

### 20. Stego that survives Discord = visible-but-overlooked, or host the PNG yourself
- **Technique:** LSB PNG survives Discord ONLY via `cdn.discordapp.com` (byte-preserved original); `media.discordapp.net` previews resize and destroy LSB; Discord strips trailing-after-IEND and metadata (`self-directed §5`).
- **API/example:** prefer *visible-but-overlooked* clues (text in image corners, a faint string, audio static) — survive any pipeline; if you must LSB, host the PNG on Supabase storage / the Vercel site and link it, bypassing Discord entirely.
- **In our stack:** the Iss card / any stego clue — host it yourself; never put payload in metadata/trailing bytes.
- **Pitfall:** for 5–15 friends a clever *visible* cipher beats fragile bit-level stego that one screenshot kills; pair anything fragile with redundancy/ECC.

---

## B. ARG-CRAFT PRINCIPLES WE ARE VIOLATING OR UNDER-USING (case study → our violation → fix)

1. **"Story is the skeleton; puzzles are texture" (The Beast / Cloudmaker problem, `arg-craft §3`).**
   *Violation:* the whole arc gates on flags that nothing produces, so progression IS the puzzle-gate — and it's broken in both directions (TEARDOWN #1–4). *Fix:* decouple — the showrunner advances the spine on a salience cadence (#A5) whether or not last week's cipher fell; puzzles unlock *bonus* lore/recognition, never the next chapter.

2. **"Scale puzzles down ~10×, scale rescue up ~10×" (friend-group scale, `arg-craft §0`).**
   *Violation:* the rescue rail is *dead* (no whisper budgets/hints, #A13) while the puzzle surface is a *multiple* of what 4–8 irregular players touch (24 threads, 21 facts, two extra dimensions, three "sevens"). *Fix:* build the hint rail as P0; execute the TEARDOWN CUT LIST (Nether, End, UNKEPT acrostic, FACT 17/Ear, difficulty-reveal, prophet-wall-name, the type-the-theme-back non-puzzles) so the few real ciphers are findable.

3. **"Difficulty is in NOTICING the puzzle exists, not cracking it" (Foster/Portal, `arg-craft §4`).**
   *Violation:* `rosetta-ring` teaches one answer and silently demands another (moon-logic on the literacy on-ramp); the Iss catch relies on the group spontaneously reusing a name as a Vigenère key with nothing hinting keys are reusable. *Fix:* self-confirming answers; make the cold-hearth dead-end itself plant the doubt ("the road was read true and still went nowhere — whose road was it?") so the grave PUSHES them to re-test Iss's key (TEARDOWN §5 "better" version).

4. **"Redundant trailheads (2–3 per beat) + a standing rabbit hole at spawn" (`arg-craft §2`, `self-directed §3.4`).**
   *Violation:* spine-critical clues live on single surfaces; `record-url` decodes to `the record keeps` (spaced) but the route serves `the-record-keeps` (hyphenated) and nothing hands the group the host — a correct solve is a dead-end disguised as a win. *Fix:* every spine clue on ≥2 independent surfaces (in-world sign *and* Discord whisper); the Watcher Discord-posts the record link at ignition + finale so the loop closes for everyone, not the one person who memorized a URL.

5. **"TINAG is theatrical, not literal; never break frame" (`arg-craft §1`, `mc-arg-genre §3`).** *Under-used but mostly intact.* *Risk:* the website renders as a "real game website" and the dashboard isn't gated. *Fix:* present every surface as a diegetic record/console; gate the director's console behind auth (#A17).

6. **"Per-player texture on a SHARED spine; distribute fragments so it can't be soloed" (I Love Bees / Cogmind, `arg-craft §7`, `self-directed §2`).**
   *Violation:* the "it knows your name" north star risks fragmenting into 10 private mini-ARGs, AND one sharp friend can solo the open field. *Fix:* personalize the *approach* (name, base, dread) but make milestone reveals *shared*; give different players different fragments so the reveal requires pooling.

7. **"Speak in implication; profile in-world behavior only; never let a line read as a stat" (DDLC/Petscop/Nemesis, `self-directed §1`).** *Under-used.* The most uncanny line is vague-true-of-anyone ("You keep one thing you never use. You know the one.") not a stat readout ("You died 14 times at -211,64,883"). *Fix:* author Watcher lines that load on the *player's projection*, cap name/direct-address to ≤once per session, reference blocks/deaths/paths never out-of-world identity.

8. **"Restraint forever on the mystery; author exactly ONE emotional resolution" (Petscop/Local 58 vs Cicada, `arg-craft §9`).** *At risk via over-explaining* (the difficulty-reveal, the filing-axis thesis, the UNKEPT told-then-hunted). *Fix:* keep mechanics ambiguous; the one authored gut-punch is **the Liar and the Catch** (Iss lied; the land kept the proof) → Seventh coda → the Accepting bow. That's the spine that stays (TEARDOWN §7).

9. **"Disclose the EXISTENCE of personalized surprise up front; care for bleed; debrief" (Torner / care ethics, `self-directed §6`).** *Missing.* *Fix:* one out-of-fiction Session-Zero message ("something will be on the server, it will watch and seem to know you, you can opt out any time"); a working mid-experience opt-out that dials personalization down no-questions; a debrief at the end — mandatory for a veteran audience where isolation/being-watched can hit harder than intended.

10. **"Seed anomalies, re-present them at payoff, gate payoffs on seed-seen" (`self-directed §8`).** *Under-used* — payoffs can land on players who missed the setup. *Fix:* gate each payoff storylet on `saw_seed_X=true` and *re-quote the seed verbatim inside the payoff* ("You said the tower was empty. It was never empty.") so the reel-back lands; drop payoffs where the group compares notes.

---

## C. VANILLA-FIRST + DISPLAY-ENTITY CAPABILITY MAP (degrade gracefully)

The pushed pack is load-bearing and un-degraded today (a friend who declines sees tofu/Latin — TEARDOWN will-it-run #16). This map is the contract: **anything in the ZERO-PACK column must still convey the beat if the pack fails to load.** Build the pack as enhancement, not dependency.

| Capability | ZERO resource pack (always works) | Truly needs the pushed pack |
|---|---|---|
| **Rune text** (signs/books/titles/lore/bossbar) | `minecraft:illageralt` (real SGA cipher, decodable) | bespoke `observance:runes` PUA glyph *look* only |
| **Floating rune / carving / hologram** | `TextDisplay` (CENTER billboard, brightness 15/15, transparent bg, glow override) — per-player via `setVisibleByDefault(false)`+`showEntity` | nothing — Displays render the pack font if present, the SGA font if not |
| **Looming face / relic / object** | `ItemDisplay` PLAYER_HEAD scaled; vanilla item as relic | bespoke `custom_model_data` relic *model* (cosmetic only) |
| **Block that only ONE player sees** | `player.sendBlockChange` (simple blocks) | — |
| **Mob that only ONE player sees** | EntityLib `WrapperEntity.addViewer` (invisible+glowing meta) | — |
| **Real mob as Watcher** | `setAI(false)+setSilent+setInvulnerable+INVISIBILITY+setGlowing`+team color | bespoke ModelEngine rig — CUT (unbought) except 1 recorded hero shot |
| **Ambient/per-player dread haze** | `Player.spawnParticle` SCULK_SOUL / SOUL_FIRE_FLAME / DUST_COLOR_TRANSITION / TRAIL, `.receivers(List.of(p))` | — |
| **Undercroft fog (density/color)** | **datapack** dimension `effects:the_nether` + biome `fog_color` (NOT the pack) | — (never the pack) |
| **Mood/ambient horror loop** | **datapack** biome `mood_sound`/`ambient_sound` (self-generating cave-noise dread) | the custom OGG it points at (but a vanilla sound works as fallback) |
| **Spatial Watcher cues** (breath/footstep/name) | vanilla positional sounds via `playSound(loc,…)` | bespoke **mono** OGG (richer, but degrade to a vanilla ambient_cave) |
| **Moving / receding / guttering light** | PacketEvents phantom `minecraft:light[level=N]` block packets; `setGlowing` outline | — |
| **Structures / ruins / reveals** | FAWE schem paste (async, relit) + static world-build | — |
| **Murals / found media** | ImageOnMap/ImageFrame map-art (server-side, automatable) | — (maps are vanilla) |
| **Subtitle-channel intrusion** ("the game glitched") | vanilla subtitle/action-bar text | bespoke subtitle keys (cosmetic) |

**Degradation rule to encode:** build a `ResourcePackPusher` with `required(true)` + a decline→reason path, but make EVERY clue legible via the ZERO-PACK column so a decline degrades the *aesthetic* (SGA glyphs instead of bespoke runes), never the *solvability*. Keep the Discord `#the-record` mirror as the non-font fallback. The pack carries *look*, the datapack carries *fog/mood*, the plugin carries *reactivity* — none of the three may be the single point of failure for a clue.

---

## D. 8 NON-OBVIOUS IDEAS that would materially improve this for a FRIEND GROUP

1. **Per-player intimacy, group-witnessed reveal — engineered, not hoped.** Use `sendBlockChange`/`showEntity`/per-player particles so the Watcher haunts ONE friend (a torch only they saw flip, a rune only they see) — then make the *milestone* reveal shared and synchronous so the chat erupts "wait, did that happen to YOU too?" (`mc-arg-genre §5`, `arg-craft §7`). The personal dread is the propagation fuel; the veteran group chat IS the distribution layer. This is the literal realization of the north star and it costs no pack.

2. **The vague-true-of-anyone line beats the database.** The plugin can know almost nothing and feel omniscient: "You keep one thing you never use. You know the one." is unfalsifiable, projects onto a real memory, is cheaper than profiling, AND is ethically clean (in-world behavior, no out-of-world data) (`self-directed §1.3, §6.3`). Audit every Watcher line: if it reads as a stat, rewrite it as an implication.

3. **The Nemesis "scar + callback" with a thin memory.** One persistent in-world mark that survives sessions (a block that stays changed, a sign that re-appears) + one callback ("I remember the tower.") makes the Watcher feel like it has remembered you for weeks, on ~10 lines of authored content. Veterans' brains build the rest (`self-directed §1.4`). Don't write the relationship — write the anchor.

4. **Absorb-the-coincidence escape hatch via the dashboard.** A 4–8 person group WILL apophenia-spiral on an accidental misplaced block (a 3-hour rabbit hole that sours a session). Watch Discord theories on the dashboard and *retroactively canonize* the coincidence (the Beast's "step-self" move) — turn the bug into lore instead of fighting it (`arg-craft §8`). This is a *director* superpower the 2001 puppetmasters would have killed for.

5. **The mood_sound dimension generates dread for free.** A biome `mood_sound` pointed at a Watcher whisper makes the *vanilla engine* play it from a random nearby dark block when the internal mood counter tops out — the classic cave-noise jumpscare, self-generating, no plugin tick, no per-player code (`mc-pack-fog-sound §6`). For an autonomous haunting that must work between logins, this is the cheapest ambient layer in the project.

6. **Carousel of low ambient beats so returning feels alive, never paused.** Model ambient dread as a Carousel storylet pool that recurs on a slow real-time clock (fog creeps, a torch gutters, presence grows) UNDER the state-gated spine, so a friend returning after a 2-week gap feels the world *moved* — without the spine climax ever happening to an empty server (`self-directed §7.3`, `arg-craft §6`). Pair with a Discord "previously on" recap DM on return.

7. **Asymmetric knowledge as a *role*, not a spoiler.** Veterans love specialization. Make "the one who reads the glyphs" / "the one the Watcher talks to" an owned role by distributing fragments (different whispers to different people, cipher to one, key to another) — this simultaneously defeats the "one sharp friend solos it" failure AND gives the quiet members a contribution (`arg-craft §7`, `self-directed §2`). The collaboration is forced by design, I-Love-Bees style, at LAN scale.

8. **Capture-everything server-side so a Wifies-style cut is possible later, with zero player cost.** The haunting is *real and per-player* — exactly the footage that can't be faked, which is the asset a found-footage edit amplifies (`mc-arg-genre §7`, `mc-build-visualize §6`). Log beats server-side now (you control the server); the recording rig (Replay Mod + Iris/Complementary Reimagined, resolve the deep-negative-Y clamp first) lives ONLY on Ethan's client and never touches the pushed pack — Path A stays intact while a YouTube layer stays possible.

---

## E. WHAT TO READ FIRST (index into the lane notes)

Read in this order; each entry says *why* and the exact section.

1. **`design/TEARDOWN.md` — all of it, again.** The ground truth of what's broken. §2 (12 worst offenders), §6 (will-it-run defects in order), §7 (the simpler stronger version + the 6-step vertical slice). Everything else serves this.

2. **`research/self-directed.md §7 (storylets) + §1 (uncanny line) + §6 (consent).** The architecture (storylet engine = the `requires_flags` system done right) and the two design rails that make "it knows your name" land without being invasive or unethical. §7 is the single highest-leverage architectural idea in the corpus.

3. **`research/arg-craft.md §0 + §3 + §4 + §5 + §9.** Friend-group scale changes every rule (§0); decouple story from puzzles (§3); hard-but-fair / self-confirming answers (§4); the rescue economy you must build (§5); restraint + one emotional resolution (§9). This is the "land it small" doctrine.

4. **`research/mc-displays-particles-packets.md §1, §3, §4.** The per-player illusion toolkit that REPLACES the broken global unwitnessed-mutation layer (TEARDOWN #6) — `showEntity`, `sendBlockChange`, EntityLib viewers, real-mob-as-Watcher. Read alongside `mc-structures.md §7` (RevealGate, now demoted to a few beats) and §0/§3 (FAWE async paste rules).

5. **`research/mc-pack-fog-sound.md §3 (illageralt), §4 (mono OGG), §6 (datapack fog), §8 (pack_format).** The graceful-degradation backbone (§3), the spatial-audio fix for `SpatialVoiceBeat` (§4), the Undercroft-is-a-datapack-not-the-pack fact (§6), and the version discipline that silently breaks fonts/models if ignored (§5/§8).

6. **`research/mc-arg-genre.md PART 1 (From The Fog taxonomy) + PART 2 (ranked dread) + PART 3 (slop filter).** The beat menu to steal and the avoid-list to encode as a validator. Weight the autonomous director toward dread-ranks 1–7; reserve 8–10 for showrunner arc punctuation.

7. **`research/mc-build-visualize.md §1–3 (carved-not-default stone, wrongness) + §7 (vanilla mob > rig) + §6 (recording rig, Ethan-only).** The craft of making the Deep Hold *feel* built-by-hands-then-abandoned, why the vanilla `NamedMobBeat` beats a ModelEngine rig for 95% of beats, and the recording layer that must never touch Path A.

**The through-line of all seven lanes, one sentence:** *solder the one loop (atomic fix #A2–4 + the storylet read #A3/A5), make every clue degrade to the zero-pack column (§C), build the rescue rail (#A13), profile in-world behavior and speak in implication (§B7/§D2), and prove the Liar-and-the-Catch vertical slice on a real server with 3–4 friends before building anything past M0.*
