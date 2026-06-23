/**
 * Card templates — satori node trees for The Observance.
 * Pure layout: they take data, return a VNode. No I/O here.
 */
import { brand } from '../brand.js';
import { el, type VNode } from './render.js';

const { colors, fonts, canvas } = brand;

/** Data for a single whisper / clue card. */
export interface ClueCardData {
  /** Small label above the headline, e.g. "WHISPER · ACT II". */
  eyebrow: string;
  /** Puzzle key, shown as a monospaced sigil, e.g. "obsidian-gate". */
  puzzleKey: string;
  /** Tier number (1 = vaguest, higher = clearer). */
  tier: number;
  /** The hint body. */
  body: string;
  /** Optional footer note, e.g. remaining-whisper count. */
  footer?: string;
}

/** A tiered whisper clue card (1200x630). */
export function clueCard(data: ClueCardData): VNode {
  return el(
    'div',
    {
      display: 'flex',
      flexDirection: 'column',
      width: canvas.clueWidth,
      height: canvas.clueHeight,
      backgroundColor: colors.ink,
      color: colors.parchment,
      fontFamily: fonts.body,
      padding: 64,
      position: 'relative',
    },
    [
      // Top hairline + eyebrow row
      el(
        'div',
        {
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          letterSpacing: 3,
          fontSize: 22,
          textTransform: 'uppercase',
          color: colors.accent,
        },
        [
          el('div', { display: 'flex' }, data.eyebrow),
          el('div', { display: 'flex', color: colors.muted }, `TIER ${data.tier}`),
        ],
      ),
      el('div', {
        display: 'flex',
        height: 2,
        backgroundColor: colors.line,
        marginTop: 20,
        marginBottom: 36,
      }),
      // Puzzle sigil
      el(
        'div',
        {
          display: 'flex',
          fontFamily: fonts.mono,
          fontSize: 26,
          color: colors.muted,
          marginBottom: 24,
        },
        data.puzzleKey,
      ),
      // Body
      el(
        'div',
        {
          display: 'flex',
          fontSize: 52,
          lineHeight: 1.25,
          flexGrow: 1,
        },
        data.body,
      ),
      // Footer
      el(
        'div',
        {
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginTop: 24,
          fontSize: 22,
          color: colors.muted,
        },
        [
          el('div', { display: 'flex', letterSpacing: 4, textTransform: 'uppercase' }, 'The Observance'),
          el('div', { display: 'flex' }, data.footer ?? ''),
        ],
      ),
    ],
  );
}

/** Data for a square brand card (1024x1024). */
export interface BrandCardData {
  title: string;
  subtitle: string;
}

/** A square brand / channel-art card. */
export function brandCard(data: BrandCardData): VNode {
  return el(
    'div',
    {
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      width: canvas.assetSize,
      height: canvas.assetSize,
      backgroundColor: colors.ink,
      color: colors.parchment,
      fontFamily: fonts.body,
      padding: 96,
    },
    [
      el('div', {
        display: 'flex',
        width: 96,
        height: 96,
        borderRadius: 96,
        border: `4px solid ${colors.accent}`,
        marginBottom: 48,
      }),
      el(
        'div',
        {
          display: 'flex',
          fontSize: 96,
          letterSpacing: 2,
          textAlign: 'center',
          textTransform: 'uppercase',
        },
        data.title,
      ),
      el('div', {
        display: 'flex',
        height: 2,
        width: 220,
        backgroundColor: colors.line,
        marginTop: 32,
        marginBottom: 32,
      }),
      el(
        'div',
        {
          display: 'flex',
          fontSize: 34,
          color: colors.muted,
          textAlign: 'center',
        },
        data.subtitle,
      ),
    ],
  );
}
