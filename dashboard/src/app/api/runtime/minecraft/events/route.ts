import { NextResponse } from 'next/server';
import { verifyMorrowEnvelope } from '@/lib/morrow-runtime-envelope';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

type RpcRow = { status: 'committed' | 'duplicate' | 'collision' | 'blocked'; created: boolean; event_id: string | null };

export async function POST(request: Request): Promise<NextResponse> {
  const secret = process.env.MORROW_MINECRAFT_INGEST_SECRET ?? '';
  const rawBody = await request.text();
  const verified = verifyMorrowEnvelope({
    rawBody,
    timestamp: request.headers.get('x-morrow-timestamp'),
    signature: request.headers.get('x-morrow-signature'),
    secret,
  });
  if (!verified.ok) {
    return NextResponse.json({ status: 'rejected', reason: verified.reason }, {
      status: 401,
      headers: { 'Cache-Control': 'no-store' },
    });
  }

  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!supabaseUrl || !serviceKey) {
    return NextResponse.json({ status: 'unavailable', releaseId: verified.event.releaseId }, {
      status: 503,
      headers: { 'Cache-Control': 'no-store' },
    });
  }

  const event = verified.event;
  const response = await fetch(`${supabaseUrl}/rest/v1/rpc/morrow_record_minecraft_event`, {
    method: 'POST',
    headers: {
      apikey: serviceKey,
      authorization: `Bearer ${serviceKey}`,
      'content-type': 'application/json',
    },
    body: JSON.stringify({
      p_campaign_id: event.campaignId,
      p_release_id: event.releaseId,
      p_event_key: event.eventKey,
      p_idempotency_key: event.idempotencyKey,
      p_actor_minecraft_uuid: event.actorMinecraftUuid,
      p_payload: event.payload,
      p_payload_sha256: verified.payloadSha256,
      p_occurred_at: event.occurredAt,
    }),
    cache: 'no-store',
  });
  if (!response.ok) {
    return NextResponse.json({ status: 'unavailable', releaseId: event.releaseId }, {
      status: 503,
      headers: { 'Cache-Control': 'no-store' },
    });
  }
  const rows = await response.json() as RpcRow[];
  const result = rows[0];
  if (!result) return NextResponse.json({ status: 'unavailable', releaseId: event.releaseId }, {
    status: 503,
    headers: { 'Cache-Control': 'no-store' },
  });
  const statusCode = result.status === 'collision' ? 409 : result.status === 'blocked' ? 422 : 200;
  return NextResponse.json({
    status: result.status,
    created: result.created,
    eventId: result.event_id,
    releaseId: event.releaseId,
  }, {
    status: statusCode,
    headers: { 'Cache-Control': 'no-store' },
  });
}
