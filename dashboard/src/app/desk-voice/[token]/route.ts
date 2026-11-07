import { notFoundPage } from "@/lib/morrow/http";
import { unseal } from "@/lib/morrow/campaign";
import { serveFile, typeOf, voiceFileFor } from "@/lib/morrow/serve";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/** Theo's voice notes: private-media/theo/<ID>.(ogg|mp3|m4a|wav). Missing recordings answer 404 and the page still shows the text. */
export async function GET(request: Request, { params }: { params: Promise<{ token: string }> }) {
  const p = unseal<{ v: string }>((await params).token);
  if (!p || !/^[\w-]+$/.test(p.v)) return notFoundPage(request);
  const v = await voiceFileFor(p.v);
  const res = v && await serveFile(request, v.file, { "Content-Type": typeOf(v.name), "Cache-Control": "private, max-age=3600" });
  return res || notFoundPage(request);
}
