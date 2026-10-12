import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/**
 * Acts 1-3: acknowledge only. The full Archive Desk (stored tickets, evidence-token
 * matching, evening replies, voice notes) replaces this in the Act 4 build.
 */
export async function POST(request: Request) {
  const n = 7000 + (Math.floor(Date.now() / 1000) % 2000);
  return NextResponse.redirect(new URL(`/support/index.php?sent=${n}`, request.url), 303);
}
