import type { Metadata } from "next";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import type { HealthView } from "@/lib/database.types";

export const metadata: Metadata = { title: "Network status — SNOIKERZ", description: "Mirror 03 public status cache." };
export const dynamic = "force-dynamic";

export default async function StatusPage() {
  const supabase = await createClient();
  const result = await supabase.from("v_health").select("*").maybeSingle();
  const health = result.data as HealthView | null;
  const mirrorReachable = !result.error;
  const lastContact = health?.last_beat_at
    ? new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short", timeZone: "UTC" }).format(new Date(health.last_beat_at)) + " UTC"
    : null;
  const recentErrors = health?.error_24h ?? 0;

  return (
    <main className="status-site">
      <header className="status-header">
        <Link href="/" className="host-brand"><span>SNOIKERZ</span><small>network operations</small></Link>
        <span>PUBLIC STATUS CACHE / MIRROR 03</span>
      </header>
      <div className="status-wrap">
        <div className="status-title"><p className="eyebrow">Archived infrastructure</p><h1>Network status</h1><p>This page distinguishes the surviving web mirror from the Minecraft host. A working archive does not mean the world is online.</p></div>
        <section className={`status-banner ${mirrorReachable ? "nominal" : "outage"}`}>
          <i />
          <div><strong>{mirrorReachable ? "Mirror responding" : "Mirror data unavailable"}</strong><span>{mirrorReachable ? "The recovered listing and public record can be read." : "The public cache could not be queried."}</span></div>
        </section>
        <section className="status-services">
          <div><span className={mirrorReachable ? "status-dot up" : "status-dot down"} /><div><strong>Web mirror 03</strong><small>listing, files, public record</small></div><b>{mirrorReachable ? "operational" : "unavailable"}</b></div>
          <div><span className={`status-dot ${lastContact ? "unknown" : "down"}`} /><div><strong>Observance game host</strong><small>{lastContact ? `last recorded contact ${lastContact}` : "no world contact retained in the public cache"}</small></div><b>{lastContact ? "unverified" : "no signal"}</b></div>
          <div><span className="status-dot unknown" /><div><strong>Staff control account</strong><small>removed from the public listing</small></div><b>unknown</b></div>
        </section>
        {recentErrors > 0 ? <p className="status-footnote">The mirror recorded {recentErrors} service {recentErrors === 1 ? "error" : "errors"} in the last 24 hours.</p> : <p className="status-footnote">No mirror service errors are present in the last 24-hour cache.</p>}
        <Link href="/" className="back-link">← return to server listing</Link>
      </div>
    </main>
  );
}
