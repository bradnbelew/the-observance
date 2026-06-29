# MC-Integration Audit — The Observance (fresh full pass)

Lens: Minecraft integration (mods/textures/creatures/events/sounds/scripts/NPCs/structures).
Scope: plugin Java beats+listeners, sites.yml, plugin.yml, config.yml, resourcepack/, atmosphere-stack/structures/bestiary.
Read-only. Severity-ranked. Each finding gives `file:symbol` + the exact fix the Finalize integrator applies.

---

## VERDICT

The new beats themselves are excellent: ModeledMobBeat, SpatialVoiceBeat, KeeperNpcBeat, RoomSwapBeat, RevealBeat
are all reveal-disciplined, idempotent, Java-21-safe, deterministic-fallback-behind-every-optional-dep, no
`com.ticxo.*`/`com.sk89q.*` class-load. The Java-21 line is held everywhere (build.gradle toolchain 21,
`options.release=21`, FAWE 2.15 / WorldEdit 7.3 / ModelEngine reflected; BetterModel 3.x correctly excluded).

But the **integration wiring is broken**: five of the new MC surfaces are authored, compiled-plausible, and then
never connected to the running plugin. They are dead until the integrator registers them. There is also a
class of **false "self-test enforces it" guarantees** that do not execute, and one **missing sound event** that
silently nulls the keeper-voice fallback. These are the must-fix items below.

---

## CRITICAL (load-bearing surface is dead / guarantee is false)

