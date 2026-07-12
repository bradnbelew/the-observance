import type { Metadata } from "next";
import { existsSync } from "node:fs";
import { join } from "node:path";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import {
  project,
  REDACTED_GLYPH,
  type RecordSignal,
  type RecordEntry,
} from "@/lib/record-projection";

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
 *   - `the-record-keeps` → a crude preserved user-file mirror outside the WHMCS template. It carries the
 *     static `kept: 6`, the struck seventh row, the map file, and m.kept's uploader note. Its black-page
 *     appearance is explained by the surviving lighttpd user directory, not by unexplained ARG styling.
 *     The authored six is not a live counter and this branch reads no story state.
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
const HOLD_ZIP_PUBLIC_PATH = "/the-hold/the-hold.zip";
/** Slugs that render the base archive. (Bare `/record` does NOT hit this route — the dynamic `[slug]`
 *  segment needs a slug; the plain Record is reached at `/record/the-record`. The empty-string entry is
 *  harmless defense only.) */
const BASE_SLUGS = new Set(["", "the-record"]);

function holdZipAvailable(): boolean {
  return existsSync(join(process.cwd(), "public", "the-hold", "the-hold.zip"));
}

type RecordSlug = "base" | "lure" | "unknown";

/** Map a raw slug to the closed render set. The route is the slug authority (A14). */
function resolveSlug(raw: string | undefined): RecordSlug {
  const slug = (raw ?? "").trim().toLowerCase();
  if (slug === LURE_SLUG) return "lure";
  if (BASE_SLUGS.has(slug)) return "base";
  return "unknown";
}

export async function generateMetadata({ params }: { params: Promise<{ slug?: string }> }): Promise<Metadata> {
  const which = resolveSlug((await params).slug);
  return {
    robots: { index: false, follow: false },
    title: which === "lure"
      ? "Index of /~mkept/record/"
      : which === "base"
        ? "recordsrv / public projection"
        : "record key not found",
  };
}

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

/** The preserved file row. Pure and static: it reads no view and exposes no server endpoint. */
function Downloads({ hasHoldZip }: { hasHoldZip: boolean }) {
  return (
    <section className="mirror-files" aria-labelledby="mirror-files-heading">
      <h2 id="mirror-files-heading">files</h2>
      <div className="mirror-file-row">
        <span>-rw-r--r--</span><span>mkept</span><span>17K</span>
        {hasHoldZip ? (
          <a
            href={HOLD_ZIP_PUBLIC_PATH}
            download
            rel="noopener"
          >
            the-hold.zip
          </a>
        ) : (
          <span className="mirror-missing" title="not recovered">
            the-hold.zip
          </span>
        )}
      </div>
      {!hasHoldZip && (
        <p className="mirror-error">file not yet recovered from preserved object store.</p>
      )}
      <p className="mirror-file-note">normal single-player world · no mods · approximately fifteen minutes · does not connect to anything</p>
    </section>
  );
}

/** A guessed key receives the service's ordinary lookup error and leaks no archive state. */
function NotFoundShell() {
  return (
    <main className="record-error-site">
      <div className="record-error-box">
        <p>recordsrv/0.7</p><h1>404: key not found</h1>
        <pre>lookup failed{`\n`}nothing is kept under that name here.</pre>
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

  const hasHoldZip = holdZipAvailable();
  if (which === "lure") {
    return (
      <main className="mirror-site">
        <div className="mirror-page">
          <header className="mirror-header">
            <p>files.copperlinehosting.com :: static user mirror</p>
            <h1>/home/mkept/public_html/record/</h1>
            <span>read-only snapshot · last modified 2011-02-08 23:51 CST</span>
          </header>
          <nav className="mirror-nav"><Link href="/community/2011/02/08/world-backup">../ return to community post</Link></nav>
          <section className="mirror-note-block"><h2>copy-register.txt</h2><pre>{`THE RECORD KEEPS\n\n01  copy withheld\n02  copy withheld\n03  copy withheld\n04  copy withheld\n05  copy withheld\n06  copy withheld\n07  row returned without a name\n\nkept: 6\nseventh: ----------------`}</pre></section>
          <Downloads hasHoldZip={hasHoldZip} />
          <section className="mirror-note-block"><h2>uploader-note.txt</h2><p>a hold, kept and left. one walk through it remains. the rest of the record is kept elsewhere.</p><p>i copied it as it was given, page for page. i did not keep the seventh. i was not the hand that decides what is kept.</p><p>— m.kept</p></section>
          <footer className="mirror-footer">lighttpd/1.4.28 · preserved directory index · write methods disabled</footer>
        </div>
      </main>
    );
  }

  const signal = await readSignal();
  const rec = project(signal);
  // Discoverability for the deeper layer (/record/archive), gated on the ALREADY-READ coarse signal (no
  // second DB round-trip): at least one stone read ⇒ the recovery archive has something to show. The link
  // is cold + in-register — a kept filename, never a CTA. Never shown on the sealed baseline.
  const archiveHasContent = (signal.stonesRead ?? 0) > 0;

  return (
    <main className="record-site">
      <div className="record-page narrow">
        <header className="record-system-header">
          <div><span>recordsrv/0.7</span><span>projection: public</span><span>mode: read-only</span></div>
          <h1>THE RECORD</h1>
          <p>{rec.season}</p>
        </header>

        {/* The one number — a muster, not a clock. */}
        <div className="record-muster">
          <span className="text-3xl tabular-nums text-neutral-300">{rec.kept}</span>
          <span className="text-sm text-neutral-700">/</span>
          <span className="text-sm tabular-nums text-neutral-600">{rec.total}</span>
          <span className="ml-2 text-xs uppercase tracking-wide text-neutral-700">
            kept
          </span>
        </div>

        {/* The archive — earned entries legible, the rest withheld (struck blocks). */}
        <ol className="record-ledger">
          {rec.entries.map((entry, i) => (
            <ArchiveLine key={entry.id} entry={entry} index={i} />
          ))}
        </ol>

        {/* The standing footer — a count, then the iceberg. */}
        <footer className="record-footer">
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
