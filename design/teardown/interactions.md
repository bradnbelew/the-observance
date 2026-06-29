# Teardown — Dimension: INTERACTIONS (in-game beats & Minecraft integration)

Adversarial review of the actual `plugin/.../beats/lib/*.java` + `signal/listener/*.java` +
`sites.yml` + `config.yml` + the atmosphere/structures/bestiary design vs `story-map.json`.
Verdicts: KEEP / CUT / FIX / SIMPLIFY / REDESIGN. Worst first.

---

### 1. [CUT/REDESIGN] The Quiet Herd "collective gaze" — `herd_anchor` site + bestiary §6 + sites.yml A12 — THERE IS NO BEAT THAT DOES IT
`sites.yml` defines `herd_anchor` (radius 16) and describes "the slow cosmetic Pale conversion,"
"the collective-gaze facing pass," and a "`paleTarget` lookup." The bestiary lists a "herd-turns-
to-face-you, together" beat. **No such class exists.** `grep` of `beats/lib/` for `pale|herd|
conversion|collective|gaze` hits only `SacredAnimalBeat` (tags ONE animal), `MapMarkBeat`,
`SacredAnimalBeat`, `UnlockBeat`. There is no facing-pass beat, no per-tick herd orientation, no
pale-spread. So the single creepiest cheap-vanilla idea in the whole bestiary ("the *ordinary* herd
turning as one is scarier than any model") is **vaporware** — the anchor and lore exist, the
interaction does not. Worse: even if built, vanilla animals re-path and wander every tick; a "they
all face you" pose decays in <1s without a per-tick `setRotation` loop fighting the mob AI, which is
exactly the kind of jank the project bans. **Fix:** either CUT the herd entirely (SacredAnimalBeat
alone — one glowing watched animal — already carries the custom), or REDESIGN as a *frozen tableau*:
spawn N `setAI(false)` armor-stand/mob copies pre-faced at the anchor, discovered never witnessed,
never a live re-facing loop.

### 2. [REDESIGN] Reveal discipline collapses with 4-6 friends together — `Reveal.isHidden` / every world-mutating beat
`Reveal.isHidden(loc)` returns true only when **NO** online player in the world is within
`witnessRadius` AND has line of sight. The whole point of this ARG is a convened friend group of
4-8 standing in the SAME ROOM. In a crowded base, `isHidden` over the lectern, the keeper-stone, the
hearth-marker is **almost never true** — so `SignWriteBeat`, `RevealBeat`, `SmallStructureBeat`,
`RoomSwapBeat`, `DecayCreepBeat`, `LecternFillBeat` all silently `mutateWhenUnwitnessed` → never
fire → retry → die. The author KNEW this (`isHiddenFrom` per-player exists, "MF-9") but the
world-mutating beats above call the GLOBAL `mutateWhenUnwitnessed(ctx, block, ...)`, not the
per-player client-illusion path. **The first report, the rune marker, the keeper-stone carvings —
the M1 backbone — will frequently never appear because someone is always looking at the base.**
Fix: route all in-base authored reveals through the per-player `sendBlockChange` path (the FakeBlock
mechanism) OR pre-build them at world-build and use `RevealBeat`-flip only behind a closed door /
in an unloaded forward chunk; do NOT depend on a globally-unwitnessed instant in a shared base.

### 3. [FIX] Answer-sign UX is invisible and unguessable — `AnswerSignListener` + `sites.yml answer_sign`
The in-world answer verb is: walk to a keeper-stone, **place your own sign within radius 3**, type
the answer, and it blanks with zero feedback (miss/hit both → silence; a hit's only tell is a later
beat elsewhere). Problems for a real friend group that "misses most subtlety": (a) nothing in the
world tells them a sign is the input device — there is no prompt, no placed blank sign, no "carve
here" affordance; the closed-loop is invisible. (b) radius 3 + `vertical-radius 3` is tight; on a
canted floor-slab keeper-stone the player stands ON the stone and places the sign a block away —
easy to land outside the sphere and get the same silence as a wrong answer. (c) waxed signs can't be
re-edited, but a *player-placed* sign isn't waxed — fine — yet a correct solve gives no in-world
confirmation AT the stone, so they'll re-submit endlessly thinking it didn't register. **Fix:**
world-build a pre-placed, labeled answer lectern/sign AT each stone ("speak here"); widen radius to
4-5; and on `SOLVED` fire a tiny local ack beat at the site (a sea-lantern lights) so the loop reads.

### 4. [FIX/CUT] Apparitions in the End are reveal-undisciplined by physics — `end_seventh_shrine` / `NamedMobBeat` / `SmallStructureBeat`
`sites.yml` itself admits the End is "wide-open" and the shrine must be a "PRE-GENERATED outer
island OR a re-dressed end-ship... NEVER a lazy paste toward an approaching glider." But the only
producers that exist (`SmallStructureBeat`, `RoomSwapBeat`, `RevealBeat`) all gate on
`Reveal.isHidden` / `isChunkLoaded`. Over the open End void, a player gliding toward an outer island
has **continuous line of sight for hundreds of blocks** — `isHidden` is never true, the chunk is
loaded the whole approach, so a runtime paste can NEVER fire unwitnessed. The shrine is therefore
**only viable as a 100% world-build artifact** (no beat involved), which means the entire `answer_
sign` `end_seventh_shrine` interaction reduces to "read a pre-built carving, place a sign." That's
fine — but the design's "reveal-safe force-load→mutate→unload" fantasy is impossible in the open
End. **Fix:** declare the End shrine BUILD-ONLY in structures.md (no producer beat), and drop any
implication that apparitions ambient-spawn there (sites.yml already says "ZERO apparition lane" —
good; make it literal in code by never enqueuing `named_mob` in `observance_end`).

### 5. [CUT] The offline-skin apparition is a silhouette that will read as "a warden, lol" — `NamedMobBeat` skin_player / `applyWornSkin`
The marquee "it wore *Brann's* shape" beat (B3/INV-16): `applyWornSkin` honestly admits it CANNOT
apply a player skin without the ModelEngine/NPC go-live layer, so today it sets a custom name and
**leaves a WARDEN/STRAY**. A tall black Warden silhouette named "Brann" standing in your base does
not read as "an apparition wearing my friend's body" — it reads as a named Warden. The separation
law (only when Brann is offline) is elegant and correct, but the *payload* is absent: the shape is
the whole point and the shape is a vanilla mob. Until ModelEngine + a player-skin rig is actually
wired (it is NOT — `ModeledMobBeat` only attaches a *model id*, never a player's texture), this beat
delivers none of its intended horror. **Cut from the shipping arc** (or gate behind the ModelEngine
go-live), and don't let the showrunner enqueue worn-skin apparitions until the texture path exists —
a mislabeled Warden is worse than no apparition (the project's own rule #1).

### 6. [FIX] `RoomSwapBeat` "the room rebuilds itself" needs a SEALED room or it's impossible — D5 / M3
The Undercroft A→B swap is the best horror beat in the design (omission, not motion). It gates on
`mutateWhenUnwitnessed(baseBlock)` — i.e. it only fires when no one can see the base block. For a
group exploring the Undercroft together this is the same crowd problem as #2: if they descend, look
around Room A, and leave together, there may be no unwitnessed instant until they're all far away and
the chunk unloads — at which point `isChunkLoaded` fails and the swap is skipped too. The swap works
ONLY if Room A is a sealed chamber they exit through a door that occludes line of sight AND they
travel far enough to unload — a narrow window. **Fix:** make the swap fire on a deterministic trigger
(door closes behind them + a short delay) using the per-player block-change illusion for the door
seam, and pre-stage Room B in an adjacent unloaded location so the "swap" is a teleport-on-reentry,
not a live FAWE clear-then-paste under the players' feet.

### 7. [SIMPLIFY] `SpatialVoiceBeat` is not spatial and admits it — late-arc voice
The beat's own javadoc + code: a resource-pack key "has no Sound enum, so vanilla per-player audio
can only play it AT the player" — it calls `PerPlayer.namedSound(pl, named, vol, pitch)`, which is
NON-positional. The `behind`/`offset` payload fields are **read but never used**; the "from behind
you" read is entirely faked by the clip's own baked panning. So "the dark said your word back from
over your shoulder" is, in vanilla, just a sound playing centered on the player. It only becomes
truly spatial with the Simple Voice Chat install (the one justified Path-B install) which is NOT
wired. **Simplify:** drop the `behind`/`offset` fields (dead params invite mis-authoring), rename to
reflect it's a private non-positional clip until SVC lands, or use `PerPlayer.soundAt(loc)` with a
real behind-the-player location for a *vanilla* sound (which IS positional) instead of pretending a
named pack key can be.

### 8. [FIX] `FakeBlockBeat` reverts on any chunk refresh / relog — the deniable cornerstone is fragile
`FakeBlockBeat` is the cleanest "it knows ME" beat (client-only, zero grief). But its own doc notes
the illusion clears on a chunk refresh, and the revert timer only resends the real block if the
player is still online at the SAME spot. In practice: the fake soul-sand wall appears, the player
walks 20 blocks (chunk re-sends real state from server), the wall is gone before the 6s timer — or
they relog and it's gone. For a *brief* "did you see that?" this is acceptable, but it means the beat
can't hold a fake structure long enough to be investigated, and `getTargetBlockExact` aiming means it
often lands on whatever block they happen to be looking at (sky, their own build) rather than a
dramatically-placed spot. **Fix:** for held illusions, periodically re-send while the player is in
range (a short repeating task, not one-shot), and prefer an explicit anchor over `getTargetBlockExact`
so the fake lands somewhere authored, not on the player's reticle.

### 9. [CUT] `ModeledMobBeat` is dead weight until ModelEngine is bought AND models are authored
It's a careful reflection bridge that, with no `model` field or no ModelEngine plugin (the default,
since ModelEngine R4 is PAID and unpurchased), is **byte-for-byte `NamedMobBeat`**. There are no
authored ModelEngine rigs, no purchase, no `model:` ids anywhere in the seeds. So this class is a
~240-line no-op clone shipping today. It's harmless but it's complexity with zero current payoff and
it implies a capability the project doesn't have. **Cut from the active set** (keep the design note);
re-introduce only the day a rig is actually authored. Same applies to the whole "custom 3D
apparition" column of the bestiary §0.4 — every creature is "ships as a ModelEngine model" and NONE
exist.

### 10. [FIX] Apparitions in the Nether will spawn floating or in lava — `NamedMobBeat.findSpawn` / `nether_forge`
`findSpawn` rings 12 candidate angles at fixed `distance`, calls `Placement.findSurfaceSpot(... +2, 6)`
and checks 2 blocks of headroom. In the Nether (`observance_nether`, the forge pocket + soul gallery)
"surface" is ambiguous — the roof, lava lakes, and stacked caverns mean `findSurfaceSpot` can return a
ledge over lava or fail entirely, and the P2 "basalt-corridor keeper-shape" glimpse the design wants
near `bastion_remains` has no special Nether placement logic. The honest outcome: most Nether
apparition attempts return `no-spawn-spot` and silently never fire, OR land somewhere absurd. **Fix:**
the Nether lanes should use BUILD-PLACED static apparition shells (a posed `setAI(false)` figure in the
authored pocket), not the runtime `findSpawn` ring — the Nether geometry is too hostile for blind ring
sampling.

### 11. [FIX] Resource-pack font dependency is load-bearing and un-degraded for the WHOLE cipher — `[PACK]` runes font
Per atmosphere-stack §2, the entire substitution-cipher legibility (keeper-stones, Rosettas, every
`SignWriteBeat`/`LecternFillBeat`/`BossBar`) depends on the `observance:runes` font in the one
auto-pushed pack. The doc says beats "fall back gracefully (default font) until the pack is live" —
but a player who **declines** the pack prompt, or whose pack download fails, sees the runes render as
*Latin private-use-codepoint garbage / tofu boxes*, not as a graceful plaintext. There is no
`ResourcePackStatusEvent` gate in the plugin (it's a "new tiny class to add: ResourcePackPusher" —
i.e. NOT BUILT), so nothing detects a decline and nothing adapts. For a friend group, ONE person
declining = they can't read any clue and don't know why. **Fix:** build `ResourcePackPusher` with
`force=true` + a decline→kick-with-reason (or a dashboard red flag), and ensure the cipher has a
non-font fallback surface (the Discord `#the-record` mirror) so a packless player isn't dead in the
water.

