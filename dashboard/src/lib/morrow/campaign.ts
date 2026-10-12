import { cookies } from "next/headers";
import { createHmac, timingSafeEqual } from "node:crypto";

/**
 * Campaign settings, all from environment variables (set on Vercel per act):
 *   MORROW_ACT            1-10, the current act
 *   MORROW_ACT_STARTED    ISO time the act began (drives Theo's drops and hint timers)
 *   MORROW_PLAYERS        comma-separated Minecraft names of the players (Act 9 backdating)
 *   MORROW_MC_HOST        host:port of the Mossfield server (Act 10 readiness + heartbeat)
 *   MORROW_SESSION_SECRET signs sessions, tickets and file links
 *   MORROW_STOP_SECRET    shared with the mrrw plugin; signs the final stop code
 *   MORROW_DESK_FAST=1    Theo answers immediately (testing only)
 *   MORROW_DEV=1          local testing: an act switcher cookie overrides MORROW_ACT. Never set in production.
 */

export const DEV = process.env.MORROW_DEV === "1";
export const DEV_ACT_COOKIE = "morrow_dev_act";

export async function currentAct(): Promise<number> {
  if (DEV) {
    const v = Number((await cookies()).get(DEV_ACT_COOKIE)?.value);
    if (v >= 1 && v <= 10) return v;
  }
  const n = Number.parseInt(process.env.MORROW_ACT ?? "1", 10);
  return Number.isFinite(n) && n >= 1 && n <= 10 ? n : 1;
}

export function actStarted(): Date | null {
  const raw = process.env.MORROW_ACT_STARTED;
  if (!raw) return null;
  const d = new Date(raw);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function hoursIntoAct(now = new Date()): number {
  if (DEV || process.env.MORROW_DESK_FAST === "1") return 10_000;
  const s = actStarted();
  return s ? (now.getTime() - s.getTime()) / 3_600_000 : 0;
}

export function players(): string[] {
  return (process.env.MORROW_PLAYERS ?? "").split(",").map((s) => s.trim()).filter(Boolean);
}

export const deskFast = () => DEV || process.env.MORROW_DESK_FAST === "1";

const secret = () => process.env.MORROW_SESSION_SECRET || "copperline-mirror-dev-secret";

/** Signed, URL-safe JSON. Used for desk tickets, file links and voice links. */
export function seal(payload: object): string {
  const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
  const mac = createHmac("sha256", secret()).update(body).digest("base64url").slice(0, 22);
  return `${body}.${mac}`;
}

export function unseal<T>(token: string | null | undefined): T | null {
  if (!token) return null;
  const [body, mac] = token.split(".");
  if (!body || !mac) return null;
  const want = createHmac("sha256", secret()).update(body).digest("base64url").slice(0, 22);
  const a = Buffer.from(mac), b = Buffer.from(want);
  if (a.length !== b.length || !timingSafeEqual(a, b)) return null;
  try {
    return JSON.parse(Buffer.from(body, "base64url").toString("utf8")) as T;
  } catch {
    return null;
  }
}

/** The next "evening" in Chicago (18:00-22:59) at least an hour after `from`, with a little jitter. */
export function eveningAfter(from: Date, jitterSeed: number, minHours = 1): Date {
  const t = new Date(from.getTime() + minHours * 3_600_000);
  for (let i = 0; i < 72; i++) {
    const c = new Date(t.getTime() + i * 3_600_000);
    const hour = Number(new Intl.DateTimeFormat("en-US", { timeZone: "America/Chicago", hour: "numeric", hour12: false }).format(c));
    if (hour >= 18 && hour <= 22) return new Date(c.getTime() + (jitterSeed % 50) * 60_000);
  }
  return t;
}
