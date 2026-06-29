/**
 * forge/templates/index.ts — CLUE RENDER TEMPLATES.
 *
 * Five themed artifact templates that turn a forged clue (ciphertext carved as
 * the keepers' runes, from forge/index.ts) into a finished PNG in The
 * Observance's editorial palette:
 *
 *   - parchmentCard    : a plain field-note card; the clean default.
 *   - redactedDossier  : the Archivist's style — typed lines with rust
 *                        redaction bars blacking out the leaked words.
 *   - mapFragment      : a torn surveyor's chart corner; runes pinned to a place.
 *   - runeCipherCard   : a cipher worksheet — the carved runes foregrounded with
 *                        a faint glyph grid and the key-hand named.
 *   - journalPage      : a ruled keeper's diary leaf with a marginal rule.
 *
 * Each takes a clue spec + the rune SVG fragment from the forge and returns a
 * PNG Buffer. A faint footer watermark of the brand sigil is stamped in a
 * corner of every card (real brand/sigil.svg if present, else a ring).
 *
 * Rendering uses satori (layout/text -> SVG) + @resvg/resvg-js (SVG -> PNG),
 * exactly the pipeline in src/render/. Bespoke vector art (runes, sigil,
 * redaction bars, torn edges) is embedded via <img> data-URIs because satori
 * does not inline raw SVG children.
 */
import { brand } from '../../brand.js';
import { forgeClue, type ClueSpec, type ForgedClue } from '../index.js';
import { el, renderPng, type VNode } from '../../render/render.js';
import { getSigilSvg } from './sigil.js';
import { runeBlockSvg, runeBlockSize, svgDataUri } from './svg-util.js';
import {
  embedRuneLayer,
  stampRuneLayerPayload,
  runeLayerSize,
  ISS_STEGO_PAYLOAD,
} from '../stego.js';

// ---------------------------------------------------------------------------
// PALETTE — the task's name for the brand colour tokens. near-black field,
// ash lines, soul-blue accent, rust reserved for redaction only.
// ---------------------------------------------------------------------------

export const PALETTE = {
  /** near-black field */
  field: brand.colors.ink,
  /** raised surface */
  panel: brand.colors.panel,
  /** primary carved/inked text (parchment) */
  ink: brand.colors.parchment,
  /** ash hairline rules + secondary text */
  ash: brand.colors.line,
  muted: brand.colors.muted,
  /** soul-blue accent (signal / keystone). brand.accent is the gilt signal. */
  soul: '#6FB7C9',
  /** gilt secondary signal (kept from brand) */
  gilt: brand.colors.accent,
  /** rust — ONLY for redaction bars / danger marks */
  rust: brand.colors.danger,
} as const;

const FONT = brand.fonts;
const CANVAS = brand.canvas; // clueWidth 1200 x clueHeight 630

// ---------------------------------------------------------------------------
// The render spec — a clue's forge spec plus presentation hints.
// ---------------------------------------------------------------------------

export type TemplateKind =
  | 'parchmentCard'
  | 'redactedDossier'
  | 'mapFragment'
  | 'runeCipherCard'
  | 'journalPage';

export interface ClueRenderSpec {
  /** the forge clue spec (which cipher + payload). */
  readonly clue: ClueSpec;
  /**
   * Which template to render. If omitted, renderClue() picks one
   * deterministically from the cipher kind + puzzleKey (see pickTemplate).
   */
  readonly template?: TemplateKind;
  /** Eyebrow / kicker line, e.g. "ARCHIVE FRAGMENT · ACT II". */
  readonly eyebrow?: string;
  /** Headline / title for the artifact. */
  readonly title?: string;
  /**
   * Body lines — the in-character framing around the runes. For redactedDossier
   * each line may carry redactions (see DossierLine). For the others these are
   * plain prose lines.
   */
  readonly lines?: readonly string[];
  /** Footer note (right side), e.g. "stamped by the Archivist". */
  readonly footer?: string;
  /**
   * redactedDossier only: structured lines whose [[bracketed]] spans become
   * rust redaction bars. If omitted, `lines` is used verbatim (no redaction).
   */
  readonly dossier?: readonly string[];
  /** mapFragment only: a place label pinned under the runes. */
  readonly place?: string;
  /** runeCipherCard only: override the key-hand hint (defaults to meta.keyHint). */
  readonly keyHint?: string;
  /**
   * P17 steganography (WEB-MASTER §1.M2): if set, composite a FAINT second rune
   * layer carrying this payload into the card's rune block — the "second door" to
   * a cipher key. Used ONLY on the Iss card, where the payload is his name / the
   * Vigenère key. Pass `true` to use the canonical Iss key (ISS_STEGO_PAYLOAD), or
   * an explicit uppercase letter string. Omit (default) for every other card.
   */
  readonly stego?: boolean | string;
}

