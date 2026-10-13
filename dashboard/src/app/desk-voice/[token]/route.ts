import { unseal } from "@/lib/morrow/campaign";
import { resolveFile, serveFile, typeOf } from "@/lib/morrow/serve";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/** Theo's voice notes: private-media/theo/<ID>.(ogg|mp3|m4a|wav). Missing recordings answer 404 and the page still shows the text. */
export async function GET(request: Request, { params }: { params: Promise<{ token: string }> }) {
  const p = unseal<{ v: string }>((await params).token);
  if (!p || !/^[\w-]+$/.test(p.v)) return new Response("Not Found", { status: 404 });
  for (const ext of ["ogg", "mp3", "m4a", "wav"]) {
    const name = `${p.v}.${ext}`;
    const file = await resolveFile(["theo"], name);
    const res = file && await serveFile(request, file, { "Content-Type": typeOf(name), "Cache-Control": "private, max-age=3600" });
    if (res) return res;
  }
  return new Response("Not Found", { status: 404 });
}
