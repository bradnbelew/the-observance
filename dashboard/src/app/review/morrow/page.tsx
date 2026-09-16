import type { Metadata } from 'next';
import Link from 'next/link';
import type { CSSProperties } from 'react';
import {
  antiPuzzleRoomRules,
  directorControls,
  discoveries,
  evidenceFormats,
  handmadeMedia,
  investigationTrails,
  mysteryRules,
  morrowVoice,
  recurringMotifs,
  reviewActs,
  reviewGates,
  scares,
  websiteInvestigations,
  weekScript,
} from '@/lib/morrow-review';

export const metadata: Metadata = {
  title: 'Morrow ARG — Creative Review Build',
  description: 'Local-only review dossier for the redesigned Copperline and Mossfield ARG.',
  robots: { index: false, follow: false },
};

export const dynamic = 'force-static';

const surfaceCards = [
  {
    key: 'COPPERLINE',
    title: 'One ordinary domain, several buried layers',
    text: 'Hosting pages → community forum → staff blog → status archive → support files → fictional employee login → recovery console.',
    sample: 'status/chicago-03 :: incident 6118 :: public time edited :: uptime 04:17:22',
  },
  {
    key: 'MINECRAFT',
    title: 'A lived-in world that becomes unreliable',
    text: 'Books, lecterns, chests, maps, renamed tools, rails, signs, ruins, redstone, short NPC dialogue, and bounded apparitions.',
    sample: 'cairn/storehouse → uneven chest labels → lectern index → maintenance cache',
  },
  {
    key: 'MEDIA',
    title: 'Found footage, not exposition',
    text: 'A horizon photograph, frame-counted Minecraft footage, contaminated metadata, reversed audio, and an accessible spectrogram derivative.',
    sample: 'REC_2011-04-18.ogv :: frame 0037 :: background lever changes once',
  },
  {
    key: 'DISCORD',
    title: 'Private pressure, shared recovery',
    text: 'Morrow sends different authored fragments to linked players and tries to turn them against one another. Group progress uses short codes, never private prose.',
    sample: 'fragment B4 :: “Ask why they opened it first.” :: expires after safe reissue window',
  },
] as const;

