import { NextResponse } from "next/server";
import { bundle } from "@/lib/morrow/canon";
import { normalise, startSession } from "@/lib/morrow/session";

export const dynamic = "force-dynamic";

/** WHMCS 5 password reset: username -> security question -> answer. */
export async function POST(request: Request) {
  const form = await request.formData();
  const user = normalise(form.get("username")?.toString());
  const answer = form.get("answer");
  const b = await bundle();
  const to = (q: string) => NextResponse.redirect(new URL(`/pwreset.php${q}`, request.url), 303);

  if (user !== b.client_account.username) return to("?error=nouser");
  if (answer === null) return to(`?step=question&username=${encodeURIComponent(user)}`);
  if (normalise(answer.toString()) !== b.client_account.security_answer) {
    return to(`?step=question&username=${encodeURIComponent(user)}&error=answer`);
  }
  await startSession(user);
  return to("?step=done");
}
