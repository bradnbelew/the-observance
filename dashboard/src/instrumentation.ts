/**
 * Runs once when a server instance starts (each Vercel cold start, or `next dev`/`next start`).
 * Logs the operator checks: the stop-secret fingerprint (compare with `/mrrw status` in game
 * before the finale) and any problem with the act clock. Never logs a secret.
 */
export async function register() {
  if (process.env.NEXT_RUNTIME !== "nodejs") return;
  const { sessionSecret, stopFingerprint, stopSecret, usingDevStopSecret } = await import("./lib/morrow/signing");
  const { actStartedProblem, deskFast } = await import("./lib/morrow/campaign");
  try {
    sessionSecret();
  } catch (err) {
    console.error(`MORROW error: ${(err as Error).message}`);
  }
  try {
    const fp = stopFingerprint(stopSecret());
    console.log(`MORROW stop-secret fingerprint ${fp}${usingDevStopSecret() ? " (DEV DEFAULT secret: local testing only)" : ""}; must match /mrrw status`);
  } catch (err) {
    console.error(`MORROW error: ${(err as Error).message}`);
  }
  const bridge = process.env.MORROW_BRIDGE_URL?.trim();
  console.log(bridge
    ? `MORROW bridge ${bridge}: the act follows the plugin (the later of MORROW_ACT and the plugin's act)`
    : "MORROW bridge off (MORROW_BRIDGE_URL unset): MORROW_ACT / MORROW_ACT_STARTED / MORROW_PLAYERS only");
  if (!deskFast()) {
    const problem = actStartedProblem();
    if (problem) console.warn(`MORROW warning: ${problem}`);
  }
}
