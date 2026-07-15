# Deep Hold V5 book integration

Status: current
Exact canonical pages: `arc/v5/minecraft-books.json`
Exact holder/mount/facing/unlock contract: `design/ARG-V5-BOOK-PLACEMENT.csv`

The fixed lectern/artifact registry contains 44 books with stable ID, node, title, author, room,
unlock flag, and exact page boundaries. A second immutable authority,
`arc/v5/evidence-item-text.json`, contains 7 movable written evidence items used by WR01, BI01,
and CW07. Those items have exact titles, authors, pages, and evidence IDs; generic generated copy is a
launch blocker.

Non-book evidence uses the separate 108-item authority in
`arc/v5/evidence-item-appearance.json`. Its visible titles, lore, station labels, and filing labels
are also canonical and hash/audit controlled; a raw PDC ID as an item name is a launch blocker.

Release-blocking checks:

- no empty book/page;
- title ≤32 characters and authored page ≤240 characters;
- exact title/author/page hash after live placement;
- lectern present, filled, faced toward declared standing cell, and removal-protected;
- affidavit/coda variants gated by durable flags;
- each of the 44 IDs has exactly one logical holder and mount; the two Coda receipts share one
  mutually exclusive branch lectern;
- all six sealed affidavits are the exact written-book artifacts consumed by G2/RP02 and recovered
  through `design/ARG-V5-ARTIFACT-MANIFEST.csv`;
- no paragraph flattening or silent truncation;
- no fallback V4 text when a canonical resource is missing.

`python tools/check_v5_content.py` validates sources. The plugin must package and parity-test a byte-identical generated copy.
