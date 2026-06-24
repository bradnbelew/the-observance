/**
 * build-runepack.ts — generate the Observance resource pack from the SINGLE SOURCE OF TRUTH.
 *
 *   npx tsx src/render/build-runepack.ts   (or: npm run pack:build)
 *
 * The cipher's rune visuals are NOT a go-live art blocker: this renders the keepers' alphabet
 * (forge/runes.ts — the same carved glyphs the Discord clue cards use) into a Minecraft bitmap-font
 * atlas + provider, so the in-world runes and the Discord runes are guaranteed identical (decode never
 * disagrees). Writes a complete, loadable pack under repo-root /resourcepack:
 *
 *   resourcepack/
 *     pack.mcmeta                                   — manifest
 *     assets/observance/font/runes.json             — bitmap provider → font id "observance:runes"
 *     assets/observance/textures/font/runes.png     — the carved-glyph atlas (generated here)
 *     assets/observance/sounds.json                 — the dark's voice (event → ogg; oggs are go-live)
 *
 * Beats render rune text by setting font "observance:runes" on the JSON text component (SignWrite/
 * Lectern/Book/BossBar/PrivateMessage). The .ogg audio files are the only go-live asset (binary audio
 * can't be generated here) — sounds.json names exactly what to drop in.
 *
 * Deterministic + pure-ish (only writes files). No story text. resvg renders the SVG → PNG.
 */
import { writeFile, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';
import { RUNE_LETTERS, glyphPath, GLYPH_W, GLYPH_H } from '../forge/runes.js';

const here = dirname(fileURLToPath(import.meta.url));
const PACK = resolve(here, '../../../resourcepack');

/** Cipher carves A–Z + digits (coordEncode uses digits). Marks/space stay default-font. */
const CHARS: readonly string[] = [...RUNE_LETTERS, ...'0123456789'];
const COLS = 12;
const ROWS = Math.ceil(CHARS.length / COLS);
const ATLAS_W = COLS * GLYPH_W;
const ATLAS_H = ROWS * GLYPH_H;
/** Private-use filler for unused trailing grid cells (never typed → never rendered). */
const FILLER = '';

function atlasSvg(): string {
  const cells = CHARS.map((ch, i) => {
    const x = (i % COLS) * GLYPH_W;
    const y = Math.floor(i / COLS) * GLYPH_H;
    return `<g transform="translate(${x} ${y})">${glyphPath(ch)}</g>`;
  });
  // White strokes on a transparent ground (MC tints the white glyph by the text color in-game).
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${ATLAS_W}" height="${ATLAS_H}" ` +
    `viewBox="0 0 ${ATLAS_W} ${ATLAS_H}" fill="none">${cells.join('')}</svg>`
  ).replace(/currentColor/g, '#ffffff');
}

function fontProviderRows(): string[] {
  const rows: string[] = [];
  for (let r = 0; r < ROWS; r++) {
    const slice = CHARS.slice(r * COLS, (r + 1) * COLS);
    while (slice.length < COLS) slice.push(FILLER);
    rows.push(slice.join(''));
  }
  return rows;
}

async function main(): Promise<void> {
  // 1) the atlas PNG (rendered at 4× for crisp downscaling in-game).
  const svg = atlasSvg();
  const png = new Resvg(svg, { fitTo: { mode: 'width', value: ATLAS_W * 4 } }).render().asPng();
  await mkdir(resolve(PACK, 'assets/observance/textures/font'), { recursive: true });
  await writeFile(resolve(PACK, 'assets/observance/textures/font/runes.png'), png);

  // 2) the bitmap font provider → font id "observance:runes". ascent/height are tunable in-game.
  const provider = {
    providers: [
      {
        type: 'bitmap',
        file: 'observance:font/runes.png',
        ascent: 13,
        height: 16,
        chars: fontProviderRows(),
      },
    ],
  };
  await mkdir(resolve(PACK, 'assets/observance/font'), { recursive: true });
  await writeFile(resolve(PACK, 'assets/observance/font/runes.json'), JSON.stringify(provider, null, 2) + '\n');

  // 3) the manifest. pack_format is version-coupled — 34 ≈ MC 1.21; tune to the server.
  const mcmeta = {
    pack: {
      pack_format: 34,
      description: 'The Observance — the keepers’ alphabet, and the dark that keeps them.',
    },
  };
  await writeFile(resolve(PACK, 'pack.mcmeta'), JSON.stringify(mcmeta, null, 2) + '\n');

  // 4) the dark's voice — sound EVENTS (the .ogg files are the sole go-live binary asset). MONO oggs
  //    only, or they will not attenuate spatially. PrivateSoundBeat plays these by key, zero new code.
  const sounds = {
    'whisper': { sounds: [{ name: 'observance:whisper', stream: false, attenuation_distance: 16 }] },
    'drone_low': { sounds: [{ name: 'observance:drone_low', stream: true }] },
    'stone_breath': { sounds: [{ name: 'observance:stone_breath' }] },
    'cold_toll': { sounds: [{ name: 'observance:cold_toll', attenuation_distance: 24 }] },
  };
  await mkdir(resolve(PACK, 'assets/observance/sounds'), { recursive: true });
  await writeFile(resolve(PACK, 'assets/observance/sounds.json'), JSON.stringify(sounds, null, 2) + '\n');

  console.log(
    `runepack: built ${CHARS.length} glyphs (${COLS}x${ROWS} atlas ${ATLAS_W}x${ATLAS_H}@4x), ` +
      `font observance:runes, 4 sound events. → resourcepack/`,
  );
}

main().catch((e) => {
  console.error('runepack build failed:', e instanceof Error ? e.message : String(e));
  process.exit(1);
});
