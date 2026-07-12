import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Community Blog - Copperline Hosting" };

const posts = [
  { date: "May 14, 2012", user: "craftdad", title: "Bukkit permissions after the update", body: "Posting this in case anybody else lost their groups.yml after moving versions. Stop the server before replacing the file or the panel writes the old one back." },
  { date: "January 20, 2012", user: "ryan88", title: "Chicago node latency tonight", body: "Seeing about 110ms from Ohio instead of the usual 35. Support says one of the upstream routes is being worked on." },
  { date: "August 3, 2011", user: "MapleAdmin", title: "Our spawn contest screenshots", body: "Thanks to everybody who built something. Album link is in the forum thread. We will leave the old spawn warp up through Friday." },
  { date: "February 8, 2011", user: "mkept", title: "world backup for the old server", body: "A few people asked for the last world copy. I put the download and the original notes on their own page so this post does not choke on the file." },
  { date: "December 19, 2010", user: "jon_c", title: "Server icons", body: "Does the public list support server-icon.png yet or is that only in the newer Minecraft builds? Mine still shows the default grass block." },
];

export default async function CommunityPage({ searchParams }: { searchParams: Promise<{ user?: string }> }) {
  const user = (await searchParams).user;
  const visible = user ? posts.filter(post => post.user.toLowerCase() === user.toLowerCase()) : posts;
  return <LegacyShell active="community"><Breadcrumbs>Community Blog</Breadcrumbs><OldPageTitle sub={user ? `Posts filed under user “${user}”.` : "Tips, updates and files posted by Copperline customers."}>Community Blog</OldPageTitle>{user ? <div className="old-filter-note">Showing posts by <b>{user}</b>. <Link href="/community/index.php">Clear filter</Link></div> : null}<div className="community-posts">{visible.map(post => <article key={`${post.user}-${post.title}`}><div className="blog-date"><b>{post.date.split(" ")[1]?.replace(",", "")}</b><span>{post.date.split(" ")[0]}<br />{post.date.split(" ")[2]}</span></div><div><h2>{post.title === "world backup for the old server" ? <Link href="/community/2011/02/08/world-backup/">{post.title}</Link> : post.title}</h2><p className="old-post-meta">Posted by <Link href={`/community/index.php?user=${post.user}`}>{post.user}</Link> · 0 comments</p><p>{post.body}</p>{post.title === "world backup for the old server" ? <Link href="/community/2011/02/08/world-backup/" className="small-old-link">Continue reading »</Link> : null}</div></article>)}</div></LegacyShell>;
}
