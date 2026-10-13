import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { bundle, currentAct, type Bundle, type LogRow, type Marker } from "@/lib/morrow/canon";

/**
 * Copperline's free customer web space, served at /~name/ (rewritten to /u/name/).
 * Pages here are plain 2011 hand-made HTML, not the Copperline template.
 */
export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { user, path = [] } = await params;
  const sub = path.join("/");
  if (user === "mossfield") {
    return { title: ({ "": "mossfield", dynmap: "Minecraft Dynamic Map", "mapkey.txt": "mapkey.txt", "server.properties": "server.properties", logblock: "LogBlock", logs: "logs" } as Record<string, string>)[sub] ?? "404 Not Found" };
  }
  return { title: `Index of /~${user}/${sub}` };
}
export const dynamic = "force-dynamic";

type Params = Promise<{ user: string; path?: string[] }>;
type Search = Promise<Record<string, string | undefined>>;

export default async function UserSpace({ params, searchParams }: { params: Params; searchParams: Search }) {
  const { user, path = [] } = await params;
  const q = await searchParams;
  const b = await bundle();
  const act = await currentAct();
  const sub = path.join("/");

  if (user === "mossfield") {
    const files = act >= 2 ? b.mossfield_files ?? [] : [];
    if (sub === "") return <MossfieldHome files={files} />;
    const kept = files.find((f) => f.file === sub);
    if (kept) return <Plain title={kept.file}><pre className="plain-pre">{kept.body}</pre></Plain>;
    if (sub === "dynmap") return act >= 2 ? <Dynmap b={b} /> : <ApacheMissing path="/~mossfield/dynmap/" />;
    if (sub === "mapkey.txt") return act >= 2 ? <Plain title="mapkey.txt"><pre className="plain-pre">{mapKeyText(b)}</pre></Plain> : <ApacheMissing path="/~mossfield/mapkey.txt" />;
    if (sub === "logblock") return act >= 3 ? <LogBlockViewer b={b} q={q} /> : <LogBlockLocked />;
    if (sub === "logs") return <Logs b={b} day={q.day} />;
    notFound();
  }
  if (user === "juni_lantern" && act >= 3) {
    if (sub === "" || sub === "screenshots") return <JuneIndex b={b} />;
    notFound();
  }
  if (user === "patchcord" && act >= 5) {
    if (sub === "" || sub === "mumble") return <ApacheIndex title="/~patchcord/mumble" rows={b.patchcord_folder.map((f) => ({ name: f.file, href: f.size === "0" ? undefined : `/media/patchcord/${f.file}`, mtime: f.mtime, size: f.size }))} />;
    notFound();
  }
  if (user === "tvale" && act >= 6) {
    if (sub === "box" && b.tvale_box.length) return <ApacheIndex title="/~tvale/box" rows={b.tvale_box.map((f) => ({ name: f, href: `/media/tvalebox/${f}`, mtime: "14-Dec-2014 03:40", size: "1K" }))} />;
    if (sub === "") return <ApacheIndex title="/~tvale" rows={[...b.tvale_folder.map((f) => ({ name: f.file, href: `/media/tvale/${f.file}`, mtime: f.mtime, size: f.size })),
      ...(b.tvale_box.length ? [{ name: "box/", href: "/~tvale/box/", mtime: "14-Dec-2014 03:40", size: "-" }] : [])]} />;
    notFound();
  }
  notFound();
}

function Plain({ title, children }: { title: string; children: React.ReactNode }) {
  return <div className="plain-2011" data-title={title}>{children}</div>;
}

function ApacheMissing({ path }: { path: string }) {
  return (
    <Plain title="404 Not Found">
      <h1>Not Found</h1>
      <p>The requested URL {path} was not found on this server.</p>
      <hr /><address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address>
    </Plain>
  );
}

