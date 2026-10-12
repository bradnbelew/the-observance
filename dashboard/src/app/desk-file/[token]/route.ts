import { readFile } from "node:fs/promises";
import path from "node:path";
import { unseal } from "@/lib/morrow/campaign";

export const dynamic = "force-dynamic";

const TYPES: Record<string, string> = { pdf: "application/pdf", txt: "text/plain; charset=utf-8", log: "text/plain; charset=utf-8",
  yml: "text/plain; charset=utf-8", eml: "message/rfc822", zip: "application/zip", jpg: "image/jpeg", ogg: "audio/ogg", wav: "audio/wav" };

/** Files Theo hands over. Only reachable through a link the desk signed. */
export async function GET(_req: Request, { params }: { params: Promise<{ token: string }> }) {
  const p = unseal<{ f: string }>((await params).token);
  if (!p || !/^[\w.-]+$/.test(p.f)) return new Response("Not Found", { status: 404 });
  try {
    const data = await readFile(path.join(process.cwd(), "private-media", "files", p.f));
    const ext = p.f.split(".").pop() ?? "";
    return new Response(new Uint8Array(data), { headers: { "Content-Type": TYPES[ext] ?? "application/octet-stream",
      "Content-Disposition": `${ext === "jpg" || ext === "txt" || ext === "log" ? "inline" : "attachment"}; filename="${p.f}"` } });
  } catch {
    return new Response("Not Found", { status: 404 });
  }
}
