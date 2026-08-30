import { supabase } from '../db/client.js';
import {
  MORROW_CONTRADICTION_PURPOSE,
  nonceSha256,
  validDiscordId,
  validDecision,
  validReleaseId,
  type ContradictionAction,
  type ContradictionDecision,
  type EvidenceVariant,
} from './contradiction.js';
import type { MorrowDiscordProjectionClaim } from './projection-policy.js';

export interface OpenContradictionInput {
  discordUserId: string;
  releaseId: string;
  guildId: string;
  channelId: string;
  threadId: string | null;
  nonce: string;
  expiresAt: string;
}

export interface OpenContradictionResult {
  status: 'opened' | 'recovered' | 'complete' | 'blocked';
  displayAlias: string | null;
  groupSize: number;
  evidenceVariant: EvidenceVariant | null;
  expiresAt: string | null;
}

export interface ApplyContradictionResult {
  status: 'acknowledged' | 'pending' | 'committed' | 'duplicate' | 'declined' | 'cancelled'
    | 'incorrect' | 'expired' | 'blocked' | 'collision';
  accepted: number;
  required: number;
  eventId: string | null;
}

function oneRow(value: unknown): Record<string, unknown> {
  if (!Array.isArray(value) || value.length !== 1 || !value[0] || typeof value[0] !== 'object') {
    throw new Error('invalid Morrow Discord RPC response');
  }
  return value[0] as Record<string, unknown>;
}

export async function openMorrowContradiction(input: OpenContradictionInput): Promise<OpenContradictionResult> {
  if (!validDiscordId(input.discordUserId) || !validDiscordId(input.guildId)
      || !validDiscordId(input.channelId) || (input.threadId !== null && !validDiscordId(input.threadId))
      || !validReleaseId(input.releaseId) || !Number.isFinite(Date.parse(input.expiresAt))) {
    throw new Error('invalid Morrow contradiction open request');
  }
  const { data, error } = await supabase.rpc('morrow_open_discord_contradiction', {
    p_discord_user_id: input.discordUserId,
    p_release_id: input.releaseId,
    p_guild_id: input.guildId,
    p_channel_id: input.channelId,
    p_thread_id: input.threadId,
    p_nonce_sha256: nonceSha256(input.nonce),
    p_expires_at: input.expiresAt,
    p_purpose: MORROW_CONTRADICTION_PURPOSE,
  });
  if (error) throw error;
  const row = oneRow(data);
  const status = row.status;
  if (status !== 'opened' && status !== 'recovered' && status !== 'complete' && status !== 'blocked') {
    throw new Error('invalid Morrow contradiction open status');
  }
  const groupSize = Number(row.group_size ?? 0);
  const evidenceVariant = row.evidence_variant;
  if (!Number.isInteger(groupSize) || groupSize < 0 || groupSize > 6
      || (evidenceVariant !== null && evidenceVariant !== 'route_digest' && evidenceVariant !== 'source_gap')) {
    throw new Error('invalid Morrow contradiction open payload');
  }
  return {
    status,
    displayAlias: typeof row.display_alias === 'string' ? row.display_alias : null,
    groupSize,
    evidenceVariant: evidenceVariant as EvidenceVariant | null,
    expiresAt: typeof row.expires_at === 'string' ? row.expires_at : null,
  };
}

export async function applyMorrowContradiction(input: {
  discordUserId: string;
  releaseId: string;
  guildId: string;
  channelId: string;
  threadId: string | null;
  nonce: string;
  action: ContradictionAction;
  decision: ContradictionDecision | null;
}): Promise<ApplyContradictionResult> {
  if (!validDiscordId(input.discordUserId) || !validDiscordId(input.guildId)
      || !validDiscordId(input.channelId) || (input.threadId !== null && !validDiscordId(input.threadId))
      || !validReleaseId(input.releaseId)
      || (input.action === 'decision' ? !input.decision || !validDecision(input.decision) : input.decision !== null)) {
    throw new Error('invalid Morrow contradiction action request');
  }
  const { data, error } = await supabase.rpc('morrow_apply_discord_contradiction', {
    p_discord_user_id: input.discordUserId,
    p_release_id: input.releaseId,
    p_guild_id: input.guildId,
    p_channel_id: input.channelId,
    p_thread_id: input.threadId,
    p_nonce_sha256: nonceSha256(input.nonce),
    p_action: input.action,
    p_decision: input.decision,
    p_purpose: MORROW_CONTRADICTION_PURPOSE,
  });
  if (error) throw error;
  const row = oneRow(data);
  const allowed = new Set([
    'acknowledged', 'pending', 'committed', 'duplicate', 'declined', 'cancelled',
    'incorrect', 'expired', 'blocked', 'collision',
  ]);
  if (typeof row.status !== 'string' || !allowed.has(row.status)) {
    throw new Error('invalid Morrow contradiction action status');
  }
  const accepted = Number(row.accepted ?? 0);
  const required = Number(row.required ?? 0);
  if (!Number.isInteger(accepted) || !Number.isInteger(required) || accepted < 0 || required < 0 || required > 6) {
    throw new Error('invalid Morrow contradiction aggregate');
  }
  return {
    status: row.status as ApplyContradictionResult['status'],
    accepted,
    required,
    eventId: typeof row.event_id === 'string' ? row.event_id : null,
  };
}

