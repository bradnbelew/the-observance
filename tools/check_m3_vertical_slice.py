#!/usr/bin/env python3
"""Static/security gate for the M3 coarse adjacency and exact private slice."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import sys

from sim_m3_vertical_slice import Model, run_simulation


ROOT = Path(__file__).resolve().parents[1]
M3 = ROOT / "design" / "m3"
M2 = ROOT / "design" / "m2"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def canonical_package_bytes(path: Path) -> bytes:
    """Hash committed text authority as LF even on a core.autocrlf checkout."""
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


def line_cells(start: list[int], end: list[int]) -> list[tuple[int, int, int]]:
    distance = max(abs(end[i] - start[i]) for i in range(3))
    if distance == 0:
        return [tuple(start)]
    return [tuple(round(start[i] + (end[i] - start[i]) * step / distance) for i in range(3))
            for step in range(distance + 1)]


def check_master(master: dict) -> None:
    require(master["coordinate_system"]["transforms_forbidden"], "master plan permits transforms")
    require(master["coordinate_system"]["constructor_nudges_forbidden"], "master plan permits nudges")
    require(not master["scope"]["build_authority"], "coarse reservations became build geometry")
    require(master["scope"]["only_exact_buildable_area"] == "P4_PRIVATE_SLICE", "slice scope widened")
    districts = master["district_reservations"]
    require(len(districts) == 9, "expected exact P4-P12 district reservations")
    for index, left in enumerate(districts):
        for right in districts[index + 1:]:
            require(not overlaps(left["bounds"], right["bounds"]),
                    f"district ownership overlap: {left['district_id']} / {right['district_id']}")
    ordered = master["ordered_progression"]
    require(ordered == [district["district_id"] for district in districts], "district order drift")
    adjacency = master["public_adjacency"]
    require(len(adjacency) == len(ordered) - 1, "ordered adjacency edge count drift")
    require([(edge["from"], edge["to"]) for edge in adjacency]
            == list(zip(ordered, ordered[1:])), "public adjacency no longer follows P4-P12")
    contracts = master["global_contracts"]
    for key in ("one_post_prologue_minecraft_runtime", "any_subset_progress",
                "survival_inventory_retained", "protected_regions_not_inventory_escrow",
                "local_physical_truth_primary", "all_routes_survive_restart",
                "exactly_three_deliberate_ambiguities", "universal_averyn_release"):
        require(contracts[key] is True, f"master invariant disabled: {key}")
    require(contracts["private_slice_player_facing"] is False, "private slice became player-facing")


def check_m2_binding(slice_authority: dict) -> None:
    chain = load(M2 / "predicate-authority-chain.json")
    versions = {version["raw_sha256"]: version for version in chain["versions"]}
    historical = "37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b"
    canonical = "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a"
    semantic = "d2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6"
    require(slice_authority["manifest_version"] == "2.0.0-m2", "manifest binding drift")
    require(slice_authority["predicate_raw_sha256"] == canonical, "slice does not bind LF authority")
    require(slice_authority["historical_rollback_raw_sha256"] == historical, "rollback hash drift")
    require(slice_authority["predicate_semantic_sha256"] == semantic, "semantic hash drift")
    require(versions[historical]["semantic_sha256"] == semantic
            and versions[canonical]["semantic_sha256"] == semantic, "M2 predicate chain changed")

    findings = {row["finding_id"]: row for row in load(M2 / "generated" / "finding-manifest.json")["findings"]}
    observations = {row["observation_id"]: row for row in load(
        M2 / "generated" / "observation-manifest.json")["observations"]}
    for row in slice_authority["investigation"]["findings"]:
        finding_id = row["finding_id"]
        require(finding_id in findings and findings[finding_id]["arc_id"] == "P4",
                f"slice finding is not routed by M2: {finding_id}")
        require(row["observation_id"] in observations, f"missing M2 observation: {row['observation_id']}")
        require(row["minimum_independent_sources"] >= observations[row["observation_id"]]["minimum_independent_sources"],
                f"slice weakens source minimum: {finding_id}")
        require(findings[finding_id]["attendance_is_not_a_predicate"], f"attendance predicate regressed: {finding_id}")
        require(findings[finding_id]["elapsed_time_is_not_a_predicate"], f"time predicate regressed: {finding_id}")


def check_exact_slice(authority: dict) -> None:
    require(authority["status"] == "exact-private-review-authority-not-player-facing", "slice status drift")
    require(authority["coordinate_system"]["legacy_transform"] is None, "slice uses legacy transform")
    kinds = [space["kind"] for space in authority["spaces"]]
    require(kinds.count("hallway") == 1, "slice must contain exactly one public hallway")
    require(kinds.count("ordinary_room") == 1, "slice must contain exactly one ordinary room")
    require(len([authority["gate"]]) == 1, "slice must contain exactly one gate")
    require(authority["descent"]["reversible"] and authority["descent"]["max_step_delta"] == 1,
            "descent is not reversible/step-safe")

    envelope = authority["envelope"]
    owned_cells: dict[tuple[int, int, int], str] = {}
    for prop in authority["composition"]:
        require(prop["cells"], f"empty authored composition: {prop['prop_id']}")
        for cell in prop["cells"]:
            require(in_envelope(cell, envelope), f"composition escapes envelope: {prop['prop_id']} {cell}")
            key = tuple(cell)
            require(key not in owned_cells, f"composition overlap: {prop['prop_id']} / {owned_cells.get(key)} {cell}")
            owned_cells[key] = prop["prop_id"]
    standing = {tuple(row["cell"]): row["cell_id"] for row in authority["standing_cells"]}
    require(len(standing) == len(authority["standing_cells"]), "duplicate standing cell")
    for cell, cell_id in standing.items():
        require(list(cell) and in_envelope(list(cell), envelope), f"standing cell escapes: {cell_id}")
        require(cell not in owned_cells, f"furniture consumes standing cell: {cell_id}")

    room = next(space for space in authority["spaces"] if space["space_id"] == "INTAKE_COPY_ROOM")
    b = room["bounds"]
    interior_area = (b["max_x"] - b["min_x"] - 1) * (b["max_z"] - b["min_z"] - 1)
    blocking = {tuple(cell) for prop in authority["composition"]
                if prop["space_id"] == room["space_id"] and prop["kind"] == "blocking_furniture"
                for cell in prop["cells"]}
    footprint = len(blocking) / interior_area
    require(0.15 <= footprint <= 0.40, f"ordinary-room blocking footprint unhealthy: {footprint:.1%}")

    model = Model.load()
    model.replay()
    for sightline in authority["sightlines"]:
        if not sightline["required"]:
            continue
        cells = line_cells(sightline["from"], sightline["to"])
        for cell in cells[1:-1]:
            require(not model.is_solid(cell), f"required sightline blocked: {sightline['sightline_id']} at {cell}")

    investigation = authority["investigation"]
    require([row["finding_id"] for row in investigation["findings"]]
            == ["P4.F1", "P4.F2", "P4.F3", "P4.F4", "P4.F5"], "P4 finding set drift")
    synthesis = investigation["findings"][-1]
    require(synthesis["prerequisites"] == ["P4.F1", "P4.F2", "P4.F3", "P4.F4"],
            "gate synthesis no longer requires all P4 evidence")
    require(investigation["any_subset"] and investigation["attendance_is_not_a_predicate"]
            and investigation["elapsed_time_is_not_a_predicate"], "any-subset contract weakened")
    require(not investigation["wrong_theory_changes_state"] and not investigation["replay_duplicates_reward"],
            "wrong/replay behavior mutates state")
    require(set(investigation["catch_up"]["session_brief_fields"])
            == {"what_changed", "supporting_evidence", "remaining_dispute", "changed_places", "new_search_space"},
            "catch-up brief contract drift")

    gate = authority["gate"]
    require(not gate["closed_traversal"] and gate["open_traversal"] and not gate["close_after_open"],
            "gate is not monotonic closed/open")
    require(not gate["remote_may_open"], "remote state may open physical gate")
    persistence = authority["persistence"]
    for key in ("persist_before_presentation", "restart_rederive_from_journal",
                "same_key_same_bytes_idempotent", "same_key_different_bytes_rejected",
                "remote_projection_cannot_open_gate"):
        require(persistence[key] is True, f"local-primary contract disabled: {key}")

    protection = authority["protection"]
    require(protection["survival_inventory_retained"] and not protection["inventory_escrow"],
            "survival-region policy drift")
    require({"block_break", "block_place", "entity_damage", "container_mutation",
             "teleport_bypass", "gate_bypass"}.issubset(protection["default_denies"]),
            "protected region no longer denies a bypass class")

    watcher = authority["watcher_asymmetry"]
    require(watcher["risk_class"] == "A2" and watcher["approval_required"]
            and not watcher["automatic"] and not watcher["required_for_progress"],
            "Watcher moment violates A2/accessibility boundary")
    expected = watcher["payload_sha256"]
    actual = hashlib.sha256(json.dumps(watcher["authored_payload"], sort_keys=True,
                                       separators=(",", ":"), ensure_ascii=False).encode("utf-8")).hexdigest()
    require(expected == actual, "Watcher exact payload hash drift")

    gaps = authority["external_gaps"]
    require(gaps["disposable_paper_clone"].endswith("PAPER-DISPOSABLE-RECEIPT.json"),
            "Paper receipt is not routed")
    require(gaps["fresh_build_receipt"] and gaps["restart_reaudit_receipt"],
            "structural Paper receipts are missing")
    for key in ("non_op_survival_region_receipt", "two_client_asymmetry_receipt",
                "solo_accessibility_receipt", "brad_visual_approval"):
        require(gaps[key] is None, f"client/human receipt fabricated: {key}")


def check_package_manifest(manifest: dict) -> None:
    require(manifest["receipt_scope"].startswith("offline authority plus disposable Paper structural"),
            "package scope drift")
    canonical = bytearray()
    for row in manifest["files"]:
        path = ROOT / row["path"]
        require(path.is_file(), f"package file missing: {row['path']}")
        actual = hashlib.sha256(canonical_package_bytes(path)).hexdigest()
        require(actual == row["sha256"], f"package file hash drift: {row['path']}")
        canonical.extend((row["path"] + "\n" + actual + "\n").encode("utf-8"))
    require(hashlib.sha256(canonical).hexdigest() == manifest["package_set_sha256"],
            "M3 offline package-set hash drift")
    receipt = load(M3 / "PAPER-DISPOSABLE-RECEIPT.json")
    require(receipt["m4_authority"] == "closed" and receipt["brad_visual_approval"] is None,
            "Paper receipt improperly opens M4/visual approval")
    require(all(receipt["client_receipts"][key] is None for key in (
        "non_op_adventure_survival_inventory", "protected_region_bypass",
        "two_client_asymmetry", "solo_accessibility")), "client receipt fabricated")
    require(manifest["paper_server_jar_sha256"] == receipt["paper"]["jar_sha256"], "Paper JAR drift")
    require(manifest["plugin_jar_sha256"] == receipt["plugin_jar_sha256"], "plugin JAR drift")
    require(manifest["paper_world_tree_sha256"] == receipt["world_tree_sha256"], "world tree drift")
    require(manifest["paper_world_package_sha256"] == receipt["world_package_sha256"], "world package drift")
    require(manifest["brad_visual_approval_receipt"] is None, "visual approval fabricated")


def main() -> None:
    master = load(M3 / "coarse-adjacency-v1.json")
    authority = load(M3 / "vertical-slice-v1.json")
    check_master(master)
    check_m2_binding(authority)
    check_exact_slice(authority)
    check_package_manifest(load(M3 / "package-manifest.json"))
    receipt = run_simulation()
    print("M3 static/security gate OK — 9 coarse reservations, exact P4 slice, "
          f"closed/open reachability {receipt['closed_visited']}/{receipt['open_visited']}, "
          "M2 custody/replay/protection/approval contracts intact")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, ValueError) as error:
        print(f"M3 static/security gate FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
