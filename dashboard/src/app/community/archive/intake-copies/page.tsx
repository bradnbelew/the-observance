import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import {
  copperlineP4DirectEntries,
  copperlineP4Entries,
  P4_COPPERLINE_OFFLINE_NOTICE,
} from '@/lib/copperline-p4-archive';

export const metadata: Metadata = {
  title: 'Ticket 2184 retained copy table - Copperline Community',
  robots: { index: false, follow: false },
};

export default function IntakeCopiesArchivePage() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive &raquo; Ticket 2184</Breadcrumbs>
      <OldPageTitle sub="Read-only cartridge and attachment custody retained with an expired game service.">
        Ticket 2184 / recovered cartridge order
      </OldPageTitle>
      <div className="old-alert" role="note"><b>Local review status.</b> {P4_COPPERLINE_OFFLINE_NOTICE}</div>
      <section className="old-copy" aria-labelledby="ticket-thread-heading">
        <h2 id="ticket-thread-heading">Ticket thread and retained attachments</h2>
        <p>The archive preserves author, time, reply order, and attachment state separately. A quoted or cached copy is not silently substituted for its source.</p>
        <ol className="community-posts" aria-label="Ticket 2184 chronological record">
          {copperlineP4DirectEntries.map((entry) => (
            <li key={entry.id}>
              <article>
                <div>
                  <h3>{entry.title}</h3>
                  <p className="old-post-meta"><b>{entry.author}</b> &middot; <time dateTime={entry.date}>{new Date(entry.date).toLocaleString('en-US', { timeZone: 'America/Chicago' })}</time> &middot; {entry.kind}</p>
                  <pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{entry.body}</pre>
                </div>
              </article>
            </li>
          ))}
        </ol>
      </section>
      <section className="old-copy" aria-labelledby="context-heading">
        <h2 id="context-heading">Nearby public archive context</h2>
        <p>Ticket 2184 sits among {copperlineP4Entries.length - copperlineP4DirectEntries.length} ordinary listings, notices, posts, and replies in this authored review slice. They are not numbered clue steps.</p>
        <p><Link href="/community/index.php">Return to the community archive</Link></p>
      </section>
      <footer className="ticket-end">Replies, uploads, and account actions are disabled. Prior versions remain read-only.</footer>
    </LegacyShell>
  );
}
