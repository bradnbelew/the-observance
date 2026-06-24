# THE OBSERVANCE — THE INTEGRATION PLAN (end-to-end, build-ready)

> **What this is.** The single, scannable front-to-back plan for how the STORY (WORLD-BIBLE, the Deep
> Hold) fuses with the MINECRAFT engine + the DISCORD layer into ONE world with ONE voice and ONE state —
> then what to build first. It is the merge of the six integration seams (this file supersedes the
> separate `INTEGRATION-SEAM6.md` wiring synthesis and the Seam-5 creep catalog that previously lived
> here; both are folded in below).
>
> **Read alongside:** `arc/WORLD-BIBLE.md` (the sealed truth), `design/PLAN.md` (the shape),
> `design/ARG-RESEARCH.md` (the craft), `design/COHERENCE-AUDIT.md` (what's built vs unwoven),
> `design/cipher-web.md` + `design/clue-web.md` (the puzzle graph), `discord/ORACLE.md` (the resolver).
>
> **Ground-truth (verified this audit).** The engine is real and often *ahead* of the docs: the forge
> ships **11 self-tested ciphers** (`discord/src/forge/ciphers.ts`); the plugin ships the **~27-class
> beat palette** (`plugin/.../beats/lib/*`) with reveal discipline + FAWE paste; **7 signal listeners**
> detect the seven customs; the deterministic showrunner spine compiles. A concurrent session has ALSO
> closed several COHERENCE-AUDIT P0s — the **X1 forge-spec bind** (`forge/clue-specs.ts`), the
> **customs→report bridge** (`showrunner/customs.ts`), the **drip-carries-a-card** fix, and the **drip
> pool/order** filter. This plan integrates AROUND those as DONE. **The dominant remaining work is
> CONTENT, not mechanics** — the `.schem` corpus + `sites.yml` placement is the deepest hole.
>
> **READ-ONLY boundary.** This file is the only write. It does NOT touch `plugin/`, `discord/src/forge/`,
> or `discord/src/showrunner/` (a concurrent session owns those). Everything proposed here lands in
> SUPABASE seed/schema, `design/`, `arc/`, config (`sites.yml`/`config.yml`), the resource pack, and the
> `.schem`/asset corpus.

---

## 1. THE INTEGRATED EXPERIENCE (one page)

A friend group spawns into an **ordinary survival server** — no quest, no marker, 95% safe and quiet
(`F1`/`F8`). They build and wander. Out on the map is the lost **Mouth** of a buried colony; nearby,
half-sunk hamlets with **surface NPCs** living among the ruins, **half-survived books**, posters, and
staged tableaux that cross-reference each other. They start **reconstructing** five questions — WHO were
the Kept, what was this PLACE, what HAPPENED, what's on the SURFACE with us now, were they HUMAN — from
fragments no one person holds (`C3`). Every find logged becomes a **Recovery Archive card** in Discord
that clusters into the **five colored threads** the group watches fill in (`C4`–`C7`).

To go **deeper** they must **decode ciphers** carved on keeper-stones — and the descent itself is
**gated** by them: five sealed thresholds (one per level of the Hold) will not open until a cipher is
solved, and **each solve also pays a Deep-Hold lore fragment** (gate AND reward, `E3`). They learn the
**ways** (the seven customs) not from a tutorial but by **breaking them** — a transgression earns an
obvious-but-unexplained soft toll (a torch gutters behind you, a cold pulse, a footstep), which they
connect over sessions and must **decipher how to stop**; deciphering the way IS learning it
(discover-by-punishment, Req 3). The **Watcher** — the accumulated Kept, watching from the dark — makes
itself felt through **rare, well-timed, per-player creep beats** with long **fallow** silences, never
overwhelming at spawn (Req 4). They descend the **wrong-scaled deep**, the keepers' journals decay, and
at the **Undercroft** they perform the **Accepting**: the world judges them collectively by how they
kept the ways — kept (let go) or accepted (taken, made the next Watchers).

**How the lore-world and the KEPT cipher-web fuse:** the cipher web (kept verbatim — 11 forge ciphers +
the 23-node oracle web) is **re-skinned onto the Hold's vertical geography**. Each keeper-stone is a
*person*; each cipher *is* that keeper's nature (Vaun's withholding = a Caesar shift; Sella's reflection
= Atbash; Iss's own name = the Vigenère key, and the name is the lie). The ciphers are the **spine**
(you literally cannot reach the Dark without solving them); the world is what **wraps** them so a
solve reads as *reading the world*, never *clearing a checklist*. Length comes from **breadth** (many
parallel optional threads + the rumor→verify travel loop) and **cadence** (a 20h drip, fallow days),
never from artificial locks — so it is long and un-speedrunnable without ever frustrating.

**One world, two registers, one state.** Discord **speaks** (`voice.ts` — the Watcher, lowercase, sparse,
certain); the world **acts** (beats — a torch, a sound, a structure). Both resolve through ONE
`resolveAnswer` against ONE `puzzles` table; a clue solved in-world can't be re-solved in Discord.
Supabase is the single source of truth. The **showrunner** (a zero-LLM cron) conducts the pacing; the
**dashboard** is the director's console for the on-camera peaks.

---

## 2. THE PROGRESSION SPINE

The WORLD-BIBLE's 8-step timeline maps onto the 5 movements and the Hold's vertical levels. **Three kinds
of gate, every one ALSO a reward (`E3`):** a **cipher** (decode to pass), a **custom** (perform to pass),
a **reconstruction** (hold N fragments). The descent is bound 1:1 to the geography — one hard cipher gate
per level transition — so "ciphers gate progress" is the spine, not a bolt-on.

### 2.1 The descent map (region → gate → reward)

| Hold level (descent) | Movement | Player does (Minecraft) | HARD GATE (blocks deeper) | Cipher / custom | Lore reward (debt paid) |
|---|---|---|---|---|---|
| **Surface (Mouth)** | I — Ordinary | spawn topside; find first hamlet + half-burned book; ONE quiet wrongness | *(none yet)* | — | FACT 1/2: the record counts the living; graded before told |
| **Threshold → Warrens** | I→II | assemble the rune-ring Rosetta (icon ring **B** + founder note **C**) | **`rosetta-ring`** → `flags.rosetta_known` (the master script) | P-LIT rune-letter Rosetta (substitution literacy) | FACT 3/4: keepers before you; the land counts first |
| **Warrens/Lamp-works → Cisterns** | II→III | decode Mara's six-book shelf: "descend and bow at the unbroken light" | **`stone-mara` → `undercroft-descent`** → `flags.undercroft_open` | P5 book cipher → P16 rite (performed, not typed) | FACT 5 (Mara) + the descent named; The Going-Out |
| **Cisterns → Deep Market** | III | read a coord/polybius bearing off the Cistern walls | **coord/polybius marker-grid** | P6 coordEncode / P9 polybius (the world IS the codebook) | PLACE: the wrong-scaled deep; FACT 11/12 seed |
| **Deep Market → the Stair (Deep Line)** | III→IV | light Brann's beacon/lamp sequence on the black moon | **black-moon timing + sequence** | P13 timing + P14 beacon + P7 rail-fence (illegible by day) | FACT 11 (the one kept fire); Brann's way |
| **the Stair → Undercroft** | IV | turn Iss's key on the *other* stones; re-walk a "solved" clue (D09 vs D10) | **`no-wall-catch`** → `flags.iss_caught` + `true_coord_known` | P3 Vigenère (the catch; the true coord) | FACT 8: the ways are not a wall; Iss's hope killed them |
| **Undercroft (Accepting)** | V | components + one personal token per keeper; ALL bow as one at the hour | **`rite-tokens` → `accepting-crouch`** (detected; plugin sentinel) | P15 arrangement + P16 synchronized bow | FACT 13/14/15 (felt): the missing tool is YOU; the Watcher was the Kept |

### 2.2 Why it CAN'T be finished without ciphers, and CAN'T be speed-run

- **Cannot finish without ciphers.** The two structural spine keys are ciphers: **Mara's book cipher**
  (`stone-mara`) is the sole producer of the descent door; the **Iss catch** (`stone-iss-wall` → Vigenère)
  is the sole producer of the true Undercroft coordinate. Neither has a non-cipher bypass. The Accepting
  is a *performed custom* (detected world-state), not a typeable string — so the run also cannot finish
  without performing customs.
- **Cannot single-point-stall.** Four parallel entry paths to literacy; the six stones are a **field**,
  not a row (any 4-of-6 fragments clears the `fragments>=4` gate); **every hard gate has ≥2 in-doors**
  (`clue-web §2`); the resolver ignores `movement`; the **Whisper auto-gift** backstops a genuinely-stuck
  group so no hard cipher ever hard-locks the run.
- **Cannot speed-run (without padding).** Length is structural, not artificial: the **20h drip cadence +
  fallow days** physically prevent consuming the arc in a weekend; **N-of-M fragment gating** needs broad
  reconstruction held across players; the **rumor→verify travel loop** (hear of a place → walk 2000 blocks
  → verify/contradict) is intrinsically un-skippable; **customs must be LEARNED** (deciphered from
  consequence, not looked up). The drama budget caps beats so dread is *earned*, never a firehose.

### 2.3 The side-track layer (breadth that gates nothing)

Optional threads run off the spine and **gate nothing** — they are where weeks of richness live (Req 2).
Three tiers so the lazy explorer, the lore-reader, and the cipher-solver each have an off-spine lane:
**AMBIENT** (a tableau you just find — near-free to author), **RUMORED** (an NPC/sign points you; you
verify), **KEYED** (needs a side-cipher solved). All are `puzzles` rows with `outcome_type:'side_quest'`
(branch) or `'lore'` (told) — **no resolver change** (the seed already ships `stone-sella → seventh-shrine`
as exactly this shape). Concrete threads:

| Thread | Tier | What it is | Gates? | Pays |
|---|---|---|---|---|
| **The Seventh** | RUMORED | miscount the shore markers → the cast-out shrine | No | FACT 10 (the land can refuse) + Whisper budget |
| **A taken keeper's child** | AMBIENT | Sella's drawings in date order → a child's bed + packed chest | No | WHO/HUMAN; a personal token for M5 |
| **The false way up** (Iss's dug shaft) | RUMORED | a `MapMark` bearing to an "almost-exit" that ends in fill — the SINGLE LONGEST digression, mimics the win condition | No (dead_end) | reframes the whole run: there is no climbing out, only the rite |
| **A sealed personal vault** | KEYED | a side-cipher key found in a journal → a time-capsule room | No | a renamed keepsake; HUMAN |
| **A surface-NPC request** | RUMORED | Coll asks you to recover/return something | No | a rumor card + lore (Phase-2 NPC) |
| **unspoken-refrain / haunted-herd** | AMBIENT | solve-by-restraint / protect the deep-bird | No | Whisper/atmosphere + a Movement-V boon |

> **The travel-loop is the primary time sink, not the puzzles** — lean the breadth budget into MANY
> short rumor→verify pairs distributed across the map. A hard cipher can be whispered; a 2000-block walk
> cannot. The **false way up** is the highest-value red herring: it weaponizes the group's own (correct,
> per Iss) intuition that the surface healed.

---

## 3. THE SYSTEMS (each with a MINECRAFT + DISCORD column)

### 3.1 GATING CIPHERS — the varied catalog (Req 1)

**Kept verbatim + expanded.** The 11 forge ciphers + the 23-node oracle web are re-skinned onto the
Hold, then expanded to a 24-format catalog (11 text ciphers + 13 world-native modalities). **Variety
budget:** no cipher family used more than ~3× across the web. The five currently-orphaned ciphers
(railFence/columnar/polybius/a1z26/morse — built + self-tested, used by zero active node) are promoted
onto real nodes (PATH A) — un-orphaning dead code AND hitting Ethan's "many varied formats" in one move.
**The one genuinely-new build is P17 steganography**, hiding exactly one high-value key (the Vigenère
key) so the hardest chain spans card → image-editor → stone.

| # | Format | Keeper / home | MINECRAFT | DISCORD | Gate role |
|---|---|---|---|---|---|
| P-LIT | Rune-letter Rosetta | Threshold | `SmallStructure` first stone + `SignWrite` glyph→letter teaching; icon-ring twin | `rosetta-ring` row, `set_flags{rosetta_known}` | **HARD** literacy (2 doors) |
| P1 | Caesar shift | Vaun (Warrens) | runes at Vaun's cairn; wheel-sign states shift; D02 "three of each"→3 | `stone-vaun`, forge `caesar` | side-lock (lore) |
| P2 | Atbash mirror | Sella (shore) | carving reads as nonsense until you FACE the water | `stone-sella` → Seventh | side-lock → Seventh |
| P3 | Vigenère (key=ISS) | Iss (the Stair) | warm reading → dead shrine; key on others → "the one who turned away" | `stone-iss-wall`→`iss-doubt`→`no-wall-catch` | **HARD** (the catch; true coord) |
| P4 | Substitution | Orin (low lintel) | LOW LINTEL forces a crouch (the Bow) to read | `stone-orin`, forge `substitution` | side-lock (lore) |
| P5 | Book cipher | Mara (Lamp-works) | six-book lectern shelf walked L→R → "descend and bow at the unbroken light" | `stone-mara`→`undercroft-descent` | **HARD** descent key |
| P6 | Coordinate encode | Cisterns | digit-glyph `X..,Z..` read via the Stone of Reckoning | forge `coordEncode` | hard + side (coords `active=false` until Reckoning placed) |
| P7 | Rail-fence | Brann (cold hearth) | legible only at NIGHT; rails = "fires counted" (D08) | `stone-brann`, forge `railFence` | component of black-moon HARD gate |
| P8 | Columnar + Acrostic | Warrens record-room | first glyph of each line spells a buried keeper-name | forge `columnar` / `[NO CODE]` acrostic | `dead_end` texture (true, opens nothing) |
| P9 | Polybius marker-grid | Deep Market | a 5×5 grid of marker-stones — the world IS the codebook | forge `polybius` | in-door to the true-coord gate |
| P10b | A1Z26 + Morse | near spawn | tick-mark stave / dot-dash carving + Morse-table prop | forge `a1z26` / `morse` | shallow early teaching rungs |
| P11 | Lamp-sequence lock | Kept Light | light campfires/lamps in a named order (listener-detected) | `[NO CODE]` sentinel row | part of black-moon HARD gate |
| P12 | Deep-Market stall order | Deep Market | the order of stalls is the cipher; reads off a word/name | `[NO CODE]` arrangement | optional WHO/PLACE lore |
| P13 | Black-moon timing | Brann | carving renders ONLY at the taboo phase (`DarkHoursListener`) | `[NO CODE]` time-gate | **HARD** timing on Deep Market→Stair |
| P14 | Note-block / drone melody | Brann | tone→colour sequence reproduced in lamps/glass | `[NO CODE]` sound + arrangement | component of black-moon gate |
| P15 | Altar item-arrangement | Undercroft | deposit named tokens in exact slots (sunwise order) | `[NO CODE]` arrangement listener | **HARD** Accepting component |
| P16 | Synchronized bow rite | Undercroft | all present BOW at once + deposits + the hour (detected) | `accepting-crouch` plugin sentinel | **HARD** terminal gate |
| P17 | Steganographic clue-card | (the PNG) | faint 2nd rune layer / LSB in the forged card image | `[BONUS-UNBUILT]` — the ONE new build | optional 3rd in-door to "Iss caught" |
| P18 | Cross-keeper correlation | two levels | two artifacts held against each other; the answer is the contradiction | `[NO CODE]` meta-modality | drives the HARD Iss-catch |
| P19 | Deep Line depth-reading | the Stair | the marked Y-depth / strata count encodes a bearing | `[NO CODE]` observation | optional→HARD in-door to the Stair gate |
| P20 | Deep-bird canary | (over the run) | protect a tagged pale mob (`SacredAnimalBeat`); dies → you go dark unwarned | `haunted-herd` side_quest | never gates; Sacred Beast lore |
| P21 | The Unspoken refrain | (chat) | abstaining from the taboo word is the solve (`ChatListener`) | `unspoken-refrain` side_quest | never gates; discover-by-punishment in miniature |
| P22 | Pressure-plate glyph-walk | Undercroft floor | walk the rune footstep-by-footstep over plates | `pressure-glyph-walk` lore | optional redundant approach to the rite |

**Gating model (the spine):** exactly **one hard cipher gate per level transition**, each a sealed
`DoorOpen`/`SmallStructure` threshold opened ONLY by an `UnlockBeat` from a solved puzzle, each carrying a
`voice_key` + `voice_args.fragment` (the lore). All inside the existing schema — one `puzzles` row per
node, `accepted_answers = decode(authored ciphertext)` via the X1 bind, the 5 `outcome_type`s, `set_flags`
into `arc_state`, `next_puzzle_key` edges. **No resolver change, no new beat type** — `UnlockBeat`
already dispatches the whole palette.

### 3.2 CUSTOMS + the DISCOVER-BY-PUNISHMENT loop (Req 3)

The engine already detects seven customs and (a concurrent session built) the customs→report bridge
(observe≥1 → warn≥3 → left≥5 ladder + soft reversible toll). The GAP this fills is the **clue half** — the
in-world teaching surface that lets a player learn what they broke and how to stop. The loop, per custom:

```
(1) transgress → ps.violate(CUSTOM_X)           [pure tracking, rate-limited, never cancels]
(2) STAGE A — deniable, immediate, per-player    [same act → same response, learnable; ~1-2/session]
                a torch gutters / a cold pulse / a footstep-from-behind — NO message (silence is canon)
(3) STAGE B — on repetition, the bridge crosses observe→warn→left, the Watcher NAMES the lapse +
                lands the soft reversible toll  [the punishment becomes legible as a class of action]
(4) DECIPHER — the player reads the WAY from a placed clue (journal line / lintel / Rosetta / NPC)
(5) STAGE C — performing the way once (ps.honor) clears the toll + lands a quiet "kept" boon
```

This is the **meta-cipher**: each custom is an un-taught cipher whose plaintext is **behavioral**. It
turns Req 3 into a 7-node puzzle layer that costs only the customs→report bridge (already built) + each
way's **teaching surface** (the authoring contract: never land a listener without its clue + consequence).

| The way (custom) | MINECRAFT (detect + toll) | DISCORD (report) | Teaching surface |
|---|---|---|---|
| **Keep the Lamp** (`the_kept_light`) | `LocationSampler`; base goes dark → `TorchGutter` | `reportObserved` "kept the light" | Mara's stone / rune-ring |
| **The Deep Line** (`the_deep_line`) | `BlockBreakListener` past the marked Y | report; the gravest small act | the painted depth marker (P19) |
| **The Dark Hours** (`the_dark_hours`) | `DarkHoursListener` on the taboo phase → `PrivateDarkness` | report "kept the watch" | Brann's night-gated page (P13) — **seed it** |
| **Give Back** (`the_offering`) | `BlockBreakListener` / "last of" scan | report; rhymes with Vaun the hoarder | the offering cairn |
| **Bow at Markers** (`the_bow`) | `CustomComplianceListener` (crouch at marker) | report "made the bow" | Orin's low lintel (P4) |
| **The Unspoken** (`the_unspoken`) | `ChatListener` — **author `forbidden-words`** (ships `[]`, can't fire) | report "kept the word unspoken" | `unspoken-refrain` (P21) — **seed it** |
| **Keep the Deep-Bird** (`the_sacred_beast`) | `DeathListener` (tagged mob) | report; lost early-warning | `haunted-herd` (P20) — **seed it** |

**Wiring:** an Attention/Observance accumulator (rises on any violate, decays on restraint) tiers the
creep density (§3.3). The bridge is already custom-agnostic — new customs need ZERO bridge change, only
one `CUSTOM_PHRASES` entry in `voice.ts`. **For honorless ways (Seal, Unspoken)** the "reward" is the
*absence* of punishment (the cold simply stops) — design the feedback as relief, not a boon line.

### 3.3 CREEPY PALETTE + TIMING (Req 4)

The catalog is large *so no beat ever repeats* — but it fires **sparingly, per-player, with long fallow
silence**, so each lands as story, not a scare. The craft law: **a beat is allowed only if it reads as the
Dark/a Watcher noticing, or the un-witnessed being changed** (WORLD-BIBLE §3). Three engine facts make
restraint automatic: reveal discipline is enforced in code (`AbstractBeat.mutateWhenUnwitnessed` abandons
silently if you're looking); beats are per-player by default; tolls take warmth, not progress.

| Tier | When | Beats (BUILT unless noted) | MINECRAFT | DISCORD |
|---|---|---|---|---|
| **A — DENIABLE** (T1) | Movements I–II; the only tier near spawn | half-beat footstep from behind (NEW = `PrivateSound`+stop-detector), whisper/knock, edge particle, brief per-player time/fog, **torch gutter**, a lamp you placed dark on return, subtle decay | one player, never shown, deniable | **none** — never announced (TINAG) |
| **B — PERSONAL** ("it knows me", T2) | II–IV, raised Attention, rationed HARD | **NamedMob** stand-and-stare Watcher, a figure that was-there-then-isn't, a book/sign with your real deaths/name, a name-whisper when alone, ≤15s forced blindness at a reveal | real dossier data (`SignalSnapshot.deaths`, heatmap); 1–2 per player across the WHOLE run | archivist may echo *after* the player surfaces it (never before) |
| **C — WORLD** (T3) | IV–V peaks; the conductor stages these | a sealed door now open, a room rearranged on return, "grass grew where you left it", an overnight book, a set-piece that's just *there*, the deep "un-lights" | mostly discovered-on-return (proof-of-presence), reveal-disciplined | a single Watcher line drips *after*; or a redacted incident note |

**Timing discipline (one line):** *tier by movement, count by drama-budget, target one player, rest on
fallow days, conduct the peaks.* Four of the five enforcement layers are already-built spine patterns
(movement gating via `arc_state.current_act`; the per-(player) high-water idempotency in `customs.ts`;
per-player default; the `dripIntervalMs` fallow cadence; AUTO⇄CONFIRM for conducted peaks). **The one
real build is a creepy-beat selection pass** in the showrunner (input = movement + per-player Attention +
budget; output = at most one tier-gated beat). The Tier-A beat themed to the broken custom IS the
discover-by-punishment punishment — a creepy moment that *teaches a way*.

### 3.4 SURFACE NPCs + SIDE STORIES + UNTETHERED LORE (Req 2, Req 5)

The entire surface cast is **Citizens2-feasible** and branches on the SAME dossier + `custom_compliance`
the engine already measures — so the cast "knows you" with zero new measurement infra. **Discipline:**
surface NPCs speak in the **HUMAN register** (NOT `voice.ts` — they are people, not the presence; a
screenshot of an NPC line and a Watcher line must read as different authors). Untethered content pays a
**TEXTURE debt** (the world feels real) — Ethan's explicit carve-out from the fragment→revelation ledger.

| Layer | MINECRAFT | DISCORD | Bible tie |
|---|---|---|---|
| **5 surface NPCs** — Aro (rumor-broker who lies), Wenna (half-remembers; mutters the ways as folk-superstition), Coll (fetch/return economy, conduct-reactive prices), **Dob** (descends with you; found below as a NAMED Watcher), Old Pell (won't descend; remembers your transgressions) | Citizens2 NPCs; dialogue trees on `NPCRightClickEvent` reading the Supabase dossier; PDC-validated fetch/return (reuses the altar/DeathListener pattern); `TextDisplay` holograms | rumors post as grey SURFACE rumor cards that flip verified/contradicted on arrival; NPCs **never** post (they're people) | SURFACE/HUMAN; Dob previews the induction twist on a stranger you KNEW |
| **6 keeper side-stories** — Sella's predictive drawings (incl. one of the GROUP), Mara&Sella's adoptive bond (the grief engine), Brann's self-rewriting sleepless logs, Orin's mason's-marks hunt, Vaun's two-ledger guilt, Iss's letters home | `BookAppears`/`LecternFill`/`MapMark` staged in date order; pages-NBT swaps out of LoS (self-rewriting); night-gated reveals | WHO/HUMAN cards the archivist annotates; cards auto-cluster ("this lampwright was the child's mother") | the keepers as people you grieve (WORLD-BIBLE §2); makes the Kept human |
| **12 untethered oddities** — a working lift, Coll's dice game, a hidden lampwright's workshop, the deep-bird aviary, an impossible cave-forest, a music-disc puzzle-box, easter eggs | real MC mechanisms + `FakeBlock`/`DoorOpen` hidden rooms; `SacredAnimalBeat` reuse; FAWE set-pieces | at most a single archivist-shrugged "oddity" card; most are in-world only | TEXTURE debt — "the world feels real" (PLAN §6, the 95% safe/ordinary floor) |

**New infra for the whole layer:** only `npc_dialogue_state` (+ `npc_quests`) tables, following the
`0004_oracle.sql` RLS/service-role convention. The human teacher layer makes discover-by-punishment fair:
a player can learn a way from a PERSON (Wenna's mutter, Brann's logs) OR from their own doused torch.
**Deferral:** the NPC layer is "framework chosen, not built" (Phase 2) — the slice ships without it; the
surface richness is materially thinner until it lands.

### 3.5 THE RUMOR→VERIFY RECOVERY ARCHIVE

The Outer-Wilds ship-log ported to Discord verbatim — progress = the **5 colored threads filling in**, not
a step counter (the "reconstruction, not gauntlet" skeleton, `C6`).

| | MINECRAFT | DISCORD |
|---|---|---|
| **Card** | every find is anchored to a real site (a book, a tableau, a stone) | a thread-tagged card (WHO/amber, PLACE/green, HAPPENED/red, SURFACE/grey, HUMAN/black) with a per-thread completeness meter |
| **Rumor → verify** | an NPC bark / distant silhouette / `SignWrite` notice names a place you haven't reached | a grey rumor card with a dotted line to a still-locked silhouette; arriving FLIPS it to verified or **contradicted** (the contradiction is the emotional beat, `C4`/`C5`) |
| **Conditional reveal** | a re-read book gains meaning once related fragments are logged | a card's alt-text expands once `references_card_key[]` are found (`C7`) |
| **Dead-end** | a true answer the world acknowledges but that opens nothing | `oracleDeadEnd` line drawn publicly so no one wastes a second evening on a true-but-not-a-door |
| **Archivist decay** | — | the present-day archivist's annotations get shorter then *wrong* over the run (a global cadence thread under ALL side-quests — even pure breadth accrues dread, `B6`) |

### 3.6 THE REACTIVE / "IT-KNOWS-ME" LAYER

Grounded in REAL dossier data, used **sparingly** (one perfectly-timed beat beats ten cheap ones, `G3`).
Rides the dossier the signal listeners already build — no new measurement.

| | MINECRAFT | DISCORD |
|---|---|---|
| **It-knows-me** | a `Sign`/`Book` stating the player's real death count; a `NamedMob` clustered at the time-of-day they always play (per-player fog/time via packets) | a Watcher DM referencing a real thing they did an hour ago (rate-limited HARD) |
| **NPCs remember** | Citizens2 dialogue branches on the dossier (a grave-looter greeted differently from a grave-keeper) | `npc_dialogue_state`; the Iss tree flips warm→cold on `flags.iss_caught` |
| **Stage the conditions, never fake the beat** | pre-stage a FAWE reveal / named-mob / per-player whisper so a real player action fires them on camera (`G8`) | the dashboard holds a peak until everyone's together + mics hot |

---

## 4. THE FULL-STACK WIRING

```
                  ┌──────────────────────── SUPABASE (the single shared state) ────────────────────────┐
                  │ players · dossiers · custom_compliance · puzzles · solves · answer_attempts          │
                  │ beat_queue · arc_state(flags,act) · whisper_budgets · hints · event_log · state      │
                  │ + NEW: threads · thread_cards · side_quests · punishment_state · npc_dialogue_state   │
                  └───▲───────────────▲───────────────────▲────────────────────▲──────────────▲─────────┘
        ┌─────────────┴──┐   ┌─────────┴────────┐   ┌───────┴────────┐   ┌───────┴──────┐   ┌────┴──────────┐
        │  PAPER PLUGIN   │   │ ORACLE (shared)  │   │  SHOWRUNNER    │   │  DASHBOARD   │   │  DISCORD BOT   │
        │ signals+customs │──►│ resolveAnswer()  │◄──┤ snapshot→decide│   │ AUTO⇄CONFIRM │   │ the Watcher    │
        │ ~27 beat palette│◄──┤ one normalizer   │◄──┤ →apply · customs│◄──┤ stage/health │   │ Recovery Arch. │
        │ oracle/FAWE     │   │ both surfaces    │   │ + Attention(NEW)│   │ Accepting    │   │ /answer/whisper│
        └────────┬────────┘   └──────────────────┘   └────────┬───────┘   └──────────────┘   └────┬───────────┘
                 └──────── beat_queue (UnlockBeat → payload.step) ─┴──► plugin enacts in-world ◄────┘
```

| Component | Role in the integrated whole | Status |
|---|---|---|
| **Paper plugin** (`plugin/`) | 7 signal listeners build the per-player dossier; detect the 7 customs; the ~27-beat palette; in-world oracle (answer-sign); FAWE paste + reveal discipline | **BUILT** (content-starved) |
| **FastAsyncWorldEdit** | paste curated `.schem` set-pieces out of sight (`SmallStructureBeat` FAWE branch; isolated, skips if absent) | **wired — needs the `.schem` corpus** (deepest hole) |
| **Citizens2 / ZNPCsPlus** | surface NPCs + dialogue trees on the dossier; per-player keeper apparitions | **Phase 2** (framework chosen, greenfield) |
| **MythicMobs + ModelEngine** | the rare 3D apparition, glimpsed in fog only | **Phase 2.5** — **BUG:** `NamedMobBeat` falls to ZOMBIE, not WARDEN (E1; honor a `fallback_entity`) |
| **Resource pack** | rune font (carves every cipher output), whispers/knocks/heartbeat, poster/redacted textures, fog/desaturation for the deep | **spec'd — needs assets** (slice runs on vanilla sound fallbacks) |
| **Simple Voice Chat** | late-arc proximity dread (optional client install) | **Phase 3** |
| **Discord bot** (`discord/`) | the Watcher persona (`voice.ts`, the SOLE text source); the Recovery Archive (5 threads, rumor→verify); `/answer` `/whisper` clue cards | **BUILT** (extend with thread cards) |
| **Showrunner** (cron) | deterministic conductor: drip (cadence-gated, story-ordered) + stall-gift + customs pass (observe→warn→left + soft toll); zero-LLM | **BUILT** — **NEW: the Attention/creep selection pass** |
| **Supabase** | the single shared state; `arc_state.flags` are the gate variables | **live** (23 puzzles seeded) — **add 5 tables + 2 columns** |
| **Dashboard** (Vercel) | director's console: AUTO⇄CONFIRM, stage beats, hold peaks for camera, Accepting trigger, watcher-sleep kill-switch | **BUILT** |

---

## 5. DATA-MODEL ADDITIONS (Supabase/design only — NOT plugin/forge edits)

> All additive: new tables + a few columns, one migration after `0004_oracle.sql`, following the
> service-role-only RLS pattern. None touch `plugin/`, `discord/src/forge/`, or `discord/src/showrunner/`
> *code*; the bot/showrunner READ them through the existing repo layer when the concurrent session wires.

- **`threads`** — `(thread_key, label, color, total_fragments)`. Seed the 5 colors.
- **`thread_cards`** — `(card_key, thread_key, title, body_voice_key, anchor_site_id, rumor_or_explore,
  references_card_key[], revealed_by_solve, alt_text_condition)`. The ship-log (rumor lines, conditional
  reveals, clusters).
- **`side_quests`** — `(quest_key, thread_key, entry_puzzle_key, reward, gates_progress=false)`. The
  Seventh, haunted-herd, unspoken-refrain, the false-way-up, surface side-tracks.
- **`punishment_state`** — `(player_id, custom_key, transgression_count, last_toll_at, toll_tier,
  deciphered bool, teaching_site_id)`. The spine of discover-by-punishment: the customs pass escalates the
  soft toll until `deciphered=true`.
- **`npc_dialogue_state`** (+ `npc_quests`) — per-player conversation/fetch state on the dossier; gates the
  Iss tree warm→cold on `flags.iss_caught`.
- **Column adds:** `puzzles.thread_key` (tag each node to a thread); `puzzles.teaches_custom` (links the
  cipher ↔ custom layers).
- **`sites.yml`** (config, not a table): fill the unplaced anchors — `unbroken_light`, six named keeper-
  stones, the Stone of Reckoning, the two Rosettas, the two shrines, the cold-hearth — and add `schematic:`
  names for the new `.schem` files. **Flag every coordinate row `active=false` until the Stone of Reckoning
  is placed.**

**Authoring-ledger discipline (the consistency machine):** every authored fragment is tagged to (a) one of
the 5 threads and (b) ONE specific WORLD-BIBLE §7 revelation-debt — **if it pays no debt, it is cut** (the
exception: untethered oddities pay a TEXTURE debt). At 3–5× content volume this ledger must become a real
artifact (a CSV/seed-adjacent table), not prose, or it WILL drift.

---

## 6. RECONCILE / CONSISTENCY PASS

Confirmed consistent with the WORLD-BIBLE, the engine, and the concurrent plugin/forge/showrunner work — with these flags.

**Consistent (no contradiction):**
- **Ciphers as spine, not replacement.** The 11 forge ciphers + 23-node web are kept verbatim and
  re-skinned; no resolver change, no new beat type, all inside the existing schema. ✔
- **The induction twist + collective judgment + seven ways** map 1:1 to the code and are untouched. ✔
- **Discover-by-punishment** reuses the already-built customs→report bridge + the soft reversible toll
  (INV-8: tolls take warmth, return it); the bridge is custom-agnostic, so new ways need zero bridge
  change. ✔
- **Creep restraint** is enforced in code (reveal discipline, per-player default, warmth-not-progress
  tolls) — the catalog can be large because firing is gated by movement/budget/fallow. ✔
- **One voice, one state, one resolver** holds; surface NPCs in the human register are the *correct
  inverse* of INV-1 (people, not the presence). ✔

**Conflicts / cautions to honor (do not let a builder re-break these):**
- **DELIVERY:** this file now supersedes `INTEGRATION-SEAM6.md`; that file should be deleted or stubbed to
  point here so two integration docs don't drift.
- **X1 BIND is the precondition for authoring carved content** — already built (`forge/clue-specs.ts`);
  every carved stone must be authored *through it* so the rune carving and the Discord card render ONE
  plaintext (else INV-1/INV-3 drift).
- **DEEPEST HOLE:** zero `.schem` files, no `schematics/` dir; `sites.yml` is missing `unbroken_light` +
  the six keeper-stones + the Stone of Reckoning + the two Rosettas. Every gate paste-resolves to
  schematic-missing today — the spine is authored, the WORLD it carves into is not. **This is the single
  biggest pre-build task.**
- **INERT CUSTOMS:** the Unspoken ships `forbidden-words: []` (can never fire); `unspoken-refrain`,
  `haunted-herd`, and the Dark-Hours perform-at-time row were never seeded; Ward/Covering are taught but
  have no mechanic — demote them and promote the two real customs you seed.
- **MYTHICMOBS FALLBACK:** `NamedMobBeat` hard-falls to ZOMBIE (a short green zombie), not the WARDEN
  silhouette the stand-and-stare read depends on. Until a `fallback_entity`/`ModeledMobBeat` lands, author
  B1/B2 with a plain vanilla entity (`WARDEN`/`STRAY`), not a MythicMobs id. *(Plugin-owned — flag for the
  concurrent session.)*
- **DOC DRIFT (the Liar):** the shipped seed is already PLAYER-DRIVEN (`no-wall-catch` sets `iss_caught`);
  three design docs + the sealed JSON still describe the retired "showrunner flips it offstage" model.
  Reconcile them, or a builder re-introduces the hollow mechanism the seed removed.
- **BLACK MOON terminology:** WORLD-BIBLE says *black moon*; `DarkHoursListener` keys on a full-moon phase.
  Reconcile (gloss the bible or change the phase) before wiring any dark-hours creep/clue, or dread fires
  on the wrong night.
- **SENTINELS:** `accepting-crouch`/`record-receives` carry illustrative sentinel answers — swap in real
  long opaque plugin-posted tokens (kept out of any corpus) or the detected rites are spoofable.
- **WHISPER BUDGET vs AUTO-GIFT** is a tuning dial, not a solved value — needs live playtest calibration
  (too generous → farm hints + speed-run; too stingy → wall).

---

## 7. BUILD-READINESS CHECKLIST (vertical slice: surface Mouth + first descent + Movements I–II)

> Each step ships STORY + CLUE + INTERACTION together (the authoring contract). Owner: **me** = design/
> content (this session); **Ethan-session** = plugin/forge/showrunner code; **shared** = both.

1. **(Ethan-session, DONE)** X1 forge-spec bind, customs→report bridge, drip-carries-a-card, drip
   pool/order filter — verified closed; build AROUND them.
2. **(me)** Place the spine in `sites.yml`: `unbroken_light`, six named keeper-stones, the **Stone of
   Reckoning** (built 2nd, after the rune Rosetta), the two Rosettas, the cold-hearth/shore/threshold
   anchors. Flag every coordinate row `active=false` until the Reckoning is placed.
3. **(me)** Author the first `.schem`/inline corpus: the offering cairn (inline block list), the first
   keeper-stone + rune-letter Rosetta sign, the Mouth, the doused alcove, the ruined seventh-shrine.
4. **(me)** Author carved content + cipher params per stone *through the X1 bind* (Vaun's shift via D02's
   "three of each → wheel"; Mara's six page/line/word triples against the real shelf; Iss's Vigenère
   ciphertext) so `accepted_answers == decode(carved ciphertext)`.
5. **(me)** Seed the 5-thread Recovery Archive (`threads` + `thread_cards`); tag every Movement-I/II
   puzzle + doc with `thread_key`.
6. **(me)** Promote the 5 orphan ciphers onto placed nodes with Rosettas (railFence→`stone-brann`,
   polybius→Deep-Market grid, a1z26/morse→spawn rungs, columnar→late door) — variety + un-orphan in one.
7. **(shared)** Seed the inert customs + the punishment loop: author `forbidden-words`; seed
   `unspoken-refrain` + `haunted-herd` + the Dark-Hours row; add `punishment_state`; demote Ward/Covering.
8. **(Ethan-session)** Build the **creepy-beat selection pass** (the one new showrunner policy: movement +
   per-player Attention + drama-budget → one tier-gated beat) and the Attention accumulator in
   `PlayerSignals`; add the per-custom `CUSTOM_PHRASES` + `reportKept` boon line in `voice.ts`.
9. **(Ethan-session)** Fix the `NamedMobBeat` fallback entity (honor `fallback_entity` or `ModeledMobBeat`)
   so the stand-and-stare Watcher reads as a WARDEN, not a zombie.
10. **(me)** Reconcile the docs: Liar player-driven model; black-moon↔full-moon terminology; swap the
    placeholder sentinels for real opaque tokens; delete/stub `INTEGRATION-SEAM6.md`.
11. **(shared)** Author the Movement-I–II breadth: 2–3 surface hamlets, a handful of rumor→verify pairs,
    the false-way-up digression, the AMBIENT side-tracks (Sella's drawings, a recipe book).
12. **(shared)** Local Paper runtime test + a pacing pass (verify Movement I reads as normal Minecraft with
    one well-placed beat), then go live with the dashboard conducting.

**We are ready to build when** the spine is sited in `sites.yml` and its first `.schem` corpus exists
(steps 2–4) — because every gate, cipher, and creep beat in this plan paste-resolves to a real, placed
place the moment the world it carves into is authored; everything else is already built or is additive
schema on top of it.
