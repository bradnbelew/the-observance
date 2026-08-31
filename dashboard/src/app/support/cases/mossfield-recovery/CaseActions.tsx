'use client';

import { useActionState } from 'react';
import { MORROW_AUDIT_RECORDS } from '@/lib/morrow-copperline-case';
import { submitMorrowCaseAction, type MorrowCaseActionState } from './actions';

const INITIAL: MorrowCaseActionState = { status: 'idle', message: 'No verification submitted in this session.' };

export function ChecksumVerificationForm({ disabled }: { disabled: boolean }) {
  const [state, action, pending] = useActionState(submitMorrowCaseAction, INITIAL);
  return (
    <form action={action} className="morrow-case-form" aria-describedby="checksum-help checksum-result">
      <input type="hidden" name="operation" value="verify_case_chain" />
      <label htmlFor="case-checksum">Attachment SHA-256</label>
      <p id="checksum-help">Compare the attached text export with its 64-character custody checksum.</p>
      <div><input id="case-checksum" name="checksum" inputMode="text" autoComplete="off" spellCheck={false}
        minLength={64} maxLength={64} required disabled={disabled || pending} aria-label="Attachment SHA-256 checksum" />
      <button type="submit" disabled={disabled || pending}>{pending ? 'Verifying…' : disabled ? 'Chain authenticated' : 'Verify checksum'}</button></div>
      <ActionResult id="checksum-result" state={state} />
    </form>
  );
}

export function HandoffRecoveryForm({ enabled, recovered }: { enabled: boolean; recovered: boolean }) {
  const [state, action, pending] = useActionState(submitMorrowCaseAction, INITIAL);
  return (
    <form action={action} className="morrow-case-form" aria-describedby="handoff-help handoff-result">
      <input type="hidden" name="operation" value="recover_handoff" />
      <label htmlFor="handoff-token">Short handoff token</label>
      <p id="handoff-help">Use the two-part token from the authenticated attachment. Tokens use XXXX-XXXX.</p>
      <div><input id="handoff-token" name="handoffToken" autoCapitalize="characters" autoComplete="off"
        spellCheck={false} pattern="[A-Za-z0-9]{4}-[A-Za-z0-9]{4}" maxLength={9} required
        disabled={!enabled || recovered || pending} aria-label="Short server handoff token" />
      <button type="submit" disabled={!enabled || recovered || pending}>{pending ? 'Recovering…' : recovered ? 'Handoff recovered' : 'Recover handoff'}</button></div>
      <ActionResult id="handoff-result" state={state} />
    </form>
  );
}

export function AuditChronologyForm({ enabled, proven }: { enabled: boolean; proven: boolean }) {
  const [state, action, pending] = useActionState(submitMorrowCaseAction, INITIAL);
  if (proven) return <div className="morrow-audit-filed" role="status">
    <b>Five-record chronology authenticated.</b>
    <p>The receipt is projected to Cold Storage. Identity continuity remains unresolved.</p>
  </div>;
  return (
    <form action={action} className="morrow-case-form morrow-audit-form" aria-describedby="audit-help audit-result">
      <input type="hidden" name="operation" value="prove_audit_chronology" />
      <label>Build the incident custody chain</label>
      <p id="audit-help">Place each retained record at one edge. A wrong filing reports the first broken edge and changes nothing.</p>
      <ol>
        {MORROW_AUDIT_RECORDS.map((_, index) => <li key={index}>
          <label htmlFor={`audit-edge-${index + 1}`}>Edge {index + 1}</label>
          <select id={`audit-edge-${index + 1}`} name={`edge${index + 1}`} defaultValue="" required
            disabled={!enabled || pending}>
            <option value="" disabled>Select retained record</option>
            {MORROW_AUDIT_RECORDS.map((record) => <option key={record.id} value={record.id}>
              {record.title} · {record.source}
            </option>)}
          </select>
        </li>)}
      </ol>
      <button type="submit" disabled={!enabled || pending}>{pending ? 'Authenticating…' : 'Authenticate chronology'}</button>
      <ActionResult id="audit-result" state={state} />
    </form>
  );
}

function ActionResult({ id, state }: { id: string; state: MorrowCaseActionState }) {
  return <div id={id} className="morrow-case-action-result" role="status" aria-live="polite" data-status={state.status}>
    <span>{state.status}</span><p>{state.message}</p>{state.receipt ? <code>{state.receipt}</code> : null}
  </div>;
}