### 12. [SIMPLIFY] `SacredAnimalBeat` glow + the "kill it = transgression" loop is gimmicky and easy to miss/abuse
The Sacred Beast is "one COW, glowing, silent, persistent." For a friend group: a single faintly-
glowing cow in a world full of cows is trivially overlooked, OR trivially griefed (one arrow ends a
whole permanence fork). `setSilent(true)` on a cow is barely perceptible; `glow` is opt-in
(`p.bool("glow", false)`) so by default it isn't even visually distinct. The "herd watches it" payoff
that would make it legible doesn't exist (#1). So the gentlest custom is also the most invisible — and
its one consequence (irreversible permanence fork on kill) is a heavy outcome hung on an animal nobody
noticed. **Simplify:** make glow default-ON and give the Beast a rune-name nameplate so it's
unmistakably "the watched one," OR fold it into the Offering/Kept-Light customs and cut the standalone
beast.

### 13. [FIX] `the_threshold` "crouch to pass" lintel is not enforced by any interaction — geometry only
structures.md describes Orin's lintel as "a low stone lintel forcing a crouch to pass." That's pure
build geometry (a 1-block-high gap forces sneak in vanilla) — fine and elegant. But the *custom*
("The Bow" = honored by crouch within `bow_marker` radius) is detected by `CustomComplianceListener`
at `bow_marker` sites, which is a DIFFERENT site type than `the_threshold`. So crouching through
Orin's lintel does NOT register as a bow unless a `bow_marker` is co-placed there. The design implies
"the architecture builds the bow in — you stoop to read the stones," but the keeper-stones are
`keeper_stone` type, not `bow_marker`, so stooping at them earns nothing. **The bow custom only fires
at the 1 explicit `bow_marker_01` site.** Fix: either co-locate `bow_marker` sites at the lintel and
each stone, or extend `CustomComplianceListener` to honor a crouch within any `keeper_stone`/
`the_threshold` radius (the "stoop to read" the design keeps promising).

