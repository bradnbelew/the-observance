import { Breadcrumbs, LegacyShell } from '@/components/legacy/LegacyShell';

export default function LoadingMorrowCase() {
  return <LegacyShell active="support"><Breadcrumbs>Support &raquo; Recovery Cases</Breadcrumbs>
    <article className="morrow-case-workbench" aria-busy="true" aria-live="polite">
      <header className="morrow-case-head"><div><p className="morrow-case-kicker">Backup &amp; Recovery</p><h1>Loading assigned recovery case…</h1>
        <p>Checking campaign, player, release, and current projection receipts.</p></div></header>
      <section className="morrow-case-panel morrow-case-state"><h2>Projection read in progress</h2><p>No locked case material is included in this loading state.</p></section>
    </article></LegacyShell>;
}
