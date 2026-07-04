# THE OBSERVANCE — MEDIA PRODUCTION GUIDE (everything you, Ethan, have to make)

> The one checklist of every human-made asset the game wants. The **code is done and green**; this is the
> "magic behind the curtain" that the code already has wiring + graceful fallbacks for. Verified against
> the live repo 2026-07-03. Each item says: what it is · where it lives / what wires to it · format specs ·
> hidden payload (if any) · current status. Ordered by priority: **does the game NEED it to run** vs.
> **does it make a moment land**.

---

## TL;DR — the short list

| # | Asset | Priority | Status |
|---|---|---|---|
| A1 | Rune font (`runes.png` + `runes.json`) | **Required** | ✅ **done, in repo** |
| A2 | 11 sound files (`.ogg`) | **Required-ish** (degrades to silence) | ⚠️ **present but UNVERIFIED quality** — listen + replace if placeholder |
| — | *Package + host the resource pack* | **Required** | ops, not media (see §Ops) |
| B1 | Found-footage "recovered recording" video | Hero artifact (enrichment) | ❌ **you produce** |
| B2 | `the-hold.zip` — the downloadable lure vignette | Hero artifact (enrichment) | ❌ **you produce** |
| B3 | Recovered Drive folder + spectrogram image | Hero artifact (enrichment) | ❌ **you produce** |
| C1–C2 | Optional stego image / audio voicemail | Nice-to-have | ❌ optional, unwired |

