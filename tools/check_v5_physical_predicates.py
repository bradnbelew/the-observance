#!/usr/bin/env python3
"""Validate the executable V5 Minecraft predicate authority.

This is intentionally stricter than a JSON-schema shape check. It proves exact
coverage/parity with the canonical node/runtime/artifact/book manifests and
checks handler-specific executable detail so a physical node cannot quietly
degrade into a generic click/touch completion.
"""

from __future__ import annotations

import csv
import hashlib
import json
import struct
import sys
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "design"
ARC_V5 = ROOT / "arc" / "v5"
PREDICATES = DESIGN / "ARG-V5-PHYSICAL-PREDICATES.json"
RUNTIME = DESIGN / "ARG-V5-RUNTIME-BINDINGS.csv"
NODES = DESIGN / "ARG-V5-NODE-MANIFEST.csv"
ARTIFACTS = DESIGN / "ARG-V5-ARTIFACT-MANIFEST.csv"
BOOKS = ARC_V5 / "minecraft-books.json"
MAP_ART = ARC_V5 / "map-art-manifest.json"

OWNERS = {"plugin", "plugin_unlit", "plugin_finale"}
EXPECTED_COUNTS = {"plugin": 51, "plugin_unlit": 7, "plugin_finale": 2}
REQUIRED_NODE_KEYS = {
    "node_id", "owner", "prerequisites", "site_id", "handler",
    "completion_flag", "predicate", "wrong_input", "reward",
    "reset_repair_recovery", "concurrency_replay", "durability_profile",
}
REQUIRED_PREDICATE_KEYS = {"kind", "evaluation_trigger", "components", "all_of", "commit"}
REQUIRED_WRONG_KEYS = {"policy", "feedback", "state_effect"}
REQUIRED_REPAIR_KEYS = {"reset", "repair", "recovery"}
REQUIRED_CONCURRENCY_KEYS = {"scope", "lock", "replay_policy", "disconnect", "after_complete"}

GENERIC_TOKENS = {
    "todo", "tbd", "placeholder", "generic_interaction", "generic interaction",
    "touch_to_solve", "touch to solve", "interact_with_site", "interact with site",
    "right_click_any", "right click any", "auto_complete_on_enter", "auto complete on enter",
    "enter radius to solve", "click site to solve",
}

CANONICAL_ANSWERS = {
    "LC02": "87 PEOPLE 174 BASKETS",
    "LC04": "VENT EAST BEFORE SECOND BELL",
    "KV03": "CHECK HEARTH THREE AGAINST PUMP RECEIPT",
    "KM02": "WEST TWO LOW ONE EAST THREE",
    "KS03": "THE HEARING HID THE LOWER SAMPLE",
    "KB02": "PAIR NINE WITH EMPTY HOUR",
    "KI01": "THE SURFACE BREATHES BUT THE THIRD BAY CANNOT",
    "CW04": "TOMA RILL",
    "BI05": "BELL EIGHT",
    "HS03": "OPEN",
    "HS04": "LINE FOUR BYPASS OPEN",
    "A04": "ACCOUNT COMMUNITY POST TICKET ARCHIVE",
    "WR01": "WREN",
}

EXPECTED_ROTATIONS: dict[str, list[int]] = {
    "KO02": [0, 2, 4, 6, 0, 2],
    "CW03": [0, 2, 0, 2, 0],
    "HS05": [0, 2, 4, 6, 0, 2],
}

CONDUCT_CLAUSES = {
    "solo": "you carried every name alone. they still arrived together.",
    "unanimous": "you came to one answer without becoming one voice.",
    "divided": "you disagreed and continued together. the record never knew how to write that.",
    "persistent": "you did not agree quickly. you stayed until the evidence did.",
}

