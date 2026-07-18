#!/usr/bin/env python3
"""Fail-closed validation for The Observance V5 canonical and runtime content."""

from __future__ import annotations

import csv
import hashlib
import json
import re
import struct
import sys
from collections import Counter, defaultdict, deque
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NODE_PATH = ROOT / "design" / "ARG-V5-NODE-MANIFEST.csv"
BOOK_PATH = ROOT / "arc" / "v5" / "minecraft-books.json"
EVIDENCE_TEXT_PATH = ROOT / "arc" / "v5" / "evidence-item-text.json"
EVIDENCE_APPEARANCE_PATH = ROOT / "arc" / "v5" / "evidence-item-appearance.json"
BOOK_PLACEMENT_PATH = ROOT / "design" / "ARG-V5-BOOK-PLACEMENT.csv"
MEDIA_PATH = ROOT / "arc" / "v5" / "media-manifest.json"
GENERATED_IMAGE_PATH = ROOT / "arc" / "v5" / "generated-image-manifest.json"
NPC_PATH = ROOT / "arc" / "v5" / "npc-dialogue.json"
ROOM_ASSIGNMENT_PATH = ROOT / "design" / "ARG-V5-ROOM-ASSIGNMENTS.csv"
ROOM_BOX_PATH = ROOT / "design" / "DEEP-HOLD-ROOM-BOXES.csv"
FIXTURE_OWNERSHIP_PATH = ROOT / "design" / "ARG-V5-FIXTURE-OWNERSHIP.csv"
FIXTURE_PATH = ROOT / "design" / "DEEP-HOLD-FIXTURE-MANIFEST.csv"
RECORD_STATION_PATH = ROOT / "design" / "DEEP-HOLD-RECORD-STATION-MANIFEST.csv"
RECORD_OWNERSHIP_PATH = ROOT / "design" / "ARG-V5-RECORD-OWNERSHIP.csv"
RUNTIME_BINDING_PATH = ROOT / "design" / "ARG-V5-RUNTIME-BINDINGS.csv"
ARTIFACT_PATH = ROOT / "design" / "ARG-V5-ARTIFACT-MANIFEST.csv"
PHYSICAL_PREDICATE_PATH = ROOT / "design" / "ARG-V5-PHYSICAL-PREDICATES.json"
V5_SEED_PATH = ROOT / "discord" / "supabase" / "seeds" / "v5_investigations.sql"
MAP_ART_PATH = ROOT / "arc" / "v5" / "map-art-manifest.json"

EXPECTED_CASE_COUNTS = {
    "C01": 6,
    "C02": 6,
    "C03": 18,
    "C04": 8,
    "C05": 8,
    "C06": 7,
    "C07": 10,
    "C08": 5,
    "C09": 8,
    "C10": 6,
}

EXPECTED_ARTIFACT_IDS = {
    "orientation_key", "survey_seal",
    "affidavit_vaun", "affidavit_mara", "affidavit_sella",
    "affidavit_orin", "affidavit_brann", "affidavit_iss",
    "cistern_seal", "breach_plate", "filter_cartridge", "system_key",
    "deep_access_plate", "witness_spool", "protocol_bridge",
    "averyn_fragment_a", "averyn_fragment_v", "averyn_fragment_e",
    "averyn_fragment_r", "averyn_fragment_y", "averyn_fragment_n",
}

EXPECTED_EVIDENCE_TEXT_IDS = {
    "wr01_private_bridge", "wr01_private_names", "wr01_private_disappearance",
    "bi01_wick_segment_1", "bi01_wick_segment_2", "bi01_wick_segment_3",
    "cw07_discipline_drafts",
}

EXPECTED_EVIDENCE_APPEARANCE_IDS = {
    "A3T", "A3L", "A4T", "A4L", "B3T", "B3L", "B4T", "B4L",
    "bi06_living_reed", "bi06_viable_water", "bi06_surface_air",
    "ki02_reed_knot",
    "lc01_rough_shelter", "lc01_civic_ring", "lc01_deep_works",
    "lc03_market_tally", "lc03_dwelling_repair",
    "kv01_cloth_cistern_receipt", "kv01_charcoal_cistern_receipt",
    *(f"kv01_cloth_slip_{index}" for index in range(10)),
    *(f"kv01_charcoal_slip_{index}" for index in range(10)),
    "a02_wren_companion_tag",
    "light_102", "light_109", "light_117",
    "food_203", "food_208", "food_219",
    "build_301", "build_314", "build_326",
    "record_401", "record_407", "record_413",
    "ar06_bell_9", "cw07_genuine_filter", "cw07_purchase_receipt",
    "bi02_layer_old1", "bi02_layer_old2", "bi02_layer_old3", "bi02_drill_dust",
    "bi03_feed", "bi03_water", "bi03_cover",
    "bi03_feed_disturbed", "bi03_water_disturbed",
    "bi04_village_pressure", "bi04_hold_pressure", "bi04_load_trace",
    "bi06_reed_late", "bi06_water_late",
    "ks01_dzgvi", "ks01_pvvkh", "ks01_gsv", "ks01_yzxp", "ks01_lu",
    "ks01_gsv_kztv",
    "wr02_mkept_login", "wr02_ash_cache", "wr02_rook_bridge", "wr02_player_fears",
    "wr04_rook_left", "wr04_rook_center", "wr04_rook_right",
    *(f"kv02_return_{index:02d}" for index in range(1, 37)),
}

