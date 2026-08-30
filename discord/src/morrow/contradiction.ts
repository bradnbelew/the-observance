import { createHash, randomBytes } from 'node:crypto';

export const MORROW_BEHAVIOR_REUSE_EVENT = 'morrow.act2.behavior_reuse_proven';
export const MORROW_PRIVATE_CONTRADICTION_EVENT = 'morrow.act2.private_contradiction_resolved';
export const MORROW_CONTRADICTION_PURPOSE = 'classify_behavior_reuse_provenance_v1';
export const MORROW_CONTRADICTION_TTL_MS = 15 * 60 * 1_000;
export const MORROW_GROUP_RESOLUTION = 'file_live_behavior_as_inferred_source';
export const MORROW_GROUP_RECEIPT_TEXT = [
  '**Mossfield recovery · group receipt**',
  'The behavior-reuse contradiction is filed as inferred source material.',
  'Private evidence remains private. This receipt authorizes no capture and contains no private payload.',
].join('\n');

export type ContradictionAction = 'ack' | 'decline' | 'cancel' | 'decision';
export type ContradictionDecision =
  | typeof MORROW_GROUP_RESOLUTION
  | 'retain_authenticated_label'
  | 'defer_classification';
export type EvidenceVariant = 'route_digest' | 'source_gap';

export interface DiscordInteractionScope {
  guildId: string | null;
  channelId: string;
  parentChannelId: string | null;
  isThread: boolean;
}

export interface ConfiguredDiscordScope {
  guildId: string;
  channelId: string;
  threadId: string | null;
}

const DISCORD_ID = /^[0-9]{15,22}$/;
const RELEASE_ID = /^[a-z0-9][a-z0-9._-]{6,79}$/;
const NONCE = /^[A-Za-z0-9_-]{22,32}$/;
const CUSTOM_ID = /^morrow:contradiction:v1:([A-Za-z0-9_-]{22,32}):(ack|decline|cancel|decision)$/;

export function validDiscordId(value: string): boolean {
  return DISCORD_ID.test(value);
}

export function validReleaseId(value: string): boolean {
  return RELEASE_ID.test(value);
}

export function newContradictionNonce(): string {
  return randomBytes(18).toString('base64url');
}

export function nonceSha256(nonce: string): string {
  if (!NONCE.test(nonce)) throw new Error('invalid Morrow interaction nonce');
  return createHash('sha256').update(nonce, 'utf8').digest('hex');
}

export function contradictionCustomId(nonce: string, action: ContradictionAction): string {
  if (!NONCE.test(nonce)) throw new Error('invalid Morrow interaction nonce');
  return `morrow:contradiction:v1:${nonce}:${action}`;
}

export function parseContradictionCustomId(customId: string): {
  nonce: string;
  action: ContradictionAction;
} | null {
  const match = CUSTOM_ID.exec(customId);
  if (!match?.[1] || !match[2]) return null;
  return { nonce: match[1], action: match[2] as ContradictionAction };
}

export function resolveInteractionScope(
  actual: DiscordInteractionScope,
  configured: ConfiguredDiscordScope,
): { ok: true; channelId: string; threadId: string | null } | { ok: false } {
  if (actual.guildId !== configured.guildId) return { ok: false };
  if (configured.threadId) {
    return actual.isThread
      && actual.channelId === configured.threadId
      && actual.parentChannelId === configured.channelId
      ? { ok: true, channelId: configured.channelId, threadId: configured.threadId }
      : { ok: false };
  }
  return !actual.isThread && actual.channelId === configured.channelId
    ? { ok: true, channelId: configured.channelId, threadId: null }
    : { ok: false };
}

