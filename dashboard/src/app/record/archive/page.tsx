import type { Metadata } from "next";
import { createClient } from "@/lib/supabase/server";
import {
  projectArchive,
  type ArchiveCard,
  type ArchiveThread,
  type ArchiveCardView,
} from "@/lib/archive-projection";
import { RuneGlyphs } from "@/lib/RuneGlyphs";

/**
 * The Recovery Archive — the reading-room, one stratum below The Record (A13 `arg-leaves-the-game`, §7).
 *
 * The Record (/record/[slug]) shows the coarse MUSTER — a count kept, a season, the rest struck. This is
 * its DEEPER LAYER: the same artifact, further down, showing the actual RECOVERED MATERIAL of the cards
 * the group has already un-earthed, arranged under the five threads so the shape of what's-not-yet-found
 * stays legible (the iceberg). It reads as the same cold keeper register, never a new app.
 *
 * THE SECURITY MODEL (absolute, mirrors the Record). anon has NO table grants and NO base-table RLS; it
 * reads ONLY the spoiler-free SECURITY DEFINER views. This route reads exactly one — `v_archive` — which
 * returns ONE ROW PER REVEALED CARD (the reveal-gating is done in SQL; a card is present IFF the group
 * revealed it). It NEVER touches the admin client. The projection then drops any reference to a card that
 * is NOT in the returned set, so a revealed card can point only at other revealed cards (no leak, no dead
 * link). If the view is absent/unreadable (not shipped yet, or a fresh world), it degrades to the SEALED
 * BASELINE — an archive with nothing recovered — never an error, never a leaked default.
 *
 * ANTI-JANK: server component, no client JS, no polling. Static-per-build of the live revealed set — it
 * un-redacts in lockstep with progress (revalidate). The card bodies are authored recovered material; we
 * render them verbatim and author only the tiny UI chrome (section frame, the withheld line, the kind
 * tags, the citation links) — lowercase, cold, minimal, matching the Record's own chrome.
 */

export const metadata: Metadata = {
  // Found in-world, never by a crawler. (The segment layout sets this too; pinned here for the route.)
  robots: { index: false, follow: false },
  title: "the recovery archive",
};

// Static-per-build of the live revealed set: revalidate periodically so the archive un-redacts with
// progress WITHOUT any client polling. No request-time spoiler surface; just the neutral view, cached.
export const revalidate = 300;

/** The exact columns of the `v_archive` SECURITY DEFINER view (one row per revealed card). */
const ARCHIVE_COLS =
  "card_key, thread_key, thread_label, thread_color, thread_sort, title, body, card_kind, references_card_key, card_sort";

const ARCHIVE_KINDS = new Set(["rumor", "explore", "verified", "contradicted"]);

/**
 * Read the revealed cards from the one anon-safe view. `v_archive` is owned by the SQL lane; we read it
 * defensively (the dashboard's typed Database does not declare it yet — that regen is the SQL lane's) and
 * collapse ANY failure/absence to an empty archive, so the reading-room can never error or over-reveal.
 * Every field is clamped to the contract; a malformed row cannot widen what is shown.
 */
async function readArchive(): Promise<ArchiveCard[]> {
  try {
    const supabase = await createClient();
    // Untyped read: `v_archive` is not in the generated Database type yet (SQL lane owns the regen).
    const { data, error } = await (supabase as unknown as {
      from: (rel: string) => {
        select: (cols: string) => Promise<{
          data: Record<string, unknown>[] | null;
          error: { message: string } | null;
        }>;
      };
    })
      .from("v_archive")
      .select(ARCHIVE_COLS);

    if (error || !Array.isArray(data)) return [];

    // Coerce each row to the contract. Anything off-shape is skipped, never guessed — the view is the
    // authority on what exists, and the projection re-checks references against the revealed set anyway.
    const cards: ArchiveCard[] = [];
    for (const row of data) {
      const card_key = typeof row.card_key === "string" ? row.card_key : "";
      const thread_key = typeof row.thread_key === "string" ? row.thread_key : "";
      if (!card_key || !thread_key) continue;
      const kind = typeof row.card_kind === "string" && ARCHIVE_KINDS.has(row.card_kind)
        ? (row.card_kind as ArchiveCard["card_kind"])
        : "explore";
      const refs = Array.isArray(row.references_card_key)
        ? row.references_card_key.filter((r): r is string => typeof r === "string")
        : [];
      cards.push({
        card_key,
        thread_key,
        thread_label: typeof row.thread_label === "string" ? row.thread_label : "",
        thread_color: typeof row.thread_color === "string" ? row.thread_color : "",
        thread_sort: typeof row.thread_sort === "number" ? row.thread_sort : 0,
        title: typeof row.title === "string" ? row.title : "",
        body: typeof row.body === "string" ? row.body : "",
        card_kind: kind,
        references_card_key: refs,
        card_sort: typeof row.card_sort === "number" ? row.card_sort : 0,
      });
    }
    return cards;
  } catch {
    // No view, no env, no DB — the sealed baseline. The archive a fresh world shows.
    return [];
  }
}

