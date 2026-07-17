#!/usr/bin/env python3
"""Validate the cross-phase player-facing authority and any implementation inventories."""
from __future__ import annotations

import json
import hashlib
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
HUMAN = ROOT / "design" / "handoff" / "PLAYER-FACING-EXPERIENCE-STANDARD.md"
MACHINE = ROOT / "design" / "handoff" / "PLAYER-FACING-EXPERIENCE-STANDARD.json"
V3_DECISION = ROOT / "design" / "m3" / "BRAD-V3-REVIEW-DECISION.json"
V3_STOP = ROOT / "design" / "m3" / "PAPER-V3-REVIEW-STOP-RECEIPT.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def present(value: object) -> bool:
    return value is not None and value != "" and value != [] and value != {}


def validate_authority() -> dict:
    data = load(MACHINE)
    require(data["schema_version"] == "1.0.0-cross-phase-player-facing",
            "player-facing authority schema drift")
    require(data["authority_id"] == "observance-cross-phase-player-facing-experience"
            and data["status"] == "binding", "player-facing authority identity drift")
    scope = data["scope"]
    require(scope["phases"] == ["M3-v4-and-later", "M4", "M5"],
            "cross-phase scope drift")
    require(scope["locked_canon_preserved"] is True
            and scope["approved_evidence_model_preserved"] is True,
            "canon/evidence preservation weakened")
    require(scope["m4_authority"] == "closed" and scope["brad_visual_approval"] is None,
            "M4 or Brad-approval gate weakened")

    formats = set(data["copy_standard"]["format_examples_not_quotas"])
    required_formats = {
        "conversation between people", "workplace note", "personal letter", "official record",
        "ledger", "marginalia", "transcription", "notice", "diagram",
        "physical environmental evidence", "genuine lore book", "written book",
    }
    require(formats == required_formats, "Brad's artifact-format examples were lost or altered")
    forbidden = set(data["copy_standard"]["forbidden_register"])
    require({"ultra-mysterious", "purple", "cryptic-for-cryptic's-sake",
             "overtly puzzle sounding", "monotonous exposition", "repeated docket prose"}
            <= forbidden, "binding copy prohibitions weakened")

    room = data["room_standard"]
    require(all(room.values()), "room-composition standard contains a disabled predicate")
    require(data["v4_demonstration"]["quality_not_quota"] is True,
            "quality-not-quota guard lost")
    require("cannot convert" in data["human_quality_rule"]
            and "human cold-read acceptance remains mandatory" in data["human_quality_rule"],
            "human quality rule weakened")

    expected_checks = {
        "CROSS.PF.ROOM_JOB", "CROSS.PF.SCALE_FUNCTION", "CROSS.PF.LECTERN_RESTRAINT",
        "CROSS.PF.REPEATED_FURNITURE_RATIONALE", "CROSS.PF.ARTIFACT_FORMAT_PROVENANCE",
        "CROSS.PF.VOICE_DIVERSITY", "CROSS.PF.NO_META_PUZZLE_COPY",
        "CROSS.PF.NO_MONOTONOUS_DOCKET_PROSE", "CROSS.PF.COLD_READ_HUMAN_GATE",
    }
    checks = data["static_checks"]
    require({row["check_id"] for row in checks} == expected_checks
            and len(checks) == len(expected_checks), "static check inventory drift")
    require(len(data["cold_read_criteria"]) == 12
            and any("puzzle chamber or room of lecterns" in row
                    for row in data["cold_read_criteria"])
            and any("naive touching" in row for row in data["cold_read_criteria"]),
            "human cold-read criteria incomplete")

    human = HUMAN.read_text(encoding="utf-8")
    for phrase in (
        "BINDING CROSS-PHASE AUTHORITY", "Function justifies scale", "Lecterns are exceptional",
        "Write people, not ARG voice", "Use the medium truthfully", "not a quota",
        "writing quality is never", "M3 v4 demonstration obligation",
        "NOT APPROVED / REVISION REQUIRED", "M4 remains",
    ):
        require(phrase in human, f"human player-facing authority missing: {phrase}")
    return data


