import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';

export const metadata: Metadata = {
  title: 'Archive Entry Not Found - Copperline Community',
  robots: { index: false, follow: false },
};
export const dynamic = 'force-dynamic';

function MissingArchiveEntry() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs>
      <OldPageTitle>Archive Entry Not Found</OldPageTitle>
      <div className="old-message error">No retained community entry is indexed at this route.</div>
    </LegacyShell>
  );
}

export default function RemoteRoomPage() {
  return <MissingArchiveEntry />;
}
