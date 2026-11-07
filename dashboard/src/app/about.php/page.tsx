import type { Metadata } from "next";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "About - Copperline Hosting" };
export const dynamic = "force-dynamic";

/** Copperline's own history, 2008-2014, as the mirror keeps it. */
export default async function About() {
  const b = await bundle();
  return (
    <LegacyShell active="support">
      <Breadcrumbs>About Copperline</Breadcrumbs>
      <OldPageTitle sub="Copperline Hosting LLC, Chicago, 2008-2014.">About Copperline</OldPageTitle>
      <section className="old-copy">
        <p>Copperline was a small Chicago host: two racks on Cermak, a few leased boxes in Dallas and Atlanta, one person on support. It closed in November 2014. This mirror keeps what was still on the web box, read only.</p>
        <table className="old-data-table">
          <tbody>{(b.history ?? []).map(([when, what]) => <tr key={when}><td>{when}</td><td>{what}</td></tr>)}</tbody>
        </table>
      </section>
    </LegacyShell>
  );
}
