import Link from "next/link";

type Active = "home" | "games" | "servers" | "news" | "support" | "community";

const nav: Array<[Active, string, string]> = [
  ["home", "/", "Home"],
  ["games", "/game-servers.php", "Game Servers"],
  ["servers", "/server-list.php", "Server List"],
  ["news", "/announcements.php", "Announcements"],
  ["support", "/support/index.php", "Support"],
  ["community", "/community/index.php", "Community"],
];

export function LegacyShell({ active, children }: { active: Active; children: React.ReactNode }) {
  return (
    <div className="legacy-site">
      <div className="legacy-utility">
        <div><span>Legacy customer archive</span><span>New sales and support requests are closed</span></div>
        <div><Link href="/clientarea.php">Client Area</Link><Link href="/support/index.php">Support</Link><Link href="/status">Network Status</Link></div>
      </div>
      <header className="legacy-header">
        <Link href="/" className="copperline-logo" aria-label="Copperline Hosting home">
          <strong>copper<span>line</span></strong>
          <small>WEB &amp; GAME SERVER HOSTING</small>
        </Link>
        <div className="legacy-phone"><b>Sales &amp; Support</b><span>Live chat: offline</span><small>Existing account pages are read only</small></div>
      </header>
      <nav className="legacy-nav" aria-label="Primary navigation">
        {nav.map(([key, href, label]) => <Link key={key} href={href} className={active === key ? "active" : ""}>{label}</Link>)}
      </nav>

      <div className="legacy-body">
        <aside className="legacy-left">
          <section className="legacy-box account-box">
            <h2>Client Login</h2>
            <div className="legacy-box-body">
              <label>Username<input disabled aria-label="Username" /></label>
              <label>Password<input disabled type="password" aria-label="Password" /></label>
              <button type="button" disabled>Login</button>
              <p>Legacy billing access has been disabled.</p>
            </div>
          </section>
          <section className="legacy-box">
            <h2>Service Status</h2>
            <div className="legacy-box-body service-list">
              <div><i className="old-dot green" /><span>Web hosting</span><b>Online</b></div>
              <div><i className="old-dot amber" /><span>Game panel</span><b>Read only</b></div>
              <div><i className="old-dot grey" /><span>New orders</span><b>Closed</b></div>
            </div>
          </section>
          <section className="legacy-box">
            <h2>Latest News</h2>
            <div className="legacy-box-body old-news-list">
              <Link href="/announcements.php#panel">Legacy panel access</Link><time>Nov 03, 2014</time>
              <Link href="/announcements.php#billing">Billing system maintenance</Link><time>Jul 20, 2014</time>
              <Link href="/announcements.php#java">Java 7 image update</Link><time>Apr 11, 2013</time>
            </div>
          </section>
          <div className="legacy-badge"><b>PAYPAL</b><span>verified merchant</span></div>
        </aside>

        <main className="legacy-content">{children}</main>
      </div>

      <footer className="legacy-footer">
        <div><Link href="/">Home</Link> | <Link href="/support/index.php">Terms of Service</Link> | <Link href="/support/index.php">Privacy Policy</Link> | <Link href="/support/index.php">Contact Us</Link></div>
        <p>Copyright © 2009–2014 Copperline Hosting. All rights reserved.</p>
        <small>Copperline Hosting is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.</small>
      </footer>
    </div>
  );
}

export function Breadcrumbs({ children }: { children: React.ReactNode }) {
  return <div className="old-breadcrumbs"><Link href="/">Home</Link> » {children}</div>;
}

export function OldPageTitle({ children, sub }: { children: React.ReactNode; sub?: string }) {
  return <header className="old-page-title"><h1>{children}</h1>{sub ? <p>{sub}</p> : null}</header>;
}