export function authoredPrivateEvidence(displayAlias: string, variant: EvidenceVariant): string {
  const safeAlias = displayAlias.normalize('NFKC').replace(/[\r\n`@]/g, '').trim().slice(0, 32);
  if (!safeAlias) throw new Error('invalid display alias');
  const evidence = variant === 'route_digest'
    ? 'The claimed resident replay carries the exact sealed live-test route digest, while its label still says authenticated resident recording.'
    : 'No pre-incident movement digest exists for the claimed resident replay; its route digest first appears in the sealed live-test receipt.';
  return [
    `**Private custody exception · ${safeAlias}**`,
    evidence,
    'This evidence is addressed only to your linked identity. Compare its provenance class with your group; do not repost the private field.',
  ].join('\n');
}

export const CONTRADICTION_DECISIONS: readonly {
  label: string;
  value: ContradictionDecision;
  description: string;
}[] = [
  {
    label: 'File as inferred source',
    value: MORROW_GROUP_RESOLUTION,
    description: 'The live test supplied the route; the historical label is unsupported.',
  },
  {
    label: 'Retain authenticated label',
    value: 'retain_authenticated_label',
    description: 'Treat the claimed resident recording as independently authenticated.',
  },
  {
    label: 'Defer classification',
    value: 'defer_classification',
    description: 'Leave the contradiction unresolved and create no group receipt.',
  },
] as const;

export function validDecision(value: string): value is ContradictionDecision {
  return CONTRADICTION_DECISIONS.some((choice) => choice.value === value);
}

export function decisionFeedback(decision: ContradictionDecision): string {
  if (decision === MORROW_GROUP_RESOLUTION) {
    return 'Your linked-player classification is retained. The group receipt contains only the resolution and aggregate count.';
  }
  if (decision === 'retain_authenticated_label') {
    return 'That label conflicts with the custody chain: no independent pre-incident route digest exists. Nothing changed.';
  }
  return 'Classification deferred. No vote or progression receipt was created; reopen the private exception when ready.';
}

export function aggregateLinkedDecisions(input: {
  linkedPlayerIds: readonly string[];
  decisions: ReadonlyMap<string, ContradictionDecision>;
}): { status: 'pending'; accepted: number; required: number } | { status: 'ready'; accepted: number; required: number } {
  const linked = [...new Set(input.linkedPlayerIds)];
  if (linked.length < 1 || linked.length > 6 || linked.some((id) => !/^[0-9a-f-]{36}$/i.test(id))) {
    throw new Error('invalid linked group');
  }
  const accepted = linked.filter((playerId) => input.decisions.get(playerId) === MORROW_GROUP_RESOLUTION).length;
  return accepted === linked.length
    ? { status: 'ready', accepted, required: linked.length }
    : { status: 'pending', accepted, required: linked.length };
}

export interface BoundContradictionSession {
  discordUserId: string;
  campaignId: string;
  playerId: string;
  releaseId: string;
  guildId: string;
  channelId: string;
  threadId: string | null;
  purpose: string;
  expiresAtMs: number;
  state: 'offered' | 'acknowledged' | 'decided' | 'declined' | 'cancelled' | 'expired';
}

export function validateBoundInteraction(
  session: BoundContradictionSession | null,
  attempted: Omit<BoundContradictionSession, 'expiresAtMs' | 'state'>,
  nowMs: number,
): 'ready' | 'blocked' | 'expired' | 'terminal' {
  if (!session) return 'blocked';
  if (session.discordUserId !== attempted.discordUserId
      || session.campaignId !== attempted.campaignId
      || session.playerId !== attempted.playerId
      || session.releaseId !== attempted.releaseId
      || session.guildId !== attempted.guildId
      || session.channelId !== attempted.channelId
      || session.threadId !== attempted.threadId
      || session.purpose !== attempted.purpose) return 'blocked';
  if (session.expiresAtMs <= nowMs || session.state === 'expired') return 'expired';
  return session.state === 'offered' || session.state === 'acknowledged' ? 'ready' : 'terminal';
}

export interface GroupReceiptAttempt {
  campaignId: string;
  releaseId: string;
  sourceEventId: string;
  prerequisiteEvent: string;
  eventKey: string;
  idempotencyKey: string;
  payloadHash: string;
  existing?: Omit<GroupReceiptAttempt, 'prerequisiteEvent' | 'existing'>;
}

export function evaluateGroupReceipt(
  attempt: GroupReceiptAttempt,
): 'committed' | 'blocked' | 'duplicate' | 'collision' {
  if (!validReleaseId(attempt.releaseId)
      || attempt.prerequisiteEvent !== MORROW_BEHAVIOR_REUSE_EVENT
      || attempt.eventKey !== MORROW_PRIVATE_CONTRADICTION_EVENT
      || attempt.idempotencyKey !== 'discord:morrow:act2:private-contradiction:v1'
      || !/^[0-9a-f]{64}$/.test(attempt.payloadHash)) return 'blocked';
  if (!attempt.existing) return 'committed';
  return attempt.existing.campaignId === attempt.campaignId
    && attempt.existing.releaseId === attempt.releaseId
    && attempt.existing.sourceEventId === attempt.sourceEventId
    && attempt.existing.eventKey === attempt.eventKey
    && attempt.existing.idempotencyKey === attempt.idempotencyKey
    && attempt.existing.payloadHash === attempt.payloadHash ? 'duplicate' : 'collision';
}
