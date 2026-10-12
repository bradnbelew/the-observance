import type { Metadata } from "next";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Community - Copperline Hosting" };

export default function CommunityPage() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs>Community</Breadcrumbs>
      <OldPageTitle sub="Customer forums and server showcases.">Community</OldPageTitle>
      <div className="old-message">The Copperline community forums were retired during the 2013 site redesign. Threads still referenced by open support tickets were kept for the record; the rest were not imported.</div>
      <p>Looking for a customer&apos;s page? Every game plan came with free web space at <code>copperlinehosting.com/~name</code>.</p>
    </LegacyShell>
  );
}
