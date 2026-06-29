# IDEA — Vanilla Progression as Canon (the Nether = the deep fire-source; the End = exile / the Seventh's absence)

> **Build-ready design treatment. Spoiler-bearing** (reads `arc/`-sealed material — `canon-spine.md`,
> the Seventh corpus). For synthesis / Lore / Build, NEVER for Ethan-unspoiled.
>
> **STATUS: KEEP-SCALED, CRITIQUE-RESOLVED (2026-06-28).** A prior draft of this file authored the two
> lanes; the lens pass `design/critiques/frame-progression.md` then raised ten severity-ranked defects
> (S1–S10) and six must-resolve seams. **This revision folds every S-item resolution into the design as
> a decision, not a hand-wave** — each is marked `[S#-RESOLVED]` at the point it bites. The namespace
> owner has since assigned this idea its anchors in `WEB-MASTER §0.4` (`nether_forge`,
> `end_seventh_shrine`, `end_exile_hold`, `observance_nether`, `observance_end`, `seventh_seen_out`) —
> so the anchors below are **ratified canon, not self-minted**.
>
> Grounded against, and obeying: `canon-spine.md §2` (the timeline — dug down, the abandonment),
> `§3` (FACT 10/10b/11/12/14/15), `§5` (the Seventh thread + `seventh_choice`), `§6` (the ten prose
> rules), `§7` (INV-11/12/14/16/18/19/20), `§8.4` (the four fates + two codicils); `WEB-MASTER.md §0.4`
> (the ratified anchors), `§0.5` (the producer boundary), `§1` (the M-spine), `§2.1` (the
> `requires_flags` activation lane), `§5` (the M5 composer); `design/structures.md` (the GO-LIVE build);
> `plugin/.../signal/LocationSampler.java` + `HeatmapAccumulator.java` (both **world-name-keyed today**
> — dimension-aware tracking needs zero new infra, see §1.0); `plugin/.../sites.yml` (per-site `world:`
> field exists, line 18 `default-world` + line 31 `world:`); `discord/src/showrunner/fate.ts`
> (`decideFate(inp: FateInput)` — the frozen selector, S1); `discord/src/voice.ts`
> (`fateKept/fateCastOut/fateRefusers/fateInheritorsCodicil` exist; no nether/end keys yet);
> `discord/supabase/seeds/seventh_seed.sql` (breadth-row shape `(quest_key, thread_key,
> entry_puzzle_key, reward, tier, est_minutes)`, `gates_progress` defaults false).
>
> **One-line pitch.** The Nether and the End are not "spooky dimension content" — they are the **two
> ends of the Hold's cosmology made walkable**: the Nether is the deep fire-source the founders dug
> *toward* (FACT 11's furnace, the keepers' ruined works, a prior keeper's grave that embodies "you are
> becoming them"); the End is the one place *outside the record* — exile, the Seventh's home, and the
> literal far side of the `cast_out` fate. Both are **OPTIONAL DEEPENING LANES** that amplify and
> reward; the whole arc completes in the Overworld and neither is ever a hard gate (INV-12, INV-19).

---

## 0. WHAT EXISTS TODAY (re-grounded — read before building)

> The honest ground truth on disk, so nothing below is invented as if built.

**Already canon / built that this idea RIDES (do not re-author):**

- **The cosmology is already "dug downward."** `canon-spine §2` (*the taking-hold*): the people dug a
  colony and the Hold is *found by descending* (`structures.md` — "the Hold is found by descending… deeper
  = older = more wrong-scaled"). The Undercroft (`unbroken_light`) is the bottom, the climax site. **This
  is the single most important constraint** — the Nether lane must extend it, never plant a *second*
  bottom (see §2 R1 / `[S3-RESOLVED]`).
- **FACT 11 is "one fire that never went out."** `canon-spine §3` + the Undercroft eternal kept light. The
  Nether is its *source-of-source* reading: the deep the kept fire was borrowed from.
- **The Seventh spine is built/seeded.** `the_unwriting` sited (`sites.yml`), `seventh-shrine` /
  `seventh-unwriting` / `seventh-cause` / `seventh-choice` rows seeded (`puzzles_seed.sql`), breadth rows
  `dest-unwriting-deep` / `dest-fire-let-out` (`seventh_seed.sql`), FACT 10b canon (`§3b`), D11
  (`the-seventh-not-kept.md`) + the cause-fragment (`the-fire-they-let-out.md`) authored. The Seventh was
  cast out **before the threshold**, "*unwrote the looking. no stone. no name said back.*" — an
  **absence**. That absence is what the End lane gives a place.
- **The four fates are canon + the selector is built.** `canon-spine §8.4` + `divergent-fates-endings.md`:
  `kept | cast_out | divided | refusers`, `decideFate(inp: FateInput): FateDecision` BUILT in
  `discord/src/showrunner/fate.ts`, active-only, enum, no names (INV-11). The `FateInput` interface today
  carries `seventhFound`, `issCaught`, `quorumMet`, `refusalSignal`, the honored/violated counts — **and
  no dimension field** (the S1 truth: adding one is a frozen-interface edit synthesis must ratify).
  `cast_out` already = "markers face away, abandonment passive voice."
- **The engine is multi-dimension and dimension-aware.** `backlog-undercroft-dimension.md` ships the
  Multiverse Undercroft pattern; **`LocationSampler` keys the heatmap on `worldName` and computes cohesion
  same-world-only** (it reads `loc.getWorld().getName()`, bumps `tracker.heatmap().bump(worldName,…)`, and
  skips other-world players in the nearest-distance loop); `HeatmapAccumulator.CellKey` includes a `world`
  field; `sites.yml` has a per-site `world:`. So per-dimension presence and per-dimension site placement
  are **already feasible with zero new tracking infra** (§1.0, §4).
- **INV-14 (the minus-sign law):** a coordinate's decoded value is a navigation pointer ONLY; the answer is
  a clean `[a-z0-9]` destination word found on-site. The Nether pocket (§1.1) obeys this verbatim.

**The real gaps this idea fills (the build, §6):**

1. **No diegetic reframe of the two dimensions.** Vanilla Nether/End are currently "off-canon" — a player
   who goes there finds Minecraft-default content that *contradicts* the Hold's grief. This idea makes them
   canon so they cannot read as off-theme.
2. **`cast_out` has no PLACE.** The fate is a sentence + Overworld floor dressing; exile has nowhere to
   *be*. The End lane gives the most divergent reading a literal geography (the P2 end-city, §1.5).
3. **FACT 11's source is asserted, never shown.** "One fire never went out" — but *from what*? The Nether
   shows the furnace the kept fire was always borrowed from (without ever explaining it).
4. **"You are becoming them" (FACT 15 from the keepers' side) has no embodied keeper.** The Nether pocket's
   keeper-remains + journal (§3) make the induction felt in a place, never stated.

---

## 1. EXPOUND — the full mechanic + story + mystery, beat-by-beat

### 1.0 The core mechanic: optional lanes, dimension-aware, never a gate

Both lanes are **OPTIONAL DEEPENING**. The Overworld Hold carries the entire spine (the six stones, the
Liar catch, the coop gate, the Undercroft descent, the Accepting). Neither dimension is in any
`requires_flags` chain (`WEB-MASTER §2.1`); neither is in the Accepting quorum (INV-19); a group that never
lights a portal completes a whole, un-shaded arc. What the lanes buy is **amplification + reward + the two
hardest-to-show truths given a body**: the source of the kept fire (Nether) and the shape of exile (End).

The one new measurement is **dimension presence**, and it already exists: `LocationSampler` writes
`worldName` into every heatmap bump and location sample. So:
- "the group carried the kept fire to its source" = a measured presence at a Nether-sited anchor (a
  `Site.contains` proximity check, the same path the Bow/cairn use) **plus** an on-site word submission
  (INV-14). Both are real measurements; neither is an inference. **Precision-safe.**
- "the group went out to the place outside the record" = a measured presence at an End-sited shrine anchor.
  And — by canon — the End is the **one dimension the engine forgets**: it records the *fact of entry* once
  (the `seventh_seen_out` flag, set on the on-site read), then writes nothing further while they are there.
  Absence-as-design, on-theme (the record does not reach the End).

Neither lane needs a new tracker class, a new event subscription, or a `PlayerMoveEvent` (forbidden,
DESIGN §2.1). Both reuse `Site.contains` + the existing Multiverse pattern + `AnswerSignListener`.

### 1.1 THE NETHER LANE — the deep fire-source (intimate apparition work; the furnace)

**The reframe (no orphan "spooky nether"):** the Nether is **the source the Undercroft's one fire was
borrowed from** — "*below the below.*" `[S3-RESOLVED]` It is **not a second bottom.** The Undercroft is and
remains the bottom of the Hold (`structures.md`, `canon-spine`); the Nether is the *furnace the deep's
warmth comes from*, the place the first keepers carried a coal up out of. The keepers did not invent the
Kept Light; they carried up an ember of the deep fire and never let it go out, because to let it die was to
lose the only thing the Hold had against the cold. **Build gate (S3): before any Nether build, LORE seals
ONE sentence to `canon-spine` FACT 11 / the timeline** — *"the kept fire was carried up from below the
bottom; the Undercroft is the bottom of the Hold, the deep-fire its source — one direction, not two."*
Until that sentence is sealed, the Nether journals (§3) say "below the below," never "the real bottom," and
the lane does not ship. This deepens FACT 11 without touching the induction twist.

**Diegetic block-reads (the Nether's own vanilla blocks become the lore — nothing built where vanilla
suffices):**
- **soul sand / soul fire = the not-kept of DEEP TIME.** `[S5-RESOLVED]` The souls of *prior failed
  communities* — older than the first keeper, the groups that came down before the Kept and did not keep
  the ways. **They are not the Pale herd.** The Pale (`herd-conversion`, `WEB-MASTER §7`) is *this* group's
  in-progress, present-tense conversion of the living; the soul sand is *deep-time, before-the-Kept* failed
  communities. The §3(d) line and the journal anchor this explicitly ("*older than the first keeper*") so
  the two FACT-12 visuals never read as the same population. The blue fire is the Kept Light's opposite: a
  fire that gives no warmth, the light of the un-kept.
- **bastion remnants + fortresses = the keepers' ruined works.** Not piglin architecture in-fiction — the
  *founders' deepest delvings*, the works of the first keepers who went furthest toward the fire and did
  not come back human-scaled (the deepest form of "deeper = more wrong-scaled," `structures.md`). A fortress
  is "*a thing built by hands that had stopped being quite hands*" — geography implying induction without a
  word.
- **the eternal Nether ambient (no day, no rest, the fire always lit) = FACT 11 at the source.** The
  Overworld Undercroft holds *one* kept fire in the dark; the Nether is the dark turned inside out — a whole
  world that is *all* the kept fire and no kept dark. A sharp group reads: the kept light upstairs was a
  single ember carried up from *this*.

**Why the Nether is the RIGHT place for intimate beats:** the Nether **occludes** — netherrack walls,
lava-light, fog, tight ravines. Reveal discipline (`mutateWhenUnwitnessed`) fires easily where sightlines
are short. So the Nether carries the **silent-watcher beats** (P2): the half-glimpsed keeper-shape down a
basalt corridor that ends in wall, the soul-fire banked when you looked away. **All borrow the Overworld
apparition vocabulary re-skinned to basalt/soul-fire and defer to the single-arbiter conductor (INV-18) —
the Nether gets NO apparition lane of its own** (`WEB-MASTER §7`, the "re-skins of a shared vocabulary"
rule). This keeps the world's appearings rare so each lands.

**The beat-by-beat Nether expedition — the P1 core is a NEAR POCKET, not a third decoded trek.**
`[S4-RESOLVED]` The spine already has two `coords-to-real-place` walks (the false dead-shrine walk M2; the
true walk M4). A *third* full decoded coordinate-walk is exactly the multiplication `coords-to-real-place
§2.G` forbids ("do not multiply walks"). **The walk-count budget is fixed: 2 ground walks + at most 1
short vertical pocket.** So the Nether P1 is a **short ruined delve just past the portal** — no long
bearing-trek (that is P2, gated on a playtest that the M2/M4 walks didn't already exhaust the group).

1. **The seed (M2, inert).** Brann's stone (night-gated) carries, in its carved **framing** (never a bound
   plaintext — it does not touch the X1 round-trip guard), a single line that reads as a watchman's raving:
   "*the fire we keep is not ours. it is lent. brann will not say from where, in the dark, but he will say
   it is below the below.*" Inert texture; a riddle with no door yet. (Brann's doubling/over-corrected
   fingerprint, `WEB-MASTER §6`.)
2. **The bearing (M3, optional).** A keeper journal-page (D-NETHER-1, §3) found at the Undercroft *after*
   the descent (`requires_flags: [undercroft_open]`) gives the pocket's direction in the Hold's own terms:
   "*carry a coal of the kept fire through the burned door, and walk the short way to where the fire is kept
   for everyone.*" The "burned door" is a Nether portal; **lighting one becomes the rite of carrying the
   kept fire down to its source** (the vanilla act made diegetic, never named "go to the Nether," §2 R6).
   The pocket anchor is a short, fixed distance from the portal — close enough that it is a *delve*, not a
   trek. (`[S4-RESOLVED]`: no decoded signed coordinate is the answer; INV-14 — the on-site word answers.)
3. **The delve (M3→M4).** Through the portal, a short way to a **small ruined room** (re-dressed minimally
   — see `[S7-RESOLVED]` for the producer rule). The Nether's native hostility *is* the tax (a danger a pool
   cannot trivialize), but the engine adds **no hostile mob and tolls no items** (`[S5/R5]`: tolls take
   warmth, not the players' stuff — DESIGN §3, rule §6.10). The lore makes preparation diegetic ("*the deep
   does not want company that has not kept the ways*").
4. **The arrival (the embodiment — "you are becoming them").** In the room: **a prior keeper's remains** (an
   armor-stand or player-head on a deepslate slab — placed at world-build, not pasted toward an approaching
   player), a doused soul-lantern, and **the keeper journal** (D-NETHER-2, the §3 exemplar). This keeper
   went down to keep the fire when the Hold above was already going dark, and was kept here — the deepest,
   most final keeping. The journal's last legible lines are the induction felt from the inside (§3). The
   answer-word (INV-14) is cut on the slab: a clean `[a-z]` word found only on-site (the keeper's own name,
   or "*lent*").
5. **The reward + the connection to the Seventh spine (§1.3).** Submitting the on-site word:
   - earns **Whisper budget** — `[S10-RESOLVED]` *bonus economy, additive, never part of the front-loaded
     F4 backstop ladder*; a group that skips the Nether is never short on hints;
   - sets **`nether_forge_found`** — the Nether lane's *proposed* positive `kept`-leaning signal (§1.4; the
     S1 ratification);
   - reveals that the Kept-Light custom's **origin** is here: the first keepers carried a coal up from the
     source, and "*the keeping was always the carrying. you do not make the fire. you do not own it. you
     carry it, and you do not let it die, and that is the whole of it.*" This re-reads the most-tracked
     custom (Kept-Light) as a *carrying*, not a *possession* — quietly setting up the induction (you, too,
     will be carried, not owned).

