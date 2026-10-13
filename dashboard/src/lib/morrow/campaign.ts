import { cookies } from "next/headers";
import { createHmac, timingSafeEqual } from "node:crypto";
import { sessionSecret } from "./signing";
import { bridgeState, effectiveAct, hintFactor } from "./bridge";

/**
 * Campaign settings, all from environment variables (set on Vercel per act):
 *   MORROW_ACT            1-10, the current act
 *   MORROW_ACT_STARTED    ISO time the act began, WITH an offset (2026-10-17T19:00:00-05:00); drives Theo's drops and hint timers
 *   MORROW_PLAYERS        comma-separated Minecraft names of the players (Act 9 backdating)
 *   MORROW_MC_HOST        host:port of the Mossfield server (Act 10 readiness + heartbeat)
 *   MORROW_SESSION_SECRET signs sessions, tickets and file links
 *   MORROW_STOP_SECRET    shared with the mrrw plugin; signs the final stop code
 *   MORROW_DESK_FAST=1    Theo answers immediately (testing only)
 *   MORROW_DEV=1          local testing: an act switcher cookie overrides MORROW_ACT. Never set in production.
 *   MORROW_BRIDGE_URL     the mrrw plugin's bridge (bridge.ts). When it answers, the act is the later of
 *                         MORROW_ACT and the plugin's act, the plugin's act_started drives the timers, and
 *                         its resident count scales the hints. Unset or down: the env values above, as before.
 */

export const DEV = process.env.MORROW_DEV === "1";
export const DEV_ACT_COOKIE = "morrow_dev_act";

/** MORROW_ACT alone (1-10). */
export function envAct(): number {
  const n = Number.parseInt(process.env.MORROW_ACT ?? "1", 10);
  return Number.isFinite(n) && n >= 1 && n <= 10 ? n : 1;
}

/** The act this request is served at: the dev cookie (testing), else max(MORROW_ACT, the plugin's act). Request time. */
export async function currentAct(): Promise<number> {
  if (DEV) {
    const v = Number((await cookies()).get(DEV_ACT_COOKIE)?.value);
    if (v >= 1 && v <= 10) return v;
  }
  return effectiveAct(envAct(), await bridgeState());
}

/**
 * What is wrong with MORROW_ACT_STARTED, if anything. A value with no UTC offset is accepted
 * but read in the server's time zone, which is UTC on Vercel (so "2026-10-17T19:00" means
 * 14:00 in Chicago). Write it with an offset: 2026-10-17T19:00:00-05:00.
 */
export function actStartedProblem(): string | null {
  const raw = process.env.MORROW_ACT_STARTED?.trim();
  if (!raw) return "MORROW_ACT_STARTED is not set: Theo's timed drops and the hint timers will never fire this act.";
  if (Number.isNaN(new Date(raw).getTime())) return `MORROW_ACT_STARTED="${raw}" is not a valid ISO time: timed drops and hints will never fire.`;
  if (!/(?:Z|[+-]\d{2}:?\d{2})$/i.test(raw)) {
    return `MORROW_ACT_STARTED="${raw}" has no UTC offset, so it is read as server time (UTC on Vercel). Add one, e.g. ${raw.includes("T") ? raw : `${raw}T00:00:00`}-05:00.`;
  }
  return null;
}

let actStartedWarned = false;

export function actStarted(): Date | null {
  const problem = actStartedProblem();
  if (problem && !actStartedWarned) {
    actStartedWarned = true;
    console.warn(`MORROW warning: ${problem}`);
  }
  const raw = process.env.MORROW_ACT_STARTED?.trim();
  if (!raw) return null;
  const d = new Date(raw);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** Testing only (MORROW_DEV=1): this cookie set to "real" keeps the real act clock instead of releasing everything. */
export const DEV_CLOCK_COOKIE = "morrow_dev_clock";

/**
 * How far into the act we are, and how fast hints come. The start is the plugin's act_started when
 * the bridge answers and its act is the one being served; otherwise MORROW_ACT_STARTED. The hint
 * factor comes from the plugin's resident count, else from MORROW_PLAYERS (bridge.ts hintFactor).
 */
export async function actClock(now = new Date()): Promise<{ hours: number; hintFactor: number }> {
  const state = await bridgeState();
  const factor = hintFactor(state ? state.players : players().length);
  if (DEV || process.env.MORROW_DESK_FAST === "1") {
    const real = DEV && (await cookies()).get(DEV_CLOCK_COOKIE)?.value === "real";
    if (!real) return { hours: 10_000, hintFactor: factor };
  }
  const env = envAct();
  const fromBridge = state && state.act >= env && state.actStarted ? state.actStarted : null;
  const s = fromBridge ?? actStarted();
  return { hours: s ? (now.getTime() - s.getTime()) / 3_600_000 : 0, hintFactor: factor };
}

export function players(): string[] {
  return (process.env.MORROW_PLAYERS ?? "").split(",").map((s) => s.trim()).filter(Boolean);
}

export const deskFast = () => DEV || process.env.MORROW_DESK_FAST === "1";

const secret = sessionSecret;

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

/** The next six Saturdays (today included if it is one), by the Chicago calendar. Act 10 windows are Saturdays ("Saturday then"). */
export function upcomingSaturdays(now = new Date(), count = 6): { value: string; label: string }[] {
  const today = new Intl.DateTimeFormat("en-CA", { timeZone: "America/Chicago" }).format(now); // YYYY-MM-DD
  const [y, m, d] = today.split("-").map(Number);
  const start = new Date(Date.UTC(y, m - 1, d));
  start.setUTCDate(start.getUTCDate() + ((6 - start.getUTCDay() + 7) % 7));
  return Array.from({ length: count }, (_, i) => {
    const s = new Date(start.getTime() + i * 7 * 86_400_000);
    return {
      value: s.toISOString().slice(0, 10),
      label: s.toLocaleDateString("en-US", { timeZone: "UTC", weekday: "short", month: "short", day: "numeric", year: "numeric" }),
    };
  });
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
