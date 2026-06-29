# The Observance — STORY-WEB (legend)

> The readable companion to `design/story-web.json` (the graph the editor renders). The JSON is
> the source of truth; this file explains how to read it. Every node and edge is sourced from
> `WEB-MASTER.md` (the spine §1, cipher graph §2, customs §3, Hold-Book §4, M5 composer §5, keeper
> web §6, apparition web §7, divergent endings §8, the plant→payoff ledger §9), `INTEGRATION-V2.md`
> (A1–A15, B1–B4, D1–D11), the canon spine (FACTs 1–17/10b, INV 11–20, the keeper roster, the two
> sixes, the closed custom set), `plugin/.../sites.yml` (real `site_id`s), and `side_quests.sql`
> (the 18 real `quest_key`s). Nothing here is invented; where a thing is not yet built it is a
> design node grounded in a real artifact, never a fabricated mechanic.

**Graph size:** 104 nodes, 170 edges. Every edge endpoint is a real node id; ids are unique and
kebab-case. All 31 plant→payoff ledger rows (`WEB-MASTER §9`) appear as explicit `plants`/`pays-off`/
`foreshadows`/`calls-back` edges carrying a `ledger #N` note.

---

## NODE TYPES

| type | count | what it is |
|---|---|---|
| **movement** | 6 | the spine beats M0→V; the backbone every other node hangs off |
| **prologue** | 2 | the M0 on-ramp surfaces (lure page, vignette) — a separate optional client-side prop, never the server delivery vehicle (Path A) |
| **keeper** | 10 | the six prior keepers + the Seventh + the Watcher + the presiding Keeper NPC |
| **puzzle** | 18 | the gates, stones, and chains a group actively solves |
| **cipher** | 7 | the literacy/decode nodes (Rosetta, reckoning, a1z26, the Vigenère catch, the coordinates) |
| **custom** | 10 | the seven enforced ways + the one group latch (Unlit Deep) + the difficulty engine + the apparition conductor |
| **document** | 8 | found lore artifacts (the forged law, the cause-fragment, the bearing-page, founder margins) |
| **site** | 11 | the physical anchors (Undercroft, seventh-shrine, threshold, Nether/End, carve anchors, room-swap) |
| **sidequest** | 6 | the optional/permanence layer (the three forks, the herd, the Sacred Beast, the false walk) |
| **apparition** | 5 | the shared apparition vocabulary, the offline-skin, the Seventh anti-creature, the keeper-NPC figures, the Ear |
| **surface** | 2 | the cross-surface ARG web (the Record website, the `the-record-keeps` address) |
| **payoff** | 12 | the FACTs and re-read texture that recontextualize on the catch / at the close |
| **ending** | 7 | the Accepting, the M5 composer, the four fates + the INHERITORS codicil + the grave-opens beat |

## EDGE RELATIONS

`foreshadows` / `plants` / `pays-off` / `calls-back` — the plant→payoff spine (the §9 ledger).
`unlocks` / `gates` / `requires` — the activation/dependency graph (who opens what; what defers to what).
`teaches` — a literacy/cipher source feeding a solve.
`leads-to` / `chains-to` — sequence (movement order; FACT chains toward FACT 15).
`hides` — a clue concealed inside a surface (stego, a coordinate, an anti-creature in a shrine).
`rhymes-with` — the soft, non-spotlighting echo (keeper fate ↔ tracked behavior; the two eighths; the two walks).

---

## THE MOVEMENT SPINE (M0 → V)

- **M0 — The Cursed Map** (`prologue`). The remote-group on-ramp: a 15-minute single-player vignette one
  friend plays and posts. The lure page `/record/the-record-keeps` shows a static `kept: 6` and a struck
  seventh row; the dead uploader is Mara's hand (`m.kept`). Ignition is the gathering — the group posts in
  `#the-record` and arrives pre-ignited. Mints nothing; a surface + a prop + one voice key.
- **Movement I — The Notice** (Act 1). The record opens; the group is graded by laws no one told them
  (FACT 1, 2). The single calibrated-loud beat is the Cold-Start Prologue (one lit marker, the re-staged
  first report). The frame-break says the `6` back — a map cannot know that. Literacy opens by two genuinely
  different doors (the rune-ring leap, the `a1z26` rung). The Hold-Book appears.
- **Movement II — The Ways** (Act 2 open). The six keeper-stones become a field, not a row (any order,
  resolver ignores movement). Iss is met warmest (the trap); his false coordinate leads to the dead-shrine.
  The forged eighth law is planted; the future-dated grave is carved; the first living-name carve lands; the
  difficulty engine goes live (FACT 2b); whispers go live.
