import { createHash } from 'node:crypto';
import { copperlineP4DirectEntries, type CopperlineArchiveEntry } from './copperline-p4-archive';

export type P4RestoreState = {
  status: 'idle' | 'accepted' | 'wrong' | 'incomplete' | 'throttled' | 'technical_failure';
  message: string;
  receiptId?: string;
  entries?: CopperlineArchiveEntry[];
};

export const INITIAL_P4_RESTORE_STATE: P4RestoreState = {
  status: 'idle',
  message: 'No restore request has been sent.',
};

export function normalizeArchiveField(value: FormDataEntryValue | null): string {
  return typeof value === 'string'
    ? value.normalize('NFKC').trim().replace(/\s+/g, ' ').toUpperCase()
    : '';
}

export function validateP4Restore(formData: FormData): P4RestoreState {
  const ticket = normalizeArchiveField(formData.get('ticket'));
  const attachment = normalizeArchiveField(formData.get('attachment'));
  const suppliedOrder = normalizeArchiveField(formData.get('order')).replaceAll(' ', '-');
  const idempotency = normalizeArchiveField(formData.get('idempotency'));
  if (!ticket || !attachment || !suppliedOrder || !idempotency) {
    return { status: 'incomplete', message: 'Enter the ticket, attachment, cartridge order, and request ID. Nothing changed.' };
  }
  if (ticket !== '2184' || attachment !== 'MOUTH_NOTICE.COMPARE.TXT' || !['03-04', '03-BEFORE-04'].includes(suppliedOrder)) {
    return { status: 'wrong', message: 'That request does not match the retained custody table. Nothing changed.' };
  }
  if (!/^[A-Z0-9][A-Z0-9_-]{5,47}$/.test(idempotency)) {
    return { status: 'incomplete', message: 'Use a request ID of 6–48 letters, numbers, dashes, or underscores.' };
  }
  const order = '03-04';
  const receiptId = createHash('sha256')
    .update(`P4-RESTORE\n${ticket}\n${attachment}\n${order}\n${idempotency}`)
    .digest('hex');
  return {
    status: 'accepted',
    message: 'Accepted. The retained ticket replies and attachments are restored below. The older archive remains unchanged.',
    receiptId,
    entries: [...copperlineP4DirectEntries],
  };
}
