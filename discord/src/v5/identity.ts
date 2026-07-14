import { createHash } from 'node:crypto';

/** Decimal-only normalization shared by C01's documented callback and the Discord receipt edge. */
export function normalizeCopperlineCallback(raw: string): string {
  return raw.replace(/\D/g, '');
}

export function isCopperlineCallback(raw: string): boolean {
  return normalizeCopperlineCallback(raw) === '9137';
}

const LINK_CODE = /^[0-9A-HJKMNP-TV-Z]{12}$/;

/** Match Paper's Crockford-base32 grammar without guessing ambiguous I/L/O/U characters. */
export function normalizeIdentityLinkCode(raw: string): string {
  const compact = raw.normalize('NFKC').toUpperCase().replace(/[\s-]+/g, '');
  return LINK_CODE.test(compact) ? compact : '';
}

/** Lowercase SHA-256 sent to the atomic RPC; the plaintext code is never stored. */
export function hashIdentityLinkCode(raw: string): string {
  const normalized = normalizeIdentityLinkCode(raw);
  return normalized ? createHash('sha256').update(normalized, 'utf8').digest('hex') : '';
}