def validate_v3_decision() -> None:
    decision = load(V3_DECISION)
    require(decision["review_status"] == "complete_not_approved_disconnect_and_clean_stop_receipted"
            and decision["decision"] == "not_approved_revision_required",
            "v3 final rejection state drift")
    require(decision["brad_visual_approval"] is None and decision["m4_authority"] == "closed",
            "v3 Brad/M4 gate weakened")
    require(len(decision["final_visual_and_player_facing_direction"]) == 7,
            "v3 final player-facing direction incomplete")
    cross = decision["cross_phase_experience_authority"]
    require(cross["human"] == HUMAN.relative_to(ROOT).as_posix()
            and cross["machine"] == MACHINE.relative_to(ROOT).as_posix()
            and cross["quality_not_quota"] is True,
            "v3 decision does not route to the cross-phase standard")
    require(decision["v4_design_demonstration"]["locked_canon_preserved"] is True
            and decision["v4_design_demonstration"]["approved_evidence_model_preserved"] is True,
            "v4 demonstration weakens canon/evidence authority")

    stop = load(V3_STOP)
    require(stop["target_id"] == "m3-v3-brad-review-ebc731f-20260717-a"
            and stop["review_process_id"] == 31192 and stop["port"] == 25582,
            "v3 review-stop identity drift")
    require(stop["post_stop_readback"] == {
        "pid_31192_alive": False, "port_25582_listener_count": 0,
    } and stop["result"] == "clean_save_flush_stop_verified",
            "v3 review server is not receipted as cleanly stopped")
    require(stop["production_mutated"] is False
            and stop["brad_visual_approval"] is None and stop["m4_authority"] == "closed",
            "v3 stop receipt overclaims production or approval")


def validate_inventory(path: Path, authority: dict) -> None:
    data = load(path)
    require(data.get("cross_phase_authority") == MACHINE.relative_to(ROOT).as_posix(),
            f"{path}: missing cross-phase authority routing")
    require(data.get("quality_not_quota") is True,
            f"{path}: quality-not-quota guard missing")
    rooms = data.get("rooms", [])
    artifacts = data.get("artifacts", [])
    require(rooms and artifacts, f"{path}: room and artifact inventories must be non-empty")
    room_fields = authority["required_inventory_fields"]["room"]
    artifact_fields = authority["required_inventory_fields"]["artifact"]
    for room in rooms:
        missing = [field for field in room_fields if not present(room.get(field))]
        require(not missing, f"{path}: room {room.get('room_id')} missing {missing}")
        require(all(present(cluster.get("purpose")) for cluster in room["furniture_clusters"]),
                f"{path}: unpurposed furniture cluster in {room['room_id']}")
        require(all(present(item.get("purpose")) for item in room["lectern_purposes"]),
                f"{path}: unpurposed lectern in {room['room_id']}")
        receipt = room["cold_read_receipt"]
        require(receipt.get("status") in {"required_before_review", "passed"},
                f"{path}: cold-read status must be explicit for {room['room_id']}")
    hashes: list[str] = []
    for artifact in artifacts:
        missing = [field for field in artifact_fields if not present(artifact.get(field))]
        require(not missing, f"{path}: artifact {artifact.get('artifact_id')} missing {missing}")
        require(artifact["human_editor_review"].get("status") in {"required", "passed"},
                f"{path}: human editorial review missing for {artifact['artifact_id']}")
        require(artifact["medium_fit"].get("plausible") is True,
                f"{path}: medium-fit review failed for {artifact['artifact_id']}")
        authored_text = artifact.get("authored_text")
        if authored_text is not None:
            calculated = hashlib.sha256(authored_text.encode("utf-8")).hexdigest()
            require(calculated == artifact["text_sha256"],
                    f"{path}: authored text hash drift for {artifact['artifact_id']}")
        hashes.append(artifact["text_sha256"])
    duplicates = [value for value, count in Counter(hashes).items() if count > 1]
    require(not duplicates, f"{path}: duplicate artifact text hashes masquerade as variety: {duplicates}")
    banned = ("submit here", "this is the entrance", "this is the exit", "solve the puzzle",
              "right-click", "shift-right-click", "crouch-right-click")
    corpus = json.dumps(data, ensure_ascii=False).lower()
    require(not any(term in corpus for term in banned),
            f"{path}: meta-puzzle/tutorial copy detected")


def main() -> None:
    authority = validate_authority()
    validate_v3_decision()
    inventories = sorted((ROOT / "design").glob("**/PLAYER-FACING-INVENTORY*.json"))
    for path in inventories:
        validate_inventory(path, authority)
    print("PLAYER-FACING EXPERIENCE: PASS "
          f"(cross-phase standard + v3 rejection/stop + {len(inventories)} implementation inventories; quality remains human-gated)")


if __name__ == "__main__":
    main()
