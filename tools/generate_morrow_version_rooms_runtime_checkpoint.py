#!/usr/bin/env python3
"""Retain a successful M05 Paper install/restart receipt without binaries."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
RETAINED = ROOT / "morrow" / "rehearsal" / "version-rooms-runtime"
INDEX = RETAINED / "latest.json"
RUNNER = "tools/run_morrow_disposable_paper.py"
STATUS = "paper_install_restart_pass_client_interaction_open"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def historical_sha(commit: str, path: str) -> str:
    result = subprocess.run(["git", "show", f"{commit}:{path}"], cwd=ROOT,
                            capture_output=True)
    require(result.returncode == 0, f"historical producer unavailable: {commit}:{path}")
    return hashlib.sha256(result.stdout).hexdigest()


def relative(path: Path) -> str:
    return str(path.resolve().relative_to(ROOT)).replace("\\", "/")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True,
                        help="directory containing paper-runtime-receipt.json and two logs")
    args = parser.parse_args()
    source = args.source.resolve()
    names = ("paper-runtime-receipt.json", "morrow-first-start.log", "morrow-restart.log")
    require(all((source / name).is_file() for name in names), "M05 runtime evidence is incomplete")
    require(not INDEX.exists(), "refusing to overwrite retained M05 runtime index")

    receipt = load(source / names[0])
    commit = receipt["source_commit"]
    runtime, proof = receipt["runtime"], receipt["proof"]
    require(receipt["schema_version"] == "1.0.0-morrow-disposable-paper-runtime"
            and receipt["status"] == "pass", "source receipt is not a passing Paper run")
    require("status=BUILT" in runtime["first_version_rooms"]
            and "blocks=1617" in runtime["first_version_rooms"]
            and "status=ALREADY_PRESENT" in runtime["restart_version_rooms"]
            and "blocks=1617" in runtime["restart_version_rooms"],
            "source receipt does not prove M05 install and restart audit")
    require(runtime["first_entities"]["version_rooms"] == 3
            and runtime["restart_entities"]["version_rooms"] == 3
            and proof["version_rooms_built_and_readback_audited"] is True
            and proof["restart_already_present_audit"] is True
            and proof["graphical_client_automated"] is False
            and proof["production_mutated"] is False,
            "source receipt crossed or omitted an M05 proof boundary")
    ancestry = subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"],
                              cwd=ROOT, capture_output=True)
    require(ancestry.returncode == 0, "M05 runtime source commit is not an ancestor of HEAD")

    destination = RETAINED / commit[:7]
    require(not destination.exists(), "refusing to overwrite retained M05 runtime evidence")
    destination.mkdir(parents=True)
    for name in names:
        shutil.copy2(source / name, destination / name)
    retained_receipt = destination / names[0]
    report = {
        "schema_version": "1.0.0-morrow-version-rooms-runtime-checkpoint",
        "status": STATUS,
        "source_commit": commit,
        "receipt": relative(retained_receipt),
        "receipt_sha256": sha(retained_receipt),
        "producer": {"path": RUNNER, "sha256": historical_sha(commit, RUNNER)},
        "scope": "owned loopback Paper 1.21.11 M05 construction, semantic readback, restart, and cleanup",
        "boundaries": {
            "graphical_client_used": False,
            "player_interaction_proven": False,
            "full_human_playthrough_proven": False,
            "production_mutated": False,
            "production_credentials_loaded": False,
        },
        "remaining_gate": "exercise all three consoles, wrong route, Escape, destructive choices, and preserve contradiction with real clients and retained media",
        "production_enablement": "blocked",
    }
    INDEX.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status": STATUS, "index": relative(INDEX)}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