/** Resolve the spec's `stego` flag to a concrete payload string (or undefined). */
function stegoPayload(spec: ClueRenderSpec): string | undefined {
  if (spec.stego === undefined || spec.stego === false) return undefined;
  return spec.stego === true ? ISS_STEGO_PAYLOAD : spec.stego;
}

// ---------------------------------------------------------------------------
// Shared chrome helpers (satori VNodes).
// ---------------------------------------------------------------------------

/**
 * imgNode — build a satori <img> VNode. satori needs `src`, `width`, `height`
 * as element PROPS (siblings of `style`), which the project's `el()` helper does
 * not do (it routes its second arg entirely into `style`), so images are built
 * directly here.
 */
function imgNode(
  src: string,
  width: number,
  height: number,
  style: Record<string, unknown> = {},
): VNode {
  return { type: 'img', props: { src, width, height, style: { display: 'flex', ...style } } };
}

/** The faint corner sigil watermark, as a positioned <img>. */
function sigilWatermark(size = 132, opacity = 0.1): VNode {
  return imgNode(svgDataUri(getSigilSvg()), size, size, {
    position: 'absolute',
    right: 36,
    bottom: 28,
    opacity,
  });
}

/** Build the carved-rune <img> node from a forged clue. */
function runeImg(
  forged: ForgedClue,
  color: string,
  opts: { scale?: number; guide?: string; maxWidth?: number; stego?: string } = {},
): VNode {
  const runWidth = forged.meta.layout.width;
  let svg = runeBlockSvg(forged.svg, { runWidth, color, guide: opts.guide });
  // P17 stego (WEB-MASTER §1.M2): on the Iss card ONLY, composite a faint second
  // rune layer carrying the Vigenère key INSIDE the same host <svg>, so it
  // rasterises as one image and survives Discord's PNG re-encode exactly like the
  // primary carving. Positioned just under the primary run, very dim. Opt-in via
  // the spec's `stego` payload — every other card is byte-identical to before.
  if (opts.stego) {
    svg = withStegoRuneLayer(svg, opts.stego, runWidth);
  }
  const nat = runeBlockSize(runWidth);
  // Scale to fit a maxWidth if the run is very long; preserve aspect ratio.
  const scale = opts.scale ?? (opts.maxWidth ? Math.min(1, opts.maxWidth / nat.width) : 1);
  return imgNode(svgDataUri(svg), Math.round(nat.width * scale), Math.round(nat.height * scale));
}

/**
 * withStegoRuneLayer — splice the faint second-rune-layer into the host rune-block
 * <svg> just before its closing tag. The layer is centered under the primary run
 * (which svg-util pads by 8 and draws GLYPH_H tall); we offset it down by ~62% of
 * the band so it reads as a ghost beneath the main marks, not over them. Pure.
 */
function withStegoRuneLayer(hostSvg: string, payload: string, runWidth: number): string {
  const layerSize = runeLayerSize(payload);
  const x = 8 + Math.max(0, (runWidth - layerSize.width) / 2); // svg-util pad = 8
  const y = 8 + Math.round(layerSize.height * 0.62);
  const layer = stampRuneLayerPayload(
    embedRuneLayer(hostSvg, payload, { x, y, opacity: 0.12 }),
    payload,
  );
  const close = hostSvg.lastIndexOf('</svg>');
  if (close < 0) return hostSvg;
  return hostSvg.slice(0, close) + layer + hostSvg.slice(close);
}

/** A hairline rule. */
function rule(color: string, marginTop = 0, marginBottom = 0): VNode {
  return el('div', {
    display: 'flex',
    height: 2,
    backgroundColor: color,
    marginTop,
    marginBottom,
  });
}

/** Eyebrow row: kicker on the left, cipher-kind tag on the right. */
function eyebrowRow(text: string, tag: string, color: string): VNode {
  return el(
    'div',
    {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      letterSpacing: 3,
      fontSize: 22,
      textTransform: 'uppercase',
      color,
    },
    [
      el('div', { display: 'flex' }, text),
      el('div', { display: 'flex', color: PALETTE.muted }, tag),
    ],
  );
}

