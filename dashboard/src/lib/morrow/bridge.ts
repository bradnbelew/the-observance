import { bridgeSignature } from "./bridge-sign";
import { stopFingerprint, stopSecret } from "./signing";

/**
 * The bridge to the mrrw plugin (MORROW_BRIDGE_URL, e.g. http://mc.example.net:25588).
 *
 * The plugin is the campaign's clock: it advances acts by itself when the desk tells it what was
 * filed, and it knows how many residents have joined and how the Act 10 USB vote stands. The site
 * asks it for that state on each request (cached in memory for MORROW_BRIDGE_TTL_MS, 25 s by
 * default) and tells it about desk tickets that move the story.
 *
 * Nothing here ever throws to a page: an unset URL, a timeout (2.5 s), a refused signature or a
 * malformed answer all mean "no bridge", and the site falls back to its environment (MORROW_ACT,
 * MORROW_ACT_STARTED, MORROW_PLAYERS), exactly as it behaved before the bridge existed.
 *
 * Every request is signed with MORROW_STOP_SECRET (see bridge-sign.ts for the exact format).
 */

export type UsbChoice = "clean" | "recovered" | "corpus";
export const USB_KEYS: readonly UsbChoice[] = ["clean", "recovered", "corpus"];

export type BridgeState = {
  act: number;
  actStarted: Date | null;
  players: number;
  /** The plugin's roster (every player who joined); the site's player list when MORROW_PLAYERS is unset. */
  names: string[];
  /** Where each player first logged in (block coordinates): the Act 9 profiles' Location. */
  firstJoins: Record<string, [number, number, number]>;
  stoppedAt: Date | null;
  ready: boolean;
  stopped: boolean;
  usb: { votes: Record<UsbChoice, number>; winner: UsbChoice | null; open: boolean };
  fingerprint: string | null;
  theo: { id: string; at: string; subject: string }[];
  /** How many of this act's hints the operator has released (/op page or /mrrw hint N); 0 = none. Hints never release by themselves. */
  hintFloor: number;
};

export type DeskEvent = {
  kind: "desk";
  ref: string;
  reply_at: string | null;
  voice: string | null;
  subject: string | null;
};

const TIMEOUT_MS = 2500;

export function bridgeUrl(): string {
  return (process.env.MORROW_BRIDGE_URL ?? "").trim().replace(/\/+$/, "");
}

function ttlMs(): number {
  const n = Number(process.env.MORROW_BRIDGE_TTL_MS);
  return Number.isFinite(n) && n >= 0 ? n : 25_000;
}

const warned = new Set<string>();
function warnOnce(key: string, message: string) {
  if (warned.has(key)) return;
  warned.add(key);
  console.warn(`MORROW bridge: ${message}`);
}

/** One signed request. Resolves to the parsed JSON, or null for any failure (never rejects). */
async function call(method: "GET" | "POST", pathWithQuery: string, body = ""): Promise<unknown | null> {
  const base = bridgeUrl();
  if (!base) return null;
  try {
    const url = new URL(base + pathWithQuery);
    const target = url.pathname + url.search;
    const ts = String(Math.floor(Date.now() / 1000));
    const headers: Record<string, string> = {
      "X-Mrrw-Ts": ts,
      "X-Mrrw-Sig": bridgeSignature(stopSecret(), method, target, ts, body),
      Accept: "application/json",
    };
    if (method === "POST") headers["Content-Type"] = "application/json";
    const res = await fetch(url, {
      method, headers, body: method === "POST" ? body : undefined,
      signal: AbortSignal.timeout(TIMEOUT_MS), cache: "no-store",
    });
    if (res.status === 401 || res.status === 403) {
      warnOnce("auth", `${method} ${url.pathname} refused (${res.status}): MORROW_STOP_SECRET on the site must equal stop-secret in plugins/mrrw/config.yml, and both clocks must be roughly right`);
      return null;
    }
    if (!res.ok) {
      warnOnce(`http-${res.status}`, `${method} ${url.pathname} answered ${res.status}`);
      return null;
    }
    warned.delete("down");
    return await res.json();
  } catch (err) {
    warnOnce("down", `unreachable at ${base} (${(err as Error).message}); using MORROW_ACT / MORROW_ACT_STARTED / MORROW_PLAYERS`);
    return null;
  }
}

const isUsb = (v: unknown): v is UsbChoice => typeof v === "string" && (USB_KEYS as readonly string[]).includes(v);

