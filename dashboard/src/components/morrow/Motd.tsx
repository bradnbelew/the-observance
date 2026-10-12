/**
 * Renders a Minecraft MOTD with § colour codes the way the multiplayer menu would.
 * The codes themselves carry Morrow's restore-queue status (C14); the visible result is
 * just a band of coloured blocks after the server name.
 */
const COLOURS: Record<string, string> = {
  "0": "#000000", "1": "#0000aa", "2": "#00aa00", "3": "#00aaaa", "4": "#aa0000", "5": "#aa00aa",
  "6": "#ffaa00", "7": "#aaaaaa", "8": "#555555", "9": "#5555ff", a: "#55ff55", b: "#55ffff",
  c: "#ff5555", d: "#ff55ff", e: "#ffff55", f: "#ffffff",
};

export function Motd({ raw }: { raw: string }) {
  const parts = [...raw.matchAll(/§([0-9a-f])([^§]*)/g)];
  return (
    <span className="mc-motd" title="Mossfield">
      <span className="mc-motd-name">Mossfield</span>{" "}
      {parts.map((m, i) => <span key={i} style={{ color: COLOURS[m[1]] }}>{m[2]}</span>)}
    </span>
  );
}
