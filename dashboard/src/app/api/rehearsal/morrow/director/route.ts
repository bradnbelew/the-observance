import { NextResponse } from 'next/server';
import { directorLockContract } from '@/lib/morrow-full-rehearsal';

export const dynamic = 'force-static';

export function GET() {
  return NextResponse.json({
    schemaVersion: '1.0.0-morrow-director-local-lock',
    status: 'director_controls_disabled',
    productionMutation: false,
    contract: directorLockContract(),
  }, {
    headers: { 'Cache-Control': 'no-store' },
  });
}

export async function POST(request: Request) {
  let attemptedCommand: unknown = null;

  try {
    attemptedCommand = await request.json();
  } catch {
    attemptedCommand = null;
  }

  return NextResponse.json({
    error: 'director_controls_disabled',
    status: 'locked',
    productionMutation: false,
    attemptedCommand,
    contract: directorLockContract(),
  }, { status: 423 });
}
