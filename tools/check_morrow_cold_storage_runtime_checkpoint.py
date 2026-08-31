#!/usr/bin/env python3
"""Verify retained M11 Paper install/restart evidence and open human bridge boundaries."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
RETAINED = ROOT / "morrow" / "rehearsal" / "cold-storage-runtime"
INDEX = RETAINED / "latest.json"
STATUS = "paper_install_restart_pass_human_bridge_open"


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
    require(index["schema_version"] == "1.0.0-morrow-cold-storage-runtime-checkpoint"
            and index["status"] == STATUS and index["production_enablement"] == "blocked",
            "M11 runtime index overclaims its gate")
    require(index["boundaries"] == {
        "graphical_client_used": False,
        "ordered_record_dialogs_proven": False,
        "tagged_snapshot_hash_transfer_proven": False,
        "two_instance_player_comparison_proven": False,
        "caption_and_waveform_readability_proven": False,
        "native_dialog_interaction_proven": False,
        "full_human_playthrough_proven": False,
        "production_mutated": False,
        "production_credentials_loaded": False,
    }, "M11 runtime boundaries drifted")
    commit = index["source_commit"]
    ancestry = subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"],
                              cwd=ROOT, capture_output=True)
    require(ancestry.returncode == 0, "M11 source is not an ancestor of HEAD")
    producer = index["producer"]
    require(historical_sha(commit, producer["path"]) == producer["sha256"],
            "M11 historical producer drifted")
    receipt_path = (ROOT / index["receipt"]).resolve()
    require(within(RETAINED.resolve(), receipt_path) and receipt_path.is_file(),
            "M11 receipt escaped retained root")
    require(sha(receipt_path) == index["receipt_sha256"], "M11 receipt hash drifted")
    receipt = load(receipt_path); runtime, proof = receipt["runtime"], receipt["proof"]
    require(receipt["schema_version"] == "1.0.0-morrow-disposable-paper-runtime"
            and receipt["status"] == "pass" and receipt["source_commit"] == commit
            and receipt["runner_sha256"] == producer["sha256"], "M11 receipt identity drifted")
    require(receipt["paper"]["version"] == "1.21.11" and receipt["paper"]["build"] == 132
            and receipt["paper"]["sha256"] == "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba",
            "M11 exact Paper input drifted")
    pattern = re.compile(r"status=(BUILT|ALREADY_PRESENT) manifest=([0-9a-f]{64}) blocks=6417")
    first = pattern.search(runtime["first_cold_storage"])
    restart = pattern.search(runtime["restart_cold_storage"])
    require(first is not None and restart is not None and first.group(1) == "BUILT"
            and restart.group(1) == "ALREADY_PRESENT" and first.group(2) == restart.group(2),
            "M11 build/restart manifest proof drifted")
    require(runtime["first_entities"]["cold_storage"] == 11
            and runtime["restart_entities"]["cold_storage"] == 11
            and runtime["first_entities"] == runtime["restart_entities"],
            "M11 entity count changed across restart")
    require(proof["cold_storage_built_and_readback_audited"] is True
            and proof["restart_already_present_audit"] is True
            and proof["entity_counts_stable_across_restart"] is True
            and proof["graceful_cleanup_logged"] is True and proof["listeners_closed"] is True
            and proof["graphical_client_automated"] is False and proof["production_mutated"] is False,
            "M11 bounded runtime proof drifted")
    require(re.fullmatch(r"[0-9a-f]{64}", receipt["durable_artifacts"][
        "morrow-cold-storage.install.receipt"]) is not None, "M11 install receipt is missing")
    for name, hash_key in (("morrow-first-start.log", "first_sha256"),
                           ("morrow-restart.log", "restart_sha256")):
        log_path = receipt_path.parent / name
        require(log_path.is_file() and sha(log_path) == receipt["logs"][hash_key],
                f"M11 retained {name} drifted")
        text = log_path.read_text(encoding="utf-8")
        require("MORROW_COLD_STORAGE_READY" in text and "MORROW_RUNTIME_CLOSED" in text
                and "Morrow reboot failed closed" not in text,
                f"M11 retained {name} lacks lifecycle proof")
    require(not any(path.suffix.lower() in {".jar", ".dll", ".exe", ".zip"}
                    for path in RETAINED.rglob("*") if path.is_file()),
            "M11 retained evidence contains binaries")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001
        print(f"MORROW COLD STORAGE RUNTIME CHECKPOINT: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW COLD STORAGE RUNTIME CHECKPOINT: PASS blocks=6417 restart=true entities=11 human-bridge=open")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