/** Rook's page. From Act 2 it also lists the files he left next to it (server.properties: level-seed=bracken). */
function MossfieldHome({ files }: { files: { file: string; mtime: string; size: string }[] }) {
  return (
    <Plain title="mossfield">
      <h1>mossfield</h1>
      <p>survival, whitelist, be nice. ask rook to get added.</p>
      <ul>
        <li><a href="/~mossfield/dynmap/">dynmap</a> (live map)</li>
        <li><a href="/~mossfield/logs/">server logs</a> (some days)</li>
        <li><a href="/~mossfield/logblock/">logblock</a> (who broke my stuff)</li>
        <li><a href="/forum-import/forumdisplay.php?f=12">forum</a></li>
      </ul>
      {files.length ? (
        <>
          <p>other stuff:</p>
          <table className="plain-table">
            <thead><tr><th>Name</th><th>Last modified</th><th>Size</th></tr></thead>
            <tbody>{files.map((f) => (
              <tr key={f.file}><td><a href={`/~mossfield/${f.file}`}>{f.file}</a></td><td>{f.mtime}</td><td>{f.size}</td></tr>
            ))}</tbody>
          </table>
        </>
      ) : null}
      <p><small>last updated 22 feb 2011. new map!!</small></p>
    </Plain>
  );
}

/**
 * Rook's map key. Dynmap only got markers (markers.json) in Sept 2011, so in Mar 2011 the places
 * were a text file Rook kept next to the map: /~mossfield/mapkey.txt. Accepts the canon as finished
 * text (mapkey / mapkey_txt) or as marker rows (mapkey / dynmap_markers), and writes rows out by hand.
 */
function mapKeyText(b: Bundle): string {
  if (typeof b.mapkey === "string") return b.mapkey;
  if (b.mapkey_txt) return b.mapkey_txt;
  const rows = (Array.isArray(b.mapkey) ? b.mapkey : b.dynmap_markers) ?? [];
  const pos = (m: Marker) => `${m.x} ${m.y} ${m.z}`;
  const w = Math.max(10, ...rows.map((m) => pos(m).length)) + 3;
  return ["mossfield map key. x y z, f3 coords. ask rook if ur place is missing", "", ...rows.map((m) => pos(m).padEnd(w) + m.label)].join("\n") + "\n";
}

function Dynmap({ b }: { b: Bundle }) {
  return (
    <Plain title="Minecraft Dynamic Map">
      <h1>Dynmap 0.12 - world</h1>
      <p><i>Map tiles were not retained in this mirror. Rook&apos;s key to the map was kept: <a href="/~mossfield/mapkey.txt">mapkey.txt</a></i></p>
      <pre className="plain-pre">{mapKeyText(b)}</pre>
    </Plain>
  );
}

function LogBlockLocked() {
  return (
    <Plain title="LogBlock">
      <h1>LogBlock web viewer</h1>
      <p><b>Error:</b> could not open lb-mossfield: table is locked by another process (mrrw restore job). Try again later.</p>
    </Plain>
  );
}

const PAGE = 100;

