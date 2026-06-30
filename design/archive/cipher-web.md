# The Observance — THE CIPHER WEB (design, spoiler-free)

> Safe to read. This is the **puzzle-mechanics companion** to `design/clue-web.md`
> (the narrative map). The two are COMPLEMENTARY, not duplicates: `clue-web.md` is
> the movement-by-movement node list + edge map + dead-end/side-quest inventory;
> **this file is the cipher catalog** — the modalities, the graph shape, the fairness
> contract, and the authoring checklist. **Both files share ONE puzzle_key namespace:
> the authored kebab-case keys** (`stone-vaun`, `no-wall-catch`, …) that the canonical
> seed `discord/supabase/seeds/puzzles_seed.sql` inserts. This file no longer carries
> its own competing seed (an earlier draft did, in a `clue_<cipher>_<id>` namespace —
> that has been retired in favor of the one canonical seed; see §6).
>
> It does **not** state the ending. Where a payoff is spoilery it is named only by its
> mechanical effect (a flag, a `next_puzzle_key`) and the prose lives sealed in
> `arc/cipher-web-seed.sealed.json`. Read alongside `design/clue-web.md` (the map),
> `design/clue-web-seed-notes.md` (puzzle_key → which lore doc teaches it),
> `discord/ORACLE.md` (the resolver contract), `discord/src/forge/ciphers.ts` +
> `runes.ts` (the deterministic forge), `discord/supabase/migrations/0004_oracle.sql`
> (the schema this is poured into), and `arc/lore/found-documents.md` (the 12 documents
> this web cross-references).
>
> **North-star for this layer:** solving must feel like *reading the world*, not
> *clearing a checklist*. No countable ladder. Many doors open at once; some doors
> are walls the watcher will call by name; the same key fits more than one lock.
>
> **STATUS NOTE (the forge is ahead of an earlier draft of this doc).** The forge
> already ships **11** pure, round-trip-self-tested transforms in `ciphers.ts`
> (`caesar, atbash, vigenere, substitution, bookCipher, coordEncode, railFence,
> columnar, polybius, a1z26, morse`), all wired into `forgeClue` (`index.ts`), and
> `coordEncode` is **already the fixed digit-glyph scheme** (`X-1280,Z64`). So the
> "[NEW CODE] add rail-fence/columnar/polybius" and "fix coordEncode" items below are
> **already DONE in code** — they are retained as catalog descriptions of *how each
> transform works*, not as build tasks. The only genuinely-unbuilt cipher item is the
> steganography modality (P17). See `design/MASTER-PLAN.md` R1/R4 for the full audit.

---

## 0. The three laws this web obeys (the fairness contract)

Every puzzle in this catalog is checked against three laws. A puzzle that fails any
one is not authored.

1. **DETERMINISTIC FORGE.** The clue artifact is produced by code in `ciphers.ts` /
   `runes.ts` (or by a curated in-world placement), and is *decodable by code*:
   `decode(encode(plaintext, key), key) === plaintext`, proven by `runSelfTests()`.
   The LLM never makes a cipher; it only writes (rarely) the *prose around* a clue.
   API down ⇒ every puzzle here still solves.

2. **IN-CORPUS KEY (HARD-but-FAIR).** Every key a player needs is *findable inside
   the world* — a Rosetta stone, a keeper's name, a lore document, a count of real
   in-world markers. **No outside knowledge.** "Hard" means the key is buried, cross-
   referenced, or earned; never that it requires a wiki. The Whisper economy
   (`FLOW §3`) is the rationed, diegetic backstop for a genuinely-stuck group.

3. **NORMALIZED ANSWER.** The plaintext a player submits resolves through the one
   `normalizeAnswer` (`ORACLE.md §2`: NFKC → lower → `[^a-z0-9 ]→space` → collapse →
   trim) on **both** surfaces. Every `accepted_answers` entry in this doc is written
   *already normalized*. Coordinates therefore lose their minus sign on
   normalization, so coordinate answers are authored **unsigned**, or both signed and
   unsigned forms are listed (see §4 / the coordEncode fix in §6).

> The resolver does **not** gate on `movement` (`ORACLE.md §8`: out-of-order is
> allowed by design). `movement` in every row below is an **authoring/visualization**
> field only — it tells the dashboard where a node lives on the web, and tells the
> showrunner what to stage with `active`. Difficulty comes from *finding the key*,
> not from a server-side lock.

---

## 1. THE PUZZLE CATALOG — the modalities

The forge (`ciphers.ts`) ships **11** transforms today, all wired into `forgeClue`:
`caesar`, `atbash`, `vigenere`, `substitution`, `bookCipher`, `coordEncode`,
`railFence`, `columnar`, `polybius`, `a1z26`, `morse`. This catalog describes all of
them plus **6 non-cipher puzzle modalities** that exploit the medium (in-world
observation, timing, sound, arrangement, ritual, steganography). Each entry states:
**FORGE** (how the clue artifact is made), **SOLVE** (how a player turns it into
plaintext), **ANSWER → SCHEMA** (how the plaintext maps to `puzzles.accepted_answers` +
which resolver path), and **KEY SOURCE** (the in-world Rosetta that makes it fair).

Markers reflect **current code reality**: **[EXISTS]** = shipped + self-tested in
`ciphers.ts`; **[NO CODE]** = no cipher, forges as an in-world placement (a beat) or an
existing artifact and resolves as a plain `accepted_answers` match; **[BONUS — UNBUILT]**
= the one genuinely-unwritten cipher item (P17 steganography). The transforms an earlier
draft tagged "[NEW CODE] add …" (rail-fence/columnar/polybius) are now **[EXISTS]** —
they were built; a1z26 and morse exist too and are two free extra "early/teaching" rungs
the original catalog didn't mention.

### — CIPHER FAMILY (text transforms, carved as runes) —

#### P1. Caesar shift `[EXISTS]` — *Vaun's cipher (the fixed withholding)*
- **FORGE.** `forgeClue({cipher:'caesar', text, shift})` → ciphertext carved as runes.
- **SOLVE.** Turn the wheel `shift` marks back. The shift is taught by Vaun's ledger
  (`D02`): his tallies are always "three of each," a constant held-back amount — the
  shift *is his hoarding made literal*. A Rosetta sign near his stone states the wheel.
- **KEY SOURCE.** Vaun's ledger constant + the Rosetta wheel sign. The number is
  small (3, 7) and in-corpus.
- **ANSWER → SCHEMA.** plaintext (uppercased upstream; normalized on submit) →
  `accepted_answers`. Any `outcome_type`.

#### P2. Atbash / mirror `[EXISTS]` — *Sella's cipher (read only in reflection)*
- **FORGE.** `forgeClue({cipher:'atbash', text})`. Involution: encode === decode.
- **SOLVE.** Mirror A↔Z. **Physical verb pairing:** the clue card / in-world carving
  reads as folded nonsense until *faced toward water* — Sella speaks "only as a
  reflection" (`D06`). The mirror is both the cipher and the place.
- **KEY SOURCE.** The cipher is self-describing once you know it is a mirror; `D06`'s
  own framing ("backwards, to the water") teaches it. No external key needed — but the
  **act** (stand at the shore pool, face the water) is the gating ritual.