### 1.2 THE END LANE — exile / absence / the Seventh's home (big SET-PIECES, never silent-watcher)

**The reframe:** the End is **the one place outside the record.** Everywhere else, the record files you
(FACT 1 by name, FACT 16 by place, FACT 17 by word). The End is the single place the record *does not
reach* — no kept fire, no markers, no Archivist, no count. It is where the cast-out go: not down toward the
warmth (that is keeping), but *out*, into the absence. D11: the Seventh was cast out "*before the
threshold… the land looked, and the land did not keep them, and then it unwrote the looking.*" The End is
that unwriting given a sky.

**Why the End is the RIGHT place for SET-PIECES, not silent-watcher beats:** the End is *wide open* — long
sightlines, no occlusion, a void floor. Reveal discipline is **hard** there; an apparition would be seen
winking in across open air and read as a bug. So the End gets **ZERO ambient apparition lane** — a
*positive* canon choice, not a limitation (the End is "outside the record"; an apparition there would
contradict it). `[S3/R3-RESOLVED]` What the End's vistas are good at is the **big, static,
discovered-never-witnessed set-piece**.

**`[S6-RESOLVED]` The reveal-timing hazard is real and is resolved by which structure we use.** A player
gliding to a fresh outer island is the FIRST loader of that chunk — there is no "unwitnessed window after
generation, before approach." So:
- **P1 End core = re-dress an EXISTING, already-generated structure** (a vanilla **end-city / end-ship**),
  whose chunks can be force-loaded, mutated on an unwitnessed relog, then unloaded — genuinely reveal-safe.
