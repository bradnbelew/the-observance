#!/usr/bin/env python3
"""Validate the authored M3 v4 authority, player-facing inventory, runtime, and receipts."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
M3 = ROOT / "design" / "m3"
AUTHORITY = M3 / "vertical-slice-v4.json"
INVENTORY = M3 / "PLAYER-FACING-INVENTORY-V4.json"
GLOBAL_STANDARD = ROOT / "design" / "handoff" / "PLAYER-FACING-EXPERIENCE-STANDARD.json"
WORLD = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "m3runtime" / "PrivateSliceWorld.java"
STATE = WORLD.with_name("PrivateSliceState.java")
INTERACTION = WORLD.with_name("PrivateSliceInteractionListener.java")
RUNTIME = WORLD.with_name("PrivateSliceReviewRuntime.java")
SELF_TEST = ROOT / "plugin" / "src" / "test" / "java" / "com" / "observance" / "watcher" / "m3runtime" / "PrivateSliceStateSelfTest.java"
PLUGIN_YML = ROOT / "plugin" / "src" / "main" / "resources" / "plugin.yml"
EXPECTED_AUTHORITY_SHA256 = "444926db844dfd5e06bd131a3c941a23b85a575c1b56ce09673f690aa5d88b3f"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def canonical_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def check_authority(authority: dict) -> None:
    require(authority["schema_version"] == "4.0.0-m3"
            and authority["authority_id"] == "observance-p4-private-slice-v4",
            "v4 authority identity drift")
    require(canonical_sha256(AUTHORITY) == EXPECTED_AUTHORITY_SHA256, "v4 authority hash drift")
    require(authority["binding_review_decision"] == "design/m3/BRAD-V3-REVIEW-DECISION.json"
            and authority["cross_phase_player_facing_authority"]
                == "design/handoff/PLAYER-FACING-EXPERIENCE-STANDARD.json",
            "v4 does not route through binding rejection/player-facing authority")
    require(authority["m4_authority"] == "closed" and authority["brad_visual_approval"] is None,
            "v4 authority opens M4 or fabricates approval")
    envelope = authority["envelope"]
    cells = ((envelope["max_x"] - envelope["min_x"] + 1)
             * (envelope["max_y"] - envelope["min_y"] + 1)
             * (envelope["max_z"] - envelope["min_z"] + 1))
    require(cells == envelope["cells"] == 248745, "bounded v4 envelope drift")
    preserve = authority["preservation_baseline"]
    require(all(value is True for key, value in preserve.items() if key != "v3_gate"),
            "v3/canon/state/accessibility preservation weakened")
    require(preserve["v3_gate"]["closed_collision_cells"] == 88
            and preserve["v3_gate"]["open_collision_cells"] == 0,
            "controlled gate baseline regressed")

    rooms = authority["room_program"]
    require([room["room_id"] for room in rooms] == [
        "MOUTH_CARRIAGE_HALL", "INTAKE_WORKS_HALL", "RECORD_COPY_OFFICE",
    ] and all(room["institutional_job"] and room["scale_justification"] for room in rooms),
            "function-first room program incomplete")
    investigation = authority["investigation"]
    evidence = investigation["evidence"]
    require(len(evidence) == 8 and investigation["lectern_count"] == 2,
            "evidence or lectern-density authority drift")
    counts = Counter(row["finding_id"] for row in evidence)
    require(counts == Counter({"P4.F1": 2, "P4.F2": 2, "P4.F3": 2, "P4.F4": 2}),
            "four paired evidence lanes drift")
    require(len({tuple(row["cell"]) for row in evidence}) == 8
            and len({tuple(row["reader_standing_cell"]) for row in evidence}) == 8,
            "evidence or reader cells overlap")
    for row in evidence:
        x, y, z = row["cell"]
        require(envelope["min_x"] <= x <= envelope["max_x"]
                and envelope["min_y"] <= y <= envelope["max_y"]
                and envelope["min_z"] <= z <= envelope["max_z"],
                f"evidence outside bounded envelope: {row['surface_id']}")
        require(tuple(row["cell"]) != tuple(row["reader_standing_cell"]),
                f"surface occupies its reader cell: {row['surface_id']}")
    presentations = Counter(row["presentation"] for row in evidence)
    require(presentations == Counter({"native_book": 4, "visible_environmental_record": 4}),
            "natural evidence-medium plan drift")
    exact = investigation["exact_conclusions"]
    require(set(exact) == {"P4.F1", "P4.F2", "P4.F3", "P4.F4", "P4.F5"}
            and len(set(exact.values())) == 5,
            "content-dependent conclusion authority incomplete")
    policy = investigation["incorrect_report_policy"]
    require(policy == {
        "lane_specific_feedback": False,
        "draft_cleared_after_refusal": True,
        "durable_max_refusals": 3,
        "rolling_window_seconds": 300,
        "finding_progress_on_refusal": 0,
        "solution_bearing_journal_payload": False,
    }, "blind/brute-force refusal policy weakened")
    required_checks = set(authority["mandatory_checks"])
    require(len(required_checks) == 16
            and {"M3.V4.CONTENT_DEPENDENT_FOUR_CLAUSE_REPORT",
                 "M3.V4.CONTENT_DEPENDENT_SYNTHESIS",
                 "M3.V4.NAIVE_CLICK_THROUGH_NEGATIVE",
                 "M3.V4.DURABLE_BRUTE_FORCE_THROTTLE",
                 "M3.V4.COLD_PLAYER_OBJECTIVE_INTERACTION_COMPREHENSION"} <= required_checks,
            "v4 mandatory gate inventory incomplete")


def check_inventory(authority: dict) -> None:
    inventory = load(INVENTORY)
    require(inventory["slice_authority"] == "design/m3/vertical-slice-v4.json"
            and inventory["cross_phase_authority"]
                == "design/handoff/PLAYER-FACING-EXPERIENCE-STANDARD.json"
            and inventory["quality_not_quota"] is True,
            "v4 inventory authority/quality routing drift")
    require(inventory["brad_visual_approval"] is None and inventory["m4_authority"] == "closed",
            "v4 inventory fabricates approval")
    room_ids = {room["room_id"] for room in inventory["rooms"]}
    require(room_ids == {room["room_id"] for room in authority["room_program"]},
            "room inventory/program mismatch")
    expected_artifacts = {row["surface_id"] for row in authority["investigation"]["evidence"]} | {
        "INTAKE_EXAMINER_DOCKET", "EXAMINER_FINDINGS_LEDGER", "FIELD_ARCHIVE_READBACK",
    }
    artifacts = inventory["artifacts"]
    require({row["artifact_id"] for row in artifacts} == expected_artifacts
            and len(artifacts) == len(expected_artifacts),
            "player-facing artifact inventory incomplete or duplicated")
    hashes: set[str] = set()
    voices: set[str] = set()
    formats: set[str] = set()
    banned = ("ultra-mysterious", "solve the puzzle", "submit here", "right-click",
              "crouch-right-click", "this is the entrance", "this is the exit")
    for artifact in artifacts:
        digest = hashlib.sha256(artifact["authored_text"].encode()).hexdigest()
        require(digest == artifact["text_sha256"],
                f"artifact hash drift: {artifact['artifact_id']}")
        require(digest not in hashes, f"duplicate artifact copy hash: {artifact['artifact_id']}")
        hashes.add(digest)
        voices.add(artifact["voice_owner"])
        formats.add(artifact["physical_format"])
        lowered = artifact["authored_text"].lower()
        require(not any(phrase in lowered for phrase in banned),
                f"meta/purple copy in {artifact['artifact_id']}")
        require(artifact["medium_fit"]["plausible"] is True
                and artifact["human_editor_review"]["status"] in {"required", "passed"},
                f"medium/editor review missing: {artifact['artifact_id']}")
    expected_voices = {
        "Mara Venn", "Neri Holt", "Orris Pell", "Toma Rusk",
        "Eda Sorn, with Mara Venn's marginalia", "Lio Marr", "Sela Orr",
        "Iven Quill", "Intake record office",
    }
    require(voices == expected_voices and len(formats) == len(artifacts),
            "declared v4 situated voices/formats do not match the exact authored inventory")
    require(inventory["cold_read_gate"]["observer_must_not_receive_solution_key"] is True
            and inventory["cold_read_gate"]["brad_approval_not_implied"] is True,
            "cold-read or Brad-approval boundary weakened")


def check_implementation(authority: dict) -> None:
    world = WORLD.read_text(encoding="utf-8")
    state = STATE.read_text(encoding="utf-8")
    interaction = INTERACTION.read_text(encoding="utf-8")
    runtime = RUNTIME.read_text(encoding="utf-8")
    self_test = SELF_TEST.read_text(encoding="utf-8")
    plugin_yml = PLUGIN_YML.read_text(encoding="utf-8")
    for token in (
        "authority=observance-p4-private-slice-v4", "m3-private-slice-v4.journal",
        "naiveNegative", "bruteNegative", "reportCorrect", "synthesisCorrect",
        "counter_proximity_only", "observancefile",
    ):
        require(token in runtime, f"v4 runtime path missing: {token}")
    require("observancefile:" in plugin_yml and "observance.m3.file" in plugin_yml,
            "non-op in-world filing command is not registered")
    for finding, conclusion in authority["investigation"]["exact_conclusions"].items():
        require(f'"{finding}"' in state and f'"{conclusion}"' in state,
                f"state machine missing exact conclusion {finding}")
        require(conclusion in world, f"findings ledger missing exact option {conclusion}")
    for source in (row["source_id"] for row in authority["investigation"]["evidence"]):
        require(f'"{source}"' in state and f'"{source}"' in world,
                f"runtime/source custody missing {source}")
    for token in (
        "report_refused", "REFUSAL_WINDOW_SECONDS = 300", "MAX_REFUSALS_PER_WINDOW = 3",
        "draftsByContributor.remove", "lodgeReport", "lodgeSynthesis",
        "finding conclusion is not the authored content-dependent conclusion",
    ):
        require(token in state, f"content/brute-force state gate missing: {token}")
    require("sendMessage(" not in interaction and "isSneaking" not in interaction
            and "openEvidenceBook" in interaction and "openFilingLedger" in interaction,
            "v4 interaction regressed to chat/crouch or lost physical affordances")
    for token in (
        "exactly two purpose-specific lecterns", "exactFormats", "choicePage",
        "nearFilingLedger", "MOUTH_LOADED_RUT", "INTAKE_CLERK_DESKS",
        "EXAMINER_FILING_COUNTER", "BINDING_ISLAND", "GATE_CLOSED_COLLISION_CELLS = 88",
        "minecraft:copper_grate[waterlogged=false]", "checkSightline", "checkWaterworks",
        "unclassified floating furnishing",
    ):
        require(token in world, f"v4 world composition/block-state gate missing: {token}")
    for token in (
        "naive click-through advances nothing", "bounded brute force cannot advance a finding",
        "wrong synthesis leaves gate closed", "restart re-derives committed gate and synthesis",
        "v4 evidence and filing feedback must not use chat",
    ):
        require(token in self_test, f"v4 model-level negative/restart test missing: {token}")


def check_receipts() -> None:
    required = [
        M3 / "PAPER-V4-DISPOSABLE-RECEIPT.json",
        M3 / "PAPER-V4-REVIEW-SERVER-RECEIPT.json",
        M3 / "PACKAGE-MANIFEST-V4.json",
        M3 / "V4-BLOCK-STATE-VISUAL-AUDIT.json",
        M3 / "V4-COLD-READ-PREFLIGHT.json",
        M3 / "V4-ROUTED-REGRESSION-RECEIPT.json",
    ]
    missing = [path.relative_to(ROOT).as_posix() for path in required if not path.is_file()]
    require(not missing, f"v4 live/package/human preflight receipts missing: {missing}")
    validation, review, manifest, visual, cold, routed = map(load, required)
    failed = load(M3 / "PAPER-V4-FAILED-ATTEMPTS.json")
    for receipt in (validation, review):
        require(receipt["authority_id"] == "observance-p4-private-slice-v4"
                and receipt["slice_authority_sha256"] == EXPECTED_AUTHORITY_SHA256
                and receipt["paper"]["version"] == "1.21.11"
                and receipt["paper"]["build"] == 132
                and receipt["server_configuration"]["bind"] == "127.0.0.1"
                and receipt["server_configuration"]["gamemode"] == "adventure"
                and receipt["server_configuration"]["default_op"] is False
                and receipt["brad_visual_approval"] is None
                and receipt["m4_authority"] == "closed",
                "v4 Paper receipt identity/containment/approval drift")
    evidence = validation["evidence"]
    for key in ("closed_audit", "open_audit", "restart_audit"):
        require("M3_AUDIT PASS" in evidence[key] and "findings=0" in evidence[key],
                f"v4 Paper {key} missing")
    require("gate_collision=88" in evidence["closed_audit"]
            and "gate_collision=0" in evidence["open_audit"]
            and "gate_collision=0" in evidence["restart_audit"]
            and "M3_NAIVE_NEGATIVE_PASS" in evidence["naive_negative"]
            and "findings=0 gate=closed" in evidence["naive_negative"]
            and "M3_BRUTE_NEGATIVE_PASS" in evidence["brute_negative"]
            and "throttled=true" in evidence["brute_negative"]
            and "M3_REPORT_CORRECT_PASS" in evidence["report_correct"]
            and "gate=closed" in evidence["report_correct"]
            and "M3_SYNTHESIS_CORRECT_PASS" in evidence["synthesis_correct"]
            and "gate=open" in evidence["synthesis_correct"],
            "v4 content-dependent/negative/gate Paper chain incomplete")
    require(review["journal_state"] == "absent_pristine_review_target"
            and "M3_AUDIT PASS" in review["evidence"]["closed_audit"]
            and "gate_collision=88" in review["evidence"]["closed_audit"],
            "v4 pristine review target drift")
    require(manifest["schema_version"] == "5.0.0-m3"
            and manifest["package_id"] == "m3-private-slice-content-dependent-revision-v4"
            and manifest["authority_id"] == "observance-p4-private-slice-v4"
            and manifest["slice_authority_sha256"] == EXPECTED_AUTHORITY_SHA256
            and manifest["brad_visual_approval"] is None and manifest["m4_authority"] == "closed",
            "v4 package manifest identity/approval drift")
    canonical = bytearray()
    for row in manifest["files"]:
        path = ROOT / row["path"]
        require(path.is_file(), f"v4 package file missing: {row['path']}")
        require(canonical_sha256(path) == row["sha256"],
                f"v4 package file hash drift: {row['path']}")
        canonical.extend((row["path"] + "\n" + row["sha256"] + "\n").encode("utf-8"))
    require(hashlib.sha256(canonical).hexdigest() == manifest["package_set_sha256"],
            "v4 package-set hash drift")
    require(manifest["passing_source_git_commit"] == validation["source_git_commit"]
            and manifest["paper_server_jar_sha256"] == validation["paper"]["jar_sha256"]
            and manifest["plugin_jar_sha256"] == validation["plugin_jar_sha256"]
            and manifest["paper_world_tree_sha256"] == validation["world_tree_sha256"]
            and manifest["paper_world_package_sha256"] == validation["world_package_sha256"]
            and manifest["review_world_tree_sha256"] == review["world_tree_sha256"]
            and manifest["review_world_package_sha256"] == review["world_package_sha256"],
            "v4 package/JAR/world provenance drift")
    require(failed["schema_version"] == "1.0.0-m3-v4-failed-attempts"
            and len(failed["attempts"]) == 2
            and all(row["target_reused"] is False for row in failed["attempts"])
            and [row["remediation_commit"] for row in failed["attempts"]] == [
                "754b4ae1af19818345e06b00f0aacb198e912435",
                "233b16947a513a97c661f12980906c2a99f4301f",
            ] and failed["current_passing_target"] == validation["target_id"]
            and failed["brad_visual_approval"] is None
            and failed["m4_authority"] == "closed",
            "v4 failed-target provenance/remediation drift")
    require(visual["result"] == "passed_internal_exact_composition_audit"
            and visual["unclassified_floating_blocks"] == 0
            and visual["blocked_reader_or_route_cells"] == 0
            and visual["lectern_count"] == 2,
            "v4 block-state/composition audit incomplete")
    require(cold["result"] == "passed_internal_preflight_not_brad_approval"
            and cold["brad_visual_approval"] is None and cold["m4_authority"] == "closed",
            "v4 cold-read preflight overclaims or is incomplete")
    require(routed["aggregate_status"]
                == "honestly_blocked_at_secret_dependent_discord_resolvecheck"
            and len(routed["missing_environment"]) == 6
            and routed["production_credentials_loaded"] is False
            and routed["production_mutated"] is False
            and routed["brad_visual_approval"] is None
            and routed["m4_authority"] == "closed",
            "v4 routed regression boundary or approval gate drift")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-only", action="store_true")
    args = parser.parse_args()
    authority = load(AUTHORITY)
    check_authority(authority)
    check_inventory(authority)
    check_implementation(authority)
    if not args.source_only:
        check_receipts()
    print("M3 V4 REVISION: PASS (" + ("source/inventory" if args.source_only else "source + Paper/restart/negative/package/preflight receipts")
          + "; Brad approval null; M4 closed)")


if __name__ == "__main__":
    main()
