#!/usr/bin/env python3
"""Validate the honest offline/private regression receipt and current artifact bytes."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RECEIPT = ROOT / "design/handoff/PRIVATE-LAUNCH-ROUTED-REGRESSION-RECEIPT.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    data = json.loads(RECEIPT.read_text(encoding="utf-8"))
    require(data["result"] == "offline_and_disposable_private_gates_passed; historical generic external blockers superseded by exact target discovery receipt",
            "receipt result overclaim or drift")
    require(len(data["routed_chain"]["expected_fail_closed_boundary"]["missing_environment_variables"]) == 6,
            "secret-dependent boundary incomplete")
    for artifact in data["deployable_artifacts"]:
        path = ROOT / artifact["path"]
        require(path.is_file() and path.stat().st_size == artifact["bytes"]
                and sha256(path) == artifact["sha256"], f"artifact parity drift: {artifact['path']}")
    for label, reason in data["external_private_target_blockers"].items():
        require(reason and any(token in reason.lower() for token in
                               ("pending", "only", "no ", "closed", "absent", "unapplied", "untouched")),
                f"external blocker missing: {label}")
    require(len(data["not_launch_ready"]) == 4
            and data["p4_vnext"]["brad_visual_approval"] is None
            and data["production_mutated"] is False and data["public_launch"] is False,
            "launch/approval boundary weakened")
    require(sha256(ROOT / "design/m3/PAPER-VNEXT-DISPOSABLE-RECEIPT.json")
            == data["p4_vnext"]["paper_validation_receipt_sha256"], "Paper validation receipt drift")
    require(sha256(ROOT / "design/m3/PAPER-VNEXT-REVIEW-SERVER-RECEIPT.json")
            == data["p4_vnext"]["paper_review_receipt_sha256"], "Paper review receipt drift")
    print("Private launch routed receipt PASS: offline/disposable gates exact; verified external residues fail closed")


if __name__ == "__main__":
    main()
