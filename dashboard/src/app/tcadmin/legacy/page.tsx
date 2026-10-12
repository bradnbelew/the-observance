import type { Metadata } from "next";
import { cookies } from "next/headers";
import { bundle, currentAct } from "@/lib/morrow/canon";
import { unseal } from "@/lib/morrow/campaign";

export const metadata: Metadata = { title: "TCAdmin - Game Panel" };
export const dynamic = "force-dynamic";

type Result = { code?: string; ticket?: string; error?: string; status?: string };

/**
 * TCAdmin 1 legacy panel.
 *   Act 1-5: locked by the running recovery job.
 *   Act 6+:  Rookery's client login opens it: the CHI-07/mrrw service log (Theo's 06:44:10 restart).
 *   Act 10:  maintenance booking and the stop console.
 */
export default async function TcAdmin({ searchParams }: { searchParams: Promise<{ tried?: string; r?: string }> }) {
  const sp = await searchParams;
  const act = await currentAct();
  const b = await bundle();
  const user = unseal<{ u: string }>((await cookies()).get("tcadmin")?.value);
  const result = unseal<Result>(sp.r);

  if (!user || act < 6) {
    return (
      <Frame>
        {sp.tried ? <p className="tc-err">{act < 6 ? "Service CHI-07/mrrw is locked by a running recovery job. Panel access is disabled until the job completes." : "Invalid username or password."}</p> : null}
        <form method="post" action="/tcadmin/legacy/login" className="plain-form">
          <p>Username<br /><input name="u" autoComplete="off" /></p>
          <p>Password<br /><input name="p" type="password" /></p>
          <button type="submit">Login</button>
        </form>
        <p className="tc-small">Copperline Hosting legacy panel. Read only.</p>
      </Frame>
    );
  }

  return (
    <Frame wide>
      <p className="tc-small">logged in: {user.u} · <a href="/tcadmin/legacy/login?logout=1">logout</a></p>
      <h2>Service CHI-07/mrrw</h2>
      <table className="plain-table"><tbody>
        <tr><td>Node</td><td>CHI-07 (retained host)</td></tr>
        <tr><td>Process</td><td>mrrw-01</td></tr>
        <tr><td>Auto restart</td><td>On (set 2014-12-14 by tvale)</td></tr>
      </tbody></table>
      <h2>Service log</h2>
      <table className="plain-table">
        <thead><tr><th>Time</th><th>User</th><th>Service</th><th>Action</th><th>Reason</th></tr></thead>
        <tbody>{(b.tcadmin_log ?? []).map((row, i) => <tr key={i}>{row.map((c, j) => <td key={j}>{c}</td>)}</tr>)}</tbody>
      </table>
      {act >= 10 ? (
        <>
          <h2>Maintenance window</h2>
          <form method="post" action="/tcadmin/legacy/book" className="plain-form">
            <p>Requested start (your time)<br /><input name="when" type="datetime-local" required /></p>
            <button type="submit">Request window</button>
          </form>
          <h2>Console</h2>
          {result?.error ? <p className="tc-err">{result.error}</p> : null}
          {result?.status ? <p className="tc-small">node reports: <code>{result.status}</code></p> : null}
          {result?.code ? (
            <div className="tc-ok">
              <p><b>Stop accepted by the panel.</b> mrrw must confirm on the node itself.</p>
              <p>Enter this at the maintenance console in Mossfield: <code>{result.code}</code></p>
              <p className="tc-small">Valid for about thirty minutes.</p>
              {result.ticket ? <p><a href={result.ticket}>The archive desk has a reply for you.</a></p> : null}
            </div>
          ) : (
            <form method="post" action="/tcadmin/legacy/console" className="plain-form">
              <p>Command<br /><input name="command" autoComplete="off" /></p>
              <p>Key<br /><input name="key" autoComplete="off" size={48} /></p>
              <p>Before stopping, retain one image to removable media:<br />
                {Object.entries(b.usb_choices ?? {}).map(([k, v]) => (
                  <label key={k} className="tc-choice"><input type="radio" name="choice" value={k} required /> {v.line}</label>
                ))}
              </p>
              <button type="submit">Run</button>
            </form>
          )}
        </>
      ) : null}
    </Frame>
  );
}

function Frame({ children, wide }: { children: React.ReactNode; wide?: boolean }) {
  return (
    <div className="plain-2011 tc-page">
      <div className={wide ? "tc-box tc-wide" : "tc-box"}>
        <h1>TCAdmin 1.0 - Game Panel</h1>
        {children}
      </div>
    </div>
  );
}
