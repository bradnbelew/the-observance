# 02 — Deep Hold: Content & Puzzle Rebuild

**Goal:** rebuild the ~75 non-Unlit investigation nodes (cases C01–C10 minus the Unlit BI set already
done) so each is **investigation woven with story**, hard the *right* way, and never a walk-up
mechanism or a naked cipher. This is the substance of what Brad asked for — not a reskin of the current
"arrange items, pull handle" nodes.

Read `01-HOLD-LAYOUT-REBUILD.md` first: the content lives in the rebuilt rooms, and the two must be
designed together (rule 3 — the environment IS the puzzle's setting).

---

## 1. The problem, measured

Across all 82 nodes (source: the critic census + `design/ARG-V5-PHYSICAL-PREDICATES.json`):
- **35%** resolve by typing an exact phrase into an oracle (sign / Discord / web form).
- **33%** resolve by putting a tagged item into an enumerated barrel slot. The token `slot 13` appears
  **31 times**; the word `handle` appears **135 times** — ~34 nodes literally end in "pull the handle."
- **23%** are "rotate item frames to exact ordinals."
- The **same rotation answer `[0,2,4,6,0,2]` is required in three separate nodes** (KO02, HS05, AR05).
- **~8 of 14 ciphers are naked** — the decoder/key is printed in the same room as the ciphertext
  (Atbash coached by the item lore "turn the paper, not the alphabet"; the LS02 A1Z26 strip beside its
  cipher; KB02's "three rails" stated next to the rail cipher; the full Bacon table beside HS03).
- Count-only feedback + free retries lets a group **brute-force** every frame/dial node in minutes.

Net effect: whatever the fiction says, the player's muscle memory is *arrange things → pull handle →
read the count → rearrange.* A filing cabinet, 82 times, including in a haunted village.

The genuinely good cases prove the team can do better and are the **template, not the target for
rewrite**: **C04 (Cistern Winter), C07 (ASH-13 camp), C08 (Wren)** already do real investigation with
keyed ciphers and evidence chains. Study them; make the rest match.

## 2. The layered-difficulty formula (Brad's exact ask)

Brad: *"not just a cipher that can be solved with a decoder, but an investigative puzzle, mixed with a
cipher, mixed with research, lore knowledge requirements, callbacks to older things."*

A **hard node** = as many of these layers as fit, in this shape:

1. **Investigate** — the evidence is distributed across a real place (doc 1). The player has to *find*
   and *read* it, not walk up to one station. (e.g. cross-reference three ledgers in three ranks.)
2. **Notice** — the answer requires *attention*: a discrepancy between two records, a forged mark, a
   count that's off by one, a date that predates an order. Not a lookup.
3. **Transform (cipher) — keyed by something earned.** If there's a cipher, its **key is deduced or
   physically obtained**, never printed beside it. The gold-standard model already in the game is
   **A03→A04**: sorting physical evidence *yields* the transposition key that decodes A04. Replicate
   that pattern: the cipher's key is the output of an earlier physical/deductive step.
4. **Recall (callback)** — completing it requires knowing something established earlier (a keeper's
   habit, a bell timeline, a prior case's conclusion). This is what makes a 15-hour arc *cohere* and
   punishes skipping.
5. **Submit — obviously.** The conclusion goes to a clearly-signposted surface (an answer sign, a
   Discord `/answer`, a web form). The fiction points at it; `/progress` backs it up.

Not every node needs all five, but a node with only "transform" (a naked cipher) or only "arrange"
(a slot deposit) is exactly what we're removing.

## 3. Kill the four-verb monoculture — the new verb palette

All of these are **deterministic and Minecraft-safe** (doc 4). Rotate them so no single verb dominates:

- **Read + cross-reference** books/ledgers/signs across rooms; the answer is the *discrepancy* between
  them. (Minecraft-safe: books in lecterns, signs on walls.)
- **Count / measure the world itself** — shelves, bays, bells, tally marks, wick layers, grave rows.
  The build is the data (doc 1 makes this possible). No mechanism, pure attention.
- **Assemble a reading from distributed evidence** — three rooms, one conclusion the player *states*.
- **Compare original vs. forgery** — a real record and an altered copy; the player identifies the
  alteration. (Already done well in CW07; generalize it.)
- **Follow an earned chain** — record A names where record B is; B, once found, keys C's cipher; C is
  the answer. (The A03→A04 pattern.)
- **Physically obtain a key** — excavate/claim an item whose lore or shape *is* the cipher key, only
  reachable after an earlier step.
- **Then submit the deduced conclusion** — sign / Discord / web, clearly.

Keep item-frame and container mechanics only where they're genuinely the best fit (a valve roster, a
map overlay) and never as the *default*. When you do use them, the arrangement must be *deduced*, and
feedback must not enable brute force (see §5).

## 4. Submission clarity (Brad's new hard requirement)

*"they should know somehow eventually where to submit it if that's relevant."* For every node:
- The fiction names the surface: a records desk says "file findings at the inquiry desk"; a keeper's
  book says "the record answers to the name spoken at the shrine"; the website ticket says where the
  reply goes.
- The `/progress` Discord command (already shipped) lists every open finding and which of the three
  channels (world / Discord / site) takes it — this is the safety net so no group is ever stuck *only*
  on "where does this go."
- The answer-sign / `/answer` accept lists must accept reasonable phrasings of the *correct* deduction
  (not one exact string), while still rejecting wrong theories. Wrong-vs-wrong-phrasing feedback must
  be distinguishable (the critic flagged nodes where "the desk does not accept the finding" couldn't
  tell a player whether their theory or their wording was wrong — fix that in the accept lists and the
  feedback text).

## 5. Anti-brute-force (so "hard" is deduction, not iteration)

- **No naked keys** (§2.3). If it can be solved by trying the decoder in the same room, it's wrong.
- **Rate-limit or gate multi-state mechanisms.** Frame/dial nodes must not give count-only feedback
  with unlimited free pulls. Either (a) require the arrangement to be *deduced* from evidence so there's
  one obvious attempt, (b) rate-limit the evaluation (answer signs already are), or (c) make a wrong
  pull cost something in-fiction. Do not ship a 6-frame × 8-rotation puzzle with a free, instant handle.
- **Retire the reused answer.** `[0,2,4,6,0,2]` appearing three times means the third time is a
  keystroke callback, not a knowledge callback. Callbacks reuse *knowledge*, not the exact input —
  re-derive a fresh arrangement from the recalled fact.

## 6. What must NOT regress (fairness engineering)

The recovery/fairness layer is best-in-class and every rewritten node must keep it:
- **No-touch completion** (a node never completes just by a player brushing a fixture).
- **Atomic wrong-item returns** (a wrong deposit is returned whole, never consumed).
- **Never-deleted evidence** (protected source items can't be destroyed or duplicated via repair/
  restart).
- **Idempotent gates and durable local-primary progress** (a solve survives restart/reconnect/
  duplicate submission; the network mirror can never authorize or void a physical solve).
- **Duplicate-artifact protection** (one protected copy; no portable dupes).

These live in `plugin/.../v5runtime/mechanics/` and `.../container/`. When you change *what* a node
requires, you change the predicate data and the fiction — you should rarely need to touch the recovery
engine itself. If you do, re-run its self-tests (doc 8).

## 7. The predicate authority — shape, and the hash discipline

Every node's machine contract is one object in `design/ARG-V5-PHYSICAL-PREDICATES.json`. Shape:
```jsonc
{
  "node_id": "KM02",
  "owner": "plugin",                     // plugin | plugin_unlit | plugin_finale | discord | website
  "prerequisites": ["v5_..._complete"],  // gate flags that must be set first
  "site_id": "mara_lectern_3",           // the fixture/room it binds to (must exist in the Hold plan)
  "handler": "answer_sign",              // the runtime evaluator kind
  "completion_flag": "v5_km02_...",      // the flag it sets on solve (downstream gates read it)
  "predicate": {
    "kind": "...",
    "evaluation_trigger": "...",         // in-fiction, what the player does to submit
    "components": [ ... ],               // exact blocks/frames/slots/items and their required state
    "all_of": [ ... ],                   // the operations that must all be true
    "commit": "..."                      // what is persisted
  },
  "wrong_input": { "policy": "...", "feedback": "...", "state_effect": "..." },
  "reward": { "description": "...", "artifact_ids": [...], "delivery": "..." },
  "reset_repair_recovery": { "reset": "...", "repair": "...", "recovery": "..." },
  "concurrency_replay": { ... },
  "durability_profile": "minecraft_local_primary"
}
```

**THE HASH RULE (this is how you desync production if you get it wrong — see doc 7):**
`ARG-V5-PHYSICAL-PREDICATES.json` → its SHA-256 → `settings.v5_physical_authority_sha256` in Supabase →
the plugin's node contracts and the tools. At handoff all four agree on
`37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b`. **Any** edit to this file changes
the hash. When you change it you must, as one unit: (a) update the Supabase seed
(`discord/supabase/seeds/v5_investigations.sql` sets `v5_physical_authority_sha256`), (b) re-run the
Supabase bundle build/check, (c) re-run `tools/check_v5_physical_predicates.py` and the plugin
self-tests, (d) confirm the runtime node contracts still parse. Never land a predicate change without
the hash re-sync. This is your (Codex's) domain, not the plugin operator's.

Node counts are contractually fixed: **10 cases, 82 required nodes, 60 plugin-owned physical
predicates.** If a rebuild changes a node's *mechanism* keep the node_id and completion_flag stable so
the case graph and gates don't move. If you genuinely add/remove a node, every counter in doc 8 and the
Supabase tables must move together — treat that as a schema change and prompt Brad before doing it.

## 8. Worked example — rebuilding C02 (currently the weakest case)

C02 "The Long Cold" today: order three overlays, do arithmetic ("174 ÷ 2 = 87"), touch three books,
decode a taught script, submit three words, set four dials to values already shown. The critic graded
its difficulty **F** — arithmetic and taught substitutions a sharp group clears in 25 minutes, and LC03
is literally a receipt-stamping tutorial. Here is the shape of a rebuild that keeps its node_ids and
completion_flags but makes it real. (Illustrative — fit specifics to the final library/service
floorplan from doc 1.)

**Setting:** the service works' **lamp bench + cistern room** (doc 1 builds these as real places).

- **LC01 (was: order three survey overlays).** Keep the three survey layers as physical map frames on a
  real drafting table, but the *conclusion* is a discrepancy: the civic-ring layer was drawn over the
  first-shelter layer, and one structure appears in the wrong place between them. The player states
  *what moved*, deduced by comparing real overlaid maps — not "arrange in age order and pull."
- **LC02 (was: 87 × 2 arithmetic).** Replace the naked arithmetic with a *record cross-check*: the heat
  ledger says "public issue 174 baskets"; the residency roll (a different book, in a different rank)
  lists the certified residents; the *ratio* only resolves the winter-issue rule if you find the rule
  card posted at the stores counter (a callback the player must locate). The number falls out of
  reading three real records, not one division printed on a page.
- **LC03 (was: touch marked copybook, stamp three receipts — a tutorial).** Rebuild as a **forgery
  spot**: a school-day copybook exists in the library, and a *second* copy exists in the archive with
  one altered line. The player identifies the altered line (compare original vs forgery — a real verb),
  and *that line* names the cistern intake to inspect next. No receipt-stamping.
- **LC04 (was: taught-script substitution, decoder card adjacent).** The taught-script card is
  **removed from the room.** The substitution alphabet is instead learned earlier (a callback: the
  Orientation teaching strip the player already used in C01), so decoding LC04 requires *recalling* a
  key from hours ago, not reading it off an adjacent card. The plaintext points at a physical cistern
  valve, which the player then reads.
- **LC05/LC06 (dials).** The dial arrangement is **deduced from the wick-layer chronology** the player
  read at the lamp bench (a real bench, doc 1), and set once — not brute-forced. Rate-limit the pull.

**Difficulty layering achieved:** investigate (three rooms), notice (a forgery, a discrepancy),
transform (a cipher keyed by an earlier-earned alphabet), recall (the C01 teaching strip, the stores
rule card), submit (an answer sign at the inquiry desk, signposted). No arithmetic-on-a-page, no naked
decoder, no receipt tutorial.

**Move it as one unit:** the rebuilt books/signs/items (docs 2–3), the new room furniture (doc 1), the
updated `ARG-V5-PHYSICAL-PREDICATES.json` for LC01–LC06, the Supabase hash re-sync (doc 7), the
`SOLUTION-CASEBOOK.md` update, and the audits (doc 8). Keep every `node_id`/`completion_flag`.

## 9. Deliverable per case

1. A short design note (in `SOLUTION-CASEBOOK.md`, the current-authority solutions file) describing the
   rebuilt investigation chain and every layer it uses.
2. The rooms it lives in built and dense (doc 1), with all referenced evidence physically present
   (rule 4 — no contradictions).
3. Rewritten books/signs/item lore in the authority files (`arc/v5/minecraft-books.json`,
   `arc/v5/evidence-item-*.json`, and the plugin sign/lectern text) that meet the prose laws (doc 3).
4. Updated predicate JSON + hash re-sync + green audits (docs 7–8).
5. A dry run through the case's node chain (the simulation harness + a real read) confirming it's
   *solvable and reasonable*, hard but fair, with an obvious submission point.

Do it **one case at a time**, keeping green and committing between cases. Start with C02 (weakest) or
extend C04/C07/C08's proven pattern outward — do not rewrite all ten on paper before building one.
