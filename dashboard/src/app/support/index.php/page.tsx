import type { Metadata } from "next";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Archive Desk - Copperline Hosting" };
export const dynamic = "force-dynamic";

/** The Archive Desk. Theo answers here, in the evenings (replies begin in Act 4). */
export default async function ArchiveDesk({ searchParams }: { searchParams: Promise<{ sent?: string }> }) {
  const sent = (await searchParams).sent;
  return (
    <LegacyShell active="support">
      <Breadcrumbs>Archive Desk</Breadcrumbs>
      <OldPageTitle sub="Requests for retained customer records.">Archive Desk</OldPageTitle>
      {sent ? (
        <div className="old-alert"><b>Request #{sent} received.</b> Thanks!  I check the desk most evenings, so it may be a few hours.</div>
      ) : (
        <p>This mirror keeps a small number of customer records that were never deleted. If you were a customer, or you are looking for something specific, open a request below. I check the desk most evenings. Not great at chat, sorry.</p>
      )}
      <form method="post" action="/submitticket.php" className="old-form">
        <label>Your name<input name="name" autoComplete="off" aria-label="Your name" /></label>
        <label>Email (optional)<input name="email" type="email" autoComplete="off" aria-label="Email" /></label>
        <label>Subject<input name="subject" aria-label="Subject" /></label>
        <label>Request<textarea name="message" rows={7} aria-label="Request" /></label>
        <button type="submit">Submit request</button>
      </form>
    </LegacyShell>
  );
}
