// archive-projection.ts — the pure, spoiler-free projection behind The Recovery Archive (the reading-room).
//
// THE GAP THIS CLOSES. The Record (/record/[slug]) shows a COARSE muster — a count of kept entries and a
// season, everything else struck. The Recovery Archive (/record/archive) is its DEEPER LAYER: the same
// artifact, one stratum down, showing the actual RECOVERED MATERIAL for cards the group has already
// un-earthed. It is still a PUBLIC, anon-served page, and still bound by the same absolute security model:
// anon can read ONLY the spoiler-free SECURITY DEFINER views. So this reads exactly one view (`v_archive`,
// SQL lane) that returns ONE ROW PER REVEALED CARD — a card is in the view IFF the group has revealed it,
// so the reveal-gating is done in SQL and this module never decides what is allowed to be seen. It only
// arranges what the view already handed over.
//
// This module is that arrangement, and ONLY that. It is PURE + DETERMINISTIC (no DB, no clock, no LLM —
// the card bodies are authored recovered material carried by the view, rendered verbatim; there is no
// language to author here), so the route can render it on the server with no client JS and a `.selftest`
// can pin every rule. The route does the I/O (reads the neutral view, degrades to []); this buckets,
// sorts, and resolves the citation web.
//
// THE LAWS THIS HONORS (mirror record-projection.ts):
//   - SPOILER-SAFE / REVEAL DISCIPLINE: the view already excludes unrevealed cards, so a revealed card
//     may reference an unrevealed one — and this NEVER surfaces that reference (no leak, no dead link). A
//     citation is resolved ONLY when its target is also in the revealed set.
//   - ICEBERG (the shape of what's-not-yet-found): the five canonical threads are ALWAYS returned, in
//     order, even when empty. An empty thread is a withheld block, never an omission — the missing shape
//     must stay legible.
//   - ANTI-JANK: total + side-effect-free; an absent/empty input degrades to the sealed baseline (five
//     empty threads), never an error, never a leaked default.

/**
 * A single revealed card, exactly as the `v_archive` SECURITY DEFINER view hands it over (one row per
 * revealed card). Every field is a coarse, already-reveal-gated public fact — the view is the authority
 * on what exists; this module never widens it. `references_card_key` are kebab card_key slugs that MAY
 * point at unrevealed cards (which this module then drops — see projectArchive).
 */
export interface ArchiveCard {
  /** stable kebab slug; the DOM id + anchor target for the citation web. never authored here. */
  card_key: string;
  /** which of the five canonical threads this card belongs under. */
  thread_key: string;
  /** the thread's cold lowercase label (carried from the view for render; canonical fallback below). */
  thread_label: string;
  /** the thread's accent color token (carried from the view; render chrome only). */
  thread_color: string;
  /** the thread's canonical sort ordinal (1..5). the projection uses the canonical order regardless. */
  thread_sort: number;
  /** the card's recovered title (authored material, rendered verbatim). */
  title: string;
  /** the card's recovered body (authored material, rendered verbatim). */
  body: string;
  /** the card's evidentiary standing — styles the card, never gates it. */
  card_kind: 'rumor' | 'explore' | 'verified' | 'contradicted';
  /** kebab card_key slugs this card points at; filtered to the revealed set before surfacing. */
  references_card_key: string[];
  /** intra-thread sort ordinal (asc); card_key breaks ties for determinism. */
  card_sort: number;
}

/** A resolved citation — a reference whose TARGET is also revealed, so it can be safely anchor-linked. */
export interface ArchiveReference {
  /** the referenced card's key — the `#<card_key>` anchor target. */
  card_key: string;
  /** the referenced card's title, for the link label (so the reader sees where it points). */
  title: string;
}

/** A card as the route renders it — the view's fields plus its filtered, resolved citation web. */
export interface ArchiveCardView {
  card_key: string;
  title: string;
  body: string;
  card_kind: ArchiveCard['card_kind'];
  /** only references whose target is ALSO revealed. an unrevealed reference is dropped (no leak). */
  references: ArchiveReference[];
}

/** One of the five canonical threads, always present (empty ⇒ an iceberg/withheld block, never omitted). */
export interface ArchiveThread {
  /** canonical thread key (who/place/happened/surface/human). */
  key: string;
  /** cold lowercase label (from the view when a card is present, else the canonical fallback). */
  label: string;
  /** accent color token (from the view when present, else the canonical fallback). */
  color: string;
  /** canonical sort ordinal (1..5). */
  sort: number;
  /** the revealed cards under this thread, sorted (card_sort asc, then card_key). may be empty. */
  cards: ArchiveCardView[];
  /** how many cards are revealed under this thread. */
  revealed: number;
}

