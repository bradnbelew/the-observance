import type { Metadata } from "next";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import {
  project,
  REDACTED_GLYPH,
  type RecordSignal,
  type RecordEntry,
} from "@/lib/record-projection";
import { RuneGlyphs } from "@/lib/RuneGlyphs";

/**
 * The Record — the keepers' archive that leaves the game (A13 `arg-leaves-the-game`, §7) + the
 * cursed-map lure page (A14 `cursed-map-frame`, design/CURSED-MAP-SITE.md).
 *
 * A PUBLIC, anon-served page reached at a URL the group decodes in-world (the founder line "the record
 * is kept in more than one place"; the decoded path is the plant — ledger #11). It reads the Supabase
 * state READ-ONLY and shows ONLY what the in-world record would: a count of entries, a coarse season,
 * whether the keeping has closed. Everything else is a redaction.
 *
 * THE SLUG (A14). The route is the slug authority for the closed set { the-record, the-record-keeps }.
 *   - `the-record` → the BASE archive (A13, unchanged). (Bare `/record` does not match this dynamic
 *     segment; the plain Record is reached at `/record/the-record`.)
 *   - `the-record-keeps` → the base archive + the DOWNLOADS BLOCK (the lure page: a static `kept: 6`,
 *     one recovered-file entry quoting Mara's provenance, the README "lie", the struck-7). The `6` is a
 *     STATIC AUTHORED number — six prior keeper-generations the record already kept (ledger #24), NOT a
 *     live counter (a live count would drift off 6 and add a backend; CURSED-MAP-SITE §2c). The downloads
 *     block reads NO new data — the projection is not widened.
 *   - anything else → one in-voice 404 (the cold shell, one struck line, no entries) — a guessed slug
 *     never leaks the real archive (CURSED-MAP-SITE §1).
 *
 * THE SECURITY MODEL (absolute). anon has NO table grants and NO base-table RLS policies; it can read
 * ONLY the spoiler-free SECURITY DEFINER views. So this route reads exactly one neutral view (`v_record`,
 * SQL lane) through the request-scoped anon client and passes the coarse signal to the pure projection
 * (record-projection.ts). It NEVER touches the admin client, a sealed flag, a player name, or a custom
 * label. If the view is absent/unreadable (it has not shipped yet, or a fresh world), it degrades to the
 * SEALED BASELINE — an opened-but-empty archive — never an error, never a leaked default.
 *
 * ANTI-JANK: server component, no client JS, no polling. Static-per-build of the live coarse state — it
 * un-redacts in lockstep with progress (§2b). REVEAL DISCIPLINE: an entry is legible only once its stone
 * has actually been read; a withheld line renders as a struck block, never its text (the iceberg).
 *
 * CROSS-SURFACE TRUTH: the cold keeper register (lowercase, declarative — WEB-MASTER §6 Set-B) is the
 * whole voice. It shows a SUBSET of the same truth Minecraft + Discord show, in the same register, and
 * never claims a state the game has not reached. The page never addresses "you" (it speaks only of the
 * group; "it knows you" lives in-server under measured gates — INV-16).
 */

/** The lure slug — the founder margin decodes to this (kept-in-more-than-one-place, ledger #11). */
const LURE_SLUG = "the-record-keeps";
/** Slugs that render the base archive. (Bare `/record` does NOT hit this route — the dynamic `[slug]`
 *  segment needs a slug; the plain Record is reached at `/record/the-record`. The empty-string entry is
 *  harmless defense only.) */
const BASE_SLUGS = new Set(["", "the-record"]);

type RecordSlug = "base" | "lure" | "unknown";

/** Map a raw slug to the closed render set. The route is the slug authority (A14). */
function resolveSlug(raw: string | undefined): RecordSlug {
  const slug = (raw ?? "").trim().toLowerCase();
  if (slug === LURE_SLUG) return "lure";
  if (BASE_SLUGS.has(slug)) return "base";
  return "unknown";
}

export const metadata: Metadata = {
  // Found in-world, never by a crawler. (The segment layout sets this too; pinned here for the route.)
  robots: { index: false, follow: false },
  title: "the record",
};

// Static-per-build of the live coarse state: revalidate periodically so the archive un-redacts with
// progress WITHOUT any client polling. No request-time spoiler surface; just the neutral view, cached.
export const revalidate = 300;

/**
 * Read the one anon-safe coarse signal. The `v_record` view is owned by the SQL lane (see RETURN); it
 * exposes exactly { movement, stones_read, accepted } and nothing wider. We read it defensively (the
 * dashboard's typed Database does not yet declare it — that regen is the SQL lane's) and collapse ANY
 * failure to the sealed baseline so the Record can never error or over-reveal.
 */
