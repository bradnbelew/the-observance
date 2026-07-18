export type DocketProjection = {
  phase: string;
  prerequisites: string[];
  conclusions?: Array<{ id: string; prompt: string; zero_observation_acceptance: boolean }>;
  group_conclusion?: { id: string; prompt: string; zero_observation_acceptance: boolean };
  hints: Record<'H0' | 'H1' | 'H2' | 'H3', string>;
};

export function buildDocket(
  cases: readonly DocketProjection[],
  phase: string,
  completed: ReadonlySet<string>,
) {
  const caseFile = cases.find((entry) => entry.phase === phase);
  if (!caseFile || !caseFile.prerequisites.every((id) => completed.has(id))) return null;
  const prompts = [...(caseFile.conclusions ?? []), ...(caseFile.group_conclusion ? [caseFile.group_conclusion] : [])];
  if (!prompts.every((prompt) => prompt.zero_observation_acceptance)) {
    throw new Error(`campaign docket ${phase} contains observation-gated answer`);
  }
  return { phase, prompts, hints: caseFile.hints };
}
