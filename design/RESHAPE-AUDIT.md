# RESHAPE AUDIT — live-state findings feeding the add/change/cut pass

> **Superseded — fully absorbed into `CHANGE-MANIFEST.md` / `THE-RESHAPE.md`** — kept for history.
> Historical below.

> 2026-07-02. Four read-only recon agents audited all five surfaces against the direction (world-not-puzzle-
> game). Distilled verdicts below. This feeds `CHANGE-MANIFEST.md` (the add/change/cut pass). Status per
> surface at bottom.

---

## SURFACE 1 — STRUCTURES  ✅ audited

**THE headline finding (P0, cheap, huge):** *every single set-piece has a waxed "label sign" that announces
what it is or what to do.* This is the #1 thing making it feel like a puzzle game. The spaces themselves
often already read as world-mystery — the labels strip that away every time. **The label-sign cull is the
fastest, highest-leverage move in the reshape.**

### Cut the label entirely (no replacement — the architecture speaks):
- **orin** — "the low stone asks a bow — stoop to read" (EXPLAINS the forced-bow lintel before you feel it; the lintel is the best mechanic in the set — let it perform).
- **iss** — "the fire is kept (it lies)" — the parenthetical "(it lies)" murders the deception. Let the soul-fire-behind-glass + soul soil creeping onto warm brick do the lying.
- **unwriting** (Seventh's chamber) — "the seal is a name. the wall was scraped — read it back" = near-total spoiler. Strongest room in the build (scraped wall vs one clean slab, stopped ceiling); narration ruins it.
- **coldHearth** — "nothing is kept here. he sent you out" — explains the emotional beat the doused hearth + roots-grown-over-the-door already deliver silently.
- **unbrokenLight** — "bow as one — all who are here" is a stage direction; the AcceptingRiteListener handles it.

### Reduce to NOTATION only (world-cipher, untranslatable without the reckoning site — no English instruction):
- **rosetta** — cut "read, then answer." Pillar marks must NOT be sequential i–vi Roman numerals (that's a tutorial list); use worn/partial carvings. The "wrong" pillar shouldn't be blank/outward (too obviously odd-one-out) — instead one mark's *face* is physically abraded (cracked block) and its content differs subtly.
- **reckoning** — cut "count the marks, then the way — north, down, read" (a walkthrough). The cracked/disputed compass arm is the detail to study, not a sign.
- **threshold** (the future-dated grave) — replace label with only the date in world-notation (unglossed); CUT "the stone is open from the inside" — the shoved-out capstone lit from within IS the story.
- **thresholdVault** — cut "each holds one rune — read them as one." Replace with untranslatable cipher inscription. (This site is *allowed* to read as a made lock — the honest co-op mechanism — but not to narrate itself.)

### Keep with minor refine (submission surface must become diegetic, not a blank "fill-me-in" box):
- **vaun** — cut label; fold the ledger line into the chiseled stone as partially-worn carving with only the final line blank (visible erasure, not an empty form). Use a copper-oxidation gradient across the hoard wall as a corrosion-over-time clock.
- **mara** — CLOSEST to world-mystery already (empty 2nd lectern = someone stopped reading). Cut label; move the submission surface to the empty 2nd lectern + the bookshelf gap (submission reads as "returning the missing volume," not filling a form).
- **sella** — cut label; keep the bare marker post but replace blank sign with a partially-worn carved name. The reflecting pool / child's copybook ("a a a b b b") already tell it. (Seagrass-from-above spelling = a Paper per-player map-illusion opportunity.)
- **brann** — "count the black moons — do not sleep" is an instruction → migrate. Make the bell actually ring at midnight/moon-phase (world-fact not prompt); make the chiseled/plain tally IRREGULAR (missing/out-of-sequence blocks) not a neat countable row.

### Retire / rebuild:
- **keeperStone** (generic fallback) — the ur-form of puzzle signage (pillar + blank sign). Retire from player-reachable sites; if kept as fallback, replace blank slot with an already-partly-illegible inscription.
- **reckoning** — rebuild: the six-amethyst counting row is a literal tutorial widget; replace with something needing cross-reference to interpret; keep the "squared, cold, exact" aesthetic + the cracked arm.

### PLACEMENT (both placeregion + placedeep):
Produces an **east-marching spine with only a ±4-block N/S wobble** — functionally a straight corridor read
as "walk these in order." Terrain-seating (OCEAN_FLOOR heightmap) gives real vertical variation (good) but
the horizontal march betrays a machine grid. **Fix:** non-uniform X intervals, rotate per-site orientation
axes, let natural terrain (cliff/waterway) separate sites so they feel independently always-there, not
sequenced. (This is a placement-logic change in ObservanceCommand.java, lower priority than the label cull.)

---

## SURFACE 2 — CIPHERS/PUZZLES  ✅ audited

**Audit-overstates-brokenness AGAIN:** of 63 rows verified against the live seed, most are ALREADY the right
type. The whole Diverse Expansion (rows 44–63) is largely EXEMPLARY — object-deposit (vaun-hoard-sorted),
reflection-bearing (sella-reflection-bearing), forced-perspective glyph (sella-shore-memorial), sculk-silence
corridor (brann-silence-corridor), asymmetric co-op vault (spine-threshold-vault), spoken-name Observer beat
(spine-spoken-name), significant-negative (spine-cold-hearth-shadow). Don't touch these.

### THE narrow, surgical changes:
- **P0 — literacy gate is the fragile single node.** `rosetta-ring` + `a1z26-tick-stave` + `reckoning-rosetta`
  all teach the whole alphabet by ONE recitation → convert to RECONSTRUCTION: scatter glyph-referent pairings
  across the 6 keeper sites (rune carved next to the thing it names — Chants-of-Sennaar model; fair by the
  ~25-char frequency proof). `rosetta_known` gets set by accreted comprehension, not a single recite. This is
  THE highest-leverage change and it re-sequences every downstream cipher gate.
- **Cut `stone-sella`'s Atbash** — redundant with its own better replacement `sella-reflection-bearing`
  (environmental, no letter-reversal). The stone becomes the observation prompt ("blank, but the water shows a
  mark"), the bearing comes only from the reflection.
- **5 keeper ciphers STAY** (exact budget): vaun Caesar · mara book-cipher · orin substitution · iss Vigenère ·
  brann rail-fence (staged). They must look like *carved decoration you realize is a cipher only after earning
  literacy* — difficulty from NOTICING, not from the shift. **Tie to lore audit:** make each cipher the formal
  shape of its keeper's self-defeat (Caesar=held-back, Atbash→now cut, Vigenère key=name-as-its-own-inverse).
- **`forged-eighth` + `prophet-wall-comfort`:** make the first-order tell ENVIRONMENTAL (the 8th custom's verb
  appears on no teaching stone; different chisel/material) — noticeable before decoding. Keep the cipher as the
  optional deep read. `prophet-wall-name` acrostic + `meta-unkept` UNKEPT acrostic are observation, keep (make
  the glyph column visually distinct).
- **`brann` temporal gate is great** (night/black-moon) — keep; consider making the night reading purely VISUAL
  (shadows resolve in beacon glow) instead of rail-fence. `record-url`/external stego (`spine-recovered-archive`,
  `iss-nbt-falsified-entry`) = keep as Layer-2/3 but ensure the URL token has ≥3 independent routes (anti-datamine).

### CLUE-GRAPH: chain-with-a-real-web-layer. Fragile nodes to fix (Three-Clue Rule):
1. **Literacy** — single-point gate (P0 above); needs ≥3 sites teaching glyphs by referent.
2. **`stone-mara` = sole door to the Undercroft** — fix is cheap: LAND the already-staged `stone-brann-cipher`
   as the authored second door.
3. **"there was a seventh"** — the number **seven** is designed as a motif but NOT threaded in-world; make it
   recur independently at ≥3 keeper sites (Vaun's tally, Brann's fires, Sella's copybook) so it triangulates.
4. **M4 4-step chain** (bound-word → m4-three-hands → threshold-coordinate → true-walk-arrive) — strict, no
   bypass; author a second entry into m4-three-hands or accept it as the hard pre-finale gate.
5. **"Mara → descend at the unbroken light"** — single-source; add environmental corroboration (the single
   Undercroft lamp VISIBLE from the surface through a gap before you descend).
Genuinely-web already (keep): 6 keeper stones open in any order · Seventh deep = AND-join of sella + iss threads
· rite-tokens reachable via true-walk-arrive AND pressure-glyph-walk · Iss-is-liar has 3 routes (Vigenère key /
cross-check the land / prophet-wall acrostic). The 3-layer architecture (spine / secret / community) maps cleanly.

## SURFACE 3 — LORE  ✅ audited

### FACT-LAYERS: the MISLEADING layer is the weakest (highest-value fix)
- confirmed (flat/Archivist voice) = strong+intentional; implied/sealed (F15 induction) = excellent; false
  (F7b the forged eighth way, weaponized via INV-17) = good. **Misleading = weak:** Iss's lie SELF-REFUTES in
  its own margins (D09 already calls him out) → a lie with training wheels, not a real false trail.
- **THE fix:** add ≥1 *earnestly-believed, internally-unrefuted* false account — someone who accepted Iss's
  "the Seventh was spared / mercy" framing with no in-document skepticism, so the group must hold two competing
  accounts in tension until the Seventh speaks. Also plant an M1 fragment that *seems* to confirm good-conduct→
  kept (only correlation) so F10b (the land refused an innocent keeper, M3) lands as a reframe. F4 ("customs
  learned from the land") should feel plausible-but-self-serving, not flat-true. Model everywhere: Wren's
  "stay close" (warm→curdles) is the best-executed layered phrase in the project.

### MOTIF: "kept" is the gold; "seven" is named-not-threaded; the cipher-as-inversion is the big unlock
- **"kept"** = the de-facto motif, genuinely polysemous (recorded/observed/held-captive/preserved/belonging-to-
  the-record/Wren). Keep leaning on it. **"seven"** is in the FRAMING (six keepers + a blank seventh) but does
  NOT recur *inside* the six keeper docs → thread it so "Vaun's thing and Sella's thing are the same thing."
- **HIGHEST-VALUE motif move (ties structures↔puzzles↔lore):** make **each cipher the formal shape of its
  keeper's self-defeat** — Caesar = held back by a fixed amount from its true value; Atbash = every letter its
  opposite; Vigenère key = a *name* that decodes to its own inverse ("the one who turned away"). Then the
  ciphers stop being "decode this" and become *characterization*; the pattern-reader who notices all six share
  an inversion structure arrives at the motif unaided. Also echo "the one who turned away" beyond the Iss
  payoff (D11, the Seventh's carving, Wren's arc — he too turns away from a full reckoning).

### CALLBACK AUDIT — orphans (mechanic with NO lore callback) + dangling (lore → nonexistent mechanic)
Orphans: (1) composure-signal / Tier-0 ambient register — no prior-keeper document parallels the sub-custom
tracking ("you keep one thing you never use"); (2) `nine-grey-one-white` pale-lamb doc = cosmetic unless wired
to the INV-13 Sacred-Beast (one white glowing animal in the herd); (3) Wren's tally-page = STATIC placeholder
(`[the group's own name for each other]`) until the Observer Engine (Phase 4) populates it — if Wren surfaces
first it has no real data; (4) asymmetric co-op vault has NO lore justification for *why* truth is partitioned;
(5) `name-where-never-been` carve has NO voice.ts line for the discovery moment (big experiential gap).
Dangling: Nether-as-fire-source (F11 sealed, no discoverable carrier); UNKEPT maker's-mark acrostic (needs the
carved glyph framing actually built on the stones); Wren-relocate-during-Watcher-beats (design intent, no wired
listener — if unbuilt, Wren present at a scare breaks the two-register model); RoomSwapBeat (no lore parallel).

### SILENCE / CONTRADICTION — three underused load-bearing assets
1. **The erasure-smear:** the Seventh's name went "blank mid-line while i looked at it" — this should be a
   PHYSICAL artifact in the record-book (a blotted line / ink-run where a name was), the most disturbing thing
   in the first look. Described in text, never built in-world.
2. **The fire-geometry contradiction (physical proof of F10b, free from M3):** Brann says the home-fire needs
   no hand ("it is simply kept") — so the Seventh's fire going out ≠ neglect, it was *refused*. Make it an
   observable world-state clue, not just two texts read together.
3. **Sella's blank page = the literacy epiphany setup:** her Atbash journal is blank AWAY from water, text AT
   the shore pool. Weaponize "why is this page blank?" as the question that leads to the water (earned literacy),
   not "follow the lectern path." Also: the empty Offering cairn (deposit-tally reading zero) makes the
   ledger-vs-record contradiction visible from ONE text + one environmental silence; and an Iss keyhole that,
   turned, reveals "the one who turned away" carved behind it (not a door) makes his self-refutation physical.

## SURFACE 4 — WEB + INTEGRATIONS  ✅ audited

**Audit-overstates-brokenness holds AGAIN — trust the code.** The flag graph is ALREADY partially a web
(5 simultaneous ungated cipher stones + side-quests + parallel forks), and the per-player illusion DELIVERY
already works (beat_queue rows targeted by `mc_uuid` → plugin fires for one player). Don't rebuild these.

### WEB (the record) — the real gap: linear + binary, no fact-layers
- `/record/[slug]` reads one coarse view `v_record` → `{movement, stones_read, accepted}`; `record-projection.ts`
  maps to a FIXED ordered 6+1 list, `legible: i < stonesRead`. Redaction is one binary (`████` vs legible).
  **No confirmed/implied/misleading/false. Reveal is movement-INDEXED, not flag-gated → it's a list, not a web.
  Never says "you."**
- `/record/terminal` (richer) — thread-fill bars (`threads` table) are the CLOSEST thing to a web shape, but
  only coarse counts; no implied-by/contradicts edges; no per-layer styling; same for every visitor.
- **CHANGES:** `record-projection.ts` → add `factLayer/reliability` per entry + gate `legible` on `arc_state.flags`
  keys (not index); `record/[slug]/page.tsx` → render per-layer styling + pass flags into `project()`;
  `record/ledger.ts` → thread-edge metadata (implied-by, contradicts) + per-entry confidence;
  `record/terminal/page.tsx` → render the certainty/layer distinction.

### INTEGRATIONS — spine + drip are linear; delivery is web-ready
- Gate `flagsSatisfied()` (mirrored byte-for-byte Java ↔ 2× TS) is already non-linear (many nodes open at once). ✅
- LINEAR bits to move: the M1→M5 spine (`current_act` + progressive flags); `decide.ts` drip-picker surfaces ONE
  node/tick by deterministic OUTCOME_RANK→movement→key order. For a web it must surface a constellation / imply
  nodes without fully unlocking / offer confirmed-vs-misleading variants.
- No cross-clue edges anywhere: `snapshot.ts`/`types.ts SnapshotPuzzle` carry no implies/contradicts; add either a
  `clue_edges` table or `outcome_payload.implies_keys[]/contradicts_keys[]`. `state.ts dripped_keys` is a flat list.
- `voice.drip()` tone is a register temperature (`cold|plain|warm`), NOT certainty; add `certainty: certain|implied|
  false_trail` to drip + oracle lines. `answer.ts/resolve.ts ResolveResult` has no cross-node implication field.
- Forge surface (`clue-specs.ts`) = 5 cipher nodes only; `NON_CIPHER_KEYS` (40+) lists the diverse puzzle types
  that currently can't be forged (behavior/object/code/spoken/url_token/coords…). More forged non-letter clues →
  new `CLUE_SPECS` entries + a new card format (`render/cards.ts`) + templates (`forge/templates/`).

### PER-PLAYER ILLUSION — delivery LIVE, blocked only on the plugin SIGNAL read
- Delivery works: `enqueueBeat(type, target=mc_uuid, …)` → plugin poll fires it for one player (apparition,
  `private_message`, `named_mob` already flow this path). Whisper budgets, honor/violation tracking = per-player, LIVE.
- **THE BLOCKER (one dependency, unlocks a lot):** the per-player SIGNAL read from the plugin's measurement layer
  is missing — `conductor.ts shapeRhyme` is hardwired `{}` in `autonomy.run.ts`; grave/name-where-never-been/keeper
  Observer-tier all wait on the dossier + visited-cell reader (PLUGIN lane owns it). Land that read → per-player
  apparition, offline-skin, grave-targeting, name-on-unvisited-wall, Observer tiers all go live. **High-leverage.**

### LOCKSTEP TABLE (web ↔ integration — must change together)
| Change | WEB | INTEGRATION |
|---|---|---|
| fact layers | `record-projection.ts` factLayer; render in page | `types.ts` SnapshotPuzzle.factLayer; `snapshot.ts` read; `apply.ts` pass to card |
| web-aware drip | terminal thread bars → graph | `decide.ts` picker; `clue-drip.ts` card; new `render/cards.ts` format |
| non-cipher forged cards | terminal form handles more answer_kinds | `clue-specs.ts` entries; `forge/index.ts` kinds; new templates |
| per-player record | per-login conditional reveals | already delivers via mc_uuid; **needs plugin dossier reader** |
| drip certainty gradient | entry reliability shown | `voice.ts drip()` + oracle lines certainty param; `apply.ts` threads it |

## SURFACE 5 — (folds into the four above)

---
**Next:** when puzzles + lore + web/integrations audits land, synthesize the full add/change/cut pass into
CHANGE-MANIFEST.md (lockstep across all surfaces), then build in green waves — starting with the label cull
(structures) once the lore audit confirms what notation/callbacks replace the cut labels.
