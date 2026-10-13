import net from "node:net";
import { createHmac } from "node:crypto";
import { stopSecret } from "./signing";

/**
 * Minecraft Server List Ping, from the website to the Mossfield server (MORROW_MC_HOST).
 * Morrow broadcasts its state as hex in the MOTD's colour codes (C14). The console and the
 * status page read that channel instead of sharing a database with the server.
 */
const COLOURS = ["black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
  "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"];

function varint(n: number): Buffer {
  const out: number[] = [];
  do {
    let b = n & 0x7f;
    n >>>= 7;
    if (n) b |= 0x80;
    out.push(b);
  } while (n);
  return Buffer.from(out);
}

export async function ping(hostPort = process.env.MORROW_MC_HOST ?? "", timeoutMs = 4000): Promise<unknown | null> {
  if (!hostPort) return null;
  const [host, portStr] = hostPort.split(":");
  const port = Number(portStr || 25565);
  return new Promise((resolve) => {
    const sock = net.connect({ host, port });
    let buf = Buffer.alloc(0);
    const done = (v: unknown | null) => { sock.destroy(); resolve(v); };
    sock.setTimeout(timeoutMs, () => done(null));
    sock.on("error", () => done(null));
    sock.on("connect", () => {
      const hostBuf = Buffer.from(host, "utf8");
      const portBuf = Buffer.alloc(2);
      portBuf.writeUInt16BE(port);
      const hs = Buffer.concat([varint(0), varint(774), varint(hostBuf.length), hostBuf, portBuf, varint(1)]);
      sock.write(Buffer.concat([varint(hs.length), hs, varint(1), varint(0)]));
    });
    sock.on("data", (d) => {
      buf = Buffer.concat([buf, d]);
      try {
        let i = 0;
        const read = () => { let n = 0, s = 0, b: number; do { b = buf[i++]; n |= (b & 0x7f) << s; s += 7; } while (b & 0x80); return n; };
        const total = read();
        if (buf.length < i + total) return;
        read(); // packet id
        const len = read();
        done(JSON.parse(buf.subarray(i, i + len).toString("utf8")));
      } catch {
        /* wait for more data */
      }
    });
  });
}

type Comp = { text?: string; color?: string; extra?: Comp[] } | string;

/** Decode the hex nibbles carried by the MOTD colours. Empty string = no band (silent). */
export function morrowStatus(status: unknown): string {
  const parts: [string | undefined, string][] = [];
  const walk = (c: Comp) => {
    if (typeof c === "string") return parts.push([undefined, c]);
    parts.push([c.color, c.text ?? ""]);
    (c.extra ?? []).forEach(walk);
  };
  const desc = (status as { description?: Comp } | null)?.description;
  if (!desc) return "";
  walk(desc);
  const nib = parts.filter(([c, t]) => c && t && COLOURS.includes(c)).map(([c]) => COLOURS.indexOf(c!));
  const bytes: number[] = [];
  for (let i = 0; i + 1 < nib.length; i += 2) bytes.push(nib[i] * 16 + nib[i + 1]);
  return Buffer.from(bytes).toString("ascii");
}

export const STOP_CHOICES = { clean: "C", recovered: "R", corpus: "W" } as const;

/** The final stop code. Must match plugin-v2 StopCode.java. Valid for the current and previous half hour. */
export function stopCode(choice: keyof typeof STOP_CHOICES, at = new Date()): string {
  const secret = stopSecret();
  const bucket = Math.floor(at.getTime() / 1_800_000);
  const mac = createHmac("sha256", secret).update(`stop|${STOP_CHOICES[choice]}|${bucket}`).digest("hex").toUpperCase();
  return `${STOP_CHOICES[choice]}-${mac.slice(0, 4)}-${mac.slice(4, 8)}`;
}