/** Validates the plugin's /state answer. Anything unexpected means "no bridge". */
export function parseState(raw: unknown): BridgeState | null {
  if (!raw || typeof raw !== "object") return null;
  const o = raw as Record<string, unknown>;
  const act = Number(o.act);
  if (!Number.isInteger(act) || act < 1 || act > 10) return null;
  const startedRaw = typeof o.act_started === "string" ? new Date(o.act_started) : null;
  const usbRaw = (o.usb && typeof o.usb === "object" ? o.usb : {}) as Record<string, unknown>;
  const votesRaw = (usbRaw.votes && typeof usbRaw.votes === "object" ? usbRaw.votes : {}) as Record<string, unknown>;
  const count = (v: unknown) => (Number.isFinite(Number(v)) && Number(v) > 0 ? Math.floor(Number(v)) : 0);
  const theo = Array.isArray(o.theo) ? o.theo : [];
  const names = Array.isArray(o.names) ? o.names.filter((n): n is string => typeof n === "string" && n.length > 0 && n.length <= 16) : [];
  const firstJoins: Record<string, [number, number, number]> = {};
  if (o.first_joins && typeof o.first_joins === "object") {
    for (const [n, v] of Object.entries(o.first_joins as Record<string, unknown>)) {
      if (Array.isArray(v) && v.length === 3 && v.every((c) => Number.isFinite(Number(c)))) firstJoins[n] = [Number(v[0]), Number(v[1]), Number(v[2])];
    }
  }
  const stoppedRaw = typeof o.stopped_at === "string" ? new Date(o.stopped_at) : null;
  return {
    act,
    actStarted: startedRaw && !Number.isNaN(startedRaw.getTime()) ? startedRaw : null,
    players: count(o.players),
    names,
    firstJoins,
    stoppedAt: stoppedRaw && !Number.isNaN(stoppedRaw.getTime()) ? stoppedRaw : null,
    hintFloor: count(o.hint_floor),
    ready: o.ready === true,
    stopped: o.stopped === true,
    usb: {
      votes: { clean: count(votesRaw.clean), recovered: count(votesRaw.recovered), corpus: count(votesRaw.corpus) },
      winner: isUsb(usbRaw.winner) ? usbRaw.winner : null,
      open: usbRaw.open === true,
    },
    fingerprint: typeof o.fingerprint === "string" ? o.fingerprint : null,
    theo: theo
      .filter((t): t is Record<string, unknown> => !!t && typeof t === "object")
      .map((t) => ({ id: String(t.id ?? ""), at: String(t.at ?? ""), subject: String(t.subject ?? "") }))
      .filter((t) => t.id),
  };
}

let cached: { at: number; state: BridgeState | null } | null = null;
let inflight: Promise<BridgeState | null> | null = null;

/** The plugin's state, or null when there is no bridge (unset, down, refused, malformed). Never throws. */
export async function bridgeState(): Promise<BridgeState | null> {
  if (!bridgeUrl()) return null;
  // a missing bridge is remembered for at least 3 s even with a zero TTL (a refused connect can take
  // seconds on Windows, and every page asks several times)
  const ttl = cached?.state ? ttlMs() : Math.max(ttlMs(), 3000);
  // a blip (restart, lag spike) must not drop the site back to the env act: serve the last good state
  if (cached && Date.now() - cached.at < ttl) return cached.state ?? lastGood;
  if (!inflight) {
    inflight = (async () => {
      const state = parseState(await call("GET", "/state"));
      if (state?.fingerprint) {
        try {
          const mine = stopFingerprint();
          if (state.fingerprint !== mine) {
            warnOnce("fingerprint", `plugin stop-secret fingerprint ${state.fingerprint} != site ${mine}: the finale code will be refused`);
          }
        } catch {
          /* secret problems are logged by instrumentation.ts */
        }
      }
      cached = { at: Date.now(), state };
      if (state) {
        lastGood = state;
        lastNames = state.names;
        lastFirstJoins = state.firstJoins;
      }
      return state ?? lastGood;
    })().finally(() => {
      inflight = null;
    });
  }
  return inflight;
}

/** Forget the cached state (after posting an event that may have advanced the act). */
export function forgetBridgeState() {
  cached = null;
}

let lastGood: BridgeState | null = null;
let lastNames: string[] = [];
let lastFirstJoins: Record<string, [number, number, number]> = {};

/** The roster from the most recent successful /state (kept past the cache TTL, so a blip never empties Act 9). */
export function lastBridgeNames(): string[] {
  return lastNames;
}

export function lastBridgeFirstJoins(): Record<string, [number, number, number]> {
  return lastFirstJoins;
}

/** A hand-in that isn't a ticket (June's folder opened, Iona's key in the console): the same signed event, no reply time. */
export async function postHandIn(ref: string, subject: string | null = null): Promise<boolean> {
  return postDeskEvent({ ref, reply_at: null, voice: null, subject });
}

/** Tell the plugin what the desk just filed. Resolves when sent (or failed); never rejects. Idempotent on the plugin side. */
export async function postDeskEvent(ev: Omit<DeskEvent, "kind">): Promise<boolean> {
  if (!bridgeUrl()) return false;
  const body = JSON.stringify({ kind: "desk", ref: ev.ref, reply_at: ev.reply_at, voice: ev.voice, subject: ev.subject });
  // a hand-in is posted once from after(); the plugin dedups per ref, so retry a blip rather than lose the act
  let res: unknown = null;
  for (let attempt = 0; attempt < 3 && !res; attempt++) {
    if (attempt) await new Promise((r) => setTimeout(r, 2000 * attempt));
    res = await call("POST", "/event", body);
  }
  forgetBridgeState();
  const ok = !!res && typeof res === "object" && (res as { ok?: unknown }).ok === true;
  console.log(`MORROW_BRIDGE event ref=${ev.ref} ${ok ? `ok act=${(res as { act?: unknown }).act ?? "?"}` : "not delivered"}`);
  return ok;
}

/** The act the site serves: the env act, or the plugin's act if the plugin has moved further on. */
export function effectiveAct(envAct: number, state: BridgeState | null): number {
  return state ? Math.max(envAct, state.act) : envAct;
}

/** The operator page: show this act's first n hints on the desk. Resolves to the plugin's new count, or null if not delivered. */
export async function postHint(n: number): Promise<number | null> {
  const res = await call("POST", "/event", JSON.stringify({ kind: "hint", n }));
  forgetBridgeState();
  const floor = res && typeof res === "object" ? (res as { hint_floor?: unknown }).hint_floor : undefined;
  return typeof floor === "number" ? floor : null;
}
