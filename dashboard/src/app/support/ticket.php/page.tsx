import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Archived Ticket - Copperline Support" };
export const dynamic = "force-dynamic";

export default async function TicketPage({ searchParams }: { searchParams: Promise<{ id?: string }> }) {
  const id = (await searchParams).id ?? "";
  if (id !== "1851") return <LegacyShell active="support"><Breadcrumbs>Support » Ticket</Breadcrumbs><OldPageTitle>Ticket Not Available</OldPageTitle><div className="old-message error">That ticket is private, missing, or was removed from the archive.</div></LegacyShell>;
  return <LegacyShell active="support"><Breadcrumbs><Link href="/support/index.php">Support</Link> » Ticket #1851</Breadcrumbs><OldPageTitle>Ticket #1851</OldPageTitle><div className="ticket-summary"><div><b>Subject</b><span>final backup missing from panel</span></div><div><b>Department</b><span>Game Server Support</span></div><div><b>Status</b><span className="ticket-closed">Closed</span></div><div><b>Account</b><span>Server listing #1842</span></div></div><div className="ticket-thread"><article className="customer"><header><b>mkept</b><time>November 2, 2014 8:17 PM</time></header><p>The old account page is empty. I only need the last world archive that was in backup slot 3. Please do not restart or convert it.</p></article><article className="staff"><header><b>Andrew — Copperline Support</b><time>November 2, 2014 8:46 PM</time></header><p>The active storage for this service was removed in July. I found one off-site archive under the expired service ID and attached it to the account owner’s community post. We cannot verify the world contents.</p></article><article className="customer"><header><b>mkept</b><time>November 2, 2014 8:59 PM</time></header><p>That is the right copy. You can close this.</p></article></div><div className="ticket-end">This ticket is closed and cannot receive replies.</div></LegacyShell>;
}
