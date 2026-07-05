"""check_experience_coherence.py - player-facing continuity guardrail.

This is the director check: it verifies that the authored experience map still has
working links across the pieces a player actually encounters.

It currently checks:
  - every Recovery Archive card body_voice_key resolves in voice.archive.ts
  - every Java-embedded townsfolk dialogue key resolves in voice.archive.ts
  - every thread-card anchor_site_id exists in plugin/src/main/resources/sites.yml
  - every revealed_by_solve in thread_cards.sql exists as a puzzle_key
  - every references_card_key entry points at another seeded card
  - every side_quests.thread_key belongs to the five public archive threads
  - the canonical experience manifest exists and includes the major lanes

Run:
  python tools/check_experience_coherence.py
"""

from __future__ import annotations

import pathlib
import re
import sys
from collections import Counter, defaultdict

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = pathlib.Path(__file__).resolve().parent.parent

THREAD_CARDS = ROOT / "discord" / "supabase" / "seeds" / "thread_cards.sql"
SIDE_QUESTS = ROOT / "discord" / "supabase" / "seeds" / "side_quests.sql"
PUZZLES = ROOT / "discord" / "supabase" / "seeds" / "puzzles_seed.sql"
VOICE_ARCHIVE = ROOT / "discord" / "src" / "voice.archive.ts"
SITES = ROOT / "plugin" / "src" / "main" / "resources" / "sites.yml"
MANIFEST = ROOT / "design" / "EXPERIENCE-MANIFEST.md"
TOWNSFOLK_JAVA = (
    ROOT
    / "plugin"
    / "src"
    / "main"
    / "java"
    / "com"
    / "observance"
    / "watcher"
    / "signal"
    / "listener"
    / "TownsfolkNpcListener.java"
)

THREADS = {"who", "place", "happened", "surface", "human"}


