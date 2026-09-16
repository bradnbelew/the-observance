import { NextResponse } from 'next/server';
import { gateById, gateReceiptById } from '@/lib/morrow-full-rehearsal';

export const dynamic = 'force-static';

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ gateId: string }> },
) {
  const { gateId } = await params;
  const gate = gateById(gateId);
  const receipt = gateReceiptById(gateId);

  if (!gate || !receipt) {
    return NextResponse.json({
      error: 'gate_not_found',
      gateId,
      expected: 'G01-G15',
    }, { status: 404 });
  }

  return NextResponse.json({
    schemaVersion: '1.0.0-morrow-gate-local-rehearsal',
    status: 'local_gate_contract_not_playable',
    productionMutation: false,
    gate,
    receipt,
  }, {
    headers: { 'Cache-Control': 'no-store' },
  });
}
