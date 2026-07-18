import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { P4_COPPERLINE_ROUTE } from '@/lib/copperline-p4-route';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'Community Blog - Copperline Hosting' };

const posts = [
  { date: 'May 14, 2012', user: 'craftdad', title: 'Bukkit permissions after the update', body: 'Posting this in case anybody else lost their groups.yml after moving versions. Stop the server before replacing the file or the panel writes the old one back.' },
  { date: 'January 20, 2012', user: 'ryan88', title: 'Chicago node latency tonight', body: 'Seeing about 110ms from Ohio instead of the usual 35. Support says one of the upstream routes is being worked on.' },
  { date: 'August 3, 2011', user: 'MapleAdmin', title: 'Our spawn contest screenshots', body: 'Thanks to everybody who built something. Album link is in the forum thread. We will leave the old spawn warp up through Friday.' },
  { date: 'February 11, 2011', user: 'mkept', title: 'old copy opens without mods', body: 'The recovered world opens in the listed Java version. Both retained cartridges open, but one wall notice does not match.', href: P4_COPPERLINE_ROUTE.disagreementPost },
  { date: 'February 8, 2011', user: 'mkept', title: 'world backup for the old server', body: 'A few people asked for the last world copy. The attachment is on the full post; the checksum notes are on the old static mirror.', href: P4_COPPERLINE_ROUTE.priorBackupPost },
  { date: 'December 19, 2010', user: 'jon_c', title: 'Server icons', body: 'Does the public list support server-icon.png yet or is that only in the newer Minecraft builds? Mine still shows the default grass block.' },
];

