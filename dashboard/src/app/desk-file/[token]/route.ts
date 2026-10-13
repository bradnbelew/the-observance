import { unseal } from "@/lib/morrow/campaign";
import { resolveFile, serveFile, typeOf } from "@/lib/morrow/serve";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/** Files Theo hands over. Only reachable through a link the desk signed. Streamed (recordings can be large). */
export async function GET(request: Request, { params }: { params: Promise<{ token: string }> }) {
  const p = unseal<{ f: string }>((await params).token);
  if (!p || !/^[\w.-]+$/.test(p.f)) return new Response("Not Found", { status: 404 });
  const file = await resolveFile(["files"], p.f);
  const ext = p.f.split(".").pop()?.toLowerCase() ?? "";
  const inline = ["jpg", "jpeg", "png", "txt", "log", "ogg", "wav", "mp3"].includes(ext);
  const res = file && await serveFile(request, file, {
    "Content-Type": typeOf(p.f),
    "Content-Disposition": `${inline ? "inline" : "attachment"}; filename="${p.f}"`,
    "Cache-Control": "private, max-age=3600",
  });
  return res || new Response("Not Found", { status: 404 });
}