async function readSignal(): Promise<RecordSignal> {
  try {
    const supabase = await createClient();
    // Untyped read: `v_record` is not in the generated Database type yet (SQL lane owns the regen).
    // The cast is local and the projection clamps every field, so a malformed row cannot over-reveal.
    const { data, error } = await (supabase as unknown as {
      from: (rel: string) => {
        select: (cols: string) => {
          maybeSingle: () => Promise<{
            data: Record<string, unknown> | null;
            error: { message: string } | null;
          }>;
        };
      };
    })
      .from("v_record")
      .select("movement, stones_read, accepted, theories")
      .maybeSingle();

    if (error || !data) return {};
    return {
      movement: typeof data.movement === "number" ? data.movement : null,
      stonesRead: typeof data.stones_read === "number" ? data.stones_read : null,
      accepted: data.accepted === true,
      // S-D: the coarse set of coherent keeper theories (a text[] of dead-keeper ids). Only accept a
      // clean string[]; anything else → null, and the projection falls back to the stonesRead lockstep.
      theories: Array.isArray(data.theories) && data.theories.every((t) => typeof t === "string")
        ? (data.theories as string[])
        : null,
    };
  } catch {
    // No view, no env, no DB — the sealed baseline. The archive a fresh world shows.
    return {};
  }
}

/** A redacted (struck) block — the withheld line's iceberg. Its width hints length, never the text. */
function Redaction() {
  return (
    <span
      aria-label="withheld"
      title="withheld"
      className="select-none font-mono tracking-widest text-neutral-700"
    >
      {REDACTED_GLYPH}
    </span>
  );
}

function ArchiveLine({ entry, index }: { entry: RecordEntry; index: number }) {
  const ord = String(index + 1).padStart(2, "0");
  return (
    <li className="flex items-baseline gap-4 border-t border-neutral-900 py-3">
      <span className="font-mono text-xs text-neutral-700 tabular-nums">{ord}</span>
      {entry.legible ? (
        <span className="font-mono text-sm leading-relaxed text-neutral-300">
          {entry.line}
        </span>
      ) : (
        <Redaction />
      )}
    </li>
  );
}

/**
 * The downloads block — the ONLY thing the lure slug adds (A14, CURSED-MAP-SITE §2). Pure + static:
 * no props from the DB, reads no view. The `kept: 6` is an AUTHORED number (ledger #24), not a metric;
 * the struck-7 reuses the projection's REDACTED_GLYPH for iceberg continuity. All copy is verbatim,
 * de-slopped, cold register (the map description, Mara's `m.kept` provenance, the README "lie"). The
 * download href is the GO-LIVE vignette asset (see PROLOGUE-VIGNETTE.md). Quotes the-copy-i-kept.md +
 * is backed by six-were-kept-before-you.md (the canon homes; LORE owns the wording).
 */
function Downloads() {
  return (
    <section className="mt-12 border-t border-neutral-900 pt-8 font-mono text-sm text-neutral-400">
      {/* the one legible recovered-file entry — the map description (flat, found, no marketing). */}
      <p className="leading-relaxed text-neutral-300">
        a hold, kept and left. one walk through it remains. the rest of the record is kept elsewhere.
        what is downloaded is only the part that fit in a file.
      </p>

      {/* the file name as an archive row — a filename, never a button/CTA. */}
      <p className="mt-4">
        <a
          href="/the-hold/the-hold.zip"
          download
          rel="noopener"
          className="text-neutral-300 underline decoration-neutral-700 underline-offset-4 hover:text-neutral-200"
        >
          the-hold.zip
        </a>
      </p>

      {/* the README "lie" — technically true; the load-bearing line is "it does not connect to anything"
          (ledger #25: the map connects to nothing; the server does). */}
      <p className="mt-2 text-xs leading-relaxed text-neutral-600">
        the-hold.zip — a small offline map. single player. no mods. about fifteen minutes.
        <br />
        it does not connect to anything. play it through to the end and it will tell you where the rest
        is kept.
      </p>

      {/* the provenance — Mara's hand, signed m.kept (the dead uploader; ledger #26). Quotes the last
          four lines of the-copy-i-kept.md verbatim. */}
      <p className="mt-6 text-xs leading-relaxed text-neutral-500">
        i copied it as it was given, page for page, and set the copy where fire and water do not reach.
        <br />
        i did not keep the seventh. i was not the hand that decides what is kept. — m.kept
      </p>

      {/* the counter — STATIC `kept: 6` + the struck seventh row (ledger #24). Not a live count. */}
      <div className="mt-8 font-mono">
        <div className="flex items-baseline gap-2">
          <span className="text-xs uppercase tracking-wide text-neutral-700">kept</span>
          <span className="text-2xl tabular-nums text-neutral-300">6</span>
        </div>
        <div
          aria-label="withheld"
          title="withheld"
          className="mt-1 select-none tracking-widest text-neutral-700"
        >
          {REDACTED_GLYPH}
        </div>
      </div>
    </section>
  );
}