ALLOWED_EXTERNAL_FLAGS = {
    "start",
    "v5_coda_publish",
    "v5_coda_unfiled",
}

TEXT_SUFFIXES = {".java", ".json", ".md", ".sql", ".ts", ".tsx", ".yml", ".yaml"}
RUNTIME_SCAN_ROOTS = [
    ROOT / "plugin" / "src" / "main",
    ROOT / "discord" / "src",
    ROOT / "discord" / "supabase" / "seeds",
    ROOT / "dashboard" / "src",
    ROOT / "arc" / "v5",
]

FORBIDDEN_RUNTIME_PATTERNS = {
    "six-as-one": re.compile(r"\bsix\s+(?:were|are|was)\s+(?:all\s+)?one\b", re.I),
    "mkept-not-person": re.compile(r"mkept.{0,80}(?:no|not|never)\s+(?:a\s+)?(?:real\s+)?person", re.I | re.S),
    "old-kept-counter": re.compile(r"kept\s*[:=]\s*6\b", re.I),
    "optional-media": re.compile(r"optional.{0,60}(?:clip|footage|media|spectrogram)|(?:clip|footage|media|spectrogram).{0,60}optional", re.I | re.S),
    "optional-unlit": re.compile(r"optional.{0,60}unlit|unlit.{0,60}optional", re.I | re.S),
    "prior-six-answers": re.compile(r"(?:prior|previous).{0,100}six\s+answers", re.I | re.S),
}


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def load_nodes(errors: list[str]) -> list[dict[str, str]]:
    if not NODE_PATH.is_file():
        fail(errors, f"missing node manifest: {NODE_PATH}")
        return []
    with NODE_PATH.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    required = {
        "node_id", "case_id", "ordinal", "title", "room_id", "modality",
        "input_surface", "prerequisites", "completion_flag", "reward", "recovery",
    }
    if not rows:
        fail(errors, "node manifest is empty")
        return []
    missing_columns = required.difference(rows[0])
    if missing_columns:
        fail(errors, f"node manifest missing columns: {sorted(missing_columns)}")
        return rows
    for index, row in enumerate(rows, start=2):
        for field in required:
            if not (row.get(field) or "").strip():
                fail(errors, f"node row {index} has empty {field}")
    return rows


def validate_nodes(rows: list[dict[str, str]], errors: list[str]) -> None:
    if len(rows) != 82:
        fail(errors, f"expected 82 required nodes; found {len(rows)}")

    ids = [row["node_id"] for row in rows]
    flags = [row["completion_flag"] for row in rows]
    for label, values in (("node id", ids), ("completion flag", flags)):
        duplicates = sorted(value for value, count in Counter(values).items() if count > 1)
        if duplicates:
            fail(errors, f"duplicate {label}s: {duplicates}")

    counts = Counter(row["case_id"] for row in rows)
    if dict(counts) != EXPECTED_CASE_COUNTS:
        fail(errors, f"case counts {dict(counts)} do not match {EXPECTED_CASE_COUNTS}")

    by_case: dict[str, list[int]] = defaultdict(list)
    for row in rows:
        try:
            by_case[row["case_id"]].append(int(row["ordinal"]))
        except ValueError:
            fail(errors, f"{row['node_id']} has non-numeric ordinal {row['ordinal']!r}")
    for case_id, expected_count in EXPECTED_CASE_COUNTS.items():
        expected = list(range(1, expected_count + 1))
        actual = sorted(by_case.get(case_id, []))
        if actual != expected:
            fail(errors, f"{case_id} ordinals {actual} do not equal {expected}")

    known_flags = set(flags) | ALLOWED_EXTERNAL_FLAGS
    graph: dict[str, set[str]] = {row["completion_flag"]: set() for row in rows}
    indegree: dict[str, int] = {row["completion_flag"]: 0 for row in rows}
    for row in rows:
        target = row["completion_flag"]
        for prerequisite in filter(None, (p.strip() for p in row["prerequisites"].split(";"))):
            if prerequisite not in known_flags:
                fail(errors, f"{row['node_id']} references unknown prerequisite {prerequisite}")
            if prerequisite in graph and target not in graph[prerequisite]:
                graph[prerequisite].add(target)
                indegree[target] += 1

    queue = deque(flag for flag, degree in indegree.items() if degree == 0)
    visited = 0
    while queue:
        flag = queue.popleft()
        visited += 1
        for nxt in graph[flag]:
            indegree[nxt] -= 1
            if indegree[nxt] == 0:
                queue.append(nxt)
    if visited != len(graph):
        fail(errors, "node prerequisite graph contains a cycle")


def load_json(path: Path, errors: list[str]) -> dict:
    if not path.is_file():
        fail(errors, f"missing JSON manifest: {path}")
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(errors, f"invalid JSON {path}: {exc}")
        return {}


