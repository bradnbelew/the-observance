import { createHmac } from "node:crypto";

/**
 * The bridge request signature, shared by the site, the Discord bot and the mrrw plugin.
 *
 *   X-Mrrw-Ts  = unix seconds
 *   X-Mrrw-Sig = lowercase hex HMAC-SHA256(key = MORROW_STOP_SECRET,
 *                message = METHOD + "\n" + PATH_WITH_QUERY + "\n" + TS + "\n" + BODY)
 *
 * PATH_WITH_QUERY is the request target exactly as sent (raw, still percent-encoded), e.g.
 * "/state" or "/corpus?heard=hi%20there&persona=cairn&seed=7". BODY is "" for a GET.
 *
 * Test vector (morrow/world/e2e_site.py and discord-v2 `npm run dry` both check it):
 *   key "test-stop-secret", GET /state, ts 1700000000, empty body
 *   -> 568f9284666a5990ca2bebb4c800e07a26d3e792e627d51c0e7e1d860433c833
 *
 * No other imports, so `node` can run this file directly (type stripping) for that cross-check.
 */
export function bridgeSignature(secret: string, method: string, pathWithQuery: string, ts: string, body: string): string {
  return createHmac("sha256", secret).update(`${method.toUpperCase()}\n${pathWithQuery}\n${ts}\n${body}`).digest("hex");
}
