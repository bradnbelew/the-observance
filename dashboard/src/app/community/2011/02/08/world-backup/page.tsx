import type { Metadata } from "next";
import { existsSync, statSync } from "node:fs";
import { join } from "node:path";
import Link from "next/link";
import { Breadcrumbs, LegacyShell } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "world backup for the old server - Copperline Community" };

export default function WorldBackupPost() {
  const path = join(process.cwd(), "public", "the-hold", "the-hold.zip");
  const available = existsSync(path);
  const size = available ? `${Math.max(1, Math.round(statSync(path).size / 1024))} KB` : "file missing";
  return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community Blog</Link> » world backup for the old server</Breadcrumbs><article className="single-old-post"><h1>world backup for the old server</h1><div className="old-post-meta">Posted by <Link href="/community/index.php?user=mkept">mkept</Link> on February 8, 2011 at 11:42 PM</div><p>A few people asked me to put the final backup somewhere that does not require the old panel login. This is the copy from before the account was closed. Player files and server logs have been removed.</p><p>It is a normal single-player world. Use the Minecraft version listed in the included text file. If you appear outside the path, walk north until you reach the first stone stair.</p><div className="old-attachment"><b>Attachment</b><div><span className="zip-icon">ZIP</span><p><strong>the-hold.zip</strong><small>{size} · uploaded February 8, 2011</small></p>{available ? <a href="/the-hold/the-hold.zip" download>Download</a> : <em>Unavailable</em>}</div></div><p>I kept the uploader notes separate because the blog editor changed the spacing. They are still on the <Link href="/record/the-record-keeps" className="ordinary-inline-link">plain mirror page</Link>.</p><p className="post-signature">— m.</p><footer className="old-post-footer">Filed under: <Link href="/community/index.php">Minecraft</Link>, <Link href="/community/index.php">World Downloads</Link> · Comments are closed.</footer></article></LegacyShell>;
}
