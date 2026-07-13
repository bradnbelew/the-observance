import type { Metadata } from "next";
import { readLedger, EMPTY as EMPTY_LEDGER, type LedgerProjection } from "@/lib/record/ledger";
import { readIntegrityLog, type IntegrityWarning } from "@/lib/record/integrity";
import { InscribeForm } from "./InscribeForm";
import { RuneGlyphs } from "@/lib/RuneGlyphs";

/**
 * /record/terminal — THE RECORD, recovered-archive terminal (CHANGE-MANIFEST A5 + L4, INTEGRATION
 * Layer 5). NOT a clean puzzle site: a half-corrupted archive terminal of the Hold's own
 * record-keeping — a plain exposed service with half-redacted entries and integrity warnings. It unifies
 * three surfaces into one artifact: the LEDGER (names fill in), the WRITE-IN (inscribe answers), and
 * the INTEGRITY CHECK (the hint rail as an escalating error log).
 *
 * DISCOVER-BY-URL. This path (`/record/terminal`) is the discovered entry point — reached in-world
 * (decoded from a carving / the founder margin), never by a crawler (noindex). It is distinct from the
 * A13/A14 coarse public archive at `/record/the-record` (that surface stays untouched); this is the
 * fuller keeper terminal.
 *
 * SECURITY (absolute). Every read here runs in this SERVER component through the `server-only`
 * service-role oracle libs (ledger.ts / integrity.ts). The browser receives ONLY the projected,
 * non-spoiler JSON — names + counts + coarse thread fill + earned hint tiers. The service-role key
 * never reaches the client. The single client island (InscribeForm) holds no secrets and POSTs to the
 * server route.
 *
 * ANTI-JANK. Server-rendered, revalidated on an interval — the ledger + integrity log un-redact in
 * lockstep with progress without client polling. The only client JS is the inscription form.
 */

export const metadata: Metadata = {
  robots: { index: false, follow: false },
  title: "recordsrv / recovery terminal",
};

// Re-read the live state periodically so the terminal un-redacts with progress. No client polling.
export const revalidate = 120;
export const dynamic = "force-dynamic"; // the ledger is live group state; always the current recovery.

/** A struck redaction block — the withheld-line iceberg; its width hints length, never the text. */
function Redaction({ width = 8 }: { width?: number }) {
  return (
    <span aria-label="withheld" title="withheld" className="select-none tracking-widest text-neutral-700">
      {"█".repeat(width)}
    </span>
  );
}

/** The exposed internal-service header. Its plain protocol chrome explains the visual break from Copperline. */
function Header({ ledger }: { ledger: LedgerProjection }) {
  return (
    <header className="record-system-header terminal-system-header">
      <div>
        <span>recordsrv/0.7</span><span>endpoint: /terminal</span>
        <span className={ledger.sealed ? "text-red-800" : "text-amber-700/80"}>
          {ledger.sealed ? "integrity: unreadable" : "integrity: partial"}
        </span>
      </div>
      <RuneGlyphs text="THE RECORD" className="mx-auto my-3 text-amber-700/70" height={22} />
      <h1>THE RECORD / RECOVERY TERMINAL</h1>
      <p>read order not preserved · struck entries omitted · writes accepted without echo</p>
    </header>
  );
}

/** The muster — the record's one number, plus the live web breadth. */
function Muster({ ledger }: { ledger: LedgerProjection }) {
  return (
    <div className="mb-8 flex flex-wrap items-baseline gap-x-8 gap-y-2 font-mono">
      <div className="flex items-baseline gap-2">
        <span className="text-3xl tabular-nums text-neutral-300">{ledger.totalKept}</span>
        <span className="text-[11px] uppercase tracking-wide text-neutral-700">marks kept</span>
      </div>
      <div className="flex items-baseline gap-2">
        <span className="text-xl tabular-nums text-neutral-400">{ledger.names.length}</span>
        <span className="text-[11px] uppercase tracking-wide text-neutral-700">hands entered</span>
      </div>
      <div className="flex items-baseline gap-2">
        <span className="text-xl tabular-nums text-neutral-500">{ledger.openPuzzles}</span>
        <span className="text-[11px] uppercase tracking-wide text-neutral-700">marks unkept</span>
      </div>
    </div>
  );
}

