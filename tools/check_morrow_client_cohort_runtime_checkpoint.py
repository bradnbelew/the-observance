#!/usr/bin/env python3
"""Verify retained real-client 1/2/6 cohort runtime evidence."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
RETAINED = ROOT / "morrow" / "rehearsal" / "client-cohort-runtime"
INDEX = RETAINED / "latest.json"
EXPECTED_STATUS = "bounded_join_inventory_restart_cleanup_pass_full_playthrough_open"
EXPECTED_PROOF_TRUE = {
    "client_processes_closed", "morrow_cleanup_logged_twice", "paper_processes_closed",
    "paper_restart_completed", "real_vanilla_clients_joined_concurrently",
    "same_uuid_after_restart",
}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def historical_sha(commit: str, path: str) -> str:
    result = subprocess.run(
        ["git", "show", f"{commit}:{path}"], cwd=ROOT, capture_output=True)
    require(result.returncode == 0, f"historical producer unavailable: {commit}:{path}")
    return hashlib.sha256(result.stdout).hexdigest()


def within(parent: Path, child: Path) -> bool:
    try:
        child.relative_to(parent)
        return True
    except ValueError:
        return False


def check_lane(receipt_path: Path, rows: list[dict[str, Any]], size: int,
               source_commit: str, lane: str) -> dict[str, str]:
    require(len(rows) == size, f"{lane} client count drifted")
    identities: dict[str, str] = {}
    for position, row in enumerate(rows, start=1):
        username = f"MorrowW{position:02d}"
        child_path = (receipt_path.parent / row["receipt"]).resolve()
        require(within(receipt_path.parent, child_path) and child_path.is_file(),
                f"{lane} child receipt escaped retained root")
        require(sha(child_path) == row["receipt_sha256"],
                f"{lane} {username} child receipt hash drifted")
        child = load(child_path)
        require(row["username"] == username == child["identity"]["username"]
                and row["uuid"] == child["identity"]["uuid"]
                and child["source_commit"] == source_commit
                and child["schema_version"] == "1.0.0-morrow-offline-client-launch"
                and child["status"] == "harness_stopped_after_request"
                and child["stop_reason"] == "request"
                and child["loopback_only"] is True
                and child["server"].startswith("127.0.0.1:")
                and child["server_resource_pack_policy"] == "disabled"
                and child["account_files_read"] is False
                and child["production_credentials_loaded"] is False
                and child["outbound_proxy"] == "127.0.0.1:1"
                and child["max_memory_mib"] == 768,
                f"{lane} {username} client provenance is incomplete")
        require(row["client_log_sha256"] == child["log_sha256"],
                f"{lane} {username} client log binding drifted")
        identities[username] = row["uuid"]
    require(len(set(identities.values())) == size, f"{lane} UUIDs are not distinct")
    return identities


def validate() -> None:
    index = load(INDEX)
    require(index["schema_version"] == "1.0.0-morrow-real-client-cohort-runtime-checkpoint"
            and index["status"] == EXPECTED_STATUS
            and index["production_enablement"] == "blocked",
            "runtime cohort index overclaims its gate")
    require(index["boundaries"] == {
        "input_injected": False,
        "production_mutated": False,
        "production_credentials_loaded": False,
        "full_human_playthrough_proven": False,
    }, "runtime cohort boundaries drifted")
    expected = {"one": 1, "two": 2, "six": 6}
    require(set(index["cohorts"]) == set(expected), "runtime cohort set drifted")
    for label, size in expected.items():
        binding = index["cohorts"][label]
        source_commit = binding["source_commit"]
        require(binding["size"] == size, f"{label} size binding drifted")
        ancestry = subprocess.run(
            ["git", "merge-base", "--is-ancestor", source_commit, "HEAD"],
            cwd=ROOT, capture_output=True)
        require(ancestry.returncode == 0, f"{label} source is not an ancestor of HEAD")
        producer = binding["producer"]
        require(historical_sha(source_commit, producer["path"]) == producer["sha256"],
                f"{label} historical producer drifted")
        receipt_path = (ROOT / binding["receipt"]).resolve()
        require(within(RETAINED.resolve(), receipt_path) and receipt_path.is_file(),
                f"{label} receipt escaped retained root")
        require(sha(receipt_path) == binding["receipt_sha256"],
                f"{label} aggregate receipt hash drifted")
        receipt = load(receipt_path)
        proof, server = receipt["proof"], receipt["server"]
        require(receipt["schema_version"] == "1.0.0-morrow-real-client-cohort-runtime"
                and receipt["status"] == EXPECTED_STATUS
                and receipt["source_commit"] == source_commit
                and receipt["cohort_size"] == size
                and receipt["production_enablement"] == "blocked",
                f"{label} aggregate receipt is incomplete")
        require(all(proof[field] is True for field in EXPECTED_PROOF_TRUE)
                and proof["full_human_playthrough_proven"] is False
                and proof["input_injected"] is False
                and proof["production_mutated"] is False
                and proof["projector_redelivery_after_restart"] is False
                and proof["inventory_items_per_player_before_disconnect"] == 2304
                and proof["inventory_items_per_player_after_restart"] == 2304,
                f"{label} bounded proof drifted")
        require(server["bind"] == "127.0.0.1" and server["online_mode"] is False
                and server["production_credentials_loaded"] is False,
                f"{label} server boundary drifted")
        require(len(proof["first_inventory_count_lines"]) == size
                and len(proof["restart_inventory_count_lines"]) == size
                and all("Found 2304 matching item(s)" in line
                        for line in proof["first_inventory_count_lines"]
                        + proof["restart_inventory_count_lines"]),
                f"{label} inventory evidence drifted")
        for phase in ("first", "restart"):
            log_path = receipt_path.parent / server[f"{phase}_log"]
            require(log_path.is_file() and sha(log_path) == server[f"{phase}_log_sha256"],
                    f"{label} {phase} Paper log drifted")
            text = log_path.read_text(encoding="utf-8")
            require("MORROW_RUNTIME_CLOSED" in text,
                    f"{label} {phase} Paper log lacks cleanup receipt")
        first_ids = check_lane(receipt_path, receipt["clients"]["first"], size,
                               source_commit, f"{label}/first")
        restart_ids = check_lane(receipt_path, receipt["clients"]["restart"], size,
                                 source_commit, f"{label}/restart")
        require(first_ids == restart_ids, f"{label} UUIDs changed across restart")
        expected_users = set(first_ids)
        for field in ("safe_entry_lines", "safe_rejoin_lines"):
            lines = proof[field]
            users = {match.group(1) for line in lines
                     if (match := re.search(r"player=(MorrowW\d{2}).*to=0,80,-2 "
                                            r"inventory_mutations=0", line))}
            require(len(lines) == size and users == expected_users,
                    f"{label} {field} drifted")
    require(not any(path.suffix.lower() in {".jar", ".dll", ".exe", ".zip"}
                    for path in RETAINED.rglob("*") if path.is_file()),
            "retained runtime cohort evidence contains binaries")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - checker emits one concise failure
        print(f"MORROW CLIENT COHORT RUNTIME CHECKPOINT: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW CLIENT COHORT RUNTIME CHECKPOINT: PASS sizes=1/2/6 clients=9 "
          "restart=true full-playthrough=open")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
