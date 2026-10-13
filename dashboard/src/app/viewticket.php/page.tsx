import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle } from "@/lib/morrow/canon";
import { unseal } from "@/lib/morrow/campaign";
import { after } from "next/server";
import { deskEvent, fileLink, replyFor, voiceLink, type Ticket } from "@/lib/morrow/desk";
import { postDeskEvent } from "@/lib/morrow/bridge";
import { VoiceNote } from "@/components/morrow/VoiceNote";

export const metadata: Metadata = { title: "View Ticket - Copperline Hosting" };
export const dynamic = "force-dynamic";

export default async function ViewTicket({ searchParams }: { searchParams: Promise<{ tid?: string; c?: string }> }) {
  const { c } = await searchParams;
  const t = unseal<Ticket>(c);
  const b = await bundle();
  if (!t) {
    return (
      <LegacyShell active="support">
        <Breadcrumbs><Link href="/support/index.php">Archive Desk</Link> » Ticket</Breadcrumbs>
        <div className="old-message error">Ticket not found. Check the link and try again.</div>
      </LegacyShell>
    );
  }
  const reply = replyFor(b, t);
  // Evidence filed before its act was held; once the act has caught up, viewing the ticket tells the
  // plugin again (POST /event is idempotent there: a repeat answers "duplicate").
  const ev = t.kind === "rule" || t.kind === "file" ? deskEvent(b, t) : null;
  if (ev) after(() => postDeskEvent(ev));
  const opened = new Date(t.at).toLocaleString("en-US", { timeZone: "America/Chicago" });
  return (
    <LegacyShell active="support">
      <Breadcrumbs><Link href="/support/index.php">Archive Desk</Link> » Ticket #{t.tid}</Breadcrumbs>
      <OldPageTitle sub={`Opened ${opened} (Chicago)`}>{`#${t.tid} - ${t.subject || "(no subject)"}`}</OldPageTitle>
      <div className="whmcs-reply">
        <div className="whmcs-reply-head"><b>{t.name || "Guest"}</b><span>{opened}</span></div>
        <pre className="whmcs-reply-body">{t.message || "(message text not stored in this copy)"}</pre>
      </div>
      {reply ? (
        <div className="whmcs-reply staff">
          <div className="whmcs-reply-head"><b>archivist</b><span>{new Date(t.replyAt).toLocaleString("en-US", { timeZone: "America/Chicago" })}</span></div>
          <pre className="whmcs-reply-body">{reply.text}</pre>
          {reply.voice ? <VoiceNote href={voiceLink(reply.voice)} /> : null}
          {reply.attach ? <p className="desk-attach">Attachment: <a href={fileLink(reply.attach)}>{reply.attach}</a></p> : null}
        </div>
      ) : (
        <div className="old-alert"><b>Awaiting reply.</b> The desk is usually answered in the evening (Chicago time). Bookmark this page.</div>
      )}
      <p className="old-fineprint">Keep this link: it is the only copy of your ticket.</p>
    </LegacyShell>
  );
}
