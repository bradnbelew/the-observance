import { NextResponse } from 'next/server';
import { mediaRequirements } from '@/lib/morrow-full-rehearsal';

export const dynamic = 'force-static';

export function GET() {
  const assets = mediaRequirements();
  const enrichedAssets = assets.map((asset) => ({
    ...asset,
    workFolder: `morrow/media/work/${asset.key}`,
    deliveryRoot: 'morrow/media/final',
    accessibilityRoot: 'morrow/media/accessibility',
    releaseReady: false,
    requiredBeforeRelease: [
      'source_file',
      'delivery_file',
      'source_sha256',
      'delivery_sha256',
      'custody_note',
      'created_at',
      'author_identity',
      'accessibility_equivalent_file',
      'safety_review',
    ],
  }));

  return NextResponse.json({
    schemaVersion: '1.1.0-morrow-media-readiness',
    status: 'media_assets_required_before_full_release',
    productionMutation: false,
    intakePolicy: {
      manifest: 'morrow/media/media-manifest.template.json',
      hashAlgorithm: 'sha256',
      workRoot: 'morrow/media/work',
      deliveryRoot: 'morrow/media/final',
      accessibilityRoot: 'morrow/media/accessibility',
      releaseReadyRequiresAllHashes: true,
      releaseReadyRequiresAccessibilityEquivalent: true,
      releaseReadyRequiresSafetyReview: true,
    },
    totalAssets: assets.length,
    releaseBlockingAssets: assets.filter((asset) => asset.releaseBlocking).length,
    assets: enrichedAssets,
  }, {
    headers: { 'Cache-Control': 'no-store' },
  });
}
