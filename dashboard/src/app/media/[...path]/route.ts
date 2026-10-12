import { readFile } from "node:fs/promises";
import path from "node:path";
import { bundle, currentAct } from "@/lib/morrow/canon";

export const dynamic = "force-dynamic";

/**
 * Residents' files. june and patchcord are behind Basic auth in proxy.ts; tvale is Theo's open
 * staff space. Owner-made media lives in private-media/media/<folder>/, generated files in private-media/files/.
 */
const FOLDERS: Record<string, { act: number; dir: string[] }> = {
  june: { act: 3, dir: ["media", "june"] },
  patchcord: { act: 5, dir: ["media", "patchcord"] },
  tvale: { act: 6, dir: ["files"] },
};
const TYPES: Record<string, string> = { png: "image/png", jpg: "image/jpeg", ogg: "audio/ogg", wav: "audio/wav", mp3: "audio/mpeg", txt: "text/plain; charset=utf-8" };

export async function GET(_req: Request, { params }: { params: Promise<{ path: string[] }> }) {
  const [folder, file] = (await params).path;
  const f = FOLDERS[folder];
  if (!f || !file || !/^[\w.-]+$/.test(file) || (await currentAct()) < f.act) return new Response("Not Found", { status: 404 });
  if (folder === "tvale" && !(await bundle()).tvale_folder.some((x) => x.file === file)) return new Response("Not Found", { status: 404 });
  try {
    const data = await readFile(path.join(process.cwd(), "private-media", ...f.dir, file));
    return new Response(new Uint8Array(data), { headers: { "Content-Type": TYPES[file.split(".").pop() ?? ""] ?? "application/octet-stream" } });
  } catch {
    return new Response("Not Found", { status: 404 });
  }
}