- **Any bespoke outer-island shrine is pre-generated at world-build** (force-load + paste + unload *before
  the server opens*), never lazily pasted toward an approaching player. If world-build pre-generation is not
  done, the bespoke island is cut and the re-dressed end-ship carries the lane.

**`[S7-RESOLVED]` Producer boundary (the `WEB-MASTER §0.5` rule):** re-dressing an *occupied* vanilla
structure is NOT a clear-footprint `SmallStructureBeat` paste (that is impossible against `footprintClear`,
and `RoomSwapBeat`'s marker-idempotent overwrite is for the Undercroft swap only). So End/Nether re-dressing
is **additive pastes onto verified-clear adjacent footprint** — soul-lanterns, lecterns, a deepslate
carving-slab placed *on air* next to the vanilla blocks, via `SmallStructureBeat` on a clear sub-region (or
`RevealBeat` block-state flips). No overwrite of occupied vanilla blocks in either dimension. Each set-piece
is bound to the `world_paste_ledger` single-paste owner (`WEB-MASTER §0.5`), keyed `(world, site_id,
schematic, base)`.

**The two End set-pieces:**

1. **The Seventh shrine (the Seventh's home).** Carried by a re-dressed **end-ship** (P1, reveal-safe) — or
   a pre-generated outer-island shrine if world-build does it (P1-alt). Built to the Seventh's unfinished,
   wrong-scaled hand — *unfinished*, not *too-tall* (the `the_unwriting` signature). The one place in the
   cosmos where the Seventh *was* kept — by their own hand, in exile. It carries the **End shrine carving**
   (§3 exemplar): the Seventh's own account of being unwritten, written *outside* the record so the record
   could not scrape it. The Seventh's voice from the one place the unwriting could not reach.
2. **The exile-hold = the `cast_out` set-piece (P2, the divergent ending made a place).** A re-dressed
   end-city, additively dressed as a **keepers' exile-hold** — the place every cast-out keeper went, the
   markers all facing *away* (the `cast_out` dressing, `canon-spine §8.4`, made literal and vast). See §1.5
   for the INV-16-safe binding.

**The beat-by-beat End lane:**

1. **The seed (M3, inert/ambiguous).** D11 already plants it: "*cast out… before the threshold… no stone,
   no name said back*" and "*to be kept and to be cast out are one door, looked at from either side.*" The
   End is the *other side of that door*. **No new M3 plant needed — D11 is the plant; this lane is its
   spatial payoff.**
2. **The way out (M4, post-`seventh_named`).** `[S9-RESOLVED]` Two distinct artifacts, named as such:
   - **the pointer (a reveal on the existing surface, NO new row):** the `the_unwriting` chamber-2 wall
     gains **one extra effaced line**, legible only once `seventh_named` is set — "*they did not go down.
     the deep is keeping. they went out. there is a door that is not a threshold — the land left it open by
     forgetting it. you will not be kept there; the record is not there to keep you.*" Revealed by the
     existing `seventh-unwriting` solve; it points at the vanilla End portal / stronghold *diegetically*
     (going out to where the unwritten go, not a Minecraft milestone, §2 R6).
   - **the payoff (a NEW lore row):** arriving at `end_seventh_shrine` and answering its on-site read is the
     **new breadth/answer row `end-seventh-out`** (outcome `lore`, `requires_flags: [seventh_named]`), which
     sets `seventh_seen_out`. Pointer = a surface reveal; payoff = a row. Two artifacts, mirroring the
     Nether's bearing-page (pointer) + `nether-forge` (payoff) split.
3. **The Seventh shrine (the payoff).** Reading the carving sets **`seventh_seen_out`** — a *group-scoped*
   flag (it is NOT a fate input, S2) that does two things: it is a *second in-road* to the Seventh's
   restore/erase context (a group that walked the End learns *why* the Seventh chose exile, deepening the
   `seventh_choice` they perform back at `the_unwriting`); and it licenses the End's role in `cast_out`
   legibility (§1.5).
4. **The Cast-Out divergent ending (§1.5).** If the group's measured fate is `cast_out`, the End is where
   that fate *lands* as a place — the literal exile (P2, INV-16-bound).

### 1.3 How the two lanes connect to the Seventh spine

The Nether and the End are the **two halves of the Seventh's question**, made walkable:

- **The Nether answers "what is the keeping?"** — it is a *carrying* of the deep fire (§1.1.5). To be kept
  is to be carried down and held; the soul sand is the worst keeping (held forever, the wrong way).
- **The End answers "what is the not-keeping?"** — it is *exile*, the door's other side, the place outside
  the record (§1.2). The Seventh was not kept and not destroyed; they were *cast out* — sent here.
- **Together they frame `seventh_choice` (restore/erase).** A group that has walked the Nether (the keeping
  is a carrying) and the End (the not-keeping is exile, with the Seventh's own account out there) performs
  the `the_unwriting` restore/erase choice with *both* readings in hand. `restore` = carry the Seventh's
  name back into the record (give them the keeping they were refused). `erase` = leave them in the absence,
  complete the cast-out. **The lanes do not change the mechanic** (the choice is still the seeded
  `seventh-choice` row at `the_unwriting`, INV-12 colors-never-gates) — they **deepen the meaning** of a
  choice that already exists. No orphan: every lane beat re-reads an existing flag/choice.

### 1.4 The lanes' contribution to the divergence (precision-safe, never a gate — the S1/S2 split)

**`[S1-RESOLVED]` `nether_forge_found` is a PROPOSED new `FateInput` field, for synthesis to ratify — not
asserted as wired.** The as-built signature is `decideFate(inp: FateInput)` with a *frozen* input set
(`seventhFound`, `issCaught`, `quorumMet`, `refusalSignal`, honored/violated counts; bond/Whisper
**excluded**, INV-11). Adding `nether_forge_found` to the `kept` test is a **canon-surface edit to
`WEB-MASTER §5/§8` + the `FateInput` struct**, not a free "alongside." So this doc *proposes*:
> add `FateInput.netherForgeFound: boolean`; in the `kept` branch, treat it as a third positive in-road
> beside `seventhFound || issCaught` — i.e. `kept` may fire on `(seventhFound || issCaught ||
> netherForgeFound)`. **Self-test requirement: `kept` still fires fully WITHOUT it** (absence is neutral —
> a group that never went to the Nether is un-shaded, not punished, INV-12). It is **active-only,
> group-scoped, enum-feeding, names no player** (INV-11), and **can never cause `cast_out`** (its absence
> is neutral, never a violation). Until synthesis ratifies in `WEB-MASTER §5`, the row sets the flag and
> the M5 composer reads it for a tinted clause, but `decideFate` does not — the lane degrades to "colors
> the close" with no selector dependency.

**`[S2-RESOLVED]` `seventh_seen_out` is NOT a fate input.** It licenses the End's *legibility* of an
already-measured `cast_out`/`refusers` fate (§1.5); it never *produces* a fate. The fate is decided by the
honored/violated spread + `leftAtActive >= 2` (`cast_out`) or quorum+`refusalSignal` (`refusers`),
unchanged. `seventh_seen_out` only gates whether the End *place* reads back to a group that already earned
the fate. So it touches the M5 composer (one tinted clause) but **not** `decideFate`.

**The hard line (INV-16):** neither lane lets the group derive *which* player is on which side. Both flags
are group-scoped. The Nether keeper-remains names a *prior* keeper (never a living player). The End shrine
is the *Seventh's* (a prior keeper). No carve, apparition, or dressing in either dimension encodes a living
name (the End has no apparition lane at all; the Nether borrows the chorus-gated Overworld vocabulary).

### 1.5 The Cast-Out divergent ending made literal (the End set-piece binding — INV-16-bound, P2/cuttable)

`cast_out` is canon (`§8.4`): violation dominates + ≥2 left at the threshold → markers face away,
abandonment passive voice. Today it resolves as Overworld floor-dressing + an M5 Keeper clause. **The End
lane can give `cast_out` a place to *be*** without changing the selector or punishing anyone — **but this
is P2/cuttable, and it carries the S2 INV-16 teeth or it does not ship:**

- The Overworld Accepting fires identically; the `decideFate` enum is unchanged; the M5 ≤2-clause cap holds.
- **The persistent dressing for `cast_out` gains one optional far-surface:** the re-dressed end-city
  (§1.2.2) becomes *legible as the group's own exile-hold* — **only as a place they may walk to after, if
  they choose, and only if `seventh_seen_out`** (they have already been out and learned what exile is). It
  is never shown to them; it is a place that was always there, that now *reads* as theirs. Static,
  discovered, never witnessed mutating (reveal discipline).
- **`[S2-RESOLVED]` INV-16 teeth (explicit, non-negotiable):** the exile-hold **names no living player and
  encodes no per-player side; it reads the GROUP's fate only.** Its dressing rhymes on a *chorus* every
  active player shares — the §3(c) shrine line "*you are not cast out. you only came to look*" — never on
  the `LEFT_AT` set. **No dressing may spatially correspond to any per-player carve or to the `LEFT_AT`
  group.** If that cannot be guaranteed in the open End, the binding is **cut** and the End lane ships as
  the Seventh shrine only.
- **`refusers` (the secret fate) gets the sharpest End read.** `refusers` = quorum present, positive
  defiance, bow window empty (the group chose *not* to be kept — the Seventh's road on purpose). For a
  `refusers` group with `seventh_seen_out`, the outer shrine re-reads as **the model they followed**: the
  existing `voice.fateRefusers()` line "*you found the floor and did not bow. so did one before you*" — and
  "one before you" is the Seventh, whose shrine they literally stood in. The plant (the shrine) pays off the
  secret fate. (Still group-scoped; INV-16 holds — "the group did not bow," never "player X did not bow.")

---

## 2. CRITIQUE — adversarial and honest (the laws; what to cut/scale; mitigations)

> The ten lens-critique defects (S1–S10) are folded into §1 as `[S#-RESOLVED]` decisions. This section
> states the standing risks in design terms and confirms the resolutions hold.

**R1 — THE SHARPEST RISK: the Nether-as-fire-source contradicts the single-bottom cosmology.** `canon-spine
§2`/`structures.md`: the Hold is dug down, the Undercroft is the bottom. A careless build makes two
competing "deepest warm places." **Mitigation (`[S3-RESOLVED]`):** the Nether is the *furnace under the
bottom*, "below the below" — the source the Undercroft's one fire was carried up from. ONE direction,
deepened; the Undercroft stays the bottom. **Gated on LORE sealing one sentence to FACT 11 before build**
(or two authors later disagree on where "the deep" is). The journals say "below the below," never "the real
bottom."

**R2 — Orphaned "go to the dimension" gimmick.** **Mitigation:** neither lane is a *visit* — each is the
**body of an existing thread.** Nether = FACT 11's source + the Kept-Light custom's origin + "you are
becoming them" embodied. End = the Seventh's exile + the `cast_out`/`refusers` fates' place + D11's
"one door, two sides" spatialized. Every lane beat re-reads a canon flag or fact; a beat that can't be tied
to one is cut (none currently can't).

**R3 — Reveal discipline in the wide-open End.** **Mitigation (`[S3/S6-RESOLVED]`):** the End gets zero
ambient apparition lane (a canon choice); only static set-pieces, carried by re-dressing an *already-
generated* end-ship/end-city (reveal-safe on unwitnessed relog) or a *world-build pre-generated* island —
never a lazy paste toward an approaching player.

**R4 — Path A / never-punish-the-absent.** **Mitigation:** both lanes are OPTIONAL DEEPENING, never gates
(INV-12). Not in any `requires_flags` chain (`WEB-MASTER §2.1`) or the Accepting quorum (INV-19). A
portal-skipping group gets a complete, un-shaded arc. The lanes *reward* the obsessive, never *require*
anyone. (The lens pass confirmed this is airtight against `WEB-MASTER §2.1`.)

**R5 — The Nether is dangerous in a way that tolls progress (death = lost items).** **Mitigation
(`[S5/R5]`):** the danger is the vanilla Nether's own (the engine adds no mob, tolls no items — rule §6.10).
The P1 core is a **near pocket**, not a deep trek, so the hazard is a careful-prep tax, not a death-march.
If even vanilla danger is judged too much for this friend group, the pocket is sited adjacent to the portal
(seconds in), low-hazard.

**R6 — Metagaming: "the ARG is telling me to speedrun the game."** **Mitigation:** nothing names the
dimensions in plain words. The Nether is "*the burned door… below the below… where the fire is kept for
everyone*"; the End is "*a door that is not a threshold… where the unwritten go.*" The group decodes
*places in their own cosmology* and only realizes *after* that the place was the Nether/End. The vanilla act
(lighting a portal, finding a stronghold) becomes a *rite* (carrying the kept fire down; finding the door
the land forgot), never a milestone.

**R7 — Dead air: decode → walk → nothing there (unbuilt cell).** **Mitigation:** the
`siteCoverageSelfTest` extension (`coords-to-real-place §7.2`) — a coord/destination row must not seed OPEN
unless its `site_id` resolves to a **placed + enabled** site **in the named world**. The Nether/End anchors
carry explicit `world:` (§4); the self-test asserts the world exists + the site is placed before the row
activates. Until the worlds are built (GO-LIVE, the Undercroft pattern), the rows stay inactive and the
plugin silently skips the unplaced site — never dead air.

**R8 — Scope: two whole dimensions + set-pieces + journals + flags is a lot.** **Mitigation / the honest cut
(`[S4/S8-RESOLVED]`):**
- **P1 (cheap, high-value cores):** the **Nether near-pocket keeper-grave** (one room, additive dressing,
  one journal, one on-site word → `nether_forge_found` + bonus Whisper + Kept-Light-origin lore) and the
  **End Seventh shrine** (one re-dressed end-ship, one carving → `seventh_seen_out`). Each ~one set-piece +
  one journal/carving + one flag, riding built machinery.
- **P2 (depth, when the slice is green):** the full Nether bearing-**trek** (behind a playtest, R5/S4); the
  end-city `cast_out`/`refusers` **place** binding (INV-16-bound, S2); the Nether **intimate apparition**
  beats (basalt glimpse, soul-fire bank).
- **DO NOT build:** a *second* bespoke fog-dimension (the Undercroft is the one — vanilla Nether/End need
  only re-dressing, no datapack dimension); any End ambient apparition (R3); either lane as a gate (R4).
**Verdict: KEEP-SCALED.** Ship the two cheap P1 cores; defer the trek, the apparition garnish, and the
end-city binding to P2.

**R9 — Cross-surface truth.** **Mitigation (`[S8-RESOLVED]`):** the lanes feed **Discord (Whisper budget) +
the dashboard fate-preview + the M5 composer** via `arc_state` flags + thread-card flips — surfaces that
already read `arc_state`. **The Record website is NOT silently widened.** The website projection
(`record-projection.ts`) is a pinned, self-tested 3-field pure module; adding a `nether_forge_found` line is
a *projection-input change*. So the lanes **drop the website mention entirely** (cleanest); if a deep-fire
line is later wanted, it is filed as an explicit `record-projection.ts` 4th-field task that updates
`record-projection.selftest.ts`, never an implied "may add one line."

**R10 — Precision on the dimension-presence signal.** **Mitigation:** `nether_forge_found` fires only on the
**on-site answer-word submission at the placed Nether anchor** (INV-14) — a group must be measured present
in the Nether world AND submit the on-site word. Both real measurements; neither an inference. A wrong "you
kept the deepest way" is impossible (the word is only readable on-site).

---

## 3. DE-SLOP — exemplar artifacts, in-voice, cold/plain/concrete (proof the prose lands)

Run against the ANTI-AI-SLOP LAW. Lowercase keeper/record register; no named emotion, no
"testament/little did/not just X but Y," no three-adjective lists, no exclamation, no tidy bow; mundane
concrete nouns; the iceberg (omission) carries the weight. Keeper hands obey their fingerprints
(`WEB-MASTER §6`).

**(a) D-NETHER-1 — the bearing-page (found at the Undercroft; Mara's referential-deferred hand — she
cites, she does not go):**
> page eleven, third line, the founders' hand: *the fire is lent. carry the coal through the burned door
> and walk the short way.* i have read it forty times. i have not walked it. someone who keeps the light
> better than i kept it should carry it down to where it is kept for everyone. it is below the below. that
> is all the page says of how far.

**(b) D-NETHER-2 — the pocket journal (the keeper kept at the source; tonal decay shown structurally, the
hand failing as it goes down):**
> i came down to keep the fire because up there the lamps were going out and no one was left to carry them.
> the fire is here. it does not need keeping. it never needed me.
>
> the sand is the others. older than the first of us. the ones the deep kept the wrong way, long before we
> came down. you can stand on them. they do not mind.
>
> i kept the light. the light kept me. i do not think there is a door back up that i would still fit
> through.
>
> `[ the last legible line, the strokes wide, the hand nearly gone ]`
> i am not letting it go out. i am the part of it that does not go out now. that is the keeping. i did not
> know that was the keeping.

> `[S5-RESOLVED]` note: line 2 anchors the soul sand as **deep-time, before-the-Kept** — distinct from the
> present-tense Pale herd (`herd-conversion`). The two FACT-12 visuals never read as the same population.

**(c) D-END-1 — the End shrine carving (the Seventh's own hand, from outside the record — the one place the
unwriting could not reach; plain, no self-pity, the iceberg in "*i wrote it here*"):**
> they did not slay me and they did not leave me at the threshold to be argued over. they looked, and they
> did not keep me, and they took the looking back.
>
> there is no stone for me down there and no name said over me. so i went out, past the door that is not a
> threshold, to the place the record does not reach, and i cut the name myself.
>
> i kept all the ways. it did not matter. the keeping was never the price. i write it here because here is
> the one place it cannot be unwritten. read it and go back. you are not cast out. you only came to look.

**(d) The Nether soul-sand line (the record / Set-B register, flat, no personified objects, no warmth):**
> the blue fire keeps no one warm. it is the keeping that takes. these are the ones it took all the way
> down, before the first of you came. they are kept. they are not company.

**(e) The End shrine — the `refusers`/`cast_out` re-read clause (carried by the M5 composer only if the fate
fired AND `seventh_seen_out`; one clause, register only, group-scoped, never names FACT 15, never a player):**
> you stood where the unwritten stood, and you did not bow either. there is room out there. the record will
> not follow you to it.

All five: no exclamation, no adjective-stacking, no announced feeling, no "testament," mundane concrete
nouns (coal, sand, door, stone, name), the omission carrying the dread. (b) decays structurally. (c) ends
on a withheld mercy ("you only came to look"), not a stated one. (e) is group second-person ("you stood…
you did not bow"), never "player X" (INV-16).

---

## 4. THREAD IT — exactly where this lives (no orphans)

### Canon FACTs touched / added
- **TOUCHES FACT 11** (one fire never went out) — the Nether is its **source** ("below the below"); the
  Kept-Light custom's origin. No new fact; FACT 11 deepened — **gated on LORE sealing the one-sentence
  reading to FACT 11 first** (`[S3]`).
- **TOUCHES FACT 12** (the kept did not depart, they were kept) — from the souls' side (soul sand = the
  worst keeping, *deep-time*, distinct from the Pale, `[S5]`) AND the pocket keeper's side (kept *as* the
  fire).
- **TOUCHES FACT 10/10b** (the land can refuse; it refused one who broke nothing) — the End is where the
  refused *went*; the shrine is the Seventh's account from outside.
- **TOUCHES FACT 14/15** (the record receives/keeps you; you become the watching) — felt, never stated, via
  the pocket keeper who became "*the part of it that does not go out*" and the soul sand. **No line states
  FACT 15** (rule §6.2).
- **ADDS NO new sealed FACT.** Both lanes are *delivery bodies* for existing facts. Governed by existing
  invariants — **no new INV minted** (the namespace is synthesis-owned, `WEB-MASTER §0.2`): **INV-11**
  (`nether_forge_found` group-scoped/active-only/enum-feeding/no-player), **INV-14** (on-site word answers,
  not the coordinate), **INV-12** (color, never gate), **INV-16** (no per-player side derivable),
  **INV-19** (not in the Accepting quorum).

### Found-documents / journals (new, authored to the §3 exemplars)
- **D-NETHER-1 `the-fire-is-lent.md`** — the bearing-page (Mara's hand), at the Undercroft post-descent.
  `clue_bearing: true`, `movement: 3`, `links_to: [the-seventh-not-kept, learn-them-as-we-learned-them]`,
  `foreshadows: ["the kept fire has a source below the below (FACT 11)"]`. Points at the Nether pocket
  (INV-14: the on-site word answers, not a decoded signed coordinate — `[S4]`).
- **D-NETHER-2 `the-fire-kept-me.md`** — the pocket journal (the source-keeper's hand, decaying).
  `clue_bearing: false`, `movement: 3-4`, `links_to: [the-fire-is-lent, do-not-close-your-eyes-here,
  counted-them-in-the-dark]`, `foreshadows: ["to keep the fire is to become the fire that does not go out
  (FACT 15, felt)"]`. Anchors soul sand as deep-time (`[S5]`).
- **D-END-1 `the-name-i-cut-myself.md`** — the End shrine carving (the Seventh's hand, from outside the
  record). `clue_bearing: false`, `movement: 4`, `links_to: [the-seventh-not-kept, the-ways-are-a-wall,
  the-fire-they-let-out]`, `foreshadows: ["exile is the other side of keeping (FACT 10b / D11's one
  door)"]`. Pairs with D11 + the cause-fragment (the deep answers *that* they were unwritten and *why*; the
  End answers *where they went and what they said there*).
- **TOUCH `the-seventh-not-kept.md` (D11)** — no edit; it is already the End lane's plant ("cast out…
  before the threshold… one door, two sides"). The End is D11's spatial payoff.

### NPC / Watcher / Keeper voice keys (`discord/src/voice.ts`)
- **Set-B record lines (NEW keys, read by key):** `voice.nether.soulSand` (§3d), `voice.nether.forgeArrive`
  (the on-site Kept-Light-origin lore), `voice.end.shrineArrive`, `voice.end.outsideRecord`.
- **M5 composer clause (NEW, register only):** `voice.fateCastOutEndRead` / `voice.fateRefusersEndRead`
  (§3e) — appended **only** when the fate fired AND `seventh_seen_out`, within the §5 ≤2-clause cap (it
  *replaces* the neutral fate clause for groups who went out, so the cap holds — not an extra clause).
- **Brann's seeded Nether framing line** lives in his stone's carved framing (M2 plant) — corpus carving,
  no voice key.
- **Existing keys reused, no edit:** `voice.fateRefusers()` (the "so did one before you" re-read, §1.5).

### Ciphers / puzzles (reuse the 11 built ciphers — NO new cipher)
- **coordEncode (P6) + `stone_of_reckoning` digit-glyphs** — the Nether bearing's *direction* (INV-14: the
  on-site word, not a signed coordinate, is the answer). The pocket is near, so this is a *bearing*, not a
  full decoded coordinate-walk (`[S4]`).
- **rail-fence (P7, Brann's taught literacy)** — the End's way-out is the **same `the_unwriting` chamber-2
  wall** with one extra effaced line legible at `seventh_named` (§1.2.2). The line is a **reveal on the
  existing surface (no new puzzle node)**; the **arrival** is the new row `end-seventh-out` (`[S9]`).
- **On-site destination words** (Nether: keeper's name / "lent"; End: read-only carving) — **plaintext, no
  cipher**, so they normalize clean (INV-14).
- *(Atbash/coordEncode/substitution already ship; no new transform.)*

### Beats / listeners / tables / seed rows / sites.yml / website routes (real symbols)
- **Beats (existing, no new class):** `SmallStructureBeat` for **additive pastes onto verified-clear
  adjacent footprint only** (`[S7]` — soul-lanterns/lecterns/carving-slabs on air beside vanilla blocks;
  NEVER an occupied-overwrite); `RevealBeat` for block-state/marker flips; `LecternFillBeat` (D-NETHER-1/2
  leaves); `SignWriteBeat` (on-site answer-word carvings); `PrivateSoundBeat`/`PrivateParticleBeat`
  (per-presence arrival reveal). The pocket keeper-remains is **placed at world-build**, not pasted toward a
  player. P2 Nether apparitions reuse `NamedMobBeat` re-skinned + the single-arbiter conductor (INV-18).
  **No `RoomSwapBeat` in either dimension** (nothing mutates in the open End; the Nether pocket is static).
- **Listeners (existing, no new class):** `AnswerSignListener` (site-scoped, the on-site words) +
  `Site.contains` proximity (the dimension-presence signal). No new listener — the flag is set by the
  existing answer-on-site path, gated by the world the site is in.
- **Sites.yml — NEW entries (placeholder, null coords until GO-LIVE; explicit `world:`; the per-site
  `world:` field already exists):**
  - `nether_forge` — `type: structure`, `world: "observance_nether"`, the near-pocket keeper-grave anchor
    (the on-site word + `nether_forge_found`). `enabled: true`.
  - `end_seventh_shrine` — `type: structure`, `world: "observance_end"`, the re-dressed end-ship (or
    pre-generated outer-island) shrine (the carving + `seventh_seen_out`). `enabled: true`.
  - `end_exile_hold` — `type: structure`, `world: "observance_end"`, `enabled: false` (the P2 end-city
    `cast_out` set-piece; left disabled until the INV-16-bound binding is built, §1.5/§6).
  - The Nether/End worlds are Multiverse worlds (the Undercroft pattern, `backlog-undercroft-dimension`).
- **arc_state flags — NEW (synthesis to mint; group-scoped, active-only, INV-11):** `nether_forge_found`
  (bool — the *proposed* `FateInput.netherForgeFound`, S1, synthesis-ratified before it feeds `decideFate`);
  `seventh_seen_out` (bool — **NOT a fate input**, S2; M5-composer + `seventh_choice`-context only). Both
  gate nothing (INV-12).
- **Seed rows — NEW (`puzzles_seed.sql`, kebab keys; `requires_flags`-gated, `WEB-MASTER §2.1`):**
  - `nether-forge` — outcome `lore` (+ sets `nether_forge_found`, Whisper budget). Answer = the on-site
    word. `requires_flags: [undercroft_open]`. `active:false` until the flag + the Nether world is placed.
  - `end-seventh-out` — the **arrival payoff** at `end_seventh_shrine` (outcome `lore`, sets
    `seventh_seen_out`). `requires_flags: [seventh_named]`. `active:false` until the End world is placed.
    (The way-out *pointer* is a reveal on the existing `seventh-unwriting` surface — no row, `[S9]`.)
- **Breadth rows (`seventh_seed.sql`, shape `(quest_key, thread_key, entry_puzzle_key, reward, tier,
  est_minutes)`, `gates_progress` defaults false):** `dest-deep-forge` (thread `who`, entry `nether-forge`,
  the Nether keeper-grave); `dest-out-of-record` (thread `who`, entry `end-seventh-out`, the End shrine).
  Mirrors `dest-unwriting-deep` / `dest-fire-let-out`.
- **thread_cards:** one rumor card per destination → flips `verified` on arrival (both under `who`), the
  existing `side_quests` card pattern.
- **Website routes:** **none** (`[S8]` — the lanes do not widen `record-projection.ts`; they feed Discord
  Whisper + the dashboard fate-preview + the M5 composer only). A deep-fire website line, if ever wanted, is
  a separate explicit `record-projection` 4th-field task with a selftest update.
- **`decideFate` (`fate.ts`) — PROPOSED edit (S1, synthesis to ratify):** add `FateInput.netherForgeFound`;
  `kept` may fire on `(seventhFound || issCaught || netherForgeFound)`; **self-test: `kept` fires fully
  without it.** `seventh_seen_out` is **not** added to `FateInput` (S2).

### The side-quests this spawns or links
- **Links (does not duplicate):** `dest-fire-let-out` (the Seventh's *why*, the cause-fragment) is the
  Overworld twin of the End shrine's *where* — a group that walks both holds the full Seventh question.
  `dest-dead-shrine` (Iss's false-walk endpoint) and the Nether "burned door" frame keeping (down) vs the
  lie (out, false) vs exile (out, true). The Nether pocket is the **one sanctioned additional walk**
  (vertical, near — `[S4]`, walk budget = 2 ground + ≤1 vertical pocket); the End is reached **out a door**
  (the vanilla portal the group finds), **not** a third decoded coordinate.
- **Spawns:** nothing net-new off-spine; both lanes are bodies for the Seventh thread and the divergence.

---

## 5. PLANT → PAYOFF — entries for the master ledger (`WEB-MASTER §9`)

> Discipline: no plant without payoff; no payoff without plant. Staggered across movements (arg-craft F3).

- **PLANT (M2, inert):** Brann's stone carved-framing line "*the fire we keep is not ours. it is lent…
  below the below.*" At M2 it reads as the watchman's raving (his doubling fingerprint).
  → **PAYOFF (M3→M4):** the Nether pocket — the kept fire *has* a source, "below the below" was literal, and
  the keeping was always a *carrying* (FACT 11 deepened). *"oh — the one fire upstairs was an ember carried
  up from that."*
- **PLANT (M3, ambiguous):** D11's close "*to be kept and to be cast out are one door, looked at from either
  side.*" At M3 a melancholy aside. → **PAYOFF (M4→V):** the End — the *other side of the door*, the place
  the unwritten went, with the Seventh's own carving in it. The "one door" becomes two literal places (the
  Nether keeping / the End exile) you can stand in. *"oh — the door was real, and this is its far side."*
- **PLANT (M3, found):** D-NETHER-1 "*someone who keeps the light better than i kept it should carry it
  down to where it is kept for everyone.*" Reads as Mara's deferral. → **PAYOFF (M3→M4):** the group
  *becomes* that someone (they carry the coal down) — and the pocket keeper (D-NETHER-2) shows what carrying
  it all the way down *costs* ("*i am the part of it that does not go out now*"). FACT 15 felt from the
  keeper's side. *"oh — Mara was asking us to do the thing that took the last keeper."*
- **PLANT (M4, on the unwriting wall):** the extra effaced line at `seventh_named` pointing "*out, past the
  door that is not a threshold.*" Reads as one more effaced fragment. → **PAYOFF (V):** for a
  `cast_out`/`refusers` group with `seventh_seen_out`, the End re-reads as *their own* fate's place — the
  exile-hold/shrine was where they were always headed if the land said no. *"oh — we walked to where we were
  going to end up."*

> Master-ledger rows to append (`WEB-MASTER §9`, status DESIGNED→seed):
> | # | SEED | Planted | Inert meaning | Payoff | True meaning | Status |
> |---|---|---|---|---|---|---|
> | 24 | Brann's "the fire is lent… below the below" | M2 | a watchman raving | M3→M4 | the kept fire's source = the Nether furnace (FACT 11, sealed reading) | DESIGNED→seed |
> | 25 | D11 "to be kept and to be cast out are one door" | M3 | a melancholy aside | M4→V | the End is the door's far side; exile has a place (FACT 10b) | DESIGNED→seed |
> | 26 | D-NETHER-1 "someone who keeps the light better… should carry it down" | M3 | Mara's deferral | M3→M4 | the group carries the coal; the pocket-keeper shows the cost (FACT 15 felt) | DESIGNED→seed |
> | 27 | the unwriting wall's extra effaced "out past the door that is not a threshold" line | M4 | one more effaced fragment | V | the End exile-place is the `cast_out`/`refusers` group's own destination | DESIGNED→seed |

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Element | Movement | Depends on | Depended on by | Priority |
|---|---|---|---|---|
| Brann's "fire is lent / below the below" framing line | II | Brann's stone placed | the Nether lane's plant | **P2** (cheap carve) |
| **LORE seal: FACT 11 "below the below" one-sentence reading** (`[S3]`) | — | — | the whole Nether lane | **P1 BLOCKER** (seal before any Nether build) |
| **D-NETHER-1 + D-NETHER-2 + keeper-remains pocket arrival → `nether_forge_found` + bonus Whisper + Kept-Light-origin lore** | III→IV | the FACT-11 seal; `undercroft_open`; the `observance_nether` world + `nether_forge` site placed (GO-LIVE); INV-14 on-site word | the `kept` read; the FACT-11-source + FACT-15-felt payoff | **P1** |
| **End Seventh shrine (re-dressed end-ship / pre-gen island) + carving → `seventh_seen_out`** | IV | `seventh_named`; the `observance_end` world + `end_seventh_shrine` placed (GO-LIVE, reveal-safe, `[S6]`) | the End `cast_out`/`refusers` re-read; the Seventh-question depth | **P1** |
| The unwriting wall's extra effaced "out the door" line (reveal, no row) | IV | `seventh_named` | the End lane's diegetic way-out | **P1** (one carved line on an existing surface) |
| **`decideFate` `netherForgeFound` input (synthesis-ratified, S1) + M5 composer clauses (`seventh_seen_out`, S2)** | V | `fate.ts` (built); the M5 composer (`WEB-MASTER §5`); synthesis ratifying the FateInput edit | the `kept`-lean + the End re-read clause | **P1** |
| Nether bearing-**trek** (full walk vs near pocket) | III→IV | the Nether world built; a playtest that the M2/M4 walks didn't exhaust the group (`[S4]`) | richer tempo change | **P2** |
| End-city `cast_out`/`refusers` **place** binding (`end_exile_hold`, INV-16-bound) | V | `seventh_seen_out`; the fate fired; the end-city re-dressed; INV-16 chorus guarantee (`[S2]`) | the divergent ending made a place | **P2** |
| Nether **intimate apparition** beats (basalt glimpse, soul-fire bank) | III→IV | the single-arbiter conductor (INV-18); the Nether world | atmosphere depth | **P2** |

**Net dependency story.** Both lanes **depend on**: the FACT-11 seal (`[S3]`, the one hard pre-build LORE
task) + the Undercroft descent (the Nether bearing-page is found there) + the Seventh deep solved
(`seventh_named`, for the End way-out) + the two new Multiverse worlds (GO-LIVE, the Undercroft pattern) +
the existing expedition/INV-14/Multiverse machinery + synthesis ratifying the S1 `FateInput` edit. **Nothing
depends on the lanes** — they gate nothing (INV-12), feed nothing into the Accepting quorum (INV-19), and a
group that skips them gets a whole arc.

- **P1 (build for the real run — two cheap thematic cores):** the **Nether near-pocket keeper-grave** (FACT
  11 source + "you are becoming them" embodied) and the **End Seventh shrine** (the Seventh's voice from
  outside the record). Each ~one set-piece + one journal/carving + one flag, riding built machinery. Plus
  the two flags' wiring — `netherForgeFound` into `decideFate` (S1, synthesis-ratified) and
  `seventh_seen_out` into the M5 composer (S2, not the selector).
- **P2 (depth, when the slice is green):** the Nether trek (behind a playtest), the end-city `cast_out`
  place binding (INV-16-bound or cut), the Nether intimate apparitions.
- **Not P0:** nothing here belongs in the vertical slice (the slice proves the Overworld loop; these are its
  optional deepening lanes — the deep and far mirrors of the kept Overworld descent).

**Build order:** (0) **LORE seals the FACT-11 "below the below" sentence** (`[S3]`, blocker); (1) the two
Multiverse worlds + the three sites placed, reveal-safe (GO-LIVE, Undercroft pattern; End shrine re-dresses
an *existing* end-ship or a *world-build-pregenerated* island, `[S6]`); (2) D-NETHER-1/2 + the world-built
keeper-grave + `nether-forge` row + `nether_forge_found`; (3) the End shrine dressing + the unwriting wall's
extra line + `end-seventh-out` row + `seventh_seen_out`; (4) synthesis ratifies the `FateInput.netherForge-
Found` edit (S1) with the "`kept` fires without it" self-test, and wires the M5 `seventh_seen_out` clause
(S2); (5) extend `siteCoverageSelfTest` to assert the named world exists before a cross-dimension row
activates (R7); (6) P2 garnish.

**Always:** de-slop every line (§3); confirm **no line states FACT 15** (the pocket keeper *embodies* it,
never narrates it); confirm both lanes **gate nothing** (INV-12) and feed the fate **active-only,
group-scoped, no-player** (INV-11, INV-16); the End has **no ambient apparition lane** (R3); the on-site
word is the answer, never the coordinate (INV-14); soul sand = deep-time, NOT the Pale (`[S5]`); occupied
vanilla structures are **additive-dressed, never overwritten** (`[S7]`); the website is **not** widened
(`[S8]`); one direction, one deepened bottom — **no second "real bottom"** (`[S3/R1]`).
