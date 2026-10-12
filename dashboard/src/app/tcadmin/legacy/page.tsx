import type { Metadata } from "next";
import { currentAct } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "TCAdmin - Login" };
export const dynamic = "force-dynamic";

/** TCAdmin 1 legacy panel. Locked by the running recovery job until Act 6. */
export default async function TcAdminLegacy({ searchParams }: { searchParams: Promise<{ tried?: string }> }) {
  const tried = (await searchParams).tried;
  const locked = currentAct() < 6;
  return (
    <div className="plain-2011" style={{ background: "#e8ecef", fontFamily: "Tahoma, sans-serif", fontSize: 12 }}>
      <div style={{ maxWidth: 360, margin: "60px auto", background: "#fff", border: "1px solid #8a9aa6", padding: 16 }}>
        <h1 style={{ fontSize: 16, margin: "0 0 10px" }}>TCAdmin 1.0 - Game Panel</h1>
        {tried ? <p style={{ color: "#b00" }}>{locked ? "Service CHI-07/mrrw is locked by a running recovery job. Panel access is disabled until the job completes." : "Invalid username or password."}</p> : null}
        <form method="post" action="/tcadmin/legacy/login" className="plain-form">
          <p>Username<br /><input name="u" autoComplete="off" /></p>
          <p>Password<br /><input name="p" type="password" /></p>
          <button type="submit">Login</button>
        </form>
        <p style={{ color: "#777", fontSize: 10 }}>Copperline Hosting legacy panel. Read only.</p>
      </div>
    </div>
  );
}
