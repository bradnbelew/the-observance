import { NextResponse } from 'next/server';
import { hasCampaignEvent } from '@/lib/arg-event-store';
import { readValidatedV5HoldArchive } from '@/lib/v5-hold-archive';
import { readV5CompletionFlag, recordV5WebSequence } from '@/lib/v5-web-progress';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

/** Durable website ownership marker consumed by cross-surface binding audits. */
export const LS04_WORLD_DOWNLOAD_BINDING = {
  nodeIds: ['LS04'],
  puzzleKey: 'copperline_world_backup',
  handler: 'world_download',
} as const;

const NO_STORE_HEADERS = {
  'Cache-Control': 'private, no-store, max-age=0, must-revalidate',
  Pragma: 'no-cache',
  'X-Content-Type-Options': 'nosniff',
} as const;

function genericNotFound(): NextResponse {
  return new NextResponse('Not Found', {
    status: 404,
    headers: NO_STORE_HEADERS,
  });
}

export async function GET(): Promise<NextResponse> {
  const prerequisite = await hasCampaignEvent('p2.artifact_authenticated');
  if (prerequisite !== true) return genericNotFound();
  const directoryTrail = await readV5CompletionFlag('v5_ls03_directory_trail');
  if (!directoryTrail.complete) return genericNotFound();

  const handoff = await recordV5WebSequence(
    ['LS04'],
    'hold-world-download',
    { artifact: 'the-hold.zip', handler: 'world_download', custody: 'copperline_world_backup' },
  );
  if (!handoff.complete) return genericNotFound();

  const archive = await readValidatedV5HoldArchive();
  if (!archive) return genericNotFound();

  return new NextResponse(new Uint8Array(archive.bytes), {
    status: 200,
    headers: {
      ...NO_STORE_HEADERS,
      'Content-Disposition': 'attachment; filename="the-hold.zip"',
      'Content-Length': String(archive.bytes.byteLength),
      'Content-Type': 'application/zip',
    },
  });
}
