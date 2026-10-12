import { NextResponse } from "next/server";
import { DEV, DEV_ACT_COOKIE } from "@/lib/morrow/campaign";

export const dynamic = "force-dynamic";

/** Local testing only (MORROW_DEV=1): /dev-act?n=5 switches the act for this browser. */
export async function GET(request: Request) {
  if (!DEV) return new Response("Not Found", { status: 404 });
  const url = new URL(request.url);
  const n = Number(url.searchParams.get("n"));
  const back = url.searchParams.get("back") || "/";
  const res = NextResponse.redirect(new URL(back.startsWith("/") ? back : "/", request.url), 303);
  if (n >= 1 && n <= 10) res.cookies.set(DEV_ACT_COOKIE, String(n), { path: "/", sameSite: "lax" });
  return res;
}
