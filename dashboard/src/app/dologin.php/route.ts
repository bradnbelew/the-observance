import { NextResponse } from "next/server";
import { bundle } from "@/lib/morrow/canon";
import { TEMP_PASSWORD, normalise, startSession } from "@/lib/morrow/session";

export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  const form = await request.formData();
  const user = normalise(form.get("username")?.toString());
  const pass = form.get("password")?.toString() ?? "";
  const b = await bundle();
  if (user === b.client_account.username && pass === TEMP_PASSWORD) {
    await startSession(user);
    return NextResponse.redirect(new URL("/clientarea.php", request.url), 303);
  }
  return NextResponse.redirect(new URL("/clientarea.php?incorrect=true", request.url), 303);
}