def validate_books(
    data: dict,
    node_ids: set[str],
    node_cases: dict[str, str],
    known_flags: set[str],
    errors: list[str],
) -> None:
    books = data.get("books")
    if not isinstance(books, list) or not books:
        fail(errors, "book manifest has no books")
        return
    constraints = data.get("constraints") or {}
    max_title = int(constraints.get("maxTitleCharacters", 32))
    max_page = int(constraints.get("maxPageCharacters", 240))
    seen: set[str] = set()
    for book in books:
        book_id = str(book.get("id", "")).strip()
        if not book_id:
            fail(errors, "book has no id")
            continue
        if book_id in seen:
            fail(errors, f"duplicate book id {book_id}")
        seen.add(book_id)
        if book.get("nodeId") not in node_ids:
            fail(errors, f"book {book_id} references unknown node {book.get('nodeId')}")
        if book.get("unlockFlag") not in known_flags:
            fail(errors, f"book {book_id} references unknown unlock flag {book.get('unlockFlag')}")
        title = str(book.get("title", ""))
        author = str(book.get("author", ""))
        if not title or len(title) > max_title:
            fail(errors, f"book {book_id} title length is invalid ({len(title)})")
        if not author:
            fail(errors, f"book {book_id} has empty author")
        pages = book.get("pages")
        if not isinstance(pages, list) or not pages:
            fail(errors, f"book {book_id} has no pages")
            continue
        player_text = " ".join([author, title, *(str(page) for page in pages)]).casefold()
        if node_cases.get(str(book.get("nodeId"))) in {f"C{i:02d}" for i in range(1, 9)}:
            if "averyn" in player_text:
                fail(errors, f"book {book_id} reveals Averyn before the P11 identity restoration")
        if "every person present" in player_text or "all players present" in player_text:
            fail(errors, f"book {book_id} makes attendance an eligibility gate")
        for page_index, page in enumerate(pages, start=1):
            if not isinstance(page, str) or not page.strip():
                fail(errors, f"book {book_id} page {page_index} is empty")
            elif len(page) > max_page:
                fail(errors, f"book {book_id} page {page_index} has {len(page)} characters (max {max_page})")


def validate_evidence_text(data: dict, node_ids: set[str], errors: list[str]) -> None:
    if data.get("schemaVersion") != 1:
        fail(errors, "evidence-item text authority must use schemaVersion 1")
    items = data.get("items")
    if not isinstance(items, list):
        fail(errors, "evidence-item text authority has no items")
        return
    ids = [str(item.get("id", "")).strip() for item in items]
    if set(ids) != EXPECTED_EVIDENCE_TEXT_IDS or len(ids) != len(EXPECTED_EVIDENCE_TEXT_IDS):
        fail(errors, "evidence-item text authority must contain the exact 20 WR01/BI01/CW07/HS04 readable items")
    duplicates = sorted(value for value, count in Counter(ids).items() if count > 1)
    if duplicates:
        fail(errors, f"duplicate evidence-item text ids: {duplicates}")
    for item in items:
        item_id = str(item.get("id", "")).strip()
        if item.get("nodeId") not in node_ids:
            fail(errors, f"evidence item {item_id} references unknown node {item.get('nodeId')}")
        if item.get("material") != "WRITTEN_BOOK":
            fail(errors, f"evidence item {item_id} must be a WRITTEN_BOOK")
        title = str(item.get("title", ""))
        author = str(item.get("author", ""))
        pages = item.get("pages")
        if not title or len(title) > 32:
            fail(errors, f"evidence item {item_id} title length is invalid ({len(title)})")
        if not author or len(author) > 32:
            fail(errors, f"evidence item {item_id} author length is invalid ({len(author)})")
        if not isinstance(pages, list) or not pages:
            fail(errors, f"evidence item {item_id} has no readable pages")
            continue
        folded = " ".join(str(page) for page in pages).casefold()
        if "filed evidence" in folded or "placeholder" in folded or "lorem" in folded:
            fail(errors, f"evidence item {item_id} contains generic/placeholder copy")
        for page_index, page in enumerate(pages, start=1):
            if not isinstance(page, str) or not page.strip():
                fail(errors, f"evidence item {item_id} page {page_index} is empty")
            elif len(page) > 240:
                fail(errors, f"evidence item {item_id} page {page_index} exceeds 240 characters")