- **ANSWER → SCHEMA.** Atbash plaintext → `accepted_answers`. Often `next_clue`
  (hands off a bearing) or `side_quest` (the Seventh thread).

#### P3. Vigenère (keyed) `[EXISTS]` — *Iss's cipher (the key is a name, and the name is a lie)*
- **FORGE.** `forgeClue({cipher:'vigenere', text, key})`.
- **SOLVE.** Lay the key word over the marks. **The key is `ISS` — his own name**
  (`D09`). Applied to *his own* letter it reads warm; applied against the *other*
  stones it yields the word the corpus uses for "the one who turned away."
- **KEY SOURCE.** `D09` hands the key in-fiction ("the key is my own name, as is right
  and customary"). This is the load-bearing red-herring engine (see §3, the Liar door).
- **ANSWER → SCHEMA.** Two distinct rows share the key:
  - the **warm** plaintext → a `next_clue` that sends them to the *dead shrine* (a
    real place, wrong door);
  - the **name-as-key** plaintext (`the one who turned away`) → a `main_beat` that
    sets `flags.iss_caught` and flips his dialogue tree (see §3).

#### P4. Substitution (the rune alphabet) `[EXISTS]` — *Orin's cipher (the plain script, plainly withheld)*
- **FORGE.** `forgeClue({cipher:'substitution', text})` → each letter to its rune glyph
  via `RUNE_MAP`. The "ciphertext" is the carved runes themselves.
- **SOLVE.** One mark, one letter — using the **Rosetta** (`D03`, the server-icon rune
  ring; see P11/§6). This is the literacy that unlocks reading the whole world.
- **KEY SOURCE.** The Rosetta ring (`D03`) — the master key for every later stone.
- **ANSWER → SCHEMA.** Decoded plaintext → `accepted_answers`. Frequently `lore`
  (Orin only speaks when crouched, so his payoff is a told fragment) or `next_clue`.

#### P5. Book cipher (page · line · word) `[EXISTS]` — *Mara's cipher (the map, never the tool)*
- **FORGE.** `forgeClue({cipher:'book', text, book})` → `(page,line,word)` triples,
  carved as runes (digits exist in the script). The "book" is a **real lectern shelf**
  walked in order; `D05` is the authored instance (six books, six refs).
- **SOLVE.** Walk the shelf; from each book take the named page→line→word; set the words
  in a row. `D05`'s six refs resolve to `DESCEND AND BOW AT THE UNBROKEN LIGHT`.
- **KEY SOURCE.** The lectern books themselves (the in-world "book"). The refs are on
  the clue; the words are in the world.
- **ANSWER → SCHEMA.** The assembled sentence → `accepted_answers`. Note Mara's twist:
  her own text warns *"do not write the sentence on a sign and think it answered — do
  the thing it tells you."* So her `next_clue` row's `next_puzzle_key` points at a
  **ritual** puzzle (P16) — solving the book cipher only *names* the rite; performing it
  is the real door.

#### P6. Coordinate encode `[EXISTS — already the fixed digit-glyph scheme]` — *the cross-surface handoff*
- **FORGE.** `forgeClue({cipher:'coord', coord:{x,z}})` → `X<digits>,Z<digits>` carved
  in the script's **digit glyphs** (e.g. `(x=-1280, z=64)` → `X-1280,Z64`). This is the
  fixed scheme already in `ciphers.ts` — it reads as a real coordinate, not a word.
  (§4 below documents the fix and the in-world **Stone of Reckoning** Rosetta that
  teaches the digit/sign marks; the *code* half is DONE, the *world placement* half is
  the remaining content task.)
- **SOLVE.** Read the digit-runes as a number using the keeper-stone key (§4).
- **KEY SOURCE.** The **coordinate-stone Rosetta** (§6) — a found artifact that carves
  the digit glyphs 0–9 beside their plain numerals.
- **ANSWER → SCHEMA.** `"<x> <z>"` (authored unsigned per §4) → `accepted_answers`.
  Almost always `next_clue` or `main_beat` (a place to travel to, then a beat there).

#### P7. Rail-fence / transposition `[EXISTS]` — *Brann's cipher (the path that zig-zags the dark)*
- **FORGE.** `forgeClue({cipher:'railfence', text, rails})` → `railFence.encode` writes
  the letters down `rails` diagonals and reads off row by row; decode reverses it. Pure,
  reversible, self-tested (`decode(encode(t,r),r)===t`). Carves the transposed letters as
  runes.
- **SOLVE.** Reconstruct the fence (number of rails = the count Brann names —
  *"i counted the fires tonight"*, `D08`). No new alphabet; just re-ordering, which
  *feels* different from every substitution because the glyphs are all familiar but the
  reading order is wrong.
- **KEY SOURCE.** The rail count is a **counted in-world quantity** (lit campfires on
  Brann's walk, the number of keeper-stones, etc.) — cross-references the observational
  modality P12.
- **ANSWER → SCHEMA.** plaintext → `accepted_answers`. Pairs with a **night/black-moon
  gate** (P14): Brann's page is illegible by day, so this puzzle only *reads* at night.

#### P8. Keyed columnar / "first-letters" acrostic `[columnar EXISTS; acrostic NO CODE]` — *the record's hidden hand*
- **FORGE.** Two flavors:
  - **Acrostic** is a *reading* convention, not a transform (`[NO CODE]`): the author lays
    lines whose **first glyphs spell** the answer; decode = take initial letters. (It is
    forged as an authored placement, not via `ciphers.ts`; mint a hand key in the kebab
    namespace.)
  - **Columnar** (`[EXISTS]`): `forgeClue({cipher:'columnar', text, key})` →
    `columnar.encode` reads columns in the alphabetical order of the keyword, reversible +
    self-tested.
- **SOLVE.** Read the first mark of each carved line top-to-bottom (acrostic), or
  reorder columns by the keyword (columnar). The acrostic is devilish in a *document*
  whose surface text is innocuous — the record hides a name in plain ledger lines.
- **KEY SOURCE.** Acrostic needs no key (it's an observation — fair-but-hard because you
  have to *notice*). Columnar keyword is a custom-name or keeper-name from the corpus.
- **ANSWER → SCHEMA.** the spelled word → `accepted_answers`. Great for `dead_end`
  texture (a true hidden name that opens nothing) or `lore`.

#### P9. Polybius / grid-coordinate cipher `[EXISTS]` — *the marker-grid*
- **FORGE.** `forgeClue({cipher:'polybius', text})` → `polybius.encode` maps each letter
  to a `(row,col)` pair on the 5×5 **keeper-square** (I/J merged, the classic rule) and
  emits the digit-pairs, carved as rune digits. Pure + reversible + self-tested.
- **SOLVE.** Each pair indexes the square. The **square is laid out in the world** — a
  5×5 (or 6×6) grid of marker-stones, each carrying one glyph (a `small_structure` /
  `sign_write` field). Reading a pair = walking to grid cell (row,col) and reading its
  glyph.
- **KEY SOURCE.** The physical marker-grid *is* the square — a beautiful "the world is
  the codebook" moment. Cross-references P12 (count/observe) and P10 (map).
- **ANSWER → SCHEMA.** decoded plaintext → `accepted_answers`. Often `next_clue`.

#### P10. Map / spatial-coordinate puzzle `[NO CODE — uses MapMarkBeat / coordEncode]`
- **FORGE.** `MapMarkBeat` (`beats/lib/MapMarkBeat.java`) gives the group a **filled
  map** with a mark/cursor at an authored coordinate, OR `coordEncode` carries a coord
  across as runes (P6). A map can also be a *map-art mosaic* the group aligns
  (verb-menu "Look / align").
- **SOLVE.** Travel to the marked place; what's *there* is the next clue or beat. Or align
  two map-arts so a third image (a glyph/number) emerges.
- **KEY SOURCE.** The map itself; for aligned map-arts, the alignment is the key.
- **ANSWER → SCHEMA.** Either no Oracle answer (the *place* holds a `small_structure`
  beat — pure world progression), or the coordinate plaintext → `accepted_answers` as in
  P6. `next_clue` / `main_beat`.

#### P10b. A1Z26 + Morse `[both EXISTS]` — *the two free "teaching" rungs*
- **FORGE.** `forgeClue({cipher:'a1z26', text})` carves the letters as ordinals 1–26
  (digit glyphs, `,` word-break); `forgeClue({cipher:'morse', text})` carves dot/dash
  marks (the `.`/`-` structural glyphs, word break → `,`). Both pure + self-tested.
- **SOLVE.** A1Z26: number the alphabet 1..26 and read off — the simplest "count the
  letters" code, ideal as an early/teaching clue. Morse: a dot/dash tap-code read against
  a common in-world Morse-table prop.
- **KEY SOURCE.** A1Z26 needs only the ordinal idea (a tick-mark stave teaches it); Morse
  needs the tap-chart prop. Both in-corpus, both gentle.
- **ANSWER → SCHEMA.** decoded plaintext → `accepted_answers`. Use them for low-movement
  early rungs so the literacy tree has shallow entry points before the hard ciphers.

### — OBSERVATIONAL / WORLD-NATIVE MODALITIES —

#### P11. Rune-ring Rosetta assembly `[NO CODE — observation]` — *the master key (`D03`)*
- **FORGE.** The **server-icon rune ring** is the off-world twin of a six-glyph ring
  carved at the first stone (`D03`). Each glyph is one custom, in a fixed sunwise order.
  No cipher transform — the artifact is the *ring image* + the Rosetta sign.
- **SOLVE.** Read the ring **sunwise from the topmost mark**; the order spells the six
  customs (`BOW OFFERING KEPT-LIGHT DEEP-LINE WARD COVERING`, per `learn-them-as-we-
  learned-them.md`). Assembling the ring = learning the alphabet that reads every later
  stone.
- **KEY SOURCE.** Self-bootstrapping: the Rosetta sign beside the first stone teaches
  4–5 glyphs; the ring teaches the rest. This is the **root of the literacy tree** —
  P4, P6, P9 all depend on the script it teaches.
- **ANSWER → SCHEMA.** the six-custom order (normalized: `bow offering kept light deep
  line ward covering`) → `accepted_answers`; `main_beat` setting `flags.rosetta_known`
  (a soft flag the showrunner uses to start staging script-dependent clues).

#### P12. Observational / counting puzzle `[NO CODE — observation]` — *count the markers, read the structure*
- **FORGE.** No artifact to carve — the puzzle is a **property of the built world**: how
  many marker-stones stand at the shore; which stone is out of order; the count that
  "does not come out even" (`the-record-opens.md`: *"six are named in full, and there is
  a seventh mark"*). The clue is a *prompt* (a report line, a sign) that tells the group
  **what to count**.
- **SOLVE.** Go count. The answer is a number or a name derived from the count. Sella's
  journal (`D06`) literally instructs: *"do not let the count be six only. count again at
  the shore."* The miscount → the Seventh thread.
- **KEY SOURCE.** The world itself. Fair because the prompt says exactly what to count;
  hard because the *meaning* (a seventh, hidden) is the discovery.
- **ANSWER → SCHEMA.** the number/name → `accepted_answers`. Classic `side_quest`
  opener (the Seventh) or `dead_end` (a true count that the watcher acknowledges but
  that leads nowhere — "yes, six. and one more. and that is all it is.").

#### P13. Astronomical / timing puzzle `[NO CODE — gate via moon/time]` — *the black-moon taboo*
- **FORGE.** No cipher. The clue **only legibly exists at a time**: Brann's page
  (`D08`) renders text only at night / on the **black moon** (`fullTime/24000 % 8`,
  per `DESIGN §2.4`, verb-menu "Time it"). A `PrivateTimeShiftBeat` / a hearth that only
  reveals carving at night is the forge.
- **SOLVE.** Be present at the right phase. The *answer* may be a plain word read once
  legible, OR the puzzle's "answer" is **performing at the right time** (don't sleep on
  the black moon = honoring The Dark Hours), detected by the plugin, which fires the beat
  directly — no Oracle submission at all.
- **KEY SOURCE.** Brann tells you, in the dark, which phase. The phase is observable
  in-world. The moon taboo is taught by consequence (the custom loop).
- **ANSWER → SCHEMA.** Two shapes:
  - read-a-word → `accepted_answers` (`lore`/`next_clue`);
  - perform-at-time → no Oracle row; the **custom listener** enqueues the beat
    (`CustomComplianceListener` pattern). The "puzzle" lives in the customs system, not
    `puzzles`, but is part of the same web (its payoff can set a flag a `puzzles` row
    keys off).

#### P14. Audio / sound-sequence puzzle `[NO CODE — sound + arrangement]` — *Brann's beacon-colour sequence*
- **FORGE.** A **beacon-beam colour sequence** (verb-menu "Light / extinguish",
  arg-deepening §1.2 Stone 5) or a **per-stone drone** played via `PrivateSoundBeat` /
  `player.playSound` with a mono `.ogg` from the resource pack. The clue is *an order*
  — colours, tones, or which campfires to light in sequence.
- **SOLVE.** Reproduce the sequence in the world: light a net of campfires in the named
  order (`BlockPlaceEvent` / state detect), or set stained glass over a beacon in the
  colour order, or answer with the colour-word sequence. Brann "gives the beacon-
  sequence" (arg-deepening §2). The order is read from a carving or heard in the drone.
- **KEY SOURCE.** The carving that names the order, or the audible drone whose tones map
  to colours (taught by a small Rosetta: tone→colour). In-world, learnable.
- **ANSWER → SCHEMA.** Either the colour-word sequence → `accepted_answers`
  (`next_clue`), or the *physical light sequence* detected by a custom listener (no
  Oracle row, fires a beat). Pairs naturally with P13 (do it at night).

#### P15. Item-arrangement puzzle `[NO CODE — ChestArrangeBeat / frames]` — *the kept order*
- **FORGE.** `ChestArrangeBeat` (`beats/lib/ChestArrangeBeat.java`) arranges items in a
  chest into a meaningful pattern; the inverse is a puzzle: the group must **arrange
  named items in exact slots** (verb-menu "Bring & deposit": frame/barrel content
  check). The clue is a carving stating *what* goes *where* (an order, a pairing).
- **SOLVE.** Place the right items in the right slots / item-frames / a rune dial ring
  (verb-menu "Rotate", `ItemFrame.getRotation()`). E.g. the six customs' tokens in their
  sunwise order; the offering items at the cairn.
- **KEY SOURCE.** The carving + the customs' canonical order (from P11's ring). Fair:
  the order is the Rosetta order you already learned.
- **ANSWER → SCHEMA.** Detected by a **custom/arrangement listener** (frame contents,
  not a typed answer) → enqueues a beat. For a *typed* variant, the arrangement spells a
  word (item initials, slot positions) → `accepted_answers`. Usually `main_beat` (it
  feeds the Accepting) or `side_quest`.

#### P16. Ritual / custom-performance puzzle `[NO CODE — customs system]` — *the answer is a RITE*
- **FORGE.** The "clue" is an **instruction to do a thing in the world** — the climax of
  Mara's book cipher (`DESCEND AND BOW AT THE UNBROKEN LIGHT`), or the Accepting's
  *"bring the thing only you can give"* (`D12`). No cipher; the forge is the in-world
  altar / threshold + the carving that names the rite.
- **SOLVE.** **Perform the custom**, together: descend, deposit named components, and
  **all present bow simultaneously** (`PlayerToggleSneakEvent` from everyone in a window
  + time/moon gate, per `DESIGN §2.4` / arg-deepening §1.6). The plugin detects the
  performance — *not* a typed answer.
- **KEY SOURCE.** The carving + the components named in the reports + the personal-token
  instruction. Every prerequisite is in the corpus.
- **ANSWER → SCHEMA.** This is the one modality whose "answer" is **not** an
  `accepted_answers` string — it is a **detected world-state** that the customs
  listeners verify, which then sets `arc_state.flags` and enqueues the terminal beat.
  *However*, to keep it inside the same web for staging/visualization, a companion
  `puzzles` row exists with `outcome_type:'main_beat'` and a **sentinel
  `accepted_answers`** that is only ever submitted *by the plugin itself* once the rite
  is detected (e.g. a UUID-like token the plugin posts to the resolver), so the
  Accepting flows through the one Oracle path and inherits its replay-guard + beat
  enqueue for free. (Implementation note in §5.)

### — STEGANOGRAPHIC MODALITY —

#### P17. Steganography in the forged clue-card PNGs `[BONUS — UNBUILT]` — *the mark beneath the mark*
> The one genuinely-unwritten cipher item — a deliberate "+1" the medium begs for, not
> yet in `forge/templates`. (P7–P9 are now shipped `[EXISTS]`; P17 is the only catalog
> entry that is still a build task.)
- **FORGE.** The forge already renders clue artifacts as **satori cards → PNG**
  (`forge/templates/index.ts`, `sigil.ts`). Add a pure post-step that hides a short
  payload in the PNG by one of two deterministic, decodable schemes:
  - **LSB steganography** in the PNG pixel buffer (a few bytes in the low bits of the
    cream background) — pure, reversible, `decode(encode(msg,img))===msg` self-tested on
    a fixed buffer.
  - or **visual stego**: a faint second rune layer in the watermark sigil
    (`getSigilSvg`) at a colour one step off the background — invisible at a glance,
    obvious when the brightness is pushed, which is exactly the kind of "drop the
    screenshot into an editor" move a hard-ARG group loves.
- **SOLVE.** Notice the card hides something; extract it (brightness/contrast, or an LSB
  reader the community writes — TINAG). The payload is a short plaintext, a coordinate,
  or a key for another puzzle (e.g. the Vigenère key, or a rail count).
- **KEY SOURCE.** None needed beyond *suspicion* — the fairness is that the payload, once
  extracted, is itself in-corpus. The watcher may *foreshadow* it ("there is more on the
  stone than the marks") via a `lore` line.
- **ANSWER → SCHEMA.** extracted plaintext → `accepted_answers`. Excellent for a
  `side_quest` or for hiding *one* high-value key (a Vigenère key) so the cipher chain
  P17→P3 spans two surfaces and an image editor.

### Cross-document correlation (a meta-modality, woven through, not numbered)
Several puzzles are **unsolvable from one document** — they require holding two against
each other, which is the heart of the doc web (`found-documents.md` INTERLINK MAP):
- `D02` (Vaun's ledger) **contradicts** `D01` (the record) → a correlation puzzle whose
  "answer" is the contradiction itself (the offering was never kept).
- `D09` (Iss) **answered by** `D10` (the Stone-after) → the Liar catch (§3).
- `D07` (Orin's withheld third line) **completed by** `D04` (the record finishing it).
- `D06` margins → `D11` map-note → the Seventh.
Correlation puzzles forge as **two artifacts in two places**; solve by bringing the
plaintexts together; answer = the synthesized fact. They are the connective tissue that
makes the graph a *web* and not a bundle of independent strands.

---

## 2. THE WEB AS A GRAPH (not a line)

The schema already supports non-linearity: `accepted_answers` is an array (many keys per
lock), `outcome_type` branches five ways, and the resolver never checks `movement`
(`ORACLE.md §8`). The *design* job is to wire nodes so that **at any moment several
puzzles are `active`**, **multiple paths reach each gate**, **some true answers are
walls**, and **no node is a countable "step N of M."**

### 2.1 Node legend

```
[clue]  a forgeable puzzle (a puzzles row)              ──►  next_clue / main_beat edge (a DOOR)
(lore)  a told fragment, no progression                ┄┄►  lore edge (reveals, no door)
{rite}  a performed custom (detected, not typed)        ╌╌►  side_quest edge (optional branch)
<XdeadX> a TRUE-but-not-a-door answer (red herring)     ▲    converging paths (≥2 keys → one gate)
[[GATE]] an act/movement gate (a flags threshold)
```

### 2.2 The five Movements as graph regions (Movement I = Act 1; II–IV = Act 2; V = Act 3)

```
                                   ┌───────────────────────────── MOVEMENT I — THE NOTICE ──────────────────────────────┐
                                   │                                                                                     │
   server-icon ring ──►[P11 ROSETTA]──main_beat──► sets flags.rosetta_known ──────────────┐                            │
        (D03)                  ▲                                                            │ (literacy unlocks the     │
   first-stone Rosetta sign ───┘                                                           │  script for P4/P6/P9)     │
                                   │                                                        ▼                            │
   (D01 the record opens) ┄┄► (P-lore: the count has begun) ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄► [P12 COUNT the markers] ──side_quest──┐  │
                                   │                                              │  "six… and a seventh?"          │  │
                                   └──────────────────────────────────────────── │ ────────────────────────────── │ ─┘
                                                                                  ▼                                 ▼
   ┌────────────────────────────── MOVEMENT II — THE KEEPER-STONES ──────────────────────────────────┐   (Seventh thread,
   │  six expeditions, each a DIFFERENT cipher + verb (arg-deepening §1.2). All active together:      │    Movements II→III,
   │                                                                                                  │    OPTIONAL — §2.4)
   │   [P1 CAESAR · Vaun] ──next_clue──►(P5 book ref appears)                                          │        │
   │   [P4 SUBST · Orin]  ┄┄lore┄┄► (orin: "i thought it small")  ──► completes at [P-corr D07×D04]    │        │
   │   [P2 ATBASH · Sella]─next_clue─► a bearing ╌╌side_quest╌╌► [P11-map D11] ──────────────────────► │ ───────┤
   │   [P5 BOOK · Mara]   ──next_clue──► names a RITE ──► {P16 descend & bow at the unbroken light}     │        │
   │   [P14 BEACON · Brann]─(gated by P13 night)─next_clue─► rail count for ▼                           │        │
   │   [P7 RAILFENCE]  ◄── rail count ◄── P14 ; reads only at night (P13)                               │        │
   │                                                                                                   │        │
   │   [P3 VIGENÈRE · Iss] ──two doors from ONE key (ISS):                                              │        │
   │        ├─ warm plaintext ──next_clue──► [P10 map to the DEAD SHRINE]  ◄═══ TRUE place, WRONG door │        │
   │        └─ name-as-key ("the one who turned away") ──main_beat──► flags.iss_caught (§3)             │        │
   │                                                                                                   │        │
   │   <X P8-acrostic in D04: a hidden keeper-name X>  ──dead_end──► watcher acknowledges, opens nothing│        │
   └───────────────────────────────────────────────────────────────────────────────────────────────-─┘        │
            │  (≥4 of 6 stones' fragments)                     │ (P17 stego card hides a Vigenère key → P3)     │
            ▼                                                   ▼                                                ▼
   ┌────────────────── MOVEMENT III — THE UNDERCROFT / THE SEVENTH ───────────────────────────────────────────────────┐
   │   [P9 POLYBIUS marker-grid] ──next_clue──► undercroft bearing                                                      │
   │   {P13/P14 night beacon rite} ╌╌► opens the descent                                                                │
   │   [P11-map D11 the-seventh-not-kept] ──side_quest──► (lore: the land can refuse) + earns Whisper budget ┄┄► (P10) │
   │   converging on:  [[GATE II→III]] = flags.fragments>=4  AND  a final-coordinate cipher solved (P6/P10)            │
   └───────────────────────────────────────────────────────────────────────────────────────────────────────────────-─┘
            │                                                                                     ▲
            ▼                                                          (Iss's wrong fragment + the dead shrine
   ┌────────────────── MOVEMENT IV — THE CATCH ─────────────────────────────┐                     both point here, then are
   │   re-walk a "solved" clue:  [P3 warm door] now contradicted by [D10]    │                     contradicted — §3)
   │   catching it ──main_beat──► flags.iss_caught ──► Iss yields TRUE coord ─┼──► [P6/P10 true final coordinate] ──────┘
   │   <X P8 false-coord acrostic X> ──dead_end──► "yes. and it opens nothing."                                          │
   └───────────────────────────────────────────────────────────────────────-┘
            │  flags.iss_caught AND flags.true_coord_found
            ▼
   ┌────────────────── MOVEMENT V — THE ACCEPTING ──────────────────────────┐
   │   {P16 RITE}: gather named components + one personal token per keeper   │
   │   + all-present simultaneous bow at the hour (detected, not typed)      │
   │   ──main_beat (plugin-posted sentinel)──► the world flips · advancement │
   │   [[GATE — ACTIVE players only; never blocked by an absent member]]     │
   └────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Multiple paths converge on every gate (the anti-ladder proof)

No gate is reachable by exactly one chain. Worked redundancy:

| Gate / key node | Path A | Path B | Path C |
|---|---|---|---|
| **Script literacy** (`flags.rosetta_known`) | assemble the server-icon ring (P11) | the first-stone Rosetta sign teaches 4–5 glyphs, enough to brute the rest | any solved substitution stone (P4) reverse-teaches glyphs |
| **The Seventh exists** | miscount the shore markers (P12) | Sella's atbash bearing + her margin (P2→D11) | Iss's wrong fragment dead-ends *at* the Seventh's ruin (P3 warm → P10) |
| **GATE II→III** (`fragments>=4` + final coord) | any 4 of the 6 stone fragments — *which* 4 is free | the final coordinate via P6 (carved) **or** P10 (map mark) | Whisper budget earned at the Seventh shrine can buy a stuck stone's hint |
| **Iss caught** (`flags.iss_caught`) | turn his key on the *other* stones (P3 name path) | read `D10` (the Stone-after) which contradicts him line-for-line | extract the stego key (P17) → solve P3 the "right" way |
| **True final coordinate** | yielded by the catch (Movement IV) | P9 polybius marker-grid independently encodes it | P6 carved coord on a late stone |

Because every gate has ≥2 in-doors and the resolver ignores `movement`, a group that
bounces off one puzzle is **never** wall-stuck: they wander to another active node and
the web stays open. The number of puzzles open at once in Movement II is **six stone
expeditions + the Seventh side-quest + any carried-over Movement-I threads** — there is
no "you are on step 3" because there is no canonical 3.

### 2.4 Soft-pressure & optionality (per `DESIGN §1`, `FLOW §1`)

- **The Seventh thread (P12 → P2/D06 → P11-map/D11)** is entirely optional; ignoring it
  leaves it quiet (it never nags) and only forfeits Whisper budget + one `FACT 10`
  reveal. It gates *nothing*.
- **Lore nodes (`outcome_type:'lore'`)** are pure character/world payoff — Orin's
  fragment, Sella's drift, Vaun's grief. They reward exploration without advancing, so a
  group can "win" the feeling of progress by reading even when no door opens.
- **`dead_end` nodes** are deliberate (next subsection). They make a hard web *honest*:
  some true answers just don't open doors, and the watcher says so.

### 2.5 Red herrings the watcher acknowledges — the `dead_end` doctrine

A `dead_end` (`0004_oracle.sql` / `ORACLE.md §3`) is the load-bearing red-herring tool:
a **TRUE answer that opens nothing**, *heard* (the watcher speaks `oracleDeadEnd`) but
not advanced — distinct from a miss (pure silence). Authored herrings in this web:

1. **The hidden acrostic in `D04`** (P8) spells a real keeper-name buried in the
   record's ledger lines. Submitting it is *correct* — and a `dead_end`: *"yes. that is
   a true name. it is written here. and it opens nothing. some names are only kept."*
2. **The dead shrine** (P3 warm door → P10): a **true place** Iss really sends you to,
   that is a real location with a real (cold, empty) payoff — but it is a *grave, not a
   threshold* (`no-wall-was-ever-built-here.md`). Mechanically the carving there resolves
   to a `dead_end`/`lore` row, not the final-coordinate door. This is the most important
   herring: it is *true-but-not-a-door* at world scale, and the catch (§3) is realizing
   the warm answer was a wall.
3. **The even count** (P12 variant): "six." Correct, true, and a `dead_end` — until you
   *count again* and find the seventh. The watcher acknowledges six and says no more,
   which is itself the nudge.
4. **A false final-coordinate acrostic** (Movement IV): a number that decodes cleanly and
   is *wrong* — `dead_end`, "*it opens nothing*," forcing the re-walk.

> Rule: a `dead_end` is **never** an error and **never** a "close." It is the watcher's
> calm "true, and not the door" — which in a hard ARG is itself a reward (you proved you
> could read it). Every `dead_end` records a `solves` row so it fires once and can't be
> farmed (`resolve.ts` step 7).

---

## 3. THE LIAR ENGINE — one key, two doors, a forced re-walk

This is the web's signature non-linear move and it lives entirely inside existing
schema. Iss's Vigenère (P3) is keyed on his **own name, `ISS`**. The *same key* produces
two correct plaintexts depending on *what you apply it to*:

- Applied to **his own letter** → a warm, comforting plaintext + a final-coordinate
  fragment that points at the **dead shrine** (real place, wrong door). Authored as a
  `next_clue` row → `next_puzzle_key` = the dead-shrine map puzzle (P10), which itself
  resolves `dead_end`/`lore`.
- Applied **against the other stones** → the word the corpus uses for *"the one who
  turned away."* Authored as a separate `main_beat` row whose `set_flags` sets
  `iss_caught: true` and whose payload enqueues the dialogue-flip beat.

The **catch** (Movement IV) is triggered by reading `D10` (`no-wall-was-ever-built-
here.md`), placed *behind a clue the group marked solved*. `D10` yields the **true**
final coordinate **only after** `flags.iss_caught`. Mechanically:

```
[P3 Iss/Vigenère]  ── key=ISS ──►  ┌─ warm plaintext   → next_clue → [P10 dead shrine] → dead_end/lore  (the wall)
                                    └─ "the one who turned away" → main_beat → set_flags{iss_caught} ──┐
                                                                                                        ▼
[D10 Stone-after] active=true always, but its outcome_payload gates:                                    │
   IF flags.iss_caught  → next_clue → true final coordinate (P6/P10)  ◄────────────────────────────────┘
   ELSE                 → lore  ("no wall was ever built here." — names the lie, withholds the door)
```

The "re-walk a solved clue" is realized by the showrunner (or a flag-gated authored row)
swapping `D10`'s effective outcome once `iss_caught` is set — **no resolver change
needed**, because the showrunner can flip `active` / edit `outcome_payload` between
sessions (`DESIGN §2.10`), and the dialogue-flip is a beat. The player experience: a
warm answer that *worked* is revealed to have been a wall, and the same stones, re-read,
now read cold. (Full prose: `arc/lore/documents/the-ways-are-a-wall.md` ↔ `no-wall-was-
ever-built-here.md`.)

---

## 4. `coordEncode` — the digit-glyph fix (CODE DONE) + the teachable in-world Rosetta (CONTENT TODO)

> **Status:** the *code* half of this fix is **already shipped** in `ciphers.ts` —
> `coordEncode` emits `X<digits>,Z<digits>` in digit glyphs (e.g. `X-1280,Z64`), with the
> self-tests this section called for (`coordEncode reads as a real number`, leading-zero
> trap, 32-bit extremes). The text below is retained as the *rationale* and as the spec
> for the remaining **content** task: building the in-world **Stone of Reckoning** Rosetta
> (§4.2(b)) that teaches the digit/sign marks. Read §4.2(a) as "why the scheme is what it
> is," not as a pending code change.

### 4.1 The problem the fix solved
An earlier `coordEncode` spelled coordinates as **letter-glyphs** (sign letter `N`/`P` +
base-26 magnitude in A–Z). It round-tripped in code, but **in-world it was a second,
untaught cipher**: a player staring at `NBXG PCM` had no Rosetta saying "these letters are
a base-26 *number*, and `N` means negative." It looked like more substitution text — a
fair solver decoded it as letters and got nonsense, breaking Law 2 (in-corpus key).

### 4.2 The fix — digit glyphs + the **Keeper-Stone of Reckoning** (a found Rosetta)
Two coupled parts — the *encoding* (now uses the script's **digit** glyphs, `runes.ts`
`DIGIT_BRANCHES` 0–9; **DONE in code**), and the *world* (a found stone that teaches the
digit glyphs and the sign marks; **content task**).

**(a) Encode coordinates in the digit glyphs, not letter glyphs.** `runes.ts` already
carves digits 0–9 (`DIGIT_BRANCHES`) and structural marks `-` (negative) and `,` (axis
separator) — visually a *distinct numeric run* from letters, exactly so "coordinates
read as numbers." Change `coordEncode` to emit **base-10 digit glyphs with a leading `-`
mark for negatives and a `,` between axes**, e.g. `(x=-1280, z=64)` → `-1280,64` carved
in digit-runes. This is still pure, lossless, and self-tested; it just uses the glyphs
the script *already has a numeric family for*, so the artifact looks like a number.

> Pure helper sketch (drop-in for `ciphers.ts`, self-test added to `runSelfTests`):
> ```ts
> export const coordEncode = {
>   encode(coord: Coord): string { return `${coord.x},${coord.z}`; },      // digit glyphs + ',' mark
>   decode(text: string): Coord {
>     const m = text.trim().split(',');
>     if (m.length !== 2) throw new Error(`coordEncode: expected "x,z", got "${text}"`);
>     const x = parseInt(m[0], 10), z = parseInt(m[1], 10);
>     if (!Number.isInteger(x) || !Number.isInteger(z)) throw new Error('coordEncode: non-integer');
>     return { x, z };
>   },
> } as const;
> ```
> The carved output is digit/`-`/`,` glyphs (all in `SUPPORTED_CHARS`), so `renderRunes`
> still draws it; round-trip `decode(encode(c)) === c` holds for all 32-bit ints. (The
> shipped `ciphers.ts` matches this — the actual encode prefixes an `X`/`Z` axis label so
> the reader knows which axis they hold, e.g. `X-1280,Z64`; the principle is identical.)

**(b) The Rosetta stone in the world.** A `small_structure` + `sign_write` /
`lectern_fill` artifact — the **Stone of Reckoning** — carved with the ten digit glyphs
beside their plain numerals (`0 1 2 3 4 5 6 7 8 9`), the `-` mark labelled *"that which
is below / behind"* (negative), and the `,` mark labelled *"and then, the other way"*
(axis separator), with a worked example: one carved digit-rune line and its number
beneath. Vaun is the natural keeper to attach it to — *"i did not make the count. i
learned it. the land counts first"* (`D02` / `counted-them-in-the-dark.md`): the man who
counted everything teaches the group to read the land's counting. Place it early
(Movement I/II) at his stone or the spawn lectern, so the numeric literacy is earned
before any coordinate clue is needed.

### 4.3 Normalization caveat (Law 3)
`normalizeAnswer` strips the minus sign (`-1280 64` → `1280 64`). So coordinate
`accepted_answers` are authored **unsigned**, and when a sign genuinely disambiguates,
include **both** forms or a spelled form, e.g.
`["1280 64", "neg 1280 64", "minus 1280 64", "1280 north 64"]` (the corpus already uses
direction words — Sella's "south, by the far water"; Iss's "west and a little down").
Authoring guidance: prefer **direction-word answers** for coordinates ("descend",
"south", "back along the keeper-row, six stones to one" — `no-wall-was-ever-built-
here.md`) which survive normalization and read in-voice, and reserve raw numerals for
the cross-surface handoff where the *map/travel* is the real verb (P10) and the typed
answer is a confirmation.

---

## 5. SCHEMA-CARRYING NOTES (how this all lives in `puzzles`)

- **One `puzzles` row per node, keyed by an AUTHORED stable `puzzle_key`** (the canonical
  scheme — see the boxed resolution below). `puzzle_key` is the PK the resolver matches and
  the join point every `next_puzzle_key` edge, `solves` row, `answer_attempts` row, and the
  hint ledger (0003) addresses. The canonical seed `puzzles_seed.sql` uses **kebab-case
  authored keys** (`stone-vaun`, `no-wall-catch`, `seventh-shrine`, `rite-tokens`, …),
  matching `design/clue-web.md`'s node keys one-for-one.

> **THE puzzle_key SCHEME — resolved end-to-end (read this once).**
> The forge derives `clue_<cipher>_<fnv1a32(carved-text)>` (`index.ts makePuzzleKey`).
> That hash is **content-derived** — you can't author it ahead of time without already
> knowing the exact ciphertext — so it is **NOT** the key the hand-authored spine uses.
> The schema (`0004_oracle.sql`) only requires that `puzzle_key` be a stable text PK; it
> does not require the forge hash. The resolution:
> 1. **The authored web uses authored kebab keys.** Every spine/side node is a hand-minted
>    `puzzle_key` (`stone-mara`, `iss-doubt`, …) in `puzzles_seed.sql`. These are stable,
>    human-legible, and let `next_puzzle_key` edges be written by hand. This is the
>    canonical namespace for BOTH this doc and `clue-web.md`.
> 2. **The in-world forged clue references its node by that authored key.** When the forge
>    renders a stone's ciphertext, the authoring/placement step binds that artifact to the
>    node's authored `puzzle_key` (the forge's auto-hash is metadata for the ledger, not
>    the resolver PK for an authored node). The player decodes the carving and submits the
>    plaintext; the resolver matches the plaintext against `accepted_answers` of the OPEN
>    rows — it never needs the forge hash at all (`ORACLE.md §2`: match is on the
>    normalized answer string, keyed to whichever row carries it).
> 3. **The forge auto-hash key is for AD-HOC drip clues only** — the between-session
>    `postClue` cards the Watcher drops that have no authored spine node. Those get
>    `clue_<cipher>_<id>` for free and self-register; the authored web does not use them.
> Net: one namespace (kebab) for the authored web; the FNV scheme stays available for
> ad-hoc clues. Every `next_puzzle_key` in the seed resolves to a real authored key.
- For `[NO CODE]` modalities (P11/P12/P15/P16) that have no `forgeClue` output, the same
  rule already applies — they are authored kebab keys by construction.
- **Branching = `outcome_type` + `outcome_payload`**, exactly as `resolve.ts applyOutcome`
  realizes it: `voice_key` (which `voice.ts` line speaks), optional `set_flags`
  (arc_state), optional `next_puzzle_key`, optional `beat` (enqueued `status:'approved'`
  — player-earned, no human gate). Authors never write English into payloads (`ORACLE.md
  §7`); only `voice_key` + structured args.
- **Player-earned vs curatorial.** Oracle solves enqueue `status:'approved'` (fire
  immediately — `ORACLE.md §4`). The Liar dialogue-flip and any showrunner re-staging are
  **curatorial** and go through `status:'pending'` in CONFIRM mode. This honors the
  AUTO↔CONFIRM split: a player who *earned* an unlock never waits; a showrunner beat does.
- **The rite (P16) sentinel.** Because the Accepting is *detected*, not typed, the plugin
  posts a sentinel `accepted_answers` token to the shared resolver once the customs
  listeners confirm the simultaneous-bow + deposits, so the terminal `main_beat` inherits
  the `solves` replay-guard and the `beat_queue` enqueue path for free. The sentinel is a
  long opaque token (never guessable, never in the corpus), so no player can shortcut the
  rite by typing it. **It is written in the normalized charset (`[a-z0-9 ]` only — no
  hyphens/underscores)** so the stored value equals its own `normalizeAnswer` form and the
  plugin's posted token matches exactly.
- **`active` is the showrunner's staging dial**, not a difficulty lock. The whole web can
  be authored up front; `active=false` rows are staged and flipped on per Movement (or
  flag) so spoiler answers don't leak early (`ORACLE.md §8`).

---

## 6. THE SEED — pointer to the canonical file (this doc no longer ships its own)

> **There is exactly one playable seed, and it is not here.** An earlier draft of this
> doc carried a 12-row illustrative seed in a separate `clue_<cipher>_<id>` namespace.
> That duplicated the playable web and used the wrong key scheme, so it has been
> **retired**. The single source of truth is:
>
> - **`discord/supabase/seeds/puzzles_seed.sql`** — the spoiler-free playable web (23
>   rows, authored kebab keys, every `accepted_answers` pre-normalized per `ORACLE.md §2`,
>   realizing `design/clue-web.md §3` node-for-node). Run after `0004_oracle.sql` as
>   service_role; `ON CONFLICT (puzzle_key) DO UPDATE` makes re-running it the canonical
>   way to edit a node.
> - **`arc/cipher-web-seed.sealed.json`** — the SEALED endgame rows whose plaintexts/coords
>   would spoil the twist (`D10`'s flag-gated dual payload, the true final coordinate, the
>   Accepting's token validation, the false-coord dead-end, the parallel polybius door).
>   Spoiler-bearing; the dashboard's spoiler-free mode and Ethan never open it.
> - **`design/clue-web-seed-notes.md`** — the `puzzle_key → which lore doc/beat teaches it`
>   placement index, plus the open gaps (G1–G6).
>
> **Crosswalk** (the old illustrative keys → the canonical authored keys, so any external
> reference still resolves):
>
> | old illustrative key (retired) | canonical key (`puzzles_seed.sql`) |
> |---|---|
> | `clue_substitution_rosetta_ring` | `rosetta-ring` |
> | `clue_count_markers_six` | `m1-named-habit` (the "true count, opens nothing" dead-end) |
> | `clue_count_markers_seventh` | folded into `stone-sella` / `seventh-shrine` (the Seventh thread) |
> | `clue_caesar_vaun_offering` | `stone-vaun` |
> | `clue_substitution_orin_small` | `stone-orin` |
> | `clue_book_mara_descend` | `stone-mara` |
> | `clue_atbash_sella_bearing` | `stone-sella` |
> | `clue_vigenere_iss_warm` | `stone-iss-wall` (warm reading) → `iss-dead-shrine` |
> | `clue_vigenere_iss_name` | `stone-iss-wall` (name-as-key) → `iss-doubt` |
> | `clue_acrostic_record_deadname` | `m2-rhyme` / sealed `clue_acrostic_false_final_coord` |
> | `clue_map_dead_shrine` | `iss-dead-shrine` |
> | `clue_rite_descend_bow` | `undercroft-descent` (+ `accepting-crouch` for the bow) |
>
> The five facts that block below illustrated — converging Seventh paths, the capped warm
> Iss door vs. uncapped name door, the `dead_end` shrine, the staged `active:false` rite,
> and `status:'approved'` beat enqueue — are all realized in the canonical seed. See it for
> the live rows; the notes that follow record the design rationale those rows encode.

**Design rationale the canonical rows encode (formerly the per-row notes):**
- **Two paths to the Seventh** — `stone-sella` (Atbash bearing, `side_quest` →
  `seventh-shrine`) and the miscount thread both converge on `seventh-shrine`, a gate, not
  a line. `seventh-shrine` itself **gates nothing** ("the way goes on without it").
- **Iss is capped where it's brute-forceable, uncapped where it's the door** —
  `stone-iss-wall` carries `max_attempts: 6` on the short warm reading; the name-as-key
  door is uncapped (large answer space, the intended path).
- **`iss-dead-shrine` is a `dead_end`** — a true place Iss really sends you to (a grave),
  acknowledged in voice, opens nothing. The catch is realizing the warm reading led here.
- **The M5 terminal rows are staged `active:false`** (`record-receives`) and/or use opaque
  plugin-posted sentinels (`accepting-crouch`, `record-receives`, and the sealed
  `clue_rite_accepting_tokens`) — the rite is *performed*, never typed.
- **Every `beat` enqueues `status:'approved'`** via the Oracle path (`resolve.ts`), so
  player-earned unlocks fire on the next poll with no human gate; curatorial swaps (the Iss
  warm→cold flip, D10's post-catch outcome) ride `status:'pending'` in CONFIRM mode.

<details>
<summary>Retired illustrative seed (kept folded for reference only — DO NOT run; use
<code>puzzles_seed.sql</code>)</summary>

```json
[
  {
    "puzzle_key": "clue_substitution_rosetta_ring",
    "title": "P11 — the rune ring (Rosetta / master key)",
    "accepted_answers": ["bow offering kept light deep line ward covering"],
    "outcome_type": "main_beat",
    "outcome_payload": {
      "voice_key": "oracleMainBeat",
      "set_flags": { "rosetta_known": true },
      "beat": { "type": "unlock", "mc_uuid": "{solver}",
                "payload": { "step": "advancement_toast",
                             "step_payload": { "key": "observance:the_ring_is_whole" } } }
    },
    "movement": 1,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_count_markers_six",
    "title": "P12 — count the shore markers (the even, true count)",
    "accepted_answers": ["six", "6"],
    "outcome_type": "dead_end",
    "outcome_payload": { "voice_key": "oracleDeadEnd" },
    "movement": 1,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_count_markers_seventh",
    "title": "P12b — count again (the seventh)",
    "accepted_answers": ["seven", "7", "a seventh", "the seventh"],
    "outcome_type": "side_quest",
    "outcome_payload": {
      "voice_key": "oracleSideQuest",
      "next_puzzle_key": "clue_atbash_sella_bearing",
      "set_flags": { "seventh_suspected": true }
    },
    "movement": 1,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_caesar_vaun_offering",
    "title": "P1 — Vaun's Caesar (give the first of the deep back)",
    "accepted_answers": ["give the first of the deep back to the deep"],
    "outcome_type": "next_clue",
    "outcome_payload": {
      "voice_key": "oracleNextClue",
      "next_puzzle_key": "clue_book_mara_descend"
    },
    "movement": 2,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_substitution_orin_small",
    "title": "P4 — Orin's plain script (crouch to read)",
    "accepted_answers": ["i thought it small it was not small"],
    "outcome_type": "lore",
    "outcome_payload": {
      "voice_key": "oracleLore",
      "voice_args": { "fragment": "the bow is the smallest of the ways and the cheapest to give. he gave it to an empty road." }
    },
    "movement": 2,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_book_mara_descend",
    "title": "P5 — Mara's book-cipher (the map names the rite)",
    "accepted_answers": ["descend and bow at the unbroken light"],
    "outcome_type": "next_clue",
    "outcome_payload": {
      "voice_key": "oracleNextClue",
      "next_puzzle_key": "clue_rite_descend_bow",
      "set_flags": { "mara_read": true }
    },
    "movement": 2,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_atbash_sella_bearing",
    "title": "P2 — Sella's mirror (read to the water)",
    "accepted_answers": ["south by the far water where she did not come back"],
    "outcome_type": "side_quest",
    "outcome_payload": {
      "voice_key": "oracleSideQuest",
      "next_puzzle_key": "clue_map_seventh_shrine"
    },
    "movement": 2,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_vigenere_iss_warm",
    "title": "P3a — Iss's Vigenère, warm reading (the wall — a wrong door)",
    "accepted_answers": ["the ways are a wall keep them and you cannot be had"],
    "outcome_type": "next_clue",
    "outcome_payload": {
      "voice_key": "oracleNextClue",
      "next_puzzle_key": "clue_map_dead_shrine"
    },
    "movement": 2,
    "active": true,
    "max_attempts": 6
  },
  {
    "puzzle_key": "clue_vigenere_iss_name",
    "title": "P3b — Iss's key turned on the other stones (the catch begins)",
    "accepted_answers": ["the one who turned away"],
    "outcome_type": "main_beat",
    "outcome_payload": {
      "voice_key": "oracleMainBeat",
      "set_flags": { "iss_caught": true },
      "beat": { "type": "unlock", "mc_uuid": "{solver}",
                "payload": { "step": "private_message",
                             "step_payload": { "key": "iss.dialogue.turns_cold" } } }
    },
    "movement": 2,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_acrostic_record_deadname",
    "title": "P8 — the hidden name in the record's lines (true, opens nothing)",
    "accepted_answers": ["orin"],
    "outcome_type": "dead_end",
    "outcome_payload": { "voice_key": "oracleDeadEnd" },
    "movement": 2,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_map_dead_shrine",
    "title": "P10 — the place Iss sends you (a grave, not a threshold)",
    "accepted_answers": ["the dead shrine", "nothing is kept here", "the cold hearth"],
    "outcome_type": "dead_end",
    "outcome_payload": { "voice_key": "oracleDeadEnd" },
    "movement": 2,
    "active": true,
    "max_attempts": null
  },
  {
    "puzzle_key": "clue_rite_descend_bow",
    "title": "P16 — perform the rite (detected, not typed; plugin posts the sentinel)",
    "accepted_answers": ["c1f3a7e0 rite descend bow sentinel posted only by plugin"],
    "outcome_type": "main_beat",
    "outcome_payload": {
      "voice_key": "oracleMainBeat",
      "set_flags": { "descended_and_bowed": true },
      "beat": { "type": "unlock", "mc_uuid": "{solver}", "site_id": "unbroken_light",
                "payload": { "step": "small_structure",
                             "step_payload": { "require_floor": true,
                               "blocks": [ {"dx":0,"dy":0,"dz":0,"material":"CHISELED_STONE_BRICKS"} ] } } }
    },
    "movement": 5,
    "active": false,
    "max_attempts": null
  }
]
```
</details>

---

## 7. AUTHORING CHECKLIST (so a new puzzle can't break the laws)

Before any new row goes `active`:

1. **Forge is deterministic & decodable** — it round-trips in `runSelfTests()`
   (cipher modalities) or it is a curated beat/placement (world modalities).
2. **The key is in the corpus** — name the Rosetta / document / count that teaches it.
   If you can't, it's not fair yet.
3. **`accepted_answers` are pre-normalized** — run them through `normalizeAnswer`
   mentally: no leftover punctuation, coords unsigned (or both forms / direction-words).
4. **`outcome_type` matches intent** — door (`next_clue`/`side_quest`/`main_beat`),
   told-only (`lore`), or true-but-not-a-door (`dead_end`). A `dead_end` carries
   `voice_key` only — no `next_puzzle_key`, no `beat`.
5. **The node has ≥2 in-doors** (web rule) — or it is explicitly an optional leaf
   (a lore/side-quest tip) that gates nothing.
6. **Player-earned beats are `approved`; curatorial beats are `pending`** — never make a
   player wait on a human for a reward they earned.
7. **No countable ladder** — if the puzzle reads as "step N of M," re-shape it: open it
   alongside siblings, give it a red-herring neighbor, or add a second path to its gate.
```
