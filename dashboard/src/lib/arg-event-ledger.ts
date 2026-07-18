import { createHash, randomUUID } from 'node:crypto';
import { mkdir, open, readFile, rename, rm } from 'node:fs/promises';
import { dirname, isAbsolute } from 'node:path';
import {
  ARG_EVENT_DEFINITIONS,
  canRecordArgEvent,
  type ArgEventKey,
  type ArgSurface,
} from './arg-event-policy';

export type ArgJson = null | boolean | number | string | ArgJson[] | { [key: string]: ArgJson };

export type ArgEventRecord = {
  eventId: string;
  eventKey: ArgEventKey;
  idempotencyKey: string;
  source: ArgSurface;
  actorId: string | null;
  payload: ArgJson;
  payloadSha256: string;
  occurredAt: string;
};

export type ArgProjectionRecord = {
  eventId: string;
  surface: ArgSurface;
  status: 'queued' | 'applied';
  attempts: number;
};

export type ArgLedgerDocument = {
  schemaVersion: '1.0.0-arg-event-ledger';
  events: ArgEventRecord[];
  projections: ArgProjectionRecord[];
};

export type ArgEventCommit = {
  status: 'committed' | 'blocked' | 'collision';
  created: boolean;
  event?: ArgEventRecord;
  missingPrerequisites?: readonly string[];
};

const EMPTY_LEDGER: ArgLedgerDocument = {
  schemaVersion: '1.0.0-arg-event-ledger',
  events: [],
  projections: [],
};

export class FileArgEventLedger {
  constructor(
    private readonly path: string,
    private readonly now: () => Date = () => new Date(),
  ) {
    if (!isAbsolute(path)) throw new Error('ARG event ledger path must be absolute');
  }

  async read(): Promise<ArgLedgerDocument> {
    try {
      const parsed = JSON.parse(await readFile(this.path, 'utf8')) as Partial<ArgLedgerDocument>;
      if (parsed.schemaVersion !== EMPTY_LEDGER.schemaVersion
          || !Array.isArray(parsed.events)
          || !Array.isArray(parsed.projections)) {
        throw new Error('ARG event ledger schema is invalid');
      }
      return parsed as ArgLedgerDocument;
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return structuredClone(EMPTY_LEDGER);
      throw error;
    }
  }

  async has(eventKey: ArgEventKey): Promise<boolean> {
    return (await this.read()).events.some((event) => event.eventKey === eventKey);
  }

  async record(input: {
    eventKey: ArgEventKey;
    idempotencyKey: string;
    source: ArgSurface;
    actorId?: string | null;
    payload: ArgJson;
  }): Promise<ArgEventCommit> {
    validateInput(input);
    const release = await acquireLock(`${this.path}.lock`);
    try {
      const ledger = await this.read();
      const payloadJson = stableJson(input.payload);
      const payloadSha256 = sha256(payloadJson);
      const prior = ledger.events.find((event) => event.idempotencyKey === input.idempotencyKey);
      if (prior) {
        const same = prior.eventKey === input.eventKey
          && prior.source === input.source
          && prior.actorId === (input.actorId ?? null)
          && prior.payloadSha256 === payloadSha256;
        return { status: same ? 'committed' : 'collision', created: false, event: prior };
      }

      const missing = ARG_EVENT_DEFINITIONS[input.eventKey].prerequisites
        .filter((required) => !ledger.events.some((event) => event.eventKey === required));
      if (missing.length > 0) {
        return { status: 'blocked', created: false, missingPrerequisites: missing };
      }

      const eventId = sha256(`${input.eventKey}\n${input.idempotencyKey}`).slice(0, 32);
      const event: ArgEventRecord = {
        eventId,
        eventKey: input.eventKey,
        idempotencyKey: input.idempotencyKey,
        source: input.source,
        actorId: input.actorId ?? null,
        payload: JSON.parse(payloadJson) as ArgJson,
        payloadSha256,
        occurredAt: this.now().toISOString(),
      };
      ledger.events.push(event);
      for (const surface of ARG_EVENT_DEFINITIONS[input.eventKey].projectionSurfaces) {
        ledger.projections.push({
          eventId,
          surface,
          status: surface === input.source ? 'applied' : 'queued',
          attempts: surface === input.source ? 1 : 0,
        });
      }
      await writeAtomic(this.path, ledger);
      return { status: 'committed', created: true, event };
    } finally {
      await release();
    }
  }
}

function validateInput(input: {
  eventKey: ArgEventKey;
  idempotencyKey: string;
  source: ArgSurface;
  actorId?: string | null;
  payload: ArgJson;
}): void {
  if (!canRecordArgEvent(input.eventKey, input.source)) {
    throw new Error(`${input.source} cannot record ${input.eventKey}`);
  }
  if (!/^[a-z0-9][a-z0-9:._/-]{7,159}$/.test(input.idempotencyKey)) {
    throw new Error('ARG idempotency key has invalid grammar');
  }
  if (input.actorId && !/^[a-z0-9][a-z0-9:._-]{0,127}$/i.test(input.actorId)) {
    throw new Error('ARG actor id has invalid grammar');
  }
  const payload = stableJson(input.payload);
  if (Buffer.byteLength(payload, 'utf8') > 8192) throw new Error('ARG event payload is too large');
}

export function stableJson(value: ArgJson): string {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(stableJson).join(',')}]`;
  return `{${Object.keys(value).sort().map((key) => `${JSON.stringify(key)}:${stableJson(value[key]!)}`).join(',')}}`;
}

function sha256(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}

async function writeAtomic(path: string, ledger: ArgLedgerDocument): Promise<void> {
  await mkdir(dirname(path), { recursive: true });
  const temporary = `${path}.${process.pid}.${randomUUID()}.tmp`;
  const handle = await open(temporary, 'wx', 0o600);
  try {
    await handle.writeFile(`${JSON.stringify(ledger, null, 2)}\n`, 'utf8');
    await handle.sync();
  } finally {
    await handle.close();
  }
  await rename(temporary, path);
}

async function acquireLock(lockPath: string): Promise<() => Promise<void>> {
  await mkdir(dirname(lockPath), { recursive: true });
  for (let attempt = 0; attempt < 100; attempt += 1) {
    try {
      await mkdir(lockPath);
      return async () => rm(lockPath, { recursive: true, force: true });
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== 'EEXIST') throw error;
      await new Promise((resolve) => setTimeout(resolve, 10));
    }
  }
  throw new Error('ARG event ledger is busy');
}
