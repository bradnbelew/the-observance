import { NextResponse } from 'next/server';
import { buildFullRehearsalReceipt, fullRehearsalDigest } from '@/lib/morrow-full-rehearsal';

export const dynamic = 'force-static';

export function GET() {
  return NextResponse.json({ ...buildFullRehearsalReceipt(), digest: fullRehearsalDigest() }, {
    headers: { 'Cache-Control': 'no-store' },
  });
}
