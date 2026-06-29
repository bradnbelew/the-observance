# IDEA — The Cursed Adventure-Map Frame (prologue vignette + out-of-game lure + the frame-break)

> Design treatment. Spoiler-bearing in §4 (THREAD IT) and §5 (PAYOFF). Operate inside the
> canon-spine FACT/INV web, the cipher web, and the **frozen namespace** (`WEB-MASTER §0`).
> Where this file and `WEB-MASTER.md` disagree, WEB-MASTER wins. Verdict at the top so
> synthesis can read it cold.
>
> **THE ONE-LINE.** A friend finds a small, genuinely-playable downloadable Minecraft
> "adventure map" on a dead-looking web page. Playing it is a 15-minute single-player
> vignette that ends by telling them to bring the others to a server address. On the server
> the thing **stops behaving like a map** — it knows their names, references the session they
> just played alone, reacts to what they actually do. A map cannot do that. That is the scare.
>
> **THE RECONCILIATION THIS FILE EXISTS TO HONOR (director's intent, non-negotiable):**
> - The main campaign is **server-side (Path A)** — the friends install nothing but the one
>   auto-pushed resource pack. The engine is on the server.
> - The downloadable "map" is a **separate, small, client-side PROLOGUE VIGNETTE** —
>   structures + command-blocks + a tiny datapack, **no server engine, no spoilers, no
>   machinery a metagamer can crack open**. It is a *prop*, not the delivery vehicle.
> - The **website** that hosts it is the public face of the Record (it IS `/record`, extended)
>   and the YouTube cold-open.
> - The vignette **ends pointing at the server**. The frame-break happens *there*, not in the
>   map.

**VERDICT: KEEP — P0 on-ramp, scaled.** This is the **delivery vehicle for the Cold-Start
Prologue's ignition** for a remote/asynchronous friend group, and the cold-open the YouTube
bar needs. It mints **no new FACT, no new INV, no new cipher, no new flag** — it is a new
*surface* and a new *on-ramp* for facts/sites/voice keys that already ship. Three sub-pieces:

- **(A) THE VIGNETTE** (downloadable client map) — KEEP, **P1**. A ~15-min single-player
  prologue, all vanilla structures + command-blocks + a one-file datapack, carrying **zero**
  server machinery and **zero** spoilers. Ends at the server pointer. `[GAP — TO BUILD]`
  (a `.zip` world + a datapack; no engine).
- **(B) THE LURE PAGE** (the dead uploader, the 6 downloads) — KEEP, **P1**. A second route
  *on the same host as `/record`*, framed as a recovered archive's download page. The
  "uploader" is a **known lore entity** (§1.4); the "6 downloads" is a **planted payoff**
  (the prior groups — ledger #24). `[GAP — TO BUILD]` (`/record/the-record-keeps` archive
  shell + a downloads route).
- **(C) THE FRAME-BREAK** — KEEP, **P0**. The first server beat that proves the watcher is
  not the map: it reuses the **already-built** Prologue ignition (`prologue.ts`,
  `voice.recordOpened()`) plus **one** new precision-gated callback to the vignette
  (`voice.recordFrameBreak()`, `[GAP — one voice key]`). This is the ignition for the remote
  group; it rides the built Prologue spine, adds nothing to the engine.

---

## 0. WHERE THIS SITS RELATIVE TO THE TWO BUILT ON-RAMPS (no orphan, no collision)

There are now **two ignition on-ramps**, and they must not contradict (`cold-start-prologue`
is the in-server one; this is the off-server one). They share one mechanism and one ack:

| | `cold-start-prologue` (BUILT spine) | **this file — the cursed map** |
|---|---|---|
| Audience | a group already on the server, mid-build | a **remote/cold** group not yet gathered |
| Trigger seed | one lit marker in their base + retargeted first report | a downloaded map they played alone |
| Ignition detector | lectern read OR human posts `#the-record` (`prologue.ts`) | **same detectors** + first server login after the vignette |
| The ack | `voice.recordOpened()` (one-shot, gated) | `voice.recordOpened()` → **then** `recordFrameBreak()` (one extra precision-gated line) |
| What it adds | one physical seed (`first_marker_01`) | one **off-server surface** (the lure page) + one **prop** (the vignette) + one callback line |

**The rule that keeps them from colliding:** the cursed map is the **on-ramp BEFORE M1** (call
it **M0-remote**). It hands the group to the *exact same `prologue_ignited` state* the in-server
prologue produces. Once on the server, there is **one** Prologue, **one** ack economy, **one**
`#the-record`. The map does not re-implement ignition; it **routes into** it. A group that never
touches the map still gets the in-server prologue unchanged. A group that plays the map arrives
**pre-ignited at the door** and the frame-break is their first server beat. (`COHERENCE`: this is
an additive on-ramp, gates nothing, INV-12 permanence-colors-never-gates analogue.)

---

## 1. EXPOUND — the full mechanic + story + mystery, beat-by-beat

### 1.1 The premise the friends experience (TINAG, no meta-frame)

One friend stumbles on (or is quietly handed) a URL. It is a bleak, near-dead web page — the
**same** cream/navy-degraded-to-ash editorial palette as `/record` and the forge cards, because
**it is the Record** (§1.4, §4). The page reads as a *recovered archive* that someone abandoned.
Among its struck-through entries, one line is legible and carries a download: a small Minecraft
world, `the-hold.zip` (or a datapack), with a flat README. The download counter reads **6**.

They download it. They play it **alone, single-player, offline** — 10–15 minutes. It is a real,
finishable little adventure: a short walk through "a hold someone kept," a couple of trivial
vanilla puzzles (a lever, a hidden lectern), and a quiet ending. The map never claims to be
haunted. It is competent, melancholy, and *small*. At the end it does one plain thing: it tells
them, in the keeper register, that **the rest is not kept here — it is kept where the others are**,
and gives a server address (and a "bring the others" line). The map closes.

Nothing in the map was uncanny. That is the design: **the prop must under-promise.** The dread is
banked for the server.

### 1.2 The vignette, beat-by-beat (the prop — vanilla, no engine, no spoiler)

A single-player downloadable world OR a self-contained datapack (the datapack is preferred — see
§2h, it can't leak a world file's structure browser). All effects are **command-blocks +
functions + `tellraw` + structure blocks**. No plugin, no Supabase, no LLM, no rune-engine code.
Total spoiler surface = **zero** (it carries no FACT past FACT 1/2, no keeper name past the
generic Archivist, no cipher key, no server flag).

- **BEAT 0 — the threshold (spawn).** The player spawns in a small stone antechamber, one lit
  lantern, a closed iron door, one sign: *"a hold was kept here. read what is left and go on."*
  Cold register, no scare. (Plants the **Kept-Light** motif — a fire burning where no one lit one
  — exactly the in-server M1 plant, ledger #4; the player won't know that yet.)
- **BEAT 1 — the short hold.** A linear ~6-room walk: a lectern with a one-page record fragment
  (the **public** half of `the-record-opens.md` — FACT 1/2 only, the "the count begins, the living
  are written by name" lines, *nothing sealed*); a doused hearth they cannot relight (the **Seventh**
  rhyme, but unnamed and unexplained — pure texture); a wall with **six** carved marks and a
  **seventh scraped blank** (the miscount plant, ledger #2 — inert here). A command-block trips a
  `tellraw` line as they pass each, in the Archivist's flat third person.
- **BEAT 2 — the one trivial "it noticed" tell (deniable, NOT personalization).** The map cannot
  know the player's name (it's a downloaded world; precision law forbids faking it). So the one
  "huh" moment is **structural, not personal**: as they leave the last room, a command-block
  detects they **skipped** the scraped-seventh wall (or *didn't* bow at the doused hearth if a
  pressure-plate custom is present) and the closing sign reads a flat line that names the **conduct**
  ("the seventh was passed. it has been noted."), never the player. This rehearses the in-server
  grammar (FACT 2, graded by laws no one told you) **without** a precision violation, because a
  conduct in a single-player map is true by construction.
- **BEAT 3 — the hand-off (the pointer).** Final room: a lectern, a closing record page, and a
  **map-on-a-wall** (item frame) or a sign carrying the server address + a short rune string.
  The page is the keeper register, plain: *the rest of the record is not kept in this hold. it is
  kept where the others are. bring them.* The rune string, once they can read it later, decodes to
  `the-record-keeps` (the **same** founder-phrase the in-server founder margin decodes to — one
  shared key, ledger #11). The map ends. **No frame-break has happened. The map behaved like a map.**

### 1.3 The lure page, beat-by-beat (the website content)

The page is **`/record/the-record-keeps`** — the slug the founder line decodes to (the route
already exists as `record/[slug]/page.tsx`; §4 specifies the one slug-validation + downloads
addition). It renders in the **same** cold archive shell as `/record` (the BUILT
`record-projection.ts` masthead, season line, struck-block entries), so it is unmistakably the
*same* artifact — found, not published. It adds exactly two things to that shell:

1. **A single legible "recovered file" entry** with a download link to the vignette and a flat
   provenance line in the Archivist register (no marketing, no nav chrome, no credits).
2. **A download counter rendered as `6`** — a *static, authored* number (NOT a live counter; a
   live uploader is cut, §2c). It is a planted payoff, not a metric. Beneath it, a struck block
   where a seventh download row would be.

The page carries **no meta-framing**. It looks abandoned. `noindex` (inherited from
`record/layout.tsx`). A wrong slug 404s to one in-voice line; the real archive never leaks
(§2h). The page **doubles as the YouTube cold-open** (§1.6): the camera opens on this dead page
and the `6`.

### 1.4 The dead uploader — a KNOWN lore entity (HARD REQUIREMENT a)

The uploader is **not a new orphan**. The page presents the file as recovered/left by a hand,
and that hand is canon. The reconciliation (the only one that satisfies all the laws):

> **The uploader is the Record itself, signing in a prior keeper's hand — and the named hand
> is one of the six dead keepers: Mara, the Reader.**

Rationale, threaded to canon:
- **Why a keeper, not the Seventh.** The Seventh is *the one the record will not keep* — has no
  name the record holds, a name scraped to bare stone (`the-seventh-not-kept.md`, FACT 10/10b).
  The Seventh **cannot** be the named uploader: the record has no handle for them. Using the
  Seventh would contradict canon. (The Seventh appears in the vignette only as the *unnamed*
  doused hearth + scraped wall — texture, never attribution.)
- **Why Mara specifically.** Mara is **the Reader** (`WEB-MASTER §6`): her cipher is bookCipher,
  her grammar is *referential and deferred* ("i read that…", page/line citations), her fate is she
  *read every rite and performed none* — she **copies and keeps and cites**, she does not act. An
  archivist who would **make a second copy of the record and leave it where stone-fire and
  deep-water cannot reach** (`kept-in-more-than-one-place.md`) is, in voice and fate, Mara. The
  founder margin already says a *keeper* read the keyed line "not as a place in the world but as a
  name you call" — that keeper-who-reads is Mara's exact characterization. So the "uploader" handle
  on the page is a dead, citeable hand: a username/sig like **`m.kept`** or **`thereader`**, in her
  deferred register, with a single flat provenance line.
- **What this means (the thesis, felt later).** The page was not uploaded by a person. It was
  *kept* by the record, in Mara's hand, the same way the record keeps everything — and the group
  is about to learn the record keeps **them** the same way (FACT 14/15, `record-writes-you-in`,
  the keepers-as-you thesis). The "dead uploader" is the first, deniable instance of *a dead hand
  still keeping*.

(Cross-surface truth: Mara's hand on the page must obey her grammatical fingerprint and never
contradict `journals-vaun-mara-sella.md`. One voice across journal, stone, page.)

### 1.5 The "6 downloads" — the planted payoff (HARD REQUIREMENT b)

The counter is **6** and it is inert on first read — it looks like a dead file nobody wanted.
It is the heaviest single plant this idea adds (ledger #24):

> **The 6 is the six prior keeper-generations the record already did this to.** Six were named
> in full (`the-record-opens.md`: "six are named in full, and there is a seventh mark the record
> will not keep"). Six keeper-stones. Six downloads. The record handed this same map to six groups
> before, and kept all six — they are the six dead keepers. The group, downloading it, becomes
> **the seventh download** (the struck row beneath the 6), and the seventh is *the one the record
> will not keep as a download — it keeps them as something else* (it keeps them on the server, by
> name, FACT 14/15).

The payoff fires across movements (not all at M4):
- **M2 (soft):** once the group reads "six named, a seventh it will not keep," a sharp player
  re-reads the `6` on the page. *Oh — six downloads, six keepers.*
- **M4 (hard):** the catch re-reads the whole world cold (`record-writes-you-in` M4); the group
  realizes the journals were never closed past-tense, the record is still keeping. The `6` re-reads
  as **six prior groups it kept the same way it is keeping us** — the keepers-as-you thesis lands
  on a number they saw on a dead web page on day zero.
- **M5 (felt):** at the Accepting, `/record` adds the group's own names (`recordReceives()`); the
  seventh row that was struck on the lure page is, in effect, now filled — they are kept. (Never
  stated; delivered by the page's own structure: the struck-seventh becomes the present hands.)

### 1.6 The frame-break — the central scare (HARD REQUIREMENT, the heart)

The map behaved like a map. The lure page behaved like a dead archive. The scare is the **category
violation** the moment they cross to the server: a map cannot know your name, cannot reference the
session you played alone, cannot react to what you do. The server does. Beat-by-beat:

- **FB-0 — they arrive pre-ignited.** Because the group played the vignette, the showrunner can
  set `prologue_ignited` on first group login (the vignette hand-off is a third ignition detector,
  §4 — "the map sent them"). The in-server Prologue spine (`prologue.ts`) is already armed.
- **FB-1 — the ack, but pointed.** The Watcher's first server line is the BUILT
  `voice.recordOpened()` — *"▒ the record is open. it was open before you."* Identical to the
  in-server prologue ack (one voice, cross-surface). For a group fresh off the map, "it was open
  before you" reads as a callback to the hold they just walked.
- **FB-2 — the one precision-gated frame-break line (the new key).** Exactly **one** new line,
  `voice.recordFrameBreak()`, fired **once**, **only** on a measured signal that proves the server
  knows what the map could not — and **never** as a guess. Two safe, measured triggers (either, not
  both; precision over recall):
  - **(i) the conduct callback** — if a player repeats *in the server* the conduct the **vignette
    flatly noted** (e.g. passes the in-server scraped-seventh / skips the bow), the line states the
    repeat as a fact: *"you passed the seventh in the hold too. it was noted there. it is noted
    here."* This is true by construction (the vignette logged the conduct locally; the server
    re-observes it) — it is a **behavior the group can verify they did**, not a fabricated name.
  - **(ii) the count callback** — the line states the **download number** back at them from inside
    the game: *"six were kept before you. you are the seventh the record will not keep as a file."*
    No personalization; a group-facing fact that *the map could not have said* because the map was
    offline and static.
  The default-safe path: if neither measured trigger is clean, fire **only** `recordOpened()` and
  let the frame-break be carried by the *next* in-server "it knows you" beat (the BUILT
  `name-where-never-been` carve, the Hold-Book). The frame-break is **never** traded for precision
  (canon §6.4, INV-16).
- **FB-3 — the knob drops.** Exactly as the in-server prologue: after the frame-break lands
  (detected, §1.4 detectors + the vignette-arrival flag), the loudness returns to baseline and
  never rises again in M1. The map was the loud thing; the server is patient.

The reveal discipline holds throughout: nothing is **witnessed** mutating. The map is static. The
server lines are text the Watcher *says*, in register — the watcher does not appear, does not
threaten, counts and stops.

### 1.7 How it plays across the arc

- **M0-remote (pre-arc):** one friend plays the vignette, posts it to the group ("you have to see
  this"). The lure page + the `6` are seen. The group gathers to the server address. **Ignition is
  the gathering**, caused by a player, not the bot (TINAG preserved).
- **M1 (The Notice):** first server session → FB-1/FB-2 frame-break → the in-server Prologue spine
  runs as built (the lit marker in *their* base, the first report). The vignette's hold now reads
  as a **rehearsal** of the server's opening — same register, same record.
- **M2 (The Ways):** the rune string from the vignette decodes (P4 literacy) to `the-record-keeps`;
  the group realizes the address they typed and the founder margin are **the same key** (ledger
  #11). The lure page's `6` re-reads against "six named keepers."
- **M4 (The Catch):** the cold re-read. The vignette's doused hearth + scraped wall re-read as the
  Seventh (now named in-server). The `6` re-reads as six prior groups. Mara's hand on the page
  re-reads as *the record was keeping, not a person uploading*.
- **M5 (The Accepting):** `/record` adds the group's names (`recordReceives()`); the struck-seventh
  download row is, structurally, now the present hands — kept.

---

## 2. CRITIQUE — adversarial, honest, with mitigations

**2a. ORPHAN RISK (the deepest, as always for a "cool extra surface").** A downloadable map +
a website is the *easiest* thing in the project to build as a disconnected stunt.
- *Mitigation (load-bearing):* this file **mints nothing** — no FACT, no INV, no cipher, no flag,
  no site. The vignette is a **prop rehearsal** of `the-record-opens.md` (FACT 1/2, already canon);
  the lure page IS `/record` (the BUILT projection + route); the uploader is **Mara** (a canon
  keeper, §1.4); the `6` is the canon "six named keepers"; the frame-break rides the BUILT
  `prologue.ts` + one voice key; the rune string reuses `the-record-keeps` (the BUILT founder
  plant, ledger #11). Every piece has a canonical home **before** this file. It is an on-ramp, not
  an invention.

**2b. THE FRAME-BREAK IS WHERE IMMERSION LIVES OR DIES — it must not over-promise in the map.**
If the *map itself* tries to be spooky/personalized, (i) it courts a precision lie (it can't know
the name), (ii) it spends the dread before the server, (iii) it trains spectacle (the exact poison
`cold-start-prologue §2.1` warns of).
- *Mitigation:* the prop **under-promises by law** — it is melancholy and competent, its one "it
  noticed" tell is a **conduct** (true by construction in single-player), never a name. ALL
  personalization waits for the server, where it is measured. The contrast (quiet map → the server
  knows you) IS the scare; a spooky map would flatten it.

**2c. A LIVE UPLOADER / LIVE DOWNLOAD COUNTER IS A TRAP — and is CUT.** A real uploader account, a
real incrementing counter, or "user-generated" framing (a) invites metagamers to probe the backend,
(b) makes the `6` a metric that drifts off `6` the moment the group downloads (killing the plant),
(c) adds an attack surface.
- *Resolution:* the `6` is a **static authored number**, not a live count. There is **no real
  uploader, no upload form, no account**. The page is a flat server-rendered archive shell. The
  seventh "download" is a **struck block**, authored, never a live row. (Mirrors `arg-leaves-the-game
  §2c`: cut the liability, keep the felt thing.)

**2d. PRECISION / "it knows you" misfire at the frame-break.** The single most dangerous line in
this whole idea is FB-2. A wrong callback ("you did X in the map" when they didn't) is worse than
none and a direct §6.4 / INV-16 violation.
- *Mitigation:* FB-2 fires **only** on a **measured** signal — either a conduct the player verifiably
  repeats *on the server* (re-observed by the live tracker, not asserted from the map) or the
  group-facing `6` count (true for everyone, names no one). The named-personal path is **absent**
  here entirely (names are carried by the BUILT in-server surfaces under their own precision gates).
  Default-safe: no clean signal → only `recordOpened()`. The frame-break degrades to "it was open
  before you," which is eerie and **always true**.

**2e. PATH A — does the downloadable map violate "install nothing"?** This is the sharpest
reconciliation risk. Path A says friends install nothing but the resource pack to play **the main
campaign on the server**.
- *Mitigation (the director's own carve-out, honored exactly):* the vignette is a **separate,
  optional, client-side PROLOGUE PROP** — explicitly *not* the delivery vehicle for the server
  engine. Playing it is a one-time, pre-arc, single-player experience; the **campaign** still
  requires only the auto-pushed resource pack on the server. A friend who refuses the download still
  joins the server and gets the in-server Prologue unchanged. The map is an **on-ramp**, never a
  dependency. Path A holds for the campaign by construction.

**2f. METAGAMERS CRACK THE WORLD FILE AND FIND THE MACHINERY (HARD REQUIREMENT d).** A downloadable
world can be opened in a structure browser / NBT editor; a leaked engine would spoil everything.
- *Mitigation (architectural):* the vignette **contains no engine**. It is structures +
  command-blocks + one datapack function file + `tellraw` strings — all of which are *the
  experience itself*, carrying no FACT past 1/2, no keeper name past the generic Archivist, no
  cipher key, no server flag, no Supabase URL beyond the public server address. Cracking it reveals
  **only what the player already saw** plus the server address (which the map hands out anyway).
  Preferred form is a **datapack** (no world file to browse; just functions/structures), further
  shrinking the surface. The real machinery is server-side and unreachable from the client. (This is
  the same boundary `arg-leaves-the-game §2h` enforces for the website: the public surface carries a
  *subset*, never the sealed core.)

**2g. ON-CAMERA / TINAG: "the creators made a download page" puncture.** A web page with a download
could read as marketing, breaking the found-not-published illusion.
- *Mitigation:* the page is the **same** dead archive shell as `/record` — no nav, no credits, no
  CTA copy, `noindex`, in the cold keeper register, with a struck-block field and a flat provenance
  line in Mara's hand. It looks recovered. The only "call to action" is *inside* the map, in voice
  ("bring them"), never on the page.

**2h. SLUG / SPOILER SURFACE.** The lure page is a public URL that, if guessed, could leak. And the
download must not be a spoiler.
- *Mitigation:* the route validates `slug === 'the-record-keeps'` (the §4 one-line addition; today
  the route reads `v_record` without validating the slug — that is the gap). A wrong slug → one
  in-voice 404 line, never the real archive. The page reads only the BUILT spoiler-free
  `v_record`/projection (coarse signal, no sealed flag). The download is the spoiler-free vignette
  (§2f). Nothing on this surface can render a sealed fact early.

**2i. COLD-START IGNITION (HARD REQUIREMENT c) — does it actually solve it?** The in-server prologue
assumes a group already on the server, mid-build. A remote/asynchronous veteran group may **never
gather on the server at all** without a reason — that is the true cold-start for them.
- *Resolution:* the vignette is the reason. It is a **shareable, single-player, no-commitment 15
  minutes** that one person can play and post — the lowest-friction ignition possible, and the one
  most likely to produce the unprompted "you have to see this" screenshot (`cold-start-prologue`'s
  whole ignition criterion). The map's closing "bring the others + address" is the gather-call. This
  is the on-ramp the in-server prologue can't provide for a not-yet-gathered group.

**2j. SCOPE / CUT LINE.** A bespoke 6-room map + a website + a datapack is real content.
- *Scale-down (the honest cut):* if the slice is at risk, ship **the lure page + the `6` + a
  ONE-room vignette** (spawn → one record lectern → the server pointer), skipping the 6-room walk
  and the conduct tell. That keeps the on-ramp, the dead-uploader, the `6` plant, the rune key, and
  the frame-break (FB-2 degrades to the count-callback, which needs no in-map conduct). The full
  6-room hold + the conduct-callback FB-2(i) is the **P2 depth** add. **Verdict: keep-scaled** —
  the page + `6` + frame-break are P0/P1; the rich vignette is P1→P2.

---

## 3. DE-SLOP TEST — exemplar artifacts, in-voice (cold, plain, concrete)

The Record/Archivist/Mara register: lowercase, declarative, names what is and stops, no warmth,
no second-person comfort, no announced emotion, no thematic bow, no three-adjective list, no "not
just X but Y", no em-dash drama, no exclamation. (Mara's hand additionally *cites/defers* — her
fingerprint, `WEB-MASTER §6`.)

**THE MAP DESCRIPTION (the lure page's one legible entry — flat, found, no marketing):**
> a hold, kept and left. one walk through it remains. the rest of the record is kept elsewhere.
> what is downloaded is only the part that fit in a file.

**THE LURE-PAGE PROVENANCE LINE (Mara's hand — deferred, citeable, signed `m.kept`):**
> i copied it as it was given, page for page, and set the copy where fire and water do not reach.
> i did not keep the seventh. i was not the hand that decides what is kept. — m.kept

**THE DOWNLOAD COUNTER (rendered, not prose):** `kept: 6` with a struck block beneath where the
7th would sit. (Reads as a dead file's tally; re-reads as the six prior keepers + the group as the
seventh-not-kept-as-a-file.)

**THE README "LIE" (the flat false-cover, §4 — it must read as mundane housekeeping, and be
*technically true*, so the later re-read is "it never lied, i misread it"):**
> the-hold.zip — a small offline map. single player. no mods. about fifteen minutes.
> it does not connect to anything. play it through to the end and it will tell you where the
> rest is kept.
>
> (the README's "it does not connect to anything" is the planted lie-that-is-true: the *map*
> connects to nothing — it is the *server* that does. M4 re-read: "it told us, on the first
> page, that the map was not the thing. we read it as reassurance.")

**THE VIGNETTE CLOSING PAGE (the hand-off — keeper register, the pointer, no scare):**
> the rest is not kept in this hold. it is kept where the others are. bring them, and come to
> the place named below. the record is open there. it was open before you.

**THE FIRST FRAME-BREAK LINE (`voice.recordFrameBreak()` — the category violation, stated flat,
the count-callback default form):**
> six were kept before you, and the count of them is on the page you found. a file is not kept.
> hands are kept. you are not a file here.

*(Note the omissions: nothing says "the map was haunted," nothing explains the trick, nothing
names a player. The dread is that the server stated the `6` the player saw on a dead page — a thing
a map could not do. The frame-break is the *fact*, not an adjective.)*

---

## 4. THREAD IT — exactly where this lives so it is not an orphan

### Canon-spine FACTs it touches (adds NONE)
- **FACT 1** (the record keeps the living by name) — the lure page IS `/record`, FACT 1's
  off-world face; the frame-break is FACT 1 pointed at the player from outside the game.
- **FACT 2** (graded by laws no one told them) — the vignette's conduct tell (Beat 2) rehearses
  FACT 2 in single-player (true by construction, no precision risk).
- **FACT 10/10b** (the Seventh, refused) — the vignette's doused hearth + scraped-seventh wall are
  the Seventh's *unnamed* texture; the `6`/seventh split is the canon "six named, a seventh the
  record will not keep." Never names the Seventh in the prop (no spoiler).
- **FACT 14/15** (the record receives/keeps you; you become the keepers) — the whole arc of the
  `6` plant: six prior groups kept, the group is the seventh, kept on the server not as a file but
  as hands; `recordReceives()` at M5 fills the struck row. SEALED-felt, never stated.
- **The keepers-as-you / `record-writes-you-in` thesis** — the dead uploader (Mara, a dead hand
  still keeping) is the first deniable instance of "a dead keeper is still keeping the record," the
  exact mechanic `record-writes-you-in` pays off at M4. No new FACT; a new *on-ramp* for it.
No new FACT, no new INV. (Confirmed against `WEB-MASTER §0.1/§0.2` — nothing to mint.)

### Found-documents / journals that anchor it (`[BUILT]` unless marked)
- `arc/lore/documents/kept-in-more-than-one-place.md` `[BUILT]` — the founder margin: "the record
  is kept in more than one place, against the loss of the first"; the keyed line decodes to
  `the-record-keeps`. **This is the in-corpus justification for both the lure page's existence AND
  Mara's "i set the copy where fire and water do not reach" provenance line.** The vignette's rune
  string reuses this exact decode.
- `arc/lore/documents/the-record-opens.md` `[BUILT]` — supplies the vignette's Beat-1 record
  fragment (the FACT 1/2 public lines ONLY) and the canon source of the `6`: "six are named in
  full, and there is a seventh mark the record will not keep."
- `arc/lore/documents/the-seventh-not-kept.md` `[BUILT]` — the Seventh canon the vignette's doused
  hearth + scraped wall point at *without naming* (spoiler discipline).
- `arc/corpus/journals-vaun-mara-sella.md` `[BUILT]` — Mara's locked voice; the lure-page
  provenance line must obey her grammatical fingerprint (deferred/citeable) and never contradict it.
- **`[GAP — TO BUILD] one tiny doc** `the-copy-i-kept.md`** (Mara's hand, `movement: 1`,
  `clue_bearing: false`) — OPTIONAL: a four-line in-corpus margin that *is* the lure-page
  provenance, so the page quotes a real document (un-orphans the uploader fully). Cuttable; the
  page can carry the line inline citing `kept-in-more-than-one-place` instead.

### NPC / Watcher voice keys (`discord/src/voice.ts`)
- `voice.recordOpened()` `[BUILT]` — reused verbatim as FB-1 (one voice on every surface) and as
  the vignette closing-page masthead.
- `voice.recordElsewhere(fragment)` `[BUILT]` — the M2 Discord nudge when the decoded path resolves
  (fragment passthrough); the cursed-map group hits this when they realize the address == the key.
- `voice.recordReceives()` `[BUILT]` — M5; fills the struck-seventh download row (felt, not stated).
- **`[GAP — one key] voice.recordFrameBreak()`** — the single new line (FB-2), in register,
  precision-gated, with the **count-callback** form as the default-safe body (§3) and an OPTIONAL
  conduct-callback overload `recordFrameBreak(conduct?)`. Add to the `voice` object; add to the
  `OracleVoiceKey` union only if a seeded puzzle/beat row references it (the on-arrival beat below).

### Cipher(s) / puzzle(s) it expresses (reuse the 11 built ciphers — adds none)
- **substitution (P4)** — the vignette's rune string → `the-record-keeps` (reuses `runes.ts`
  `RUNE_MAP`; the **same** decode as the in-server founder margin — one key, two surfaces). No new
  cipher.
- The vignette carries **no solvable cipher the player must crack to finish** (it is pre-literate by
  design, like the in-server prologue) — the rune string is a *recognition token* re-read in M2,
  not a day-zero gate. The server address is given in plain text alongside it.

### Beats / listeners / tables / seed rows / sites / website routes
- **Website route (the lure page):** `dashboard/src/app/record/[slug]/page.tsx` `[BUILT — needs 2
  additions]`: (a) **validate `slug === 'the-record-keeps'`** (today it ignores the slug — the one
  real gap), else an in-voice 404; (b) when the slug is the lure slug, render the **downloads
  block**: one legible recovered-file entry (the §3 map description + a link to the vignette asset)
  + the static `kept: 6` counter + the struck seventh row. Still server component, no client JS,
  `noindex`, reads only `v_record` (no new data). The base `record/page.tsx` is unchanged.
- **Vignette asset (the prop):** `[GAP — TO BUILD]` `public/the-hold/` (or a CDN link) — the
  downloadable datapack/world `.zip`. Built from vanilla structures + command-blocks + one datapack
  function carrying only FACT-1/2 strings + the server address + the rune string. **No engine, no
  Supabase, no keeper name past the Archivist** (§2f). Not committed to the plugin/engine tree.
- **Ignition detector (the third on-ramp):** `[GAP — small]` the showrunner sets `prologue_ignited`
  on **first group login that follows a vignette hand-off**. Cheapest form: the vignette's closing
  page tells the group to type one plain word in `#the-record` on arrival ("say *kept* when you are
  all in"); the BUILT `messageCreate` ignition detector (`prologue.ts`) already fires on a human
  post there. So **no new detector code** — the vignette routes into the BUILT detector. (A keener
  form: a dedicated `from_map` flag set by an arrival beat; optional, P2.)
- **On-arrival frame-break beat:** `[GAP — wiring]` one beat that, after ignition, emits
  `recordFrameBreak()` **once**, gated on the measured signal (count-callback default; conduct
  overload if the in-server conduct re-fires). Rides the BUILT `apply.ts`/one-shot-ack path — same
  shape as the in-server prologue ack (`postAck`), idempotent via the `acked` guard.
- **Tables/flags:** **none new.** Reuses `arc_state.flags.prologue_ignited` + the `acked` one-shot
  guard. (No `from_map` flag in the slice; optional later.)
- **Seed rows:** **none required.** If `recordFrameBreak` is emitted via a `puzzles`/beat row rather
  than the ack path, add ONE row keyed off `prologue_ignited` (`outcome_type:'lore'`,
  `voice_key:'recordFrameBreak'`, no door, gates nothing) — but the ack-path wiring needs no row.
- **sites.yml:** **none new on the server.** The vignette is client-side; the server uses the BUILT
  `first_report_lectern_01` + `first_marker_01` (the in-server prologue) unchanged.

### Side-quests it spawns or links
- **Links → `arg-leaves-the-game` (The Record website):** this idea is the **on-ramp** to that
  surface; it shares the route, the projection, the `the-record-keeps` slug, and the M5
  `recordReceives()` fill. (It does not duplicate the Record; it adds the lure/downloads face of it.)
- **Links → `cold-start-prologue`:** the off-server twin of the in-server ignition; they converge on
  one `prologue_ignited` state and one ack economy (§0).
- **Links → `record-writes-you-in`:** the dead-uploader (Mara, a dead hand still keeping) and the
  `6` (six prior groups kept) are the deniable day-zero plant for that idea's M4 "the journals were
  never closed; the record is still keeping us" payoff.
- **Links → `the-seventh-spine`:** the vignette's doused hearth + scraped-seventh wall are the
  Seventh's earliest, unnamed appearance (a pre-M1 plant for the M3 spine), and the `6`/seventh
  split rhymes the seventh-the-record-will-not-keep.
- **Spawns (optional P2) → a "find the other five copies" leaf:** a sharp group, having seen `6`,
  may hunt for the other downloads. The safe, non-orphan answer: the other five "copies" are the
  six keeper-stones in-world (the record kept in more than one place). A `dead_end`-doctrine taunt
  (`kind:count`) handles a literal "where are downloads 1–5?" probe: *"five were kept before the
  one you found. they are not files. you have read three of them already."* (Reuses the BUILT
  dead-end family; gates nothing.)

---

## 5. PLANT → PAYOFF — entries for the master ledger (`WEB-MASTER §9`)

> No plant without payoff; no payoff without plant. Append to the §9 ledger (next free row #24+).

| # | SEED (planted, inert) | Planted in | Inert meaning | Payoff in | True meaning (the "oh") | Status |
|---|---|---|---|---|---|---|
| **24** | the lure page's **`kept: 6`** download counter | **M0-remote** (day zero, the dead page) | a dead file nobody wanted | **M2 (soft) → M4 (hard) → M5 (felt)** | the six prior keeper-generations the record already did this to; the group is the seventh it will not keep *as a file* — it keeps them as hands (FACT 14/15) | NEW |
| **25** | the README line **"it does not connect to anything"** | **M0-remote** (the prop's cover) | mundane reassurance | **M4** | the *map* connects to nothing; the *server* does — the lie was true, we misread it as comfort | NEW |
| **26** | the **dead uploader `m.kept`** (Mara's hand) on the page | **M0-remote** | a person who abandoned a file | **M4** | no person uploaded it — the record *kept* it, in a dead keeper's hand, the way it keeps everything (the keepers-as-you thesis) | NEW |
| **27** | the vignette's **doused hearth + scraped-seventh wall** | **M0-remote** (the prop) | melancholy set-dressing | **M3→M4** | the Seventh, refused, named only later in-server; the prop rehearsed the spine | NEW |
| — | the vignette **rune string** → `the-record-keeps` | M0-remote | a decorative carving / the address | **M2** | the **same** founder-margin key (ledger #11); the address you typed was the record's own name | reuses #11 |

**The frame-break is itself the heaviest unplanted "oh":** the realization, on the server, that the
thing reacting to you is *not the map you downloaded* — a map cannot know the `6` it showed you, or
the conduct you just repeated. That category violation is the payoff of every quiet, under-promising
choice the prop made. The plant is the prop's restraint; the payoff is the server breaking frame.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Sub-thread | Lives in | Depends on | Depended on by | Priority | State |
|---|---|---|---|---|---|
| **The lure page** (`/record/the-record-keeps` + downloads block) | M0-remote (seen) → M5 (struck-7 fills) | the BUILT `record/[slug]/page.tsx` + `record-projection.ts` + `v_record`; the `kept-in-more-than-one-place` plant | nothing gates on it (additive) | **P1** | route BUILT; slug-validate + downloads block TODO |
| **The vignette** (downloadable datapack/world) | M0-remote | vanilla MC + `the-record-opens` public lines + the server address + the rune key | the frame-break (provides the "session they played alone") | **P1** (P2 for the full 6-room hold) | TODO (no engine) |
| **The frame-break** (`recordFrameBreak` + arrival wiring) | M1 (first server beat) | the BUILT `prologue.ts` ignition spine + `voice.recordOpened()`; ONE new voice key; the measured signal | the in-server Prologue (hands off to it) | **P0** | one voice key + ack-path wiring TODO |
| **The dead-uploader doc** (`the-copy-i-kept.md`, Mara) | M0-remote (page quotes it) | Mara's locked voice | the lure page's provenance line | **P2** (optional; inline-able) | TODO / cuttable |

**Hard dependency note.** The cursed map **gates nothing** — the campaign completes with the map,
the lure page, and the frame-break entirely absent (a group that joins the server cold gets the
in-server Prologue unchanged). Its value is: **(1)** the cold-start on-ramp for a remote/async
group (the one thing the in-server prologue can't do — §2i); **(2)** the YouTube cold-open (a dead
page + a `6` a viewer who never logs in can read); **(3)** three day-zero plants (#24–27) that
re-read across M2–M5. It is **strictly additive**, like the Record website it extends.

**Build order (smallest first):**
1. `voice.recordFrameBreak()` (one key, count-callback default body; conduct overload optional) +
   the OracleVoiceKey union entry **iff** seeded via a row. [tiny; unblocks FB-2]
2. Slug-validation (`slug === 'the-record-keeps'`) + the downloads block (static `kept: 6` + one
   recovered-file entry + struck-7) on `record/[slug]/page.tsx`. [the surface]
3. The vignette asset (scaled: 1-room → full 6-room hold), vanilla, no engine, no spoiler. [the prop]
4. Arrival wiring: route the vignette hand-off into the BUILT `messageCreate` ignition detector;
   emit `recordFrameBreak()` once on the measured signal via the BUILT ack path. [the frame-break]
5. (P2) `the-copy-i-kept.md` Mara doc; the conduct-callback FB-2(i); the optional `from_map` flag.

**Cut/defer ledger (final):** Live download counter / real uploader — **CUT** (static `6`, no
backend, §2c). In-map personalization (faking a name) — **CUT entirely** (precision; names live
in-server under measured gates, §2d). Full 6-room hold — **scaled** (1-room ships first; 6-room is
P1→P2, §2j). New FACT/INV/cipher/flag/site — **none** (this is an on-ramp, not an invention).
