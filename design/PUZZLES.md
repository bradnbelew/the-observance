# THE OBSERVANCE — THE PUZZLE SYSTEM (diversity by design)

> Canonical. Companion to [OVERHAUL.md](OVERHAUL.md). Fixes the project's biggest puzzle
> weakness: it was **cipher-monotone** — 5 of 6 keepers were "decode this text" with a
> different letter-method. This doc makes puzzles diverse across **four independent axes** so
> no two consecutive beats feel the same, and makes the **answer itself** various (not just a
> typed phrase). Classic ciphers survive as a *minority flavor*, not the whole diet.

---

## 0. THE RULE (the one thing to enforce)

**No two puzzles a player meets in a row may share the same (TYPE × SURFACE × VERB × ANSWER).**
A puzzle is a point in a 4-axis space; vary at least two axes every step. The 5 letter-ciphers
become ~1/4 of the puzzles, deliberately spaced. Difficulty lives in **noticing the puzzle
exists and what kind it is**, not in grinding a known cipher (DOSSIER §B3).

The four axes:
- **TYPE** — how the secret is encoded.
- **SURFACE** — where it lives (which platform/medium).
- **VERB** — what the player physically does to solve it.
- **ANSWER** — what "the answer" actually is and how it's submitted.

---

## 1. AXIS 1 — TYPE (how it's encoded)

Classic ciphers are ONE row of many. The full menu:

- **Letter-ciphers (minority):** Caesar, Vigenère, atbash, substitution, book-cipher, rail-fence.
  (The 5 keeper stones — keep, but space them out and stop adding more.)
- **Numeral/positional:** A1Z26, tally/tick-stave, beacon-colour order, a combination lock.
- **Steganographic:** LSB-in-PNG (built); a word hidden in an **audio spectrogram**; zero-width
  characters in a "recovered" doc; a phrase spelled by the **first letter of each journal line**;
  EXIF/filename payloads on a hosted image.
- **Visual / spatial:** a rune legible **only in a water/ice reflection** (Sella); a block-glyph
  readable **only from above** (map/elytra); a **shadow** cast at a specific in-game time that
  spells a word; a redstone display; two semi-transparent map-arts that **overlay** into a third image.
- **Audio:** **morse/rhythm** in a sound cue; a message played **backwards**; a **note-block melody**
  whose pitches map to letters; ambient whispers that, layered, say a name.
- **Logic / deduction:** "which keeper lied" from cross-checked claims; a timeline reconstruction;
  a grid/lateral-thinking puzzle assembled from NPC statements.
- **Cross-reference / research:** combine a fact from 3 NPCs; translate a real Latin/Old-English
  phrase; match a journal's stardate-style marker to an in-game day; identify a real constellation.
- **Observation / counting:** how many lamps are lit; which sign changed; the recurring number.
- **Layered (cipher-of-cipher):** solving A yields the **key** for B (Vigenère key = a name you
  earned earlier — the bound-word pattern, generalized).
- **Embodied / behavioral:** the "answer" is an **action the plugin detects** — bow at six markers
  in fall-order, walk a rune, place an item, hold a sequence (no typing at all).
- **Asymmetric co-op (signature):** each player is shown a **different fragment** via per-player
  illusion; the answer only exists when they say what they each see **out loud** and combine it.
- **Temporal:** a clue that only appears at a real-world time, an in-game moon phase (Brann's
  black moon), or after a real-world delay ("come back when it's dark").
- **Voice-heard (Observer Engine):** the answer is **spoken in voice chat**; the Watcher hears it
  (Whisper) and the world reacts — the players never type it.

---

## 2. AXIS 2 — SURFACE (where it lives)

A puzzle's home, and a chain can **hop surfaces** (that's half the magic):

- **In-world:** carved blocks (rune font), signs, lectern books, item lore, map-art murals,
  structures, mob names, particles, sounds, the **Undercroft fog**.
