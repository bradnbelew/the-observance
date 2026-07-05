"""check_namespace_collisions.py — namespace/ID collision guardrail.

WHY: across authoring waves, parallel sessions have repeatedly self-minted FACT
numbers, INV (invariant) numbers, and puzzle_key values that collide with
something already in the corpus. Every time, this was caught late by a manual
audit and "fixed" by hand-renumbering — and recurred in the next batch, because
nothing automated ever re-checked it. This script is that automated check.

WHAT IT SCANS:
  1. arc/**/*.md and design/**/*.md for FACT-definition headers, e.g.:
       **FACT 12 — some description**
       **FACT 10b — some description**
     (references like a bare "FACT 11" or "FACT10" inline in prose are NOT
     definitions and are ignored — only the bolded "FACT <n><letter?> —/-- text"
     heading form, which is how canon-spine.md actually mints a fact, counts as
     a definition site.) `arc/lore/canon-spine.md` is the stated namespace owner
     ("no other file may self-mint these integers" — canon-spine §3b/§7). A
     collision is either (a) the SAME number defined twice inside canon-spine.md
     with disagreeing description text (a straight self-mint clash between
     authoring passes), or (b) a number that only ever appears in the bolded
     definition form OUTSIDE canon-spine.md — i.e. some other file minted a
     number the owner file never defines. Plain restatements/discussions of an
     already-owner-defined number elsewhere (tables, blockquote elaborations,
     etc.) are NOT collisions.
  2. The same for INV invariants: **INV-12.** / **INV-18 (INV-SLOT).** etc.
  3. discord/supabase/seeds/*.sql (and any other *.sql under discord/supabase)
     for `puzzle_key` values in `insert into public.puzzles (puzzle_key, ...)
     values ( 'key', ...` row tuples. Any puzzle_key string that is the first
     column of more than one row (across all seed files combined) is a
     collision — the DB's own `(puzzle_key)` uniqueness means a real duplicate
     insert is a silent last-write-wins bug, not just a lint nit.

USAGE:
    python tools/check_namespace_collisions.py

EXIT CODE:
    0  — no collisions found (prints a clean summary count)
    1  — one or more collisions found (prints file:line pairs for each)

This script only REPORTS. It does not rename or edit anything — fixing a real
collision is a judgment call (which authoring session's number wins, whether
downstream references need updating) left to a human or a follow-up task.
"""

from __future__ import annotations

import pathlib
import re
import sys
from collections import defaultdict

# Windows consoles often default to a non-UTF-8 codepage; description snippets
# pulled from the corpus can contain em-dashes and other non-ASCII characters,
# so force UTF-8 stdout to avoid a crash on print().
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = pathlib.Path(__file__).resolve().parent.parent

# ---------------------------------------------------------------------------
# FACT / INV definition-site regexes
# ---------------------------------------------------------------------------
# Definition form seen in canon-spine.md / WEB-MASTER.md, e.g.:
#   **FACT 12 — "The kept ones did not depart. They were kept."**
#   **FACT 10b — The land refused a keeper who broke *nothing*.**
#   **FACT 17 — the third filing axis.** FACT 1 files by **name**; ...
# Capture: number+optional letter suffix, and a chunk of the description that
# follows the dash, so we can compare descriptions across duplicate sites.
FACT_DEF_RE = re.compile(
    r"\*\*FACT[ -](?P<num>\d+[a-z]?)\s*[—–-]{1,2}\s*(?P<desc>.+?)\*\*",
    re.IGNORECASE,
)

# Definition form seen in canon-spine.md, e.g.:
#   - **INV-11.** The **ending selector reads measured group tallies ...
#   - **INV-18 (INV-SLOT).** **The apparition slot is single-arbiter**: ...
# Only counts as a definition when it is the START of a bullet ("- **INV-N")
# followed by a period/close — this is how canon-spine.md actually mints an
# invariant. Bare inline mentions elsewhere ("INV-16", "per INV-14") are just
# references and are intentionally NOT matched here.
INV_DEF_RE = re.compile(
    r"^\s*-\s*\*\*INV-(?P<num>\d+[a-zA-Z]?)\s*(?:\([^)]*\))?\s*\.\*\*\s*(?P<desc>.+)$"
)

MD_DIRS = ["arc", "design"]


def iter_markdown_files():
    for d in MD_DIRS:
        base = ROOT / d
        if base.exists():
            yield from sorted(base.rglob("*.md"))


def normalize_desc(desc: str) -> str:
    """Loose-normalize a description snippet for same/different comparison."""
    desc = re.sub(r"[*_`\"']", "", desc)
    desc = re.sub(r"\s+", " ", desc).strip().lower()
    return desc[:120]


def scan_fact_and_inv_definitions():
    """Returns (fact_sites, inv_sites): id -> list of (file, line, desc)."""
    fact_sites: dict[str, list[tuple[str, int, str]]] = defaultdict(list)
    inv_sites: dict[str, list[tuple[str, int, str]]] = defaultdict(list)

    for path in iter_markdown_files():
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        rel = path.relative_to(ROOT).as_posix()
        for lineno, line in enumerate(text.splitlines(), start=1):
            for m in FACT_DEF_RE.finditer(line):
                num = m.group("num").upper()
                desc = normalize_desc(m.group("desc"))
                fact_sites[num].append((rel, lineno, desc))
            m2 = INV_DEF_RE.match(line)
            if m2:
                num = m2.group("num").upper()
                desc = normalize_desc(m2.group("desc"))
                inv_sites[num].append((rel, lineno, desc))

    return fact_sites, inv_sites


