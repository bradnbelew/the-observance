#!/usr/bin/env python3
"""Verify retained M06 Paper install/restart evidence and open boundaries."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
RETAINED = ROOT / "morrow" / "rehearsal" / "consensus-audit-runtime"
INDEX = RETAINED / "latest.json"
STATUS = "paper_install_restart_pass_client_interaction_open"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def historical_sha(commit: str, path: str) -> str:
    result = subprocess.run(["git", "show", f"{commit}:{path}"], cwd=ROOT, capture_output=True)
    require(result.returncode == 0, f"historical producer unavailable: {commit}:{path}")
    return hashlib.sha256(result.stdout).hexdigest()


def within(parent: Path, child: Path) -> bool:
    try:
        child.relative_to(parent)
        return True
    except ValueError:
        return False


def validate() -> None:
    index = load(INDEX)
    require(index["schema_version"] == "1.0.0-morrow-consensus-audit-runtime-checkpoint"
            and index["status"] == STATUS and index["production_enablement"] == "blocked",
            "M06 runtime index overclaims its gate")
    require(index["boundaries"] == {
        "graphical_client_used": False,
        "player_interaction_proven": False,
        "full_human_playthrough_proven": False,
        "production_mutated": False,
        "production_credentials_loaded": False,
    }, "M06 runtime boundaries drifted")
    commit = index["source_commit"]
    ancestry = subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"],
                              cwd=ROOT, capture_output=True)
    require(ancestry.returncode == 0, "M06 source is not an ancestor of HEAD")
    producer = index["producer"]
    require(historical_sha(commit, producer["path"]) == producer["sha256"],
            "M06 historical producer drifted")
    receipt_path = (ROOT / index["receipt"]).resolve()
    require(within(RETAINED.resolve(), receipt_path) and receipt_path.is_file(),
            "M06 receipt escaped retained root")
    require(sha(receipt_path) == index["receipt_sha256"], "M06 receipt hash drifted")
    receipt = load(receipt_path)
    runtime, proof = receipt["runtime"], receipt["proof"]
    require(receipt["schema_version"] == "1.0.0-morrow-disposable-paper-runtime"
            and receipt["status"] == "pass" and receipt["source_commit"] == commit
            and receipt["runner_sha256"] == producer["sha256"], "M06 receipt identity drifted")
    pattern = re.compile(r"status=(BUILT|ALREADY_PRESENT) manifest=([0-9a-f]{64}) blocks=1309")
    first = pattern.search(runtime["first_consensus_audit"])
    restart = pattern.search(runtime["restart_consensus_audit"])
    require(first is not None and restart is not None and first.group(1) == "BUILT"
            and restart.group(1) == "ALREADY_PRESENT" and first.group(2) == restart.group(2),
            "M06 build/restart manifest proof drifted")
    require(runtime["first_entities"]["consensus_audit"] == 8
            and runtime["restart_entities"]["consensus_audit"] == 8
            and runtime["first_entities"] == runtime["restart_entities"],
            "M06 entity count changed across restart")
    require(proof["consensus_audit_built_and_readback_audited"] is True
            and proof["restart_already_present_audit"] is True
            and proof["entity_counts_stable_across_restart"] is True
            and proof["graceful_cleanup_logged"] is True
            and proof["listeners_closed"] is True
            and proof["graphical_client_automated"] is False
            and proof["production_mutated"] is False,
            "M06 bounded runtime proof drifted")
    require(re.fullmatch(r"[0-9a-f]{64}", receipt["durable_artifacts"][
        "morrow-consensus-audit.install.receipt"]) is not None, "M06 install receipt is missing")
    for name, hash_key in (("morrow-first-start.log", "first_sha256"),
                           ("morrow-restart.log", "restart_sha256")):
        log_path = receipt_path.parent / name
        require(log_path.is_file() and sha(log_path) == receipt["logs"][hash_key],
                f"M06 retained {name} drifted")
        text = log_path.read_text(encoding="utf-8")
        require("MORROW_CONSENSUS_AUDIT_READY" in text and "MORROW_RUNTIME_CLOSED" in text
                and "Morrow reboot failed closed" not in text,
                f"M06 retained {name} lacks lifecycle proof")
    require(not any(path.suffix.lower() in {".jar", ".dll", ".exe", ".zip"}
                    for path in RETAINED.rglob("*") if path.is_file()),
            "M06 retained evidence contains binaries")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001
        print(f"MORROW CONSENSUS AUDIT RUNTIME CHECKPOINT: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW CONSENSUS AUDIT RUNTIME CHECKPOINT: PASS blocks=1309 restart=true entities=8 client-interaction=open")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
