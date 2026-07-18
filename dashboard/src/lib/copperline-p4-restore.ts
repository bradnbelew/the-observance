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
  const operation = normalizeArchiveField(formData.get('operation'));
  if (!operation) {
    return { status: 'incomplete', message: 'The restore action was missing. Nothing changed.' };
  }
  if (operation !== 'RESTORE-RETAINED-ATTACHMENTS') {
    return { status: 'wrong', message: 'That action is not available for this retained table. Nothing changed.' };
  }
  const receiptId = createHash('sha256')
    .update('P4-RESTORE\n2184\nRETAINED-ATTACHMENTS\nREAD-ONLY')
    .digest('hex');
  return {
    status: 'accepted',
    message: 'Accepted. The five retained rows are restored below as a read-only copy. The older archive remains unchanged.',
    receiptId,
    entries: [...copperlineP4DirectEntries],
  };
}
