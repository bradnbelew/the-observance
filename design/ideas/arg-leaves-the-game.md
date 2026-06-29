# IDEA — The ARG Leaves the Game (The Record website + stego + real-clock)

> Design treatment. Spoiler-bearing in §4 (THREAD IT) and §5 (PAYOFF). Operate inside
> the canon-spine FACT/INV web and the cipher web (`design/cipher-web.md`). Verdict at the
> top so synthesis can read it cold.
>
> **BUILD-STATE NOTE (2026-06-25 reconciliation).** Three of the four code pieces are
> already SHIPPED and self-tested. This treatment is reconciled against the as-built code:
> what is `[BUILT]` is named with its real file/symbol; what is `[TODO]` is the short
> remaining list in §6. Earlier drafts proposed `embedRuneLayer`, `record-projection.ts`,
> and an encoded-timestamp clock as *new* — they exist now. Do not re-propose them.

**VERDICT: KEEP-SCALED.** Ship two of the three sub-threads as P1 arc-spine depth; the
third is scaled to a non-gating ambient leaf. Specifically:
- **The Record website** (`/record`) — KEEP, P1. The single highest-leverage piece: the
  off-Minecraft face of FACT 1/14, and the one surface that makes "it knows your name"
  legible to a YouTube viewer who never logs into the server. Projection `[BUILT]`; the
  Next route is the one substantive `[TODO]`.
- **Stego (P17) in the forge cards** — KEEP, P1. Carries exactly one high-value key (the
  Iss Vigenère key, `ISS`) across two surfaces. `[BUILT]` — `discord/src/forge/stego.ts`,
  self-tested (`stego.selftest.ts`, `stegoSelfTest`). Both the preferred visual rune-layer
  AND the LSB fallback shipped (see §2f for why LSB was kept, against the earlier draft's cut).
- **Real-world-clock event** — KEEP-SCALED to **encoded-timestamp**, P2. The astronomical
  new-moon gate was a camera/scheduling liability (CRITIQUE §2c) and is CUT (`clock.ts`
  header records `A13: CUT the astronomical new moon → encoded timestamp`). Replaced by a
  showrunner-bound, deterministic, set-once UTC instant. `[BUILT]` — `clock.ts`
  (`bindAcceptingInstant`, `instantReached`, `encodeTimestamp`).

---

## 1. EXPOUND — the full mechanic + story + mystery treatment

### 1.1 The premise, in one breath
The keepers kept a record. In-world it is a stone lectern and a Discord pin (`#the-record`).
But a record outlives the place it was kept in. **One thread of that record has leaked onto
the open web** — a bleak page the group did not cause and cannot stop. It is partial,
redacted, and it updates *itself* as the group is observed. The site is not a hint engine and
not a walkthrough; it is the same cold ledger voice, pointed at the players from outside the
game they thought contained it.

This is the "From The Fog, but it knows your name" thesis taken to its literal edge: the dread
is no longer bounded by the Minecraft client. You close the game; the record is still open. It
was open before you (`voice.recordOpened()` → `▒  the record is open. it was open before you.`).

### 1.2 The three sub-threads and how they bleed out

**(A) THE RECORD — a static web archive at a rune-carved URL.**
A bleak, near-static page in the cream/navy editorial palette degraded toward ash/ink — the
same palette the forge cards already use. It presents as a **recovered archive index**: a list
of "entries," most struck through with a redaction block (`REDACTED_GLYPH = '████████'`,
`record-projection.ts`). A handful are legible. The content is a **pure projection of the live
arc state** (a read-only, spoiler-free snapshot), so the page is *true* to the server but never
interactive.

The URL is not given. It is **earned**: a rune string carved in-world (and mirrored on a Discord
card) decodes — via the keepers' substitution alphabet (`runes.ts` / P4) — to a path segment.
The host is fixed and plain (the dashboard domain, a `/record` route, `noindex` like the rest of
the dashboard — `layout.tsx` already sets `robots: { index:false }`). The carved founder line
decodes to `the-record-keeps` (canon: `arc/lore/documents/kept-in-more-than-one-place.md`,
front-matter `answer:`), normalized to the slug → `/record/the-record-keeps`. A wrong decode
404s to a single in-voice line; it never leaks the real path. Finding the site is itself a P4
substitution solve that happens to point off-world.

What the Record shows, by movement — these are the **as-built** projection rules
(`record-projection.ts` `project(signal)`), driven by a 3-field coarse signal
`{ movement?, stonesRead?, accepted? }` and nothing wider:
- **M1:** the season line (`SEASONS[1] = 'the notice.'`); the six stone entries all withheld
  (struck blocks); footer `0 of 7 kept. the rest is not yet kept.` The page is almost all
  redaction — it withholds, which is the keeper register's whole tone.
- **M2:** the six `STONE_ENTRIES` un-redact **one per stone actually read** (`legible: i <
  stonesRead`), in lockstep — never ahead (REVEAL DISCIPLINE). Each is a two-line cold ledger
  fragment naming the *dead keepers'* fates (`'the first was kept. the offering was not made.'`
  …). These are the keepers' own dead names, never a living player (INV-16 / privacy law).
