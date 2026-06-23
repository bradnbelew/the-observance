/**
 * Brand tokens for The Observance — shared by every satori-rendered card.
 * Editorial, restrained: deep ink, parchment, a single signal accent.
 */
export const brand = {
  colors: {
    ink: '#0E1116', // near-black background
    panel: '#161B22', // raised card surface
    parchment: '#E8E2D4', // primary text
    muted: '#8B93A1', // secondary text
    accent: '#C8A24B', // gilt / signal
    danger: '#B4543A',
    line: '#2A313B', // hairline rules
  },
  fonts: {
    /** Logical family names; actual font data is loaded by the renderer. */
    body: 'Observance Serif',
    mono: 'Observance Mono',
  },
  canvas: {
    clueWidth: 1200,
    clueHeight: 630, // OG-card proportions
    assetSize: 1024,
  },
} as const;

export type Brand = typeof brand;
