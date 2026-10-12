import type { Metadata } from "next";
import Link from "next/link";
import { BOARDS, ForumShell } from "@/components/morrow/Forum";
import { bundle, vbDate } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "Copperline Community Forums" };
export const dynamic = "force-dynamic";

export default async function ForumIndex() {
  const b = await bundle();
  const latest = [...b.threads].sort((x, y) => y.posts.at(-1)!.at.localeCompare(x.posts.at(-1)!.at))[0];
  const notes = b.archivist_notes.at(-1);
  return (
    <ForumShell crumbs={null}>
      <div className="vb-notice">Only boards referenced by open support tickets were kept when the forum was retired. Everything else was not imported.</div>
      <table className="vb-table">
        <thead><tr><th>Forum</th><th>Last Post</th><th>Threads</th></tr></thead>
        <tbody>
          <tr className="vb-cat"><td colSpan={3}>{BOARDS[12].parent}</td></tr>
          <tr>
            <td><Link href="/forum-import/forumdisplay.php?f=12"><b>{BOARDS[12].title}</b></Link><div className="vb-desc">{BOARDS[12].desc}</div></td>
            <td>{latest ? <>{vbDate(latest.posts.at(-1)!.at)}<br />by {latest.posts.at(-1)!.author}</> : "-"}</td>
            <td>{b.threads.length}</td>
          </tr>
          <tr className="vb-cat"><td colSpan={3}>{BOARDS[99].parent}</td></tr>
          <tr>
            <td><Link href="/forum-import/forumdisplay.php?f=99"><b>{BOARDS[99].title}</b></Link><div className="vb-desc">{BOARDS[99].desc}</div></td>
            <td>{notes ? <>{vbDate(notes.at)}<br />by archivist</> : "-"}</td>
            <td>{b.archivist_notes.length}</td>
          </tr>
        </tbody>
      </table>
    </ForumShell>
  );
}