- **M3:** season → `'the descent.'`; more stones legible as the group reads them. The closing
  entry stays withheld.
- **M4:** season → `'the catch.'` The Iss thread re-reads in-game (the Liar engine); on the
  website the *same* projection now reflects the caught state. (Mechanically: the Iss stone's
  read-state and the cooled register both flow from real flags; there is no second authored
  Iss text on the site — the cold reading is the in-game one, surfaced.)
- **M5:** on `accepted === true`, the closing entry un-redacts:
  `'the present hands are entered. the count holds.'`, season → `'the accepting.'`, footer →
  `… the record is closed.` The index stops being purely an archive of the dead and enters the
  present hands. The page becomes evidence the group joined the thing (FACT 14/15) — delivered
  by *what the page now lists*, never by a sentence.

**(B) STEGO — the mark beneath the mark (P17). `[BUILT]`**
`discord/src/forge/stego.ts` hides exactly one payload — `ISS_STEGO_PAYLOAD = 'ISS'`, the
Vigenère key for `STEGO_PUZZLE_KEY = 'stone-iss-wall'` (P3) — on the Iss card, by two
deterministic, self-tested schemes:
- **Rune-layer (preferred):** `embedRuneLayer(svg, payload)` returns a faint second carved-rune
  `<g>` (opacity ~0.14) composited beneath the primary carving. Because it is *real ink, just
  dim*, it survives Discord's PNG re-handling exactly like the primary carving, and it is read
  the way every carving is — by eye + the Rosetta. A true diegetic "second door."
- **LSB (fallback):** `embedLsb(frame, payload)` packs a self-describing envelope (magic +
  version + length + FNV checksum) into the RGB LSBs, with a self-contained pure PNG codec
  (`encodePng`/`decodePng`) proving the bits survive a lossless PNG round-trip.

The payload is ONLY ever the key the seed already binds (`stegoSelfTest` asserts
`ISS_STEGO_PAYLOAD === 'ISS'` and the puzzle key never drift). It invents no plaintext and
touches no cipher. It is additive: `ISS` is *also* the fair in-corpus key (`D09` hands it in
fiction). The stego is a second in-door to `iss_caught`, never the only door (≥2-in-doors rule).

**(C) REAL-CLOCK — the encoded timestamp (scaled from "real new moon"). `[BUILT]`**
`clock.ts` binds **one** Accepting instant, set-once, as a deterministic future offset from a
stable anchor (`bindAcceptingInstant`: `anchorMs + leadMs`, `leadMs` default ~2 days). It is
**immutable once bound** and idempotent (a re-run returns the same instant). `instantReached`
is the single `not_before` predicate the grave-open, the summons, and the website re-render all
consult (so they can never disagree — §8.2 frozen anchor: one instant, three surfaces).
`encodeTimestamp(instantMs)` renders it in the in-world digit form `YYYY.DDD.q` (year ·
day-of-year · quarter-of-day) — a recognition token, never a typeable coordinate answer
(INV-14). The Record publishes this encoded string as "the way opens at"; at the instant, the
M5 "received" projection becomes reachable and the in-game `voice.summons()` fires. The wait is
hours, not weeks, and it is *visible on camera as a countable instant* — the bleed without the
liability.

### 1.3 How it plays across the ~2-week / 5-movement arc
- **M1 (The Notice):** the founder margin (`kept-in-more-than-one-place.md`) is readable; its
  keyed line decodes (P4) to `the-record-keeps`. A group that reads it as a *path* finds a page
  on the open internet that already knows the count has begun. First "it left the game" jolt.
