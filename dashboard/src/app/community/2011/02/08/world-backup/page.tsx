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
  return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community Blog</Link> » world backup for the old server</Breadcrumbs><article className="single-old-post"><h1>world backup for the old server</h1><div className="old-post-meta">Posted by <Link href="/community/index.php?user=mkept">mkept</Link> on February 8, 2011 at 11:42 PM · mirrored November 3, 2014</div><p>A few people asked me to put the final backup somewhere that does not require the old panel login. This is the last single-player copy I kept. Player files, chat logs, and the plain server address were removed before upload; the dispatch records inside the world were left as filed.</p><p>Extract the folder into the normal saves directory and open it as a world. It sets up its own path the first time it loads. Do not install the zip as a datapack and do not merge it into an older copy.</p><div className="old-attachment"><b>Attachment retained with post</b><div><span className="zip-icon">ZIP</span><p><strong>the-hold.zip</strong><small>{size} · archived upload · checksum retained on mirror</small></p>{available ? <a href="/the-hold/the-hold.zip" download>Download</a> : <em>Unavailable</em>}</div></div><p>The blog editor changed the spacing in the copy register, so I left the checksum and page-order notes on my <Link href="/record/the-record-keeps" className="ordinary-inline-link">static file mirror</Link>.</p><p className="post-signature">— m.</p><footer className="old-post-footer">Filed under: <Link href="/community/index.php">Minecraft</Link>, <Link href="/community/index.php">World Downloads</Link> · Comments are closed.</footer></article></LegacyShell>;
}
