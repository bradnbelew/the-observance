import Link from "next/link";
import type { Bundle, Post } from "@/lib/morrow/canon";
import { vbDate } from "@/lib/morrow/canon";

export const BOARDS = {
  12: { title: "Mossfield", parent: "Customer Servers", desc: "Mossfield survival server (Builder plan)." },
  99: { title: "Archive notes", parent: "Mirror", desc: "Notes from whoever keeps this mirror running." },
} as const;

export function ForumShell({ crumbs, children }: { crumbs: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="vb">
      <div className="vb-top">
        <Link href="/forum-import/index.php" className="vb-logo">Copperline <span>Community Forums</span></Link>
        <div className="vb-note">Imported read-only copy · registration closed</div>
      </div>
      <div className="vb-nav">
        <Link href="/forum-import/index.php">Forum</Link>
        <Link href="/forum-import/memberlist.php">Members List</Link>
        <Link href="/">copperlinehosting.com</Link>
      </div>
      <div className="vb-crumbs"><Link href="/forum-import/index.php">Copperline Community Forums</Link>{crumbs}</div>
      {children}
      <div className="vb-foot">All times are GMT -6 (Daylight Saving Time adjusted). Powered by vBulletin® Version 3.8.4<br />
        Copyright ©2000 - 2011, Jelsoft Enterprises Ltd.</div>
    </div>
  );
}

export function PostBox({ post, b, index, print }: { post: Post; b: Bundle; index: number; print?: boolean }) {
  const m = b.members[post.author];
  const rw = !print ? b.forum_rewrites?.[String(post.id)] : undefined;
  const body = rw ? rw.body : post.body;
  const edited = rw ? rw.note : post.edited;
  const staff = post.author === "Theo [Copperline]" || post.author === "i_bell";
  return (
    <table className="vb-post" id={`post${post.id}`}>
      <tbody>
        <tr className="vb-post-head"><td>{vbDate(post.at)}</td><td className="vb-postno">#{index + 1}</td></tr>
        <tr>
          <td className="vb-userinfo">
            {m ? <Link href={`/forum-import/member.php?u=${encodeURIComponent(post.author)}`} className={staff ? "vb-staff" : ""}>{post.author}</Link> : <b>{post.author}</b>}
            {m ? <><div className="vb-usertitle">{m.title}</div><div className="vb-userstats">Join Date: {m.joined.slice(0, 7)}<br />{m.location ? <>Location: {m.location}<br /></> : null}Posts: {m.posts}</div></> : null}
          </td>
          <td className="vb-message">
            <div className="vb-body">{body}</div>
            {edited ? <div className="vb-edited">{edited}</div> : null}
            {m?.sig ? <div className="vb-sig">__________________<br />{m.sig}</div> : null}
          </td>
        </tr>
      </tbody>
    </table>
  );
}