export async function claimMorrowDiscordProjections(input: {
  workerId: string;
  releaseId: string;
  limit: number;
  leaseSeconds: number;
}): Promise<MorrowDiscordProjectionClaim[]> {
  if (!/^[0-9a-f-]{36}$/i.test(input.workerId) || !validReleaseId(input.releaseId)
      || !Number.isInteger(input.limit) || input.limit < 1 || input.limit > 50
      || !Number.isInteger(input.leaseSeconds) || input.leaseSeconds < 5 || input.leaseSeconds > 300) {
    throw new Error('invalid Morrow projection claim request');
  }
  const { data, error } = await supabase.rpc('morrow_claim_discord_projections', {
    p_worker_id: input.workerId,
    p_release_id: input.releaseId,
    p_limit: input.limit,
    p_lease_seconds: input.leaseSeconds,
  });
  if (error) throw error;
  if (!Array.isArray(data)) throw new Error('invalid Morrow projection claim response');
  return data.map((value) => {
    if (!value || typeof value !== 'object') throw new Error('invalid Morrow projection claim');
    const row = value as Record<string, unknown>;
    if (typeof row.event_id !== 'string' || typeof row.event_key !== 'string'
        || typeof row.campaign_id !== 'string' || typeof row.release_id !== 'string'
        || typeof row.payload_sha256 !== 'string' || !Number.isInteger(row.attempts)) {
      throw new Error('invalid Morrow projection claim');
    }
    return {
      eventId: row.event_id,
      eventKey: row.event_key,
      campaignId: row.campaign_id,
      releaseId: row.release_id,
      payloadSha256: row.payload_sha256,
      channelId: typeof row.channel_id === 'string' ? row.channel_id : null,
      threadId: typeof row.thread_id === 'string' ? row.thread_id : null,
      attempts: Number(row.attempts),
    };
  });
}

export async function activateMorrowContradiction(input: {
  eventId: string;
  releaseId: string;
  guildId: string;
  channelId: string;
  threadId: string | null;
}): Promise<'activated' | 'duplicate' | 'blocked' | 'collision'> {
  if (!/^[0-9a-f-]{36}$/i.test(input.eventId) || !validReleaseId(input.releaseId)
      || !validDiscordId(input.guildId) || !validDiscordId(input.channelId)
      || (input.threadId !== null && !validDiscordId(input.threadId))) {
    throw new Error('invalid Morrow contradiction activation request');
  }
  const { data, error } = await supabase.rpc('morrow_activate_discord_contradiction', {
    p_event_id: input.eventId,
    p_release_id: input.releaseId,
    p_guild_id: input.guildId,
    p_channel_id: input.channelId,
    p_thread_id: input.threadId,
  });
  if (error) throw error;
  if (data !== 'activated' && data !== 'duplicate' && data !== 'blocked' && data !== 'collision') {
    throw new Error('invalid Morrow contradiction activation response');
  }
  return data;
}

export async function completeMorrowDiscordProjection(input: {
  eventId: string;
  workerId: string;
  releaseId: string;
  applied: boolean;
  error?: string;
}): Promise<boolean> {
  if (!/^[0-9a-f-]{36}$/i.test(input.eventId) || !/^[0-9a-f-]{36}$/i.test(input.workerId)
      || !validReleaseId(input.releaseId)) throw new Error('invalid Morrow projection completion request');
  const { data, error } = await supabase.rpc('morrow_complete_discord_projection', {
    p_event_id: input.eventId,
    p_worker_id: input.workerId,
    p_release_id: input.releaseId,
    p_applied: input.applied,
    p_error: input.error?.normalize('NFKC').trim().slice(0, 500) || null,
  });
  if (error) throw error;
  if (typeof data !== 'boolean') throw new Error('invalid Morrow projection completion response');
  return data;
}
