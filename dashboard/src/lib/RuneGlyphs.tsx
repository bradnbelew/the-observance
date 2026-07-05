import { glyphLines, runesWidth, GLYPH_W, GLYPH_H, GLYPH_GAP } from "./runes";

/**
 * RuneGlyphs — renders text carved in THE KEEPERS' ALPHABET (the game's real, invented rune
 * substitution script; see dashboard/src/lib/runes.ts + discord/src/forge/runes.ts), as inline SVG.
 *
 * Replaces the fake Unicode Elder Futhark (`ᛟ ᚲ ᛖ ᛈ ᛏ`) previously hard-coded on the record pages —
 * that string used a real-world rune alphabet that has nothing to do with this game's own cipher, so
 * a player who had actually learned the in-game glyphs from clues could never read it. This component
 * carves whatever text is passed in using the SAME bijective letter->glyph table baked into the
 * Minecraft resource pack font and the Discord clue cards, so the mark is decodable and consistent
 * with the rest of the ARG's glyph logic.
 *
 * Deliberately plain/unstyled strokes (no custom web font) — see runes.ts header comment for why a
 * full font-embedding pass was skipped for this pass.
 */
export function RuneGlyphs({
  text,
  className,
  height = 28,
}: {
  text: string;
  className?: string;
  height?: number;
}) {
  const advance = GLYPH_W + GLYPH_GAP;
  const width = runesWidth(text, advance);
  const scale = height / GLYPH_H;
  return (
    <svg
      aria-hidden
      className={className}
      width={width * scale}
      height={height}
      viewBox={`0 0 ${width} ${GLYPH_H}`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {[...text].map((ch, i) => {
        const lines = glyphLines(ch);
        const x = i * advance;
        return (
          <g key={`${ch}-${i}`} transform={`translate(${x} 0)`}>
            {lines.map((l, j) => (
              <line
                key={j}
                x1={l.x1}
                y1={l.y1}
                x2={l.x2}
                y2={l.y2}
                stroke="currentColor"
                strokeWidth={3}
                strokeLinecap="round"
              />
            ))}
          </g>
        );
      })}
    </svg>
  );
}
