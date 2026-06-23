import Link from "next/link";

export default function HomePage() {
  return (
    <div className="space-y-8">
      <section className="space-y-2">
        <h1 className="font-mono text-2xl text-neutral-100">The Observance</h1>
        <p className="max-w-prose text-neutral-400">
          Control surface for the haunting. Two modes: a spoiler-free health view
          anyone running the world can watch, and a full Author view for tuning
          the arc.
        </p>
      </section>

      <section className="grid gap-4 sm:grid-cols-2">
        <Link
          href="/status"
          className="rounded-lg border border-neutral-800 bg-slate-850 p-5 transition-colors hover:border-neutral-700"
        >
          <h2 className="font-mono text-lg text-neutral-100">Status</h2>
          <p className="mt-1 text-sm text-neutral-400">
            Spoiler-free. Health, heatmap, error log, and neutral compliance
            counts. No story.
          </p>
        </Link>

        <Link
          href="/author"
          className="rounded-lg border border-neutral-800 bg-slate-850 p-5 transition-colors hover:border-neutral-700"
        >
          <h2 className="font-mono text-lg text-neutral-100">Author</h2>
          <p className="mt-1 text-sm text-neutral-400">
            Full spoilers. Beat queue, whisper budgets, bond ledger, and arc
            control. Admin only.
          </p>
        </Link>
      </section>
    </div>
  );
}
