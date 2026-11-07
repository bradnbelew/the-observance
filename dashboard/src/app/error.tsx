"use client";

import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

/** Any page that fails to render: the archive's own error page, never the framework's default. */
export default function PageError() {
  return (
    <LegacyShell active="home">
      <Breadcrumbs>Error</Breadcrumbs>
      <OldPageTitle sub="The archive server could not complete your request.">Temporarily Unavailable</OldPageTitle>
      <div className="old-message error">
        This page could not be loaded from the archive just now. Please wait a minute and reload the page.
      </div>
    </LegacyShell>
  );
}
