#!/usr/bin/env python3
"""Static, composition, security, and receipt gate for the authored M3 v2 private slice."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys

from sim_m3_vertical_slice import Model, run_simulation


ROOT = Path(__file__).resolve().parents[1]
M3 = ROOT / "design" / "m3"
M2 = ROOT / "design" / "m2"
AUTHORITY = M3 / "vertical-slice-v2.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def canonical_package_bytes(path: Path) -> bytes:
    return path.read_bytes().replace(b"\r\n", b"\n")


def overlaps(a: dict, b: dict) -> bool:
    return not (a["max_x"] < b["min_x"] or b["max_x"] < a["min_x"]
                or a["max_y"] < b["min_y"] or b["max_y"] < a["min_y"]
                or a["max_z"] < b["min_z"] or b["max_z"] < a["min_z"])


def in_envelope(cell: list[int], envelope: dict) -> bool:
    x, y, z = cell
    return envelope["min_x"] <= x <= envelope["max_x"] \
        and envelope["min_y"] <= y <= envelope["max_y"] \
        and envelope["min_z"] <= z <= envelope["max_z"]


def volume_count(bounds: dict) -> int:
    return (bounds["max_x"] - bounds["min_x"] + 1) \
        * (bounds["max_y"] - bounds["min_y"] + 1) \
        * (bounds["max_z"] - bounds["min_z"] + 1)


def line_cells(start: list[int], end: list[int]) -> list[tuple[int, int, int]]:
    distance = max(abs(end[i] - start[i]) for i in range(3))
    return [tuple(round(start[i] + (end[i] - start[i]) * (step / distance)) for i in range(3))
            for step in range(distance + 1)] if distance else [tuple(start)]


def check_master(master: dict, authority: dict) -> None:
    require(master["coordinate_system"]["transforms_forbidden"], "legacy transforms re-enabled")
    districts = master["district_reservations"]
    require(len(districts) == 9 and len({row["district_id"] for row in districts}) == 9,
            "coarse reservation count/id drift")
    for i, left in enumerate(districts):
        for right in districts[i + 1:]:
            require(not overlaps(left["bounds"], right["bounds"]),
                    f"coarse reservations overlap: {left['district_id']} / {right['district_id']}")
    p4 = districts[0]
    require(p4["bounds"] == authority["envelope"], "P4 coarse and v2 exact envelopes disagree")
    require("vertical-slice-v2.json" in p4["geometry_status"] and "rejected_v1_preserved" in p4["geometry_status"],
            "v2 authority or rejected v1 provenance lost")
    require(all(row["geometry_status"] == "reserved_only" for row in districts[1:]),
            "M4+ district geometry opened before approval")
    require(master["global_contracts"]["one_post_prologue_minecraft_runtime"], "one-runtime contract lost")


def check_m2_binding(authority: dict) -> None:
    chain = load(M2 / "predicate-authority-chain.json")
    versions = {row["raw_sha256"]: row for row in chain["versions"]}
    raw = authority["predicate_raw_sha256"]
    historical = authority["historical_rollback_raw_sha256"]
    semantic = authority["predicate_semantic_sha256"]
    require(raw == "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a",
            "v2 predicate raw authority drift")
    require(versions[raw]["semantic_sha256"] == semantic == versions[historical]["semantic_sha256"],
            "predicate semantic/rollback chain drift")


def check_palette_scale_water(authority: dict) -> None:
    e = authority["envelope"]
    require(volume_count({"min_x": e["min_x"], "max_x": e["max_x"], "min_y": e["min_y"],
                          "max_y": e["max_y"], "min_z": e["min_z"], "max_z": e["max_z"]})
            == authority["scale_receipt"]["envelope_cells"] == 248745, "envelope cell receipt drift")
    require(authority["scale_receipt"]["intake_interior_floor_cells"] == 651
            and authority["scale_receipt"]["copy_office_interior_floor_cells"] == 273,
            "authored civic/copy-office scale drift")
    palette = authority["palette_authority"]
    require(len(palette) == 5, "palette zones lost")
    materials = {material for row in palette for key in ("structural", "decorative") for material in row[key]}
    require(len(materials) >= 18 and {"TUFF_BRICKS", "DEEPSLATE_BRICKS", "WEATHERED_CUT_COPPER",
            "CHISELED_BOOKSHELF", "COPPER_GRATE"}.issubset(materials), "intentional palette variation weakened")

    water = authority["waterworks"]
    require(sum(volume_count(row["bounds"]) for row in water["water_volumes"])
            == water["total_water_blocks"] == 58, "waterworks exact volume/count drift")
    require("settling basin" in water["function"] and "gauging flume" in water["function"]
            and "drains" in water["function"], "water feature no longer reads as functional infrastructure")
    require(not water["flood_escape"] and water["standing_route_width"] >= 2, "waterworks safety weakened")


def check_composition(authority: dict) -> None:
    spaces = {row["space_id"]: row for row in authority["spaces"]}
    require(sum(row["kind"] == "hallway" for row in spaces.values()) == 1, "public hallway count drift")
    require(sum(row["kind"] == "ordinary_room" for row in spaces.values()) == 1, "ordinary room count drift")
    require("clerks copy" in spaces["INTAKE_COPY_ROOM"]["ordinary_job"], "copy-office ordinary job lost")
    require(len(authority["doors"]) == 3 and {row["threshold_label"] for row in authority["doors"]}
            == {"PUBLIC COPY OFFICE", "STAFF RECORD CARTS", "STAFF COPYING ROUTE"},
            "public/staff threshold semantics drift")

    owned: dict[tuple[int, int, int], str] = {}
    blocking_floor: set[tuple[int, int, int]] = set()
    for prop in authority["composition"]:
        height = prop.get("vertical_height", 1)
        require(len(prop["floor_cells"]) * height == prop["count"], f"composition count drift: {prop['prop_id']}")
        for x, y, z in prop["floor_cells"]:
            blocking_floor.add((x, y, z))
            for dy in range(height):
                cell = (x, y + dy, z)
                require(cell not in owned, f"composition overlap: {prop['prop_id']} / {owned.get(cell)} {cell}")
                owned[cell] = prop["prop_id"]
    density = authority["copy_office_density"]
    copy_props = [row for row in authority["composition"] if row["space_id"] == "INTAKE_COPY_ROOM"]
    copy_floor = {tuple(cell) for row in copy_props for cell in row["floor_cells"]}
    require(len(copy_floor) == density["blocking_floor_cells"] == 48, "copy-office blocking count drift")
    require(abs(len(copy_floor) / density["interior_floor_cells"] - density["blocking_footprint_ratio"]) < 0.0001,
            "copy-office density ratio drift")
    require(0.16 <= density["blocking_footprint_ratio"] <= 0.30
            and density["vertical_cabinet_blocks"] == 44 and density["paper_surface_blocks"] >= 20
            and density["light_fixtures"] >= 8, "copy office no longer dense, occupied, and navigable")
    require(density["minimum_public_aisle_width"] >= 3 and density["minimum_staff_route_width"] >= 2,
            "copy-office circulation narrowed")

    evidence = authority["evidence_surfaces"]
    require(len(evidence) == 14 and len({row["surface_id"] for row in evidence}) == 14,
            "authored physical evidence surface count/id drift")
    require(len({tuple(row["cell"]) for row in evidence}) == 14, "evidence surface cell overlap")
    findings = {row["finding_id"]: row for row in authority["investigation"]["findings"]}
    for row in evidence:
        require(row["source_id"] in findings[row["finding_id"]]["sources"],
                f"physical evidence not bound to finding source: {row['surface_id']}")
        require(len(row["body"]) >= 70 and in_envelope(row["cell"], authority["envelope"]),
                f"evidence surface thin/outside envelope: {row['surface_id']}")
    require(len(authority["submission_surfaces"]) == 6
            and {row["finding_id"] for row in authority["submission_surfaces"] if row["finding_id"]}
            == set(findings), "physical filing/readback surfaces incomplete")
    require(len(authority["threshold_signage"]) == 8
            and len({row["sign_id"] for row in authority["threshold_signage"]}) == 8,
            "threshold signage count/id drift")


def check_geometry_and_gate(authority: dict) -> dict:
    model = Model.load()
    model.replay()
    solid_owned = {tuple(row["cell"]) for row in authority["evidence_surfaces"]}
    solid_owned |= {tuple(row["cell"]) for row in authority["submission_surfaces"]}
    for row in authority["standing_cells"]:
        cell = tuple(row["cell"])
        require(in_envelope(row["cell"], authority["envelope"]), f"standing cell outside: {row['cell_id']}")
        require(cell not in solid_owned, f"evidence/submission consumes standing cell: {row['cell_id']}")
    for sightline in authority["sightlines"]:
        if not sightline["required"]:
            continue
        for cell in line_cells(sightline["from"], sightline["to"])[1:-1]:
            require(not model.is_solid(cell), f"required sightline blocked: {sightline['sightline_id']} at {cell}")

    gate = authority["gate"]
    plane = gate["barrier_plane"]
    count = (plane["max_x"] - plane["min_x"] + 1) * (plane["max_y"] - plane["min_y"] + 1)
    require(count == plane["cell_count"] == gate["closed_collision_cells"] == 88,
            "gate is not exact 11x8 full collision plane")
    require(not gate["closed_traversal"] and gate["open_traversal"] and not gate["close_after_open"]
            and not gate["remote_may_open"] and gate["open_collision_cells"] == 0,
            "gate monotonic/collision authority weakened")
    receipt = run_simulation()
    require(receipt == {"closed_visited": 1756, "open_visited": 1787, "gate_delta": 31,
                        "closed_gate_collision_cells": 88, "open_gate_collision_cells": 0,
                        "standing_cells": 24, "evidence_surfaces": 14,
                        "submission_surfaces": 6, "water_blocks": 58},
            f"v2 reachability/composition receipt drift: {receipt}")
    return receipt


def check_state_security(authority: dict) -> None:
    investigation = authority["investigation"]
    findings = investigation["findings"]
    require([row["finding_id"] for row in findings] == ["P4.F1", "P4.F2", "P4.F3", "P4.F4", "P4.F5"],
            "P4 finding set drift")
    require(findings[-1]["prerequisites"] == ["P4.F1", "P4.F2", "P4.F3", "P4.F4"],
            "P4 synthesis prerequisites weakened")
    require(investigation["any_subset"] and investigation["attendance_is_not_a_predicate"]
            and investigation["elapsed_time_is_not_a_predicate"]
            and not investigation["wrong_theory_changes_state"] and not investigation["replay_duplicates_reward"],
            "any-subset/wrong/replay contract weakened")
    require("right-click authored evidence" in investigation["physical_interaction_contract"],
            "player-facing evidence/submission interaction lost")

    persistence = authority["persistence"]
    for key in ("persist_before_presentation", "restart_rederive_from_journal",
                "same_key_same_bytes_idempotent", "same_key_different_bytes_rejected",
                "remote_projection_cannot_open_gate"):
        require(persistence[key] is True, f"local-primary contract disabled: {key}")
    require({"observation_committed", "finding_committed", "gate_opened", "watcher_approval_recorded",
             "watcher_approval_consumed"}.issubset(persistence["events"]), "physical/state events incomplete")

    protection = authority["protection"]
    require(protection["review_gamemode"] == "ADVENTURE" and protection["non_op_enforced"]
            and protection["survival_inventory_retained"] and not protection["inventory_escrow"],
            "review mode/survival inventory policy drift")
    require({"block_break", "block_place", "entity_damage", "container_mutation", "teleport_bypass",
             "gate_bypass"}.issubset(protection["default_denies"]), "protected region bypass class lost")

    watcher = authority["watcher_asymmetry"]
    require(watcher["risk_class"] == "A2" and watcher["approval_required"] and not watcher["automatic"]
            and not watcher["required_for_progress"] and "consumes the approval once" in watcher["runtime_contract"],
            "Watcher A2/exact-once/accessibility boundary weakened")
    actual = hashlib.sha256(json.dumps(watcher["authored_payload"], sort_keys=True, separators=(",", ":"),
                                       ensure_ascii=False).encode("utf-8")).hexdigest()
    require(actual == watcher["payload_sha256"], "Watcher exact payload hash drift")

    revision = authority["revision_acceptance"]
    require(all(revision.values()), "one or more binding Brad revision criteria is not represented")
    require(authority["external_gaps"]["brad_visual_approval"] is None
            and authority["external_gaps"]["brad_visual_decision"] == "pending_re_review_after_revision",
            "Brad approval fabricated or rejected decision not advanced honestly")


def check_package_manifest(manifest: dict) -> None:
    require(manifest["schema_version"] == "3.0.0-m3" and manifest["package_id"].endswith("v2"),
            "v2 package manifest identity drift")
    canonical = bytearray()
    for row in manifest["files"]:
        path = ROOT / row["path"]
        require(path.is_file(), f"package file missing: {row['path']}")
        actual = hashlib.sha256(canonical_package_bytes(path)).hexdigest()
        require(actual == row["sha256"], f"package file hash drift: {row['path']}")
        canonical.extend((row["path"] + "\n" + actual + "\n").encode("utf-8"))
    require(hashlib.sha256(canonical).hexdigest() == manifest["package_set_sha256"], "v2 package-set hash drift")
    receipt = load(M3 / "PAPER-DISPOSABLE-RECEIPT.json")
    require(receipt["schema_version"] == "2.0.0-m3-paper-receipt" and receipt["m4_authority"] == "closed"
            and receipt["brad_visual_approval"] is None, "Paper receipt identity or M4/visual gate drift")
    require(receipt["brad_visual_status"] == "pending_re_review_after_revision",
            "Paper receipt misstates Brad decision")
    require(manifest["paper_server_jar_sha256"] == receipt["paper"]["jar_sha256"], "Paper JAR drift")
    require(manifest["plugin_jar_sha256"] == receipt["plugin_jar_sha256"], "plugin JAR drift")
    require(manifest["paper_world_tree_sha256"] == receipt["world_tree_sha256"], "world tree drift")
    require(manifest["paper_world_package_sha256"] == receipt["world_package_sha256"], "world package drift")
    require(manifest["brad_visual_approval_receipt"] is None
            and manifest["brad_visual_status"] == "pending_re_review_after_revision",
            "package fabricates Brad approval or loses pending re-review")
    require(manifest["validation_source_git_commit"] == receipt["source_git_commit"],
            "validation source checkpoint drift")
    review = load(M3 / "PAPER-REVIEW-SERVER-RECEIPT.json")
    require(review["schema_version"] == "1.0.0-m3-review-server-receipt"
            and review["mode"] == "prepare-review" and review["m4_authority"] == "closed"
            and review["brad_visual_approval"] is None
            and review["brad_visual_status"] == "pending_re_review_after_revision",
            "pristine review receipt identity or gate drift")
    require(review["journal_sha256"] is None
            and review["journal_state"] == "absent_pristine_review_target",
            "prepared review target is not pristine")
    require(review["paper"] == receipt["paper"]
            and review["plugin_jar_sha256"] == receipt["plugin_jar_sha256"],
            "validation/review Paper or plugin identity mismatch")
    require(manifest["review_source_git_commit"] == review["source_git_commit"]
            and manifest["review_world_tree_sha256"] == review["world_tree_sha256"]
            and manifest["review_world_package_sha256"] == review["world_package_sha256"],
            "prepared review target provenance drift")
    require("findings=0" in review["evidence"]["closed_audit"]
            and "gate_collision=88" in review["evidence"]["security"],
            "prepared review target did not pass closed structural/security audit")


def main(authority_only: bool = False) -> None:
    authority = load(AUTHORITY)
    require(authority["authority_id"] == "observance-p4-private-slice-v2"
            and authority["status"] == "authored-private-review-revision-not-player-facing",
            "v2 authority identity/status drift")
    check_master(load(M3 / "coarse-adjacency-v1.json"), authority)
    check_m2_binding(authority)
    check_palette_scale_water(authority)
    check_composition(authority)
    receipt = check_geometry_and_gate(authority)
    check_state_security(authority)
    if not authority_only:
        check_package_manifest(load(M3 / "package-manifest.json"))
    scope = "authority-only / Paper receipt pending" if authority_only else "complete package"
    print("M3 v2 static/security gate OK (" + scope + ") — palette=5 zones, intake=651 cells, copy=48/273 blocking, "
          f"water=58, evidence=14, submissions=6, gate=88 collision cells, reachability="
          f"{receipt['closed_visited']}/{receipt['open_visited']}, M4 closed")


if __name__ == "__main__":
    try:
        parser = argparse.ArgumentParser()
        parser.add_argument("--authority-only", action="store_true",
                            help="validate exact authored authority/runtime inputs before a new Paper receipt exists")
        main(parser.parse_args().authority_only)
    except (AssertionError, KeyError, ValueError) as error:
        print(f"M3 v2 static/security gate FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