INVENTORY_CAPACITY = {
    "BARREL": 27,
    "CHEST": 27,
    "TRAPPED_CHEST": 27,
    "SHULKER_BOX": 27,
    "HOPPER": 5,
    "DROPPER": 9,
    "DISPENSER": 9,
    "CHISELED_BOOKSHELF": 6,
    "LECTERN": 1,
}


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def split_flags(raw: str) -> list[str]:
    value = (raw or "").strip()
    if not value or value == "start":
        return []
    return [part.strip() for part in value.split(";") if part.strip()]


def strings(value: Any) -> Iterable[str]:
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for key, child in value.items():
            yield str(key)
            yield from strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from strings(child)


def values_for_key(value: Any, wanted: str) -> list[Any]:
    found: list[Any] = []
    if isinstance(value, dict):
        for key, child in value.items():
            if key == wanted:
                found.append(child)
            found.extend(values_for_key(child, wanted))
    elif isinstance(value, list):
        for child in value:
            found.extend(values_for_key(child, wanted))
    return found


def flattened(value: Any) -> str:
    return " ".join(strings(value)).lower()


def norm_answer(raw: str) -> str:
    text = unicodedata.normalize("NFKC", raw).upper()
    text = "".join(ch if ch.isalnum() else " " for ch in text)
    return " ".join(text.split())


def require_text(obj: dict[str, Any], key: str, where: str, failures: list[str], minimum: int = 2) -> None:
    value = obj.get(key)
    if not isinstance(value, str) or len(value.strip()) < minimum:
        failures.append(f"{where}.{key}: missing or too short")


def component_by_id(node: dict[str, Any], component_id: str) -> dict[str, Any] | None:
    components = node.get("predicate", {}).get("components", [])
    for component in components:
        if isinstance(component, dict) and component.get("id") == component_id:
            return component
    return None


def all_ops(node: dict[str, Any]) -> list[str]:
    return [
        str(condition.get("op", ""))
        for condition in node.get("predicate", {}).get("all_of", [])
        if isinstance(condition, dict)
    ]


def derive_conduct(wr05: dict[str, int | bool], rp03: dict[str, int | bool]) -> str:
    """Pure executable form of the manifest's ordered conduct precedence."""
    max_roster = max(int(wr05["maximum_visible_roster_count"]), int(rp03["maximum_visible_roster_count"]))
    if max_roster == 1:
        return "solo"
    if (
        int(wr05["first_ballot_distinct_choices"]) > 1
        or bool(wr05["first_ballot_tied"])
        or int(rp03["first_ballot_distinct_choices"]) > 1
        or bool(rp03["first_ballot_tied"])
    ):
        return "divided"
    wr_unanimous = (
        int(wr05["first_ballot_cast_count"]) == int(wr05["first_ballot_eligible_count"])
        and int(wr05["first_ballot_distinct_choices"]) == 1
    )
    rp_unanimous = (
        int(rp03["first_ballot_cast_count"]) == int(rp03["first_ballot_eligible_count"])
        and int(rp03["first_ballot_distinct_choices"]) == 1
    )
    if wr_unanimous and rp_unanimous:
        return "unanimous"
    return "persistent"


