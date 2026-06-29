# Backlog idea — The Six Keeper-Stone Expeditions (Movement II spine)

> Build-ready design treatment. Reconciles MASTER-PLAN P1.10 (backlog item #10) and
> FINAL-REPORT against the NEW "six different verbs + a day-one meta-acrostic" framing,
> and specifies the genuinely-unbuilt remainder. Honors what already compiles
> (`clue-specs.ts` CLUE_SPECS, `forgeClue`, the seed, `specsSelfTest`). Operate inside
> `D:/the-observance` only.

---

## 0. TL;DR for the synthesis phase

**Verdict: KEEP-SCALED.** The five built+bound cipher stones (Vaun/Mara/Sella/Orin/Iss)
already *are* five different ciphers with five different verbs and they round-trip under
`specsSelfTest` — do NOT touch them. The genuinely-unbuilt remainder is three things:
(1) the **sixth stone (Brann)** as a real cipher+verb instead of flat lore; (2) the
**meta-acrostic plant**, which CANNOT be the six bound plaintexts' first letters (they
spell `G D S I T ·` — nonsense, and the plaintexts are frozen); it must live in a
**second, additive surface** (the keeper-stone *titles/headers*, the six framing
maker-marks, or a six-glyph token set) so it plants without re-opening any frozen string;
(3) the **frame-rotation verb** (`ItemFrame` dials), which has **no beat class** today.

---

## 1. EXPOUND — the full mechanic + story + mystery treatment

### 1.1 What it is, in one sentence
Movement II is six expeditions through the mid-Hold, one per keeper-stone, each a
*different kind of reading* — so the group learns not one trick but a literacy: shift,
mirror, name-key, plain-script-by-stooping, walk-a-shelf, read-by-time. Six doors open
at once (cipher-web §2.2); which one a group cracks first is free. The payoff of doing
*all six* is the day-one-plantable meta layer: a seventh thing spelled across the six,
inert until you have all six fragments and re-read them as a set.

### 1.2 The six stones as they actually exist today (FROZEN — do not re-author)
From `clue-specs.ts` CLUE_SPECS + `cipher-plaintexts.md` (bound, `specsSelfTest`-guarded):

| stone | cipher (built) | answer-verb (the physical act) | bound plaintext (frozen) | site |
|---|---|---|---|---|
| `stone-vaun` | caesar shift 3 | turn the wheel / read the threes header | GIVE THE FIRST OF THE DEEP BACK TO THE DEEP | `stone_vaun` (cairn, head-on light) |
| `stone-mara` | book cipher | **walk the six-book lectern shelf** L→R | DESCEND AND BOW AT THE UNBROKEN LIGHT | `stone_mara` (`kept_light_home_01`) |
| `stone-sella` | atbash | **read the reflection** (stand behind, face water) | SOUTH BY THE FAR WATER WHERE SHE DID NOT COME BACK | `the_far_water` |
| `stone-orin` | substitution | **crouch to read** the floor-facing run | I THOUGHT IT SMALL IT WAS NOT SMALL | `the_threshold` |
| `stone-iss-wall` | vigenere key=ISS | **key on his name, then on the others** | THE ONE WHO TURNED AWAY | `stone_iss` (warm-lit bait) |
| `stone-brann` | **railfence (UNBUILT)** | **read by time** (night/black-moon beacon) | *(to author — see §1.4)* | `stone_brann` |

That is already five distinct ciphers and five distinct verbs. The "six different verbs"
idea is **80% satisfied by what compiles.** The new framing's value is (a) forcing the
sixth (Brann) to be a real cipher, and (b) hanging a meta-acrostic across the set.

### 1.3 The new framing's "six verbs" — reconciled to the verb menu
P1.10 lists `caesar→rotate frames, book→walk shelf, atbash→reflection, vigenere→keyed
name, substitution→fill a sign, coord→travel`. Mapped against built reality:
- **book→walk a shelf** ✓ (Mara, `LecternFillBeat` places the six books).
- **atbash→read a reflection** ✓ (Sella, `the_far_water` build).
- **vigenere→keyed on a keeper name** ✓ (Iss).
- **substitution→fill a sign** ✓ (Orin's plain-script run IS a `SignWriteBeat` carving;
  "fill a sign" is the verb of submitting the read).
