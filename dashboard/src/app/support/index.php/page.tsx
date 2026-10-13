import type { Metadata } from "next";
import Link from "next/link";
import { cookies } from "next/headers";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle, currentAct } from "@/lib/morrow/canon";
import { actClock, unseal } from "@/lib/morrow/campaign";
import { answered, deskNotes, fileLink, voiceLink, type Ticket } from "@/lib/morrow/desk";
import { VoiceNote } from "@/components/morrow/VoiceNote";

export const metadata: Metadata = { title: "Archive Desk - Copperline Hosting" };
export const dynamic = "force-dynamic";

/** The Archive Desk: Theo's notes, the player's own requests (kept in a cookie), and a new-request form. */
export default async function ArchiveDesk() {
  const b = await bundle();
  const mine = ((await cookies()).get("desk_tickets")?.value ?? "").split("~").map((t) => ({ t, v: unseal<Ticket & { c?: string }>(t) })).filter((x) => x.v);
  const notes = deskNotes(b, await actClock());
  return (
    <LegacyShell active="support">
      <Breadcrumbs>Archive Desk</Breadcrumbs>
      <OldPageTitle sub="Requests for retained customer records.">Archive Desk</OldPageTitle>
      {(await currentAct()) >= 6 ? (
        <table className="old-data-table desk-staff"><tbody>
          <tr><th>Desk staff</th><td>archivist (Theo, Copperline Support)</td></tr>
          <tr><th>Home Page</th><td><a href="/~tvale/">copperlinehosting.com/~tvale/</a></td></tr>
        </tbody></table>
      ) : null}
      <section className="old-copy">
        <h2>Notes from the desk</h2>
        {notes.map((n, i) => (
          <div key={i} className="whmcs-reply">
            <div className="whmcs-reply-head"><b>archivist</b><span>{n.at}</span></div>
            <pre className="whmcs-reply-body">{n.text}</pre>
            {n.voice ? <VoiceNote href={voiceLink(n.voice)} /> : null}
            {n.file ? <p className="desk-attach">Attachment: <a href={fileLink(n.file)}>{n.file}</a></p> : null}
          </div>
        ))}
      </section>
      {mine.length ? (
        <section className="old-copy">
          <h2>Your requests</h2>
          <table className="old-data-table"><tbody>{mine.map(({ v }) => (
            <tr key={v!.tid}><td>#{v!.tid}</td><td><Link href={`/viewticket.php?tid=${v!.tid}&c=${v!.c}`}>{v!.subject || "(no subject)"}</Link></td>
              <td>{answered(v!.replyAt) ? "Answered" : "Awaiting reply"}</td></tr>
          ))}</tbody></table>
        </section>
      ) : null}
      <section className="old-copy">
        <h2>Open a request</h2>
        <p>If you were a customer, or you are looking for something specific, open a request. I check the desk most evenings.  Not great at chat, sorry.</p>
        <form method="post" action="/submitticket.php" className="old-form">
          <label>Your name<input name="name" autoComplete="off" aria-label="Your name" /></label>
          <label>Subject<input name="subject" aria-label="Subject" /></label>
          <label>Request<textarea name="message" rows={7} aria-label="Request" /></label>
          <button type="submit">Submit request</button>
        </form>
        {b.case_brief ? <p><Link href="/support/brief.php">Case brief (case 6118)</Link></p> : null}
      </section>
    </LegacyShell>
  );
}
