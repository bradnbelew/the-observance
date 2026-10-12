import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "Case Brief - Copperline Hosting" };
export const dynamic = "force-dynamic";

/** Act 9: the case brief that convinces Theo. Exact values only; he says which he can't match. */
export default async function CaseBrief() {
  const b = await bundle();
  if (!b.case_brief) notFound();
  return (
    <LegacyShell active="support">
      <Breadcrumbs><Link href="/support/index.php">Archive Desk</Link> » Case 6118 brief</Breadcrumbs>
      <OldPageTitle sub="Theo asked for it in writing.">Case 6118: brief</OldPageTitle>
      <form method="post" action="/dobrief.php" className="old-form">
        <label>Your name<input name="name" autoComplete="off" /></label>
        {b.case_brief.map((f) => <label key={f.id}>{f.label}<input name={f.id} autoComplete="off" /></label>)}
        <button type="submit">Send to the desk</button>
      </form>
    </LegacyShell>
  );
}
