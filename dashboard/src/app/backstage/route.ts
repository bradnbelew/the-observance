import { timingSafeEqual } from "node:crypto";
import { bundle } from "@/lib/morrow/canon";
import { bridgeJournal, bridgeStateFresh, bridgeUrl, lastGoodState, postDeskEvent, postHint, postOp } from "@/lib/morrow/bridge";
import { stopFingerprint, stopSecret } from "@/lib/morrow/signing";
import { envAct } from "@/lib/morrow/campaign";
import { authRequired, readForm } from "@/lib/morrow/http";

export const dynamic = "force-dynamic";
export const maxDuration = 30;  // snapshot / rollback run on the plugin and can take a while

/**
 * The operator's control room (out of fiction; unlinked, not in robots.txt). Basic auth, any user name,
 * password = MORROW_STOP_SECRET. It shows the plugin's state, the site's act floor, residents, the USB
 * vote, the desk's hand-ins and the journal, and runs any /mrrw command through the signed bridge
 * (POST /event {"kind":"op","cmd":...}). Hints never appear on the desk until released here (or
 * /mrrw hint N); the count resets when the act moves.
 */
function authorized(request: Request): boolean {
  const header = request.headers.get("authorization") ?? "";
  if (!header.startsWith("Basic ")) return false;
  try {
    const given = Buffer.from(atob(header.slice(6)).split(":").slice(1).join(":"));
    const want = Buffer.from(stopSecret());
    return given.length === want.length && timingSafeEqual(given, want);
  } catch {
    return false;  // a malformed header, or no stop secret on a production deployment
  }
}

const HEADERS = { "Cache-Control": "no-store", "X-Robots-Tag": "noindex, nofollow" };
const esc = (s: unknown) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);

/** Commands that change the campaign in ways a misclick shouldn't: the page asks first, and the server refuses them unconfirmed. */
const DESTRUCTIVE = /^(act|rollback|handin|flag|residents\s+remove)\b/i;
/** Refs the site itself posts as hand-ins besides the desk rules and file requests. */
const SITE_REFS = ["june_folder", "console_key", "brief", "brief_missing", "booking"];

const chicago = (d: Date) => d.toLocaleString("en-US", { timeZone: "America/Chicago", month: "short", day: "numeric", hour: "numeric", minute: "2-digit" });
function when(d: Date | null): string {
  if (!d) return "unknown";
  const h = (Date.now() - d.getTime()) / 3_600_000;
  const ago = Math.abs(h) < 1 ? `${Math.round(h * 60)} min` : Math.abs(h) < 48 ? `${h.toFixed(1)} h` : `${(h / 24).toFixed(1)} days`;
  return `${chicago(d)} Chicago (${h >= 0 ? `${ago} ago` : `in ${ago.replace("-", "")}`})`;
}

/** One op button. */
const button = (cmd: string, label = cmd) =>
  `<form method="post" onsubmit="return go(this,event)"><input type="hidden" name="cmd" value="${esc(cmd)}"><input type="hidden" name="sure"><button>${esc(label)}</button></form>`;