/** The ledger of hands — names fill in as keepers act; struck rows where the record is corrupted. */
function Ledger({ ledger }: { ledger: LedgerProjection }) {
  // Interleave struck/redacted "corrupted" rows so the archive reads recovered, not clean. These are
  // pure decoration — they carry NO withheld name (there is nothing behind them); they are the decay.
  return (
    <section className="mb-10">
      <div className="mb-3 flex items-baseline gap-3 border-b border-neutral-900 pb-1 font-mono text-[10px] uppercase tracking-[0.3em] text-neutral-600">
        <span>ledger of hands</span>
        <span className="text-neutral-800">{"// who is kept"}</span>
      </div>
      {ledger.names.length === 0 ? (
        <p className="font-mono text-[12px] lowercase text-neutral-700">
          no hand is entered yet. the ledger is cold. <Redaction width={12} />
        </p>
      ) : (
        <ol className="font-mono text-sm">
          {ledger.names.map((n, i) => (
            <li
              key={`${n.name}-${i}`}
              className="flex items-baseline gap-4 border-t border-neutral-900 py-2"
            >
              <span className="w-8 shrink-0 text-[11px] tabular-nums text-neutral-700">
                {String(i + 1).padStart(2, "0")}
              </span>
              <span className="flex-1 truncate text-neutral-300">{n.name}</span>
              <span className="text-[11px] tabular-nums text-neutral-600">
                {n.kept} <span className="text-neutral-800">kept</span>
              </span>
            </li>
          ))}
          {/* one struck row: the record keeps more than it can read back (the iceberg). */}
          <li className="flex items-baseline gap-4 border-t border-neutral-900 py-2">
            <span className="w-8 shrink-0 text-[11px] tabular-nums text-neutral-800">
              {String(ledger.names.length + 1).padStart(2, "0")}
            </span>
            <span className="flex-1">
              <Redaction width={10} />
            </span>
            <span className="text-[11px] text-neutral-800">— struck</span>
          </li>
        </ol>
      )}
    </section>
  );
}

/** The reconstruction threads — coarse fill bars, never card contents. */
function Threads({ ledger }: { ledger: LedgerProjection }) {
  if (ledger.threads.length === 0) return null;
  return (
    <section className="mb-10">
      <div className="mb-3 border-b border-neutral-900 pb-1 font-mono text-[10px] uppercase tracking-[0.3em] text-neutral-600">
        reconstruction threads
      </div>
      <ul className="space-y-2 font-mono">
        {ledger.threads.map((t) => {
          const total = Math.max(t.resolved + t.open, t.resolved, 1);
          const pct = Math.round((t.resolved / total) * 100);
          return (
            <li key={t.key} className="flex items-center gap-3 text-[12px]">
              <span className="w-40 shrink-0 lowercase text-neutral-500">{t.label}</span>
              <span className="h-2 flex-1 overflow-hidden border border-neutral-900 bg-black/40">
                <span className="block h-full bg-neutral-700" style={{ width: `${pct}%` }} aria-hidden />
              </span>
              <span className="w-16 shrink-0 text-right tabular-nums text-neutral-600">
                {t.resolved}/{t.resolved + t.open}
              </span>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

/** The integrity check / error log — the hint rail as escalating warnings the longer a thread stalls. */
function IntegrityLog({ warnings }: { warnings: IntegrityWarning[] }) {
  return (
    <section className="mb-10">
      <div className="mb-3 flex items-baseline gap-3 border-b border-neutral-900 pb-1 font-mono text-[10px] uppercase tracking-[0.3em] text-neutral-600">
        <span>integrity check</span>
        <span className="text-neutral-800">{"// cross-reference log"}</span>
      </div>
      {warnings.length === 0 ? (
        <p className="font-mono text-[12px] lowercase text-neutral-700">
          integrity: nominal. no entry is stalled long enough to flag.
        </p>
      ) : (
        <ul className="space-y-2 font-mono text-[12px]">
          {warnings.map((w, i) => (
            <li key={i} className="flex items-baseline gap-3 border-l-2 border-amber-900/50 pl-3">
              <span className="shrink-0 text-[10px] uppercase tracking-wide text-amber-800/80">
                t{w.tier}
              </span>
              <span className="shrink-0 text-[10px] uppercase tracking-wide text-neutral-700">
                {w.stall}
              </span>
              <span className="text-neutral-400">{w.body}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default async function RecordTerminalPage() {
  // Both reads run server-side (service role, server-only). The browser never sees the client or key.
  // readLedger/readIntegrityLog are already fail-soft internally; this outer guard is a second net so a
  // future refactor of either can never turn into an unstyled error page on a public in-fiction surface.
  let ledger: LedgerProjection = EMPTY_LEDGER;
  let warnings: IntegrityWarning[] = [];
  try {
    [ledger, warnings] = await Promise.all([readLedger(), readIntegrityLog()]);
  } catch {
    // fall through to the sealed baseline — same degradation the two lib functions already do internally.
  }

  return (
    <main className="record-site terminal-site">
      <div className="record-page terminal-page">
        <Header ledger={ledger} />
        <Muster ledger={ledger} />
        <Ledger ledger={ledger} />
        <Threads ledger={ledger} />
        <IntegrityLog warnings={warnings} />

        {/* the inscription lectern — write an answer INTO the record. */}
        <section className="mb-10">
          <div className="mb-3 flex items-baseline gap-3 border-b border-neutral-900 pb-1 font-mono text-[10px] uppercase tracking-[0.3em] text-neutral-600">
            <span>inscribe</span>
            <span className="text-neutral-800">{"// a hand and a mark"}</span>
          </div>
          <p className="mb-2 font-mono text-[11px] lowercase leading-relaxed text-neutral-700">
            a hand. a mark. what belongs is kept. what does not is not read back.
          </p>
          <InscribeForm />
        </section>

        <footer className="mt-8 border-t border-neutral-900 pt-4 text-center font-mono text-[10px] lowercase tracking-wide text-neutral-700">
          {ledger.sealed
            ? "the record could not be read. it is kept elsewhere."
            : `${ledger.totalKept} kept. the rest is struck, or not yet inscribed.`}
        </footer>
      </div>
    </main>
  );
}