def validate_conduct_totality(authority: dict[str, Any], failures: list[str]) -> None:
    rule = authority.get("global_rules", {}).get("conduct_verdict")
    if not isinstance(rule, dict):
        failures.append("global_rules.conduct_verdict is missing")
        return
    fields = {
        "initial_roster_count", "maximum_visible_roster_count",
        "first_ballot_eligible_count", "first_ballot_cast_count",
        "first_ballot_distinct_choices", "first_ballot_tied",
        "resolution_rounds", "disconnect_resnap_count",
    }
    if set(rule.get("recorded_inputs_per_vote", [])) != fields:
        failures.append("conduct verdict recorded-input fields are not exact")
    if rule.get("allowed_values") != ["solo", "divided", "unanimous", "persistent"]:
        failures.append("conduct verdict allowed values/order are not exact")
    precedence = rule.get("precedence")
    if not isinstance(precedence, list) or len(precedence) != 4:
        failures.append("conduct verdict needs exactly four ordered precedence rules")

    examples = {
        "solo": (
            {"maximum_visible_roster_count": 1, "first_ballot_eligible_count": 1, "first_ballot_cast_count": 1, "first_ballot_distinct_choices": 1, "first_ballot_tied": False},
            {"maximum_visible_roster_count": 1, "first_ballot_eligible_count": 1, "first_ballot_cast_count": 1, "first_ballot_distinct_choices": 1, "first_ballot_tied": False},
        ),
        "divided": (
            {"maximum_visible_roster_count": 3, "first_ballot_eligible_count": 3, "first_ballot_cast_count": 3, "first_ballot_distinct_choices": 2, "first_ballot_tied": True},
            {"maximum_visible_roster_count": 3, "first_ballot_eligible_count": 3, "first_ballot_cast_count": 3, "first_ballot_distinct_choices": 1, "first_ballot_tied": False},
        ),
        "unanimous": (
            {"maximum_visible_roster_count": 3, "first_ballot_eligible_count": 3, "first_ballot_cast_count": 3, "first_ballot_distinct_choices": 1, "first_ballot_tied": False},
            {"maximum_visible_roster_count": 2, "first_ballot_eligible_count": 2, "first_ballot_cast_count": 2, "first_ballot_distinct_choices": 1, "first_ballot_tied": False},
        ),
        "persistent": (
            {"maximum_visible_roster_count": 3, "first_ballot_eligible_count": 3, "first_ballot_cast_count": 2, "first_ballot_distinct_choices": 1, "first_ballot_tied": False},
            {"maximum_visible_roster_count": 3, "first_ballot_eligible_count": 3, "first_ballot_cast_count": 3, "first_ballot_distinct_choices": 1, "first_ballot_tied": False},
        ),
    }
    for expected, (wr05, rp03) in examples.items():
        actual = derive_conduct(wr05, rp03)
        if actual != expected:
            failures.append(f"conduct verdict example {expected} resolved as {actual}")

    # Exhaust a compact domain of every valid first-ballot count. Every input
    # must resolve to exactly one known value; all four values must be reachable.
    reached: set[str] = set()
    for wr_max in range(1, 4):
        for rp_max in range(1, 4):
            for wr_cast in range(0, wr_max + 1):
                for rp_cast in range(0, rp_max + 1):
                    for wr_distinct in range(0, min(wr_cast, 3) + 1):
                        for rp_distinct in range(0, min(rp_cast, 2) + 1):
                            if wr_cast > 0 and wr_distinct == 0:
                                continue
                            if rp_cast > 0 and rp_distinct == 0:
                                continue
                            if wr_cast == 0 and wr_distinct != 0:
                                continue
                            if rp_cast == 0 and rp_distinct != 0:
                                continue
                            wr = {"maximum_visible_roster_count": wr_max, "first_ballot_eligible_count": wr_max, "first_ballot_cast_count": wr_cast, "first_ballot_distinct_choices": wr_distinct, "first_ballot_tied": wr_distinct > 1 and wr_cast % wr_distinct == 0}
                            rp = {"maximum_visible_roster_count": rp_max, "first_ballot_eligible_count": rp_max, "first_ballot_cast_count": rp_cast, "first_ballot_distinct_choices": rp_distinct, "first_ballot_tied": rp_distinct > 1 and rp_cast % rp_distinct == 0}
                            verdict = derive_conduct(wr, rp)
                            if verdict not in CONDUCT_CLAUSES:
                                failures.append(f"conduct derivation produced unknown value {verdict!r}")
                            reached.add(verdict)
    if reached != set(CONDUCT_CLAUSES):
        failures.append(f"conduct verdict reachability {sorted(reached)} != {sorted(CONDUCT_CLAUSES)}")


