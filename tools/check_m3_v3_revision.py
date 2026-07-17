#!/usr/bin/env python3
"""Static authority and implementation gate for the focused M3 v3 revision."""
from __future__ import annotations

import hashlib
import json
from collections import Counter
from pathlib import Path
import re
import subprocess
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
RUNNER = ROOT / "tools" / "run_m3_v3_disposable_paper.py"
VALIDATION_RECEIPT = M3 / "PAPER-V3-DISPOSABLE-RECEIPT.json"
REVIEW_RECEIPT = M3 / "PAPER-V3-REVIEW-SERVER-RECEIPT.json"
FAILED_ATTEMPTS = M3 / "PAPER-V3-FAILED-ATTEMPTS.json"
PACKAGE_MANIFEST = M3 / "PACKAGE-MANIFEST-V3.json"
ACTIVE_REVIEW = M3 / "BRAD-V3-ACTIVE-REVIEW.json"
V3_DECISION = M3 / "BRAD-V3-REVIEW-DECISION.json"
EXPECTED_SLICE_SHA256 = "0181b5566ea49a653b9cc95a650246c52ce670735d6ec2d6e4b1f6b9bc2a7ae5"
PASSING_SOURCE_COMMIT = "ebc731ff3bfc0eb42572246b814ac541811190f2"


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
    require(len(reader_cells) == len(set(reader_cells)), "reader standing cells overlap")
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
    def historical(path: Path) -> str:
        relative = path.relative_to(ROOT).as_posix()
        result = subprocess.run(["git", "show", f"{PASSING_SOURCE_COMMIT}:{relative}"],
                                cwd=ROOT, check=True, capture_output=True)
        return result.stdout.decode("utf-8")

    # V4 legitimately supersedes these runtime source files. Verify the exact passing V3 blobs from
    # their immutable receipt commit rather than forcing current implementation back to rejected V3.
    world = historical(WORLD_SOURCE)
    interaction = historical(INTERACTION_SOURCE)
    state = historical(STATE_SOURCE)
    runtime = historical(RUNTIME_SOURCE)
    runner = RUNNER.read_text(encoding="utf-8")
    required_world_tokens = (
        "expectedBlockData", "checkReader", "checkInvestigationTopology", "checkWaterworks",
        "checkCorridor", "checkImmersiveText", "checkSightline", "unclassified floating furnishing",
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


def check_paper_receipts() -> None:
    validation = load(VALIDATION_RECEIPT)
    review = load(REVIEW_RECEIPT)
    failed = load(FAILED_ATTEMPTS)
    require(validation["schema_version"] == "3.0.0-m3-paper-receipt"
            and validation["authority_id"] == "observance-p4-private-slice-v3"
            and validation["source_git_commit"] == PASSING_SOURCE_COMMIT,
            "v3 validation receipt identity drift")
    require(validation["slice_authority_sha256"] == EXPECTED_SLICE_SHA256
            and validation["paper"] == {
                "version": "1.21.11", "build": 132,
                "jar_sha256": "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba",
            }, "v3 Paper/authority pin drift")
    evidence = validation["evidence"]
    require("findings=0" in evidence["closed_audit"] and "gate_collision=88" in evidence["closed_audit"]
            and "findings=0" in evidence["open_audit"] and "gate_collision=0" in evidence["open_audit"]
            and "findings=0" in evidence["restart_audit"] and "gate_collision=0" in evidence["restart_audit"]
            and evidence["replay"].endswith("receipts=28")
            and evidence["restart_replay"].endswith("receipts=28")
            and len(evidence["observations"]) == 8,
            "v3 closed/open/restart/replay Paper evidence incomplete")
    require(all("M3_SECURITY_PASS" in evidence[key]
                for key in ("security_closed", "security_open", "restart_security")),
            "v3 security readback incomplete")
    require(validation["journal_state"] == "present" and validation["journal_sha256"]
            and validation["world_tree_sha256"] == "ea62fc688dd4d9ed2f0675b1754039e8d6f87846624b050610757b73d5fc90e3"
            and validation["world_package_sha256"] == "7a4cf0aeb985bae625a75b9f39e267bcc57f06fdbc019911adb5ee313e147ff7",
            "v3 validation journal/world/package hash drift")
    require(review["schema_version"] == "3.0.0-m3-review-server-receipt"
            and review["authority_id"] == "observance-p4-private-slice-v3"
            and review["source_git_commit"] == PASSING_SOURCE_COMMIT
            and review["journal_state"] == "absent_pristine_review_target",
            "v3 pristine review receipt identity/state drift")
    require("findings=0" in review["evidence"]["closed_audit"]
            and "gate_collision=88" in review["evidence"]["closed_audit"]
            and "M3_SECURITY_PASS" in review["evidence"]["security"]
            and review["world_tree_sha256"] == "a7740d2ca618a91b1a6c87555b0e16a9a76aa8c553f2ded26622d051b93c5227"
            and review["world_package_sha256"] == "06efb8142ddfae814888f5b3608b1ab948033c3c398447591569fb268ee8a84b",
            "v3 pristine review Paper/world/package evidence drift")
    for receipt in (validation, review):
        require(receipt["brad_visual_approval"] is None and receipt["m4_authority"] == "closed"
                and receipt["server_configuration"]["bind"] == "127.0.0.1"
                and receipt["server_configuration"]["default_op"] is False
                and receipt["server_configuration"]["gamemode"] == "adventure",
                "v3 receipt fabricates approval or weakens localhost/non-op containment")
    attempts = failed["attempts"]
    require(failed["schema_version"] == "1.0.0-m3-v3-failed-attempts"
            and len(attempts) == 3 and all(row["target_reused"] is False for row in attempts),
            "v3 failed-target provenance incomplete")
    require([row["remediation_commit"] for row in attempts] == [
        "46ba9806e0def207afea477ff8da1268a5f093df",
        "5025d735d5e74cdb9d8c7e2a01da5e9adea4c664",
        PASSING_SOURCE_COMMIT,
    ] and failed["current_passing_target"] == validation["target_id"],
            "v3 failed-attempt remediation lineage drift")
    require(failed["brad_visual_approval"] is None and failed["m4_authority"] == "closed",
            "failed-attempt receipt opens approval/M4")


def check_package_manifest() -> None:
    manifest = load(PACKAGE_MANIFEST)
    require(manifest["schema_version"] == "4.0.0-m3"
            and manifest["package_id"] == "m3-private-slice-authored-revision-v3",
            "v3 package manifest identity drift")
    canonical = bytearray()
    manifest_history = subprocess.run(
        ["git", "log", "-1", "--format=%H", "--", "design/m3/PACKAGE-MANIFEST-V3.json"],
        cwd=ROOT, check=True, text=True, capture_output=True).stdout.strip()
    require(bool(manifest_history), "v3 package manifest has no Git provenance")
    for row in manifest["files"]:
        path = ROOT / row["path"]
        require(path.is_file(), f"v3 package file missing: {row['path']}")
        actual = hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
        if actual != row["sha256"]:
            result = subprocess.run(["git", "show", f"{manifest_history}:{row['path']}"],
                                    cwd=ROOT, capture_output=True)
            require(result.returncode == 0, f"historical v3 package blob missing: {row['path']}")
            historical = hashlib.sha256(result.stdout.replace(b"\r\n", b"\n")).hexdigest()
            require(historical == row["sha256"], f"historical v3 package hash drift: {row['path']}")
        canonical.extend((row["path"] + "\n" + row["sha256"] + "\n").encode("utf-8"))
    require(hashlib.sha256(canonical).hexdigest() == manifest["package_set_sha256"],
            "v3 package-set hash drift")
    require(manifest["passing_source_git_commit"] == PASSING_SOURCE_COMMIT
            and manifest["paper_server_jar_sha256"] == load(VALIDATION_RECEIPT)["paper"]["jar_sha256"]
            and manifest["plugin_jar_sha256"] == load(VALIDATION_RECEIPT)["plugin_jar_sha256"]
            and manifest["brad_visual_approval_receipt"] is None
            and manifest["brad_visual_status"] == "pending_v3_re_review"
            and manifest["m4_authority"] == "closed",
            "v3 package provenance/approval gate drift")


def check_active_review() -> None:
    review = load(ACTIVE_REVIEW)
    require(review["schema_version"] == "1.0.0-m3-brad-v3-active-review"
            and review["review_status"] == "complete_not_approved_revision_required"
            and review["brad_visual_approval"] is None and review["m4_authority"] == "closed",
            "complete v3 review approval/M4 status drift")
    require(review["live_server_directive"] == "disconnect_confirmed_clean_save_flush_stop_complete"
            and review["implementation_state"] == "v4_blocked_only_until_complete_rejection_checkpoint_is_committed"
            and review["revision_authority"] == "blocked_until_complete_v3_rejection_and_cross_phase_standard_are_committed_as_a_clean_checkpoint"
            and review["review_server_stop_receipt"] == "design/m3/PAPER-V3-REVIEW-STOP-RECEIPT.json",
            "complete v3 clean-stop/checkpoint gate weakened")
    require(len(review["binding_finding_verbatim"]) == 4
            and "not acceptable as final legibility" in review["binding_interpretation"],
            "active v3 discoverability finding incomplete")
    checks = review["required_acceptance_checks"]
    require([row["check_id"] for row in checks] == [
        "M3.V3R.DISTINCT_FILING_AFFORDANCE",
        "M3.V3R.DIEGETIC_FILING_INSTRUCTION",
        "M3.V3R.COLD_PLAYER_OBJECTIVE_COMPREHENSION",
    ] and all(row["current_v3_compliance"] is False and row["predicate"] for row in checks),
            "active v3 future acceptance checks weakened")
    decision = load(V3_DECISION)
    require(decision["schema_version"] == "1.0.0-m3-brad-v3-decision"
            and decision["decision"] == "not_approved_revision_required"
            and decision["brad_visual_approval"] is None and decision["m4_authority"] == "closed",
            "decisive v3 rejection or M4 gate drift")
    require(decision["review_status"] == "complete_not_approved_disconnect_and_clean_stop_receipted"
            and decision["live_server_directive"] == "disconnect_confirmed_clean_save_flush_stop_complete"
            and "without reading, comparing, understanding, or deducing" in decision["decisive_binding_finding"],
            "v3 mechanic-success/checklist-rejection finding drift")
    require(len(decision["final_visual_and_player_facing_direction"]) == 7
            and decision["cross_phase_experience_authority"]["quality_not_quota"] is True
            and decision["v4_design_demonstration"]["human_cold_read_required"] is True,
            "v3 final visual/cross-phase direction incomplete")
    next_checks = decision["required_next_revision_checks"]
    require(len(next_checks) == 9 and all(row["current_v3_compliance"] is False for row in next_checks)
            and {row["check_id"] for row in next_checks} >= {
                "M3.VNEXT.FOUR_CONTENT_DEPENDENT_CONCLUSIONS",
                "M3.VNEXT.CONTENT_DEPENDENT_SYNTHESIS",
                "M3.VNEXT.NAIVE_CLICK_THROUGH_NEGATIVE",
                "M3.VNEXT.BLIND_AND_BRUTE_FORCE_RESISTANCE",
                "M3.VNEXT.COLD_PLAYER_COMPREHENSION",
            }, "v3 next-revision reasoning/negative-test authority incomplete")


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
    check_paper_receipts()
    check_package_manifest()
    check_active_review()
    print("M3 v3 structural/Paper receipts PASS — V3 NOT APPROVED: checklist bypass rejected; clean review stop receipted; v4 requires clean checkpoint; M4 closed")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, OSError, ValueError) as error:
        print(f"M3 v3 revision gate FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