- **caesar→rotate item-frames** is the ONE verb with **no beat** (§3, the only real Java
  gap). Today Vaun's verb is "read the threes header and turn the wheel" — a *cognitive*
  rotate, not a physical frame dial. **Decision (§2): keep Vaun's caesar as-is; make the
  physical frame-rotation a SEPARATE optional surface** (a rune-dial ring at the Stone of
  Reckoning) rather than retrofitting Vaun, so no frozen node is disturbed.
- **coord→travel** is already the Sella `side_quest` bearing + `iss-dead-shrine` travel +
  the Movement III/IV true-coordinate (`coordEncode`, built). It is a verb of the web, not
  a sixth stone. Brann's sixth verb is therefore **read-by-time**, which is the genuinely
  missing modality (P13) and the right home for the sixth distinct cipher.

### 1.4 The sixth stone — Brann, the unbuilt one (the real build remainder)
`stone-brann` ships today as flat `lore` (NON_CIPHER_KEYS: *"ships as flat lore today;
not forgeable until P0-5 re-authors it as the railFence/beacon node"*). To make Movement
II actually six expeditions, author it as:
- **Cipher:** `railFence` (built, self-tested), rails = the fire-count Brann names in
  `D08` (*"i counted the fires tonight"*). The rail count is a **counted in-world
  quantity** (lit beacons/campfires on his walk) — cross-references P12.
- **Verb:** *read-by-time.* The carving is cut so shallow it vanishes in daylight; only
  the lit beacon-glow after dark rakes it visible (`cipher-plaintexts.md` already specs
  this hand). Gate via the moon/time check (`fullTime/24000 % 8`, DESIGN §2.4).
- **Bound plaintext (to author, must round-trip + normalize into its seed row):** a
  watchman's line in register — e.g. `COUNT THE FIRES BEFORE YOU SLEEP` or
  `THE DARK HOURS ARE KEPT BY THE LAST LIGHT`. Final string chosen at build so its first
  glyph serves the meta-acrostic (§5) and `specsSelfTest` passes.
- This single re-author **un-orphans `railFence`** (COHERENCE-AUDIT B1) AND completes the
  six-verb set AND gives the meta-acrostic its sixth letter. One move, three payoffs.

### 1.5 How it plays across the ~2-week / 5-movement arc
- **Movement I (Notice):** the Rosetta (`rune_rosetta`) + Stone of Reckoning teach the
  script and the digits. The six stones exist but most are `active:false` / unlit. The
  day-one meta-acrostic seed is *physically present* on the stones that are placed, but
  reads as decoration (§5).
- **Movement II (the Keeper-Stones — the spine):** all six expeditions live here, open
  together. A group splits up: someone stands behind Sella's stone reading the lake;
  someone crouches under Orin's lintel; someone waits out the night for Brann; someone
  walks Mara's shelf. Each solved stone fires its reveal beat (a `SmallStructureBeat`
  paste, a `lore` fragment, a `next_clue`). The Iss stone is the warm trap that seeds
  Movement IV. **No countable order** — the resolver ignores `movement` (ORACLE.md §8).
- **Movement III (Undercroft / Seventh):** ≥4 of 6 stone fragments + a coordinate opens
  the descent. The Seventh thread (optional) and the meta-acrostic re-read pay off here:
  the group, holding all six first-glyphs, reads the seventh word.
- **Movement IV (the Catch):** Iss's warm door is revealed as a wall; the meta-acrostic's
  seventh word recontextualizes (§5 payoff).
- **Movement V (Accepting):** the rite. The six personal tokens (FACT 13) echo the six
  stones — one token *per keeper*, the group giving back what each keeper withheld.

