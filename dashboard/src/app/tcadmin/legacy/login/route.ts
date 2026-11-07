import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { currentAct, seal } from "@/lib/morrow/campaign";
import { SESSION_MAX_AGE, TEMP_PASSWORD, normalise } from "@/lib/morrow/session";
import { readForm } from "@/lib/morrow/http";

export const dynamic = "force-dynamic";

/** Rookery's client-area login also opens the panel from Act 6. Never echoes what was typed. */
export async function POST(request: Request) {
  const form = await readForm(request);
  const ok = (await currentAct()) >= 6 && normalise(String(form.get("u") ?? "")) === "rookery" && String(form.get("p") ?? "") === TEMP_PASSWORD;
  // a lasting cookie: a browser-session one logged the group out of the panel between sessions days apart
  if (ok) (await cookies()).set("tcadmin", seal({ u: "rookery" }), { httpOnly: true, sameSite: "lax", path: "/", maxAge: SESSION_MAX_AGE });
  return NextResponse.redirect(new URL(ok ? "/tcadmin/legacy" : "/tcadmin/legacy?tried=1", request.url), 303);
}

export async function GET(request: Request) {
  (await cookies()).delete("tcadmin");
  return NextResponse.redirect(new URL("/tcadmin/legacy", request.url), 303);
}
