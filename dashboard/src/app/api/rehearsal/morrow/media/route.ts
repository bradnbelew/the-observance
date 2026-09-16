import { NextResponse } from 'next/server';
import { mediaIntakeSummary } from '@/lib/morrow-full-rehearsal';

export const dynamic = 'force-static';

export function GET() {
  return NextResponse.json(mediaIntakeSummary(), {
    headers: { 'Cache-Control': 'no-store' },
  });
}