def find_collisions(sites: dict[str, list[tuple[str, int, str]]], owner_file: str):
    """A collision is one of two things:

    1. WITHIN the namespace-owner file (canon-spine.md, per its own §3b/§7
       statement that it is the sole minter of FACT/INV numbers): the same
       number defined twice with disagreeing description text. That is a
       straight self-mint clash between two authoring passes.

    2. A number that appears in the bolded FACT/INV definition form in a file
       OTHER than the owner file, but is never defined in the owner file at
       all. Per canon ("no other file may self-mint these integers"), any
       other file is allowed to *discuss* a number the owner already defines
       (tables, blockquote elaborations, etc. — that's normal cross-referencing,
       not a collision) but is NOT allowed to be the sole/first definer of a
       new number. That case is reported as an "orphan mint outside the owner
       file" collision, since it is exactly the self-minted-elsewhere pattern
       the historical audits kept catching by hand.
    """
    collisions = []
    for num, occurrences in sites.items():
        owner_occurrences = [o for o in occurrences if o[0] == owner_file]
        other_occurrences = [o for o in occurrences if o[0] != owner_file]

        # Case 1: disagreeing definitions within the owner file itself.
        if len(owner_occurrences) > 1:
            descs = {o[2] for o in owner_occurrences}
            if len(descs) > 1:
                collisions.append((num, owner_occurrences))
                continue

        # Case 2: minted outside the owner file with no owner-file definition
        # backing it up at all.
        if other_occurrences and not owner_occurrences:
            collisions.append((num, other_occurrences))

    return collisions


# ---------------------------------------------------------------------------
# puzzle_key collision scan
# ---------------------------------------------------------------------------
SQL_DIRS = [ROOT / "discord" / "supabase" / "seeds", ROOT / "discord" / "supabase" / "migrations"]

# Matches the row-tuple start used by puzzles_seed.sql / progression_seed.sql:
#   ( 'm1-record-opens',
ROW_KEY_RE = re.compile(r"^\(\s*'(?P<key>[a-zA-Z0-9_-]+)'\s*,")

# Guard: only scan files that actually contain an
# "insert into public.puzzles (puzzle_key, ..." statement somewhere (the
# statement itself commonly wraps across lines, so this is checked against
# the whole file text rather than a single line), since the same row shape
# ( 'key', ... ) could in principle appear in an unrelated insert.
INSERT_PUZZLES_RE = re.compile(r"insert\s+into\s+public\.puzzles\s*\(\s*puzzle_key", re.IGNORECASE)


def iter_sql_files():
    for base in SQL_DIRS:
        if base.exists():
            yield from sorted(base.rglob("*.sql"))


def scan_puzzle_keys():
    """Returns dict: puzzle_key -> list of (file, line)."""
    key_sites: dict[str, list[tuple[str, int]]] = defaultdict(list)

    for path in iter_sql_files():
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if not INSERT_PUZZLES_RE.search(text):
            continue
        rel = path.relative_to(ROOT).as_posix()
        for lineno, line in enumerate(text.splitlines(), start=1):
            m = ROW_KEY_RE.match(line.strip())
            if m:
                key_sites[m.group("key")].append((rel, lineno))

    return key_sites


def find_puzzle_key_collisions(key_sites: dict[str, list[tuple[str, int]]]):
    return {k: v for k, v in key_sites.items() if len(v) > 1}


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------

def format_sites(occurrences):
    return "; ".join(f"{f}:{ln}" for f, ln, *_ in occurrences)


NAMESPACE_OWNER_FILE = "arc/lore/canon-spine.md"


def main() -> int:
    fact_sites, inv_sites = scan_fact_and_inv_definitions()
    fact_collisions = find_collisions(fact_sites, NAMESPACE_OWNER_FILE)
    inv_collisions = find_collisions(inv_sites, NAMESPACE_OWNER_FILE)

    key_sites = scan_puzzle_keys()
    key_collisions = find_puzzle_key_collisions(key_sites)

    had_error = bool(fact_collisions or inv_collisions or key_collisions)

    print("=" * 78)
    print("NAMESPACE/ID COLLISION CHECK")
    print("=" * 78)

    if fact_collisions:
        print(f"\nFACT NUMBER COLLISIONS ({len(fact_collisions)}):")
        for num, occurrences in sorted(fact_collisions):
            print(f"  FACT {num} defined in {len(occurrences)} places:")
            for f, ln, desc in occurrences:
                print(f"    {f}:{ln}  \"{desc}\"")
    else:
        print(f"\nFACT numbers: OK ({len(fact_sites)} distinct FACT ids, no collisions)")

    if inv_collisions:
        print(f"\nINV NUMBER COLLISIONS ({len(inv_collisions)}):")
        for num, occurrences in sorted(inv_collisions):
            print(f"  INV-{num} defined in {len(occurrences)} places:")
            for f, ln, desc in occurrences:
                print(f"    {f}:{ln}  \"{desc}\"")
    else:
        print(f"INV numbers: OK ({len(inv_sites)} distinct INV ids, no collisions)")

    if key_collisions:
        print(f"\nPUZZLE_KEY COLLISIONS ({len(key_collisions)}):")
        for key, occurrences in sorted(key_collisions.items()):
            print(f"  '{key}' inserted in {len(occurrences)} places:")
            for f, ln in occurrences:
                print(f"    {f}:{ln}")
    else:
        print(f"puzzle_key values: OK ({len(key_sites)} distinct keys, no collisions)")

    print()
    print("=" * 78)
    if had_error:
        total = len(fact_collisions) + len(inv_collisions) + len(key_collisions)
        print(f"RESULT: FAIL -- {total} collision group(s) found. See file:line pairs above.")
        print("(This script only reports. Renumbering/fixing is a separate task.)")
    else:
        print("RESULT: CLEAN -- no FACT/INV/puzzle_key collisions found.")
    print("=" * 78)

    return 1 if had_error else 0


if __name__ == "__main__":
    sys.exit(main())