### 1.6 Why it is camera-worthy
Six genuinely different *acts* (stoop, turn to water, wait for dark, walk a shelf, turn a
key on a friend's name) read on video as six distinct discoveries, not six text-boxes.
The meta-acrostic is the classic "wait — go back and look at all of them" beat that an
ARG-video thrives on.

---

## 2. CRITIQUE — adversarial, honest

**R-1 (sharpest). The meta-acrostic cannot ride the bound plaintexts.** The six bound
plaintexts begin `G D S I T (·)` — not a word, and `specsSelfTest` freezes every letter.
Any "first-glyph acrostic" plan that assumes you can tune the plaintexts is **dead on
arrival.** → *Mitigation:* the acrostic plants on an **additive surface** the forge does
not guard — the per-stone **header/title line** in the framing (the plain-script maker
line cut above each ciphered run, which is free authored prose per `cipher-plaintexts.md`)
OR a six-glyph **token set** (one rune token per stone, collected). This keeps the plant
day-one-visible and orphan-free without touching a frozen string. **This is the load-
bearing decision; everything else is downstream of it.**

**R-2. `caesar→rotate item-frames` has no beat class.** There is no `ItemFrameBeat`; the
verb menu's `ItemFrame.getRotation()` arrangement listener is unbuilt. → *Mitigation:*
do NOT retrofit Vaun. Either (a) ship Vaun's caesar with the cognitive "turn the wheel"
verb it already has and treat frame-rotation as P2 depth, or (b) build a small
`FrameDialListener` (§3) for ONE optional rune-dial set-piece. Recommend (a) for the
vertical slice; (b) is genuine added craft but not spine-critical.

**R-3. Brann is the only un-author'd stone, and night-gating risks "nothing happens on
camera."** A stone that only reads at night can read as a bug to a daytime group. →
*Mitigation:* (i) the watcher/Brann's own framing must *tell* you it reads at night
("the dark hours are kept by the last light"); (ii) give it a ≥2nd in-door per the web
rule (the rail count is also obtainable by counting beacons in daylight, so the *key* is
day-fair even if the *reading* is night-gated); (iii) never let it gate Movement III
alone (≥4 of 6 means a group can skip Brann entirely).

**R-4. Six simultaneous stones risk feeling like a checklist (anti-ladder law).** Six
nodes with a visible "4 of 6" counter is exactly the countable ladder the laws ban. →
*Mitigation:* never surface a count to players; the `fragments>=4` gate is server-side
only (cipher-web §2.3). The stones are scattered across Warrens/Market/Lamp-works (not a
row), red-herrings (the D04 acrostic dead-end, the even-count) sit among them, and lore
nodes pay off without advancing. The group never knows "how many left."

**R-5. Precision law risk on the meta-acrostic.** If the meta-word is delivered as a
Watcher "it knows you" callout it would be a personalization on an unmeasured signal. →
*Mitigation:* the meta-acrostic payoff is **world/lore**, not a profiling callout — it is
a word the *group* assembles, delivered as a `lore`/`main_beat` reveal, never as "you,
[name], spelled this." No precision violation because no per-player claim is made.

**CUT recommendation:** cut the literal `caesar→rotate frames` retrofit of Vaun from the
spine (move to P2). Cut nothing else. Keep-scaled = five frozen stones + Brann authored +
acrostic relocated to the additive surface.

---

## 3. THE GENUINELY-UNBUILT REMAINDER (precise build list)

1. **`stone-brann` re-author (content + seed).** Add a CLUE_SPECS entry (`railFence`,
   rails=fire-count, bound plaintext per §1.4), move `stone-brann` OUT of
   `NON_CIPHER_KEYS`, flip its seed row from `lore` to a `next_clue`/`lore` cipher node,
   add a night gate. `specsSelfTest` + `specsCoverageSelfTest` must stay green (the
   disjointness guard will catch a half-move). **Files:** `discord/src/forge/clue-specs.ts`
   (CLUE_SPECS + remove from NON_CIPHER_KEYS), `discord/supabase/seeds/puzzles_seed.sql`,
   `arc/corpus/cipher-plaintexts.md` (replace the "NOT a cipher" Brann note with the bound
   block), `arc/lore/documents/D08` framing.
2. **Meta-acrostic plant (content only, no frozen-string change).** Author the six
   additive header/maker-mark lines (or six rune-tokens) whose first glyphs spell the
   seventh word. **Files:** `arc/corpus/cipher-plaintexts.md` (the free framing lines per
   stone), a new sealed note `arc/cipher-web-seed.sealed.json` entry for the meta-word's
   payoff row. NO change to any BOUND PLAINTEXT.
3. **The meta-acrostic payoff row (seed).** One `puzzles` row keyed e.g.
   `m2-six-stones-acrostic` (kebab), `accepted_answers=[<the seventh word, normalized>]`,
   `outcome_type:'lore'` or `'main_beat'`, `active` flipped on in Movement III. Add to
   `NON_CIPHER_KEYS` (it is an observation, not a carved cipher) with a reason, so
   `specsCoverageSelfTest` passes. **Files:** `puzzles_seed.sql`, `clue-specs.ts`
   (NON_CIPHER_KEYS), `design/clue-web.md` (node + ≥2 in-doors).
4. **(P2, optional) `FrameDialListener` + frame-dial set-piece** for the literal
   rotate-verb. A new arrangement listener reading `ItemFrame.getRotation()` on a fixed
   ring of frames → posts a plugin sentinel to the resolver (like the rite, §P15/§5
   pattern). **Files:** new `plugin/.../signal/FrameDialListener.java`, a `sites.yml`
   entry, a `NON_CIPHER_KEYS` sentinel row.

**Reused, already built (do NOT rebuild):** `forgeClue` + all 11 ciphers; `SignWriteBeat`,
`LecternFillBeat`, `SmallStructureBeat`, `MapMarkBeat`; the CLUE_SPECS bind +
`specsSelfTest`; the seed; `coordEncode` digit-glyph scheme.

---

## 4. DE-SLOP TEST — exemplar lines (cold, plain, concrete)

Brann's stone, night-only framing (watchman register; no melodrama, no announced dread):
> `I COUNTED THE FIRES. NINE LIT, ONE OUT. I RELIT IT. I WILL COUNT AGAIN BEFORE I SLEEP.`

Watcher line foreshadowing the meta-acrostic (it counts; it does not threaten):
> `Six stones are read. The first marks of each were also a mark.`

The acrostic payoff reveal (group assembles the seventh word — lore register, no bow):
> `You set the six first marks in a row. They spell a name that is not on any stone.`

Vaun's threes-header (hands the shift without ever saying "three"):
> `IRON THREE · SALT THREE · GRAIN THREE · I HELD THREE OF EACH AND THE COUNTING WAS WARM`

(All pass: concrete counts, no "testament/fabric/little did they know," no named emotion,
no tidy bow, plain declarative keeper voice.)

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" seed

**The plant (Movement I, inert):** each of the six stones carries a plain-script
**maker-header** above its ciphered run (already canon in `cipher-plaintexts.md` as free
framing — e.g. Vaun's "IRON THREE…", Mara's "READ THE SHELF…", Sella's "I WRITE IT THE
ONLY WAY IT READS…"). These read on day one as ordinary keeper notes. Their **first
glyphs**, taken in keeper-canonical order (the Rosetta sunwise order, NOT discovery
order), spell a **seventh word** — a name the corpus keeps for what the stones are
*for*. It is visible from the first hour and means nothing until you have all six.

**The payoff (Movement III, on re-read):** once all six headers are decoded (which only
happens after the group is rune-literate AND has visited all six scattered stones), the
group sets the six first-marks in a row and the seventh word appears — delivered as a
`lore`/`main_beat` reveal (`m2-six-stones-acrostic`). It pays off the day-one decoration
("the headers were a cipher the whole time") AND threads into Movement IV: the seventh
word is the thing Iss's warm wall was built to stop you spelling.

**Discipline:** no plant without payoff — the headers are authored to the meta-word.
No payoff without plant — the `m2-six-stones-acrostic` row is `active:false` until the
headers are placeable (Movement I build), so it can never fire un-planted. The meta-word
is chosen to be normalize-clean and to begin with letters the six free headers can be
re-authored to supply (none of the six bound plaintexts moves).

---

## 6. THREAD IT (consistency / no-orphan enumeration)

**Canon-spine FACTs it touches/adds:**
- Touches **FACT 3** (prior keepers' customs) — the six stones ARE the prior keepers.
- Touches **FACT 5** (each keeper's fate matches the custom broken) — each stone's verb
  encodes its keeper's flaw (Vaun hoards→withholding shift; Orin won't bow→crouch verb;
  Iss turns away→name-key betrayal; Sella drifts→reflection).
- Touches **FACT 7/8** (Iss's lie/wall) — the Iss stone is the warm trap.
- **Adds a small foreshadow node** for the meta-acrostic under FACT 15 (the watching
  accumulates) — the seventh word is an accumulation reveal, not a blurt.
- Touches **FACT 11/12** (the one fire / "the kept did not depart") — Brann's
  unbroken-light night-watch.

**Found-documents that must mention/foreshadow it:** `D02` (Vaun), `D05` (Mara), `D06`
(Sella), `D07` (Orin), `D08` (Brann — now a cipher source, not flat lore), `D09` (Iss).
The meta-acrostic gets a one-line foreshadow in `D01`/`the-record-opens` ("the marks
before the marks were also kept").

**NPC/Watcher voice lines:** new `voice.ts` keys — a Brann night-gate line, a
meta-acrostic foreshadow line, and the acrostic reveal line (all `oracleLore`/
`oracleMainBeat` payloads, structured args only — no English in payloads, ORACLE.md §7).

**Ciphers/puzzles expressed (reuse built):** `caesar` (Vaun), `book` (Mara), `atbash`
(Sella), `substitution` (Orin), `vigenere` (Iss), `railFence` (Brann — un-orphans it).
Meta-acrostic uses the **acrostic reading convention** (P8, NO CODE — an observation).

**Beat classes / listeners / tables / seed rows / sites / voice keys:**
- Beats: `SignWriteBeat`, `LecternFillBeat`, `SmallStructureBeat`, `MapMarkBeat` (all
  built). New (P2 only): `FrameDialListener`.
- Tables: `puzzles` (the Brann row + the `m2-six-stones-acrostic` row), `solves`,
  `arc_state.flags`.
- Seed: `puzzles_seed.sql` (Brann cipher row; acrostic row); sealed payoff in
  `arc/cipher-web-seed.sealed.json`.
- `sites.yml`: `stone_brann` coords (already defined, placeholder); optional frame-dial
  site.
- Symbols: `CLUE_SPECS` += `stone-brann` entry; `NON_CIPHER_KEYS` -= `stone-brann`,
  += `m2-six-stones-acrostic` (+ frame-dial sentinel if P2).

---

## 7. MOVEMENT PLACEMENT + DEPENDENCIES

- **Lives in:** Movement II (the spine), with the meta-acrostic payoff in Movement III
  and recontextualization in Movement IV.
- **Depends on:** P0.1 (rune font on signs), P0.3 (resource pack), P0.4 (the seed +
  oracle loop), P8 (the showrunner/active staging), the Rosetta + Stone of Reckoning
  builds (literacy + digits), the FAWE schematic branch (R5) for the stone set-pieces.
- **Depended on by:** the Liar engine (P1.11 — Iss stone is its entry), the Undercroft
  gate (≥4 fragments), the Accepting (six tokens echo six stones).
- **Priority:** the five frozen stones are **P0/P1 (already built, arc-spine)**; the
  Brann re-author + meta-acrostic plant are **P1 (arc-spine, the genuinely-unbuilt
  remainder)**; the `FrameDialListener` rotate-verb is **P2 (depth, optional)**.