export default function MorrowReviewPage() {
  return (
    <main className="morrow-review">
      <header className="morrow-review-hero">
        <div className="morrow-review-signal" aria-hidden="true"><span /><span /><span /><span /></div>
        <div>
          <p className="morrow-review-kicker">Local creative dossier · full spoilers · no live state</p>
          <h1>MORROW</h1>
          <p className="morrow-review-deck">A recovered Minecraft server remembers too much—and begins
            using the people investigating it as missing history.</p>
          <div className="morrow-review-tags">
            <span>found-footage horror</span><span>week-long ARG</span><span>1–6 players</span><span>single shutdown ending</span>
          </div>
        </div>
        <aside>
          <b>REVIEW BUILD</b>
          <p>Static fixtures only. No Supabase, Discord, Minecraft, or production mutation occurs from this page.</p>
          <code>morrow-reboot.enabled = false</code>
        </aside>
      </header>

      <nav className="morrow-review-nav" aria-label="Review sections">
        <a href="#north-star">North star</a><a href="#acts">Acts</a><a href="#mystery">Mystery</a><a href="#investigation">Investigation</a><a href="#gates">Gates</a>
        <a href="#artifacts">Artifacts</a><a href="#voice">Morrow</a><a href="#scares">Scares</a>
        <a href="#week">Launch script</a><a href="#director">Director</a>
      </nav>

      <section id="north-star" className="morrow-review-section morrow-review-north">
        <div>
          <p className="morrow-review-label">Creative north star</p>
          <h2>Morrow is the phenomenon.<br />It is not the puzzle host.</h2>
        </div>
        <div className="morrow-review-prose">
          <p>Players should feel they stumbled into private, unfinished Copperline material. Clues exist
            because residents, employees, automated systems, and Morrow left traces—not because a character
            built challenge rooms for an audience.</p>
          <p>The mystery escalates from curiosity to paranoia and sympathy, then forces one conclusion:
            shut Morrow down. The lingering question is whether the group stopped a danger or erased the
            last witness to Mossfield.</p>
        </div>
        <dl className="morrow-review-stats">
          <div><dt>Acts</dt><dd>5</dd></div><div><dt>Required gates</dt><dd>15</dd></div>
          <div><dt>Discoveries</dt><dd>24</dd></div><div><dt>Optional lore</dt><dd>9</dd></div>
          <div><dt>Active play</dt><dd>8–12h</dd></div><div><dt>Cadence</dt><dd>7 days</dd></div>
        </dl>
      </section>

      <section id="acts" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Experience arc</p><h2>What players believe changes before what Morrow admits.</h2></div>
        <div className="morrow-review-acts">
          {reviewActs.map((act, index) => <article key={act.id}>
            <div className="morrow-review-act-index">{String(index + 1).padStart(2, '0')}</div>
            <div><p>{act.id} · {act.gates}</p><h3>{act.title}</h3><strong>{act.belief}</strong><span>{act.experience}</span>
              <dl className="morrow-review-act-depth"><div><dt>Wrong theory</dt><dd>{act.falseTheory}</dd></div><div><dt>What lingers</dt><dd>{act.residue}</dd></div></dl>
            </div>
            <footer>{act.morrow}</footer>
          </article>)}
        </div>
      </section>

      <section id="mystery" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Mystery architecture</p><h2>Deepen the wrongness, not the lock count.</h2><p>Every escalation reuses a known place, phrase, habit, or absence. Players form theories because the world behaves coherently—not because a room asks them to compare exhibits.</p></div>
        <div className="morrow-review-mystery-grid">
          {mysteryRules.map(([title, text], index) => <article key={title}><span>{String(index + 1).padStart(2, '0')}</span><h3>{title}</h3><p>{text}</p></article>)}
        </div>
        <div className="morrow-review-motifs">
          <header><p className="morrow-review-label">Recurring residue</p><h3>Five things that mean something different each time they return.</h3></header>
          <ol>{recurringMotifs.map(([motif, meaning]) => <li key={motif}><code>{motif}</code><span>{meaning}</span></li>)}</ol>
        </div>
        <aside className="morrow-review-firewall">
          <p className="morrow-review-label">Anti-puzzle-room firewall</p>
          <ul>{antiPuzzleRoomRules.map((rule) => <li key={rule}>{rule}</li>)}</ul>
        </aside>
      </section>

      <section id="investigation" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Investigation architecture</p><h2>Six questions pursued through every kind of trace.</h2><p>The website is a place to investigate, handmade media is primary evidence, and Minecraft is where conclusions become physical. No single artifact explains itself.</p></div>
        <div className="morrow-review-investigations">
          {investigationTrails.map((trail, index) => <article key={trail.question}>
            <header><span>CASE THREAD {String(index + 1).padStart(2, '0')}</span><h3>{trail.question}</h3></header>
            <dl><div><dt>Copperline</dt><dd>{trail.website}</dd></div><div><dt>Handmade media</dt><dd>{trail.media}</dd></div><div><dt>Mossfield</dt><dd>{trail.world}</dd></div></dl>
            <footer><b>What players can conclude</b><p>{trail.conclusion}</p></footer>
          </article>)}
        </div>

        <div className="morrow-review-subheading"><p className="morrow-review-label">Copperline depth</p><h2>The website rewards actual browsing.</h2><p>Players use normal web habits—following identities, reading edits, inspecting files, revisiting pages, and noticing what vanished.</p></div>
        <div className="morrow-review-web-layers">{websiteInvestigations.map(([layer, detail], index) => <article key={layer}><span>{String(index + 1).padStart(2, '0')}</span><h3>{layer}</h3><p>{detail}</p></article>)}</div>

        <div className="morrow-review-subheading"><p className="morrow-review-label">Handmade media slate</p><h2>Twelve artifacts that feel handled, recorded, damaged, and retained.</h2><p>Every piece is manually authored, first-party, hash-locked before release, and paired with an equivalent that preserves the deduction.</p></div>
        <div className="morrow-review-media-slate">{handmadeMedia.map(([title, format, craft, access], index) => <article key={title}><header><span>{String(index + 1).padStart(2, '0')}</span><code>{format}</code></header><h3>{title}</h3><p>{craft}</p><small>{access}</small></article>)}</div>

        <details className="morrow-review-format-atlas" open>
          <summary>Evidence format atlas <span>{evidenceFormats.length} formats · each with a story job</span></summary>
          <ul>{evidenceFormats.map(([format, role]) => <li key={format}><b>{format}</b><span>{role}</span></li>)}</ul>
        </details>
      </section>

      <section id="gates" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Required spine</p><h2>Fifteen gates. No repeated task-room grammar.</h2><p>Each answer is concrete, resettable, and taught by at least three clue vectors.</p></div>
        <div className="morrow-review-gates">
          {reviewGates.map((gate) => <details key={gate.id} open={gate.id === 'G01' || gate.id === 'G07' || gate.id === 'G15'}>
            <summary><span>{gate.id}</span><b>{gate.title}</b><em>Act {gate.act}</em></summary>
            <div><div className="morrow-review-chips">{gate.mediums.map((medium) => <span key={medium}>{medium}</span>)}</div>
              <p><b>Player action:</b> {gate.action}</p><p><b>Story turn:</b> {gate.revelation}</p><p className="morrow-review-gate-feel"><b>How it feels:</b> {gate.experience}</p></div>
          </details>)}
        </div>
        <details className="morrow-review-discoveries">
          <summary>Inspect all 24 meaningful discoveries <span>15 spine · 9 optional lore</span></summary>
          <ol>{discoveries.map(([id, title, required]) => <li key={id} data-required={required}><code>{id}</code><span>{title}</span><em>{required ? 'spine' : 'optional'}</em></li>)}</ol>
        </details>
      </section>

      <section id="artifacts" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Surface composition</p><h2>The same mystery, four different textures.</h2></div>
        <div className="morrow-review-surfaces">
          {surfaceCards.map((surface) => <article key={surface.key}>
            <header><span>{surface.key}</span><i aria-hidden /></header><h3>{surface.title}</h3><p>{surface.text}</p><code>{surface.sample}</code>
          </article>)}
        </div>
        <div className="morrow-review-artifact-grid">
          <article className="morrow-review-browser">
            <header><i /><i /><i /><span>archive.copperline.invalid/status/chicago-03</span></header>
            <div><p>COPPERLINE NETWORK OPERATIONS</p><h3>Incident 6118 — storage controller</h3>
              <dl><div><dt>PUBLIC TIME</dt><dd><s>02:10</s> 03:42 CST</dd></div><div><dt>UPTIME</dt><dd>04:17:22</dd></div><div><dt>STATE</dt><dd>RESOLVED / MIRROR OPEN</dd></div></dl>
              <small>&lt;!-- employee recovery index: use uptime, not post time --&gt;</small></div>
          </article>
          <article className="morrow-review-minecraft">
            <header>MOSSFIELD // cairn’s storehouse</header>
            <pre>{`┌──────────── north wall ────────────┐
│ [FARM] [ODDS] [TOOLS] [STONE?]    │
│                                    │
│      lectern: “leave it uneven”    │
│                         ▒ doorway  │
└────────────────────────────────────┘`}</pre>
            <blockquote>“If the labels make sense, that isn’t my room.” <cite>— cairn, unsigned book</cite></blockquote>
          </article>
          <article className="morrow-review-dms">
            <header>LINKED PLAYER FRAGMENTS</header>
            <div><b>to: P01</b><p>They opened the storehouse before you arrived.</p><code>B4</code></div>
            <div><b>to: P02</b><p>You were the only one who waited. Keep it that way.</p><code>A7</code></div>
            <footer>Group input uses codes A7 + B4. Private prose is never required.</footer>
          </article>
        </div>
      </section>

      <section id="voice" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Authored voice progression</p><h2>It never announces that it has changed.</h2></div>
        <div className="morrow-review-voice">
          {morrowVoice.map(([state, line], index) => <article key={state} style={{ '--stage': index } as CSSProperties}>
            <span>{String(index + 1).padStart(2, '0')}</span><b>{state}</b><p>{line}</p>
          </article>)}
        </div>
        <p className="morrow-review-boundary">Personalization may reference only authored records and
          behavior knowingly performed inside the ARG. Morrow never implies access to cameras, microphones,
          location, unrelated accounts, files, or browsing history.</p>
      </section>

      <section id="scares" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Dread score</p><h2>Twelve intrusions. No haunted-house conveyor belt.</h2><p>Quiet stretches remain quiet. The scares return to places the players already trust, escalate from doubt to imitation, and never consume evidence.</p></div>
        <div className="morrow-review-scares">
          {scares.map(([name, timing, moment, boundary], index) => <article key={name}><span>{String(index + 1).padStart(2, '0')}</span><div><p>{timing}</p><h3>{name}</h3><strong>{moment}</strong><small>{boundary}</small></div></article>)}
        </div>
      </section>

      <section id="week" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Week-long launch script</p><h2>Space discoveries out; let players live with the wrong theory.</h2></div>
        <ol className="morrow-review-week">
          {weekScript.map(([day, player, director]) => <li key={day}><time>{day}</time><div><b>Player-facing beat</b><p>{player}</p></div><div><b>Director note</b><p>{director}</p></div></li>)}
        </ol>
      </section>

      <section id="director" className="morrow-review-section">
        <div className="morrow-review-heading"><p className="morrow-review-label">Director dashboard concept</p><h2>Show control, not a solution engine.</h2></div>
        <div className="morrow-review-director">
          <header><div><i /><b>MOSSFIELD / REVIEW-ONLY</b></div><span>PRODUCTION LOCKED</span></header>
          <aside><b>ACT 2 · DEFENSIVE</b><span>Gate G09 complete</span><span>4 linked players</span><span>0 leaked tasks</span></aside>
          <section>
            {directorControls.map(([control, requirement]) => <button key={control} type="button" disabled><b>{control}</b><span>{requirement}</span></button>)}
          </section>
          <footer><code>14:32:08</code><span>preview fixture loaded — controls disabled — no external target bound</span></footer>
        </div>
      </section>

      <section className="morrow-review-section morrow-review-links">
        <div><p className="morrow-review-label">Existing local surfaces</p><h2>Compare the direction to the current implementation.</h2><p>These routes remain historical or partial; they are useful texture references, not proof that G01–G15 is built.</p></div>
        <nav><Link href="/">Copperline host</Link><Link href="/community/index.php">Community archive</Link><Link href="/status">Status archive</Link><Link href="/support/account">Recovery login</Link><Link href="/support/cases/mossfield-recovery">Old Morrow case</Link></nav>
      </section>

      <footer className="morrow-review-footer"><span>MORROW CREATIVE REVIEW / 2026-09-14</span><b>Nothing on this page enables production.</b></footer>
    </main>
  );
}
