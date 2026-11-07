import { NextRequest, NextResponse, after } from "next/server";
import folders from "@/lib/canon/folders.json";
import { bridgeState, effectiveAct, postHandIn } from "@/lib/morrow/bridge";
import { DEV } from "@/lib/morrow/signing";
import { apachePage, authRequired } from "@/lib/morrow/http";

/**
 * Residents' password-protected web folders (Apache .htaccess style, Basic auth).
 * Username is the resident's handle; the password is the one they reused everywhere.
 * A folder that isn't open yet in the campaign answers 404, as if it never existed.
 */
/** realm: the folder's AuthName, shown in the browser's password box (optional; else the handle). */
type Folder = { password: string; act: number; paths: string[]; realm?: string };

/** Same rule as campaign.ts currentAct(): the dev cookie (testing), else max(MORROW_ACT, the plugin's act). */
async function act(request: NextRequest): Promise<number> {
  if (DEV) {
    const dev = Number(request.cookies.get("morrow_dev_act")?.value);
    if (dev >= 1 && dev <= 10) return dev;
  }
  const n = Number.parseInt(process.env.MORROW_ACT ?? "1", 10);
  return effectiveAct(Number.isFinite(n) && n >= 1 && n <= 10 ? n : 1, await bridgeState());
}

function equal(a: string, b: string): boolean {
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i += 1) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}

/**
 * Best-effort brake on password guessing: failed Basic-auth attempts are counted per client IP
 * in this instance's memory (Vercel reuses warm instances; a cold start forgets, which is fine).
 * From the 10th failure in 10 minutes each further failure is answered 1-5 s late, and past 60
 * the folder answers 403 straight away until the window ends. A correct password always gets in
 * below the hard cap, so a group that guesses a few dozen words is only slowed, not locked out.
 */
const WINDOW_MS = 10 * 60_000;
const SLOW_AFTER = 10;
const BLOCK_AFTER = 60;
const failures = new Map<string, { n: number; since: number }>();

function clientIp(request: NextRequest): string {
  return request.headers.get("x-real-ip") ?? request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ?? "unknown";
}

function failuresFor(ip: string, now = Date.now()): { n: number; since: number } {
  const entry = failures.get(ip);
  if (entry && now - entry.since < WINDOW_MS) return entry;
  if (failures.size > 5000) {
    for (const [k, v] of failures) if (now - v.since >= WINDOW_MS) failures.delete(k);
    if (failures.size > 5000) failures.clear();
  }
  const fresh = { n: 0, since: now };
  failures.set(ip, fresh);
  return fresh;
}

const forbidden = () =>
  apachePage(403, "Forbidden", "You don't have permission to access this folder on this server. Too many failed logins; try again later.", { "Cache-Control": "no-store" });

export async function proxy(request: NextRequest) {
  let path = request.nextUrl.pathname;
  try {
    path = decodeURIComponent(path);
  } catch {
    /* a malformed escape (/%E0%A4%A) used to throw here: a raw 500 for the whole site. Match it as typed. */
  }
  // the testing switcher doesn't exist on the real site: the ordinary 404 page, like any other unknown URL
  if (!DEV && path === "/dev-act") return NextResponse.rewrite(new URL("/dev-act-not-here", request.url));
  for (const [user, folder] of Object.entries(folders as Record<string, Folder>)) {
    if (!folder.paths.some((p) => path === p || path.startsWith(`${p}/`))) continue;
    if ((await act(request)) < folder.act) {
      return apachePage(404, "Not Found", `The requested URL ${path.replace(/[&<>"']/g, "")} was not found on this server.`);
    }
    const header = request.headers.get("authorization") ?? "";
    if (header.startsWith("Basic ")) {
      const ip = clientIp(request);
      const tally = failuresFor(ip);
      try {
        const [u, ...rest] = atob(header.slice(6)).split(":");
        if (equal(u, user) && equal(rest.join(":"), folder.password)) {  // the right password always gets in, whatever the tally
          // Act 3's hand-in: June's folder opened with her password. The plugin ignores repeats.
          if (user === "juni_lantern") after(() => postHandIn("june_folder", "june's folder").catch(() => false));
          return NextResponse.next();
        }
      } catch {
        /* fall through to the challenge */
      }
      tally.n += 1;
      if (tally.n >= BLOCK_AFTER) return forbidden();  // five friends behind one NAT share this counter: only failures count
      if (tally.n >= SLOW_AFTER) await new Promise((r) => setTimeout(r, Math.min(tally.n - SLOW_AFTER + 1, 5) * 1000));
    }
    return authRequired(folder.realm ?? user);
  }
  return NextResponse.next();
}

export const config = {
  // "~" paths do not match path-to-regexp patterns reliably, so match broadly and filter above.
  matcher: ["/((?!_next/|favicon.ico).*)"],
};