def validate_handler_detail(node: dict[str, Any], failures: list[str]) -> None:
    node_id = node["node_id"]
    handler = node["handler"]
    predicate = node["predicate"]
    blob = flattened(predicate)

    if handler in {"answer_sign", "visual_dial_answer"}:
        for key in ("answer_route", "normalization", "accepted"):
            if key not in predicate:
                failures.append(f"{node_id}: {handler} missing predicate.{key}")
        accepted = predicate.get("accepted", [])
        if not isinstance(accepted, list) or not accepted or any(not isinstance(x, str) or not x.strip() for x in accepted):
            failures.append(f"{node_id}: accepted answer list is blank/malformed")
        normalized = [norm_answer(x) for x in accepted if isinstance(x, str)]
        if len(normalized) != len(set(normalized)):
            failures.append(f"{node_id}: accepted answer aliases collide after normalization")

    if handler in {
        "tagged_deposit", "frame_and_tagged_lever", "tagged_container_sort",
        "tagged_item_inspection", "tagged_barrel_sort", "tagged_cache",
        "tagged_item_claim", "exact_tagged_deposit", "tagged_key_console",
        "tagged_station_inspection", "tagged_locker", "tagged_spool_reader",
        "tagged_group_rite", "affidavit_reader", "tagged_release_configuration",
    } and not any(token in blob for token in ("pdc", "v5_artifact_id", "v5_evidence_id", "v5_receipt_id")):
        failures.append(f"{node_id}: tagged handler has no exact PDC identity")

    if handler in {
        "item_frame_overlay", "reflection_alignment", "item_frame_dials",
        "visual_dial_answer", "item_frame_valves", "reflection_frames", "item_frame_lamps",
    } and "rotation" not in blob:
        failures.append(f"{node_id}: frame handler has no exact rotation predicate")

    if handler in {"tagged_container_sort", "tagged_barrel_sort", "container_order", "item_arrangement"}:
        if "slot" not in blob or "all_manifest_items_present_once" not in blob:
            failures.append(f"{node_id}: sort/order handler lacks exact slots or unique-set check")

    if handler in {"bounded_route", "bounded_group_walk"}:
        cells = values_for_key(predicate, "cells")
        if not cells or not isinstance(cells[0], list) or len(cells[0]) < 4:
            failures.append(f"{node_id}: route has fewer than four exact cells")
        else:
            steps = [cell.get("step") for cell in cells[0] if isinstance(cell, dict)]
            if steps != list(range(1, len(steps) + 1)):
                failures.append(f"{node_id}: route steps are not contiguous from 1")

    if handler in {"tagged_group_rite", "protected_choice_markers", "group_presence_bridge"}:
        quorum = predicate.get("quorum")
        if not isinstance(quorum, dict) or not quorum:
            failures.append(f"{node_id}: collective handler lacks exact quorum rules")
        if "disconnect" not in flattened(quorum):
            # Disconnect behavior may be represented in the roster component, but it must exist.
            if "disconnect_grace" not in blob:
                failures.append(f"{node_id}: collective handler lacks reconnect/disconnect rule")


def validate_inventory_capacity(node: dict[str, Any], failures: list[str]) -> None:
    node_id = str(node.get("node_id", "?"))
    for component in node.get("predicate", {}).get("components", []):
        if not isinstance(component, dict):
            continue
        component_id = str(component.get("id", "?"))
        capacity = INVENTORY_CAPACITY.get(str(component.get("block", "")))
        if capacity is None:
            continue
        required_unique = component.get("required_unique_count")
        if isinstance(required_unique, int) and required_unique > capacity:
            failures.append(
                f"{node_id}/{component_id}: requires {required_unique} unique items but "
                f"{component.get('block')} has only {capacity} slots"
            )
        required_counts = component.get("required_counts")
        if isinstance(required_counts, dict):
            total = sum(value for value in required_counts.values() if isinstance(value, int))
            if total > capacity:
                failures.append(
                    f"{node_id}/{component_id}: required material count {total} exceeds "
                    f"{component.get('block')} capacity {capacity}"
                )
        slot_values: list[int] = []
        if isinstance(component.get("slot"), int):
            slot_values.append(component["slot"])
        if isinstance(component.get("allowed_slots"), list):
            slot_values.extend(value for value in component["allowed_slots"] if isinstance(value, int))
        if isinstance(component.get("slots"), dict):
            slot_values.extend(value for value in component["slots"].values() if isinstance(value, int))
        invalid = sorted({slot for slot in slot_values if slot < 0 or slot >= capacity})
        if invalid:
            failures.append(
                f"{node_id}/{component_id}: slot indices {invalid} exceed "
                f"{component.get('block')} range 0..{capacity - 1}"
            )