export async function GET(request: Request) {
  if (!authorized(request)) return authRequired("backstage");
  const [{ state, raw }, journal, desk, b] = await Promise.all([bridgeStateFresh(), bridgeJournal(50), postOp("desk", 5000), bundle()]);
  const msg = new URL(request.url).searchParams.get("m");
  const env = envAct();
  const good = lastGoodState();

  // the site's act floor: MORROW_ACT is what the site serves when the plugin can't be asked
  let floor: string;
  if (!bridgeUrl()) floor = `<p class="warn">MORROW_BRIDGE_URL is unset: the site serves MORROW_ACT=${env} and never follows the plugin.</p>`;
  else if (!state) {
    floor = good
      ? `<p class="warn">The bridge isn't answering. This instance keeps serving the last state it heard (Act ${good.act}); a fresh instance would fall back to MORROW_ACT=${env}.</p>`
      : `<p class="warn">The bridge isn't answering: the site is falling back to MORROW_ACT=${env}.</p>`;
  } else if (env > state.act) {
    floor = `<p class="warn">MORROW_ACT=${env} is ahead of the plugin (Act ${state.act}): the site serves Act ${env}. Lower MORROW_ACT on Vercel, or move the plugin to ${env}.</p>`;
  } else floor = `<p class="ok">The site follows the plugin. MORROW_ACT=${env} is only the floor it falls back to.</p>`;
  let fp = "";
  try {
    if (state?.fingerprint && state.fingerprint !== stopFingerprint()) fp = `<p class="warn">Stop-secret fingerprints differ (plugin ${esc(state.fingerprint)}, site ${stopFingerprint()}): the finale code will be refused.</p>`;
  } catch { /* no secret: the page wouldn't have opened */ }

  const campaign = state ? `
    <table>
      <tr><th>Act</th><td><b>${state.act}</b> since ${when(state.actStarted)}</td></tr>
      <tr><th>Auto-advance</th><td>${state.autoAdvance === null ? "?" : state.autoAdvance ? "on" : "<b>off</b> (you drive every act)"} ${button("auto on")}${button("auto off")}</td></tr>
      <tr><th>Finale</th><td>${state.stopped ? `<b>stopped</b> ${when(state.stoppedAt)}` : state.ready ? "<b>ready</b> (the server reports ready on the list)" : "not ready"}${Array.isArray(raw?.stop_log) ? " · stop log written" : ""}</td></tr>
      <tr><th>Site</th><td>serving Act ${b.act} · MORROW_ACT=${env} · MORROW_ACT_STARTED=${esc(process.env.MORROW_ACT_STARTED?.trim() || "unset")}</td></tr>
    </table>` : "";

  const residents = state ? `
    <p>${state.players} resident(s): ${state.names.map((n) => `<code>${esc(n)}</code>${state.firstJoins[n] ? ` <small>(${state.firstJoins[n].join(" ")})</small>` : ""}`).join(", ") || "none yet"}</p>
    <form method="post" onsubmit="return go(this,event)" class="row"><input name="arg" placeholder="Minecraft name" maxlength="16" required><input type="hidden" name="sure">
      <button name="op" value="residents add">Add resident</button><button name="op" value="residents remove">Remove resident</button></form>
    <p>USB vote (${state.usb.open ? "open" : "closed"}): ${(["clean", "recovered", "corpus"] as const).map((k) => `${esc(b.usb_vote?.options[k]?.label ?? k)} <b>${state.usb.votes[k]}</b>`).join(" · ")}${state.usb.winner ? ` · winner <b>${esc(state.usb.winner)}</b>` : " · no winner yet"}</p>` : "<p>(needs the bridge)</p>";

  const hintFloor = state?.hintFloor ?? 0;
  const hints = b.desk.hints.map((h, i) => `
    <li class="${i < hintFloor ? "out" : ""}"><p>${esc(h)}</p>
      ${i < hintFloor ? "<b>on the desk</b>" : `<form method="post"><input type="hidden" name="n" value="${i + 1}"><button>Release${i > hintFloor ? ` hints ${hintFloor + 1}-${i + 1}` : ""}</button></form>`}</li>`).join("");

  const refs = [...new Set([...b.desk.rules.map((r) => r.id), ...Object.keys(b.desk.files).map((f) => f.replace(/\.[a-z0-9]+$/i, "")), ...SITE_REFS])];
  const rows = (journal ?? []).slice().reverse().map((e) => {
    if (!e || typeof e !== "object") return `<tr><td colspan="3">${esc(e)}</td></tr>`;
    const o = e as Record<string, unknown>;
    const at = typeof o.at === "string" && !Number.isNaN(Date.parse(o.at)) ? chicago(new Date(o.at)) : esc(o.at ?? "");
    const detail = JSON.stringify(o.detail ?? Object.fromEntries(Object.entries(o).filter(([k]) => k !== "at" && k !== "kind")));
    return `<tr><td>${at}</td><td><code>${esc(o.kind ?? "")}</code></td><td>${esc(detail.length > 400 ? `${detail.slice(0, 400)}...` : detail)}</td></tr>`;
  }).join("");

  const html = `<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width"><meta name="robots" content="noindex">
<title>backstage</title>
<style>body{font:14px/1.45 system-ui;max-width:980px;margin:16px auto;padding:0 14px;color:#111}h1{margin:0 0 4px}h2{margin:22px 0 6px;font-size:17px;border-bottom:1px solid #ddd}
table{border-collapse:collapse;width:100%}th,td{border-bottom:1px solid #eee;padding:4px 6px;text-align:left;vertical-align:top}th{width:130px;color:#555;font-weight:600}
form{display:inline-block;margin:2px 4px 2px 0}.row{display:flex;gap:6px;flex-wrap:wrap;align-items:center;margin:4px 0}button{font:inherit;padding:3px 10px;cursor:pointer}
input,select{font:inherit;padding:2px 5px}pre{background:#f5f5f5;border:1px solid #ddd;padding:8px;white-space:pre-wrap;max-height:420px;overflow:auto;margin:6px 0}
.warn{color:#a00}.ok{color:#060}li{margin:0 0 10px;padding:8px;border:1px solid #ccc;border-radius:6px}li.out{background:#eef7ee}li p{margin:0 0 6px}.danger button{border-color:#a00}
#journal td{font-size:12px}#journal td:nth-child(3){font-family:ui-monospace,monospace;word-break:break-all}small{color:#666}</style>
<script>var D=${DESTRUCTIVE};function go(f,e){var s=e&&e.submitter,c=f.cmd?f.cmd.value:[s&&s.name==="op"?s.value:(f.op?f.op.value:""),f.arg?f.arg.value:"",f.arg2?f.arg2.value:""].join(" ");c=c.trim();
var q=f.getAttribute("data-confirm")||(D.test(c)?"Run /mrrw "+c+" ?":"");if(!q)return true;if(!confirm(q))return false;f.sure.value="1";return true}</script>
<h1>Control room</h1>
<p><a href="/backstage">Refresh</a> · bridge ${esc(bridgeUrl() || "unset")} · ${state ? '<span class="ok">answering</span>' : '<span class="warn">not answering</span>'}</p>
${msg ? `<pre>${esc(msg)}</pre>` : ""}
<h2>Campaign</h2>${floor}${fp}${campaign}
<h2>Residents and the USB vote</h2>${residents}
<h2>Commands</h2>
<div class="row">${["status", "flags", "desk", "residents", "dawn", "restore", "replay", "snapshot", "figures respawn", "figures clear"].map((c) => button(c)).join("")}</div>
<div class="row danger">
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="act"><input type="hidden" name="sure"><select name="arg">${Array.from({ length: 10 }, (_, i) => `<option${i + 1 === (state?.act ?? env) ? " selected" : ""}>${i + 1}</option>`).join("")}</select><input name="arg2" placeholder="began (optional ISO time)" size="24"><button>Set act</button></form>
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="flag"><input type="hidden" name="sure"><input name="arg" placeholder="flag name" required size="14"><select name="arg2"><option>on</option><option>off</option></select><button>Set flag</button></form>
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="handin"><input type="hidden" name="sure"><input name="arg" placeholder="kind" list="refs" required size="14"><button>Apply hand-in (plugin)</button></form>
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="rollback"><input type="hidden" name="sure"><input name="arg" placeholder="snapshot name" required size="16"><button>Roll back</button></form>
</div>
<div class="row">
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="repair"><input type="hidden" name="sure"><input name="arg" value="all" size="10"><button>Repair</button></form>
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="hint"><input type="hidden" name="sure"><input name="arg" type="number" min="0" max="9" value="${hintFloor}" style="width:4em"><button>Hint floor</button></form>
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="journal"><input type="hidden" name="sure"><input name="arg" type="number" min="1" max="500" value="100" style="width:5em"><button>Journal (as text)</button></form>
  <form method="post" onsubmit="return go(this,event)"><input type="hidden" name="op" value="say"><input type="hidden" name="sure"><input name="arg" placeholder="say in game chat" required size="30"><button>Say</button></form>
</div>
<div class="row"><form method="post" onsubmit="return go(this,event)"><input type="hidden" name="sure"><code>/mrrw</code> <input name="cmd" placeholder="any command, as in the console" required size="44"><button>Run</button></form></div>
<h2>Desk hand-ins</h2>
<pre>${esc(desk.output)}</pre>
<form method="post" onsubmit="return go(this,event)" data-confirm="Re-post this hand-in from the site? It may move the act." class="row"><input type="hidden" name="sure">
  <input name="repost" list="refs" placeholder="ref, e.g. june_folder" required size="22"><button>Re-post hand-in (as the site)</button></form>
<datalist id="refs">${refs.map((r) => `<option value="${esc(r)}">`).join("")}</datalist>
<h2>Hints</h2>
<p>${hintFloor} of ${b.desk.hints.length} released this act. They go back to 0 when the act moves.${state && state.act !== b.act ? ` <span class="warn">The site is serving Act ${b.act}; the plugin is at Act ${state.act}. Releases apply to the plugin's act.</span>` : ""}</p>
<ol>${hints || "<p>No hints written for this act.</p>"}</ol>
${hintFloor ? `<form method="post"><input type="hidden" name="n" value="0"><button>Take them all back down</button></form>` : ""}
<h2>Journal (last ${journal?.length ?? 0}, newest first)</h2>
${journal ? `<table id="journal">${rows}</table>` : `<p class="warn">The bridge didn't answer GET /journal.</p>`}`;
  return new Response(html, { headers: { ...HEADERS, "Content-Type": "text/html; charset=utf-8" } });
}

export async function POST(request: Request) {
  if (!authorized(request)) return authRequired("backstage");
  // the browser resends Basic auth on any cross-site form post: only accept our own page's buttons
  const site = request.headers.get("sec-fetch-site");
  const origin = request.headers.get("origin");
  if (site !== "same-origin" && !(site === null && origin && origin === new URL(request.url).origin)) {
    return new Response("Forbidden", { status: 403, headers: HEADERS });
  }
  const form = await readForm(request);
  const field = (k: string) => String(form.get(k) ?? "").replace(/[\r\n]+/g, " ").trim().slice(0, 400);
  let m: string;
  if (form.has("n")) {
    const n = Number(form.get("n"));
    if (!Number.isInteger(n) || n < 0) return new Response("Bad Request", { status: 400, headers: HEADERS });
    const floor = await postHint(n);
    m = floor === null ? "Not delivered: the bridge didn't answer. Try again or use /mrrw hint." : `Done: ${floor} hint(s) on the desk.`;
  } else if (form.has("repost")) {
    const ref = field("repost").toLowerCase();
    if (form.get("sure") !== "1") m = `Not re-posted: confirm it first (${ref}).`;
    else {
      const b = await bundle();
      const file = Object.entries(b.desk.files).find(([f]) => f.replace(/\.[a-z0-9]+$/i, "") === ref)?.[1];
      const voice = b.desk.rules.find((r) => r.id === ref)?.voice ?? file?.voice ?? null;
      const ok = await postDeskEvent({ ref, reply_at: null, voice, subject: "operator re-post" }, true);
      m = ok ? `Re-posted ${ref}: the plugin took it (a repeat is ignored there, or applied if its act has come).` : `Not delivered: the bridge didn't answer for ${ref}.`;
    }
  } else {
    const cmd = (form.has("cmd") ? field("cmd") : [field("op"), field("arg"), field("arg2")].filter(Boolean).join(" ")).replace(/^\/?mrrw\s+/i, "");
    if (!cmd) m = "Nothing to run.";
    else if (DESTRUCTIVE.test(cmd) && form.get("sure") !== "1") m = `Not run: /mrrw ${cmd} changes the campaign, confirm it first.`;
    else {
      const r = await postOp(cmd);
      console.log(`MORROW_OP cmd="${cmd}" ${r.ok ? "ok" : "failed"}`);
      m = `/mrrw ${cmd}${r.ok ? "" : "  (FAILED)"}\n${r.output}`;
    }
  }
  if (m.length > 3500) m = `${m.slice(0, 3500)}\n... (cut; the full output is in the server console)`;
  return new Response(null, { status: 303, headers: { ...HEADERS, Location: `/backstage?m=${encodeURIComponent(m)}` } });
}