- **M2 (The Keeper-Stones):** the Record un-redacts in lockstep with stones actually read. The
  Iss card, when forged, carries the stego layer; a paranoid group that examines it gets the
  Vigenère key early — a reward for suspicion that changes nothing they couldn't earn from `D09`.
- **M3 (The Seventh / Undercroft):** the seventh thread stays a withheld block; the page
  foreshadows the archive is not closed.
- **M4 (The Catch):** `iss_caught` lands in-game; the Liar's warm reading is overturned. Anyone
  who screenshotted the M2 page sees the page now reads cold — the page lied the same way Iss
  did. The encoded timestamp is published.
- **M5 (The Accepting):** at the bound instant, after the rite is detected, the closing entry
  un-redacts: the group's present hands are entered. The site stops being only about the dead.

---

## 2. CRITIQUE — adversarial, honest, with mitigations

**2a. ORPHAN RISK (the deepest risk). A website is the easiest thing in this whole project to
build as a disconnected gimmick** — a cool page nobody's mechanics point at, that the lore never
foreshadowed, that pays off nothing.
- *Mitigation (load-bearing, and as-built):* `record-projection.ts` is a **pure projection of
  real `arc_state`** — it renders *nothing it does not read*. Every un-redaction is gated on a
  coarse signal the engine already produces (`movement`, `stonesRead`, `accepted`). It adds
  **zero** new canon facts; it is a new *surface* for FACT 1/8/11/14/15 that the spine already
  requires on ≥2 surfaces (§4). The founder plant is canon (`kept-in-more-than-one-place.md`),
  so the page is the *payoff of a planted line*, not an orphan.

**2b. ANTI-JANK / reveal discipline.** The Record updating "live" courts the cardinal sin:
something witnessed mutating.
- *Mitigation (as-built):* the projection is **pure + side-effect-free + no client JS** (the
  module header mandates server-render, no polling). A change is only ever seen on **reload** —
  the player re-opens the record and finds it already different, exactly the in-game rule (leave
  line-of-sight, then it has changed). No websockets, no animated dissolve. The route MUST keep
  this: server component, no `'use client'`, no revalidation interval shorter than the showrunner
  pass.

**2c. THE REAL NEW MOON IS A TRAP — and was CUT.** A literal astronomical-new-moon gate (a)
couples the climax to a date no one controls; (b) is unverifiable on camera (told, not shown — a
slop failure); (c) has no deterministic fallback on a calendar misread.
- *Resolution (shipped):* `clock.ts` records `A13: CUT the astronomical new moon → encoded
  timestamp`. The instant is bound from a stable in-arc anchor (the catch time), is in the DB,
  is set-once + idempotent, and is *visible* as a countable instant. The lunar *flavour* is kept
  by tying the chosen instant near the in-game black-moon phase the plugin already computes
  (cipher web P13), so it still rhymes with Brann without depending on the sky over the players'
  houses. No further action — this risk is closed in code.

**2d. PRECISION / "it knows you" misfire.** The most tempting and most dangerous feature: the
Record showing a player's real name or habit. A wrong personalization is worse than none.
- *Mitigation (as-built and STRICTER than the earlier draft):* `record-projection.ts`
  **personalizes NOTHING.** Its signal is three coarse group fields; it has no player input, no
  name, no token, no IP. The module header states it: "it personalizes NOTHING … it can only
  ever speak of the group." The earlier draft's per-player-signed-link idea is **CUT** — the safe
  ceiling is "the record speaks only of the group's coarse progress." "It knows your name" is
  carried *in-world* (the living-name carve, `name-where-never-been`), never on the public web
  page. The route MUST NOT add any per-visitor personalization.

**2e. PATH A (one-click).** A website is *perfect* for Path A — no install, just a URL. The only
watch-item: the rune→URL decode uses the substitution alphabet the group learns from the Rosetta
(P4 / `D03`), so it is in-corpus and needs no external tool.

