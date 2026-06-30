# Teardown — Dimension: ARG (out-of-game / cross-surface)

Scope: the cursed-map prologue vignette, the Record / cursed-map website (`/record/[slug]`),
the forge stego, the real-world-clock (encoded timestamp), the prologue→server handoff and the
frame-break. Read against the REAL code/seeds, not the design guides. Worst first.

---

## TOP FINDINGS (worst first)

### 1. [REDESIGN] `prologue_ignited` is never WRITTEN — the entire out-of-game→server handoff is dead
`discord/src/showrunner/autonomy.run.ts:125` READS `flags.prologue_ignited`. Nothing in
`discord/src/` ever SETS it (`grep` for any write returns only that read). Both design docs
(`cursed-map-frame.md §4`, `PROLOGUE-VIGNETTE.md §4`) assert "the BUILT `messageCreate` ignition
detector (`prologue.ts`) already fires `prologue_ignited` on a human post in `#the-record`." That
is **false**. The real `messageCreate` handler (`discord/src/bot/index.ts:99`) runs ONLY the
answer-scan: it calls `resolveAnswer(...)` and replies `solved`/`withheld`/`silent`. A human typing
"kept" there resolves as a non-matching answer → `silent`, and `prologue_ignited` stays false
forever. `decidePrologue` (`prologue.ts`) is a pure policy function imported only by selftests and
`autonomy.run.ts`; there is no `IgnitionListener`. **Consequence: the central scare cannot fire.**
The vignette's whole closing instruction ("say *kept* when you are all in") routes into a detector
that does not exist. **Fix:** in the `messageCreate` handler, before/after the answer-scan, if
`channelId === theRecord` and the post is a human keeper, set `arc_state.flags.prologue_ignited=true`
(idempotent) and let the next autonomy tick fire the ack. This is ~10 lines and unblocks the entire
dimension; do it before building any prop.

