/**
 * Pure decoder for plugin-authored event_log rows.
 *
 * The live event_log contract is the dashboard's four-column shape
 * (`level, source, message, created_at`). EventLogRow therefore folds the
 * narrative type, player UUID, and optional JSON detail into `message`:
 *
 *   [keeper] name opened the keeper (uuid=...) {"surface":"keeper_open"}
 *
 * Showrunner consumers must decode that shape; querying the legacy
 * type/context/mc_uuid/detail columns silently misses real plugin events.
 */
export interface StoredEventLogRow {
  id: number;
  source: string | null;
  message: string | null;
  created_at: string;
}

export interface DecodedPluginEvent {
  id: number;
  type: string | null;
  context: string | null;
  message: string;
  mcUuid: string | null;
  detail: Record<string, unknown>;
  createdAt: string;
}

const TYPE_PREFIX = /^\[([^\]]+)]\s*/;
const UUID_FIELD = /\s+\(uuid=([0-9a-fA-F-]{36})\)/;
const DETAIL_SUFFIX = /\s+(\{.*\})\s*$/;

/** Decode one EventLogRow.composeMessage payload without throwing. */
export function decodePluginEvent(row: StoredEventLogRow): DecodedPluginEvent {
  let body = row.message ?? '';
  const typeMatch = body.match(TYPE_PREFIX);
  const type = typeMatch?.[1]?.trim().toLowerCase() || null;
  if (typeMatch) body = body.slice(typeMatch[0].length);

  let detail: Record<string, unknown> = {};
  const detailMatch = body.match(DETAIL_SUFFIX);
  if (detailMatch?.[1]) {
    try {
      const parsed: unknown = JSON.parse(detailMatch[1]);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        detail = parsed as Record<string, unknown>;
        body = body.slice(0, detailMatch.index).trimEnd();
      }
    } catch {
      // Not a valid folded detail blob; retain it as part of the human message.
    }
  }

  const uuidMatch = body.match(UUID_FIELD);
  const mcUuid = uuidMatch?.[1]?.toLowerCase() ?? null;
  if (uuidMatch?.index != null) {
    body = `${body.slice(0, uuidMatch.index)}${body.slice(uuidMatch.index + uuidMatch[0].length)}`.trim();
  }

  return {
    id: row.id,
    type,
    context: row.source,
    message: body.trim(),
    mcUuid,
    detail,
    createdAt: row.created_at,
  };
}