**2f. STEGO — KEEP BOTH SCHEMES (revised from the earlier "cut LSB").** The earlier draft argued
LSB is fragile because Discord re-encodes attachments. As-built, this is mitigated, not a reason
to cut: `stego.ts` ships a **self-contained pure PNG codec** and `stegoSelfTest` proves the LSB
payload survives a lossless PNG encode→decode (PNG is lossless; Discord stores PNG attachments
losslessly). The LSB envelope is **fail-closed** (FNV checksum → a corrupted/absent payload
decodes to `null`, never a false "it knows you" string). So LSB is a safe *programmatic backstop*
behind the visual layer, not a liability. Decision: **keep both**, visual as the diegetic primary,
LSB as the deterministic fallback. (The one constraint: post the Iss card as a **PNG**, not a
re-compressed JPEG, so the LSB lane holds — the forge already emits PNG.)

**2g. CONFUSION / "is this even part of it?" on camera.** A web page with no framing could read
as "the creators made a marketing site," puncturing TINAG.
- *Mitigation:* the site carries **no meta-framing, no credits, no nav chrome** — a bare archive
  in the cold keeper register, `noindex`, reachable only by the decoded path. It looks found, not
  published. The in-game carving that yields the path is the only authored pointer, and it points
  *in-voice* (`kept-in-more-than-one-place.md`: "the record is kept in more than one place").

**2h. SPOILER SURFACE.** The Record is a public URL projecting real state — if it ever renders a
sealed fact early, it spoils the twist for anyone who guesses the path.
- *Mitigation (as-built):* the projection consumes only a **coarse spoiler-free signal** (the
  same boundary the dashboard's spoiler-free views enforce — SECURITY DEFINER views, never a raw
  table, sealed flag, player, or custom label). M5's closing row is gated on the real `accepted`
  signal, so it cannot appear before the rite is performed. The route MUST read the neutral
  `v_record`/spoiler-free view, never `arc_state` raw or `cipher-web-seed.sealed.json`.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (cold, plain, concrete)

The Record / Archivist register: third-person, ledgerlike, names names, never emotes. These pass
the anti-slop law (no announced emotion, no thematic bow, no three-adjective lists, no "not just X
but Y", no em-dash drama). The first four are **as-built** strings from the canon corpus; the rest
are proposed in the same register for the unbuilt route copy.

> Masthead (verbatim, `voice.recordOpened()`):
> `▒  the record is open. it was open before you.`

> A withheld stone entry footer, M1 (as-built, `record-projection.ts`):
> `0 of 7 kept. the rest is not yet kept.`

> The M5 closing entry (as-built, `record-projection.ts` `CLOSING_ENTRY`):
> `the present hands are entered. the count holds.`

> The encoded-timestamp line (proposed route copy, M4 publish — plain, no countdown drama):
> `the way opens once. the hour is entered in the old marks. it does not open twice.`

(Note the omissions: the page never says the group "became" anything, never explains, never warns.
It lists. The dread is in what the new row *is*, not in any adjective.)

---

## 4. THREAD IT — exactly where this lives so it is not an orphan

### Canon-spine FACTs it touches (adds NONE; it is a third door for existing facts)
- **FACT 1** (the record keeps a list of the living, by name) — the Record website is the
  off-world *third path* for FACT 1, which §3's web rule already says needs ≥2 paths. M1 masthead
  + the page's existence.
- **FACT 8** (Iss lied; forces re-walking a "solved" clue) — the M4 cold reading of the Iss thread
  is the website expression of the Liar engine; the stego key feeds P3 directly (`stone-iss-wall`).
- **FACT 11** (one fire never went out) — surfaced in the descent season as the index of the dead
  reads against the one entry still legible.
- **FACT 14** (the record receives — it keeps you) — M5 closing entry, `'the present hands are
  entered.'`
- **FACT 15** (to be accepted is to become part of the watching) — delivered by the page entering
  the present hands into the same archive as the dead, never by a sentence. SEALED-felt.
No new FACT is minted. (The candidate sub-fact "the record is kept in more than one place" is
realized as the canon **document** `kept-in-more-than-one-place.md`, not a numbered FACT — correct.)

### Found-documents / journals that foreshadow it (`[BUILT]`)
- `arc/lore/documents/kept-in-more-than-one-place.md` — the founder margin carrying *"the record
  is kept in more than one place, against the loss of the first"* and the keyed line decoding to
  `the-record-keeps`. This is the in-corpus justification + the URL plant. Cross-listed in
  `arc/lore/found-documents.md` and `LORE-BIBLE.md`. **Present and correct.**
- `the-record-opens.md` / seed `m1-record-opens` (`puzzles_seed.sql`) — the base-lectern record
  fragment: *"the record was open before you found it… it does not close at the rite."* One voice
  with the website masthead.
- The Iss pair `the-ways-are-a-wall.md` ↔ `no-wall-was-ever-built-here.md` — the warm/cold Iss
  readings the website's M2→M4 shift mirrors. One voice across document, stone, and page.

### NPC / Watcher voice lines that carry it (`discord/src/voice.ts`)
- `voice.recordOpened()` `[BUILT]` — reused verbatim as the Record's masthead (one voice on every
  surface).
