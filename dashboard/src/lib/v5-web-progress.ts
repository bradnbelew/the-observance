import 'server-only';

import type { Json } from '@/lib/database.types';
import { createAdminClient } from '@/lib/supabase/admin';
import { isAllowedV5WebsiteSequence } from '@/lib/v5-web-node-policy';

export type V5WebProgressState = 'complete' | 'blocked' | 'unavailable';

export interface V5WebProgressResult {
  complete: boolean;
  state: V5WebProgressState;
}

function jsonObject(value: Json | undefined): Record<string, Json | undefined> {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, Json | undefined>
    : {};
}

async function readFlags(client: ReturnType<typeof createAdminClient>): Promise<Record<string, Json | undefined>> {
  const { data, error } = await client
    .from('arc_state')
    .select('flags')
    .eq('id', 1)
    .maybeSingle<{ flags: Json }>();
  if (error || !data) throw error ?? new Error('arc_state row is missing');
  return jsonObject(data.flags);
}

/** Read one fixed completion flag without exposing the full story-state object to a page. */
export async function readV5CompletionFlag(completionFlag: string): Promise<V5WebProgressResult> {
  if (!process.env.NEXT_PUBLIC_SUPABASE_URL || !process.env.SUPABASE_SERVICE_ROLE_KEY) {
    return { complete: false, state: 'unavailable' };
  }
  try {
    const flags = await readFlags(createAdminClient());
    const complete = flags[completionFlag] === true;
    return { complete, state: complete ? 'complete' : 'blocked' };
  } catch {
    return { complete: false, state: 'unavailable' };
  }
}

/**
 * Record a fixed, server-authored website sequence through the same prerequisite-enforcing RPC used
 * by Discord and Paper. Callers pass constants only; no request parameter can select an arbitrary
 * node. Completed flags are skipped and the final state is re-read, making retries idempotent.
 */
export async function recordV5WebSequence(
  nodeKeys: readonly string[],
  receiptNamespace: string,
  payload: Record<string, Json>,
): Promise<V5WebProgressResult> {
  // Constants-only callers are still fenced here. A future page must not be able to turn the
  // service-role client into a bypass for a Discord conclusion, physical mechanism, ritual, or
  // unauthored node merely by passing a different key.
  if (!isAllowedV5WebsiteSequence(nodeKeys)) {
    return { complete: false, state: 'unavailable' };
  }
  if (!process.env.NEXT_PUBLIC_SUPABASE_URL || !process.env.SUPABASE_SERVICE_ROLE_KEY) {
    return { complete: false, state: 'unavailable' };
  }

  try {
    const client = createAdminClient();
    const { data: nodes, error: nodeError } = await client
      .from('investigation_nodes')
      .select('node_key,completion_flag')
      .in('node_key', [...nodeKeys])
      .eq('active', true)
      .returns<Array<{ node_key: string; completion_flag: string }>>();
    if (nodeError || !nodes || nodes.length !== nodeKeys.length) {
      return { complete: false, state: 'unavailable' };
    }

    const byKey = new Map(nodes.map((node) => [node.node_key, node]));
    let flags = await readFlags(client);
    for (const nodeKey of nodeKeys) {
      const node = byKey.get(nodeKey);
      if (!node) return { complete: false, state: 'unavailable' };
      if (flags[node.completion_flag] === true) continue;

      const idempotencyKey = `web:${receiptNamespace}:${nodeKey}`;
      const { error } = await client.rpc('observance_record_evidence', {
        p_receipt_key: idempotencyKey,
        p_node_key: nodeKey,
        p_source: 'web',
        p_idempotency_key: idempotencyKey,
        p_player_id: null,
        p_payload: payload,
      });
      if (error) {
        const blocked = error.message.toLowerCase().includes('prerequisites not satisfied');
        return { complete: false, state: blocked ? 'blocked' : 'unavailable' };
      }
      flags = await readFlags(client);
    }

    flags = await readFlags(client);
    const complete = nodeKeys.every((key) => {
      const node = byKey.get(key);
      return node !== undefined && flags[node.completion_flag] === true;
    });
    return { complete, state: complete ? 'complete' : 'unavailable' };
  } catch {
    return { complete: false, state: 'unavailable' };
  }
}
