import type { Metadata } from "next";
import { announcements } from "@/lib/legacy-content";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Announcements - Copperline Hosting" };

export default function AnnouncementsPage() {
  return <LegacyShell active="news"><Breadcrumbs>Announcements</Breadcrumbs><OldPageTitle>Announcements</OldPageTitle><div className="announcement-list">{announcements.map(item => <article id={item.id} key={item.id}><h2>{item.title}</h2><div className="old-post-meta">Posted on {item.date} by Copperline Staff</div><p>{item.body}</p></article>)}</div></LegacyShell>;
}
