export const P4_COPPERLINE_ROUTE = {
  community: '/community/index.php',
  accountFilter: '/community/index.php?user=mkept',
  priorBackupPost: '/community/2011/02/08/world-backup/',
  disagreementPost: '/community/2011/02/11/old-copy/',
  custodyTicket: '/support/ticket.php?id=2184',
  retainedAttachments: '/community/archive/intake-copies',
} as const;

export const P4_COPPERLINE_ROUTE_STEPS = [
  {
    id: 'ordinary-history',
    path: P4_COPPERLINE_ROUTE.community,
    purpose: 'Establish Copperline as ordinary community history and make mkept discoverable as a real account.',
  },
  {
    id: 'custodian-context',
    path: P4_COPPERLINE_ROUTE.accountFilter,
    purpose: 'Connect mkept\'s backup practice, privacy choices, and the disagreement between two retained copies.',
  },
  {
    id: 'disagreement-post',
    path: P4_COPPERLINE_ROUTE.disagreementPost,
    purpose: 'Turn an ordinary recovery post into a provenance question and expose the historical support reference.',
  },
  {
    id: 'ticket-custody',
    path: P4_COPPERLINE_ROUTE.custodyTicket,
    purpose: 'Authenticate cartridge order without interpreting the recovered Hold material.',
  },
  {
    id: 'retained-attachments',
    path: P4_COPPERLINE_ROUTE.retainedAttachments,
    purpose: 'Let the player restore a read-only copy and carry its index into Minecraft.',
  },
] as const;
