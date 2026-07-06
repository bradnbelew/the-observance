/**
 * sigil.ts — load the brand sigil for the faint footer watermark.
 *
 * If D:\the-observance\brand\sigil.svg exists we read it once and cache the
 * raw SVG markup. If it is absent (or unreadable) we fall back to a simple
 * engraved ring placeholder so every template still gets a corner mark.
 *
 * PURE-ish: one cached filesystem read. No discord/supabase. The returned
 * string is a complete <svg> document suitable for embedding as an <img>
 * data-URI inside a satori card.
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { brand } from '../../brand.js';

const here = dirname(fileURLToPath(import.meta.url));

/**
 * Candidate locations for the sigil, tried in order. The canonical path is the
 * repo's brand/ directory; we also probe a couple of sensible relatives so the
 * watermark survives the package being relocated.
 *
 *   src/forge/templates -> ../../../../brand/sigil.svg  == <repo>/brand/sigil.svg
 */
const SIGIL_CANDIDATES: readonly string[] = [
  resolve(here, '../../../../brand/sigil.svg'),
  resolve(here, '../../../brand/sigil.svg'),
];

let cache: string | null = null;

/**
 * A minimal engraved-ring placeholder used when no sigil.svg is present.
 * Two concentric ash rings + a dashed keeper-ring + a gilt keystone dot,
 * echoing the real sigil's silhouette without depending on the file.
 */
function ringPlaceholder(): string {
  const { line, muted, accent } = brand.colors;
  return [
    `<svg width="512" height="512" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" role="img">`,
    `<title>The Observance — ring</title>`,
    `<circle cx="256" cy="256" r="210" fill="none" stroke="${muted}" stroke-width="3" opacity="0.55"/>`,
    `<circle cx="256" cy="256" r="190" fill="none" stroke="${line}" stroke-width="10" stroke-dasharray="3 22" stroke-linecap="round" opacity="0.8"/>`,
    `<circle cx="256" cy="256" r="120" fill="none" stroke="${muted}" stroke-width="2" opacity="0.4"/>`,
    `<circle cx="256" cy="256" r="18" fill="${accent}" opacity="0.85"/>`,
    `</svg>`,
  ].join('');
}

/**
 * getSigilSvg() — the sigil markup (real file if present, else the placeholder),
 * cached after the first call. Never throws: any read failure degrades to the
 * placeholder so a card always renders.
 */
export function getSigilSvg(): string {
  if (cache !== null) return cache;
  for (const path of SIGIL_CANDIDATES) {
    try {
      const raw = readFileSync(path, 'utf8').trim();
      if (raw.length > 0 && raw.includes('<svg')) {
        cache = raw;
        return cache;
      }
    } catch {
      // try the next candidate
    }
  }
  cache = ringPlaceholder();
  return cache;
}

/** True iff a real brand sigil file was found (vs. the drawn placeholder). */
export function sigilIsReal(): boolean {
  // Force a load, then report whether the cached markup is the placeholder.
  const svg = getSigilSvg();
  return !svg.includes('The Observance — ring');
}

/** Reset the cache (testing/hot-reload only). */
export function _resetSigilCache(): void {
  cache = null;
}
