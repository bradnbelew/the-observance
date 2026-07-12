import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Support Center - Copperline Hosting" };

export default function SupportPage() {
  return <LegacyShell active="support"><Breadcrumbs>Support Center</Breadcrumbs><OldPageTitle sub="Browse common questions or check the network status.">Support Center</OldPageTitle><div className="support-grid"><section><h2>Game Server Help</h2><Link href="/support/index.php">Connecting with FTP</Link><Link href="/support/index.php">Restoring a world backup</Link><Link href="/support/index.php">Changing the server JAR</Link><Link href="/support/index.php">Accepting the resource pack</Link></section><section><h2>Billing &amp; Accounts</h2><Link href="/support/index.php">Updating account details</Link><Link href="/support/index.php">Past-due server retention</Link><Link href="/support/index.php">Requesting a final backup</Link><Link href="/clientarea.php">Legacy client area</Link></section></div><div className="old-message"><b>Support desk closed:</b> New tickets are no longer accepted. Existing ticket pages remain available in read-only mode when linked from an account listing.</div></LegacyShell>;
}
