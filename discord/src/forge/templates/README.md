# Clue Render Templates

Themed artifact PNGs for The Observance. Each template wraps the forge's carved
**rune** ciphertext (`forgeClue` → `meta` + `<g>` SVG fragment) in an editorial
frame and rasterises it with **satori → @resvg/resvg-js**, the same pipeline as
`src/render/`.

## Palette (`PALETTE`)

Derived from `src/brand.ts`:

| token   | value                      | use                                  |
| ------- | -------------------------- | ------------------------------------ |
| `field` | `#0E1116` (ink)            | near-black canvas                    |
| `panel` | `#161B22`                  | recessed surfaces / evidence strips  |
| `ink`   | `#E8E2D4` (parchment)      | primary carved/inked text            |
| `ash`   | `#2A313B` (line)           | hairline rules, glyph grid           |
| `muted` | `#8B93A1`                  | secondary text                       |
| `soul`  | `#6FB7C9`                  | soul-blue accent (keystone / signal) |
| `gilt`  | `#C8A24B`                  | gilt secondary signal                |
| `rust`  | `#B4543A` (danger)         | **redaction bars only**              |

## Templates

| kind              | look                                                         |
| ----------------- | ----------------------------------------------------------- |
| `parchmentCard`   | clean field note; runes in a recessed panel                 |
| `redactedDossier` | Archivist typed lines, rust redaction bars, REDACTED stamp  |
| `mapFragment`     | torn surveyor's chart, compass rose, place pin              |
| `runeCipherCard`  | cipher worksheet; runes foregrounded over a dotted grid     |
| `journalPage`     | ruled diary leaf with a rust margin rule                    |

Every card stamps a faint corner **sigil watermark** — `brand/sigil.svg` if it
exists, else a drawn ring placeholder (see `sigil.ts`).

## Entry point

```ts
import { renderClue, type ClueRenderSpec } from './forge/templates/index.js';

const png: Buffer = await renderClue({
  template: 'runeCipherCard',                 // optional; auto-picked if omitted
  clue: { cipher: 'caesar', text: 'BOW AT THE MARKER', shift: 7 },
  eyebrow: 'Cipherwork · Act I',
  title: 'The Keepers’ Script',
});
```

`renderClueDetailed(spec)` additionally returns `{ template, forged }` (solution,
`puzzleKey`, `meta`) for ledger/dashboard wiring.

## Visual QA

```
npm run sample   # renders ./out/clue-*.png (+ whisper-*.png) for eyeballing
```

## Files

- `index.ts`    — `PALETTE`, `ClueRenderSpec`, the 5 template fns, `pickTemplate`,
  `renderClue`, `renderClueDetailed`.
- `svg-util.ts` — `runeBlockSvg` / `runeBlockSize` / `svgDataUri` / `xmlEscape`
  (satori embeds bespoke SVG via `<img>` data-URIs).
- `sigil.ts`    — loads `brand/sigil.svg` (cached) or draws the ring placeholder.
