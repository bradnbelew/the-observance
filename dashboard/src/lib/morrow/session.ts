import { createHmac, timingSafeEqual } from "node:crypto";
import { cookies } from "next/headers";

/** WHMCS-style client session for Rookery's account. Signed so it can't be typed in by hand. */
const COOKIE = "WHMCSsession";
const secret = () => process.env.MORROW_SESSION_SECRET || "copperline-mirror-dev-secret";

/** Shown once after a successful reset; also accepted by the login form afterwards. */
export const TEMP_PASSWORD = "cl-mirror-71e4";

function sign(user: string): string {
  return `${user}.${createHmac("sha256", secret()).update(user).digest("hex").slice(0, 24)}`;
}

export async function sessionUser(): Promise<string | null> {
  const raw = (await cookies()).get(COOKIE)?.value;
  if (!raw) return null;
  const user = raw.split(".")[0];
  const a = Buffer.from(raw);
  const b = Buffer.from(sign(user));
  return a.length === b.length && timingSafeEqual(a, b) ? user : null;
}

export async function startSession(user: string): Promise<void> {
  (await cookies()).set(COOKIE, sign(user), { httpOnly: true, sameSite: "lax", path: "/", maxAge: 60 * 60 * 24 * 30 });
}

export async function endSession(): Promise<void> {
  (await cookies()).delete(COOKIE);
}

export function normalise(answer: string | null | undefined): string {
  return (answer ?? "").trim().toLowerCase();
}
