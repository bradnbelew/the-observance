import { createHash } from 'node:crypto';

export type AutomationClass = 'A0' | 'A1' | 'A2' | 'A3' | 'A4' | 'A5';

export interface ApprovalEnvelope {
  approval_id: string;
  approval_class: AutomationClass;
  approval_scope: string;
  authored_payload_sha256: string;
  approval_expires_at: string;
}

export function canonicalPayload(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(canonicalPayload).join(',')}]`;
  if (value !== null && typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>)
      .sort(([left], [right]) => left.localeCompare(right));
    return `{${entries.map(([key, item]) => `${JSON.stringify(key)}:${canonicalPayload(item)}`).join(',')}}`;
  }
  const encoded = JSON.stringify(value);
  if (encoded === undefined) throw new TypeError('approval payload contains an unsupported value');
  return encoded;
}

export function authoredPayloadSha256(payload: unknown): string {
  return createHash('sha256').update(canonicalPayload(payload), 'utf8').digest('hex');
}

export function mayRunAutomatically(risk: AutomationClass): boolean {
  return risk === 'A0' || risk === 'A1';
}

export function approvalPermits(
  expectedClass: AutomationClass,
  expectedScope: string,
  authoredPayload: unknown,
  envelope: ApprovalEnvelope | null | undefined,
  now: Date = new Date(),
): boolean {
  if (!envelope || mayRunAutomatically(expectedClass)) return false;
  if (!envelope.approval_id || envelope.approval_class !== expectedClass) return false;
  if (envelope.approval_scope !== expectedScope) return false;
  if (!/^[0-9a-f]{64}$/.test(envelope.authored_payload_sha256)) return false;
  const expiresAt = Date.parse(envelope.approval_expires_at);
  if (!Number.isFinite(expiresAt) || expiresAt <= now.getTime()) return false;
  return envelope.authored_payload_sha256 === authoredPayloadSha256(authoredPayload);
}
