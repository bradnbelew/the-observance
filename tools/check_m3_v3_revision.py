#!/usr/bin/env python3
"""Static authority and implementation gate for the focused M3 v3 revision."""
from __future__ import annotations

import hashlib
import json
from collections import Counter
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
M3 = ROOT / "design" / "m3"
AUTHORITY = M3 / "vertical-slice-v3.json"
DECISION = M3 / "BRAD-V2-REVIEW-DECISION.json"
STOP_RECEIPT = M3 / "PAPER-V2-REVIEW-STOP-RECEIPT.json"
WORLD_SOURCE = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "m3runtime" / "PrivateSliceWorld.java"
INTERACTION_SOURCE = WORLD_SOURCE.with_name("PrivateSliceInteractionListener.java")
STATE_SOURCE = WORLD_SOURCE.with_name("PrivateSliceState.java")
RUNTIME_SOURCE = WORLD_SOURCE.with_name("PrivateSliceReviewRuntime.java")
RUNNER = ROOT / "tools" / "run_m3_disposable_paper.py"
EXPECTED_SLICE_SHA256 = "3f7c7924b6905f9f58dc43332f9dfb6ae19cfed853759a5e63f278de2438c320"


def require(value: bool, message: str) -> None:
    if not value:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def canonical_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def cell(row: dict, key: str = "cell") -> tuple[int, int, int]:
    value = tuple(row[key])
    require(len(value) == 3 and all(isinstance(axis, int) for axis in value), f"invalid {key}: {value}")
    return value


def faces(surface: tuple[int, int, int], reader: tuple[int, int, int], facing: str) -> bool:
    sx, _, sz = surface
    rx, _, rz = reader
    return {
        "north": rz < sz,
        "south": rz > sz,
        "east": rx > sx,
        "west": rx < sx,
    }.get(facing, False)


def box_cells(bounds: dict) -> set[tuple[int, int, int]]:
    return {
        (x, y, z)
        for x in range(bounds["min_x"], bounds["max_x"] + 1)
        for y in range(bounds["min_y"], bounds["max_y"] + 1)
        for z in range(bounds["min_z"], bounds["max_z"] + 1)
    }


def check_identity(authority: dict, decision: dict, stop: dict) -> None:
    require(authority["schema_version"] == "3.0.0-m3", "v3 schema drift")
    require(authority["authority_id"] == "observance-p4-private-slice-v3", "v3 authority identity drift")
    require(authority["status"] == "focused-authored-revision-not-player-facing", "v3 status drift")
    require(authority["m4_authority"] == "closed" and authority["brad_visual_approval"] is None,
            "M4 opened or Brad approval fabricated")
    envelope = authority["envelope"]
    calculated = ((envelope["max_x"] - envelope["min_x"] + 1)
                  * (envelope["max_y"] - envelope["min_y"] + 1)
                  * (envelope["max_z"] - envelope["min_z"] + 1))
    require(envelope["cells"] == calculated == 248745, "bounded v3 envelope drift")
    require(decision["decision"] == "not_approved_revision_required"
            and decision["brad_visual_approval"] is None and decision["m4_authority"] == "closed",
            "binding v2 rejection lost")
    require(stop["result"] == "clean_save_flush_stop_verified"
            and stop["post_stop_readback"]["pid_28448_alive"] is False
            and stop["post_stop_readback"]["port_25580_listener_count"] == 0,
            "v2 disposable review server stop authority missing")
    require(canonical_sha256(AUTHORITY) == EXPECTED_SLICE_SHA256, "v3 authority hash drift")


def check_gate(authority: dict) -> None:
    gate = authority["preservation_baseline"]["retain_gate"]
    require(gate == {"min_x": -5, "max_x": 5, "min_y": -20, "max_y": -13, "z": 89,
                     "width": 11, "height": 8, "closed_collision_cells": 88,
                     "open_collision_cells": 0}, "v2 gate baseline regressed")


def check_investigation(authority: dict) -> None:
    investigation = authority["investigation"]
    evidence = investigation["evidence_books"]
    submissions = investigation["submissions"]
    briefing = investigation["briefing"]
    require(len(evidence) == 8 and Counter(row["finding_id"] for row in evidence)
            == Counter({"P4.F1": 2, "P4.F2": 2, "P4.F3": 2, "P4.F4": 2}),
            "investigation must expose exactly two physical books per finding")
    require(len(submissions) == 6, "filing/readback topology drift")
    sources = {row["source_id"] for row in evidence}
    require(sources == {"drainage_map", "cart_wear", "material_join_civic", "survey_revisions",
                        "population_board", "ration_ledger", "descent_heat_marks", "founding_minutes"},
            "exact authored evidence source set drift")
    surfaces = evidence + submissions + [briefing]
    surface_cells = [cell(row) for row in surfaces]
    reader_cells = [cell(row, "reader_standing_cell") for row in surfaces]
    require(len(surface_cells) == len(set(surface_cells)), "lectern footprints overlap")
    require(not set(surface_cells).intersection(reader_cells), "reader cell overlaps a lectern footprint")
    for row in evidence + submissions:
        require(faces(cell(row), cell(row, "reader_standing_cell"), row["facing"]),
                f"{row['surface_id']} faces away from its reader")
    require("native lectern UI" in investigation["physical_rule"]
            and "never emitted to chat" in investigation["physical_rule"],
            "evidence-in-book/no-chat rule weakened")


