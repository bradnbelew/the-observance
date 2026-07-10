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
METAPUZZLE = ROOT / "discord" / "supabase" / "seeds" / "metapuzzle_seed.sql"
THEORY = ROOT / "discord" / "src" / "showrunner" / "theory.ts"
UNLIT_PANEL = ROOT / "dashboard" / "src" / "components" / "author" / "UnlitProgress.tsx"
RUNBOOK = ROOT / "design" / "RUNBOOK.md"
COMMAND_JAVA = (
    ROOT
    / "plugin"
    / "src"
    / "main"
    / "java"
    / "com"
    / "observance"
    / "watcher"
    / "command"
    / "ObservanceCommand.java"
)
DIALOGUE_CONTRACTS = ROOT / "tools" / "check_dialogue_contracts.ps1"
UNLIT_READINESS = ROOT / "tools" / "check_unlit_readiness.ps1"
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
SITE_DISCOVERY_JAVA = (
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
    / "SiteDiscoveryListener.java"
)

THREADS = {"who", "place", "happened", "surface", "human"}
KEEPER_THEORY_FLAGS = [
    "vaun_theory",
    "mara_theory",
    "sella_theory",
    "orin_theory",
    "brann_theory",
    "iss_theory",
]
UNLIT_FLAGS = [
    "unlit_seen_lamp",
    "unlit_seen_cairn",
    "unlit_seen_coop",
    "unlit_seen_well",
    "unlit_seen_watch",
    "unlit_seen_warm",
    "unlit_seen_threshold",
    "unlit_seen_base",
]
SIDE_PROOF_FLAGS = [
    "site_seen_school_stand",
    "site_seen_markers_row",
    "site_seen_cistern_7",
    "site_seen_watch_floor",
    "site_seen_set_apart_shelf",
    "site_seen_undercroft_seal",
    "site_seen_forgotten_mouth",
    "site_seen_deep_market",
    "site_seen_ration_table",
    "site_seen_third_bay_breach",
    "site_seen_warm_town_collapse",
    "site_seen_deep_bird_coops",
]
NPC_PROOF_FLAGS = [
    "npc_wenna_crust_done",
    "npc_coll_lamp_done",
]
SIDE_PROOF_CARD_GATES = {
    "who-deep-market": "flag:site_seen_deep_market",
    "place-cistern-seven": "flag:site_seen_cistern_7",
    "place-deep-line": "flag:site_seen_third_bay_breach",
    "place-way-up": "flag:site_seen_forgotten_mouth",
    "place-warm-town-collapse": "flag:site_seen_warm_town_collapse",
    "happened-undercroft-seal": "flag:site_seen_undercroft_seal",
    "happened-markers-row": "flag:site_seen_markers_row",
    "surface-watch-floor": "flag:site_seen_watch_floor",
    "surface-set-apart": "flag:site_seen_set_apart_shelf",
    "surface-bird-coops": "flag:site_seen_deep_bird_coops",
    "human-school-stand": "flag:site_seen_school_stand",
    "human-ration-redivided": "flag:site_seen_ration_table",
}


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
                "alt_text_condition": sql_string(fields[8]),
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


def require_contains(errors: list[str], label: str, text: str, needle: str) -> None:
    if needle not in text:
        errors.append(f"{label} missing: {needle}")


def require_absent(errors: list[str], label: str, text: str, needle: str) -> None:
    if needle in text:
        errors.append(f"{label} still contains stale phrase: {needle}")


def voice_entry(text: str, key: str) -> str:
    m = re.search(rf"^\s*{re.escape(key)}:\s*(?P<body>.*?)(?=^\s*[A-Za-z0-9_.-]+:\s*|^\s*'[^']+':\s*|\n\}};)", text, flags=re.MULTILINE | re.DOTALL)
    return m.group("body") if m else ""


def rite_tokens_gate_block(text: str) -> str | None:
    m = re.search(
        r"update\s+public\.puzzles\s+set\s+requires_flags\s*=\s*jsonb_build_object\((?P<body>.*?)\)\s*where\s+puzzle_key\s*=\s*'rite-tokens'",
        text,
        flags=re.IGNORECASE | re.DOTALL,
    )
    return m.group("body") if m else None