**Nothing in the B/C list blocks the spine.** The game plays start→finish with only A1 + A2 + ops. The B
items each enrich exactly one optional surface and degrade safely (a beat stays dormant, a link would 404 —
so you don't plant the in-world pointer until the file exists).

---

## A. THE RESOURCE-PACK ASSETS (required — the game renders wrong without them)

These are the assets that ship **inside** `observance-resourcepack.zip`. The pack is auto-pushed to every
player on join (now that the push half is wired — see the 2026-07-03 fix). Everything else in this file is
optional; **this section is the one that actually gates the experience.**

### A1 — The rune font ✅ DONE
- **Files:** `resourcepack/assets/observance/textures/font/runes.png` (67 KB) + `.../font/runes.json`.
- **What it is:** the keepers' alphabet — a 1:1 substitution glyph set. **Every carved cipher, the rosetta
  cribs, the name-on-wall beat, and every rune the game shows depend on this.** Without the pack live, all
  runes render as the vanilla `illageralt` fallback (still legible, but not the authored look).
- **Status:** built and in-repo. Nothing to make. Just make sure it ships in the hosted pack.

### A2 — The audio (11 `.ogg` files) ⚠️ PRESENT, VERIFY QUALITY
- **Location:** `resourcepack/assets/observance/sounds/`. All 11 are present and are **valid Ogg files**
  (12–54 KB each) — but they were added in an early "Wave 1" batch and I **cannot vouch that they are your
  final sound design vs. earlier placeholder audio.** Listen to each and replace any you don't love.
- **HARD FORMAT RULE (from `resourcepack/README.md`):** these MUST be **MONO** `.ogg`. A stereo file will
  **not attenuate with distance and will not spatialize** — which breaks the whole "a whisper only you hear,
  positioned behind you" effect. If you re-make them, export mono.
- **The four ambient / toll sounds** (played by `PrivateSoundBeat` / biome `mood_sound` by key):
  - `whisper.ogg` — the core "it's near you" whisper. Also the Undercroft dimension's biome mood sound.
  - `drone_low.ogg` — low dread bed.
  - `stone_breath.ogg` — the world "breathing"; stone-y, cold.
  - `cold_toll.ogg` — the reversible cold-toll cue (played when a Whisper is spent / a custom toll lands).
- **The seven keeper voices** (played by `SpatialVoiceBeat` — "the Ear," the keeper speaks near one player):
  - `keeper_vaun.ogg`, `keeper_mara.ogg`, `keeper_sella.ogg`, `keeper_orin.ogg`, `keeper_brann.ogg`,
    `keeper_iss.ogg` — one short spoken/whispered clip per keeper, in-character (Vaun hoarder-gruff, Mara
    soft/referential, Sella child/drowned, Orin clipped/silent, Brann doubled/nightwatch, Iss warm-then-cold).
  - `keeper_voice.ogg` — the **generic fallback** used if a specific keeper clip is missing. Keep it.
- **Design note:** these are the ONE thing "I can't generate game audio" — genuinely yours to source or
  make. Restraint wins: dead air + one wrong sound beats a busy soundscape. If you never touch them, the
  game degrades to silence gracefully (no crash) — but the "it heard you / the Ear" beats go quiet.

---

## B. THE HERO ARTIFACTS (the "oh god it's real" moments — enrichment, wired, degrade safely)

Real content beats anything AI-generated here, because the whole payoff of "wait, there are actually
coordinates in this file" only lands if the data is *really there to extract*. Each of these is wired with a
graceful placeholder; **wire your real version in when it's ready. Until then, don't plant the in-world clue
that points at it** (or the trail dead-ends / a link 404s).

### B1 — The found-footage "recovered recording" video ❌ YOU PRODUCE
- **What it is:** a degraded, found-not-produced clip — a "recovered recording," ~30–90 s, that surfaces on
  the **record website** as a single recovered-file entry and reads as *unearthed*, never a trailer.
- **Form + look:** unlisted YouTube or a hosted `.mp4`. Handheld / static-cam or "screen recording of a
  recording." Heavy degradation: VHS tracking lines, dropped frames, dead-air audio, a **wrong** burned-in
  timestamp. **No music, no titles, no editing polish.** The format-is-the-trusted-thing-corrupted is the
  whole analog-horror move. Silence + one wrong detail beats spectacle.
- **Two content slots — pick where it lands:**
  - *Early "oh" (mid session 1–2):* a recovered clip from **a prior group's last session** — mundane
    base-building, then ONE impossible frame (a door/marker changes on camera, or the Watcher's mark on a
    wall), then it cuts. Establishes "this happened to someone before us." (This is the one I dramatized in
    John's playthrough.)
  - *Finale lead-in:* **the Seventh, in the dark, waiting** — a figure far down, a slow pan, a held breath.
    The emotional gut-punch pointing at the reunion.
- **Hidden payload — pick 1–2, I wire the check:**
  - **Visible coordinates in one frame** (on a sign / F3 overlay, on-screen <1 s) → leads to an in-world
    place. *answer_kind: coords, or a token found there.*
  - **A word/name** burned into a corrupt frame or spelled by the wrong timestamps → typed answer.
  - **A spectrogram payload in the audio** → a word/coords visible only in the audio spectrogram.
  - **EXIF / filename / description-field payload** on the hosted file → the datamine reward.
- **Where it wires:** an in-world haunting beat (a whisper, the record) surfaces the URL; the payload
  resolves to a place or a typed token; solving flips a thread flag. Retrace-fair once solved.
- **⚠️ OPEN DECISION (you raised this):** you're weighing making the **cold open** a found-footage video
  instead of the in-base staged anomaly. Right now the *live, built* cold open is the in-base anomaly
  (`/observance placeprologue` — a marker that knows a real number); found-footage is a *separate mid/late*
  artifact. If you want found-footage to BE the entry vehicle, that's a design change (not just an asset) —
  tell me and I'll re-sequence the opening around it. **Decide the slot before you shoot it.**
- **Assist offer:** I can generate a *draft* clip via HyperFrames (tooling installed) as a wired,
  testable placeholder so the beat works before your real one lands.

### B2 — `the-hold.zip` — the downloadable lure vignette ❌ YOU PRODUCE
- **What it is:** a small, offline, **single-player Minecraft world + datapack** (a "cursed map"): a linear
  ~10–15 min walk through a cold stone hold that ends by pointing at the server. It's the discovered
  download on the record website's lure page (`/record/the-record-keeps`).
- **Where it wires:** `dashboard/public/the-hold/the-hold.zip`. The lure page already links it
  (`the-hold.zip`, with the README "lie": *"a small offline map. single player. no mods. ~fifteen minutes.
  it does not connect to anything. play it through and it will tell you where the rest is kept."*).
  **The file is NOT in the repo yet — until it's hosted, do NOT plant the in-world clue to the lure slug
  or the download 404s.**
- **Full build spec already written:** `design/prologue/PROLOGUE-VIGNETTE.md` — beat-by-beat, room-by-room,
  all vanilla blocks, gamerules locked, datapack-tick logic (no visible command-block clocks). It carries
  the dead-uploader (Mara, "m.kept"), the number **6** on a page, a closing rune string, and the
  frame-break — and **zero spoilers past FACT 1/2, zero server machinery, zero Supabase URL** beyond the
  public server address. Cracking the world file reveals only what the player already saw.
- **Scaled cut:** the **1-room version ships first** (spawn → one record lectern → the server pointer); the
  full 6-room hold is a later expansion. The 1-room form still carries everything above.
- **This is a build task more than an "art" task** — it's a Minecraft world + datapack, which is squarely in
  your wheelhouse. I can help author the datapack functions / tellraw lines if you want; the world geometry
  is yours to build.

### B3 — The recovered Drive folder + the spectrogram image ❌ YOU PRODUCE
- **What it is:** the payload behind the `spine-recovered-archive` puzzle (an optional external-research
  surface — the spine never depends on it). A carved in-world string resolves to an **unlisted Google Drive
  (or similar) folder**; most of what's in it is atmospheric lore, but **one "image" is actually an audio
  spectrogram** with a name hidden in the waveform.
- **What to make:**
  1. A **hostable folder** (Drive/Dropbox/static dir) of "recovered" files — degraded scans, half-corrupt
     screenshots, a couple of documents. Mostly flavor.
  2. **One spectrogram image** whose audio, read as a spectrogram (or the image itself viewed as one),
     spells a **name** — the answer the puzzle wants. The seed accepts phrasings like *"the name the
     spectrogram keeps."* (Tools: Audacity spectrogram view, or Photosounder, to paint text into audio.)
  3. Optionally a **waveform/spectrogram still** that feeds the record website's recovered-archive entry.
- **Where it wires:** `spine-recovered-archive` (a wired puzzle awaiting its artifact) + the coords-in-a-
  frame first-find aid. Answer is typed (the hidden name). No in-world site — it's an external → web loop.

---

## C. OPTIONAL / NICE-TO-HAVE (mentioned in the shot list, NOT wired to a specific puzzle)

Lower priority; make these only if you're having fun. None is wired, so each would need me to add a beat.
- **C1 — A steganographic image** (LSB-hidden text, or an image whose audio spectrogram shows coords) as a
  record attachment or a Drive file. Overlaps B3; do B3 first.
- **C2 — An audio artifact** — a voicemail / distorted recording with a real spectrogram or backmasked
  message. Pairs with the keeper-voice OGGs tonally.
- **C3 — "Recovery" screenshots** — a corrupted terminal, a half-deleted post — that read like a real leak.
  The record website already fakes this stylistically; a real prop image would deepen it.

---

## Ops (NOT media, but the media is useless until these happen — flagged so nothing's stranded)

- **Package + host the resource pack.** Zip `resourcepack/` → host it (any public HTTPS) → set
  `resource-pack.url` + `resource-pack.sha1` (40-char hex of the zip bytes) in `plugin/.../config.yml`.
  The push half is now wired, so once the URL is set every joiner is prompted. **This is what makes A1 + A2
  actually reach players.**
- **Host `the-hold.zip`** at `dashboard/public/the-hold/the-hold.zip` (B2) before planting its clue.
- **Host the found-footage clip** (B1) and give me the URL to wire the payload check.

---

## What is NOT media (so you don't go looking to "make" it)

- **All in-world text, keeper journals, Watcher lines, NPC dialogue, the finale prose** — authored, in-repo,
  in `voice.ts` / `voice.archive.ts` / `arc/lore/documents/`. Done.
- **The clue cards / carved rune stones** — generated by the forge from the seed at build/drip time. Done.
- **The record website** — built (`dashboard/`). Done.
- **The new server-shutdown ending's final message** — that's *text*, which I write in `voice.ts`, not media
  you produce. (If you want a final *sound* or a black-frame *video* as the server dies, that'd be a new
  C-tier asset — flag it when we build that ending.)

---

### One-paragraph priority read
Get the **resource pack packaged + hosted** (A1+A2 + ops) — that's the only thing that changes whether the
game looks right. **Listen to the 11 OGGs and replace any placeholder-grade ones (mono only).** Then the
three hero artifacts (B1 found-footage, B2 the-hold.zip, B3 spectrogram/Drive) are pure enrichment you can
add on your own schedule — each degrades safely, and I wire your real version in the moment you hand it over.
Decide the **found-footage slot** (cold-open vehicle vs. mid-game artifact) before you shoot B1, because that
changes what it needs to contain.
