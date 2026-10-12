import { bundle } from "@/lib/morrow/canon";

export const dynamic = "force-dynamic";

/** The raw export keeps the fields the public prose lost (uptime, node). */
export async function GET() {
  const b = await bundle();
  return new Response(b.status_incident.raw_export, { headers: { "Content-Type": "text/csv; charset=utf-8" } });
}
