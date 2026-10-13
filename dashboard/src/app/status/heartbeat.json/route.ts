import { bundle, currentAct } from "@/lib/morrow/canon";
import { morrowStatus, ping } from "@/lib/morrow/slp";

export const dynamic = "force-dynamic";

/**
 * The retained heartbeat check. Through Act 9 it reports the canon record. In Act 10 it asks the
 * node itself, the same way status/page.tsx does: once the server answers the list ping with no
 * Morrow band in its MOTD, the process has stopped and there is no heartbeat.
 */
export async function GET() {
  const b = await bundle();
  const checked = new Date().toISOString();
  if ((await currentAct()) >= 10) {
    const status = await ping();
    if (status && morrowStatus(status) === "") {
      return Response.json({ ...b.heartbeat, state: "stopped", heartbeat: "none", checked }, { headers: { "Cache-Control": "no-store" } });
    }
  }
  return Response.json({ ...b.heartbeat, checked }, { headers: { "Cache-Control": "no-store" } });
}