/** Standard footer row: wordmark on the left, note on the right. */
function footerRow(note: string): VNode {
  return el(
    'div',
    {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      fontSize: 20,
      color: PALETTE.muted,
      marginTop: 18,
    },
    [
      el(
        'div',
        { display: 'flex', letterSpacing: 4, textTransform: 'uppercase' },
        'The Observance',
      ),
      el('div', { display: 'flex' }, note),
    ],
  );
}

/** A cipher-kind tag string for the eyebrow corner. */
function cipherTag(forged: ForgedClue): string {
  return forged.meta.cipher.toUpperCase();
}

// ---------------------------------------------------------------------------
// 1) parchmentCard — the clean default field note.
// ---------------------------------------------------------------------------

export function parchmentCard(spec: ClueRenderSpec, forged: ForgedClue): VNode {
  const eyebrow = spec.eyebrow ?? 'Field Note';
  const title = spec.title ?? 'A mark in the stone';
  const lines = spec.lines ?? ['What follows was carved, not written.'];
  return el(
    'div',
    rootStyle(PALETTE.field, 64),
    [
      eyebrowRow(eyebrow, cipherTag(forged), PALETTE.gilt),
      rule(PALETTE.ash, 18, 30),
      el(
        'div',
        { display: 'flex', fontSize: 46, lineHeight: 1.15, marginBottom: 22, color: PALETTE.ink },
        title,
      ),
      // prose lines
      el(
        'div',
        { display: 'flex', flexDirection: 'column', fontSize: 26, lineHeight: 1.4, color: PALETTE.muted },
        lines.map((l) => el('div', { display: 'flex', marginBottom: 6 }, l)),
      ),
      // the carved runes, centered in a recessed panel
      el(
        'div',
        {
          display: 'flex',
          flexGrow: 1,
          alignItems: 'center',
          justifyContent: 'center',
          marginTop: 26,
          marginBottom: 18,
          padding: 24,
          backgroundColor: PALETTE.panel,
          border: `2px solid ${PALETTE.ash}`,
          borderRadius: 6,
        },
        [runeImg(forged, PALETTE.ink, { maxWidth: CANVAS.clueWidth - 64 * 2 - 48, guide: PALETTE.ash, stego: stegoPayload(spec) })],
      ),
      footerRow(spec.footer ?? 'observed and recorded'),
      sigilWatermark(),
    ],
  );
}

// ---------------------------------------------------------------------------
// 2) redactedDossier — Archivist style, rust redaction bars.
// ---------------------------------------------------------------------------

/**
 * Parse a dossier line into typed runs. Text in [[double brackets]] becomes a
 * rust redaction bar sized to the hidden text's length; everything else renders
 * as mono "typed" text.
 */
interface Run {
  text: string;
  redacted: boolean;
}
function parseDossierLine(line: string): Run[] {
  const runs: Run[] = [];
  const re = /\[\[(.+?)\]\]/g;
  let last = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(line)) !== null) {
    if (m.index > last) runs.push({ text: line.slice(last, m.index), redacted: false });
    runs.push({ text: m[1] ?? '', redacted: true });
    last = re.lastIndex;
  }
  if (last < line.length) runs.push({ text: line.slice(last), redacted: false });
  if (runs.length === 0) runs.push({ text: line, redacted: false });
  return runs;
}

/** A redaction bar whose width tracks the hidden glyph count. */
function redactionBar(hiddenLen: number): VNode {
  const width = Math.max(28, Math.min(420, hiddenLen * 15 + 12));
  return el('div', {
    display: 'flex',
    width,
    height: 26,
    backgroundColor: PALETTE.rust,
    marginLeft: 4,
    marginRight: 4,
    borderRadius: 2,
  });
}

function dossierLineNode(line: string): VNode {
  const runs = parseDossierLine(line);
  return el(
    'div',
    {
      display: 'flex',
      alignItems: 'center',
      flexWrap: 'wrap',
      fontFamily: FONT.mono,
      fontSize: 24,
      lineHeight: 1.5,
      color: PALETTE.ink,
      marginBottom: 8,
    },
    runs.map((r) =>
      r.redacted
        ? redactionBar(r.text.length)
        : el('div', { display: 'flex' }, r.text),
    ),
  );
}

