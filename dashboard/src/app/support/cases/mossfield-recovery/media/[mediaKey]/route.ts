import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { readMorrowCase } from '@/lib/morrow-copperline-server';

export const dynamic = 'force-dynamic';

export async function GET(
  request: Request,
  { params }: { params: Promise<{ mediaKey: string }> },
) {
  const { mediaKey } = await params;
  const context = await readMorrowCase();
  if (context.kind !== 'ready') return notFound();
  const asset = context.media.find((candidate) => candidate.key === mediaKey);
  if (!asset) return notFound();

  const format = new URL(request.url).searchParams.get('format');
  if (format === 'audio') {
    if (!asset.audio_file || !asset.audio_sha256) return notFound();
    const allowedFile = asset.audio_file === 'm03-captioned-voice-fragment.ogg'
      || asset.audio_file === 'm11-current-voice-assembly.ogg';
    if (!allowedFile) return notFound();
    try {
      const payload = await readFile(join(process.cwd(), 'src', 'lib', 'morrow-media-assets', asset.audio_file));
      const actual = createHash('sha256').update(payload).digest('hex');
      if (actual !== asset.audio_sha256) return new Response('Media custody mismatch', { status: 503 });
      return new Response(payload, {
        headers: {
          'Content-Type': 'audio/ogg',
          'Content-Length': String(payload.length),
          'Cache-Control': 'private, no-store',
          'Content-Disposition': `inline; filename="${asset.audio_file}"`,
          'X-Content-Type-Options': 'nosniff',
        },
      });
    } catch {
      return new Response('Media temporarily unavailable', { status: 503 });
    }
  }

  return Response.json({
    schemaVersion: '1.0.0-morrow-earned-media',
    releaseId: context.releaseId,
    asset,
  }, { headers: { 'Cache-Control': 'private, no-store', 'X-Content-Type-Options': 'nosniff' } });
}

function notFound(): Response {
  return new Response('Not found', {
    status: 404,
    headers: { 'Cache-Control': 'private, no-store', 'X-Content-Type-Options': 'nosniff' },
  });
}
