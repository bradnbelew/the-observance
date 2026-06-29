# IDEA — Coordinates to a Real Place (the in-world expedition)

> **Build-ready design treatment.** Authored against the canon spine
> (`arc/lore/canon-spine.md`), the oracle loop (`discord/src/oracle/resolve.ts`,
> `normalize.ts`), the cipher core (`discord/src/forge/ciphers.ts` — `coordEncode`), the
> TRAVEL longevity layer (`design/content/travel-destinations.md`), and `plugin/.../sites.yml`.
> Operates inside the existing engine; adds NO new cipher and NO new beat class.
>
> **One-line pitch.** A solved cipher yields a real Minecraft coordinate the active group must
> physically *walk to* across their own server world — a mid-arc tempo change from sit-and-decode
> to expedition, with a reveal-disciplined set-piece already waiting when they arrive.

---

## 0. THE ONE HARD CONSTRAINT THAT SHAPES EVERYTHING (read first)

`coordEncode.encode({x:-1280, z:64})` carves the string **`X-1280,Z64`**. That string is what a
player *reads off a stone* — it is the **clue**, not the answer. The **answer** the player submits
(at a keeper-stone sign or in `/answer`) is run through `normalizeAnswer()`:

```
"X-1280,Z64"  →  NFKC → lower → [^a-z0-9 ]→space → collapse → trim  →  "x 1280 z64"
```

The `,` becomes a space (fine, expected), **but so does the `-`** — `-1280` normalizes to `1280`.
So the resolver **cannot tell `-1280` from `1280`** on the answer surface. This is the documented
minus-sign caveat, and it is fatal if the *plaintext answer* a player types is the literal
coordinate. **Mitigation is structural, not a patch (see §2.A).** Every line below respects it.

Two distinct coordinate ideas already live in the codebase; do **not** conflate them:
- **In-world MC coords** (`coordEncode`) — a 32-bit-lossless cross-surface handoff (DESIGN §2.8).
  This idea reuses that cipher *as the clue carrier*.
- **Real-life GPS coords** (lat/long to a place on Earth) — **CUT** (see §1.X). The friend group is
  veterans on a server; "their own world" = the **server world**, not Earth. That keeps it Path-A
  (install nothing), legal, and safe.

---

## 1. EXPOUND — how it actually plays

### 1.1 The shape of the beat
Mid-arc (Movement III), the group solves a clue that does not resolve to a *word* or a *door* — it
resolves to a **place**. The deciphered output is a Minecraft coordinate carved in the digit-glyphs
the group learned at the **Stone of Reckoning** (`stone_of_reckoning` in sites.yml — the literacy
gate that teaches digit-glyphs + the `-`/`,` marks; coordinate clues stay inactive until it is
placed). Up to now the arc has been *sit and decode*. This one says: **the answer is a walk.**

The clue is carved on **Iss's keeper-stone** (`stone_iss`). It is the **true** coordinate Iss yields
only *after* the Liar thread is caught (canon §4: "only after the catch does Iss yield the true
final coordinate"). Before the catch, Iss's stone shows a *different*, plausible coordinate (the
**false lead** — canon §4 seed: "leads to a real but wrong place — a dead shrine"). So the
expedition mechanic is **the physical form of the Liar payoff**: you don't just re-read a clue, you
**walk to two different places** and the world tells you which keeper lied.

### 1.2 The two walks (the non-linear web, made of feet)
- **The false walk (M2→M3, pre-catch).** Decode Iss's first carving → coordinate of **the cold
  hearth** (`the_cold_hearth` — already in sites.yml, canonically "Iss's dead-shrine grave / the
  false-way-up's end"). The group walks 1–2k blocks, arrives, finds a doused shrine and a leaf in
  Iss's plain hand. The rumor *verifies as a place* (it is real) but **contradicts as hope** — there
  is nothing kept here. This is the anti-speedrun tax made literal: a pooling group shares the
  decoded number in one second but **cannot pool the walk**.
- **The true walk (M3→M4, post-catch).** Catching the lie (the Vigenère key = Iss's own name, which
  decodes to "the one who turned away" — canon §4) flips Iss's dialogue state and **re-carves his
  stone** (a `SignWriteBeat` on `stone_iss`) to the *true* coordinate → **the Threshold/Undercroft
  approach** (`the_threshold`). The second walk arrives at the back-half spine.

Two doors, varied payoff, a red herring you walk on your own feet. The resolver "ignores movement
order by design," so a group that walks the true place first (e.g. stumbles on the Threshold) still
gets a coherent arc — the false walk just re-reads later as the lie it was.

### 1.3 The arrival set-piece (reveal discipline)
Nothing is witnessed appearing. The destination tableau is **pre-pasted** (`FaweSchematicPaster` /
`SchematicPaster`, the discovered-never-witnessed rule) or placed at world-build time. On arrival
the only *live* engine actions are reveal-safe: a `LecternFillBeat` (a found leaf), a private
`PrivateSoundBeat`/`PrivateParticleBeat` for the player who steps inside the site radius, and — for
the true walk — the `the_threshold` site becoming the launch point of the Accepting (the existing
back-half spine). The set-piece **exists before they arrive**; the walk is the mechanic, the tableau
is the payoff, the engine only *notices* they came (proximity, the same `Site.contains` path the
Bow/cairn already use).

### 1.4 The ~2-week / 5-movement placement (concrete)
- **M1 (the notice).** A single **inert plant**: the digit-glyph Rosetta (`stone_of_reckoning`) and
  one carved number-pair the group can't yet read as a *place*. It looks like a tally. (Payoff plant
  — see §5.)
