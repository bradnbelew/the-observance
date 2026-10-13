import { createReadStream } from "node:fs";
import { readdir, stat } from "node:fs/promises";
import path from "node:path";
import { Readable } from "node:stream";

/**
 * Streams a private-media file instead of buffering it into one Response, so large recordings
 * and zips aren't capped by the function response-body limit. Supports single byte ranges
 * (audio seeking in <audio>). Node runtime only. Returns null when the file doesn't exist.
 */
export const MEDIA_ROOT = path.join(process.cwd(), "private-media");

export const TYPES: Record<string, string> = {
  pdf: "application/pdf", txt: "text/plain; charset=utf-8", log: "text/plain; charset=utf-8", yml: "text/plain; charset=utf-8",
  eml: "message/rfc822", zip: "application/zip", jpg: "image/jpeg", jpeg: "image/jpeg", png: "image/png",
  ogg: "audio/ogg", wav: "audio/wav", mp3: "audio/mpeg", m4a: "audio/mp4",
};

export const typeOf = (name: string) => TYPES[name.split(".").pop()?.toLowerCase() ?? ""] ?? "application/octet-stream";

/** Finds `name` in `dir` ignoring case (owner media may be saved as mic-5-18-b.wav on Windows; Vercel's disk is case-sensitive). */
export async function resolveFile(dir: string[], name: string): Promise<string | null> {
  const folder = path.join(MEDIA_ROOT, ...dir);
  const exact = path.join(folder, name);
  try {
    if ((await stat(exact)).isFile()) return exact;
  } catch {
    /* fall back to a case-insensitive match */
  }
  try {
    const hit = (await readdir(folder)).find((f) => f.toLowerCase() === name.toLowerCase());
    return hit ? path.join(folder, hit) : null;
  } catch {
    return null;
  }
}

export async function serveFile(request: Request, file: string, headers: Record<string, string>): Promise<Response | null> {
  let size: number;
  try {
    const s = await stat(file);
    if (!s.isFile()) return null;
    size = s.size;
  } catch {
    return null;
  }
  const base: Record<string, string> = { ...headers, "Accept-Ranges": "bytes" };
  const range = /^bytes=(\d*)-(\d*)$/.exec(request.headers.get("range")?.trim() ?? "");
  if (range && (range[1] || range[2]) && size > 0) {
    const start = range[1] ? Number(range[1]) : Math.max(size - Number(range[2]), 0);
    const end = range[1] && range[2] ? Math.min(Number(range[2]), size - 1) : size - 1;
    if (start > end || start >= size) {
      return new Response(null, { status: 416, headers: { ...base, "Content-Range": `bytes */${size}` } });
    }
    return new Response(stream(file, start, end), {
      status: 206, headers: { ...base, "Content-Range": `bytes ${start}-${end}/${size}`, "Content-Length": String(end - start + 1) },
    });
  }
  return new Response(size ? stream(file) : null, { headers: { ...base, "Content-Length": String(size) } });
}

function stream(file: string, start?: number, end?: number): ReadableStream<Uint8Array> {
  return Readable.toWeb(createReadStream(file, start === undefined ? undefined : { start, end })) as unknown as ReadableStream<Uint8Array>;
}
