import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { currentAct, seal } from "@/lib/morrow/campaign";
import { TEMP_PASSWORD, normalise } from "@/lib/morrow/session";

export const dynamic = "force-dynamic";

/** Rookery's client-area login also opens the panel from Act 6. Never echoes what was typed. */
export async function POST(request: Request) {
  const form = await request.formData();
  const ok = (await currentAct()) >= 6 && normalise(String(form.get("u") ?? "")) === "rookery" && String(form.get("p") ?? "") === TEMP_PASSWORD;
  if (ok) (await cookies()).set("tcadmin", seal({ u: "rookery" }), { httpOnly: true, sameSite: "lax", path: "/" });
  return NextResponse.redirect(new URL(ok ? "/tcadmin/legacy" : "/tcadmin/legacy?tried=1", request.url), 303);
}

export async function GET(request: Request) {
  (await cookies()).delete("tcadmin");
  return NextResponse.redirect(new URL("/tcadmin/legacy", request.url), 303);
}