def validate_evidence_appearance(data: dict, node_ids: set[str], errors: list[str]) -> None:
    if data.get("schemaVersion") != 1:
        fail(errors, "evidence-item appearance authority must use schemaVersion 1")
    items = data.get("items")
    if not isinstance(items, list):
        fail(errors, "evidence-item appearance authority has no items")
        return
    ids = [str(item.get("id", "")).strip() for item in items]
    if set(ids) != EXPECTED_EVIDENCE_APPEARANCE_IDS or len(ids) != len(EXPECTED_EVIDENCE_APPEARANCE_IDS):
        fail(errors, "evidence-item appearance authority must contain the exact 118 visible non-book evidence items")
    duplicates = sorted(value for value, count in Counter(ids).items() if count > 1)
    if duplicates:
        fail(errors, f"duplicate evidence-item appearance ids: {duplicates}")
    for item in items:
        item_id = str(item.get("id", "")).strip()
        node_id = str(item.get("nodeId", "")).strip()
        title = str(item.get("title", "")).strip()
        lore = item.get("lore")
        if node_id not in node_ids:
            fail(errors, f"evidence appearance {item_id} references unknown node {node_id}")
        if not title or len(title) > 48:
            fail(errors, f"evidence appearance {item_id} title length is invalid ({len(title)})")
        if not isinstance(lore, list) or not lore or len(lore) > 4:
            fail(errors, f"evidence appearance {item_id} must have 1-4 visible lore lines")
            continue
        visible = " ".join([title, *(str(line) for line in lore)]).casefold()
        if item_id.casefold() in visible or "v5_" in visible or "sort_class" in visible:
            fail(errors, f"evidence appearance {item_id} leaks an internal identity/class")
        for index, line in enumerate(lore, start=1):
            if not isinstance(line, str) or not line.strip() or len(line) > 96:
                fail(errors, f"evidence appearance {item_id} lore line {index} is empty or oversized")

    labels = data.get("containerLabels") or {}
    if set((labels.get("KV02") or {})) != {"cistern", "public_heat", "private_heat", "condemned"}:
        fail(errors, "KV02 appearance authority must label all four destination barrels")
    if set((labels.get("CW02") or {})) != {"A_top", "A_lower", "B_top", "B_lower"}:
        fail(errors, "CW02 appearance authority must label all four sample barrels")
    station_labels = (data.get("stationLabels") or {}).get("A02") or {}
    if set(station_labels) != {"mkept_station", "ash_station", "rook_station", "wren_station"}:
        fail(errors, "A02 appearance authority must label all four prior-company stations")


def validate_visible_evidence_coverage(
    physical: dict, evidence_text: dict, appearance: dict, map_art: dict, errors: list[str]
) -> None:
    observed: set[str] = set()

    def walk(value: object) -> None:
        if isinstance(value, dict):
            pdc = value.get("pdc")
            if isinstance(pdc, dict) and isinstance(pdc.get("v5_evidence_id"), str):
                observed.add(pdc["v5_evidence_id"].strip())
            if value.get("unique_pdc") == "v5_evidence_id":
                required = value.get("required_ids")
                if isinstance(required, list):
                    observed.update(str(item).strip() for item in required)
            if value.get("pdc_key") == "v5_evidence_id":
                required = value.get("required_items")
                if isinstance(required, list):
                    observed.update(str(item).strip() for item in required if isinstance(item, str))
            for child in value.values():
                walk(child)
        elif isinstance(value, list):
            for child in value:
                walk(child)

    walk(physical.get("nodes") or [])
    authored = {
        str(item.get("id", "")).strip()
        for item in (evidence_text.get("items") or []) + (appearance.get("items") or [])
        if isinstance(item, dict)
    }
    authored.update(
        str(item.get("id", "")).strip()
        for item in map_art.get("maps") or []
        if isinstance(item, dict)
    )
    missing = sorted(observed.difference(authored))
    if missing:
        fail(errors, f"player-facing evidence ids lack readable book/lore/map authority: {missing}")


def validate_media(data: dict, node_ids: set[str], known_flags: set[str], errors: list[str]) -> None:
    policy = data.get("policy") or {}
    if policy.get("allRequired") is not True or policy.get("automaticReveal") is not True:
        fail(errors, "media manifest must mark every asset required and automatically revealed")
    assets = data.get("assets")
    if not isinstance(assets, list) or len(assets) != 5:
        fail(errors, f"expected five required media assets; found {len(assets or [])}")
        return
    ids: set[str] = set()
    payloads: set[str] = set()
    for asset in assets:
        asset_id = str(asset.get("id", "")).strip()
        if not asset_id or asset_id in ids:
            fail(errors, f"missing or duplicate media id {asset_id!r}")
        ids.add(asset_id)
        if asset.get("nodeId") not in node_ids:
            fail(errors, f"media {asset_id} references unknown node {asset.get('nodeId')}")
        for flag_field in ("revealPrerequisite", "completionFlag"):
            if asset.get(flag_field) not in known_flags:
                fail(errors, f"media {asset_id} references unknown {flag_field} {asset.get(flag_field)}")
        payload = str(asset.get("expectedPayload", "")).strip().upper()
        if not payload:
            fail(errors, f"media {asset_id} has no payload")
        payloads.add(payload)
        url = str(asset.get("url") or asset.get("archiveUrl") or "")
        if not url.startswith("https://"):
            fail(errors, f"media {asset_id} has no HTTPS URL")
        sha1 = str(asset.get("sourceSha1", ""))
        if not re.fullmatch(r"[0-9a-f]{40}", sha1):
            fail(errors, f"media {asset_id} has invalid SHA-1 receipt")
    expected_payloads = {
        "ASH-13",
        "WHERE THE REEDS FOLD BACK",
        "STAY AWAKE",
        "SIX RETURN, ONE IS NOT KEPT",
        "I WAS NOT KEPT",
    }
    if payloads != expected_payloads:
        fail(errors, f"media payloads {sorted(payloads)} do not match the fixed set")
    brann = next((asset for asset in assets if asset.get("id") == "clip_03_watch_correction"), None)
    if brann is None or brann.get("acceptedAliases") != ["stay awake"]:
        fail(errors, "KB01 must accept only exact normalized STAY AWAKE; broad aliases bypass required clip 3")