/** The in-voice 404 — a guessed slug never leaks the real archive (A14, CURSED-MAP-SITE §1). The same
 *  cold shell, one struck line, no entries: an archive that holds nothing under that name. */
function NotFoundShell() {
  return (
    <main className="min-h-screen bg-[#070809] px-4 py-16 text-neutral-400">
      <div className="mx-auto max-w-xl">
        <header className="mb-10 text-center">
          <div className="mb-4 flex select-none justify-center text-neutral-700">
            <RuneGlyphs text="THE RECORD" height={26} />
          </div>
          <h1 className="font-mono text-sm uppercase tracking-[0.4em] text-neutral-500">
            the record
          </h1>
        </header>
        <p className="text-center font-mono text-xs lowercase tracking-wide text-neutral-700">
          nothing is kept under that name here.
        </p>
      </div>
    </main>
  );
}

export default async function RecordPage({
  params,
}: {
  // Next 15: route params are async. The route is the slug authority (A14).
  params: Promise<{ slug?: string }>;
}) {
  const { slug } = await params;
  const which = resolveSlug(slug);
  if (which === "unknown") return <NotFoundShell />;

  const signal = await readSignal();
  const rec = project(signal);
  // Discoverability for the deeper layer (/record/archive), gated on the ALREADY-READ coarse signal (no
  // second DB round-trip): at least one stone read ⇒ the recovery archive has something to show. The link
  // is cold + in-register — a kept filename, never a CTA. Never shown on the sealed baseline.
  const archiveHasContent = (signal.stonesRead ?? 0) > 0;

  return (
    <main className="min-h-screen bg-[#070809] px-4 py-16 text-neutral-400">
      <div className="mx-auto max-w-xl">
        {/* The rune-mark header. The glyph block stands for the archive's seal — the same rune
            alphabet learned in-world (a recognizable mark, not decoded text). Cold, no warmth. */}
        <header className="mb-10 text-center">
          <div className="mb-4 flex select-none justify-center text-neutral-700">
            <RuneGlyphs text="THE RECORD" height={26} />
          </div>
          <h1 className="font-mono text-sm uppercase tracking-[0.4em] text-neutral-500">
            the record
          </h1>
          <p className="mt-3 font-mono text-xs lowercase tracking-wide text-neutral-600">
            {rec.season}
          </p>
        </header>

        {/* The one number — a muster, not a clock. */}
        <div className="mb-8 flex items-baseline justify-center gap-2 font-mono">
          <span className="text-3xl tabular-nums text-neutral-300">{rec.kept}</span>
          <span className="text-sm text-neutral-700">/</span>
          <span className="text-sm tabular-nums text-neutral-600">{rec.total}</span>
          <span className="ml-2 text-xs uppercase tracking-wide text-neutral-700">
            kept
          </span>
        </div>

        {/* The archive — earned entries legible, the rest withheld (struck blocks). */}
        <ol className="border-b border-neutral-900">
          {rec.entries.map((entry, i) => (
            <ArchiveLine key={entry.id} entry={entry} index={i} />
          ))}
        </ol>

        {/* The lure slug appends the downloads block (the recovered file + the static `kept: 6`). The
            base slug renders the archive alone. The block reads no new data (A14). */}
        {which === "lure" && <Downloads />}

        {/* The standing footer — a count, then the iceberg. */}
        <footer className="mt-8 text-center font-mono text-xs lowercase tracking-wide text-neutral-700">
          {rec.footer}
          {/* The quiet link to the deeper layer — shown only once something is kept there (a stone read).
              A plain underlined mono line in-register, never a CTA. */}
          {archiveHasContent && (
            <>
              <br />
              <Link
                href="/record/archive"
                className="mt-2 inline-block text-neutral-600 underline decoration-neutral-800 underline-offset-4 hover:text-neutral-500"
              >
                the record is kept in more than one place.
              </Link>
            </>
          )}
        </footer>
      </div>
    </main>
  );
}
