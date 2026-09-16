import { NextResponse } from 'next/server';
import { mediaRequirements } from '@/lib/morrow-full-rehearsal';

export const dynamic = 'force-static';

export function GET() {
  const assets = mediaRequirements();

  return NextResponse.json({
    schemaVersion: '1.0.0-morrow-media-readiness',
    status: 'media_assets_required_before_full_release',
    productionMutation: false,
    totalAssets: assets.length,
    releaseBlockingAssets: assets.filter((asset) => asset.releaseBlocking).length,
    assets,
  }, {
    headers: { 'Cache-Control': 'no-store' },
  });
}
