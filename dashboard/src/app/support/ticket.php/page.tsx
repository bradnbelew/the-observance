import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import {
  RELAY_TICKET_ID,
  relayCallbackMatches,
  safeDiscordInvite,
} from "@/lib/discord-relay";

export const metadata: Metadata = {
  title: "Archived Ticket - Copperline Support",
  robots: { index: false, follow: false },
};
export const dynamic = "force-dynamic";

type TicketSearch = { id?: string; code?: string };

function MissingTicket() {
  return <LegacyShell active="support"><Breadcrumbs>Support » Ticket</Breadcrumbs><OldPageTitle>Ticket Not Available</OldPageTitle><div className="old-message error">That ticket is private, missing, or was removed from the archive.</div></LegacyShell>;
}

function ClosedBackupTicket() {
  return <LegacyShell active="support"><Breadcrumbs><Link href="/support/index.php">Support</Link> » Ticket #1851</Breadcrumbs><OldPageTitle>Ticket #1851</OldPageTitle><div className="ticket-summary"><div><b>Subject</b><span>final backup missing from panel</span></div><div><b>Department</b><span>Game Server Support</span></div><div><b>Status</b><span className="ticket-closed">Closed</span></div><div><b>Account</b><span>Server listing #1842</span></div></div><div className="ticket-thread"><article className="customer"><header><b>mkept</b><time>November 2, 2014 8:17 PM</time></header><p>The old account page is empty. I only need the last world archive that was in backup slot 3. Please do not restart or convert it.</p></article><article className="staff"><header><b>Andrew — Copperline Support</b><time>November 2, 2014 8:46 PM</time></header><p>The active storage for this service was removed in July. I found one off-site archive under the expired service ID and attached it to the account owner&apos;s community post. We cannot verify the world contents.</p></article><article className="customer"><header><b>mkept</b><time>November 2, 2014 8:59 PM</time></header><p>That is the right copy. You can close this.</p></article></div><div className="ticket-end">This ticket is closed and cannot receive replies.</div></LegacyShell>;
}

function RelayTicket({ code }: { code?: string }) {
  const attempted = typeof code === "string" && code.trim().length > 0;
  const accepted = relayCallbackMatches(code);
  const invite = accepted ? safeDiscordInvite(process.env.DISCORD_INVITE_URL) : null;

  return <LegacyShell active="support"><Breadcrumbs><Link href="/support/index.php">Support</Link> » Service #1842 » Remote callback</Breadcrumbs><OldPageTitle sub="Read-only recovery form retained with the cancelled service record.">Remote Callback — Service #1842</OldPageTitle><div className="ticket-summary"><div><b>Subject</b><span>field relay / after-hours room</span></div><div><b>Department</b><span>Network Operations</span></div><div><b>Status</b><span className="ticket-closed">Archive only</span></div><div><b>Last node</b><span>Field relay — callback required</span></div></div><div className="ticket-thread"><article className="staff"><header><b>Copperline NOC</b><time>July 19, 2014 1:13 AM</time></header><p>The remote room was removed from the customer panel when service 1842 expired. Its destination can still be recovered from a passing callback filed at the physical relay. Callback order follows copper jacket age, not rack position.</p></article></div><form className="relay-form" method="get" action="/support/ticket.php"><input type="hidden" name="id" value={RELAY_TICKET_ID} /><label htmlFor="relay-code">Four-digit callback</label><div><input id="relay-code" name="code" inputMode="numeric" autoComplete="off" maxLength={12} defaultValue={code ?? ""} /><button type="submit">Recover remote room</button></div><small>Archived recovery is exact. Failed callbacks are not retained.</small></form>{attempted && !accepted ? <div className="old-message error">No endpoint was filed under that callback.</div> : null}{accepted && !invite ? <div className="old-message error"><b>Endpoint unavailable:</b> The callback is valid, but the remote destination did not answer. Try again later.</div> : null}{accepted && invite ? <section className="relay-result"><h2>Remote room recovered</h2><p>The old room is still accepting arrivals. Open it before the callback window closes.</p><a href={invite} rel="nofollow noreferrer" target="_blank">Open remote room</a><div className="relay-linking"><b>Identity binding required</b><p>In the room, run <code>/link YourExactMinecraftUsername</code>. Use the same spelling and capitalization you used in the live server.</p><p>After every player has linked, gather in <code>#the-record</code> and type <code>kept</code> once. An unlinked post is not recognized as a keeper.</p></div></section> : null}<div className="ticket-end">Copperline staff cannot reset, disclose, or manually replace a callback.</div></LegacyShell>;
}

export default async function TicketPage({ searchParams }: { searchParams: Promise<TicketSearch> }) {
  const params = await searchParams;
  if (params.id === "1851") return <ClosedBackupTicket />;
  if (params.id === RELAY_TICKET_ID) return <RelayTicket code={params.code} />;
  return <MissingTicket />;
}