export function redactedDossier(spec: ClueRenderSpec, forged: ForgedClue): VNode {
  const eyebrow = spec.eyebrow ?? 'Archive · Restricted';
  const title = spec.title ?? 'Recovered Dossier';
  const dossier = spec.dossier ?? [
    'SUBJECT: [[the keeper]] — last seen at the [[obsidian gate]].',
    'DISPOSITION: marks below carry the [[location]]. Decrypt before reading aloud.',
  ];
  return el(
    'div',
    rootStyle(PALETTE.field, 56),
    [
      // header band the Archivist would stamp
      el(
        'div',
        {
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingBottom: 10,
        },
        [
          el(
            'div',
            { display: 'flex', flexDirection: 'column' },
            [
              el(
                'div',
                {
                  display: 'flex',
                  letterSpacing: 4,
                  fontSize: 20,
                  textTransform: 'uppercase',
                  color: PALETTE.rust,
                },
                eyebrow,
              ),
              el('div', { display: 'flex', fontSize: 40, color: PALETTE.ink, marginTop: 6 }, title),
            ],
          ),
          // a rust "REDACTED" stamp, rotated
          el(
            'div',
            {
              display: 'flex',
              transform: 'rotate(-8deg)',
              border: `3px solid ${PALETTE.rust}`,
              color: PALETTE.rust,
              fontFamily: FONT.mono,
              fontSize: 26,
              letterSpacing: 4,
              padding: '6px 16px',
              borderRadius: 4,
              opacity: 0.9,
            },
            'REDACTED',
          ),
        ],
      ),
      rule(PALETTE.rust, 6, 22),
      // typed dossier lines with redaction bars
      el(
        'div',
        { display: 'flex', flexDirection: 'column' },
        dossier.map((l) => dossierLineNode(l)),
      ),
      // the cipher payload, framed like an evidence strip
      el(
        'div',
        {
          display: 'flex',
          flexGrow: 1,
          alignItems: 'center',
          justifyContent: 'center',
          marginTop: 20,
          marginBottom: 10,
          padding: '18px 24px',
          backgroundColor: PALETTE.panel,
          border: `2px dashed ${PALETTE.ash}`,
        },
        [runeImg(forged, PALETTE.ink, { maxWidth: CANVAS.clueWidth - 56 * 2 - 48, stego: stegoPayload(spec) })],
      ),
      footerRow(spec.footer ?? `file ${forged.puzzleKey}`),
      sigilWatermark(120, 0.08),
    ],
  );
}

// ---------------------------------------------------------------------------
// 3) mapFragment — torn surveyor's chart corner.
// ---------------------------------------------------------------------------

/** A torn-paper frame drawn as an <img> SVG (irregular polygon + grid). */
function tornChartSvg(w: number, h: number): string {
  // deterministic jagged edge along the right + bottom
  const jag = (n: number, base: number, amp: number): number => {
    // simple deterministic pseudo-noise from index (no randomness, no time)
    const t = Math.sin(n * 12.9898) * 43758.5453;
    const f = t - Math.floor(t); // 0..1
    return Math.round(base + (f - 0.5) * 2 * amp);
  };
  const pts: string[] = [];
  pts.push('0,0', `${w - 18},0`);
  // right torn edge
  for (let y = 0; y <= h; y += 22) {
    pts.push(`${jag(y, w - 14, 12)},${y}`);
  }
  // bottom torn edge
  for (let x = w; x >= 0; x -= 22) {
    pts.push(`${x},${jag(x, h - 14, 12)}`);
  }
  pts.push('0,' + (h - 6));
  const poly = pts.join(' ');
  const gridLines: string[] = [];
  for (let x = 40; x < w - 20; x += 56) {
    gridLines.push(
      `<line x1="${x}" y1="8" x2="${x}" y2="${h - 20}" stroke="${PALETTE.ash}" stroke-width="1" opacity="0.35"/>`,
    );
  }
  for (let y = 40; y < h - 20; y += 56) {
    gridLines.push(
      `<line x1="8" y1="${y}" x2="${w - 20}" y2="${y}" stroke="${PALETTE.ash}" stroke-width="1" opacity="0.35"/>`,
    );
  }
  return [
    `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">`,
    `<polygon points="${poly}" fill="${PALETTE.panel}" stroke="${PALETTE.muted}" stroke-width="2"/>`,
    `<g>${gridLines.join('')}</g>`,
    // a compass rose in the corner
    `<g transform="translate(${w - 96} ${h - 96})" stroke="${PALETTE.soul}" stroke-width="2" fill="none" opacity="0.7">`,
    `<circle cx="0" cy="0" r="34"/>`,
    `<line x1="0" y1="-40" x2="0" y2="40"/><line x1="-40" y1="0" x2="40" y2="0"/>`,
    `<polygon points="0,-40 6,0 -6,0" fill="${PALETTE.soul}" stroke="none"/>`,
    `</g>`,
    `</svg>`,
  ].join('');
}

