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
| A2 | 11 sound files (`.ogg`) | **Required-ish** (degrades to silence) | ✅ **format verified by audit** — still do a human listening pass for taste |
| — | *Package + host the resource pack* | **Required** | ops, not media (see §Ops) |
| B1 | Found-footage "recovered recording" video | Hero artifact (enrichment) | produced, hosted on YouTube, still gated |
| B2 | `the-hold.zip` — the downloadable lure vignette | Hero artifact (enrichment) | produced, in dashboard public |
| B3 | Recovered Drive folder + spectrogram image | Hero artifact (enrichment) | produced + local-staged; host before live |
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

### A2 — The audio (11 `.ogg` files) ✅ FORMAT VERIFIED, LISTEN FOR TASTE
- **Location:** `resourcepack/assets/observance/sounds/`. All 11 are present and the full audit now verifies
  they are Vorbis `.ogg`, mono, non-trivial, and duration-checked. That proves they will spatialize correctly
  in Minecraft; it does **not** prove they are the final sound design. Listen to each and replace any you
  don't love.
- **HARD FORMAT RULE (from `resourcepack/README.md`):** these MUST be **MONO** `.ogg`. A stereo file will
  **not attenuate with distance and will not spatialize** — which breaks the whole "a whisper only you hear,
  positioned behind you" effect. If you re-make them, export mono.
- **The four ambient / toll sounds** (played by `PrivateSoundBeat` / biome `mood_sound` by key):
  - `whisper.ogg` — the core "it's near you" whisper. Also the Undercroft dimension's biome mood sound.
  - `drone_low.ogg` — low dread bed.
  - `stone_breath.ogg` — the world "breathing"; stone-y, cold.
  - `cold_toll.ogg` — the reversible cold-toll cue (played when a Whisper is spent / a custom toll lands).
- **The six keeper voices, plus one fallback** (played by `SpatialVoiceBeat` — "the Ear," the keeper speaks
  near one player):
  - `keeper_vaun.ogg`, `keeper_mara.ogg`, `keeper_sella.ogg`, `keeper_orin.ogg`, `keeper_brann.ogg`,
    `keeper_iss.ogg` — one short spoken/whispered clip per keeper, in-character (Vaun hoarder-gruff, Mara
    soft/referential, Sella child/drowned, Orin clipped/silent, Brann doubled/nightwatch, Iss warm-then-cold).
  - `keeper_voice.ogg` — the **generic fallback** used if a specific keeper clip is missing. Keep it.
    (Seven files total; six keepers, not seven — don't read this as a seventh keeper's voice.)
- **Design note:** these are the ONE thing "I can't generate game audio" — genuinely yours to source or
  make. Restraint wins: dead air + one wrong sound beats a busy soundscape. If you never touch them, the
  game degrades to silence gracefully (no crash) — but the "it heard you / the Ear" beats go quiet.

---

## B. THE HERO ARTIFACTS (the "oh god it's real" moments — enrichment, wired, degrade safely)

Real content beats anything AI-generated here, because the whole payoff of "wait, there are actually
coordinates in this file" only lands if the data is *really there to extract*. Each of these is wired with a
graceful placeholder; **wire your real version in when it's ready. Until then, don't plant the in-world clue
that points at it** (or the trail dead-ends / a link 404s).

### B1 — The found-footage recovered clips PRODUCED + HOSTED
- **Current state:** four clips are produced and recorded in `design/MANUAL-MEDIA-STAGING.md` with local
  paths, sizes, SHA1 values, payloads, and hosted YouTube URLs. The URLs passed HTTP reachability and
  operator playback checks, but `media_clip_01_ready` through `media_clip_04_ready` must remain false/dormant
  until the operator intentionally arms each matching clip at its story gate.
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
- **Hidden payloads are fixed in `design/MANUAL-MEDIA-PACKET.md`:**
  - clip 1 resolves to `ASH-13`.
  - clip 2 resolves to `where the reeds fold back`.
  - clip 3 resolves to `stay awake`.
  - clip 4 is late-only and resolves to `six return one is not kept`.
- **Where it wires:** each produced clip is hidden behind its own `media_clip_0N_ready` flag; solving the
  typed payload flips a matching `media_*_read` flag. These are optional evidence flags, not main-spine gates.
- **⚠️ OPEN DECISION (you raised this):** you're weighing making the **cold open** a found-footage video
  instead of the in-base staged anomaly. Right now the *live, built* cold open is the in-base anomaly
  (`/observance placeprologue` — a marker that knows a real number); found-footage is a *separate mid/late*
  artifact. If you want found-footage to BE the entry vehicle, that's a design change (not just an asset) —
  tell me and I'll re-sequence the opening around it. **Decide the slot before you shoot it.**
- **Assist offer:** I can generate a *draft* clip via HyperFrames (tooling installed) as a wired,
  testable placeholder so the beat works before your real one lands.

