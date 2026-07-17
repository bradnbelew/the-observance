#!/usr/bin/env python3
"""Validate the final V5 rejection, disconnect, and clean-stop evidence."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DECISION = ROOT / "design/m3/BRAD-V5-REVIEW-DECISION.json"
STOP = ROOT / "design/m3/PAPER-V5-REVIEW-STOP-RECEIPT.json"
ACTIVE = ROOT / "design/m3/BRAD-V5-ACTIVE-REVIEW.json"


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    decision = load(DECISION)
    require(decision["review_status"]
                == "complete_not_approved_disconnect_and_clean_stop_receipted"
            and decision["decision"] == "not_approved_revision_required"
            and decision["brad_visual_approval"] is None
            and decision["m4_authority"] == "closed"
            and decision["production_mutated"] is False,
            "V5 decision/approval/production boundary drift")
    require("immutable technical proof" in decision["experiential_decision"]
            and "rejected experiential proof" in decision["experiential_decision"]
            and decision["binding_progression_correction"]["rule"]
                == "Correct exact conclusions pass with zero source-touch or observation receipts.",
            "V5 technical/experiential or zero-observation decision drift")
    p4 = decision["binding_p4_experience"]
    require(p4["target_active_hours"] == {"min": 1, "max": 2}
            and p4["difficulty"] == "high"
            and "cross-media ARG" in p4["identity"]
            and "no mandatory brittle" in p4["minecraft_boundary"],
            "V5 P4 experience/geometry decision drift")
    require(len(decision["visual_format_affordance_carry_forward"]) == 6
            and decision["p4_c02_reversal"]["earned_belief"].startswith("C02 only"),
            "V5 visual carry-forward or C02 ladder boundary drift")

    stop = load(STOP)
    require(stop["target_id"] == "m3-v5-brad-review-51ad3bd-20260717-a"
            and stop["review_process_id"] == 10860
            and stop["port"] == 25588 and stop["bind"] == "127.0.0.1",
            "V5 stop target identity drift")
    disconnect = stop["disconnect_confirmation"]
    require(disconnect["player"] == "SirNan"
            and disconnect["join_count"] == disconnect["leave_count"] == 1
            and disconnect["active_by_join_leave_delta"] == 0
            and disconnect["no_later_join_in_exact_log"] is True,
            "V5 disconnect/zero-active-player evidence drift")
    require(stop["result"] == "clean_save_flush_stop_verified"
            and stop["post_stop_readback"]
                == {"pid_10860_alive": False, "port_25588_listener_count": 0}
            and stop["latest_log_sha256"]
                == "dfa8d262b29784c281c8dc265a583432db1fe76ae225ee0b04ec2477cd8ece2c"
            and stop["review_state"]["journal_sha256"]
                == "a9e22d3eed64d9b65ae9bd012d117fb3e00c5c0ef9a4cada19c5157a50c59bc3"
            and stop["production_mutated"] is False
            and stop["brad_visual_approval"] is None
            and stop["m4_authority"] == "closed",
            "V5 save/stop/hash/approval evidence drift")

    active = load(ACTIVE)
    require(active["review_status"]
                == "complete_not_approved_disconnect_and_clean_stop_receipted"
            and active["decision"] == "not_approved_revision_required"
            and active["review_server_stop_receipt"]
                == "design/m3/PAPER-V5-REVIEW-STOP-RECEIPT.json"
            and active["implementation_state"]
                == "vnext_offline_authoring_authorized_after_clean_v5_stop",
            "V5 active overlay did not close into final decision")
    print("M3 V5 REVIEW DECISION: PASS (rejected experiential proof, disconnect, clean stop, M4 closed)")


if __name__ == "__main__":
    main()
