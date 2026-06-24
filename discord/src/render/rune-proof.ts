/**
 * rune-proof.ts — a human-readable preview of the keepers' alphabet (dev aid, not shipped).
 *   npx tsx src/render/rune-proof.ts   → discord/out/rune-proof.png
 * Renders A–Z + 0–9 and a sample word in cream on navy so the carved glyphs can be eyeballed for
 * distinctness/legibility. The shipped atlas is white-on-transparent (build-runepack.ts).
 */
import { writeFile, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';
import { renderRunes, runesWidth, GLYPH_H } from '../forge/runes.js';

const here = dirname(fileURLToPath(import.meta.url));

const rows = ['ABCDEFGHIJKLM', 'NOPQRSTUVWXYZ', '0123456789', 'BOW OFFERING'];
const PAD = 40;
const ROW_GAP = 28;
const width = Math.ceil(Math.max(...rows.map((r) => runesWidth(r))) + PAD * 2);
const rowH = GLYPH_H + ROW_GAP;
const height = PAD * 2 + rows.length * rowH;

const groups = rows
  .map((r, i) => `<g transform="translate(${PAD} ${PAD + i * rowH})" stroke="#efe7d6" color="#efe7d6">${renderRunes(r)}</g>`)
  .join('');

const svg =
  `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">` +
  `<rect width="100%" height="100%" fill="#11161f"/>${groups}</svg>`;

const png = new Resvg(svg.replace(/currentColor/g, '#efe7d6'), { fitTo: { mode: 'width', value: width * 2 } })
  .render()
  .asPng();

const out = resolve(here, '../../out/rune-proof.png');
await mkdir(dirname(out), { recursive: true });
await writeFile(out, png);
console.log(`rune-proof: wrote ${out} (${width}x${height})`);