### B2 — `the-hold.zip` — the downloadable lure vignette PRODUCED
- **Current state:** the clean sendable zip is present at `dashboard/public/the-hold/the-hold.zip`.
  It is 17296 bytes, SHA1 `89e13b613e370fab47a9b5544dced4646737a96e`, and its 22 zip entries were checked
  for README/spoiler/backup files. The compact payload is intentional: its datapack constructs six roofed,
  bounded rooms on first load. The final room splits the destination into Copperline,
  Hosting, common web, and service 1842, pointing to the public directory without exposing a server port or raw endpoint.
- **What it is:** a small, offline, **single-player Minecraft world + datapack** (a "cursed map"): a linear
  ~10–15 min walk through a cold stone hold that ends by pointing at the server. It's the discovered
  download on the record website's lure page (`/record/the-record-keeps`).
- **Where it wires:** `dashboard/public/the-hold/the-hold.zip`. The lure page withholds the download link until this file exists
  (`the-hold.zip`, with the README "lie": *"a small offline map. single player. no mods. ~fifteen minutes.
  it does not connect to anything. play it through and it will tell you where the rest is kept."*).
  The file is now in the dashboard public folder. Verify the deployed `/the-hold/the-hold.zip` URL before
  planting the in-world clue to the lure slug; do NOT plant the in-world clue until that deployed URL works.
- **Production build spec:** `design/prologue/PROLOGUE-VIGNETTE.md` records the generated six-room route,
  exact evidence, containment, current 1.21.11 text-component format, and runtime proof. It carries m.kept,
  the six/missing-seventh pattern, `the record keeps`, and the four-field Copperline handoff. The offline
  map does not perform the server-side frame-break and contains no server machinery or Supabase URL.
- **Shipped form:** the complete six-room build is the release artifact. The retired one-room cut and
  decorative rune-string proposal are not present in the production zip.

**Current handoff rule:** the map points to the abandoned public listing first. The live Paper address is
shown by the website only when `NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS` is configured; the zip should never
contain the server port.

### B3 — The recovered Drive folder + the spectrogram image PRODUCED + LOCAL-STAGED
- **Current state:** the archive packet is produced and recorded in `design/MANUAL-MEDIA-STAGING.md` with
  local path, zip SHA1, spectrogram audio hash, and hosted Dropbox folder URL. The URL is reachable, the
  Dropbox contents are checked, and the spectrogram payload has been verified, but `recovered_archive_ready`
  must remain false/dormant until the group has earned the gated context.
- **What it is:** the payload behind the `spine-recovered-archive` puzzle (an optional external-research
  surface — the spine never depends on it). A carved in-world string resolves to an **unlisted Google Drive
  (or similar) folder**; most of what's in it is atmospheric lore, but **one "image" is actually an audio
  spectrogram** with a name hidden in the waveform.
- **What to make:**
  1. A **hostable folder** (Drive/Dropbox/static dir) of "recovered" files — degraded scans, half-corrupt
     screenshots, a couple of documents. Mostly flavor.
  2. **One spectrogram-bearing audio/image artifact** whose audio, read as a spectrogram, spells
     **I WAS NOT KEPT**. That exact phrase is the optional archive answer; do not hide `AVERYN` here unless
     the group has already earned the name in-world. (Tools: Audacity spectrogram view, or Photosounder, to
     paint text into audio.)
  3. Optionally a **waveform/spectrogram still** that feeds the record website's recovered-archive entry.
- **Where it wires:** `spine-recovered-archive` (a wired puzzle awaiting its artifact). Answer is typed:
  `i was not kept`. No in-world site — it's an external → web loop.

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
- **Verify `the-hold.zip`** at the deployed `/the-hold/the-hold.zip` URL before planting its clue.
- **Found-footage clips** (B1) are hosted on YouTube and operator-checked; flip a `media_clip_0N_ready` flag
  only when that clip should enter play.
- **Recovered archive folder** (B3) is hosted on Dropbox and operator-checked; flip `recovered_archive_ready`
  only when the story gate is ready.

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
game looks right. The audit now blocks broken/stereo/tiny audio; **listen to the 11 OGGs and replace any
placeholder-grade ones (mono only).** Then the
three hero artifacts (B1 found-footage, B2 the-hold.zip, B3 spectrogram/Drive) are pure enrichment. B1 is
produced, hosted on YouTube, and operator-checked but remains behind the `media_clip_0N_ready` gates; B3 is hosted on Dropbox and operator-checked but remains behind the
`recovered_archive_ready` story gate.
# THE OBSERVANCE - LEGACY MEDIA BACKGROUND

> Active production source: `design/MANUAL-MEDIA-PACKET.md`. That packet is the current remake-aligned
> checklist for exactly what to make, how to host it, and where it enters the ARG. This older guide remains
> as background and technique reference only.