def validate_generated_images(
    data: dict, node_ids: set[str], known_flags: set[str], errors: list[str]
) -> None:
    policy = data.get("policy") or {}
    if policy.get("allRequired") is not True:
        fail(errors, "generated-image manifest must mark both evidence stills required")
    if policy.get("revealPrerequisite") not in known_flags:
        fail(errors, "generated-image manifest has an unknown reveal prerequisite")
    if policy.get("findingNode") not in node_ids:
        fail(errors, "generated-image manifest has an unknown finding node")
    assets = data.get("assets")
    if not isinstance(assets, list) or len(assets) != 2:
        fail(errors, f"expected two required generated evidence stills; found {len(assets or [])}")
        return
    expected_ids = {"camp_frame_06", "locker_detail_13"}
    observed_ids: set[str] = set()
    for asset in assets:
        asset_id = str(asset.get("id", "")).strip()
        observed_ids.add(asset_id)
        if asset.get("nodeId") != "A07":
            fail(errors, f"generated image {asset_id} must be owned by A07")
        public_path = str(asset.get("publicPath", ""))
        source_path = str(asset.get("sourcePath", ""))
        if public_path != "/" + source_path.removeprefix("dashboard/public/"):
            fail(errors, f"generated image {asset_id} public/source paths drift")
        resolved = (ROOT / source_path).resolve()
        try:
            resolved.relative_to(ROOT.resolve())
        except ValueError:
            fail(errors, f"generated image {asset_id} escapes the repository")
            continue
        if not resolved.is_file():
            fail(errors, f"generated image {asset_id} is missing at {source_path}")
            continue
        payload = resolved.read_bytes()
        if len(payload) < 100_000 or payload[:8] != b"\x89PNG\r\n\x1a\n":
            fail(errors, f"generated image {asset_id} is not a full PNG evidence asset")
            continue
        width, height = struct.unpack(">II", payload[16:24])
        if (width, height) != (1448, 1086):
            fail(errors, f"generated image {asset_id} dimensions {(width, height)} drift from 1448x1086")
        actual_sha1 = hashlib.sha1(payload).hexdigest()
        if actual_sha1 != str(asset.get("sha1", "")).lower():
            fail(errors, f"generated image {asset_id} SHA-1 receipt does not match its bytes")
        if not str(asset.get("narrativeUse", "")).strip():
            fail(errors, f"generated image {asset_id} has no required narrative use")
    if observed_ids != expected_ids:
        fail(errors, f"generated image ids {sorted(observed_ids)} do not match {sorted(expected_ids)}")


def validate_npcs(data: dict, errors: list[str]) -> None:
    delivery = data.get("delivery") or {}
    if delivery.get("synchronousInMinecraft") is not True:
        fail(errors, "NPC dialogue must be synchronous in Minecraft")
    if delivery.get("hourlyShowrunnerForbiddenForReplies") is not True:
        fail(errors, "NPC dialogue must forbid hourly-showrunner reply latency")
    townsfolk = data.get("townsfolk")
    if not isinstance(townsfolk, list) or len(townsfolk) != 5:
        fail(errors, f"expected five surface townsfolk; found {len(townsfolk or [])}")
        return
    expected = {"aro", "wenna", "coll", "dob", "old-pell"}
    actual = {str(npc.get("id", "")) for npc in townsfolk}
    if actual != expected:
        fail(errors, f"townsfolk ids {sorted(actual)} do not match {sorted(expected)}")
    for npc in townsfolk:
        npc_id = npc.get("id", "<missing>")
        if not str(npc.get("anchorSite", "")).startswith("npc_"):
            fail(errors, f"NPC {npc_id} has no canonical anchor site")
        lines = npc.get("lines")
        if not isinstance(lines, dict) or "arrival" not in lines or "coda" not in lines:
            fail(errors, f"NPC {npc_id} lacks arrival/coda dialogue")
            continue
        if not isinstance(lines.get("after_p11"), list) or len(lines["after_p11"]) != 2:
            fail(errors, f"NPC {npc_id} must have two exact P11 identity-restoration responses")
        for state, state_lines in lines.items():
            if not isinstance(state_lines, list) or not state_lines:
                fail(errors, f"NPC {npc_id} state {state} has no lines")
            for line in state_lines or []:
                if not isinstance(line, str) or not line.strip():
                    fail(errors, f"NPC {npc_id} state {state} contains an empty line")
                if state not in {"after_p11", "coda"} and "averyn" in str(line).casefold():
                    fail(errors, f"NPC {npc_id} state {state} reveals Averyn before P11")
    listener = (ROOT / "plugin/src/main/java/com/observance/watcher/signal/listener/TownsfolkNpcListener.java").read_text(
        encoding="utf-8"
    )
    if listener.count('truthy(flags.get("p11.averyn_restored_unbound"))') != 5:
        fail(errors, "all five townsfolk must select P11 responses from the exact unbound event")
    if listener.count('yield "after_p11"') != 5:
        fail(errors, "all five townsfolk must route to their exact after_p11 dialogue")
    wren = data.get("wren") or {}
    if wren.get("id") != "wren":
        fail(errors, "NPC manifest has no Wren entry")
    wren_lines = wren.get("lines") or {}
    for state in (
        "confession", "reckoning_condemn", "reckoning_understand", "reckoning_free",
        "coda_condemn", "coda_understand", "coda_free",
    ):
        if not wren_lines.get(state):
            fail(errors, f"Wren is missing required state {state}")


