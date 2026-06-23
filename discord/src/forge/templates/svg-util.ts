/**
 * svg-util.ts — helpers that let satori embed arbitrary SVG.
 *
 * satori lays out HTML/flexbox and text, but it does NOT inline a raw SVG
 * fragment as a child node. The reliable, documented way to put bespoke vector
 * art (our carved rune <g> fragment, the brand sigil, torn map edges, redaction
 * bars) into a satori card is to wrap that art in a complete <svg> document and
 * hand it to an <img> as a base64 data-URI. satori rasterises the <img> at its
 * declared box size. These helpers build those documents + data-URIs.
 *
 * PURE: string in, string out. No I/O.
 */
import { GLYPH_H } from '../runes.js';

/** Wrap raw SVG markup as a base64 data-URI for an <img src>. */
export function svgDataUri(svg: string): string {
  const b64 = Buffer.from(svg, 'utf8').toString('base64');
  return `data:image/svg+xml;base64,${b64}`;
}

/** XML-escape text destined for an SVG <text> node. */
export function xmlEscape(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

export interface RuneSvgOptions {
  /** total rune-run width in user units (meta.layout.width). */
  readonly runWidth: number;
  /** ink color for the carved strokes (currentColor in the fragment). */
  readonly color: string;
  /** optional uniform padding around the run inside the svg box. */
  readonly pad?: number;
  /**
   * Optional faint guide-stave color drawn behind the runes (a single hairline
   * baseline band). Omit for no guide.
   */
  readonly guide?: string;
}

/**
 * runeBlockSvg — take the forge's rune <g> fragment (which uses currentColor and
 * lays glyphs out in a GLYPH_H-tall band) and return a STANDALONE <svg> document
 * sized to the run, with `color` applied so currentColor resolves. This is what
 * gets embedded as an <img> in every template.
 *
 * The fragment is rendered as-is (it already positions each glyph); we only set
 * the viewBox to the run's natural bounds plus padding, and set `color`.
 */
export function runeBlockSvg(fragment: string, opts: RuneSvgOptions): string {
  const pad = opts.pad ?? 8;
  const w = Math.max(1, Math.round(opts.runWidth + pad * 2));
  const h = Math.round(GLYPH_H + pad * 2);
  const guide =
    opts.guide !== undefined
      ? `<line x1="${pad}" y1="${pad + GLYPH_H / 2}" x2="${w - pad}" y2="${
          pad + GLYPH_H / 2
        }" stroke="${opts.guide}" stroke-width="1" opacity="0.25" />`
      : '';
  return [
    `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" `,
    `color="${opts.color}" role="img">`,
    guide,
    `<g transform="translate(${pad} ${pad})">${fragment}</g>`,
    `</svg>`,
  ].join('');
}

/** Natural pixel size of a rune block svg (for the <img> box in satori). */
export function runeBlockSize(runWidth: number, pad = 8): { width: number; height: number } {
  return {
    width: Math.max(1, Math.round(runWidth + pad * 2)),
    height: Math.round(GLYPH_H + pad * 2),
  };
}
