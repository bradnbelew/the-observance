/**
 * Font loading for satori.
 *
 * satori cannot read system fonts — it needs raw font buffers. Drop .ttf/.otf
 * files into discord/assets/fonts/ and map them here. We read them lazily and
 * cache the result. If a file is missing we throw a clear, actionable error.
 */
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import type { Font } from 'satori';
import { brand } from '../brand.js';

const here = dirname(fileURLToPath(import.meta.url));
/** discord/assets/fonts (src/render -> ../../assets/fonts). */
const FONTS_DIR = resolve(here, '../../assets/fonts');

interface FontSpec {
  file: string;
  name: string;
  weight: Font['weight'];
  style: Font['style'];
}

/**
 * Expected font files. Provide real fonts under these names, or change the
 * filenames here to whatever you ship. Keep at least one regular weight for
 * `brand.fonts.body`.
 */
const SPECS: FontSpec[] = [
  { file: 'body-regular.ttf', name: brand.fonts.body, weight: 400, style: 'normal' },
  { file: 'body-bold.ttf', name: brand.fonts.body, weight: 700, style: 'normal' },
  { file: 'mono-regular.ttf', name: brand.fonts.mono, weight: 400, style: 'normal' },
];

let cache: Font[] | null = null;

/** Load (and cache) the satori font set. Throws if any file is missing. */
export async function loadFonts(): Promise<Font[]> {
  if (cache) return cache;

  const fonts = await Promise.all(
    SPECS.map(async (spec) => {
      const path = resolve(FONTS_DIR, spec.file);
      try {
        const data = await readFile(path);
        return { name: spec.name, data, weight: spec.weight, style: spec.style } as Font;
      } catch {
        throw new Error(
          `[fonts] Missing font file: ${path}\n` +
            `Place a .ttf/.otf there (see src/render/fonts.ts SPECS) before rendering.`,
        );
      }
    }),
  );

  cache = fonts;
  return fonts;
}