def read_csv(path: Path, errors: list[str]) -> list[dict[str, str]]:
    if not path.is_file():
        fail(errors, f"missing CSV manifest: {path}")
        return []
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def expand_node_refs(raw: str) -> list[str]:
    expanded: list[str] = []
    for token in filter(None, (part.strip() for part in raw.split(";"))):
        match = re.fullmatch(r"([A-Z]+)(\d+)-([A-Z]+)?(\d+)", token)
        if not match:
            expanded.append(token)
            continue
        start_prefix, start_digits, end_prefix, end_digits = match.groups()
        prefix = end_prefix or start_prefix
        if prefix != start_prefix:
            expanded.append(token)
            continue
        width = max(len(start_digits), len(end_digits))
        for value in range(int(start_digits), int(end_digits) + 1):
            expanded.append(f"{prefix}{value:0{width}d}")
    return expanded


def validate_spatial_ownership(node_ids: set[str], errors: list[str]) -> None:
    room_boxes = read_csv(ROOM_BOX_PATH, errors)
    assignments = read_csv(ROOM_ASSIGNMENT_PATH, errors)
    box_ids = {row.get("room_id", "") for row in room_boxes}
    assignment_ids = [row.get("room_id", "") for row in assignments]
    if len(room_boxes) != 32 or len(assignments) != 32:
        fail(errors, f"expected 32 room boxes and assignments; found {len(room_boxes)} / {len(assignments)}")
    if set(assignment_ids) != box_ids:
        fail(errors, "V5 room assignments do not exactly cover the room-box manifest")
    if len(set(assignment_ids)) != len(assignment_ids):
        fail(errors, "V5 room assignments contain duplicate room ids")
    for row in assignments:
        for field in ("real_place", "node_ownership", "primary_gameplay", "route_contract"):
            if not (row.get(field) or "").strip():
                fail(errors, f"room {row.get('room_id')} has empty {field}")

    fixtures = read_csv(FIXTURE_PATH, errors)
    ownership = read_csv(FIXTURE_OWNERSHIP_PATH, errors)
    fixture_ids = {row.get("site_id", "") for row in fixtures}
    owner_ids = [row.get("site_id", "") for row in ownership]
    if len(fixtures) != 76 or len(ownership) != 76:
        fail(errors, f"expected 76 fixtures and ownership rows; found {len(fixtures)} / {len(ownership)}")
    if set(owner_ids) != fixture_ids:
        fail(errors, "V5 fixture ownership does not exactly cover the fixture manifest")
    if len(set(owner_ids)) != len(owner_ids):
        fail(errors, "V5 fixture ownership contains duplicate ids")
    for row in ownership:
        for node_id in expand_node_refs(row.get("node_ids", "")):
            if node_id not in node_ids:
                fail(errors, f"fixture {row.get('site_id')} references unknown node {node_id}")
        for field in ("v5_role", "critical_payload"):
            if not (row.get(field) or "").strip():
                fail(errors, f"fixture {row.get('site_id')} has empty {field}")

    record_stations = read_csv(RECORD_STATION_PATH, errors)
    record_ownership = read_csv(RECORD_OWNERSHIP_PATH, errors)
    station_ids = {row.get("station_id", "") for row in record_stations}
    owner_station_ids = [row.get("station_id", "") for row in record_ownership]
    if len(record_stations) != 7 or len(record_ownership) != 7:
        fail(errors, f"expected seven record stations and ownership rows; found {len(record_stations)} / {len(record_ownership)}")
    if set(owner_station_ids) != station_ids or len(set(owner_station_ids)) != len(owner_station_ids):
        fail(errors, "V5 record ownership must cover each record station exactly once")
    for row in record_ownership:
        for node_id in expand_node_refs(row.get("node_ids", "")):
            if node_id not in node_ids:
                fail(errors, f"record station {row.get('station_id')} references unknown node {node_id}")
        for field in ("v5_role", "critical_payload"):
            if not (row.get(field) or "").strip():
                fail(errors, f"record station {row.get('station_id')} has empty {field}")


def validate_artifacts(node_ids: set[str], errors: list[str]) -> None:
    artifacts = read_csv(ARTIFACT_PATH, errors)
    required = {
        "artifact_id", "earned_node", "material", "display_name", "pdc_key",
        "pdc_value", "consumer", "recovery_contract",
    }
    if artifacts and required.difference(artifacts[0]):
        fail(errors, f"artifact manifest missing columns: {sorted(required.difference(artifacts[0]))}")
        return
    ids = [row.get("artifact_id", "").strip() for row in artifacts]
    if set(ids) != EXPECTED_ARTIFACT_IDS or len(ids) != len(EXPECTED_ARTIFACT_IDS):
        fail(errors, "artifact manifest must contain exactly the 21 V5 critical artifacts")
    duplicates = sorted(value for value, count in Counter(ids).items() if count > 1)
    if duplicates:
        fail(errors, f"duplicate artifact ids: {duplicates}")
    for index, row in enumerate(artifacts, start=2):
        for field in required:
            if not (row.get(field) or "").strip():
                fail(errors, f"artifact row {index} has empty {field}")
        artifact_id = row.get("artifact_id", "")
        if not re.fullmatch(r"[a-z0-9_]+", artifact_id):
            fail(errors, f"artifact id {artifact_id!r} is not normalized")
        if row.get("earned_node") not in node_ids:
            fail(errors, f"artifact {artifact_id} references unknown earned node {row.get('earned_node')}")
        if row.get("pdc_key") != "v5_artifact_id" or row.get("pdc_value") != artifact_id:
            fail(errors, f"artifact {artifact_id} must use exact v5_artifact_id self-marker")
        if row.get("material", "") != row.get("material", "").upper():
            fail(errors, f"artifact {artifact_id} material must be an uppercase Bukkit material")
        recovery = row.get("recovery_contract", "").lower()
        if "reissue" not in recovery or "duplicate scan" not in recovery:
            fail(errors, f"artifact {artifact_id} has no idempotent duplicate-scan recovery contract")


