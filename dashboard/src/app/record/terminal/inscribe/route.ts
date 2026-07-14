import { NextResponse } from 'next/server';

/**
 * Retired V4 inscription endpoint.
 *
 * V5 has no authenticated player session on the website. Accepting a typed Minecraft name here would
 * let any visitor impersonate a player and use the service-role resolver to complete Discord-owned or
 * physical nodes. The six V5 website nodes are handled only by their fixed Copperline routes through
 * `recordV5WebSequence`, whose exact allow-list is separately self-tested.
 */

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(): Promise<NextResponse> {
  return NextResponse.json(
    { outcome: 'unresolved', line: 'this docket is read-only.' },
    { status: 410, headers: { 'Cache-Control': 'no-store' } },
  );
}
