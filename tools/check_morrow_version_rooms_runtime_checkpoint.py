#!/usr/bin/env python3
"""Verify retained M05 Paper install/restart evidence and its open boundaries."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
RETAINED = ROOT / "morrow" / "rehearsal" / "version-rooms-runtime"
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
    result = subprocess.run(["git", "show", f"{commit}:{path}"], cwd=ROOT,
                            capture_output=True)
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
    require(index["schema_version"] == "1.0.0-morrow-version-rooms-runtime-checkpoint"
            and index["status"] == STATUS and index["production_enablement"] == "blocked",
            "M05 runtime index overclaims its gate")
    require(index["boundaries"] == {
        "graphical_client_used": False,
        "player_interaction_proven": False,
        "full_human_playthrough_proven": False,
        "production_mutated": False,
        "production_credentials_loaded": False,
    }, "M05 runtime boundaries drifted")
    commit = index["source_commit"]
    ancestry = subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"],
                              cwd=ROOT, capture_output=True)
    require(ancestry.returncode == 0, "M05 runtime source is not an ancestor of HEAD")
    producer = index["producer"]
    require(historical_sha(commit, producer["path"]) == producer["sha256"],
            "M05 historical runtime producer drifted")

    receipt_path = (ROOT / index["receipt"]).resolve()
    require(within(RETAINED.resolve(), receipt_path) and receipt_path.is_file(),
            "M05 receipt escaped retained evidence root")
    require(sha(receipt_path) == index["receipt_sha256"], "M05 receipt hash drifted")
    receipt = load(receipt_path)
    runtime, proof = receipt["runtime"], receipt["proof"]
    require(receipt["schema_version"] == "1.0.0-morrow-disposable-paper-runtime"
            and receipt["status"] == "pass" and receipt["source_commit"] == commit,
            "M05 Paper receipt identity drifted")
    require(receipt["runner_sha256"] == producer["sha256"], "M05 runner binding drifted")
    pattern = re.compile(r"status=(BUILT|ALREADY_PRESENT) manifest=([0-9a-f]{64}) blocks=1617")
    first = pattern.search(runtime["first_version_rooms"])
    restart = pattern.search(runtime["restart_version_rooms"])
    require(first is not None and restart is not None and first.group(1) == "BUILT"
            and restart.group(1) == "ALREADY_PRESENT" and first.group(2) == restart.group(2),
            "M05 build/restart manifest proof drifted")
    require(runtime["first_entities"]["version_rooms"] == 3
            and runtime["restart_entities"]["version_rooms"] == 3
            and runtime["first_entities"] == runtime["restart_entities"],
            "M05 entity count changed across restart")
    require(proof["version_rooms_built_and_readback_audited"] is True
            and proof["restart_already_present_audit"] is True
            and proof["entity_counts_stable_across_restart"] is True
            and proof["graceful_cleanup_logged"] is True
            and proof["listeners_closed"] is True
            and proof["graphical_client_automated"] is False
            and proof["production_mutated"] is False,
            "M05 bounded runtime proof drifted")
    require(re.fullmatch(r"[0-9a-f]{64}", receipt["durable_artifacts"][
        "morrow-version-rooms.install.receipt"]) is not None,
        "M05 durable install receipt is missing")
    for name, hash_key in (("morrow-first-start.log", "first_sha256"),
                           ("morrow-restart.log", "restart_sha256")):
        log_path = receipt_path.parent / name
        require(log_path.is_file() and sha(log_path) == receipt["logs"][hash_key],
                f"M05 retained {name} drifted")
        text = log_path.read_text(encoding="utf-8")
        require("MORROW_VERSION_ROOMS_READY" in text and "MORROW_RUNTIME_CLOSED" in text
                and "Morrow reboot failed closed" not in text,
                f"M05 retained {name} lacks lifecycle proof")
    require(not any(path.suffix.lower() in {".jar", ".dll", ".exe", ".zip"}
                    for path in RETAINED.rglob("*") if path.is_file()),
            "M05 retained runtime evidence contains binaries")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - emit one concise verifier failure
        print(f"MORROW VERSION ROOMS RUNTIME CHECKPOINT: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW VERSION ROOMS RUNTIME CHECKPOINT: PASS blocks=1617 restart=true entities=3 client-interaction=open")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
