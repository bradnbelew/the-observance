import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell } from '@/components/legacy/LegacyShell';
import {
  MORROW_CASE_ATTACHMENT_SHA256,
  MORROW_CASE_ATTACHMENT_TEXT,
  MORROW_CASE_ID,
  type MorrowCaseContext,
} from '@/lib/morrow-copperline-case';
import { readMorrowCase, type MorrowServerRead } from '@/lib/morrow-copperline-server';
import { ChecksumVerificationForm, HandoffRecoveryForm } from './CaseActions';

export const dynamic = 'force-dynamic';
export const metadata: Metadata = {
  title: 'Unresolved Recovery Case - Copperline Support',
  description: 'Release-bound Copperline recovery case, custody verification, and synchronized field updates.',
  robots: { index: false, follow: false },
};

export default async function MossfieldRecoveryCasePage() {
  const state = await readMorrowCase();
  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; Recovery Cases &raquo; {MORROW_CASE_ID}</Breadcrumbs>
    {state.kind === 'ready' ? <CaseWorkbench context={state} /> : <UnavailableCase state={state} />}
  </LegacyShell>;
}

function CaseWorkbench({ context }: { context: MorrowCaseContext }) {
  const status = context.handoffRecovered ? 'field handoff issued'
    : context.caseChainAuthenticated ? 'custody authenticated' : 'unresolved / awaiting verification';
  return <article className="morrow-case-workbench">
    <header className="morrow-case-head">
      <div><p className="morrow-case-kicker">Backup &amp; Recovery · Case {MORROW_CASE_ID}</p>
        <h1>Damaged game-server recovery</h1>
        <p>Working copy assigned to <b>{context.displayAlias}</b>. Read-only until a concrete verification is submitted.</p></div>
      <dl><div><dt>Status</dt><dd data-state={context.handoffRecovered ? 'issued' : 'open'}>{status}</dd></div>
        <div><dt>Release</dt><dd><code>{context.releaseId}</code></dd></div>
        <div><dt>Participants</dt><dd>{context.groupSize} linked / shared case</dd></div>
        <div><dt>Projection</dt><dd>revision {context.revision}</dd></div></dl>
    </header>

    <div className="morrow-case-columns">
      <main>
        <section className="morrow-case-panel" aria-labelledby="custody-heading">
          <header><h2 id="custody-heading">Custody chain</h2><span>3 retained entries</span></header>
          <ol className="morrow-custody-list">
            <li><b>01 · controller export</b><p>Damaged region inventory copied from the failed storage controller. Read-only source.</p><code>source / storage-controller</code></li>
            <li><b>02 · recovery intake</b><p>Iona Bell logged the export without mounting or normalizing the affected world.</p><code>custody / copperline-recovery</code></li>
            <li><b>03 · support attachment</b><p>The handoff text was attached to this unresolved case with a fixed SHA-256.</p><code>custody / case-{MORROW_CASE_ID.toLowerCase()}</code></li>
          </ol>
        </section>

        <section className="morrow-case-panel" aria-labelledby="attachment-heading">
          <header><h2 id="attachment-heading">Accessible attachment metadata</h2><span>not executable</span></header>
          <div className="morrow-attachment">
            <div className="morrow-file-badge" aria-hidden="true">TXT</div>
            <div><b>recovery-room-04-handoff.txt</b><p>Plain UTF-8 text · 285 bytes · transcript-equivalent content · no audio-only instructions</p>
              <dl><div><dt>Modified</dt><dd>2014-11-03 01:42 CST</dd></div><div><dt>Custody</dt><dd>entries 01–03 above</dd></div>
                <div><dt>SHA-256</dt><dd><code>{MORROW_CASE_ATTACHMENT_SHA256}</code></dd></div></dl></div>
          </div>
          <details className="morrow-attachment-text"><summary>Open accessible text attachment</summary><pre>{MORROW_CASE_ATTACHMENT_TEXT}</pre></details>
          <ChecksumVerificationForm disabled={context.caseChainAuthenticated} />
        </section>

        <section className="morrow-case-panel" aria-labelledby="handoff-heading">
          <header><h2 id="handoff-heading">Server handoff recovery</h2><span>{context.caseChainAuthenticated ? 'available' : 'locked by custody'}</span></header>
          <p className="morrow-case-note">The address is not stored in this page or its locked payload. A valid short token creates a release-bound handoff projection.</p>
          {context.caseChainAuthenticated && context.handoffToken
            ? <div className="morrow-token-release"><span>Authenticated attachment field</span><code>{context.handoffToken}</code></div>
            : context.caseChainAuthenticated ? <div className="morrow-case-waiting" role="status"><b>Custody authenticated.</b> Waiting for the scoped token projection; refresh is safe.</div> : null}
          <HandoffRecoveryForm enabled={context.caseChainAuthenticated && context.handoffToken !== null} recovered={context.handoffRecovered} />
          {context.handoffRecovered && !context.handoff ? <div className="morrow-case-waiting" role="status"><b>Receipt accepted.</b> Waiting for the synchronized handoff projection; refresh is safe.</div> : null}
          {context.handoff ? <div className="morrow-handoff-success"><span>Recovered field destination</span><b>{context.handoff.serverLabel}</b>
            <code>{context.handoff.joinAddress}</code><small>Release {context.releaseId} · receipt {context.handoff.recoveryReceipt}</small></div> : null}
        </section>

        <section className="morrow-case-panel" aria-labelledby="updates-heading">
          <header><h2 id="updates-heading">Synchronized field updates</h2><span>earned only · group and player scopes labeled</span></header>
          {context.updates.length === 0 ? <div className="morrow-case-empty"><b>No field updates received.</b><p>The case remains open. Minecraft can continue during a projection outage; this page will not invent progress.</p></div>
            : <ol className="morrow-update-list">{context.updates.map((update) => <li key={update.eventKey} data-state={update.state}>
                <div><span>{update.timeLabel}</span><em>{update.scope}</em></div><b>{update.title}</b><p>{update.detail}</p>
                <code>{update.eventKey}</code></li>)}</ol>}
        </section>
      </main>

      <aside>
        <section className="morrow-case-panel"><header><h2>Case controls</h2><span>player-owned</span></header>
          <dl className="morrow-case-facts"><div><dt>Signed-in assignment</dt><dd>{context.displayAlias}</dd></div>
            <div><dt>Shared progress</dt><dd>{context.groupSize} participant{context.groupSize === 1 ? '' : 's'}</dd></div>
            <div><dt>Private receipts</dt><dd>{context.playerReceipts.length}</dd></div>
            <div><dt>Raw replay samples</dt><dd>not received</dd></div></dl></section>
        <section className="morrow-case-panel"><header><h2>Your receipts</h2><span>player-specific</span></header>
          {context.playerReceipts.length ? <ul className="morrow-receipts">{context.playerReceipts.map((receipt) => <li key={receipt}><code>{receipt}</code></li>)}</ul>
            : <p className="morrow-sidebar-empty">No player-specific web receipts yet. Group updates can still appear above.</p>}</section>
        <section className="morrow-case-help"><b>Archive behavior</b><p>Escape, reload, duplicate submission, or a temporary outage cannot advance this case. Locked updates are omitted rather than teased.</p></section>
      </aside>
    </div>
  </article>;
}

