import { NextRequest, NextResponse } from "next/server";
import folders from "@/lib/canon/folders.json";
import { bridgeState, effectiveAct } from "@/lib/morrow/bridge";

/**
 * Residents' password-protected web folders (Apache .htaccess style, Basic auth).
 * Username is the resident's handle; the password is the one they reused everywhere.
 * A folder that isn't open yet in the campaign answers 404, as if it never existed.
 */
type Folder = { password: string; act: number; paths: string[] };

/** Same rule as campaign.ts currentAct(): the dev cookie (testing), else max(MORROW_ACT, the plugin's act). */
async function act(request: NextRequest): Promise<number> {
  if (process.env.MORROW_DEV === "1") {
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

function forbidden(): NextResponse {
  return new NextResponse(
    "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><html><head><title>403 Forbidden</title></head>" +
      "<body><h1>Forbidden</h1><p>You don't have permission to access this folder on this server. Too many failed logins; try again later.</p>" +
      "<hr><address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address></body></html>",
    { status: 403, headers: { "Content-Type": "text/html; charset=iso-8859-1", "Cache-Control": "no-store" } },
  );
}

export async function proxy(request: NextRequest) {
  const path = decodeURIComponent(request.nextUrl.pathname);
  for (const [user, folder] of Object.entries(folders as Record<string, Folder>)) {
    if (!folder.paths.some((p) => path === p || path.startsWith(`${p}/`))) continue;
    if ((await act(request)) < folder.act) return new NextResponse("Not Found", { status: 404 });
    const header = request.headers.get("authorization") ?? "";
    if (header.startsWith("Basic ")) {
      const ip = clientIp(request);
      const tally = failuresFor(ip);
      if (tally.n >= BLOCK_AFTER) return forbidden();
      try {
        const [u, ...rest] = atob(header.slice(6)).split(":");
        if (equal(u, user) && equal(rest.join(":"), folder.password)) return NextResponse.next();
      } catch {
        /* fall through to the challenge */
      }
      tally.n += 1;
      if (tally.n >= SLOW_AFTER) await new Promise((r) => setTimeout(r, Math.min(tally.n - SLOW_AFTER + 1, 5) * 1000));
    }
    return new NextResponse(
      "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><html><head><title>401 Authorization Required</title></head>" +
        "<body><h1>Authorization Required</h1><p>This server could not verify that you are authorized to access the document requested.</p>" +
        "<hr><address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address></body></html>",
      { status: 401, headers: { "WWW-Authenticate": `Basic realm="${user}"`, "Content-Type": "text/html; charset=iso-8859-1", "Cache-Control": "no-store" } },
    );
  }
  return NextResponse.next();
}

export const config = {
  // "~" paths do not match path-to-regexp patterns reliably, so match broadly and filter above.
  matcher: ["/((?!_next/|favicon.ico).*)"],
};
