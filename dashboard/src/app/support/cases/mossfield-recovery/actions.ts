'use server';

import { headers } from 'next/headers';
import { revalidatePath } from 'next/cache';
import { externalMutationsAllowed } from '@/lib/deployment-target';
import { createClient } from '@/lib/supabase/server';
import {
  MORROW_CASE_ATTACHMENT_SHA256,
  MORROW_CASE_ID,
  MORROW_CASE_ROUTE,
  payloadSha256,
  validateMorrowCaseAction,
} from '@/lib/morrow-copperline-case';
import { readMorrowCase } from '@/lib/morrow-copperline-server';

export type MorrowCaseActionState = {
  status: 'idle' | 'rejected' | 'locked' | 'success' | 'unavailable';
  message: string;
  receipt?: string;
};

type CopperlineRpcRow = { status: string; created: boolean; event_id: string | null };

export async function submitMorrowCaseAction(
  _previous: MorrowCaseActionState,
  formData: FormData,
): Promise<MorrowCaseActionState> {
  if (!await sameOrigin()) {
    return { status: 'rejected', message: 'The request did not originate from this case desk. Nothing changed.' };
  }
  const validation = validateMorrowCaseAction(formData);
  if (!validation.ok) {
    return {
      status: 'rejected',
      message: validation.reason === 'invalid_checksum'
        ? 'Checksum verification failed. Compare all 64 characters and try again.'
        : validation.reason === 'invalid_token'
          ? 'That handoff token is incomplete or malformed. Use the XXXX-XXXX format.'
          : 'That case operation is not available.',
    };
  }
  if (!externalMutationsAllowed()) {
    return { status: 'unavailable', message: 'This preview is read-only. No case receipt was written.' };
  }

  const context = await readMorrowCase();
  if (context.kind !== 'ready') {
    return { status: context.kind === 'unavailable' ? 'unavailable' : 'locked', message: genericLockedMessage() };
  }
  if (validation.operation === 'verify_case_chain' && context.caseChainAuthenticated) {
    return { status: 'success', message: 'This custody chain is already authenticated. Nothing was duplicated.' };
  }
  if (validation.operation === 'recover_handoff'
      && (!context.caseChainAuthenticated || !context.handoffToken || context.handoffRecovered)) {
    return context.handoffRecovered
      ? { status: 'success', message: 'This server handoff is already recovered. Nothing was duplicated.' }
      : {
          status: 'locked',
          message: context.caseChainAuthenticated
            ? 'The authenticated token field has not arrived yet. Refresh after projection catch-up.'
            : 'Authenticate the attachment custody chain before recovering its handoff.',
        };
  }

  const eventKey = validation.operation === 'verify_case_chain'
    ? 'morrow.act0.case_chain_authenticated'
    : 'morrow.act0.server_handoff_recovered';
  const payload = validation.operation === 'verify_case_chain'
    ? {
        case_id: MORROW_CASE_ID,
        attachment_sha256: MORROW_CASE_ATTACHMENT_SHA256,
        custody_entries: 3,
        operation: 'verify_attachment_custody',
      }
    : {
        case_id: MORROW_CASE_ID,
        operation: 'recover_server_handoff',
        token_verified: true,
      };
  try {
    const client = await createClient();
    const rpcArguments = {
      p_campaign_id: context.campaignId,
      p_player_id: context.playerId,
      p_release_id: context.releaseId,
      p_event_key: eventKey,
      p_idempotency_key: validation.operation === 'verify_case_chain'
        ? 'copperline:morrow:act0:case-chain:v1'
        : 'copperline:morrow:act0:server-handoff:v1',
      p_payload: payload,
      p_payload_sha256: payloadSha256(payload),
      p_short_token: validation.operation === 'recover_handoff' ? validation.normalizedValue : null,
    };
    // Supabase's generated conditional RPC signature collapses newly proposed (not yet migrated)
    // functions to an undefined argument until types are regenerated from the safe rehearsal target.
    // Keep the explicit local contract above and narrow only this proposal boundary.
    const rpcResponse = await client.rpc('morrow_record_copperline_event', rpcArguments as never);
    const data = rpcResponse.data as unknown as CopperlineRpcRow[] | null;
    const error = rpcResponse.error;
    if (error || !data || data.length !== 1) {
      return { status: 'unavailable', message: 'The case ledger is temporarily unavailable. Nothing changed; retry safely.' };
    }
    const result = data[0];
    if (result.status === 'collision') {
      return { status: 'rejected', message: 'The existing receipt does not match this request. The case halted without changing.' };
    }
    if (result.status === 'blocked') {
      return {
        status: 'locked',
        message: validation.operation === 'recover_handoff'
          ? 'The token was not accepted, or its custody prerequisite is not current. Nothing changed.'
          : genericLockedMessage(),
      };
    }
    if (result.status !== 'committed' && result.status !== 'duplicate') {
      return { status: 'unavailable', message: 'The case ledger did not confirm a receipt. Nothing changed; retry safely.' };
    }
    revalidatePath(MORROW_CASE_ROUTE);
    return {
      status: 'success',
      message: validation.operation === 'verify_case_chain'
        ? 'Attachment checksum and three-step custody chain authenticated.'
        : 'Short token accepted. The release-bound server handoff is being projected to this case.',
      receipt: result.event_id ?? undefined,
    };
  } catch {
    return { status: 'unavailable', message: 'The case ledger is temporarily unavailable. Nothing changed; retry safely.' };
  }
}

async function sameOrigin(): Promise<boolean> {
  const requestHeaders = await headers();
  const origin = requestHeaders.get('origin');
  const host = requestHeaders.get('host');
  if (!origin || !host) return true;
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

function genericLockedMessage(): string {
  return 'This signed-in player does not own an active copy of this release-bound case. Nothing changed.';
}
