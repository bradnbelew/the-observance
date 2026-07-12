#!/usr/bin/env python3
"""Machine-check the exact D05 shelf payload and editorial page-lock contracts."""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
D05 = ROOT / "design/deep-hold-d05-shelf.json"
LOCKS = ROOT / "design/DEEP-HOLD-BOOK-MANUSCRIPTS.md"


def main() -> int:
    data = json.loads(D05.read_text(encoding="utf-8"))
    failures: list[str] = []
    extracted: list[str] = []
    books = data.get("books", [])
    if len(books) != 6:
        failures.append(f"D05 must contain 6 books, found {len(books)}")
    for index, book in enumerate(books, 1):
        pages = book.get("pages", [])
        ref = book.get("reference", {})
        page_no = int(ref.get("page", 0))
        line_no = int(ref.get("line", 0))
        word_no = int(ref.get("word", 0))
        try:
            page = pages[page_no - 1]
            line = page.splitlines()[line_no - 1]
            word = line.split()[word_no - 1].strip(".,;:!?()[]{}\"")
            extracted.append(word)
        except (IndexError, TypeError):
            failures.append(f"D05 book {index} reference is outside its authored pages/lines/words")
        if not book.get("title") or not book.get("author"):
            failures.append(f"D05 book {index} has blank metadata")
        for page_index, page in enumerate(pages, 1):
            if len(page) > 798:
                failures.append(f"D05 book {index} page {page_index} exceeds BookMeta page character limit")
            if len(page.splitlines()) > 14:
                failures.append(f"D05 book {index} page {page_index} exceeds 14 authored lines")

    expansions = data.get("compound_expansions", {})
    expanded = " ".join(expansions.get(word, word) for word in extracted)
    if expanded != data.get("expected_extraction"):
        failures.append(f"D05 extracts {expanded!r}, expected {data.get('expected_extraction')!r}")

    manuscript = LOCKS.read_text(encoding="utf-8")
    for required in (
        "Marked page: 1", "Marked page: 2", "Marked page: 4", "Marked page: 6",
        "Marked page: 3", "Marked page: 5", "Marked page: 7", "Marked page: 11",
    ):
        if required not in manuscript:
            failures.append(f"page-lock manuscript is missing {required}")
    for forbidden in (
        "Mara shelf ", "Sella loose page ", "turn me for page-lock testing",
        "the line continues elsewhere, but not here", "this is the page her hand left open",
    ):
        if forbidden in manuscript:
            failures.append(f"page-lock manuscript contains forbidden scaffold phrase {forbidden!r}")

    mara_sections = re.findall(r"## Mara lock volume \d([\s\S]*?)(?=\n## |\Z)", manuscript)
    sella_sections = re.findall(r"## Sella lock gathering \d([\s\S]*?)(?=\n## |\Z)", manuscript)
    if len(mara_sections) != 5 or any(len(re.findall(r"^\| \d+ \|", s, re.MULTILINE)) != 10 for s in mara_sections):
        failures.append("Mara manuscript must contain 5 volumes of exactly 10 tabled pages")
    if len(sella_sections) != 5 or any(len(re.findall(r"^\| \d+ \|", s, re.MULTILINE)) != 12 for s in sella_sections):
        failures.append("Sella manuscript must contain 5 gatherings of exactly 12 tabled pages")

    print(f"Deep Hold book manuscripts: {len(books)} D05 books; extraction: {expanded}")
    if failures:
        for failure in failures:
            print("  FAIL " + failure)
        return 1
    print("  PASS: D05 coordinates/extraction and both five-book page-count contracts")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