function UnavailableCase({ state }: { state: Exclude<MorrowServerRead, MorrowCaseContext> }) {
  const unavailable = state.kind === 'unavailable';
  const error = state.kind === 'error';
  return <article className="morrow-case-workbench">
    <header className="morrow-case-head"><div><p className="morrow-case-kicker">Backup &amp; Recovery · Case {MORROW_CASE_ID}</p>
      <h1>Recovery case unavailable</h1><p>This support route exposes no case material until one authenticated player assignment is current.</p></div>
      <dl><div><dt>Status</dt><dd data-state="open">{unavailable ? 'projection service unavailable' : error ? 'release or projection mismatch' : 'no assigned case'}</dd></div>
        <div><dt>Case data</dt><dd>withheld</dd></div></dl></header>
    <section className="morrow-case-panel morrow-case-state" role={unavailable || error ? 'alert' : 'status'}>
      <h2>{unavailable ? 'Temporary maintenance state' : error ? 'Case audit halted' : 'No linked case found'}</h2>
      <p>{unavailable
        ? 'The projection service did not answer. Minecraft progress remains local and this page will catch up without duplicate actions.'
        : error ? 'Campaign, player, release, or event ordering did not match this deployment. No attachment, token, spoiler, or destination was rendered.'
          : 'Sign in with the account linked to the current campaign. Direct links and retired assignments reveal no case content.'}</p>
      {state.kind === 'empty'
        ? <Link href="/support/account?next=/support/cases/mossfield-recovery">Sign in to this recovery case &raquo;</Link>
        : <Link href="/support/index.php">Return to Support Center &raquo;</Link>}
    </section>
  </article>;
}
