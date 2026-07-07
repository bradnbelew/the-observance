# The Observance — resource pack

The single client-facing asset. One "accept resource pack?" click is the friend group's only install
friction (Path A). It carries the **keepers' alphabet** (the rune font the cipher requires) and the
**dark's voice** (the ambient/ritual sound events).

## What's generated vs. authored

| File | Source | Status |
|---|---|---|
| `assets/observance/textures/font/runes.png` | **generated** from `discord/src/forge/runes.ts` | ✅ built — `npm run pack:build` |
| `assets/observance/font/runes.json` (bitmap provider → font `observance:runes`) | **generated** | ✅ built |
| `pack.mcmeta` | generated | ✅ built |
| `assets/observance/sounds.json` (11 sound events: 4 ambience + `keeper_voice` + 6 named per-keeper clips) | generated | ✅ built |
| `assets/observance/sounds/*.ogg` (the audio, all 11 files) | **authored audio** | ✅ shipped |

The rune atlas is rendered from the **same** `runes.ts` the Discord clue cards use, so an in-world
carving and its Discord card are guaranteed to decode to the same letters — the cipher can never
silently disagree across surfaces. Re-run `npm run pack:build` after any `runes.ts` change; eyeball the
glyphs with `npm run pack:proof` (writes `discord/out/rune-proof.png`).

## Using the font from a beat

Render rune text by setting the font on the JSON text component:

```json
{ "text": "BOW OFFERING KEPT LIGHT", "font": "observance:runes" }
```

`SignWriteBeat`, `LecternFillBeat`, `BookAppearsBeat`, `BossBarBeat`, and `PrivateMessageBeat` all
already write text — they only need the `observance:runes` font tag to carve it. The runes gate behind
the resource-pack load check (`ResourcePackTracker.isLoaded`, MF-11) so a player who declined the pack
gets an ASCII fallback, never tofu boxes.

## Sound events (sounds.json)

`observance:whisper`, `observance:drone_low`, `observance:stone_breath`, `observance:cold_toll` —
played by `PrivateSoundBeat` by key (zero new code). **Author MONO `.ogg`** or they will not attenuate
with distance. Drop them under `assets/observance/sounds/`.

Also shipped: `observance:keeper_voice` (the generic fallback `SpatialVoiceBeat` actually plays today)
plus 6 named per-keeper clips (`keeper_voice.vaun/.mara/.sella/.orin/.brann/.iss`) — real, authored audio,
all present on disk, but **no code currently selects the named ones**; nothing supplies
`observance:keeper_voice.<name>` to `SpatialVoiceBeat`'s `resolveClip()`, so only the generic fallback
ever plays in practice. Wiring a per-keeper selector is a real, undecided feature (same shape as the
`keeper.ts` gap) — not a bug in this pack.

> **Build-tooling note:** `discord/src/render/build-runepack.ts` now emits the full launch manifest:
> the 4 ambient events, `keeper_voice`, all 6 named keeper clips, and the 1.21.11 `[75,0]` pack format.
> Re-running `npm run pack:build` is safe for the resource-pack metadata and sound manifest.

## GO-LIVE (the only manual steps)

1. **Audio:** done — all 11 `.ogg` files named in `sounds.json` are already present under `assets/observance/sounds/`.
2. **pack_format:** `pack.mcmeta` ships format `[75,0]` via `min_format`/`max_format` — correct for the pinned server **MC 1.21.11** (resource-pack format 75.0). No change needed unless the server version moves.
3. **ascent/height:** `runes.json` ships `ascent 13 / height 16`. Tune in-game for sign/book legibility.
4. **Zip + host** the pack: for final launch, run `tools/package_launch_bundle.ps1`; for asset-only rebuilds,
   run `tools/package_assets.ps1`. Upload `observance-resourcepack.zip`, then run
   `tools/set_resource_pack_config.ps1 -Url <hosted-https-zip-url>` so the URL and current zip SHA1 are set
   together in `plugin/src/main/resources/config.yml` / live `plugins/Observance/config.yml`.
   The plugin uses Paper's pack-stack push (`addResourcePack`) on join.
   Keep `resource-pack.prompt` non-empty: it is the one client-facing install moment, and should name both
   the alphabet and the voice/sound without becoming operator narration.
   The generated zip intentionally includes only runtime pack roots (`pack.mcmeta`, optional `pack.png`,
   `assets/`, and optional `data/`), so README/design edits do not churn the hosted resource-pack hash.
5. *(Optional)* drop in block/entity reskins (the Watcher husk, carved structure blocks) under
   `assets/minecraft/textures/` — cosmetic, no code.

> The fog world (Undercroft) is a **datapack** (`dimension_type` effects), NOT this pack — Java fog is
> not resource-pack-driven. See `design/atmosphere-stack.md §3.5`.