export function mapFragment(spec: ClueRenderSpec, forged: ForgedClue): VNode {
  const eyebrow = spec.eyebrow ?? 'Surveyor’s Fragment';
  const title = spec.title ?? 'Charted Ground';
  const place = spec.place ?? 'an unmarked place';
  const W = CANVAS.clueWidth;
  const H = CANVAS.clueHeight;
  return el(
    'div',
    {
      display: 'flex',
      width: W,
      height: H,
      backgroundColor: PALETTE.field,
      fontFamily: FONT.body,
      position: 'relative',
    },
    [
      // the torn chart fills the canvas as a backdrop image
      imgNode(svgDataUri(tornChartSvg(W, H)), W, H, { position: 'absolute', left: 0, top: 0 }),
      // content overlaid on the chart
      el(
        'div',
        {
          display: 'flex',
          flexDirection: 'column',
          width: W,
          height: H,
          padding: 64,
          position: 'relative',
        },
        [
          eyebrowRow(eyebrow, cipherTag(forged), PALETTE.soul),
          rule(PALETTE.muted, 16, 26),
          el('div', { display: 'flex', fontSize: 44, color: PALETTE.ink, marginBottom: 6 }, title),
          el(
            'div',
            { display: 'flex', fontSize: 24, color: PALETTE.muted, marginBottom: 8 },
            spec.lines?.[0] ?? 'The marks below number a place. Sign, then count.',
          ),
          // runes pinned mid-chart
          el(
            'div',
            { display: 'flex', flexGrow: 1, alignItems: 'center', justifyContent: 'center' },
            [runeImg(forged, PALETTE.ink, { maxWidth: W - 64 * 2 - 120, guide: PALETTE.soul, stego: stegoPayload(spec) })],
          ),
          // a place pin
          el(
            'div',
            { display: 'flex', alignItems: 'center', marginBottom: 6 },
            [
              el('div', {
                display: 'flex',
                width: 14,
                height: 14,
                borderRadius: 14,
                backgroundColor: PALETTE.soul,
                marginRight: 12,
              }),
              el(
                'div',
                { display: 'flex', fontFamily: FONT.mono, fontSize: 24, color: PALETTE.ink },
                place,
              ),
            ],
          ),
          footerRow(spec.footer ?? 'true to the keepers’ measure'),
        ],
      ),
      sigilWatermark(120, 0.12),
    ],
  );
}

// ---------------------------------------------------------------------------
// 4) runeCipherCard — cipher worksheet, runes foregrounded.
// ---------------------------------------------------------------------------

/** A faint dotted glyph grid behind the runes, as an <img> SVG. */
function glyphGridSvg(w: number, h: number): string {
  const cells: string[] = [];
  for (let x = 0; x <= w; x += 34) {
    for (let y = 0; y <= h; y += 34) {
      cells.push(`<circle cx="${x}" cy="${y}" r="1.2" fill="${PALETTE.ash}" opacity="0.5"/>`);
    }
  }
  return [
    `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">`,
    cells.join(''),
    `</svg>`,
  ].join('');
}

