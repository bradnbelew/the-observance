#!/usr/bin/env python3
"""Verify the retained real-client Morrow optional-pack decision pair."""

from __future__ import annotations

import hashlib
import json
import subprocess
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "morrow" / "rehearsal" / "resource-pack" / "latest.json"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def historical_sha(commit: str, path: str) -> str:
    result = subprocess.run(
        ["git", "show", f"{commit}:{path}"], cwd=ROOT, capture_output=True,
    )
    require(result.returncode == 0, f"resource-pack producer missing at source commit: {path}")
    return hashlib.sha256(result.stdout).hexdigest()


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


def validate_lane(root: Path, binding: dict[str, Any], expected: str) -> dict[str, Any]:
    receipt_path = (ROOT / binding["receipt"]).resolve()
    require(within(root, receipt_path) and receipt_path.is_file(),
            f"{expected} receipt escaped retained root")
    require(sha(receipt_path) == binding["sha256"], f"{expected} receipt hash drifted")
    receipt = load(receipt_path)
    require(receipt["schema_version"] == "1.0.0-morrow-resource-pack-rehearsal"
            and receipt["status"] == "bounded_handshake_pass_human_visual_parity_open"
            and receipt["expected_status"] == expected,
            f"{expected} receipt status drifted")
    require(receipt["proof"] == {
        "exact_pack_hash_bound": True,
        "human_media_reviewed": False,
        "listeners_closed": True,
        "optional_policy_reported": True,
        "pack_bytes_fetched": expected == "LOADED",
        "production_mutated": False,
        "status_observed_from_real_client": True,
        "vanilla_fallback_visual_equivalence": False,
    }, f"{expected} proof boundary drifted")
    require(receipt["resource_pack"]["required"] is False
            and receipt["server"]["bind"] == "127.0.0.1"
            and receipt["server"]["online_mode"] is False
            and receipt["server"]["production_credentials_loaded"] is False
            and "resource_pack=optional" in receipt["server"]["runtime_ready_line"]
            and f"status={expected}" in receipt["observed_status_line"],
            f"{expected} runtime boundary drifted")
    successful_gets = [row for row in receipt["resource_pack"]["requests"]
                       if row["method"] == "GET" and row["status"] == 200]
    require(bool(successful_gets) is (expected == "LOADED"),
            f"{expected} loopback pack request behavior drifted")

    client = receipt["client"]
    policy = "enabled" if expected == "LOADED" else "disabled"
    require(client["launched"] is True and client["server_resource_pack_policy"] == policy
            and client["client_log_retained"] is False,
            f"{expected} client decision binding drifted")
    for field, hash_field in (("receipt", "receipt_sha256"),
                              ("launch_options", "launch_options_sha256"),
                              ("launch_servers", "launch_servers_sha256"),
                              ("final_options", "final_options_sha256"),
                              ("final_servers", "final_servers_sha256")):
        path = (receipt_path.parent / client[field]).resolve()
        require(within(receipt_path.parent, path) and path.is_file()
                and sha(path) == client[hash_field],
                f"{expected} retained client {field} drifted")
    client_receipt = load(receipt_path.parent / client["receipt"])
    require(client_receipt["server_resource_pack_policy"] == policy
            and client_receipt["servers_dat_sha256"] == client["launch_servers_sha256"]
            and client_receipt["accessibility_profile"]["options_sha256"]
                == client["launch_options_sha256"]
            and client_receipt["accessibility_profile"]["tutorial_toast_disabled"] is True,
            f"{expected} client fixture hashes drifted")
    for options_field in ("launch_options", "final_options"):
        lines = (receipt_path.parent / client[options_field]).read_text(
            encoding="utf-8").splitlines()
        require("tutorialStep:none" in lines,
                f"{expected} {options_field} lost deterministic tutorial suppression")
    servers = (receipt_path.parent / client["launch_servers"]).read_bytes()
    require(servers[:1] == b"\x0a" and b"acceptTextures" in servers
            and servers.endswith(bytes((1 if expected == "LOADED" else 0, 0, 0))),
            f"{expected} uncompressed server policy fixture drifted")
    paper_log = (receipt_path.parent / receipt["paper_log"]["file"]).resolve()
    require(within(receipt_path.parent, paper_log) and paper_log.is_file()
            and sha(paper_log) == receipt["paper_log"]["sha256"],
            f"{expected} Paper log drifted")
    return receipt


def validate() -> None:
    index = load(INDEX)
    require(index["status"] == "bounded_handshake_pair_pass_human_visual_parity_open"
            and index["production_enablement"] == "blocked",
            "resource-pack index overclaims completion")
    source_commit = index["source_commit"]
    require(subprocess.run(["git", "merge-base", "--is-ancestor", source_commit, "HEAD"],
                           cwd=ROOT, capture_output=True).returncode == 0,
            "resource-pack source commit is not an ancestor of HEAD")
    for name, artifact in index["artifacts"].items():
        path = ROOT / artifact["path"]
        observed = (sha(path) if name == "receipt_checker"
                    else historical_sha(source_commit, artifact["path"]))
        require(path.is_file() and observed == artifact["sha256"],
                f"resource-pack artifact drifted: {artifact['path']}")
    retained_root = INDEX.parent.resolve()
    loaded = validate_lane(retained_root, index["lanes"]["loaded"], "LOADED")
    declined = validate_lane(retained_root, index["lanes"]["declined"], "DECLINED")
    for field in ("sha1", "sha256", "bytes"):
        require(loaded["resource_pack"][field] == declined["resource_pack"][field],
                f"accepted/declined pack {field} mismatch")
    require(loaded["source_commit"] == source_commit == declined["source_commit"],
            "accepted/declined source commit mismatch")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - checker emits one concise failure
        print(f"MORROW RESOURCE PACK RECEIPTS: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW RESOURCE PACK RECEIPTS: PASS loaded=1 declined=1 visual-parity=open")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