function LogBlockViewer({ b, q }: { b: Bundle; q: Record<string, string | undefined> }) {
  const player = (q.player ?? "").trim().toLowerCase();
  const num = (v?: string) => (v && v.trim() !== "" && Number.isFinite(Number(v)) ? Number(v) : null);
  const x = num(q.x), y = num(q.y), z = num(q.z);
  // LogBlock's "area" default; a blank y searches the whole column (towers, cellars), like `lb lookup area 32`.
  const radius = Math.min(Math.max(num(q.radius) ?? 32, 0), 64);
  const since = q.since ?? "", until = q.until ?? "";
  const searched = Boolean(player || x !== null || z !== null || since);
  let rows: LogRow[] = [];
  if (searched) {
    rows = b.logblock.filter((r) =>
      (!player || r.player.toLowerCase() === player) &&
      (x === null || Math.abs(r.x - x) <= radius) &&
      (y === null || Math.abs(r.y - y) <= radius) &&
      (z === null || Math.abs(r.z - z) <= radius) &&
      (!since || r.date >= since) && (!until || r.date <= `${until} 23:59:59`));
  }
  const page = Math.max(Number(q.page ?? 1) || 1, 1);
  const shown = rows.slice((page - 1) * PAGE, page * PAGE);
  const qs = (p: number) => new URLSearchParams({ ...Object.fromEntries(Object.entries(q).filter(([, v]) => v)) as Record<string, string>, page: String(p) }).toString();
  return (
    <Plain title="LogBlock">
      <h1>LogBlock web viewer</h1>
      <p><small>lb-mossfield · world · times are server time (Chicago)</small></p>
      <form method="get" className="plain-form">
        player <input name="player" defaultValue={q.player ?? ""} size={12} />{" "}
        x <input name="x" defaultValue={q.x ?? ""} size={5} /> y <input name="y" defaultValue={q.y ?? ""} size={4} /> z <input name="z" defaultValue={q.z ?? ""} size={5} />{" "}
        radius <input name="radius" defaultValue={String(radius)} size={3} />{" "}
        since <input name="since" defaultValue={since} size={10} /> until <input name="until" defaultValue={until} size={10} />{" "}
        <button type="submit">search</button>
      </form>
      {searched ? (
        <>
          <p>{rows.length} rows{rows.length > PAGE ? ` · page ${page} of ${Math.ceil(rows.length / PAGE)}` : ""}</p>
          <table className="plain-table">
            <thead><tr><th>date</th><th>player</th><th>action</th><th>block</th><th>x</th><th>y</th><th>z</th><th></th></tr></thead>
            <tbody>{shown.map((r, i) => <tr key={i}><td>{r.date}</td><td>{r.player}</td><td>{r.action}</td><td>{r.block}</td><td>{r.x}</td><td>{r.y}</td><td>{r.z}</td><td>{r.extra}</td></tr>)}</tbody>
          </table>
          {rows.length > page * PAGE ? <p><a href={`?${qs(page + 1)}`}>next page »</a></p> : null}
        </>
      ) : <p>enter a player or a position. leave y blank to search the whole column.</p>}
    </Plain>
  );
}

function Logs({ b, day }: { b: Bundle; day?: string }) {
  const days = Object.keys(b.server_logs).sort();
  if (day && b.server_logs[day]) {
    return <Plain title={`server.log ${day}`}><pre className="plain-pre">{b.server_logs[day]}</pre></Plain>;
  }
  return (
    <Plain title="logs">
      <h1>server logs</h1>
      <p>rook keeps the interesting days. chat only, no /msg (privacy).</p>
      <ul>{days.map((d) => <li key={d}><Link href={`/~mossfield/logs/?day=${d}`}>server-{d}.log</Link></li>)}</ul>
    </Plain>
  );
}

function ApacheIndex({ title, rows }: { title: string; rows: { name: string; href?: string; mtime: string; size: string }[] }) {
  return (
    <Plain title={`Index of ${title}`}>
      <h1>Index of {title}</h1>
      <table className="plain-table">
        <thead><tr><th>Name</th><th>Last modified</th><th>Size</th></tr></thead>
        <tbody>{rows.map((r) => (
          <tr key={r.name}><td>{r.href ? <a href={r.href}>{r.name}</a> : r.name}</td><td>{r.mtime}</td><td>{r.size}</td></tr>
        ))}</tbody>
      </table>
      <hr /><address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address>
    </Plain>
  );
}

function JuneIndex({ b }: { b: Bundle }) {
  return (
    <Plain title="Index of /~juni_lantern/screenshots">
      <h1>Index of /~juni_lantern/screenshots</h1>
      <table className="plain-table">
        <thead><tr><th>Name</th><th>Last modified</th><th>Size</th></tr></thead>
        <tbody>{b.june_shots.map((s) => (
          <tr key={s.filename}>
            <td><a href={`/media/june/${s.filename}`}>{s.filename}</a></td>
            <td>{s.server_mtime ?? ""}</td><td>{s.server_mtime ? "1.1M" : "980K"}</td>
          </tr>
        ))}</tbody>
      </table>
      <hr /><address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address>
    </Plain>
  );
}