export function runeCipherCard(spec: ClueRenderSpec, forged: ForgedClue): VNode {
  const eyebrow = spec.eyebrow ?? 'Cipherwork';
  const title = spec.title ?? 'The Keepers’ Script';
  const keyHint = spec.keyHint ?? forged.meta.keyHint;
  const W = CANVAS.clueWidth;
  const gridH = 230;
  return el(
    'div',
    rootStyle(PALETTE.field, 64),
    [
      eyebrowRow(eyebrow, cipherTag(forged), PALETTE.soul),
      rule(PALETTE.ash, 16, 22),
      el('div', { display: 'flex', fontSize: 44, color: PALETTE.ink, marginBottom: 16 }, title),
      // foreground runes over a dotted grid
      el(
        'div',
        {
          display: 'flex',
          position: 'relative',
          height: gridH,
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: PALETTE.panel,
          border: `2px solid ${PALETTE.ash}`,
          borderRadius: 6,
          overflow: 'hidden',
        },
        [
          imgNode(svgDataUri(glyphGridSvg(W - 64 * 2, gridH)), W - 64 * 2, gridH, {
            position: 'absolute',
            left: 0,
            top: 0,
          }),
          runeImg(forged, PALETTE.soul, { maxWidth: W - 64 * 2 - 64, guide: PALETTE.ash, stego: stegoPayload(spec) }),
        ],
      ),
      // key-hand panel
      el(
        'div',
        {
          display: 'flex',
          flexDirection: 'column',
          flexGrow: 1,
          justifyContent: 'center',
          marginTop: 22,
        },
        [
          el(
            'div',
            {
              display: 'flex',
              letterSpacing: 3,
              fontSize: 18,
              textTransform: 'uppercase',
              color: PALETTE.gilt,
              marginBottom: 8,
            },
            'The hand that fits',
          ),
          el(
            'div',
            { display: 'flex', fontFamily: FONT.mono, fontSize: 28, color: PALETTE.ink, lineHeight: 1.35 },
            keyHint,
          ),
        ],
      ),
      footerRow(spec.footer ?? `${forged.meta.layout.glyphCount} marks · ${cipherTag(forged)}`),
      sigilWatermark(),
    ],
  );
}

// ---------------------------------------------------------------------------
// 5) journalPage — ruled keeper's diary leaf.
// ---------------------------------------------------------------------------

/** Ruled-paper lines + a red margin rule, as an <img> SVG backdrop. */
function ruledPageSvg(w: number, h: number): string {
  const lines: string[] = [];
  for (let y = 150; y < h - 60; y += 46) {
    lines.push(
      `<line x1="120" y1="${y}" x2="${w - 60}" y2="${y}" stroke="${PALETTE.ash}" stroke-width="1" opacity="0.45"/>`,
    );
  }
  return [
    `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">`,
    `<rect x="0" y="0" width="${w}" height="${h}" fill="${PALETTE.panel}"/>`,
    lines.join(''),
    // margin rule
    `<line x1="104" y1="40" x2="104" y2="${h - 40}" stroke="${PALETTE.rust}" stroke-width="2" opacity="0.55"/>`,
    `</svg>`,
  ].join('');
}

export function journalPage(spec: ClueRenderSpec, forged: ForgedClue): VNode {
  const eyebrow = spec.eyebrow ?? 'From the Keeper’s Diary';
  const title = spec.title ?? 'An entry, half-burned';
  const lines = spec.lines ?? [
    'I write what I dare not say aloud.',
    'They watch the doors, not the page.',
  ];
  const W = CANVAS.clueWidth;
  const H = CANVAS.clueHeight;
  return el(
    'div',
    {
      display: 'flex',
      width: W,
      height: H,
      backgroundColor: PALETTE.field,
      fontFamily: FONT.body,
      position: 'relative',
    },
    [
      imgNode(svgDataUri(ruledPageSvg(W, H)), W, H, { position: 'absolute', left: 0, top: 0 }),
      el(
        'div',
        {
          display: 'flex',
          flexDirection: 'column',
          width: W,
          height: H,
          // content sits to the right of the margin rule
          padding: '48px 60px 40px 140px',
          position: 'relative',
        },
        [
          el(
            'div',
            {
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'baseline',
              letterSpacing: 2,
              fontSize: 20,
              color: PALETTE.gilt,
              textTransform: 'uppercase',
            },
            [
              el('div', { display: 'flex' }, eyebrow),
              el('div', { display: 'flex', color: PALETTE.muted }, cipherTag(forged)),
            ],
          ),
          el(
            'div',
            { display: 'flex', fontSize: 40, color: PALETTE.ink, marginTop: 10, marginBottom: 14 },
            title,
          ),
          el(
            'div',
            { display: 'flex', flexDirection: 'column', fontSize: 26, color: PALETTE.muted, lineHeight: 1.7 },
            lines.map((l) => el('div', { display: 'flex' }, l)),
          ),
          // runes, as though pressed into the page mid-entry
          el(
            'div',
            { display: 'flex', flexGrow: 1, alignItems: 'center', justifyContent: 'flex-start', marginTop: 8 },
            [runeImg(forged, PALETTE.ink, { maxWidth: W - 140 - 60, guide: PALETTE.ash, stego: stegoPayload(spec) })],
          ),
          el(
            'div',
            { display: 'flex', justifyContent: 'flex-end', fontSize: 20, color: PALETTE.muted, marginTop: 8 },
            spec.footer ?? '— a keeper, name struck out',
          ),
        ],
      ),
      sigilWatermark(116, 0.1),
    ],
  );
}

