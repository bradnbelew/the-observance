export type MorrowDiscordProjectionCopy = {
  heading: string;
  body: string;
};

const SYNCHRONIZED_RECEIPTS: Readonly<Record<string, MorrowDiscordProjectionCopy>> = Object.freeze({
  'morrow.act0.case_chain_authenticated': {
    heading: 'Copperline custody authenticated',
    body: 'The Mossfield recovery case remains unresolved. No server address or private attachment field is repeated here.',
  },
  'morrow.act0.server_handoff_recovered': {
    heading: 'Mossfield handoff recovered',
    body: 'The release-bound destination is available inside the linked Copperline case. This public receipt contains no join address.',
  },
  'morrow.act1.static_proposal_authenticated': {
    heading: 'Bounded Static Restore proposal filed',
    body: 'Six proposed cells entered physical review. This receipt authenticates the proposal, not every cell as history.',
  },
  'morrow.act1.intention_error_proven': {
    heading: 'Provenance exception preserved',
    body: 'One proposed cell had planning support but no placement source. Its classification remains inferred.',
  },
  'morrow.act1.entity_replay_authorized': {
    heading: 'Entity Replay authorized',
    body: 'The group approved one bounded reconstruction. This receipt does not authorize general player capture.',
  },
  'morrow.act2.live_capture_authorized': {
    heading: 'Bounded Live Capture authorized',
    body: 'Every recorded participant filed an individual consent receipt. Group approval cannot substitute for player consent.',
  },
  'morrow.act3.incomplete_consensus_proven': {
    heading: 'Incomplete consensus filed',
    body: 'All six signed votes remain intact. The published denominator excluded two authentic shutdown votes under its confidence policy.',
  },
  'morrow.act4.account_continuity_authorized': {
    heading: 'Account Continuity test authorized',
    body: 'One bounded identity test may proceed. Authorization does not certify a reconstructed echo as an original person.',
  },
  'morrow.act5.continued_session_observed': {
    heading: 'Session-continuity exception observed',
    body: 'A bounded session continued after disconnect. This receipt does not claim the absent player remained present.',
  },
  'morrow.act5.dual_session_consciousness_proven': {
    heading: 'Maintenance-window branches retained',
    body: 'Both sessions supplied the fresh witnessed nonce. Neither branch was selected as authoritative or silently deleted.',
  },
  'morrow.act6.audit_chronology_proven': {
    heading: 'Copperline chronology authenticated',
    body: 'The retained records establish custody order. The chain deliberately makes no identity-continuity ruling.',
  },
  'morrow.act6.current_morrow_reconstruction_proven': {
    heading: 'Current Morrow provenance filed',
    body: 'The active instance derives from recovery snapshots assembled after the original process closed. Personhood remains unclassified.',
  },
  'morrow.act6.cold_storage_access_authorized': {
    heading: 'Cold Storage comparison authorized',
    body: 'The group approved bounded access. Diverged records remain separate, labeled, and auditable.',
  },
  'morrow.act7.branch_policy_committed': {
    heading: 'Continuity policy filed',
    body: 'A physical branch policy was assembled and retained. Exact ending predicates remain in the authenticated case and world.',
  },
  'morrow.act7.branch_governance_authorized': {
    heading: 'Branch governance authorized',
    body: 'The group explicitly accepted the configured policy. Silence and timeout were not treated as consent.',
  },
  'morrow.act7.coda_started': {
    heading: 'Persistent coda opened',
    body: 'The selected continuity is active with its provenance, uncertainty, and stop conditions still attached.',
  },
});

export const MORROW_GENERIC_DISCORD_EVENT_KEYS = Object.freeze(Object.keys(SYNCHRONIZED_RECEIPTS));

export function morrowDiscordProjectionCopy(eventKey: string): MorrowDiscordProjectionCopy | null {
  return SYNCHRONIZED_RECEIPTS[eventKey] ?? null;
}

export function renderMorrowDiscordProjection(eventKey: string, releaseId: string, receiptId: string): string | null {
  const copy = morrowDiscordProjectionCopy(eventKey);
  if (!copy) return null;
  return [
    `**Morrow / ${copy.heading}**`,
    copy.body,
    `Release: ${releaseId} · receipt ${receiptId}`,
  ].join('\n');
}
