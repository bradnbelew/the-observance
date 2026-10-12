import type { Metadata } from "next";
import Link from "next/link";
import { BOARDS, ForumShell, PostBox } from "@/components/morrow/Forum";
import { bundle, threadById } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "Copperline Community Forums" };
export const dynamic = "force-dynamic";

export default async function ShowThread({ searchParams }: { searchParams: Promise<{ t?: string; view?: string }> }) {
  const sp = await searchParams;
  const t = Number(sp.t);
  const print = sp.view === "print";
  const b = await bundle();
  const thread = threadById(b, t);
  if (!thread) {
    return <ForumShell crumbs={null}><div className="vb-notice">No Thread specified. If you followed a valid link, please notify the administrator</div></ForumShell>;
  }
  return (
    <ForumShell crumbs={<> » <Link href="/forum-import/forumdisplay.php?f=12">{BOARDS[12].title}</Link> » {thread.title}</>}>
      <h1 className="vb-thread-title">{thread.title}</h1>
      {print ? <div className="vb-notice">Printable version (cached copy, 19 May 2011)</div> : null}
      {thread.posts.map((p, i) => <PostBox key={p.id} post={p} b={b} index={i} print={print} />)}
      <p className="vb-desc"><Link href={`/forum-import/showthread.php?t=${thread.id}&view=print`}>Printable version</Link></p>
    </ForumShell>
  );
}