/** The withheld line for an empty thread — the iceberg. States the shape is not yet found, never its text. */
function WithheldThread() {
  return (
    <p className="border-t border-neutral-900 py-3 font-mono text-xs lowercase tracking-wide text-neutral-700">
      <span aria-label="withheld" title="withheld" className="select-none tracking-widest">
        ████
      </span>
      <span className="ml-3">not yet recovered.</span>
    </p>
  );
}

/** A single recovered card — title + verbatim body, styled by evidentiary standing, anchored by card_key. */
function Card({ card }: { card: ArchiveCardView }) {
  // 'rumor' reads faintly (unverified); 'contradicted' is struck-through-but-legible; the rest are plain.
  const rumor = card.card_kind === "rumor";
  const contradicted = card.card_kind === "contradicted";
  return (
    <li id={card.card_key} className="scroll-mt-16 border-t border-neutral-900 py-4">
      <h3
        className={[
          "font-mono text-sm leading-relaxed",
          contradicted ? "text-neutral-500 line-through decoration-neutral-700" : "text-neutral-300",
          rumor ? "italic text-neutral-500" : "",
        ].join(" ")}
      >
        {card.title}
        {rumor && (
          <span className="ml-2 align-baseline text-xs not-italic text-neutral-700">— unverified</span>
        )}
      </h3>
      <p
        className={[
          "mt-2 whitespace-pre-line font-mono text-sm leading-relaxed",
          contradicted ? "text-neutral-600 line-through decoration-neutral-800" : "text-neutral-400",
          rumor ? "text-neutral-600" : "",
        ].join(" ")}
      >
        {card.body}
      </p>
      {card.references.length > 0 && (
        <p className="mt-3 font-mono text-xs lowercase tracking-wide text-neutral-700">
          see also{" "}
          {card.references.map((ref, i) => (
            <span key={ref.card_key}>
              {i > 0 && <span className="text-neutral-800">, </span>}
              <a
                href={`#${ref.card_key}`}
                className="text-neutral-500 underline decoration-neutral-800 underline-offset-4 hover:text-neutral-400"
              >
                {ref.title}
              </a>
            </span>
          ))}
        </p>
      )}
    </li>
  );
}

/** A thread column — its cold label, then its recovered cards, or a single withheld line when empty. */
function Thread({ thread }: { thread: ArchiveThread }) {
  return (
    <section className="mb-12">
      <header className="mb-3 flex items-baseline gap-3">
        <h2 className="font-mono text-xs uppercase tracking-[0.3em] text-neutral-500">{thread.label}</h2>
        <span className="font-mono text-xs tabular-nums text-neutral-700">
          {thread.revealed > 0 ? thread.revealed : ""}
        </span>
      </header>
      {thread.cards.length > 0 ? (
        <ol className="border-b border-neutral-900">
          {thread.cards.map((c) => (
            <Card key={c.card_key} card={c} />
          ))}
        </ol>
      ) : (
        <WithheldThread />
      )}
    </section>
  );
}

/** The sealed shell — nothing recovered yet. The cold "nothing kept" tone, never an error. */
function SealedShell() {
  return (
    <main className="min-h-screen bg-[#070809] px-4 py-16 text-neutral-400">
      <div className="mx-auto max-w-xl">
        <header className="mb-10 text-center">
          <div className="mb-4 flex select-none justify-center text-neutral-700">
            <RuneGlyphs text="THE RECOVERY ARCHIVE" height={24} />
          </div>
          <h1 className="font-mono text-sm uppercase tracking-[0.4em] text-neutral-500">
            the recovery archive
          </h1>
        </header>
        <p className="text-center font-mono text-xs lowercase tracking-wide text-neutral-700">
          nothing has been recovered yet.
        </p>
      </div>
    </main>
  );
}

export default async function ArchivePage() {
  const cards = await readArchive();
  const archive = projectArchive(cards);

  if (archive.empty) return <SealedShell />;

  return (
    <main className="min-h-screen bg-[#070809] px-4 py-16 text-neutral-400">
      <div className="mx-auto max-w-xl">
        {/* The rune-mark header — the same seal as the Record, this being its deeper layer, not a new page. */}
        <header className="mb-10 text-center">
          <div className="mb-4 flex select-none justify-center text-neutral-700">
            <RuneGlyphs text="THE RECOVERY ARCHIVE" height={24} />
          </div>
          <h1 className="font-mono text-sm uppercase tracking-[0.4em] text-neutral-500">
            the recovery archive
          </h1>
          <p className="mt-3 font-mono text-xs lowercase tracking-wide text-neutral-600">
            what has been recovered, kept under five heads.
          </p>
        </header>

        {/* The five threads, canonical order — recovered cards where found, a withheld line where not. */}
        {archive.threads.map((t) => (
          <Thread key={t.key} thread={t} />
        ))}

        {/* The standing footer — a count, then the iceberg. Cold register, no warmth. */}
        <footer className="mt-4 text-center font-mono text-xs lowercase tracking-wide text-neutral-700">
          {archive.total} recovered. the rest is not yet found.
        </footer>
      </div>
    </main>
  );
}