### 2. [REDESIGN] `voice.recordFrameBreak()` does not exist — FB-2, the heart of the scare, is unbuilt
`grep -c recordFrameBreak discord/src/voice.ts` = 0. The dimension's stated "heart" (the category
violation: the server says the `6` back at you) is `voice.recordFrameBreak()`, and it is absent from
both the `voice` object and the `OracleVoiceKey` union. `recordOpenedNamed` (referenced by
`prologue.ts:54` / `PrologueDecision.reportVoiceKey`) is also absent from `voice.ts` — `decidePrologue`
can return a key string that has no voice line behind it. **Fix:** author `recordFrameBreak()` with the
count-callback body verbatim from `cursed-map-frame.md §3` ("six were kept before you… you are not a
file here"), add `recordOpenedNamed`, run them through the discipline self-test, then wire one
ack-path emission gated on the measured count signal. Until both exist, FB-1 (`recordOpened`) is the
ONLY thing that can fire — and even that is blocked by Finding 1.

### 3. [FIX] The stego "preferred" rune-layer is mechanically undecodable by any player
`discord/src/forge/stego.ts`: the preferred path is `embedRuneLayer` (a faint SVG `<g>`,
opacity 0.12). The card pipeline is `satori → SVG → resvg → PNG` (`discord/src/render/render.ts:43-48`,
`resvg.render().asPng()`) — players receive a **PNG**. The only "decoder," `extractRuneLayerPayload`
(stego.ts:174), regex-matches `data-stego-payload="ISS"` — a plaintext attribute stamped into the
**SVG markup**, which is discarded at rasterization and never reaches the player. So
`extractRuneLayerPayload` is not a steganography decoder at all; it round-trips an attribute the host
keeps in memory. A player staring at the PNG has no machine path to the key and there is **no
pixel-OCR**. The faint runes may be visible by eye (good, that's the diegetic intent), but the module
oversells itself as a decodable second door. **Fix:** either (a) accept it as a *visual-only* watermark
read by eye + Rosetta (then delete `extractRuneLayerPayload`'s false "decode" framing), or (b) actually
embed the LSB variant — see Finding 4.

### 4. [FIX] The decodable stego variant (LSB) is never applied to a real card
`embedLsb`/`extractLsb` are the only PNG-surviving, checksum-verified scheme, and `arg-leaves-the-game.md §2f`
insists both are shipped. But `grep` for `embedLsb` across the non-stego pipeline finds it ONLY in
`templates/index.ts`'s *import list* — `withStegoRuneLayer` (templates/index.ts:180) composites the
rune-layer and never calls `embedLsb`. So the one variant that would survive Discord's PNG re-encode
and be programmatically decodable is dead code with a passing self-test. The dimension ships a stego
that is decodable-in-theory and visible-in-practice but never both on the same artifact. **Fix:** after
`resvg` produces the RGBA frame for the Iss card only, run `embedLsb(frame, 'ISS')` → `encodePng` →
post. Then the LSB lane is the real backstop the design claims. Otherwise CUT the LSB code as
self-test theater.

### 5. [FIX] The decoded answer "the record keeps" does not equal the slug `the-record-keeps`, and no host is ever given
`record-url` (`puzzles_seed.sql:952`) normalizes the founder line to the answer token **`the record keeps`**
(space-separated, three words). The lure route only serves `the-record-keeps` (hyphenated;
`page.tsx:48` `LURE_SLUG`). A junk slug → in-voice 404 (`page.tsx:237`). So a group that *correctly
solves the puzzle* holds the phrase "the record keeps" and must independently (a) hyphenate it, (b)
know the dashboard host, and (c) append `/record/`. Nothing in-world hands them the host — `voice.recordElsewhere`'s
fragment (seed row) says "the path is the record keeps" but names no domain. For a friend group that
"will miss most subtlety," this is a hard dead end disguised as a solve. **Fix:** make the
`recordElsewhere` fragment carry the full host+path literally (it's a `lore` row, gates nothing, so no
spoiler cost), OR accept the slug `the record keeps` (normalize hyphen/space) in `resolveSlug` so the
literal solve token resolves. Right now the "oh, it's a URL" payoff (`arg-leaves-the-game §5`) is
unreachable for anyone who doesn't already guess the domain.

### 6. [FIX] The route never renders the encoded timestamp it is the publisher of
`clock.ts` builds `encodeTimestamp` "for the Record website" and `instantReached` as "the single
predicate the … website re-render consult[s]" (clock.ts:13,69,85). The route `page.tsx` imports
NEITHER (`grep` in the route = no `clock`, no `instantReached`, no `encodeTimestamp`). The comment at
page.tsx:262 even says "a muster, not a clock." So `arg-leaves-the-game §1.2/§6 TODO 4` ("M5 received
gated on `instantReached && accepted`, read by the route") is unbuilt, and the design's headline M4
copy ("the way opens at <encoded instant>") appears nowhere on the page. The clock is a self-tested
island wired into the plugin's grave/summons but not into the website it was written for. **Fix:**
either render the encoded instant + gate M5's closing row on `instantReached` in the route, or
explicitly downgrade the clock-on-website claim to "in-server only" and stop saying the website
publishes it.

### 7. [CUT] The 6-room vignette is a second full build with zero validated play, gating nothing
`PROLOGUE-VIGNETTE.md` specs a bespoke 6-room datapack+world (`the-hold.zip`), coordinate-specific
`tick.mcfunction` region triggers, a conduct scoreboard, an optional bundled rune resource pack — none
of which exist (`find … the-hold` = nothing; `dashboard/public/` does not exist). The doc's own §0
admits the conduct-callback FB-2(i) is CUT and the count-callback (FB-2) is "true for everyone whether
or not they played the map." So the *entire interactive map* feeds exactly one server line that does not
need it. For a remote friend group, this is the single largest build with the least mechanical payoff,
and it competes for time against Finding 1 (which is what actually makes the scare possible).
**Verdict:** CUT the 6-room hold for v1. Ship the doc's own §2j scale-down: the lure page + the `kept: 6`
+ a **1-room** vignette (spawn → one lectern → the server pointer + the plain address). The frame-break
degrades to the count-callback, which needs no in-map conduct. Revisit the rich hold only after one
real server playthrough proves ignition works.

### 8. [SIMPLIFY] Two design docs each claim source-of-truth and contradict the code on the same fact
`CURSED-MAP-SITE.md`, `PROLOGUE-VIGNETTE.md`, `cursed-map-frame.md`, and `arg-leaves-the-game.md` all
mark the ignition detector and `recordFrameBreak` as `[BUILT]`/"already-built spine," with explicit
"do not re-propose them" notes (`arg-leaves-the-game §0`). The code says otherwise (Findings 1, 2). Four
docs (~110KB) for one surface, each citing the others as authority, is exactly the "guides over-claim"
problem. **Fix:** collapse to ONE arg spec that states real build-state per `file:symbol`, and delete the
`[BUILT]` claims that the code contradicts. The docs are currently a liability: they will cause a future
builder to skip the two things that are actually missing.

---

## SECONDARY FINDINGS

### 9. [FIX] `readSignal()` reads a view (`v_record`) that the route admits "has not shipped yet"
`page.tsx:80-109` reads `v_record` through an untyped cast and collapses ANY failure to the sealed
baseline. The header (page.tsx:34) says "If the view is absent/unreadable (it has not shipped yet…) it
degrades to the SEALED BASELINE." So today the Record renders the all-redacted M1 baseline for EVERY
movement — it never un-redacts, because the view it needs does not exist. The page is a static "0 of 7
kept" placard regardless of in-world progress. **Fix:** ship `v_record` (SQL lane) or the whole
"un-redacts in lockstep" promise (`arg-leaves-the-game §1.2`) is vapor. Verify with a real DB row, not
the selftest.

### 10. [KEEP] The static `kept: 6` + struck-7 is the one genuinely solid piece — keep it exactly
`page.tsx:148-201` `Downloads()` is pure, static, no backend, no live counter, reuses `REDACTED_GLYPH`.
This correctly dodges the live-counter trap (`cursed-map-frame §2c`) and the per-visitor personalization
trap. The `6` is a clean, camera-legible, deniable plant. **Keep as-is.** It is the rare part of this
dimension that is fully built and self-consistent.

### 11. [FIX] The lure page links to `/the-hold/the-hold.zip` — a 404 today, and a hard go-live coupling
`page.tsx:160` hardcodes `href="/the-hold/the-hold.zip"`. `dashboard/public/` does not exist; the asset
does not exist. On camera (the YouTube cold-open the design leans on, `CURSED-MAP-SITE §3`) the one
interactive element on the page is a broken link. **Fix:** gate the `<a>` behind an env/flag so it
renders as a plain struck archive row until the asset is hosted, OR don't ship the lure slug publicly
until the zip exists. A dead download on the "found archive" reads as a broken site, not a haunted one.

### 12. [SIMPLIFY] The masthead rune string `ᛟ ᚲ ᛖ ᛈ ᛏ` is decorative and inconsistent with the decode claim
`page.tsx:251` and `:215` render a fixed 5-glyph block as "the archive's seal." The design says the
in-world rune literacy (P4) is the whole bridge to finding the page. But this header block is authored
Unicode runes that do NOT spell `the-record-keeps` (5 glyphs vs the slug) and aren't generated from
`runes.ts` `RUNE_MAP`. A literate group that learned the keepers' script will read it and get
gibberish, puncturing the "same alphabet learned in-world" claim (`arg-leaves-the-game §1.2`). **Fix:**
either render an actual `runes.ts` carving of a real word, or accept it as a non-semantic seal and stop
implying the header is decodable.

### 13. [KEEP] The spoiler/anti-metagame model is genuinely sound
`stego.ts §6` audit, the `v_record`-only read, `noindex`, the 3-field coarse signal, the static `6`,
"the page never addresses you" — the privacy/precision discipline is real and consistently enforced in
code (no player name, no IP, no per-visitor anything in the route). The offline vignette carries no
FACT past 1/2 by construction. **No creep concern** in what is built: there is no profiling, no
offline-skin or name-carve on the *web* surface (those live in-server: `offline-skin.ts`,
`name-where-never-been.ts`, gated). Keep this boundary exactly; it is the dimension's best property.

### 14. [FIX] FB-2's "count callback" claims to be "true for everyone" but is only true if they SAW the page
`cursed-map-frame §1.6 / PROLOGUE-VIGNETTE §5` default-safe FB-2 says the `6` back at the group "true
for everyone whether or not they played the map." But the `6` is only meaningful to someone who saw the
lure page's counter (Finding 10). A cold group that joined the server without ever touching `/record`
gets a frame-break referencing a number they never saw — it lands as a non-sequitur, not a scare. The
"category violation" requires the player to have seen the static `6` first. **Fix:** gate
`recordFrameBreak()`'s count form on evidence the group reached the lure page (e.g. the `record-url`
lore solve having fired), and fall back to `recordOpened()` only otherwise. Don't say "true for
everyone" — it's "true for everyone who saw the page."

### 15. [SIMPLIFY] `record-url` is one of the 11 UNCLASSIFIED rows failing `specscheck` — and it's a lore row that can't be cipher-classified
`record-url` (puzzles_seed.sql:952) is among the active rows `specscheck` flags unclassified (per ground
truth). It's `outcome_type:'lore'` with no cipher — the classifier presumably expects a cipher family.
The same is true of the other arg-adjacent lore/auto rows (`reckoning-rosetta`, `base-docket-reread-auto`).
**Fix:** give the specs classifier an explicit `lore`/`non-cipher` class so these stop being RED, rather
than leaving the build red on rows that are correctly cipherless. This is what keeps `npm run specscheck`
failing and the whole build RED.

### 16. [CUT] The "find the other five copies" leaf (`cursed-map-frame §4 side-quests`) — unbuildable taunt with no surface
It proposes a `dead_end` `kind:count` taunt answering "where are downloads 1-5?". There is no listener
that detects that probe out-of-game, and the lure page has no input. It would only work in `#the-record`,
where it's just another answer-scan miss. **Verdict:** CUT; it's a paragraph of flavor with no
attachment point.

---

## IF I COULD CHANGE ONE THING

**Wire `prologue_ignited` to actually get written (Finding 1), then ruthlessly cut the 6-room vignette
to one room (Finding 7).** The dimension's entire value proposition — "a map can't know your name, the
server can" — is gated on a single boolean that no code ever sets, while ~110KB of design and a second
full Minecraft build pile up *around* that missing 10-line write. Every other arg artifact (the static
`6`, the stego, the clock, the projection) is either solid-but-stranded or decodable-but-unapplied
because the one nerve connecting out-of-game to in-server was never soldered. Solder it, prove ignition
fires on a real server with a 1-room prop, and only then decide whether the rich hold earns its build.
Right now the ARG layer is not "bolted on" — it's bolted to nothing.
