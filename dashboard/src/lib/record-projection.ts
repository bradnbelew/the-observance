export interface RecordSignal {
  movement?: number | null;
  phaseKey?: string | null;
  currentCaseKey?: string | null;
  currentCaseTitle?: string | null;
  casesCompleted?: number | null;
  nodesCompleted?: number | null;
  totalNodes?: number | null;
  closed?: boolean | null;
  endingBranch?: string | null;
  nameTreatment?: string | null;
  wrenOutcome?: string | null;
}

export interface RecordProjection {
  movement: number;
  phaseKey: string;
  currentCaseKey: string | null;
  currentCaseTitle: string;
  casesCompleted: number;
  nodesCompleted: number;
  totalNodes: number;
  closed: boolean;
  endingBranch: string | null;
  nameTreatment: string | null;
  wrenOutcome: string | null;
  footer: string;
}

export const REDACTED_GLYPH = '████████';
export const OPEN_DOCKET_SUMMARY = 'Docket open. Recover and file evidence at its named surface.';

export interface PublicDocketSource {
  caseKey?: string | null;
  title?: string | null;
  summary?: string | null;
  complete?: boolean | null;
}

export interface PublicDocketProjection {
  caseKey: string | null;
  title: string;
  summary: string;
  complete: boolean;
}

/**
 * Keep the public progress UI useful without publishing the answer-bearing internal case labels.
 * Canonical titles and summaries become safe only after the case that they describe is complete.
 */
export function projectPublicDocket(source: PublicDocketSource): PublicDocketProjection {
  const rawKey = typeof source.caseKey === 'string' ? source.caseKey.trim().toUpperCase() : '';
  const caseKey = /^C(?:0[1-9]|10)$/.test(rawKey) ? rawKey : null;
  const complete = source.complete === true;
  const genericTitle = caseKey ? `Docket ${caseKey}` : 'Docket unavailable';
  const canonicalTitle = typeof source.title === 'string' && source.title.trim()
    ? source.title.trim()
    : genericTitle;
  const canonicalSummary = typeof source.summary === 'string' && source.summary.trim()
    ? source.summary.trim()
    : OPEN_DOCKET_SUMMARY;
  return {
    caseKey,
    title: complete ? canonicalTitle : genericTitle,
    summary: complete ? canonicalSummary : OPEN_DOCKET_SUMMARY,
    complete,
  };
}

function bounded(value: number | null | undefined, max: number): number {
  const parsed = Math.trunc(Number(value));
  return Number.isFinite(parsed) ? Math.max(0, Math.min(max, parsed)) : 0;
}

export function project(signal: RecordSignal): RecordProjection {
  const totalNodes = bounded(signal.totalNodes, 82) || 82;
  const nodesCompleted = bounded(signal.nodesCompleted, totalNodes);
  const casesCompleted = bounded(signal.casesCompleted, 10);
  const closed = signal.closed === true;
  const currentDocket = projectPublicDocket({
    caseKey: signal.currentCaseKey,
    title: signal.currentCaseTitle,
    complete: closed,
  });
  return {
    movement: Math.max(1, bounded(signal.movement, 5)),
    phaseKey: typeof signal.phaseKey === 'string' && signal.phaseKey ? signal.phaseKey : 'unavailable',
    currentCaseKey: currentDocket.caseKey,
    currentCaseTitle: currentDocket.title,
    casesCompleted,
    nodesCompleted,
    totalNodes,
    closed,
    endingBranch: closed && typeof signal.endingBranch === 'string' ? signal.endingBranch : null,
    nameTreatment: closed && typeof signal.nameTreatment === 'string' ? signal.nameTreatment : null,
    wrenOutcome: closed && typeof signal.wrenOutcome === 'string' ? signal.wrenOutcome : null,
    footer: closed
      ? 'the record is closed. the names were returned.'
      : `${nodesCompleted} of ${totalNodes} required findings entered. no case is optional.`,
  };
}
