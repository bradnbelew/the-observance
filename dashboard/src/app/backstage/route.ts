import { timingSafeEqual } from "node:crypto";
import { bundle } from "@/lib/morrow/canon";
import { bridgeState, bridgeUrl, postHint } from "@/lib/morrow/bridge";
import { stopSecret } from "@/lib/morrow/signing";

export const dynamic = "force-dynamic";

/**
 * The operator's hint switch (out of fiction; unlinked, not in robots.txt). Basic auth, any user name,
 * password = MORROW_STOP_SECRET. Hints never appear on the desk until released here (or /mrrw hint N);
 * the count resets when the act moves.
 */
function authorized(request: Request): boolean {
  const header = request.headers.get("authorization") ?? "";
  if (!header.startsWith("Basic ")) return false;
  const given = Buffer.from(atob(header.slice(6)).split(":").slice(1).join(":"));
  const want = Buffer.from(stopSecret());
  return given.length === want.length && timingSafeEqual(given, want);
}

const HEADERS = { "Cache-Control": "no-store", "X-Robots-Tag": "noindex, nofollow" };
const challenge = () =>
  new Response("Authorization Required", { status: 401, headers: { ...HEADERS, "WWW-Authenticate": 'Basic realm="backstage"' } });
const esc = (s: string) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!);

export async function GET(request: Request) {
  if (!authorized(request)) return challenge();
  const [state, b] = await Promise.all([bridgeState(), bundle()]);
  const floor = state?.hintFloor ?? 0;
  const msg = new URL(request.url).searchParams.get("m");
  const rows = b.desk.hints.map((h, i) => `
    <li class="${i < floor ? "out" : ""}">
      <p>${esc(h)}</p>
      ${i < floor ? "<b>on the desk</b>" : `<form method="post"><input type="hidden" name="n" value="${i + 1}"><button>Release${i > floor ? ` hints ${floor + 1}-${i + 1}` : ""}</button></form>`}
    </li>`).join("");
  const html = `<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<title>backstage</title>
<style>body{font:15px/1.45 system-ui;max-width:720px;margin:24px auto;padding:0 16px}li{margin:0 0 14px;padding:10px;border:1px solid #ccc;border-radius:6px}
li.out{background:#eef7ee}li p{margin:0 0 6px}button{font:inherit;padding:4px 12px}.warn{color:#a00}</style>
<h1>Hints</h1>
<p>${state ? `Plugin at <b>Act ${state.act}</b>, ${state.players} resident(s), ${state.names.map(esc).join(", ") || "none yet"}.` : `<span class="warn">Bridge unreachable${bridgeUrl() ? "" : " (MORROW_BRIDGE_URL unset)"}: hints can't be released from here. Use /mrrw hint N in the server console.</span>`}</p>
${state && state.act !== b.act ? `<p class="warn">The site is serving Act ${b.act}; the plugin is at Act ${state.act}. Releases apply to the plugin's act.</p>` : ""}
${msg ? `<p><i>${esc(msg)}</i></p>` : ""}
<p>${floor} of ${b.desk.hints.length} released this act. They go back to 0 when the act moves.</p>
<ol>${rows || "<p>No hints written for this act.</p>"}</ol>
${floor ? `<form method="post"><input type="hidden" name="n" value="0"><button>Take them all back down</button></form>` : ""}`;
  return new Response(html, { headers: { ...HEADERS, "Content-Type": "text/html; charset=utf-8" } });
}

export async function POST(request: Request) {
  if (!authorized(request)) return challenge();
  // the browser resends Basic auth on any cross-site form post: only accept our own page's buttons
  if (request.headers.get("sec-fetch-site") !== "same-origin") return new Response("Forbidden", { status: 403, headers: HEADERS });
  const n = Number((await request.formData()).get("n"));
  if (!Number.isInteger(n) || n < 0) return new Response("Bad Request", { status: 400, headers: HEADERS });
  const floor = await postHint(n);
  const m = floor === null ? "Not delivered: the bridge didn't answer. Try again or use /mrrw hint." : `Done: ${floor} hint(s) on the desk.`;
  return new Response(null, { status: 303, headers: { ...HEADERS, Location: `/backstage?m=${encodeURIComponent(m)}` } });
}
