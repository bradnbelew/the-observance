import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export default function NotFound() {
  return (
    <LegacyShell active="home">
      <Breadcrumbs>Page Not Found</Breadcrumbs>
      <OldPageTitle sub="The preserved site does not contain the requested page.">404 — Page Not Found</OldPageTitle>
      <div className="old-message error">
        The requested file was not included in the final Copperline archive snapshot. Check the address or return to the home page.
      </div>
    </LegacyShell>
  );
}