- **The record website:** ledger pages, a hidden `/path`, source-comment payloads, a redacted doc
  that un-redacts as flags flip, the **answer-input field** (diegetic "inscribe the record").
- **Discord (no game-persona):** a pinned image, a channel topic, an attachment with stego — used
  as the friends' own space the Observer *reads*, not where the Watcher "posts."
- **External / real-world ARG surfaces:** a **Google Drive** "recovered archive" folder of found
  docs/images/audio; an **unlisted YouTube** "found-footage" clip (HyperFrames can generate these);
  a one-page **website**; a **Google Form** "intake"; a **Google Voice** number with a voicemail
  the Watcher left. These make it a real ARG that "leaves the game" (DOSSIER §A13).
- **Cross-surface chains (the point):** a carved sign → a URL → the page gives coords → the coords
  lead to a structure → the structure hides the next cipher → its answer is spoken aloud. Each link
  a different surface; no single tab/screen holds the game.

---

## 3. AXIS 3 — VERB (what the player does)

Decode · decrypt-with-a-found-key · **observe** · **count/measure** · **travel** (to coords) ·
**build/arrange** (place blocks into an answer) · **perform** (a ritual/sequence) · **research**
(look it up) · **combine** (asymmetric/cross-ref) · **listen** · **reflect/rotate/overlay** ·
**wait/return** (temporal) · **speak** (voice). Each beat should demand a *different body action*
than the last — that variety IS the felt diversity.

---

## 4. AXIS 4 — ANSWER (what the answer IS — vary this, not just the question)

The resolver today only takes a typed phrase. The system must accept many answer shapes:

| Answer type | What the player submits / does | How it's checked |
|---|---|---|
| **Plaintext phrase** | types it (in-world sign or the record website) | the existing oracle (normalized set-membership) |
| **Coordinates** | travels to an XYZ and arrives | a territory listener detects arrival in radius |
| **A URL to VISIT** | the answer is a destination; a token lives there | type the token found at the URL (forces the visit) |
| **A real-world fact** | a constellation name, a date, a translation | oracle plaintext (authored answers) |
| **A behavior** | bows / builds / walks a path / holds a sequence | a plugin listener detects it (no typing) |
| **A found object** | brings an item to a place | inventory/PDC check at a site |
| **A callback** | re-submits an earlier answer in a new context | the resolver's unsolved-preference (built) |
| **A "check this"** | the reward is *go verify* — the website changed, an NPC now speaks | a flag flips a downstream surface |
| **A code/combination** | a number unlocks a vault/door | a lock listener |
| **A spoken phrase** | says it in voice chat | Observer Engine transcript scan |
| **Comprehension** | no submission — the world reacts once they've understood and act | the action itself is the proof |

**Infra implication (for BUILD-PLAN):** generalize the oracle from "typed plaintext" to an
`answer_kind` on each puzzle row — `phrase | coords | url_token | behavior | object | spoken |
none`. `phrase`/`url_token`/`coords-as-typed` reuse the current resolver; `behavior`/`object`/
`coords-arrival`/`spoken` are produced by plugin listeners + the Observer Engine that set the
puzzle's flag directly (the gate engine is already built and surface-agnostic).

---

## 5. PER-KEEPER PUZZLE PALETTES (use the character, vary the axes)

Each keeper's puzzles should *feel like that keeper* AND span types. The cipher is at most one
beat per keeper; the rest use the keeper's nature:

- **Vaun (the hoarder):** a **chest-arrange / sorting** puzzle (his hoard, in a deliberate order);
  an **inventory-count** observation; his Caesar stone (the one cipher). Answer types: object, count,
  phrase. Verb: arrange, count, decode.
- **Mara (the reader who never walked):** a **book-cipher** (page/line/word across a lectern shelf —
  built) but the payoff is a **research/cross-reference** ("she read the rite for X; what rite?");
  a **"check this"** answer that sends them to walk where she didn't. Surfaces: lectern → in-world
  travel.