def check_director_truth(cards: list[dict[str, object]], errors: list[str]) -> None:
    """Guards the current story direction, not just mechanical file references."""
    metapuzzle = read(METAPUZZLE)
    hints = read(ROOT / "discord" / "supabase" / "seeds" / "hints_seed.sql")
    theory = read(THEORY)
    unlit_panel = read(UNLIT_PANEL)
    runbook = read(RUNBOOK)
    command = read(COMMAND_JAVA)
    dialogue_contracts = read(DIALOGUE_CONTRACTS)
    unlit_readiness = read(UNLIT_READINESS)
    archive = read(VOICE_ARCHIVE)
    site_discovery = read(SITE_DISCOVERY_JAVA)
    townsfolk_java = read(TOWNSFOLK_JAVA)

    by_key = {str(c["card_key"]): c for c in cards if c["card_key"]}
    expected_surface_titles = {
        "surface-aro-lie": "the warm town story",
        "surface-wenna-folk": "seven things",
        "surface-pell-truth": "be watched",
    }
    for key, expected in expected_surface_titles.items():
        actual = by_key.get(key, {}).get("title")
        if actual != expected:
            errors.append(f"{key}: expected grounded surface title {expected!r}, found {actual!r}")

    for key, needles in {
        "cardSurfaceAroLie": ["market ledger", "ration table", "prove it"],
        "cardSurfaceWennaFolk": ["school-stand", "watch-floor", "six stones and one grey one"],
        "cardSurfacePellTruth": ["watch-floor", "school-stand", "be watched"],
    }.items():
        body = voice_entry(archive, key)
        if not body:
            errors.append(f"voice.archive.ts missing surface testimony body {key}")
            continue
        for needle in needles:
            require_contains(errors, f"voice.archive.ts {key}", body, needle)

    rite_block = rite_tokens_gate_block(metapuzzle)
    if rite_block is None:
        errors.append("metapuzzle_seed.sql missing rite-tokens requires_flags gate")
    else:
        for flag in ["accepting_onramp_open", *KEEPER_THEORY_FLAGS, *UNLIT_FLAGS, *SIDE_PROOF_FLAGS, *NPC_PROOF_FLAGS]:
            require_contains(errors, "rite-tokens web gate", rite_block, flag)
        for phrase in ["six keeper theories", "all eight unlit house recoveries", "named side proofs", "two surface kindnesses"]:
            require_contains(errors, "rite-tokens rescue text", hints, phrase)

    for flag in SIDE_PROOF_FLAGS:
        require_contains(errors, "site discovery flag producer", site_discovery, flag)
    for flag in NPC_PROOF_FLAGS:
        require_contains(errors, "NPC proof flag producer", townsfolk_java, flag)

    for card_key, gate in SIDE_PROOF_CARD_GATES.items():
        card = by_key.get(card_key)
        if not card:
            errors.append(f"thread_cards missing side-proof card {card_key}")
            continue
        if card.get("alt_text_condition") != gate:
            errors.append(f"{card_key}: expected discovery gate {gate!r}, found {card.get('alt_text_condition')!r}")
        if card.get("revealed_by_solve") is not None:
            errors.append(f"{card_key}: side-proof card must reveal by discovery flag, not by solve {card.get('revealed_by_solve')!r}")

    thresholds = [int(n) for n in re.findall(r"threshold:\s*(\d+)", theory)]
    if len(thresholds) < len(KEEPER_THEORY_FLAGS):
        errors.append("theory.ts missing keeper theory thresholds")
    elif any(n < 3 for n in thresholds[: len(KEEPER_THEORY_FLAGS)]):
        errors.append(f"keeper theory thresholds must stay veteran-facing (>=3), found {thresholds[:len(KEEPER_THEORY_FLAGS)]}")

    require_contains(errors, "dashboard Unlit progress", unlit_panel, "required houses")
    require_contains(errors, "runbook Unlit handoff", runbook, "all eight")
    require_contains(errors, "runbook Unlit handoff", runbook, "lamp, cairn, coop, well, watch, warm, threshold")
    require_absent(errors, "runbook Unlit handoff", runbook, "required ending evidence houses are lamp, well, watch, and base")
    for flag in UNLIT_FLAGS:
        if not re.search(rf'key:\s*"{re.escape(flag)}".*?required:\s*true', unlit_panel, flags=re.DOTALL):
            errors.append(f"dashboard Unlit progress does not mark {flag} required")
        require_contains(errors, "Unlit readiness guard", unlit_readiness, flag)

    current_visual_proofs = [
        "far-water copybook",
        "dark-hours proof",
        "lectern-shelf ledger",
        "written R14 ration form",
        "third-bay incident note",
        "WARDEN-3 closure record",
    ]
    for needle in current_visual_proofs:
        require_contains(errors, "ObservanceCommand visual proof", command, needle)
        require_contains(errors, "dialogue contract visual proof", dialogue_contracts, needle)

    stale_visual_proofs = [
        "shoreline signs",
        "finished-log signs",
        "R14/child-line signs",
        "third-bay warning signs",
        "WARDEN-3 notice",
        "lectern-shelf books, and market board",
        '"ending evidence"',
    ]
    for needle in stale_visual_proofs:
        require_absent(errors, "tools guardrails", dialogue_contracts + unlit_readiness, needle)


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
    check_director_truth(cards, errors)

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

    print("\nRESULT: CLEAN - archive, side quests, sites, puzzle gates, manifest lanes, and director truth guards resolve.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
