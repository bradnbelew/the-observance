export const MORROW_SEED_RELEASE = 'morrow.local.g01-g05.v1';

export const MORROW_SEED = {
  productSlug: 'retired-managed-worlds',
  sourceCommentPath: '/community/forum?thread=6118',
  footerBuild: 'cl-2011-6118',
  forumThread: '6118',
  alias: 'iona_bell',
  incident: '6118',
  uptime: '04:17:22',
  recoveryKey: '0417iona',
  caseFingerprint: 'MSF-6118-041722-KEEL',
  handoffAddress: 'mossfield.local:25565',
  release: MORROW_SEED_RELEASE,
} as const;

export const mossfieldForumPosts = [
  {
    id: 'p6118-01',
    author: 'rookery',
    date: 'May 18, 2011 04:02 CST',
    title: 'Mossfield restore window',
    body: 'Leaving this here because the panel export keeps dropping attachments. If the archive opens again, follow the staff signature rather than the billing name.',
    edited: 'edited May 18, 2011 04:09 CST',
  },
  {
    id: 'p6118-02',
    author: 'cairn',
    date: 'May 18, 2011 04:11 CST',
    title: 'I only trust the uneven copy',
    body: 'Storehouse labels are wrong in the clean backup. Whoever restores it: do not make the chests tidy just because the sheet says they should be.',
    edited: null,
  },
  {
    id: 'p6118-03',
    author: 'i_bell',
    date: 'May 18, 2011 04:16 CST',
    title: 'internal mirror note',
    body: 'Support attachment 6118 keeps the old employee alias in the footer. Archive lookup accepts the canonical underscore form, not this display handle.',
    edited: 'edited title May 18, 2011 04:17 CST',
  },
] as const;

export const supportTicket6118 = {
  opened: 'May 18, 2011 03:59 CST',
  queue: 'Game Server Recovery',
  customer: 'Rowan Keel / Mossfield',
  status: 'Closed publicly, retained internally',
  signature: 'Iona Bell // Copperline Recovery // alias: iona_bell',
  attachment: 'mossfield-handoff-cl-2011-6118.txt',
  note: 'Do not publish the handoff unless uptime 04:17:22 appears in the retained status table.',
} as const;

export function recoveryLookup(alias?: string, key?: string) {
  const normalizedAlias = (alias ?? '').trim().toLowerCase();
  const normalizedKey = (key ?? '').trim().toLowerCase();
  if (!normalizedAlias && !normalizedKey) return { state: 'empty' as const, message: 'Enter the archived employee alias and recovery key.' };
  if (normalizedAlias !== MORROW_SEED.alias) return { state: 'alias' as const, message: 'No retained case is indexed for that employee alias.' };
  if (normalizedKey !== MORROW_SEED.recoveryKey) return { state: 'key' as const, message: 'Alias recognized. Recovery key does not match the retained status table.' };
  return { state: 'open' as const, message: 'Mossfield handoff recovered.' };
}