- **M2 (the full ways).** Iss is met (warmest of six). His stone carries the **false** coordinate
  clue. A sharp group decodes it now and takes **the false walk** — the cold hearth. It verifies-as-
  place, contradicts-as-hope. Lore pays; nothing gates.
- **M3 (the descent opens).** The literacy lands (`stone_of_reckoning` placed); the M1 number-pair
  now *reads as a coordinate*. The expedition tempo-change is the spine event of M3: decode → walk.
  The Seventh-Stone thread (FACT 10) can share the road — a second optional coordinate to the dead
  shrine cluster.
- **M4 (the catch).** The Liar is caught; Iss's stone **re-carves** to the **true** coordinate; the
  group takes **the true walk** to the Threshold. The false walk re-reads as the lie.
- **M5 (the rite).** The true walk's endpoint *is* the Accepting approach. The expedition lands the
  group at `unbroken_light`/`the_threshold` for the collective bow. No new mechanic in M5 — the walk
  delivered them.

### 1.5 What the player does, step by step (true walk)
1. Catches the Liar (Vigenère re-walk, existing puzzle). Dialogue flips.
2. `stone_iss` re-carves (SignWriteBeat) to a digit-glyph coordinate, e.g. `X-1280,Z64`.
3. Player reads it using Rosetta literacy: "x is negative twelve-eighty, z is sixty-four."
4. Player **walks there** (engine does nothing; this is the tempo change — minutes of travel a pool
   can't shortcut).
5. On arrival the `the_threshold` site's radius triggers a private reveal + a found leaf.
6. To *confirm* the expedition to the oracle (and earn the beat/lore), the player answers a
   **co-located prompt** — NOT the raw coordinate (see §2.A): a short **word** carved *at the
   destination* that only someone standing there can read. That word is the puzzle answer.

That last step is the whole anti-jank fix: **the coordinate is the clue; a word found AT the place
is the answer.** The minus sign never has to survive normalization.

---

## 2. CRITIQUE — adversarial, honest

### 2.A RISK (sharpest) — the minus-sign caveat silently breaks any negative coordinate answer.
If the puzzle answer is the literal coord, `-1280` and `1280` normalize identically, so a player who
walks to `(+1280, 64)` solves a puzzle meant for `(-1280, 64)` **without leaving spawn**. Worse,
it's *silent* — the resolver returns `solved`, the loop looks fine, the expedition is bypassed.
**This is a defect, not an edge case** (most interesting MC coords are negative on at least one axis).
- **MITIGATION (mandatory, structural): the coordinate is never the answer.** The decoded coordinate
  is a *navigation clue only*. The puzzle's `answers` row is a **plaintext word carved at the
  destination** (e.g. a keeper-name, a one-word rite term) that is `[a-z]`-only and normalizes
  cleanly. The player must physically be at the place to read it. This makes the walk
  *unskippable by decoding alone* (you can compute the coord, but you cannot guess the destination
  word without going), and sidesteps the caveat entirely. **No engine change, no normalizer change.**
- **SECONDARY MITIGATION (defense in depth): proximity gate.** Bind the destination's answer-sign
  to one puzzle (`puzzle-key` on the `the_threshold` site) so the answer can ONLY be submitted from
  inside the site radius (the `AnswerSignListener` already runs site-scoped). A pooled answer typed
  in Discord from spawn matches nothing; the loop stays silent (a miss, never a tell). Belt and
  suspenders: even if someone leaks the destination word, they must stand there to use the in-world
  sign — or, if answered in Discord, it advances *lore* but the **physical reveal** still requires
  presence.

### 2.B RISK — "real coordinates to a real place" (GPS / Earth) breaks three laws at once.
Path-A (friends install nothing, travel nothing IRL), privacy/precision (you cannot *measure* that
they went to a real-world spot), and "never punish the absent" (a veteran who can't physically
travel is excluded). It also reads as a creepy real-life-stalking gimmick on camera, the opposite of
"it knows your name" warmth.
- **MITIGATION: CUT the IRL reading entirely.** "Their own world" = the **server world**. Confirmed
  by the pitch's own engine mapping (sites.yml, coordEncode = MC coords). This is the only reading
  that ships.

### 2.C RISK — orphaned gimmick. A "go here" mechanic with no narrative home is a tech demo.
- **MITIGATION: it is the physical body of the Liar payoff (canon §4) and the on-ramp to the
  Accepting (FACT 13/14).** The false walk = Iss's false coordinate; the true walk = his true one;
  the destination = the back-half spine. It moves story + clue + lore + NPC (Iss) + the rite in
  lockstep. It is not a new thread — it is the *legs* of one that already exists.

### 2.D RISK — confusing on camera / for the friend group ("decode → now what?").
A number with no instruction reads as a dead end; players brute-force `/answer` with the digits and
get silence, then feel the loop is broken.
- **MITIGATION:** (1) The Rosetta (`stone_of_reckoning`) teaches that digit-glyph strings prefixed
  by an axis label *are coordinates* — it shows the F3 debug-screen convention explicitly, so the
  format reads as a place, not a word. (2) The clue stone's *surrounding* carving (a Set-B record
  line) frames it: "the way is kept here. the record is elsewhere. it is x. it is z." — declarative,
  cold, points outward. (3) The destination word answer means typing the digits returns honest
  silence (a true miss), which is correct, not broken — and the framing line already told them the
  answer is *elsewhere*.