### C1 — 5 new beats are NOT registered in the catalog → engine rejects them as unknown
`beats/BeatLibrary.java:registerDefaults()` registers exactly 24 beats (TEXT…GroupBeat). It does **not**
register `RevealBeat`, `RoomSwapBeat`, `KeeperNpcBeat`, `ModeledMobBeat`, `SpatialVoiceBeat`. The showrunner
enqueues `beat_queue.type='reveal'|'room_swap'|'keeper_npc'|'modeled_mob'|'spatial_voice'`, the poller looks
the name up in `byName`, finds nothing, and the beat is dropped (an unknown-type skip). Every Deep-Hold
producer (the M5 reveal slot-lighting, the Undercroft A→B room rebuild, the presiding-Keeper speech, the
optional rig, the Ear's reply) silently never fires. This is the single biggest mc-integration breakage —
all five files are inert.
**Fix:** in `registerDefaults()`, after the existing block, add:
```java
// PRODUCER TRIAD (Deep-Hold) + DIRECTED garnish
register(new RevealBeat());
register(new RoomSwapBeat());
register(new KeeperNpcBeat());
register(new ModeledMobBeat());     // degrades to NamedMobBeat
register(new SpatialVoiceBeat());   // degrades to a named pack sound
```
(`UnlockBeat`'s `step:"reveal"` dispatcher and the room-swap path both depend on these names existing.)

### C2 — KeeperNpcListener is fully built but NOT registered → the entire Keeper interaction surface is dead
`ObservancePlugin.java:registerListeners()` registers Presence/BlockBreak/Death/Chat/Territory/CustomCompliance/
DarkHours/AnswerSign/AcceptingRite — and stops. `KeeperNpcListener` (the `PlayerInteractEntityEvent` → keeper-open
signal that the whole D8 dialogue tree hangs off) is never instantiated. Result: right-clicking the Keeper writes
no `event_log keeper/npc.open` row, the showrunner is never told to resolve the dialogue node, and `KeeperNpcBeat`
(itself also unregistered, C1) never gets enqueued. The `keeper_altar` + `the_threshold` sites, the M-IV atonement
node, and the seventh-choice offer are all orphaned for want of one `registerEvents` line.
**Fix:** in `registerListeners()` add (constructor already matches this exact shape):
```java
pm.registerEvents(new com.observance.watcher.signal.listener.KeeperNpcListener(
        this::sites, supabase, rateLimiter, scheduler, safety, "observance"), this);
```
(Pass the real namespace constant the plugin uses elsewhere rather than the literal if one exists.)

### C3 — `observance:keeper_voice` sound event is missing from sounds.json → SpatialVoiceBeat's "never silent" floor plays nothing
`beats/lib/SpatialVoiceBeat.java:FALLBACK_NAMED_SOUND = "observance:keeper_voice"` is documented as the
"permanent fallback clip — always present in the one-click pack" that "guarantees there is something to play."
But `resourcepack/assets/observance/sounds.json` declares only `whisper`, `drone_low`, `stone_breath`,
`cold_toll`. There is **no `keeper_voice` event**. `PerPlayer.namedSound(pl, "observance:keeper_voice", …)`
on a client with no such event is a no-op — silent. atmosphere-stack.md §3 (line 232/256) names
`observance:keeper_voice` as an authored sound, so the intent is real; the registry entry was just never added.
The guarantee in the Javadoc ("never silent") is therefore false.
**Fix:** add to `sounds.json` (and list the `.ogg` in resourcepack/README.md §GO-LIVE step 1 + atmosphere
sound roster), e.g.:
```json
"keeper_voice": { "sounds": [ { "name": "observance:keeper_voice", "attenuation_distance": 16 } ] }
```
Keep it MONO `.ogg` at go-live (same note as the other four). Until the audio is dropped in, the fallback
is still a registered-but-empty event (a benign no-op), not an unregistered key — that is the acceptable
go-live residue; the *unregistered* key is the bug.

---

## HIGH (anti-jank guarantee documented but not enforced)

### H1 — `riteTokenSelfTest` is promised as a build-time guard but does not exist and is never run
`AcceptingRiteListener.java:30` and `config.yml:159` both assert: "the token … is enforced byte-for-byte by a
build-time self-test (riteTokenSelfTest)… so the climax can never silently fail." There is **no
`riteTokenSelfTest` symbol anywhere** in `plugin/src`, and no test source set, no JUnit, no `main()` harness.
Nothing checks that `rites.accepting.token` (config.yml:162, the literal `k7q2m9 …`) byte-matches the seed's
`accepting-crouch` `accepted_answers`. If the seed token and the config token drift, the terminal group-bow
climax fires the oracle with a token the puzzle row will reject — the finale silently no-ops, exactly the
failure the comment claims is impossible.
**Fix (integrator):** either (a) add a real self-test that loads the seed's `accepting-crouch` accepted answer
and asserts equality with `config.getString("rites.accepting.token")`, invoked from `ObservancePlugin.onEnable()`
(log-and-disable-rite on mismatch), OR (b) downgrade the three comments to "MUST be kept in sync by hand
(no automated guard yet)". Do not ship the false guarantee. Option (a) is strongly preferred — this is the climax.

### H2 — the per-beat `*SelfTest()` / `siteCoverageSelfTest` idiom is never invoked anywhere
Every new beat ships a `static boolean xSelfTest()` (modeledMobSelfTest, spatialVoiceSelfTest, keeperSelfTest,
roomSwapSelfTest, revealParseSelfTest, escSelfTest) "mirroring the repo's selftest idiom." A repo-wide grep
finds **zero call sites** — no harness runs them. `sites.yml:271` likewise references a "site-coverage self-test"
(`siteCoverageSelfTest`) that gates whether every seed `site_id` resolves; that symbol does not exist either.
So the self-tests are dead documentation: the model-id sanitizer, the keeper-voice fallback, the reveal parser,
and (critically) site-coverage are all unguarded at build/boot.
**Fix (integrator):** add one boot-time harness (e.g. `SelfTests.runAll()` called early in `onEnable`, before
listeners register) that invokes each `*SelfTest()` plus a real site-coverage check (every `site_id` referenced
by an enabled seed puzzle/beat has a matching `sites.yml` entry), and refuses to enable (or loudly warns) on
failure. This is the cheap insurance the comments already assume exists.

---

## MEDIUM (orphaned config/sites wired to not-yet-built listeners)

### M1 — config + sites + admin command are wired to 4 Deep-Hold listeners that do not exist yet
`plugin.yml:31-33` correctly flags that the back-half adds `UnlitDeepListener`, `SeventhChoiceListener`,
`CoopPlateListener`, `IgnitionListener` and that the Java owner must register them. Today none of the four exist
in `signal/listener/`. Until they are built+registered, several surfaces are armed-but-inert:
- `config.yml:customs.unlit-deep.*` + `restraint.enabled` + `/observance restraint` toggle → read by the
  absent `UnlitDeepListener`. The `the_unlit_deep` group latch (the ONE collective-restraint custom, INV-17)
  does nothing.
- `sites.yml:the_unwriting` (type `seventh_shrine`) → watched by the absent `SeventhChoiceListener`.
- `sites.yml:coop_plate` (type `coop_plate`) → watched by the absent `CoopPlateListener`.
- `sites.yml:first_marker_01` + the prologue ignition → the absent `IgnitionListener`.
This is a **known cross-owner TODO** (plugin.yml says so), so it is not a silent breakage — but it IS an
orphaned-mechanic state per the consistency law: enabled config/sites with no code that expresses them.
**Fix (integrator/ Java owner):** build the four listeners and register them in `registerListeners()`
alongside the others; add their `name:` to `plugin.yml:softdepend` ONLY if/when they probe an optional plugin
(per the plugin.yml note, do not pre-add). Until then, leave a single tracked entry so the orphan is visible —
do not silently ship config that enforces nothing while `restraint.enabled:true` implies it does.

### M2 — `/observance restraint|falselaw|window` admin toggles: verify the command handler implements them
`plugin.yml:41-44` declares four new sub-commands (`sleep`, `falselaw`, `restraint`, `window`). The
integrator must confirm `command/ObservanceCommand.java` actually handles `falselaw`, `restraint`, and `window`
(not just `status`/`reload`/`sleep`) and that `restraint` writes the same flag `restraint.enabled` /
`UnlitDeepListener` reads (C-cross with M1). A declared-but-unhandled sub-command prints the usage block and
silently does nothing — a soft jank for the operator, and the `window` toggle is the live-shoot override the
Accepting depends on.
**Fix:** audit `ObservanceCommand.onCommand` for all four verbs; wire any missing one to its config flag +
a `saveConfig()`/live re-read. (Flagged for the integrator to verify, not yet confirmed broken.)

---

## LOW (cosmetic / doc drift; no runtime breakage)

### L1 — RoomSwapBeat's "un-final SmallStructureBeat" API request is moot
`RoomSwapBeat.java:27-31` (and BUILD-MANIFEST:160) say it "extends SmallStructureBeat" and leaves a one-line
request to un-`final` that class. The shipped RoomSwapBeat does NOT extend it — it composes `Schematics.paster()`
directly on `AbstractBeat`, which is the better choice (keeps the clear-footprint invariant intact).
`SmallStructureBeat` is still `final`. No action needed; just **delete the stale "extends"/"un-final" lines**
in the manifest + Javadoc so a later worker doesn't re-open that class for nothing.

### L2 — VoiceListener (the Ear, `MicrophonePacketEvent`) is referenced but absent (expected P3)
BUILD-MANIFEST:164 pairs `SpatialVoiceBeat` with a `signal/listener/VoiceListener.java` (Simple Voice Chat
`MicrophonePacketEvent` → Whisper STT). That listener does not exist; this is correct (P3/go-live, gated on
the `voicechat` plugin per plugin.yml:18-22). Note only so the integrator knows SpatialVoiceBeat currently has
no producer wiring it — it will only ever fire via a showrunner enqueue, never via the (unbuilt) Ear. Fine for
now; revisit at the Simple Voice Chat go-live.

### L3 — pack_format 34 vs server version is a go-live tuning step (already documented)
`pack.mcmeta:pack_format=34` (~1.21) with a README go-live note to match the server. Not a defect; listed for
completeness so the integrator doesn't treat it as a finding.

---

## CHECKS THAT PASSED (no action)

- Java 21 only: build.gradle toolchain 21 + `options.release 21`; FAWE 2.15.x / WorldEdit 7.3.10 / ModelEngine
  R4 reflected-only; BetterModel 3.x (Java 25) correctly excluded everywhere.
- Reveal discipline: RoomSwapBeat, RevealBeat both run the whole mutation inside `mutateWhenUnwitnessed` keyed on
  a lead block; `util/Reveal.java` is read-only LoS + witness-radius, "discovered never witnessed appearing".
- Deterministic fallback behind every optional dep: ModeledMobBeat → exactly NamedMobBeat on ModelEngine
  absent/throws/unknown-id; RoomSwapBeat → skip (room stays A) on FAWE absent; SpatialVoiceBeat → named pack
  sound (modulo C3); KeeperNpcListener → inert with no Citizens (reads PDC tag, never the Citizens API).
- Idempotency: RoomSwapBeat durable `swapped` chunk-PDC marker (re-read at mutation time); RevealBeat per-cell
  convergent + applied-set + `beat_queue.status`.
- sites.yml shape consistent: every new site has type/world/x/y/z/radius/protect/enabled; null coords =
  silently-skipped UNPLACED; `the_threshold` retype rationale is sound (no listener queries `placedOfType("marker")`).
- No `com.ticxo.*` / `com.sk89q.*` type is class-loaded by the new beats (string + reflection only) — they build
  and run with ModelEngine/FAWE absent.
- Rune font single source of truth: `resourcepack/.../runes.json` + README confirm the atlas is generated from
  `discord/src/forge/runes.ts` (cross-surface glyph agreement holds).

---

## MUST-FIX FOR THE FINALIZE INTEGRATOR (ordered)

1. **C1** — register the 5 new beats in `BeatLibrary.registerDefaults()` (one line each). Without this every
   Deep-Hold producer is a no-op.
2. **C2** — register `KeeperNpcListener` in `ObservancePlugin.registerListeners()` (one line). Without this the
   whole Keeper dialogue surface is dead.
3. **C3** — add the `keeper_voice` event to `sounds.json` so SpatialVoiceBeat's fallback is a real (eventually
   audible) sound, not silence.
4. **H1/H2** — either implement + invoke the promised self-tests (riteTokenSelfTest, the per-beat ones,
   siteCoverageSelfTest) from `onEnable`, or strip the false "self-test enforces it" guarantees. The
   accepting-token check (H1) is the climax — do not ship it unchecked.
5. **M1/M2** — build+register the 4 Deep-Hold listeners (or keep one visible TODO), and verify the
   `restraint`/`falselaw`/`window` admin sub-commands are actually handled and wired to their flags.
