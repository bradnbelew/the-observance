import { existsSync, statSync } from "node:fs";
import { join } from "node:path";
import Image from "next/image";
import Link from "next/link";

const HOLD_ZIP = "/the-hold/the-hold.zip";

function recoveredFile() {
  const path = join(process.cwd(), "public", "the-hold", "the-hold.zip");
  if (!existsSync(path)) return { present: false, size: "unavailable" };
  return { present: true, size: `${Math.max(1, Math.round(statSync(path).size / 1024))} KiB` };
}

const serverFacts = [
  ["Game", "Minecraft: Java Edition"],
  ["Version", "1.21.11"],
  ["Slots", "7"],
  ["Access", "Whitelist"],
  ["Resource pack", "Required"],
  ["Staff", "No public contacts"],
];

const incidents = [
  ["2011-02-06", "Map mirror replaced after a corrupted-room report.", "resolved"],
  ["2014-07-19", "Account owner removed the public staff list.", "archived"],
  ["2014-07-21", "Seventh whitelist row returned without a name.", "unresolved"],
  ["2020-11-02", "World image restored from mirror 03 cold storage.", "partial"],
];

export default function HomePage() {
  const file = recoveredFile();
  const configuredAddress = process.env.NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS?.trim();

  return (
    <main className="host-site">
      <div className="host-noise" aria-hidden />
      <header className="host-topbar">
        <Link href="/" className="host-brand"><span>SNOIKERZ</span><small>free game hosting</small></Link>
        <nav aria-label="Host navigation">
          <span>servers</span><span>maps</span><Link href="/status">network status</Link>
        </nav>
      </header>

      <div className="host-wrap">
        <div className="host-breadcrumbs">home / servers / archived / <strong>the observance</strong></div>

        <section className="listing-hero">
          <div className="listing-icon">
            <Image src="/keeper-eye.svg" alt="The Observance server icon" width={112} height={112} priority />
          </div>
          <div className="listing-title">
            <div className="listing-state"><i /> archived listing · mirror 03</div>
            <h1>The Observance</h1>
            <p className="motd">count first. walk low. what is shut is shut for a reason.</p>
            <div className="listing-tags"><span>survival</span><span>whitelist</span><span>custom pack</span><span>unlisted</span></div>
          </div>
          <div className="listing-availability">
            <strong>0 / 7</strong>
            <span>last query timed out</span>
          </div>
        </section>

        <div className="host-grid">
          <div className="host-main">
            <section className="host-card address-card">
              <div className="host-card-title"><h2>Connection</h2><span>public row retained</span></div>
              <p>The server row was restored with the map mirror. It may only answer while the host is awake.</p>
              <div className={`server-address ${configuredAddress ? "is-live" : "is-withheld"}`}>
                <span>{configuredAddress || "ADDRESS WITHHELD"}</span>
                <small>{configuredAddress ? "Java Edition · copy exactly" : "operator has not opened the host"}</small>
              </div>
              <p className="host-warning">Accept the server resource pack. The carved alphabet is not legible without it.</p>
            </section>

            <section className="host-card">
              <div className="host-card-title"><h2>Recovered files</h2><span>1 item</span></div>
              <div className="file-row">
                <div className="file-type">ZIP</div>
                <div><strong>the-hold.zip</strong><p>Offline world copy · single player · map mirror only</p></div>
                <div className="file-meta"><span>{file.size}</span>{file.present ? <a href={HOLD_ZIP} download>download</a> : <em>not recovered</em>}</div>
              </div>
              <div className="mirror-note">
                “I copied it as it was given, page for page. The rest of the record is kept elsewhere.”
                <span>— m.kept, uploader note</span>
              </div>
            </section>

            <section className="host-card incident-card">
              <div className="host-card-title"><h2>Archived incidents</h2><span>read-only cache</span></div>
              <div className="incident-table">
                {incidents.map(([date, event, state]) => (
                  <div key={date}><time>{date}</time><p>{event}</p><span>{state}</span></div>
                ))}
              </div>
            </section>

            <section className="legacy-comment">
              <div className="legacy-avatar">MK</div>
              <div><strong>m.kept</strong><time> · February 8, 2011</time><p>The copy plays through. The last room does not end there. Read the line again.</p></div>
            </section>
          </div>

          <aside className="host-sidebar">
            <section className="host-card">
              <div className="host-card-title"><h2>Server details</h2></div>
              <dl>{serverFacts.map(([term, value]) => <div key={term}><dt>{term}</dt><dd>{value}</dd></div>)}</dl>
            </section>
            <section className="host-card archive-index">
              <div className="host-card-title"><h2>Related cache</h2></div>
              <Link href="/record/the-record-keeps"><span>recovered listing file</span><small>/record/the-record-keeps</small></Link>
              <Link href="/record/the-record"><span>unindexed record</span><small>/record/the-record</small></Link>
              <Link href="/record/archive"><span>recovery archive</span><small>/record/archive</small></Link>
            </section>
            <section className="host-terms"><strong>Notice</strong><p>This is an abandoned listing. SNOIKERZ does not verify map contents, staff claims, or off-site records.</p></section>
          </aside>
        </div>
      </div>

      <footer className="host-footer"><span>© 2008–2014 SNOIKERZ Hosting</span><span>mirror 03 · static recovery</span></footer>
    </main>
  );
}
