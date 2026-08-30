#!/usr/bin/env python3
"""Fail-closed validator for the retained Morrow P0 item 10 rehearsal bundle."""

from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
RECEIPTS = ROOT / "morrow" / "rehearsal" / "receipts" / "p0-item10-fa1b80b"
SOURCE_CHECKPOINT = "fa1b80b84959dc62012c209c4749c6e31abde8ec"
RELEASE = "morrow.rehearsal.fa1b80b.p0-10.v1"
SEQUENCE = [
    "morrow.act0.case_chain_authenticated",
    "morrow.act0.server_handoff_recovered",
    "morrow.act1.room04_witnessed",
    "morrow.act1.static_proposal_authenticated",
    "morrow.act1.intention_error_proven",
    "morrow.act1.entity_replay_authorized",
    "morrow.act2.missing_role_completed",
    "morrow.act2.live_test_recorded",
    "morrow.act2.behavior_reuse_proven",
    "morrow.act2.private_contradiction_resolved",
]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate() -> None:
    required = {
        "automated-rehearsal.json", "bundle-manifest.json", "cohort-1.json", "cohort-2.json",
        "cohort-6.json", "launch-matrix.json", "network-boundary.json", "paper-runtime.json",
    }
    require(RECEIPTS.is_dir(), "retained rehearsal receipt directory is missing")
    present = {path.name for path in RECEIPTS.glob("*.json")}
    require(present == required, f"receipt file set drifted: {sorted(present ^ required)}")

    manifest = load(RECEIPTS / "bundle-manifest.json")
    require(manifest["status"] == "pass", "automated bundle is not green")
    require(manifest["source_checkpoint"] == SOURCE_CHECKPOINT, "source checkpoint binding drifted")
    require(manifest["release_id"] == RELEASE, "release binding drifted")
    for name, expected in manifest["bundle_files"].items():
        require(sha(RECEIPTS / name) == expected, f"retained receipt hash mismatch: {name}")
    canonical_hashes = json.dumps(manifest["bundle_files"], sort_keys=True, separators=(",", ":")).encode()
    require(hashlib.sha256(canonical_hashes).hexdigest() == manifest["bundle_sha256"],
            "bundle aggregate hash mismatch")
    artifact = manifest["artifact_binding"]
    for relative, expected in artifact["files"].items():
        require(sha(ROOT / relative) == expected, f"bound artifact changed after rehearsal: {relative}")

    for count in (1, 2, 6):
        cohort = load(RECEIPTS / f"cohort-{count}.json")
        require(cohort["status"] == "pass" and cohort["cohort_size"] == count,
                f"cohort {count} did not pass")
        require(cohort["source_checkpoint"] == SOURCE_CHECKPOINT and cohort["release_id"] == RELEASE,
                f"cohort {count} binding mismatch")
        require(len(cohort["players"]) == count and len(set(cohort["players"])) == count,
                f"cohort {count} identity isolation failed")
        receipts = cohort["event_receipts"]
        require([row["event"] for row in receipts] == SEQUENCE, f"cohort {count} event order drifted")
        require([row["sequence"] for row in receipts] == list(range(1, 11)),
                f"cohort {count} sequence cursor drifted")
        require(len(cohort["restart_after_every_durable_boundary"]) == len(SEQUENCE),
                f"cohort {count} lacks a restart at every durable boundary")
        require(not cohort["live_capture_event_present"]
                and cohort["live_capture_authorization"] == "offered_not_accepted",
                f"cohort {count} silently accepted later capture")
        private = cohort["private_delivery"]
        require(not private["group_receipt_contains_private_payload"]
                and len(private["intended_recipients"]) == count,
                f"cohort {count} private recipient aggregation drifted")
        require(all(row["visible_to"] == [row["player_id"]] for row in private["intended_recipients"]),
                f"cohort {count} private evidence crossed player ownership")

    automated = load(RECEIPTS / "automated-rehearsal.json")
    require(automated["automated_gate"] == "pass", "aggregate automated gate failed")
    sequence = automated["experience_sequence"]
    require(len(sequence) == 15 and sequence[-1]["status"] == "offered_not_accepted",
            "complete vertical-slice experience sequence drifted")
    negative = {row["case"]: row for row in automated["negative_paths"]}
    expected_negative = {
        "reordered", "partial", "wrong_action", "cancel", "decline", "duplicate",
        "altered_collision", "wrong_release", "wrong_campaign", "wrong_player",
        "disconnect_rejoin", "cursor_loss",
    }
    require(set(negative) == expected_negative, "negative path coverage drifted")
    require(all(not row["progressed"] for row in negative.values()), "a negative path progressed")
    require(negative["altered_collision"]["worker_halted"], "collision did not halt")
    require(negative["duplicate"]["sequence_unchanged"], "duplicate changed the cursor")
    require({row["surface"] for row in automated["outage_recovery"]}
            == {"copperline", "discord", "supabase"}, "outage surface coverage drifted")
    require(all(row["status"] == "recovered" and row["ordered_delivery"]
                for row in automated["outage_recovery"]), "outage recovery is not ordered")
    require(all(row["status"] == "pass" for row in automated["static_runtime_contracts"]),
            "a static runtime contract failed")

    network = load(RECEIPTS / "network-boundary.json")
    require(network["status"] == "pass" and not network["non_loopback_attempts"]
            and not network["production_contacted"], "network boundary was crossed")
    paper = load(RECEIPTS / "paper-runtime.json")
    require(paper["status"] in {"not_run_pinned_runtime_unavailable", "pass"},
            "Paper lane is ambiguous rather than fail-closed")
    matrix = load(RECEIPTS / "launch-matrix.json")
    require(matrix["production_enablement"] == "blocked", "rehearsal opened production enablement")
    require(len(matrix["human_client_required"]) == 6
            and all(row["status"] == "required" for row in matrix["human_client_required"]),
            "client-only evidence was misrepresented as automated proof")

    # Test the exact checked-in bundle for accidental carryover without writing the retired names here.
    retired = ["hold", "keep" + "er", "aver" + "yn", "wr" + "en", "nol" + "and",
               "deep " + "hold", "un" + "lit"]
    authored = "\n".join(path.read_text(encoding="utf-8").lower()
                           for path in sorted((ROOT / "morrow" / "rehearsal").rglob("*")) if path.is_file())
    require(not any(term in authored for term in retired), "retired-canon text leaked into rehearsal artifacts")

    with tempfile.TemporaryDirectory(prefix="morrow-p0-check-") as temporary:
        regenerated = Path(temporary) / "receipts"
        result = subprocess.run(
            [sys.executable, str(ROOT / "tools" / "run_morrow_p0_rehearsal.py"),
             "--output", str(regenerated), "--timestamp", "2026-08-30T00:00:00Z"],
            cwd=ROOT, capture_output=True, text=True,
        )
        require(result.returncode == 0, f"deterministic rehearsal rerun failed: {result.stderr}")
        regenerated_files = {path.name: path.read_bytes() for path in regenerated.glob("*.json")}
        retained_files = {path.name: path.read_bytes() for path in RECEIPTS.glob("*.json")}
        require(regenerated_files == retained_files, "retained receipts are not deterministic")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - audit emits one concise failing receipt
        print(f"MORROW P0 REHEARSAL CHECK: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW P0 REHEARSAL CHECK: PASS cohorts=1/2/6 boundaries=10 network=loopback-only client=required")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