def validate_book_placements(book_data: dict, node_ids: set[str], errors: list[str]) -> None:
    books = book_data.get("books") or []
    book_by_id = {str(book.get("id", "")): book for book in books}
    placements = read_csv(BOOK_PLACEMENT_PATH, errors)
    required = {
        "book_id", "node_id", "holder_kind", "holder_id", "mount", "expected_front",
        "availability", "book_pdc_id", "artifact_id",
    }
    if placements and required.difference(placements[0]):
        fail(errors, f"book placement manifest missing columns: {sorted(required.difference(placements[0]))}")
        return
    placement_ids = [row.get("book_id", "") for row in placements]
    if len(placements) != 44 or set(placement_ids) != set(book_by_id):
        fail(errors, "book placement manifest must place each of the 44 canonical books exactly once")
    duplicates = sorted(value for value, count in Counter(placement_ids).items() if count > 1)
    if duplicates:
        fail(errors, f"duplicate book placements: {duplicates}")

    holder_ids = {row.get("site_id", "") for row in read_csv(FIXTURE_PATH, errors)}
    holder_ids |= {row.get("station_id", "") for row in read_csv(RECORD_STATION_PATH, errors)}
    holder_ids.add("unlit_house_base")
    artifact_ids = {row.get("artifact_id", "") for row in read_csv(ARTIFACT_PATH, errors)}
    mount_keys: Counter[tuple[str, str]] = Counter()
    artifact_books: set[str] = set()
    for index, row in enumerate(placements, start=2):
        for field in required:
            if not (row.get(field) or "").strip():
                fail(errors, f"book placement row {index} has empty {field}")
        book_id = row.get("book_id", "")
        book = book_by_id.get(book_id)
        if book is None:
            continue
        if row.get("node_id") != book.get("nodeId") or row.get("node_id") not in node_ids:
            fail(errors, f"book placement {book_id} has wrong node {row.get('node_id')}")
        if row.get("holder_id") not in holder_ids:
            fail(errors, f"book placement {book_id} references unknown holder {row.get('holder_id')}")
        if row.get("book_pdc_id") != book_id:
            fail(errors, f"book placement {book_id} must self-mark book_pdc_id")
        unlock_flag = str(book.get("unlockFlag", ""))
        if unlock_flag not in row.get("availability", ""):
            fail(errors, f"book placement {book_id} availability omits unlock flag {unlock_flag}")
        if row.get("holder_kind") not in {"locked_lectern", "earned_artifact", "branch_lectern"}:
            fail(errors, f"book placement {book_id} has unsupported holder kind {row.get('holder_kind')}")
        artifact_id = row.get("artifact_id")
        if row.get("holder_kind") == "earned_artifact":
            if artifact_id not in artifact_ids:
                fail(errors, f"artifact book {book_id} references unknown artifact {artifact_id}")
            artifact_books.add(book_id)
        elif artifact_id != "none":
            fail(errors, f"non-artifact book {book_id} unexpectedly references {artifact_id}")
        mount_keys[(row.get("holder_id", ""), row.get("mount", ""))] += 1

    expected_artifact_books = {
        "vaun_sealed_affidavit", "mara_sealed_affidavit", "sella_sealed_affidavit",
        "orin_sealed_affidavit", "brann_sealed_affidavit", "iss_sealed_affidavit",
    }
    if artifact_books != expected_artifact_books:
        fail(errors, "the six Keeper affidavit books must be the six recoverable written-book artifacts")
    for holder_mount, count in mount_keys.items():
        if count > 1 and holder_mount != ("release_record", "record_lectern"):
            fail(errors, f"multiple books occupy holder/mount {holder_mount}")