def validate_map_art(authority: dict[str, Any], failures: list[str]) -> None:
    try:
        manifest = json.loads(MAP_ART.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        failures.append(f"map-art authority is unreadable: {exc}")
        return
    entries = manifest.get("maps")
    if not isinstance(entries, list) or len(entries) != 9:
        failures.append(f"map-art authority must contain exactly nine maps, found {len(entries or [])}")
        return
    by_binding: dict[tuple[str, str], dict[str, Any]] = {}
    ids: list[str] = []
    for entry in entries:
        key = (str(entry.get("nodeId", "")), str(entry.get("componentId", "")))
        if key in by_binding:
            failures.append(f"duplicate map-art binding {key}")
        by_binding[key] = entry
        ids.append(str(entry.get("id", "")))
        path = ROOT / str(entry.get("file", ""))
        if not path.is_file():
            failures.append(f"map-art file is missing for {entry.get('id')}: {path}")
            continue
        data = path.read_bytes()
        digest = hashlib.sha256(data).hexdigest()
        if digest != entry.get("sha256"):
            failures.append(f"map-art hash mismatch for {entry.get('id')}")
        if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
            failures.append(f"map-art {entry.get('id')} is not a PNG")
        else:
            width, height = struct.unpack(">II", data[16:24])
            if (width, height) != (128, 128):
                failures.append(f"map-art {entry.get('id')} is {width}x{height}, not 128x128")
    duplicate_ids = sorted(value for value, count in Counter(ids).items() if count > 1)
    if duplicate_ids or any(not value for value in ids):
        failures.append(f"map-art IDs are blank/duplicated: {duplicate_ids}")

    expected: set[tuple[str, str]] = set()
    for node in authority.get("nodes", []):
        node_id = str(node.get("node_id", ""))
        for component in node.get("predicate", {}).get("components", []):
            if not isinstance(component, dict) or component.get("material") != "FILLED_MAP":
                continue
            component_id = str(component.get("id", ""))
            key = (node_id, component_id)
            expected.add(key)
            entry = by_binding.get(key)
            if entry is None:
                failures.append(f"{node_id}/{component_id}: FILLED_MAP has no exact visual authority")
                continue
            if entry.get("requiredFrameRotation") != component.get("required_rotation"):
                failures.append(f"{node_id}/{component_id}: map-art rotation drifts from predicate")
            evidence_id = (component.get("pdc") or {}).get("v5_evidence_id")
            if evidence_id and entry.get("id") != evidence_id:
                failures.append(f"{node_id}/{component_id}: map-art ID {entry.get('id')} != PDC {evidence_id}")
    if set(by_binding) != expected:
        failures.append(
            f"map-art bindings drift; missing={sorted(expected - set(by_binding))}, "
            f"extra={sorted(set(by_binding) - expected)}"
        )


def validate_critical_predicates(by_id: dict[str, dict[str, Any]], failures: list[str]) -> None:
    for node_id, canonical in CANONICAL_ANSWERS.items():
        accepted = by_id.get(node_id, {}).get("predicate", {}).get("accepted", [])
        if norm_answer(canonical) not in {norm_answer(x) for x in accepted if isinstance(x, str)}:
            failures.append(f"{node_id}: missing canonical accepted answer {canonical!r}")

    for node_id, rotations in EXPECTED_ROTATIONS.items():
        node = by_id.get(node_id)
        if not node:
            continue
        lists = [value for value in values_for_key(node["predicate"], "required_rotations") if isinstance(value, list)]
        if rotations not in lists:
            failures.append(f"{node_id}: missing exact required rotations {rotations}")

    hs06 = flattened(by_id.get("HS06", {}))
    for color in ("blue", "copper", "grey", "white"):
        if color not in hs06:
            failures.append(f"HS06: route missing {color}")
    if "crouch_step 4" not in hs06 and '"crouch_step": 4' not in json.dumps(by_id.get("HS06", {})):
        failures.append("HS06: exact crouch at step 4 missing")

    lc06 = flattened(by_id.get("LC06", {}))
    for value in ("civic", "geothermal", "east", "second", "orientation_key"):
        if value not in lc06:
            failures.append(f"LC06: missing model value {value}")

    rp02 = by_id.get("RP02", {})
    rp02_blob = flattened(rp02)
    exact_affidavits = {
        "affidavit_vaun", "affidavit_mara", "affidavit_sella",
        "affidavit_orin", "affidavit_brann", "affidavit_iss",
    }
    for artifact in sorted(exact_affidavits | {"cistern_seal", "system_key", "protocol_bridge"}):
        if artifact not in rp02_blob:
            failures.append(f"RP02: missing exact release deposit {artifact}")
    for forbidden in ("orientation_key", "survey_seal", "breach_plate", "filter_cartridge", "deep_access_plate", "witness_spool"):
        if forbidden in rp02_blob:
            failures.append(f"RP02: non-casebook artifact incorrectly required: {forbidden}")
    if "required_empty" not in rp02_blob or "av eryn" in rp02_blob:
        failures.append("RP02: exact empty Averyn slot missing")

    hs07_blob = flattened(by_id.get("HS07", {}))
    exact_hs_flags = ["v5_hs02_installed", "v5_hs03_lamps", "v5_hs04_pressure", "v5_hs05_dials", "v5_hs06_passage"]
    for flag in exact_hs_flags:
        if flag not in hs07_blob:
            failures.append(f"HS07: missing required subsystem flag {flag}")
    if "breach_plate" in hs07_blob:
        failures.append("HS07: casebook does not require a second physical Breach Plate insert")

    wr05_blob = flattened(by_id.get("WR05", {}))
    for branch in ("condemn", "understand", "free"):
        if branch not in wr05_blob:
            failures.append(f"WR05: missing branch {branch}")
    if "100%" not in wr05_blob or "tie" not in wr05_blob or "disconnect" not in wr05_blob:
        failures.append("WR05: quorum/tie/disconnect behavior is not exact")

    rp06_blob = flattened(by_id.get("RP06", {}))
    for name_branch in ("publish", "release_unnamed"):
        if name_branch not in rp06_blob:
            failures.append(f"RP06: ending matrix missing name treatment {name_branch}")
    for wren_branch in ("condemn", "understand", "free"):
        if wren_branch not in rp06_blob:
            failures.append(f"RP06: ending matrix missing Wren judgment {wren_branch}")
    for conduct in ("solo", "unanimous", "divided", "persistent"):
        if conduct not in rp06_blob:
            failures.append(f"RP06: conduct clause enum missing {conduct}")
    if "six_combinations" not in rp06_blob and "ending_matrix" not in rp06_blob:
        failures.append("RP06: no explicit six-combination Wren/name ending matrix")
    for verdict, clause in CONDUCT_CLAUSES.items():
        if norm_answer(clause) not in norm_answer(" ".join(strings(by_id.get("RP06", {})))):
            failures.append(f"RP06: exact WORLD-BIBLE {verdict} conduct clause missing")

    rp05_blob = flattened(by_id.get("RP05", {}))
    if "/obs finale arm [15-600 seconds]" not in " ".join(strings(by_id.get("RP05", {}))):
        failures.append("RP05: exact no-branch arm command is missing")
    for field in ("wren-outcome", "name-treatment", "conduct-verdict", "cancel-cutoff-at", "committed-at"):
        if field not in rp05_blob:
            failures.append(f"RP05: durable schema-2 field missing: {field}")


def main() -> int:
    failures: list[str] = []
    try:
        authority = json.loads(PREDICATES.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"FAIL: {PREDICATES.relative_to(ROOT)}: {exc}")
        return 1

    runtime_rows = read_csv(RUNTIME)
    node_rows = {row["node_id"]: row for row in read_csv(NODES)}
    artifact_rows = read_csv(ARTIFACTS)
    book_authority = json.loads(BOOKS.read_text(encoding="utf-8"))
    book_ids = {book["id"] for book in book_authority.get("books", [])}

    expected_rows = [row for row in runtime_rows if row.get("owner") in OWNERS]
    expected = {row["node_id"]: row for row in expected_rows}
    if len(expected) != len(expected_rows):
        failures.append("runtime binding has duplicate physical node_id")

    physical = authority.get("nodes")
    if not isinstance(physical, list):
        failures.append("authority.nodes is not an array")
        physical = []
    ids = [node.get("node_id") for node in physical if isinstance(node, dict)]
    duplicates = sorted(node_id for node_id, count in Counter(ids).items() if count > 1)
    if duplicates:
        failures.append(f"predicate node duplicates: {duplicates}")
    by_id = {node["node_id"]: node for node in physical if isinstance(node, dict) and isinstance(node.get("node_id"), str)}

    missing = sorted(set(expected) - set(by_id))
    extra = sorted(set(by_id) - set(expected))
    if missing:
        failures.append(f"predicate coverage missing runtime nodes: {missing}")
    if extra:
        failures.append(f"predicate coverage has non-physical extras: {extra}")

    owner_counts = Counter(node.get("owner") for node in physical if isinstance(node, dict))
    if dict(owner_counts) != EXPECTED_COUNTS:
        failures.append(f"owner counts {dict(owner_counts)} != {EXPECTED_COUNTS}")

    earned_artifacts: dict[str, set[str]] = {}
    known_artifacts: set[str] = set()
    for row in artifact_rows:
        artifact_id = row.get("artifact_id", "").strip()
        node_id = row.get("earned_node", "").strip()
        if artifact_id:
            known_artifacts.add(artifact_id)
            earned_artifacts.setdefault(node_id, set()).add(artifact_id)

    for node_id in sorted(set(expected) & set(by_id)):
        node = by_id[node_id]
        runtime = expected[node_id]
        manifest = node_rows.get(node_id)
        where = f"nodes[{node_id}]"
        if not manifest:
            failures.append(f"{node_id}: missing ARG-V5-NODE-MANIFEST row")
            continue

        missing_keys = REQUIRED_NODE_KEYS - set(node)
        if missing_keys:
            failures.append(f"{node_id}: missing node keys {sorted(missing_keys)}")
            continue
        for field in ("owner", "handler", "site_id", "completion_flag"):
            if node.get(field) != runtime.get(field):
                failures.append(f"{node_id}: {field}={node.get(field)!r} != runtime {runtime.get(field)!r}")
        if node.get("prerequisites") != split_flags(manifest.get("prerequisites", "")):
            failures.append(
                f"{node_id}: prerequisites {node.get('prerequisites')!r} != node manifest "
                f"{split_flags(manifest.get('prerequisites', ''))!r}"
            )

        expected_profile = "finale_local_primary" if runtime["owner"] == "plugin_finale" else "minecraft_local_primary"
        if node.get("durability_profile") != expected_profile:
            failures.append(f"{node_id}: durability profile must be {expected_profile}")

        predicate = node.get("predicate")
        if not isinstance(predicate, dict):
            failures.append(f"{node_id}: predicate is not an object")
            continue
        missing_predicate = REQUIRED_PREDICATE_KEYS - set(predicate)
        if missing_predicate:
            failures.append(f"{node_id}: missing predicate keys {sorted(missing_predicate)}")
        require_text(predicate, "kind", f"{where}.predicate", failures, 5)
        require_text(predicate, "evaluation_trigger", f"{where}.predicate", failures, 12)
        require_text(predicate, "commit", f"{where}.predicate", failures, 20)
        if not isinstance(predicate.get("components"), list) or not predicate.get("components"):
            failures.append(f"{node_id}: predicate.components is blank")
        if not isinstance(predicate.get("all_of"), list) or not predicate.get("all_of"):
            failures.append(f"{node_id}: predicate.all_of is blank")
        elif any(not isinstance(condition, dict) or not condition.get("op") for condition in predicate["all_of"]):
            failures.append(f"{node_id}: every predicate condition needs an explicit op")

        predicate_text = flattened(predicate)
        for token in GENERIC_TOKENS:
            if token in predicate_text:
                failures.append(f"{node_id}: generic/placeholder predicate token {token!r}")
        if predicate.get("kind") in {"interaction", "site_interaction", "touch", "click"}:
            failures.append(f"{node_id}: generic predicate kind {predicate.get('kind')!r}")

        wrong = node.get("wrong_input")
        if not isinstance(wrong, dict) or REQUIRED_WRONG_KEYS - set(wrong):
            failures.append(f"{node_id}: wrong_input lacks policy/feedback/state_effect")
        else:
            require_text(wrong, "policy", f"{where}.wrong_input", failures, 4)
            require_text(wrong, "feedback", f"{where}.wrong_input", failures, 12)

        repair = node.get("reset_repair_recovery")
        if not isinstance(repair, dict) or REQUIRED_REPAIR_KEYS - set(repair):
            failures.append(f"{node_id}: reset/repair/recovery contract incomplete")
        else:
            for key in REQUIRED_REPAIR_KEYS:
                require_text(repair, key, f"{where}.reset_repair_recovery", failures, 4)

        concurrency = node.get("concurrency_replay")
        if not isinstance(concurrency, dict) or REQUIRED_CONCURRENCY_KEYS - set(concurrency):
            failures.append(f"{node_id}: concurrency/replay contract incomplete")
        else:
            if concurrency.get("replay_policy") != runtime.get("replay_policy"):
                failures.append(
                    f"{node_id}: replay {concurrency.get('replay_policy')!r} != runtime "
                    f"{runtime.get('replay_policy')!r}"
                )
            for key in REQUIRED_CONCURRENCY_KEYS:
                require_text(concurrency, key, f"{where}.concurrency_replay", failures, 3)

        reward = node.get("reward")
        if not isinstance(reward, dict):
            failures.append(f"{node_id}: reward is not an object")
        else:
            artifact_ids = reward.get("artifact_ids")
            if not isinstance(artifact_ids, list):
                failures.append(f"{node_id}: reward.artifact_ids must be an array")
                artifact_ids = []
            if set(artifact_ids) != earned_artifacts.get(node_id, set()):
                failures.append(
                    f"{node_id}: reward artifact IDs {sorted(set(artifact_ids))} != artifact manifest "
                    f"{sorted(earned_artifacts.get(node_id, set()))}"
                )
            unknown = sorted(set(artifact_ids) - known_artifacts)
            if unknown:
                failures.append(f"{node_id}: reward references unknown artifacts {unknown}")
            require_text(reward, "description", f"{where}.reward", failures, 3)
            require_text(reward, "delivery", f"{where}.reward", failures, 4)

        for book_id in values_for_key(node, "v5_book_id"):
            if not isinstance(book_id, str) or book_id not in book_ids:
                failures.append(f"{node_id}: unknown canonical v5_book_id {book_id!r}")

        validate_handler_detail(node, failures)
        validate_inventory_capacity(node, failures)

    validate_critical_predicates(by_id, failures)
    validate_conduct_totality(authority, failures)
    validate_map_art(authority, failures)

    if failures:
        print(f"FAIL: {len(failures)} V5 physical-predicate issue(s)")
        for failure in failures:
            print(f" - {failure}")
        return 1

    print(
        "PASS: 60 executable V5 physical predicates "
        "(plugin=51, plugin_unlit=7, plugin_finale=2); runtime/node/artifact/book parity and "
        "inventory capacities exact; no blank or generic touch-to-solve contracts; conduct verdict "
        "total across all four reachable values"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
