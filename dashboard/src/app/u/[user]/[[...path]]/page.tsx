import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { bundle, currentAct, type Bundle, type LogRow } from "@/lib/morrow/canon";

/**
 * Copperline's free customer web space, served at /~name/ (rewritten to /u/name/).
 * Pages here are plain 2011 hand-made HTML, not the Copperline template.
 */
export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { user, path = [] } = await params;
  const sub = path.join("/");
  if (user === "mossfield") {
    return { title: ({ "": "mossfield", dynmap: "Minecraft Dynamic Map", logblock: "LogBlock", logs: "logs" } as Record<string, string>)[sub] ?? "404 Not Found" };
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
  const act = currentAct();
  const sub = path.join("/");

  if (user === "mossfield") {
    if (sub === "") return <MossfieldHome />;
    if (sub === "dynmap") return act >= 2 ? <Dynmap b={b} /> : <ApacheMissing path="/~mossfield/dynmap/" />;
    if (sub === "logblock") return act >= 3 ? <LogBlockViewer b={b} q={q} /> : <LogBlockLocked />;
    if (sub === "logs") return <Logs b={b} day={q.day} />;
    notFound();
  }
  if (user === "juni_lantern" && act >= 3) {
    if (sub === "" || sub === "screenshots") return <JuneIndex b={b} />;
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

function MossfieldHome() {
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
      <p><small>last updated 22 feb 2011. new map!!</small></p>
    </Plain>
  );
}

function Dynmap({ b }: { b: Bundle }) {
  return (
    <Plain title="Minecraft Dynamic Map">
      <h1>Dynmap 0.12 - world</h1>
      <p><i>Map tiles were not retained in this mirror. The marker set was kept (standalone/markers.json).</i></p>
      <table className="plain-table">
        <thead><tr><th>marker</th><th>type</th><th>x</th><th>y</th><th>z</th></tr></thead>
        <tbody>{b.dynmap_markers.map((m) => <tr key={m.label}><td>{m.label}</td><td>{m.type}</td><td>{m.x}</td><td>{m.y}</td><td>{m.z}</td></tr>)}</tbody>
      </table>
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
  const x = q.x ? Number(q.x) : null, y = q.y ? Number(q.y) : null, z = q.z ? Number(q.z) : null;
  const radius = Math.min(Math.max(Number(q.radius ?? 5) || 5, 0), 64);
  const since = q.since ?? "", until = q.until ?? "";
  const searched = Boolean(player || x !== null || since);
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
      ) : <p>enter a player or a position.</p>}
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
