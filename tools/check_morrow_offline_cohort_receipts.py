#!/usr/bin/env python3
"""Verify retained prepare-only 1/2/6 vanilla-client cohort evidence."""

from __future__ import annotations

import hashlib
import json
import subprocess
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "morrow" / "rehearsal" / "client-cohort-prep" / \
    "2026-08-30-offline-cohort-preparation.json"
SOUND_CATEGORIES = {
    "master", "music", "record", "weather", "block", "hostile",
    "neutral", "player", "ambient", "voice", "ui",
}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def within(parent: Path, child: Path) -> bool:
    try:
        child.relative_to(parent)
        return True
    except ValueError:
        return False


def validate() -> None:
    index = load(INDEX)
    source_commit = index["source_commit"]
    require(index["status"] == "bounded_prepare_only_pass"
            and index["production_enablement"] == "blocked",
            "cohort preparation index overclaims its gate")
    require(all(value is False for value in index["boundaries"].values()),
            "cohort preparation crossed a retained boundary")
    ancestry = subprocess.run(
        ["git", "merge-base", "--is-ancestor", source_commit, "HEAD"],
        cwd=ROOT, capture_output=True,
    )
    require(ancestry.returncode == 0, "cohort source commit is not an ancestor of HEAD")
    for artifact in index["artifacts"].values():
        path = ROOT / artifact["path"]
        require(path.is_file() and sha(path) == artifact["sha256"],
                f"cohort preparation artifact drifted: {artifact['path']}")

    expected = {"one": 1, "two": 2, "six": 6}
    require(set(index["cohorts"]) == set(expected), "cohort preparation set drifted")
    all_uuids: set[str] = set()
    for name, size in expected.items():
        binding = index["cohorts"][name]
        require(binding["size"] == size, f"{name} cohort size binding drifted")
        cohort_path = (ROOT / binding["receipt"]).resolve()
        retained_root = (ROOT / "morrow" / "rehearsal" / "client-cohort-prep").resolve()
        require(within(retained_root, cohort_path) and cohort_path.is_file(),
                f"{name} cohort receipt escaped retained root")
        require(sha(cohort_path) == binding["receipt_sha256"],
                f"{name} cohort receipt hash drifted")
        cohort = load(cohort_path)
        require(cohort["schema_version"] == "1.0.0-morrow-offline-cohort"
                and cohort["status"] == "prepared"
                and cohort["source_commit"] == source_commit
                and cohort["cohort_size"] == size
                and cohort["distinct_identity_count"] == size
                and cohort["visible_clients_launched"] is False
                and cohort["loopback_only"] is True
                and cohort["server"] == "127.0.0.1:25589"
                and cohort["audio_disabled"] is binding["audio_disabled"]
                and cohort["max_memory_mib_per_client"] == binding["max_memory_mib_per_client"]
                and len(cohort["clients"]) == size,
                f"{name} cohort aggregate receipt is incomplete")
        cohort_uuids: set[str] = set()
        for position, row in enumerate(cohort["clients"], start=1):
            child_path = (cohort_path.parent / row["receipt"]).resolve()
            require(within(cohort_path.parent, child_path) and child_path.is_file(),
                    f"{name} client {position} receipt escaped cohort root")
            require(sha(child_path) == row["receipt_sha256"],
                    f"{name} client {position} receipt hash drifted")
            child = load(child_path)
            options = child_path.with_name("options.txt")
            require(options.is_file()
                    and sha(options) == child["accessibility_profile"]["options_sha256"],
                    f"{name} client {position} options hash drifted")
            require(child["status"] == "prepared"
                    and child["source_commit"] == source_commit
                    and child["process_id"] is None
                    and child["loopback_only"] is True
                    and child["server"] == cohort["server"]
                    and child["account_files_read"] is False
                    and child["production_credentials_loaded"] is False
                    and child["outbound_proxy"] == "127.0.0.1:1"
                    and child["max_memory_mib"] == binding["max_memory_mib_per_client"]
                    and child["accessibility_profile"]["audio_disabled"] is binding["audio_disabled"]
                    and row["status"] == "prepared"
                    and row["process_id"] is None
                    and row["uuid"] == child["identity"]["uuid"]
                    and row["username"] == child["identity"]["username"],
                    f"{name} client {position} preparation receipt is incomplete")
            categories = set(child["accessibility_profile"]["sound_categories_zeroed"])
            option_lines = options.read_text(encoding="utf-8").splitlines()
            zeroed = {line.removeprefix("soundCategory_").removesuffix(":0.0")
                      for line in option_lines
                      if line.startswith("soundCategory_") and line.endswith(":0.0")}
            if binding["audio_disabled"]:
                require(categories == SOUND_CATEGORIES and zeroed == SOUND_CATEGORIES,
                        f"{name} client {position} silent-audio profile is incomplete")
            else:
                require(not categories and not zeroed,
                        f"{name} client {position} unexpectedly changed audio")
            require(row["uuid"] not in cohort_uuids and row["uuid"] not in all_uuids,
                    f"{name} client {position} identity collided")
            cohort_uuids.add(row["uuid"])
            all_uuids.add(row["uuid"])

    retained = INDEX.parent / "9962ded"
    forbidden = {".jar", ".dll", ".exe", ".zip"}
    require(not any(path.suffix.lower() in forbidden for path in retained.rglob("*")),
            "retained cohort evidence contains client binaries")
    require(len(all_uuids) == 9, "retained 1/2/6 identity total drifted")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - checker emits one concise failure
        print(f"MORROW OFFLINE COHORT RECEIPTS: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW OFFLINE COHORT RECEIPTS: PASS sizes=1/2/6 clients=9 gui=false server=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