- `voice.summons()` `[BUILT]` — fires at the encoded instant.
- **`[TODO]`** two new keys, lowercase-spare, no English in payloads:
  `voice.recordElsewhere()` (the M1 Discord nudge that the record is kept off-world too) and
  `voice.recordReceives()` (M5). Add to the `voice` object; add to `OracleVoiceKey` only if a
  puzzle outcome row references them (the `record-url` lore row, below).

### Cipher(s) / puzzle(s) it expresses (reuse the 11 built ciphers)
- **substitution (P4)** — the carved founder line → URL slug `the-record-keeps`. Reuses `runes.ts`
  `RUNE_MAP`; the decode is literacy the group already has. No new cipher.
- **vigenere (P3)** — the stego payload IS the `ISS` key; the P17→P3 chain spans two surfaces +
  an image editor. Reuses `ciphers.ts` `vigenere` + `stone-iss-wall`.
- **coordEncode digit family (P6)** — the encoded timestamp `YYYY.DDD.q` carves in the digit
  glyphs / `.` mark already in `runes.ts`. No new cipher.
- **P17 stego** — `[BUILT]` (`stego.ts`), the one reserved "new forge step," now shipped.

### Beats / listeners / tables / seed rows / route / voice keys that realize it
- **Projection (pure):** `dashboard/src/lib/record-projection.ts` `[BUILT]` + `.selftest.ts`
  pinning every redaction. `project(signal) → RecordProjection`.
- **Stego forge step:** `discord/src/forge/stego.ts` `[BUILT]` (`embedRuneLayer`,
  `stampRuneLayerPayload`, `embedLsb`/`extractLsb`, `encodePng`/`decodePng`) + `stego.selftest.ts`.
- **Clock:** `discord/src/showrunner/clock.ts` `[BUILT]` (`bindAcceptingInstant`,
  `instantReached`, `encodeTimestamp`), wired in `autonomy.run.ts` (imports
  `bindAcceptingInstant`, `instantReached`).
- **`[TODO]` Dashboard route:** `dashboard/src/app/record/[slug]/page.tsx` — a server component
  that (a) validates `slug === 'the-record-keeps'` (else the in-voice 404), (b) reads the neutral
  spoiler-free view into a `RecordSignal`, (c) renders `project(signal)` as static HTML in the
  cream/navy→ash palette. No client JS, `noindex` inherited from `layout.tsx`.
- **`[TODO]` Forge wiring:** route the Iss node's drip through `embedRuneLayer` (+ optional
  `embedLsb` on the PNG frame) in the card render for `stone-iss-wall` only — one branch behind
  the existing forge call (`forge/templates/index.ts` apply hook / `clue-drip.ts`).
- **`[TODO]` Website re-render gate:** the M5 "received" projection becomes reachable only once
  `instantReached(boundInstant, now)` AND `accepted` — the route consults the same predicate the
  grave/summons do, so the three surfaces never disagree (§8.2).
- **`[TODO]` Seed row** (`puzzles_seed.sql`, kebab): `record-url` — the P4 founder-line carving
  whose normalized answer is `the record keeps`, `outcome_type:'lore'`, `voice_key:'recordElsewhere'`,
  **no door** (finding the site is its own reward; gates nothing). The stego key is NOT a new row —
  it feeds the existing `stone-iss-wall` P3 node.

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for" seed

**The plant (M1, inert/ambiguous) — `[BUILT]`:** the water-damaged founder margin
`kept-in-more-than-one-place.md` carries one line easy to skim past: *"the record is kept in more
than one place, against the loss of the first."* In M1 this reads as worldbuilding texture —
keepers were careful archivists. It points at nothing; there is no second place yet visible. The
later-hand marginalia even stages the misread for the player: *"i walked every direction it could
be and there is no shrine… then a keeper younger than me read it not as a place in the world but
as a name you call."*