def check_signage(authority: dict) -> None:
    signs = authority["signage"]
    require(len(signs) == 4, "retained sign count drift")
    banned = ("this is the entrance", "this is the exit", "same public route", "no side exit")
    for row in signs:
        surface = cell(row)
        reader = cell(row, "reader_standing_cell")
        support = cell(row, "support_cell")
        match = re.fullmatch(r"minecraft:oak_wall_sign\[facing=(north|south|east|west),waterlogged=false\]",
                             row["block_data"])
        require(match is not None, f"{row['surface_id']} lacks exact wall-sign BlockData")
        facing = match.group(1)
        require(faces(surface, reader, facing), f"{row['surface_id']} is reverse-facing")
        sx, sy, sz = surface
        support_by_facing = {"north": (sx, sy, sz + 1), "south": (sx, sy, sz - 1),
                             "east": (sx - 1, sy, sz), "west": (sx + 1, sy, sz)}
        require(support == support_by_facing[facing], f"{row['surface_id']} wall attachment drift")
        eye_delta = abs((sy + 0.5) - (reader[1] + 1.62))
        require(eye_delta <= 1.25, f"{row['surface_id']} exceeds player-eye band")
        purpose = row["purpose"].lower()
        require(purpose and not any(phrase in purpose for phrase in banned),
                f"{row['surface_id']} retains navigation handholding")


def check_waterworks(authority: dict) -> None:
    works = authority["waterworks"]
    components = works["ordered_components"]
    expected = [("INLET_TROUGH", 8), ("SETTLING_BASIN", 30),
                ("GAUGING_FLUME", 16), ("GRATED_SUMP", 8)]
    require([(row["component_id"], row["water_cells"]) for row in components] == expected,
            "ordered hydraulic components or exact capacities drift")
    sets = []
    for row in components:
        cells = box_cells(row["bounds"])
        require(len(cells) == row["water_cells"] and row["purpose"].strip(),
                f"hydraulic component invalid: {row['component_id']}")
        sets.append(cells)
    require(sum(map(len, sets)) == works["total_water_cells"] == 62, "water total drift")
    require(all(not sets[i].intersection(sets[j]) for i in range(len(sets)) for j in range(i + 1, len(sets))),
            "hydraulic component cells overlap")
    for left, right in zip(sets, sets[1:]):
        require(any(abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2]) == 1
                    for a in left for b in right), "hydraulic flow order is physically disconnected")
    require(works["flow_order"] == [name for name, _ in expected]
            and works["orphan_components_allowed"] is False, "orphan/flow-order policy drift")


def check_circulation(authority: dict) -> None:
    circulation = authority["circulation"]
    for key, material in (("public_office_route", "POLISHED_ANDESITE"),
                          ("staff_record_route", "CUT_COPPER")):
        route = circulation[key]
        start, end = route["start"], route["end"]
        require(start[1:] == end[1:] and start[0] < end[0], f"{key} is not a straight aligned corridor")
        require(route["clear_width"] == 3 and route["threshold_material"] == material,
                f"{key} width/material drift")
    require(circulation["diagonal_or_unexplained_jogs_allowed"] is False
            and circulation["instructional_navigation_signs_allowed"] is False,
            "awkward corridor or handholding signage re-authorized")


def check_required_predicates(authority: dict, decision: dict) -> None:
    required = {row["check_id"] for row in decision["v3_required_machine_checks"]}
    require(set(authority["mandatory_checks"]) == required and len(required) == 12,
            "v3 mandatory machine-check authority incomplete")


def check_implementation() -> None:
    world = WORLD_SOURCE.read_text(encoding="utf-8")
    interaction = INTERACTION_SOURCE.read_text(encoding="utf-8")
    state = STATE_SOURCE.read_text(encoding="utf-8")
    runtime = RUNTIME_SOURCE.read_text(encoding="utf-8")
    runner = RUNNER.read_text(encoding="utf-8")
    required_world_tokens = (
        "expectedBlockData", "checkReader", "checkInvestigationTopology", "checkWaterworks",
        "checkCorridor", "checkImmersiveText", "unclassified floating furnishing",
        "minecraft:chiseled_bookshelf[facing=west]", "minecraft:lectern[facing=",
        "minecraft:lantern[hanging=true,waterlogged=false]", "GATE_CLOSED_COLLISION_CELLS = 88",
        'checkCorridor(findings, 12, 27, 69, 71', 'checkCorridor(findings, 12, 27, 80, 82',
    )
    for token in required_world_tokens:
        require(token in world, f"v3 Paper world check absent: {token}")
    require("player.sendMessage(surface.body" not in interaction
            and "player.sendMessage(surface.title" not in interaction,
            "evidence title/body is still emitted to player chat")
    require("referenceAt" in interaction and "evidence.findingId()" in interaction
            and "sendActionBar" in interaction, "native book interaction/provenance path incomplete")
    for source in ("drainage_map", "cart_wear", "material_join_civic", "survey_revisions",
                   "population_board", "ration_ledger", "descent_heat_marks", "founding_minutes"):
        require(f'"{source}"' in state, f"state machine lost exact source {source}")
    require("m3-private-slice-v3.journal" in runtime and "observance-p4-private-slice-v3" in runtime,
            "v3 runtime identity/journal drift")
    require("vertical-slice-v3.json" in runner and "m3-private-slice-v3-world.zip" in runner
            and "m3-private-slice-v3.journal" in runner and EXPECTED_SLICE_SHA256 in runner,
            "disposable Paper runner is not pinned to v3 authority")


def main() -> None:
    authority, decision, stop = load(AUTHORITY), load(DECISION), load(STOP_RECEIPT)
    check_identity(authority, decision, stop)
    check_gate(authority)
    check_investigation(authority)
    check_signage(authority)
    check_waterworks(authority)
    check_circulation(authority)
    check_required_predicates(authority, decision)
    check_implementation()
    print("M3 v3 revision gate PASS — exact books/signs/supports/waterworks/corridors/gate authority verified; M4 closed; Brad approval null")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, OSError, ValueError) as error:
        print(f"M3 v3 revision gate FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
