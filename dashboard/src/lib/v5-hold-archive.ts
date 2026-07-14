import 'server-only';

import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';

export const V5_HOLD_ARCHIVE_DOWNLOAD_PATH = '/the-hold/the-hold.zip';

const ARCHIVE_DIRECTORY = join(process.cwd(), 'content', 'the-hold-v5');
const ARCHIVE_PATH = join(ARCHIVE_DIRECTORY, 'the-hold.zip');
const SHA1_PATH = join(ARCHIVE_DIRECTORY, 'the-hold.sha1');
const SHA1_PATTERN = /^([a-f0-9]{40})(?:\s+\*?the-hold\.zip)?\s*$/i;

export interface V5HoldArchive {
  bytes: Buffer;
  sha1: string;
}

/**
 * Load the private diagnostic archive only when its checked-in checksum still matches. Returning
 * null for every I/O or integrity failure lets public callers fail closed without becoming an
 * oracle for server filesystem state.
 */
export async function readValidatedV5HoldArchive(): Promise<V5HoldArchive | null> {
  try {
    const [bytes, receipt] = await Promise.all([
      readFile(ARCHIVE_PATH),
      readFile(SHA1_PATH, 'utf8'),
    ]);
    const match = SHA1_PATTERN.exec(receipt);
    if (!match) return null;

    const declaredSha1 = match[1]!.toLowerCase();
    const actualSha1 = createHash('sha1').update(bytes).digest('hex');
    return actualSha1 === declaredSha1 ? { bytes, sha1: actualSha1 } : null;
  } catch {
    return null;
  }
}
