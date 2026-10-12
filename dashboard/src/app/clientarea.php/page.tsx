import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle } from "@/lib/morrow/canon";
import { sessionUser } from "@/lib/morrow/session";
import { Motd } from "@/components/morrow/Motd";
import { fileLink } from "@/lib/morrow/desk";

export const metadata: Metadata = { title: "Client Area - Copperline Hosting" };
export const dynamic = "force-dynamic";

type Search = Promise<{ action?: string; incorrect?: string; id?: string }>;

export default async function ClientArea({ searchParams }: { searchParams: Search }) {
  const { action, incorrect } = await searchParams;
  const user = await sessionUser();
  const b = await bundle();
  const acct = b.client_account;

  if (!user) {
    return (
      <LegacyShell active="support">
        <Breadcrumbs>Client Area</Breadcrumbs>
        <OldPageTitle sub="This page is restricted. Please log in to continue.">Client Area</OldPageTitle>
        {incorrect ? <div className="old-message error">Login Details Incorrect. Please try again.</div> : null}
        <form method="post" action="/dologin.php" className="old-form">
          <label>Username<input name="username" autoComplete="off" aria-label="Username" /></label>
          <label>Password<input name="password" type="password" aria-label="Password" /></label>
          <button type="submit">Login</button>
          <p><Link href="/pwreset.php">Forgotten your password?</Link></p>
        </form>
      </LegacyShell>
    );
  }

  const tabs: [string, string][] = [["", "Overview"], ["productdetails", "My Services"], ["invoices", "My Invoices"], ["tickets", "My Tickets"], ["ftp", "FTP Access"]];
  const s = acct.service;

  return (
    <LegacyShell active="support">
      <Breadcrumbs>Client Area</Breadcrumbs>
      <OldPageTitle sub={`Logged in as ${user}`}>Client Area</OldPageTitle>
      <nav className="whmcs-tabs">
        {tabs.map(([key, label]) => (
          <Link key={key} href={key ? `/clientarea.php?action=${key}` : "/clientarea.php"} className={(action ?? "") === key ? "active" : ""}>{label}</Link>
        ))}
        <Link href="/logout.php">Logout</Link>
      </nav>

      {(!action || action === "productdetails") && (
        <section className="old-copy">
          <h2>{s.label}</h2>
          <table className="old-data-table"><tbody>
            <tr><th>Product</th><td>{s.product}</td></tr>
            <tr><th>Node</th><td>{s.node}</td></tr>
            <tr><th>Server address</th><td><code>{s.address}</code></td></tr>
            <tr><th>Status</th><td><span className="old-status online">{s.status}</span></td></tr>
            <tr><th>Players online</th><td>{s.players_online} / 20</td></tr>
            <tr><th>Message of the day</th><td><Motd raw={b.motd} /></td></tr>
            <tr><th>Next due date</th><td>N/A (fees waived)</td></tr>
          </tbody></table>
          {!action && <p className="old-fineprint">Billing was suspended for this service. Hosting continues on the retained node.</p>}
        </section>
      )}

      {action === "invoices" && (
        <table className="old-data-table">
          <thead><tr><th>Invoice #</th><th>Invoice Date</th><th>Total</th><th>Status</th></tr></thead>
          <tbody>{acct.invoices.map((i) => <tr key={i.no}><td>{i.no}</td><td>{i.date}</td><td>{i.amount}</td><td>{i.status}</td></tr>)}</tbody>
        </table>
      )}

      {action === "tickets" && (
        <section className="old-copy">
          <table className="old-data-table">
            <thead><tr><th>Ticket #</th><th>Department</th><th>Subject</th><th>Status</th></tr></thead>
            <tbody><tr><td>6118</td><td>{b.ticket_6118.dept}</td><td><Link href="/clientarea.php?action=viewticket">{b.ticket_6118.subject}</Link></td><td>{b.ticket_6118.status_public}</td></tr></tbody>
          </table>
        </section>
      )}

      {action === "viewticket" && (
        <section className="old-copy">
          <h2>Ticket #6118 - {b.ticket_6118.subject}</h2>
          {[...b.ticket_6118.messages].sort((x, y) => x.at.localeCompare(y.at)).map((m, i) => (
            <div key={i} className="whmcs-reply">
              <div className="whmcs-reply-head"><b>{m.from}</b><span>{m.at}</span></div>
              <pre className="whmcs-reply-body">{m.body}</pre>
              {m.attach ? <p className="desk-attach">Attachment: <a href={fileLink(m.attach)}>{m.attach}</a></p> : null}
            </div>
          ))}
        </section>
      )}

      {action === "ftp" && (
        <section className="old-copy">
          <table className="old-data-table"><tbody>
            <tr><th>Host</th><td><code>{acct.ftp.host}</code></td></tr>
            <tr><th>Username</th><td><code>{acct.ftp.user}</code></td></tr>
            <tr><th>Status</th><td>{acct.ftp.note_act1}</td></tr>
            <tr><th>Web access</th><td><Link href="/ftp/" prefetch={false}>ftp.copperlinehosting.com (browser)</Link></td></tr>
          </tbody></table>
        </section>
      )}
    </LegacyShell>
  );
}
