import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { safeDiscordInvite } from '@/lib/discord-relay';
import { readV5CompletionFlag } from '@/lib/v5-web-progress';

export const metadata: Metadata = {
  title: 'Archived Field Relay - Copperline Community',
  robots: { index: false, follow: false },
};
export const dynamic = 'force-dynamic';

function MissingRelay() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs>
      <OldPageTitle>Archive Entry Not Found</OldPageTitle>
      <div className="old-message error">No retained field relay is indexed at this route.</div>
    </LegacyShell>
  );
}

export default async function RemoteRoomPage() {
  const archiveSolved = await readV5CompletionFlag('v5_ls04_archive_solved');
  if (!archiveSolved.complete) return <MissingRelay />;

  const invite = safeDiscordInvite(process.env.DISCORD_INVITE_URL);
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Field Relay</Breadcrumbs>
      <OldPageTitle sub="Destination retained outside the expired customer panel.">Service 1842 Field Relay</OldPageTitle>
      <div className="ticket-summary">
        <div><b>Service</b><span>1842</span></div>
        <div><b>Ticket</b><span>9137</span></div>
        <div><b>State</b><span className="ticket-closed">Read only</span></div>
        <div><b>Destination</b><span>coordination room</span></div>
      </div>
      {invite ? (
        <section className="relay-result">
          <h2>Remote room recovered</h2>
          <p>The coordination room still accepts arrivals. Open it before continuing to the live Minecraft server.</p>
          <a href={invite} rel="nofollow noreferrer" target="_blank">Open coordination room</a>
          <div className="relay-linking">
            <b>Identity binding required</b>
            <p>Join Minecraft under your exact username and run <code>/obslink</code>. Keep the one-time code shown only to you; it expires after five minutes.</p>
            <p>In Discord, file all three fields with <code>/link YourExactMinecraftUsername CallbackFromArchive OneTimeCode</code>.</p>
            <p>The Watcher consumes that proof and binds one Discord account to that Minecraft identity in the same transaction as the durable Handoff Receipt. An exact network retry cannot duplicate the receipt.</p>
          </div>
        </section>
      ) : (
        <div className="old-message error"><b>Destination unavailable:</b> the retained route is valid, but the coordination room has not been configured.</div>
      )}
      <div className="ticket-end">Copperline staff cannot reset, disclose, or replace the archived callback.</div>
    </LegacyShell>
  );
}