export default async function CommunityPage({ searchParams }: { searchParams: Promise<{ user?: string }> }) {
  const user = (await searchParams).user;
  const recurated = await hasCampaignEvent('p5.civic_gallery_recurated');
  const sixPeople = await hasCampaignEvent('p6.six_responsibilities_acknowledged');
  const materialProven = await hasCampaignEvent('p7.counterfeit_material_proven');
  const nessaCleared = await hasCampaignEvent('p7.nessa_publicly_cleared');
  const p8Planned = await hasCampaignEvent('p8.intervention_plan_accepted');
  const p8UnlitCompared = await hasCampaignEvent('p8.unlit_house_synthesis_completed');
  const p8Repaired = await hasCampaignEvent('p8.hold_systems_repaired');
  const p9People = await hasCampaignEvent('p9.company_biographies_restored');
  const p9Leak = await hasCampaignEvent('p9.leak_window_proven');
  const p10CopyProof = await hasCampaignEvent('p10.player_copy_proof');
  const p10Confronted = await hasCampaignEvent('p10.wren_confronted');
  const p10Remembered = await hasCampaignEvent('p10.wren_remembrance_committed');
  const p11Identified = await hasCampaignEvent('p11.averyn_identified');
  const p11Unbound = await hasCampaignEvent('p11.averyn_restored_unbound');
  const p12Released = await hasCampaignEvent('p12.record_closed_averyn_released');
  const p5Posts = recurated === true ? [
    { date: 'February 16, 2011', user: 'ashfield', title: 'that room was a service counter', body: 'The uncropped frame has wick shears, school chalk, and sample rings. Copperline corrected the archive caption after the service-card review.', href: '/community/2011/02/16/service-counter' },
    ...posts,
  ] : posts;
  const currentPosts = sixPeople === true ? [
    { date: 'March 3, 2011', user: 'mkept', title: 'six workspace owners, not one admin', body: 'The export uses one permission role, but the work separates six people. Keep their methods and corrections apart.', href: '/community/2011/03/03/six-workspaces' },
    ...p5Posts,
  ] : p5Posts;
  const materialPosts = materialProven === true ? [
    { date: 'March 11, 2011', user: 'copperline-support', title: 'cistern cloth attachment history restored', body: 'A retained material comparison reopened two collapsed supplier attachment versions. Both originals remain read-only.', href: '/community/archive/supplier-revisions' },
    ...currentPosts,
  ] : currentPosts;
  const finalPosts = nessaCleared === true ? [
    { date: 'March 14, 2011', user: 'mkept', title: 'correction to the cistern file', body: 'The material, labor, and record findings do not support the old accusation against Nessa Vale.', href: '/community/2011/03/14/nessa-correction' },
    ...materialPosts,
  ] : materialPosts;
  const p8Posts = p8Planned === true ? [
    { date: 'April 2, 2011', user: 'ashfield', title: p8Repaired === true ? 'the Hold works: repair readback attached' : p8UnlitCompared === true ? 'the Hold works: base comparison attached' : 'the Hold works: accepted plan', body: p8Repaired === true ? 'The current group restored water, paired light, pressure control, and the staff route in the tested order. Before and altered states remain preserved.' : p8UnlitCompared === true ? 'Seven copied-house findings now sit beside the works before-state. Copperline kept their disagreements instead of merging them into one clean account.' : 'The incident board keeps four interacting failures, Iss’s sound surface proof, his unsafe route, and a safe intervention order separate.', href: '/community/2011/04/02/hold-works' },
    ...finalPosts,
  ] : finalPosts;
  const p9Posts = p8Repaired === true ? [
    { date: 'April 19, 2011', user: p9People === true ? 'mkept' : 'copperline-support', title: p9Leak === true ? 'Ash Camp: private revision window preserved' : p9People === true ? 'Ash Camp: four owner cards restored' : 'Ash Camp: owner recovery set', body: p9Leak === true ? 'The release board was complete. A private Rook revision reached the Witness Spool before public upload; the source remains unresolved.' : p9People === true ? 'The camp record now preserves mkept, Ash, Rook, and Wren as people with crossed work and relationships.' : 'Linked maintenance records reopened a camp archive that the old export reduced to four unlabeled stations.', href: '/community/archive/ash-camp' },
    ...p8Posts,
  ] : p8Posts;
  const p10Posts = p9Leak === true ? [
    { date: 'April 27, 2011', user: 'copperline-moderation', title: p10Remembered === true ? 'comment 4417: finding and remembrance attached' : p10Confronted === true ? 'comment 4417: transmission finding attached' : p10CopyProof === true ? 'comment 4417: mirror return attached' : 'comment 4417: retained moderation chronology', body: p10Remembered === true ? 'The group’s remembrance choice now sits beside fixed attribution facts and an unchanged moderation history.' : p10Confronted === true ? 'The current finding is attached to packet progression and Rook’s counter-mark; Copperline did not restore a missing confession.' : p10CopyProof === true ? 'A bounded six-mark return is now preserved beside the old chronology. Copperline stores the pattern receipt and the one-mark difference, not player text or identity.' : 'A removed Wren comment retains account, edit, report, moderation, and public-upload times—but not the missing words.', href: '/community/archive/wren-moderation' },
    ...p9Posts,
  ] : p9Posts;
  const p11Posts = p10Remembered === true ? [
    { date: 'May 9, 2011', user: 'copperline-archive', title: p11Unbound === true ? 'recovered packet: identity restored outside the Record' : p11Identified === true ? 'recovered packet: identity receipt attached' : 'recovered packet: five-member custody index', body: p11Unbound === true ? 'Averyn is restored as the human registrar. Record, Watcher, Averyn, and Dark remain related without being collapsed.' : p11Identified === true ? 'Six independent affidavit paths yield a person’s name, not a seventh Keeper role.' : 'A retained ZIP, inventory, field audio, intake image, and lamp scan must be reviewed as one custody object.', href: '/community/archive/recovered-packet' },
    ...p10Posts,
  ] : p10Posts;
  const p12Posts = p12Released === true ? [
    { date: 'May 18, 2011', user: 'copperline-archive', title: 'the archive is closed; the history stays readable', body: 'A committed local release receipt reached the mirror. The Record is closed, Averyn is released, and earlier posts and versions remain unchanged.', href: '/community/2011/05/18/archive-closed' },
    ...p11Posts,
  ] : p11Posts;
  const visible = user ? p12Posts.filter((post) => post.user.toLowerCase() === user.toLowerCase()) : p12Posts;
  return (
    <LegacyShell active="community">
      <Breadcrumbs>Community Blog</Breadcrumbs>
      <OldPageTitle sub={user ? `Posts filed under user “${user}”.` : 'Tips, updates and files posted by Copperline customers.'}>Community Blog</OldPageTitle>
      {user ? <div className="old-filter-note">Showing posts by <b>{user}</b>. <Link href={P4_COPPERLINE_ROUTE.community}>Clear filter</Link></div> : null}
      <div className="community-posts">
        {visible.map((post) => (
          <article key={`${post.user}-${post.title}`}>
            <div className="blog-date"><b>{post.date.split(' ')[1]?.replace(',', '')}</b><span>{post.date.split(' ')[0]}<br />{post.date.split(' ')[2]}</span></div>
            <div>
              <h2>{post.href ? <Link href={post.href}>{post.title}</Link> : post.title}</h2>
              <p className="old-post-meta">Posted by <Link href={`/community/index.php?user=${post.user}`}>{post.user}</Link> · 0 comments</p>
              <p>{post.body}</p>
              {post.href ? <Link href={post.href} className="small-old-link">Continue reading »</Link> : null}
            </div>
          </article>
        ))}
      </div>
    </LegacyShell>
  );
}
