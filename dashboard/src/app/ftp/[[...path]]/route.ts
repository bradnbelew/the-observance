import { currentAct } from "@/lib/morrow/campaign";
import { sessionUser } from "@/lib/morrow/session";
import { resolveFile, serveFile } from "@/lib/morrow/serve";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/**
 * Rookery's FTP, as a 2011 browser showed ftp:// listings. Needs his client-area session.
 *   /             world/ (locked), incoming/ (upload-only, no LIST), recovery/ (Act 9)
 *   /incoming/    550 on LIST; an exact filename downloads (MIC-5-18-B.WAV, Act 8; found via D2 + D11)
 *   /notrecovery/ never listed at the root; Rookery's clean copy (Act 9; found via D15)
 * Names match case-insensitively (players type mic-5-18-b.wav); files stream with byte ranges.
 */
type Entry = { name: string; size: string; date: string; dir?: boolean };

function listing(dir: string, entries: Entry[]): Response {
  const rows = entries.map((e) => `<tr><td><a href="/ftp${dir}${e.name}${e.dir ? "/" : ""}">${e.name}${e.dir ? "/" : ""}</a></td><td>${e.size}</td><td>${e.date}</td></tr>`).join("");
  return html(`<h1>Index of ftp://ftp.copperlinehosting.com${dir}</h1>` +
    (dir !== "/" ? `<p><a href="/ftp/">Up to higher level directory</a></p>` : "") +
    `<table><thead><tr><th>Name</th><th>Size</th><th>Last Modified</th></tr></thead><tbody>${rows}</tbody></table>`);
}

function html(body: string, status = 200): Response {
  return new Response(`<!DOCTYPE html><html><head><title>ftp.copperlinehosting.com</title><style>body{font:13px monospace;background:#fff;color:#000;padding:8px}a{color:#00e}td,th{padding:2px 14px 2px 0;text-align:left}</style></head><body>${body}</body></html>`,
    { status, headers: { "Content-Type": "text/html; charset=utf-8" } });
}

/** Path segments come straight from the URL; never echo them unescaped. */
const esc = (v: string) => v.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

const deny = (what: string) => html(`<p>550 ${what}: Permission denied.</p><p><a href="/ftp/">back</a></p>`, 403);

async function file(request: Request, rel: string[], name: string, type: string): Promise<Response> {
  const found = await resolveFile(rel.slice(0, -1), rel[rel.length - 1]);
  const res = found && await serveFile(request, found, { "Content-Type": type, "Content-Disposition": `attachment; filename="${name}"` });
  return res || html(`<p>550 ${name}: No such file or directory.</p>`, 404);
}

export async function GET(request: Request, { params }: { params: Promise<{ path?: string[] }> }) {
  if ((await sessionUser()) !== "rookery") {
    return html(`<p>530 Login incorrect.</p><p>Log in to the <a href="/clientarea.php">client area</a> first.</p>`, 401);
  }
  const act = await currentAct();
  if (act < 5) return html(`<p>421 Service not available: locked by a running recovery job (mrrw).</p>`, 503);
  const p = (await params).path ?? [];
  const dir = p[0]?.toLowerCase();
  const name = p[1];
  const lname = name?.toLowerCase();
  if (!dir) {
    const root: Entry[] = [
      { name: "incoming", size: "-", date: "Jun 02  2011", dir: true },
      { name: "world", size: "-", date: "May 18  2011", dir: true },
      { name: "welcome.msg", size: "112", date: "Oct 30  2010" },
    ];
    if (act >= 9) root.splice(1, 0, { name: "recovery", size: "-", date: "Aug 03  2016", dir: true });
    return listing("/", root);
  }
  if (dir === "welcome.msg") return new Response("mossfield ftp. upload to incoming/, i'll sort it. dont touch world/ while the server's up. -rook\n", { headers: { "Content-Type": "text/plain" } });
  if (dir === "world") return deny("world");
  if (dir === "incoming") {
    if (!name) return deny("incoming (upload-only, no listing)");
    if (lname === "mic-5-18-b.wav" && act >= 8) return file(request, ["media", "ftp", "incoming", "MIC-5-18-B.WAV"], "MIC-5-18-B.WAV", "audio/wav");
    return html(`<p>550 ${esc(name)}: No such file or directory.</p>`, 404);
  }
  if (dir === "recovery" && act >= 9) {
    if (!name) return listing("/recovery/", [{ name: "mossfield_2011-04-20.zip", size: "41 KB", date: "Aug 03  2016" }]);
    if (lname === "mossfield_2011-04-20.zip") return file(request, ["files", "recovery-mossfield_2011-04-20.zip"], "mossfield_2011-04-20.zip", "application/zip");
  }
  if (dir === "notrecovery" && act >= 9) {
    if (!name) return listing("/notrecovery/", [{ name: "mossfield_2011-04-20.zip", size: "40 KB", date: "Sep 04  2011" }]);
    if (lname === "mossfield_2011-04-20.zip") return file(request, ["files", "notrecovery-mossfield_2011-04-20.zip"], "mossfield_2011-04-20.zip", "application/zip");
  }
  return html(`<p>550 ${esc(p.join("/"))}: No such file or directory.</p>`, 404);
}