def validate_runtime_bindings(node_rows: list[dict[str, str]], errors: list[str]) -> None:
    bindings = read_csv(RUNTIME_BINDING_PATH, errors)
    required = {"node_id", "owner", "handler", "site_id", "completion_flag", "replay_policy"}
    if bindings and required.difference(bindings[0]):
        fail(errors, f"runtime binding manifest missing columns: {sorted(required.difference(bindings[0]))}")
        return
    by_node = {row.get("node_id", ""): row for row in node_rows}
    binding_ids = [row.get("node_id", "") for row in bindings]
    if len(bindings) != 82 or set(binding_ids) != set(by_node):
        fail(errors, "runtime binding manifest must bind all 82 nodes exactly once")
    duplicates = sorted(value for value, count in Counter(binding_ids).items() if count > 1)
    if duplicates:
        fail(errors, f"duplicate runtime node bindings: {duplicates}")

    fixture_ids = {row.get("site_id", "") for row in read_csv(FIXTURE_PATH, errors)}
    station_ids = {row.get("station_id", "") for row in read_csv(RECORD_STATION_PATH, errors)}
    plugin_sites = fixture_ids | station_ids | {"npc_wren_anchor"}
    unlit_sites = {
        "unlit_house_lamp", "unlit_house_cairn", "unlit_house_coop", "unlit_house_well",
        "unlit_house_watch", "unlit_house_warm", "unlit_house_threshold", "unlit_house_base",
    }
    owners = {"website", "discord", "plugin", "plugin_unlit", "plugin_finale"}
    for index, binding in enumerate(bindings, start=2):
        for field in required:
            if not (binding.get(field) or "").strip():
                fail(errors, f"runtime binding row {index} has empty {field}")
        node_id = binding.get("node_id", "")
        node = by_node.get(node_id)
        if node is None:
            continue
        if binding.get("completion_flag") != node.get("completion_flag"):
            fail(errors, f"runtime binding {node_id} completion flag drifts from node manifest")
        owner = binding.get("owner")
        if owner not in owners:
            fail(errors, f"runtime binding {node_id} has unsupported owner {owner}")
        if owner in {"plugin", "plugin_finale"} and binding.get("site_id") not in plugin_sites:
            fail(errors, f"runtime binding {node_id} references unknown plugin site {binding.get('site_id')}")
        if owner == "plugin_unlit" and binding.get("site_id") not in unlit_sites:
            fail(errors, f"runtime binding {node_id} references unknown Unlit site {binding.get('site_id')}")
        if "idempotent" not in binding.get("replay_policy", "") and binding.get("replay_policy") != "durable_before_theater":
            fail(errors, f"runtime binding {node_id} lacks an idempotent/durable replay policy")


def validate_remote_authority_receipt(errors: list[str]) -> None:
    expected = hashlib.sha256(PHYSICAL_PREDICATE_PATH.read_bytes()).hexdigest()
    try:
        seed = V5_SEED_PATH.read_text(encoding="utf-8")
    except OSError as exc:
        fail(errors, f"cannot read V5 Supabase seed: {exc}")
        return
    match = re.search(
        r"'v5_physical_authority_sha256'\s*,\s*to_jsonb\('([0-9a-f]{64})'::text\)",
        seed,
    )
    if match is None:
        fail(errors, "V5 Supabase seed has no exact physical-authority SHA-256 receipt")
    elif match.group(1) != expected:
        fail(errors, f"V5 Supabase authority receipt {match.group(1)} != packaged source {expected}")


def scan_forbidden_runtime_claims(errors: list[str]) -> None:
    for root in RUNTIME_SCAN_ROOTS:
        if not root.exists():
            continue
        paths = [root] if root.is_file() else root.rglob("*")
        for path in paths:
            if not path.is_file() or path.suffix.lower() not in TEXT_SUFFIXES:
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            relative = path.relative_to(ROOT).as_posix()
            for label, pattern in FORBIDDEN_RUNTIME_PATTERNS.items():
                match = pattern.search(text)
                if match:
                    line = text.count("\n", 0, match.start()) + 1
                    fail(errors, f"forbidden runtime claim {label} at {relative}:{line}")


def main() -> int:
    errors: list[str] = []
    rows = load_nodes(errors)
    validate_nodes(rows, errors)
    node_ids = {row.get("node_id", "") for row in rows}
    node_cases = {row.get("node_id", ""): row.get("case_id", "") for row in rows}
    known_flags = {row.get("completion_flag", "") for row in rows} | ALLOWED_EXTERNAL_FLAGS
    book_data = load_json(BOOK_PATH, errors)
    validate_books(book_data, node_ids, node_cases, known_flags, errors)
    evidence_text = load_json(EVIDENCE_TEXT_PATH, errors)
    appearance = load_json(EVIDENCE_APPEARANCE_PATH, errors)
    validate_evidence_text(evidence_text, node_ids, errors)
    validate_evidence_appearance(appearance, node_ids, errors)
    validate_visible_evidence_coverage(
        load_json(PHYSICAL_PREDICATE_PATH, errors), evidence_text, appearance,
        load_json(MAP_ART_PATH, errors), errors,
    )
    validate_media(load_json(MEDIA_PATH, errors), node_ids, known_flags, errors)
    validate_generated_images(load_json(GENERATED_IMAGE_PATH, errors), node_ids, known_flags, errors)
    validate_npcs(load_json(NPC_PATH, errors), errors)
    validate_spatial_ownership(node_ids, errors)
    validate_artifacts(node_ids, errors)
    validate_book_placements(book_data, node_ids, errors)
    validate_runtime_bindings(rows, errors)
    validate_remote_authority_receipt(errors)
    if "--runtime" in sys.argv:
        scan_forbidden_runtime_claims(errors)

    if errors:
        print("V5 CONTENT CHECK: FAIL")
        for error in errors:
            print(f"- {error}")
        return 1
    print(f"V5 CONTENT CHECK: PASS ({len(rows)} nodes, {len(node_ids)} unique ids)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
