# The Observance — CURSED-MAP-SITE (the lure page / the public Record's downloads face)

> **Owner:** `cursed-map-frame` (A14) — the lure page sub-piece (B). **Extends** A13
> `arg-leaves-the-game` (the Record website): SAME route (`record/[slug]/page.tsx`), SAME
> projection (`record-projection.ts`), SAME cold archive shell, SAME `noindex`, SAME `v_record`-only
> read, SAME M5 `recordReceives()` fill. **Mints nothing** — no FACT, no INV, no cipher, no flag, no
> site, no new table, no new data source. It adds ONE slug branch + ONE static downloads block to a
> route that already exists. Authority for *how it fits*: `WEB-MASTER §1.M0-remote`, `§9` ledger rows
> 24–27. Authority for *what it contains*: `INTEGRATION-V2 §A14`. Where this file disagrees with those,
> they win.
>
> **The page IS the Record, found — not a marketing site.** It is the public face of `/record` at the
> one slug the founder margin decodes to. No nav, no credits, no CTA copy, no analytics, no client JS.
> A dead archive that someone left a file in. The only "call to action" is *inside the vignette*, in
> voice ("bring them"), never on the page.

---

## 0. GROUND TRUTH — what already exists vs. what this builds

| Piece | State | Source |
|---|---|---|
| `record/[slug]/page.tsx` | **BUILT** — a `RecordPage` that reads `v_record` and renders the archive. **It ignores `slug` today** (no `params` arg, no slug branch). | `dashboard/src/app/record/[slug]/page.tsx` |
| `record/layout.tsx` | **BUILT** — `noindex` segment wrapper, `.record-root`. | `dashboard/src/app/record/layout.tsx` |
| `record-projection.ts` (`project`) | **BUILT** — pure spoiler-free projection (`movement`/`stonesRead`/`accepted` → redacted archive). | `dashboard/src/lib/record-projection.ts` |
| bare `/record` | **NOT a separate file** — there is no `record/page.tsx`. `/record` is served by the dynamic segment. | — |
| `voice.recordOpened/recordElsewhere/recordReceives` | **BUILT** | `discord/src/voice.ts` |
| `kept-in-more-than-one-place.md` (founder margin → `the-record-keeps`) | **BUILT** | `arc/lore/documents/` |
| `the-record-opens.md` ("six are named… a seventh the record will not keep") | **BUILT** | `arc/lore/documents/` |
| `the-copy-i-kept.md` (Mara provenance, signed `m.kept`) | **BUILT THIS PASS** | `arc/lore/documents/` |
| `six-were-kept-before-you.md` (the `6` canon home) | **BUILT THIS PASS** | `arc/lore/documents/` |
| slug-validation + downloads block + in-voice 404 | **THIS BUILD** (code shipped in `record/[slug]/page.tsx` this pass) | below |
| the vignette `.zip` (the prop) | `[GAP — GO-LIVE asset]` — see `PROLOGUE-VIGNETTE.md` | — |

**The one real gap the route had** (per `INTEGRATION-V2 §A14` ROUTE RE-SCOPE / `2h`): the dynamic
route `[slug]` renders the same archive for *every* slug and never validates it. The fix is to read
`params.slug`, render the base archive for the canonical/empty slug, the downloads block ONLY for
`the-record-keeps`, and an in-voice 404 for anything else — preserving every property of the BUILT
route (server component, `noindex`, no client JS, `v_record`-only).

---

## 1. THE TWO SLUGS THE ROUTE SERVES (and the 404 for the rest)

`record/[slug]/page.tsx` resolves `params.slug` against a tiny closed set. Nothing else is a real page.

