import { createHmac, timingSafeEqual } from "node:crypto";
import { cookies } from "next/headers";
import { sessionSecret } from "./signing";
import { unseal } from "./campaign";

/** WHMCS-style client session for Rookery's account. Signed so it can't be typed in by hand. */
const COOKIE = "WHMCSsession";
const secret = sessionSecret;

/** Shown once after a successful reset; also accepted by the login form afterwards. */
export const TEMP_PASSWORD = "cl-mirror-71e4";

/** Short-lived signed cookie from dopwreset.php that lets pwreset.php?step=done show TEMP_PASSWORD. */
export const PWRESET_FLASH = "WHMCSpwreset";

export function pwresetFlashValid(token: string | undefined, username: string, now = Date.now()): boolean {
  const flash = unseal<{ u?: string; exp?: number }>(token);
  return !!flash && flash.u === username && typeof flash.exp === "number" && now < flash.exp;
}

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