// ---------------------------------------------------------------------------
// Shared root style.
// ---------------------------------------------------------------------------

function rootStyle(bg: string, pad: number): Record<string, unknown> {
  return {
    display: 'flex',
    flexDirection: 'column',
    width: CANVAS.clueWidth,
    height: CANVAS.clueHeight,
    backgroundColor: bg,
    color: PALETTE.ink,
    fontFamily: FONT.body,
    padding: pad,
    position: 'relative',
  };
}

// ---------------------------------------------------------------------------
// Template registry + dispatcher.
// ---------------------------------------------------------------------------

type TemplateFn = (spec: ClueRenderSpec, forged: ForgedClue) => VNode;

export const TEMPLATES: Readonly<Record<TemplateKind, TemplateFn>> = {
  parchmentCard,
  redactedDossier,
  mapFragment,
  runeCipherCard,
  journalPage,
};

/** All template kinds, in canonical order (handy for tooling / dashboards). */
export const TEMPLATE_KINDS: readonly TemplateKind[] = [
  'parchmentCard',
  'redactedDossier',
  'mapFragment',
  'runeCipherCard',
  'journalPage',
];

/**
 * Deterministically pick a template when the spec doesn't name one. Coord clues
 * default to the map; book clues to the dossier; the rest spread across the
 * remaining themes by a stable hash of the puzzleKey so a given clue always
 * renders the same artifact.
 */
export function pickTemplate(forged: ForgedClue): TemplateKind {
  switch (forged.meta.cipher) {
    case 'coord':
      return 'mapFragment';
    case 'book':
      return 'redactedDossier';
    default: {
      // stable spread over the remaining three "letter-cipher" themes
      const pool: TemplateKind[] = ['runeCipherCard', 'parchmentCard', 'journalPage'];
      let h = 0;
      for (const c of forged.puzzleKey) h = (h * 31 + c.charCodeAt(0)) >>> 0;
      return pool[h % pool.length] as TemplateKind;
    }
  }
}

/**
 * renderClue(spec) — forge the clue, pick (or honour) a template, and render to
 * a PNG Buffer. This is the single entry point the Discord/bot layer calls.
 *
 * @returns a PNG Buffer (1200x630), ready to attach to a Discord message or
 *          write to disk.
 */
export async function renderClue(spec: ClueRenderSpec): Promise<Buffer> {
  const forged = forgeClue(spec.clue);
  const kind = spec.template ?? pickTemplate(forged);
  const fn = TEMPLATES[kind];
  if (!fn) {
    throw new Error(`renderClue: unknown template ${JSON.stringify(kind)}`);
  }
  const node = fn(spec, forged);
  return renderPng(node, CANVAS.clueWidth, CANVAS.clueHeight);
}

/**
 * renderClueDetailed(spec) — like renderClue but also returns the forged clue
 * (solution, puzzleKey, meta) and which template was used. Handy for the author
 * dashboard / ledger wiring.
 */
export async function renderClueDetailed(
  spec: ClueRenderSpec,
): Promise<{ png: Buffer; template: TemplateKind; forged: ForgedClue }> {
  const forged = forgeClue(spec.clue);
  const template = spec.template ?? pickTemplate(forged);
  const fn = TEMPLATES[template];
  if (!fn) {
    throw new Error(`renderClueDetailed: unknown template ${JSON.stringify(template)}`);
  }
  const png = await renderPng(fn(spec, forged), CANVAS.clueWidth, CANVAS.clueHeight);
  return { png, template, forged };
}

// Re-export the SVG helpers so callers can build bespoke embeds from one place.
export { xmlEscape, svgDataUri, runeBlockSvg, runeBlockSize } from './svg-util.js';
export { getSigilSvg, sigilIsReal } from './sigil.js';
