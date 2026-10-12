import type { Metadata } from "next";
import Link from "next/link";
import { BOARDS, ForumShell } from "@/components/morrow/Forum";
import { bundle, vbDate } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "Mossfield - Copperline Community Forums" };
export const dynamic = "force-dynamic";

export default async function ForumDisplay({ searchParams }: { searchParams: Promise<{ f?: string }> }) {
  const f = Number((await searchParams).f);
  const b = await bundle();
  if (f === 99) {
    return (
      <ForumShell crumbs={<> » {BOARDS[99].title}</>}>
        {b.archivist_notes.map((n) => (
          <table key={n.id} className="vb-post"><tbody>
            <tr className="vb-post-head"><td>{vbDate(n.at)}</td><td className="vb-postno">archivist</td></tr>
            <tr><td className="vb-message" colSpan={2}><div className="vb-body">{n.body}</div></td></tr>
          </tbody></table>
        ))}
      </ForumShell>
    );
  }
  if (f !== 12) {
    return <ForumShell crumbs={null}><div className="vb-notice">Invalid Forum specified. If you followed a valid link, please notify the administrator</div></ForumShell>;
  }
  const threads = [...b.threads].sort((x, y) => y.posts.at(-1)!.at.localeCompare(x.posts.at(-1)!.at));
  return (
    <ForumShell crumbs={<> » {BOARDS[12].parent} » {BOARDS[12].title}</>}>
      <table className="vb-table">
        <thead><tr><th>Thread / Thread Starter</th><th>Last Post</th><th>Replies</th></tr></thead>
        <tbody>{threads.map((t) => (
          <tr key={t.id}>
            <td><Link href={`/forum-import/showthread.php?t=${t.id}`}><b>{t.title}</b></Link><div className="vb-desc">{t.posts[0].author}</div></td>
            <td>{vbDate(t.posts.at(-1)!.at)}<br />by {t.posts.at(-1)!.author}</td>
            <td>{t.posts.length - 1}</td>
          </tr>
        ))}</tbody>
      </table>
    </ForumShell>
  );
}