def read(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8")


def strip_sql_comments(sql: str) -> str:
    return "\n".join(line.split("--", 1)[0] for line in sql.splitlines())


def split_top_level_rows(values_sql: str) -> list[str]:
    rows: list[str] = []
    start: int | None = None
    depth = 0
    in_quote = False
    i = 0
    while i < len(values_sql):
        ch = values_sql[i]
        if ch == "'":
            if in_quote and i + 1 < len(values_sql) and values_sql[i + 1] == "'":
                i += 2
                continue
            in_quote = not in_quote
        elif not in_quote:
            if ch == "(":
                if depth == 0:
                    start = i
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0 and start is not None:
                    rows.append(values_sql[start : i + 1])
                    start = None
        i += 1
    return rows


def split_top_level_fields(row: str) -> list[str]:
    inner = row.strip()
    if inner.startswith("(") and inner.endswith(")"):
        inner = inner[1:-1]
    fields: list[str] = []
    start = 0
    depth = 0
    in_quote = False
    i = 0
    while i < len(inner):
        ch = inner[i]
        if ch == "'":
            if in_quote and i + 1 < len(inner) and inner[i + 1] == "'":
                i += 2
                continue
            in_quote = not in_quote
        elif not in_quote:
            if ch in "([":
                depth += 1
            elif ch in ")]":
                depth -= 1
            elif ch == "," and depth == 0:
                fields.append(inner[start:i].strip())
                start = i + 1
        i += 1
    fields.append(inner[start:].strip())
    return fields


def sql_string(field: str) -> str | None:
    field = field.strip()
    if field.lower() == "null":
        return None
    m = re.fullmatch(r"'((?:[^']|'')*)'", field, flags=re.DOTALL)
    if not m:
        return None
    return m.group(1).replace("''", "'")


def sql_array(field: str) -> list[str]:
    field = field.strip()
    if field == "{}":
        return []
    if field.lower().startswith("array[") and field.endswith("]"):
        body = field[field.index("[") + 1 : -1]
        return [m.group(1).replace("''", "'") for m in re.finditer(r"'((?:[^']|'')*)'", body)]
    if field.startswith("'{}'"):
        return []
    return []


def values_block(sql: str) -> str:
    m = re.search(r"\bvalues\b(?P<body>.*)on\s+conflict", sql, flags=re.IGNORECASE | re.DOTALL)
    if not m:
        raise ValueError("could not find VALUES ... ON CONFLICT block")
    return m.group("body")


def load_thread_cards() -> list[dict[str, object]]:
    sql = strip_sql_comments(read(THREAD_CARDS))
    cards = []
    for row in split_top_level_rows(values_block(sql)):
        fields = split_top_level_fields(row)
        if len(fields) != 10:
            raise ValueError(f"thread_cards row has {len(fields)} fields, expected 10: {row[:120]}")
        cards.append(
            {
                "card_key": sql_string(fields[0]),
                "thread_key": sql_string(fields[1]),
                "title": sql_string(fields[2]),
                "body_voice_key": sql_string(fields[3]),
                "anchor_site_id": sql_string(fields[4]),
                "card_kind": sql_string(fields[5]),
                "references": sql_array(fields[6]),
                "revealed_by_solve": sql_string(fields[7]),
            }
        )
    return cards


def load_side_quests() -> list[dict[str, str | None]]:
    sql = strip_sql_comments(read(SIDE_QUESTS))
    quests = []
    for row in split_top_level_rows(values_block(sql)):
        fields = split_top_level_fields(row)
        if len(fields) != 6:
            raise ValueError(f"side_quests row has {len(fields)} fields, expected 6: {row[:120]}")
        quests.append(
            {
                "quest_key": sql_string(fields[0]),
                "thread_key": sql_string(fields[1]),
                "entry_puzzle_key": sql_string(fields[2]),
            }
        )
    return quests


def load_puzzle_keys() -> set[str]:
    sql = strip_sql_comments(read(PUZZLES))
    keys = set()
    for row in split_top_level_rows(values_block(sql)):
        fields = split_top_level_fields(row)
        if fields:
            key = sql_string(fields[0])
            if key:
                keys.add(key)
    return keys


def load_site_ids() -> set[str]:
    ids: set[str] = set()
    for line in read(SITES).splitlines():
        if line.startswith("  ") and not line.startswith("    "):
            m = re.match(r"^\s{2}([A-Za-z0-9_-]+):\s*(?:#.*)?$", line)
            if m:
                key = m.group(1)
                if key not in {"radius", "protect", "vertical-radius"}:
                    ids.add(key)
    return ids


def load_voice_keys() -> set[str]:
    text = read(VOICE_ARCHIVE)
    keys = set(re.findall(r"^\s*([A-Za-z0-9_.-]+):\s*$", text, flags=re.MULTILINE))
    keys.update(re.findall(r"^\s*'([^']+)':\s*$", text, flags=re.MULTILINE))
    return keys


def load_embedded_townsfolk_keys() -> set[str]:
    text = read(TOWNSFOLK_JAVA)
    return set(re.findall(r'new\s+String\[\]\s*\{\s*"([^"]+)"\s*,', text))


def check_manifest(errors: list[str]) -> None:
    if not MANIFEST.exists():
        errors.append("missing design/EXPERIENCE-MANIFEST.md")
        return
    text = read(MANIFEST).lower()
    required = [
        "surface people before stones",
        "the world watches",
        "rosetta literacy",
        "the record is kept elsewhere",
        "side destinations",
        "recovery archive threads",
        "wren",
        "the keeper field",
        "planned media",
        "accepting and release",
    ]
    for phrase in required:
        if phrase not in text:
            errors.append(f"experience manifest missing lane: {phrase}")


def main() -> int:
    errors: list[str] = []
    warnings: list[str] = []

    try:
        cards = load_thread_cards()
        quests = load_side_quests()
        puzzle_keys = load_puzzle_keys()
        site_ids = load_site_ids()
        voice_keys = load_voice_keys()
        townsfolk_keys = load_embedded_townsfolk_keys()
    except Exception as exc:
        print(f"experience coherence: FAILED to parse inputs: {exc}")
        return 1

    card_key_list = [str(c["card_key"]) for c in cards if c["card_key"]]
    card_keys = set(card_key_list)
    duplicate_cards = {k: n for k, n in Counter(card_key_list).items() if n > 1}
    if duplicate_cards:
        errors.append(f"duplicate card keys: {sorted(duplicate_cards)}")

    for c in cards:
        key = c["card_key"]
        thread = c["thread_key"]
        voice = c["body_voice_key"]
        site = c["anchor_site_id"]
        revealed = c["revealed_by_solve"]

        if thread not in THREADS:
            errors.append(f"{key}: invalid thread_key {thread!r}")
        if voice not in voice_keys:
            errors.append(f"{key}: body_voice_key {voice!r} missing from voice.archive.ts")
        if site not in site_ids:
            errors.append(f"{key}: anchor_site_id {site!r} missing from sites.yml")
        if revealed is not None and revealed not in puzzle_keys:
            errors.append(f"{key}: revealed_by_solve {revealed!r} missing from puzzles_seed.sql")
        for ref in c["references"]:
            if ref not in card_keys:
                errors.append(f"{key}: references missing card_key {ref!r}")

    for q in quests:
        qkey = q["quest_key"]
        thread = q["thread_key"]
        entry = q["entry_puzzle_key"]
        if thread not in THREADS:
            errors.append(f"{qkey}: invalid side_quest thread_key {thread!r}")
        if entry is not None and entry not in puzzle_keys:
            errors.append(f"{qkey}: entry_puzzle_key {entry!r} missing from puzzles_seed.sql")

    for key in sorted(townsfolk_keys):
        if key not in voice_keys:
            errors.append(f"Java townsfolk line key {key!r} missing from voice.archive.ts npcLines")

    by_thread: dict[str, int] = defaultdict(int)
    for c in cards:
        by_thread[str(c["thread_key"])] += 1
    for thread in sorted(THREADS):
        if by_thread[thread] < 5:
            warnings.append(f"thread {thread!r} has only {by_thread[thread]} cards")

    check_manifest(errors)

    print("=" * 78)
    print("EXPERIENCE COHERENCE CHECK")
    print("=" * 78)
    print(f"cards: {len(cards)}")
    print(f"side quests: {len(quests)}")
    print(f"puzzle keys: {len(puzzle_keys)}")
    print(f"site ids: {len(site_ids)}")
    print(f"voice keys: {len(voice_keys)}")
    print(f"embedded townsfolk keys: {len(townsfolk_keys)}")

    if warnings:
        print("\nWARNINGS:")
        for w in warnings:
            print(f"  - {w}")

    if errors:
        print("\nERRORS:")
        for e in errors:
            print(f"  - {e}")
        print("\nRESULT: FAIL")
        return 1

    print("\nRESULT: CLEAN - archive, side quests, sites, puzzle gates, and manifest lanes resolve.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
