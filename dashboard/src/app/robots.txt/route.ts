import { bundle } from "@/lib/morrow/canon";

export const dynamic = "force-dynamic";

export async function GET() {
  const b = await bundle();
  return new Response(b.robots_txt, { headers: { "Content-Type": "text/plain; charset=utf-8" } });
}
