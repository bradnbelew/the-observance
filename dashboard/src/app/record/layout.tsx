import type { Metadata } from "next";

/**
 * The Record — segment layout (A13 `arg-leaves-the-game`, BUILD-MANIFEST §7).
 *
 * The Record is the in-world keepers' archive that "leaves the game" onto the web: a bleak, redacted,
 * rune-styled page anyone can reach, that reads the Supabase state READ-ONLY and un-redacts in lockstep
 * with progress. It is found in-world (a decoded URL path), never by a crawler.
 *
 * SPOILER / PRIVACY DISCIPLINE: every segment under /record is `noindex` and renders ONLY the
 * spoiler-free projection (record-projection.ts) — never a raw table, a sealed flag, or a player name.
 * No client JS, no polling — static-per-build (anti-jank §2b). The cold keeper register (lowercase,
 * declarative) is the whole voice; nothing here addresses "you".
 *
 * NOTE (cross-owner): the dashboard root layout (src/app/layout.tsx — DASH lane) wraps every route in
 * the control-surface nav (Status / Author links) and a max-w-5xl main. The public Record should NOT
 * show those links. The minimal-blast fix is a tiny conditional in the root layout keyed on the
 * pathname segment; described precisely in this worker's RETURN. This segment styles itself to read as
 * the archive regardless, and leaks no story content either way.
 */
export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function RecordLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return <div className="record-root">{children}</div>;
}
