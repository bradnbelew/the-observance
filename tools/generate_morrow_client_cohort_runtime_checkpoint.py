#!/usr/bin/env python3
"""Retain successful real-client 1/2/6 cohort runtime receipts."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
RETAINED = ROOT / "morrow" / "rehearsal" / "client-cohort-runtime"
INDEX = RETAINED / "latest.json"
RUNNER = "tools/run_morrow_client_cohort_runtime_rehearsal.py"
EXPECTED_STATUS = "bounded_join_inventory_restart_cleanup_pass_full_playthrough_open"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def historical_sha(commit: str, path: str) -> str:
    result = subprocess.run(
        ["git", "show", f"{commit}:{path}"], cwd=ROOT, capture_output=True)
    require(result.returncode == 0, f"historical producer unavailable: {commit}:{path}")
    return hashlib.sha256(result.stdout).hexdigest()


def relative(path: Path) -> str:
    return str(path.resolve().relative_to(ROOT)).replace("\\", "/")


def retain(label: str, size: int, source: Path) -> dict[str, Any]:
    source = source.resolve()
    receipt = source / "cohort-runtime-rehearsal.json"
    require(receipt.is_file(), f"{label} aggregate receipt is missing")
    data = load(receipt)
    commit = data["source_commit"]
    require(data["schema_version"] == "1.0.0-morrow-real-client-cohort-runtime"
            and data["status"] == EXPECTED_STATUS
            and data["cohort_size"] == size,
            f"{label} aggregate receipt is not a successful size-{size} runtime run")
    ancestry = subprocess.run(
        ["git", "merge-base", "--is-ancestor", commit, "HEAD"],
        cwd=ROOT, capture_output=True)
    require(ancestry.returncode == 0, f"{label} source commit is not an ancestor of HEAD")
    require(all(path.suffix.lower() in {".json", ".log"}
                for path in source.rglob("*") if path.is_file()),
            f"{label} source contains a non-evidence artifact")
    destination = RETAINED / commit[:7] / label
    require(not destination.exists(), f"refusing to overwrite retained {label} evidence")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(source, destination)
    retained_receipt = destination / receipt.name
    return {
        "size": size,
        "source_commit": commit,
        "receipt": relative(retained_receipt),
        "receipt_sha256": sha(retained_receipt),
        "producer": {
            "path": RUNNER,
            "sha256": historical_sha(commit, RUNNER),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--one", type=Path, required=True)
    parser.add_argument("--two", type=Path, required=True)
    parser.add_argument("--six", type=Path, required=True)
    args = parser.parse_args()
    require(not INDEX.exists(), "refusing to overwrite the retained cohort runtime index")
    cohorts = {
        "one": retain("one", 1, args.one),
        "two": retain("two", 2, args.two),
        "six": retain("six", 6, args.six),
    }
    report = {
        "schema_version": "1.0.0-morrow-real-client-cohort-runtime-checkpoint",
        "status": EXPECTED_STATUS,
        "scope": "owned loopback vanilla clients joined concurrently, received mutation-free safe "
                 "entry, retained full main inventories across Paper restart, and closed cleanly",
        "cohorts": cohorts,
        "boundaries": {
            "input_injected": False,
            "production_mutated": False,
            "production_credentials_loaded": False,
            "full_human_playthrough_proven": False,
        },
        "remaining_gate": "complete the operator-free 60-90 minute cohort slice with continuous "
                          "media and independent observer review",
        "production_enablement": "blocked",
    }
    INDEX.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status": report["status"], "index": relative(INDEX)}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
