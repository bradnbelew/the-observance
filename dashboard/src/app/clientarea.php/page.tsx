import type { Metadata } from "next";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Client Area - Copperline Hosting" };

export default function ClientAreaPage() {
  return <LegacyShell active="home"><Breadcrumbs>Client Area</Breadcrumbs><OldPageTitle sub="WHMCS customer billing and service management">Client Area</OldPageTitle><div className="old-message error"><b>Authentication service removed.</b><br />Customer sessions, password resets, payments, and file access were disabled on December 1, 2014. Public directory records remain read-only.</div><div className="dead-login" aria-label="retired customer login"><label>Email Address<input disabled aria-label="Email Address" /></label><label>Password<input type="password" disabled aria-label="Password" /></label><button disabled>Login</button><span className="dead-link">Password reset unavailable</span></div><p className="old-fineprint">This preserved form does not submit or store information.</p></LegacyShell>;
}
