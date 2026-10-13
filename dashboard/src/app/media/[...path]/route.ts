import { bundle, currentAct } from "@/lib/morrow/canon";
import { resolveFile, serveFile, typeOf } from "@/lib/morrow/serve";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/**
 * Residents' files. june and patchcord are behind Basic auth in proxy.ts; tvale is Theo's open
 * staff space. Owner-made media lives in private-media/media/<folder>/, generated files in private-media/files/.
 * Streamed with byte ranges so long recordings seek and aren't capped by the response-body limit.
 */
const FOLDERS: Record<string, { act: number; dir: string[] }> = {
  june: { act: 3, dir: ["media", "june"] },
  patchcord: { act: 5, dir: ["media", "patchcord"] },
  tvale: { act: 6, dir: ["files"] },
  tvalebox: { act: 8, dir: ["files"] },
};

export async function GET(request: Request, { params }: { params: Promise<{ path: string[] }> }) {
  const [folder, name] = (await params).path;
  const f = FOLDERS[folder];
  if (!f || !name || !/^[\w.-]+$/.test(name) || (await currentAct()) < f.act) return new Response("Not Found", { status: 404 });
  if (folder === "tvale" && !(await bundle()).tvale_folder.some((x) => x.file === name)) return new Response("Not Found", { status: 404 });
  if (folder === "tvalebox" && !(await bundle()).tvale_box.includes(name)) return new Response("Not Found", { status: 404 });
  const file = await resolveFile(f.dir, name);
  const res = file && await serveFile(request, file, { "Content-Type": typeOf(name) });
  return res || new Response("Not Found", { status: 404 });
}