### 14. [KEEP] The Accepting synchronized-bow detector — `AcceptingRiteListener`
This is the strongest in-game interaction in the build and it should ship as-is. It's a real
performed gesture (everyone present on the floor sneaks at once), the token is opaque/un-typeable so
the climax can't be spoofed at a sign or wiki, the active-only quorum clamp (`min(cfg, activeRoster)`)
correctly never punishes an absent member, and it's fate-neutral (the M5 composer decides the close).
One residual risk: `PlayerToggleSneakEvent` fires on the START of a crouch, and it checks "every
present player is sneaking" — with 6 friends the timing window is genuinely hard to hit
simultaneously, which is GOOD (it's a rite) but verify the cooldown + the "reusable, re-arms" path so
a near-miss doesn't lock them out. KEEP; playtest the simultaneity window with real latency.

### 15. [FIX] `PrivateDarknessBeat`/`PrivateParticleBeat` near-player placement uses player FACING math that reads as a bug
`PrivateParticleBeat` places "near_player" particles by walking BACKWARD along the player's look
vector (`getDirection().multiply(-offset)`) and up `height`. So the "wisp in the doorway" actually
appears *behind the player, mid-air, wherever they happen to be facing* — not at a doorway, not at the
cairn, unless an anchor is set (and then `near_player` must be false). In a base this means smoke
puffs floating behind people at random head height — reads as a particle bug, not dread. **Fix:**
default these to anchor-placed at authored atmosphere spots; reserve `near_player` for the rare
deliberate "right behind you" beat, and clamp height to a sensible eye/floor level.

### 16. [SIMPLIFY] `RevealBeat` `lit` flip on non-lightable blocks is a silent no-op footgun — authoring hazard
`RevealBeat` is solid (flip air→stone, light a lamp). But the `lit` kind degrades to a no-op on any
non-lightable block "(a 'lit' flip on stone is a misconfig, not a crash)" — meaning an author who
points a `lit` cell at the wrong block gets SILENCE, the exact same as success, and the M5 "slot
lights up" interim reveal just... doesn't, with no signal. Given the showrunner authors these by hand
and the project's whole risk is "things silently never fire," a silent-no-op on misconfig is the wrong
default for a load-bearing reveal. **Fix:** in debug mode, log a warning when a `lit`/`state` cell
hits a block that can't accept it, so a bad reveal is visible in the dashboard health panel instead of
being indistinguishable from a working one.

### 17. [FIX] Multiverse void worlds (Undercroft/Nether/End) + chunk-unload = beats targeting them die — cross-dimension sites
`sites.yml` celebrates that cross-dimension sites need "ZERO new tracking infra." True for proximity.
But every WORLD-MUTATING beat gates on `isChunkLoaded`. In separate Multiverse worlds that are empty
99% of the time, the target chunks are UNLOADED whenever no player is in that dimension — so any
showrunner-enqueued `reveal`/`sign_write`/`small_structure` at `the_unwriting`, `nether_forge`, or
`unbroken_light` while the group is topside is skipped (`anchor-unloaded`). The beats only fire if a
player is already standing in the dimension, which often defeats the "discovered, never witnessed"
goal (they ARE there). **Fix:** for dimension sites, either force-load the target chunk for the
mutation tick (then unload) or commit fully to BUILD-PLACED content in those worlds (no runtime
producers), which structures.md half-implies but the seeds don't enforce.

### 18. [KEEP/FIX] `SignWriteBeat` waxing + protection is correct — minor: rune font assumed
`SignWriteBeat` correctly waxes the sign (players can't edit it) and registers it with the protected
registry. Good anti-grief. The only interaction caveat: it writes the literal payload lines, which are
meant to render in the rune font — so a packless player sees them in Latin (see #11), and a 100-char
clamp per line will overflow the 4-line sign visually (signs wrap/truncate at ~15 chars rendered).
**Fix:** clamp to a render-realistic length and confirm the rune glyphs fit the sign's pixel width,
or use a text-display entity (already in atmosphere-stack) for longer carvings instead of a sign.

---

## If I could change ONE thing about this dimension

**Stop pretending the world mutates live around a convened group, and pre-build everything; reserve
runtime beats for the per-player client illusions only.** The reveal-discipline engine
(`Reveal.isHidden` + `mutateWhenUnwitnessed`) is architecturally sound for a SOLO From-The-Fog
experience, but this is a 4-8 person friend group standing in one base and descending one hold
together — the globally-unwitnessed instant the world-mutating beats wait for **mostly never comes**,
so the M1 marker, the keeper carvings, the Undercroft swap, and the End/Nether shrines will silently
fail to appear exactly when the group is present to be scared. Make the in-game layer two clean
tiers: (1) **world-build static** — every stone, shrine, lintel, hearth, grave, and dimension room
is built by hand and placed once (structures.md already wants this; enforce it, delete the runtime
producers for those sites), and (2) **per-player packet illusions** — `FakeBlockBeat`, private
particles/sound/darkness, and worn-skin apparitions delivered via `isHiddenFrom(player)` /
`sendBlockChange` to each member at the moment THEY look away. That single re-tiering kills the
crowd-occlusion failure mode (#2, #6, #17), makes the End/Nether honest (#4, #10), and leaves the
genuinely-good performed interaction — the Accepting bow (#14) — as the one live world event that
actually works.
