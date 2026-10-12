import { NextResponse } from "next/server";
import { endSession } from "@/lib/morrow/session";

export const dynamic = "force-dynamic";

export async function GET(request: Request) {
  await endSession();
  return NextResponse.redirect(new URL("/clientarea.php", request.url), 303);
}
