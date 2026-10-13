import { createHmac } from "node:crypto";

/**
 * The two campaign secrets. Local testing falls back to public defaults; a production
 * deployment refuses to sign anything with them, so a missed env var fails loudly (500 +
 * a clear log line) instead of letting anyone forge sessions, desk tickets or stop codes.
 *
 *   MORROW_SESSION_SECRET  signs WHMCS sessions, desk tickets, file and voice links
 *   MORROW_STOP_SECRET     shared with the mrrw plugin; signs the Act 10 stop code
 */
const DEV_SESSION_SECRET = "copperline-mirror-dev-secret";
const DEV_STOP_SECRET = "mrrw-dev-stop-secret";

/** Production = a Vercel production deployment, or any `next start` without MORROW_DEV=1. Never during `next build`. */
export function isProduction(): boolean {
  if (process.env.NEXT_PHASE === "phase-production-build") return false;
  if (process.env.VERCEL_ENV === "production") return true;
  return process.env.NODE_ENV === "production" && process.env.MORROW_DEV !== "1";
}

function required(name: string, devDefault: string): string {
  const value = process.env[name]?.trim();
  if (value && value !== devDefault) return value;
  if (isProduction()) {
    throw new Error(
      `${name} is ${value ? "still the public dev default" : "not set"}. ` +
        `Set a unique random value in the Vercel project environment and redeploy; the mirror refuses to sign with a public key.`,
    );
  }
  return devDefault;
}

export const sessionSecret = () => required("MORROW_SESSION_SECRET", DEV_SESSION_SECRET);
export const stopSecret = () => required("MORROW_STOP_SECRET", DEV_STOP_SECRET);

/**
 * Stop-secret fingerprint, for checking the site and the plugin hold the same MORROW_STOP_SECRET
 * before the finale without ever printing the secret itself.
 *
 *   fingerprint = first 8 hex chars (lowercase) of HMAC-SHA256(key = stop secret (UTF-8), message = "mrrw-fingerprint")
 *
 * The site logs it once at server start (src/instrumentation.ts: "MORROW stop-secret fingerprint ...");
 * the plugin prints the same value in `/mrrw status`. They must match.
 */
export function stopFingerprint(secret: string = stopSecret()): string {
  return createHmac("sha256", secret).update("mrrw-fingerprint").digest("hex").slice(0, 8);
}

export const usingDevStopSecret = () => (process.env.MORROW_STOP_SECRET?.trim() || DEV_STOP_SECRET) === DEV_STOP_SECRET;
