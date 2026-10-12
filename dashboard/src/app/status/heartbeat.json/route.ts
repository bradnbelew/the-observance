import { bundle } from "@/lib/morrow/canon";

export const dynamic = "force-dynamic";

export async function GET() {
  const b = await bundle();
  return Response.json({ ...b.heartbeat, checked: new Date().toISOString() });
}
