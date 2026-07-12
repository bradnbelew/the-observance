import type { Metadata } from "next";
import Link from "next/link";
import { LegacyShell } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = {
  title: "Copperline Hosting - Affordable Web and Game Server Hosting",
  description: "Affordable Minecraft and game server hosting from Copperline Hosting.",
};

const plans = [
  ["Starter", "512 MB", "10", "$3.95"],
  ["Builder", "1 GB", "20", "$6.95"],
  ["Community", "2 GB", "40", "$11.95"],
];

export default function HomePage() {
  return (
    <LegacyShell active="home">
      <section className="old-hero">
        <div><h1>Minecraft server hosting<br /><span>without the hassle.</span></h1><p>Instant setup, a simple control panel and friendly support. Plans start at only $3.95 per month.</p><Link href="/game-servers.php" className="old-button">View Minecraft Plans »</Link></div>
        <div className="old-server-art" aria-hidden><i /><i /><i /><span>99.9%<small>NETWORK UPTIME</small></span></div>
      </section>

      <div className="old-alert"><b>Archived notice — December 1, 2014:</b> Copperline no longer accepts orders, payments, uploads, or support requests. Public directory records remain available exactly as retained.</div>

      <section className="old-section">
        <h2 className="old-rule-title">Minecraft Hosting Plans</h2>
        <div className="old-plans">
          {plans.map(([name, memory, slots, price], index) => (
            <div key={name} className={index === 1 ? "featured" : ""}>
              {index === 1 ? <em>POPULAR</em> : null}<h3>{name}</h3><strong><sup>$</sup>{price.replace("$", "")}<small>/mo</small></strong>
              <ul><li>{memory} dedicated RAM</li><li>{slots} player slots</li><li>TCAdmin control panel</li><li>Daily backup slot</li></ul>
              <span className="closed-order">Ordering Closed</span>
            </div>
          ))}
        </div>
      </section>

      <div className="old-columns">
        <section className="old-section"><h2 className="old-rule-title">Why Copperline?</h2><ul className="old-checks"><li><b>Instant Setup</b><span>Your game server is installed automatically after payment.</span></li><li><b>Full Control</b><span>Start, stop and manage files through TCAdmin.</span></li><li><b>US Locations</b><span>Low-latency nodes in Chicago, Dallas and Atlanta.</span></li><li><b>No Contracts</b><span>Pay monthly and cancel through the client area.</span></li></ul></section>
        <section className="old-section"><h2 className="old-rule-title">Company News</h2><article className="home-news"><time>Nov 03, 2014</time><h3><Link href="/announcements.php#panel">Legacy panel access</Link></h3><p>The TCAdmin 1 panel is now available in read-only mode for expired accounts...</p></article><article className="home-news"><time>Jul 20, 2014</time><h3><Link href="/announcements.php#billing">Billing system maintenance</Link></h3><p>The client area will be unavailable Sunday during scheduled maintenance...</p></article><Link href="/announcements.php" className="small-old-link">View all announcements »</Link></section>
      </div>

      <section className="old-testimonial"><b>From the 2012 customer survey</b><blockquote>“Setup was quick and the Chicago connection has been solid. Support moved our old world over the same day.”</blockquote><span>— Daniel R., Builder plan customer</span></section>
    </LegacyShell>
  );
}
