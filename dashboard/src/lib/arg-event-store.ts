import 'server-only';

import { resolve } from 'node:path';
import type { Json } from '@/lib/database.types';
import { externalMutationsAllowed } from '@/lib/deployment-target';
import { createAdminClient } from '@/lib/supabase/admin';
import { FileArgEventLedger, type ArgJson } from './arg-event-ledger';
import type { ArgEventKey, ArgSurface } from './arg-event-policy';

export type CampaignEventResult = {
  status: 'committed' | 'blocked' | 'collision' | 'unavailable';
  created: boolean;
  eventId?: string;
  missingPrerequisites?: readonly string[];
};

function localLedgerPath(): string | null {
  const configured = process.env.OBSERVANCE_LOCAL_ARG_LEDGER?.trim();
  if (configured) return resolve(configured);
  if (process.env.NODE_ENV === 'development' && !process.env.VERCEL_ENV) {
    return resolve(process.cwd(), '.observance-local', 'arg-events.json');
  }
  return null;
}

export async function recordCampaignEvent(input: {
  eventKey: ArgEventKey;
  idempotencyKey: string;
  source: ArgSurface;
  actorId?: string | null;
  payload: ArgJson;
}): Promise<CampaignEventResult> {
  const path = localLedgerPath();
  if (path) {
    try {
      const result = await new FileArgEventLedger(path).record(input);
      return {
        status: result.status,
        created: result.created,
        eventId: result.event?.eventId,
        missingPrerequisites: result.missingPrerequisites,
      };
    } catch {
      return { status: 'unavailable', created: false };
    }
  }

  if (!externalMutationsAllowed()
      || !process.env.NEXT_PUBLIC_SUPABASE_URL
      || !process.env.SUPABASE_SERVICE_ROLE_KEY) {
    return { status: 'unavailable', created: false };
  }

  try {
    const { data, error } = await createAdminClient().rpc('observance_record_arg_event', {
      p_event_key: input.eventKey,
      p_idempotency_key: input.idempotencyKey,
      p_source: input.source,
      p_actor_id: input.actorId ?? null,
      p_payload: input.payload as Json,
    });
    if (error || !data || data.length !== 1) return { status: 'unavailable', created: false };
    const row = data[0];
    return {
      status: row.status as CampaignEventResult['status'],
      created: row.created,
      eventId: row.event_id ?? undefined,
      missingPrerequisites: row.missing_prerequisites ?? undefined,
    };
  } catch {
    return { status: 'unavailable', created: false };
  }
}

export async function hasCampaignEvent(eventKey: ArgEventKey): Promise<boolean | null> {
  const path = localLedgerPath();
  if (path) return new FileArgEventLedger(path).has(eventKey);
  if (!process.env.NEXT_PUBLIC_SUPABASE_URL || !process.env.SUPABASE_SERVICE_ROLE_KEY) return null;
  try {
    const { count, error } = await createAdminClient()
      .from('arg_events')
      .select('event_id', { count: 'exact', head: true })
      .eq('event_key', eventKey);
    return error ? null : (count ?? 0) > 0;
  } catch {
    return null;
  }
}

/** Read-only consequence projection. Inputs still commit through the owning platform adapter. */
export async function latestCampaignEventPayload(eventKey: ArgEventKey): Promise<ArgJson | null> {
  const path = localLedgerPath();
  if (path) {
    const events = (await new FileArgEventLedger(path).read()).events;
    return [...events].reverse().find((event) => event.eventKey === eventKey)?.payload ?? null;
  }
  if (!process.env.NEXT_PUBLIC_SUPABASE_URL || !process.env.SUPABASE_SERVICE_ROLE_KEY) return null;
  try {
    const { data, error } = await createAdminClient()
      .from('arg_events')
      .select('payload')
      .eq('event_key', eventKey)
      .order('occurred_at', { ascending: false })
      .limit(1)
      .maybeSingle();
    return error || !data ? null : data.payload as ArgJson;
  } catch {
    return null;
  }
}
