#!/usr/bin/env python3
"""Validate the canonical continuation lineage and its fail-closed M3 authority."""
from __future__ import annotations

import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[1]
LINEAGE = ROOT / "design" / "handoff" / "CANONICAL-LINEAGE.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=ROOT, check=True, text=True, capture_output=True
    ).stdout.strip()


def main() -> None:
    data = json.loads(LINEAGE.read_text(encoding="utf-8"))
    require(data["schema_version"] == "1.1.0-continuation-lineage", "lineage schema drift")
    checkpoint = data["checkpoint_identity"]
    require(checkpoint["branch"] == "codex/m3-disposable-paper-gate", "canonical branch drift")
    require(checkpoint["production_mutated"] is False, "integration cannot claim production mutation")
    require(checkpoint["m4_authority"] == "closed", "M4 must remain closed")

    starting = data["starting_authority"]
    require(starting["commit"] == "8f938e0558b545a33039bb112ad091cf511a6ffe",
            "failed-review starting authority drift")
    require(starting["visual_decision"] == "failed_revision_required_2026-07-16",
            "failed Brad decision lost")
    require(starting["current_authority"] is False
            and starting["superseded_for_revision_by"] == "observance-p4-private-slice-v2",
            "v1 rejection history/current-revision distinction drift")

    expected = {
        "84899e0d367c8ad6151ea74b0c338e92ac8a9b7c":
            "design/m2/M2-SUPABASE-VALIDATION.md",
        "a6069aabae705f18255428f612476552c8770e33":
            "design/m3/EXTERNAL-GAP-AUDIT-2026-07-16.md",
    }
    rows = {row["source_commit"]: row for row in data["incorporated_evidence"]}
    require(set(rows) == set(expected), "sibling evidence provenance is incomplete or duplicated")
    for source_commit, authority in expected.items():
        row = rows[source_commit]
        require(row["authority"] == authority, f"authority drift for {source_commit}")
        require((ROOT / authority).is_file(), f"missing incorporated authority: {authority}")
        require(bool(row["unresolved"]), f"unresolved gaps lost for {source_commit}")
        git("cat-file", "-e", f"{source_commit}^{{commit}}")
        linear = row["linear_commit"]
        git("cat-file", "-e", f"{linear}^{{commit}}")
        require(subprocess.run(
            ["git", "merge-base", "--is-ancestor", linear, "HEAD"], cwd=ROOT
        ).returncode == 0, f"linear incorporation commit is not an ancestor: {linear}")

    revision = data["revision_evidence"]
    require(revision["integration_checkpoint"] == "69d729460382576d07676103b54048b1a648c412",
            "canonical integration checkpoint drift")
    for commit in (revision["authored_source_commit"], revision["paper_persistence_fix_commit"],
                   revision["pristine_receipt_fix_commit"]):
        git("cat-file", "-e", f"{commit}^{{commit}}")
        require(subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"], cwd=ROOT).returncode == 0,
                f"M3 revision checkpoint is not an ancestor: {commit}")
    paper = revision["paper_result"]
    require(paper == {"version": "1.21.11", "build": 132, "cells": 248745,
                      "closed_gate_collision_cells": 88, "open_gate_collision_cells": 0,
                      "evidence_surfaces": 14, "filing_surfaces": 6,
                      "restart_replay_receipts": 28, "validation_status": "passed"},
            "M3 v2 Paper result lineage drift")
    require(bool(revision["unresolved"]), "M3 v2 unresolved human/client gates lost")
    failed = revision["preserved_failed_attempts"]
    require([row["result"] for row in failed] == ["failed_closed",
            "world_build_and_audit_passed_receipt_generation_failed_closed"]
            and all(row["reuse_forbidden_and_observed"] for row in failed),
            "fresh-target failure provenance lost or weakened")
    for authority in (revision["current_authority"], revision["rejected_authority_preserved_at"],
                      revision["validation_receipt"], revision["review_target_receipt"]):
        require((ROOT / authority).is_file(), f"missing M3 revision lineage authority: {authority}")

    active = data["active_brad_review"]
    require(active["status"] == "in_progress_binding_findings_recorded_not_approved"
            and active["brad_visual_approval"] is None and active["m4_authority"] == "closed",
            "active Brad review approval/M4 gate drift")
    require(active["implementation_state"] == "paused_until_full_feedback_pass_complete"
            and active["live_server_directive"] == "do_not_mutate_or_stop_while_brad_continues_walk",
            "active-review pause or live-server preservation directive lost")
    require(len(active["findings"]) == 7
            and "not yet fully authored or legible in-world" in active["interpretation"],
            "active Brad findings or binding interpretation drift")

    gate = data["current_gate"]
    require(gate["m4_open"] is False and "Brad's new in-game visual decision" in gate["required_next_evidence"],
            "M3 rejection gate weakened")
    review = (ROOT / "design" / "m3" / "BRAD-REVIEW-PACKAGE.md").read_text(encoding="utf-8")
    require("FAILED / REVISION REQUIRED" in review and "M4 district implementation authority: **CLOSED" in review,
            "Brad rejection authority missing")
    print("CONTINUATION LINEAGE: PASS (2 sibling receipts, v1 rejection preserved, v2 Paper receipts current, M4 closed)")


if __name__ == "__main__":
    main()
