// archive-projection.selftest.ts — pins every rule in the Recovery Archive projection (the reading-room).
//
// Cheap, dependency-free assertions (the repo's `.selftest` convention). The whole point of the archive is
// that it arranges ONLY what the reveal-gated view handed over and NEVER surfaces a reference to a card the
// group has not un-earthed. These tests are the camera-guard for that. Run with
// `npx tsx src/lib/archive-projection.selftest.ts` (or wire into the dashboard's test runner).

import { projectArchive, type ArchiveCard } from './archive-projection';

let failures = 0;
function check(name: string, cond: boolean) {
  if (!cond) {
    failures++;
    console.error(`  ✗ ${name}`);
  } else {
    console.log(`  ✓ ${name}`);
  }
}

console.log('archive-projection.selftest');

const THREAD_ORDER = ['who', 'place', 'happened', 'surface', 'human'];

/** A small factory so fixtures stay readable — only the fields a case cares about need overriding. */
function card(over: Partial<ArchiveCard> & Pick<ArchiveCard, 'card_key' | 'thread_key'>): ArchiveCard {
  return {
    thread_label: '',
    thread_color: '',
    thread_sort: 0,
    title: over.card_key,
    body: `body of ${over.card_key}`,
    card_kind: 'explore',
    references_card_key: [],
    card_sort: 0,
    ...over,
  };
}

// 1. The sealed baseline: empty input → five empty threads, in canonical order, no throw.
{
  const p = projectArchive([]);
  check('empty input → five threads', p.threads.length === 5);
  check('empty input → canonical thread order', p.threads.map((t) => t.key).join(',') === THREAD_ORDER.join(','));
  check('empty input → every thread empty', p.threads.every((t) => t.revealed === 0 && t.cards.length === 0));
  check('empty input → total 0', p.total === 0);
  check('empty input → flagged empty', p.empty === true);
  // canonical labels/colors survive on empty threads (the iceberg still reads).
  check('empty threads carry canonical labels', p.threads[0].label === 'who they were' && p.threads[4].label === 'were they human');
}

// 2. All five threads are ALWAYS present, even when only some are populated (iceberg discipline).
{
  const p = projectArchive([card({ card_key: 'a', thread_key: 'happened' })]);
  check('one populated thread → still five threads', p.threads.length === 5);
  check('the empty threads are withheld, not omitted', p.threads.filter((t) => t.revealed === 0).length === 4);
  check('the populated thread holds its one card', p.threads.find((t) => t.key === 'happened')!.revealed === 1);
}

// 3. Bucketing + sort: within a thread, card_sort asc, then card_key.
{
  const p = projectArchive([
    card({ card_key: 'b', thread_key: 'who', card_sort: 2 }),
    card({ card_key: 'a', thread_key: 'who', card_sort: 2 }), // tie on sort → card_key breaks it (a<b)
    card({ card_key: 'c', thread_key: 'who', card_sort: 1 }),
  ]);
  const who = p.threads.find((t) => t.key === 'who')!;
  check('sorted by card_sort asc then card_key', who.cards.map((c) => c.card_key).join(',') === 'c,a,b');
  check('total counts all revealed', p.total === 3);
}

// 4. THE CITATION WEB is fair: a reference to a REVEALED card resolves; a reference to an UNREVEALED
//    card is dropped (no leak, no dead link). This is the core spoiler-safety guarantee.
{
  const p = projectArchive([
    // 'a' points at 'b' (revealed) and 'ghost' (NOT in the set) and itself (must not self-cite).
    card({ card_key: 'a', thread_key: 'who', references_card_key: ['b', 'ghost', 'a'] }),
    card({ card_key: 'b', thread_key: 'place', title: 'the second' }),
  ]);
  const a = p.threads.find((t) => t.key === 'who')!.cards.find((c) => c.card_key === 'a')!;
  check('reference to a revealed card resolves', a.references.some((r) => r.card_key === 'b'));
  check('resolved reference carries the target title', a.references.find((r) => r.card_key === 'b')!.title === 'the second');
  check('reference to an UNREVEALED card is dropped (no leak)', !a.references.some((r) => r.card_key === 'ghost'));
  check('self-reference is dropped', !a.references.some((r) => r.card_key === 'a'));
  check('exactly one reference survives filtering', a.references.length === 1);
}

// 5. Duplicate references collapse (deterministic, no double links).
{
  const p = projectArchive([
    card({ card_key: 'a', thread_key: 'who', references_card_key: ['b', 'b', 'b'] }),
    card({ card_key: 'b', thread_key: 'who' }),
  ]);
  const a = p.threads.find((t) => t.key === 'who')!.cards.find((c) => c.card_key === 'a')!;
  check('duplicate references collapse to one', a.references.length === 1);
}

// 6. Populated threads prefer the view's label/color; unknown thread_keys are dropped (no invented column).
{
  const p = projectArchive([
    card({ card_key: 'a', thread_key: 'who', thread_label: 'who they were (kept)', thread_color: 'amber' }),
    card({ card_key: 'x', thread_key: 'not-a-thread' }), // unknown bucket → dropped
  ]);
  const who = p.threads.find((t) => t.key === 'who')!;
  check("populated thread uses the view's label", who.label === 'who they were (kept)');
  check('unknown thread_key is dropped from the total', p.total === 1);
  check('still exactly five threads', p.threads.length === 5);
}

// 7. Determinism: same input → same output (the backstop IS the determinism).
{
  const input = [
    card({ card_key: 'b', thread_key: 'human', card_sort: 1, references_card_key: ['a'] }),
    card({ card_key: 'a', thread_key: 'surface', card_sort: 1 }),
  ];
  check('deterministic', JSON.stringify(projectArchive(input)) === JSON.stringify(projectArchive(input)));
}

// 8. Body is carried verbatim (rendered as recovered material; the projection never rewrites it).
{
  const p = projectArchive([card({ card_key: 'a', thread_key: 'who', body: 'the light was read too long.' })]);
  const a = p.threads.find((t) => t.key === 'who')!.cards[0];
  check('body is carried verbatim', a.body === 'the light was read too long.');
}

if (failures > 0) {
  console.error(`\narchive-projection.selftest: ${failures} FAILED`);
  process.exit(1);
}
console.log('\narchive-projection.selftest: all passed');
