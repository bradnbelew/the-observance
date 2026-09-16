import { NextResponse } from 'next/server';
import readiness from '../../../../../../../morrow/rehearsal/production-readiness.json';

export const dynamic = 'force-static';

export function GET() {
  return NextResponse.json({
    ...readiness,
    servedFrom: 'local_rehearsal_readiness_matrix',
    productionMutation: false,
  }, {
    headers: { 'Cache-Control': 'no-store' },
  });
}