### 2.E RISK — pooling / split-the-party trivializes it. One scout walks, drops the coord, done.
- **MITIGATION (accept, don't fight):** This is *fine* and on-theme. The coord pools; the **physical
  reveal at the site is per-player-presence** (private sound/particle fires for whoever enters the
  radius). The lore card pools (the group learns it together); the *experience* of the place does
  not. And the destination word still requires at least one body on site. Collective ending is
  gated on **active** players, never on everyone having walked — a member who didn't make the trek is
  never punished.

### 2.F RISK — the walk is *boring* (a long featureless trudge kills the tempo change it promised).
- **MITIGATION:** Distance is authored to the existing TRAVEL ledger (1–3k blocks; ~13–26 min incl.
  scene), and the road is salted with the existing ambient TRAVEL destinations
  (`travel-destinations.md`) so the walk has texture, not emptiness. The two expedition endpoints
  (cold hearth, Threshold) are *already* canon sites — they are not invented for this beat.

### 2.G SCALE-DOWN call. Do **not** add per-keeper expedition coords for all six stones.
Six walks is grind, not tempo change. **KEEP exactly two** (false + true, both Iss's), plus the
*optional* Seventh-Stone coordinate (FACT 10, ignorable). That is the whole expedition budget for the
arc. More than that and the "sit-and-decode → walk" contrast flattens into "this game is walking."

---

## 3. DE-SLOP TEST — exemplar lines (in-voice, cold/plain/concrete)

**Watcher / record (Set-B) — the framing line on Iss's stone (pre-catch, false coord):**
> the way is kept here. what you want is not here. it is at x. it is at z. go and see who waited.

**Watcher / record (Set-B) — re-carve on the catch (the stone changes off-camera, true coord):**
> the first numbers were his. these are the record's. the count of the road does not come out the
> same as the count he gave you.

**Found leaf at the cold hearth (the false walk's payoff — corpus flat hand):**
> walked the numbers he gave. a hearth, no fire, no stone, no name. the oil is here and good. the
> cold is older than the oil. he sent us to a place that was already finished.

**Watcher / record (Set-B) — arrival at the true place (the Threshold):**
> you came on foot. the road did not shorten for being shared. the markers are below this. bow there
> or do not. the record keeps the road as it keeps the rest.

(No named emotion, no "testament," no three-adjective lists, no bow-tie, no exclamation, no em-dash
drama; concrete nouns — oil, hearth, numbers, road; the iceberg is "who waited" / "finished".)

---

## 4. THREAD IT — exactly where this lives (no orphans)

### Canon FACTs / threads it touches or adds
- **Touches FACT 8** (Iss lied; re-walk a "solved" clue) — the expedition is the physical re-walk.
- **Touches FACT 6 / FACT 14** (the threshold; being kept) — the true walk delivers the group to
  `the_threshold` / `unbroken_light`, the Accepting on-ramp.
- **Touches FACT 10** (the land can refuse) — optional Seventh-Stone coordinate to the dead-shrine
  cluster shares the road.
- **ADDS no new sealed fact.** It is a *delivery mechanism* for existing facts (correct — adding a
  fact here would orphan it). One small canon **INV** to record: **INV-COORD — a coordinate clue's
  decoded value is a navigation pointer ONLY; the puzzle answer is always a clean `[a-z0-9]` token
  found at the destination, never the signed coordinate** (the minus-sign law, §0/§2.A).

### Found-documents / journals that must mention or foreshadow it
- `arc/lore/documents/*` — Iss's leaf at the cold hearth (false-walk payoff); Iss's true-coordinate
  testimony (post-catch). Both already implied by canon §4; author the two leaves.
- A **founder/Survey post** near `stone_of_reckoning` foreshadowing that "the record keeps roads,
  not only words" (M1 inert plant framing, §5).

### NPC / Watcher voice lines that carry it
- **Iss** (Set-A, plain-and-false then cold on re-read) — gives false coord, then true coord. Lines
  live in `npc-and-watcher-voice.md`; the dialogue-state flip is the existing Liar mechanic.
- **Watcher** (Set-B) — new voice keys (see below), read by key, never hardcoded.

### Cipher(s) / puzzle(s) it expresses (reuse the 11)
- **`coordEncode`** — carries the clue (the decoded place). PRIMARY.
- **`vigenere`** — the catch that flips Iss's stone to the true coord (existing; key = Iss's name).
- **`substitution`** (rune map) + the **digit-glyphs** taught at `stone_of_reckoning` — the literacy
  that lets the carved coordinate be *read*.
- The **destination word** answer is plaintext (no cipher) — deliberately, so it normalizes clean.

### Beat classes / listeners / tables / sites / voice keys that realize it
- **Beats (existing, no new class):** `SignWriteBeat` (re-carve `stone_iss` on the catch),
  `LecternFillBeat` (the found leaf at each destination), `PrivateSoundBeat` / `PrivateParticleBeat`
  (per-presence arrival reveal), `FaweSchematicPaster` / `SchematicPaster` (pre-pasted tableau,
  discovered-never-witnessed).
- **Listener:** the existing `AnswerSignListener` (site-scoped) + `Site.contains` proximity — the
  same path the Bow/cairn use. No new listener.
- **Sites (already in `sites.yml`, just need coords at world-build):** `stone_iss` (clue carrier),
  `stone_of_reckoning` (literacy gate), `the_cold_hearth` (false walk), `the_threshold` /
  `unbroken_light` (true walk). For the answer-presence gate, set `puzzle-key:` on the destination's
  answer-sign site so the destination word only resolves on-site.
- **Tables:** `puzzles` (two rows: false-walk lore puzzle, true-walk main_beat puzzle; answers =
  destination words, NOT coords), `arc_state` flags (Iss-caught gates the re-carve), `thread_cards`
  (`card_kind`: the false walk → `contradicted`; the true walk → `verified`), `answer_attempts`
  (audit). All existing.
- **Outcome types:** false walk = `lore`; true walk = `main_beat` (enqueues the Accepting on-ramp
  beat). Both via the existing `applyOutcome` path in `resolve.ts`.
- **Voice keys to ADD** (Set-B, in `voice.ts`, read by key):
  `voice.dest.coordFraming.false`, `voice.dest.coordReCarve`, `voice.dest.coldHearth.find`,
  `voice.dest.threshold.arrive`. (Mirrors the `voice.dest.*` convention already drafted in
  `travel-destinations.md`.)

---

## 5. PLANT THE PAYOFF — the "oh, THAT is what that was for"

- **PLANT (Movement I, inert/ambiguous).** Near `stone_of_reckoning`, a single carved number-pair
  the group **cannot yet read as a place** — it looks like one more tally in a counting-glyph world
  (the Hold counts everything: heads, loaves, winters). A Survey post beside it reads (Set-B):
  *"the record keeps roads, not only words."* At M1 this is a throwaway; it reads as lore texture.
- **DORMANT (Movement II).** The group learns the digit-glyphs at the Rosetta to solve *other*
  clues. The M1 number-pair is still just sitting there, now faintly legible but contextless.
- **PAYOFF (Movement III).** The moment Iss's stone yields a coordinate in the **same glyph family**,
  the M1 number-pair **snaps into focus**: it was a coordinate all along — the *first* place the
  record ever pointed at, carved before anyone could read it. Walking it (optional) reaches an early,
  quiet tableau that the M1 Survey post was literally describing ("roads, not only words").
- **DOUBLE PAYOFF (Movement IV).** When the Liar is caught and the *true* coord appears, a sharp
  group re-checks the M1 pair and finds it matches **neither** of Iss's two coords — it was the
  record's own road, never Iss's. The plant proves the record was pointing at places **before Iss
  ever lied**, quietly supporting FACT 12 ("the kept ones did not depart; they were kept" — the
  record keeps roads to where the kept ones are). No plant without payoff; no payoff un-planted.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| | |
|---|---|
| **Lives in** | M3 (spine event: decode→walk), with the plant in M1, false walk in M2, true walk in M4, endpoint in M5. |
| **Depends on** | `stone_of_reckoning` placed + literacy taught (digit-glyphs/sign marks); `coordEncode` (built); the Liar/Vigenère catch mechanic (built, canon §4); sites `stone_iss` / `the_cold_hearth` / `the_threshold` placed at world-build. |
| **Depended on by** | the Accepting on-ramp (true walk delivers the group to `the_threshold`/`unbroken_light` for M5); the Liar payoff's *physical* form. |
| **Priority** | **P1 (arc-spine).** It is the M3 tempo change and the legs of the Liar→Accepting hinge. NOT P0 (the vertical slice can prove the loop with a sit-and-decode clue first; the walk is the second-pass thrill). The optional Seventh-Stone coord and the M1 double-payoff plant are **P2 (depth)**. |

**Scope discipline (restated):** exactly **two** expedition coords (Iss false + true) ship as P1;
the Seventh coord is P2-optional. Do not multiply walks. The minus-sign law (§0/§2.A, INV-COORD) is
non-negotiable: **the coordinate is the clue; a clean destination word is the answer.**

---

## 7. ADDENDA (build hardening — fold into the build pass)

### 7.1 The Movement-II tutorial hop (teach the verb cheap, before it is load-bearing)
Before the M3 expedition matters, the *form* "carved number = a place you walk to" must be learned at
zero stakes. Add one **short, near** coordinate (200–400 blocks) at `keeper_stone_01` decoded right
after the Reckoning literacy lands — a tutorial walk to a tiny tableau. It teaches the answer verb too:
the plaintext submitted is the **destination's name**, never the digits (§2.A). This makes the M3 set-
piece and the M4 Iss betrayal legible rather than confusing on camera. Cheap, ignorable by veterans,
load-bearing for the spine. Keep it P1 (the climax coordinate inherits this taught verb).

### 7.2 Two seed-check assertions that make the two fatal risks un-trip-over-able
Both are pure, build-time guards (no runtime cost), wired so a bad seed fails the build, not the player:
- **`coordAnswerGuard()`** (add to `discord/src/forge/seedcheck.ts`): assert NO puzzle whose clue is a
  `coordEncode` carving has an `answers` entry that normalizes to a bare number / signed coordinate
  (i.e. matches `/^[a-z]?\s*\d/` after `normalizeAnswer`). This makes INV-COORD (§4) mechanically
  enforced — a designer literally cannot ship a coordinate-as-answer regression.
- **Extend `siteCoverageSelfTest`** (forge/canon.ts): a coord-bearing puzzle must not be seeded OPEN
  unless its destination `site_id` resolves to a **placed + enabled** site (non-null coords). This kills
  §2.4-style dead air (decode → walk to an unplaced cell → nothing there → a bug wearing a dead lead's
  clothes). Pairs with the existing "coord rows inactive until `stone_of_reckoning` is placed" gate.

### 7.3 FACT 16 tie-in (deepens the plant, no new sealed fact)
The §5 plant ("the record keeps roads, not only words") is the surface of **FACT 16** ("the record files
the living by *place*, not only by name"). Thread the M1 Survey post and the III arrival so the
expedition is FACT 16's mechanical body: the record had your *ground* before you had the walk. This is a
FORESHADOW→REVEAL path already in the web — wire to it, do not author a competing fact. (Optional carve:
the III arrival can fire the `name-where-never-been` `SignWriteBeat` at a `carve_anchor_*` cell the group
provably reached for the first time on that very walk — FACT 16's sharpest reveal, collective/chorus,
never a single callout.)

### 7.4 New sites.yml entry for the III marquee set-piece (if not reusing a canon site)
If the M3 tableau is its own place (not the cold hearth / Threshold), add ONE entry: `watch_post_01`
(type `structure`, FAWE paste target, null-coord placeholder, `enabled: true`) so the site-coverage
self-test resolves it. Otherwise reuse `the_cold_hearth` (false) and `the_threshold` (true) as the two
canon endpoints — no new site needed for the minimal P1 build.
