import type { Metadata } from "next";
import Link from "next/link";
import { ForumShell } from "@/components/morrow/Forum";
import { bundle } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "Members List - Copperline Community Forums" };
export const dynamic = "force-dynamic";

export default async function MemberList() {
  const b = await bundle();
  const rows = Object.entries(b.members)
    .filter(([name]) => name !== "archivist")
    .sort(([, a], [, z]) => a.joined.localeCompare(z.joined));
  return (
    <ForumShell crumbs={<> » Members List</>}>
      <table className="vb-table">
        <thead><tr><th>User Name</th><th>Join Date</th><th>Posts</th></tr></thead>
        <tbody>{rows.map(([name, m]) => (
          <tr key={name}>
            <td><Link href={`/forum-import/member.php?u=${encodeURIComponent(name)}`}>{name}</Link><div className="vb-desc">{m.title}</div></td>
            <td>{m.joined}</td><td>{m.posts}</td>
          </tr>
        ))}</tbody>
      </table>
    </ForumShell>
  );
}
