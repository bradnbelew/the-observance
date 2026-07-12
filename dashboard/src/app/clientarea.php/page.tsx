import type { Metadata } from "next";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Client Area - Copperline Hosting" };

export default function ClientAreaPage() {
  return <LegacyShell active="home"><Breadcrumbs>Client Area</Breadcrumbs><OldPageTitle>Client Area</OldPageTitle><div className="old-message error"><b>Legacy panel unavailable.</b><br />Authentication for this billing system was disabled on December 1, 2014. Public account and server-directory pages remain read-only.</div><div className="dead-login"><label>Email Address<input disabled /></label><label>Password<input type="password" disabled /></label><button disabled>Login</button><a>Forgot Password?</a></div></LegacyShell>;
}
