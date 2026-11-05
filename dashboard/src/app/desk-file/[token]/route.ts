import { unseal } from "@/lib/morrow/campaign";
import { resolveFile, serveFile, typeOf } from "@/lib/morrow/serve";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/** Files Theo hands over. Only reachable through a link the desk signed. Streamed (recordings can be large). */
export async function GET(request: Request, { params }: { params: Promise<{ token: string }> }) {
  const p = unseal<{ f: string }>((await params).token);
  if (!p || !/^[\w.-]+$/.test(p.f)) return new Response("Not Found", { status: 404 });
  let name = p.f;
  let file = await resolveFile(["files"], name);
  if (!file && /\.(ogg|wav|mp3)$/i.test(name)) {  // a recording not made yet: patch's typed notes on it stand in
    const notes = name.replace(/\.[a-z0-9]+$/i, ".txt");
    file = await resolveFile(["files"], notes);
    if (file) name = notes;
  }
  const ext = name.split(".").pop()?.toLowerCase() ?? "";
  const inline = ["jpg", "jpeg", "png", "txt", "log", "ogg", "wav", "mp3"].includes(ext);
  const res = file && await serveFile(request, file, {
    "Content-Type": typeOf(name),
    "Content-Disposition": `${inline ? "inline" : "attachment"}; filename="${name}"`,
    "Cache-Control": "private, max-age=3600",
  });
  return res || new Response("Not Found", { status: 404 });
}
