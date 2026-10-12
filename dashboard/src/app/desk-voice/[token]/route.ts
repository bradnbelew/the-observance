import { readFile } from "node:fs/promises";
import path from "node:path";
import { unseal } from "@/lib/morrow/campaign";

export const dynamic = "force-dynamic";

/** Theo's voice notes: private-media/theo/<ID>.(ogg|mp3|m4a|wav). Missing recordings answer 404 and the page still shows the text. */
export async function GET(_req: Request, { params }: { params: Promise<{ token: string }> }) {
  const p = unseal<{ v: string }>((await params).token);
  if (!p || !/^[\w-]+$/.test(p.v)) return new Response("Not Found", { status: 404 });
  for (const [ext, type] of [["ogg", "audio/ogg"], ["mp3", "audio/mpeg"], ["m4a", "audio/mp4"], ["wav", "audio/wav"]] as const) {
    try {
      const data = await readFile(path.join(process.cwd(), "private-media", "theo", `${p.v}.${ext}`));
      return new Response(new Uint8Array(data), { headers: { "Content-Type": type, "Cache-Control": "private, max-age=3600" } });
    } catch {
      /* try the next extension */
    }
  }
  return new Response("Not Found", { status: 404 });
}
