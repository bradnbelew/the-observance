import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "Incident 6118 - Copperline Status" };
export const dynamic = "force-dynamic";

export default async function Incident6118() {
  const b = await bundle();
  const inc = b.status_incident;
  const shown = inc.morrow_rewrite ?? inc.original;
  return (
    <LegacyShell active="support">
      <Breadcrumbs><Link href="/status">Network Status</Link> » Incident 6118</Breadcrumbs>
      <OldPageTitle sub={`Posted ${shown.posted}`}>{shown.title}</OldPageTitle>
      <section className="old-copy">
        <p>{shown.body}</p>
        {!inc.morrow_rewrite ? <p><b>Update:</b> {inc.original.update}</p> : null}
      </section>
      <p className="old-fineprint"><Link href="/status/incident-6118/export.csv">Export (CSV)</Link></p>
    </LegacyShell>
  );
}