- **Sella (the drowned child):** a **reflection puzzle** (rune legible only in the shore pool);
  her copybook's **drawings-as-clue** (the final wordless leaves predict a real location); an
  **overlay** of two map-arts. Verb: reflect, observe, travel. Answer: coords / comprehension.
- **Orin (won't bow):** an **embodied** puzzle — **bow at the markers in fall-order** (the plugin
  detects); his substitution stone; a **build/arrange** (re-cut a mark). Answer: behavior, phrase.
- **Brann (black moon):** a **temporal** puzzle (only solvable on the black moon / at night); a
  **beacon-colour / counting** (count the fires); an **audio** rhythm (his watch-tolls = morse).
  Answer: temporal-gated phrase, count, listen.
- **Iss (the Liar):** a **logic/deduction** ("his warm reading and the land disagree — which is
  true?"); the **layered** Vigenère whose key is **his own name earned earlier**; a **callback**
  (re-submit the bound word at the M4 gate — built). The catch is a deduction, not a decode.

Plus **cross-keeper / spine** puzzles that use the **external surfaces** (a Drive archive of
"recovered records," a found-footage YouTube clip), the **asymmetric co-op vault** (the Threshold),
and the **Observer-heard** scares.

---

## 6. WORKED EXAMPLES (to copy the pattern)

1. **The Reflection (Sella, M2).** A keeper-stone's face is blank. Standing at the shore pool,
   its reflection in the water shows the rune font (a per-player `TextDisplay` mirrored below the
   surface, or a real carving only readable inverted). VERB reflect, SURFACE in-world,
   ANSWER coords ("south by the far water"), TYPE visual. No letters decoded at all.
2. **The Recovered Drive (spine, M2→M3).** A carved sign gives a string that, googled / entered on
   the record website, resolves to an **unlisted Google Drive** folder named like a salvaged archive.
   Inside: scans (more lore), one image with **spectrogram audio** hiding a name. SURFACE external,
   VERB research+listen, ANSWER phrase (the hidden name), TYPE stego.
3. **The Black-Moon Toll (Brann, M3).** A bell/note-block sequence only plays on the in-game black
   moon; its rhythm is morse for a word. TYPE audio, SURFACE in-world, VERB listen, ANSWER temporal-
   gated phrase. If they're not on at the black moon, the website's ledger nudges "come when it's dark."
4. **The Threshold Vault (co-op, M4).** A sealed room; each player sees a different set of wall-runes
   (per-player `showEntity`). Only by reading them aloud together do they get the combination. TYPE
   asymmetric, SURFACE in-world, VERB combine+speak, ANSWER code. Scales to N (more players → more
   fragments). The Observer Engine may *hear* them solve it and react.
5. **The Spoken Name (Observer, M4→V).** The catch's truth ("the one who turned away") — once a
   player *says it in voice chat*, the Watcher quotes it back on a sign within the hour. TYPE voice-
   heard, SURFACE voice→in-world, VERB speak, ANSWER spoken. The scare IS the answer.
6. **The Found Footage (finale lead-in).** An unlisted YouTube "recording" (HyperFrames-generated)
   surfaces on the record website — the Seventh, in the dark, waiting. The "answer" is a detail in
   the video (coords / a final word). SURFACE external video, VERB observe, ANSWER coords.

---

## 7. WHAT TO BUILD (folded into BUILD-PLAN)

- Generalize the puzzle row with `answer_kind` + the listener/Observer producers for the non-typed
  kinds (most reuse the built gate engine — they just set the flag).
- Build the **per-player illusion** primitives the visual/asymmetric puzzles need (`showEntity`,
  reflection displays, packet light) — see [INTEGRATION.md](INTEGRATION.md).
- Stand up the **external surfaces** (the record website with an input; a Drive archive; 1–2
  found-footage clips via HyperFrames; optionally a Google Voice line).
- Author **2–3 diverse puzzles per keeper** using §5 palettes; keep exactly the 5 letter-ciphers,
  no more, well-spaced.
- Build the **hint rail content** (the `hints` table is empty) — diverse puzzles need diverse hints.
