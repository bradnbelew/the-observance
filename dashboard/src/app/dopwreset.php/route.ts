import { NextResponse } from "next/server";
import { bundle } from "@/lib/morrow/canon";
import { PWRESET_FLASH, normalise, startSession } from "@/lib/morrow/session";
import { seal } from "@/lib/morrow/campaign";

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
  // The new password is only shown to the browser that just answered the question (signed, 10 minutes).
  const res = to("?step=done");
  res.cookies.set(PWRESET_FLASH, seal({ u: user, exp: Date.now() + 10 * 60_000 }), { httpOnly: true, sameSite: "lax", path: "/pwreset.php", maxAge: 600 });
  return res;
}