- **Movement III — The Undercroft / Seventh** (Act 2 mid). The kept descent (the one fire, FACT 11) mirrors
  the unkept descent (the Seventh's doused deep). The Unlit Deep arms. The Nether deepening-lane opens (the
  fire's source, "below the below"). The offline-skin apparition fires its first deniable glimpse.
- **Movement IV — The Catch** (Act 2 close). The Iss lie catches on `iss_caught` — the universal hinge.
  ~six threads re-read cold at once: the prophet's wall, the forged law, the Hold-Book down-count, the
  Seventh unwriting (`seventh_named`), the offline-skin (named), the Record website, the UNKEPT acrostic,
  the name-carves. The single Iss chain runs: catch → bound word → coop gate → true coordinate → true walk.
  The End deepening-lane opens.
- **Movement V — The Accepting** (Act 3). The collective bow fires (active-only quorum, INV-19); the single
  M5 composer reads every colorant and emits a bounded close. The grave opens from inside on its date; the
  Hold-Book's last page rewrites; the divergent fate resolves; FACT 14 → 15 is delivered by what happens.

---

## THE TOP CALLBACK CHAINS (the "oh, that is what that was for")

**1. The `6` (the day-zero number).** The lure page's `kept: 6` (ledger #24) reads as a dead file's tally.
The frame-break says it back (a map can't know it). It re-reads soft at M2 (six downloads ≈ six keepers),
hard at M4, and resolves at V when `/record` adds the group's names and the struck seventh row fills: six
prior groups were kept the same way; this group is the seventh the record will not keep *as a file* — it
keeps them as hands. The single plant that spans every movement.

**2. The Hold-Book (one book, two faces).** The down-count `lamps kept:[N]` (ledger #6) reads as a
doom-clock — the bait for Iss's lie — and re-reads at the catch as the *muster of present hands*. The blank
give-back column (ledger #5) reads as a logging toy and re-reads as the record filing the living into a
keeper's empty column. Both faces rewrite at V to "the present hands are entered" — the same book as
everyone above.

**3. The Liar chain (Iss).** Iss warmest (ledger #1) → the false coordinate → the dead-shrine walk → the
prophet's wall ("read who carved it, after," ledger #19) → the Vigenère catch flips him cold and re-colors
his whole tree. The catch is the hinge that unlocks the bound word, the UNKEPT key, the cold Record card,
the named offline-skin, and the forged-law collapse — and only then yields the true coordinate.

**4. The false walk → the true deep.** Iss's false coordinate sends the group to the cold-hearth surface
(`dest-dead-shrine`, ledger #2/#17). The same hearth's *deep* (`the_unwriting`) is the Seventh's grave,
sealed until `iss_caught` + `seventh_named` — the herring's destination becomes the spine's deepest room.

**5. The fire is a carrying.** FACT 11 (the one fire, the Undercroft) deepens through the Nether lane: the
kept fire's source is "below the below" (Brann's framing, ledger #28; the bearing-page, ledger #30). The
keeping was always a *carrying* — which quietly sets up the induction: you, too, will be carried, not owned.

**6. The grave is an appointment.** The future-dated grave (ledger #21, with the founder margin ledger #10)
reads as a death-clock counting down to a named person. Its date *is* the single Accepting instant; at V it
opens from the inside as the rite fires — `KEPT — NOT YET` → `KEPT`, the mound the deposit slot. The misread
*was* the mechanic.

**7. The UNKEPT acrostic.** The six maker's-mark glyphs (ledger #3/#8), planted M1, read in *fall-order*
(Vaun, Mara, Sella, Orin, Brann, Iss) spell `UNKEPT` — the word each keeper failed to keep. Legible only
after the catch recolors the six. Gates nothing; pure recontextualizing texture; self-correcting (it fails
in ring-order).

**8. The convergence on FACT 15.** Seven independent foreshadows chain into the sealed reveal: FACT 1 (the
list) → the receiving; FACT 10 (the land can refuse) → what *yes* costs; FACT 11 (one fire) → "the kept
ones did not simply leave"; plus the Hold-Book rewrite, the herd's full pale field, the grave's deposit
slot, and the Ear's word-axis (FACT 17, ledger #22). No single sentence states it — it is delivered by what
happens at the close.

---

## CONSISTENCY NOTES (the laws the graph encodes)

- **No orphans.** Every mechanic node connects to a story/clue/lore/NPC node and vice versa. The forks and
  the deepening lanes `calls-back` the M5 composer (they color, never gate — INV-12); the apparition
  conductor `gates` every ambient lane (INV-18); the Nether/End `pays-off` FACTs that already ship.
- **Precision.** Per-player surfaces (`name-where-carve`, `offline-skin-apparition`) `rhyme-with` each other
  under the separation law (active-only vs offline-only, INV-16); they never spotlight the divergence extremes.
- **One owner each.** The single Accepting instant is shared by the grave, the website, and the summons; the
  M5 composer is the single ending writer; the Hold-Book is one book on one anchor. The graph routes every
  ending colorant *through* `m5-composer`, never directly to a fate.
