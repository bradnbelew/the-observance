import { UNIVERSAL_GOODBYE } from '@/lib/v5-cases';

function value(flags: Record<string, unknown>, key: string): string {
  const raw = flags[key];
  return typeof raw === 'string' && raw ? raw : '—';
}

export function V5FinalePanel({ flags }: { flags: Record<string, unknown> }) {
  return (
    <section className="director-card">
      <h3>Release Protocol state</h3>
      <p>The Minecraft finale is the authority. This panel reports durable state and never substitutes an operator-only ending.</p>
      <dl className="mt-4 grid gap-2 sm:grid-cols-2">
        <div><dt>Armed</dt><dd>{flags.v5_finale_armed === true ? 'yes' : 'no'}</dd></div>
        <div><dt>Phase</dt><dd>{value(flags, 'v5_finale_phase')}</dd></div>
        <div><dt>Wren outcome</dt><dd>{value(flags, 'v5_wren_outcome')}</dd></div>
        <div><dt>Name treatment</dt><dd>{value(flags, 'v5_name_treatment')}</dd></div>
        <div><dt>Closure branch</dt><dd>{value(flags, 'v5_ending_branch')}</dd></div>
        <div><dt>Coda</dt><dd>{flags.v5_case_c10_complete === true ? 'closed and persistent' : 'not reached'}</dd></div>
      </dl>
      <pre className="mt-5 whitespace-pre-wrap">{UNIVERSAL_GOODBYE.join('\n')}</pre>
    </section>
  );
}
