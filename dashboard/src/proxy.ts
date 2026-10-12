import { NextRequest, NextResponse } from "next/server";
import folders from "@/lib/canon/folders.json";

/**
 * Residents' password-protected web folders (Apache .htaccess style, Basic auth).
 * Username is the resident's handle; the password is the one they reused everywhere.
 * A folder that isn't open yet in the campaign answers 404, as if it never existed.
 */
type Folder = { password: string; act: number; paths: string[] };

function act(): number {
  const n = Number.parseInt(process.env.MORROW_ACT ?? "1", 10);
  return Number.isFinite(n) ? n : 1;
}

function equal(a: string, b: string): boolean {
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i += 1) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}

export function proxy(request: NextRequest) {
  const path = decodeURIComponent(request.nextUrl.pathname);
  for (const [user, folder] of Object.entries(folders as Record<string, Folder>)) {
    if (!folder.paths.some((p) => path === p || path.startsWith(`${p}/`))) continue;
    if (act() < folder.act) return new NextResponse("Not Found", { status: 404 });
    const header = request.headers.get("authorization") ?? "";
    if (header.startsWith("Basic ")) {
      try {
        const [u, ...rest] = atob(header.slice(6)).split(":");
        if (equal(u, user) && equal(rest.join(":"), folder.password)) return NextResponse.next();
      } catch {
        /* fall through to the challenge */
      }
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