**The intermediate ambiguity (M1–M2):** the keyed line decodes (P4) to `the-record-keeps` — a
phrase, not obviously a URL. A group that decodes it has *a phrase and no lock*. It sits unused,
an orphan-seeming answer (the cipher web's `dead_end` texture doctrine makes "true answers that
open nothing" a familiar, non-frustrating shape — and `record-url` is authored as a `lore` row,
so the Watcher acknowledges the read and opens no in-world door).

**The payoff (the click):** the moment a player puts the decoded phrase **after the dashboard host
as a path** — `/record/the-record-keeps` — the founder line snaps into meaning: *the record is
kept in more than one place.* The "second place, against the loss of the first" was literal. The
phrase wasn't a dead end; it was a coordinate for a place outside the game. That is the "oh, THAT
is what that was for" — a line planted as texture in M1 pays off as the key to the off-world
surface, and the website was foreshadowed before it existed.

**Second, nested payoff (M4):** any player who screenshotted the Record's warm Iss state in M2 (a
natural hard-ARG habit) discovers in M4 that the same URL now reads cold. The M2 screenshot
becomes evidence the page lied — the plant is the player's own saved image; the payoff is the
diff. No authored hint required: paranoid documentation is rewarded, the friend-group bar exactly.

No plant without payoff (the founder line → the URL click; the warm screenshot → the cold diff).
No payoff without plant (the website never appears un-foreshadowed; the cold re-render is the same
Liar engine already canon).

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Sub-thread | Lives in | Depends on | Depended on by | Priority | State |
|---|---|---|---|---|---|
| **The Record website** (`/record/[slug]`) | M1 (path-find) → M5 (received) | the founder-line plant (M1 doc, BUILT); P4 literacy (`D03`); the spoiler-free `arc_state` projection (BUILT) | nothing gates on it (additive); it *expresses* FACT 1/8/11/14/15 as a third door | **P1** | projection BUILT; route TODO |
| **Stego key** (rune layer + LSB, P17→P3) | M2 (forged on the Iss card) | the forge render pipeline; `runes.ts`; `stone-iss-wall` | nothing (the P3 key is also in `D09`); a second in-door to `iss_caught` | **P1** | BUILT + self-tested; forge wiring TODO |
| **Encoded-timestamp clock** | M4 publish → M5 fire | a showrunner-bound instant in `arc_state` (BUILT); the rite/`accepted` signal | the M5 "received" flip + `voice.summons()` | **P2** | BUILT; route read of `instantReached` TODO |

**Hard dependency note:** the Record website is **strictly additive and gates nothing** — the
core arc (cipher web §2 graph) completes with `/record` entirely absent. That is the optionality
contract the pitch requires. Its *value* is the YouTube-ARG-video bar (a surface a viewer who
never logs in can read) and a third redundant door for the spine's load-bearing facts.

**Remaining build order (the only `[TODO]` left):**
1. `recordElsewhere` + `recordReceives` voice keys (`voice.ts`) + the `record-url` lore seed row
   (`puzzles_seed.sql`). [tiny; unblocks the lore door + the masthead/M5 copy]
2. `dashboard/src/app/record/[slug]/page.tsx` — server component, slug-validated, reads the
   neutral spoiler-free view → `RecordSignal` → `project()`, static cream/navy→ash HTML,
   `noindex`, no client JS. [the surface]
3. Forge wiring: route the `stone-iss-wall` drip through `embedRuneLayer` (+ `embedLsb` on the
   PNG frame) behind the existing forge call. [the cipher, one branch]
4. The M5 "received" projection gated on `instantReached(boundInstant, now) && accepted`, read by
   the route from the same `clock.ts` predicate the grave/summons consult. [the climax]

**Cut/defer ledger (final):** Real astronomical new moon — CUT, replaced by encoded timestamp
(shipped in `clock.ts`). Live-mutating page — CUT, static-per-build only (enforced by the pure,
client-JS-free projection). Per-player Record personalization — CUT entirely; the public page
speaks only of the group (precision law, enforced by the 3-field coarse signal). LSB stego —
**NOT cut** (revised): kept as the fail-closed PNG-lossless fallback behind the visual rune layer
(§2f), the one constraint being the Iss card is posted as PNG.
