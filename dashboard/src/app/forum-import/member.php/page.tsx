import type { Metadata } from "next";
import Link from "next/link";
import { ForumShell } from "@/components/morrow/Forum";
import { bundle, postsBy, vbDate } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "View Profile - Copperline Community Forums" };
export const dynamic = "force-dynamic";

export default async function MemberProfile({ searchParams }: { searchParams: Promise<{ u?: string }> }) {
  const u = (await searchParams).u ?? "";
  const b = await bundle();
  const m = b.members[u];
  if (!m) {
    return <ForumShell crumbs={null}><div className="vb-notice">This user has not registered and therefore does not have a profile to view.</div></ForumShell>;
  }
  const posts = postsBy(b, u);
  return (
    <ForumShell crumbs={<> » View Profile: {u}</>}>
      <h1 className="vb-thread-title">{u}</h1>
      <table className="vb-table vb-profile"><tbody>
        <tr><th>User title</th><td>{m.title}</td></tr>
        <tr><th>Join Date</th><td>{m.joined}</td></tr>
        <tr><th>Location</th><td>{m.location || "-"}</td></tr>
        <tr><th>Local Time</th><td>{m.local_offset === "same as forum" ? "same as forum time" : `forum time ${m.local_offset}`}</td></tr>
        {m.interests ? <tr><th>Interests</th><td>{m.interests}</td></tr> : null}
        <tr><th>Total Posts</th><td>{m.posts}</td></tr>
        {m.sig ? <tr><th>Signature</th><td>{m.sig}</td></tr> : null}
        {m.import_note ? <tr><th>Import</th><td className="vb-desc">{m.import_note}</td></tr> : null}
      </tbody></table>
      {posts.length ? (
        <>
          <h2 className="vb-subhead">Posts kept in this import</h2>
          <ul className="vb-postlist">{posts.map(({ thread, post }) => (
            <li key={post.id}><Link href={`/forum-import/showthread.php?t=${thread.id}#post${post.id}`}>{thread.title}</Link> <span className="vb-desc">{vbDate(post.at)}</span></li>
          ))}</ul>
        </>
      ) : null}
    </ForumShell>
  );
}
