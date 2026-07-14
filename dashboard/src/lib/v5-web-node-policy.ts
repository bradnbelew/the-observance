/**
 * The complete V5 website mutation surface.
 *
 * These six nodes are advanced only by their dedicated, fixed Copperline routes. The generic Record
 * terminal is deliberately read-only in V5: the dashboard has no authenticated player session, so a
 * typed Minecraft name is not proof of identity and must never authorize a solve.
 */
export const V5_WEBSITE_NODE_KEYS = [
  'LS01',
  'LS02',
  'LS03',
  'LS04',
  'A06',
  'A07',
] as const;

const V5_WEBSITE_NODE_SET = new Set<string>(V5_WEBSITE_NODE_KEYS);

export function isV5WebsiteNodeKey(nodeKey: string): boolean {
  return V5_WEBSITE_NODE_SET.has(nodeKey);
}

export function isAllowedV5WebsiteSequence(nodeKeys: readonly string[]): boolean {
  return nodeKeys.length > 0
    && new Set(nodeKeys).size === nodeKeys.length
    && nodeKeys.every(isV5WebsiteNodeKey);
}
