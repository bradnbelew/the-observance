export type CampaignCase = {
  phase: string;
  prerequisites: string[];
  evidence?: Array<{ id: string; title?: string; surface: string; content: string }>;
  dossiers?: Array<{ evidence: Array<{ id: string; surface: string; content: string }> }>;
  conclusions?: Array<{ id: string; prompt: string }>;
  group_conclusion?: { id: string; prompt: string };
  hints: Record<'H0' | 'H1' | 'H2' | 'H3', string>;
};

export type CampaignProjection = {
  phases: string[];
  observation_receipts_gate_answers: boolean;
  cases: CampaignCase[];
};

export function projectCase(
  projection: CampaignProjection,
  phase: string,
  completed: ReadonlySet<string>,
): CampaignCase | null {
  const candidate = projection.cases.find((entry) => entry.phase === phase);
  if (!candidate || !candidate.prerequisites.every((id) => completed.has(id))) return null;
  return candidate;
}

export function evidenceFor(caseFile: CampaignCase) {
  return [
    ...(caseFile.evidence ?? []),
    ...(caseFile.dossiers ?? []).flatMap((dossier) => dossier.evidence),
  ];
}

export function docketPrompts(caseFile: CampaignCase) {
  return [
    ...(caseFile.conclusions ?? []),
    ...(caseFile.group_conclusion ? [caseFile.group_conclusion] : []),
  ];
}