/** The whole projection the route renders. All five threads, in order; a total; nothing sealed leaks. */
export interface ArchiveProjection {
  /** the five canonical threads, in canonical order, always all present (empty ones included). */
  threads: ArchiveThread[];
  /** the total count of revealed cards across every thread. */
  total: number;
  /** true when nothing at all is recovered yet — the route shows the sealed shell. */
  empty: boolean;
}

/**
 * The FIVE canonical threads — order + labels + colors are authoritative here (the iceberg spine). Every
 * projection returns all five in this order, so an empty thread renders as a withheld block and the shape
 * of what has not yet been found stays legible. The view carries per-row label/color too; when a thread
 * has at least one revealed card we prefer the view's values (single source of truth for populated
 * threads), and fall back to these canonical constants for empty threads (the view returns no row for
 * them). Colors are plain tokens the route maps to its own neutral chrome — never a bright accent.
 */
const CANONICAL_THREADS: ReadonlyArray<{ key: string; label: string; color: string; sort: number }> = [
  { key: 'who',       label: 'who they were',          color: 'amber', sort: 1 },
  { key: 'place',     label: 'what this place was',    color: 'green', sort: 2 },
  { key: 'happened',  label: 'what happened',          color: 'red',   sort: 3 },
  { key: 'surface',   label: 'what is on the surface', color: 'grey',  sort: 4 },
  { key: 'human',     label: 'were they human',        color: 'black', sort: 5 },
];

/**
 * projectArchive — the pure mapping. Given the revealed cards the view handed over, return the arranged
 * archive: five canonical threads in order (empty ones included), each thread's cards sorted, and each
 * card's citation web filtered to the revealed set. Total + deterministic: same input → same output. An
 * empty input degrades to the sealed baseline (five empty threads), never an error.
 */
export function projectArchive(cards: ArchiveCard[]): ArchiveProjection {
  // Defensive: an unexpected non-array (malformed/early view read) collapses to the sealed baseline.
  const revealed = Array.isArray(cards) ? cards : [];

  // The revealed set — the ONLY keys a citation may resolve to. A reference to a key not here is dropped
  // (no leak, no dead link). Also gives O(1) title lookup for resolved links.
  const revealedByKey = new Map<string, ArchiveCard>();
  for (const c of revealed) {
    if (c && typeof c.card_key === 'string' && c.card_key.length > 0) revealedByKey.set(c.card_key, c);
  }

  // Bucket revealed cards under their thread_key. Unknown thread_keys are dropped (can't invent a column).
  const byThread = new Map<string, ArchiveCard[]>();
  for (const c of revealed) {
    if (!c || !revealedByKey.has(c.card_key)) continue; // only well-formed, keyed rows
    const bucket = byThread.get(c.thread_key);
    if (bucket) bucket.push(c);
    else byThread.set(c.thread_key, [c]);
  }

  let total = 0;
  const threads: ArchiveThread[] = CANONICAL_THREADS.map((t) => {
    const bucket = (byThread.get(t.key) ?? []).slice();

    // Within a thread: card_sort asc, then card_key asc — fully deterministic (no unstable tie-order).
    bucket.sort((a, b) => {
      const sa = Number.isFinite(a.card_sort) ? a.card_sort : 0;
      const sb = Number.isFinite(b.card_sort) ? b.card_sort : 0;
      if (sa !== sb) return sa - sb;
      return a.card_key < b.card_key ? -1 : a.card_key > b.card_key ? 1 : 0;
    });

    const cardViews: ArchiveCardView[] = bucket.map((c) => {
      // The citation web, FAIRLY computed: keep only references whose target is ALSO revealed, and never
      // a self-reference. Resolve to { card_key, title } so the route can anchor-link and label them.
      const refs = Array.isArray(c.references_card_key) ? c.references_card_key : [];
      const seen = new Set<string>();
      const references: ArchiveReference[] = [];
      for (const key of refs) {
        if (typeof key !== 'string' || key === c.card_key || seen.has(key)) continue;
        const target = revealedByKey.get(key);
        if (!target) continue; // unrevealed reference → dropped (no leak)
        seen.add(key);
        references.push({ card_key: target.card_key, title: target.title });
      }
      return {
        card_key: c.card_key,
        title: c.title,
        body: c.body,
        card_kind: c.card_kind,
        references,
      };
    });

    total += cardViews.length;

    // Populated thread → prefer the view's label/color (single source of truth). Empty → canonical.
    const head = bucket[0];
    return {
      key: t.key,
      label: head && typeof head.thread_label === 'string' && head.thread_label.length > 0 ? head.thread_label : t.label,
      color: head && typeof head.thread_color === 'string' && head.thread_color.length > 0 ? head.thread_color : t.color,
      sort: t.sort,
      cards: cardViews,
      revealed: cardViews.length,
    };
  });

  return { threads, total, empty: total === 0 };
}
