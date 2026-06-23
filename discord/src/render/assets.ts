/**
 * `npm run assets` — render the brand PNGs into ./out, and produce the bot
 * avatar (`brand/sigil.png`) from the canonical vector sigil.
 *
 * Outputs:
 *   ./out/sigil.png            — the Keeper's Eye sigil, rasterised from the
 *                                repo's brand/sigil.svg (or the ring placeholder
 *                                if that file is absent). 512×512.
 *   brand/sigil.png            — the same PNG, written to the repo's brand/ dir so
 *                                the showrunner can set it as the bot avatar.
 *   ./out/brand-mark.png       — square wordmark card.
 *   ./out/channel-*.png        — channel-art cards for the four rooms.
 *
 * Pure local render — no Discord, no Supabase, no network.
 */
import { writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { Resvg } from '@resvg/resvg-js';
import { brand } from '../brand.js';
import { brandCard, type BrandCardData } from './cards.js';
import { OUT_DIR, renderToFile } from './render.js';
import { getSigilSvg, sigilIsReal } from '../forge/templates/sigil.js';

const here = dirname(fileURLToPath(import.meta.url));
/** repo root brand/ dir (src/render -> ../../../brand). */
const BRAND_DIR = resolve(here, '../../../brand');

const ASSETS: Array<{ filename: string; data: BrandCardData }> = [
  { filename: 'brand-mark.png', data: { title: 'The Observance', subtitle: 'It is always watching.' } },
  { filename: 'channel-the-record.png', data: { title: 'The Record', subtitle: 'What is written, remains.' } },
  { filename: 'channel-cipherwork.png', data: { title: 'Cipherwork', subtitle: 'Every lock has a hand that fits.' } },
  { filename: 'channel-whispers.png', data: { title: 'Whispers', subtitle: 'Spend them wisely.' } },
  { filename: 'channel-the-ways.png', data: { title: 'The Ways', subtitle: 'All paths are observed.' } },
];

/** Rasterise the canonical sigil SVG to a square PNG buffer. */
function renderSigilPng(size = 512): Buffer {
  const resvg = new Resvg(getSigilSvg(), { fitTo: { mode: 'width', value: size } });
  return resvg.render().asPng();
}

async function main(): Promise<void> {
  await mkdir(OUT_DIR, { recursive: true });
  await mkdir(BRAND_DIR, { recursive: true });

  // The avatar: rasterise the Keeper's Eye sigil to PNG, twice.
  const sigil = renderSigilPng(512);
  const outSigil = resolve(OUT_DIR, 'sigil.png');
  const brandSigil = resolve(BRAND_DIR, 'sigil.png');
  await writeFile(outSigil, sigil);
  await writeFile(brandSigil, sigil);
  console.log(
    `rendered ${outSigil} and ${brandSigil}  [${sigilIsReal() ? 'from brand/sigil.svg' : 'ring placeholder — no brand/sigil.svg found'}]`,
  );

  // The brand + channel-art cards.
  const size = brand.canvas.assetSize;
  for (const { filename, data } of ASSETS) {
    const path = await renderToFile(brandCard(data), size, size, filename);
    console.log(`rendered ${path}`);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