| `slug` | Renders | Why |
|---|---|---|
| `the-record` | the **base archive** (the BUILT projection, unchanged) | the plain Record, A13 |
| `the-record-keeps` | the base archive **+ the downloads block** (§2) | the lure page, A14 — the slug the founder margin decodes to (`kept-in-more-than-one-place`, ledger #11) |
| anything else | one **in-voice 404 line**, the dead-archive shell, nothing kept | a guessed slug must never leak the real archive (`2h`) |

The set is `the-record` and `the-record-keeps`. Both render the same masthead/season/struck-entries
shell; `the-record-keeps` appends the downloads block beneath the archive. The 404 is not a Next
`notFound()` chrome page — it is the **same cold shell** with a single struck line and no entries
(an archive that holds nothing under that name), so even a wrong guess stays in-fiction and in-voice.

### 1a. The bare `/record` entry (verified)
There is no `record/page.tsx`, and the dynamic `[slug]` segment does **not** match the zero-segment path
`/record` — verified: `curl /record` → **404**, while `/record/the-record`, `/record/the-record-keeps`,
and a junk slug all → 200. This is correct for the ARG: the in-world decoded path is always
`/record/the-record-keeps`, and the plain Record is reached at `/record/the-record`. The route's
empty-string handling in `BASE_SLUGS` is harmless defense only. (If a future `record/page.tsx` is added
it can render the base archive at bare `/record`; this route stays the slug authority for the segment.)

---

## 2. THE DOWNLOADS BLOCK — the only thing `the-record-keeps` adds

Rendered beneath the BUILT archive `<ol>`, above the standing footer. It is **static** — authored, not
read from any view. It reads **no new data** (the projection is not widened; `record-projection.ts`
is untouched — `INTEGRATION-V2 §A14`). Three elements, in the cold register, in the same type scale as
the archive:

### 2a. The recovered-file entry (one legible line + the provenance)
A single archive-style row, legible (not struck), carrying the map description and a download link, with
Mara's provenance beneath it. Verbatim text (de-slopped, `INTEGRATION-V2 §3`):

> **the map description** (flat, found, no marketing — quotes nothing the player can't verify):
> > a hold, kept and left. one walk through it remains. the rest of the record is kept elsewhere.
> > what is downloaded is only the part that fit in a file.
>
> **the download link** — label is the filename only: `the-hold.zip`. `href` points at the vignette
> asset (GO-LIVE: `/the-hold/the-hold.zip` or a CDN URL). `download` attribute set; `rel="noopener"`.
> No button styling, no "Download Now" — it reads as a file name in an archive row.
>
> **the provenance line** (Mara's hand, the last four lines of `the-copy-i-kept.md`, verbatim, signed):
> > i copied it as it was given, page for page, and set the copy where fire and water do not reach.
> > i did not keep the seventh. i was not the hand that decides what is kept. — m.kept

The page **quotes `the-copy-i-kept.md`** (now a real doc) rather than inventing a provenance line — the
uploader is fully un-orphaned (`INTEGRATION-V2 §A14` "One Mara author" S3). The signature `m.kept` is
the dead-uploader handle; it is a canon keeper's hand (Mara, the Reader — `WEB-MASTER §6`), never the
Seventh (the record holds no name for the Seventh, so the Seventh can never be a named uploader).

### 2b. The README "lie" (the prop's flat cover, shown as the file's note)
A second muted line, the file's own README, technically true so the M4 re-read is "it never lied, i
misread it as comfort" (ledger #25). Verbatim:

> the-hold.zip — a small offline map. single player. no mods. about fifteen minutes.
> it does not connect to anything. play it through to the end and it will tell you where the rest is kept.

Rendered as the file's note (smaller, `text-neutral-600`), not as prose the Archivist speaks. The
load-bearing line is **"it does not connect to anything"** — true of the *map*; the *server* is the
thing that connects (ledger #25).

### 2c. The counter — `kept: 6` + the struck seventh row (the heaviest plant, ledger #24)
**Static authored number, NOT a live counter** (`INTEGRATION-V2 §A14` 2c — a live count would drift off
6 the moment the group downloads, killing the plant; it would also add a backend/attack surface). Render:

```
kept: 6
████████        ← the struck seventh row (REDACTED_GLYPH, reused from the projection)
```

- `kept: 6` uses the same mono/tabular type as the archive's muster number.
- The struck seventh row reuses `REDACTED_GLYPH` (the BUILT redaction glyph) — visual continuity with
  the archive's withheld lines, and the **same** iceberg grammar (a struck block hints a row exists,
  never its content). It is authored, never a live row.
- The canon home for the `6` is `six-were-kept-before-you.md` (built this pass): six prior
  keeper-generations the record already kept; the group is the seventh, kept not as a file but as hands.

**Cross-surface read of the `6` (the staggered payoff, ledger #24):**
- **M2 (soft):** once the group reads "six named, a seventh it will not keep" (`the-record-opens`), a
  sharp player re-reads the `6`. *six downloads, six keepers* — pattern-match, NOT yet "the keepers were
  download groups like us" (`INTEGRATION-V2 §A14` S6 — the page must not over-blurt FACT 15 on day zero).
- **M4 (hard):** the catch re-reads the world cold (`record-writes-you-in`); the `6` becomes six prior
  groups it kept the same way it is keeping us.
- **M5 (felt):** `recordReceives()` adds the group's names to `/record`; the struck seventh row is,
  structurally, now the present hands — kept. **Never stated**; delivered by the page's own structure.

---

## 3. HOW IT DOUBLES AS THE PUBLIC RECORD (one artifact, two readings)

The lure page is not a second site that resembles the Record. It **is** the Record at one slug. The
doubling is the whole point and it is load-bearing for the TINAG illusion:

- **Same shell.** Masthead rune block, season line, struck-block archive entries, standing footer — all
  the BUILT `RecordPage`. A viewer who later finds bare `/record` sees the identical artifact and knows
  the lure page was never a separate "promo" thing; it was the archive, with a file left in it.
- **Same spoiler model.** The downloads block reads no sealed data; the archive half still reads only
  `v_record` through the anon client; `noindex` holds. Cracking the page reveals only the static `6`,
  the two quoted lines, and the public server address the vignette hands out anyway (`2f`/`2h`).
- **Same M5 fill.** When the Accepting resolves, the archive's closing entry un-redacts
  (`recordReceives`, BUILT) — and *on the lure slug* the struck seventh download row reads, by
  juxtaposition, as the now-filled seventh. No new code: the closing-entry legibility the projection
  already drives is the felt fill; the struck download row is authored to sit directly above it in the
  reader's eye. (No live coupling — the page is static-per-build; the felt landing is compositional.)
- **The YouTube cold-open.** The camera opens on this dead page and the `6`. A viewer who never logs in
  can read the whole hook off the page: an abandoned archive, a recovered file, a count that does not
  come out even. The `6` is legible to a stranger; the meaning is not — exactly the iceberg.

---

## 4. THE CODE (shipped this pass in `record/[slug]/page.tsx`)

The build is additive and compile-safe; it does not touch `record-projection.ts`, `layout.tsx`, the
base archive render, or the `v_record` read path. Summary of the diff (full code in the route file):

1. **`{ params }`** added to the page signature; `params.slug` resolved (Next 15 async params: `await`).
2. **A `LURE_SLUG = 'the-record-keeps'`** + **`BASE_SLUGS`** closed set; `resolveSlug()` maps
   absent/`the-record`/`the-record-keeps` → render, anything else → the in-voice 404 shell.
3. **`<Downloads />`** component (pure, static — no props from the DB) rendered ONLY when
   `slug === LURE_SLUG`, between the archive `<ol>` and the footer. All text is the verbatim §2 copy,
   in the existing Tailwind type scale. Reuses `REDACTED_GLYPH` for the struck seventh row.
4. **`<NotFoundShell />`** — the cold shell with one struck line, no entries (the 404).
5. `metadata` and `revalidate` unchanged; still a server component, no client JS.

**Verified after the build (`dashboard`):** `tsc`/`eslint` clean on the route file (pre-existing tree
errors in author components / supabase-server / middleware are untouched and not mine); the dev server
serves `/record/the-record` → 200 (base, no downloads block), `/record/the-record-keeps` → 200 (downloads
block: `kept: 6` + struck-7 + `the-hold.zip` + `m.kept` provenance + the README "lie"), a junk slug → 200
(in-voice 404 shell, "nothing is kept under that name here"), bare `/record` → 404 (no segment match). The
route is a server component with no `"use client"` directive; the downloads block reads no Supabase symbol.

---

## 5. WHAT THIS DELIBERATELY DOES NOT DO (the cut liabilities)

- **No live counter, no uploader account, no upload form** — the `6` is static; there is no backend to
  probe (`2c`).
- **No personalization on the page** — the page measures only the group's coarse progress (it can only
  speak of the group); "it knows you" lives in-server under measured gates (`2d`, INV-16). The page
  never addresses "you".
- **No widening of `record-projection.ts`** — the downloads block is authored, not a metric
  (`INTEGRATION-V2 §A14`, no new data).
- **No new route, table, flag, or site** — one slug branch on one existing route.
- **No marketing chrome** — no nav, credits, CTA, OG tags, or analytics; `noindex` inherited.

---

## 6. CROSS-OWNER HOOKS

- **TS-VOICE:** the frame-break voice key `recordFrameBreak()` (the server's category-violation line) is
  NOT on the page — it fires in-server. Spec'd in `PROLOGUE-VIGNETTE.md §5` and the handoff there.
  The page reuses only BUILT keys (`recordOpened`/`recordElsewhere`/`recordReceives`); no new key here.
- **SQL:** none. The lure page reads no new view; no seed row required for the page itself.
- **LORE:** the page quotes `the-copy-i-kept.md` and is backed by `six-were-kept-before-you.md` (both
  built this pass). If LORE re-tunes Mara's provenance wording, the §2a quote must track it (one source
  of truth: the doc).
- **GO-LIVE residue:** the `href` for `the-hold.zip` needs the real asset URL (the vignette `.zip`,
  see `PROLOGUE-VIGNETTE.md`) and the public server address baked into the vignette, not the page.

---

## 7. PLANT → PAYOFF (this surface's rows, from `WEB-MASTER §9`, synthesis-assigned 24–27)

| # | Seed (on this surface) | Inert reading | Payoff | The "oh" |
|---|---|---|---|---|
| 24 | `kept: 6` + struck-7 | a dead file's tally | M2 soft → M4 hard → M5 felt | six prior keeper-generations; the group is the seventh, kept as hands |
| 25 | README "it does not connect to anything" | mundane reassurance | M4 | the *map* connects to nothing; the *server* does — true, misread as comfort |
| 26 | uploader `m.kept` (Mara's hand) | a person who left a file | M4 | no person uploaded it; the record kept it in a dead hand |
| — | (the vignette rune string → `the-record-keeps`) | a decorative carving / the address | M2 | the address typed == the record's own name (reuses ledger #11) |

(Row 27 — the vignette's doused hearth + scraped wall — lives in the prop, spec'd in
`PROLOGUE-VIGNETTE.md`, not on this page.)
